package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;


/**
 * A ch. 4.10 {@code Let} standing between a NEED and the ch. 10.2 branch node that reads it, pinned in BOTH
 * transpiler branches ({@link BothBranches}).
 *
 * <p>WHY it exists: until 2026-09-19 the DRL-text branch placed such a nested {@code Let} eagerly and answered
 * a wrong VALUE for it. Two separate defects sat on one shape. The forcing gate of a deeper need tested that
 * need's own emptiness instead of the enclosing {@code Let}'s BARRIER — under a branch guard those are
 * different propositions, and on the arm that does not read the enclosing need the gate was false for a reason
 * ch. 10.6 knows nothing about, so the deeper need was never forced and its own barrier emptied the answer
 * (measured: {@code leeg} where ref, clingo and the exec branch answer 1,00; with a coalescing wrapper around
 * the branch node, 5,00 where all three answer 6,00 — the barrier shrunk to the arm). And a branch guard
 * reading a binder bound DEEPER in the body was a forward reference, refused by name
 * ({@code synth3-armwissel}); two adjacent {@code Let}s commute, so the exchange gives that guard its binder.
 *
 * <p>Both corpus goldens that carried those two reasons — {@code synth3-armverdeling} and
 * {@code synth3-armwissel} — play in the DRL branch the moment they close. But gate 12 measures the exec
 * branch alone, so the row that says the two branches agree has to be here: a row that runs in one branch
 * pins one branch.
 *
 * <p><b>Every pinned value was produced by {@code ref/} on this exact model+case first</b> (the arbiter), and
 * each is reproduced by clingo as a third reading; this backend is the fourth executor. The last two rows are
 * the SCOPE of the fix, in both directions: a need genuinely read on both arms keeps eager placement, and the
 * one shape no route reaches is refused loudly rather than placed.
 */
class NestedLetPushWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the value defect: the nested Let follows the arm that reads the need ----------------

    @Test void the_nested_let_between_need_and_arm_node_follows_the_read_arm() throws Exception {
        // "uit := Let k = inkomen in (Let z = k gedeeld door restpost in (Let w = 1,00 in
        //         (als k groter dan 10,00 dan w anders z)))" — the corpus shape `synth3-armverdeling`
        // carries, in miniature. `z` is read only in the anders-arm, `w` only in the dan-arm.
        //   inkomen 40,00, restpost ABSENT -> dan-arm: ref uit 1,00, CONSISTENT (z is never read, so the
        //                                     division never runs — ch. 10.6 clause 3).
        //   inkomen  5,00, restpost ABSENT -> anders-arm: ref uit leeg, INCONSISTENT.
        //   inkomen  5,00, restpost 2,00   -> anders-arm, and it simply computes: ref uit 2,50, CONSISTENT.
        // The first flank is the one that was red: `w`'s forcing gate carried `z != null`, which is false on
        // the dan-arm because `z` was never forced there, so `w` went unforced and its barrier emptied.
        BothBranches.green(write(pushModel()), jutter(n("inkomen", "40.00"), valueJson("1.00") + ", " + consistent()),
                "de dan-arm leest de geneste behoefte niet");
        BothBranches.green(write(pushModel()), jutter(n("inkomen", "5.00"), empty() + ", " + inconsistent()),
                "de anders-arm leest haar en meldt");
        BothBranches.green(write(pushModel()), jutter(n("inkomen", "5.00") + ", " + n("restpost", "2.00"),
                valueJson("2.50") + ", " + consistent()), "de anders-arm rekent gewoon");
    }

    // ---------------- the SCOPE pin of the barrier: the whole expression, never the arm ----------------

    @Test void the_barrier_of_the_nested_need_empties_the_whole_and_not_the_arm() throws Exception {
        // The same shape with a COALESCING wrapper inside the nested Let's body: "… Let z = k gedeeld door
        // restpost in ((Let w = 1,00 in (als k groter dan 10,00 dan w anders z)) plus 5,00)" — the corpus
        // shape `synth3-armverdeling`'s wikkelheffing rule. The `plus` is what makes the barrier's EXTENT
        // observable (§6.2 coalesces a leeg operand to 0).
        //   inkomen 40,00, restpost ABSENT -> ref uit 6,00 (1,00 + 5,00), CONSISTENT.
        //   inkomen  5,00, restpost ABSENT -> ref uit leeg, INCONSISTENT — the barrier belongs to `Let z` and
        //                                     empties its whole body, the `plus` included. NOT 5,00.
        // Both flanks are falsifiable in the same direction: a lowering that shrinks the barrier to the arm
        // answers 5,00 on each of them.
        BothBranches.green(write(wrapperModel()), jutter(n("inkomen", "40.00"), valueJson("6.00") + ", " + consistent()),
                "de niet-lezende arm telt gewoon op");
        BothBranches.green(write(wrapperModel()), jutter(n("inkomen", "5.00"), empty() + ", " + inconsistent()),
                "de barriere leegt het geheel, niet de arm");
    }

    // ---------------- the refusal that closed: the guard's binder is bound deeper ----------------

    @Test void the_arm_guard_reads_its_binder_because_the_two_lets_swap() throws Exception {
        // "uit := Let k = inkomen in (Let z = k gedeeld door restpost in (Let w = k in
        //         (als w groter dan 10,00 dan 0,00 anders z)))" — the corpus shape `synth3-armwissel`.
        // `z` is read only in the anders-arm, so its need is forced under that arm's guard — and that guard
        // reads `w`, which is bound one `Let` DEEPER. Stated at `z`'s own position it is a forward reference,
        // which is why this branch refused the document by name. The two `Let`s commute, so the exchange puts
        // `w` above `z` and the guard resolves the ordinary way.
        //   inkomen 40,00, restpost ABSENT -> dan-arm: ref uit 0,00, CONSISTENT.
        //   inkomen  5,00, restpost ABSENT -> anders-arm: ref uit leeg, INCONSISTENT.
        //   inkomen  5,00, restpost 2,00   -> anders-arm: ref uit 2,50, CONSISTENT.
        // The middle flank also pins the COMPANION side of the exchange: after the swap `w`'s need IS a
        // binder, so the companion has to decompose a need through the environment it is itself building.
        BothBranches.green(write(switchModel()), jutter(n("inkomen", "40.00"), valueJson("0.00") + ", " + consistent()),
                "de dan-arm leest de geneste behoefte niet");
        BothBranches.green(write(switchModel()), jutter(n("inkomen", "5.00"), empty() + ", " + inconsistent()),
                "de anders-arm leest haar en meldt");
        BothBranches.green(write(switchModel()), jutter(n("inkomen", "5.00") + ", " + n("restpost", "2.00"),
                valueJson("2.50") + ", " + consistent()), "de anders-arm rekent gewoon");
    }

    // ---------------- the lens finding: a need that is ITSELF a binder ----------------

    @Test void a_need_that_is_itself_a_binder_reports_on_its_own_site() throws Exception {
        // "uit := Let k = inkomen in (Let m = k in (m gedeeld door restpost))" — no branch node at all, and
        // the §6.5 site sits behind TWO needs, the inner of which is a read of the outer. The companion
        // decomposed its needs in one loop and its binder environment in another, so that inner need met an
        // empty environment and the companion was dropped: ref, clingo and the exec branch reported, this
        // branch answered a silent CONSISTENT.
        //   inkomen 10,00, restpost ABSENT -> ref uit leeg, INCONSISTENT.
        //   inkomen 10,00, restpost  2,00  -> ref uit 5,00, CONSISTENT (the fabricage flank: nothing faults,
        //                                     so nothing reports).
        BothBranches.green(write(chainModel()), jutter(n("inkomen", "10.00"), empty() + ", " + inconsistent()),
                "de behoefte-als-binder meldt");
        BothBranches.green(write(chainModel()), jutter(n("inkomen", "10.00") + ", " + n("restpost", "2.00"),
                valueJson("5.00") + ", " + consistent()), "de behoefte-als-binder verzint niets");
    }

    // ---------------- the SCOPE, first direction: a need both arms read stays eager ----------------

    @Test void a_need_that_both_arms_read_stays_eager_and_that_is_exact() throws Exception {
        // "… Let w = 1,00 in (als k groter dan 10,00 dan (w plus z) anders z)" — `z` is read on BOTH arms, so
        // its need is wanted whichever arm runs and there is no arm to confine it to. The guard that decides
        // this is a MEASUREMENT of where the binder is read (through nested values as well), not a syntactic
        // look at the shape, and here it measures "always".
        //   inkomen 40,00, restpost ABSENT -> dan-arm READS z too: ref uit leeg, INCONSISTENT.
        //   inkomen  5,00, restpost ABSENT -> anders-arm: ref uit leeg, INCONSISTENT.
        //   inkomen 40,00, restpost 2,00   -> dan-arm computes: ref uit 21,00 (1,00 + 20,00), CONSISTENT.
        // A lowering that confined this need to one arm leaves the other arm reading an unbound binder.
        BothBranches.green(write(bothArmsModel()), jutter(n("inkomen", "40.00"), empty() + ", " + inconsistent()),
                "de dan-arm leest de behoefte ook");
        BothBranches.green(write(bothArmsModel()), jutter(n("inkomen", "5.00"), empty() + ", " + inconsistent()),
                "de anders-arm leest haar");
        BothBranches.green(write(bothArmsModel()), jutter(n("inkomen", "40.00") + ", " + n("restpost", "2.00"),
                valueJson("21.00") + ", " + consistent()), "beide lezingen rekenen");
    }

    // ---------------- the SCOPE, second direction: the shape no route reaches ----------------

    @Test void a_wrapper_between_the_two_lets_plays_in_both_branches() throws Exception {
        // "… Let z = k gedeeld door restpost in ((Let w = k in (als w groter dan 10,00 dan 0,00 anders z))
        //  plus 5,00)" — the exchange cannot step OVER the arithmetic wrapper standing between the two
        // `Let`s, and there is no branch node at `z`'s own position to distribute into. ref answers 5,00 here
        // (inkomen 40,00, restpost ABSENT) and the analysis reproduces it, on both branches since card
        // drl-teksttak-op-truth-maintenance; until then the destructive DRL-text lowering refused it BY NAME ("takgarde
        // van een voorwaardelijk gelezen behoefte (ch. 10.2)"), and clingo raises its own loud TRANSLATION FAULT on the
        // same document.
        BothBranches.green(write(wrapperBetweenModel()), jutter(n("inkomen", "40.00"), valueJson("5.00") + ", "
                + consistent()), "de wikkel tussen de twee Lets speelt");
    }

    @Test void an_arm_guard_whose_binder_is_rebound_in_between_plays_in_both_branches() throws Exception {
        // "uit := Let w = saldo in ((Let z = w gedeeld door restpost in (Let w = inkomen in
        //         (als w groter dan 10,00 dan 0,00 anders z))) plus w)" — `z`'s need is read in the anders-arm
        // alone, so its guard is "NOT (w > 10,00)"; but the `w` that guard means is the INNER one, and at `z`'s
        // own position that name still belongs to the OUTER binding. The exchange cannot help: moving `z` past
        // `Let w = inkomen` would make its own value read the inner `w` instead of the outer one.
        //
        // Measured 2026-09-19 and PRE-EXISTING (identical on the branch state before this card): the guard was
        // resolved against the outer binding, and with saldo 5,00, inkomen 40,00 and restpost ABSENT that made
        // this branch FABRICATE an Inconsistency — ref answers uit 5,00 CONSISTENT (the dan-arm runs, `z` is
        // never read, the division never happens), the exec branch reproduces it, and clingo refuses the
        // document with its own loud fault. An invented fault is the worst answer available, so the destructive
        // DRL-text lowering refused the shape by name, and a sibling spelling whose two bindings happened to AGREE as
        // well; since card drl-teksttak-op-truth-maintenance both branches render the analysis, which reads each `w`
        // where it is bound, and play both flanks as ref answers.
        Path ir = write(reboundModel());
        // saldo 5,00 / inkomen 40,00 / restpost ABSENT — the fabricage flank: ref uit 5,00, CONSISTENT.
        // saldo 5,00 / inkomen  8,00 / restpost 2,00  — the VALUE flank: ref uit 7,50 (5,00/2,00 + 5,00), and
        //   it is 9,00 if the two `w`s are confused (8,00/2,00 + 5,00), so the two bindings are told apart by
        //   a number here and not only by a report.
        for (JsonNode c : new JsonNode[] {
                jutter(n("saldo", "5.00") + ", " + n("inkomen", "40.00"), valueJson("5.00") + ", " + consistent()),
                jutter(n("saldo", "5.00") + ", " + n("inkomen", "8.00") + ", " + n("restpost", "2.00"),
                        valueJson("7.50") + ", " + consistent()) })
            BothBranches.green(ir, c, "de herbonden takgarde speelt");
    }

    // ---------------- helpers: replay ----------------

    /** One authored model on disk — {@link BothBranches} replays a PATH through both branches. */
    private static Path write(String ir) throws Exception {
        Path tmp = Files.createTempFile("geneste-let-duw-witness", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return tmp;
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static String n(String attribute, String number) {
        return "\"%s\": { \"number\": \"%s\" }".formatted(attribute, number);
    }

    private static JsonNode jutter(String attributes, String expected) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(attributes, expected));
    }

    private static String valueJson(String number) {
        return "{ \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"uit\", \"value\": { \"number\": \"%s\" } }"
                .formatted(number);
    }
    private static String empty() { return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"uit\" }"; }
    private static String consistent() { return "{ \"kind\": \"consistent\" }"; }
    private static String inconsistent() { return "{ \"kind\": \"inconsistent\" }"; }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10 and its neighbours) ----------------

    private static String num(int dec, String sign) {
        return "{\"node\":\"Number\",\"whole\":false,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(dec, sign);
    }
    private static String num() { return num(2, "unspecified"); }
    private static String pos() { return num(2, "nonNegative"); }

    private static String attr(String name) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"zijn %s\"}}")
                .formatted(name, name);
    }

    private static String lit(String valueJson) {
        return IrPath.literal(valueJson, num(), valueJson);
    }

    private static String binary(String op, String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, num(), op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String let(String binder, String value, String body) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"%s is …\"}}"
                .formatted(num(), binder, value, body, binder);
    }

    /** NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String name, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"%s\"}}".formatted(name, name);
    }

    private static String ite(String condition, String then, String otherwise) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(num(), condition, then, otherwise);
    }

    /** "&lt;binder&gt; groter dan 10,00" — the ch. 10.2 guard every branch shape below shares. */
    private static String above(String binder) {
        return ("{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":%s,\"right\":%s,"
                + "\"source\":{\"snippet\":\"boven de grens\"}}").formatted(bref(binder, pos()), lit("10.00"));
    }

    private static final String NAME = "uit van een jutter";

    /** A one-objecttype document over {@code Jutter} with "uit van een jutter" as its only rule. */
    private static String model(String valueJson) {
        return ("{\"format\":\"uchar-ir-v1\","
                + "\"model\":{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,"
                + "\"plural\":\"Jutters\",\"attributes\":["
                + "{\"node\":\"Attribute\",\"name\":\"inkomen\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"restpost\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"saldo\",\"datatype\":%s},"
                + "{\"node\":\"Attribute\",\"name\":\"uit\",\"datatype\":%s}],"
                + "\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[{\"node\":\"Rule\",\"name\":\"%s\",\"versions\":[{"
                + "\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":%s,\"value\":%s,\"initialisation\":false,\"presentation\":\"calculation\"}]}],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(pos(), pos(), pos(), num(), NAME, target(), valueJson);
    }

    private static String target() {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"uit\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"De uit van een Jutter\"}}");
    }

    // ---------------- the models (each ref-replayed, and clingo-replayed, before its values were pinned) ----

    /** "k gedeeld door zijn restpost" — the §6.5 site every branch shape below puts in the anders-arm. */
    private static String division() {
        return binary("dividedBy", bref("k", pos()), attr("restpost"));
    }

    /** {@code Let k = inkomen in (Let z = k/restpost in <body>)} — the two outer needs all four branch
     *  shapes share; only what stands in `z`'s body differs. */
    private static String twoNeeds(String body) {
        return let("k", attr("inkomen"), let("z", division(), body));
    }

    /** A THIRD `Let` between the need and the branch node, read in the dan-arm alone. */
    private static String pushModel() {
        return model(twoNeeds(let("w", lit("1.00"), ite(above("k"), bref("w", num()), bref("z", num())))));
    }

    /** The same, with a coalescing wrapper around the branch node INSIDE `z`'s body. */
    private static String wrapperModel() {
        return model(twoNeeds(binary("plus",
                let("w", lit("1.00"), ite(above("k"), bref("w", num()), bref("z", num()))), lit("5.00"))));
    }

    /** The third `Let`'s own binder is what the branch CONDITION reads — the exchange's shape. */
    private static String switchModel() {
        return model(twoNeeds(let("w", bref("k", pos()), ite(above("w"), lit("0.00"), bref("z", num())))));
    }

    /** The same, with an arithmetic wrapper standing BETWEEN the two `Let`s — the shape no route reaches. */
    private static String wrapperBetweenModel() {
        return model(twoNeeds(binary("plus",
                let("w", bref("k", pos()), ite(above("w"), lit("0.00"), bref("z", num()))), lit("5.00"))));
    }

    /** `z` read on BOTH arms — the need is wanted whichever arm runs. */
    private static String bothArmsModel() {
        return model(twoNeeds(let("w", lit("1.00"), ite(above("k"),
                binary("plus", bref("w", num()), bref("z", num())), bref("z", num())))));
    }

    /** The branch guard's own binder REBOUND between the `Let` it gates and the branch node — the one name
     *  that means two different bindings at the two positions. */
    private static String reboundModel() {
        return model(let("w", attr("saldo"),
                binary("plus",
                        let("z", binary("dividedBy", bref("w", pos()), attr("restpost")),
                                let("w", attr("inkomen"),
                                        ite(above("w"), lit("0.00"), bref("z", num())))),
                        bref("w", pos()))));
    }

    /** "Let k = inkomen in (Let m = k in (m gedeeld door restpost))" — a need that is itself a binder. */
    private static String chainModel() {
        return model(let("k", attr("inkomen"),
                let("m", bref("k", pos()), binary("dividedBy", bref("m", pos()), attr("restpost")))));
    }
}
