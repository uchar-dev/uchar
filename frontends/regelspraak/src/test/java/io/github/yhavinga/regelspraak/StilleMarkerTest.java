package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.emit.IrEmitter;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The marker discipline (maintainer decision 2026-09-19): EVERY {@code Onopgelost} that survives into
 * the emitted model carries a WARNING beside it, so the document stays emittable — the marker remains
 * the honest wire form — while the author is told what stayed unresolved and why.
 *
 * <p>Before this card a marker could reach the wire without a single diagnostic. Seventeen production
 * sites were silent that way; three of them — the aggregation's own subselectie routes — spoke only
 * through their caller. The loud ones (an aggregation over an undeclared member, the three
 * {@code keurVouwbron} refusals, the §8.2 qualifier over a static onderwerp, a rangtelwoord over two
 * bindings) already said their piece, and they still do: they keep their own text and the sweep does
 * NOT repeat it, which is what the marker's {@code reden == null} records.
 *
 * <p><b>Why a SWEEP and not a diagnostic at each construction.</b> A marker is not final where it is
 * built. {@code uitToplevelKaal} markers a condition its caller then re-reads as a quantifier fold;
 * {@code doelVoorwaarde} re-resolves a markered feitcreatie guard against the doel; the §13.4.17.53
 * re-split rebuilds a markered aggregation operand in place. Diagnosing at the construction would
 * report every one of those as unresolved and be wrong about it — measurably: the membership-fold
 * goldens produce such a marker on the way to a clean fold. So the reason travels ON the marker and
 * the report is raised over what is still there when the model is finished.
 */
class StilleMarkerTest {

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

    private static RegelSpraakModel resolveer(String regels) {
        return Resolver.resolve(MODEL + regels);
    }

