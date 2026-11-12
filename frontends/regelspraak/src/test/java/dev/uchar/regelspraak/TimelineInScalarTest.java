package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RUNTIME-SEMANTICS ch. 3.3 "Scalar contexts": a time-dependent attribute or parameter as the value of a rule on a
 * target without a timeline is refused; the temporal total collapses a timeline to a scalar. Measured before: both
 * forms below passed the frontend and the verifier, ref faulted ("parameter tarief is granular"), clingo refused the
 * translation and Drools excluded the golden.
 */
class TimelineInScalarTest {

    private static final String MODEL = """
        Eenheidsysteem Valuta
            de euro (mv: euros) EUR €

        Parameter het tarief : Numeriek (getal met 2 decimalen) met eenheid € voor elk jaar

        Objecttype de Jutter (mv: Jutters)
            het loon Numeriek (getal met 2 decimalen) met eenheid €;
            het jaarloon Numeriek (getal met 2 decimalen) met eenheid € voor elk jaar;
            het maandloon Numeriek (getal met 2 decimalen) met eenheid € voor elk jaar;

        Regel loon
            geldig altijd
                %s
        """;

    private static List<String> errors(String rule) {
        RegelSpraakModel m = Resolver.resolve(MODEL.formatted(rule));
        return m.errors().stream().map(d -> d.code() + " " + d.line() + " " + d.params().get("operand")).toList();
    }

    @Test void a_time_dependent_parameter_in_a_scalar_target_is_refused() {
        assertEquals(List.of("RGS-434 13 tarief"),
                errors("Het loon van een Jutter moet berekend worden als het tarief."));
    }

    @Test void a_time_dependent_attribute_in_a_scalar_target_is_refused() {
        assertEquals(List.of("RGS-434 13 jaarloon"),
                errors("Het loon van een Jutter moet berekend worden als zijn jaarloon plus 1 €."));
    }

    @Test void the_temporal_total_is_a_scalar() {
        assertEquals(List.of(), errors("Het loon van een Jutter moet gesteld worden op het totaal van zijn jaarloon."));
    }

    @Test void a_timeline_target_reads_a_timeline() {
        assertEquals(List.of(), errors("Het maandloon van een Jutter moet berekend worden als zijn jaarloon."));
    }
}
