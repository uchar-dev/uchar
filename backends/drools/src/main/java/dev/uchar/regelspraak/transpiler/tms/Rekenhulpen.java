package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.runtime.Accessors;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    // ---------------- conditions ----------------

    /** A date comparison of {@code links} and {@code rechts}, two dates or two date-times of one precision, a value that
     *  is no date reading as leeg, with its leeg tables: an ordering with a leeg operand is onwaar, equality of two leeg
     *  dates is onwaar (§5.3 Tabel 38), inequality holds where exactly one is leeg (§5.6 Tabel 44). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean datumVergelijk(Object links, Object rechts, Conditie.Operator op) {
        Object a = datumWaarde(links), b = datumWaarde(rechts);
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

    private static Object datumWaarde(Object v) {
        return v instanceof java.time.LocalDate || v instanceof java.time.LocalDateTime ? v : null;
    }

    /** The equality of a text, an enumeration label or a truth value with its leeg tables: equal only where both are
     *  filled and the same, unequal where exactly one is leeg or the two differ; {@code gelijk} false asks unequal. */
    public static boolean tekstGelijk(Object a, Object b, boolean gelijk) {
        boolean beideLeeg = a == null && b == null;
        boolean zelfde = Objects.equals(a, b);
        return gelijk ? zelfde && !beideLeeg : !zelfde && !beideLeeg;
    }

    /** The leeg test of {@code waarde}, where {@code leeg}, or else the gevuld test; a read over an absent link is leeg
     *  (RUNTIME-SEMANTICS ch. 5.8 rule 1). */
    public static boolean leegtoets(Object waarde, boolean leeg) {
        return (waarde == null) == leeg;
    }

    /** The eleven test (§8.1.3, {@code rsElfproef}) of {@code waarde}, a leeg value failing it, whose verdict must equal
     *  {@code gewenst}: false for the written negation, which complements it. */
    public static boolean elfproef(Object waarde, boolean gewenst) {
        return RegelSpraakRuntime.rsElfproef(waarde) == gewenst;
    }

    /** The exact-digit test (§8.1.4, {@code rsNumeriekMetExact}) of {@code waarde} with {@code cijfers} digits, whose
     *  verdict must equal {@code gewenst}. */
    public static boolean numeriekMetExact(Object waarde, int cijfers, boolean gewenst) {
        return RegelSpraakRuntime.rsNumeriekMetExact(waarde, cijfers) == gewenst;
    }

    /** A characteristic check of the subject (RUNTIME-SEMANTICS ch. 5.8): its truth value {@code waarde}, bound off a
     *  total standing, so never leeg, equal to {@code gewenst}, false for a negated check. */
    public static boolean kenmerk(Object waarde, boolean gewenst) {
        return (Boolean) waarde == gewenst;
    }

    /** A characteristic check of the object a path of roles reaches, as {@link #kenmerk}: a check over an absent link,
     *  whose read is leeg, holds in neither polarity (RUNTIME-SEMANTICS ch. 5.8 rule 3). */
    public static boolean kenmerkLangsPad(Object waarde, boolean gewenst) {
        return waarde != null && (Boolean) waarde == gewenst;
    }

    /** A role check over {@code aantal}, the number of links that hold the subject in the role (RUNTIME-SEMANTICS
     *  ch. 5.8a): whether there is one, complemented where the check is negated ({@code ontkend}). */
    public static boolean rolaantal(Object aantal, boolean ontkend) {
        return (((Number) aantal).longValue() > 0) != ontkend;
    }

    /** A role check over the subject's own reference field, bound as whether it is set ({@code gezet}), complemented
     *  where the check is negated ({@code ontkend}). */
    public static boolean rolveld(Object gezet, boolean ontkend) {
        return (Boolean) gezet != ontkend;
    }

    /** The evaluation-date gate of a time window (RUNTIME-SEMANTICS ch. 3.10): whether the evaluation date
     *  {@code datum} lies in the window, from {@code van} inclusive to {@code eind}, exclusive or, where
     *  {@code totInclusief}, inclusive. A bound the window does not have ({@code heeftVanaf}, {@code heeftTot}) is open;
     *  an unset evaluation date never matches, nor does a leeg bound; neither is a fault. */
    public static boolean tijdvenster(Object van, boolean heeftVanaf, Object eind, boolean heeftTot, boolean totInclusief,
                                      Object datum) {
        if (!(datum instanceof LocalDate dag)) return false;
        if (heeftVanaf && !(van instanceof LocalDate v && !dag.isBefore(v))) return false;
        if (heeftTot && !(eind instanceof LocalDate t && (totInclusief ? !dag.isAfter(t) : dag.isBefore(t))))
            return false;
        return true;
    }

    /** The fold of the whole-period qualifier (RUNTIME-SEMANTICS ch. 3.9) as the runtime folds it
     *  ({@code rsGeheelPeriode}): over {@code tijdlijn}, a list of segments or null, on the grid {@code raster}
     *  ("JAAR" or "MAAND"), with the test of code {@code toets} and the bound {@code grens} of a comparison read as a
     *  decimal, null where the test has none; complemented where the qualifier is negated ({@code niet}). */
    public static boolean geheelPeriode(Object tijdlijn, Object grens, String raster, int toets, boolean niet) {
        List<?> tl = (List<?>) tijdlijn;
        BigDecimal g = RegelSpraakRuntime.toBd(grens);
        return RegelSpraakRuntime.rsGeheelPeriode(tl, raster, toets, g) != niet;
    }

    /** The stop triggers of a composite and of a quantifier fold (RUNTIME-SEMANTICS ch. 5.10): the verdict once
     *  {@code gelezen} members are read, of which {@code raak} hold and {@code rest} are unread, or null while the
     *  unread members can still change it. */
    public static Boolean vastgesteld(Conditie.Kwantor kw, int drempel, int raak, int gelezen, int rest) {
        return switch (kw) {
            case ALLE, DE -> raak < gelezen ? Boolean.FALSE : null;
            case GEEN -> raak > 0 ? Boolean.FALSE : null;
            case TEN_MINSTE -> raak >= drempel ? Boolean.TRUE : (raak + rest < drempel ? Boolean.FALSE : null);
            case TEN_HOOGSTE -> raak > drempel ? Boolean.FALSE : (raak + rest <= drempel ? Boolean.TRUE : null);
            case PRECIES -> (raak > drempel || raak + rest < drempel) ? Boolean.FALSE : null;
        };
    }

    /** The verdict of a quantifier over {@code n} members of which {@code raak} hold, every member read
     *  (RUNTIME-SEMANTICS ch. 5.10): all where all hold, none where none does, the counting quantifiers on the
     *  threshold. */
    public static boolean eindoordeel(Conditie.Kwantor kw, int drempel, long raak, long n) {
        return switch (kw) {
            case ALLE, DE -> raak == n;
            case GEEN -> raak == 0;
            case TEN_MINSTE -> raak >= drempel;
            case TEN_HOOGSTE -> raak <= drempel;
            case PRECIES -> raak == drempel;
        };
    }

    /** The verdict of a quantifier fold whose matrix can neither fault nor deposit (RUNTIME-SEMANTICS ch. 3.9a): over
     *  {@code aantal} contributors, of which the sum {@code raak} of the verdicts, 1 or 0 each, hold, under {@code kw}
     *  with the threshold {@code drempel}. Over no contributor the accumulate's sum is not read. */
    public static boolean kwantoroordeel(Object aantal, Object raak, Conditie.Kwantor kw, int drempel) {
        long n = ((Number) aantal).longValue();
        return eindoordeel(kw, drempel, n == 0 ? 0 : ((BigDecimal) raak).longValueExact(), n);
    }

    /**
     * The matrix of a quantifier fold read as the reference interpreter reads it, over the {@link Lidtoets} of each
     * contributor, the list {@code bijdragen} over the count {@code aantal} (RUNTIME-SEMANTICS ch. 3.9a, 5.10): in the
     * contributors' order, with the stop triggers of ch. 5.10 tested before each contributor, the empty prefix
     * included. A contributor that is reached deposits where its matrix deposited, heard by {@code sonde}, the probe of
     * the reading rule or nothing, and faults where its matrix faulted; one that is not reached does neither. Over no
     * contributor the accumulate's list is not read.
     */
    public static boolean kwantorlezing(Object aantal, Object bijdragen, Conditie.Kwantor kw, int drempel,
                                        Object sonde) {
        List<?> lijst = ((Number) aantal).longValue() == 0 ? List.of() : (List<?>) bijdragen;
        List<Lidtoets> leden = new ArrayList<>(lijst.size());
        for (Object b : lijst) leden.add((Lidtoets) b);
        leden.sort(null);
        int n = leden.size(), raak = 0;
        for (int k = 0; k < n; k++) {
            Boolean vast = vastgesteld(kw, drempel, raak, k, n - k);
            if (vast != null) return vast;
            Lidtoets lid = leden.get(k);
            if (lid.gedeponeerd()) Sonde.meld(sonde);
            if (lid.fout() != null) throw lid.fout();
            if (lid.waar()) raak++;
        }
        return eindoordeel(kw, drempel, raak, n);
    }

    /** A test of the §9.5 channel (RUNTIME-SEMANTICS ch. 5.6): whether {@code inconsistent}, the standing of a form's
     *  channel bound off its {@code KanaalStand}, holds. */
    public static boolean kanaal(Object inconsistent) {
        return Boolean.TRUE.equals(inconsistent);
    }

    /** The negation of {@code waarde}, a condition's truth value: the test of a consistency, which holds where its
     *  assertion does not (IR-SPEC ch. 3.5). */
    public static boolean niet(Object waarde) {
        return !(Boolean) waarde;
    }

    /** Whether {@code waarde} is the asOfDate-bucket scalar a case gives for a timeline ({@link Emmerscalair},
     *  RUNTIME-SEMANTICS ch. 8.4), equal to {@code gewenst}: a part of a test a form composes itself. */
    public static boolean emmerscalair(Object waarde, boolean gewenst) {
        return (waarde instanceof Emmerscalair) == gewenst;
    }

    /** Whether the target of an initialisation is empty for its guard (RUNTIME-SEMANTICS ch. 5.4): {@code legen}, the
     *  number of other writers that wrote leeg on {@code onderwerp}, is not 0, or the input of member {@code lid} of
     *  {@code onderwerp}'s bean, read through the getter of {@code klasse}, is leeg; the input is read only where no such
     *  writer settles it. */
    public static boolean doelLeeg(Object legen, Object onderwerp, Class<?> klasse, String lid) {
        return ((Number) legen).longValue() != 0 || Accessors.getter(klasse, lid).apply(onderwerp) == null;
    }

    /** Whether the value facts in {@code waarden}, a list, hold two different values, leeg among them
     *  ({@link #gelijkeWaarde}). */
    public static boolean verschillen(Object waarden) {
        List<?> l = (List<?>) waarden;
        for (Object x : l)
            if (!gelijkeWaarde(((Waarde) l.get(0)).waarde(), ((Waarde) x).waarde())) return true;
        return false;
    }

    /** Value equality at the write boundary: numbers by value, so that 5.0 and 5.00 are one value; leeg is null. */
    public static boolean gelijkeWaarde(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }

    /** Whether a computed value is leeg: null, or a timeline without a segment, which is the temporal leeg
     *  (RUNTIME-SEMANTICS ch. 3.1, 7.8: an empty timeline is one never written). */
    public static boolean isLeeg(Object v) {
        return v == null || v instanceof List<?> l && l.isEmpty();
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
