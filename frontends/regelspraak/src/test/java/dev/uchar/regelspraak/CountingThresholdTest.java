package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A counting quantifier with threshold 0 ("ten minste 0 van de …") is refused (RGS-203) wherever the wire writes it
 * as a compound, as the verifier refuses it there (WF-041): on the condition side, and also in consistency criteria,
 * a nested criterion and a compound subselection, which reached the verifier without a word.
 */
class CountingThresholdTest {

    static final String MODEL = """
        Objecttype de Schuit (mv: Schuiten)
            het aantal zware ladingen Numeriek (geheel getal);

        Objecttype de Lading (mv: Ladingen)
            het gewicht Numeriek (geheel getal);
            de lengte Numeriek (geheel getal);

        Feittype belading
            de schuit Schuit
            de lading (mv: ladingen) Lading
        Eén schuit heeft meerdere ladingen

        """;

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
        consistency criteria |15| Regel keuring\\n    geldig altijd\\n        Een Lading moet voldoen aan ten minste 0 van de volgende criteria:\\n        - zijn gewicht is groter dan 0\\n        - zijn lengte is groter dan 10.
        nested criterion     |17| Regel keuring\\n    geldig altijd\\n        Een Lading moet voldoen aan alle volgende criteria:\\n        - zijn gewicht is groter dan 0\\n        - er wordt voldaan aan ten minste 0 van de volgende criteria:\\n        .. zijn lengte is groter dan 10\\n        .. zijn gewicht is kleiner dan 50.
        compound subselection|15| Regel zwaar\\n    geldig altijd\\n        Het aantal zware ladingen van een Schuit moet berekend worden als het aantal ladingen van de Schuit die aan ten minste 0 van de volgende voorwaarden voldoen:\\n        - zijn gewicht is groter dan 100\\n        - zijn lengte is groter dan 10.
        inline subselection  |15| Regel zwaar\\n    geldig altijd\\n        Het aantal zware ladingen van een Schuit moet berekend worden als het aantal ladingen van de Schuit die aan ten minste 0 van de volgende voorwaarden voldoen: zijn gewicht is groter dan 100.
        """)
    void a_threshold_of_zero_is_refused(String where, int line, String rule) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rule.replace("\\n", "\n") + "\n");
        List<Diagnostic> errors = m.errors();
        assertEquals(1, errors.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        assertEquals("RGS-203", errors.get(0).code(), errors.get(0).message());
        assertEquals(line, errors.get(0).line(), errors.get(0).message());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
        consistency criteria | Regel keuring\\n    geldig altijd\\n        Een Lading moet voldoen aan ten minste 1 van de volgende criteria:\\n        - zijn gewicht is groter dan 0\\n        - zijn lengte is groter dan 10.
        compound subselection| Regel zwaar\\n    geldig altijd\\n        Het aantal zware ladingen van een Schuit moet berekend worden als het aantal ladingen van de Schuit die aan ten minste 1 van de volgende voorwaarden voldoen:\\n        - zijn gewicht is groter dan 100\\n        - zijn lengte is groter dan 10.
        """)
    void a_threshold_of_one_stays_clean(String where, String rule) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rule.replace("\\n", "\n") + "\n");
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
    }
}
