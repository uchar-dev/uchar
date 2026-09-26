package dev.uchar.regelspraak.graaf;

import dev.uchar.regelspraak.graaf.RegelGraaf.Node;
import dev.uchar.regelspraak.graaf.RegelGraaf.NodeKind;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static dev.uchar.regelspraak.graaf.TweeRoutes.kanten;
import static dev.uchar.regelspraak.graaf.TweeRoutes.knoop;
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
class GraafTest {

    static final String BRON = """
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

    @Test void knopen_krijgen_de_juiste_soort() {
        TweeRoutes.lees(BRON).grafen().forEach((route, g) -> {
            // three rules + four data elements = seven nodes
            assertEquals(7, g.nodes().size(), () -> route + ": " + g.nodes());
            assertEquals(3, g.nodes().stream().filter(n -> n.kind() == NodeKind.REGEL).count(), route);
            assertEquals(NodeKind.INVOER, knoop(g, "gewicht · vondst").kind(),
                    route + ": een datum die geen regel produceert is een invoer");
            assertEquals(NodeKind.KENMERK, knoop(g, "zwaar · vondst").kind(),
                    route + ": 'is zwaar' is een kenmerk, geen gewoon attribuut");
            assertEquals(NodeKind.ATTRIBUUT, knoop(g, "toeslag · vondst").kind(), route);
            assertEquals(NodeKind.ATTRIBUUT, knoop(g, "bergloon · vondst").kind(), route);
        });
    }

    @Test void productloze_eind_output_wordt_gemarkeerd() {
        TweeRoutes.lees(BRON).grafen().forEach((route, g) -> {
            // "bergloon" is produced by "bergloon van een vondst" and read by no rule: the only eind-output
            assertTrue(knoop(g, "bergloon · vondst").eindOutput(), route + ": bergloon wordt door niets gelezen");
            assertEquals(1, g.nodes().stream().filter(Node::eindOutput).count(), route);
            // an INVOER is never an eind-output, and a consumed attribuut is not one either
            assertFalse(knoop(g, "gewicht · vondst").eindOutput(), route);
            assertFalse(knoop(g, "toeslag · vondst").eindOutput(), route);
        });
    }

    @Test void kanten_volgen_de_dataflow_richting() {
        TweeRoutes.lees(BRON).grafen().forEach((route, g) -> {
            List<String> k = kanten(g);
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

    @Test void een_regel_leest_zijn_eigen_doel_niet_als_afhankelijkheid() {
        // "bergloon van een vondst" writes "bergloon" and reads "gewicht" + "toeslag"; the target it writes is no
        // read of its own
        TweeRoutes.lees(BRON).grafen().forEach((route, g) -> assertFalse(
                kanten(g).contains("bergloon · vondst -> bergloon van een vondst"),
                route + ": een regel heeft geen consumentkant naar zijn eigen productie"));
    }

    @Test void de_afhankelijkheden_zijn_de_paden_regel_element_regel() {
        TweeRoutes.lees(BRON).grafen().forEach((route, g) -> assertEquals(Set.of(
                "zwaar bij gewicht -> zwaar -> toeslag van een zware vondst",
                "toeslag van een zware vondst -> toeslag -> bergloon van een vondst"),
                Set.copyOf(g.afhankelijkheden().stream()
                        .map(a -> a.producent() + " -> " + a.element().naam() + " -> " + a.consument()).toList()),
                route));
    }

    @Test void mermaid_bevat_alle_knopen_en_klassen() {
        // a light render-shape check: a flowchart carrying every node id and the class definitions (the exact
        // styling is the render's concern, not the extraction's)
        TweeRoutes.lees(BRON).grafen().forEach((route, g) -> {
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
