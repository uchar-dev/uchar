package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WF-111 (maintainer decision 2026-09-23, widened by IMPLICIT-TIME VB 6.1), producer side: a time window's bound is
 * a day-precise date expression over the subject, the parameters and literals. §13.4.15.68 lets a periodevergelijking
 * take any date expression; a bound that navigates, reads a timeline or is a datum-tijd is refused, here as in the
 * verifier (WF-111, and WF-053 for the supertype-named read).
 *
 * <p><b>Own means: written on the subject.</b> This frontend writes an own read — an inherited attribute
 * included — with the SUBJECT as its object type ("zijn peildatum" in a Klipjutter rule is an
 * {@code AttributeReference} on Klipjutter). A read naming any other type, a supertype of the subject included, goes
 * out as a navigation (by-pair bindings when a fact type relates the two types), which the verifier refuses as
 * navigated (WF-111). Where no fact type relates them the emitter refuses the model, so no document carries the
 * read (measured 2026-09-25), and the refusal keeps its Dutch words: the sub- and supertype rows below, whose model
 * relates Jutter and Klipjutter by no fact type. So the fence compares object types by identity, as the Drools gate
 * {@code TimeCalculation.periodBoundLowerable} does, and for this frontend's output that gives exactly the verifier's
 * verdict.
 */
class PeriodBoundaryTest {

    static final String MODEL = """
        Parameter de peilparameter : Datum in dagen

        Objecttype de Jutter (mv: Jutters) (bezield)
            de peildatum Datum in dagen;
            de knipdatum Datum in dagen voor elke maand;
            het meldmoment Datum en tijd in millisecondes;
            de premie Numeriek (geheel getal);

        Objecttype de Klipjutter is een Jutter (mv: Klipjutters) (bezield)
            de klipdatum Datum in dagen;

        Objecttype het Strandvak (mv: Strandvakken)
            de opendatum Datum in dagen;

        Feittype vaktoewijzing
            de vakhouder Jutter
            het strandvak Strandvak
        Eén vakhouder heeft één strandvak
        """;

    private static String rule(String subject, String window) {
        return """

            Regel premie
                geldig altijd
                    De premie van een %s moet gesteld worden op 10
                    %s.
            """.formatted(subject, window);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Jutter     | vanaf de opendatum van het strandvak van de Jutter | een genavigeerde lezing van 'opendatum'",
            "Jutter     | vanaf de opendatum van zijn strandvak    | een genavigeerde lezing van 'opendatum'",
            "Jutter     | vanaf de opendatum van zijn strandvak plus 1 dg | een genavigeerde lezing van 'opendatum'",
            "Jutter     | vanaf zijn meldmoment                    | is een datum met millisecondeprecisie",
            "Jutter     | vanaf zijn knipdatum                     | het tijdsafhankelijke attribuut 'knipdatum'",
            "Jutter     | vanaf zijn knipdatum plus 1 dg           | het tijdsafhankelijke attribuut 'knipdatum'",
            "Jutter     | vanaf dd. 1-7-2026 08:30:00.000          | is een datum met millisecondeprecisie",
            "Jutter     | vanaf de klipdatum van de Klipjutter     | een subtype van het onderwerp 'Jutter'",
            "Klipjutter | vanaf de peildatum van de Jutter         | een supertype van het onderwerp 'Klipjutter'"
    })
    void a_bound_that_reads_more_than_the_subject_the_parameters_and_literals_is_refused(String subject, String window, String fragment) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rule(subject, window));
        List<Diagnostic> faults = m.errors();
        assertEquals(1, faults.size(), () -> "verwachtte precies één fout voor '" + window + "', kreeg: " + m.diagnostics);
        String t = faults.get(0).message();
        // the verifier's WF-111 sentence; a sub- or supertype read that no relation reaches has no wire form, so that
        // refusal keeps its own words, which cite the rule
        assertTrue(t.contains("-grens van een tijdvenster is ") || t.contains("WF-111") && t.contains("§8.4.3"), t);
        assertTrue(t.contains(fragment), () -> "de melding noemt '" + fragment + "' niet: " + t);
        // an ERROR keeps the document off disk: Resolver.irMode writes nothing
        assertFalse(m.isOk());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Jutter     | vanaf dd. 01-01-2024",
            "Jutter     | vanaf zijn peildatum",
            "Jutter     | tot en met zijn peildatum",
            "Jutter     | van dd. 01-01-2024 tot zijn peildatum",
            "Klipjutter | vanaf zijn peildatum",
            "Klipjutter | vanaf de peildatum van de Klipjutter",
            "Klipjutter | vanaf zijn klipdatum",
            "Jutter     | vanaf de peilparameter",
            "Jutter     | van dd. 01-01-2024 tot de peilparameter",
            "Jutter     | vanaf zijn peildatum plus 1 dg",
            "Jutter     | van zijn peildatum plus 1 dg tot en met de peilparameter"
    })
    void a_day_precise_date_expression_over_the_subject_the_parameters_and_literals_stays_clean(String subject, String window) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rule(subject, window));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek voor '" + window + "': " + m.diagnostics);
    }
}
