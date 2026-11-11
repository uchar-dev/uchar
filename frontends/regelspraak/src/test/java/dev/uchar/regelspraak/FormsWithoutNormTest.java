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
 * Three RegelSpraak forms have no norm in the IR: the eleven test, the hour, minute, second and millisecond of a
 * date-time, and "regelversie X is gevuurd". The wire has no node or value for them, so each sentence reaches the wire
 * as an Unresolved marker with its RGS-092 diagnostic (WF-113), the mechanism for every form the IR does not carry.
 * "Regelversie X is inconsistent" is normed (RUNTIME-SEMANTICS ch. 5.6) and stays a {@code Regelversie} node.
 */
class FormsWithoutNormTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de naam Tekst;
            het vondstmoment Datum en tijd in millisecondes;
            de uitkomst Numeriek (geheel getal);
            is waardevol kenmerk (bijvoeglijk);

        Regel controle uitkomst
            geldig altijd
                De uitkomst van een Jutter moet groter zijn dan 0.

        """;

    /** The one WARNING of the model of {@code rules}: RGS-092 with {@code reasonKey}, quoting {@code snippet}; the
     *  emitted document carries a marker with that snippet, no {@code forbidden} text, and accounts for the marker. */
    private static String marked(String rules, String reasonKey, String snippet, String forbidden) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rules);
        assertTrue(m.diagnostics.stream().noneMatch(Diagnostic::isError), () -> m.diagnostics.toString());
        List<Diagnostic> w = m.diagnostics.stream().filter(d -> d.severity() == Severity.WARNING).toList();
        assertEquals(1, w.size(), w::toString);
        Diagnostic d = w.get(0);
        assertEquals("RGS-092", d.code(), d::toString);
        assertEquals(reasonKey, ((Msg) d.args().get("reason")).key(), d::toString);
        assertTrue(d.message().contains("'" + snippet + "'"), d::message);
        String ir = IrEmitter.emit(m);
        String compact = ir.replaceAll("\\s+", "");
        assertTrue(compact.contains("{\"node\":\"Unresolved\",\"source\":{\"snippet\":\""
                + snippet.replaceAll("\\s+", "") + "\"}}"), compact);
        assertFalse(ir.contains(forbidden), ir);
        Report.assertAccountedFor(ir);
        return ir;
    }

    @Test void the_hour_of_a_date_time_is_a_marker() {
        marked("""
            Regel uur van de vondst
                geldig altijd
                    De uitkomst van een Jutter moet gesteld worden op het uur uit zijn vondstmoment.
            """, "RGS-092.time-of-day-part", "het uur uit zijn vondstmoment", "hourOf");
    }

    @Test void the_millisecond_of_a_date_time_is_a_marker() {
        marked("""
            Regel milliseconde van de vondst
                geldig altijd
                    De uitkomst van een Jutter moet gesteld worden op de milliseconde uit zijn vondstmoment.
            """, "RGS-092.time-of-day-part", "de milliseconde uit zijn vondstmoment", "millisecondOf");
    }

    @Test void the_eleven_test_is_a_marker() {
        marked("""
            Regel waardevolle naam
                geldig altijd
                    Een Jutter is waardevol indien zijn naam aan de elfproef voldoet.
            """, "RGS-092.eleven-test", "zijn naam aan de elfproef voldoet", "elevenTest");
    }

    @Test void a_fired_rule_version_is_a_marker() {
        marked("""
            Regel waardevolle uitkomst
                geldig altijd
                    Een Jutter is waardevol indien regelversie controle uitkomst is gevuurd.
            """, "RGS-092.rule-fired", "regelversie controle uitkomst is gevuurd", "Regelversie");
    }

    @Test void an_inconsistent_rule_version_stays_on_the_wire() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel waardevolle uitkomst
                geldig altijd
                    Een Jutter is waardevol indien regelversie controle uitkomst is inconsistent.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> m.diagnostics.toString());
        String compact = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(compact.contains("{\"node\":\"Regelversie\",\"regel\":\"controleuitkomst\",\"inconsistent\":true,"), compact);
    }
}
