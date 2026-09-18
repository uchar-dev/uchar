package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.3.9 REPEATED-TYPE-PAIR witness: what the bean model derives when MORE THAN ONE feittype
 * relates the same two object types, and which readers may address the resulting fields.
 *
 * <p>The by-PAIR authority ({@code RegelSpraakModel.verbindendeRol}) answers null there — two or more
 * candidate roles, an honest ambiguity — and until this card {@code FactModelGenerator.leden} asked it
 * FIRST for every ordinary cross-type field. So a document whose author wrote a UNIQUE role name got a
 * bean with no reference field at all, and every reader refused: the fold's join, the §13.4.7
 * feitcreatie's three positions, the navigated read, the case-input write.
 *
 * <p>This file is the RED half, written before any production edit: it pins the two refusals the card
 * names, in both transpiler branches, on a model authored here out of one corpus golden.
 */
class HerhaaldTypepaarWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** FOUR feittypen relate Haven to Schuit (thuisligging, winterberging, keurberging, vlagvoering), so
     *  the by-pair search is ambiguous by construction while every written role name is unique. */
    private static final String DUBBELVERBINDING = "jutterswet-dubbelverbinding";

    // ---------------- the two refusals this card removes ----------------

    @Test void de_aggregatiejoin_over_een_herhaald_typepaar_heeft_geen_beanveld() throws Exception {
        // "het aantal liggers van de Haven": the element role `ligger` is written and resolves UNIQUELY by
        // name (verbindendeRolViaElementnaam -> the sister role `thuishaven`), so nothing about the SOURCE
        // is ambiguous. What refuses is the bean model: leden() asks verbindendeRol(Schuit, Haven) first,
        // which counts four candidate roles and answers null, so Schuit carries no Haven reference at all.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(alleenTelling(), JSON.readTree(tellingCase()), false));
        assertTrue(e.getMessage().contains("de aggregatie-join heeft geen beanveld"), e.getMessage());
        assertTrue(e.getMessage().contains("geen referentieveld"), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'thuishaven' op Schuit"), e.getMessage());
    }

    @Test void de_aggregatiejoin_weigert_ook_in_de_drl_tak() throws Exception {
        // The DRL-text branch markers the same join (scope() -> navVeld -> the same shared question), so the
        // package compiles and the accumulate silently disappears — which is exactly why the marker must be
        // refused rather than fired.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(alleenTelling(), JSON.readTree(tellingCase()), true));
        assertTrue(e.getMessage().contains("drl-transpiler"), e.getMessage());
        assertTrue(e.getMessage().contains("aggregatie-join"), e.getMessage());
        assertTrue(e.getMessage().contains("geen referentieveld"), e.getMessage());
    }

    @Test void de_feitcreatie_over_een_herhaald_typepaar_heeft_geen_beanveld() throws Exception {
        // §13.4.7 "Een winterschuit van een Haven is een ligger van de Haven": joinVeld `thuishaven` and
        // zetVeld `winterhaven` are two DIFFERENT fields on Schuit, both pointing at Haven, and both are
        // named by a uniquely written role. The same null from the same by-pair question refuses both.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(alleenCreatie(), JSON.readTree(creatieCase()), false));
        assertTrue(e.getMessage().contains("feitcreatie"), e.getMessage());
        assertTrue(e.getMessage().contains("geen referentieveld"), e.getMessage());
        assertTrue(e.getMessage().contains("winterberging door de haven"), e.getMessage());
    }

    @Test void de_feitcreatie_weigert_ook_in_de_drl_tak() throws Exception {
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(alleenCreatie(), JSON.readTree(creatieCase()), true));
        assertTrue(e.getMessage().contains("drl-transpiler"), e.getMessage());
        assertTrue(e.getMessage().contains("feitcreatie"), e.getMessage());
        assertTrue(e.getMessage().contains("winterberging door de haven"), e.getMessage());
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    /** The dubbelverbinding model reduced to ONE rule — the fold over the written element role `liggers` —
     *  with both fact creations dropped, so the aggregation's own refusal is the one reached. An in-memory
     *  variant written to a temp file; the corpus ir.json is untouched. */
    private static Path alleenTelling() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(DUBBELVERBINDING)));
        ir.putArray("feitCreaties");
        houdRegel(ir, "liggertal van een haven");
        return tijdelijk("dubbelverbinding-telling", ir);
    }

    /** The same model reduced to ONE fact creation and NO rules, so the §13.4.7 refusal is the one reached. */
    private static Path alleenCreatie() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(DUBBELVERBINDING)));
        ir.putArray("regels");
        ir.putArray("resolvedRegels");
        houdCreatie(ir, "winterberging door de haven");
        return tijdelijk("dubbelverbinding-creatie", ir);
    }

    /** Reduce a model to ONE named rule, in both the declaration list and the resolved list. */
    private static void houdRegel(ObjectNode ir, String naam) {
        for (String veld : new String[] { "regels", "resolvedRegels" }) {
            ArrayNode houd = JSON.createArrayNode();
            for (JsonNode r : ir.get(veld)) if (naam.equals(r.get("naam").asText())) houd.add(r);
            ir.set(veld, houd);
        }
    }

    /** Reduce a model to ONE named fact creation. */
    private static void houdCreatie(ObjectNode ir, String naam) {
        ArrayNode houd = JSON.createArrayNode();
        for (JsonNode f : ir.get("feitCreaties")) if (naam.equals(f.get("naam").asText())) houd.add(f);
        ir.set("feitCreaties", houd);
    }

    /** One Haven with two liggers, the fold pinned at 2. */
    private static String tellingCase() {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-06-01",
                  "input": { "instances": [
                      { "label": "H1", "objectType": "Haven", "attributes": { "kadegeld": { "number": "40" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S1", "objectType": "Schuit", "attributes": { "romplengte": { "number": "12" } },
                        "roles": { "thuishaven": ["H1"] }, "characteristics": [] },
                      { "label": "S2", "objectType": "Schuit", "attributes": { "romplengte": { "number": "7" } },
                        "roles": { "thuishaven": ["H1"] }, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "H1", "attribute": "liggertal", "value": { "number": "2" } } ] }
                """;
    }

    /** The same world without the fold: the creation writes a field no rule reads here, so the case pins
     *  only the inputs — the refusal happens at lowering, before a single expectation is compared. */
    private static String creatieCase() {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-06-01",
                  "input": { "instances": [
                      { "label": "H1", "objectType": "Haven", "attributes": { "kadegeld": { "number": "40" } },
                        "roles": {}, "characteristics": ["veilig"] },
                      { "label": "S1", "objectType": "Schuit", "attributes": { "romplengte": { "number": "12" } },
                        "roles": { "thuishaven": ["H1"] }, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "S1", "attribute": "romplengte", "value": { "number": "12" } } ] }
                """;
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
