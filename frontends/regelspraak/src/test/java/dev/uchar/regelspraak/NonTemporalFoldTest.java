package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The quantifier fold WITHOUT the §8.2 whole-period qualifier — "… indien de dienstjaren van ALLE
 * wachten van de Schouwploeg groter zijn dan 5", "… indien ALLE wachten van de Schouwploeg
 * gediplomeerd zijn".
 *
 * <p><b>The spec ground.</b> v2.3.0 writes quantification and the whole-period check as INDEPENDENT
 * productions. The quantifier is the {@code kwantificatie?} of §13.4.2.9 (attribuut van onderwerp) and
 * of §13.4.17.5 (objectexpressie), with its own vocabulary in §13.4.14.5 (de | alle | al | geen van de
 * | (ten minste|ten hoogste|precies) &lt;telwoord&gt; van de). The whole-period check is
 * §13.4.15.70's {@code gehelePeriodevergelijking}, and it appears as an OPTIONAL prefix in every one
 * of the comparison-operator productions §13.4.15.19-.48 (verb-split) and §13.4.15.71-.100
 * (verb-leading). Neither production mentions the other, so the unqualified spelling of a quantified
 * condition is as much RegelSpraak as its qualified sibling.
 *
 * <p>Until 2026-09-19 this frontend nevertheless opened its lowering with a qualifier test
 * ({@code if (wholePeriodOf(c) == null) return c;}) and refused the plain spelling: the comparison form
 * fell on WF-096 ("setlezing buiten een vouwbron") and the membership form resolved to an
 * {@code Unresolved} marker. That put a FRONTEND boundary where the language has none — measured on
 * the two corpus goldens whose hand-written IR carried exactly these nodes with no {@code source.rgs}
 * beside them, because no RegelSpraak source could produce them. Maintainer reading 2026-09-17: the
 * unqualified form is valid RegelSpraak and the frontend must carry it.
 *
 * <p>The matrix is then an ordinary, NON-temporal condition of the member — RUNTIME-SEMANTICS ch. 3.9a
 * states the fold's formula over an arbitrary {@code test}, so nothing about the quantifier moves; only
 * the {@code wholePeriod} field of the matrix is absent. {@link QuantifierFoldTest} keeps measuring the
 * qualified sentence, and every decline this file does not repeat is measured there and in
 * {@link QuantifierFoldFilterTest}: those refusals are unchanged, and a spelling v2.3.0 does not bear is
 * still refused out loud.
 */
class NonTemporalFoldTest {

    /** The corpus goldens' model, reduced: one Schouwploeg to many Strandwachten, with a scalar
     *  attribute and a kenmerk on the ELEMENT and a kenmerk on the SUBJECT. Nothing here is
     *  tijdsafhankelijk — which is exactly what makes every fold below non-temporal. */
    static final String MODEL = """
        Objecttype de Schouwploeg (mv: Schouwploegen)
            het wachttal Numeriek (geheel getal);
            is bevoegd kenmerk (bijvoeglijk);
            is sluitend kenmerk (bijvoeglijk);
            is gekeurd kenmerk (bijvoeglijk);

        Objecttype de Strandwacht (mv: Strandwachten) (bezield)
            de dienstjaren Numeriek (geheel getal);
            is gediplomeerd kenmerk (bijvoeglijk);
            is ervaren kenmerk (bijvoeglijk);

        Feittype schouwindeling
            de ploeg Schouwploeg
            de wacht (mv: wachten) Strandwacht
        Eén ploeg omvat meerdere wachten
        """;

    /** The comparison spelling, with {@code %s} for the written quantifier — the §13.4.2.9 shape
     *  "&lt;attribuut&gt; van &lt;kwantificatie&gt; &lt;leden&gt; van de &lt;Onderwerp&gt;". */
    static final String COMPARISON = """
        Regel wachttal van een schouwploeg
            geldig altijd
                Het wachttal van een Schouwploeg moet gesteld worden op 1
                indien de dienstjaren van %s wachten van de Schouwploeg groter zijn dan 5.
        """;

