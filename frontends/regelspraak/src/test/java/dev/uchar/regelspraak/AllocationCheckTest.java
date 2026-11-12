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
            de omschrijving Tekst;

        Objecttype de Berger (mv: Bergers) (bezield)
            het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;
            de notitie (mv: notities) Tekst;
            de naam Tekst;

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
        assertEquals(22, errors.get(0).line(), errors.get(0).message());
        assertTrue(errors.get(0).message().contains("'Het kortingspercentage van de opbrengst van een berging'"),
                errors.get(0).message());
    }

    /** "zijn" in the condition of an allocation reads the source instance the allocation runs on: before, the rule
     *  had no subject there, the read stayed a marker with a warning, and ref faulted on it (EF-019). */
    @Test void a_condition_reads_the_source_as_its_subject() {
        RegelSpraakModel m = Resolver.resolve(allocation("De opbrengst van een berging wordt verdeeld over de "
                + "bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen\n"
                + "        indien zijn opbrengst groter is dan 10 €."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        var c = (dev.uchar.regelspraak.ir.Condition.Comparison) m.allocations.get(0).condition();
        var read = (dev.uchar.regelspraak.ir.Expr.AttributeReference) c.left();
        assertEquals("opbrengst", read.attribute());
        assertEquals("Berging", read.objectType().name);
    }

    /** An allocation computes with amounts (WF-025): a Text pot distributed over a Text share, and a Text weight,
     *  were written without a word, the first with no type check at all. One ERROR per member. */
    @Test void a_member_that_is_no_number_is_refused() {
        RegelSpraakModel m = Resolver.resolve(allocation("De omschrijving van een berging wordt verdeeld over de "
                + "notities van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen."));
        assertEquals(List.of("RGS-423 22 omschrijving", "RGS-423 22 notitie"), m.errors().stream()
                .map(d -> d.code() + " " + d.line() + " " + d.params().get("attribute")).toList(), m.diagnostics::toString);
        RegelSpraakModel w = Resolver.resolve(allocation("De opbrengst van een berging wordt verdeeld over de "
                + "bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld naar rato van de naam."));
        assertEquals(List.of("RGS-423 naam"), w.errors().stream().map(d -> d.code() + " " + d.params().get("attribute"))
                .toList(), w.diagnostics::toString);
        assertTrue(w.errors().get(0).message().contains("het gewicht 'naam' is van het type Tekst"),
                w.errors().get(0).message());
    }

    @Test void an_allocation_of_attributes_stays_clean() {
        RegelSpraakModel m = Resolver.resolve(allocation("De opbrengst van een berging wordt verdeeld over de "
                + "bergingsaandelen van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen."));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(1, m.allocations.size());
    }
}
