package dev.uchar.regelspraak.transpiler.tms;

/**
 * The judgement of a condition that is neither established true nor established false (RUNTIME-SEMANTICS ch. 1.5, "The
 * judgement of a condition"): one value of Kleene's strong table. A condition whose judgement is established returns its
 * truth value; one that is undetermined throws this, so that a composite counts it as its member ({@link Composite}) and
 * the test of a rule asks of it what its role names ({@link ComputeHelpers#establishedTrue}, {@link ComputeHelpers#judge}).
 * What the condition deposited stands: what an open read inside it deposited was dropped at its own boundary
 * ({@link OpenRead}). {@code read} is the open read that left it so. A signal, not a diagnosis: no stack trace.
 */
public final class Undetermined extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final transient OpenMember read;

    Undetermined(OpenMember read) {
        super(null, null, false, false);
        this.read = read;
    }

    @Override public String getMessage() {
        return "undetermined: " + read;
    }

    /** The open read that left the judgement so. */
    public OpenMember read() {
        return read;
    }
}
