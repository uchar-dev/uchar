package dev.uchar.regelspraak.core;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RationalParser;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granularity;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.Unit;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The precision bound of RUNTIME-SEMANTICS ch. 2.3: the most decimals the value of an expression can carry, read from
 * the declarations and the shape of the expression, never from the types a producer writes on a composite: ALEF's
 * typing, in which a quotient by anything but a literal has no finite bound. The frontend's precision gate (ch. 2.4)
 * and the Drools backend's text of a number (ch. 1.2) read this one Java derivation; the verifier derives the same
 * bound in OCaml (WF-023, {@code precision} in {@code spec/verifier/lib/checks.ml}), so the frontend and the verifier
 * refuse the same writes. The two readers see the document through a {@link Reading}: the frontend through the types
 * its resolver computed, the backend through the types it derives from the declarations (ch. 2.10).
 */
public final class PrecisionBound {

    /** How a reader sees the parts of the document the bound reads beyond the expression itself. */
    public interface Reading {
        /** The datatype of {@code e}: a reference's declaration, a composite's type with its derived unit. */
        Datatype typeOf(Expr e);

        /** Whether the fold {@code f} ranges over a set of objects (a fold over members), not over the timeline of
         *  its subject: the verifier's {@code ranged}, a source role or a source path on the fold. */
        boolean ranged(Expr.FunctionCall f);

        /** The defaults groups of the document, which write attributes as rules do. */
        List<PrioritizedDefaults> defaults();
    }

    /** A number of decimals; {@link #UNBOUNDED} is the absence of a finite bound. */
    public record Precision(int decimals) {
        public static final Precision UNBOUNDED = new Precision(-1);
        public boolean bounded() { return decimals >= 0; }
        Precision widest(Precision o) { return !bounded() || !o.bounded() ? UNBOUNDED : decimals >= o.decimals ? this : o; }
        Precision plus(Precision o) { return !bounded() || !o.bounded() ? UNBOUNDED : new Precision(decimals + o.decimals); }
        /** A value of this many decimals times the exact factor {@code f}: the decimals of {@code f} added, the powers
         *  of ten in its numerator taken off, never below 0 — the rule ALEF applies to a unit conversion and to a
         *  literal operand of maal and gedeeld door ({@code OmrekenFactor.decimalen}). A factor 0 makes every value 0. */
        Precision scaled(Unit.Fraction f) {
            if (!bounded()) return this;
            if (f.num().signum() == 0) return new Precision(0);
            Precision fd = of(f);
            if (!fd.bounded()) return UNBOUNDED;
            BigInteger n = f.num().abs();
            int tens = Math.min(powers(n, 2), powers(n, 5));
            return new Precision(Math.max(0, decimals + fd.decimals - tens));
        }
        /** The decimals of an exact fraction: the larger power of 2 and of 5 in its reduced denominator, and no bound
         *  where another prime divides it. */
        static Precision of(Unit.Fraction f) {
            BigInteger g = f.num().gcd(f.den());
            BigInteger d = f.den().abs().divide(g.signum() == 0 ? BigInteger.ONE : g);
            int twos = powers(d, 2), fives = powers(d, 5);
            BigInteger rest = d.divide(BigInteger.TWO.pow(twos)).divide(BigInteger.valueOf(5).pow(fives));
            return rest.equals(BigInteger.ONE) ? new Precision(Math.max(twos, fives)) : UNBOUNDED;
        }
        private static int powers(BigInteger n, int p) {
            if (n.signum() == 0) return 0;
            BigInteger bp = BigInteger.valueOf(p);
            int k = 0;
            while (n.mod(bp).signum() == 0) { n = n.divide(bp); k++; }
            return k;
        }
    }

    /** A bound and the unit of the value it bounds, null where it has none (a unitless literal adopts the unit of its
     *  sibling). A value is one quantity in every unit, so a bound in any unit is sound once a reader restates it into
     *  the unit it reads the value in. */
    public record Bounded(Precision bound, Unit unit) {
        public Precision in(Unit into) { return bound.scaled(factor(unit, into)); }
    }

