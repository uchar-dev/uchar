package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Eenheid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

/**
 * A value or a condition of a rule as the analysis plans it: a tree of calls over the values the rule's left-hand side
 * binds, in a closed vocabulary that a rendering turns into a function over the rule's value array or into DRL text
 * without deciding anything itself. A condition is a term whose value is a truth value. Every decision the analysis
 * takes about a value or a condition is a node or a field of a node: the function that computes it, by name
 * ({@link Functie}), among them the helper that compares and the operator, polarity and leeg tables it takes as
 * constants; the factor that restates a unit ({@link Herschaal}); where a division, a square root or the matrix of a
 * quantifier fold deposits ({@link Deel}, {@link Wortel}, {@link Kwantorlezing}); where the evaluation faults
 * ({@link Faal}, {@link Vereist}, {@link Vouwlezing} and the functions that can, {@link Functie#kanFalen}) and where a
 * fault does not hold instead ({@link ZonderFout}); what is evaluated only on need, on one branch or up to a stop
 * ({@link Let}, {@link Keuze}, {@link Samengesteld}). The children of a node are evaluated in the order the node lists
 * them, which is the order of RUNTIME-SEMANTICS ch. 5.11.
 *
 * <p>A node that stands for a node of the IR carries that node's source fragment, the IR's {@code source.snippet}
 * ({@link #bron}): a rendering into DRL text puts it above the text as a comment. It is null where the IR has none, and
 * on a node the analysis adds without a node of its own in the IR, such as the arguments of a helper.
 */
public sealed interface Term {

    /** The source fragment of the IR node this term stands for, or null. */
    String bron();

    /**
     * The value {@code waarde}, exactly as the analysis made it: a number as a {@code BigDecimal} with its scale or as
     * an exact rational ({@code Breuk}), a date, a date-time, a truth value, a text or an enumeration label. {@code type}
     * is the IR's type of a literal, with its unit, and a text for the literal chunk of a text composition; it is null
     * for a constant of the analysis that an argument of a {@link Functie} takes (a number of decimals, a rounding mode,
     * a unit of time, a label, a class), and for an absent bound, whose value is null.
     */
    record Const(Object waarde, Datatype type, String bron) implements Term {}

    /** The value the left-hand side binds at slot {@code slot} of the value array ({@link Leestabel}). */
    record Lees(int slot, String bron) implements Term {}

    /**
     * The read of the binder {@code naam} of the {@link Let} whose slot is {@code slot} (RUNTIME-SEMANTICS ch. 10.6): the
     * first read evaluates the {@code Let}'s value and every further one reads that result; a leeg value ends that
     * {@code Let} with leeg.
     */
    record Binder(int slot, String naam, String bron) implements Term {}

    /**
     * {@code lichaam} with the binder {@code naam} bound to {@code waarde} as a need (RUNTIME-SEMANTICS ch. 10.6): the
     * value is evaluated at the first read of the binder ({@link Binder}, slot {@code slot}, which no pattern binds) and
     * not at all where the body reads none. {@code gretig} holds where the body reads the binder before anything else of
     * it can fault, deposit or end an evaluation, so that evaluating the value before the body gives the same outcome,
     * the same faults and the same deposits; a rendering that cannot evaluate by need may then evaluate it first. The
     * rendering into closures evaluates by need always.
     */
    record Let(int slot, String naam, Term waarde, Term lichaam, boolean gretig, String bron) implements Term {}

    /** The condition {@code voorwaarde}, then exactly the branch it chooses: {@code dan} where it holds, {@code anders}
     *  where not (RUNTIME-SEMANTICS ch. 10.2). The other branch evaluates nothing. */
    record Keuze(Term voorwaarde, Term dan, Term anders, String bron) implements Term {}

    /** The function {@code functie} on the values of {@code argumenten}, evaluated in that order. */
    record Aanroep(Functie functie, List<Term> argumenten, String bron) implements Term {
        public Aanroep {
            argumenten = List.copyOf(argumenten);
        }
    }

