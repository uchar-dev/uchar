package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the rule checker refuses of an allocation (§13.4.10, IR-SPEC ch. 3.2) where the verifier would refuse the
 * document, or where the allocation would leave the document without a word.
 */
class AllocationCheckTest {

    static final String MODEL = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Objecttype de Berging (mv: Bergingen)
            de opbrengst Numeriek (geheel getal) met eenheid €;

        Objecttype de Berger (mv: Bergers) (bezield)
            het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;

        Parameter het kortingspercentage : Percentage (geheel getal)

        Feittype bergingsdeelname
            de berging Berging
            de berger (mv: bergers) Berger
        Eén berging heeft meerdere bergers
        """;

    private static String allocation(String sentence) {
        return MODEL + """

            Regel verdeel de opbrengst
                geldig altijd
                    %s
            """.formatted(sentence);
    }

    /** A source that reads as the percentage function over an attribute left the allocation out of the document,
     *  with no diagnostic and a document the verifier passes. */
    @Test void a_source_that_reads_as_a_computation_is_refused() {
        RegelSpraakModel m = Resolver.resolve(allocation("Het kortingspercentage van de opbrengst van een berging wordt "
                + "verdeeld over de bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen."));
        List<Diagnostic> errors = m.errors();
        assertEquals(1, errors.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        assertEquals("RGS-421", errors.get(0).code(), errors.get(0).message());
        assertEquals(19, errors.get(0).line(), errors.get(0).message());
        assertTrue(errors.get(0).message().contains("'Het kortingspercentage van de opbrengst van een berging'"),
                errors.get(0).message());
    }

    @Test void an_allocation_of_attributes_stays_clean() {
        RegelSpraakModel m = Resolver.resolve(allocation("De opbrengst van een berging wordt verdeeld over de "
                + "bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(1, m.allocations.size());
    }
}
