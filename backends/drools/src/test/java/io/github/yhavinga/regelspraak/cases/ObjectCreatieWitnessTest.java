package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.4.6 objectcreatie witness: what the creation consequence actually does to the fact base.
 *
 * <p>Gate 12 replays the goldens that carry the form and checks their pinned ref≡clingo VALUES on LABELLED
 * instances. It can never see the mint itself — a created object has no case label (ref mints into the
 * reserved {@code "::"} namespace, refused for case labels at load), so every corpus observation of a
 * creation is indirect: a fold or a navigated read on an input. That leaves the parts a wrong creation gets
 * wrong while the total still lands: a mint linked in ONE direction only (the defect this card fixed — the
 * fold found it, the navigation read leeg), a member written on the subject instead of the mint, a second
 * mint where the fact base already answered the question. Those are asserted directly here, off
 * {@link CaseRunner.NaFire}'s post-fire facts.
 *
 * <p>What the backend must reproduce is {@code RUNTIME-SEMANTICS.md} ch. 5.5, NOT the ported production
 * lowering: production writes only the created object's back-reference and unboxes a kenmerk value straight
 * into the primitive field. Every expectation below was replayed through {@code ref/} on this exact model and
 * case FIRST (the models are corpus goldens' {@code ir.json} off the wire, or a programmatic variant of one;
 * every case is authored here, so no corpus expectation is read, moved or touched).
 */
class ObjectCreatieWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The Jutter -> Rekenkamer model: a 1:1 creation over `taxatieopdracht`, two toekenningen, no voorwaarde;
     *  the jutter's heffing is read THROUGH the mint ("de heffing van haar rekenkamer"). */
    private static final String REKENKAMER = "anchor/jutterswet-rekenkamer";
    /** The same shape plus a §13.4.6.4 kenmerkwaardetoekenning: the mint's `overzees` mark comes from a
     *  NULLABLE Booleaan attribute on the subject, and the mint's own heffing rule branches on the mark. */
    private static final String OVERZEE = "anchor/jutterswet-rekenkamer-overzee";

    // ---------------- 1. the mint itself: a fact, its members, both link ends ----------------

    @Test void de_creatie_levert_een_fact_met_ingevulde_leden_op() throws Exception {
        CaseRunner.NaFire na = CaseRunner.naFire(ir(REKENKAMER), jutter("240", "90"));

        Object mint = enigeMint(na);
        assertEquals("Rekenkamer", mint.getClass().getSimpleName(),
                "the mint is an instance of the CREATED type, not of the subject's");
        // the toekenningen, on the MINT — a value written to the subject instead would leave these leeg while
        // the jutter's own reading still looked plausible.
        assertEquals(0, bedrag(mint, "grondslag").compareTo(new java.math.BigDecimal("240")));
        assertEquals(0, bedrag(mint, "aftrek").compareTo(new java.math.BigDecimal("90")));
        // and the mint is a full participant in the fixpoint: the rule over the created type ran on it.
        assertEquals(0, bedrag(mint, "heffing").compareTo(new java.math.BigDecimal("150")),
                "the created object is inserted as a fact, so rules over its type fire on it");
    }

    @Test void beide_uiteinden_van_de_terugkoppeling_worden_geschreven() throws Exception {
        // ch. 5.5: "new object and subject are linked over the back-link relation — BOTH ends". The ported
        // production lowering writes only the first of these two; without the second the navigated read below
        // reads leeg, which is exactly how this card's defect showed up.
        CaseRunner.NaFire na = CaseRunner.naFire(ir(REKENKAMER), jutter("240", "90"));
        Object jutter = na.invoer().get("J1");
        Object mint = enigeMint(na);

        assertSame(jutter, RegelSpraakRuntime.get(mint, "opdrachtgever"),
                "the created object references its subject over the subject's role");
        assertSame(mint, RegelSpraakRuntime.get(jutter, "rekenkamer"),
                "and the subject references the created object over the created-side role");
    }

    @Test void de_genavigeerde_lezing_bereikt_het_gecreeerde_object() throws Exception {
        // the observable consequence of the second leg, through the FULL replay: "de heffing van haar
        // rekenkamer" resolves over the forward field. ref agrees on this model+case (240 - 90 = 150).
        assertNull(CaseRunner.speelCase(ir(REKENKAMER), jutter("240", "90")),
                "a to-one navigation from the subject must reach the mint");

        // and the assertion is not vacuous: the comparator does reject the wrong value on this very case.
        String fout = CaseRunner.speelCase(ir(REKENKAMER), jutterMetVerwachting("240", "90", "149"));
        assertNotNull(fout, "the navigated read must be observably 150, not merely unasserted");
        assertTrue(fout.contains("heffing"), fout);
    }

    @Test void een_subject_munt_precies_een_object() throws Exception {
        // §9 idempotence: the creation is a function of the fact base, not of the fire count. The rule that
        // writes the jutter's heffing MODIFIES a matched subject, which re-activates an unguarded creation.
        // On the GUARDED model (metVoorwaarde) that re-activation is real and this assertion is live: with the
        // §9 guard removed the model never reaches a fixpoint. On the plain REKENKAMER model the same
        // assertion is VACUOUS — it survives the guard's removal — so the witness lives here, on the model
        // where the property it claims is the one doing the work.
        CaseRunner.NaFire na = CaseRunner.naFire(metVoorwaarde(), jutter("240", "90"));
        assertEquals(1, mints(na).size(), "one creation per rule per subject: " + mints(na));
    }

    // ---------------- 1b. WHICH end of the link the 1:1 wait reads ----------------

    @Test void een_koppeling_op_de_subjectzijde_blokkeert_de_creatie() throws Exception {
        // ch. 5.5's 1:1 guard is a STORAGE-DIRECTION narrowing, and the direction is free — twelve green gates
        // do not hold it — until a case delivers the link on ONE end only. Subject end: an existing Rekenkamer
        // already fills the jutter's `rekenkamer` field, so nothing is minted and the navigated read reaches
        // the SUPPLIED kamer. ref on this exact model+case: J1.heffing = 6 (10 - 4, the supplied kamer's own
        // rule). Read the created side instead and this arm mints anyway and lands on 150.
        CaseRunner.NaFire na = CaseRunner.naFire(ir(REKENKAMER), koppelingOpSubject("6"));
        assertTrue(mints(na).isEmpty(), "an already-linked 1:1 subject mints nothing: " + mints(na));
        assertSame(na.invoer().get("R1"), RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"),
                "and the supplied link is left as delivered");
        assertNull(CaseRunner.speelCase(ir(REKENKAMER), koppelingOpSubject("6")));
    }

    @Test void een_koppeling_op_de_gemunte_zijde_blokkeert_de_creatie_niet() throws Exception {
        // The other end, which the created-side reading gets wrong in the OPPOSITE direction: the same relation
        // delivered solely on the Rekenkamer bean leaves the subject's to-one field empty, so ch. 5.5 still
        // mints — and the MINT, not the supplied kamer, is what the jutter then navigates to. ref: J1.heffing
        // = 150 (240 - 90). Read the created side and this arm blocks, leaving the heffing leeg. Together the
        // two arms fix the direction: neither reading passes both.
        CaseRunner.NaFire na = CaseRunner.naFire(ir(REKENKAMER), koppelingOpGemunteZijde("150"));
        assertEquals(1, mints(na).size(), "a created-side link does not block the mint: " + mints(na));
        assertSame(mints(na).get(0), RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"),
                "and the forward field points at the mint, not at the supplied kamer");
        assertNull(CaseRunner.speelCase(ir(REKENKAMER), koppelingOpGemunteZijde("150")));
    }

    // ---------------- 2. the §13.4.6.4 kenmerkwaardetoekenning ----------------

    @Test void een_ware_kenmerktoekenning_zet_de_markering_op_het_gemunte_object() throws Exception {
        CaseRunner.NaFire na = CaseRunner.naFire(ir(OVERZEE), overzee("800", Boolean.TRUE));
        assertEquals(Boolean.TRUE, RegelSpraakRuntime.get(enigeMint(na), "overzees"),
                "a true kenmerk value sets the mark on the mint");
        // observable through the branch the mark selects: the overzeese arm adds 1000 (ref: 1800).
        assertNull(CaseRunner.speelCase(ir(OVERZEE), overzeeMetVerwachting("800", Boolean.TRUE, "1800")));
    }

    @Test void een_lege_kenmerktoekenning_laat_de_markering_ongezet() throws Exception {
        // ch. 5.5: a characteristic initialisation evaluating false OR EMPTY leaves the mark unset —
        // equivalent to an absent initialisation. The subject's `overzees` is a NULLABLE Booleaan attribute,
        // so an unsupplied one reads leeg; writing that leeg through to the mint's primitive boolean field
        // was the intValue() NPE that kept this whole form out of the backend.
        CaseRunner.NaFire na = CaseRunner.naFire(ir(OVERZEE), overzee("800", null));
        assertEquals(Boolean.FALSE, RegelSpraakRuntime.get(enigeMint(na), "overzees"),
                "an empty kenmerk value leaves the mark unset — never a fault, never true");
        assertNull(CaseRunner.speelCase(ir(OVERZEE), overzeeMetVerwachting("800", null, "800")),
                "so the gewone arm decides: 800");
    }

    @Test void een_onware_kenmerktoekenning_laat_de_markering_ook_ongezet() throws Exception {
        // the third value the flank must separate from true: explicit false, same outcome as empty.
        CaseRunner.NaFire na = CaseRunner.naFire(ir(OVERZEE), overzee("800", Boolean.FALSE));
        assertEquals(Boolean.FALSE, RegelSpraakRuntime.get(enigeMint(na), "overzees"));
        assertNull(CaseRunner.speelCase(ir(OVERZEE), overzeeMetVerwachting("800", Boolean.FALSE, "800")));
    }

    @Test void een_ware_literal_kenmerktoekenning_zet_de_markering_ook() throws Exception {
        // The same three-way flank once more, now with the initialisation value a LITERAL instead of a read of
        // a nullable attribute — the arms no case input can reach, so only a pin holds them. ref on this
        // mutated model: 1800 (800 + the overzeese arm's 1000).
        Path ir = kenmerkWaarde("""
                { "node": "Literal", "waarde": "waar", "type": { "node": "Booleaan" }, "bron": "waar" }""", "lit-waar");
        assertEquals(Boolean.TRUE, RegelSpraakRuntime.get(enigeMint(CaseRunner.naFire(ir, overzeeMetVerwachting("800", null, "1800"))), "overzees"));
        assertNull(CaseRunner.speelCase(ir, overzeeMetVerwachting("800", null, "1800")));
    }

    @Test void een_onware_literal_kenmerktoekenning_laat_de_markering_ongezet() throws Exception {
        // and its literal false — the arm a "write whatever the value is" lowering would set (the primitive
        // field would then hold false anyway, so ONLY the branch value separates the two). ref: 800.
        Path ir = kenmerkWaarde("""
                { "node": "Literal", "waarde": "onwaar", "type": { "node": "Booleaan" }, "bron": "onwaar" }""", "lit-onwaar");
        assertEquals(Boolean.FALSE, RegelSpraakRuntime.get(enigeMint(CaseRunner.naFire(ir, overzeeMetVerwachting("800", null, "800"))), "overzees"));
        assertNull(CaseRunner.speelCase(ir, overzeeMetVerwachting("800", null, "800")));
    }

    @Test void een_niet_booleaanse_kenmerktoekenning_faalt_met_wf027() throws Exception {
        // The fourth value, and the one the empty arm must stay SEPARABLE from: a non-boolean initialisation is
        // a FAULT, never a silently unset mark. ref on this mutated model faults too, with the same rule:
        // "EVALUATION FAULT: characteristic initialisation of "overzees" is not boolean: 5 (WF-027)".
        Path ir = kenmerkWaarde("""
                { "node": "Literal", "waarde": "5",
                  "type": { "node": "Numeriek", "geheel": true, "decimalen": 0, "teken": "ONBEPAALD", "eenheid": null },
                  "bron": "5" }""", "lit-getal");
        RuntimeException e = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> CaseRunner.naFire(ir, overzeeMetVerwachting("800", null, "800")));
        String keten = oorzaken(e);
        assertTrue(keten.contains("niet booleaans") && keten.contains("(WF-027)"), keten);
    }

    // ---------------- 3. a creation with NO toekenningen ----------------

    @Test void een_toekenningloze_creatie_munt_en_koppelt_toch() throws Exception {
        // The corpus carries this shape (the penninguitgifte Dossier), but that golden is excluded on an
        // unrelated Phase-D consistentie form, so the shape is exercised here on a programmatic variant of
        // the rekenkamer model with its toekenningen removed. The mint must still EXIST and be linked both
        // ways — a "nothing to write, so write nothing" reading would skip the whole consequence.
        Path ir = zonderToekenningen();
        CaseRunner.NaFire na = CaseRunner.naFire(ir, jutter("240", "90"));
        Object mint = enigeMint(na);

        assertSame(na.invoer().get("J1"), RegelSpraakRuntime.get(mint, "opdrachtgever"));
        assertSame(mint, RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"));
        assertNull(RegelSpraakRuntime.get(mint, "grondslag"), "no toekenning wrote a member");
        // and the empty mint propagates as leeg, not as a fault — ref pins the same on this model+case.
        assertNull(CaseRunner.speelCase(ir, jutterMetLegeVerwachting("240", "90")));
    }

    // ---------------- 4. the voorwaarde arm ----------------

    @Test void een_vervulde_voorwaarde_munt_wel() throws Exception {
        Path ir = metVoorwaarde();                       // "de grondslag van de Jutter is groter dan 100"
        assertEquals(1, mints(CaseRunner.naFire(ir, jutter("240", "90"))).size());
        assertNull(CaseRunner.speelCase(ir, jutter("240", "90")), "ref agrees: 150");
    }

    @Test void een_onvervulde_voorwaarde_munt_niet() throws Exception {
        // the other side, which a condition dropped on the floor would fail: below the threshold NOTHING is
        // created, and the navigated read finds nothing to read. ref pins the same emptiness.
        Path ir = metVoorwaarde();
        CaseRunner.NaFire na = CaseRunner.naFire(ir, jutter("50", "20"));
        assertTrue(mints(na).isEmpty(), "an unmet condition creates nothing: " + mints(na));
        assertNull(RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"),
                "and no forward link is written either");
        assertNull(CaseRunner.speelCase(ir, jutterMetLegeVerwachting("50", "20")));
    }

    // ---------------- 5. the shapes the bean model cannot hold: refused per creation, by name ----------------

    @Test void een_zelfgetypeerd_rollenpaar_wordt_luid_geweigerd() throws Exception {
        // ch. 5.5 links BOTH ends, so a creation whose back-link no bean field can hold would mint an ORPHAN —
        // inserted, and reachable from nothing. A self-typed pair (Wachter creates Wachter over `opvolging`'s
        // two same-typed roles) is one such shape: the by-pair derivation is ambiguous by construction. The
        // refusal is per CREATION and names WHICH shape, so the gate reports the real limit, not a blanket
        // "objectcreatie is not carried" for every model that happens to contain one.
        CaseRunner.Excluded e = weigering(Path.of("..", "..", "corpus", "rood", "runaway-recursion", "ir.json"));
        assertTrue(e.getMessage().contains("zelfgetypeerd rollenpaar"), e.getMessage());
    }

    @Test void een_niet_uniek_verbindend_feittype_wordt_luid_geweigerd() throws Exception {
        // the other shape, and a DIFFERENT reason: two feittypen relate the pair, so the back-link is ambiguous
        // and ch. 5.5 refuses to guess. Both must be separable — one message for two causes would report the
        // wrong limit for whichever model hits the other.
        CaseRunner.Excluded e = weigering(Path.of("..", "..", "corpus", "rood", "ambiguous-backlink", "ir.json"));
        assertTrue(e.getMessage().contains("geen uniek verbindend feittype"), e.getMessage());
    }

    @Test void een_meervoudige_subjectzijdige_rol_wordt_luid_geweigerd() throws Exception {
        // The THIRD shape, and it does not present as an orphan: the FORWARD field exists, only the back-
        // reference has nowhere to live — FactModelGenerator puts a to-many pair's scalar reference on the
        // other side, so the created type carries no `opdrachtgever` member. Writing it anyway reached a setter
        // no bean has, an unclassified fire-time ERROR ("geen setter voor 'opdrachtgever' op Rekenkamer") on a
        // model that is verifier-clean (checked) and that ref plays straight through (150 on the plain case,
        // where the link is symbolic rather than a bean field). Under the gate-12 regime an ERROR is never a
        // valid not-carried outcome, so the shape is refused per creation, by its own reason.
        // A POPULATED case, unlike the two refusals above: the crash this replaces happens at FIRE, so an
        // empty fact base would let the un-refused shape pass silently and leave the witness proving nothing.
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.naFire(meervoudigeSubjectrol(), jutter("240", "90")));
        assertTrue(e.getMessage().contains("het beanmodel draagt het terugveld niet"), e.getMessage());
        assertTrue(e.getMessage().contains("opdrachtgever"), e.getMessage());
    }

    // ---------------- fixtures ----------------

    /** The refusal building {@code irPath} raises — an assertion failure if it builds at all. */
    private static CaseRunner.Excluded weigering(Path irPath) {
        return org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.naFire(irPath, JSON.readTree("""
                        { "format": "uchar-case-v0", "asOfDate": null,
                          "input": { "instances": [], "parameters": {} }, "expected": [] }
                        """)));
    }

    /** The full cause chain of a throwable as one string — a Drools rule fault wraps the runtime's own
     *  exception, so the pinned fragment sits at the deepest level. */
    private static String oorzaken(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; ) {
            sb.append(c).append(" <- ");
            Throwable volgende = c.getCause();
            c = volgende == c ? null : volgende;
        }
        return sb.toString();
    }

    /** Every post-fire fact that is NOT one of the case's own input beans — the mints. */
    private static List<Object> mints(CaseRunner.NaFire na) {
        return na.feiten().stream().filter(f -> na.invoer().values().stream().noneMatch(b -> b == f)).toList();
    }

    private static Object enigeMint(CaseRunner.NaFire na) {
        List<Object> mints = mints(na);
        assertEquals(1, mints.size(), "expected exactly one created object, got: " + mints);
        return mints.get(0);
    }

    private static java.math.BigDecimal bedrag(Object bean, String lid) {
        Object v = RegelSpraakRuntime.get(bean, lid);
        assertNotNull(v, "member '" + lid + "' is leeg on " + bean.getClass().getSimpleName());
        return (java.math.BigDecimal) v;
    }

    private static Path ir(String golden) {
        return CORPUS.resolve(golden).resolve("ir.json");
    }

    private static JsonNode jutter(String grondslag, String aftrek) throws Exception {
        return jutterMetVerwachting(grondslag, aftrek,
                new java.math.BigDecimal(grondslag).subtract(new java.math.BigDecimal(aftrek)).toPlainString());
    }

    private static JsonNode jutterMetVerwachting(String grondslag, String aftrek, String heffing) throws Exception {
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

    private static JsonNode jutterMetLegeVerwachting(String grondslag, String aftrek) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "%s" }, "aftrek": { "number": "%s" } },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeEmpty", "label": "J1", "attribute": "heffing" },
                                { "kind": "consistent" } ] }
                """).formatted(grondslag, aftrek));
    }

    /** J1 (240/90) plus an EXISTING Rekenkamer R1 (10/4, so its own rule gives it heffing 6), with the
     *  taxatieopdracht relation delivered on the SUBJECT: the case writes the jutter's `rekenkamer` to-one
     *  field and leaves the kamer's `opdrachtgever` empty. */
    private static JsonNode koppelingOpSubject(String heffing) throws Exception {
        return tweeInstanties("\"rekenkamer\": [\"R1\"]", "", heffing);
    }

    /** The same two instances and the SAME relation, delivered on the CREATED side: the case writes the
     *  Rekenkamer bean's `opdrachtgever` field and leaves the jutter's to-one role empty. */
    private static JsonNode koppelingOpGemunteZijde(String heffing) throws Exception {
        return tweeInstanties("", "\"opdrachtgever\": [\"J1\"]", heffing);
    }

    private static JsonNode tweeInstanties(String jutterRol, String kamerRol, String heffing) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { "grondslag": { "number": "240" }, "aftrek": { "number": "90" } },
                        "roles": { %s }, "characteristics": [] },
                      { "label": "R1", "objectType": "Rekenkamer",
                        "attributes": { "grondslag": { "number": "10" }, "aftrek": { "number": "4" } },
                        "roles": { %s }, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
                                  "value": { "number": "%s" } },
                                { "kind": "consistent" } ] }
                """).formatted(jutterRol, kamerRol, heffing));
    }

    /** The overzee case: `overzees` is a nullable Booleaan ATTRIBUTE on the subject — null means unsupplied. */
    private static JsonNode overzee(String grondslag, Boolean overzees) throws Exception {
        return overzeeMetVerwachting(grondslag, overzees, Boolean.TRUE.equals(overzees)
                ? new java.math.BigDecimal(grondslag).add(new java.math.BigDecimal("1000")).toPlainString()
                : grondslag);
    }

    private static JsonNode overzeeMetVerwachting(String grondslag, Boolean overzees, String heffing) throws Exception {
        String attr = overzees == null ? "" : ", \"overzees\": { \"boolean\": " + overzees + " }";
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "%s" }%s },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "heffing",
                                  "value": { "number": "%s" } },
                                { "kind": "consistent" } ] }
                """).formatted(grondslag, attr, heffing));
    }

    /** The rekenkamer model with the creation's toekenningen removed — the corpus shape (a creation that only
     *  links) on a model this backend can otherwise replay whole. */
    private static Path zonderToekenningen() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(REKENKAMER)));
        ((com.fasterxml.jackson.databind.node.ObjectNode) ir.get("objectCreaties").get(0))
                .set("toekenningen", JSON.createArrayNode());
        return schrijf(ir, "zonder-toekenningen");
    }

    /** The overzee model with the §13.4.6.4 kenmerk toekenning's VALUE replaced — the arms no case input can
     *  reach (a literal true/false, a non-boolean). In memory: no corpus file is read for its expectations,
     *  moved, or touched. */
    private static Path kenmerkWaarde(String waardeJson, String naam) throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(OVERZEE)));
        for (JsonNode t : ir.get("objectCreaties").get(0).get("toekenningen"))
            if (t.get("kenmerk").asBoolean())
                ((com.fasterxml.jackson.databind.node.ObjectNode) t).set("waarde", JSON.readTree(waardeJson));
        return schrijf(ir, naam);
    }

    /** The rekenkamer model with the SUBJECT-side role (`opdrachtgever`) made MEERVOUDIG — one Rekenkamer
     *  serves many Jutters, so the pair's scalar reference lives on the Jutter and the created type carries no
     *  back-reference field at all. */
    private static Path meervoudigeSubjectrol() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(REKENKAMER)));
        ((com.fasterxml.jackson.databind.node.ObjectNode)
                ir.get("feittypen").get("taxatieopdracht").get("rollen").get(0)).put("meervoudig", true);
        return schrijf(ir, "meervoudige-subjectrol");
    }

    /** The rekenkamer model with a §8.1.1 comparison guarding the creation. */
    private static Path metVoorwaarde() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(REKENKAMER)));
        ((com.fasterxml.jackson.databind.node.ObjectNode) ir.get("objectCreaties").get(0))
                .set("voorwaarde", JSON.readTree("""
                        { "node": "Vergelijking", "op": "GROTER",
                          "links": { "node": "AttribuutRef", "attribuut": "grondslag", "objecttype": "Jutter",
                                     "onderwerpBron": "de Jutter", "navigatieRol": null,
                                     "type": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                               "teken": "NIET_NEGATIEF", "eenheid": null },
                                     "bron": "de grondslag van de Jutter", "set": false, "navigatiePad": [] },
                          "rechts": { "node": "Literal", "waarde": "100",
                                      "type": { "node": "Numeriek", "geheel": true, "decimalen": 0,
                                                "teken": "ONBEPAALD", "eenheid": null }, "bron": "100" },
                          "periode": null, "bron": "de grondslag van de Jutter is groter dan 100" }
                        """));
        return schrijf(ir, "met-voorwaarde");
    }

    private static Path schrijf(JsonNode ir, String naam) throws Exception {
        Path p = Files.createTempDirectory("uchar-oc-" + naam).resolve("ir.json");
        Files.writeString(p, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        p.toFile().deleteOnExit();
        return p;
    }
}
