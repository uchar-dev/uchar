package dev.uchar.regelspraak.resolver;

import dev.uchar.regelspraak.ir.Source;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.message.Msg;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.Unit;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;
import dev.uchar.regelspraak.model.RegelSpraakModel.Parameter;
import dev.uchar.regelspraak.core.Literals;
import dev.uchar.regelspraak.core.TimeCalculation;
import dev.uchar.regelspraak.parser.RegelSpraak.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic.params;

/**
 * Builds the resolved, typed {@link Expr} IR from a parsed §13.4.17 expressie,
 * binding references against the model and computing result datatypes via the
 * {@link Typing} matrix.
 *
 * The resolver's "unknown ⇒ no claim" guarantee holds for the node datatypes it
 * produces. Arithmetic, aggregations and bounds are first-class nodes, and
 * attribute/parameter references are bound, giving the transpiler something to walk
 * instead of a bare datatype. {@code functionType} types the aggregations — SOM, TOTAAL
 * (with the §4.20 rate strip), MIN/MAX, AANTAL and the date functions all carry a
 * computed type; the kinds with no scalar meaning keep Onbekend on purpose.
 */
final class ExpressionBuilder {
    private final RegelSpraakModel model;
    // The object type the current rule is about, so a possessive "zijn <attribuut>"
    // (and "hij") resolve against the rule subject. Set per rule by the checker.
    private ObjectType currentSubject;
    // §11 the current rule's "Daarbij geldt:" variabelen, keyed by normalized name -> the resolved definition Expr,
    // INLINED at each read site (nameRef). Per-rule; cleared and re-set by setVariables each rule. This is the
    // form a VOORWAARDE read takes — see setVariables for why the two forms coexist.
    private final Map<String, Expr> variables = new LinkedHashMap<>();
    // §11 the SAME variabelen in BINDER form, in declaration order (§11.1 rule 1): the value a ch. 4.10 Let node
    // carries, in which a read of an EARLIER variable is a BinderRef rather than that variable's inlined subtree.
    private final Map<String, Variable> binders = new LinkedHashMap<>();
    // Which of the two forms nameRef hands back. false (the default) inlines — the VOORWAARDE route; true yields a
    // BinderRef — the RESULTAAT route, under the Let chain wrapWithVariables builds. Scoped by buildResult,
    // and turned back OFF inside a structure-sensitive position by inlining().
    private boolean binderMode;
    // The inlined definitions nameRef may substitute RIGHT NOW. It is `variables` itself everywhere a rule's own
    // text is built (the voorwaarde and the resultaat see every declaration, as they did in v0), and a growing
    // PREFIX while pass 2 rebuilds the definitions themselves — so a structure-sensitive position INSIDE a
    // definition sees exactly what pass 1 saw there: earlier declarations only, a forward reference still unbound.
    private Map<String, Expr> visible = variables;

    /** One §13.4.2.10 declared variable in BINDER form: its lookup {@code key}, the {@code name} the Let binder
     *  and every BinderRef carry (the written name minus a leading bepaald lidwoord), the resolved value, and the
     *  declaration's source anchor. */
    record Variable(String key, String name, Expr value, Source source) {}

    ExpressionBuilder(RegelSpraakModel model) { this.model = model; }

    void setSubject(ObjectType subject) { this.currentSubject = subject; }
    ObjectType subject() { return currentSubject; }

    /** §5.6: the die/dat subselecties this builder CONSUMED — by identity, over the whole document.
     *  Consuming means the predicaat reached a resolution that accounts for it: a bound filter, or a
     *  refusal that names it. Everything the parse produced and this set does not hold was DROPPED, and
     *  {@link RuleChecker} makes that loud rather than letting the document answer about a set nobody
     *  wrote. */
    private final java.util.Set<PredicaatContext> consumed =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    java.util.Set<PredicaatContext> consumedPartSelections() { return consumed; }

    /** §13.4.13.4 resolves a COMPOUND subselectie predicaat ("die aan alle volgende voorwaarden voldoen: …")
     *  against a given AGGREGATED ELEMENT type, producing a {@link Condition.Compound} whose bullets are bound
     *  to that element (not the rule subject). Implemented by {@link RuleChecker} (which owns the bullet machinery +
     *  the bouwer subject); null until set, so the compound stays an honest marker if unwired. */
    @FunctionalInterface
    interface CompoundFilterResolver { Condition resolve(SamengesteldPredicaatContext sp, ObjectType elem); }
    private CompoundFilterResolver compoundFilter;
    void setCompoundFilter(CompoundFilterResolver r) { this.compoundFilter = r; }

    /**
     * A filter being built (grammar/DIVERGENCES.md, "The rule subject in a subselection — zelf"): the subject the fold
     * or quantifier is written from ({@code outer}), its element type, and the filter as written. A filter is read per
     * element, with the element as its subject (RUNTIME-SEMANTICS ch. 3.4a, 3.9a); a read that belongs to the subject of
     * the form is built at that subject and carries the form anchor (IR-SPEC ch. 4.1a, WF-122).
     */
    record Frame(ObjectType outer, ObjectType element, Source filter) {}

    /** The filters being built, innermost first. */
    private final java.util.Deque<Frame> frames = new java.util.ArrayDeque<>();

    /** Where a subselection was built: its element and the filters around it, innermost first, so the type check of its
     *  conditions reads them as the filter did ({@link #inFilterScope}). */
    private record Scope(ObjectType element, List<Frame> frames) {}
    private final Map<PredicaatContext, Scope> scopes = new java.util.IdentityHashMap<>();

    /** The reads written with "zelf" and the variables of the rule, by identity: they name the subject of the form
     *  explicitly, so the test of a quantifier over elements of its own type may read them (RGS-349 refuses the rest). */
    private final java.util.Set<Expr> explicitSubjectReads =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** The anchor a bare reference written as an operand ("de Vergunning zelf", "de Schuit") was built from, by identity. */
    private final Map<Expr, Anchor> writtenAnchors = new java.util.IdentityHashMap<>();

    /** The anchor the operand {@code e} was written with where it is a bare reference, or null. */
    Anchor writtenAnchor(Expr e) { return e == null ? null : writtenAnchors.get(e); }

    /** Whether {@code e} names the subject of the form explicitly: written with "zelf", or a variable of the rule. */
    boolean namesFormSubject(Expr e) { return explicitSubjectReads.contains(e); }

    /** The subject of the form: the subject the outermost filter is written from, or the current subject outside one. */
    ObjectType formSubject() { return frames.isEmpty() ? currentSubject : frames.peekLast().outer(); }

    /** Whether a filter is being built with its element as the current subject: a per-element context. */
    boolean inPerElementContext() { return !frames.isEmpty() && frames.peek().element() == currentSubject; }

    /** {@code build} with {@code frame} around it, the filter of subselection {@code p} over {@code elem}; without a frame
     *  (a fold without a subject) as before. */
    private <T> T inFilter(Frame frame, PredicaatContext p, ObjectType elem, java.util.function.Supplier<T> build) {
        if (frame != null) frames.push(frame);
        scopes.put(p, new Scope(elem, List.copyOf(frames)));
        try {
            return build.get();
        } finally {
            if (frame != null) frames.pop();
        }
    }

    /** {@code check} with the subject and the filters subselection {@code p} was built with; as it is where {@code p} was
     *  built by no fold. The type check of a rule's conditions reads a subselection's conditions as its filter did, so it
     *  says what the filter means, and no diagnostic of a read on the wrong subject survives it. */
    void inFilterScope(PredicaatContext p, Runnable check) {
        Scope scope = scopes.get(p);
        if (scope == null) { check.run(); return; }
        ObjectType previous = currentSubject;
        List<Frame> saved = List.copyOf(frames);
        currentSubject = scope.element();
        frames.clear();
        frames.addAll(scope.frames());
        try {
            check.run();
        } finally {
            currentSubject = previous;
            frames.clear();
            frames.addAll(saved);
        }
    }

    /** {@code build} at the subject of the form, outside every filter: what a node with the form anchor reads, as if it
     *  stood in the form's own condition or value (IR-SPEC ch. 4.1a). */
    <T> T atForm(java.util.function.Supplier<T> build) {
        ObjectType previous = currentSubject;
        List<Frame> saved = List.copyOf(frames);
        currentSubject = formSubject();
        frames.clear();
        try {
            return build.get();
        } finally {
            currentSubject = previous;
            frames.clear();
            frames.addAll(saved);
        }
    }

    /** The innermost written reference of a chain — the anchor its walk starts from — and whether it carries "zelf". */
    record Anchor(String name, boolean self) {}

    /** The anchor of {@code o}'s chain: the last segment of its innermost definite reference ("de Schuit" in "alle
     *  klachten van de Schuit"), or null where the chain ends in none (a possessive, an indefinite subject, a
     *  population). */
    static Anchor anchorOf(OnderwerpContext o) {
        OnderwerpContext rest = o;
        while (rest != null) {
            if (rest.onderwerp() != null) { rest = rest.onderwerp(); continue; }
            // a greedy name runs on over an inner "van": "alle klachten van de Schuit" can be one name, anchored at its
            // last segment; a set without one names a population
            if (rest.kaleNaam() != null) {
                String name = src(rest.kaleNaam());
                return isSetSubject(rest) && name.split("\\s+van\\s+").length < 2 ? null : new Anchor(lastSegment(name), false);
            }
            OnderwerpKetenContext k = rest.onderwerpKeten();
            if (k == null) return null;
            if (k.selector() != null && k.onderwerp() != null) { rest = k.onderwerp(); continue; }
            if (k.onderwerpVerwijzing() != null && k.onderwerpVerwijzing().kaleNaam() != null)
                return new Anchor(lastSegment(src(k.onderwerpVerwijzing().kaleNaam())), k.onderwerpVerwijzing().ZELF() != null);
            return null;
        }
        return null;
    }

    /** The last "van"-segment of a written name, without its article: the name a greedy name ran on to. */
    private static String lastSegment(String name) {
        String[] parts = name.split("\\s+van\\s+");
        return stripArticle(parts[parts.length - 1]).trim();
    }

    /** What a written reference reaches in the context it is built in. */
    enum Reach {
        /** the current subject, or what a walk from it reaches: built as written */
        ELEMENT,
        /** the subject of the form, from a filter or a quantifier test: built there, with the anchor */
        FORM,
        /** the bare name of a type that the subject of the form and an element both have (RGS-350) */
        AMBIGUOUS,
        /** the bare name of the element of an enclosing subselection, which the anchor does not reach (B6) */
        INTERMEDIATE,
        /** "zelf" after a name that is not the name of the type of the form's subject */
        SELF_ELSEWHERE
    }

    /** What {@code a} reaches here. "zelf" names the subject of the form wherever it stands, and is only admitted after
     *  that type's own name. In a filter, the bare own name of the form subject's type names that subject unless an
     *  element has the type as well; the bare name of an enclosing element's type names nothing the IR can reach. A
     *  role name or a name in another case is no type name here: it keeps the walk from the element. */
    Reach reach(Anchor a) {
        if (a == null) return Reach.ELEMENT;
        ObjectType form = formSubject();
        ObjectType named = model.objectTypes.get(a.name());
        if (a.self()) return named != null && named == form ? Reach.FORM : Reach.SELF_ELSEWHERE;
        if (named == null || !inPerElementContext()) return Reach.ELEMENT;
        boolean element = false, enclosing = false;
        int depth = 0;
        for (Frame f : frames) {
            if (f.element() == named) { if (depth == 0) element = true; else enclosing = true; }
            depth++;
        }
        if (named == form) return element || enclosing ? Reach.AMBIGUOUS : Reach.FORM;
        return enclosing && !element ? Reach.INTERMEDIATE : Reach.ELEMENT;
    }

