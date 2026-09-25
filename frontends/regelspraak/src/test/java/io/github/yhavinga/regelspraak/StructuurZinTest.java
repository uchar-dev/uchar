package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The structural refusals the verifier shares, in its words: where the resolver refuses a document the verifier
 * refuses too — a rule form, a group, a declaration, a count's shape — the message after the Dutch context prefix is
 * the verifier's sentence, with the names the verifier would print for the document the refusal kept off disk
 * (measured per sentence by emitting that document with the refusal switched off, or by building it by hand where
 * the frontend refuses before it has one). Where no reader refuses that document, the refusal keeps its Dutch words
 * (grammar/DIVERGENCES.md, "Producer refusals without a reader sentence"), and the witnesses below pin that too.
 * Measured on the frontend before this: every sentence below was Dutch.
 */
class StructuurZinTest {

    private static List<String> fouten(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        return m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList();
    }

    // ---- WF-022: a plain rule that negates a characteristic

    private static final String KENMERKEN = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is uitgesloten kenmerk (bijvoeglijk);
            is geschorst kenmerk (bijvoeglijk);
            het aantal aangemelde vondsten Numeriek (niet-negatief geheel getal);

        """;

    @Test void een_ontkenning_in_een_gewone_regel() {
        assertEquals(List.of("regel 'vlak': characteristic target's value must be the boolean constant true"),
                fouten(KENMERKEN + """
                    Regel vlak
                        geldig altijd
                            Een Jutter is niet uitgesloten
                            indien hij geschorst is.
                    """));
        assertEquals(List.of("regel 'Uitsluitingstabel 1' (een rij van beslistabel 'Uitsluitingstabel'): characteristic "
                        + "target's value must be the boolean constant true"),
                fouten(KENMERKEN + """
                    Beslistabel Uitsluitingstabel
                        geldig altijd
                    | | een Jutter is niet uitgesloten | indien zijn aantal aangemelde vondsten groter is dan |
                    |---|---|---|
                    | 1 | waar | 5 |
                    """));
    }

    // ---- WF-050: the single writer of a defaults target

    private static final String PREMIE = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal);
            is noodweer verklaard kenmerk (bijvoeglijk);
            is kustwacht kenmerk (bijvoeglijk);

        Regel basispremie
            geldig altijd
                De premie van een Jutter moet gesteld worden op 10.

        Uitzondering op basispremie: noodpremie
            geldig altijd
                De premie van een Jutter moet gesteld worden op 20
                indien hij noodweer verklaard is.

        """;

    @Test void een_gewone_schrijver_naast_een_defaults_groep() {
        assertEquals(List.of("Uitzondering op 'basispremie' (defaults-groep 'basispremie', doel ook geschreven door regel "
                        + "'andere premie'): a rule also writes defaults target 'Jutter.premie' — an unordered plain "
                        + "writer would silently defeat the group's decisions"),
                fouten(PREMIE + """
                    Regel andere premie
                        geldig altijd
                            De premie van een Jutter moet gesteld worden op 30
                            indien hij kustwacht is.
                    """));
    }

    @Test void twee_geordende_defaults_groepen_op_een_doel() {
        assertEquals(List.of("Uitzondering op 'tweede basis' (defaults-groepen 'basispremie' en 'tweede basis'): multiple "
                        + "defaults groups on target 'Jutter.premie' without the declared-unordered marking — priority "
                        + "is explicit (defaults) or explicitly absent ('unordered'), never implicit (ch. 7.2)"),
                fouten(PREMIE + """
                    Regel tweede basis
                        geldig altijd
                            De premie van een Jutter moet gesteld worden op 5.

                    Uitzondering op tweede basis: kustpremie
                        geldig altijd
                            De premie van een Jutter moet gesteld worden op 30
                            indien hij kustwacht is.
                    """));
    }

    // ---- WF-033: a recursive group without a creation member

    @Test void een_recursieve_groep_zonder_creatie() {
        assertEquals(List.of("recursieve regelgroep 'estafette': recursive group without a creation member"),
                fouten("""
                    Objecttype de Wachter (mv: Wachters) (bezield)
                        de rang Numeriek (geheel getal);

                    Regelgroep estafette is recursief
                    Regel verhoog de rang
                        geldig altijd
                            De rang van een Wachter moet gesteld worden op 1.
                    """));
    }

    @Test void een_recursieve_groep_met_alleen_een_feitcreatie_houdt_de_eigen_zin() {
        // the verifier admits a relation creation as the recursion's carrier (ch. 5.5); §9.10 asks an object creation
        assertEquals(List.of("recursieve regelgroep 'beseining' zonder objectcreatieregel — de 'is recursief'-markering "
                        + "kan de keten niet laten voortschrijden (§9.10, gedocumenteerde extensie)"),
                fouten("""
                    Objecttype het Kustvak (mv: Kustvakken)
                        het vakseinaantal Numeriek (geheel getal);

                    Objecttype de Post (mv: Posten)
                        de hoogte Numeriek (geheel getal);

                    Objecttype het Sein (mv: Seinen)
                        is gehesen kenmerk (bijvoeglijk);

                    Feittype kustligging
                        het kustvak Kustvak
                        de post (mv: posten) Post
                    Eén kustvak telt meerdere posten

                    Feittype seinvoering
                        de post Post
                        het sein (mv: seinen) Sein
                    Eén post telt meerdere seinen

                    Feittype vakbeseining
                        het seinvak Kustvak
                        het vaksein (mv: vakseinen) Sein
                    Eén seinvak telt meerdere vakseinen

                    Regelgroep beseining is recursief
                    Regel vakbeseining door het kustvak
                        geldig altijd
                            Een vaksein van een Kustvak is een sein van de post van het kustvak
                            indien het sein gehesen is.
                    """));
    }

