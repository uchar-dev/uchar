package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The <b>effective unit</b> of an expression: at a REFERENCE or COMPOSITE position, derived from the
 * DECLARATIONS the document carries (an attribute's declared datatype, a parameter's declared datatype)
 * plus the §4 / RUNTIME-SEMANTICS ch. 2.6 derivation rules — <b>not</b> from the node's own wire
 * annotation. The named positions where that annotation IS the source, and is returned unchanged, are
 * listed below; they are exactly ch. 2.10's, and they are a scope boundary rather than an escape hatch.
 *
 * <p><b>Why.</b> An IR node repeats its type on the wire at positions where it is a DERIVATION over its operands
 * (an arithmetic node, a Let, a value aggregation). The reference positions, where the v0 wire repeated a COPY of a
 * declaration (an {@code AttributeReference}, a {@code ParameterRef}, a {@code DimensionCellRead} read, a binder
 * read), carry no type at all (L2 of the v1 wire migration) — theirs is the declaration's ({@link #typeOf}) — and
 * nothing crosses the derived ones mechanically. A consumer that
 * reads the repetition instead of the source therefore answers a different number than {@code ref} and the
 * ASP translator do on the same document — measured before this class existed: a unit lie on a value
 * aggregation's own result type turned 3,75 into 3.750,00 in this backend while ref and clingo stayed put.
 * Both of those consumers derive: {@code ref/lib/world.ml}'s write boundary collapses into the DECLARED
 * representation, and {@code backends/clingo/lib/translate.ml}'s {@code eff_unit} derives the effective
 * unit from declarations and the join rules. This class is that same derivation for the Drools backend,
 * and {@code scripts/check-reftype-derivation.sh} is what holds all three to it.
 *
 * <p><b>The reason-carrying exceptions</b> (RUNTIME-SEMANTICS ch. 2.10, the consumer clause) — the named
 * positions this class returns the annotation at, with the reason each is not a wire read in disguise:
 * <ul>
 *   <li>a {@link Expr.Literal}: its type is the lexeme's own carrier ("3,5 EUR"), a copy of nothing;</li>
 *   <li>a duration function ({@code TIJDSDUUR} / {@code ABSOLUTE_TIJDSDUUR}): the node type's unit is the
 *       OPERATOR's parameter ("de tijdsduur van A tot B in jaren", WF-038), not a derived result;</li>
 *   <li>ch. 2.10's FOURTH class, which says something weaker: every POSITION whose result unit v0 does not
 *       DERIVE (a division, a power, a percentage-of, a root, an absolute value, the date/time extractors)
 *       keeps the annotation because there is nothing to put in its place — not because the annotation is the
 *       source. A position, not a node kind: {@code GEDEELD_DOOR} and {@code PLUS} are both {@code Binary}
 *       and only one of them derives, which is why the {@code switch}es below dispatch on the OP and the
 *       KIND, and why the harness's clamp keys on (node, op) / (node, kind) too. The ASP translator answers
 *       {@code None} there and fences; this backend has no such fence and computes with the annotation. Measured 2026-09-04: no corpus annotation at such a position
 *       carries a unit, and {@code check-reftype-derivation.sh} fails on the first one that does.</li>
 * </ul>
 *
 * <p><b>Scope.</b> This derives the UNIT only. The soort and the precision of an annotation are equally
 * repetitions, but this backend reads neither for its §4 restatements: it reads {@code isNumber}/
 * {@code isDate} as ROUTING predicates (a soort lie is a different document, crossed by WF-091; a binder read
 * carries no annotation)
 * and it never reads {@code Datatype.decimals} at all — only {@code Rounding.decimals}, a node field.
 * Grafting the derived unit onto the node's own annotation therefore changes exactly what is read and
 * nothing else.
 */
public final class EffectiveUnit {

    /** Depth ceiling for the derivation walk — a guard against a pathological tree, never a semantic bound
     *  (the deepest corpus value tree is an order of magnitude shallower). At the ceiling the node's own
     *  annotation stands, which is the pre-derivation behaviour. */
    private static final int MAX_DEPTH = 200;

    private static final Map<String, Unit> EMPTY = Map.of();

