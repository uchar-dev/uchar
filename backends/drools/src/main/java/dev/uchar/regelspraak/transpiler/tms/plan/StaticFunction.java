package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.ComputeHelpers;
import dev.uchar.regelspraak.transpiler.tms.Period;

/**
 * A static function a {@link Term} calls, by its class and its name: a helper of the runtime, which the other branch
 * calls as well, or a helper of this branch ({@link ComputeHelpers}) that holds what the value compilers computed around
 * such a call. A rendering into closures calls the function itself; a rendering into DRL text calls it
 * class-qualified, never through an {@code import function} ({@link Route}), and finds it by name, so no function is
 * overloaded. Every argument is a value, evaluated before the call in the order of
 * {@link Term.Call#arguments}; a constant of the analysis (a number of decimals, a rounding mode, a unit of time, a
 * class) is a {@link Term.Const}. {@link #canFail} says whether the function can raise an evaluation fault, a division by a
 * numeric zero or a {@code ComputeFault} or an {@code InputFormFault} of the branch (RUNTIME-SEMANTICS ch. 5.2).
 */
public enum StaticFunction {
    /** A sum, a leeg operand 0 (ch. 1.2), exact where an operand is a rational. */
    RS_PLUS(RegelSpraakRuntime.class, "rsPlus", false),
    /** A difference, a leeg operand 0. */
    RS_MIN(RegelSpraakRuntime.class, "rsMin", false),
    /** "verminderd met": a leeg left operand leeg, a leeg right one 0. */
    RS_REDUCED(RegelSpraakRuntime.class, "rsVerminderd", false),
    /** A product, a leeg operand 0. */
    RS_TIMES(RegelSpraakRuntime.class, "rsMaal", false),
    /** A percentage of a value, exact. */
    RS_PERCENTAGE_OF(RegelSpraakRuntime.class, "rsPercentageVan", false),
    /** A power with a whole exponent of at least 0, exact (ch. 2.2); the exponent an {@code Integer} constant. */
    RS_POWER(RegelSpraakRuntime.class, "rsMacht", false),
    /** A rounding (ch. 2.5) to a number of decimals, an {@code Integer} constant, by a {@code RoundingMode} constant. */
    RS_ROUNDING(RegelSpraakRuntime.class, "rsAfronding", false),
    /** A clamp (ch. 2.7) of a value by a minimum and a maximum, an absent or leeg bound no bound. */
    RS_CLAMP(RegelSpraakRuntime.class, "rsBegrens", false),
    /** The absolute value. */
    RS_ABS(RegelSpraakRuntime.class, "rsAbs", false),
    /** The text of a text or an enumeration label, a leeg value the empty text (ch. 1.2). */
    RS_TEXT(RegelSpraakRuntime.class, "rsTekst", false),
    /** A duration in whole units (ch. 3.8) between two dates, the unit a {@code String} constant as
     *  {@code TimeCalculation.chronoUnit} names it, absolute where a {@code Boolean} constant says so. */
    RS_DURATION(RegelSpraakRuntime.class, "rsTijdsduur", false),
    /** A duration in whole days of a day kind between two dates (ch. 3.8), the kind a {@link Term.DayKind}, absolute
     *  where a {@code Boolean} constant says so. */
    RS_DURATION_DAY_KIND(RegelSpraakRuntime.class, "rsTijdsduurDagsoort", false),
    /** A part of a date or a date-time, the part a {@code ChronoField} constant. */
    RS_DATE_PART(RegelSpraakRuntime.class, "rsDatumDeel", false),
    /** The calendar year of a date, as a whole number without a unit. */
    RS_EVALUATION_YEAR(RegelSpraakRuntime.class, "rsRekenjaar", false),
    /** The sum of the cells of a dimension map at a {@code List} constant of labels, a leeg cell skipped. */
    RS_DIM_SUM(RegelSpraakRuntime.class, "rsDimSom", false),
    /** The least cell of a dimension map at a list of labels. */
    RS_DIM_MIN(RegelSpraakRuntime.class, "rsDimMin", false),
    /** The greatest cell of a dimension map at a list of labels. */
    RS_DIM_MAX(RegelSpraakRuntime.class, "rsDimMax", false),
    /** The total over time of a timeline on a grid, a {@code String} constant (ch. 3.6). */
    RS_TIME_TOTAL(RegelSpraakRuntime.class, "rsTijdTotaal", false),
    /** A timeline clipped to the true periods of a characteristic (ch. 3.6), the segment class a constant. */
    RS_TIME_CLIP(RegelSpraakRuntime.class, "rsTijdKnip", false),
    /** A timeline clipped to the periods outside the true periods of a characteristic, within its segments. */
    RS_TIME_CLIP_NOT(RegelSpraakRuntime.class, "rsTijdKnipNiet", false),
    /** The true periods of a comparison of a timeline with a value, the comparison an {@code Integer} code of
     *  {@code TimeCalculation.test}, the value a {@code BigDecimal}. */
    RS_TIME_CONDITION(RegelSpraakRuntime.class, "rsTijdConditie", false),
    /** The pro-rata part of a base timeline per bucket of a grid over the true periods of a characteristic (ch. 3.7). */
    RS_TIME_PROPORTIONAL(RegelSpraakRuntime.class, "rsTijdsevenredig", false),
    /** The true days per bucket of a grid on the calendar (ch. 3.8). */
    RS_DAY_COUNT(RegelSpraakRuntime.class, "rsAantalDagenIn", false),
    /** The true days per bucket of a grid from the anchor of a fixed start point, a {@code LocalDate} constant (ch. 3.2). */
    RS_DAY_COUNT_FROM(RegelSpraakRuntime.class, "rsAantalDagenInVanaf", false),
    /** A timeline as this branch carries it, a list of {@code Period}, from the segments a runtime helper returns. */
    PERIOD_LIST(Period.class, "list", false),
    /** A division (ch. 1.4, 1.2): a leeg dividend 0, a leeg divisor leeg with the §6.5 deposit to the probe, a numeric
     *  zero the fault; the exact quotient. The function of a {@link Term.Division}. */
    DIVIDED(ComputeHelpers.class, "divided", true),
    /** As {@link #DIVIDED}, the quotient truncated toward zero at 5 decimals. */
    DIVIDED_ABS(ComputeHelpers.class, "dividedAbs", true),
    /** The square root at 34 digits under the rounding §13.4.17.13 mandates, a negative radicand leeg with the §6.6
     *  deposit to the probe. The function of a {@link Term.SquareRoot}. */
    SQUARE_ROOT(ComputeHelpers.class, "squareRoot", false),
    /** The exact square root of a rational square, a negative radicand leeg with the §6.6 deposit to the probe, and
     *  any other radicand the fault of an irrational root without a rounding (ch. 2.2). The function of a
     *  {@link Term.SquareRoot}. */
    EXACT_SQUARE_ROOT(ComputeHelpers.class, "exactSquareRoot", true),
    /** A date or a date-time plus or minus a count in a calendar or clock unit, a {@code String} constant, adding where
     *  a {@code Boolean} constant says so; a step outside the range of ch. 3.8 the fault. */
    DATE_STEP(ComputeHelpers.class, "dateStep", true),
    /** A date plus or minus a count of days of a day kind, a {@link Term.DayKind}, adding where a {@code Boolean}
     *  constant says so (ch. 3.8); a walk past the reach the fault. */
    DAY_KIND_STEP(ComputeHelpers.class, "dayKindStep", true),
    /** As {@link #DAY_KIND_STEP} on a date-time, over its date part, the clock time riding through. */
    DATE_TIME_DAY_KIND_STEP(ComputeHelpers.class, "dateTimeDayKindStep", true),
    /** easterSunday of a whole year without a unit (ch. 3.8); a year that is not whole, or outside the years a date
     *  holds, the fault. */
    EASTER_SUNDAY(ComputeHelpers.class, "easterSunday", true),
    /** A date of three parts, after the spelling of the builtin, a {@code String} constant for the sentence of a
     *  fault; a part that is not a whole number within a date part's range, or parts that name no day, the fault. */
    DATUM_MET(ComputeHelpers.class, "dateWith", true),
    /** As {@link #DATUM_MET}, a date-time of seven parts. */
    DATUMTIJD_MET(ComputeHelpers.class, "dateTimeWith", true),
    /** The cell at a label, a {@code String} constant, of a dimension map, leeg where the map or the label is absent. */
    CELL(ComputeHelpers.class, "cell", false),
    /** A dimension map as it is, or leeg where the value is no map. */
    MAP(ComputeHelpers.class, "map", false),
    /** A value restated by the exact canonical factors of two units (ch. 2.1, 2.6), a {@code BigDecimal} numerator and
     *  denominator, as rationals; a leeg value and a value that is no number pass unchanged. The function of a
     *  {@link Term.Restate}. */
    RESTATE_EXACT(ComputeHelpers.class, "restateExact", false),
    /** A value restated by the single ratio of two units, a {@code BigDecimal}, a rate whose factors are not both
     *  known; a leeg value and a value that is no number pass unchanged. The function of a {@link Term.Restate}. */
    RESTATE(ComputeHelpers.class, "restate", false),
    /** One step of a listed fold that keeps the least operand (ch. 1.2e): from the value so far and the next operand,
     *  a leeg operand skipped, a tie taking the later one, compared exactly ({@code ComputeHelpers.compare}). The function
     *  of a {@link Term.ListedFold}. */
    SMALLEST(ComputeHelpers.class, "smallest", false),
    /** As {@link #SMALLEST}, keeping the greatest operand. */
    LARGEST(ComputeHelpers.class, "largest", false),
    /** A value, or the fallback where it is leeg. */
    FALLBACK(ComputeHelpers.class, "fallback", false),
    /** A count bound off an accumulate, as a number. */
    COUNT(ComputeHelpers.class, "countOf", false),
    /** The value of a fold from its count, its folded value and its fallback: the fallback over no contribution. */
    FOLD_VALUE(ComputeHelpers.class, "foldValue", false),
    /** The subject's own total over time without a clip (ch. 3.6): from the asOfDate-bucket scalar of the input, the
     *  fault for a full timeline and for a session without an evaluation date. */
    OWN_TOTAL(ComputeHelpers.class, "ownTotal", true),
    /** The full timeline a form reads whole, the fault where the case gives the asOfDate-bucket scalar (ch. 8.4), in
     *  a sentence that names the object. */
    COMPLETE(ComputeHelpers.class, "complete", true),
    /** A value at rest: an exact rational as a decimal (§6.1.2), any other value as it is. */
    COLLAPSE(ComputeHelpers.class, "collapse", false),

