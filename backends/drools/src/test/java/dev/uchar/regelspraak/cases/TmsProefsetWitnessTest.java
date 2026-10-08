package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The goldens of the truth-maintenance trial's proefset, replayed through the branch as the CLI's {@code --tms} flag
 * replays them, against the expectations gates 5, 6 and 12 pin. The branch is in no gate, so this witness, which the
 * suite runs, is what holds it against regression: a golden the branch carries must play every case, and one it does not
 * carry yet must stay refused, so that a step that makes another golden play moves it here by name.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's first step, which carried characteristic
 * attributions only). The first row failed on "anchor/jutterswet-kustwacht: Result[status=EXCLUDE, reason=tms-transpiler
 * SKIP: regel: uitkijkbonus van een wachter — een attribuutdoel (gelijkstelling van 'uitkijkbonus' van Wachter): de
 * truth-maintenance-tak leidt nog alleen kenmerken af, caseFailures=[], casesPlayed=0] ==&gt; expected: &lt;PLAYED&gt;
 * but was: &lt;EXCLUDE&gt;", the second on its first golden, "jutterswet-meldingsketen ==&gt; expected:
 * &lt;tms-transpiler SKIP: defaults: erkende meldingsdatum — …&gt; but was: &lt;tms-transpiler SKIP: regel:
 * aanmelddatum van een vondst — een attribuutdoel (gelijkstelling van 'aanmelddatum' van Vondst): de
 * truth-maintenance-tak leidt nog alleen kenmerken af&gt;".
 *
 * <p><b>Red before the initialisation</b> (measured 2026-10-08 on the branch's second step): the first row failed on
 * "jutterswet-initialisatievolgorde: Result[status=EXCLUDE, reason=tms-transpiler SKIP: regel: standaardbergloon — een
 * initialisatie (van 'bergloon' van Vondst): de truth-maintenance-tak draagt deze vorm nog niet, caseFailures=[],
 * casesPlayed=0] ==&gt; expected: &lt;PLAYED&gt; but was: &lt;EXCLUDE&gt;", the second on
 * "jutterswet-wrakinitialisatie ==&gt; expected: &lt;tms-transpiler SKIP: regel: standaardbergloon — een doel dat ook
 * een defaultsgroep schrijft ('bergloon' van Vondst): …&gt; but was: &lt;tms-transpiler SKIP: regel: standaardbergloon
 * — een initialisatie (van 'bergloon' van Vondst): de truth-maintenance-tak draagt deze vorm nog niet&gt;". With a
 * guard that counted the initialisation's own write, the first row failed on the golden's cases, "case1-beide-
 * geschreven.json: attribuut bergloon: verwacht 26.00, werkelijk leeg; attribuut vindloon: verwacht 18.00, werkelijk
 * leeg, case2-voorwaarde-onwaar.json: attribuut vindloon: verwacht 14.00, werkelijk leeg, case4-per-vondst.json: geen
 * fixpunt (kringverwijzing?)" ({@link TmsInitialisatieWitnessTest}).
 *
 * <p><b>Red before the consistency</b> (measured 2026-10-08 on the branch's third step): the second row failed on
 * "jutterswet-opgave-boven-berekening ==&gt; expected: &lt;tms-transpiler SKIP: defaults: opgave boven berekening
 * waarde — een PrioritizedDefaults: …&gt; but was: &lt;tms-transpiler SKIP: consistentie: controle opgave waarde — een
 * Consistency: de truth-maintenance-tak draagt deze vorm nog niet&gt;".
 */
class TmsProefsetWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void de_goldens_die_de_tak_draagt_spelen_elke_case() throws Exception {
        speelt("anchor/jutterswet-kustwacht", 2);
        speelt("jutterswet-initialisatievolgorde", 4);
        speelt("jutterswet-voorwaardelijke-initialisatie", 4);
    }

    /** Each by the first form its manifest refuses. The manifest lists the rules before every other family, so a golden
     *  refused on a later family has every rule lowered. */
    @Test void de_goldens_die_de_tak_nog_niet_draagt_blijven_geweigerd() throws Exception {
        Map<String, String> weigering = new LinkedHashMap<>();
        weigering.put("jutterswet-meldingsketen",
                "tms-transpiler SKIP: defaults: erkende meldingsdatum — een PrioritizedDefaults: " + NOG_NIET);
        // the initialisation on the supertype folds into the subtype's defaults stack (RUNTIME-SEMANTICS ch. 7.10)
        weigering.put("jutterswet-wrakinitialisatie", "tms-transpiler SKIP: regel: standaardbergloon — een doel dat ook"
                + " een defaultsgroep schrijft ('bergloon' van Vondst): " + NOG_NIET);
        weigering.put("anchor/jutterswet-nestdiepte",
                "tms-transpiler SKIP: defaults: heffing van een jutter (nestdiepte) — een PrioritizedDefaults: " + NOG_NIET);
        weigering.put("anchor/jutterswet-nestuitzondering",
                "tms-transpiler SKIP: defaults: heffing van een jutter — een PrioritizedDefaults: " + NOG_NIET);
        weigering.put("jutterswet-opgave-boven-berekening",
                "tms-transpiler SKIP: defaults: opgave boven berekening waarde — een PrioritizedDefaults: " + NOG_NIET);
        for (Map.Entry<String, String> e : weigering.entrySet()) {
            String g = e.getKey();
            CaseRunner.Result r = CaseRunner.viaTruthMaintenance(GOLDEN.resolve(g).resolve("ir.json"), zaken(g));
            assertEquals("EXCLUDE", r.status(), () -> g + ": " + r);
            assertEquals(e.getValue(), r.reason(), g);
        }
    }

    private static final String NOG_NIET = "de truth-maintenance-tak draagt deze vorm nog niet";

    private static void speelt(String golden, int cases) throws Exception {
        CaseRunner.Result r = CaseRunner.viaTruthMaintenance(GOLDEN.resolve(golden).resolve("ir.json"), zaken(golden));
        assertEquals("PLAYED", r.status(), () -> golden + ": " + r);
        assertEquals(cases, r.casesPlayed(), golden);
    }

    private static List<Path> zaken(String golden) throws Exception {
        try (Stream<Path> s = Files.list(GOLDEN.resolve(golden))) {
            return s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }
}
