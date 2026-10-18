package dev.uchar.regelspraak.graaf;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.graaf.ProducentConsument.Navigatie;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static dev.uchar.regelspraak.graaf.TwoRoutes.edges;
import static dev.uchar.regelspraak.graaf.TwoRoutes.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The element key over an inheritance chain (IR-SPEC ch. 2.2): a member is keyed on the type that DECLARES it
 * ({@link ProducentConsument#elementVan}), so a producer and a consumer that range over different types of one
 * chain meet on one element, in both directions.
 *
 * <p>Keyed on the rule's own subject type instead, a producer on the supertype and a consumer on the subtype were
 * two elements: the graph drew the member twice, once as an INVOER nobody writes and once as an eind-output nobody
 * reads, and a scope-completeness check over the same analysis concluded "no producer at all" and let a scope
 * through whose kenmerk then evaluated false without a word.
 */
class GraphChainTest {

    private static final String MODEL_FLAT = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);
            de premie Numeriek (geheel getal);
            is ervaren kenmerk (bijvoeglijk);
        """;
    /** Klipjutter ⊑ Jutter; the subtype declares only its own member, everything else is inherited. */
    private static final String MODEL_CHAIN = MODEL_FLAT + """
        Objecttype de Klipjutter is een Jutter (mv: Klipjutters) (bezield)
            het klipnummer Numeriek (geheel getal);
        """;

    private static final String PRODUCER_SUPER = """
        Regel ervaren bij leeftijd
            geldig altijd
                Een Jutter is ervaren indien de leeftijd van de Jutter groter is dan 17.
        """;
    private static final String PRODUCER_SUB = """
        Regel ervaren bij leeftijd
            geldig altijd
                Een Klipjutter is ervaren indien de leeftijd van de Klipjutter groter is dan 17.
        """;
    private static final String CONSUMER_SUPER = """
        Regel premie van een ervaren jutter
            geldig altijd
                De premie van een Jutter moet gesteld worden op 5 indien de Jutter ervaren is.
        """;
    private static final String CONSUMER_SUB = """
        Regel premie van een ervaren jutter
            geldig altijd
                De premie van een Klipjutter moet gesteld worden op 5 indien de Klipjutter ervaren is.
        """;

    private static final ElementSleutel ERVAREN = ElementSleutel.van("Jutter", "ervaren", true);

    @Test void producer_and_consumer_key_on_the_declaring_type() {
        TwoRoutes.Twin t = TwoRoutes.lees(MODEL_CHAIN + PRODUCER_SUPER + CONSUMER_SUB);
        assertKey(t.sourceModel(), t.sourceModel().prioritizedDefaults, "bron",
                ProducentConsument.bronNavigatie(t.sourceModel()));
        assertKey(t.wire().model(), t.wire().prioritizedDefaults(), "wire", ProducentConsument.wireNavigatie(t.wire()));
    }

    private static void assertKey(RegelSpraakModel m, List<PrioritizedDefaults> defaults, String route,
                                      Navigatie nav) {
        assertTrue(ProducentConsument.producentIndex(m, defaults).containsKey(ERVAREN),
                () -> route + ": producentsleutel " + ProducentConsument.producentIndex(m, defaults).keySet());
        ResolvedRegel consumer = m.resolvedRegels.stream()
                .filter(r -> ProducentConsument.basisNaam(r.naam()).equals("premie van een ervaren jutter"))
                .findFirst().orElseThrow();
        assertTrue(ProducentConsument.verbruikt(consumer, nav).contains(ERVAREN),
                route + ": de consument op het subtype draagt dezelfde sleutel");
    }

    @Test void the_key_is_kind_aware() {
        // A kenmerk and an attribuut may carry one name on DIFFERENT types of one chain, so the declarant search is
        // per kind; otherwise the kenmerk read on the subtype would find the attribuut there and key on the wrong
        // declarant.
        TwoRoutes.Twin t = TwoRoutes.lees("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                is gekeurd kenmerk (bijvoeglijk);
            Objecttype de Klipjutter is een Jutter (mv: Klipjutters) (bezield)
                de gekeurd Numeriek (geheel getal);
            """);
        for (RegelSpraakModel m : List.of(t.sourceModel(), t.wire().model())) {
            RegelSpraakModel.Objecttype klipjutter = m.objecttypeOpNaam("Klipjutter");
            assertEquals(ElementSleutel.van("Jutter", "gekeurd", true),
                    ProducentConsument.elementVan(klipjutter, "gekeurd", true), "het kenmerk is op Jutter gedeclareerd");
            assertEquals(ElementSleutel.van("Klipjutter", "gekeurd", false),
                    ProducentConsument.elementVan(klipjutter, "gekeurd", false), "het gelijknamige attribuut op Klipjutter");
        }
    }

    @Test void the_graph_shares_the_key_and_draws_the_member_one_time() {
        // producer on the supertype, consumer on the subtype: the full-coverage direction
        TwoRoutes.lees(MODEL_CHAIN + PRODUCER_SUPER + CONSUMER_SUB).graphs().forEach((route, g) -> assertMember(route, g));
    }

    @Test void a_producer_on_the_subtype_and_a_consumer_on_the_supertype_share_the_key_too() {
        // The other direction is PARTIAL coverage (the producer serves Klipjutter instances only) and keys the
        // same: the question a reader of this graph asks is whether a producer EXISTS, and a partial one is one.
        TwoRoutes.lees(MODEL_CHAIN + PRODUCER_SUB + CONSUMER_SUPER).graphs().forEach((route, g) -> assertMember(route, g));
    }

    private static void assertMember(String route, RegelGraaf g) {
        assertEquals(1, g.nodes().stream().filter(n -> n.label().startsWith("ervaren ·")).count(),
                () -> route + ": één elementknoop voor één lid: " + g.nodes());
        RegelGraaf.Node ervaren = node(g, "ervaren · jutter");
        assertEquals(RegelGraaf.NodeKind.KENMERK, ervaren.kind(), route + ": geen INVOER, er IS een producent");
        assertFalse(ervaren.eindOutput(), route + ": geen eind-output, er IS een consument");
        assertTrue(edges(g).containsAll(Set.of("ervaren bij leeftijd -> ervaren · jutter",
                "ervaren · jutter -> premie van een ervaren jutter")), () -> route + ": " + edges(g));
    }

    @Test void the_graph_on_a_flat_model_does_not_change() {
        // on a model without subtypes the chain reading does nothing: every chain is one type long
        TwoRoutes.lees(MODEL_FLAT + PRODUCER_SUPER + CONSUMER_SUPER).graphs().forEach((route, g) -> {
            assertEquals(5, g.nodes().size(), () -> route + ": " + g.nodes());
            assertEquals(4, g.edges().size(), () -> route + ": " + g.edges());
            assertEquals(RegelGraaf.NodeKind.INVOER, node(g, "leeftijd · jutter").kind(), route);
        });
    }
}
