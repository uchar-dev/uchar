package dev.uchar.regelspraak.ir;

import java.time.LocalDate;

/**
 * §13.4.2.5 rule-version validity: the period in which a rule version applies. Versions
 * of one rule are selected at runtime against the Rekendatum (§4.2) — the version whose
 * window contains the Rekendatum is the one that fires.
 *
 * {@code from} is the inclusive lower bound (null = open, from the beginning) and
 * {@code until} the inclusive upper bound (null = open, "t/m" being inclusive). "Geldig
 * altijd" is both-null. A year shorthand widens to the whole year — "vanaf 2024" is
 * 2024-01-01, "t/m 2024" is 2024-12-31 — so the bounds are always concrete dates.
 */
public record Validity(LocalDate from, LocalDate until) {
    public static final Validity ALTIJD = new Validity(null, null);
    public boolean always() { return from == null && until == null; }

    /**
     * Whether {@code d} falls within this window — both bounds INCLUSIVE, a null bound OPEN (matching every
     * date on that side). {@link #ALTIJD} contains every date. Used by parameter-vintage selection to pick
     * the interval that applies on a peildatum (see {@link dev.uchar.regelspraak.model.ParameterSet}).
     */
    public boolean contains(LocalDate d) {
        if (d == null) return false;
        if (from != null && d.isBefore(from)) return false;
        if (until != null && d.isAfter(until)) return false;
        return true;
    }
}
