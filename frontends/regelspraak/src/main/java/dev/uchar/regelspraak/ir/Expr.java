package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Granularity;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Parameter;

import java.util.List;

/**
 * A resolved, typed expression — the intermediate representation a §13.4.16/.17
 * RegelSpraak expressie denotes, and the input the Drools transpiler will consume.
 *
 * Every node carries (a) its result {@link Datatype}, computed via the typeringen
 * matrix ({@link dev.uchar.regelspraak.resolver.Typing}), and (b) the original
 * source slice, for diagnostics and traceability back to the regel text.
 *
 * References are bound to the model where the resolver can do so —
 * attribute-of-subject against a known object type, and parameters. Role navigation and
 * deep van-chains have since been bound too: {@link AttributeReference} carries them as
 * {@code navigationRole} and {@code navigationPath}. What still cannot be bound becomes an
 * {@link Unresolved} node carrying {@link Datatype.Unknown}, so the IR is explicit about
 * its resolution coverage rather than guessing: a transpiler can see exactly what is and
 * isn't resolved. This mirrors the resolver's "unknown ⇒ no claim" discipline.
 */
public sealed interface Expr {
    /** The expression's result datatype (per the typeringen matrix). */
    Datatype type();
    /** The structured source anchor (file/line/column + whitespace-normalised snippet) this node
     *  was built from. Its {@link Source#toString()} is the snippet, the node's {@code source} text on the wire. */
    Source source();

    /** Whether {@code e} is or contains a sub-expression matching {@code test} — a generic IR-tree scan over
     *  every Expr-carrying component (arithmetic operands, functie argumenten + leegwaarde-fallback + its
     *  filter-conditie's operands, rounding/clamp bases, tekst-interpolaties, the dimensiecel base). Null-safe.
     *
     *  <p>The switch is EXHAUSTIVE over the sealed union — no {@code default} arm — so the next node the union
     *  grows is a COMPILE ERROR here rather than a silent leaf. That is deliberate: a missing arm under-reports
     *  ("this tree does not mention X") and every caller reads a false as a licence to omit something. The seven
     *  leaves are spelled out for the same reason; each is a genuine leaf, not an unhandled case. */
    static boolean contains(Expr e, java.util.function.Predicate<Expr> test) {
        if (e == null) return false;
        if (test.test(e)) return true;
        return switch (e) {
            case Binary b      -> contains(b.left(), test) || contains(b.right(), test);
            case FunctionCall f     -> f.arguments().stream().anyMatch(x -> contains(x, test))
                                  || contains(f.emptyFallback(), test) || Condition.containsExpr(f.filter(), test);
            case Rounding a    -> contains(a.base(), test);
            case Clamp b    -> contains(b.base(), test) || contains(b.minimum(), test) || contains(b.maximum(), test);
            case TextComposition t  -> t.parts().stream().anyMatch(d -> d instanceof TextComposition.EmbeddedExpression i && contains(i.expr(), test));
            case DimensionCellRead c -> contains(c.attribute(), test);
            // A Let's binding is a child on BOTH sides: the value may never be evaluated (ch. 10.6 is
            // call-by-need) but it is structurally present, and every caller of this scan asks a
            // STRUCTURAL question ("does this tree mention X?"). A scan that stopped at the binder would
            // under-report a Rekendatum/parameter read hidden in a Let value.
            case Let l         -> contains(l.value(), test) || contains(l.body(), test);
            // Both branches are children, and so is every Expr operand of the guard: exactly one branch RUNS
            // (ch. 10.2), but the question this scan answers is structural, and a scan that saw only the taken
            // branch would have to know a runtime verdict it cannot have. The guard's operands are reached
            // through the shared Conditie walk, the same way a FunctionCall's filter is.
            case IfThenElse k  -> contains(k.then(), test) || contains(k.otherwise(), test)
                                  || Condition.containsExpr(k.condition(), test);
            // A union's own fields are SYMBOLIC — a type name and a role binding — so the only child
            // expressions live in the selections' filters, reached through the shared Conditie walk
            // exactly as a FunctionCall's filter is.
            case InstanceSetUnion u -> u.branches().stream()
                                        .anyMatch(t -> Condition.containsExpr(t.filter(), test));
            case Literal ignored      -> false;
            case AttributeReference ignored -> false;
            case ParameterRef ignored -> false;
            case VariableRef ignored -> false;
            case BinderRef ignored    -> false;
            case Constant ignored    -> false;
            case Unresolved ignored   -> false;
        };
    }

