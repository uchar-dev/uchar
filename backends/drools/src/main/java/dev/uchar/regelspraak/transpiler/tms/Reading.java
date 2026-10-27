package dev.uchar.regelspraak.transpiler.tms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The values the condition of one form reads on one object, inserted logically by a reading rule of that form in a
 * build with readings (card fixpoint-readings): a rule with the left-hand side of the form's own rule without its test,
 * so that it matches on every object the form reaches, whether the condition holds or not, and binds what the form's
 * test binds. Under truth maintenance it follows the standings that rule reads, so at the fixpoint it carries the
 * values the test saw. {@code form} is the JSON pointer of the form into the document ({@code TmsTranspiler#readingsOf});
 * {@code values} is the rule's value array, the bound value at each slot the left-hand side binds and null at every
 * other. No rule reads it, and it writes nothing: {@code TmsTranspiler#readingsOf} evaluates the condition over it.
 *
 * <p>{@code kind} says which reading rule inserted it. The reading rule of a form keeps the guard of the form's turn,
 * {@link Kind#TURN}. A default whose turn its stack guards (a higher default of its chain, the exception groups of the
 * default it falls back from, the defaults above the one whose exception group it is) has a second reading rule
 * without those guards, {@link Kind#SPECULATIVE}, which stands also where the default is displaced, so that its
 * condition and its value are read on the fixpoint world there as well (RUNTIME-SEMANTICS ch. 5.12, 7.1).
 *
 * <p>Keyed and compared as {@link Decides}: the subject by identity, the form and the kind by value, and the values one
 * by one by their own equality, a bean by identity.
 */
public record Reading(Object subject, String form, List<Object> values, Kind kind) {

    /** Which reading rule of the form inserted the reading. */
    public enum Kind {
        /** The reading rule with the guard of the form's turn: it stands where the form is at its turn. */
        TURN,
        /** The reading rule of a default without the control facts of its stack: it stands whether a higher default
         *  decided or not. */
        SPECULATIVE
    }

    public Reading {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(kind, "kind");
        // a slot no pattern binds holds null, which List.copyOf refuses
        values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    /** The reading of the reading rule that keeps the form's turn. */
    public Reading(Object subject, String form, List<Object> values) {
        this(subject, form, values, Kind.TURN);
    }

    /** The reading of {@code subject} over the value array {@code values}, as the rendering into closures fills it. */
    public Reading(Object subject, String form, Object[] values) {
        this(subject, form, Arrays.asList(values), Kind.TURN);
    }

    /** As {@link #Reading(Object, String, Object[])}, inserted by the reading rule of {@code kind}. */
    public Reading(Object subject, String form, Object[] values, Kind kind) {
        this(subject, form, Arrays.asList(values), kind);
    }

    @Override public boolean equals(Object o) {
        return o instanceof Reading r && r.subject == subject && r.form.equals(form) && r.kind == kind
                && r.values.equals(values);
    }

    @Override public int hashCode() {
        // the values may hold a bean, whose hash would follow its fields; the subject, the form and the kind separate
        // the readings
        return Objects.hash(System.identityHashCode(subject), form, kind);
    }

    @Override public String toString() {
        return "Reading(" + FactNames.object(subject) + ", " + form + ", " + values.size() + " slots"
                + (kind == Kind.SPECULATIVE ? ", speculative" : "") + ")";
    }
}
