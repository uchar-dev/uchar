package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Period;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The stack of a timeline target read per day (RUNTIME-SEMANTICS ch. 7.8), on both branches of the lowering with truth
 * maintenance, on the documents and cases that the reference interpreter's suite plays as well
 * ({@code WV-7.8-stapel-per-dag-…}): the initialisation is the lowest layer, which decides every day on which no writer
 * decides and the input reads no value, over all time, its condition read as the condition of a rule on a timeline
 * target; the member value of an object creation is the input of the made object per day.
 *
 * <ul>
 *   <li>the documents of {@code ref/test/fixtures/writers-stack}: an initialisation with a condition beside a rule and
 *       the input; one whose value divides by 0 on some days, which the input leaves no day, some days or only the days
 *       on which its value is leeg; one within a window beside a rule, read by a total; a creation of a scalar on every
 *       day read by a sum; a creation of the marker {@code onbekend} beside a rule on the made object; and the goldens
 *       {@code jutterswet-initialisatie-per-dag}, {@code jutterswet-creatie-per-dag} and
 *       {@code jutterswet-afwezigheidsmarkers-initialisatie};</li>
 *   <li>the faults: the initialisation over the asOfDate-bucket scalar of its target (EF-037), and a total that meets
 *       the open segments it writes (EF-036); and none of an initialisation whose value would fault where the input
 *       leaves it no day, since its value is not evaluated there;</li>
 *   <li>the object a creation made holds the value of its making per day, and the rule on its member decides its own
 *       days;</li>
 *   <li>the readings at the fixpoint (ch. 5.12): DISPLACED without nodes where the writers and the input leave the
 *       initialisation no day, and else its nodes, the root the filled segments of its value within the periods of its
 *       condition; line by line the reference interpreter's trace;</li>
 *   <li>the refusals by name: a value of an initialisation or of a member that is no period arithmetic and no
 *       constructor of a literal; an allocation that names a time-dependent attribute, which IR-SPEC WF-128 makes
 *       ill-formed; and a sum per period over a made link that reaches an object of the input, whose place in the fold
 *       order no made link has.</li>
 * </ul>
 */
class TmsTimelineStackWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the fixtures are two levels up
    private static final Path DOCUMENTS = Path.of("..", "..", "ref", "test", "fixtures", "writers-stack");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path OWN = Path.of("src", "test", "resources", "timeline-stack");

    @Test void the_documents_play_per_day_on_both_branches() throws Exception {
        List<Path[]> plays = new ArrayList<>();
        for (String[] d : new String[][]{{"timeline-initialisation", "case.json"},
                {"timeline-initialisation-deposit", "case.json"}, {"timeline-initialisation-deposit", "case-decides.json"},
                {"timeline-initialisation-deposit", "case-leeg.json"}, {"timeline-initialisation-window", "case.json"},
                {"timeline-creation", "case.json"}, {"timeline-creation-marker", "case.json"}})
            plays.add(new Path[]{DOCUMENTS.resolve(d[0]).resolve("ir.json"), DOCUMENTS.resolve(d[0]).resolve(d[1])});
        for (String[] g : new String[][]{{"jutterswet-initialisatie-per-dag", "case1-zonder-invoer.json"},
                {"jutterswet-initialisatie-per-dag", "case2-invoer.json"},
                {"jutterswet-initialisatie-per-dag", "case3-zonder-stormwacht.json"},
                {"jutterswet-creatie-per-dag", "case1-strandloon-tot-juli.json"},
                {"jutterswet-creatie-per-dag", "case2-zonder-strandloon.json"},
                {"jutterswet-afwezigheidsmarkers-initialisatie", "case1-zonder-invoer.json"},
                {"jutterswet-afwezigheidsmarkers-initialisatie", "case2-invoer.json"},
                {"jutterswet-afwezigheidsmarkers-initialisatie", "case3-ongekeurd-naast-invoer.json"}})
            plays.add(new Path[]{GOLDEN.resolve(g[0]).resolve("ir.json"), GOLDEN.resolve(g[0]).resolve(g[1])});
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Path[] p : plays) {
            JsonNode c = JSON.readTree(Files.readString(p[1]));
            for (boolean viaDrl : new boolean[]{false, true}) {
                String head = p[1].getParent().getFileName() + "/" + p[1].getFileName()
                        + (viaDrl ? " drl-tekst" : " exec-model");
                String verdict;
                try {
                    verdict = CaseRunner.playCase(p[0], c, viaDrl);
                } catch (CaseRunner.Excluded e) {
                    verdict = e.getMessage();
                }
                outcomes.add(head + " " + (verdict == null ? "gespeeld" : verdict));
                expected.add(head + " gespeeld");
            }
        }
        assertEquals(expected, outcomes);
    }

    @Test void an_initialisation_faults_where_it_is_evaluated() {
        faults(DOCUMENTS.resolve("timeline-initialisation").resolve("ir.json"),
                DOCUMENTS.resolve("timeline-initialisation").resolve("case-bucket-scalar.json"), "EF-037");
        faults(DOCUMENTS.resolve("timeline-initialisation-total").resolve("ir.json"),
                DOCUMENTS.resolve("timeline-initialisation-total").resolve("case.json"), "EF-036");
    }

    /** "Een Jutter heeft een vondst met bergkosten gelijk aan onbekend indien hij actief is" beside "De bergkosten van
     *  een Vondst moet gesteld worden op 10 van dd. 01-03-2025 tot dd. 01-06-2025": the made Vondst holds onbekend,
     *  waarde(10) in spring and onbekend (the {@code ref/} test {@code WV-7.8-stapel-per-dag-creatie-marker}). */
    @Test void the_made_object_holds_the_value_of_its_making_per_day() throws Exception {
        Path d = DOCUMENTS.resolve("timeline-creation-marker");
        JsonNode c = JSON.readTree(Files.readString(d.resolve("case.json")));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            List<String> made = CaseRunner.forMeasurement(d.resolve("ir.json"), branch).withSession(c,
                    (build, session, labels) -> {
                        DroolsBackend.Firing firing = DroolsBackend.fire(build, session);
                        if (firing.noFixpoint() != null) throw new IllegalStateException(firing.noFixpoint());
                        Fixpoint fixpoint = DroolsBackend.fixpointOf(build, session);
                        DroolsBackend.setDerivedValues(build, session);
                        List<String> out = new ArrayList<>();
                        for (Fixpoint.MadeObject m : fixpoint.objects())
                            out.add(String.valueOf(Period.list((List<?>) RegelSpraakRuntime.get(m.object(),
                                    "bergkosten"))));
                        return out;
                    });
            assertEquals(List.of("[[-inf, 2025-03-01)=onbekend, [2025-03-01, 2025-06-01)=waarde(10),"
                    + " [2025-06-01, +inf)=onbekend]"), made, branch.toString());
        }
    }

    @Test void the_readings_are_those_of_the_reference_interpreter() {
        Path d = DOCUMENTS.resolve("timeline-initialisation-deposit");
        String r0 = "reading /rules/rules/0/versions/0 \"J1\" ", r1 = "reading /rules/rules/1/versions/0 \"J1\" ";
        TmsConditionPerPeriodWitnessTest.readings(d.resolve("ir.json"), d.resolve("case.json"), List.of("case case.json",
                r0 + "DISPLACED", r1 + "DISPLACED", "READINGS 1"));
        List<String> decides = List.of(r0 + "0 Rounding [[- 2025-01-01 50], [2025-07-01 - 50]]",
                r0 + "1 Binary [[- 2025-01-01 50], [2025-01-01 2025-07-01 empty], [2025-07-01 - 50]]",
                r0 + "2 Literal 100",
                r0 + "3 AttributeReference [[- 2025-01-01 2], [2025-01-01 2025-07-01 0], [2025-07-01 - 2]]",
                r0 + "HOLDS", r1 + "0 CharacteristicCheck true",
                r1 + "1 Rounding [[- 2025-01-01 50], [2025-07-01 - 50]]",
                r1 + "2 Binary [[- 2025-01-01 50], [2025-01-01 2025-07-01 empty], [2025-07-01 - 50]]",
                r1 + "3 Literal 100",
                r1 + "4 AttributeReference [[- 2025-01-01 2], [2025-01-01 2025-07-01 0], [2025-07-01 - 2]]",
                r1 + "HOLDS");
        for (String c : List.of("case-decides.json", "case-leeg.json")) {
            List<String> expected = new ArrayList<>(List.of("case " + c));
            expected.addAll(decides);
            expected.add("READINGS 1");
            TmsConditionPerPeriodWitnessTest.readings(d.resolve("ir.json"), d.resolve(c), expected);
        }
        Path w = DOCUMENTS.resolve("timeline-initialisation-window");
        String v0 = "reading /rules/rules/0/versions/0 \"J1\" ", v1 = "reading /rules/rules/1/versions/0 \"J1\" ",
                v2 = "reading /rules/rules/2/versions/0 \"J1\" ";
        TmsConditionPerPeriodWitnessTest.readings(w.resolve("ir.json"), w.resolve("case.json"), List.of("case case.json",
                v0 + "0 TimeWindow [[2025-03-01 2025-06-01 empty]]", v0 + "1 Literal 2025-03-01",
                v0 + "2 Literal 2025-06-01", v0 + "3 Literal [[2025-03-01 2025-06-01 20]]", v0 + "HOLDS",
                v1 + "0 TimeWindow [[2025-01-01 2026-01-01 empty]]", v1 + "1 Literal 2025-01-01",
                v1 + "2 Literal 2026-01-01", v1 + "3 Literal [[2025-01-01 2026-01-01 5]]", v1 + "HOLDS",
                v2 + "0 Aggregation 105",
                v2 + "1 AttributeReference [[2025-01-01 2025-03-01 5], [2025-03-01 2025-06-01 20], [2025-06-01 2026-01-01 5]]",
                v2 + "HOLDS", "READINGS 1"));
        Path t = DOCUMENTS.resolve("timeline-initialisation");
        TmsConditionPerPeriodWitnessTest.readings(t.resolve("ir.json"), t.resolve("case.json"), List.of("case case.json",
                v0 + "0 TimeWindow [[2025-03-01 2025-06-01 empty]]", v0 + "1 Literal 2025-03-01",
                v0 + "2 Literal 2025-06-01", v0 + "3 Literal [[2025-03-01 2025-06-01 20]]", v0 + "HOLDS",
                v1 + "0 CharacteristicCheck true", v1 + "1 Literal [[- - 5]]", v1 + "HOLDS", "READINGS 1"));
    }

    /** A value that is no period arithmetic and no constructor of a literal, on a timeline target of a sum type, in an
     *  initialisation with and without a condition and in a creation's member (the {@code ref/} tests
     *  {@code WV-7.8-stapel-per-dag-somproducent-…}); and an allocation over a share for every month, which IR-SPEC
     *  WF-128 makes ill-formed ({@code -verdeling}). */
    @Test void the_forms_without_a_reading_per_day_are_refused_by_name() throws Exception {
        for (String d : List.of("sum-producer-initialisation", "sum-producer-conditional", "sum-producer-creation"))
            TmsConditionPerPeriodWitnessTest.refused(DOCUMENTS.resolve(d).resolve("ir.json"),
                    JSON.readTree(Files.readString(DOCUMENTS.resolve(d).resolve("case.json"))), "opslagkosten",
                    "DROOLS-316");
        Path a = DOCUMENTS.resolve("timeline-allocation");
        TmsConditionPerPeriodWitnessTest.refused(a.resolve("ir.json"),
                JSON.readTree(Files.readString(a.resolve("case.json"))), "WF-128", "DROOLS-315");
    }

    /** "Het strandloon van een Jutter moet geïnitialiseerd worden op zijn kustloon" over the input 10 on all time, the
     *  kustloon given as the asOfDate-bucket scalar 40: the input leaves the initialisation no day, so its value, which
     *  would fault with EF-037, is not evaluated, and the strandloon is 10 on all time (ch. 7.8; the reference
     *  interpreter and the ASP runner play the case alike). */
    @Test void an_initialisation_that_decides_no_day_is_not_evaluated() throws Exception {
        Path d = OWN.resolve("not-evaluated");
        JsonNode c = JSON.readTree(Files.readString(d.resolve("case.json")));
        for (boolean viaDrl : new boolean[]{false, true})
            assertEquals(null, CaseRunner.playCase(d.resolve("ir.json"), c, viaDrl), viaDrl ? "drl-tekst" : "exec-model");
    }

    /** "Het meldloon van een Melding moet berekend worden als de som van het loon van alle melders van de Melding" on a
     *  Melding that "Een Jutter heeft een melding met meldnummer gelijk aan 1" makes: the made link reaches the
     *  creation's subject, an object of the input, which the case may give the loon as the asOfDate-bucket scalar, and
     *  a made link has no place in the fold order that names the first such member, so both branches refuse the sum by
     *  name, where a sum from the Jutter over the made Meldingen plays ({@code timeline-creation}). */
    @Test void a_sum_over_a_made_link_to_an_object_of_the_input_is_refused_by_name() throws Exception {
        Path d = OWN.resolve("made-link-to-input");
        TmsConditionPerPeriodWitnessTest.refused(d.resolve("ir.json"), JSON.readTree(Files.readString(d.resolve("case.json"))),
                "jutmelding", "DROOLS-212");
    }

    private static void faults(Path ir, Path c, String code) {
        CaseRunner.RedResult exec = CaseRunner.runRed(ir, c, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, CaseRunner.runRed(ir, c, DroolsBackend.Branch.DRL_TEXT), "de twee takken, " + c);
        assertEquals("FAULT", exec.status(), exec::detail);
        assertEquals(code, exec.code(), exec::detail);
        assertEquals(List.of(Map.of("attribute", "strandloon", "label", "J1")), exec.params(), exec::detail);
    }
}