    /** ch. 4.10: every {@link Expr.Let} the document carries ({@code IrLoader.Loaded.lets}). Only
     *  {@link #buildReads} needs it: the derivation itself meets a Let as an ordinary node in the tree it is
     *  walking, but the binder-read table must START at bindings, and a {@link Expr.BinderRef} asked in isolation
     *  carries no route back up to its own. Empty for a document without a Let. */
    private final Collection<Expr> lets;

    /** The derived unit of every binder READ, by identity — precomputed, because a restatement site deep inside a
     *  Let body asks for a binder's unit without the binding in hand. Built by walking each {@link Expr.Let} with a
     *  proper scope, so shadowing resolves the way WF-064 says (nearest enclosing binder wins). Null until first
     *  use. A recorded NULL value means "bound to a unitless value" — membership, not the value, is what separates
     *  a bound read from an unbound one. */
    private Map<Object, Unit> readUnits;

    public EffectiveUnit(Collection<? extends Expr> lets) {
        this.lets = lets == null ? List.of() : List.copyOf(lets);
    }

    /** The datatype every §4 restatement site must use: the node's datatype ({@link #typeOf}) with its unit
     *  replaced by the DERIVED one. Non-numeric types (and a node whose derivation lands on the annotation anyway)
     *  pass through unchanged, so a verifier-clean document lowers byte-for-byte as before. */
    public Datatype of(Expr e) {
        Datatype t = typeOf(e);
        if (!(t instanceof Datatype.Numeric) && !(t instanceof Datatype.Percentage)) return t;
        Unit derived = unit(e, EMPTY, 0);
        if (Objects.equals(derived, t.unitOf())) return t;
        if (t instanceof Datatype.Numeric n)
            return new Datatype.Numeric(n.wholeNumber(), n.decimals(), n.sign(), derived);
        Datatype.Percentage p = (Datatype.Percentage) t;
        return new Datatype.Percentage(p.wholeNumber(), p.decimals(), derived);
    }

    /** The derived unit itself (null = unitless), for a caller that compares units rather than datatypes. */
    public Unit unitOf(Expr e) {
        Datatype t = of(e);
        return t == null ? null : t.unitOf();
    }

    // --- the datatype -----------------------------------------------------------------------------

    /**
     * IR-SPEC ch. 2.10 / RUNTIME-SEMANTICS ch. 2.10: the DATATYPE of an expression, the one reading of it both
     * transpiler branches use. A reference carries no annotation, so its type is its declaration's: an attribute read and a dimension cell have the attribute's declared datatype, a
     * parameter read the parameter's, a binder read the datatype of the value it is bound to (WF-064: the
     * nearest enclosing Let). A rounding and a clamp compute theirs from their base, as their records do. Every
     * other node carries its own annotation — a literal, a composite, a fold, a Let, a constant, a marker.
     *
     * <p>Where the document gives no type — a read whose name declares no attribute (the count source names a
     * role), an unbound variable or binder, a marker that leaves its type out, a derived position written null —
     * the answer is the model's {@link Datatype.Unknown}, which every reader here already takes as "no claim".
     * It is this backend's in-memory reading of an absent type, never a wire value.
     */
    public Datatype typeOf(Expr e) {
        return switch (e) {
            case null -> null;
            case Expr.BinderRef b -> {
                if (readTypes == null) buildReadTypes();
                Datatype t = readTypes.get(b);
                yield t != null ? t : UNKNOWN;
            }
            case Expr.Rounding a -> rounded(typeOf(a.base()), a.decimals());
            case Expr.Clamp b -> typeOf(b.base());
            default -> typeWithoutBinder(e);
        };
    }

