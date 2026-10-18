package dev.uchar.regelspraak.transpiler.tms;

/**
 * The barrier of a {@code Let} (RUNTIME-SEMANTICS ch. 10.6, clause 2): a read of the binder whose value is leeg ends
 * that {@code Let} with leeg, also from inside a nested one. The barrier names the cell of the {@code Let} whose binder
 * was read ({@link ComputeHelpers#binderCell}), so the evaluation of that {@code Let} recognises it as its own
 * ({@link ComputeHelpers#intercepted}) and every other passes it on. Public, so that a function of a DRL text catches it by
 * its name. It carries no stack trace: it is a jump within one evaluation, never a fault.
 */
public final class EmptyBarrier extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Object cell;

    EmptyBarrier(Object cell) {
        super(null, null, false, false);
        this.cell = cell;
    }

    /** The cell of the {@code Let} this barrier ends. */
    public Object cell() {
        return cell;
    }
}
