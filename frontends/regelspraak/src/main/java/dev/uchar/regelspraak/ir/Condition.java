package dev.uchar.regelspraak.ir;

import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;

import java.util.List;

/**
 * A resolved condition — the §13.4.13/.14/.15 voorwaardendeel as IR, i.e. the
 * "when" side of the rule the Drools transpiler will emit (the {@link Expr} value
 * being the "then" side).
 *
 * Operands are resolved {@link Expr} trees, so a comparison or predicate already
 * carries the bound references and datatypes of its operands. The operator and
 * predicate enums are defined here, in the IR, so the IR depends only on the
 * model — the resolver maps the parse tokens onto these and runs the typeringen
 * checks while it builds them.
 *
 * The forms this doc once named as unstructured have since landed as nodes of their own:
 * kenmerk membership as {@link CharacteristicCheck}, objecttype/role membership as
 * {@link RoleMembership} and {@link FarRoleMembership}, period comparisons as
 * {@link TimeWindow}. What the resolver still cannot structure becomes an {@link Unresolved}
 * node carrying its source text, keeping the IR explicit about its coverage.
 */
public sealed interface Condition {
    /** The structured source anchor (file/line/column + whitespace-normalised snippet) this
     *  condition was built from; {@link Source#toString()} is the snippet (wire-preserving). */
    Source source();

    /** §8.2 / §13.4.15.70 "gedurende de/het gehele &lt;periode&gt;" whole-period qualifier on a predicate over a
     *  tijdsafhankelijk onderwerp (§8.2). The qualified predicate yields "waar" only if the underlying
     *  check holds at EVERY moment of each named calendar period the subject's timeline touches (§8.2).
     *  {@code year} = "gedurende het gehele jaar" (false = "de gehele maand"); {@code negated} = the §8.2
     *  "niet gedurende …" form, which complements the whole-period result. Attached to the temporal predicate it
     *  qualifies — a {@link Comparison}, a {@link Predicate}, or a {@link CharacteristicCheck}; null on an ordinary predicate. */
    record WholePeriod(boolean year, boolean negated) {}

    /** §5.1-5.10 comparison operators (the typeringen ordering/equality/date families). */
    enum Operator {
        KLEINER, KLEINER_OF_GELIJK, GROTER, GROTER_OF_GELIJK,
        GELIJK, ONGELIJK,
        EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK
    }
    record Comparison(Operator op, Expr left, Expr right, WholePeriod period, Source source) implements Condition {
        public Comparison(Operator op, Expr left, Expr right, Source source) { this(op, left, right, null, source); }
    }

    /** §5.11-5.16 one-sided predicates. {@code dayKind} names the §3.12 dagsoort for a
     *  {@link PredicateKind#DAGSOORT} control ("&lt;datum&gt; is een kerstdag", §8.1.5) — the date is
     *  tested against that dagsoort's §9.8 definitie predicate; it is null for every other soort.
     *  {@code negation} marks the §8.1 Tabel 16 NEGATED form of the ELFPROEF / NUMERIEK_MET_EXACT predicates
     *  ("voldoet NIET aan de elfproef", "is NIET numeriek met exact N cijfers"); false for the others.
     *  {@code digits} is the §8.1.4 digit-count N of the NUMERIEK_MET_EXACT predicate ("numeriek met exact N
     *  cijfers" — the §13.4.15.84 &lt;geheelgetal&gt;); 0 for every other soort. */
    enum PredicateKind { LEEG, GEVULD, ELFPROEF, NUMERIEK_MET_EXACT, DAGSOORT }
    record Predicate(PredicateKind kind, Expr operand, String dayKind, boolean negation, int digits, WholePeriod period, Source source) implements Condition {
        public Predicate(PredicateKind kind, Expr operand, String dayKind, WholePeriod period, Source source) { this(kind, operand, dayKind, false, 0, period, source); }
        public Predicate(PredicateKind kind, Expr operand, boolean negation, WholePeriod period, Source source) { this(kind, operand, null, negation, 0, period, source); }
        public Predicate(PredicateKind kind, Expr operand, boolean negation, int digits, WholePeriod period, Source source) { this(kind, operand, null, negation, digits, period, source); }
        public Predicate(PredicateKind kind, Expr operand, WholePeriod period, Source source) { this(kind, operand, null, false, 0, period, source); }
        public Predicate(PredicateKind kind, Expr operand, Source source) { this(kind, operand, null, false, 0, null, source); }
    }

