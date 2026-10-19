package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.transpiler.tms.ComputeHelpers;

/**
 * A public static method of the branch that a rendering calls around the functions of a term ({@link Term}), by its
 * class and its name: what a test of deposits and a consequence do beside the evaluation of a term, and the facts a
 * consequence makes whose class or constructor is not public; and what a DRL text needs where MVEL has no construct of
 * its own: catching, naming and throwing an evaluation fault, reading a bean and walking a path, the cell of a
 * {@code Let}, building a list. A DRL text calls each class-qualified, never through an {@code import function}, which a
 * field of a fact with the same name shadows without a complaint (FABLE-2 §2.1); no route is overloaded, so a call by
 * name finds one method. The rendering into closures calls those it has a use for, so that the two renderings run one
 * implementation.
 */
public enum Route {
    /** A fresh probe of the §6.5 and §6.6 deposit, which a test of deposits ({@link TestItem.Role#DEPOSIT}) and a
     *  consequence with a deposit ({@link Consequence.Deposit}) put in the probe's slot before they evaluate. */
    FRESH_DEPOSIT_FLAG(ComputeHelpers.class, "depositFlag"),
    /** A place that deposits reports to the probe in its slot, with the EF codes of what it deposits: the evaluation
     *  of a fold whose filter deposited ({@link Term.FoldRead}); a division and a square root report within their own
     *  function. */
    REPORT(ComputeHelpers.class, "report"),
    /** The deposits of a fold's filter, the list of its {@code FilterDeposit} facts, reported to the probe of the rule
     *  that reads the fold with their codes ({@link Term.FoldRead}). */
    REPORT_FILTER_DEPOSITS(ComputeHelpers.class, "reportFilterDeposits"),
    /** Whether the probe heard a place: the verdict of a test of deposits, and the deposit of a consequence. */
    REPORTED(ComputeHelpers.class, "reported"),
    /** The EF codes the probe heard, which the deposit of a consequence and a member's test carry
     *  (RUNTIME-SEMANTICS ch. 5.6, 5.7b). */
    DEPOSIT_CODES(ComputeHelpers.class, "depositCodes"),
    /** Whether a computed value is leeg, null or a timeline without a segment: a {@link Consequence.Value} inserts it only
     *  where it is not, or where the plan writes leeg. */
    IS_EMPTY(ComputeHelpers.class, "isEmpty"),
    /** The evaluation fault of a fixed sentence, named in a rule, which {@link Consequence.Fault} inserts. */
    COMPUTE_FAULT(ComputeHelpers.class, "computeFault"),
    /** The §9.5 inconsistency of a form on an object with the EF codes of its kinds, which
     *  {@link Consequence.Inconsistency} and the deposit of a {@link Consequence.Value} insert, and of the conflict of a
     *  defaults stack at its root under the conflict's key ({@link Consequence.StackFact.Inconsistency}). */
    INCONSISTENCY(ComputeHelpers.class, "inconsistency"),
    /** An input timeline as the asOfDate-bucket scalar the case gave, which {@link Consequence.State} carries where it says
     *  so. */
    BUCKET_SCALAR(ComputeHelpers.class, "bucketScalarOf"),
    /** The fault of the form of an input, its sentence around the object, named in a rule, which
     *  {@link Consequence.InputFormFault} inserts. */
    INPUT_FORM_FAULT(ComputeHelpers.class, "inputFormFault"),
    /** The keys of an instance in a source of a uniqueness, one per value a component on a timeline ever carries, which
     *  {@link Consequence.UniquenessKey} inserts. */
    UNIQUENESS_KEYS(ComputeHelpers.class, "uniquenessKeys"),
    /** The facts of the distribution of an allocation over the recipients of one source, computed by
     *  {@code Allocation.bereken}: its deposits, its shares and its rest, which {@link Consequence.Payout} inserts. */
    PAYOUT(ComputeHelpers.class, "payout"),
    /** The contribution of a member to a fold by its value, or none where the value is leeg or the member's timeline
     *  stands as the asOfDate-bucket scalar, which {@link Consequence.Input.MemberValue} inserts. */
    CONTRIBUTION(ComputeHelpers.class, "contribution"),
    /** The place of a member in a fold's order from the places its rule binds ({@link Consequence.FoldPlace}), which a
     *  filter fault, a member fault and a quantifier fold's test of a member carry; where the input states no place,
     *  the broken contract, named after what the rule needs the place for. */
    FOLD_PLACE(ComputeHelpers.class, "foldPlace"),
    /** The creation depth of an object creation's new object, which {@link Consequence.CreationDepth} carries the arguments
     *  of; past the budget the fault at the making, {@code CreationBudget}, whose constructor is not public. */
    CREATION_DEPTH(ComputeHelpers.class, "creationDepth"),
    /** The new bean of an object creation, of the class the text names, loaded by the class loader of the subject's bean
     *  ({@link Consequence.Construction}): a DRL text names a bean class and cannot load one. */
    NEW_OBJECT(ComputeHelpers.class, "newObject"),
    /** An attribute initialisation set on the new bean of an object creation where its value is not null, through the
     *  setter {@code Namen.lidnamen} names ({@link Consequence.Construction.Setting}). */
    SET(ComputeHelpers.class, "set"),
    /** Whether the outcome of an evaluation is an evaluation fault, which a consequence inserts as a fault. */
    IS_EVALUATION_FAULT(ComputeHelpers.class, "isEvaluationFault"),
    /** An exception caught in an evaluation as its outcome where it is an evaluation fault; any other passes on. */
    CAUGHT(ComputeHelpers.class, "caught"),
    /** An evaluation fault named after the rule it ran in, as a fault carries it. */
    DESIGNATE(ComputeHelpers.class, "designate"),
    /** The evaluation fault of a sentence, thrown ({@link Term.Fail}). */
    FAIL(ComputeHelpers.class, "fail"),
    /** A value, and the evaluation fault of a sentence where it is leeg ({@link Term.Required}). */
    REQUIRED(ComputeHelpers.class, "required"),
    /** Texts joined in order ({@link Term.TextComposition}). */
    TEXT_COMPOSITION(ComputeHelpers.class, "textComposition"),
    /** A member of a bean by its property ({@link Binding.Field#property}), null on an absent object. */
    READ(ComputeHelpers.class, "readProperty"),
    /** The object a walk over reference fields reaches, null where a link is absent: a read along a path. */
    ALONG(ComputeHelpers.class, "along"),
    /** A characteristic read off a bean as whether it holds {@code true} ({@link Binding.ReadForm#WAAR}). */
    WAAR(ComputeHelpers.class, "holds"),
    /** The true periods of a characteristic on a timeline ({@link Binding.ReadForm#PERIODS}). */
    PERIODS(ComputeHelpers.class, "periods"),
    /** Whether a reference field is set, to an instance of a named class ({@link Binding.ReadForm#IS_SET}). */
    IS_SET(ComputeHelpers.class, "isSet"),
    /** Whether the evaluation date lies in a validity window ({@link Alpha.Comparator#WITHIN}). */
    WITHIN(ComputeHelpers.class, "inside"),
    /** Whether an object is an instance of a named class ({@link Alpha.Comparator#IS_A}). */
    IS_A(ComputeHelpers.class, "isA"),
    /** Whether the own class of an object has none of the names ({@link Alpha.Comparator#CLASS_NOT_IN}). */
    CLASS_NOT_IN(ComputeHelpers.class, "classNotIn"),
    /** Whether the own class of an object, exactly, has a name ({@link Alpha.Comparator#CLASS_IS}). */
    CLASS_IS(ComputeHelpers.class, "classIs"),
    /** A fresh cell for the binder of one evaluation of a {@link Term.Let}. */
    BINDER_CELL(ComputeHelpers.class, "binderCell"),
    /** Whether the cell of a binder holds its value. */
    BINDER_CELL_FILLED(ComputeHelpers.class, "binderCellFilled"),
    /** Fills the cell of a binder with its value. */
    FILL_BINDER_CELL(ComputeHelpers.class, "fillBinderCell"),
    /** The value of a binder, or the barrier of its {@code Let} where it is leeg ({@code EmptyBarrier}). */
    BINDER_VALUE(ComputeHelpers.class, "binderValue"),
    /** The outcome of a {@code Let} its barrier ended: leeg where the barrier is its own; another passes on. */
    INTERCEPTED(ComputeHelpers.class, "intercepted"),
    /** The verdict of a composite once some of its members are read, or none while the rest can change it: the stop
     *  triggers before each member ({@link Term.Compound}). */
    ESTABLISHED(ComputeHelpers.class, "established"),
    /** The verdict of a composite whose members are all read ({@link Term.Compound}). */
    END_VERDICT(ComputeHelpers.class, "endVerdict"),
    /** The verdict of a matrix as a member's contribution, 1 or 0 ({@link Consequence.Input.Verdict}). */
    VERDICT(ComputeHelpers.class, "verdict"),
    /** A broken contract, thrown: no evaluation fault. */
    CONTRACT_BREACH(ComputeHelpers.class, "contractBreach"),
    /** The fault of writers that disagree, as the {@link Consequence.WritersConflict} plans it. */
    WRITERS_CONFLICT(ComputeHelpers.class, "writersConflict"),
    /** The empty list, for a route that takes a list. */
    LIST0(ComputeHelpers.class, "list0"),
    /** A list of one value. */
    LIST1(ComputeHelpers.class, "list1"),
    /** A list of two values. */
    LIST2(ComputeHelpers.class, "list2"),
    /** A list of three values. */
    LIST3(ComputeHelpers.class, "list3"),
    /** A list of four values. */
    LIST4(ComputeHelpers.class, "list4"),
    /** A list of five values. */
    LIST5(ComputeHelpers.class, "list5"),
    /** A list of six values. */
    LIST6(ComputeHelpers.class, "list6");

    private final Class<?> javaClass;
    private final String method;

    Route(Class<?> javaClass, String method) {
        this.javaClass = javaClass;
        this.method = method;
    }

    /** The class whose public static method this is. */
    public Class<?> javaClass() { return javaClass; }

    /** The name of the method. */
    public String method() { return method; }
}
