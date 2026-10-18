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
 * a characteristic check gives where the bean carries no reference field. Since card tms-omschakeling (2026-10-10) the
 * marker row and that refusal run on the exec-model branch with truth maintenance, which refuses the fired-rule marker
 * by name.
 */
class NavigatedConditionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void the_rule_fires_once_the_derivation_on_the_navigated_object_is_there() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op de meldingsdatum van de jutter van de Vondst indien de
        // afgeleide datum van de jutter van de Vondst gevuld is", with "De afgeleide datum van een Jutter moet
        // gesteld worden op de meldingsdatum van de Jutter" before it. ref and clingo: uitkomst 01-03-2026.
        String ir = document(model(JUTTER_DATES, List.of(VONDST_OUTCOME), List.of(JUTTER_VONDST), ""),
                rules(DERIVED, regel("uitkomst", "Vondst", "uitkomst", viaJutter("meldingsdatum"),
                        filled(viaJutter("afgeleide datum")))), "", "");
        assertNull(play(ir, caseDocument(List.of(jutter("J", MESSAGE_MARCH), vondst("V", "J", "")),
                        value("J", "afgeleide datum", "2026-03-01") + ", " + value("V", "uitkomst", "2026-03-01"))),
                "de voorwaarde ziet de afgeleide datum van de jutter");
    }

    @Test void without_relation_and_without_derivation_decides_the_empty_read() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op 01-01-2026 indien de afgeleide datum van de jutter van
        // de Vondst leeg is". V1 has a jutter without meldingsdatum (the join alternative, the value stays leeg),
        // V2 has no jutter (the null alternative, RUNTIME-SEMANTICS ch. 5.8 rule 1: an absent link reads leeg).
        // ref and clingo: both 01-01-2026.
        String ir = document(model(JUTTER_DATES, List.of(VONDST_OUTCOME), List.of(JUTTER_VONDST), ""),
                rules(regel("uitkomst", "Vondst", "uitkomst", date("2026-01-01"),
                        empty(viaJutter("afgeleide datum"))), DERIVED), "", "");
        assertNull(play(ir, caseDocument(List.of(jutter("J", ""), vondst("V1", "J", ""), vondst("V2", null, "")),
                        value("V1", "uitkomst", "2026-01-01") + ", " + value("V2", "uitkomst", "2026-01-01"))),
                "beide alternatieven van de join vuren op de lege lezing");
    }

    @Test void the_division_by_empty_reports_itself_under_the_same_condition() throws Exception {
        // "De uitkomstwaarde van een Vondst moet berekend worden als het basisbedrag van de Vondst gedeeld door de
        // deler van de Vondst indien de afgeleide datum van de jutter van de Vondst gevuld is". The §6.5 companion
        // mirrors the host's voorwaarde, so it must see the derivation too. ref and clingo: with the deler empty
        // uitkomstwaarde leeg and inconsistent; with deler 2 uitkomstwaarde 5,00 and consistent.
        String ir = document(model(JUTTER_DATES, List.of(attribute("basisbedrag", WHOLE), attribute("deler", WHOLE),
                        attribute("uitkomstwaarde", TWO_DECIMALS)), List.of(JUTTER_VONDST), ""),
                rules(DERIVED, regel("uitkomstwaarde", "Vondst", "uitkomstwaarde",
                        "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + own("basisbedrag", "Vondst")
                                + ",\"right\":" + own("deler", "Vondst") + ",\"type\":{\"node\":\"Number\",\"whole\":false,"
                                + "\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"deling\"}}",
                        filled(viaJutter("afgeleide datum")))), "", "");
        assertNull(play(ir, caseDocument(List.of(jutter("J", MESSAGE_MARCH), vondst("V", "J", number("basisbedrag", "10"))),
                        emptyExpected("V", "uitkomstwaarde") + ", {\"kind\":\"inconsistent\"}")),
                "de deling door een lege deler");
        assertNull(play(ir, caseDocument(List.of(jutter("J", MESSAGE_MARCH),
                                vondst("V", "J", number("basisbedrag", "10") + ", " + number("deler", "2"))),
                        value("V", "uitkomstwaarde", null, "5.00") + ", {\"kind\":\"consistent\"}")),
                "de deling door 2");
    }

    @Test void a_fired_condition_is_refused_by_name() throws Exception {
        // "Een Vondst is afgehandeld indien regelversie uitkomst is gevuurd", over the reproduction's rule. No arbiter:
        // ref faults on 'is gevuurd' and the ASP backend does not translate it. The destructive exec-model branch
        // carried it with a companion that repeated the host's voorwaarde, and this row pinned that reading, the host
        // fired (V afgehandeld). The exec-model branch refuses the predicate by name: it is no form of the
        // kernel but a predicate of the RegelSpraak dialect over the fixpoint (RUNTIME-SEMANTICS ch. 5.6, CONSTRUCTS
        // row 30). Coverage lost against the destructive branch, accepted (card tms-omschakeling).
        String model = "{\"objectTypes\":[" + JUTTER_DATES + ",{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
                + "\"plural\":\"Vondsten\",\"attributes\":[" + VONDST_OUTCOME + "],\"characteristics\":[{\"node\":"
                + "\"Characteristic\",\"name\":\"afgehandeld\",\"kind\":\"adjectival\"}]}],\"factTypes\":[" + JUTTER_VONDST
                + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        String afgehandeld = "{\"node\":\"Rule\",\"name\":\"afgehandeld\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afgehandeld\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"Een Vondst is afgehandeld\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":{\"node\":\"Regelversie\","
                + "\"regel\":\"uitkomst\",\"inconsistent\":false,\"source\":{\"snippet\":\"regelversie uitkomst is gevuurd\"}},"
                + "\"initialisation\":false}]}";
        String ir = document(model, rules(DERIVED, regel("uitkomst", "Vondst", "uitkomst", viaJutter("meldingsdatum"),
                filled(viaJutter("afgeleide datum"))), afgehandeld), "", "");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> playTms(ir, caseDocument(List.of(jutter("J",
                MESSAGE_MARCH), vondst("V", "J", "\"uitkomst\":{\"date\":\"2026-03-01\"}")), value("V", "uitkomst",
                "2026-03-01") + ", {\"kind\":\"characteristicPresent\",\"label\":\"V\",\"characteristic\":\"afgehandeld\"}")));
        assertTrue(e.getMessage().contains("regel: afgehandeld — een Regelversie 'is gevuurd' in de voorwaarde"
                + " ('regelversie uitkomst is gevuurd'): geen vorm van de kern maar een predicaat van het"
                + " RegelSpraak-dialect over het fixpunt (RUNTIME-SEMANTICS ch. 5.6, CONSTRUCTS rij 30)"), e.getMessage());
    }

    @Test void an_object_creation_sees_the_derived_value() throws Exception {
        // "Een Vondst heeft een melding met meldingsuitkomst gelijk aan 01-01-2026 indien de afgeleide datum van
        // de jutter van de Vondst gevuld is", and "Het meldingaantal van een Vondst moet berekend worden als het
        // aantal meldingen van de Vondst", the derivation of the afgeleide datum declared LAST, so no update of
        // the Vondst after it can run the test again by accident. ref and clingo: meldingaantal 1.
        String ir = document(model(JUTTER_DATES, List.of(attribute("meldingaantal", WHOLE)),
                        List.of(JUTTER_VONDST, "{\"node\":\"FactType\",\"name\":\"VondstHeeftMelding\",\"reciprocal\":false,"
                                + "\"roles\":[{\"node\":\"Role\",\"name\":\"gemelde vondst\",\"objectType\":\"Vondst\",\"many\":false},"
                                + "{\"node\":\"Role\",\"name\":\"melding\",\"objectType\":\"Melding\",\"plural\":\"meldingen\","
                                + "\"many\":true}]}"),
                        ",{\"node\":\"ObjectType\",\"name\":\"Melding\",\"animate\":false,\"plural\":\"Meldingen\","
                                + "\"attributes\":[" + attribute("meldingsuitkomst", DATE) + "],\"characteristics\":[]}"),
                rules(regel("meldingaantal", "Vondst", "meldingaantal", "{\"node\":\"Aggregation\",\"kind\":\"count\","
                                + "\"arguments\":[" + own("meldingen", "Vondst") + "],\"elementType\":\"Melding\","
                                + "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"VondstHeeftMelding\",\"role\":\"melding\"},"
                                + "\"type\":" + WHOLE_NON_NEGATIVE + ",\"source\":{\"snippet\":\"het aantal meldingen\"}}", null),
                        DERIVED),
                "\"objectCreations\":[{\"node\":\"ObjectCreation\",\"name\":\"maak een melding\",\"versions\":[{"
                        + "\"createdType\":\"Melding\",\"subject\":\"Vondst\",\"initialisations\":[{\"node\":\"MemberInit\","
                        + "\"member\":\"meldingsuitkomst\",\"value\":" + date("2026-01-01") + ",\"characteristic\":false}],"
                        + "\"condition\":" + filled(viaJutter("afgeleide datum")) + ",\"validity\":{\"node\":\"Validity\"},"
                        + "\"connectingRole\":{\"node\":\"RoleBinding\",\"factType\":\"VondstHeeftMelding\",\"role\":\"melding\"}}]}],",
                "");
        assertNull(play(ir, caseDocument(List.of(jutter("J", MESSAGE_MARCH), vondst("V", "J", "")),
                        value("J", "afgeleide datum", "2026-03-01") + ", " + value("V", "meldingaantal", null, "1"))),
                "de creatie onder de voorwaarde op de afgeleide datum");
    }

    @Test void a_fact_creation_sees_the_derived_value() throws Exception {
        // "Een aangemelde vondst van een Jutter is een vondst van de Jutter indien de afgeleide aanwijzing van de
        // strandvoogd van de Jutter gevuld is", "Het meldingaantal van een Jutter moet berekend worden als het
        // aantal aangemelde vondsten van de Jutter", and the derivation of the afgeleide aanwijzing last. ref and
        // clingo: meldingaantal 1.
        String strandvoogd = "{\"node\":\"ObjectType\",\"name\":\"Strandvoogd\",\"animate\":true,\"plural\":\"Strandvoogden\","
                + "\"attributes\":[" + attribute("aanwijzingsdatum", DATE) + "," + attribute("afgeleide aanwijzing", DATE)
                + "],\"characteristics\":[]}";
        String jutter = "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":["
                + attribute("meldingaantal", WHOLE) + "],\"characteristics\":[]}";
        String vondst = "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
                + attribute("vinddag", DATE) + "],\"characteristics\":[]}";
        String factTypes = factType("Toezicht", "strandvoogd", "Strandvoogd", "jutter", "jutters", "Jutter") + ","
                + factType("JutterHeeftVondst", "vinder", "Jutter", "vondst", "vondsten", "Vondst") + ","
                + factType("Aanmelding", "aanmelder", "Jutter", "aangemelde vondst", "aangemelde vondsten", "Vondst");
        String model = "{\"objectTypes\":[" + strandvoogd + "," + jutter + "," + vondst + "],\"factTypes\":[" + factTypes
                + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        String opStrandvoogd = "{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide aanwijzing\",\"subjectType\":\"Strandvoogd\","
                + "\"source\":{\"snippet\":\"de afgeleide aanwijzing van de strandvoogd van de Jutter\"},\"path\":[{\"node\":\"RoleBinding\","
                + "\"factType\":\"Toezicht\",\"role\":\"strandvoogd\"}]}";
        String ir = document(model,
                rules(regel("meldingaantal", "Jutter", "meldingaantal", "{\"node\":\"Aggregation\",\"kind\":\"count\","
                                + "\"arguments\":[" + own("aangemelde vondsten", "Jutter") + "],\"elementType\":\"Vondst\","
                                + "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"Aanmelding\",\"role\":\"aangemelde vondst\"},"
                                + "\"type\":" + WHOLE_NON_NEGATIVE + ",\"source\":{\"snippet\":\"het aantal aangemelde vondsten\"}}", null),
                        regel("afgeleid", "Strandvoogd", "afgeleide aanwijzing", own("aanwijzingsdatum", "Strandvoogd"), null)),
                "",
                "\"factCreations\":[{\"node\":\"FactCreation\",\"name\":\"aanmelding\",\"versions\":[{\"factType\":\"Aanmelding\","
                        + "\"anchorRole\":\"aanmelder\",\"targetRole\":\"aangemelde vondst\",\"anchor\":\"Jutter\",\"path\":[{"
                        + "\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}],\"condition\":"
                        + filled(opStrandvoogd) + ",\"validity\":{\"node\":\"Validity\"}}]}],");
        String caseDocument = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + "{\"label\":\"S\",\"objectType\":\"Strandvoogd\",\"attributes\":{\"aanwijzingsdatum\":{\"date\":\"2026-02-01\"}},"
                + "\"roles\":{},\"characteristics\":[]},"
                + "{\"label\":\"J\",\"objectType\":\"Jutter\",\"attributes\":{},\"roles\":{\"strandvoogd\":[\"S\"]},\"characteristics\":[]},"
                + "{\"label\":\"V\",\"objectType\":\"Vondst\",\"attributes\":{},\"roles\":{\"vinder\":[\"J\"]},\"characteristics\":[]}],"
                + "\"parameters\":{}},\"expected\":[" + value("S", "afgeleide aanwijzing", "2026-02-01") + ", "
                + value("J", "meldingaantal", null, "1") + "]}";
        assertNull(play(ir, caseDocument), "de feitcreatie onder de voorwaarde op de afgeleide aanwijzing");
    }

    @Test void a_navigated_characteristic_check_over_a_displaced_role_field_is_refused() throws Exception {
        // synth-navigatiepremie with a Werf characteristic 'bedrijvig', a Schuit ATTRIBUTE named 'werf', which takes
        // the bean property the role 'werf' would have (FactModelGenerator.members: attribute > characteristic >
        // reference), and the one rule "De toeslag van een Schuit moet gesteld worden op de lengte van de Schuit
        // indien zijn werf bedrijvig is". The far-side join of the check asks for the role's field and finds the
        // attribute's, so the rule is refused by name, as every other reader of that field is (RoleFieldReaderWitnessTest,
        // OpslagDirectionWitnessTest). Measured 2026-09-28: ref and clingo answer toeslag leeg; on the jar of the claim
        // the check read the attribute as the relation, an unclassified fault at fire once it is filled
        // (ERROR ConstraintEvaluationException). On the exec-model branch, whose sentence names the missing
        // reference field.
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(
                Path.of("..", "..", "corpus", "golden", "synth-navigatiepremie", "ir.json")));
        IrPath.objectType(ir, "Werf").putArray("characteristics").addObject()
                .put("node", "Characteristic").put("name", "bedrijvig").put("kind", "adjectival");
        IrPath.addAttribute(IrPath.objectType(ir, "Schuit"), "werf", JSON.readTree(WHOLE));
        ObjectNode toeslag = IrPath.form(ir, "rules", "toeslag van een schuit");
        IrPath.rules(ir).putArray("rules").add(toeslag);
        ObjectNode version = IrPath.version(ir, "rules", "toeslag van een schuit");
        version.set("value", JSON.readTree(own("lengte", "Schuit")));
        version.set("condition", JSON.readTree("{\"node\":\"CharacteristicCheck\",\"characteristic\":\"bedrijvig\","
                + "\"negated\":false,\"source\":{\"snippet\":\"zijn werf bedrijvig is\"},\"navigation\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"ligplaats\",\"role\":\"werf\"}}"));
        String caseDocument = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + "{\"label\":\"W1\",\"objectType\":\"Werf\",\"attributes\":{\"tarief\":{\"number\":\"312\"}},\"roles\":{},"
                + "\"characteristics\":[\"bedrijvig\"]},"
                + "{\"label\":\"S1\",\"objectType\":\"Schuit\",\"attributes\":{" + number("lengte", "8") + ", "
                + number("werf", "5") + "},\"roles\":{},\"characteristics\":[]}],\"parameters\":{}},"
                + "\"expected\":[" + emptyExpected("S1", "toeslag") + "]}";
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> playTms(ir.toString(), caseDocument));
        assertTrue(e.getMessage().contains("regel: toeslag van een schuit — de kenmerkcheck 'zijn werf bedrijvig is':"
                + " Schuit heeft geen referentieveld 'werf' naar Werf (§13.3.9 opslagrichting)"), e.getMessage());
    }

    @Test void the_anchor_kustwacht_plays() throws Exception {
        // "Een Wachter is rustwacht indien zijn post geen risicopost is", where risicopost is itself derived
        // ("Een Post is risicopost indien zijn kustvak verraderlijk is"): a negated check on a derived
        // characteristic of the reached post. The stratum put the rule after its producer, but the test stayed the
        // one run at insertion, so rustwacht held for a wachter whose post became a risicopost.
        Path golden = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-kustwacht");
        for (String c : List.of("case1.json", "case2.json"))
            assertNull(CaseRunner.playCase(golden.resolve("ir.json"), JSON.readTree(Files.readString(golden.resolve(c))), false),
                    "jutterswet-kustwacht " + c);
    }

    // ---------------- documents ----------------

    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String WHOLE_NON_NEGATIVE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String MESSAGE_MARCH = "\"meldingsdatum\":{\"date\":\"2026-03-01\"}";

    private static final String JUTTER_DATES = "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
            + "\"attributes\":[" + attribute("meldingsdatum", DATE) + "," + attribute("afgeleide datum", DATE)
            + "],\"characteristics\":[]}";
    private static final String VONDST_OUTCOME = attribute("uitkomst", DATE);
    private static final String JUTTER_VONDST = factType("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst");
    /** "De afgeleide datum van een Jutter moet gesteld worden op de meldingsdatum van de Jutter." */
    private static final String DERIVED = regel("afgeleid", "Jutter", "afgeleide datum", own("meldingsdatum", "Jutter"), null);

    private static String attribute(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String factType(String naam, String role1, String type1, String role2, String plural2, String type2) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + role1 + "\",\"objectType\":\"" + type1 + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + role2 + "\",\"objectType\":\"" + type2 + "\",\"plural\":\"" + plural2
                + "\",\"many\":true}]}";
    }

    /** The Jutter type given, a Vondst with {@code vondstAttributes}, the fact types, and {@code moreTypes} (a leading
     *  comma and further object types, or empty). */
    private static String model(String jutter, List<String> vondstAttributes, List<String> factTypes, String moreTypes) {
        return "{\"objectTypes\":[" + jutter + ",{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
                + "\"plural\":\"Vondsten\",\"attributes\":[" + String.join(",", vondstAttributes) + "],\"characteristics\":[]}"
                + moreTypes + "],\"factTypes\":[" + String.join(",", factTypes) + "],\"parameters\":[],\"domains\":[],"
                + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
    }

    private static String own(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"de " + attribute + "\"}}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst": one hop over JutterHeeftVondst. */
    private static String viaJutter(String attribute) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"de " + attribute + " van de jutter van de Vondst\"},\"path\":[{\"node\":\"RoleBinding\","
                + "\"factType\":\"JutterHeeftVondst\",\"role\":\"jutter\"}]}";
    }

    private static String date(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATE + ",\"source\":{\"snippet\":\"" + iso + "\"}}";
    }

    private static String filled(String operand) { return predicate("filled", operand); }

    private static String empty(String operand) { return predicate("empty", operand); }

    private static String predicate(String category, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + category + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + category + "\"}}";
    }

    /** One rule with one version; {@code condition} null for none. */
    private static String regel(String naam, String type, String target, String value, String condition) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + target + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + target + "\"}},\"value\":" + value
                + (condition == null ? "" : ",\"condition\":" + condition) + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    private static String rules(String... rules) { return String.join(",", rules); }

    /** {@code objectCreations} and {@code factCreations} are each empty, or the whole member with its trailing comma. */
    private static String document(String model, String rules, String objectCreations, String factCreations) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + rules + "],"
                + "\"dayKindDefinitions\":[],"
                + (objectCreations.isEmpty() ? "\"objectCreations\":[]," : objectCreations)
                + "\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + (factCreations.isEmpty() ? "\"factCreations\":[]," : factCreations)
                + "\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String jutter(String label, String attributes) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Jutter\",\"attributes\":{" + attributes
                + "},\"roles\":{},\"characteristics\":[]}";
    }

    /** A Vondst whose jutter is {@code jutter}, or that has none when it is null. */
    private static String vondst(String label, String jutter, String attributes) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{" + attributes + "},\"roles\":{"
                + (jutter == null ? "" : "\"jutter\":[\"" + jutter + "\"]") + "},\"characteristics\":[]}";
    }

    private static String number(String attribute, String value) {
        return "\"" + attribute + "\":{\"number\":\"" + value + "\"}";
    }

    private static String value(String label, String attribute, String date) {
        return value(label, attribute, date, null);
    }

    /** An attributeValue expectation: a date when {@code datum} is set, else the number {@code number}. */
    private static String value(String label, String attribute, String date, String number) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute + "\",\"value\":"
                + (date != null ? "{\"date\":\"" + date + "\"}" : "{\"number\":\"" + number + "\"}") + "}";
    }

    private static String emptyExpected(String label, String attribute) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute + "\"}";
    }

    private static String caseDocument(List<String> instances, String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances)
                + "],\"parameters\":{}},\"expected\":[" + expected + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("genavigeerde-voorwaarde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, false);
    }

    private static String playTms(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("genavigeerde-voorwaarde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.playCase(tmp, JSON.readTree(caseDocument), DroolsBackend.Branch.EXEC_MODEL);
    }
}
