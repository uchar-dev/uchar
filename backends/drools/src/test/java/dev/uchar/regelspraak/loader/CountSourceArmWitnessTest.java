package dev.uchar.regelspraak.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * IR-SPEC ch. 4.3 / WF-037 — a fold that names its population ({@code elementType}) and hands no source is refused
 * while loading, in the verifier's sentence, so neither branch lowers it. Three shapes: no argument, under every
 * kind; one {@code ListLiteral} under a count or a filled count; and under every kind one node of any other kind but
 * an {@code AttributeReference}, the fold source (the card aantal-bronarm-en-drools-lader). Beside them WF-110 (c):
 * a count over an instance-set union names no source of its own.
 *
 * <p>Measured before that card, with the population of golden {@code jutterswet-vondsttelling} (J1 reaches three
 * Vondsten, J2 none, J3 one; expected counts 3, 0 and 1): a count beside its range whose one argument was a
 * {@code Literal}, a {@code Binary}, a {@code ParameterReference}, a {@code VariableReference}, a
 * {@code BuiltinCall}, an {@code Unresolved} marker or a nested {@code Aggregation} PLAYED in both branches — it
 * counted the instances the range reaches and never read the argument — and so did a count without an argument
 * beside its range; the DRL-text branch played the filled count with such an argument, and the filled count
 * without one beside its range, as an ordinary count. The rest was refused by scope notes (transpiler SKIP,
 * {@code TODO aggregatie}) or, for the {@code ListLiteral}, as a node this backend does not carry — never by the
 * shape. The verifier refuses all of them under WF-037, and {@code ref} and the ASP backend in the same sentences.
 *
 * <p>Every document here is a corpus negatief of that rule, read and never written, or an in-memory variant of
 * one; each row pins the full sentence at the node's position, character for character as the verifier reports
 * it (the negatief's own {@code expect.txt} pins a fragment of it for gate 3).
 */
class CountSourceArmWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path NEGATIEF = Path.of("..", "..", "corpus", "negatief");
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final String SITE = "/rules/rules/0/versions/0/value";
    private static final String SOURCE_ARM = " operand: the source of an instance fold is an AttributeReference";
    private static final String NO_SOURCE = " but no source operand: an instance fold ranges over a written source";
    private static final String UNION = "a count over an instance-set union names no source of its own, and this node"
            + " carries ";
    private static final String UNION_TAIL = ": the union's selections carry the population, the relation and the"
            + " subselection, one set per selection, so a source here is a second claim on one node (ch. 4.3)";
    private static final String WHOLE = "{ \"node\": \"Number\", \"whole\": true, \"decimals\": 0, \"sign\": \"unspecified\" }";
    private static final String SOURCE_PATH_C = "a sourcePath is admissible only as the range of a single-argument VALUE"
            + " fold's set source — a count's range is one step, spelled sourceRole";

    // ---------------- 1. the fold-source arm: one node that is no AttributeReference ----------------

    @Test void a_literal_as_count_source_is_refused() throws Exception {
        refuses("wf037-aantal-elementtype-literaal", "a count aggregation carries elementType \"Vondst\" and a Literal"
                + SOURCE_ARM);
    }

    @Test void a_binary_as_count_source_is_refused() throws Exception {
        refuses("wf037-aantal-elementtype-binair", "a count aggregation carries elementType \"Vondst\" and a Binary"
                + SOURCE_ARM);
    }

    @Test void a_parameter_as_count_source_is_refused() throws Exception {
        refuses("wf037-aantal-elementtype-parameter", "a count aggregation carries elementType \"Vondst\" and a"
                + " ParameterReference" + SOURCE_ARM);
    }

    /** The VariableReference is ill-formed wherever it stands (WF-034): the loader refuses it at the argument, in the
     *  shared loader's sentence, before the fold's source arm is reached — as ref and the ASP backend do. */
    @Test void a_variable_as_count_source_is_refused() throws Exception {
        String arg = SITE + "/arguments/0";
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                () -> IrLoader.loadAll(negative("wf037-aantal-elementtype-variabele")));
        assertEquals(arg, e.site(), e.getMessage());
        assertEquals("malformed uchar-ir-v1 document at '" + arg + "': "
                + IrLoader.variableReferenceRefusal("x"), e.getMessage());
    }

    @Test void a_builtin_as_count_source_is_refused() throws Exception {
        refuses("wf037-aantal-elementtype-builtin", "a count aggregation carries elementType \"Vondst\" and a"
                + " BuiltinCall" + SOURCE_ARM);
    }

    @Test void a_marker_as_count_source_is_refused_and_not_lowered() throws Exception {
        // before: PLAYED in both branches, so the marker was lowered silently — the article follows the vowel
        refuses("wf037-aantal-elementtype-onopgelost", "a count aggregation carries elementType \"Vondst\" and an"
                + " Unresolved" + SOURCE_ARM);
    }

    @Test void a_nested_aggregation_as_count_source_is_refused() throws Exception {
        // the argument is itself a well-formed count; what is refused is its position, not its content
        ObjectNode ir = negative("wf037-aantal-elementtype-literaal");
        ObjectNode count = (ObjectNode) value(ir);
        ObjectNode within = count.deepCopy();
        within.putArray("arguments").addObject().put("node", "AttributeReference").put("attribute", "vondsten")
                .put("subjectType", "Jutter");
        count.putArray("arguments").add(within);
        refuses(ir, "a count aggregation carries elementType \"Vondst\" and an Aggregation" + SOURCE_ARM);
    }

    /** A node this backend does not carry stands at the source all the same, and what is refused there is the shape,
     *  not the node. Before the fix both branches excluded the rule as a form not carried (a Refusal) for each of these
     *  eleven kinds, where the verifier, ref and the ASP backend gave this sentence (measured on the union of the six
     *  cards of 2026-09-25). Only the discriminator is read, and a temporal read's keys, so each node here is that. */
    @Test void a_non_carried_node_as_count_source_is_refused_as_form_and_not_as_node() throws Exception {
        for (String node : List.of("Lambda", "ListLength", "Match", "Construct", "StructLiteral", "FieldAccess",
                "Invoke", "Fold", "MapList", "FilterList", "TemporalRead")) {
            ObjectNode ir = negative("wf037-aantal-elementtype-literaal");
            ObjectNode arg = ((ObjectNode) value(ir)).putArray("arguments").addObject().put("node", node);
            if (node.equals("TemporalRead")) {
                arg.set("base", JSON.readTree("{ \"node\": \"ParameterReference\", \"parameter\": \"p\" }"));
                arg.set("date", JSON.readTree("{ \"node\": \"ContextValue\", \"name\": \"evaluationDate\","
                        + " \"type\": { \"node\": \"Date\", \"precision\": \"day\" } }"));
                arg.set("type", JSON.readTree(WHOLE));
            }
            refuses(ir, "a count aggregation carries elementType \"Vondst\" and "
                    + ("AEIOU".indexOf(node.charAt(0)) >= 0 ? "an " : "a ") + node + SOURCE_ARM);
        }
    }

    @Test void a_non_carried_node_as_source_of_the_sum_is_refused() throws Exception {
        ObjectNode ir = negative("wf037-som-elementtype-literaal");
        ((ObjectNode) value(ir)).putArray("arguments").addObject().put("node", "ListLength");
        refuses(ir, "a sum aggregation carries elementType \"Vondst\" and a ListLength" + SOURCE_ARM);
    }

    @Test void a_non_carried_node_under_the_count_source_is_refused() throws Exception {
        // the argument is a Binary the loader reads, and the Lambda in it is what it cannot: the sentence names the
        // argument, as the verifier's does. Before the fix: the Refusal of the Lambda, in both branches
        ObjectNode ir = negative("wf037-aantal-elementtype-binair");
        ((ObjectNode) value(ir).get("arguments").get(0)).putObject("left").put("node", "Lambda");
        refuses(ir, "a count aggregation carries elementType \"Vondst\" and a Binary" + SOURCE_ARM);
    }

    @Test void a_literal_as_source_of_the_filled_count_is_refused() throws Exception {
        // before: the DRL-text branch played it as an ordinary count, the exec-model branch skipped the rule
        refuses("wf037-aantalgevulde-niet-setbron", "a filledCount aggregation carries elementType \"Vondst\" and a"
                + " Literal" + SOURCE_ARM);
    }

    @Test void a_literal_as_source_of_the_sum_is_refused() throws Exception {
        // before: the DRL-text branch crashed on it (ClassCastException), the exec-model branch skipped the rule
        refuses("wf037-som-elementtype-literaal", "a sum aggregation carries elementType \"Vondst\" and a Literal"
                + SOURCE_ARM);
    }

    @Test void a_parameter_as_source_of_the_sum_is_refused() throws Exception {
        refuses("wf037-som-elementtype-parameter", "a sum aggregation carries elementType \"Vondst\" and a"
                + " ParameterReference" + SOURCE_ARM);
    }

    @Test void a_literal_as_own_read_of_the_total_is_refused() throws Exception {
        // a total without a range: the argument is the subject's own read, and a literal is no read. Before: the
        // verifier passed it, and no branch ran it (transpiler SKIP, DRL marker)
        refuses("wf037-totaal-elementtype-literaal", "a total aggregation carries elementType \"Jutter\" and a Literal"
                + SOURCE_ARM);
    }

    // ---------------- 2. the converse arm: no argument, or one ListLiteral ----------------

    @Test void a_count_without_argument_is_refused() throws Exception {
        refuses("wf037-aantal-elementtype-zonder-operand", "a count aggregation carries elementType \"Vondst\""
                + NO_SOURCE);
    }

    @Test void a_count_without_argument_beside_its_range_is_refused() throws Exception {
        // the variant that PLAYED in both branches before: the range written, the argument list empty
        ObjectNode ir = negative("wf037-aantal-elementtype-zonder-operand");
        ((ObjectNode) value(ir)).putObject("sourceRole").put("node", "RoleBinding")
                .put("factType", "vondstaanmelding").put("role", "vondst");
        refuses(ir, "a count aggregation carries elementType \"Vondst\"" + NO_SOURCE);
    }

    @Test void a_filled_count_without_argument_is_refused() throws Exception {
        refuses("wf037-gevuldetelling-elementtype-zonder-operand", "a filledCount aggregation carries elementType"
                + " \"Vondst\"" + NO_SOURCE);
    }

    @Test void a_sum_without_argument_is_refused() throws Exception {
        refuses("wf037-som-elementtype-zonder-operand", "a sum aggregation carries elementType \"Vondst\""
                + NO_SOURCE);
    }

    @Test void a_list_literal_as_count_source_is_refused_as_form_and_not_as_node() throws Exception {
        refuses("wf037-aantal-elementtype-lijstliteraal", "a count aggregation carries elementType \"Vondst\" and a"
                + " ListLiteral operand: a listed value is no source of instances");
    }

    @Test void a_list_literal_as_source_of_the_filled_count_is_refused() throws Exception {
        refuses("wf037-gevuldetelling-elementtype-lijstliteraal", "a filledCount aggregation carries elementType"
                + " \"Vondst\" and a ListLiteral operand: a listed value is no source of instances");
    }

    // ---------------- 3. WF-110 (c): the count over a union names no source of its own ----------------

    @Test void an_element_type_with_range_beside_the_union_is_refused() throws Exception {
        // before: PLAYED in both branches, the union counted and the node's own source never read
        refuses("wf110-unie-naast-vouwbron", UNION + "elementType + sourceRole" + UNION_TAIL);
    }

    @Test void a_range_only_beside_the_union_is_refused() throws Exception {
        ObjectNode ir = negative("wf110-unie-naast-vouwbron");
        ((ObjectNode) value(ir)).remove("elementType");
        refuses(ir, UNION + "sourceRole" + UNION_TAIL);
    }

    @Test void a_filter_only_beside_the_union_is_refused() throws Exception {
        ObjectNode ir = negative("wf110-unie-naast-vouwbron");
        ObjectNode count = (ObjectNode) value(ir);
        count.remove("elementType");
        count.remove("sourceRole");
        count.set("filter", JSON.readTree("{ \"node\": \"Comparison\", \"op\": \"greater\","
                + " \"left\": { \"node\": \"Literal\", \"value\": { \"number\": \"1\" }, \"type\": " + WHOLE + " },"
                + " \"right\": { \"node\": \"Literal\", \"value\": { \"number\": \"0\" }, \"type\": " + WHOLE + " } }"));
        refuses(ir, UNION + "filter" + UNION_TAIL);
    }

    // ---------------- 4. the order: the range is asked before the source, as ref and the ASP backend ask it --------

    /** A count beside a path carries two defects, the path (WF-102 (c)) and the list literal (WF-037), and every
     *  executor names the path first. Before the fix this loader named the list literal first: it judged that node
     *  before the range, while ref and the ASP backend judged it after. */
    @Test void a_list_literal_beside_a_source_path_is_first_refused_as_source_path() throws Exception {
        ObjectNode ir = negative("wf037-aantal-elementtype-lijstliteraal");
        withPath((ObjectNode) value(ir), 2);
        refusedAt(ir, SITE + "/sourcePath", SOURCE_PATH_C);
    }

    @Test void a_list_literal_as_source_of_the_filled_count_beside_a_source_path_is_first_refused_as_source_path()
            throws Exception {
        ObjectNode ir = negative("wf037-gevuldetelling-elementtype-lijstliteraal");
        withPath((ObjectNode) value(ir), 2);
        refusedAt(ir, SITE + "/sourcePath", SOURCE_PATH_C);
    }

    /** A value fold's path of one step beside a source that is no attribute reference: ref and the ASP backend ask
     *  the position (WF-102 (c)) before the number of steps, and this loader now does too. Before the fix it named the
     *  steps first, as a form it does not carry. */
    @Test void a_literal_beside_a_source_path_of_a_step_is_refused_as_source_path() throws Exception {
        ObjectNode ir = negative("wf037-som-elementtype-literaal");
        withPath((ObjectNode) value(ir), 1);
        refusedAt(ir, SITE + "/sourcePath", SOURCE_PATH_C);
    }

    /** No range at all beside a list literal: the shared loader refuses the missing range while loading, before any
     *  source question, and so does this loader now. Before the fix it named the list literal. */
    @Test void a_list_literal_without_range_is_refused_for_the_range() throws Exception {
        ObjectNode ir = negative("wf037-aantal-elementtype-lijstliteraal");
        ((ObjectNode) value(ir)).remove("sourceRole");
        refusedAt(ir, SITE, "a single-argument fold with an elementType carries its range (sourceRole or sourcePath)"
                + " under every kind but total: the range is written, never derived");
    }

    // ---------------- 5. the boundary ----------------

    @Test void a_count_over_its_source_loads() throws Exception {
        // the golden's count source, an AttributeReference beside its range: the arm leaves it alone
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(
                GOLDEN.resolve("jutterswet-vondsttelling").resolve("ir.json")));
        assertDoesNotThrow(() -> IrLoader.loadAll(ir));
    }

    // ---------------- fixtures ----------------

    private static ObjectNode negative(String name) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(NEGATIEF.resolve(name).resolve("ir.json")));
    }

    private static com.fasterxml.jackson.databind.JsonNode value(ObjectNode ir) {
        return ir.get("rules").get("rules").get(0).get("versions").get(0).get("value");
    }

    private static void refuses(String name, String sentence) throws Exception {
        refuses(negative(name), sentence);
    }

    /** The load refusal, for both branches: the loader is theirs in common, so nothing is lowered. */
    private static void refuses(ObjectNode ir, String sentence) {
        refusedAt(ir, SITE, sentence);
    }

    private static void refusedAt(ObjectNode ir, String site, String sentence) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir));
        assertEquals(site, e.site(), e.getMessage());
        assertEquals("malformed uchar-ir-v1 document at '" + site + "': " + sentence, e.getMessage());
    }

    /** The fold's range written as a path of {@code steps} steps in place of its sourceRole. */
    private static void withPath(ObjectNode fold, int steps) {
        fold.remove("sourceRole");
        var path = fold.putArray("sourcePath");
        for (int i = 0; i < steps; i++)
            path.addObject().put("node", "RoleBinding").put("factType", "vondstaanmelding").put("role", "vondst");
    }
}