    /** The precision of a declaration: 0 for a whole number, the declared decimals, and no bound for the unbounded
     *  form; null for anything that is no number. */
    public static Precision declared(Datatype t) {
        if (!t.isNumberLike()) return null;
        if (t instanceof Datatype.Numeric n ? n.wholeNumber() : t instanceof Datatype.Percentage p && p.wholeNumber())
            return new Precision(0);
        if (t.unbounded()) return Precision.UNBOUNDED;
        return new Precision(t instanceof Datatype.Numeric n ? n.decimals() : ((Datatype.Percentage) t).decimals());
    }

    /** Whether a timeline on the grid {@code operand} fits a grid {@code target}: every wall of the operand's grid is a
     *  wall of the target's (WF-126). */
    public static boolean fits(Granularity operand, Granularity target) {
        if (target == Granularity.DAG || operand == target) return true;
        int o = months(operand), t = months(target);
        return o > 0 && t > 0 && o % t == 0;
    }

    private static int months(Granularity g) {
        return switch (g) { case MAAND -> 1; case KWARTAAL -> 3; case JAAR -> 12; default -> 0; };
    }

    private final RegelSpraakModel model;
    private final Reading reading;

    public PrecisionBound(RegelSpraakModel model, Reading reading) {
        this.model = model;
        this.reading = reading;
    }

    /** The bound of {@code e} (RUNTIME-SEMANTICS ch. 2.3), or null where it makes no claim: a value that is no number,
     *  or a node nothing declares. {@code binders} holds the bound of each enclosing {@code Let}'s binder, by its
     *  normalised name. */
    public Bounded of(Expr e, Map<String, Bounded> binders) {
        if (e == null) return null;
        return switch (e) {
            case Expr.Literal l -> {
                Unit.Fraction v = literalValue(l);
                yield v == null ? null : new Bounded(Precision.of(v), unitOf(l));
            }
            case Expr.AttributeReference a -> declaredBound(reading.typeOf(a));
            case Expr.DimensionCellRead c -> declaredBound(reading.typeOf(c));
            case Expr.ParameterRef p -> declaredBound(reading.typeOf(p));
            case Expr.Constant c -> declaredBound(reading.typeOf(c));
            case Expr.BinderRef b -> b.name() == null ? null : binders.get(norm(b.name()));
            case Expr.Let l -> {
                Map<String, Bounded> inner = new HashMap<>(binders);
                if (l.binder() != null) inner.put(norm(l.binder()), of(l.value(), binders));
                yield of(l.body(), inner);
            }
            case Expr.IfThenElse k -> combined(Arrays.asList(of(k.then(), binders), of(k.otherwise(), binders)));
            // the value is rounded in the unit of its base
            case Expr.Rounding r -> new Bounded(new Precision(r.decimals()), unitOf(r.base()));
            case Expr.Clamp c -> {
                List<Bounded> xs = new ArrayList<>();
                xs.add(of(c.base(), binders));
                if (c.minimum() != null) xs.add(of(c.minimum(), binders));
                if (c.maximum() != null) xs.add(of(c.maximum(), binders));
                yield combined(xs);
            }
            case Expr.Binary b -> binary(b, binders);
            case Expr.FunctionCall f -> function(f, binders);
            case Expr.TextComposition t -> null;
            case Expr.InstanceSetUnion u -> null;
            case Expr.VariableRef v -> null;
            case Expr.Unresolved o -> null;
            // a constructor value is no number, and a payload is bounded as its declaration
            case Expr.Construct c -> null;
            case Expr.PayloadAccess p -> declaredBound(reading.typeOf(p));
        };
    }

    private static Bounded declaredBound(Datatype t) {
        Precision p = t == null ? null : declared(t);
        return p == null ? null : new Bounded(p, t.unitOf());
    }

