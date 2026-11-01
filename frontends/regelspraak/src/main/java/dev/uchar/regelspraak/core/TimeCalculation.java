package dev.uchar.regelspraak.core;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;

/**
 * THE time-unit mapping both back-ends share (single shared implementation): a declared tijdseenheid → the
 * {@link java.time.temporal.ChronoUnit} name date arithmetic keys on, and the §8.2 toets-codes
 * {@code rsGeheelPeriode} evaluates.
 */
public final class TimeCalculation {
    private TimeCalculation() {}

    /** A duration's declared unit (its result {@link Datatype}'s eenheid, e.g. an abbreviated "jr" or a bare "jaar")
     *  → a ChronoUnit name for {@code rsDatumReken}/{@code rsTijdsduur}. Normalised through the shared
     *  plural→singular reducer so "jaren"/"jaar"/"jr" all land on YEARS. §3.7 a kwartaal is 3 maanden and
     *  java.time has no ChronoUnit.QUARTERS, so it maps to the pseudo-unit "KWARTAAL" the runtime
     *  special-cases as 3×MONTHS. null for a dagsoort or an unrecognised unit — the caller markers/skips. */
    public static String chronoUnit(Datatype t) {
        return chronoUnit(t.unitOf());
    }

    /** As {@link #chronoUnit(Datatype)}, keyed directly on an {@link Unit} ({@code null}-safe). */
    public static String chronoUnit(Unit e) {
        return e == null || !e.isAtom() ? null : spellingChronoUnit(e.abbreviation());
    }

    /** The time unit the grid of {@code g} counts in, as {@link #chronoUnit(Unit)} names it. */
    public static String gridChronoUnit(RegelSpraakModel.Granularity g) {
        return switch (g) {
            case DAG -> "DAYS";
            case WEEK -> "WEEKS";
            case MAAND -> "MONTHS";
            case KWARTAAL -> "KWARTAAL";
            case JAAR -> "YEARS";
        };
    }

    /** Whether a negative factor of {@code u} is the time unit {@code grid} by the calendar table. */
    public static boolean hasNegativeFactorOfGrid(Unit u, String grid) {
        return u.negativeFactors().stream().anyMatch(f -> grid.equals(spellingChronoUnit(f.abbreviation())));
    }

    /** The unit a fold over a timeline of grid {@code g} leaves (RUNTIME-SEMANTICS ch. 2.6, rule 8): the negative factor of
     *  the grid's class rises one power. That is the factor the calendar table spells as the grid's unit, else, where
     *  {@code model} is given, the first of the class the document declares that unit in: over months "€/m/mnd" gives
     *  "€/m" and "€/jr/m" with "jr = 12 mnd" gives "€/m". Without a factor of the class the reading of a unit without a
     *  grid stands ({@link Unit#afterTimeFold}). */
    public static Unit afterTimeFold(Unit u, RegelSpraakModel.Granularity g, RegelSpraakModel model) {
        String grid = gridChronoUnit(g);
        for (Unit.Factor f : u.negativeFactors())
            if (grid.equals(spellingChronoUnit(f.abbreviation()))) return u.risen(f);
        String gridClass = model == null ? null : gridClass(model, grid);
        if (gridClass != null)
            for (Unit.Factor f : u.negativeFactors())
                if (gridClass.equals(f.quantityClass())) return u.risen(f);
        return u.afterTimeFold();
    }

    /** The class of the unit the document declares for the grid unit {@code grid}; null where it declares none or
     *  units of several classes, where no factor can be named as the grid's. */
    private static String gridClass(RegelSpraakModel model, String grid) {
        java.util.Set<String> classes = new java.util.LinkedHashSet<>();
        for (RegelSpraakModel.UnitSystem s : model.unitSystems.values())
            for (Unit u : s.units.values())
                if (grid.equals(chronoUnit(u))) classes.add(u.factors().get(0).quantityClass());
        return classes.size() == 1 ? classes.iterator().next() : null;
    }

    /** Whether {@code spelling} stands in the NORMATIVE calendar table (uchar's RUNTIME-SEMANTICS ch. 3.8,
     *  thirty spellings over nine units). That table is what decides which of the two readings a duration
     *  unit has — "a spelling outside the table is a declared day kind" — so the day-kind registration guard
     *  must compare against exactly it and against no list of its own, or the producer admits a spelling the
     *  IR then refuses (WF-017). Trims and lower-cases nothing: the caller passes the identity-normalised
     *  spelling, as the registry lookup does. */
    public static boolean isCalendarSpelling(String spelling) {
        return spellingChronoUnit(spelling) != null;
    }

