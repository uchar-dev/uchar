package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §8.2 / IR-SPEC ch. 4.5 (WF-108): the official RegelSpraak sentence
 *
 * <pre>… indien de &lt;attribuut&gt; van ALLE &lt;leden&gt; van de &lt;Onderwerp&gt; gedurende het gehele jaar
 * groter is dan &lt;grens&gt;.</pre>
 *
 * <p>It parses and resolves, but until this card it built a {@code Vergelijking} whose left operand was
 * an {@code AttribuutRef} with {@code set: true} — a collection in a slot that takes one value — so
 * WF-096 raised an ERROR and the document never reached disk, let alone a backend. A sanctioned
 * sentence this frontend could read and never emit.
 *
 * <p>The IR has since grown {@link dev.uchar.regelspraak.ir.Conditie.QuantifierFold}, which is
 * that sentence's home: first-order quantification over the members of a linked instance set, with the
 * range spelled exactly as the instance fold spells it. The resolver now LOWERS to it, and the set
 * reading is not excused but REMOVED — the quantified operand comes out as an element-own
 * {@code set: false} read, so the set fence finds nothing left to name.
 *
 * <p>The scope is one set operand in a CONDITION, qualified or not. The §8.2 qualifier was the opening
 * test of this lowering until 2026-09-19: v2.3.0 writes the quantification (§13.4.2.9
 * {@code kwantificatie?}, §13.4.17.5) and the whole-period check (§13.4.15.70
 * {@code gehelePeriodevergelijking?}, optional in every §13.4.15.19-.48/.71-.100 operator) as
 * INDEPENDENT productions, so the plain spelling is as much RegelSpraak as its qualified sibling and the
 * gate put a frontend boundary where the language has none (maintainer reading 2026-09-17). The
 * unqualified rows live in {@link NonTemporalFoldTest}; this file keeps measuring the qualified one.
 * The other measured producer routes of the set fence — every VALUE position — are unchanged, and
 * {@link SetReadWithoutFoldTest} is the proof. The counting quantor was pinned here as a refusal until
 * 2026-09-16, when it became a lowering of its own — its row below now measures the telvouw it
 * produces. Beside those stand the shapes that decline to lower on purpose — three when this file was
 * written, four since the die/dat subselection joined them (see {@link QuantifierFoldFilterTest}).
 */
class QuantifierFoldTest {

    /** One Jutter to many Vondsten, the Vondst's gewicht on a §13.3.6 month timeline — §8.2's
     *  whole-period qualifier is only defined over a tijdsafhankelijk onderwerp, so the quantified
     *  attribute must carry one for the sentence to be well-formed at all. */
    static final String MODEL = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Objecttype de Jutter (mv: Jutters) (bezield)
            het maatgevend gewicht Numeriek (getal met 2 decimalen) met eenheid kg;
            het drempelgewicht Numeriek (getal met 2 decimalen) met eenheid kg;

