package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §5.6: a "die/dat" SUBSELECTION the resolution does not consume is refused, not ignored.
 *
 * <p>The sister of {@link SetReadWithoutFoldTest}, and the same shape of argument. A set reading
 * outside a fold source has no one-value meaning, so the set fence refuses it; a SUBSELECTION outside a fold
 * RANGE has no wire form — {@code filter} belongs to the instance fold (IR-SPEC ch. 4.3) and to the
 * quantifier fold (ch. 4.5) and nowhere else — so it is refused here.
 *
 * <p>What makes it an ERROR and not a WARNING: there is no reading of the emitted document under which
 * the omitted subselectie is harmless. "de leeftijd van de Jutter DIE BEVOEGD IS" names WHICH jutter
 * the sentence is about; dropping it answers a different question, verifier-clean, and every executor
 * then agrees on the wrong answer. A warning would leave that document on disk.
 *
 * <p>This makes the stillness loud; it does not build the semantics. What a subselection MEANS on a
 * scalar navigation is a spec question — a named extension point with a loud refusal, the posture the
 * counting quantors held on the quantifier fold until they were built (2026-09-16, WF-108 (a)).
 */
class SubselectionWithoutFoldTest {

    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            de leeftijd Numeriek (geheel getal);
            het vindersloon Numeriek (geheel getal);
            is bevoegd kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            het gewicht Numeriek (geheel getal);
            is waardevol kenmerk (bijvoeglijk);

        Feittype vondstaanmelding
            de aanmelder Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmelder heeft meerdere vondsten
        """;

    private static List<Diagnostic> refusal(RegelSpraakModel m) {
        return m.diagnostics.stream()
                .filter(d -> d.message().startsWith("de 'die/dat'-deelselectie '")).toList();
    }

    @Test void a_subselection_on_an_ordinary_reading_is_refused() {
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vindersloon van een jutter
                geldig altijd
                    Het vindersloon van een Jutter moet gesteld worden op 10
                    indien de leeftijd van de Jutter die bevoegd is groter is dan 5.
            """);
        assertEquals(1, refusal(m).size(), () -> "de bijzin hoort geweigerd te worden: " + m.diagnostics);
        assertEquals(Severity.ERROR, refusal(m).get(0).severity(),
                "een WAARSCHUWING zou het document dóórlaten, en dat document beantwoordt een andere vraag");
        assertTrue(refusal(m).get(0).message().contains("bevoegd is"),
                () -> "de weigering hoort de bijzin te citeren: " + m.diagnostics);
        assertFalse(m.errors().isEmpty(), "een fout blokkeert de emissie — dat is het hele punt");
    }

    @Test void the_fold_source_keeps_its_own_subselection() {
        // The regression pin: the instance fold CONSUMES its subselectie (§5.6, into FunctionCall.filter),
        // so the new refusal must not see it. Without this the fix would refuse every filtered
        // aggregation in the corpus.
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel vindersloon van een jutter
                geldig altijd
                    Het vindersloon van een Jutter moet berekend worden als
                    het aantal vondsten van de Jutter die waardevol zijn.
            """);
        assertTrue(refusal(m).isEmpty(), () -> "een verbruikte bijzin hoort niet geweigerd te worden: " + m.diagnostics);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
    }

    @Test void the_quantifier_fold_keeps_its_own_subselection() {
        // The second consumer, added by the same card: the §8.2 quantifier fold. Same pin, other route.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                het maatgevend gewicht Numeriek (geheel getal);

            Objecttype de Vondst (mv: Vondsten)
                het gewicht (mv: gewichten) Numeriek (geheel getal) voor elke maand;
                is waardevol kenmerk (bijvoeglijk);

            Feittype vondstaanmelding
                de aanmelder Jutter
                de vondst (mv: vondsten) Vondst
            Eén aanmelder heeft meerdere vondsten

            Regel maatgevend gewicht
                geldig altijd
                    Het maatgevend gewicht van een Jutter moet gesteld worden op 10
                    indien het gewicht van alle vondsten van de Jutter die waardevol zijn
                    gedurende het gehele jaar groter is dan 5.
            """);
        assertTrue(refusal(m).isEmpty(), () -> "een verbruikte bijzin hoort niet geweigerd te worden: " + m.diagnostics);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    @Test void a_refused_fold_subselection_is_not_reported_two_times() {
        // The quantifier fold's own refusal (the WARNING that names the axis) already accounts for the
        // bijzin, so this check must stay silent on it — one sentence, one refusal, and the precise one.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                het maatgevend gewicht Numeriek (geheel getal);
                is bevoegd kenmerk (bijvoeglijk);

            Objecttype de Vondst (mv: Vondsten)
                het gewicht (mv: gewichten) Numeriek (geheel getal) voor elke maand;

            Feittype vondstaanmelding
                de aanmelder Jutter
                de vondst (mv: vondsten) Vondst
            Eén aanmelder heeft meerdere vondsten

            Regel maatgevend gewicht
                geldig altijd
                    Het maatgevend gewicht van een Jutter moet gesteld worden op 10
                    indien het gewicht van alle vondsten van de Jutter die bevoegd zijn
                    gedurende het gehele jaar groter is dan 5.
            """);
        assertTrue(refusal(m).isEmpty(),
                () -> "de vouw meldde de bijzin al; een tweede melding zegt hetzelfde met een vagere reden: "
                        + m.diagnostics);
        assertEquals(1, m.diagnostics.stream().filter(d -> d.message().contains("deelselectie")).count(),
                () -> "precies één melding over de bijzin: " + m.diagnostics);
    }
}
