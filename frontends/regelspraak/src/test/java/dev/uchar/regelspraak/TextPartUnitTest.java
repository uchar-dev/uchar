package dev.uchar.regelspraak;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The unit of a text part (grammar/DIVERGENCES.md, "De eenheid van een tekstdeel"): {@code «X (eenheid)»} writes the
 * unit of the number X after it (RUNTIME-SEMANTICS ch. 1.2), the wire's {@code unit: true}. The suffix is one token with
 * its parentheses, so an attribute named eenheid stays readable, and a part whose type has no unit is refused at check
 * in the verifier's rule (WF-129, RGS-388).
 */
class TextPartUnitTest {

    private static final String MODEL = """
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Objecttype de Vondst (mv: Vondsten)
                de basis Numeriek (getal met 2 decimalen) met eenheid €;
                het aandeel Percentage (getal met 2 decimalen);
                het aantal Numeriek (geheel getal);
                de eenheid Numeriek (getal met 2 decimalen) met eenheid €;
                de bevestiging Boolean;
                de omschrijving Tekst;
            """;

    private static String rule(String text) {
        return MODEL + """

            Regel omschrijving
                geldig altijd
                    De omschrijving van een Vondst moet berekend worden als "%s".
            """.formatted(text);
    }

    /** The parts of the one rule's text composition, as (unit, snippet) pairs. */
    private static List<String> parts(RegelSpraakModel m) {
        ResolvedRule r = m.resolvedRules.get(0);
        Expr.TextComposition t = (Expr.TextComposition) r.value();
        return t.parts().stream().filter(p -> p instanceof Expr.TextComposition.EmbeddedExpression)
                .map(p -> (Expr.TextComposition.EmbeddedExpression) p)
                .map(i -> i.unit() + " " + i.expr().source().snippet()).toList();
    }

    @Test void the_suffix_marks_the_part_and_leaves_the_expression_whole() {
        RegelSpraakModel m = Resolver.resolve(rule(
                "«zijn basis (eenheid)», «zijn aandeel (eenheid)», «zijn basis maal 1,1 (eenheid)», «zijn basis»"));
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(List.of("true zijn basis", "true zijn aandeel", "true zijn basis maal 1,1", "false zijn basis"),
                parts(m));
    }

    @Test void an_attribute_named_eenheid_stays_readable() {
        RegelSpraakModel m = Resolver.resolve(rule("«zijn eenheid» en «zijn eenheid (eenheid)»"));
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(List.of("false zijn eenheid", "true zijn eenheid"), parts(m));
    }

    @Test void a_part_whose_type_has_no_unit_is_refused() {
        RegelSpraakModel m = Resolver.resolve(rule("«zijn aantal (eenheid)» en «zijn bevestiging (eenheid)»"));
        List<String> codes = m.diagnostics.stream().map(d -> d.code() + " " + d.message()).toList();
        assertEquals(List.of(
                "RGS-388 het tekstdeel «zijn aantal» schrijft de eenheid van zijn getal, en Numeriek heeft geen eenheid (WF-129)",
                "RGS-388 het tekstdeel «zijn bevestiging» schrijft de eenheid van zijn getal, en Boolean heeft geen eenheid (WF-129)"),
                codes);
    }
}
