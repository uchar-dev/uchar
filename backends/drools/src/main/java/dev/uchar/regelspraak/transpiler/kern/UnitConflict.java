package dev.uchar.regelspraak.transpiler.kern;

import dev.uchar.regelspraak.model.Eenheid;

/**
 * The fault a restatement site raises when the two units it must reconcile <b>cannot</b> be reconciled: no
 * shared canonical, hence no conversion factor, hence no honest way to put one value in the other's unit.
 *
 * <p><b>Why this exists.</b> {@link EffectiveUnit} derives the unit of a reference position from the
 * DECLARATION it reads (RUNTIME-SEMANTICS ch. 2.10), and that derivation was already in place. What was
 * missing is what the derivation is FOR. Every restatement helper in both transpiler branches guarded with
 * one composite test:
 *
 * <pre>if (ev == null || en == null || !ev.convertibleTo(en) || !ev.requiredConversion(en)) return identity;</pre>
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
 * <p><b>The register is ref's.</b> ref funnels every reconciliation through one {@code restate_into}, called
 * from SEVEN sites: the three additive joins (add, sub, sub_min), a bound, the comparison, the write boundary,
 * and the timeline restatement. It faults when the pair does not convert. This backend now faults across the
 * same span, so the third leg of the differential square answers a broken document the way the other two do
 * instead of answering it with a number.
 *
 * <p><b>Where the fault is raised.</b> Most sites ask at BUILD time, where the pair is already known, and the
 * KieBase assembly is what fails. Two do not: {@code valueFactor} and {@code exactHerschaalFactoren} are also
 * reached from inside a fired consequence, so a document whose only irreconcilable pair sits on that path
 * faults during fire and the CLI reports it wrapped in a {@code ConsequenceException}. That is not a weaker
 * outcome and the choice is not build-time-or-nothing: for the gate an ERROR out of a consequence is exactly as
 * hard as one out of the build — both are the CLI's exit 3, which gate 12 refuses to classify away. Build time
 * is preferred only because the message arrives unwrapped and names the site. Each fence therefore carries its
 * OWN {@code site} text, so the report says which of them fired rather than three of them sharing one line.
 *
 * <p><b>The two exemptions</b>, each because there is genuinely nothing to reconcile:
 * <ul>
 *   <li>a UNITLESS side ({@code null}): ch. 2.6 lets it adopt the other unit, value unchanged — ref does the
 *       same, and no factor is due;</li>
 *   <li>an OPAQUE PRODUCT unit ("€·kg", what {@link Eenheid#product} builds for a two-unit MAAL): it carries no
 *       canonical by construction, so no factor is invented and the value simply stands in the unit it was
 *       computed in. RUNTIME-SEMANTICS ch. 2.10 records that stance for this backend explicitly — a silent
 *       divergence on a shape the corpus does not carry and no frontend can emit — and faulting on it here
 *       would make that recorded sentence false. The exemption keys on PROVENANCE (identity), not on the
 *       abbreviation: a wire-declared unit that merely LOOKS like a product ("appel·peer", no canonical, no
 *       factor) is not one, {@code ref} faults on it, and so does this fence.
 *       <p>It is one side that carries the exemption, so it also covers TWO DIFFERENT derived products meeting
 *       each other — "€·kg" against "€·m" passes, not only a product against a concrete unit. That is the same
 *       ch. 2.10 stance and not a wider one: neither side has a canonical, so there is no factor to invent
 *       either way, and the value stands in the unit it was computed in. What it costs is a diagnosis — two
 *       products that could never be reconciled are not reported as such — and that limit is named here rather
 *       than left to be discovered. Reaching it needs a document carrying two distinct two-unit MAAL results at
 *       one restatement site, which no frontend emits and the corpus does not hold.</li>
 * </ul>
 *
 * <p><b>What is NOT exempt, and used to be.</b> A rate against a plain unit with convertible numerators
 * ("€/mnd" against "€") was exempted here until 2026-09-09, to keep two §7.1 goldens green. That exemption was
 * place-blind: it applied at all eight fence positions, so an additive join, a write boundary and a comparison
 * between a rate and its own numerator all stayed silent while {@code ref} and clingo faulted. The repair is a
 * derivation, not an exemption — {@link EffectiveUnit#afterTimeFolding} makes a TOTAAL over a
 * timeline-declared operand yield the numerator, which is what the fold actually produces — so the pair never
 * reaches this fence on those goldens, and everywhere else it faults.
 * A unit that declares a canonical but no factor is NOT exempt: {@link Eenheid#conversionFactorTo} already
 * faults there (the WF-092 wire form), and it must keep doing so.
 */
public final class UnitConflict extends RuntimeException {

    private final transient Eenheid van;
    private final transient Eenheid naar;
    private final String site;

    private UnitConflict(Eenheid van, Eenheid naar, String site) {
        super("onverenigbare eenheden bij " + site + ": '" + van.weergave() + "' (canoniek " + van.canoniek()
                + ") herschaalt niet naar '" + naar.weergave() + "' (canoniek " + naar.canoniek()
                + ") — er bestaat geen omrekenfactor");
        this.van = van;
        this.naar = naar;
        this.site = site;
    }

    /**
     * Guard a restatement of a {@code van}-unit quantity into {@code naar}: raise when the two are unit-bearing,
     * concrete and do not inter-convert. Called at the HEAD of each restatement helper, before its own
     * "no conversion due" test, so the two facts that test conflates stay apart.
     *
     * @param plek the site, for the message — the fault names where it stood, the way the verifier names a path.
     */
    public static void requireRescalable(Eenheid van, Eenheid naar, String site) {
        if (van == null || naar == null) return;              // a unitless side adopts the other (ch. 2.6)
        if (van.convertibleTo(naar)) return;                // a factor exists (or the two are the same unit)
        if (opaqueProduct(van) || opaqueProduct(naar)) return;   // ch. 2.10's recorded conservative no-op
        throw new UnitConflict(van, naar, site);
    }

    /** An opaque product THIS backend's derivation built — provenance, by identity, never the abbreviation.
     *  See {@link EffectiveUnit#isDerivedProduct} for why the name cannot carry this. */
    private static boolean opaqueProduct(Eenheid e) {
        return EffectiveUnit.isDerivedProduct(e);
    }

    public Eenheid van() { return van; }

    public Eenheid naar() { return naar; }

    /** The restatement site the fault stood at. */
    public String site() { return site; }
}
