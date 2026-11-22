package dev.uchar.regelspraak.transpiler.tms;

/**
 * An evaluation read an open value (RUNTIME-SEMANTICS ch. 1.5, "An open read"): the read of {@code member} broke it off,
 * so the expression has no value and what it deposited up to there is dropped. Thrown by {@link ComputeHelpers#read}
 * and caught at the boundary of one atomic condition or one value: a member of a composite
 * ({@link Composite#read(RuntimeException)}), the test of a rule's condition, the operand of a test on leeg and gevuld
 * ({@link ComputeHelpers#closed}) and the value of a consequence, where the writer leaves its target open
 * ({@link LeftOpen}). A signal, not a diagnosis: no stack trace.
 */
public final class OpenRead extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final transient OpenMember member;

    OpenRead(OpenMember member) {
        super(member.toString(), null, false, false);
        this.member = member;
    }

    /** The member the evaluation read open, on its object. */
    public OpenMember member() {
        return member;
    }
}
