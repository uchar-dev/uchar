package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

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
 * <p>ref keys links per instance per role key; the bean model has no such key — one scalar field per to-one
 * role, and at 1:N that single field necessarily serves both directions. The two therefore only agree where
 * the field derivation and the case-input write agree with ref's keying. Until 2026-09-10 they did NOT agree,
 * and this file pinned ref's DIRECTED reading as the arbiter; RUNTIME-SEMANTICS ch. 5.7a then decided the
 * other way — a supplied role link ESTABLISHES the relation and BOTH roles read it — so ref's loader mirrors
 * on the sister role and {@code CaseInput.setRoles} writes the second field on a 1:1 shape. The three tests
 * that carried the old reading were converted at that decision and say so at their own site.
 *
 * <p>Gate 12 cannot see any of this: every corpus golden supplies its relations from the end where all
 * executors agree, so the seams below are exactly the ones a wrong storage decision gets wrong while every
 * pinned total still lands. What is pinned here is now agreement, not a characterised divergence — except
 * where a bean-model limit genuinely refuses a form, and those refusals carry their reason.
 *
 * <p>Every expectation was replayed through {@code ref/} on this exact model and case FIRST. The models are
 * corpus goldens' {@code ir.json} off the wire, or an in-memory variant of one; every case is authored here,
 * so no corpus expectation is read, moved or touched.
 */
class StorageDirectionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The ORDINARY self-typed pair: `opvolging` relates Wachter to Wachter through two DISTINCT to-one sibling
     *  roles (`voorganger`, `opvolger`), beside a second, to-many self-pair (`erflijn`). */
    private static final String INHERITANCE_LINE = "anchor/jutterswet-erflijn";
    /** A to-many element role: `vondstaanmelding` pairs a to-one `aanmelder` with a to-many `vondst`, so the
     *  relation has exactly ONE bean field (Vondst.aanmelder) serving both directions. */
    private static final String VONDSTTELLING = "jutterswet-vondsttelling";
    /** The SELF-TYPED creation: a Wachter creates a Wachter over `opvolging` (roles `voorganger`/`opvolger`,
     *  both to-one), reads the fold over its opvolgers, and carries a consistentie. */
    private static final String WACHTERSKETEN = "anchor/jutterswet-wachtersketen";
    /** A to-ONE NAVIGATED value read: `ligplaats` pairs the to-one `werf` with the to-many `schuit`, so the
     *  reference field lives on Schuit, named after the role — the field every navigated read walks. */
    private static final String NAVIGATION_PREMIE = "synth-navigatiepremie";

    // ---------------- 1. the case-input write on an ordinary self-typed pair ----------------

    @Test void a_written_self_pair_role_lands_on_the_field_of_that_role() throws Exception {
        // The by-pair search derives NULL here by construction (connectingRole counts voorganger, opvolger,
        // erflater and erfvolger as candidates), so the case's role KEY is the only route to the field — the
        // very by-name authority FactModelGenerator.selfPairRoles derived the field from. Before this route
        // existed the whole golden was refused ("has no uniquely derivable reference field"), a reason that
        // stopped being true when the card gave the bean its two self-pair fields.
        Map<String, Object> beans = beans(countOverOpvolgers(), chain("\"opvolger\": [\"W2\"]", ""));
        assertSame(beans.get("W2"), RegelSpraakRuntime.get(beans.get("W1"), "opvolger"),
                "the written key 'opvolger' resolves by NAME to the opvolging role and lands on ITS field");
    }

    @Test void the_ordinary_self_pair_link_mirrors_on_the_sister_role() throws Exception {
        // CONVERTED 2026-09-10, maintainer decision on the case-input reading (RUNTIME-SEMANTICS ch. 5.7a):
        // a supplied link ESTABLISHES the relation and BOTH roles read it. This test used to be
        // `de_gewone_zelfpaarkoppeling_spiegelt_niet` and asserted the opposite — that W2's `voorganger` stays
        // null after W1 -> W2 over `opvolging` — on the then-true ground that ref's mirror was gated on
        // ft.reciprocal. That gate is gone: ref's Case.materialise_relations mirrors EVERY two-role relation on
        // the SISTER role, and the 1:1 arm of CaseInput.setRoles writes the second bean field to match.
        // RED FIRST — this assertion measured on this model and case with that write removed again:
        //     expected: <dev.uchar.regelspraak.facts.Wachter@…> but was: <null>
        //
        // What the test still separates is the SISTER role from the WRITTEN one: the mirror fills
        // W2.voorganger, NOT W2.opvolger. A mirror that wrote the same role would make W2 its own successor's
        // predecessor, and the two remaining nulls are what refuse that reading.
        Map<String, Object> beans = beans(countOverOpvolgers(), chain("\"opvolger\": [\"W2\"]", ""));
        assertSame(beans.get("W1"), RegelSpraakRuntime.get(beans.get("W2"), "voorganger"),
                "the reverse leg lands on the SISTER role's field: W1 is W2's voorganger");
        assertNull(RegelSpraakRuntime.get(beans.get("W1"), "voorganger"),
                "and no more than the relation given: nobody is W1's voorganger");
        assertNull(RegelSpraakRuntime.get(beans.get("W2"), "opvolger"),
                "the mirror writes the sister role, never the written one");
    }

    @Test void the_count_reads_the_named_role_not_the_sister_role() throws Exception {
        // The end-to-end consequence the bean-level assertions above do not imply, RENAMED 2026-09-10: its old
        // name (`de_gerichte_koppeling_beslist_de_gelezen_waarde`) carried the DIRECTED reading that
        // RUNTIME-SEMANTICS ch. 5.7a replaced, and its old comment explained the numbers by "a mirrored fill
        // would make both 1". That explanation was wrong even then, and the VALUES are unchanged under the
        // symmetric reading — which is exactly what makes this test worth keeping. Model: the erflijn golden
        // with its count rewired onto the TO-ONE `opvolgers` (an in-memory variant; the corpus ir.json is
        // untouched); the fold counts the role `opvolger`.
        //
        // The mirror fills the SISTER field (W2.voorganger), so W2's `opvolger` is still empty and W2 still
        // counts 0. REMEASURED through ref under ch. 5.7a, on exactly this model and case:
        //     1 / 0  ->  oracle: PASS (2 expectations)
        //     1 / 1  ->  MISMATCH: W2.erfvolgeraantal = 0, expected 1
        // A mirror that wrote the WRITTEN role instead of the sister one would make both 1 — that is the
        // reading this pair refuses, and it refuses it in ref and in the backend alike.
        Path ir = countOverOpvolgers();
        assertNull(CaseRunner.playCase(ir, JSON.readTree(chainWithExpectation("1", "0"))), "ref agrees: W1 1, W2 0");
        assertNotNull(CaseRunner.playCase(ir, JSON.readTree(chainWithExpectation("1", "1"))),
                "counting the sister role instead would be a named mismatch, not a silent pass");
    }

    @Test void the_many_to_many_self_relation_plays_with_truth_maintenance() throws Exception {
        // `erflijn` relates Wachter to Wachter through TWO TO-MANY roles, so the bean model derives no field at
        // EITHER end — a to-many role's storage is the far end's field, and here both ends are to-many. The
        // destructive exec-model branch joined the aggregation over `erfvolgers` on such a field and refused it by
        // name (§13.3.9 storage direction). The exec-model branch with truth maintenance joins over the links of the
        // relation, which need no field, and answers as ref on the erflijn golden with this case: W1 1, W2 0
        // (TmsLikeRefWitnessTest, many-to-many-self-relation).
        assertNull(playTms(ir(INHERITANCE_LINE), JSON.readTree(chainWithExpectation("1", "0"))), "ref agrees: W1 1, W2 0");
    }

    @Test void a_multiple_self_pair_role_is_refused_loudly_with_the_true_reason() throws Exception {
        // What is LEFT of the refusal after the by-name route: `erfvolger` resolves by name, but it is TO-MANY,
        // so its storage is the far end's field — and on a self-typed pair that end is the same bean, which the
        // by-pair search cannot name either. ref carries this case (it keys the link under 'erfvolger' and
        // counts 1 on W1), so the backend must refuse it BY NAME rather than pretend the field is underivable.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> beans(countOverOpvolgers(), chain("\"erfvolger\": [\"W2\"]", "")));
        assertTrue(e.getMessage().contains("no to-one reference field"), e.getMessage());
        assertTrue(e.getMessage().contains("Wachter -> Wachter"), e.getMessage());
    }

    // ---------------- 2. which end an aggregation reads ----------------

    @Test void the_one_to_one_count_reads_the_field_of_the_subject() throws Exception {
        // §13.3.9 forward arm: with BOTH roles to-one each direction owns a field, so the fold must read the
        // SUBJECT's own reference (Jutter.vondst), not the element's back-reference. Model: vondsttelling with
        // the `vondst` role clipped to to-one; case supplies the link from the SUBJECT side. ref on exactly
        // this model and case: J1 counts 1 and sums 2.50.
        Path ir = vondstRoleToOne();
        assertNull(CaseRunner.playCase(ir, JSON.readTree(count("\"vondsten\": [\"V1\"]", "", "", "1", "2.50"))),
                "ref agrees: 1 / 2.50");
    }

    @Test void the_same_count_also_reads_a_delivery_to_the_element_side() throws Exception {
        // CONVERTED 2026-09-10 (RUNTIME-SEMANTICS ch. 5.7a). This test used to be
        // `dezelfde_telling_negeert_een_levering_aan_de_elementkant` and pinned 0 / 0: the SAME 1:1 model with
        // the link supplied from the ELEMENT side (V1 names its aanmelder) left the subject's own field empty,
        // so the fold found nothing — and ref, which keyed links per instance per role key, agreed.
        //
        // Under the decided reading the case names ONE LEG of a relation that has two, so J1's own `vondst`
        // field is written too and the fold reads 1 / 2.50 — the same numbers the subject-side supply gives in
        // `the_one_to_one_count_reads_the_field_of_the_subject` above. That pair is now the point: on a 1:1
        // relation the two spellings of one relation must compute the same thing. RED FIRST — the 1 / 2.50
        // assertion measured on this model and case with the 1:1 mirror write removed again:
        //     case: attribuut aantal aangemelde vondsten: verwacht 1, werkelijk 0;
        //           attribuut aangemeld gewicht: verwacht 2.50, werkelijk 0
        // and ref, remeasured under ch. 5.7a on exactly this model and case, answers 1 / 2.50 from EITHER end
        // (`oracle: PASS (2 expectations)` on both spellings; the old 0 / 0 is a named MISMATCH there now).
        Path ir = vondstRoleToOne();
        assertNull(CaseRunner.playCase(ir, JSON.readTree(count("", "\"aanmelder\": [\"J1\"]", "", "1", "2.50"))),
                "ref agrees: 1 / 2.50 — the element-side supply establishes the same relation");
        assertNotNull(CaseRunner.playCase(ir, JSON.readTree(count("", "\"aanmelder\": [\"J1\"]", "", "0", "0"))),
                "and the old directed reading is a named mismatch now, not a silent pass");
    }

    @Test void the_many_sister_role_counts_the_element_side_along_and_ref_now_too() throws Exception {
        // RETIRED AS A DIVERGENCE 2026-09-10 — the gap this test was written to keep visible is CLOSED, and it
        // closed from the SPEC side rather than the backend side. It used to read
        // `de_meervoudige_zusterrol_telt_de_elementkant_wel_mee_bekende_restlacune` and characterise:
        //     ref    -> J1 aantal 0, gewicht 0     (nothing was keyed on J1)
        //     Drools -> J1 aantal 2, gewicht 3.75  (both elements fold in)
        // with the note "it FAILS WHEN THE GAP CLOSES". It does not fail, because the backend's numbers were
        // the RIGHT ones: with a TO-MANY element role the relation has exactly ONE field (Vondst.aanmelder) and
        // it necessarily serves both directions, so no engine on this fact model can read such a link as
        // directed. RUNTIME-SEMANTICS ch. 5.7a decided the reading that way — a supplied link establishes the
        // relation, both roles read it — and ref's loader now mirrors on the sister role. REMEASURED on the
        // UNCLIPPED vondsttelling golden, element-end supply: ref `oracle: PASS (2 expectations)` on 2 / 3.75.
        // Three executors, one answer; what is left to pin is that agreement.
        assertNull(CaseRunner.playCase(ir(VONDSTTELLING),
                        JSON.readTree(count("", "\"aanmelder\": [\"J1\"]", "\"aanmelder\": [\"J1\"]", "2", "3.75"))),
                "ref agrees: 2 / 3.75 — one field, both directions, one reading");
    }

    // ---------------- 3. the name-sharing displacement: a role name a declared member already owns ----------------

    // The trap, in one sentence: FactModelGenerator.members ports every member through a NAME-DEDUP (attribuut >
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

    @Test void the_creation_arm_refuses_a_name_sharing_back_link() throws Exception {
        // The §13.4.6 arm: the creation's own connectingRole binding names `opvolger`, whose field the attribute
        // took. ref on exactly this model and case: `oracle: PASS (1 expectations)` — the model is ref-clean AND
        // verifier-clean (`verifier: PASS`), so this is not a broken program filed under "not carried": it is a
        // bean-model limit of THIS backend, which must therefore say so per creation.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.playCase(nameSharingWachter(false), JSON.readTree(chainWithoutRoles(false))));
        assertTrue(e.getMessage().contains("objectcreatie: werf de opvolger"), e.getMessage());
        assertTrue(e.getMessage().contains("deelt haar naam met een gedeclareerd lid"), e.getMessage());
    }

    @Test void the_case_role_input_refuses_the_same_field() throws Exception {
        // The case-input arm, on the same collision with the rules and the creation stripped away (they refuse
        // first, and each arm must be witnessed on its own). ref on exactly this model and case: `oracle: PASS
        // (2 expectations)` — ref keys the link under the role name and the attribute beside it, untroubled.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.inputBeans(nameSharingInputWachter(), JSON.readTree(chainWithSuccessorRole())));
        assertTrue(e.getMessage().contains("role 'opvolger' resolves to the to-one role 'opvolger'"), e.getMessage());
        assertTrue(e.getMessage().contains("name-sharing bean limit"), e.getMessage());
    }

    @Test void the_foldjoin_refuses_the_same_field_with_the_same_reason() throws Exception {
        // The third reader of that field — the §5.8 fold's join (`het aantal opvolgers`) — asked for the name
        // only, so it lowered a join reading a BigDecimal. On the FULL model this is the first refusal reached,
        // which is why the creation witness above strips the rules. ref: `oracle: PASS (2 expectations)`. The
        // exec-model branch reaches the creation first, whose link to the mint has no field.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> playTms(nameSharingWachter(true), JSON.readTree(chainWithoutRoles(true))));
        assertTrue(e.getMessage().contains("objectcreatie: werf de opvolger — de rol 'opvolger' op Wachter deelt haar naam"
                + " met een gedeclareerd lid"), e.getMessage());
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
    // with the displacing attribute LEFT EMPTY plays green, so the corpus can carry the broken model unseen.
    // Since L2 of the v1 wire migration a one-hop read is one binding, which navReader reads; the chain reader
    // (navKetenReader) takes a read of two bindings or more, and asks the same question at EVERY hop.

    @Test void the_navigated_value_read_refuses_the_displaced_field() throws Exception {
        // navReader — the read as the wire carries it: one binding, ligplaats/werf.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> playTms(nameSharingSchuit(), JSON.readTree(schuitWithoutRole("premie", "0"))));
        assertTrue(e.getMessage().contains("regel: premie van een schuit — de lezing 'het tarief van de werf van de Schuit':"
                + " Schuit heeft geen referentieveld 'werf' naar Werf (§13.3.9 opslagrichting)"), e.getMessage());
    }

    @Test void the_chain_read_refuses_the_displaced_field_at_each_step() throws Exception {
        // navKetenReader — a read of two bindings, Schuit -(ligplaats/werf)-> Werf -(werfhaven/thuishaven)-> Haven.
        // The displacing member sits at the FIRST hop (Schuit.werf) in one model and at the SECOND (Werf.thuishaven)
        // in the other; both branches name the hop whose field is gone and never read past it. In the exec branch
        // the question is asked twice, by the chain reader and by the far-side reactivity walk (navReactiviteit),
        // so this row holds the construct's refusal, not one of the two sites alone (measured: dropping the
        // chain reader's per-hop question leaves the row green).
        for (int step : new int[] { 1, 2 }) {
            String role = step == 1 ? "de rol 'werf' op Schuit" : "de rol 'thuishaven' op Werf";
            String field = step == 1 ? "Schuit heeft geen referentieveld 'werf' naar Werf"
                    : "Werf heeft geen referentieveld 'thuishaven' naar Haven";
            BothBranches.refused(nameSharingChain(step), JSON.readTree(schuitWithoutRole("premie", "0")),
                    "regel: premie van een schuit — de lezing 'het havengeld van de thuishaven van de werf van de Schuit': "
                            + field + " (§13.3.9 opslagrichting)",
                    "the displaced field at hop " + step + " of a two-binding chain");
        }
    }

    @Test void the_navigated_comparison_refuses_the_same_field() throws Exception {
        // navComparisonRole — the CONDITION side, which reads the relation field itself (§5.5.5: a null relation
        // is a leegwaarde). On the pre-gate jar this arm did not even reach a wrong value: the constraint
        // evaluated a BigDecimal as a fact and aborted the fire with an unclassified
        // ERROR ConstraintEvaluationException, which gate 12 refuses to excuse. ref on exactly this model and
        // case: toeslag stays leeg (leeg > 5 is onwaar, Tabel 33-44), `oracle: PASS (1 expectations)`.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> playTms(navigatingComparison(), JSON.readTree(schuitWithoutRole(null, null))));
        assertTrue(e.getMessage().contains("regel: toeslag van een schuit — de lezing 'het tarief van de werf van de"
                + " Schuit': Schuit heeft geen referentieveld 'werf' naar Werf (§13.3.9 opslagrichting)"), e.getMessage());
    }

    @Test void the_role_membership_check_refuses_the_same_field() throws Exception {
        // The FIFTH reader, and the one that does NOT navigate: the §8.1.7 near-side rolcheck decides whether the
        // subject fills a role by NULL-TESTING that same reference field ("is een schuit" -> `werf != null`). That
        // is why it sat outside the navigated four — and why it is the worst of the family: a displacing attribute
        // is non-null whenever it holds a value, so the check reads that VALUE as proof of role membership and
        // fires a rule ref and clingo leave unfired. Measured on the pre-gate jar, on a verifier-clean and
        // ref-clean model, in BOTH transpiler branches identically:
        //     ref / clingo : S1.toeslag leeg   (S1 fills no role — no link was written)
        //     Drools       : FAIL "verwacht leeg, werkelijk 8"   (the attribute's 5 read as a filled role)
        // Not a navigation bug and not fixed by gating the navigated readers: it needs the SAME shared question,
        // asked at the null-test.
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> playTms(nameSharingRoleCheck(), JSON.readTree(schuitWithoutRole(null, null))));
        assertTrue(e.getMessage().contains("regel: toeslag van een schuit — een RoleMembership in de voorwaarde ('de"
                + " Schuit een schuit is'): de rol 'werf' op Schuit deelt haar naam met een gedeclareerd lid"), e.getMessage());
        assertTrue(e.getMessage().contains("§13.3.9 opslagrichting"), e.getMessage());
    }

    // navReactiviteit — the fourth hop reader — gets the same gate but NO witness of its own, and the reason is
    // structural: it is reached only from the scalar value path AFTER that path's own reader lowered, and both
    // readers now gate every hop they walk, so a hop without a field cannot arrive there. The gate stays because
    // navRefs walks argument positions the value path does not necessarily lower through (a Functie's arguments),
    // and a silently DROPPED far-side watch is a stale value, not a refusal — the one outcome that method exists
    // to prevent. It refuses the construct rather than continuing past the hop.

    // ---------------- fixtures ----------------

    /** The replay of one case on the exec-model branch: null on a match. */
    private static String playTms(Path ir, com.fasterxml.jackson.databind.JsonNode c) throws Exception {
        return CaseRunner.playCase(ir, c, DroolsBackend.Branch.EXEC_MODEL);
    }

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static Map<String, Object> beans(Path irPath, String caseJson) throws Exception {
        return CaseRunner.inputBeans(irPath, JSON.readTree(caseJson));
    }

    /** Two Wachters with the given role objects, and the given erfvolgeraantal expectations (empty for none). */
    private static String chain(String rolesW1, String expected) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Wachter", "attributes": {},
                        "roles": { %s }, "characteristics": [] },
                      { "label": "W2", "objectType": "Wachter", "attributes": {},
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(rolesW1, expected);
    }

    private static String chainWithExpectation(String w1, String w2) {
        return chain("\"opvolger\": [\"W2\"]", """
                { "kind": "attributeValue", "label": "W1", "attribute": "erfvolgeraantal", "value": { "number": "%s" } },
                { "kind": "attributeValue", "label": "W2", "attribute": "erfvolgeraantal", "value": { "number": "%s" } }"""
                .formatted(w1, w2));
    }

    /** One Jutter and two Vondsten (2.50 kg, 1.25 kg) with the relation written on the given sides, plus the
     *  pinned fold over the jutter. */
    private static String count(String rolesJ1, String rolesV1, String rolesV2, String count, String weight) {
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
                """.formatted(rolesJ1, rolesV1, rolesV2, count, weight);
    }

    /** The vondsttelling model with the `vondst` role clipped to TO-ONE — the 1:1 shape in which each direction
     *  owns its own bean field. An in-memory variant written to a temp file; the corpus ir.json is untouched. */
    private static Path vondstRoleToOne() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(VONDSTTELLING)));
        for (JsonNode r : IrPath.declaration(ir, "factTypes", "vondstaanmelding").get("roles"))
            if ("vondst".equals(r.get("name").asText())) ((ObjectNode) r).put("many", false);
        return temporary("vondst-een-op-een", ir);
    }

    /** The erflijn model with its count rewired from the to-many `erfvolgers` onto the TO-ONE `opvolgers` — the
     *  two feitcreaties and the recursive regelgroep that fed the `erflijn` relation dropped with it. The
     *  `erflijn` feittype itself STAYS, so the model keeps both self-pair shapes: the to-one one the bean model
     *  can hold, and the double-to-many one it cannot. An in-memory variant; the corpus ir.json is untouched. */
    private static Path countOverOpvolgers() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(INHERITANCE_LINE)));
        IrPath.rules(ir).putArray("factCreations");
        IrPath.rules(ir).putArray("ruleGroups");
        ObjectNode value = (ObjectNode) IrPath.version(ir, "rules", 0, 0).get("value");
        ((ObjectNode) value.get("arguments").get(0)).put("attribute", "opvolgers");
        ((ObjectNode) value.get("sourceRole")).put("factType", "opvolging").put("role", "opvolger");
        return temporary("erflijn-telling-op-opvolgers", ir);
    }

    /** The wachtersketen model with a Numeriek attribute that SHARES the self-pair role name `opvolger`, so
     *  members()'s name-dedup displaces the reference field. {@code withFold} keeps the §5.8 fold rule (whose join
     *  reads that same field, and refuses FIRST); without it the §13.4.6 creation arm is the refusal reached.
     *  Both are in-memory variants; the corpus ir.json is untouched. Both are verifier-clean and ref-clean. */
    private static Path nameSharingWachter(boolean withFold) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(WACHTERSKETEN)));
        IrPath.addAttribute(IrPath.objectType(ir, "Wachter"), "opvolger", number());
        if (!withFold) IrPath.rules(ir).putArray("rules");
        return temporary("wachter-naamdeling-" + (withFold ? "fold" : "creatie"), ir);
    }

    /** The same collision on an INPUT-ONLY model — creation, rules, group and consistentie dropped — so the
     *  case-input arm is reached instead of a lowering refusal. Verifier-clean and ref-clean. */
    private static Path nameSharingInputWachter() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(WACHTERSKETEN)));
        IrPath.addAttribute(IrPath.objectType(ir, "Wachter"), "opvolger", number());
        ObjectNode rules = IrPath.rules(ir);
        rules.putArray("objectCreations"); rules.putArray("rules");
        rules.putArray("consistencies"); rules.putArray("ruleGroups");
        return temporary("wachter-naamdeling-invoer", ir);
    }

    /** The navigatiepremie golden with a Numeriek attribute that SHARES the to-one role name `werf`, so members()'s
     *  name-dedup displaces Schuit's reference field, reduced to its `premie` rule (`20% van het tarief van de
     *  werf van de Schuit`, one binding). Verifier-clean and ref-clean; the corpus ir.json is untouched. */
    private static Path nameSharingSchuit() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(NAVIGATION_PREMIE)));
        IrPath.addAttribute(IrPath.objectType(ir, "Schuit"), "werf", number());
        keepRule(ir, "premie van een schuit");
        return temporary("schuit-naamdeling", ir);
    }

    /** The premie rule reading a Haven's havengeld over two hops — ligplaats/werf, then a new to-one
     *  werfhaven/thuishaven — with a Numeriek member displacing the field of hop {@code step}: {@code werf} on
     *  Schuit, or {@code thuishaven} on Werf. */
    private static Path nameSharingChain(int step) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(NAVIGATION_PREMIE)));
        ObjectNode haven = IrPath.modelList(ir, "objectTypes").addObject();
        haven.put("node", "ObjectType").put("name", "Haven").put("animate", false);
        haven.putArray("attributes");
        haven.putArray("characteristics");
        IrPath.addAttribute(haven, "havengeld", number());
        ObjectNode ft = IrPath.modelList(ir, "factTypes").addObject();
        ft.put("node", "FactType").put("name", "werfhaven").put("reciprocal", false);
        ft.putArray("roles")
          .add(JSON.createObjectNode().put("node", "Role").put("name", "thuishaven").put("objectType", "Haven")
                  .put("many", false))
          .add(JSON.createObjectNode().put("node", "Role").put("name", "havenwerf").put("objectType", "Werf")
                  .put("plural", "havenwerven").put("many", true));
        if (step == 1) IrPath.addAttribute(IrPath.objectType(ir, "Schuit"), "werf", number());
        else IrPath.addAttribute(IrPath.objectType(ir, "Werf"), "thuishaven", number());
        keepRule(ir, "premie van een schuit");
        ObjectNode chain = (ObjectNode) IrPath.version(ir, "rules", 0, 0).get("value").get("right");
        chain.put("attribute", "havengeld").put("subjectType", "Haven")
             .set("source", IrPath.sourceReference("het havengeld van de thuishaven van de werf van de Schuit"));
        chain.putArray("path")
             .add(JSON.createObjectNode().put("node", "RoleBinding").put("factType", "ligplaats").put("role", "werf"))
             .add(JSON.createObjectNode().put("node", "RoleBinding").put("factType", "werfhaven")
                     .put("role", "thuishaven"));
        return temporary("keten-naamdeling-" + step, ir);
    }

    /** The same collision on the CONDITION side: the `toeslag` rule with its voorwaarde operand replaced by the
     *  navigated read (`het tarief van de werf van de Schuit groter is dan 5`) and its VALUE reduced to an own
     *  attribute, so the refusal can only come from the comparison's hop reader. Verifier-clean and ref-clean. */
    private static Path navigatingComparison() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(NAVIGATION_PREMIE)));
        IrPath.addAttribute(IrPath.objectType(ir, "Schuit"), "werf", number());
        keepRule(ir, "toeslag van een schuit");
        ObjectNode rule = IrPath.version(ir, "rules", 0, 0);
        ObjectNode nav = JSON.createObjectNode();
        nav.put("node", "AttributeReference").put("attribute", "tarief").put("subjectType", "Werf");
        nav.set("source", IrPath.sourceReference("het tarief van de werf van de Schuit"));
        nav.putArray("path").addObject()
           .put("node", "RoleBinding").put("factType", "ligplaats").put("role", "werf");
        ((ObjectNode) rule.get("condition")).set("left", nav);
        ((ObjectNode) rule.get("condition")).set("source", IrPath.sourceReference("het tarief van de werf van de Schuit groter is dan 5"));
        ObjectNode own = JSON.createObjectNode();
        own.put("node", "AttributeReference").put("attribute", "lengte").put("subjectType", "Schuit");
        own.set("source", IrPath.sourceReference("de lengte van de Schuit"));
        rule.set("value", own);
        rule.put("initialisation", false).put("presentation", "equation");
        return temporary("schuit-naamdeling-vergelijking", ir);
    }

    /** The same collision under a §8.1.7 ROLE CHECK stored on the subject: the `toeslag` rule keeps its
     *  own-attribute value and its voorwaarde becomes "de Schuit een schuit is" (the Schuit plays `schuit` in
     *  ligplaats), whose lowering null-tests Schuit's `werf` reference — the field the attribute displaced.
     *  Verifier-clean and ref-clean (ref: toeslag stays leeg). */
    private static Path nameSharingRoleCheck() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(ir(NAVIGATION_PREMIE)));
        IrPath.addAttribute(IrPath.objectType(ir, "Schuit"), "werf", number());
        keepRule(ir, "toeslag van een schuit");
        ObjectNode rule = IrPath.version(ir, "rules", 0, 0);
        ObjectNode check = JSON.createObjectNode();
        check.put("node", "RoleMembership").put("factType", "ligplaats").put("role", "schuit")
             .put("negated", false).set("source", IrPath.sourceReference("de Schuit een schuit is"));
        rule.set("condition", check);
        ObjectNode own = JSON.createObjectNode();
        own.put("node", "AttributeReference").put("attribute", "lengte").put("subjectType", "Schuit");
        own.set("source", IrPath.sourceReference("de lengte van de Schuit"));
        rule.set("value", own);
        rule.put("initialisation", false).put("presentation", "equation");
        return temporary("schuit-naamdeling-rolcheck", ir);
    }

    /** Reduce a model to ONE named rule in its resolved list — so each arm below is witnessed on its own refusal
     *  and not on a sibling rule that refuses first. */
    private static void keepRule(ObjectNode ir, String name) {
        com.fasterxml.jackson.databind.node.ArrayNode keep = JSON.createArrayNode();
        keep.add(IrPath.form(ir, "rules", name));
        IrPath.rules(ir).set("rules", keep);
    }

    /** One Werf (tarief 312) and one Schuit that fills the DISPLACING attribute (werf 5) and lengte 8, with NO
     *  role written — the shape that never reaches the case-input gate. {@code attribute} null pins the toeslag
     *  as leeg instead of a value. */
    private static String schuitWithoutRole(String attribute, String value) {
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
                """.formatted(attribute == null
                ? "{ \"kind\": \"attributeEmpty\", \"label\": \"S1\", \"attribute\": \"toeslag\" }"
                : "{ \"kind\": \"attributeValue\", \"label\": \"S1\", \"attribute\": \"" + attribute
                  + "\", \"value\": { \"number\": \"" + value + "\" } }");
    }

    private static ObjectNode number() {
        return JSON.createObjectNode().put("node", "Number").put("whole", true)
                .put("decimals", 0).put("sign", "unspecified");
    }

    /** W1 (rang 1), no roles — the golden's own case1, with the fold expectation dropped where that rule is. */
    private static String chainWithoutRoles(boolean withFold) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Wachter", "attributes": { "rang": { "number": "1" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s
                      { "kind": "inconsistent", "rule": "rangdetector" } ] }
                """.formatted(withFold
                ? "{ \"kind\": \"attributeValue\", \"label\": \"W1\", \"attribute\": \"opvolgerrang\","
                  + " \"value\": { \"number\": \"2\" } },"
                : "");
    }

    /** Two wachters with the self-pair role written BY NAME onto the displaced field, expectations echoing the
     *  inputs (the input-only model computes nothing). */
    private static String chainWithSuccessorRole() {
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

    private static Path temporary(String name, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(name, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
