package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.model.Unit;

import java.util.List;
import java.util.Objects;

/**
 * The reading of the condition of one form on one object at the fixpoint of a session of a build with readings
 * ({@code DroolsBackend.readingsOf}, card fixpoint-readings): the evaluation of the condition, and of the form's value
 * where the condition holds, over the values the form's own rule bound at the fixpoint, with the functions, the stop
 * rule and the order of evaluation of that rule (RUNTIME-SEMANTICS ch. 5.10, 5.11), recording per node what the
 * evaluation reached and what it found. It is read off the session, never computed from the beans: the values are those
 * the rule's test saw, and a reading deposits nothing, faults nothing and writes nothing.
 *
 * <p>The form is named by the JSON pointer (RFC 6901) of its version or its default into the IR document of the build:
 * {@code /rules/rules/k/versions/v}, {@code /rules/consistencies/k/versions/v}, {@code /rules/allocations/k/versions/v},
 * {@code /rules/objectCreations/k/versions/v}, {@code /rules/factCreations/k/versions/v}, and for a default
 * {@code /rules/prioritizedDefaults/i/defaults/j(/exceptions/members/m/defaults/l)*}, or the version of the ordinary
 * rule that the fold of RUNTIME-SEMANTICS ch. 7.10 took into a stack, as {@link DefaultsDecision} names a default. A form
 * has a reading where it has a condition: a rule's or a creation's {@code condition}, a consistency's
 * {@code assertion}, a default's {@code justification}. A fact creation whose condition is a check on its target, which
 * filters the targets, has none. A form without a condition that computes a value — a rule on an attribute, an
 * attribute default without a justification, an ordinary rule without a condition that the fold took into a stack —
 * has the reading of its value (card readings-of-unconditional-forms): {@link Status#HOLDS} where it is at its turn,
 * its nodes those of the value, the root 0.
 *
 * <p>The nodes are numbered in the pre-order of the document over the nodes of the condition union (IR-SPEC ch. 4.5) and
 * the expression union (IR-SPEC ch. 4): first those of the condition, its root 0, then those of the form's value where it
 * computes one (a rule's {@code value} on an attribute, an attribute default's {@code consequence}), each node before
 * its fields and the fields in the order the document writes them. A text part, a whole-period qualifier, a dimension
 * selection, a type and a role binding are no nodes. A node inside the filter, the matrix or the source of a fold over a
 * set is evaluated per member of the fold, in its own rules, and has no reading of its own: the node of the fold
 * carries its contributors, and the numbers count those nodes all the same.
 *
 * @param object the object the form ran on, a bean of the build's bean model, compared by identity
 * @param form   the form, by its JSON pointer
 * @param status how the condition came out, or why the form has no reading on the object
 * @param nodes  the nodes in the order of their numbers; empty where the status is {@link Status#DISPLACED} or
 *               {@link Status#OUTSIDE_VALIDITY}, and where a form with a dated validity faults a session without an
 *               evaluation date
 */
public record ConditionReading(Object object, String form, Status status, List<NodeReading> nodes) {

    public ConditionReading {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(status, "status");
        nodes = List.copyOf(nodes);
    }

    /** How the condition came out on the object, or why the form has no reading there. */
    public enum Status {
        /** The condition holds; or the form has none, computes a value and is at its turn. */
        HOLDS,
        /** The condition does not hold. */
        FAILS,
        /** The evaluation of the condition faults (RUNTIME-SEMANTICS ch. 5.2), or the form has a dated validity and the
         *  session has no evaluation date (ch. 8.1): the session has no answer. */
        FAULTS,
        /** The form was not at its turn on the object: a higher default decided, or an exception group of the default
         *  settled (ch. 7.1, 7.9); the target of an initialisation, a default's or a rule's, holds a value of the input
         *  or of another writer (ch. 5.4, 7.3); or the subject of a creation of the 1:1 shape is linked in the input
         *  (ch. 5.5). */
        DISPLACED,
        /** The validity window of the form does not admit the session's evaluation date (ch. 8.2). */
        OUTSIDE_VALIDITY
    }

    /**
     * One node of the condition or of the value: whether the evaluation reached it, and what it found there. A node
     * after the stop of a composite or of a quantifier fold (ch. 5.10), in the branch of a choice that is not taken
     * (ch. 10.2), in the value of a {@code Let} whose binder no read forces (ch. 10.6) or in the value of a form whose
     * condition does not hold, is not reached, and has neither a value nor a fault. A node that a read of a leeg binder
     * ended (ch. 10.6, clause 2) is reached and has neither.
     *
     * @param node         the number of the node
     * @param kind         the kind of the node as the wire spells it, {@code Comparison}, {@code Aggregation}, …
     * @param reached      whether the evaluation reached the node
     * @param value        a condition's truth value, an expression's value in the form {@code Fixpoint.DerivedValue}
     *                     gives a value (a number as a {@code BigDecimal}, an exact rational collapsed to its decimal
     *                     as the write boundary collapses it, a date, a text, a truth value, a timeline as a list of
     *                     periods), or null: leeg, or no value where the node was not reached or faulted
     * @param unit         the unit of an expression by the effective-type law (RUNTIME-SEMANTICS ch. 2.10), null for a
     *                     condition and for a value without one
     * @param fault        the evaluation fault the node raised or passed on, null where it raised none: a
     *                     {@code ComputeFault}, an {@code InputFormFault} or a division by a numeric zero, unnamed, as
     *                     the evaluation raises it, whose sentence names an object of the session by its type and
     *                     identity where it names one, as a {@code Fault} of {@code Fixpoint} does without labels
     * @param contributors for the node of a fold over a set that the evaluation reached, its contributors; empty for
     *                     every other node
     */
    public record NodeReading(int node, String kind, boolean reached, Object value, Unit unit, RuntimeException fault,
                              List<Contributor> contributors) {
        public NodeReading {
            Objects.requireNonNull(kind, "kind");
            contributors = List.copyOf(contributors);
        }
    }

    /**
     * One contributor of a fold over a set on the object: a member of its range that passes its filter
     * (RUNTIME-SEMANTICS ch. 3.4, 3.9a). For a quantifier fold every contributor carries the verdict of the matrix on it,
     * or its fault, after the stop and after a fault as well: the table of ch. 3.9a over the contributors, which
     * ch. 5.12 records, read off the rule that evaluates the matrix per member and deposits nothing here. Its position is
     * {@code reached}. Where the matrix can fault or deposit, the contributors are in their order (ch. 5.11) and
     * {@code reached} says whether the stop rule of ch. 5.10 reached the contributor; a verdict or a fault of a
     * contributor it did not reach is the reading's, not the session's. Where the matrix can do neither, no fault and no
     * deposit shows the order, the session keeps none, and the fold has no position: {@code reached} is null. For a
     * value fold every contributor is reached. A fold whose filter faults has no contributors.
     *
     * @param member  the member, a bean of the build's bean model
     * @param value   for a quantifier fold the verdict of its matrix on the member, a {@code Boolean}, null where it
     *                faulted; for a value fold the value the member contributes, null for a count
     * @param fault   the evaluation fault the matrix raised for the member, unnamed, null where it raised none
     * @param reached the position: whether the evaluation reached the member, null where the fold has none
     */
    public record Contributor(Object member, Object value, RuntimeException fault, Boolean reached) {
        public Contributor {
            Objects.requireNonNull(member, "member");
        }
    }
}
