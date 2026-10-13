package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * post manned through 2025 and 2026 does, and a post without any period counts vacuously. The verifier passes both
 * documents, and the reference interpreter and the ASP backend pass every case.
 *
 * <p>Measured on {@code 4c8a687b}, before step 3a, per document (verifier, ref, clingo, exec-model branch, DRL-text
 * branch, {@code DRL_TMS}): the computation rule PASS, PASS 4, PASS 4, PLAYED 4, EXCLUDE on the compiler error above,
 * EXCLUDE "drl-tms: de DRL-weergave draagt Bijdrage nog niet"; the creation guard PASS, PASS 3, PASS 3, PLAYED 3, the
 * same compiler error, and EXCLUDE "drl-tms: de DRL-weergave draagt Creatie nog niet". The creation guard plays through
 * {@code DRL_TMS} once the branch carries a creation (step 5a); until then the branch refuses it for the creation.
 *
 * <p><b>Red</b> on {@code 4c8a687b}: the test of the computation rule fails on the refusal of the contribution. The
 * test of the creation guard holds there too, since the rendering meets the creation first: it pins the loud refusal
 * until step 5a, not a form this step brings.
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
            assertNull(CaseRunner.speelCase(ir, c, DroolsBackend.Branch.DRL_TMS), "drl-tms — " + zaak);
        }
        String tekst = DroolsBackend.build(ir, DroolsBackend.Branch.DRL_TMS).drl();
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

    @Test void de_creatieguard_wacht_op_stap_5a_en_drl_tms_weigert_haar_luid() throws Exception {
        Path ir = DOCUMENTEN.resolve("creatieguard").resolve("ir.json");
        List<Path> zaken = zaken(ir);
        assertEquals(3, zaken.size(), "de cases van de creatieguard");
        for (Path zaak : zaken) {
            JsonNode c = JSON.readTree(zaak.toFile());
            assertNull(CaseRunner.speelCase(ir, c, DroolsBackend.Branch.EXEC_MODEL), "exec-model — " + zaak);
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.speelCase(ir, c, DroolsBackend.Branch.DRL_TMS), "drl-tms — " + zaak);
            assertEquals("drl-tms: de DRL-weergave draagt Creatie nog niet", e.getMessage());
        }
    }

    private static List<Path> zaken(Path ir) throws Exception {
        try (Stream<Path> s = Files.list(ir.getParent())) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
