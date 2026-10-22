package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §13.4.14.6–7: the depth of a bullet follows from its sign (grammar/DIVERGENCES.md, "§13.4.14.6–7 The depth of a
 * bullet"; card third-condition-level). The grammar reads a bullet without depth, and a nested group takes every bullet
 * after it; the frontend builds the tree from the flat list. The depth is the pair (number of sign characters, column),
 * compared on the sign first: '-' and '•' are level 1, '..' and '••' level 2, '...' and '•••' level 3, '....' level 4,
 * and the column decides only between bullets with the same sign. A deeper bullet is a child of the bullet before it,
 * which must open a group; an equal one is its sister; a shallower one returns to the open level of exactly its depth.
 */
class BulletDepthTest {

    static final String MODEL = """
        Domein Vondstsoort is van het type Enumeratie
            'Touw'
            'Glas'
            'Kurk'
            'Hout'
            'Blik'

        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);
            de soort Vondstsoort;
            de vrijstelling kenmerk (bezittelijk);

        """;

    /** The tree the card's witness means: 'Blik', or older than 65 with 'Touw', 'Glas' or 'Kurk'. */
    static final String THREE_LEVELS = "atLeast1[zijn soort is gelijk aan 'Blik', all0[zijn leeftijd is groter dan 65, "
            + "atLeast1[zijn soort is gelijk aan 'Touw', zijn soort is gelijk aan 'Glas', zijn soort is gelijk aan 'Kurk']]]";

    private static String rule(String first, String second, String third, String thirdIndent) {
        return MODEL + """
            Regel vrijstelling
                geldig altijd
                    Een Jutter heeft de vrijstelling
                    indien hij aan ten minste één van de volgende voorwaarden voldoet:
                    %1$s zijn soort is gelijk aan 'Blik'
                    %1$s hij voldoet aan alle volgende voorwaarden:
                    %2$s zijn leeftijd is groter dan 65
                    %2$s hij voldoet aan ten minste één van de volgende voorwaarden:
                    %4$s%3$s zijn soort is gelijk aan 'Touw'
                    %4$s%3$s zijn soort is gelijk aan 'Glas'
                    %4$s%3$s zijn soort is gelijk aan 'Kurk'.
            """.formatted(first, second, third, thirdIndent);
    }

    /** R1 of the card: '-', '..', '....'. "Deeper" is strictly greater, so a level may skip a dot. */
    @Test void r1_dash_and_dots_read_three_levels() {
        assertThreeLevels(rule("-", "..", "....", ""));
    }

    /** R2 of the card, the spec's own signs: '•', '••', '•••' (§13.4.14.7 counts the bullets). */
    @Test void r2_bullet_signs_read_three_levels() {
        assertThreeLevels(rule("•", "••", "•••", ""));
    }

    /** R2 of the card, the third level written with the sign of the second, four columns further in. */
    @Test void r2_an_indented_third_level_reads_three_levels() {
        assertThreeLevels(rule("-", "..", "..", "    "));
    }

    /** R3 of the card: '...' is one token and level 3, no syntax error. */
    @Test void r3_three_dots_are_the_third_level() {
        assertThreeLevels(rule("-", "..", "...", ""));
    }

    /**
     * The silent case of the Fable advice (proef/q3): under 'alle', a nested 'ten minste één' group with every bullet
     * '-' on one column. The grammar put the last bullet, meant as a member of the 'alle' list, into the or-group, and
     * the IR carried that reading without a diagnostic. The group header has no deeper bullet after it: an ERROR.
     */
    @Test void a_group_header_followed_by_its_own_depth_is_an_error_under_all() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vrijstelling
                geldig altijd
                    Een Jutter heeft de vrijstelling
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn leeftijd is groter dan 65
                    - hij voldoet aan ten minste één van de volgende voorwaarden:
                    - zijn soort is gelijk aan 'Touw'
                    - zijn soort is gelijk aan 'Glas'
                    - zijn leeftijd is kleiner dan 80.
            """);
        Diagnostic d = onlyError(m, "RGS-353");
        assertEquals(MODEL.lines().count() + 6, d.line(), "de ERROR staat op de regel van de groepskop");
        assertTrue(d.message().contains("- hij voldoet aan ten minste één van de volgende voorwaarden:"),
                () -> "de ERROR noemt de groepskop: " + d.message());
    }

    /** The silent case written with its depth: the or-group one level deeper, the last bullet back on the level of the
     *  'alle' list. The group's source runs to its own last bullet, not to the end of the list as its parse does. */
    @Test void a_return_to_the_outer_level_closes_the_group() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vrijstelling
                geldig altijd
                    Een Jutter heeft de vrijstelling
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn leeftijd is groter dan 65
                    - hij voldoet aan ten minste één van de volgende voorwaarden:
                    .. zijn soort is gelijk aan 'Touw'
                    .. zijn soort is gelijk aan 'Glas'
                    - zijn leeftijd is kleiner dan 80.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "geen diagnostic verwacht: " + m.diagnostics);
        Condition c = onlyRule(m).condition();
        assertEquals("all0[zijn leeftijd is groter dan 65, atLeast1[zijn soort is gelijk aan 'Touw', zijn soort is gelijk "
                + "aan 'Glas'], zijn leeftijd is kleiner dan 80]", tree(c));
        assertEquals("hij voldoet aan ten minste één van de volgende voorwaarden: .. zijn soort is gelijk aan 'Touw' .. "
                + "zijn soort is gelijk aan 'Glas'", ((Condition.Compound) c).components().get(1).source().snippet());
    }

    /** A shallower bullet returns to the open level of its depth, two levels up at once. */
    @Test void a_return_may_close_two_groups() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vrijstelling
                geldig altijd
                    Een Jutter heeft de vrijstelling
                    indien hij aan ten minste één van de volgende voorwaarden voldoet:
                    - hij voldoet aan alle volgende voorwaarden:
                    .. zijn leeftijd is groter dan 65
                    .. hij voldoet aan ten minste één van de volgende voorwaarden:
                    ... zijn soort is gelijk aan 'Touw'
                    ... zijn soort is gelijk aan 'Glas'
                    - zijn soort is gelijk aan 'Blik'.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "geen diagnostic verwacht: " + m.diagnostics);
        assertEquals("atLeast1[all0[zijn leeftijd is groter dan 65, atLeast1[zijn soort is gelijk aan 'Touw', zijn soort "
                + "is gelijk aan 'Glas']], zijn soort is gelijk aan 'Blik']", tree(onlyRule(m).condition()));
    }

    /** R4 of the card, three levels under 'alle', read as before. */
    @Test void r4_three_levels_under_all_read_as_before() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vrijstelling
                geldig altijd
                    Een Jutter heeft de vrijstelling
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn leeftijd is groter dan 17
                    - hij voldoet aan ten minste één van de volgende voorwaarden:
                    .. zijn soort is gelijk aan 'Blik'
                    .. hij voldoet aan alle volgende voorwaarden:
                    .... zijn leeftijd is groter dan 65
                    .... zijn soort is gelijk aan 'Touw'.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "geen diagnostic verwacht: " + m.diagnostics);
        assertEquals("all0[zijn leeftijd is groter dan 17, atLeast1[zijn soort is gelijk aan 'Blik', all0[zijn leeftijd "
                + "is groter dan 65, zijn soort is gelijk aan 'Touw']]]", tree(onlyRule(m).condition()));
    }

    @Test void a_deeper_bullet_without_a_group_header_is_an_error() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vrijstelling
                geldig altijd
                    Een Jutter heeft de vrijstelling
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn leeftijd is groter dan 65
                    .. zijn soort is gelijk aan 'Touw'.
            """);
        Diagnostic d = onlyError(m, "RGS-354");
        assertEquals(MODEL.lines().count() + 6, d.line());
        assertTrue(d.message().contains("'.. zijn soort is gelijk aan 'Touw'.'")
                && d.message().contains("'- zijn leeftijd is groter dan 65'"), d.message());
    }

    /** Open are level 1 and level 4; a bullet of level 2 returns to neither. */
    @Test void a_return_to_a_depth_that_is_no_open_level_is_an_error() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vrijstelling
                geldig altijd
                    Een Jutter heeft de vrijstelling
                    indien hij aan ten minste één van de volgende voorwaarden voldoet:
                    - hij voldoet aan alle volgende voorwaarden:
                    .... zijn leeftijd is groter dan 65
                    .... zijn soort is gelijk aan 'Touw'
                    .. zijn soort is gelijk aan 'Blik'.
            """);
        Diagnostic d = onlyError(m, "RGS-355");
        assertEquals(MODEL.lines().count() + 8, d.line());
        assertTrue(d.message().contains("'.. zijn soort is gelijk aan 'Blik'.' staat minder diep dan de bullet ervoor"),
                d.message());
    }

    /** Two sign families in one list, also where the column alone would order them (until card
     *  third-condition-level a '•' list indented under a '-' header was re-nested by column). */
    @Test void two_sign_families_are_an_error() {
        for (String second : List.of("• zijn soort is gelijk aan 'Touw'.", "- hij voldoet aan alle volgende voorwaarden:\n"
                + "    • zijn soort is gelijk aan 'Touw'\n    • zijn soort is gelijk aan 'Glas'.")) {
            RegelSpraakModel m = Resolver.resolve(MODEL + """
                Regel vrijstelling
                    geldig altijd
                        Een Jutter heeft de vrijstelling
                        indien hij aan alle volgende voorwaarden voldoet:
                        - zijn leeftijd is groter dan 65
                """ + second.indent(8));
            Diagnostic d = onlyError(m, "RGS-356");
            assertTrue(d.message().contains("de bullettekens '-' en '•'"), d.message());
        }
    }

    // ---------------- consistency criteria: the same tree ----------------

    @Test void consistency_criteria_read_three_levels() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vrijstellingseis
                geldig altijd
                    Een Jutter moet voldoen aan ten minste één van de volgende criteria:
                    - zijn soort is gelijk aan 'Blik'
                    - er wordt voldaan aan alle volgende criteria:
                    .. zijn leeftijd is groter dan 65
                    .. er wordt voldaan aan ten minste één van de volgende criteria:
                    ... zijn soort is gelijk aan 'Touw'
                    ... zijn soort is gelijk aan 'Glas'.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "geen diagnostic verwacht: " + m.diagnostics);
        assertEquals(1, m.consistencies.size());
        assertEquals("atLeast1[zijn soort is gelijk aan 'Blik', all0[zijn leeftijd is groter dan 65, atLeast1[zijn soort "
                + "is gelijk aan 'Touw', zijn soort is gelijk aan 'Glas']]]", tree(m.consistencies.get(0).assertion()));
    }

    /** The silent case among criteria; the ERROR is the rule's one message (no RGS-180 beside it). */
    @Test void a_criteria_header_followed_by_its_own_depth_is_an_error() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vrijstellingseis
                geldig altijd
                    Een Jutter moet voldoen aan alle volgende criteria:
                    - zijn leeftijd is groter dan 17
                    - er wordt voldaan aan ten minste één van de volgende criteria:
                    - zijn soort is gelijk aan 'Touw'
                    - zijn soort is gelijk aan 'Glas'.
            """);
        Diagnostic d = onlyError(m, "RGS-353");
        assertEquals(MODEL.lines().count() + 5, d.line());
    }

    // ---------------- subselections: the same tree ----------------

    static final String PLOEG = """
        Objecttype de Schouwploeg (mv: Schouwploegen)
            het wachttal Numeriek (geheel getal);

        Objecttype de Strandwacht (mv: Strandwachten) (bezield)
            de dienstjaren Numeriek (geheel getal);
            is gediplomeerd kenmerk (bijvoeglijk);

        Feittype schouwindeling
            de ploeg Schouwploeg
            de wacht (mv: wachten) Strandwacht
        Eén ploeg omvat meerdere wachten

        """;

    /** A counting quantifier over a subselection whose group holds a group: until card third-condition-level a marker
     *  (the grammar might have pulled sister bullets into the deeper group); with the depth in the sign it reads. */
    @Test void a_counting_subselection_reads_a_nested_group() {
        RegelSpraakModel m = Resolver.resolve(PLOEG + """
            Regel wachttal van een ploeg
                geldig altijd
                    Het wachttal van een Schouwploeg moet berekend worden als het aantal wachten van de Schouwploeg die aan ten minste 2 van de volgende voorwaarden voldoen:
                    .. hij is gediplomeerd
                    .. hij voldoet aan alle volgende voorwaarden:
                    ... zijn dienstjaren zijn groter dan 3
                    ... zijn dienstjaren zijn kleiner dan 10
                    .. zijn dienstjaren zijn groter dan 20.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "geen diagnostic verwacht: " + m.diagnostics);
        assertEquals("atLeast2[hij is gediplomeerd, all0[zijn dienstjaren zijn groter dan 3, zijn dienstjaren zijn "
                + "kleiner dan 10], zijn dienstjaren zijn groter dan 20]", tree(filterOf(onlyRule(m).value())));
    }

    static final String SCHUIT = """
        Domein Klachtsoort is van het type Enumeratie
            'Licht'
            'Zwaar'

        Objecttype de Schuit (mv: Schuiten)
            de controle Datum in dagen;
            de dagen Numeriek (geheel getal);
            is gekeurd kenmerk (bijvoeglijk);

        Objecttype de Klacht (mv: Klachten)
            de soort Klachtsoort;
            de einddag Datum in dagen;
            de dagen Numeriek (geheel getal);

        Feittype schuitklacht
            de beklaagde schuit Schuit
            de klacht (mv: klachten) Klacht
        Eén beklaagde schuit heeft meerdere klachten

        Regel gekeurd
            geldig altijd
                Een Schuit is gekeurd
                indien hij aan alle volgende voorwaarden voldoet:
        """;

    private static final String SUBSELECTION = "zijn controle is later dan de laatste van de einddag van alle klachten "
            + "van de Schuit die aan alle volgende voorwaarden voldoen:";

    /**
     * A bulleted subselection in a bullet of an outer list, followed by a bullet of that list. The grammar gives the
     * subselection every bullet after it; until card third-condition-level '- zijn dagen is groter dan 10' became a
     * member of the filter, read on the Klacht (which has 'dagen' too), with no diagnostic. The bullet is not deeper than
     * the bullet of the subselection, so it is not hers, and the tree cannot hand it back: an ERROR with the two ways
     * out, both of which read.
     */
    @Test void a_bullet_after_a_bulleted_subselection_is_refused_with_the_ways_out() {
        RegelSpraakModel m = Resolver.resolve(SCHUIT + """
                    - %s
                    .. zijn soort is gelijk aan 'Zwaar'
                    .. zijn dagen is groter dan 2
                    - zijn dagen is groter dan 10.
            """.formatted(SUBSELECTION));
        List<Diagnostic> errors = m.diagnostics.stream().filter(Diagnostic::isError).toList();
        assertEquals(1, errors.size(), () -> "één ERROR verwacht: " + m.diagnostics);
        assertEquals("RGS-355", errors.get(0).code());
        assertEquals(SCHUIT.lines().count() + 4, errors.get(0).line());
        assertTrue(errors.get(0).message().contains("'- zijn dagen is groter dan 10.' staat niet dieper dan de bullet '- "
                + SUBSELECTION + "'") && errors.get(0).message().contains("of zet de deelselectie tussen haakjes"),
                errors.get(0).message());

        String last = SCHUIT + """
                    - zijn dagen is groter dan 10
                    - %s
                    .. zijn soort is gelijk aan 'Zwaar'
                    .. zijn dagen is groter dan 2.
            """.formatted(SUBSELECTION);
        String parenthesised = SCHUIT + """
                    - zijn controle is later dan (de laatste van de einddag van alle klachten van de Schuit die aan alle volgende voorwaarden voldoen:
                    .. zijn soort is gelijk aan 'Zwaar'
                    .. zijn dagen is groter dan 2)
                    - zijn dagen is groter dan 10.
            """;
        for (String source : List.of(last, parenthesised)) {
            RegelSpraakModel ok = Resolver.resolve(source);
            assertTrue(ok.diagnostics.isEmpty(), () -> "geen diagnostic verwacht: " + ok.diagnostics);
            Condition.Compound outer = (Condition.Compound) onlyRule(ok).condition();
            assertEquals(2, outer.components().size(), () -> tree(outer));
            Condition.Comparison withFilter = (Condition.Comparison) outer.components().stream()
                    .filter(c -> c instanceof Condition.Comparison v && v.right() instanceof dev.uchar.regelspraak.ir.Expr.FunctionCall)
                    .findFirst().orElseThrow();
            assertEquals("all0[zijn soort is gelijk aan 'Zwaar', zijn dagen is groter dan 2]",
                    tree(filterOf(withFilter.right())));
        }
    }

    /** The bullets of a subselection in a bullet stand deeper than that bullet. */
    @Test void a_subselection_whose_bullets_are_not_deeper_than_its_bullet_is_an_error() {
        RegelSpraakModel m = Resolver.resolve(SCHUIT + """
                    - zijn dagen is groter dan 10
                    - %s
                    - zijn soort is gelijk aan 'Zwaar'
                    - zijn dagen is groter dan 2.
            """.formatted(SUBSELECTION));
        List<Diagnostic> errors = m.diagnostics.stream().filter(Diagnostic::isError).toList();
        assertEquals(1, errors.size(), () -> "één ERROR verwacht: " + m.diagnostics);
        assertEquals("RGS-353", errors.get(0).code());
        assertEquals(SCHUIT.lines().count() + 2, errors.get(0).line());
    }

    private static void assertThreeLevels(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.diagnostics.isEmpty(), () -> "geen diagnostic verwacht: " + m.diagnostics);
        assertEquals(THREE_LEVELS, tree(onlyRule(m).condition()));
    }

    private static dev.uchar.regelspraak.ir.ResolvedRule onlyRule(RegelSpraakModel m) {
        assertEquals(1, m.resolvedRules.size(), () -> "één regel verwacht: " + m.resolvedRules);
        return m.resolvedRules.get(0);
    }

    /** One ERROR, of {@code code}, and no other diagnostic. */
    static Diagnostic onlyError(RegelSpraakModel m, String code) {
        assertEquals(1, m.diagnostics.size(), () -> "één diagnostic verwacht: " + m.diagnostics);
        Diagnostic d = m.diagnostics.get(0);
        assertEquals(code, d.code(), () -> "verwachtte " + code + ": " + d);
        assertEquals(Severity.ERROR, d.severity());
        return d;
    }

    /** The filter of an aggregation. */
    private static Condition filterOf(dev.uchar.regelspraak.ir.Expr e) {
        assertInstanceOf(dev.uchar.regelspraak.ir.Expr.FunctionCall.class, e, () -> "een aggregatie verwacht: " + e);
        Condition filter = ((dev.uchar.regelspraak.ir.Expr.FunctionCall) e).filter();
        assertNotNull(filter, () -> "de aggregatie draagt geen filter: " + e);
        return filter;
    }

    /** A compound as its quantifier, threshold and parts; any other condition as its source snippet. */
    static String tree(Condition c) {
        if (c instanceof Condition.Compound s)
            return quantifier(s.quantifier()) + s.threshold()
                    + s.components().stream().map(BulletDepthTest::tree).collect(Collectors.joining(", ", "[", "]"));
        return c.source().snippet();
    }

    private static String quantifier(Condition.Quantifier q) {
        return switch (q) {
            case ALLE -> "all";
            case DE -> "the";
            case GEEN -> "none";
            case TEN_MINSTE -> "atLeast";
            case TEN_HOOGSTE -> "atMost";
            case PRECIES -> "exactly";
        };
    }
}