    /** §5.3: whether {@code e} is the Rekendatum or Rekenjaar execution-date constant — the trigger for the
     *  §4.2 rekendatum global when used as a VALUE operand (§13.4.17.34). */
    static boolean isContextValue(Expr e) {
        return e instanceof Constant c && ("Rekendatum".equals(c.name()) || "Rekenjaar".equals(c.name()));
    }

    /** §13.2 literal: a number, text, boolean, date, or enumeration value. */
    record Literal(String valueText, Datatype type, Source source) implements Expr {}

    /** §5.4/§13.4.17.9 "reeks van teksten en waarden": an interleaving of literal karakterreeks chunks and
     *  «expressie» parts, each coerced to Tekst and concatenated at runtime. A PLAIN "…" (no «…») stays an
     *  {@link Literal}; this node is built ONLY when at least one «expressie» part is present. */
    record TextComposition(List<Part> parts, Source source) implements Expr {
        @Override public Datatype type() { return new Datatype.Text(); }
        /** One piece of a tekstenwaardereeks: a literal chunk or an embedded expression. */
        public sealed interface Part {}
        /** A literal karakterreeks chunk (already unquoted, no guillemets). */
        public record LiteralChunk(String text) implements Part {}
        /** A «expressie» part — its value is coerced to Tekst at evaluation. */
        public record EmbeddedExpression(Expr expr) implements Part {}
    }

    /** §13.4.16 arithmetic operators and §13.4.17.12 percentage-of. */
    enum BinOp { PLUS, MIN, VERMINDERD_MET, MAAL, GEDEELD_DOOR, GEDEELD_DOOR_ABS, TOT_DE_MACHT, PERCENTAGE_VAN }
    record Binary(BinOp op, Expr left, Expr right, Datatype type, Source source) implements Expr {}

    /** §13.4.17.10-.58 named functions and aggregations over their argument expressions.
     *
     *  <p>{@code AANTAL_GEVULDE} is the one member with NO source-language spelling: the filled-value
     *  count (IR-SPEC ch. 4.3, RUNTIME-SEMANTICS ch. 3.4c) is a uchar dialect kind, written first on
     *  the wire and read here by the Drools backend, which compiles this record. Since 2026-09-20 it
     *  also has a PRODUCER route, and that route is a dialect extension of this frontend's grammar
     *  rather than a v2.3.0 form: "het aantal gevulde waarden van &lt;setlezing&gt;", measured inert on
     *  official documents in {@code grammar/DIVERGENCES.md} §6. */
    enum FunctionKind {
        SOM, TOTAAL, AANTAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE, AANTAL_GEVULDE, WORTEL, ABSOLUTE_WAARDE,
        CONCATENATIE, TIJDSDUUR, ABSOLUTE_TIJDSDUUR, EERSTE_PAASDAG, TIJDSEVENREDIG_DEEL,
        AANTAL_DAGEN_IN, JAAR_UIT, MAAND_UIT, DAG_UIT, UUR_UIT, MINUUT_UIT, SECONDE_UIT, MILLISECONDE_UIT,
        DATUM_MET, DATUMTIJD_MET
    }
    /** `elementObjectType` is the object type the aggregation ranges over — the type
     *  whose instances are summed/counted/etc. (null if unresolved). It is what the
     *  transpiler binds the accumulate source pattern to.
     *
     *  `emptyFallback` is the §5.8.2 "of … als die er niet zijn" fallback for a sommatie
     *  (SOM/TOTAAL): the value the sum takes when it has no non-empty contributors,
     *  instead of a leegwaarde. Null when the source declared no fallback (an empty
     *  sommatie is then a leegwaarde); always null for non-sommatie functions.
     *
     *  `filter` is a §13.4.2 "die/dat &lt;predicaat&gt;" subselectie on the counted/aggregated
     *  element ("het aantal deelnemers van de groep die een kind zijn" — only the kind-deelnemers).
     *  It is resolved against the ELEMENT type, not the rule subject. Null when the operand
     *  carries no such filter. A kenmerk-membership predicate now resolves to a real
     *  {@link Condition.CharacteristicCheck} against the element type; a filter the resolver still cannot
     *  structure is kept here as an {@link Condition.Unresolved} marker so the transpiler can see it
     *  must NOT lower the aggregation to a count over every instance — it emits a visible marker
     *  instead of a silently-wrong total. */
    /** §13.4.17.51-.54 a resolved dimension selection on an aggregation: the dimension the
     *  aggregation ranges over and the CONCRETE ordered list of labels selected — the
     *  "vanaf &lt;a&gt; t/m &lt;b&gt;" interval and the "in {…}" set are already expanded against the
     *  dimension's declared label order (§13.3.7.5), and "over alle …" is every label. The
     *  transpiler folds the dimensioned attribute over exactly these labels (a fixed sum/min/max
     *  of {@code map.get(label)}), so no label arithmetic survives into the back-end. */
    record DimensionSelection(String dimension, List<String> labels, Source source) {}