    /**
     * The value of {@code term} restated from the unit {@code van} into the unit {@code naar} (RUNTIME-SEMANTICS ch.
     * 2.6) through {@code functie}: {@link Functie#HERSCHAAL_EXACT}, a number times {@code teller} and divided by
     * {@code noemer}, the exact canonical factors, as exact rationals; or {@link Functie#HERSCHAAL}, where
     * {@code noemer} is null, times the single ratio {@code teller} of the two units. A leeg value and a value that is
     * no number pass unchanged. The analysis adds no node where no restatement is due.
     */
    record Herschaal(Functie functie, Term term, Eenheid van, Eenheid naar, BigDecimal teller, BigDecimal noemer,
                     String bron) implements Term {
        public Herschaal {
            boolean exact = functie == Functie.HERSCHAAL_EXACT;
            if (!exact && functie != Functie.HERSCHAAL || exact != (noemer != null))
                throw new IllegalArgumentException("een herschaling noemt HERSCHAAL_EXACT met een noemer of HERSCHAAL"
                        + " zonder, niet " + functie + " met noemer " + noemer);
        }
    }

    /**
     * A division, {@code links} by {@code rechts}, both evaluated in that order, through {@code functie}
     * ({@link Functie#GEDEELD} or {@link Functie#GEDEELD_ABS}) with the probe of the rule's §6.5 deposit: where a filled
     * dividend meets a leeg divisor the probe in slot {@code sonde} of the value array hears it, and with {@code sonde}
     * -1 the place does not deposit (RUNTIME-SEMANTICS ch. 1.2). A numeric zero divisor is the fault of ch. 1.4.
     */
    record Deel(Functie functie, Term links, Term rechts, int sonde, String bron) implements Term {}

    /**
     * A square root of {@code radicand} through {@code functie} ({@link Functie#WORTEL} under a rounding,
     * {@link Functie#EXACTE_WORTEL} without one) with the probe of the rule's §6.6 deposit: where the radicand is
     * negative the probe in slot {@code sonde} hears it, and with {@code sonde} -1 the place does not deposit.
     */
    record Wortel(Functie functie, Term radicand, int sonde, String bron) implements Term {}

    /** The evaluation fault {@code zin}, a {@code Rekenfout}, wherever the evaluation reaches this node: an
     *  {@code Unresolved} node (RUNTIME-SEMANTICS ch. 5.7, WF-044) or the bound day outside a day kind (ch. 8.2). */
    record Faal(String zin, String bron) implements Term {}

    /** The value of {@code waarde}, and the evaluation fault {@code zin} where it is leeg: the evaluation date in a
     *  session without one (RUNTIME-SEMANTICS ch. 8.2). */
    record Vereist(Term waarde, String zin, String bron) implements Term {}

    /** The texts of {@code delen}, in order, joined: a text composition or a concatenation (IR-SPEC ch. 4.2). */
    record Tekstreeks(List<Term> delen, String bron) implements Term {
        public Tekstreeks {
            delen = List.copyOf(delen);
        }
    }

    /**
     * A listed fold (RUNTIME-SEMANTICS ch. 1.2e): the operands in order, each taken by the step {@code functie} as it is
     * evaluated, from leeg before the first: {@link Functie#KLEINSTE} keeps the least, {@link Functie#GROOTSTE} the
     * greatest, a leeg operand skipped and a tie taking the later operand; leeg where every operand is.
     */
    record GelijsteVouw(Functie functie, List<Term> operanden, String bron) implements Term {
        public GelijsteVouw {
            if (functie != Functie.KLEINSTE && functie != Functie.GROOTSTE)
                throw new IllegalArgumentException("een gelijste vouw neemt KLEINSTE of GROOTSTE als stap, niet " + functie);
            operanden = List.copyOf(operanden);
        }
    }

