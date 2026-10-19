package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.DecisionTableDesugar;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §12 beslistabel desugar (piece 1: the semantic core, no grammar change). Asserts a captured decision-table
 * desugars to the canonical one-regel-per-row source, and that the output resolves through the EXISTING pipeline.
 * The fixtures are own-authored: they exercise the §12 column model, not any external table.
 */
class DecisionTableDesugarTest {

    @Test void multi_value_equal_cell_desugars_to_an_or_list() {
        // §12: a multi-value "is gelijk aan A, B of C" cell expands to an "er aan tenminste één van de volgende
        // voorwaarden" §13.4.14 OR-list. The desugar reproduces that (the resolver lowers the OR-list, not a
        // verbatim value-series).
        List<String> headers = List.of("",
                "de regiofactor van een Klant moet gesteld worden op",
                "indien zijn regio gelijk is aan");
        List<List<String>> rows = List.of(
                List.of("1", "1", "\"Noord\", \"Oost\", \"West\", \"Zuid\" of \"Midden\""),
                List.of("2", "2", "\"Veen\", \"Zand\", \"Klei\" of \"Polder\""),
                List.of("3", "3", "\"Kust\", \"Wad\" of \"Duin\""));
        List<String> rules = DecisionTableDesugar.desugar("Regiofactor", "altijd", headers, rows);

        assertEquals(3, rules.size());
        assertEquals("Regel Regiofactor 1\n  geldig altijd\n    "
                + "de regiofactor van een Klant moet gesteld worden op 1 "
                + "indien er aan tenminste één van de volgende voorwaarden wordt voldaan:\n"
                + "      • zijn regio gelijk is aan \"Noord\"\n"
                + "      • zijn regio gelijk is aan \"Oost\"\n"
                + "      • zijn regio gelijk is aan \"West\"\n"
                + "      • zijn regio gelijk is aan \"Zuid\"\n"
                + "      • zijn regio gelijk is aan \"Midden\".\n",
                rules.get(0));
        assertEquals("Regel Regiofactor 3\n  geldig altijd\n    "
                + "de regiofactor van een Klant moet gesteld worden op 3 "
                + "indien er aan tenminste één van de volgende voorwaarden wordt voldaan:\n"
                + "      • zijn regio gelijk is aan \"Kust\"\n"
                + "      • zijn regio gelijk is aan \"Wad\"\n"
                + "      • zijn regio gelijk is aan \"Duin\".\n",
                rules.get(2));
    }

    @Test void multi_condition_column_folds_to_all_next_conditions_and_omits_nvt() {
        List<String> headers = List.of("",
                "de duurheffing van een klant moet gesteld worden op",
                "indien de duur in dagen van zijn verblijf groter is dan",
                "indien de duur in dagen van zijn verblijf kleiner of gelijk is aan");
        List<List<String>> rows = List.of(
                List.of("1", "het lage tarief", "n.v.t.", "de eerste drempel"),
                List.of("2", "het hoge tarief", "de eerste drempel", "de tweede drempel"));
        List<String> rules = DecisionTableDesugar.desugar("Duurheffing", "altijd", headers, rows);

        // row 1: the "n.v.t." condition is OMITTED -> a single (the second) condition.
        assertEquals("Regel Duurheffing 1\n  geldig altijd\n    "
                + "de duurheffing van een klant moet gesteld worden op het lage tarief "
                + "indien de duur in dagen van zijn verblijf kleiner of gelijk is aan de eerste drempel.\n",
                rules.get(0));
        // row 2: both conditions -> AND via "alle volgende voorwaarden", "indien" stripped per bullet.
        assertEquals("Regel Duurheffing 2\n  geldig altijd\n    "
                + "de duurheffing van een klant moet gesteld worden op het hoge tarief "
                + "indien hij aan alle volgende voorwaarden voldoet:\n"
                + "      • de duur in dagen van zijn verblijf groter is dan de eerste drempel\n"
                + "      • de duur in dagen van zijn verblijf kleiner of gelijk is aan de tweede drempel.\n",
                rules.get(1));
    }

