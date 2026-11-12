package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The cardinality line of a fact type (§13.3.9.1) marks a role 'meerdere' through the part that begins with its name.
 * Measured before this check: a 'meerdere' part that named no role ("Eén schuit heeft meerdere ladingen" over the roles
 * vervoerder and vracht) was dropped without a word, both roles went to the wire single, the verifier passed the
 * document, and a case with two loads on one barge failed in ref ("to-one role carries 2 labels").
 */
class CardinalityLineTest {

    private static String factType(String line) {
        return """
            Objecttype de Schuit (mv: Schuiten)
                het totaal Numeriek (geheel getal);
            Objecttype de Lading (mv: Ladingen)
                het gewicht Numeriek (geheel getal);

            Feittype belading
                de vervoerder Schuit
                de vracht (mv: vrachten) Lading
            %s

            Regel gewicht
                geldig altijd
                    Het gewicht van een Lading moet berekend worden als 1.
            """.formatted(line);
    }

    @Test void a_many_part_that_names_no_role_is_refused() {
        RegelSpraakModel m = Resolver.resolve(factType("Eén schuit heeft meerdere ladingen"));
        assertEquals(List.of("RGS-432 9"), m.errors().stream().map(d -> d.code() + " " + d.line()).toList(),
                m.diagnostics::toString);
        assertEquals("feittype 'belading': het deel 'ladingen' van de kardinaliteitsregel noemt geen van de rollen "
                + "'vervoerder', 'vracht', dus 'meerdere' hoort bij geen rol en de relatie zou als één-op-één gelden; "
                + "een deel begint met de naam van zijn rol (§13.3.9.1)", m.errors().get(0).message());
    }

    /** The part that names its role marks it; the other part binds the remaining role by elimination. */
    @Test void a_many_part_that_names_its_role_marks_it() {
        RegelSpraakModel m = Resolver.resolve(factType("Eén schuit heeft meerdere vrachten"));
        assertTrue(m.errors().isEmpty(), m.diagnostics::toString);
        var roles = m.factTypes.get("belading").roles;
        assertEquals(List.of(false, true), roles.stream().map(r -> r.many()).toList());
    }

    /** A 'één' part that names no role loses nothing: a role is single by default. */
    @Test void a_single_part_that_names_no_role_passes() {
        RegelSpraakModel m = Resolver.resolve(factType("Eén schuit heeft één lading"));
        assertTrue(m.errors().isEmpty(), m.diagnostics::toString);
    }
}
