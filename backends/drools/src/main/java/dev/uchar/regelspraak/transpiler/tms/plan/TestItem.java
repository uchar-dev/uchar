package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * The test of the rule over the values at {@code slots}, as one constraint with its own {@code id} (see {@link Alpha}),
 * after the patterns that bind them. {@code toets} is the condition as a tree of calls over a value array of
 * {@code grootte} slots with the read values at {@code slots}, a truth value, from which a rendering into the executable
 * model makes a predicate and one into DRL text makes the calls. {@code role} says what the test asks of the condition,
 * which a rendering cannot tell from the condition itself: that it holds, that it holds without a fault, that it
 * faults, or that it deposits, with the probe in slot {@code depositFlag} (-1 for every other role).
 */
public record TestItem(String id, Role role, Term test, int depositFlag, List<Integer> slots, int size)
        implements LhsItem {

    public TestItem {
        slots = List.copyOf(slots);
        if ((role == Role.DEPOSIT) != (depositFlag >= 0))
            throw new IllegalArgumentException("alleen een deponeringstoets heeft een sonde, en zij altijd: " + role
                    + " met sonde " + depositFlag);
    }

    /**
     * What the test asks of the condition (RUNTIME-SEMANTICS ch. 5.2, 1.2). An evaluation fault is a division by a
     * numeric zero or a fault of the branch's own ({@code ComputeFault}, {@code InputFormFault}); any other exception is a
     * broken contract and leaves the test as it is.
     */
    public enum Role {
        /** The condition holds; it cannot fault. */
        CONDITION,
        /** The condition holds and its evaluation does not fault: where it faults, the rule of the fault takes it
         *  ({@link #FAULT}), and this test does not hold. */
        WITHOUT_FAULT,
        /** The evaluation of the condition faults: the test of the rule that inserts the fault. */
        FAULT,
        /** The evaluation of the condition reaches a place that deposits (§6.5, §6.6): the test puts a fresh probe in
         *  slot {@code depositFlag}, evaluates the condition, and holds where the probe heard a place; a fault after it leaves
         *  the deposit heard. */
        DEPOSIT,
        /** A test the form composes itself, no condition of the IR: whether a key takes part, whether an input is the
         *  asOfDate-bucket scalar a form cannot read, whether a member's value faults. It holds where the predicate
         *  holds. */
        VORM
    }
}