    /** The membership spelling, with {@code %s} for the quantifier and {@code %s} for the predicate —
     *  the §13.4.15.45 "&lt;kwantificatie&gt; &lt;leden&gt; van de &lt;Onderwerp&gt; &lt;kenmerk&gt;
     *  zijn". Here the quantifier sits on the attribuutVanOnderwerp itself, so the operand carries no
     *  set reading at all and WF-096 never fired — the sentence simply markered. */
    static final String KENMERK = """
        Regel bevoegd bij diploma
            geldig altijd
                Een Schouwploeg is bevoegd
                indien %s wachten van de Schouwploeg %s.
        """;

    private static RegelSpraakModel resolve(String regels) {
        return Resolver.resolve(MODEL + regels);
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

    private static void clean(RegelSpraakModel m) {
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    // ---------------- the comparison matrix ----------------

    /**
     * All five §13.4.14.5 quantifiers over the unqualified comparison. The counting three carry their
     * written telwoord as the fold's drempel, exactly as the qualified sentence does — the qualifier was
     * never what selected the quantifier, which is the whole content of the widening.
     */
    @Test void all_five_quantifiers_lowered_without_qualification() {
        record Row(String written, String quantifier, String threshold) {}
        for (Row r : new Row[]{new Row("alle", "all", ""),
                               new Row("geen van de", "none", ""),
                               new Row("ten minste 2 van de", "atLeast", "\"threshold\":2,"),
                               new Row("ten hoogste 1 van de", "atMost", "\"threshold\":1,"),
                               new Row("precies 3 van de", "exactly", "\"threshold\":3,")}) {
            RegelSpraakModel m = resolve(COMPARISON.formatted(r.written()));
            assertTrue(m.diagnostics.isEmpty(),
                    () -> r.written() + " — onverwachte diagnostiek: " + m.diagnostics);
            String v = condition(m);
            assertTrue(v.startsWith("\"condition\":{\"node\":\"QuantifierFold\",\"quantifier\":\""
                            + r.quantifier() + "\"," + r.threshold() + "\"elementType\":\"Strandwacht\","),
                    () -> r.written() + " hoort de kwantor (en zijn telwoord) te dragen:\n" + v);
        }
    }

    /** The one field that differs from the qualified sentence, and the reason this widening is a
     *  widening and not a rewrite: the matrix carries NO period. */
    @Test void the_test_carries_no_period() {
        String v = condition(resolve(COMPARISON.formatted("alle")));
        assertTrue(v.contains("\"test\":{\"node\":\"Comparison\",\"op\":\"greater\","),
                () -> "de toets hoort de gewone vergelijking te zijn:\n" + v);
        assertFalse(v.contains("\"wholePeriod\""),
                () -> "zonder §8.2-kwalificatie hoort er geen periode op de toets te staan:\n" + v);
        assertFalse(v.contains("\"WholePeriod\""),
                () -> "er is geen periode geschreven, dus hoort er geen in de emissie te staan:\n" + v);
    }

    /** Everything else is the qualified sentence's own lowering, unchanged: the element-own read, no fold
     *  left in the matrix, and the range spelled as the instance fold spells it. */
    @Test void the_quantified_operand_comes_back_as_own_read_of_the_element() {
        String v = condition(resolve(COMPARISON.formatted("alle")));
        assertTrue(v.contains("\"left\":{\"node\":\"AttributeReference\",\"attribute\":\"dienstjaren\","
                        + "\"subjectType\":\"Strandwacht\""),
                () -> "de toets hoort het attribuut op het ELEMENTtype te lezen:\n" + v);
        String test = v.substring(v.indexOf("\"test\":"), v.lastIndexOf("\"sourceRole\":"));
        assertFalse(test.contains("\"sourceRole\"") || test.contains("\"sourcePath\""),
                () -> "na de verlaging hoort er geen vouw over gekoppelde instanties meer in de toets te staan:\n" + v);
        assertTrue(v.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"schouwindeling\","
                        + "\"role\":\"wacht\"}"),
                () -> "de vouw hoort het verbindende feittype+rol als bereik te dragen:\n" + v);
    }

    /** The §5.6 die/dat subselection on the unqualified sentence — the {@code filter} field, per
     *  contributor, bound against the ELEMENT type. */
    @Test void the_subselection_becomes_the_filter_of_the_unqualified_fold() {
        RegelSpraakModel m = resolve("""
            Regel wachttal van een schouwploeg
                geldig altijd
                    Het wachttal van een Schouwploeg moet gesteld worden op 1
                    indien de dienstjaren van ten minste 2 van de wachten van de Schouwploeg die gediplomeerd zijn groter zijn dan 5.
            """);
        clean(m);
        String v = condition(m);
        assertTrue(v.contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gediplomeerd\""),
                () -> "de geschreven deelselectie hoort het filter te worden:\n" + v);
        assertTrue(v.contains("\"quantifier\":\"atLeast\",\"threshold\":2,"),
                () -> "de tellende kwantor hoort naast het filter te staan:\n" + v);
    }

