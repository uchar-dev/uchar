package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tabel 16 (§8.1.8.3) negates a nominal characteristic with "is niet een &lt;k&gt;", and in the question order of a
 * condition that is "indien &lt;onderwerp&gt; niet een &lt;k&gt; is" (card nominal-negation-in-question-order). It lowers to
 * the negated characteristic check, the same test as "geen &lt;k&gt;": for the subject itself, for "hij" and for a
 * navigation to one. After "niet een" a characteristic of another kind and a name that binds nothing stay refused
 * loudly.
 *
 * <p><b>Red before the card</b> (measured 2026-11-06 on {@code 890519e8d}): "indien de Vondst niet een wrakstuk is"
 * left a marker with the WARNING "onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp",
 * since the parse put "niet" in the subject and the EEN-form looked for the name "niet wrakstuk"; "indien hij niet een
 * wrakstuk is" was a syntax error ("no viable alternative at input 'nieteen'").
 */
class NominalNegationInQuestionOrderTest {

    static final String MODEL = """
        Objecttype de Vondst (mv: Vondsten)
            is zwaar kenmerk (bijvoeglijk);
            is gemeld kenmerk (bijvoeglijk);
            het wrakstuk kenmerk;
        Objecttype de Melding (mv: Meldingen)
            is dringend kenmerk (bijvoeglijk);
        Feittype melding van een vondst
            de gemelde vondst Vondst
            de melding (mv: meldingen) Melding
        Eén gemelde vondst heeft meerdere meldingen
        """;

    private static final String NEGATED = "\"node\":\"CharacteristicCheck\",\"characteristic\":\"wrakstuk\",\"negated\":true";

    @Test void niet_een_in_the_question_order_is_the_negated_characteristic() {
        String[][] forms = {
            {"Vondst is gemeld", "de Vondst niet een wrakstuk is", "de Vondst geen wrakstuk is"},
            {"Vondst is gemeld", "hij niet een wrakstuk is", "hij geen wrakstuk is"},
            {"Melding is dringend", "zijn gemelde vondst niet een wrakstuk is", "zijn gemelde vondst geen wrakstuk is"},
        };
        for (String[] f : forms) {
            String niet = condition(clean(rule(f[0], f[1])));
            assertTrue(niet.contains(NEGATED), () -> f[1] + ":\n" + niet);
            // the same test as "geen <k>", up to the source text
            assertEquals(withoutSources(condition(clean(rule(f[0], f[2])))), withoutSources(niet), f[1]);
        }
    }

    @Test void after_niet_een_another_kind_and_an_unbound_name_stay_refused() {
        for (String c : new String[] {"hij niet een zwaar is", "de Vondst niet een zwaar is", "hij niet een drijfhout is"}) {
            RegelSpraakModel m = Resolver.resolve(MODEL + rule("Vondst is gemeld", c));
            assertFalse(m.diagnostics.isEmpty(), () -> c + ": geweigerd met een melding");
            // an ERROR writes no document; a WARNING leaves the condition as a marker on the wire
            if (m.diagnostics.stream().anyMatch(d -> d.severity() == RegelSpraakModel.Severity.ERROR))
                continue;
            String ir = IrEmitter.emit(m).replaceAll("\\s+", "");
            assertTrue(condition(ir).contains("\"node\":\"Unresolved\""), () -> c + ":\n" + ir);
        }
    }

    private static String clean(String rule) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rule);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek bij\n" + rule + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static String rule(String subject, String condition) {
        return "Regel t\n    geldig altijd\n        Een " + subject + "\n        indien " + condition + ".\n";
    }

    /** The condition of the rule, as the document writes it: the key and its object, up to the brace that closes it. */
    private static String condition(String ir) {
        int at = ir.indexOf("\"condition\":");
        assertTrue(at >= 0, ir);
        int depth = 0;
        for (int i = ir.indexOf('{', at); i < ir.length(); i++) {
            if (ir.charAt(i) == '{') depth++;
            else if (ir.charAt(i) == '}' && --depth == 0) return ir.substring(at, i + 1);
        }
        throw new AssertionError("no closing brace after the condition: " + ir);
    }

    private static String withoutSources(String json) {
        return json.replaceAll("\"source\":\\{[^{}]*\\}", "");
    }
}
