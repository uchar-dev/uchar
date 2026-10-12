package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * An accumulate over the objects {@code bron} matches, which binds a row also over no object: each of {@code functies}
 * folds the matched objects, or a value the pattern binds of each, into a variable of the rule.
 */
public record Accumulatie(Patroon bron, List<Functie> functies) implements LhsItem {

    public Accumulatie {
        functies = List.copyOf(functies);
        if (functies.isEmpty()) throw new IllegalArgumentException("een accumulatie vouwt met ten minste één functie");
    }

    /** {@code soort} over the matched object itself where {@code bron} is null, else over the variable {@code bron}
     *  that the pattern binds, into the variable {@code doel}. */
    public record Functie(Soort soort, String bron, String doel) {}

    /** The folds the branch uses: the number of objects, the exact sum of numbers, the least and the greatest value,
     *  and the list of the objects. Over no object the count is 0, the sum 0, the least and the greatest null and the
     *  list empty. */
    public enum Soort { AANTAL, SOM, MINIMUM, MAXIMUM, LIJST }
}
