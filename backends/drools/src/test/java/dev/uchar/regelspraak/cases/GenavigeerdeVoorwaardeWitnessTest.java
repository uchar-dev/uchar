package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A voorwaarde that reads, through a to-one role, a value of the reached object that another rule DERIVES. The
 * exec-model branch put such a test on the subject's pattern, which Drools runs when the SUBJECT is inserted or
 * updated; the derivation updates the reached object, and nothing ran the test again. So the rule never fired, or
 * fired on the value the reached object held at insertion, while {@code ref/} and the ASP backend answer the
 * fixpoint (IR-SPEC ch. 5.5, the recorded Drools flank). A read of the same value in the rule's VALUE did react:
 * that side already had a far-side join. Every document here is authored in this class on Jutterswet material,
 * except the anchor {@code jutterswet-kustwacht}, which is read from the corpus and not written.
 *
 * <p><b>Red before the fix</b> (measured 2026-09-28 on the jar of the parent commit, exec-model branch): the
 * reproduction {@code attribuut uitkomst: verwacht 2026-03-01, werkelijk leeg}; the object creation {@code
 * attribuut meldingaantal: verwacht 1, werkelijk 0}; the fact creation the same; the division by an empty divisor
 * under the voorwaarde {@code verwacht inconsistent, werkelijk consistent} beside {@code verwacht 5.00, werkelijk
 * leeg}; the fired-rule marker under the voorwaarde {@code kenmerk afgehandeld: verwacht aanwezig, werkelijk
 * afwezig}; and the anchor {@code kenmerk rustwacht: verwacht afwezig, werkelijk aanwezig} in both its cases. The
 * division row and the marker row stay red with the host rule alone repaired: their companions repeat the host's
 * voorwaarde and need the same test. The row without a relation is a flank: green before and after, it holds the
 * null alternative of the far-side join. The row over a displaced role field pins the refusal the far-side join of
 * a characteristic check gives where the bean carries no reference field.
 */
class GenavigeerdeVoorwaardeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void de_regel_vuurt_zodra_de_afleiding_op_het_genavigeerde_object_er_is() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op de meldingsdatum van de jutter van de Vondst indien de
        // afgeleide datum van de jutter van de Vondst gevuld is", with "De afgeleide datum van een Jutter moet
        // gesteld worden op de meldingsdatum van de Jutter" before it. ref and clingo: uitkomst 01-03-2026.
        String ir = document(model(JUTTER_DATUMS, List.of(VONDST_UITKOMST), List.of(JUTTER_VONDST), ""),
                regels(AFGELEID, regel("uitkomst", "Vondst", "uitkomst", viaJutter("meldingsdatum"),
                        gevuld(viaJutter("afgeleide datum")))), "", "");
        assertNull(speel(ir, zaak(List.of(jutter("J", MELDING_MAART), vondst("V", "J", "")),
                        waarde("J", "afgeleide datum", "2026-03-01") + ", " + waarde("V", "uitkomst", "2026-03-01"))),
                "de voorwaarde ziet de afgeleide datum van de jutter");
    }

    @Test void zonder_relatie_en_zonder_afleiding_beslist_de_lege_lezing() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op 01-01-2026 indien de afgeleide datum van de jutter van
        // de Vondst leeg is". V1 has a jutter without meldingsdatum (the join alternative, the value stays leeg),
        // V2 has no jutter (the null alternative, RUNTIME-SEMANTICS ch. 5.8 rule 1: an absent link reads leeg).
        // ref and clingo: both 01-01-2026.
        String ir = document(model(JUTTER_DATUMS, List.of(VONDST_UITKOMST), List.of(JUTTER_VONDST), ""),
                regels(regel("uitkomst", "Vondst", "uitkomst", datum("2026-01-01"),
                        leeg(viaJutter("afgeleide datum"))), AFGELEID), "", "");
        assertNull(speel(ir, zaak(List.of(jutter("J", ""), vondst("V1", "J", ""), vondst("V2", null, "")),
                        waarde("V1", "uitkomst", "2026-01-01") + ", " + waarde("V2", "uitkomst", "2026-01-01"))),
                "beide alternatieven van de join vuren op de lege lezing");
    }

    @Test void de_deling_door_leeg_meldt_zich_onder_dezelfde_voorwaarde() throws Exception {
        // "De uitkomstwaarde van een Vondst moet berekend worden als het basisbedrag van de Vondst gedeeld door de
        // deler van de Vondst indien de afgeleide datum van de jutter van de Vondst gevuld is". The §6.5 companion
        // mirrors the host's voorwaarde, so it must see the derivation too. ref and clingo: with the deler empty
        // uitkomstwaarde leeg and inconsistent; with deler 2 uitkomstwaarde 5,00 and consistent.
        String ir = document(model(JUTTER_DATUMS, List.of(attribuut("basisbedrag", GEHEEL), attribuut("deler", GEHEEL),
                        attribuut("uitkomstwaarde", TWEE_DECIMALEN)), List.of(JUTTER_VONDST), ""),
                regels(AFGELEID, regel("uitkomstwaarde", "Vondst", "uitkomstwaarde",
                        "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + eigen("basisbedrag", "Vondst")
                                + ",\"right\":" + eigen("deler", "Vondst") + ",\"type\":{\"node\":\"Number\",\"whole\":false,"
                                + "\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"deling\"}}",
                        gevuld(viaJutter("afgeleide datum")))), "", "");
        assertNull(speel(ir, zaak(List.of(jutter("J", MELDING_MAART), vondst("V", "J", getal("basisbedrag", "10"))),
                        leegVerwacht("V", "uitkomstwaarde") + ", {\"kind\":\"inconsistent\"}")),
                "de deling door een lege deler");
        assertNull(speel(ir, zaak(List.of(jutter("J", MELDING_MAART),
                                vondst("V", "J", getal("basisbedrag", "10") + ", " + getal("deler", "2"))),
                        waarde("V", "uitkomstwaarde", null, "5.00") + ", {\"kind\":\"consistent\"}")),
                "de deling door 2");
    }

    @Test void de_gevuurd_begeleider_volgt_de_voorwaarde_van_zijn_gastregel() throws Exception {
        // "Een Vondst is afgehandeld indien regelversie uitkomst is gevuurd", over the reproduction's rule. The
        // marker is inserted by a companion that repeats the host's voorwaarde, so it must see the derivation as
        // the host does. V's uitkomst is given as the value the host writes, so the host's write changes nothing
        // and updates no fact: only the join on the jutter can run the companion's test again. No arbiter: ref
        // faults on 'is gevuurd' and the ASP backend does not translate it, so this row pins the Drools reading,
        // the host fired. Measured 2026-09-28: red on the jar of the claim and with the host alone repaired
        // (kenmerk afgehandeld: verwacht aanwezig, werkelijk afwezig), green once the companion takes the test.
        String model = "{\"objectTypes\":[" + JUTTER_DATUMS + ",{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
                + "\"plural\":\"Vondsten\",\"attributes\":[" + VONDST_UITKOMST + "],\"characteristics\":[{\"node\":"
                + "\"Characteristic\",\"name\":\"afgehandeld\",\"kind\":\"adjectival\"}]}],\"factTypes\":[" + JUTTER_VONDST
                + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        String afgehandeld = "{\"node\":\"Rule\",\"name\":\"afgehandeld\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afgehandeld\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"Een Vondst is afgehandeld\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":{\"node\":\"Regelversie\","
                + "\"regel\":\"uitkomst\",\"inconsistent\":false,\"source\":{\"snippet\":\"regelversie uitkomst is gevuurd\"}},"
                + "\"initialisation\":false}]}";
        String ir = document(model, regels(AFGELEID, regel("uitkomst", "Vondst", "uitkomst", viaJutter("meldingsdatum"),
                gevuld(viaJutter("afgeleide datum"))), afgehandeld), "", "");
        assertNull(speel(ir, zaak(List.of(jutter("J", MELDING_MAART), vondst("V", "J", "\"uitkomst\":{\"date\":\"2026-03-01\"}")),
                        waarde("V", "uitkomst", "2026-03-01") + ", {\"kind\":\"characteristicPresent\",\"label\":\"V\","
                        + "\"characteristic\":\"afgehandeld\"}")),
                "de gevuurd-markering onder de voorwaarde op de afgeleide datum");
    }

    @Test void een_objectcreatie_ziet_de_afgeleide_waarde() throws Exception {
        // "Een Vondst heeft een melding met meldingsuitkomst gelijk aan 01-01-2026 indien de afgeleide datum van
        // de jutter van de Vondst gevuld is", and "Het meldingaantal van een Vondst moet berekend worden als het
        // aantal meldingen van de Vondst", the derivation of the afgeleide datum declared LAST, so no update of
        // the Vondst after it can run the test again by accident. ref and clingo: meldingaantal 1.
        String ir = document(model(JUTTER_DATUMS, List.of(attribuut("meldingaantal", GEHEEL)),
                        List.of(JUTTER_VONDST, "{\"node\":\"FactType\",\"name\":\"VondstHeeftMelding\",\"reciprocal\":false,"
                                + "\"roles\":[{\"node\":\"Role\",\"name\":\"gemelde vondst\",\"objectType\":\"Vondst\",\"many\":false},"
                                + "{\"node\":\"Role\",\"name\":\"melding\",\"objectType\":\"Melding\",\"plural\":\"meldingen\","
                                + "\"many\":true}]}"),
                        ",{\"node\":\"ObjectType\",\"name\":\"Melding\",\"animate\":false,\"plural\":\"Meldingen\","
                                + "\"attributes\":[" + attribuut("meldingsuitkomst", DATUM) + "],\"characteristics\":[]}"),
                regels(regel("meldingaantal", "Vondst", "meldingaantal", "{\"node\":\"Aggregation\",\"kind\":\"count\","
                                + "\"arguments\":[" + eigen("meldingen", "Vondst") + "],\"elementType\":\"Melding\","
                                + "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"VondstHeeftMelding\",\"role\":\"melding\"},"
                                + "\"type\":" + GEHEEL_NIET_NEGATIEF + ",\"source\":{\"snippet\":\"het aantal meldingen\"}}", null),
                        AFGELEID),
                "\"objectCreations\":[{\"node\":\"ObjectCreation\",\"name\":\"maak een melding\",\"versions\":[{"
                        + "\"createdType\":\"Melding\",\"subject\":\"Vondst\",\"initialisations\":[{\"node\":\"MemberInit\","
                        + "\"member\":\"meldingsuitkomst\",\"value\":" + datum("2026-01-01") + ",\"characteristic\":false}],"
                        + "\"condition\":" + gevuld(viaJutter("afgeleide datum")) + ",\"validity\":{\"node\":\"Validity\"},"
                        + "\"connectingRole\":{\"node\":\"RoleBinding\",\"factType\":\"VondstHeeftMelding\",\"role\":\"melding\"}}]}],",
                "");
        assertNull(speel(ir, zaak(List.of(jutter("J", MELDING_MAART), vondst("V", "J", "")),
                        waarde("J", "afgeleide datum", "2026-03-01") + ", " + waarde("V", "meldingaantal", null, "1"))),
                "de creatie onder de voorwaarde op de afgeleide datum");
    }

    @Test void een_feitcreatie_ziet_de_afgeleide_waarde() throws Exception {
        // "Een aangemelde vondst van een Jutter is een vondst van de Jutter indien de afgeleide aanwijzing van de
        // strandvoogd van de Jutter gevuld is", "Het meldingaantal van een Jutter moet berekend worden als het
        // aantal aangemelde vondsten van de Jutter", and the derivation of the afgeleide aanwijzing last. ref and
        // clingo: meldingaantal 1.
        String strandvoogd = "{\"node\":\"ObjectType\",\"name\":\"Strandvoogd\",\"animate\":true,\"plural\":\"Strandvoogden\","
                + "\"attributes\":[" + attribuut("aanwijzingsdatum", DATUM) + "," + attribuut("afgeleide aanwijzing", DATUM)
                + "],\"characteristics\":[]}";
        String jutter = "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":["
                + attribuut("meldingaantal", GEHEEL) + "],\"characteristics\":[]}";
        String vondst = "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
                + attribuut("vinddag", DATUM) + "],\"characteristics\":[]}";
        String feittypen = feittype("Toezicht", "strandvoogd", "Strandvoogd", "jutter", "jutters", "Jutter") + ","
                + feittype("JutterHeeftVondst", "vinder", "Jutter", "vondst", "vondsten", "Vondst") + ","
                + feittype("Aanmelding", "aanmelder", "Jutter", "aangemelde vondst", "aangemelde vondsten", "Vondst");
        String model = "{\"objectTypes\":[" + strandvoogd + "," + jutter + "," + vondst + "],\"factTypes\":[" + feittypen
                + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        String opStrandvoogd = "{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide aanwijzing\",\"subjectType\":\"Strandvoogd\","
                + "\"source\":{\"snippet\":\"de afgeleide aanwijzing van de strandvoogd van de Jutter\"},\"path\":[{\"node\":\"RoleBinding\","
                + "\"factType\":\"Toezicht\",\"role\":\"strandvoogd\"}]}";
        String ir = document(model,
                regels(regel("meldingaantal", "Jutter", "meldingaantal", "{\"node\":\"Aggregation\",\"kind\":\"count\","
                                + "\"arguments\":[" + eigen("aangemelde vondsten", "Jutter") + "],\"elementType\":\"Vondst\","
                                + "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"Aanmelding\",\"role\":\"aangemelde vondst\"},"
                                + "\"type\":" + GEHEEL_NIET_NEGATIEF + ",\"source\":{\"snippet\":\"het aantal aangemelde vondsten\"}}", null),
                        regel("afgeleid", "Strandvoogd", "afgeleide aanwijzing", eigen("aanwijzingsdatum", "Strandvoogd"), null)),
                "",
                "\"factCreations\":[{\"node\":\"FactCreation\",\"name\":\"aanmelding\",\"versions\":[{\"factType\":\"Aanmelding\","
                        + "\"anchorRole\":\"aanmelder\",\"targetRole\":\"aangemelde vondst\",\"anchor\":\"Jutter\",\"path\":[{"
                        + "\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}],\"condition\":"
                        + gevuld(opStrandvoogd) + ",\"validity\":{\"node\":\"Validity\"}}]}],");
        String zaak = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + "{\"label\":\"S\",\"objectType\":\"Strandvoogd\",\"attributes\":{\"aanwijzingsdatum\":{\"date\":\"2026-02-01\"}},"
                + "\"roles\":{},\"characteristics\":[]},"
                + "{\"label\":\"J\",\"objectType\":\"Jutter\",\"attributes\":{},\"roles\":{\"strandvoogd\":[\"S\"]},\"characteristics\":[]},"
                + "{\"label\":\"V\",\"objectType\":\"Vondst\",\"attributes\":{},\"roles\":{\"vinder\":[\"J\"]},\"characteristics\":[]}],"
                + "\"parameters\":{}},\"expected\":[" + waarde("S", "afgeleide aanwijzing", "2026-02-01") + ", "
                + waarde("J", "meldingaantal", null, "1") + "]}";
        assertNull(speel(ir, zaak), "de feitcreatie onder de voorwaarde op de afgeleide aanwijzing");
    }

    @Test void een_genavigeerde_kenmerkcheck_over_een_verdrongen_rolveld_weigert() throws Exception {
        // synth-navigatiepremie with a Werf characteristic 'bedrijvig', a Schuit ATTRIBUTE named 'werf', which takes
        // the bean property the role 'werf' would have (FactModelGenerator.leden: attribute > characteristic >
        // reference), and the one rule "De toeslag van een Schuit moet gesteld worden op de lengte van de Schuit
        // indien zijn werf bedrijvig is". The far-side join of the check asks for the role's field and finds the
        // attribute's, so the rule is refused by name, as every other reader of that field is (RolveldlezerWitnessTest,
        // OpslagrichtingWitnessTest). Measured 2026-09-28: ref and clingo answer toeslag leeg; on the jar of the claim
        // the check read the attribute as the relation, an unclassified fault at fire once it is filled
        // (ERROR ConstraintEvaluationException).
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(
                Path.of("..", "..", "corpus", "golden", "synth-navigatiepremie", "ir.json")));
        IrPad.objectType(ir, "Werf").putArray("characteristics").addObject()
                .put("node", "Characteristic").put("name", "bedrijvig").put("kind", "adjectival");
        IrPad.addAttribute(IrPad.objectType(ir, "Schuit"), "werf", JSON.readTree(GEHEEL));
        ObjectNode toeslag = IrPad.vorm(ir, "rules", "toeslag van een schuit");
        IrPad.rules(ir).putArray("rules").add(toeslag);
        ObjectNode versie = IrPad.versie(ir, "rules", "toeslag van een schuit");
        versie.set("value", JSON.readTree(eigen("lengte", "Schuit")));
        versie.set("condition", JSON.readTree("{\"node\":\"CharacteristicCheck\",\"characteristic\":\"bedrijvig\","
                + "\"negated\":false,\"source\":{\"snippet\":\"zijn werf bedrijvig is\"},\"navigation\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"ligplaats\",\"role\":\"werf\"}}"));
        String zaak = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + "{\"label\":\"W1\",\"objectType\":\"Werf\",\"attributes\":{\"tarief\":{\"number\":\"312\"}},\"roles\":{},"
                + "\"characteristics\":[\"bedrijvig\"]},"
                + "{\"label\":\"S1\",\"objectType\":\"Schuit\",\"attributes\":{" + getal("lengte", "8") + ", "
                + getal("werf", "5") + "},\"roles\":{},\"characteristics\":[]}],\"parameters\":{}},"
                + "\"expected\":[" + leegVerwacht("S1", "toeslag") + "]}";
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> speel(ir.toString(), zaak));
        assertTrue(e.getMessage().contains("genavigeerde kenmerkcheck"), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'werf' op Schuit deelt haar naam met een gedeclareerd lid"),
                e.getMessage());
    }

    @Test void het_anker_kustwacht_speelt() throws Exception {
        // "Een Wachter is rustwacht indien zijn post geen risicopost is", where risicopost is itself derived
        // ("Een Post is risicopost indien zijn kustvak verraderlijk is"): a negated check on a derived
        // characteristic of the reached post. The stratum put the rule after its producer, but the test stayed the
        // one run at insertion, so rustwacht held for a wachter whose post became a risicopost.
        Path golden = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-kustwacht");
        for (String c : List.of("case1.json", "case2.json"))
            assertNull(CaseRunner.speelCase(golden.resolve("ir.json"), JSON.readTree(Files.readString(golden.resolve(c))), false),
                    "jutterswet-kustwacht " + c);
    }

    // ---------------- documents ----------------

    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String GEHEEL_NIET_NEGATIEF = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String MELDING_MAART = "\"meldingsdatum\":{\"date\":\"2026-03-01\"}";

    private static final String JUTTER_DATUMS = "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
            + "\"attributes\":[" + attribuut("meldingsdatum", DATUM) + "," + attribuut("afgeleide datum", DATUM)
            + "],\"characteristics\":[]}";
    private static final String VONDST_UITKOMST = attribuut("uitkomst", DATUM);
    private static final String JUTTER_VONDST = feittype("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst");
    /** "De afgeleide datum van een Jutter moet gesteld worden op de meldingsdatum van de Jutter." */
    private static final String AFGELEID = regel("afgeleid", "Jutter", "afgeleide datum", eigen("meldingsdatum", "Jutter"), null);

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String feittype(String naam, String rol1, String type1, String rol2, String meervoud2, String type2) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + rol1 + "\",\"objectType\":\"" + type1 + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + rol2 + "\",\"objectType\":\"" + type2 + "\",\"plural\":\"" + meervoud2
                + "\",\"many\":true}]}";
    }

    /** The Jutter type given, a Vondst with {@code vondstAttributen}, the fact types, and {@code meerTypen} (a leading
     *  comma and further object types, or empty). */
    private static String model(String jutter, List<String> vondstAttributen, List<String> feittypen, String meerTypen) {
        return "{\"objectTypes\":[" + jutter + ",{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
                + "\"plural\":\"Vondsten\",\"attributes\":[" + String.join(",", vondstAttributen) + "],\"characteristics\":[]}"
                + meerTypen + "],\"factTypes\":[" + String.join(",", feittypen) + "],\"parameters\":[],\"domains\":[],"
                + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
    }

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"de " + attribuut + "\"}}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst": one hop over JutterHeeftVondst. */
    private static String viaJutter(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"de " + attribuut + " van de jutter van de Vondst\"},\"path\":[{\"node\":\"RoleBinding\","
                + "\"factType\":\"JutterHeeftVondst\",\"role\":\"jutter\"}]}";
    }

    private static String datum(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATUM + ",\"source\":{\"snippet\":\"" + iso + "\"}}";
    }

    private static String gevuld(String operand) { return predicaat("filled", operand); }

    private static String leeg(String operand) { return predicaat("empty", operand); }

    private static String predicaat(String soort, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + soort + "\"}}";
    }

    /** One rule with one version; {@code voorwaarde} null for none. */
    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + doel + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde) + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    private static String regels(String... regels) { return String.join(",", regels); }

    /** {@code objectCreations} and {@code factCreations} are each empty, or the whole member with its trailing comma. */
    private static String document(String model, String regels, String objectCreations, String factCreations) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + regels + "],"
                + "\"dayKindDefinitions\":[],"
                + (objectCreations.isEmpty() ? "\"objectCreations\":[]," : objectCreations)
                + "\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + (factCreations.isEmpty() ? "\"factCreations\":[]," : factCreations)
                + "\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String jutter(String label, String attributen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Jutter\",\"attributes\":{" + attributen
                + "},\"roles\":{},\"characteristics\":[]}";
    }

    /** A Vondst whose jutter is {@code jutter}, or that has none when it is null. */
    private static String vondst(String label, String jutter, String attributen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{" + attributen + "},\"roles\":{"
                + (jutter == null ? "" : "\"jutter\":[\"" + jutter + "\"]") + "},\"characteristics\":[]}";
    }

    private static String getal(String attribuut, String waarde) {
        return "\"" + attribuut + "\":{\"number\":\"" + waarde + "\"}";
    }

    private static String waarde(String label, String attribuut, String datum) {
        return waarde(label, attribuut, datum, null);
    }

    /** An attributeValue expectation: a date when {@code datum} is set, else the number {@code getal}. */
    private static String waarde(String label, String attribuut, String datum, String getal) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":"
                + (datum != null ? "{\"date\":\"" + datum + "\"}" : "{\"number\":\"" + getal + "\"}") + "}";
    }

    private static String leegVerwacht(String label, String attribuut) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}";
    }

    private static String zaak(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("genavigeerde-voorwaarde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, false);
    }
}