    // ---- WF-021: overlapping versions, named as the verifier names them, in the array their kind lands in

    @Test void overlappende_versies_van_een_regel() {
        assertEquals(List.of("regel 'premie': validity windows of versions 'premie #1' and 'premie #2' overlap (rules)"),
                fouten("""
                    Objecttype de Jutter (mv: Jutters) (bezield)
                        de premie Numeriek (geheel getal);

                    Regel premie
                        geldig vanaf 2024 t/m 2025
                            De premie van een Jutter moet gesteld worden op 10.
                        geldig vanaf 2025
                            De premie van een Jutter moet gesteld worden op 20.
                    """));
    }

    @Test void overlappende_versies_van_een_objectcreatie() {
        assertEquals(List.of("regel 'werf de opvolger': validity windows of versions 'werf de opvolger #1' and 'werf de "
                        + "opvolger #2' overlap (objectCreations)"),
                fouten("""
                    Objecttype de Wachter (mv: Wachters) (bezield)
                        de rang Numeriek (geheel getal);

                    Feittype opvolging
                        de voorganger Wachter
                        de opvolger (mv: opvolgers) Wachter
                    Eén voorganger heeft meerdere opvolgers

                    Regel werf de opvolger
                        geldig vanaf 2025
                            Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 1
                            indien de rang van de Wachter kleiner is dan 5.
                        geldig vanaf 2026
                            Een Wachter heeft een opvolger met rang gelijk aan de rang van de Wachter plus 2
                            indien de rang van de Wachter kleiner is dan 5.
                    """));
    }

    @Test void overlappende_versies_van_een_dagsoortdefinitie_houden_de_eigen_zin() {
        // WF-021 compares the versions of the seven rule forms; a day-kind definition is none of them
        assertEquals(List.of("geldigheidsperioden van regelversies overlappen (regel 'Dagsoortdefinitie springtijdag')"),
                fouten("""
                    Dagsoort de springtijdag (mv: springtijdagen);

                    Regel Dagsoortdefinitie springtijdag
                        geldig altijd
                            Een dag is een springtijdag
                            indien de dag uit de dag gelijk is aan 1.
                        geldig altijd
                            Een dag is een springtijdag
                            indien de dag uit de dag gelijk is aan 15.
                    """));
    }

    // ---- WF-020: two decision-table rows with one rule name

    @Test void twee_tabelrijen_met_een_regelnaam() {
        assertEquals(List.of("beslistabel 'Premietabel': duplicate name 'Premietabel 1' in rules"),
                fouten("""
                    Objecttype de Jutter (mv: Jutters) (bezield)
                        de premie Numeriek (geheel getal);
                        het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

                    Beslistabel Premietabel
                        geldig altijd
                    | | de premie van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
                    |---|---|---|
                    | 1 | 10 | 5 |
                    | 1 | 20 | 10 |
                    """));
    }

    // ---- WF-033: an exception block whose base is a group member

    private static final String BERGING = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is bergingsbevoegd kenmerk (bijvoeglijk);
            is noodweer verklaard kenmerk (bijvoeglijk);
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

        Regelgroep berging
        Regel bergingsbevoegd bij quotum
            geldig altijd
                Een Jutter is bergingsbevoegd
                indien zijn aantal geborgen wrakken groter of gelijk is aan 20.

        """;

    private static final String NOODWEER = """
        Uitzondering op bergingsbevoegd bij quotum: bergingsbevoegd bij noodweer
            geldig altijd
                Een Jutter is bergingsbevoegd
                indien hij noodweer verklaard is.
        """;

    @Test void een_uitzonderingsblok_op_een_groepslid() {
        assertEquals(List.of("Uitzondering op 'bergingsbevoegd bij quotum' (zijn basisregel is lid van regelgroep "
                        + "'berging'): group member 'bergingsbevoegd bij quotum' does not resolve to a rule form"),
                fouten(BERGING + "Parameter de noodweerdrempel : Numeriek (geheel getal)\n\n" + NOODWEER));
        assertEquals(List.of("Uitzondering op 'bergingsbevoegd bij quotum' (zijn basisregel is lid van regelgroep "
                        + "'berging', en het blok staat direct na het laatste lid van regelgroep 'berging'): group "
                        + "member 'bergingsbevoegd bij quotum' does not resolve to a rule form"),
                fouten(BERGING + NOODWEER));
    }

}