    /** §8.1.8 kenmerk-membership test ("de Meting is oud" / "hij heeft het recht …"): true when the
     *  named object carries the boolean kenmerk. {@code navigation} is null for the subject's OWN kenmerk
     *  (lowered to the subject's boolean property, {@code oud == true}); for a TO-ONE navigated kenmerk
     *  ("de partner van X is gepensioneerd") it is the navigation role, lowered to the property path
     *  {@code partner.gepensioneerd == true}. A to-many navigation in a single-pattern rule stays
     *  {@link Unresolved}. {@code owner}, when non-null, is the object type whose pattern this BARE
     *  constraint must attach to — a §13.4.7 feitcreatie hosts a doel-kenmerk on the {@code $doel} pattern,
     *  so the transpiler emits it only on that type and markers it elsewhere (never on the anker, even when
     *  the anker declares a same-named kenmerk). null leaves it unconstrained (the single-subject rule). */
    record CharacteristicCheck(String characteristic, String navigation, boolean negation, ObjectType owner, WholePeriod period, Source source) implements Condition {
        public CharacteristicCheck(String characteristic, String navigation, boolean negation, ObjectType owner, Source source) { this(characteristic, navigation, negation, owner, null, source); }
        public CharacteristicCheck(String characteristic, String navigation, boolean negation, Source source) { this(characteristic, navigation, negation, null, null, source); }
        public CharacteristicCheck(String characteristic, Source source) { this(characteristic, null, false, null, null, source); }
    }

    /** §8.1.7 rolcheck ("hij is een deelnemer"): true when the subject FILLS the role {@code role}.
     *  In the §9.4 reference-field fact model the subject fills a role iff its reference to the OTHER
     *  end of that feittype is set, so this lowers to a bare null-test on that reference field
     *  ({@code groep != null}); {@code otherToFiller} is that other end's object type, from which the
     *  transpiler looks up the reference field. {@code negation} marks the §8.1.7/§8.1.8.3 "is geen &lt;rol&gt;"
     *  negation — the subject does NOT fill the role — which lowers to the inverse null-test ({@code groep == null}).
     *  When the reference lives on the other side (the §8.1.7 "heeft" form) it is a {@link FarRoleMembership}. */
    record RoleMembership(String role, ObjectType otherToFiller, Source source, boolean negation) implements Condition {
        /** Positive rolcheck — the subject fills the role. */
        RoleMembership(String role, ObjectType otherToFiller, Source source) { this(role, otherToFiller, source, false); }
    }

    /** §8.1.7 FAR-side rolcheck ("de groep heeft een deelnemer"): the subject sits on the "één" side, so the
     *  reference lives on the OTHER object — a to-many {@code filler} whose {@code backref} field points back to
     *  the subject. True when at least one such filler exists, so it lowers to an EXISTS-join (a separate
     *  when-clause {@code exists <filler>( <backref> == $subject )}), NOT an in-pattern constraint — which is why
     *  it can only be peeled when it is a rule's TOP-LEVEL condition; nested in an AND/OR or a comparison it
     *  stays a marker. {@code negation} is the "heeft geen" form ({@code not <filler>(...)}). */
    record FarRoleMembership(ObjectType filler, String backref, boolean negation, Source source) implements Condition {}

    /** §8.4.3 / §13.4.15.68-.69 periodevergelijking as a CONDITION: the rule applies during the period
     *  bounded by the datum-expressies — "vanaf X" / "van X tot [en met] Y" / "tot [en met] X". Standalone it is
     *  the §13.4.13.1 voorwaardendeel alternative ("… van dd. 1-1-2024 tot dd. 8-2-2025"); inside a compound it
     *  is the §13.4.15.69 "het is de periode …" bullet. §5.1.3 bound semantics: {@code from} is INCLUSIVE,
     *  {@code until} EXCLUSIVE unless {@code toInclusive} (the "tot en met" form). Either bound may be null
     *  (the one-sided vanaf/tot forms); never both. The engine evaluates scalar rules as of the §4.2 Rekendatum,
     *  so the transpilers lower this to "rekendatum inside the period" (see grammar/DIVERGENCES.md). */
    record TimeWindow(Expr from, Expr until, boolean toInclusive, Source source) implements Condition {}

    /** Whether {@code c} is or contains a {@link TimeWindow} — the §4.2 Rekendatum-global trigger, and the
     *  transpilers' symmetric marker/skip gate for the contexts that cannot host one (a counting quantifier's
     *  fold, a tijdsafhankelijk doel whose timeline the period should CLIP rather than gate). */
    static boolean containsTimeWindow(Condition c) {
        if (c instanceof TimeWindow) return true;
        if (c instanceof Compound s) return s.components().stream().anyMatch(Condition::containsTimeWindow);
        if (c instanceof QuantifierFold k) return containsTimeWindow(k.filter()) || containsTimeWindow(k.test());
        return false;
    }

