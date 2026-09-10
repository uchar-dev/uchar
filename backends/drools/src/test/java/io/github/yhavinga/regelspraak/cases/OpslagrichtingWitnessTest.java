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
    /** The SELF-TYPED creation: a Wachter creates a Wachter over `opvolging` (roles `voorganger`/`opvolger`,
     *  both to-one), reads the fold over its opvolgers, and carries a consistentie. */
    private static final String WACHTERSKETEN = "anchor/jutterswet-wachtersketen";
    /** A to-ONE NAVIGATED value read: `ligplaats` pairs the to-one `werf` with the to-many `schuit`, so the
     *  reference field lives on Schuit, named after the role — the field every navigated read walks. */
    private static final String NAVIGATIEPREMIE = "synth-navigatiepremie";

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
        // reason-carrying per-form exclusion of the whole golden — and the REASON must name the §13.3.9
        // storage-direction limit it is, not the generic "aggregatie buiten Phase-C scope" bucket that reads as
        // an arm nobody has built yet (the schendingReden distinction, on the fold side).
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir(ERFLIJN), JSON.readTree(ketenMetVerwachting("1", "0"))));
        assertTrue(e.getMessage().contains("erfvolgeraantal van een wachter"), e.getMessage());
        assertTrue(e.getMessage().contains("de aggregatie-join heeft geen beanveld"), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'erflater' op Wachter is meervoudig"), e.getMessage());
        assertTrue(e.getMessage().contains("§13.3.9 opslagrichting"), e.getMessage());
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

    // ---------------- 3. the name-sharing displacement: a role name a declared member already owns ----------------

    // The trap, in one sentence: FactModelGenerator.leden ports every member through a NAME-DEDUP (attribuut >
    // kenmerk > referentie, because a bean carries one field per name), so a Wachter that declares a Numeriek
    // attribute `opvolger` keeps the PROPERTY and loses the self-pair REFERENCE — `opvolger` is a BigDecimal
    // field. Nothing upstream stops such a model: the verifier PASSES it and ref plays it (measured below).
    // Every reader that asked only for the NAME then wrote a fact into a BigDecimal. Measured on the pre-card
    // jar (HEAD 71c9ad9), both arms produced UNCLASSIFIED ERRORs where main gave a clean per-form EXCLUDE:
    //     creation arm : ERROR ConsequenceException … ClassCastException: Cannot cast …facts.Wachter to
    //                    java.math.BigDecimal   (rule "estafette: werf de opvolger")
    //     case-role arm: ERROR RuntimeException: kon setter setOpvolger(Wachter) niet aanroepen op Wachter
    // Gate 12 fails on an ERROR by construction ("a fault at fire is not a value-divergence"), so each arm must
    // refuse the FORM instead — loudly, naming the member that displaced the reference.

    @Test void de_creatiearm_weigert_een_naamdelende_terugkoppeling() throws Exception {
        // The §13.4.6 arm: the creation's own verbindendeRol binding names `opvolger`, whose field the attribute
        // took. ref on exactly this model and case: `oracle: PASS (1 expectations)` — the model is ref-clean AND
        // verifier-clean (`verifier: PASS`), so this is not a broken program filed under "not carried": it is a
        // bean-model limit of THIS backend, which must therefore say so per creation.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(naamdelendeWachter(false), JSON.readTree(ketenZonderRollen(false))));
        assertTrue(e.getMessage().contains("objectcreatie: werf de opvolger"), e.getMessage());
        assertTrue(e.getMessage().contains("deelt haar naam met een gedeclareerd lid"), e.getMessage());
    }

    @Test void de_case_rolinvoer_weigert_hetzelfde_veld() throws Exception {
        // The case-input arm, on the same collision with the rules and the creation stripped away (they refuse
        // first, and each arm must be witnessed on its own). ref on exactly this model and case: `oracle: PASS
        // (2 expectations)` — ref keys the link under the role name and the attribute beside it, untroubled.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.invoerBeans(naamdelendeInvoerWachter(), JSON.readTree(ketenMetOpvolgerrol())));
        assertTrue(e.getMessage().contains("role 'opvolger' resolves to the to-one role 'opvolger'"), e.getMessage());
        assertTrue(e.getMessage().contains("name-sharing bean limit"), e.getMessage());
    }

    @Test void de_foldjoin_weigert_hetzelfde_veld_met_dezelfde_reden() throws Exception {
        // The third reader of that field — the §5.8 fold's join (`het aantal opvolgers`) — asked for the name
        // only, so it lowered a join reading a BigDecimal. On the FULL model this is the first refusal reached,
        // which is why the creation witness above strips the rules. ref: `oracle: PASS (2 expectations)`.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(naamdelendeWachter(true), JSON.readTree(ketenZonderRollen(true))));
        assertTrue(e.getMessage().contains("de aggregatie-join heeft geen beanveld"), e.getMessage());
        assertTrue(e.getMessage().contains("deelt haar naam met een gedeclareerd lid"), e.getMessage());
    }

    // The FOURTH reader of that same field — the ordinary navigated VALUE read — asked for the name only until
    // 2026-09-10, and it is the one arm that produced no ERROR at all. Its shape is the quiet one: the case
    // writes NO role (so the case-input gate above is never reached) while a rule navigates over the displaced
    // name. The hop then reads a BigDecimal/boolean instead of a fact, the chain's leeg short-circuit does not
    // fire because that value is NOT null, and the attribute read off it yields null — which the arithmetic
    // treats as an unset value rather than as ch. 1.2's absent relation. Measured on the pre-gate jar, on a
    // verifier-clean and ref-clean model:
    //     ref / clingo : S1.premie = 0        (20% van leeg coalesces, RUNTIME-SEMANTICS ch. 1.2)
    //     Drools       : FAIL "attribuut premie: verwacht 0, werkelijk leeg"     (value-divergence, no reason)
    // A value-divergence is exactly what gate 12's played channel is meant to catch — but a golden of this shape
    // with the displacing attribute LEFT EMPTY plays green, so the corpus can carry the broken model unseen. Both
    // hop readers (single-hop navReader, chain navKetenReader) are witnessed apart: they differ only in whether
    // the ref carries a navigatiePad, and each lowered the same wrong read on its own.

    @Test void de_genavigeerde_waardelezing_weigert_het_verdrongen_veld() throws Exception {
        // navKetenReader — the ref as the frontend emits it: navigatiePad ["werf"].
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(naamdelendeSchuit(true), JSON.readTree(schuitZonderRol("premie", "0"))));
        assertTrue(e.getMessage().contains("premie van een schuit"), e.getMessage());
        assertTrue(e.getMessage().contains("genavigeerde lezing"), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'werf' op Schuit deelt haar naam met een gedeclareerd lid"), e.getMessage());
        assertTrue(e.getMessage().contains("§13.3.9 opslagrichting"), e.getMessage());
    }

    @Test void de_enkelvoudige_hoplezing_weigert_hetzelfde_veld() throws Exception {
        // navReader's GENAVIGEERD case — the SAME model and case with the pad cleared and the hop named by the
        // role (the shape synth3-kamerheffing carries). ref is indifferent to which of the two the wire uses (it
        // resolves the hop either way and answers 0), so the two readers must be too.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(naamdelendeSchuit(false), JSON.readTree(schuitZonderRol("premie", "0"))));
        assertTrue(e.getMessage().contains("genavigeerde lezing"), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'werf' op Schuit deelt haar naam met een gedeclareerd lid"), e.getMessage());
    }

    @Test void de_genavigeerde_vergelijking_weigert_hetzelfde_veld() throws Exception {
        // navComparisonRole — the CONDITION side, which reads the relation field itself (§5.5.5: a null relation
        // is a leegwaarde). On the pre-gate jar this arm did not even reach a wrong value: the constraint
        // evaluated a BigDecimal as a fact and aborted the fire with an unclassified
        // ERROR ConstraintEvaluationException, which gate 12 refuses to excuse. ref on exactly this model and
        // case: toeslag stays leeg (leeg > 5 is onwaar, Tabel 33-44), `oracle: PASS (1 expectations)`.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(navigerendeVergelijking(), JSON.readTree(schuitZonderRol(null, null))));
        assertTrue(e.getMessage().contains("toeslag van een schuit"), e.getMessage());
        assertTrue(e.getMessage().contains("genavigeerde lezing"), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'werf' op Schuit deelt haar naam met een gedeclareerd lid"), e.getMessage());
    }

    // navReactiviteit — the fourth hop reader — gets the same gate but NO witness of its own, and the reason is
    // structural: it is reached only from the scalar value path AFTER that path's own reader lowered, and both
    // readers now gate every hop they walk, so a hop without a field cannot arrive there. The gate stays because
    // navRefs walks argument positions the value path does not necessarily lower through (a Functie's arguments),
    // and a silently DROPPED far-side watch is a stale value, not a refusal — the one outcome that method exists
    // to prevent. It refuses the construct rather than continuing past the hop.

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

    /** The wachtersketen model with a Numeriek attribute that SHARES the self-pair role name `opvolger`, so
     *  leden()'s name-dedup displaces the reference field. {@code metFold} keeps the §5.8 fold rule (whose join
     *  reads that same field, and refuses FIRST); without it the §13.4.6 creation arm is the refusal reached.
     *  Both are in-memory variants; the corpus ir.json is untouched. Both are verifier-clean and ref-clean. */
    private static Path naamdelendeWachter(boolean metFold) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(WACHTERSKETEN)));
        ((ObjectNode) ir.get("objecttypen").get("Wachter").get("attributen")).set("opvolger", numeriek());
        if (!metFold) {
            ir.putArray("resolvedRegels");
            com.fasterxml.jackson.databind.node.ArrayNode regels = ir.putArray("regels");
            for (JsonNode r : JSON.readTree(Files.readString(ir(WACHTERSKETEN))).get("regels"))
                if (!"opvolgerrang van een wachter".equals(r.get("naam").asText())) regels.add(r);
        }
        return tijdelijk("wachter-naamdeling-" + (metFold ? "fold" : "creatie"), ir);
    }

    /** The same collision on an INPUT-ONLY model — creation, rules, group and consistentie dropped — so the
     *  case-input arm is reached instead of a lowering refusal. Verifier-clean and ref-clean. */
    private static Path naamdelendeInvoerWachter() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(WACHTERSKETEN)));
        ((ObjectNode) ir.get("objecttypen").get("Wachter").get("attributen")).set("opvolger", numeriek());
        ir.putArray("objectCreaties"); ir.putArray("resolvedRegels"); ir.putArray("regels");
        ir.putArray("consistenties"); ir.putArray("regelGroepen");
        return tijdelijk("wachter-naamdeling-invoer", ir);
    }

    /** The navigatiepremie golden with a Numeriek attribute that SHARES the to-one role name `werf`, so leden()'s
     *  name-dedup displaces Schuit's reference field, reduced to its `premie` rule (`20% van het tarief van de
     *  werf van de Schuit`). {@code ketenPad} keeps the ref as the frontend emits it — navigatiePad ["werf"],
     *  lowered by navKetenReader; without it the pad is cleared and the hop is named by the role, which is
     *  navReader's GENAVIGEERD case. Verifier-clean and ref-clean in BOTH shapes; the corpus ir.json is untouched. */
    private static Path naamdelendeSchuit(boolean ketenPad) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(NAVIGATIEPREMIE)));
        ((ObjectNode) ir.get("objecttypen").get("Schuit").get("attributen")).set("werf", numeriek());
        houdRegel(ir, "premie van een schuit");
        if (!ketenPad) {
            ObjectNode nav = (ObjectNode) ir.get("resolvedRegels").get(0).get("waarde").get("rechts");
            nav.putArray("navigatiePad");
            nav.put("navigatieRol", "werf");
        }
        return tijdelijk("schuit-naamdeling-" + (ketenPad ? "keten" : "enkelvoudig"), ir);
    }

    /** The same collision on the CONDITION side: the `toeslag` rule with its voorwaarde operand replaced by the
     *  navigated read (`het tarief van de werf van de Schuit groter is dan 5`) and its VALUE reduced to an own
     *  attribute, so the refusal can only come from the comparison's hop reader. Verifier-clean and ref-clean. */
    private static Path navigerendeVergelijking() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(NAVIGATIEPREMIE)));
        ((ObjectNode) ir.get("objecttypen").get("Schuit").get("attributen")).set("werf", numeriek());
        houdRegel(ir, "toeslag van een schuit");
        ObjectNode regel = (ObjectNode) ir.get("resolvedRegels").get(0);
        ObjectNode nav = JSON.createObjectNode();
        nav.put("node", "AttribuutRef").put("attribuut", "tarief").put("objecttype", "Werf")
           .put("onderwerpBron", "de werf").put("navigatieRol", "werf");
        nav.set("type", numeriek());
        nav.put("bron", "het tarief van de werf van de Schuit").put("set", false);
        nav.putArray("navigatiePad");
        nav.putArray("navigatieBindingen").addObject()
           .put("node", "RolBinding").put("feittype", "ligplaats").put("rol", "werf");
        ((ObjectNode) regel.get("voorwaarde")).set("links", nav);
        ((ObjectNode) regel.get("voorwaarde")).put("bron", "het tarief van de werf van de Schuit groter is dan 5");
        ObjectNode eigen = JSON.createObjectNode();
        eigen.put("node", "AttribuutRef").put("attribuut", "lengte").put("objecttype", "Schuit")
             .put("onderwerpBron", "de Schuit").putNull("navigatieRol");
        eigen.set("type", numeriek());
        eigen.put("bron", "de lengte van de Schuit").put("set", false);
        eigen.putArray("navigatiePad");
        regel.set("waarde", eigen);
        regel.put("soort", "GELIJKSTELLING");
        return tijdelijk("schuit-naamdeling-vergelijking", ir);
    }

    /** Reduce a model to ONE named rule, in both the declaration list and the resolved list — so each arm below
     *  is witnessed on its own refusal and not on a sibling rule that refuses first. */
    private static void houdRegel(ObjectNode ir, String naam) {
        for (String veld : new String[] { "regels", "resolvedRegels" }) {
            com.fasterxml.jackson.databind.node.ArrayNode houd = JSON.createArrayNode();
            for (JsonNode r : ir.get(veld)) if (naam.equals(r.get("naam").asText())) houd.add(r);
            ir.set(veld, houd);
        }
    }

    /** One Werf (tarief 312) and one Schuit that fills the DISPLACING attribute (werf 5) and lengte 8, with NO
     *  role written — the shape that never reaches the case-input gate. {@code attribuut} null pins the toeslag
     *  as leeg instead of a value. */
    private static String schuitZonderRol(String attribuut, String waarde) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Werf", "attributes": { "tarief": { "number": "312" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S1", "objectType": "Schuit",
                        "attributes": { "lengte": { "number": "8" }, "werf": { "number": "5" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(attribuut == null
                ? "{ \"kind\": \"attributeEmpty\", \"label\": \"S1\", \"attribute\": \"toeslag\" }"
                : "{ \"kind\": \"attributeValue\", \"label\": \"S1\", \"attribute\": \"" + attribuut
                  + "\", \"value\": { \"number\": \"" + waarde + "\" } }");
    }

    private static ObjectNode numeriek() {
        return JSON.createObjectNode().put("node", "Numeriek").put("geheel", true)
                .put("decimalen", 0).put("teken", "ONBEPAALD").putNull("eenheid");
    }

    /** W1 (rang 1), no roles — the golden's own case1, with the fold expectation dropped where that rule is. */
    private static String ketenZonderRollen(boolean metFold) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Wachter", "attributes": { "rang": { "number": "1" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s
                      { "kind": "inconsistent", "rule": "rangdetector" } ] }
                """.formatted(metFold
                ? "{ \"kind\": \"attributeValue\", \"label\": \"W1\", \"attribute\": \"opvolgerrang\","
                  + " \"value\": { \"number\": \"2\" } },"
                : "");
    }

    /** Two wachters with the self-pair role written BY NAME onto the displaced field, expectations echoing the
     *  inputs (the input-only model computes nothing). */
    private static String ketenMetOpvolgerrol() {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Wachter", "attributes": { "rang": { "number": "1" } },
                        "roles": { "opvolger": ["W2"] }, "characteristics": [] },
                      { "label": "W2", "objectType": "Wachter", "attributes": { "rang": { "number": "2" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "W1", "attribute": "rang", "value": { "number": "1" } },
                      { "kind": "attributeValue", "label": "W2", "attribute": "rang", "value": { "number": "2" } } ] }
                """;
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
