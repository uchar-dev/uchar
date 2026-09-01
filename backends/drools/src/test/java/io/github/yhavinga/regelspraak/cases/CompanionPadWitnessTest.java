package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §6.5/§6.6 fout-companions carry the PATH: a companion fires exactly when the division / wortel it
 * guards is actually EVALUATED under ch. 10.2 (exactly one branch runs) and ch. 10.6 (call-by-need, the
 * leeg-barrier at the read) — never for a site on a path that does not run, never silent for a site on a
 * path that does. Witnessed via {@link CaseRunner#speelCase} over models authored HERE (no corpus file is
 * read, moved or touched).
 *
 * <p>Before this card the companion was a SEPARATE static rule keyed on the operands' leegte, which cannot
 * see which path ran. The card before it chose never-fabricate ({@code letKinderenZeker}): no companion for a
 * site in a branch, in a body, or in a value not read on every path — so every such site with a REAL fault
 * missed it (the "spiegelflank": ref inconsistent, Drools consistent). Both flank families are pinned here,
 * each beside its twin, so an implementation that errs in either direction fails:
 * <ul>
 *   <li><b>the MISSER family</b> (a fault ref raises that a path-blind companion cannot): a division in a
 *       Let BODY, which under call-by-need always runs; a division in the branch ch. 10.2 DOES take; a
 *       division in a Let VALUE whose binder IS read; a division evaluated BEFORE a foreign barrier;</li>
 *   <li><b>the FABRICAGE family</b> (a fault ref does not raise, because the site never runs): a division
 *       behind a read that raises the barrier first; in the branch NOT taken; in a value no read forces; in
 *       a value read only on the branch not taken.</li>
 * </ul>
 * <b>Every pinned value was produced by {@code ref/} on this exact model+case first</b> (2026-09-02, the
 * L-ref) — ref is the arbiter, this backend the third executor. Evaluation ORDER matters for the barrier
 * flanks and was measured rather than assumed: ref evaluates a {@code Binair} left before right
 * ({@code ref/lib/eval.ml}), so a read left of a division ends the Let before the division, and a read
 * right of it does not.
 */
class CompanionPadWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- (a) the spiegelflank: a Let body always runs ----------------

    @Test void een_deling_in_een_draaiende_letbody_met_ongelezen_binder_is_inconsistent() throws Exception {
        // "uit := Let d = deler in inkomen gedeeld door tweede" — `d` is never read, so ch. 10.6 clause 3 never
        // evaluates `deler` and the body ALWAYS runs: the division by the absent `tweede` is real, whatever
        // `deler` holds. ref, both cases: uit leeg, INCONSISTENT. The bare twin (no Let) says the same.
        String verwacht = leeg("uit") + ", " + inconsistent();
        assertNull(speel(delingModel(), jutter(INKOMEN, verwacht)), "de kale deling door leeg");
        assertNull(speel(spiegelModel(), jutter(INKOMEN, verwacht)), "dezelfde deling in een Let-body, deler leeg");
        assertNull(speel(spiegelModel(), jutter(INKOMEN + ", " + n("deler", "2.00"), verwacht)),
                "dezelfde deling in een Let-body, deler gevuld");
    }

    // ---------------- (b) the barrier that IS passed first, and the one that is not ----------------

    @Test void een_lezing_die_aan_de_deling_voorafgaat_kapt_via_de_barriere_de_fout_af() throws Exception {
        // "uit := Let d = deler in d plus (inkomen gedeeld door tweede)" — the read of `d` comes FIRST (left
        // operand). With `deler` absent that read raises the barrier, the Let ends, and the division never
        // happens: ref uit leeg, CONSISTENT. With `deler` = 2,00 the read succeeds, the division runs and
        // faults (leeg, coalesced to 0 by the plus): ref uit 2.0000, INCONSISTENT.
        assertNull(speel(barriereModel(true), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "lezing eerst, barriere: geen deling, geen fout");
        assertNull(speel(barriereModel(true), jutter(INKOMEN + ", " + n("deler", "2.00"),
                        waarde("uit", "2.0000") + ", " + inconsistent())),
                "lezing eerst, gevuld: de deling draait en faalt");
    }

    @Test void dezelfde_deling_voor_de_lezing_deponeert_de_fout_wel() throws Exception {
        // the mirror: "(inkomen gedeeld door tweede) plus d" — the division is evaluated BEFORE the read, so
        // its fault is deposited whatever the read then does. ref: deler absent -> leeg, INCONSISTENT (the
        // barrier still empties the Let, but after the fault); deler 2,00 -> 2.0000, INCONSISTENT.
        assertNull(speel(barriereModel(false), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "deling eerst, dan de barriere: de fout staat");
        assertNull(speel(barriereModel(false), jutter(INKOMEN + ", " + n("deler", "2.00"),
                        waarde("uit", "2.0000") + ", " + inconsistent())),
                "deling eerst, gevuld");
    }

    // ---------------- (c) ch. 10.2: the branch taken, and the branch not taken ----------------

    @Test void een_deling_in_de_genomen_tak_is_inconsistent_en_in_de_niet_genomen_niet() throws Exception {
        // "als a groter dan 100 dan inkomen gedeeld door deler anders 7,0000", `deler` absent. a = 200 takes
        // the DAN branch: the division runs -> ref uit leeg, INCONSISTENT. a = 1 takes ANDERS: ref 7.0000,
        // CONSISTENT. Both orientations, bare (the IfThenElse as the rule value) and in a Let body whose binder
        // feeds the condition (the WF-077 shape).
        String genomen = leeg("uit") + ", " + inconsistent();
        String nietGenomen = waarde("uit", "7.0000") + ", " + consistent();
        for (boolean inLet : new boolean[]{false, true}) {
            String vorm = inLet ? " (in een Let-body)" : " (kaal)";
            assertNull(speel(keuzeModel(inLet, false), jutter(INKOMEN + ", " + n("a", "200.00"), genomen)),
                    "de genomen tak" + vorm);
            assertNull(speel(keuzeModel(inLet, false), jutter(INKOMEN + ", " + n("a", "1.00"), nietGenomen)),
                    "de niet-genomen tak" + vorm);
        }
    }

    // ---------------- (d) ch. 10.6: a division in the VALUE runs only when a read forces it ----------------

    @Test void een_deling_in_een_letvalue_faalt_alleen_als_de_binder_gelezen_wordt() throws Exception {
        // "Let q = inkomen gedeeld door tweede in q plus 1,0000" — the read forces the need: the division runs,
        // faults (leeg) and the barrier then empties the Let. ref: leeg, INCONSISTENT.
        // "Let q = inkomen gedeeld door tweede in 42,0000" — nothing reads q, nothing runs. ref: 42.0000, CONSISTENT.
        assertNull(speel(valueModel(true), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())), "gelezen");
        assertNull(speel(valueModel(false), jutter(INKOMEN, waarde("uit", "42.0000") + ", " + consistent())), "ongelezen");
    }

    @Test void een_letvalue_gelezen_in_slechts_een_tak_faalt_per_tak() throws Exception {
        // "Let k = a in Let q = inkomen gedeeld door tweede in (als k groter dan 100 dan q anders 7,0000)":
        // q is read only in the DAN branch. a = 200 -> the read forces the faulting division: ref leeg,
        // INCONSISTENT. a = 1 -> ANDERS, q never read: ref 7.0000, CONSISTENT. (The card before this one could
        // certify neither — "read on some but not every path" got no companion at all.)
        assertNull(speel(eenTakModel(), jutter(INKOMEN + ", " + n("a", "200.00"), leeg("uit") + ", " + inconsistent())),
                "gelezen op de genomen tak");
        assertNull(speel(eenTakModel(), jutter(INKOMEN + ", " + n("a", "1.00"), waarde("uit", "7.0000") + ", " + consistent())),
                "ongelezen op de niet-genomen tak");
    }

    // ---------------- (e) a FOREIGN barrier: another Let's read, before or after the forcing read ----------------

    @Test void een_lezing_van_een_andere_lege_binder_voor_de_deling_kapt_af_erna_niet() throws Exception {
        // "Let x = leegding in Let q = inkomen gedeeld door tweede in x plus q", `leegding` absent: the read of
        // x raises the OUTER Let's barrier before q is ever forced — ref leeg, CONSISTENT. Mirror "q plus x":
        // q is forced first (fault), then x ends the outer Let — ref leeg, INCONSISTENT. The barrier belongs to
        // the Let that bound the binder that was read, and the order of the two reads decides.
        assertNull(speel(vreemdeBarriereModel(true), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "de vreemde lezing eerst");
        assertNull(speel(vreemdeBarriereModel(false), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "de vreemde lezing erna");
    }

    // ---------------- (f) the §6.6 wortel twins ----------------

    @Test void een_wortel_in_een_draaiende_letbody_is_even_inconsistent_als_de_kale() throws Exception {
        // "Let d = deler in de wortel van het saldo", saldo = -4,00, binder unread: the body runs, the negative
        // radicand degrades to leeg AND deposits the §6.6 Fout. ref on both models: uit leeg, INCONSISTENT.
        String verwacht = leeg("uit") + ", " + inconsistent();
        String invoer = n("saldo", "-4.00");
        assertNull(speel(wortelModel(false), jutter(invoer, verwacht)), "de kale wortel van negatief");
        assertNull(speel(wortelModel(true), jutter(invoer, verwacht)), "dezelfde wortel in een Let-body");
    }

    @Test void een_wortel_in_de_genomen_tak_is_inconsistent_en_in_de_niet_genomen_niet() throws Exception {
        // "Let a = a in (als a groter dan 100 dan de wortel van het saldo anders 7,0000)", saldo = -4,00.
        // ref: a = 200 -> leeg, INCONSISTENT; a = 1 -> 7.0000, CONSISTENT.
        assertNull(speel(keuzeModel(true, true), jutter(n("saldo", "-4.00") + ", " + n("a", "200.00"),
                leeg("uit") + ", " + inconsistent())), "de genomen tak");
        assertNull(speel(keuzeModel(true, true), jutter(n("saldo", "-4.00") + ", " + n("a", "1.00"),
                waarde("uit", "7.0000") + ", " + consistent())), "de niet-genomen tak");
    }

    // ---------------- (h) two sites in one rule value: each carries its OWN path ----------------

    @Test void twee_delingen_in_een_regelwaarde_krijgen_elk_hun_eigen_padconditie() throws Exception {
        // "(inkomen gedeeld door deler) plus (Let d = leegding in d plus (inkomen gedeeld door tweede))" — the
        // first division is bare (runs whenever the rule does), the second sits behind the read of `d`. Two
        // companions, indexed 1 and 2 (the DRL naming parity), each with its own condition. ref:
        //   everything absent      -> 0.0000, INCONSISTENT (site 1 faults; site 2 is cut off by the barrier)
        //   deler 2, leegding 1    -> 6.0000, INCONSISTENT (site 1 = 5; d read fine, site 2 faults -> 1 + 0)
        //   deler 2, leegding leeg -> 5.0000, CONSISTENT   (site 1 = 5; the barrier empties the Let; no fault)
        assertNull(speel(tweeDelingenModel(), jutter(INKOMEN, waarde("uit", "0.0000") + ", " + inconsistent())),
                "beide operanden leeg: de kale deling faalt, de omhulde is afgekapt");
        assertNull(speel(tweeDelingenModel(), jutter(INKOMEN + ", " + n("deler", "2.00") + ", " + n("leegding", "1.00"),
                waarde("uit", "6.0000") + ", " + inconsistent())), "alleen de omhulde deling faalt");
        assertNull(speel(tweeDelingenModel(), jutter(INKOMEN + ", " + n("deler", "2.00"),
                waarde("uit", "5.0000") + ", " + consistent())), "de omhulde deling is afgekapt, de kale rekent");
    }

    // ---------------- (p) a Parameter inside the carrier: the companion binds what the path reads ----------------

    @Test void een_parameter_in_de_drager_wordt_voor_de_padconditie_gebonden() throws Exception {
        // "Let d = deler in (inkomen gedeeld door tweede) plus GRONDSLAG", grondslag = 3 (a Parameter global).
        // The path condition evaluates the carrier, so it must bind the same globals the host binds. ref:
        // tweede absent -> 3.0000, INCONSISTENT; tweede 2,00 -> 8.0000, CONSISTENT.
        assertNull(speel(parameterModel(), jutter(INKOMEN, waarde("uit", "3.0000") + ", " + inconsistent(), "\"grondslag\": { \"number\": \"3\" }")),
                "de deling faalt, de parameter telt op");
        assertNull(speel(parameterModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"), waarde("uit", "8.0000") + ", " + consistent(), "\"grondslag\": { \"number\": \"3\" }")),
                "de deling rekent");
    }

    // ---------------- (g) the defaults consequence keeps its fence ----------------

    @Test void het_defaults_delingshek_blijft_staan_voor_een_deling_in_een_letbody() throws Exception {
        // A ch. 7 default whose consequence is "Let d = deler in inkomen gedeeld door tweede". ref on this
        // model+case (tweede absent, kindertal 3): toeslag 0.0000, CONSISTENT — the faulting default falls
        // through and ch. 7.1 discards its speculative channel deposit. That value this backend deliberately
        // does not produce: the division fence refuses the group, and the path-carrying companions of this
        // card are RULE machinery that must not start playing a defaults consequence. The honest outcome is
        // still the refusal, naming the fence.
        CaseRunner.Excluded weigering = assertThrows(CaseRunner.Excluded.class,
                () -> speel(defaultsModel(), jutter(INKOMEN + ", " + n("kindertal", "3"),
                        waarde("toeslag", "0.0000") + ", " + consistent())));
        assertTrue(weigering.getMessage().contains("deling"),
                "de weigering noemt het delingshek: " + weigering.getMessage());
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. */
    private static String speel(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("companion-pad-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.speelCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static final String INKOMEN = "\"inkomen\": { \"number\": \"10.00\" }";

    private static String n(String attribuut, String getal) {
        return "\"%s\": { \"number\": \"%s\" }".formatted(attribuut, getal);
    }

    private static String jutter(String attributen, String verwacht) { return jutter(attributen, verwacht, ""); }

    private static String jutter(String attributen, String verwacht, String parameters) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": { %s } },
                  "expected": [ %s ] }
                """.formatted(attributen, parameters, verwacht);
    }

    private static String waarde(String attribuut, String getal) {
        return "{ \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"%s\", \"value\": { \"number\": \"%s\" } }"
                .formatted(attribuut, getal);
    }

    private static String leeg(String attribuut) {
        return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"%s\" }".formatted(attribuut);
    }

    private static String consistent() { return "{ \"kind\": \"consistent\" }"; }
    private static String inconsistent() { return "{ \"kind\": \"inconsistent\" }"; }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10 and its neighbours) ----------------

    private static String num(int dec, boolean geheel, String teken) {
        return "{\"node\":\"Numeriek\",\"geheel\":%b,\"decimalen\":%d,\"teken\":\"%s\",\"eenheid\":null}"
                .formatted(geheel, dec, teken);
    }
    private static String num(int dec) { return num(dec, false, "ONBEPAALD"); }
    private static String pos(int dec) { return num(dec, false, "NIET_NEGATIEF"); }
    private static final String GEHEEL_POS = num(0, true, "NIET_NEGATIEF");

    private static String attr(String naam, String type) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"Jutter\","
                + "\"onderwerpBron\":\"de Jutter\",\"navigatieRol\":null,\"type\":%s,"
                + "\"bron\":\"het %s van de Jutter\",\"set\":false,\"navigatiePad\":[]}")
                .formatted(naam, type, naam);
    }
    private static String attr(String naam) { return attr(naam, pos(2)); }

    private static String lit(String waarde, String type) {
        return "{\"node\":\"Literal\",\"waarde\":\"%s\",\"type\":%s,\"bron\":\"%s\"}".formatted(waarde, type, waarde);
    }

    private static String param(String naam, String type) {
        return "{\"node\":\"ParameterRef\",\"parameter\":\"%s\",\"type\":%s,\"bron\":\"de %s\"}".formatted(naam, type, naam);
    }

    private static String binair(String op, String links, String rechts, String type) {
        return "{\"node\":\"Binair\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"type\":%s,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, type, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String wortel(String argument, String type) {
        return ("{\"node\":\"Functie\",\"kind\":\"WORTEL\",\"argumenten\":[%s],\"type\":%s,"
                + "\"bron\":\"de wortel van\",\"filter\":null,\"leegwaarde\":null,\"elementObjecttype\":null,"
                + "\"dimensieSelectie\":null,\"granulariteit\":null}").formatted(argument, type);
    }

    private static String let(String binder, String value, String body, String type) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"bron\":\"%s is …\"}"
                .formatted(type, binder, value, body, binder);
    }

    /** NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String naam, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"type\":%s,\"bron\":\"%s\"}".formatted(naam, type, naam);
    }

    private static String ite(String conditie, String dan, String anders, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"bron\":\"als … dan … anders …\"}").formatted(type, conditie, dan, anders);
    }

    private static String verg(String op, String links, String rechts) {
        return "{\"node\":\"Vergelijking\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"periode\":null,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static final String ALTIJD =
            "{\"node\":\"Samengesteld\",\"kwantor\":\"ALLE\",\"drempel\":0,\"onderdelen\":[],\"bron\":\"altijd\"}";

    private static String regel(String naam, String doel, String waarde) {
        return ("{\"node\":\"ResolvedRegel\",\"naam\":\"%s\",\"soort\":\"BEREKENING\",\"doel\":%s,\"waarde\":%s,"
                + "\"voorwaarde\":null,\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(naam, doel, waarde);
    }

    /** The one Jutter every model here uses, carrying every attribute any flank reads. */
    private static final String ATTRIBUTEN =
            ("\"a\":%s,\"inkomen\":%s,\"deler\":%s,\"tweede\":%s,\"leegding\":%s,\"saldo\":%s,"
                    + "\"kindertal\":%s,\"uit\":%s,\"toeslag\":%s")
                    .formatted(pos(2), pos(2), pos(2), pos(2), pos(2), num(2), GEHEEL_POS, num(4), num(4));

    /** A one-objecttype document over {@code Jutter}; {@code parameters} and {@code prioritizedDefaults} are
     *  spliced verbatim (empty for the plain shapes). */
    private static String model(String[] regelNamen, String[] regels, String parameters, String prioritizedDefaults) {
        StringBuilder namen = new StringBuilder();
        for (String n : regelNamen) {
            if (namen.length() > 0) namen.append(',');
            namen.append("{\"node\":\"Regel\",\"naam\":\"%s\",\"soort\":\"regel\"}".formatted(n));
        }
        return ("{\"formaat\":\"regelspraak-ir-v0\","
                + "\"objecttypen\":{\"Jutter\":{\"node\":\"Objecttype\",\"naam\":\"Jutter\",\"attributen\":{%s},"
                + "\"attribuutMeervoud\":{},\"attribuutDimensies\":{},\"attribuutGranulariteit\":{},"
                + "\"attribuutTijdlijnAantal\":{},\"attribuutTijdlijnNaam\":{},\"kenmerken\":[],\"kenmerkSoorten\":{},"
                + "\"kenmerkGranulariteit\":{},\"kenmerkTijdlijnAantal\":{},\"bezield\":true,\"meervoud\":\"Jutters\"}},"
                + "\"domeinen\":{},\"parameters\":{%s},\"eenheidstelsels\":{},\"feittypen\":{},\"dimensies\":{},"
                + "\"tijdlijnen\":[],\"dagsoorten\":[],\"dagsoortPerEenheid\":{},\"regels\":[%s],\"resolvedRegels\":[%s],"
                + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
                + "\"uniciteiten\":[],\"uniciteitUnies\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
                + "\"regelGroepen\":[],\"diagnostics\":[]%s}")
                .formatted(ATTRIBUTEN, parameters, namen, String.join(",", regels), prioritizedDefaults);
    }

    /** "uit van een jutter" over the given value. */
    private static String uitModel(String waarde) { return uitModel(waarde, ""); }
    private static String uitModel(String waarde, String parameters) {
        return model(new String[]{"uit van een jutter"},
                new String[]{regel("uit van een jutter", attr("uit", num(4)), waarde)}, parameters, "");
    }

    // ---------------- the models (each ref-replayed before its values were pinned) ----------------

    /** "het inkomen gedeeld door de tweede" — the §6.5 site every Let-shaped model here wraps. */
    private static final String DELING = binair("GEDEELD_DOOR", attr("inkomen"), attr("tweede"), num(4));
    private static final String DELING_DELER = binair("GEDEELD_DOOR", attr("inkomen"), attr("deler"), num(4));
    private static final String ZEVEN = lit("7.0000", num(4));
    private static final String WORTEL = wortel(attr("saldo", num(2)), num(4));

    /** (a) the bare division. */
    private static String delingModel() { return uitModel(DELING); }

    /** (a) the spiegelflank: the same division as the BODY of a Let whose binder the body never reads. */
    private static String spiegelModel() { return uitModel(let("d", attr("deler"), DELING, num(4))); }

    /** (b) "Let d = deler in d plus (inkomen gedeeld door tweede)" ({@code lezingEerst}) or its mirror with the
     *  division on the left. */
    private static String barriereModel(boolean lezingEerst) {
        String body = lezingEerst
                ? binair("PLUS", bref("d", pos(2)), DELING, num(4))
                : binair("PLUS", DELING, bref("d", pos(2)), num(4));
        return uitModel(let("d", attr("deler"), body, num(4)));
    }

    /** (c)/(f) a ch. 10.2 choice over "a groter dan 100" with the §6.5 division (or the §6.6 wortel) in the DAN
     *  branch — the IfThenElse bare as the rule value, or in a Let body whose binder feeds the condition. */
    private static String keuzeModel(boolean inLetBody, boolean metWortel) {
        String dan = metWortel ? WORTEL : DELING_DELER;
        String links = inLetBody ? bref("a", pos(2)) : attr("a");
        String keuze = ite(verg("GROTER", links, lit("100", GEHEEL_POS)), dan, ZEVEN, num(4));
        return uitModel(inLetBody ? let("a", attr("a"), keuze, num(4)) : keuze);
    }

    /** (d) the division as a Let VALUE, read ({@code q plus 1}) or not ({@code 42}). */
    private static String valueModel(boolean gelezen) {
        String body = gelezen ? binair("PLUS", bref("q", num(4)), lit("1.0000", num(4)), num(4)) : lit("42.0000", num(4));
        return uitModel(let("q", DELING, body, num(4)));
    }

    /** (d) the division as a Let VALUE read only in the DAN branch of a choice on another binder. */
    private static String eenTakModel() {
        String keuze = ite(verg("GROTER", bref("k", pos(2)), lit("100", GEHEEL_POS)), bref("q", num(4)), ZEVEN, num(4));
        return uitModel(let("k", attr("a"), let("q", DELING, keuze, num(4)), num(4)));
    }

    /** (e) "Let x = leegding in Let q = inkomen gedeeld door tweede in x plus q" ({@code vreemdeEerst}) or "q plus x". */
    private static String vreemdeBarriereModel(boolean vreemdeEerst) {
        String body = vreemdeEerst
                ? binair("PLUS", bref("x", pos(2)), bref("q", num(4)), num(4))
                : binair("PLUS", bref("q", num(4)), bref("x", pos(2)), num(4));
        return uitModel(let("x", attr("leegding"), let("q", DELING, body, num(4)), num(4)));
    }

    /** (f) "de wortel van het saldo", bare or as the body of a Let whose binder is never read. */
    private static String wortelModel(boolean inLetBody) {
        return uitModel(inLetBody ? let("d", attr("deler"), WORTEL, num(4)) : WORTEL);
    }

    /** (h) a bare division plus a Let-wrapped one behind a read. */
    private static String tweeDelingenModel() {
        String omhuld = let("d", attr("leegding"), binair("PLUS", bref("d", pos(2)), DELING, num(4)), num(4));
        return uitModel(binair("PLUS", DELING_DELER, omhuld, num(4)));
    }

    /** (p) a Parameter read inside the carrier, beside the division. */
    private static String parameterModel() {
        String body = binair("PLUS", DELING, param("grondslag", GEHEEL_POS), num(4));
        return uitModel(let("d", attr("deler"), body, num(4)),
                "\"grondslag\":{\"node\":\"Parameter\",\"naam\":\"grondslag\",\"datatype\":%s,\"granulariteit\":null}"
                        .formatted(GEHEEL_POS));
    }

    /** (g) a ch. 7 group over {@code Jutter.toeslag}: a justified first default whose consequence wraps the
     *  division in a Let with an unread binder, then an unconditional 0. */
    private static String defaultsModel() {
        String consequentie = let("d", attr("deler"), DELING, num(4));
        String groep = ("{\"node\":\"PrioritizedDefaults\",\"naam\":\"toeslag\",\"onderwerp\":\"Jutter\",\"kenmerk\":null,"
                + "\"attribuut\":\"toeslag\",\"defaults\":["
                + "{\"node\":\"Default\",\"besluit\":null,\"voorwaarde\":%s,\"consequentie\":%s,\"bron\":\"toeslag\"},"
                + "{\"node\":\"Default\",\"besluit\":null,\"voorwaarde\":%s,\"consequentie\":%s,\"bron\":\"toeslag\"}],"
                + "\"bron\":\"toeslag\",\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(verg("GROTER_OF_GELIJK", attr("kindertal", GEHEEL_POS), lit("2", GEHEEL_POS)),
                        consequentie, ALTIJD, lit("0.0000", num(4)));
        return model(new String[]{}, new String[]{}, "", ",\"prioritizedDefaults\":[" + groep + "]");
    }
}
