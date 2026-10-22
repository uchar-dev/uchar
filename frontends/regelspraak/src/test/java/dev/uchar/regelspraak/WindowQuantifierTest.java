package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WF-112 (maintainer decision 2026-09-23), producer side: a time window never sits under 'geen van de' or a
 * counting quantifier — a composite member at any depth, or the filter or test of such a quantifier fold. The
 * window gates the RULE, and no reading is specified for a verdict that is complemented or counted. Measured on
 * the frontend before this fence (78edd45d): the window as a bullet under 'geen van de', 'ten minste één van de',
 * 'ten hoogste één van de' and 'precies één van de', and under a 'geen van de' sub-compound of an 'alle'
 * compound, each reached disk with {@code diagnostics: []}, and the verifier refused every one under WF-112.
 * Under 'alle' the window is admitted and stays clean.
 */
class WindowQuantifierTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de peildatum Datum in dagen;
            de premie Numeriek (geheel getal);
            is gemeld kenmerk (bijvoeglijk);

        """;

    private static String under(String quantifier) {
        return MODEL + """
            Regel premie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 10
                    indien hij aan %s volgende voorwaarden voldoet:
                    - het is de periode vanaf dd. 01-01-2024
                    - hij is gemeld.
            """.formatted(quantifier);
    }

    private static Diagnostic onlyError(RegelSpraakModel m) {
        List<Diagnostic> faults = m.errors();
        assertEquals(1, faults.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        return faults.get(0);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "geen van de            | geen van de",
            "ten minste één van de  | ten minste 1",
            "ten hoogste één van de | ten hoogste 1",
            "precies één van de     | precies 1"
    })
    void a_window_under_no_or_a_counting_quantifier_is_refused(String quantifier, String words) {
        RegelSpraakModel m = Resolver.resolve(under(quantifier));
        Diagnostic d = onlyError(m);
        assertTrue(d.message().contains("a time window under none or a counting quantifier")
                && d.message().contains("'" + words + "'"), d.message());
        assertFalse(m.isOk(), "een ERROR houdt het document van de schijf");
    }

    @Test void a_window_under_no_within_all_refuses() {
        String src = MODEL + """
            Regel premie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 10
                    indien hij aan alle volgende voorwaarden voldoet:
                    - hij is gemeld
                    - er wordt voldaan aan geen van de volgende voorwaarden:
                        .. het is de periode vanaf dd. 01-01-2024
                        .. zijn peildatum is gevuld.
            """;
        Diagnostic d = onlyError(Resolver.resolve(src));
        assertTrue(d.message().contains("a time window under none or a counting quantifier")
                && d.message().contains("'geen van de'"), d.message());
    }

    @Test void a_window_under_all_stays_clean() {
        RegelSpraakModel m = Resolver.resolve(under("alle"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    @Test void a_window_as_own_condition_part_stays_clean() {
        String src = MODEL + """
            Regel premie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 10
                    vanaf dd. 01-01-2024.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }
}
