package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A declared attribute with 'van' in its name whose words also spell a navigation that binds (grammar/DIVERGENCES.md,
 * "An undeclared attribute read"). "de premie van de jutter van de Vondst", with 'premie van de jutter' declared on
 * the Vondst and 'premie' on the Jutter its role 'jutter' reaches, reads the declaration — and did so without a word
 * before this witness. The declaration keeps winning (maintainer decision); the author now gets a WARNING that names
 * both readings, and the document is still written. Where the words spell no navigation that reaches the member, no
 * warning is raised.
 */
class DubbeleLezingTest {

    private static List<String> waarschuwingen(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.severity() == RegelSpraakModel.Severity.WARNING)
                .map(RegelSpraakModel.Diagnostic::message).toList();
    }

    private static final String JUTTERS = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal);
            de bonus van de vondst Numeriek (geheel getal);
            de score Numeriek (geheel getal);

        Objecttype de Berging (mv: Bergingen)
            de premie van de beheerder Numeriek (geheel getal);

        Objecttype de Vondst (mv: Vondsten)
            de bonus Numeriek (geheel getal);
            het gewicht Numeriek (geheel getal);
            de premie van de jutter Numeriek (geheel getal);
            de datum van keuring Datum in dagen;
            de keurdag Datum in dagen;

        Feittype JutterHeeftVondst
            de jutter Jutter
            de vondst (mv: vondsten) Vondst
        Eén jutter heeft meerdere vondsten

        Feittype BergingBevatVondst
            de berging Berging
            de geborgen vondst (mv: geborgen vondsten) Vondst
        Eén berging bevat meerdere geborgen vondsten

        Feittype BeheerderBeheertBerging
            de beheerder Jutter
            de beheerde berging (mv: beheerde bergingen) Berging
        Eén beheerder beheert meerdere beheerde bergingen

        """;

    private static String waarde(String lezing) {
        return JUTTERS + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet berekend worden als "
                + lezing + " plus 1.\n";
    }

    private static String voorwaarde(String lezing) {
        return JUTTERS + "Regel bonus\n    geldig altijd\n        De bonus van een Vondst moet gesteld worden op 1\n"
                + "        indien " + lezing + " groter is dan 2.\n";
    }

    private static String rechtvaardiging(String lezing) {
        return JUTTERS + """
            Regel bonus
                geldig altijd
                    De bonus van een Vondst moet gesteld worden op 0.

            Uitzondering op bonus: hoge bonus
                geldig altijd
                    De bonus van een Vondst moet gesteld worden op 9
            """ + "        indien " + lezing + " groter is dan 2.\n";
    }

    private static final String JUTTER_ZIN = "lezing 'de premie van de jutter van de Vondst': het gedeclareerde "
            + "attribuut 'premie van de jutter' van 'Vondst' wint; dezelfde woorden lezen ook als het attribuut 'premie' "
            + "van 'Jutter' over de rol 'jutter', en die lezing geldt hier niet";

    @Test void de_declaratie_wint_en_de_waarschuwing_noemt_beide_lezingen() {
        RegelSpraakModel m = Resolver.resolve(waarde("de premie van de jutter van de Vondst"));
        assertEquals(List.of(), m.errors());
        assertEquals(List.of(JUTTER_ZIN), waarschuwingen(m));
        Expr.Binair som = (Expr.Binair) m.resolvedRegels.get(0).waarde();
        Expr.AttribuutRef lezing = (Expr.AttribuutRef) som.links();
        assertEquals("premie van de jutter", lezing.attribuut(), "the declaration is what is read");
        assertEquals("Vondst", lezing.objecttype().naam);
    }

    @Test void twee_rollen_ver() {
        RegelSpraakModel m = Resolver.resolve(waarde("de premie van de beheerder van de berging van de Vondst"));
        assertEquals(List.of(), m.errors());
        assertEquals(List.of("lezing 'de premie van de beheerder van de berging van de Vondst': het gedeclareerde "
                + "attribuut 'premie van de beheerder' van 'Berging' wint; dezelfde woorden lezen ook als het attribuut "
                + "'premie' van 'Jutter' over de rol 'beheerder', en die lezing geldt hier niet"), waarschuwingen(m));
    }

    @Test void elke_positie() {
        for (String bron : List.of(voorwaarde("de premie van de jutter van de Vondst"),
                rechtvaardiging("de premie van de jutter van de Vondst"))) {
            RegelSpraakModel m = Resolver.resolve(bron);
            assertEquals(List.of(), m.errors());
            assertEquals(List.of(JUTTER_ZIN), waarschuwingen(m));
        }
    }

    @Test void geen_waarschuwing_waar_de_woorden_geen_navigatie_naar_het_lid_spellen() {
        // no role named: 'keuring' is no role and no type
        assertEquals(List.of(), waarschuwingen(Resolver.resolve(JUTTERS + "Regel keurdag\n    geldig altijd\n"
                + "        De keurdag van een Vondst moet gesteld worden op de datum van keuring van de Vondst.\n")));
        // the role is walked, but its filler declares no such member
        assertEquals(List.of(), waarschuwingen(Resolver.resolve(waarde("de premie van de jutter van de Vondst")
                .replace("    de premie Numeriek (geheel getal);\n", ""))));
        // the navigation itself: no declaration is read, so there is nothing to choose between
        assertEquals(List.of(), waarschuwingen(Resolver.resolve(waarde("de premie van zijn jutter"))));
        // a to-many step ends the walk: that navigation reads no single value
        assertEquals(List.of(), waarschuwingen(Resolver.resolve(JUTTERS + "Regel score\n    geldig altijd\n"
                + "        De score van een Jutter moet berekend worden als de bonus van de vondst van de Jutter plus 1.\n")));
    }
}
