package dev.uchar.regelspraak.resolver;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * One sentence per defect: where a refusal of this frontend speaks English, it is the verifier's sentence for the same
 * defect, copied letter for letter after a Dutch context prefix (grammar/DIVERGENCES.md, "Producer refusals without a
 * reader sentence"). A copy drifts when the verifier rewords a sentence and the frontend keeps the old words, and the
 * refusal still refuses, so no other test sees it: two date-arithmetic sentences drifted that way before this test
 * existed. This test reads both sources and requires every English fragment of the frontend's refusals to stand in the
 * verifier's string literals.
 *
 * <p><b>What it reads.</b> The frontend's side is every string literal of the resolver and of the emitter, the two
 * places refusals are written, outside the text of an exception and outside a key of the catalogue, and the English
 * catalogue's entries of the codes in
 * {@link #VERIFIER_COPIES}. An exception's text guards an invariant of the frontend and is no refusal of a document; it
 * is English and the verifier has no sentence for it (card message-language, owner's decision 1 of 2026-10-19). The
 * Dutch catalogue is Dutch throughout (decision 2 of the same day, held by {@code scripts/check-rgs-codes.sh}), so the
 * verifier's sentence stands in the English entry of its code, after a context in this frontend's words: the number
 * after a key in {@link #VERIFIER_COPIES} counts the {@code ": "} segments of that context, which are left out. The
 * English of {@code ir/Spelling} is its tables' own consistency checks, thrown when the class loads and never a refusal
 * of a document. A literal or an entry is split at its format specifiers or places, a producer prefix is taken off (the
 * emitter's name, a WF arm tag, a pointer, a context segment before {@code ": "} that is not English by the rule below)
 * and a trailing {@code " — "} joiner, and what is left is English when it holds two words or more, a word of
 * {@link #ENGLISH} and no word of {@link #NEDERLANDS}. The verifier's side is every string literal of
 * {@code spec/verifier/lib}, escapes decoded and split at its Printf specifiers; a fragment passes when it stands in one
 * of those pieces, and every code of {@link #VERIFIER_COPIES} gives at least one fragment.
 *
 * <p><b>Its edges.</b> A sentence the verifier builds around a variable is written in the frontend with the same
 * template, so that its fixed pieces are the verifier's (Typering's clamp and keuzetype-domain sentences). An English
 * literal without a word of {@link #ENGLISH} escapes; the list holds the words of the sentences the frontend speaks,
 * measured when this test was written and again when the structural refusals took the verifier's sentences. The two
 * sources are found by walking up from the working directory, and a missing source FAILS, as in
 * {@link FoldKindsBoundaryTest}.
 */
class ReaderSentenceBoundaryTest {

    private static final String VERIFIER = "spec/verifier/lib";
    private static final String FRONTEND = "frontends/regelspraak/src/main/java/dev/uchar/regelspraak";

    /** Words that make a fragment English; none of them is a Dutch word. */
    private static final Set<String> ENGLISH = Set.of(("the must operand operands fold folds value values unit units "
            + "target date dates number numbers text precision precisions duration ordering equality assignment "
            + "arithmetic clamp base sign restriction literal source line snippet marker diagnostic severity qualifier "
            + "window bound quantifier threshold creation group timeline point counted characteristic initialisation "
            + "expression kind day time families family incompatible assigned written orders share order ill-formed "
            + "neither nor recognised declared without carries carry which and has with into under not comparable "
            + "calendar positions amounts parameter operator numeric unitless power additive whole "
            + "millisecond-precision day-precision sub-day recursive unguarded object fixed-start fewer two enumeration "
            + "domain label priority complemented element slot aggregation duplicate rules validity anchor "
            + "navigated").split(" "));

    /** Words that make a fragment Dutch; none of them is an English word. */
    private static final Set<String> NEDERLANDS = Set.of(("de het een van niet geen met voor bij naar zijn wordt moet "
            + "kan dat die als aan te om vereist kreeg heeft wel ook maar dus deze dit haar hij er nog zo op tot uit "
            + "worden omgezet regel regels onopgelost blijft komt koppelt").split(" "));

    /** The codes whose English entry carries the verifier's sentence for the same defect, measured on 2026-11-06 as
     *  the codes whose Dutch entry carried it until the card message-language; after a colon, the number of
     *  {@code ": "} segments of the context in front of it. */
    private static final String VERIFIER_COPIES = ("RGS-015:1 RGS-016:1 RGS-018 RGS-035 RGS-036 RGS-037 RGS-060 "
            + "RGS-066 RGS-067 RGS-071 RGS-077 RGS-078:1 RGS-079 RGS-080 "
            + "RGS-081.operand-parameter RGS-081.operand-position RGS-081.rationale-date RGS-081.rationale-other "
            + "RGS-081.sentence RGS-083:1 RGS-084.operand-sentence RGS-085:1 RGS-086 RGS-087 RGS-088 "
            + "RGS-098.duplicate-name RGS-098.name-collision RGS-102:1 RGS-103:1 RGS-104 RGS-106:1 RGS-107 RGS-108:1 "
            + "RGS-110:1 RGS-114:1 RGS-115:1 RGS-118:1 RGS-120.hop RGS-122:1 RGS-123:1 RGS-124:2 "
            + "RGS-125.neither-member RGS-125.reaches RGS-126:2 RGS-127:1 RGS-128.range-anchor RGS-128.range-element "
            + "RGS-128.range-written RGS-128.reads-own RGS-132:1 RGS-164:1 RGS-169.duplicate-name RGS-171 RGS-177 "
            + "RGS-178 RGS-183 RGS-189:1 RGS-192 RGS-193 RGS-194:1 RGS-196:1 RGS-197 RGS-197.negative-literal "
            + "RGS-198 RGS-200 RGS-201:1 RGS-202:1 RGS-202.computed RGS-202.navigated-read "
            + "RGS-202.no-subject RGS-202.parameter-bound RGS-202.time-dependent RGS-203:1 RGS-206 RGS-207 RGS-280 "
            + "RGS-283:1 RGS-285 RGS-286 RGS-288 RGS-289 RGS-290 RGS-291 RGS-292 RGS-295 RGS-296 RGS-297 RGS-298 "
            + "RGS-299 RGS-309:2 RGS-318 RGS-319 RGS-323.side RGS-324 RGS-325.side RGS-326 RGS-327.side RGS-328 "
            + "RGS-333 RGS-334 RGS-335 RGS-337 RGS-343 RGS-344.dates-of-two-precisions RGS-344.numbers-and-dates "
            + "RGS-344.operand-sentence");

    /** A floor on the fragments read, so an extractor that reads nothing is no silent green (measured: 99; 153 once
     *  the structural refusals spoke the verifier's sentences). */
    private static final int MIN_FRAGMENTS = 149;

    private static final Pattern WORD = Pattern.compile("[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\-']*");
    private static final Pattern SPECIFIER = Pattern.compile("%[sdS]");
    private static final Pattern PLACE = Pattern.compile("\\{[A-Za-z][A-Za-z0-9]*\\}");
    private static final String CATALOGUE =
            "frontends/regelspraak/src/main/resources/dev/uchar/regelspraak/message/messages-frontend.en.tsv";

    private static Path root() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent())
            if (Files.isDirectory(p.resolve(VERIFIER)) && Files.isDirectory(p.resolve(FRONTEND))) return p;
        return fail("kon " + VERIFIER + " en " + FRONTEND + " niet vinden vanaf " + Path.of("").toAbsolutePath()
                + " — deze getuige LEEST beide bronnen en mag niet overslaan als hij ze niet vindt");
    }

    /** A fragment, with the line of its literal or the code of its entry. */
    private record Fragment(String file, Object rule, String text) {}

    private static final String CATALOGUE_FILE = "messages-frontend.en.tsv";

    @Test void every_english_sentence_of_the_frontend_is_in_the_verifier() throws IOException {
        List<String> pieces = verifierPieces(root());
        List<String> faults = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int read = 0;
        for (Fragment f : frontendFragments(root())) {
            seen.add(f.file() + ":" + f.rule());
            read++;
            if (pieces.stream().noneMatch(s -> s.contains(f.text())))
                faults.add(f.file() + ":" + f.rule() + " '" + f.text() + "'");
        }
        assertTrue(faults.isEmpty(), () -> "Engelse frontendzinnen die niet (meer) in " + VERIFIER + " staan — de "
                + "verifier formuleerde ze anders, of de frontend schrijft een eigen Engelse zin:\n  "
                + String.join("\n  ", faults));
        int count = read;
        assertTrue(count >= MIN_FRAGMENTS, () -> "slechts " + count + " Engelse fragmenten gelezen, ondergrens "
                + MIN_FRAGMENTS + " — het uitlezen sluit niet meer aan op de bron");
        List<String> silent = new ArrayList<>();
        for (String entry : VERIFIER_COPIES.split(" ")) {
            String key = entry.split(":")[0];
            if (!seen.contains(CATALOGUE_FILE + ":" + key)) silent.add(key);
        }
        assertEquals(List.of(), silent, "codes van VERIFIER_COPIES waarvan de Engelse entry geen zin van de verifier draagt");
    }

    @Test void the_find_rule_refuses_an_outdated_sentence() throws IOException {
        // the two sentences the frontend kept after the verifier reworded them, and the words that replaced them
        List<String> pieces = verifierPieces(root());
        for (String oud : List.of("duration + date is ill-formed (the date stands on the left; v0 does not commute)",
                "' is not a recognised time unit or declared day kind"))
            assertTrue(pieces.stream().noneMatch(s -> s.contains(oud)), () -> "de oude zin staat nog in de verifier: " + oud);
        for (String fresh : List.of("duration + date is ill-formed (the date stands on the left; date arithmetic does "
                + "not commute)", "' is neither a recognised time unit nor a declared day kind"))
            assertTrue(pieces.stream().anyMatch(s -> s.contains(fresh)), () -> "de zin ontbreekt in de verifier: " + fresh);
    }

    @Test void the_sources_carry_no_form_that_the_scanner_does_not_read() throws IOException {
        Path w = root();
        for (Path p : frontendSources(w))
            assertFalse(Files.readString(p, UTF_8).contains("\"\"\""), () -> p + " draagt een tekstblok, dat deze "
                    + "getuige niet leest");
        for (Path p : verifierSources(w))
            assertFalse(Files.readString(p, UTF_8).contains("{|"), () -> p + " draagt een quoted string, die deze "
                    + "getuige niet leest");
    }

    // ---- the verifier's side ----

    private static List<Path> verifierSources(Path w) throws IOException {
        try (Stream<Path> s = Files.list(w.resolve(VERIFIER))) {
            List<Path> sources = s.filter(p -> p.toString().endsWith(".ml")).sorted().toList();
            assertFalse(sources.isEmpty(), "geen .ml-bronnen in " + VERIFIER);
            return sources;
        }
    }

    /** The fixed pieces of every string literal of the verifier, split at its Printf specifiers. */
    private static List<String> verifierPieces(Path w) throws IOException {
        List<String> pieces = new ArrayList<>();
        for (Path p : verifierSources(w))
            for (String literal : ocamlLiterals(Files.readString(p, UTF_8)))
                pieces.addAll(List.of(SPECIFIER.split(literal, -1)));
        return pieces;
    }

    /** OCaml string literals, comments (nesting) and character literals skipped, escapes decoded: {@code \xHH} bytes,
     *  {@code \n}, {@code \t}, an escaped quote or backslash, and a backslash-newline with the blanks after it. */
    static List<String> ocamlLiterals(String s) {
        List<String> result = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            if (s.startsWith("(*", i)) {
                int depth = 0;
                do {
                    if (s.startsWith("(*", i)) { depth++; i += 2; }
                    else if (s.startsWith("*)", i)) { depth--; i += 2; }
                    else i++;
                } while (depth > 0 && i < n);
                continue;
            }
            char c = s.charAt(i);
            if (c == '\'' && i + 2 < n && s.charAt(i + 2) == '\'') { i += 3; continue; }
            if (c == '\'' && i + 3 < n && s.charAt(i + 1) == '\\' && s.charAt(i + 3) == '\'') { i += 4; continue; }
            if (c != '"') { i++; continue; }
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            i++;
            while (i < n && s.charAt(i) != '"') {
                char d = s.charAt(i);
                if (d != '\\') { b.writeBytes(String.valueOf(d).getBytes(UTF_8)); i++; continue; }
                char e = s.charAt(i + 1);
                if (e == '\n') {
                    i += 2;
                    while (i < n && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
                } else if (e == 'x') {
                    b.write(Integer.parseInt(s.substring(i + 2, i + 4), 16));
                    i += 4;
                } else {
                    b.writeBytes((e == 'n' ? "\n" : e == 't' ? "\t" : String.valueOf(e)).getBytes(UTF_8));
                    i += 2;
                }
            }
            result.add(b.toString(UTF_8));
            i++;
        }
        return result;
    }

    // ---- the frontend's side ----

    private static List<Path> frontendSources(Path w) throws IOException {
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> s = Files.list(w.resolve(FRONTEND).resolve("resolver"))) {
            sources.addAll(s.filter(p -> p.toString().endsWith(".java")).sorted().toList());
        }
        sources.add(w.resolve(FRONTEND).resolve("emit/IrEmitter.java"));
        for (Path p : sources) assertTrue(Files.isRegularFile(p), () -> p + " ontbreekt");
        return sources;
    }

    private static List<Fragment> frontendFragments(Path w) throws IOException {
        List<Fragment> result = new ArrayList<>();
        for (Path p : frontendSources(w)) {
            String source = Files.readString(p, UTF_8);
            List<int[]> exceptions = exceptionTexts(source);
            for (int[] position : javaLiteralPositions(source)) {
                if (exceptions.stream().anyMatch(r -> r[0] <= position[0] && position[0] < r[1])) continue;
                if (CATALOGUE_KEY.matcher(source.substring(position[0], position[1])).matches()) continue;
                String literal = javaLiteral(source, position[0], position[1]);
                int rule = 1 + (int) source.substring(0, position[0]).chars().filter(ch -> ch == '\n').count();
                for (String part : SPECIFIER.split(literal, -1)) {
                    String core = core(part);
                    if (core != null) result.add(new Fragment(p.getFileName().toString(), rule, core));
                }
            }
        }
        // the sentences stand in the catalogue, not in the literals above: per code its English entry, its context left out
        Path catalogue = w.resolve(CATALOGUE);
        assertTrue(Files.isRegularFile(catalogue), () -> catalogue + " ontbreekt");
        Map<String, String> entries = new java.util.HashMap<>();
        for (String line : Files.readAllLines(catalogue, UTF_8))
            entries.put(line.substring(0, line.indexOf('\t')), line.substring(line.indexOf('\t') + 1));
        for (String entry : VERIFIER_COPIES.split(" ")) {
            String[] keyAndContext = entry.split(":");
            String key = keyAndContext[0];
            int context = keyAndContext.length == 1 ? 0 : Integer.parseInt(keyAndContext[1]);
            String template = entries.get(key);
            assertNotNull(template, () -> key + " staat niet in " + CATALOGUE);
            List<String> segments = List.of(template.replace("{{", "{").replace("}}", "}").split(": ", -1));
            assertTrue(context < segments.size(), () -> key + ": de context telt meer segmenten dan de entry heeft");
            for (String part : PLACE.split(String.join(": ", segments.subList(context, segments.size())), -1)) {
                String core = core(part);
                if (core != null) result.add(new Fragment(CATALOGUE_FILE, key, core));
            }
        }
        return result;
    }

    /** A key of the catalogue, or its beginning, which a site completes ({@code "PART.clamp." + which}): no sentence. */
    private static final Pattern CATALOGUE_KEY = Pattern.compile("(?:PART|FRAME|RGS-\\d{3})(?:\\.[a-z0-9-]+)*\\.?");

    private static final Pattern EXCEPTION_TEXT = Pattern.compile("\\bthrow\\b|\\bnew\\s+[A-Za-z_.]*(?:Exception|Error)\\s*\\(");

    /** Where the text of an exception stands: a throw statement up to its semicolon, and the arguments of the
     *  construction of an exception, which a method may return to be thrown elsewhere. A match inside a string or a
     *  comment is no start. */
    private static List<int[]> exceptionTexts(String s) {
        List<int[]> result = new ArrayList<>();
        List<int[]> skipped = new ArrayList<>(javaLiteralPositions(s));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') { i++; while (i < s.length() && s.charAt(i) != c) i += s.charAt(i) == '\\' ? 2 : 1; }
            else if (s.startsWith("//", i)) { int e = s.indexOf('\n', i); skipped.add(new int[]{i, e < 0 ? s.length() : e}); i = e < 0 ? s.length() : e; }
            else if (s.startsWith("/*", i)) { int e = s.indexOf("*/", i + 2); skipped.add(new int[]{i, e < 0 ? s.length() : e}); i = e < 0 ? s.length() : e + 1; }
        }
        Matcher m = EXCEPTION_TEXT.matcher(s);
        while (m.find()) {
            int start = m.start();
            if (skipped.stream().anyMatch(r -> r[0] <= start && start <= r[1])) continue;
            boolean statement = m.group().startsWith("throw");
            int j = statement ? m.end() : m.end() - 1, depth = 0;
            for (; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == '"' || c == '\'') { j++; while (s.charAt(j) != c) j += s.charAt(j) == '\\' ? 2 : 1; }
                else if (c == '(' || c == '[' || c == '{') depth++;
                else if (c == ')' || c == ']' || c == '}') { depth--; if (!statement && depth == 0) break; }
                else if (c == ';' && statement && depth <= 0) break;
            }
            result.add(new int[]{start, j});
        }
        return result;
    }

    /** Start and end (exclusive, the closing quote) of every string literal, comments and char literals skipped. */
    private static List<int[]> javaLiteralPositions(String s) {
        List<int[]> result = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            if (s.startsWith("//", i)) { while (i < n && s.charAt(i) != '\n') i++; continue; }
            if (s.startsWith("/*", i)) { int e = s.indexOf("*/", i + 2); i = e < 0 ? n : e + 2; continue; }
            char c = s.charAt(i);
            if (c == '\'') {
                i++;
                while (i < n && s.charAt(i) != '\'') i += s.charAt(i) == '\\' ? 2 : 1;
                i++;
                continue;
            }
            if (c != '"') { i++; continue; }
            int begin = ++i;
            while (i < n && s.charAt(i) != '"') i += s.charAt(i) == '\\' ? 2 : 1;
            result.add(new int[]{begin, i});
            i++;
        }
        return result;
    }

    /** The literal's text, its escapes decoded ({@code \}{@code uXXXX}, {@code \n}, {@code \t}, an escaped char). */
    private static String javaLiteral(String s, int begin, int end) {
        StringBuilder b = new StringBuilder();
        for (int i = begin; i < end; i++) {
            char c = s.charAt(i);
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(++i);
            if (e == 'u') { b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; }
            else b.append(e == 'n' ? '\n' : e == 't' ? '\t' : e);
        }
        return b.toString();
    }

    /** The English part of a literal piece, a producer prefix and a trailing joiner taken off; null where none is. */
    static String core(String part) {
        String r = part.replaceFirst("^IrEmitter: ", "").replaceFirst("^WF-\\d+ \\([a-z]\\)( —)? ?", "");
        List<String> segments = new ArrayList<>(List.of(r.split(": ", -1)));
        while (segments.size() > 1 && (segments.get(0).startsWith("/") || !english(segments.get(0))))
            segments.remove(0);
        r = String.join(": ", segments).replaceFirst(" — $", "");
        return english(r) ? r : null;
    }

    private static boolean english(String text) {
        List<String> words = new ArrayList<>();
        Matcher m = WORD.matcher(text);
        while (m.find()) words.add(m.group().toLowerCase(Locale.ROOT));
        return words.size() >= 2 && words.stream().noneMatch(NEDERLANDS::contains)
                && words.stream().anyMatch(ENGLISH::contains);
    }
}
