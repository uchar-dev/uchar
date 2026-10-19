package dev.uchar.regelspraak.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.uchar.regelspraak.refusal.DroolsCode;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The language of the Drools backend's refusals (card message-catalogues): English unless the system property or the
 *  caller says otherwise. The rest of the suite runs under the property {@code nl}, since its assertions are the Dutch
 *  sentences. */
class BackendMessageLanguageTest {

    private String saved;

    @BeforeEach void clearProperty() {
        saved = System.getProperty(Messages.PROPERTY);
        System.clearProperty(Messages.PROPERTY);
    }

    @AfterEach void restoreProperty() {
        if (saved == null) System.clearProperty(Messages.PROPERTY); else System.setProperty(Messages.PROPERTY, saved);
    }

    private static final String ENGLISH = "the DRL rendering does not yet carry a bean class as the constant of a term";
    private static final String DUTCH = "de DRL-weergave draagt een beanklasse als constante van een term nog niet";

    @Test void a_refusal_is_english_without_the_property() {
        assertEquals(ENGLISH, DroolsCode.DRL_BEAN_CLASS_CONSTANT.reason(
                "category", DroolsCode.DRL_BEAN_CLASS_CONSTANT.part("bean-class")).text());
    }

    @Test void the_property_and_the_caller_choose_the_language() {
        var reason = DroolsCode.DRL_BEAN_CLASS_CONSTANT.reason("category", DroolsCode.DRL_BEAN_CLASS_CONSTANT.part("bean-class"));
        System.setProperty(Messages.PROPERTY, "nl");
        assertEquals(DUTCH, reason.text());
        assertEquals(ENGLISH, reason.text(Locale.ENGLISH));
        assertEquals(DUTCH, reason.text(Locale.forLanguageTag("nl")));
    }

    @Test void a_language_without_a_catalogue_and_a_missing_parameter_are_faults_with_the_code() {
        var reason = DroolsCode.DRL_BEAN_CLASS_CONSTANT.reason("category", "x");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> reason.text(Locale.FRENCH));
        assertEquals(true, e.getMessage().contains("DROOLS-056"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> DroolsCode.DRL_BEAN_CLASS_CONSTANT.reason().text());
        assertEquals(true, e.getMessage().contains("DROOLS-056") && e.getMessage().contains("{category}"), e.getMessage());
    }
}
