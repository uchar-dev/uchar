package dev.uchar.regelspraak.kern;

import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granulariteit;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The fixed start point of a timeline — its two lexemes and the grid it lays (IR-SPEC ch. 2.8, WF-115;
 * RUNTIME-SEMANTICS ch. 3.2) — in the one implementation the frontend, the Drools loader and its case runner
 * share. The WF-115 sentences are the verifier's letter for letter, so a document the verifier refuses is refused
 * here in the same words.
 */
public final class VastStartpunt {
    private VastStartpunt() {}

    /** A lexeme read: a date start ({@code yyyy-mm-dd}, {@link #datum} set) or a day-month start ({@code --mm-dd},
     *  {@link #maand} and {@link #dag} set, a day of that month in a leap year). */
    public record Lexeem(LocalDate datum, int maand, int dag) {
        public boolean isDatum() { return datum != null; }
    }

    private static final java.util.regex.Pattern DATUM = java.util.regex.Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    private static final java.util.regex.Pattern DAG_MAAND = java.util.regex.Pattern.compile("--(\\d{2})-(\\d{2})");

    /** The year a day-month start is anchored in: a leap year, so {@code --02-29} names a day. Every year that has
     *  the day lays the same walls, so the choice is invisible in every result. */
    private static final int ANKERJAAR = 2000;

    /** The lexeme {@code s} read, or null when it is neither a calendar day {@code yyyy-mm-dd} nor a day-month
     *  {@code --mm-dd} whose day exists in its month. */
    public static Lexeem lees(String s) {
        if (s == null) return null;
        java.util.regex.Matcher d = DATUM.matcher(s);
        if (d.matches()) {
            try {
                return new Lexeem(LocalDate.of(Integer.parseInt(d.group(1)), Integer.parseInt(d.group(2)),
                        Integer.parseInt(d.group(3))), 0, 0);
            } catch (java.time.DateTimeException geenDag) {
                return null;
            }
        }
        java.util.regex.Matcher dm = DAG_MAAND.matcher(s);
        if (dm.matches()) {
            int maand = Integer.parseInt(dm.group(1)), dag = Integer.parseInt(dm.group(2));
            if (maand >= 1 && maand <= 12 && dag >= 1 && dag <= dagenIn(maand)) return new Lexeem(null, maand, dag);
        }
        return null;
    }

    /** The days of {@code maand} in a leap year. */
    private static int dagenIn(int maand) {
        return LocalDate.of(ANKERJAAR, maand, 1).lengthOfMonth();
    }

    /** The months between two walls of a single-unit month, quarter or year grid; 0 for a day or week grid. */
    public static int stapMaanden(Granulariteit g) {
        return switch (g) {
            case MAAND -> 1;
            case KWARTAAL -> 3;
            case JAAR -> 12;
            case DAG, WEEK -> 0;
        };
    }

    /** The canonical day-month of a single-unit month, quarter or year grid with a wall on day {@code dag} of month
     *  {@code maand}: the earliest month of the year in that month's cycle that has the day itself. Null when no
     *  month of the cycle has it. */
    public static String canoniek(Granulariteit g, int maand, int dag) {
        int stap = stapMaanden(g);
        if (stap == 0 || maand < 1 || maand > 12 || dag < 1) return null;
        for (int m = (maand - 1) % stap + 1; m <= 12; m += stap)
            if (dag <= dagenIn(m)) return String.format("--%02d-%02d", m, dag);
        return null;
    }

    /** WF-115 (b): the refusal of fixed start point {@code s} on a grid of {@code g} and {@code aantal}, in the
     *  verifier's sentence, or null when it fits. A count below 1 is WF-005's, and this answers null there. */
    public static String weigering(Granulariteit g, int aantal, String s) {
        if (aantal < 1) return null;
        String reden;
        Lexeem l = lees(s);
        if (g == Granulariteit.DAG && aantal == 1)
            reden = "a day grid has a wall at every day boundary and takes no start point";
        else if (g == Granulariteit.WEEK || aantal >= 2)
            reden = l != null && l.isDatum() ? null
                    : "a week grid and a grid of several units start on a date, written yyyy-mm-dd, the ISO form of "
                    + "a day of the calendar";
        else if (l == null || l.isDatum())
            reden = "a month, quarter or year grid of one unit starts on a day-month, written --mm-dd, a day of "
                    + "month mm in a leap year";
        else {
            String c = canoniek(g, l.maand(), l.dag());
            reden = "--01-01".equals(c) ? "it lays the calendar's own grid, which is written without a fixedStart"
                    : c.equals(s) ? null
                    : "its canonical spelling is " + c + ", with the earliest month of the year that has a wall on "
                    + "that day";
        }
        return reden == null ? null : "fixed start point '" + s + "' does not fit a "
                + Spelling.GRANULARITY.spelling(g) + " grid of count " + aantal + ": " + reden;
    }

