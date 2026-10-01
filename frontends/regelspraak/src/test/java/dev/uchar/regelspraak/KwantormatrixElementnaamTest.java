package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRegel;
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
class KwantormatrixElementnaamTest {

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

    private static String laatklasse(String rechts) {
        return MODEL + "Regel laatklasse\n    geldig altijd\n        De laatklasse van een Jutter moet berekend worden als 1\n"
                + "        indien de vinddag van ten minste één van de vondsten van de Jutter later is dan " + rechts + ".\n";
    }

    private static RegelSpraakModel schoon(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.diagnostics.stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    private static Conditie.Kwantorvouw vouw(RegelSpraakModel m) {
        ResolvedRegel r = m.resolvedRegels.stream().filter(x -> x.naam().equals("laatklasse")).findFirst().orElseThrow();
        return assertInstanceOf(Conditie.Kwantorvouw.class, r.voorwaarde());
    }

    @Test void de_elementnaam_in_de_matrix_bindt() {
        // before: ERROR "genavigeerde lezing 'de sluitdag van de jutter van de Vondst' is niet als navigatie gelegd:
        // 'sluitdag' staat op 'Jutter', de vuller van de rol 'jutter', maar zij is gelezen als het attribuut 'sluitdag
        // van de jutter' van 'Vondst': …"
        Conditie.Kwantorvouw k = vouw(schoon(laatklasse("de sluitdag van de jutter van de Vondst")));
        Conditie.Vergelijking v = assertInstanceOf(Conditie.Vergelijking.class, k.toets());
        Expr.AttribuutRef r = assertInstanceOf(Expr.AttribuutRef.class, v.rechts());
        assertEquals("sluitdag", r.attribuut());
        assertEquals("Jutter", r.objecttype().naam);
        assertEquals(List.of("jutter"), r.navigatiePad());
    }

    @Test void de_wire_is_die_van_de_bezittelijke_vorm() {
        // the two spellings name the same role from the element, so they write one document but for the source text
        String elementnaam = IrEmitter.emit(schoon(laatklasse("de sluitdag van de jutter van de Vondst")));
        String bezittelijk = IrEmitter.emit(schoon(laatklasse("de sluitdag van zijn jutter")));
        assertEquals(zonderBron(bezittelijk), zonderBron(elementnaam));
    }

    @Test void een_typefout_na_de_tweede_lezing_blijft_luid() {
        // before: the same refusal as above; now the read binds and the comparison of a date with a number is the
        // typing ERROR of a condition
        RegelSpraakModel m = Resolver.resolve(laatklasse("het late gewicht van de jutter van de Vondst"));
        assertEquals(1, m.errors().size(), () -> m.errors().toString());
        assertTrue(m.errors().get(0).message().startsWith("voorwaarde: "), () -> m.errors().toString());
    }

    // ---- what stays as it was

    @Test void de_bezittelijke_vorm_en_een_eigen_lid_van_het_onderwerp_blijven() {
        Conditie.Vergelijking v = assertInstanceOf(Conditie.Vergelijking.class,
                vouw(schoon(laatklasse("de sluitdag van zijn jutter"))).toets());
        assertEquals("sluitdag", assertInstanceOf(Expr.AttribuutRef.class, v.rechts()).attribuut());
        vouw(schoon(laatklasse("zijn startdag")));
    }

    @Test void dezelfde_lezing_in_een_subselectie_bindt() {
        RegelSpraakModel m = schoon(MODEL.replace("    de laatklasse Numeriek (geheel getal);\n", "    de laatklasse Numeriek "
                + "(geheel getal);\n    de eerste late vinddag Datum in dagen;\n") + "Regel eerste late vinddag\n    geldig "
                + "altijd\n        De eerste late vinddag van een Jutter moet berekend worden als de eerste van de vinddag van "
                + "alle vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn vinddag is later dan de "
                + "sluitdag van de jutter van de Vondst.\n");
        assertNotNull(m);
    }

    private static String zonderBron(String json) {
        return json.replaceAll("\"source\": \\{[^}]*\\}", "").replaceAll("\\s+", "");
    }
}
