package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The word 'onbekend' has no meaning of its own in the dialect: a status that a value does not come is written as a
 * characteristic (corpus/golden/jutterswet-afhaalbaar). A declaration that marks an attribute "(of onbekend)" is a
 * syntax error; "zijn taxatie onbekend is" is the characteristic check its form spells, which binds nothing on a Vondst
 * without that characteristic; and a characteristic named onbekend is checked as every other one.
 */
class UnknownWordTest {

    @Test void a_marking_on_a_declaration_is_a_syntax_error() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen) (of onbekend);
            """);
        assertEquals(List.of("RGS-091"), m.errors().stream().map(Diagnostic::code).toList(),
                () -> "one syntax error: " + m.errors());
        assertTrue(m.errors().get(0).message().startsWith("syntaxfout: extraneous input '('"), () -> "" + m.errors());
        assertEquals(2, m.errors().get(0).line(), () -> "on the line of the declaration: " + m.errors());
    }

    @Test void a_test_of_the_word_is_a_characteristic_check_that_binds_nothing() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen);
                het vindloon Numeriek (getal met 2 decimalen);

            Regel forfaitair vindloon
                geldig altijd
                    Het vindloon van een Vondst moet gesteld worden op 25,00
                    indien zijn taxatie onbekend is.
            """);
        assertTrue(m.errors().isEmpty(), () -> "no ERROR: " + m.errors());
        assertEquals(1, m.diagnostics.size(), () -> "one finding: " + m.diagnostics);
        Diagnostic d = m.diagnostics.get(0);
        assertEquals("RGS-092", d.code(), d::toString);
        assertEquals(Severity.WARNING, d.severity(), d::toString);
        assertEquals("zijn taxatie onbekend is", String.valueOf(d.args().get("expression")), d::toString);
        assertTrue(d.message().contains("bindt geen kenmerk en geen rol van het onderwerp"), d::message);
        String ir = IrEmitter.emit(m);
        assertTrue(ir.contains("\"node\": \"Unresolved\""), ir);
        assertFalse(ir.contains("SumType") || ir.contains("Construct"), ir);
    }

    @Test void a_characteristic_named_onbekend_is_checked_as_every_other() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (getal met 2 decimalen);
                is onbekend kenmerk (bijvoeglijk);

            Regel waarde
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 5,00
                    indien hij onbekend is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        assertTrue(ir.contains("\"node\": \"CharacteristicCheck\""), ir);
        assertFalse(ir.contains("SumType"), ir);
    }
}
