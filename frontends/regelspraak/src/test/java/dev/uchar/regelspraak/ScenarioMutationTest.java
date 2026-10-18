package dev.uchar.regelspraak;

import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundle;
import org.junit.jupiter.api.Test;

import static dev.uchar.regelspraak.ScenarioFixture.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The mutation gate of the scenariotoetsen resolver: one valid Jutterswet scenario document, and per mutation the
 * refusal it must draw, on the line the mutation touched. The document is clean as written, so every ERROR below is
 * the mutation's own; a mutation the resolver let through would lower to a case file that says something else than
 * the author wrote.
 */
class ScenarioMutationTest {

    private static final String DOCUMENT = """
        Situatie basis:
        een Jutter J
        - het vondstgewicht is 12,00 kg
        - de naam is "Aukje"
        de parameter de vondstdrempel is 3

        Situatie vervolg gebaseerd op de situatie basis:
        een Vondst V
        - de aanmelder is J

        Scenario bevoegd
        met peildatum 01-06-2025
        gebaseerd op de situatie vervolg
        Met als verwacht resultaat:
        - J is bergingsbevoegd
        - de kustwachtbonus van J is 3
        - de wachtbonus van J is 5
        - de gegevens zijn consistent
        """;

    /** {@link #DOCUMENT} with {@code van} replaced by {@code naar} exactly once. */
    private static String muteer(String van, String to) {
        int i = DOCUMENT.indexOf(van);
        assertTrue(i >= 0 && DOCUMENT.indexOf(van, i + 1) < 0, () -> "de mutatie '" + van + "' past niet precies één keer");
        return DOCUMENT.substring(0, i) + to + DOCUMENT.substring(i + van.length());
    }

    private static void refuses(String mutated, String merker, String fragment) {
        ScenarioBundle b = resolve(mutated);
        assertFalse(b.isOk(), "de mutatie hoort geweigerd te worden");
        errorOnLine(b, MODEL + mutated, merker, fragment);
    }

    @Test void the_unmutated_document_resolves_clean() {
        ScenarioBundle b = resolve(DOCUMENT);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        assertEquals(1, b.scenarios().size());
    }

    @Test void wrong_type_in_the_situation() {
        refuses(muteer("- het vondstgewicht is 12,00 kg", "- het vondstgewicht is waar"), "vondstgewicht is waar",
                "het attribuut 'vondstgewicht': toekenning: a boolean value is assigned to a number target");
    }

    @Test void wrong_type_in_the_expectation() {
        refuses(muteer("- de kustwachtbonus van J is 3", "- de kustwachtbonus van J is 01-01-2025"), "van J is 01-01-2025",
                "de verwachting voor 'kustwachtbonus': toekenning: a date-day value is assigned to a number target");
    }

    @Test void unknown_attribute_in_the_situation() {
        refuses(muteer("- het vondstgewicht is 12,00 kg", "- het vangstgewicht is 12,00 kg"), "vangstgewicht",
                "het objecttype 'Jutter' kent geen attribuut 'vangstgewicht'");
    }

    @Test void unknown_attribute_in_the_expectation() {
        refuses(muteer("- de wachtbonus van J is 5", "- de wachtpremie van J is 5"), "wachtpremie",
                "het objecttype 'Jutter' kent geen attribuut 'wachtpremie'");
    }

    @Test void unknown_label_in_the_expectation() {
        refuses(muteer("- J is bergingsbevoegd", "- K is bergingsbevoegd"), "K is bergingsbevoegd",
                "het label 'K' in de verwachting is nergens in de situatie gedeclareerd");
    }

    @Test void duplicate_situation() {
        String mutated = muteer("Situatie vervolg", "Situatie basis:\neen Jutter J\n\nSituatie vervolg");
        ScenarioBundle b = resolve(mutated);
        assertFalse(b.isOk());
        String complete = MODEL + mutated;
        int second = complete.indexOf("Situatie basis:", complete.indexOf("Situatie basis:") + 1);
        int regel = 1 + (int) complete.substring(0, second).chars().filter(c -> c == '\n').count();
        assertTrue(b.model().errors().stream().anyMatch(d -> d.line() == regel
                        && d.message().contains("de situatie 'basis' is meer dan eens gedeclareerd")),
                () -> "verwacht de dubbele declaratie op regel " + regel + ": " + b.model().errors());
    }

    @Test void cycle_via_based_on() {
        refuses(muteer("Situatie basis:", "Situatie basis gebaseerd op de situatie vervolg:"),
                "Situatie vervolg gebaseerd op",
                "de situaties vormen een kring via 'gebaseerd op': 'basis' → 'vervolg' → 'basis'");
    }

    @Test void cycle_on_itself() {
        refuses(muteer("Situatie basis:", "Situatie basis gebaseerd op de situatie basis:"), "Situatie basis gebaseerd",
                "de situaties vormen een kring via 'gebaseerd op': 'basis' → 'basis'");
    }

    @Test void unknown_base() {
        refuses(muteer("Situatie vervolg gebaseerd op de situatie basis:", "Situatie vervolg gebaseerd op de situatie bodem:"),
                "situatie bodem", "de situatie 'bodem' is nergens in het document gedeclareerd");
    }
}
