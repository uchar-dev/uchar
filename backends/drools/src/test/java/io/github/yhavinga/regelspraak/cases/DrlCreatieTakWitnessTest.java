package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.4.6 objectcreatie, read on BOTH transpiler branches — the branch mirror {@link BeideTakken}
 * exists for, applied to the construct where the two branches had drifted furthest apart.
 *
 * <p>{@link ObjectCreatieWitnessTest} pins what the creation does to the fact base, but every row there
 * runs the exec-model branch alone. The DRL-TEXT branch lowers the same construct through
 * {@code DrlTranspiler.objectCreatie} — the PORTED PRODUCTION lowering — and production implements
 * neither of the two ch. 5.5 clauses below, so four corpus goldens carrying exactly one creation went
 * through that branch answering leeg or faulting at fire while no witness said a word (measured
 * 2026-09-19, card drl-objectcreatie-defect):
 *
 * <ul>
 *   <li><b>Both ends of the link.</b> ch. 5.5: "new object and subject are linked over the back-link
 *       relation … <em>both ends</em>". Production writes only the created object's back-reference, so a
 *       fold over the created type finds the mint and a to-one navigation FROM the subject reads leeg —
 *       and the subject's own attribute, computed from that navigation, stays empty.</li>
 *   <li><b>The characteristic initialisation's empty flank.</b> ch. 5.5: the mark is set when its value
 *       evaluates true and, "evaluating false <em>or empty</em>, leaves it unset". Production writes the
 *       value through unconditionally, and a LEEG one unboxes null into the mint's primitive boolean
 *       field — a fault at fire that takes the whole {@code fireAllRules} pass down.</li>
 * </ul>
 *
 * <p>Both rows therefore demand the SAME verdict of both branches. The models are corpus goldens' own
 * {@code ir.json} read off the wire; every case is authored here and replayed through {@code ref/} first,
 * so no corpus expectation is read, moved or touched.
 */
class DrlCreatieTakWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** Jutter -> Rekenkamer over `taxatieopdracht`: a 1:1 creation, two toekenningen, no voorwaarde; the
     *  jutter's own heffing is read THROUGH the mint ("de heffing van haar rekenkamer"), which is what
     *  makes the forward end of the link observable at all. */
    private static final String REKENKAMER = "anchor/jutterswet-rekenkamer";
    /** The same shape plus a §13.4.6.4 kenmerkwaardetoekenning: the mint's `overzees` mark comes from a
     *  NULLABLE Booleaan attribute on the subject, and the mint's own heffing rule branches on the mark —
     *  so true, false and LEEG each have a different observable outcome. */
    private static final String OVERZEE = "anchor/jutterswet-rekenkamer-overzee";

    // ---------------- ch. 5.5: both ends of the back-link ----------------

    @Test void beide_takken_bereiken_de_mint_vanaf_het_subject() throws Exception {
        // 240 - 90 = 150 on the mint, and the jutter reads it over the FORWARD field. Without that second
        // write the navigation finds no rekenkamer and the jutter's heffing stays leeg — which is exactly
        // what the DRL-text branch answered for this model before the forward write landed.
        BeideTakken.groen(ir(REKENKAMER), jutter("240", "90", "150"),
                "a to-one navigation from the subject must reach the mint");
    }

    @Test void de_mintlezing_is_in_beide_takken_een_echte_waarde() throws Exception {
        // the flank: the comparator does reject a wrong value on this very case, in both branches, so the
        // row above is an assertion about 150 and not about "no complaint".
        BeideTakken.rood(ir(REKENKAMER), jutter("240", "90", "149"), "heffing",
                "the navigated read must be observably 150, not merely unasserted");
    }

    @Test void een_geleverde_koppeling_blokkeert_de_creatie_in_beide_takken() throws Exception {
        // §9 idempotence, the 1:1 linked-instance guard (ch. 5.5 / ch. 5.7a): a rekenkamer supplied on the
        // CREATED side blocks the mint, so the jutter reads the SUPPLIED one (10 - 4 = 6) and not a mint of
        // its own (240 - 90). Writing the forward end also fills the field the guard could read, so this row
        // is what keeps that write from becoming a second, self-cancelling mint.
        BeideTakken.groen(ir(REKENKAMER), geleverdeRekenkamer(),
                "an already linked instance blocks the creation");
    }

    // ---------------- ch. 5.5: the characteristic initialisation's three values ----------------

    @Test void een_lege_kenmerkwaarde_laat_de_mark_in_beide_takken_ongezet() throws Exception {
        // the subject's `overzees` is absent, so the initialisation value is LEEG: the mark stays unset and
        // the mint takes the gewone-rekenkamer rule (heffing = grondslag). Production wrote the leeg through
        // and the primitive boolean field unboxed null — a ConsequenceException at fire.
        BeideTakken.groen(ir(OVERZEE), overzeeseJutter(null, "5000"),
                "an empty characteristic initialisation leaves the mark unset");
    }

    @Test void een_onware_kenmerkwaarde_laat_de_mark_in_beide_takken_ongezet() throws Exception {
        BeideTakken.groen(ir(OVERZEE), overzeeseJutter(false, "5000"),
                "a false characteristic initialisation leaves the mark unset");
    }

    @Test void een_ware_kenmerkwaarde_zet_de_mark_in_beide_takken() throws Exception {
        // and the mark is really SET, not merely never wrong: the overzeese rule adds 1000.
        BeideTakken.groen(ir(OVERZEE), overzeeseJutter(true, "6000"),
                "a true characteristic initialisation sets the mark on the mint");
    }

    @Test void de_marklezing_is_in_beide_takken_een_echte_waarde() throws Exception {
        // the flank for the row above: a mint whose mark was NOT set would answer 5000, which the comparator
        // rejects here — so "6000" is an assertion about the mark and not about the arithmetic alone.
        String exec = BeideTakken.rood(ir(OVERZEE), overzeeseJutter(true, "5000"), "heffing",
                "the set mark must be observably worth 1000 on the mint's own rule");
        assertTrue(exec.contains("6000"), exec);
    }

    // ---------------- the authored documents ----------------

    private static Path ir(String golden) {
        return CORPUS.resolve(golden).resolve("ir.json");
    }

    /** One Jutter with the given grondslag/aftrek, expecting {@code heffing} read through its mint. */
    private static JsonNode jutter(String grondslag, String aftrek, String heffing) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "%s" }, "aftrek": { "number": "%s" } },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
                                  "value": { "number": "%s" } },
                                { "kind": "consistent" } ] }
                """).formatted(grondslag, aftrek, heffing));
    }

    /** A jutter AND a rekenkamer already linked to it on the created side (ch. 5.7a): both read 6. */
    private static JsonNode geleverdeRekenkamer() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "grondslag": { "number": "240" }, "aftrek": { "number": "90" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "R1", "objectType": "Rekenkamer",
                        "attributes": { "grondslag": { "number": "10" }, "aftrek": { "number": "4" } },
                        "roles": { "opdrachtgever": ["J1"] }, "characteristics": [] } ],
                      "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "R1", "attribute": "heffing",
                                  "value": { "number": "6" } },
                                { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
                                  "value": { "number": "6" } },
                                { "kind": "consistent" } ] }
                """);
    }

    /** One Jutter on the OVERZEE model: {@code overzees} supplied true/false, or ABSENT for the leeg flank. */
    private static JsonNode overzeeseJutter(Boolean overzees, String heffing) throws Exception {
        String mark = overzees == null ? "" : ", \"overzees\": { \"boolean\": " + overzees + " }";
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "5000" }%s },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
                                  "value": { "number": "%s" } },
                                { "kind": "consistent" } ] }
                """).formatted(mark, heffing));
    }
}