    /**
     * The evaluation of a fold in the rule that reads it (RUNTIME-SEMANTICS ch. 5.10, 5.11): where the number of the
     * filter's deposits bound at slot {@code deponeringen} is not 0, the probe in slot {@code sonde} hears the deposit;
     * then, where the least filter fault bound at slot {@code filterfout} stands, its fault; then, where the least member
     * fault bound at slot {@code lidfout} stands, its fault; then the fold's value {@code uitkomst}. A slot of -1: the
     * fold has none of that kind. The analysis adds no node where the fold has none of the three.
     */
    record Vouwlezing(Term uitkomst, int deponeringen, int sonde, int filterfout, int lidfout, String bron)
            implements Term {}

    /**
     * A composite condition (RUNTIME-SEMANTICS ch. 5.10, IR-SPEC ch. 4.5): its members, truth values, read in the order
     * of {@code leden}, the wire order, and counted under {@code kwantor}, with the threshold {@code drempel} of a
     * counting quantifier. Before each member the stop triggers of ch. 5.10 are tested on the members read so far
     * ({@code Rekenhulpen.vastgesteld}), and the composite stops at the first point from which its verdict stands, so a
     * member after it is not evaluated: it raises no fault and deposits nothing. Where no trigger stops it, every member
     * is read and the verdict is the quantifier's over them ({@code Rekenhulpen.eindoordeel}).
     */
    record Samengesteld(Conditie.Kwantor kwantor, int drempel, List<Term> leden, String bron) implements Term {
        public Samengesteld {
            leden = List.copyOf(leden);
        }
    }

    /**
     * The verdict of a quantifier fold whose matrix can fault or deposit, in the rule that reads the fold
     * (RUNTIME-SEMANTICS ch. 3.9a, 5.10, 5.11), through {@code functie} ({@link Functie#KWANTORLEZING}): the outcome of
     * the matrix per contributor, the list {@code bijdragen} over the count {@code aantal}, read in the contributors'
     * order under {@code kwantor} with the threshold {@code drempel} of a counting quantifier, the stop triggers of
     * ch. 5.10 tested before each contributor. A contributor that is reached deposits where its matrix deposited, heard
     * by the probe in slot {@code sonde} of the value array (-1: the matrix does not deposit), and faults where its
     * matrix faulted; one that is not reached does neither. A fold whose matrix can do neither is an {@link Aanroep} of
     * {@link Functie#KWANTOROORDEEL} over the count and the sum of the verdicts, where the order does not show.
     */
    record Kwantorlezing(Functie functie, Term aantal, Term bijdragen, Conditie.Kwantor kwantor, int drempel, int sonde,
                         String bron) implements Term {
        public Kwantorlezing {
            if (functie != Functie.KWANTORLEZING)
                throw new IllegalArgumentException("een kwantorlezing neemt KWANTORLEZING als functie, niet " + functie);
        }
    }

    /**
     * The condition {@code voorwaarde} where its evaluation answers, and not holding where it faults
     * (RUNTIME-SEMANTICS ch. 5.2): a part of a test a form composes itself, where another rule takes the fault. An
     * exception that is no evaluation fault is a broken contract and passes.
     */
    record ZonderFout(Term voorwaarde, String bron) implements Term {}

    /** The predicate of the day kind {@code naam} over a calendar day: a constant of the document, which reads nothing
     *  of working memory (IR-SPEC ch. 3.7, WF-031). */
    record Dagsoort(String naam, Predicate<LocalDate> predikaat, String bron) implements Term {}

    /**
     * A condition as the condition compiler compiled it: a predicate over the value array, a truth value.
     *
     * <p>INTERIM FORM, until step 1b3 of card drl-teksttak-op-truth-maintenance plans what it still holds: a quantifier
     * fold, a test of the §9.5 channel, the negation of a consistency's assertion and the tests a form composes itself.
     * No rendering can show what it tests.
     */
    record GecompileerdeToets(Predicate<Object[]> toets, String bron) implements Term {}
}
