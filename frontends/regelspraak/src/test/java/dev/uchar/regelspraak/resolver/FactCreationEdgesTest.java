package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The dependency graph of IR-SPEC ch. 5 that {@link Dependencies} reads, at two forms (RUNTIME-SEMANTICS ch. 6.1,
 * since 2026-10-06). A fact creation writes the relation it creates and no population: it mints no instance, it links
 * instances that exist. A role check reads the link set of the relation it names, positively, and negated negatively.
 *
 * <p><b>Red before the fix</b> (the frontend of {@code d07b9f93}): the fact creation wrote the population of the filler
 * of its target role as a proxy, so an object creation that counts the members of a role over that type and the fact
 * creation that links those members to the created object made a WF-045 cycle that does not exist (the casus of the
 * parity zone has it); and a role check read nothing, so no edge ran from a fact creation to the rule that checks the
 * role it fills. The lists below are the verifier's {@code --dependency-edges} for the same document.
 */
class FactCreationEdgesTest {

    /** The model of {@code corpus/golden/jutterswet-premiepot}. */
    static final String PREMIEPOT = """
        Parameter de premie per havenjutter : Numeriek (geheel getal)

        Objecttype de Haven (mv: Havens)
            het premietotaal Numeriek (geheel getal);
            het premietal Numeriek (geheel getal);

        Objecttype de Jutter (mv: Jutters) (bezield)
            is premiegerechtigd kenmerk (bijvoeglijk);
            is premieloos kenmerk (bijvoeglijk);

        Objecttype de Premiepot (mv: Premiepotten)
            de pot Numeriek (geheel getal);
            het jutteraantal Numeriek (geheel getal);

        Feittype HavenHeeftJutter
            de haven Haven
            de havenjutter (mv: havenjutters) Jutter
        Eén haven heeft meerdere havenjutters

        Feittype HavenHeeftPremiepot
            de premiehaven Haven
            de premiepot Premiepot
        Eén premiehaven heeft één premiepot

        Feittype PremiepotVoorJutters
            de verdeelde premiepot Premiepot
            de premiejutter (mv: premiejutters) Jutter
        Eén verdeelde premiepot heeft meerdere premiejutters

        Regel premiepot
            geldig altijd
                Een Haven heeft de premiepot met pot gelijk aan het aantal havenjutters van de Haven maal de premie per havenjutter.

        Regel premiejutters
            geldig altijd
                Een premiejutter van een verdeelde premiepot is een havenjutter van de premiehaven van de verdeelde premiepot.

        Regel jutteraantal
            geldig altijd
                Het jutteraantal van een Premiepot moet berekend worden als het aantal premiejutters van de Premiepot.
        """;

    private static String ir(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return IrEmitter.emit(m);
    }

    @Test void a_fact_creation_writes_no_population_and_closes_no_cycle() {
        String ir = ir(PREMIEPOT);
        assertEquals(List.of(), Dependencies.cycles(ir), "geen kring: de feitcreatie schrijft Jutter niet");
        assertEquals(List.of(
                "positive\t/rules/factCreations/0/versions/0\tpremiejutters\trel:premiepotvoorjutters\t"
                        + "/rules/rules/0/versions/0\tjutteraantal",
                "strict\t/rules/objectCreations/0/versions/0\tpremiepot\trel:havenheeftpremiepot\t"
                        + "/rules/factCreations/0/versions/0\tpremiejutters"),
                Dependencies.edges(ir));
    }

    @Test void a_role_check_reads_the_relation_that_it_names() {
        String ir = ir(PREMIEPOT + """

            Regel premiegerechtigd
                geldig altijd
                    Een Jutter is premiegerechtigd
                    indien hij een premiejutter is.

            Regel premieloos
                geldig altijd
                    Een Jutter is premieloos
                    indien hij geen premiejutter is.
            """);
        assertEquals(List.of(), Dependencies.cycles(ir));
        List<String> edges = Dependencies.edges(ir);
        // a characteristic attribution writes irrevocably, so its positive read of a relation is stratum-strict;
        // negated, the check holds on absence and a later link makes it false
        assertTrue(edges.contains("strict\t/rules/factCreations/0/versions/0\tpremiejutters\trel:premiepotvoorjutters\t"
                + "/rules/rules/1/versions/0\tpremiegerechtigd"), () -> String.join("\n", edges));
        assertTrue(edges.contains("negative\t/rules/factCreations/0/versions/0\tpremiejutters\trel:premiepotvoorjutters\t"
                + "/rules/rules/2/versions/0\tpremieloos"), () -> String.join("\n", edges));
    }
}
