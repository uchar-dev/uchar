package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A die/dat subselection with a whole-period qualifier on the operand of an aggregation ("het aantal posten van het
 * Kustvak die gedurende het gehele jaar bemand zijn"): the filter of the fold is the characteristic check of the
 * element with that qualifier (IR-SPEC ch. 4.5, RUNTIME-SEMANTICS ch. 3.9). A subselection is no clip, so the qualifier
 * is an ordinary field of its check (WF-097 fences the clips only); on a characteristic that is not time-dependent it
 * has no meaning (WF-043), and the frontend refuses it there in the verifier's words.
 */
class WholePeriodSubselectionTest {

    static final String MODEL = """
        Objecttype het Kustvak (mv: Kustvakken)
            het postental Numeriek (niet-negatief geheel getal);

        Objecttype de Post (mv: Posten)
            is bemand kenmerk (bijvoeglijk) voor elke maand;
            is bewaakt kenmerk (bijvoeglijk);

        Feittype postindeling
            het kustvak Kustvak
            de post (mv: posten) Post
        Eén kustvak omvat meerdere posten

        """;

    private static RegelSpraakModel count(String subselection) {
        return Resolver.resolve(MODEL + """
            Regel postental van een kustvak
                geldig altijd
                    Het postental van een Kustvak moet berekend worden als het aantal posten van het Kustvak die %s.
            """.formatted(subselection));
    }

    private static String filterOf(String period, boolean negated, String snippet) {
        return "\"elementType\":\"Post\",\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"bemand\","
                + "\"negated\":false,\"wholePeriod\":{\"node\":\"WholePeriod\",\"granularity\":\"" + period
                + "\",\"negated\":" + negated + "},\"source\":{\"snippet\":\"" + snippet.replaceAll("\\s+", "") + "\"}}";
    }

    @Test void the_whole_year_is_the_qualifier_of_the_element_check() {
        RegelSpraakModel m = count("gedurende het gehele jaar bemand zijn");
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains(filterOf("year", false, "gedurende het gehele jaar bemand zijn")), c);
    }

    @Test void the_whole_month_and_its_negation_are_carried_as_written() {
        RegelSpraakModel m = count("niet gedurende de gehele maand bemand zijn");
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains(filterOf("month", true, "niet gedurende de gehele maand bemand zijn")), c);
    }

    @Test void a_characteristic_that_is_not_time_dependent_takes_no_qualifier() {
        RegelSpraakModel m = count("gedurende het gehele jaar bewaakt zijn");
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.isError() && "RGS-207".equals(d.code())),
                m.diagnostics::toString);
    }

    /** The source of the golden jutterswet-aantalfilter-gehele-jaar: the count as the value of a rule and in the
     *  condition of another, each with the filter the golden carries. */
    @Test void the_source_of_the_golden_emits_its_filter_in_both_places() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype het Kustvak (mv: Kustvakken)
                het postental Numeriek (niet-negatief geheel getal);
                het waaktarief Numeriek (geheel getal);

            Objecttype de Post (mv: Posten)
                is bemand kenmerk (bijvoeglijk) voor elke maand;

            Feittype postindeling
                het kustvak Kustvak
                de post (mv: posten) Post
            Eén kustvak omvat meerdere posten

            Regel postental van een kustvak
                geldig altijd
                    Het postental van een Kustvak moet berekend worden als het aantal posten van het Kustvak die gedurende het gehele jaar bemand zijn.

            Regel waaktarief van een kustvak
                geldig altijd
                    Het waaktarief van een Kustvak moet gesteld worden op 10
                    indien het aantal posten van het Kustvak die gedurende het gehele jaar bemand zijn groter is dan 0.
            """);
        assertTrue(m.diagnostics.isEmpty(), m.diagnostics::toString);
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        String filter = filterOf("year", false, "gedurende het gehele jaar bemand zijn");
        assertEquals(2, c.split(java.util.regex.Pattern.quote(filter), -1).length - 1, c);
    }
}
