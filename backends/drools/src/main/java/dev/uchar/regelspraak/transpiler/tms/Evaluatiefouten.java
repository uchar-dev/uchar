package dev.uchar.regelspraak.transpiler.tms;

import java.util.ArrayList;
import java.util.List;

/**
 * Two or more evaluation faults that stand at the fixpoint of one session of the exec-model branch
 * ({@link Fout}). A single fault is thrown as the exception it is; several are thrown as this exception, ordered by the
 * name of the form and then by the sentence, so the report does not depend on the order of the rules in the model or of
 * the firings. Its message is a head ({@link #kop}) that names the rule of every fault, then the sentences of the
 * faults ({@link #zinnen}). A report that bounds its length bounds the sentences and keeps the head whole, as the CLI's
 * ERROR line does: the bound may shorten a sentence, but no fault drops off the report. The first fault in that order
 * is the cause, and the others are suppressed exceptions.
 */
public final class Evaluatiefouten extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String kop;
    private final String zinnen;
    private final transient List<RuntimeException> fouten;

    /** {@code fouten} in the order of the report. */
    Evaluatiefouten(List<Fout> fouten) {
        this(kop(fouten), zinnen(fouten), fouten);
    }

    private Evaluatiefouten(String kop, String zinnen, List<Fout> fouten) {
        super(kop + ": " + zinnen, fouten.get(0).fout());
        if (fouten.size() < 2) throw new IllegalArgumentException("één fout wordt geworpen zoals ze is: " + fouten);
        this.kop = kop;
        this.zinnen = zinnen;
        this.fouten = fouten.stream().map(Fout::fout).toList();
        for (RuntimeException f : this.fouten.subList(1, this.fouten.size())) addSuppressed(f);
    }

    /** Each rule once, with the number of its faults where it has more than one: the faults of one rule are adjacent in
     *  the order of the report, so the head grows with the rules of the document and not with the objects of the case. */
    private static String kop(List<Fout> fouten) {
        List<String> regels = new ArrayList<>();
        for (int i = 0; i < fouten.size(); ) {
            String regel = fouten.get(i).regel();
            int n = 0;
            for (; i < fouten.size() && fouten.get(i).regel().equals(regel); i++) n++;
            regels.add("\"" + regel + "\"" + (n > 1 ? " (" + n + "×)" : ""));
        }
        return "aan het fixpunt staan " + fouten.size() + " evaluatiefouten, in de regel" + (regels.size() > 1 ? "s " : " ")
                + String.join(", ", regels);
    }

    private static String zinnen(List<Fout> fouten) {
        return String.join("; ", fouten.stream().map(Fout::melding).toList());
    }

    /** The head of the message: the number of faults and the name of the form of each, in the order of the report. */
    public String kop() {
        return kop;
    }

    /** The sentences of the faults, in the order of the report, the rest of the message after the head. */
    public String zinnen() {
        return zinnen;
    }

    /** The faults, in the order of the message. */
    public List<RuntimeException> fouten() {
        return fouten;
    }
}
