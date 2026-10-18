package dev.uchar.regelspraak.graph;

import dev.uchar.regelspraak.graph.ProducerConsumer.FormKind;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.uchar.regelspraak.graph.TwoRoutes.edges;
import static dev.uchar.regelspraak.graph.TwoRoutes.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The forms of the rule layer beside the rules, on both routes: a defaults group and an object creation write a
 * member, an allocation writes and reads members, a consistency, a uniqueness and a fact creation read them
 * (IR-SPEC ch. 5.1, RUNTIME-SEMANTICS ch. 6.1). Drawn with the rules alone, a member that one of these forms writes
 * was an INVOER nobody could write, and a member that one of them reads was an eind-output nobody reads; a scope
 * check over the same producer index then took the member for request input.
 */
class GraphFormTest {

    /** "form name (kind)" of both ends of every dependency, as "producent -> lid -> consument". */
    private static List<String> dependencies(RuleGraph g) {
        return g.dependencies().stream().map(a -> a.producer() + " (" + a.producerForm() + ") -> "
                + a.element().naam() + " -> " + a.consumer() + " (" + a.consumerForm() + ")").toList();
    }

    @Test void a_defaults_group_writes_its_target_and_reads_its_justifications() {
        String source = """
            Objecttype de Jutter (mv: Jutters) (bezield)
                is vergunning geweigerd kenmerk (bijvoeglijk);
                is weigering herroepen kenmerk (bijvoeglijk);
                is uitgesloten kenmerk (bijvoeglijk);
                is penninggerechtigd kenmerk (bijvoeglijk);
            Regel uitsluiting van het penningrecht
                geldig altijd
                    Een Jutter is uitgesloten
                    indien hij vergunning geweigerd is.
            Uitzondering op uitsluiting van het penningrecht: herroepen weigering
                geldig altijd
                    Een Jutter is niet uitgesloten
                    indien hij weigering herroepen is.
            Regel penninggerechtigd tenzij uitgesloten
                geldig altijd
                    Een Jutter is penninggerechtigd
                    indien hij niet uitgesloten is.
            """;
        TwoRoutes.read(source).graphs().forEach((route, g) -> {
            assertEquals(RuleGraph.NodeKind.CHARACTERISTIC, node(g, "uitgesloten · jutter").kind(),
                    route + ": de defaultsgroep schrijft het kenmerk, dus het is geen invoer");
            assertEquals(List.of("uitsluiting van het penningrecht (DEFAULTSGROEP) -> uitgesloten -> "
                    + "penninggerechtigd tenzij uitgesloten (REGEL)"), dependencies(g), route);
            // the justification of every default is a read of the group, the exception's included
            assertTrue(edges(g).containsAll(List.of("vergunning geweigerd · jutter -> uitsluiting van het penningrecht",
                    "weigering herroepen · jutter -> uitsluiting van het penningrecht")), () -> route + ": " + edges(g));
        });
    }

    @Test void an_object_creation_writes_the_members_that_it_assigns() {
        String source = """
            Objecttype het Wrakrecht (mv: Wrakrechten)
                de bergingsvergoeding Numeriek (niet-negatief getal met 2 decimalen);
            Objecttype het Strandrecht (mv: Strandrechten)
                de grondslag Numeriek (niet-negatief getal met 2 decimalen);
                de uitkering Numeriek (niet-negatief getal met 2 decimalen);
            Feittype wrakboeking
                de boeker Wrakrecht
                het geboekt strandrecht Strandrecht
            Eén boeker boekt één geboekt strandrecht
            Regel boek een strandrecht bij elk wrakrecht
                geldig altijd
                    Een Wrakrecht heeft een geboekt strandrecht met grondslag gelijk aan zijn bergingsvergoeding.
            Regel uitkering van een strandrecht
                geldig altijd
                    De uitkering van een Strandrecht moet gesteld worden op zijn grondslag.
            """;
        TwoRoutes.read(source).graphs().forEach((route, g) -> {
            assertEquals(RuleGraph.NodeKind.ATTRIBUTE, node(g, "grondslag · strandrecht").kind(),
                    route + ": de creatie kent de grondslag toe, dus die is geen invoer");
            assertEquals(List.of("boek een strandrecht bij elk wrakrecht (OBJECTCREATIE) -> grondslag -> "
                    + "uitkering van een strandrecht (REGEL)"), dependencies(g), route);
            // the assigned value is read against the creation's subject
            assertTrue(edges(g).contains("bergingsvergoeding · wrakrecht -> boek een strandrecht bij elk wrakrecht"),
                    () -> route + ": " + edges(g));
        });
    }

