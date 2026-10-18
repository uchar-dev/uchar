package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §9.10 recursie: a read-write cycle between berekeningen is refused with an ERROR, as WF-045
 * refuses a dependency cycle that passes through no recursive creation; the WARNING that names
 * the cycle in the members the author wrote stays beside it. A same-instance self-derivation (the
 * begrenzing/clamp idiom) is such a cycle too, of one node. Until 2026-10-06 the posture was a
 * WARNING alone, because v2.3.0 calls cyclic-vs-recursive not reliably automatable; uchar's
 * Regelgroep extension ("is recursief") marks the recursion WF-045 admits, so a cycle outside it is
 * an error of the document (grammar/DIVERGENCES.md, "Kringen: WF-045 en WF-051 in check").
 */
class RecursieTest {

    static final String MODEL = """
        Objecttype de Ding
            de x Numeriek (geheel getal);
            de y Numeriek (geheel getal);
        """;

    @Test void cycle_reference_is_an_error_with_the_warning_beside_it() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel a
                geldig altijd
                    De x van een Ding moet berekend worden als de y van de Ding plus 1.
            Regel b
                geldig altijd
                    De y van een Ding moet berekend worden als de x van de Ding plus 1.
            """);
        assertEquals(1, m.errors().size(), () -> "de kring weigert met één ERROR: " + m.diagnostics);
        assertTrue(m.errors().get(0).message().endsWith("(WF-045): dependency cycle through no recursive creation: "
                + "a -> b -> a"), () -> "de weigering noemt WF-045 en de kring: " + m.errors());
        assertEquals(4, m.errors().get(0).line(), "de kop van de regel waar de kring begint");
        assertTrue(m.diagnostics.stream().anyMatch(d -> !d.isError() && d.message().contains("kringverwijzing")),
                () -> "de kringverwijzing-WARNING ontbreekt: " + m.diagnostics);
    }

    @Test void same_instance_self_derivation_clamp_idiom_is_a_cycle_that_is_refused() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel begrens x
                geldig altijd
                    De x van een Ding moet gesteld worden op de x van de Ding, met een maximum van de y van de Ding.
            """);
        // the verifier refuses this self-read under WF-045 like every dependency cycle, and so does the frontend
        assertEquals(1, m.errors().size(), () -> "de kring van één knoop weigert met één ERROR: " + m.diagnostics);
        assertTrue(m.errors().get(0).message().endsWith("(WF-045): dependency cycle through no recursive creation: "
                + "begrens x -> begrens x"), () -> "de weigering noemt WF-045 en de kring: " + m.errors());
        assertTrue(m.diagnostics.stream().anyMatch(d -> !d.isError() && d.message().contains("kringverwijzing")),
                () -> "een zelf-lezing is een kring van één knoop en krijgt de kringmelding: " + m.diagnostics);
    }
}
