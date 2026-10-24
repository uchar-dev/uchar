package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.DrlPiece;

import org.junit.jupiter.api.Test;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code TmsTranspiler.drlPieces} over the corpus (card drl-rule-functions, acceptance (a)): every golden the backend
 * carries, built on the DRL-text branch, once without readings, the build whose text the toolchain shows, and once with
 * them, whose reading rules have a function of their own. Per text:
 *
 * <ul>
 *   <li>the pieces name the rules of the KieBase, each once, in the order of the lines {@code rule "…"} of the text,
 *       read as the parser reads them ({@link #RULE_HEADER}, unescaped): an oracle of this witness, which the rendering
 *       does not consult;</li>
 *   <li>the text before the first piece is the header, lines {@code package}, {@code import}, {@code global} and empty
 *       lines; the pieces follow one another without a gap and without an overlap up to the end of the text;</li>
 *   <li>a piece begins with its first function, or with its line {@code rule "…"} where it has none, and that line, with
 *       the name as a DRL string literal, stands at {@code ruleStart}; the rule ends with the line {@code end} and an
 *       empty line, at the end of the piece, and has no other line {@code end} and no declaration of a function;</li>
 *   <li>a function begins with its comment, one line {@code // …}, or with its declaration; the declaration at
 *       {@code declaration} declares the function by the name of the record; the function ends with its closing
 *       brace and an empty line, and declares no other function;</li>
 *   <li>every declaration of a function in the text ({@link #DECLARATION}, a second oracle) lies in a piece;</li>
 *   <li>every call of a declared function in a piece, outside the lines of a comment and of a declaration, is a call of
 *       a function of that piece ({@link #CALL}, a third oracle). A call of a function of another rule is a difference:
 *       the record has no place for it, since the rendering names a function after its rule and writes its calls only
 *       in that rule's piece. Measured 2026-10-24: none;</li>
 *   <li>every function of a piece is reached from its rule, directly or through other functions of the piece, but the
 *       ones in {@link #EXPECTED_UNREACHED}, each by golden, rule and function, per build: the list holds with equality,
 *       so a function that stops being reached, or one that stops being unreached, fails by name. The functions in it are
 *       binders of a {@code Let} whose body does not read its variable.</li>
 * </ul>
 *
 * <p>The places are UTF-16 code units, as {@code String.substring} takes them: {@value #EXPECTED_MIN_NON_ASCII_TEXTS}
 * texts carry a character outside ASCII, where a place in bytes of UTF-8 would miss. No golden carries a character
 * outside the Basic Multilingual Plane, where a place in code points would miss; {@code DrlPiecesWitnessTest} has a
 * document with one.
 *
 * <p>Floors, measured 2026-10-24 on 282 carried goldens: the pieces, functions, comments and calls of a function of the
 * same piece, without readings and with them ({@link #EXPECTED_MIN}); a smaller count is a corpus that lost goldens or a
 * selection that silently stopped checking.
 */
class DrlPiecesCorpusWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    /** The goldens the backend carries, measured 2026-10-24. */
    static final int EXPECTED_MIN_GOLDENS = 282;
    /** The texts, without readings, that carry a character outside ASCII, measured 2026-10-24. */
    static final int EXPECTED_MIN_NON_ASCII_TEXTS = 49;
    /**
     * The functions that their rule does not reach, directly or through other functions of its piece, measured
     * 2026-10-24 per build, without readings and with them, the same one in both: the binder of the {@code Let} "de
     * bergingsgrond is zijn bergingsgrondslag" in {@code defaults A:Jutter.bergingsforfait: default 0} of
     * {@code jutterswet-bergingstoeslag}, whose body is a constant ({@code return new java.math.BigDecimal("42")}) and
     * does not read the variable, so nothing calls the binder function. The list is the whole allowance: both builds
     * are held to it with equality, so a function that stops being reached, or another that starts not to be, fails
     * by golden, rule and function name, where a pattern on the name would let any binder through.
     */
    static final List<Unreached> EXPECTED_UNREACHED = List.of(
            new Unreached("jutterswet-bergingstoeslag", "defaults A:Jutter.bergingsforfait: default 0", "r2_binder4"));

    /** A function that its rule does not reach: the golden (the path under the golden zone), the name of the rule and
     *  the name of the function. */
    record Unreached(String golden, String rule, String function) {}

    /** Per count, without readings and with them ({@code met lezingen}), measured 2026-10-24. */
    static final Map<String, Integer> EXPECTED_MIN = Map.ofEntries(
            Map.entry("stukken", 2124), Map.entry("stukken zonder functies", 763), Map.entry("functies", 2499),
            Map.entry("functies met commentaar", 1944), Map.entry("functies die een functie van hun stuk aanroepen", 481),
            Map.entry("stukken met lezingen", 3046), Map.entry("functies met lezingen", 3456));

    /** The line that opens a rule: {@code rule} and its name as a DRL string literal. */
    private static final Pattern RULE_HEADER = Pattern.compile("^rule \"((?:[^\"\\\\]|\\\\.)*)\"$", Pattern.MULTILINE);
    /** The line that declares a function: {@code function}, its type and its name before its parameters. */
    private static final Pattern DECLARATION = Pattern.compile("^function \\S+ ([A-Za-z_][A-Za-z0-9_]*)\\(",
            Pattern.MULTILINE);
    /** A name followed by an opening parenthesis, not a member of something and not part of a longer name. */
    private static final Pattern CALL = Pattern.compile("(?<![A-Za-z0-9_$.])([A-Za-z_][A-Za-z0-9_]*)\\(");
    private static final Pattern HEADER_LINE = Pattern.compile("(package|import|global) [^\\n]*;|");

    @Test void the_pieces_of_the_rules_cover_the_text_after_its_header() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        List<String> differences = new ArrayList<>();
        List<Unreached> unreached = new ArrayList<>(), unreachedWithReadings = new ArrayList<>();
        Map<String, Integer> counts = new TreeMap<>();
        int carried = 0, nonAscii = 0;
        for (Path g : goldens) {
            JsonNode ir = TmsDocuments.JSON.readTree(g.resolve("ir.json").toFile());
            DroolsBackend.Build build;
            try {
                build = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT);
            } catch (IrLoader.Refusal | IrLoader.DocumentError | CaseRunner.Excluded notCarried) {
                continue;
            }
            carried++;
            String name = GOLDEN.relativize(g).toString();
            if (build.drl().chars().anyMatch(c -> c > 127)) nonAscii++;
            check(name, build, "", differences, unreached, counts);
            check(name, DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT, true), " met lezingen", differences,
                    unreachedWithReadings, counts);
        }
        assertEquals(List.of(), differences, "stukken die de DRL-tekst niet precies dekken");
        int n = carried, a = nonAscii;
        System.out.println("DrlPiecesCorpusWitnessTest: " + n + " goldens, " + a + " teksten met een teken buiten ASCII, "
                + counts + ", niet bereikt: " + unreached + ", met lezingen " + unreachedWithReadings);
        assertSameUnreached(unreached, "zonder lezingen");
        assertSameUnreached(unreachedWithReadings, "met lezingen");
        assertTrue(n >= EXPECTED_MIN_GOLDENS, () -> "gedragen goldens " + n + ", onder de vloer " + EXPECTED_MIN_GOLDENS);
        assertTrue(a >= EXPECTED_MIN_NON_ASCII_TEXTS, () -> "teksten met een teken buiten ASCII " + a
                + ", onder de vloer " + EXPECTED_MIN_NON_ASCII_TEXTS);
        for (Map.Entry<String, Integer> e : EXPECTED_MIN.entrySet()) {
            int c = counts.getOrDefault(e.getKey(), 0);
            assertTrue(c >= e.getValue(), () -> e.getKey() + " " + c + ", onder de vloer " + e.getValue() + ": " + counts);
        }
    }

    /** The unreached functions of one build are those of {@link #EXPECTED_UNREACHED}, no more and no fewer. */
    private static void assertSameUnreached(List<Unreached> actual, String build) {
        List<Unreached> outside = new ArrayList<>(actual), stale = new ArrayList<>(EXPECTED_UNREACHED);
        outside.removeAll(EXPECTED_UNREACHED);
        stale.removeAll(actual);
        assertTrue(outside.isEmpty() && stale.isEmpty() && actual.equals(EXPECTED_UNREACHED),
                () -> "functies die hun regel niet bereiken, " + build + ": " + actual.size() + ", in de lijst "
                        + EXPECTED_UNREACHED.size() + "; niet in de lijst: " + outside.size() + " " + first(outside)
                        + "; in de lijst en wel bereikt: " + stale.size() + " " + first(stale));
    }

    private static String first(List<Unreached> list) {
        return list.size() > 10 ? list.subList(0, 10) + " en " + (list.size() - 10) + " meer" : list.toString();
    }

    /** The checks of the text of {@code build} and its pieces; {@code suffix} names the build in the counts and the
     *  differences. */
    private static void check(String golden, DroolsBackend.Build build, String suffix, List<String> differences,
                              List<Unreached> unreached, Map<String, Integer> counts) {
        String which = golden + suffix;
        String text = build.drl();
        List<DrlPiece> pieces = build.execModelTranspiler().drlPieces();
        int before = differences.size();

        // the pieces name the rules of the KieBase, in the order of the headers of the text
        List<String> headers = new ArrayList<>();
        for (Matcher m = RULE_HEADER.matcher(text); m.find(); ) headers.add(unescape(m.group(1)));
        List<String> named = pieces.stream().map(DrlPiece::rule).toList();
        if (!named.equals(headers))
            differences.add(which + ": de stukken noemen " + named + ", de koppen van de tekst " + headers);
        KiePackage p = build.kieBase().getKiePackage(TmsTranspiler.PKG);
        Set<String> inBase = new java.util.TreeSet<>();
        if (p != null) for (Rule r : p.getRules()) inBase.add(r.getName());
        if (!new java.util.TreeSet<>(named).equals(inBase) || new LinkedHashSet<>(named).size() != named.size())
            differences.add(which + ": de stukken noemen " + named + ", de KieBase " + inBase);

        // the header, then the pieces one after the other up to the end of the text
        int headerEnd = pieces.isEmpty() ? text.length() : pieces.get(0).start();
        if (headerEnd < 0 || headerEnd > text.length() || headerEnd > 0 && text.charAt(headerEnd - 1) != '\n') {
            differences.add(which + ": het eerste stuk begint op " + headerEnd + ", niet aan het begin van een regel");
            return;
        }
        for (String line : text.substring(0, headerEnd).split("\n", -1))
            if (!HEADER_LINE.matcher(line).matches())
                differences.add(which + ": de kop vóór het eerste stuk heeft de regel '" + line + "'");
        for (int i = 0; i < pieces.size(); i++) {
            int next = i + 1 < pieces.size() ? pieces.get(i + 1).start() : text.length();
            if (pieces.get(i).end() != next)
                differences.add(which + " | " + pieces.get(i).rule() + ": het stuk eindigt op " + pieces.get(i).end()
                        + ", het volgende begint op " + next);
        }
        if (differences.size() > before) return;

        Set<String> declared = new LinkedHashSet<>();
        for (Matcher m = DECLARATION.matcher(text); m.find(); )
            if (!declared.add(m.group(1))) differences.add(which + ": de functie " + m.group(1) + " staat er twee keer");
        Set<String> inPieces = new LinkedHashSet<>();
        for (DrlPiece piece : pieces) {
            String where = which + " | " + piece.rule();
            counts.merge("stukken" + suffix, 1, Integer::sum);
            if (piece.functions().isEmpty()) counts.merge("stukken zonder functies" + suffix, 1, Integer::sum);
            String rule = text.substring(piece.ruleStart(), piece.end());
            if (!rule.startsWith("rule " + Namen.drlString(piece.rule()) + "\n"))
                differences.add(where + ": op ruleStart staat niet de kop van de regel: " + head(rule));
            if (!rule.endsWith("\nend\n\n") || rule.indexOf("\nend\n") != rule.length() - "\nend\n\n".length())
                differences.add(where + ": de regel eindigt niet met zijn ene regel end en een lege regel");
            if (DECLARATION.matcher(rule).find()) differences.add(where + ": een functie in de regel zelf");
            Map<String, String> bodies = new HashMap<>();
            for (DrlPiece.DrlFunction f : piece.functions()) {
                counts.merge("functies" + suffix, 1, Integer::sum);
                String comment = text.substring(f.start(), f.declaration());
                String function = text.substring(f.declaration(), f.end());
                if (!comment.isEmpty()) {
                    counts.merge("functies met commentaar" + suffix, 1, Integer::sum);
                    if (!comment.startsWith("// ") || comment.indexOf('\n') != comment.length() - 1)
                        differences.add(where + " | " + f.name() + ": het commentaar is niet één regel // …: "
                                + head(comment));
                }
                Matcher d = DECLARATION.matcher(function);
                if (!d.lookingAt() || !d.group(1).equals(f.name()) || d.find())
                    differences.add(where + " | " + f.name() + ": op declaration staat niet precies de declaratie van"
                            + " die functie: " + head(function));
                if (!function.endsWith("\n}\n\n"))
                    differences.add(where + " | " + f.name() + ": de functie eindigt niet met haar accolade en een lege"
                            + " regel");
                inPieces.add(f.name());
                bodies.put(f.name(), function);
            }
            // every call in the piece is of a function of the piece; every function of it is reached from the rule
            Set<String> fromRule = calls(rule, declared);
            for (String c : fromRule)
                if (!bodies.containsKey(c)) differences.add(where + ": de regel roept " + c + " aan, een functie van een"
                        + " ander stuk");
            for (Map.Entry<String, String> b : bodies.entrySet()) {
                Set<String> called = calls(b.getValue(), declared);
                if (!called.isEmpty())
                    counts.merge("functies die een functie van hun stuk aanroepen" + suffix, 1, Integer::sum);
                for (String c : called)
                    if (!bodies.containsKey(c)) differences.add(where + " | " + b.getKey() + ": roept " + c + " aan, een"
                            + " functie van een ander stuk");
            }
            Set<String> reached = new LinkedHashSet<>(fromRule);
            Deque<String> todo = new ArrayDeque<>(fromRule);
            while (!todo.isEmpty())
                for (String c : calls(bodies.getOrDefault(todo.pop(), ""), declared))
                    if (reached.add(c)) todo.push(c);
            for (DrlPiece.DrlFunction f : piece.functions())
                if (!reached.contains(f.name())) unreached.add(new Unreached(golden, piece.rule(), f.name()));
        }
        if (!inPieces.equals(declared))
            differences.add(which + ": de tekst declareert " + declared.size() + " functies, de stukken noemen er "
                    + inPieces.size());
    }

    /** The declared functions that {@code code} calls, outside the lines of a comment and of a declaration. */
    private static Set<String> calls(String code, Set<String> declared) {
        Set<String> out = new LinkedHashSet<>();
        for (String line : code.split("\n")) {
            if (line.startsWith("// ") || line.startsWith("function ")) continue;
            for (Matcher m = CALL.matcher(line); m.find(); )
                if (declared.contains(m.group(1))) out.add(m.group(1));
        }
        return out;
    }

    /** The name of a header, as {@code Namen.drlString} escapes it, unescaped. */
    private static String unescape(String literal) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            char c = literal.charAt(i);
            if (c != '\\') { out.append(c); continue; }
            char e = literal.charAt(++i);
            out.append(switch (e) {
                case 'r' -> '\r';
                case 'n' -> '\n';
                default -> e;
            });
        }
        return out.toString();
    }

    private static String head(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }
}