    private static String emitCompact(RegelSpraakModel m) {
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    /** The WARNINGs the SURVIVOR SWEEP raised — every one carries the "onopgelost: " prefix. The
     *  quantifier fold's own declines are a second channel (see {@code meldVouwWeigering}) and are
     *  deliberately not counted here: they name an axis the sweep cannot. */
    private static List<Diagnostic> sweepMeldingen(RegelSpraakModel m) {
        return m.diagnostics.stream()
                .filter(d -> d.severity() == Severity.WARNING && d.message().startsWith("onopgelost: "))
                .toList();
    }

    /** A markered document must stay EMITTABLE: a WARNING leaves the file on disk (that is the whole
     *  difference with the ERROR routes), so assert both halves at once. */
    private static void markerMetMelding(RegelSpraakModel m, String fragment) {
        String c = emitCompact(m);
        assertTrue(c.contains("\"node\":\"Onopgelost\""),
                () -> "verwachtte een marker op de wire:\n" + c);
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING
                        && d.message().contains(fragment)),
                () -> "verwachtte een WARNING met '" + fragment + "', kreeg: " + m.diagnostics);
        assertTrue(m.diagnostics.stream().noneMatch(Diagnostic::isError),
                () -> "een marker blokkeert de emissie niet — geen ERROR verwacht: " + m.diagnostics);
    }

    // ---------------- the condition routes ----------------

    /**
     * A route no corpus golden walks and real casus material does: "hij is een &lt;naam&gt;" where the
     * name is neither a kenmerk nor a role of the subject. Until this card the whole sentence became a
     * marker without one word said about it.
     */
    @Test void een_naam_die_geen_kenmerk_en_geen_rol_bindt_wordt_gemeld() {
        RegelSpraakModel m = resolveer("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - hij is een zeewaardige ploeg
                    - het wachttal van de Schouwploeg is groter dan 0.
            """);
        markerMetMelding(m, "bindt geen kenmerk en geen rol");
        assertTrue(sweepMeldingen(m).get(0).message().contains("zeewaardige ploeg"),
                () -> "de melding hoort de geschreven zin te citeren: " + m.diagnostics);
    }

    /** The verb-trailing register of the same refusal — one sentence, the whole voorwaardendeel. */
    @Test void de_vragende_vorm_van_dezelfde_weigering_wordt_ook_gemeld() {
        RegelSpraakModel m = resolveer("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien de Schouwploeg een zeewaardige ploeg is.
            """);
        markerMetMelding(m, "bindt geen kenmerk en geen rol");
    }

    /** §8.1.8.3 "is niet een &lt;naam&gt;" over a name that binds nothing: the positive form is resolved
     *  and then flipped, so the refusal arrives at {@code negeer} with the negation no longer in the
     *  parse tree. Its own reason says so. */
    @Test void een_ontkende_vorm_zonder_binding_wordt_gemeld() {
        RegelSpraakModel m = resolveer("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien hij aan alle volgende voorwaarden voldoet:
                    - hij is niet een zeewaardige ploeg
                    - het wachttal van de Schouwploeg is groter dan 0.
            """);
        markerMetMelding(m, "er is niets om te ontkennen");
    }

    // ---------------- the quantifier-fold membership route ----------------

    /**
     * §8.1.7: a ROLCHECK of the element is a different quantified question, so the membership fold
     * declines it. The card that opened this one measured it as one of four silent declines; it now
     * names the element and the name it could not bind on it.
     */
    @Test void een_rolcheck_van_het_element_meldt_de_as_van_de_weigering() {
        RegelSpraakModel m = resolveer("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien alle wachten van de Schouwploeg gekeurd zijn.
            """);
        markerMetMelding(m, "is geen kenmerk van Strandwacht");
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.message().contains("kwantorvouw")),
                () -> "de vouw hoort haar eigen weigering te noemen: " + m.diagnostics);
    }

    /**
     * A MULTI-STEP written range: the fold spells one relation, so binding the first step would
     * quantify a set the sentence does not name. The decline was silent; it now says which phrase
     * stacked the second set.
     */
    @Test void een_meerstapsbereik_meldt_de_gestapelde_verzameling() {
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
        markerMetMelding(m, "meerstapsbereik");
    }

    /**
     * The bare definite "de" over a PLURAL member phrase reads as a quantification but writes no
     * quantor, so there is nothing to fold. The sentence used to fall to a marker in silence.
     */
    @Test void een_kale_de_over_een_meervoud_wordt_gemeld() {
        RegelSpraakModel m = resolveer("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien de wachten van de Schouwploeg gediplomeerd zijn.
            """);
        markerMetMelding(m, "geen kwantor");
    }

    /** The element type resolves — "Strandwachten" is its declared meervoud — but NO feittype joins it
     *  to the subject, so the range of the quantifier cannot be bound. The fold declines rather than
     *  guess a relation, and now says which two types it found nothing between. */
    @Test void een_onbindbaar_bereik_wordt_gemeld() {
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
        markerMetMelding(m, "het bereik van de kwantor is niet te binden");
    }

    // ---------------- the expression routes ----------------

    /** A bare name that is no parameter, no "Daarbij geldt:" variable and no attribute of the subject
     *  became an {@code Expr.Onopgelost} with nothing said. */
    @Test void een_onbindbare_naam_in_een_expressie_wordt_gemeld() {
        RegelSpraakModel m = resolveer("""
            Regel wachttal
                geldig altijd
                    Het wachttal van een Schouwploeg moet berekend worden als de vloothoogte.
            """);
        markerMetMelding(m, "geen gedeclareerde parameter");
    }

    // ---------------- what must NOT happen ----------------

    /**
     * The anti-test of the whole design: a membership fold that LANDS produces a marker on the way
     * (the kenmerkcheck of the element binds nothing against the rule subject) and must stay clean.
     * A diagnostic raised at the construction instead of over the survivors would report this
     * sentence — and every golden that writes one — as unresolved.
     */
    @Test void een_gelande_vouw_meldt_niets() {
        RegelSpraakModel m = resolveer("""
            Regel bevoegde ploeg
                geldig altijd
                    Een Schouwploeg is bevoegd
                    indien alle wachten van de Schouwploeg gediplomeerd zijn.
            """);
        assertFalse(emitCompact(m).contains("Onopgelost"),
                () -> "de vouw hoort te landen:\n" + emitCompact(m));
        assertTrue(m.diagnostics.isEmpty(), () -> "een gelande vouw meldt niets: " + m.diagnostics);
    }

    /** A route that already speaks keeps its own text and gets no second WARNING — that is what the
     *  marker's {@code reden == null} buys. */
    @Test void een_luide_route_wordt_niet_twee_keer_gemeld() {
        RegelSpraakModel m = resolveer("""
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

    /** The diagnostics ride the v0 wire (the emitter writes them), so the sweep must raise NOTHING on
     *  a document without markers — otherwise every golden's ir.json would move. */
    @Test void een_schoon_document_krijgt_geen_enkele_melding() {
        RegelSpraakModel m = resolveer("""
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