    /** {@link #typeOf} at a position no Let encloses (a day-kind definition): a binder read there has no
     *  binding, and so no type. */
    public static Datatype typeWithoutBinder(Expr e) {
        Datatype t = switch (e) {
            case null -> null;
            case Expr.AttributeReference a -> a.objectType() == null ? null : a.objectType().attribute(a.attribute());
            case Expr.DimensionCellRead c -> typeWithoutBinder(c.attribute());
            case Expr.ParameterRef p -> p.parameter() == null ? null : p.parameter().datatype();
            case Expr.BinderRef ignored -> null;
            case Expr.VariableRef ignored -> null;
            case Expr.Rounding a -> rounded(typeWithoutBinder(a.base()), a.decimals());
            case Expr.Clamp b -> typeWithoutBinder(b.base());
            case Expr.InstanceSetUnion ignored -> null;   // it denotes instances and carries no datatype
            default -> e.type();
        };
        return t != null || e instanceof Expr.InstanceSetUnion || e == null ? t : UNKNOWN;
    }

    private static final Datatype UNKNOWN = new Datatype.Unknown("geen type op de wire");

    /** {@code Expr.Rounding.type()}, over the base's datatype as {@link #typeOf} reads it. */
    private static Datatype rounded(Datatype b, int decimals) {
        if (b instanceof Datatype.Numeric n) return new Datatype.Numeric(decimals == 0, decimals, n.sign(), n.unit());
        if (b instanceof Datatype.Percentage p) return new Datatype.Percentage(decimals == 0, decimals);
        return b;
    }

    /** The datatype of every binder READ, by identity, in the scope WF-064 gives it — built like
     *  {@link #readUnits}. Null until first use. */
    private Map<Object, Datatype> readTypes;

    private void buildReadTypes() {
        readTypes = new IdentityHashMap<>();
        for (Expr l : lets) noteType(l, Map.of(), 0);
    }

    private void noteType(Expr e, Map<String, Datatype> environment, int depth) {
        if (e == null || depth > MAX_DEPTH) return;
        if (e instanceof Expr.BinderRef lz) {
            String k = key(lz.name());
            if (environment.containsKey(k)) readTypes.put(e, environment.get(k));
            return;
        }
        if (e instanceof Expr.Let b) {
            noteType(b.value(), environment, depth + 1);
            // WF-076: the value is typed in the ENCLOSING scope, where the binder is not yet visible
            Map<String, Datatype> inside = new HashMap<>(environment);
            inside.put(key(b.binder()), typeIn(b.value(), environment));
            noteType(b.body(), inside, depth + 1);
            return;
        }
        if (e instanceof Expr.IfThenElse k) {
            for (Expr x : conditionOperands(k.condition())) noteType(x, environment, depth + 1);
            noteType(k.then(), environment, depth + 1);
            noteType(k.otherwise(), environment, depth + 1);
            return;
        }
        for (Expr kind : children(e)) noteType(kind, environment, depth + 1);
    }

    /** A bound value's datatype in scope {@code environment}: a binder read there takes its binding's. */
    private Datatype typeIn(Expr e, Map<String, Datatype> environment) {
        return switch (e) {
            case Expr.BinderRef b -> environment.containsKey(key(b.name())) ? environment.get(key(b.name())) : UNKNOWN;
            case Expr.Rounding a -> rounded(typeIn(a.base(), environment), a.decimals());
            case Expr.Clamp b -> typeIn(b.base(), environment);
            default -> typeWithoutBinder(e);
        };
    }

    // --- the derivation -------------------------------------------------------------------------

