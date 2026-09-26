package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.emit.IrEmitter;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two builtins whose result the resolver leaves without a claim still carry a static type on the wire: a
 * derived position that writes null is refused by WF-034 ("value node without a static type"), and both types
 * are fixed by the spec whatever the operands are. The day count counts days, so it is typed as every count is
 * (RUNTIME-SEMANTICS ch. 2.3 and ch. 3.8: whole, non-negative, unitless — the type every dayCount in the corpus
 * carries); the concatenation joins its parts into one text (IR-SPEC ch. 4.3, the text fold). Measured on the
 * frontend before this: both went out as {@code "type": null}.
 */
class BuiltinTypeTest {

    private static String emitCompact(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    @Test void de_dagentelling_draagt_het_type_van_een_telling() {
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

    @Test void de_concatenatie_draagt_het_type_tekst() {
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
