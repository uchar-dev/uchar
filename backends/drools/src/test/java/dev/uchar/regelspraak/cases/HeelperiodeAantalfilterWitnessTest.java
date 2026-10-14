package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The form of card drl-aantal-filter-zink through {@code DRL_TMS} (card drl-teksttak-op-truth-maintenance, step 3a,
 * FABLE-2 §7.4): a whole-period qualification in the filter of a count, "het aantal posten van het Kustvak die
 * gedurende het gehele jaar bemand zijn", in a computation rule and in the guard of an object creation. The DRL-text
 * branch sank both documents with a compiler error in the accumulate of its anchor attempt
 * ({@code Unable to Analyse Expression rsGeheelPeriode($subject.getBemand(), "JAAR", 7, null)}). In the rendering of the
 * analysis the filter stands in the test of the rule that inserts a member's contribution, as a function that calls
 * the route of the whole period, and no accumulate holds it.
 *
 * <p>The two documents under {@code heelperiode-aantalfilter} are own work: written in RegelSpraak ({@code bron.rgs})
 * and emitted by the frontend, which does not lower a die/dat-subselection with a qualifier on the operand of an
 * aggregation yet (it writes an {@code Unresolved} filter and a warning). The filter of each count is therefore written
 * in by hand, the check of the characteristic "bemand" of the member under the qualifier of the whole year, and the
 * warning is dropped. The expectations are computed by hand from RUNTIME-SEMANTICS ch. 3.9: a post counts where each
 * year its true periods touch is covered on every day, so a post manned for the first half of 2026 does not count, a
 * post manned through 2025 and 2026 does, and a post without any period counts vacuously.
 *
 * <p>This witness fires two branches only: the exec-model branch and {@code DRL_TMS}. The documents lie outside the
 * corpus, so no gate runs them through the verifier, the reference interpreter, the ASP backend or the DRL-text
 * branch; those were run by hand. Measured on {@code 4c8a687b}, before step 3a, per document (verifier, ref, clingo,
 * exec-model branch, DRL-text branch, {@code DRL_TMS}): the computation rule PASS, PASS 4, PASS 4, PLAYED 4, EXCLUDE
 * on the compiler error above, EXCLUDE "drl-tms: de DRL-weergave draagt Bijdrage nog niet"; the creation guard PASS,
 * PASS 3, PASS 3, PLAYED 3, the same compiler error, and EXCLUDE "drl-tms: de DRL-weergave draagt Creatie nog niet".
 * Measured again at fix round 3a of the card, on {@code 85b9a970}, with the same outcomes but for {@code DRL_TMS}, which
 * plays the computation rule (PLAYED 4): the verifier PASS on both documents, ref and clingo PASS on each of the seven
 * cases, one case per call ({@code ref/bin/runner.exe <ir.json> <case.json>} and
 * {@code backends/clingo/bin/clingo_runner.exe <ir.json> <case.json>} under {@code _build/default}). The raw output of
 * that run is not in the repository; it is kept with the measurements of the card, in
 * {@code stap3b/fix3a/heelperiode-ruw.txt} (verifier, ref, clingo) and {@code stap3b/fix3a/heelperiode-ruw-drools.txt}
 * (the three Drools branches through the CLI). The creation guard plays through {@code DRL_TMS} since the branch
 * carries a creation (step 5); until then the branch refused it for the creation, and this witness pinned that refusal.
 *
 * <p><b>Red</b> on {@code 4c8a687b}: the test of the computation rule fails on the refusal of the contribution. The
 * test of the creation guard held there too, since the rendering met the creation first: it pinned the loud refusal
 * until step 5, not a form step 3a brings. <b>Red</b> on {@code b4d83a69}, before the creations of step 5: the creation
 * guard failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Creatie nog niet".
 */
class HeelperiodeAantalfilterWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    // cwd under surefire is the module dir (backends/drools)
    private static final Path DOCUMENTEN = Path.of("src", "test", "resources", "heelperiode-aantalfilter");

    /** A function of the text that holds the route of the whole period, and its name. */
    private static final Pattern GEHEEL_PERIODE = Pattern.compile(
            "function boolean (r\\d+_toets\\d+)\\([^)]*\\) \\{\\n    return Rekenhulpen\\.geheelPeriode\\(");

    @Test void de_rekenregel_speelt_door_drl_tms_zoals_door_de_exec_model_tak() throws Exception {
        Path ir = DOCUMENTEN.resolve("rekenregel").resolve("ir.json");
        List<Path> zaken = zaken(ir);
        assertEquals(4, zaken.size(), "de cases van de rekenregel");
        for (Path zaak : zaken) {
            JsonNode c = JSON.readTree(zaak.toFile());
            assertNull(CaseRunner.speelCase(ir, c, DroolsBackend.Branch.EXEC_MODEL), "exec-model — " + zaak);
            assertNull(CaseRunner.speelCase(ir, c, DroolsBackend.Branch.DRL_TEXT), "drl-tms — " + zaak);
        }
        String tekst = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TEXT).drl();
        List<String> functies = new ArrayList<>();
        Matcher m = GEHEEL_PERIODE.matcher(tekst);
        while (m.find()) functies.add(m.group(1));
        assertEquals(2, functies.size(), () -> "een functie van het filter per bijdrageregel:\n" + tekst);
        for (String f : functies)
            assertTrue(Pattern.compile("rule \"[^\"]*: bijdrage aan vouw \\d+\"\\n(?:(?!end\\n)[^\\n]*\\n)*?    eval\\( " + f
                    + "\\(").matcher(tekst).find(), () -> f + " is de toets van een bijdrageregel:\n" + tekst);
        for (String regel : tekst.split("\n"))
            assertFalse(regel.contains("accumulate(") && regel.contains("geheelPeriode"),
                    () -> "het filter staat in een accumulate: " + regel);
    }

    /** The two rules of the computation rule in the other order of the document, its only other order (card
     *  drl-aantal-filter-zink, acceptance (a): in every order of the rules involved), play every case on both branches.
     *  Green from the start, on {@code 0458b2d0}: truth maintenance reaches the same fixpoint in either order, and no
     *  step of card drl-teksttak-op-truth-maintenance changes that; the row guards it for the text. */
    @Test void de_rekenregel_speelt_ook_in_de_andere_volgorde_van_haar_twee_regels() throws Exception {
        Path ir = DOCUMENTEN.resolve("rekenregel").resolve("ir.json");
        JsonNode document = JSON.readTree(ir.toFile());
        ArrayNode regels = (ArrayNode) document.get("rules").get("rules");
        assertEquals(List.of("postental van een kustvak", "waaktarief van een kustvak"),
                List.of(regels.get(0).get("name").asText(), regels.get(1).get("name").asText()), "de regels in het document");
        List<JsonNode> omgekeerd = new ArrayList<>();
        regels.forEach(omgekeerd::add);
        Collections.reverse(omgekeerd);
        regels.removeAll();
        regels.addAll(omgekeerd);
        Path andere = Files.createTempFile("heelperiode-andere-volgorde", ".json");
        andere.toFile().deleteOnExit();
        Files.writeString(andere, JSON.writeValueAsString(document));
        for (Path zaak : zaken(ir)) {
            JsonNode c = JSON.readTree(zaak.toFile());
            for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
                assertNull(CaseRunner.speelCase(andere, c, tak), tak + " — " + zaak);
        }
    }

    @Test void de_creatieguard_speelt_op_beide_takken() throws Exception {
        Path ir = DOCUMENTEN.resolve("creatieguard").resolve("ir.json");
        List<Path> zaken = zaken(ir);
        assertEquals(3, zaken.size(), "de cases van de creatieguard");
        for (Path zaak : zaken) {
            JsonNode c = JSON.readTree(zaak.toFile());
            for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
                assertNull(CaseRunner.speelCase(ir, c, tak), tak + " — " + zaak);
        }
    }

    private static List<Path> zaken(Path ir) throws Exception {
        try (Stream<Path> s = Files.list(ir.getParent())) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
