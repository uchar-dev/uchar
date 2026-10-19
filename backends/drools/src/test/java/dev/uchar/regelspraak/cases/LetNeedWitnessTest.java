package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * RUNTIME-SEMANTICS ch. 10.6 as UNIFIED to <b>call-by-need</b> (maintainer decision O3, 2026-09-01) — the
 * three clauses that separate L from the strict binding this backend carried before, witnessed through
 * {@link CaseRunner#playCase} over models authored HERE (no corpus file is read, moved or touched).
 *
 * <p>The two sibling files pin what the cluster computes ({@code LetClusterWitnessTest}) and what a ch. 4.10
 * node hides from the hand-written walks ({@code LetCarrierWalkWitnessTest}); both keep their pins under L,
 * because every binder they bind is read on every path. This file is the other half: the models on which
 * strict and call-by-need DISAGREE, so a backend that quietly kept the strict binding fails here and nowhere
 * else. <b>Every pinned value was produced by {@code ref/} on this exact model+case first</b> — ref carries the
 * decision, this backend is the third executor — and each was re-checked by mutation (the laziness reverted to
 * a strict bind in a scratch copy makes exactly these witnesses red).
 *
 * <ul>
 *   <li><b>clause 3, an unread binder evaluates nothing.</b> A value that would deposit (division by numeric
 *       zero, a §9.5 fact of kind EF-005 since the card zero-division-channel; until then a fault) under a binder
 *       the body never reads: the Let's outcome is the body's, no deposit — where the strict binding evaluated the
 *       value first. Its twin reads the binder, and there the deposit stands and the barrier empties the Let: a
 *       "never evaluate anything" implementation passes the first and fails the second;</li>
 *   <li><b>clause 2, the barrier sits at the READ.</b> The probe-B1 shape: a leeg value whose binder is read
 *       only in the branch ch. 10.2 does NOT take, so nothing reads it and the target is written with the
 *       body's outcome — where the strict binding emptied the whole Let at the bind place and wrote nothing;</li>
 *   <li><b>clause 1, one need, however many reads.</b> The same binder read twice against the same binder read
 *       once: same value, same §9.5 verdict.</li>
 * </ul>
 */
class LetNeedWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- clause 3: an unread binder evaluates nothing ----------------

    @Test void an_unread_binder_with_a_faulting_value_lets_the_body_decide() throws Exception {
        // "uitkomst := Let q = 100,00 gedeeld door 0,00 in 42,00" — the binder is NEVER read, so ch. 10.6
        // clause 3 never evaluates the value: no fault, no leeg, no §9.5 reason, and the Let's outcome IS the
        // body's. ref on this exact model+case: uitkomst = 42.00, CONSISTENT. Under the strict binding the
        // value ran first and the run died on the division.
        assertNull(play(zeroDivisionModel(false), jutter("", valueJson("uitkomst", "42.00"))));
    }

    @Test void the_same_value_under_a_read_binder_deposits_and_empties_the_let() throws Exception {
        // the twin that keeps the first honest: the SAME value, now read. The read forces the need, so the
        // division by numeric zero happens: it deposits one §9.5 reason of kind EF-005 and its need is leeg, so the
        // read ends the Let (clause 2) and nothing is written. ref (RUNTIME-SEMANTICS ch. 10.6 row ii): leeg, one
        // reason. Until the card zero-division-channel the division failed fast here, in ref and in this backend.
        assertNull(play(zeroDivisionModel(true), jutter("", "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\","
                + " \"attribute\": \"uitkomst\" }, " + TmsDocuments.inconsistent("uitkomst van een jutter", "J1", "EF-005"))));
    }

    // ---------------- clause 2: the leeg-barrier fires at the READ, not at the bind ----------------

    @Test void an_empty_value_read_in_the_not_chosen_arm_writes_the_target_nevertheless() throws Exception {
        // the probe-B1 shape, the one that closed the false-green window on the clingo side:
        //   "uitkomst := Let k = kindertal in Let x = leegwaarde in (als k groter dan 1 dan 7,00 anders x maal 2,00)"
        // `leegwaarde` is ABSENT. The condition reads `k` (so k's need IS forced, on every path — WF-077), and
        // `x` is read ONLY in the anders-branch, which kindertal = 2 does not take. Nothing reads x, so nothing
        // evaluates its leeg value and no barrier fires. ref on this exact model+case: uitkomst = 7.00,
        // CONSISTENT. Under the strict binding the leeg value emptied the inner Let at its BIND place and the
        // target was never written.
        assertNull(play(probeModel(), jutter("\"kindertal\": { \"number\": \"2\" }", valueJson("uitkomst", "7.00"))));
    }

    @Test void the_same_empty_value_read_on_the_indeed_chosen_arm_makes_the_let_empty() throws Exception {
        // the other flank of the same model: kindertal = 0 takes the anders-branch, which DOES read `x`, so the
        // barrier fires at that read and empties the Let that bound x — and with it the whole value, so the
        // standing scalar target is not written. ref: uitkomst leeg, CONSISTENT. Without this flank the witness
        // above would also pass on an implementation that simply ignored the barrier.
        assertNull(play(probeModel(), jutter("\"kindertal\": { \"number\": \"0\" }",
                "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"uitkomst\" }")));
    }

    // ---------------- clause 1: one need, however many reads ----------------

    @Test void reading_twice_gives_the_same_outcome_as_reading_once() throws Exception {
        // "tweemaal := Let q = grondslag gedeeld door deler in q plus q" beside
        // "eenmaal  := Let q = grondslag gedeeld door deler in q", `deler` ABSENT.
        // §6.5 Tabel 12 makes the need leeg AND deposits a Fout; the read then raises the barrier, so BOTH
        // targets are leeg and both rules are inconsistent — the second read neither recomputes into a
        // different answer nor coalesces to 0 inside `plus`. ref on this exact model+case: tweemaal leeg,
        // eenmaal leeg, INCONSISTENT for both rules.
        //
        // NB the once-ness itself has no observable artefact in THIS backend: its §9.5 companion is a separate
        // STATIC rule keyed on the operands' leegte (see delingCompanions), so it fires once per division site
        // whatever the read count. What is pinnable here is the outcome identity across one read and two —
        // ref's own clause-1 witness (a single channel reason) carries the multiplicity.
        assertNull(play(needModel(), jutter("\"grondslag\": { \"number\": \"100.00\" }",
                "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"tweemaal\" }, "
                        + "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"eenmaal\" }, "
                        + "{ \"kind\": \"inconsistent\", \"rule\": \"tweemaal van een jutter\" }, "
                        + "{ \"kind\": \"inconsistent\", \"rule\": \"eenmaal van een jutter\" }")));
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. */
    private static String play(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("let-behoefte-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.playCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** One Jutter, the given input attributes, the given expectations. The consistency verdict is part of each
     *  expectation list, so it is never left unasserted. */
    private static String jutter(String attributes, String expected) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(attributes, expected);
    }

    /** A value expectation plus the consistent verdict — the shape the three non-§6.5 witnesses use. */
    private static String valueJson(String attribute, String number) {
        return """
                { "kind": "attributeValue", "label": "J1", "attribute": "%s", "value": { "number": "%s" } },
                { "kind": "consistent" }""".formatted(attribute, number);
    }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10) ----------------

    private static String num(int dec, boolean whole, String sign) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(whole, dec, sign);
    }
    private static String num(int dec) { return num(dec, false, "unspecified"); }
    private static final String WHOLE_POS = num(0, true, "nonNegative");

    private static String attr(String name, String type) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"het %s van de Jutter\"}}")
                .formatted(name, name);
    }

    private static String lit(String valueJson, String type) {
        return IrPath.literal(valueJson, type, valueJson);
    }

    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, type, op.toLowerCase(java.util.Locale.ROOT));
    }

    /** ch. 4.10 the binding — a NEED under ch. 10.6's call-by-need. Its {@code type} is the BODY's (WF-076). */
    private static String let(String binder, String value, String body, String type) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"%s is …\"}}"
                .formatted(type, binder, value, body, binder);
    }

    /** NB the wire field is {@code name}, not {@code name}. */
    private static String bref(String name, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"%s\"}}".formatted(name, name);
    }

    private static String ite(String condition, String then, String otherwise, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(type, condition, then, otherwise);
    }

    private static String compare(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String rule(String name, String target, String valueJson) {
        return IrPath.rule(name, "BEREKENING", target, valueJson, "null");
    }

    /** A one-objecttype document over {@code Jutter}. */
    private static String model(String attributes, String[] rules) {
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{"
                + "\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(attributes, String.join(",", rules));
    }

    /** The {@code attributes} list of {@code Jutter}, from alternating name / datatype arguments. */
    private static String attributes(String... nameAndType) {
        if (nameAndType.length % 2 != 0) throw new IllegalArgumentException("a name without a datatype");
        java.util.StringJoiner list = new java.util.StringJoiner(",");
        for (int i = 0; i < nameAndType.length; i += 2)
            list.add("{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(nameAndType[i], nameAndType[i + 1]));
        return list.toString();
    }

    // ---------------- the models (each ref-replayed before its values were pinned) ----------------

    /** ch. 10.6 clause 3 / its twin: a value that FAULTS, under a binder the body reads or does not. */
    private static String zeroDivisionModel(boolean read) {
        String division = binary("dividedBy", lit("100.00", num(2)), lit("0.00", num(2)), num(2));
        String body = read ? bref("q", num(2)) : lit("42.00", num(2));
        return model(attributes("uitkomst", num(2)),
                new String[]{rule("uitkomst van een jutter", attr("uitkomst", num(2)),
                        let("q", division, body, num(2)))});
    }

    /** ch. 10.6 clause 2, the probe-B1 shape: a leeg need read ONLY in one branch of a ch. 10.2 choice whose
     *  condition reads a DIFFERENT, filled binder. `kindertal` decides which branch — and so whether the leeg
     *  need is ever read at all. */
    private static String probeModel() {
        String inside = let("x", attr("leegwaarde", num(2)),
                ite(compare("greater", bref("k", WHOLE_POS), lit("1", WHOLE_POS)),
                        lit("7.00", num(2)),
                        binary("times", bref("x", num(2)), lit("2.00", num(2)), num(2)),
                        num(2)),
                num(2));
        return model(attributes("kindertal", WHOLE_POS, "leegwaarde", num(2), "uitkomst", num(2)),
                new String[]{rule("uitkomst van een jutter", attr("uitkomst", num(2)),
                        let("k", attr("kindertal", WHOLE_POS), inside, num(2)))});
    }

    /** ch. 10.6 clause 1: one §6.5-depositing need, read twice and read once, on one subject. */
    private static String needModel() {
        String division = binary("dividedBy", attr("grondslag", num(2)), attr("deler", num(2)), num(2));
        return model(attributes("grondslag", num(2), "deler", num(2), "tweemaal", num(2), "eenmaal", num(2)),
                new String[]{
                        rule("tweemaal van een jutter", attr("tweemaal", num(2)),
                                let("q", division, binary("plus", bref("q", num(2)), bref("q", num(2)), num(2)), num(2))),
                        rule("eenmaal van een jutter", attr("eenmaal", num(2)),
                                let("q", division, bref("q", num(2)), num(2)))});
    }
}
