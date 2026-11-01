package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.model.Unit;

/**
 * The fault a restatement site raises when the two units it must reconcile <b>cannot</b> be reconciled: no
 * shared canonical, hence no conversion factor, hence no honest way to put one value in the other's unit.
 *
 * <p><b>Why this exists.</b> {@link EffectiveUnit} derives the unit of a reference position from the
 * DECLARATION it reads (RUNTIME-SEMANTICS ch. 2.10); this class is what the derivation is FOR. A restatement
 * helper guarded with one composite test
 *
 * <pre>if (ev == null || en == null || !ev.convertibleTo(en) || !ev.requiredConversion(en)) return identity;</pre>
 *
 * collapses two different facts into one answer. "No conversion is due" (same unit, or a unitless side)
 * and "no conversion EXISTS" (two units with disjoint canonicals) both return the identity, so a document
 * whose declarations make a join impossible produces exactly the numbers a consistent document produces.
 * Measured with that guard alone on a copy of {@code corpus/golden/jutterswet-schouwrooster} whose declaration of one addend was
 * moved to {@code uur} (factor 3600 to {@code seconde}) while the annotations stayed {@code schouwdagen}:
 * the verifier failed WF-091, {@code ref} faulted ("incompatible units: schouwdagen vs uur"), clingo faulted
 * ahead of translation, and this backend replayed both cases against the untouched golden's pinned values.
 * The same copy with a CONVERTIBLE sister declaration (factor 0,5) moved the outcome from 15 to 13,5 — proof
 * that the declaration WAS consulted, and that the silence was this collapsed guard alone, not a wire read.
 *
 * <p><b>The register is ref's.</b> ref funnels every reconciliation through one {@code restate_into}, called
 * from SEVEN sites: the three additive joins (add, sub, sub_min), a bound, the comparison, the write boundary,
 * and the timeline restatement. It faults when the pair does not convert. This backend faults across the
 * same span, so the third leg of the differential square answers a broken document the way the other two do
 * instead of answering it with a number.
 *
 * <p><b>Where the fault is raised.</b> Most sites ask at BUILD time, where the pair is already known, and the
 * KieBase assembly is what fails. Two do not: {@code valueFactor} and {@code exactRestateFactors} are also
 * reached from inside a fired consequence, so a document whose only irreconcilable pair sits on that path
 * faults during fire and the CLI reports it wrapped in a {@code ConsequenceException}. That is not a weaker
 * outcome and the choice is not build-time-or-nothing: for the gate an ERROR out of a consequence is exactly as
 * hard as one out of the build — both are the CLI's exit 3, which gate 12 refuses to classify away. Build time
 * is preferred only because the message arrives unwrapped and names the site. Each fence therefore carries its
 * OWN {@code site} text, so the report says which of them fired rather than three of them sharing one line.
 *
 * <p><b>The one exemption</b>, because there is genuinely nothing to reconcile: a UNITLESS side ({@code null}).
 * ch. 2.6 lets it adopt the other unit, value unchanged — ref does the same, and no factor is due. A product is a unit
 * like any other: it restates where its dimension allows and faults where it does not.
 *
 * <p><b>What is NOT exempt.</b> A rate against a plain unit with convertible numerators ("€/mnd" against "€"). An
 * exemption would be place-blind: it would apply at all eight fence positions, so an additive join, a write boundary
 * and a comparison between a rate and its own numerator would all stay silent while {@code ref} and clingo fault.
 * The two §7.1 goldens that meet the pair rest on a derivation, not an exemption —
 * {@link EffectiveUnit#afterTimeFolding} makes a TOTAAL over a timeline-declared operand yield the numerator, which
 * is what the fold actually produces — so the pair never reaches this fence there, and everywhere else it faults.
 * A unit that declares a canonical but no factor is NOT exempt: {@link Unit#exactRestateFactors} already
 * faults there (the WF-092 wire form), and it must keep doing so.
 */
public final class UnitConflict extends RuntimeException {

    private final transient Unit from;
    private final transient Unit destination;
    private final String site;

    private UnitConflict(Unit from, Unit destination, String site) {
        super("onverenigbare eenheden bij " + site + ": '" + from.rendering() + "' (canoniek " + from.dimensionRendering()
                + ") herschaalt niet naar '" + destination.rendering() + "' (canoniek " + destination.dimensionRendering()
                + ") — er bestaat geen omrekenfactor");
        this.from = from;
        this.destination = destination;
        this.site = site;
    }

    /**
     * Guard a restatement of a {@code from}-unit quantity into {@code destination}: raise when the two are unit-bearing,
     * concrete and do not inter-convert. Called at the HEAD of each restatement helper, before its own
     * "no conversion due" test, so the two facts that test conflates stay apart.
     *
     * @param site the site, for the message — the fault names where it stood, the way the verifier names a path.
     */
    public static void requireRescalable(Unit from, Unit destination, String site) {
        if (from == null || destination == null) return;              // a unitless side adopts the other (ch. 2.6)
        if (from.convertibleTo(destination)) return;                // a factor exists (or the two are the same unit)
        throw new UnitConflict(from, destination, site);
    }

    public Unit from() { return from; }

    public Unit destination() { return destination; }

    /** The restatement site the fault stood at. */
    public String site() { return site; }
}