    @Test void conclusion_column_after_condition_column_is_classified_by_header_not_position() {
        // §12: conclusie-kolommen may sit AFTER conditie-kolommen — detected by the header text, not the column index.
        List<String> headers = List.of("",
                "indien zijn leeftijd kleiner is dan",
                "de korting van een Klant moet gesteld worden op");
        List<List<String>> rows = List.of(List.of("1", "18", "10"));
        List<String> rules = DecisionTableDesugar.desugar("Korting", "altijd", headers, rows);
        assertEquals("Regel Korting 1\n  geldig altijd\n    "
                + "de korting van een Klant moet gesteld worden op 10 indien zijn leeftijd kleiner is dan 18.\n",
                rules.get(0));
    }

    @Test void characteristic_condition_true_is_the_check_itself_not_an_appended_value() {
        // §12: a "waar" cell in a kenmerk CONDITIE-kolom means the check holds — the predicate kop IS the
        // condition; appending "waar" would mis-parse and silently drop sibling AND conditions (a real bug, fixed).
        List<String> headers = List.of("",
                "de belasting van een Klant moet gesteld worden op",
                "indien hij een recht op duurzaamheidskorting heeft",
                "indien zijn duur kleiner of gelijk is aan");
        List<List<String>> rows = List.of(List.of("1", "10", "waar", "60"));
        List<String> rules = DecisionTableDesugar.desugar("Belasting", "altijd", headers, rows);
        assertEquals("Regel Belasting 1\n  geldig altijd\n    "
                + "de belasting van een Klant moet gesteld worden op 10 indien hij aan alle volgende voorwaarden voldoet:\n"
                + "      • hij een recht op duurzaamheidskorting heeft\n"
                + "      • zijn duur kleiner of gelijk is aan 60.\n",
                rules.get(0));
    }

    @Test void characteristic_condition_false_negates_the_possessive_check() {
        List<String> headers = List.of("",
                "de belasting van een Klant moet gesteld worden op",
                "indien hij een recht op duurzaamheidskorting heeft");
        List<List<String>> rows = List.of(List.of("1", "10", "onwaar"));
        List<String> rules = DecisionTableDesugar.desugar("Belasting", "altijd", headers, rows);
        assertEquals("Regel Belasting 1\n  geldig altijd\n    "
                + "de belasting van een Klant moet gesteld worden op 10 "
                + "indien hij geen recht op duurzaamheidskorting heeft.\n",
                rules.get(0));
    }

    @Test void multi_value_cell_under_equality_becomes_an_or_list_nested_last_inside_an_and() {
        // A §5.7 disjunction cell combined with another (AND) condition: the disjunction is reordered LAST and nested
        // as a §13.4.14.2 "er voldoet aan tenminste één …" sub-list (so its greedy bullet capture is harmless).
        List<String> headers = List.of("",
                "de korting van een Klant moet gesteld worden op",
                "indien zijn leeftijd groter is dan",
                "indien zijn klasse gelijk is aan");
        List<List<String>> rows = List.of(List.of("1", "5", "18", "\"eerste\" of \"tweede\""));
        List<String> rules = DecisionTableDesugar.desugar("Korting", "altijd", headers, rows);
        assertEquals("Regel Korting 1\n  geldig altijd\n    "
                + "de korting van een Klant moet gesteld worden op 5 indien hij aan alle volgende voorwaarden voldoet:\n"
                + "      • zijn leeftijd groter is dan 18\n"
                + "      • er voldoet aan tenminste één van de volgende voorwaarden:\n"
                + "        •• zijn klasse gelijk is aan \"eerste\"\n"
                + "        •• zijn klasse gelijk is aan \"tweede\".\n",
                rules.get(0));
    }

