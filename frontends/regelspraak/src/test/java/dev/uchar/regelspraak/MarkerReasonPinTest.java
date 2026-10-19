package dev.uchar.regelspraak;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The clamp on the {@code reden == null} convention (maintainer decision 2026-09-19, lens round of the
 * same date).
 *
 * <p><b>What it guards.</b> Since the card, every {@code Unresolved} carries a {@code reason} and the
 * resolver's survivor sweep reports each surviving marker whose reden is non-null. A reden of null means
 * "the marker's diagnosis is raised elsewhere" — by the producing route itself, by its caller, or by the
 * route that replaces it. The compiler forces every NEW production site to pass SOMETHING; it cannot
 * force that something to be a reason. The lens measured the hole: mutating one site's reden to null
 * drops exactly one of the suite's tests and leaves everything else green, so a future site that writes
 * null would be silent again — the very class this card closed.
 *
 * <p><b>Why a source pin and not a runtime heuristic.</b> The alternative was a runtime clamp: a
 * surviving null-reden marker in a model whose diagnostics list is EMPTY is a proven silence, so raise a
 * generic WARNING. That is cheap but heuristic — one loud diagnostic anywhere else in the document
 * masks the silence, and a document with a syntax error masks all of them. This pin is exact instead,
 * and it follows the house pattern of the gate scripts: a reason-carrying list of sanctioned entries
 * that fails when a new one appears AND when a listed one disappears. The cost is maintenance, and that
 * cost is the point — adding a null is then a deliberate act with a written reason, exactly as raising a
 * gate floor is.
 *
 * <p><b>How it reads the source.</b> Line numbers drift, so nothing here pins one: the unit is
 * (file, enclosing method, count). String literals and line comments are blanked before scanning, since
 * the word "onopgelost" occurs in the prose of several diagnostics.
 */
class MarkerReasonPinTest {

    /** One sanctioned family of {@code reden == null} constructions: all of them in {@code method} of
     *  {@code file}, with the reason that route needs no survivor report. */
    record Site(String file, String method, int count, String why) {}

    /** The fourteen sanctioned null-constructions, measured 2026-09-19, grown by one 2026-09-20. Growth
     *  is NOT free here (unlike
     *  a gate floor): a null is a promise that something else speaks, so every new one is listed with
     *  the diagnostic that keeps that promise. */
    static final List<Site> SANCTIONED = List.of(
            new Site("RuleChecker.java", "checkWholePeriod", 1,
                    "de §8.2-ERROR die de regel erboven opwerpt IS de diagnose van deze marker"),
            new Site("RuleChecker.java", "dayKindOperandError", 1,
                    "meldPredicaat werpt de Tabel-57-ERROR op; die is de diagnose van deze marker"),
            new Site("ExpressionBuilder.java", "buildFunction", 3,
                    "alle drie de markers staan direct onder hun eigen melding — de aggregatie over een "
                    + "ongedeclareerd lid, de niet-omgezette dimensieselectie / 'gedurende de tijd dat', "
                    + "en (2026-09-20) de WF-037-ERROR op een niet-set bron onder 'het aantal gevulde "
                    + "waarden van'"),
            new Site("ExpressionBuilder.java", "checkFoldSource", 4,
                    "elk van de vier vouwbron-weigeringen zet haar eigen WARNING neer vlak voor de marker"),
            new Site("ExpressionBuilder.java", "aggregationFilter", 3,
                    "de enige aanroeper (bouwFunctie) toetst het resultaat op Onopgelost en waarschuwt; "
                    + "de sweep zou dat herhalen"),
            new Site("ExpressionBuilder.java", "checkOrdinal", 2,
                    "de §5.5.3-ERROR respectievelijk -WARNING staat vlak boven de marker"));

    /** A marker construction: the two record constructors and the two {@code unresolvedMarker(…)} helpers. */
    private static final Pattern BUILD =
            Pattern.compile("\\b(?:new\\s+(?:Condition|Expr)\\.Unresolved|unresolvedMarker)\\s*\\(");

    /** A class-member declaration: a non-blank line at member indentation whose first identifier before
     *  a '(' names it. Deeper constructs (if/for/switch) sit at 8 spaces or more and never match. */
    private static final Pattern MEMBER = Pattern.compile("^ {4}\\S.*?\\b(\\w+)\\s*\\(");

    /** A floor on what the scanner sees, so a regex that stops matching cannot pass in silence — the
     *  same reasoning the gate scripts' EXPECTED_MIN constants carry. Measured 2026-09-19: 32. */
    private static final int MINIMAL_COUNT_BUILD_SITES = 30;

