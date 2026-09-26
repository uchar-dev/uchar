package dev.uchar.regelspraak.graaf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.uchar.regelspraak.graaf.TweeRoutes.kanten;
import static dev.uchar.regelspraak.graaf.TweeRoutes.knoop;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a rule reads, per kind of read, on both routes: the navigated read (a kenmerk check or an attribuut read over
 * a hop), the kenmerk read inside a fold's filter, and the fold source — a value fold reads an attribuut of every
 * contributor, a count names the relation's role and reads no member at all.
 */
class GraafLezingTest {

    private static final String POSTEN = """
        Objecttype de Wachter (mv: Wachters) (bezield)
            is seingerechtigd kenmerk (bijvoeglijk);
            de uitkijkbonus Numeriek (geheel getal);
        Objecttype de Post (mv: Posten)
            is risicopost kenmerk (bijvoeglijk);
            de hoogte Numeriek (geheel getal);
        Feittype postbezetting
            de wachter (mv: wachters) Wachter
            de post Post
        Eén post telt meerdere wachters
        Regel risicopost bij hoogte
            geldig altijd
                Een Post is risicopost indien de hoogte van de Post groter is dan 10.
        Regel seingerechtigd bij risicopost
            geldig altijd
                Een Wachter is seingerechtigd indien zijn post risicopost is.
        Regel uitkijkbonus van een wachter
            geldig altijd
                De uitkijkbonus van een Wachter moet gesteld worden op de hoogte van zijn post.
        """;

    @Test void de_genavigeerde_kenmerkcheck_leest_het_kenmerk_van_het_bereikte_type() {
        TweeRoutes.lees(POSTEN).grafen().forEach((route, g) -> {
            assertTrue(kanten(g).contains("risicopost · post -> seingerechtigd bij risicopost"), () -> route + ": " + kanten(g));
            assertFalse(knoop(g, "risicopost · post").eindOutput(), route + ": de genavigeerde check leest het kenmerk");
            assertTrue(g.nodes().stream().noneMatch(n -> n.label().equals("risicopost · wachter")),
                    () -> route + ": de check leest niet het eigen kenmerk van de wachter: " + g.nodes());
            assertTrue(g.afhankelijkheden().stream().anyMatch(a -> a.producent().equals("risicopost bij hoogte")
                    && a.consument().equals("seingerechtigd bij risicopost")), route);
        });
    }

    @Test void de_genavigeerde_attribuutlezing_leest_het_attribuut_van_het_bereikte_type() {
        TweeRoutes.lees(POSTEN).grafen().forEach((route, g) -> assertTrue(
                kanten(g).contains("hoogte · post -> uitkijkbonus van een wachter"), () -> route + ": " + kanten(g)));
    }

    @Test void het_navigatiepredicaat_van_de_andere_route_leest_de_check_als_eigen_kenmerk() {
        // Why the caller answers the navigation question: a wire model's records carry no hop (it rides beside the
        // model), so the bron route's answer, read on a wire model, takes every navigated check for the subject's
        // own kenmerk. The graph then draws a kenmerk of the Wachter nobody writes, and the Post's kenmerk nobody
        // reads.
        TweeRoutes.Tweeling t = TweeRoutes.lees(POSTEN);
        RegelGraaf fout = RegelGraaf.bouw(t.wire().model(), k -> k.navigatie() != null);
        assertEquals(RegelGraaf.NodeKind.INVOER, knoop(fout, "risicopost · wachter").kind());
        assertTrue(knoop(fout, "risicopost · post").eindOutput());
    }

    @Test void een_genavigeerde_check_op_een_kenmerk_van_twee_declaranten_leest_beide() {
        // The record does not name the type the hop reaches, only the kenmerk; with one declarant that is exact,
        // with two the check counts as a read of each — more rather than less, as the chain key reports.
        String bron = POSTEN.replace("""
                Feittype postbezetting""", """
                Objecttype het Kustvak (mv: Kustvakken)
                    is risicopost kenmerk (bijvoeglijk);
                Feittype postbezetting""");
        TweeRoutes.lees(bron).grafen().forEach((route, g) -> {
            assertTrue(kanten(g).contains("risicopost · post -> seingerechtigd bij risicopost"), () -> route + ": " + kanten(g));
            assertTrue(kanten(g).contains("risicopost · kustvak -> seingerechtigd bij risicopost"), () -> route + ": " + kanten(g));
        });
    }

    private static final String VONDSTEN = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het pronkstukaantal Numeriek (geheel getal);
            het vondstgewicht Numeriek (geheel getal);
            het vondstaantal Numeriek (geheel getal);
        Objecttype de Vondst (mv: Vondsten)
            is waardevol kenmerk (bijvoeglijk);
            het gewicht Numeriek (geheel getal);
        Feittype aanmelding
            de aanmeldende jutter Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmeldende jutter telt meerdere vondsten
        Regel waardevol bij gewicht
            geldig altijd
                Een Vondst is waardevol indien het gewicht van de Vondst groter is dan 10.
        Regel pronkstukaantal van een jutter
            geldig altijd
                Het pronkstukaantal van een Jutter moet berekend worden als het aantal vondsten van de Jutter die waardevol zijn.
        Regel vondstgewicht van een jutter
            geldig altijd
                Het vondstgewicht van een Jutter moet berekend worden als de som van het gewicht van alle vondsten van de Jutter of 0 als die er niet zijn.
        Regel vondstaantal van een jutter
            geldig altijd
                Het vondstaantal van een Jutter moet berekend worden als het aantal vondsten van de Jutter.
        """;

    @Test void de_kenmerkcheck_in_een_vouwfilter_leest_het_kenmerk_van_het_element() {
        // the filter is evaluated per contributor, with the Vondst as its subject, not the Jutter the rule ranges over
        TweeRoutes.lees(VONDSTEN).grafen().forEach((route, g) -> {
            assertTrue(kanten(g).contains("waardevol · vondst -> pronkstukaantal van een jutter"), () -> route + ": " + kanten(g));
            assertTrue(g.nodes().stream().noneMatch(n -> n.label().equals("waardevol · jutter")),
                    () -> route + ": " + g.nodes());
            assertFalse(knoop(g, "waardevol · vondst").eindOutput(), route);
        });
    }

    @Test void de_bron_van_een_waardevouw_leest_het_attribuut_van_elk_element() {
        TweeRoutes.lees(VONDSTEN).grafen().forEach((route, g) -> assertTrue(
                kanten(g).contains("gewicht · vondst -> vondstgewicht van een jutter"), () -> route + ": " + kanten(g)));
    }

    @Test void de_bron_van_een_telling_noemt_de_rol_en_leest_geen_lid() {
        // "het aantal vondsten van de Jutter": the source names the relation's role, which no type declares as a
        // member, so it is no read of an attribuut — before, the graph drew it as an input nobody could ever write
        TweeRoutes.lees(VONDSTEN).grafen().forEach((route, g) -> {
            assertTrue(g.nodes().stream().noneMatch(n -> n.label().startsWith("vondsten")), () -> route + ": " + g.nodes());
            List<String> naarTelling = kanten(g).stream().filter(k -> k.endsWith("-> vondstaantal van een jutter")).toList();
            assertEquals(List.of(), naarTelling, route + ": een telling zonder filter leest niets");
        });
    }
}
