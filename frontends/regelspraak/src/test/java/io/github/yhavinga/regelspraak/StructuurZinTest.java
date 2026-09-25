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

}