    @Test void multi_value_cell_split_is_quote_aware() {
        // §5.4: a tekstwaarde may legally contain a comma, the word "of", or '|'; none of those split the value.
        List<String> headers = List.of("",
                "de regiofactor van een Klant moet gesteld worden op",
                "indien zijn regio gelijk is aan");
        List<List<String>> rows = List.of(List.of("1", "1", "\"Kust, Noord\", \"A of B\" of \"Wad|land\""));
        List<String> rules = DecisionTableDesugar.desugar("W", "altijd", headers, rows);
        assertEquals(1, rules.size());
        String r = rules.get(0);
        assertTrue(r.contains("• zijn regio gelijk is aan \"Kust, Noord\""), r);
        assertTrue(r.contains("• zijn regio gelijk is aan \"A of B\""), r);
        assertTrue(r.contains("• zijn regio gelijk is aan \"Wad|land\""), r);
    }

    @Test void not_equal_series_uses_and_becomes_an_and_of_inequalities() {
        // §12: a concatenatie under "is ongelijk aan" uses "en" and means !=A AND !=B AND !=C.
        List<String> headers = List.of("",
                "de regiofactor van een Klant moet gesteld worden op",
                "indien zijn regio ongelijk is aan");
        List<List<String>> rows = List.of(List.of("1", "1", "\"Noord\", \"Oost\" en \"West\""));
        List<String> rules = DecisionTableDesugar.desugar("W", "altijd", headers, rows);
        assertEquals("Regel W 1\n  geldig altijd\n    "
                + "de regiofactor van een Klant moet gesteld worden op 1 indien hij aan alle volgende voorwaarden voldoet:\n"
                + "      • zijn regio ongelijk is aan \"Noord\"\n"
                + "      • zijn regio ongelijk is aan \"Oost\"\n"
                + "      • zijn regio ongelijk is aan \"West\".\n",
                rules.get(0));
    }

    @Test void more_than_one_disjunction_in_one_and_row_is_skipped_with_a_warning() {
        List<String> headers = List.of("",
                "de regiofactor van een Klant moet gesteld worden op",
                "indien zijn regio gelijk is aan",
                "indien zijn taal gelijk is aan");
        List<List<String>> rows = List.of(List.of("1", "1",
                "\"Noord\" of \"Oost\"", "\"Fries\" of \"Nedersaksisch\""));
        List<String> warns = new ArrayList<>();
        List<String> rules = DecisionTableDesugar.desugar("W", "altijd", headers, rows, warns::add);
        assertTrue(rules.isEmpty(), () -> "verwacht geen regel (niet stil renderen): " + rules);
        assertEquals(1, warns.size(), () -> "" + warns);
        assertTrue(warns.get(0).contains("meer dan één concatenatie-cel"), warns.get(0));
    }

