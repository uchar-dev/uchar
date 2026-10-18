package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.ResolvedScenario.Instantie;
import dev.uchar.regelspraak.ir.ResolvedScenario.RolWaarde;
import dev.uchar.regelspraak.ir.ResolvedScenario.Situatie;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A role assignment of a situation carries the fact type the resolver resolved it to. One role name can stand in
 * several fact types — here {@code vergunning} in three — and which one an assignment means depends on the type of
 * the instance it is written on; a consumer that looked the fact type up by the role name alone would pick one for
 * all three. The resolver has the fact type in hand when it finds the role, so it hands that one on.
 */
class ScenarioRoleFactTypeTest {

    private static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de naam Tekst;

        Objecttype de Vergunning (mv: Vergunningen)
            het nummer Numeriek (geheel getal);

        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (getal);

        Objecttype de Strook (mv: Stroken)
            de lengte Numeriek (getal);

        Feittype vergunninghouderschap
            de houder Jutter
            de vergunning (mv: vergunningen) Vergunning
        Eén houder heeft meerdere vergunningen

        Feittype vergunde vondst
            de vergunning Vergunning
            de vondst (mv: vondsten) Vondst
        Eén vergunning heeft meerdere vondsten

        Feittype vergunde strook
            de vergunning Vergunning
            de strook Strook
        Eén vergunning heeft één strook

        Wederkerig feittype strandmaatschap
            de strandmaat (mv: strandmaten) Jutter
        Eén strandmaat heeft één strandmaat

        Regel naam van een jutter
            geldig altijd
                De naam van een Jutter moet gesteld worden op "jutter".
        """;

    private static final String SCENARIO = """
        Situatie vergund:
        een Jutter J
        - de vergunningen zijn G
        - de strandmaat is K
        een Jutter K
        een Vergunning G
        - de houder is J
        - de vondsten zijn V, W
        - de strook is S
        een Vondst V
        - de vergunning is G
        een Vondst W
        een Strook S
        - de vergunning is G
        Scenario vergund
        gebaseerd op de situatie vergund
        Met als verwacht resultaat:
        - de naam van J is "jutter"
        """;

    private static Situatie situatie() {
        ScenarioBundel b = ScenarioResolver.resolve(MODEL + SCENARIO);
        assertTrue(b.isOk(), () -> "het scenario hoort schoon te resolven: " + b.model().errors());
        return b.scenarios().get(0).situatie();
    }

    private static RolWaarde rol(Situatie sit, String label, String sleutel) {
        Instantie i = sit.instantie(label);
        assertNotNull(i, label);
        return i.rollen().stream().filter(r -> r.sleutel().equals(sleutel)).findFirst()
                .orElseGet(() -> fail("instantie " + label + " heeft geen rol '" + sleutel + "': " + i.rollen()));
    }

    @Test void the_role_name_vergunning_belongs_per_instance_to_another_fact_type() {
        Situatie sit = situatie();
        assertEquals("vergunninghouderschap", rol(sit, "J", "vergunningen").feittype().naam);
        assertEquals("vergunde vondst", rol(sit, "V", "vergunning").feittype().naam);
        assertEquals("vergunde strook", rol(sit, "S", "vergunning").feittype().naam);
    }

    @Test void the_other_side_carries_the_same_fact_type() {
        Situatie sit = situatie();
        assertEquals("vergunninghouderschap", rol(sit, "G", "houder").feittype().naam);
        assertEquals("vergunde vondst", rol(sit, "G", "vondsten").feittype().naam);
        assertEquals("vergunde strook", rol(sit, "G", "strook").feittype().naam);
    }

    @Test void a_reciprocal_fact_type() {
        assertEquals("strandmaatschap", rol(situatie(), "J", "strandmaat").feittype().naam);
    }

    @Test void the_fact_type_is_that_of_the_model() {
        ScenarioBundel b = ScenarioResolver.resolve(MODEL + SCENARIO);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        Situatie sit = b.scenarios().get(0).situatie();
        for (String[] pair : List.of(new String[]{"J", "vergunningen"}, new String[]{"V", "vergunning"},
                new String[]{"S", "vergunning"}, new String[]{"G", "houder"}, new String[]{"J", "strandmaat"})) {
            RolWaarde r = rol(sit, pair[0], pair[1]);
            assertSame(b.model().feittypen.get(r.feittype().naam), r.feittype(), pair[0] + " " + pair[1]);
        }
    }
}
