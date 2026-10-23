package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The matrix of a quantifier fold reads with the element as subject (RUNTIME-SEMANTICS ch. 3.9a), and a read in it
 * that names the element by its type binds as it does in a subselection and in a condition (grammar/DIVERGENCES.md
 * §13.4.1): "indien de vinddag van ten minste één van de vondsten van de Jutter later is dan de sluitdag van de
 * jutter van de Vondst". The operand beside the quantified one was built with the rule subject, from which the
 * element is a to-many step, so the chain was not walked and the read was refused as not laid as a navigation, where
 * "de sluitdag van zijn jutter" bound. Such an operand is now built again with the element as subject.
 */
class QuantifierMatrixElementNameTest {

    private static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de startdag Datum in dagen;
            de sluitdag Datum in dagen;
            de laatklasse Numeriek (geheel getal);
            het late gewicht Numeriek (geheel getal);

        Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;

        Feittype JutterHeeftVondst
            de jutter Jutter
            de vondst (mv: vondsten) Vondst
        Eén jutter heeft meerdere vondsten

        Regel sluitdag
            geldig altijd
                De sluitdag van een Jutter moet gesteld worden op zijn startdag.

        """;

    private static String laatklasse(String right) {
        return MODEL + "Regel laatklasse\n    geldig altijd\n        De laatklasse van een Jutter moet berekend worden als 1\n"
                + "        indien de vinddag van ten minste één van de vondsten van de Jutter later is dan " + right + ".\n";
    }

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.diagnostics.stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    private static Condition.QuantifierFold fold(RegelSpraakModel m) {
        ResolvedRule r = m.resolvedRules.stream().filter(x -> x.name().equals("laatklasse")).findFirst().orElseThrow();
        return assertInstanceOf(Condition.QuantifierFold.class, r.condition());
    }

    @Test void the_element_name_in_the_matrix_binds() {
        // before: ERROR "genavigeerde lezing 'de sluitdag van de jutter van de Vondst' is niet als navigatie gelegd:
        // 'sluitdag' staat op 'Jutter', de vuller van de rol 'jutter', maar zij is gelezen als het attribuut 'sluitdag
        // van de jutter' van 'Vondst': …"
        Condition.QuantifierFold k = fold(clean(laatklasse("de sluitdag van de jutter van de Vondst")));
        Condition.Comparison v = assertInstanceOf(Condition.Comparison.class, k.test());
        Expr.AttributeReference r = assertInstanceOf(Expr.AttributeReference.class, v.right());
        assertEquals("sluitdag", r.attribute());
        assertEquals("Jutter", r.objectType().name);
        assertEquals(List.of("jutter"), r.navigationPath());
    }

    /** Changed 2026-10-23 (card subselection-reads-subject): the two spellings wrote one document, both over the role
     *  'jutter' from the Vondst. They read the same object, but the possessive one names the rule subject's own role and
     *  is built at the rule subject, so it carries the form anchor (IR-SPEC ch. 4.1a), and the element-name one navigates
     *  from the element: the two spellings of one reading of RUNTIME-SEMANTICS ch. 3.4a. */
    @Test void the_possessive_form_carries_the_anchor_and_the_element_name_navigates() {
        String elementName = IrEmitter.emit(clean(laatklasse("de sluitdag van de jutter van de Vondst"))).replaceAll("\\s+", "");
        String possessive = IrEmitter.emit(clean(laatklasse("de sluitdag van zijn jutter"))).replaceAll("\\s+", "");
        assertTrue(elementName.contains("\"right\":{\"node\":\"AttributeReference\",\"attribute\":\"sluitdag\","
                + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"desluitdagvandejuttervandeVondst\"},"
                + "\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"jutter\"}]}"),
                elementName);
        assertTrue(possessive.contains("\"right\":{\"node\":\"AttributeReference\",\"attribute\":\"sluitdag\","
                + "\"subjectType\":\"Jutter\",\"anchor\":\"form\",\"source\":{\"snippet\":\"desluitdagvanzijnjutter\"}}"),
                possessive);
    }

    @Test void a_type_error_after_the_second_read_stays_loud() {
        // before: the same refusal as above; now the read binds and the comparison of a date with a number is the
        // typing ERROR of a condition
        RegelSpraakModel m = Resolver.resolve(laatklasse("het late gewicht van de jutter van de Vondst"));
        assertEquals(1, m.errors().size(), () -> m.errors().toString());
        assertTrue(m.errors().get(0).message().startsWith("voorwaarde: "), () -> m.errors().toString());
    }

    // ---- what stays as it was

    @Test void the_possessive_form_and_an_own_member_of_the_subject_stay() {
        Condition.Comparison v = assertInstanceOf(Condition.Comparison.class,
                fold(clean(laatklasse("de sluitdag van zijn jutter"))).test());
        assertEquals("sluitdag", assertInstanceOf(Expr.AttributeReference.class, v.right()).attribute());
        fold(clean(laatklasse("zijn startdag")));
    }

    @Test void the_same_read_in_a_subselection_binds() {
        RegelSpraakModel m = clean(MODEL.replace("    de laatklasse Numeriek (geheel getal);\n", "    de laatklasse Numeriek "
                + "(geheel getal);\n    de eerste late vinddag Datum in dagen;\n") + "Regel eerste late vinddag\n    geldig "
                + "altijd\n        De eerste late vinddag van een Jutter moet berekend worden als de eerste van de vinddag van "
                + "alle vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn vinddag is later dan de "
                + "sluitdag van de jutter van de Vondst.\n");
        assertNotNull(m);
    }
}
