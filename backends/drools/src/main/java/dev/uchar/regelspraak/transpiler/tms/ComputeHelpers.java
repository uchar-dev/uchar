package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * The functions of this branch's values beside the runtime's helpers: what the value compilers computed around a
 * runtime call, as plain functions of the values the call receives, so that a value is one tree of named calls
 * ({@code plan.Term.Call}, {@code plan.Aggregation}) that the rendering into closures calls and a rendering into DRL text
 * calls class-qualified, never through an {@code import function}, which a field of a fact with the same name as a
 * helper shadows without a complaint. A function takes its arguments evaluated; the probe of a deposit is the
 * object its slot of the value array holds, a {@link DepositFlag} or nothing. A fault is an evaluation fault of the branch in
 * the reference interpreter's sentence ({@link Rekenfout}, {@link Invoervormfout}, a division by a numeric zero), which
 * the consequence or the fault rule of the form inserts as a {@link Fout}.
 *
 * <p>Beside them the routes of a test and a consequence ({@code plan.Route}): a fresh probe, a report to it and its
 * verdict, and the facts a consequence makes whose class or constructor a DRL text cannot reach, the fault of a fixed
 * sentence, the inconsistency of a form and the asOfDate-bucket scalar of an input, or whose number a computation over
 * the bound values decides: the fault of an input's form, the keys of a uniqueness, and the shares, the rest and the
 * deposits of an allocation. A {@link DepositFlag} and the generated
 * inconsistency class stay out of reach themselves: a rendering only hands them on, typed {@code Object}. Last the
 * routes of a DRL text, which MVEL needs where a test or a consequence would otherwise catch, throw, cast, build a
 * list or read a bean: no route is overloaded, so a call by name finds one method.
 */
public final class ComputeHelpers {

    private ComputeHelpers() {}

    // ---------------- division and square root, with the probe of their deposit ----------------

    /** A division (RUNTIME-SEMANTICS ch. 1.2, 1.4, {@code rsGedeeld}): {@code depositFlag} hears the §6.5 deposit where a
     *  filled dividend meets a leeg divisor; a numeric zero divisor faults. */
    public static Object divided(Object left, Object right, Object depositFlag) {
        if (left != null && right == null) DepositFlag.report(depositFlag);
        return RegelSpraakRuntime.rsGedeeld(left, right);
    }

    /** As {@link #divided}, truncated toward zero at 5 decimals ({@code rsGedeeldAbs}). */
    public static Object dividedAbs(Object left, Object right, Object depositFlag) {
        if (left != null && right == null) DepositFlag.report(depositFlag);
        return RegelSpraakRuntime.rsGedeeldAbs(left, right);
    }

    /** A square root under its rounding (§13.4.17.13): the runtime's root at 34 digits ({@code rsWortel}), which the
     *  rounding above it collapses; a leeg radicand leeg, and a negative one leeg with the §6.6 deposit to
     *  {@code depositFlag} (RUNTIME-SEMANTICS ch. 1.2). */
    public static Object squareRoot(Object radicand, Object depositFlag) {
        BigDecimal number = RegelSpraakRuntime.toBd(radicand);
        if (number != null && number.signum() < 0) DepositFlag.report(depositFlag);
        return RegelSpraakRuntime.rsWortel(radicand);
    }

    /** A square root that is not the base of a rounding, exact as the reference interpreter computes it: the root of
     *  a radicand that is the square of a rational, and otherwise the fault that executor gives, since an irrational
     *  root exists only under the rounding §13.4.17.13 mandates (RUNTIME-SEMANTICS ch. 2.2). A leeg radicand is leeg,
     *  a negative one leeg with the §6.6 deposit to {@code depositFlag} (ch. 1.2). */
    public static Object exactSquareRoot(Object radicand, Object depositFlag) {
        Breuk q = Breuk.van(radicand);
        if (q == null) return null;
        if (q.signum() < 0) {
            DepositFlag.report(depositFlag);
            return null;
        }
        BigInteger counter = q.teller().sqrt(), denominator = q.noemer().sqrt();
        if (!counter.multiply(counter).equals(q.teller()) || !denominator.multiply(denominator).equals(q.noemer()))
            // the reference interpreter writes the radicand as a rational here (Q.to_string), not as a decimal
            throw new Rekenfout("irrational square root of " + (q.noemer().equals(BigInteger.ONE) ? q.teller()
                    : q.teller() + "/" + q.noemer()) + " reached without an explicit rounding (§13.4.17.13 mandates"
                    + " one)");
        Breuk r = Breuk.van(counter, denominator);
        return r.isTerminerend() ? r.naarDecimal() : r;
    }

    // ---------------- units ----------------

    /** {@code v} restated by the exact canonical factors {@code teller ÷ noemer} as rationals, so a restatement that
     *  does not terminate stays exact until a collapse point (RUNTIME-SEMANTICS ch. 2.1, 2.6); a leeg value and a value
     *  that is no number pass unchanged. */
    public static Object rescaleExact(Object v, BigDecimal counter, BigDecimal denominator) {
        return v instanceof BigDecimal || v instanceof Breuk
                ? RegelSpraakRuntime.rsGedeeld(RegelSpraakRuntime.rsMaal(v, counter), denominator) : v;
    }

    /** {@code v} restated by the single ratio {@code factor} of two units, a rate whose factors are not both known; a
     *  leeg value and a value that is no number pass unchanged. */
    public static Object rescale(Object v, BigDecimal factor) {
        return v instanceof BigDecimal || v instanceof Breuk ? RegelSpraakRuntime.rsMaal(v, factor) : v;
    }

    // ---------------- dates ----------------

    /** A date or a date-time plus or minus {@code aantal} in the calendar or clock unit {@code unit}
     *  ({@code rsDatumReken}); a step or a count outside the range of RUNTIME-SEMANTICS ch. 3.8 is an evaluation fault
     *  ({@link #dateFault}). */
    public static Object dateStep(Object date, Object aantal, String unit, boolean optellen) {
        java.time.temporal.Temporal d = (java.time.temporal.Temporal) date;
        try {
            return RegelSpraakRuntime.rsDatumReken(d, aantal, unit, optellen);
        } catch (IllegalStateException e) {
            throw dateFault(e);
        }
    }

    /** A date plus or minus a count of days of the day kind {@code datePredicate} (RUNTIME-SEMANTICS ch. 3.8,
     *  {@code rsDatumPlusDagsoort}); a walk past the reach of ch. 3.8 is an evaluation fault ({@link #dateFault}). */
    public static Object dayKindStep(Object date, Object aantal, Predicate<LocalDate> datePredicate, boolean optellen) {
        try {
            return RegelSpraakRuntime.rsDatumPlusDagsoort((LocalDate) date, aantal, datePredicate, optellen);
        } catch (IllegalStateException e) {
            throw dateFault(e);
        }
    }

