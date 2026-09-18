package io.github.yhavinga.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.10 / RUNTIME-SEMANTICS ch. 10.6 — the §11 <b>variabelendeel</b> that the result part reads,
 * held to ONE answer in BOTH transpiler branches. {@link LetClusterWitnessTest} pins the cluster's semantics
 * through the exec-model branch alone; this one runs every case through both, because until 2026-09-18 the
 * DRL-text branch did not lower the construct at all (a {@code TODO expressie} marker on the first result
 * binding) and a branch that markers agrees with everything.
 *
 * <p><b>The arbiter is {@code ref/}, not this backend.</b> Every value below was produced on this exact model
 * and case by the verifier + {@code ref} + the clingo backend first (measured 2026-09-18: verifier PASS, ref
 * PASS, clingo PASS on all five cases), and only then pinned here. No corpus file is read, written or moved:
 * the model and its cases are authored in this file.
 *
 * <p>What each test isolates, and what a wrong lowering it would let through:
 * <ul>
 *   <li>a variable read in the VOORWAARDE <i>and</i> in the result — the frontend substitutes the first and
 *       binds the second (ch. 10.6, tracked delta 1), so both halves must arrive in one rule;</li>
 *   <li>a LATER binder reading an EARLIER one — the scope that makes a chain a chain; without it the second
 *       definition lowers to an unbound-read marker and the whole golden stops playing;</li>
 *   <li>the ZERO guard: the rule's voorwaarde keeps the route unentered, so the division inside the variable
 *       definition is never evaluated. Sharp rather than cosmetic — {@code rsGedeeld} by a numeric zero
 *       THROWS, and a throw inside a consequence aborts the whole fire, so an implementation that evaluated
 *       the need anyway reports a fault instead of these values;</li>
 *   <li>the same guard one level in: an OUTER need that is leeg must keep an INNER need (a division by zero)
 *       from being forced — the ch. 10.6 clause-2 barrier standing in front of a clause-1 forcing;</li>
 *   <li>leeg BESIDE gevuld, so that an always-writing and a never-writing implementation each go red;</li>
 *   <li>one binder read twice, name-sameness that shadows without capturing, and a sibling binding that keeps
 *       its own answer when its neighbour empties — the three shapes a consequence-wide null guard, an
 *       in-place binder map or a value-text substitution each get wrong;</li>
 *   <li>a VOORWAARDE-ONLY variable, which the frontend already brought to both branches WITHOUT a Let: the
 *       non-regression flank of this whole card.</li>
 * </ul>
 *
 * <p><b>What the pins actually catch, measured by mutation on 2026-09-18</b> — a suite that only agrees with
 * the implementation it was written beside proves nothing, so the three ways to get this construct wrong were
 * each built and run:
 * <ul>
 *   <li>the barrier dropped (an ALWAYS-WRITING Let) → 3 red:
 *       {@code een_lege_behoefte_maakt_de_hele_let_leeg…}, {@code een_lege_buitenbehoefte…} and
 *       {@code het_lege_doel_is_observeerbaar_afwezig…};</li>
 *   <li>the Let always leeg (a NEVER-WRITING one) → 9 red, every filled pin among them;</li>
 *   <li>the prelude's own guard dropped → {@code een_lege_buitenbehoefte…} ERRORS with
 *       "Breuk deling door 0", the fault ch. 10.6 says must not happen.</li>
 * </ul>
 */
class LetResultaatTakWitnessTest {

    // ---------------- 1. the variable that feeds the voorwaarde AND the result ----------------

    @Test void een_variabele_in_voorwaarde_en_resultaat_komt_in_beide_takken_aan() throws Exception {
        // "de dubbel is het inkomen maal 2" is read by the voorwaarde (dubbel > 10,00 — SUBSTITUTED there,
        // since no Let can span the wire's separate voorwaarde and waarde fields) AND by the result
        // (dubbel plus dubbel — a Let with two BinderRefs). ref: aandeel = 160.00 at inkomen 40,00.
        beideSpelen(jutter(GEVULD, waarde("aandeel", "160.00")));
    }

