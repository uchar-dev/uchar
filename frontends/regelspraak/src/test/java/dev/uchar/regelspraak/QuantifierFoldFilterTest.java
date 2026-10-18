package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §5.6 / IR-SPEC ch. 4.5: the "die/dat" SUBSELECTION on a §8.2 quantifier fold — the {@code filter}
 * field, evaluated per contributor with the element as subject.
 *
 * <p>What this file pins is a correctness boundary, not a missing feature. Before this card the
 * frontend PARSED the subselectie (it shows up in the operand's {@code bron}), lowered the sentence to
 * a {@link dev.uchar.regelspraak.ir.Condition.QuantifierFold} — and wrote {@code filter: null}
 * with no diagnostic at all. A sentence that quantifies over the AANGEMELDE vondsten produced a
 * document that quantifies over ALL of them: a verifier-clean wire form carrying the wrong set, which
 * every executor then answers consistently and wrongly.
 *
 * <p>So the rule this file holds is: the subselection reaches the wire, or the document does not. A
 * shape this lowering cannot bind keeps the set fence's ERROR and draws a WARNING naming itself — never a
 * fold over the FULL range. That is the same discipline the counting quantor and the unbindable range
 * already carry in {@link QuantifierFoldTest}.
 *
 * <p>The subselection binds against the ELEMENT type, exactly as the instance fold's does (§5.6): it
 * is written after the onderwerp ("… van alle vondsten van de Jutter die aangemeld zijn") but it
 * selects the quantified MEMBERS, so a name that is a kenmerk of the SUBJECT and not of the element
 * binds nothing.
 */
class QuantifierFoldFilterTest {

    /** One Jutter to many Vondsten, the Vondst's gewicht on a §13.3.6 month timeline (§8.2 needs a
     *  tijdsafhankelijk onderwerp). {@code aangemeld} is a kenmerk of the ELEMENT, {@code bevoegd} one
     *  of the SUBJECT — the pair the element-binding test needs. */
    static final String MODEL = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Objecttype de Jutter (mv: Jutters) (bezield)
            het maatgevend gewicht Numeriek (getal met 2 decimalen) met eenheid kg;
            is bevoegd kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen) met eenheid kg voor elke maand;
            is aangemeld kenmerk (bijvoeglijk);
            is geborgen kenmerk (bijvoeglijk);

        Feittype vondstaanmelding
            de aanmelder Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmelder heeft meerdere vondsten
        """;

    /** The §8.2 sentence, with {@code %s} for the written quantifier and {@code %s} for the
     *  subselection tail (the empty string when there is none). */
    static final String SENTENCE = """
        Regel maatgevend gewicht
            geldig altijd
                Het maatgevend gewicht van een Jutter moet gesteld worden op 10 kg
                indien het gewicht van %s vondsten van de Jutter%s gedurende het gehele jaar groter is dan 5 kg.
        """;

    private static RegelSpraakModel resolve(String quantifier, String subselection) {
        return Resolver.resolve(MODEL + SENTENCE.formatted(quantifier, subselection));
    }

    /** Sets the model's ERRORs aside ({@link Emission}): several rows read the node a REFUSED sentence built. */
    private static String emitCompact(RegelSpraakModel m) {
        return Emission.withoutErrors(m).replaceAll("\\s+", "");
    }

    /** The emitted condition of the single rule's version, compacted. */
    private static String condition(RegelSpraakModel m) {
        String c = emitCompact(m);
        int van = c.indexOf("\"condition\":");
        assertTrue(van >= 0, () -> "geen voorwaarde in de emissie:\n" + c);
        return c.substring(van);
    }

    /** The set fence's refusal, matched on its own sentence (the declining WARNINGs cite the fence's rule too). */
    private static List<Diagnostic> setReadErrors(RegelSpraakModel m) {
        return m.diagnostics.stream()
                .filter(d -> d.message().startsWith("setlezing buiten een vouwbron")).toList();
    }

    private static List<Diagnostic> subselectionWarning(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.message().contains("deelselectie")).toList();
    }

    @Test void the_characteristic_subselection_becomes_the_filter_on_the_wire() {
        RegelSpraakModel m = resolve("alle", " die aangemeld zijn");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String v = condition(m);
        assertTrue(v.contains("\"node\":\"QuantifierFold\""), () -> "geen kwantorvouw:\n" + v);
        assertTrue(v.contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"aangemeld\""),
                () -> "de die/dat-deelselectie hoort als filter op de wire te staan:\n" + v);
        assertTrue(v.contains("\"elementType\":\"Vondst\",\"filter\":{"),
                () -> "een geschreven deelselectie mag nooit als een vol bereik landen:\n" + v);
    }

    @Test void the_none_variant_carries_the_same_subselection() {
        // The filter is the RANGE's, not the quantifier's: GEEN quantifies over the same selected
        // members. A lowering that carried it under one quantor only would read "geen van de
        // aangemelde" as "geen van de".
        RegelSpraakModel m = resolve("geen van de", " die aangemeld zijn");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String v = condition(m);
        assertTrue(v.contains("\"quantifier\":\"none\""), () -> "verkeerde kwantor:\n" + v);
        assertTrue(v.contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"aangemeld\""),
                () -> "ook onder GEEN hoort de deelselectie op de wire te staan:\n" + v);
    }

    @Test void the_filter_reads_the_characteristic_of_the_ELEMENT_without_navigation() {
        // ch. 4.5: the filter is evaluated PER CONTRIBUTOR with the element as subject. So it carries
        // no navigation — a navigated read would be a hop from the Jutter inside a condition whose
        // subject is the Vondst, the same trap the toets avoids.
        String v = condition(resolve("alle", " die aangemeld zijn"));
        int i = v.indexOf("\"filter\":");
        String f = v.substring(i, v.indexOf("\"test\":", i));
        assertTrue(f.contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"aangemeld\",\"negated\":false,"),
                () -> "een bevestigende deelselectie:\n" + f);
        assertFalse(f.contains("\"navigation\""), () -> "het filter navigeert niet:\n" + f);
    }

    @Test void a_subselection_over_a_characteristic_of_the_subject_not_binds() {
        // §5.6 binds the subselection against the ELEMENT. "bevoegd" is a kenmerk of the Jutter and
        // not of the Vondst, so there is nothing to select on — and the lowering must say so rather
        // than reach for the subject's kenmerk (which would quantify over a set nobody wrote).
        RegelSpraakModel m = resolve("alle", " die bevoegd zijn");
        assertEquals(1, setReadErrors(m).size(), () -> "de setlezing hoort te blijven staan: " + m.diagnostics);
        assertEquals(Severity.ERROR, setReadErrors(m).get(0).severity());
        assertEquals(1, subselectionWarning(m).size(),
                () -> "de geweigerde deelselectie hoort benoemd te worden: " + m.diagnostics);
        assertEquals(Severity.WARNING, subselectionWarning(m).get(0).severity());
        assertFalse(emitCompact(m).contains("QuantifierFold"),
                "een onherleidbare deelselectie mag nooit als vouw over het VOLLE bereik landen");
    }

    @Test void a_possession_predicate_as_subselection_falls_loudly() {
        // "die een kind hebben" is a possession/role query, not a kenmerk of the element — the
        // §13.4.13.5-.8 class the instance fold's filter also declines. The loud refusal is the
        // point: silently dropping it folds over every vondst.
        RegelSpraakModel m = resolve("alle", " die een kind hebben");
        assertEquals(1, setReadErrors(m).size(), () -> "de setlezing hoort te blijven staan: " + m.diagnostics);
        assertEquals(1, subselectionWarning(m).size(),
                () -> "de geweigerde deelselectie hoort benoemd te worden: " + m.diagnostics);
        assertFalse(emitCompact(m).contains("QuantifierFold"),
                "een geweigerde deelselectie mag nooit als vouw over het VOLLE bereik landen");
    }

    @Test void a_negated_subselection_falls_loudly_in_place_of_binding_reversed() {
        // "die NIET aangemeld zijn" is an ordinary sentence and this resolver does not bind it — the
        // §13.4.13.5-.8 kenmerk register it shares with the instance fold is the affirmative one. What
        // matters is the direction of the failure: refusing costs a sentence, silently binding the
        // AFFIRMATIVE kenmerk would quantify over exactly the complement of what was written.
        RegelSpraakModel m = resolve("alle", " die niet aangemeld zijn");
        assertEquals(1, setReadErrors(m).size(), () -> "de setlezing hoort te blijven staan: " + m.diagnostics);
        assertEquals(1, subselectionWarning(m).size(),
                () -> "de geweigerde deelselectie hoort benoemd te worden: " + m.diagnostics);
        assertFalse(emitCompact(m).contains("QuantifierFold"),
                "een ontkende deelselectie mag nooit als de BEVESTIGENDE vouw landen");
    }

    @Test void a_number_predicate_subselection_falls_loudly_on_a_temporal_attribute() {
        // §5.6's getalpredicaat names no attribute of its own: the comparison is of the very attribute
        // the fold ranges over. Here that attribute is TIJDSAFHANKELIJK, and a timeline in a scalar slot
        // has no reading (ch. 3.3), so the filter would emit a document that falls over by name in all
        // three executors — which makes ADMITTING it the misleading answer. On the §8.2 position the
        // operand was temporal BY CONSTRUCTION (the qualifier is only defined over a temporal onderwerp
        // and was what admitted the fold at all), so this row read as a refusal of the POSITION until
        // the unqualified fold landed on 2026-09-19. The check was always keyed to the attribute's
        // granularity, so nothing moved: over a NON-temporal attribute the same sentence binds
        // (NonTemporalFoldTest), and the node carried the shape all along.
        RegelSpraakModel m = resolve("alle", " die groter zijn dan 3 kg");
        assertEquals(1, setReadErrors(m).size(), () -> "de setlezing hoort te blijven staan: " + m.diagnostics);
        assertEquals(1, subselectionWarning(m).size(),
                () -> "de geweigerde deelselectie hoort benoemd te worden: " + m.diagnostics);
        assertTrue(subselectionWarning(m).get(0).message().contains("gekwantificeerde attribuut 'gewicht'"),
                () -> "de weigering hoort de AS te noemen waarop zij valt: " + m.diagnostics);
        assertFalse(emitCompact(m).contains("QuantifierFold"),
                "een getalpredicaat-deelselectie mag nooit als vouw over het VOLLE bereik landen");
    }

    @Test void the_compound_subselection_reaches_the_filter() {
        // §13.4.13.4 "die aan alle volgende voorwaarden voldoen: …" — the same compound machinery the
        // aggregation's subselectie uses, bound against the ELEMENT (the bullets' "hij" is the member).
        RegelSpraakModel m = Resolver.resolve(MODEL + """
            Regel maatgevend gewicht
                geldig altijd
                    Het maatgevend gewicht van een Jutter moet gesteld worden op 10 kg
                    indien het gewicht van alle vondsten van de Jutter die aan alle volgende voorwaarden voldoen:
                        - hij aangemeld is
                        - hij geborgen is
                    gedurende het gehele jaar groter is dan 5 kg.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String v = condition(m);
        assertTrue(v.contains("\"filter\":{\"node\":\"Compound\",\"quantifier\":\"all\""),
                () -> "de samengestelde deelselectie hoort als filter op de wire te staan:\n" + v);
        assertTrue(v.contains("\"characteristic\":\"aangemeld\"") && v.contains("\"characteristic\":\"geborgen\""),
                () -> "beide bullets horen tegen het ELEMENT gebonden te zijn:\n" + v);
    }

    @Test void without_subselection_the_filter_stays_empty() {
        // The regression pin beside the four above: the unfiltered sentence is unchanged, no filter,
        // no diagnostics — a die-scan that started seeing subselections where none are written would
        // break every existing §8.2 golden.
        RegelSpraakModel m = resolve("alle", "");
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertFalse(condition(m).contains("\"filter\""),
                () -> "zonder geschreven deelselectie hoort er geen filter te staan:\n" + condition(m));
    }
}
