package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A diagnostic stands at the line of what it is about, where the frontend knows that line: three of them stood at line
 * 0, which points an author at nothing and gives the multi-file report no file to name.
 */
class DiagnosticLineTest {

    private static Diagnostic only(RegelSpraakModel m, String code) {
        List<Diagnostic> ds = m.diagnostics.stream().filter(d -> code.equals(d.code())).toList();
        assertEquals(1, ds.size(), () -> "verwachtte één " + code + ", kreeg: " + m.diagnostics);
        return ds.get(0);
    }

    @Test void a_digit_count_below_one_stands_at_its_condition() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                het nummer Tekst;
                is gekeurd kenmerk (bijvoeglijk);

            Regel keuring
                geldig altijd
                    Een Vondst is gekeurd
                    indien zijn nummer numeriek is met exact 0 cijfers.
            """);
        assertEquals(8, only(m, "RGS-205").line());
    }

    @Test void a_role_check_negated_with_an_article_stands_at_its_condition() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                is beloond kenmerk (bijvoeglijk);

            Objecttype het Strandvak (mv: Strandvakken)
                de lengte Numeriek (geheel getal);

            Feittype vaktoewijzing
                de vakhouder Jutter
                het strandvak Strandvak
            Eén vakhouder heeft één strandvak

            Regel beloning
                geldig altijd
                    Een Jutter is beloond
                    indien hij niet een vakhouder is.
            """);
        assertEquals(15, only(m, "RGS-214").line());
    }

    @Test void an_undeclared_domain_stands_at_its_declaration() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);
                de soort Vondstsoort;

            Parameter de standaardsoort : Vondstsoort
            """);
        List<Diagnostic> ds = m.diagnostics.stream().filter(d -> "RGS-175".equals(d.code())).toList();
        assertEquals(List.of(3, 5), ds.stream().map(Diagnostic::line).toList(), m.diagnostics::toString);
    }
}