    /** WF-115 (a), in the verifier's sentence. */
    public static String weigeringVariabel(String tijdlijn, String s) {
        return "timeline '" + tijdlijn + "' has a variable start and a fixedStart '" + s + "': its periods run from "
                + "the anchor its start-point determination binds per object, and a timeline has one start";
    }

    /** WF-115 (c), in the verifier's sentence: the facet's fixed start point against the named timeline's. */
    public static String weigeringOvername(String facet, String tijdlijn, String vanTijdlijn) {
        return "named-timeline adoption is partial: fixed start point " + tekst(facet) + " on the facet, "
                + tekst(vanTijdlijn) + " on timeline '" + tijdlijn + "'";
    }

    private static String tekst(String s) {
        return s == null ? "none" : "'" + s + "'";
    }

    /** Whether day {@code d} is a wall of the grid of {@code g}, {@code aantal} units and fixed start point
     *  {@code vast} (null: no fixed start), or null where that grid lays no wall a day can be held against: a grid of
     *  several units without a date start, a week grid of one unit without one, and a fixed start point that is no
     *  lexeme of WF-115 for its grid (RUNTIME-SEMANTICS ch. 3.2). The calendar's month, quarter and year grids are
     *  the grids {@code --01-01} lays; a day grid of one unit has a wall at every day. */
    public static Boolean opWand(Granulariteit g, int aantal, String vast, LocalDate d) {
        if (g == null || aantal < 1 || d == null) return null;
        if (aantal >= 2) {
            Lexeem l = lees(vast);
            if (l == null || !l.isDatum()) return null;
            LocalDate a = l.datum();
            return switch (g) {
                case DAG -> Math.floorMod(ChronoUnit.DAYS.between(a, d), (long) aantal) == 0;
                case WEEK -> Math.floorMod(ChronoUnit.DAYS.between(a, d), 7L * aantal) == 0;
                case MAAND, KWARTAAL, JAAR -> {
                    long stap = (long) stapMaanden(g) * aantal;
                    long k = Math.floorDiv(ChronoUnit.MONTHS.between(a.withDayOfMonth(1), d.withDayOfMonth(1)), stap);
                    yield a.plusMonths(stap * k).equals(d);
                }
            };
        }
        if (g == Granulariteit.DAG) return vast == null ? Boolean.TRUE : null;
        String start = vast != null ? vast : g == Granulariteit.WEEK ? null : "--01-01";
        if (start == null || weigering(g, 1, start) != null && vast != null) return null;
        try {
            return periode(g, start, d)[0].equals(d);
        } catch (IllegalArgumentException geenRaster) {
            return null;
        }
    }

    /** The date a day-month start's walls are computed from: day {@code dag} of month {@code maand} in a leap
     *  year. A date start is its own anchor. */
    public static LocalDate anker(Lexeem l) {
        return l.isDatum() ? l.datum() : LocalDate.of(ANKERJAAR, l.maand(), l.dag());
    }

    /** The period {@code [van, tot)} of the grid laid by fixed start point {@code s} (well-formed, WF-115) that
     *  contains day {@code d} (RUNTIME-SEMANTICS ch. 3.2). Every wall is computed from the anchor, never from the
     *  previous wall, so a month-end anchor clamps the same way in every period. A multi-period grid has a grid and
     *  no operation, so it is refused by the caller before this is asked. */
    public static LocalDate[] periode(Granulariteit g, String s, LocalDate d) {
        Lexeem l = lees(s);
        if (l == null) throw new IllegalArgumentException("fixed start point '" + s + "' is no lexeme of WF-115");
        LocalDate anker = anker(l);
        if (g == Granulariteit.WEEK) {
            long k = Math.floorDiv(ChronoUnit.DAYS.between(anker, d), 7);
            return new LocalDate[]{ anker.plusDays(7 * k), anker.plusDays(7 * (k + 1)) };
        }
        int stap = stapMaanden(g);
        if (stap == 0 || l.isDatum())
            throw new IllegalArgumentException("fixed start point '" + s + "' on a " + g + " grid: no period of "
                    + "one unit is laid from it (WF-115)");
        long k = Math.floorDiv(ChronoUnit.MONTHS.between(anker.withDayOfMonth(1), d.withDayOfMonth(1)), stap);
        LocalDate van = anker.plusMonths(stap * k);
        if (van.isAfter(d)) { k--; van = anker.plusMonths(stap * k); }
        LocalDate tot = anker.plusMonths(stap * (k + 1));
        if (!tot.isAfter(d)) { k++; van = tot; tot = anker.plusMonths(stap * (k + 1)); }
        return new LocalDate[]{ van, tot };
    }
}