    /** {@code dimensionSelection} (§13.4.17.51-.54), when non-null, restricts a SOM/MIN/MAX over a
     *  gedimensioneerd attribuut to the selected dimension labels — the only aggregation that
     *  ranges over a dimension's value space rather than over object instances. Null for an
     *  ordinary instance aggregation. */
    record FunctionCall(FunctionKind kind, List<Expr> arguments, ObjectType elementObjectType,
                   Expr emptyFallback, Condition filter, DimensionSelection dimensionSelection,
                   Datatype type, Source source, Granularity granularity) implements Expr {
        /** {@code granularity} is the §13.4.17.57 aantal-dagen-in operator's stated bucket unit ("in <de maand|
         *  het jaar>"); null for every other functie. The transpiler requires it to MATCH the doel's tijdlijn
         *  (§4.21/§7.2), markering a mismatch rather than silently bucketing by the wrong unit. */
        public FunctionCall(FunctionKind kind, List<Expr> arguments, ObjectType elementObjectType,
                       Expr emptyFallback, Condition filter, DimensionSelection dimensionSelection,
                       Datatype type, Source source) {
            this(kind, arguments, elementObjectType, emptyFallback, filter, dimensionSelection, type, source, null);
        }
        /** Most call sites build an instance aggregation with no dimensieselectie and no granulariteit. */
        public FunctionCall(FunctionKind kind, List<Expr> arguments, ObjectType elementObjectType,
                       Expr emptyFallback, Condition filter, Datatype type, Source source) {
            this(kind, arguments, elementObjectType, emptyFallback, filter, null, type, source, null);
        }
    }

    /** The §5.7 CONCATENATION of subject expressions as the source of an instance count — the wire's
     *  {@code InstanceSetUnion} (uchar IR-SPEC ch. 4.3, WF-110; semantics RUNTIME-SEMANTICS ch. 3.4d,
     *  maintainer decision 2026-09-20). "het aantal &lt;rolselectie&gt; en &lt;rolselectie&gt;" joins the
     *  instance sets the selections yield into ONE set: the members are the union of the selections'
     *  contributors, deduplicated on instance identity, so an instance two selections both select is
     *  counted once.
     *
     *  <p>It is the SINGLE argument of a {@link FunctionKind#AANTAL} functie and stands nowhere else;
     *  the count above it then names no source of its own. It carries no {@link #type()} — it denotes
     *  instances, not a value — which is why {@code type()} answers null here and the wire writes no
     *  {@code type} key at all.
     *
     *  <p>This is the reading the multi-operand VALUE count does NOT have: that form stays refused
     *  (WF-037, {@code ExpressionBuilder.countArityError}), and the two are distinguishable on the
     *  wire because this one has exactly ONE argument. */
    record InstanceSetUnion(List<Branch> branches, Source source) implements Expr {
        @Override public Datatype type() { return null; }

        /** One selection: the population it ranges over, the one-step relation it walks from the
         *  count's subject, and its own die/dat subselection — evaluated PER CONTRIBUTOR with the
         *  element as the condition's subject, exactly as a fold's filter is. {@code filter} may be
         *  null: an UNFILTERED selection beside a filtered one is an ordinary case here, where the v0
         *  toolchain leaves it unlowered because its own lowering ORs the operands' filter terms and
         *  has no term to OR for one without a filter. There is deliberately no path spelling: a
         *  selection carries a one-step relation, and a count's range is one step (WF-102 (c)). */
        public record Branch(ObjectType elementType, RegelSpraakModel.RoleBindingRef sourceRole,
                          Condition filter, Source source) {}
    }

