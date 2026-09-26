package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The §9.5 channel reason for a §6.5 place inside a {@link io.github.yhavinga.regelspraak.model.Expr.Let}
 * BODY, pinned in BOTH transpiler branches ({@link BeideTakken}).
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
class LetMeldplekWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's two probes: the same site, one `Let` apart ----------------

    @Test void dezelfde_deling_meldt_met_en_zonder_let_in_beide_takken() throws Exception {
        // ref on both models (inkomen 10,00, restpost ABSENT): uit leeg, INCONSISTENT — §6.5 Tabel 12 degrades
        // "waarde gedeeld door leeg" to leeg AND deposits the Fout, and the read of the need does not change
        // which site ran. The bare spelling played in both branches all along; the bound one is the fix.
        String verwacht = leeg() + ", " + inconsistent();
        BeideTakken.groen(schrijf(kaalModel()), jutter(INKOMEN, verwacht), "de kale deling door leeg");
        BeideTakken.groen(schrijf(letModel()), jutter(INKOMEN, verwacht), "dezelfde deling onder een Let");
    }

    @Test void de_gebonden_deling_meldt_niet_als_de_deler_gevuld_is() throws Exception {
        // the fabricage flank of the pair above, on the same model: nothing faults, so nothing reports.
        // ref (inkomen 10,00, restpost 2,00): uit 5.0000, CONSISTENT. A companion that fired on the divisor's
        // leegte alone — or on the binding's mere presence — answers INCONSISTENT here.
        BeideTakken.groen(schrijf(letModel()), jutter(INKOMEN + ", " + n("restpost", "2.00"),
                waarde("5.0000") + ", " + consistent()), "de gebonden deling rekent gewoon");
    }

    // ---------------- ch. 10.6 clause 2: the need's own barrier still cuts the report off ----------------

    @Test void de_barriere_van_de_behoefte_kapt_de_melding_af_en_laat_haar_anders_staan() throws Exception {
        // "uit := Let k = inkomen in (k plus (saldo gedeeld door restpost))" — the read of `k` is the LEFT
        // operand, so it runs BEFORE the division. The site's own operands (saldo, restpost) are filled resp.
        // absent in both flanks; what differs is the NEED.
        //   inkomen ABSENT  -> the read raises the barrier, the division never runs: ref uit leeg, CONSISTENT.
        //   inkomen 10,00   -> the read succeeds, the division runs and faults (leeg, coalesced to 0 by the
        //                      plus): ref uit 10.0000, INCONSISTENT.
        // The need guard is what separates the two, and it is not the same read as the site's operands — a
        // companion that kept only the operand guards reports on both.
        BeideTakken.groen(schrijf(barriereModel()), jutter(n("saldo", "8.00"), leeg() + ", " + consistent()),
                "de lege behoefte kapt af voor de deling");
        BeideTakken.groen(schrijf(barriereModel()), jutter(INKOMEN + ", " + n("saldo", "8.00"),
                waarde("10.0000") + ", " + inconsistent()), "de gevulde behoefte laat de deling draaien");
    }

    // ---------------- ch. 10.2: the site under an arm, in the corpus shape ----------------

    @Test void de_plek_onder_een_tak_meldt_alleen_op_de_betreden_arm() throws Exception {
        // "uit := Let k = inkomen in (als k groter dan 10,00 dan 5,0000 anders (k gedeeld door restpost))" —
        // the shape corpus golden `synth3-takhek` carries, in miniature. restpost is ABSENT in both flanks.
        //   inkomen 40,00 -> the dan-arm runs, the division does not: ref uit 5.0000, CONSISTENT.
        //   inkomen  5,00 -> the anders-arm runs: ref uit leeg, INCONSISTENT.
        // Both the need guard and the arm guard have to reach the companion, and the arm guard has to read the
        // binder through the need (a separate rule sees no consequence local) — drop either and one flank falls.
        BeideTakken.groen(schrijf(takModel()), jutter(n("inkomen", "40.00"), waarde("5.0000") + ", " + consistent()),
                "de niet-betreden arm meldt niet");
        BeideTakken.groen(schrijf(takModel()), jutter(n("inkomen", "5.00"), leeg() + ", " + inconsistent()),
                "de betreden arm meldt");
    }

    // ---------------- the boundary: what a binding is NOT responsible for ----------------

    @Test void een_literale_deeltal_meldt_in_geen_van_beide_takken_ook_zonder_let() throws Exception {
        // The OTHER reason a §6.5 site carries no companion, separated from the binding so the two are never
        // confused again: a LITERAL (or parameter, or to-many) operand lowers to no re-readable leeg/vol guard,
        // so the companion is dropped — with or without a `Let` around it. "uit := 100,00 gedeeld door
        // restpost", restpost ABSENT: ref uit leeg, INCONSISTENT; BOTH branches answer CONSISTENT. That is the
        // documented scope limit of the companion machinery (see companionDeel), it predates this card, it is
        // NOT what the card closed, and it is pinned RED here so that lifting it is a deliberate act with a
        // failing row to its name rather than a silent widening.
        BeideTakken.rood(schrijf(literaalModel()), jutter("", leeg() + ", " + inconsistent()),
                "verwacht inconsistent", "het literale deeltal draagt geen herleesbaar hek");
    }

    // ---------------- helpers: replay ----------------

    /** One authored model on disk — {@link BeideTakken} replays a PATH through both branches. */
    private static Path schrijf(String ir) throws Exception {
        Path tmp = Files.createTempFile("let-meldplek-witness", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return tmp;
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static final String INKOMEN = "\"inkomen\": { \"number\": \"10.00\" }";

    private static String n(String attribuut, String getal) {
        return "\"%s\": { \"number\": \"%s\" }".formatted(attribuut, getal);
    }

    private static com.fasterxml.jackson.databind.JsonNode jutter(String attributen, String verwacht) throws Exception {
        return JSON.readTree("""
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(attributen, verwacht));
    }

    private static String waarde(String getal) {
        return "{ \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"uit\", \"value\": { \"number\": \"%s\" } }"
                .formatted(getal);
    }
    private static String leeg() { return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"uit\" }"; }
    private static String consistent() { return "{ \"kind\": \"consistent\" }"; }
    private static String inconsistent() { return "{ \"kind\": \"inconsistent\" }"; }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10 and its neighbours) ----------------

    private static String num(int dec, boolean geheel, String teken) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(geheel, dec, teken);
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
        return IrPad.literal(waarde, type, waarde);
    }

    private static String binair(String op, String links, String rechts, String type) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, links, rechts, type, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String let(String binder, String value, String body, String type) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"%s is …\"}}"
                .formatted(type, binder, value, body, binder);
    }

    /** NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String naam, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"%s\"}}".formatted(naam, naam);
    }

    private static String ite(String conditie, String dan, String anders, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(type, conditie, dan, anders);
    }

    private static String verg(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, links, rechts, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static final String NAAM = "uit van een jutter";

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
                .formatted(pos(2), pos(2), num(2), num(4), NAAM, attr("uit", num(4)), waarde);
    }

    // ---------------- the models (each ref-replayed, and clingo-replayed, before its values were pinned) ----------------

    /** "het inkomen gedeeld door de restpost" — the §6.5 site, un-bound. */
    private static String kaalModel() {
        return model(binair("dividedBy", attr("inkomen"), attr("restpost"), num(4)));
    }

    /** The SAME site, one `Let` deep: "Let k = inkomen in (k gedeeld door restpost)". */
    private static String letModel() {
        return model(let("k", attr("inkomen"), gebondenDeling(), num(4)));
    }

    /** "Let k = inkomen in (k plus (saldo gedeeld door restpost))" — the need is read BEFORE the site, and it
     *  is not one of the site's own operands. */
    private static String barriereModel() {
        String deling = binair("dividedBy", attr("saldo", num(2)), attr("restpost"), num(4));
        return model(let("k", attr("inkomen"), binair("plus", bref("k", pos(2)), deling, num(4)), num(4)));
    }

    /** "Let k = inkomen in (als k groter dan 10,00 dan 5,0000 anders (k gedeeld door restpost))". */
    private static String takModel() {
        String keuze = ite(verg("greater", bref("k", pos(2)), lit("10.00", pos(2))),
                lit("5.0000", num(4)), gebondenDeling(), num(4));
        return model(let("k", attr("inkomen"), keuze, num(4)));
    }

    /** "100,00 gedeeld door de restpost" — a LITERAL dividend, no `Let` in sight. */
    private static String literaalModel() {
        return model(binair("dividedBy", lit("100.00", pos(2)), attr("restpost"), num(4)));
    }

    private static String gebondenDeling() {
        return binair("dividedBy", bref("k", pos(2)), attr("restpost"), num(4));
    }
}
