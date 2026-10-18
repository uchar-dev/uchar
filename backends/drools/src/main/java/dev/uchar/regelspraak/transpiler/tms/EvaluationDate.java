package dev.uchar.regelspraak.transpiler.tms;

import java.time.LocalDate;

/**
 * The evaluation date of the session (RUNTIME-SEMANTICS ch. 8.1, the case's {@code asOfDate}) as a fact: one per
 * session, put in working memory from the global {@code rekendatum} by a rule of its own, with a null {@code datum}
 * where the case carries none, a time-free session. A form that needs the date binds this fact in its left-hand side,
 * the way it binds a parameter's {@link ParameterState}, so the date is a bound value: a validity window gates on it
 * (ch. 8.2), a {@code TimeWindow} tests it, and the context values evaluationDate and evaluationYear read it. The global
 * does not change while the session fires, so the fact is never withdrawn.
 *
 * <p>A fault that needs the date and belongs to no object, a version with a bounded validity in a time-free session,
 * is a {@link Fault} on this fact. Compared by value.
 */
public record EvaluationDate(LocalDate date) {

    @Override public String toString() {
        return "Rekendatum(" + (date == null ? "geen" : date) + ")";
    }
}
