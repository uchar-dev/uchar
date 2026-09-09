package io.github.yhavinga.regelspraak.transpiler.kern;

import io.github.yhavinga.regelspraak.model.Eenheid;

/**
 * The fault a restatement site raises when the two units it must reconcile <b>cannot</b> be reconciled: no
 * shared canonical, hence no conversion factor, hence no honest way to put one value in the other's unit.
 *
 * <p><b>Why this exists.</b> {@link EffectieveEenheid} derives the unit of a reference position from the
 * DECLARATION it reads (RUNTIME-SEMANTICS ch. 2.10), and that derivation was already in place. What was
 * missing is what the derivation is FOR. Every restatement helper in both transpiler branches guarded with
 * one composite test:
 *
 * <pre>if (ev == null || en == null || !ev.convertibleNaar(en) || !ev.vereistOmrekening(en)) return identity;</pre>
 *
 * That collapses two different facts into one answer. "No conversion is due" (same unit, or a unitless side)
 * and "no conversion EXISTS" (two units with disjoint canonicals) both returned the identity, so a document
 * whose declarations make a join impossible produced exactly the numbers a consistent document produces.
 * Measured on a copy of {@code corpus/golden/jutterswet-schouwrooster} whose declaration of one addend was
 * moved to {@code uur} (factor 3600 to {@code seconde}) while the annotations stayed {@code schouwdagen}:
 * the verifier failed WF-091, {@code ref} faulted ("incompatible units: schouwdagen vs uur"), clingo faulted
 * ahead of translation, and this backend replayed both cases against the untouched golden's pinned values.
 * The same copy with a CONVERTIBLE sister declaration (factor 0,5) moved the outcome from 15 to 13,5 — proof
 * that the declaration WAS consulted, and that the silence was this collapsed guard alone, not a wire read.
 *
 * <p><b>The register is ref's.</b> ref reconciles at exactly these sites — an additive join, a comparison, the
 * write boundary, a timeline restatement — through one {@code restate_into}, and faults when the pair does not
 * convert. This backend now faults at the same sites, so the third leg of the differential square answers a
 * broken document the way the other two do instead of answering it with a number.
 *
 * <p><b>The three exemptions</b>, each because there is genuinely nothing to reconcile:
 * <ul>
 *   <li>a UNITLESS side ({@code null}): ch. 2.6 lets it adopt the other unit, value unchanged — ref does the
 *       same, and no factor is due;</li>
 *   <li>an OPAQUE PRODUCT unit ("€·kg", what {@link Eenheid#product} builds for a two-unit MAAL): it carries no
 *       canonical by construction, so no factor is invented and the value simply stands in the unit it was
 *       computed in. RUNTIME-SEMANTICS ch. 2.10 records that stance for this backend explicitly — a silent
 *       divergence on a shape the corpus does not carry and no frontend can emit — and faulting on it here
 *       would make that recorded sentence false. A bare wire unit whose abbreviation happens to contain "·"
 *       and which declares neither canonical nor factor falls in the same class and is treated the same way:
 *       conservative, never rescaled.</li>
 *   <li>the §7.1 CANCELLATION shape — a rate against its own numerator unit, "€/mnd" against "€" — where a
 *       total over the denominator's granularity has already consumed that denominator. See
 *       {@link #afgetopteNoemer}, which carries the measurement and names the two corpus goldens.</li>
 * </ul>
 * A unit that declares a canonical but no factor is NOT exempt: {@link Eenheid#omrekenfactorNaar} already
 * faults there (the WF-092 wire form), and it must keep doing so.
 */
public final class Eenheidsconflict extends RuntimeException {

    private final transient Eenheid van;
    private final transient Eenheid naar;
    private final String plek;

    private Eenheidsconflict(Eenheid van, Eenheid naar, String plek) {
        super("onverenigbare eenheden bij " + plek + ": '" + van.weergave() + "' (canoniek " + van.canoniek()
                + ") herschaalt niet naar '" + naar.weergave() + "' (canoniek " + naar.canoniek()
                + ") — er bestaat geen omrekenfactor");
        this.van = van;
        this.naar = naar;
        this.plek = plek;
    }

    /**
     * Guard a restatement of a {@code van}-unit quantity into {@code naar}: raise when the two are unit-bearing,
     * concrete and do not inter-convert. Called at the HEAD of each restatement helper, before its own
     * "no conversion due" test, so the two facts that test conflates stay apart.
     *
     * @param plek the site, for the message — the fault names where it stood, the way the verifier names a path.
     */
    public static void eisHerschaalbaar(Eenheid van, Eenheid naar, String plek) {
        if (van == null || naar == null) return;              // a unitless side adopts the other (ch. 2.6)
        if (van.convertibleNaar(naar)) return;                // a factor exists (or the two are the same unit)
        if (opaakProduct(van) || opaakProduct(naar)) return;   // ch. 2.10's recorded conservative no-op
        if (afgetopteNoemer(van, naar)) return;                // the §7.1 cancellation, see below
        throw new Eenheidsconflict(van, naar, plek);
    }

    /**
     * The §7.1 CANCELLATION shape: exactly one side is a rate, the other is plain, and their NUMERATORS
     * inter-convert — "€/mnd" against "€". A total over the denominator's granularity consumes that
     * denominator, so the restatement that follows lands on the identity BY DESIGN and is not a mismatch.
     *
     * <p>This is an exemption rather than a derivation because of WHERE the two consumers disagree. {@code ref}
     * derives the fold's result unit as the numerator and never reaches a restatement at all; this backend
     * derives it as operand 0's unit (§4.6 <i>Eenheid1</i>, what {@link EffectieveEenheid} implements) and so
     * still holds the rate when the write boundary asks. Measured 2026-09-09, the two corpus goldens that
     * exercise it: {@code jutterswet-tempototaal} ("bewaargeld", a TOTAAL of a €/mnd attribute into a plain €
     * doel) and {@code jutterswet-tempovouw} ("bewaarlast"). Both replay against their pinned values today
     * precisely because the restatement degrades to the identity.
     *
     * <p>Narrowing the derivation instead — the honest repair — needs to tell a TIMELINE total from a SET total,
     * and the two are indistinguishable in the expression alone: the same golden carries a set TOTAAL of the
     * same €/mnd attribute into a €/mnd doel, and both shapes occur with {@code set} true and false. That
     * context lives in the transpiler, not in the expression walker, so it is a separate card. Until then the
     * shape is exempt and said out loud here rather than answered with a fault the corpus contradicts.
     *
     * <p>The exemption is narrow: it requires convertible numerators, so "€/mnd" against "kg" still faults.
     */
    private static boolean afgetopteNoemer(Eenheid van, Eenheid naar) {
        return (van.noemer() == null) != (naar.noemer() == null)
                && van.teller().convertibleNaar(naar.teller());
    }

    /** An opaque product as {@link Eenheid#product} builds it: no canonical, no factor, no denominator, and an
     *  abbreviation that is the "·"-join of its factors. */
    private static boolean opaakProduct(Eenheid e) {
        return e.canoniek() == null && e.factorNaarCanoniek() == null && e.noemer() == null
                && e.afkorting() != null && e.afkorting().contains("·");
    }

    public Eenheid van() { return van; }

    public Eenheid naar() { return naar; }

    /** The restatement site the fault stood at. */
    public String plek() { return plek; }
}
