package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two named forms with one name (IR-SPEC WF-020): the rules, the other forms of ch. 3 and the defaults groups share one
 * namespace, compared without case and with whitespace collapsed. The frontend refuses a second declaration whose form
 * reaches the wire under a name another form already carries, in the verifier's words, with both source lines; the
 * name of an 'Uitzondering op' or 'Afwijking van' block does not reach the wire (its default joins the group of its
 * base), so it is not this refusal's to judge.
 *
 * <p><b>Red before the refusal</b> (measured 2026-10-02 on the frontend of {@code da03d480}): two rules named
 * "actief" passed {@code check} with {@code diagnostics: []}, and the verifier refused the document ({@code WF-020 FAIL
 * /rules/rules/1: duplicate name 'actief' in rules}) while ref and clingo played it; so did the name in another case, a
 * rule beside a consistency rule ({@code name 'gewicht' collides with 'gewicht' in rules}), a member of a Regelgroep
 * beside a rule and a decision-table row beside a rule. Two blocks of one name with two versions each ended in the
 * emitter's exception on the version suffixes.
 */
class DuplicateFormNameTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de startdag Datum in dagen;
            het gewicht Numeriek (geheel getal);
            de premie Numeriek (geheel getal);
            is actief kenmerk (bijvoeglijk);
            is ervaren kenmerk (bijvoeglijk);

        """;

    /** The line of the {@code n}-th declaration head (1-based) in {@code source}: a line that opens with "Regel " or
     *  "Beslistabel ", after indentation. */
    private static int head(String source, int n) {
        String[] regels = source.split("\n", -1);
        int seen = 0;
        for (int i = 0; i < regels.length; i++) {
            String r = regels[i].stripLeading();
            if ((r.startsWith("Regel ") || r.startsWith("Beslistabel ")) && ++seen == n) return i + 1;
        }
        throw new AssertionError("geen kop " + n + " in de bron");
    }

    private static List<Diagnostic> nameErrors(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.message().contains("vormnamen zijn uniek (WF-020)")).toList();
    }

    /** One ERROR on the line of the second head, naming the line of the first. */
    private static void refused(String regels, int first, int second, String sentence) {
        String source = MODEL + regels;
        RegelSpraakModel m = Resolver.resolve(source);
        List<Diagnostic> ds = nameErrors(m);
        assertEquals(1, ds.size(), () -> "verwacht één naamweigering, kreeg: " + m.diagnostics);
        assertEquals(Severity.ERROR, ds.get(0).severity());
        assertEquals(head(source, second), ds.get(0).line(), ds.get(0)::message);
        assertEquals(sentence.replace("<n>", String.valueOf(head(source, first))), ds.get(0).message());
    }

    @Test void two_lines_with_a_name() {
        refused("""
            Regel actief
                geldig altijd
                    Een Jutter is actief
                    indien zijn startdag gevuld is.

            Regel actief
                geldig altijd
                    Het gewicht van een Jutter moet gesteld worden op 3.
            """, 1, 2, "regel 'actief' is al gedeclareerd op regel <n>; vormnamen zijn uniek (WF-020): duplicate name "
                + "'actief' in rules");
    }

    @Test void the_name_in_another_spelling() {
        refused("""
            Regel actief
                geldig altijd
                    Een Jutter is actief
                    indien zijn startdag gevuld is.

            Regel Actief
                geldig altijd
                    Het gewicht van een Jutter moet gesteld worden op 3.
            """, 1, 2, "regel 'Actief' is al gedeclareerd op regel <n>; vormnamen zijn uniek (WF-020): duplicate name "
                + "'actief' in rules");
    }

    @Test void a_rule_and_a_consistency_rule() {
        refused("""
            Regel gewicht
                geldig altijd
                    Het gewicht van een Jutter moet gesteld worden op 3.

            Regel gewicht
                geldig altijd
                    De premie van een Jutter moet groter zijn dan 0.
            """, 1, 2, "regel 'gewicht' is al gedeclareerd op regel <n>; vormnamen zijn uniek (WF-020): name "
                + "'gewicht' collides with 'gewicht' in rules (unique across all named forms)");
    }

    @Test void a_member_of_a_rule_group_and_a_rule() {
        refused("""
            Regelgroep beoordeling
                Regel actief
                    geldig altijd
                        Een Jutter is actief
                        indien zijn startdag gevuld is.

            Regel actief
                geldig altijd
                    Het gewicht van een Jutter moet gesteld worden op 3.
            """, 1, 2, "regel 'actief' is al gedeclareerd op regel <n>; vormnamen zijn uniek (WF-020): duplicate name "
                + "'actief' in rules");
    }

    @Test void a_row_of_a_decision_table_and_a_rule() {
        refused("""
            Beslistabel premie
                geldig altijd
            | | indien zijn gewicht groter is dan | de premie van een Jutter moet gesteld worden op |
            |---|---|---|
            | 1 | 2 | 10 |

            Regel premie 1
                geldig altijd
                    Het gewicht van een Jutter moet gesteld worden op 3.
            """, 1, 2, "regel 'premie 1' is al gedeclareerd op regel <n>, als rij van beslistabel 'premie'; vormnamen "
                + "zijn uniek (WF-020): duplicate name 'premie 1' in rules");
    }

    @Test void two_blocks_with_a_version() {
        refused("""
            Regel gewicht
                geldig vanaf 01-01-2026
                    Het gewicht van een Jutter moet gesteld worden op 3.

            Regel gewicht
                geldig t/m 31-12-2025
                    Het gewicht van een Jutter moet gesteld worden op 2.
            """, 1, 2, "regel 'gewicht' is al gedeclareerd op regel <n>; vormnamen zijn uniek (WF-020): duplicate name "
                + "'gewicht' in rules");
    }

    @Test void two_blocks_with_two_versions() {
        // before: the emitter's exception, "de versieachtervoegsels zijn [1, 1, 2, 2], niet precies 1 tot en met 4"
        refused("""
            Regel gewicht
                geldig vanaf 01-01-2026
                    Het gewicht van een Jutter moet gesteld worden op 3.
                geldig t/m 31-12-2025
                    Het gewicht van een Jutter moet gesteld worden op 2.

            Regel gewicht
                geldig vanaf 01-01-2027
                    De premie van een Jutter moet gesteld worden op 3.
                geldig t/m 31-12-2026
                    De premie van een Jutter moet gesteld worden op 2.
            """, 1, 2, "regel 'gewicht' is al gedeclareerd op regel <n>; vormnamen zijn uniek (WF-020): duplicate name "
                + "'gewicht' in rules");
    }

    @Test void a_rule_with_two_versions_stays_clean() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel gewicht
                geldig vanaf 01-01-2026
                    Het gewicht van een Jutter moet gesteld worden op 3.
                geldig t/m 31-12-2025
                    Het gewicht van een Jutter moet gesteld worden op 2.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> m.diagnostics.toString());
    }

    @Test void the_name_of_a_deviation_does_not_reach_the_wire() {
        // the default of 'zwaar' joins the group 'gewicht'; the lift refuses the two blocks of one name in its own words
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel gewicht
                geldig altijd
                    Het gewicht van een Jutter moet gesteld worden op 3.

            Afwijking van gewicht: zwaar
                geldig altijd
                    Het gewicht van een Jutter moet gesteld worden op 5
                    indien hij actief is.

            Regel zwaar
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 7.
            """);
        assertTrue(nameErrors(m).isEmpty(), () -> m.diagnostics.toString());
    }

    @Test void two_names_stay_clean() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel actief
                geldig altijd
                    Een Jutter is actief
                    indien zijn startdag gevuld is.

            Regel gewicht
                geldig altijd
                    Het gewicht van een Jutter moet gesteld worden op 3.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> m.diagnostics.toString());
    }
}
