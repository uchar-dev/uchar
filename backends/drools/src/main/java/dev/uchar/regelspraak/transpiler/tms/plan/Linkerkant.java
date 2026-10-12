package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * The left-hand side of one rule of the form {@code vorm} as the analysis plans it: the slots of its value array and
 * the items that bind them, in order.
 *
 * <p>Until step 1c of card drl-teksttak-op-truth-maintenance the plan holds what {@code TmsTranspiler.Lezingen}
 * decides: the subject pattern with the values bound off it, the parameter and standing patterns, the accumulates and
 * the test. What the forms add to a rule beside it still goes into the executable model directly, without a plan: the
 * patterns before and after the subject pattern (the fold's subject and its range), constraints on the subject pattern,
 * the control facts of a default, and the patterns a form puts after the left-hand side.
 */
public record Linkerkant(String vorm, Leestabel lezingen, List<LhsItem> items) {

    public Linkerkant {
        items = List.copyOf(items);
    }
}