    private Unit unitOf(Expr e) {
        if (e == null) return null;
        Datatype t = reading.typeOf(e);
        return t == null ? null : t.unitOf();
    }

    /** The exact factor from {@code from} to {@code to}; 1 where either has no unit, and where the two do not
     *  inter-convert, which is the unit check's refusal. */
    private static Unit.Fraction factor(Unit from, Unit to) {
        return from != null && to != null && from.convertibleTo(to) ? from.exactRestateFactors(to) : Unit.Fraction.ONE;
    }

    private static boolean narrower(Precision a, Precision b) {
        return a.bounded() && (!b.bounded() || a.decimals() < b.decimals());
    }

    /** The bound of operands the value is one of or adds (a sum, a difference, a clamp, a minimum, a maximum, a
     *  branch): every operand restated into one unit, the unit of one of them that gives the smallest bound. ALEF
     *  restates both sides of a sum into their common unit, so "2 jaar plus b maand", with "jaar = 12 maand", is bound
     *  in whole months, where restated into the left unit (ch. 2.6, rule 4) b/12 has no bound. Null where an operand
     *  makes no claim. */
    private static Bounded combined(List<Bounded> xs) {
        if (xs.isEmpty() || xs.contains(null)) return null;
        List<Unit> candidates = new ArrayList<>();
        for (Bounded x : xs) if (x.unit() != null) candidates.add(x.unit());
        if (candidates.isEmpty()) candidates.add(null);
        Bounded best = null;
        for (Unit c : candidates) {
            Precision p = new Precision(0);
            for (Bounded x : xs) p = p.widest(x.in(c));
            if (best == null || narrower(p, best.bound())) best = new Bounded(p, c);
        }
        return best;
    }

    /** The bound of {@code x} in the unit {@code x} carries. */
    private Precision own(Expr x, Map<String, Bounded> binders) {
        Bounded b = of(x, binders);
        return b == null ? null : b.in(unitOf(x));
    }

    private static Bounded bounded(Precision p, Unit unit) { return p == null ? null : new Bounded(p, unit); }

    private Bounded binary(Expr.Binary b, Map<String, Bounded> binders) {
        Unit.Fraction l = literal(b.left()), r = literal(b.right());
        Unit lu = unitOf(b.left()), ru = unitOf(b.right());
        return switch (b.op()) {
            case PLUS, MIN, VERMINDERD_MET -> combined(Arrays.asList(of(b.left(), binders), of(b.right(), binders)));
            case MAAL -> {
                Unit.Fraction f = Unit.timesFactors(lu, ru);
                Unit product = Unit.times(lu, ru);
                if (l != null && r != null) yield new Bounded(Precision.of(l.times(r).times(f)), product);
                if (l != null) yield bounded(scaled(own(b.right(), binders), l.times(f)), product);
                if (r != null) yield bounded(scaled(own(b.left(), binders), r.times(f)), product);
                Precision a = own(b.left(), binders), c = own(b.right(), binders);
                yield a == null || c == null ? null : new Bounded(a.plus(c).scaled(f), product);
            }
            // a quotient by anything but a literal has no finite bound; by a literal it is a product with its reciprocal
            case GEDEELD_DOOR -> {
                Unit quotient = Unit.divide(lu, ru);
                if (r == null || r.num().signum() == 0) yield new Bounded(Precision.UNBOUNDED, quotient);
                Unit.Fraction f = Unit.divideFactors(lu, ru);
                if (l != null) yield new Bounded(Precision.of(l.times(r.inverted()).times(f)), quotient);
                yield bounded(scaled(own(b.left(), binders), r.inverted().times(f)), quotient);
            }
            case GEDEELD_DOOR_ABS -> new Bounded(new Precision(5), unitOf(b));
            case TOT_DE_MACHT -> new Bounded(Precision.UNBOUNDED, unitOf(b));
            // l/100 × r, in the unit of r: the hundred adds two decimals to the left operand
            case PERCENTAGE_VAN -> {
                Unit.Fraction hundredth = new Unit.Fraction(BigInteger.ONE, BigInteger.valueOf(100));
                if (l != null && r != null) yield new Bounded(Precision.of(l.times(hundredth).times(r)), ru);
                if (l != null) yield bounded(scaled(own(b.right(), binders), l.times(hundredth)), ru);
                Precision a = own(b.left(), binders);
                if (r != null) yield a == null ? null : new Bounded(a.plus(new Precision(2)).scaled(r), ru);
                Precision c = own(b.right(), binders);
                yield a == null || c == null ? null : new Bounded(a.plus(new Precision(2)).plus(c), ru);
            }
        };
    }