    /**
     * The §5.6 getalpredicaat over the quantified attribute, on the NON-temporal position. The
     * diedat-kaart refused it with a reason that was true only under the §8.2 gate — on the fold
     * position the quantified operand was TIJDSAFHANKELIJK by construction, and a timeline in a scalar
     * slot has no reading (ch. 3.3). That construction is gone here, and the refusal is keyed to the
     * ATTRIBUTE's granularity rather than to the position, so the filter binds: this row is the
     * measurement the card asked for, and it needed no code.
     */
    @Test void a_number_predicate_binds_on_a_non_temporal_attribute() {
        RegelSpraakModel m = resolve("""
            Regel wachttal van een schouwploeg
                geldig altijd
                    Het wachttal van een Schouwploeg moet gesteld worden op 1
                    indien de dienstjaren van alle wachten van de Schouwploeg die groter zijn dan 2 groter zijn dan 5.
            """);
        clean(m);
        String v = condition(m);
        assertTrue(v.contains("\"filter\":{\"node\":\"Comparison\",\"op\":\"greater\","),
                () -> "het getalpredicaat hoort het filter te worden:\n" + v);
    }

    // ---------------- the membership matrix ----------------

    /**
     * The second shape the goldens needed, and a route of its own. "alle wachten van de Schouwploeg"
     * parses as §13.4.2.9 {@code kwantificatie? attribuutMetLidwoord VAN onderwerpKeten}, so the
     * quantifier sits on the attribuutVanOnderwerp itself and the operand resolves as "the wachten of
     * the Schouwploeg" — a reference that is no set reading and carries no quantor. There was therefore
     * no set reading for WF-096 to fence and no operand for the fold to find: the sentence resolved to an
     * {@code Unresolved} marker, silently emitted.
     */
    @Test void the_characteristic_form_becomes_a_fold_over_a_characteristic_of_the_element() {
        RegelSpraakModel m = resolve(KENMERK.formatted("alle", "gediplomeerd zijn"));
        clean(m);
        String v = condition(m);
        assertTrue(v.startsWith("\"condition\":{\"node\":\"QuantifierFold\",\"quantifier\":\"all\","
                        + "\"elementType\":\"Strandwacht\",\"test\":"),
                () -> "de kenmerkvorm hoort een vouw over Strandwacht te worden:\n" + v);
        assertTrue(v.contains("\"test\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gediplomeerd\","
                        + "\"negated\":false,"),
                () -> "de matrix hoort de kenmerkcheck van het ELEMENT te zijn:\n" + v);
        assertFalse(v.contains("\"navigation\""), () -> "de kenmerkcheck van het element navigeert niet:\n" + v);
        assertFalse(v.contains("Unresolved"), () -> "er hoort geen marker meer te staan:\n" + v);
        assertTrue(v.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"schouwindeling\","
                        + "\"role\":\"wacht\"}"),
                () -> "de vouw hoort het verbindende feittype+rol als bereik te dragen:\n" + v);
    }

    /**
     * "geen van de wachten van de Schouwploeg NIET gediplomeerd is" — the negation belongs to the
     * MATRIX and is evaluated per member, so it must land on the {@code CharacteristicCheck} and not on the fold.
     * It is worth its own row because the greedy onderwerp parse ABSORBS the "niet" into the anchor's
     * kaleNaam ("Schouwploeg niet"), leaving the operator with the bare kenmerk; dropping it there
     * would emit the positive membership — the same reading, inverted, with nothing saying so.
     */
    @Test void an_absorbed_not_lands_on_the_matrix() {
        RegelSpraakModel m = resolve(KENMERK.formatted("geen van de", "niet gediplomeerd is"));
        clean(m);
        String v = condition(m);
        assertTrue(v.contains("\"quantifier\":\"none\""), () -> "de geschreven kwantor hoort none te zijn:\n" + v);
        assertTrue(v.contains("\"test\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gediplomeerd\","
                        + "\"negated\":true,"),
                () -> "de geabsorbeerde 'niet' hoort de matrix te ontkennen:\n" + v);
    }

    /** All five quantifiers over the membership matrix too — the counting ones carry their telwoord
     *  there for the same reason they do over a comparison. */
    @Test void all_five_quantifiers_over_the_characteristic_form() {
        record Row(String written, String quantifier, String threshold) {}
        for (Row r : new Row[]{new Row("alle", "all", ""),
                               new Row("geen van de", "none", ""),
                               new Row("ten minste 2 van de", "atLeast", "\"threshold\":2,"),
                               new Row("ten hoogste 1 van de", "atMost", "\"threshold\":1,"),
                               new Row("precies 3 van de", "exactly", "\"threshold\":3,")}) {
            RegelSpraakModel m = resolve(KENMERK.formatted(r.written(), "gediplomeerd zijn"));
            assertTrue(m.diagnostics.isEmpty(),
                    () -> r.written() + " — onverwachte diagnostiek: " + m.diagnostics);
            String v = condition(m);
            assertTrue(v.startsWith("\"condition\":{\"node\":\"QuantifierFold\",\"quantifier\":\""
                            + r.quantifier() + "\"," + r.threshold() + "\"elementType\":\"Strandwacht\","),
                    () -> r.written() + " hoort de kwantor (en zijn telwoord) te dragen:\n" + v);
        }
    }

    /** The verb-leading (stellende) register of the same membership sentence — a §13.4.14.7 bullet of a
     *  samengestelde voorwaarde. One lowering serves both registers; the bullet is where these
     *  sentences actually appear in a compound rule. */
    @Test void the_verb_leading_bullet_form_also_folds() {
        RegelSpraakModel m = resolve("""
            Regel bevoegd bij diploma
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - alle wachten van de Schouwploeg zijn gediplomeerd
                    - het wachttal van de Schouwploeg is groter dan 0.
            """);
        clean(m);
        String v = condition(m);
        assertTrue(v.contains("\"node\":\"QuantifierFold\",\"quantifier\":\"all\","
                        + "\"elementType\":\"Strandwacht\""),
                () -> "de bullet hoort dezelfde vouw te worden:\n" + v);
    }

    /** The subselection on the membership route binds the same way — and the filter and the matrix are
     *  two DIFFERENT kenmerken of the element, so a lowering that confused them would show here. */
    @Test void the_subselection_also_binds_on_the_characteristic_form() {
        RegelSpraakModel m = resolve(
                KENMERK.formatted("alle", "").replace("van de Schouwploeg .", "van de Schouwploeg die ervaren zijn gediplomeerd zijn."));
        clean(m);
        String v = condition(m);
        assertTrue(v.contains("\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"ervaren\""),
                () -> "de deelselectie hoort het filter te worden:\n" + v);
        assertTrue(v.contains("\"test\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gediplomeerd\""),
                () -> "de matrix hoort het ANDERE kenmerk te blijven:\n" + v);
    }

    // ---------------- what stays refused ----------------

    /** A kenmerk the ELEMENT does not declare binds nothing: the name is a kenmerk of the SUBJECT here,
     *  and reading it on the element would answer a question the sentence does not ask. The honest
     *  marker stays. */
    @Test void a_characteristic_of_the_subject_does_not_bind_on_the_element() {
        RegelSpraakModel m = resolve(KENMERK.formatted("alle", "gekeurd zijn"));
        String v = condition(m);
        assertTrue(v.contains("\"node\":\"Unresolved\""),
                () -> "een kenmerk van het SUBJECT hoort op het element niets te binden:\n" + v);
    }

    /**
     * A die/dat that does not bind against the element must BLOCK the document, and this row is the one
     * that says so out loud. On the comparison route a declined fold leaves the set reading standing and
     * the set fence raises the blocking ERROR; this route has no set reading, so the only thing between the
     * sentence and a silent wrong set is {@code checkPartSelections}, whose whole point is that it is an
     * ERROR and not a WARNING. The fold therefore hands the subselectie BACK when it declines —
     * measured regression during this card: for one commit the sentence produced a document with a
     * silent {@code Unresolved} and a WARNING the CLI does not even print.
     */
    @Test void a_non_binding_subselection_blocks_the_characteristic_form() {
        // 'gekeurd' is a kenmerk of the SUBJECT, not of the element, so the subselectie binds nothing.
        RegelSpraakModel m = resolve(
                KENMERK.formatted("alle", "").replace("van de Schouwploeg .", "van de Schouwploeg die gekeurd zijn gediplomeerd zijn."));
        List<Diagnostic> silent = m.diagnostics.stream()
                .filter(d -> d.message().startsWith("de 'die/dat'-deelselectie 'gekeurd zijn' wordt niet omgezet")).toList();
        assertEquals(1, silent.size(), () -> "de weggevallen deelselectie hoort benoemd te worden: " + m.diagnostics);
        assertEquals(Severity.ERROR, silent.get(0).severity(),
                () -> "een WAARSCHUWING laat het document op schijf staan — dat is precies de stilte die "
                        + "deze controle bestaat om te voorkomen: " + m.diagnostics);
        // …and the axis the binding failed on is named beside it, which the blocking ERROR cannot say
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING
                        && d.message().contains("bindt geen kenmerk of getalpredicaat van Strandwacht")),
                () -> "de as van de weigering hoort erbij te staan: " + m.diagnostics);
        assertTrue(condition(m).contains("\"node\":\"Unresolved\""),
                () -> "een niet-bindende deelselectie mag nooit als vouw over het VOLLE bereik landen:\n"
                        + condition(m));
    }

    /** A telwoord under 1 declines on the membership route for the reason it declines on the comparison
     *  one: WF-041 refuses that drempel on the wire, so emitting would only move the refusal
     *  downstream. The WARNING names the telwoord; the marker stays. */
    @Test void a_numeral_under_one_is_also_refused_on_the_characteristic_form() {
        RegelSpraakModel m = resolve(KENMERK.formatted("ten minste 0 van de", "gediplomeerd zijn"));
        List<Diagnostic> telwoord = m.diagnostics.stream()
                .filter(d -> d.message().contains("telwoord van de kwantor")).toList();
        assertEquals(1, telwoord.size(), () -> "het geweigerde telwoord hoort benoemd te worden: " + m.diagnostics);
        assertEquals(Severity.WARNING, telwoord.get(0).severity());
        assertFalse(emitCompact(m).contains("QuantifierFold"),
                "een drempel onder 1 hoort niet op de wire te belanden");
    }

    /** A §8.2-QUALIFIED membership matrix is NOT what this card opened: a whole-period quantified
     *  kenmerkcheck has no worked semantics here, so it keeps the marker it always had. Refusing a
     *  spelling out loud is the rule; inventing one is not. */
    @Test void the_qualified_characteristic_form_stays_a_marker() {
        RegelSpraakModel m = resolve(
                KENMERK.formatted("alle", "gedurende het gehele jaar gediplomeerd zijn"));
        assertTrue(condition(m).contains("\"node\":\"Unresolved\""),
                () -> "de gekwalificeerde kenmerkvorm hoort haar marker te houden:\n" + condition(m));
    }

    /** A MULTI-STEP written range on the membership route declines, exactly as it does on the
     *  comparison one: the fold spells one relation, and binding the first step of a chain would
     *  quantify a set the sentence does not name. */
    @Test void a_multi_step_range_on_the_characteristic_form_stays_a_marker() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Haven (mv: Havens)
                is veilig kenmerk (bijvoeglijk);

            Objecttype de Schouwploeg (mv: Schouwploegen)

            Objecttype de Strandwacht (mv: Strandwachten) (bezield)
                is gediplomeerd kenmerk (bijvoeglijk);

            Feittype havenindeling
                de haven Haven
                de ploeg (mv: ploegen) Schouwploeg
            Eén haven omvat meerdere ploegen

            Feittype schouwindeling
                de ploeg2 Schouwploeg
                de wacht (mv: wachten) Strandwacht
            Eén ploeg2 omvat meerdere wachten

            Regel veilig bij diploma
                geldig altijd
                    Een Haven is veilig
                    indien alle wachten van alle ploegen van de Haven gediplomeerd zijn.
            """);
        assertTrue(condition(m).contains("\"node\":\"Unresolved\""),
                () -> "een meerstapsbereik hoort niet als eenstapsvouw te landen:\n" + condition(m));
    }
}