    private Unit unit(Expr e, Map<String, Unit> environment, int depth) {
        if (e == null) return null;
        Datatype t = typeOf(e);
        Unit annotation = t == null ? null : t.unitOf();
        if (depth > MAX_DEPTH) return annotation;

        if (e instanceof Expr.Let || e instanceof Expr.BinderRef || e instanceof Expr.IfThenElse)
            return letUnit(e, environment, depth, annotation);

        if (e instanceof Expr.AttributeReference a) {
            // the declaration is the source; there is none to consult when the resolver left the objecttype
            // unbound or the type declares no such member — the annotation then stands, and says so.
            if (a.objectType() == null) return annotation;
            Datatype decl = a.objectType().attribute(a.attribute());
            return decl == null ? annotation : decl.unitOf();
        }
        if (e instanceof Expr.DimensionCellRead c) return unit(c.attribute(), environment, depth + 1);
        if (e instanceof Expr.ParameterRef p) {
            Datatype decl = p.parameter() == null ? null : p.parameter().datatype();
            return decl == null ? annotation : decl.unitOf();
        }
        if (e instanceof Expr.Rounding roundingNode) return unit(roundingNode.base(), environment, depth + 1);
        if (e instanceof Expr.Clamp bg) return unit(bg.base(), environment, depth + 1);
        if (e instanceof Expr.Binary b) {
            // §4 / ch. 2.6: an additive result carries the left unit and a unitless side adopts the other.
            // Every other operator (division, power, percentage-of) has no v0 join rule here — annotation.
            return switch (b.op()) {
                case PLUS, MIN, VERMINDERD_MET -> {
                    Unit l = unit(b.left(), environment, depth + 1);
                    yield l != null ? l : unit(b.right(), environment, depth + 1);
                }
                case MAAL -> timesUnit(unit(b.left(), environment, depth + 1),
                                         unit(b.right(), environment, depth + 1));
                default -> annotation;
            };
        }
        if (e instanceof Expr.FunctionCall f) {
            // §4.6 Eenheid1: a value aggregation carries its FIRST operand's unit (the rule this backend's own
            // concatenatieFold already states). TIJDSDUUR/ABSOLUTE_TIJDSDUUR are the documented exception above.
            return switch (f.kind()) {
                case SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE -> {
                    List<Expr> args = f.arguments();
                    if (args == null || args.isEmpty()) yield annotation;
                    Unit u = unit(args.get(0), environment, depth + 1);
                    yield foldsTimeline(f.kind()) ? afterTimeFolding(args.get(0), u) : u;
                }
                default -> annotation;
            };
        }
        return annotation;
    }

    /**
     * §7.1 / ch. 3.6 — the unit a TOTAAL leaves behind when its operand is a <b>timeline-declared</b> attribute.
     *
     * <p>Such a fold runs in two steps, and the first is what this rule is about: each element's own timeline
     * collapses at that attribute's declared granularity, every segment contributing <i>value × the whole grid
     * units of its span</i> (§4.20 Tabel 30). A RATE operand is therefore multiplied by a COUNT of its
     * denominator's grid units, and what survives is the rate's NUMERATOR — "€/mnd getotaliseerd over maanden
     * is €". Only then does a set fold (when there is one) sum those per-element scalars.
     *
     * <p>Deriving operand 0's unit unchanged here (the plain §4.6 <i>Eenheid1</i> rule) would answer €/mnd for a
     * result that is €, and every restatement downstream would then be handed a pair with no conversion factor.
     * Measured 2026-09-09: that is exactly what {@code jutterswet-tempototaal} ("bewaargeld") and
     * {@code jutterswet-tempovouw} ("bewaarlast") produced, and both only replayed against their pinned values
     * because the restatement silently degraded to the identity. {@code ref} derives the numerator and never
     * reaches a restatement at all; this is that same derivation.
     *
     * <p><b>Scope: exactly the two kinds that fold.</b> SOM and TOTAAL, because those are exactly the kinds both
     * consumers admit into the per-element timeline fold, and the scope is taken from them rather than from the
     * corpus. The exec-model branch refuses a fold over a member on a timeline by name for every other kind;
     * {@code ref}'s {@code eval.ml} faults on it in as many words ("only sum and total carry the per-element
     * timeline fold"). A MINIMUM or an EERSTE over a timeline has no defined moment to rank, so neither consumer
     * folds it, no denominator is consumed, and none may be dropped.
     *
     * <p>Restricted to TOTAAL alone, this would leave SOM answering the rate for a value that is already folded.
     * Measured 2026-09-09 with that restriction, on a copy of {@code jutterswet-tempovouw} with the fold's kind set
     * to SOM: into a plain € doel this backend faulted where ref computes, and into a €/jr doel it silently
     * produced 576 for 48, the mnd→jr factor applied to an already-folded value, where ref faults with
     * "incompatible units: EUR/jr vs EUR". The corpus itself carries no SOM over a
     * timeline-declared attribute (all 19 such folds are TOTAAL, 4 of them over a rate), which is precisely why
     * the corpus could not have been the source of this rule.
     *
     * <p>What this does NOT check, deliberately: that the denominator is the grid's own unit ("€/kg" on a MAAND
     * timeline). {@code ref} does check it; doing so here would need a granularity→unit mapping this class has no
     * honest source for, and guessing one from abbreviations is the kind of name test this backend has already
     * been bitten by. The shape is absent from the corpus (all four rate cases are €/mnd on MAAND), and it is a
     * question about the FOLD's value, which the verifier and ref own, not about a restatement.
     */
    /** The fold kinds that carry the §7.1 per-element timeline fold — the same two both consumers admit. */
    private static boolean foldsTimeline(Expr.FunctionKind kind) {
        return kind == Expr.FunctionKind.TOTAAL || kind == Expr.FunctionKind.SOM;
    }

