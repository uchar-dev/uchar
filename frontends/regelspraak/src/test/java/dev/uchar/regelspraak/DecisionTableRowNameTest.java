package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A decision-table row becomes a rule named after the table and the row's number, the first cell ("Bonustabel
 * 1"). Rows without a number, or with the same number, therefore get the SAME rule name. Measured on the frontend
 * before this fix (e67fc386): such a table reached disk with {@code diagnostics: []} and two rules of one name,
 * which the verifier refused under WF-020 (duplicate name) and WF-021 (overlapping versions). It is an ERROR now,
 * naming the duplicate, and a group holding the table lists each name once.
 */
class DecisionTableRowNameTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de bonus Numeriek (geheel getal);
            het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

        """;

    private static String table(String number1, String number2) {
        return """
            Beslistabel Bonustabel
                geldig altijd
            | | de bonus van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | %s | 10 | 5 |
            | %s | 20 | 10 |
            """.formatted(number1, number2);
    }

    private static Diagnostic onlyError(RegelSpraakModel m) {
        List<Diagnostic> fouten = m.errors();
        assertEquals(1, fouten.size(), () -> "verwachtte precies één fout, kreeg: " + m.diagnostics);
        return fouten.get(0);
    }

    @Test void rows_without_number_are_refused_by_name() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + table(" ", " ")));
        assertTrue(d.message().contains("duplicate name 'Bonustabel' in rules"), d.message());
    }

    @Test void rows_with_the_same_number_are_refused_by_name() {
        Diagnostic d = onlyError(Resolver.resolve(MODEL + table("1", "1")));
        assertTrue(d.message().contains("duplicate name 'Bonustabel 1' in rules"), d.message());
    }

    @Test void the_group_names_a_duplicate_row_name_one_time() {
        RegelSpraakModel m = Resolver.resolve(MODEL + "Regelgroep bonus\n" + table("1", "1"));
        onlyError(m);
        assertEquals(List.of("Bonustabel 1"), m.regelGroepen.get(0).leden());
    }

    @Test void numbered_rows_stay_clean() {
        RegelSpraakModel m = Resolver.resolve(MODEL + table("1", "2"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }
}
