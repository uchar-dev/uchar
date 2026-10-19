package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.10 / RUNTIME-SEMANTICS ch. 10.6 + 10.2 — the <b>Let cluster</b> ({@code Let},
 * {@code BinderRef}, {@code IfThenElse}) as the Drools backend now lowers it, witnessed through
 * {@link CaseRunner#playCase} over models authored HERE (no corpus file is read, moved or touched).
 *
 * <p><b>Every pinned value below was produced by {@code ref/} on this exact model+case first</b> — ref is
 * the arbiter, this backend the third executor. The corpus goldens that carry the cluster
 * ({@code jutterswet-tariefstelling}, {@code synth3-drempelheffing}, {@code jutterswet-kinderweging},
 * {@code jutterswet-tariefrecht-catala}) pin the composed forms; what they cannot isolate is pinned here,
 * each as a separately falsifiable case:
 *
 * <ul>
 *   <li>the binding itself, and the same binder READ TWICE (a second read that answered leeg would
 *       coalesce to 0 inside {@code plus} and go unnoticed on a single-read model);</li>
 *   <li>the <b>leeg-barrier</b> beside its contrast in ONE model: a leeg {@code value} makes the whole
 *       {@code Let} leeg (and, on a standing scalar target, writes nothing), while the SAME product
 *       WITHOUT the Let coalesces the leeg operand to 0 and writes 0. A "Let is just a substitution"
 *       implementation passes every other check and fails exactly here;</li>
 *   <li>nested Lets: the inner binder SHADOWS the outer inside its body (WF-064 nearest-enclosing), and
 *       the outer binding is intact again outside it;</li>
 *   <li>both branches of an {@code IfThenElse} in a Let body, its condition reading only binders
 *       (WF-077) — a one-sided implementation fails one of the two;</li>
 *   <li>a {@code Let} inside a ch. 7 prioritizedDefaults CONSEQUENCE — the defaults lowering compiles its
 *       values through the same path, so the deciding default and the leeg fall-through are both pinned;</li>
 *   <li>the reads a ch. 4.10 node hides from the HAND-WRITTEN walks: a Let-wrapped read must still put
 *       its attribute in the freshness watch, or the rule computes once against a leeg producer and never
 *       recomputes. The producer's turn is forced by stratification, so the witness is deterministic.</li>
 * </ul>
 */
class LetClusterWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- 1. the binding, and one binder read twice ----------------

    @Test void a_let_binds_its_value_and_the_body_computes_with_it() throws Exception {
        // ref on this model+case: basis = 1100.00 (Let ink = inkomen in ink plus 100,00, inkomen 1000.00).
        assertNull(play(computingModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }, \"kindertal\": { \"number\": \"7\" }",
                valueJson("basis", "1100.00"))));
    }

    @Test void the_same_binder_read_two_times_yields_two_times_the_value() throws Exception {
        // ref: dubbelop = 2000.00 (Let ink = inkomen in ink plus ink). A second read answering leeg would
        // coalesce to 0 inside plus and quietly write 1000.00 — this pin is what catches it.
        assertNull(play(computingModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }, \"kindertal\": { \"number\": \"7\" }",
                valueJson("dubbelop", "2000.00"))));
    }

    // ---------------- 2. nested Lets: shadowing in, restoration out ----------------

    @Test void an_innermost_binder_shadows_and_the_outermost_applies_there_again_outside() throws Exception {
        // geschaduwd := Let x = inkomen in (Let y = (Let x = 5,00 in x) in y plus x).
        // ref: 1005.00 — the INNER x wins inside its own body (5,00), the OUTER x is intact again outside it
        // (1000,00). Reading the outer binder inside would give 2000.00; binding the inner x IN PLACE of the
        // outer — the in-place-mutation bug — leaves x = 5,00 for the trailing read and gives 10.00.
        assertNull(play(computingModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }, \"kindertal\": { \"number\": \"7\" }",
                valueJson("geschaduwd", "1005.00"))));
    }

    // ---------------- 3. IfThenElse in a Let body — both branches ----------------

    @Test void ifthenelse_in_a_letbody_chooses_the_then_arm() throws Exception {
        // tak := Let a = inkomen in Let b = kindertal maal 100,00 in (als b groter dan a dan 1,00 anders 2,00);
        // both condition operands are BINDERS (WF-077). inkomen 100.00, kindertal 7 -> b=700.00 > a=100.00.
        // ref: tak = 1.00.
        assertNull(play(computingModel(), jutter("\"inkomen\": { \"number\": \"100.00\" }, \"kindertal\": { \"number\": \"7\" }",
                valueJson("tak", "1.00"))));
    }

    @Test void ifthenelse_in_a_letbody_chooses_the_else_arm() throws Exception {
        // the other side of the same guard: inkomen 1000.00 -> b=700.00 is NOT > a=1000.00. ref: tak = 2.00.
        assertNull(play(computingModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }, \"kindertal\": { \"number\": \"7\" }",
                valueJson("tak", "2.00"))));
    }

    // ---------------- 4. the leeg-barrier, beside its contrast ----------------

    @Test void an_empty_let_value_makes_the_whole_let_empty_while_the_same_times_without_let_computes_zero() throws Exception {
        // ch. 10.6's normative contrast, in ONE model over ONE case with `inkomen` ABSENT:
        //   metlet   := Let ink = inkomen in ink maal 0,21  -> the body DOES run (ch. 10.6 is call-by-need), but
        //               its read of `ink` forces the leeg need and raises the barrier AT THAT READ (clause 2):
        //               the whole Let is leeg, so on this standing scalar target nothing is written (ref: leeg).
        //   zonderlet := inkomen maal 0,21                  -> the bare maal COALESCES leeg to 0 (ref: 0.0000).
        // A Let lowered as plain substitution would write 0.0000 on BOTH.
        assertNull(play(barrierModel(), jutter("",
                "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"metlet\" }, "
                        + valueJson("zonderlet", "0.0000"))));
    }

    @Test void the_barrier_is_not_always_empty_the_filled_input_does_compute() throws Exception {
        // the other side: with `inkomen` present BOTH compute, and to the same value — so the assertion above
        // is not passing on an arm that never writes a Let result at all. ref: 210.0000 each.
        assertNull(play(barrierModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }",
                valueJson("metlet", "210.0000") + ", " + valueJson("zonderlet", "210.0000"))));
    }

    @Test void the_empty_target_is_observable_and_absent_not_only_unasserted() throws Exception {
        // the comparator must SEE the absence: the same case with a value expectation on `metlet` must MISMATCH.
        String fault = play(barrierModel(), jutter("", valueJson("metlet", "0.0000")));
        assertNotNull(fault, "de leeg-barriere moet observeerbaar zijn, niet slechts niet-geasserteerd");
        assertTrue(fault.contains("metlet"), "de mismatch noemt het doel: " + fault);
    }

    // ---------------- 5. a Let inside a prioritizedDefaults consequence ----------------

    @Test void a_let_within_a_defaults_value_decides() throws Exception {
        // the ch. 7 group's first default is justified (kindertal >= 2) and its CONSEQUENCE is a Let over an
        // attribute: Let g = grondslag in g plus 100,00. ref: toeslag = 300.00 at grondslag 200.00.
        assertNull(play(defaultsModel(), jutter("\"kindertal\": { \"number\": \"3\" }, \"grondslag\": { \"number\": \"200.00\" }",
                valueJson("toeslag", "300.00"))));
    }

    @Test void an_unjustified_default_lets_the_let_value_untouched() throws Exception {
        // justification false (kindertal 1 < 2): the group falls through to the unconditional 0,00, so the Let
        // value is never the decision. ref: toeslag = 0.00.
        assertNull(play(defaultsModel(), jutter("\"kindertal\": { \"number\": \"1\" }, \"grondslag\": { \"number\": \"200.00\" }",
                valueJson("toeslag", "0.00"))));
    }

    @Test void an_empty_let_value_in_a_default_falls_through_to_the_next() throws Exception {
        // justified, but `grondslag` is ABSENT so the Let is leeg (the barrier again) — ch. 7's leeg
        // fall-through hands the decision to the next default. ref: toeslag = 0.00.
        assertNull(play(defaultsModel(), jutter("\"kindertal\": { \"number\": \"3\" }",
                valueJson("toeslag", "0.00"))));
    }

    // ---------------- 6. the reads a ch. 4.10 node hides: the freshness watch ----------------

    @Test void a_let_wrapped_read_keeps_the_rule_reactive_on_its_producer() throws Exception {
        // The freshness watch (the DRL @watch mirror) is a HAND-WRITTEN walk over concrete v0 node shapes: a
        // Let matches none of them, so without taking letKinderen it sees NO reads for
        // `gewogen := Let x = inkomen in x maal factor` — and a pattern without
        // constraints has an EMPTY reactivity mask: the rule computes ONCE, here against a leeg `factor`
        // (maal coalesces it to 0), and never recomputes when its producer lands.
        //
        // The order is FORCED, not left to agenda luck: `factor`'s rule reads a NEGATED kenmerk whose producer
        // exists, so §9.2 stratification gives it salience -1 and it fires AFTER this rule. ref on this exact
        // model+case: factor 3.0000, gewogen 3000.000000 (the fixpoint). Without the walk descending the Let
        // the replay writes 0.00 — verified by mutation.
        assertNull(play(reactivityModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }",
                valueJson("factor", "3.0000") + ", " + valueJson("gewogen", "3000.000000"))));
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. */
    private static String play(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("let-cluster-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.playCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static String jutter(String attributes, String expected) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(attributes, expected);
    }

    private static String valueJson(String attribute, String number) {
        return """
                { "kind": "attributeValue", "label": "J1", "attribute": "%s", "value": { "number": "%s" } }"""
                .formatted(attribute, number);
    }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10) ----------------

    private static String num(int dec, boolean whole, String sign) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(whole, dec, sign);
    }
    private static String num(int dec) { return num(dec, false, "unspecified"); }

    private static String attr(String name, int dec, boolean whole, String sign) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"het %s van de Jutter\"}}")
                .formatted(name, name);
    }

    private static String lit(String valueJson, int dec, boolean whole) {
        return IrPath.literal(valueJson, num(dec, whole, "unspecified"), valueJson);
    }
    private static String lit(String valueJson, int dec) { return lit(valueJson, dec, false); }

    private static String binary(String op, String left, String right, int dec) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, num(dec), op.toLowerCase());
    }

    /** ch. 4.10 the value-level binding. Its {@code type} is the BODY's type (WF-076). */
    private static String let(String binder, String value, String body, int dec) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"%s is …\"}}"
                .formatted(num(dec), binder, value, body, binder);
    }

    /** ch. 4.10 a read of the nearest enclosing binder. NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String name, int dec) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"%s\"}}".formatted(name, name);
    }

    private static String ite(String condition, String then, String otherwise, int dec) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(num(dec), condition, then, otherwise);
    }

    private static String compare(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, op.toLowerCase());
    }

    private static String rule(String name, String target, String valueJson) {
        return rule(name, "BEREKENING", target, valueJson, "null");
    }
    private static String rule(String name, String category, String target, String valueJson, String conditionJson) {
        return IrPath.rule(name, category, target, valueJson, conditionJson);
    }

    /** A one-objecttype document: the declared attributes and kenmerken, the rules, and an optional defaults
     *  section. {@code attributes}/{@code characteristics} are the bodies of the Jutter's two member lists (the
     *  kenmerken body is empty for a kenmerk-free model). */
    private static String model(String attributes, String characteristics, String[] rules, String prioritizedDefaults) {
        return ("{\"format\":\"uchar-ir-v1\","
                + "\"model\":{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,"
                + "\"plural\":\"Jutters\",\"attributes\":[%s],\"characteristics\":[%s]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]%s},\"diagnostics\":[]}")
                .formatted(attributes, characteristics, String.join(",", rules), prioritizedDefaults);
    }

    /** One entry of the Jutter's attribute list. */
    private static String attribute(String name, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(name, datatype);
    }

    // ---------------- the three models (each verifier-clean and ref-replayed before pinning) ----------------

    /** Binding, reuse, shadowing and the branch guard on one Jutter. */
    private static String computingModel() {
        String inkomen = attr("inkomen", 2, false, "nonNegative");
        String kindertal = attr("kindertal", 0, true, "nonNegative");
        String base = let("ink", inkomen, binary("plus", bref("ink", 2), lit("100.00", 2), 2), 2);
        String redundant = let("ink", inkomen, binary("plus", bref("ink", 2), bref("ink", 2), 2), 2);
        String shadowed = let("x", inkomen,
                let("y", let("x", lit("5.00", 2), bref("x", 2), 2),
                        binary("plus", bref("y", 2), bref("x", 2), 2), 2), 2);
        String branch = let("a", inkomen,
                let("b", binary("times", kindertal, lit("100.00", 2), 2),
                        ite(compare("greater", bref("b", 2), bref("a", 2)), lit("1.00", 2), lit("2.00", 2), 2), 2), 2);
        return model(String.join(",", attribute("inkomen", num(2, false, "nonNegative")),
                        attribute("kindertal", num(0, true, "nonNegative")), attribute("basis", num(2)),
                        attribute("dubbelop", num(2)), attribute("geschaduwd", num(2)), attribute("tak", num(2))), "",
                new String[]{rule("basis van een jutter", attr("basis", 2, false, "unspecified"), base),
                        rule("dubbelop van een jutter", attr("dubbelop", 2, false, "unspecified"), redundant),
                        rule("geschaduwd van een jutter", attr("geschaduwd", 2, false, "unspecified"), shadowed),
                        rule("tak van een jutter", attr("tak", 2, false, "unspecified"), branch)},
                "");
    }

    /** ch. 10.6's contrast: the same product with and without the Let binding, on one subject. */
    private static String barrierModel() {
        String inkomen = attr("inkomen", 2, false, "nonNegative");
        return model(String.join(",", attribute("inkomen", num(2, false, "nonNegative")),
                        attribute("metlet", num(4)), attribute("zonderlet", num(4))), "",
                new String[]{
                        rule("met let van een jutter", attr("metlet", 4, false, "unspecified"),
                                let("ink", inkomen, binary("times", bref("ink", 2), lit("0.21", 2), 4), 4)),
                        rule("zonder let van een jutter", attr("zonderlet", 4, false, "unspecified"),
                                binary("times", inkomen, lit("0.21", 2), 4))},
                "");
    }

    /** A ch. 7 prioritizedDefaults group whose first default's CONSEQUENCE is a Let over an attribute. */
    private static String defaultsModel() {
        String consequence = let("g", attr("grondslag", 2, false, "nonNegative"),
                binary("plus", bref("g", 2), lit("100.00", 2), 2), 2);
        String group = (",\"prioritizedDefaults\":[{\"node\":\"PrioritizedDefaults\",\"name\":\"toeslag\","
                + "\"subject\":\"Jutter\",\"attribute\":\"toeslag\",\"defaults\":["
                + "{\"node\":\"Default\",\"justification\":%s,\"consequence\":%s,\"source\":{\"snippet\":\"toeslag\"}},"
                + "{\"node\":\"Default\",\"justification\":"
                + "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],\"source\":{\"snippet\":\"altijd\"}},"
                + "\"consequence\":%s,\"source\":{\"snippet\":\"toeslag\"}}],"
                + "\"source\":{\"snippet\":\"toeslag\"},\"validity\":{\"node\":\"Validity\"}}]")
                .formatted(compare("greaterOrEqual", attr("kindertal", 0, true, "nonNegative"), lit("2", 0, true)),
                        consequence, lit("0.00", 2));
        return model(String.join(",", attribute("kindertal", num(0, true, "nonNegative")),
                        attribute("grondslag", num(2, false, "nonNegative")), attribute("toeslag", num(2))), "",
                new String[]{}, group);
    }

    /** A Let-wrapped read of an attribute whose producer is FORCED to fire later (§9.2 stratification: its
     *  rule reads a negated kenmerk that has a producer, so its salience is -1). Only a freshness watch that
     *  descends the Let keeps the consumer reactive on that producer's write. */
    private static String reactivityModel() {
        String inkomen = attr("inkomen", 2, false, "nonNegative");
        String vermistTarget = ("{\"node\":\"AttributeReference\",\"attribute\":\"vermist\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"Een Jutter is vermist\"}}");
        String trueLiteral = "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}";
        String notVermist = ("{\"node\":\"CharacteristicCheck\",\"characteristic\":\"vermist\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet vermist is\"}}");
        String weighted = let("x", inkomen,
                binary("times", bref("x", 2), attr("factor", 4, false, "unspecified"), 6), 6);
        return model(String.join(",", attribute("inkomen", num(2, false, "nonNegative")),
                        attribute("factor", num(4)), attribute("gewogen", num(6))),
                "{\"node\":\"Characteristic\",\"name\":\"vermist\",\"kind\":\"adjectival\"}",
                new String[]{
                        // never true for the case input — its only job is to BE a producer of `vermist`, which
                        // is what sinks the factor rule's stratum below this consumer's.
                        rule("vermissing van een jutter", "KENMERKTOEKENNING", vermistTarget, trueLiteral,
                                compare("greater", inkomen, lit("999999.00", 2))),
                        rule("factor van een jutter", "BEREKENING", attr("factor", 4, false, "unspecified"),
                                lit("3.0000", 4), notVermist),
                        rule("gewogen van een jutter", attr("gewogen", 6, false, "unspecified"), weighted)},
                "");
    }
}