    @Test void an_allocation_reads_its_source_and_writes_its_target() {
        String source = """
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €
            Objecttype de Berging (mv: Bergingen)
                de bruto opbrengst Numeriek (geheel getal) met eenheid €;
                de opbrengst Numeriek (geheel getal) met eenheid €;
            Objecttype de Berger (mv: Bergers) (bezield)
                het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;
                het loon Numeriek (geheel getal) met eenheid €;
            Feittype bergingsdeelname
                de berging Berging
                de berger (mv: bergers) Berger
            Eén berging heeft meerdere bergers
            Regel opbrengst van een berging
                geldig altijd
                    De opbrengst van een Berging moet gesteld worden op de bruto opbrengst van de Berging.
            Regel verdeel de opbrengst van een berging
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld:
                    - in gelijke delen,
                    - afgerond op 0 decimalen naar beneden.
            Regel loon van een berger
                geldig altijd
                    Het loon van een Berger moet gesteld worden op zijn bergingsaandeel.
            """;
        TwoRoutes.read(source).graphs().forEach((route, g) -> {
            assertEquals(RuleGraph.NodeKind.ATTRIBUTE, node(g, "bergingsaandeel · berger").kind(), route);
            assertFalse(node(g, "opbrengst · berging").endOutput(), route + ": de verdeling leest de opbrengst");
            assertEquals(List.of(
                    "opbrengst van een berging (REGEL) -> opbrengst -> verdeel de opbrengst van een berging (VERDELING)",
                    "verdeel de opbrengst van een berging (VERDELING) -> bergingsaandeel -> loon van een berger (REGEL)"),
                    dependencies(g), route);
        });
    }

    @Test void a_consistency_and_a_uniqueness_read_their_members() {
        String source = """
            Objecttype de Vondst (mv: Vondsten)
                het vondstnummer Numeriek (geheel getal);
                het registernummer (mv: registernummers) Numeriek (geheel getal);
            Regel registernummer van een vondst
                geldig altijd
                    Het registernummer van een Vondst moet gesteld worden op het vondstnummer van de Vondst.
            Regel registernummer onder de grens
                geldig altijd
                    Het registernummer van een Vondst moet kleiner zijn dan 1000.
            Regel eenmalig registernummer
                geldig altijd
                    De registernummers van alle Vondsten moeten uniek zijn.
            """;
        TwoRoutes.read(source).graphs().forEach((route, g) -> {
            assertFalse(node(g, "registernummer · vondst").endOutput(), route + ": twee controles lezen het");
            assertEquals(List.of(
                    "registernummer van een vondst (REGEL) -> registernummer -> registernummer onder de grens (CONSISTENTIE)",
                    "registernummer van een vondst (REGEL) -> registernummer -> eenmalig registernummer (UNICITEIT)"),
                    dependencies(g), route);
        });
    }

    @Test void a_fact_creation_reads_its_condition() {
        String source = """
            Objecttype het Kustvak (mv: Kustvakken)
                het vakseinaantal Numeriek (geheel getal);
            Objecttype de Post (mv: Posten)
                de naam Tekst;
            Objecttype de Mast (mv: Masten)
                de naam Tekst;
            Objecttype het Sein (mv: Seinen)
                de hoogte Numeriek (geheel getal);
                is gehesen kenmerk (bijvoeglijk);
            Feittype kustligging
                het kustvak Kustvak
                de post (mv: posten) Post
            Eén kustvak telt meerdere posten
            Feittype mastvoering
                de post Post
                de mast (mv: masten) Mast
            Eén post telt meerdere masten
            Feittype seinvoering
                de mast Mast
                het sein (mv: seinen) Sein
            Eén mast telt meerdere seinen
            Feittype vakbeseining
                het seinvak Kustvak
                het vaksein (mv: vakseinen) Sein
            Eén seinvak telt meerdere vakseinen
            Regel hijsing naar hoogte
                geldig altijd
                    Een Sein is gehesen indien de hoogte van het Sein groter is dan 2.
            Regel vakbeseining door het kustvak
                geldig altijd
                    Een vaksein van een Kustvak is een sein van de mast van de post van het kustvak
                    indien het sein gehesen is.
            """;
        TwoRoutes.read(source).graphs().forEach((route, g) -> {
            assertFalse(node(g, "gehesen · sein").endOutput(), route + ": de feitcreatie leest het kenmerk");
            assertEquals(List.of("hijsing naar hoogte (REGEL) -> gehesen -> vakbeseining door het kustvak (FEITCREATIE)"),
                    dependencies(g), route);
            assertEquals(FormKind.FEITCREATIE, g.dependencies().get(0).consumerForm(), route);
        });
    }
}
