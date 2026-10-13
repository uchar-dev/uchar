package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documents fired through {@code DRL_TMS}, the analysis of the exec-model branch rendered as DRL text (card
 * drl-teksttak-op-truth-maintenance, step 2b), each case a match of the expectations the reference interpreter, the
 * ASP backend and the exec-model branch give; the harness of the step compares every fact of these sessions with that
 * branch.
 *
 * <p><b>A link that is absent</b> (FABLE-2 §4 point 2): a read along a path is a function of the text that walks the
 * path with the route {@code langs} and gives null where a link is absent, as the executable model reads it; the
 * null-safe navigation of the dialect ({@code post!.hoogte}) would drop the match instead. The cases are the four
 * goldens whose case derives a value over an absent link in a binding along a path, which the spike of the second
 * advice measured to differ under {@code !.}, and two goldens whose case has an absent link in a field test along a
 * path: a Wachter without a post in {@code anchor/jutterswet-kustwacht}, a Schuit without a werf in
 * {@code jutterswet-gehele-periode-leeg}. Each text holds the form, and each case leaves the link out.
 *
 * <p><b>Members with names outside the corpus</b>: the documents of {@code EigenschapWitnessTest}, the member "İş"
 * with and without input, the input "日 X" and the derived "İç", the input characteristic "ışık", and the cases of the
 * golden {@code jutterswet-sluimerwacht} with a key in another capitalisation: the text reads and writes each member
 * through the names the generator wrote.
 *
 * <p><b>Red</b> on {@code 5eda2eff}, before the branch: {@code DroolsBackend.Branch.DRL_TMS} did not exist, and this class
 * did not compile.
 */
class DrlTmsWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path EIGENSCHAP = Path.of("src", "test", "resources", "eigenschap");

    /** A binding of a value along a path: the function of the path as the bound expression. */
    private static final Pattern PAD_IN_BINDING = Pattern.compile("\\$\\w+ : \\(r\\d+_pad\\d+\\(this\\)\\)");
    /** A field test along a path: the function of the path tested for null. */
    private static final Pattern PAD_IN_VELDTOETS = Pattern.compile("r\\d+_pad\\d+\\(this\\) [=!]= null");

    /** One case: a golden, its case file, the form of the text the case reaches, and the instance whose link the case
     *  leaves out with the role it lacks. */
    private record Geval(String golden, String zaak, Pattern vorm, String label, String rol) {}

    @Test void een_ontbrekende_link_in_een_binding_en_in_een_veldtoets() throws Exception {
        List<Geval> gevallen = List.of(
                new Geval("synth-navigatiepremie", "case2.json", PAD_IN_BINDING, "S1", "werf"),
                new Geval("jutterswet-havengeld", "case4-zonder-taxatie.json", PAD_IN_BINDING, "V", null),
                new Geval("jutterswet-sluitsom", "case2-leeg-en-onverbonden.json", PAD_IN_BINDING, "V3",
                        "bewarende jutter"),
                new Geval("synth-gehelejaarkenmerk", "case1-geen-koppeling.json", PAD_IN_BINDING, "S1", "werf"),
                new Geval("anchor/jutterswet-kustwacht", "case2.json", PAD_IN_VELDTOETS, "W3", "post"),
                new Geval("jutterswet-gehele-periode-leeg", "case1-geheel-leeg.json", PAD_IN_VELDTOETS, "S2", "werf"));
        List<String> fouten = new ArrayList<>();
        for (Geval g : gevallen) {
            Path ir = GOLDEN.resolve(g.golden()).resolve("ir.json");
            JsonNode zaak = JSON.readTree(GOLDEN.resolve(g.golden()).resolve(g.zaak()).toFile());
            String tekst = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TMS).drl();
            if (!g.vorm().matcher(tekst).find()) fouten.add(g + ": de tekst heeft de vorm niet");
            if (!zonderLink(zaak, g)) fouten.add(g + ": de case geeft de link");
            String uitkomst = CaseRunner.speelCase(ir, zaak, DroolsBackend.Branch.DRL_TMS);
            if (uitkomst != null) fouten.add(g.golden() + " " + g.zaak() + ": " + uitkomst);
        }
        assertEquals(List.of(), fouten);
    }

    /** Whether the case leaves out the link of {@code g}: the instance carries no role {@code g.rol()}, or, where that is
     *  null, no instance names the label in a role, the link held from the other end. */
    private static boolean zonderLink(JsonNode zaak, Geval g) {
        for (JsonNode i : zaak.get("input").get("instances")) {
            JsonNode rollen = i.get("roles");
            if (g.rol() == null) {
                if (rollen != null) for (JsonNode labels : rollen) for (JsonNode l : labels)
                    if (l.asText().equals(g.label())) return false;
            } else if (i.get("label").asText().equals(g.label())) {
                return rollen == null || !rollen.has(g.rol()) || rollen.get(g.rol()).isEmpty();
            }
        }
        return g.rol() == null;
    }

    /** The documents of {@code EigenschapWitnessTest} and the cases of {@code eigenschap/spelling}, each case through
     *  {@code DRL_TMS}; that test fires them through the exec-model branch. */
    @Test void de_documenten_met_lastige_namen_vuren() throws Exception {
        List<String[]> gevallen = new ArrayList<>();
        for (String document : List.of("turks-lid", "lastige-leden", "turks-kenmerk"))
            try (Stream<Path> s = Files.list(EIGENSCHAP.resolve(document))) {
                for (Path zaak : s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList())
                    gevallen.add(new String[]{EIGENSCHAP.resolve(document).resolve("ir.json").toString(), zaak.toString()});
            }
        try (Stream<Path> s = Files.list(EIGENSCHAP.resolve("spelling"))) {
            for (Path zaak : s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList())
                gevallen.add(new String[]{GOLDEN.resolve("jutterswet-sluimerwacht").resolve("ir.json").toString(),
                        zaak.toString()});
        }
        List<String> fouten = new ArrayList<>();
        for (String[] g : gevallen) {
            String uitkomst = CaseRunner.speelCase(Path.of(g[0]), JSON.readTree(Path.of(g[1]).toFile()),
                    DroolsBackend.Branch.DRL_TMS);
            if (uitkomst != null) fouten.add(g[1] + ": " + uitkomst);
        }
        assertEquals(9, gevallen.size(), "de cases van de documenten");
        assertEquals(List.of(), fouten);
    }

    /** The CLI's {@code --drl-tms} replays through the branch: a golden plays, also one with day kinds, whose
     *  predicates the build gives as the globals the text declares ({@code Build.dagsoortGlobalen}). <b>Red</b> on
     *  {@code 2704d81d}, before the day kinds of step 5: the branch refused the golden with day kinds, "EXCLUDE drl-tms:
     *  een document met dagsoorten", and this test pinned that refusal. */
    @Test void de_vlag_drl_tms_speelt_door_de_tak() throws Exception {
        java.io.ByteArrayOutputStream uit = new java.io.ByteArrayOutputStream();
        Path kustwacht = GOLDEN.resolve("anchor").resolve("jutterswet-kustwacht");
        int code = DroolsBackendMain.voer(new String[]{"--drl-tms", kustwacht.resolve("ir.json").toString(),
                kustwacht.resolve("case1.json").toString(), kustwacht.resolve("case2.json").toString()},
                new java.io.PrintStream(uit, true, java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("PLAYED 2", uit.toString(java.nio.charset.StandardCharsets.UTF_8).trim());
        assertEquals(0, code);
        uit.reset();
        Path getijklok = GOLDEN.resolve("jutterswet-getijklok");
        List<String> argumenten = new ArrayList<>(List.of("--drl-tms", getijklok.resolve("ir.json").toString()));
        try (Stream<Path> s = Files.list(getijklok)) {
            s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().forEach(p -> argumenten.add(p.toString()));
        }
        code = DroolsBackendMain.voer(argumenten.toArray(String[]::new),
                new java.io.PrintStream(uit, true, java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("PLAYED 2", uit.toString(java.nio.charset.StandardCharsets.UTF_8).trim());
        assertEquals(0, code);
        DroolsBackend.Build b = DroolsBackend.build(getijklok.resolve("ir.json"), DroolsBackend.Branch.DRL_TMS);
        java.util.Set<String> gedeclareerd = new java.util.TreeSet<>();
        java.util.regex.Matcher m = Pattern.compile("(?m)^global java\\.util\\.function\\.Predicate (\\w+);$")
                .matcher(b.drl());
        while (m.find()) gedeclareerd.add(m.group(1));
        assertTrue(!gedeclareerd.isEmpty(), "de tekst declareert de global van een dagsoort");
        assertEquals(gedeclareerd, new java.util.TreeSet<>(b.dagsoortGlobalen().keySet()));
    }

    /**
     * Two day kinds whose predicates differ on the dates the case reaches (round 5b, on the lens of round 5), so that a
     * text that swaps two predicates cannot pass. On {@code DRL_TMS} the predicate of a day kind is a global of the
     * text, which the session sets by its name from {@code Build.dagsoortGlobalen}; the test of the names alone
     * ({@link #de_vlag_drl_tms_speelt_door_de_tak}) passes with a swapped pair. The document
     * {@code src/test/resources/dagsoorten/twee-dagsoorten} is {@code jutterswet-getijklok} with a second day kind
     * beside the springtijdag (day 1 or 15 of the month): the doodtijdag (day 8 or 22), with a doodtermijn in
     * doodtijdagen and a doodsluiting, the meldtijdstip plus the doodtermijn. The case steps over both: the
     * sluittijdstip of G1 is 2026-04-15T09:15:30 and its doodsluiting 2026-04-22T09:15:30, which a swap turns around.
     * Each global carries the predicate of its own day kind, true on a day of that kind and false on one of the other,
     * and the case plays on both branches.
     *
     * <p><b>Measured</b> (2026-10-14): verifier PASS; the reference interpreter and the ASP backend PASS (7
     * expectations). <b>Red</b> with a mutant that swaps the two predicates among the globals of the text (in
     * {@code DrlWeergave.render}, not committed): "rsDagsoort_Springtijdag: de springtijdag ==&gt; expected: &lt;[true,
     * false]&gt; but was: &lt;[false, true]&gt;"; and with that check left out of a copy, the case on {@code DRL_TMS},
     * "case: attribuut sluittijdstip: verwacht 2026-04-15T09:15:30, werkelijk 2026-04-22T09:15:30; attribuut
     * doodsluiting: verwacht 2026-04-22T09:15:30, werkelijk 2026-04-15T09:15:30; attribuut openingstijdstip: verwacht
     * 2026-02-01T09:15:30, werkelijk 2026-02-08T09:15:30; …".
     */
    @Test void twee_dagsoorten_op_datums_waarop_hun_predicaten_verschillen() throws Exception {
        Path map = Path.of("src", "test", "resources", "dagsoorten", "twee-dagsoorten");
        Path ir = map.resolve("ir.json");
        JsonNode zaak = JSON.readTree(map.resolve("case.json").toFile());
        Map<String, Predicate<LocalDate>> predicaten =
                DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TMS).dagsoortGlobalen();
        assertEquals(Set.of("rsDagsoort_Springtijdag", "rsDagsoort_Doodtijdag"), predicaten.keySet(),
                "de predicaatglobals van de tekst");
        LocalDate vijftien = LocalDate.of(2026, 3, 15), tweeëntwintig = LocalDate.of(2026, 3, 22);
        Predicate<LocalDate> spring = predicaten.get("rsDagsoort_Springtijdag");
        Predicate<LocalDate> dood = predicaten.get("rsDagsoort_Doodtijdag");
        assertEquals(List.of(true, false), List.of(spring.test(vijftien), spring.test(tweeëntwintig)),
                "rsDagsoort_Springtijdag: de springtijdag");
        assertEquals(List.of(false, true), List.of(dood.test(vijftien), dood.test(tweeëntwintig)),
                "rsDagsoort_Doodtijdag: de doodtijdag");
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TMS))
            assertEquals(null, CaseRunner.speelCase(ir, zaak, tak), tak + ": twee dagsoorten");
    }
}
