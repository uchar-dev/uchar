package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * mint where the fact base already answered the question. Those are asserted directly here, on the
 * truth-maintenance branch, through the entry a consumer uses: one {@link DroolsBackend#vuur} to the fixpoint, the
 * objects and links the creations made as {@link DroolsBackend#lees} gives them, and the beans after
 * {@link DroolsBackend#zetAfgeleideWaarden}, which puts the derived values and the links on them (the branch writes no
 * bean while it fires).
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
    /** The SELF-TYPED creation: a Wachter creates a Wachter over `opvolging`, whose two DISTINCT sibling roles
     *  (`voorganger`, `opvolger`) are both filled by Wachter — the pair no by-pair derivation can resolve. */
    private static final String WACHTERSKETEN = "anchor/jutterswet-wachtersketen";
    /** The §9.5 Phase-D model: two consistenties (a kenmerk assertie, an AANTAL-over-a-role assertie) beside two
     *  creations, one of them toekenningloos. */
    private static final String PENNING = "anchor/jutterswet-penninguitgifte";
    /** Jutter -> Penningbesluit over `penningtoekenning`, a 1:1 creation WITHOUT a connectingRole binding: its back-link
     *  is derived from the type pair, and the jutter reads the besluit back over the relation in both directions. */
    private static final String PENNINGBESLUIT = "jutterswet-penningbesluit";
    /** The red model whose creation subject and created type are related by TWO feittypen. */
    private static final Path AMBIGU = Path.of("..", "..", "corpus", "rood", "ambiguous-backlink", "ir.json");

    // ---------------- 1. the mint itself: a fact, its members, both link ends ----------------

    @Test void de_creatie_levert_een_fact_met_ingevulde_leden_op() throws Exception {
        Na na = naVuur(ir(REKENKAMER), jutter("240", "90"));

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
        Na na = naVuur(ir(REKENKAMER), jutter("240", "90"));
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
        assertNull(speel(ir(REKENKAMER), jutter("240", "90")),
                "a to-one navigation from the subject must reach the mint");

        // and the assertion is not vacuous: the comparator does reject the wrong value on this very case.
        String fout = speel(ir(REKENKAMER), jutterMetVerwachting("240", "90", "149"));
        assertNotNull(fout, "the navigated read must be observably 150, not merely unasserted");
        assertTrue(fout.contains("heffing"), fout);
    }

    @Test void een_subject_munt_precies_een_object() throws Exception {
        // §9 idempotence: the creation is a function of the fact base, not of the fire count. The driver fires the
        // creations at every fixpoint of the rule layer, one generation per phase, and the condition of the guarded
        // model (metVoorwaarde) still holds for J1 in the phase after the mint; one object per creation per subject is
        // what the creation's own mark for its subject keeps (TmsTranspiler.objectCreatie).
        Na na = naVuur(metVoorwaarde(), jutter("240", "90"));
        assertEquals(1, mints(na).size(), "one creation per rule per subject: " + mints(na));
    }

    // ---------------- 1b. WHICH end of the link the 1:1 wait reads ----------------
    //
    // CONVERTED 2026-09-10, maintainer decision on the case-input reading (RUNTIME-SEMANTICS ch. 5.7a). This
    // pair was written when ch. 5.5's 1:1 creation guard was a STORAGE-DIRECTION narrowing whose direction was
    // free — twelve green gates did not hold it — and the two arms fixed that direction by disagreeing: the
    // subject-end supply blocked the mint (heffing 6), the created-end supply did not (heffing 150), and
    // "neither reading passes both".
    //
    // ch. 5.7a removes the question rather than answering it. A supplied link ESTABLISHES the relation and
    // BOTH roles read it, so the guard no longer asks which END was written — it asks whether the subject
    // FILLS the role, and after one supplied leg it does, whichever leg that was. Both arms therefore block
    // and both land on 6, and THAT is now what they are for: two spellings of one relation must compute the
    // same thing. REMEASURED through ref on this exact model, created-end supply:
    //     heffing 150 -> MISMATCH: J1.heffing = 6, expected 150
    //     heffing 6   -> oracle: PASS (2 expectations)
    // The arms still refuse a real mistake — a guard that read the CREATED side would mint here and land on
    // 150, and the negative assertion below names that.

    @Test void een_koppeling_op_de_subjectzijde_blokkeert_de_creatie() throws Exception {
        // Subject end: an existing Rekenkamer already fills the jutter's `rekenkamer` field, so nothing is
        // minted and the navigated read reaches the SUPPLIED kamer. ref: J1.heffing = 6 (10 - 4, the supplied
        // kamer's own rule).
        Na na = naVuur(ir(REKENKAMER), koppelingOpSubject("6"));
        assertTrue(mints(na).isEmpty(), "an already-linked 1:1 subject mints nothing: " + mints(na));
        assertSame(na.invoer().get("R1"), RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"),
                "and the supplied link is left as delivered");
        assertNull(speel(ir(REKENKAMER), koppelingOpSubject("6")));
    }

    @Test void een_koppeling_op_de_gemunte_zijde_blokkeert_de_creatie_ook() throws Exception {
        // The other end, RENAMED from `..._blokkeert_de_creatie_niet`: the same relation delivered solely on
        // the Rekenkamer bean now reaches the subject too (the ch. 5.7a mirror write), so the jutter FILLS the
        // to-one role and ch. 5.5 mints nothing. The navigated read lands on the SUPPLIED kamer, exactly as in
        // the arm above — one relation, one outcome, two spellings.
        Na na = naVuur(ir(REKENKAMER), koppelingOpGemunteZijde("6"));
        assertTrue(mints(na).isEmpty(), "the relation reaches the subject through its far leg too: " + mints(na));
        assertSame(na.invoer().get("R1"), RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"),
                "and the forward field points at the SUPPLIED kamer, not at a mint");
        assertNull(speel(ir(REKENKAMER), koppelingOpGemunteZijde("6")));
        assertNotNull(speel(ir(REKENKAMER), koppelingOpGemunteZijde("150")),
                "and the minting reading — a guard that read the created side — is a named mismatch, not a "
                        + "silent pass");
    }

    // ---------------- 2. the §13.4.6.4 kenmerkwaardetoekenning ----------------

    @Test void een_ware_kenmerktoekenning_zet_de_markering_op_het_gemunte_object() throws Exception {
        Na na = naVuur(ir(OVERZEE), overzee("800", Boolean.TRUE));
        assertEquals(Boolean.TRUE, RegelSpraakRuntime.get(enigeMint(na), "overzees"),
                "a true kenmerk value sets the mark on the mint");
        // observable through the branch the mark selects: the overzeese arm adds 1000 (ref: 1800).
        assertNull(speel(ir(OVERZEE), overzeeMetVerwachting("800", Boolean.TRUE, "1800")));
    }

    @Test void een_lege_kenmerktoekenning_laat_de_markering_ongezet() throws Exception {
        // ch. 5.5: a characteristic initialisation evaluating false OR EMPTY leaves the mark unset —
        // equivalent to an absent initialisation. The subject's `overzees` is a NULLABLE Booleaan attribute,
        // so an unsupplied one reads leeg; writing that leeg through to the mint's primitive boolean field
        // was the intValue() NPE that kept this whole form out of the backend.
        Na na = naVuur(ir(OVERZEE), overzee("800", null));
        assertEquals(Boolean.FALSE, RegelSpraakRuntime.get(enigeMint(na), "overzees"),
                "an empty kenmerk value leaves the mark unset — never a fault, never true");
        assertNull(speel(ir(OVERZEE), overzeeMetVerwachting("800", null, "800")),
                "so the gewone arm decides: 800");
    }

    @Test void een_onware_kenmerktoekenning_laat_de_markering_ook_ongezet() throws Exception {
        // the third value the flank must separate from true: explicit false, same outcome as empty.
        Na na = naVuur(ir(OVERZEE), overzee("800", Boolean.FALSE));
        assertEquals(Boolean.FALSE, RegelSpraakRuntime.get(enigeMint(na), "overzees"));
        assertNull(speel(ir(OVERZEE), overzeeMetVerwachting("800", Boolean.FALSE, "800")));
    }

    @Test void een_ware_literal_kenmerktoekenning_zet_de_markering_ook() throws Exception {
        // The same three-way flank once more, now with the initialisation value a LITERAL instead of a read of
        // a nullable attribute — the arms no case input can reach, so only a pin holds them. ref on this
        // mutated model: 1800 (800 + the overzeese arm's 1000).
        Path ir = kenmerkWaarde("""
                { "node": "Literal", "value": { "boolean": true }, "type": { "node": "Boolean" }, "source": { "snippet": "waar" } }""", "lit-waar");
        assertEquals(Boolean.TRUE, RegelSpraakRuntime.get(enigeMint(naVuur(ir, overzeeMetVerwachting("800", null, "1800"))), "overzees"));
        assertNull(speel(ir, overzeeMetVerwachting("800", null, "1800")));
    }

    @Test void een_onware_literal_kenmerktoekenning_laat_de_markering_ongezet() throws Exception {
        // and its literal false — the arm a "write whatever the value is" lowering would set (the primitive
        // field would then hold false anyway, so ONLY the branch value separates the two). ref: 800.
        Path ir = kenmerkWaarde("""
                { "node": "Literal", "value": { "boolean": false }, "type": { "node": "Boolean" }, "source": { "snippet": "onwaar" } }""", "lit-onwaar");
        assertEquals(Boolean.FALSE, RegelSpraakRuntime.get(enigeMint(naVuur(ir, overzeeMetVerwachting("800", null, "800"))), "overzees"));
        assertNull(speel(ir, overzeeMetVerwachting("800", null, "800")));
    }

    @Test void een_niet_booleaanse_kenmerktoekenning_weigert_met_wf027() throws Exception {
        // The fourth value, and the one the empty arm must stay SEPARABLE from: a non-boolean initialisation is
        // never a silently unset mark. ref on this mutated model faults, with the same rule: "EVALUATION FAULT:
        // characteristic initialisation of "overzees" is not boolean: 5"; the verifier rejects the document (WF-027).
        // The branch refuses the form by name when it lowers the creation, on the effective type of the value, so no
        // case reaches a fire.
        Path ir = kenmerkWaarde("""
                { "node": "Literal", "value": { "number": "5" }, "type": { "node": "Number", "whole": true, "decimals": 0, "sign": "unspecified" }, "source": { "snippet": "5" } }""", "lit-getal");
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> speel(ir, overzeeMetVerwachting("800", null, "800")));
        assertTrue(e.getMessage().contains("objectcreatie: richt de rekenkamer op — een kenmerkinitialisatie van "
                + "'overzees' met een waarde van type Numeriek ('5'): een kenmerk is waar of onwaar (WF-027)"),
                e.getMessage());
    }

    // ---------------- 3. a creation with NO toekenningen ----------------

    @Test void een_toekenningloze_creatie_munt_en_koppelt_toch() throws Exception {
        // The corpus carries this shape (the penninguitgifte Dossier), but that golden is excluded on an
        // unrelated Phase-D consistentie form, so the shape is exercised here on a programmatic variant of
        // the rekenkamer model with its toekenningen removed. The mint must still EXIST and be linked both
        // ways — a "nothing to write, so write nothing" reading would skip the whole consequence.
        Path ir = zonderToekenningen();
        Na na = naVuur(ir, jutter("240", "90"));
        Object mint = enigeMint(na);

        assertSame(na.invoer().get("J1"), RegelSpraakRuntime.get(mint, "opdrachtgever"));
        assertSame(mint, RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"));
        assertNull(RegelSpraakRuntime.get(mint, "grondslag"), "no toekenning wrote a member");
        // and the empty mint propagates as leeg, not as a fault — ref pins the same on this model+case.
        assertNull(speel(ir, jutterMetLegeVerwachting("240", "90")));
    }

    // ---------------- 4. the voorwaarde arm ----------------

    @Test void een_vervulde_voorwaarde_munt_wel() throws Exception {
        Path ir = metVoorwaarde();                       // "de grondslag van de Jutter is groter dan 100"
        assertEquals(1, mints(naVuur(ir, jutter("240", "90"))).size());
        assertNull(speel(ir, jutter("240", "90")), "ref agrees: 150");
    }

    @Test void een_onvervulde_voorwaarde_munt_niet() throws Exception {
        // the other side, which a condition dropped on the floor would fail: below the threshold NOTHING is
        // created, and the navigated read finds nothing to read. ref pins the same emptiness.
        Path ir = metVoorwaarde();
        Na na = naVuur(ir, jutter("50", "20"));
        assertTrue(mints(na).isEmpty(), "an unmet condition creates nothing: " + mints(na));
        assertNull(RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"),
                "and no forward link is written either");
        assertNull(speel(ir, jutterMetLegeVerwachting("50", "20")));
    }

    // ---------------- 5. the shapes the bean model cannot hold: refused per creation, by name ----------------

    @Test void een_zelfgetypeerd_rollenpaar_koppelt_beide_uiteinden() throws Exception {
        // WAS a refusal witness until 2026-08-30 (card drools-zelfgetypeerd-creatiepaar): a self-typed pair
        // (Wachter creates Wachter over `opvolging`'s two same-typed roles) is now CARRIED, because the creation's
        // own symbolic connectingRole binding rides beside the model and names the role the MINT plays — the only
        // route there is, since the by-pair derivation counts two candidates and yields null by construction (ref
        // reads it exactly so: "the explicit back-link binding wins ... the only route for a self-typed pair").
        // Both ends are the point: `opvolger` and `voorganger` are DISTINCT sibling roles filled by the same type,
        // so a lowering that wrote one end twice, or that picked the wrong sibling, would still produce a linked
        // pair — and the corpus totals would still land, because the chain is symmetric in rank. Only the two
        // assertSames below separate them. ref on this model+case: W1.opvolgerrang = 5, rangdetector violated
        // (by the mint, rank 5).
        Na na = naVuur(ir(WACHTERSKETEN), wachter("4"));
        Object w1 = na.invoer().get("W1");
        Object mint = enigeMint(na);

        assertEquals("Wachter", mint.getClass().getSimpleName());
        assertEquals(0, bedrag(mint, "rang").compareTo(new java.math.BigDecimal("5")));
        assertSame(mint, RegelSpraakRuntime.get(w1, "opvolger"),
                "the subject references the mint over the BOUND role (the one the created object plays)");
        assertSame(w1, RegelSpraakRuntime.get(mint, "voorganger"),
                "and the mint references the subject over that role's SIBLING — not over the bound role again");
        assertNull(speel(ir(WACHTERSKETEN), wachter("4")), "ref agrees: opvolgerrang 5");
    }

    @Test void een_niet_uniek_verbindend_feittype_weigert_bij_de_bouw() throws Exception {
        // Two feittypen relate the pair, so ch. 5.5 refuses to guess: a property of the PROGRAM, on which ref faults in
        // every world. The branch refuses the creation by name when it lowers it, in the words ref faults with, so no
        // case reaches a fire and the red fence still finds ONE fragment.
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> speel(AMBIGU, jutterZonderVerwachting()));
        assertTrue(e.getMessage().endsWith("objectcreatie: insigne voor een jutter — more than one fact type relates "
                + "\"Jutter\" to \"Insigne\"; the created object's back-link is ambiguous (ch. 5.5); deze"
                + " tak draagt deze vorm nog niet"), e.getMessage());
    }

    @Test void diezelfde_ambiguiteit_zonder_subject_weigert_ook() throws Exception {
        // The same refusal on an empty world, where ref, which resolves the pair before it walks the subjects, faults
        // too: the refusal belongs to the build and not to a subject that reaches it.
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> speel(AMBIGU, JSON.readTree("""
                        { "format": "uchar-case-v0", "asOfDate": null,
                          "input": { "instances": [], "parameters": {} }, "expected": [] }
                        """)));
        assertTrue(e.getMessage().contains("the created object's back-link is ambiguous (ch. 5.5)"), e.getMessage());
    }

    @Test void twee_meervoudige_rollen_worden_luid_geweigerd() throws Exception {
        // The THIRD shape, and the only cardinality one that IS an orphan: with BOTH roles meervoudig neither
        // end derives a bean field, so the mint would be inserted and reachable from nothing. Its reason is
        // named apart from the two above — the feittype here is unique and the pair is not self-typed, so
        // either of those messages would report the wrong limit. The model is verifier-clean (its navigating
        // rule removed; WITH that rule the verifier rejects it, WF-053) and ref replays it without a fault,
        // leaving J1.heffing leeg — with no navigating rule no case expectation reaches the mint at all.
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> speel(beideRollenMeervoudig(), jutterMetLegeVerwachting("240", "90")));
        assertTrue(e.getMessage().contains("beide rollen zijn meervoudig"), e.getMessage());
        assertTrue(e.getMessage().contains("onbereikbaar"), e.getMessage());
    }

    // ---------------- 5b. the shapes with ONE writable end: degraded, not refused ----------------

    @Test void een_meervoudige_subjectzijdige_rol_degradeert_en_speelt() throws Exception {
        // A MEERVOUDIGE subject-side role (one Rekenkamer serves many Jutters) leaves the CREATED type without
        // an `opdrachtgever` member — FactModelGenerator puts a to-many pair's scalar reference on the other
        // side. Only the back-reference has nowhere to live; the jutter's `rekenkamer` field still exists and
        // still carries ch. 5.5's link, so the creation plays with the value ref gives. Refusing the whole
        // creation here (which this backend did until the asymmetry was found) withheld a value it can
        // produce. The model is verifier-clean (checked); ref on this exact model + case: J1.heffing = 150.
        Path ir = meervoudigeSubjectrol();
        Na na = naVuur(ir, jutter("240", "90"));
        Object mint = enigeMint(na);
        assertSame(mint, RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"),
                "the writable end still links subject and mint");
        // and the un-writable end is SKIPPED, not attempted: the created type has no such member at all, so a
        // symmetric "write both ends" would reach a setter no bean carries (the fire-time ERROR this replaces).
        assertTrue(java.util.Arrays.stream(mint.getClass().getMethods())
                        .noneMatch(m -> m.getName().equals("getOpdrachtgever")),
                "the created type carries no back-reference field for the to-many role");
        assertNull(speel(ir, jutter("240", "90")), "ref agrees: 150");
        // not vacuous: the comparator does reject the wrong value on this very model and case.
        assertNotNull(speel(ir, jutterMetVerwachting("240", "90", "149")));
    }

    @Test void de_gedegradeerde_vorm_houdt_de_een_op_een_wacht() throws Exception {
        // The invariant the degradation rests on: the 1:1 wait is decided by the CREATED-side role, and that is
        // exactly the end still writable here, the subject's own reference field — so the guard keeps a field to read
        // and the wait survives. ref on this model with the link already supplied on the subject: J1.heffing = 6 (the
        // supplied kamer's own rule), NOT 150.
        Path ir = meervoudigeSubjectrol();
        Na na = naVuur(ir, koppelingOpSubject("6"));
        assertTrue(mints(na).isEmpty(), "an already-linked subject mints nothing: " + mints(na));
        assertNull(speel(ir, koppelingOpSubject("6")));
    }

    @Test void een_meervoudige_gecreeerde_rol_degradeert_naar_het_markerpad() throws Exception {
        // The MIRROR degradation: a to-many CREATED-side role gives the subject no forward field, so the 1:1 wait has
        // nothing to read, and one object per subject is what the creation's own mark for its subject keeps. Model:
        // created-side role meervoudig, the navigating rule removed (with it the verifier rejects the model, WF-053).
        // Verifier-clean; ref: J1.heffing leeg.
        Path ir = markerPad();
        Na na = naVuur(ir, jutterMetLegeVerwachting("240", "90"));
        Object mint = enigeMint(na);
        assertEquals("Rekenkamer", mint.getClass().getSimpleName());
        assertSame(na.invoer().get("J1"), RegelSpraakRuntime.get(mint, "opdrachtgever"),
                "the writable end (the back-reference) links mint and subject");
        assertNull(speel(ir, jutterMetLegeVerwachting("240", "90")));
    }

    // ---------------- 5c. the §9.5 / §13.4.8 Phase-D consistentie assertions ----------------

    @Test void een_kenmerkassertie_vlagt_de_genegeerde_lezing() throws Exception {
        // §9.5 form (a): "elke Penning is uitgereikt". The VIOLATION is the negated kenmerk read — and a mark is
        // two-valued under the closed-world reading, so unlike a numeric ordering (whose leeg flank makes BOTH the
        // criterion and its negation a violation) the schending is exactly !leafPredicate. Model: the
        // penninguitgifte golden with the OTHER consistentie removed, so this arm is what decides. ref on this
        // model+case: the supplied P1 (never uitgereikt) is inconsistent under `penningcontrole`; the MINTED
        // penning, whose kenmerkinitialisatie sets the mark, is not.
        // The creations of this model have no connectingRole binding: the branch derives their back-link from the type
        // pair, as the DRL-text branch does. From the switch-over (2026-10-10) until card
        // 20261010-objectcreatie-zonder-connectingrole-binding it refused the document by name instead.
        assertNull(speel(zonderHouderscontrole(), penningCase(false)), "ref agrees: penningcontrole @ P1");
    }

    @Test void dezelfde_kenmerkassertie_vlagt_de_vervulde_lezing_niet() throws Exception {
        // The flank that keeps the arm from being a rule that always fires: the SAME model and instances with the
        // mark supplied on P1 leaves every Penning consistent. ref pins the same. The back-link comes from the type
        // pair, as in the row above.
        assertNull(speel(zonderHouderscontrole(), penningCase(true)), "ref agrees: consistent");
    }

    @Test void een_aantalassertie_over_een_enkelvoudige_rol_vlagt_de_lege_telling() throws Exception {
        // §9.5 form (b): an AANTAL over a role compared with 1. Over a TO-ONE role the count is the PRESENCE
        // count — 0 or 1 — read from the subject's own scalar reference field, which is where ref reads it too
        // (its role_links are per instance per role key). Model: penninguitgifte with the houderscontrole
        // rewritten onto `dossiervoering`, the 1:1 relation where each direction owns a field (see the refusal
        // witness below for why the penningbezit form cannot be carried). A jutter who is not penninggerechtigd
        // gets no dossier minted, so the count is 0 and the assertion is violated. ref agrees: inconsistent @ J1.
        // The creations of the penninguitgifte model have no connectingRole binding, and the back-link comes from the
        // type pair, as in the kenmerk assertion above.
        assertNull(speel(dossierAantalAssertie(), jutterMetDossierVerwachting(false, "0")),
                "ref agrees: houderscontrole @ J1");
    }

    @Test void dezelfde_aantalassertie_vlagt_een_gevulde_telling_niet() throws Exception {
        // and its flank: a jutter whose dossier the CASE supplies counts 1 and is not flagged. Without this arm a
        // lowering that always counted 0 would pass the one above. ref agrees: consistent.
        assertNull(speel(dossierAantalAssertie(), jutterMetIngevoerdDossier()), "ref agrees: consistent");
    }

    @Test void een_aantalassertie_over_een_gedeeld_veld_wordt_luid_geweigerd() throws Exception {
        // The §13.3.9 STORAGE-DIRECTION limit on an INPUT relation, named rather than silently answered.
        // `penningbezit` pairs a to-one `houder` with a to-many `penning`, so the bean model gives the relation exactly
        // ONE field (Penning.houder), which carries BOTH directions, and the branch refuses a presence count over that
        // shared field by name until a card of its own measures it. Model: the penninguitgifte golden without its two
        // creations, so that nothing creates `penningbezit` and the count reads the bean field. Verifier PASS; ref and
        // clingo play it (measured with J1 holding P1, and P2 without a houder: aantal penningen 1, houderscontrole
        // @ P2). With its creations the golden plays on this branch since card
        // 20261010-objectcreatie-zonder-connectingrole-binding: they create `penningbezit`, and a created relation is
        // read over Koppeling facts, each link once with its two ends, not over the shared field.
        CaseRunner.Excluded e = weigering(zonderCreaties());
        assertTrue(e.getMessage().contains("houderscontrole"), e.getMessage());
        assertTrue(e.getMessage().contains("zusterrol 'penning' meervoudig"), e.getMessage());
    }

    @Test void een_typenpaar_via_een_supertype_weigert_bij_de_bouw() throws Exception {
        // The penningbesluit golden with its creation making a SUBTYPE, `Jaarbesluit is een Penningbesluit`, so that
        // the created-side role `toegekend besluit` is declared on the supertype. The shared decision reaches both roles
        // through the supertype and links them, and the verifier, ref and clingo do too (ref's connecting_pair asks
        // whether a type FILLS a role, which a subtype does): all PASS, uitkering 250, as does the DRL-text branch
        // (PLAYED 1). The strata of this branch derive the back-link from the exact types (Kringen.terugkoppeling), see
        // no relation written, and could let a creation that reads the relation fire before this one has made its
        // links; so the branch refuses the creation by name when it builds.
        CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> speel(jaarbesluit(), gerechtigdeJutter()));
        assertTrue(e.getMessage().contains("objectcreatie: stel het penningbesluit vast — een objectcreatie zonder"
                + " connectingRole-binding waarvan de terugkoppeling uit het typenpaar ('begunstigde' en 'toegekend"
                + " besluit') via een supertype van het onderwerp of van het gemaakte type loopt"), e.getMessage());
    }

    // ---------------- 6. the §4.2 validity window ON the creation ----------------

    @Test void een_creatie_buiten_haar_venster_munt_niet() throws Exception {
        // §4.2 geldigheid on the objectcreatie itself: `geldig vanaf 2030`, replayed as of 2026. Nothing is
        // minted, so the navigated read finds nothing and J1.heffing stays leeg — ref on this exact model and
        // case pins that (and rejects 150 on it). Without an asOfDate ref faults outright ("a validity-bounded
        // form needs an evaluation date"), so both arms carry one.
        Path ir = vanaf2030();
        Na na = naVuur(ir, jutterOp("2026-01-01", null));
        assertTrue(mints(na).isEmpty(), "outside its window the creation mints nothing: " + mints(na));
        assertNull(RegelSpraakRuntime.get(na.invoer().get("J1"), "rekenkamer"), "and links nothing");
        assertNull(speel(ir, jutterOp("2026-01-01", null)));
    }

    @Test void dezelfde_creatie_binnen_haar_venster_munt_wel() throws Exception {
        // the other side of the same window — without it the arm above would also pass on a creation that
        // never fires at all. ref as of 2031 on the same model: J1.heffing = 150.
        Path ir = vanaf2030();
        assertEquals(1, mints(naVuur(ir, jutterOp("2031-06-01", "150"))).size());
        assertNull(speel(ir, jutterOp("2031-06-01", "150")), "ref agrees: 150");
    }

    // ---------------- fixtures ----------------

    /** The refusal building {@code irPath} raises — an assertion failure if it builds at all. */
    private static CaseRunner.Excluded weigering(Path irPath) {
        return org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                () -> speel(irPath, JSON.readTree("""
                        { "format": "uchar-case-v0", "asOfDate": null,
                          "input": { "instances": [], "parameters": {} }, "expected": [] }
                        """)));
    }

    /** One case of {@code irPath} on the truth-maintenance branch, as a consumer plays it through the entry: the input
     *  through {@link DroolsBackend#voegIn} (the replay's set-up), one {@link DroolsBackend#vuur} to the fixpoint, the
     *  fixpoint {@link DroolsBackend#lees} gives, and then {@link DroolsBackend#zetAfgeleideWaarden}, so that the beans
     *  of the input and of the made objects carry the derived values and the links. */
    private static Na naVuur(Path irPath, JsonNode c) throws Exception {
        return CaseRunner.voorMeting(irPath, DroolsBackend.Branch.EXEC_MODEL).metSessie(c,
                (bouw, sessie, labels) -> {
            DroolsBackend.Vuring vuring = DroolsBackend.vuur(bouw, sessie);
            if (vuring.geenFixpunt() != null) throw new IllegalStateException(vuring.geenFixpunt());
            Fixpunt fixpunt = DroolsBackend.lees(bouw, sessie);
            DroolsBackend.zetAfgeleideWaarden(bouw, sessie);
            return new Na(new LinkedHashMap<>(labels), fixpunt);
        });
    }

    /** The input beans of a case by label, and the fixpoint of its session. */
    private record Na(Map<String, Object> invoer, Fixpunt fixpunt) {}

    /** The full replay of case {@code c} on the truth-maintenance branch: null on a match. */
    private static String speel(Path irPath, JsonNode c) throws Exception {
        return CaseRunner.speelCase(irPath, c, DroolsBackend.Branch.EXEC_MODEL);
    }

    /** The objects the creations made, as the fixpoint gives them. */
    private static List<Object> mints(Na na) {
        return na.fixpunt().objecten().stream().map(Fixpunt.GemaaktObject::object).toList();
    }

    private static Object enigeMint(Na na) {
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
        IrPad.versie(ir, "objectCreations", 0, 0).set("initialisations", JSON.createArrayNode());
        return schrijf(ir, "zonder-toekenningen");
    }

    /** The overzee model with the §13.4.6.4 kenmerk toekenning's VALUE replaced — the arms no case input can
     *  reach (a literal true/false, a non-boolean). In memory: no corpus file is read for its expectations,
     *  moved, or touched. */
    private static Path kenmerkWaarde(String waardeJson, String naam) throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(OVERZEE)));
        for (JsonNode t : IrPad.versie(ir, "objectCreations", 0, 0).get("initialisations"))
            if (t.get("characteristic").asBoolean())
                ((com.fasterxml.jackson.databind.node.ObjectNode) t).set("value", JSON.readTree(waardeJson));
        return schrijf(ir, naam);
    }

    /** The rekenkamer model with the SUBJECT-side role (`opdrachtgever`) made MEERVOUDIG — one Rekenkamer
     *  serves many Jutters, so the pair's scalar reference lives on the Jutter and the created type carries no
     *  back-reference field at all. */
    private static Path meervoudigeSubjectrol() throws Exception {
        return schrijf(metCardinaliteit(true, false), "meervoudige-subjectrol");
    }

    /** The mirror: the CREATED-side role (`rekenkamer`) made meervoudig, so the reference lives on the
     *  Rekenkamer and the subject has no forward field. The jutter's navigating rule ("de heffing van haar
     *  rekenkamer") is removed with it — a scalar read over a to-many role is WF-053, so the model would not
     *  be verifier-clean and the witness would pin a shape no corpus document may have. */
    private static Path markerPad() throws Exception {
        return schrijf(zonderNavigerendeRegel(metCardinaliteit(false, true)), "marker-pad");
    }

    /** Both roles meervoudig: neither end derives a bean field. Same WF-053 removal as {@link #markerPad}. */
    private static Path beideRollenMeervoudig() throws Exception {
        return schrijf(zonderNavigerendeRegel(metCardinaliteit(true, true)), "beide-meervoudig");
    }

    /** The rekenkamer model with the taxatieopdracht roles' cardinality set: role 0 is the subject side
     *  (`opdrachtgever`), role 1 the created side (`rekenkamer`). */
    private static com.fasterxml.jackson.databind.node.ObjectNode metCardinaliteit(boolean subjectVeel, boolean gemunteVeel) throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(REKENKAMER)));
        JsonNode rollen = IrPad.declaratie(ir, "factTypes", "taxatieopdracht").get("roles");
        ((com.fasterxml.jackson.databind.node.ObjectNode) rollen.get(0)).put("many", subjectVeel);
        ((com.fasterxml.jackson.databind.node.ObjectNode) rollen.get(1)).put("many", gemunteVeel);
        return ir;
    }

    /** Drops "heffing van een jutter" — the rule that reads THROUGH the created-side role — from the resolved
     *  rule list. Its scalar read is only well-formed while that role is to-one. */
    private static com.fasterxml.jackson.databind.node.ObjectNode zonderNavigerendeRegel(com.fasterxml.jackson.databind.node.ObjectNode ir) {
        var behouden = JSON.createArrayNode();
        for (JsonNode r : IrPad.rulesLijst(ir, "rules"))
            if (!"heffing van een jutter".equals(r.path("name").asText())) behouden.add(r);
        if (behouden.size() != IrPad.rulesLijst(ir, "rules").size() - 1)
            throw new AssertionError("'heffing van een jutter' is not in the document");
        IrPad.rules(ir).set("rules", behouden);
        return ir;
    }

    /** The rekenkamer model with a §4.2 validity window ON the creation: it exists from 2030 onward. */
    private static Path vanaf2030() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(REKENKAMER)));
        IrPad.versie(ir, "objectCreations", 0, 0)
                .set("validity", JSON.readTree("""
                        { "node": "Validity", "from": "2030-01-01" }"""));
        return schrijf(ir, "vanaf-2030");
    }

    /** J1 (240/90) replayed AS OF {@code datum}; {@code heffing} null expects the attribute to stay leeg. */
    private static JsonNode jutterOp(String datum, String heffing) throws Exception {
        String verwachting = heffing == null
                ? "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"heffing\" }"
                : "{ \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"heffing\", \"value\": { \"number\": \"" + heffing + "\" } }";
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": "%s",
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "grondslag": { "number": "240" }, "aftrek": { "number": "90" } },
                      "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """).formatted(datum, verwachting));
    }

    /** The rekenkamer model with a §8.1.1 comparison guarding the creation. */
    private static Path metVoorwaarde() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(REKENKAMER)));
        IrPad.versie(ir, "objectCreations", 0, 0)
                .set("condition", JSON.readTree("""
                        { "node": "Comparison", "op": "greater",
                          "left": { "node": "AttributeReference", "attribute": "grondslag", "subjectType": "Jutter",
                                     "source": { "snippet": "de grondslag van de Jutter" } },
                          "right": { "node": "Literal", "value": { "number": "100" }, "type": { "node": "Number", "whole": true, "decimals": 0,
                                                "sign": "unspecified" }, "source": { "snippet": "100" } },
                          "source": { "snippet": "de grondslag van de Jutter is groter dan 100" } }
                        """));
        return schrijf(ir, "met-voorwaarde");
    }

    /** W1 with the given rank, replayed against the ref-verified wachtersketen expectations: the chain advances
     *  ONE generation from rank 4 (the creation's voorwaarde is rank &lt; 5), so exactly one mint exists. */
    private static JsonNode wachter(String rang) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "W1", "objectType": "Wachter",
                      "attributes": { "rang": { "number": "%s" } }, "roles": {}, "characteristics": [] } ],
                      "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "W1", "attribute": "opvolgerrang",
                                  "value": { "number": "5" } },
                                { "kind": "inconsistent", "rule": "rangdetector" } ] }
                """).formatted(rang));
    }

    /** One Jutter, no expectations asserted — the input a fault witness needs (the rule must have a subject). */
    private static JsonNode jutterZonderVerwachting() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": {}, "roles": {}, "characteristics": [] } ], "parameters": {} },
                  "expected": [] }
                """);
    }

    /** The penninguitgifte model without its two object creations: nothing creates `penningbezit`, so the relation is
     *  input and a read of it goes over the bean fields. */
    private static Path zonderCreaties() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(PENNING)));
        IrPad.rules(ir).set("objectCreations", JSON.createArrayNode());
        return schrijf(ir, "zonder-creaties");
    }

    /** The penningbesluit model with a subtype `Jaarbesluit` of `Penningbesluit`, which the creation makes instead:
     *  the connecting relation `penningtoekenning` declares its role `toegekend besluit` on the supertype. */
    private static Path jaarbesluit() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(PENNINGBESLUIT)));
        IrPad.modelLijst(ir, "objectTypes").add(JSON.readTree("""
                { "node": "ObjectType", "name": "Jaarbesluit", "supertype": "Penningbesluit", "animate": false,
                  "plural": "Jaarbesluiten", "attributes": [], "characteristics": [] }"""));
        IrPad.versie(ir, "objectCreations", 0, 0).put("createdType", "Jaarbesluit");
        return schrijf(ir, "jaarbesluit");
    }

    /** One penninggerechtigde Jutter for the penningbesluit model (14 vondsten against a vondstdrempel of 12): the
     *  besluit made for him carries the penningbedrag 250 and his penningjaar 2025, which he reads back. */
    private static JsonNode gerechtigdeJutter() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "aantal aangemelde vondsten": { "number": "14" }, "penningjaar": { "number": "2025" } },
                      "roles": {}, "characteristics": [] } ],
                      "parameters": { "vondstdrempel": { "number": "12" }, "penningbedrag": { "number": "250" } } },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "uitkering",
                                  "value": { "number": "250" } },
                                { "kind": "attributeValue", "label": "J1", "attribute": "uitkeringsjaar",
                                  "value": { "number": "2025" } },
                                { "kind": "consistent" } ] }
                """);
    }

    /** The penninguitgifte model with the AANTAL consistentie removed, so the KENMERK assertion is the only
     *  §9.5 construct and the model lowers whole. */
    private static Path zonderHouderscontrole() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(PENNING)));
        IrPad.rules(ir).set("consistencies", zonder(IrPad.rulesLijst(ir, "consistencies"), "houderscontrole"));
        return schrijf(ir, "zonder-houderscontrole");
    }

    /** The same model with the AANTAL consistentie REWRITTEN onto `dossiervoering` — the 1:1 relation where both
     *  directions own a bean field, so a directional presence count IS expressible. */
    private static Path dossierAantalAssertie() throws Exception {
        var ir = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(Files.readString(ir(PENNING)));
        var k = IrPad.versie(ir, "consistencies", "houderscontrole");
        k.put("subject", "Jutter");
        var f = (com.fasterxml.jackson.databind.node.ObjectNode) k.get("assertion").get("left");
        f.put("elementType", "Dossier");
        f.set("sourceRole", JSON.readTree("""
                { "node": "RoleBinding", "factType": "dossiervoering", "role": "dossier" }"""));
        var arg = (com.fasterxml.jackson.databind.node.ObjectNode) f.get("arguments").get(0);
        arg.put("attribute", "dossiers");
        arg.put("subjectType", "Jutter");
        return schrijf(ir, "dossier-aantal");
    }

    private static com.fasterxml.jackson.databind.node.ArrayNode zonder(JsonNode lijst, String naam) {
        var behouden = JSON.createArrayNode();
        for (JsonNode n : lijst) if (!naam.equals(n.path("name").asText())) behouden.add(n);
        if (behouden.size() != lijst.size() - 1) throw new AssertionError("'" + naam + "' is not in the list");
        return behouden;
    }

    /** J3 with a supplied penning P1 and dossier D1; {@code uitgereikt} marks P1, which decides whether the
     *  kenmerk assertion is violated. The pinned values are ref's on the houderscontrole-less model. */
    private static JsonNode penningCase(boolean uitgereikt) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J3", "objectType": "Jutter", "attributes": { "vondstwaarde": { "number": "7" } },
                        "roles": { "penningen": ["P1"], "dossier": ["D1"] }, "characteristics": ["penninggerechtigd"] },
                      { "label": "P1", "objectType": "Penning", "attributes": { "bedrag": { "number": "5" } },
                        "roles": {}, "characteristics": [%s] },
                      { "label": "D1", "objectType": "Dossier", "attributes": {}, "roles": {}, "characteristics": [] } ],
                      "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J3", "attribute": "aantal penningen",
                                  "value": { "number": "2" } }, %s ] }
                """).formatted(uitgereikt ? "\"uitgereikt\"" : "",
                uitgereikt ? "{ \"kind\": \"consistent\" }"
                           : "{ \"kind\": \"attributeValue\", \"label\": \"J3\", \"attribute\": \"aantal dossiers\", \"value\": { \"number\": \"1\" } },"
                             + " { \"kind\": \"inconsistent\", \"rule\": \"penningcontrole\", \"label\": \"P1\" }"));
    }

    /** One dossierless Jutter against the rewritten dossier assertion: the count is 0 and the assertion violated. */
    private static JsonNode jutterMetDossierVerwachting(boolean gerechtigd, String aantal) throws Exception {
        return JSON.readTree(("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ { "label": "J1", "objectType": "Jutter",
                      "attributes": { "vondstwaarde": { "number": "5" } }, "roles": {},
                      "characteristics": [%s] } ], "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "aantal dossiers",
                                  "value": { "number": "%s" } }, %s ] }
                """).formatted(gerechtigd ? "\"penninggerechtigd\"" : "", aantal,
                gerechtigd ? "{ \"kind\": \"consistent\" }"
                           : "{ \"kind\": \"inconsistent\", \"rule\": \"houderscontrole\", \"label\": \"J1\" }"));
    }

    /** The same jutter with the dossier delivered as INPUT (so no creation, hence no rule-ordering dependence):
     *  the count is 1 and nothing is flagged. */
    private static JsonNode jutterMetIngevoerdDossier() throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": { "vondstwaarde": { "number": "5" } },
                        "roles": { "dossier": ["D1"] }, "characteristics": [] },
                      { "label": "D1", "objectType": "Dossier", "attributes": {}, "roles": {}, "characteristics": [] } ],
                      "parameters": {} },
                  "expected": [ { "kind": "attributeValue", "label": "J1", "attribute": "aantal dossiers",
                                  "value": { "number": "1" } },
                                { "kind": "consistent" } ] }
                """);
    }

    private static Path schrijf(JsonNode ir, String naam) throws Exception {
        Path p = Files.createTempDirectory("uchar-oc-" + naam).resolve("ir.json");
        Files.writeString(p, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        p.toFile().deleteOnExit();
        return p;
    }
}
