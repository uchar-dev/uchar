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
 * Two forms that reached disk silently while the verifier refused them, measured on the frontend before this
 * fence (78edd45d).
 *
 * <p><b>A rule target that is no attribute of the subject.</b> A rule writes an attribute or a characteristic
 * of its own universal subject (WF-022). "De vakpremie van het strandvak van een Jutter moet gesteld worden op 10"
 * went out as a target {@code 'vakpremie van het strandvak'} on Jutter — the name swallowed the navigation — with
 * {@code diagnostics: []}; the verifier gave WF-022 and WF-034. The same for an undeclared attribute, and for a
 * characteristic whose subject is a navigation ("Het strandvak van een Jutter is gemarkeerd": no object type at
 * all). An undeclared characteristic on a resolved subject is left alone here: that is the ground the dialect card
 * for negated decisions ("is niet X") covers.
 *
 * <p><b>A window bullet as the LAST bullet of a compound subselection.</b> The parser ends the subselection at
 * "het is de periode" — read as a characteristic of that name, a marker — and takes the bound as a window on the
 * whole RULE: a different document from the one written, with warnings about the marker only. The same bullet
 * written first is read as a window inside the filter.
 */
class RuleTargetTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal);

        Objecttype het Strandvak (mv: Strandvakken)
            de vakpremie Numeriek (geheel getal);
            is gemarkeerd kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            is gewogen kenmerk (bijvoeglijk);

        Feittype vaktoewijzing
            de vakhouder Jutter
            het strandvak Strandvak
        Eén vakhouder heeft één strandvak

        Feittype vondstaanmelding
            de aanmelder Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmelder heeft meerdere vondsten
        """;

    private static String regel(String resultaat) {
        return MODEL + """

            Regel doel
                geldig altijd
                    %s
            """.formatted(resultaat);
    }

    private static Diagnostic onlyError(RegelSpraakModel m) {
        List<Diagnostic> fouten = m.errors();
        assertEquals(1, fouten.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        return fouten.get(0);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "De vakpremie van het strandvak van een Jutter moet gesteld worden op 10.  | genavigeerd",
            "De vakpremie van het strandvak van een Jutter moet berekend worden als 10. | genavigeerd",
            "De onbekende premie van een Jutter moet gesteld worden op 10.             | geen gedeclareerd attribuut van Jutter",
            "Het strandvak van een Jutter is gemarkeerd.                               | lost niet op tot een objecttype",
            // before: IrEmitter's exception ("het doel van regel 'doel' … navigeert; het doel legt het onderwerp vast")
            "De premie van zijn vakhouder moet gesteld worden op 10.                   | het doel navigeert via 'zijn vakhouder'"
    })
    void a_target_outside_the_subject_is_refused(String resultaat, String fragment) {
        RegelSpraakModel m = Resolver.resolve(regel(resultaat));
        Diagnostic d = onlyError(m);
        assertTrue(d.message().contains(fragment) && d.message().contains("WF-022"), d.message());
        assertFalse(m.isOk(), "een ERROR houdt het document van de schijf");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "De vakpremie van een Strandvak moet gesteld worden op 10.",
            "De premie van een Jutter moet berekend worden als 10.",
            "Een Strandvak is gemarkeerd."
    })
    void a_own_target_stays_clean(String resultaat) {
        RegelSpraakModel m = Resolver.resolve(regel(resultaat));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    // -------------------------------------------------------------------- a refused target in an extension block

    /** Only the target refusals, and nothing of the lowering's: the lift checks judge a target the target fence
     *  already refused, and before this fix added "is geen kenmerktoekenning of gelijkstelling (bv. een
     *  initialisatie)" (a characteristic on a navigated subject) or "schrijft een ander doel" (an attribute
     *  target, an unordered member) — messages about a form the author did not write. */
    private static void onlyRuleTargetErrors(String src, int verwacht) {
        RegelSpraakModel m = Resolver.resolve(src);
        List<Diagnostic> fouten = m.errors();
        assertEquals(verwacht, fouten.size(), () -> "verwachtte " + verwacht + " fout(en), kreeg: " + fouten);
        assertTrue(fouten.stream().allMatch(d -> d.message().startsWith("regeldoel ")),
                () -> "alleen de regeldoelweigering hoort te spreken: " + fouten);
    }

    @Test void a_refused_characteristic_target_in_an_exception_family_speaks_only() {
        onlyRuleTargetErrors(MODEL + """

            Regel markering
                geldig altijd
                    Het strandvak van een Jutter is gemarkeerd.

            Uitzondering op markering: noodmarkering
                geldig altijd
                    Het strandvak van een Jutter is gemarkeerd.
            """, 2);
    }

    @Test void a_refused_attribute_target_in_an_exception_speaks_only() {
        onlyRuleTargetErrors(MODEL + """

            Regel premie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 10.

            Uitzondering op premie: vakpremie
                geldig altijd
                    De vakpremie van het strandvak van een Jutter moet gesteld worden op 20.
            """, 1);
    }

    @Test void a_refused_target_in_an_unordered_block_speaks_only() {
        onlyRuleTargetErrors(MODEL + """

            Uitzonderingen ongeordend:
            Regel premie een
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 10.

            Regel premie twee
                geldig altijd
                    De vakpremie van het strandvak van een Jutter moet gesteld worden op 20.
            """, 1);
    }

    @Test void a_period_bullet_as_last_in_a_subselection_is_refused() {
        String src = MODEL + """

            Regel premie
                geldig altijd
                    De premie van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan alle volgende voorwaarden voldoen:
                    - zij zijn gewogen
                    - het is de periode vanaf dd. 01-01-2024.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        Diagnostic d = onlyError(m);
        assertTrue(d.message().contains("'het is de periode'") && d.message().contains("§8.4.3"), d.message());
    }

    @Test void a_period_bullet_first_in_a_subselection_stays_a_filter_member() {
        String src = MODEL + """

            Regel premie
                geldig altijd
                    De premie van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan alle volgende voorwaarden voldoen:
                    - het is de periode vanaf dd. 01-01-2024
                    - zij zijn gewogen.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertNull(m.resolvedRegels.get(0).voorwaarde(), "het venster hoort in het filter, niet op de regel");
    }

    @Test void a_window_behind_a_single_subselection_stays_a_rule_window() {
        String src = MODEL + """

            Regel premie
                geldig altijd
                    De premie van een Jutter moet berekend worden als het aantal vondsten van de Jutter die gewogen zijn
                    vanaf dd. 01-01-2024.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }
}
