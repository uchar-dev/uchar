package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.uchar.regelspraak.cases.CaseRunner;
import dev.uchar.regelspraak.cases.IrPath;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader's refusals of a document that is not a well-formed {@code uchar-ir-v1} document in its M1 shape: the
 * container, the declaration lists, the member nodes of an object type, the domain's datatype and each diagnostic.
 * Every fixture is a replaying corpus golden with ONE twist in memory; the corpus file is read and never written.
 *
 * <p>Measured 2026-09-23 by the M1 lens on the loader as M1 first wrote it: each twist below was read without a
 * word — a node spelled {@code Objecttype} or without {@code node}, a v0 side map beside the member nodes, an
 * unknown field on a member node, an optional field written as {@code null}, a missing {@code diagnostics},
 * {@code dayKinds} or {@code characteristics}, {@code animate: "true"}, {@code objectTypes: {}}, a count of
 * {@code "2"} or {@code 2.0}, a characteristic declared twice, two object types whose names differ only in case,
 * an Enumeratie domain that kept its v0 self-name — each PLAYED, or loaded with part of the document ignored. The
 * OCaml loader refuses every one of them with a JSON pointer; this loader now does the same, and names an absent
 * field as the OCaml loader does, at the object that lacks it, with the field in the detail.
 *
 * <p>Each refusal is an {@link IrLoader.DocumentError}, a hard error, and never an {@link IrLoader.Refusal}: a
 * Refusal is the differential gate's auto-excluded channel ("form not carried"), and a malformed document filed
 * there leaves the gate green over a document nobody read. Corpus golden and red documents are verifier-clean,
 * so none of them reaches these arms; this suite is the only place they are pinned. Three tests pin the other side
 * of that line: a well-formed document of a form this backend does not carry (another format, a functionDefs
 * list, a period count beyond a Java int) stays a Refusal.
 */
class LoaderRefusalWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** One object type, Bakenmeester: temporal attributes (year and month) and the temporal characteristic
     *  'dienstdoend'. One case, both branches PLAYED. */
    private static final String BAKENMEESTER = "jutterswet-bakenmeester";
    /** Klipjutter declares the supertype Jutter. One case, both branches PLAYED. */
    private static final String ERFWACHT = "jutterswet-erfwacht";
    /** The Enumeration domain Vondstklasse. Three cases, both branches PLAYED. */
    private static final String VONDSTKLASSE = "jutterswet-vondstklasse";
    /** The kernel anchor of inheritance: Strandrecht -&gt; Wrakrecht plus an aggregating Kustgemeente. */
    private static final String CHAIN = "anchor/jutterswet-strandrechtketen";
    /** One parameter, vondstdrempel. One case, both branches PLAYED. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** Carries the model's functionDefs extension: EXCLUDE in both branches, by a loader Refusal. */
    private static final String KAMERHEFFING = "jutterswet-kamerheffing-catala";

    // ---------------- the node and its fields ----------------

    @Test void a_node_with_the_v0_spelling_of_its_kind_is_refused() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePath(ir, "Bakenmeester");
        IrPath.objectType(ir, "Bakenmeester").put("node", "Objecttype");
        IrLoader.DocumentError e = refuses(ir, ot + "/node");
        assertTrue(e.getMessage().contains("\"Objecttype\""), e.getMessage());
    }

    @Test void a_member_node_without_a_node_is_refused() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        String attr = memberPath(ir, "Bakenmeester", "attributes", "onderhoudstarief");
        IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").remove("node");
        missing(ir, attr, "node");
    }

    @Test void a_v0_side_map_beside_the_member_nodes_is_refused() throws Exception {
        // The side map was ignored whole: its entry gave no member a timeline, and the member node it
        // duplicates decided alone. A side map that DISAGREED with the member nodes went unread just as quietly.
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePath(ir, "Bakenmeester");
        IrPath.objectType(ir, "Bakenmeester").putObject("attribuutGranulariteit").put("onderhoudstarief", "DAG");
        IrLoader.DocumentError e = refuses(ir, ot + "/attribuutGranulariteit");
        assertTrue(e.getMessage().contains("unknown field"), e.getMessage());
    }

    @Test void an_unknown_field_on_a_m1_node_is_refused() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePath(ir, "Bakenmeester");
        IrPath.objectType(ir, "Bakenmeester").put("naam", "Bakenmeester");
        refuses(ir, ot + "/naam");

        ir = golden(BAKENMEESTER);
        String attr = memberPath(ir, "Bakenmeester", "attributes", "onderhoudstarief");
        IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").put("granulariteit", "JAAR");
        refuses(ir, attr + "/granulariteit");

        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                .put("aantal", 1);
        refuses(ir, attr + "/timeline/aantal");

        ir = golden(BAKENMEESTER);
        String characteristic = memberPath(ir, "Bakenmeester", "characteristics", "dienstdoend");
        IrPath.characteristic(IrPath.objectType(ir, "Bakenmeester"), "dienstdoend").put("soort", "BIJVOEGLIJK");
        refuses(ir, characteristic + "/soort");
    }

    @Test void an_optional_field_as_null_is_refused() throws Exception {
        // IR-SPEC § Method: an optional field is written whole or not at all. Read leniently, each null below
        // was the same as the absent field, so a producer that wrote one never heard of it.
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePath(ir, "Bakenmeester");
        String attr = memberPath(ir, "Bakenmeester", "attributes", "onderhoudstarief");
        IrPath.objectType(ir, "Bakenmeester").putNull("plural");
        refuses(ir, ot + "/plural");

        ir = golden(BAKENMEESTER);
        IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").putNull("plural");
        refuses(ir, attr + "/plural");

        ir = golden(BAKENMEESTER);
        IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").putNull("dimensions");
        refuses(ir, attr + "/dimensions");

        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                .putNull("timelineName");
        refuses(ir, attr + "/timeline/timelineName");

        // a null supertype made Klipjutter a root type without a word
        ir = golden(ERFWACHT);
        String clip = objectTypePath(ir, "Klipjutter");
        IrPath.objectType(ir, "Klipjutter").putNull("supertype");
        refuses(ir, clip + "/supertype");
    }

    @Test void a_missing_required_field_is_refused() throws Exception {
        // named as the OCaml loader names it: at the object that lacks the field, the root being ""
        ObjectNode ir = golden(BAKENMEESTER);
        ir.remove("diagnostics");
        IrLoader.DocumentError e = missing(ir, "", "diagnostics");
        assertTrue(e.getMessage().contains(" at <root>: "), e.getMessage());

        ir = golden(BAKENMEESTER);
        IrPath.model(ir).remove("dayKinds");
        missing(ir, "/model", "dayKinds");

        ir = golden(BAKENMEESTER);
        String ot = objectTypePath(ir, "Bakenmeester");
        IrPath.objectType(ir, "Bakenmeester").remove("characteristics");
        missing(ir, ot, "characteristics");
    }

    @Test void a_document_that_is_no_object_is_refused_on_the_root() throws Exception {
        IrLoader.DocumentError e = refuses(JSON.createArrayNode(), "");
        assertTrue(e.getMessage().contains(" at <root>: expected an object"), e.getMessage());
    }

    @Test void a_field_of_the_wrong_json_type_is_refused() throws Exception {
        // animate "true" read as true, and objectTypes {} as a document with no object types at all
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePath(ir, "Bakenmeester");
        IrPath.objectType(ir, "Bakenmeester").put("animate", "true");
        refuses(ir, ot + "/animate");

        ir = golden(BAKENMEESTER);
        IrPath.model(ir).putObject("objectTypes");
        refuses(ir, "/model/objectTypes");

        ir = golden(BAKENMEESTER);
        String characteristic = memberPath(ir, "Bakenmeester", "characteristics", "dienstdoend");
        IrPath.characteristic(IrPath.objectType(ir, "Bakenmeester"), "dienstdoend").put("name", 5);
        refuses(ir, characteristic + "/name");
    }

    @Test void a_period_count_that_is_no_whole_number_is_refused() throws Exception {
        // "2" and 2.0 both read as 2: a raster the document did not write as a count
        for (JsonNode count : List.of(JSON.getNodeFactory().textNode("2"), JSON.getNodeFactory().numberNode(2.0))) {
            ObjectNode ir = golden(BAKENMEESTER);
            String attr = memberPath(ir, "Bakenmeester", "attributes", "onderhoudstarief");
            ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                    .set("count", count);
            IrLoader.DocumentError e = refuses(ir, attr + "/timeline/count");
            assertTrue(e.getMessage().contains("integer"), e.getMessage());
        }
    }

    @Test void a_period_count_outside_the_int_range_is_a_form_and_no_document_error() throws Exception {
        // The boundary of the test above. 2147483648 and 3000000000 are integers: the schema admits them and the
        // verifier, which reads into OCaml's native int, passes them. This backend holds a count in a Java int,
        // so the document is a form it does not carry, and a Refusal says so. Until 2026-09-23 it was a document
        // defect reading "expected an integer, got 3000000000", about a value that is one.
        for (long count : new long[] { 2_147_483_648L, 3_000_000_000L }) {
            ObjectNode ir = golden(BAKENMEESTER);
            String attr = memberPath(ir, "Bakenmeester", "attributes", "onderhoudstarief");
            ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                    .put("count", count);
            IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(ir), "count " + count);
            assertEquals("TimelineFacet.count", r.form(), r.getMessage());
            assertTrue(r.getMessage().contains("outside this backend's integer range"), r.getMessage());
            assertTrue(r.getMessage().contains(count + " at '" + attr + "/timeline/count'"), r.getMessage());
        }
    }

    @Test void a_timeline_facet_without_granularity_is_refused_on_loading() throws Exception {
        // Until 2026-09-23 this was InheritanceRefusalWitnessTest's "de gedegenereerde variabele tijdlijn is het
        // gat dat werkelijk stil was": under v0 a tijdlijn declaration could carry `granulariteit: null`
        // (schema-legal), WF-005 then compared None with None, and an attribute riding that named tijdlijn needed
        // NO granularity entry — the silent §9.9 hole the narrowed inheritance guard closed (measured then, probe
        // p1cv: verifier PASS, merge base PLAYED 4). M1 made the shape unwritable: a member rides a named tijdlijn
        // only through a TimelineFacet, and the facet requires its granularity. What is left to pin is this
        // loader's refusal of the grid-less facet, instead of letting the member in as temporal with a null grid.
        // The old fixture's startpuntbepaling is gone: the facet is refused while the object types are read,
        // before any rule is.
        ObjectNode ir = golden(CHAIN);
        IrPath.modelList(ir, "timelines").add(JSON.createObjectNode().put("node", "Timeline").put("name", "boekjaar")
                .put("variableStart", true).putNull("granularity").put("count", 1));
        ObjectNode timelineAttribute = IrPath.addAttribute(IrPath.objectType(ir, "Strandrecht"), "peilwaarde",
                JSON.createObjectNode().put("node", "Number").put("whole", true).put("decimals", 0)
                        .put("sign", "nonNegative"));
        timelineAttribute.putObject("timeline").put("node", "TimelineFacet").put("count", 1).put("timelineName", "boekjaar");
        missing(ir, memberPath(ir, "Strandrecht", "attributes", "peilwaarde") + "/timeline", "granularity");
    }

    @Test void a_timeline_facet_without_count_is_refused() throws Exception {
        // the characteristic's facet, the second of the two places a facet is read
        ObjectNode ir = golden(BAKENMEESTER);
        String characteristic = memberPath(ir, "Bakenmeester", "characteristics", "dienstdoend");
        ((ObjectNode) IrPath.characteristic(IrPath.objectType(ir, "Bakenmeester"), "dienstdoend").get("timeline"))
                .remove("count");
        missing(ir, characteristic + "/timeline", "count");
    }

    @Test void a_timeline_name_on_a_characteristic_facet_is_refused() throws Exception {
        // The schema gives a characteristic's facet no timelineName (its own CharacteristicTimelineFacet
        // definition): riding a named tijdlijn is an attribute's form (§13.3.6.9). Read leniently the field was
        // dropped without a word; the OCaml loader refuses it as an unknown field, and so does this one.
        ObjectNode ir = golden(BAKENMEESTER);
        String characteristic = memberPath(ir, "Bakenmeester", "characteristics", "dienstdoend");
        ((ObjectNode) IrPath.characteristic(IrPath.objectType(ir, "Bakenmeester"), "dienstdoend").get("timeline"))
                .put("timelineName", "boekjaar");
        IrLoader.DocumentError e = refuses(ir, characteristic + "/timeline/timelineName");
        assertTrue(e.getMessage().contains("unknown field"), e.getMessage());
    }

    @Test void a_characteristic_without_a_kind_or_with_an_unknown_kind_is_refused() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        String characteristic = memberPath(ir, "Bakenmeester", "characteristics", "dienstdoend");
        IrPath.characteristic(IrPath.objectType(ir, "Bakenmeester"), "dienstdoend").remove("kind");
        missing(ir, characteristic, "kind");

        // the v0 spelling of the value is no member since L5, and neither is a word outside the family: both are
        // refused in the shared loader's sentence
        for (String kind : List.of("BIJVOEGLIJK", "bijvoeglijk", "Adjectival")) {
            ir = golden(BAKENMEESTER);
            IrPath.characteristic(IrPath.objectType(ir, "Bakenmeester"), "dienstdoend").put("kind", kind);
            IrLoader.DocumentError e = refuses(ir, characteristic + "/kind");
            assertTrue(e.getMessage().endsWith(": unknown kind value \"" + kind + "\""), e.getMessage());
        }
    }

    @Test void an_unknown_section_is_refused_and_functionDefs_stays_a_form() throws Exception {
        // the v0 rule stubs (delta B3), a v0 model table name, a misspelt rules list: each was an "unknown
        // section" Refusal, the auto-excluded channel; each is a defect of the document
        ObjectNode ir = golden(BAKENMEESTER);
        ir.putArray("regels");
        refuses(ir, "/regels");

        ir = golden(BAKENMEESTER);
        IrPath.model(ir).putObject("objecttypen");
        refuses(ir, "/model/objecttypen");

        ir = golden(BAKENMEESTER);
        IrPath.rules(ir).putArray("prioritizedDefault");
        refuses(ir, "/rules/prioritizedDefault");

        // The boundary: functionDefs is an extension the schema admits and this backend does not carry — a
        // form, so it stays a Refusal and its goldens stay in the reasoned EXCLUDE channel.
        JsonNode catala = JSON.readTree(Files.readString(CORPUS.resolve(KAMERHEFFING).resolve("ir.json")));
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(catala));
        assertEquals("functionDefs", r.form(), r.getMessage());
    }

    // ---------------- what the first strict reading left lenient ----------------
    // Measured 2026-09-23 by the second lens round: each twist below PLAYED, landed in the auto-excluded channel
    // with a reason that named no defect, or died in a NullPointerException naming no place.

    @Test void a_datatype_without_a_node_is_refused_with_a_site() throws Exception {
        // at an M1 position (the attribute, the domain) and, since L2's advisory round, at a rule-side position
        // too, each with its JSON pointer (the rule side named only the kind until then)
        ObjectNode ir = golden(BAKENMEESTER);
        String attr = memberPath(ir, "Bakenmeester", "attributes", "onderhoudstarief");
        ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("datatype"))
                .remove("node");
        missing(ir, attr + "/datatype", "node");

        ir = golden(VONDSTKLASSE);
        String domain = "/model/domains/" + index(IrPath.modelList(ir, "domains"), "name", "Vondstklasse");
        ((ObjectNode) IrPath.domain(ir, "Vondstklasse").get("datatype")).remove("node");
        missing(ir, domain + "/datatype", "node");

        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPath.version(ir, "rules", 0, 0).get("value").get("type")).remove("node");
        IrLoader.DocumentError e = refuses(ir, "/rules/rules/0/versions/0/value/type");
        assertTrue(e.getMessage().contains("missing field 'node'"), e.getMessage());
    }

    @Test void a_domain_datatype_carries_exactly_the_fields_of_its_kind() throws Exception {
        // the key check the Enumeration, SumType and StructType arms already had, for the kinds without an arm;
        // no golden declares a number domain, so one is added: clean it loads, with a stray field it is refused
        ObjectNode ir = golden(BAKENMEESTER);
        ArrayNode domains = IrPath.modelList(ir, "domains");
        ObjectNode number = domains.addObject().put("node", "Domain").put("name", "Bedrag").putObject("datatype")
                .put("node", "Number").put("whole", false).put("decimals", 2).put("sign", "unspecified");
        IrLoader.loadAll(ir);
        number.put("naam", "Bedrag");
        refuses(ir, "/model/domains/" + (domains.size() - 1) + "/datatype/naam");
    }

    @Test void a_diagnostic_is_a_node_with_the_form_of_the_schema() throws Exception {
        // WF-113 itself is the verifier's; this is the node's form: the discriminator, the exact keys, a severity
        // and a message that are strings, an integer line
        ObjectNode ir = golden(BAKENMEESTER);
        ArrayNode diagnoses = (ArrayNode) ir.get("diagnostics");
        diagnoses.addObject().put("node", "Diagnostic").put("severity", "warning").put("message", "een melding")
                .put("line", 1);
        IrLoader.loadAll(ir);
        String d = "/diagnostics/" + (diagnoses.size() - 1);
        ObjectNode diagnostic = (ObjectNode) diagnoses.get(diagnoses.size() - 1);

        for (String field : List.of("node", "severity", "message", "line")) {
            ObjectNode twist = ir.deepCopy();
            ((ObjectNode) twist.get("diagnostics").get(diagnoses.size() - 1)).remove(field);
            missing(twist, d, field);
        }
        for (String field : List.of("severity", "message", "line")) {
            ObjectNode twist = ir.deepCopy();
            ((ObjectNode) twist.get("diagnostics").get(diagnoses.size() - 1)).putNull(field);
            refuses(twist, d + "/" + field);
        }
        diagnostic.set("source", IrPath.sourceReference("een melding"));
        IrLoader.DocumentError e = refuses(ir, d + "/source");
        assertTrue(e.getMessage().contains("unknown field"), e.getMessage());
    }

    @Test void a_day_kind_is_a_node_with_a_name_and_a_plural() throws Exception {
        // IR-SPEC ch. 2.9: an entry is a DayKind node; a bare name is its L2 form, and the registry beside the list
        // is gone with it. A name is a string and required, a plural written whole or not at all.
        ObjectNode ir = golden(BAKENMEESTER);
        IrPath.model(ir).putArray("dayKinds").add("springtijdag");
        IrLoader.DocumentError e = refuses(ir, "/model/dayKinds/0");
        assertTrue(e.getMessage().contains("(DayKind)"), e.getMessage());

        ir = golden(BAKENMEESTER);
        IrPath.model(ir).putObject("dagsoortPerEenheid");
        e = refuses(ir, "/model/dagsoortPerEenheid");
        assertTrue(e.getMessage().contains("unknown field"), e.getMessage());

        ir = golden(BAKENMEESTER);
        IrPath.model(ir).putArray("dayKinds").addObject().put("node", "DayKind").put("name", 5);
        refuses(ir, "/model/dayKinds/0/name");

        ir = golden(BAKENMEESTER);
        IrPath.model(ir).putArray("dayKinds").addObject().put("node", "DayKind").put("name", "springtijdag")
                .putNull("plural");
        e = refuses(ir, "/model/dayKinds/0/plural");
        assertTrue(e.getMessage().contains("null is neither"), e.getMessage());

        // read, the name and the plural are the day kind's spellings as a duration unit
        ir = golden(BAKENMEESTER);
        IrPath.model(ir).putArray("dayKinds").addObject().put("node", "DayKind").put("name", "Springtijdag")
                .put("plural", "springtijdagen");
        RegelSpraakModel m = IrLoader.load(ir);
        assertEquals(List.of("Springtijdag"), m.dayKinds);
        assertEquals(java.util.Map.of("springtijdag", "Springtijdag", "springtijdagen", "Springtijdag"),
                m.dayKindPerUnit);
    }

    @Test void format_and_functionDefs_are_first_read_by_form() throws Exception {
        // an absent or non-string format, and a functionDefs that is no list, are defects of the document; only a
        // well-formed one reaches the refusal of a form this backend does not carry
        ObjectNode ir = golden(BAKENMEESTER);
        ir.remove("format");
        missing(ir, "", "format");

        ir = golden(BAKENMEESTER);
        ir.put("format", 1);
        refuses(ir, "/format");

        ObjectNode other = golden(BAKENMEESTER).put("format", "uchar-ir-v0");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(other));
        assertEquals("format", r.form(), r.getMessage());

        ir = golden(BAKENMEESTER);
        IrPath.model(ir).putObject("functionDefs");
        IrLoader.DocumentError e = refuses(ir, "/model/functionDefs");
        assertTrue(e.getMessage().contains("expected an array"), e.getMessage());
    }

    // ---------------- names ----------------

    @Test void an_exact_duplicate_characteristic_is_refused() throws Exception {
        // WF-002. Read leniently, the second declaration's kind won and the first one's timeline stayed.
        ObjectNode ir = golden(BAKENMEESTER);
        ObjectNode ot = IrPath.objectType(ir, "Bakenmeester");
        ArrayNode characteristics = (ArrayNode) ot.get("characteristics");
        characteristics.addObject().put("node", "Characteristic").put("name", "dienstdoend").put("kind", "possessive");
        IrLoader.DocumentError e = refuses(ir, objectTypePath(ir, "Bakenmeester") + "/characteristics/"
                + (characteristics.size() - 1) + "/name");
        assertTrue(e.getMessage().contains("'dienstdoend'"), e.getMessage());
    }

    @Test void two_object_types_that_differ_only_in_capitals_are_refused() throws Exception {
        // WF-001 compares case-insensitively; the model's tables are keyed on the exact string and its
        // lookup falls back to a case-insensitive search, which would pick one of the two.
        ObjectNode ir = golden(BAKENMEESTER);
        ArrayNode list = IrPath.modelList(ir, "objectTypes");
        ObjectNode copy = IrPath.objectType(ir, "Bakenmeester").deepCopy().put("name", "bakenmeester");
        list.add(copy);
        IrLoader.DocumentError e = refuses(ir, "/model/objectTypes/" + (list.size() - 1) + "/name");
        assertTrue(e.getMessage().contains("'Bakenmeester'"), "de melding noemt de eerste spelling: " + e.getMessage());
    }

    @Test void names_collide_under_the_name_identity_of_the_verifier() throws Exception {
        // WF-001 on the parameter list: a difference in case makes no second name (checks.ml
        // norm), and the exact-key table would have kept both
        ObjectNode ir = golden(PENNINGDREMPEL);
        ArrayNode parameters = IrPath.modelList(ir, "parameters");
        parameters.add(IrPath.declaration(ir, "parameters", "vondstdrempel").deepCopy().put("name", "Vondstdrempel"));
        IrLoader.DocumentError e = refuses(ir, "/model/parameters/" + (parameters.size() - 1) + "/name");
        assertTrue(e.getMessage().endsWith(": the name 'Vondstdrempel' collides with the earlier 'vondstdrempel'"
                + " (names are compared case-insensitively)"), e.getMessage());

        // WF-002 on attributes, which the loader compared exactly before: neither case nor a run of spaces makes
        // a second name
        ir = golden(ERFWACHT);
        ObjectNode ot = IrPath.objectType(ir, "Jutter");
        ArrayNode attributes = (ArrayNode) ot.get("attributes");
        attributes.add(IrPath.attribute(ot, "wachtvergoeding per maand").deepCopy()
                .put("name", "Wachtvergoeding  per maand"));
        e = refuses(ir, objectTypePath(ir, "Jutter") + "/attributes/" + (attributes.size() - 1) + "/name");
        assertTrue(e.getMessage().endsWith(": the name 'Wachtvergoeding  per maand' collides with the earlier"
                + " 'wachtvergoeding per maand' (names are compared case-insensitively)"), e.getMessage());
    }

    // ---------------- the domain ----------------

    @Test void an_enumeration_domain_with_a_leftover_self_name_is_refused() throws Exception {
        // IR-SPEC ch. 2.3 / delta B2: the Domain wrapper holds the name, and a self-name beside it is a second
        // spelling of one name — under the key an Enumeration carries it at a reference position, and under its v0
        // key, which is no key of the node since L5.
        for (String key : List.of("domain", "domein")) {
            ObjectNode ir = golden(VONDSTKLASSE);
            String domain = "/model/domains/" + index(IrPath.modelList(ir, "domains"), "name", "Vondstklasse");
            ((ObjectNode) IrPath.domain(ir, "Vondstklasse").get("datatype")).put(key, "Vondstklasse");
            refuses(ir, domain + "/datatype/" + key);
        }
    }

    // ---------------- what a refusal says ----------------

    @Test void an_unknown_value_names_a_field_and_a_value_and_no_java_class() throws Exception {
        // Before: "No enum constant dev.uchar.regelspraak.model.RegelSpraakModel.Granularity.year",
        // the JDK's sentence, naming a Java class and no position in the document. Since L5 the value is read
        // through the translation table and refused in the shared loader's sentence, which names the field and the
        // value, the v0 spelling included.
        for (String value : List.of("jaar", "JAAR")) {
            ObjectNode ir = golden(BAKENMEESTER);
            String attr = memberPath(ir, "Bakenmeester", "attributes", "onderhoudstarief");
            ((ObjectNode) IrPath.attribute(IrPath.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                    .put("granularity", value);
            IrLoader.DocumentError e = refuses(ir, attr + "/timeline/granularity");
            assertTrue(e.getMessage().endsWith(": unknown granularity value \"" + value + "\""), e.getMessage());
            assertFalse(e.getMessage().contains("No enum constant"), e.getMessage());
            assertFalse(e.getMessage().contains("io.github"), e.getMessage());
        }
    }

    @Test void a_document_error_does_not_reach_the_exclude_channel() throws Exception {
        // The channel, end to end: the replay turns a Refusal into EXCLUDE and lets every other exception
        // through, which the CLI prints as ERROR. Both replay branches and the red path build through this
        // loader first; the DRL branch and the red path are the ones reachable from this package.
        ObjectNode ir = golden(BAKENMEESTER);
        IrPath.objectType(ir, "Bakenmeester").put("node", "Objecttype");
        Path tmp = Files.createTempFile("documentfout", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        List<Path> cases = cases(BAKENMEESTER);
        assertThrows(IrLoader.DocumentError.class, () -> CaseRunner.viaDrlText(tmp, cases));
        assertThrows(IrLoader.DocumentError.class, () -> CaseRunner.runRed(tmp, cases.get(0)));
    }

    // ---------------- helpers ----------------

    /** Loads the twisted document and demands a document defect at exactly {@code site}. */
    private static IrLoader.DocumentError refuses(JsonNode ir, String site) {
        IrLoader.DocumentError e = assertThrows(IrLoader.DocumentError.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + site);
        assertEquals(site, e.site(), e.getMessage());
        return e;
    }

    /** {@link #refuses} for an absent field: the defect sits at the object {@code parent}, and the detail names the
     *  field. */
    private static IrLoader.DocumentError missing(JsonNode ir, String parent, String field) {
        IrLoader.DocumentError e = refuses(ir, parent);
        assertTrue(e.getMessage().contains("missing field '" + field + "'"), e.getMessage());
        return e;
    }

    private static ObjectNode golden(String name) throws Exception {
        Path ir = CORPUS.resolve(name).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }

    private static List<Path> cases(String golden) throws Exception {
        try (var s = Files.list(CORPUS.resolve(golden))) {
            List<Path> out = s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
            assertFalse(out.isEmpty(), "golden carries case files: " + golden);
            return out;
        }
    }

    private static String objectTypePath(JsonNode ir, String objectType) {
        return "/model/objectTypes/" + index(IrPath.modelList(ir, "objectTypes"), "name", objectType);
    }

    private static String memberPath(JsonNode ir, String objectType, String members, String member) {
        return objectTypePath(ir, objectType) + "/" + members + "/"
                + index((ArrayNode) IrPath.objectType(ir, objectType).get(members), "name", member);
    }

    private static int index(ArrayNode list, String nameField, String name) {
        for (int i = 0; i < list.size(); i++)
            if (name.equals(list.get(i).path(nameField).asText(null))) return i;
        throw new AssertionError("no entry with " + nameField + " '" + name + "' in " + list);
    }
}
