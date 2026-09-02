package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.10 — the FENCES a Let-cluster CARRIER must keep, the structural WALKS that must see through
 * it, and the two paths on which a companion must NOT FIRE, witnessed via {@link CaseRunner#speelCase} over
 * models authored HERE (no corpus file is read, moved or touched).
 *
 * <p>The sibling {@code LetClusterWitnessTest} pins what the Let cluster COMPUTES. This file pins the other
 * half — what the carrier HIDES. To every generic {@code Expr} tree walk the carrier is an opaque
 * {@code Onopgelost} leaf, so a walk that does not descend under-reports in SILENCE: a missing §9.5
 * fout-companion reads as "consistent", a missing read reads as "nothing to watch". Each witness below is a
 * model on which the walk's blindness is OBSERVABLE, and each was measured against {@code ref/} on this exact
 * model+case before it was pinned here — ref is the arbiter, this backend the third executor.
 *
 * <ul>
 *   <li><b>the two fences that must REFUSE.</b> A refusal is the honest outcome: the cross-unit guard in the
 *       ch. 10.2 branch condition (whose absence picks the WRONG branch — 500 m &gt; 1 km read as 500 &gt; 1),
 *       and the ch. 7 defaults division fence (which must keep firing through a Let). Both are asserted as a
 *       reason-carrying exclusion, not as a value;</li>
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
 *       re-evaluates the carrier and deposits only when a §6.5/§6.6 site under it really ran. A companion
 *       fired for code that never executed is INCONSISTENT where ref says consistent, which is a wrong ANSWER, not an
 *       over-cautious refusal. Each is pinned beside the flank where the fault IS real.</li>
 * </ul>
 */
class LetDragerWandelingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- 1. the fences: a loud refusal, never a quiet answer ----------------

    @Test void het_cross_unit_hek_weigert_luid_in_plaats_van_de_verkeerde_tak_te_kiezen() throws Exception {
        // "uit := Let a = aanloop in (als a groter dan 1 km dan 1,00 anders 2,00)", aanloop = 500 m.
        // ref on this exact model+case: uit = 2.00 (the §5.1 Tabel 32 restatement makes it 500 m > 1000 m,
        // which is FALSE). rsAggVergelijk applies NO restatement, so lowering this guard would compare the
        // raw numbers 500 > 1 and take the DAN branch: 1.00, silently wrong. letConditie's restatement guard
        // refuses instead — and the whole rule then falls out of scope, loudly, with its name in the reason.
        CaseRunner.Excluded weigering = assertThrows(CaseRunner.Excluded.class,
                () -> speel(kruisEenheidModel(), geval(inst("B1", "Baan", "\"aanloop\": { \"number\": \"500\" }", ""),
                        waarde("B1", "uit", "2.00") + ", " + consistent())));
        assertTrue(weigering.getMessage().contains("uit van een baan"),
                "de weigering noemt de regel die zij laat vallen: " + weigering.getMessage());
    }

    @Test void het_defaults_delingshek_vuurt_door_een_let_heen() throws Exception {
        // The ch. 7 defaults lowering refuses a consequence that can DEPOSIT in the §6.5 fout channel, because
        // a default that is evaluated and then discarded would leave the Inconsistentie behind. The division
        // here sits behind a Let ("Let d = deler in inkomen gedeeld door d"), so a carrier-blind bevatDeling
        // would let the group through and the fence would be a dead letter on every Let-carrying default.
        // ref on this model+case: toeslag = 5.0000 (kindertal 3, inkomen 10.00, deler 2.00) — a value this
        // backend deliberately does NOT produce; the honest outcome here is the refusal, and it names the fence.
        CaseRunner.Excluded weigering = assertThrows(CaseRunner.Excluded.class,
                () -> speel(defaultsDelingModel(), geval(inst("J1", "Jutter",
                        "\"kindertal\": { \"number\": \"3\" }, \"inkomen\": { \"number\": \"10.00\" }, "
                                + "\"deler\": { \"number\": \"2.00\" }", ""),
                        waarde("J1", "toeslag", "5.0000") + ", " + consistent())));
        assertTrue(weigering.getMessage().contains("deling"),
                "de weigering noemt het delingshek: " + weigering.getMessage());
    }

    // ---------------- 2. the walks that must see through the carrier ----------------

    @Test void een_letverborgen_genavigeerde_lezing_houdt_de_verre_zijde_reactief() throws Exception {
        // "uit := Let k = het afgeleid van zijn strandmaat in k maal 2,00" — the read is NAVIGATED, so the
        // consumer's reactivity depends on navRefs seeing it. Its producer ("afgeleid") is FORCED into a lower
        // stratum (§9.2: it reads a negated kenmerk that has a producer, salience -1), so it fires AFTER this
        // rule. Carrier-blind, the far-side pattern carries no constraint on `afgeleid`, the rule computes once
        // against a leeg partner value (maal coalesces it to 0) and never recomputes.
        // ref on this exact model+case: J1.afgeleid 30.0000, J2.afgeleid 60.0000, J1.uit 120.000000,
        // J2.uit 60.000000 (the fixpoint; the strandmaatschap is wederkerig, so J2 reads J1 back).
        assertNull(speel(verreReactiviteitModel(), geval(
                inst("J1", "Jutter", "\"strandtegoed\": { \"number\": \"10.00\" }", "\"strandmaat\": [\"J2\"]")
                        + ", " + inst("J2", "Jutter", "\"strandtegoed\": { \"number\": \"20.00\" }", ""),
                waarde("J1", "afgeleid", "30.0000") + ", " + waarde("J2", "afgeleid", "60.0000") + ", "
                        + waarde("J1", "uit", "120.000000") + ", " + waarde("J2", "uit", "60.000000") + ", "
                        + consistent())));
    }

    @Test void een_letverborgen_regelafgeleide_lezing_krijgt_zijn_defaults_salience() throws Exception {
        // TWO defaults members over one target, declared unordered — a conflict-capable ch. 7 group, whose
        // members must fire only after everything they READ is derived. Member A's consequence hides its read
        // of the RULE-DERIVED `grondslag` behind a Let ("Let g = grondslag in g"); its producer is again forced
        // into a lower stratum. Carrier-blind, exprLeesnamen reports no read, the group's salience does not
        // sink below the producer, and member A decides on a leeg grondslag.
        // ref on this exact model+case: grondslag = 200.00, toeslag = 200.00 (the conflict resolves to the
        // literal member — which it can only do once member A has a REAL value to lose with).
        assertNull(speel(defaultsSalienceModel(), geval(
                inst("J1", "Jutter", "\"kindertal\": { \"number\": \"3\" }, \"basis\": { \"number\": \"100.00\" }", ""),
                waarde("J1", "grondslag", "200.00") + ", " + waarde("J1", "toeslag", "200.00") + ", "
                        + consistent())));
    }

    // ---------------- 3. the §9.5 companions and the exact comparison ----------------

    @Test void een_letomhulde_deling_door_leeg_is_even_inconsistent_als_de_kale() throws Exception {
        // §6.5 Tabel 12: a filled dividend over a LEEG divisor degrades the value to leeg AND deposits a Fout.
        // The value side is carrier-agnostic (the Let body is ordinary arithmetic), so ONLY the companion —
        // verzamelDelingen — can go missing, and it goes missing SILENTLY: "consistent" instead of a fault.
        // ref on both models with `deler` absent: uit leeg, INCONSISTENT. Pinned bare AND wrapped, in that
        // order: an implementation that raises neither fault fails on the bare twin first.
        String verwacht = leeg("J1", "uit") + ", " + inconsistent();
        String invoer = inst("J1", "Jutter", "\"inkomen\": { \"number\": \"10.00\" }", "");
        assertNull(speel(delingModel(false), geval(invoer, verwacht)), "de kale deling door leeg");
        assertNull(speel(delingModel(true), geval(invoer, verwacht)), "dezelfde deling achter een Let");
    }

    @Test void een_letomhulde_wortel_van_negatief_is_even_inconsistent_als_de_kale() throws Exception {
        // §6.6: a negative radicand degrades to leeg AND deposits a Fout — the same shape as §6.5, carried by
        // verzamelWortels, and blind in the same way.
        // ref on both models with saldo = -4.00: uit leeg, INCONSISTENT.
        String verwacht = leeg("J1", "uit") + ", " + inconsistent();
        String invoer = inst("J1", "Jutter", "\"saldo\": { \"number\": \"-4.00\" }", "");
        assertNull(speel(wortelModel(false), geval(invoer, verwacht)), "de kale wortel van negatief");
        assertNull(speel(wortelModel(true), geval(invoer, verwacht)), "dezelfde wortel achter een Let");
    }

    // ------- 4. the other half: a companion the walk DOES build and the path probe must keep SILENT -------

    @Test void een_deling_in_de_niet_genomen_tak_levert_geen_fout() throws Exception {
        // ch. 10.2 evaluates EXACTLY the chosen branch. The DAN branch divides by `deler`, which is absent; the
        // condition is false (a = 1,00, not > 100), so that division never runs. The walk DOES descend into the
        // branch — the §9.5 companion for that division exists — and what keeps it silent is its path probe:
        // the companion re-evaluates the carrier (a ch. 10.2 Keuze is one) and deposits only when a §6.5 site
        // under it really ran. A companion deciding on the static "dividend present, divisor leeg" predicate
        // alone cannot see which branch ran and would answer INCONSISTENT where ref answers consistent.
        // ref on BOTH models with `deler` absent: uit = 7.0000, CONSISTENT. Pinned bare (the IfThenElse as the
        // rule value) AND inside a Let body, because the carrier hides the branches in both shapes.
        // NB what this method pins is the OUTCOME; it would also pass under a never-fabricating walk. The probe
        // MECHANISM is pinned by the sister {@code CompanionPadWitnessTest} —
        // een_deling_in_de_genomen_tak_is_inconsistent_en_in_de_niet_genomen_niet — which drives both flanks
        // through one and the same rule base, so only the path decides.
        String verwacht = waarde("J1", "uit", "7.0000") + ", " + consistent();
        String invoer = inst("J1", "Jutter",
                "\"a\": { \"number\": \"1.00\" }, \"inkomen\": { \"number\": \"10.00\" }", "");
        assertNull(speel(nietGenomenTakModel(false), geval(invoer, verwacht)), "de kale keuze");
        assertNull(speel(nietGenomenTakModel(true), geval(invoer, verwacht)), "dezelfde keuze in een Let-body");
    }

    @Test void een_deling_in_een_letbody_met_ongelezen_binder_levert_de_fout_wel() throws Exception {
        // ch. 10.6 under call-by-need (maintainer decision O3): "Let d = deler in inkomen gedeeld door tweede",
        // `deler` AND `tweede` absent. The binder `d` is never read, so clause 3 never evaluates `deler`, no
        // barrier is raised, and the body ALWAYS runs — its division by the absent `tweede` is real: a §6.5
        // fault. (Under the strict binding this chapter stated before, the leeg value ended the Let at the bind
        // place and the body never ran; that is what this witness pinned until the L-unification.) The bare twin
        // is the same division WITHOUT the Let — same fault — so an implementation that simply stopped
        // collecting divisions fails on both. Rewritten under maintainer decision 2026-09-01 (card
        // companion-onder-drager); the sister witness below pins the barrier that IS passed first.
        // ref (the L-ref, 2026-09-02): bare -> uit leeg + INCONSISTENT; in the Let body -> uit leeg + INCONSISTENT.
        String invoer = inst("J1", "Jutter", "\"inkomen\": { \"number\": \"10.00\" }", "");
        assertNull(speel(leegBarriereModel(false), geval(invoer, leeg("J1", "uit") + ", " + inconsistent())),
                "de kale deling door leeg");
        assertNull(speel(leegBarriereModel(true), geval(invoer, leeg("J1", "uit") + ", " + inconsistent())),
                "dezelfde deling in een Let-body waarvan de binder niet gelezen wordt");
    }

    @Test void een_deling_achter_een_gepasseerde_leeg_barriere_levert_geen_fout() throws Exception {
        // the sister: "Let d = deler in d plus (inkomen gedeeld door tweede)", `deler` and `tweede` absent. Now the
        // body READS `d` — and reads it FIRST (the left operand; ref evaluates a Binair left before right). That
        // read forces the leeg need, raises the barrier at the read (clause 2) and ends the Let before the
        // division is ever reached: no §6.5 fault, uit leeg. This is the ch. 10.6-L shape of "a barrier the
        // division sits behind" — the read place, not the bind place, is where the path is cut.
        // ref (the L-ref, 2026-09-02): uit leeg + CONSISTENT.
        String invoer = inst("J1", "Jutter", "\"inkomen\": { \"number\": \"10.00\" }", "");
        assertNull(speel(gepasseerdeBarriereModel(), geval(invoer, leeg("J1", "uit") + ", " + consistent())),
                "de deling achter de lezing die de barriere opwerpt");
    }

    @Test void een_breuk_achter_een_let_vergelijkt_exact_in_plaats_van_te_crashen() throws Exception {
        // "als (Let x = a gedeeld door 3 in x) groter is dan b plus 0,00 dan uit := 1,00", a = 10.00, b = 1.00.
        // 10/3 does not terminate, so the value side produces an EXACT Breuk. kanBreukOpleveren decides whether
        // the comparison routes through the Breuk-aware helper or through a raw (BigDecimal) cast; carrier-blind
        // it answers "no Breuk here" and the cast throws a ConstraintEvaluationException at fire — an
        // unexplained crash, which gate 12 counts as a not-carried form rather than a value.
        // ref on both models: uit = 1.00, consistent (10/3 = 3.33… > 1.00).
        String verwacht = waarde("J1", "uit", "1.00") + ", " + consistent();
        String invoer = inst("J1", "Jutter",
                "\"a\": { \"number\": \"10.00\" }, \"b\": { \"number\": \"1.00\" }", "");
        assertNull(speel(breukVergelijkingModel(false), geval(invoer, verwacht)), "de kale breuk-vergelijking");
        assertNull(speel(breukVergelijkingModel(true), geval(invoer, verwacht)), "dezelfde vergelijking achter een Let");
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. A form
     *  the backend refuses propagates as {@link CaseRunner.Excluded} — the loud channel two witnesses assert. */
    private static String speel(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("let-drager-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.speelCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static String geval(String instances, String verwacht) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [ %s ], "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(instances, verwacht);
    }

    private static String inst(String label, String objecttype, String attributen, String rollen) {
        return """
                { "label": "%s", "objectType": "%s", "attributes": { %s },
                  "roles": { %s }, "characteristics": [] }""".formatted(label, objecttype, attributen, rollen);
    }

    private static String waarde(String label, String attribuut, String getal) {
        return """
                { "kind": "attributeValue", "label": "%s", "attribute": "%s", "value": { "number": "%s" } }"""
                .formatted(label, attribuut, getal);
    }

    private static String leeg(String label, String attribuut) {
        return "{ \"kind\": \"attributeEmpty\", \"label\": \"%s\", \"attribute\": \"%s\" }"
                .formatted(label, attribuut);
    }

    private static String consistent() { return "{ \"kind\": \"consistent\" }"; }
    private static String inconsistent() { return "{ \"kind\": \"inconsistent\" }"; }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10 and its neighbours) ----------------

    private static final String BOOLEAAN = "{\"node\":\"Booleaan\"}";

    private static String num(int dec, boolean geheel, String teken, String eenheid) {
        return "{\"node\":\"Numeriek\",\"geheel\":%b,\"decimalen\":%d,\"teken\":\"%s\",\"eenheid\":%s}"
                .formatted(geheel, dec, teken, eenheid == null ? "null" : eenheid);
    }
    private static String num(int dec) { return num(dec, false, "ONBEPAALD", null); }
    private static String pos(int dec) { return num(dec, false, "NIET_NEGATIEF", null); }
    private static final String GEHEEL_POS = num(0, true, "NIET_NEGATIEF", null);

    /** One unit of the two-unit "Afstand" system below (canonical metre, factor to the canonical unit). */
    private static String eenheid(String afkorting, int factor) {
        return ("{\"node\":\"Eenheid\",\"afkorting\":\"%s\",\"stelsel\":\"Afstand\",\"canoniek\":\"m\","
                + "\"factorNaarCanoniek\":%d,\"noemer\":null}").formatted(afkorting, factor);
    }

    private static String attr(String objecttype, String naam, String type) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"%s\",\"onderwerpBron\":\"de %s\","
                + "\"navigatieRol\":null,\"type\":%s,\"bron\":\"het %s van de %s\",\"set\":false,\"navigatiePad\":[]}")
                .formatted(naam, objecttype, objecttype, type, naam, objecttype);
    }

    /** A read across a role — the shape whose reactivity {@code navRefs} is responsible for. */
    private static String navAttr(String objecttype, String naam, String type, String feittype, String rol) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"%s\",\"onderwerpBron\":\"zijn %s\","
                + "\"navigatieRol\":\"%s\",\"type\":%s,\"bron\":\"het %s van zijn %s\",\"set\":false,"
                + "\"navigatiePad\":[],\"navigatieBindingen\":[{\"node\":\"RolBinding\",\"feittype\":\"%s\","
                + "\"rol\":\"%s\"}]}").formatted(naam, objecttype, rol, rol, type, naam, rol, feittype, rol);
    }

    private static String lit(String waarde, String type) {
        return "{\"node\":\"Literal\",\"waarde\":\"%s\",\"type\":%s,\"bron\":\"%s\"}".formatted(waarde, type, waarde);
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

    /** The §9.2 stratification lever both walk witnesses use: a NEGATED kenmerk read whose kenmerk has a
     *  producer sinks the reading rule's salience below that producer's, so the producer fires LAST. */
    private static final String NIET_VERMIST =
            "{\"node\":\"Kenmerk\",\"kenmerk\":\"vermist\",\"navigatie\":null,\"negatie\":true,"
                    + "\"eigenaar\":null,\"periode\":null,\"bron\":\"hij niet vermist is\"}";
    private static final String VERMIST_DOEL =
            "{\"node\":\"AttribuutRef\",\"attribuut\":\"vermist\",\"objecttype\":\"Jutter\","
                    + "\"onderwerpBron\":\"Een Jutter\",\"navigatieRol\":null,\"type\":" + BOOLEAAN + ","
                    + "\"bron\":\"Een Jutter is vermist\",\"set\":false,\"navigatiePad\":[]}";
    private static final String WAAR =
            "{\"node\":\"Literal\",\"waarde\":\"waar\",\"type\":" + BOOLEAAN + ",\"bron\":\"waar\"}";

    private static String regel(String naam, String doel, String waarde) {
        return regel(naam, "BEREKENING", doel, waarde, "null");
    }
    private static String regel(String naam, String soort, String doel, String waarde, String voorwaarde) {
        return ("{\"node\":\"ResolvedRegel\",\"naam\":\"%s\",\"soort\":\"%s\",\"doel\":%s,\"waarde\":%s,"
                + "\"voorwaarde\":%s,\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(naam, soort, doel, waarde, voorwaarde);
    }

    private static String objecttype(String naam, String meervoud, String attributen,
            String kenmerken, String kenmerkSoorten) {
        return ("\"%s\":{\"node\":\"Objecttype\",\"naam\":\"%s\",\"attributen\":{%s},"
                + "\"attribuutMeervoud\":{},\"attribuutDimensies\":{},\"attribuutGranulariteit\":{},"
                + "\"attribuutTijdlijnAantal\":{},\"attribuutTijdlijnNaam\":{},\"kenmerken\":[%s],"
                + "\"kenmerkSoorten\":{%s},\"kenmerkGranulariteit\":{},\"kenmerkTijdlijnAantal\":{},"
                + "\"bezield\":true,\"meervoud\":\"%s\"}")
                .formatted(naam, naam, attributen, kenmerken, kenmerkSoorten, meervoud);
    }

    /** One document. {@code regelNamen} is the declaration list beside {@code regels} (the resolved bodies);
     *  {@code prioritizedDefaults} is appended verbatim (empty for the flat v0 shape). */
    private static String doc(String objecttypen, String feittypen, String eenheidstelsels,
            String[] regelNamen, String[] regels, String prioritizedDefaults) {
        StringBuilder namen = new StringBuilder();
        for (String n : regelNamen) {
            if (namen.length() > 0) namen.append(',');
            namen.append("{\"node\":\"Regel\",\"naam\":\"%s\",\"soort\":\"regel\"}".formatted(n));
        }
        return ("{\"formaat\":\"regelspraak-ir-v0\",\"objecttypen\":{%s},"
                + "\"domeinen\":{},\"parameters\":{},\"eenheidstelsels\":{%s},\"feittypen\":{%s},\"dimensies\":{},"
                + "\"tijdlijnen\":[],\"dagsoorten\":[],\"dagsoortPerEenheid\":{},\"regels\":[%s],"
                + "\"resolvedRegels\":[%s],\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],"
                + "\"consistenties\":[],\"uniciteiten\":[],\"uniciteitUnies\":[],\"feitCreaties\":[],"
                + "\"resolvedStartpunten\":[],\"regelGroepen\":[],\"diagnostics\":[]%s}")
                .formatted(objecttypen, eenheidstelsels, feittypen, namen, String.join(",", regels),
                        prioritizedDefaults);
    }

    /** A ch. 7 group over {@code Jutter.toeslag}: a justified first default with {@code consequentie}, then an
     *  unconditional 0. {@code ongeordend} makes a two-member set conflict-capable. */
    private static String defaultsGroep(String consequentie, String nulWaarde, boolean ongeordend) {
        return ("{\"node\":\"PrioritizedDefaults\",\"naam\":\"toeslag\",\"onderwerp\":\"Jutter\",\"kenmerk\":null,"
                + "\"attribuut\":\"toeslag\"%s,\"defaults\":["
                + "{\"node\":\"Default\",\"besluit\":null,\"voorwaarde\":%s,\"consequentie\":%s,\"bron\":\"toeslag\"},"
                + "{\"node\":\"Default\",\"besluit\":null,\"voorwaarde\":%s,\"consequentie\":%s,\"bron\":\"toeslag\"}],"
                + "\"bron\":\"toeslag\",\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(ongeordend ? ",\"ongeordend\":true" : "",
                        verg("GROTER_OF_GELIJK", attr("Jutter", "kindertal", GEHEEL_POS), lit("2", GEHEEL_POS)),
                        consequentie, ALTIJD, nulWaarde);
    }

    // ---------------- the models (each ref-replayed before its values were pinned) ----------------

    /** A ch. 10.2 branch guard whose two operands live in DIFFERENT units of one system — the restatement is
     *  what decides the branch, and the guard that refuses it is what keeps the wrong branch off the table. */
    private static String kruisEenheidModel() {
        String meter = num(0, true, "NIET_NEGATIEF", eenheid("m", 1));
        String kilometer = num(0, true, "NIET_NEGATIEF", eenheid("km", 1000));
        String stelsel = "\"Afstand\":{\"node\":\"Eenheidstelsel\",\"naam\":\"Afstand\",\"eenheden\":{"
                + "\"m\":" + eenheid("m", 1) + ",\"km\":" + eenheid("km", 1000) + "},\"aliassen\":{}}";
        String waarde = let("a", attr("Baan", "aanloop", meter),
                ite(verg("GROTER", bref("a", meter), lit("1", kilometer)),
                        lit("1.00", num(2)), lit("2.00", num(2)), num(2)), num(2));
        return doc(objecttype("Baan", "Baans", "\"aanloop\":%s,\"uit\":%s".formatted(meter, num(2)), "", ""),
                "", stelsel,
                new String[]{"uit van een baan"},
                new String[]{regel("uit van een baan", attr("Baan", "uit", num(2)), waarde)},
                "");
    }

    /** A ch. 7 defaults group whose consequence hides a §6.5 division behind a Let. */
    private static String defaultsDelingModel() {
        String consequentie = let("d", attr("Jutter", "deler", pos(2)),
                binair("GEDEELD_DOOR", attr("Jutter", "inkomen", pos(2)), bref("d", pos(2)), num(4)), num(4));
        return doc(objecttype("Jutter", "Jutters",
                        "\"kindertal\":%s,\"inkomen\":%s,\"deler\":%s,\"toeslag\":%s"
                                .formatted(GEHEEL_POS, pos(2), pos(2), num(4)), "", ""),
                "", "", new String[]{}, new String[]{},
                ",\"prioritizedDefaults\":[" + defaultsGroep(consequentie, lit("0.0000", num(4)), false) + "]");
    }

    /** A Let over a NAVIGATED read of an attribute whose producer is forced to fire last. */
    private static String verreReactiviteitModel() {
        String ver = navAttr("Jutter", "afgeleid", num(4), "strandmaatschap", "strandmaat");
        String feittype = "\"strandmaatschap\":{\"node\":\"Feittype\",\"naam\":\"strandmaatschap\","
                + "\"wederkerig\":true,\"rollen\":[{\"node\":\"Rol\",\"naam\":\"strandmaat\","
                + "\"objecttypeNaam\":\"Jutter\",\"meervoud\":\"strandmaten\",\"meervoudig\":false}]}";
        String uit = let("k", ver, binair("MAAL", bref("k", num(4)), lit("2.00", num(2)), num(6)), num(6));
        return doc(objecttype("Jutter", "Jutters",
                        "\"strandtegoed\":%s,\"afgeleid\":%s,\"uit\":%s".formatted(pos(2), num(4), num(6)),
                        "\"vermist\"", "\"vermist\":\"BIJVOEGLIJK\""),
                feittype, "",
                new String[]{"vermissing van een jutter", "afgeleid van een jutter", "uit van een jutter"},
                new String[]{
                        // never true on this input — its only job is to BE a producer of `vermist`
                        regel("vermissing van een jutter", "KENMERKTOEKENNING", VERMIST_DOEL, WAAR,
                                verg("GROTER", attr("Jutter", "strandtegoed", pos(2)), lit("999999.00", num(2)))),
                        regel("afgeleid van een jutter", "BEREKENING", attr("Jutter", "afgeleid", num(4)),
                                binair("MAAL", attr("Jutter", "strandtegoed", pos(2)), lit("3.00", num(2)), num(4)),
                                NIET_VERMIST),
                        regel("uit van een jutter", attr("Jutter", "uit", num(6)), uit)},
                "");
    }

    /** Two unordered ch. 7 defaults members over one target; member A reads a rule-derived attribute
     *  through a Let, and its producer is forced to fire last. */
    private static String defaultsSalienceModel() {
        String letLees = let("g", attr("Jutter", "grondslag", num(2)), bref("g", num(2)), num(2));
        String groepen = ",\"prioritizedDefaults\":["
                + defaultsGroep(letLees, lit("0.00", num(2)), true) + ","
                + defaultsGroep(lit("200.00", num(2)), lit("0.00", num(2)), true) + "]";
        return doc(objecttype("Jutter", "Jutters",
                        "\"kindertal\":%s,\"basis\":%s,\"grondslag\":%s,\"toeslag\":%s"
                                .formatted(GEHEEL_POS, pos(2), num(2), num(2)),
                        "\"vermist\"", "\"vermist\":\"BIJVOEGLIJK\""),
                "", "",
                new String[]{"vermissing van een jutter", "grondslag van een jutter"},
                new String[]{
                        regel("vermissing van een jutter", "KENMERKTOEKENNING", VERMIST_DOEL, WAAR,
                                verg("GROTER", attr("Jutter", "basis", pos(2)), lit("999999.00", num(2)))),
                        regel("grondslag van een jutter", "BEREKENING", attr("Jutter", "grondslag", num(2)),
                                binair("MAAL", attr("Jutter", "basis", pos(2)), lit("2.00", num(2)), num(2)),
                                NIET_VERMIST)},
                groepen);
    }

    /** §6.5: "het inkomen gedeeld door de deler", bare or behind a Let. */
    private static String delingModel(boolean achterLet) {
        String deling = binair("GEDEELD_DOOR", attr("Jutter", "inkomen", pos(2)),
                attr("Jutter", "deler", pos(2)), num(4));
        String waarde = achterLet ? let("t", deling, bref("t", num(4)), num(4)) : deling;
        return doc(objecttype("Jutter", "Jutters",
                        "\"inkomen\":%s,\"deler\":%s,\"uit\":%s".formatted(pos(2), pos(2), num(4)), "", ""),
                "", "", new String[]{"uit van een jutter"},
                new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), waarde)}, "");
    }

    /** §6.6: "de wortel van het saldo", bare or behind a Let. */
    private static String wortelModel(boolean achterLet) {
        String w = wortel(attr("Jutter", "saldo", num(2)), num(4));
        String waarde = achterLet ? let("t", w, bref("t", num(4)), num(4)) : w;
        return doc(objecttype("Jutter", "Jutters",
                        "\"saldo\":%s,\"uit\":%s".formatted(num(2), num(4)), "", ""),
                "", "", new String[]{"uit van een jutter"},
                new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), waarde)}, "");
    }

    /** ch. 10.2: a §6.5 division in the branch the condition does NOT take — the IfThenElse bare as the rule
     *  value, or wrapped in a Let whose binder feeds the condition. */
    private static String nietGenomenTakModel(boolean inLetBody) {
        String deling = binair("GEDEELD_DOOR", attr("Jutter", "inkomen", pos(2)),
                attr("Jutter", "deler", pos(2)), num(4));
        String links = inLetBody ? bref("a", pos(2)) : attr("Jutter", "a", pos(2));
        String keuze = ite(verg("GROTER", links, lit("100", GEHEEL_POS)),
                deling, lit("7.0000", num(4)), num(4));
        String waarde = inLetBody ? let("a", attr("Jutter", "a", pos(2)), keuze, num(4)) : keuze;
        return doc(objecttype("Jutter", "Jutters",
                        "\"a\":%s,\"inkomen\":%s,\"deler\":%s,\"uit\":%s"
                                .formatted(pos(2), pos(2), pos(2), num(4)), "", ""),
                "", "", new String[]{"uit van een jutter"},
                new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), waarde)}, "");
    }

    /** ch. 10.6: the same §6.5 division, bare or as the BODY of a Let whose leeg value is never read (so the
     *  body runs and the fault is real under call-by-need). */
    private static String leegBarriereModel(boolean achterBarriere) {
        String deling = binair("GEDEELD_DOOR", attr("Jutter", "inkomen", pos(2)),
                attr("Jutter", "tweede", pos(2)), num(4));
        String waarde = achterBarriere
                ? let("d", attr("Jutter", "deler", pos(2)), deling, num(4))
                : deling;
        return doc(objecttype("Jutter", "Jutters",
                        "\"inkomen\":%s,\"deler\":%s,\"tweede\":%s,\"uit\":%s"
                                .formatted(pos(2), pos(2), pos(2), num(4)), "", ""),
                "", "", new String[]{"uit van een jutter"},
                new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), waarde)}, "");
    }

    /** ch. 10.6 clause 2: the same division, but the body reads the leeg binder FIRST — the barrier is raised at
     *  that read and the division behind it is never reached. */
    private static String gepasseerdeBarriereModel() {
        String deling = binair("GEDEELD_DOOR", attr("Jutter", "inkomen", pos(2)),
                attr("Jutter", "tweede", pos(2)), num(4));
        String waarde = let("d", attr("Jutter", "deler", pos(2)),
                binair("PLUS", bref("d", pos(2)), deling, num(4)), num(4));
        return doc(objecttype("Jutter", "Jutters",
                        "\"inkomen\":%s,\"deler\":%s,\"tweede\":%s,\"uit\":%s"
                                .formatted(pos(2), pos(2), pos(2), num(4)), "", ""),
                "", "", new String[]{"uit van een jutter"},
                new String[]{regel("uit van een jutter", attr("Jutter", "uit", num(4)), waarde)}, "");
    }

    /** A non-terminating division as the LEFT operand of a rule's voorwaarde, bare or behind a Let. */
    private static String breukVergelijkingModel(boolean achterLet) {
        String deling = binair("GEDEELD_DOOR", attr("Jutter", "a", pos(2)), lit("3", GEHEEL_POS), num(6));
        String links = achterLet ? let("x", deling, bref("x", num(6)), num(6)) : deling;
        String conditie = verg("GROTER", links,
                binair("PLUS", attr("Jutter", "b", pos(2)), lit("0.00", num(2)), num(6)));
        return doc(objecttype("Jutter", "Jutters",
                        "\"a\":%s,\"b\":%s,\"uit\":%s".formatted(pos(2), pos(2), num(2)), "", ""),
                "", "", new String[]{"uit van een jutter"},
                new String[]{regel("uit van een jutter", "BEREKENING", attr("Jutter", "uit", num(2)),
                        lit("1.00", num(2)), conditie)}, "");
    }
}
