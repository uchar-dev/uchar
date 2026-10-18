package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WF-050's single writer, producer side: a target with a defaults group has no other writer — no plain rule
 * beside the group, and no second group unless every group on the target is declared unordered. Measured on
 * the frontend before this fence (402dc130), each of the five placements below reached disk with
 * {@code diagnostics: []} and the verifier refused it under WF-050: an 'Uitzondering op' with another plain
 * rule on the base's target; an 'Uitzondering op' on one row of a decision table (outside a group — the other
 * rows go on writing); two ordered families on one target; an ordered family beside an unordered block; an
 * unordered block beside a plain rule. Two unordered blocks on one target are admitted (every group unordered)
 * and stay clean.
 */
class OnlyWriterTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de premie Numeriek (geheel getal);
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);
            is noodweer verklaard kenmerk (bijvoeglijk);
            is kustwacht kenmerk (bijvoeglijk);

        """;

    private static final String BASE = """
        Regel basispremie
            geldig altijd
                De premie van een Jutter moet gesteld worden op 10.

        """;

    private static final String NOOD = """
        Uitzondering op basispremie: noodpremie
            geldig altijd
                De premie van een Jutter moet gesteld worden op 20
                indien hij noodweer verklaard is.

        """;

    private static Diagnostic onlyError(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        List<Diagnostic> faults = m.errors();
        assertEquals(1, faults.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        // one of WF-050's two clauses, in the verifier's sentence
        assertTrue(faults.get(0).message().contains("a rule also writes defaults target '")
                || faults.get(0).message().contains("multiple defaults groups on target '"), faults.get(0).message());
        return faults.get(0);
    }

    @Test void another_ordinary_rule_on_the_target_is_refused() {
        Diagnostic d = onlyError(MODEL + BASE + """
            Regel andere premie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.

            """ + NOOD);
        assertTrue(d.message().contains("'andere premie'") && d.message().contains("'basispremie'")
                && d.message().contains("Jutter.premie"), d.message());
    }

    @Test void an_exception_on_a_table_row_is_refused_because_the_other_rows_write_through() {
        Diagnostic d = onlyError(MODEL + """
            Beslistabel Premietabel
                geldig altijd
            | | de premie van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | 1 | 10 | 5 |
            | 2 | 20 | 10 |

            Uitzondering op Premietabel 1: noodpremie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 50
                    indien hij noodweer verklaard is.
            """);
        assertTrue(d.message().contains("'Premietabel 2' (een rij van beslistabel 'Premietabel')"), d.message());
    }

    @Test void two_ordered_families_on_a_target_are_refused() {
        Diagnostic d = onlyError(MODEL + BASE + NOOD + """
            Regel tweede basis
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 5.

            Uitzondering op tweede basis: kustpremie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.
            """);
        assertTrue(d.message().contains("'basispremie'") && d.message().contains("'tweede basis'")
                && d.message().contains("without the declared-unordered marking"), d.message());
    }

    @Test void a_family_beside_an_unordered_block_is_refused() {
        Diagnostic d = onlyError(MODEL + BASE + NOOD + """
            Uitzonderingen ongeordend:
            Regel kustpremie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.
            """);
        assertTrue(d.message().contains("'basispremie'") && d.message().contains("'kustpremie'"), d.message());
    }

    @Test void an_unordered_block_beside_an_ordinary_rule_is_refused() {
        Diagnostic d = onlyError(MODEL + BASE + """
            Uitzonderingen ongeordend:
            Regel kustpremie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.

            Regel noodpremie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 20
                    indien hij noodweer verklaard is.
            """);
        assertTrue(d.message().contains("'basispremie'"), d.message());
    }

    @Test void two_unordered_blocks_on_a_target_stay_clean() {
        String src = MODEL + """
            Uitzonderingen ongeordend:
            Regel kustpremie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 30
                    indien hij kustwacht is.

            Objecttype de Strandvoogd (mv: Strandvoogden)
                de rang Numeriek (geheel getal);

            Uitzonderingen ongeordend:
            Regel noodpremie
                geldig altijd
                    De premie van een Jutter moet gesteld worden op 20
                    indien hij noodweer verklaard is.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    @Test void a_family_with_a_rule_on_another_target_stays_clean() {
        String src = MODEL + BASE + NOOD + """
            Regel wrakken
                geldig altijd
                    Het aantal geborgen wrakken van een Jutter moet gesteld worden op 0.
            """;
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }
}
