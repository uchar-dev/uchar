package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScopeForms;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The public scope reading ({@link ScenarioResolver#scopeForms}): a consumer that scopes a model by rule names and rule
 * groups gets the forms a scenario with the same names carries. A rule name carries its forms or the rows of the
 * decision table of that name, never forms that only look like rows; a rule group carries its members, its name
 * compared as a scenario compares it; a defaults group comes with its stack. Names that carry no form and groups the
 * model lacks come back by name.
 */
class ScopeFormsTest {

    private static final String SOURCE = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);
            de premie Numeriek (geheel getal);
            de toeslag Numeriek (geheel getal);
            de vondstdag Datum in dagen;
            de vinddag Datum in dagen;
            de meldtermijn Datum in dagen;
            het vergunningseinde Datum in dagen;
        Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
            de strandgrens Datum in dagen;

        Beslistabel premietabel
            geldig altijd

        | | de premie van een Jutter moet gesteld worden op | indien zijn leeftijd groter is dan |
        |---|---|---|
        | 1 | 10 | 60 |
        | 2 | 5 | 18 |

        Regel vondstdag
            geldig altijd
                De vondstdag van een Jutter moet gesteld worden op zijn vinddag.

        Regel meldtermijn
            geldig altijd
                De meldtermijn van een Jutter moet gesteld worden op zijn vinddag
                indien zijn vinddag gevuld is.

        Afwijking van meldtermijn: meldtermijn tot de strandgrens
            geldig altijd
                De meldtermijn van een Strandjutter moet gesteld worden op zijn strandgrens
                indien zijn strandgrens gevuld is.

        Regelgroep Toeslagen
        Regel toeslag 1
            geldig altijd
                De toeslag van een Jutter moet gesteld worden op 3
                indien zijn leeftijd groter is dan 60.

        Regel toeslag 2
            geldig altijd
                De toeslag van een Jutter moet gesteld worden op 2
                indien zijn leeftijd kleiner is dan 18.

        """;

    private static final String SCENARIO = """
        Gegeven is de volgende situatie:
        een Jutter J
        - de vinddag is 10-03-2026
        Met als verwacht resultaat:
        - de vondstdag van J is 10-03-2026
        """;

    private static ScopeForms read(List<String> names, List<String> groups) {
        RegelSpraakModel model = Resolver.resolve(SOURCE);
        assertTrue(model.errors().isEmpty(), () -> model.errors().toString());
        return ScenarioResolver.scopeForms(model, model.prioritizedDefaults, names, groups);
    }

    /** The forms a scenario with header {@code scope} carries. */
    private static List<String> scenario(String scope) {
        ScenarioBundle b = ScenarioResolver.resolve(SOURCE + "Scenario s\n" + scope + SCENARIO);
        assertTrue(b.model().errors().isEmpty(), () -> b.model().errors().toString());
        return b.scenarios().get(0).scope().forms();
    }

    @Test void a_rule_name_carries_its_form_as_a_scenario_reads_it() {
        assertEquals(List.of("vondstdag"), read(List.of("Vondstdag"), List.of()).forms());
        assertEquals(scenario("toetst de regel vondstdag\n"), read(List.of("vondstdag"), List.of()).forms());
    }

    @Test void a_table_name_carries_its_rows_and_a_name_like_a_row_is_no_table() {
        assertEquals(List.of("premietabel 1", "premietabel 2"), read(List.of("premietabel"), List.of()).forms());
        assertEquals(scenario("toetst de regel premietabel\n"), read(List.of("premietabel"), List.of()).forms());
        ScopeForms toeslag = read(List.of("toeslag"), List.of());
        assertEquals(List.of(), toeslag.forms(), "the rules 'toeslag 1' and 'toeslag 2' are no rows of a table");
        assertEquals(List.of("toeslag"), toeslag.withoutForm());
    }

    @Test void a_rule_group_carries_its_members_and_its_name_compares_case_insensitively() {
        ScopeForms group = read(List.of(), List.of("toeslagen"));
        assertEquals(List.of("toeslag 1", "toeslag 2"), group.forms());
        assertEquals(scenario("toetst de regelgroep TOESLAGEN\n"), group.forms());
        assertEquals(List.of("nergens"), read(List.of(), List.of("toeslagen", "nergens")).unknownGroups());
    }

    @Test void a_defaults_group_comes_with_its_stack() {
        assertEquals(List.of("meldtermijn voor Strandjutter", "meldtermijn"),
                read(List.of("meldtermijn voor Strandjutter"), List.of()).forms());
        assertEquals(scenario("toetst de regel meldtermijn voor Strandjutter\n"),
                read(List.of("meldtermijn voor Strandjutter"), List.of()).forms());
        assertEquals(scenario("toetst de regels vondstdag en meldtermijn\n"),
                read(List.of("vondstdag", "meldtermijn"), List.of()).forms());
    }
}
