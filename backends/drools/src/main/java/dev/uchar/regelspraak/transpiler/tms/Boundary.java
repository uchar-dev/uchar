package dev.uchar.regelspraak.transpiler.tms;

/**
 * The boundary of one value or one atomic condition under evaluation (RUNTIME-SEMANTICS ch. 1.5, "A closure inside a
 * value"): where a fold inside it applied a closure to an open value, what the evaluation gives stands for the form itself
 * and not for another, so the fold tells the innermost boundary under way ({@link #closes}) and whoever opened it asks
 * ({@link #closed}). The boundaries of one evaluation nest on its thread. A value that an open read breaks off or that
 * faults is no outcome, so what its boundary heard goes with it.
 */
final class Boundary implements AutoCloseable {
    private static final ThreadLocal<Boundary> INNERMOST = new ThreadLocal<>();

    private final Boundary outer = INNERMOST.get();
    private OpenMember closed;

    Boundary() {
        INNERMOST.set(this);
    }

    /** A closure over {@code read} was applied in the evaluation under way. */
    static void closes(OpenMember read) {
        Boundary b = INNERMOST.get();
        if (b == null)
            throw new IllegalStateException("a closure over the " + read + " outside the boundary of one value or one"
                    + " judgement, where no form would learn that its outcome does not stand for another");
        if (b.closed == null) b.closed = read;
    }

    /** The read the first closure applied within this boundary closed over, null where none was applied. */
    OpenMember closed() {
        return closed;
    }

    @Override public void close() {
        if (outer == null) INNERMOST.remove();
        else INNERMOST.set(outer);
    }
}
