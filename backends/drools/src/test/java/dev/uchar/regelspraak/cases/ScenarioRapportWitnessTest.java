package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.engine.ScenarioResultaat;
import dev.uchar.regelspraak.engine.ScenarioResultaat.DekkingSoort;
import dev.uchar.regelspraak.engine.ScenarioResultaat.Status;
import dev.uchar.regelspraak.engine.ScenarioResultaat.VerwachtingResultaat;

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
 * Until card tms-omschakeling the second fixture was {@code anchor/jutterswet-erflijn}, which the exec-model branch
 * with truth maintenance plays.
 *
 * <p>The trace of the first case is pinned whole. The report counts a firing under the form of the document its rule
 * belongs to ({@code TmsTranspiler.vormVan}), so "defaults K:Jutter.uitgesloten" is the document's one defaults group,
 * which decides for M, its default and its agreement over the root in one row; a rule of no form keeps its full name,
 * here the standing of the characteristic that follows. Until the switch-over the trace was pinned by the name of the
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
        // a row names the case's objects its rules matched: M, by the default; the agreement of the group over the root
        // matches facts of the branch alone and adds none
        List<String> spoor = geweigerd.trace().vuringen().stream().map(v -> v.regel() + " " + v.labels() + " "
                + v.aantal()).sorted().toList();
        assertEquals(List.of("defaults K:Jutter.uitgesloten [M] 2",
                        "kenmerkstand Jutter.uitgesloten: waar uit een afleiding [M] 1"), spoor,
                "de defaultsgroep vuurt haar default en haar overeenstemming op M, en het kenmerk volgt");
        assertEquals(DekkingSoort.GEVUURD, dekking(geweigerd, "defaults K:Jutter.uitgesloten"));
        // the rule reads 'niet uitgesloten': with the refusal standing it never matches, without it it fires
        assertEquals(DekkingSoort.NIET_GERAAKT, dekking(geweigerd, "penninggerechtigd tenzij uitgesloten"));
        assertEquals(DekkingSoort.GEVUURD, dekking(van(rapport, "case4-kaal.json"), "penninggerechtigd tenzij uitgesloten"));
    }

    @Test void een_vuring_telt_onder_de_vorm_van_haar_regel() throws Exception {
        Path premiestaffel = GOLDEN.resolve("jutterswet-premiestaffel");
        ScenarioResultaat r = CaseRunner.rapport(premiestaffel.resolve("ir.json"),
                List.of(premiestaffel.resolve("case1-staffel.json"))).get(0);
        assertEquals(Status.PASS, r.status(), r::toString);
        // the labels sorted: a row is pinned by what its rules bound, not by the order of the agenda
        List<String> spoor = r.trace().vuringen().stream()
                .map(v -> v.regel() + " " + v.labels().stream().sorted().toList() + " " + v.aantal()).sorted().toList();
        assertEquals(List.of(
                "defaults A:Controle.controlepremie [C1, C2, C3, C4] 6",
                "defaults A:Jutter.premie [J1, J2, J3, J4, J5, V1, V2, V3, V4, V5, V6, V7, V8, V9] 20",
                "defaults A:Melding.gemelde premie [M1, M2, M3, M4] 7",
                "defaults K:Jutter.ijverig [J1, J2, J3, J4, J5, V1, V2, V3, V4, V5, V6, V7, V8, V9] 29",
                "defaults K:Melding.dringend [M1, M2, M3, M4] 5",
                "kenmerkstand Vondst.zwaar: onwaar [V2, V4, V5, V6, V8, V9] 6",
                "kenmerkstand Vondst.zwaar: waar uit een afleiding [V1, V3, V7] 3",
                "padstand Controle.melding.vondst.jutter naar Jutter.premie: link aanwezig [C1, C2] 3",
                "padstand Controle.melding.vondst.jutter naar Jutter.premie: link afwezig [C3, C4] 2",
                "padstand Melding.vondst.jutter naar Jutter.premie: link aanwezig [M1, M2, M3] 4",
                "padstand Melding.vondst.jutter naar Jutter.premie: link afwezig [M4] 1",
                "parameterstand zwaartegrens [] 1",
                "stand Jutter.premie: uit een afleiding [J1, J2, J3, J4, J5] 6",
                "zwaarte [V1, V3, V7] 3"), spoor, "één rij per vorm, en per regel zonder vorm haar eigen naam");
        assertEquals(96, r.trace().totaal(), "het rapport telt elke vuring, onder welke rij ook");
        for (ScenarioResultaat.DekkingRegel d : r.dekking())
            assertEquals(DekkingSoort.GEVUURD, d.soort(), () -> d.regel() + " vuurde: " + spoor);
        assertEquals(6, r.dekking().size(), () -> r.dekking().toString());
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
