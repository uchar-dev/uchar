package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.engine.ScenarioResultaat;
import dev.uchar.regelspraak.engine.ScenarioResultaat.DekkingSoort;
import dev.uchar.regelspraak.engine.ScenarioResultaat.Status;
import dev.uchar.regelspraak.engine.ScenarioResultaat.VerwachtingResultaat;
import dev.uchar.regelspraak.engine.ScenarioResultaat.Vuring;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
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
 * Until card tms-omschakeling the second fixture was {@code anchor/jutterswet-erflijn}, which the truth-maintenance
 * branch plays; the rows hold what both the destructive and the truth-maintenance exec-model branch report, since the
 * report replays on the exec-model branch and the switch-over changes which of the two that is: the trace is pinned by
 * its labels and counts, not by the names of the rules a branch fires.
 */
class ScenarioRapportWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path UITSLUITING = GOLDEN.resolve("jutterswet-uitsluiting-rgs");

    private static List<Path> cases(Path golden) throws Exception {
        try (Stream<Path> s = Files.list(golden)) {
            return s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
    }

    private static ScenarioResultaat van(List<ScenarioResultaat> rapport, String naam) {
        return rapport.stream().filter(r -> r.naam().equals(naam)).findFirst()
                .orElseThrow(() -> new AssertionError("geen rapport voor " + naam + ": " + rapport));
    }

    @Test void een_golden_die_speelt_rapporteert_verdicten_vuringen_en_dekking() throws Exception {
        List<ScenarioResultaat> rapport = CaseRunner.rapport(UITSLUITING.resolve("ir.json"), cases(UITSLUITING));
        assertEquals(4, rapport.size());
        assertTrue(rapport.stream().allMatch(ScenarioResultaat::pass), () -> rapport.toString());

        ScenarioResultaat geweigerd = van(rapport, "case1-geweigerd.json");
        assertEquals(List.of(
                new VerwachtingResultaat("characteristicPresent M uitgesloten", "aanwezig", "aanwezig", true, 1),
                new VerwachtingResultaat("characteristicAbsent M penninggerechtigd", "afwezig", "afwezig", true, 2),
                new VerwachtingResultaat("consistent", "consistent", "consistent", true, 3)),
                geweigerd.verwachtingen());
        assertEquals("", geweigerd.diagnose());
        List<String> gevuurd = geweigerd.trace().vuringen().stream().map(Vuring::regel).toList();
        assertFalse(gevuurd.isEmpty(), "de defaultsgroep vuurt");
        assertTrue(geweigerd.trace().vuringen().stream().allMatch(v -> v.labels().equals(List.of("M")) && v.aantal() > 0),
                () -> geweigerd.trace().toString());
        // the rule reads 'niet uitgesloten': with the refusal standing it never matches, without it it fires
        assertEquals(DekkingSoort.NIET_GERAAKT, dekking(geweigerd, "penninggerechtigd tenzij uitgesloten"));
        assertEquals(DekkingSoort.GEVUURD, dekking(van(rapport, "case4-kaal.json"), "penninggerechtigd tenzij uitgesloten"));
    }

    private static DekkingSoort dekking(ScenarioResultaat r, String regel) {
        return r.dekking().stream().filter(d -> d.regel().equals(regel)).findFirst()
                .orElseThrow(() -> new AssertionError("geen dekking voor " + regel + ": " + r.dekking())).soort();
    }

    @Test void een_gemiste_verwachting_draagt_verwacht_en_werkelijk() throws Exception {
        Path d = Files.createTempDirectory("scenario-rapport");
        String bron = Files.readString(UITSLUITING.resolve("case1-geweigerd.json"));
        String gemuteerd = bron.replace("\"kind\": \"characteristicPresent\",\n      \"label\": \"M\",\n      \"characteristic\": \"uitgesloten\"",
                "\"kind\": \"characteristicAbsent\",\n      \"label\": \"M\",\n      \"characteristic\": \"uitgesloten\"");
        assertNotEquals(bron, gemuteerd, "de mutatie moet het casebestand raken");
        Path c = Files.writeString(d.resolve("case1-gemuteerd.json"), gemuteerd);
        ScenarioResultaat r = CaseRunner.rapport(UITSLUITING.resolve("ir.json"), List.of(c)).get(0);
        assertEquals(Status.FAIL, r.status());
        assertEquals(new VerwachtingResultaat("characteristicAbsent M uitgesloten", "afwezig", "aanwezig", false, 1),
                r.verwachtingen().get(0));
        assertTrue(r.verwachtingen().subList(1, 3).stream().allMatch(VerwachtingResultaat::ok));
        assertEquals("case1-gemuteerd.json: kenmerk uitgesloten: verwacht afwezig, werkelijk aanwezig", r.diagnose(),
                "de diagnose is de verdictregel van de replay");
    }

    @Test void een_case_die_de_replay_niet_kan_spelen_faalt_alleen_zelf() throws Exception {
        // A timeline expectation whose first boundary is off the attribute's month grid: the case reader refuses it
        // with a runtime exception, as ref and clingo refuse the file. That case is FAIL with the exception as its
        // reason, and the report goes on, so the case beside it is reported as the replay plays it.
        Path golden = GOLDEN.resolve("jutterswet-dagentelling-rgs");
        Path d = Files.createTempDirectory("scenario-rapport");
        String bron = Files.readString(golden.resolve("case1-voorbeeld.json"));
        String gemuteerd = bron.replaceFirst("\"from\": \"2024-01-01\",\n          \"to\": \"2024-02-01\"",
                "\"from\": \"2024-01-05\",\n          \"to\": \"2024-02-01\"");
        assertNotEquals(bron, gemuteerd, "de mutatie moet het casebestand raken");
        Path buiten = Files.writeString(d.resolve("case1-buiten-raster.json"), gemuteerd);
        List<ScenarioResultaat> rapport =
                CaseRunner.rapport(golden.resolve("ir.json"), List.of(buiten, golden.resolve("case2-jaargrens.json")));
        assertEquals(2, rapport.size(), rapport::toString);
        ScenarioResultaat rood = van(rapport, "case1-buiten-raster.json");
        assertEquals(Status.FAIL, rood.status(), rood::toString);
        assertEquals("case1-buiten-raster.json: ERROR IllegalArgumentException: "
                + "segmentgrens op 'borgdagen' ligt niet op het MAAND-raster", rood.diagnose());
        assertTrue(rood.verwachtingen().isEmpty() && rood.trace().vuringen().isEmpty() && rood.dekking().isEmpty(),
                "een case die niet uitspeelde draagt geen verdicten: " + rood);
        assertEquals(Status.PASS, van(rapport, "case2-jaargrens.json").status());
    }

    @Test void een_document_dat_de_backend_niet_draagt_is_marker() throws Exception {
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