    private Bounded function(Expr.FunctionCall f, Map<String, Bounded> binders) {
        List<Expr> args = f.arguments();
        return switch (f.kind()) {
            case AANTAL, AANTAL_GEVULDE, AANTAL_DAGEN_IN, TIJDSDUUR, ABSOLUTE_TIJDSDUUR, JAAR_UIT, MAAND_UIT, DAG_UIT ->
                    new Bounded(new Precision(0), null);
            case SOM, MINIMUM, MAXIMUM, EERSTE, LAATSTE, CONCATENATIE -> operands(f, binders);
            case TOTAAL -> {
                if (setRead(f) || args.size() != 1) yield operands(f, binders);
                // the temporal total adds the periods of the grid a segment holds (ch. 3.6); a rate whose period is
                // not the grid's own unit is restated into it (ch. 2.6, rule 8)
                Expr a = args.get(0);
                // a clip that is a marker cuts where nothing says: no claim, as the verifier makes none
                if (f.filter() instanceof Condition.Unresolved && !reading.ranged(f)) yield null;
                Precision operand = own(a, binders);
                if (operand != null && (calendarUnbounded(a) || partialPeriod(f, a))) operand = Precision.UNBOUNDED;
                Bounded total = bounded(scaled(operand, gridFactor(a)), unitOf(f));
                yield f.emptyFallback() == null ? total
                        : combined(Arrays.asList(total, of(f.emptyFallback(), binders)));
            }
            case WORTEL, TIJDSEVENREDIG_DEEL -> new Bounded(Precision.UNBOUNDED, unitOf(f));
            case ABSOLUTE_WAARDE -> args.size() == 1 ? of(args.get(0), binders) : null;
            case EERSTE_PAASDAG, DATUM_MET, DATUMTIJD_MET -> null;
        };
    }

    /** Whether the total {@code f} folds the values of a set of objects: one read over members of an attribute without
     *  a timeline. */
    private boolean setRead(Expr.FunctionCall f) {
        List<Expr> args = f.arguments();
        return reading.ranged(f) && args.size() == 1 && args.get(0) instanceof Expr.AttributeReference a
                && !(a.objectType() != null && a.objectType().granularityOf(a.attribute()) != null);
    }

    /** A fold over its operands, its empty fallback among them. */
    private Bounded operands(Expr.FunctionCall f, Map<String, Bounded> binders) {
        if (f.arguments().isEmpty()) return null;
        List<Bounded> xs = new ArrayList<>();
        for (Expr x : f.arguments()) xs.add(of(x, binders));
        if (f.emptyFallback() != null) xs.add(of(f.emptyFallback(), binders));
        return combined(xs);
    }

    /** The factor a temporal total of {@code operand} restates its value by: 1 where the grid is the unit's own period,
     *  the grid over that period where the period is another unit of the grid's class. */
    private Unit.Fraction gridFactor(Expr operand) {
        Unit u = unitOf(operand);
        if (u == null || !(operand instanceof Expr.AttributeReference a) || a.objectType() == null) return Unit.Fraction.ONE;
        Granularity g = a.objectType().granularityOf(a.attribute());
        if (g == null) return Unit.Fraction.ONE;
        Unit grid = TimeCalculation.gridUnit(g, model);
        if (grid == null) return Unit.Fraction.ONE;
        for (Unit.Factor f : u.negativeFactors())
            if (atom(f).equals(grid)) return Unit.Fraction.ONE;
        for (Unit.Factor f : u.negativeFactors())
            if (f.quantityClass().equals(grid.factors().get(0).quantityClass())) return grid.exactRestateFactors(atom(f));
        return Unit.Fraction.ONE;
    }

