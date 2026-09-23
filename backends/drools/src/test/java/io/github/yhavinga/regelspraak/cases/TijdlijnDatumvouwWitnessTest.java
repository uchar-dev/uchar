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
 * Both documents are verifier-CLEAN. A whole-file compile failure names the generated text, not the
 * construct; the marker names the fold, the timeline and the member type.
 *
 * <p>The two branches refuse at DIFFERENT phases and in different words, and the rows pin that rather than
 * paper over it: the exec branch's case loader declines a non-numeric segment before its lowering is ever
 * asked (a Segment holds a BigDecimal), so the shared {@code BeideTakken.geweigerd} — one fragment for both —
 * does not fit here. The DRL marker is asked of the DECLARATION, so it holds on a case that supplies no
 * value at all, where the exec branch plays leeg exactly as ref does.
 *
 * <p>The last row is the narrowness guard: the numeric originals of both documents still play in both
 * branches.
 */
class TijdlijnDatumvouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path ROOD = Path.of("..", "..", "corpus", "rood");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static final String TOTAAL = "datumtijdlijn-totaalvouw";
    private static final String KLIP = "datumtijdlijn-klipvouw";

    @Test void de_eigen_tijdlijntotaal_over_datums_weigert_bij_naam() throws Exception {
        weigert(TOTAAL, zaak(ROOD, TOTAAL, "case1.json"),
                "vouw TOTAAL over de tijdlijn 'bewaarvergoeding per maand' van Jutter: de segmentwaarden zijn"
                        + " van type Datum in dagen en niet getalachtig",
                "non-numeric timeline value on 'bewaarvergoeding per maand'");
    }

    @Test void de_gedurende_klip_over_datums_weigert_bij_naam() throws Exception {
        weigert(KLIP, zaak(ROOD, KLIP, "case1.json"),
                "vouw TOTAAL over de tijdlijn 'wachtvergoeding per maand' van Klipjutter: de segmentwaarden zijn"
                        + " van type Datum in dagen en niet getalachtig",
                "non-numeric timeline value on 'wachtvergoeding per maand'");
    }

    @Test void de_drl_marker_hangt_aan_de_declaratie_en_niet_aan_de_casus() throws Exception {
        // J1 without a value: ref answers leeg and so does the exec branch, but the document still asks the
        // DRL branch to scale a date, so it declines — the same document-level stance as the ASP backend.
        ObjectNode leeg = (ObjectNode) zaak(ROOD, TOTAAL, "case1.json").deepCopy();
        ((ObjectNode) leeg.get("input").get("instances").get(0)).set("attributes", JSON.createObjectNode());
        leeg.remove("expectedFault");
        leeg.set("expected", JSON.readTree(
                "[{\"kind\":\"attributeEmpty\",\"label\":\"J1\",\"attribute\":\"bewaargeld\"}]"));
        Path ir = ROOD.resolve(TOTAAL).resolve("ir.json");
        assertTrue(CaseRunner.speelCase(ir, leeg, false) == null, "exec-model plays the empty case leeg");
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

    private static void weigert(String rood, JsonNode c, String drl, String exec) throws Exception {
        Path ir = ROOD.resolve(rood).resolve("ir.json");
        assertTrue(Files.exists(ir), "red ir.json present: " + ir.toAbsolutePath());
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
