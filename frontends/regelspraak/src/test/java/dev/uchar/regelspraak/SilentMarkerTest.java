package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The marker discipline (maintainer decision 2026-09-19): EVERY {@code Unresolved} that survives into
 * the emitted model carries a WARNING beside it, so the document stays emittable — the marker remains
 * the honest wire form — while the author is told what stayed unresolved and why.
 *
 * <p>Before this card a marker could reach the wire without a single diagnostic. Seventeen production
 * sites were silent that way; three of them — the aggregation's own subselectie routes — spoke only
 * through their caller. The loud ones (an aggregation over an undeclared member, the three
 * {@code checkFoldSource} refusals, the §8.2 qualifier over a static onderwerp, a rangtelwoord over two
 * bindings) already said their piece, and they still do: they keep their own text and the sweep does
 * NOT repeat it, which is what the marker's {@code reden == null} records.
 *
 * <p><b>Why a SWEEP and not a diagnostic at each construction.</b> A marker is not final where it is
 * built. {@code conditionFromToplevelBare} markers a condition its caller then re-reads as a quantifier fold;
 * {@code targetCondition} re-resolves a markered feitcreatie guard against the doel; the §13.4.17.53
 * re-split rebuilds a markered aggregation operand in place. Diagnosing at the construction would
 * report every one of those as unresolved and be wrong about it — measurably: the membership-fold
 * goldens produce such a marker on the way to a clean fold. So the reason travels ON the marker and
 * the report is raised over what is still there when the model is finished.
 */
class SilentMarkerTest {

    /** A Jutterswet fragment: a Schouwploeg holding many Strandwachten, with a kenmerk on the
     *  SUBJECT and one on the ELEMENT — enough to write both a binding and a non-binding sentence. */
    static final String MODEL = """
        Objecttype de Schouwploeg (mv: Schouwploegen)
            het wachttal Numeriek (geheel getal);
            is bevoegd kenmerk (bijvoeglijk);
            is gekeurd kenmerk (bijvoeglijk);

        Objecttype de Strandwacht (mv: Strandwachten) (bezield)
            de dienstjaren Numeriek (geheel getal);
            is gediplomeerd kenmerk (bijvoeglijk);

        Feittype schouwindeling
            de ploeg Schouwploeg
            de wacht (mv: wachten) Strandwacht
        Eén ploeg omvat meerdere wachten
        """;

    private static RegelSpraakModel resolve(String rules) {
        return Resolver.resolve(MODEL + rules);
    }

