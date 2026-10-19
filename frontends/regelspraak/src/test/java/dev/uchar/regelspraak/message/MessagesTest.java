package dev.uchar.regelspraak.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The renderer and its locale API (card message-catalogues): the language of a message is the argument, else the
 *  system property, else the default of the component; nothing falls back, and every fault names the key. The
 *  entries used are real ones of the frontend's catalogue. */
class MessagesTest {

    private String saved;

    @BeforeEach void clearProperty() {
        saved = System.getProperty(Messages.PROPERTY);
        System.clearProperty(Messages.PROPERTY);
    }

    @AfterEach void restoreProperty() {
        if (saved == null) System.clearProperty(Messages.PROPERTY); else System.setProperty(Messages.PROPERTY, saved);
    }

    private static final Map<String, Object> ARGS = Map.of("name", "vondst");

    @Test void the_frontend_renders_dutch_unless_told_otherwise() {
        assertEquals(Locale.forLanguageTag("nl"), Messages.defaultLocale(Component.FRONTEND));
        assertTrue(Messages.render(Component.FRONTEND, "RGS-001", ARGS).startsWith("verdeling 'vondst' wordt niet geschreven"));
    }

    @Test void the_drools_backend_renders_english_unless_told_otherwise() {
        assertEquals(Locale.forLanguageTag("en"), Messages.defaultLocale(Component.DROOLS));
    }

    @Test void the_system_property_sets_the_default_language_of_every_component() {
        System.setProperty(Messages.PROPERTY, "en");
        assertEquals(Locale.forLanguageTag("en"), Messages.defaultLocale(Component.FRONTEND));
        assertTrue(Messages.render(Component.FRONTEND, "RGS-001", ARGS).startsWith("allocation 'vondst' is not written"));
        System.setProperty(Messages.PROPERTY, "nl");
        assertEquals(Locale.forLanguageTag("nl"), Messages.defaultLocale(Component.DROOLS));
    }

    @Test void an_explicit_locale_wins_over_the_property() {
        System.setProperty(Messages.PROPERTY, "nl");
        assertTrue(Messages.render(Component.FRONTEND, Locale.ENGLISH, "RGS-001", ARGS).startsWith("allocation 'vondst' is not written"));
    }

    @Test void a_language_without_a_catalogue_is_a_fault_with_the_key() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Messages.render(Component.FRONTEND, Locale.FRENCH, "RGS-001", ARGS));
        assertTrue(e.getMessage().contains("RGS-001") && e.getMessage().contains("fr"), e.getMessage());
        System.setProperty(Messages.PROPERTY, "fr");
        e = assertThrows(IllegalArgumentException.class, () -> Messages.render(Component.FRONTEND, "RGS-001", ARGS));
        assertTrue(e.getMessage().contains("RGS-001"), e.getMessage());
    }

    @Test void a_key_without_an_entry_is_a_fault_with_the_key() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Messages.render(Component.FRONTEND, "RGS-999", Map.of()));
        assertTrue(e.getMessage().contains("RGS-999"), e.getMessage());
    }

    @Test void a_place_without_a_parameter_is_a_fault_with_the_key() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Messages.render(Component.FRONTEND, "RGS-001", Map.of()));
        assertTrue(e.getMessage().contains("RGS-001") && e.getMessage().contains("{name}"), e.getMessage());
    }

    @Test void a_parameter_the_sentence_does_not_print_is_no_fault() {
        assertEquals(Messages.render(Component.FRONTEND, "RGS-001", ARGS),
                Messages.render(Component.FRONTEND, "RGS-001", Map.of("name", "vondst", "unused", "x")));
    }

    @Test void a_sentence_part_renders_in_the_language_of_the_sentence_around_it() {
        Map<String, Object> args = Map.of("objectType", "Vondst", "supertype", "Ding",
                "rationale", Msg.frontend("RGS-004.wf103"));
        assertTrue(Messages.render(Component.FRONTEND, Locale.forLanguageTag("nl"), "RGS-004", args).contains("een supertype noemt"));
        assertTrue(Messages.render(Component.FRONTEND, Locale.ENGLISH, "RGS-004", args).contains("a supertype names"));
    }

    @Test void a_list_renders_its_elements_joined_by_semicolons_and_an_enumeration_with_the_word_of_the_language() {
        Object list = List.of(Msg.frontend("RGS-323.side", "kind", "text"), "plain");
        assertEquals("operand of a numeric operator is text; plain", Messages.valueText(list, Locale.ENGLISH));
        Object three = new Msg.Enumeration(Component.FRONTEND, List.of("a", "b", "c"));
        assertEquals("a, b en c", Messages.valueText(three, Locale.forLanguageTag("nl")));
        assertEquals("a, b and c", Messages.valueText(three, Locale.ENGLISH));
    }

    @Test void a_brace_written_twice_is_a_literal_brace() {
        Map<String, Object> args = Map.of("unit", "u", "unitSystem", "s", "num", "0", "den", "0", "factor", "{\"num\":0,\"den\":0}");
        assertTrue(Messages.render(Component.FRONTEND, "RGS-015", args).contains("exact rational {num, den} whose"));
    }
}