    private static Unit afterTimeFolding(Expr operand, Unit u) {
        if (u == null || u.denominator() == null) return u;
        if (!(operand instanceof Expr.AttributeReference a) || a.objectType() == null) return u;
        return Namen.containsName(a.objectType().granularityMembers(), a.attribute()) ? u.numerator() : u;
    }

    /**
     * §4.4 / ch. 2.6, the MAAL join — the Java twin of the frontend's {@code Typing.compoundUnit}, which
     * is where this rule is authored. One unit-bearing operand hands its unit through (the factor-free product).
     * TWO of them do NOT: a rate times its (convertible) denominator unit CANCELS to the numerator ("€/jr maal jr
     * = €"), and anything else is the OPAQUE product ("€ maal kg = €·kg") — a unit with no canonical, convertible
     * only to an identical product.
     *
     * <p>That opacity is load-bearing here, not cosmetic: it is what stands between a two-unit product and a
     * restatement by a live factor. Handing back the left operand's unit would scale €·kg into a € target.
     * But what an opaque product buys downstream is NOT uniformly an honest marker, and the two shapes must
     * not be conflated (measured 2026-09-04 on {@code Unit}: for m·m against m, {@code convertibleTo} is
     * false, {@code requiredConversion} is false, {@code conversionFactorTo} FAULTS rather than answering 1 —
     * the fail-fast card of the same day, and the reason the no-op below is each site's OWN identity, taken
     * before it ever asks, and never an invented factor — and {@code exactRestateFactors} is null):
     * <ul>
     *   <li>a site that REFUSES on {@code convertibleTo} gets a false and turns the whole construct into an
     *       honest marker/skip;</li>
     *   <li>a site that merely COMPUTES a factor — the scalar §4 write boundary, the additive operand
     *       boundary, the §5.1 Tabel 32 comparison restatement — gets the IDENTITY and the restatement becomes
     *       a NO-OP. {@code TmsTranspiler.rescaled} returns the value unchanged. No marker.</li>
     * </ul>
     *
     * <p>So on a scalar restatement the value simply stands UNRESTATED, in whatever unit it was computed in.
     * That is conservative in the sense that matters — no lie-driven magnitude, because no factor was invented
     * — but it is a SILENT ref/Drools divergence, not a loud one, and this class must not claim otherwise.
     * Its blast radius is bounded by who else accepts the form: the frontend's
     * {@code Typing.compoundUnit} flags the unit mismatch, {@code ref} faults, and the ASP
     * translator refuses ahead of translation, so no corpus golden carries it and the divergence is
     * unobservable in every gate. {@code CaseRunner} builds both branches, which render one analysis, so the form
     * takes the same course on both.
     *
     * <p>The unspellable case (a rate factor, "€·m/jr") reaches a consumer only on a document the frontend
     * never wrote — {@code Typing} makes it a loud error rather than a type. This join answers it with a
     * DENOMINATOR-LESS opaque product ({@link Unit#product} keeps the numerator factors and drops the
     * noemer, so "€/jr maal kg" and "€/jr maal kg/jr" both yield €·kg): again the conservative,
     * non-restatable outcome rather than an invented factor. RUNTIME-SEMANTICS ch. 2.10 records it as the
     * fourth MAAL outcome and names the three consumers that refuse it.
     */
    private static Unit timesUnit(Unit el, Unit rightUnit) {
        if (el == null) return rightUnit;
        if (rightUnit == null) return el;
        if (el.denominator() != null && rightUnit.denominator() == null && rightUnit.convertibleTo(el.denominator())) return el.numerator();
        if (rightUnit.denominator() != null && el.denominator() == null && el.convertibleTo(rightUnit.denominator())) return rightUnit.numerator();
        return opaqueProduct(el, rightUnit);
    }

