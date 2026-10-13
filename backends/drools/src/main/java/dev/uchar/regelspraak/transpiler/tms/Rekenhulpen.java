package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The functions of this branch's values beside the runtime's helpers: what the value compilers computed around a
 * runtime call, as plain functions of the values the call receives, so that a value is one tree of named calls
 * ({@code plan.Term.Aanroep}, {@code plan.Functie}) that the rendering into closures calls and a rendering into DRL text
 * can name in an {@code import function}. A function takes its arguments evaluated; the probe of a deposit is the
 * object its slot of the value array holds, a {@link Sonde} or nothing. A fault is an evaluation fault of the branch in
 * the reference interpreter's sentence ({@link Rekenfout}, {@link Invoervormfout}, a division by a numeric zero), which
 * the consequence or the fault rule of the form inserts as a {@link Fout}.
 */
public final class Rekenhulpen {

    private Rekenhulpen() {}

    // ---------------- division and square root, with the probe of their deposit ----------------

    /** A division (RUNTIME-SEMANTICS ch. 1.2, 1.4, {@code rsGedeeld}): {@code sonde} hears the §6.5 deposit where a
     *  filled dividend meets a leeg divisor; a numeric zero divisor faults. */
    public static Object gedeeld(Object links, Object rechts, Object sonde) {
        if (links != null && rechts == null) Sonde.meld(sonde);
        return RegelSpraakRuntime.rsGedeeld(links, rechts);
    }

    /** As {@link #gedeeld}, truncated toward zero at 5 decimals ({@code rsGedeeldAbs}). */
    public static Object gedeeldAbs(Object links, Object rechts, Object sonde) {
        if (links != null && rechts == null) Sonde.meld(sonde);
        return RegelSpraakRuntime.rsGedeeldAbs(links, rechts);
    }

    /** A square root under its rounding (§13.4.17.13): the runtime's root at 34 digits ({@code rsWortel}), which the
     *  rounding above it collapses; a leeg radicand leeg, and a negative one leeg with the §6.6 deposit to
     *  {@code sonde} (RUNTIME-SEMANTICS ch. 1.2). */
    public static Object wortel(Object radicand, Object sonde) {
        BigDecimal getal = RegelSpraakRuntime.toBd(radicand);
        if (getal != null && getal.signum() < 0) Sonde.meld(sonde);
        return RegelSpraakRuntime.rsWortel(radicand);
    }

    /** A square root that is not the base of a rounding, exact as the reference interpreter computes it: the root of
     *  a radicand that is the square of a rational, and otherwise the fault that executor gives, since an irrational
     *  root exists only under the rounding §13.4.17.13 mandates (RUNTIME-SEMANTICS ch. 2.2). A leeg radicand is leeg,
     *  a negative one leeg with the §6.6 deposit to {@code sonde} (ch. 1.2). */
    public static Object exacteWortel(Object radicand, Object sonde) {
        Breuk q = Breuk.van(radicand);
        if (q == null) return null;
        if (q.signum() < 0) {
            Sonde.meld(sonde);
            return null;
        }
        BigInteger teller = q.teller().sqrt(), noemer = q.noemer().sqrt();
        if (!teller.multiply(teller).equals(q.teller()) || !noemer.multiply(noemer).equals(q.noemer()))
            // the reference interpreter writes the radicand as a rational here (Q.to_string), not as a decimal
            throw new Rekenfout("irrational square root of " + (q.noemer().equals(BigInteger.ONE) ? q.teller()
                    : q.teller() + "/" + q.noemer()) + " reached without an explicit rounding (§13.4.17.13 mandates"
                    + " one)");
        Breuk r = Breuk.van(teller, noemer);
        return r.isTerminerend() ? r.naarDecimal() : r;
    }

    // ---------------- units ----------------

    /** {@code v} restated by the exact canonical factors {@code teller ÷ noemer} as rationals, so a restatement that
     *  does not terminate stays exact until a collapse point (RUNTIME-SEMANTICS ch. 2.1, 2.6); a leeg value and a value
     *  that is no number pass unchanged. */
    public static Object herschaalExact(Object v, BigDecimal teller, BigDecimal noemer) {
        return v instanceof BigDecimal || v instanceof Breuk
                ? RegelSpraakRuntime.rsGedeeld(RegelSpraakRuntime.rsMaal(v, teller), noemer) : v;
    }

    /** {@code v} restated by the single ratio {@code factor} of two units, a rate whose factors are not both known; a
     *  leeg value and a value that is no number pass unchanged. */
    public static Object herschaal(Object v, BigDecimal factor) {
        return v instanceof BigDecimal || v instanceof Breuk ? RegelSpraakRuntime.rsMaal(v, factor) : v;
    }

    // ---------------- dates ----------------

    /** A date or a date-time plus or minus {@code aantal} in the calendar or clock unit {@code eenheid}
     *  ({@code rsDatumReken}); a step or a count outside the range of RUNTIME-SEMANTICS ch. 3.8 is an evaluation fault
     *  ({@link #datumfout}). */
    public static Object datumstap(Object datum, Object aantal, String eenheid, boolean optellen) {
        java.time.temporal.Temporal d = (java.time.temporal.Temporal) datum;
        try {
            return RegelSpraakRuntime.rsDatumReken(d, aantal, eenheid, optellen);
        } catch (IllegalStateException e) {
            throw datumfout(e);
        }
    }

