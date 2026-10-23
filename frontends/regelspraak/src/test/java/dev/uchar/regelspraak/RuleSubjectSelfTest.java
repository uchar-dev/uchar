package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rule subject in a subselection — "zelf" (card subselection-reads-subject; grammar/DIVERGENCES.md, "The rule
 * subject in a subselection — zelf"; IR-SPEC ch. 4.1a, WF-122). The filter of a fold and the test of a quantifier fold
 * are read per element, with the element as their subject; "de Vergunning zelf" names the subject of the rule there,
 * and its reads carry the form anchor. The bare type name names the element and the rule subject alike where both are a
 * Vergunning and stays refused (RGS-349, RGS-350), now with "zelf" as the way out; a read the anchor cannot write is
 * refused by RGS-358. The word stays free text in the names of a scenario and a rule.
 */
class RuleSubjectSelfTest {

    /** The model of the golden jutterswet-registervoorrang: a Register holds Vergunningen. */
    static final String MODEL = """
        Objecttype de Vergunning (mv: Vergunningen)
            de ingangsdag Datum in dagen;
            het tarief Numeriek (geheel getal);
            het hoogste latere tarief Numeriek (geheel getal);
            de voorrang kenmerk (bezittelijk);
            de proef kenmerk (bezittelijk);

        Objecttype het Register (mv: Registers)
            het nummer Numeriek (geheel getal);

        Feittype registerinschrijving
            het register Register
            de vergunning (mv: vergunningen) Vergunning
        Eén register heeft meerdere vergunningen

        """;

    private static String tarief(String filter) {
        return MODEL + """
            Regel hoogste latere tarief
                geldig altijd
                    Het hoogste latere tarief van een Vergunning moet berekend worden als de maximale waarde van het tarief van alle vergunningen van het register van de Vergunning die aan de volgende voorwaarde voldoen: %s.
            """.formatted(filter);
    }

    private static String voorrang(String condition) {
        return MODEL + """
            Regel voorrang
                geldig altijd
                    Een Vergunning heeft de voorrang
                    indien %s.
            """.formatted(condition);
    }

    private static String clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.diagnostics.stream().map(Diagnostic::toString).toList());
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static Diagnostic only(String source, String code) {
        RegelSpraakModel m = Resolver.resolve(source);
        List<Diagnostic> found = m.diagnostics.stream().filter(d -> code.equals(d.code())).toList();
        assertEquals(1, found.size(), () -> "precies één " + code + ": " + m.diagnostics);
        assertTrue(found.get(0).isError(), () -> "" + found.get(0));
        return found.get(0);
    }

    private static final String ANCHORED_INGANGSDAG = "{\"node\":\"AttributeReference\",\"attribute\":\"ingangsdag\","
            + "\"subjectType\":\"Vergunning\",\"anchor\":\"form\",\"source\":{\"snippet\":\"deingangsdagvandeVergunningzelf\"}}";

    // ---- the filter of a fold (RGS-350) ----

    @Test void the_bare_type_name_in_a_filter_stays_refused_and_names_the_way_out() {
        Diagnostic d = only(tarief("zijn ingangsdag is later dan de ingangsdag van de Vergunning"), "RGS-350");
        assertTrue(d.message().contains("'de ingangsdag van de Vergunning'"), d::message);
        assertTrue(d.message().contains("de Vergunning zelf"), () -> "de melding noemt de uitweg: " + d.message());
    }

    @Test void zelf_in_a_filter_reads_the_rule_subject() {
        String ir = clean(tarief("zijn ingangsdag is later dan de ingangsdag van de Vergunning zelf"));
        assertTrue(ir.contains("\"right\":" + ANCHORED_INGANGSDAG), ir);
        assertTrue(ir.contains("\"left\":{\"node\":\"AttributeReference\",\"attribute\":\"ingangsdag\",\"subjectType\":"
                + "\"Vergunning\",\"source\":{\"snippet\":\"zijningangsdag\"}}"), () -> "'zijn' blijft het element: " + ir);
    }

    // ---- the test of a quantifier fold (RGS-349) ----

    @Test void the_bare_type_name_in_a_quantifier_test_stays_refused_and_names_the_way_out() {
        for (String read : List.of("de ingangsdag van de Vergunning", "zijn ingangsdag")) {
            Diagnostic d = only(voorrang("de ingangsdag van alle vergunningen van het register van de Vergunning later is "
                    + "dan " + read), "RGS-349");
            assertTrue(d.message().contains("'" + read + "'"), d::message);
            assertTrue(d.message().contains("de Vergunning zelf"), () -> "de melding noemt de uitweg: " + d.message());
        }
    }

    @Test void zelf_in_a_quantifier_test_reads_the_rule_subject() {
        String ir = clean(voorrang("de ingangsdag van alle vergunningen van het register van de Vergunning later is dan de "
                + "ingangsdag van de Vergunning zelf"));
        assertTrue(ir.contains("\"right\":" + ANCHORED_INGANGSDAG), ir);
    }

    // ---- the identity check ----

    @Test void the_identity_check_in_a_bullet_and_in_a_subselection() {
        String bullet = clean(MODEL + """
            Regel hoogste latere tarief
                geldig altijd
                    Het hoogste latere tarief van een Vergunning moet berekend worden als de maximale waarde van het tarief van alle vergunningen van het register van de Vergunning die aan alle volgende voorwaarden voldoen:
                    - hij is niet de Vergunning zelf
                    - zijn tarief is groter dan 0.
            """);
        assertTrue(bullet.contains("{\"node\":\"IdentityCheck\",\"anchor\":\"form\",\"negated\":true,\"source\":{\"snippet\":"
                + "\"hijisnietdeVergunningzelf\"}}"), bullet);
        String negated = clean(voorrang("de ingangsdag van alle vergunningen van het register van de Vergunning die niet de "
                + "Vergunning zelf zijn later is dan 01-01-2026"));
        assertTrue(negated.contains("\"filter\":{\"node\":\"IdentityCheck\",\"anchor\":\"form\",\"negated\":true,"), negated);
        String affirmed = clean(voorrang("de ingangsdag van alle vergunningen van het register van de Vergunning die de "
                + "Vergunning zelf zijn later is dan 01-01-2026"));
        assertTrue(affirmed.contains("\"filter\":{\"node\":\"IdentityCheck\",\"anchor\":\"form\",\"negated\":false,"), affirmed);
    }

    @Test void the_identity_check_outside_a_subselection_is_refused() {
        Diagnostic d = only(voorrang("hij niet de Vergunning zelf is"), "RGS-358");
        assertTrue(d.message().contains("identiteitstoets"), d::message);
    }

    // ---- a characteristic of the rule subject ----

    @Test void a_characteristic_of_the_rule_subject_in_a_subselection_carries_the_anchor() {
        String ir = clean(MODEL + """
            Regel hoogste latere tarief
                geldig altijd
                    Het hoogste latere tarief van een Vergunning moet berekend worden als de maximale waarde van het tarief van alle vergunningen van het register van de Vergunning die aan alle volgende voorwaarden voldoen:
                    - hij heeft de voorrang
                    - de Vergunning zelf heeft de proef.
            """);
        assertTrue(ir.contains("{\"node\":\"CharacteristicCheck\",\"characteristic\":\"voorrang\",\"negated\":false,"
                + "\"source\""), () -> "'hij' blijft het element: " + ir);
        assertTrue(ir.contains("{\"node\":\"CharacteristicCheck\",\"characteristic\":\"proef\",\"negated\":false,"
                + "\"anchor\":\"form\",\"source\""), ir);
    }

    // ---- RGS-358 ----

    @Test void zelf_after_the_name_of_another_type_is_refused() {
        Diagnostic d = only(tarief("zijn ingangsdag is later dan de ingangsdag van het Register zelf"), "RGS-358");
        assertTrue(d.message().contains("'Register'") && d.message().contains("Vergunning"), d::message);
    }

    /** B6 of the card: the element of an enclosing subselection has no name in the inner one; the read from the inner
     *  element over its role stays. */
    @Test void the_element_of_an_enclosing_subselection_is_refused() {
        String model = """
            Objecttype de Haven (mv: Havens)
                de drukte Numeriek (geheel getal);
            Objecttype de Schuit (mv: Schuiten)
                de afmeerdag Datum in dagen;
                de klachtenteller Numeriek (geheel getal);
            Objecttype de Klacht (mv: Klachten)
                de einddag Datum in dagen;
            Feittype ligplaats
                de haven Haven
                de schuit (mv: schuiten) Schuit
            Eén haven heeft meerdere schuiten
            Feittype schuitklacht
                de beklaagde schuit Schuit
                de klacht (mv: klachten) Klacht
            Eén beklaagde schuit heeft meerdere klachten

            Regel drukte
                geldig altijd
                    De drukte van een Haven moet berekend worden als het aantal schuiten van de Haven die aan de volgende voorwaarde voldoen: het aantal klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn einddag is later dan %s is groter dan 0.
            """;
        Diagnostic d = only(model.formatted("de afmeerdag van de Schuit"), "RGS-358");
        assertTrue(d.message().contains("Schuit") && d.message().contains("omsluitende deelselectie"), d::message);
        String ir = clean(model.formatted("de afmeerdag van de beklaagde schuit van de Klacht"));
        assertTrue(ir.contains("\"attribute\":\"afmeerdag\",\"subjectType\":\"Schuit\",\"source\":{\"snippet\":\"deafmeerdag"
                + "vandebeklaagdeschuitvandeKlacht\"},\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"schuitklacht\","
                + "\"role\":\"beklaagdeschuit\"}]"), ir);
    }

    // ---- outside a subselection the word changes nothing ----

    @Test void zelf_at_the_rule_level_reads_what_the_bare_name_reads() {
        String withSelf = clean(voorrang("de ingangsdag van de Vergunning zelf gevuld is"));
        String without = clean(voorrang("de ingangsdag van de Vergunning gevuld is"));
        assertEquals(without, withSelf.replace("deingangsdagvandeVergunningzelf", "deingangsdagvandeVergunning"));
        String check = clean(voorrang("de Vergunning zelf de proef heeft"));
        assertTrue(check.contains("\"condition\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"proef\","
                + "\"negated\":false,\"source\""), check);
    }

    // ---- the word stays free text in the names of a scenario and a rule (answer of the werkbank) ----

    private static final String RULE_NAME = MODEL + """
        Regel voorrang van een vergunning dat zelf is geëindigd
            geldig altijd
                Een Vergunning heeft de voorrang
                indien zijn tarief groter is dan 10.

        Scenario voorrang van het recht zelf in een samenloop
        toetst de regel voorrang van een vergunning dat zelf is geëindigd
        met peildatum 01-03-2026
        Gegeven is de volgende situatie:
        een Vergunning V
        - het tarief is 20
        Met als verwacht resultaat:
        - V heeft de voorrang
        """;

    private static final String SCOPE_LIST = MODEL + """
        Regel proef zelf
            geldig altijd
                Een Vergunning heeft de proef
                indien zijn tarief groter is dan 15.

        Regel tarief zelf
            geldig altijd
                Het hoogste latere tarief van een Vergunning moet gesteld worden op zijn tarief.

        Scenario de proef zelf en het tarief zelf
        toetst de regels proef zelf en tarief zelf
        met peildatum 01-03-2026
        Gegeven is de volgende situatie:
        een Vergunning V
        - het tarief is 20
        Met als verwacht resultaat:
        - V heeft de proef
        - het hoogste latere tarief van V is 20
        """;

    /** v2 of the werkbank writes "zelf" in two scenario names; "… van het recht zelf in een samenloop" and "… dat zelf is
     *  geëindigd" resolve as they did before the word became a token, as any other free word of the name, and so do the
     *  rule names with "zelf" in the list of a scope. */
    @Test void zelf_stays_free_text_in_the_names_of_a_scenario_and_a_rule() {
        sameAsAnotherWord(RULE_NAME, "voorrang van het recht zelf in een samenloop",
                List.of("voorrang van een vergunning dat zelf is geëindigd"));
        sameAsAnotherWord(SCOPE_LIST, "de proef zelf en het tarief zelf", List.of("proef zelf", "tarief zelf"));
    }

    /** {@code source} resolves without a diagnostic, with the scenario and rule names as written, and emits what the same
     *  document with "ook" for "zelf" emits, but for that word: the IR, the case file and the scope. */
    private static void sameAsAnotherWord(String source, String scenario, List<String> rules) {
        ScenarioResolver.ScenarioBundle b = ScenarioResolver.resolve(source);
        assertEquals(List.of(), b.model().diagnostics.stream().map(Diagnostic::toString).toList());
        assertEquals(List.of(scenario), b.scenarios().stream().map(ResolvedScenario::name).toList());
        assertEquals(rules, b.model().resolvedRules.stream().map(r -> r.name()).toList());
        ScenarioResolver.ScenarioBundle other = ScenarioResolver.resolve(source.replace("zelf", "ook"));
        assertEquals(List.of(), other.model().diagnostics.stream().map(Diagnostic::toString).toList());
        assertEquals(IrEmitter.emit(other.model()), IrEmitter.emit(b.model()).replace("zelf", "ook"));
        assertEquals(CaseEmitter.emit(other.scenarios().get(0)), CaseEmitter.emit(b.scenarios().get(0)));
        assertEquals(String.valueOf(other.scenarios().get(0).scope()),
                String.valueOf(b.scenarios().get(0).scope()).replace("zelf", "ook"));
    }
}