    /** The table keyed directly on a SPELLING — the shared body of the two entries above. A null spelling is
     *  no unit at all and answers null, as the interpreter's own table does. The wire's {@code Unit} always
     *  carries its abbreviation, but the record's afkorting is nullable and the plural reducer hands a null
     *  straight back through its default arm, so without this guard the switch below would throw on a record
     *  that holds none instead of answering. */
    private static String spellingChronoUnit(String spelling) {
        if (spelling == null) return null;
        return switch (RegelSpraakModel.normaliseTimeUnit(spelling)) {
            case "jaar", "jr"             -> "YEARS";
            case "maand", "mnd"           -> "MONTHS";
            case "week", "wk"             -> "WEEKS";
            case "dag", "dg"              -> "DAYS";     // §3.7 declares the dag afkorting "dg"
            case "uur", "u"               -> "HOURS";
            case "minuut", "min"          -> "MINUTES";
            case "seconde", "secondes", "sec", "s"     -> "SECONDS";
            case "milliseconde", "millisecondes", "ms" -> "MILLIS";   // §6.10 spells the plural with -s
            case "kwartaal", "kwartalen", "kw" -> "KWARTAAL";         // §3.7 declares the kwartaal afkorting "kw"
            default -> null;   // the -en reducer leaves unchanged; dagsoort / custom -> marker
        };
    }

    /** The §6.10/§6.11 duration unit read as a DAY KIND: the dagsoort {@code e}'s spelling names, or null when
     *  it names none — and null, too, whenever the spelling stands in the calendar table, WHATEVER the day-kind
     *  registry says about it.
     *
     *  <p>That order is the point of this method, and it is normative: RUNTIME-SEMANTICS ch. 3.8 recognises a
     *  duration's unit by spelling through the table and settles the remainder with "a spelling outside the
     *  table is a declared day kind" — outside, so the table goes first. The verifier (WF-035, WF-038), the
     *  reference interpreter and the ASP backend read it that way, and so does this method at the four day-kind
     *  positions of the Drools backend. Asked registry first, on a document that registers a calendar spelling
     *  as a day-kind unit, the executors SPLIT — three read calendar arithmetic where the fourth reads a day-kind
     *  walk. Measured 2026-09-20 on {@code jutterswet-bergingsklok} with its unit spelled {@code wk} and
     *  {@code wk} registered beside it: {@code ref} answered 2026-03-31 / 2026-02-17 / 5 / 5 where the
     *  registry-first reading answered the day-kind 2026-04-15 / 2026-02-01 / 3 / 3. WF-017 refuses such a
     *  document, so the wire does not carry the contradiction; this method is why an executor cannot act on it
     *  either way.
     *
     *  <p>The four callers keep their own fall-through to {@link #chronoUnit}: a spelling that is neither a day
     *  kind nor a table entry is still an honest marker/skip there. */
    public static String dayKindForUnit(RegelSpraakModel model, Unit e) {
        if (e == null || chronoUnit(e) != null) return null;
        return model.dayKindForUnit(e.abbreviation());
    }

    /** §8.4.3: whether a periodevergelijking bound can be lowered — a §13.2.19 Datum-in-dagen LITERAL or the
     *  subject's OWN scalar (non-temporal, non-dimensioned) Datum-in-dagen attribute. Everything else — a
     *  parameter/navigated/computed bound, a Datum-en-tijd (the §4.2 Rekendatum is day-granular) — stays an
     *  honest marker/skip, SHARED by both back-ends so they fire/skip the same shapes. */
    public static boolean periodBoundLowerable(Expr limit, ObjectType subject) {
        if (!(limit.type() instanceof Datatype.DateInDays)) return false;
        if (limit instanceof Expr.Literal) return true;
        return limit instanceof Expr.AttributeReference a && a.navigationRole() == null && !a.set()
                && a.objectType() != null && a.objectType() == subject
                && subject.granularityOf(a.attribute()) == null
                && subject.dimensionsOf(a.attribute()).isEmpty();
    }

    /** The §8.2 {@code rsGeheelPeriode}/{@code rsTijdConditie} toets code for a comparison operator
     *  (0..5 = KLEINER..ONGELIJK), or null for a DATUM ordering (EERDER/LATER) — the runtime Segment
     *  carries a BigDecimal waarde, so a date-valued whole-period comparison is out of scope. */
    public static Integer test(Condition.Operator op) {
        return switch (op) {
            case KLEINER -> 0; case KLEINER_OF_GELIJK -> 1;
            case GROTER  -> 2; case GROTER_OF_GELIJK  -> 3;
            case GELIJK  -> 4; case ONGELIJK          -> 5;
            default -> null;
        };
    }
}
