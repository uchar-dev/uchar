package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * An accumulate over the objects {@code source} matches, which binds a row also over no object: each of {@code aggregations}
 * folds the matched objects, or a value the pattern binds of each, into a variable of the rule.
 */
public record Accumulation(FactPattern source, List<Aggregation> aggregations) implements LhsItem {

    public Accumulation {
        aggregations = List.copyOf(aggregations);
        if (aggregations.isEmpty()) throw new IllegalArgumentException("een accumulatie vouwt met ten minste één functie");
    }

    /** {@code category} over the matched object itself where {@code source} is null, else over the variable {@code source}
     *  that the pattern binds, into the variable {@code target}. */
    public record Aggregation(Kind category, String source, String target) {}

    /** The folds the branch uses: the number of objects, the exact sum of numbers, the least and the greatest value,
     *  and the list of the objects. Over no object the count is 0, the sum 0, the least and the greatest null and the
     *  list empty. */
    public enum Kind { COUNT, SUM, MINIMUM, MAXIMUM, LIST }
}
