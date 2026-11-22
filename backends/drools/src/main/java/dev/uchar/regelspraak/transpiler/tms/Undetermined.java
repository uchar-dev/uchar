package dev.uchar.regelspraak.transpiler.tms;

/**
 * The judgement of a condition where it is not established for another form (RUNTIME-SEMANTICS ch. 1.5, "The judgement
 * of a condition", "For the form itself and for another"): undetermined for the form itself as well
 * ({@code own} null), or established for the form itself, true or false, by a closure over an open value, a test on leeg or
 * gevuld or a fold that closed ({@link ComputeHelpers#atom}). A condition
 * whose judgement is established for both returns its truth value; one whose judgement is not throws this, so that
 * a composite counts it as its member ({@link Composite}) and the test of a rule asks of it what its role names
 * ({@link ComputeHelpers#holdsForItself}, {@link ComputeHelpers#judge}). What the condition deposited stands: what an
 * open read inside it deposited was dropped at its own boundary ({@link OpenRead}). {@code read} is the open read that
 * left it so, for a closure the read it closed over. A signal, not a diagnosis: no stack trace.
 */
public final class Undetermined extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Boolean own;
    private final transient OpenMember read;

    Undetermined(Boolean own, OpenMember read) {
        super(null, null, false, false);
        this.own = own;
        this.read = read;
    }

    @Override public String getMessage() {
        return (own == null ? "undetermined" : own) + " for the form itself, undetermined for another: " + read;
    }

    /** The truth for the form itself, null where it is undetermined. */
    public Boolean own() {
        return own;
    }

    /** The open read that left the judgement so, for a closure the read it closed over. */
    public OpenMember read() {
        return read;
    }
}
