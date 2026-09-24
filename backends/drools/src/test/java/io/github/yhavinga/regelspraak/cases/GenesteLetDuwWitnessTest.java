package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A ch. 4.10 {@code Let} standing between a NEED and the ch. 10.2 branch node that reads it, pinned in BOTH
 * transpiler branches ({@link BeideTakken}).
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
class GenesteLetDuwWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the value defect: the nested Let follows the arm that reads the need ----------------

    @Test void de_geneste_let_tussen_behoefte_en_takknoop_volgt_de_gelezen_arm() throws Exception {
        // "uit := Let k = inkomen in (Let z = k gedeeld door restpost in (Let w = 1,00 in
        //         (als k groter dan 10,00 dan w anders z)))" — the corpus shape `synth3-armverdeling`
        // carries, in miniature. `z` is read only in the anders-arm, `w` only in the dan-arm.
        //   inkomen 40,00, restpost ABSENT -> dan-arm: ref uit 1,00, CONSISTENT (z is never read, so the
        //                                     division never runs — ch. 10.6 clause 3).
        //   inkomen  5,00, restpost ABSENT -> anders-arm: ref uit leeg, INCONSISTENT.
        //   inkomen  5,00, restpost 2,00   -> anders-arm, and it simply computes: ref uit 2,50, CONSISTENT.
        // The first flank is the one that was red: `w`'s forcing gate carried `z != null`, which is false on
        // the dan-arm because `z` was never forced there, so `w` went unforced and its barrier emptied.
        BeideTakken.groen(schrijf(duwModel()), jutter(n("inkomen", "40.00"), waarde("1.00") + ", " + consistent()),
                "de dan-arm leest de geneste behoefte niet");
        BeideTakken.groen(schrijf(duwModel()), jutter(n("inkomen", "5.00"), leeg() + ", " + inconsistent()),
                "de anders-arm leest haar en meldt");
        BeideTakken.groen(schrijf(duwModel()), jutter(n("inkomen", "5.00") + ", " + n("restpost", "2.00"),
                waarde("2.50") + ", " + consistent()), "de anders-arm rekent gewoon");
    }

    // ---------------- the SCOPE pin of the barrier: the whole expression, never the arm ----------------

    @Test void de_barriere_van_de_geneste_behoefte_leegt_het_geheel_en_niet_de_arm() throws Exception {
        // The same shape with a COALESCING wrapper inside the nested Let's body: "… Let z = k gedeeld door
        // restpost in ((Let w = 1,00 in (als k groter dan 10,00 dan w anders z)) plus 5,00)" — the corpus
        // shape `synth3-armverdeling`'s wikkelheffing rule. The `plus` is what makes the barrier's EXTENT
        // observable (§6.2 coalesces a leeg operand to 0).
        //   inkomen 40,00, restpost ABSENT -> ref uit 6,00 (1,00 + 5,00), CONSISTENT.
        //   inkomen  5,00, restpost ABSENT -> ref uit leeg, INCONSISTENT — the barrier belongs to `Let z` and
        //                                     empties its whole body, the `plus` included. NOT 5,00.
        // Both flanks are falsifiable in the same direction: a lowering that shrinks the barrier to the arm
        // answers 5,00 on each of them.
        BeideTakken.groen(schrijf(wikkelModel()), jutter(n("inkomen", "40.00"), waarde("6.00") + ", " + consistent()),
                "de niet-lezende arm telt gewoon op");
        BeideTakken.groen(schrijf(wikkelModel()), jutter(n("inkomen", "5.00"), leeg() + ", " + inconsistent()),
                "de barriere leegt het geheel, niet de arm");
    }

    // ---------------- the refusal that closed: the guard's binder is bound deeper ----------------

    @Test void de_takgarde_leest_haar_binder_doordat_de_twee_lets_wisselen() throws Exception {
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
        BeideTakken.groen(schrijf(wisselModel()), jutter(n("inkomen", "40.00"), waarde("0.00") + ", " + consistent()),
                "de dan-arm leest de geneste behoefte niet");
        BeideTakken.groen(schrijf(wisselModel()), jutter(n("inkomen", "5.00"), leeg() + ", " + inconsistent()),
                "de anders-arm leest haar en meldt");
        BeideTakken.groen(schrijf(wisselModel()), jutter(n("inkomen", "5.00") + ", " + n("restpost", "2.00"),
                waarde("2.50") + ", " + consistent()), "de anders-arm rekent gewoon");
    }

    // ---------------- the lens finding: a need that is ITSELF a binder ----------------

    @Test void een_behoefte_die_zelf_een_binder_is_meldt_op_haar_eigen_plek() throws Exception {
        // "uit := Let k = inkomen in (Let m = k in (m gedeeld door restpost))" — no branch node at all, and
        // the §6.5 site sits behind TWO needs, the inner of which is a read of the outer. The companion
        // decomposed its needs in one loop and its binder environment in another, so that inner need met an
        // empty environment and the companion was dropped: ref, clingo and the exec branch reported, this
        // branch answered a silent CONSISTENT.
        //   inkomen 10,00, restpost ABSENT -> ref uit leeg, INCONSISTENT.
        //   inkomen 10,00, restpost  2,00  -> ref uit 5,00, CONSISTENT (the fabricage flank: nothing faults,
        //                                     so nothing reports).
        BeideTakken.groen(schrijf(ketenModel()), jutter(n("inkomen", "10.00"), leeg() + ", " + inconsistent()),
                "de behoefte-als-binder meldt");
        BeideTakken.groen(schrijf(ketenModel()), jutter(n("inkomen", "10.00") + ", " + n("restpost", "2.00"),
                waarde("5.00") + ", " + consistent()), "de behoefte-als-binder verzint niets");
    }

    // ---------------- the SCOPE, first direction: a need both arms read stays eager ----------------

    @Test void een_behoefte_die_beide_armen_lezen_blijft_eager_en_dat_is_exact() throws Exception {
        // "… Let w = 1,00 in (als k groter dan 10,00 dan (w plus z) anders z)" — `z` is read on BOTH arms, so
        // its need is wanted whichever arm runs and there is no arm to confine it to. The guard that decides
        // this is a MEASUREMENT of where the binder is read (through nested values as well), not a syntactic
        // look at the shape, and here it measures "always".
        //   inkomen 40,00, restpost ABSENT -> dan-arm READS z too: ref uit leeg, INCONSISTENT.
        //   inkomen  5,00, restpost ABSENT -> anders-arm: ref uit leeg, INCONSISTENT.
        //   inkomen 40,00, restpost 2,00   -> dan-arm computes: ref uit 21,00 (1,00 + 20,00), CONSISTENT.
        // A lowering that confined this need to one arm leaves the other arm reading an unbound binder.
        BeideTakken.groen(schrijf(beideArmenModel()), jutter(n("inkomen", "40.00"), leeg() + ", " + inconsistent()),
                "de dan-arm leest de behoefte ook");
        BeideTakken.groen(schrijf(beideArmenModel()), jutter(n("inkomen", "5.00"), leeg() + ", " + inconsistent()),
                "de anders-arm leest haar");
        BeideTakken.groen(schrijf(beideArmenModel()), jutter(n("inkomen", "40.00") + ", " + n("restpost", "2.00"),
                waarde("21.00") + ", " + consistent()), "beide lezingen rekenen");
    }

    // ---------------- the SCOPE, second direction: the shape no route reaches ----------------

    @Test void een_wikkel_tussen_de_twee_lets_wordt_luid_geweigerd_in_de_drl_tak() throws Exception {
        // "… Let z = k gedeeld door restpost in ((Let w = k in (als w groter dan 10,00 dan 0,00 anders z))
        //  plus 5,00)" — the exchange cannot step OVER the arithmetic wrapper standing between the two
        // `Let`s, and there is no branch node at `z`'s own position to distribute into. ref answers 5,00 here
        // (inkomen 40,00, restpost ABSENT) and the exec branch reproduces it; the DRL-text branch refuses BY
        // NAME, and clingo raises its own loud TRANSLATION FAULT on the same document. The boundary is thus
        // the same one on both rewriting branches, which is the point of pinning it.
        //
        // {@link BeideTakken} has no shape for "one branch plays, the other refuses" on purpose — a row that
        // asserts a per-branch verdict is how a branch silently stops being covered. So this one is spelled
        // out, and it fails the day the refusal turns into an answer, which is exactly when carrying this
        // shape must become a deliberate act with a measured value behind it.
        Path ir = schrijf(wikkelTussenModel());
        JsonNode c = jutter(n("inkomen", "40.00"), waarde("5.00") + ", " + consistent());
        assertNull(CaseRunner.speelCase(ir, c, false), "exec-model — de wikkel tussen de twee Lets speelt");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, c, true),
                "drl-tekst — de wikkel tussen de twee Lets wordt geweigerd");
        assertTrue(e.getMessage().contains("takgarde van een voorwaardelijk gelezen behoefte (ch. 10.2)"),
                "drl-tekst noemt de weigering bij naam: " + e.getMessage());
    }

    @Test void een_takgarde_wier_binder_tussendoor_herbonden_wordt_wordt_luid_geweigerd() throws Exception {
        // "uit := Let w = saldo in ((Let z = w gedeeld door restpost in (Let w = inkomen in
        //         (als w groter dan 10,00 dan 0,00 anders z))) plus w)" — `z`'s need is read in the anders-arm
        // alone, so its guard is "NOT (w > 10,00)"; but the `w` that guard means is the INNER one, and at `z`'s
        // own position that name still belongs to the OUTER binding. The exchange cannot help: moving `z` past
        // `Let w = inkomen` would make its own value read the inner `w` instead of the outer one.
        //
        // Measured 2026-09-19 and PRE-EXISTING (identical on the branch state before this card): the guard was
        // resolved against the outer binding, and with saldo 5,00, inkomen 40,00 and restpost ABSENT that made
        // this branch FABRICATE an Inconsistentie — ref answers uit 5,00 CONSISTENT (the dan-arm runs, `z` is
        // never read, the division never happens), the exec branch reproduces it, and clingo refuses the
        // document with its own loud fault. An invented fault is the worst answer available, so the shape is
        // refused by name now. A sibling spelling whose two bindings happened to AGREE played correctly before
        // and is refused too: the coincidence was not a reason to keep it.
        Path ir = schrijf(herbondenModel());
        // saldo 5,00 / inkomen 40,00 / restpost ABSENT — the fabricage flank: ref uit 5,00, CONSISTENT.
        // saldo 5,00 / inkomen  8,00 / restpost 2,00  — the VALUE flank: ref uit 7,50 (5,00/2,00 + 5,00), and
        //   it is 9,00 if the two `w`s are confused (8,00/2,00 + 5,00), so the two bindings are told apart by
        //   a number here and not only by a report.
        for (JsonNode c : new JsonNode[] {
                jutter(n("saldo", "5.00") + ", " + n("inkomen", "40.00"), waarde("5.00") + ", " + consistent()),
                jutter(n("saldo", "5.00") + ", " + n("inkomen", "8.00") + ", " + n("restpost", "2.00"),
                        waarde("7.50") + ", " + consistent()) }) {
            assertNull(CaseRunner.speelCase(ir, c, false), "exec-model — de herbonden takgarde speelt");
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(ir, c, true),
                    "drl-tekst — de herbonden takgarde wordt geweigerd");
            assertTrue(e.getMessage().contains("takgarde van een voorwaardelijk gelezen behoefte (ch. 10.2)"),
                    "drl-tekst noemt de weigering bij naam: " + e.getMessage());
        }
    }

    // ---------------- helpers: replay ----------------

    /** One authored model on disk — {@link BeideTakken} replays a PATH through both branches. */
    private static Path schrijf(String ir) throws Exception {
        Path tmp = Files.createTempFile("geneste-let-duw-witness", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return tmp;
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static String n(String attribuut, String getal) {
        return "\"%s\": { \"number\": \"%s\" }".formatted(attribuut, getal);
    }

    private static JsonNode jutter(String attributen, String verwacht) throws Exception {
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

    private static String num(int dec, String teken) {
        return "{\"node\":\"Number\",\"whole\":false,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(dec, teken);
    }
    private static String num() { return num(2, "ONBEPAALD"); }
    private static String pos() { return num(2, "NIET_NEGATIEF"); }

    private static String attr(String naam) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"Jutter\","
                + "\"source\":{\"snippet\":\"zijn %s\"}}")
                .formatted(naam, naam);
    }

    private static String lit(String waarde) {
        return IrPad.literal(waarde, num(), waarde);
    }

    private static String binair(String op, String links, String rechts) {
        return "{\"node\":\"Binair\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, links, rechts, num(), op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String let(String binder, String value, String body) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"%s is …\"}}"
                .formatted(num(), binder, value, body, binder);
    }

    /** NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String naam, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"%s\"}}".formatted(naam, naam);
    }

    private static String ite(String conditie, String dan, String anders) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(num(), conditie, dan, anders);
    }

    /** "&lt;binder&gt; groter dan 10,00" — the ch. 10.2 guard every branch shape below shares. */
    private static String boven(String binder) {
        return ("{\"node\":\"Vergelijking\",\"op\":\"GROTER\",\"links\":%s,\"rechts\":%s,"
                + "\"periode\":null,\"source\":{\"snippet\":\"boven de grens\"}}").formatted(bref(binder, pos()), lit("10.00"));
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
                + "\"rules\":{\"resolvedRegels\":[{\"node\":\"Rule\",\"name\":\"%s\",\"versions\":[{"
                + "\"validity\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null},"
                + "\"target\":%s,\"value\":%s,\"initialisation\":false,\"presentation\":\"BEREKENING\"}]}],"
                + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
                + "\"uniciteiten\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
                + "\"regelGroepen\":[]},\"diagnostics\":[]}")
                .formatted(pos(), pos(), pos(), num(), NAAM, doel(), waarde);
    }

    private static String doel() {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"uit\",\"objecttype\":\"Jutter\","
                + "\"source\":{\"snippet\":\"De uit van een Jutter\"}}");
    }

    // ---------------- the models (each ref-replayed, and clingo-replayed, before its values were pinned) ----

    /** "k gedeeld door zijn restpost" — the §6.5 site every branch shape below puts in the anders-arm. */
    private static String deling() {
        return binair("GEDEELD_DOOR", bref("k", pos()), attr("restpost"));
    }

    /** {@code Let k = inkomen in (Let z = k/restpost in <body>)} — the two outer needs all four branch
     *  shapes share; only what stands in `z`'s body differs. */
    private static String tweeBehoeften(String body) {
        return let("k", attr("inkomen"), let("z", deling(), body));
    }

    /** A THIRD `Let` between the need and the branch node, read in the dan-arm alone. */
    private static String duwModel() {
        return model(tweeBehoeften(let("w", lit("1.00"), ite(boven("k"), bref("w", num()), bref("z", num())))));
    }

    /** The same, with a coalescing wrapper around the branch node INSIDE `z`'s body. */
    private static String wikkelModel() {
        return model(tweeBehoeften(binair("PLUS",
                let("w", lit("1.00"), ite(boven("k"), bref("w", num()), bref("z", num()))), lit("5.00"))));
    }

    /** The third `Let`'s own binder is what the branch CONDITION reads — the exchange's shape. */
    private static String wisselModel() {
        return model(tweeBehoeften(let("w", bref("k", pos()), ite(boven("w"), lit("0.00"), bref("z", num())))));
    }

    /** The same, with an arithmetic wrapper standing BETWEEN the two `Let`s — the shape no route reaches. */
    private static String wikkelTussenModel() {
        return model(tweeBehoeften(binair("PLUS",
                let("w", bref("k", pos()), ite(boven("w"), lit("0.00"), bref("z", num()))), lit("5.00"))));
    }

    /** `z` read on BOTH arms — the need is wanted whichever arm runs. */
    private static String beideArmenModel() {
        return model(tweeBehoeften(let("w", lit("1.00"), ite(boven("k"),
                binair("PLUS", bref("w", num()), bref("z", num())), bref("z", num())))));
    }

    /** The branch guard's own binder REBOUND between the `Let` it gates and the branch node — the one name
     *  that means two different bindings at the two positions. */
    private static String herbondenModel() {
        return model(let("w", attr("saldo"),
                binair("PLUS",
                        let("z", binair("GEDEELD_DOOR", bref("w", pos()), attr("restpost")),
                                let("w", attr("inkomen"),
                                        ite(boven("w"), lit("0.00"), bref("z", num())))),
                        bref("w", pos()))));
    }

    /** "Let k = inkomen in (Let m = k in (m gedeeld door restpost))" — a need that is itself a binder. */
    private static String ketenModel() {
        return model(let("k", attr("inkomen"),
                let("m", bref("k", pos()), binair("GEDEELD_DOOR", bref("m", pos()), attr("restpost")))));
    }
}