    /** A date plus or minus a count of days of the day kind {@code predikaat} (RUNTIME-SEMANTICS ch. 3.8,
     *  {@code rsDatumPlusDagsoort}); a walk past the reach of ch. 3.8 is an evaluation fault ({@link #datumfout}). */
    public static Object dagsoortstap(Object datum, Object aantal, Predicate<LocalDate> predikaat, boolean optellen) {
        try {
            return RegelSpraakRuntime.rsDatumPlusDagsoort((LocalDate) datum, aantal, predikaat, optellen);
        } catch (IllegalStateException e) {
            throw datumfout(e);
        }
    }

    /** As {@link #dagsoortstap}, on a date-time over its date part, the clock time riding through
     *  ({@code rsDatumTijdPlusDagsoort}). */
    public static Object datumtijdDagsoortstap(Object datum, Object aantal, Predicate<LocalDate> predikaat,
                                               boolean optellen) {
        try {
            return RegelSpraakRuntime.rsDatumTijdPlusDagsoort((LocalDateTime) datum, aantal, predikaat, optellen);
        } catch (IllegalStateException e) {
            throw datumfout(e);
        }
    }

    /** The fault of a date step the runtime raised as {@code e}, as an evaluation fault in its own sentence, which is
     *  the reference interpreter's: a step that leaves the representable range of a datum-tijd, a duration count outside
     *  that range (RUNTIME-SEMANTICS ch. 3.8), a step on a date beyond the years the runtime's dates hold, and a walk over
     *  the days of a day kind that passes the reach of ch. 3.8 without one. Any other {@code IllegalStateException} out
     *  of the date step is no evaluation fault and is thrown as it is. */
    private static RuntimeException datumfout(IllegalStateException e) {
        String zin = String.valueOf(e.getMessage());
        boolean bereik = zin.equals(RegelSpraakRuntime.DATUMTIJD_BUITEN_BEREIK)
                || zin.equals(RegelSpraakRuntime.DATUM_BUITEN_JAREN)
                || (zin.startsWith("duration count ") && zin.endsWith(" is out of range"))
                || zin.startsWith(DAGSOORTWANDELING);
        return bereik ? new Rekenfout(zin, e) : e;
    }

    /** The opening of the runtime's fault of a day-kind walk past its reach ({@code rsDatumPlusDagsoort}), which is the
     *  reference interpreter's sentence word for word, the reach and the date after it. */
    private static final String DAGSOORTWANDELING = "day-kind walk: no day of the kind within "
            + RegelSpraakRuntime.DAGSOORT_REIKWIJDTE + " calendar days of ";

    /** easterSunday of a whole year without a unit (RUNTIME-SEMANTICS ch. 3.8, WF-038), by the algorithm of that chapter
     *  ({@code rsEerstePaasdag}); a leeg year leeg. A year that is not whole is a fault in the reference interpreter's
     *  sentence, never a silent truncation, and so is a year outside the years this backend's dates hold. */
    public static Object paasdag(Object jaar) {
        if (jaar == null) return null;
        Breuk q = Breuk.van(jaar);
        if (!q.noemer().equals(BigInteger.ONE))
            throw new Rekenfout("easterSunday: expected a whole unitless year, got " + getaltekst(q));
        if (q.teller().abs().compareTo(BigInteger.valueOf(999_999_999)) > 0)
            throw new Rekenfout("easterSunday: the year " + q.teller() + " lies outside the years -999999999 to"
                    + " 999999999 that this backend's dates hold");
        return RegelSpraakRuntime.rsEerstePaasdag(new BigDecimal(q.teller()));
    }

    /** A date of three parts ({@code rsDatumMet}), {@code spelling} the builtin's name for the sentence of a fault: any
     *  leeg part leeg (RUNTIME-SEMANTICS ch. 1.2); a part that is not a whole number within the range of a date part, or
     *  parts that name no day, fault, since the reference interpreter makes no such date, and the runtime would
     *  truncate the part or throw past the fire. */
    public static Object datumMet(String spelling, Object jaar, Object maand, Object dag) {
        return datumconstructie(spelling, false, jaar, maand, dag);
    }

    /** As {@link #datumMet}, a date-time of seven parts ({@code rsDatumtijdMet}). */
    public static Object datumtijdMet(String spelling, Object jaar, Object maand, Object dag, Object uur, Object minuut,
                                      Object seconde, Object milliseconde) {
        return datumconstructie(spelling, true, jaar, maand, dag, uur, minuut, seconde, milliseconde);
    }

