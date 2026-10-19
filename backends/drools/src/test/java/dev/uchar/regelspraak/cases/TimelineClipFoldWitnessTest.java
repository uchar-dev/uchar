package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * The gedurende-clipped total (RUNTIME-SEMANTICS ch. 3.6) over a Date timeline, on both branches (card
 * ref-klipvouw-nul) — the half that {@link TimelineDateFoldWitnessTest} left to its case loader.
 *
 * <p>The analysis asks the question of the DECLARATION: a total over a timeline takes a member whose value is a number
 * ({@code TmsTranspiler.integrable}). So both branches refuse the document when they build it, before any case is
 * read, in one sentence that names the rule and WF-091, whatever the case supplies. WF-091 rejects the document since
 * card ref-klipvouw-nul, so this refusal is the backend's second line, not a corpus route.
 *
 * <p>RED FIRST, measured 2026-09-25 with the exec code of main 2f47d80e on {@code wf091-clip-fold-date} (until
 * then the red case {@code datumtijdlijn-klipvouw}, byte-identical):
 * <pre>
 *   no value timeline      exec  ERROR ConsequenceException … java.lang.ClassCastException:
 *                                Cannot cast java.math.BigDecimal to java.time.LocalDate
 *   value clipped away     exec  EXCLUDE non-numeric timeline value on 'wachtvergoeding per maand' …
 *                          drl   EXCLUDE drl-transpiler MARKER … vouw TOTAAL over de tijdlijn … (both cases)
 * </pre>
 * Without a value the case loader had no date to refuse, the clip totalled 0 (ch. 3.6: absence under a clip is 0),
 * and that BigDecimal reached a LocalDate field at fire; a crash names no form. The exec branch of that day then asked
 * the question of the declaration that the destructive DRL-text lowering asked ({@code Vouwwaarde.tijdlijnWeigering},
 * gone with that lowering at card drl-teksttak-op-truth-maintenance), until the analysis with truth maintenance took
 * it over.
 *
 * <p>The last row is the other side: a NUMERIC clip target without a value plays 0 in both branches, the ch. 3.6
 * norm {@code ref} and clingo answer and {@code golden/jutterswet-getijdenwacht} pins in the square.
 */
class TimelineClipFoldWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CLIP_DATE =
            Path.of("..", "..", "corpus", "negative", "wf091-clip-fold-date", "ir.json");
    private static final Path CLIP_NUMBER =
            Path.of("..", "..", "corpus", "golden", "jutterswet-erfwacht", "ir.json");

    /** The refusal of the analysis, on both branches, which names the rule and the verifier's rule. */
    private static final String TMS_REASON = "regel: wachtsom van een klipjutter — een totaal over de eigen tijdlijn ('het"
            + " totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is') over"
            + " 'wachtvergoeding per maand', dat geen getal is (WF-091)";

    @Test void the_clip_without_value_over_dates_is_refused_by_name() throws Exception {
        BothBranches.refused(CLIP_DATE, caseDocument(null, "\"attributeEmpty\"", ""), TMS_REASON,
                "no value timeline under the clip: a refusal, never a crash");
    }

    @Test void the_clipped_date_value_is_refused_in_the_same_sentence() throws Exception {
        BothBranches.refused(CLIP_DATE, caseDocument("{\"date\": \"2024-01-15\"}", "\"attributeEmpty\"", ""), TMS_REASON,
                "a value the clip removes entirely: the same document, the same sentence");
    }

    @Test void the_numeric_clip_target_without_value_plays_zero() throws Exception {
        String zero = ", \"value\": {\"number\": \"0\"}";
        BothBranches.green(CLIP_NUMBER, caseDocument(null, "\"attributeValue\"", zero), "no value timeline under the clip: 0");
        BothBranches.green(CLIP_NUMBER, caseDocument("{\"number\": \"10\"}", "\"attributeValue\"", zero),
                "a value the clip removes entirely: 0");
    }

    // ---------------- fixtures ----------------

    /** K1 is wachtplichtig through July 2023. {@code waarde} null: no value timeline; otherwise one value on
     *  [2024-01-01, 2024-02-01), disjoint from the clip, so the clip removes all of it. */
    private static JsonNode caseDocument(String value, String category, String expected) throws Exception {
        String attributes = value == null ? "{}"
                : "{\"wachtvergoeding per maand\": {\"timeline\": [{\"from\": \"2024-01-01\", \"to\": \"2024-02-01\","
                        + " \"value\": " + value + "}]}}";
        return JSON.readTree("""
                {"format": "uchar-case-v0", "asOfDate": null,
                 "input": {"instances": [{"label": "K1", "objectType": "Klipjutter", "attributes": %s,
                     "roles": {}, "characteristics": [],
                     "characteristicTimelines": {"wachtplichtig": [{"from": "2023-07-01", "to": "2023-08-01"}]}}],
                  "parameters": {}},
                 "expected": [{"kind": %s, "label": "K1", "attribute": "wachtsom"%s}]}
                """.formatted(attributes, category, expected));
    }
}