    // the functions of a condition below give a truth value

    /** A comparison of two numbers with the empty operand of ch. 1.2: an ordering with a leeg operand is onwaar, two
     *  leeg numbers are equal, exactly one leeg is unequal; exact where a value is a rational. The operator a
     *  {@code String} constant as {@code Operators.symbol} spells it. */
    RS_AGG_COMPARE(RegelSpraakRuntime.class, "rsAggVergelijk", false),
    /** A comparison of two dates or two date-times, a value that is no date reading as leeg, the operator a
     *  {@code Condition.Operator} constant: an ordering with a leeg operand is onwaar, two leeg dates are not equal
     *  (§5.3 Tabel 38), and they are unequal where exactly one is leeg (§5.6 Tabel 44). */
    DATE_COMPARE(ComputeHelpers.class, "dateCompare", false),
    /** The equality of two texts, enumeration labels or truth values with the empty operand of ch. 1.2, a
     *  {@code Boolean} constant asking equal (true) or unequal (false): equal only where both are filled and the same,
     *  unequal where exactly one is leeg or the two differ. */
    TEXT_EQUAL(ComputeHelpers.class, "textEqual", false),
    /** An enumeration label up to its normalisation, a leeg value leeg: both sides of an equality where either is an
     *  enumeration (maintainer decision 2026-09-20). */
    RS_ENUM_NAME(RegelSpraakRuntime.class, "rsEnumNaam", false),
    /** The leeg test, where a {@code Boolean} constant is true, or the gevuld test, where it is false, of a value
     *  (ch. 5.8 rule 1: a read over an absent link is leeg). */
    EMPTY_TEST(ComputeHelpers.class, "emptyTest", false),
    /** The eleven test of a value ({@code rsElfproef}, §8.1.3), whose verdict a {@code Boolean} constant must equal:
     *  false for the written negation. */
    ELEVEN_TEST(ComputeHelpers.class, "elevenTest", false),
    /** The exact-digit test of a value ({@code rsNumeriekMetExact}, §8.1.4) with an {@code Integer} constant of
     *  digits, whose verdict a {@code Boolean} constant must equal. */
    NUMERIEK_MET_EXACT(ComputeHelpers.class, "exactDigits", false),
    /** A characteristic check of the subject (ch. 5.8): its truth value off a total standing, which a {@code Boolean}
     *  constant must equal: false for a negated check. */
    KENMERK(ComputeHelpers.class, "characteristic", false),
    /** A characteristic check of the object a path of roles reaches, as {@link #KENMERK}; over an absent link, whose
     *  read is leeg, it holds in neither polarity (ch. 5.8 rule 3). */
    CHARACTERISTIC_CHECK_ALONG_PATH(ComputeHelpers.class, "characteristicCheckAlongPath", false),
    /** A role check over a count of links (ch. 5.8a): whether there is one, complemented where a {@code Boolean}
     *  constant says the check is negated. */
    ROLE_COUNT(ComputeHelpers.class, "roleCount", false),
    /** A role check over the subject's own reference field, bound as whether it is set, complemented where a
     *  {@code Boolean} constant says the check is negated. */
    ROLE_FIELD(ComputeHelpers.class, "roleField", false),
    /** The evaluation-date gate of a time window (ch. 3.10) over its bounds, each with a {@code Boolean} constant that
     *  says whether the window has it, a {@code Boolean} constant for an inclusive end, and the evaluation date: an
     *  absent bound is open, a leeg one and an unset date never match. */
    TIME_WINDOW(ComputeHelpers.class, "timeWindow", false),
    /** The fold of the whole-period qualifier (ch. 3.9, {@code rsGeheelPeriode}) over a timeline, with the bound of a
     *  comparison or none, the grid ("JAAR" or "MAAND") and the test's code as constants, complemented where a
     *  {@code Boolean} constant says the qualifier is negated. */
    WHOLE_PERIOD(ComputeHelpers.class, "wholePeriod", false),
    /** The verdict of a quantifier fold whose matrix can neither fault nor deposit (ch. 3.9a), from the count of its
     *  contributors and the sum of their verdicts, 1 or 0 each, under a {@code Condition.Quantifier} constant with an
     *  {@code Integer} threshold; over no contributor the sum is not read. */
    QUANTIFIER_VERDICT(ComputeHelpers.class, "quantifierVerdict", false),
    /** The verdict of a quantifier fold whose matrix can fault or deposit, read over the outcome per contributor in
     *  the contributors' order and stopped as ch. 5.10 says; a contributor that is reached deposits to the probe and
     *  raises its fault. The function of a {@link Term.QuantifierRead}. */
    QUANTIFIER_READ(ComputeHelpers.class, "quantifierRead", true),
    /** A test of the §9.5 channel of a form (ch. 5.6): whether the standing of its channel, bound off its
     *  {@code ChannelState}, says it is inconsistent. */
    CHANNEL(ComputeHelpers.class, "channel", false),
    /** The negation of a condition's truth value: the test of a consistency, which holds where its assertion does not
     *  (IR-SPEC ch. 3.5). */
    NIET(ComputeHelpers.class, "not", false),
    /** Whether a value is the asOfDate-bucket scalar a case gives for a timeline (ch. 8.4), which a {@code Boolean}
     *  constant must equal: a part of a test a form composes itself. */
    BUCKET_SCALAR(ComputeHelpers.class, "bucketScalar", false),
    /** Whether the target of an initialisation is empty for its guard (RUNTIME-SEMANTICS ch. 5.4): another writer wrote
     *  leeg on the subject, by a count of such writers, or the subject's input on the target, which the plan reads off
     *  the bean ({@link Term.Input}), is leeg. A part of a test a form composes itself. */
    TARGET_EMPTY(ComputeHelpers.class, "targetEmpty", false),
    /** Whether the value facts on one attribute of one object hold two different values, leeg among them, numbers by
     *  value (RUNTIME-SEMANTICS ch. 5.7). A test a form composes itself. */
    DIFFER(ComputeHelpers.class, "differ", false),
    /** Whether more than one key of a uniqueness collides with a key, by the count of the keys that collide with it,
     *  itself included (RUNTIME-SEMANTICS ch. 5.9): the test of the rule that finds a duplicate. */
    DUPLICATE(ComputeHelpers.class, "duplicate", false),
    /** Whether a key of a uniqueness is the same key of one form as the key another pattern binds
     *  ({@code UniquenessKey.collidesWith}): a constraint between the two ({@link BetaTest}). */
    COLLIDES_WITH(ComputeHelpers.class, "collidesWith", false),
    /** Whether the walk over the reference fields of one chain from a pattern's object and the walk over those of
     *  another from the object another pattern binds reach one object, by identity, a broken link meeting nothing: a
     *  constraint of a fold's range of more steps on its member or on an intermediate ({@link BetaTest}), the two
     *  chains its constants. */
    MEETING(ComputeHelpers.class, "meeting", false),
    /** Whether a {@code ListPlace} is the place of the member that the walk over the reference fields of a chain
     *  from the object another pattern binds reaches, by identity: the constraint of the accumulate that binds a
     *  member's place in its list ({@link BetaTest}), the chain its constant. */
    PLACE_OF(ComputeHelpers.class, "placeOf", false),
    /** Whether a {@code CreationStamp} is of the creation phase whose number the other pattern binds: the constraint of an
     *  object creation's guard that keeps an object made in the phase that fires from being a subject before the next
     *  one ({@link BetaTest}). */
    IN_PHASE(ComputeHelpers.class, "inPhase", false),
    /** Whether a {@code RelationLink} is of another creation phase than the one whose number the other pattern binds: the
     *  constraint of a fact creation's path over a relation a form creates, which does not walk a link of the phase it
     *  fires in ({@link BetaTest}). */
    NOT_IN_PHASE(ComputeHelpers.class, "notInPhase", false),
    /** Whether two {@code RelationLink} facts hold different objects at the end their constant names ({@code "eerste"} or
     *  {@code "tweede"}), by identity: the constraint of the rule of a second filler of a to-one role, between the
     *  second link and the first ({@link BetaTest}). */
    OTHER_FILLING(ComputeHelpers.class, "otherFilling", false),
    /** Whether an {@code Uitkomst} is of an earlier member chain than the one another pattern binds, by the number of
     *  its chain: the constraint of a reconciliation of a defaults stack that keeps to the first member in member order
     *  (RUNTIME-SEMANTICS ch. 7.2; {@link BetaTest}). */
    EARLIER_MEMBER(ComputeHelpers.class, "earlierMember", false),
    /** Whether an {@code Uitkomst} decides otherwise than the one another pattern binds, numbers by value
     *  (RUNTIME-SEMANTICS ch. 7.2): the constraint of a reconciliation of a defaults stack that finds a member which
     *  decides differently ({@link BetaTest}). */
    OTHER_DECISION(ComputeHelpers.class, "anotherDecision", false);

    private final Class<?> javaClass;
    private final String method;
    private final boolean canFail;

    StaticFunction(Class<?> javaClass, String method, boolean canFail) {
        this.javaClass = javaClass;
        this.method = method;
        this.canFail = canFail;
    }

    /** The class whose public static method this is. */
    public Class<?> javaClass() { return javaClass; }

    /** The name of the method. */
    public String method() { return method; }

    /** Whether a call can raise an evaluation fault. */
    public boolean canFail() { return canFail; }
}
