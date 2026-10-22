package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A variable of the rule ("Daarbij geldt:", §13.4.2.10) read in a subselection means what it means outside it: its
 * expression belongs to the rule subject, so the frontend writes it from the element with the way back to the rule
 * subject in front of it (card rule-variable-in-subselection; grammar/DIVERGENCES.md, "A read of the rule subject in a
 * filter"). Until then a read of two steps and a fold were refused, and a read of one step bound by the type pair.
 */
class RuleVariableInSubselectionTest {

    static final String MODEL = """
        Eenheidsysteem Tijd
            de dag dg
            de maand mnd

        Objecttype de Schuit (mv: Schuiten)
            de eerste schone maand Datum in dagen;
            de peildatum Datum in dagen;

        Objecttype het Verzoek (mv: Verzoeken)
            de gewenste dag Datum in dagen;

        Objecttype de Lading in-een-maand (mv: Ladingen in-een-maand)
            de maand Datum in dagen;
            het gewicht Numeriek (geheel getal);

        Feittype lading in-een-maand
            de schuit Schuit
            de lading (mv: ladingen) Lading in-een-maand
        Eén schuit heeft meerdere ladingen

        Feittype schuitverzoek
            de verzochte schuit Schuit
            het verzoek Verzoek
        Eén verzochte schuit heeft één verzoek

        """;

    static String rule(String model, String comparand, String definition) {
        return model + """
            Regel eerste schone maand
                geldig altijd
                    De eerste schone maand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen:
                    - zijn gewicht is kleiner dan 100
                    - zijn maand plus 1 mnd is later dan\s""" + comparand + "."
                + (definition == null ? "" : "\n        Daarbij geldt:\n            de peildag is " + definition + ".") + "\n";
    }

    @Test void r1_a_variable_of_two_steps_reads_as_inline() {
        RegelSpraakModel m = Resolver.resolve(rule(MODEL, "de peildag", "de gewenste dag van het verzoek van de Schuit"));
        assertTrue(m.diagnostics.isEmpty(), () -> "R1 gaat zonder diagnostiek: " + m.diagnostics);
        assertEquals(comparand(FoldInElementConditionTest.clean(rule(MODEL, "de gewenste dag van het verzoek van de Schuit", null))),
                comparand(m), "de variabele leest wat de inline lezing R2 leest");
    }

    @Test void a_variable_of_one_step_reads_as_inline() {
        RegelSpraakModel m = Resolver.resolve(rule(MODEL, "de peildag", "de peildatum van de Schuit"));
        assertTrue(m.diagnostics.isEmpty(), () -> "" + m.diagnostics);
        assertEquals(comparand(FoldInElementConditionTest.clean(rule(MODEL, "de peildatum van de Schuit", null))), comparand(m));
    }

    @Test void a_variable_of_literals_reads_as_written() {
        RegelSpraakModel m = Resolver.resolve(rule(MODEL, "de peildag", "01-03-2026"));
        assertTrue(m.diagnostics.isEmpty(), () -> "" + m.diagnostics);
        assertEquals(comparand(FoldInElementConditionTest.clean(rule(MODEL, "01-03-2026", null))), comparand(m));
    }

    @Test void r4_a_fold_with_a_filter_as_variable_reads_as_inline() {
        RegelSpraakModel m = Resolver.resolve(FoldInElementConditionTest.MODEL + """
            Regel eerste schone maand vanaf de eindmaand
                geldig altijd
                    De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen:
                    - hij heeft geen overlading
                    - zijn maand plus 1 mnd is later dan de laatste einddag.
                    Daarbij geldt:
                        de laatste einddag is de laatste van de einddag van alle klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "R4 gaat zonder diagnostiek: " + m.diagnostics);
        assertEquals(FoldInElementConditionTest.filter(FoldInElementConditionTest.clean(FoldInElementConditionTest.R1)),
                FoldInElementConditionTest.filter(m), "de variabele leest wat de inline vouw R1 leest");
    }

    @Test void a_variable_outside_the_subselection_is_unchanged() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel eerste schone maand
                geldig altijd
                    De eerste schone maand van een Schuit moet gesteld worden op de peildag
                    indien de peildag gevuld is.
                    Daarbij geldt:
                        de peildag is de gewenste dag van het verzoek van de Schuit.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "R3 gaat zonder diagnostiek: " + m.diagnostics);
    }

    @Test void a_variable_without_a_way_back_is_refused() {
        String manyToMany = MODEL.replace("Eén schuit heeft meerdere ladingen", "meerdere schuiten hebben meerdere ladingen");
        RegelSpraakModel m = Resolver.resolve(rule(manyToMany, "de peildag", "de gewenste dag van het verzoek van de Schuit"));
        Diagnostic d = FoldInElementConditionTest.only(m, "RGS-352");
        for (String part : List.of("de peildag", "de gewenste dag van het verzoek van de Schuit", "eigen lid"))
            assertTrue(d.message().contains(part), () -> "de weigering noemt '" + part + "': " + d.message());
    }

    @Test void a_variable_in_a_subselection_over_the_rule_subjects_own_type_is_refused() {
        String towing = MODEL.replace("Objecttype het Verzoek", """
                Feittype sleep
                    de sleper Schuit
                    de gesleepte (mv: gesleepten) Schuit
                Eén sleper heeft meerdere gesleepten

                Objecttype het Verzoek""");
        RegelSpraakModel m = Resolver.resolve(towing + """
            Regel eerste schone maand
                geldig altijd
                    De eerste schone maand van een Schuit moet berekend worden als de eerste van de peildatum van alle gesleepten van de Schuit die aan de volgende voorwaarde voldoen: zijn peildatum is later dan de peildag.
                    Daarbij geldt:
                        de peildag is zijn peildatum.
            """);
        Diagnostic d = FoldInElementConditionTest.only(m, "RGS-350");
        assertTrue(d.message().contains("de peildag"), () -> "de weigering noemt de variabele: " + d.message());
    }

    /** The right side of the second condition on the ladingen, without source records. */
    private static Object comparand(RegelSpraakModel m) {
        Map<?, ?> filter = (Map<?, ?>) FoldInElementConditionTest.filter(m);
        Map<?, ?> second = (Map<?, ?>) ((List<?>) filter.get("parts")).get(1);
        return second.get("right");
    }
}
