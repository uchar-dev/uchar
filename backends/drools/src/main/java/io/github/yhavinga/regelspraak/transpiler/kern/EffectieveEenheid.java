package io.github.yhavinga.regelspraak.transpiler.kern;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Eenheid;

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
 * <p><b>Why.</b> A v0 IR node repeats its type on the wire at positions where that type is a COPY of a
 * declaration (an {@code AttribuutRef}, a {@code ParameterRef}, a {@code DimensieCel} read, a binder read)
 * or a DERIVATION over its operands (an arithmetic node, a Let, a value aggregation). WF-091 makes the
 * copy mechanically true for the reference positions; nothing crosses the derived ones. A consumer that
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
 *       source. A position, not a node kind: {@code GEDEELD_DOOR} and {@code PLUS} are both {@code Binair}
 *       and only one of them derives, which is why the {@code switch}es below dispatch on the OP and the
 *       KIND, and why the harness's clamp keys on (node, op) / (node, kind) too. The ASP translator answers
 *       {@code None} there and fences; this backend has no such fence and computes with the annotation,
 *       exactly as it did before this class. Measured 2026-09-04: no corpus annotation at such a position
 *       carries a unit, and {@code check-reftype-derivation.sh} fails on the first one that does.</li>
 * </ul>
 *
 * <p><b>Scope.</b> This derives the UNIT only. The soort and the precision of an annotation are equally
 * repetitions, but this backend reads neither for its §4 restatements: it reads {@code isNumeriek}/
 * {@code isDatum} as ROUTING predicates (a soort lie is a different document, crossed by WF-078/WF-091)
 * and it never reads {@code Datatype.decimalen} at all — only {@code Afgerond.decimalen}, a node field.
 * Grafting the derived unit onto the node's own annotation therefore changes exactly what is read and
 * nothing else.
 */
public final class EffectieveEenheid {

    /** Depth ceiling for the derivation walk — a guard against a pathological tree, never a semantic bound
     *  (the deepest corpus value tree is an order of magnitude shallower). At the ceiling the node's own
     *  annotation stands, which is the pre-derivation behaviour. */
    private static final int MAX_DIEPTE = 200;

    private static final Map<String, Eenheid> LEEG = Map.of();

    /** ch. 4.10: every {@link Expr.Let} the document carries ({@code IrLoader.Loaded.lets}). Only
     *  {@link #bouwLezingen} needs it: the derivation itself meets a Let as an ordinary node in the tree it is
     *  walking, but the binder-read table must START at bindings, and a {@link Expr.BinderRef} asked in isolation
     *  carries no route back up to its own. Empty for a document without a Let. */
    private final Collection<Expr> lets;

    /** The derived unit of every binder READ, by identity — precomputed, because a restatement site deep inside a
     *  Let body asks for a binder's unit without the binding in hand. Built by walking each {@link Expr.Let} with a
     *  proper scope, so shadowing resolves the way WF-064 says (nearest enclosing binder wins). Null until first
     *  use. A recorded NULL value means "bound to a unitless value" — membership, not the value, is what separates
     *  a bound read from an unbound one. */
    private Map<Object, Eenheid> lezingEenheden;

    public EffectieveEenheid(Collection<? extends Expr> lets) {
        this.lets = lets == null ? List.of() : List.copyOf(lets);
    }

    /** The datatype every §4 restatement site must use in place of {@code e.type()}: the node's own annotation
     *  with its unit replaced by the DERIVED one. Non-numeric types (and a node whose derivation lands on the
     *  annotation anyway) pass through unchanged, so a verifier-clean document lowers byte-for-byte as before. */
    public Datatype van(Expr e) {
        Datatype t = e == null ? null : e.type();
        if (!(t instanceof Datatype.Numeriek) && !(t instanceof Datatype.Percentage)) return t;
        Eenheid afgeleid = eenheid(e, LEEG, 0);
        if (Objects.equals(afgeleid, t.eenheidOf())) return t;
        if (t instanceof Datatype.Numeriek n)
            return new Datatype.Numeriek(n.geheel(), n.decimalen(), n.teken(), afgeleid);
        Datatype.Percentage p = (Datatype.Percentage) t;
        return new Datatype.Percentage(p.geheel(), p.decimalen(), afgeleid);
    }

    /** The derived unit itself (null = unitless), for a caller that compares units rather than datatypes. */
    public Eenheid eenheidVan(Expr e) {
        Datatype t = van(e);
        return t == null ? null : t.eenheidOf();
    }

