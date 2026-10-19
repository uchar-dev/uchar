package dev.uchar.regelspraak.transpiler.tms;

import java.util.ArrayList;
import java.util.List;

/**
 * Two or more evaluation faults that stand at the fixpoint of one session of the exec-model branch
 * ({@link Fault}). A single fault is thrown as the exception it is; several are thrown as this exception, ordered by the
 * name of the form and then by the sentence, so the report does not depend on the order of the rules in the model or of
 * the firings. Its message is a head ({@link #head}) that names the rule of every fault, then the sentences of the
 * faults ({@link #sentences}). A report that bounds its length bounds the sentences and keeps the head whole, as the CLI's
 * ERROR line does: the bound may shorten a sentence, but no fault drops off the report. The first fault in that order
 * is the cause, and the others are suppressed exceptions.
 */
public final class EvaluationFaults extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String head;
    private final String sentences;
    private final transient List<RuntimeException> faults;

    /** {@code faults} in the order of the report. */
    EvaluationFaults(List<Fault> faults) {
        this(head(faults), sentences(faults), faults);
    }

    private EvaluationFaults(String head, String sentences, List<Fault> faults) {
        super(head + ": " + sentences, faults.get(0).fault());
        if (faults.size() < 2) throw new IllegalArgumentException("één fout wordt geworpen zoals ze is: " + faults);
        this.head = head;
        this.sentences = sentences;
        this.faults = faults.stream().map(Fault::fault).toList();
        for (RuntimeException f : this.faults.subList(1, this.faults.size())) addSuppressed(f);
    }

    /** Each rule once, with the number of its faults where it has more than one: the faults of one rule are adjacent in
     *  the order of the report, so the head grows with the rules of the document and not with the objects of the case. */
    private static String head(List<Fault> faults) {
        List<String> rules = new ArrayList<>();
        for (int i = 0; i < faults.size(); ) {
            String rule = faults.get(i).rule();
            int n = 0;
            for (; i < faults.size() && faults.get(i).rule().equals(rule); i++) n++;
            rules.add("\"" + rule + "\"" + (n > 1 ? " (" + n + "×)" : ""));
        }
        return "aan het fixpunt staan " + faults.size() + " evaluatiefouten, in de regel" + (rules.size() > 1 ? "s " : " ")
                + String.join(", ", rules);
    }

    private static String sentences(List<Fault> faults) {
        return String.join("; ", faults.stream().map(Fault::message).toList());
    }

    /** The head of the message: the number of faults and the name of the form of each, in the order of the report. */
    public String head() {
        return head;
    }

    /** The sentences of the faults, in the order of the report, the rest of the message after the head. */
    public String sentences() {
        return sentences;
    }

    /** The faults, in the order of the message. */
    public List<RuntimeException> faults() {
        return faults;
    }
}
