package dev.uchar.regelspraak.graph;

import dev.uchar.regelspraak.graph.RuleGraph.Node;
import dev.uchar.regelspraak.graph.RuleGraph.NodeKind;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static dev.uchar.regelspraak.graph.TwoRoutes.edges;
import static dev.uchar.regelspraak.graph.TwoRoutes.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The regelgraaf extraction on a model that yields exactly one node of each kind, so a regression in colouring or
 * direction shows at the extraction, before any render, on both routes to the model.
 *
 * <p>The chain: "gewicht" is read and written by no rule (an INVOER); "zwaar bij gewicht" writes the KENMERK
 * "zwaar", which "toeslag van een zware vondst" reads to write the ATTRIBUUT "toeslag"; "bergloon van een vondst"
 * reads "gewicht" and "toeslag" to write "bergloon", which no rule reads (a product-less eind-output).
 */
class GraphTest {

    static final String SOURCE = """
        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);
            de toeslag Numeriek (geheel getal);
            het bergloon Numeriek (geheel getal);
            is zwaar kenmerk (bijvoeglijk);
        Regel zwaar bij gewicht
            geldig altijd
                Een Vondst is zwaar indien het gewicht van de Vondst groter is dan 100.
        Regel toeslag van een zware vondst
            geldig altijd
                De toeslag van een Vondst moet gesteld worden op 10 indien de Vondst zwaar is.
        Regel bergloon van een vondst
            geldig altijd
                Het bergloon van een Vondst moet berekend worden als het gewicht van de Vondst plus de toeslag van de Vondst.
        """;

    @Test void nodes_get_the_correct_kind() {
        TwoRoutes.read(SOURCE).graphs().forEach((route, g) -> {
            // three rules + four data elements = seven nodes
            assertEquals(7, g.nodes().size(), () -> route + ": " + g.nodes());
            assertEquals(3, g.nodes().stream().filter(n -> n.kind() == NodeKind.RULE).count(), route);
            assertEquals(NodeKind.INPUT, node(g, "gewicht · vondst").kind(),
                    route + ": een datum die geen regel produceert is een invoer");
            assertEquals(NodeKind.CHARACTERISTIC, node(g, "zwaar · vondst").kind(),
                    route + ": 'is zwaar' is een kenmerk, geen gewoon attribuut");
            assertEquals(NodeKind.ATTRIBUTE, node(g, "toeslag · vondst").kind(), route);
            assertEquals(NodeKind.ATTRIBUTE, node(g, "bergloon · vondst").kind(), route);
        });
    }

    @Test void productless_end_output_is_marked() {
        TwoRoutes.read(SOURCE).graphs().forEach((route, g) -> {
            // "bergloon" is produced by "bergloon van een vondst" and read by no rule: the only eind-output
            assertTrue(node(g, "bergloon · vondst").endOutput(), route + ": bergloon wordt door niets gelezen");
            assertEquals(1, g.nodes().stream().filter(Node::endOutput).count(), route);
            // an INVOER is never an eind-output, and a consumed attribuut is not one either
            assertFalse(node(g, "gewicht · vondst").endOutput(), route);
            assertFalse(node(g, "toeslag · vondst").endOutput(), route);
        });
    }

    @Test void edges_follow_the_dataflow_direction() {
        TwoRoutes.read(SOURCE).graphs().forEach((route, g) -> {
            List<String> k = edges(g);
            // producer edges: rule -> the datum it writes; consumer edges: the datum -> the rule that reads it
            assertEquals(Set.of(
                    "zwaar bij gewicht -> zwaar · vondst",
                    "toeslag van een zware vondst -> toeslag · vondst",
                    "bergloon van een vondst -> bergloon · vondst",
                    "gewicht · vondst -> zwaar bij gewicht",
                    "zwaar · vondst -> toeslag van een zware vondst",
                    "gewicht · vondst -> bergloon van een vondst",
                    "toeslag · vondst -> bergloon van een vondst"), Set.copyOf(k), route);
            assertEquals(7, k.size(), () -> route + ": precies de zeven dataflowkanten, zonder herhaling: " + k);
        });
    }

    @Test void a_rule_does_not_read_its_own_target_as_a_dependency() {
        // "bergloon van een vondst" writes "bergloon" and reads "gewicht" + "toeslag"; the target it writes is no
        // read of its own
        TwoRoutes.read(SOURCE).graphs().forEach((route, g) -> assertFalse(
                edges(g).contains("bergloon · vondst -> bergloon van een vondst"),
                route + ": een regel heeft geen consumentkant naar zijn eigen productie"));
    }

    @Test void the_dependencies_are_the_paths_rule_element_rule() {
        TwoRoutes.read(SOURCE).graphs().forEach((route, g) -> assertEquals(Set.of(
                "zwaar bij gewicht -> zwaar -> toeslag van een zware vondst",
                "toeslag van een zware vondst -> toeslag -> bergloon van een vondst"),
                Set.copyOf(g.dependencies().stream()
                        .map(a -> a.producer() + " -> " + a.element().name() + " -> " + a.consumer()).toList()),
                route));
    }

    @Test void mermaid_contains_all_nodes_and_classes() {
        // a light render-shape check: a flowchart carrying every node id and the class definitions (the exact
        // styling is the render's concern, not the extraction's)
        TwoRoutes.read(SOURCE).graphs().forEach((route, g) -> {
            String mmd = g.mermaid();
            assertTrue(mmd.startsWith("flowchart LR"), () -> route + ": " + mmd);
            assertTrue(mmd.contains("classDef invoer"), route + ": invoerklasse ontbreekt");
            assertTrue(mmd.contains("classDef kenmerk"), route + ": kenmerkklasse ontbreekt");
            assertTrue(mmd.contains("eindoutput"), route + ": eind-outputmarkering ontbreekt");
            for (Node n : g.nodes())
                assertTrue(mmd.contains("  " + n.id()), () -> route + ": knoop " + n.id() + " (" + n.label() + ") niet in mermaid");
        });
    }
}
