package dev.uchar.regelspraak;

import dev.uchar.regelspraak.parser.RegelSpraakLexer;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.SemanticTokens;
import dev.uchar.regelspraak.resolver.SemanticTokens.Kind;
import dev.uchar.regelspraak.resolver.SemanticTokens.Span;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The semantic tokens of the TOKA casus (frontends/regelspraak/grammar/DIVERGENCES.md, "Semantic tokens"): the 47
 * fragments {@code toka/casus/toka-*.rgs}, in name order, read as one document through {@link Resolver#tokens}. The
 * corpus witness of the frontend suite ({@code SemanticTokensCorpusTest}) cannot read them, because gate 1 keeps
 * {@code frontends/} free of this material; this witness stands in the parity zone, where gate 11 runs it.
 *
 * <p>It holds what the corpus witness holds. Every token of the lexer, on the default channel and on the hidden one,
 * lies in exactly one span, apart from the whitespace at its edges (a decision-table row, one token that the frontend
 * splits into its cells, has each of its characters outside whitespace in a span); spans do not overlap and carry no
 * whitespace at their edges; the text of each span is the source text at its position, in UTF-16 columns; a
 * declaration names something and points nowhere; every reference points at a span that is a declaration of the same
 * kind, and every reference whose kind has declarations carries one. Gate 11 compiles only the frontend's main
 * classes, so the frontend's own check ({@code SemanticTokensCheck}, a test class) is not on its class path: the checks
 * are written here again, and the tokens are lexed here again over the chain the frontend reads (each text followed by
 * a line end), so the check does not share the frontend's reading of them.
 *
 * <p>The casus is a document with diagnostics, and it writes names it declares nowhere: the corpus witness's rule "no
 * span {@code unresolved}" does not hold for it. Instead the spans {@code unresolved} are exactly those that
 * {@link #UNDECLARED} pins, per name with its reason. The list is stale-guarded in both directions: a span
 * {@code unresolved} outside it fails, and so does an entry whose name no longer stands in its fragment or no longer
 * leaves exactly its words unresolved. All problems are gathered before the test fails, so one run names every one.
 */
class SemanticTokensCasusTest {

    /** A name the fragments write and declare nowhere: the fragments it stands in (by their number), the name as they
     *  write it (its words, with any whitespace between them), the words of each occurrence that come out
     *  {@code unresolved}, in order, and why. */
    private record Undeclared(List<String> fragments, String written, List<String> words, String reason) {}

    /** Measured 2026-10-22: 24 spans {@code unresolved}, from six names. Each is a member that every replay slice under
     *  toka/cases declares (29 of 29, the scenario test model of the original toolchain) and no fragment does. */
    private static final List<Undeclared> UNDECLARED = List.of(
            new Undeclared(List.of("toka-22"), "daadwerkelijke datum-tijd van vertrek",
                    List.of("daadwerkelijke", "datum-tijd", "vertrek"),
                    "an attribute of Vlucht; toka-02 declares 'verwachte datum-tijd van vertrek' and no "
                            + "'daadwerkelijke' one. The resolution reads it without a diagnostic (DIVERGENCES.md, "
                            + "\"An undeclared attribute read\")."),
            new Undeclared(List.of("toka-26"), "in het hoogseizoen", List.of("hoogseizoen"),
                    "a characteristic of Vlucht that toka-26 assigns; toka-02 declares the characteristics of Vlucht, "
                            + "and this one is not among them."),
            new Undeclared(List.of("toka-32", "toka-33", "toka-34", "toka-35", "toka-36", "toka-37"),
                    "de treinmiles van alle passagiers", List.of("treinmiles"),
                    "the attribute 'treinmiles' of Natuurlijk persoon, the target of the allocation; toka-01 declares "
                            + "'treinmiles op basis van evenredige verdeling' and no 'treinmiles'."),
            new Undeclared(List.of("toka-35", "toka-37"), "het maximaal aantal te ontvangen treinmiles",
                    List.of("maximaal", "te", "ontvangen", "treinmiles"),
                    "the attribute 'maximaal aantal te ontvangen treinmiles' of Natuurlijk persoon, the maximum of the "
                            + "allocation; toka-01 declares 'maximaal te ontvangen treinmiles bij evenredige verdeling "
                            + "volgens rangorde'. 'aantal' stays a keyword."),
            new Undeclared(List.of("toka-41", "toka-45"), "passagier jonger dan 18 jaar", List.of("jonger"),
                    "a characteristic of Natuurlijk persoon; toka-01 declares 'passagier van 18 tot en met 24 jaar', "
                            + "'passagier van 25 tot en met 64 jaar' and 'passagier van 65 jaar of ouder'. 'passagier' "
                            + "names the role of toka-08. The resolution leaves a marker with a WARNING (RGS-092)."),
            new Undeclared(List.of("toka-43", "toka-44"), "de datum van vertrek van de reis",
                    List.of("datum", "vertrek"),
                    "the attribute 'datum van vertrek' of Vlucht, the start of the variable period; toka-02 declares "
                            + "'vluchtdatum' and 'verwachte datum-tijd van vertrek'. The resolution reads it without a "
                            + "diagnostic (DIVERGENCES.md, \"An undeclared attribute read\")."));

    /** Floors, measured 2026-10-22: the fragments read, the spans written, and the name spans per kind. A floor
     *  catches shrink; a change that moves names from one kind to another lowers the floor it falls under in the same
     *  commit. The kinds the casus does not write (dimension, dimension label, vocabulary, rule group) carry none. */
    private static final int MIN_FRAGMENTS = 47;
    private static final int MIN_SPANS = 1667;
    private static final Map<Kind, Integer> MIN_NAMES = new EnumMap<>(Map.ofEntries(
            Map.entry(Kind.OBJECT_TYPE, 38), Map.entry(Kind.ATTRIBUTE, 93), Map.entry(Kind.CHARACTERISTIC, 26),
            Map.entry(Kind.ROLE, 75), Map.entry(Kind.FACT_TYPE, 4), Map.entry(Kind.PARAMETER, 32),
            Map.entry(Kind.DOMAIN, 14), Map.entry(Kind.ENUMERATION_VALUE, 4), Map.entry(Kind.UNIT_SYSTEM, 3),
            Map.entry(Kind.UNIT, 65), Map.entry(Kind.TIMELINE, 3), Map.entry(Kind.DAY_KIND, 3),
            Map.entry(Kind.NAMED_FORM, 31), Map.entry(Kind.VARIABLE, 12)));

    @Test void the_casus_is_covered_and_every_unresolved_name_is_pinned() throws IOException {
        Path dir = Path.of("toka/casus");
        assertTrue(Files.isDirectory(dir), () -> "toka/casus not found from " + Path.of("").toAbsolutePath()
                + "; gate 11 runs this test from the repository root, and it does not skip without the casus");
        List<SemanticTokens.SourceFile> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : s.filter(p -> p.getFileName().toString().matches("toka-.*\\.rgs")).sorted().toList())
                files.add(new SemanticTokens.SourceFile(p.getFileName().toString(),
                        Files.readString(p, StandardCharsets.UTF_8)));
        }
        SemanticTokens.Document d = Resolver.tokens(files);
        assertEquals(files.size(), d.files().size(), "one entry per fragment read");

        List<String> problems = new ArrayList<>();
        List<List<int[]>> tokens = lex(files, problems);
        List<Located> spans = new ArrayList<>();
        for (int f = 0; f < files.size(); f++) {
            SemanticTokens.FileSpans fs = d.files().get(f);
            if (!files.get(f).name().equals(fs.file())) {
                problems.add("entry " + f + " is " + fs.file() + ", not " + files.get(f).name());
                continue;
            }
            spans.addAll(cover(files.get(f).text(), fs, tokens.get(f), problems));
        }
        checkReferences(spans, problems);

        Map<Kind, Integer> names = new EnumMap<>(Kind.class);
        List<Located> unresolved = new ArrayList<>();
        for (Located l : spans) {
            Span s = l.span();
            if (s.kind().names()) names.merge(s.kind(), 1, Integer::sum);
            if (s.kind().names() && !s.declaration() && s.declaredAt() == null && s.generatedFrom() == null
                    && s.kind() != Kind.UNIT)
                problems.add(l + ": a " + s.kind().wire() + " without its declaration");
            if (s.kind() == Kind.WORD)
                problems.add(l + ": a word outside every name, which only ANTLR's recovery after a syntax error leaves,"
                        + " and the casus has none");
            if (s.kind() == Kind.UNRESOLVED) unresolved.add(l);
        }
        checkUndeclared(files, unresolved, problems);

        System.out.println("semantic tokens over the TOKA casus: fragments=" + files.size() + " spans=" + spans.size()
                + " unresolved=" + unresolved.size() + " diagnostics=" + d.diagnostics().size() + " names=" + names);
        assertTrue(problems.isEmpty(), () -> problems.size() + " problem(s):\n  " + String.join("\n  ", problems));
        assertTrue(files.size() >= MIN_FRAGMENTS, () -> "fragments read " + files.size() + " < floor " + MIN_FRAGMENTS);
        assertTrue(spans.size() >= MIN_SPANS, () -> "spans " + spans.size() + " < floor " + MIN_SPANS);
        for (var floor : MIN_NAMES.entrySet()) {
            int n = names.getOrDefault(floor.getKey(), 0);
            assertTrue(n >= floor.getValue(),
                    () -> "name spans " + floor.getKey().wire() + " " + n + " < floor " + floor.getValue());
        }
    }

    /** A span with the file it stands in and its offsets in that file's text. */
    private record Located(String file, Span span, int start, int end) {
        @Override public String toString() {
            return file + ":" + span.line() + ":" + span.column() + " '" + span.text() + "'";
        }
    }

    /** The positions, the texts and the coverage of one file's spans; returns them located. */
    private static List<Located> cover(String text, SemanticTokens.FileSpans fs, List<int[]> tokens,
                                       List<String> problems) {
        int[] lineStart = lineStarts(text);
        int[] owner = new int[text.length()];
        Arrays.fill(owner, -1);
        List<Located> located = new ArrayList<>();
        int previousEnd = 0;
        for (Span s : fs.spans()) {
            String where = fs.file() + ":" + s.line() + ":" + s.column();
            int start = offset(lineStart, text, s.line(), s.column());
            int end = offset(lineStart, text, s.endLine(), s.endColumn());
            if (start < 0 || end < 0) { problems.add(where + ": a position outside the file"); continue; }
            if (start >= end) { problems.add(where + ": an empty span"); continue; }
            if (!text.substring(start, end).equals(s.text()))
                problems.add(where + ": the span says '" + s.text() + "', the source has '" + text.substring(start, end) + "'");
            if (Character.isWhitespace(text.charAt(start)) || Character.isWhitespace(text.charAt(end - 1)))
                problems.add(where + ": whitespace at the edge of the span");
            if (start < previousEnd) problems.add(where + ": the span overlaps the one before it or is out of order");
            previousEnd = Math.max(previousEnd, end);
            for (int k = start; k < end; k++) owner[k] = located.size();
            located.add(new Located(fs.file(), s, start, end));
        }
        for (int[] token : tokens) {
            for (int k = token[0]; k < token[1]; k++)
                if (!Character.isWhitespace(text.charAt(k)) && owner[k] < 0) {
                    problems.add("character '" + text.charAt(k) + "' at " + fs.file() + ":" + place(lineStart, k)
                            + " lies in no span");
                    break;
                }
            if (token[2] == RegelSpraakLexer.TABEL_RIJ) continue;
            for (int k = token[0]; k < token[1]; k++)
                if (owner[k] != owner[token[0]]) {
                    problems.add("the token '" + text.substring(token[0], token[1]) + "' at " + fs.file() + ":"
                            + place(lineStart, token[0]) + " lies in more than one span");
                    break;
                }
        }
        return located;
    }

    /** A declaration names something and points nowhere; a reference and a generated name point at a declaration
     *  span, a reference at one of its own kind. */
    private static void checkReferences(List<Located> spans, List<String> problems) {
        List<Located> declarations = spans.stream().filter(l -> l.span().declaration()).toList();
        for (Located l : spans) {
            Span s = l.span();
            if (s.declaration()) {
                if (!s.kind().names()) problems.add(l + ": a declaration of kind " + s.kind().wire());
                if (s.declaredAt() != null) problems.add(l + ": a declaration that points elsewhere");
                continue;
            }
            if (s.generatedFrom() != null && !declarationAt(declarations, s.generatedFrom(), null))
                problems.add(l + ": generated from " + s.generatedFrom() + ", where no declaration stands");
            if (s.declaredAt() == null) continue;
            if (!s.kind().names()) problems.add(l + ": a span of kind " + s.kind().wire() + " points at a declaration");
            else if (!declarationAt(declarations, s.declaredAt(), s.kind()))
                problems.add(l + ": points at " + s.declaredAt() + ", where no declaration of kind " + s.kind().wire()
                        + " stands");
        }
    }

    private static boolean declarationAt(List<Located> declarations, SemanticTokens.Location at, Kind kind) {
        for (Located decl : declarations)
            if (decl.file().equals(at.file()) && decl.span().line() == at.line() && decl.span().column() == at.column()
                    && (kind == null || decl.span().kind() == kind)) return true;
        return false;
    }

    /** The spans {@code unresolved} against {@link #UNDECLARED}: each lies in an occurrence of a pinned name in its
     *  fragment, and each occurrence of a pinned name in a pinned fragment leaves exactly the pinned words. */
    private static void checkUndeclared(List<SemanticTokens.SourceFile> files, List<Located> unresolved,
                                        List<String> problems) {
        List<Located> claimed = new ArrayList<>();
        for (Undeclared u : UNDECLARED)
            for (String fragment : u.fragments()) {
                List<SemanticTokens.SourceFile> matches = files.stream()
                        .filter(f -> f.name().startsWith(fragment + "-")).toList();
                if (matches.size() != 1) {
                    problems.add("UNDECLARED is stale: '" + u.written() + "' names fragment " + fragment + ", which "
                            + matches.size() + " files carry");
                    continue;
                }
                String file = matches.get(0).name();
                Matcher m = pattern(u.written()).matcher(matches.get(0).text());
                int occurrences = 0;
                while (m.find()) {
                    occurrences++;
                    int from = m.start(), to = m.end();
                    List<Located> inside = unresolved.stream()
                            .filter(l -> l.file().equals(file) && l.start() >= from && l.end() <= to).toList();
                    List<String> words = inside.stream().map(l -> l.span().text()).toList();
                    if (!words.equals(u.words()))
                        problems.add("UNDECLARED is stale: '" + u.written() + "' in " + file + " at offset " + from
                                + " leaves " + words + " unresolved, the list pins " + u.words());
                    claimed.addAll(inside);
                }
                if (occurrences == 0)
                    problems.add("UNDECLARED is stale: '" + u.written() + "' does not stand in " + file);
            }
        for (Located l : unresolved)
            if (!claimed.contains(l))
                problems.add(l + ": unresolved, and no name of UNDECLARED in its fragment covers it");
    }

    /** The written name as a pattern: its words in order, any whitespace between them, no letter or digit around. */
    private static Pattern pattern(String written) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])"
                + Arrays.stream(written.split(" ")).map(Pattern::quote).collect(Collectors.joining("\\s+"))
                + "(?![\\p{L}\\p{N}])");
    }

    /** The tokens of each file as {start, end, type}, UTF-16 offsets into its own text without the whitespace at the
     *  edges, lexed once over the chain the frontend reads (each text followed by a line end). */
    private static List<List<int[]>> lex(List<SemanticTokens.SourceFile> files, List<String> problems) {
        StringBuilder chain = new StringBuilder();
        int[] from = new int[files.size() + 1];
        for (int i = 0; i < files.size(); i++) {
            from[i] = chain.length();
            chain.append(files.get(i).text()).append('\n');
        }
        from[files.size()] = chain.length();
        String text = chain.toString();
        int[] offset = new int[text.codePointCount(0, text.length()) + 1];
        for (int cp = 0, i = 0; ; cp++) {
            offset[cp] = i;
            if (i >= text.length()) break;
            i += Character.charCount(text.codePointAt(i));
        }
        List<List<int[]>> result = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) result.add(new ArrayList<>());
        RegelSpraakLexer lexer = new RegelSpraakLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        for (Token t : lexer.getAllTokens()) {
            int s = offset[t.getStartIndex()], e = offset[t.getStopIndex() + 1];
            while (s < e && Character.isWhitespace(text.charAt(s))) s++;
            while (e > s && Character.isWhitespace(text.charAt(e - 1))) e--;
            if (s >= e) continue;
            int f = 0;
            while (from[f + 1] <= s) f++;
            if (e > from[f] + files.get(f).text().length()) {
                problems.add("the token '" + text.substring(s, e) + "' runs past the end of " + files.get(f).name());
                continue;
            }
            result.get(f).add(new int[]{s - from[f], e - from[f], t.getType()});
        }
        return result;
    }

    private static int[] lineStarts(String text) {
        List<Integer> starts = new ArrayList<>(List.of(0));
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') starts.add(i + 1);
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The offset of a line (from 1) and a UTF-16 column (from 0), or -1 when the place is not in the text. */
    private static int offset(int[] lineStart, String text, int line, int column) {
        if (line < 1 || line > lineStart.length || column < 0) return -1;
        int o = lineStart[line - 1] + column;
        return o <= text.length() ? o : -1;
    }

    private static String place(int[] lineStart, int offset) {
        int line = 1;
        while (line < lineStart.length && lineStart[line] <= offset) line++;
        return line + ":" + (offset - lineStart[line - 1]);
    }
}
