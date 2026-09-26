package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A {@code Regelgroep}'s members on the wire are RULE names (IR-SPEC ch. 3.9, WF-033: every member resolves to a
 * rule form). A decision table is no rule form — it desugars to one rule per row — so a group that holds one lists
 * the rules of its rows. Measured on the frontend before this fix (a8ab1639): the table's own name went out as
 * the member, silently, and the verifier refused every such document ({@code WF-033 FAIL /regelGroepen/0: group
 * member 'Bergingspremie' does not resolve to a rule form}), with or without anything else in it. That path is the
 * pre-M1 spelling of the measurement; since M1 (2026-09-23) the verifier names the position /rules/regelGroepen/0.
 */
class RegelgroepLedenTest {

    static final String GROEP = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);
            de bergingspremie Numeriek (niet-negatief geheel getal);
            de bergingsbonus Numeriek (niet-negatief geheel getal);

        Regelgroep berging
        Regel bergingsbonus
            geldig altijd
                De bergingsbonus van een Jutter moet berekend worden als zijn aantal geborgen wrakken maal 2.

        Beslistabel Bergingspremie
            geldig altijd
        | | de bergingspremie van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
        |---|---|---|
        | 1 | 10 | 5 |
        | 2 | 20 | 10 |
        """;

    @Test void een_gegroepeerde_beslistabel_noemt_haar_rijen_als_leden() {
        RegelSpraakModel m = Resolver.resolve(GROEP);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("bergingsbonus", "Bergingspremie 1", "Bergingspremie 2"), m.regelGroepen.get(0).leden(),
                "de leden zijn regelnamen, in geschreven volgorde; de tabel staat voor haar rijen");
        // every member names a rule the model carries, which is what WF-033 asks of the wire
        for (String lid : m.regelGroepen.get(0).leden())
            assertTrue(m.resolvedRegels.stream().anyMatch(r -> r.naam().equals(lid)), () -> "lid '" + lid + "' is geen regel");
    }

    /** WF-033: an object creation in a recursive group mints new instances each generation, so it carries a guard,
     *  and the verifier refuses an unguarded one. Measured on the frontend before this: a WARNING, the document
     *  written, and {@code WF-033 FAIL /rules/ruleGroups/0: unguarded object creation 'werf de opvolger' in a
     *  recursive group}. */
    @Test void een_onbewaakte_objectcreatie_in_een_recursieve_groep_wordt_geweigerd() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Wachter (mv: Wachters) (bezield)
                de rang Numeriek (geheel getal);

            Feittype opvolging
                de voorganger Wachter
                de opvolger (mv: opvolgers) Wachter
            Eén voorganger heeft één opvolger

            Regelgroep estafette is recursief
            Regel werf de opvolger
                geldig altijd
                    Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1.
            """);
        assertEquals(List.of("ERROR recursieve regelgroep 'estafette': unguarded object creation 'werf de opvolger' in a "
                        + "recursive group"),
                m.diagnostics.stream().map(d -> d.severity() + " " + d.message()).toList());
    }

    /** A creation with two versions is one model entry per version, named {@code <naam> #<i>}, and each version is a
     *  member of its group. Measured on the frontend before this: every such recursive group was refused as having no
     *  object creation, also with both versions guarded, a document the verifier accepts. */
    @Test void een_objectcreatie_met_twee_versies_is_met_elke_versie_lid() {
        String groep = """
            Objecttype de Wachter (mv: Wachters) (bezield)
                de rang Numeriek (geheel getal);

            Feittype opvolging
                de voorganger Wachter
                de opvolger (mv: opvolgers) Wachter
            Eén voorganger heeft meerdere opvolgers

            Regelgroep estafette is recursief
            Regel werf de opvolger
                geldig vanaf 2025 t/m 2025
                    Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1%s.
                geldig vanaf 2026
                    Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 2%s.
            """;
        String bewaakt = "\n        indien de rang van de Wachter kleiner is dan 5";
        java.util.function.BiFunction<String, String, List<String>> meldingen = (v1, v2) ->
                Resolver.resolve(groep.formatted(v1, v2)).diagnostics.stream()
                        .map(d -> d.severity() + " " + d.message()).toList();
        String onbewaakt = "ERROR recursieve regelgroep 'estafette': unguarded object creation 'werf de opvolger #%d' in a "
                + "recursive group";
        assertEquals(List.of(), meldingen.apply(bewaakt, bewaakt));
        assertEquals(List.of(onbewaakt.formatted(2)), meldingen.apply(bewaakt, ""));
        assertEquals(List.of(onbewaakt.formatted(1), onbewaakt.formatted(2)), meldingen.apply("", ""));
    }
}
