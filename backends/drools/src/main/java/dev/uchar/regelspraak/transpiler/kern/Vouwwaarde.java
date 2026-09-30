package dev.uchar.regelspraak.transpiler.kern;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

/**
 * Whether a VALUE fold over a set source can be accumulated at all, and why not when it cannot.
 *
 * <p>Both branches accumulate a value fold in {@code BigDecimal}: the exec-model one binds a
 * {@code BigDecimal} variable and hands it to Drools' BigDecimal sum/min/max functions, the DRL-text
 * one emits {@code sum(…)}/{@code min(…)}/{@code max(…)} over the same getter. That is right for the
 * only member type the §6 exact-precision invariant maps to a {@code BigDecimal} getter — a numeric
 * one — and wrong for every other, where it does not decline but CRASHES. Measured 2026-09-19 and
 * remeasured 2026-09-20 on verifier-CLEAN documents: a DATE member gave the exec branch a
 * {@code ClassCastException} (LocalDate to BigDecimal) on som/totaal and an
 * {@code IllegalStateException} out of the accumulate cast on minimum/maximum, while the DRL-text
 * branch emitted text that did not compile at all ("Unable to Analyse Expression"); a TEXT member
 * gave the same pair with {@code String}.
 *
 * <p>A crash is not a refusal. It names no form, it reaches the gate as an ERROR rather than as a
 * channel with a reason, and the same document would look identical whether the backend cannot carry
 * the form or is simply broken. So the question is asked HERE, once, before either branch lowers
 * anything, and both refuse in the same words.
 *
 * <p>The reason is deliberately split in two, because the two halves are different claims. For the
 * ADDITIVE kinds the form has no reading at all — adding two calendar positions is not a position,
 * and {@code ref}, the arbiter, faults on the addition itself. For the ORDERING kinds the form IS
 * well defined ({@code spec/RUNTIME-SEMANTICS.md} ch. 1.2e — a fold that selects one contributed
 * value, which {@code ref} and the ASP backend both carry over a date source, witness
 * {@code corpus/golden/jutterswet-vondstbereik}). Since 2026-09-30 the exec-model branch carries it
 * too, in a rule's value, and does not ask this question for it; the DRL-text branch is the one that
 * does not carry it, and its sentence says so. Whoever reads a gate report and decides what to card
 * next needs to see which of the two it is.
 */
public final class Vouwwaarde {
    private Vouwwaarde() {}