    private static String emitCompact(RegelSpraakModel m) {
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    /** The WARNINGs the SURVIVOR SWEEP raised — every one carries the "onopgelost: " prefix. The
     *  quantifier fold's own declines are a second channel (see {@code reportFoldRefusal}) and are
     *  deliberately not counted here: they name an axis the sweep cannot. */
    private static List<Diagnostic> sweepMeldingen(RegelSpraakModel m) {
        return m.diagnostics.stream()
                .filter(d -> d.severity() == Severity.WARNING && d.message().startsWith("onopgelost: "))
                .toList();
    }

    /** A markered document must stay EMITTABLE: a WARNING leaves the file on disk (that is the whole
     *  difference with the ERROR routes), so assert both halves at once. */
    private static void markerWithMessage(RegelSpraakModel m, String fragment) {
        String c = emitCompact(m);
        assertTrue(c.contains("\"node\":\"Unresolved\""),
                () -> "verwachtte een marker op de wire:\n" + c);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING
                        && d.message().contains(fragment)),
                () -> "verwachtte een WARNING met '" + fragment + "', kreeg: " + m.diagnostics);
        assertTrue(m.diagnostics.stream().noneMatch(Diagnostic::isError),
                () -> "een marker blokkeert de emissie niet — geen ERROR verwacht: " + m.diagnostics);
        // WF-113 (b) on the written document itself: every route in this class goes through here
        Report.assertAccountedFor(IrEmitter.emit(m));
    }

    // ---------------- the condition routes ----------------

    /**
     * A route no corpus golden walks and real casus material does: "hij is een &lt;naam&gt;" where the
     * name is neither a kenmerk nor a role of the subject. Until this card the whole sentence became a
     * marker without one word said about it.
     */
    @Test void a_name_that_binds_no_characteristic_and_no_role_is_reported() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - hij is een zeewaardige ploeg
                    - het wachttal van de Schouwploeg is groter dan 0.
            """);
        markerWithMessage(m, "bindt geen kenmerk en geen rol");
        assertTrue(sweepMeldingen(m).get(0).message().contains("zeewaardige ploeg"),
                () -> "de melding hoort de geschreven zin te citeren: " + m.diagnostics);
    }

    /** The verb-trailing register of the same refusal — one sentence, the whole voorwaardendeel. */
    @Test void the_verb_trailing_form_of_the_same_refusal_is_also_reported() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien de Schouwploeg een zeewaardige ploeg is.
            """);
        markerWithMessage(m, "bindt geen kenmerk en geen rol");
    }

    /** §8.1.8.3 "is niet een &lt;naam&gt;" over a name that binds nothing: the positive form is resolved
     *  and then flipped, so the refusal arrives at {@code negate} with the negation no longer in the
     *  parse tree. Its own reason says so. */
    @Test void a_negated_form_without_binding_is_reported() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - hij is niet een zeewaardige ploeg
                    - het wachttal van de Schouwploeg is groter dan 0.
            """);
        markerWithMessage(m, "er is niets om te ontkennen");
    }

    // ---------------- the quantifier-fold membership route ----------------

    /**
     * §8.1.7: a ROLCHECK of the element is a different quantified question, so the membership fold
     * declines it. The card that opened this one measured it as one of four silent declines; it now
     * names the element and the name it could not bind on it.
     */
    @Test void a_role_check_of_the_element_reports_the_axis_of_the_refusal() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien alle wachten van de Schouwploeg gekeurd zijn.
            """);
        markerWithMessage(m, "is geen kenmerk van Strandwacht");
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.message().contains("kwantorvouw")),
                () -> "de vouw hoort haar eigen weigering te noemen: " + m.diagnostics);
    }

    /**
     * A MULTI-STEP written range: the fold spells one relation, so binding the first step would
     * quantify a set the sentence does not name. The decline was silent; it now says which phrase
     * stacked the second set.
     */
    @Test void a_multi_step_range_reports_the_stacked_set() {
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
        markerWithMessage(m, "meerstapsbereik");
    }

    /**
     * The bare definite "de" over a PLURAL member phrase reads as a quantification but writes no
     * quantor, so there is nothing to fold. The sentence used to fall to a marker in silence.
     */
    @Test void a_bare_the_over_a_plural_is_reported() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien de wachten van de Schouwploeg gediplomeerd zijn.
            """);
        markerWithMessage(m, "geen kwantor");
    }

    /** The element type resolves — "Strandwachten" is its declared meervoud — but NO feittype joins it
     *  to the subject, so the range of the quantifier cannot be bound. The fold declines rather than
     *  guess a relation, and now says which two types it found nothing between. */
    @Test void an_unbindable_range_is_reported() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Schouwploeg (mv: Schouwploegen)
                is bevoegd kenmerk (bijvoeglijk);

            Objecttype de Strandwacht (mv: Strandwachten) (bezield)
                is gediplomeerd kenmerk (bijvoeglijk);

            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien alle Strandwachten van de Schouwploeg gediplomeerd zijn.
            """);
        markerWithMessage(m, "het bereik van de kwantor is niet te binden");
    }

    // ---------------- the expression routes ----------------

    /** A bare name that is no parameter, no "Daarbij geldt:" variable and no attribute of the subject
     *  became an {@code Expr.Unresolved} with nothing said. */
    @Test void an_unbindable_name_in_an_expression_is_reported() {
        RegelSpraakModel m = resolve("""
            Regel wachttal
                geldig altijd
                    Het wachttal van een Schouwploeg moet berekend worden als de vloothoogte.
            """);
        markerWithMessage(m, "geen gedeclareerde parameter");
    }

    // ---------------- what must NOT happen ----------------

    /**
     * The anti-test of the whole design: a membership fold that LANDS produces a marker on the way
     * (the kenmerkcheck of the element binds nothing against the rule subject) and must stay clean.
     * A diagnostic raised at the construction instead of over the survivors would report this
     * sentence — and every golden that writes one — as unresolved.
     */
    @Test void a_landed_fold_reports_nothing() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien alle wachten van de Schouwploeg gediplomeerd zijn.
            """);
        assertFalse(emitCompact(m).contains("Unresolved"),
                () -> "de vouw hoort te landen:\n" + emitCompact(m));
        assertTrue(m.diagnostics.isEmpty(), () -> "een gelande vouw meldt niets: " + m.diagnostics);
    }

    /** A route that already speaks keeps its own text and gets no second WARNING — that is what the
     *  marker's {@code reden == null} buys. */
    @Test void a_loud_route_is_not_reported_twice() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien ten minste 0 van de wachten van de Schouwploeg gediplomeerd zijn.
            """);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.message().contains("telwoord van de kwantor")),
                () -> "de bestaande melding hoort te blijven: " + m.diagnostics);
        assertEquals(1, sweepMeldingen(m).size(),
                () -> "precies één sweep-melding over de overgebleven marker: " + m.diagnostics);
    }

    // ---------------- the remaining reason-carrying routes ----------------

    /**
     * §13.4.14.6: the indentation-blind bullet grammar can absorb a dedented sibling into a deeper
     * list, so a list whose direct bullets differ in COLUMN or in GLYPH did not nest the way it reads.
     * One '-' beside one '..' is the smallest such sentence.
     */
    @Test void inconsistent_bullets_report_the_unclear_nesting() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - het wachttal van de Schouwploeg is groter dan 0
                    .. hij is gekeurd.
            """);
        markerWithMessage(m, "staan op verschillende kolommen of dragen verschillende tekens");
    }

    /**
     * §13.4.14.6 again, one level in: a die/dat subselectie under a COUNTING quantifier whose own
     * bullets carry a further compound. The same flattening changes WHAT is counted, so the selection
     * declines. Note the aggregation route ALSO warns here (its caller names the set-aside aggregation);
     * the two say different things — that one the aggregation is dropped, this one why the selection
     * could not be structured — and only this one reaches the fold route, where nothing else speaks.
     */
    @Test void a_counting_quantifier_over_deeper_nesting_is_reported() {
        RegelSpraakModel m = resolve("""
            Regel wachttal van een ploeg
                geldig altijd
                    Het wachttal van een Schouwploeg moet berekend worden als het aantal wachten van de Schouwploeg die aan ten minste 2 van de volgende voorwaarden voldoen:
                    .. hij is gediplomeerd
                    .. hij voldoet aan alle volgende voorwaarden:
                    .. hij is gediplomeerd
                    .. hij voldoet aan alle volgende voorwaarden:
                    .. zijn dienstjaren zijn groter dan 3.
            """);
        markerWithMessage(m, "een tellende kwantor over een deelselectie");
    }

    /**
     * §8.1: the greedy onderwerp parse pulls a trailing "niet" INTO the operand, and a comparison
     * cannot put it back — emitting the positive form would invert the question. Written here in the
     * VRAGENDE register ("… niet groter is dan"), which is {@code conditionFromToplevelBare}'s guard.
     */
    @Test void an_absorbed_not_on_a_comparison_is_reported() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien zijn wachttal niet groter is dan 0.
            """);
        markerWithMessage(m, "trok de ontkenning 'niet' in het onderwerp");
    }

    /**
     * The same refusal in the STELLENDE register ("… niet is groter dan"), which is
     * {@code conditionFromNonToplevelBare}'s guard — a second production site, reached through the bullet
     * grammar. Both carry ONE text on purpose: it is one refusal, and two spellings of a sentence
     * should not read as two different problems. That the two sites are genuinely distinct was
     * measured by marking one of them apart: this sentence changed, the one above did not.
     */
    @Test void the_same_refusal_in_the_verb_leading_register() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn wachttal niet is groter dan 0
                    - hij is gekeurd.
            """);
        markerWithMessage(m, "trok de ontkenning 'niet' in het onderwerp");
    }

    // ---------------- routes that only an ill-formed document reaches ----------------

    /**
     * Four production sites are structural fall-throughs: every alternative their grammar rule offers
     * is handled above them, so only ANTLR's ERROR RECOVERY — which hands the resolver a context with
     * missing children — can reach them. Such a document already carries a syntax ERROR and
     * {@code --ir} writes nothing for it, so these witnesses assert the reason AND the blocking error
     * instead of a successful emission: that pairing IS the statement about the route.
     *
     * <p>Their siblings that no input reaches at all carry a GEEN GETUIGE comment at the production
     * site instead ({@code buildFromNodes}, {@code buildCriterion}, {@code buildElementary},
     * {@code characteristicCheckPredicate}'s §8.2 arm, {@code build}'s fall-through, {@code buildPrimary(null)} and
     * {@code buildAttribute}'s guard).
     */
    private static void messageAtSyntaxError(RegelSpraakModel m, String fragment) {
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING
                        && d.message().contains(fragment)),
                () -> "verwachtte een WARNING met '" + fragment + "', kreeg: " + m.diagnostics);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.isError() && d.message().startsWith("syntaxfout")),
                () -> "deze route is alleen via foutherstel bereikbaar, dus er hoort een syntaxfout naast "
                        + "te staan — anders meet deze test iets anders dan zij denkt: " + m.diagnostics);
    }

    /** {@code buildCondition}: a voorwaardendeel whose §13.4.13 alternative never completed. */
    @Test void a_condition_part_without_form_is_reported() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien zijn wachttal.
            """);
        messageAtSyntaxError(m, "het voorwaardendeel heeft geen van de §13.4.13-vormen");
    }

    /** {@code buildNested}: a bullet whose §13.4.14 alternative never completed. */
    @Test void a_bullet_without_form_is_reported() {
        RegelSpraakModel m = resolve("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - zijn wachttal
                    - hij is gekeurd.
            """);
        messageAtSyntaxError(m, "deze bullet van de samengestelde voorwaarde is geen geneste");
    }

    /** {@code build(null)} and {@code buildPrimary}'s fall-through, in one document: the recovery leaves
     *  an operand position empty and a primaire expressie that binds nothing. */
    @Test void a_missing_expression_and_an_unbindable_primary_are_reported() {
        RegelSpraakModel m = resolve("""
            Regel wachttal van een ploeg
                geldig altijd
                    Het wachttal van een Schouwploeg moet berekend worden als het aantal wachten van de Schouwploeg die aan ten minste 2 van de volgende voorwaarden voldoen:
                    .. hij is gediplomeerd
                    .. hij voldoet aan alle volgende voorwaarden:
                    ... hij is gediplomeerd
                    ... zijn dienstjaren zijn groter dan 3.
            """);
        messageAtSyntaxError(m, "er is geen expressie om te bouwen");
        messageAtSyntaxError(m, "deze primaire expressie bindt geen literal");
    }

    // ---------------- what must NOT happen ----------------

    /** The diagnostics ride the v0 wire (the emitter writes them), so the sweep must raise NOTHING on
     *  a document without markers — otherwise every golden's ir.json would move. */
    @Test void a_clean_document_gets_no_single_message() {
        RegelSpraakModel m = resolve("""
            Regel wachttal
                geldig altijd
                    Het wachttal van een Schouwploeg moet gesteld worden op 1
                    indien de Schouwploeg gekeurd is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertTrue(emitCompact(m).contains("\"diagnostics\":[]"),
                () -> "de wire hoort een lege diagnostics-lijst te dragen:\n" + emitCompact(m));
    }
}
