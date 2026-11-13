package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Within one declaration a name stands once, case not counted (§13.2.30): the attributes and the characteristics of an
 * object type (WF-002), the roles of a fact type (WF-012), the values of an enumeration (WF-006) and the labels of a
 * dimension (WF-014); and a timeline is declared once per name, also where both declarations agree (WF-001, WF-015).
 * Measured on the frontend before this check: an exact repeat of an attribute replaced the first without a word and
 * the verifier passed the document; a repeat in other letters was a WARNING; the other lists and the timelines reached
 * the verifier without a diagnostic.
 */
class RepeatedNameTest {

    private static final String RULE = """

        Regel waarde
            geldig altijd
                De waarde van een Vondst moet berekend worden als 1.
        """;

    /** The ERRORs of {@code source} as "code line name earlierLine". */
    private static List<String> errors(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        return m.errors().stream().map(RepeatedNameTest::row).toList();
    }

    private static String row(Diagnostic d) {
        return d.code() + " " + d.line() + " " + d.params().get("name") + " " + d.params().get("earlierLine");
    }

    @Test void an_exact_repeat_of_an_attribute_is_refused() {
        assertEquals(List.of("RGS-431 3 waarde 2"), errors("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
                de waarde Tekst;
            """ + RULE));
    }

    @Test void a_repeat_of_an_attribute_in_other_letters_is_an_error() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
                de vondstwaarde Numeriek (geheel getal);
                de vondstWaarde Numeriek (geheel getal);
            """ + RULE);
        assertEquals(List.of("RGS-431 4 vondstWaarde 3"), m.errors().stream().map(RepeatedNameTest::row).toList());
        assertEquals("objecttype 'Vondst': het attribuut 'vondstWaarde' is in deze declaratie al gedeclareerd als 'vondstwaarde' "
                + "(bronregel 3); een attribuutnaam komt in een objecttype één keer voor, hoofdletters niet meegeteld "
                + "(WF-002)", m.errors().get(0).message());
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte alleen de fout, kreeg: " + m.diagnostics);
    }

    @Test void a_repeat_of_a_characteristic_is_refused() {
        assertEquals(List.of("RGS-431 4 laat 3"), errors("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
                is laat kenmerk (bijvoeglijk);
                is laat kenmerk (bijvoeglijk);
            """ + RULE));
    }

    /** An attribute and a characteristic are two lists, so they may share a name. */
    @Test void an_attribute_and_a_characteristic_may_share_a_name() {
        assertEquals(List.of(), errors("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
                de bijzonder Numeriek (geheel getal);
                is bijzonder kenmerk (bijvoeglijk);
            """ + RULE));
    }

    /** A literal repeat inside one extension is a repeat within one declaration, not the admitted repeat of a member
     *  the type carries. */
    @Test void a_repeat_within_one_extension_is_refused() {
        assertEquals(List.of("RGS-431 6 prijs 5"), errors("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);

            Extensie van objecttype de Vondst
                de prijs Numeriek (geheel getal);
                de prijs Numeriek (geheel getal);
            """ + RULE));
    }

    @Test void a_repeat_of_a_role_is_refused() {
        assertEquals(List.of("RGS-431 7 vervoerder 6"), errors("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
            Objecttype de Schuit (mv: Schuiten)

            Feittype belading
                de vervoerder Schuit
                de vervoerder Vondst
            Eén vervoerder heeft één vervoerder
            """ + RULE));
    }

    @Test void a_repeat_of_an_enumeration_value_is_refused() {
        assertEquals(List.of("RGS-431 3 Drijfgoed 2"), errors("""
            Domein Vondstklasse is van het type Enumeratie
                'drijfgoed'
                'Drijfgoed'

            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
                de klasse Vondstklasse;
            """ + RULE));
    }

    @Test void a_repeat_of_a_dimension_label_is_refused() {
        assertEquals(List.of("RGS-431 3 Voorjaar 2"), errors("""
            Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
                1. voorjaar
                2. Voorjaar

            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
                de prijs Numeriek (geheel getal) gedimensioneerd met seizoen;
            """ + RULE));
    }

    /** IR-SPEC WF-001: "even an agreeing re-declaration is ill-formed". */
    @Test void a_timeline_declared_twice_alike_is_refused() {
        RegelSpraakModel m = Resolver.resolve("""
            Tijdlijn het penningjaar heeft tijdvakken van een jaar startend op dag 1 van maand 1;
            Tijdlijn het penningjaar heeft tijdvakken van een jaar startend op dag 1 van maand 1;

            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
                het aantal Numeriek (geheel getal) voor elke penningjaar;
            """ + RULE);
        assertEquals(List.of("RGS-008 2 het penningjaar 1"), m.errors().stream().map(d -> d.code() + " " + d.line() + " "
                + d.params().get("name") + " " + d.params().get("earlierLine")).toList(), m.diagnostics::toString);
        assertEquals("timelines", m.errors().get(0).params().get("namespace"));
    }

    private static final String TWO_GROUPS = """
        Vocabulaire handboek

        Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);
            de premie Numeriek (geheel getal);

        Regelgroep %s
        Regel waarde
            geldig altijd
                De waarde van een Vondst moet berekend worden als 1.

        Regelgroep %s
        Regel premie
            geldig altijd
                De premie van een Vondst moet berekend worden als 2.
        """;

    /** IR-SPEC WF-001: the rule groups are a namespace of their own. Measured on the frontend before this check: two
     *  groups of one name reached the wire without a diagnostic, and a scope 'toetst de regelgroep g' took the first;
     *  with designations the two groups shared one key, which yielded a WF-117 and a WF-118 refusal and no WF-001. */
    @Test void a_rule_group_declared_twice_is_refused() {
        for (String[] heads : List.of(new String[] {"g", "g"}, new String[] {"g", "G"},
                new String[] {"g (handboek: eerste groep)", "g (handboek: tweede groep)"},
                new String[] {"g (handboek: groep)", "G (handboek: groep)"})) {
            String second = heads[1].split(" ")[0];
            RegelSpraakModel m = Resolver.resolve(TWO_GROUPS.formatted(heads[0], heads[1]));
            assertEquals(List.of("RGS-008 12 " + second + " 7"), m.errors().stream().map(RepeatedNameTest::row).toList(),
                    m.diagnostics::toString);
            assertEquals("ruleGroups", m.errors().get(0).params().get("namespace"));
        }
    }

    /** A rule group and a rule are two namespaces (WF-118 scopes them each on its own), so they may share a name. */
    @Test void a_rule_group_and_a_rule_may_share_a_name() {
        assertEquals(List.of(), errors("""
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);

            Regelgroep waarde
            Regel waarde
                geldig altijd
                    De waarde van een Vondst moet berekend worden als 1.
            """));
    }
}
