package dev.uchar.regelspraak.transpiler.tms.plan;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Rekenhulpen;
import dev.uchar.regelspraak.transpiler.tms.Tijdvak;

/**
 * A static function a {@link Term} calls, by its class and its name: a helper of the runtime, which the other branch
 * calls as well, or a helper of this branch ({@link Rekenhulpen}) that holds what the value compilers computed around
 * such a call. A rendering into closures calls the function itself; a rendering into DRL text names it in an
 * {@code import function}. Every argument is a value, evaluated before the call in the order of
 * {@link Term.Aanroep#argumenten}; a constant of the analysis (a number of decimals, a rounding mode, a unit of time, a
 * class) is a {@link Term.Const}. {@link #kanFalen} says whether the function can raise an evaluation fault, a division by a
 * numeric zero or a {@code Rekenfout} or an {@code Invoervormfout} of the branch (RUNTIME-SEMANTICS ch. 5.2).
 */
public enum Functie {
    /** A sum, a leeg operand 0 (ch. 1.2), exact where an operand is a rational. */
    RS_PLUS(RegelSpraakRuntime.class, "rsPlus", false),
    /** A difference, a leeg operand 0. */
    RS_MIN(RegelSpraakRuntime.class, "rsMin", false),
    /** "verminderd met": a leeg left operand leeg, a leeg right one 0. */
    RS_VERMINDERD(RegelSpraakRuntime.class, "rsVerminderd", false),
    /** A product, a leeg operand 0. */
    RS_MAAL(RegelSpraakRuntime.class, "rsMaal", false),
    /** A percentage of a value, exact. */
    RS_PERCENTAGE_VAN(RegelSpraakRuntime.class, "rsPercentageVan", false),
    /** A power with a whole exponent of at least 0, exact (ch. 2.2); the exponent an {@code Integer} constant. */
    RS_MACHT(RegelSpraakRuntime.class, "rsMacht", false),
    /** A rounding (ch. 2.5) to a number of decimals, an {@code Integer} constant, by a {@code RoundingMode} constant. */
    RS_AFRONDING(RegelSpraakRuntime.class, "rsAfronding", false),
    /** A clamp (ch. 2.7) of a value by a minimum and a maximum, an absent or leeg bound no bound. */
    RS_BEGRENS(RegelSpraakRuntime.class, "rsBegrens", false),
    /** The absolute value. */
    RS_ABS(RegelSpraakRuntime.class, "rsAbs", false),
    /** The text of a text or an enumeration label, a leeg value the empty text (ch. 1.2). */
    RS_TEKST(RegelSpraakRuntime.class, "rsTekst", false),
    /** A duration in whole units (ch. 3.8) between two dates, the unit a {@code String} constant as
     *  {@code Tijdrekening.chronoUnit} names it, absolute where a {@code Boolean} constant says so. */
    RS_TIJDSDUUR(RegelSpraakRuntime.class, "rsTijdsduur", false),
    /** A duration in whole days of a day kind between two dates (ch. 3.8), the kind a {@link Term.Dagsoort}, absolute
     *  where a {@code Boolean} constant says so. */
    RS_TIJDSDUUR_DAGSOORT(RegelSpraakRuntime.class, "rsTijdsduurDagsoort", false),
    /** A part of a date or a date-time, the part a {@code ChronoField} constant. */
    RS_DATUM_DEEL(RegelSpraakRuntime.class, "rsDatumDeel", false),
    /** The calendar year of a date, as a whole number without a unit. */
    RS_REKENJAAR(RegelSpraakRuntime.class, "rsRekenjaar", false),
    /** The sum of the cells of a dimension map at a {@code List} constant of labels, a leeg cell skipped. */
    RS_DIM_SOM(RegelSpraakRuntime.class, "rsDimSom", false),
    /** The least cell of a dimension map at a list of labels. */
    RS_DIM_MIN(RegelSpraakRuntime.class, "rsDimMin", false),
    /** The greatest cell of a dimension map at a list of labels. */
    RS_DIM_MAX(RegelSpraakRuntime.class, "rsDimMax", false),
    /** The total over time of a timeline on a grid, a {@code String} constant (ch. 3.6). */
    RS_TIJD_TOTAAL(RegelSpraakRuntime.class, "rsTijdTotaal", false),
    /** A timeline clipped to the true periods of a characteristic (ch. 3.6), the segment class a constant. */
    RS_TIJD_KNIP(RegelSpraakRuntime.class, "rsTijdKnip", false),
    /** A timeline clipped to the periods outside the true periods of a characteristic, within its segments. */
    RS_TIJD_KNIP_NIET(RegelSpraakRuntime.class, "rsTijdKnipNiet", false),
    /** The true periods of a comparison of a timeline with a value, the comparison an {@code Integer} code of
     *  {@code Tijdrekening.toets}, the value a {@code BigDecimal}. */
    RS_TIJD_CONDITIE(RegelSpraakRuntime.class, "rsTijdConditie", false),
    /** The pro-rata part of a base timeline per bucket of a grid over the true periods of a characteristic (ch. 3.7). */
    RS_TIJDSEVENREDIG(RegelSpraakRuntime.class, "rsTijdsevenredig", false),
    /** The true days per bucket of a grid on the calendar (ch. 3.8). */
    RS_AANTAL_DAGEN_IN(RegelSpraakRuntime.class, "rsAantalDagenIn", false),
    /** The true days per bucket of a grid from the anchor of a fixed start point, a {@code LocalDate} constant (ch. 3.2). */
    RS_AANTAL_DAGEN_IN_VANAF(RegelSpraakRuntime.class, "rsAantalDagenInVanaf", false),
    /** A timeline as this branch carries it, a list of {@code Tijdvak}, from the segments a runtime helper returns. */
    TIJDVAK_LIJST(Tijdvak.class, "lijst", false),
    /** A division (ch. 1.4, 1.2): a leeg dividend 0, a leeg divisor leeg with the §6.5 deposit to the probe, a numeric
     *  zero the fault; the exact quotient. The function of a {@link Term.Deel}. */
    GEDEELD(Rekenhulpen.class, "gedeeld", true),
    /** As {@link #GEDEELD}, the quotient truncated toward zero at 5 decimals. */
    GEDEELD_ABS(Rekenhulpen.class, "gedeeldAbs", true),
    /** The square root at 34 digits under the rounding §13.4.17.13 mandates, a negative radicand leeg with the §6.6
     *  deposit to the probe. The function of a {@link Term.Wortel}. */
    WORTEL(Rekenhulpen.class, "wortel", false),
    /** The exact square root of a rational square, a negative radicand leeg with the §6.6 deposit to the probe, and
     *  any other radicand the fault of an irrational root without a rounding (ch. 2.2). The function of a
     *  {@link Term.Wortel}. */
    EXACTE_WORTEL(Rekenhulpen.class, "exacteWortel", true),
    /** A date or a date-time plus or minus a count in a calendar or clock unit, a {@code String} constant, adding where
     *  a {@code Boolean} constant says so; a step outside the range of ch. 3.8 the fault. */
    DATUMSTAP(Rekenhulpen.class, "datumstap", true),
    /** A date plus or minus a count of days of a day kind, a {@link Term.Dagsoort}, adding where a {@code Boolean}
     *  constant says so (ch. 3.8); a walk past the reach the fault. */
    DAGSOORTSTAP(Rekenhulpen.class, "dagsoortstap", true),
    /** As {@link #DAGSOORTSTAP} on a date-time, over its date part, the clock time riding through. */
    DATUMTIJD_DAGSOORTSTAP(Rekenhulpen.class, "datumtijdDagsoortstap", true),
    /** easterSunday of a whole year without a unit (ch. 3.8); a year that is not whole, or outside the years a date
     *  holds, the fault. */
    PAASDAG(Rekenhulpen.class, "paasdag", true),
    /** A date of three parts, after the spelling of the builtin, a {@code String} constant for the sentence of a
     *  fault; a part that is not a whole number within a date part's range, or parts that name no day, the fault. */
    DATUM_MET(Rekenhulpen.class, "datumMet", true),
    /** As {@link #DATUM_MET}, a date-time of seven parts. */
    DATUMTIJD_MET(Rekenhulpen.class, "datumtijdMet", true),
    /** The cell at a label, a {@code String} constant, of a dimension map, leeg where the map or the label is absent. */
    CEL(Rekenhulpen.class, "cel", false),
    /** A dimension map as it is, or leeg where the value is no map. */
    KAART(Rekenhulpen.class, "kaart", false),
    /** A value restated by the exact canonical factors of two units (ch. 2.1, 2.6), a {@code BigDecimal} numerator and
     *  denominator, as rationals; a leeg value and a value that is no number pass unchanged. The function of a
     *  {@link Term.Herschaal}. */
    HERSCHAAL_EXACT(Rekenhulpen.class, "herschaalExact", false),
    /** A value restated by the single ratio of two units, a {@code BigDecimal}, a rate whose factors are not both
     *  known; a leeg value and a value that is no number pass unchanged. The function of a {@link Term.Herschaal}. */
    HERSCHAAL(Rekenhulpen.class, "herschaal", false),
    /** One step of a listed fold that keeps the least operand (ch. 1.2e): from the value so far and the next operand,
     *  a leeg operand skipped, a tie taking the later one, compared exactly ({@code Rekenhulpen.vergelijk}). The function
     *  of a {@link Term.GelijsteVouw}. */
    KLEINSTE(Rekenhulpen.class, "kleinste", false),
    /** As {@link #KLEINSTE}, keeping the greatest operand. */
    GROOTSTE(Rekenhulpen.class, "grootste", false),
    /** A value, or the fallback where it is leeg. */
    TERUGVAL(Rekenhulpen.class, "terugval", false),
    /** A count bound off an accumulate, as a number. */
    AANTAL(Rekenhulpen.class, "aantal", false),
    /** The value of a fold from its count, its folded value and its fallback: the fallback over no contribution. */
    VOUWWAARDE(Rekenhulpen.class, "vouwwaarde", false),
    /** The subject's own total over time without a clip (ch. 3.6): from the asOfDate-bucket scalar of the input, the
     *  fault for a full timeline and for a session without an evaluation date. */
    EIGEN_TOTAAL(Rekenhulpen.class, "eigenTotaal", true),
    /** The full timeline a form reads whole, the fault where the case gives the asOfDate-bucket scalar (ch. 8.4), in
     *  a sentence that names the object. */
    VOLLEDIG(Rekenhulpen.class, "volledig", true),
    /** A value at rest: an exact rational as a decimal (§6.1.2), any other value as it is. */
    COLLAPSE(Rekenhulpen.class, "collapse", false),

