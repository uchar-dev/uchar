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
 * places refusals are written; the English of {@code ir/Spelling} is its tables' own consistency checks, thrown when
 * the class loads and never a refusal of a document. A literal is split at its format specifiers, a producer prefix is
 * taken off (the emitter's name, a WF arm tag, a pointer, a Dutch context segment before {@code ": "}) and a trailing
 * {@code " — "} joiner, and what is left is English when it holds two words or more, a word of {@link #ENGELS} and no
 * word of {@link #NEDERLANDS}. The verifier's side is every string literal of {@code spec/verifier/lib}, escapes
 * decoded and split at its Printf specifiers; a fragment passes when it stands in one of those pieces.
 *
 * <p><b>Its edges.</b> A sentence the verifier builds around a variable is written in the frontend with the same
 * template, so that its fixed pieces are the verifier's (Typering's clamp and keuzetype-domain sentences). An English
 * literal without a word of {@link #ENGELS} escapes; the list holds the words of the sentences the frontend speaks,
 * measured when this test was written and again when the structural refusals took the verifier's sentences. The two
 * sources are found by walking up from the working directory, and a missing source FAILS, as in
 * {@link VouwsoortenGrensTest}.
 */
class LezerszinGrensTest {

    private static final String VERIFIER = "spec/verifier/lib";
    private static final String FRONTEND = "frontends/regelspraak/src/main/java/dev/uchar/regelspraak";

    /** Words that make a fragment English; none of them is a Dutch word. */
    private static final Set<String> ENGELS = Set.of(("the must operand operands fold folds value values unit units "
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

    /** English producer sentences without a reader, each with the reason. */
    private static final Map<String, String> ZONDER_LEZER = Map.of(
            "the resolver builds none, ",
            "IrEmitter's refusal of an IfThenElse, an invariant of this emitter: no resolver route builds the node "
                    + "(IR-SPEC ch. 4.10), so no document carries it",
            "so this emitter has no verified wire shape for it",
            "the second line of the same refusal");

    /** A floor on the fragments read, so an extractor that reads nothing is no silent green (measured: 99; 153 once
     *  the structural refusals spoke the verifier's sentences). */
    private static final int MIN_FRAGMENTEN = 149;

    private static final Pattern WOORD = Pattern.compile("[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ\\-']*");
    private static final Pattern SPECIFIER = Pattern.compile("%[sdS]");

    private static Path wortel() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent())
            if (Files.isDirectory(p.resolve(VERIFIER)) && Files.isDirectory(p.resolve(FRONTEND))) return p;
        return fail("kon " + VERIFIER + " en " + FRONTEND + " niet vinden vanaf " + Path.of("").toAbsolutePath()
                + " — deze getuige LEEST beide bronnen en mag niet overslaan als hij ze niet vindt");
    }

    private record Fragment(String bestand, int regel, String tekst) {}

    @Test void elke_engelse_zin_van_de_frontend_staat_in_de_verifier() throws IOException {
        List<String> stukken = verifierstukken(wortel());
        List<String> fouten = new ArrayList<>();
        Set<String> gezien = new LinkedHashSet<>();
        int gelezen = 0;
        for (Fragment f : frontendfragmenten(wortel())) {
            if (ZONDER_LEZER.containsKey(f.tekst())) { gezien.add(f.tekst()); continue; }
            gelezen++;
            if (stukken.stream().noneMatch(s -> s.contains(f.tekst())))
                fouten.add(f.bestand() + ":" + f.regel() + " '" + f.tekst() + "'");
        }
        assertTrue(fouten.isEmpty(), () -> "Engelse frontendzinnen die niet (meer) in " + VERIFIER + " staan — de "
                + "verifier formuleerde ze anders, of de frontend schrijft een eigen Engelse zin:\n  "
                + String.join("\n  ", fouten));
        int aantal = gelezen;
        assertTrue(aantal >= MIN_FRAGMENTEN, () -> "slechts " + aantal + " Engelse fragmenten gelezen, ondergrens "
                + MIN_FRAGMENTEN + " — het uitlezen sluit niet meer aan op de bron");
        assertEquals(ZONDER_LEZER.keySet(), gezien, "de lijst van Engelse zinnen zonder lezer is verouderd");
    }

    @Test void de_zoekregel_weigert_een_verouderde_zin() throws IOException {
        // the two sentences the frontend kept after the verifier reworded them, and the words that replaced them
        List<String> stukken = verifierstukken(wortel());
        for (String oud : List.of("duration + date is ill-formed (the date stands on the left; v0 does not commute)",
                "' is not a recognised time unit or declared day kind"))
            assertTrue(stukken.stream().noneMatch(s -> s.contains(oud)), () -> "de oude zin staat nog in de verifier: " + oud);
        for (String nieuw : List.of("duration + date is ill-formed (the date stands on the left; date arithmetic does "
                + "not commute)", "' is neither a recognised time unit nor a declared day kind"))
            assertTrue(stukken.stream().anyMatch(s -> s.contains(nieuw)), () -> "de zin ontbreekt in de verifier: " + nieuw);
    }

    @Test void de_bronnen_dragen_geen_vorm_die_de_scanner_niet_leest() throws IOException {
        Path w = wortel();
        for (Path p : frontendbronnen(w))
            assertFalse(Files.readString(p, UTF_8).contains("\"\"\""), () -> p + " draagt een tekstblok, dat deze "
                    + "getuige niet leest");
        for (Path p : verifierbronnen(w))
            assertFalse(Files.readString(p, UTF_8).contains("{|"), () -> p + " draagt een quoted string, die deze "
                    + "getuige niet leest");
    }

    // ---- the verifier's side ----

    private static List<Path> verifierbronnen(Path w) throws IOException {
        try (Stream<Path> s = Files.list(w.resolve(VERIFIER))) {
            List<Path> bronnen = s.filter(p -> p.toString().endsWith(".ml")).sorted().toList();
            assertFalse(bronnen.isEmpty(), "geen .ml-bronnen in " + VERIFIER);
            return bronnen;
        }
    }

    /** The fixed pieces of every string literal of the verifier, split at its Printf specifiers. */
    private static List<String> verifierstukken(Path w) throws IOException {
        List<String> stukken = new ArrayList<>();
        for (Path p : verifierbronnen(w))
            for (String literal : ocamlLiterals(Files.readString(p, UTF_8)))
                stukken.addAll(List.of(SPECIFIER.split(literal, -1)));
        return stukken;
    }

    /** OCaml string literals, comments (nesting) and character literals skipped, escapes decoded: {@code \xHH} bytes,
     *  {@code \n}, {@code \t}, an escaped quote or backslash, and a backslash-newline with the blanks after it. */
    static List<String> ocamlLiterals(String s) {
        List<String> uit = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            if (s.startsWith("(*", i)) {
                int diepte = 0;
                do {
                    if (s.startsWith("(*", i)) { diepte++; i += 2; }
                    else if (s.startsWith("*)", i)) { diepte--; i += 2; }
                    else i++;
                } while (diepte > 0 && i < n);
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
            uit.add(b.toString(UTF_8));
            i++;
        }
        return uit;
    }

    // ---- the frontend's side ----

    private static List<Path> frontendbronnen(Path w) throws IOException {
        List<Path> bronnen = new ArrayList<>();
        try (Stream<Path> s = Files.list(w.resolve(FRONTEND).resolve("resolver"))) {
            bronnen.addAll(s.filter(p -> p.toString().endsWith(".java")).sorted().toList());
        }
        bronnen.add(w.resolve(FRONTEND).resolve("emit/IrEmitter.java"));
        for (Path p : bronnen) assertTrue(Files.isRegularFile(p), () -> p + " ontbreekt");
        return bronnen;
    }

    private static List<Fragment> frontendfragmenten(Path w) throws IOException {
        List<Fragment> uit = new ArrayList<>();
        for (Path p : frontendbronnen(w)) {
            String bron = Files.readString(p, UTF_8);
            for (int[] positie : javaLiteralPosities(bron)) {
                String literal = javaLiteral(bron, positie[0], positie[1]);
                int regel = 1 + (int) bron.substring(0, positie[0]).chars().filter(ch -> ch == '\n').count();
                for (String deel : SPECIFIER.split(literal, -1)) {
                    String kern = kern(deel);
                    if (kern != null) uit.add(new Fragment(p.getFileName().toString(), regel, kern));
                }
            }
        }
        return uit;
    }

    /** Start and end (exclusive, the closing quote) of every string literal, comments and char literals skipped. */
    private static List<int[]> javaLiteralPosities(String s) {
        List<int[]> uit = new ArrayList<>();
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
            uit.add(new int[]{begin, i});
            i++;
        }
        return uit;
    }

    /** The literal's text, its escapes decoded ({@code \}{@code uXXXX}, {@code \n}, {@code \t}, an escaped char). */
    private static String javaLiteral(String s, int begin, int eind) {
        StringBuilder b = new StringBuilder();
        for (int i = begin; i < eind; i++) {
            char c = s.charAt(i);
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(++i);
            if (e == 'u') { b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; }
            else b.append(e == 'n' ? '\n' : e == 't' ? '\t' : e);
        }
        return b.toString();
    }

    /** The English part of a literal piece, a producer prefix and a trailing joiner taken off; null where none is. */
    static String kern(String deel) {
        String r = deel.replaceFirst("^IrEmitter: ", "").replaceFirst("^WF-\\d+ \\([a-z]\\)( —)? ?", "");
        List<String> segmenten = new ArrayList<>(List.of(r.split(": ", -1)));
        while (segmenten.size() > 1 && (segmenten.get(0).startsWith("/") || !engels(segmenten.get(0))))
            segmenten.remove(0);
        r = String.join(": ", segmenten).replaceFirst(" — $", "");
        return engels(r) ? r : null;
    }

    private static boolean engels(String tekst) {
        List<String> woorden = new ArrayList<>();
        Matcher m = WOORD.matcher(tekst);
        while (m.find()) woorden.add(m.group().toLowerCase(Locale.ROOT));
        return woorden.size() >= 2 && woorden.stream().noneMatch(NEDERLANDS::contains)
                && woorden.stream().anyMatch(ENGELS::contains);
    }
}
