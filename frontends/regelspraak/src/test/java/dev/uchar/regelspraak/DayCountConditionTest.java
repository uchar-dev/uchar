package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The condition of a day count ("het aantal dagen in de maand dat …", §13.4.17.57) and of the two gedurende clips
 * (the temporal total and the pro-rata part, "gedurende de tijd dat …"). A day count counts the days of one own
 * time-dependent characteristic of the subject, held positively (RUNTIME-SEMANTICS ch. 3.8, WF-058); a clip member
 * carries no whole-period qualifier (WF-097); the clips take the shapes WF-055 and WF-056 admit. The frontend lowers
 * exactly those conditions. Any other condition is an ERROR (RGS-402) that names why, so the IR never says something
 * else than the source and the frontend writes no document the verifier refuses: no count without a condition, no
 * count of the subject's own characteristic where the source names another object's, no clip that drops a qualifier
 * the source writes, and no clip over another object or with a static member.
 */
class DayCountConditionTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is borgplichtig kenmerk (bijvoeglijk) voor elke maand;
            is aangesteld kenmerk (bijvoeglijk) voor elke maand;
            is vrijgesteld kenmerk (bijvoeglijk);
            de heffing Numeriek (geheel getal) voor elke maand;
            de borgdagen Numeriek (niet-negatief geheel getal) voor elke maand;

        Objecttype de Reis (mv: Reizen)
            is borgplichtig kenmerk (bijvoeglijk) voor elke maand;

        Feittype reisdeelname
            de reizende jutter Jutter
            de vaartocht Reis
        Eén reizende jutter maakt één vaartocht

        Objecttype de Ploeg (mv: Ploegen)
            is borgplichtig kenmerk (bijvoeglijk) voor elke maand;

        Feittype ploegindeling
            de ploeg Ploeg
            de jutter (mv: jutters) Jutter
        Eén ploeg omvat meerdere jutters

        """;

    private static RegelSpraakModel dayCount(String condition) {
        return Resolver.resolve(MODEL + """
            Regel borgdagen van een jutter
                geldig altijd
                    De borgdagen van een Jutter moet berekend worden als het aantal dagen in de maand dat %s.
            """.formatted(condition));
    }

    private static List<Diagnostic> warnings(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.severity() == Severity.WARNING).toList();
    }

    /** The one diagnostic of {@code m}: the ERROR RGS-402 that refuses the condition {@code snippet}, with
     *  {@code reasonKey} as its reason. */
    private static Msg refusedWith(RegelSpraakModel m, String reasonKey, String snippet) {
        assertEquals(1, m.diagnostics.size(), m.diagnostics::toString);
        Diagnostic d = m.diagnostics.get(0);
        assertTrue(d.isError(), d::toString);
        assertEquals("RGS-402", d.code(), d::toString);
        Msg reason = (Msg) d.args().get("reason");
        assertEquals(reasonKey, reason.key(), d::toString);
        assertTrue(d.message().contains("'" + snippet + "'"), d::message);
        return reason;
    }

    @Test void an_own_time_dependent_characteristic_is_the_filter_of_the_count() {
        RegelSpraakModel m = dayCount("hij borgplichtig is");
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"kind\":\"dayCount\",\"arguments\":[],\"filter\":{\"node\":\"CharacteristicCheck\","
                + "\"characteristic\":\"borgplichtig\",\"negated\":false,"), c);
    }

    @Test void a_name_that_binds_no_characteristic_is_named() {
        Msg reason = refusedWith(dayCount("hij borgvrij is"), "RGS-092.day-count-name-unbound", "hij borgvrij is");
        assertEquals("borgvrij", String.valueOf(reason.args().get("name")));
        assertEquals("Jutter", String.valueOf(reason.args().get("subject")));
    }

    @Test void a_predicate_is_no_characteristic() {
        refusedWith(dayCount("zijn heffing gevuld is"), "RGS-092.day-count-condition-form", "zijn heffing gevuld is");
    }

    @Test void a_comparison_is_no_characteristic() {
        refusedWith(dayCount("zijn heffing groter is dan 3"), "RGS-092.day-count-condition-form",
                "zijn heffing groter is dan 3");
    }

    @Test void a_characteristic_that_is_not_time_dependent_is_refused_by_its_form() {
        refusedWith(dayCount("hij vrijgesteld is"), "RGS-092.day-count-condition-form", "hij vrijgesteld is");
    }

    @Test void a_negated_characteristic_is_refused_by_its_form() {
        refusedWith(dayCount("hij niet borgplichtig is"), "RGS-092.day-count-condition-form", "hij niet borgplichtig is");
    }

    @Test void a_characteristic_of_another_object_is_not_read_as_the_subjects_own() {
        refusedWith(dayCount("zijn vaartocht borgplichtig is"), "RGS-092.day-count-condition-form",
                "zijn vaartocht borgplichtig is");
    }

    /** "zijn ploeg" builds no navigation of its own here; the onderwerp still names another object. */
    @Test void a_characteristic_of_an_object_over_a_role_by_type_is_not_read_as_the_subjects_own() {
        refusedWith(dayCount("zijn ploeg borgplichtig is"), "RGS-092.day-count-condition-form",
                "zijn ploeg borgplichtig is");
    }

    @Test void the_subject_named_by_its_type_is_the_subject() {
        RegelSpraakModel m = dayCount("de Jutter borgplichtig is");
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"kind\":\"dayCount\",\"arguments\":[],\"filter\":{\"node\":\"CharacteristicCheck\","
                + "\"characteristic\":\"borgplichtig\",\"negated\":false,"), c);
    }

    @Test void a_whole_period_qualifier_is_refused_by_its_form() {
        refusedWith(dayCount("hij gedurende de gehele maand borgplichtig is"), "RGS-092.day-count-condition-form",
                "hij gedurende de gehele maand borgplichtig is");
    }

    private static final String DAY_KIND = """
        Dagsoort de schrikkeldag (mv: schrikkeldagen);

        Regel schrikkeldag
            geldig altijd
                Een dag is een schrikkeldag
                indien er aan alle volgende voorwaarden wordt voldaan:
                - de maand uit de dag gelijk is aan 2
                - de dag uit de dag gelijk is aan 29.

        """;

    /** IMPLICIT-TIME 7b: a day-kind test of "de dag", the counted day, beside one own characteristic, is a member of
     *  the count's filter (WF-058), with the bound day as its operand. */
    @Test void a_day_kind_test_of_the_counted_day_is_a_member_of_the_filter() {
        RegelSpraakModel m = Resolver.resolve(MODEL + DAY_KIND + """
            Regel borgdagen van een jutter
                geldig altijd
                    De borgdagen van een Jutter moet berekend worden als het aantal dagen in de maand dat er aan alle volgende voorwaarden wordt voldaan:
                    - hij is borgplichtig
                    - de dag is geen schrikkeldag.
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"kind\":\"dayCount\",\"arguments\":[],\"filter\":{\"node\":\"Compound\",\"quantifier\":\"all\","
                + "\"threshold\":0,\"parts\":[{\"node\":\"CharacteristicCheck\",\"characteristic\":\"borgplichtig\","), c);
        assertTrue(c.contains("{\"node\":\"Predicate\",\"kind\":\"dayKind\",\"operand\":{\"node\":\"BoundDay\","), c);
        Report.assertAccountedFor(IrEmitter.emit(m));
    }

    /** The same count bound to a variable of the source ("Daarbij geldt:") is a Let whose type is the count's, which
     *  the spec fixes (whole, non-negative); the emitter wrote no type for it and threw instead. */
    @Test void a_variable_bound_to_a_compound_day_count_is_emitted() {
        RegelSpraakModel m = Resolver.resolve(MODEL + DAY_KIND + """
            Regel borgdagen van een jutter
                geldig altijd
                    De borgdagen van een Jutter moet berekend worden als de getelde dagen.
                    Daarbij geldt:
                        de getelde dagen is het aantal dagen in de maand dat er aan alle volgende voorwaarden wordt voldaan:
                        - hij is borgplichtig
                        - de dag is geen schrikkeldag.
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("{\"node\":\"Let\",\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,"
                + "\"sign\":\"nonNegative\"},\"binder\":\"geteldedagen\""), c);
    }

    /** Two characteristics in the count's compound stay a marker: WF-058 admits one beside the day-kind tests. */
    @Test void a_compound_of_a_characteristic_and_a_day_kind_test_needs_one_characteristic() {
        RegelSpraakModel m = Resolver.resolve(MODEL + DAY_KIND + """
            Regel borgdagen van een jutter
                geldig altijd
                    De borgdagen van een Jutter moet berekend worden als het aantal dagen in de maand dat er aan alle volgende voorwaarden wordt voldaan:
                    - hij is borgplichtig
                    - hij is aangesteld
                    - de dag is geen schrikkeldag.
            """);
        refusedWith(m, "RGS-092.day-count-condition-form",
                "er aan alle volgende voorwaarden wordt voldaan: - hij is borgplichtig - hij is aangesteld - de dag is geen schrikkeldag");
    }

    @Test void a_compound_condition_is_refused_by_its_form() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel borgdagen van een jutter
                geldig altijd
                    De borgdagen van een Jutter moet berekend worden als het aantal dagen in de maand dat hij aan alle volgende voorwaarden voldoet:
                    - hij borgplichtig is
                    - hij aangesteld is.
            """);
        refusedWith(m, "RGS-092.day-count-condition-form",
                "hij aan alle volgende voorwaarden voldoet: - hij borgplichtig is - hij aangesteld is");
    }

    // ---------------- the whole-period qualifier in the condition of a gedurende clip ----------------

    private static RegelSpraakModel clip(String value) {
        return Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de wachtvergoeding per maand Numeriek (geheel getal) voor elke maand;
                het wachttarief Numeriek (getal met 2 decimalen) voor elk jaar;
                is wachtplichtig kenmerk (bijvoeglijk) voor elke maand;
                is aangesteld kenmerk (bijvoeglijk) voor elke maand;
                is bemand kenmerk (bijvoeglijk) voor elke maand;
                de leeftijd Numeriek (geheel getal);
                de wachtsom Numeriek (geheel getal);
                de stilwachtsom Numeriek (geheel getal);
                de wachtbijdrage Numeriek (getal met 2 decimalen) voor elke maand;

            Objecttype de Reis (mv: Reizen)
                is gestremd kenmerk (bijvoeglijk) voor elke maand;

            Feittype reisdeelname
                de reizende jutter Jutter
                de vaartocht Reis
            Eén reizende jutter maakt één vaartocht

            Objecttype de Ploeg (mv: Ploegen)
                is bemand kenmerk (bijvoeglijk) voor elke maand;

            Feittype ploegindeling
                de ploeg Ploeg
                de jutter (mv: jutters) Jutter
            Eén ploeg omvat meerdere jutters

            """ + value);
    }

    /** A clip condition the wire does not carry is refused: one ERROR RGS-402 that quotes the condition, with the
     *  shapes of the total's clip (WF-055) or the pro-rata part's (WF-056) as its reason. */
    private static void clipRefused(RegelSpraakModel m, String condition) {
        assertEquals(1, m.diagnostics.size(), m.diagnostics::toString);
        Diagnostic d = m.diagnostics.get(0);
        assertTrue(d.isError(), d::toString);
        assertEquals("RGS-402", d.code(), d::toString);
        assertTrue(d.message().contains("'" + condition + "'"), d::message);
    }

    @Test void a_total_keeps_no_clip_whose_qualifier_it_drops() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij gedurende het gehele jaar wachtplichtig is).
            """), "gedurende de tijd dat hij gedurende het gehele jaar wachtplichtig is");
    }

    @Test void a_pro_rata_part_keeps_no_clip_whose_qualifier_it_drops() {
        clipRefused(clip("""
            Regel wachtbijdrage van een jutter
                geldig altijd
                    De wachtbijdrage van een Jutter moet gesteld worden op het tijdsevenredig deel per maand van zijn wachttarief gedurende de tijd dat hij gedurende de gehele maand wachtplichtig is.
            """), "gedurende de tijd dat hij gedurende de gehele maand wachtplichtig is");
    }

    @Test void a_comparison_clip_keeps_no_qualifier_it_drops() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn wachtvergoeding per maand gedurende het gehele jaar groter is dan 10).
            """), "gedurende de tijd dat zijn wachtvergoeding per maand gedurende het gehele jaar groter is dan 10");
    }

    @Test void a_compound_clip_keeps_no_member_whose_qualifier_it_drops() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
                    - hij is gedurende het gehele jaar wachtplichtig
                    - hij niet aangesteld is).
            """), "gedurende de tijd dat hij aan alle volgende voorwaarden voldoet: - hij is gedurende het gehele jaar wachtplichtig - hij niet aangesteld is");
    }

    @Test void a_navigated_clip_keeps_no_qualifier_it_drops() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn vaartocht gedurende het gehele jaar gestremd is).
            """), "gedurende de tijd dat zijn vaartocht gedurende het gehele jaar gestremd is");
    }

    @Test void a_static_member_of_a_compound_clip_keeps_no_qualifier_it_drops() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
                    - hij wachtplichtig is
                    - zijn leeftijd is gedurende het gehele jaar groter dan 18).
            """), "gedurende de tijd dat hij aan alle volgende voorwaarden voldoet: - hij wachtplichtig is - zijn leeftijd is gedurende het gehele jaar groter dan 18");
    }

    // ---------------- the onderwerp of a membership in the condition of a gedurende clip ----------------

    @Test void a_clip_does_not_read_another_objects_characteristic_as_the_subjects_own() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn ploeg bemand is).
            """), "gedurende de tijd dat zijn ploeg bemand is");
    }

    @Test void a_compound_clip_does_not_read_another_objects_characteristic_in_a_bullet() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
                    - hij wachtplichtig is
                    - zijn ploeg bemand is).
            """), "gedurende de tijd dat hij aan alle volgende voorwaarden voldoet: - hij wachtplichtig is - zijn ploeg bemand is");
    }

    @Test void a_compound_clip_over_another_object_is_not_the_subjects() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn ploeg aan alle volgende voorwaarden voldoet:
                    - hij bemand is
                    - hij bemand is).
            """), "gedurende de tijd dat zijn ploeg aan alle volgende voorwaarden voldoet: - hij bemand is - hij bemand is");
    }

    /** Without a qualifier, a clip over another object's characteristic and a compound clip with a static member are
     *  still no shapes WF-055 admits: the total's clip reads the subject's own characteristics only. */
    @Test void a_navigated_clip_and_a_static_member_are_refused() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn vaartocht gestremd is) rekenkundig afgerond op 0 decimalen.
            """), "gedurende de tijd dat zijn vaartocht gestremd is");
        clipRefused(clip("""
            Regel stilwachtsom van een jutter
                geldig altijd
                    De stilwachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
                    - hij wachtplichtig is
                    - zijn leeftijd is groter dan 18) rekenkundig afgerond op 0 decimalen.
            """), "gedurende de tijd dat hij aan alle volgende voorwaarden voldoet: - hij wachtplichtig is - zijn leeftijd is groter dan 18");
    }

    /** The comparison a total's clip takes reads the subject's own timeline on the left (WF-055); a timeline on the
     *  right, an at-least compound and a comparison or compound on the pro-rata part are refused at their place. */
    @Test void the_clip_shapes_the_verifier_refuses_are_refused() {
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn leeftijd kleiner is dan zijn wachtvergoeding per maand).
            """), "gedurende de tijd dat zijn leeftijd kleiner is dan zijn wachtvergoeding per maand");
        clipRefused(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat er aan ten minste één van de volgende voorwaarden wordt voldaan:
                    - hij is wachtplichtig
                    - hij is aangesteld).
            """), "gedurende de tijd dat er aan ten minste één van de volgende voorwaarden wordt voldaan: - hij is wachtplichtig - hij is aangesteld");
        clipRefused(clip("""
            Regel wachtbijdrage van een jutter
                geldig altijd
                    De wachtbijdrage van een Jutter moet gesteld worden op het tijdsevenredig deel per maand van zijn wachttarief gedurende de tijd dat zijn wachtvergoeding per maand groter is dan 10.
            """), "gedurende de tijd dat zijn wachtvergoeding per maand groter is dan 10");
        clipRefused(clip("""
            Regel wachtbijdrage van een jutter
                geldig altijd
                    De wachtbijdrage van een Jutter moet gesteld worden op het tijdsevenredig deel per maand van zijn wachttarief gedurende de tijd dat er aan alle volgende voorwaarden wordt voldaan:
                    - hij is wachtplichtig
                    - hij is aangesteld.
            """), "gedurende de tijd dat er aan alle volgende voorwaarden wordt voldaan: - hij is wachtplichtig - hij is aangesteld");
    }

    @Test void the_admitted_clips_stay_lowered() {
        RegelSpraakModel m = clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn wachtvergoeding per maand groter is dan 10) rekenkundig afgerond op 0 decimalen.

            Regel stilwachtsom van een jutter
                geldig altijd
                    De stilwachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
                    - hij wachtplichtig is
                    - hij niet aangesteld is) rekenkundig afgerond op 0 decimalen.
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"filter\":{\"node\":\"Comparison\","), c);
        assertTrue(c.contains("\"filter\":{\"node\":\"Compound\",\"quantifier\":\"all\","), c);
    }

    @Test void a_clip_without_qualifier_stays_lowered() {
        RegelSpraakModel m = clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is) rekenkundig afgerond op 0 decimalen.
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"wachtplichtig\",\"negated\":false,\"source\""), c);
    }
}