    // the functions of a condition below give a truth value

    /** A comparison of two numbers with the empty operand of ch. 1.2: an ordering with a leeg operand is onwaar, two
     *  leeg numbers are equal, exactly one leeg is unequal; exact where a value is a rational. The operator a
     *  {@code String} constant as {@code Operatoren.symbool} spells it. */
    RS_AGG_VERGELIJK(RegelSpraakRuntime.class, "rsAggVergelijk", false),
    /** A comparison of two dates or two date-times, a value that is no date reading as leeg, the operator a
     *  {@code Conditie.Operator} constant: an ordering with a leeg operand is onwaar, two leeg dates are not equal
     *  (§5.3 Tabel 38), and they are unequal where exactly one is leeg (§5.6 Tabel 44). */
    DATUM_VERGELIJK(Rekenhulpen.class, "datumVergelijk", false),
    /** The equality of two texts, enumeration labels or truth values with the empty operand of ch. 1.2, a
     *  {@code Boolean} constant asking equal (true) or unequal (false): equal only where both are filled and the same,
     *  unequal where exactly one is leeg or the two differ. */
    TEKST_GELIJK(Rekenhulpen.class, "tekstGelijk", false),
    /** An enumeration label up to its normalisation, a leeg value leeg: both sides of an equality where either is an
     *  enumeration (maintainer decision 2026-09-20). */
    RS_ENUM_NAAM(RegelSpraakRuntime.class, "rsEnumNaam", false),
    /** The leeg test, where a {@code Boolean} constant is true, or the gevuld test, where it is false, of a value
     *  (ch. 5.8 rule 1: a read over an absent link is leeg). */
    LEEGTOETS(Rekenhulpen.class, "leegtoets", false),
    /** The eleven test of a value ({@code rsElfproef}, §8.1.3), whose verdict a {@code Boolean} constant must equal:
     *  false for the written negation. */
    ELFPROEF(Rekenhulpen.class, "elfproef", false),
    /** The exact-digit test of a value ({@code rsNumeriekMetExact}, §8.1.4) with an {@code Integer} constant of
     *  digits, whose verdict a {@code Boolean} constant must equal. */
    NUMERIEK_MET_EXACT(Rekenhulpen.class, "numeriekMetExact", false),
    /** A characteristic check of the subject (ch. 5.8): its truth value off a total standing, which a {@code Boolean}
     *  constant must equal: false for a negated check. */
    KENMERK(Rekenhulpen.class, "kenmerk", false),
    /** A characteristic check of the object a path of roles reaches, as {@link #KENMERK}; over an absent link, whose
     *  read is leeg, it holds in neither polarity (ch. 5.8 rule 3). */
    KENMERK_LANGS_PAD(Rekenhulpen.class, "kenmerkLangsPad", false),
    /** A role check over a count of links (ch. 5.8a): whether there is one, complemented where a {@code Boolean}
     *  constant says the check is negated. */
    ROLAANTAL(Rekenhulpen.class, "rolaantal", false),
    /** A role check over the subject's own reference field, bound as whether it is set, complemented where a
     *  {@code Boolean} constant says the check is negated. */
    ROLVELD(Rekenhulpen.class, "rolveld", false),
    /** The evaluation-date gate of a time window (ch. 3.10) over its bounds, each with a {@code Boolean} constant that
     *  says whether the window has it, a {@code Boolean} constant for an inclusive end, and the evaluation date: an
     *  absent bound is open, a leeg one and an unset date never match. */
    TIJDVENSTER(Rekenhulpen.class, "tijdvenster", false),
    /** The fold of the whole-period qualifier (ch. 3.9, {@code rsGeheelPeriode}) over a timeline, with the bound of a
     *  comparison or none, the grid ("JAAR" or "MAAND") and the test's code as constants, complemented where a
     *  {@code Boolean} constant says the qualifier is negated. */
    GEHEEL_PERIODE(Rekenhulpen.class, "geheelPeriode", false),
    /** The verdict of a quantifier fold whose matrix can neither fault nor deposit (ch. 3.9a), from the count of its
     *  contributors and the sum of their verdicts, 1 or 0 each, under a {@code Conditie.Kwantor} constant with an
     *  {@code Integer} threshold; over no contributor the sum is not read. */
    KWANTOROORDEEL(Rekenhulpen.class, "kwantoroordeel", false),
    /** The verdict of a quantifier fold whose matrix can fault or deposit, read over the outcome per contributor in
     *  the contributors' order and stopped as ch. 5.10 says; a contributor that is reached deposits to the probe and
     *  raises its fault. The function of a {@link Term.Kwantorlezing}. */
    KWANTORLEZING(Rekenhulpen.class, "kwantorlezing", true),
    /** A test of the §9.5 channel of a form (ch. 5.6): whether the standing of its channel, bound off its
     *  {@code KanaalStand}, says it is inconsistent. */
    KANAAL(Rekenhulpen.class, "kanaal", false),
    /** The negation of a condition's truth value: the test of a consistency, which holds where its assertion does not
     *  (IR-SPEC ch. 3.5). */
    NIET(Rekenhulpen.class, "niet", false),
    /** Whether a value is the asOfDate-bucket scalar a case gives for a timeline (ch. 8.4), which a {@code Boolean}
     *  constant must equal: a part of a test a form composes itself. */
    EMMERSCALAIR(Rekenhulpen.class, "emmerscalair", false),
    /** Whether the target of an initialisation is empty for its guard (RUNTIME-SEMANTICS ch. 5.4): another writer wrote
     *  leeg on the subject, by a count of such writers, or the subject's input on the target, which the plan reads off
     *  the bean ({@link Term.Invoer}), is leeg. A part of a test a form composes itself. */
    DOEL_LEEG(Rekenhulpen.class, "doelLeeg", false),
    /** Whether the value facts on one attribute of one object hold two different values, leeg among them, numbers by
     *  value (RUNTIME-SEMANTICS ch. 5.7). A test a form composes itself. */
    VERSCHILLEN(Rekenhulpen.class, "verschillen", false);

    private final Class<?> klasse;
    private final String methode;
    private final boolean kanFalen;

    Functie(Class<?> klasse, String methode, boolean kanFalen) {
        this.klasse = klasse;
        this.methode = methode;
        this.kanFalen = kanFalen;
    }

    /** The class whose public static method this is. */
    public Class<?> klasse() { return klasse; }

    /** The name of the method. */
    public String methode() { return methode; }

    /** Whether a call can raise an evaluation fault. */
    public boolean kanFalen() { return kanFalen; }
}