    // --- the derivation -------------------------------------------------------------------------

    private Eenheid eenheid(Expr e, Map<String, Eenheid> omgeving, int diepte) {
        if (e == null) return null;
        Datatype t = e.type();
        Eenheid annotatie = t == null ? null : t.eenheidOf();
        if (diepte > MAX_DIEPTE) return annotatie;

        if (e instanceof Expr.Let || e instanceof Expr.BinderRef || e instanceof Expr.IfThenElse)
            return letEenheid(e, omgeving, diepte, annotatie);

        if (e instanceof Expr.AttribuutRef a) {
            // the declaration is the source; there is none to consult when the resolver left the objecttype
            // unbound or the type declares no such member — the annotation then stands, and says so.
            if (a.objecttype() == null) return annotatie;
            Datatype decl = a.objecttype().attribuut(a.attribuut());
            return decl == null ? annotatie : decl.eenheidOf();
        }
        if (e instanceof Expr.DimensieCel c) return eenheid(c.attribuut(), omgeving, diepte + 1);
        if (e instanceof Expr.ParameterRef p) {
            Datatype decl = p.parameter() == null ? null : p.parameter().datatype();
            return decl == null ? annotatie : decl.eenheidOf();
        }
        if (e instanceof Expr.Afgerond af) return eenheid(af.basis(), omgeving, diepte + 1);
        if (e instanceof Expr.Begrensd bg) return eenheid(bg.basis(), omgeving, diepte + 1);
        if (e instanceof Expr.Binair b) {
            // §4 / ch. 2.6: an additive result carries the left unit and a unitless side adopts the other.
            // Every other operator (division, power, percentage-of) has no v0 join rule here — annotation.
            return switch (b.op()) {
                case PLUS, MIN, VERMINDERD_MET -> {
                    Eenheid l = eenheid(b.links(), omgeving, diepte + 1);
                    yield l != null ? l : eenheid(b.rechts(), omgeving, diepte + 1);
                }
                case MAAL -> maalEenheid(eenheid(b.links(), omgeving, diepte + 1),
                                         eenheid(b.rechts(), omgeving, diepte + 1));
                default -> annotatie;
            };
        }
        if (e instanceof Expr.Functie f) {
            // §4.6 Eenheid1: a value aggregation carries its FIRST operand's unit (the rule this backend's own
            // concatenatieFold already states). TIJDSDUUR/ABSOLUTE_TIJDSDUUR are the documented exception above.
            return switch (f.kind()) {
                case SOM, TOTAAL, MINIMUM, MAXIMUM, EERSTE, LAATSTE -> {
                    List<Expr> args = f.argumenten();
                    if (args == null || args.isEmpty()) yield annotatie;
                    Eenheid u = eenheid(args.get(0), omgeving, diepte + 1);
                    yield vouwtDeTijdlijn(f.kind()) ? naTijdvouwing(args.get(0), u) : u;
                }
                default -> annotatie;
            };
        }
        return annotatie;
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
     * corpus. {@code ExecModelTranspiler}'s instance-fold guard skips a temporal operand for every other kind;
     * {@code ref}'s {@code eval.ml} faults on it in as many words ("only som and totaal carry the per-element
     * timeline fold"). A MINIMUM or an EERSTE over a timeline has no defined moment to rank, so neither consumer
     * folds it, no denominator is consumed, and none may be dropped.
     *
     * <p>Restricting this to TOTAAL alone — which it was until 2026-09-09 — left SOM answering the rate for a
     * value that had already been folded. Measured on a copy of {@code jutterswet-tempovouw} with the fold's kind
     * set to SOM: into a plain € doel this backend faulted where ref computes, and into a €/jr doel it silently
     * produced 576 for 48, the mnd→jr factor applied to an already-folded value, where ref faults with
     * "incompatible units: EUR/jr vs EUR". Both are now right. The corpus itself carries no SOM over a
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
    private static boolean vouwtDeTijdlijn(Expr.FunctieKind kind) {
        return kind == Expr.FunctieKind.TOTAAL || kind == Expr.FunctieKind.SOM;
    }

    private static Eenheid naTijdvouwing(Expr operand, Eenheid u) {
        if (u == null || u.noemer() == null) return u;
        if (!(operand instanceof Expr.AttribuutRef a) || a.objecttype() == null) return u;
        return a.objecttype().attribuutGranulariteit.containsKey(a.attribuut()) ? u.teller() : u;
    }

    /**
     * §4.4 / ch. 2.6, the MAAL join — the Java twin of the frontend's {@code Typering.samengesteldeEenheid}, which
     * is where this rule is authored. One unit-bearing operand hands its unit through (the factor-free product).
     * TWO of them do NOT: a rate times its (convertible) denominator unit CANCELS to the numerator ("€/jr maal jr
     * = €"), and anything else is the OPAQUE product ("€ maal kg = €·kg") — a unit with no canonical, convertible
     * only to an identical product.
     *
     * <p>That opacity is load-bearing here, not cosmetic: it is what stands between a two-unit product and a
     * restatement by a live factor. Handing back the left operand's unit would scale €·kg into a € target.
     * But what an opaque product buys downstream is NOT uniformly an honest marker, and the two shapes must
     * not be conflated (measured 2026-09-04 on {@code Eenheid}: for m·m against m, {@code convertibleNaar} is
     * false, {@code vereistOmrekening} is false, {@code omrekenfactorNaar} FAULTS rather than answering 1 —
     * the fail-fast card of the same day, and the reason the no-op below is each site's OWN identity, taken
     * before it ever asks, and never an invented factor — and {@code exacteHerschaalFactoren} is null):
     * <ul>
     *   <li>a site that REFUSES on {@code convertibleNaar} — the §4.1/§5.1.4 TEMPORAL restatements
     *       ({@code DrlTranspiler.schaalbaarNaarDoel} / {@code schaalTijd}, and the fold- and
     *       timeline-restatement guards in {@code ExecModelTranspiler}) — gets a false and turns the whole
     *       construct into an honest marker/skip. Symmetric in both back-ends;</li>
     *   <li>a site that merely COMPUTES a factor — the scalar §4 write boundary, the additive operand
     *       boundary, the §5.1 Tabel 32 comparison restatement — gets the IDENTITY and the restatement becomes
     *       a NO-OP. {@code ExecModelTranspiler.restatement} returns {1, 1} and {@code herschaalWaarde} falls
     *       through {@code valueFactor}'s null to the unchanged value; {@code DrlTranspiler.herschaalExact}
     *       and {@code herschaalConditie} likewise return the expression unchanged, one
     *       line ABOVE their "an OPAQUE product unit stays an honest marker" branch (which an opaque product
     *       never reaches, having already failed the convertibility guard). No marker on either side.</li>
     * </ul>
     *
     * <p>So on a scalar restatement the value simply stands UNRESTATED, in whatever unit it was computed in.
     * That is conservative in the sense that matters — no lie-driven magnitude, because no factor was invented
     * — but it is a SILENT ref/Drools divergence, not a loud one, and this class must not claim otherwise.
     * Its blast radius is bounded by who else accepts the form: the frontend's
     * {@code Typering.samengesteldeEenheid} flags the unit mismatch, {@code ref} faults, and the ASP
     * translator refuses ahead of translation, so no corpus golden carries it and the divergence is
     * unobservable in every gate. {@code CaseRunner} builds BOTH transpiler branches since the DRL-measurement
     * card (2026-09-04), so the form has two legs to reach rather than one — the split above is stated for each
     * — while the gate-12 replay path passes no flag and stays exec-model-only; the {@code --drl} flag is what
     * selects the DRL-text leg (the harness/measurement entry).
     *
     * <p>The unspellable case (a rate factor, "€·m/jr") reaches a consumer only on a document the frontend
     * never wrote — {@code Typering} makes it a loud error rather than a type. This join answers it with a
     * DENOMINATOR-LESS opaque product ({@code Eenheid.product} keeps the numerator factors and drops the
     * noemer, so "€/jr maal kg" and "€/jr maal kg/jr" both yield €·kg): again the conservative,
     * non-restatable outcome rather than an invented factor. RUNTIME-SEMANTICS ch. 2.10 records it as the
     * fourth MAAL outcome and names the three consumers that refuse it.
     */
    private static Eenheid maalEenheid(Eenheid el, Eenheid er) {
        if (el == null) return er;
        if (er == null) return el;
        if (el.noemer() != null && er.noemer() == null && er.convertibleNaar(el.noemer())) return el.teller();
        if (er.noemer() != null && el.noemer() == null && el.convertibleNaar(er.noemer())) return er.teller();
        return opaakProduct(el, er);
    }

    /**
     * The identity table of every opaque product THIS derivation built — the provenance mark
     * {@link Eenheidsconflict} keys its exemption on.
     *
     * <p><b>Why identity and not the value.</b> {@link Eenheid#product} builds "€·kg" as a plain
     * {@code Eenheid} with no canonical, no factor and no denominator, and the wire can declare a unit with
     * exactly those four fields and a "·" in its name. The two are therefore value-indistinguishable, and a
     * test on the ABBREVIATION hands a wire-declared "appel·peer" the exemption meant for a derived product —
     * measured 2026-09-09: {@code ref} faults on such a document, this backend replayed it. Only WHERE the unit
     * came from separates them, and provenance is identity.
     *
     * <p>A marker field on {@code Eenheid} itself would be the smaller encoding, but that record lives in the
     * frontend module and is outside this card's reach; this table is the same statement made from the one place
     * in the backend that builds such a product.
     *
     * <p><b>Its growth, measured rather than assumed.</b> One entry per {@link #maalEenheid} EVALUATION — not
     * per node and not per model: {@link Eenheid#product} allocates a fresh instance every time, so transpiling
     * one document three times in a JVM adds three sets of entries (measured 0 → 2 → 4 → 6 on a document with
     * two such nodes). The table is monotone for the lifetime of the JVM and is never cleared, so it is a
     * STRONG-REFERENCE LEAK, stated plainly. Two things bound the damage and neither is a correctness argument
     * about the leak: the corpus produces ZERO entries (no golden carries a two-unit MAAL), and each entry is
     * one small record.
     *
     * <p>Correctness is unaffected by the growth: a stale entry can never mark a NEW unit, because
     * {@code Eenheid.product} never returns an existing instance, so no aliasing is possible however long the
     * table lives. Clearing it per transpilation was considered and rejected this round: it would need a
     * guarantee that no other derivation instance is live, and nothing in the build route provides one — the
     * two transpiler branches each construct their own {@code EffectieveEenheid}, and a clear on construction
     * would silently unmark a product the other branch is still holding. That turns a bounded leak into a wrong
     * answer, which is the wrong trade. Bounding it properly wants a weak IDENTITY set, which the JDK has no
     * form of; it is carded rather than improvised here.
     */
    private static final java.util.Set<Eenheid> AFGELEIDE_PRODUCTEN =
            java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new IdentityHashMap<>()));

    private static Eenheid opaakProduct(Eenheid a, Eenheid b) {
        Eenheid p = Eenheid.product(a, b);
        AFGELEIDE_PRODUCTEN.add(p);
        return p;
    }

    /** Whether {@code e} is an opaque product THIS derivation built (identity, never the name). */
    static boolean isAfgeleidProduct(Eenheid e) {
        return e != null && AFGELEIDE_PRODUCTEN.contains(e);
    }

    /** The table's current size — so the growth law above is a CHECKED claim rather than a comment. */
    static int aantalAfgeleideProducten() {
        return AFGELEIDE_PRODUCTEN.size();
    }

    private Eenheid letEenheid(Expr e, Map<String, Eenheid> omgeving, int diepte, Eenheid annotatie) {
        if (e instanceof Expr.Let b) return eenheid(b.body(), metBinder(omgeving, b, diepte), diepte + 1);
        if (e instanceof Expr.BinderRef lz) {
            // BOUND (to a unit, or to a unitless value) and UNBOUND are different facts, so both tables are asked
            // with containsKey: a binder bound to a unitless value HAS a source and the annotation may not speak
            // over it; only a read no Let anywhere binds falls back. Both maps hold nulls for exactly this.
            if (omgeving.containsKey(sleutel(lz.naam()))) return omgeving.get(sleutel(lz.naam()));
            if (lezingEenheden == null) bouwLezingen();
            return lezingEenheden.containsKey(e) ? lezingEenheden.get(e) : annotatie;
        }
        // ch. 10.2: exactly one branch runs; both must agree on the unit, so either arm derives it.
        Expr.IfThenElse k = (Expr.IfThenElse) e;
        Eenheid dan = eenheid(k.dan(), omgeving, diepte + 1);
        return dan != null ? dan : eenheid(k.anders(), omgeving, diepte + 1);
    }

    private Map<String, Eenheid> metBinder(Map<String, Eenheid> omgeving, Expr.Let b, int diepte) {
        Map<String, Eenheid> binnen = new HashMap<>(omgeving);
        // WF-076: the value is derived in the ENCLOSING scope (the binder is not visible in its own value).
        // A UNITLESS value binds null (HashMap carries it) rather than removing the key: removing would report
        // the binder as unbound, and an unbound read falls back to its own annotation — the very repetition this
        // class exists to not read. Removal would also un-shadow an outer binder of the same name (WF-064).
        binnen.put(sleutel(b.binder()), eenheid(b.value(), omgeving, diepte + 1));
        return binnen;
    }

    // --- the binder-read table ------------------------------------------------------------------

    /**
     * Record the derived unit of every binder READ, by walking each {@link Expr.Let} with a scope. Walking from
     * EVERY Let (not only the outermost) is deliberate and conflict-free: a walk started at an inner Let simply
     * does not bind the outer names, so it records nothing for their reads; a walk started at an enclosing one
     * shadows correctly and records both. A read whose binder no walk ever binds keeps its own annotation.
     */
    private void bouwLezingen() {
        lezingEenheden = new IdentityHashMap<>();
        for (Expr l : lets) noteer(l, LEEG, 0);
    }

    private void noteer(Expr e, Map<String, Eenheid> omgeving, int diepte) {
        if (e == null || diepte > MAX_DIEPTE) return;
        if (e instanceof Expr.BinderRef lz) {
            String k = sleutel(lz.naam());
            if (omgeving.containsKey(k)) lezingEenheden.put(e, omgeving.get(k));
            return;
        }
        if (e instanceof Expr.Let b) {
            noteer(b.value(), omgeving, diepte + 1);
            noteer(b.body(), metBinder(omgeving, b, diepte), diepte + 1);
            return;
        }
        if (e instanceof Expr.IfThenElse k) {
            for (Expr x : conditieOperanden(k.conditie())) noteer(x, omgeving, diepte + 1);
            noteer(k.dan(), omgeving, diepte + 1);
            noteer(k.anders(), omgeving, diepte + 1);
            return;
        }
        for (Expr kind : kinderen(e)) noteer(kind, omgeving, diepte + 1);
    }

    /** Every Expr-carrying component of an ordinary v0 node (the {@code Expr.bevat} inventory minus its ch. 4.10
     *  arms — {@link #noteer} matches all three cluster nodes itself, because each needs a SCOPE this cannot
     *  carry). */
    private static List<Expr> kinderen(Expr e) {
        if (e instanceof Expr.Binair b) return kinderenVan(b.links(), b.rechts());
        if (e instanceof Expr.Afgerond a) return kinderenVan(a.basis());
        if (e instanceof Expr.Begrensd b) return kinderenVan(b.basis(), b.minimum(), b.maximum());
        if (e instanceof Expr.DimensieCel c) return kinderenVan(c.attribuut());
        if (e instanceof Expr.Functie f) {
            java.util.List<Expr> uit = new java.util.ArrayList<>();
            if (f.argumenten() != null) uit.addAll(f.argumenten());
            if (f.leegwaarde() != null) uit.add(f.leegwaarde());
            uit.addAll(conditieOperanden(f.filter()));
            return uit;
        }
        if (e instanceof Expr.TekstReeks t) {
            java.util.List<Expr> uit = new java.util.ArrayList<>();
            if (t.delen() != null)
                for (Expr.TekstReeks.Deel d : t.delen())
                    if (d instanceof Expr.TekstReeks.Ingevoegd i) uit.add(i.expr());
            return uit;
        }
        return List.of();
    }

    private static List<Expr> kinderenVan(Expr... xs) {
        java.util.List<Expr> uit = new java.util.ArrayList<>();
        for (Expr x : xs) if (x != null) uit.add(x);
        return uit;
    }

    /** The Expr operands a condition carries, collected through the shared {@code Conditie.bevatOperand} scan.
     *  That scan is a FULL pre-order walk, so the list holds each operand and then its descendants; {@link #noteer}
     *  re-walks the ones it already reached through the operand itself. Recording a binder read is idempotent, and
     *  the cluster nodes carry their own scope in {@link #noteer}, so the repetition costs work and nothing else —
     *  the same shape this list has carried for a {@code Let} operand since {@code Expr.bevat} grew its Let arm. */
    private static List<Expr> conditieOperanden(Conditie c) {
        if (c == null) return List.of();
        java.util.List<Expr> uit = new java.util.ArrayList<>();
        Conditie.bevatOperand(c, x -> { uit.add(x); return false; });
        return uit;
    }

    private static String sleutel(String naam) {
        return naam == null ? "" : naam.toLowerCase(Locale.ROOT);
    }
}
