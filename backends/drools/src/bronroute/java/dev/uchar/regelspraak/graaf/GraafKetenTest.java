package dev.uchar.regelspraak.graaf;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.ResolvedRegel;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static dev.uchar.regelspraak.graaf.TweeRoutes.kanten;
import static dev.uchar.regelspraak.graaf.TweeRoutes.knoop;
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
class GraafKetenTest {

    private static final String MODEL_VLAK = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);
            de premie Numeriek (geheel getal);
            is ervaren kenmerk (bijvoeglijk);
        """;
    /** Klipjutter ⊑ Jutter; the subtype declares only its own member, everything else is inherited. */
    private static final String MODEL_KETEN = MODEL_VLAK + """
        Objecttype de Klipjutter is een Jutter (mv: Klipjutters) (bezield)
            het klipnummer Numeriek (geheel getal);
        """;

    private static final String PRODUCENT_SUPER = """
        Regel ervaren bij leeftijd
            geldig altijd
                Een Jutter is ervaren indien de leeftijd van de Jutter groter is dan 17.
        """;
    private static final String PRODUCENT_SUB = """
        Regel ervaren bij leeftijd
            geldig altijd
                Een Klipjutter is ervaren indien de leeftijd van de Klipjutter groter is dan 17.
        """;
    private static final String CONSUMENT_SUPER = """
        Regel premie van een ervaren jutter
            geldig altijd
                De premie van een Jutter moet gesteld worden op 5 indien de Jutter ervaren is.
        """;
    private static final String CONSUMENT_SUB = """
        Regel premie van een ervaren jutter
            geldig altijd
                De premie van een Klipjutter moet gesteld worden op 5 indien de Klipjutter ervaren is.
        """;

    private static final ElementSleutel ERVAREN = ElementSleutel.van("Jutter", "ervaren", true);

    @Test void producent_en_consument_sleutelen_op_het_declarerende_type() {
        TweeRoutes.Tweeling t = TweeRoutes.lees(MODEL_KETEN + PRODUCENT_SUPER + CONSUMENT_SUB);
        assertSleutel(t.bronModel(), "bron", k -> k.navigatie() != null);
        assertSleutel(t.wire().model(), "wire", k -> t.wire().navigatieBindingen().containsKey(k));
    }

    private static void assertSleutel(RegelSpraakModel m, String route, Predicate<Conditie.Kenmerk> nav) {
        assertTrue(ProducentConsument.producentIndex(m).containsKey(ERVAREN),
                () -> route + ": producentsleutel " + ProducentConsument.producentIndex(m).keySet());
        ResolvedRegel consument = m.resolvedRegels.stream()
                .filter(r -> ProducentConsument.basisNaam(r.naam()).equals("premie van een ervaren jutter"))
                .findFirst().orElseThrow();
        assertTrue(ProducentConsument.verbruikt(m, consument, nav).contains(ERVAREN),
                route + ": de consument op het subtype draagt dezelfde sleutel");
    }

    @Test void de_sleutel_is_soort_bewust() {
        // A kenmerk and an attribuut may carry one name on DIFFERENT types of one chain, so the declarant search is
        // per kind; otherwise the kenmerk read on the subtype would find the attribuut there and key on the wrong
        // declarant.
        TweeRoutes.Tweeling t = TweeRoutes.lees("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                is gekeurd kenmerk (bijvoeglijk);
            Objecttype de Klipjutter is een Jutter (mv: Klipjutters) (bezield)
                de gekeurd Numeriek (geheel getal);
            """);
        for (RegelSpraakModel m : List.of(t.bronModel(), t.wire().model())) {
            RegelSpraakModel.Objecttype klipjutter = m.objecttypeOpNaam("Klipjutter");
            assertEquals(ElementSleutel.van("Jutter", "gekeurd", true),
                    ProducentConsument.elementVan(klipjutter, "gekeurd", true), "het kenmerk is op Jutter gedeclareerd");
            assertEquals(ElementSleutel.van("Klipjutter", "gekeurd", false),
                    ProducentConsument.elementVan(klipjutter, "gekeurd", false), "het gelijknamige attribuut op Klipjutter");
        }
    }

    @Test void de_graaf_deelt_de_sleutel_en_tekent_het_lid_een_keer() {
        // producer on the supertype, consumer on the subtype: the full-coverage direction
        TweeRoutes.lees(MODEL_KETEN + PRODUCENT_SUPER + CONSUMENT_SUB).grafen().forEach((route, g) -> assertEenLid(route, g));
    }

    @Test void een_producent_op_het_subtype_en_een_consument_op_het_supertype_delen_de_sleutel_ook() {
        // The other direction is PARTIAL coverage (the producer serves Klipjutter instances only) and keys the
        // same: the question a reader of this graph asks is whether a producer EXISTS, and a partial one is one.
        TweeRoutes.lees(MODEL_KETEN + PRODUCENT_SUB + CONSUMENT_SUPER).grafen().forEach((route, g) -> assertEenLid(route, g));
    }

    private static void assertEenLid(String route, RegelGraaf g) {
        assertEquals(1, g.nodes().stream().filter(n -> n.label().startsWith("ervaren ·")).count(),
                () -> route + ": één elementknoop voor één lid: " + g.nodes());
        RegelGraaf.Node ervaren = knoop(g, "ervaren · jutter");
        assertEquals(RegelGraaf.NodeKind.KENMERK, ervaren.kind(), route + ": geen INVOER, er IS een producent");
        assertFalse(ervaren.eindOutput(), route + ": geen eind-output, er IS een consument");
        assertTrue(kanten(g).containsAll(Set.of("ervaren bij leeftijd -> ervaren · jutter",
                "ervaren · jutter -> premie van een ervaren jutter")), () -> route + ": " + kanten(g));
    }

    @Test void de_graaf_op_een_vlak_model_verandert_niet() {
        // on a model without subtypes the chain reading does nothing: every chain is one type long
        TweeRoutes.lees(MODEL_VLAK + PRODUCENT_SUPER + CONSUMENT_SUPER).grafen().forEach((route, g) -> {
            assertEquals(5, g.nodes().size(), () -> route + ": " + g.nodes());
            assertEquals(4, g.edges().size(), () -> route + ": " + g.edges());
            assertEquals(RegelGraaf.NodeKind.INVOER, knoop(g, "leeftijd · jutter").kind(), route);
        });
    }
}
