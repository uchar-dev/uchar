package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A timeline that the value or the condition of a rule on a timeline target reads per period lies on a grid that
 * fits the target's (IR-SPEC WF-126; RegelSpraak typeringen §3): a timeline for every month cannot carry a cut on a
 * day inside a month. The frontend refuses such a rule with an ERROR, in the verifier's terms; a coarser operand and
 * the operand of a whole-period qualifier pass, as do the producers with a grid of their own.
 */
class FinerTimelineTest {

    static final String MODEL = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Eenheidsysteem Tijd
            de dag (mv: dagen) dg
            het jaar (mv: jaren) jr

        Parameter de dagrentevoet : Numeriek (getal met 2 decimalen) met eenheid 1/jr voor elke dag;
        Parameter de jaarrentevoet : Numeriek (getal met 2 decimalen) met eenheid 1/jr voor elk jaar;

        Objecttype de Afdracht (mv: Afdrachten)
            het afdrachtbedrag Numeriek (geheel getal) met eenheid €;
            is rentedragend kenmerk (bijvoeglijk) voor elke dag;
            is verzuimd kenmerk (bijvoeglijk) voor elke maand;
            de maandrente Numeriek (getal met 2 decimalen) met eenheid €/jr voor elke maand;
            de jaarpremie Numeriek (geheel getal) voor elk jaar;
            de minimumdagen Numeriek (niet-negatief geheel getal) voor elke maand;

        """;

    private static List<Diagnostic> errors(String rules) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rules);
        return m.diagnostics.stream().filter(d -> d.severity() == Severity.ERROR).toList();
    }

    private static void refused(String rules, String operand, String operandGrid, String targetGrid) {
        List<Diagnostic> errors = errors(rules);
        assertEquals(1, errors.size(), () -> "one ERROR expected, got " + errors);
        Diagnostic d = errors.get(0);
        assertEquals("RGS-374", d.code());
        assertEquals(operand, String.valueOf(d.params().get("operand")));
        assertEquals(operandGrid, String.valueOf(d.params().get("operandGrid")));
        assertEquals(targetGrid, String.valueOf(d.params().get("targetGrid")));
    }

    @Test
    void aParameterForEveryDayInATargetForEveryMonthIsRefused() {
        refused("""
            Regel maandrente
                geldig altijd
                    De maandrente van een Afdracht moet berekend worden als de dagrentevoet maal zijn afdrachtbedrag.
            """, "dagrentevoet", "dag", "maand");
    }

    @Test
    void aCharacteristicForEveryDayInTheConditionOfATargetForEveryMonthIsRefused() {
        refused("""
            Regel maandrente
                geldig altijd
                    De maandrente van een Afdracht moet berekend worden als de jaarrentevoet maal zijn afdrachtbedrag gedurende de tijd dat hij rentedragend is.
            """, "rentedragend", "dag", "maand");
    }

    @Test
    void aQualifierForEveryMonthInTheConditionOfATargetForEveryYearIsRefused() {
        refused("""
            Regel jaarpremie
                geldig altijd
                    De jaarpremie van een Afdracht moet gesteld worden op 20 gedurende de tijd dat hij gedurende de gehele maand rentedragend is.
            """, "de gehele maand", "maand", "jaar");
    }

    @Test
    void aCoarserOperandTheOperandOfAQualifierAndAProducerPass() {
        assertEquals(List.of(), errors("""
            Regel maandrente
                geldig altijd
                    De maandrente van een Afdracht moet berekend worden als de jaarrentevoet maal zijn afdrachtbedrag gedurende de tijd dat hij verzuimd is.

            Regel minimumdagen
                geldig altijd
                    De minimumdagen van een Afdracht moet berekend worden als (het aantal dagen in de maand dat hij rentedragend is) gedurende de tijd dat hij niet gedurende de gehele maand rentedragend is.
            """));
    }
}