    @Test void every_null_reason_is_on_the_sanctioned_list() throws IOException {
        Path root = Path.of("src/main/java");
        assertTrue(Files.isDirectory(root),
                () -> "de bronboom " + root.toAbsolutePath() + " is er niet — deze test moet vanuit "
                        + "frontends/regelspraak draaien (zo draait `make test` hem)");

        Map<String, Integer> found = new TreeMap<>();
        List<String> emptinessErrors = new ArrayList<>();
        int buildSites = 0;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path p : paths.filter(x -> x.toString().endsWith(".java")).sorted().toList()) {
                String raw = Files.readString(p, StandardCharsets.UTF_8);
                String clean = withoutTextAndComment(raw);
                String[] rules = raw.split("\n", -1);
                int[] start = lineStarts(rules);
                Matcher m = BUILD.matcher(clean);
                while (m.find()) {
                    int rule = lineOf(start, m.start());
                    if (rules[rule].contains("private Expr unresolvedMarker")) continue;   // the helper itself
                    buildSites++;
                    String last = lastArgument(clean, m.end() - 1);
                    if (last.isEmpty() || "\"\"".equals(last))
                        emptinessErrors.add(p.getFileName() + ":" + (rule + 1) + " draagt een lege reden");
                    if (!"null".equals(last)) continue;
                    found.merge(p.getFileName() + "#" + enclosingMember(rules, rule), 1, Integer::sum);
                }
            }
        }

        assertTrue(emptinessErrors.isEmpty(),
                () -> "een lege reden is een stille marker met extra stappen: " + emptinessErrors);
        int gemeten = buildSites;
        assertTrue(gemeten >= MINIMAL_COUNT_BUILD_SITES,
                () -> "de scanner vond maar " + gemeten + " markerconstructies (vloer "
                        + MINIMAL_COUNT_BUILD_SITES + ") — de patronen sluiten niet meer aan op de bron, "
                        + "en dan keurt deze klem alles goed zonder iets te lezen");

        Map<String, Integer> expected = new LinkedHashMap<>();
        Map<String, String> why = new LinkedHashMap<>();
        for (Site p : SANCTIONED) {
            expected.put(p.file() + "#" + p.method(), p.count());
            why.put(p.file() + "#" + p.method(), p.why());
        }

        List<String> fresh = found.keySet().stream().filter(k -> !expected.containsKey(k)).toList();
        assertTrue(fresh.isEmpty(),
                () -> "nieuwe reden==null-plek(ken) zonder vermelding: " + fresh + ". Een null belooft dat "
                        + "een andere melding deze marker dekt; zet die belofte in GESANCTIONEERD of geef "
                        + "de marker een reden.");

        List<String> missing = expected.keySet().stream().filter(k -> !found.containsKey(k)).toList();
        assertTrue(missing.isEmpty(),
                () -> "de lijst is verouderd — deze plek(ken) bouwen geen null-marker meer: " + missing);

        for (var e : expected.entrySet())
            assertEquals(e.getValue(), found.get(e.getKey()),
                    () -> e.getKey() + " telt " + found.get(e.getKey()) + " null-markers in plaats van "
                            + e.getValue() + " — gesanctioneerd omdat: " + why.get(e.getKey()));
    }

    // ---------------- source scanning ----------------

    /** {@code source} with every string literal and line comment blanked to spaces (offsets preserved):
     *  several diagnostic texts contain the word "onopgelost" followed by a '(' section reference, and a
     *  scanner that read those would attribute a construction to a sentence. */
    private static String withoutTextAndComment(String source) {
        char[] result = source.toCharArray();
        for (int i = 0; i < result.length; ) {
            if (result[i] == '"') {
                int j = i + 1;
                while (j < result.length && !(result[j] == '"' && result[j - 1] != '\\')) result[j++] = ' ';
                i = j + 1;
            } else if (result[i] == '/' && i + 1 < result.length && result[i + 1] == '/') {
                int j = i;
                while (j < result.length && result[j] != '\n') result[j++] = ' ';
                i = j;
            } else i++;
        }
        return new String(result);
    }

    /** The last argument of the call whose '(' sits at {@code bracket}, trimmed — parenthesis-balanced,
     *  so a nested {@code source(f)} does not end the scan. */
    private static String lastArgument(String s, int bracket) {
        int depth = 0, i = bracket, lastComma = bracket;
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { if (--depth == 0) break; }
            else if (c == ',' && depth == 1) lastComma = i;
        }
        assertTrue(depth == 0, () -> "ongebalanceerde haakjes vanaf positie " + bracket);
        return s.substring(lastComma == bracket ? bracket + 1 : lastComma + 1, i).trim();
    }

    /** The name of the class member that encloses {@code rule} — the nearest declaration above it. */
    private static String enclosingMember(String[] rules, int rule) {
        for (int k = rule; k >= 0; k--) {
            Matcher m = MEMBER.matcher(rules[k]);
            if (m.find()) return m.group(1);
        }
        return "<buiten een lid>";
    }

    private static int[] lineStarts(String[] rules) {
        int[] start = new int[rules.length];
        for (int i = 1, off = rules[0].length() + 1; i < rules.length; off += rules[i].length() + 1, i++)
            start[i] = off;
        return start;
    }

    private static int lineOf(int[] start, int position) {
        int lo = 0, hi = start.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (start[mid] <= position) lo = mid; else hi = mid - 1;
        }
        return lo;
    }
}