    /** The refusal of a read {@code reach} does not let this frontend write, with its marker; null for the readings it
     *  writes. A {@code characteristic} check is told the way out in the words of a check. */
    Expr refusedReach(Reach reach, Anchor a, Object read, Datatype type, Source source, boolean characteristic) {
        switch (reach) {
            case AMBIGUOUS -> {
                String named = model.objectTypes.get(a.name()).definite();
                Msg escape = characteristic ? Msg.frontend("PART.subject-escape-characteristic", "named", named)
                        : Msg.frontend("PART.subject-escape-value", "named", named);
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR, "RGS-350",
                        params("escape", escape, "filter", frames.peek().filter(), "read", read, "type", a.name()),
                        source.line()));
            }
            case INTERMEDIATE -> {
                Frame enclosing = null;
                for (Frame f : frames) if (f.element() == model.objectTypes.get(a.name())) enclosing = f;
                refuseRuleSubject(read, Msg.frontend("PART.subject-intermediate-element", "type", a.name(),
                        "filter", enclosing == null ? "" : enclosing.filter()), source);
            }
            case SELF_ELSEWHERE -> refuseRuleSubject(read, selfElsewhere(a.name()), source);
            default -> { return null; }
        }
        return new Expr.Unresolved(type, source, null);   // the ERROR above is this marker's diagnosis
    }

    /** Why "zelf" after {@code name} names nothing: it names the subject of the form, and only after its type's name. */
    Msg selfElsewhere(String name) {
        ObjectType form = formSubject();
        return Msg.frontend("PART.subject-self-elsewhere", "name", name, "subject", form == null ? "?" : form.name);
    }

    /** The refusal of a read of the subject of the form that this frontend cannot write (RGS-358). */
    void refuseRuleSubject(Object read, Msg reason, Source source) {
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR, "RGS-358",
                params("read", read, "reason", reason), source.line()));
    }

    /** In a filter or a test, the refusal of a quantifier whose member phrase {@code e} names the subject of the form, or a
     *  name the IR cannot reach from there (RGS-350, RGS-358), with true; false where it names neither. The anchor of a
     *  member phrase runs on over the words of the predicate after it, so its longest opening type name counts. */
    boolean refusesQuantifiedRange(ExpressieContext e, Source source) {
        PrimaireExpressieContext p = e instanceof PrimaireExprContext pe ? pe.primaireExpressie() : null;
        AttribuutVanOnderwerpContext a = p == null ? null : p.attribuutVanOnderwerp();
        Anchor written = a == null || a.onderwerp() == null ? null : anchorOf(a.onderwerp());
        if (written == null) return false;
        String[] words = written.name().split("\\s+");
        Anchor anchor = written;
        for (int n = words.length; n >= 1; n--) {
            String prefix = String.join(" ", java.util.Arrays.copyOfRange(words, 0, n));
            if (model.objectTypes.get(prefix) != null) { anchor = new Anchor(prefix, written.self()); break; }
        }
        Reach reach = reach(anchor);
        if (reach == Reach.ELEMENT || reach == Reach.FORM && !inPerElementContext()) return false;
        if (reach == Reach.FORM) refuseRuleSubject(source(a), Msg.frontend("PART.subject-quantifier-in-filter"), source);
        else refusedReach(reach, anchor, source(a), unknown(), source, false);
        return true;
    }

    /** What {@link #anchor} gives: the expression with the form anchor, or why it has no such spelling. */
    record Anchored(Expr expr, Msg refusal) {}

    /**
     * {@code e}, built at the subject of the form, read where the subject is an element (IR-SPEC ch. 4.1a): every read
     * and every fold of {@code e} that starts at the subject carries the form anchor; a fold's own filter reads its
     * element, and its fallback is read where the fold stands, so neither is touched. The nodes are new, so a shared
     * definition keeps its own reading where it stands outside a filter.
     */
    Anchored anchor(Expr e) {
        return anchor(e, null);
    }

    /** {@link #anchor}, where an own read of {@code element} — the type of the elements, another than the form subject's —
     *  names the element and stays as it is: the operand beside the quantified one, whose read of the element by its
     *  type name the matrix takes from the element (RUNTIME-SEMANTICS ch. 3.9a). */
    Anchored anchor(Expr e, ObjectType element) {
        Msg[] why = new Msg[1];
        Expr result = anchor(e, element, why);
        return why[0] != null ? new Anchored(null, why[0]) : new Anchored(result, null);
    }

    private Expr anchor(Expr e, ObjectType element, Msg[] why) {
        if (e == null || why[0] != null) return e;
        java.util.function.UnaryOperator<Expr> again = x -> anchor(x, element, why);
        switch (e) {
            case Expr.AttributeReference a -> {
                boolean elementRead = element != null && a.objectType() == element && a.navigationRole() == null
                        && a.navigationPath().isEmpty() && !a.set();
                return a.anchored() || elementRead ? a : new Expr.AttributeReference(a.attribute(), a.objectType(), a.subjectSource(),
                        a.navigationRole(), a.type(), a.source(), a.set(), a.navigationPath(), a.rangePath(), a.quantifier(),
                        a.threshold(), true);
            }
            case Expr.FunctionCall f -> {
                if (f.anchored()) return f;
                if (dev.uchar.regelspraak.emit.IrEmitter.isFoldSource(f))
                    return new Expr.FunctionCall(f.kind(), f.arguments(), f.elementObjectType(), f.emptyFallback(), f.filter(),
                            f.dimensionSelection(), f.type(), f.source(), f.granularity(), true);
                // a fold over a dimension or of another shape, and the operators over the subject's own timeline, read
                // the subject without a node the anchor can stand on
                if (f.dimensionSelection() != null || f.elementObjectType() != null || f.filter() != null
                        || f.kind() == Expr.FunctionKind.TOTAAL || f.kind() == Expr.FunctionKind.TIJDSEVENREDIG_DEEL
                        || f.kind() == Expr.FunctionKind.AANTAL_DAGEN_IN) {
                    why[0] = Msg.frontend("PART.subject-no-anchor", "expression", f.source());
                    return e;
                }
                List<Expr> arguments = new ArrayList<>();
                boolean changed = false;
                for (Expr x : f.arguments()) {
                    Expr moved = again.apply(x);
                    changed |= moved != x;
                    arguments.add(moved);
                }
                Expr fallback = again.apply(f.emptyFallback());
                return !changed && fallback == f.emptyFallback() ? f
                        : new Expr.FunctionCall(f.kind(), List.copyOf(arguments), f.elementObjectType(), fallback, f.filter(),
                                f.dimensionSelection(), f.type(), f.source(), f.granularity());
            }
            case Expr.Binary b -> {
                Expr l = again.apply(b.left()), r = again.apply(b.right());
                return l == b.left() && r == b.right() ? b : new Expr.Binary(b.op(), l, r, b.type(), b.source());
            }
            case Expr.Rounding r -> {
                Expr base = again.apply(r.base());
                return base == r.base() ? r : new Expr.Rounding(base, r.mode(), r.decimals(), r.source());
            }
            case Expr.Clamp c -> {
                Expr base = again.apply(c.base()), min = again.apply(c.minimum()), max = again.apply(c.maximum());
                return base == c.base() && min == c.minimum() && max == c.maximum() ? c
                        : new Expr.Clamp(base, min, max, c.source());
            }
            case Expr.TextComposition t -> {
                List<Expr.TextComposition.Part> parts = new ArrayList<>();
                boolean changed = false;
                for (Expr.TextComposition.Part part : t.parts()) {
                    if (part instanceof Expr.TextComposition.EmbeddedExpression x) {
                        Expr moved = again.apply(x.expr());
                        changed |= moved != x.expr();
                        parts.add(moved == x.expr() ? part : new Expr.TextComposition.EmbeddedExpression(moved));
                    } else parts.add(part);
                }
                return changed ? new Expr.TextComposition(List.copyOf(parts), t.source()) : t;
            }
            // the anchor stands on the read inside the cell (IR-SPEC ch. 4.1a)
            case Expr.DimensionCellRead d -> {
                Expr moved = again.apply(d.attribute());
                return moved == d.attribute() || !(moved instanceof Expr.AttributeReference a) ? d
                        : new Expr.DimensionCellRead(a, d.labels(), d.source());
            }
            case Expr.Let l -> {
                Expr value = again.apply(l.value()), body = again.apply(l.body());
                return value == l.value() && body == l.body() ? l : new Expr.Let(l.binder(), value, body, l.type(), l.source());
            }
            // the two walk starters without an anchor (IR-SPEC ch. 4.1a), and the node no RegelSpraak sentence builds
            case Expr.InstanceSetUnion u -> { why[0] = Msg.frontend("PART.subject-no-anchor", "expression", u.source()); return e; }
            case Expr.IfThenElse k -> { why[0] = Msg.frontend("PART.subject-no-anchor", "expression", k.source()); return e; }
            case Expr.Literal x -> { return e; }
            case Expr.ParameterRef x -> { return e; }
            case Expr.VariableRef x -> { return e; }
            case Expr.BinderRef x -> { return e; }
            case Expr.Constant x -> { return e; }
            case Expr.Unresolved x -> { return e; }
        }
    }

    /** The kinds that fold a set of instances or values over one written source. */
    private static boolean folds(Expr.FunctionKind kind) {
        return switch (kind) {
            case AANTAL, SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE, AANTAL_GEVULDE -> true;
            default -> false;
        };
    }

    /** Whether {@code e} reads anything of a subject: a read, a fold, a union or a choice. */
    static boolean readsSubject(Expr e) {
        return Expr.contains(e, x -> x instanceof Expr.AttributeReference || x instanceof Expr.FunctionCall f
                && (f.elementObjectType() != null || f.dimensionSelection() != null)
                || x instanceof Expr.InstanceSetUnion || x instanceof Expr.IfThenElse);
    }

    /**
     * A fold whose written range is anchored at the subject of the form, in a filter or a quantifier test ("de laatste
     * van de einddag van alle klachten van de Schuit" in the filter of a fold over the ladingen of a Schuit): built at
     * that subject, as written, with the form anchor. The refusal of a range anchored at a name the IR cannot reach
     * from there, with its marker. Null where the range is anchored at the element or at nothing a filter names, which
     * the walk from the element decides as before; at the form's own level a range anchored with "zelf" is built as
     * written, and named as explicit.
     */
    private Expr foldAtForm(FunctieAanroepContext f) {
        // a set source writes its elements and then its anchor; a count source names its elements as a member of the
        // anchor ("het aantal klachten van de Schuit"), so its onderwerp is the anchor alone
        OnderwerpContext o = foldSourceSubject(f);
        if (o == null && f instanceof AantalFuncContext cf && cf.primaireExpressie().size() == 1
                && cf.primaireExpressie(0).attribuutVanOnderwerp() != null)
            o = cf.primaireExpressie(0).attribuutVanOnderwerp().onderwerp();
        Anchor a = o == null ? null : anchorOf(o);
        Reach reach = reach(a);
        Expr refused = refusedReach(reach, a, source(f), unknown(), source(f), false);
        if (refused != null) return refused;
        if (reach != Reach.FORM) return null;
        if (!inPerElementContext()) {
            Expr built = buildFold(f);
            explicitSubjectReads.add(built);
            return built;
        }
        Expr built = atForm(() -> buildFold(f));
        if (built instanceof Expr.Unresolved) return built;
        Anchored x = anchor(built);
        if (x.refusal() == null) return x.expr();
        refuseRuleSubject(source(f), x.refusal(), source(f));
        return new Expr.Unresolved(built.type(), source(f), null);   // the ERROR above is this marker's diagnosis
    }

    /**
     * A variable of the rule read in a filter or a quantifier test: its definition was built at the subject of the form,
     * so it is read there, with the form anchor on every read and fold of it that starts at the subject (IR-SPEC ch.
     * 4.10 (c)). Outside a filter, and in a filter built at another subject than the variables', the definition stands as
     * built.
     */
    private Expr variableInFilter(String name, Expr definition, Source source) {
        if (!inPerElementContext() || variablesSubject == null || variablesSubject != formSubject()
                || !readsSubject(definition)) return definition;
        Anchored x = anchor(definition);
        if (x.refusal() == null) return x.expr();
        refuseRuleSubject(Msg.frontend("PART.variable-read", "name", name, "definition", definition.source()), x.refusal(),
                source);
        return new Expr.Unresolved(definition.type(), source, null);   // the ERROR above is this marker's diagnosis
    }

    /** The subject the current rule's variables were built at: their reads belong to it. */
    private ObjectType variablesSubject;

    /** The text of {@code ctx} without the "zelf" of its references: the name the reference is resolved by. A
     *  subselection keeps its own words. */
    static String withoutSelf(ParserRuleContext ctx) {
        List<org.antlr.v4.runtime.Token> self = new ArrayList<>();
        collectSelf(ctx, self);
        if (self.isEmpty() || ctx.getStart() == null || ctx.getStop() == null) return src(ctx);
        org.antlr.v4.runtime.CharStream in = ctx.getStart().getInputStream();
        StringBuilder text = new StringBuilder();
        int from = ctx.getStart().getStartIndex();
        for (org.antlr.v4.runtime.Token t : self) {
            if (t.getStartIndex() > from) text.append(in.getText(org.antlr.v4.runtime.misc.Interval.of(from, t.getStartIndex() - 1)));
            from = t.getStopIndex() + 1;
        }
        if (ctx.getStop().getStopIndex() >= from)
            text.append(in.getText(org.antlr.v4.runtime.misc.Interval.of(from, ctx.getStop().getStopIndex())));
        return text.toString().replaceAll("\\s+", " ").trim();
    }

    private static void collectSelf(ParseTree t, List<org.antlr.v4.runtime.Token> out) {
        if (t instanceof OnderwerpVerwijzingContext v && v.ZELF() != null) out.add(v.ZELF().getSymbol());
        if (t instanceof PredicaatContext) return;
        for (int i = 0; i < t.getChildCount(); i++) collectSelf(t.getChild(i), out);
    }

    /**
     * §11 capture the "Daarbij geldt: [lidwoord] X is &lt;expressie&gt;" variabelendeel of the current rule, in BOTH
     * forms the wire needs. CLEARS first (so a previous rule's variables never leak — call it for EVERY rule, even
     * one with no variabeleDeel: a null ctx just clears). Subject-relative "zijn X" inside a definition resolves
     * against the subject, so {@link #setSubject} must be called BEFORE this.
     *
     * <p><b>Why two forms.</b> A rule's voorwaarde and its waarde are two SEPARATE wire fields
     * ({@code ResolvedRule.condition} is a {@link Condition}, {@code .value} an {@link Expr}), and a ch. 4.10
     * {@code Let} is an Expr node — so no Let can span them. A variable read in the RESULT part therefore becomes a
     * {@link Expr.BinderRef} under the Let chain {@link #wrapWithVariables} builds (sharing survives: two reads,
     * one Let); a variable read in the VOORWAARDE keeps the v0 route and is SUBSTITUTED at its use site. What that
     * costs is SHARING: a variable read twice inside one condition is substituted twice and so evaluated twice,
     * where one Let would evaluate it once (ch. 10.6 clause 1). No case observes the difference — the values agree
     * — but they are not identical in theory: a definition whose evaluation deposits a §9.5 channel reason would
     * deposit one PER READ. The leeg-barrier a Let would raise at the read place is no loss either way: it
     * coincides with the §5 leeg-operand discipline the comparison already applies.
     *
     * <p><b>Pass 1</b> resolves each definition in declaration order (§11.1 rule 1) with earlier variables INLINED
     * — byte-for-byte the v0 behaviour, and the map {@link #nameRef} reads on the voorwaarde route. <b>Pass 2</b>
     * re-resolves the SAME sources with earlier variables as BinderRefs. Two passes rather than one plus a
     * substitution rewrite: the second pass reuses the whole resolution machinery instead of duplicating it, and a
     * forward/cyclic reference falls through identically in both because {@link #binders} fills incrementally.
     */
    void setVariables(VariabeleDeelContext ctx) {
        variables.clear();
        binders.clear();
        variablesSubject = currentSubject;
        if (ctx == null) return;
        int beforeInline = model.diagnostics.size();
        for (VariabeleOnderdeelContext vo : ctx.variabeleOnderdeel()) {
            if (vo.variabeleNaam() == null || vo.expressie() == null) continue;
            variables.put(variableKey(src(vo.variabeleNaam())), build(vo.expressie()));
        }
        int afterInline = model.diagnostics.size();
        boolean preceding = binderMode;
        Map<String, Expr> previousVisible = visible;
        // Pass 2 sees the same growing PREFIX of inlined definitions pass 1 saw, so a structure-sensitive position
        // inside definition i substitutes exactly the earlier definitions (never a forward one, which stays unbound
        // in both passes) — the two passes then differ ONLY in what nameRef hands back, never in what it can see.
        Map<String, Expr> prefix = new LinkedHashMap<>();
        binderMode = true;
        visible = prefix;
        try {
            for (VariabeleOnderdeelContext vo : ctx.variabeleOnderdeel()) {
                if (vo.variabeleNaam() == null || vo.expressie() == null) continue;
                String written = src(vo.variabeleNaam());
                String key = variableKey(written);
                binders.put(key, new Variable(key, binderName(written), build(vo.expressie()), source(vo)));
                prefix.put(key, variables.get(key));
            }
        } finally {
            binderMode = preceding;
            visible = previousVisible;
        }
        // Pass 2 re-resolves the same source text, so a diagnostic it repeats VERBATIM is pass 1's own — report it
        // once. A diagnostic pass 2 raises that pass 1 did not is a REAL difference (a BinderRef is opaque where
        // the inlined subtree was transparent) and is KEPT, never swallowed.
        List<RegelSpraakModel.Diagnostic> inlineMessages = new ArrayList<>(model.diagnostics.subList(beforeInline, afterInline));
        for (int i = model.diagnostics.size() - 1; i >= afterInline; i--)
            if (inlineMessages.contains(model.diagnostics.get(i))) model.diagnostics.remove(i);
    }

    /**
     * §11 wrap a rule's RESULT-part value in the Let chain its variabelendeel denotes: one ch. 4.10 {@link Expr.Let}
     * per declared variable, nested in DECLARATION order (§11.1 rule 1 — the first-declared variable is the
     * OUTERMOST Let, so a later definition's BinderRef to an earlier one is in scope), each carrying that variable's
     * binder-form value and typed with its body (WF-076).
     *
     * <p><b>Full lowering, no dead Lets</b> (maintainer decision 5, as reconciled with the wire): a variable the
     * result reads is ALWAYS a Let — at one read as much as at five, so nothing is inlined away and sharing is
     * visible. A variable NO read reaches — the voorwaarde-only variable (substituted there instead) and the
     * declared-but-unused one — gets none: under ch. 10.6 an unread binder evaluates nothing, so such a Let is a
     * guaranteed no-op that would only pollute the wire. The test is TRANSITIVE by construction: the chain is built
     * inside-out, so a variable read only by a LATER variable's value is still wrapped, because that later Let is
     * already part of the body when this one is considered.
     */
    Expr wrapWithVariables(Expr body) {
        if (body == null || binders.isEmpty()) return body;
        List<Variable> vs = new ArrayList<>(binders.values());
        Expr result = body;
        for (int i = vs.size() - 1; i >= 0; i--) {
            Variable v = vs.get(i);
            if (!readsBinder(result, v.key())) continue;
            result = new Expr.Let(v.name(), v.value(), result, result.type(), v.source());
        }
        return result;
    }

    /** §11/§13.4.2 build a rule's RESULT-part value: the expressie resolved with every variable read as a
     *  {@link Expr.BinderRef}, wrapped in {@link #wrapWithVariables}'s Let chain. A rule with no variabelendeel
     *  gets back exactly what {@link #build} produces — the binder modus is inert without declared variables. */
    Expr buildResult(ExpressieContext e) {
        boolean preceding = binderMode;
        binderMode = true;
        try {
            return wrapWithVariables(build(e));
        } finally {
            binderMode = preceding;
        }
    }

    /**
     * §11/ch. 4.10 build {@code f} as a SHAPE-READING CONSTRUCT: one where the resolver decides on the SHAPE of the
     * resolved subtree, not on its value. A variable read there is SUBSTITUTED (the v0 route), never handed back
     * as a {@link Expr.BinderRef} — a BinderRef is an opaque leaf, so every {@code instanceof Expr.AttributeReference} /
     * {@code Literal} / {@code ParameterRef} test would fail and the construct would silently degrade to an
     * Onopgelost marker (a wrong answer, not a lost optimisation). This is the THIRD lowering boundary, beside the
     * voorwaarde-only variable and the never-read one; it is normative in {@code IR-SPEC.md} ch. 4.10.
     *
     * <p>The boundary is the WHOLE SUBTREE, not the shape-reading positions inside it: the scope stays off until
     * the construct is built, so a pure VALUE position underneath — a §5.8.2 leegwaarde fallback, say — is
     * substituted too, though it decides nothing about shape. One scope, no half-inlined subtree. The consequence
     * is normative: inside an aggregation subtree the v0 inlining-coalesce still governs, so the ch. 10.6 tracked
     * delta 1 (the leeg-barrier on the variabelendeel) speaks only about reads OUTSIDE such a subtree.
     */
    private <T> T inlining(java.util.function.Supplier<T> f) {
        boolean preceding = binderMode;
        binderMode = false;
        try { return f.get(); } finally { binderMode = preceding; }
    }

    /** The functie kinds that range over a SET or a TIMELINE and therefore read their operand's shape: the eight
     *  instance/dimension aggregations, plus the two §7 temporal operators whose operand must be a single-period
     *  temporal attribuut ({@code TIJDSEVENREDIG_DEEL}) or whose "dat …" condition clips one
     *  ({@code AANTAL_DAGEN_IN}). {@link #inlining} covers exactly these. */
    private static boolean aggregates(Expr.FunctionKind kind) {
        return switch (kind) {
            case AANTAL, SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE, AANTAL_GEVULDE,
                 TIJDSEVENREDIG_DEEL, AANTAL_DAGEN_IN -> true;
            default -> false;
        };
    }

    /** Whether {@code e} reads the binder keyed {@code key} anywhere — including inside a nested Let's value,
     *  which {@link Expr#contains} descends into. Variable names are unique per rule (both maps are keyed on the
     *  normalized name), so no inner binder can shadow an outer one here. */
    private static boolean readsBinder(Expr e, String key) {
        return Expr.contains(e, x -> x instanceof Expr.BinderRef b && key.equals(variableKey(b.name())));
    }

    /** Normalize a variabele name to its lookup key: strip a leading bepaald lidwoord (§11 "Mogelijkheid van
     *  lidwoord bij naam variabele"), lowercase, collapse whitespace — so "het Lage tarief" and "lage  tarief"
     *  map to the same variable. Mirrors {@link #nameRef}'s parameter-name strip. */
    private static String variableKey(String name) {
        return name.replaceFirst("^(?i)(de|het|een)\\s+", "").trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /** The name a Let binder and its BinderRefs carry on the wire: the DECLARED spelling minus a leading bepaald
     *  lidwoord (§11), case preserved — "het Lage tarief" binds "Lage tarief". Distinct from {@link
     *  #variableKey}, which lowercases for lookup only; the wire keeps what the author wrote. */
    private static String binderName(String name) {
        return name.replaceFirst("^(?i)(de|het|een)\\s+", "").trim().replaceAll("\\s+", " ");
    }

    // ---------------- §13.4.16/.17 expressie ----------------
    Expr build(ExpressieContext e) {
        if (e == null) return unresolvedMarker(e, Msg.frontend("RGS-092.no-expression"));
        Source source = source(e);
        if (e instanceof PrimaireExprContext p) return buildPrimary(p.primaireExpressie());
        if (e instanceof SomExprContext s) {
            Expr.BinOp op = s.PLUS() != null ? Expr.BinOp.PLUS
                    : s.MIN() != null ? Expr.BinOp.MIN : Expr.BinOp.VERMINDERD_MET;
            return binary(op, s.expressie(0), s.expressie(1), source, s.getStart().getLine());
        }
        if (e instanceof ProductExprContext s) {
            Expr.BinOp op = s.MAAL() != null ? Expr.BinOp.MAAL
                    : s.GEDEELD_DOOR_ABS() != null ? Expr.BinOp.GEDEELD_DOOR_ABS : Expr.BinOp.GEDEELD_DOOR;
            return binary(op, s.expressie(0), s.expressie(1), source, s.getStart().getLine());
        }
        if (e instanceof MachtExprContext s) {
            // §13.4.17.14: "X tot de macht Y <afronding>" — the exponent Y is a primaireExpressie
            // and the mandatory afronding rounds the POWER (it is a sibling of Y, not inside it).
            Expr base = binaryExpr(Expr.BinOp.TOT_DE_MACHT, build(s.expressie()), buildPrimary(s.primaireExpressie()), source, s.getStart().getLine());
            return rounded(base, s.afronding(), source);
        }
        if (e instanceof PercentageVanExprContext s) {
            // §4.8 Tabel 18: "X% van Y" carries Y's UNIT. But §4.8 Precisie makes the result an
            // exact fraction with "geen decimale precisie" of its own (the §4.5 gedeeld door rule),
            // so do NOT propagate Y's declared decimal count: "50% van prijs (2 dec)" is an exact
            // whole or fraction, and asserting 2 decimals would trip the §6.1.2 assignment check
            // against a lower-precision target. Keep the unit, drop the decimal claim.
            Expr left = build(s.expressie(0)), right = build(s.expressie(1));
            Datatype t = right.type() instanceof Datatype.Numeric n
                    ? new Datatype.Numeric(false, 0, Datatype.Sign.ONBEPAALD, n.unit()) : unknown();
            return new Expr.Binary(Expr.BinOp.PERCENTAGE_VAN, left, right, t, source);
        }
        if (e instanceof AfgerondeExprContext s) return rounded(build(s.expressie()), s.afronding(), source);
        if (e instanceof BegrensdeExprContext s) {
            Expr base = build(s.expressie());
            Expr[] mm = bounds(s.begrenzing());
            // §6.1.4: each bound must be order-comparable with the bounded value (same numeric
            // family, unit-convertible) — else the clamp is meaningless and lowers to invalid DRL.
            // the base is one position: a non-numeric base is reported once, as the verifier reports it
            Typing.Coded baseError = Typing.clampBase(base.type());
            if (baseError != null)
                model.diagnostics.add(baseError.at(RegelSpraakModel.Severity.ERROR, s.getStart().getLine()));
            else {
                if (mm[0] != null) reportClamp(base, mm[0], "minimum", s);
                if (mm[1] != null) reportClamp(base, mm[1], "maximum", s);
            }
            return new Expr.Clamp(base, mm[0], mm[1], source);
        }
        // GEEN GETUIGE: expressie heeft zeven gelabelde takken en alle zeven staan hierboven, dus geen
        // enkele ontleding — ook geen herstelde — bereikt deze regel. Gemeten 2026-09-19.
        return unresolvedMarker(e, Msg.frontend("RGS-092.expression-form"));
    }

    /** Arithmetic node whose result datatype is taken from the typeringen matrix
     *  (a type error there yields Onbekend, matching the checker's no-claim path). A date plus or min a duration is
     *  judged here, not in {@link #binaryExpr}, because its refusal quotes both operands as the author wrote them. */
    private Expr binary(Expr.BinOp op, ExpressieContext l, ExpressieContext r, Source source, int line) {
        Expr left = build(l), right = build(r);
        Expr result = binaryExpr(op, left, right, source, line);
        if ((op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN) && result.type() != null && result.type().isDate()) {
            Typing.Coded error = brokenDurationError(op, left, right, source, src(l), src(r));
            if (error != null) {
                // a re-resolution of the same text (a definition's two passes, a re-read condition) is one defect
                var d = error.at(RegelSpraakModel.Severity.ERROR, line);
                if (!model.diagnostics.contains(d)) model.diagnostics.add(d);
            }
        }
        return result;
    }

    /** RUNTIME-SEMANTICS ch. 3.8: every executor truncates a fractional duration count toward zero before the
     *  calendar step. A sentence that adds or subtracts a duration whose count can carry a fraction therefore says
     *  more than the executors do — "zijn startdag plus 20,5 mnd" lands half a month short — while ref, the ASP
     *  backend and the Drools branches agree on the truncated date. The wire keeps the ch. 3.8 meaning (a hand-written document with a fractional count is
     *  well-formed and pinned), so the refusal is this producer's own: the duration and its unit as written, the
     *  truncation, and the forms that say what is meant (grammar/DIVERGENCES.md, "A date plus or min a duration
     *  that can carry a fraction"). Null when the count is whole by its form. */
    private Typing.Coded brokenDurationError(Expr.BinOp op, Expr left, Expr right, Source source, String writtenLeft,
                                    String writtenRight) {
        Rational b = rational(right);
        if (b == null) return null;
        String onName = op == Expr.BinOp.PLUS ? "plus" : "min";
        String duration = withoutOutsideParentheses(writtenRight);
        String unit = right.type().unitOf().rendering();
        Object cause = b.site() == right ? b.reason()
                : Msg.frontend("RGS-058.site-cause", "site", b.site().source(), "reason", b.reason());
        Msg example, truncate;
        if (b.site() == right && right instanceof Expr.Literal l) {
            String whole = truncated(l, duration);
            example = Msg.frontend("RGS-058.example-literal", "duration", duration, "whole", whole);
            truncate = Msg.frontend("RGS-058.truncate-literal", "left", writtenLeft, "operation", onName, "whole", whole);
        } else {
            example = Msg.frontend("RGS-058.example-fraction", "unit", unit);
            truncate = Msg.frontend("RGS-058.truncate-rounded", "left", writtenLeft, "operation", onName, "duration", duration);
        }
        String smaller = smallerUnit(right.type(), left.type());
        Msg suffix = smaller != null
                ? Msg.frontend("RGS-058.smaller-unit", "truncate", truncate, "operation", onName, "smaller", smaller)
                : Msg.frontend("RGS-058.no-smaller-unit", "reason", noSmallerUnit(right.type()));
        return new Typing.Coded("RGS-058",
                params("operation", onName, "expression", source, "duration", duration, "cause", cause, "unit", unit,
                        "example", example, "truncate", truncate, "suffix", suffix));
    }

    /** Where a count can take a fraction in, and why: {@code site} is the sub-expression that brings it, in the
     *  author's words. */
    private record Rational(Expr site, Msg reason) {}

    /** Whether the value of {@code e} can carry a fraction, judged from its form alone (no case value): the first
     *  place that brings one in, or null when the value is whole by construction. The declared type is not enough
     *  on its own: it calls a product of two whole numbers fractional (RUNTIME-SEMANTICS ch. 2.3) and a sum whole
     *  even where a restatement into the left unit makes it fractional ("1 jr plus 6 mnd" is 1,5 jr). Exhaustive
     *  over the sealed union, so a new node is a compile error here rather than a silent whole. */
    private Rational rational(Expr e) {
        if (e == null) return null;
        return switch (e) {
            case Expr.Literal l -> hasRational(l) ? new Rational(l, Msg.frontend("PART.fraction-literal")) : null;
            case Expr.Rounding a -> a.decimals() > 0
                    ? new Rational(a, Msg.frontend("PART.fraction-rounded", "decimals", a.decimals())) : null;
            case Expr.Clamp b -> first(rational(b.base()), rational(b.minimum()), rational(b.maximum()),
                    conversion(b.minimum(), b.type()), conversion(b.maximum(), b.type()));
            case Expr.Binary b -> switch (b.op()) {
                case GEDEELD_DOOR, GEDEELD_DOOR_ABS -> new Rational(b, Msg.frontend("PART.fraction-division"));
                case PERCENTAGE_VAN -> new Rational(b, Msg.frontend("PART.fraction-percentage"));
                case TOT_DE_MACHT -> new Rational(b, Msg.frontend("PART.fraction-power"));
                case PLUS, MIN, VERMINDERD_MET -> first(rational(b.left()), rational(b.right()),
                        conversion(b.right(), b.type()));
                case MAAL -> first(rational(b.left()), rational(b.right()), denominatorConversion(b));
            };
            case Expr.FunctionCall f -> switch (f.kind()) {
                case AANTAL, AANTAL_GEVULDE, TIJDSDUUR, ABSOLUTE_TIJDSDUUR, AANTAL_DAGEN_IN, JAAR_UIT, MAAND_UIT,
                     DAG_UIT, UUR_UIT, MINUUT_UIT, SECONDE_UIT, MILLISECONDE_UIT, EERSTE_PAASDAG, DATUM_MET,
                     DATUMTIJD_MET, CONCATENATIE -> null;
                case TOTAAL -> new Rational(f, Msg.frontend("PART.fraction-total"));
                case TIJDSEVENREDIG_DEEL -> new Rational(f, Msg.frontend("PART.fraction-pro-rata"));
                case WORTEL -> new Rational(f, Msg.frontend("PART.fraction-root"));
                case SOM, MINIMUM, MAXIMUM, EERSTE, LAATSTE, ABSOLUTE_WAARDE -> {
                    Rational result = null;
                    for (Expr a : f.arguments()) result = first(result, rational(a), conversion(a, f.type()));
                    yield first(result, rational(f.emptyFallback()), conversion(f.emptyFallback(), f.type()));
                }
            };
            case Expr.BinderRef r -> {
                Variable v = binders.get(variableKey(r.name()));
                yield v != null ? rational(v.value()) : typeRational(r);
            }
            case Expr.Let l -> rational(l.body());
            case Expr.IfThenElse k -> first(rational(k.then()), rational(k.otherwise()),
                    conversion(k.then(), k.type()), conversion(k.otherwise(), k.type()));
            case Expr.AttributeReference r -> typeRational(r);
            case Expr.ParameterRef r -> typeRational(r);
            case Expr.VariableRef r -> typeRational(r);
            case Expr.Constant c -> typeRational(c);
            case Expr.DimensionCellRead c -> typeRational(c);
            case Expr.TextComposition ignored -> null;
            case Expr.InstanceSetUnion ignored -> null;
            case Expr.Unresolved ignored -> null;        // no claim: the marker carries its own diagnostic
        };
    }

    private static Rational first(Rational... candidates) {
        for (Rational b : candidates) if (b != null) return b;
        return null;
    }

    /** A read takes the fraction its declared type admits: a number with decimals, or the unbounded "getal". */
    private static Rational typeRational(Expr e) {
        if (!(e.type() instanceof Datatype.Numeric n) || n.wholeNumber()) return null;
        return new Rational(e, n.decimals() > 0 ? Msg.frontend("PART.fraction-decimals", "decimals", n.decimals())
                : Msg.frontend("PART.fraction-unbounded"));
    }

    private static boolean hasRational(Expr.Literal l) {
        if (!(l.type() instanceof Datatype.Numeric)) return false;
        java.math.BigDecimal[] r = dev.uchar.regelspraak.model.RationalParser.of(l.valueText());
        return r != null && r[0].remainder(r[1]).signum() != 0;
    }

    /** The literal as written with its number truncated toward zero, the count every executor steps. */
    private static String truncated(Expr.Literal l, String written) {
        java.math.BigDecimal[] r = dev.uchar.regelspraak.model.RationalParser.of(l.valueText());
        String whole = r[0].divide(r[1], 0, java.math.RoundingMode.DOWN).toPlainString();
        return written.replaceFirst(java.util.regex.Pattern.quote(l.valueText()),
                java.util.regex.Matcher.quoteReplacement(whole));
    }

    /** A restatement into {@code naar}'s unit brings a fraction in where one {@code sub} unit is no whole number of
     *  target units (a mnd is 1/12 jr, a dg 1/7 wk); the reverse direction multiplies and stays whole. */
    private static Rational conversion(Expr sub, Datatype to) {
        return sub == null || to == null ? null : conversion(sub, to.unitOf());
    }

    private static Rational conversion(Expr sub, Unit target) {
        Unit van = sub.type() == null ? null : sub.type().unitOf();
        if (van == null || target == null || !van.convertibleTo(target)) return null;
        java.math.BigDecimal[] f = van.exactRestateFactors(target);
        if (f == null || f[0].remainder(f[1]).signum() == 0) return null;
        // a literal's count is known, so its restatement is too: 12 mnd is exactly 1 jr
        java.math.BigDecimal[] r = sub instanceof Expr.Literal l ? dev.uchar.regelspraak.model.RationalParser.of(l.valueText()) : null;
        if (r != null && r[0].multiply(f[0]).remainder(r[1].multiply(f[1])).signum() == 0) return null;
        return new Rational(sub, Msg.frontend("PART.fraction-conversion", "target", target.rendering(), "from", van.rendering()));
    }

    /** A rate times a quantity restates the quantity into the rate's denominator (Typing's product arm). */
    private static Rational denominatorConversion(Expr.Binary b) {
        Unit el = b.left().type() == null ? null : b.left().type().unitOf();
        Unit rightUnit = b.right().type() == null ? null : b.right().type().unitOf();
        if (el == null || rightUnit == null) return null;
        if (el.denominator() != null && rightUnit.denominator() == null) return conversion(b.right(), el.denominator());
        if (rightUnit.denominator() != null && el.denominator() == null) return conversion(b.left(), rightUnit.denominator());
        return null;
    }

    /** The next smaller calendar unit the author can step the rest in, spelled as the language reads it ("min"
     *  lexes as the operator); null where the date has none below this one. */
    private static String smallerUnit(Datatype duration, Datatype date) {
        String unit = TimeCalculation.chronoUnit(duration);
        if (unit == null) return null;
        return switch (unit) {
            case "YEARS", "KWARTAAL" -> "maanden";
            case "MONTHS", "WEEKS" -> "dagen";
            case "DAYS" -> date instanceof Datatype.DateTime ? "uur" : null;
            case "HOURS" -> "minuten";
            case "MINUTES" -> "seconden";
            case "SECONDS" -> "milliseconden";
            default -> null;
        };
    }

    private static Msg noSmallerUnit(Datatype duration) {
        String unit = TimeCalculation.chronoUnit(duration);
        if (unit == null) return Msg.frontend("RGS-058.no-smaller-day-kind");
        if (unit.equals("DAYS")) return Msg.frontend("RGS-058.no-smaller-day");
        return Msg.frontend("RGS-058.no-smaller-millisecond");
    }

    /** The written operand without one pair of parentheses around the whole of it, so the rounding can wrap it. */
    private static String withoutOutsideParentheses(String t) {
        if (!t.startsWith("(") || !t.endsWith(")")) return t;
        int depth = 0;
        for (int i = 0; i < t.length(); i++) {
            if (t.charAt(i) == '(') depth++;
            else if (t.charAt(i) == ')' && --depth == 0 && i < t.length() - 1) return t;
        }
        return t.substring(1, t.length() - 1).trim();
    }

    /** Arithmetic node over already-built operands (used where the right operand is a
     *  primaireExpressie, e.g. the exponent of "tot de macht"). A typeringen error (a
     *  family mismatch or incompatible/inconvertible units) is SURFACED as a diagnostic
     *  before degrading the result to Onbekend, as the comparison and begrenzing paths
     *  do (p45). An Onbekend operand makes
     *  reken return ok (no claim), so no false positive on an unresolved sub-expression. */
    private Expr binaryExpr(Expr.BinOp op, Expr left, Expr right, Source source, int line) {
        // §6.11: "datum plus 2 kerstdagen" — a declared dagsoort as duration unit is a valid date shift
        // (day-by-day through the §9.8 predicate); the matrix itself cannot know the declared dagsoorten.
        // Through the shared reader, so this typing question and the four lowering positions in the Drools
        // branches read the spelling in ONE order (ch. 3.8: the calendar table first).
        boolean dayKindRight = (op == Expr.BinOp.PLUS || op == Expr.BinOp.MIN)
                && right.type() != null
                && TimeCalculation.dayKindForUnit(model, right.type().unitOf()) != null;
        Typing.Result res = Typing.compute(toTypingOp(op), left.type(), right.type(), dayKindRight,
                unitlessNumberLiteral(left), unitlessNumberLiteral(right));
        if (!res.ok())
            model.diagnostics.add(res.failure().at(RegelSpraakModel.Severity.ERROR, line));
        Datatype t = res.ok() ? res.type() : unknown();
        return new Expr.Binary(op, left, right, t, source);
    }

    private Expr rounded(Expr base, AfrondingContext a, Source source) {
        Expr.RoundingMode mode =
                a.NAAR_BENEDEN() != null ? Expr.RoundingMode.NAAR_BENEDEN
              : a.NAAR_BOVEN()  != null ? Expr.RoundingMode.NAAR_BOVEN
              : a.RICHTING_NUL() != null ? Expr.RoundingMode.RICHTING_NUL
              : a.WEG_VAN_NUL()  != null ? Expr.RoundingMode.WEG_VAN_NUL
              : Expr.RoundingMode.REKENKUNDIG;
        int dec = 0;
        if (a.NUMBER() != null) try { dec = Integer.parseInt(a.NUMBER().getText()); } catch (NumberFormatException ignore) {}
        Typing.Coded error = roundingError(base, source, src(a));
        if (error != null)
            model.diagnostics.add(error.at(RegelSpraakModel.Severity.ERROR, source.line()));
        return new Expr.Rounding(base, mode, dec, source);
    }

    /** A rounding rounds a number (§13.4.17.26). The rounding binds weakest, so written behind a date sum without the
     *  parentheses around the duration it rounds the date: a Rounding over a Date, which the verifier admits, ref
     *  refuses at evaluation and the ASP backend reads as the unrounded date. Null where the value is a number, or no claim is made. */
    private static Typing.Coded roundingError(Expr base, Source source, String rounding) {
        Datatype t = base.type();
        if (t == null || t.isUnknown() || t.isNumberLike()) return null;
        Object category = switch (t) {
            case Datatype.DateInDays d -> Msg.frontend("RGS-059.as-date");
            case Datatype.DateTime d -> Msg.frontend("RGS-059.as-date-time");
            case Datatype.Text x -> Msg.frontend("RGS-059.as-text");
            case Datatype.BooleanType b -> Msg.frontend("RGS-059.as-boolean");
            case Datatype.Enumeration e -> Msg.frontend("RGS-059.as-enumeration", "domain", e.domain());
            default -> t.name();
        };
        Object durationHint = base instanceof Expr.Binary b && b.left().type() != null && b.left().type().isDate()
                && (b.op() == Expr.BinOp.PLUS || b.op() == Expr.BinOp.MIN)
                ? Msg.frontend("RGS-059.round-the-duration", "left", b.left().source(),
                        "operation", b.op() == Expr.BinOp.PLUS ? "plus" : "min", "right", b.right().source(),
                        "rounding", rounding)
                : "";
        return new Typing.Coded("RGS-059", params("source", source, "base", base.source(), "category", category,
                "durationHint", durationHint));
    }

    /** Emit a §6.1.4 ERROR when a begrenzing bound is not order-comparable with the bounded
     *  value (a non-numeric, wrong-family, or unit-incompatible bound). Onbekend = no claim.
     *  {@code which} is the bound's name on the wire, {@code minimum} or {@code maximum}. */
    private void reportClamp(Expr base, Expr limit, String which, ParserRuleContext ctx) {
        Typing.Coded error = Typing.clamp(base.type(), limit.type(), which);
        if (error != null) {
            model.diagnostics.add(error.at(RegelSpraakModel.Severity.ERROR, ctx.getStart().getLine()));
            return;
        }
        // the verifier refuses this clamp; a WARNING and not an ERROR, because the casus shape Typing.clamp
        // tolerates carries it and an ERROR writes no document at all for it
        if (oneSidedUnit(base, limit))
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-060", params("bound", which), ctx.getStart().getLine()));
    }

    /** WF-099: a clamp restates each bound into its base's unit, so a unit on one side alone leaves nothing to
     *  restate — unless the unitless side is a numeric literal, which adopts the other side's unit (IR-SPEC ch. 4.6,
     *  the verifier's reading). {@link Typing#clamp} admits the one-sided pair; a side it makes no claim on
     *  (Onbekend, not numeric) makes none here either. */
    private static boolean oneSidedUnit(Expr base, Expr limit) {
        Datatype b = base.type(), g = limit.type();
        if (b == null || g == null || !b.isNumberLike() || !g.isNumberLike()) return false;
        if ((b.unitOf() == null) == (g.unitOf() == null)) return false;
        return !unitlessNumberLiteral(base) && !unitlessNumberLiteral(limit);
    }

    static boolean unitlessNumberLiteral(Expr e) {
        return e instanceof Expr.Literal l && l.type() instanceof Datatype.Numeric n && n.unit() == null;
    }

    /** Resolve a begrenzing into [minimum, maximum] (either may be null). */
    private Expr[] bounds(BegrenzingContext b) {
        Expr min = null, max = null;
        if (b.begrenzingMinimum() != null) min = buildPrimary(b.begrenzingMinimum().primaireExpressie());
        if (b.begrenzingMaximum() != null) max = buildPrimary(b.begrenzingMaximum().primaireExpressie());        // standalone 'met een maximum van'
        else if (b.begrenzingMaximumNaEn() != null) max = buildPrimary(b.begrenzingMaximumNaEn().primaireExpressie()); // after 'en' (bare allowed)
        return new Expr[]{ min, max };
    }

    // ---------------- §13.4.17 primaire expressie ----------------
    private Expr buildPrimary(PrimaireExpressieContext p) {
        // GEEN GETUIGE: ANTLR kiest de primaireExpr-tak van `expressie` pas NA een geslaagde match, dus
        // een PrimaireExprContext zonder kind is niet te produceren. De zusterwacht in build(e) is wel
        // bereikbaar (zie SilentMarkerTest). Gemeten 2026-09-19.
        if (p == null) return unresolvedMarker(p, Msg.frontend("RGS-092.no-primary"));
        Source source = source(p);
        if (p.getalWaarde() != null) {
            GetalWaardeContext g = p.getalWaarde();
            int dec = literalDecimals(source.snippet());    // §6.1.2: the literal's real precision, not 0
            boolean wholeNumber = dec == 0;
            // uchar WF-081 (IR-SPEC ch. 4.1): the literal's waarde is the BARE number lexeme —
            // a '%' or unit suffix belongs in the datatype's eenheid, never in the value string.
            // The NUMBER token is that lexeme; bron keeps the full source snippet ("5 €").
            String lexeme = g.NUMBER().getText();
            if (g.PERCENT_SIGN() != null) return new Expr.Literal(lexeme, new Datatype.Percentage(wholeNumber, dec), source);
            if (g.eenheidExpressie() != null) {
                // §13.2.17: the literal-with-unit surface admits a composite (rate) denominator
                // ("120 EUR/jr"). Resolve it through the SAME composite-aware helper the attribute
                // datatype uses, so the denominator survives and the literal carries a full
                // Unit.compound — otherwise a rate literal is typed by its numerator alone and
                // both §4 unit checking and the exacte cross-unit restatement operate on the wrong unit.
                Unit e = DefinitionResolver.unitOf(model, g.eenheidExpressie());
                return new Expr.Literal(lexeme, new Datatype.Numeric(wholeNumber, dec, Datatype.Sign.ONBEPAALD, e), source);
            }
            return new Expr.Literal(source.snippet(), new Datatype.Numeric(wholeNumber, dec, Datatype.Sign.ONBEPAALD, null), source);
        }
        if (p.tekstWaarde() != null) {
            // §5.4/§13.4.17.9: the lexer splits a "…" into literal STRING_TEXT chunks and «expressie» escapes.
            // A PLAIN string (no «…») builds an Expr.Literal(Tekst); an interpolated string ("…«expressie»…")
            // builds an Expr.TextComposition whose «…» parts are each a full, recursively-resolved expressie (any
            // datatype, coerced to Tekst at evaluation, §5.4).
            java.util.List<Expr.TextComposition.Part> parts = new ArrayList<>();
            boolean hasExpression = false;
            for (var part : p.tekstWaarde().tekstReeksDeel()) {
                if (part.expressie() != null) {
                    hasExpression = true;
                    parts.add(new Expr.TextComposition.EmbeddedExpression(build(part.expressie())));
                } else if (part.STRING_TEXT() != null) {
                    parts.add(new Expr.TextComposition.LiteralChunk(part.STRING_TEXT().getText()));
                }
            }
            if (!hasExpression) {
                // uchar, maintainer decision 2026-09-18: a Tekst literal's waarde is the PARSED string — the
                // karakterreeks BETWEEN the delimiters — not the source snippet with its own outer '"' still on.
                // Emitting the snippet gave v0 two lexeme families for one text value (a hand-authored wire's
                // `vondst` beside an emitted `"vondst"`), and since all four consumers compare Tekst VERBATIM
                // (Literals.textOrEnumLabel) those two documents say the same thing and are not equal — an
                // emitted golden then only played when its case input carried the delimiters too. Third in the
                // row of bare-lexeme fixes beside WF-081 (number) and WF-084 (date): waarde is the lexeme the
                // executors read, bron keeps the full source snippet.
                // The unquoting rule, measured at the grammar rather than guessed: tekstWaarde is
                // STRING_START textCompositionPart* STRING_END, and a literal chunk is STRING_TEXT `~["«»\r\n]+`
                // (RegelSpraakLexer.g4). There is NO escape sequence, so a chunk can never hold a '"' and the
                // delimiters are separate tokens — taking the chunks IS stripping the outer delimiters, total
                // and lossless, with every embedded character kept verbatim and nothing unescaped. Taking the
                // TOKENS rather than source.snippet() minus its first and last char also keeps the interior's own
                // whitespace: DefinitionResolver.src collapses every whitespace run to one space, which a
                // snippet-derived value would silently carry into the text ("a  b" would read "a b").
                StringBuilder text = new StringBuilder();
                for (Expr.TextComposition.Part part : parts) text.append(((Expr.TextComposition.LiteralChunk) part).text());
                return new Expr.Literal(text.toString(), new Datatype.Text(), source);
            }
            return new Expr.TextComposition(parts, source);
        }
        if (p.booleanWaarde() != null)  return new Expr.Literal(source.snippet(), new Datatype.BooleanType(), source);
        if (p.deDato() != null) {
            // §13.2.19/.21: a dedato WITH a time component (hh:mm:ss[.mmm]) is a Datum-tijd (millisecond
            // precision), not a Datum in dagen — typing it as a bare date drops the time and lowers a
            // LocalDate into a LocalDateTime target (a Drools type error / silent wrong value).
            boolean withTime = source.snippet().matches(".*\\d{1,2}:\\d{2}:\\d{2}.*");
            if (withTime) {
                // uchar WF-095 (IR-SPEC ch. 4.1, narrowed 2026-09-22): waarde is the canonical ISO
                // lexeme yyyy-mm-ddThh:mm:ss.fff, bron keeps the day-first snippet — the date branch's
                // split. The DATE token always writes the three millisecond digits, so .SSS loses nothing.
                // A calendar-impossible token (30 February, hour 24) is an ERROR, which stops emission,
                // so the snippet returned beside it never reaches a wire. The literal is built more than
                // once per model, hence the contains-guard against a repeated report.
                java.time.LocalDateTime t = sourceDateTime(source.snippet());
                if (t == null) {
                    RegelSpraakModel.Diagnostic error = new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                            "RGS-061", params("literal", source.snippet()),
                            p.getStart().getLine());
                    if (!model.diagnostics.contains(error)) model.diagnostics.add(error);
                    return new Expr.Literal(source.snippet(), new Datatype.DateTime(), source);
                }
                String iso = t.format(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS"));
                return new Expr.Literal(iso, new Datatype.DateTime(), source);
            }
            // uchar WF-084 (IR-SPEC ch. 4.1): a date literal's waarde is the BARE ISO lexeme the
            // executors parse (yyyy-mm-dd, fixed field widths) — NOT the Dutch day-first source
            // snippet ("31-01-2024"), which both executors read as ISO and so silently misparse as
            // year 31. The shared reader already holds the parsed date; bron keeps the full source
            // snippet. Mirror of the numeric-literal fix above (bare lexeme in waarde, snippet in bron).
            // A calendar-impossible token (30 February) is refused exactly as the datum-tijd branch
            // above refuses it: an ERROR stops emission, so the snippet never reaches a wire.
            // The source reader matches the whole snippet because the token takes DIGIT+ for day and
            // month: a searching reader read 2024-01-2026 as 2024-01-20 and 123-01-2026 as 23-01-2026.
            java.time.LocalDate d = sourceDate(source.snippet());
            if (d == null) {
                RegelSpraakModel.Diagnostic error = new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "RGS-062", params("literal", source.snippet()),
                        p.getStart().getLine());
                if (!model.diagnostics.contains(error)) model.diagnostics.add(error);
                return new Expr.Literal(source.snippet(), new Datatype.DateInDays(), source);
            }
            return new Expr.Literal(d.toString(), new Datatype.DateInDays(), source);
        }
        if (p.ENUM_LITERAL() != null)   return enumValue(p, p.ENUM_LITERAL().getText());
        if (p.rekenConstante() != null) {
            boolean year = p.rekenConstante().REKENJAAR() != null;
            Datatype t = year ? new Datatype.Numeric(true, 0, Datatype.Sign.ONBEPAALD, null) : new Datatype.DateInDays();
            return new Expr.Constant(year ? "Rekenjaar" : "Rekendatum", t, source);
        }
        if (p.HIJ() != null)            return new Expr.Constant("hij", unknown(), source);
        // §9.8/§13.4.17.43 "de dag" — the dagsoortreferentie, the (arbitrary) day a dagsoortdefinitie is about. It IS
        // a date (Datum in dagen): typing it so lets the §8.1.5 "<datum> is een <dagsoort>" recognition fire over it,
        // which is exactly the spec's central cross-dagsoort case (a feestdag definitie tests "de dag is een kerstdag").
        if (p.DE() != null && p.DAG() != null) return new Expr.Constant("de dag", new Datatype.DateInDays(), source);
        if (p.functieAanroep() != null) return buildFunction(p.functieAanroep());
        if (p.datumMet() != null)       return new Expr.FunctionCall(Expr.FunctionKind.DATUM_MET, args(p.datumMet()), null, null, null, new Datatype.DateInDays(), source);
        if (p.datumTijdMet() != null)   return new Expr.FunctionCall(Expr.FunctionKind.DATUMTIJD_MET, args(p.datumTijdMet()), null, null, null, new Datatype.DateTime(), source);
        if (p.attribuutVanOnderwerp() != null) return buildAttribute(p.attribuutVanOnderwerp());
        if (p.percentageVanHaakjes() != null) return buildPercentageOfParentheses(p.percentageVanHaakjes(), source);
        if (p.expressie() != null)      return build(p.expressie()); // ( expressie )
        if (p.onderwerp() != null) {
            Expr poss = subjectPossessive(p.onderwerp(), source); // "zijn <attribuut van het subject>"
            if (poss != null) return poss;
            Expr named = nameRef(src(p.onderwerp()), source);
            // "de Vergunning zelf" as an operand: what a characteristic check reads its subject by
            Anchor anchor = anchorOf(p.onderwerp());
            if (named instanceof Expr.Unresolved && anchor != null) writtenAnchors.put(named, anchor);
            return named;
        }
        if (p.variabeleNaam() != null)  return nameRef(src(p.variabeleNaam()), source);
        return unresolvedMarker(p, Msg.frontend("RGS-092.primary-form"));
    }

    /** §13.4.17.12 "het percentage P van ( Y )" without '%' (grammar/DIVERGENCES.md, "The percentage forms"): the
     *  function the '%' form spells, typed the same way (Y's unit, no decimal claim). Without the sign only a
     *  Percentage on the left makes the sentence the percentage function: P is read as a name — a parameter or a
     *  "Daarbij geldt" variable — and any other left operand is refused rather than read as one. */
    private Expr buildPercentageOfParentheses(PercentageVanHaakjesContext h, Source source) {
        String name = (h.bepaaldLidwoord() != null ? src(h.bepaaldLidwoord()) + " " : "") + src(h.kaleNaamDeel());
        Expr left = nameRef(name, source(h.kaleNaamDeel())), right = build(h.expressie());
        if (!(left.type() instanceof Datatype.Percentage)) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "RGS-063", params("expression", source.snippet(), "name", name), h.getStart().getLine()));
            // the ERROR keeps the document off disk, so the node is typed unknown and never reaches a wire
            return new Expr.Binary(Expr.BinOp.PERCENTAGE_VAN, left, right, unknown(), source);
        }
        Datatype t = right.type() instanceof Datatype.Numeric n
                ? new Datatype.Numeric(false, 0, Datatype.Sign.ONBEPAALD, n.unit()) : unknown();
        return new Expr.Binary(Expr.BinOp.PERCENTAGE_VAN, left, right, t, source);
    }

    /* The RegelSpraak SOURCE readers: the §13.2.19/.21 DATE token as the author wrote it, day-first, the
     * variable-width day and month the token admits cut to one or two digits, matched whole. They live here
     * and not in core.Literals because Literals is the WIRE reader the back-ends link, and the wire carries
     * the ISO lexeme alone (WF-084, WF-095; card datumtijd-lezers-versmalling). The Drools
     * backend's build excludes this package, so no back-end can reach these. */
    private static final java.util.regex.Pattern SOURCE_DATE = java.util.regex.Pattern.compile(
            "(?:dd\\. +)?(\\d{1,2})-(\\d{1,2})-(\\d{4})");
    private static final java.util.regex.Pattern SOURCE_DATE_TIME = java.util.regex.Pattern.compile(
            "(?:dd\\. +)?(\\d{1,2})-(\\d{1,2})-(\\d{4}) +(\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d{3}))?");

    /** The day-first date snippet ({@code [dd. ]d-m-yyyy}) as a LocalDate; null when it is not one. */
    static java.time.LocalDate sourceDate(String snippet) {
        java.util.regex.Matcher m = SOURCE_DATE.matcher(snippet);
        if (!m.matches()) return null;
        try { return java.time.LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1))); }
        catch (java.time.DateTimeException e) { return null; }
    }

    /** The day-first datum-tijd snippet ({@code [dd. ]d-m-yyyy hh:mm:ss[.SSS]}) as a LocalDateTime, the
     *  milliseconds scaled to nanoOfSecond; null when it is not one. */
    static java.time.LocalDateTime sourceDateTime(String snippet) {
        java.util.regex.Matcher m = SOURCE_DATE_TIME.matcher(snippet);
        if (!m.matches()) return null;
        try {
            int nanos = (m.group(7) == null ? 0 : Integer.parseInt(m.group(7))) * 1_000_000;
            return java.time.LocalDateTime.of(
                    Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)), nanos);
        } catch (java.time.DateTimeException e) { return null; }
    }

    /** §13.2.2-.5: the decimal precision of a numeric literal's value — its minimal scale, since
     *  §6.1.2 treats 0,5 and 0,50 as the same precision. A §13.2.5 rational reduces to its exact
     *  value (4/2 -> 0, 3/2 -> 1); a non-terminating one (1/3) has no finite scale, so it is read at
     *  DECIMAL64 and types as "many decimals" — which §6.1.2 then rightly demands be rounded. */
    private static int literalDecimals(String source) {
        java.math.BigDecimal[] r = dev.uchar.regelspraak.model.RationalParser.of(source);
        if (r == null) return 0;
        // the literal's exact value teller/noemer (a decimal/integer has noemer ONE, so the divide is exact);
        // a non-terminating rational (1/3) has no finite scale, so read it at DECIMAL64 — §6.1.2 then rightly
        // demands the "many decimals" result be rounded.
        java.math.BigDecimal v;
        try { v = r[0].divide(r[1]); }
        catch (ArithmeticException nonTerminating) { v = r[0].divide(r[1], java.math.MathContext.DECIMAL64); }
        return Math.max(0, v.stripTrailingZeros().scale());
    }

    // ---------------- §13.4.17 functies / aggregaties ----------------

    /** A member generated on the element type of a value fold over ONE set followed by to-one steps
     *  (grammar/DIVERGENCES.md, "A fold over a set and to-one steps after it"): its name, the element type it is
     *  declared on, the declared member the steps reach, the type that member is declared on and its datatype, the
     *  declared role names element-outward, and the source anchors of the rule that sets it. {@link RuleChecker}
     *  adds that rule once every authored rule has resolved. */
    record HelperMember(String name, ObjectType element, String member, ObjectType target, Datatype type, List<String> roles,
                   Source targetSource, Source valueSource) {}

    /** The generated members, keyed by the element type's name and the member's name under the name identity. */
    private final Map<String, HelperMember> helperMembers = new LinkedHashMap<>();

    /** The generated members in the order their folds were resolved. */
    List<HelperMember> helperMembers() { return List.copyOf(helperMembers.values()); }

    /**
     * §13.4.17 a value fold over ONE set followed by to-one steps: "de maximale waarde van het gewicht van de weging
     * van de vondst van alle jutters van de Ploeg". The name of the read runs over every inner 'van', so the fold reads
     * the member "gewicht van de weging van de vondst" of the element type, which no type declares. The steps are
     * walked from the element type as {@link #attributeRef} walks a chain from the subject (fewest steps first, each a
     * to-one role, the rest of the name a declared attribute of the type reached); where they reach a scalar member,
     * a generated member "&lt;lid&gt; via &lt;rol 1&gt;, … en &lt;rol n&gt;" of that member's datatype is declared,
     * set by a generated rule from the chain ({@link RuleChecker#helperRules}), and the fold ranges over it, a
     * subselection staying on the element. The member stands on the type that fills the other role of the first
     * step's fact type, where the element type is that type or a subtype of it and the steps read the same from it:
     * a fold over a subtype and one over its supertype then share one member, whatever their order in the document.
     * The wire, the verifier and the executors see the document an author writes by hand with such a member. Two
     * folds over the same steps share one member; a member of that name in the inheritance line refuses the lowering
     * with an ERROR. A step that is no to-one role, a member that is time-dependent or dimensioned, and every other
     * shape keep the arguments as they are, and with them the refusal of an undeclared member below.
     */
    private List<Expr> stepsAfterSet(Expr.FunctionKind kind, List<Expr> arguments, FunctieAanroepContext f) {
        boolean valueFold = switch (kind) {
            case SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE, AANTAL_GEVULDE -> true;
            default -> false;
        };
        if (!valueFold || arguments.size() != 1 || !(arguments.get(0) instanceof Expr.AttributeReference sa)
                || !sa.set() || sa.objectType() == null || sa.attribute() == null
                || sa.objectType().hasMember(sa.attribute()) || !sa.attribute().contains(" van "))
            return arguments;
        ObjectType element = sa.objectType();
        String[] parts = sa.attribute().split("\\s+van\\s+");
        List<String> roles = new ArrayList<>();
        ObjectType cur = element, firstTarget = null;
        for (int i = parts.length - 1; i >= 1; i--) {
            RegelSpraakModel.ChainHop hop = model.chainHop(cur, stripArticle(parts[i]).trim());
            if (hop == null) return arguments;
            roles.add(hop.role());
            cur = hop.targetName();
            if (firstTarget == null) firstTarget = cur;
            String prefix = String.join(" van ", java.util.Arrays.copyOfRange(parts, 0, i));
            String member = cur.attributeName(prefix);
            if (member == null) {
                String singular = PluralMatcher.findAttribute(cur, prefix);
                if (singular != null) member = cur.attributeName(singular);
            }
            if (member == null) continue;
            if (cur.granularityOf(member) != null || !cur.dimensionsOf(member).isEmpty()) return arguments;
            HelperMember h = helperMember(carrier(element, firstTarget, roles, cur), member, cur, roles, sa, f);
            if (h == null) return arguments;
            return List.of(new Expr.AttributeReference(h.name(), element, sa.subjectSource(), sa.navigationRole(), h.type(),
                    sa.source(), true, sa.navigationPath(), sa.rangePath(), sa.quantifier(), sa.threshold()));
        }
        return arguments;
    }

    /** The type a generated member stands on: the filler of the other role of the first step's fact type, where the
     *  element type is that type or a subtype of it and the steps reach the same type from it; else the element type
     *  itself (a reciprocal fact type, a first step that binds no unique role from the element). */
    private ObjectType carrier(ObjectType element, ObjectType firstTarget, List<String> roles, ObjectType target) {
        RegelSpraakModel.RoleBindingRef b = model.connectingRoleViaNameBinding(element, firstTarget, roles.get(0));
        RegelSpraakModel.FactType ft = b == null ? null : model.factTypes.get(b.factType());
        if (ft == null || ft.roles.size() != 2) return element;
        RegelSpraakModel.Role other = ft.roles.get(0).name().equals(b.role()) ? ft.roles.get(1) : ft.roles.get(0);
        if (other.name().equals(b.role())) return element;
        ObjectType fillerType = model.objectTypeByName(other.objectTypeName());
        if (fillerType == null || fillerType == element || !element.isA(fillerType)) return element;
        List<ObjectType> reached = model.followChain(fillerType, roles);
        return reached != null && !reached.isEmpty() && reached.get(reached.size() - 1) == target ? fillerType : element;
    }

    /** The member {@link #stepsAfterSet} folds over, on {@code element}, the type {@link #carrier} chose: the
     *  one generated for the same steps on that type or an ancestor, or a new one declared on it. Null, with an ERROR
     *  at the fold, when a member of that name stands anywhere in that type's inheritance line — declared by the
     *  author, or generated on a subtype (one instance would carry two members of one name, WF-105). */
    private HelperMember helperMember(ObjectType element, String member, ObjectType target, List<String> roles,
                            Expr.AttributeReference sa, FunctieAanroepContext f) {
        String name = member + " via " + (roles.size() == 1 ? roles.get(0)
                : String.join(", ", roles.subList(0, roles.size() - 1)) + " en " + roles.get(roles.size() - 1));
        String keyName = dev.uchar.regelspraak.emit.IrEmitter.norm(name);
        for (ObjectType t : element.chain()) {
            HelperMember earlier = helperMembers.get(t.name + "\u0000" + keyName);
            if (earlier != null) return earlier;
        }
        for (ObjectType u : model.objectTypes.values()) {
            if (u != element && !u.isA(element) && !element.isA(u)) continue;
            boolean occupied = u.attributes.keySet().stream()
                    .anyMatch(k -> dev.uchar.regelspraak.emit.IrEmitter.norm(k).equals(keyName))
                    || u.characteristics.stream().anyMatch(k -> dev.uchar.regelspraak.emit.IrEmitter.norm(k).equals(keyName));
            if (!occupied) continue;
            RegelSpraakModel.Diagnostic error = new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "RGS-064", params("expression", source(f).snippet(), "name", name, "objectType", element.name, "occupiedBy", u.name), f.getStart().getLine());
            if (!model.diagnostics.contains(error)) model.diagnostics.add(error);
            return null;
        }
        Datatype type = target.attribute(member);
        element.attributes.put(name, type);
        // the texts of the generated rule, spelled as the author writes it by hand: the member read keeps its
        // written article, the steps read from the element as "van de <element>"
        String read = sa.source().snippet();
        String suffix = " van " + sa.subjectSource();
        String attributeRead = read.endsWith(suffix) ? read.substring(0, read.length() - suffix.length()) : read;
        String article = attributeRead.regionMatches(true, 0, "het ", 0, 4) ? "Het" : "De";
        HelperMember h = new HelperMember(name, element, member, target, type, List.copyOf(roles),
                Source.of(f, article + " " + name + " van een " + element.name),
                Source.of(f, attributeRead + " van de " + element.name));
        helperMembers.put(element.name + "\u0000" + keyName, h);
        return h;
    }

    private Expr buildFunction(FunctieAanroepContext f) {
        Source source = source(f);
        Expr.FunctionKind kind = functionKind(f);
        // §11/ch. 4.10 an AGGREGATION is a structure-sensitive position through and through: its element type, its
        // set-ness, its dimensieselectie, its "gedurende" clip and its "die …" subselectie are all read off the
        // SHAPE of the resolved operand (and of the subselectie's right-hand side), never off a value. Resolve the
        // whole construct with variable reads SUBSTITUTED — see {@link #inlining}. A non-aggregating functie
        // (wortel, absolute waarde, a date component, a tijdsduur) decides on TYPES only, which a BinderRef carries
        // faithfully, so it keeps the sharing a Let gives it.
        if (binderMode && aggregates(kind)) return inlining(() -> buildFunction(f));
        if (folds(kind)) {
            Expr atForm = foldAtForm(f);
            if (atForm != null) return atForm;
        }
        return buildFold(f);
    }

    /** {@link #buildFunction} past the fold whose range starts at the subject of the form. */
    private Expr buildFold(FunctieAanroepContext f) {
        Source source = source(f);
        Expr.FunctionKind kind = functionKind(f);
        List<Expr> arguments = stepsAfterSet(kind, args(f), f);
        ObjectType elem = aggregationElement(kind, arguments);
        // §13.4.17.47 telling vs a declared parameter whose name begins with "aantal": the spec
        // gives no syntactic tie-break for "het aantal <phrase>" (a count of <phrase>, or a
        // reference to a declared name "aantal <phrase>"). Resolve it by countability: a count
        // ranges over a countable element, so when "het aantal <phrase>" binds none AND the whole
        // phrase is a declared parameter, read it as that parameter — real casus source declares
        // parameters whose names begin with "aantal" and reads them back with the article
        // ("het aantal …"). In a well-formed model a genuine count always binds an
        // element (so is never reinterpreted); only a count phrase that cannot resolve a countable
        // element falls back to a same-named declared parameter, where the parameter is the
        // concrete reading and the count reading is meaningless.
        if (kind == Expr.FunctionKind.AANTAL && elem == null) {
            Expr ref = nameRef(source.snippet(), source);
            if (!(ref instanceof Expr.Unresolved)) return ref;
        }
        // §5.7 the CONCATENATION of subject expressions under a count (IR-SPEC ch. 4.3, WF-110;
        // maintainer decision 2026-09-20). Tried BEFORE reportAggregation, because a concatenation that
        // resolves is not the listed WAARDE-telling that arm refuses: it counts INSTANCES and reaches
        // the wire as an InstanceSetUnion — ONE argument — so WF-037's arity arm never sees it.
        if (f instanceof AantalFuncContext && arguments.size() >= 2) {
            Expr union = buildInstanceUnion(f);
            if (union != null)
                return new Expr.FunctionCall(Expr.FunctionKind.AANTAL, java.util.List.of(union), null, null,
                        null, functionType(Expr.FunctionKind.AANTAL, arguments), source);
        }
        // WF-037's count-source arm (maintainer decision 2026-09-23). Placed after the parameter
        // fallback and the union above, the two routes by which a count without an element type is
        // legitimate; what is left counts its operand rather than a population.
        if (kind == Expr.FunctionKind.AANTAL && elem == null && arguments.size() < 2)
            model.diagnostics.add(countSourceError(arguments, f).at(RegelSpraakModel.Severity.ERROR, f.getStart().getLine()));
        reportAggregation(kind, arguments, f);
        Datatype type = f instanceof TijdsduurFuncContext t ? durationType(t) : functionType(kind, arguments);
        if (f instanceof TijdsduurFuncContext) {
            Typing.Coded unitError = durationUnitError(kind, type.unitOf(), arguments);
            if (unitError != null)
                model.diagnostics.add(unitError.frame("duration")
                        .at(RegelSpraakModel.Severity.ERROR, f.getStart().getLine()));
        }
        Expr emptyFallback = emptyFallbackOf(f);
        // §4.10/§5.8.2: the fallback substitutes for the sum's RESULT, so it must be numeriek
        // (a Tekst/Datum fallback would otherwise emit a type-mixed DRL ternary that throws at
        // fire time). Onbekend = no claim, so it is not flagged. The verifier refuses every such fallback but a
        // percentage (WF-038), so the others carry its sentence.
        if (emptyFallback != null && !emptyFallback.type().isNumber() && !emptyFallback.type().isUnknown()) {
            if (emptyFallback.type().isNumberLike())
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "RGS-065", params("type", emptyFallback.type().name()), f.getStart().getLine()));
            else
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "RGS-066", params("kind", Typing.kindWord(emptyFallback.type())), f.getStart().getLine()));
        }
        // declared-names-only IR: an aggregation over a SET read ("de <x> van alle <elem>") whose written form binds NO
        // declared member of the (known) element type is Unresolved — with a WARNING — so both back-ends
        // marker it instead of accumulating over a getter that does not exist (an uncompilable DRL package /
        // a fire-time reflection abort). The plural-binding in attributeRef already mapped a written plural
        // onto its declared singular, so only a genuinely unknown member reaches this.
        boolean aggregationKind = switch (kind) {
            case AANTAL, SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE, AANTAL_GEVULDE -> true;
            default -> false;
        };
        // WF-037's filled-count shape (IR-SPEC ch. 4.3): the single argument MUST be a SET reading.
        // Refused HERE, at the operator, because every other kind admits a non-set source for a reason
        // of its own — the §7.1 timeline fold, the row-1 count wart — so the wire fence is per kind and
        // the frontend must draw the same one or it emits a document the verifier rejects. ERROR, so
        // irMode writes nothing: the dialect phrase is new, no committed expectation depends on the
        // laxer reading, and a silent marker would leave the author with a fold over nothing.
        if (kind == Expr.FunctionKind.AANTAL_GEVULDE
                && !(arguments.size() == 1
                     && arguments.get(0) instanceof Expr.AttributeReference zb && zb.set())) {
            // written, the fold carries no element type, which the verifier refuses in these words
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "RGS-067", params("expression", source.snippet()),
                    f.getStart().getLine()));
            // the aggregation's OWN type, not the unknown sentinel: a refused fold must still leave a
            // well-formed marker (IR-SPEC ch. 2.4), exactly as checkFoldSource's three refusals do
            return new Expr.Unresolved(type, source, null);   // the ERROR above is this marker's diagnosis
        }
        if (aggregationKind && arguments.size() == 1
                && arguments.get(0) instanceof Expr.AttributeReference sa && sa.set()
                && sa.objectType() != null && !sa.objectType().hasMember(sa.attribute())) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-068", params("attribute", sa.attribute(), "objectType", sa.objectType().name, "expression", source(f)),
                    f.getStart().getLine()));
            return unresolvedMarker(f, null);   // the WARNING above is this marker's diagnosis
        }
        if (aggregationKind && arguments.size() == 1
                && arguments.get(0) instanceof Expr.AttributeReference vb && vb.set()) {
            Expr refused = checkFoldSource(f, vb, type);
            if (refused != null) return refused;
        }
        Condition filter = aggregationFilter(f, elem, arguments);
        if (filter instanceof Condition.Unresolved)   // a resolved kenmerk-filter is lowered; only an
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-069", params("filter", filter.source()), f.getStart().getLine()));
        // §13.4.17.51-.54: a dimensieselectie over a gedimensioneerd attribuut ("over alle <dim>",
        // "over de <dim> vanaf … t/m …", "in {…}", or the bare "… vanaf <label> t/m <label>") aggregates
        // over a DIMENSION's label set, not over object instances. When it resolves — a single dimensioned
        // attribute operand whose dimension and labels are all known — carry it as a DimensionSelection so the
        // transpiler folds the attribute over exactly those labels.
        Expr.DimensionSelection dimSel = resolveDimensionSelection(f, arguments);
        if (dimSel != null)
            // recomputed: the §13.4.17.53 re-split may have rebuilt the polluted operand in-place, so the
            // pre-computed type/element (derived from the then-Onopgelost argument) can be stale.
            return new Expr.FunctionCall(kind, arguments, aggregationElement(kind, arguments), emptyFallback, filter,
                    dimSel, functionType(kind, arguments), source);
        // §13.4.17.51-.58: a dimensieselectie that did NOT resolve, or a conditiebijexpressie ("gedurende
        // de tijd dat …") — the temporal axis is not built yet (p5) — must not be SILENTLY DROPPED, which
        // would aggregate the whole set as if unrestricted. Capture its source as an Unresolved filter so
        // the transpiler markers the aggregation (a visible no-op), and surface a WARNING. (Reuses the
        // aggregation-restriction marker path; an already-unresolved die-filter keeps its own marker.)
        Source dimSource = dimensionOrConditionSource(f);
        if (dimSource != null) {
            // §7.1 a "gedurende de tijd dat <temporeel kenmerk>" restriction on a totaal over a single-period
            // temporal own-attribute resolves to a temporal Condition.CharacteristicCheck the transpiler clips the value
            // timeline by; anything else (a dimensieselectie, a navigated/compound/non-temporal condition) stays
            // an Unresolved marker so the aggregation is a visible no-op, not a silently-unrestricted total.
            Condition during = duringCharacteristicCheckFilter(f, kind, arguments);
            if (during != null) {
                filter = during;
            } else {
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                        "RGS-070", params("expression", dimSource), f.getStart().getLine()));
                if (!(filter instanceof Condition.Unresolved)) filter = new Condition.Unresolved(dimSource, null);
            }
        }
        // §13.4.17.57 "het aantal dagen in <de maand|het jaar> dat <conditie>": the post-"dat" condition is a
        // DIRECT grammar child (not a conditiebijexpressie), so it is otherwise dropped. Structure a simple
        // single-period subject temporal-kenmerk membership into a Condition.CharacteristicCheck (reusing the gedurende path's
        // resolver) so the transpiler can count true-days per period; anything else (a comparison / compound /
        // navigated / non-temporal condition) leaves the filter unset -> the transpiler markers.
        RegelSpraakModel.Granularity operatorGran = null;
        if (kind == Expr.FunctionKind.AANTAL_DAGEN_IN && f instanceof AantalDagenFuncContext adf) {
            // §13.4.17.57/§4.21 carry the operator's "in <de maand|het jaar>" bucket unit so the transpiler can
            // require it to MATCH the doel's tijdlijn rather than silently bucketing by the doel's unit.
            operatorGran = adf.MAAND() != null ? RegelSpraakModel.Granularity.MAAND
                    : (adf.JAAR() != null ? RegelSpraakModel.Granularity.JAAR : null);
            if (adf.toplevelElementaireVoorwaarde() != null && !(filter instanceof Condition.CharacteristicCheck)) {
                ToplevelElementaireVoorwaardeContext c = adf.toplevelElementaireVoorwaarde();
                Condition.CharacteristicCheck k = temporalCharacteristicCheckFromOperator(c.toplevelOperator(), null, source(adf));  // §8.3.1 toplevel = vragende only
                if (k != null) filter = k;
            } else if (adf.toplevelSamengesteldeVoorwaarde() != null && !(filter instanceof Condition.Compound)) {
                // §8.4.2 a COMPOUND "dat er aan alle / ten minste 1 van de volgende voorwaarden wordt voldaan: …" —
                // the same temporal-kenmerk compound as a gedurende; the transpiler buckets the days the COMBINED
                // condition (intersection for ALLE / union for ten-minste-1 of the bullets) holds.
                Condition compound = duringCompound(adf.toplevelSamengesteldeVoorwaarde(), source(adf));
                if (compound != null) filter = compound;
            }
        } else if (kind == Expr.FunctionKind.TIJDSEVENREDIG_DEEL && f instanceof TijdsevenredigFuncContext tvf) {
            // §13.4.17.58/§7.3.2 carry the operator's "per <maand|jaar>" bucket unit; the transpiler requires it to
            // MATCH the doel's tijdlijn ("de knips ... moeten passen op de tijdlijn van het attribuut", §7.3.2).
            operatorGran = tvf.MAAND() != null ? RegelSpraakModel.Granularity.MAAND
                    : (tvf.JAAR() != null ? RegelSpraakModel.Granularity.JAAR : null);
        }
        // WF-037 (IR-SPEC ch. 4.3): a value fold without an element type and without a dimension selection is the
        // listed form, which has two or more operands. One operand without a range (a parameter, a literal) is no fold
        // the wire has, so the frontend refuses it in the verifier's words instead of writing what the verifier refuses.
        String valueFold = switch (kind) {
            case SOM -> "som van";
            case TOTAAL -> "totaal van";
            case MINIMUM -> "minimale waarde van";
            case MAXIMUM -> "maximale waarde van";
            case EERSTE -> "eerste van";
            case LAATSTE -> "laatste van";
            default -> null;
        };
        if (valueFold != null && elem == null && arguments.size() < 2)
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "RGS-071", params("fold", valueFold), f.getStart().getLine()));
        Expr function = new Expr.FunctionCall(kind, arguments, elem, emptyFallback, filter, null, type, source, operatorGran);
        // A count whose written source walks further than the one relation it counts, where the emitter would still
        // write a range: by the head's name or by the type pair of subject and element (IrEmitter.foldRange), the
        // other steps dropped without a word ("het aantal havenjutters van de haven van de jutter van de Vondst" went
        // out as a count over the jutter of the Vondst). A count's range is one relation from the subject (IR-SPEC
        // ch. 4.3, WF-102 (c)), so until a count over a path has a lowering it is refused, naming the path. Where the
        // emitter writes no range, the fold fences refuse the count in their own words; a set reading is the set
        // fence's.
        if (kind == Expr.FunctionKind.AANTAL && elem != null && f instanceof AantalFuncContext cf
                && cf.primaireExpressie().size() == 1 && arguments.get(0) instanceof Expr.AttributeReference sourceRead
                && !sourceRead.set()
                && dev.uchar.regelspraak.emit.IrEmitter.foldRefusalOf(model, currentSubject, (Expr.FunctionCall) function) == null) {
            List<String> steps = countSourceSteps(cf.primaireExpressie(0));
            if (steps != null && steps.size() > 1) {
                RegelSpraakModel.Diagnostic error = new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                        "RGS-072", params("expression", source.snippet(), "reason", countPath(steps, Msg.frontend("PART.count-norm-frontend"))),
                        f.getStart().getLine());
                if (!model.diagnostics.contains(error)) model.diagnostics.add(error);
            }
        }
        // §13.4.17.13: the wortel's afronding is MANDATORY and rounds the ROOT (it is a sibling of the
        // radicand, not inside it). It was parsed but dropped; wrap the function so the rounding survives.
        if (f instanceof WortelFuncContext w) return rounded(function, w.afronding(), source);
        return function;
    }

    /**
     * The three ways a MEMBER fold ("de som/minimale waarde/… van &lt;attr&gt; van alle &lt;elem&gt; van
     * &lt;onderwerp&gt;") can bind a DIFFERENT collection than the sentence names. All three end in the honest
     * marker with a WARNING — the {@link Expr.Unresolved} idiom this resolver already uses for a fold source
     * it cannot bind — never in a fold over a guessed relation. Returns the marker, or null when the source
     * binds what it says. Each marker carries {@code reden = null}: the WARNING beside it IS its reason, and
     * the survivor report must not say it twice.
     *
     * <p>The marker carries the aggregation's OWN result type, not the unknown sentinel: IR-SPEC ch. 2.4
     * defines Onopgelost as "a well-formed marker with a reason, not a type" and WF-044's fence is
     * behavioural, so a refused fold must leave a WELL-FORMED document that faults observably (WF-044) in
     * every executor — the red-zone shape, not an ill-formed one.
     *
     * <p>(a) A written CHAIN that does not become a path: a NESTED set step ("… van alle X van alle Y van
     * de Z"), or any chain of three segments or more ("… van alle X van de Y van de Z" too) that the walk
     * declines. The uchar wire carries a written multi-step range as {@code sourcePath} (IR-SPEC ch. 4.3,
     * WF-102) where {@link #rangePath} binds it off the chain, and such a chain returns before this route (the
     * first check below). A chain it declines has the one-step spelling alone, one {@code elementType} plus one
     * {@code sourceRole}, and the emitter's derivation ranges from the RULE SUBJECT to the innermost element
     * type — so the written steps disappear without a trace and the fold runs over the wrong set. Where no
     * relation joins subject and element the emitter refuses a fold it cannot bind; where one happens to exist it is verifier-CLEAN and silently wrong,
     * which is the shape this refusal exists for. The WARNING names where the walk stopped
     * ({@link #walkRange}): the step that binds no role and why, an anchor that is not the subject, or a
     * landing on another type than the element type.
     *
     * <p>(b) An ANCHOR that is not the subject under a one-step range ("… van alle X van de Y"), the same
     * loss along a third route: the fold source names one relation FROM THE RULE SUBJECT, so an anchor that is
     * not the subject drops its step just as silently — and here the written element name may legitimately
     * name the subject-side role, so (c) does not see it either. The anchor must therefore NAME the subject's own
     * object type, literally. An anchor that resolves to no object type at all (a role name, "alle vondsten
     * van de aanmelder") is left alone: resolving a role anchor is the wire question, not this refusal.
     *
     * <p>(c) A by-PAIR binding the written name contradicts. {@code connectingRoleBinding} searches on
     * TYPES only; with exactly one fact type joining subject and element it always wins, whatever role the
     * sentence named. Dropping the binding does not help — measured: a bindingless fold over a uniquely
     * related pair is verifier-clean and every executor re-derives the same wrong relation — so the
     * resolution itself must decline. The escape is the case where the author claimed no role at all: a
     * written name that IS an object type's own name or its declared plural.
     */
    private Expr checkFoldSource(FunctieAanroepContext f, Expr.AttributeReference source, Datatype type) {
        // WF-102 (2026-09-11): a range the resolver RESOLVED is carried on the wire, so there is nothing
        // left to refuse. Routes (a) and (b) below exist because a written range without a path would lose
        // its steps; where one is bound they would refuse an expressible sentence, and (c) has nothing to
        // contradict — a bound path makes no by-pair guess. The refusals stay for every chain that declines.
        if (source.rangePath() != null && !source.rangePath().isEmpty()) return null;
        OnderwerpContext o = foldSourceSubject(f);
        if (o == null) return null;
        // (a): a written chain the walk declined, and every nested set step. The refusal names where the chain
        // stopped: the step that binds no role and why, an anchor that is not the subject, a landing on another
        // type. A nested chain the walk does not decide has no anchor it can read: fewer than three segments,
        // or a last segment that is no name.
        RangeWalk walk = walkRange(o);
        if (walk.refusal() != null || nestedSetStep(o) != null) {
            Object reason = walk.refusal() != null ? walk.refusal()
                    : Msg.frontend(currentSubject == null ? "RGS-073.not-at-subject" : "RGS-073.not-at-subject-named",
                            "range", src(o), "subject", currentSubject == null ? "" : currentSubject.name);
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-073", params("reason", reason, "exit", walk.exit(), "expression", source(f),
                            "exitClause", Msg.frontendIf(walk.exit() != null, "RGS-073.exit", "exit", walk.exit())),
                    f.getStart().getLine()));
            return new Expr.Unresolved(type, source(f), null);
        }
        // The emitter derives a sourceRole only for a navigation-free member fold over a resolved element type
        // (IrEmitter.foldRange); outside that shape there is no by-pair guess to contradict.
        if (source.objectType() == null || currentSubject == null) return null;
        if (source.navigationRole() != null || !source.navigationPath().isEmpty()) return null;
        String written = primaryName(o);
        // (b): the anchor the written chain hands the element to — the first hop past the element head,
        // read with the same name candidates the element type's own binding was tried with.
        int sp = written == null ? -1 : written.toLowerCase().indexOf(" van ");
        if (sp > 0)
            for (String candidate : RegelSpraakModel.nameCandidates(written.substring(sp + 5))) {
                ObjectType anchor = model.objectTypeByName(candidate);
                if (anchor == null) {
                    // WF-102's anchor rule (2026-09-11): a role anchor is admissible only where it denotes
                    // the SUBJECT, under ch. 4.1a's order. A far role — one reachable FROM the subject —
                    // denotes another instance, so binding the fold to the subject's own relation would
                    // fold the wrong set in silence; that is the shape refused here. A SELF-DENOTING name
                    // (zero far hits, and the subject type fills a role of that name) is the subject, and
                    // the chain simply starts there. A name that resolves to nothing at all keeps its
                    // pre-existing silence: there is nothing to say about it.
                    if (model.roleHitsToSubject(currentSubject, candidate) == 0) continue;
                    model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                            "RGS-074", params("expression", src(o), "role", candidate, "subject", currentSubject.name, "source", source(f)), f.getStart().getLine()));
                    return new Expr.Unresolved(type, source(f), null);
                }
                if (anchor == currentSubject) break;       // the chain starts at the subject, as the wire needs
                model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                        "RGS-075", params("expression", src(o), "anchor", anchor.name, "subject", currentSubject.name, "source", source(f)), f.getStart().getLine()));
                return new Expr.Unresolved(type, source(f), null);
            }
        // (c): the written element name, under every form the element type's own binding was tried with, so a
        // phrase that bound its type by dropping a leading qualifier is judged by that same form.
        List<String> names = RegelSpraakModel.nameCandidates(written);
        if (names.isEmpty()) return null;
        if (names.stream().anyMatch(n -> model.objectTypeByName(n) != null)) return null;
        // a type named in its declared plural ("alle Schuiten") claims no role either, as a navigated read by type
        // name does not: the pair binds where exactly one relation joins the two types. The whole written name only:
        // "vondsten" at the end of "geborgen vondsten" names the role, not the type
        if (model.objectTypes.values().stream().anyMatch(ot -> ot.plural != null
                && ot.plural.equalsIgnoreCase(names.get(0)))) return null;
        RegelSpraakModel.RoleBindingRef pair = model.connectingRoleBinding(currentSubject, source.objectType());
        if (pair == null || names.stream().anyMatch(n -> model.roleNamedAs(pair, n))) return null;
        model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                "RGS-076", params("writtenName", names.get(0), "subject", currentSubject.name, "objectType", source.objectType().name, "factType", pair.factType(), "role", pair.role(), "source", source(f)),
                f.getStart().getLine()));
        return new Expr.Unresolved(type, source(f), null);
    }

    /** The set-quantified onderwerp of an aggregation's OWN operand — the fold source as written. Found by
     *  descending {@code t}, stopping at a nested functieAanroep (which judges its own operand) and at a
     *  §5.8.2 leegwaardeAlternatief (the fallback is not the fold's source). Null when the operand names no
     *  set onderwerp — a count over an attribuut-position element word ("het aantal vondsten van de Jutter")
     *  reaches a to-ONE onderwerp and is left to its own derivation. */
    private OnderwerpContext foldSourceSubject(ParseTree t) {
        if (t instanceof AttribuutVanOnderwerpContext a && a.onderwerp() != null && isSetSubject(a.onderwerp()))
            return a.onderwerp();
        for (int i = 0; i < t.getChildCount(); i++) {
            ParseTree ch = t.getChild(i);
            if (ch instanceof FunctieAanroepContext || ch instanceof LeegwaardeAlternatiefContext) continue;
            OnderwerpContext o = foldSourceSubject(ch);
            if (o != null) return o;
        }
        return null;
    }

    /** The first set-quantified onderwerp NESTED under {@code o}'s "van"-chain, or null when every inner step
     *  is a single object. Walks the whole chain rather than only the next step, so "alle X van de Y van alle
     *  Z" is caught as surely as "alle X van alle Y van de Z". */
    private static OnderwerpContext nestedSetStep(OnderwerpContext o) {
        for (OnderwerpContext in = o.onderwerp(); in != null; in = in.onderwerp())
            if (isSetSubject(in)) return in;
        return null;
    }

    /** §13.4.17.51-.58: the raw source of an aggregation's dimensieselectie (som / min-max) or
     *  conditiebijexpressie (totaal / tijdsevenredig deel), or null when absent — used to mark the dropped
     *  temporal/dimensional restriction so the aggregation is not silently lowered as if unrestricted. */
    private Source dimensionOrConditionSource(FunctieAanroepContext f) {
        DimensieSelectieContext dim = f instanceof SomFuncContext s ? s.dimensieSelectie()
                : f instanceof MinMaxFuncContext m ? m.dimensieSelectie() : null;
        if (dim != null) return source(dim);
        ConditieBijExpressieContext cnd = f instanceof TotaalFuncContext t ? t.conditieBijExpressie()
                : f instanceof TijdsevenredigFuncContext tv ? tv.conditieBijExpressie() : null;
        return cnd != null ? source(cnd) : null;
    }

    /** §13.4.17.51-.54: resolve a SOM/MIN/MAX dimensieselectie to the concrete dimension and the ordered
     *  list of labels it ranges over, or null when it cannot be lowered — a null leaves the honest marker in
     *  place. It only lowers a fold over a SINGLE gedimensioneerd attribuut's labels, and only when the
     *  operand is the rule subject's OWN dimensioned attribute ({@code a.objectType() == huidigSubject} —
     *  the §5.8.5 case, "zijn betaalde belasting in jaar"). A NAVIGATED operand is deferred: a to-MANY read
     *  ("de prijs in jaar van alle deelnemers") would be a 2-level aggregation (over instances AND labels)
     *  that the single-Map fold cannot express — resolving it would drop the marker and let the instance
     *  accumulate sum Maps (a miscompile) — and a to-one navigated fold has no spec/corpus example yet. The
     *  attribute must carry exactly ONE dimension (the value space is a Map&lt;label,value&gt;; a
     *  multi-dimension attribute is keyed by label COMBINATIONS, §3.6, which the single-label fold would
     *  mis-read), the named/implied dimension must be that one, every label must be a declared value, and a
     *  "vanaf … t/m …" interval must not run backwards. (A conditiebijexpressie — temporal — never reaches
     *  here.) */
    private Expr.DimensionSelection resolveDimensionSelection(FunctieAanroepContext f, List<Expr> arguments) {
        DimensieSelectieContext dim = f instanceof SomFuncContext s ? s.dimensieSelectie()
                : f instanceof MinMaxFuncContext m ? m.dimensieSelectie() : null;
        if (dim == null) return null;
        if (arguments.size() != 1) return null;
        // §13.4.17.53 the EXPLICIT "over de <meervoud> vanaf … t/m …" form: OVER and DE are name words
        // (attribute names like "belasting over de afgelopen vier jaar" need them), so the greedy attribute
        // bareName absorbs the " over de <meervoud>" tail — the possessive operand then fails to resolve
        // AT ALL (an Expr.Unresolved) and the explicit form reaches here as the BARE interval. Re-split by
        // declared names (the §13.4-names design, like the objectcreatie/feitcreatie re-splits): peel the
        // tail only when it is a declared dimension's meervoud AND the peeled head is the subject's OWN
        // declared attribute ("zijn/haar …") dimensioned by exactly that dimension; the fold operand is
        // rebuilt clean (declared spelling + element datatype) in the argument list.
        RegelSpraakModel.Dimension explicit = null;
        if (dim.VANAF() != null && arguments.get(0) instanceof Expr.Unresolved on && currentSubject != null
                && on.source() != null && on.source().snippet().matches("(?is)(zijn|haar)\\s+.*")) {
            int snee = on.source().snippet().toLowerCase().lastIndexOf(" over de ");
            if (snee >= 0) {
                String head = on.source().snippet().substring(0, snee).replaceFirst("^(?is)(zijn|haar)\\s+", "");
                String mv = on.source().snippet().substring(snee + " over de ".length()).trim();
                RegelSpraakModel.Dimension dd = model.dimensionByPlural(mv);
                List<String> headDims = dd == null ? List.of() : currentSubject.dimensionsOf(head);
                if (dd != null && headDims.size() == 1 && headDims.get(0).equalsIgnoreCase(dd.name())) {
                    // the declared spelling may sit on an ancestor: headDims came from the chain-aware
                    // dimensionsOf, so the own map alone would fall back to the WRITTEN form and put a
                    // name on the wire that no declaration carries
                    String own = currentSubject.attributeName(head);
                    String declared = own != null ? own : head;
                    arguments.set(0, new Expr.AttributeReference(declared, currentSubject, "zijn", null,
                            currentSubject.attribute(declared), on.source(), false));
                    explicit = dd;
                }
            }
        }
        if (!(arguments.get(0) instanceof Expr.AttributeReference a) || a.objectType() == null) return null;
        if (a.objectType() != currentSubject) return null;   // only the subject's OWN dimensioned attribuut folds; a navigated read is deferred
        List<String> attrDims = a.objectType().dimensionsOf(a.attribute());
        if (attrDims.size() != 1) return null;   // not gedimensioneerd, or multi-dimension (composite key) — deferred

        RegelSpraakModel.Dimension d;
        List<String> labels;
        if (dim.VANAF() != null) {
            // §5.8.5 bare interval: the dimension is left implicit (unambiguous — the attribute carries
            // exactly one dimension, guaranteed above); the re-split §13.4.17.53 form carries it explicitly.
            d = explicit != null ? explicit : model.dimensionByName(attrDims.get(0));
            if (d == null) return null;
            labels = interval(d, src(dim.dimensieWaarde(0)), src(dim.dimensieWaarde(1)));
        } else if (dim.aggregerenOverAlleDimensies() != null) {
            d = selectedDimension(dim.aggregerenOverAlleDimensies().dimensieNaamMeervoud(), attrDims);
            if (d == null) return null;
            labels = new ArrayList<>(d.values());
        } else if (dim.aggregerenOverVerzameling() != null) {
            AggregerenOverVerzamelingContext v = dim.aggregerenOverVerzameling();
            d = selectedDimension(v.dimensieNaamMeervoud(), attrDims);
            if (d == null) return null;
            labels = interval(d, src(v.dimensieWaarde(0)), src(v.dimensieWaarde(1)));
        } else if (dim.aggregerenOverBereik() != null) {
            AggregerenOverBereikContext v = dim.aggregerenOverBereik();
            d = selectedDimension(v.dimensieNaamMeervoud(), attrDims);
            if (d == null) return null;
            labels = range(d, v.dimensieWaarde());
        } else {
            return null;
        }
        if (labels == null || labels.isEmpty()) return null;
        return new Expr.DimensionSelection(d.name(), labels, source(dim));
    }

    /** The dimension an over-form names by its plural, but only when the operand attribute is actually
     *  gedimensioneerd with it (else summing a different axis would be silently wrong). */
    private RegelSpraakModel.Dimension selectedDimension(DimensieNaamMeervoudContext mv, List<String> attrDims) {
        RegelSpraakModel.Dimension d = model.dimensionByPlural(src(mv));
        if (d == null) return null;
        return attrDims.stream().anyMatch(n -> n.equalsIgnoreCase(d.name())) ? d : null;
    }

    /** §5.8.5 the contiguous label interval [van .. t/m] in the dimension's DECLARED order (§13.3.7.5),
     *  or null when a bound is not a declared label or the interval runs backwards. */
    private static List<String> interval(RegelSpraakModel.Dimension d, String van, String tm) {
        int i = labelIndex(d, van), j = labelIndex(d, tm);
        if (i < 0 || j < 0 || i > j) return null;
        return new ArrayList<>(d.values().subList(i, j + 1));
    }

    /** §13.4.17.54 the explicit "in {…}" label set, mapped to the declared label spelling, or null when
     *  any named label is not a value of the dimension. */
    private List<String> range(RegelSpraakModel.Dimension d, List<DimensieWaardeContext> values) {
        List<String> result = new ArrayList<>();
        for (DimensieWaardeContext w : values) {
            List<String> labels = memberLabels(d, src(w));
            if (labels == null) return null;
            result.addAll(labels);
        }
        return result;
    }

    /** §13.4.17.54: the declared labels one written member of an "in {…}" set denotes. The grammar admits "en"
     *  inside a label, so "{voorjaar en najaar}" reaches here as the ONE member "voorjaar en najaar". A member
     *  that is a declared label is read whole; any other member is read as labels joined by "en" when exactly one
     *  way of grouping its words gives declared labels only. Null when no reading, or more than one, does. */
    private static List<String> memberLabels(RegelSpraakModel.Dimension d, String written) {
        int idx = labelIndex(d, written);
        if (idx >= 0) return List.of(d.values().get(idx));
        String[] parts = written == null ? new String[0] : written.trim().split("\\s+en\\s+");
        if (parts.length < 2) return null;
        List<List<String>> readings = new ArrayList<>();
        groupings(d, parts, 0, new ArrayList<>(), readings);
        return readings.size() == 1 ? readings.get(0) : null;
    }

    /** Every grouping of {@code parts[van..]} into consecutive runs that are declared labels, appended to
     *  {@code result}. */
    private static void groupings(RegelSpraakModel.Dimension d, String[] parts, int van, List<String> until,
                                   List<List<String>> result) {
        if (van == parts.length) { result.add(List.copyOf(until)); return; }
        for (int end = van + 1; end <= parts.length; end++) {
            int idx = labelIndex(d, String.join(" en ", java.util.Arrays.copyOfRange(parts, van, end)));
            if (idx < 0) continue;
            until.add(d.values().get(idx));
            groupings(d, parts, end, until, result);
            until.remove(until.size() - 1);
        }
    }

    private static int labelIndex(RegelSpraakModel.Dimension d, String label) {
        String norm = label == null ? "" : label.trim();
        for (int i = 0; i < d.values().size(); i++)
            if (d.values().get(i).equalsIgnoreCase(norm)) return i;
        return -1;
    }

    /** The §5.6 "die/dat &lt;predicaat&gt;" subselectie filtering a SINGLE-operand set-aggregation's
     *  element ("het aantal / de som / de minimale waarde van … deelnemers van de groep die een kind
     *  zijn" — only the kind-deelnemers). Resolved against the ELEMENT type (not the rule subject): a
     *  kenmerk of the element becomes a real {@link Condition.CharacteristicCheck} (lowered as an accumulate-source
     *  constraint); anything else stays {@link Condition.Unresolved}, so the aggregation is a visible
     *  marker rather than a silently-wrong total over EVERY instance.
     *
     *  Only a single operand is considered: a multi-operand telling (the §5.8.1 union) or a min/max
     *  VALUE-fold has no single element filter — there a die belongs to one operand's own navigation
     *  (e.g. "de eerste deelnemer die …"), not the aggregation, and markering on it would kill a
     *  working accumulate (see p20b). With one operand a generic subtree scan is safe. */
    /** §7.1/§7.3.2 the "gedurende de tijd dat <voorwaarde>" restriction on "het totaal van <temporeel attribuut>"
     *  OR "het tijdsevenredig deel per <maand|jaar> van <temporeel attribuut>", when it is a SIMPLE single-period
     *  temporal-kenmerk membership of the subject ("hij rechthebbend is", or the verbatim example's bezittelijk
     *  "hij een recht op belastingvermindering heeft"), resolved to a {@link Condition.CharacteristicCheck} the transpiler clips
     *  the value timeline by. Tightly gated: only a TOTAAL or TIJDSEVENREDIG_DEEL over the subject's OWN single-period
     *  temporal attribute (the operand the operator ranges over — the totaled attribute, or the §7.3.2 base whose
     *  value-timeline §5.1.2 makes intrinsic), with a non-navigated "is/heeft <kenmerk>" condition naming a
     *  single-period temporal kenmerk of the subject. BOTH membership verbs bind: a bijvoeglijk/zelfstandig kenmerk
     *  reads "is <k>", a bezittelijk one "heeft (een) <k>" (§3.5) — the verb is not the discriminator; the
     *  granularity check below is (a role/possession "heeft een <rol>" names no temporal kenmerk, so it still returns
     *  null). Everything else (a navigated/compound/comparison/multi-period condition, a non-temporal or navigated/
     *  scalar operand) returns null and stays an Unresolved marker — so a SCALAR (parameter) base, whose §5.1.2
     *  value-timeline the model cannot carry, never lowers as a silently-constant proration. */
    private Condition duringCharacteristicCheckFilter(FunctieAanroepContext f, Expr.FunctionKind kind, List<Expr> arguments) {
        if ((kind != Expr.FunctionKind.TOTAAL && kind != Expr.FunctionKind.TIJDSEVENREDIG_DEEL)
                || arguments.size() != 1 || currentSubject == null) return null;
        Expr operand = arguments.get(0);
        // The operand the operator ranges over must be a single-period TIMELINE: the subject's own temporal attribute
        // (totaal + tijdsevenredig), OR — for §7.3.2 tijdsevenredig only — a TEMPORAL PARAMETER (the spec's STANDAARD
        // BELASTINGVERMINDERING base, "kan jaarlijks wijzigen"), whose runtime value is itself a timeline (§5.1.2).
        boolean okOperand = operand instanceof Expr.AttributeReference a && a.navigationRole() == null && !a.set()
                && a.objectType() == currentSubject && currentSubject.granularityOf(a.attribute()) != null
                && currentSubject.timelineCountOf(a.attribute()) == null;
        if (!okOperand && kind == Expr.FunctionKind.TIJDSEVENREDIG_DEEL
                && operand instanceof Expr.ParameterRef p && p.parameter().granularity() != null)
            okOperand = true;
        // §7.3.2 a TO-ONE NAVIGATED tijdsevenredig base ("de X van zijn reis") — a single-period temporal attribute of
        // the RELATED type. Its value-timeline is intrinsic (§5.1.2), read root-guarded by both back-ends (navReader /
        // the DRL root-guarded getter). The gedurende condition still ranges over the rule SUBJECT, so it resolves
        // against huidigSubject below; only the base reads through the navigation. (A to-many base markers in lowering.)
        if (!okOperand && kind == Expr.FunctionKind.TIJDSEVENREDIG_DEEL
                && operand instanceof Expr.AttributeReference a && a.navigationRole() != null && !a.set()
                && a.objectType() != null && a.objectType().granularityOf(a.attribute()) != null
                && a.objectType().timelineCountOf(a.attribute()) == null)
            okOperand = true;
        if (!okOperand) return null;
        ConditieBijExpressieContext cnd = f instanceof TotaalFuncContext t ? t.conditieBijExpressie()
                : f instanceof TijdsevenredigFuncContext tv ? tv.conditieBijExpressie() : null;
        if (cnd == null) return null;
        // §7.1 a SIMPLE (single) membership, or §8.4.2 a COMPOUND ("er aan alle/ten minste 1 van de volgende
        // voorwaarden wordt voldaan: …") of such memberships. Each bullet must be a single-period temporal kenmerk
        // of the subject; anything else (a comparison, a non-temporal/navigated kenmerk, another kwantor, a nested
        // compound) leaves the WHOLE condition an Unresolved marker — so the spec's own mixed temporal+non-temporal
        // §8.4 example never silently drops its non-temporal predicate.
        if (cnd.toplevelElementaireVoorwaarde() != null) {
            ToplevelElementaireVoorwaardeContext c = cnd.toplevelElementaireVoorwaarde();
            // §7.1 a NAVIGATED temporal kenmerk ("zijn reis een recht op X heeft") FIRST — it inspects the onderwerp,
            // so when the navigated type shares the subject's type (a self-relation) the navigation is not shadowed by
            // the subject's own same-named kenmerk; temporalCharacteristicCheckFromOperator (own) only sees the operator's bareName.
            Condition nav = duringNavigationCharacteristicCheck(c, source(cnd));
            if (nav != null) return nav;
            // If the onderwerp IS a navigation that the navigated path could NOT structure (a to-many role, a negated
            // or non-temporal kenmerk), MARKER — never fall through to temporalCharacteristicCheckFromOperator, which inspects only
            // the operator's bareName and would silently bind the SUBJECT's own same-named kenmerk (a self-relation
            // shadow, a silently-wrong clip).
            if (c.expressie() != null && build(c.expressie()) instanceof Expr.AttributeReference a && a.navigationRole() != null)
                return null;
            Condition.CharacteristicCheck k = temporalCharacteristicCheckFromOperator(c.toplevelOperator(), null, source(cnd));  // §8.3.1 toplevel = vragende only
            if (k != null) return k;
            return duringComparison(c, source(cnd));   // §7.1 a temporal-attribute COMPARISON condition (EUR71)
        }
        if (cnd.toplevelSamengesteldeVoorwaarde() != null)
            return duringCompound(cnd.toplevelSamengesteldeVoorwaarde(), source(cnd));
        return null;
    }

    /** §7.1 structure a "gedurende de tijd dat &lt;temporeel attribuut&gt; &lt;vergelijking&gt; &lt;scalar&gt;"
     *  enkelvoudige condition (the EUR71 shape, e.g. "gedurende de tijd dat het inkomen groter is dan 1000") into a
     *  {@link Condition.Comparison} the transpiler clips the value timeline by (its boolean timeline). Built only
     *  when ONE operand is a single-period temporal attribute of the subject (the condition varies over time) and the
     *  form is a verb-split comparison; else null (the caller markers). The transpiler validates numeric type / units. */
    private Condition duringComparison(ToplevelElementaireVoorwaardeContext c, Source source) {
        if (c.expressie() == null || c.toplevelOperator() == null
                || c.toplevelOperator().vergelijkingsTekenGesplitst() == null || c.toplevelOperator().expressie() == null)
            return null;
        Expr lhs = build(c.expressie());
        Expr rhs = build(c.toplevelOperator().expressie());
        if (!isTemporalSubjectAttr(lhs) && !isTemporalSubjectAttr(rhs)) return null;   // a static single condition is no gedurende
        Condition.Operator op = RuleChecker.toOperator(RuleChecker.cmpOf(c.toplevelOperator().vergelijkingsTekenGesplitst()));
        return comparison(op, lhs, rhs, null, source);
    }

    /** A single-period temporal attribute of the rule subject (the operand a §7.1 gedurende comparison varies over). */
    private boolean isTemporalSubjectAttr(Expr e) {
        return currentSubject != null && e instanceof Expr.AttributeReference a && a.navigationRole() == null && !a.set()
                && a.objectType() == currentSubject && currentSubject.granularityOf(a.attribute()) != null
                && currentSubject.timelineCountOf(a.attribute()) == null;
    }

    /** §7.1 structure a "gedurende de tijd dat &lt;zijn rol&gt; (een/het) &lt;temporeel kenmerk&gt; heeft/is"
     *  NAVIGATED temporal-kenmerk condition (e.g. "gedurende de tijd dat zijn reis een recht op korting heeft") into a
     *  navigated {@link Condition.CharacteristicCheck} the transpiler clips by the to-one role's kenmerk timeline. The onderwerp is
     *  a to-one role navigation; the kenmerk must be a SINGLE-PERIOD temporal kenmerk of the navigated type. A negated
     *  form, a to-many role, or a non-temporal kenmerk -> null (the caller markers). */
    private Condition duringNavigationCharacteristicCheck(ToplevelElementaireVoorwaardeContext c, Source source) {
        ToplevelOperatorContext top = c.toplevelOperator();
        if (c.expressie() == null || top == null || top.kaleNaam() == null) return null;
        boolean membership = top.IS() != null || top.ZIJN() != null || top.HEEFT() != null || top.HEBBEN() != null;
        if (!membership) return null;                                                   // not an is/heeft kenmerkcheck
        if (!(build(c.expressie()) instanceof Expr.AttributeReference a) || a.navigationRole() == null || a.objectType() == null)
            return null;                                                                // not a to-one role navigation
        ObjectType target = model.roleTargetTypeToSubject(a.objectType(), a.attribute());
        if (target == null || model.toManyRole(a.objectType(), target, a.navigationRole())) return null;   // to-many -> marker
        String characteristic = CharacteristicMatcher.find(target, stripArticle(src(top.kaleNaam())));
        if (characteristic == null) return null;                                               // not a kenmerk of the navigated type
        if (target.characteristicGranularityOf(characteristic) == null || target.characteristicTimelineCountOf(characteristic) != null)
            return null;                                                                // must be a single-period temporal kenmerk
        return new Condition.CharacteristicCheck(characteristic, a.navigationRole(), false, source);
    }

    /** Resolve an elementaire "is/heeft (geen/niet) (een/het) &lt;kenmerk&gt;" membership operator into a
     *  single-period temporal {@link Condition.CharacteristicCheck} of the subject (negation + article-strip handled), or null
     *  when it is not one — a comparison, a non-temporal / multi-period / navigated kenmerk, or a role/possession.
     *  Shared by the single gedurende condition and each bullet of a compound one. */
    private Condition.CharacteristicCheck temporalCharacteristicCheckFromOperator(ToplevelOperatorContext top, NonToplevelOperatorContext non, Source source) {
        KaleNaamContext kn; boolean isVerb, hasVerb;
        if (top != null) {
            kn = top.kaleNaam(); isVerb = top.IS() != null || top.ZIJN() != null;
            hasVerb = top.HEEFT() != null || top.HEBBEN() != null;
        } else if (non != null) {
            kn = non.kaleNaam(); isVerb = non.IS() != null || non.ZIJN() != null;
            hasVerb = non.HEEFT() != null || non.HEBBEN() != null;
        } else return null;
        if (kn == null || (!isVerb && !hasVerb)) return null;         // an "is/heeft <kenmerk>" membership (§3.5)
        // §8.1.8.1/.2 a "geen"/"niet" before the kenmerk negates the membership ("hij geen recht ... heeft" / "hij
        // niet rechthebbend is"); the negator lexes into the bareName (no GEEN/NIET token here), so try the
        // verbatim name first (a kenmerk literally beginning with geen/niet still binds positively), then a stripped
        // form. A negated gedurende clips the value timeline to the kenmerk's FALSE periods (the complement).
        String written = src(kn);
        boolean negation = false;
        String characteristic = CharacteristicMatcher.find(currentSubject, written);
        if (characteristic == null && (written.regionMatches(true, 0, "geen ", 0, 5)
                || written.regionMatches(true, 0, "niet ", 0, 5))) {
            characteristic = CharacteristicMatcher.find(currentSubject, written.substring(5).trim());
            negation = characteristic != null;
        }
        if (characteristic == null) {
            // §8.1.8.2 the CANONICAL bezittelijk form is "heeft HET <k>" (the §3.5 example "heeft het recht op
            // duurzaamheidskorting"): the article is absorbed into the bareName (only "een" is a separate token), so
            // a verbatim match misses it. Retry article-stripped — verbatim-first above keeps a kenmerk literally
            // named "het …" binding itself.
            String withoutArticle = written.replaceFirst("^(?i)(de|het|een)\\s+", "").trim();
            if (!withoutArticle.equals(written)) characteristic = CharacteristicMatcher.find(currentSubject, withoutArticle);
        }
        if (characteristic == null || currentSubject.characteristicGranularityOf(characteristic) == null
                || currentSubject.characteristicTimelineCountOf(characteristic) != null) return null;
        return new Condition.CharacteristicCheck(characteristic, null, negation, source);
    }

    /** §8.4.2 a COMPOUND tijdsafhankelijke conditie on a temporal totaal: an ALLE (intersect the true-period sets)
     *  or "ten minste 1" (union them) of bullets. Each bullet is a single-period temporal-kenmerk membership of the
     *  subject; an ALLE compound may ALSO mix in a NON-temporal comparison bullet (the spec's own §8.4 example,
     *  "zijn geboortedatum is eerder dan 1-1-2002") — built as a {@link Condition.Comparison} — which the transpiler
     *  lowers to a value-side static guard wrapping the clipped totaal ("de samengestelde conditie is als geheel wel
     *  tijdsafhankelijk"). A "ten minste 1" (OR) compound likewise admits a non-temporal comparison bullet — but its
     *  TRUE case WIDENS the clip to the full value span (the transpiler branches on the kwantor; the WRITE producers
     *  still marker an OR-mixed). Either compound must still carry at least one temporal kenmerk (the actual clip). Any
     *  OTHER kwantor (geen van de / ten hoogste / precies / ten minste &gt;1), a nested compound bullet, or a
     *  non-comparison non-temporal bullet returns null — the compound stays an Unresolved marker, never a dropped
     *  predicate. */
    private Condition duringCompound(ToplevelSamengesteldeVoorwaardeContext sv, Source source) {
        VoorwaardeKwantificatieContext q = sv.voorwaardeKwantificatie();
        if (q == null || sv.samengesteldeVoorwaardeOnderdeel() == null) return null;
        Condition.Quantifier quantifier;
        if (q.ALLE() != null) quantifier = Condition.Quantifier.ALLE;
        else if (q.TEN_MINSTE() != null && q.telwoord() != null && numeralValue(q.telwoord()) == 1)
            quantifier = Condition.Quantifier.TEN_MINSTE;                       // "ten minste 1" = OR (union)
        else return null;                                               // de / geen van de / ten hoogste / precies / ten minste >1
        List<Condition> parts = new ArrayList<>();
        int temporal = 0;
        for (GenesteVoorwaardeContext g : sv.samengesteldeVoorwaardeOnderdeel().genesteVoorwaarde()) {
            if (g.elementaireVoorwaarde() == null) return null;         // a nested compound bullet -> marker
            ElementaireVoorwaardeContext ev = g.elementaireVoorwaarde();
            Condition.CharacteristicCheck k = temporalCharacteristicCheckFromOperator(ev.toplevelOperator(), ev.nonToplevelOperator(), source(ev));
            if (k != null) { parts.add(k); temporal++; continue; }
            // §8.4 a non-temporal member (a COMPARISON) lowers to a static value-side guard. In an ALLE compound it
            // AND-gates the clipped totaal; in a "ten minste 1" (OR) compound a TRUE static member instead WIDENS the
            // result to the full value span (the transpiler branches on the kwantor). The WRITE producers still marker
            // an OR-mixed (conditieTijdreeks returns null) — only the totaal handles it.
            Condition.Comparison v = nonTemporalComparison(ev);
            if (v == null) return null;                                 // a non-comparison non-temporal bullet -> marker
            parts.add(v);
        }
        if (parts.size() < 2 || temporal == 0) return null;           // a single bullet is the elementaire path; a pure-static gedurende is not a clip
        return new Condition.Compound(quantifier, 1, parts, source);
    }

    /** §8.4 a NON-temporal COMPARISON bullet of a mixed ALLE gedurende ("zijn geboortedatum is eerder dan 1-1-2002")
     *  built as a {@link Condition.Comparison} (reusing RuleChecker's shared comparison-operator mapping), or null
     *  when the bullet is not a plain comparison (a predicate / kenmerk / role membership — deferred) OR when an
     *  operand is itself a TEMPORAL attribute (§8.4 admits the static member precisely because it "heeft geen
     *  verschillende waarden op verschillende momenten"; a comparison on a List&lt;Segment&gt; read is time-varying and
     *  would lower to a List-vs-scalar term that fails the whole DRL build — reject so the compound markers). */
    private Condition.Comparison nonTemporalComparison(ElementaireVoorwaardeContext ev) {
        Expr left = ev.expressie() != null ? build(ev.expressie()) : null;
        if (left == null) return null;
        Condition.Comparison v = null;
        if (ev.toplevelOperator() != null && ev.toplevelOperator().vergelijkingsTekenGesplitst() != null
                && ev.toplevelOperator().expressie() != null)
            v = comparison(RuleChecker.toOperator(RuleChecker.cmpOf(ev.toplevelOperator().vergelijkingsTekenGesplitst())),
                    left, build(ev.toplevelOperator().expressie()), source(ev));
        else if (ev.nonToplevelOperator() != null && ev.nonToplevelOperator().vergelijkingsTeken() != null
                && ev.nonToplevelOperator().expressie() != null)
            v = comparison(RuleChecker.toOperator(RuleChecker.cmpOf(ev.nonToplevelOperator().vergelijkingsTeken())),
                    left, build(ev.nonToplevelOperator().expressie()), source(ev));
        if (v == null || temporalAttributeOperand(v.left()) || temporalAttributeOperand(v.right())) return null;
        return v;
    }

    /** Whether an operand READS a TEMPORAL (tijdlijn) attribute ANYWHERE — its runtime value is a {@code List<Segment>},
     *  not a scalar, so it cannot be a §8.4 static comparison member. Recurses through arithmetic wrappers (a temporal
     *  read nested in "X plus 1" or an afronding/begrenzing is just as time-varying), so the whole comparison markers
     *  rather than leaking a List into a scalar guard term (which would break the DRL build). */
    private static boolean temporalAttributeOperand(Expr e) {
        if (e instanceof Expr.AttributeReference a)
            return a.objectType() != null && a.objectType().granularityOf(a.attribute()) != null;
        if (e instanceof Expr.Binary b) return temporalAttributeOperand(b.left()) || temporalAttributeOperand(b.right());
        if (e instanceof Expr.Rounding roundedExpr) return temporalAttributeOperand(roundedExpr.base());
        if (e instanceof Expr.Clamp bg) return temporalAttributeOperand(bg.base())
                || (bg.minimum() != null && temporalAttributeOperand(bg.minimum()))
                || (bg.maximum() != null && temporalAttributeOperand(bg.maximum()));
        if (e instanceof Expr.DimensionCellRead dc) return temporalAttributeOperand(dc.attribute());
        return false;
    }

    /** The integer value of a §13.4.14 telwoord (a digit literal or een/twee/drie/vier), or -1 if unrecognised. */
    private int numeralValue(TelwoordContext t) {
        if (t.NUMBER() != null) try { return Integer.parseInt(t.NUMBER().getText()); } catch (NumberFormatException e) { return -1; }
        if (t.EEN_TELWOORD() != null) return 1;
        if (t.TWEE_TELWOORD() != null) return 2;
        if (t.DRIE_TELWOORD() != null) return 3;
        if (t.VIER_TELWOORD() != null) return 4;
        return -1;
    }

    /** Every marker this method returns carries {@code reden = null}: its single caller ({@link
     *  #buildFunction}) tests the result for {@link Condition.Unresolved} and raises the WARNING, so the
     *  survivor report would only repeat it. */
    private Condition aggregationFilter(FunctieAanroepContext f, ObjectType elem, List<Expr> arguments) {
        if (elem == null) return null;
        java.util.List<ParseTree> operands = new ArrayList<>();
        for (int i = 0; i < f.getChildCount(); i++) {
            ParseTree ch = f.getChild(i);
            if (ch instanceof ExpressieContext || ch instanceof PrimaireExpressieContext) operands.add(ch);
        }
        if (operands.size() == 1) {
            PredicaatContext p = findSubselectionPredicate(operands.get(0));
            if (p == null) return null;
            consumed.add(p);
            // §13.4.13.4 the COMPOUND subselectie "die aan alle volgende voorwaarden voldoen: …" — resolve its bullets
            // against the ELEMENT (via the RuleChecker callback) into a Condition.Compound on the source pattern.
            if (p.samengesteldPredicaat() != null) {
                Frame frame = currentSubject == null ? null : new Frame(currentSubject, elem, source(p));
                Condition c = compoundFilter != null
                        ? inFilter(frame, p, elem, () -> compoundFilter.resolve(p.samengesteldPredicaat(), elem)) : null;
                return c != null ? c : new Condition.Unresolved(source(p), null);
            }
            Condition filter = identityFilter(p);
            if (filter == null) filter = elementCharacteristicCheckFilter(p, elem);
            // §5.6 getalpredicaat "die <op> <expr>" (e.g. "die kleiner zijn dan 18"): a comparison of the AGGREGATED
            // attribute (the LHS the operator folds) -> a Condition.Comparison the accumulate source pattern constrains.
            if (filter == null) filter = elementComparison(p, elem, arguments);
            return filter != null ? filter : new Condition.Unresolved(source(p), null);
        }
        // §5.8.1 union telling "het aantal X, Y en Z": the count is the UNION of the per-operand
        // sets — a disjunctively-filtered single accumulate. OR the per-operand kenmerk filters over
        // the shared element (an instance in several sets matches the OR once, so it is counted
        // once). Any operand without a resolvable kenmerk filter (a comparison, an unfiltered set,
        // a different element) leaves the whole union Unresolved -> a marker, never a wrong count.
        // (A min/max value-fold reaches here too with >1 operand, but its die is on a sub-operand
        // navigation, not the element — so it is excluded; only AANTAL unions.)
        //
        // NOT the §5.7 route, and dead for every concatenation that RESOLVES (card unie-telling). A
        // resolving concatenation becomes an InstanceSetUnion in buildFunction, which runs BEFORE this
        // method, so what reaches this branch is a concatenation buildInstanceUnion REFUSED. The
        // OR-filter form it builds is deliberately not a second spelling of the union (IR-SPEC ch.
        // 4.3); what it can reach never reaches disk.
        //
        // The branch stays (measured 2026-09-22): removing it shifts the marker-reason census
        // MarkerReasonPinTest pins (3 -> 2) and costs the `consumed` bookkeeping its consumption of
        // the operands' subselecties, which then report themselves as silently dropped — three
        // diagnoses where the ERROR says it once. The guard below raises the "die/dat wordt nog niet
        // omgezet" WARNING only when an operand really writes a die/dat, so the WARNING is true
        // wherever it fires and silent where it has nothing to report; a sentence WITH an
        // unresolvable subselectie keeps both its ERROR and its WARNING (measured, both probes).
        if (f instanceof AantalFuncContext) {
            // No operand writes a subselectie at all: there is nothing to OR and nothing to report, so
            // this branch has no business producing a marker whose only effect is a WARNING about a
            // die/dat the sentence does not carry. Null is "no filter", which is exactly true here.
            boolean hasSubselection = false;
            for (ParseTree op : operands) if (findSubselectionPredicate(op) != null) hasSubselection = true;
            if (!hasSubselection) return null;
            java.util.List<Condition> parts = new ArrayList<>();
            // account for EVERY operand's subselectie before the first one can bail out: the marker below
            // refuses the whole union, so the later operands' subselecties are refused with it and must
            // not read as silently dropped
            for (ParseTree op : operands) {
                PredicaatContext p = findSubselectionPredicate(op);
                if (p != null) consumed.add(p);
            }
            for (ParseTree op : operands) {
                // Every operand must range over the SAME element (a single accumulate has one source
                // pattern) AND resolve to a kenmerk filter of it. An operand over a DIFFERENT element
                // — even when it shares a kenmerk name — would otherwise have its foreign set silently
                // dropped and its filter OR-ed onto the wrong fact, so it leaves the whole union a marker.
                PredicaatContext p = findSubselectionPredicate(op);
                Condition k = p != null && operandElement(op) == elem ? elementCharacteristicCheckFilter(p, elem) : null;
                if (k == null) return new Condition.Unresolved(source(f), null);
                parts.add(k);
            }
            return new Condition.Compound(Condition.Quantifier.TEN_MINSTE, 1, parts, source(f));
        }
        return null;
    }

    /**
     * §5.6 / IR-SPEC ch. 4.5: the "die/dat &lt;predicaat&gt;" subselectie over the range of a §8.2
     * QUANTIFIER FOLD — the same field, the same reading and the same resolution as an aggregation's
     * (see {@link #aggregationFilter}), reached from {@link RuleChecker}'s lowering instead of from a
     * functieaanroep. Returns null when the operand writes no subselectie at all, a resolved
     * {@link Condition} when it writes one this resolver binds, and {@link Condition.Unresolved} when it
     * writes one it does not — the caller then DECLINES to lower, because a fold over the full range is
     * exactly the silent wrong set the field exists to prevent.
     *
     * <p>{@code operand} is the parse subtree of the QUANTIFIED operand specifically, never the whole
     * condition: the subselectie is written after the onderwerp ("… van alle vondsten van de Jutter die
     * aangemeld zijn") but selects the quantified MEMBERS, so it must be looked for on the side the
     * quantifier is on. A die on the other operand belongs to that operand's own navigation.
     */
    /** The outcome of resolving a §5.6 subselectie on the quantifier-fold position: exactly one of the
     *  two is non-null. {@code reason} is the sentence the caller's refusal quotes, so the refusal names
     *  the axis the subselectie was declined on rather than one generic text for every shape. */
    record Subselection(Condition filter, Msg reason) {}

    /**
     * Give a subselectie back to {@code checkPartSelections}. {@link #subselection} books the predicate as
     * CONSUMED before it knows whether the fold will take it, and on the COMPARISON route that is right:
     * a declined fold leaves the set reading standing, so the set fence raises the ERROR that blocks the
     * document and the subselectie is refused WITH it. On the MEMBERSHIP route there is no set reading
     * and hence no fence ERROR — a declined fold leaves nothing but a marker — so the predicate has to go
     * back on the refusal list or the sentence falls silent, which is the one outcome
     * {@code checkPartSelections} exists to prevent ("ERROR, not WARNING, and the difference is the
     * point"). Measured regression, fixed 2026-09-19.
     */
    void returnSubselection(ParseTree operand) {
        if (operand == null) return;
        PredicaatContext p = findSubselectionPredicate(operand);
        if (p != null) consumed.remove(p);
    }

    Subselection subselection(ParseTree operand, ObjectType elem, Expr quantified) {
        if (elem == null || operand == null) return null;
        PredicaatContext p = findSubselectionPredicate(operand);
        if (p == null) return null;
        consumed.add(p);
        if (p.samengesteldPredicaat() != null) {
            Frame frame = currentSubject == null ? null : new Frame(currentSubject, elem, source(p));
            Condition c = compoundFilter != null
                    ? inFilter(frame, p, elem, () -> compoundFilter.resolve(p.samengesteldPredicaat(), elem)) : null;
            return c != null ? new Subselection(c, null)
                    : new Subselection(null, Msg.frontend("PART.subselection-no-compound", "element", elem.name));
        }
        Condition filter = identityFilter(p);
        if (filter == null) filter = elementCharacteristicCheckFilter(p, elem);
        if (filter != null) return new Subselection(filter, null);
        // §5.6 getalpredicaat "die <op> <expr>" names no attribute of its own, so elementComparison
        // re-anchors the FOLD's operand as its left side. When that operand is TIJDSAFHANKELIJK the filter
        // has no reading — a timeline in a scalar slot (ch. 3.3) — and it is refused below. The matrix of an
        // unqualified fold is an ordinary condition and the operand is usually scalar, so the filter binds
        // here; the refusal is keyed to the ATTRIBUTE's granularity rather than to the position.
        // A MEMBERSHIP fold (the kenmerkcheck over a quantified subject) quantifies no attribute at all,
        // so there is no operand for a getalpredicaat to re-anchor on; it falls into the reason below.
        filter = elementComparison(p, elem, quantified == null ? List.of() : List.of(quantified));
        if (filter == null)
            return new Subselection(null, Msg.frontend("PART.subselection-no-predicate", "element", elem.name));
        if (quantified instanceof Expr.AttributeReference q && q.objectType() != null
                && q.objectType().granularityOf(q.attribute()) != null)
            return new Subselection(null, Msg.frontend("PART.subselection-time-dependent", "attribute", q.attribute()));
        return new Subselection(filter, null);
    }

    /** The element type a count operand ("deelnemers van …") ranges over, or null when it is not an
     *  "&lt;attribuut&gt; van &lt;onderwerp&gt;" naming a countable element — used to confirm every operand
     *  of a §5.8.1 union shares one element before OR-ing their filters. */
    private ObjectType operandElement(ParseTree op) {
        if (op instanceof PrimaireExpressieContext pe && pe.attribuutVanOnderwerp() != null
                && pe.attribuutVanOnderwerp().attribuutMetLidwoord() != null
                && pe.attribuutVanOnderwerp().attribuutMetLidwoord().kaleNaam() != null)
            return countElement(src(pe.attribuutVanOnderwerp().attribuutMetLidwoord().kaleNaam()));
        return null;
    }

    /** Why the §5.7 union route did not open for the count being built — the Dutch context of
     *  {@link #countArityError}'s refusal, so one diagnostic says both what the wire refuses (an
     *  operand list under a count, in the verifier's sentence) and why the form that WOULD have carried
     *  this sentence does not apply. Set by {@link #buildInstanceUnion} on every null return and read immediately
     *  after, the {@code aggregatieReden} contract the Drools backend uses on the same kind of question. */
    private Msg unionRefusal;

    /** §5.7/§4.11 build the CONCATENATION of subject expressions under "het aantal" as an
     *  {@link Expr.InstanceSetUnion} — one selection per operand, each with its own population, its
     *  own one-step relation and its own die/dat subselection. Null when the route does not open,
     *  with {@link #unionRefusal} carrying the reason unless the operands are not subject
     *  expressions at all (there the arity refusal says everything).
     *
     *  <p>What it does NOT build: a union over two POPULATIONS (WF-110 (d)), a selection whose
     *  relation is not uniquely derivable (WF-110 (e) — a union names each selection's relation and
     *  never derives one from the type pair, because every selection here shares one pair), and a
     *  selection whose "die/dat" does not resolve to a characteristic of the element. That last one
     *  is a refusal rather than a dropped filter: a silently unfiltered selection counts the wrong
     *  number, and the union ABSORBS it (an unfiltered selection is a superset of every filtered
     *  one over the same relation), so the error would be invisible in the answer. */
    private Expr buildInstanceUnion(FunctieAanroepContext f) {
        unionRefusal = null;
        java.util.List<ParseTree> operands = new ArrayList<>();
        for (int i = 0; i < f.getChildCount(); i++) {
            ParseTree ch = f.getChild(i);
            if (ch instanceof ExpressieContext || ch instanceof PrimaireExpressieContext) operands.add(ch);
        }
        if (operands.size() < 2) return null;
        ObjectType shared = null;
        java.util.List<Expr.InstanceSetUnion.Branch> branches = new ArrayList<>();
        java.util.List<PredicaatContext> seen = new ArrayList<>();
        for (int i = 0; i < operands.size(); i++) {
            ParseTree op = operands.get(i);
            ObjectType el = operandElement(op);
            if (el == null) return null;          // a VALUE operand: the arity refusal owns this sentence
            if (shared == null) shared = el;
            else if (shared != el) {
                unionRefusal = Msg.frontend("PART.union-populations", "first", shared.name, "second", el.name);
                return null;
            }
            // a selection over a written path would take its range from the type pair below, dropping its steps
            List<String> steps = op instanceof PrimaireExpressieContext pe ? countSourceSteps(pe) : null;
            if (steps != null && steps.size() > 1) {
                unionRefusal = Msg.frontend("PART.union-selection-path", "n", i + 1, "source", src((ParserRuleContext) op),
                        "path", countPath(steps, Msg.frontend("PART.count-norm-selection")));
                return null;
            }
            String word = writtenElementWord(op);
            RegelSpraakModel.RoleBindingRef rb =
                    currentSubject == null || word == null ? null : model.elementRoleBinding(currentSubject, word);
            // The written name did not bind. That is not yet a refusal: elementRoleBinding answers the
            // BY-NAME question and answers it narrowly on purpose — only a declared singular or a
            // declared (mv:) plural binds, never a guessed one — and the §13.3.9.1 cardinality sentence
            // ("Eén vinder meldt meerdere vondsten") writes a plural the model does NOT register as the
            // role's declared one. Measured 2026-09-22: it sets `meervoudig` and drops the spelling, and
            // registering it instead would both move a golden's bytes and destroy the WF-034 witness
            // `jutterswet-kaveltelling`, whose whole point is that such a plural does not bind.
            //
            // So fall back to the TYPE PAIR — the unique fact-type role reaching the element from the
            // subject — which is the derivation the ONE-selection count leans on too. WF-110 (e) is
            // unmoved by this: it forbids the WIRE from omitting the relation, so that a consumer never
            // derives one; how the PRODUCER arrives at the relation it writes is the producer's own
            // business, and it writes the result out explicitly either way.
            if (rb == null && currentSubject != null) rb = model.connectingRoleBinding(currentSubject, el);
            if (rb == null) {
                unionRefusal = Msg.frontend("PART.union-relation", "n", i + 1, "word", word);
                return null;
            }
            PredicaatContext p = findSubselectionPredicate(op);
            Condition filter = null;
            if (p != null) {
                filter = elementCharacteristicCheckFilter(p, el);
                if (filter == null) {
                    unionRefusal = Msg.frontend("PART.union-subselection", "n", i + 1, "source", src(p), "element", el.name);
                    return null;
                }
                seen.add(p);
            }
            branches.add(new Expr.InstanceSetUnion.Branch(el, rb, filter, source((ParserRuleContext) op)));
        }
        consumed.addAll(seen);   // only once the whole union stands: a refused union hands its die back
        return new Expr.InstanceSetUnion(java.util.List.copyOf(branches), source(f));
    }

    /** The written element word of a count operand ("aangemelde vondsten" in "aangemelde vondsten van
     *  de Jutter") — the name {@link RegelSpraakModel#elementRoleBinding} resolves the selection's
     *  relation from, and the same word {@link #operandElement} resolves its population from. */
    private String writtenElementWord(ParseTree op) {
        if (op instanceof PrimaireExpressieContext pe && pe.attribuutVanOnderwerp() != null
                && pe.attribuutVanOnderwerp().attribuutMetLidwoord() != null
                && pe.attribuutVanOnderwerp().attribuutMetLidwoord().kaleNaam() != null)
            return src(pe.attribuutVanOnderwerp().attribuutMetLidwoord().kaleNaam());
        return null;
    }

    /** The relations the written source of a count operand walks outward from the rule subject: the parts of its name
     *  at every 'van', then those of its onderwerp, the anchor dropped where it denotes the subject (ch. 4.1a, as in
     *  {@link #anchorsSubject}); a possessive's noun is a step, "zijn" being the subject. One part is the one
     *  relation a count counts. Null where the operand is no "&lt;naam&gt; van &lt;onderwerp&gt;" phrase this walk
     *  reads, or no subject is bound. */
    private List<String> countSourceSteps(PrimaireExpressieContext pe) {
        AttribuutVanOnderwerpContext a = pe.attribuutVanOnderwerp();
        if (currentSubject == null || a == null || a.attribuutMetLidwoord() == null
                || a.attribuutMetLidwoord().kaleNaam() == null || a.onderwerp() == null) return null;
        List<String> parts = new ArrayList<>();
        nameParts(src(a.attribuutMetLidwoord().kaleNaam()), parts);
        boolean possessive = false;
        for (OnderwerpContext rest = a.onderwerp(); rest != null; rest = rest.onderwerp()) {
            String kn = rest.kaleNaam() != null ? src(rest.kaleNaam()) : primaryName(rest);
            if (kn == null) return null;
            nameParts(kn, parts);
            possessive = rest.kaleNaam() == null && possessive(rest.onderwerpKeten());
        }
        if (!possessive && anchorsSubject(parts.get(parts.size() - 1))) parts.remove(parts.size() - 1);
        java.util.Collections.reverse(parts);
        return parts;
    }

    /** The parts of a written name at every 'van', without articles; a name that names a role or an object type as a
     *  whole stays one part. */
    private void nameParts(String name, List<String> result) {
        String whole = stripArticle(name).trim();
        if (model.navigationRoleName(whole) != null || model.objectTypeByName(whole) != null) {
            result.add(whole);
            return;
        }
        for (String part : whole.split("\\s+van\\s+")) result.add(stripArticle(part).trim());
    }

    /** Whether an onderwerp chain is the possessive "zijn &lt;naam&gt;", under a subselection or not. */
    private static boolean possessive(OnderwerpKetenContext k) {
        if (k == null) return false;
        return k.ZIJN() != null;
    }

    /** The refusal of a count over a written path, after "is": the steps, and {@code norm}, what counts and where. */
    private static Msg countPath(List<String> steps, Msg norm) {
        return Msg.frontend("PART.count-path", "count", steps.size(), "steps", String.join(", ", steps), "norm", norm);
    }

    /** The first "die/dat &lt;predicaat&gt;" subselectie anywhere in an operand subtree. */
    private PredicaatContext findSubselectionPredicate(ParseTree t) {
        if (t instanceof OnderwerpKetenContext k && k.predicaat() != null) return k.predicaat();
        for (int i = 0; i < t.getChildCount(); i++) {
            PredicaatContext r = findSubselectionPredicate(t.getChild(i));
            if (r != null) return r;
        }
        return null;
    }

    /** "die (niet) de Vergunning zelf zijn" (grammar/DIVERGENCES.md, "The rule subject in a subselection — zelf"): the
     *  identity check of the element against the subject of the form (IR-SPEC ch. 4.5, {@code IdentityCheck}), the
     *  name before "zelf" that subject's type's own name; null where the predicaat is no identity check. */
    private Condition identityFilter(PredicaatContext p) {
        IdentiteitsToetsContext t = p.toplevelOperator() != null ? p.toplevelOperator().identiteitsToets()
                : p.nonToplevelOperator() != null ? p.nonToplevelOperator().identiteitsToets() : null;
        if (t == null) return null;
        String name = stripArticle(src(t.kaleNaam()));
        if (model.objectTypes.get(name) != null && model.objectTypes.get(name) == formSubject())
            return new Condition.IdentityCheck(t.NIET() != null, source(p));
        refuseRuleSubject(source(p), selfElsewhere(name), source(p));
        return new Condition.Unresolved(source(p), null);   // the ERROR above is this marker's diagnosis
    }

    /** A "die een &lt;kenmerk&gt; zijn" subselectie resolved against the counted element type: the
     *  predicaat's name matched (case-insensitively) to a declared kenmerk of the element, as a
     *  {@link Condition.CharacteristicCheck}; null when the predicaat is not such a kenmerk of the element (a
     *  comparison, an objecttype/role membership, an unknown name — all left to the marker path). */
    private Condition elementCharacteristicCheckFilter(PredicaatContext p, ObjectType elem) {
        if (elem == null) return null;
        // §13.4.13.5-.8: a kenmerk membership predicate is "(een) <kenmerk> is/zijn" — the spec's
        // verb-trailing toplevelOperator register ("die een minderjarige zijn") — OR, in the permissive
        // verb-leading register, "is/zijn (een) <kenmerk>". Either way the bareName is the kenmerk and the
        // verb is is/zijn. A "heeft/hebben een <X>" predicaat is a possession/role query ("die een kind
        // hebben" / "die heeft een kind" = that HAVE a child), a different predicate class — not the
        // element's boolean kenmerk; its bareName carries no is/zijn here, so it falls through to the marker.
        String nameSource = null;
        boolean hasWholePeriod = false;
        if (p.toplevelOperator() != null) {                  // EEN? bareName (IS | HEEFT | ZIJN | HEBBEN)
            ToplevelOperatorContext op = p.toplevelOperator();
            if (op.kaleNaam() != null && (op.IS() != null || op.ZIJN() != null)) nameSource = src(op.kaleNaam());
            hasWholePeriod = op.gehelePeriodevergelijking() != null;
        } else if (p.nonToplevelOperator() != null) {        // (IS | HEEFT | ZIJN | HEBBEN) EEN? bareName, or EEN bareName (IS | ZIJN)
            NonToplevelOperatorContext op = p.nonToplevelOperator();
            if (op.kaleNaam() != null && (op.IS() != null || op.ZIJN() != null)) nameSource = src(op.kaleNaam());
            hasWholePeriod = op.gehelePeriodevergelijking() != null;
        }
        if (nameSource == null) return null;
        // §8.2: a "(niet) gedurende de/het gehele <periode>" whole-period qualifier on a subselectie element kenmerk is
        // NOT yet lowered onto the accumulate source — MARKER it (return null -> the caller markers the whole subselectie)
        // rather than SILENTLY binding a static kenmerk check that drops the §8.2 semantics. Both back-ends decline
        // symmetrically (no-silent-drop). The unqualified kenmerk subselectie ("die een minderjarige zijn") fires as before.
        if (hasWholePeriod) return null;
        // §5.6: the written form may be the kenmerk's inflected/nominalized spelling
        // ("die een minderjarige zijn" against the declared "minderjarig") — bind it morphologically.
        String characteristic = CharacteristicMatcher.find(elem, nameSource);
        return characteristic != null ? new Condition.CharacteristicCheck(characteristic, source(p)) : null;
    }

    /** §5.6/§13.4.13.5-.7 a getal/datum/tekst elementairpredicaat "die &lt;op&gt; &lt;expr&gt;" (the vragende-meervoud
     *  "die kleiner zijn dan 18", or the permissive verb-leading register) -> a {@link Condition.Comparison} over the
     *  element's AGGREGATED attribute. The subselectie names no attribute, so the LHS is the very attribute the
     *  aggregation folds (the single AttributeReference operand of the element, e.g. "de leeftijd van alle deelnemers"); the
     *  comparison is then constrained on the accumulate's source pattern. A count over the bare element (no folded
     *  attribute), a navigated/set-of operand, or a non-comparison predicaat returns null -> the caller markers. The
     *  RHS is resolved in the rule's subject context (a literal or a parameter-global — not an element attribute). */
    private Condition elementComparison(PredicaatContext p, ObjectType elem, List<Expr> arguments) {
        if (arguments.size() != 1 || !(arguments.get(0) instanceof Expr.AttributeReference agg)
                || agg.objectType() != elem || agg.navigationRole() != null) return null;
        Typing.CmpOp cmp; ExpressieContext rhsCtx;
        if (p.toplevelOperator() != null && p.toplevelOperator().vergelijkingsTekenGesplitst() != null
                && p.toplevelOperator().expressie() != null) {
            cmp = RuleChecker.cmpOf(p.toplevelOperator().vergelijkingsTekenGesplitst());
            rhsCtx = p.toplevelOperator().expressie();
        } else if (p.nonToplevelOperator() != null && p.nonToplevelOperator().vergelijkingsTeken() != null
                && p.nonToplevelOperator().expressie() != null) {
            cmp = RuleChecker.cmpOf(p.nonToplevelOperator().vergelijkingsTeken());
            rhsCtx = p.nonToplevelOperator().expressie();
        } else return null;
        Expr rhs = build(rhsCtx);
        // §5.6: the predicate ranges over the SELECTED ELEMENT, but the RHS is build()'d in the rule's SUBJECT context.
        // A subject/element attribute mis-binds ("zijn drempel" -> the subject's drempel, not the element's), so reject
        // an AttributeReference. Accept a LITERAL (both back-ends lower it as a fact-only source constraint) OR a §13.3.8
        // Parameter — the spec's worked-example RHS "die kleiner zijn dan de volwassenleeftijd". A Parameter is
        // context-independent (a session global in both back-ends), so it does not mis-bind. Anything else stays an
        // honest, symmetric marker.
        if (!(rhs instanceof Expr.Literal) && !(rhs instanceof Expr.ParameterRef)) return null;
        // WF-091 (reference-type identity, IR-SPEC ch. 4.1): the one VALUE-position reference whose type this builder
        // copies from another node instead of looking the declaration up. It invents nothing: it COPIES the fold
        // source, re-anchored on the element (subjectSource/bron reset, and set=false — the LHS reads ONE element,
        // not the set), carrying that node's (objectType, attribute, type) triple verbatim — so it carries no deviation the
        // source does not already have, and the verifier checks it at its own use site like any other reference.
        // Deliberately NOT the aggregation's own type: §4.20 strips the noemer off "het totaal van <€/mnd>" (€), while
        // the filter compares the unstripped per-element value (€/mnd). Re-looking the declaration up here would be
        // LESS safe: it could let this term and the accumulate's fold source drift apart, and both back-ends need them
        // to denote ONE read of ONE attribute.
        Expr lhs = new Expr.AttributeReference(agg.attribute(), elem, "", null, agg.type(), source(p));
        // §5.6 getal/datum/tekstpredicaat: enforce the typeringen comparison legality (as the normal voorwaarde path
        // does), so a type-mismatched filter (a Numeriek attribute vs a Tekst literal) is an ERROR, not silent bad DRL.
        Typing.Result tr = Typing.compare(cmp, lhs.type(), rhs.type(), false, unitlessNumberLiteral(rhs));
        if (!tr.ok()) {
            model.diagnostics.add(tr.failure().frame("subselection-predicate")
                    .at(RegelSpraakModel.Severity.ERROR, p.getStart().getLine()));
            return null;
        }
        return comparison(RuleChecker.toOperator(cmp), lhs, rhs, source(p));
    }

    /** §4.12-4.15: type-check the operand list of a value/date aggregation. min/max demand all
     *  numeriek with inter-convertible units (Tabel 22/23); eerste/laatste demand all datum with
     *  one tijd-precisie (Tabel 24/25). Other functies (sum/total parse a single arithmetic
     *  operand, the date-component extractors, etc.) carry no such operand-list contract here. */
    private void reportAggregation(Expr.FunctionKind kind, List<Expr> arguments, FunctieAanroepContext f) {
        List<Datatype> types = arguments.stream().map(Expr::type).toList();
        Typing.Coded error = switch (kind) {
            case MINIMUM -> verifierSentence("minimale waarde van", Typing.numberAggregation("minimale waarde van", types),
                    Typing.orderingFoldSentence(Spelling.AGGREGATION_KIND.spelling(kind), types));
            case MAXIMUM -> verifierSentence("maximale waarde van", Typing.numberAggregation("maximale waarde van", types),
                    Typing.orderingFoldSentence(Spelling.AGGREGATION_KIND.spelling(kind), types));
            // §4.10 Tabel 21 (som van): operands "moeten wel allemaal numeriek zijn" — the strict Numeriek
            // reading, DECIDED consistent with MIN/MAX (Tabel 22/23, identical wording): the spec adds an
            // explicit Percentage row + prose exactly where it admits one (§5.1/§5.3 comparison tables) and
            // explicitly excludes Percentage where it means to (§6.7 maal) — Tabel 21 does neither. The
            // "gespecialiseerd numeriek datatype" sentence (§3.3.5) does not override the tables (see
            // grammar/DIVERGENCES.md). A Percentage som-operand is a type error, never silently summed.
            case SOM -> verifierSentence("som van", Typing.numberAggregation("som van", types),
                    arithmeticFoldSentence(Spelling.AGGREGATION_KIND.spelling(kind), arguments));
            // §4.6 Tabel 16 (wortel van) / §4.9 Tabel 20 (absolute waarde van): the single operand must be Numeriek.
            // WORTEL always types Numeriek regardless of its radicand, so "de wortel van <tekst>" would produce a
            // Numeriek result with NO diagnostic; ABSOLUTE_WAARDE propagates the operand type, so "absolute waarde
            // van (<datum>)" would leak a datum unless a downstream assignment happened to reject it. Both reject a concrete
            // non-numeriek operand at the operator (Onbekend skipped, no claim), in the verifier's words where it
            // refuses the operand too (WF-038).
            case WORTEL -> verifierSentence("de wortel van", Typing.numberAggregation("de wortel van", types),
                    numericBuiltinSentence(kind, types));
            case ABSOLUTE_WAARDE -> verifierSentence("de absolute waarde van",
                    Typing.numberAggregation("de absolute waarde van", types), numericBuiltinSentence(kind, types));
            // §4.19 Tabel 29 (eerste paasdag van): "één numerieke expressie zonder eenheid ... altijd een geheel getal
            // met 4 cijfers". EERSTE_PAASDAG always types Datum, so an illegal operand produced a Datum result with no
            // diagnostic.
            case EERSTE_PAASDAG -> easterSundayOperandError(types);
            // §4.20 Tabel 30 (totaal van / aggregeren in de tijd): the operand "moet numeriek zijn met een
            // eenheid per tijdseenheid" — the NUMERIEK part via numberAggregation (like MIN/MAX), the rate part
            // via totalOperandError: a unit-BEARING operand must carry a tijdseenheid denominator; a UNITLESS
            // operand is admitted, because the spec's own §7.1 worked example totals a unitless Bedrag (the
            // temporality lives in the timeline, not the unit) — see grammar/DIVERGENCES.md.
            case TOTAAL -> verifierSentence("totaal van", totalOperandError(arguments),
                    arithmeticFoldSentence(Spelling.AGGREGATION_KIND.spelling(kind), arguments));
            case EERSTE  -> verifierSentence("eerste van",
                    Typing.dateAggregation("eerste van", Spelling.AGGREGATION_KIND.spelling(kind), types),
                    Typing.orderingFoldSentence(Spelling.AGGREGATION_KIND.spelling(kind), types));
            case LAATSTE -> verifierSentence("laatste van",
                    Typing.dateAggregation("laatste van", Spelling.AGGREGATION_KIND.spelling(kind), types),
                    Typing.orderingFoldSentence(Spelling.AGGREGATION_KIND.spelling(kind), types));
            // §6.12 / §13.4.17.21-.24: a time-of-day component is defined only on a datum-TIJD — a plain
            // "Datum in dagen" has no hour/minute/second. The DATE parts (jaar/maand/dag) accept any date, so
            // they are NOT checked. Onbekend is skipped (no claim); only a concrete DateInDays is a violation.
            case UUR_UIT, MINUUT_UIT, SECONDE_UIT, MILLISECONDE_UIT -> timeComponentError(types);
            // §4.16/§4.17 typeringen: the two operands of a (absolute) tijdsduur must BOTH be datum AND have the
            // SAME tijd-precisie.
            case TIJDSDUUR, ABSOLUTE_TIJDSDUUR -> durationPrecisionError(kind, types);
            // WF-038's scope sentence, spelled out rather than left to the default arm: the filled
            // count is NOT a numeric fold. Its test is presence, so every operand type counts
            // (decision G8, RUNTIME-SEMANTICS ch. 3.4c) — a numberAggregation check here would
            // silently undo that by refusing a tekst/datum/enumeratie/booleaan source.
            case AANTAL_GEVULDE -> null;
            // WF-037's count-arity arm (IR-SPEC ch. 4.3, maintainer decision 2026-09-20). The one arm
            // in this switch about SHAPE rather than operand TYPES, and it sits here because this is
            // the per-kind aggregation diagnosis and the arity is known at the node. It sits beside
            // the filled count's arm above and never reaches it: that kind carries its own arity law
            // (exactly ONE argument, WF-037's other new arm), so the two counts are fenced apart.
            case AANTAL -> countArityError(arguments, f);
            default -> null;
        };
        if (error != null)
            model.diagnostics.add(error.at(RegelSpraakModel.Severity.ERROR, f.getStart().getLine()));
    }

    /** WF-037's count-arity arm (IR-SPEC ch. 4.3, maintainer decision 2026-09-20): a count folds
     *  INSTANCES and has no LISTED form, so two or more operands are ill-formed on the wire.
     *
     *  <p><b>This fence closes a LEAK of this frontend's own making.</b> The source language spells no
     *  operand list for a count — §5.8.1 ranges it over instances and the typing document (§4.11)
     *  excludes the value reading — but {@code functieAanroep}'s {@code aantalFunc} alternative reads
     *  {@code primaireExpressie ( COMMA primaireExpressie )* ( EN primaireExpressie )?}, a registered
     *  divergence ({@code grammar/DIVERGENCES.md}), and {@code args} builds every child as its own
     *  argument. Measured 2026-09-20: three sentences reached the wire unblocked — the {@code en} and
     *  {@code comma} spellings with {@code diagnostics: []} and the element-binding spelling with a
     *  non-blocking WARNING — all rejected by the verifier
     *  ({@code dev.uchar.regelspraak.CountEnumeratedTest}).
     *
     *  <p>ERROR and not WARNING, deliberately: {@link Resolver#irMode} writes nothing when the model
     *  carries an error, so the fail-fast is what keeps the ill-formed document off disk. That is the
     *  level {@code CountSetSourceTest} pins for the set fence one decision earlier, and unlike there
     *  no committed expectation asserts this shape resolves clean — measured, no rgs golden and no
     *  other test carries a listed count.
     *
     *  <p>It reads the KIND and the operand COUNT and never {@code elementObjectType}: a count that
     *  binds an element AND carries an operand list is the same listed form with a second claim on one
     *  node, and the wire rule draws its boundary in exactly that place. Below two operands it is
     *  silent, which is the wire rule's boundary too: one operand without an element type is
     *  {@link #countSourceError}'s. */
    private Typing.Coded countArityError(List<Expr> arguments, FunctieAanroepContext f) {
        if (arguments.size() < 2) return null;
        // where the §5.7 union route opened and {@link #buildInstanceUnion} refused it, its reason is the context
        // the author needs, so it stands in the Dutch prefix; the listed count itself is the verifier's sentence
        boolean unionRefused = f instanceof AantalFuncContext && unionRefusal != null;
        return new Typing.Coded("RGS-077",
                params("expression", source(f).snippet(), "unionRefusal", unionRefused ? unionRefusal : "",
                        "operandCount", arguments.size(),
                        "noUnion", Msg.frontendIf(unionRefused, "RGS-077.no-union", "unionRefusal", unionRefusal)));
    }

    /** WF-037's count-source arm (IR-SPEC ch. 4.3, maintainer decision 2026-09-23): a count MUST
     *  carry an element type, unless its one argument is an instance-set union.
     *
     *  <p><b>A live route, measured 2026-09-23 before this arm.</b> {@code aantalFunc} takes any
     *  {@code primaireExpressie}, and when the operand names no role or object type
     *  {@link #aggregationElement} yields null and, unless the whole phrase is a declared parameter,
     *  the node was emitted anyway: "het aantal gewogen vangst van de Jutter" (an own attribute),
     *  "het aantal 3", "het aantal (3 plus 4)", "het aantal drempel" (a parameter), "het aantal
     *  Vondsten" — each an AANTAL with {@code elementObjectType: null}, all with
     *  {@code diagnostics: []}, in the v0 wire spelling of that date; on the L5 wire the shape is a
     *  {@code count} without {@code elementType}. The verifier rejects that shape, where {@code ref}
     *  answered it with the operand counted, not a population ({@link dev.uchar.regelspraak
     *  .CountWithoutSourceTest}).
     *
     *  <p>ERROR for the same reason as the arity arm: {@link Resolver#irMode} then writes nothing.
     *  The grammar is untouched; the source language's count takes a subject expression, and the
     *  refusal belongs where the element is derived, not in the parse. */
    private Typing.Coded countSourceError(List<Expr> arguments, FunctieAanroepContext f) {
        // one wire defect, the count without an element type, in the verifier's sentence; the Dutch context names
        // what the operand turned out to be where that tells the author more
        Msg context = countAttributeCollision(arguments, f);
        if (context == null) context = countOverObjectType(arguments, f);
        return new Typing.Coded("RGS-078",
                params("expression", arguments.isEmpty() ? "" : source(f).snippet(), "context", context == null ? "" : context,
                        "quoted", Msg.frontendIf(!arguments.isEmpty(), "RGS-078.quoted", "expression", source(f).snippet()),
                        "detail", Msg.frontendIf(context != null, "RGS-078.detail", "context", context)));
    }

    /** "het aantal Vondsten": a count whose one operand names an OBJECT TYPE — by name or plural, bare, with an
     *  article or with "alle" — and nothing it is counted from. The card frontend-typemeervoud-telling
     *  (2026-09-23) answered the norm question NO: RegelSpraak v2.3.0 has no count over all instances of a type.
     *  The count takes a plural subject expression (§5.8.1, typeringen §4.11), and §5.8 names the plural ones —
     *  a concatenation, a selection whose object or role is plural, a subselection — each hanging on a subject
     *  the rule binds per instance (§5.5.3); an object type stands in an onderwerpketen only in the singular,
     *  as a universal subject or a reference back to one (§5.5.1, §13.4.1). The one document-wide range, "van alle
     *  &lt;objecttype&gt;", is the uniqueness check's own subject expression (§8.1.6) and no aggregation operand.
     *
     *  <p>The refusal is {@link #countSourceError}'s, in the verifier's sentence; this names the object type in its
     *  Dutch context, so an author who named a population learns which one the count read as a type. Only an
     *  operand that resolved to NOTHING declared qualifies — an unresolved one, a bare name among them — so a
     *  parameter, a "Daarbij geldt" variable or an attribute that shares a type's name is never re-read. Null when the
     *  operand names no type. */
    private Msg countOverObjectType(List<Expr> arguments, FunctieAanroepContext f) {
        if (!(f instanceof AantalFuncContext cf) || arguments.size() != 1) return null;
        if (!(arguments.get(0) instanceof Expr.Unresolved)) return null;
        String operand = src(cf.primaireExpressie(0)).replaceAll("^\\((.*)\\)$", "$1").trim();
        String bare = operand.replaceFirst("(?i)^(de|het|een|alle)\\s+", "");
        ObjectType type = PluralMatcher.findObjectType(model.objectTypes.values(), bare);
        return type == null ? null : Msg.frontend("PART.count-over-object-type", "type", type.name);
    }

    /** The source-less count whose words also spell a DECLARED attribute: "het aantal gewogen posten
     *  van de Jutter" with "aantal gewogen posten" declared on Jutter. §13.4.17.3 derives the sentence
     *  as a getalaggregatie and as an attribuutvanonderwerp alike, with no tie-break; {@code
     *  functieAanroep} precedes {@code attribuutVanOnderwerp} in {@code primaireExpressie}, so the
     *  parse is the count. It is NOT reinterpreted as the attribute here, unlike the parameter
     *  fallback in {@link #buildFunction}: the article-less spelling (§13.3.2.6 makes the article
     *  optional) already reads the attribute, and no source measured reads such an attribute under
     *  "het aantal" — a second reading would widen the language rather than resolve it. The refusal is
     *  {@link #countSourceError}'s, in the verifier's sentence; this names the collision in its Dutch context,
     *  so the author sees why a declared name is refused. Null when there is no such attribute. */
    private Msg countAttributeCollision(List<Expr> arguments, FunctieAanroepContext f) {
        if (!(f instanceof AantalFuncContext cf) || arguments.size() != 1
                || !(arguments.get(0) instanceof Expr.AttributeReference a) || a.objectType() == null) return null;
        String name = "aantal " + a.attribute();
        if (a.objectType().attribute(name) == null) return null;
        return Msg.frontend("PART.count-collision", "name", name, "type", a.objectType().name);
    }

    /** §4.19 Tabel 29 (eerste paasdag van): the single operand is "een numerieke expressie zonder eenheid ... altijd een
     *  geheel getal met 4 cijfers". Enforce the STATIC part — Numeriek, geheel, geen eenheid (the 4-cijfer VALUE range is
     *  a runtime concern, unknowable for a variable). EERSTE_PAASDAG always types Datum, so an illegal operand ("eerste
     *  paasdag van <tekst>", "… van <EUR getal>", "… van <decimaal>") produced a Datum result with NO diagnostic.
     *  Onbekend is skipped (no claim). "het jaar uit (…)" — the spec's canonical operand — types Numeriek geheel zonder
     *  eenheid, so it passes. */
    private static Typing.Coded easterSundayOperandError(List<Datatype> types) {
        if (types.size() != 1 || types.get(0).isUnknown()) return null;
        Datatype d = types.get(0);
        if (!(d instanceof Datatype.Numeric n))
            return easterSundaySentence(d);
        // Reject only a DECLARED decimal precision (getal met N decimalen) or a unit — NOT a !geheel() flag. A whole-year
        // ARITHMETIC operand ("het jaar uit X plus 1" = Easter of the next year, or "2024 plus 1") is spec-legal (§6.13:
        // "Het jaartal wordt gespecificeerd door een geheel getal"), but Typing.numericBinary types every plus/min as
        // geheel=false (it erases geheel-ness of integer arithmetic), so a !geheel() gate would FALSELY reject it (review
        // a29b3e97). decimalen()>0 keeps rejecting the concrete "getal met 2 decimalen" case while admitting a dec=0
        // arithmetic year (an unbounded "getal" — statically indistinguishable from arithmetic — is likewise admitted; the
        // 4-cijfer value range stays a runtime concern).
        if (n.decimals() > 0 || n.hasUnit())
            return easterSundaySentence(d);
        return null;
    }

    /** The verifier refuses every operand this check refuses (WF-038: a whole unitless number or nothing). */
    private static Typing.Coded easterSundaySentence(Datatype d) {
        return new Typing.Coded("RGS-079", params("kind", Typing.kindWord(d)));
    }

    /** The verifier's sentence in place of RegelSpraak's refusal {@code error} where the verifier refuses the same
     *  operands ({@code sentence} non-null), after the Dutch context {@code contextPrefix}; RegelSpraak's own words where it
     *  admits them. */
    private static Typing.Coded verifierSentence(String contextPrefix, Typing.Coded error, Typing.Coded sentence) {
        return error == null ? null : sentence != null ? sentence.frame("context", "contextPrefix", contextPrefix) : error;
    }

    /** WF-038's sentence for the one operand of a square root or an absolute value where the verifier refuses it; a
     *  percentage is a number to the verifier, so that refusal keeps RegelSpraak's own words. */
    private static Typing.Coded numericBuiltinSentence(Expr.FunctionKind kind, List<Datatype> types) {
        if (types.size() != 1 || types.get(0).isUnknown() || types.get(0).isNumberLike()) return null;
        return new Typing.Coded("RGS-080",
                params("builtin", Spelling.BUILTIN_KIND.spelling(kind), "kind", Typing.kindWord(types.get(0))));
    }

    /** WF-091's sentence for an arithmetic fold ({@code fold}: sum or total on the wire) over an operand that is no
     *  number, one per such operand and joined by "; ", naming the operand as the verifier does. Null where every
     *  operand is a number to the verifier, a percentage included. */
    private static Typing.Coded arithmeticFoldSentence(String fold, List<Expr> arguments) {
        List<Msg> sentences = new ArrayList<>();
        List<String> operands = new ArrayList<>(), typeNames = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            Expr a = arguments.get(i);
            Datatype d = a.type();
            if (d.isUnknown() || d.isNumberLike()) continue;
            Msg operand = a instanceof Expr.AttributeReference r && r.attribute() != null && r.objectType() != null
                    ? Msg.frontend("RGS-081.operand-attribute", "attribute", r.attribute(), "objectType", r.objectType().name)
                    : a instanceof Expr.ParameterRef p && p.parameter() != null
                    ? Msg.frontend("RGS-081.operand-parameter", "parameter", p.parameter().name())
                    : Msg.frontend("RGS-081.operand-position", "position", i);
            String t = Typing.typeName(d);
            operands.add(operand.text());
            typeNames.add(t);
            sentences.add(Msg.frontend("RGS-081.sentence", "fold", fold, "operand", operand, "typeName", t,
                    "rationale", d.isDate() ? Msg.frontend("RGS-081.rationale-date")
                            : Msg.frontend("RGS-081.rationale-other", "typeName", t)));
        }
        // one sentence per operand that is no number, in operand order; the template renders them joined
        return sentences.isEmpty() ? null : new Typing.Coded("RGS-081",
                params("fold", fold, "operands", String.join("; ", operands), "typeNames", String.join(", ", typeNames),
                        "sentences", sentences));
    }

    /** §4.20 Tabel 30 (totaal van): the operand is "numeriek met een eenheid per tijdseenheid". The NUMERIEK
     *  part rides {@link Typing#numberAggregation}; the RATE part is enforced only when the operand CARRIES a
     *  unit AND is not a §5.1.4 TEMPORAL attribute — a plain unit (EUR) or a non-time denominator (EUR/km)
     *  on a NON-temporal operand is the Tabel-30 fout, while a temporal attribute's per-tijdseenheid
     *  dimension lives in its TIMELINE declaration ("voor elke maand"), not its unit: the spec's own §7.1
     *  worked example totals a timeline-declared Bedrag, so a plain-unit temporal operand must not be
     *  rejected (see grammar/DIVERGENCES.md). A unitless operand is likewise admitted; Onbekend makes no claim. */
    private static Typing.Coded totalOperandError(List<Expr> arguments) {
        Typing.Coded f = Typing.numberAggregation("totaal van", arguments.stream().map(Expr::type).toList());
        if (f != null) return f;
        for (Expr arg : arguments) {
            Unit e = arg.type().unitOf();
            if (e == null || TimeCalculation.chronoUnit(e.denominator()) != null) continue;
            boolean temporal = arg instanceof Expr.AttributeReference a && a.objectType() != null
                    && a.objectType().granularityOf(a.attribute()) != null;
            if (!temporal)
                return new Typing.Coded("RGS-082", params("unit", e.rendering()));
        }
        return null;
    }

    /** §6.12 / §13.4.17.21-.24: the operand of a time-of-day component (uur/minuut/seconde/milliseconde uit)
     *  must be a datum-tijd; a plain "Datum in dagen" carries no time. Returns the fout message for a concrete
     *  DateInDays operand, else null (Onbekend and any non-date type are not this check's concern). */
    private static Typing.Coded timeComponentError(List<Datatype> types) {
        if (types.size() != 1 || !(types.get(0) instanceof Datatype.DateInDays)) return null;
        return new Typing.Coded("RGS-083", params("kind", Typing.kindWord(types.get(0))));   // the verifier's WF-038
    }

    /** §4.16/§4.17 (typeringen, normative): the two operands (van, tot) of a (absolute) tijdsduur must BOTH be a
     *  datumexpressie AND have the SAME tijd-precisie — both {@link Datatype.DateInDays} or both {@link Datatype.DateTime}.
     *  A non-datum operand (a Numeriek passed where a datum belongs) or a concrete mixed pair (Datum-in-dagen tot
     *  Datum-en-tijd) is spec-illegal but otherwise silently emits code neither back-end can run (rsTijdsduur wants a
     *  Temporal). Onbekend is skipped (no claim). The verifier refuses both in WF-038, so the diagnostic is its sentence
     *  after the Dutch context; null where the operands are legal. */
    private static Typing.Coded durationPrecisionError(Expr.FunctionKind kind, List<Datatype> types) {
        String category = Spelling.BUILTIN_KIND.spelling(kind);   // the verifier names the builtin's own kind
        Datatype ref = null;
        for (Datatype d : types) {
            if (d.isUnknown()) continue;                       // no claim
            if (!d.isDate())                                   // §4.16/§4.17: both operands must be a datumexpressie
                // the sentences are joined, so one template carries the kinds of the operands that are no date
                return new Typing.Coded("RGS-084", params("category", category, "kinds",
                        String.join(", ", types.stream().filter(x -> !x.isUnknown() && !x.isDate())
                                .map(Typing::kindWord).toList()),
                        "sentences", types.stream().filter(x -> !x.isUnknown() && !x.isDate())
                                .map(x -> Msg.frontend("RGS-084.operand-sentence", "category", category, "kind", Typing.kindWord(x))).toList()));
            if (ref == null) ref = d;
            else if (!ref.getClass().equals(d.getClass()))      // both datum but a different tijd-precisie: WF-038
                return new Typing.Coded("RGS-085", params("category", category));
        }
        return null;
    }

    /** WF-038 on the counted unit of a duration, the unit of its node type: a recognised time unit or a declared day
     *  kind (the calendar table first, as {@link TimeCalculation#dayKindForUnit} reads it), and a sub-day unit only
     *  between millisecond-precision dates. The verifier's sentence where it refuses the unit, else null. */
    private Typing.Coded durationUnitError(Expr.FunctionKind kind, Unit e, List<Expr> arguments) {
        String category = Spelling.BUILTIN_KIND.spelling(kind);   // the verifier names the builtin's own kind
        if (e == null)
            return new Typing.Coded("RGS-086", params("category", category));
        String unit = TimeCalculation.chronoUnit(e);
        if (unit == null)
            return TimeCalculation.dayKindForUnit(model, e) != null ? null
                    : new Typing.Coded("RGS-087", params("category", category, "unit", e.abbreviation()));
        return Typing.subDayUnit(unit) && arguments.stream().anyMatch(a -> a.type() instanceof Datatype.DateInDays)
                ? new Typing.Coded("RGS-088", params("category", category)) : null;
    }

    /** The §5.8.2 "of … als die er niet zijn" fallback expression of a sommatie, or null.
     *  Only "som van" carries it: an empty "som van" is a leegwaarde unless this overrides
     *  it (§4.10), whereas an empty "totaal van" is unconditionally 0 (§4.20, no fallback). */
    private Expr emptyFallbackOf(FunctieAanroepContext f) {
        if (f instanceof SomFuncContext s && s.leegwaardeAlternatief() != null)
            return build(s.leegwaardeAlternatief().expressie());
        return null;
    }

    /** §13.4.17.32 "(absolute) tijdsduur van A tot B in [hele] <eenheid>": a number of
     *  that unit (whole when "hele" is present). Unit normalisation makes "jaren"
     *  convertible with a declared "jr". */
    private Datatype durationType(TijdsduurFuncContext t) {
        dev.uchar.regelspraak.model.Unit e = t.eenheidMeervoud() != null
                ? model.resolveUnit(src(t.eenheidMeervoud())) : null;
        return new Datatype.Numeric(t.HELE() != null, 0, Datatype.Sign.ONBEPAALD, e);
    }

    /**
     * The object type an aggregation ranges over. For value-aggregations
     * (sum/min/max/first/last) it is the object the aggregated attribute belongs to
     * — the argument's bound AttributeReference objectType. For a count "het aantal
     * <rol> van <onderwerp>" the parser makes <rol> the attribute name, so the
     * counted element type is that role resolved to its object type.
     */
    private ObjectType aggregationElement(Expr.FunctionKind kind, List<Expr> args) {
        if (args.isEmpty() || !(args.get(0) instanceof Expr.AttributeReference a)) return null;
        return switch (kind) {
            // AANTAL_GEVULDE rides the VALUE-fold derivation, not the count's: its argument is an
            // ATTRIBUTE read over a set, so the element type is the attribute's owner — the very
            // asymmetry RuleChecker's FOLD_KINDS note names (a count's element word is a ROLE)
            case SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE, AANTAL_GEVULDE -> a.objectType();
            case AANTAL -> countElement(a.attribute());
            default -> null;
        };
    }

    /**
     * The object type a count "het aantal &lt;X&gt; van &lt;onderwerp&gt;" ranges over (§4.11
     * counts onderwerpexpressies). The naive reading — {@link RegelSpraakModel#resolveObjectType}
     * — is subject-blind and tries the name as an objecttype FIRST, so a count element word that
     * collides with a declared objecttype name ("het aantal bestemming", where Bestemming is a
     * type AND "bestemming" a role to Luchthaven) binds the wrong type and then loses its join to
     * a comment placeholder — counting ALL instances globally. Mirror the p12 navigation
     * disambiguation EXACTLY: take the role reading when a same-named role uniquely reaches a type
     * FROM the subject and no role NAMED-as-written reaches the by-name objecttype from the subject
     * — the written reference IS that role navigation (§13.4.1.2/.3). The name-aware
     * {@link RegelSpraakModel#connectingRoleViaName} (not the name-blind by-pair {@code
     * connectingRole}) is what makes a count and the analogous plain navigation resolve the same
     * written filler identically, even when the by-name objecttype joins the subject through a
     * DIFFERENT, unwritten role. Otherwise the by-name reading stands.
     */
    private ObjectType countElement(String word) {
        ObjectType byName = model.resolveObjectType(word);
        // the count's element word is a population name too ("het aantal Munten van de boot"), and
        // it is written in the plural by construction — same fallback, same precedence as the set
        // onderwerp above (see typeViaPlural); the role reading below still wins where it applies.
        if (byName == null) byName = typeViaPlural(word);
        if (currentSubject == null) return byName;
        ObjectType viaRole = model.roleTargetTypeToSubject(currentSubject, word);
        if (viaRole != null && (byName == null || model.connectingRoleViaName(currentSubject, byName, word) == null))
            return viaRole;
        return byName;
    }

    /**
     * Result datatype of an aggregation/function. The aggregations that preserve
     * their operand's type — sum/total/min/max/first/last/abs — yield the element
     * type (which carries a real unit once the argument's subject resolves
     * through a role); a count is a non-negative integer; concatenation is Tekst;
     * the date constructors and Easter are dates. The unit/precision-sensitive
     * functions (tijdsduur's "in hele <eenheid>", wortel's unit drop, date-component
     * extraction, day counts, tijdsevenredig deel) are left Onbekend until unit
     * normalisation exists — typing them naively would mis-match "jaren" against the
     * declared "jr" and raise a false positive. Structure is captured regardless.
     */
    /** The least declared decimal count over a min/max operand list — the floor of the
     *  value-dependent result precision, so the §6.1.2 check never over-reports. */
    private static int minDecimals(List<Expr> args) {
        return args.stream().mapToInt(a -> a.type() instanceof Datatype.Numeric m ? m.decimals()
                : a.type() instanceof Datatype.Percentage p ? p.decimals() : 0).min().orElse(0);
    }

    /** Whether EVERY min/max operand is a whole number — the only case in which the winning value
     *  is provably integral. Derived from each operand's geheel flag, NOT from decimalen==0 (which an
     *  unbounded "getal" operand also carries), so a min/max of unbounded or decimal operands is not
     *  mislabelled geheel — which would wrongly admit it to the §5.13 elfproef (Tabel 55). */
    private static boolean allWhole(List<Expr> args) {
        return args.stream().allMatch(a -> (a.type() instanceof Datatype.Numeric m && m.wholeNumber())
                || (a.type() instanceof Datatype.Percentage p && p.wholeNumber()));
    }

    /** Whether an aggregation's operand is a NON-TEMPORAL SET read — "&lt;attribuut&gt; van alle &lt;elementen&gt;"
     *  over a plain (non-timeline) attribute: the pure instance fold, which adds element values and integrates
     *  over nothing. Both halves are load-bearing. The {@code set()} flag is the same one
     *  {@link #duringCharacteristicCheckFilter} tests negatively to admit the temporal clip; the granularity half is the
     *  same test {@link #totalOperandError} applies (a §5.1.4 timeline-declared attribute, "voor elke maand"),
     *  because a set read over a TEMPORAL attribute is the §7.1 PER-ELEMENT TIMELINE FOLD — each element's
     *  tijdreeks is summed over time FIRST (rsTijdTotaal at the attribute's granularity) and only then over the
     *  set — so that form DOES integrate over time and the §4.20 strip must keep applying to it. */
    private static boolean setRead(List<Expr> args) {
        return args.size() == 1 && args.get(0) instanceof Expr.AttributeReference a && a.set()
                && !(a.objectType() != null && a.objectType().granularityOf(a.attribute()) != null);
    }

    private Datatype functionType(Expr.FunctionKind kind, List<Expr> args) {
        Datatype first = args.isEmpty() ? unknown() : args.get(0).type();
        return switch (kind) {
            case SOM, EERSTE, LAATSTE, ABSOLUTE_WAARDE -> first;
            // §4.20 Tabel 30: totaal-van INTEGRATES a rate over time, so the result drops the /tijdseenheid —
            // row 1 result eenheid "-", row 2 "Eenheid1" (the numerator). "het totaal van <EUR/mnd>" types EUR
            // and assigns cleanly to a EUR target (a EUR/mnd target is the unit mismatch it should be).
            // A non-rate operand type passes through unchanged (the §7.1 unitless-Bedrag path).
            // The strip is suppressed on the NON-TEMPORAL SET READING alone (maintainer decision 2026-09-04,
            // read narrowly: a non-set non-temporal rate read keeps the strip and both executors refuse it
            // loudly — the §4.20 operand contract): the SET reading
            // "het totaal van <x> van alle <elementen>" over a PLAIN (non-timeline) attribute — which the grammar
            // admits — is an ALIAS of "de som van", an instance fold that integrates over nothing, so it carries
            // the ELEMENT's type verbatim exactly as SOM does. Stripping there invented a wire type no executor
            // can produce: the fold adds EUR/mnd values and the EUR target then faults on assignment
            // ("incompatible units: EUR vs EUR"), while an honest EUR/mnd target was rejected by the §6.1.2
            // assignment check. A set read over a TEMPORAL attribute is NOT that alias — it is the §7.1
            // per-element timeline fold, a time integration, and setRead excludes it so the strip still fires.
            // One predicate, no other behaviour moves.
            case TOTAAL -> setRead(args) ? first
                    : first instanceof Datatype.Numeric n && n.unit() != null && n.unit().denominator() != null
                    ? new Datatype.Numeric(n.wholeNumber(), n.decimals(), n.sign(), n.unit().numerator())
                    : first;
            // §4.12/§4.13: a multi-operand min/max result is the SMALLEST/LARGEST VALUE, whose
            // precision is value-dependent and not statically known. A single-operand min/max over a
            // set carries the element's precision (reliable). For the value-fold, report the LEAST
            // operand precision so the §6.1.2 check never over-reports (the exact value-dependent
            // precision is carded); the first operand's precision would be order-dependent.
            case MINIMUM, MAXIMUM -> args.size() <= 1 || !(first instanceof Datatype.Numeric n) ? first
                    : new Datatype.Numeric(allWhole(args), minDecimals(args), n.sign(), n.unit());
            // both counts: a cardinality is a whole, UNITLESS number whatever the folded attribute
            // measures (RUNTIME-SEMANTICS ch. 3.4c) — the source's kg never reaches the result
            case AANTAL, AANTAL_GEVULDE -> new Datatype.Numeric(true, 0, Datatype.Sign.NIET_NEGATIEF, null);
            // §5.7/§13.4.17.2: a concatenatie is a MULTI-VALUED set (§5.8.1 "altijd meervoudig"), NOT a scalar
            // Tekst — it carries the common type of its operands and is only meaningful once an aggregation
            // (het aantal / de som / de min-max / de eerste-laatste) consumes it (§5.8). This "de concatenatie
            // van …" primaireExpressie surface is a repo artifact (the spec keyword belongs to §13.4.8.14
            // uniciteitconcatenatie only; the real §13.4.17.2 value concatenatie has no keyword and lives inline
            // in each aggregation's operand list). As a STANDALONE value it has no scalar meaning, so both back-
            // ends marker it symmetrically; typing it Tekst fabricated a spurious "Tekst→Numeriek" mismatch that
            // MASKED that honest marker. Leave it onbekend (the not-lowered-kind convention) — see
            // grammar/DIVERGENCES.md "concatenatie as a standalone value".
            case CONCATENATIE -> unknown();
            // §13.4.17.13: a square root is non-negative; its precision comes from the MANDATORY afronding that
            // wraps it (a sqrt is generally irrational), and its unit would be the operand's unit's root —
            // composite-unit territory, dropped for now (carded). The afronding's Rounding re-derives decimals.
            case WORTEL -> new Datatype.Numeric(false, 0, Datatype.Sign.NIET_NEGATIEF, null);
            case EERSTE_PAASDAG, DATUM_MET -> new Datatype.DateInDays();
            case DATUMTIJD_MET -> new Datatype.DateTime();
            // §13.4.17.18-.24 / typeringen Tabel 28: a date/time component (jaar/maand/dag/uur/minuut/seconde/
            // milliseconde uit) is a unitless non-negative whole Numeriek. Typing it (rather than leaving it
            // Onbekend, which makes Typing.compare short-circuit OK) lets the comparison matrix REJECT an
            // illegal use — e.g. "de maand uit X eerder dan <een datum>", which Tabel 45 restricts to two dates
            // — instead of the transpiler lowering it to an uncompilable BigDecimal-vs-LocalDate term.
            case JAAR_UIT, MAAND_UIT, DAG_UIT, UUR_UIT, MINUUT_UIT, SECONDE_UIT, MILLISECONDE_UIT ->
                    new Datatype.Numeric(true, 0, Datatype.Sign.NIET_NEGATIEF, null);
            // §7.3.2/§13.4.17.58: the pro-rata part carries the BASE's value type — the per-bucket fraction
            // scales the base within one bucket and never changes its unit or family (a rate base is restated
            // by the transpiler BEFORE proration, §4.20-style). Typing it — instead of the not-lowered
            // onbekend — puts the concrete datatype on the emitted IR node (the uchar spec forbids the
            // unknown-type sentinel inside a structured node, its WF-034).
            case TIJDSEVENREDIG_DEEL -> first;
            default -> unknown();
        };
    }

    private Expr.FunctionKind functionKind(FunctieAanroepContext f) {
        if (f instanceof SomFuncContext)        return Expr.FunctionKind.SOM;
        if (f instanceof AantalFuncContext)     return Expr.FunctionKind.AANTAL;
        if (f instanceof AantalGevuldeFuncContext) return Expr.FunctionKind.AANTAL_GEVULDE;
        if (f instanceof TotaalFuncContext)     return Expr.FunctionKind.TOTAAL;
        if (f instanceof WortelFuncContext)     return Expr.FunctionKind.WORTEL;
        if (f instanceof AbsWaardeFuncContext)  return Expr.FunctionKind.ABSOLUTE_WAARDE;
        if (f instanceof PaasdagFuncContext)    return Expr.FunctionKind.EERSTE_PAASDAG;
        if (f instanceof ConcatFuncContext)     return Expr.FunctionKind.CONCATENATIE;
        if (f instanceof AantalDagenFuncContext)   return Expr.FunctionKind.AANTAL_DAGEN_IN;
        if (f instanceof TijdsevenredigFuncContext) return Expr.FunctionKind.TIJDSEVENREDIG_DEEL;
        if (f instanceof JaarUitFuncContext)    return Expr.FunctionKind.JAAR_UIT;
        if (f instanceof MaandUitFuncContext)   return Expr.FunctionKind.MAAND_UIT;
        if (f instanceof DagUitFuncContext)     return Expr.FunctionKind.DAG_UIT;
        if (f instanceof UurUitFuncContext)     return Expr.FunctionKind.UUR_UIT;
        if (f instanceof MinuutUitFuncContext)  return Expr.FunctionKind.MINUUT_UIT;
        if (f instanceof SecondeUitFuncContext) return Expr.FunctionKind.SECONDE_UIT;
        if (f instanceof MilliUitFuncContext)   return Expr.FunctionKind.MILLISECONDE_UIT;
        if (f instanceof MinMaxFuncContext m)   return m.MAXIMALE_WAARDE_VAN() != null ? Expr.FunctionKind.MAXIMUM : Expr.FunctionKind.MINIMUM;
        if (f instanceof EersteLaatsteFuncContext el) return el.EERSTE_VAN() != null ? Expr.FunctionKind.EERSTE : Expr.FunctionKind.LAATSTE;
        if (f instanceof TijdsduurFuncContext t) return t.ABSOLUTE_TIJDSDUUR_VAN() != null ? Expr.FunctionKind.ABSOLUTE_TIJDSDUUR : Expr.FunctionKind.TIJDSDUUR;
        return Expr.FunctionKind.SOM; // unreachable: every functieAanroep alternative is covered above
    }

    /** The direct expression children of a context, built in order — an aggregation's
     *  arguments. The telling (§13.4.17.47 aantal) takes a higher-precedence
     *  primaireExpressie operand (so a trailing 'maal'/'plus' binds outside the count),
     *  so a primaireExpressie child is built as such; every other aggregation takes full
     *  expressie children. */
    private List<Expr> args(ParserRuleContext ctx) {
        List<Expr> out = new ArrayList<>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree ch = ctx.getChild(i);
            if (ch instanceof ExpressieContext ec) out.add(build(ec));
            else if (ch instanceof PrimaireExpressieContext pe) out.add(buildPrimary(pe));
        }
        return out;
    }

    // ---------------- §13.2.12 rangtelwoord (§5.5.3) ----------------
    /** The 1-based mention position a §13.2.12 rangtelwoord denotes, or -1 for "laatstgenoemde". */
    private static int ordinalPosition(RangtelwoordContext r) {
        if (r.EERSTGENOEMDE() != null) return 1;
        if (r.ALS_TWEEDE_GENOEMDE() != null) return 2;
        if (r.ALS_DERDE_GENOEMDE() != null) return 3;
        if (r.ALS_VIERDE_GENOEMDE() != null) return 4;
        if (r.LAATSTGENOEMDE() != null) return -1;
        return r.NUMBER() != null ? Integer.parseInt(r.NUMBER().getText()) : 1;   // "als <N>e genoemde"
    }

    /** The subject reference (onderwerpVerwijzing) of a reference's keten that carries a rangtelwoord, or null. */
    private OnderwerpVerwijzingContext referenceWithOrdinal(OnderwerpContext o) {
        return o == null ? null : referenceWithOrdinal(o.onderwerpKeten());
    }
    private OnderwerpVerwijzingContext referenceWithOrdinal(OnderwerpKetenContext k) {
        if (k == null) return null;
        if (k.onderwerpVerwijzing() != null)
            return k.onderwerpVerwijzing().rangtelwoord() != null ? k.onderwerpVerwijzing() : null;
        return null;
    }

    /** §5.5.3: how many universele onderwerpen ("een &lt;naam&gt;") the enclosing rule binds under {@code name} —
     *  the mentions a rangtelwoord indexes ("de plaats van het bedoelde onderwerp in de regel"). A definite
     *  back-reference is a REFERENCE to a binding, not a new one, so only the indefinite form counts. */
    private int countBindings(ParserRuleContext ref, String name) {
        ParserRuleContext p = ref;
        while (p != null && !(p instanceof RegelSpraakRegelContext)) p = p.getParent();
        return p == null ? 0 : countUniversal(p, name.trim().replaceAll("\\s+", " "));
    }
    private int countUniversal(org.antlr.v4.runtime.tree.ParseTree t, String name) {
        int n = t instanceof UniverseelOnderwerpContext u && u.kaleNaam() != null
                && src(u.kaleNaam()).trim().replaceAll("\\s+", " ").equalsIgnoreCase(name) ? 1 : 0;
        for (int i = 0; i < t.getChildCount(); i++) n += countUniversal(t.getChild(i), name);
        return n;
    }

    /** §5.5.3/§13.2.12 consume a rangtelwoord on a reference to the onderwerp {@code name}. Returns null when
     *  the reference may resolve as the BARE form — the ordinal trivially selects the rule's single (or sole
     *  implied) binding — or the Expr that replaces it:
     *  - positie beyond the rule's same-named bindings: ERROR (the rule does not name that many onderwerpen);
     *  - two or more same-named bindings: the genuinely multi-bound case needs one pattern variable PER
     *    mention, which the single-subject rule IR does not carry — WARNING + honest marker, never a silent
     *    alias of one binding (which is what dropping the ordinal would mean);
     *  - eerstgenoemde/laatstgenoemde/positie-1 over at most one binding: the bare resolution IS the answer. */
    private Expr checkOrdinal(RangtelwoordContext r, String name, ParserRuleContext ref, Source source) {
        if (r == null) return null;
        int position = ordinalPosition(r);
        int bindings = countBindings(ref, name);
        if (position > Math.max(bindings, 1)) {
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.ERROR,
                    "RGS-089", params("position", position, "name", name),
                    r.getStart().getLine()));
            return unresolvedMarker(source, null);   // the ERROR above is this marker's diagnosis
        }
        if (bindings >= 2) {
            // §5.5.3 ¶1888 states ONLY that a rangtelwoord points out "de plaats van het bedoelde onderwerp in de
            // regel"; it pins no operational semantics — no worked multi-binding rule, no definition of what the
            // pairing/ranging over the several bindings is, and the typeringen matrix is silent. So a genuinely
            // multi-bound rule is spec-UNDERDETERMINED, not merely unimplemented: we cannot lower it soundly (which
            // mention order, which pairing). It stays a visible WARNING + honest marker in both back-ends — never a
            // silent alias of one binding (which is what dropping the ordinal would mean).
            model.diagnostics.add(new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "RGS-090", params("bindings", bindings, "name", name, "expression", source),
                    r.getStart().getLine()));
            return unresolvedMarker(source, null);   // the WARNING above is this marker's diagnosis
        }
        return null;
    }

    // ---------------- references ----------------
    /** `<attribuut> van <onderwerp>`: bind the attribute's datatype via the subject's object type. */
    Expr buildAttribute(AttribuutVanOnderwerpContext a) {
        Source source = source(a);
        if (a != null && a.attribuutMetLidwoord() == null && a.rangtelwoord() != null && a.kaleNaam() != null) {
            // §13.4.2.9 the ordinal-ATTRIBUTE form "de <rangtelwoord> <attribuutnaam> van <onderwerp>". The
            // eerstgenoemde/laatstgenoemde/1st reading over a single mention is the bare attribute read; a
            // deeper position needs per-mention binding (§5.5.3) and stays an honest marker like the subject form.
            Expr blocked = checkOrdinal(a.rangtelwoord(), src(a.kaleNaam()), a, source);
            return blocked != null ? blocked : attributeRef(src(a.kaleNaam()), a.onderwerp(), source);
        }
        // GEEN GETUIGE: de enige aanroeper geeft een gevulde context mee, `attribuutMetLidwoord` is
        // `lidwoord? bareName` (de naam is er dus altijd), en de rangtelwoord-vorm is hierboven al
        // afgehandeld — er blijft geen invoer over die hier uitkomt. Gemeten 2026-09-19.
        if (a == null || a.attribuutMetLidwoord() == null || a.attribuutMetLidwoord().kaleNaam() == null)
            return unresolvedMarker(a, Msg.frontend("RGS-092.attribute-reference"));
        String attr = src(a.attribuutMetLidwoord().kaleNaam());
        // §13.4.17.12/§6.8 "het percentage P van V" WITHOUT the '%': our grammar requires '%' on percentageVanExpr to
        // disambiguate from a 'van' navigation (grammar/DIVERGENCES.md, "The percentage forms"), so the no-'%' form
        // parses HERE as a navigation. But when the leftmost name P is a Percentage-typed PARAMETER (never a real
        // attribuut of any objecttype), this IS the percentage function, not a navigation — reinterpret it as
        // PERCENTAGE_VAN(P, V) (V·P/100, §4.8 Tabel 19; leeg telt als 0). A name that is no Percentage parameter, or a
        // V we cannot build, falls through to the normal navigation (an honest marker, never a wrong gate).
        // The greedy bareName absorbs every inner 'van' ("korting van de prijs" -> ONE bareName of two delen), so the
        // percentage P is the LEFTMOST kaleNaamDeel and V is the ordinary value read "<every remaining deel> van
        // <onderwerp>", which walks a chain of to-one steps as any read does ("het taxatiebedrag van de taxatie van
        // de Vondst"); with no inner 'van' (one deel) V is the onderwerp built as a value, or a "Daarbij geldt"
        // variable it names.
        // Each operand carries the text that writes it, so that a sentence naming the operands with their values
        // names each once: P its name part without the sign (the sign is the function's, as in the '%' form), V
        // the remaining delen with the onderwerp.
        java.util.List<KaleNaamDeelContext> parts = a.attribuutMetLidwoord().kaleNaam().kaleNaamDeel();
        Parameter pct = percentageParameter(src(parts.get(0)));
        if (pct != null) {
            int before = model.diagnostics.size();
            Expr v = parts.size() == 1 ? percentageBase(a.onderwerp())
                   : attributeRef(stripArticle(String.join(" van ",
                           parts.subList(1, parts.size()).stream().map(ExpressionBuilder::src).toList())),
                           a.onderwerp(), tailSource(parts.get(1), a));
            // §4.8 Tabel 18 requires a NUMERIEK right operand: only reinterpret as the percentage function when V is
            // a getalachtig value. This guards the name collision where the leftmost name is BOTH a Percentage
            // parameter AND a real attribute reachable through the onderwerp ("de korting van zijn bestelling", korting
            // also a Bestelling attribute): there V is a to-one ROLE navigation (type unknown), so the percentage
            // reading is rejected and the construct stays the correct attribute navigation.
            if (v != null && !(v instanceof Expr.Unresolved) && v.type().isNumberLike()) {
                // §4.8 Tabel 18: the result carries V's unit; §4.8 Precisie makes it an exact fraction, so drop V's
                // declared decimal count (the same typing the '%' form uses, lines above).
                Datatype t = v.type() instanceof Datatype.Numeric n
                        ? new Datatype.Numeric(false, 0, Datatype.Sign.ONBEPAALD, n.unit()) : unknown();
                Source name = Source.of(parts.get(0), withoutPercentSign(src(parts.get(0))));
                return new Expr.Binary(Expr.BinOp.PERCENTAGE_VAN, new Expr.ParameterRef(pct, name), v, t, source);
            }
            // The reading is not taken, so what building V raised is not the sentence's: the navigation below reads
            // the same onderwerp and raises its own (an ordinal it cannot place was otherwise reported twice).
            model.diagnostics.subList(before, model.diagnostics.size()).clear();
        }
        return attributeRef(attr, a.onderwerp(), source);
    }

    /** Build "&lt;attr&gt; van &lt;onderwerp&gt;" into an {@link Expr.AttributeReference} — the shared core of
     *  {@link #buildAttribute} and {@link #subjectAsValue}. In a filter or a quantifier test a read of the subject of the
     *  form is built at that subject and carries the form anchor (IR-SPEC ch. 4.1a); a name the IR cannot reach from
     *  there is refused ({@link #reach}). */
    private Expr attributeRef(String attr, OnderwerpContext subjectType, Source source) {
        Anchor a = subjectType == null ? null : anchorOf(subjectType);
        Reach reach = reach(a);
        Expr refused = refusedReach(reach, a, source, unknown(), source, false);
        if (refused != null) return refused;
        if (reach != Reach.FORM) return attributeRefAt(attr, subjectType, source);
        if (!inPerElementContext()) {
            Expr built = attributeRefAt(attr, subjectType, source);
            explicitSubjectReads.add(built);
            return built;
        }
        Expr built = atForm(() -> attributeRefAt(attr, subjectType, source));
        Anchored x = anchor(built);
        if (x.refusal() == null) return x.expr();
        refuseRuleSubject(source, x.refusal(), source);
        return new Expr.Unresolved(built.type(), source, null);   // the ERROR above is this marker's diagnosis
    }

    /** {@link #attributeRef} from the current subject. */
    private Expr attributeRefAt(String attr, OnderwerpContext subjectType, Source source) {
        String subjectSource = subjectType != null ? withoutSelf(subjectType) : "";
        // §13.4.1.3/§5.5.3 a rangtelwoord on the SUBJECT reference ("de bijdrage van de als tweede genoemde
        // deelnemer"): consume the ordinal — the bare resolution when it selects the rule's single binding,
        // an ERROR/marker otherwise (checkOrdinal). Never silently dropped (the pre-consumption behavior
        // aliased every mention to one binding).
        OnderwerpVerwijzingContext ov = referenceWithOrdinal(subjectType);
        if (ov != null) {
            Expr blocked = checkOrdinal(ov.rangtelwoord(), src(ov.kaleNaam()), ov, source);
            if (blocked != null) return blocked;
        }
        ObjectType ot = resolveSubject(subjectType);
        // A set read whose written range walks two steps or more: the walk decides the element type where the
        // element name is a role name and no object type (walkRange), the head noun's binding otherwise.
        if (isSetSubject(subjectType)) {
            ObjectType walked = walkRange(subjectType).element();
            if (walked != null) ot = walked;
        }
        // The role the author wrote to reach `ot` (the noun of a DEFINITE reference or a
        // possessive, when it names a feittype role) — threaded so the transpiler
        // navigates the exact field even when the (subject, ot) pair is connected by more
        // than one feittype/role. An indefinite "een <X>" is the universal subject, not a
        // navigation, so it is excluded (it stays an own-attribute read).
        String noun = navigationNoun(subjectType);
        String navRole = model.navigationRoleName(noun);
        // §13.4.1.2/.3: a navigation filler may name an objecttype OR a role. Take the
        // role reading only when the objecttype reading is NOT the navigation the author
        // wrote — it is not the subject's own type (a definite back-reference to the
        // subject's type reads the subject's own attribute, §5.5.3), AND no role NAMED as
        // written reaches that objecttype from the subject. Then, when a same-named role
        // uniquely reaches a type that actually CARRIES the attribute, bind the role and
        // record it for the join. Scoped to a single-object definite/possessive reference
        // (`noun` is null for an indefinite subject or a set quantifier) and an
        // unambiguous, attribute-bearing role, so an honestly unresolvable reference stays
        // a marker rather than becoming a wrong join or a getter for an absent property.
        if (noun != null && currentSubject != null && ot != currentSubject
                && model.connectingRoleViaName(currentSubject, ot, noun) == null) {
            ObjectType viaRole = model.roleTargetTypeToSubject(currentSubject, noun);
            if (viaRole != null && viaRole.attribute(attr) != null) { ot = viaRole; navRole = noun; }
        }
        // §8.1.5 + Tabel 16 NEGATED navigated dagsoortcontrole ("de <datum> van zijn <rol> GEEN <dagsoort> is"): the
        // greedy onderwerp absorbs the trailing "geen" into the navigation role's bareName ("reis geen"), so the real
        // role fails to resolve. Recover the real role (mirror absorbedNegator) when the stripped role uniquely reaches
        // a DATUM attribute, keeping the bron's trailing " geen" so RuleChecker.characteristicCheckPredicateBare case (b) builds the
        // NEGATED dagsoort predicaat. Scoped to a datum attr (the dagsoort operand), so a navigated kenmerk negation
        // (still out of scope) stays a marker.
        if ((ot == null || ot.attribute(attr) == null) && noun != null && currentSubject != null
                && noun.length() > 5 && noun.regionMatches(true, noun.length() - 5, " geen", 0, 5)) {
            String realRole = noun.substring(0, noun.length() - 5).stripTrailing();
            ObjectType viaRole = model.roleTargetTypeToSubject(currentSubject, realRole);
            Datatype rdt = viaRole != null ? viaRole.attribute(attr) : null;
            // §8.1.5/§5.15/Tabel 57: a dagsoort operand may be ANY datum/tijd (Datum-in-dagen OR Datum-en-tijd); both
            // back-ends take the date part for the day-granular §9.8 predicate. Scoped to a datum attr, so a navigated
            // kenmerk negation (still out of scope) stays a marker.
            if (rdt != null && rdt.isDate()) { ot = viaRole; navRole = realRole; }
        }
        Datatype dt = ot != null && ot.attribute(attr) != null ? ot.attribute(attr) : null;
        if (dt == null && ot != null) {
            // §3.7/§5.1 an appositive "in <eenheid>" on an attribuut read — a §12 conditie such as
            // "de looptijd IN MINUTEN van zijn meting" re-states the attribute's own unit inline. This is not its
            // own EBNF production (the only grammar "in <eenheid>" is the §13.4.17.32 tijdsduur "in hele
            // <eenheid>"), so the unit is greedily swallowed into the bareName; but the spec body writes it and
            // it merely RE-STATES the attribute's own unit (the spec always names the attribute's NATIVE unit:
            // "in minuten" on a minuut attribute). Strip the suffix and retry on the bare name, keeping the native
            // type, so BOTH back-ends lower the bare AttributeReference identically (resolver-only fix).
            // IDENTITY-only, deliberately: a non-identity but convertible unit ("in jaren" on a mnd attribute)
            // would need a REAL conversion of the raw read (×1/12), not a name strip — dropping the qualifier and
            // reading the native magnitude would silently compare 6[mnd] to a 1[jr] literal as "6 > 1" (a
            // shared-bug both back-ends agree on). No spec example forms a non-native qualifier, so it stays an
            // honest marker. An attribute name that merely contains " in ", or a non-convertible tail, also markers.
            int sp = attr.lastIndexOf(" in ");
            if (sp > 0) {
                Datatype bareDt = ot.attribute(attr.substring(0, sp));
                Unit nativeE = bareDt == null ? null : bareDt.unitOf();
                Unit named = nativeE == null ? null : model.resolveUnit(attr.substring(sp + 4).trim());
                if (nativeE != null && nativeE.convertibleTo(named) && !nativeE.requiredConversion(named)) {
                    attr = attr.substring(0, sp);
                    dt = bareDt;
                }
            }
        }
        // declared-names-only IR: a WRITTEN PLURAL of a declared attribute binds the DECLARED name at resolve time
        // ("de leeftijden van alle deelnemers" reads the attribute declared 'leeftijd'), so the IR only ever
        // carries declared members and no back-end synthesizes a getter from surface morphology. The matcher
        // is ambiguity-safe (declared "(mv: …)" or the conservative regular plural of exactly ONE attribute);
        // an unbindable written form keeps its Unknown type and stays a downstream marker.
        if (dt == null && ot != null) {
            String singular = PluralMatcher.findAttribute(ot, attr);
            if (singular != null) {
                attr = singular;
                dt = ot.attribute(singular);
            }
        }
        // §13.4.1 MULTI-HOP to-one navigation chain ("de X van de Y van de Z"): the greedy bareName absorbs
        // every inner 'van', so a chained reference reaches here as ONE attr string ("capaciteit van het
        // vliegtuig") whose TRAILING delen are role hops beyond the onderwerp. When no direct reading above
        // bound an attribute, walk those delen outward from the onderwerp's type: each deel must resolve to a
        // to-ONE hop (chainHop: written role name or objecttype name) and the remaining prefix must name a
        // declared attribute — checked at EVERY boundary, so a multi-word attribute name containing 'van'
        // ("datum van vertrek") wins over deeper navigation and is never split. The resolved DECLARED role
        // names (subject-first, including the onderwerp hop) thread into the IR as navigationPath; both
        // back-ends re-validate the walk against their pattern subject (followChain) and emit one null-guarded
        // getter per hop. Any unresolvable/ambiguous/to-many hop declines the WHOLE chain (dt stays null ->
        // the downstream marker, never a wrong join). navigationRole stays non-null for a chain (the AttributeReference
        // invariant), so every own-attribute fast path routes it to the navigated branches.
        java.util.List<String> navigationPath = java.util.List.of();
        if (dt == null && currentSubject != null && noun != null && attr.contains(" van ")
                && !isSetSubject(subjectType)) {
            // The onderwerp anchor: the type the chain starts its delen-walk from. The 1326 fix-up above only
            // rebinds ot when the ROLE TYPE carries the (still-chained) attr — which fails for a chain by
            // construction — so recover the role reading here when ot is still unbound.
            ObjectType anchor = ot != null ? ot : model.roleTargetTypeToSubject(currentSubject, noun);
            java.util.List<String> path = null;
            ObjectType cur = null;
            if (anchor != null) {
                // First hop (subject -> anker), mirroring the transpiler's naviRol order: by written name,
                // then the §5.5.3 own-type back-reference (no hop), then by pair; declined when to-many.
                String role1 = navRole != null ? model.connectingRoleViaName(currentSubject, anchor, navRole) : null;
                boolean ownStart = role1 == null && anchor == currentSubject;
                if (role1 == null && !ownStart) role1 = model.connectingRole(currentSubject, anchor);
                if (ownStart || (role1 != null && !model.toManyRole(currentSubject, anchor, role1))) {
                    path = new java.util.ArrayList<>();
                    if (!ownStart) path.add(role1);
                    cur = anchor;
                }
            }
            if (path != null) {
                String[] parts = attr.split("\\s+van\\s+");
                for (int i = parts.length - 1; i >= 1; i--) {
                    RegelSpraakModel.ChainHop hop = model.chainHop(cur, stripArticle(parts[i]).trim());
                    if (hop == null) break;   // unresolvable / ambiguous / to-many -> decline the whole chain
                    path.add(hop.role());
                    cur = hop.targetName();
                    String prefix = String.join(" van ", java.util.Arrays.copyOfRange(parts, 0, i));
                    Datatype d2 = cur.attribute(prefix);
                    if (d2 == null) {
                        String ev = PluralMatcher.findAttribute(cur, prefix);
                        if (ev != null) { prefix = ev; d2 = cur.attribute(ev); }
                    }
                    if (d2 != null) {
                        attr = prefix; ot = cur; dt = d2;
                        navigationPath = java.util.List.copyOf(path);
                        if (navRole == null) navRole = noun;   // invariant: a chain always reads as navigated
                        break;
                    }
                }
            }
        }
        if (dt == null) dt = unknown();
        return new Expr.AttributeReference(attr, ot, subjectSource, navRole, dt, source, isSetSubject(subjectType),
                navigationPath, rangePath(subjectType), writtenQuantifier(subjectType),
                writtenThreshold(subjectType));
    }

    /**
     * §5.8/§13.4.2 the MULTI-STEP fold-source range ("de &lt;attr&gt; van alle X van alle Y van &lt;onderwerp&gt;"),
     * resolved into the symbolic bindings the uchar wire carries as {@code sourcePath} (IR-SPEC ch. 4.3,
     * WF-102). The written names are authoritative (§13.4.1: the written role IS the navigation), which is
     * the whole point — a by-PAIR derivation looks only at TYPES and will bind
     * a coincidental direct subject-to-element relation, folding a different set from the one the sentence
     * names.
     *
     * <p>Empty — leaving the caller's existing behaviour untouched — for every shape this does not decide:
     * a single written step (whose range IS one relation, spelled {@code sourceRole}), a non-set onderwerp, an
     * anchor that denotes neither the subject's object type nor the subject itself, a step that resolves to
     * no role or to several, a reciprocal fact type, and a walk that does not land on the element type the
     * head noun already bound. Declining the WHOLE path rather than binding part of it is what keeps the
     * two backends symmetric by construction: they consume the same path or none.
     *
     * <p>The ANCHOR (the innermost written segment) may name a ROLE instead of an object type, and the test
     * is the one ch. 4.1a already fixes rather than a new one: far-role resolution FIRST — a role reachable
     * FROM the subject is a hop whose filler is another instance, so it belongs on the path as a step and
     * never as the anchor — and the self-denoting reading only at ZERO far hits, where the subject type
     * itself fills a role of that name ("van de aanmelder", read on the Jutter who IS the aanmelder).
     */
    private java.util.List<RegelSpraakModel.RoleBindingRef> rangePath(OnderwerpContext subjectType) {
        return walkRange(subjectType).path();
    }

    /** What the walk of a written fold-source range found: the path and the element type it lands on, or, where it
     *  walked a chain of three segments or more and declined it, why — a clause about the written source
     *  ({@code refusal}) — and the way out a declaration gives where there is one ({@code exit}). The element type
     *  and both texts are null for every shape the walk does not decide, the one-step range among them. */
    private record RangeWalk(java.util.List<RegelSpraakModel.RoleBindingRef> path, Msg refusal,
                                   Msg exit, ObjectType element) {
        static final RangeWalk GEEN = new RangeWalk(java.util.List.of(), null, null, null);
        static RangeWalk refused(Msg refusal, Msg exit) {
            return new RangeWalk(java.util.List.of(), refusal, exit, null);
        }
    }

    /** The walk behind {@link #rangePath}, with the reason where it declines. One walk for both, so that the path
     *  the emitter writes and the refusal {@link #checkFoldSource} words cannot disagree about one written chain. */
    private RangeWalk walkRange(OnderwerpContext subjectType) {
        if (subjectType == null || currentSubject == null || !isSetSubject(subjectType))
            return RangeWalk.GEEN;
        java.util.List<String> segments = new java.util.ArrayList<>();
        if (!rangeSegments(subjectType, segments)) return RangeWalk.GEEN;
        return walkSegments(segments, src(subjectType));
    }

    /** The written segments of {@code o}'s chain, outside-in, appended to {@code segments}; false where a link of the
     *  chain has no name. Two shapes produce them, and both are walked: a nested set onderwerp is its own parse node,
     *  while an inner TO-ONE step is swallowed by the greedy bareName (§13.4.1, the same split the navigationPath
     *  block above performs). */
    private boolean rangeSegments(OnderwerpContext o, java.util.List<String> segments) {
        for (OnderwerpContext rest = o; rest != null; rest = rest.onderwerp()) {
            String kn = rest.kaleNaam() != null ? src(rest.kaleNaam()) : primaryName(rest);
            if (kn == null) return false;
            for (String part : kn.split("\\s+van\\s+")) segments.add(stripArticle(part).trim());
        }
        return true;
    }

    /** The walk of written segments, outside-in, from the anchor (the last) to the element (the first); {@code source} is
     *  the written range its refusals quote. */
    private RangeWalk walkSegments(java.util.List<String> segments, String source) {
        // Two hops plus an anchor is the shortest thing this field exists for; anything shorter is the
        // one-step range and keeps its own spelling.
        if (segments.size() < 3) return RangeWalk.GEEN;
        String anchor = segments.get(segments.size() - 1);
        if (!anchorsSubject(anchor))
            return RangeWalk.refused(Msg.frontend("PART.range-not-at-subject", "source", source, "anchor", anchor,
                    "subject", currentSubject.name), null);
        java.util.List<RegelSpraakModel.RoleBindingRef> path = new java.util.ArrayList<>();
        ObjectType holder = currentSubject;
        for (int i = segments.size() - 2; i >= 0; i--) {
            RegelSpraakModel.RangeHop hop = model.rangeHop(holder, segments.get(i));
            if (hop == null) return stepRefusal(source, holder, segments.get(i));
            path.add(hop.binding());
            holder = hop.targetName();
        }
        // The last step bound the written element name to a role from the holder before it, so the walk lands on
        // that role's filler. Where the name is no object type (not its name, not its declared plural), it named
        // the role, and the role decides the element type: the filler. Where it names an object type, that type is
        // the population, which a subtype of the filler narrows (IR-SPEC ch. 2.2) and any other type contradicts:
        // binding it anyway would fold a set the sentence does not name (grammar/DIVERGENCES.md §13.4.1).
        ObjectType written = elementNameObjectType(segments.get(0));
        if (written != null && written != holder && !written.isA(holder))
            return RangeWalk.refused(Msg.frontend("PART.range-element-mismatch", "source", source, "holder", holder.name,
                    "written", written.name), null);
        return new RangeWalk(java.util.List.copyOf(path), null, null, written == null ? holder : written);
    }

    /** The object type a written element name names as a whole — its name or its declared plural — or null; a role
     *  name that is no object type's name is no object type here, and no leading word is dropped. */
    private ObjectType elementNameObjectType(String name) {
        ObjectType t = model.objectTypeByName(name);
        return t != null ? t : PluralMatcher.findObjectType(model.objectTypes.values(), name);
    }

    /** Why the written step {@code step} binds no role from {@code holder}: the classes of
     *  {@link RegelSpraakModel#stepCandidates}, so the words follow the search that declined the step. A plural the
     *  model does not declare binds no step, also where the one-step range accepts it: the uchar wire has no
     *  best-effort plural (IR-SPEC ch. 2.6, decided 2026-08-25), and a path step has no type-pair derivation to fall
     *  back on (WF-102). Only that cause has a way out a declaration gives, so only it carries one. */
    private RangeWalk stepRefusal(String source, ObjectType holder, String step) {
        RegelSpraakModel.StepCandidates k = model.stepCandidates(holder, step);
        java.util.function.Function<Msg, Msg> head = tail -> Msg.frontend("PART.step-refusal", "source", source,
                "step", step, "holder", holder.name, "tail", tail);
        if (k.declared().size() > 1)
            return RangeWalk.refused(head.apply(Msg.frontend("PART.step-tail-ambiguous", "step", step,
                    "roles", roles(k.declared(), false))), null);
        if (k.declared().size() == 1)
            return RangeWalk.refused(head.apply(Msg.frontend("PART.step-tail-undeclared-filler",
                    "roles", roles(k.declared(), false), "filler", k.declared().get(0).role().objectTypeName())), null);
        if (!k.derived().isEmpty())
            return RangeWalk.refused(head.apply(Msg.frontend("PART.step-tail-derived-plural", "step", step,
                    "roles", roles(k.derived(), false))),
                    k.derived().size() == 1
                            ? Msg.frontend("PART.step-exit-one", "role", k.derived().get(0).role().name(), "step", step)
                            : Msg.frontend("PART.step-exit-several", "step", step));
        if (!k.reciprocal().isEmpty())
            return RangeWalk.refused(head.apply(Msg.frontend("PART.step-tail-reciprocal", "step", step,
                    "roles", roles(k.reciprocal(), true))), null);
        return RangeWalk.refused(head.apply(Msg.frontend("PART.step-tail-none", "holder", holder.name, "step", step)), null);
    }

    /** The candidates a step refusal names, each once: "de rol 'x' van het feittype 'f'", or "de rollen 'x' van het
     *  feittype 'f' en 'y' van het feittype 'g'". */
    private static Msg roles(java.util.List<RegelSpraakModel.StepCandidate> candidates, boolean reciprocal) {
        java.util.List<Msg> parts = candidates.stream().distinct()
                .map(k -> Msg.frontend(reciprocal ? "PART.role-of-reciprocal-fact-type" : "PART.role-of-fact-type",
                        "role", k.role().name(), "factType", k.factType().name))
                .toList();
        return parts.size() == 1 ? Msg.frontend("PART.roles-one", "part", parts.get(0))
                : Msg.frontend("PART.roles-several", "head", new Msg.Joined(parts.subList(0, parts.size() - 1), ", "),
                        "last", parts.get(parts.size() - 1));
    }

    /** Whether the written anchor segment denotes the RULE SUBJECT — literally by object-type name, or
     *  (ch. 4.1a's order: only at zero far hits) by a role the subject itself fills. */
    private boolean anchorsSubject(String anchor) {
        for (String candidate : RegelSpraakModel.nameCandidates(anchor))
            if (model.objectTypeByName(candidate) == currentSubject) return true;
        return model.roleHitsToSubject(currentSubject, anchor) == 0
                && model.fillsSelfRole(currentSubject, anchor);
    }

    /** The {@link Parameter} named {@code name} (lidwoord-stripped) when it is Percentage-typed — the §13.4.17.12
     *  "het percentage P van V" left operand. Null otherwise. §13.4.17.12 lets a '%' follow P, and the name parts
     *  take it as a word of their own ("percentage havengeld %"), so a trailing '%' is no part of the name. */
    private Parameter percentageParameter(String name) {
        String withoutSign = withoutPercentSign(name);
        String stripped = stripArticle(withoutSign);
        Parameter p = model.parameters.get(stripped);
        if (p == null) p = model.parameters.get(withoutSign);
        return p != null && p.datatype() != null && p.datatype().isPercentage() ? p : null;
    }

    private static String withoutPercentSign(String name) {
        return name.replaceFirst("\\s*%$", "");
    }

    /** The source of {@code ctx} from the start of {@code from}, a context inside it, to its end, as
     *  {@link DefinitionResolver#snippet} writes it: without a closing period, and empty for an empty match. */
    private static Source tailSource(ParserRuleContext from, ParserRuleContext ctx) {
        return Source.of(from, DefinitionResolver.snippet(from.getStart(), ctx.getStop()));
    }

    /** The V of "het percentage P van V" when no name part follows P: the onderwerp built as a value, or a §11
     *  "Daarbij geldt" variable the onderwerp names ("het percentage havengeld van het getaxeerde bedrag"), read as
     *  {@link #nameRef} reads it at this position. Null when it is neither (the caller keeps the navigation). */
    private Expr percentageBase(OnderwerpContext o) {
        Expr v = subjectAsValue(o);
        if (v != null || o == null) return v;
        String name = src(o);
        String key = variableKey(name);
        boolean variable = binderMode ? binders.containsKey(key) : visible.containsKey(key);
        return variable ? nameRef(name, source(o)) : null;
    }

    private static String stripArticle(String name) {
        return name.replaceFirst("^(?i)(de|het|een)\\s+", "");
    }

    /** Build an onderwerp as a VALUE expression — the V in "het percentage P van V". Handles the
     *  "de &lt;attr&gt; van &lt;subject&gt;" navigation ({@code selector VAN onderwerp}), the possessive
     *  "zijn &lt;attr&gt;", and a bare subject-own "de &lt;attr&gt;"; otherwise null (the caller keeps the marker). */
    private Expr subjectAsValue(OnderwerpContext o) {
        if (o == null) return null;
        // a chain that carries a subselection is no plain navigation or subject-own read
        OnderwerpKetenContext k = o.onderwerpKeten() != null && o.onderwerpKeten().predicaat() == null
                ? o.onderwerpKeten() : null;
        if (k != null && k.selector() != null && k.selector().kaleNaam() != null && k.onderwerp() != null)
            return attributeRef(src(k.selector().kaleNaam()), k.onderwerp(), source(o));   // "de <attr> van <subject>"
        Expr poss = subjectPossessive(o, source(o));
        if (poss != null) return poss;                                                  // "zijn <attr>"
        // §5.5.3 a bare definite subject-own attribute referring to the rule subject ("de <attr>") — either the
        // onderwerpVerwijzing form (under an onderwerpKeten) or the kwantificatie+bareName form (k == null). Only the
        // bare definite (NOT a set quantifier alle/geen/…) and only an attribute the subject actually carries.
        String bare = k != null && k.onderwerpVerwijzing() != null && k.onderwerpVerwijzing().kaleNaam() != null
                ? src(k.onderwerpVerwijzing().kaleNaam())
                : (o.kaleNaam() != null && !isSetSubject(o) ? src(o.kaleNaam()) : null);
        if (bare != null && currentSubject != null) {
            Datatype dt = currentSubject.attribute(bare);
            if (dt != null) return new Expr.AttributeReference(bare, currentSubject, src(o), null, dt, source(o));
        }
        return null;
    }

    /** §13.4.2 whether {@code o}'s top-level quantifier ranges over a SET — a kwantificatie that is NOT the bare
     *  definite "de" (alle / al / geen van de / ten minste … / ten hoogste … / precies …). A possessive, an
     *  indefinite "een &lt;X&gt;" universal subject, or an onderwerpketen carries no kwantificatie and is a single
     *  object. Computed once from the parse tree, not by a text scan of subjectSource for "alle ", so it catches the
     *  threshold quantifiers too and is immune to casing/spacing. */
    private static boolean isSetSubject(OnderwerpContext o) {
        if (o == null || o.kwantificatie() == null) return false;
        KwantificatieContext q = o.kwantificatie();
        return !(q.DE() != null && q.getChildCount() == 1);     // bare "de" = one object; every other quantor = a set
    }

    /** §13.4.14.5 the WRITTEN quantifier of {@code o}, as the IR's own {@link Condition.Quantifier} vocabulary —
     *  what {@link #isSetSubject} throws away when it collapses every non-bare quantor to one bit.
     *  Recording which one the sentence used is exactly what lets a consumer (today: the §8.2
     *  {@link Condition.QuantifierFold} lowering) answer the quantifier that was WRITTEN, instead of reading
     *  them all as a forall. Null for the bare definite "de", which names one object and no set at all. */
    private static Condition.Quantifier writtenQuantifier(OnderwerpContext o) {
        return o == null ? null : writtenQuantifier(o.kwantificatie());
    }
    private static Condition.Quantifier writtenQuantifier(KwantificatieContext q) {
        if (q == null) return null;
        if (q.ALLE() != null || q.AL() != null) return Condition.Quantifier.ALLE;
        if (q.GEEN_VAN_DE() != null)            return Condition.Quantifier.GEEN;
        if (q.TEN_MINSTE() != null)             return Condition.Quantifier.TEN_MINSTE;
        if (q.TEN_HOOGSTE() != null)            return Condition.Quantifier.TEN_HOOGSTE;
        if (q.PRECIES() != null)                return Condition.Quantifier.PRECIES;
        return null;                            // bare "de"
    }

    /** §13.4.14.4 the telwoord N of a COUNTING quantifier on {@code o} ("ten minste 2 van de …"), 0 when the
     *  written quantor carries none. It travels beside {@link #writtenQuantifier} for the same reason that
     *  one does: without it "ten minste 2 van de" and "ten minste 3 van de" reach a consumer as one and the
     *  same sentence. An unparseable or absent telwoord stays 0 and is refused downstream (WF-041), never
     *  silently read as 1. */
    private static int writtenThreshold(OnderwerpContext o) {
        return o == null ? 0 : writtenThreshold(o.kwantificatie());
    }
    private static int writtenThreshold(KwantificatieContext q) {
        return q == null ? 0 : RuleChecker.numeralValue(q.telwoord());
    }

    /** The facts a QUANTIFIED MEMBER PHRASE carries, read off the parse tree: the written quantor, its
     *  telwoord, the element type it ranges over, and the written source (the {@code subjectSource} a
     *  range binding is looked up by).
     *
     *  <p>{@code reason}, when non-null, means the phrase IS a member phrase but the fold declines it, and
     *  names the axis it declined on — the {@link Subselection} idiom, for the same reason: one generic
     *  text for every shape tells the author nothing. The other fields are then empty.
     *
     *  <p>{@code path} is the range of a member phrase written over two relations or more, the {@code sourcePath}
     *  of the walk ({@link #walkRange}'s, over the member name and the onderwerp); empty for the one-step range, which
     *  the caller binds itself. {@code anchorTail} is, for such a phrase, what the parse took after the name of the
     *  anchor (the words {@link #anchorTail} reads for the one-step range); null otherwise. */
    record QuantifiedSubject(Condition.Quantifier quantifier, int threshold, ObjectType element,
                                    String subjectSource, Msg reason,
                                    List<RegelSpraakModel.RoleBindingRef> path, String anchorTail) {
        QuantifiedSubject(Condition.Quantifier quantifier, int threshold, ObjectType element, String subjectSource) {
            this(quantifier, threshold, element, subjectSource, null, List.of(), null);
        }
        /** A declined member phrase: the written source and the axis it was declined on. */
        static QuantifiedSubject refused(String subjectSource, Msg reason) {
            return new QuantifiedSubject(null, 0, null, subjectSource, reason, List.of(), null);
        }
    }

    /**
     * §13.4.2.9 / ch. 4.5: the quantified member phrase an expression consists OF — "alle wachten van
     * de Schouwploeg" as the WHOLE left operand of a §8.1.8 kenmerkcheck. The §13.4.2.9
     * {@code kwantificatie? attribuutMetLidwoord VAN onderwerpKeten} puts the quantifier on the
     * attribuutVanOnderwerp ITSELF there, not on an inner onderwerp, so the phrase resolves as "the
     * &lt;wachten&gt; of the Schouwploeg" — a reference whose {@code set} bit is false and whose
     * {@code quantifier} is null, which is why the fold's {@link RuleChecker} operand scan finds nothing
     * to quantify. This is the parse-tree reading of the same facts, for the caller that folds over a
     * MEMBERSHIP matrix instead of a value one.
     *
     * <p>Null for everything that is not this shape AT ALL — the caller then has nothing to say about a
     * fold, because the sentence writes none. A phrase that IS the shape but declines comes back
     * {@link QuantifiedSubject#refused geweigerd}, naming its axis: the bare definite "de" over
     * a DECLARED PLURAL (one object, no set, but written as a set), a member phrase whose element type
     * does not resolve — quantifying over an unresolved population is the silent-wrong-set failure a
     * marker is preferable to — and a MULTI-STEP written range ("alle wachten van alle ploegen van de
     * Haven") that the walk of {@link #walkRange} does not bind, with the walk's own reason where it gave one.
     * A multi-step range the walk binds comes back with its {@code path} (card frontend-kwantor-over-een-pad,
     * 2026-10-20): the fold carries it as {@code sourcePath} (IR-SPEC ch. 4.5, WF-108 (c)).
     *
     * <p>The bare "de" declines QUIETLY (null, not geweigerd) unless the member name is a declared
     * plural: "de reis van de passagier" is an ordinary to-one navigation, and telling its author that a
     * quantifier fold declined would name a reading the sentence never had.
     */
    QuantifiedSubject quantifiedSubject(ExpressieContext e) {
        PrimaireExpressieContext p = e instanceof PrimaireExprContext pe ? pe.primaireExpressie() : null;
        AttribuutVanOnderwerpContext a = p == null ? null : p.attribuutVanOnderwerp();
        if (a == null || a.attribuutMetLidwoord() == null || a.attribuutMetLidwoord().kaleNaam() == null
                || a.onderwerp() == null) return null;
        Condition.Quantifier q = writtenQuantifier(a.kwantificatie());
        String members = src(a.attribuutMetLidwoord().kaleNaam());
        if (q == null)
            return !isDeclaredPlural(members) ? null : QuantifiedSubject.refused(src(a),
                    Msg.frontend("PART.fold-bare-definite", "members", members));
        // More than ONE written hop is a path. The greedy bareName swallows an inner to-one step, so a written
        // "van" on EITHER side of the hop — in the member phrase or in the anchor — is what tells the two apart
        // (the quantifier's own "van de" is not part of either, which is why this is not counted over the whole
        // phrase).
        if (isSetSubject(a.onderwerp()) || hasStep(members) || hasStep(src(a.onderwerp())))
            return pathSubject(a, q, members);
        ObjectType elem = model.resolveObjectType(members);
        if (elem == null) elem = typeViaPlural(members);
        return elem != null
                ? new QuantifiedSubject(q, writtenThreshold(a.kwantificatie()), elem, src(a))
                : QuantifiedSubject.refused(src(a),
                    Msg.frontend("PART.fold-unknown-population", "members", members));
    }

    /**
     * A member phrase written over two relations or more ("ten minste één van de vondsten van de melder van de
     * Melding", "alle wachten van alle ploegen van de Haven"), walked as a value fold's range is ({@link #walkSegments}):
     * the segments of the member name, then those of the onderwerp, the anchor last. The anchor's name runs on over
     * the words of the predicate after it ("… van de Melding boven de drempel is" leaves the anchor "Melding boven de"),
     * so the anchor is cut after the longest name it may call the rule subject by ({@link #subjectNames}) and the rest
     * goes back to the predicate as {@code anchorTail}. A walk that declines refuses the phrase with its own reason; a
     * shape it does not decide, with the reason {@code PART.fold-multi-step}.
     */
    private QuantifiedSubject pathSubject(AttribuutVanOnderwerpContext a, Condition.Quantifier q, String members) {
        java.util.List<String> segments = new java.util.ArrayList<>();
        for (String part : members.split("\\s+van\\s+")) segments.add(stripArticle(part).trim());
        if (currentSubject == null || !rangeSegments(a.onderwerp(), segments))
            return QuantifiedSubject.refused(src(a), Msg.frontend("PART.fold-multi-step"));
        String anchor = segments.get(segments.size() - 1), tail = "";
        String name = openingName(anchor, subjectNames());
        if (name != null) {
            tail = anchor.substring(name.length()).trim();
            segments.set(segments.size() - 1, anchor.substring(0, name.length()));
        }
        RangeWalk walk = walkSegments(segments, src(a));
        if (walk.refusal() != null) return QuantifiedSubject.refused(src(a), walk.refusal());
        if (walk.path().isEmpty()) return QuantifiedSubject.refused(src(a), Msg.frontend("PART.fold-multi-step"));
        return new QuantifiedSubject(q, writtenThreshold(a.kwantificatie()), walk.element(), src(a), null, walk.path(),
                tail);
    }

    /** The names the anchor of a member phrase may call the rule subject by: the type names along its chain, and the
     *  declared name and plural of every role a type of that chain fills. */
    private List<String> subjectNames() {
        List<String> names = new ArrayList<>();
        List<ObjectType> chain = currentSubject.chain();
        for (ObjectType t : chain) names.add(t.name);
        for (RegelSpraakModel.FactType ft : model.factTypes.values())
            for (RegelSpraakModel.Role r : ft.roles)
                if (chain.contains(model.objectTypeByName(r.objectTypeName()))) {
                    names.add(r.name());
                    if (r.plural() != null) names.add(r.plural());
                }
        return names;
    }

    /** The longest of {@code names} that {@code text} opens with, as a whole word, or null. */
    private static String openingName(String text, List<String> names) {
        String chosen = null;
        for (String n : names) {
            if (n == null || n.isBlank()) continue;
            String name = n.trim().replaceAll("\\s+", " ");
            boolean opens = text.equalsIgnoreCase(name) || text.regionMatches(true, 0, name + " ", 0, name.length() + 1);
            if (opens && (chosen == null || name.length() > chosen.length())) chosen = name;
        }
        return chosen;
    }

    /**
     * The words the parse of a quantified member phrase took from the predicate after it. The anchor's name
     * runs on over every name word, so in "alle vondsten van de Jutter geen recht op berging hebben" it reads
     * "Jutter geen recht op" and the operator keeps "berging" alone: the negation and the leading words of a
     * multi-word characteristic stand in the anchor. The anchor names the rule subject, so the words after
     * the name it is written with are the ones the parse took. {@code names} are the names it may be written
     * with; the longest that opens the anchor wins.
     *
     * <p>Empty when the anchor is exactly such a name. Null when the anchor is no plain "de|het|een
     * &lt;naam&gt;" (a subselection, a possessive, a further step) or opens with none of the names: the
     * split is then not read here.
     */
    String anchorTail(ExpressieContext e, List<String> names) {
        PrimaireExpressieContext p = e instanceof PrimaireExprContext pe ? pe.primaireExpressie() : null;
        AttribuutVanOnderwerpContext a = p == null ? null : p.attribuutVanOnderwerp();
        KaleNaamContext anchor = a == null ? null : anchorName(a.onderwerp());
        if (anchor == null) return null;
        String text = src(anchor);
        String chosen = openingName(text, names);
        return chosen == null ? null : text.substring(chosen.length()).trim();
    }

    /** The name of a plain anchor: "de &lt;naam&gt;" as a quantification without a further step, "de|het
     *  &lt;naam&gt;" as a reference without a rangtelwoord, or "een &lt;naam&gt;". Null for any other shape. */
    private static KaleNaamContext anchorName(OnderwerpContext o) {
        if (o == null) return null;
        if (o.kwantificatie() != null)
            return o.kwantificatie().getChildCount() == 1 && o.kwantificatie().DE() != null && o.onderwerp() == null
                    ? o.kaleNaam() : null;
        OnderwerpKetenContext k = o.onderwerpKeten();
        if (k == null || k.predicaat() != null) return null;     // a subselection is no plain anchor
        if (k.onderwerpVerwijzing() != null)
            return k.onderwerpVerwijzing().rangtelwoord() == null ? k.onderwerpVerwijzing().kaleNaam() : null;
        return k.universeelOnderwerp() != null ? k.universeelOnderwerp().kaleNaam() : null;
    }

    /** Whether {@code name} is a DECLARED plural — an objecttype's "(mv: …)" or a feittype role's. It is
     *  what tells a bare "de &lt;leden&gt; van de &lt;X&gt;" written as a SET from an ordinary to-one
     *  navigation ("de reis van de passagier"), so only the former hears that a quantifier fold declined.
     *  Declared forms only: RegelSpraak defines no plural morphology (§3.1 footnote 2), and a synthesised
     *  plural would put that message on sentences that never read as a quantification. */
    private boolean isDeclaredPlural(String name) {
        if (name == null) return false;
        for (ObjectType ot : model.objectTypes.values())
            if (ot.plural != null && ot.plural.equalsIgnoreCase(name)) return true;
        for (RegelSpraakModel.FactType ft : model.factTypes.values())
            for (RegelSpraakModel.Role r : ft.roles)
                if (r.plural() != null && r.plural().equalsIgnoreCase(name)) return true;
        return false;
    }

    /** Whether a written name carries a " van " navigation step of its own. */
    private static boolean hasStep(String name) {
        return name != null && name.toLowerCase().matches(".*\\s+van\\s+.*");
    }

    /** A possessive "zijn <bareName>" resolves to the named attribute OF the rule subject,
     *  when it is one (a "zijn <rol>" navigation is not an attribute and is left to the
     *  generic path). Returns null when it is not a subject-attribute possessive. */
    private Expr subjectPossessive(OnderwerpContext o, Source source) {
        if (currentSubject == null || o.onderwerpKeten() == null) return null;
        OnderwerpKetenContext k = o.onderwerpKeten();
        if (k.ZIJN() == null || k.kaleNaam() == null || k.predicaat() != null) return null;
        String attr = src(k.kaleNaam());
        Datatype dt = currentSubject.attribute(attr);
        // a subject-own attribute — no role navigation
        if (dt != null) return new Expr.AttributeReference(attr, currentSubject, "zijn", null, dt, source);
        // §3.6 the bare name may be a gedimensioneerd attribuut READ with the label woven in
        // ("zijn betaalde belasting in jaar van huidig jaar"): peel "<voorzetsel> <label>" off the run. An
        // own-attribute read carries no navigation role (navRole null), exactly as the scalar branch above.
        Expr cell = dimensionCell(attr, currentSubject, null, "zijn", source);
        if (cell != null) return cell;
        // §8.1.7/§8.1.8 a possessive that names a feittype ROLE the subject reaches ("zijn partner een <k>
        // heeft") is a to-one navigation, not an own attribute — emit the SAME navigated AttributeReference shape
        // buildAttribute produces for the written "de <rol> van <X>", so a navigated kenmerkcheck reaches
        // navigationCharacteristicCheck and a navigated value read reaches the to-one getter (the symmetric mirror of the
        // working "de <rol> van <X>" form; without it the possessive role stayed Unresolved and markered).
        if (model.roleTargetTypeToSubject(currentSubject, attr) != null)
            return new Expr.AttributeReference(attr, currentSubject, "zijn", model.navigationRoleName(attr), unknown(), source);
        // §8.1.8.1 the greedy onderwerp can absorb a TRAILING "niet"/"geen" negator into the possessive role
        // ("zijn reis niet <kenmerk>" -> role "reis niet", unresolvable). Strip it to resolve the role, but keep the
        // FULL bron so navigationCharacteristicCheck/navigationTail recover the negator onto the kenmerk and the §8.1.8 navigated
        // negation de-negates (instead of markering). Verbatim-first above means a role literally ending in
        // niet/geen still binds (it resolved there); only an unresolvable role reaches this split.
        for (String neg : new String[]{" niet", " geen"}) {   // SPACE-bounded (mirror bindCharacteristicCheck) — never split a glued "<rol>geen"
            if (attr.regionMatches(true, attr.length() - neg.length(), neg, 0, neg.length())) {
                String plain = attr.substring(0, attr.length() - neg.length()).trim();
                if (plain.isEmpty()) continue;
                if (model.roleTargetTypeToSubject(currentSubject, plain) != null)
                    return new Expr.AttributeReference(plain, currentSubject, "zijn", model.navigationRoleName(plain), unknown(), source);
                // §8.1.3/§8.1.4 a possessive ATTRIBUTE with a greedily-absorbed trailing "niet" (the vragende negated
                // elfproef / numeriek-met-exact "zijn <attr> niet aan de elfproef voldoet"): resolve the BARE
                // attribuut so the read lowers, keeping the FULL bron so the predicate builder recovers the negation.
                Datatype adt = currentSubject.attribute(plain);
                if (adt != null) return new Expr.AttributeReference(plain, currentSubject, "zijn", null, adt, source);
            }
        }
        // §3.5 a possessive that names a ROLE followed by an ARTICLE-led kenmerk ("zijn reis HET recht op korting
        // heeft") absorbs "reis het recht op" into the role bareName (an article is a kaleNaamWoord, unlike the EEN
        // form whose "een" stays a separate operator token). Find the LONGEST word-prefix that resolves as a role and
        // emit the navigated AttributeReference for it, keeping the FULL bron — navigationTail then recovers the absorbed
        // tail ("het recht op") and navigationCharacteristicCheck rejoins it with the operator's bareName ("korting") -> the kenmerk
        // "recht op korting". Longest-first so a multi-word role wins; a non-empty remainder (n < length) means the
        // exact-role case above already handled n == length.
        String[] words = attr.split("\\s+");
        for (int n = words.length - 1; n >= 1; n--) {
            String prefix = String.join(" ", java.util.Arrays.copyOfRange(words, 0, n));
            if (model.roleTargetTypeToSubject(currentSubject, prefix) != null)
                return new Expr.AttributeReference(prefix, currentSubject, "zijn", model.navigationRoleName(prefix), unknown(), source);
        }
        return null;
    }

    /** §3.6 resolve {@code bareName} as a single-cell read of a gedimensioneerd attribuut of {@code ot} —
     *  the bare attribuut followed by the dimension's read preposition and a declared label
     *  ("betaalde belasting in jaar van huidig jaar" = attribuut "betaalde belasting in jaar" + jaardimensie
     *  label "huidig jaar"). Returns the {@link Expr.DimensionCellRead}, or null when {@code bareName} is not such a
     *  read. Scope (the rest stays an honest marker): a SINGLE-dimension attribute (a multi-dimension cell
     *  needs every label and a composite-key model) read via the SUFFIX voorzetsel form (the prefix
     *  "&lt;label&gt; &lt;attribuut&gt;" form is deferred). The longest matching attribute wins, so a
     *  dimensioned name is not shadowed by a shorter scalar prefix of it. */
    private Expr dimensionCell(String bareName, ObjectType ot, String navRole, String subjectSource, Source source) {
        if (ot == null || bareName == null) return null;
        String best = null; String bestLabel = null; RegelSpraakModel.Dimension bestDim = null;
        for (var e : ot.dimensionedMembers().entrySet()) {   // own AND inherited (ch. 2.2)
            String attr = e.getKey();
            if (e.getValue().size() != 1) continue;                 // multi-dimension cell deferred
            RegelSpraakModel.Dimension d = model.dimensionByName(e.getValue().get(0));
            if (d == null || d.preposition() == null) continue;      // prefix-form (no voorzetsel) deferred
            String prefix = attr + " " + d.preposition() + " ";
            if (bareName.length() <= prefix.length()
                    || !bareName.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
            String label = dimensionLabel(d, bareName.substring(prefix.length()).trim());
            if (label == null) continue;
            if (best == null || attr.length() > best.length()) { best = attr; bestLabel = label; bestDim = d; }
        }
        if (best == null) return null;
        Datatype dt = ot.attribute(best);
        Expr.AttributeReference ref = new Expr.AttributeReference(best, ot, subjectSource, navRole, dt, source);
        return new Expr.DimensionCellRead(ref, List.of(bestLabel), source);
    }

    /** The declared label of {@code d} that equals {@code name} case-insensitively (returning the declared
     *  spelling), or null. */
    private static String dimensionLabel(RegelSpraakModel.Dimension d, String name) {
        for (String w : d.values()) if (w.equalsIgnoreCase(name)) return w;
        return null;
    }

    /** A bare name: a "Daarbij geldt" variable, else a known parameter binds to a ParameterRef; anything else is
     *  unresolved, a marker whose WARNING names it. The onderwerp route ("de basisprijs") and the variabeleNaam route
     *  ("basisprijs", without an article) answer alike; the name kept as a VariableRef without a word would be a
     *  document the verifier refuses (WF-034: a variable reference resolves against no binding). */
    private Expr nameRef(String name, Source source) {
        // §11 resolve the captured "Daarbij geldt:" definition FIRST: a local variable reference works WITH or WITHOUT
        // a leading article (the spec explicitly allows a lidwoord at a variable reference — "de drempel" arrives via
        // the onderwerp path; variableKey strips the lidwoord so both forms hit one key),
        // and it SHADOWS a same-named global parameter (a variable "geldt alleen binnen de regel"). A name with no
        // definition (or a forward/cyclic reference, not yet in the map) falls through to the parameter, else the marker.
        String key = variableKey(name);
        if (binderMode) {
            // The RESULT route: a BinderRef under the Let chain, typed with the definition's own type — the type the
            // inlined subtree would have carried, so every downstream typering claim is unchanged.
            Variable v = binders.get(key);
            if (v != null) return new Expr.BinderRef(v.name(), v.value().type(), source);
        } else {
            Expr inlined = visible.get(key);
            if (inlined != null) {
                Expr read = variableInFilter(name, inlined, source);
                explicitSubjectReads.add(read);   // a variable of the rule is the rule subject's, wherever it is read
                return read;
            }
        }
        String stripped = name.replaceFirst("^(?i)(de|het|een)\\s+", "");
        Parameter p = model.parameters.get(stripped);
        if (p == null) p = model.parameters.get(name);
        if (p != null) return new Expr.ParameterRef(p, source);
        return unresolvedMarker(source, Msg.frontend("PART.unresolved-name", "name", name));
    }

    /**
     * The object type a subject denotes: first via its primary noun (resolved
     * against object-type names and feittype roles, §13.4.1), then — as a fallback
     * for navigation shapes the primary-noun extraction does not cover — the older
     * deep scan for a directly-named object type. Returns null when neither binds.
     */
    ObjectType resolveSubject(OnderwerpContext o) {
        String n = primaryName(o);
        ObjectType ot = n != null ? model.resolveObjectType(n) : null;
        if (ot == null) ot = typeViaPlural(n);
        return ot != null ? ot : findObjectType(o);
    }

    /**
     * The declared object type a written PLURAL names ("alle Hoofdbergers"), for the positions that
     * name a POPULATION rather than navigate to one: a set onderwerp and a count's element word.
     *
     * <p>{@link RegelSpraakModel#resolveObjectType} keys object types on their declared SINGULAR and
     * otherwise reads the name as a ROLE (name or declared plural). That covers a population whose
     * name happens to be a role's — "alle bergers" binds Berger through the role {@code berger
     * (mv: bergers)} — and covers nothing else. A population named by a SUBTYPE has no role of its
     * own by construction: the relation is declared once, on the ancestor the subtype inherits from
     * (ch. 2.6 makes a filler an UPPER bound), so "alle Hoofdbergers" matched no object-type key and
     * no role and resolved to nothing at all. The reach and the population are two different
     * questions (ch. 2.2: quantification is extensional at every type-ranging position), and this
     * answers the second one; the role binding answers the first and already resolves through ⊑.
     *
     * <p>Tried after the singular and the role reading, and BEFORE the older deep parse-tree scan —
     * a written plural that names a declared type is answered here rather than by that scan's
     * deepest-match guess. Every singular and every role keeps resolving exactly as it did. The
     * matcher is ambiguity-safe on EVERY pass (a shared declared {@code (mv: …)} binds neither, and
     * the un-bound name then falls loudly on WF-034 instead of silently picking a type), so this
     * never introduces a first-hit bind.
     */
    private ObjectType typeViaPlural(String name) {
        if (name == null) return null;
        for (String candidate : RegelSpraakModel.nameCandidates(name)) {
            ObjectType ot = PluralMatcher.findObjectType(model.objectTypes.values(), candidate);
            if (ot != null) return ot;
        }
        return null;
    }

    /** The object type an onderwerpketen denotes (a rule subject), via its primary noun. */
    ObjectType chainObjectType(OnderwerpKetenContext k) {
        String n = primaryNameChain(k);
        return n != null ? model.resolveObjectType(n) : null;
    }

    /**
     * The role noun for a navigation reference: the noun of a DEFINITE singular
     * reference ("de reis" — parsed either as a kwantificatie "de" or an
     * onderwerpVerwijzing) or a possessive ("zijn partner"). An indefinite "een &lt;X&gt;"
     * (universeelOnderwerp) is the rule's universal subject, and a set quantifier
     * (alle/geen/ten minste …) ranges over a set — neither is a single-object role
     * navigation, so both yield null and the reference stays an own-attribute read.
     */
    private String navigationNoun(OnderwerpContext o) {
        if (o == null) return null;
        if (o.kwantificatie() != null && o.kaleNaam() != null) // alt: "de <X>" (only the bare definite, not "alle"/"ten minste … van de")
            return o.kwantificatie().DE() != null && o.kwantificatie().getChildCount() == 1 ? src(o.kaleNaam()) : null;
        return navigationNoun(o.onderwerpKeten());
    }
    private String navigationNoun(OnderwerpKetenContext k) {
        if (k == null) return null;
        if (k.onderwerpVerwijzing() != null) return src(k.onderwerpVerwijzing().kaleNaam()); // "de reis"
        if (k.ZIJN() != null && k.kaleNaam() != null) return src(k.kaleNaam());              // "zijn partner"
        return null; // universeelOnderwerp ("een <X>" = subject) / selector-van: not a single role navigation
    }

    /** The primary (head) noun of a subject — used by the resolver's §13.4.7 feitcreatie
     *  back-reference check to see whether onderwerpD directly names the anker objecttype. */
    String subjectPrimaryName(OnderwerpContext o) { return primaryName(o); }

    /** The subject's primary noun — the entity an attribute attaches to. */
    private String primaryName(OnderwerpContext o) {
        if (o == null) return null;
        if (o.kaleNaam() != null) return src(o.kaleNaam());   // kwantificatie bareName (van …)?
        return primaryNameChain(o.onderwerpKeten());
    }
    private String primaryNameChain(OnderwerpKetenContext k) {
        if (k == null) return null;
        if (k.universeelOnderwerp() != null) return src(k.universeelOnderwerp().kaleNaam());
        if (k.onderwerpVerwijzing() != null) return src(k.onderwerpVerwijzing().kaleNaam());
        if (k.ZIJN() != null && k.kaleNaam() != null) return src(k.kaleNaam()); // "zijn reis"
        return null; // selector-van / other navigation: leave to the deep-scan fallback
    }

    /** Find the object type a subject denotes by matching any name in its subtree against the model. */
    private ObjectType findObjectType(ParseTree t) {
        if (t == null) return null;
        ObjectType found = null;
        if (t instanceof ParserRuleContext ctx) {
            ObjectType direct = model.objectTypes.get(src(ctx));
            if (direct != null) found = direct;
        }
        for (int i = 0; i < t.getChildCount(); i++) {
            ObjectType c = findObjectType(t.getChild(i));
            if (c != null) found = c; // prefer the deepest/last match (the base subject)
        }
        return found;
    }

    // ---------------- helpers ----------------
    private static Typing.BinOp toTypingOp(Expr.BinOp op) {
        return switch (op) {
            case PLUS -> Typing.BinOp.PLUS;
            case MIN -> Typing.BinOp.MIN;
            case VERMINDERD_MET -> Typing.BinOp.VERMINDERD_MET;
            case MAAL -> Typing.BinOp.MAAL;
            case GEDEELD_DOOR, GEDEELD_DOOR_ABS -> Typing.BinOp.GEDEELD_DOOR;
            case TOT_DE_MACHT -> Typing.BinOp.TOT_DE_MACHT;
            case PERCENTAGE_VAN -> Typing.BinOp.MAAL; // not reached: handled inline
        };
    }

    // ---------------- §13.2.14 enumeratiewaarde: the domain comes from the POSITION ----------------

    /** The reason an enumeratiewaarde marker carries until a position types it. One constant, because
     *  {@link #enumAgainstContext} recognises the marker by it. */
    static final Msg REASON_ENUM_WITHOUT_DOMAIN = Msg.frontend("RGS-092.enum-without-domain");

    /** §13.2.14 an enumeratiewaarde, built as a MARKER that the positions below promote to a typed
     *  {@link Expr.Literal}. Marker-first is the fail-fast half of uchar's V8 decision (2026-09-19): the
     *  literal names a LABEL and not a domain, and which domain applies is a property of where it stands,
     *  so a builder that types it here would have to guess one. What the wire admits is only the promoted
     *  form — WF-039 crosses the BARE label byte-exactly against the declared value list and WF-034 (r)
     *  refuses the unknown sentinel — so an unpromoted marker is the honest form of "no domain derivable",
     *  and {@code Resolver}'s marker discipline raises a WARNING over every one that survives.
     *
     *  <p>{@code lexemeText} is the ENUM_LITERAL TOKEN text, and the bron snippet is that token rather than
     *  {@link DefinitionResolver#src}: an ENUM_LITERAL is one lexer token, so the token IS the whole source
     *  slice, while src() folds every whitespace run to a single space. The DECLARATION keeps the token text
     *  ({@link DefinitionResolver#enumeration}), so a folded label would not equal a declared value carrying a
     *  whitespace run — before this, such a value could not be written at all: the membership check read the
     *  folded snippet and refused a correct document. The bron is also where {@link #enumAgainstContext} reads
     *  the label back from, which is why the marker must carry the unfolded token. */
    private Expr enumValue(ParserRuleContext ctx, String lexemeText) {
        return unresolvedMarker(Source.of(ctx, lexemeText), REASON_ENUM_WITHOUT_DOMAIN);
    }

    /** Promote an enumeratiewaarde marker to the domain {@code context} names, or hand back {@code candidate}
     *  untouched. The two contexts that name one are the two the §13.3.4.2 membership check already reads: a
     *  WRITE TARGET's declared datatype (a rule's doel, an objectcreatie member) and the OTHER side of a
     *  comparison. The label is the source lexeme minus its delimiters, via the shared reader — total and
     *  lossless, since the grammar makes the delimiters the token's first and last character.
     *
     *  <p>The domain is taken from the context even when the label is NOT one of its values: that case is an
     *  ERROR the membership check raises, and an unresolvable model writes no IR at all, so the ill-formed
     *  wire is never reached. Typing it anyway keeps the label visible to that check, which is what reports
     *  the author's actual mistake.
     *
     *  <p>It descends through the ch. 4.10 Let chain a variabelendeel builds, because such a chain DENOTES its
     *  value (ch. 10.6): {@code Daarbij geldt: de tint is 'gaaf'} writes exactly what the bare literal writes
     *  and must reach the wire in the same shape. Rebuilding the chain is not cosmetic — the Let and its
     *  BinderRef are typed FROM the value, so an unpromoted chain carries the unknown sentinel at three
     *  positions WF-034 (r) refuses instead of one. The descent follows the shape {@link RuleChecker#peeled}
     *  follows, and stops where that one still resolves: a body that reads an OUTER binder past an inner Let
     *  stays a marker, loud and honest, rather than being half-promoted. */
    static Expr enumAgainstContext(Expr candidate, Datatype context) {
        if (!(context instanceof Datatype.Enumeration)) return candidate;
        if (candidate instanceof Expr.Unresolved o && REASON_ENUM_WITHOUT_DOMAIN.equals(o.reason()))
            return new Expr.Literal(Literals.enumLabel(o.source().snippet()), context, o.source());
        if (candidate instanceof Expr.Let l) {
            if (l.body() instanceof Expr.BinderRef b && b.name().equals(l.binder())) {
                Expr value = enumAgainstContext(l.value(), context);
                if (value == l.value()) return candidate;
                return new Expr.Let(l.binder(), value,
                        new Expr.BinderRef(b.name(), context, b.source()), context, l.source());
            }
            Expr body = enumAgainstContext(l.body(), context);
            if (body == l.body()) return candidate;
            return new Expr.Let(l.binder(), l.value(), body, body.type(), l.source());
        }
        return candidate;
    }

    /** The label of a value that IS an enumeratiewaarde — the bare label of a promoted literal, or the lexeme
     *  of a marker no position promoted. Both arms carry: the §13.3.4.2 membership check must report an
     *  undeclared label wherever it stands, also in a shape {@link #enumAgainstContext} leaves alone. null for
     *  anything else, which that check reads as "no claim". */
    static String valueEnumLabel(Expr e) {
        if (e instanceof Expr.Literal l && l.type() instanceof Datatype.Enumeration) return l.valueText();
        if (e instanceof Expr.Unresolved o && REASON_ENUM_WITHOUT_DOMAIN.equals(o.reason()))
            return Literals.enumLabel(o.source().snippet());
        return null;
    }

    /** A comparison, with each operand offered the other's datatype as its enumeratiewaarde context. Every
     *  {@link Condition.Comparison} the frontend builds goes through here, so the rule holds of comparisons
     *  as such rather than of the handful of shapes a card happened to exercise.
     *
     *  <p>One caller reaches it only for operands that are already narrowed: {@link #elementComparison}
     *  (the §5.6 die/dat subselectie on an aggregation operand) refuses any right-hand side that is neither
     *  a Literal nor a ParameterRef BEFORE building the comparison, so an enumeratiewaarde marker never
     *  arrives here and the whole predicaat is refused instead — an Unresolved conditie or a reason-carrying
     *  deelselectie, never a silently untyped comparison. That is the pre-existing narrowing of that
     *  position, not a hole this rule opened: the position folds an AGGREGATED attribute, and no aggregation
     *  ranges over an Enumeratie, so a well-typed enum comparison cannot stand there in the first place. */
    static Condition.Comparison comparison(Condition.Operator op, Expr left, Expr right,
                                              Condition.WholePeriod period, Source source) {
        Expr l = enumAgainstContext(left, right == null ? null : right.type());
        Expr r = enumAgainstContext(right, left == null ? null : left.type());
        return new Condition.Comparison(op, l, r, period, source);
    }

    static Condition.Comparison comparison(Condition.Operator op, Expr left, Expr right, Source source) {
        return comparison(op, left, right, null, source);
    }

    private static String src(ParserRuleContext ctx) { return DefinitionResolver.src(ctx); }
    private static dev.uchar.regelspraak.ir.Source source(ParserRuleContext ctx) { return DefinitionResolver.source(ctx); }
    private static Datatype unknown() { return new Datatype.Unknown("niet opgelost"); }
    /** An unresolved expression marker carrying its {@code reason} — the Dutch sentence the frontend's
     *  survivor report raises over it when it reaches the finished model. {@code null} where the calling
     *  route already raises its own diagnostic (see {@link Expr.Unresolved#reason()}). */
    private Expr unresolvedMarker(ParserRuleContext ctx, Object reason) { return new Expr.Unresolved(unknown(), source(ctx), reason); }
    private Expr unresolvedMarker(dev.uchar.regelspraak.ir.Source source, Object reason) { return new Expr.Unresolved(unknown(), source, reason); }
}
