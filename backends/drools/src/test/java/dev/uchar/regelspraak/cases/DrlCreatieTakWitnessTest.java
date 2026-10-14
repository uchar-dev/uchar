package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.4.6 objectcreatie, read on BOTH transpiler branches — the branch mirror {@link BeideTakken}
 * exists for, applied to the construct where the two branches had drifted furthest apart.
 *
 * <p>{@link ObjectCreatieWitnessTest} pins what the creation does to the fact base, but every row there
 * runs the exec-model branch alone. Until card drl-teksttak-op-truth-maintenance the DRL-TEXT branch lowered the same
 * construct through {@code DrlTranspiler.objectCreatie} — the PORTED PRODUCTION lowering — and production implements
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
 * <p>Those rows demand the SAME verdict of both branches. Two further rows below cover the SCOPES the
 * forward write rides on — a displaced reference field, and the to-many arm that has no forward field at
 * all. The displaced-field row pinned a branch divergence until card drl-creatie-koppelloos levelled it;
 * the link rows of that card live in {@link DrlCreatieKoppelingWitnessTest}.
 *
 * <p>The models are corpus goldens' own {@code ir.json} read off the wire, or a programmatic variant of
 * one; every case is authored here and replayed through {@code ref/} first, so no corpus expectation is
 * read, moved or touched.
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
    /** The §9.5 model whose ONE subject type carries BOTH creation arms: a to-MANY penning (the marker
     *  guard, no forward write) and a 1:1 dossier (the linked-instance guard, forward write). */
    private static final String PENNING = "anchor/jutterswet-penninguitgifte";

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

    // ---------------- the two guards the forward write rides on ----------------
    //
    // ch. 5.5 links both ends, but only the end the BEAN MODEL can hold. The forward write is therefore
    // scoped twice, and each scope has its own shape below. Both documents are programmatic variants of a
    // corpus golden, both are verifier-clean, and both were replayed through ref/ first.

    @Test void een_gelijknamig_lid_verdringt_het_voorwaartse_veld() throws Exception {
        // FactModelGenerator.leden carries ONE bean property per name, attribuut > kenmerk > referentie: a
        // Jutter that also declares a Numeriek `rekenkamer` keeps the NUMBER and loses the REFERENCE. The
        // forward write must then not be emitted at all — `$subject.setRekenkamer($new)` would push a fact
        // into a BigDecimal field, which sinks the WHOLE package on a document ref plays (measured: the
        // KieBuilder answers "Unable to Analyse Expression … $subject.setRekenkamer").
        Path ir = gelijknamigLid();

        // Until card drl-creatie-koppelloos (2026-09-23) this row pinned a DIVERGENCE: the exec branch refused
        // the creation by name, and this branch wrote only the back-reference and played. Writing one end of
        // ch. 5.5's link is the one-sided link the exec branch refuses to leave, so both branches now refuse,
        // by the same sentence from FactModelGenerator.geenReferentieveldReden.
        BeideTakken.geweigerd(ir, jutterZonderLezing(), "deelt haar naam met een gedeclareerd lid",
                "a displaced forward end is refused by name");
    }

    @Test void de_naar_veel_arm_koppelt_alleen_aan_de_gemunte_zijde() throws Exception {
        // A to-MANY created-side role has no bean field on the subject — FactModelGenerator derives the
        // scalar reference on the ONE side only — so the link is stored solely as the mint's own back-
        // reference and there is no forward end to write. The document below carries BOTH arms of the same
        // method at once: `penning` is to-many (the CreationMarker guard, no forward write) and `dossier` is
        // 1:1 (the linked-instance guard, forward write), so one replay walks both.
        //   What it holds: a fold FROM the subject over the created relation reaches the mint through that
        // back-reference (1 penning, totalling 7, and 1 dossier). A lowering that wrote the forward end here
        // anyway would reach `$subject.setPenning(…)`, which no Jutter bean carries — measured, and the whole
        // package then fails to build, so this row is what keeps the write on the arm that has a field.
        //   Neither creation has a connectingRole binding, and both branches derive the back-link from the type pair.
        // From the switch-over (2026-10-10) until card 20261010-objectcreatie-zonder-connectingrole-binding the
        // exec-model branch refused the document by name, and this row ran on the DRL-text branch alone.
        BeideTakken.groen(penningVorm(), penningJutter(),
                "the to-many arm links on the created side only, and the fold still reaches the mint");
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

    /** The rekenkamer model with a Numeriek attribute named after the CREATED-SIDE role (`rekenkamer`), which
     *  displaces the reference field on the Jutter bean, AND without the rule that navigates through it — that
     *  rule refuses in both branches on the same §13.3.9 reason, and would hide the creation's own answer
     *  behind it. What is left is a document whose only user of the displaced field is the creation. */
    private static Path gelijknamigLid() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(REKENKAMER)));
        IrPad.addAttribute(IrPad.objectType(ir, "Jutter"), "rekenkamer", JSON.readTree("""
                        { "node": "Number", "whole": true, "decimals": 0,
                          "sign": "unspecified" }"""));
        IrPad.rules(ir).set("rules", zonder(IrPad.rulesLijst(ir, "rules"), "heffing van een jutter"));
        return schrijf(ir, "gelijknamig-lid");
    }

    /** The penninguitgifte model with BOTH §9.5 consistenties (and their regels) removed: this branch markers
     *  each of them, which would refuse the document before either creation arm ran. What is left is the two
     *  creations — one to-many, one 1:1 — and the three folds that read them back. */
    private static Path penningVorm() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(PENNING)));
        var rules = IrPad.rules(ir);
        for (JsonNode c : IrPad.rulesLijst(ir, "consistencies"))
            rules.set("rules", zonder(rules.get("rules"), c.path("name").asText()));
        rules.set("consistencies", JSON.createArrayNode());
        return schrijf(ir, "penning-vorm");
    }

    private static com.fasterxml.jackson.databind.node.ArrayNode zonder(JsonNode lijst, String naam) {
        var behouden = JSON.createArrayNode();
        for (JsonNode n : lijst) if (!naam.equals(n.path("name").asText())) behouden.add(n);
        return behouden;
    }

    /** J1 on the displaced-field model: nothing writes its heffing, so the assertion is that the document
     *  RAN — a package that did not build never reaches the comparator at all. */
    private static JsonNode jutterZonderLezing() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "240" }, "aftrek": { "number": "90" } },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeEmpty", "label": "J1", "attribute": "heffing" },
                                { "kind": "consistent" } ] }
                """);
    }

    /** One penninggerechtigde jutter: the to-many arm mints one penning (bedrag = vondstwaarde) and the 1:1
     *  arm one dossier; all three folds read their mint back. ref agrees on this model and case. */
    private static JsonNode penningJutter() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "vondstwaarde": { "number": "7" } },
                      "roles": {}, "characteristics": ["penninggerechtigd"] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "aantal penningen",
                                  "value": { "number": "1" } },
                                { "kind": "attributeValue", "label": "J1", "attribute": "penningtotaal",
                                  "value": { "number": "7" } },
                                { "kind": "attributeValue", "label": "J1", "attribute": "aantal dossiers",
                                  "value": { "number": "1" } },
                                { "kind": "consistent" } ] }
                """);
    }

    private static Path schrijf(JsonNode ir, String naam) throws Exception {
        Path p = Files.createTempDirectory("uchar-drlcreatie-" + naam).resolve("ir.json");
        Files.writeString(p, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        p.toFile().deleteOnExit();
        return p;
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