    /**
     * The identity table of every opaque product THIS derivation built — the provenance mark
     * {@link UnitConflict} keys its exemption on.
     *
     * <p><b>Why identity and not the value.</b> {@link Unit#product} builds "€·kg" as a plain
     * {@code Unit} with no canonical, no factor and no denominator, and the wire can declare a unit with
     * exactly those four fields and a "·" in its name. The two are therefore value-indistinguishable, and a
     * test on the ABBREVIATION hands a wire-declared "appel·peer" the exemption meant for a derived product —
     * measured 2026-09-09: {@code ref} faults on such a document, this backend replayed it. Only WHERE the unit
     * came from separates them, and provenance is identity.
     *
     * <p>A marker field on {@code Unit} itself would be the smaller encoding, but that record lives in the
     * frontend module and is outside this card's reach; this table is the same statement made from the one place
     * in the backend that builds such a product.
     *
     * <p><b>Its growth, measured rather than assumed.</b> One entry per {@link #timesUnit} EVALUATION — not
     * per node and not per model: {@link Unit#product} allocates a fresh instance every time, so transpiling
     * one document three times in a JVM adds three sets of entries (measured 0 → 2 → 4 → 6 on a document with
     * two such nodes). The table is monotone for the lifetime of the JVM and is never cleared, so it is a
     * STRONG-REFERENCE LEAK, stated plainly. Two things bound the damage and neither is a correctness argument
     * about the leak: the corpus produces ZERO entries (no golden carries a two-unit MAAL), and each entry is
     * one small record.
     *
     * <p>Correctness is unaffected by the growth: a stale entry can never mark a NEW unit, because
     * {@link Unit#product} never returns an existing instance, so no aliasing is possible however long the
     * table lives. Clearing it per transpilation is rejected: it would need a guarantee that no other derivation
     * instance is live, and nothing in the build route provides one — the analysis and the IR loader each
     * construct their own {@code EffectiveUnit}, and a clear on construction would silently unmark a product the
     * other is still holding. That turns a bounded leak into a wrong
     * answer, which is the wrong trade. Bounding it properly wants a weak IDENTITY set, which the JDK has no
     * form of; it is carded rather than improvised here.
     */
    private static final java.util.Set<Unit> DERIVED_PRODUCTS =
            java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new IdentityHashMap<>()));

    private static Unit opaqueProduct(Unit a, Unit b) {
        Unit p = Unit.product(a, b);
        DERIVED_PRODUCTS.add(p);
        return p;
    }

    /** Whether {@code e} is an opaque product THIS derivation built (identity, never the name). */
    static boolean isDerivedProduct(Unit e) {
        return e != null && DERIVED_PRODUCTS.contains(e);
    }

    /** The table's current size — so the growth law above is a CHECKED claim rather than a comment. */
    static int countDerivedProducts() {
        return DERIVED_PRODUCTS.size();
    }

    private Unit letUnit(Expr e, Map<String, Unit> environment, int depth, Unit annotation) {
        if (e instanceof Expr.Let b) return unit(b.body(), withBinder(environment, b, depth), depth + 1);
        if (e instanceof Expr.BinderRef lz) {
            // BOUND (to a unit, or to a unitless value) and UNBOUND are different facts, so both tables are asked
            // with containsKey: a binder bound to a unitless value HAS a source and the annotation may not speak
            // over it; only a read no Let anywhere binds falls back. Both maps hold nulls for exactly this.
            if (environment.containsKey(key(lz.name()))) return environment.get(key(lz.name()));
            if (readUnits == null) buildReads();
            return readUnits.containsKey(e) ? readUnits.get(e) : annotation;
        }
        // ch. 10.2: exactly one branch runs; both must agree on the unit, so either arm derives it.
        Expr.IfThenElse k = (Expr.IfThenElse) e;
        Unit then = unit(k.then(), environment, depth + 1);
        return then != null ? then : unit(k.otherwise(), environment, depth + 1);
    }

    private Map<String, Unit> withBinder(Map<String, Unit> environment, Expr.Let b, int depth) {
        Map<String, Unit> inside = new HashMap<>(environment);
        // WF-076: the value is derived in the ENCLOSING scope (the binder is not visible in its own value).
        // A UNITLESS value binds null (HashMap carries it) rather than removing the key: removing would report
        // the binder as unbound, and an unbound read falls back to its own annotation — the very repetition this
        // class exists to not read. Removal would also un-shadow an outer binder of the same name (WF-064).
        inside.put(key(b.binder()), unit(b.value(), environment, depth + 1));
        return inside;
    }

    // --- the binder-read table ------------------------------------------------------------------

    /**
     * Record the derived unit of every binder READ, by walking each {@link Expr.Let} with a scope. Walking from
     * EVERY Let (not only the outermost) is deliberate and conflict-free: a walk started at an inner Let simply
     * does not bind the outer names, so it records nothing for their reads; a walk started at an enclosing one
     * shadows correctly and records both. A read whose binder no walk ever binds keeps its own annotation.
     */
    private void buildReads() {
        readUnits = new IdentityHashMap<>();
        for (Expr l : lets) noteUnit(l, EMPTY, 0);
    }

    private void noteUnit(Expr e, Map<String, Unit> environment, int depth) {
        if (e == null || depth > MAX_DEPTH) return;
        if (e instanceof Expr.BinderRef lz) {
            String k = key(lz.name());
            if (environment.containsKey(k)) readUnits.put(e, environment.get(k));
            return;
        }
        if (e instanceof Expr.Let b) {
            noteUnit(b.value(), environment, depth + 1);
            noteUnit(b.body(), withBinder(environment, b, depth), depth + 1);
            return;
        }
        if (e instanceof Expr.IfThenElse k) {
            for (Expr x : conditionOperands(k.condition())) noteUnit(x, environment, depth + 1);
            noteUnit(k.then(), environment, depth + 1);
            noteUnit(k.otherwise(), environment, depth + 1);
            return;
        }
        for (Expr kind : children(e)) noteUnit(kind, environment, depth + 1);
    }

    /**
     * Every Expr-carrying component of an ordinary v0 node (the {@link Expr#contains} inventory minus its ch. 4.10
     * arms — {@link #noteUnit} matches all three cluster nodes itself, because each needs a SCOPE this cannot
     * carry).
     *
     * <p><b>This accessor is LOAD-BEARING.</b> {@link #conditionOperands} hands back only ROOTS, so a kind this
     * method does not know contributes NO children at all and
     * every binder read beneath it silently loses its recorded unit — falling back to the annotation this class
     * exists to not read. So the {@code switch} below is EXHAUSTIVE over the sealed union, with no
     * {@code default}: the next node the union grows is a COMPILE ERROR here rather than a silent gap, exactly
     * as {@link Expr#contains} is written for the same reason. The six genuine leaves are spelled out; the three
     * ch. 4.10 nodes are unreachable — {@link #noteUnit} intercepts each before it gets here — and they REFUSE
     * rather than answering, because handing back their children from a place that carries no scope is the
     * defect, not the fix.
     */
    private static List<Expr> children(Expr e) {
        return switch (e) {
            case Expr.Binary b      -> childrenOf(b.left(), b.right());
            case Expr.Rounding a    -> childrenOf(a.base());
            case Expr.Clamp b    -> childrenOf(b.base(), b.minimum(), b.maximum());
            case Expr.DimensionCellRead c -> childrenOf(c.attribute());
            case Expr.FunctionCall f -> {
                java.util.List<Expr> out = new java.util.ArrayList<>();
                if (f.arguments() != null) out.addAll(f.arguments());
                if (f.emptyFallback() != null) out.add(f.emptyFallback());
                out.addAll(conditionOperands(f.filter()));
                yield out;
            }
            case Expr.TextComposition t -> {
                java.util.List<Expr> out = new java.util.ArrayList<>();
                if (t.parts() != null)
                    for (Expr.TextComposition.Part d : t.parts())
                        if (d instanceof Expr.TextComposition.EmbeddedExpression i) out.add(i.expr());
                yield out;
            }
            case Expr.Literal ignored      -> List.of();
            case Expr.AttributeReference ignored -> List.of();
            case Expr.ParameterRef ignored -> List.of();
            case Expr.VariableRef ignored -> List.of();
            case Expr.Constant ignored    -> List.of();
            case Expr.Unresolved ignored   -> List.of();
            // the §5.7 instance-set union (WF-110): its own fields are SYMBOLIC — a type name and a
            // role binding — so the only child expressions are the selections' filter operands, and
            // each is read against its own selection's element, exactly as a Functie's filter is
            case Expr.InstanceSetUnion u -> {
                java.util.List<Expr> out = new java.util.ArrayList<>();
                for (Expr.InstanceSetUnion.Branch t : u.branches()) out.addAll(conditionOperands(t.filter()));
                yield out;
            }
            case Expr.Let ignored        -> throw clusterNode();
            case Expr.BinderRef ignored  -> throw clusterNode();
            case Expr.IfThenElse ignored -> throw clusterNode();
        };
    }

    private static IllegalStateException clusterNode() {
        return new IllegalStateException("een ch. 4.10-knoop hoort nooit bij kinderen() te komen: noteer() vangt "
                + "hem af, omdat hij een scope draagt die deze accessor niet kan meegeven");
    }

    private static List<Expr> childrenOf(Expr... xs) {
        java.util.List<Expr> out = new java.util.ArrayList<>();
        for (Expr x : xs) if (x != null) out.add(x);
        return out;
    }

    /**
     * The ROOT of every Expr operand a condition carries — the nodes {@link #noteUnit} may start a walk at.
     *
     * <p><b>Roots, never descendants.</b> The shared {@link Condition#containsOperand} scan is a FULL pre-order walk:
     * asked with a predicate that never short-circuits it yields each operand AND every node beneath it. Handing
     * that flat list to {@link #noteUnit} makes it RE-ENTER each operand tree midway, carrying the enclosing scope
     * into a place that scope does not govern. For a SHADOWING inner {@code Let} that is not redundant work but a
     * wrong answer: the correct recording is made first, through the Let, and the mid-tree re-entry then
     * overwrites it with the outer binder's unit (measured kg where WF-064 demands t, in both shapes
     * {@code EffectiveUnitWitnessTest} now pins). Scope is a property of the PATH to a node, so a walk may
     * only ever begin where a path begins.
     *
     * <p>The roots are derived FROM the shared scan rather than by re-listing {@code Conditie}'s own arms: an
     * entry that another entry contains is a descendant by definition, and what is left is exactly the operand
     * roots. That keeps one source of truth for which conditions carry operands — a {@code Conditie} arm added
     * later is covered here the moment the shared walk covers it, with nothing to keep in step. The same
     * derivation serves {@link #children}'s {@code FunctionCall} filter, so both callers gained the repair at once.
     *
     * <p><b>Aliasing, stated rather than left implicit.</b> Should ONE instance appear both as a bare operand and
     * as a descendant of another operand, the containment test drops it as a descendant and only the enclosing
     * walk reaches it — so the INNER scope wins. That is the harmless direction: {@link #readUnits} is
     * identity-keyed, so the two positions are indistinguishable to it anyway and one of the two answers must be
     * discarded regardless. No corpus shape reaches it either: the loader builds a fresh record per wire node, so
     * a shared instance can only come from a hand-built tree.
     */
    private static List<Expr> conditionOperands(Condition c) {
        if (c == null) return List.of();
        java.util.List<Expr> flat = new java.util.ArrayList<>();
        Condition.containsOperand(c, x -> { flat.add(x); return false; });
        java.util.List<Expr> roots = new java.util.ArrayList<>();
        for (Expr candidate : flat) {
            boolean underAnother = false;
            for (Expr possibleParent : flat) {
                // identity, not equality: two operands can be value-equal records and still be separate roots
                if (possibleParent == candidate) continue;
                if (Expr.contains(possibleParent, x -> x == candidate)) { underAnother = true; break; }
            }
            if (!underAnother) roots.add(candidate);
        }
        return roots;
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
