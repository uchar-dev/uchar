package dev.uchar.regelspraak.transpiler.tms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The values the condition of one form reads on one object, inserted logically by the reading rule of that form in a
 * build with readings (card fixpoint-readings): a rule with the left-hand side of the form's own rule without its test,
 * so that it matches on every object the form reaches, whether the condition holds or not, and binds what the form's
 * test binds. Under truth maintenance it follows the standings that rule reads, so at the fixpoint it carries the
 * values the test saw. {@code form} is the JSON pointer of the form into the document ({@code TmsTranspiler#readingsOf});
 * {@code values} is the rule's value array, the bound value at each slot the left-hand side binds and null at every
 * other. No rule reads it, and it writes nothing: {@code TmsTranspiler#readingsOf} evaluates the condition over it.
 *
 * <p>Keyed and compared as {@link Decides}: the subject by identity, the form by value, and the values one by one by
 * their own equality, a bean by identity.
 */
public record Reading(Object subject, String form, List<Object> values) {

    public Reading {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(form, "form");
        // a slot no pattern binds holds null, which List.copyOf refuses
        values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    /** The reading of {@code subject} over the value array {@code values}, as the rendering into closures fills it. */
    public Reading(Object subject, String form, Object[] values) {
        this(subject, form, Arrays.asList(values));
    }

    @Override public boolean equals(Object o) {
        return o instanceof Reading r && r.subject == subject && r.form.equals(form) && r.values.equals(values);
    }

    @Override public int hashCode() {
        // the values may hold a bean, whose hash would follow its fields; the subject and the form separate the readings
        return Objects.hash(System.identityHashCode(subject), form);
    }

    @Override public String toString() {
        return "Reading(" + FactNames.object(subject) + ", " + form + ", " + values.size() + " slots)";
    }
}
