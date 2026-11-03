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
 * carries no whole-period qualifier (WF-097). The frontend lowers exactly those conditions. Any other condition stays
 * on the wire as a marker with a WARNING that names why, so the IR never says something else than the source: no
 * count without a condition, no count of the subject's own characteristic where the source names another object's,
 * and no clip that drops a qualifier the source writes.
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

    /** The one WARNING of {@code m}: the sweep's report of the marker, with {@code reasonKey} as its reason; the
     *  document carries the marker as the filter of the count and stays emittable and accounted for (WF-113). */
    private static Msg markedWith(RegelSpraakModel m, String reasonKey, String snippet) {
        assertTrue(m.diagnostics.stream().noneMatch(Diagnostic::isError), () -> m.diagnostics.toString());
        List<Diagnostic> w = warnings(m);
        assertEquals(1, w.size(), w::toString);
        Diagnostic d = w.get(0);
        assertEquals("RGS-092", d.code(), d::toString);
        Msg reason = (Msg) d.args().get("reason");
        assertEquals(reasonKey, reason.key(), d::toString);
        assertTrue(d.message().contains("'" + snippet + "'"), d::message);
        String ir = IrEmitter.emit(m);
        String c = ir.replaceAll("\\s+", "");
        assertTrue(c.contains("\"kind\":\"dayCount\",\"arguments\":[],\"filter\":{\"node\":\"Unresolved\",\"source\":{\"snippet\":\""
                + snippet.replaceAll("\\s+", "") + "\"}}"), c);
        Report.assertAccountedFor(ir);
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
        Msg reason = markedWith(dayCount("hij borgvrij is"), "RGS-092.day-count-name-unbound", "hij borgvrij is");
        assertEquals("borgvrij", String.valueOf(reason.args().get("name")));
        assertEquals("Jutter", String.valueOf(reason.args().get("subject")));
    }

    @Test void a_predicate_is_no_characteristic() {
        markedWith(dayCount("zijn heffing gevuld is"), "RGS-092.day-count-condition-form", "zijn heffing gevuld is");
    }

    @Test void a_comparison_is_no_characteristic() {
        markedWith(dayCount("zijn heffing groter is dan 3"), "RGS-092.day-count-condition-form",
                "zijn heffing groter is dan 3");
    }

    @Test void a_characteristic_that_is_not_time_dependent_is_refused_by_its_form() {
        markedWith(dayCount("hij vrijgesteld is"), "RGS-092.day-count-condition-form", "hij vrijgesteld is");
    }

    @Test void a_negated_characteristic_is_refused_by_its_form() {
        markedWith(dayCount("hij niet borgplichtig is"), "RGS-092.day-count-condition-form", "hij niet borgplichtig is");
    }

    @Test void a_characteristic_of_another_object_is_not_read_as_the_subjects_own() {
        markedWith(dayCount("zijn vaartocht borgplichtig is"), "RGS-092.day-count-condition-form",
                "zijn vaartocht borgplichtig is");
    }

    @Test void a_whole_period_qualifier_is_refused_by_its_form() {
        markedWith(dayCount("hij gedurende de gehele maand borgplichtig is"), "RGS-092.day-count-condition-form",
                "hij gedurende de gehele maand borgplichtig is");
    }

    @Test void a_compound_condition_is_refused_by_its_form() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel borgdagen van een jutter
                geldig altijd
                    De borgdagen van een Jutter moet berekend worden als het aantal dagen in de maand dat hij aan alle volgende voorwaarden voldoet:
                    - hij borgplichtig is
                    - hij aangesteld is.
            """);
        assertEquals(1, warnings(m).size(), m.diagnostics::toString);
        Msg reason = (Msg) warnings(m).get(0).args().get("reason");
        assertEquals("RGS-092.day-count-condition-form", reason.key(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"kind\":\"dayCount\",\"arguments\":[],\"filter\":{\"node\":\"Unresolved\","), c);
        Report.assertAccountedFor(IrEmitter.emit(m));
    }

    // ---------------- the whole-period qualifier in the condition of a gedurende clip ----------------

    private static RegelSpraakModel clip(String value) {
        return Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de wachtvergoeding per maand Numeriek (geheel getal) voor elke maand;
                het wachttarief Numeriek (getal met 2 decimalen) voor elk jaar;
                is wachtplichtig kenmerk (bijvoeglijk) voor elke maand;
                is aangesteld kenmerk (bijvoeglijk) voor elke maand;
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

            """ + value);
    }

    /** The clip keeps the marker the gedurende route writes for a condition it does not lower: the RGS-070 WARNING
     *  and an Unresolved filter that quotes the condition. */
    private static void clipMarked(RegelSpraakModel m, String condition) {
        assertTrue(m.diagnostics.stream().noneMatch(Diagnostic::isError), () -> m.diagnostics.toString());
        List<Diagnostic> w = warnings(m);
        assertEquals(1, w.size(), w::toString);
        assertEquals("RGS-070", w.get(0).code(), w::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"filter\":{\"node\":\"Unresolved\",\"source\":{\"snippet\":\""
                + condition.replaceAll("\\s+", "") + "\"}}"), c);
        Report.assertAccountedFor(IrEmitter.emit(m));
    }

    @Test void a_total_keeps_no_clip_whose_qualifier_it_drops() {
        clipMarked(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij gedurende het gehele jaar wachtplichtig is).
            """), "gedurende de tijd dat hij gedurende het gehele jaar wachtplichtig is");
    }

    @Test void a_pro_rata_part_keeps_no_clip_whose_qualifier_it_drops() {
        clipMarked(clip("""
            Regel wachtbijdrage van een jutter
                geldig altijd
                    De wachtbijdrage van een Jutter moet gesteld worden op het tijdsevenredig deel per maand van zijn wachttarief gedurende de tijd dat hij gedurende de gehele maand wachtplichtig is.
            """), "gedurende de tijd dat hij gedurende de gehele maand wachtplichtig is");
    }

    @Test void a_comparison_clip_keeps_no_qualifier_it_drops() {
        clipMarked(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn wachtvergoeding per maand gedurende het gehele jaar groter is dan 10).
            """), "gedurende de tijd dat zijn wachtvergoeding per maand gedurende het gehele jaar groter is dan 10");
    }

    @Test void a_compound_clip_keeps_no_member_whose_qualifier_it_drops() {
        clipMarked(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
                    - hij is gedurende het gehele jaar wachtplichtig
                    - hij niet aangesteld is).
            """), "gedurende de tijd dat hij aan alle volgende voorwaarden voldoet: - hij is gedurende het gehele jaar wachtplichtig - hij niet aangesteld is");
    }

    @Test void a_navigated_clip_keeps_no_qualifier_it_drops() {
        clipMarked(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn vaartocht gedurende het gehele jaar gestremd is).
            """), "gedurende de tijd dat zijn vaartocht gedurende het gehele jaar gestremd is");
    }

    @Test void a_static_member_of_a_compound_clip_keeps_no_qualifier_it_drops() {
        clipMarked(clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
                    - hij wachtplichtig is
                    - zijn leeftijd is gedurende het gehele jaar groter dan 18).
            """), "gedurende de tijd dat hij aan alle volgende voorwaarden voldoet: - hij wachtplichtig is - zijn leeftijd is gedurende het gehele jaar groter dan 18");
    }

    @Test void the_same_clips_without_qualifier_stay_lowered() {
        RegelSpraakModel m = clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn vaartocht gestremd is).

            Regel stilwachtsom van een jutter
                geldig altijd
                    De stilwachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij aan alle volgende voorwaarden voldoet:
                    - hij wachtplichtig is
                    - zijn leeftijd is groter dan 18).
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gestremd\","), c);
        assertTrue(c.contains("\"filter\":{\"node\":\"Compound\",\"quantifier\":\"all\","), c);
    }

    @Test void a_clip_without_qualifier_stays_lowered() {
        RegelSpraakModel m = clip("""
            Regel wachtsom van een jutter
                geldig altijd
                    De wachtsom van een Jutter moet gesteld worden op (het totaal van zijn wachtvergoeding per maand gedurende de tijd dat hij wachtplichtig is).
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"wachtplichtig\",\"negated\":false,\"source\""), c);
    }
}
