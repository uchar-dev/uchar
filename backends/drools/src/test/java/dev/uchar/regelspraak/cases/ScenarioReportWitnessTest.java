package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.engine.ScenarioResultaat;
import dev.uchar.regelspraak.engine.ScenarioResultaat.DekkingSoort;
import dev.uchar.regelspraak.engine.ScenarioResultaat.Status;
import dev.uchar.regelspraak.engine.ScenarioResultaat.VerwachtingResultaat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The scenario report of the replay ({@code CaseRunner.rapport}): what the source toolchain's scenario runner reported
 * on top of a comparison — per expectation the expected and the actual value, the fired rules with the labels they
 * bound, the coverage of the lowered constructs, and MARKER where the backend does not carry the document — filled
 * by the same exec-model replay the gate plays. The fixtures are corpus goldens: {@code jutterswet-uitsluiting-rgs},
 * whose four cases its scenario document regenerates, and {@code anchor/jutterswet-bootberging}, which no exec-model
 * branch carries (a source path over a relation whose roles are both to-many, for which the bean model has no field).
 * Until card tms-omschakeling the second fixture was {@code anchor/jutterswet-erflijn}, which the exec-model branch
 * with truth maintenance plays.
 *
 * <p>The trace of the first case is pinned whole. The report counts a firing under the form of the document its rule
 * belongs to ({@code TmsTranspiler.formOf}), so "defaults K:Jutter.uitgesloten" is the document's one defaults group,
 * which decides for M, its default and its agreement over the root in one row; a rule of no form, here the standing
 * of the characteristic that follows, has no row. Until the switch-over the trace was pinned by the name of the
 * destructive branch's rule, "defaults K:Jutter.uitgesloten"; while both branches stood it was pinned by its labels and
 * counts alone, which a report without a firing of the group passed as well (the lens on step 5a1 of card
 * tms-omschakeling, 2026-10-10); after the switch-over it was pinned by the part of each rule's name after its form,
 * "default 1" and "eens wortel", until a second fixture showed that the part puts rules of different forms in one row.
 *
 * <p>That fixture is {@code jutterswet-premiestaffel}, with five defaults groups on three object types, each one row
 * with the labels of its own type and of the members its folds read, and in the coverage each fired. <b>Red before the
 * repair</b> (measured 2026-10-10 on {@code 9906feb7}): one row "default 0" with the labels of three types (J2, J5, M1,
 * C1, M2, J1), likewise "default 1", "default 2", "bijdrage aan vouw 0" to "3", "onwaar" and "uit een afleiding", and
 * every defaults group NIET_GERAAKT in the coverage, since no row carried its name.
 *
 * <p>A form's name stays whole: the grammar allows ": " in a rule name, and a third fixture renames the two rules of
 * {@code anchor/jutterswet-borgdeel} to "premie: basis" and "toeslag: basis" (verifier, ref and clingo pass it). <b>Red
 * before the repair</b> (measured 2026-10-10 on {@code 732009b7}): one row "basis [J1] 2", since the report cut a name
 * at its first ": ", and the rules of no form, the standings, in rows of their own ("stand Jutter.borgtarief: uit de
 * invoer"); the lens on steps 5b2 and 5b3 of card tms-omschakeling found the cut. The report before the switch-over
 * cut the same way.
 *
 * <p>The coverage takes a form's exact name, so each version of a rule has a verdict of its own, while the trace gives
 * the versions of one rule one row. On {@code anchor/jutterswet-jutheffing} the first version of "jutheffing naar
 * jaartarief" holds in 2025 and the second does not. <b>Red before the repair</b> (measured 2026-10-10 on
 * {@code cad4b55b}): "jutheffing naar jaartarief #2 GEVUURD", since the coverage compared on the row's name; the
 * report before the switch-over said the same, when the destructive lowering fired the rule of every version and let
 * its window decide (the herkeuring after the fix round).
 */
class ScenarioReportWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path EXCLUSION = GOLDEN.resolve("jutterswet-uitsluiting-rgs");

    private static List<Path> cases(Path golden) throws Exception {
        try (Stream<Path> s = Files.list(golden)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }

    private static ScenarioResultaat van(List<ScenarioResultaat> rapport, String naam) {
        return rapport.stream().filter(r -> r.naam().equals(naam)).findFirst()
                .orElseThrow(() -> new AssertionError("geen rapport voor " + naam + ": " + rapport));
    }

    @Test void a_golden_that_plays_reports_verdicts_firings_and_coverage() throws Exception {
        List<ScenarioResultaat> rapport = CaseRunner.rapport(EXCLUSION.resolve("ir.json"), cases(EXCLUSION));
        assertEquals(4, rapport.size());
        assertTrue(rapport.stream().allMatch(ScenarioResultaat::pass), () -> rapport.toString());

        ScenarioResultaat refused = van(rapport, "case1-geweigerd.json");
        assertEquals(List.of(
                new VerwachtingResultaat("characteristicPresent M uitgesloten", "aanwezig", "aanwezig", true, 1),
                new VerwachtingResultaat("characteristicAbsent M penninggerechtigd", "afwezig", "afwezig", true, 2),
                new VerwachtingResultaat("consistent", "consistent", "consistent", true, 3)),
                refused.verwachtingen());
        assertEquals("", refused.diagnose());
        // a row names the case's objects its rules matched: M, by the default; the agreement of the group over the root
        // matches facts of the branch alone and adds none
        List<String> trace = refused.trace().vuringen().stream().map(v -> v.regel() + " " + v.labels() + " "
                + v.aantal()).sorted().toList();
        assertEquals(List.of("defaults K:Jutter.uitgesloten [M] 2"), trace,
                "de defaultsgroep vuurt haar default en haar overeenstemming op M; de stand van het kenmerk heeft geen rij");
        assertEquals(DekkingSoort.GEVUURD, dekking(refused, "defaults K:Jutter.uitgesloten"));
        // the rule reads 'niet uitgesloten': with the refusal standing it never matches, without it it fires
        assertEquals(DekkingSoort.NIET_GERAAKT, dekking(refused, "penninggerechtigd tenzij uitgesloten"));
        assertEquals(DekkingSoort.GEVUURD, dekking(van(rapport, "case4-kaal.json"), "penninggerechtigd tenzij uitgesloten"));
    }

    @Test void a_firing_counts_under_the_form_of_its_rule() throws Exception {
        Path premiestaffel = GOLDEN.resolve("jutterswet-premiestaffel");
        ScenarioResultaat r = CaseRunner.rapport(premiestaffel.resolve("ir.json"),
                List.of(premiestaffel.resolve("case1-staffel.json"))).get(0);
        assertEquals(Status.PASS, r.status(), r::toString);
        // the labels sorted: a row is pinned by what its rules bound, not by the order of the agenda
        List<String> trace = r.trace().vuringen().stream()
                .map(v -> v.regel() + " " + v.labels().stream().sorted().toList() + " " + v.aantal()).sorted().toList();
        assertEquals(List.of(
                "defaults A:Controle.controlepremie [C1, C2, C3, C4] 6",
                "defaults A:Jutter.premie [J1, J2, J3, J4, J5, V1, V2, V3, V4, V5, V6, V7, V8, V9] 20",
                "defaults A:Melding.gemelde premie [M1, M2, M3, M4] 7",
                "defaults K:Jutter.ijverig [J1, J2, J3, J4, J5, V1, V2, V3, V4, V5, V6, V7, V8, V9] 29",
                "defaults K:Melding.dringend [M1, M2, M3, M4] 5",
                "zwaarte [V1, V3, V7] 3"), trace, "één rij per vorm, en geen rij voor een regel zonder vorm");
        assertEquals(70, r.trace().totaal(), "het rapport telt elke vuring van een regel van een vorm");
        for (ScenarioResultaat.DekkingRegel d : r.dekking())
            assertEquals(DekkingSoort.GEVUURD, d.soort(), () -> d.regel() + " vuurde: " + trace);
        assertEquals(6, r.dekking().size(), () -> r.dekking().toString());
    }

    @Test void a_form_name_with_a_double_point_stays_whole() throws Exception {
        Path borgdeel = GOLDEN.resolve("anchor").resolve("jutterswet-borgdeel");
        Path d = Files.createTempDirectory("scenario-rapport");
        String bron = Files.readString(borgdeel.resolve("ir.json"));
        String renamed = bron.replace("\"name\": \"borgdeel van een jutter\"", "\"name\": \"premie: basis\"")
                .replace("\"name\": \"jaardeel van een jutter\"", "\"name\": \"toeslag: basis\"");
        assertTrue(renamed.contains("\"premie: basis\"") && renamed.contains("\"toeslag: basis\""),
                "de hernoeming moet beide regels raken");
        Path ir = Files.writeString(d.resolve("ir.json"), renamed);
        ScenarioResultaat r = CaseRunner.rapport(ir, List.of(borgdeel.resolve("case1.json"))).get(0);
        assertEquals(Status.PASS, r.status(), r::toString);
        List<String> trace = r.trace().vuringen().stream().map(v -> v.regel() + " " + v.labels() + " " + v.aantal())
                .sorted().toList();
        assertEquals(List.of("premie: basis [J1] 1", "toeslag: basis [J1] 1"), trace,
                "twee vormen, twee rijen, elk met de hele naam");
        assertEquals(DekkingSoort.GEVUURD, dekking(r, "premie: basis"));
        assertEquals(DekkingSoort.GEVUURD, dekking(r, "toeslag: basis"));
    }

    @Test void every_version_of_a_rule_has_its_own_coverage() throws Exception {
        Path jutheffing = GOLDEN.resolve("anchor").resolve("jutterswet-jutheffing");
        ScenarioResultaat r = CaseRunner.rapport(jutheffing.resolve("ir.json"),
                List.of(jutheffing.resolve("case1-tarief-2025.json"))).get(0);
        assertEquals(Status.PASS, r.status(), r::toString);
        List<String> trace = r.trace().vuringen().stream().map(v -> v.regel() + " " + v.labels() + " " + v.aantal())
                .sorted().toList();
        assertEquals(List.of("jutheffing naar jaartarief [A1] 1", "registratiegetal bij aangifte [A1] 1"), trace,
                "de versies van één regel delen een rij");
        assertEquals(DekkingSoort.GEVUURD, dekking(r, "jutheffing naar jaartarief #1"));
        assertEquals(DekkingSoort.NIET_GERAAKT, dekking(r, "jutheffing naar jaartarief #2"),
                "de tweede versie geldt niet in 2025 en vuurt dus niet");
    }

    private static DekkingSoort dekking(ScenarioResultaat r, String regel) {
        return r.dekking().stream().filter(d -> d.regel().equals(regel)).findFirst()
                .orElseThrow(() -> new AssertionError("geen dekking voor " + regel + ": " + r.dekking())).soort();
    }

    @Test void a_missed_expectation_carries_expected_and_actual() throws Exception {
        Path d = Files.createTempDirectory("scenario-rapport");
        String bron = Files.readString(EXCLUSION.resolve("case1-geweigerd.json"));
        String mutated = bron.replace("\"kind\": \"characteristicPresent\",\n      \"label\": \"M\",\n      \"characteristic\": \"uitgesloten\"",
                "\"kind\": \"characteristicAbsent\",\n      \"label\": \"M\",\n      \"characteristic\": \"uitgesloten\"");
        assertNotEquals(bron, mutated, "de mutatie moet het casebestand raken");
        Path c = Files.writeString(d.resolve("case1-gemuteerd.json"), mutated);
        ScenarioResultaat r = CaseRunner.rapport(EXCLUSION.resolve("ir.json"), List.of(c)).get(0);
        assertEquals(Status.FAIL, r.status());
        assertEquals(new VerwachtingResultaat("characteristicAbsent M uitgesloten", "afwezig", "aanwezig", false, 1),
                r.verwachtingen().get(0));
        assertTrue(r.verwachtingen().subList(1, 3).stream().allMatch(VerwachtingResultaat::ok));
        assertEquals("case1-gemuteerd.json: kenmerk uitgesloten: verwacht afwezig, werkelijk aanwezig", r.diagnose(),
                "de diagnose is de verdictregel van de replay");
    }

    /** A case may write a member in another capitalisation than its declaration, "schouwPlicht" for a declared
     *  "schouwplicht": the reference interpreter and the ASP runner read it as the same member and pass both cases below
     *  (measured 2026-10-14), and the replay reads the bean through the declared name. The report reads its actual side
     *  the same way, for a value, a characteristic and a timeline. <b>Red before the repair</b> (end lens on card
     *  drl-teksttak-op-truth-maintenance; measured 2026-10-14 on {@code 10ccd8f9}): both cases FAIL with "geen getter
     *  …", the getter of the written name, where the replay plays them. */
    @Test void a_member_in_another_capital_reads_the_declared_member() throws Exception {
        Path d = Files.createTempDirectory("scenario-rapport");
        Path schouwrooster = GOLDEN.resolve("jutterswet-schouwrooster");
        Path dayCount = GOLDEN.resolve("jutterswet-dagentelling-rgs");
        String waarde = Files.readString(schouwrooster.resolve("case2.json"));
        String valueMutated = waarde.replace("\"attribute\": \"schouwplicht\"", "\"attribute\": \"schouwPlicht\"")
                .replace("\"characteristic\": \"schouwvrij\"", "\"characteristic\": \"schouwVrij\"");
        String timeline = Files.readString(dayCount.resolve("case2-jaargrens.json"));
        String timelineMutated = timeline.replace("\"attribute\": \"borgdagen\"", "\"attribute\": \"borgDagen\"");
        assertNotEquals(waarde, valueMutated, "de mutatie moet het casebestand raken");
        assertNotEquals(timeline, timelineMutated, "de mutatie moet het casebestand raken");
        Path[][] paren = {
                { schouwrooster, Files.writeString(d.resolve("case2-hoofdletters.json"), valueMutated) },
                { dayCount, Files.writeString(d.resolve("case2-jaargrens-hoofdletters.json"), timelineMutated) } };
        List<String> fouten = new ArrayList<>();
        for (Path[] pair : paren) {
            Path ir = pair[0].resolve("ir.json");
            assertNull(CaseRunner.playCase(ir, TmsDocuments.JSON.readTree(pair[1].toFile())),
                    "de replay speelt " + pair[1].getFileName());
            ScenarioResultaat r = CaseRunner.rapport(ir, List.of(pair[1])).get(0);
            if (r.status() != Status.PASS) fouten.add(r.status() + " " + r.diagnose());
            for (VerwachtingResultaat v : r.verwachtingen())
                if (!v.verwacht().equals(v.werkelijk())) fouten.add(r.naam() + ": " + v);
        }
        assertEquals(List.of(), fouten, "het rapport van de cases met een lid in een andere hoofdletter");
    }

    @Test void a_case_that_the_replay_can_not_play_fails_only_itself() throws Exception {
        // A timeline expectation whose first boundary is off the attribute's month grid: the case reader refuses it
        // with a runtime exception, as ref and clingo refuse the file. That case is FAIL with the exception as its
        // reason, and the report goes on, so the case beside it is reported as the replay plays it.
        Path golden = GOLDEN.resolve("jutterswet-dagentelling-rgs");
        Path d = Files.createTempDirectory("scenario-rapport");
        String bron = Files.readString(golden.resolve("case1-voorbeeld.json"));
        String mutated = bron.replaceFirst("\"from\": \"2024-01-01\",\n          \"to\": \"2024-02-01\"",
                "\"from\": \"2024-01-05\",\n          \"to\": \"2024-02-01\"");
        assertNotEquals(bron, mutated, "de mutatie moet het casebestand raken");
        Path outside = Files.writeString(d.resolve("case1-buiten-raster.json"), mutated);
        List<ScenarioResultaat> rapport =
                CaseRunner.rapport(golden.resolve("ir.json"), List.of(outside, golden.resolve("case2-jaargrens.json")));
        assertEquals(2, rapport.size(), rapport::toString);
        ScenarioResultaat red = van(rapport, "case1-buiten-raster.json");
        assertEquals(Status.FAIL, red.status(), red::toString);
        assertEquals("case1-buiten-raster.json: ERROR IllegalArgumentException: "
                + "segmentgrens op 'borgdagen' ligt niet op het MAAND-raster", red.diagnose());
        assertTrue(red.verwachtingen().isEmpty() && red.trace().vuringen().isEmpty() && red.dekking().isEmpty(),
                "een case die niet uitspeelde draagt geen verdicten: " + red);
        assertEquals(Status.PASS, van(rapport, "case2-jaargrens.json").status());
    }

    @Test void a_document_that_the_backend_does_not_carry_is_a_marker() throws Exception {
        Path bootberging = GOLDEN.resolve("anchor").resolve("jutterswet-bootberging");
        List<ScenarioResultaat> rapport = CaseRunner.rapport(bootberging.resolve("ir.json"), cases(bootberging));
        assertFalse(rapport.isEmpty());
        for (ScenarioResultaat r : rapport) {
            assertEquals(Status.MARKER, r.status(), r::toString);
            assertTrue(r.diagnose().contains("transpiler SKIP: regel: laagste vondstgewicht van een jutter"), r::diagnose);
            assertTrue(r.verwachtingen().isEmpty() && r.trace().vuringen().isEmpty() && r.dekking().isEmpty());
        }
    }
}
