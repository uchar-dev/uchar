package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * The gedurende-clipped total (RUNTIME-SEMANTICS ch. 3.6) over a Date timeline, in the exec branch (card
 * ref-klipvouw-nul) — the exec half that {@link TijdlijnDatumvouwWitnessTest} left to its case loader.
 *
 * <p>RED FIRST, measured 2026-09-25 with the exec code of main 2f47d80e on {@code wf091-klipvouw-datum} (until
 * then the red case {@code datumtijdlijn-klipvouw}, byte-identical):
 * <pre>
 *   no value timeline      exec  ERROR ConsequenceException … java.lang.ClassCastException:
 *                                Cannot cast java.math.BigDecimal to java.time.LocalDate
 *   value clipped away     exec  EXCLUDE non-numeric timeline value on 'wachtvergoeding per maand' …
 *                          drl   EXCLUDE drl-transpiler MARKER … vouw TOTAAL over de tijdlijn … (both cases)
 * </pre>
 * Without a value the case loader has no date to refuse, the clip totals 0 (ch. 3.6: absence under a clip is 0),
 * and that BigDecimal reached a LocalDate field at fire. A crash names no form. The exec branch then asked the
 * DRL-text branch's own question of the DECLARATION ({@code Vouwwaarde.tijdlijnWeigering}, which went with the
 * destructive lowering at card drl-teksttak-op-truth-maintenance), so both branches
 * refuse the document before any case is read, in one sentence, whatever the case supplies. WF-091 rejects the
 * document since the same card, so this refusal is the backend's second line, not a corpus route.
 *
 * <p>The last row is the other side: a NUMERIC clip target without a value plays 0 in both branches, the ch. 3.6
 * norm {@code ref} and clingo answer and {@code golden/jutterswet-getijdenwacht} pins in the square.
 */
class TijdlijnKlipvouwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path KLIP_DATUM =
            Path.of("..", "..", "corpus", "negatief", "wf091-klipvouw-datum", "ir.json");
    private static final Path KLIP_GETAL =
            Path.of("..", "..", "corpus", "golden", "jutterswet-erfwacht", "ir.json");

    private static final String REDEN = "vouw TOTAAL over de tijdlijn 'wachtvergoeding per maand' van Klipjutter:"
            + " de segmentwaarden zijn van type Datum in dagen en niet getalachtig";

    /** The refusal of the exec-model branch, which names the rule and the verifier's rule. */
    private static final String TMS_REDEN = "regel: wachtsom van een klipjutter — een totaal over de eigen tijdlijn ('het"
            + " totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is') over"
            + " 'wachtvergoeding per maand', dat geen getal is (WF-091)";

    @Test void de_klip_zonder_waarde_over_datums_weigert_bij_naam() throws Exception {
        BeideTakken.geweigerd(KLIP_DATUM, zaak(null, "\"attributeEmpty\"", ""), TMS_REDEN,
                "no value timeline under the clip: a refusal, never a crash");
    }

    @Test void de_weggeklipte_datumwaarde_weigert_in_dezelfde_zin() throws Exception {
        BeideTakken.geweigerd(KLIP_DATUM, zaak("{\"date\": \"2024-01-15\"}", "\"attributeEmpty\"", ""), TMS_REDEN,
                "a value the clip removes entirely: the same document, the same sentence");
    }

    @Test void het_numerieke_klipdoel_zonder_waarde_speelt_nul() throws Exception {
        String nul = ", \"value\": {\"number\": \"0\"}";
        BeideTakken.groen(KLIP_GETAL, zaak(null, "\"attributeValue\"", nul), "no value timeline under the clip: 0");
        BeideTakken.groen(KLIP_GETAL, zaak("{\"number\": \"10\"}", "\"attributeValue\"", nul),
                "a value the clip removes entirely: 0");
    }

    // ---------------- fixtures ----------------

    /** K1 is wachtplichtig through July 2023. {@code waarde} null: no value timeline; otherwise one value on
     *  [2024-01-01, 2024-02-01), disjoint from the clip, so the clip removes all of it. */
    private static JsonNode zaak(String waarde, String soort, String verwacht) throws Exception {
        String attributen = waarde == null ? "{}"
                : "{\"wachtvergoeding per maand\": {\"timeline\": [{\"from\": \"2024-01-01\", \"to\": \"2024-02-01\","
                        + " \"value\": " + waarde + "}]}}";
        return JSON.readTree("""
                {"format": "uchar-case-v0", "asOfDate": null,
                 "input": {"instances": [{"label": "K1", "objectType": "Klipjutter", "attributes": %s,
                     "roles": {}, "characteristics": [],
                     "characteristicTimelines": {"wachtplichtig": [{"from": "2023-07-01", "to": "2023-08-01"}]}}],
                  "parameters": {}},
                 "expected": [{"kind": %s, "label": "K1", "attribute": "wachtsom"%s}]}
                """.formatted(attributen, soort, verwacht));
    }
}
