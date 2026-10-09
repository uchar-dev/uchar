package dev.uchar.regelspraak.transpiler.kern;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Expr;

import java.util.function.Predicate;

/**
 * The operations of a condition or a value that can end an evaluation with a fault in this backend's runtime, as both
 * Drools branches ask it where a test would otherwise run in a place the reference interpreter does not reach.
 *
 * <p>Drools tests a constraint on a pattern when a fact of the pattern's type is inserted or updated, whether or not
 * a form reaches that test, and an accumulate on the left-hand side of a rule folds its source whenever a tuple
 * reaches it. The reference interpreter evaluates a fold's subselection only where the evaluation of a form reaches
 * the fold, for the members of that subject's range (RUNTIME-SEMANTICS ch. 3.4a, 3.9a), a composite condition only up
 * to the member after which its verdict stands (ch. 5.10), and the defaults of a group only until one decides
 * (ch. 7.1). A test that cannot fault gives the same answer wherever it runs. A test that can fault, run where the
 * reference interpreter does not evaluate it, ends the session where the reference interpreter answers.
 *
 * <p>The list is closed: it names every operation a condition or value lowers to in {@code RegelSpraakRuntime} and
 * {@code Breuk} that throws on some value of its operands, with what it throws.
 * <ul>
 *   <li>a division, {@code gedeeld door} and {@code gedeeld door (ABS)}: a numeric zero as divisor,
 *       {@code Breuk.DelingDoorNul} (ch. 1.4, point 4);</li>
 *   <li>a power, {@code tot de macht}: a zero base under a negative exponent ({@code BigDecimal.pow}, or a
 *       {@code Breuk} with denominator 0) and a result that is no finite double ({@code rsMachtReeel});</li>
 *   <li>date arithmetic, a date plus or minus a duration: a step over a day kind past the walk's reach
 *       ({@code rsDatumPlusDagsoort}, ch. 3.8) and a date outside the calendar's range ({@code rsDatumReken});</li>
 *   <li>a date built from parts, {@code de datum met}, {@code de datum en tijd met} and {@code de eerste paasdag van}:
 *       parts that name no day, and a year outside the calendar's range ({@code java.time});</li>
 *   <li>the Rekenjaar: a session without an evaluation date ({@code rsRekenjaar}).</li>
 * </ul>
 * Every other operation of the runtime reads a leeg or an out-of-range operand as leeg or as onwaar, and throws on
 * nothing a verifier-clean document carries. An operation that comes to throw on a value belongs on this list.
 */
public final class Faalbaar {
    private Faalbaar() {}

    /** The first operation in {@code c} that can fault, as "<kind> ('<source text>')", or null when it holds none.
     *  {@code isDatum} answers whether an operand is a date, from the declarations (the effective type of ch. 2.10,
     *  never the wire's annotation). Null-safe. */
    public static String plek(Conditie c, Predicate<Expr> isDatum) {
        String[] eerste = { null };
        Conditie.bevatOperand(c, e -> (eerste[0] = soort(e, isDatum)) != null);
        return eerste[0];
    }

    /** As {@link #plek(Conditie, Predicate)}, for a value; the subselection of a fold inside it included. */
    public static String plek(Expr e, Predicate<Expr> isDatum) {
        String[] eerste = { null };
        Expr.bevat(e, x -> (eerste[0] = soort(x, isDatum)) != null);
        return eerste[0];
    }

    private static String soort(Expr e, Predicate<Expr> isDatum) {
        String wat = switch (e) {
            case Expr.Binair b when b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS ->
                    "een deling";
            case Expr.Binair b when b.op() == Expr.BinOp.TOT_DE_MACHT -> "een machtsverheffing";
            case Expr.Binair b when (b.op() == Expr.BinOp.PLUS || b.op() == Expr.BinOp.MIN
                    || b.op() == Expr.BinOp.VERMINDERD_MET) && (isDatum.test(b.links()) || isDatum.test(b.rechts())) ->
                    "een datumrekening";
            case Expr.Functie f when f.kind() == Expr.FunctieKind.DATUM_MET || f.kind() == Expr.FunctieKind.DATUMTIJD_MET
                    || f.kind() == Expr.FunctieKind.EERSTE_PAASDAG -> "een datum uit delen";
            case Expr.Constante k when "Rekenjaar".equals(k.naam()) -> "het Rekenjaar";
            default -> null;
        };
        if (wat == null) return null;
        String snippet = e.bron() == null ? null : e.bron().snippet();
        return snippet == null || snippet.isEmpty() ? wat : wat + " ('" + snippet + "')";
    }
}