    /** §8.1.9 collect the rule names referenced by a "regelversie &lt;X&gt; is gevuurd" predicate inside
     *  {@code c} into {@code result} — the gevuurd (NOT inconsistent) arm of {@link RuleStatusCheck}, recursing
     *  through compound children. The transpilers walk every construct's voorwaarde with this FIRST, so
     *  only a rule whose fired-status is actually referenced emits the {@code RuleFired} marker (§8.1.9
     *  opt-in emission — no fact-base bloat from markers no rule reads). Null-safe. */
    static void collectFiredReferences(Condition c, java.util.Set<String> result) {
        if (c == null) return;
        if (c instanceof RuleStatusCheck rv && !rv.inconsistent() && rv.rule() != null) result.add(rv.rule());
        if (c instanceof Compound s) s.components().forEach(d -> collectFiredReferences(d, result));
        if (c instanceof QuantifierFold k) {
            collectFiredReferences(k.filter(), result);
            collectFiredReferences(k.test(), result);
        }
    }

    /** Whether any operand {@link Expr} inside {@code c} matches {@code test} — comparison sides, the
     *  one-sided predicate operand, periode bounds, recursing through compound children. Null-safe. */
    static boolean containsExpr(Condition c, java.util.function.Predicate<Expr> test) {
        if (c == null) return false;
        if (c instanceof Comparison v)  return Expr.contains(v.left(), test) || Expr.contains(v.right(), test);
        if (c instanceof Predicate p)     return Expr.contains(p.operand(), test);
        if (c instanceof TimeWindow p)       return Expr.contains(p.from(), test) || Expr.contains(p.until(), test);
        if (c instanceof Compound s)  return s.components().stream().anyMatch(d -> containsExpr(d, test));
        if (c instanceof QuantifierFold k)   return containsExpr(k.filter(), test) || containsExpr(k.test(), test);
        return false;
    }

    /** Whether any operand {@link Expr} inside {@code c} matches {@code test} — the PUBLIC form of
     *  {@link #containsExpr}, for cross-package IR consumers (the scenariotoetsen resolver's parametergate /
     *  peildatum-gate need to see whether a scope rule's condition references a parameter or the Rekendatum).
     *  A pure read; identical semantics to the package-private walker. Null-safe. */
    public static boolean containsOperand(Condition c, java.util.function.Predicate<Expr> test) {
        return containsExpr(c, test);
    }

    /** Every node of {@code c} that is no 'all', each once, handed to {@code doe}: an 'all' is entered part by
     *  part, and a node already in {@code seen} is passed over with everything it holds. The sweeps over a
     *  finished defaults tree walk each justification with it, because a restrictive exception repeats its
     *  carrier's conjuncts as the very same nodes (grammar/DIVERGENCES.md §4, form 2): each shared conjunct is
     *  judged once, not once per exception above it. An 'all' itself neither complements nor counts a verdict
     *  and holds nothing but its parts, so a sweep misses nothing by not being handed it. Null-safe. */
    static void eachConjunctOnce(Condition c, java.util.Set<Condition> seen,
                                   java.util.function.Consumer<Condition> doe) {
        if (c == null || !seen.add(c)) return;
        if (c instanceof Compound s && s.quantifier() == Quantifier.ALLE) {
            for (Condition d : s.components()) eachConjunctOnce(d, seen, doe);
            return;
        }
        doe.accept(c);
    }

    /** §13.4.14 compound condition: a quantifier over a bullet list of sub-conditions.
     *  `threshold` is the threshold count N for the counting quantifiers (ten minste / ten
     *  hoogste / precies N, §13.4.14.4); 0 and unused for alle / geen / de. */
    enum Quantifier { DE, ALLE, GEEN, TEN_MINSTE, TEN_HOOGSTE, PRECIES }
    record Compound(Quantifier quantifier, int threshold, List<Condition> components, Source source) implements Condition {}