    /** Whether a temporal total of {@code operand} integrates by the calendar where no finite bound is left
     *  (RUNTIME-SEMANTICS ch. 2.3, ch. 3.6): its rate has no negative factor of the grid's class, and the first negative
     *  factor the calendar table puts in a family is of the other family than the grid. A day weighs 1/len of its month,
     *  quarter or year, and a week 1/7 of the days a month or a year holds, so only a day over a grid of the month family
     *  keeps the bound of its operand (whole days). The rule of the verifier ({@code calendar_denominator} and the
     *  calendar clause of the temporal total in its precision bound, {@code spec/verifier/lib/checks.ml}). */
    private boolean calendarUnbounded(Expr operand) {
        Unit.Factor f = calendarFactor(operand);
        return f != null && !TimeCalculation.isDay(f.abbreviation());
    }

    /** The negative factor of the rate of {@code operand} that the calendar integrates over its grid (ch. 3.6): none
     *  where a negative factor is of the grid's class, else the first one the calendar table puts in the other family
     *  than the grid; null where the calendar does not integrate. */
    private Unit.Factor calendarFactor(Expr operand) {
        Unit u = unitOf(operand);
        if (u == null || !(operand instanceof Expr.AttributeReference a) || a.objectType() == null) return null;
        Granularity g = a.objectType().granularityOf(a.attribute());
        if (g == null) return null;
        Unit grid = TimeCalculation.gridUnit(g, model);
        if (grid != null)
            for (Unit.Factor f : u.negativeFactors())
                if (f.quantityClass().equals(grid.factors().get(0).quantityClass())) return null;
        var gridFamily = TimeCalculation.calendarFamily(g);
        for (Unit.Factor f : u.negativeFactors()) {
            var family = TimeCalculation.calendarFamily(f.abbreviation());
            if (family != null && family != gridFamily) return f;
        }
        return null;
    }

    /** Whether the temporal total {@code total} of {@code operand} can count a part of a period of its grid, which it
     *  counts by its days (RUNTIME-SEMANTICS ch. 2.3, ch. 3.6): a share no number of decimals bounds. The calendar
     *  measures in days or in the calendar unit and is {@link #calendarUnbounded}'s. A timeline the case gives lies on
     *  the walls of its grid; a rule cuts the timeline it writes where its condition and its operands cut it, and a clip
     *  cuts at the days its condition holds on, a characteristic holding on periods of days whatever its granularity.
     *  So only the day grid, a total of a timeline no rule writes without a clip, and a clip by a comparison of an own
     *  timeline no rule writes whose calendar grid fits the calendar grid of the total keep whole periods; a writer or a
     *  clip that would cut on walls in some other way is not looked for. The filter of the per-element fold selects
     *  members and cuts nothing. The writers are read from the model: a gate that judges a rule before a later rule
     *  has registered is judged again once every writer has (the frontend's {@code RuleChecker}). The rule of the
     *  verifier ({@code total_can_split} in {@code spec/verifier/lib/checks.ml}). */
    private boolean partialPeriod(Expr.FunctionCall total, Expr operand) {
        if (calendarFactor(operand) != null) return false;
        if (!(operand instanceof Expr.AttributeReference a) || a.objectType() == null) return false;
        Granularity g = a.objectType().granularityOf(a.attribute());
        if (g == null) return false;
        if (g == Granularity.DAG && a.objectType().timelineCountOf(a.attribute()) == null) return false;
        if (written(a.objectType(), a.attribute())) return true;
        Condition clip = reading.ranged(total) ? null : total.filter();
        return clip != null && !onWalls(clip, a);
    }

