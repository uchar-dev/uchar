package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §7.1 TOTAAL over a TIMELINE whose segment values are dates — the timeline half of
 * {@link DatumSetvouwWitnessTest} (card temporele-datumvouw).
 *
 * <p>RED FIRST, measured 2026-09-23 on the jar built from main b91df009, on the two red witnesses below:
 * <pre>
 *   own-subject total   drl   EXCLUDE drl compile: … Unable to Analyse Expression
 *                             if (rsTijdTotaal($subject.getBewaarvergoedingPerMaand(), "MAAND") != null && …
 *   gedurende clip      drl   EXCLUDE drl compile: … Unable to Analyse Expression
 *                             if (rsTijdTotaal(rsTijdKnip($subject.getWachtvergoedingPerMaand(), …
 * </pre>
 * Both documents were verifier-CLEAN then. A whole-file compile failure names the generated text, not the
 * construct; the marker names the fold, the timeline and the member type. WF-091 rejects both since the card
 * ref-klipvouw-nul (a total over a Date operand), so they are negatief witnesses without a case file; this
 * backend does not run the verifier, and the inputs of their former red cases are held below.
 *
 * <p>Until the card ref-klipvouw-nul the two branches refused at DIFFERENT phases: the exec branch's case
 * loader declined a non-numeric segment before its lowering was asked, and on a case without a value it
 * played leeg (and, on the clip, crashed — {@link TijdlijnKlipvouwWitnessTest}). Since that card the exec
 * branch asks the DRL marker's own question of the DECLARATION, so both branches refuse the document in one
 * sentence, whatever the case supplies; the rows pin each branch apart as before.
 *
 * <p>The last row is the narrowness guard: the numeric originals of both documents still play in both
 * branches.
 */
class TijdlijnDatumvouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path NEGATIEF = Path.of("..", "..", "corpus", "negatief");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static final String TOTAAL = "wf091-tijdlijntotaal-datum";
    private static final String KLIP = "wf091-klipvouw-datum";

    // The inputs of the two former red cases (corpus/rood/datumtijdlijn-totaalvouw and
    // corpus/rood/datumtijdlijn-klipvouw, case1.json), without their expectedFault pins, which were ref's and
    // clingo's.
    private static final String TOTAAL_ZAAK = """
            {"format": "uchar-case-v0", "asOfDate": "2026-03-15",
             "input": {"instances": [{"label": "J1", "objectType": "Jutter",
                 "attributes": {"bewaarvergoeding per maand": {"date": "2024-01-15"}},
                 "roles": {}, "characteristics": []}], "parameters": {}}}
            """;
    private static final String KLIP_ZAAK = """
            {"format": "uchar-case-v0", "asOfDate": null,
             "input": {"instances": [{"label": "K1", "objectType": "Klipjutter",
                 "attributes": {"wachtvergoeding per maand": {"timeline": [
                     {"from": "2023-07-01", "to": "2023-09-01", "value": {"date": "2024-01-15"}}]}},
                 "roles": {}, "characteristics": [],
                 "characteristicTimelines": {"wachtplichtig": [{"from": "2023-07-01", "to": "2023-08-01"}]}}],
              "parameters": {}}}
            """;

    @Test void de_eigen_tijdlijntotaal_over_datums_weigert_bij_naam() throws Exception {
        weigert(TOTAAL, JSON.readTree(TOTAAL_ZAAK),
                "vouw TOTAAL over de tijdlijn 'bewaarvergoeding per maand' van Jutter: de segmentwaarden zijn"
                        + " van type Datum in dagen en niet getalachtig",
                "vouw TOTAAL over de tijdlijn 'bewaarvergoeding per maand' van Jutter: de segmentwaarden zijn"
                        + " van type Datum in dagen en niet getalachtig");
    }

    @Test void de_gedurende_klip_over_datums_weigert_bij_naam() throws Exception {
        weigert(KLIP, JSON.readTree(KLIP_ZAAK),
                "vouw TOTAAL over de tijdlijn 'wachtvergoeding per maand' van Klipjutter: de segmentwaarden zijn"
                        + " van type Datum in dagen en niet getalachtig",
                "vouw TOTAAL over de tijdlijn 'wachtvergoeding per maand' van Klipjutter: de segmentwaarden zijn"
                        + " van type Datum in dagen en niet getalachtig");
    }

    @Test void de_drl_marker_hangt_aan_de_declaratie_en_niet_aan_de_casus() throws Exception {
        // J1 without a value: ref answers leeg, but the document still asks both branches to scale a date, so both
        // decline — the same document-level stance as the ASP backend (the exec branch played leeg until the card
        // ref-klipvouw-nul).
        ObjectNode leeg = (ObjectNode) JSON.readTree(TOTAAL_ZAAK);
        ((ObjectNode) leeg.get("input").get("instances").get(0)).set("attributes", JSON.createObjectNode());
        leeg.remove("expectedFault");
        leeg.set("expected", JSON.readTree(
                "[{\"kind\":\"attributeEmpty\",\"label\":\"J1\",\"attribute\":\"bewaargeld\"}]"));
        Path ir = NEGATIEF.resolve(TOTAAL).resolve("ir.json");
        CaseRunner.Excluded x = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, leeg, false));
        assertTrue(x.getMessage().contains("vouw TOTAAL over de tijdlijn 'bewaarvergoeding per maand'"),
                "exec-model names the form: " + x.getMessage());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, leeg, true));
        assertTrue(e.getMessage().contains("vouw TOTAAL over de tijdlijn 'bewaarvergoeding per maand'"),
                "drl-tekst names the form: " + e.getMessage());
    }

    @Test void de_getalwaardige_originelen_blijven_spelen() throws Exception {
        // NARROWNESS: the same two arms over numeric timelines, untouched.
        BeideTakken.groen(GOLDEN.resolve("jutterswet-tempototaal").resolve("ir.json"),
                zaak(GOLDEN, "jutterswet-tempototaal", "case.json"), "own-subject timeline total over amounts");
        BeideTakken.groen(GOLDEN.resolve("jutterswet-erfwacht").resolve("ir.json"),
                zaak(GOLDEN, "jutterswet-erfwacht", "case.json"), "gedurende clip over amounts");
    }

    // ---------------- fixtures ----------------

    private static void weigert(String dir, JsonNode c, String drl, String exec) throws Exception {
        Path ir = NEGATIEF.resolve(dir).resolve("ir.json");
        assertTrue(Files.exists(ir), "negatief ir.json present: " + ir.toAbsolutePath());
        CaseRunner.Excluded d = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, c, true));
        assertTrue(d.getMessage().contains(drl), "drl-tekst names '" + drl + "': " + d.getMessage());
        CaseRunner.Excluded x = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, c, false));
        assertTrue(x.getMessage().contains(exec), "exec-model names '" + exec + "': " + x.getMessage());
    }

    private static JsonNode zaak(Path zone, String dir, String bestand) throws Exception {
        Path p = zone.resolve(dir).resolve(bestand);
        assertTrue(Files.exists(p), "case present: " + p.toAbsolutePath());
        return JSON.readTree(Files.readString(p));
    }
}