    @Test void characteristic_conclusion_false_is_skipped_with_a_warning_not_silently() {
        List<String> headers = List.of("", "een klant is minderjarig", "indien zijn leeftijd kleiner is dan");
        List<List<String>> rows = List.of(List.of("1", "waar", "18 jr"), List.of("2", "onwaar", "65 jr"));
        List<String> warns = new ArrayList<>();
        List<String> rules = DecisionTableDesugar.desugar("M", "altijd", headers, rows, warns::add);
        assertEquals(1, rules.size(), () -> "alleen rij 1 (waar) levert een regel: " + rules);
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains("onwaar"), warns.get(0));
    }

    @Test void characteristic_condition_false_negates_each_characteristic_shape() {
        // bezittelijk "een"->"geen"; trailing-verb "X is"->"is niet X" (NOT a silently-positive "X is niet").
        List<String> headers = List.of("", "de belasting van een Klant moet gesteld worden op",
                "indien hij een recht op duurzaamheidskorting heeft", "indien hij gepensioneerd is");
        List<List<String>> rows = List.of(List.of("1", "10", "onwaar", "onwaar"));
        String r = DecisionTableDesugar.desugar("X", "altijd", headers, rows).get(0);
        assertTrue(r.contains("hij geen recht op duurzaamheidskorting heeft"), r);
        assertTrue(r.contains("hij is niet gepensioneerd"), r);
    }

    @Test void forbidden_conclusions_are_rejected_with_a_warning_not_mis_rendered() {
        // §12 C-6/C-7/C-8/C-9: only gelijkstelling ("moet gesteld worden op") + kenmerktoekenning ("waar") are
        // allowed; "berekend als", any other resultaatdeel, an empty conclusie cell, and an inline "indien" are out.
        List<String> warns = new ArrayList<>();
        java.util.function.BiFunction<String, String, List<String>> run = (conclHeader, conclCell) ->
                DecisionTableDesugar.desugar("T", "altijd",
                        List.of("", conclHeader, "indien zijn leeftijd kleiner is dan"),
                        List.of(List.of("1", conclCell, "18")), warns::add);
        assertTrue(run.apply("de korting van een Klant wordt berekend als", "5").isEmpty());          // C-7
        assertTrue(run.apply("de korting van een Klant wordt verdeeld over de klanten", "x").isEmpty()); // C-6
        assertTrue(run.apply("de korting van een Klant moet gesteld worden op", "").isEmpty());        // C-8
        assertTrue(run.apply("de korting van een Klant moet gesteld worden op", "5 indien iets").isEmpty()); // C-9
        assertEquals(4, warns.size(), () -> "elke verboden rij waarschuwt: " + warns);
        assertTrue(warns.get(0).contains("berekend als"));
        assertTrue(warns.get(1).contains("toegestaan"));
        assertTrue(warns.get(2).contains("lege conclusie-cel"));
        assertTrue(warns.get(3).contains("indien"));
    }

    @Test void c9_condition_keyword_rejection_is_token_and_quote_aware() {
        // §12 C-9 must reject only a STANDALONE "indien" OUTSIDE quotes — not the substring in an attribute name
        // ("indientermijn") nor an "indien" inside a Tekst literal ("indien nodig") (review false-positives).
        List<String> warns = new ArrayList<>();
        java.util.function.Function<String, List<String>> conclusion = cell ->
                DecisionTableDesugar.desugar("T", "altijd",
                        List.of("", "de korting van een Klant moet gesteld worden op", "indien zijn leeftijd kleiner is dan"),
                        List.of(List.of("1", cell, "18")), warns::add);
        assertEquals(1, conclusion.apply("zijn indientermijn").size(), () -> "indientermijn afgewezen: " + warns);
        assertEquals(1, conclusion.apply("\"indien nodig\"").size(), () -> "quoted indien afgewezen: " + warns);
        assertTrue(conclusion.apply("10 indien iets").isEmpty());   // a genuine standalone inline indien -> rejected
        assertTrue(warns.stream().anyMatch(w -> w.contains("indien")));
    }

    @Test void desugared_rows_resolve_through_the_existing_pipeline() {
        List<String> headers = List.of("",
                "de korting van een Klant moet gesteld worden op",
                "indien zijn leeftijd kleiner is dan");
        List<List<String>> rows = List.of(List.of("1", "10", "18"), List.of("2", "0", "65"));
        List<String> rules = DecisionTableDesugar.desugar("Korting", "altijd", headers, rows);

        String src = "Objecttype de Klant (mv: Klanten) (bezield)\n"
                + "    de leeftijd Numeriek (geheel getal);\n"
                + "    de korting Numeriek (geheel getal);\n"
                + String.join("", rules);
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "resolver-fout: " + m.errors());
        assertEquals(2, m.resolvedRules.size());
        for (ResolvedRule r : m.resolvedRules) {
            assertEquals(ResolvedRule.Kind.GELIJKSTELLING, r.kind());
            assertTrue(r.target() instanceof Expr.AttributeReference a && a.attribute().equals("korting"),
                    () -> "doel niet de korting: " + r.target());
            assertNotNull(r.condition(), "voorwaarde ontbreekt");
        }
    }
}