    /** Whether a clip cuts on the walls of the grid of {@code totalled} only: a comparison of an own timeline no rule
     *  writes, both timelines on a calendar grid and the compared one's fitting the totalled one's (WF-126's fit). */
    private boolean onWalls(Condition clip, Expr.AttributeReference totalled) {
        if (!(clip instanceof Condition.Comparison k) || k.period() != null) return false;
        if (!(k.left() instanceof Expr.AttributeReference b) || b.navigationRole() != null || b.objectType() == null)
            return false;
        Granularity gb = calendarGrain(b), ga = calendarGrain(totalled);
        return gb != null && ga != null && !written(b.objectType(), b.attribute()) && fits(gb, ga);
    }

    /** The granularity of the timeline {@code a} reads where it lies on the calendar's own grid of one period: no start
     *  point and no named timeline on the declaring chain; null elsewhere. */
    private static Granularity calendarGrain(Expr.AttributeReference a) {
        ObjectType ot = a.objectType();
        String m = a.attribute();
        if (ot.granularityOf(m) == null || ot.timelineCountOf(m) != null || ot.fixedStartPointOf(m) != null) return null;
        for (var t : ot.chain()) if (t.attributeTimelineName.containsKey(m)) return null;
        return ot.granularityOf(m);
    }

    /** Whether a rule or a defaults group writes the attribute {@code attribute} on a population that shares an object
     *  with {@code ot}: one of the two types is the other, or a type is both. */
    private boolean written(ObjectType ot, String attribute) {
        String name = norm(attribute);
        Predicate<ObjectType> overlaps = t -> t != null
                && (t.isA(ot) || ot.isA(t) || model.objectTypes.values().stream().anyMatch(d -> d.isA(t) && d.isA(ot)));
        for (ResolvedRule r : model.resolvedRules) {
            if (r.kind() == ResolvedRule.Kind.KENMERKTOEKENNING) continue;
            Expr target = r.target() instanceof Expr.DimensionCellRead c ? c.attribute() : r.target();
            if (target instanceof Expr.AttributeReference t && name.equals(norm(t.attribute()))
                    && overlaps.test(t.objectType())) return true;
        }
        for (PrioritizedDefaults d : reading.defaults())
            if (d.attribute() != null && name.equals(norm(d.attribute()))
                    && overlaps.test(model.objectTypes.get(d.subject()))) return true;
        return false;
    }

    /** The declared unit a factor names, with exponent 1. */
    private static Unit atom(Unit.Factor f) {
        return new Unit(f.abbreviation(), f.system(), f.canonical(), f.factorToCanonical());
    }

    private static Precision scaled(Precision p, Unit.Fraction f) { return p == null ? null : p.scaled(f); }

    /** The exact value of a numeric literal operand, null for any other operand. */
    private Unit.Fraction literal(Expr e) {
        return e instanceof Expr.Literal l ? literalValue(l) : null;
    }

    private Unit.Fraction literalValue(Expr.Literal l) {
        Datatype t = reading.typeOf(l);
        if (t == null || !t.isNumberLike()) return null;
        BigDecimal[] r = RationalParser.of(l.valueText());
        if (r == null) return null;
        BigDecimal num = r[0], den = r[1];
        int scale = Math.max(num.scale(), den.scale());
        return new Unit.Fraction(num.movePointRight(scale).toBigIntegerExact(), den.movePointRight(scale).toBigIntegerExact());
    }

    /** The verifier's name identity ({@code norm} in {@code checks.ml}): runs of spaces and tabs collapse to one space
     *  and only A-Z fold to lower case. */
    private static String norm(String s) {
        StringBuilder result = new StringBuilder();
        for (String word : s.split("[ \t]+")) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                result.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
            }
        }
        return result.toString();
    }
}
