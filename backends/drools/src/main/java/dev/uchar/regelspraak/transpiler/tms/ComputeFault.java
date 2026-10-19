package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

/**
 * An evaluation fault of the exec-model branch other than a division by a numeric zero
 * ({@link Breuk.DelingDoorNul}): a date step or a duration count outside the range of RUNTIME-SEMANTICS ch. 3.8, a
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
 * condition, inserts it logically as a {@link Fault}, which counts only where it stands at the fixpoint
 * ({@link TmsTranspiler#setDerivedValues}). The branch passes intermediate states the reference interpreter does not
 * see, and a fault thrown there could not be withdrawn when the state passes (PLAN §9, 2026-10-09, "Faults and deposits
 * count at the fixpoint").
 */
public final class ComputeFault extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** RUNTIME-SEMANTICS ch. 8.2: the evaluationDate context value in a session without an evaluation date, in the
     *  reference interpreter's words. */
    public static final String EVALUATION_DATE_WITHOUT_DATE = Msg.drools("EF-011.date").text();

    /** RUNTIME-SEMANTICS ch. 8.1: a version with a bounded validity in a session without an evaluation date, in the
     *  reference interpreter's words. */
    public static final String VALIDITY_WITHOUT_DATE = Msg.drools("EF-012").text();

    /** RUNTIME-SEMANTICS ch. 8.2: the bound day of a day-kind definition read where no day-kind evaluation binds it, in
     *  the reference interpreter's words. */
    public static final String DAY_OUTSIDE_DAY_KIND = Msg.drools("EF-010").text();

    /** RUNTIME-SEMANTICS ch. 5.7 and WF-044: an expression marker the evaluation reaches, in the reference interpreter's
     *  words. */
    public static final String UNRESOLVED = Msg.drools("EF-019.expression").text();

    /** The condition marker's sentence, in the reference interpreter's words. */
    public static final String UNRESOLVED_CONDITION = Msg.drools("EF-019.condition").text();

    private final String sentence;
    private final String rule;

    ComputeFault(String sentence) {
        this(sentence, null, null);
    }

    ComputeFault(String sentence, Throwable cause) {
        this(sentence, null, cause);
    }

    private ComputeFault(String sentence, String rule, Throwable cause) {
        super(rule == null ? sentence : Msg.drools("FRAME.fault-in-rule", "rule", rule, "sentence", sentence).text(), cause);
        this.sentence = sentence;
        this.rule = rule;
    }

    /** The same fault named after the rule {@code rule} it ran in, with this one as its cause, as
     *  {@link Breuk.DelingDoorNul#inDeRegel} names a division. */
    public ComputeFault inRule(String rule) {
        return new ComputeFault(sentence, rule, this);
    }

    /** The sentence of the fault, without the rule. */
    public String sentence() {
        return sentence;
    }

    /** The rule a named fault ran in; null where nothing named it yet. */
    public String rule() {
        return rule;
    }

    /** The fault kind of this fault (RUNTIME-SEMANTICS ch. 5.7b), read off its sentence: the sentences are this
     *  class's constants and the runtime's, the reference interpreter's words, and a plan term carries a sentence
     *  rather than a code, so the kind is named here, beside the text and never inside it. Null for a sentence of a
     *  kind no red case witnesses yet (an irrational square root, an easterSunday or a dateWith out of range). */
    public String code() {
        if (sentence.equals(EVALUATION_DATE_WITHOUT_DATE) || sentence.equals(RegelSpraakRuntime.REKENJAAR_ZONDER_REKENDATUM))
            return "EF-011";
        if (sentence.equals(VALIDITY_WITHOUT_DATE)) return "EF-012";
        if (sentence.equals(DAY_OUTSIDE_DAY_KIND)) return "EF-010";
        if (sentence.equals(UNRESOLVED) || sentence.equals(UNRESOLVED_CONDITION)) return "EF-019";
        if (sentence.equals(RegelSpraakRuntime.DATUMTIJD_BUITEN_BEREIK)) return "EF-007";
        if (sentence.startsWith("duration count ") && sentence.endsWith(" is out of range")) return "EF-008";
        if (sentence.startsWith(ComputeHelpers.DAY_KIND_WALK)) return "EF-009";
        return null;
    }

    private static final java.util.regex.Pattern COUNT_OUT_OF_RANGE =
            java.util.regex.Pattern.compile("duration count (.*) is out of range");
    private static final java.util.regex.Pattern WALK_PAST_REACH =
            java.util.regex.Pattern.compile("no day of the kind within (\\d+) calendar days of (\\S+)");

    /** The parameters of this fault's kind (RUNTIME-SEMANTICS ch. 5.7b) that its sentence carries, in the order of the
     *  table: the count of EF-008, the reach and the starting day of EF-009. The sentence is the one place they stand
     *  (a plan term carries a sentence, not fields); a kind whose sentence names none has none. */
    public java.util.Map<String, String> params() {
        java.util.Map<String, String> p = new java.util.LinkedHashMap<>();
        var count = COUNT_OUT_OF_RANGE.matcher(sentence);
        if ("EF-008".equals(code()) && count.matches()) p.put("count", count.group(1));
        var walk = WALK_PAST_REACH.matcher(sentence);
        if ("EF-009".equals(code()) && walk.find()) {
            p.put("from", walk.group(2));
            p.put("reach", walk.group(1));
        }
        return p;
    }

    /** Whether {@code t} is an evaluation fault this branch turns into a {@link Fault}: a division by a numeric zero, a
     *  fault of this class, or an input in the other convention of an attribute on a timeline ({@link InputFormFault}).
     *  Every other exception is a broken contract or a bug and leaves the fire as it is. */
    static boolean isEvaluationFault(Throwable t) {
        return t instanceof Breuk.DelingDoorNul || t instanceof ComputeFault || t instanceof InputFormFault;
    }

    /** The evaluation fault {@code fault} named after the rule {@code rule} it ran in. */
    static RuntimeException designate(RuntimeException fault, String rule) {
        if (fault instanceof Breuk.DelingDoorNul zero) return zero.inDeRegel(rule, zero);
        if (fault instanceof ComputeFault r) return r.inRule(rule);
        if (fault instanceof InputFormFault i) return i.inRule(rule);
        throw new IllegalArgumentException("geen evaluatiefout: " + fault, fault);
    }
}
