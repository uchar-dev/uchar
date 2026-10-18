package dev.uchar.regelspraak.transpiler.kern;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

import java.util.function.Predicate;

/**
 * The time window's well-formed POSITION and BOUND ({@code spec/IR-SPEC.md} ch. 4.6;
 * {@code spec/RUNTIME-SEMANTICS.md} ch. 3.10), as both Drools branches refuse them.
 *
 * <p>WF-112: a window never sits under GEEN or a counting quantifier — not as a member of a composite at any
 * depth, and not in the filter or toets of a quantifier fold. ALLE and DE carry it. WF-111: each bound is a date
 * literal or the subject's own scalar date attribute.
 *
 * <p>The verifier rejects such a document, but gate 12 and the replay API load {@code ir.json} directly, so a
 * document that never went through the verifier (an older wire, a hand-made one) reaches both branches as it is.
 * Measured 2026-09-23 on such documents: the DRL-text branch lowered a window in a fold under all five
 * quantifiers, and under GEEN in a time-free session — where the window never matches — it fired the rule
 * for every subject, the complement reading nothing specifies. So each branch refuses the position itself,
 * and both give the sentence built here.
 *
 * <p>The walk is the verifier's: every composite and every fold is judged by its OWN quantifier over the
 * conditions beneath it, and a window counts only where it is a condition, never inside an operand (the
 * verifier's position test does not enter expressions either). The walk DOES go on into operands to find
 * further positions: a composite in the die/dat filter of an aggregation is a position of its own. The
 * sentence names the node that is ill-formed, not the one around it, so a GEEN fold nested in an ALLE fold is
 * reported with the inner fold's element type.
 */
public final class WindowPosition {
    private WindowPosition() {}

    /** The refusal for a window whose bound is neither shape WF-111 admits. */
    public static final String BOUND_FORM = "een periodevenster met een grens die geen datumliteral en geen eigen"
            + " scalair datumattribuut van het element is (WF-111)";

    /** The refusal for the first WF-112 position in {@code c}, or null when it has none. Null-safe. */
    public static String refusal(Conditie c) {
        if (c instanceof Conditie.Samengesteld s) {
            if (counts(s.kwantor()) && s.onderdelen().stream().anyMatch(Conditie::bevatPeriode))
                return sentence("een " + s.kwantor() + "-samenstelling", COUNTS);
            for (Conditie part : s.onderdelen()) {
                String w = refusal(part);
                if (w != null) return w;
            }
            return null;
        }
        if (c instanceof Conditie.Kwantorvouw kv) {
            if (counts(kv.kwantor())) {
                boolean inFilter = Conditie.bevatPeriode(kv.filter()), inTest = Conditie.bevatPeriode(kv.toets());
                if (inFilter || inTest) {
                    String site = inFilter && inTest ? "het filter en de toets" : inFilter ? "het filter" : "de toets";
                    return sentence(site + " van een kwantorvouw onder " + kv.kwantor() + " over "
                            + (kv.elementObjecttype() == null ? "<onopgelost elementtype>" : kv.elementObjecttype().naam),
                            inTest ? COUNTS : CHOOSES);
                }
            }
            String w = refusal(kv.filter());
            return w != null ? w : refusal(kv.toets());
        }
        if (c instanceof Conditie.Vergelijking v) {
            String w = inExpression(v.links());
            return w != null ? w : inExpression(v.rechts());
        }
        if (c instanceof Conditie.Predicaat p) return inExpression(p.operand());
        return null;
    }

    /** The refusal for the first WF-112 position in the die/dat filter of an aggregation inside {@code e}, or
     *  null. Null-safe. */
    public static String inExpression(Expr e) {
        String[] eerste = { null };
        Expr.bevat(e, x -> x instanceof Expr.Functie f && (eerste[0] = refusal(f.filter())) != null);
        return eerste[0];
    }

    /** Whether a window in {@code c} — as a condition, through composites and folds, not into operands — has a
     *  bound neither WF-111 shape describes for {@code subject}: the shape both branches lower, shared through
     *  {@link #boundLowerable}. */
    public static boolean uncarriableBound(Conditie c, Objecttype subject, Predicate<Expr.AttribuutRef> navigated) {
        if (c instanceof Conditie.Periode per)
            return (per.vanaf() != null && !boundLowerable(per.vanaf(), subject, navigated))
                    || (per.tot() != null && !boundLowerable(per.tot(), subject, navigated));
        if (c instanceof Conditie.Samengesteld s)
            return s.onderdelen().stream().anyMatch(d -> uncarriableBound(d, subject, navigated));
        return false;
    }

    /** WF-111's two bound shapes, as both branches lower them: a {@code Datum in dagen} literal, or the subject's own
     *  scalar {@code Datum in dagen} attribute — a read without bindings (IR-SPEC ch. 4.1a) of the subject's type,
     *  with no timeline and no dimension. The read's type is its declaration's (IR-SPEC ch. 2.10). A bound date
     *  computed, navigated or held in a parameter is neither. */
    public static boolean boundLowerable(Expr bound, Objecttype subject, Predicate<Expr.AttribuutRef> navigated) {
        if (!(EffectiveUnit.typeWithoutBinder(bound) instanceof Datatype.DatumInDagen)) return false;
        if (bound instanceof Expr.Literal) return true;
        return bound instanceof Expr.AttribuutRef a && !navigated.test(a)
                && a.objecttype() != null && a.objecttype() == subject
                && subject.granulariteitVan(a.attribuut()) == null
                && subject.dimensiesVan(a.attribuut()).isEmpty();
    }

    /** GEEN complements the window's verdict and the three counting quantifiers count it; ALLE and DE only
     *  conjoin it, which is the reading ch. 3.10 gives. */
    private static boolean counts(Conditie.Kwantor k) {
        return k != Conditie.Kwantor.ALLE && k != Conditie.Kwantor.DE;
    }

    private static final String COUNTS = "de kwantor telt of ontkent het vensterverdict";
    private static final String CHOOSES = "het venster kiest de leden waarover de kwantor telt of ontkent";

    private static String sentence(String site, String why) {
        return "periodevenster in " + site + " (WF-112): " + why + ", terwijl het venster de regel poort"
                + " (RUNTIME-SEMANTICS ch. 3.10), en voor die lezing is niets gespecificeerd";
    }
}
