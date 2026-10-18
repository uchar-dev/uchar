package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §7.1 TOTAAL over a TIMELINE whose segment values are dates — the timeline half of
 * {@link DateSetFoldWitnessTest} (card temporele-datumvouw).
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
 * played leeg (and, on the clip, crashed — {@link TimelineClipFoldWitnessTest}). Since that card the exec
 * branch asks the DRL marker's own question of the DECLARATION, so both branches refuse the document in one
 * sentence, whatever the case supplies; the rows pin each branch apart as before.
 *
 * <p>The last row is the narrowness guard: the numeric originals of both documents still play in both
 * branches.
 */
class TimelineDateFoldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path NEGATIEF = Path.of("..", "..", "corpus", "negatief");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static final String TOTAL = "wf091-tijdlijntotaal-datum";
    private static final String CLIP = "wf091-klipvouw-datum";

    // The inputs of the two former red cases (corpus/rood/datumtijdlijn-totaalvouw and
    // corpus/rood/datumtijdlijn-klipvouw, case1.json), without their expectedFault pins, which were ref's and
    // clingo's.
    private static final String TOTAL_CASE = """
            {"format": "uchar-case-v0", "asOfDate": "2026-03-15",
             "input": {"instances": [{"label": "J1", "objectType": "Jutter",
                 "attributes": {"bewaarvergoeding per maand": {"date": "2024-01-15"}},
                 "roles": {}, "characteristics": []}], "parameters": {}}}
            """;
    private static final String CLIP_CASE = """
            {"format": "uchar-case-v0", "asOfDate": null,
             "input": {"instances": [{"label": "K1", "objectType": "Klipjutter",
                 "attributes": {"wachtvergoeding per maand": {"timeline": [
                     {"from": "2023-07-01", "to": "2023-09-01", "value": {"date": "2024-01-15"}}]}},
                 "roles": {}, "characteristics": [],
                 "characteristicTimelines": {"wachtplichtig": [{"from": "2023-07-01", "to": "2023-08-01"}]}}],
              "parameters": {}}}
            """;

    @Test void the_own_timeline_total_over_dates_is_refused_by_name() throws Exception {
        refuses(TOTAL, JSON.readTree(TOTAL_CASE),
                "regel: bewaargeld van een jutter — een totaal over de eigen tijdlijn ('het totaal van zijn bewaarvergoeding per"
                        + " maand') over 'bewaarvergoeding per maand', dat geen getal is (WF-091)");
    }

    @Test void the_during_clip_over_dates_is_refused_by_name() throws Exception {
        refuses(CLIP, JSON.readTree(CLIP_CASE),
                "regel: wachtsom van een klipjutter — een totaal over de eigen tijdlijn ('het totaal van zijn"
                        + " wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is') over 'wachtvergoeding"
                        + " per maand', dat geen getal is (WF-091)");
    }

    @Test void the_refusal_hangs_on_the_declaration_and_not_on_the_case() throws Exception {
        // J1 without a value: ref answers leeg, but the document still asks both branches to scale a date, so both
        // decline — the same document-level stance as the ASP backend (the exec branch played leeg until the card
        // ref-klipvouw-nul).
        ObjectNode empty = (ObjectNode) JSON.readTree(TOTAL_CASE);
        ((ObjectNode) empty.get("input").get("instances").get(0)).set("attributes", JSON.createObjectNode());
        empty.set("expected", JSON.readTree(
                "[{\"kind\":\"attributeEmpty\",\"label\":\"J1\",\"attribute\":\"bewaargeld\"}]"));
        // Until card drl-teksttak-op-truth-maintenance the DRL-text branch named it in its own words ("vouw TOTAAL over
        // de tijdlijn 'bewaarvergoeding per maand' …"); both branches refuse in the sentence of the analysis since.
        BothBranches.refused(NEGATIEF.resolve(TOTAL).resolve("ir.json"), empty, "een totaal over de eigen tijdlijn ('het"
                + " totaal van zijn bewaarvergoeding per maand') over 'bewaarvergoeding per maand', dat geen getal is"
                + " (WF-091)", "the refusal hangs on the declaration");
    }

    @Test void the_numeric_originals_stay_playing() throws Exception {
        // NARROWNESS: the same two arms over numeric timelines, untouched.
        BothBranches.green(GOLDEN.resolve("jutterswet-tempototaal").resolve("ir.json"),
                caseDocument(GOLDEN, "jutterswet-tempototaal", "case.json"), "own-subject timeline total over amounts");
        BothBranches.green(GOLDEN.resolve("jutterswet-erfwacht").resolve("ir.json"),
                caseDocument(GOLDEN, "jutterswet-erfwacht", "case.json"), "gedurende clip over amounts");
    }

    // ---------------- fixtures ----------------

    /** Both branches refuse the negatief document {@code dir} in the one sentence of the analysis, which names
     *  {@code tms}; until card drl-teksttak-op-truth-maintenance the DRL-text branch named the fold in its own words. */
    private static void refuses(String dir, JsonNode c, String tms) throws Exception {
        Path ir = NEGATIEF.resolve(dir).resolve("ir.json");
        assertTrue(Files.exists(ir), "negatief ir.json present: " + ir.toAbsolutePath());
        BothBranches.refused(ir, c, tms, "the timeline total over dates");
    }

    private static JsonNode caseDocument(Path zone, String dir, String file) throws Exception {
        Path p = zone.resolve(dir).resolve(file);
        assertTrue(Files.exists(p), "case present: " + p.toAbsolutePath());
        return JSON.readTree(Files.readString(p));
    }
}
