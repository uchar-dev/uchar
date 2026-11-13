package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A percentage carries no sign (IR-SPEC ch. 2.4: the one sign a percentage has is unspecified), so a sign written in
 * its declaration has no place on the wire, and no executor checks it. The frontend says so at the declaration
 * (RGS-460). Before: the sign fell away without a word, and "-5 %" in a "niet-negatief" percentage passed.
 */
class PercentageSignTest {

    private static List<RegelSpraakModel.Diagnostic> dropped(String declarations) {
        RegelSpraakModel m = Resolver.resolve("Objecttype de Jutter (mv: Jutters) (bezield)\n" + declarations);
        return m.diagnostics.stream().filter(d -> d.code().equals("RGS-460")).toList();
    }

    @Test void a_signed_percentage_is_named_at_its_declaration() {
        List<RegelSpraakModel.Diagnostic> ds = dropped("""
                de korting Percentage (niet-negatief geheel getal);
                de rente Percentage (positief getal met 2 decimalen);
            """);
        assertEquals(2, ds.size(), () -> "één melding per gedeclareerd teken: " + ds);
        assertEquals(RegelSpraakModel.Severity.WARNING, ds.get(0).severity());
        assertEquals(2, ds.get(0).line(), ds.get(0)::toString);
        assertTrue(ds.get(0).message().contains("'niet-negatief' in 'Percentage (niet-negatief geheel getal)'"),
                ds.get(0)::message);
        assertTrue(ds.get(1).message().contains("'positief'"), ds.get(1)::message);
    }

    @Test void a_percentage_without_a_sign_says_nothing() {
        assertEquals(List.of(), dropped("""
                de korting Percentage (geheel getal);
            """));
    }
}