    /** §13.4.17.25/.26 rounding wrapper. Rounding to N decimals sets the result's precision to N
     *  (§6.1.2: it is exactly the explicit afronding that lets a higher-precision result be assigned
     *  to a lower-precision attribute), keeping the base's unit and sign. */
    enum RoundingMode { NAAR_BENEDEN, NAAR_BOVEN, REKENKUNDIG, RICHTING_NUL, WEG_VAN_NUL }
    record Rounding(Expr base, RoundingMode mode, int decimals, Source source) implements Expr {
        @Override public Datatype type() {
            Datatype b = base.type();
            if (b instanceof Datatype.Numeric n) return new Datatype.Numeric(decimals == 0, decimals, n.sign(), n.unit());
            if (b instanceof Datatype.Percentage p) return new Datatype.Percentage(decimals == 0, decimals);
            return b;
        }
    }

    /** §13.4.17.27-.30 bounding wrapper (a minimum and/or maximum); type is the base's. */
    record Clamp(Expr base, Expr minimum, Expr maximum, Source source) implements Expr {
        @Override public Datatype type() { return base.type(); }
    }

    /**
     * A resolved `<attribuut> van <onderwerp>` reference (§13.4.2.9). `objectType`
     * is the type the subject denotes (null when it could not be resolved), and
     * `type` is the attribute's declared datatype on that object type.
     *
     * `navigationRole` is the role name the author WROTE to reach that object (the
     * onderwerp's primary noun, e.g. "voordeur" in "de breedte van de voordeur"),
     * or null when the reference is the subject's own attribute / names an object type
     * directly. Threading the written role lets the transpiler pick the right
     * reference field when one object-type pair is connected by more than one feittype
     * or role — and distinguishes a wederkerig self-relation ("zijn partner", where the
     * navigated type equals the subject's) from a plain own-attribute read (§13.3.9.2).
     */
    record AttributeReference(String attribute, ObjectType objectType, String subjectSource,
                        String navigationRole, Datatype type, Source source, boolean set,
                        java.util.List<String> navigationPath,
                        java.util.List<dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef> rangePath,
                        Condition.Quantifier quantifier, int threshold)
            implements Expr {
        /** {@code set} marks an operand that ranges over a SET (a §13.4.2 set quantifier — alle/al/geen van de/
         *  ten minste…/ten hoogste…/precies… — NOT the bare definite "de"), computed structurally from the
         *  onderwerp's quantifier rather than re-scanned from the onderwerp text. Lets the transpiler/resolver
         *  distinguish a set aggregation from a single-object read even when the element type equals the subject.
         *
         *  {@code navigationPath} is the resolved MULTI-HOP to-one navigation chain (§13.4.1 "de X van de Y van de Z"):
         *  the DECLARED feittype role names from the rule subject to the object carrying {@code attribute}, in
         *  navigation order — empty for the subject's own attribute and for a single-hop navigation (which stays
         *  the written {@code navigationRole}). INVARIANT: a non-empty pad implies a non-null {@code navigationRole},
         *  so every own-attribute fast path (keyed on navigationRole == null) routes a chain to the navigated
         *  branches, whose choke points (naviResolve / navReader / navComparisonRole) are pad-aware.
         *
         *  {@code rangePath} is the resolved MULTI-STEP fold-source RANGE (IR-SPEC ch. 4.3 / WF-102, the
         *  uchar wire's {@code sourcePath} on the enclosing Aggregation): the symbolic role bindings from the rule
         *  subject to the element type, subject-outwards, one per written set step. Empty for every source
         *  whose range is ONE relation — that one stays the emitter's {@code sourceRole} — and empty whenever
         *  the written chain declines to resolve, which keeps the resolver's refusal intact instead of
         *  half-binding. It sits on the operand because that is where the written chain is; the emitter
         *  lifts it onto the fold, which is where the wire carries it.
         *
         *  <p>{@code quantifier} is the WRITTEN §13.4.2 quantifier of the onderwerp, kept because {@code set}
         *  collapses them all to one bit: "alle", "geen van de" and every counting quantor are equally
         *  {@code set == true}, so a reader that took the flag for a forall would answer ALLE for a
         *  sentence that wrote "geen van de". Null for the bare definite "de" (a single object) and for
         *  every reference built outside the onderwerp path. {@code threshold} is its written telwoord,
         *  travelling beside it for the same reason: without it "ten minste 2 van de" and "ten minste 3
         *  van de" would reach the resolver as one and the same sentence. 0 when the quantor carries no
         *  count. FRONTEND-INTERNAL, both of them: they are the resolver's
         *  input for lowering a §8.2 quantified condition to {@link Condition.QuantifierFold}, which records
         *  them on the wire — the reference node itself never carries them out, so no schema field
         *  corresponds to these components. */
        public AttributeReference(String attribute, ObjectType objectType, String subjectSource,
                            String navigationRole, Datatype type, Source source, boolean set,
                            java.util.List<String> navigationPath,
                            java.util.List<dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef> rangePath) {
            this(attribute, objectType, subjectSource, navigationRole, type, source, set, navigationPath, rangePath, null);
        }
        /** {@code threshold} defaults to 0: it is read only under a COUNTING {@code quantifier}, and every
         *  reference built outside the onderwerp path carries none. */
        public AttributeReference(String attribute, ObjectType objectType, String subjectSource,
                            String navigationRole, Datatype type, Source source, boolean set,
                            java.util.List<String> navigationPath,
                            java.util.List<dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef> rangePath,
                            Condition.Quantifier quantifier) {
            this(attribute, objectType, subjectSource, navigationRole, type, source, set, navigationPath, rangePath,
                    quantifier, 0);
        }
        public AttributeReference(String attribute, ObjectType objectType, String subjectSource,
                            String navigationRole, Datatype type, Source source, boolean set,
                            java.util.List<String> navigationPath) {
            this(attribute, objectType, subjectSource, navigationRole, type, source, set, navigationPath,
                    java.util.List.of());
        }
        public AttributeReference(String attribute, ObjectType objectType, String subjectSource,
                            String navigationRole, Datatype type, Source source, boolean set) {
            this(attribute, objectType, subjectSource, navigationRole, type, source, set, java.util.List.of());
        }
        public AttributeReference(String attribute, ObjectType objectType, String subjectSource,
                            String navigationRole, Datatype type, Source source) {
            this(attribute, objectType, subjectSource, navigationRole, type, source, false, java.util.List.of());
        }
    }

