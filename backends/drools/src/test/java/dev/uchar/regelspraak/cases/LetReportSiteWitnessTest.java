package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The §9.5 channel reason for a §6.5 place inside a {@link dev.uchar.regelspraak.model.Expr.Let}
 * BODY, pinned in BOTH transpiler branches ({@link BothBranches}).
 *
 * <p>WHY it exists: until 2026-09-19 the DRL-text branch deposited no Inconsistentie for a §6.5 site whose
 * operand was a BINDER, which is every site inside a {@code Let} body that reads its need. The two documents
 * below differ in nothing but that {@code Let} — {@code uit := inkomen gedeeld door restpost} against
 * {@code uit := Let k = inkomen in (k gedeeld door restpost)} — and the first reported while the second
 * answered a silent CONSISTENT (measured on the asp-ch102-vervolg card, 2026-09-19, and reproduced here
 * before the fix: {@code FAIL: verwacht inconsistent, werkelijk consistent}). The gap was the BINDING, not
 * the branch: ch. 4.10 moves the value, not the duty to report it.
 *
 * <p>Four corpus goldens sat on that gap — {@code synth3-takhek}, {@code synth3-operatortak},
 * {@code synth3-armvangst} played in the DRL branch the moment it closed — but a corpus golden pins a whole
 * document through one entry point, and gate 12 measures the exec branch alone. So the row that says the two
 * branches agree has to be here: a row that runs in one branch pins one branch.
 *
 * <p><b>Every pinned value was produced by {@code ref/} on this exact model+case first</b> (the arbiter), and
 * each is reproduced by clingo as a third reading; this backend is the fourth executor. Both flank families
 * are present, so an implementation that errs in either direction fails: the MISSER family (a fault ref
 * raises and a branch must not lose under a binding) and the FABRICAGE family (a report ref does NOT raise —
 * the divisor is filled, the need's barrier cut the body off, the arm was not entered).
 */
class LetReportSiteWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's two probes: the same site, one `Let` apart ----------------

    @Test void the_same_division_reports_with_and_without_let_in_both_branches() throws Exception {
        // ref on both models (inkomen 10,00, restpost ABSENT): uit leeg, INCONSISTENT — §6.5 Tabel 12 degrades
        // "waarde gedeeld door leeg" to leeg AND deposits the Fout, and the read of the need does not change
        // which site ran. The bare spelling played in both branches all along; the bound one is the fix.
        String verwacht = empty() + ", " + inconsistent();
        BothBranches.green(write(bareModel()), jutter(INKOMEN, verwacht), "de kale deling door leeg");
        BothBranches.green(write(letModel()), jutter(INKOMEN, verwacht), "dezelfde deling onder een Let");
    }

    @Test void the_bound_division_does_not_report_if_the_divisor_is_filled() throws Exception {
        // the fabricage flank of the pair above, on the same model: nothing faults, so nothing reports.
        // ref (inkomen 10,00, restpost 2,00): uit 5.0000, CONSISTENT. A companion that fired on the divisor's
        // leegte alone — or on the binding's mere presence — answers INCONSISTENT here.
        BothBranches.green(write(letModel()), jutter(INKOMEN + ", " + n("restpost", "2.00"),
                waarde("5.0000") + ", " + consistent()), "de gebonden deling rekent gewoon");
    }

    // ---------------- ch. 10.6 clause 2: the need's own barrier still cuts the report off ----------------

    @Test void the_barrier_of_the_need_cuts_the_message_off_and_otherwise_leaves_it_standing() throws Exception {
        // "uit := Let k = inkomen in (k plus (saldo gedeeld door restpost))" — the read of `k` is the LEFT
        // operand, so it runs BEFORE the division. The site's own operands (saldo, restpost) are filled resp.
        // absent in both flanks; what differs is the NEED.
        //   inkomen ABSENT  -> the read raises the barrier, the division never runs: ref uit leeg, CONSISTENT.
        //   inkomen 10,00   -> the read succeeds, the division runs and faults (leeg, coalesced to 0 by the
        //                      plus): ref uit 10.0000, INCONSISTENT.
        // The need guard is what separates the two, and it is not the same read as the site's operands — a
        // companion that kept only the operand guards reports on both.
        BothBranches.green(write(barrierModel()), jutter(n("saldo", "8.00"), empty() + ", " + consistent()),
                "de lege behoefte kapt af voor de deling");
        BothBranches.green(write(barrierModel()), jutter(INKOMEN + ", " + n("saldo", "8.00"),
                waarde("10.0000") + ", " + inconsistent()), "de gevulde behoefte laat de deling draaien");
    }

    // ---------------- ch. 10.2: the site under an arm, in the corpus shape ----------------

    @Test void the_site_under_an_arm_reports_only_on_the_taken_arm() throws Exception {
        // "uit := Let k = inkomen in (als k groter dan 10,00 dan 5,0000 anders (k gedeeld door restpost))" —
        // the shape corpus golden `synth3-takhek` carries, in miniature. restpost is ABSENT in both flanks.
        //   inkomen 40,00 -> the dan-arm runs, the division does not: ref uit 5.0000, CONSISTENT.
        //   inkomen  5,00 -> the anders-arm runs: ref uit leeg, INCONSISTENT.
        // Both the need guard and the arm guard have to reach the companion, and the arm guard has to read the
        // binder through the need (a separate rule sees no consequence local) — drop either and one flank falls.
        BothBranches.green(write(armModel()), jutter(n("inkomen", "40.00"), waarde("5.0000") + ", " + consistent()),
                "de niet-betreden arm meldt niet");
        BothBranches.green(write(armModel()), jutter(n("inkomen", "5.00"), empty() + ", " + inconsistent()),
                "de betreden arm meldt");
    }

    // ---------------- the boundary: what a binding is NOT responsible for ----------------

    @Test void a_literal_dividend_reports_in_both_branches() throws Exception {
        // The OTHER reason a §6.5 site carried no companion, separated from the binding so the two are never confused
        // again: a LITERAL (or parameter, or to-many) operand lowered to no re-readable leeg/vol guard, so the
        // destructive lowerings dropped the companion, with or without a `Let` around it. "uit := 100,00 gedeeld door
        // restpost", restpost ABSENT: ref uit leeg, INCONSISTENT; the destructive DRL-text branch answered CONSISTENT
        // until card drl-teksttak-op-truth-maintenance, as the destructive exec-model branch did. The analysis with
        // truth maintenance has no companions: it deposits where the division reads leeg, whatever the dividend, and
        // answers as ref (TmsLikeRefWitnessTest, literaal-deeltal-1 and -2), on both branches.
        BothBranches.green(write(literalModel()), jutter("", empty() + ", " + inconsistent()),
                "de deling door een lege deler deponeert");
    }

    // ---------------- helpers: replay ----------------

    /** One authored model on disk — {@link BothBranches} replays a PATH through both branches. */
    private static Path write(String ir) throws Exception {
        Path tmp = Files.createTempFile("let-meldplek-witness", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return tmp;
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static final String INKOMEN = "\"inkomen\": { \"number\": \"10.00\" }";

    private static String n(String attribuut, String number) {
        return "\"%s\": { \"number\": \"%s\" }".formatted(attribuut, number);
    }

    private static com.fasterxml.jackson.databind.JsonNode jutter(String attributes, String verwacht) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(attributes, verwacht));
    }

    private static String waarde(String number) {
        return "{ \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"uit\", \"value\": { \"number\": \"%s\" } }"
                .formatted(number);
    }
    private static String empty() { return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"uit\" }"; }
    private static String consistent() { return "{ \"kind\": \"consistent\" }"; }
    private static String inconsistent() { return "{ \"kind\": \"inconsistent\" }"; }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10 and its neighbours) ----------------

    private static String num(int dec, boolean whole, String sign) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(whole, dec, sign);
    }
    private static String num(int dec) { return num(dec, false, "unspecified"); }
    private static String pos(int dec) { return num(dec, false, "nonNegative"); }

    private static String attr(String naam, String type) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"het %s van de Jutter\"}}")
                .formatted(naam, naam);
    }
    private static String attr(String naam) { return attr(naam, pos(2)); }

    private static String lit(String waarde, String type) {
        return IrPath.literal(waarde, type, waarde);
    }

    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, type, op.toLowerCase(java.util.Locale.ROOT));
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

    private static String verg(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static final String NAME = "uit van een jutter";

    /** A one-objecttype document over {@code Jutter} with "uit van een jutter" as its only rule. */
    private static String model(String waarde) {
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
                .formatted(pos(2), pos(2), num(2), num(4), NAME, attr("uit", num(4)), waarde);
    }

    // ---------------- the models (each ref-replayed, and clingo-replayed, before its values were pinned) ----------------

    /** "het inkomen gedeeld door de restpost" — the §6.5 site, un-bound. */
    private static String bareModel() {
        return model(binary("dividedBy", attr("inkomen"), attr("restpost"), num(4)));
    }

    /** The SAME site, one `Let` deep: "Let k = inkomen in (k gedeeld door restpost)". */
    private static String letModel() {
        return model(let("k", attr("inkomen"), boundDivision(), num(4)));
    }

    /** "Let k = inkomen in (k plus (saldo gedeeld door restpost))" — the need is read BEFORE the site, and it
     *  is not one of the site's own operands. */
    private static String barrierModel() {
        String division = binary("dividedBy", attr("saldo", num(2)), attr("restpost"), num(4));
        return model(let("k", attr("inkomen"), binary("plus", bref("k", pos(2)), division, num(4)), num(4)));
    }

    /** "Let k = inkomen in (als k groter dan 10,00 dan 5,0000 anders (k gedeeld door restpost))". */
    private static String armModel() {
        String keuze = ite(verg("greater", bref("k", pos(2)), lit("10.00", pos(2))),
                lit("5.0000", num(4)), boundDivision(), num(4));
        return model(let("k", attr("inkomen"), keuze, num(4)));
    }

    /** "100,00 gedeeld door de restpost" — a LITERAL dividend, no `Let` in sight. */
    private static String literalModel() {
        return model(binary("dividedBy", lit("100.00", pos(2)), attr("restpost"), num(4)));
    }

    private static String boundDivision() {
        return binary("dividedBy", bref("k", pos(2)), attr("restpost"), num(4));
    }
}
