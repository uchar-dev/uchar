package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A by-name reference in the declarations resolves case-insensitively: a domain (WF-003) and the target of a unit's
 * conversion (WF-009, the name identity of IR-SPEC ch. 2.1). Measured before: a domain written in other letters was
 * refused as undeclared, and a conversion "= 12 MND" to the declared "mnd" left the year a base of its own, so an
 * assignment from years to months was refused as incompatible.
 */
class CaseInsensitiveReferenceTest {

    @Test void a_domain_in_other_letters_resolves() {
        RegelSpraakModel m = Resolver.resolve("""
            Domein Vondstklasse is van het type Enumeratie
                'drijfgoed'
                'wrakgoed'

            Objecttype de Vondst (mv: Vondsten)
                de klasse VONDSTKLASSE;
                de bestemming Vondstklasse;

            Regel bestemming
                geldig altijd
                    De bestemming van een Vondst moet gesteld worden op zijn klasse.
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        assertEquals(new Datatype.Enumeration("Vondstklasse", java.util.List.of("drijfgoed", "wrakgoed")),
                m.objectTypes.get("Vondst").attributes.get("klasse"));
    }

    @Test void a_conversion_target_in_other_letters_resolves() {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Tijdmaat
                de maand mnd
                het jaar jr = 12 MND

            Objecttype de Vondst (mv: Vondsten)
                de looptijd Numeriek (geheel getal) met eenheid jr;
                de duur Numeriek (geheel getal) met eenheid mnd;

            Regel duur
                geldig altijd
                    De duur van een Vondst moet berekend worden als zijn looptijd.
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        var year = m.unitSystems.get("Tijdmaat").units.get("jr");
        assertEquals("mnd", year.canonical());
        assertEquals(new dev.uchar.regelspraak.model.Unit.Fraction(java.math.BigInteger.valueOf(12),
                java.math.BigInteger.ONE), year.factorToCanonical());
    }
}