    @Test void de_voorwaarde_die_niet_houdt_laat_het_doel_leeg() throws Exception {
        // the other side of the same rule: inkomen 2,00 makes dubbel 4,00, which is not > 10,00, so the rule
        // does not fire and `aandeel` stays leeg — the substituted condition half really decides. ref: leeg.
        beideSpelen(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INVOER_REST, leeg("aandeel")));
    }

    // ---------------- 2. a later binder reads an earlier one ----------------

    @Test void een_latere_binder_leest_de_eerdere() throws Exception {
        // gestaffeld := Let basis = inkomen in Let opslag = basis maal 2 in basis plus opslag.
        // `opslag`'s DEFINITION reads `basis`, which is in scope because the chain nests in declaration order
        // (§11.1 rule 1, outermost first). A branch that bound only the innermost binder lowers that read to an
        // unbound-read marker and refuses the whole rule. ref: 120.00 at inkomen 40,00 (40,00 + 80,00).
        beideSpelen(jutter(GEVULD, waarde("gestaffeld", "120.00")));
    }

    // ---------------- 3. the zero guard: an unentered route evaluates nothing ----------------

    @Test void de_nulgardering_houdt_de_deling_uit_de_niet_betreden_route() throws Exception {
        // quotient := Let q = teller gedeeld door deler in q plus 1,00, under the voorwaarde `deler ongelijk 0`.
        // With deler 0 the rule does not fire, so the need is never forced and the division never runs: the
        // target stays leeg and the run is CONSISTENT. Evaluating the need regardless is not a silent wrong
        // here — rsGedeeld throws "Breuk deling door 0" and the fire aborts — which is exactly what makes this
        // case a witness rather than a formality. ref: quotient leeg, consistent.
        beideSpelen(jutter(NULDELER, leeg("quotient") + ", " + waarde("aandeel", "160.00")));
    }

    @Test void de_gevulde_delingsroute_levert_de_oorspronkelijke_uitkomst() throws Exception {
        // the route ENTERED, so the same need is forced and the division really runs. ref: 11.00 (40,00 / 4,
        // plus 1,00) — the absolute value, not merely "something non-empty".
        beideSpelen(jutter(GEVULD, waarde("quotient", "11.00")));
    }

    @Test void een_lege_buitenbehoefte_houdt_de_binnenste_deling_ongeforceerd() throws Exception {
        // garde := Let veilig = inkomen in Let q = teller gedeeld door staffel in veilig plus q, with inkomen
        // ABSENT and staffel 0. ch. 10.6 reads `veilig` first, the barrier fires, and `q` is never forced — so
        // the division by zero never happens and the whole Let is leeg. This is the flank that makes the
        // prelude's own guard load-bearing: measured by mutation (guard dropped), this branch reports
        // "Breuk deling door 0" here instead of playing. ref: garde leeg, consistent.
        beideSpelen(jutter(BARRIERE_VOOR_DE_DELING, leeg("garde") + ", " + leeg("quotient")));
    }

    // ---------------- 4. leeg beside gevuld: both mutants go red ----------------

    @Test void een_lege_behoefte_maakt_de_hele_let_leeg_terwijl_dezelfde_maal_zonder_let_nul_rekent() throws Exception {
        // ch. 10.6's normative contrast in ONE case with `inkomen` ABSENT:
        //   metlet    := Let ink = inkomen in ink maal 2  -> the read forces the leeg need and raises the
        //                barrier, so the whole Let is leeg and nothing is written (ref: leeg);
        //   zonderlet := inkomen maal 2                   -> the bare maal COALESCES leeg to 0 (ref: 0.00).
        // An ALWAYS-WRITING lowering (the barrier dropped) writes 0.00 on both and fails here — measured.
        beideSpelen(jutter(LEEG, leeg("metlet") + ", " + waarde("zonderlet", "0.00")));
    }

    @Test void de_gevulde_invoer_schrijft_wel_en_beide_vormen_komen_uit_op_hetzelfde() throws Exception {
        // the other mutant's red: a NEVER-WRITING lowering (the Let always leeg) passes every emptiness pin
        // above and fails here — measured. ref: 80.00 for both forms at inkomen 40,00.
        beideSpelen(jutter(GEVULD, waarde("metlet", "80.00") + ", " + waarde("zonderlet", "80.00")));
    }

    @Test void het_lege_doel_is_observeerbaar_afwezig_in_beide_takken() throws Exception {
        // the comparator must SEE the absence rather than leave it unasserted: the same input with a VALUE
        // expectation on `metlet` must MISMATCH in both branches, and name the target.
        String geval = jutter(LEEG, waarde("metlet", "0.00"));
        for (String tak : List.of("exec", "drl")) {
            CaseRunner.Result r = speel(tak, geval);
            assertEquals("FAIL", r.status(), tak + ": de leeg-barriere moet observeerbaar zijn, niet slechts niet-geasserteerd");
            assertTrue(r.caseFailures().toString().contains("metlet"), tak + ": " + r.caseFailures());
        }
    }

    // ---------------- 5. one need per binder, shadowing, siblings ----------------

    @Test void dezelfde_binder_twee_keer_gelezen_levert_dezelfde_behoefte() throws Exception {
        // dubbelop := Let ink = inkomen in ink plus ink. A second read that answered leeg would coalesce to 0
        // inside plus and quietly halve the result. ref: 80.00 at inkomen 40,00.
        beideSpelen(jutter(GEVULD, waarde("dubbelop", "80.00")));
    }

    @Test void naamgelijkheid_schaduwt_binnen_en_lekt_niet_naar_buiten() throws Exception {
        // schaduw := Let x = inkomen in ((Let x = 5,00 in x) plus x). WF-064: the INNER x wins inside its own
        // body, and the OUTER x is intact again outside it. Reading the outer binder inside gives 80.00;
        // binding the inner x IN PLACE of the outer (a mutated environment instead of a scoped one) leaves
        // x = 5,00 for the trailing read and gives 10.00. Two same-named binders also may not collapse onto
        // one emitted variable. ref: 45.00 (5,00 + 40,00).
        beideSpelen(jutter(GEVULD, waarde("schaduw", "45.00")));
    }

    @Test void een_zusterbinding_houdt_haar_eigen_antwoord_als_de_buur_leegloopt() throws Exception {
        // zusters := (Let a = inkomen in a) plus (Let b = 5,00 in b), with inkomen ABSENT. The barrier belongs
        // to the Let that bound the binder that was read, so only the LEFT one empties; the §6.2 leegwaarde
        // table then coalesces it to 0 inside the plus and the right one still answers. A guard hung on the
        // whole consequence instead of on each Let node would empty the rule value and write leeg. ref: 5.00.
        beideSpelen(jutter(LEEG, waarde("zusters", "5.00")));
    }

    @Test void de_zusterbinding_rekent_gevuld_gewoon_mee() throws Exception {
        // the same pair with both sides gevuld, so the pin above is not passing on an arm that never adds.
        // ref: 45.00 (40,00 + 5,00).
        beideSpelen(jutter(GEVULD, waarde("zusters", "45.00")));
    }

    // ---------------- 6. the non-regression flank: a voorwaarde-only variable ----------------

    @Test void een_voorwaarde_only_variabele_blijft_zonder_let_werken() throws Exception {
        // "de dubbel is het inkomen maal 2" read ONLY in the voorwaarde gets no Let at all (ch. 10.6: an
        // unread binder would be a guaranteed no-op) — the frontend substitutes it, and both branches have
        // carried that shape all along. ref: premie 7.00 at inkomen 40,00 (80 > 50), leeg at inkomen 2,00.
        beideSpelen(jutter(GEVULD, waarde("premie", "7.00")));
        beideSpelen(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INVOER_REST, leeg("premie")));
    }

    // ---------------- replay: one document, one case, BOTH branches ----------------

    /** Both transpiler branches on the authored model and one case; each must PLAY it. */
    private static void beideSpelen(String geval) throws Exception {
        for (String tak : List.of("exec", "drl")) {
            CaseRunner.Result r = speel(tak, geval);
            assertEquals("PLAYED", r.status(), tak + ": " + r.reason() + " " + r.caseFailures());
        }
    }

    private static CaseRunner.Result speel(String tak, String geval) throws Exception {
        Path ir = tijdelijk("let-resultaat-ir", MODEL), c = tijdelijk("let-resultaat-case", geval);
        try {
            return tak.equals("drl") ? CaseRunner.viaDrlTekst(ir, List.of(c)) : CaseRunner.viaExecModel(ir, List.of(c));
        } finally {
            Files.deleteIfExists(ir);
            Files.deleteIfExists(c);
        }
    }

    private static Path tijdelijk(String naam, String inhoud) throws Exception {
        Path p = Files.createTempFile(naam, ".json");
        p.toFile().deleteOnExit();
        Files.writeString(p, inhoud);
        return p;
    }

    // ---------------- the case inputs ----------------

    /** teller/deler/staffel, the three inputs every case supplies the same way unless it says otherwise. */
    private static final String INVOER_REST =
            "\"teller\": { \"number\": \"40.00\" }, \"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }";
    private static final String GEVULD = "\"inkomen\": { \"number\": \"40.00\" }, " + INVOER_REST;
    private static final String LEEG = INVOER_REST;                     // inkomen ABSENT
    private static final String NULDELER =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"0\" }, \"staffel\": { \"number\": \"4\" }";
    private static final String BARRIERE_VOOR_DE_DELING =                // inkomen ABSENT and staffel 0
            "\"teller\": { \"number\": \"40.00\" }, \"deler\": { \"number\": \"0\" }, \"staffel\": { \"number\": \"0\" }";

    private static String jutter(String attributen, String verwacht) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(attributen, verwacht);
    }

    private static String waarde(String attribuut, String getal) {
        return """
                { "kind": "attributeValue", "label": "J1", "attribute": "%s", "value": { "number": "%s" } }"""
                .formatted(attribuut, getal);
    }

    private static String leeg(String attribuut) {
        return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"%s\" }".formatted(attribuut);
    }

    // ---------------- the wire shapes (IR-SPEC ch. 4.10) ----------------

    private static final String ONBEPAALD = "ONBEPAALD", NN = "NIET_NEGATIEF";

    private static String num(int dec, boolean geheel, String teken) {
        return "{\"node\":\"Numeriek\",\"geheel\":%b,\"decimalen\":%d,\"teken\":\"%s\",\"eenheid\":null}"
                .formatted(geheel, dec, teken);
    }

    private static String attr(String naam, int dec, boolean geheel, String teken) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"Jutter\","
                + "\"onderwerpBron\":\"de Jutter\",\"navigatieRol\":null,\"type\":%s,"
                + "\"bron\":\"het %s van de Jutter\",\"set\":false,\"navigatiePad\":[]}")
                .formatted(naam, num(dec, geheel, teken), naam);
    }

    private static String lit(String waarde, int dec, boolean geheel) {
        return "{\"node\":\"Literal\",\"waarde\":\"%s\",\"type\":%s,\"bron\":\"%s\"}"
                .formatted(waarde, num(dec, geheel, ONBEPAALD), waarde);
    }

    private static String binair(String op, String links, String rechts) {
        return "{\"node\":\"Binair\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"type\":%s,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, num(2, false, ONBEPAALD), op.toLowerCase());
    }

    /** ch. 4.10 the value-level binding; WF-076 makes its {@code type} the BODY's, which {@code teken} included
     *  is what the verifier checks — so the body's sign is spelled at each call site. */
    private static String let(String binder, String value, String body, String bodyTeken) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"bron\":\"de %s is …\"}"
                .formatted(num(2, false, bodyTeken), binder, value, body, binder);
    }

    /** ch. 4.10 a read of the nearest enclosing binder. NB the wire field is {@code name}, not {@code naam};
     *  WF-091 makes the read's sign that of the value it is bound to, so it rides along. */
    private static String bref(String naam, String teken) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"type\":%s,\"bron\":\"de %s\"}"
                .formatted(naam, num(2, false, teken), naam);
    }

    private static String verg(String op, String links, String rechts) {
        return "{\"node\":\"Vergelijking\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"periode\":null,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, op.toLowerCase());
    }

    private static String regel(String naam, String doel, String waarde, String voorwaarde) {
        return ("{\"node\":\"ResolvedRegel\",\"naam\":\"%s\",\"soort\":\"BEREKENING\",\"doel\":%s,\"waarde\":%s,"
                + "\"voorwaarde\":%s,\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(naam, doel, waarde, voorwaarde);
    }

    // ---------------- the one document (verifier-clean, ref- and clingo-replayed before pinning) ----------

    private static final String MODEL = bouwModel();

    private static String bouwModel() {
        String inkomen = attr("inkomen", 2, false, NN);
        String teller = attr("teller", 2, false, NN);
        String deler = attr("deler", 0, true, NN);
        String staffel = attr("staffel", 0, true, NN);
        String dubbelDef = binair("MAAL", inkomen, lit("2", 0, true));

        String[][] rs = {
            // the variable read in the voorwaarde (substituted) AND in the result (bound)
            {"aandeel van een jutter", attr("aandeel", 2, false, ONBEPAALD),
             let("dubbel", dubbelDef, binair("PLUS", bref("dubbel", ONBEPAALD), bref("dubbel", ONBEPAALD)), ONBEPAALD),
             verg("GROTER", dubbelDef, lit("10.00", 2, false))},
            // a later binder reading an earlier one
            {"gestaffeld van een jutter", attr("gestaffeld", 2, false, ONBEPAALD),
             let("basis", inkomen,
                 let("opslag", binair("MAAL", bref("basis", NN), lit("2", 0, true)),
                     binair("PLUS", bref("basis", NN), bref("opslag", ONBEPAALD)), ONBEPAALD), ONBEPAALD),
             "null"},
            // the zero guard: the division sits inside the variable definition, behind the rule's voorwaarde
            {"quotient van een jutter", attr("quotient", 2, false, ONBEPAALD),
             let("q", binair("GEDEELD_DOOR", teller, deler),
                 binair("PLUS", bref("q", ONBEPAALD), lit("1.00", 2, false)), ONBEPAALD),
             verg("ONGELIJK", deler, lit("0", 0, true))},
            // an outer need that empties must keep the inner (zero-dividing) need unforced
            {"garde van een jutter", attr("garde", 2, false, ONBEPAALD),
             let("veilig", inkomen,
                 let("q", binair("GEDEELD_DOOR", teller, staffel),
                     binair("PLUS", bref("veilig", NN), bref("q", ONBEPAALD)), ONBEPAALD), ONBEPAALD),
             "null"},
            // the barrier beside its contrast
            {"met let van een jutter", attr("metlet", 2, false, ONBEPAALD),
             let("ink", inkomen, binair("MAAL", bref("ink", NN), lit("2", 0, true)), ONBEPAALD), "null"},
            {"zonder let van een jutter", attr("zonderlet", 2, false, ONBEPAALD),
             binair("MAAL", inkomen, lit("2", 0, true)), "null"},
            // one binder, two reads
            {"dubbelop van een jutter", attr("dubbelop", 2, false, ONBEPAALD),
             let("ink", inkomen, binair("PLUS", bref("ink", NN), bref("ink", NN)), ONBEPAALD), "null"},
            // name-sameness: shadowed inside, intact outside
            {"schaduw van een jutter", attr("schaduw", 2, false, ONBEPAALD),
             let("x", inkomen,
                 binair("PLUS", let("x", lit("5.00", 2, false), bref("x", ONBEPAALD), ONBEPAALD),
                        bref("x", NN)), ONBEPAALD), "null"},
            // sibling bindings: the barrier belongs to the Let that bound the binder that was read
            {"zusters van een jutter", attr("zusters", 2, false, ONBEPAALD),
             binair("PLUS", let("a", inkomen, bref("a", NN), NN),
                    let("b", lit("5.00", 2, false), bref("b", ONBEPAALD), ONBEPAALD)), "null"},
            // the non-regression flank: a variable NO result read reaches gets no Let at all
            {"premie van een jutter", attr("premie", 2, false, ONBEPAALD), lit("7.00", 2, false),
             verg("GROTER", dubbelDef, lit("50.00", 2, false))},
        };

        StringBuilder namen = new StringBuilder(), regels = new StringBuilder();
        for (String[] r : rs) {
            if (namen.length() > 0) { namen.append(','); regels.append(','); }
            namen.append("{\"node\":\"Regel\",\"naam\":\"%s\",\"soort\":\"regel\"}".formatted(r[0]));
            regels.append(regel(r[0], r[1], r[2], r[3]));
        }
        StringBuilder attributen = new StringBuilder(
                "\"inkomen\":%s,\"teller\":%s,\"deler\":%s,\"staffel\":%s".formatted(
                        num(2, false, NN), num(2, false, NN), num(0, true, NN), num(0, true, NN)));
        for (String naam : new String[]{"aandeel", "gestaffeld", "quotient", "garde", "metlet", "zonderlet",
                                        "dubbelop", "schaduw", "zusters", "premie"})
            attributen.append(",\"%s\":%s".formatted(naam, num(2, false, ONBEPAALD)));

        return ("{\"formaat\":\"regelspraak-ir-v0\","
                + "\"objecttypen\":{\"Jutter\":{\"node\":\"Objecttype\",\"naam\":\"Jutter\",\"attributen\":{%s},"
                + "\"attribuutMeervoud\":{},\"attribuutDimensies\":{},\"attribuutGranulariteit\":{},"
                + "\"attribuutTijdlijnAantal\":{},\"attribuutTijdlijnNaam\":{},\"kenmerken\":[],\"kenmerkSoorten\":{},"
                + "\"kenmerkGranulariteit\":{},\"kenmerkTijdlijnAantal\":{},\"bezield\":true,\"meervoud\":\"Jutters\"}},"
                + "\"domeinen\":{},\"parameters\":{},\"eenheidstelsels\":{},\"feittypen\":{},\"dimensies\":{},"
                + "\"tijdlijnen\":[],\"dagsoorten\":[],\"dagsoortPerEenheid\":{},\"regels\":[%s],\"resolvedRegels\":[%s],"
                + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
                + "\"uniciteiten\":[],\"uniciteitUnies\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
                + "\"regelGroepen\":[],\"diagnostics\":[]}")
                .formatted(attributen, namen, regels);
    }
}
