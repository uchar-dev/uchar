package dev.uchar.regelspraak.graaf;

import dev.uchar.regelspraak.graaf.ProducentConsument.Vormsoort;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.uchar.regelspraak.graaf.TweeRoutes.kanten;
import static dev.uchar.regelspraak.graaf.TweeRoutes.knoop;
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
class GraafVormTest {

    /** "form name (kind)" of both ends of every dependency, as "producent -> lid -> consument". */
    private static List<String> afhankelijkheden(RegelGraaf g) {
        return g.afhankelijkheden().stream().map(a -> a.producent() + " (" + a.producentVorm() + ") -> "
                + a.element().naam() + " -> " + a.consument() + " (" + a.consumentVorm() + ")").toList();
    }

    @Test void een_defaultsgroep_schrijft_haar_doel_en_leest_haar_rechtvaardigingen() {
        String bron = """
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
        TweeRoutes.lees(bron).grafen().forEach((route, g) -> {
            assertEquals(RegelGraaf.NodeKind.KENMERK, knoop(g, "uitgesloten · jutter").kind(),
                    route + ": de defaultsgroep schrijft het kenmerk, dus het is geen invoer");
            assertEquals(List.of("uitsluiting van het penningrecht (DEFAULTSGROEP) -> uitgesloten -> "
                    + "penninggerechtigd tenzij uitgesloten (REGEL)"), afhankelijkheden(g), route);
            // the justification of every default is a read of the group, the exception's included
            assertTrue(kanten(g).containsAll(List.of("vergunning geweigerd · jutter -> uitsluiting van het penningrecht",
                    "weigering herroepen · jutter -> uitsluiting van het penningrecht")), () -> route + ": " + kanten(g));
        });
    }

    @Test void een_objectcreatie_schrijft_de_leden_die_zij_toekent() {
        String bron = """
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
        TweeRoutes.lees(bron).grafen().forEach((route, g) -> {
            assertEquals(RegelGraaf.NodeKind.ATTRIBUUT, knoop(g, "grondslag · strandrecht").kind(),
                    route + ": de creatie kent de grondslag toe, dus die is geen invoer");
            assertEquals(List.of("boek een strandrecht bij elk wrakrecht (OBJECTCREATIE) -> grondslag -> "
                    + "uitkering van een strandrecht (REGEL)"), afhankelijkheden(g), route);
            // the assigned value is read against the creation's subject
            assertTrue(kanten(g).contains("bergingsvergoeding · wrakrecht -> boek een strandrecht bij elk wrakrecht"),
                    () -> route + ": " + kanten(g));
        });
    }

    @Test void een_verdeling_leest_haar_bron_en_schrijft_haar_doel() {
        String bron = """
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
        TweeRoutes.lees(bron).grafen().forEach((route, g) -> {
            assertEquals(RegelGraaf.NodeKind.ATTRIBUUT, knoop(g, "bergingsaandeel · berger").kind(), route);
            assertFalse(knoop(g, "opbrengst · berging").eindOutput(), route + ": de verdeling leest de opbrengst");
            assertEquals(List.of(
                    "opbrengst van een berging (REGEL) -> opbrengst -> verdeel de opbrengst van een berging (VERDELING)",
                    "verdeel de opbrengst van een berging (VERDELING) -> bergingsaandeel -> loon van een berger (REGEL)"),
                    afhankelijkheden(g), route);
        });
    }

    @Test void een_consistentie_en_een_uniciteit_lezen_hun_leden() {
        String bron = """
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
        TweeRoutes.lees(bron).grafen().forEach((route, g) -> {
            assertFalse(knoop(g, "registernummer · vondst").eindOutput(), route + ": twee controles lezen het");
            assertEquals(List.of(
                    "registernummer van een vondst (REGEL) -> registernummer -> registernummer onder de grens (CONSISTENTIE)",
                    "registernummer van een vondst (REGEL) -> registernummer -> eenmalig registernummer (UNICITEIT)"),
                    afhankelijkheden(g), route);
        });
    }

    @Test void een_feitcreatie_leest_haar_voorwaarde() {
        String bron = """
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
        TweeRoutes.lees(bron).grafen().forEach((route, g) -> {
            assertFalse(knoop(g, "gehesen · sein").eindOutput(), route + ": de feitcreatie leest het kenmerk");
            assertEquals(List.of("hijsing naar hoogte (REGEL) -> gehesen -> vakbeseining door het kustvak (FEITCREATIE)"),
                    afhankelijkheden(g), route);
            assertEquals(Vormsoort.FEITCREATIE, g.afhankelijkheden().get(0).consumentVorm(), route);
        });
    }
}