    /** As {@link #dayKindStep}, on a date-time over its date part, the clock time riding through
     *  ({@code rsDatumTijdPlusDagsoort}). */
    public static Object dateTimeDayKindStep(Object date, Object aantal, Predicate<LocalDate> datePredicate,
                                               boolean optellen) {
        try {
            return RegelSpraakRuntime.rsDatumTijdPlusDagsoort((LocalDateTime) date, aantal, datePredicate, optellen);
        } catch (IllegalStateException e) {
            throw dateFault(e);
        }
    }

    /** The fault of a date step the runtime raised as {@code e}, as an evaluation fault in its own sentence, which is
     *  the reference interpreter's: a step that leaves the representable range of a datum-tijd, a duration count outside
     *  that range (RUNTIME-SEMANTICS ch. 3.8), a step on a date beyond the years the runtime's dates hold, and a walk over
     *  the days of a day kind that passes the reach of ch. 3.8 without one. Any other {@code IllegalStateException} out
     *  of the date step is no evaluation fault and is thrown as it is. */
    private static RuntimeException dateFault(IllegalStateException e) {
        String sentence = String.valueOf(e.getMessage());
        boolean range = sentence.equals(RegelSpraakRuntime.DATUMTIJD_BUITEN_BEREIK)
                || sentence.equals(RegelSpraakRuntime.DATUM_BUITEN_JAREN)
                || (sentence.startsWith("duration count ") && sentence.endsWith(" is out of range"))
                || sentence.startsWith(DAY_KIND_WALK);
        return range ? new Rekenfout(sentence, e) : e;
    }

    /** The opening of the runtime's fault of a day-kind walk past its reach ({@code rsDatumPlusDagsoort}), which is the
     *  reference interpreter's sentence word for word, the reach and the date after it. */
    private static final String DAY_KIND_WALK = "day-kind walk: no day of the kind within "
            + RegelSpraakRuntime.DAGSOORT_REIKWIJDTE + " calendar days of ";

    /** easterSunday of a whole year without a unit (RUNTIME-SEMANTICS ch. 3.8, WF-038), by the algorithm of that chapter
     *  ({@code rsEerstePaasdag}); a leeg year leeg. A year that is not whole is a fault in the reference interpreter's
     *  sentence, never a silent truncation, and so is a year outside the years this backend's dates hold. */
    public static Object easterSunday(Object year) {
        if (year == null) return null;
        Breuk q = Breuk.van(year);
        if (!q.noemer().equals(BigInteger.ONE))
            throw new Rekenfout("easterSunday: expected a whole unitless year, got " + numberText(q));
        if (q.teller().abs().compareTo(BigInteger.valueOf(999_999_999)) > 0)
            throw new Rekenfout("easterSunday: the year " + q.teller() + " lies outside the years -999999999 to"
                    + " 999999999 that this backend's dates hold");
        return RegelSpraakRuntime.rsEerstePaasdag(new BigDecimal(q.teller()));
    }

    /** A date of three parts ({@code rsDatumMet}), {@code spelling} the builtin's name for the sentence of a fault: any
     *  leeg part leeg (RUNTIME-SEMANTICS ch. 1.2); a part that is not a whole number within the range of a date part, or
     *  parts that name no day, fault, since the reference interpreter makes no such date, and the runtime would
     *  truncate the part or throw past the fire. */
    public static Object dateWith(String spelling, Object year, Object month, Object day) {
        return dateConstruction(spelling, false, year, month, day);
    }

    /** As {@link #dateWith}, a date-time of seven parts ({@code rsDatumtijdMet}). */
    public static Object dateTimeWith(String spelling, Object year, Object month, Object day, Object hour, Object minute,
                                      Object second, Object millisecond) {
        return dateConstruction(spelling, true, year, month, day, hour, minute, second, millisecond);
    }

    private static Object dateConstruction(String spelling, boolean time, Object... v) {
        for (Object part : v) if (part == null) return null;
        for (Object part : v) {
            Breuk q = Breuk.van(part);
            if (!q.noemer().equals(BigInteger.ONE) || q.teller().bitLength() > 31)
                throw new Rekenfout(spelling + ": the part " + numberText(q) + " is no whole number within the range"
                        + " of a date part");
        }
        try {
            return time ? RegelSpraakRuntime.rsDatumtijdMet(v[0], v[1], v[2], v[3], v[4], v[5], v[6])
                    : RegelSpraakRuntime.rsDatumMet(v[0], v[1], v[2]);
        } catch (java.time.DateTimeException e) {
            throw new Rekenfout(spelling + ": " + e.getMessage(), e);
        }
    }

    /** A number as the reference interpreter writes it in a sentence: a terminating one as its plain decimal, another as
     *  numerator/denominator. */
    private static String numberText(Breuk q) {
        return q.isTerminerend() ? q.naarDecimal().toPlainString() : q.teller() + "/" + q.noemer();
    }

    // ---------------- dimensions ----------------

    /** The cell at {@code label} of a dimensioned attribute's {@code Map<label, value>}, leeg where the map or the label
     *  is absent (§3.6). */
    public static Object cell(Object map, String label) {
        return map instanceof Map<?, ?> m ? m.get(label) : null;
    }

    /** The map of a dimensioned attribute as it is, leeg where the value is no map. */
    public static Map<?, ?> map(Object waarde) {
        return waarde instanceof Map<?, ?> m ? m : null;
    }

    /** {@code waarde}, or {@code fallback} where it is leeg: a sum's {@code emptyFallback} (§5.8.2), which is evaluated
     *  whether it is used or not (RUNTIME-SEMANTICS ch. 5.11). */
    public static Object fallback(Object waarde, Object fallback) {
        return waarde == null ? fallback : waarde;
    }

    // ---------------- folds ----------------

    /** A count an accumulate bound, as a number. */
    public static BigDecimal aantal(Object count) {
        return BigDecimal.valueOf(((Number) count).longValue());
    }

    /** The value of a fold from what the accumulate bound (RUNTIME-SEMANTICS ch. 1.2 "Aggregations"): the folded
     *  {@code waarde}, or {@code fallback} where the count {@code aantal} of the contributions is 0. */
    public static Object foldValue(Object aantal, Object waarde, Object fallback) {
        return ((Number) aantal).longValue() == 0 ? fallback : waarde;
    }

    /** One step of a listed fold that keeps the least operand (RUNTIME-SEMANTICS ch. 1.2e): {@code tot} the value so
     *  far, leeg before the first operand, and {@code operand} the next one; a leeg operand is skipped, and a tie takes
     *  the later operand. */
    public static Object smallest(Object to, Object operand) {
        if (operand == null) return to;
        if (to == null) return operand;
        return vergelijk(to, operand) < 0 ? to : operand;
    }

    /** As {@link #smallest}, keeping the greatest operand. */
    public static Object largest(Object to, Object operand) {
        if (operand == null) return to;
        if (to == null) return operand;
        return vergelijk(to, operand) > 0 ? to : operand;
    }