    /** §3.6 a single cell of a gedimensioneerd attribuut: the bare attribute read plus the selected label
     *  for each of its dimensions (in the attribute's declared dimension order — one label for a
     *  single-dimension attribute). The transpiler lowers it to {@code <map-read>.get(label)}; the result
     *  type is the attribute's element datatype. A read must pin EVERY dimension (§3.6), so the labels list
     *  has one entry per dimension. */
    record DimensionCellRead(AttributeReference attribute, List<String> labels, Source source) implements Expr {
        @Override public Datatype type() { return attribute.type(); }
    }

    /** A resolved parameter reference (§13.3.8); the type is the parameter's. */
    record ParameterRef(Parameter parameter, Source source) implements Expr {
        @Override public Datatype type() { return parameter.datatype(); }
    }

    /** A local rule variable introduced by a §13.4.2.10 variabeledeel ("Daarbij geldt: …") that the
     *  resolver could NOT bind to a definition — a forward or cyclic reference. A BOUND variable read in
     *  a rule's result part is an {@link BinderRef} under its {@link Let}; one read in the voorwaarde is
     *  substituted at its use site (see {@link Let}). This node is therefore a residual marker only. */
    record VariableRef(String name, Datatype type, Source source) implements Expr {}

    /**
     * IR-SPEC ch. 4.10 / RUNTIME-SEMANTICS ch. 10.6: the value-level binding. {@code binder} is visible
     * only in {@code body}, never in its own {@code value} (WF-076), and the node's {@code type} is the
     * body's (WF-076).
     *
     * Semantics is call-by-need (maintainer decision O3, 2026-09-01): the {@code value} is evaluated in
     * the ENCLOSING scope at the FIRST read of the binder and reused for every further read; a read whose
     * value evaluates leeg makes the WHOLE Let leeg (the leeg-barrier sits at the READ place); a binder
     * the body never reads is never evaluated at all.
     *
     * This is what a §13.4.2.10 variabelendeel lowers to — one Let per declared variable that the result
     * part reads, nested in DECLARATION order (§11.1 rule 1), outermost first.
     */
    record Let(String binder, Expr value, Expr body, Datatype type, Source source) implements Expr {}

