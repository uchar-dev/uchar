package dev.uchar.regelspraak.graaf;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.uchar.regelspraak.graaf.TwoRoutes.edges;
import static dev.uchar.regelspraak.graaf.TwoRoutes.jsonNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a rule reads, per kind of read, on both routes: the navigated read (a kenmerk check or an attribuut read over
 * a hop), the kenmerk read inside a fold's filter, and the fold source — a value fold reads an attribuut of every
 * contributor, a count names the relation's role and reads no member at all.
 */
class GraphReadTest {

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

    @Test void the_navigated_characteristic_check_reads_the_characteristic_of_the_reached_type() {
        TwoRoutes.lees(POSTEN).graphs().forEach((route, g) -> {
            assertTrue(edges(g).contains("risicopost · post -> seingerechtigd bij risicopost"), () -> route + ": " + edges(g));
            assertFalse(jsonNode(g, "risicopost · post").eindOutput(), route + ": de genavigeerde check leest het kenmerk");
            assertTrue(g.nodes().stream().noneMatch(n -> n.label().equals("risicopost · wachter")),
                    () -> route + ": de check leest niet het eigen kenmerk van de wachter: " + g.nodes());
            assertTrue(g.dependencies().stream().anyMatch(a -> a.producer().equals("risicopost bij hoogte")
                    && a.consumer().equals("seingerechtigd bij risicopost")), route);
        });
    }

    @Test void the_navigated_attribute_read_reads_the_attribute_of_the_reached_type() {
        TwoRoutes.lees(POSTEN).graphs().forEach((route, g) -> assertTrue(
                edges(g).contains("hoogte · post -> uitkijkbonus van een wachter"), () -> route + ": " + edges(g)));
    }

    @Test void the_navigation_of_the_other_route_reads_the_check_as_its_own_characteristic() {
        // Why the caller answers the navigation question: a wire model's records carry no hop (it rides beside the
        // model), so the bron route's answer, read on a wire model, takes every navigated check for the subject's
        // own kenmerk. The graph then draws a kenmerk of the Wachter nobody writes, and the Post's kenmerk nobody
        // reads.
        TwoRoutes.Twin t = TwoRoutes.lees(POSTEN);
        RegelGraaf fout = RegelGraaf.bouw(t.wire().model(), t.wire().prioritizedDefaults(),
                ProducentConsument.bronNavigatie(t.wire().model()));
        assertEquals(RegelGraaf.NodeKind.INVOER, jsonNode(fout, "risicopost · wachter").kind());
        assertTrue(jsonNode(fout, "risicopost · post").eindOutput());
    }

    @Test void a_navigated_check_on_a_characteristic_of_two_declarants_reads_that_of_the_reached_type() {
        // Post and Kustvak each declare and write risicopost. The Wachter's check "zijn post is risicopost" reaches a
        // Post, so it reads the Post's kenmerk and depends on the Post's writer alone, as IR-SPEC ch. 5 draws it; the
        // Kustvak's kenmerk is written and read by nobody. Read as the kenmerk of every declarant, the check drew a
        // dependency on the Kustvak's writer that the verifier does not have.
        String bron = POSTEN.replace("""
                Feittype postbezetting""", """
                Objecttype het Kustvak (mv: Kustvakken)
                    is risicopost kenmerk (bijvoeglijk);
                    de golfhoogte Numeriek (geheel getal);
                Feittype postbezetting""") + """
                Regel risicovak bij golfhoogte
                    geldig altijd
                        Een Kustvak is risicopost indien de golfhoogte van het Kustvak groter is dan 3.
                """;
        TwoRoutes.lees(bron).graphs().forEach((route, g) -> {
            assertTrue(edges(g).contains("risicopost · post -> seingerechtigd bij risicopost"), () -> route + ": " + edges(g));
            assertFalse(edges(g).contains("risicopost · kustvak -> seingerechtigd bij risicopost"), () -> route + ": " + edges(g));
            assertTrue(jsonNode(g, "risicopost · kustvak").eindOutput(), route + ": het kenmerk van het kustvak leest niemand");
            assertEquals(Set.of("risicopost bij hoogte"), g.dependencies().stream()
                    .filter(a -> a.consumer().equals("seingerechtigd bij risicopost"))
                    .map(RegelGraaf.Dependency::producer).collect(Collectors.toSet()), route);
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

    @Test void the_characteristic_check_in_a_fold_filter_reads_the_characteristic_of_the_element() {
        // the filter is evaluated per contributor, with the Vondst as its subject, not the Jutter the rule ranges over
        TwoRoutes.lees(VONDSTEN).graphs().forEach((route, g) -> {
            assertTrue(edges(g).contains("waardevol · vondst -> pronkstukaantal van een jutter"), () -> route + ": " + edges(g));
            assertTrue(g.nodes().stream().noneMatch(n -> n.label().equals("waardevol · jutter")),
                    () -> route + ": " + g.nodes());
            assertFalse(jsonNode(g, "waardevol · vondst").eindOutput(), route);
        });
    }

    @Test void the_source_of_a_value_fold_reads_the_attribute_of_each_element() {
        TwoRoutes.lees(VONDSTEN).graphs().forEach((route, g) -> assertTrue(
                edges(g).contains("gewicht · vondst -> vondstgewicht van een jutter"), () -> route + ": " + edges(g)));
    }

    @Test void the_source_of_a_count_names_the_role_and_reads_no_member() {
        // "het aantal vondsten van de Jutter": the source names the relation's role, which no type declares as a
        // member, so it is no read of an attribuut — before, the graph drew it as an input nobody could ever write
        TwoRoutes.lees(VONDSTEN).graphs().forEach((route, g) -> {
            assertTrue(g.nodes().stream().noneMatch(n -> n.label().startsWith("vondsten")), () -> route + ": " + g.nodes());
            List<String> toCount = edges(g).stream().filter(k -> k.endsWith("-> vondstaantal van een jutter")).toList();
            assertEquals(List.of(), toCount, route + ": een telling zonder filter leest niets");
        });
    }
}
