package io.github.yhavinga.regelspraak.cases;

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
 * {@link CaseRunner#speelCase} over models authored HERE (no corpus file is read, moved or touched).
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

    @Test void een_let_bindt_zijn_waarde_en_de_body_rekent_ermee() throws Exception {
        // ref on this model+case: basis = 1100.00 (Let ink = inkomen in ink plus 100,00, inkomen 1000.00).
        assertNull(speel(rekenModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }, \"kindertal\": { \"number\": \"7\" }",
                waarde("basis", "1100.00"))));
    }

    @Test void dezelfde_binder_twee_keer_gelezen_levert_twee_keer_de_waarde() throws Exception {
        // ref: dubbelop = 2000.00 (Let ink = inkomen in ink plus ink). A second read answering leeg would
        // coalesce to 0 inside plus and quietly write 1000.00 — this pin is what catches it.
        assertNull(speel(rekenModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }, \"kindertal\": { \"number\": \"7\" }",
                waarde("dubbelop", "2000.00"))));
    }

    // ---------------- 2. nested Lets: shadowing in, restoration out ----------------

    @Test void een_binnenste_binder_schaduwt_en_de_buitenste_geldt_er_weer_buiten() throws Exception {
        // geschaduwd := Let x = inkomen in (Let y = (Let x = 5,00 in x) in y plus x).
        // ref: 1005.00 — the INNER x wins inside its own body (5,00), the OUTER x is intact again outside it
        // (1000,00). Reading the outer binder inside would give 2000.00; binding the inner x IN PLACE of the
        // outer — the in-place-mutation bug — leaves x = 5,00 for the trailing read and gives 10.00.
        assertNull(speel(rekenModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }, \"kindertal\": { \"number\": \"7\" }",
                waarde("geschaduwd", "1005.00"))));
    }

    // ---------------- 3. IfThenElse in a Let body — both branches ----------------

    @Test void ifthenelse_in_een_letbody_kiest_de_danstak() throws Exception {
        // tak := Let a = inkomen in Let b = kindertal maal 100,00 in (als b groter dan a dan 1,00 anders 2,00);
        // both condition operands are BINDERS (WF-077). inkomen 100.00, kindertal 7 -> b=700.00 > a=100.00.
        // ref: tak = 1.00.
        assertNull(speel(rekenModel(), jutter("\"inkomen\": { \"number\": \"100.00\" }, \"kindertal\": { \"number\": \"7\" }",
                waarde("tak", "1.00"))));
    }

    @Test void ifthenelse_in_een_letbody_kiest_de_anderstak() throws Exception {
        // the other side of the same guard: inkomen 1000.00 -> b=700.00 is NOT > a=1000.00. ref: tak = 2.00.
        assertNull(speel(rekenModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }, \"kindertal\": { \"number\": \"7\" }",
                waarde("tak", "2.00"))));
    }

    // ---------------- 4. the leeg-barrier, beside its contrast ----------------

    @Test void een_lege_letwaarde_maakt_de_hele_let_leeg_terwijl_dezelfde_maal_zonder_let_nul_rekent() throws Exception {
        // ch. 10.6's normative contrast, in ONE model over ONE case with `inkomen` ABSENT:
        //   metlet   := Let ink = inkomen in ink maal 0,21  -> the body DOES run (ch. 10.6 is call-by-need), but
        //               its read of `ink` forces the leeg need and raises the barrier AT THAT READ (clause 2):
        //               the whole Let is leeg, so on this standing scalar target nothing is written (ref: leeg).
        //   zonderlet := inkomen maal 0,21                  -> the bare maal COALESCES leeg to 0 (ref: 0.0000).
        // A Let lowered as plain substitution would write 0.0000 on BOTH.
        assertNull(speel(barriereModel(), jutter("",
                "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"metlet\" }, "
                        + waarde("zonderlet", "0.0000"))));
    }

    @Test void de_barriere_is_geen_altijd_leeg_de_gevulde_invoer_rekent_wel() throws Exception {
        // the other side: with `inkomen` present BOTH compute, and to the same value — so the assertion above
        // is not passing on an arm that never writes a Let result at all. ref: 210.0000 each.
        assertNull(speel(barriereModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }",
                waarde("metlet", "210.0000") + ", " + waarde("zonderlet", "210.0000"))));
    }

    @Test void het_lege_doel_is_observeerbaar_afwezig_niet_slechts_ongeasserteerd() throws Exception {
        // the comparator must SEE the absence: the same case with a value expectation on `metlet` must MISMATCH.
        String fout = speel(barriereModel(), jutter("", waarde("metlet", "0.0000")));
        assertNotNull(fout, "de leeg-barriere moet observeerbaar zijn, niet slechts niet-geasserteerd");
        assertTrue(fout.contains("metlet"), "de mismatch noemt het doel: " + fout);
    }

    // ---------------- 5. a Let inside a prioritizedDefaults consequence ----------------

    @Test void een_let_binnen_een_defaultswaarde_beslist() throws Exception {
        // the ch. 7 group's first default is justified (kindertal >= 2) and its CONSEQUENCE is a Let over an
        // attribute: Let g = grondslag in g plus 100,00. ref: toeslag = 300.00 at grondslag 200.00.
        assertNull(speel(defaultsModel(), jutter("\"kindertal\": { \"number\": \"3\" }, \"grondslag\": { \"number\": \"200.00\" }",
                waarde("toeslag", "300.00"))));
    }

    @Test void een_ongerechtvaardigde_default_laat_de_letwaarde_ongemoeid() throws Exception {
        // justification false (kindertal 1 < 2): the group falls through to the unconditional 0,00, so the Let
        // value is never the decision. ref: toeslag = 0.00.
        assertNull(speel(defaultsModel(), jutter("\"kindertal\": { \"number\": \"1\" }, \"grondslag\": { \"number\": \"200.00\" }",
                waarde("toeslag", "0.00"))));
    }

    @Test void een_lege_letwaarde_in_een_default_valt_door_naar_de_volgende() throws Exception {
        // justified, but `grondslag` is ABSENT so the Let is leeg (the barrier again) — ch. 7's leeg
        // fall-through hands the decision to the next default. ref: toeslag = 0.00.
        assertNull(speel(defaultsModel(), jutter("\"kindertal\": { \"number\": \"3\" }",
                waarde("toeslag", "0.00"))));
    }

    // ---------------- 6. the reads a ch. 4.10 node hides: the freshness watch ----------------

    @Test void een_letgewikkelde_lezing_houdt_de_regel_reactief_op_zijn_producent() throws Exception {
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
        assertNull(speel(reactiviteitModel(), jutter("\"inkomen\": { \"number\": \"1000.00\" }",
                waarde("factor", "3.0000") + ", " + waarde("gewogen", "3000.000000"))));
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. */
    private static String speel(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("let-cluster-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.speelCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

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

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10) ----------------

    private static String num(int dec, boolean geheel, String teken) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(geheel, dec, teken);
    }
    private static String num(int dec) { return num(dec, false, "ONBEPAALD"); }

    private static String attr(String naam, int dec, boolean geheel, String teken) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"Jutter\","
                + "\"bron\":\"het %s van de Jutter\"}")
                .formatted(naam, naam);
    }

    private static String lit(String waarde, int dec, boolean geheel) {
        return "{\"node\":\"Literal\",\"waarde\":\"%s\",\"type\":%s,\"bron\":\"%s\"}"
                .formatted(waarde, num(dec, geheel, "ONBEPAALD"), waarde);
    }
    private static String lit(String waarde, int dec) { return lit(waarde, dec, false); }

    private static String binair(String op, String links, String rechts, int dec) {
        return "{\"node\":\"Binair\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"type\":%s,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, num(dec), op.toLowerCase());
    }

    /** ch. 4.10 the value-level binding. Its {@code type} is the BODY's type (WF-076). */
    private static String let(String binder, String value, String body, int dec) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"bron\":\"%s is …\"}"
                .formatted(num(dec), binder, value, body, binder);
    }

    /** ch. 4.10 a read of the nearest enclosing binder. NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String naam, int dec) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"bron\":\"%s\"}".formatted(naam, naam);
    }

    private static String ite(String conditie, String dan, String anders, int dec) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"bron\":\"als … dan … anders …\"}").formatted(num(dec), conditie, dan, anders);
    }

    private static String verg(String op, String links, String rechts) {
        return "{\"node\":\"Vergelijking\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"periode\":null,\"bron\":\"%s\"}"
                .formatted(op, links, rechts, op.toLowerCase());
    }

    private static String regel(String naam, String doel, String waarde) {
        return regel(naam, "BEREKENING", doel, waarde, "null");
    }
    private static String regel(String naam, String soort, String doel, String waarde, String voorwaarde) {
        return ("{\"node\":\"ResolvedRegel\",\"naam\":\"%s\",\"soort\":\"%s\",\"doel\":%s,\"waarde\":%s,"
                + "\"voorwaarde\":%s,\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(naam, soort, doel, waarde, voorwaarde);
    }

    /** A one-objecttype document: the declared attributes and kenmerken, the rules, and an optional defaults
     *  section. {@code attributen}/{@code kenmerken} are the bodies of the Jutter's two member lists (the
     *  kenmerken body is empty for a kenmerk-free model). */
    private static String model(String attributen, String kenmerken, String[] regels, String prioritizedDefaults) {
        return ("{\"format\":\"uchar-ir-v1\","
                + "\"model\":{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,"
                + "\"plural\":\"Jutters\",\"attributes\":[%s],\"characteristics\":[%s]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"resolvedRegels\":[%s],"
                + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
                + "\"uniciteiten\":[],\"uniciteitUnies\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
                + "\"regelGroepen\":[]%s},\"diagnostics\":[]}")
                .formatted(attributen, kenmerken, String.join(",", regels), prioritizedDefaults);
    }

    /** One entry of the Jutter's attribute list. */
    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(naam, datatype);
    }

    // ---------------- the three models (each verifier-clean and ref-replayed before pinning) ----------------

    /** Binding, reuse, shadowing and the branch guard on one Jutter. */
    private static String rekenModel() {
        String inkomen = attr("inkomen", 2, false, "NIET_NEGATIEF");
        String kindertal = attr("kindertal", 0, true, "NIET_NEGATIEF");
        String basis = let("ink", inkomen, binair("PLUS", bref("ink", 2), lit("100.00", 2), 2), 2);
        String dubbelop = let("ink", inkomen, binair("PLUS", bref("ink", 2), bref("ink", 2), 2), 2);
        String geschaduwd = let("x", inkomen,
                let("y", let("x", lit("5.00", 2), bref("x", 2), 2),
                        binair("PLUS", bref("y", 2), bref("x", 2), 2), 2), 2);
        String tak = let("a", inkomen,
                let("b", binair("MAAL", kindertal, lit("100.00", 2), 2),
                        ite(verg("GROTER", bref("b", 2), bref("a", 2)), lit("1.00", 2), lit("2.00", 2), 2), 2), 2);
        return model(String.join(",", attribuut("inkomen", num(2, false, "NIET_NEGATIEF")),
                        attribuut("kindertal", num(0, true, "NIET_NEGATIEF")), attribuut("basis", num(2)),
                        attribuut("dubbelop", num(2)), attribuut("geschaduwd", num(2)), attribuut("tak", num(2))), "",
                new String[]{regel("basis van een jutter", attr("basis", 2, false, "ONBEPAALD"), basis),
                        regel("dubbelop van een jutter", attr("dubbelop", 2, false, "ONBEPAALD"), dubbelop),
                        regel("geschaduwd van een jutter", attr("geschaduwd", 2, false, "ONBEPAALD"), geschaduwd),
                        regel("tak van een jutter", attr("tak", 2, false, "ONBEPAALD"), tak)},
                "");
    }

    /** ch. 10.6's contrast: the same product with and without the Let binding, on one subject. */
    private static String barriereModel() {
        String inkomen = attr("inkomen", 2, false, "NIET_NEGATIEF");
        return model(String.join(",", attribuut("inkomen", num(2, false, "NIET_NEGATIEF")),
                        attribuut("metlet", num(4)), attribuut("zonderlet", num(4))), "",
                new String[]{
                        regel("met let van een jutter", attr("metlet", 4, false, "ONBEPAALD"),
                                let("ink", inkomen, binair("MAAL", bref("ink", 2), lit("0.21", 2), 4), 4)),
                        regel("zonder let van een jutter", attr("zonderlet", 4, false, "ONBEPAALD"),
                                binair("MAAL", inkomen, lit("0.21", 2), 4))},
                "");
    }

    /** A ch. 7 prioritizedDefaults group whose first default's CONSEQUENCE is a Let over an attribute. */
    private static String defaultsModel() {
        String consequentie = let("g", attr("grondslag", 2, false, "NIET_NEGATIEF"),
                binair("PLUS", bref("g", 2), lit("100.00", 2), 2), 2);
        String groep = (",\"prioritizedDefaults\":[{\"node\":\"PrioritizedDefaults\",\"naam\":\"toeslag\","
                + "\"onderwerp\":\"Jutter\",\"kenmerk\":null,\"attribuut\":\"toeslag\",\"defaults\":["
                + "{\"node\":\"Default\",\"besluit\":null,\"voorwaarde\":%s,\"consequentie\":%s,\"bron\":\"toeslag\"},"
                + "{\"node\":\"Default\",\"besluit\":null,\"voorwaarde\":"
                + "{\"node\":\"Samengesteld\",\"kwantor\":\"ALLE\",\"drempel\":0,\"onderdelen\":[],\"bron\":\"altijd\"},"
                + "\"consequentie\":%s,\"bron\":\"toeslag\"}],"
                + "\"bron\":\"toeslag\",\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}]")
                .formatted(verg("GROTER_OF_GELIJK", attr("kindertal", 0, true, "NIET_NEGATIEF"), lit("2", 0, true)),
                        consequentie, lit("0.00", 2));
        return model(String.join(",", attribuut("kindertal", num(0, true, "NIET_NEGATIEF")),
                        attribuut("grondslag", num(2, false, "NIET_NEGATIEF")), attribuut("toeslag", num(2))), "",
                new String[]{}, groep);
    }

    /** A Let-wrapped read of an attribute whose producer is FORCED to fire later (§9.2 stratification: its
     *  rule reads a negated kenmerk that has a producer, so its salience is -1). Only a freshness watch that
     *  descends the Let keeps the consumer reactive on that producer's write. */
    private static String reactiviteitModel() {
        String inkomen = attr("inkomen", 2, false, "NIET_NEGATIEF");
        String vermistDoel = ("{\"node\":\"AttribuutRef\",\"attribuut\":\"vermist\",\"objecttype\":\"Jutter\","
                + "\"bron\":\"Een Jutter is vermist\"}");
        String waar = "{\"node\":\"Literal\",\"waarde\":\"waar\",\"type\":{\"node\":\"Booleaan\"},\"bron\":\"waar\"}";
        String nietVermist = ("{\"node\":\"Kenmerk\",\"kenmerk\":\"vermist\",\"negatie\":true,"
                + "\"eigenaar\":null,\"periode\":null,\"bron\":\"hij niet vermist is\"}");
        String gewogen = let("x", inkomen,
                binair("MAAL", bref("x", 2), attr("factor", 4, false, "ONBEPAALD"), 6), 6);
        return model(String.join(",", attribuut("inkomen", num(2, false, "NIET_NEGATIEF")),
                        attribuut("factor", num(4)), attribuut("gewogen", num(6))),
                "{\"node\":\"Characteristic\",\"name\":\"vermist\",\"kind\":\"BIJVOEGLIJK\"}",
                new String[]{
                        // never true for the case input — its only job is to BE a producer of `vermist`, which
                        // is what sinks the factor rule's stratum below this consumer's.
                        regel("vermissing van een jutter", "KENMERKTOEKENNING", vermistDoel, waar,
                                verg("GROTER", inkomen, lit("999999.00", 2))),
                        regel("factor van een jutter", "BEREKENING", attr("factor", 4, false, "ONBEPAALD"),
                                lit("3.0000", 4), nietVermist),
                        regel("gewogen van een jutter", attr("gewogen", 6, false, "ONBEPAALD"), gewogen)},
                "");
    }
}
