package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.10 — the FENCES a Let-cluster node must keep, the structural WALKS that must descend into
 * it, and the two paths on which a companion must NOT FIRE, witnessed via {@link CaseRunner#playCase} over
 * models authored HERE (no corpus file is read, moved or touched).
 *
 * <p>The sibling {@code LetClusterWitnessTest} pins what the Let cluster COMPUTES. This file pins the other
 * half — what a walk that stops at a cluster node MISSES. All three of them ({@code Let},
 * {@code BinderRef}, {@code IfThenElse}) are ordinary {@code Expr} union nodes, so the generic
 * {@code Expr.contains} descends them by construction; the walks pinned here were the HAND-WRITTEN ones in
 * {@code ExecModelTranspiler}, the destructive lowering until card tms-omschakeling (2026-10-10), which
 * matched concrete v0 node shapes and therefore reached a cluster node's children only by taking
 * {@code letKinderen}. One that does not under-reports in SILENCE: a missing §9.5
 * fout-companion reads as "consistent", a missing read reads as "nothing to watch". Each witness below is a
 * model on which that blindness is OBSERVABLE, and each was measured against {@code ref/} on this exact
 * model+case before it was pinned here — ref is the arbiter, this backend the third executor.
 *
 * <ul>
 *   <li><b>the two fences.</b> The cross-unit guard in the ch. 10.2 branch condition (whose absence picks the
 *       WRONG branch — 500 m &gt; 1 km read as 500 &gt; 1) was a refusal of the destructive exec-model branch; the
 *       exec-model branch with truth maintenance restates the comparison and answers as ref, which that row pins since
 *       card tms-omschakeling. The ch. 7 defaults division fence (which must keep firing through a Let) is asserted as
 *       a reason-carrying exclusion, not as a value;</li>
 *   <li><b>the walks that must SEE.</b> The far-side reactivity ({@code navRefs}) and the defaults salience
 *       ({@code exprLeesnamen}) each over a producer FORCED by §9.2 stratification to fire after its consumer,
 *       so the witness is deterministic rather than agenda luck;</li>
 *   <li><b>the three §9.5/exactness walks.</b> A Let-wrapped division by leeg (§6.5) and a Let-wrapped wortel
 *       of a negative number (§6.6) must raise the SAME Inconsistentie as the bare form — each is pinned
 *       beside its bare twin in one method, so an implementation that raises neither fails on the twin too —
 *       and a Breuk behind a Let must route the comparison through the exact helper instead of a
 *       {@code (BigDecimal)} cast that throws;</li>
 *   <li><b>the two paths on which a companion must NOT fire.</b> A §6.5 division in the branch ch. 10.2 does
 *       not take, and one sitting behind a read that raised ch. 10.6's leeg-barrier first. The walks DO
 *       descend into both — a STATIC walk cannot know which ch. 10.2 branch will run, and under call-by-need a
 *       Let body is always ENTERED (ch. 10.6 clauses 1+3; clause 2 may still cut it short at a read) — so
 *       blindness there would MISS a real fault, and what keeps the companion silent is its path probe, which
 *       re-evaluates the outermost ch. 4.10 node on the path and deposits only when a §6.5/§6.6 site under it
 *       really ran (that node is the probe's HOST — {@code Site.carrier} of the destructive lowering's
 *       {@code ExecModelTranspiler}, until card tms-omschakeling). A companion
 *       fired for code that never executed is INCONSISTENT where ref says consistent, which is a wrong ANSWER, not an
 *       over-cautious refusal. Each is pinned beside the flank where the fault IS real.</li>
 * </ul>
 */
class LetCarrierWalkWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- 1. the fences: a loud refusal, never a quiet answer ----------------

    @Test void the_cross_unit_fence_recomputes_and_chooses_the_arm_of_ref() throws Exception {
        // "uit := Let a = aanloop in (als a groter dan 1 km dan 1,00 anders 2,00)", aanloop = 500 m.
        // ref on this exact model+case: uit = 2.00 (the §5.1 Tabel 32 restatement makes it 500 m > 1000 m,
        // which is FALSE). The destructive exec-model branch applied NO restatement there, so it refused the
        // rule rather than compare the raw numbers 500 > 1 and take the DAN branch (1.00, silently wrong). The
        // exec-model branch restates the comparison (RUNTIME-SEMANTICS ch. 2.6) and answers 2.00, consistent,
        // as ref; clingo answers 1.00, an existing fault of the ASP backend (card
        // 20261010-clingo-let-binder-vergelijking-over-eenheden). The document and case are those of
        // TmsLikeRefWitnessTest's let-kruiseenheid.
        assertNull(playTms(crossUnitModel(), caseOf(inst("B1", "Baan", "\"aanloop\": { \"number\": \"500\" }", ""),
                valueJson("B1", "uit", "2.00") + ", " + consistent())), "de herrekende vergelijking kiest de ANDERS-tak");
    }

    @Test void the_defaults_division_fence_fires_through_a_let() throws Exception {
        // The ch. 7 defaults lowering refuses a consequence that can DEPOSIT in the §6.5 fout channel, because
        // a default that is evaluated and then discarded would leave the Inconsistentie behind. The division
        // here sits behind a Let ("Let d = deler in inkomen gedeeld door d"), so a bevatDeling that does not take
        // letKinderen would let the group through and the fence would be a dead letter on every such default.
        // ref on this model+case: toeslag = 5.0000 (kindertal 3, inkomen 10.00, deler 2.00) — a value this
        // backend deliberately does NOT produce; the honest outcome here is the refusal, and it names the fence.
        CaseRunner.Excluded refusal = assertThrows(CaseRunner.Excluded.class,
                () -> play(defaultsDivisionModel(), caseOf(inst("J1", "Jutter",
                        "\"kindertal\": { \"number\": \"3\" }, \"inkomen\": { \"number\": \"10.00\" }, "
                                + "\"deler\": { \"number\": \"2.00\" }", ""),
                        valueJson("J1", "toeslag", "5.0000") + ", " + consistent())));
        assertTrue(refusal.getMessage().contains("deling"),
                "de weigering noemt het delingshek: " + refusal.getMessage());
    }

    // ---------------- 2. the walks that must descend into a ch. 4.10 node ----------------

    @Test void a_let_hidden_navigated_read_keeps_the_far_side_reactive() throws Exception {
        // "uit := Let k = het afgeleid van zijn strandmaat in k maal 2,00" — the read is NAVIGATED, so the
        // consumer's reactivity depends on navRefs seeing it. Its producer ("afgeleid") is FORCED into a lower
        // stratum (§9.2: it reads a negated kenmerk that has a producer, salience -1), so it fires AFTER this
        // rule. Without descending the Let, the far-side pattern carries no constraint on `afgeleid`, the rule computes once
        // against a leeg partner value (maal coalesces it to 0) and never recomputes.
        // ref on this exact model+case: J1.afgeleid 30.0000, J2.afgeleid 60.0000, J1.uit 120.000000,
        // J2.uit 60.000000 (the fixpoint; the strandmaatschap is wederkerig, so J2 reads J1 back).
        assertNull(play(farSideReactivityModel(), caseOf(
                inst("J1", "Jutter", "\"strandtegoed\": { \"number\": \"10.00\" }", "\"strandmaat\": [\"J2\"]")
                        + ", " + inst("J2", "Jutter", "\"strandtegoed\": { \"number\": \"20.00\" }", ""),
                valueJson("J1", "afgeleid", "30.0000") + ", " + valueJson("J2", "afgeleid", "60.0000") + ", "
                        + valueJson("J1", "uit", "120.000000") + ", " + valueJson("J2", "uit", "60.000000") + ", "
                        + consistent())));
    }

    @Test void a_let_hidden_rule_derived_read_gets_its_defaults_salience() throws Exception {
        // TWO defaults members over one target, declared unordered — a conflict-capable ch. 7 group, whose
        // members must fire only after everything they READ is derived. Member A's consequence hides its read
        // of the RULE-DERIVED `grondslag` behind a Let ("Let g = grondslag in g"); its producer is again forced
        // into a lower stratum. Without descending the Let, exprLeesnamen reports no read, the group's salience does not
        // sink below the producer, and member A decides on a leeg grondslag.
        // ref on this exact model+case: grondslag = 200.00, toeslag = 200.00 (the conflict resolves to the
        // literal member — which it can only do once member A has a REAL value to lose with).
        assertNull(play(defaultsSalienceModel(), caseOf(
                inst("J1", "Jutter", "\"kindertal\": { \"number\": \"3\" }, \"basis\": { \"number\": \"100.00\" }", ""),
                valueJson("J1", "grondslag", "200.00") + ", " + valueJson("J1", "toeslag", "200.00") + ", "
                        + consistent())));
    }

    // ---------------- 3. the §9.5 companions and the exact comparison ----------------

    @Test void a_let_wrapped_division_by_empty_is_equally_inconsistent_as_the_bare_one() throws Exception {
        // §6.5 Tabel 12: a filled dividend over a LEEG divisor degrades the value to leeg AND deposits a Fout.
        // The value side lowers the Let itself (its body is ordinary arithmetic), so ONLY the companion —
        // verzamelDelingen — can go missing, and it goes missing SILENTLY: "consistent" instead of a fault.
        // ref on both models with `deler` absent: uit leeg, INCONSISTENT. Pinned bare AND wrapped, in that
        // order: an implementation that raises neither fault fails on the bare twin first.
        String expected = empty("J1", "uit") + ", " + inconsistent();
        String input = inst("J1", "Jutter", "\"inkomen\": { \"number\": \"10.00\" }", "");
        assertNull(play(divisionModel(false), caseOf(input, expected)), "de kale deling door leeg");
        assertNull(play(divisionModel(true), caseOf(input, expected)), "dezelfde deling achter een Let");
    }

    @Test void a_let_wrapped_square_root_of_negative_is_equally_inconsistent_as_the_bare_one() throws Exception {
        // §6.6: a negative radicand degrades to leeg AND deposits a Fout — the same shape as §6.5, carried by
        // verzamelWortels, and blind in the same way.
        // ref on both models with saldo = -4.00: uit leeg, INCONSISTENT.
        String expected = empty("J1", "uit") + ", " + inconsistent();
        String input = inst("J1", "Jutter", "\"saldo\": { \"number\": \"-4.00\" }", "");
        assertNull(play(squareRootModel(false), caseOf(input, expected)), "de kale wortel van negatief");
        assertNull(play(squareRootModel(true), caseOf(input, expected)), "dezelfde wortel achter een Let");
    }

    // ------- 4. the other half: a companion the walk DOES build and the path probe must keep SILENT -------

    @Test void a_division_in_the_not_taken_arm_yields_no_error() throws Exception {
        // ch. 10.2 evaluates EXACTLY the chosen branch. The DAN branch divides by `deler`, which is absent; the
        // condition is false (a = 1,00, not > 100), so that division never runs. The walk DOES descend into the
        // branch — the §9.5 companion for that division exists — and what keeps it silent is its path probe:
        // the companion re-evaluates the outermost ch. 4.10 node on the path (here the IfThenElse itself) and
        // deposits only when a §6.5 site under it really ran. A companion deciding on the static "dividend
        // present, divisor leeg" predicate alone cannot see which branch ran and would answer INCONSISTENT
        // where ref answers consistent.
        // ref on BOTH models with `deler` absent: uit = 7.0000, CONSISTENT. Pinned bare (the IfThenElse as the
        // rule value) AND inside a Let body, because those two shapes give the probe a DIFFERENT host — the
        // IfThenElse itself, and the Let above it — and only one of the two would break if the host were
        // picked wrong.
        // NB what this method pins is the OUTCOME; it would also pass under a never-fabricating walk. The probe
        // MECHANISM is pinned by the sister {@code CompanionPathWitnessTest} —
        // a_division_in_the_taken_arm_is_inconsistent_and_in_the_not_taken_is_not — which drives both flanks
        // through one and the same rule base, so only the path decides.
        String expected = valueJson("J1", "uit", "7.0000") + ", " + consistent();
        String input = inst("J1", "Jutter",
                "\"a\": { \"number\": \"1.00\" }, \"inkomen\": { \"number\": \"10.00\" }", "");
        assertNull(play(notTakenArmModel(false), caseOf(input, expected)), "de kale keuze");
        assertNull(play(notTakenArmModel(true), caseOf(input, expected)), "dezelfde keuze in een Let-body");
    }

    @Test void a_division_in_a_let_body_with_an_unread_binder_does_deliver_the_error() throws Exception {
        // ch. 10.6 under call-by-need (maintainer decision O3): "Let d = deler in inkomen gedeeld door tweede",
        // `deler` AND `tweede` absent. The binder `d` is never read, so clause 3 never evaluates `deler`, no
        // barrier is raised, and the body ALWAYS runs — its division by the absent `tweede` is real: a §6.5
        // fault. (Under the strict binding this chapter stated before, the leeg value ended the Let at the bind
        // place and the body never ran; that is what this witness pinned until the L-unification.) The bare twin
        // is the same division WITHOUT the Let — same fault — so an implementation that simply stopped
        // collecting divisions fails on both. Rewritten under maintainer decision 2026-09-01 (card
        // companion-onder-drager); the sister witness below pins the barrier that IS passed first.
        // ref (the L-ref, 2026-09-02): bare -> uit leeg + INCONSISTENT; in the Let body -> uit leeg + INCONSISTENT.
        String input = inst("J1", "Jutter", "\"inkomen\": { \"number\": \"10.00\" }", "");
        assertNull(play(emptyBarrierModel(false), caseOf(input, empty("J1", "uit") + ", " + inconsistent())),
                "de kale deling door leeg");
        assertNull(play(emptyBarrierModel(true), caseOf(input, empty("J1", "uit") + ", " + inconsistent())),
                "dezelfde deling in een Let-body waarvan de binder niet gelezen wordt");
    }

    @Test void a_division_behind_a_passed_empty_barrier_yields_no_error() throws Exception {
        // the sister: "Let d = deler in d plus (inkomen gedeeld door tweede)", `deler` and `tweede` absent. Now the
        // body READS `d` — and reads it FIRST (the left operand; ref evaluates a Binair left before right). That
        // read forces the leeg need, raises the barrier at the read (clause 2) and ends the Let before the
        // division is ever reached: no §6.5 fault, uit leeg. This is the ch. 10.6-L shape of "a barrier the
        // division sits behind" — the read place, not the bind place, is where the path is cut.
        // ref (the L-ref, 2026-09-02): uit leeg + CONSISTENT.
        String input = inst("J1", "Jutter", "\"inkomen\": { \"number\": \"10.00\" }", "");
        assertNull(play(passedBarrierModel(), caseOf(input, empty("J1", "uit") + ", " + consistent())),
                "de deling achter de lezing die de barriere opwerpt");
    }

    @Test void a_rational_behind_a_let_compares_exactly_instead_of_crashing() throws Exception {
        // "als (Let x = a gedeeld door 3 in x) groter is dan b plus 0,00 dan uit := 1,00", a = 10.00, b = 1.00.
        // 10/3 does not terminate, so the value side produces an EXACT Breuk. kanBreukOpleveren decides whether
        // the comparison routes through the Breuk-aware helper or through a raw (BigDecimal) cast; without
        // descending the ch. 4.10 node
        // it answers "no Breuk here" and the cast throws a ConstraintEvaluationException at fire — an
        // unexplained crash, which gate 12 counts as a not-carried form rather than a value.
        // ref on both models: uit = 1.00, consistent (10/3 = 3.33… > 1.00).
        String expected = valueJson("J1", "uit", "1.00") + ", " + consistent();
        String input = inst("J1", "Jutter",
                "\"a\": { \"number\": \"10.00\" }, \"b\": { \"number\": \"1.00\" }", "");
        assertNull(play(rationalComparisonModel(false), caseOf(input, expected)), "de kale breuk-vergelijking");
        assertNull(play(rationalComparisonModel(true), caseOf(input, expected)), "dezelfde vergelijking achter een Let");
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. A form
     *  the backend refuses propagates as {@link CaseRunner.Excluded} — the loud channel two witnesses assert. */
    private static String playTms(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("let-drager-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.playCase(tmp, JSON.readTree(caseJson), DroolsBackend.Branch.EXEC_MODEL);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static String play(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("let-drager-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.playCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static String caseOf(String instances, String expected) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ %s ], "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(instances, expected);
    }

    private static String inst(String label, String objectType, String attributeJson, String roles) {
        return """
                { "label": "%s", "objectType": "%s", "attributes": { %s },
                  "roles": { %s }, "characteristics": [] }""".formatted(label, objectType, attributeJson, roles);
    }

    private static String valueJson(String label, String attribute, String number) {
        return """
                { "kind": "attributeValue", "label": "%s", "attribute": "%s", "value": { "number": "%s" } }"""
                .formatted(label, attribute, number);
    }

    private static String empty(String label, String attribute) {
        return "{ \"kind\": \"attributeEmpty\", \"label\": \"%s\", \"attribute\": \"%s\" }"
                .formatted(label, attribute);
    }

    private static String consistent() { return "{ \"kind\": \"consistent\" }"; }
    private static String inconsistent() { return "{ \"kind\": \"inconsistent\" }"; }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10 and its neighbours) ----------------

    private static final String BOOLEAN = "{\"node\":\"Boolean\"}";

    private static String num(int dec, boolean whole, String sign, String unit) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"%s}"
                .formatted(whole, dec, sign, unit == null ? "" : ",\"unit\":" + unit);
    }
    private static String num(int dec) { return num(dec, false, "unspecified", null); }
    private static String pos(int dec) { return num(dec, false, "nonNegative", null); }
    private static final String WHOLE_POS = num(0, true, "nonNegative", null);

    /** One unit of the two-unit "Afstand" system below (canonical metre, factor to the canonical unit). */
    private static String unit(String abbreviation, int factor) {
        return ("{\"node\":\"Unit\",\"abbreviation\":\"%s\",\"system\":\"Afstand\",\"canonical\":\"m\","
                + "\"factorToCanonical\":{\"num\":%d,\"den\":1}}").formatted(abbreviation, factor);
    }

    private static String attr(String objectType, String naam, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"%s\",\"source\":{\"snippet\":\"het %s van de %s\"}}"
                .formatted(naam, objectType, naam, objectType);
    }

    /** A read across a role — the shape whose reactivity {@code navRefs} is responsible for. */
    private static String navAttr(String objectType, String naam, String type, String factType, String role) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"%s\","
                + "\"source\":{\"snippet\":\"het %s van zijn %s\"},\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"%s\","
                + "\"role\":\"%s\"}]}").formatted(naam, objectType, naam, role, factType, role);
    }

    private static String lit(String valueJson, String type) {
        return IrPath.literal(valueJson, type, valueJson);
    }

    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, type, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String squareRoot(String argument, String type) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":[%s],\"type\":%s,\"source\":{\"snippet\":\"de wortel van\"}}"
                .formatted(argument, type);
    }

    private static String let(String binder, String value, String body, String type) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"%s is …\"}}"
                .formatted(type, binder, value, body, binder);
    }

    /** NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String naam, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"%s\"}}".formatted(naam, naam);
    }

    private static String ite(String condition, String then, String otherwise, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(type, condition, then, otherwise);
    }

    private static String compare(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static final String ALTIJD =
            "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],\"source\":{\"snippet\":\"altijd\"}}";

    /** The §9.2 stratification lever both walk witnesses use: a NEGATED kenmerk read whose kenmerk has a
     *  producer sinks the reading rule's salience below that producer's, so the producer fires LAST. */
    private static final String NOT_VERMIST =
            "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"vermist\",\"negated\":true,"
                    + "\"source\":{\"snippet\":\"hij niet vermist is\"}}";
    private static final String VERMIST_TARGET =
            "{\"node\":\"AttributeReference\",\"attribute\":\"vermist\",\"subjectType\":\"Jutter\","
                    + "\"source\":{\"snippet\":\"Een Jutter is vermist\"}}";
    private static final String VERMIST_CHARACTERISTIC =
            "{\"node\":\"Characteristic\",\"name\":\"vermist\",\"kind\":\"adjectival\"}";
    private static final String WAAR =
            "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":" + BOOLEAN + ",\"source\":{\"snippet\":\"waar\"}}";

    private static String regel(String naam, String target, String valueJson) {
        return regel(naam, "BEREKENING", target, valueJson, "null");
    }
    private static String regel(String naam, String category, String target, String valueJson, String conditionJson) {
        return IrPath.regel(naam, category, target, valueJson, conditionJson);
    }

    private static String objectType(String naam, String plural, String attributes, String characteristics) {
        return ("{\"node\":\"ObjectType\",\"name\":\"%s\",\"animate\":true,\"plural\":\"%s\","
                + "\"attributes\":[%s],\"characteristics\":[%s]}")
                .formatted(naam, plural, attributes, characteristics);
    }

    /** The {@code attributes} list of an object type, from alternating name / datatype arguments. */
    private static String attributeJson(String... nameAndType) {
        if (nameAndType.length % 2 != 0) throw new IllegalArgumentException("a name without a datatype");
        java.util.StringJoiner list = new java.util.StringJoiner(",");
        for (int i = 0; i < nameAndType.length; i += 2)
            list.add("{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(nameAndType[i], nameAndType[i + 1]));
        return list.toString();
    }

    /** One document. {@code rules} are the resolved bodies; {@code prioritizedDefaults} is appended verbatim to
     *  the rules section (empty for the flat v0 shape). */
    private static String doc(String objectTypes, String factTypes, String unitSystems,
            String[] rules, String prioritizedDefaults) {
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[%s],\"factTypes\":[%s],\"parameters\":[],"
                + "\"domains\":[],\"unitSystems\":[%s],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]},\"rules\":{"
                + "\"rules\":[%s],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[]%s},\"diagnostics\":[]}")
                .formatted(objectTypes, factTypes, unitSystems, String.join(",", rules),
                        prioritizedDefaults);
    }

    /** A ch. 7 group over {@code Jutter.toeslag}: a justified first default with {@code consequence}, then an
     *  unconditional 0. {@code unordered} writes the group unordered, which makes a two-member set conflict-capable. */
    private static String defaultsGroup(String consequence, String zeroValue, boolean unordered) {
        return ("{\"node\":\"PrioritizedDefaults\",\"name\":\"toeslag\",\"subject\":\"Jutter\","
                + "\"attribute\":\"toeslag\"%s,\"defaults\":["
                + "{\"node\":\"Default\",\"justification\":%s,\"consequence\":%s,\"source\":{\"snippet\":\"toeslag\"}},"
                + "{\"node\":\"Default\",\"justification\":%s,\"consequence\":%s,\"source\":{\"snippet\":\"toeslag\"}}],"
                + "\"source\":{\"snippet\":\"toeslag\"},\"validity\":{\"node\":\"Validity\"}}")
                .formatted(unordered ? ",\"unordered\":true" : "",
                        compare("greaterOrEqual", attr("Jutter", "kindertal", WHOLE_POS), lit("2", WHOLE_POS)),
                        consequence, ALTIJD, zeroValue);
    }

    // ---------------- the models (each ref-replayed before its values were pinned) ----------------

    /** A ch. 10.2 branch guard whose two operands live in DIFFERENT units of one system — the restatement is
     *  what decides the branch, and the guard that refuses it is what keeps the wrong branch off the table. */
    private static String crossUnitModel() {
        String meter = num(0, true, "nonNegative", unit("m", 1));
        String kilometer = num(0, true, "nonNegative", unit("km", 1000));
        String system = "{\"node\":\"UnitSystem\",\"name\":\"Afstand\",\"units\":["
                + unit("m", 1) + "," + unit("km", 1000) + "]}";
        String valueJson = let("a", attr("Baan", "aanloop", meter),
                ite(compare("greater", bref("a", meter), lit("1", kilometer)),
                        lit("1.00", num(2)), lit("2.00", num(2)), num(2)), num(2));
        return doc(objectType("Baan", "Baans", attributeJson("aanloop", meter, "uit", num(2)), ""),
                "", system,
                new String[]{regel("uit van een baan", attr("Baan", "uit", num(2)), valueJson)},
                "");
    }

    /** A ch. 7 defaults group whose consequence hides a §6.5 division behind a Let. */
    private static String defaultsDivisionModel() {
        String consequence = let("d", attr("Jutter", "deler", pos(2)),
                binary("dividedBy", attr("Jutter", "inkomen", pos(2)), bref("d", pos(2)), num(4)), num(4));
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("kindertal", WHOLE_POS, "inkomen", pos(2), "deler", pos(2), "toeslag", num(4)), ""),
                "", "", new String[]{},
                ",\"prioritizedDefaults\":[" + defaultsGroup(consequence, lit("0.0000", num(4)), false) + "]");
    }

    /** A Let over a NAVIGATED read of an attribute whose producer is forced to fire last. */
    private static String farSideReactivityModel() {
        String far = navAttr("Jutter", "afgeleid", num(4), "strandmaatschap", "strandmaat");
        String factType = "{\"node\":\"FactType\",\"name\":\"strandmaatschap\","
                + "\"reciprocal\":true,\"roles\":[{\"node\":\"Role\",\"name\":\"strandmaat\","
                + "\"objectType\":\"Jutter\",\"plural\":\"strandmaten\",\"many\":false}]}";
        String out = let("k", far, binary("times", bref("k", num(4)), lit("2.00", num(2)), num(6)), num(6));
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("strandtegoed", pos(2), "afgeleid", num(4), "uit", num(6)),
                        VERMIST_CHARACTERISTIC),
                factType, "",
                new String[]{
                        // never true on this input — its only job is to BE a producer of `vermist`
                        regel("vermissing van een jutter", "KENMERKTOEKENNING", VERMIST_TARGET, WAAR,
                                compare("greater", attr("Jutter", "strandtegoed", pos(2)), lit("999999.00", num(2)))),
                        regel("afgeleid van een jutter", "BEREKENING", attr("Jutter", "afgeleid", num(4)),
                                binary("times", attr("Jutter", "strandtegoed", pos(2)), lit("3.00", num(2)), num(4)),
                                NOT_VERMIST),
                        regel("uit van een jutter", attr("Jutter", "uit", num(6)), out)},
                "");
    }

    /** Two unordered ch. 7 defaults members over one target; member A reads a rule-derived attribute
     *  through a Let, and its producer is forced to fire last. */
    private static String defaultsSalienceModel() {
        String letRead = let("g", attr("Jutter", "grondslag", num(2)), bref("g", num(2)), num(2));
        String groups = ",\"prioritizedDefaults\":["
                + defaultsGroup(letRead, lit("0.00", num(2)), true) + ","
                + defaultsGroup(lit("200.00", num(2)), lit("0.00", num(2)), true) + "]";
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("kindertal", WHOLE_POS, "basis", pos(2), "grondslag", num(2), "toeslag", num(2)),
                        VERMIST_CHARACTERISTIC),
                "", "",
                new String[]{
                        regel("vermissing van een jutter", "KENMERKTOEKENNING", VERMIST_TARGET, WAAR,
                                compare("greater", attr("Jutter", "basis", pos(2)), lit("999999.00", num(2)))),
                        regel("grondslag van een jutter", "BEREKENING", attr("Jutter", "grondslag", num(2)),
                                binary("times", attr("Jutter", "basis", pos(2)), lit("2.00", num(2)), num(2)),
                                NOT_VERMIST)},
                groups);
    }

    /** §6.5: "het inkomen gedeeld door de deler", bare or behind a Let. */
    private static String divisionModel(boolean behindLet) {
        String division = binary("dividedBy", attr("Jutter", "inkomen", pos(2)),
                attr("Jutter", "deler", pos(2)), num(4));
        String valueJson = behindLet ? let("t", division, bref("t", num(4)), num(4)) : division;
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("inkomen", pos(2), "deler", pos(2), "uit", num(4)), ""),
                "", "", new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), valueJson)}, "");
    }

    /** §6.6: "de wortel van het saldo", bare or behind a Let. */
    private static String squareRootModel(boolean behindLet) {
        String w = squareRoot(attr("Jutter", "saldo", num(2)), num(4));
        String valueJson = behindLet ? let("t", w, bref("t", num(4)), num(4)) : w;
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("saldo", num(2), "uit", num(4)), ""),
                "", "", new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), valueJson)}, "");
    }

    /** ch. 10.2: a §6.5 division in the branch the condition does NOT take — the IfThenElse bare as the rule
     *  value, or wrapped in a Let whose binder feeds the condition. */
    private static String notTakenArmModel(boolean inLetBody) {
        String division = binary("dividedBy", attr("Jutter", "inkomen", pos(2)),
                attr("Jutter", "deler", pos(2)), num(4));
        String left = inLetBody ? bref("a", pos(2)) : attr("Jutter", "a", pos(2));
        String choice = ite(compare("greater", left, lit("100", WHOLE_POS)),
                division, lit("7.0000", num(4)), num(4));
        String valueJson = inLetBody ? let("a", attr("Jutter", "a", pos(2)), choice, num(4)) : choice;
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("a", pos(2), "inkomen", pos(2), "deler", pos(2), "uit", num(4)), ""),
                "", "", new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), valueJson)}, "");
    }

    /** ch. 10.6: the same §6.5 division, bare or as the BODY of a Let whose leeg value is never read (so the
     *  body runs and the fault is real under call-by-need). */
    private static String emptyBarrierModel(boolean behindBarrier) {
        String division = binary("dividedBy", attr("Jutter", "inkomen", pos(2)),
                attr("Jutter", "tweede", pos(2)), num(4));
        String valueJson = behindBarrier
                ? let("d", attr("Jutter", "deler", pos(2)), division, num(4))
                : division;
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("inkomen", pos(2), "deler", pos(2), "tweede", pos(2), "uit", num(4)), ""),
                "", "", new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), valueJson)}, "");
    }

    /** ch. 10.6 clause 2: the same division, but the body reads the leeg binder FIRST — the barrier is raised at
     *  that read and the division behind it is never reached. */
    private static String passedBarrierModel() {
        String division = binary("dividedBy", attr("Jutter", "inkomen", pos(2)),
                attr("Jutter", "tweede", pos(2)), num(4));
        String valueJson = let("d", attr("Jutter", "deler", pos(2)),
                binary("plus", bref("d", pos(2)), division, num(4)), num(4));
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("inkomen", pos(2), "deler", pos(2), "tweede", pos(2), "uit", num(4)), ""),
                "", "", new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), valueJson)}, "");
    }

    /** A non-terminating division as the LEFT operand of a rule's voorwaarde, bare or behind a Let. */
    private static String rationalComparisonModel(boolean behindLet) {
        String division = binary("dividedBy", attr("Jutter", "a", pos(2)), lit("3", WHOLE_POS), num(6));
        String left = behindLet ? let("x", division, bref("x", num(6)), num(6)) : division;
        String condition = compare("greater", left,
                binary("plus", attr("Jutter", "b", pos(2)), lit("0.00", num(2)), num(6)));
        return doc(objectType("Jutter", "Jutters",
                        attributeJson("a", pos(2), "b", pos(2), "uit", num(2)), ""),
                "", "", new String[]{regel("uit van een jutter", "BEREKENING", attr("Jutter", "uit", num(2)),
                        lit("1.00", num(2)), condition)}, "");
    }
}