    private static Object datumconstructie(String spelling, boolean tijd, Object... v) {
        for (Object deel : v) if (deel == null) return null;
        for (Object deel : v) {
            Breuk q = Breuk.van(deel);
            if (!q.noemer().equals(BigInteger.ONE) || q.teller().bitLength() > 31)
                throw new Rekenfout(spelling + ": the part " + getaltekst(q) + " is no whole number within the range"
                        + " of a date part");
        }
        try {
            return tijd ? RegelSpraakRuntime.rsDatumtijdMet(v[0], v[1], v[2], v[3], v[4], v[5], v[6])
                    : RegelSpraakRuntime.rsDatumMet(v[0], v[1], v[2]);
        } catch (java.time.DateTimeException e) {
            throw new Rekenfout(spelling + ": " + e.getMessage(), e);
        }
    }

    /** A number as the reference interpreter writes it in a sentence: a terminating one as its plain decimal, another as
     *  numerator/denominator. */
    private static String getaltekst(Breuk q) {
        return q.isTerminerend() ? q.naarDecimal().toPlainString() : q.teller() + "/" + q.noemer();
    }

    // ---------------- dimensions ----------------

    /** The cell at {@code label} of a dimensioned attribute's {@code Map<label, value>}, leeg where the map or the label
     *  is absent (§3.6). */
    public static Object cel(Object kaart, String label) {
        return kaart instanceof Map<?, ?> m ? m.get(label) : null;
    }

    /** The map of a dimensioned attribute as it is, leeg where the value is no map. */
    public static Map<?, ?> kaart(Object waarde) {
        return waarde instanceof Map<?, ?> m ? m : null;
    }

    /** {@code waarde}, or {@code terugval} where it is leeg: a sum's {@code emptyFallback} (§5.8.2), which is evaluated
     *  whether it is used or not (RUNTIME-SEMANTICS ch. 5.11). */
    public static Object terugval(Object waarde, Object terugval) {
        return waarde == null ? terugval : waarde;
    }

    // ---------------- folds ----------------

    /** A count an accumulate bound, as a number. */
    public static BigDecimal aantal(Object telling) {
        return BigDecimal.valueOf(((Number) telling).longValue());
    }

    /** The value of a fold from what the accumulate bound (RUNTIME-SEMANTICS ch. 1.2 "Aggregations"): the folded
     *  {@code waarde}, or {@code terugval} where the count {@code aantal} of the contributions is 0. */
    public static Object vouwwaarde(Object aantal, Object waarde, Object terugval) {
        return ((Number) aantal).longValue() == 0 ? terugval : waarde;
    }

    /** One step of a listed fold that keeps the least operand (RUNTIME-SEMANTICS ch. 1.2e): {@code tot} the value so
     *  far, leeg before the first operand, and {@code operand} the next one; a leeg operand is skipped, and a tie takes
     *  the later operand. */
    public static Object kleinste(Object tot, Object operand) {
        if (operand == null) return tot;
        if (tot == null) return operand;
        return vergelijk(tot, operand) < 0 ? tot : operand;
    }

    /** As {@link #kleinste}, keeping the greatest operand. */
    public static Object grootste(Object tot, Object operand) {
        if (operand == null) return tot;
        if (tot == null) return operand;
        return vergelijk(tot, operand) > 0 ? tot : operand;
    }

    /** The order of two values a fold compares: numbers exactly, a rational among them included; dates as they order. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int vergelijk(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) return Breuk.van(x).compareTo(Breuk.van(y));
        return ((Comparable) a).compareTo(b);
    }

    // ---------------- timelines ----------------

    /**
     * The temporal total of the subject's own timeline without a clip (RUNTIME-SEMANTICS ch. 3.6): of the input
     * {@code invoer} of the attribute {@code naam}, which takes the asOfDate-bucket scalar, the session's date being the
     * horizon: {@code rsTijdTotaal} of the one period of the grid {@code granulariteit} that holds the evaluation date
     * ({@link Emmerscalair}), and leeg where the attribute has no input. A full timeline faults, in the reference
     * interpreter's words and naming {@code onderwerp}, and so does a session without an evaluation date, where the
     * total has no bucket to take: the reference interpreter tests the full timeline first, then the date
     * ({@code rekendatum}), then the value.
     */
    public static Object eigenTotaal(Object invoer, Object onderwerp, Object rekendatum, String naam,
                                     String granulariteit) {
        if (invoer instanceof Emmerscalair e) return RegelSpraakRuntime.rsTijdTotaal(e.tijdlijn(), granulariteit);
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
        return RegelSpraakRuntime.rsTijdTotaal(null, granulariteit);
    }

    /** {@code tijdlijn}, the timeline of an attribute a form reads whole, or the fault where the case gives that input
     *  as the asOfDate-bucket scalar ({@link Emmerscalair}, RUNTIME-SEMANTICS ch. 8.4), in the sentence {@code voor},
     *  the object {@code object}, {@code na}. */
    public static List<?> volledig(Object tijdlijn, String voor, Object object, String na) {
        if (tijdlijn instanceof Emmerscalair) throw new Invoervormfout(voor, object, na);
        return (List<?>) tijdlijn;
    }

    /** §6.1.2: a value at rest is a decimal, so an exact rational that survived the computation collapses here. */
    public static Object collapse(Object v) {
        return v instanceof Breuk b ? b.naarDecimal() : v;
    }
}
