package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;

/**
 * An evaluation fault of the exec-model branch other than a division by a numeric zero
 * ({@link Rational.DelingDoorNul}): a date step or a duration count outside the range of RUNTIME-SEMANTICS ch. 3.8, a
 * walk over the days of a day kind that passes the reach of ch. 3.8 without one, a context value, a validity window or
 * the subject's own temporal total without a clip that needs the evaluation date in a session that carries none
 * (ch. 3.6, 8.1, 8.2), the bound day outside a day-kind evaluation (ch. 8.2), an {@code Unresolved} node the evaluation
 * reaches (ch. 5.7), an
 * easterSunday of a year that is not whole or lies outside the years a date holds (ch. 3.8), a dateWith or a
 * dateTimeWith over parts that name no moment, and an irrational square root without a rounding (ch. 2.2). Its sentence
 * ({@link #sentence}) is the reference interpreter's wherever that executor names the fault, so a pinned fragment matches
 * both.
 *
 * <p>Like a division by zero it never leaves the fire: the consequence that computes a value, or the fault rule of a
 * condition, inserts it logically as a {@link Fout}, which counts only where it stands at the fixpoint
 * ({@code TmsTranspiler.zetAfgeleideWaarden}). The branch passes intermediate states the reference interpreter does not
 * see, and a fault thrown there could not be withdrawn when the state passes (PLAN §9, 2026-10-09, "Faults and deposits
 * count at the fixpoint").
 */
public final class Rekenfout extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** RUNTIME-SEMANTICS ch. 8.2: the evaluationDate context value in a session without an evaluation date, in the
     *  reference interpreter's words. */
    public static final String EVALUATION_DATE_WITHOUT_DATE =
            "the evaluationDate context value needs an evaluation date; the case carries asOfDate null";

    /** RUNTIME-SEMANTICS ch. 8.1: a version with a bounded validity in a session without an evaluation date, in the
     *  reference interpreter's words. */
    public static final String VALIDITY_WITHOUT_DATE =
            "a validity-bounded form needs an evaluation date; this session is time-free";

    /** RUNTIME-SEMANTICS ch. 8.2: the bound day of a day-kind definition read where no day-kind evaluation binds it, in
     *  the reference interpreter's words. */
    public static final String DAY_OUTSIDE_DAY_KIND = "a BoundDay is the bound day of a day-kind definition"
            + " (ch. 3.7); outside a day-kind evaluation it denotes nothing";

    /** RUNTIME-SEMANTICS ch. 5.7 and WF-044: an expression marker the evaluation reaches, in the reference interpreter's
     *  words. */
    public static final String UNRESOLVED = "Unresolved reached in evaluation: this rule fails observably";

    /** The condition marker's sentence, in the reference interpreter's words. */
    public static final String UNRESOLVED_CONDITION = "Unresolved condition reached in evaluation";

    private final String sentence;
    private final String regel;

    Rekenfout(String sentence) {
        this(sentence, null, null);
    }

    Rekenfout(String sentence, Throwable cause) {
        this(sentence, null, cause);
    }

    private Rekenfout(String sentence, String regel, Throwable cause) {
        super(regel == null ? sentence : "fout in de regel \"" + regel + "\": " + sentence, cause);
        this.sentence = sentence;
        this.regel = regel;
    }

    /** The same fault named after the rule {@code regel} it ran in, with this one as its cause, as
     *  {@link Rational.DelingDoorNul#inDeRegel} names a division. */
    public Rekenfout inRule(String regel) {
        return new Rekenfout(sentence, regel, this);
    }

    /** The sentence of the fault, without the rule. */
    public String sentence() {
        return sentence;
    }

    /** The rule a named fault ran in; null where nothing named it yet. */
    public String regel() {
        return regel;
    }

    /** Whether {@code t} is an evaluation fault this branch turns into a {@link Fout}: a division by a numeric zero, a
     *  fault of this class, or an input in the other convention of an attribute on a timeline ({@link Invoervormfout}).
     *  Every other exception is a broken contract or a bug and leaves the fire as it is. */
    static boolean isEvaluationFault(Throwable t) {
        return t instanceof Breuk.DelingDoorNul || t instanceof Rekenfout || t instanceof Invoervormfout;
    }

    /** The evaluation fault {@code fout} named after the rule {@code regel} it ran in. */
    static RuntimeException designate(RuntimeException fout, String regel) {
        if (fout instanceof Breuk.DelingDoorNul zero) return zero.inDeRegel(regel, zero);
        if (fout instanceof Rekenfout r) return r.inRule(regel);
        if (fout instanceof Invoervormfout i) return i.inRule(regel);
        throw new IllegalArgumentException("geen evaluatiefout: " + fout, fout);
    }
}
