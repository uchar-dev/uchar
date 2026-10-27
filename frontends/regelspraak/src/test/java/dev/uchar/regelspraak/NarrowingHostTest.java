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
 * The narrowing host (IR-SPEC ch. 4.6 (q2), RUNTIME-SEMANTICS ch. 5.8b rule 4; grammar/DIVERGENCES.md, "A characteristic
 * of a subtype — the narrowing host"): a written name that binds no characteristic and no role of the subject binds the
 * characteristic that exactly one type strictly below the subject declares, and the check names that type as its
 * {@code host}. Two declarants below the subject leave a marker that names them; none leaves the marker it was.
 */
class NarrowingHostTest {

    /** The model of the golden jutterswet-ploegblokkade: {@code vaartijd} on Wrakrecht, two steps below the Strandrecht
     *  a ploeg's rechten are. */
    static final String MODEL = """
        Objecttype het Strandrecht (mv: Strandrechten)
            het loon Numeriek (getal met 2 decimalen);
            is zeewaardig kenmerk (bijvoeglijk);

        Objecttype het Jutrecht is een Strandrecht (mv: Jutrechten)
            het hoogste vaarloon Numeriek (getal met 2 decimalen);
            is ploegvarend kenmerk (bijvoeglijk);

        Objecttype het Wrakrecht is een Jutrecht (mv: Wrakrechten)
            de vaartijd kenmerk (bezittelijk);
            is zeevast kenmerk (bijvoeglijk);

        Objecttype de Ploeg (mv: Ploegen)
            het aantal zeevaste rechten Numeriek (geheel getal);

        Feittype PloegBetreftStrandrecht
            de ploeg Ploeg
            het recht (mv: rechten) Strandrecht
        Eén ploeg betreft meerdere rechten
        """;

    private static RegelSpraakModel resolve(String model, String rules) {
        return Resolver.resolve(model + rules);
    }

    private static String compact(RegelSpraakModel m) {
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static List<Diagnostic> warnings(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.severity() == Severity.WARNING).toList();
    }

    private static void noErrors(RegelSpraakModel m) {
        assertTrue(m.diagnostics.stream().noneMatch(d -> d.severity() == Severity.ERROR), () -> m.diagnostics.toString());
    }

    @Test
    void a_fold_filter_binds_the_characteristic_of_a_subtype_with_its_host() {
        RegelSpraakModel m = resolve(MODEL, """
            Regel hoogste vaarloon
                geldig altijd
                    Het hoogste vaarloon van een Jutrecht moet berekend worden als de maximale waarde van het loon van alle rechten van de ploeg van het Jutrecht die aan alle volgende voorwaarden voldoen:
                    - hij is niet het Jutrecht zelf
                    - hij heeft de vaartijd.
            """);
        noErrors(m);
        assertEquals(List.of(), warnings(m));
        String c = compact(m);
        assertTrue(c.contains("{\"node\":\"CharacteristicCheck\",\"characteristic\":\"vaartijd\",\"negated\":false,"
                + "\"host\":\"Wrakrecht\",\"source\":{\"snippet\":\"hijheeftdevaartijd\"}}"), c);
        assertFalse(c.contains("\"Unresolved\""), c);
    }

    @Test
    void the_negated_check_and_the_type_name_bind_the_same_host() {
        RegelSpraakModel m = resolve(MODEL, """
            Regel hoogste vaarloon
                geldig altijd
                    Het hoogste vaarloon van een Jutrecht moet berekend worden als de maximale waarde van het loon van alle rechten van de ploeg van het Jutrecht die aan alle volgende voorwaarden voldoen:
                    - het Strandrecht heeft geen vaartijd
                    - hij is niet het Jutrecht zelf.
            """);
        noErrors(m);
        assertEquals(List.of(), warnings(m));
        assertTrue(compact(m).contains("\"characteristic\":\"vaartijd\",\"negated\":true,\"host\":\"Wrakrecht\""), compact(m));
    }

    @Test
    void a_rule_condition_on_the_supertype_binds_the_host() {
        RegelSpraakModel m = resolve(MODEL, """
            Regel zeewaardig
                geldig altijd
                    Een Strandrecht is zeewaardig
                    indien hij de vaartijd heeft.
            """);
        noErrors(m);
        assertEquals(List.of(), warnings(m));
        assertTrue(compact(m).contains("\"condition\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"vaartijd\","
                + "\"negated\":false,\"host\":\"Wrakrecht\""), compact(m));
    }

    @Test
    void a_quantifier_test_binds_the_host() {
        RegelSpraakModel m = resolve(MODEL, """
            Regel ploegvarend
                geldig altijd
                    Een Jutrecht is ploegvarend
                    indien ten minste één van de rechten van de ploeg van het Jutrecht de vaartijd heeft.
            """);
        noErrors(m);
        assertEquals(List.of(), warnings(m));
        String c = compact(m);
        assertTrue(c.contains("\"elementType\":\"Strandrecht\",\"test\":{\"node\":\"CharacteristicCheck\","
                + "\"characteristic\":\"vaartijd\",\"negated\":false,\"host\":\"Wrakrecht\""), c);
    }

    @Test
    void a_die_dat_subselection_binds_the_host() {
        RegelSpraakModel m = resolve(MODEL, """
            Regel zeevaste rechten
                geldig altijd
                    Het aantal zeevaste rechten van een Ploeg moet berekend worden als het aantal rechten van de Ploeg die zeevast zijn.
            """);
        noErrors(m);
        assertEquals(List.of(), warnings(m));
        assertTrue(compact(m).contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"zeevast\","
                + "\"negated\":false,\"host\":\"Wrakrecht\""), compact(m));
    }

    @Test
    void a_characteristic_of_the_chain_keeps_its_check_without_a_host() {
        RegelSpraakModel m = resolve(MODEL, """
            Regel zeewaardig
                geldig altijd
                    Een Wrakrecht is zeewaardig
                    indien hij de vaartijd heeft.
            """);
        noErrors(m);
        assertTrue(compact(m).contains("\"condition\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"vaartijd\","
                + "\"negated\":false,\"source\""), compact(m));
    }

    @Test
    void two_declarants_below_the_subject_leave_a_marker_that_names_them() {
        String siblings = MODEL.replace("""
                Objecttype de Ploeg (mv: Ploegen)""", """
                Objecttype het Kaprecht is een Strandrecht (mv: Kaprechten)
                    de vaartijd kenmerk (bezittelijk);

                Objecttype de Ploeg (mv: Ploegen)""");
        RegelSpraakModel m = resolve(siblings, """
            Regel zeewaardig
                geldig altijd
                    Een Strandrecht is zeewaardig
                    indien hij de vaartijd heeft.
            """);
        noErrors(m);
        assertTrue(compact(m).contains("\"condition\":{\"node\":\"Unresolved\""), compact(m));
        List<Diagnostic> w = warnings(m);
        assertEquals(1, w.size(), w::toString);
        Diagnostic d = w.get(0);
        assertEquals("RGS-092", d.code());
        Msg reason = (Msg) d.args().get("reason");
        assertEquals("RGS-092.characteristic-on-sibling-subtypes", reason.key());
        assertEquals("vaartijd", String.valueOf(reason.args().get("name")));
        assertEquals("Strandrecht", String.valueOf(reason.args().get("subject")));
        assertTrue(d.message().contains("'Wrakrecht'") && d.message().contains("'Kaprecht'"), d.message());
        assertTrue(d.message().contains("'hij de vaartijd heeft' blijft als marker op de wire staan"), d.message());
    }

    @Test
    void no_declarant_below_the_subject_stays_the_marker_it_was() {
        RegelSpraakModel m = resolve(MODEL, """
            Regel zeewaardig
                geldig altijd
                    Een Strandrecht is zeewaardig
                    indien hij de zeegang heeft.
            """);
        List<Diagnostic> w = warnings(m);
        assertEquals(1, w.size(), w::toString);
        assertEquals("RGS-092", w.get(0).code());
        assertEquals("RGS-092.no-characteristic-no-role", ((Msg) w.get(0).args().get("reason")).key());
    }

    @Test
    void a_characteristic_of_a_type_outside_the_chain_below_is_not_bound() {
        // Ploeg is no subtype of Strandrecht, so a characteristic it declares stays unbound on a Strandrecht
        String other = MODEL.replace("het aantal zeevaste rechten Numeriek (geheel getal);",
                "het aantal zeevaste rechten Numeriek (geheel getal);\n    de zeegang kenmerk (bezittelijk);");
        RegelSpraakModel m = resolve(other, """
            Regel zeewaardig
                geldig altijd
                    Een Strandrecht is zeewaardig
                    indien hij de zeegang heeft.
            """);
        assertEquals("RGS-092.no-characteristic-no-role", ((Msg) warnings(m).get(0).args().get("reason")).key());
    }
}