    /** The order of two values a fold compares: numbers exactly, a rational among them included; dates as they order. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int vergelijk(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) return Breuk.van(x).compareTo(Breuk.van(y));
        return ((Comparable) a).compareTo(b);
    }

    // ---------------- conditions ----------------

    /** A date comparison of {@code left} and {@code right}, two dates or two date-times of one precision, a value that
     *  is no date reading as leeg, with its leeg tables: an ordering with a leeg operand is onwaar, equality of two leeg
     *  dates is onwaar (§5.3 Tabel 38), inequality holds where exactly one is leeg (§5.6 Tabel 44). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean dateCompare(Object left, Object right, Conditie.Operator op) {
        Object a = dateValue(left), b = dateValue(right);
        switch (op) {
            case GELIJK: return Objects.equals(a, b) && !(a == null && b == null);
            case ONGELIJK: return !Objects.equals(a, b);
            default: break;
        }
        if (!(a instanceof Comparable) || b == null) return false;
        int c = ((Comparable) a).compareTo(b);
        return switch (op) {
            case EERDER -> c < 0;
            case EERDER_OF_GELIJK -> c <= 0;
            case LATER -> c > 0;
            case LATER_OF_GELIJK -> c >= 0;
            default -> false;
        };
    }

    private static Object dateValue(Object v) {
        return v instanceof java.time.LocalDate || v instanceof java.time.LocalDateTime ? v : null;
    }

    /** The equality of a text, an enumeration label or a truth value with its leeg tables: equal only where both are
     *  filled and the same, unequal where exactly one is leeg or the two differ; {@code equal} false asks unequal. */
    public static boolean textEqual(Object a, Object b, boolean equal) {
        boolean bothEmpty = a == null && b == null;
        boolean same = Objects.equals(a, b);
        return equal ? same && !bothEmpty : !same && !bothEmpty;
    }

    /** The leeg test of {@code waarde}, where {@code leeg}, or else the gevuld test; a read over an absent link is leeg
     *  (RUNTIME-SEMANTICS ch. 5.8 rule 1). */
    public static boolean emptyTest(Object waarde, boolean empty) {
        return (waarde == null) == empty;
    }

    /** The eleven test (§8.1.3, {@code rsElfproef}) of {@code waarde}, a leeg value failing it, whose verdict must equal
     *  {@code desired}: false for the written negation, which complements it. */
    public static boolean elevenTest(Object waarde, boolean desired) {
        return RegelSpraakRuntime.rsElfproef(waarde) == desired;
    }

    /** The exact-digit test (§8.1.4, {@code rsNumeriekMetExact}) of {@code waarde} with {@code digits} digits, whose
     *  verdict must equal {@code desired}. */
    public static boolean exactDigits(Object waarde, int digits, boolean desired) {
        return RegelSpraakRuntime.rsNumeriekMetExact(waarde, digits) == desired;
    }

    /** A characteristic check of the subject (RUNTIME-SEMANTICS ch. 5.8): its truth value {@code waarde}, bound off a
     *  total standing, so never leeg, equal to {@code desired}, false for a negated check. */
    public static boolean kenmerk(Object waarde, boolean desired) {
        return (Boolean) waarde == desired;
    }

    /** A characteristic check of the object a path of roles reaches, as {@link #kenmerk}: a check over an absent link,
     *  whose read is leeg, holds in neither polarity (RUNTIME-SEMANTICS ch. 5.8 rule 3). */
    public static boolean characteristicCheckAlongPath(Object waarde, boolean desired) {
        return waarde != null && (Boolean) waarde == desired;
    }

    /** A role check over {@code aantal}, the number of links that hold the subject in the role (RUNTIME-SEMANTICS
     *  ch. 5.8a): whether there is one, complemented where the check is negated ({@code negated}). */
    public static boolean roleCount(Object aantal, boolean negated) {
        return (((Number) aantal).longValue() > 0) != negated;
    }

    /** A role check over the subject's own reference field, bound as whether it is set ({@code isSet}), complemented
     *  where the check is negated ({@code negated}). */
    public static boolean roleField(Object isSet, boolean negated) {
        return (Boolean) isSet != negated;
    }

    /** The evaluation-date gate of a time window (RUNTIME-SEMANTICS ch. 3.10): whether the evaluation date
     *  {@code datum} lies in the window, from {@code van} inclusive to {@code end}, exclusive or, where
     *  {@code toInclusive}, inclusive. A bound the window does not have ({@code hasFrom}, {@code hasTo}) is open;
     *  an unset evaluation date never matches, nor does a leeg bound; neither is a fault. */
    public static boolean timeWindow(Object van, boolean hasFrom, Object end, boolean hasTo, boolean toInclusive,
                                      Object date) {
        if (!(date instanceof LocalDate day)) return false;
        if (hasFrom && !(van instanceof LocalDate v && !day.isBefore(v))) return false;
        if (hasTo && !(end instanceof LocalDate t && (toInclusive ? !day.isAfter(t) : day.isBefore(t))))
            return false;
        return true;
    }

    /** The fold of the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9) as the runtime folds it
     *  ({@code rsGeheelPeriode}): over {@code timeline}, a list of segments or null, on the grid {@code grid}
     *  ("JAAR" or "MAAND"), with the test of code {@code toets} and the bound {@code grens} of a comparison read as a
     *  decimal, null where the test has none; complemented where the qualifier is negated ({@code niet}). */
    public static boolean wholePeriod(Object timeline, Object bound, String grid, int test, boolean not) {
        List<?> tl = (List<?>) timeline;
        BigDecimal g = RegelSpraakRuntime.toBd(bound);
        return RegelSpraakRuntime.rsGeheelPeriode(tl, grid, test, g) != not;
    }

    /** The stop triggers of a composite and of a quantifier fold (RUNTIME-SEMANTICS ch. 5.10): the verdict once
     *  {@code read} members are read, of which {@code touch} hold and {@code rest} are unread, or null while the
     *  unread members can still change it. */
    public static Boolean established(Conditie.Kwantor kw, int threshold, int touch, int read, int rest) {
        return switch (kw) {
            case ALLE, DE -> touch < read ? Boolean.FALSE : null;
            case GEEN -> touch > 0 ? Boolean.FALSE : null;
            case TEN_MINSTE -> touch >= threshold ? Boolean.TRUE : (touch + rest < threshold ? Boolean.FALSE : null);
            case TEN_HOOGSTE -> touch > threshold ? Boolean.FALSE : (touch + rest <= threshold ? Boolean.TRUE : null);
            case PRECIES -> (touch > threshold || touch + rest < threshold) ? Boolean.FALSE : null;
        };
    }

    /** The verdict of a quantifier over {@code n} members of which {@code touch} hold, every member read
     *  (RUNTIME-SEMANTICS ch. 5.10): all where all hold, none where none does, the counting quantifiers on the
     *  threshold. */
    public static boolean endVerdict(Conditie.Kwantor kw, int threshold, long touch, long n) {
        return switch (kw) {
            case ALLE, DE -> touch == n;
            case GEEN -> touch == 0;
            case TEN_MINSTE -> touch >= threshold;
            case TEN_HOOGSTE -> touch <= threshold;
            case PRECIES -> touch == threshold;
        };
    }

