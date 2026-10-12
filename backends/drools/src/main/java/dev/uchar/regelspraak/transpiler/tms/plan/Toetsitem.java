package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;
import java.util.function.Predicate;

/**
 * The test of the rule's condition over the values at {@code slots}, as one constraint with its own {@code id} (see
 * {@link Alfa}), after the patterns that bind them.
 *
 * <p>INTERIM FORM, until step 1b of card drl-teksttak-op-truth-maintenance: the test is still the predicate the
 * condition compiler made, over a value array of {@code grootte} slots with the read values at {@code slots}, and no
 * rendering can show what it tests. Step 1b replaces {@code gecompileerd} by the condition as a tree of calls (a
 * {@code Term}), from which a rendering into the executable model makes this predicate and one into DRL text makes the
 * calls.
 */
public record Toetsitem(String id, Predicate<Object[]> gecompileerd, List<Integer> slots, int grootte)
        implements LhsItem {

    public Toetsitem {
        slots = List.copyOf(slots);
    }
}
