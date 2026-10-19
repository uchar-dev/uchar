package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.IrPath;
import dev.uchar.regelspraak.ir.Expr;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader's refusals of the reference layer in the shape L2 of the v1 wire migration removed: a type written on a
 * reference, the two role-check nodes and their fields beside {@code RoleMembership}, a {@code RoleMembership}
 * without its fact type or role, a narrowing written as {@code null}, the datatype {@code Onbekend} at every kind
 * of position, and a marker's type written as {@code null}. And the reference-layer shapes every executor refuses by
 * name: a role check naming no link the subject can be in (WF-034), a fold's set source its kind cannot consume
 * (WF-093, WF-080, WF-102 (c), WF-034), a {@code total} without a range over another type than its subject (WF-083 (c)),
 * and a characteristic read at a value position (WF-080). The fields of an
 * attribute read, a creation, a uniqueness and a characteristic check are pinned where their bindings are
 * ({@code NavigationWitnessTest}, {@code BeansFieldWitnessTest}); a fold, a quantifier fold and a selection
 * without their range where those are ({@code TemporalSetFoldRangeWitnessTest},
 * {@code QuantifierFoldRangeWitnessTest}, {@code InstanceUnionWitnessTest}).
 *
 * <p>Every fixture is a corpus golden with ONE twist in memory; the corpus file is read and never written. Each
 * refusal is an {@link IrLoader.DocumentError} at a JSON pointer, never an {@link IrLoader.Refusal}: these are not
 * forms this backend declines but documents that are not {@code uchar-ir-v1}, and a malformed document filed in the
 * gate's auto-excluded channel would leave it green over a document nobody read.
 */
class ReferenceLayerRefusalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** One parameter, vondstdrempel, read in a rule. Both branches PLAYED. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** A Let whose body reads its binder. Both branches PLAYED. */
    private static final String BERGINGSTOESLAG = "jutterswet-bergingstoeslag";
    /** Four role checks over wachtindeling; rules 2 and 3 narrow the other end to Strandjutter. */
    private static final String POSTBEZETTING = "jutterswet-postbezetting";
    /** Temporal attributes on Bakenmeester; rule 0 computes a value, a fold with a type of its own. */
    private static final String BAKENMEESTER = "jutterswet-bakenmeester";
    /** Munt ⊑ Vondst; rule 0 counts a Jutter's vondsten over Munt, rule 1 sums their gewicht. Both bound. */
    private static final String MUNTTELLING = "jutterswet-munttelling";
    /** Rule 0 folds over the two-step path Jutter -> Boot -> Vondst. */
    private static final String BOOTBERGING = "anchor/jutterswet-bootberging";
    /** Rule 2 totals the Jutter's own timeline, a {@code total} without a range over the subject type. */
    private static final String TEMPOTOTAAL = "jutterswet-tempototaal";

    private static final String NUMERIEK =
            "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String UNKNOWN = "{\"node\":\"Onbekend\",\"reden\":\"niet opgelost\"}";

    // ---------------- the reference nodes carry no type ----------------

    @Test void a_type_on_a_reference_is_refused_by_name() throws Exception {
        // the attribute read has its own row with its four other fields (NavigationWitnessTest)
        ObjectNode ir = golden(PENNINGDREMPEL);
        Hit p = find(ir, "ParameterReference");
        p.node().set("type", JSON.readTree(NUMERIEK));
        unknownField(ir, p.path() + "/type", "ParameterReference");

        ir = golden(BERGINGSTOESLAG);
        Hit b = find(ir, "BinderRef");
        b.node().set("type", JSON.readTree(NUMERIEK));
        unknownField(ir, b.path() + "/type", "BinderRef");

        // no replaying golden reads a dimension cell or a variable, so each stands in for rule 0's value
        ir = golden(PENNINGDREMPEL);
        ObjectNode rule = IrPath.version(ir, "rules", 0, 0);
        String value = "/rules/rules/0/versions/0/value";
        rule.set("value", JSON.readTree("{\"node\":\"DimensionCellRead\",\"attribute\":" + rule.get("target")
                + ",\"labels\":[\"noord\"],\"source\":{\"snippet\":\"de cel noord\"},\"type\":" + NUMERIEK + "}"));
        unknownField(ir, value + "/type", "DimensionCellRead");
        rule.set("value", JSON.readTree("{\"node\":\"VariableReference\",\"name\":\"x\",\"source\":{\"snippet\":\"x\"},\"type\":"
                + NUMERIEK + "}"));
        unknownField(ir, value + "/type", "VariableReference");
    }

    // ---------------- the role check is one node ----------------

    @Test void the_role_checks_of_the_l1_form_are_unknown_nodes() throws Exception {
        String vw = "/rules/rules/0/versions/0/condition";
        ObjectNode ir = golden(POSTBEZETTING);
        condition(ir, 0, "{\"node\":\"Rollidmaatschap\",\"rol\":\"wachtjutter\",\"anderTeFiller\":\"Jutter\","
                + "\"negatie\":false,\"source\":{\"snippet\":\"hij een wachtjutter heeft\"}}");
        IrLoader.DocumentError e = refuses(ir, vw + "/node");
        assertTrue(e.getMessage().contains("unknown condition node \"Rollidmaatschap\""), e.getMessage());

        condition(ir, 0, "{\"node\":\"VerreRollidmaatschap\",\"filler\":\"Jutter\",\"backref\":"
                + "\"toegewezen wachtpost\",\"negatie\":false,\"source\":{\"snippet\":\"hij een wachtjutter heeft\"}}");
        e = refuses(ir, vw + "/node");
        assertTrue(e.getMessage().contains("unknown condition node \"VerreRollidmaatschap\""), e.getMessage());
    }

    @Test void a_role_membership_carries_fact_type_role_and_negated() throws Exception {
        String vw = "/rules/rules/0/versions/0/condition";
        for (String field : new String[] { "factType", "role", "negated" }) {
            ObjectNode ir = golden(POSTBEZETTING);
            ((ObjectNode) IrPath.version(ir, "rules", 0, 0).get("condition")).remove(field);
            IrLoader.DocumentError e = refuses(ir, vw);
            assertTrue(e.getMessage().contains("missing field '" + field + "'"), e.getMessage());
        }
    }

    @Test void a_field_of_the_l1_role_check_is_unknown_on_a_role_membership() throws Exception {
        String vw = "/rules/rules/0/versions/0/condition";
        for (String field : new String[] { "rol", "anderTeFiller", "filler", "backref", "negatie" }) {
            ObjectNode ir = golden(POSTBEZETTING);
            ObjectNode rm = (ObjectNode) IrPath.version(ir, "rules", 0, 0).get("condition");
            if (field.equals("negatie")) rm.put(field, false); else rm.put(field, "Jutter");
            unknownField(ir, vw + "/" + field, "RoleMembership");
        }
    }

    @Test void a_counterpart_is_whole_or_absent() throws Exception {
        // IR-SPEC ch. 4.5: absent means the other role's declared filler; written, it names a declared object type
        String vw = "/rules/rules/2/versions/0/condition";
        ObjectNode ir = golden(POSTBEZETTING);
        ObjectNode rm = (ObjectNode) IrPath.version(ir, "rules", 2, 0).get("condition");
        assertEquals("Strandjutter", rm.get("counterpartType").asText(), "de golden vernauwt regel 2");
        rm.putNull("counterpartType");
        IrLoader.DocumentError e = refuses(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("null is neither"), e.getMessage());

        rm.put("counterpartType", "Kustwacht");
        e = refuses(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("the counterpart type \"Kustwacht\" is no declared object type"),
                e.getMessage());

        rm.put("counterpartType", 2);
        e = refuses(ir, vw + "/counterpartType");
        assertTrue(e.getMessage().contains("expected a string"), e.getMessage());
    }

    // ---------------- Onbekend is no datatype ----------------

    @Test void unknown_is_a_datatype_on_no_single_position() throws Exception {
        // a declaration (an M1 position; the refusal stands at the discriminator of the datatype the field holds)
        ObjectNode ir = golden(BAKENMEESTER);
        String attr = "/model/objectTypes/" + index(IrPath.modelList(ir, "objectTypes"), "name", "Bakenmeester")
                + "/attributes/" + index((ArrayNode) IrPath.objectType(ir, "Bakenmeester").get("attributes"), "name",
                "onderhoudstarief");
        IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").set("datatype",
                JSON.readTree(UNKNOWN));
        unknownDatatype(ir, attr + "/datatype");

        // a derived position: the fold's own annotation
        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPath.version(ir, "rules", 0, 0).get("value")).set("type", JSON.readTree(UNKNOWN));
        unknownDatatype(ir, "/rules/rules/0/versions/0/value/type");

        // a marker's type: absent is the unknown one (IR-SPEC ch. 4.4), so no spelling of it is admitted
        ir = golden(BAKENMEESTER);
        IrPath.version(ir, "rules", 0, 0).set("value",
                JSON.readTree("{\"node\":\"Unresolved\",\"type\":" + UNKNOWN + ",\"source\":{\"snippet\":\"iets onleesbaars\"}}"));
        unknownDatatype(ir, "/rules/rules/0/versions/0/value/type");
    }

    @Test void a_marker_without_type_is_unknown_and_null_is_no_spelling() throws Exception {
        // rule 0 computes a value, so a marker may stand there (a characteristic assignment admits only waar)
        ObjectNode ir = golden(BAKENMEESTER);
        ObjectNode rule = IrPath.version(ir, "rules", 0, 0);
        rule.set("value", JSON.readTree("{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"iets onleesbaars\"}}"));
        Expr w = IrLoader.loadAll(ir).model().resolvedRules.get(0).value();
        assertTrue(w instanceof Expr.Unresolved, String.valueOf(w));
        assertNull(w.type(), "een marker zonder type draagt geen type — nooit een gok");

        rule.set("value", JSON.readTree("{\"node\":\"Unresolved\",\"type\":" + NUMERIEK + ",\"source\":{\"snippet\":\"x\"}}"));
        assertNotNull(IrLoader.loadAll(ir).model().resolvedRules.get(0).value().type(), "een echt datatype blijft");

        rule.set("value", JSON.readTree("{\"node\":\"Unresolved\",\"type\":null,\"source\":{\"snippet\":\"x\"}}"));
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/0/versions/0/value/type");
        assertTrue(e.getMessage().contains("null is neither"), e.getMessage());
    }

    // ---------------- the role check names a link of the model (WF-034) ----------------
    // Until L2's fix round each of these loaded and was then refused with a storage reason, or played: a check
    // that names no link the subject can be in holds for no instance. The sentences are the ones ref and the ASP
    // backend fault with, or the verifier's where those have none.

    @Test void a_role_check_over_an_unknown_fact_type_or_an_unknown_role_is_refused() throws Exception {
        String vw = "/rules/rules/0/versions/0/condition";
        ObjectNode ir = golden(POSTBEZETTING);
        roleCheck(ir, 0).put("factType", "wachtrooster");
        IrLoader.DocumentError e = refuses(ir, vw + "/factType");
        assertTrue(e.getMessage().contains("role check names fact type 'wachtrooster', which is not declared"),
                e.getMessage());

        ir = golden(POSTBEZETTING);
        roleCheck(ir, 0).put("role", "wachtjutters");   // the declared PLURAL names no role here
        e = refuses(ir, vw + "/role");
        assertTrue(e.getMessage().contains("'wachtjutters' is not a declared role of fact type 'wachtindeling'"),
                e.getMessage());
    }

    @Test void a_role_check_that_does_not_fill_the_subject_is_refused() throws Exception {
        // "hij een wachtjutter heeft" read with the role the OTHER end plays: a Wachtpost never fills wachtjutter
        ObjectNode ir = golden(POSTBEZETTING);
        roleCheck(ir, 0).put("role", "wachtjutter");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/0/versions/0/condition/role");
        assertTrue(e.getMessage().contains("role check: the subject \"Wachtpost\" does not fill the role"
                + " \"wachtjutter\" of \"wachtindeling\", so no link of it can hold the subject there"),
                e.getMessage());
    }

    @Test void a_counterpart_outside_the_filler_of_the_other_role_is_refused() throws Exception {
        ObjectNode ir = golden(POSTBEZETTING);
        roleCheck(ir, 2).put("counterpartType", "Wachtpost");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/2/versions/0/condition/counterpartType");
        assertTrue(e.getMessage().contains("role check: the counterpart type \"Wachtpost\" is no subtype of"
                + " \"Jutter\", the filler the role \"wachtjutter\" declares"), e.getMessage());
        // ⊑ admits the declared filler itself: the check then narrows nothing, and it loads
        roleCheck(ir, 2).put("counterpartType", "Jutter");
        IrLoader.loadAll(ir);
    }

    // ---------------- the set source of a fold with a range (WF-093, WF-080, WF-102 (c), WF-034) ----------------
    // jutterswet-munttelling: rule 0 counts the Jutter's vondsten over Munt, rule 1 sums the gewicht of those munten.
    // Measured on the L2 union before the fix round: the count over an attribute PLAYED in both branches, the value
    // fold over the role name failed with a runtime error at first fire in the exec branch and did not compile in the
    // DRL-text one.

    @Test void a_count_over_an_attribute_of_the_element_type_is_refused() throws Exception {
        ObjectNode ir = golden(MUNTTELLING);
        source(ir, 0).put("attribute", "gewicht").put("subjectType", "Munt");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/0/versions/0/value/arguments/0");
        assertTrue(e.getMessage().endsWith(": the source of this count aggregation reads 'gewicht', a member of the"
                + " element type 'Munt': a count folds INSTANCES, so its source is a count source naming the relation's"
                + " role, never a value read per contributor"), e.getMessage());
    }

    @Test void a_value_fold_over_a_role_name_is_refused() throws Exception {
        ObjectNode ir = golden(MUNTTELLING);
        source(ir, 1).put("attribute", "vondsten").put("subjectType", "Jutter");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/1/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("value aggregation over \"vondsten\", which is no attribute of \"Munt\":"
                + " a value fold reads an attribute per contributor, and a role name is a count source only a count"
                + " folds"), e.getMessage());

        // a name that is neither an attribute nor a role declares nothing (WF-034)
        ir = golden(MUNTTELLING);
        source(ir, 1).put("attribute", "glans");
        e = refuses(ir, "/rules/rules/1/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("'glans' is not a declared attribute of 'Munt'"), e.getMessage());
    }

    @Test void a_role_name_that_does_not_reach_the_subject_is_no_count_source() throws Exception {
        // `aanmelder` IS a declared role, but the one the Jutter itself fills: from the fold subject it reaches no
        // relation, so under the value fold the name is no count source but an undeclared member (WF-034), as the
        // verifier's source reading judges it. Until the fix round's second pass any declared role name counted.
        ObjectNode ir = golden(MUNTTELLING);
        source(ir, 1).put("attribute", "aanmelder").put("subjectType", "Jutter");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/1/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("'aanmelder' is not a declared attribute of 'Munt': a value fold's source"
                + " reads an attribute of each contributor"), e.getMessage());
    }

    // ---------------- the element type of the subject's own read (WF-083 (c)) ----------------

    @Test void a_total_without_range_over_another_type_is_refused() throws Exception {
        // jutterswet-tempototaal's third rule totals the Jutter's own timeline, without a range; the twist names
        // Vondst as its element type. No executor reads that field there, so before this check the twisted
        // document PLAYED in both branches here, as it passed in ref and clingo.
        ObjectNode ir = golden(TEMPOTOTAAL);
        ObjectNode w = (ObjectNode) IrPath.version(ir, "rules", 2, 0).get("value");
        assertEquals("Jutter", w.get("elementType").asText(), "de golden totaliseert de eigen tijdlijn");
        w.put("elementType", "Vondst");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/2/versions/0/value/elementType");
        assertTrue(e.getMessage().contains("the elementType 'Vondst' of a total without a range is not the fold"
                + " subject 'Jutter': without a range the fold is the subject's own read, so its elementType must"
                + " name the subject type"), e.getMessage());
    }

    @Test void a_characteristic_as_source_of_a_value_fold_is_refused() throws Exception {
        ObjectNode ir = golden(MUNTTELLING);
        IrPath.objectType(ir, "Munt").withArray("characteristics").addObject().put("node", "Characteristic")
                .put("name", "gangbaar").put("kind", "adjectival");
        source(ir, 1).put("attribute", "gangbaar");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/1/versions/0/value/arguments/0");
        assertTrue(e.getMessage().contains("'gangbaar' is a characteristic of 'Munt', not an attribute — a"
                + " characteristic endpoint is ill-formed in an aggregation source position"), e.getMessage());
    }

    @Test void a_count_over_a_path_is_refused() throws Exception {
        // anchor/jutterswet-bootberging's rule 0 folds over the two-step path Jutter -> Boot -> Vondst; as a count over
        // the reached role it names a range of two steps where a count's range is one
        ObjectNode ir = golden(BOOTBERGING);
        ObjectNode w = (ObjectNode) IrPath.version(ir, "rules", 0, 0).get("value");
        w.put("kind", "count");
        source(ir, 0).put("attribute", "geborgen vondsten").put("subjectType", "Jutter");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/0/versions/0/value/sourcePath");
        assertTrue(e.getMessage().contains("a sourcePath is admissible only as the range of a single-argument VALUE"
                + " fold's set source — a count's range is one step, spelled sourceRole"), e.getMessage());

        // ...whatever the source reads: with a null name the source check has nothing to judge, and until the
        // advisory round the count then passed and was counted along the path (the lens measured "verwacht 2,
        // werkelijk 3" on the exec branch)
        source(ir, 0).putNull("attribute");
        e = refuses(ir, "/rules/rules/0/versions/0/value/sourcePath");
        assertTrue(e.getMessage().contains("a count's range is one step, spelled sourceRole"), e.getMessage());
    }

    // ---------------- a characteristic at a value position (WF-080) ----------------

    @Test void a_characteristic_at_a_value_position_is_refused() throws Exception {
        // "hij bemand gelijk is aan onwaar": the characteristic read as an operand. Measured on the L2 union before
        // the fix round: PLAYED in both branches, where the verifier refuses it.
        ObjectNode ir = golden(POSTBEZETTING);
        IrPath.version(ir, "rules", 1, 0).set("condition", JSON.readTree(
                "{\"node\":\"Comparison\",\"op\":\"equal\",\"left\":{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"bemand\",\"subjectType\":\"Wachtpost\",\"source\":{\"snippet\":\"bemand\"}},\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"boolean\":false},\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"onwaar\"}},"
                + "\"source\":{\"snippet\":\"hij bemand gelijk is aan onwaar\"}}"));
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/1/versions/0/condition/left");
        assertTrue(e.getMessage().contains("'bemand' is a characteristic of 'Wachtpost', not an attribute — a"
                + " characteristic endpoint is ill-formed in a value position"), e.getMessage());
    }

    // ---------------- fixtures ----------------

    private static ObjectNode roleCheck(ObjectNode ir, int rule) {
        return (ObjectNode) IrPath.version(ir, "rules", rule, 0).get("condition");
    }

    /** The single argument of rule {@code rule}'s fold: its set source. */
    private static ObjectNode source(ObjectNode ir, int rule) {
        return (ObjectNode) IrPath.version(ir, "rules", rule, 0).get("value").get("arguments").get(0);
    }

    /** A node of the given kind and its JSON pointer. */
    private record Hit(ObjectNode node, String path) {}

    /** The FIRST node of kind {@code kind} under the rules, in document order; the golden must carry one. */
    private static Hit find(JsonNode ir, String kind) {
        Hit t = find(ir.get("rules"), "/rules", kind);
        assertNotNull(t, "de golden draagt geen " + kind);
        return t;
    }

    private static Hit find(JsonNode n, String path, String kind) {
        if (n instanceof ObjectNode o) {
            if (kind.equals(o.path("node").asText(null))) return new Hit(o, path);
            for (var it = o.properties().iterator(); it.hasNext(); ) {
                var e = it.next();
                Hit t = find(e.getValue(), path + "/" + e.getKey().replace("~", "~0").replace("/", "~1"), kind);
                if (t != null) return t;
            }
        } else if (n instanceof ArrayNode a) {
            for (int i = 0; i < a.size(); i++) {
                Hit t = find(a.get(i), path + "/" + i, kind);
                if (t != null) return t;
            }
        }
        return null;
    }

    private static void condition(ObjectNode ir, int rule, String json) throws Exception {
        IrPath.version(ir, "rules", rule, 0).set("condition", JSON.readTree(json));
    }

    private static int index(ArrayNode list, String nameField, String name) {
        for (int i = 0; i < list.size(); i++) if (name.equals(list.get(i).path(nameField).asText(null))) return i;
        throw new AssertionError("geen " + name + " in de lijst");
    }

    private static IrLoader.DocumentError refuses(JsonNode ir, String site) {
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + site);
        assertEquals(site, e.site(), e.getMessage());
        return e;
    }

    private static void unknownField(JsonNode ir, String site, String node) {
        IrLoader.DocumentError e = refuses(ir, site);
        assertTrue(e.getMessage().contains("unknown field on " + node), e.getMessage());
    }

    /** The shared loader's refusal of the node at the datatype position {@code site}: at its discriminator, in its
     *  sentence. */
    private static void unknownDatatype(JsonNode ir, String site) {
        IrLoader.DocumentError e = refuses(ir, site + "/node");
        assertTrue(e.getMessage().endsWith(": unknown datatype node \"Onbekend\""), e.getMessage());
    }

    private static ObjectNode golden(String name) throws Exception {
        Path ir = CORPUS.resolve(name).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }
}
