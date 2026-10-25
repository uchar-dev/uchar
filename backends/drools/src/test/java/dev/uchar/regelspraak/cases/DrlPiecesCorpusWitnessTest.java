package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.ComputeHelpers;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.DrlPiece;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.drools.drl.parser.lang.DRL6Lexer;
import org.drools.drl.parser.lang.DroolsSoftKeywords;
import org.junit.jupiter.api.Test;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.mvel2.compiler.AbstractParser;

import javax.lang.model.SourceVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
 *       binders of a {@code Let} whose body does not read its variable;</li>
 *   <li>every function does work of its own (card drl-no-passthrough-functions, acceptance (a)): its body is not only
 *       the return of the call of another function of its piece; it is not the function of a composite over no member
 *       whose verdict is true, nor of a composite over one member whose verdict is that member's, the verdicts being
 *       those of {@code ComputeHelpers.established} and {@code endVerdict}, which a composite calls; and the locals
 *       {@code raak} and {@code vast} of a composite stand only where they are read ({@link #ownWork}). Measured on
 *       {@code ce2566f3}, before that card, per build: 180 functions that only returned such a call (176 tests and 4
 *       matrices, each over its composite), 58 functions of a composite over no member, each with a {@code vast} it
 *       did not read, and 69 of a composite over one member that gives its verdict.</li>
 *   <li>every function is named after its kind and its rule (card drl-function-names-from-rule, acceptance (a),
 *       {@link #names}): {@code <kind>_<rule>}, the kind one of {@link #KINDS}, numbered from 1 in the order of the
 *       text right after the kind where the piece has more than one function of that kind, and the rule part
 *       {@code Namen.ident} of the rule's name without the code points Drools does not read in a name, as its DRL
 *       lexer and Java say ({@link #namePart}), {@code x} where none remains, followed by {@code _} and the rule's
 *       number among them where more than one piece with functions has that rule part; the name is not that of a
 *       global of the header, a day kind's among them, nor a word of DRL, of MVEL or of Java, as Drools, MVEL and
 *       Java list them; and outside comments and string literals every occurrence of it in the text is its
 *       declaration or a call, so that no member of a bean a pattern reads, no local, no parameter and no global
 *       has its name. Measured on {@code f867513d}, before that card: every function named
 *       {@code r<n>_<kind><place>}, outside the form.</li>
 *   <li>a value that computes nothing has no function (card drl-no-trivial-value-functions, acceptance (a),
 *       {@link #ownWork} and {@link #carriedValue}): no function of the value of a decision or of a value consequence
 *       ({@link #VALUE_KINDS}) only returns, within or without the standard {@code try}, a constant as the text spells
 *       one ({@link #CONSTANT}), one of its parameters or {@code ComputeHelpers.collapse} of one of them; a consequence
 *       that carries such an expression itself, {@code Object v = …;} over a constant or a variable its rule binds, is
 *       counted, with the comment line of the sentence above it apart, and asks no evaluation fault of {@code v}. A
 *       function of another kind that only returns such an expression, the key of a conflict, is counted and allowed.
 *       Measured on {@code 4707a183}, before that card, per build: 330 such functions of a value (182 of a decision, 148
 *       of a value consequence) and no consequence that carries its value.</li>
 * </ul>
 *
 * <p>The places are UTF-16 code units, as {@code String.substring} takes them: {@value #EXPECTED_MIN_NON_ASCII_TEXTS}
 * texts carry a character outside ASCII, where a place in bytes of UTF-8 would miss. No golden carries a character
 * outside the Basic Multilingual Plane, where a place in code points would miss; {@code DrlPiecesWitnessTest} has a
 * document with one.
 *
 * <p>Floors, measured 2026-10-24 on 282 carried goldens: the pieces, functions, comments and calls of a function of the
 * same piece, without readings and with them ({@link #EXPECTED_MIN}); a smaller count is a corpus that lost goldens or a
 * selection that silently stopped checking. Card drl-no-passthrough-functions lowered the floors of the functions, of
 * those with a comment and of those that call a function of their piece, measured 2026-10-25, since it writes fewer
 * functions: per build 240 functions of a composite and 58 tests went. Card drl-function-names-from-rule added the
 * floors of the names with a number of their kind, measured 2026-10-25. Card drl-no-trivial-value-functions lowered the
 * floors of the functions and of those with a comment and raised that of the pieces without functions, measured
 * 2026-10-25, since per build 330 functions of a value went, and added the floors of the consequences that carry their
 * value.
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
     * 2026-10-24 per build, without readings and with them, the same one in both (named {@code r2_binder4} until
     * card drl-no-passthrough-functions took the test and the composite of that default away, and {@code r2_binder2}
     * until card drl-function-names-from-rule named it after its kind and its rule): the binder of the
     * {@code Let} "de bergingsgrond is zijn bergingsgrondslag" in {@code defaults A:Jutter.bergingsforfait: default
     * 0} of {@code jutterswet-bergingstoeslag}, whose body is a constant
     * ({@code return new java.math.BigDecimal("42")}) and does not read the variable, so nothing calls the binder
     * function. The list is the whole allowance: both builds
     * are held to it with equality, so a function that stops being reached, or another that starts not to be, fails
     * by golden, rule and function name, where a pattern on the name would let any binder through.
     */
    static final List<Unreached> EXPECTED_UNREACHED = List.of(
            new Unreached("jutterswet-bergingstoeslag", "defaults A:Jutter.bergingsforfait: default 0",
                    "binder_defaultsAJutterBergingsforfaitDefault0"));

    /** A function that its rule does not reach: the golden (the path under the golden zone), the name of the rule and
     *  the name of the function. */
    record Unreached(String golden, String rule, String function) {}

    /** Per count, without readings and with them ({@code met lezingen}), measured 2026-10-24; the functions, those
     *  with a comment and those that call a function of their piece measured 2026-10-25 (card
     *  drl-no-passthrough-functions; before it 2499, 1944, 481 and with readings 3456); the names with a number of
     *  their kind measured 2026-10-25 (card drl-function-names-from-rule); the functions, those with a comment, the
     *  pieces without functions and the consequences that carry their value measured 2026-10-25 (card
     *  drl-no-trivial-value-functions; before it 2201, 1680, 763 and with readings 3158, 763). */
    static final Map<String, Integer> EXPECTED_MIN = Map.ofEntries(
            Map.entry("stukken", 2124), Map.entry("stukken zonder functies", 879), Map.entry("functies", 1871),
            Map.entry("functies met commentaar", 1422),
            Map.entry("functies die een functie van hun stuk aanroepen", 241),
            Map.entry("stukken met lezingen", 3046), Map.entry("functies met lezingen", 2828),
            Map.entry("stukken zonder functies met lezingen", 879),
            Map.entry("functienamen met een nummer van hun soort", 261),
            Map.entry("functienamen met een nummer van hun soort met lezingen", 265),
            Map.entry("gevolgen die hun waarde dragen", 330), Map.entry("gevolgen die hun waarde dragen met lezingen", 330),
            Map.entry("gevolgen die hun waarde dragen, met de zin erboven", 258),
            Map.entry("gevolgen die hun waarde dragen, met de zin erboven met lezingen", 258));

    /** The kinds of the functions of the text, the word of what a function computes, the first part of its name. */
    static final Set<String> KINDS = Set.of("afloop", "beta", "binder", "componenten", "conflict", "creatie",
            "deponering", "fout", "kwantorlezing", "let", "lezing", "lidtoets", "lijst", "matrix", "pad", "plaats",
            "samengesteld", "toets", "vouwlezing", "waarde", "zonderfout");

    /** The line that opens a rule: {@code rule} and its name as a DRL string literal. */
    private static final Pattern RULE_HEADER = Pattern.compile("^rule \"((?:[^\"\\\\]|\\\\.)*)\"$", Pattern.MULTILINE);
    /** The line that declares a function: {@code function}, its type and its name before its parameters. */
    private static final Pattern DECLARATION = Pattern.compile("^function \\S+ ([\\p{L}_][\\p{L}\\p{N}_]*)\\(",
            Pattern.MULTILINE);
    /** A name followed by an opening parenthesis, not a member of something and not part of a longer name. */
    private static final Pattern CALL = Pattern.compile("(?<![\\p{L}\\p{N}_$.])([\\p{L}_][\\p{L}\\p{N}_]*)\\(");
    private static final Pattern HEADER_LINE = Pattern.compile("(package|import|global) [^\\n]*;|");
    /** The line that declares a global: its type and its name. */
    private static final Pattern GLOBAL = Pattern.compile("^global \\S+ (\\S+);$", Pattern.MULTILINE);
    /** The name of a function: its kind, its number among the functions of that kind in its rule, and the rest. */
    private static final Pattern NAME = Pattern.compile("([a-z]+)([0-9]*)_(.+)");
    /** A name in a line of code, not part of a longer name and not a variable of a pattern. */
    private static final Pattern WORD = Pattern.compile("(?<![\\p{L}\\p{N}_$])([\\p{L}_][\\p{L}\\p{N}_]*)");
    /** The words of DRL, Drools's soft keywords, and of MVEL, its operators and literals, as the two list them. */
    private static final Set<String> WORDS = words();
    /** A line that returns a call: the name called and the text after its opening parenthesis up to the {@code ;}. */
    private static final Pattern RETURNED_CALL = Pattern.compile("\\s*return ([\\p{L}_][\\p{L}\\p{N}_]*)\\((.*);");
    /** The verdict of a composite once its members are read: quantifier, threshold, hits and number of members. */
    private static final Pattern END_VERDICT = Pattern.compile("ComputeHelpers\\.endVerdict\\("
            + "dev\\.uchar\\.regelspraak\\.ir\\.Condition\\.Quantifier\\.([A-Z_]+), (-?[0-9]+), ([a-z0-9]+),"
            + " ([0-9]+)\\)");
    /** The return of the verdict of a composite over no member. */
    private static final Pattern VERDICT_OVER_NONE = Pattern.compile("\\s*return ComputeHelpers\\.endVerdict\\("
            + "dev\\.uchar\\.regelspraak\\.ir\\.Condition\\.Quantifier\\.([A-Z_]+), (-?[0-9]+), [a-z0-9]+, 0\\);");
    /** A string literal, its escapes included. */
    private static final String STRING = "\"(?:[^\"\\\\]|\\\\.)*\"";
    /** A constant of the plan as the text spells it, in a function and in a consequence: null, a truth value, a text
     *  (in a consequence possibly literals joined with {@code +}), a number of each class, an exact rational, a date, a
     *  date-time, an enumeration label, a class, or a list of those. */
    private static final String SCALAR = "(?:null|Boolean\\.TRUE|Boolean\\.FALSE|" + STRING + "(?: \\+ " + STRING + ")*"
            + "|Integer\\.valueOf\\(-?[0-9]+\\)|Long\\.valueOf\\(-?[0-9]+L\\)"
            + "|new java\\.math\\.BigDecimal\\(" + STRING + "\\)|new java\\.math\\.BigInteger\\(" + STRING + "\\)"
            + "|Breuk\\.van\\(new java\\.math\\.BigInteger\\(" + STRING + "\\), new java\\.math\\.BigInteger\\(" + STRING
            + "\\)\\)|java\\.time\\.LocalDate\\.parse\\(" + STRING + "\\)|java\\.time\\.LocalDateTime\\.parse\\(" + STRING
            + "\\)|(?:[a-z][A-Za-z0-9_]*\\.)+[A-Z][A-Za-z0-9_$]*(?:\\.[A-Z][A-Za-z0-9_$]*)*\\.[A-Z][A-Z0-9_]*"
            + "|[A-Za-z][A-Za-z0-9_.$]*\\.class)";
    private static final Pattern CONSTANT = Pattern.compile(SCALAR + "|java\\.util\\.Arrays\\.asList\\((?:" + SCALAR
            + "(?:, " + SCALAR + ")*)?\\)");
    /** An expression that computes nothing: {@code ComputeHelpers.collapse} of, or else, a constant ({@link #CONSTANT})
     *  or a name; whether the name is a variable a binding binds, the caller asks. */
    private static final Pattern NOTHING_COMPUTED = Pattern.compile("ComputeHelpers\\.collapse\\((.*)\\)|(.*)");
    /** The kinds of the functions of the value of a decision and of a value consequence. */
    private static final Set<String> VALUE_KINDS = Set.of("afloop", "waarde");
    /** The line of a consequence that holds its value. */
    private static final Pattern CONSEQUENCE_VALUE = Pattern.compile("    Object v = (.*);");
    /** The lines around the verdict of a composite over no member in its function: its locals, and the try of a test
     *  that asks that it holds without a fault. */
    private static final Set<String> FRAME = Set.of("int raak = 0;", "Boolean vast;", "try {",
            "} catch (RuntimeException e) {", "ComputeHelpers.caught(e);", "return false;", "}");
    /** The lines of the standard try of the function of a value, around its return. */
    private static final Set<String> VALUE_FRAME = Set.of("try {", "} catch (RuntimeException e) {",
            "return ComputeHelpers.caught(e);", "}");

    @Test void the_pieces_of_the_rules_cover_the_text_after_its_header() throws Exception {
        List<Path> goldens;
        try (Stream<Path> s = Files.walk(GOLDEN)) {
            goldens = s.filter(p -> p.getFileName().toString().equals("ir.json")).map(Path::getParent).sorted().toList();
        }
        List<String> differences = new ArrayList<>(), notOwnWork = new ArrayList<>();
        List<String> outsideTheForm = new ArrayList<>();
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
            check(name, build, "", differences, notOwnWork, outsideTheForm, unreached, counts);
            check(name, DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT, true), " met lezingen", differences,
                    notOwnWork, outsideTheForm, unreachedWithReadings, counts);
        }
        assertEquals(List.of(), differences, "stukken die de DRL-tekst niet precies dekken");
        int n = carried, a = nonAscii;
        System.out.println("DrlPiecesCorpusWitnessTest: " + n + " goldens, " + a + " teksten met een teken buiten ASCII, "
                + counts + ", niet bereikt: " + unreached + ", met lezingen " + unreachedWithReadings + "; "
                + WORDS.size() + " woorden van DRL en MVEL");
        assertTrue(notOwnWork.isEmpty(), () -> "functies zonder eigen werk: " + notOwnWork.size() + ", de eerste "
                + notOwnWork.subList(0, Math.min(10, notOwnWork.size())) + "; " + counts);
        assertTrue(outsideTheForm.isEmpty(), () -> "functienamen buiten de vorm: " + outsideTheForm.size()
                + ", de eerste " + outsideTheForm.subList(0, Math.min(10, outsideTheForm.size())) + "; " + counts);
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
     *  differences, a function without work of its own goes to {@code notOwnWork}, and a name outside its form to
     *  {@code outsideTheForm}. */
    private static void check(String golden, DroolsBackend.Build build, String suffix, List<String> differences,
                              List<String> notOwnWork, List<String> outsideTheForm, List<Unreached> unreached,
                              Map<String, Integer> counts) {
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
            for (Map.Entry<String, String> b : bodies.entrySet())
                ownWork(where + " | " + b.getKey(), b.getKey(), b.getValue(), bodies.keySet(), suffix, notOwnWork, counts);
            carriedValue(where, rule, suffix, notOwnWork, counts);
        }
        if (!inPieces.equals(declared))
            differences.add(which + ": de tekst declareert " + declared.size() + " functies, de stukken noemen er "
                    + inPieces.size());
        names(which, text, pieces, suffix, outsideTheForm, counts);
    }

    /**
     * The checks of card drl-function-names-from-rule on the names of the functions of {@code text}, whose pieces are
     * {@code pieces}: each is {@code <kind>_<rule part>}, the kind one of {@link #KINDS} with its number among the
     * functions of its kind in its piece, from 1 in the order of the text, where the piece has more than one, and the
     * rule part that of {@link #rulePart} with the piece's number among the pieces with functions of that rule part,
     * after a {@code _}, where there are more than one; it is not a global of the header, nor a word of DRL, MVEL or
     * Java; and outside comments and string literals the text spells it only where it declares or calls it.
     */
    private static void names(String which, String text, List<DrlPiece> pieces, String suffix,
                              List<String> outsideTheForm, Map<String, Integer> counts) {
        Map<String, Integer> sharing = new HashMap<>(), partsSeen = new HashMap<>();
        for (DrlPiece p : pieces) if (!p.functions().isEmpty()) sharing.merge(rulePart(p.rule()), 1, Integer::sum);
        Set<String> globals = new HashSet<>(), names = new HashSet<>();
        for (Matcher m = GLOBAL.matcher(text); m.find(); ) globals.add(m.group(1));
        for (DrlPiece p : pieces) {
            if (p.functions().isEmpty()) continue;
            String part = rulePart(p.rule());
            if (sharing.get(part) > 1) part = part + "_" + partsSeen.merge(part, 1, Integer::sum);
            Map<String, Integer> perKind = new HashMap<>(), kindsSeen = new HashMap<>();
            for (DrlPiece.DrlFunction f : p.functions()) {
                Matcher m = NAME.matcher(f.name());
                if (m.matches()) perKind.merge(m.group(1), 1, Integer::sum);
            }
            for (DrlPiece.DrlFunction f : p.functions()) {
                String name = f.name(), where = which + " | " + p.rule() + " | " + name;
                names.add(name);
                Matcher m = NAME.matcher(name);
                if (!m.matches() || !KINDS.contains(m.group(1))) {
                    outsideTheForm.add(where + ": begint niet met een soort en een _");
                    continue;
                }
                String kind = m.group(1);
                boolean numbered = perKind.get(kind) > 1;
                if (numbered) counts.merge("functienamen met een nummer van hun soort" + suffix, 1, Integer::sum);
                String expected = kind + (numbered ? String.valueOf(kindsSeen.merge(kind, 1, Integer::sum)) : "") + "_"
                        + part;
                if (!name.equals(expected)) outsideTheForm.add(where + ": verwacht " + expected);
                if (globals.contains(name)) outsideTheForm.add(where + ": heet als een global");
                if (WORDS.contains(name) || SourceVersion.isKeyword(name))
                    outsideTheForm.add(where + ": is een woord van DRL, MVEL of Java");
            }
        }
        for (String line : text.split("\n")) {
            if (line.strip().startsWith("//")) continue;
            String code = withoutStrings(line);
            for (Matcher m = WORD.matcher(code); m.find(); )
                if (names.contains(m.group(1)) && (m.end() == code.length() || code.charAt(m.end()) != '('))
                    outsideTheForm.add(which + " | " + m.group(1) + ": staat in de tekst anders dan als declaratie of"
                            + " aanroep: " + head(line.strip()));
        }
    }

    /** The rule part of the names of the functions of rule {@code rule}: {@code Namen.ident} of its name without the
     *  code points Drools does not read in a name ({@link #namePart}), {@code x} where none remains. */
    static String rulePart(String rule) {
        StringBuilder b = new StringBuilder();
        Namen.ident(rule, false).codePoints().filter(DrlPiecesCorpusWitnessTest::namePart).forEach(b::appendCodePoint);
        return b.isEmpty() ? "x" : b.toString();
    }

    /** Whether Drools reads code point {@code cp} in a name: its DRL lexer reads {@code f} followed by it as one name,
     *  and Java takes it in an identifier. */
    static boolean namePart(int cp) {
        if (!Character.isJavaIdentifierPart(cp)) return false;
        String name = "f" + Character.toString(cp);
        DRL6Lexer lexer = new DRL6Lexer(new ANTLRStringStream(name));
        Token first = lexer.nextToken();
        return first.getType() == DRL6Lexer.ID && first.getText().equals(name)
                && lexer.nextToken().getType() == Token.EOF && lexer.getErrors().isEmpty();
    }

    /** {@code line} with the characters of its string literals blanked, so that a name in one is not read as code. */
    private static String withoutStrings(String line) {
        StringBuilder out = new StringBuilder(line);
        boolean in = false;
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c == '"') { in = !in; continue; }
            if (!in) continue;
            if (c == '\\' && i + 1 < out.length()) out.setCharAt(i++, ' ');
            out.setCharAt(i, ' ');
        }
        return out.toString();
    }

    private static Set<String> words() {
        Set<String> out = new HashSet<>();
        try {
            for (Field f : DroolsSoftKeywords.class.getFields())
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) out.add((String) f.get(null));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        out.addAll(AbstractParser.OPERATORS.keySet());
        out.addAll(AbstractParser.LITERALS.keySet());
        return out;
    }

    /**
     * The checks of card drl-no-passthrough-functions on {@code function}, a function of a piece whose functions are
     * {@code ownFunctions}, read off its text: it does more than return the call of another function of its rule; it is
     * not the function of a composite over no member whose verdict is true, nor of a composite over one member whose
     * verdict is that member's; and the locals {@code raak} and {@code vast} of a composite stand only where they are
     * read. The verdicts are those of {@link ComputeHelpers#established} and {@link ComputeHelpers#endVerdict}, the
     * runtime's own, which the evaluation of a composite calls. And the check of card drl-no-trivial-value-functions:
     * a function {@code name} of a kind of {@link #VALUE_KINDS} does more than return, within or without the standard
     * {@code try}, a constant, one of its parameters or {@code ComputeHelpers.collapse} of one of them.
     */
    private static void ownWork(String where, String name, String function, Set<String> ownFunctions, String suffix,
                                List<String> differences, Map<String, Integer> counts) {
        List<String> code = new ArrayList<>();
        String[] lines = function.split("\n");
        // the lines between the declaration and the closing brace, without the lines of a comment
        for (int i = 1; i < lines.length - 1; i++) if (!lines[i].strip().startsWith("//")) code.add(lines[i]);
        // the function of a value that only returns a constant, a parameter or collapse of one of them, within or
        // without the standard try (card drl-no-trivial-value-functions)
        List<String> returned = new ArrayList<>();
        for (String line : code) if (!VALUE_FRAME.contains(line.strip())) returned.add(line.strip());
        Matcher d = DECLARATION_WITH_PARAMETERS.matcher(lines[0]);
        if (returned.size() == 1 && returned.get(0).startsWith("return ") && returned.get(0).endsWith(";") && d.matches()) {
            Set<String> parameters = new HashSet<>();
            for (String p : d.group(1).split(", ")) if (!p.isEmpty()) parameters.add(p.substring(p.lastIndexOf(' ') + 1));
            String e = returned.get(0).substring("return ".length(), returned.get(0).length() - 1);
            if (computesNothing(e, parameters)) {
                Matcher kind = NAME.matcher(name);
                if (kind.matches() && VALUE_KINDS.contains(kind.group(1))) {
                    counts.merge("functies van een waarde die niets berekent" + suffix, 1, Integer::sum);
                    differences.add(where + ": geeft alleen " + e + " terug");
                } else {
                    counts.merge("functies van een andere soort die niets berekenen" + suffix, 1, Integer::sum);
                }
            }
        }
        if (code.size() == 1) {
            Matcher m = RETURNED_CALL.matcher(code.get(0));
            if (m.matches() && ownFunctions.contains(m.group(1)) && closes(m.group(2))) {
                counts.merge("functies die alleen een functie van hun stuk aanroepen" + suffix, 1, Integer::sum);
                differences.add(where + ": geeft alleen de aanroep van " + m.group(1) + " terug");
            }
        }
        List<String> verdict = new ArrayList<>();
        for (String line : code) if (!FRAME.contains(line.strip())) verdict.add(line);
        Matcher none = verdict.size() == 1 ? VERDICT_OVER_NONE.matcher(verdict.get(0)) : null;
        if (none != null && none.matches() && ComputeHelpers.endVerdict(Condition.Quantifier.valueOf(none.group(1)),
                Integer.parseInt(none.group(2)), 0, 0)) {
            counts.merge("functies van een samengestelde voorwaarde zonder leden met oordeel true" + suffix, 1,
                    Integer::sum);
            differences.add(where + ": is de functie van een samengestelde voorwaarde zonder leden met oordeel true");
        }
        String all = String.join("\n", code);
        Matcher end = END_VERDICT.matcher(all);
        if (end.find() && end.group(4).equals("1")) {
            Condition.Quantifier q = Condition.Quantifier.valueOf(end.group(1));
            int threshold = Integer.parseInt(end.group(2));
            boolean memberVerdict = ComputeHelpers.established(q, threshold, 0, 0, 1) == null
                    && !ComputeHelpers.endVerdict(q, threshold, 0, 1) && ComputeHelpers.endVerdict(q, threshold, 1, 1);
            if (memberVerdict) {
                counts.merge("functies van een samengestelde voorwaarde met één lid dat haar oordeel geeft" + suffix,
                        1, Integer::sum);
                differences.add(where + ": is de functie van een samengestelde voorwaarde met één lid dat haar"
                        + " oordeel geeft (" + q + " " + threshold + ")");
            }
        }
        boolean raakUnread = code.stream().anyMatch(l -> l.strip().equals("int raak = 0;"))
                && code.stream().filter(l -> l.contains("raak")).count() < 2;
        boolean vastUnread = code.stream().anyMatch(l -> l.strip().equals("Boolean vast;"))
                && code.stream().noneMatch(l -> l.contains("vast != null"));
        if (raakUnread || vastUnread) {
            counts.merge("functies met een local die niet gelezen wordt" + suffix, 1, Integer::sum);
            differences.add(where + ": declareert " + (raakUnread ? "raak" : "vast") + " en leest het niet");
        }
    }

    /** The declaration of a function and its parameters. */
    private static final Pattern DECLARATION_WITH_PARAMETERS = Pattern.compile("function \\S+ [^\\s(]+\\((.*)\\) \\{");
    /** A variable a binding binds. */
    private static final Pattern VARIABLE = Pattern.compile("\\$[\\p{L}\\p{N}_]+");

    /** Whether expression {@code e} computes nothing: {@code ComputeHelpers.collapse} of, or else, a constant or one of
     *  {@code names}. */
    private static boolean computesNothing(String e, Set<String> names) {
        Matcher m = NOTHING_COMPUTED.matcher(e);
        if (!m.matches()) return false;
        String inner = m.group(1) != null ? m.group(1) : m.group(2);
        return names.contains(inner) || CONSTANT.matcher(inner).matches();
    }

    /**
     * The value of a consequence that computes nothing, which the consequence carries itself (card
     * drl-no-trivial-value-functions): per line {@code Object v = …;} of {@code rule} whose expression is a constant, a
     * variable a binding of the rule binds, or {@code collapse} of one of them, a count, and one beside it where the line
     * before it is a comment, the sentence of the term; such a consequence asks no evaluation fault of {@code v}, since
     * its expression cannot fault.
     */
    private static void carriedValue(String where, String rule, String suffix, List<String> differences,
                                     Map<String, Integer> counts) {
        String[] parts = rule.split("\nthen\n", 2);
        Set<String> variables = new HashSet<>();
        for (Matcher m = VARIABLE.matcher(parts[0]); m.find(); ) variables.add(m.group());
        String[] lines = parts[1].split("\n");
        for (int i = 0; i < lines.length; i++) {
            Matcher m = CONSEQUENCE_VALUE.matcher(lines[i]);
            if (!m.matches() || !computesNothing(m.group(1), variables)) continue;
            counts.merge("gevolgen die hun waarde dragen" + suffix, 1, Integer::sum);
            if (i > 0 && lines[i - 1].startsWith("    // "))
                counts.merge("gevolgen die hun waarde dragen, met de zin erboven" + suffix, 1, Integer::sum);
            if (parts[1].contains("ComputeHelpers.isEvaluationFault(v)"))
                differences.add(where + ": draagt zijn waarde " + m.group(1) + " en toetst haar op een fout");
        }
    }

    /** Whether {@code arguments}, the text after the opening parenthesis of a call up to the end of its expression,
     *  closes that call only at its end, so that the call is the whole expression. */
    private static boolean closes(String arguments) {
        int depth = 1;
        for (int i = 0; i < arguments.length(); i++) {
            char c = arguments.charAt(i);
            if (c == '"') {
                // a string literal, whose parentheses are no call's
                for (i++; i < arguments.length() && arguments.charAt(i) != '"'; i++)
                    if (arguments.charAt(i) == '\\') i++;
                continue;
            }
            if (c == '(') depth++;
            if (c == ')' && --depth == 0) return i == arguments.length() - 1;
        }
        return false;
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
