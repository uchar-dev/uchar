package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two builtins whose result the resolver leaves without a claim still carry a static type on the wire: a
 * derived position that writes null is refused by WF-034 ("value node without a static type"), and both types
 * are fixed by the spec whatever the operands are. The day count counts days, so it is typed as every count is
 * (RUNTIME-SEMANTICS ch. 2.3 and ch. 3.8: whole, non-negative, unitless — the type every dayCount in the corpus
 * carries); the concatenation joins its parts into one text (IR-SPEC ch. 4.3, the text fold). A day count assigned to
 * a target of another family than a number is refused before the verifier's WF-023.
 */
class BuiltinTypeTest {

    private static String emitCompact(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    @Test void the_day_count_carries_the_type_of_a_count() {
        String c = emitCompact("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                is borgplichtig kenmerk (bijvoeglijk) voor elke maand;
                de borgdagen Numeriek (niet-negatief geheel getal) voor elke maand;

            Regel borgdagen van een jutter
                geldig altijd
                    De borgdagen van een Jutter moet gesteld worden op het aantal dagen in de maand dat hij borgplichtig is.
            """);
        assertTrue(c.contains("\"granularity\":\"month\",\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,"
                + "\"sign\":\"nonNegative\"},\"source\":{\"snippet\":\"hetaantaldageninde"), () -> "de dagentelling:\n" + c);
        assertFalse(c.contains("\"type\":null"), () -> "een type null op de wire:\n" + c);
    }

    /** The day count is a number: assigned to a text or a date target it is refused as any number is (RGS-334), and
     *  the document does not reach the verifier's WF-023. */
    @Test void the_day_count_is_a_number_to_the_assignment_check() {
        for (List<String> declaredAndWritten : List.of(List.of("de borgnaam Tekst", "De borgnaam"),
                List.of("de borgdatum Datum in dagen", "De borgdatum"))) {
            RegelSpraakModel m = Resolver.resolve("""
                Objecttype de Jutter (mv: Jutters) (bezield)
                    is borgplichtig kenmerk (bijvoeglijk) voor elke maand;
                    %s voor elke maand;

                Regel r
                    geldig altijd
                        %s van een Jutter moet gesteld worden op het aantal dagen in de maand dat hij borgplichtig is.
                """.formatted(declaredAndWritten.get(0), declaredAndWritten.get(1)));
            assertEquals(1, m.diagnostics.size(), () -> "verwachtte precies één diagnose, kreeg: " + m.diagnostics);
            assertEquals("RGS-334", m.diagnostics.get(0).code(), m.diagnostics.get(0)::toString);
        }
    }

    @Test void the_concatenation_carries_the_text_type() {
        String c = emitCompact("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de voornaam Tekst;
                de achternaam Tekst;
                de volledige naam Tekst;

            Regel volledige naam van een jutter
                geldig altijd
                    De volledige naam van een Jutter moet gesteld worden op de concatenatie van zijn voornaam en zijn achternaam.
            """);
        assertTrue(c.contains("\"kind\":\"concatenation\",\"arguments\":["), () -> "de concatenatie:\n" + c);
        assertTrue(c.contains("\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"deconcatenatievan"),
                () -> "het type van de concatenatie:\n" + c);
        assertFalse(c.contains("\"type\":null"), () -> "een type null op de wire:\n" + c);
    }
}