    /** WF-064: a read of the nearest enclosing {@link Let} binder (or match-arm binder) of this name.
     *  A bound binder is never leeg — the read either yields a gevuld value or ends its owning Let. */
    record BinderRef(String name, Datatype type, Source source) implements Expr {}

    /**
     * IR-SPEC ch. 4.10 / RUNTIME-SEMANTICS ch. 10.2: the value-level conditional. The guard is evaluated
     * under the ordinary comparison discipline (its operands disciplined to bound-non-leeg reads by WF-077),
     * then EXACTLY the chosen branch is evaluated — strict, no coalesce, the other branch not evaluated at
     * all. Both branches agree on {@code type}, which is the node's. WF-069 restricts where it may stand.
     *
     * <p>Never emitter-produced: no RegelSpraak construct lowers to it, so {@link dev.uchar.regelspraak.emit.IrEmitter}
     * writes none and no {@code source.rgs} golden carries one. It is here because the union is the shape
     * every consumer walks: a wire form parsed into anything else is an opaque leaf to {@link #contains} and
     * to every hand-written tree walk built on it, which under-report in silence.
     *
     * <p>The wire spells its fields {@code condition} / {@code then} / {@code else}; two of those are Java
     * keywords, so the record names them as the semantics does.
     */
    record IfThenElse(Condition condition, Expr then, Expr otherwise, Datatype type, Source source) implements Expr {}

    /** A built-in reckoning constant or referent: Rekendatum, Rekenjaar, hij, de dag. */
    record Constant(String name, Datatype type, Source source) implements Expr {}

    /** A reference or sub-expression the resolver could not bind to the model.
     *
     *  <p>{@code reason} is the sentence the frontend raises as a WARNING over this marker when it
     *  SURVIVES into the finished model — see {@link Condition.Unresolved#reason()} for why the reason
     *  travels on the node instead of being reported where the node is built, and why null means "the
     *  producing route already speaks for itself". Never serialised: the wire writes {@code node},
     *  {@code type} and {@code source}. */
    record Unresolved(Datatype type, Source source, Object reason) implements Expr {}

    /** Every {@link Unresolved} inside {@code e}, in structural order, handed to {@code result} as its
     *  {@code (bron, reden)} pair — the {@link Expr} half of the survivor traversal
     *  {@link Condition#collectUnresolved} drives. Walks every Expr-carrying component, and reaches a
     *  functie's filter and an if-then-else's guard back through the {@link Condition} half. Null-safe.
     *  EXHAUSTIVE over the sealed union, for the reason {@link #contains} states. */
    static void collectUnresolved(Expr e, java.util.function.BiConsumer<Source, Object> result) {
        if (e == null) return;
        switch (e) {
            case Unresolved o -> result.accept(o.source(), o.reason());
            case Binary b -> { collectUnresolved(b.left(), result); collectUnresolved(b.right(), result); }
            case FunctionCall f -> {
                f.arguments().forEach(x -> collectUnresolved(x, result));
                collectUnresolved(f.emptyFallback(), result);
                Condition.collectUnresolved(f.filter(), result);
            }
            case Rounding a -> collectUnresolved(a.base(), result);
            case Clamp b -> { collectUnresolved(b.base(), result); collectUnresolved(b.minimum(), result);
                                 collectUnresolved(b.maximum(), result); }
            case TextComposition t -> t.parts().forEach(d -> {
                if (d instanceof TextComposition.EmbeddedExpression i) collectUnresolved(i.expr(), result);
            });
            case DimensionCellRead c -> collectUnresolved(c.attribute(), result);
            case Let l -> { collectUnresolved(l.value(), result); collectUnresolved(l.body(), result); }
            case IfThenElse k -> { Condition.collectUnresolved(k.condition(), result);
                                   collectUnresolved(k.then(), result); collectUnresolved(k.otherwise(), result); }
            case InstanceSetUnion u -> u.branches()
                    .forEach(t -> Condition.collectUnresolved(t.filter(), result));
            case Literal ignored -> { }
            case AttributeReference ignored -> { }
            case ParameterRef ignored -> { }
            case VariableRef ignored -> { }
            case BinderRef ignored -> { }
            case Constant ignored -> { }
        }
    }
}