    /** The verdict of a quantifier fold whose matrix can neither fault nor deposit (RUNTIME-SEMANTICS ch. 3.9a): over
     *  {@code aantal} contributors, of which the sum {@code touch} of the verdicts, 1 or 0 each, hold, under {@code kw}
     *  with the threshold {@code drempel}. Over no contributor the accumulate's sum is not read. */
    public static boolean quantifierVerdict(Object aantal, Object touch, Conditie.Kwantor kw, int threshold) {
        long n = ((Number) aantal).longValue();
        return endVerdict(kw, threshold, n == 0 ? 0 : ((BigDecimal) touch).longValueExact(), n);
    }

    /**
     * The matrix of a quantifier fold read as the reference interpreter reads it, over the {@link MemberTest} of each
     * contributor, the list {@code contributions} over the count {@code aantal} (RUNTIME-SEMANTICS ch. 3.9a, 5.10): in the
     * contributors' order, with the stop triggers of ch. 5.10 tested before each contributor, the empty prefix
     * included. A contributor that is reached deposits where its matrix deposited, heard by {@code depositFlag}, the probe of
     * the reading rule or nothing, and faults where its matrix faulted; one that is not reached does neither. Over no
     * contributor the accumulate's list is not read.
     */
    public static boolean quantifierRead(Object aantal, Object contributions, Conditie.Kwantor kw, int threshold,
                                        Object depositFlag) {
        List<?> lijst = ((Number) aantal).longValue() == 0 ? List.of() : (List<?>) contributions;
        List<MemberTest> members = new ArrayList<>(lijst.size());
        for (Object b : lijst) members.add((MemberTest) b);
        members.sort(null);
        int n = members.size(), touch = 0;
        for (int k = 0; k < n; k++) {
            Boolean fixed = established(kw, threshold, touch, k, n - k);
            if (fixed != null) return fixed;
            MemberTest member = members.get(k);
            if (member.deposited()) DepositFlag.report(depositFlag);
            if (member.fout() != null) throw member.fout();
            if (member.holds()) touch++;
        }
        return endVerdict(kw, threshold, touch, n);
    }

    /** A test of the §9.5 channel (RUNTIME-SEMANTICS ch. 5.6): whether {@code inconsistent}, the standing of a form's
     *  channel bound off its {@code ChannelState}, holds. */
    public static boolean channel(Object inconsistent) {
        return Boolean.TRUE.equals(inconsistent);
    }

    /** The negation of {@code waarde}, a condition's truth value: the test of a consistency, which holds where its
     *  assertion does not (IR-SPEC ch. 3.5). */
    public static boolean not(Object waarde) {
        return !(Boolean) waarde;
    }

    /** Whether {@code waarde} is the asOfDate-bucket scalar a case gives for a timeline ({@link BucketScalar},
     *  RUNTIME-SEMANTICS ch. 8.4), equal to {@code desired}: a part of a test a form composes itself. */
    public static boolean emmerscalair(Object waarde, boolean desired) {
        return (waarde instanceof BucketScalar) == desired;
    }

    /** Whether the target of an initialisation is empty for its guard (RUNTIME-SEMANTICS ch. 5.4): {@code emptyWriterCount}, the
     *  number of other writers that wrote leeg on the subject, is not 0, or {@code invoer}, the subject's input on
     *  the target, which the plan reads ({@code plan.Term.Invoer}), is leeg. The read has no effect, so that it is
     *  evaluated also where such a writer settles the guard changes nothing. */
    public static boolean targetEmpty(Object emptyWriterCount, Object invoer) {
        return ((Number) emptyWriterCount).longValue() != 0 || invoer == null;
    }

    /** Whether the value facts in {@code waarden}, a list, hold two different values, leeg among them
     *  ({@link #equalValue}). */
    public static boolean differ(Object waarden) {
        List<?> l = (List<?>) waarden;
        for (Object x : l)
            if (!equalValue(((Value) l.get(0)).waarde(), ((Value) x).waarde())) return true;
        return false;
    }

    /** Whether more than one key of a uniqueness collides with a key, by {@code collisions}, the count of the keys that
     *  collide with it, itself included (RUNTIME-SEMANTICS ch. 5.9). */
    public static boolean duplicate(Object collisions) {
        return ((Number) collisions).longValue() > 1;
    }

    /** Whether the key {@code sleutel} is the same key of one form as the key {@code ander}
     *  ({@link UniquenessKey#collidesWith}). */
    public static boolean collidesWith(Object sleutel, Object earlierBean) {
        return ((UniquenessKey) sleutel).collidesWith((UniquenessKey) earlierBean);
    }