    /**
     * Why neither branch folds this set source, or null when it folds fine.
     *
     * @param f    the aggregation node; only the VALUE-folding kinds are asked about, since a count
     *             folds the members and never reads their value
     * @param elem the element object type — the declaration anchor of a SET read, so an inherited
     *             attribute answers on the type that declares it (ch. 2.2)
     * @param arg  the folded member read; its own object type is consulted when the element type
     *             does not declare the name, so the answer never depends on which of the two
     *             equivalent anchors a document happens to carry
     */
    public static String weigering(Expr.Functie f, Objecttype elem, Expr.AttribuutRef arg) {
        if (arg == null || arg.attribuut() == null) return null;
        boolean optellend = f.kind() == Expr.FunctieKind.SOM || f.kind() == Expr.FunctieKind.TOTAAL;
        boolean ordenend = f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.MAXIMUM;
        if (!optellend && !ordenend) return null;
        String attr = arg.attribuut();
        Datatype dt = declaratie(elem, attr);
        if (dt == null) dt = declaratie(arg.objecttype(), attr);
        if (dt == null || dt.isGetalachtig()) return null;
        if (elem == null) elem = arg.objecttype();
        String lid = "vouw " + f.kind() + " over de setbron '" + attr + "' van " + elem.naam
                + ": die leden zijn van type " + dt.naam() + " en niet getalachtig, terwijl ";
        // Over a DATE the ordering reading exists and three executors carry it; only the DRL-text branch asks this
        // question for it (the exec-model branch folds it in a rule's value, ExecModelTranspiler.vouwslot).
        if (ordenend && dt.isDatum())
            return lid + "de DRL-teksttak elke waardevouw in BigDecimal accumuleert. De ordenende lezing KIEST één"
                    + " bijgedragen waarde (ch. 1.2e) en is over een datumbron welgedefinieerd — ref, de ASP-backend en de"
                    + " exec-modeltak (in de waarde van een regel) dragen haar, getuige golden/jutterswet-vondstbereik —"
                    + " maar de DRL-teksttak heeft haar niet gebouwd. Een gat in die tak, geen semantisch verschil (§5.8.3)";
        String kop = lid + "beide takken elke waardevouw in BigDecimal accumuleren";
        if (optellend)
            return kop + ". Optellen is rekenkunde en alleen bedragen tellen op; ref werpt op de optelling"
                    + " zelf een typefout. Geen enkele uitvoerder draagt deze vorm (§5.8.2/§4.20)";
        // The ordering tail is chosen on the MEMBER TYPE and never on the kind, because the claim it
        // makes is about the member type. Over a DATE (above) the reading exists and the DRL-text branch
        // is the one that is behind; over anything else non-numeric NOBODY carries it
        // (measured 2026-09-20 on a text source: ref "unordered comparison", clingo declines, and the
        // verifier refuses the document outright under WF-038 — an ordering fold demands a number or
        // a date). Choosing on the kind said the first sentence in both cases, which was simply untrue
        // of the second, and a refusal that misdiagnoses points its reader at the wrong card.
        return kop + ". De ordenende lezing vraagt een ORDENING op de leden en dit type heeft er geen:"
                + " geen enkele uitvoerder draagt de vorm, en de verifier laat het document niet eens"
                + " door (WF-038 eist een getal of een datum onder een ordenende vouw). Deze weigering"
                + " staat als tweede verdedigingslinie (§5.8.3)";
    }

    /**
     * Why the §7.1 TOTAAL over a TIMELINE (own, navigated or gedurende-clipped) cannot be folded, or null
     * when it can — the timeline half of {@link #weigering}.
     *
     * <p>{@code rsTijdTotaal} multiplies each segment value by the grid periods it spans and returns a
     * {@code BigDecimal}; over a non-numeric declaration the DRL-text branch emitted text that did not
     * compile ("Unable to Analyse Expression if (rsTijdTotaal(…"), measured 2026-09-23 on a verifier-CLEAN
     * document, which reaches a report as a whole-file compile refusal naming no form. {@code ref} faults
     * on the multiplication itself, so no executor carries the form. Both branches ask it of the
     * declaration: the DRL-text branch markers the form, the exec branch skips the rule through
     * {@code tijdreeksTotaal}. WF-091 rejects such a document.
     *
     * @param ot  the declaring anchor the caller reads the granularity from
     * @param arg the totalled timeline read
     */
    public static String tijdlijnWeigering(Expr.Functie f, Objecttype ot, Expr.AttribuutRef arg) {
        if (arg == null || arg.attribuut() == null || ot == null) return null;
        Datatype dt = declaratie(ot, arg.attribuut());
        if (dt == null || dt.isGetalachtig()) return null;
        return "vouw " + f.kind() + " over de tijdlijn '" + arg.attribuut() + "' van " + ot.naam
                + ": de segmentwaarden zijn van type " + dt.naam() + " en niet getalachtig, terwijl de"
                + " tijdlijnvouw elke segmentwaarde met haar aantal roosterperioden vermenigvuldigt."
                + " Vermenigvuldigen is rekenkunde en alleen bedragen schalen; ref werpt op de"
                + " vermenigvuldiging zelf een typefout. Geen enkele uitvoerder draagt deze vorm (§7.1/ch. 3.6)";
    }

    /** The chain-closed declaration of {@code attr} on {@code ot}, asked through the shared name
     *  contract so a name written in another capitalisation resolves exactly as every reflective
     *  read of it does. */
    private static Datatype declaratie(Objecttype ot, String attr) {
        if (ot == null) return null;
        return ot.attribuutLeden().get(Namen.gedeclareerdeNaam(ot.attribuutLeden().keySet(), attr));
    }
}
