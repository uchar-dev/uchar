package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The goldens of the truth-maintenance trial's proefset, replayed through the branch as the CLI's {@code --tms} flag
 * replays them, against the expectations gates 5, 6 and 12 pin. The branch is in no gate, so this witness, which the
 * suite runs, is what holds it against regression: every golden of the proefset must play every case. Until the step
 * that lowered the defaults groups, the goldens the branch did not carry yet were pinned here by their refusal, so that
 * a step that made one play moved it by name; the measurements below are from that time.
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
 *
 * <p><b>Red before the defaults</b> (measured 2026-10-08 on the branch's fourth step and its fix of the parameters): the
 * row failed on its first golden with a defaults group, "jutterswet-meldingsketen: Result[status=EXCLUDE,
 * reason=tms-transpiler SKIP: defaults: erkende meldingsdatum — een PrioritizedDefaults: de truth-maintenance-tak draagt
 * deze vorm nog niet, caseFailures=[], casesPlayed=0] ==&gt; expected: &lt;PLAYED&gt; but was: &lt;EXCLUDE&gt;".
 */
class TmsProefsetWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    @Test void de_goldens_van_de_proefset_spelen_elke_case() throws Exception {
        speelt("anchor/jutterswet-kustwacht", 2);
        speelt("jutterswet-initialisatievolgorde", 4);
        speelt("jutterswet-voorwaardelijke-initialisatie", 4);
        speelt("jutterswet-meldingsketen", 3);
        speelt("jutterswet-wrakinitialisatie", 4);
        speelt("anchor/jutterswet-nestdiepte", 3);
        speelt("anchor/jutterswet-nestuitzondering", 9);
        speelt("jutterswet-opgave-boven-berekening", 12);
    }

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