    /** Value equality at the write boundary: numbers by value, so that 5.0 and 5.00 are one value; leeg is null. */
    public static boolean equalValue(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    /** Whether a computed value is leeg: null, or a timeline without a segment, which is the temporal leeg
     *  (RUNTIME-SEMANTICS ch. 3.1, 7.8: an empty timeline is one never written). */
    public static boolean isEmpty(Object v) {
        return v == null || v instanceof List<?> l && l.isEmpty();
    }

    // ---------------- timelines ----------------

    /**
     * The temporal total of the subject's own timeline without a clip (RUNTIME-SEMANTICS ch. 3.6): of the input
     * {@code invoer} of the attribute {@code naam}, which takes the asOfDate-bucket scalar, the session's date being the
     * horizon: {@code rsTijdTotaal} of the one period of the grid {@code granularity} that holds the evaluation date
     * ({@link BucketScalar}), and leeg where the attribute has no input. A full timeline faults, in the reference
     * interpreter's words and naming {@code onderwerp}, and so does a session without an evaluation date, where the
     * total has no bucket to take: the reference interpreter tests the full timeline first, then the date
     * ({@code rekendatum}), then the value.
     */
    public static Object ownTotal(Object invoer, Object onderwerp, Object rekendatum, String naam,
                                     String granularity) {
        if (invoer instanceof BucketScalar e) return RegelSpraakRuntime.rsTijdTotaal(e.timeline(), granularity);
        // a timeline a form writes is refused while the model is built, so a full timeline here is the case's
        if (invoer != null)
            throw new Invoervormfout("the temporal total of \"" + naam + "\" over a case-supplied full timeline"
                    + " is not exercised by any corpus case; the asOfDate-bucket convention carries the total, and ",
                    onderwerp, " supplies a full timeline");
        // the case runner places a bucket scalar only where the case carries a date, so a session without one reaches
        // this with no input at all
        if (rekendatum == null)
            throw new Rekenfout("the temporal total of \"" + naam + "\" needs the evaluation date: the case"
                    + " carries the asOfDate bucket's value");
        return RegelSpraakRuntime.rsTijdTotaal(null, granularity);
    }

    /** {@code timeline}, the timeline of an attribute a form reads whole, or the fault where the case gives that input
     *  as the asOfDate-bucket scalar ({@link BucketScalar}, RUNTIME-SEMANTICS ch. 8.4), in the sentence {@code voor},
     *  the object {@code object}, {@code na}. */
    public static List<?> complete(Object timeline, String before, Object object, String after) {
        if (timeline instanceof BucketScalar) throw new Invoervormfout(before, object, after);
        return (List<?>) timeline;
    }

    /** §6.1.2: a value at rest is a decimal, so an exact rational that survived the computation collapses here. */
    public static Object collapse(Object v) {
        return v instanceof Breuk b ? b.naarDecimal() : v;
    }

    // ---------------- the routes of a test and a consequence (plan.Route) ----------------

    /** A fresh probe of the §6.5 and §6.6 deposit ({@link DepositFlag}), which a test of deposits or a consequence puts in
     *  its slot of the value array before it evaluates. */
    public static Object depositFlag() {
        return new DepositFlag();
    }

    /** A place that deposits ran and reports to {@code depositFlag}, the probe its slot holds, or to nothing where the slot
     *  holds none. */
    public static void report(Object depositFlag) {
        DepositFlag.report(depositFlag);
    }

    /** Whether {@code depositFlag}, a probe or nothing, heard a place. */
    public static boolean reported(Object depositFlag) {
        return depositFlag instanceof DepositFlag s && s.reported();
    }

    /** The evaluation fault of the fixed sentence {@code sentence} ({@link Rekenfout}), named in the rule {@code regel}: the
     *  fault that a consequence inserts where a form faults by a sentence of its own ({@code plan.Consequence.Fout}). */
    public static Rekenfout computeFault(String sentence, String regel) {
        return new Rekenfout(sentence).inRule(regel);
    }

    /** The §9.5 inconsistency of the form {@code vorm} on {@code onderwerp}: an instance of the class the fact model
     *  generates for it ({@link TmsTranspiler#INCONSISTENCY}). */
    public static Object inconsistency(String vorm, Object onderwerp) {
        return RegelSpraakRuntime.newInconsistentie(TmsTranspiler.INCONSISTENCY, vorm, onderwerp);
    }

    /** The input timeline {@code timeline}, a list of segments, as the asOfDate-bucket scalar the case gave
     *  ({@link BucketScalar}, RUNTIME-SEMANTICS ch. 8.4): what the standing of such an input carries. */
    public static BucketScalar bucketScalarOf(Object timeline) {
        return BucketScalar.van((List<?>) timeline);
    }

    /** The fault of the form of an input ({@link Invoervormfout}), the sentence {@code voor}, the object {@code object}
     *  and {@code na}, named in the rule {@code regel}: the fault a consequence inserts where the case gives an input
     *  in a form the rule cannot read ({@code plan.Consequence.Invoervormfout}). A null {@code regel} names no rule yet: the
     *  fault of a member of a fold ({@code plan.Consequence.MemberError}), which the form that reads the fold names where it
     *  meets it. */
    public static Invoervormfout inputFormFault(String before, Object object, String after, String regel) {
        return new Invoervormfout(before, object, after).inRule(regel);
    }

    /** The keys of {@code element} in source {@code bron} of the uniqueness {@code uniciteit}, linked to the parent
     *  {@code scope} (null for a form without a scope), over its key components {@code waarden} in the source's order
     *  ({@link UniquenessKey}): one key; or, where the component at {@code timeline} is a timeline, one key per value
     *  it ever carries, in ascending order and none where it carries none (ever-uniqueness, RUNTIME-SEMANTICS ch. 5.9,
     *  {@code rsOoitWaarden}). {@code timeline} is -1 where no component is on a timeline. */
    public static List<UniquenessKey> uniquenessKeys(String uniqueness, int bron, Object element, Object scope,
                                                             List<?> waarden, int timeline) {
        if (timeline < 0) return List.of(new UniquenessKey(uniqueness, bron, element, scope, new ArrayList<>(waarden)));
        List<UniquenessKey> out = new ArrayList<>();
        for (BigDecimal waarde : new TreeSet<>(RegelSpraakRuntime.rsOoitWaarden(waarden.get(timeline)))) {
            List<Object> sleutel = new ArrayList<>(waarden);
            sleutel.set(timeline, waarde);
            out.add(new UniquenessKey(uniqueness, bron, element, scope, sleutel));
        }
        return out;
    }

    /** The facts the distribution of one allocation inserts for one source {@code bron} (RUNTIME-SEMANTICS ch. 4.2 to
     *  4.4), computed by {@link Allocation#bereken} from the {@code pot} and {@code ontvangers}, the list of the source's
     *  {@link Recipient} facts, in the order its consequence inserts them: the §9.5 inconsistency of the form
     *  {@code vorm} on each recipient a degradation surfaces on; the share of every recipient that receives one, a
     *  {@link Value} of {@code aandeel} written by {@code writer}; and where the allocation writes a rest
     *  ({@code rest} not null) and something is distributed, that rest, a {@link Value} of {@code rest} on the
     *  source. */
    public static List<Object> payout(String vorm, Object bron, ResolvedVerdeling.Methode methode, Object pot,
                                         Object recipients, boolean gewogen, boolean capped, boolean descending,
                                         Integer decimals, ElementSleutel aandeel, ElementSleutel rest, String writer) {
        List<Recipient> lijst = new ArrayList<>();
        for (Object o : (List<?>) recipients) lijst.add((Recipient) o);
        Allocation.Result u = Allocation.bereken(methode, pot, lijst, gewogen, capped, decimals, descending);
        List<Object> out = new ArrayList<>();
        for (Object r : u.depositCount()) out.add(inconsistency(vorm, r));
        for (Map.Entry<Object, BigDecimal> e : u.shares().entrySet())
            out.add(new Value(e.getKey(), aandeel, e.getValue(), writer));
        if (rest != null && u.rest() != null) out.add(new Value(bron, rest, u.rest(), writer));
        return out;
    }

    /** The contribution of the member {@code lid} to the fold {@code fold} on {@code onderwerp} by its value
     *  {@code waarde} ({@link Contribution}, RUNTIME-SEMANTICS ch. 3.4), or null where it contributes nothing: a leeg value is
     *  skipped (ch. 1.2). Where {@code totaal} is not null the value is the member's timeline and contributes its total
     *  over time per that unit (ch. 3.6, the per-element fold), and nothing where the case gives the timeline as the
     *  asOfDate-bucket scalar ({@link BucketScalar}), since the member's {@link MemberError} stands instead. Where
     *  {@code filled}, the contribution counts the member and carries no value; otherwise it carries the value, an exact
     *  rational collapsed to a decimal. */
    public static Contribution contribution(String fold, Object onderwerp, Object member, Object waarde, String totaal,
                                    boolean filled) {
        if (totaal != null && waarde instanceof BucketScalar) return null;
        Object v = totaal == null ? waarde : RegelSpraakRuntime.rsTijdTotaal((List<?>) waarde, totaal);
        return v == null ? null : new Contribution(fold, onderwerp, member, filled ? null : collapse(v));
    }

    /**
     * The place of the member {@code lid} in the order of the fold {@code fold}: the selection {@code selection}, then per
     * to-many step of its route, in order, the least place of the step's member in its list, {@code places}, over
     * {@code counts} {@link ListPlace} facts under the reference field {@code fields}. A step whose member has no
     * place, since the input did not state the order of that list, breaks the contract of the rule that needs the
     * place, for {@code reden}: the exception names the field, as no fault of the fold can be told without it
     * (RUNTIME-SEMANTICS ch. 5.10, 5.11).
     */
    public static List<Integer> foldPlace(Consequence.PlaceReason reden, String fold, Object member, int selection,
                                           List<String> fields, List<?> counts, List<?> places) {
        List<Integer> place = new ArrayList<>(fields.size() + 1);
        place.add(selection);
        for (int i = 0; i < fields.size(); i++) {
            if (((Number) counts.get(i)).longValue() == 0) {
                String veld = fields.get(i);
                throw new IllegalStateException(switch (reden) {
                    case FILTER_ERROR -> "het filter van " + fold + " faalt voor " + FactNames.object(member) + ", maar een"
                            + " lid van zijn route heeft geen Lijstplaats onder het veld '" + veld + "': de invoer stelt de"
                            + " volgorde van die lijst niet, en zonder haar is de eerste fout van de vouw niet te bepalen"
                            + " (RUNTIME-SEMANTICS ch. 5.11)";
                    case MEMBER_ERROR -> "de vouw " + fold + " leest de tijdlijn van " + FactNames.object(member) + ", die de case"
                            + " als emmerscalair geeft, maar een lid van zijn route heeft geen Lijstplaats onder het veld '"
                            + veld + "': de invoer stelt de volgorde van die lijst niet, en zonder haar is niet te bepalen"
                            + " welk lid de vouw noemt (RUNTIME-SEMANTICS ch. 3.6, 5.11)";
                    case QUANTIFIER -> "de kwantorvouw " + fold + " leest de matrix van " + FactNames.object(member) + ", maar"
                            + " dat lid heeft geen Lijstplaats onder het veld '" + veld + "': de invoer stelt de volgorde van"
                            + " die lijst niet, en zonder haar is niet te bepalen welk lid de kortsluiting bereikt"
                            + " (RUNTIME-SEMANTICS ch. 5.10, 5.11)";
                });
            }
            place.add(((Number) places.get(i)).intValue());
        }
        return place;
    }

    // ---------------- the routes of a DRL text (plan.Route) ----------------
    // A DRL text calls them class-qualified: MVEL has no try and no throw, and a function of the text returns a fault as
    // its outcome. The rendering into closures calls those it has a use for, so that both run one implementation.

    /** Whether {@code v}, the outcome of an evaluation, is an evaluation fault this branch turns into a {@link Fout}: a
     *  division by a numeric zero, a {@link Rekenfout} or an {@link Invoervormfout}. Every other exception is a broken
     *  contract and leaves the fire. */
    public static boolean isEvaluationFault(Object v) {
        return v instanceof Throwable t && Rekenfout.isEvaluationFault(t);
    }

    /** {@code e}, caught in an evaluation, where it is an evaluation fault ({@link #isEvaluationFault}); any other
     *  exception passes on as it is. */
    public static RuntimeException caught(RuntimeException e) {
        if (!Rekenfout.isEvaluationFault(e)) throw e;
        return e;
    }

    /** The evaluation fault {@code fout} named after the rule {@code regel} it ran in, as the {@link Fout} of a form
     *  carries it. */
    public static RuntimeException designate(Object fout, String regel) {
        return Rekenfout.designate((RuntimeException) fout, regel);
    }

    /** The evaluation fault of the sentence {@code sentence} ({@link Rekenfout}), thrown wherever the evaluation reaches it:
     *  an {@code Unresolved} node, or the bound day outside a day kind ({@code plan.Term.Fail}). */
    public static Object fail(String sentence) {
        throw new Rekenfout(sentence);
    }

    /** {@code waarde}, and the evaluation fault of the sentence {@code sentence} where it is leeg: the evaluation date in a
     *  session without one ({@code plan.Term.Required}). */
    public static Object required(Object waarde, String sentence) {
        if (waarde == null) throw new Rekenfout(sentence);
        return waarde;
    }

    /** The texts {@code parts}, in order, joined: a text composition or a concatenation ({@code plan.Term.TextComposition}). */
    public static String textComposition(List<?> parts) {
        StringBuilder out = new StringBuilder();
        for (Object part : parts) out.append((String) part);
        return out.toString();
    }

    /** The member {@code property}, a bean property ({@code plan.Binding.Field#property}), of the bean
     *  {@code object}, through the getter the bean generator wrote for it on the object's own class
     *  ({@code Accessors.leesEigenschap}); null where {@code object} is null, a link that is absent. */
    public static Object lees(Object object, String property) {
        return Accessors.leesEigenschap(object, property);
    }

    /** The object the walk over the reference fields {@code properties}, bean properties in order, reaches from
     *  {@code object}; null where a link on it is absent. A read along a path is this walk and never a null-safe
     *  navigation of the DRL dialect, which drops the match where a link is absent instead of reading null. */
    public static Object along(Object object, List<?> properties) {
        return walk(object, properties);
    }

    /** Whether {@code waarde} holds {@code true}: a characteristic read off a bean ({@code Binding.ReadForm.WAAR}). */
    public static boolean holds(Object waarde) {
        return Boolean.TRUE.equals(waarde);
    }

    /** The true periods of a characteristic on a timeline as a bean holds them, a list of {@link Period}; null where it
     *  holds none ({@code Binding.ReadForm.PERIODS}). */
    public static List<Period> periods(Object waarde) {
        return Period.lijst((List<?>) waarde);
    }

    /** Whether a reference field holds {@code ander}, an instance of the class named {@code soort} where that is not
     *  null ({@code Binding.ReadForm.IS_SET}, {@link #isA}). A name stands for one class here: the replay admits only
     *  a bean whose class {@code Build.beanLoader()} loaded ({@code DroolsBackend.voegIn}) and refuses a reference
     *  field that points outside the input ({@code targetOutsideInput}), so within a session two classes of one name do
     *  not meet. */
    public static boolean isSet(Object earlierBean, String soort) {
        return earlierBean != null && (soort == null || isA(earlierBean, soort));
    }

    /** Whether the evaluation date {@code datum} lies in the validity window from {@code vanaf} to {@code tot}, dates in
     *  ISO form, both inclusive and a null bound open; never where the session has no date
     *  ({@code Alpha.Comparator.WITHIN}, {@link Geldigheid#bevat}). */
    public static boolean inside(Object date, String from, String to) {
        return new Geldigheid(from == null ? null : LocalDate.parse(from), to == null ? null : LocalDate.parse(to))
                .bevat((LocalDate) date);
    }

    /** Whether {@code object} is an instance of the class named {@code klasse}: its own class or a class or interface
     *  it extends has that name ({@code Alpha.Comparator.IS_A}). A bean class is named, not loaded: a DRL text cannot
     *  reach the classes of the beans. A name stands for one class here: the replay admits only a bean whose class
     *  {@code Build.beanLoader()} loaded ({@code DroolsBackend.voegIn}) and refuses a reference field that points
     *  outside the input ({@code targetOutsideInput}), so within a session two classes of one name do not meet. */
    public static boolean isA(Object object, String javaClass) {
        return object != null && namedOrInherits(object.getClass(), javaClass);
    }

    /** Whether the own class of {@code object}, exactly, has none of the names {@code classes}
     *  ({@code Alpha.Comparator.CLASS_NOT_IN}). A name stands for one class here: the replay admits only a bean whose
     *  class {@code Build.beanLoader()} loaded ({@code DroolsBackend.voegIn}) and refuses a reference field that points
     *  outside the input ({@code targetOutsideInput}), so within a session two classes of one name do not meet. */
    public static boolean classNotIn(Object object, List<?> classes) {
        return !classes.contains(object.getClass().getName());
    }

    /** Whether the own class of {@code object}, exactly, has the name {@code klasse} ({@code Alpha.Comparator.CLASS_IS}).
     *  A name stands for one class here, as for {@link #classNotIn}. */
    public static boolean classIs(Object object, String javaClass) {
        return object.getClass().getName().equals(javaClass);
    }

    private static boolean namedOrInherits(Class<?> c, String naam) {
        if (c == null) return false;
        if (c.getName().equals(naam)) return true;
        for (Class<?> i : c.getInterfaces()) if (namedOrInherits(i, naam)) return true;
        return namedOrInherits(c.getSuperclass(), naam);
    }

    /** A fresh cell for the binder of one evaluation of a {@code Let} (RUNTIME-SEMANTICS ch. 10.6, clause 1): empty
     *  until the first read of the binder fills it ({@link #fillBinderCell}), so that the value is evaluated by need, at
     *  most once where it answers. */
    public static Object binderCell() {
        return new BinderCell();
    }

    /** Whether the cell {@code cell} holds the value of its binder: a read evaluates the value only where it does not. */
    public static boolean binderCellFilled(Object cell) {
        return ((BinderCell) cell).filled;
    }

    /** Fills the cell {@code cell} with {@code waarde}, the value of its binder, and returns it. */
    public static Object fillBinderCell(Object cell, Object waarde) {
        BinderCell c = (BinderCell) cell;
        c.waarde = waarde;
        c.filled = true;
        return waarde;
    }

    /** The value of the binder in the cell {@code cell}; where it is leeg, the barrier of that cell's {@code Let}, which
     *  ends that {@code Let} with leeg (clause 2). */
    public static Object binderValue(Object cell) {
        Object waarde = ((BinderCell) cell).waarde;
        if (waarde == null) throw new EmptyBarrier(cell);
        return waarde;
    }

    /** The outcome of the evaluation of a {@code Let} with the cell {@code cell} that the barrier {@code b} ended: leeg
     *  where {@code b} is that cell's own; a barrier of an enclosing {@code Let} passes on. */
    public static Object intercepted(EmptyBarrier b, Object cell) {
        if (b.cell() != cell) throw b;
        return null;
    }

    /** The cell of one evaluation of a {@code Let}: made in that evaluation and passed on to its body only, so it is
     *  never shared between sessions or threads. */
    private static final class BinderCell {
        private boolean filled;
        private Object waarde;
    }

    /** The verdict of the matrix of a quantifier fold as a member's contribution: 1 where it holds, 0 where it does not
     *  ({@code Consequence.Input.Verdict}). */
    public static BigDecimal judgement(boolean holds) {
        return holds ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    /** A broken contract of the branch, thrown with the sentence {@code sentence}: what cannot happen where the analysis is
     *  right, such as a condition that faulted in the test and answers in the consequence over the same values. It is
     *  no evaluation fault ({@link #isEvaluationFault}) and leaves the fire. */
    public static Object contractBreach(String sentence) {
        throw new IllegalStateException(sentence);
    }

    /** The fault of two or more writers of the attribute {@code attribuut} of {@code onderwerp} that stand with
     *  different values, over the value facts {@code waarden} ({@code Consequence.Schrijversconflict}, RUNTIME-SEMANTICS
     *  ch. 5.7): the writers in the order of the document's rules {@code documentOrder}, a writer the document has
     *  no rule of after them by name, and the object's own type named through the pairs of {@code typeClasses}, the
     *  simple names of the bean classes, and {@code typen}, the types as declared; a {@link Fout} under the first
     *  writer. */
    public static Fout writersConflict(Object onderwerp, Object waarden, String attribuut, List<?> documentOrder,
                                          List<?> typeClasses, List<?> typen) {
        List<Value> lijst = new ArrayList<>();
        for (Object w : (List<?>) waarden) lijst.add((Value) w);
        Schrijversconflict c = Schrijversconflict.van(onderwerp, attribuut, lijst, documentOrder, typeClasses, typen);
        return new Fout(onderwerp, c.writers().get(0).writer(), c);
    }

    // A list of values in order, as a DRL text builds the list argument of a route: MVEL builds no array, and a route is
    // never overloaded, so the number of values is in the name. Unmodifiable, and it may hold null.

    /** An empty list. */
    public static List<Object> list0() {
        return listOf();
    }

    /** The list of {@code a}. */
    public static List<Object> list1(Object a) {
        return listOf(a);
    }

    /** The list of {@code a} and {@code b}. */
    public static List<Object> list2(Object a, Object b) {
        return listOf(a, b);
    }

    /** The list of {@code a} to {@code c}. */
    public static List<Object> list3(Object a, Object b, Object c) {
        return listOf(a, b, c);
    }

    /** The list of {@code a} to {@code d}. */
    public static List<Object> list4(Object a, Object b, Object c, Object d) {
        return listOf(a, b, c, d);
    }

    /** The list of {@code a} to {@code e}. */
    public static List<Object> list5(Object a, Object b, Object c, Object d, Object e) {
        return listOf(a, b, c, d, e);
    }

    /** The list of {@code a} to {@code f}. */
    public static List<Object> list6(Object a, Object b, Object c, Object d, Object e, Object f) {
        return listOf(a, b, c, d, e, f);
    }

    private static List<Object> listOf(Object... waarden) {
        return Collections.unmodifiableList(Arrays.asList(waarden));
    }

    // ---------------- the constraints of a fold's range (plan.BetaTest) ----------------

    /** Whether the walk over {@code ownChain} from {@code eigen}, a pattern's object, and the walk over
     *  {@code otherChain} from {@code ander}, the object another pattern binds, reach one object, by identity; a
     *  broken link reads as null and meets nothing. The other walk goes first, and the own one only where it reached an
     *  object. */
    public static boolean meeting(Object own, Object earlierBean, List<?> ownChain, List<?> otherChain) {
        Object b = walk(earlierBean, otherChain);
        if (b == null) return false;
        return walk(own, ownChain) == b;
    }

    /** Whether {@code listPlace}, a {@link ListPlace}, is the place of the object the walk over {@code keten} from
     *  {@code jsonNode} reaches: its member is that object, by identity. */
    public static boolean placeOf(Object listPlace, Object jsonNode, List<?> chain) {
        return ((ListPlace) listPlace).member() == walk(jsonNode, chain);
    }

    // ---------------- creations and links (plan.BetaTest, Route.CREATION_DEPTH) ----------------

    /** Whether {@code made}, a {@link CreationStamp}, is of the creation phase numbered {@code phase}. */
    public static boolean inPhase(Object made, Object phase) {
        return ((CreationStamp) made).phase() == (Integer) phase;
    }

    /** Whether {@code relationLink}, a {@link RelationLink}, is of another creation phase than the one numbered {@code phase}. */
    public static boolean notInPhase(Object relationLink, Object phase) {
        return ((RelationLink) relationLink).phase() != (Integer) phase;
    }

    /** Whether the links {@code twee} and {@code een}, each a {@link RelationLink}, hold different objects at the end
     *  {@code einde} names, {@code "eerste"} or {@code "tweede"}, by identity. */
    public static boolean otherFilling(Object two, Object one, String endName) {
        RelationLink x = (RelationLink) two, y = (RelationLink) one;
        return switch (endName) {
            case "eerste" -> x.eerste() != y.eerste();
            case "tweede" -> x.tweede() != y.tweede();
            default -> throw new IllegalArgumentException("een koppeling heeft geen einde '" + endName + "'");
        };
    }

    // ---------------- the reconciliation of a defaults stack (plan.BetaTest) ----------------

    /** Whether the outcome {@code x}, an {@link Outcome}, is of an earlier member chain than the outcome {@code y}. */
    public static boolean earlierMember(Object x, Object y) {
        return ((Outcome) x).chain() < ((Outcome) y).chain();
    }

    /** Whether the outcome {@code x}, an {@link Outcome}, decides otherwise than the outcome {@code y}, as the
     *  reconciliation compares two decisions ({@link Outcome#equal}). */
    public static boolean anotherDecision(Object x, Object y) {
        return !Outcome.equal(((Outcome) x).waarde(), ((Outcome) y).waarde());
    }

    /** The creation depth of the object that the object creation {@code naam} of stratum {@code stratum} makes for a
     *  subject of declared type {@code type}, whose own marks are {@code origin}, the {@link CreationStamp} facts whose object
     *  is the subject: none for an object of the input, one for an object a creation made. It is one more than the
     *  subject's depth in this stratum, where an object of the input and one a lower stratum made stand at 0, since a
     *  lower stratum is complete before this one fires (RUNTIME-SEMANTICS ch. 5.5, 6.3). Past {@code budget} it throws
     *  {@link CreationBudget}. A subject that a higher stratum made cannot occur: a creation reads its subject's
     *  population strictly (ch. 6.1), so it sits at or above every creation that makes its subjects; such a subject, or
     *  an object with two marks, is a fault of this transpiler and throws as one. */
    public static int creationDepth(String naam, int stratum, String type, List<?> origin, int budget) {
        if (origin.size() > 1)
            throw new IllegalStateException("exec-model: objectcreatie \"" + naam + "\" ontmoet een onderwerp met "
                    + origin.size() + " merktekens van een creatie, waar een object één keer gemaakt wordt: " + origin);
        int depth = 1;
        if (!origin.isEmpty()) {
            CreationStamp g = (CreationStamp) origin.get(0);
            if (g.stratum() > stratum)
                throw new IllegalStateException("exec-model: objectcreatie \"" + naam + "\" van stratum " + stratum
                        + " ontmoet een onderwerp dat een creatie van stratum " + g.stratum() + " maakte, terwijl een"
                        + " creatie de populatie van haar onderwerp strikt leest en dus in het stratum van elke maker van"
                        + " haar onderwerpen staat of erboven (RUNTIME-SEMANTICS ch. 6.1, 6.3)");
            if (g.stratum() == stratum) depth = g.depth() + 1;
        }
        if (depth > budget)
            throw new CreationBudget("creation budget exhausted: objectcreatie \"" + naam + "\" van stratum " + stratum
                    + " zou een object maken op aanmaakdiepte " + depth + ", voor een " + type + " op diepte "
                    + (depth - 1) + " — een keten van aanmaken die niet stopt is een fout, nooit een stille afkapping"
                    + " (RUNTIME-SEMANTICS ch. 5.5)");
        return depth;
    }

    /** A new bean of the class named {@code klasse}, by its public constructor without arguments, every member unset:
     *  the construction of an object creation ({@code plan.Consequence.Construction}) in a DRL text, which names a bean class
     *  and cannot load one, since the text does not see the beans. The class is loaded by the class loader of
     *  {@code onderwerp}, the bean of the creation's subject: within a session every bean is of the build's bean loader
     *  ({@code DroolsBackend.voegIn}). A class that does not load, or that has no public constructor without arguments,
     *  fails loudly with its name. */
    public static Object newObject(Object onderwerp, String javaClass) {
        Class<?> k;
        try {
            k = Class.forName(javaClass, true, onderwerp.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("een objectcreatie maakt een " + javaClass + ", maar de beanloader van haar"
                    + " onderwerp laadt die klasse niet", e);
        }
        try {
            return k.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("een objectcreatie maakt een " + javaClass + ", maar die klasse heeft geen"
                    + " publieke constructor zonder argumenten", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("een objectcreatie kon geen " + javaClass + " maken", e);
        }
    }

    /** Sets the member whose bean property is {@code property} on the new bean {@code object} to {@code waarde}, where
     *  that is not null, through the setter that {@code Namen.lidnamen} names ({@link Accessors#eigenschapSetter}): an
     *  attribute initialisation of an object creation ({@code plan.Consequence.Construction.Setting}), set before the bean is
     *  inserted, so no rule ever sees the bean without it; a leeg value leaves the member unset. */
    public static void set(Object object, String property, Object waarde) {
        if (waarde != null) Accessors.eigenschapSetter(object.getClass(), property).accept(object, waarde);
    }

    /** The object a chain of to-one reference fields {@code fields}, bean properties, reaches from {@code start}, each
     *  read as {@link #lees} reads it; a broken link reads as null. */
    private static Object walk(Object start, List<?> fields) {
        Object cur = start;
        for (Object v : fields) {
            if (cur == null) return null;
            cur = Accessors.leesEigenschap(cur, (String) v);
        }
        return cur;
    }
}