    /** §13.4.2/§8.2 QUANTIFIER FOLD — first-order quantification of a condition over the members of a
     *  linked instance set ("… indien de &lt;attribuut&gt; van ALLE &lt;leden&gt; van de &lt;onderwerp&gt; gedurende het
     *  gehele jaar groter is dan &lt;grens&gt;"). The first-order sibling of {@link Compound}, which
     *  quantifies over PARTS of one subject; this one quantifies over INSTANCES and takes its range
     *  fields from the instance fold ({@link Expr.FunctionCall}) unchanged.
     *
     *  &lt;p&gt;{@code quantifier} is the WRITTEN quantor — recording it is the point of the node:
     *  {@code AttributeReference.set} is true for "alle", "geen van de" and every counting quantor alike, so a
     *  reading that took the set flag for a forall would answer ALLE for a sentence that wrote "geen van
     *  de". {@code threshold} is the counting quantifier's threshold N (ten minste / ten hoogste / precies
     *  N) and 0 under ALLE / GEEN, where the wire omits the field altogether (WF-108 (e)).
     *  {@code filter} is the die/dat subselection (which members are quantified
     *  over) and {@code test} the quantified matrix (what each selected member must satisfy); both are
     *  evaluated PER CONTRIBUTOR with the element as subject, exactly as {@link Expr.FunctionCall#filter} is.
     *  {@code sourceRole} / {@code sourcePath} are the range, spelled exactly as the fold's and mutually
     *  exclusive (IR-SPEC ch. 4.5, WF-108; RUNTIME-SEMANTICS ch. 3.9a). */
    record QuantifierFold(Quantifier quantifier, int threshold, ObjectType elementObjectType, Condition filter,
                       Condition test,
                       dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef sourceRole,
                       List<dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef> sourcePath,
                       Source source) implements Condition {
        public QuantifierFold(Quantifier quantifier, int threshold, ObjectType elementObjectType, Condition filter,
                           Condition test,
                           dev.uchar.regelspraak.model.RegelSpraakModel.RoleBindingRef sourceRole, Source source) {
            this(quantifier, threshold, elementObjectType, filter, test, sourceRole, List.of(), source);
        }
    }

    /** §8.1.9 / §13.4.15.66 regelpredicaat: "regelversie &lt;regel&gt; [(&lt;geldigheid&gt;)] is gevuurd |
     *  is inconsistent" — a control on the EXECUTION of the named rule, not on a subject. {@code rule} is
     *  the referenced rule's name (the parenthesised versie-aanduiding is parsed but kept only in
     *  {@code source}: the §9.5 Inconsistentie channel is already geldigheids-window-gated at insert time).
     *  {@code inconsistent} distinguishes "is inconsistent" (reads the §9.5 Inconsistentie flag facts of a
     *  consistentie/uniciteit/verdeling rule — an exists-join) from "is gevuurd" (whether the named rule's
     *  voorwaardendeel was satisfied — no observable runtime record, so both back-ends decline it
     *  symmetrically; see grammar/DIVERGENCES.md). */
    record RuleStatusCheck(String rule, boolean inconsistent, Source source) implements Condition {}

    /** A condition form the resolver cannot structure — a marker with a reason, never a type.
     *  Membership and period tests have left this node for {@link CharacteristicCheck},
     *  {@link RoleMembership}, {@link FarRoleMembership} and {@link TimeWindow}.
     *
     *  <p>{@code reason} is that reason in so many words — the sentence the frontend raises as a
     *  WARNING over every marker that SURVIVES into the finished model (maintainer decision 2026-09-19:
     *  a marker keeps the document emittable, but the author is told what stayed unresolved and why).
     *  It travels ON the node rather than being reported where the node is built, because a marker is
     *  not final there: several routes re-read a markered condition and replace it. It is NEVER
     *  serialised — the wire writes {@code node} plus {@code source} — so carrying it moves no byte.
     *
     *  <p>null means the producing route already raises its OWN diagnostic for this marker; the sweep
     *  then stays quiet rather than saying it twice.
     *
     *  <p>The reason is a {@code Msg} of the frontend catalogue, so that the warning renders in either language;
     *  it is typed {@code Object} because a test builds a marker with a plain string. */
    record Unresolved(Source source, Object reason) implements Condition {}

    /** Every {@link Unresolved} inside {@code c}, in structural order, handed to {@code result} as its
     *  {@code (bron, reden)} pair — the traversal the frontend's survivor report runs over the finished
     *  model. Recurses through compound children, the fold's filter/matrix, and every operand
     *  {@link Expr} (an aggregation's own filter is reached back through {@link Expr}). Null-safe.
     *
     *  <p>The switch is EXHAUSTIVE over the sealed union — no {@code default} arm — for the reason
     *  {@link Expr#contains} spells out: a missing arm under-reports in silence, which is precisely the
     *  failure this traversal exists to end. */
    static void collectUnresolved(Condition c, java.util.function.BiConsumer<Source, Object> result) {
        if (c == null) return;
        switch (c) {
            case Unresolved o -> result.accept(o.source(), o.reason());
            case Comparison v -> { Expr.collectUnresolved(v.left(), result); Expr.collectUnresolved(v.right(), result); }
            case Predicate p -> Expr.collectUnresolved(p.operand(), result);
            case TimeWindow p -> { Expr.collectUnresolved(p.from(), result); Expr.collectUnresolved(p.until(), result); }
            case Compound s -> s.components().forEach(d -> collectUnresolved(d, result));
            case QuantifierFold k -> { collectUnresolved(k.filter(), result); collectUnresolved(k.test(), result); }
            case CharacteristicCheck ignored -> { }
            case RoleMembership ignored -> { }
            case FarRoleMembership ignored -> { }
            case RuleStatusCheck ignored -> { }
        }
    }
}
