package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * A value or a condition of a rule as the analysis plans it: a tree of calls over the values the rule's left-hand side
 * binds, in a closed vocabulary that a rendering turns into a function over the rule's value array or into DRL text
 * without deciding anything itself. A condition is a term whose value is a truth value. Every decision the analysis
 * takes about a value or a condition is a node or a field of a node: the function that computes it, by name
 * ({@link StaticFunction}), among them the helper that compares and the operator, polarity and leeg tables it takes as
 * constants; the factor that restates a unit ({@link Restate}); where a division, a square root or the matrix of a
 * quantifier fold deposits ({@link Division}, {@link SquareRoot}, {@link QuantifierRead}); where the evaluation faults
 * ({@link Fail}, {@link Required}, {@link FoldRead} and the functions that can, {@link StaticFunction#canFail}) and where a
 * fault does not hold instead ({@link WithoutFault}); what is evaluated only on need, on one branch or up to a stop
 * ({@link Let}, {@link Choice}, {@link Compound}). The children of a node are evaluated in the order the node lists
 * them, which is the order of RUNTIME-SEMANTICS ch. 5.11.
 *
 * <p>A probe slot in a term ({@code depositFlag} of {@link Division}, {@link SquareRoot}, {@link FoldRead} and
 * {@link QuantifierRead}) is a slot that the term's position in its rule fills: a test of deposits
 * ({@link TestItem.Role#DEPOSIT}), the value of a {@link Consequence.Value} with a deposit, or the matrix of a
 * {@link Consequence.Input.MemberTest} with a probe. In every other position, such as the rule of a condition's fault, the
 * analysis sets it to -1, so a place there deposits nothing and a rendering reads no slot that nobody fills.
 *
 * <p>A node that stands for a node of the IR carries that node's source fragment, the IR's {@code source.snippet}
 * ({@link #source}): a rendering into DRL text puts it above the text as a comment. It is null where the IR has none, and
 * on a node the analysis adds without a node of its own in the IR, such as the arguments of a helper.
 */
public sealed interface Term {

    /** The source fragment of the IR node this term stands for, or null. */
    String source();

    /**
     * The value {@code value}, exactly as the analysis made it: a number as a {@code BigDecimal} with its scale or as
     * an exact rational ({@code Breuk}), a date, a date-time, a truth value, a text or an enumeration label. {@code type}
     * is the IR's type of a literal, with its unit, and a text for the literal chunk of a text composition; it is null
     * for a constant of the analysis that an argument of a {@link StaticFunction} takes (a number of decimals, a rounding mode,
     * a unit of time, a label, a class), and for an absent bound, whose value is null.
     */
    record Const(Object value, Datatype type, String source) implements Term {}

    /** The value the left-hand side binds at slot {@code slot} of the value array ({@link ReadTable}). */
    record SlotValue(int slot, String source) implements Term {}

    /**
     * The input member {@code field} on the bean of the object that {@code object} gives, read as {@code form}
     * ({@link Binding.ReadForm}): an input that a test the form composes itself reads where it evaluates, with no
     * pattern that binds it, such as the target in the guard of an initialisation. The read has no effect and cannot
     * fault.
     */
    record Input(Term object, Binding.Field field, Binding.ReadForm form, String source) implements Term {}

    /**
     * The read of the binder {@code naam} of the {@link Let} whose slot is {@code slot} (RUNTIME-SEMANTICS ch. 10.6): the
     * first read evaluates the {@code Let}'s value and every further one reads that result; a leeg value ends that
     * {@code Let} with leeg.
     */
    record Binder(int slot, String name, String source) implements Term {}

    /**
     * {@code body} with the binder {@code naam} bound to {@code value} as a need (RUNTIME-SEMANTICS ch. 10.6): the
     * value is evaluated at the first read of the binder ({@link Binder}, slot {@code slot}, which no pattern binds) and
     * not at all where the body reads none. {@code eager} holds where the body reads the binder before anything else of
     * it can fault, deposit or end an evaluation, so that evaluating the value before the body gives the same outcome,
     * the same faults and the same deposits; a rendering that cannot evaluate by need may then evaluate it first. The
     * rendering into closures evaluates by need always.
     */
    record Let(int slot, String name, Term value, Term body, boolean eager, String source) implements Term {}

    /** The condition {@code condition}, then exactly the branch it chooses: {@code dan} where it holds, {@code anders}
     *  where not (RUNTIME-SEMANTICS ch. 10.2). The other branch evaluates nothing. */
    record Choice(Term condition, Term then, Term otherwise, String source) implements Term {}

    /** The function {@code function} on the values of {@code arguments}, evaluated in that order. */
    record Call(StaticFunction function, List<Term> arguments, String source) implements Term {
        public Call {
            arguments = List.copyOf(arguments);
        }
    }

    /**
     * The value of {@code term} restated from the unit {@code van} into the unit {@code destination} (RUNTIME-SEMANTICS ch.
     * 2.6) through {@code function}: {@link StaticFunction#RESTATE_EXACT}, a number times {@code counter} and divided by
     * {@code denominator}, the exact canonical factors, as exact rationals; or {@link StaticFunction#RESTATE}, where
     * {@code denominator} is null, times the single ratio {@code counter} of the two units. A leeg value and a value that is
     * no number pass unchanged. The analysis adds no node where no restatement is due.
     */
    record Restate(StaticFunction function, Term term, Unit van, Unit destination, BigDecimal counter, BigDecimal denominator,
                     String source) implements Term {
        public Restate {
            boolean exact = function == StaticFunction.RESTATE_EXACT;
            if (!exact && function != StaticFunction.RESTATE || exact != (denominator != null))
                throw new IllegalArgumentException("een herschaling noemt HERSCHAAL_EXACT met een noemer of HERSCHAAL"
                        + " zonder, niet " + function + " met noemer " + denominator);
        }
    }

    /**
     * A division, {@code left} by {@code right}, both evaluated in that order, through {@code function}
     * ({@link StaticFunction#DIVIDED} or {@link StaticFunction#DIVIDED_ABS}) with the probe of the rule's §6.5 deposit: where a filled
     * dividend meets a leeg divisor the probe in slot {@code depositFlag} of the value array hears it, and with {@code depositFlag}
     * -1 the place does not deposit (RUNTIME-SEMANTICS ch. 1.2). A numeric zero divisor is the fault of ch. 1.4.
     */
    record Division(StaticFunction function, Term left, Term right, int depositFlag, String source) implements Term {}

    /**
     * A square root of {@code radicand} through {@code function} ({@link StaticFunction#SQUARE_ROOT} under a rounding,
     * {@link StaticFunction#EXACT_SQUARE_ROOT} without one) with the probe of the rule's §6.6 deposit: where the radicand is
     * negative the probe in slot {@code depositFlag} hears it, and with {@code depositFlag} -1 the place does not deposit.
     */
    record SquareRoot(StaticFunction function, Term radicand, int depositFlag, String source) implements Term {}

    /** The evaluation fault {@code sentence}, a {@code ComputeFault}, wherever the evaluation reaches this node: an
     *  {@code Unresolved} node (RUNTIME-SEMANTICS ch. 5.7, WF-044) or the bound day outside a day kind (ch. 8.2). */
    record Fail(String sentence, String source) implements Term {}

    /** The value of {@code value}, and the evaluation fault {@code sentence} where it is leeg: the evaluation date in a
     *  session without one (RUNTIME-SEMANTICS ch. 8.2). */
    record Required(Term value, String sentence, String source) implements Term {}

    /** The texts of {@code parts}, in order, joined: a text composition or a concatenation (IR-SPEC ch. 4.2). */
    record TextComposition(List<Term> parts, String source) implements Term {
        public TextComposition {
            parts = List.copyOf(parts);
        }
    }

    /**
     * A listed fold (RUNTIME-SEMANTICS ch. 1.2e): the operands in order, each taken by the step {@code function} as it is
     * evaluated, from leeg before the first: {@link StaticFunction#SMALLEST} keeps the least, {@link StaticFunction#LARGEST} the
     * greatest, a leeg operand skipped and a tie taking the later operand; leeg where every operand is.
     */
    record ListedFold(StaticFunction function, List<Term> operands, String source) implements Term {
        public ListedFold {
            if (function != StaticFunction.SMALLEST && function != StaticFunction.LARGEST)
                throw new IllegalArgumentException("een gelijste vouw neemt KLEINSTE of GROOTSTE als stap, niet " + function);
            operands = List.copyOf(operands);
        }
    }

    /**
     * The evaluation of a fold in the rule that reads it (RUNTIME-SEMANTICS ch. 5.10, 5.11): where the list of the
     * filter's deposits bound at slot {@code deposits} is not empty, the probe in slot {@code depositFlag} hears the
     * deposit, with the EF codes those facts carry (ch. 5.7b; none for the square root); then, where the least filter fault bound at slot {@code filterFault} stands, its fault; then, where the least member
     * fault bound at slot {@code memberFault} stands, its fault; then the fold's value {@code outcome}. A slot of -1: the
     * fold has none of that kind. The analysis adds no node where the fold has none of the three.
     */
    record FoldRead(Term outcome, int deposits, int depositFlag, int filterFault, int memberFault, String source)
            implements Term {}

    /**
     * A composite condition (RUNTIME-SEMANTICS ch. 5.10, IR-SPEC ch. 4.5): its members, truth values, read in the order
     * of {@code members}, the wire order, and counted under {@code quantifier}, with the threshold {@code threshold} of a
     * counting quantifier. Before each member the stop triggers of ch. 5.10 are tested on the members read so far
     * ({@code ComputeHelpers.established}), and the composite stops at the first point from which its verdict stands, so a
     * member after it is not evaluated: it raises no fault and deposits nothing. Where no trigger stops it, every member
     * is read and the verdict is the quantifier's over them ({@code ComputeHelpers.endVerdict}).
     */
    record Compound(Condition.Quantifier quantifier, int threshold, List<Term> members, String source) implements Term {
        public Compound {
            members = List.copyOf(members);
        }
    }

    /**
     * The verdict of a quantifier fold whose matrix can fault or deposit, in the rule that reads the fold
     * (RUNTIME-SEMANTICS ch. 3.9a, 5.10, 5.11), through {@code function} ({@link StaticFunction#QUANTIFIER_READ}): the outcome of
     * the matrix per contributor, the list {@code contributions} over the count {@code count}, read in the contributors'
     * order under {@code quantifier} with the threshold {@code threshold} of a counting quantifier, the stop triggers of
     * ch. 5.10 tested before each contributor. A contributor that is reached deposits where its matrix deposited, heard
     * by the probe in slot {@code depositFlag} of the value array (-1: the matrix does not deposit), and faults where its
     * matrix faulted; one that is not reached does neither. A fold whose matrix can do neither is an {@link Call} of
     * {@link StaticFunction#QUANTIFIER_VERDICT} over the count and the sum of the verdicts, where the order does not show.
     */
    record QuantifierRead(StaticFunction function, Term count, Term contributions, Condition.Quantifier quantifier, int threshold, int depositFlag,
                         String source) implements Term {
        public QuantifierRead {
            if (function != StaticFunction.QUANTIFIER_READ)
                throw new IllegalArgumentException("een kwantorlezing neemt KWANTORLEZING als functie, niet " + function);
        }
    }

    /**
     * The condition {@code condition} where its evaluation answers, and not holding where it faults
     * (RUNTIME-SEMANTICS ch. 5.2): a part of a test a form composes itself, where another rule takes the fault. An
     * exception that is no evaluation fault is a broken contract and passes.
     */
    record WithoutFault(Term condition, String source) implements Term {}

    /** The predicate of the day kind {@code naam} over a calendar day: a constant of the document, which reads nothing
     *  of working memory (IR-SPEC ch. 3.7, WF-031). */
    record DayKind(String name, Predicate<LocalDate> datePredicate, String source) implements Term {}
}
