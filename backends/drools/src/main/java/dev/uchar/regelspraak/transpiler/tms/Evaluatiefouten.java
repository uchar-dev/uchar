package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;

/**
 * Two or more evaluation faults that stand at the fixpoint of one session of the truth-maintenance branch
 * ({@link Fout}). A single fault is thrown as the exception it is, the one the exec-model branch names out of its fire;
 * several are thrown as this exception, whose message gives their sentences ordered by the name of the form and then by
 * the sentence, so the report does not depend on the order of the rules in the model or of the firings. The first
 * fault in that order is the cause, and the others are suppressed exceptions.
 */
public final class Evaluatiefouten extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<RuntimeException> fouten;

    Evaluatiefouten(List<RuntimeException> fouten) {
        super("aan het fixpunt staan " + fouten.size() + " evaluatiefouten: "
                + String.join("; ", fouten.stream().map(Throwable::getMessage).toList()), fouten.get(0));
        if (fouten.size() < 2) throw new IllegalArgumentException("één fout wordt geworpen zoals ze is: " + fouten);
        this.fouten = List.copyOf(fouten);
        for (RuntimeException f : fouten.subList(1, fouten.size())) addSuppressed(f);
    }

    /** The faults, in the order of the message. */
    public List<RuntimeException> fouten() {
        return fouten;
    }
}
