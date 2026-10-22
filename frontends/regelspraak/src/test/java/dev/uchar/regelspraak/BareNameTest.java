package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A bare name in an expression — written without an article, so the parse reads it as a {@code variabeleNaam} — that
 * names no "Daarbij geldt" variable and no parameter is a marker with a WARNING, as the same name with an article on
 * the onderwerp route is. Until 2026-10-22 (found by the card semantic-tokens) it went to disk as a
 * {@code VariableReference} without a diagnostic, exit 0, and the verifier refused the document (WF-034: "variable
 * reference 'basisprijs' resolves against no binding"). The written variable and the written parameter keep binding.
 */
class BareNameTest {

    private static String model(String declarations, String value) {
        return """
                Objecttype de Vondst (mv: Vondsten)
                    de waarde Numeriek (getal met 2 decimalen);
                %s
                Regel waarde van een vondst
                    geldig altijd
                        De waarde van een Vondst moet berekend worden als %s.
                """.formatted(declarations, value);
    }

    @Test void a_bare_name_that_names_nothing_is_a_marker_with_a_warning() {
        RegelSpraakModel m = Resolver.resolve(model("", "basisprijs maal 2"));
        assertTrue(m.isOk(), () -> "a marker is a WARNING, no ERROR: " + m.diagnostics);
        assertTrue(m.diagnostics.stream().anyMatch(d -> "RGS-092".equals(d.code()) && d.message().contains("'basisprijs'")),
                () -> "the WARNING names the bare name: " + m.diagnostics);
        String json = IrEmitter.emit(m);
        assertFalse(json.contains("\"VariableReference\""), "no variable reference without a binding reaches the wire");
        Report.assertAccountedFor(json);
    }

    @Test void a_bare_name_of_a_variable_or_a_parameter_binds() {
        RegelSpraakModel variable = Resolver.resolve(model("", """
                basisprijs maal 2
                        Daarbij geldt:
                            de basisprijs is 5"""));
        assertTrue(variable.diagnostics.isEmpty(), () -> "the variable binds: " + variable.diagnostics);
        assertTrue(IrEmitter.emit(variable).contains("\"BinderRef\""), "read as the variable");
        RegelSpraakModel parameter = Resolver.resolve(model(
                "\nParameter basisprijs : Numeriek (getal met 2 decimalen)\n", "basisprijs maal 2"));
        assertTrue(parameter.diagnostics.isEmpty(), () -> "the parameter binds: " + parameter.diagnostics);
        assertTrue(IrEmitter.emit(parameter).contains("\"ParameterReference\""), "read as the parameter");
    }
}
