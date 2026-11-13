package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The anchor of a range of one written step ("alle vondsten van &lt;anker&gt;") in a value fold, a quantifier over a
 * comparison and a quantifier over a characteristic check. The range of one step is one relation from the rule subject
 * (IR-SPEC ch. 4.3, ch. 4.5). An anchor that is a role from the subject ("zijn meester", "de meester") is a step: the
 * range runs through it, as "alle vondsten van de meester van de Jutter" writes it. An anchor that names another object
 * type ("de Kade", "een Kade") is refused. Before: all of them ranged over the subject's own relation, without a word,
 * and every executor folded the subject's own vondsten.
 */
class OneStepRangeAnchorTest {

    static final String MODEL = """
        Objecttype de Kade (mv: Kades)
            de diepte Numeriek (geheel getal);

        Objecttype de Jutter (mv: Jutters) (bezield)
            het cijfer Numeriek (geheel getal);
            is rijk kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);
            is zwaar kenmerk (bijvoeglijk);

        Feittype vinderschap
            de vinder Jutter
            de vondst (mv: vondsten) Vondst
        Eén vinder heeft meerdere vondsten

        Feittype leerschap
            de meester Jutter
            de leerling (mv: leerlingen) Jutter
        Eén meester heeft meerdere leerlingen
        """;

    static final String THROUGH_MEESTER = "\"sourcePath\":[{\"node\":\"RoleBinding\",\"factType\":\"leerschap\",\"role\":"
            + "\"meester\"},{\"node\":\"RoleBinding\",\"factType\":\"vinderschap\",\"role\":\"vondst\"}]";

    private static String value(String expression) {
        return MODEL + "\nRegel r\n    geldig altijd\n        Het cijfer van een Jutter moet berekend worden als "
                + expression + ".\n";
    }

    private static String condition(String condition) {
        return MODEL + "\nRegel r\n    geldig altijd\n        Een Jutter is rijk\n        indien " + condition + ".\n";
    }

    private static String emitCompact(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static void throughMeester(String source) {
        String ir = emitCompact(source);
        assertTrue(ir.contains(THROUGH_MEESTER), () -> "het bereik loopt via de meester:\n" + ir);
        assertFalse(ir.contains("\"sourceRole\""), () -> "geen bereik vanaf het onderwerp zelf:\n" + ir);
    }

    @Test void a_value_fold_runs_through_a_role_anchor() {
        throughMeester(value("de som van de waarde van alle vondsten van zijn meester"));
        throughMeester(value("de maximale waarde van de waarde van alle vondsten van de meester"));
    }

    @Test void a_quantifier_over_a_characteristic_check_runs_through_a_role_anchor() {
        throughMeester(condition("alle vondsten van zijn meester zwaar zijn"));
        String negated = emitCompact(condition("alle vondsten van de meester niet zwaar zijn"));
        assertTrue(negated.contains(THROUGH_MEESTER), negated);
        assertTrue(negated.contains("\"characteristic\":\"zwaar\",\"negated\":true"), () -> "de ontkenning blijft:\n" + negated);
    }

    @Test void a_quantifier_over_a_comparison_runs_through_a_role_anchor() {
        throughMeester(condition("de waarde van alle vondsten van zijn meester groter is dan 10"));
        throughMeester(condition("ten minste twee van de vondsten van zijn meester zwaar zijn"));
    }

    /** "de vinder" on a Jutter is the Jutter itself (zero far hits, the Jutter fills the role): one step, as before. */
    @Test void a_self_denoting_anchor_stays_one_step() {
        String ir = emitCompact(condition("alle vondsten van de vinder zwaar zijn"));
        assertTrue(ir.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"vinderschap\",\"role\":\"vondst\"}"), ir);
        assertFalse(ir.contains("\"sourcePath\""), ir);
    }

    /** An anchor that names another object type than the subject's, reached by no role: refused at each place. */
    @Test void a_type_anchor_is_refused() {
        for (String anchor : List.of("de Kade", "een Kade")) {
            assertRefused(value("de som van de waarde van alle vondsten van " + anchor), "RGS-075");
            assertRefused(condition("alle vondsten van " + anchor + " zwaar zijn"), "RGS-213");
            assertRefused(condition("de waarde van alle vondsten van " + anchor + " groter is dan 10"), "RGS-213");
        }
    }

    private static void assertRefused(String source, String code) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.code().equals(code) && d.message().contains("Kade")),
                () -> "verwachtte " + code + " die het anker noemt, kreeg: " + m.diagnostics);
        assertTrue(m.isOk(), () -> "een WARNING, geen ERROR: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        assertFalse(ir.contains("\"sourceRole\""), () -> "geen bereik vanaf het onderwerp zelf:\n" + ir);
        assertTrue(ir.contains("\"node\": \"Unresolved\""), () -> "een marker op de wire:\n" + ir);
        Report.assertAccountedFor(ir);
    }
}
