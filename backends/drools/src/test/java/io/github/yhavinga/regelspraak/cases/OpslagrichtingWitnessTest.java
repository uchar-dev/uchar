package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.3.9 STORAGE-DIRECTION witness: WHERE a relation's single scalar bean field lives, WHICH end a
 * case-supplied link lands on, and which end an aggregation then reads.
 *
 * <p>ref keys links per instance per role key, so a relation is DIRECTED at the storage layer: the side that
 * wrote it reads it, the far side reads nothing. The bean model has no such key — it has one scalar field per
 * to-one role — so the two only agree where the field derivation and the case-input write agree with ref's
 * keying. Gate 12 cannot see any of that: every corpus golden supplies its relations from the end where both
 * executors agree, so the seams below are exactly the ones a wrong storage decision gets wrong while every
 * pinned total still lands.
 *
 * <p>Every expectation was replayed through {@code ref/} on this exact model and case FIRST. The models are
 * corpus goldens' {@code ir.json} off the wire, or an in-memory variant of one; every case is authored here,
 * so no corpus expectation is read, moved or touched.
 */
class OpslagrichtingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The ORDINARY self-typed pair: `opvolging` relates Wachter to Wachter through two DISTINCT to-one sibling
     *  roles (`voorganger`, `opvolger`), beside a second, to-many self-pair (`erflijn`). */
    private static final String ERFLIJN = "anchor/jutterswet-erflijn";
    /** A to-many element role: `vondstaanmelding` pairs a to-one `aanmelder` with a to-many `vondst`, so the
     *  relation has exactly ONE bean field (Vondst.aanmelder) serving both directions. */
    private static final String VONDSTTELLING = "jutterswet-vondsttelling";

    // ---------------- 1. the case-input write on an ordinary self-typed pair ----------------

    @Test void een_geschreven_zelfpaarrol_landt_op_het_veld_van_die_rol() throws Exception {
        // The by-pair search derives NULL here by construction (verbindendeRol counts voorganger, opvolger,
        // erflater and erfvolger as candidates), so the case's role KEY is the only route to the field — the
        // very by-name authority FactModelGenerator.zelfpaarRollen derived the field from. Before this route
        // existed the whole golden was refused ("has no uniquely derivable reference field"), a reason that
        // stopped being true when the card gave the bean its two self-pair fields.
        Map<String, Object> beans = beans(tellingOverOpvolgers(), keten("\"opvolger\": [\"W2\"]", ""));
        assertSame(beans.get("W2"), RegelSpraakRuntime.get(beans.get("W1"), "opvolger"),
                "the written key 'opvolger' resolves by NAME to the opvolging role and lands on ITS field");
    }

    @Test void de_gewone_zelfpaarkoppeling_spiegelt_niet() throws Exception {
        // The flank that separates an ordinary self-pair from the §13.3.9.2 WEDERKERIG one, which DOES mirror.
        // ref's Case.materialise_reciprocal is gated on ft.wederkerig, so W1 -> W2 over `opvolging` leaves W2's
        // voorganger reading EMPTY. Measured on this model: the counting probe reads 1 opvolger on W1 and 0
        // voorgangers on W2, and the mirrored hypothesis MISMATCHes in ref.
        Map<String, Object> beans = beans(tellingOverOpvolgers(), keten("\"opvolger\": [\"W2\"]", ""));
        assertNull(RegelSpraakRuntime.get(beans.get("W2"), "voorganger"),
                "a mirrored write would fill the sibling field — ref reads nothing there");
        assertNull(RegelSpraakRuntime.get(beans.get("W1"), "voorganger"));
        assertNull(RegelSpraakRuntime.get(beans.get("W2"), "opvolger"));
    }

    @Test void de_gerichte_koppeling_beslist_de_gelezen_waarde() throws Exception {
        // and its end-to-end consequence, which the bean-level assertions above do not imply: the fold starts at
        // the written end only. Model: the erflijn golden with its count rewired onto the TO-ONE `opvolgers`
        // (an in-memory variant; the corpus ir.json is untouched). ref on exactly this model and case: W1 counts
        // 1 opvolger, W2 counts 0 — a mirrored fill would make both 1, and ref rejects that (MISMATCH on W2).
        Path ir = tellingOverOpvolgers();
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(ketenMetVerwachting("1", "0"))), "ref agrees: W1 1, W2 0");
        assertNotNull(CaseRunner.speelCase(ir, JSON.readTree(ketenMetVerwachting("1", "1"))),
                "and the mirrored reading is a named mismatch, not a silent pass");
    }

    @Test void de_dubbelmeervoudige_zelfrelatie_heeft_geen_veld_en_wordt_overgeslagen() throws Exception {
        // Why the erflijn golden STILL does not play, now that its case input IS carried. `erflijn` relates
        // Wachter to Wachter through TWO TO-MANY roles, so the bean model derives no field at EITHER end — a
        // to-many role's storage is the far end's field, and here both ends are to-many. The aggregation over
        // `erfvolgers` therefore has no join field: before the guard it lowered anyway and died at first fire
        // ("geen getter voor 'erflater'"), an unclassified ERROR. It must be a lowering SKIP, i.e. a
        // reason-carrying per-form exclusion of the whole golden.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir(ERFLIJN), JSON.readTree(ketenMetVerwachting("1", "0"))));
        assertTrue(e.getMessage().contains("erfvolgeraantal van een wachter"), e.getMessage());
    }

    @Test void een_meervoudige_zelfpaarrol_wordt_luid_geweigerd_met_de_ware_reden() throws Exception {
        // What is LEFT of the refusal after the by-name route: `erfvolger` resolves by name, but it is TO-MANY,
        // so its storage is the far end's field — and on a self-typed pair that end is the same bean, which the
        // by-pair search cannot name either. ref carries this case (it keys the link under 'erfvolger' and
        // counts 1 on W1), so the backend must refuse it BY NAME rather than pretend the field is underivable.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> beans(tellingOverOpvolgers(), keten("\"erfvolger\": [\"W2\"]", "")));
        assertTrue(e.getMessage().contains("no to-one reference field"), e.getMessage());
        assertTrue(e.getMessage().contains("Wachter -> Wachter"), e.getMessage());
    }

    // ---------------- 2. which end an aggregation reads ----------------

    @Test void de_een_op_een_telling_leest_het_veld_van_het_subject() throws Exception {
        // §13.3.9 forward arm: with BOTH roles to-one each direction owns a field, so the fold must read the
        // SUBJECT's own reference (Jutter.vondst), not the element's back-reference. Model: vondsttelling with
        // the `vondst` role clipped to to-one; case supplies the link from the SUBJECT side. ref on exactly
        // this model and case: J1 counts 1 and sums 2.50.
        Path ir = vondstRolNaarToOne();
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(telling("\"vondsten\": [\"V1\"]", "", "", "1", "2.50"))),
                "ref agrees: 1 / 2.50");
    }

    @Test void dezelfde_telling_negeert_een_levering_aan_de_elementkant() throws Exception {
        // The flank that proves the forward arm reads the subject's field and not just "whichever end was
        // filled": the SAME 1:1 model with the link supplied from the ELEMENT side (V1 names its aanmelder)
        // leaves the subject's own field empty, so the fold finds nothing. ref agrees exactly — it keys the
        // link on V1 and reads none on J1 — and rejects the 1 / 2.50 reading on this case.
        Path ir = vondstRolNaarToOne();
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(telling("", "\"aanmelder\": [\"J1\"]", "", "0", "0"))),
                "ref agrees: 0 / 0");
        assertNotNull(CaseRunner.speelCase(ir, JSON.readTree(telling("", "\"aanmelder\": [\"J1\"]", "", "1", "2.50"))),
                "and the subject-side reading is a named mismatch here, not a silent pass");
    }

    @Test void de_meervoudige_zusterrol_telt_de_elementkant_wel_mee_bekende_restlacune() throws Exception {
        // CHARACTERISATION of a KNOWN, PRE-EXISTING divergence — not an endorsement. With a TO-MANY element
        // role the relation has exactly ONE field (Vondst.aanmelder) and it necessarily serves both directions,
        // so the backref join folds a link supplied from the ELEMENT end as if the subject had written it.
        // Measured on the UNCLIPPED vondsttelling golden, element-end supply:
        //     ref    -> J1 aantal 0, gewicht 0     (nothing was keyed on J1)
        //     Drools -> J1 aantal 2, gewicht 3.75  (both elements fold in)
        // The to-one arm REFUSES exactly this shape (ExecModelTranspiler: "one shared field, both directions");
        // the to-many arm does not, and closing it means tracking WHICH end supplied a link — a change under
        // every existing aggregation, so it is carded, not smuggled in here. No corpus golden supplies a
        // to-many relation from the element end, so no gate can see it; this test is that visibility. It FAILS
        // WHEN THE GAP CLOSES, which is the point: the fix must come here to retire it.
        assertNull(CaseRunner.speelCase(ir(VONDSTTELLING),
                        JSON.readTree(telling("", "\"aanmelder\": [\"J1\"]", "\"aanmelder\": [\"J1\"]", "2", "3.75"))),
                "the backend still folds the far-end supply; ref reads 0 / 0 on this case");
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static Map<String, Object> beans(Path irPath, String caseJson) throws Exception {
        return CaseRunner.invoerBeans(irPath, JSON.readTree(caseJson));
    }

    /** Two Wachters with the given role objects, and the given erfvolgeraantal expectations (empty for none). */
    private static String keten(String rollenW1, String verwacht) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Wachter", "attributes": {},
                        "roles": { %s }, "characteristics": [] },
                      { "label": "W2", "objectType": "Wachter", "attributes": {},
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(rollenW1, verwacht);
    }

    private static String ketenMetVerwachting(String w1, String w2) {
        return keten("\"opvolger\": [\"W2\"]", """
                { "kind": "attributeValue", "label": "W1", "attribute": "erfvolgeraantal", "value": { "number": "%s" } },
                { "kind": "attributeValue", "label": "W2", "attribute": "erfvolgeraantal", "value": { "number": "%s" } }"""
                .formatted(w1, w2));
    }

    /** One Jutter and two Vondsten (2.50 kg, 1.25 kg) with the relation written on the given sides, plus the
     *  pinned fold over the jutter. */
    private static String telling(String rollenJ1, String rollenV1, String rollenV2, String aantal, String gewicht) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": {},
                        "roles": { %s }, "characteristics": [] },
                      { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "2.50" } },
                        "roles": { %s }, "characteristics": [] },
                      { "label": "V2", "objectType": "Vondst", "attributes": { "gewicht": { "number": "1.25" } },
                        "roles": { %s }, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "J1", "attribute": "aantal aangemelde vondsten", "value": { "number": "%s" } },
                      { "kind": "attributeValue", "label": "J1", "attribute": "aangemeld gewicht", "value": { "number": "%s" } } ] }
                """.formatted(rollenJ1, rollenV1, rollenV2, aantal, gewicht);
    }

    /** The vondsttelling model with the `vondst` role clipped to TO-ONE — the 1:1 shape in which each direction
     *  owns its own bean field. An in-memory variant written to a temp file; the corpus ir.json is untouched. */
    private static Path vondstRolNaarToOne() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(VONDSTTELLING)));
        for (JsonNode r : ir.get("feittypen").get("vondstaanmelding").get("rollen"))
            if ("vondst".equals(r.get("naam").asText())) ((ObjectNode) r).put("meervoudig", false);
        return tijdelijk("vondst-een-op-een", ir);
    }

    /** The erflijn model with its count rewired from the to-many `erfvolgers` onto the TO-ONE `opvolgers` — the
     *  two feitcreaties and the recursive regelgroep that fed the `erflijn` relation dropped with it. The
     *  `erflijn` feittype itself STAYS, so the model keeps both self-pair shapes: the to-one one the bean model
     *  can hold, and the double-to-many one it cannot. An in-memory variant; the corpus ir.json is untouched. */
    private static Path tellingOverOpvolgers() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(ERFLIJN)));
        ir.putArray("feitCreaties");
        ir.putArray("regelGroepen");
        ir.putArray("regels").addObject()
                .put("node", "Regel").put("naam", "erfvolgeraantal van een wachter").put("soort", "regel");
        ObjectNode waarde = (ObjectNode) ir.get("resolvedRegels").get(0).get("waarde");
        ((ObjectNode) waarde.get("argumenten").get(0)).put("attribuut", "opvolgers");
        ((ObjectNode) waarde.get("bronRol")).put("feittype", "opvolging").put("rol", "opvolger");
        return tijdelijk("erflijn-telling-op-opvolgers", ir);
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
