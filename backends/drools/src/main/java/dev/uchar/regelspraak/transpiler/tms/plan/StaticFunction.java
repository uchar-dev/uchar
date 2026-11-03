package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.ComputeHelpers;
import dev.uchar.regelspraak.transpiler.tms.Period;
import dev.uchar.regelspraak.transpiler.tms.PeriodArithmetic;
import dev.uchar.regelspraak.transpiler.tms.PeriodCondition;

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
    /** A power with a whole exponent, exact (ch. 2.2), a negative exponent inverting; the exponent an {@code Integer}
     *  constant. */
    RS_POWER(RegelSpraakRuntime.class, "rsMacht", false),
    /** A power with a fractional exponent that is no base of a rounding, its numerator and its denominator in lowest
     *  terms {@code Integer} constants: the exact root where the power is rational, else the fault (ch. 2.2). */
    EXACT_POWER(ComputeHelpers.class, "exactPower", true),
    /** A power with a fractional exponent under its rounding (§6.7), the exponent as for {@link #EXACT_POWER}, the
     *  decimals an {@code Integer} and the mode a {@code RoundingMode} constant: the true power rounded once (ch. 2.2,
     *  2.5). */
    ROUNDED_POWER(ComputeHelpers.class, "roundedPower", true),
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
    /** The sum of the cells of a dimension map at a {@code List} constant of cells, each the {@code List} of its
     *  labels, a leeg cell skipped. */
    RS_DIM_SUM(RegelSpraakRuntime.class, "rsDimSom", false),
    /** The least cell of a dimension map at a list of cells. */
    RS_DIM_MIN(RegelSpraakRuntime.class, "rsDimMin", false),
    /** The greatest cell of a dimension map at a list of cells. */
    RS_DIM_MAX(RegelSpraakRuntime.class, "rsDimMax", false),
    /** The total over time of a timeline on a grid, a {@code String} constant (ch. 3.6). */
    RS_TIME_TOTAL(RegelSpraakRuntime.class, "rsTijdTotaal", false),
    /** A timeline clipped to the true periods of a characteristic (ch. 3.6), the segment class a constant. */
    RS_TIME_CLIP(RegelSpraakRuntime.class, "rsTijdKnip", false),
    /** A timeline clipped to the periods outside the true periods of a characteristic, within its segments. */
    RS_TIME_CLIP_NOT(RegelSpraakRuntime.class, "rsTijdKnipNiet", false),
    /** The true periods of a comparison of a timeline with a value, the comparison an {@code Integer} code of
     *  {@code TimeCalculation.test}, the value a decimal or an exact rational, compared exactly, a leeg side by ch. 1.2;
     *  with them the gaps of the timeline within the segments of a second timeline, where a leeg side satisfies the
     *  comparison (ch. 3.6). */
    RS_TIME_CONDITION(RegelSpraakRuntime.class, "rsTijdConditie", false),
    /** The pro-rata part of a base timeline per bucket of a grid over the true periods of a characteristic (ch. 3.7),
     *  exact, each segment under the write boundary of the target's decimals, an {@code Integer} constant, and its
     *  attribute, a {@code String} constant; a segment that does not fit the fault (ch. 2.4). */
    PRO_RATA(ComputeHelpers.class, "proRata", true),
    /** The true days per bucket of a grid on the calendar (ch. 3.8). */
    RS_DAY_COUNT(RegelSpraakRuntime.class, "rsAantalDagenIn", false),
    /** The true days per bucket of a grid from the anchor of a fixed start point, a {@code LocalDate} constant (ch. 3.2). */
    RS_DAY_COUNT_FROM(RegelSpraakRuntime.class, "rsAantalDagenInVanaf", false),
    /** A timeline as this branch carries it, a list of {@code Period}, from the segments a runtime helper returns. */
    PERIOD_LIST(Period.class, "list", false),
    /** The day count over all time (ch. 3.8) from the counted buckets a runtime helper returns: 0 on every period
     *  outside them. */
    PERIOD_DAY_COUNT(PeriodArithmetic.class, "dayCount", false),
    /** A scalar operand of the period arithmetic of ch. 3.4 as a timeline: the one segment with two open bounds, or
     *  none where it is leeg (ch. 3.1). */
    PERIOD_SCALAR(PeriodArithmetic.class, "scalar", false),
    /** Plus per period of all time over two timelines, a leeg operand 0 (ch. 3.4). */
    PERIOD_PLUS(PeriodArithmetic.class, "plus", false),
    /** Min per period of all time over two timelines, a leeg operand 0 (ch. 3.4). */
    PERIOD_MINUS(PeriodArithmetic.class, "minus", false),
    /** "verminderd met" per period of all time over two timelines, a leeg left operand leeg, a leeg right one 0
     *  (ch. 1.2, 3.4). */
    PERIOD_REDUCED(PeriodArithmetic.class, "reduced", false),
    /** A product per period of all time over two timelines, a leeg operand 0 (ch. 1.2, 3.4). */
    PERIOD_TIMES(PeriodArithmetic.class, "times", false),
    /** A percentage of a value per period of all time over two timelines, a leeg operand 0 (ch. 1.2, 3.4). */
    PERIOD_PERCENTAGE_OF(PeriodArithmetic.class, "percentageOf", false),
    /** A division per period of all time over two timelines with the probe of the rule's deposit, a period with a leeg
     *  or numeric-zero divisor under a filled dividend leeg (ch. 1.2, 3.4). The function of a {@link Term.Division}; it
     *  does not fault, and counts as one that can, as {@link #DIVIDED} does. */
    PERIOD_DIVIDED(PeriodArithmetic.class, "divided", true),
    /** As {@link #PERIOD_DIVIDED}, each quotient truncated toward zero at 5 decimals. */
    PERIOD_DIVIDED_ABS(PeriodArithmetic.class, "dividedAbs", true),
    /** The timeline of a time-dependent parameter as an operand of the period arithmetic (ch. 3.4), the parameter's
     *  name a {@code String} constant: the fault of an unbound parameter where the case gives none (EF-014). */
    PERIOD_PARAMETER(PeriodArithmetic.class, "parameter", true),
    /** The sum per period of the timelines a fold over a set collects, a leeg member skipped, with a fallback or none
     *  (ch. 3.5). */
    PERIOD_SUM(PeriodArithmetic.class, "sum", false),
    /** The timeline a rule of period arithmetic writes: its filled segments, each under the write boundary of the
     *  target's decimals, an {@code Integer} constant, and its attribute, a {@code String} constant (ch. 2.4, 3.1). */
    PERIOD_WRITTEN(PeriodArithmetic.class, "written", true),
    /** The total over time of a timeline on a grid, a {@code String} constant (ch. 3.6), by the calendar where a second
     *  {@code String} constant names the time unit of the rate's denominator and by the whole units of the grid where
     *  it is null, the attribute a {@code String} constant and the object whose timeline it is: an open segment of 0
     *  adds 0, one of another value the fault (EF-036). */
    PERIOD_TOTAL(PeriodArithmetic.class, "total", true),
    /** A timeline restricted to the periods in which its rule's condition holds, the periods first (ch. 3.10, the clip
     *  generalised): one fragment per overlap of a segment and a period. */
    PERIOD_CLIPPED(PeriodArithmetic.class, "clipped", false),
    /** The periods of a condition read per period (ch. 3.10) in which a check of the subject's own characteristic on a
     *  timeline holds: its true periods, maximal. */
    PERIOD_TRUE_PERIODS(PeriodCondition.class, "truePeriods", false),
    /** The periods of all time outside the true periods of a characteristic: a negated check read per period. */
    PERIOD_OUTSIDE(PeriodCondition.class, "outside", false),
    /** The periods of all time on which a comparison of a timeline with a bound holds per day (ch. 1.2), the two in the
     *  order the comparison names them, the operator a {@code String} constant as {@code Operators.symbol} spells it with
     *  the timeline on its left, and a {@code Boolean} constant that says whether the timeline comes first. */
    PERIOD_WHERE(PeriodCondition.class, "where", false),
    /** The whole-period qualifier per bucket of its grid, a {@code String} constant ("MAAND" or "JAAR"), over the periods
     *  in which its test holds, complemented over all time where a {@code Boolean} constant says it is negated (ch. 3.9 on
     *  a timeline target). */
    PERIOD_WHOLE_BUCKETS(PeriodCondition.class, "wholeBuckets", false),
    /** One step of the collection of the periods of a composite's members, from leeg before the first. */
    PERIOD_MEMBER(PeriodCondition.class, "member", false),
    /** The periods in which a composite read per period holds, from its members' periods, under a
     *  {@code Condition.Quantifier} constant with an {@code Integer} threshold, every member read (ch. 3.10). */
    PERIOD_COMPOSITE(PeriodCondition.class, "composite", false),
    /** The periods of a node of a condition read per period that reads no timeline: all time where its truth value
     *  holds, none where not (ch. 3.10). */
    PERIOD_ALWAYS(PeriodCondition.class, "always", false),
    /** A division (ch. 1.4, 1.2): a leeg dividend 0, a leeg divisor leeg with the §6.5 deposit to the probe, a numeric
     *  zero leeg with the deposit of ch. 1.4 point 4 to the probe; the exact quotient. The function of a
     *  {@link Term.Division}. It does not fault (card zero-division-channel), and counts as one that can: the plans of every form with a division are built on that, and changing it is a card of its own. */
    DIVIDED(ComputeHelpers.class, "divided", true),
    /** As {@link #DIVIDED}, the quotient truncated toward zero at 5 decimals. */
    DIVIDED_ABS(ComputeHelpers.class, "dividedAbs", true),
    /** The radicand of a square root under the rounding §13.4.17.13 mandates, a negative one leeg with the §6.6 deposit
     *  to the probe. The function of a {@link Term.SquareRoot}, which {@link #ROUNDED_SQUARE_ROOT} takes. */
    RADICAND(ComputeHelpers.class, "radicand", false),
    /** The square root of a radicand ({@link #RADICAND}) rounded once to a number of decimals, an {@code Integer}
     *  constant, by a {@code RoundingMode} constant (ch. 2.2, 2.5). */
    ROUNDED_SQUARE_ROOT(ComputeHelpers.class, "roundedSquareRoot", false),
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
    /** The cell of a dimension map at its labels, a {@code List} constant, leeg where the map or the cell is absent. */
    CELL(ComputeHelpers.class, "cell", false),
    /** The first cell of a dimension map at a list of cells, each the {@code List} of its labels, that holds a value, by
     *  position; leeg where none does. */
    CELL_FIRST(ComputeHelpers.class, "firstCell", false),
    /** The last cell of a dimension map at a list of cells that holds a value, by position. */
    CELL_LAST(ComputeHelpers.class, "lastCell", false),
    /** The number of the cells of a dimension map at a list of cells that hold a value. */
    FILLED_CELLS(ComputeHelpers.class, "filledCells", false),
    /** A dimension map as it is, or leeg where the value is no map. */
    MAP(ComputeHelpers.class, "map", false),
    /** A copy of a dimension map in which the cell at its labels, a {@code List} constant, holds a value, or nothing
     *  where it is leeg: a cell a form writes, read through its standing, in a fold over the cells. */
    WITH_CELL(ComputeHelpers.class, "withCell", false),
    /** A value restated by the canonical factors of two units (ch. 2.1, 2.6), a {@code BigDecimal} numerator and
     *  denominator, as rationals; a leeg value and a value that is no number pass unchanged. The function of a
     *  {@link Term.Restate}. */
    RESTATE_EXACT(ComputeHelpers.class, "restateExact", false),
    /** One step of a listed fold that keeps the least operand (ch. 1.2e): from the value so far and the next operand,
     *  a leeg operand skipped, a tie taking the later one, compared exactly ({@link ComputeHelpers#compare}). The function
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
    /** The subject's own total over time without a clip (ch. 3.6): from the asOfDate-bucket scalar of the input, by the
     *  calendar where a {@code String} constant names the time unit of the rate's denominator, the fault for a full
     *  timeline and for a session without an evaluation date. */
    OWN_TOTAL(ComputeHelpers.class, "ownTotal", true),
    /** The full timeline a form reads whole, the fault where the case gives the asOfDate-bucket scalar (ch. 8.4), in
     *  a sentence that names the object. */
    COMPLETE(ComputeHelpers.class, "complete", true),
    /** A value as a bean holds it: an exact rational as a decimal, any other value as it is. */
    COLLAPSE(ComputeHelpers.class, "collapse", false),
    /** The write boundary of RUNTIME-SEMANTICS ch. 2.4 point 1 on a target with declared decimals, an {@code Integer}
     *  constant: the value where it fits, as a decimal; else the fault of the attribute, a {@code String} constant, of
     *  the new object of an object creation, a {@code String} constant or null. */
    FIT_TARGET(ComputeHelpers.class, "fitTarget", true),

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
    /** The identity check (ch. 3.4a, IR-SPEC ch. 4.5): whether the member is the subject of the form, the same object,
     *  complemented where a {@code Boolean} constant says the check is negated. */
    SAME_OBJECT(ComputeHelpers.class, "sameObject", false),
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
    /** Whether a timeline has a segment with an open bound whose value is not 0, whose total has none (ch. 3.6): a
     *  part of a test a form composes itself. */
    OPEN_NONZERO(PeriodArithmetic.class, "openNonZero", false),
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
    /** Whether an {@code Outcome} is of an earlier member chain than the one another pattern binds, by the number of
     *  its chain: the constraint of a reconciliation of a defaults stack that keeps to the first member in member order
     *  (RUNTIME-SEMANTICS ch. 7.2; {@link BetaTest}). */
    EARLIER_MEMBER(ComputeHelpers.class, "earlierMember", false),
    /** Whether an {@code Outcome} decides otherwise than the one another pattern binds, numbers by value
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