        Objecttype de Vondst (mv: Vondsten)
            het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen) met eenheid kg voor elke maand;

        Feittype vondstaanmelding
            de aanmelder Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmelder heeft meerdere vondsten
        """;

    /** The §8.2 sentence itself, with {@code %s} for the written quantifier. */
    static final String SENTENCE = """
        Regel maatgevend gewicht
            geldig altijd
                Het maatgevend gewicht van een Jutter moet gesteld worden op 10 kg
                indien het gewicht van %s vondsten van de Jutter gedurende het gehele jaar groter is dan 5 kg.
        """;

    private static RegelSpraakModel resolve(String regels) {
        return Resolver.resolve(MODEL + regels);
    }

    /** Sets the model's ERRORs aside ({@link Emission}): several rows read the node a REFUSED sentence built. */
    private static String emitCompact(RegelSpraakModel m) {
        return Emission.withoutErrors(m).replaceAll("\\s+", "");
    }

    /** The set fence's refusal itself, matched on its own sentence rather than on the rule id: the
     *  counting-quantor WARNING cites WF-053 too — saying which fence it leaves standing is the point of
     *  it — so an id filter would count two different diagnostics as one kind. */
    private static List<Diagnostic> setReadErrors(RegelSpraakModel m) {
        return m.diagnostics.stream()
                .filter(d -> d.message().startsWith("setlezing buiten een vouwbron")).toList();
    }

    /** The emitted condition of the single rule's version, compacted — the slice every assertion below reads. */
    private static String voorwaarde(RegelSpraakModel m) {
        String c = emitCompact(m);
        int van = c.indexOf("\"condition\":");
        assertTrue(van >= 0, () -> "geen voorwaarde in de emissie:\n" + c);
        return c.substring(van);
    }

    @Test void the_all_sentence_resolves_without_any_diagnostic() {
        // The whole point of the card: a sanctioned sentence that produced an ERROR now produces a
        // document. Zero diagnostics, not "no setlezing error" — a lowering that traded one complaint for
        // another would have reclaimed nothing.
        RegelSpraakModel m = resolve(SENTENCE.formatted("alle"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    @Test void the_all_sentence_becomes_a_quantifier_fold_over_the_element_type() {
        RegelSpraakModel m = resolve(SENTENCE.formatted("alle"));
        String v = voorwaarde(m);
        assertTrue(v.contains("\"node\":\"QuantifierFold\""),
                () -> "de §8.2-zin hoort een QuantifierFold te worden:\n" + v);
        assertTrue(v.contains("\"quantifier\":\"all\""), () -> "verkeerde kwantor:\n" + v);
        // the quantified range is the MEMBER type, not the rule subject
        assertTrue(v.contains("\"elementType\":\"Vondst\""),
                () -> "de vouw hoort over Vondst te kwantificeren:\n" + v);
        // this sentence writes NO die/dat subselectie, so the filter field stays absent. (Since the
        // diedat-filter card a written subselectie DOES reach the wire; QuantifierFoldFilterTest pins that
        // side, and this line is the regression pin for the unfiltered sentence.)
        assertFalse(v.contains("\"filter\""), () -> "zonder geschreven deelselectie geen filter:\n" + v);
    }

    @Test void the_quantifier_fold_carries_the_connecting_role_as_range() {
        // The required range, on this node (WF-048's concern until L2): a quantifier bound to no relation
        // quantifies over a set the sentence never named. The range is the connecting role, the binding the
        // instance fold's sourceRole carries — one search in the model serves both, so the two can never disagree.
        String v = voorwaarde(resolve(SENTENCE.formatted("alle")));
        assertTrue(v.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"vondstaanmelding\",\"role\":\"vondst\"}"),
                () -> "de vouw hoort het verbindende feittype+rol als bereik te dragen:\n" + v);
        // one-step range: sourceRole XOR sourcePath (WF-108 (c)), so no path beside the binding
        assertFalse(v.contains("\"sourcePath\""),
                () -> "een bereik van één stap hoort geen sourcePath te schrijven:\n" + v);
    }

    @Test void the_test_is_an_own_read_of_the_element_with_the_period() {
        // The matrix is evaluated PER CONTRIBUTOR with the element as subject, so the quantified operand
        // comes back as an OWN read of the element type — no binding, so no navigation and no range. The
        // §8.2 qualifier stays on it: per member, "gedurende het gehele jaar" is what that member must satisfy.
        String v = voorwaarde(resolve(SENTENCE.formatted("alle")));
        assertTrue(v.contains("\"test\":{\"node\":\"Comparison\",\"op\":\"greater\","
                        + "\"left\":{\"node\":\"AttributeReference\",\"attribute\":\"gewicht\",\"subjectType\":\"Vondst\""),
                () -> "de toets hoort het attribuut op het ELEMENTtype te lezen:\n" + v);
        assertTrue(java.util.regex.Pattern.compile("\"left\":\\{\"node\":\"AttributeReference\",\"attribute\":\"gewicht\","
                        + "\"subjectType\":\"Vondst\",\"source\":\\{\"snippet\":\"[^\"]*\"\\}\\}").matcher(v).find(),
                () -> "de gekwantificeerde operand hoort als eigen lezing terug te komen, zonder binding:\n" + v);
        String test = v.substring(v.indexOf("\"test\":"), v.lastIndexOf("\"sourceRole\":"));
        assertFalse(test.contains("\"sourceRole\"") || test.contains("\"sourcePath\""),
                () -> "na de verlaging hoort er geen vouw over gekoppelde instanties meer in de toets te staan:\n" + v);
        assertTrue(v.contains("\"wholePeriod\":{\"node\":\"WholePeriod\",\"granularity\":\"year\",\"negated\":false}"),
                () -> "de §8.2-kwalificatie hoort op de toets te blijven staan:\n" + v);
    }

    @Test void the_test_carries_no_path() {
        // The element-subject emission, and the reason it is not cosmetic: emitted from the OUTER
        // subject, an element-own read is an off-subject type and would pick up a by-pair
        // path — a navigation from the Jutter, inside a matrix whose subject is the
        // Vondst. Exactly the trap an aggregation's filter already avoids the same way.
        String v = voorwaarde(resolve(SENTENCE.formatted("alle")));
        assertFalse(v.contains("\"path\""),
                () -> "de toets wordt met het ELEMENT als subject geëmitteerd en hoort dus geen "
                        + "navigatiebindingen te dragen:\n" + v);
        assertTrue(v.contains("\"attribute\":\"gewicht\",\"subjectType\":\"Vondst\""),
                () -> "de eigen lezing noemt het elementtype, het onderwerp van de toets:\n" + v);
    }

    @Test void the_none_of_the_spelling_becomes_the_none_quantifier() {
        // The reason the node records the WRITTEN quantor at all: `set` is true for "alle" and for
        // "geen van de" alike, so a consumer reading the flag as a forall would answer ALLE here.
        RegelSpraakModel m = resolve(SENTENCE.formatted("geen van de"));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String v = voorwaarde(m);
        assertTrue(v.contains("\"quantifier\":\"none\""),
                () -> "'geen van de' hoort de kwantor none te schrijven, niet all:\n" + v);
    }

    /**
     * <b>Deze rij is op 2026-09-16 omgedraaid</b> (kaart tellende-kwantoren). Tot dan waren de drie
     * tellende kwantoren het benoemde uitbreidingspunt van WF-108 (a) en pinde zij precies de weigering:
     * WF-096 bleef staan en een WARNING noemde de geweigerde kwantor. Zij zijn nu gebouwd, dus de
     * aanname onder die rij is weg. De bewering die zij droeg — "ten minste 2 van de" mag NOOIT als
     * forall op de wire belanden — staat er nog, en wordt nu sterker gemeten: niet dat er geen vouw komt,
     * maar dat er een vouw komt die de GESCHREVEN kwantor en het GESCHREVEN telwoord draagt.
     */
    @Test void the_counting_quantifiers_become_the_count_fold_with_their_own_numeral() {
        record Row(String written, String kwantor, int threshold) {}
        for (Row r : new Row[]{new Row("ten minste 2 van de", "atLeast", 2),
                               new Row("ten hoogste 1 van de", "atMost", 1),
                               new Row("precies 3 van de", "exactly", 3)}) {
            RegelSpraakModel m = resolve(SENTENCE.formatted(r.written()));
            assertTrue(m.diagnostics.isEmpty(),
                    () -> r.written() + " — onverwachte diagnostiek: " + m.diagnostics);
            String v = voorwaarde(m);
            assertTrue(v.startsWith("\"condition\":{\"node\":\"QuantifierFold\",\"quantifier\":\""
                            + r.kwantor() + "\",\"threshold\":" + r.threshold() + ","),
                    () -> r.written() + " hoort de kwantor EN het telwoord te dragen:\n" + v);
        }
    }

    /**
     * De drempel staat er PRECIES onder een tellende kwantor (WF-108 (e)): onder ALLE en GEEN schrijft
     * de emitter hem NIET, anders staat er een getal op de wire dat het kwantorveld tegenspreekt.
     */
    @Test void all_and_none_carry_no_threshold_on_the_wire() {
        for (String kw : new String[]{"alle", "geen van de"})
            assertFalse(voorwaarde(resolve(SENTENCE.formatted(kw))).contains("\"threshold\""),
                    kw + " leest geen drempel, dus de wire hoort er geen te dragen");
    }

    /**
     * Een telwoord onder 1 wordt NIET gelowerd: WF-041 weigert zo'n drempel op de wire, dus emitteren zou
     * de weigering alleen verplaatsen. De setlezing-fout blijft staan en een WARNING noemt het telwoord.
     */
    @Test void a_numeral_under_one_is_not_lowered_and_keeps_the_set_read_fault() {
        RegelSpraakModel m = resolve(SENTENCE.formatted("ten minste 0 van de"));
        assertEquals(1, setReadErrors(m).size(), () -> "de setlezing hoort te blijven staan: " + m.diagnostics);
        assertEquals(Severity.ERROR, setReadErrors(m).get(0).severity());
        List<Diagnostic> telwoord = m.diagnostics.stream()
                .filter(d -> d.message().contains("telwoord van de kwantor")).toList();
        assertEquals(1, telwoord.size(), () -> "de geweigerde drempel hoort benoemd te worden: " + m.diagnostics);
        assertEquals(Severity.WARNING, telwoord.get(0).severity());
        assertFalse(emitCompact(m).contains("QuantifierFold"),
                "een drempel onder 1 hoort niet op de wire te belanden");
    }

    @Test void an_unqualified_set_read_in_a_condition_also_becomes_a_quantifier_fold() {
        // Was the pin of the §8.2 gate ("de ongekwalificeerde setlezing hoort te blijven"); since the
        // maintainer reading of 2026-09-17 it measures the opposite, because the gate was the frontend's
        // and not the language's. The qualifier is the ONLY difference with the rows above, so the row
        // belongs here as well: the same sentence, the same node, and the matrix simply carries no
        // period. NonTemporalFoldTest measures what that opens up.
        RegelSpraakModel m = resolve("""
            Regel maatgevend gewicht
                geldig altijd
                    Het maatgevend gewicht van een Jutter moet gesteld worden op 10 kg
                    indien het gewicht van alle vondsten van de Jutter gelijk is aan 5 kg.
            """);
        assertTrue(setReadErrors(m).isEmpty(), () -> "de vouw hoort de setlezing weg te nemen: " + m.diagnostics);
        String v = voorwaarde(m);
        assertTrue(v.startsWith("\"condition\":{\"node\":\"QuantifierFold\",\"quantifier\":\"all\","),
                () -> "de ongekwalificeerde zin hoort dezelfde vouw te worden:\n" + v);
        assertFalse(v.contains("\"wholePeriod\""),
                () -> "zonder §8.2-kwalificatie hoort de toets geen periode te dragen:\n" + v);
    }

    @Test void an_unbindable_range_is_not_lowered_and_keeps_the_set_read_fault() {
        // Fail fast over a silent misbinding: with no feittype connecting Jutter and Vondst the range
        // does not resolve, and a Kwantorvouw without a range quantifies over nothing the sentence
        // named. The §8.2 qualifier itself is fine here (the attribute is temporal), so the ONLY
        // complaint is the one that was there before — the refusal is intact, not replaced.
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Gewicht
                het kilogram (mv: kilogrammen) kg

            Objecttype de Jutter (mv: Jutters) (bezield)
                het maatgevend gewicht Numeriek (getal met 2 decimalen) met eenheid kg;

            Objecttype de Vondst (mv: Vondsten)
                het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen) met eenheid kg voor elke maand;
            """ + SENTENCE.formatted("alle"));
        assertEquals(1, setReadErrors(m).size(),
                () -> "een onbindbaar bereik hoort de weigering te houden: " + m.diagnostics);
        assertEquals(1, m.diagnostics.size(), () -> "en verder niets te melden: " + m.diagnostics);
        // not lowered, so the set reading stays where it was written; without a binding it has no spelling
        // either, and the emitter writes no document at all rather than a read of the Jutter's own gewicht
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> emitCompact(m),
                "een vouw zonder bereik hoort niet op de wire te belanden");
        assertTrue(e.getMessage().contains("het gewicht van alle vondsten van de Jutter"), e::getMessage);
    }

    @Test void the_written_quantifier_stays_frontend_internal() {
        // Step 1 of the lowering hangs the written quantifier on the AttribuutRef so ALLE and GEEN are
        // distinguishable downstream. It is resolver input only: the reference node has no such wire
        // field, and a legitimate fold (a set read, which the wire spells as a fold with a range) must
        // not start writing one.
        RegelSpraakModel m = resolve("""
            Regel maatgevend gewicht
                geldig altijd
                    Het maatgevend gewicht van een Jutter moet gesteld worden op de som van het gewicht van alle vondsten van de Jutter.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        String c = emitCompact(m);
        assertTrue(c.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"vondstaanmelding\",\"role\":\"vondst\"}"),
                () -> "de vouw hoort haar setlezing te houden, als bereik:\n" + c);
        assertFalse(c.contains("\"quantifier\""),
                () -> "de geschreven kwantor is frontend-intern en hoort niet op de wire te staan:\n" + c);
    }
}
