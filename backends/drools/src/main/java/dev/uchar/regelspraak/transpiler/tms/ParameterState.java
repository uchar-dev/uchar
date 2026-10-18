package dev.uchar.regelspraak.transpiler.tms;

import java.util.Objects;

/**
 * The value of one parameter for the session, as a fact: one per parameter a lowered rule reads, the value the case
 * set on the parameter's global, or null where the case left it unset, which reads leeg. A rule of its own puts it in
 * working memory from the global, and a condition or a value reads the parameter through this fact, as a positive
 * pattern, the way it reads a standing.
 *
 * <p>Why a fact and not the global itself: the branch bound a parameter through a {@code from} over the subject and
 * the parameter's global, and with two parameters of one Java type, each read by a rule of its own, the second rule
 * read the first rule's parameter, silently, while two such sources in one rule each read their own; a source function
 * that captured the parameter's name changed nothing (measured 2026-10-08, {@code TmsParameterWitnessTest}). A pattern
 * on this fact carries a constraint id of its own, as every pattern of the branch does. Compared by value: the
 * parameter's name and the value.
 */
public record ParameterState(String parameter, Object waarde) {

    public ParameterState {
        Objects.requireNonNull(parameter, "parameter");
    }

    @Override public String toString() {
        return "ParameterStand(" + parameter + ", " + (waarde == null ? "leeg" : waarde) + ")";
    }
}
