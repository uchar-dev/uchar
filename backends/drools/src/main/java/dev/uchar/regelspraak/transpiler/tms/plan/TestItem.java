package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * The test of the rule over the values at {@code slots}, as one constraint with its own {@code id} (see {@link Alpha}),
 * after the patterns that bind them. {@code test} is the condition as a tree of calls over a value array of
 * {@code size} slots with the read values at {@code slots}, a truth value, from which a rendering into the executable
 * model makes a predicate and one into DRL text makes the calls. {@code role} says what the test asks of the condition,
 * which a rendering cannot tell from the condition itself: that it holds, that it holds without a fault, that it
 * leaves its writer undecided, that it faults, or that it deposits, with the probe in slot {@code depositFlag} (-1 for
 * every other role). Where {@code open}, the condition reads a member that can be open, so its judgement can be
 * undetermined (ch. 1.5) and its evaluation can end in an open read; a rendering then answers the role for such an end
 * as well ({@code ComputeHelpers.holdsForItself} and the routes beside it).
 */
public record TestItem(String id, Role role, Term test, int depositFlag, List<Integer> slots, int size, boolean open)
        implements LhsItem {

    public TestItem {
        slots = List.copyOf(slots);
        if ((role == Role.DEPOSIT) != (depositFlag >= 0))
            throw new IllegalArgumentException("only a deposit test has a probe, and it always does: " + role
                    + " with probe " + depositFlag);
        if (role == Role.LEAVES_OPEN && !open || role == Role.VORM && open)
            throw new IllegalArgumentException("a condition whose judgement always stands leaves nothing open, and a test a"
                    + " form composes itself always stands: " + role + (open ? " open" : " established"));
    }

    /** A test of a condition whose judgement always stands. */
    public TestItem(String id, Role role, Term test, int depositFlag, List<Integer> slots, int size) {
        this(id, role, test, depositFlag, slots, size, false);
    }

    /**
     * What the test asks of the condition (RUNTIME-SEMANTICS ch. 5.2, 1.2, 1.5). An evaluation fault is a division by a
     * numeric zero or a fault of the branch's own ({@code ComputeFault}, {@code InputFormFault}); any other exception is a
     * broken contract and leaves the test as it is. A condition holds where it is true for the form itself (ch. 1.5):
     * established, or by a closure over an open value.
     */
    public enum Role {
        /** The condition holds; it cannot fault. */
        CONDITION,
        /** The condition holds and its evaluation does not fault: where it faults, the rule of the fault takes it
         *  ({@link #FAULT}), and this test does not hold. */
        WITHOUT_FAULT,
        /** The condition leaves the writer whose condition it is undecided (ch. 1.5, "A rule"): it is neither true for
         *  the form itself nor false for another form, and its evaluation does not fault. */
        LEAVES_OPEN,
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
