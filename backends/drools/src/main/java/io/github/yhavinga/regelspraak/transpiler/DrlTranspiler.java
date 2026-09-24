package io.github.yhavinga.regelspraak.transpiler;

import io.github.yhavinga.regelspraak.ir.Bron;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.ResolvedRegel;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.Rationaal;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Parameter;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;
import io.github.yhavinga.regelspraak.transpiler.kern.Dagsoorten;
import io.github.yhavinga.regelspraak.transpiler.kern.Dekking;
import io.github.yhavinga.regelspraak.transpiler.kern.Eenheidsconflict;
import io.github.yhavinga.regelspraak.transpiler.kern.Gastheer;
import io.github.yhavinga.regelspraak.kern.Literalen;
import io.github.yhavinga.regelspraak.transpiler.kern.Operatoren;
import io.github.yhavinga.regelspraak.transpiler.kern.Rekenjaar;
import io.github.yhavinga.regelspraak.transpiler.kern.Tijdlijnen;
import io.github.yhavinga.regelspraak.transpiler.kern.Vensterpositie;
import io.github.yhavinga.regelspraak.kern.Tijdrekening;

import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.bevatNaam;
import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.commentVeilig;
import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.drlString;
import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.escapeRegelNaam;
import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.gedeclareerdeNaam;
import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.getter;
import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.ident;
import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.opNaam;
import static io.github.yhavinga.regelspraak.transpiler.kern.Namen.setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * The Drools back-end: it walks the resolved IR ({@link ResolvedRegel}) and emits
 * DRL text. Each assignment rule becomes one DRL rule — the §13.4.13 condition is
 * the "when" (a pattern over the target's object type with the condition as a
 * constraint), and the target-:=-value is the "then" (a modify-block setter).
 *
 * This is the first back-end increment: it generates faithful DRL for the
 * resolvable subset (a target object type, comparisons and arithmetic over the
 * subject's own attributes, parameters and literals) and emits an explicit,
 * clearly-marked placeholder for everything the resolver left unbound — navigation
 * to other objects, aggregations (which need accumulate), bounds and rounding. So
 * the DRL states its own coverage, exactly like the IR it is built from. Parameters
 * become globals. The mvel dialect is used so arithmetic and comparison read as
 * infix over BigDecimal.
 */
public final class DrlTranspiler {
    private static final String SUBJECT = "$subject";
    /** The package the generated DRL imports its fact classes from. The fact model
     *  (a POJO per objecttype, with the getters/setters this transpiler assumes) is
     *  the contract between the generated rules and the runtime. */
    public static final String FACT_PACKAGE = "io.github.yhavinga.regelspraak.facts";
    private final RegelSpraakModel model;
    /** RUNTIME-SEMANTICS ch. 2.10: the §4 EFFECTIVE-unit derivation — declarations plus the ch. 2.6 join rules,
     *  the ONE source every unit restatement below consults instead of a node's own wire annotation. Since this
     *  branch lowers the ch. 4.10 {@link Expr.Let} chain of a §11 variabelendeel, it hands the derivation the
     *  document's Let INDEX: a restatement deep inside a body asks a {@link Expr.BinderRef} for its unit with no
     *  route back up to its own binding, and only that index resolves it — without it the read falls back to its
     *  own wire annotation, which is the repetition ch. 2.10 exists to not read. A caller that has no index
     *  (a transpile-only witness) passes none and gets the pre-existing fallback. */
    private final io.github.yhavinga.regelspraak.transpiler.kern.EffectieveEenheid eff;
    /** §9.2: the salience derivation over the declared kenmerk graph, shared with ExecModelTranspiler. */
    /** Computed at {@link #run} start, once the navigation bindings the reads depend on are known. */
    private Stratificatie strat;
    /** RUNTIME-SEMANTICS ch. 7.8 / ch. 7.7: the standing-target classification, the SHARED derivation
     *  ({@link io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen}) ExecModelTranspiler reads too.
     *  It decides WHERE a leeg write is suppressed; a branch-local re-derivation is what let this back-end
     *  erase four goldens' standing values while the exec branch preserved them. */
    private final io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen staande;

    /** The §4 restatement datatype of {@code e}: its annotation with the EFFECTIVE unit grafted on. Every
     *  restatement argument in this file goes through here — a bare {@code e.type()} feeding
     *  {@code omrekenWaarde} / {@code herschaalConditie} / {@code herschaalExact} / {@code vergelijkingsOperanden}
     *  is the defect this replaces. The two documented exceptions keep their annotation and say so at the site:
     *  a date ± duration operand's calendar unit (WF-035) and a tijdsduur's own "in &lt;eenheid&gt;" (WF-038). */
    private Datatype effType(Expr e) { return eff.van(e); }
    private final StringBuilder out = new StringBuilder();
    /** The {@link io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime} static helpers the rules called (method
     *  names), emitted once after the fact imports as {@code import function …RegelSpraakRuntime.<naam>;}
     *  lines — single-runtime principle: the generated DRL carries NO {@code function} bodies; the ONE rs* implementation
     *  is the runtime (which must therefore be on the DRL-compiling classpath, see docs/ENGINE.md). Only the
     *  helpers actually used are imported, sorted for stable output. */
    private final java.util.TreeSet<String> runtimeImports = new java.util.TreeSet<>();
    /** §13.4.6/§9 set iff an objectcreatie emitted a {@link io.github.yhavinga.regelspraak.runtime.CreationMarker} idempotence
     *  guard, so the header imports that runtime fact class exactly when it is referenced (like the Segment import). */
    private boolean creationMarkerNodig;
    /** §8.1.9 set iff a referenced rule emitted a {@link io.github.yhavinga.regelspraak.runtime.RuleFired} "is gevuurd"
     *  marker (or a condition read one), so the header imports that runtime fact class exactly when referenced. */
    private boolean ruleFiredNodig;
    /** §8.1.9 the rule names referenced by a live "regelversie X is gevuurd" predicate — walked from the model
     *  ONCE up front, so only a referenced rule's consequence inserts the {@link io.github.yhavinga.regelspraak.runtime.RuleFired}
     *  marker (opt-in emission, no fact-base bloat). Populated at {@link #run} start. */
    private java.util.Set<String> gevuurdReferenties = java.util.Set.of();
    /** Set exactly where emitted rule text references the §4.2 rekendatum global (a §8.4.3 periode-window, a
     *  §5.3 Rekendatum value read), so the global is spliced into the header iff actually used. */
    private boolean rekendatumNodig;
    /** §9.8 dagsoort predicates the rules referenced: DRL global name ({@code rsDagsoort_<Naam>}) -> the
     *  predicate compiled by the SHARED {@link Dagsoorten} compiler. Declared as
     *  {@code global java.util.function.Predicate rsDagsoort_<Naam>;} after the parameter globals; a session
     *  builder must supply them from {@link #dagsoortGlobalen()} — the analog of the parameter globals. */
    private final java.util.LinkedHashMap<String, java.util.function.Predicate<java.time.LocalDate>> dagsoortGlobalen
            = new java.util.LinkedHashMap<>();
    private final Dagsoorten dagsoorten;

    public DrlTranspiler(RegelSpraakModel model) { this(model, List.of()); }

    /** @param lets the document's ch. 4.10 {@link Expr.Let} index ({@code IrLoader.Loaded.lets}) — see {@link #eff}. */
    public DrlTranspiler(RegelSpraakModel model, List<Expr.Let> lets) { this(model, lets, List.of()); }

    /** @param lets     the document's ch. 4.10 {@link Expr.Let} index ({@code IrLoader.Loaded.lets}) — see {@link #eff}.
     *  @param defaults the ch. 7 prioritizedDefaults extension beside the model. This branch has no defaults
     *      LOWERING and its caller refuses a defaults-carrying document at the door, so the list is empty on
     *      every document that reaches emission — it is threaded anyway because the standing classification's
     *      {@code other_writer} clause reads it, and a branch that silently passed an empty list would widen
     *      its own standing domain the day this branch does gain the lowering. */
    public DrlTranspiler(RegelSpraakModel model, List<Expr.Let> lets,
            List<io.github.yhavinga.regelspraak.ir.PrioritizedDefaults> defaults) {
        this(model, lets, defaults, java.util.Map.of());
    }

    /** @param bronPaden the ch. 4.3 / WF-102 fold-source RANGES ({@code IrLoader.Loaded.bronPaden}), per
     *      {@link Expr.Functie} node, by IDENTITY. It is the ONE thing beside the model this branch cannot
     *      re-derive: the by-pair search below answers for a type PAIR, and a path's intermediate types are its
     *      own claim — so a branch that did not receive it would fold whatever DIRECT relation happens to connect
     *      subject and element and answer differently from ref, clingo and its own sister branch. */
    public DrlTranspiler(RegelSpraakModel model, List<Expr.Let> lets,
            List<io.github.yhavinga.regelspraak.ir.PrioritizedDefaults> defaults,
            java.util.Map<Object, java.util.List<RegelSpraakModel.RolBindingRef>> bronPaden) {
        this(model, lets, defaults, bronPaden, java.util.Map.of());
    }

    /** @param rolBindingen the wire's symbolic (feittype, rol) binding per IR node
     *      ({@code IrLoader.Loaded.rolBindingen}), by IDENTITY — on an aggregation {@link Expr.Functie} the
     *      ch. 4.3 {@code sourceRole}, naming the role the folded ELEMENT plays. Read as the LAST resort in
     *      {@link #scope}, after the written element name and the by-pair search, so every join the model
     *      already derives stays exactly as it was. Without it a fold whose argument is an ATTRIBUTE name
     *      (SOM/MINIMUM/MAXIMUM/TOTAAL) has only the by-pair search, which is ambiguous by construction on a
     *      repeated type pair — the marker this branch emitted there while the exec branch, which has taken
     *      the binding since 2026-08-30, played. */
    public DrlTranspiler(RegelSpraakModel model, List<Expr.Let> lets,
            List<io.github.yhavinga.regelspraak.ir.PrioritizedDefaults> defaults,
            java.util.Map<Object, java.util.List<RegelSpraakModel.RolBindingRef>> bronPaden,
            java.util.Map<Object, RegelSpraakModel.RolBindingRef> rolBindingen) {
        this.model = model;
        this.eff = new io.github.yhavinga.regelspraak.transpiler.kern.EffectieveEenheid(lets);
        this.dagsoorten = new Dagsoorten(model);
        this.staande = io.github.yhavinga.regelspraak.transpiler.kern.StaandeDoelen.van(model, defaults);
        if (bronPaden != null && !bronPaden.isEmpty()) {
            this.bronPaden = new java.util.IdentityHashMap<>();   // identity: the nodes are value-equal records
            this.bronPaden.putAll(bronPaden);
        }
        if (rolBindingen != null && !rolBindingen.isEmpty()) {
            this.rolBindingen = new java.util.IdentityHashMap<>();   // identity, for bronPaden's reason
            this.rolBindingen.putAll(rolBindingen);
        }
    }

    /** See the constructor parameter of the same name. Empty by default — every fold then carries the one-step
     *  range and this branch behaves exactly as it did before WF-102. */
    private java.util.Map<Object, java.util.List<RegelSpraakModel.RolBindingRef>> bronPaden = java.util.Map.of();

    /** See the constructor parameter of the same name ({@code IrLoader.Loaded.rolBindingen}). Empty by default. */
    private java.util.Map<Object, RegelSpraakModel.RolBindingRef> rolBindingen = java.util.Map.of();

    /** IR-SPEC ch. 4.3, the set source: a fold whose single argument is read through a range — a one-step
     *  {@code sourceRole} or a {@code sourcePath} — ranges over linked instances, and that argument is the SET source;
     *  without one the argument is the subject's own attribute. */
    private boolean heeftBereik(Expr.Functie f) {
        return rolBindingen.containsKey(f) || bronPaden.containsKey(f);
    }

    /** IR-SPEC ch. 4.1a: the navigation bindings per AttribuutRef / Kenmerk node ({@code IrLoader.Loaded.navigatieBindingen}),
     *  by IDENTITY. Where a node carries one it decides the walk ({@link FactModelGenerator#navigatiewandeling}); a
     *  bindingless node is the subject's own read. Set by the widest {@link #build}. */
    private java.util.Map<Object, java.util.List<RegelSpraakModel.RolBindingRef>> navigatieBindingen = java.util.Map.of();

    public static String transpile(RegelSpraakModel model) { return new DrlTranspiler(model).run(); }

    /** As {@link #transpile} but also exposes the coverage manifest — one {@link Dekking} verdict per
     *  construct, per soort in model-list order (comparable per soort with the exec back-end's). */
    public static DrlTranspiler build(RegelSpraakModel model) { return build(model, List.of()); }

    public static DrlTranspiler build(RegelSpraakModel model, List<Expr.Let> lets) {
        return build(model, lets, List.of());
    }

    public static DrlTranspiler build(RegelSpraakModel model, List<Expr.Let> lets,
            List<io.github.yhavinga.regelspraak.ir.PrioritizedDefaults> defaults) {
        return build(model, lets, defaults, java.util.Map.of());
    }

    public static DrlTranspiler build(RegelSpraakModel model, List<Expr.Let> lets,
            List<io.github.yhavinga.regelspraak.ir.PrioritizedDefaults> defaults,
            java.util.Map<Object, java.util.List<RegelSpraakModel.RolBindingRef>> bronPaden) {
        return build(model, lets, defaults, bronPaden, java.util.Map.of());
    }

    public static DrlTranspiler build(RegelSpraakModel model, List<Expr.Let> lets,
            List<io.github.yhavinga.regelspraak.ir.PrioritizedDefaults> defaults,
            java.util.Map<Object, java.util.List<RegelSpraakModel.RolBindingRef>> bronPaden,
            java.util.Map<Object, RegelSpraakModel.RolBindingRef> rolBindingen) {
        return build(model, lets, defaults, bronPaden, rolBindingen, java.util.Map.of());
    }

    /** As above, plus the ch. 4.1a navigation bindings ({@code IrLoader.Loaded.navigatieBindingen}) — the arity the
     *  replay API builds with. */
    public static DrlTranspiler build(RegelSpraakModel model, List<Expr.Let> lets,
            List<io.github.yhavinga.regelspraak.ir.PrioritizedDefaults> defaults,
            java.util.Map<Object, java.util.List<RegelSpraakModel.RolBindingRef>> bronPaden,
            java.util.Map<Object, RegelSpraakModel.RolBindingRef> rolBindingen,
            java.util.Map<Object, java.util.List<RegelSpraakModel.RolBindingRef>> navigatieBindingen) {
        DrlTranspiler t = new DrlTranspiler(model, lets, defaults, bronPaden, rolBindingen);
        if (navigatieBindingen != null && !navigatieBindingen.isEmpty()) {
            t.navigatieBindingen = new java.util.IdentityHashMap<>();   // identity, for bronPaden's reason
            t.navigatieBindingen.putAll(navigatieBindingen);
        }
        t.drl0 = t.run();
        return t;
    }
    private String drl0;
    private final List<Dekking> dekking = new ArrayList<>();
    public String drl() { return drl0; }
    public List<Dekking> dekking() { return dekking; }
    /** The §9.8 dagsoort predicates the emitted DRL declares as globals (name -> predicate): a session builder
     *  must {@code setGlobal} each before firing — exactly like the parameter globals it already supplies. */
    public java.util.Map<String, java.util.function.Predicate<java.time.LocalDate>> dagsoortGlobalen() {
        return java.util.Collections.unmodifiableMap(dagsoortGlobalen);
    }

    /** Record a {@link io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime} helper as used — it surfaces as one
     *  header {@code import function} line — and return its name for call-site splicing. Idempotent. */
    private String fnImport(String naam) { runtimeImports.add(naam); return naam; }

    private String run() {
        strat = Stratificatie.bereken(model.resolvedRegels, this::genavigeerd);
        gevuurdReferenties = model.gevuurdReferenties();   // §8.1.9 opt-in: only these rules mark their firing
        out.append("package io.github.yhavinga.regelspraak.generated;\n\n");
        // Import the fact class for each object type so Drools resolves the patterns.
        for (Objecttype ot : model.objecttypen.values())
            out.append("import ").append(FACT_PACKAGE).append('.').append(ident(ot.naam, true)).append(";\n");
        if (!model.objecttypen.isEmpty()) out.append('\n');
        int naImports = out.length();   // the runtime `import function` lines (if any) are spliced in here
        // Parameters become globals the runtime supplies. A §13.3.6 TEMPORAL parameter ("voor elk jaar") supplies a
        // timeline (a List<Segment>) rather than a scalar — only a §7.3.2 tijdsevenredig base reads it (any scalar use
        // markers in waarde()).
        for (Parameter p : model.parameters.values())
            out.append("global ").append(p.granulariteit() != null ? "java.util.List" : javaType(p.datatype())).append(' ')
               .append(ident(p.naam(), false)).append(";\n");
        if (!model.parameters.isEmpty()) out.append('\n');
        // §4.2 version selection: a time-bounded rule version guards on the Rekendatum the
        // runtime supplies, so declare it as a global when any rule of any kind needs it.
        boolean tijdgebonden =
                   model.resolvedRegels.stream().anyMatch(r -> tijdgebondenGeldigheid(r.geldigheid()))
                || model.objectCreaties.stream().anyMatch(o -> tijdgebondenGeldigheid(o.geldigheid()))
                || model.verdelingen.stream().anyMatch(v -> tijdgebondenGeldigheid(v.geldigheid()))
                || model.consistenties.stream().anyMatch(c -> tijdgebondenGeldigheid(c.geldigheid()))
                || model.uniciteiten.stream().anyMatch(u -> tijdgebondenGeldigheid(u.geldigheid()))
                || model.uniciteitUnies.stream().anyMatch(u -> tijdgebondenGeldigheid(u.geldigheid()))
                || model.feitCreaties.stream().anyMatch(f -> tijdgebondenGeldigheid(f.geldigheid()));
        if (tijdgebonden) out.append("global java.time.LocalDate rekendatum;\n\n");
        int naHeader = out.length();   // dagsoort-predicate globals (if any) are spliced in here, after the header

        for (ResolvedRegel r : model.resolvedRegels) meet("regel", r.naam(), () -> regel(r));
        for (io.github.yhavinga.regelspraak.ir.ResolvedObjectCreatie oc : model.objectCreaties) meet("objectcreatie", oc.naam(), () -> objectCreatie(oc));
        for (io.github.yhavinga.regelspraak.ir.ResolvedVerdeling v : model.verdelingen) meet("verdeling", v.naam(), () -> verdeling(v));
        for (io.github.yhavinga.regelspraak.ir.ResolvedConsistentie c : model.consistenties) meet("consistentie", c.naam(), () -> consistentie(c));
        for (io.github.yhavinga.regelspraak.ir.ResolvedUniciteit u : model.uniciteiten) meet("uniciteit", u.naam(), () -> uniciteit(u));
        for (io.github.yhavinga.regelspraak.ir.ResolvedUniciteitUnie u : model.uniciteitUnies) meet("uniciteit-unie", u.naam(), () -> uniciteitUnie(u));
        // The index rides along ONLY to name a NAMELESS feitcreatie (see feitCreatie): the wire position is the one
        // distinguisher a null naam leaves, and DRL rule names must be unique within the package.
        for (int i = 0; i < model.feitCreaties.size(); i++) {
            io.github.yhavinga.regelspraak.ir.ResolvedFeitCreatie f = model.feitCreaties.get(i);
            final int nr = i;
            meet("feitcreatie", f.naam(), () -> feitCreatie(f, nr));
        }
        // Splice the deferred header blocks BACK TO FRONT so the earlier offset stays valid.
        // §8.4.3 periodevergelijking / §5.3 Rekendatum-als-waarde reference the rekendatum global from a rule
        // body; the flag is set EXACTLY where that text is emitted (never an undeclared-global build error, never
        // an over-eager scan) — declare it here iff the §4.2 geldigheid trigger did not already.
        if (rekendatumNodig && !tijdgebonden) out.insert(naHeader, "global java.time.LocalDate rekendatum;\n\n");
        if (!dagsoortGlobalen.isEmpty()) {
            StringBuilder g = new StringBuilder();
            for (String naam : dagsoortGlobalen.keySet())
                g.append("global java.util.function.Predicate ").append(naam).append(";\n");
            out.insert(naHeader, g.append('\n'));
        }
        if (!runtimeImports.isEmpty() || creationMarkerNodig || ruleFiredNodig) {
            StringBuilder imp = new StringBuilder();
            // The tijdlijn helpers construct Segments via a `Segment.class` argument at their call sites; the
            // fact class is imported iff the emitted text references it — DERIVED from the output, so a new
            // helper call site can never forget to register the import separately.
            if (out.indexOf("Segment.class") >= 0)
                imp.append("import ").append(FACT_PACKAGE).append(".Segment;\n");
            if (creationMarkerNodig)   // §13.4.6/§9 the objectcreatie idempotence control fact (a runtime class, not a per-model bean)
                imp.append("import io.github.yhavinga.regelspraak.runtime.CreationMarker;\n");
            if (ruleFiredNodig)        // §8.1.9 the "is gevuurd" execution-status control fact (a runtime class, not a per-model bean)
                imp.append("import io.github.yhavinga.regelspraak.runtime.RuleFired;\n");
            for (String fn : runtimeImports)
                imp.append("import function io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime.").append(fn).append(";\n");
            out.insert(naImports, imp.append('\n'));
        }
        return out.toString();
    }

    /** Emit one construct and record its manifest verdict. Classification wraps the documented marker
     *  invariant rather than re-deriving it: every non-lowered path in this back-end leaves a
     *  {@code /* TODO *&#47;} placeholder ({@link Frag#todo} / {@link #onopgelosteConstraint} /
     *  {@link #todo}) or an {@code // onopgelost} comment clause in the emitted text, and a clean
     *  rule contains neither, so the construct's own DRL slice decides LOWERED vs MARKER. The scan
     *  runs over the slice with its STRING LITERALS blanked ({@link #zonderStrings}): user text (a
     *  Tekst/enum literal, an escaped rule name) is the one channel that could counterfeit the
     *  sentinel on a lowered rule — a {@code STRING_TEXT} cannot span lines or contain a quote, so
     *  outside quotes only the marker emitters can produce it. A MARKER also raises a deduplicated
     *  WARNING (a semantically inert rule must never be silent). */
    private void meet(String soort, String naam, Runnable emitter) {
        int start = out.length();
        navReden = null;   // a reason belongs to the construct that produced it; never carried into the next one
        emitter.run();
        String tekst = zonderStrings(out.substring(start));
        boolean marker = tekst.contains("/* TODO") || tekst.contains("// onopgelost");
        Dekking d = marker ? Dekking.marker(soort, naam, navReden != null ? navReden : markerReden(tekst))
                : Dekking.lowered(soort, naam);
        dekking.add(d);
        d.meld(model);
    }

    /** Blank the contents of double-quoted segments (emitted string literals and quoted rule names).
     *  A raw {@code "} or newline cannot occur inside either (STRING_TEXT excludes both; escapeRegelNaam
     *  escapes them), but an ESCAPED quote {@code \"} can — the scan honours backslash escapes so a
     *  quoted-name boundary is never mispaired. */
    private static String zonderStrings(String tekst) {
        return tekst.replaceAll("\"(?:\\\\.|[^\"\\\\\n])*\"", "\"\"");
    }

    /** The first placeholder's own text as the manifest reason: the body of the first
     *  {@code /* TODO … *&#47;} or the remainder of the first {@code // onopgelost …} line. */
    private static String markerReden(String tekst) {
        int todo = tekst.indexOf("/* TODO"), onop = tekst.indexOf("// onopgelost");
        if (todo >= 0 && (onop < 0 || todo < onop)) {
            int einde = tekst.indexOf("*/", todo);
            return tekst.substring(todo + 3, einde < 0 ? tekst.length() : einde).trim();
        }
        int einde = tekst.indexOf('\n', onop);
        return tekst.substring(onop + 3, einde < 0 ? tekst.length() : einde).trim();
    }

    /**
     * One DRL rule under construction: an ordered list of "when" clauses (patterns, accumulate CEs and
     * guards, in the exact textual order they must appear) and a "then" consequence body. {@link #render}
     * owns the rule header, the 4-space when-line indent, and the rule-name escaping — the structure that
     * was hand-appended, identically, by every emitter. This is rule-ASSEMBLY only: the clause and value
     * strings are still produced by the leaf emitters; here they are composed, not re-derived. An inert
     * no-op rule (the resolver could not lower it) is just a Regel whose clauses are a {@code // onopgelost}
     * comment and an {@code eval(true)} with an empty consequence — so the no-op is built structurally,
     * not special-cased in the renderer.
     */
    private final class Regel {
        private final String naam;
        private final List<String> when = new ArrayList<>();
        private String then = "";
        private boolean noLoop = true;
        private int salience = 0;

        Regel(String naam) { this.naam = naam; }

        /** §9.10 fase-2: drop {@code no-loop} so the rule can forward-chain on its OWN inserts (a recursief-group
         *  objectcreatie must reach the fixpoint). Safe ONLY because the §13.4.6/§9 idempotence guard already caps
         *  each subject at one creation — it, not no-loop, is the anti-runaway control here. */
        Regel zonderNoLoop() { this.noLoop = false; return this; }

        /** §9.2 negation-stratification: set the rule's agenda salience. A rule whose voorwaarde reads a NEGATED
         *  kenmerk sinks to the bottom of the agenda (salience &lt; 0) so every producer of that kenmerk fires
         *  first — mirrors {@link ExecModelTranspiler#stratificatieSalience}. Salience 0 (the default) is unchanged. */
        Regel salience(int s) { this.salience = s; return this; }

        /** Add one when-clause (a pattern, an accumulate, or a guard) — rendered as a single indented line. */
        Regel clausule(String c) { when.add(c); return this; }
        Regel clausules(List<String> cs) { when.addAll(cs); return this; }
        /** The §4.2 validity guard — added only for a time-bounded version, so an "altijd" rule is unchanged. */
        Regel geldigheid(io.github.yhavinga.regelspraak.ir.Geldigheid g) {
            if (tijdgebondenGeldigheid(g)) when.add(geldigheidGuard(g));
            return this;
        }
        /** The consequence body: the text between {@code then} and {@code end}, each line already 4-space
         *  indented and newline-terminated (empty for an inconsistency-free / inert rule). */
        Regel dan(String body) { this.then = body; return this; }

        /** Prepend a line to the consequence body — used to inject the §8.1.9 {@code RuleFired} insert at the
         *  TOP of the {@code then} (before any changed-value guard), so a no-op modify still records "gevuurd". */
        Regel danVoor(String prefix) { this.then = prefix + this.then; return this; }

        String render() {
            StringBuilder b = new StringBuilder();
            // §9.10 fase-1 namespacing: the ENGINE-VISIBLE header alone gets the "<groep>: " prefix; every DATA
            // KEY below (RuleFired/CreationMarker/Inconsistentie `regel ==`) keeps the BARE name. Non-grouped
            // rules are unchanged (geëmitteerdeRegelNaam is identity for them).
            b.append("rule \"").append(escapeRegelNaam(model.geëmitteerdeRegelNaam(naam))).append("\"\n");
            b.append("    dialect \"mvel\"\n")
             .append(salience != 0 ? "    salience " + salience + "\n" : "")
             .append(noLoop ? "    no-loop true\n" : "").append("when\n");
            for (String c : when) b.append("    ").append(c).append('\n');
            b.append("then\n").append(then).append("end\n\n");
            return b.toString();
        }
    }

    /** The per-rule accumulate collector (no-mutable-build-state): value/condition lowering RETURNS its hoisted
     *  accumulate/eval clauses by appending to this explicitly-threaded context — a local of each rule
     *  emitter, passed as a parameter, never a field — so clauses from one rule can structurally never
     *  leak into the next, and a bailing lowering path simply abandons its context slice ({@link #aggTerug})
     *  instead of depending on a hidden reset. {@code n} numbers the {@code $accN}/{@code $nN} bindings. */
    private static final class Acc {
        final List<String> clausules = new ArrayList<>();
        int n;
        /** ch. 4.10 / ch. 10.6 — the binder ENVIRONMENT of the value being lowered: binder key ({@link #letSleutel})
         *  -&gt; the consequence-local mvel variable its forced need lives in. Threaded with the rest of this
         *  context, and scoped by save/restore around each {@link Expr.Let} body, so a binding dies with its body
         *  (WF-064: an inner binder of the same name SHADOWS, it never mutates the outer one in place). */
        final java.util.Map<String, String> binders = new java.util.LinkedHashMap<>();
        /** The same environment for the CONDITION side: binder key -&gt; the need's own re-readable DECOMPOSITION
         *  ({@link OperandDeel}: its leeg/vol guards and its bare-property value), or null when the need does not
         *  decompose into one. A companion rule is a separate rule and cannot see the host consequence's locals,
         *  so a §6.5/§6.6 guard OR SITE that mentions a binder must re-read the need itself — and drop the
         *  companion when it cannot ({@link #takGarde}, {@link #companionDeel}). The whole decomposition and not
         *  merely its value text, because a §6.5 SITE tests leegte: {@code k gedeeld door restpost} needs
         *  {@code k}'s vol-guard exactly as the un-bound {@code inkomen gedeeld door restpost} needs its own. */
        final java.util.Map<String, OperandDeel> binderDeel = new java.util.LinkedHashMap<>();
        /** Set while lowering the children of a NON-conjunctive {@link Conditie.Samengesteld}: their verdicts are
         *  folded with {@code ||} / {@code !}, so a child that answers with a SEPARATE when-clause instead of an
         *  inline constraint has no faithful place there — the clause would assert unconditionally what the fold
         *  means to weigh. {@link LoweredConditie#heeftExtra} is that signal for one of the two hoist channels;
         *  this flag is how the OTHER channel — a child pushing straight into {@link #clausules} — learns it
         *  before it writes anything, so its refusal names ITS OWN node instead of arriving as a parse error.
         *  Sticky on the way down: an inner conjunction inside a disjunction does not lift it, because the outer
         *  fold is still the one that would have to host the clause. */
        boolean hijsVerbod;
        /** The reason of the child that refused BECAUSE of {@link #hijsVerbod}, so the compound's own marker can
         *  say WHICH node and WHY instead of the generic "voorwaarde". A markered child's text is otherwise
         *  dropped — the compound replaces it with its own — and this node's whole refusal discipline is that
         *  every decline names its axis. Scoped like the flag: saved and restored around each recursion. */
        String hijsReden;
        /** The consequence PRELUDE: one {@code $behoefteN = …;} line per forced need, in evaluation order. Non-null
         *  exactly where the consequence can carry it — the scalar setter path of {@link #regel}. A {@link Expr.Let}
         *  met while this is null has nowhere to put its need and stays an honest marker. */
        List<String> letVoorspel;
        /** The needs whose barrier stands between this point and the enclosing consequence, innermost last —
         *  ch. 10.6 clause 2. A need is FORCED under the conjunction of these, so a value the barrier would have
         *  cut off is never evaluated (and never faults). Pushed/popped around each {@link Expr.Let} body. */
        final List<String> letBewaking = new ArrayList<>();
        int letN;
    }

    /**
     * ch. 10.6 clause 3 + ch. 10.2 as one static answer: <b>under which condition does evaluating a body read a
     * given binder?</b>
     *
     * <p>Without a ch. 10.2 node the answer is a constant — every other form this branch lowers evaluates all its
     * children, so a read is reached iff it is present ({@link #ALTIJD} / {@link #NOOIT}). An {@code IfThenElse}
     * makes it a CONDITION: a binder read only in the {@code dan} arm is read exactly when the guard holds. That
     * is what {@link Tak} carries, and it is what keeps a need that only one arm reads from being forced (and its
     * barrier from firing) when the other arm runs — the flank {@code synth3-takbarriere} pins.
     *
     * <p>{@link #ONBEPAALD} is the honest fourth answer: two DIFFERENT branch conditions joining (a read in one
     * arm of one {@code IfThenElse} and in one arm of another). A conjunction could be built for it, but not one
     * whose operands are guaranteed to be assigned by the time the prelude needs them, so this branch refuses the
     * shape by name instead of guessing.
     */
    private sealed interface Leesgarde permits Altijd, Nooit, Tak, Onbepaald {}
    private record Altijd() implements Leesgarde {}
    private record Nooit() implements Leesgarde {}
    /** Read exactly when {@code conditie} holds ({@code positief}) or does not ({@code !positief}). */
    private record Tak(Conditie conditie, boolean positief) implements Leesgarde {}
    private record Onbepaald() implements Leesgarde {}
    private static final Leesgarde ALTIJD = new Altijd(), NOOIT = new Nooit(), ONBEPAALD = new Onbepaald();

    /** The join of two read-guards: ALTIJD absorbs, NOOIT is the unit, two EQUAL branch guards join to themselves
     *  and two different ones are ONBEPAALD (see {@link Leesgarde}). */
    private static Leesgarde ofOok(Leesgarde a, Leesgarde b) {
        if (a instanceof Altijd || b instanceof Altijd) return ALTIJD;
        if (a instanceof Nooit) return b;
        if (b instanceof Nooit) return a;
        if (a instanceof Tak ta && b instanceof Tak tb
                && ta.conditie() == tb.conditie() && ta.positief() == tb.positief()) return a;
        return ONBEPAALD;
    }

    /** The meet used where a read happens only if BOTH hold (a read inside a nested need's value: the need must
     *  be forced AND the value must read). ALTIJD is the unit, NOOIT absorbs, a branch guard survives beside an
     *  ALTIJD, and anything else is ONBEPAALD. */
    private static Leesgarde enOok(Leesgarde a, Leesgarde b) {
        if (a instanceof Nooit || b instanceof Nooit) return NOOIT;
        if (a instanceof Altijd) return b;
        if (b instanceof Altijd) return a;
        if (a instanceof Tak ta && b instanceof Tak tb
                && ta.conditie() == tb.conditie() && ta.positief() == tb.positief()) return a;
        return ONBEPAALD;
    }

    /** The binder-environment key: lower-cased, mirroring {@code ref}'s {@code norm} over its locals and the
     *  exec back-end's binder slots, so binder resolution is case-insensitive on every side. */
    private static String letSleutel(String naam) {
        return naam == null ? "" : naam.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * ch. 10.6 + ch. 10.2: under which condition does evaluating {@code e} READ the binder {@code sleutel} bound
     * outside it. See {@link Leesgarde} for the four answers.
     *
     * <p>Two nodes carry the whole subtlety. A nested {@code Let}'s VALUE is reached only when its own binder is
     * read (clause 3), so a read there is conjoined with that inner guard; WF-064/WF-076 put the nested binder's
     * shadow on the BODY only, never on its own value. An {@code IfThenElse} splits: a read in the CONDITION is
     * unconditional (the guard always runs), and a read in one arm alone is that arm's guard.
     */
    private static Leesgarde leesGarde(Expr e, String sleutel) {
        if (e == null) return NOOIT;
        if (e instanceof Expr.BinderRef br) return sleutel.equals(letSleutel(br.naam())) ? ALTIJD : NOOIT;
        if (e instanceof Expr.Let b) {
            // read through the nested need: the need must be forced AND its value must read the binder
            Leesgarde viaBehoefte = enOok(leesGarde(b.body(), letSleutel(b.binder())), leesGarde(b.value(), sleutel));
            if (letSleutel(b.binder()).equals(sleutel)) return viaBehoefte;   // shadowed throughout the body
            return ofOok(viaBehoefte, leesGarde(b.body(), sleutel));
        }
        if (e instanceof Expr.IfThenElse k) {
            Leesgarde inConditie = NOOIT;
            List<Expr> operanden = new ArrayList<>();
            Conditie.bevatOperand(k.conditie(), x -> { operanden.add(x); return false; });
            for (Expr o : operanden) inConditie = ofOok(inConditie, leesGarde(o, sleutel));
            if (inConditie instanceof Altijd || inConditie instanceof Onbepaald) return inConditie;
            Leesgarde dan = leesGarde(k.dan(), sleutel), anders = leesGarde(k.anders(), sleutel);
            if (dan instanceof Altijd && anders instanceof Altijd) return ALTIJD;
            if (dan instanceof Altijd && anders instanceof Nooit) return new Tak(k.conditie(), true);
            if (dan instanceof Nooit && anders instanceof Altijd) return new Tak(k.conditie(), false);
            if (dan instanceof Nooit && anders instanceof Nooit) return NOOIT;
            return ONBEPAALD;   // a branch guard nested inside a branch — refused by name, never guessed
        }
        Leesgarde uit = NOOIT;
        for (Expr k : gewoneKinderen(e)) uit = ofOok(uit, leesGarde(k, sleutel));
        return uit;
    }

    /** Whether {@code e} reads the binder at all — the site collectors' question, which needs no guard. */
    private static boolean leestBinder(Expr e, String sleutel) {
        return !(leesGarde(e, sleutel) instanceof Nooit);
    }

    /**
     * ch. 10.2/10.6 — the EXCHANGE of two adjacent {@code Let}s, and the ONLY rewrite this branch performs on
     * the tree it lowers.
     *
     * <p><b>Why one is needed at all.</b> This branch does not MOVE a {@code Let} into the arm that reads it:
     * it lowers the binding as control flow, so laziness is a guard on a prelude line and the node stays where
     * it stands. One thing that placement cannot express is ORDER. A need read in exactly one ch. 10.2 arm is
     * forced under that arm's guard, and the guard is lowered at the {@code Let}'s own position — so a guard
     * reading a binder bound DEEPER in the body is a forward reference to a prelude line that does not exist
     * yet, and the honest answer there was a refusal by name ({@code synth3-armwissel}).
     *
     * <p><b>The exchange, and why it is exact.</b> Two adjacent {@code Let}s commute. Under ch. 10.6 a need is
     * evaluated at its first READ, never at its binding, so the nesting order of a pair does not order their
     * evaluations at all; and each barrier still empties the node that bound the binder that was read, while
     * the OUTER of the pair sits at the same position either way. Swapping moves the {@code Let} one step
     * closer to the branch node, and repeating it walks the whole chain down until the guard's binder stands
     * above it. Three scope conditions, each a correctness requirement:
     * <ul>
     *   <li>the two binders differ — otherwise the exchange is a shadowing change;</li>
     *   <li>the nested value does not read the outer binder — it would leave that binder's scope;</li>
     *   <li>the outer value does not read the nested binder's NAME — it would enter the nested scope and a read
     *       meant for an enclosing binding of that name would rebind.</li>
     * </ul>
     *
     * <p>The trigger is the obstruction itself and nothing wider ({@link #gardeLeestBinnenBinder}). A document
     * whose guard already resolves at this position is returned untouched, so its DRL does not move.
     *
     * <p>Applied at BOTH descents of a {@code Let} — {@link #letWaarde} for the consequence and
     * {@link #letDescent} for the §6.5/§6.6 collectors — because it is pure and deterministic, so the two
     * sides see one tree. Termination: each exchange moves the node strictly deeper into a finite body.
     */
    private static Expr.Let letWissel(Expr.Let b) {
        String sleutel = letSleutel(b.binder());
        if (!(leesGarde(b.body(), sleutel) instanceof Tak t)) return b;
        if (!(b.body() instanceof Expr.Let binnen) || !gardeLeestBinnenBinder(t.conditie(), b.body())) return b;
        String binnenSleutel = letSleutel(binnen.binder());
        if (binnenSleutel.equals(sleutel)
                || leestBinder(binnen.value(), sleutel) || leestBinder(b.value(), binnenSleutel)) return b;
        // WF-076 keeps both node types equal to the body type, so the pair carries ONE type and the exchange
        // needs no retyping; each node keeps its own bron, so a marker below still names the node it is about.
        Expr.Let geduwd = letWissel(new Expr.Let(b.binder(), b.value(), binnen.body(), b.type(), b.bron()));
        return new Expr.Let(binnen.binder(), binnen.value(), geduwd, binnen.type(), binnen.bron());
    }

    /**
     * Whether {@code c} reads a binder that some {@code Let} INSIDE {@code e} binds. It is one question doing
     * two jobs, because they are the same question: a branch guard is stated at the {@code Let} it gates while
     * the branch node stands lower down, so if anything in between rebinds a name the condition reads, that
     * name means two different things at the two positions.
     *
     * <ul>
     *   <li>For {@link #letWissel} it is the OBSTRUCTION to remove — the exchange puts the guard's binder above
     *       the need it guards, and then the same text means the same binding at both ends.</li>
     *   <li>For {@link #letWaarde} it is the REFUSAL condition where the exchange could not reach: resolving
     *       the name against whatever happens to be in scope up here is how a guard silently starts speaking
     *       about another binding (measured 2026-09-19, pre-existing: a shadowed outer need made this branch
     *       fabricate an Inconsistentie on a document ref, clingo and the exec branch call consistent).</li>
     * </ul>
     *
     * <p>It over-approximates on purpose — a {@code Let} in a sibling operand or inside an arm trips it too,
     * though neither can shadow the condition — because the alternative is to record the path down to the
     * branch node just to be exact about a shape no corpus golden has. The over-approximation costs refusals,
     * never wrong answers, and the corpus sweep measures that it costs none today.
     */
    private static boolean gardeLeestBinnenBinder(Conditie c, Expr e) {
        if (e == null) return false;
        if (e instanceof Expr.Let l) {
            String s = letSleutel(l.binder());
            return Conditie.bevatOperand(c, x -> leestBinder(x, s))
                    || gardeLeestBinnenBinder(c, l.value()) || gardeLeestBinnenBinder(c, l.body());
        }
        if (e instanceof Expr.IfThenElse k) {
            return gardeLeestBinnenBinder(c, k.dan()) || gardeLeestBinnenBinder(c, k.anders())
                    || Conditie.bevatOperand(k.conditie(), x -> gardeLeestBinnenBinder(c, x));
        }
        for (Expr kind : gewoneKinderen(e)) if (gardeLeestBinnenBinder(c, kind)) return true;
        return false;
    }

    /**
     * Every {@link Expr} child of an ORDINARY (non-BINDING) node, condition operands included — the walk
     * {@link #leesGarde} and the §6.5/§6.6 site collectors descend with a SCOPE (or a branch guard) of their own.
     *
     * <p>The two BINDING nodes are absent because each carries a scope this accessor cannot express, so every
     * caller matches them itself. An {@link Expr.IfThenElse} is absent for a third reason: it binds nothing, but
     * exactly ONE of its arms runs (ch. 10.2), and a flat child list would report a read, or a §6.5 site, on the
     * arm that does not. Every caller therefore matches it too — {@link #leesGarde} to build the branch guard,
     * the site collectors to carry it into the companion's own pattern.
     */
    private static List<Expr> gewoneKinderen(Expr e) {
        List<Expr> uit = new ArrayList<>();
        if (e instanceof Expr.Binair b) { uit.add(b.links()); uit.add(b.rechts()); }
        else if (e instanceof Expr.Afgerond a) uit.add(a.basis());
        else if (e instanceof Expr.Begrensd b) { uit.add(b.basis()); uit.add(b.minimum()); uit.add(b.maximum()); }
        else if (e instanceof Expr.DimensieCel c) uit.add(c.attribuut());
        else if (e instanceof Expr.TekstReeks t) {
            for (Expr.TekstReeks.Deel d : t.delen())
                if (d instanceof Expr.TekstReeks.Ingevoegd i) uit.add(i.expr());
        } else if (e instanceof Expr.Functie f) {
            uit.addAll(f.argumenten());
            uit.add(f.leegwaarde());
            Conditie.bevatOperand(f.filter(), x -> { uit.add(x); return false; });
        }
        uit.removeIf(java.util.Objects::isNull);
        return uit;
    }

    /** §9.5 the consequence shared by the three inconsistency-flagging rules (consistentie, uniciteit,
     *  uniciteit-unie): insert an Inconsistentie carrying the rule name and the offending subject. The
     *  name is escaped to match the {@link #inconsistentieGuard} that keys off the same literal. */
    private String inconsistentieDan(String naam) {
        return "    insert(new " + FACT_PACKAGE + ".Inconsistentie(\""
                + escapeRegelNaam(naam) + "\", " + SUBJECT + "));\n";
    }

    /** §8.1.9 prepend a {@code RuleFired} insert to {@code rule}'s consequence iff {@code naam}'s firing is
     *  referenced by a live "regelversie X is gevuurd" predicate (§8.1.9 opt-in — an unreferenced rule marks
     *  nothing). The insert is UNCONDITIONAL and at the TOP of the {@code then}, OUTSIDE the changed-value
     *  guard: "gevuurd" means the voorwaardendeel held and the consequence ran (§8.1.9: "of er aan de
     *  voorwaarden in het voorwaardendeel van de regel is voldaan"), so even a no-op modify is "gevuurd". A
     *  rule firing for N subjects inserts N RuleFired facts with the same {@code regel}; the reader's exists
     *  collapses them, and a not-exists guard here would change THIS rule's firing semantics, so it is
     *  deliberately omitted (bounded bloat: #referenced-rules × #subjects). The name is escaped to match the
     *  {@link #ruleFiredGuard} the reader keys off the same literal. */
    private void markeerGevuurd(Regel rule, String naam) {
        if (!gevuurdReferenties.contains(naam)) return;
        ruleFiredNodig = true;
        rule.danVoor("    insert(new RuleFired(\"" + escapeRegelNaam(naam) + "\"));\n");
    }

    /** §8.1.9 the "regelversie X is gevuurd" reader's hoisted LHS existential — mirrors the "is inconsistent"
     *  {@code exists Inconsistentie( regel == "X" )}, keyed on the same escaped rule-name literal that
     *  {@link #markeerGevuurd} inserts. */
    private String ruleFiredGuard(String naam) {
        return "exists RuleFired( regel == \"" + escapeRegelNaam(naam) + "\" )";
    }

    /** §13.4.8 consistentieregel. The assertion MUST hold, so the rule fires on its
     *  VIOLATION — the negated condition — and inserts an Inconsistentie carrying the
     *  rule name and the offending subject (§9.5: "inconsistent" as the result, usable by
     *  other rules). A condition whose negation is not expressible (compound/predicate
     *  beyond leeg/gevuld) or an unresolved subject becomes an explicit marker. */
    private void consistentie(io.github.yhavinga.regelspraak.ir.ResolvedConsistentie c) {
        Acc acc = new Acc();
        Objecttype subject = c.subject();
        String constraint = subject != null ? schendingConstraint(acc, c.assertie(), subject) : null;
        Regel rule = new Regel(c.naam());
        if (constraint == null) {
            // The resolver judged this consistentie liftable, but its violation is not expressible — an
            // operand lowered to a marker (e.g. an unresolvable navigation / foreign-type right operand)
            // or the assertion has no faithful negation. The rule would be an inert eval(true) that flags
            // nothing, so surface the dropped check as a WARNING (matching the unlifted forms) instead of
            // letting it vanish silently. Deduped so a re-transpile of the same model does not pile up.
            var w = new RegelSpraakModel.Diagnostic(RegelSpraakModel.Severity.WARNING,
                    "consistentieregel '" + c.naam() + "' kon niet naar een schending-constraint worden omgezet"
                    + " (operand onresolveerbaar of geen getrouwe negatie); de regel controleert niets",
                    c.line());
            if (!model.diagnostics.contains(w)) model.diagnostics.add(w);
            // A window under GEEN or a counting quantifier is WF-112's position, not an assertion this branch
            // merely cannot negate — name it, in the sentence the exec-model branch gives.
            String vensterWeigering = Vensterpositie.weigering(c.assertie());
            rule.clausule("// onopgelost consistentieregel"
                    + (vensterWeigering == null ? "" : " (" + commentVeilig(vensterWeigering) + ")")
                    + ": " + commentVeilig(c.bron().snippet())).clausule("eval(true)");
            out.append(rule.render());
            return;
        }
        rule.clausule(SUBJECT + " : " + ident(subject.naam, true) + "( " + constraint + " )")
            .clausules(acc.clausules)
            .geldigheid(c.geldigheid())
            .clausule(inconsistentieGuard(c.naam()))
            .dan(inconsistentieDan(c.naam()));
        out.append(rule.render());
    }

    /** §8.1.6 uniciteitscontrole. "is uniek" is waar when the key is unique, so the rule is violated
     *  when two DISTINCT instances share it. The subject binds each non-null key part, and an
     *  accumulate counts the instances whose key equals it; more than one means a duplicate exists, so
     *  the subject is flagged with an Inconsistentie — once, regardless of how many instances share the
     *  key (an {@code exists}-with-{@code this != $subject} self-join is unreliable here: Drools does
     *  not exclude self inside {@code exists} for this shape, flagging even a unique instance). A null
     *  key cannot establish a duplicate (§8.1.6: two unknown identities are not the same object), so it
     *  is guarded out — the subject is matched only when its key is non-null, and a non-null "== $kN"
     *  never matches a null-keyed instance, so a null key is neither flagged nor counted as a match. */
    private void uniciteit(io.github.yhavinga.regelspraak.ir.ResolvedUniciteit u) {
        String type = ident(u.subject().naam, true);
        // ch. 5.9 key-SHAPE arity, the guard ref, clingo and the exec model hold: at most ONE temporal key
        // component. The text route has no fire moment to fault in, so it refuses in ITS loud idiom — an
        // onopgelost marker (a manifest MARKER + warning) rather than a join that pretends to lower a shape
        // all three executors reject.
        if (temporeleSleutels(u.subject(), u.sleutels()) > 1) {
            out.append(onopgelosteSleutelvorm(u.naam(), u.geldigheid()));
            return;
        }
        // IR-SPEC ch. 3.6: the scope field comes from scopeRol, which IS the scoping
        String scopeVeld = FactModelGenerator.scopeVeld(model, u);
        if (scopeVeld == null && u.scopeRol() != null) {
            out.append(new Regel(u.naam())
                .clausule("// onopgelost uniciteit (scopebinding onopgelost): " + commentVeilig(u.naam()))
                .clausule("eval(true)")
                .render());
            return;
        }
        StringBuilder binders = new StringBuilder();   // op de subject: "$k0 : p != null, …"
        StringBuilder matchers = new StringBuilder();  // in de accumulate: "p == $k0, …"
        if (scopeVeld != null) {                       // parent-scoped: count only within the subject's scope
            String sv = ident(scopeVeld, false);
            binders.append("$scope : ").append(sv).append(" != null, ");
            matchers.append(sv).append(" == $scope, ");
        }
        for (int i = 0; i < u.sleutels().size(); i++) {
            String prop = ident(sleutelVeld(u.subject(), u.sleutels().get(i)), false);
            binders.append("$k").append(i).append(" : ").append(prop).append(" != null");
            if (bevatNaam(u.subject().granulariteitLeden(), u.sleutels().get(i)))
                // ever-uniqueness (uchar RS ch. 5.9, both forms): a temporal key joins on the
                // EVER-value intersection — the same shared predicate the union emits
                matchers.append(fnImport("rsOoitBotst")).append("(").append(prop).append(", $k").append(i).append(")");
            else if (u.subject().attribuut(sleutelVeld(u.subject(), u.sleutels().get(i))) instanceof Datatype.Enumeratie)
                // an enumeration key is a declared name: two spellings of one label are ONE key (ref, clingo)
                matchers.append(fnImport("rsEnumNaam")).append("(").append(prop).append(") == ")
                        .append(fnImport("rsEnumNaam")).append("($k").append(i).append(")");
            else
                matchers.append(prop).append(" == $k").append(i);
            if (i < u.sleutels().size() - 1) { binders.append(", "); matchers.append(", "); }
        }
        out.append(new Regel(u.naam())
            .clausule(SUBJECT + " : " + type + "( " + binders + " )")
            .clausule("Number( intValue > 1 ) from accumulate( $o : " + type + "( " + matchers + " ), count($o) )")
            .geldigheid(u.geldigheid())
            .clausule(inconsistentieGuard(u.naam()))
            .dan(inconsistentieDan(u.naam()))
            .render());
    }

    /** §8.1.6 "verenigd met": uniqueness across the UNION of several sources' key spaces. One rule per
     *  source — a subject of that source is flagged when the total count, summed over EVERY source, of
     *  instances whose key equals the subject's exceeds one (so a value shared within one source or
     *  across two is caught). Keys join positionally (every source has the same arity). */
    private void uniciteitUnie(io.github.yhavinga.regelspraak.ir.ResolvedUniciteitUnie u) {
        var bronnen = u.bronnen();
        int arity = bronnen.get(0).sleutels().size();
        for (int s = 0; s < bronnen.size(); s++) {                   // the source whose instances this rule judges
            var subj = bronnen.get(s);
            // DRL rule names must be unique; the 1-based source index disambiguates even a same-type union.
            String ruleNaam = u.naam() + " (" + subj.type().naam + " " + (s + 1) + ")";
            // the same ch. 5.9 arity guard, per SOURCE (ref runs ever_tuples over each source's OWN key list).
            if (temporeleSleutels(subj.type(), subj.sleutels()) > 1) {
                out.append(onopgelosteSleutelvorm(ruleNaam, u.geldigheid()));
                continue;
            }
            StringBuilder binders = new StringBuilder();             // bind the subject's non-null key parts
            for (int i = 0; i < arity; i++) {
                if (i > 0) binders.append(", ");
                binders.append("$k").append(i).append(" : ")
                       .append(ident(sleutelVeld(subj.type(), subj.sleutels().get(i)), false)).append(" != null");
            }
            Regel rule = new Regel(ruleNaam)
                    .clausule(SUBJECT + " : " + ident(subj.type().naam, true) + "( " + binders + " )");
            StringBuilder som = new StringBuilder();
            for (int j = 0; j < bronnen.size(); j++) {               // one count per source, matched by value
                var b = bronnen.get(j);
                StringBuilder match = new StringBuilder();
                for (int i = 0; i < arity; i++) {
                    if (i > 0) match.append(", ");
                    String veld = ident(sleutelVeld(b.type(), b.sleutels().get(i)), false);
                    // each source declares its own key list, so the component's temporality is a property of
                    // the PAIR (this source's key, the subject source's key) — mirrors ExecModelTranspiler.
                    boolean elementTemporeel = bevatNaam(b.type().granulariteitLeden(), b.sleutels().get(i));
                    boolean subjectTemporeel = bevatNaam(subj.type().granulariteitLeden(), subj.sleutels().get(i));
                    if (elementTemporeel && subjectTemporeel)
                        // ever-uniqueness (uchar RS ch. 5.9): a temporal key joins on the EVER-value
                        // intersection, not on List identity (Segment has no equals — the bare == never
                        // fired, silently missing every collision)
                        match.append(fnImport("rsOoitBotst")).append("(").append(veld).append(", $k").append(i).append(")");
                    else if (elementTemporeel)
                        // MIXED: this source's timeline against the subject's plain value
                        match.append(fnImport("rsOoitBevat")).append("(").append(veld).append(", $k").append(i).append(")");
                    else if (subjectTemporeel)
                        // MIXED, the mirror: this source's plain value against the subject's timeline
                        match.append(fnImport("rsOoitBevat")).append("($k").append(i).append(", ").append(veld).append(")");
                    else if (b.type().attribuut(sleutelVeld(b.type(), b.sleutels().get(i))) instanceof Datatype.Enumeratie
                            || subj.type().attribuut(sleutelVeld(subj.type(), subj.sleutels().get(i))) instanceof Datatype.Enumeratie)
                        // an enumeration key is a declared name (WF-030 keeps the kind equal across the sources)
                        match.append(fnImport("rsEnumNaam")).append("(").append(veld).append(") == ")
                             .append(fnImport("rsEnumNaam")).append("($k").append(i).append(")");
                    else
                        match.append(veld).append(" == $k").append(i);
                }
                rule.clausule("$o" + j + " : Number() from accumulate( $i" + j + " : "
                        + ident(b.type().naam, true) + "( " + match + " ), count($i" + j + ") )");
                som.append(j > 0 ? " + " : "").append("$o").append(j).append(".intValue()");
            }
            rule.clausule("eval( " + som + " > 1 )")
                .geldigheid(u.geldigheid())
                .clausule(inconsistentieGuard(u.naam()))             // guard on the shared rule name
                .dan(inconsistentieDan(u.naam()));
            out.append(rule.render());
        }
    }

    /** A written uniqueness-key component brought back to its DECLARED spelling — the SAME shared name
     *  contract the temporality test above uses ({@link io.github.yhavinga.regelspraak.transpiler.kern.Namen}).
     *  The emitted property name is {@link #ident} of the DECLARATION (that is what the fact class carries),
     *  so a key written in another capitalisation must be resolved here or the rule names a field that does
     *  not exist. Mirrors {@code ExecModelTranspiler.gedeclareerdAttribuut}. */
    private static String sleutelVeld(Objecttype ot, String sleutel) {
        return ot == null ? sleutel : gedeclareerdeNaam(ot.attribuutLeden().keySet(), sleutel);
    }

    /** How many components of {@code sleutels} are temporal on {@code ot} — the ch. 5.9 key-SHAPE count,
     *  over the shared temporality test so the twin cannot read a key list differently than the exec model. */
    private static int temporeleSleutels(Objecttype ot, List<String> sleutels) {
        return (int) sleutels.stream().filter(k -> ot != null && bevatNaam(ot.granulariteitLeden(), k)).count();
    }

    /** The text route's refusal of an invalid ch. 5.9 key SHAPE: an inert rule carrying the {@code onopgelost}
     *  sentinel, so {@link #meet} books it as a MARKER with a reason and raises the warning — the twin of the
     *  exec model's fire-time vormfout, in the idiom this route actually has. */
    private String onopgelosteSleutelvorm(String naam, io.github.yhavinga.regelspraak.ir.Geldigheid g) {
        return new Regel(naam)
                .clausule("// onopgelost uniciteitscontrole (ch. 5.9: at most one temporal key component)")
                .clausule("eval(true)")
                .geldigheid(g)
                .render();
    }

    /** A "when" guard that makes an Inconsistentie-inserting rule IDEMPOTENT across fire cycles: it
     *  fires only while no Inconsistentie for this rule + subject already exists. Without it, a later
     *  insertion that changes a count/comparison re-activates the rule on an already-flagged subject
     *  (no-loop only blocks re-activation from the rule's OWN consequence), inserting a duplicate. */
    private String inconsistentieGuard(String naam) {
        return "not " + FACT_PACKAGE + ".Inconsistentie( regel == \"" + escapeRegelNaam(naam)
                + "\", subject == " + SUBJECT + " )";
    }

    /** The "when" constraint that detects a VIOLATION of the assertion. An ordering or
     *  date comparison is onwaar — hence inconsistent (§9.5) — not only when the
     *  complement holds but also when EITHER operand is empty (the typeringen leegwaarde
     *  tables: a leeg operand makes the comparison onwaar). The algebraic complement is
     *  likewise onwaar on null, so those operands are guarded explicitly. gelijk/ongelijk
     *  and leeg/gevuld use the plain negation (their empty cells are complementary).
     *  Returns null when no faithful negation exists — the caller marks the rule. */
    private String schendingConstraint(Acc acc, Conditie assertie, Objecttype subject) {
        if (assertie instanceof Conditie.Samengesteld s && s.kwantor() == Conditie.Kwantor.ALLE) {
            // §13.4.8: "alle criteria moeten gelden" — inconsistent when ANY criterion is violated.
            // OR the per-criterion violations; an un-lowerable criterion no-ops the whole rule.
            java.util.List<String> schendingen = new java.util.ArrayList<>();
            for (Conditie deel : s.onderdelen()) {
                String d = schendingConstraint(acc, deel, subject);
                if (d == null) return null;
                schendingen.add(d);
            }
            return schendingen.isEmpty() ? null : "(" + String.join(" || ", schendingen) + ")";
        }
        // ch. 3.10 a window criterium: violated where the window does not match. The window is total — an unset
        // rekendatum or a leeg bound puts the day outside it — so the plain negation of its constraint is exact.
        // Under GEEN or a counting quantifier it never gets here: that composite has no arm above, and the caller
        // names WF-112 for it.
        if (assertie instanceof Conditie.Periode per) {
            Frag venster = conditie(acc, per, subject);
            return venster.marker() ? null : "!( " + venster.text() + " )";
        }
        // §8.2 a whole-period-qualified criterium ("X moet gedurende het gehele jaar gelijk zijn aan Y"): the
        // criterium is satisfied iff rsGeheelPeriode(...) holds, so the VIOLATION is its negation — the same boolean
        // term as the voorwaarde form with the "== true|false" comparison FLIPPED (the NIET prefix flips it back).
        // A §8.2 form this transpiler cannot lower no-ops the rule (null), never flagging a never-evaluated criterium.
        Conditie.GeheelPeriode gp82 = geheelPeriodeVan(assertie);
        if (gp82 != null) {
            Frag f = geheelPeriodeCall(acc, assertie, gp82, subject);
            return f.marker() ? null : f.text() + " == " + (gp82.niet() ? "true" : "false");
        }
        // A comparison with a TO-ONE navigated operand cannot use the simple null-guard forms below: those
        // emit the operand via operand()'s Drools "!." which short-circuits the WHOLE term to a non-match on
        // a null reference, hiding a real gevuld-vs-leeg violation (p41). Handle it with explicit leegwaarde
        // guards per the typeringen rekentabellen instead.
        if (assertie instanceof Conditie.Vergelijking cmp) {
            String genav = genavigeerdeVergelijkingSchending(acc, cmp, subject);
            if (genav != null) return genav;
        }
        if (assertie instanceof Conditie.Vergelijking v && ordeningOfDatum(v.op())) {
            Frag l = operand(acc, v.links(), subject), r = operand(acc, v.rechts(), subject);
            // An un-evaluable operand (a /* TODO */ marker) must NOT be wrapped in this
            // null-guard: the guard's "<marker> == null" reads as "null == null" — always
            // true — so the rule would flag EVERY instance inconsistent though its criterion
            // was never evaluated (§9.5: inconsistent only on a real violation). Drop to the
            // no-op path instead, like an assertion with no faithful negation.
            if (l.marker() || r.marker()) return null;
            Frag complement = conditie(acc, new Conditie.Vergelijking(Operatoren.flip(v.op()), v.links(), v.rechts(), v.bron()), subject);
            return "(" + l.text() + " == null || " + r.text() + " == null || " + complement.text() + ")";
        }
        if (assertie instanceof Conditie.Vergelijking v && v.op() == Conditie.Operator.GELIJK
                && nietNumeriekVergelijking(v)) {
            // typeringen Tabel 38: a NON-numeriek "gelijk aan" with BOTH operands empty is "fout" — unlike the
            // numeriek Tabel 37 case, which is "waar". So the criterion is NOT satisfied -> a violation. The
            // plain complement `l != r` reads `null != null` -> false, wrongly reporting the instance consistent
            // (§9.5 reading: a criterion is satisfied only when its comparison is "waar"; "fout" is a violation).
            Frag l = operand(acc, v.links(), subject), r = operand(acc, v.rechts(), subject);
            if (l.marker() || r.marker()) return null;
            String[] o = vergelijkingsOperanden(l.text(), effType(v.links()), r.text(), effType(v.rechts()));
            return "(" + o[0] + " != " + o[1] + " || (" + l.text() + " == null && " + r.text() + " == null))";
        }
        Conditie schending = negeer(assertie);
        if (schending == null) return null;
        // Same hazard via the negation: a gevuld→leeg predicate over a marker emits
        // "<marker> == null" (always true). An un-evaluable criterion flags nothing.
        Frag c = conditie(acc, schending, subject);
        return c.marker() ? null : c.text();
    }

    /** A comparison whose operands are a RESOLVED non-numeriek type (Tekst/Datum/Boolean/Enumeratie). For
     *  these, typeringen Tabel 38 makes a both-empty "gelijk aan" evaluate to "fout"; the numeriek Tabel 37
     *  makes it "waar". An unresolved (Onbekend) operand is left to the numeriek-or-unknown default, so the
     *  both-empty special case never fires on a construct the resolver could not type. */
    private boolean nietNumeriekVergelijking(Conditie.Vergelijking v) {
        Datatype l = eff.typeVan(v.links()), r = eff.typeVan(v.rechts());
        return !l.isOnbekend() && !r.isOnbekend()
                && !l.isNumeriek() && !l.isPercentage() && !r.isNumeriek() && !r.isPercentage();
    }

    /**
     * A lowered VALUE / OPERAND fragment: the mvel term to render, plus whether it is an inert
     * {@code /* TODO … *​/} placeholder. A placeholder's {@link #text} is still compilable inert DRL
     * (a {@code null} value term, or — for a condition — a {@code false} constraint), so a marker renders
     * at the boundary exactly like a real fragment: it is a placeholder, not an absence. Callers branch on
     * the typed {@link #marker} flag instead of scanning the rendered text for the sentinel; every
     * combinator ORs the flag, so {@code marker} tracks "the text carries a placeholder" precisely (the
     * invariant that makes this a pure refactor: {@code marker} ⟺ the old {@code text.contains("/* TODO")}).
     */
    private record Frag(String text, boolean marker) {
        /** A fully-lowered fragment (no placeholder). */
        static Frag ok(String text) { return new Frag(text, false); }
        /** A value-side placeholder: {@code /* TODO wat: bron *​/ null}, an inert null term. */
        static Frag todo(String wat, String bron) { return new Frag(DrlTranspiler.todo(wat, bron), true); }
        /** As {@link #todo(String, String)} but from a structured {@link Bron} — its snippet is the text
         *  spliced into the placeholder, so the emitted DRL is byte-identical to the old String form. */
        static Frag todo(String wat, Bron bron) { return todo(wat, bron == null ? null : bron.snippet()); }
        /** A fragment assembled from {@code delen}: its marker is the OR of theirs (any placeholder
         *  operand taints the whole, exactly as the old substring scan would have detected it). */
        static Frag from(String text, Frag... delen) {
            boolean m = false;
            for (Frag d : delen) m |= d.marker;
            return new Frag(text, m);
        }
    }

    /** A comparison operand decomposed for a leegwaarde-faithful violation. {@code leeg}/{@code vol}
     *  are the "is empty"/"is present" guards; {@code waarde} is the value via a plain "." (safe ONLY
     *  behind {@code !leeg}, which short-circuits a null root). A navigated to-one operand uses an
     *  explicit root guard; a direct attribute / literal / parameter uses its plain operand() form. */
    private record OperandDeel(String leeg, String vol, String waarde, boolean genavigeerd, boolean marker) {}

    private OperandDeel operandDeel(Acc acc, Expr e, Objecttype subject) {
        if (e instanceof Expr.AttribuutRef a) {
            NaviRef n = naviResolve(a, subject);
            if (n.soort() == NaviSoort.GENAVIGEERD) {
                // §13.3.6/§8.2: a navigated TEMPORAL attribute is a List<Segment> — its leeg/vol form ("partner.attr == null")
                // would test the timeline's nullness, not its per-moment leegte, silently dropping the §8.2 period check.
                // Marker it (the leeg/vol/waarde text is unused on a marker; companionDeel/schending bail on .marker()).
                if (isTemporeelAttribuut(a)) return new OperandDeel(null, null, null, true, true);
                String wortel = ident(n.rol(), false), pad = wortel + "." + ident(a.attribuut(), false);
                return new OperandDeel("(" + wortel + " == null || " + pad + " == null)",
                        "(" + wortel + " != null && " + pad + " != null)", pad, true, false);
            }
        }
        Frag o = operand(acc, e, subject);   // direct attribute / literal / parameter — never "!."
        return new OperandDeel(o.text() + " == null", o.text() + " != null", o.text(), false, o.marker());
    }

    /** The §6.5/§6.6 fout-companion decomposition for an operand that is the subject's OWN attribute or one
     *  reached by a to-ONE navigation — the only shapes that lower to a re-readable leeg/vol guard a separate
     *  insert-only rule can re-test. null for a literal / parameter / to-many / unresolvable operand (which
     *  still degrades to leeg on the value side but raises no Inconsistentie). An own attribute reuses the bare
     *  property form, so the companion is byte-identical to the original EIGEN-only emission.
     *
     *  <p>ch. 4.10 moves the VALUE, not the duty to report it: writing a {@code Let} around a §6.5 site does not
     *  make the site a different site, so a BINDER operand stands for its need's own decomposition
     *  ({@link Acc#binderDeel}) — the same substitution {@link #takGarde} already makes on the guard side. A
     *  binder whose need does not decompose (or is not in scope here) is null like any other undecomposable
     *  operand, which drops the companion rather than guarding it wrongly. */
    private OperandDeel companionDeel(Acc acc, Expr e, Objecttype subject) {
        if (e instanceof Expr.BinderRef br) return acc.binderDeel.get(letSleutel(br.naam()));
        if (!(e instanceof Expr.AttribuutRef a)) return null;
        NaviSoort s = naviResolve(a, subject).soort();
        if (s != NaviSoort.EIGEN && s != NaviSoort.GENAVIGEERD) return null;
        OperandDeel d = operandDeel(acc, e, subject);
        return d.marker() ? null : d;
    }

    /** §13.3.9 to-one navigation + the typeringen leegwaarde rekentabellen (Tabel 37/38 gelijk,
     *  40-43 ordening, 44 ongelijk): build a consistentie violation for a comparison with at least one
     *  navigated to-one operand, treating a null root as the operand being LEEG. Drools "!." would make
     *  such an operand a silent non-match (under-reporting the violation, p41); these explicit guards
     *  short-circuit the null root before any plain "." dereference, so an empty operand yields the
     *  spec result. Returns null when NEITHER operand is navigated (the simpler forms handle it) or when
     *  an operand is an unresolved marker (the caller then WARNs). */
    private String genavigeerdeVergelijkingSchending(Acc acc, Conditie.Vergelijking v, Objecttype subject) {
        OperandDeel l = operandDeel(acc, v.links(), subject), r = operandDeel(acc, v.rechts(), subject);
        if (!l.genavigeerd() && !r.genavigeerd()) return null;
        if (l.marker() || r.marker()) return null;
        String[] ov = vergelijkingsOperanden(l.waarde(), effType(v.links()), r.waarde(), effType(v.rechts()));
        String lv = ov[0], rv = ov[1];
        if (ordeningOfDatum(v.op()))   // Tabel 40-43: any empty operand -> onwaar -> violation
            return "(" + l.leeg() + " || " + r.leeg() + " || " + lv + " " + Operatoren.symbool(Operatoren.flip(v.op())) + " " + rv + ")";
        if (v.op() == Conditie.Operator.GELIJK) {
            if (nietNumeriekVergelijking(v))   // Tabel 38: empty -> onwaar/fout -> always a violation
                return "(" + l.leeg() + " || " + r.leeg() + " || " + lv + " != " + rv + ")";
            // Tabel 37 (numeriek): BOTH empty is "waar" (consistent); only a one-sided empty (onwaar) or a
            // gevuld mismatch violates.
            return "((" + l.leeg() + " && " + r.vol() + ") || (" + r.leeg() + " && " + l.vol() + ") || ("
                    + l.vol() + " && " + r.vol() + " && " + lv + " != " + rv + "))";
        }
        if (v.op() == Conditie.Operator.ONGELIJK)   // Tabel 44: ongelijk is "waar" for gevuld-vs-empty, "onwaar"
            // for both-empty -> "moet ongelijk zijn" is violated by both-empty OR a gevuld match.
            return "((" + l.leeg() + " && " + r.leeg() + ") || (" + l.vol() + " && " + r.vol() + " && " + lv + " == " + rv + "))";
        return null;
    }

    /** The leegwaarde-faithful POSITIVE (voorwaarde) truth value of a comparison with a TO-ONE navigated operand —
     *  exactly the complement of {@link #genavigeerdeVergelijkingSchending} (the rule fires when the comparison is WAAR;
     *  the consistentie rule fires when it is VIOLATED, i.e. onwaar/fout). A navigated operand otherwise renders via
     *  operand()'s mvel {@code !.}, which short-circuits the whole constraint to a non-match on a null relation —
     *  spec-correct for ordering/GELIJK (a leeg operand is onwaar) but WRONG for ONGELIJK (§5.6 Tabel 44: a leeg-vs-gevuld
     *  "ongelijk aan" is WAAR). Reuses the schending form's explicit leeg/vol/waarde guards (operandDeel) + unit
     *  restatement, so the two stay in lock-step. null when neither operand is navigated (the plain mvel form applies) or
     *  an operand markers. Mirrors the exec navigated-comparison branches (a null relation -> leeg -> vergelijk). */
    private String genavigeerdeVergelijkingVoorwaarde(Acc acc, Conditie.Vergelijking v, Objecttype subject) {
        // Only GELIJK and ONGELIJK need the explicit form: ordering (<,>,<=,>=) of a leeg operand is onwaar (Tabel 33-42),
        // which mvel's `!.` short-circuit ALREADY gives correctly — keep its compact `partner!.x op y` rendering. GELIJK
        // both-leeg -> WAAR (Tabel 37) and ONGELIJK one-leeg -> WAAR (Tabel 44) are the cells `!.` gets wrong.
        if (v.op() != Conditie.Operator.GELIJK && v.op() != Conditie.Operator.ONGELIJK) return null;
        String schending = genavigeerdeVergelijkingSchending(acc, v, subject);
        return schending == null ? null : "!" + schending;
    }

    private static boolean ordeningOfDatum(Conditie.Operator op) {
        return switch (op) {
            case KLEINER, KLEINER_OF_GELIJK, GROTER, GROTER_OF_GELIJK,
                 EERDER, EERDER_OF_GELIJK, LATER, LATER_OF_GELIJK -> true;
            case GELIJK, ONGELIJK -> false;
        };
    }

    /** The §4.2 validity guard: an eval over the Rekendatum global that fires only inside the
     *  version's window. Null-guarded so an unset Rekendatum simply doesn't match (no NPE), and
     *  the inclusive bounds use isBefore/isAfter negations (LocalDate has no &lt;= operator). */
    private static String geldigheidGuard(io.github.yhavinga.regelspraak.ir.Geldigheid g) {
        StringJoiner sj = new StringJoiner(" && ", "eval( ", " )");
        sj.add("rekendatum != null");
        if (g.vanaf() != null) sj.add("!rekendatum.isBefore(" + datumLit(g.vanaf()) + ")");
        if (g.tot() != null)   sj.add("!rekendatum.isAfter(" + datumLit(g.tot()) + ")");
        return sj.toString();
    }
    private static String datumLit(java.time.LocalDate d) {
        return "java.time.LocalDate.of(" + d.getYear() + ", " + d.getMonthValue() + ", " + d.getDayOfMonth() + ")";
    }
    private static boolean tijdgebondenGeldigheid(io.github.yhavinga.regelspraak.ir.Geldigheid g) {
        return g != null && !g.altijd();
    }

    /** The negation of a consistency assertion — what makes the rule fire (the
     *  violation). A comparison flips to its complement; an emptiness predicate flips
     *  leeg↔gevuld; anything else has no clean single-constraint negation here and yields
     *  null so the caller marks the rule rather than emit a wrong trigger. */
    private static Conditie negeer(Conditie c) {
        if (c instanceof Conditie.Vergelijking v)
            return new Conditie.Vergelijking(Operatoren.flip(v.op()), v.links(), v.rechts(), v.bron());
        if (c instanceof Conditie.Predicaat p) {
            Conditie.Predicaatsoort neg = switch (p.soort()) {
                case LEEG -> Conditie.Predicaatsoort.GEVULD;
                case GEVULD -> Conditie.Predicaatsoort.LEEG;
                default -> null;
            };
            return neg != null ? new Conditie.Predicaat(neg, p.operand(), p.bron()) : null;
        }
        return null;
    }

    /** Splice a §13.4.10 verdeling voorwaarde into a bron pattern's constraint list: an empty voorwaarde leaves the
     *  base constraint byte-identical (the pre-voorwaarde output), otherwise the two are comma-joined. */
    private static String constraintMet(String basis, String voorwaarde) {
        if (voorwaarde.isEmpty()) return basis;
        return basis.isEmpty() ? voorwaarde : basis + ", " + voorwaarde;
    }

    /** §13.4.10 verdeling. Two methods have a single forward-rule form: EQUAL SHARES binds the source,
     *  counts the recipients joined to it, and gives each total/count; NAAR RATO (proportional) instead
     *  SUMS each recipient's weight attribute and gives each total·weight/sum. Both fold the recipients in
     *  one accumulate. The group/ordering/remainder schemes (COMPLEX) are multi-pass allocations with no
     *  single-rule form — and an unresolvable join or a weight that is not a real recipient field — stay an
     *  explicit no-op marker. The join is {@link FactModelGenerator#verdelingsjoin}'s: the wire's recipient
     *  binding decides it, the type pair only where the verdeling carries none (V1-WIREMIGRATIE P3). */
    private void verdeling(io.github.yhavinga.regelspraak.ir.ResolvedVerdeling v) {
        Objecttype bron = v.bronType(), doel = v.doelType();
        FactModelGenerator.Verdelingsjoin vj = FactModelGenerator.verdelingsjoin(model, v, rolBindingen.get(v));
        if (vj.weigering() != null) {
            out.append(new Regel(v.naam())
                .clausule("// onopgelost verdeling (" + commentVeilig(vj.weigering()) + "): " + commentVeilig(v.naam()))
                .clausule("eval(true)")
                .render());
            return;
        }
        String join = vj.rol();
        var methode = v.methode();
        // The share is numeric (a division/fold of the verdeelplafond), so the recipient attribute must be
        // getalachtig — else the modify would assign a BigDecimal to a Tekst/Datum setter, uncompilable DRL that
        // sinks the whole package. A non-numeric doel stays an honest marker (the resolver also flags the family
        // mismatch, the same fout regel raises for the equivalent assignment).
        Datatype doelType = doel != null && v.doelAttribuut() != null ? doel.attribuut(v.doelAttribuut()) : null;
        boolean doelOk = doelType != null && doelType.isGetalachtig();
        boolean gelijk = methode == io.github.yhavinga.regelspraak.ir.ResolvedVerdeling.Methode.GELIJKE_DELEN && join != null && doelOk;
        // NAAR RATO needs the join AND a weight that is a real GETALACHTIG (numeriek/percentage) field of the
        // recipient: heeftLid alone would also accept a Tekst attribute or a boolean kenmerk, whose getter
        // cannot be summed/multiplied — uncompilable DRL that sinks the whole package. A non-numeric, foreign,
        // or absent weight therefore stays the honest no-op marker.
        Datatype gewichtType = doel != null && v.ratoAttribuut() != null ? doel.attribuut(v.ratoAttribuut()) : null;
        boolean rato = methode == io.github.yhavinga.regelspraak.ir.ResolvedVerdeling.Methode.NAAR_RATO && join != null && doelOk
                && gewichtType != null && gewichtType.isGetalachtig();
        // §9.7.2 op volgorde van: the recipients are partitioned by an ORDERABLE ordering key (a number or a
        // date — rsVerdeelOpVolgorde compareTo-sorts it) and the pot is depleted group-by-group; a "naar rato"
        // tie additionally needs a getalachtig tie weight (it is summed). Anything else stays the marker.
        Datatype groepType = methode == io.github.yhavinga.regelspraak.ir.ResolvedVerdeling.Methode.OP_VOLGORDE
                && doel != null && v.groepeerAttribuut() != null ? doel.attribuut(v.groepeerAttribuut()) : null;
        boolean opVolgorde = methode == io.github.yhavinga.regelspraak.ir.ResolvedVerdeling.Methode.OP_VOLGORDE && join != null && doelOk
                && groepType != null && groepType.isOrdenbaar()
                && (v.ratoAttribuut() == null || (gewichtType != null && gewichtType.isGetalachtig()));
        if (!gelijk && !rato && !opVolgorde) {
            out.append(new Regel(v.naam())
                .clausule("// onopgelost verdeling ("
                        + (bron == null || doel == null ? "referentie" : methode.name().toLowerCase())
                        + "): " + commentVeilig(v.bronAttribuut() + " over " + v.doelAttribuut()))
                .clausule("eval(true)")
                .render());
            return;
        }
        // §13.3.9: `join` is a derived ROLE, and every recipient pattern below reads it as a FIELD. Measured
        // 2026-09-11 on a verifier-clean, ref-clean model whose recipient role is to-many (the storage sits at the
        // source end): this branch emitted `berging == $src` and the package died on "Unable to Analyse Expression
        // berging == $src". The exec branch refuses the same shape by name; both must say the same words.
        if (!navVeld("verdeling", doel, join, bron)) {
            out.append(new Regel(v.naam())
                .clausule("// onopgelost verdeling (opslagrichting): " + commentVeilig(v.naam()))
                .clausule("eval(true)")
                .render());
            return;
        }
        // §13.4.10: the verdeling voorwaarde gates the WHOLE lowered family (the share rule, the §9.7.5 onverdeelde
        // rest AND the §9.7.6 Fout companions) — a false gate must write NOTHING (not a share, not a rest). Lower it
        // to an in-pattern bron constraint spliced into every rule's bron pattern. A voorwaarde that needs a hoisted
        // join/aggregation has no in-pattern form here, so the whole verdeling is an honest no-op marker rather than
        // distribute unconditionally (mirrors the exec-model skip).
        Acc vwAcc = new Acc();
        String vw = "";
        if (v.voorwaarde() != null) {
            Frag vwFrag = conditieInline(vwAcc, v.voorwaarde(), bron, true);
            if (vwFrag.marker() || !vwAcc.clausules.isEmpty()) {
                // A window under GEEN or a counting quantifier is WF-112's position: this emitter writes its own
                // marker over the condition's, so the rule is named here, in the sentence the exec branch gives.
                String vensterWeigering = Vensterpositie.weigering(v.voorwaarde());
                out.append(new Regel(v.naam())
                    .clausule("// onopgelost verdeling ("
                            + (vensterWeigering == null ? "voorwaarde" : commentVeilig(vensterWeigering)) + "): "
                            + commentVeilig(v.naam()))
                    .clausule("eval(true)")
                    .render());
                return;
            }
            vw = vwFrag.text();
        }
        String j = ident(join, false) + " == $src";
        // a §9.7.6 empty-weight Fout applies whenever a naar-rato split is present (top-level OR the op-volgorde tie)
        boolean tieNaarRato = rato || (opVolgorde && v.ratoAttribuut() != null);
        // emit the share rule; both builders return the recipient filter the onverdeelde-rest rule reuses
        String doelFilter = opVolgorde ? opVolgordeRegel(v, bron, doel, j, vw)
                                       : gelijkOfRatoRegel(v, bron, doel, j, gelijk, vw);
        if (v.restantAttribuut() != null) {
            // §9.7.5 onverdeelde rest: the verdeler keeps total − Σ(distributed shares). A SEPARATE rule that
            // modifies the SOURCE (not the recipients), so it cannot cancel the per-recipient activations. It
            // reads the shares the distribution already assigned, so it is method-agnostic (equal-shares and
            // naar-rato alike) and needs no recompute. It is RECOMPUTABLE, not fire-once: a `restant == null`
            // freeze would lock in a partial sum when recipients arrive incrementally (the §9.4 rule-derived
            // case — the `not` guard only sees recipients already in working memory, so a single early recipient
            // looks "complete"). Instead it re-fires whenever the share set changes and always reflects the
            // CURRENT full distribution. The `not <Doel>(<filter>, <doel> == null)` guard still keeps it from
            // summing a recipient mid-assignment; termination holds because no other rule reads restant, so its
            // modify does not re-trigger the share rule (property reactivity) and the recomputation is a fixpoint.
            String doelP = ident(doel.naam, true), doelN = ident(v.doelAttribuut(), false);
            out.append(new Regel(v.naam() + " (onverdeelde rest)")
                .clausule("$src : " + ident(bron.naam, true) + "( " + constraintMet(ident(v.bronAttribuut(), false) + " != null", vw) + " )")
                .clausule("exists " + doelP + "( " + doelFilter + " )")
                .clausule("not " + doelP + "( " + doelFilter + ", " + doelN + " == null )")
                .clausule("accumulate( $u : " + doelP + "( " + doelFilter + ", " + doelN
                        + " != null ) ; $uit : sum($u." + getter(v.doelAttribuut()) + "()) )")
                .geldigheid(v.geldigheid())
                .dan("    modify($src) {\n        " + setter(v.restantAttribuut()) + "( $src."
                        + getter(v.bronAttribuut()) + "().subtract($uit) )\n    }\n")
                .render());
        }
        // §9.7.6 Tabel 19 Fouten, surfaced on the §9.5 channel by SEPARATE modify-free rules (a `not`/accumulate
        // in the share rule would cancel its sibling recipient activations). The fact lands on each offending
        // RECIPIENT — rule × subject, ch. 5.6 — as RUNTIME-SEMANTICS ch. 4.2/4.3 give it and ref and clingo
        // deposit it; the rule header is suffixed for a unique DRL name while `regel` stays the verdeling's.
        // The recipient set is ref's: an empty source amount distributes nothing and flags nothing, and under
        // naar rato a cap is only read for a weighted recipient of a split whose weight sum is non-zero.
        // Until 2026-09-23 both lay on the SOURCE, once per bron.
        String srcFout = "$src : " + ident(bron.naam, true) + "( " + constraintMet(ident(v.bronAttribuut(), false)
                + " != null", vw) + " )";
        String doelP = ident(doel.naam, true);
        if (tieNaarRato) {
            String wNaam = ident(v.ratoAttribuut(), false);
            out.append(new Regel(v.naam() + " (leeg gewicht)")
                .clausule(srcFout)
                .clausule(SUBJECT + " : " + doelP + "( " + j + ", " + wNaam + " == null )")
                .geldigheid(v.geldigheid())
                .clausule(inconsistentieGuard(v.naam()))
                .dan(inconsistentieDan(v.naam()))
                .render());
        }
        if (v.maximumAttribuut() != null) {
            // The value side already degrades a leeg/negative cap to uncapped (rsPlafond for naar rato,
            // rsVerdeelOpVolgorde for op volgorde), so no negative share is produced.
            String mNaam = ident(v.maximumAttribuut(), false);
            Regel r = new Regel(v.naam() + " (leeg of negatief maximum)").clausule(srcFout);
            String gewogen = "";
            if (rato) {
                String wNaam = ident(v.ratoAttribuut(), false);
                gewogen = ", " + wNaam + " != null";
                r.clausule("accumulate( $c : " + doelP + "( " + j + gewogen + " ) ; $sum : sum($c."
                                + getter(v.ratoAttribuut()) + "()) )")
                 .clausule("eval( $sum.signum() != 0 )");
            }
            out.append(r
                .clausule(SUBJECT + " : " + doelP + "( " + j + gewogen + ", " + mNaam + " == null || " + mNaam
                        + ".signum() < 0 )")
                .geldigheid(v.geldigheid())
                .clausule(inconsistentieGuard(v.naam()))
                .dan(inconsistentieDan(v.naam()))
                .render());
        }
    }

    /** §13.4.10 GELIJKE_DELEN / NAAR_RATO: one accumulate folds the recipients (count, or sum-of-weights); each
     *  gets total/count or total·weight/Σweight, then the §9.7.3 cap (rsPlafond, naar-rato only) and the §9.7.4
     *  floor (rsAfronding). Returns the recipient filter the onverdeelde-rest rule reuses. */
    private String gelijkOfRatoRegel(io.github.yhavinga.regelspraak.ir.ResolvedVerdeling v, Objecttype bron, Objecttype doel,
                                     String j, boolean gelijk, String voorwaarde) {
        Regel rule = new Regel(v.naam()).clausule("$src : " + ident(bron.naam, true) + "( " + constraintMet("", voorwaarde) + " )");
        String aandeel;       // the per-recipient value set into the modify
        String doelFilter;    // the recipient pattern's constraints (reused by the onverdeelde-rest rule)
        if (gelijk) {
            doelFilter = j;
            rule.clausule("accumulate( $c : " + ident(doel.naam, true) + "( " + doelFilter + " ) ; $n : count($c) )")
                .clausule("$r : " + ident(doel.naam, true) + "( " + doelFilter + " )");
            aandeel = "$src." + getter(v.bronAttribuut()) + "().divide($n, java.math.MathContext.DECIMAL64)"; // safe division
        } else {
            // §13.4.10 naar rato: each recipient gets total·weight/Σweight. The accumulate sums the weights of
            // the recipients that HAVE one, and $r binds the same set, so a weightless recipient is left unset
            // (its weight is null — there is no proportional basis) while the weighted ones share the total.
            // §9.7.6 Tabel 19 makes an empty weight on any recipient a "Fout" for the WHOLE verdeling: the Fout
            // is now surfaced as a SEPARATE Inconsistentie rule (below), since it cannot ride on this rule — a
            // second accumulate / `not` in a modify+no-loop rule cancels the sibling recipient activations so
            // only one recipient fires (verified vs Drools 8.44). DIVERGENCE that remains: the distribution is
            // NOT suppressed (the weighted recipients still receive shares) — expressing "no shares when any
            // weight is empty" needs exactly the CE that triggers the cancellation, so it is unsolved; the
            // exclude-and-redistribute split stays, now flagged by the Inconsistentie. Documented + carded. A
            // zero total weight (all weights 0) is guarded so the divide cannot throw — the spec leaves a zero
            // divisor undefined, so no-op-not-crash is the safe reading.
            String wNaam = ident(v.ratoAttribuut(), false), wGet = getter(v.ratoAttribuut());
            doelFilter = j + ", " + wNaam + " != null";
            rule.clausule("accumulate( $c : " + ident(doel.naam, true) + "( " + doelFilter
                            + " ) ; $sum : sum($c." + wGet + "()) )")
                .clausule("$r : " + ident(doel.naam, true) + "( " + doelFilter + " )")
                .clausule("eval( $sum.signum() != 0 )");
            aandeel = "$src." + getter(v.bronAttribuut()) + "().multiply($r." + wGet
                    + "()).divide($sum, java.math.MathContext.DECIMAL64)";
        }
        if (v.maximumAttribuut() != null) {
            // §9.7.3 maximale aanspraak (naar rato only): cap each share at the recipient's maximum — the surplus
            // is NOT redistributed, it falls to the onverdeelde rest (the rest rule reads the capped shares). The
            // cap precedes the §9.7.4 afronding ("als laatste … afgerond"). rsPlafond null-guards both sides (a
            // recipient without a maximum is uncapped).
            fnImport("rsPlafond");
            aandeel = "rsPlafond(" + aandeel + ", $r." + getter(v.maximumAttribuut()) + "())";
        }
        if (v.afrondingDecimalen() != null) {
            // §9.7.4 / §6.1.2: floor each share to N decimals ("naar beneden") — the explicit afronding the spec
            // requires before a division result may land in a bounded target. rsAfronding null-guards the basis.
            fnImport("rsAfronding");
            aandeel = "rsAfronding(" + aandeel + ", " + v.afrondingDecimalen() + ", java.math.RoundingMode.FLOOR)";
        }
        out.append(rule.geldigheid(v.geldigheid())
            .dan("    modify($r) {\n        " + setter(v.doelAttribuut()) + "( " + aandeel + " )\n    }\n")
            .render());
        return doelFilter;
    }

    /** §9.7.2 op volgorde van: one forward rule whose accumulate folds the recipients into index-aligned value
     *  lists (the fact itself, for an identity key; the ordering criterium; and — when present — the tie weight
     *  and the cap), hands them to rsVerdeelOpVolgorde (which computes the whole ordered allocation as one
     *  identity-keyed share map), and assigns each recipient its share via {@code $shares.get($r)}. The §9.7.3 cap
     *  and §9.7.4 floor are applied INSIDE the helper, group by group, so the §9.7.5 carry-over of the remainder
     *  reflects the capped+floored shares (Tabel 18) — not a post-hoc clamp against the full plafond. {@code from}
     *  a Map binds it once (a Map is not a Collection, so it is not iterated). Returns the recipient filter (the
     *  bare join — every recipient is collected) for the onverdeelde-rest rule. */
    private String opVolgordeRegel(io.github.yhavinga.regelspraak.ir.ResolvedVerdeling v, Objecttype bron, Objecttype doel, String j, String voorwaarde) {
        fnImport("rsVerdeelOpVolgorde");
        String doelP = ident(doel.naam, true);
        StringBuilder acc = new StringBuilder("$facts : collectList($p), $groep : collectList($p."
                + getter(v.groepeerAttribuut()) + "())");
        String gewArg = "null", maxArg = "null";
        if (v.ratoAttribuut() != null) {   // a naar-rato tie ("bij een even groot criterium naar rato van <gewicht>")
            acc.append(", $gewicht : collectList($p.").append(getter(v.ratoAttribuut())).append("())");
            gewArg = "$gewicht";
        }
        if (v.maximumAttribuut() != null) {
            acc.append(", $max : collectList($p.").append(getter(v.maximumAttribuut())).append("())");
            maxArg = "$max";
        }
        String dArg = v.afrondingDecimalen() != null ? "Integer.valueOf(" + v.afrondingDecimalen() + ")" : "null";
        out.append(new Regel(v.naam())
            .clausule("$src : " + ident(bron.naam, true) + "( " + constraintMet("", voorwaarde) + " )")
            .clausule("accumulate( $p : " + doelP + "( " + j + " ) ; " + acc + " )")
            .clausule("$shares : java.util.Map() from rsVerdeelOpVolgorde( $src." + getter(v.bronAttribuut())
                    + "(), $facts, $groep, " + (v.afnemend() ? "false" : "true") + ", " + gewArg + ", " + maxArg
                    + ", " + dArg + " )")
            .clausule("$r : " + doelP + "( " + j + " )")
            .geldigheid(v.geldigheid())
            .dan("    modify($r) {\n        " + setter(v.doelAttribuut())
                    + "( (java.math.BigDecimal) $shares.get($r) )\n    }\n")
            .render());
        return j;
    }

    /** §13.4.6 objectcreatie -> a rule that inserts a new fact, sets its attributes,
     *  and links it to the subject through the feittype role.
     *
     *  <p>TWO deliberate departures from the ported production lowering, both implementing
     *  {@code RUNTIME-SEMANTICS.md} ch. 5.5 where production does not — the spec is the product, and the exec-model
     *  branch has implemented the same two since its own creation card (card drl-objectcreatie-defect, 2026-09-19,
     *  which measured what production answers on four corpus goldens: two read leeg, two faulted at fire):
     *  <ul>
     *    <li><b>Both ends</b> of the back-link are written — the created object's reference to the subject AND,
     *        on the 1:1 arm, the subject's reference to the mint. Production writes only the first, so a fold
     *        over the created type finds the mint and a to-one navigation from the subject reads leeg.</li>
     *    <li>A <b>kenmerkwaardetoekenning</b> (§13.4.6.4) sets the mark only when its value is true; false or
     *        empty leaves it unset, instead of unboxing a leeg into the primitive boolean field.</li>
     *  </ul>
     *
     *  <p>§9 idempotence: the creation must be a FUNCTION of the fact base, not of the fire count. {@code no-loop}
     *  only blocks the rule re-activating on its OWN insertion — when ANOTHER rule later modifies a matched
     *  subject the unguarded rule re-fires and mints a DUPLICATE instance. So the LHS carries an absence guard:
     *  <ul>
     *    <li><b>1:1 target</b> (the subject relates to at most ONE created-type instance): {@code <fwdRef> == null}
     *        on the subject pattern — if the subject's forward link is already filled (whoever filled it, and ch.
     *        5.7a's case input fills both ends), create no other. The exec branch reads the same field.</li>
     *    <li><b>to-many target</b>: a per-rule per-subject
     *        {@link io.github.yhavinga.regelspraak.runtime.CreationMarker} control fact — {@code not( CreationMarker(
     *        regel == "<rule>", subject == $subject ) )} on the LHS, {@code insert(new CreationMarker(...))} in
     *        the consequence. Each subject then creates at most one instance per creation rule, while a
     *        pre-existing linked instance stays legitimate (the key is rule+subject, not the instance).</li>
     *  </ul> */
    private void objectCreatie(io.github.yhavinga.regelspraak.ir.ResolvedObjectCreatie oc) {
        Acc acc = new Acc();
        Objecttype nieuw = oc.nieuwType(), subject = oc.subject();
        // value expressions may carry aggregations -> collected into the "when" via the threaded Acc
        // context (so lower them BEFORE reading acc.clausules into the rule, even on the no-op path).
        java.util.List<String> setters = new ArrayList<>();
        for (var t : oc.toekenningen()) {
            Frag v = waarde(acc, t.waarde(), subject);
            // §6.1.2 write boundary (as in regel()): a COMPUTED numeric value (arithmetic / non-terminating rational)
            // collapses through rsWaarde so a Breuk never reaches a BigDecimal setter (mvel strict-mode) — inert for a
            // plain BigDecimal, skipped for a marker / a non-Breuk-capable value.
            if (kanBreukOpleveren(t.waarde()) && !v.marker()) { fnImport("rsWaarde"); v = Frag.from("rsWaarde(" + v.text() + ")", v); }
            // §13.4.6.4 kenmerkwaardetoekenning, RUNTIME-SEMANTICS ch. 5.5: the mark is SET when its value
            // evaluates true and, evaluating FALSE OR EMPTY, is left unset — an absent initialisation under the
            // closed-world reading a kenmerkcheck applies. The bean field is a PRIMITIVE boolean whose default
            // IS unset, so "leave it" is a skipped write. The ported production lowering wrote the value through
            // unconditionally, and a LEEG one then unboxed null into that field: a NullPointerException inside
            // the consequence, which takes the whole fireAllRules pass down (measured 2026-09-19 on the anchor
            // rekenkamer-overzee and synth3-kamerheffing cases whose subject carries no mark). The guard is one
            // rsKenmerkWaarde call, which evaluates the value once and writes the literal true only when it holds.
            // Any value other than true, false or leeg is WF-027: ref faults on it and so does the exec branch, so
            // the helper throws rather than reading it as "unset" (an Objects.equals(v, true) guard did, silently).
            if (t.kenmerk() && !v.marker())
                setters.add("    if (" + fnImport("rsKenmerkWaarde") + "( " + v.text() + ", "
                        + drlString(t.attribuut()) + " )) {\n"
                        + "        $new." + setter(t.attribuut()) + "( true );\n    }");
            else
                setters.add("    $new." + setter(t.attribuut()) + "( " + v.text() + " );");
        }

        if (subject == null || nieuw == null) {
            out.append(new Regel(oc.naam())
                .clausule("// onopgelost objectcreatie: " + commentVeilig(oc.naam()))
                .clausule("eval(true)")
                .render());
            return;
        }
        // ch. 5.5's two link roles, resolved exactly as ExecModelTranspiler.objectCreatie resolves them: the
        // creation's own verbindendeRol binding first (it names the role the CREATED object plays; the subject plays
        // its sibling), and only without one the by-pair derivation. On a repeated or self-typed pair the by-pair
        // derivation answers null by construction, so the binding is the only route there is — and without it this
        // branch used to emit an insert with no link at all, left to the marker scan in meet() to refuse.
        String backRol, fwdRol;                                  // subject-side (field on $new) / created-side (field on $subject)
        boolean backVeel, fwdVeel;
        RegelSpraakModel.RolBindingRef vr = rolBindingen.get(oc);
        if (vr != null) {
            RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, vr);
            if (paar == null) {
                creatieGeweigerd(oc, "de connectingRole-binding '" + vr.feittype() + "." + vr.rol()
                        + "' wijst geen rolpaar aan");
                return;
            }
            fwdRol = paar[0].naam(); fwdVeel = paar[0].meervoudig();
            backRol = paar[1].naam(); backVeel = paar[1].meervoudig();
        } else {
            backRol = model.verbindendeRol(nieuw, subject);
            fwdRol = model.verbindendeRol(subject, nieuw);
            backVeel = backRol != null && model.naarVeelRol(nieuw, subject, backRol);
            fwdVeel = fwdRol != null && model.naarVeelRol(subject, nieuw, fwdRol);
        }
        // Only a to-ONE role has a bean field (FactModelGenerator.leden), and only a TYPED one is writable: a
        // declared member of the same name displaces the reference. A displaced end is refused by name, as in
        // the exec branch — writing only the other end would leave ch. 5.5's link silently one-sided.
        String backRef = backRol != null && !backVeel ? backRol : null;
        String fwdRef = fwdRol != null && !fwdVeel ? fwdRol : null;
        if (backRef != null && !FactModelGenerator.heeftReferentieVeld(model, nieuw, backRef, subject)) {
            creatieGeweigerd(oc, FactModelGenerator.geenReferentieveldReden(nieuw, backRef, subject));
            return;
        }
        if (fwdRef != null && !FactModelGenerator.heeftReferentieVeld(model, subject, fwdRef, nieuw)) {
            creatieGeweigerd(oc, FactModelGenerator.geenReferentieveldReden(subject, fwdRef, nieuw));
            return;
        }
        // No writable end: the mint would be reachable from nothing. The three causes are named apart, in the
        // exec branch's words. An ambiguous pair is a fault at fire over there (ch. 5.5 refuses to guess); this
        // branch has no fault-rule idiom and refuses it by the same sentence instead.
        if (backRef == null && fwdRef == null) {
            creatieGeweigerd(oc, vr == null && subject != nieuw && verbindendeFeittypen(subject, nieuw) > 1
                    ? "more than one fact type relates \"" + subject.naam + "\" to \"" + nieuw.naam
                      + "\"; the created object's back-link is ambiguous (ch. 5.5)"
                    : (vr == null && subject == nieuw
                    ? "zelfgetypeerd rollenpaar zonder connectingRole-binding: uit twee gelijkgetypeerde rollen leidt"
                      + " de bean geen referentieveld af en de draad wijst geen rol aan"
                    : vr == null && (backRol == null || fwdRol == null)
                    ? "geen uniek verbindend feittype: de terugkoppeling is niet af te leiden en ch. 5.5 gist niet"
                    : "beide rollen zijn meervoudig ('" + backRol + "' en '" + fwdRol + "'), dus FactModelGenerator"
                      + " leidt aan geen van beide zijden een referentieveld af")
                    + ", dus het gecreëerde object zou onbereikbaar zijn");
            return;
        }
        // §9 guard shape: with a binding the cardinality is the bound role's own, without one the by-pair reading.
        // Either way a 1:1 arm must HAVE the forward field its guard reads — checked, as in the exec branch.
        boolean eenOpEen = vr != null ? !fwdVeel : !model.subjectNaarVeelCreatie(subject, nieuw);
        if (eenOpEen != (fwdRef != null))
            throw new IllegalStateException("objectcreatie " + oc.naam() + ": subjectNaarVeelCreatie(" + eenOpEen
                    + ") en het voorwaartse veld (" + fwdRef + ") spreken elkaar tegen");

        StringBuilder dan = new StringBuilder();
        dan.append("    ").append(ident(nieuw.naam, true)).append(" $new = new ")
           .append(ident(nieuw.naam, true)).append("();\n");
        for (String s : setters) dan.append(s).append('\n');
        if (backRef != null)
            dan.append("    $new.").append(setter(backRef)).append("( ").append(SUBJECT).append(" );\n");
        dan.append("    insert($new);\n");

        // §9 idempotence guard (see method doc). The 1:1 arm reads the SUBJECT's forward field, the one the
        // consequence fills — the exec branch's reading, and the only one there is when the subject-side role is
        // to-many and the mint carries no back-reference. A to-many target keys on a per-rule per-subject
        // CreationMarker.
        String idemGuard;
        if (eenOpEen) {
            idemGuard = null;
        } else {
            creationMarkerNodig = true;
            idemGuard = "not CreationMarker( regel == \"" + escapeRegelNaam(oc.naam())
                    + "\", subject == " + SUBJECT + " )";
            dan.append("    insert(new CreationMarker(\"").append(escapeRegelNaam(oc.naam())).append("\", ")
               .append(SUBJECT).append("));\n");
        }
        // RUNTIME-SEMANTICS ch. 5.5: new object and subject are linked over the back-link relation — BOTH ENDS,
        // so a to-one navigation FROM the subject reaches the mint. The ported production lowering writes only
        // the created object's back-reference above: a fold over the created type then finds the mint while
        // "de heffing van haar rekenkamer" reads leeg, and the subject's own attribute — computed from that
        // navigation — stays empty with no complaint at all. The modify is required, not cosmetic: $subject is
        // an already-matched fact, so without it every rule that navigates to the mint keeps its pre-creation
        // match. It cannot re-mint — no-loop blocks the self-retrigger and the 1:1 guard on the subject pattern
        // is false the moment the field is filled (a recursief-group creation drops no-loop and leans on that
        // guard alone). fwdRef != null is exactly the 1:1 arm (checked above), and it already passed the typed
        // reference-field test, so the write never reaches a displaced or absent field.
        if (fwdRef != null)
            dan.append("    modify(").append(SUBJECT).append(") {\n        ")
               .append(setter(fwdRef)).append("( $new )\n    }\n");

        // Like the feitcreatie anker, a §8.1.7 far-side guard on the objectcreatie subject hosts its exists/not
        // join after the subject pattern; a near guard yields empty extra and keeps the output byte-identical.
        LoweredConditie opVw = oc.voorwaarde() != null
                ? conditie(acc, oc.voorwaarde(), subject, true) : LoweredConditie.van("");
        // §9.10 fase-2: a recursief-group objectcreatie drops no-loop so it forward-chains on its own inserts to the
        // fixpoint (the idempotence guard above is the anti-runaway control, not no-loop). Non-recursief: unchanged.
        String subjectConstraints = eenOpEen
                ? (opVw.inline().isBlank() ? "" : opVw.inline() + ", ") + ident(fwdRef, false) + " == null"
                : opVw.inline();
        Regel r = new Regel(oc.naam())
            .clausule(SUBJECT + " : " + ident(subject.naam, true) + "( " + subjectConstraints + " )")
            .clausules(opVw.extra())
            .clausules(acc.clausules);
        if (idemGuard != null) r.clausule(idemGuard);
        r.geldigheid(oc.geldigheid())
            .dan(dan.toString());
        if (model.isRecursiefGroepslid(oc.naam())) r.zonderNoLoop();
        out.append(r.render());
    }

    /** A creation this branch cannot link is refused BY NAME: the marker clause carries the exec branch's
     *  "objectcreatie (<reason>): <name>" sentence, so the gate report reads the same diagnosis whichever branch
     *  answered. It rides navReden rather than the comment text because meet() blanks quoted segments before it
     *  reads a comment, and the ambiguity sentence quotes both type names. No insert is emitted — the rule body
     *  never exists, so there is nothing for the scan to rescue. */
    private void creatieGeweigerd(io.github.yhavinga.regelspraak.ir.ResolvedObjectCreatie oc, String reden) {
        navReden = "objectcreatie (" + reden + "): " + oc.naam();
        out.append(new Regel(oc.naam())
            .clausule("// onopgelost objectcreatie (" + commentVeilig(reden) + "): " + commentVeilig(oc.naam()))
            .clausule("eval(true)")
            .render());
    }

    /** How many fact types relate {@code van} and {@code naar} in ch. 5.5's sense — ref's {@code connecting_pair}
     *  count, the exec branch's {@code verbindendeFeittypen}: a two-role fact type filled by the two types in either
     *  order, plus a wederkerig one-role fact type when both are the same type. Unlike {@code verbindendeRol}, it
     *  keeps 0 (a linkless pair) and more than one (ch. 5.5's ambiguity) apart. */
    private int verbindendeFeittypen(Objecttype van, Objecttype naar) {
        int n = 0;
        for (RegelSpraakModel.Feittype ft : model.feittypen.values()) {
            List<RegelSpraakModel.Rol> r = ft.rollen;
            if (r.size() == 2) {
                boolean ab = van.naam.equalsIgnoreCase(r.get(0).objecttypeNaam()) && naar.naam.equalsIgnoreCase(r.get(1).objecttypeNaam());
                boolean ba = van.naam.equalsIgnoreCase(r.get(1).objecttypeNaam()) && naar.naam.equalsIgnoreCase(r.get(0).objecttypeNaam());
                if (ab || ba) n++;
            } else if (r.size() == 1 && ft.wederkerig && van == naar
                    && van.naam.equalsIgnoreCase(r.get(0).objecttypeNaam())) {
                n++;
            }
        }
        return n;
    }

    /** §13.4.7 feitcreatie -> a rule that, for each anker and each doel ALREADY related to it by the
     *  existing rolC relation (the join field, {@code Creatievelden.koppelveld}), creates the NEW relation by
     *  setting the written field ({@code Creatievelden.schrijfveld}) on the doel. Both come from the creation's
     *  role bindings. A relation instance is a reference field in this fact model (§9.4), so creating one
     *  is a {@code modify}-set; {@code no-loop} keeps the modify from re-triggering the rule, and setting
     *  the field to the same value is idempotent across other rules' fire cycles. */
    private void feitCreatie(io.github.yhavinga.regelspraak.ir.ResolvedFeitCreatie fc, int nr) {
        Acc acc = new Acc();
        Objecttype anker = fc.anker(), doel = fc.doel();
        // §13.4.7 `naam` is a wire-nullable position of this construct — ["string","null"] AND required, like anker
        // and doel below — and NO well-formedness rule covers it: the schema holds the symbolic block non-null and
        // WF-034 crosses it against the declarations (until L2 WF-034 gated the bean-field names zetVeld, joinVeld
        // and the steps' fields, which left the wire then), and WF-094 clause (d) gated the five KEYED tables
        // (until M1, which made them lists whose naam the schema holds non-null), while feitCreaties is a LIST.
        // Measured 2026-09-11 on jutterswet-postwacht with naam null: verifier PASS, ref PASS (10 expectations),
        // clingo PASS (10), exec PLAYED — it hands the null straight to the rule header and the creation still
        // fires (the case OBSERVES it: V1.vakseinaantal counts the created relation, 2, and a 2->99 probe fails
        // in both branches) — while THIS branch died with "Cannot invoke String.replace(…) because naam is null",
        // because every rule name here goes through escapeRegelNaam and every marker comment through
        // commentVeilig. One nameless fallback serves all three sites, in the shape of the loader's own node-kind
        // fallback, so a nameless creation is PLAYED exactly as the exec branch plays it instead of taking the
        // whole transpile down. It carries the WIRE POSITION because a DRL package refuses duplicate rule names:
        // a flat fallback made TWO nameless creations a "Duplicate rule name" compile error where exec played
        // them — measured, and a new branch inequality in place of the one being closed.
        String naam = fc.naam() != null ? fc.naam() : "<unnamed ResolvedFeitCreatie " + nr + ">";
        // An UNRESOLVED feitcreatie — a null anker or doel, or a binding that names no declared role — is refused
        // here UNCONDITIONALLY, exactly as ExecModelTranspiler.feitCreatie refuses it ("feitcreatie (onopgelost)").
        // Until 2026-09-11 this branch let a null one fall through to the emission, where ident(null) /
        // setter(null) / anker.naam threw a NullPointerException that named no construct and took the WHOLE
        // transpile down with it: one construct, two answers — a per-form reason there, an unattributed crash here.
        // The bean fields come from the role bindings (IR-SPEC ch. 3.4), which ref executes alone too.
        FactModelGenerator.Creatievelden velden = FactModelGenerator.creatievelden(model, fc);
        if (anker == null || doel == null || velden == null) {
            out.append(new Regel(naam)
                .clausule("// onopgelost feitcreatie (onopgelost): " + commentVeilig(naam))
                .clausule("eval(true)")
                .render());
            return;
        }
        // §13.3.9 STORAGE DIRECTION, at all THREE positions this construct touches a role field (2026-09-11): each
        // hop's join, the doel's join onto the last hop, and the field the modify WRITES. Measured on
        // verifier-clean, ref-clean models, this branch sank the whole package with a bare compiler error at each —
        // "Unable to Analyse Expression $doel.setSeinvak( $subject )" for the write,
        // "Unable to Analyse Expression this == $subject.gemeenteVanInschrijving" for a hop — naming neither the
        // role, the displacing member, nor the limit. The exec branch refuses the same three by name.
        Objecttype vorigeType = anker;
        String reden = null;
        for (FactModelGenerator.Creatiehop stap : velden.hops()) {
            Objecttype drager = stap.opOuder() ? vorigeType : stap.bereikt();
            Objecttype hopDoel = stap.opOuder() ? stap.bereikt() : vorigeType;
            if (!navVeld("feitcreatie", drager, stap.beanveld(), hopDoel)) { reden = navReden; break; }
            vorigeType = stap.bereikt();
        }
        if (reden == null && !navVeld("feitcreatie", doel, velden.koppelveld(), vorigeType)) reden = navReden;
        if (reden == null && !navVeld("feitcreatie", doel, velden.schrijfveld(), anker)) reden = navReden;
        if (reden != null) {
            out.append(new Regel(naam)
                .clausule("// onopgelost feitcreatie (opslagrichting): " + commentVeilig(naam))
                .clausule("eval(true)")
                .render());
            return;
        }
        // The voorwaarde selects which objects get the relation: a guard on the anker (the houder) stays on
        // the anker pattern; a guard on the doel (the elements) hosts on the $doel pattern. A guard that
        // lowers to NEITHER must NOT be silently dropped — dropping it makes the rule fire unconditionally
        // and OVER-CREATE the relation. Instead the rule becomes an inert eval(false) no-op (carrying the
        // marker), the §-wide "an unresolved condition is a compilable no-op" discipline: better to create
        // nothing (visibly) than to create wrongly.
        String ankerC = "", doelExtra = "", noFire = "";
        List<String> ankerExtra = List.of();
        if (fc.voorwaarde() != null) {
            // A §8.1.7 far-side anker guard ("indien de houder heeft een element") lowers to an EMPTY inline
            // plus a hoisted exists/not join, so lower the anker side with the FULL carrier and host its extra
            // after the anker pattern. conditieInline would degrade that join to a marker, dropping the rule to
            // an inert eval(false); a near anker guard yields empty extra and stays byte-identical.
            Acc ankerAcc = new Acc();                          // each attempt collects into its OWN context;
            LoweredConditie opAnker = conditie(ankerAcc, fc.voorwaarde(), anker, true);
            if (!opAnker.marker()) {
                ankerC = opAnker.inline();
                ankerExtra = opAnker.extra();
                acc = ankerAcc;                                // only the successful attempt's clauses host
            } else if (!Gastheer.doelpogingToegestaan(fc.voorwaarde())) {
                // The whole-period lowering below reads `$subject`, which in the $doel pattern is the ANKER — so a
                // doel attempt here sank the package at compile or read the anker from the doel's pattern. Under
                // the qualifier the doel is not a bound object anyway (WF-109), so refuse by name before trying.
                noFire = "eval( false ) /* TODO " + Gastheer.DOELPOGING_GEHEELPERIODE + " — regel inert i.p.v. over-creatie */";
            } else if (Vensterpositie.weigering(fc.voorwaarde()) != null) {
                // WF-112's composite arm: both attempts refuse the window under GEEN or a counting quantifier, and
                // the refusal says so by name rather than as a guard that lowered to neither side.
                noFire = "eval( false ) /* TODO " + Vensterpositie.weigering(fc.voorwaarde())
                        + " — regel inert i.p.v. over-creatie */";
            } else if (Gastheer.doelgastheerBuitenBereik(fc.voorwaarde(), doel, this::genavigeerd)) {
                // WF-109's first two doel fences. conditieInline below WOULD carry a composite on the $doel
                // pattern — measured, it played a guard whose checks were all hosted on the doel's supertype — but
                // inside a composite or behind a hop the doel is not a bound object, so it must not be tried.
                noFire = "eval( false ) /* TODO " + Gastheer.DOELPOGING_GENEST + " — regel inert i.p.v. over-creatie */";
            } else {
                Acc doelAcc = new Acc();                       // the failed anker attempt is simply abandoned
                Frag opDoel = conditieInline(doelAcc, fc.voorwaarde(), doel, true);
                // A DOEL-side guard rides INSIDE the $doel pattern, so it must be a PURE INLINE constraint. A
                // lowering that hoists a clause of its own would have that clause hosted after the ANKER pattern
                // below, where $subject means the anker and not the doel — a join against the wrong type, and a
                // guard whose inline half is empty, i.e. dropped. conditieInline catches the `extra` channel but
                // not a condition that pushes straight into the Acc (the ch. 3.9a fold is the first that can), so
                // the clause list is checked here too.
                if (!opDoel.marker() && doelAcc.clausules.isEmpty()) {
                    doelExtra = opDoel.text();
                    acc = doelAcc;
                } else {
                    noFire = "eval( false ) /* TODO voorwaarde niet naar anker of doel te lowereren — regel inert i.p.v. over-creatie */";
                }
            }
        }
        Regel rule = new Regel(naam);
        if (!noFire.isEmpty()) rule.clausule(noFire);
        rule.clausule(SUBJECT + " : " + ident(anker.naam, true) + "( " + ankerC + " )")
            .clausules(ankerExtra)        // far-side anker membership: the exists/not join, after $subject is bound
            .clausules(acc.clausules)
            .geldigheid(fc.geldigheid());
        // §9.4 multi-hop: bind each intermediate object on the path anker -> … -> doel, then join the doel to
        // the last one. A to-many hop reads the child's back-reference (veld == $prev); a to-one hop reads the
        // parent's forward reference (this == $prev.veld). $prev is the anker for the first step.
        String vorige = SUBJECT;
        int t = 0;
        for (FactModelGenerator.Creatiehop stap : velden.hops()) {
            String var = "$t" + t++;
            rule.clausule(var + " : " + ident(stap.bereikt().naam, true) + "( "
                    + (stap.opOuder() ? "this == " + vorige + "." + ident(stap.beanveld(), false)
                                      : ident(stap.beanveld(), false) + " == " + vorige)
                    + " )");
            vorige = var;
        }
        // IDEMPOTENT relation write, mirroring the exec branch's werkBijReflectief: modify ONLY when the
        // reference actually changes, so a re-established link does not re-propagate $doel. `no-loop` blocks
        // only the SELF-retrigger, and this rule's own anker pattern is re-activated by any OTHER rule that
        // writes the anker — a fold over the created relation is exactly such a rule, so creation and count
        // ping-ponged forever on a repeated type pair (measured: "geen fixpunt (kringverwijzing?)" on the
        // dubbelverbinding golden's cases 1 and 3, where a second feittype over the pair first made the fold
        // and the creation both lowerable in this branch). A same-valued rewrite writes nothing and the chain
        // terminates — the same argument the scalar Objects.equals guard below carries, on the same seam.
        rule.clausule("$doel : " + ident(doel.naam, true) + "( "
                + ident(velden.koppelveld(), false) + " == " + vorige
                + (!doelExtra.isEmpty() ? ", " + doelExtra : "") + " )")
            .dan("    if (!java.util.Objects.equals($doel." + ident(velden.schrijfveld(), false) + ", " + SUBJECT + ")) {\n"
                + "    modify($doel) {\n        " + setter(velden.schrijfveld()) + "( " + SUBJECT + " )\n    }\n"
                + "    }\n");
        out.append(rule.render());
    }

    private void regel(ResolvedRegel r) {
        Acc acc = new Acc();
        Objecttype subject = r.doel() instanceof Expr.AttribuutRef a ? a.objecttype() : null;
        // A TEMPORAL doel (a List<Segment> tijdlijn attribute) is written a timeline, not a scalar — its value lowers
        // through temporeleWaarde() below, NOT waarde() (which would register a wrong scalar accumulate for a SOM-over-
        // temporal-set and mis-count it). Detect it here so the scalar value pre-computation is skipped.
        boolean temporeelDoel = subject != null && r.doel() instanceof Expr.AttribuutRef td && ongenavigeerd(td)
                && td.objecttype() != null && bevatNaam(td.objecttype().granulariteitLeden(), td.attribuut())
                && !Tijdlijnen.meerPeriodiek(td.objecttype().tijdlijnAantalLeden(), td.attribuut());
        // ch. 4.10: the forced needs of a Let in the value ride the CONSEQUENCE as local variables, so hoisting is
        // opened exactly where a SCALAR setter consumes that value. The two branches below that ignore the scalar
        // value — a temporal attribute doel (single- or multi-period) and a temporal kenmerk write — would orphan
        // the prelude, so they open none and a Let there stays an honest marker.
        if (subject != null && !temporeelDoel && r.doel() instanceof Expr.AttribuutRef sd && sd.objecttype() != null
                && !bevatNaam(sd.objecttype().granulariteitLeden(), sd.attribuut())
                && !bevatNaam(sd.objecttype().kenmerkGranulariteitLeden(), sd.attribuut()))
            acc.letVoorspel = new ArrayList<>();
        // Translate the value first: any aggregation registers an accumulate pattern
        // for the "when" and yields the bound result variable used here in the "then".
        Frag valueStr = subject != null && !temporeelDoel && r.doel() instanceof Expr.AttribuutRef
                ? waarde(acc, r.waarde(), subject) : null;
        // the needs belong to the scalar consequence below; nothing emitted after this point may add one.
        List<String> letVoorspel = acc.letVoorspel == null ? List.of() : List.copyOf(acc.letVoorspel);
        acc.letVoorspel = null;
        // §5.1.4 lower a temporal-doel timeline value BEFORE the acc.clausules flush (temporeleWaarde may register a
        // collectList accumulate for a §5.8.2 to-many sommatie, which must be emitted after $subject binds).
        String temporeelTl = null; boolean temporeelOngeankerd = false;
        if (temporeelDoel && r.doel() instanceof Expr.AttribuutRef td) {
            String ankerLid0 = model.startpuntAnkerLid(td.objecttype(), td.attribuut());
            temporeelOngeankerd = ankerLid0 == null && model.heeftVariabelStartpunt(td.objecttype(), td.attribuut());
            String anker0 = ankerLid0 == null ? null : SUBJECT + "." + getter(ankerLid0) + "()";
            // ch. 2.10: the doel is a REFERENCE position — the §4 restate target is the DECLARED attribute
            // datatype, never the doel node's repetition of it.
            temporeelTl = temporeelOngeankerd ? null
                    : temporeleWaarde(acc, r.waarde(), subject, opNaam(td.objecttype().granulariteitLeden(), td.attribuut()), effType(td), anker0);
        }
        // §8.4/§13.4.5 a TEMPORAL KENMERK doel (kenmerkGranulariteit — a List<Segment> TRUE-period timeline, not a
        // boolean) is written the timeline of its voorwaarde's TRUE-periods: "Een object is duur gedurende de tijd dat
        // <temporeel kenmerk>" makes the doel-kenmerk hold exactly where the gating kenmerk does (§3.8 kenmerk-tijdlijn
        // = boolean-tijdlijn; §3.4 default-onwaar OUTSIDE those periods, so the write is monotone — no complement to
        // seed). The producer is conditieTijdreeks (the SAME TRUE-period lowering the §7.1 gedurende READ uses), so the
        // WRITE and the READ of a kenmerk-tijdlijn are symmetric. DECIDABLE only when the voorwaarde lowers to a finite
        // timeline (a positive single-period subject-kenmerk, or an ALLE/ten-minste-1 compound of them) AND geldig
        // altijd: an EMPTY voorwaarde is "altijd" (an UNBOUNDED true-timeline, no finite List<Segment>) and a
        // geldigheids-WINDOW would need a timeline×window clip (§8.4 leaves that periodization under-specified) — both
        // stay a marker below. Mirrored by the exec back-end.
        boolean temporeelKenmerkDoel = subject != null && r.soort() == ResolvedRegel.Soort.KENMERKTOEKENNING
                && r.doel() instanceof Expr.AttribuutRef ka && ongenavigeerd(ka) && ka.objecttype() != null
                && bevatNaam(ka.objecttype().kenmerkGranulariteitLeden(), ka.attribuut())
                && !Tijdlijnen.meerPeriodiek(ka.objecttype().kenmerkTijdlijnAantalLeden(), ka.attribuut());
        String kenmerkTl = null;
        if (temporeelKenmerkDoel && r.voorwaarde() != null && r.geldigheid().altijd())
            kenmerkTl = conditieTijdreeks(r.voorwaarde(), subject);
        // A decidable temporal-kenmerk write ignores the scalar voorwaarde gate (the voorwaarde IS the write timeline,
        // not a rekendatum point-check) — the pattern stays class-reactive so it re-fires when the source kenmerk lands.
        boolean kenmerkTlGelowerd = kenmerkTl != null;

        // A forward calculation should not retrigger itself on its own modify (no-loop, in the header);
        // it still recomputes when another rule changes the facts it reads.
        // §9.2 negation-stratification, DERIVED from the declared kenmerk dependency graph (Stratificatie —
        // the uchar WF-046 strata principle as a lowering): a negation-consumer sinks below every producer of
        // the kenmerk it negates, and a consumer of a DERIVED negated kenmerk sinks below that producer's own
        // stratum — the chain case the former one-level heuristic left to source order (the
        // StratificatieKetenTest red case). Shared with ExecModelTranspiler: both routes read one derivation.
        Regel rule = new Regel(r.naam());
        int stratSal = strat.salience(r);
        if (stratSal != 0) rule.salience(stratSal);
        // §8.1.7 far-side "heeft een <rol>" lowers to an EXISTS-join — a separate when-clause that cannot live
        // inside the subject pattern's inline constraint, so condition lowering hands it back as an extra clause
        // (directly for a top-level membership, carried up through a pure-conjunction §13.4.14 alle/de compound).
        // Each is appended after the subject pattern; a far-side nested under a non-conjunction quantifier stays
        // an honest marker.
        // §8.4.3 a periodevergelijking on a TIJDSAFHANKELIJK doel should CLIP the written timeline to the
        // period, not all-or-nothing gate it on the rekendatum — that clip is not built yet, so marker the
        // voorwaarde (rule inert, never a silently mis-gated timeline). Mirrored by the exec back-end.
        boolean periodeOpTijdlijnDoel = r.voorwaarde() != null && Conditie.bevatPeriode(r.voorwaarde())
                && r.doel() instanceof Expr.AttribuutRef da && da.objecttype() != null
                && (bevatNaam(da.objecttype().granulariteitLeden(), da.attribuut())
                    || bevatNaam(da.objecttype().kenmerkGranulariteitLeden(), da.attribuut()));
        LoweredConditie lc = subject != null && r.voorwaarde() != null && !kenmerkTlGelowerd
                ? (periodeOpTijdlijnDoel
                    ? LoweredConditie.marker("periodevergelijking op een tijdsafhankelijk doel (§8.4.3)", r.voorwaarde().bron())
                    : conditie(acc, r.voorwaarde(), subject, true))
                : LoweredConditie.van("");
        String constraint = lc.inline();
        // §9.6 initialisatie assigns only when the attribuut holds NO value yet ("controleert of een attribuut
        // een waarde bevat … als dit niet het geval is"): the leeg-guard makes the rule idempotent and removes
        // the agenda-order dependence vs a berekening on the same doel — the berekening (unguarded) wins whenever
        // it applies, the initialisatie fills the rest. Property reactivity then cancels a stale activation.
        if (r.soort() == ResolvedRegel.Soort.INITIALISATIE && subject != null
                && r.doel() instanceof Expr.AttribuutRef ia && ongenavigeerd(ia)) {
            String leegGuard = ident(ia.attribuut(), false) + " == null";
            constraint = constraint.isEmpty() ? leegGuard : leegGuard + ", " + constraint;
        }
        if (subject == null) {
            rule.clausule("// onopgelost doel-objecttype: " + commentVeilig(r.doel().bron().snippet())).clausule("eval(true)");
        } else {
            // Freshness: property reactivity listens only to CONSTRAINED properties, so an assignment whose
            // pattern does not constrain what its VALUE reads goes stale — it computes once (possibly against
            // leeg) and never recomputes when another rule writes an input (observed in a two-rule chain where
            // an unconditioned berekening kept 0 after its operand arrived). @watch the subject's OWN
            // attributes the value expression reads; no-loop still blocks the self-retrigger. A read-write
            // CYCLE between rules (x = y+1; y = x+1) would recompute divergently forever — the resolver
            // WARNs on it (keurKringverwijzingen) and the engine caps fireAllRules, since one-writer-per-
            // attribute does NOT make the dependency graph acyclic. The exec back-end needs no analog:
            // its opaque lambda constraints leave the pattern class-reactive.
            java.util.LinkedHashSet<String> watch = new java.util.LinkedHashSet<>();
            verzamelEigenReads(r.waarde(), subject, watch);
            // §9.9: an anchored temporal write ALSO reads the startpunt attribute (injected at lowering,
            // invisible to the IR walk) — watch it, so the write re-fires when the startpunt rule lands.
            if (r.doel() instanceof Expr.AttribuutRef da && ongenavigeerd(da) && da.objecttype() == subject) {
                String ankerLid = model.startpuntAnkerLid(subject, da.attribuut());
                if (ankerLid != null) watch.add(ident(ankerLid, false));
            }
            // §8.4/§13.4.5: a decidable temporal-kenmerk write reads its SOURCE kenmerk-tijdlijn(s) from the voorwaarde
            // (which is NOT lowered into the scalar constraint here), invisible to the value-read walk — watch them so
            // the write rebuilds when the source kenmerk lands.
            if (kenmerkTlGelowerd) verzamelKenmerkReads(r.voorwaarde(), subject, watch);
            // §7.1/§8.4: a SCALAR-doel §7.1 totaal "gedurende de tijd dat <temporeel kenmerk>" CLIPS by the subject's own
            // kenmerk-tijdlijn (in the function's filter, invisible to the argument walk). Watch it so the totaal re-fires
            // when that kenmerk is WRITTEN by a §8.4 temporal kenmerktoekenning (the write→read chain). Scoped to a scalar
            // doel: a temporal ATTRIBUTE producer (aantal-dagen-in / tijdsevenredig) already rides its own anchor/class
            // reactivity, and broadening its @watch would churn the pinned anchored-bucket DRL.
            if (!temporeelDoel && !temporeelKenmerkDoel) verzamelFilterKenmerkReads(r.waarde(), subject, watch);
            rule.clausule(SUBJECT + " : " + ident(subject.naam, true) + "( " + constraint + " )"
                    + (watch.isEmpty() ? "" : " @watch( " + String.join(", ", watch) + " )"));
            for (String e : lc.extra()) rule.clausule(e);   // the exists-join(s), after $subject is bound
            rule.clausules(acc.clausules)
                .geldigheid(r.geldigheid());   // §4.2: a time-bounded version fires only inside its window
        }
        if (r.doel() instanceof Expr.AttribuutRef a && ongenavigeerd(a) && a.objecttype() != null
                && bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut())
                && !Tijdlijnen.meerPeriodiek(a.objecttype().tijdlijnAantalLeden(), a.attribuut())) {
            // §5.1.4 the doel is a SINGLE-period TEMPORAL attribute -> write a List<Segment> timeline, not a scalar.
            // A timeline-producing RHS we cannot lower stays an honest /* TODO */ marker (never a scalar setter on a
            // List<Segment> field, which would not compile).
            // §3.8/§9.9: a doel riding a tijdlijn met VARIABEL startpunt buckets at the per-object anchor
            // (the §9.9 synthetic attribute) instead of the calendar; with NO startpuntbepaling resolved the
            // timeline has no periodization at all -> honest marker, never silently calendar-bucketed.
            String ankerLid = model.startpuntAnkerLid(a.objecttype(), a.attribuut());
            boolean ongeankerd = temporeelOngeankerd;
            String anker = ankerLid == null ? null : SUBJECT + "." + getter(ankerLid) + "()";
            String tl = temporeelTl;   // pre-lowered above (before the acc.clausules flush) so a collectList accumulate emits
            // RUNTIME-SEMANTICS ch. 7.8, the TIMELINE arm: on a STANDING temporal target (the shared
            // classification) an EMPTY fold result is the temporal leeg, decides nothing, and must write
            // NOTHING — the case-supplied input timeline stands WHOLE, never merged per bucket (a default
            // decides once, and §3.11's timeline write replaces the whole attribute timeline). The domain is
            // what keeps this from becoming "skip every empty timeline write": a multi-writer or versioned
            // temporal target keeps the v0 erase. Kenmerk-tijdlijn writes are a separate branch below and
            // never ask (the key carries no member kind while WF-002 admits a same-named kenmerk).
            boolean staandTl = tl != null && staande.staandTemporeel(a);
            String tlGevuld = staandTl ? fnImport("rsTijdreeksGevuld") + "(" + tl + ")" : null;
            if (tl != null && anker != null) {
                // IDEMPOTENT anchored write (the temporal analogue of the scalar Objects.equals guard): the
                // @watch on the startpunt attribute re-fires this rule when the anchor lands, and a sibling
                // anchored writer's modify would otherwise ping-pong with it forever. rsTijdreeksGelijk
                // compares per-segment (Segment has no equals); the producer is pure, so evaluating twice is
                // safe. Scoped to ANCHORED writes only — the calendar emission (and its byte-pins) unchanged.
                fnImport("rsTijdreeksGelijk");
                rule.dan("    if (" + (staandTl ? tlGevuld + " && " : "")
                        + "!rsTijdreeksGelijk(" + SUBJECT + "." + getter(a.attribuut()) + "(), " + tl + ")) {\n"
                        + "    modify(" + SUBJECT + ") {\n        " + setter(a.attribuut()) + "( " + tl + " )\n    }\n"
                        + "    }\n");
            } else if (staandTl) {
                // the CALENDAR emission on a standing target: unguarded it wipes the input timeline on every
                // empty fold (the measured borgdeel/dagentelling defect). The idempotence guard the anchored
                // arm carries is NOT needed here (a calendar write reads no startpunt attribute, so its
                // pattern is not re-fired by a sibling), so only the standing guard is added.
                rule.dan("    if (" + tlGevuld + ") {\n"
                        + "    modify(" + SUBJECT + ") {\n        " + setter(a.attribuut()) + "( " + tl + " )\n    }\n"
                        + "    }\n");
            } else {
                rule.dan("    modify(" + SUBJECT + ") {\n        " + setter(a.attribuut()) + "( "
                        + (tl != null ? tl : todo(ongeankerd ? "tijdlijn met variabel startpunt zonder startpuntbepaling (§9.9)"
                                                             : "temporele toekenning", r.waarde().bron())) + " )\n    }\n");
            }
        } else if (r.doel() instanceof Expr.AttribuutRef a && ongenavigeerd(a) && a.objecttype() != null
                && bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut())
                && Tijdlijnen.meerPeriodiek(a.objecttype().tijdlijnAantalLeden(), a.attribuut())) {
            // §13.3.6.7 a MULTI-PERIOD temporal doel ("voor elke N maanden") is a List<Segment> timeline whose WRITE
            // producer semantics are under-specified. MARKER it honestly — never fall through to a SCALAR setter on a
            // List<Segment> field (a silent type-mismatched write the bare value branch below would otherwise emit).
            // Mirrors the exec multi-period doel skip.
            rule.dan("    modify(" + SUBJECT + ") {\n        " + setter(a.attribuut()) + "( "
                    + todo("meer-periodieke temporele toekenning (§13.3.6.7)", r.waarde().bron()) + " )\n    }\n");
        } else if (r.doel() instanceof Expr.AttribuutRef a && ongenavigeerd(a) && a.objecttype() != null
                && bevatNaam(a.objecttype().kenmerkGranulariteitLeden(), a.attribuut())) {
            // §8.4/§13.4.5 a TEMPORAL KENMERK doel (kenmerkGranulariteit — its field is a List<Segment> TRUE-period
            // timeline, not a boolean) cannot take the scalar `setK(true)` the bare value branch below would emit
            // (a boolean into a List field does not compile). The DECIDABLE case writes the voorwaarde's TRUE-period
            // timeline (kenmerkTl, computed above via the SAME conditieTijdreeks lowering the §7.1 gedurende READ uses):
            // the doel-kenmerk holds exactly where the gating kenmerk-tijdlijn does (§3.8), default-onwaar elsewhere
            // (§3.4 — monotone, no complement seed). IDEMPOTENT (rsTijdreeksGelijk, mirroring the anchored-attribuut
            // guard): the @watch on the source kenmerk re-fires this rule when it lands, and the guard stops a same-
            // valued rebuild from re-propagating. An EMPTY / geldigheids-windowed / negated / non-kenmerk voorwaarde
            // left kenmerkTl null (under-specified periodization) -> a COMMENT-ONLY marker: a `setK(null)` write would
            // WIPE a seeded timeline, a destructive "marker" and an asymmetry, not an inert one.
            if (kenmerkTl != null) {
                fnImport("rsTijdreeksGelijk");
                rule.dan("    if (!rsTijdreeksGelijk(" + SUBJECT + "." + getter(a.attribuut()) + "(), " + kenmerkTl + ")) {\n"
                        + "    modify(" + SUBJECT + ") {\n        " + setter(a.attribuut()) + "( " + kenmerkTl + " )\n    }\n"
                        + "    }\n");
            } else {
                rule.dan("    /* TODO " + commentVeilig("temporele kenmerktoekenning (§8.4/§13.4.5) — onbepaalde write-producer (lege/geldigheids-vensterde/genavigeerde voorwaarde): " + r.doel().bron()) + " */\n");
            }
        } else if (valueStr != null && r.doel() instanceof Expr.AttribuutRef a) {
            // Navigated-read reactivity: a to-one navigated value read leaves the rule stale on the FAR fact's update
            // (mvel property reactivity is per subject TYPE). Add a far-side JOIN per distinct navigation so the
            // spiegel re-fires when the navigated fact lands (the order-fragility fix). The getter-chain read stays.
            navReactiviteitClausules(rule, r.waarde(), subject);
            // A value node with NO type is ill-formed input — WF-034's type prologue refuses it at every
            // read, and the one node that carries no type by CONSTRUCTION (the §5.7 union) is refused by
            // WF-110 (a). Such a node cannot be restated into the target's unit, and reading its type to
            // decide crashed bare rather than refusing. It arrives as a MARKER from waarde() above, so the
            // two are asserted together: a marker is not restated, and a typeless value that is somehow NOT
            // a marker becomes one here, by name, instead of NPEing on the unit read.
            if (eff.typeVan(r.waarde()) == null && !valueStr.marker())
                valueStr = Frag.todo("typeloze regelwaarde: een waardeknoop zonder statisch type kan niet in de"
                        + " eenheid van het doel worden herschreven (WF-034 / WF-110)", r.waarde().bron());
            // restate the value in the target's unit (§4); §6.1.2 forbids AUTO-rounding —
            // if the result has too many decimals the author must add an explicit afronding. The null
            // guard is not a fallback: the arm above has already turned a typeless value into a NAMED
            // marker, so reaching here with no type means "already refused, by name, one line up".
            if (eff.typeVan(r.waarde()) != null && eff.typeVan(r.waarde()).isNumeriek())
                valueStr = omrekenWaarde(valueStr, effType(r.waarde()), effType(a));
            // §6.1.2 write boundary: collapse any remaining exact Breuk to its decimal value BEFORE the setter, so a
            // Breuk never reaches a BigDecimal field (mvel strict-mode resolves setX(BigDecimal), never setX(Object))
            // and the idempotent Objects.equals guard compares like-typed values. Only a COMPUTED value can be a Breuk
            // (arithmetic / percentage / division / a non-terminating rational literal), so a bare attribute/parameter
            // copy is left unwrapped (no churn, never a Breuk). Never wrap a MARKER placeholder. The wrap is an
            // identity for a terminating BigDecimal (rsWaarde = toBd), so it is inert where it does fire.
            if (kanBreukOpleveren(r.waarde()) && !valueStr.marker()) {
                fnImport("rsWaarde");
                valueStr = Frag.from("rsWaarde(" + valueStr.text() + ")", valueStr);
            }
            // IDEMPOTENT write (the exec back-end's werkBij, mirrored): modify only when the value actually
            // CHANGES, so recomputation reaches a FIXPOINT. Complex mvel constraints (function calls,
            // navigated reads) make a pattern CLASS-reactive — it re-fires on EVERY subject update — and the
            // @watch freshness above makes readers re-fire on real input changes; without this guard two such
            // rules over one subject ping-pong forever (NO_LOOP blocks only the self-retrigger). A same-valued
            // rebuild skips the modify, so nothing re-propagates and the chain terminates. The value
            // expression is pure (rs* helpers are side-effect-free), so evaluating it twice is safe.
            // mvel PROPERTY access in the guard (not an explicit getter call): a boolean kenmerk exposes
            // is<X>(), and mvel's property navigation resolves either accessor form.
            // ch. 4.10: the needs come FIRST — the idempotence guard and the setter both read the same locals, so
            // one forced need serves both (a second evaluation is what an inlined value text would have cost).
            // RUNTIME-SEMANTICS ch. 7.8 (the SCALAR norm) together with the ch. 7.7 desugaring domain: on a
            // standing scalar target — the SHARED classification, which alone decides WHERE this applies — a
            // LEEG (null) computed value decides nothing and must write NOTHING, so the case input stands, or
            // in the desugaring domain the initialisatie (whose own §9.6 leeg-guard then lets the input win)
            // decides. A blanket "never write null" would erase the v0 multi-writer semantics this branch
            // still owes outside the domain, which is why the guard rides the classification and not the value.
            String leegGuard = staande.staandScalair(r) ? valueStr.text() + " != null && " : "";
            rule.dan(String.join("", letVoorspel)
                    + "    if (" + leegGuard + "!java.util.Objects.equals(" + SUBJECT + "." + ident(a.attribuut(), false) + ", " + valueStr.text() + ")) {\n"
                    + "    modify(" + SUBJECT + ") {\n        " + setter(a.attribuut()) + "( " + valueStr.text() + " )\n    }\n"
                    + "    }\n");
        } else {
            rule.dan("    // onopgelost doel: " + commentVeilig(r.doel().bron().snippet()) + "\n");
        }
        markeerGevuurd(rule, r.naam());   // §8.1.9 prepend the RuleFired insert iff this rule's firing is referenced
        out.append(rule.render());
        // §6.5/§6.6 fout companions are gated by the rule's INLINE constraint PLUS its hoisted join clauses. A
        // far-side condition is a separate exists/not clause, not an inline constraint, so the companion must
        // carry it too — otherwise it loses the membership gate and over-fires (raising the Inconsistentie for
        // subjects the rule never acts on). Threading lc.extra() into the companion fires it exactly when the
        // division/wortel would actually execute: the inline constraint holds AND the membership holds. (The
        // companion is insert-only, so adding an exists/not clause is safe — it cancels no sibling activation,
        // unlike a modify+no-loop rule. The own-attribute operand scope limit is unchanged.)
        if (subject != null) {
            deelDoorLeegFout(acc, r, subject, constraint, lc.marker(), lc.extra());
            wortelNegatiefFout(acc, r, subject, constraint, lc.marker(), lc.extra());
            startpuntLeegFout(acc, r, subject, constraint, lc.marker(), lc.extra());
        }
    }

    /** §6.8: a leeg startpunt-anker is een Fout — surfaced through the §9.5 Inconsistentie channel by a
     *  separate insert-only companion on every §9.9 startpunt write whose anchor decomposes to a
     *  re-readable leeg guard (the subject's OWN attribute or a to-ONE navigation). A datum-arithmetic
     *  anchor ("de datum min DE TERMIJN") is leeg exactly when its leftmost datum operand is —
     *  Tabel 15: a leeg duration counts as 0/unchanged, only a leeg datum propagates. A parameter or
     *  function anchor still degrades to a leeg write but raises no Inconsistentie (the same scope limit
     *  as the §6.5/§6.6 companions). The write itself needs no change: a leeg anchor writes leeg. */
    private void startpuntLeegFout(Acc acc, ResolvedRegel r, Objecttype subject, String voorwaarde, boolean voorwaardeMarker, List<String> extraClausules) {
        if (voorwaardeMarker) return;
        if (!(r.doel() instanceof Expr.AttribuutRef d) || !model.isStartpuntLid(subject, d.attribuut())) return;
        Expr wortel = r.waarde();
        while (wortel instanceof Expr.Binair b) wortel = b.links();
        OperandDeel anker = companionDeel(acc, wortel, subject);
        if (anker == null) return;
        String pattern = (voorwaarde.isEmpty() ? "" : voorwaarde + ", ") + anker.leeg();
        Regel rule = new Regel(r.naam() + " (leeg startpunt)")
            .clausule(SUBJECT + " : " + ident(subject.naam, true) + "( " + pattern + " )");
        for (String e : extraClausules) rule.clausule(e);   // the host rule's far-side membership gate, if any
        out.append(rule
            .geldigheid(r.geldigheid())
            .clausule(inconsistentieGuard(r.naam()))
            .dan(inconsistentieDan(r.naam()))
            .render());
    }

    /** §6.6: "de wortel van" a NEGATIVE radicand is a Fout (RegelSpraak has no complex numbers). The value side
     *  already degrades it to leeg (rsWortel returns null for a negative radicand, instead of letting
     *  BigDecimal.sqrt throw and abort the session); this surfaces the Fout through the §9.5 Inconsistentie
     *  channel — a SEPARATE rule per wortel that fires when the radicand is negative (and the host voorwaarde
     *  holds). Scope, like the division fout: only a radicand that is the subject's OWN attribute lowers to a
     *  plain re-readable property constraint; a navigated/parameter/nested/aggregation radicand still degrades
     *  to leeg but raises no Inconsistentie (carded). A leeg radicand is leeg, not a fout (§6.6), and the guard
     *  radicand &lt; 0 is false for a null, so it does not fire for leeg. */
    private void wortelNegatiefFout(Acc acc, ResolvedRegel r, Objecttype subject, String voorwaarde, boolean voorwaardeMarker, List<String> extraClausules) {
        if (voorwaardeMarker) return;
        List<Foutplek> wortels = new ArrayList<>();
        verzamelWortels(r.waarde(), Poort.OPEN, wortels);
        List<String> guards = new ArrayList<>();   // the "radicand < 0" guard per qualifying wortel
        for (Foutplek fp : wortels) {
            Expr.Functie w = (Expr.Functie) fp.plek();
            if (w.argumenten().size() != 1) continue;
            // ch. 10.6 clause 2 / ch. 10.2 gates, then the radicand itself — in the binder scope the gates
            // establish, so a radicand that is a BINDER re-reads its need. null = drop.
            String garde = plekGuards(acc, fp, subject, () -> {
                OperandDeel rd = companionDeel(acc, w.argumenten().get(0), subject);
                if (rd == null) return null;
                // a navigated radicand must guard its root before the "." dereference (mvel is not null-safe);
                // an own attribute reads `basis < 0` directly (a null is false for "< 0", no NPE) — byte-identical.
                return rd.genavigeerd()
                        ? rd.vol() + " && " + rd.waarde() + " < new java.math.BigDecimal(\"0\")"
                        : rd.waarde() + " < new java.math.BigDecimal(\"0\")";
            });
            if (garde != null) guards.add(garde);
        }
        for (int i = 0; i < guards.size(); i++) {
            String pattern = (voorwaarde.isEmpty() ? "" : voorwaarde + ", ") + guards.get(i);
            Regel rule = new Regel(r.naam() + " (wortel van negatief" + (guards.size() > 1 ? " " + (i + 1) : "") + ")")
                .clausule(SUBJECT + " : " + ident(subject.naam, true) + "( " + pattern + " )");
            for (String e : extraClausules) rule.clausule(e);   // the host rule's far-side membership gate, if any
            out.append(rule
                .geldigheid(r.geldigheid())
                .clausule(inconsistentieGuard(r.naam()))
                .dan(inconsistentieDan(r.naam()))
                .render());
        }
    }

    /** Collect every "de wortel van" function node reachable through the wrapper layers (Functie args / Binair /
     *  Afgerond / Begrensd) of a value expression — for the §6.6 negative-radicand fout surfacing. */
    /** The subject's OWN attribute properties a value expression READS (the @watch set): every EIGEN
     *  AttribuutRef in the tree, as bean property names. Navigated/aggregation reads live in their own
     *  patterns (with their own reactivity) and are not the subject pattern's to watch. */
    private void verzamelEigenReads(Expr e, Objecttype subject, java.util.Set<String> uit) {
        if (e instanceof Expr.AttribuutRef a) {
            if (naviResolve(a, subject).soort() == NaviSoort.EIGEN) uit.add(ident(a.attribuut(), false));
        } else if (e instanceof Expr.Binair b) {
            verzamelEigenReads(b.links(), subject, uit);
            verzamelEigenReads(b.rechts(), subject, uit);
        } else if (e instanceof Expr.Functie f) {
            for (Expr a : f.argumenten()) verzamelEigenReads(a, subject, uit);
        } else if (e instanceof Expr.Afgerond af) {
            verzamelEigenReads(af.basis(), subject, uit);
        } else if (e instanceof Expr.Begrensd bg) {
            verzamelEigenReads(bg.basis(), subject, uit);
            verzamelEigenReads(bg.minimum(), subject, uit);
            verzamelEigenReads(bg.maximum(), subject, uit);
        } else if (e instanceof Expr.TekstReeks tr) {
            for (Expr.TekstReeks.Deel d : tr.delen())
                if (d instanceof Expr.TekstReeks.Ingevoegd i) verzamelEigenReads(i.expr(), subject, uit);
        } else if (e instanceof Expr.DimensieCel dc) {
            verzamelEigenReads(dc.attribuut(), subject, uit);
        } else if (e instanceof Expr.Let b) {
            // ch. 4.10: a read WRAPPED in a Let is still a read of the subject's own attribute. Without this arm
            // the pattern gets an EMPTY reactivity mask for a Let-only value: the rule computes once — against a
            // producer that has not written yet — and never recomputes. Both sides, since the value is evaluated
            // whenever the body reads the binder and the watch may over-approximate but never under-approximate.
            verzamelEigenReads(b.value(), subject, uit);
            verzamelEigenReads(b.body(), subject, uit);
        } else if (e instanceof Expr.IfThenElse k) {
            // ch. 10.2: only one arm RUNS, but the watch may over-approximate and may not under-approximate —
            // an attribute the taken arm reads must be in the mask whichever arm that turns out to be.
            Conditie.bevatOperand(k.conditie(), x -> { verzamelEigenReads(x, subject, uit); return false; });
            verzamelEigenReads(k.dan(), subject, uit);
            verzamelEigenReads(k.anders(), subject, uit);
        }
    }

    /** §8.4/§13.4.5 the subject's OWN kenmerk-tijdlijn properties a temporal-kenmerk write's voorwaarde READS (the
     *  gedurende source): a single {@link Conditie.Kenmerk} adds its property; an ALLE / ten-minste-1 compound recurses
     *  its kenmerk members. Only the shapes conditieTijdreeks lowers reach here (a positive own subject kenmerk), so
     *  the @watch set matches exactly the fields the write timeline reads. */
    private void verzamelKenmerkReads(Conditie c, Objecttype subject, java.util.Set<String> uit) {
        if (c instanceof Conditie.Kenmerk k && !genavigeerd(k) && subject != null
                && bevatNaam(subject.kenmerkGranulariteitLeden(), k.kenmerk()))
            uit.add(ident(k.kenmerk(), false));
        else if (c instanceof Conditie.Samengesteld s)
            for (Conditie o : s.onderdelen()) verzamelKenmerkReads(o, subject, uit);
    }

    /** §7.1/§8.4 the own-kenmerk reads inside a value expression's aggregation FILTERS ("gedurende de tijd dat
     *  &lt;temporeel kenmerk&gt;") — the clipping kenmerk rides {@link Expr.Functie#filter()}, not an argument, so the
     *  {@link #verzamelEigenReads} value walk misses it. Watching it lets a §7.1 totaal re-fire when that kenmerk is
     *  WRITTEN by a §8.4 temporal kenmerktoekenning. Recurses the same wrapper layers as verzamelEigenReads. */
    private void verzamelFilterKenmerkReads(Expr e, Objecttype subject, java.util.Set<String> uit) {
        if (e instanceof Expr.Functie f) {
            verzamelKenmerkReads(f.filter(), subject, uit);
            for (Expr a : f.argumenten()) verzamelFilterKenmerkReads(a, subject, uit);
        } else if (e instanceof Expr.Binair b) {
            verzamelFilterKenmerkReads(b.links(), subject, uit);
            verzamelFilterKenmerkReads(b.rechts(), subject, uit);
        } else if (e instanceof Expr.Afgerond af) {
            verzamelFilterKenmerkReads(af.basis(), subject, uit);
        } else if (e instanceof Expr.Begrensd bg) {
            verzamelFilterKenmerkReads(bg.basis(), subject, uit);
            verzamelFilterKenmerkReads(bg.minimum(), subject, uit);
            verzamelFilterKenmerkReads(bg.maximum(), subject, uit);
        } else if (e instanceof Expr.Let b) {   // ch. 4.10: an aggregation's clipping kenmerk under a binding
            verzamelFilterKenmerkReads(b.value(), subject, uit);
            verzamelFilterKenmerkReads(b.body(), subject, uit);
        } else if (e instanceof Expr.IfThenElse k) {   // ch. 10.2: either arm may be the one that runs
            verzamelFilterKenmerkReads(k.dan(), subject, uit);
            verzamelFilterKenmerkReads(k.anders(), subject, uit);
        }
    }

    /** §13.4.1/§13.3.9 REACTIVITY over a to-ONE navigated value read: mvel property reactivity listens only to the
     *  subject pattern's OWN constrained/@watch-ed properties — never to a FAR fact reached by navigation. A
     *  spiegel-regel whose RHS navigates to another object's attribute therefore computed once against a still-leeg
     *  navigated value and kept it stale when a sibling rule of the SAME fire-cycle wrote the far attribute (the
     *  order-fragility of navigated reads). For each DISTINCT to-one navigation the value reads, add a far-side
     *  reactivity clause {@code ( FarType(this == $subject.rol) or eval(rol == null) )}: the JOIN alternative binds
     *  the reached fact so its modify re-activates the rule; the {@code eval} alternative fires the NULL-relation
     *  subject (which reads leeg and never goes stale, so it needs no far reactivity — but MUST still fire, §6.9). The
     *  two alternatives are mutually exclusive (join needs a non-null path; eval needs a null one), so no subject
     *  double-fires. The chain form guards EVERY intervening relation. The consequence keeps its own null-safe getter
     *  chain read. Mirrors the exec back-end navReactiviteit. */
    private void navReactiviteitClausules(Regel rule, Expr waarde, Objecttype subject) {
        List<Expr.AttribuutRef> refs = new ArrayList<>();
        verzamelNavRefs(waarde, subject, refs);
        java.util.LinkedHashMap<String, String> clausules = new java.util.LinkedHashMap<>();
        int n = 0;
        for (Expr.AttribuutRef a : refs) {
            NaviRef nr = naviResolve(a, subject);
            String pad; String nullGuard; String key;
            if (nr.soort() == NaviSoort.GENAVIGEERD) {
                pad = SUBJECT + "." + getter(nr.rol()) + "()";
                nullGuard = pad + " == null";
                key = a.objecttype().naam + "|" + nr.rol();
            } else if (nr.soort() == NaviSoort.KETEN) {
                // the JOIN pad uses mvel null-safe `!.` so a null intermediate makes the constraint a NON-match (never
                // an NPE); the eval null-guard walks the same prefixes with plain getters to fire the null-relation subject.
                StringBuilder padSb = new StringBuilder(SUBJECT);
                StringBuilder guardSb = new StringBuilder(SUBJECT);
                List<String> guards = new ArrayList<>();
                List<String> hops = nr.pad();
                for (int i = 0; i < hops.size(); i++) {
                    padSb.append(i == 0 ? "." : "!.").append(ident(hops.get(i), false));
                    guardSb.append(".").append(getter(hops.get(i))).append("()");
                    guards.add(guardSb + " == null");
                }
                pad = padSb.toString();
                nullGuard = String.join(" || ", guards);
                key = a.objecttype().naam + "|" + String.join(".", nr.pad());
            } else continue;   // EIGEN (own-attr @watch covers it) / NAAR_VEEL (aggregate join) / unresolvable (marker)
            clausules.putIfAbsent(key, "( " + ident(a.objecttype().naam, true) + "( this == " + pad + " )"
                    + " or eval( " + nullGuard + " ) )");
        }
        for (String c : clausules.values()) { rule.clausule(c); n++; }
    }

    /** The to-ONE NAVIGATED AttribuutRefs a value expression reads (the far-side reads mvel reactivity cannot cover),
     *  walked over the SAME tree shape as {@link #verzamelEigenReads}. */
    private void verzamelNavRefs(Expr e, Objecttype subject, List<Expr.AttribuutRef> uit) {
        if (e instanceof Expr.AttribuutRef a) {
            NaviSoort s = naviResolve(a, subject).soort();
            if (s == NaviSoort.GENAVIGEERD || s == NaviSoort.KETEN) uit.add(a);
        } else if (e instanceof Expr.Binair b) {
            verzamelNavRefs(b.links(), subject, uit);
            verzamelNavRefs(b.rechts(), subject, uit);
        } else if (e instanceof Expr.Functie f) {
            for (Expr a : f.argumenten()) verzamelNavRefs(a, subject, uit);
        } else if (e instanceof Expr.Afgerond af) {
            verzamelNavRefs(af.basis(), subject, uit);
        } else if (e instanceof Expr.Begrensd bg) {
            verzamelNavRefs(bg.basis(), subject, uit);
            verzamelNavRefs(bg.minimum(), subject, uit);
            verzamelNavRefs(bg.maximum(), subject, uit);
        } else if (e instanceof Expr.TekstReeks tr) {
            for (Expr.TekstReeks.Deel d : tr.delen())
                if (d instanceof Expr.TekstReeks.Ingevoegd i) verzamelNavRefs(i.expr(), subject, uit);
        } else if (e instanceof Expr.DimensieCel dc) {
            verzamelNavRefs(dc.attribuut(), subject, uit);
        } else if (e instanceof Expr.Let b) {   // ch. 4.10: a far-side read under a binding needs the same join
            verzamelNavRefs(b.value(), subject, uit);
            verzamelNavRefs(b.body(), subject, uit);
        } else if (e instanceof Expr.IfThenElse k) {   // ch. 10.2: either arm may be the one that runs
            Conditie.bevatOperand(k.conditie(), x -> { verzamelNavRefs(x, subject, uit); return false; });
            verzamelNavRefs(k.dan(), subject, uit);
            verzamelNavRefs(k.anders(), subject, uit);
        }
    }

    /**
     * A §6.5/§6.6 fault site together with the ch. 4.10 NEEDS whose barrier stands between the rule firing and
     * the site running — the values of the {@link Expr.Let}s enclosing it, outermost first, empty for a site with
     * no binding above it (the unchanged v0 shape). ch. 10.6 clause 2: a leeg need ends its {@code Let} and the
     * body's sites never run, so a companion built WITHOUT them would insert an Inconsistentie for a division
     * that never executed.
     */
    private record Foutplek(Expr plek, List<Behoefte> behoeften, List<Tak> takken) {}

    /** One ch. 4.10 need on the way down to a §6.5/§6.6 site: the binder it binds together with the value that
     *  need IS. The two travel as one pair because the companion has to state them in the same walk order — the
     *  need's own leeg/vol guard, and the binding it then puts in scope for everything below it. */
    private record Behoefte(String sleutel, Expr waarde) {}

    /**
     * What stands between the rule firing and a §6.5/§6.6 site running, as the collectors walk down to it: the
     * ch. 10.6 NEEDS whose barrier could end the {@code Let} first, and the ch. 10.2 branch guards whose arm must
     * be the chosen one. Immutable and handed down the walk, so two siblings can never inherit each other's gate.
     */
    private record Poort(List<Behoefte> behoeften, List<Tak> takken) {
        static final Poort OPEN = new Poort(List.of(), List.of());

        Foutplek plek(Expr e) { return new Foutplek(e, behoeften, takken); }

        /** Behind one more need, and with its binder in scope — a branch guard below may READ that binder, and a
         *  companion has to re-read the need itself to state it. */
        Poort achterBehoefte(String sleutel, Expr waarde) {
            List<Behoefte> b = new ArrayList<>(behoeften);
            b.add(new Behoefte(sleutel, waarde));
            return new Poort(b, takken);
        }

        Poort inTak(Conditie c, boolean positief) {
            List<Tak> t = new ArrayList<>(takken);
            t.add(new Tak(c, positief));
            return new Poort(behoeften, t);
        }
    }

    /** One §6.5/§6.6 companion's WHOLE pattern: the needs and branch guards the site sits behind
     *  ({@code "<vol>, <garde>, "}), followed by the site's own constraint from {@code plekDeel}. null when any
     *  part of it does not decompose into a re-readable form — the companion is then DROPPED, which is the same
     *  scope limit a literal/parameter operand already carries: a fault that degrades to leeg but raises no
     *  Inconsistentie, never a fault invented on a path the barrier or the unentered arm cut off.
     *
     *  <p>{@code plekDeel} is a callback rather than a caller-computed string because it must run INSIDE the
     *  site's CONDITION-side environment: each binder in scope stands for its need's own re-readable read, since
     *  the companion is a separate rule and sees no consequence local. Until 2026-09-19 that environment was
     *  installed around the branch guards alone, so a §6.5 site whose operand was a BINDER — every site inside a
     *  {@code Let} body — decomposed to null and its companion silently vanished, while the same site one `Let`
     *  less deep kept it (measured: {@code heffing := Let k = zijn inkomen in (k gedeeld door zijn restpost)}
     *  reported nothing where the un-bound spelling reported §9.5).
     *
     *  <p><b>One walk, not two</b> (2026-09-19, the second half of that correction). The needs and the
     *  environment are the SAME chain read twice, so they are stated once: each need is decomposed against the
     *  bindings strictly OUTSIDE it and then put in scope for everything below. Decomposing the needs first, in
     *  their own loop, meant a need that was ITSELF a binder ({@code Let k = inkomen in (Let m = k in (m gedeeld
     *  door restpost))}) met an empty environment and dropped the companion — the same miss one level up. The
     *  order is what protects against self-resolution: a need is never resolved through its own binding, so the
     *  environment cannot close a cycle, and a shadowing inner need reads the ENCLOSING binding of that name
     *  (WF-076: the value lives in the enclosing scope), which the collapsed map could not express at all. */
    private String plekGuards(Acc acc, Foutplek fp, Objecttype subject, java.util.function.Supplier<String> plekDeel) {
        StringBuilder b = new StringBuilder();
        java.util.Map<String, OperandDeel> vorige = new java.util.LinkedHashMap<>(acc.binderDeel);
        try {
            for (Behoefte n : fp.behoeften()) {
                OperandDeel d = companionDeel(acc, n.waarde(), subject);
                if (d == null) return null;
                b.append(d.vol()).append(", ");
                acc.binderDeel.put(n.sleutel(), d);
            }
            for (Tak t : fp.takken()) {
                String g = takGarde(acc, t, subject);
                if (g == null) return null;
                b.append(g).append(", ");
            }
            String plek = plekDeel.get();
            return plek == null ? null : b.append(plek).toString();
        } finally {
            acc.binderDeel.clear();
            acc.binderDeel.putAll(vorige);
        }
    }

    /**
     * One ch. 10.2 branch guard as a companion-PATTERN constraint. It is the same {@code rsAggVergelijk} term the
     * consequence's ternary tests, but lowered condition-side — a companion is a separate rule and sees no
     * consequence local, so a binder operand stands for its need's own re-readable read ({@link Acc#binderDeel}).
     * null when any part of that fails, and the caller then drops the companion rather than guard it wrongly.
     */
    private String takGarde(Acc acc, Tak t, Objecttype subject) {
        if (!(t.conditie() instanceof Conditie.Vergelijking v) || !numeriekeOperator(v.op())) return null;
        if (!getalachtig(v.links()) || !getalachtig(v.rechts())) return null;
        Datatype lt = effType(v.links()), rt = effType(v.rechts());
        io.github.yhavinga.regelspraak.model.Eenheid el = lt == null ? null : lt.eenheidOf();
        io.github.yhavinga.regelspraak.model.Eenheid er = rt == null ? null : rt.eenheidOf();
        if (el != null && er != null && el.convertibleNaar(er) && el.vereistOmrekening(er)) return null;
        Frag l = operand(acc, v.links(), subject), r = operand(acc, v.rechts(), subject);
        if (l.marker() || r.marker()) return null;
        String test = fnImport("rsAggVergelijk") + "(" + l.text() + ", " + r.text()
                + ", \"" + Operatoren.symbool(v.op()) + "\")";
        return t.positief() ? test : "!(" + test + ")";
    }

    private static void verzamelWortels(Expr e, Poort poort, List<Foutplek> uit) {
        if (e instanceof Expr.Functie f) {
            if (f.kind() == Expr.FunctieKind.WORTEL) uit.add(poort.plek(f));
            for (Expr a : f.argumenten()) verzamelWortels(a, poort, uit);
        } else if (e instanceof Expr.Binair b) {
            verzamelWortels(b.links(), poort, uit);
            verzamelWortels(b.rechts(), poort, uit);
        } else if (e instanceof Expr.Afgerond af) {
            verzamelWortels(af.basis(), poort, uit);
        } else if (e instanceof Expr.Begrensd bg) {
            verzamelWortels(bg.basis(), poort, uit);
            verzamelWortels(bg.minimum(), poort, uit);
            verzamelWortels(bg.maximum(), poort, uit);
        } else if (e instanceof Expr.Let b) {
            letDescent(b, poort, (x, p) -> verzamelWortels(x, p, uit));
        } else if (e instanceof Expr.IfThenElse k) {
            Conditie.bevatOperand(k.conditie(), x -> { verzamelWortels(x, poort, uit); return false; });
            verzamelWortels(k.dan(), poort.inTak(k.conditie(), true), uit);
            verzamelWortels(k.anders(), poort.inTak(k.conditie(), false), uit);
        }
    }

    /**
     * How the §6.5/§6.6 collectors descend one {@link Expr.Let}: the VALUE is walked only when the body READS the
     * binder (ch. 10.6 clause 3 — an unread need runs no site at all), and the gate the BODY then sits behind
     * (clause 2) is handed back. One statement of the scope rule for both walks.
     *
     * <p>The read may be CONDITIONAL, and then so is the value: a need only one ch. 10.2 arm reads is forced only
     * when that arm runs, so a §6.5 site inside that value sits behind the arm's guard too. Walking the value
     * under the bare gate instead fabricates an Inconsistentie for a division that never executed — the shape
     * {@code synth3-takbarriere} would have hit the day its opslag definition gained one (lens finding
     * 2026-09-18: ref consistent, this branch inconsistent).
     *
     * <p>It walks BOTH sides itself rather than handing the body's gate back, so that {@link #letWissel} has one
     * place to stand for every collector: the exchange changes which node is the outer one, and a caller that
     * kept walking its own {@code b.body()} would have descended the unexchanged tree while the consequence
     * lowered the exchanged one — two trees, one companion, and no way to notice.
     */
    private static void letDescent(Expr.Let b0, Poort poort,
                                   java.util.function.BiConsumer<Expr, Poort> walk) {
        Expr.Let b = letWissel(b0);   // ch. 10.2: the same exchange the consequence lowering makes
        Leesgarde garde = leesGarde(b.body(), letSleutel(b.binder()));
        if (garde instanceof Nooit) { walk.accept(b.body(), poort); return; }
        walk.accept(b.value(), garde instanceof Tak t ? poort.inTak(t.conditie(), t.positief()) : poort);
        // The BODY itself always runs, so it inherits the enclosing gate plus this need — never the arm guard,
        // which belongs to the value alone; a site in the body OUTSIDE the branch node would otherwise lose its
        // companion. A site INSIDE an arm picks that arm's guard up from the branch descent.
        walk.accept(b.body(), poort.achterBehoefte(letSleutel(b.binder()), b.value()));
    }

    /** §6.5 / Tabel 12: a "waarde gedeeld door leeg" is a Fout. The value side already degrades it to leeg
     *  (rsGedeeld returns null for a non-empty dividend over an empty divisor); this surfaces the Fout through
     *  the §9.5 Inconsistentie channel — a SEPARATE rule per division that fires when the dividend is present
     *  and the divisor is empty (and the host rule's voorwaarde holds, so the division would actually execute).
     *  Scope: both operands must lower to a re-readable leeg/vol guard — the subject's OWN attribute, or one
     *  reached by a to-ONE navigation (`partner.inkomen / partner.aftrek`), guarded against a null root. A
     *  literal / parameter / to-many / unresolvable operand still degrades to leeg but raises no Inconsistentie
     *  (carded). An empty LEFT short-circuits to 0 (no fout), so the guard is dividend != null AND divisor ==
     *  null, never merely divisor == null. */
    private void deelDoorLeegFout(Acc acc, ResolvedRegel r, Objecttype subject, String voorwaarde, boolean voorwaardeMarker, List<String> extraClausules) {
        if (voorwaardeMarker) return;          // the host rule is itself a no-op marker -> no division fires
        List<Foutplek> delingen = new ArrayList<>();
        verzamelDelingen(r.waarde(), Poort.OPEN, delingen);
        List<String> patronen = new ArrayList<>();   // "dividend-vol, divisor-leeg" per qualifying division
        for (Foutplek fp : delingen) {
            Expr.Binair d = (Expr.Binair) fp.plek();
            // ch. 10.6 clause 2 / ch. 10.2 gates, then the site itself — the latter in the binder scope the
            // former establishes, so a dividend or divisor that is a BINDER re-reads its need. null = drop.
            String patroon = plekGuards(acc, fp, subject, () -> {
                OperandDeel dividend = companionDeel(acc, d.links(), subject);
                OperandDeel divisor = companionDeel(acc, d.rechts(), subject);
                return dividend == null || divisor == null ? null : dividend.vol() + ", " + divisor.leeg();
            });
            if (patroon != null) patronen.add(patroon);
        }
        for (int i = 0; i < patronen.size(); i++) {
            String pattern = (voorwaarde.isEmpty() ? "" : voorwaarde + ", ") + patronen.get(i);
            Regel rule = new Regel(r.naam() + " (deling door leeg" + (patronen.size() > 1 ? " " + (i + 1) : "") + ")")
                .clausule(SUBJECT + " : " + ident(subject.naam, true) + "( " + pattern + " )");
            for (String e : extraClausules) rule.clausule(e);   // the host rule's far-side membership gate, if any
            out.append(rule
                .geldigheid(r.geldigheid())
                .clausule(inconsistentieGuard(r.naam()))
                .dan(inconsistentieDan(r.naam()))
                .render());
        }
    }

    /** Collect every "gedeeld door"/"gedeeld door (ABS)" node reachable through the arithmetic-wrapper layers
     *  (Binair / Afgerond / Begrensd) of a value expression — for the §6.5 fout surfacing. Aggregation operands
     *  are not descended (an aggregation divisor cannot be a plain re-readable guard). */
    private static void verzamelDelingen(Expr e, Poort poort, List<Foutplek> uit) {
        if (e instanceof Expr.Binair b) {
            if (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.GEDEELD_DOOR_ABS)
                uit.add(poort.plek(b));
            verzamelDelingen(b.links(), poort, uit);
            verzamelDelingen(b.rechts(), poort, uit);
        } else if (e instanceof Expr.Afgerond af) {
            verzamelDelingen(af.basis(), poort, uit);
        } else if (e instanceof Expr.Begrensd bg) {
            verzamelDelingen(bg.basis(), poort, uit);
            verzamelDelingen(bg.minimum(), poort, uit);
            verzamelDelingen(bg.maximum(), poort, uit);
        } else if (e instanceof Expr.Let b) {
            letDescent(b, poort, (x, p) -> verzamelDelingen(x, p, uit));
        } else if (e instanceof Expr.IfThenElse k) {
            // ch. 10.2: a site in the CONDITION always runs; a site in an arm runs only when that arm is chosen.
            Conditie.bevatOperand(k.conditie(), x -> { verzamelDelingen(x, poort, uit); return false; });
            verzamelDelingen(k.dan(), poort.inTak(k.conditie(), true), uit);
            verzamelDelingen(k.anders(), poort.inTak(k.conditie(), false), uit);
        }
    }

    /** THE own-read shape test (IR-SPEC ch. 4.1a): a read without bindings is the subject's (or, in a fold's filter,
     *  the element's) own. Every arm that reads a reference as an own attribute asks this. */
    private boolean ongenavigeerd(Expr.AttribuutRef a) {
        return !navigatieBindingen.containsKey(a);
    }

    /** A read whose bindings walk more than one hop: the §13.4.1 chain. */
    private boolean keten(Expr.AttribuutRef a) {
        return navigatieBindingen.containsKey(a) && navigatieBindingen.get(a).size() > 1;
    }

    /** Sentinel from {@link #naviRol}: the reference is the subject's own attribute. */
    private static final String EIGEN = "";

    /**
     * How to read an attribute reference relative to the rule subject: the reference FIELD to navigate through,
     * {@link #EIGEN} for the subject's own attribute, or null when it cannot be resolved (caller emits a marker).
     *
     * <p>IR-SPEC ch. 4.1a: the bindings are the navigation. A bound read walks them, and a walk that does not fit
     * this subject stays unresolved. A read without bindings is the subject's own, which WF-053 makes a read of
     * the subject's own type; one that names another type is unresolved, never walked by a derivation of its own.
     */
    private String naviRol(Objecttype subject, Expr.AttribuutRef a) {
        if (navigatieBindingen.containsKey(a)) {
            FactModelGenerator.Navigatiewandeling w =
                    FactModelGenerator.navigatiewandeling(model, subject, navigatieBindingen.get(a), a.objecttype());
            return w == null || w.rollen().size() != 1 ? null : w.rollen().get(0);
        }
        return a.objecttype() == subject ? EIGEN : null;
    }

    /** Whether a bare scalar read {@code $subject.getRol().getAttr()} crosses a TO-MANY role
     *  (§13.3.9.1): the subject relates to MANY of {@code a}'s object type through {@code rol},
     *  so a single getter would silently read one of many. Such a read must instead be an
     *  aggregation (de som/maximale/… van … van de <rol>), so it is emitted as a marker. Only a
     *  resolved (non-EIGEN, non-null) role can be to-many. */
    private boolean naarVeel(Objecttype subject, Expr.AttribuutRef a, String rol) {
        return rol != null && !EIGEN.equals(rol) && model.naarVeelRol(subject, a.objecttype(), rol);
    }

    /** True when the reference is bound to a known object type that does NOT declare the
     *  attribute (nor a kenmerk by that name). Emitting `getX()` for it would call a method
     *  the fact has no declaration for — uncompilable DRL — so the caller emits a marker
     *  instead. (A null object type is already handled by {@link #naviRol} returning null.) */
    private static boolean attribuutOntbreekt(Expr.AttribuutRef a) {
        return a.objecttype() != null && !a.objecttype().heeftLid(a.attribuut());
    }

    /** How an attribute reference reads relative to the rule subject, resolved ONCE so the four
     *  context-specific emitters (the value getter, the condition property, the leegwaarde decomposition,
     *  the leeg/gevuld guard) format from it instead of each re-deriving {@link #naviRol} + the
     *  EIGEN/naar-veel/absent branching. {@code EIGEN} = the subject's own attribute; {@code GENAVIGEERD} =
     *  a single to-one navigation through {@code rol}; {@code KETEN} = a resolved §13.4.1 MULTI-hop to-one
     *  chain (the read's bindings, walked from this subject) — lowered only by the value read, every other
     *  emitter treats it as a marker case; the rest are the marker cases. */
    private enum NaviSoort { EIGEN, GENAVIGEERD, KETEN, AFWEZIG, NAAR_VEEL, ONOPGELOST }
    /** {@code pad}: a KETEN's hop roles, subject-outward (empty for every other soort). */
    private record NaviRef(NaviSoort soort, String rol, List<String> pad) {
        NaviRef(NaviSoort soort, String rol) { this(soort, rol, List.of()); }
    }

    private NaviRef naviResolve(Expr.AttribuutRef a, Objecttype subject) {
        if (attribuutOntbreekt(a)) return new NaviRef(NaviSoort.AFWEZIG, null);
        if (keten(a)) {
            // A chain is lowered ONLY when its bindings walk to-one from THIS subject and land on the type that
            // declares the attribute — a chain bound against another subject markers.
            FactModelGenerator.Navigatiewandeling w = FactModelGenerator.navigatiewandeling(
                    model, subject, navigatieBindingen.get(a), a.objecttype());
            if (w == null || w.naarVeel()) return new NaviRef(NaviSoort.ONOPGELOST, null);
            List<String> rollen = w.rollen();
            List<Objecttype> bereikt = w.bereikt();
            Objecttype cur = subject;
            for (int i = 0; i < rollen.size(); i++) {   // EVERY hop needs a field of its own
                if (!navVeld("genavigeerde lezing", cur, rollen.get(i), bereikt.get(i)))
                    return new NaviRef(NaviSoort.ONOPGELOST, null);
                cur = bereikt.get(i);
            }
            return new NaviRef(NaviSoort.KETEN, null, rollen);
        }
        String rol = naviRol(subject, a);
        if (EIGEN.equals(rol))          return new NaviRef(NaviSoort.EIGEN, null);
        if (naarVeel(subject, a, rol))  return new NaviRef(NaviSoort.NAAR_VEEL, rol);
        if (rol != null)                return navVeld("genavigeerde lezing", subject, rol, a.objecttype())
                                            ? new NaviRef(NaviSoort.GENAVIGEERD, rol)
                                            : new NaviRef(NaviSoort.ONOPGELOST, null);
        return new NaviRef(NaviSoort.ONOPGELOST, null);
    }

    /** Whether a hop from {@code ot} over {@code rol} to {@code doel} has a bean field to read or null-test, asked of
     *  the ONE member derivation {@link FactModelGenerator#leden} renders — the SAME question, in the SAME words
     *  ({@link FactModelGenerator#geenReferentieveldReden}), that the exec-model branch asks, so the two branches
     *  refuse the same shapes and diagnose them identically. {@code lezer} names which reader asked.
     *  <p>Until 2026-09-10 this branch asked nothing. A role name a declared member had displaced still emitted
     *  {@code $subject.getWerf().getTarief()}, and the whole KieBase build then died on a BARE compiler error
     *  ({@code Unable to Analyse Expression …}) that named neither the role, the member, nor the limit — loud, but
     *  not a diagnosis. The §8.1.7 rolcheck was worse than loud: {@code werf != null} compiled fine and read the
     *  displacing attribute's VALUE as proof of role membership. Now both marker instead, with {@link #navReden}
     *  carrying the reason into the manifest. */
    private boolean navVeld(String lezer, Objecttype ot, String rol, Objecttype doel) {
        if (FactModelGenerator.heeftReferentieVeld(model, ot, rol, doel)) return true;
        navReden = lezer + ": " + FactModelGenerator.geenReferentieveldReden(ot, rol, doel)
                + " (§13.3.9 opslagrichting)";
        return false;
    }

    /** Why a navigated read markered, when the cause is the §13.3.9 bean-model limit rather than an arm nobody has
     *  built yet. Set by {@link #navVeld} and RESET at every construct entry ({@link #meet}), which is also the only
     *  place it is read — and only for a construct that DID marker, so a reason can never be attached to a lowered
     *  rule. It sharpens the placeholder's own text, which says "navigatie" and cannot name the displacing member. */
    private String navReden;

    /** Whether {@code a} reads a §13.3.7 gedimensioneerd attribuut (its field is a Map&lt;label,value&gt;).
     *  Such a read is only valid through an {@link Expr.DimensieCel} (which pins the label) or as the source
     *  of a dimensieaggregatie; a bare value read of it is under-specified (§3.6). */
    private boolean isGedimensioneerd(Expr.AttribuutRef a) {
        return a.objecttype() != null && !a.objecttype().dimensiesVan(a.attribuut()).isEmpty();
    }

    /** §13.3.6/§8.2: whether {@code a} reads a tijdsafhankelijk (temporal) attribute — its runtime field is a
     *  piecewise {@code List<Segment>} timeline, not a scalar. A BARE read of one in a scalar slot (a when-side
     *  comparison/predicate operand, or a then-side value term) would splice the List into a scalar position — the
     *  List-in-scalar mismatch that sinks the WHOLE KieBuilder package, an accepted input compiling to crashing/
     *  silently-wrong DRL. The LEGITIMATE temporal reads (§7.1 het totaal van / §7.3.2 tijdsevenredig deel / the
     *  §7.1 gedurende clip) intercept the {@link Expr.AttribuutRef} upstream ({@link #tijdreeksAggregatie} /
     *  {@link #temporeleWaarde}) and build the timeline getter directly, so a temporal AttribuutRef reaching the
     *  bare {@code waarde}/{@code operand} leaf is always the not-yet-lowered scalar read — marker it. This also
     *  subsumes the §8.2 "gedurende het gehele jaar / de gehele maand" whole-period qualifier, which the grammar
     *  parses (gehelePeriodevergelijking) but no lowering yet honours: its onderwerp is by definition tijdsafhankelijk
     *  (§8.2 line 3437), so the predicate markers here rather than silently dropping the period check. Detects an own
     *  OR a to-one navigated temporal read alike ({@code a.objecttype()} is the type that DECLARES the attribute). */
    private static boolean isTemporeelAttribuut(Expr.AttribuutRef a) {
        return a.objecttype() != null && bevatNaam(a.objecttype().granulariteitLeden(), a.attribuut());
    }

    /** The getter chain for an attribute read relative to the subject — the EIGEN own-attribute, a single
     *  navigated reference, or a marker for an absent/naar-veel/unresolvable navigation. Shared by the plain
     *  value read and the dimensioned-read forms (which need the bare getter — for a dimensioned attribute
     *  the Map field — before indexing a label), so the latter bypass the §3.6 under-specified guard. */
    private Frag attribuutGetter(Expr.AttribuutRef a, Objecttype subject) {
        NaviRef n = naviResolve(a, subject);
        return switch (n.soort()) {
            case EIGEN       -> Frag.ok(SUBJECT + "." + getter(a.attribuut()) + "()");
            case GENAVIGEERD -> Frag.ok(SUBJECT + "." + getter(n.rol()) + "()." + getter(a.attribuut()) + "()");
            case NAAR_VEEL   -> Frag.todo("naar-veel-navigatie", a.bron());
            default          -> Frag.todo("navigatie", a.bron());   // AFWEZIG / ONOPGELOST
        };
    }

    /** How a +/- right operand is restated in the left's unit — the null-guarded ternary form on the value side
     *  ({@link #omrekenWaarde}), the null-safe rsHerschaalExact helper form in a constraint ({@link #herschaalConditie}). */
    @FunctionalInterface private interface Omrekenen { Frag apply(Frag expr, Datatype van, Datatype naar); }

    /** §6.2-6.8 binary arithmetic, shared by the value side ({@link #waarde}) and the condition side
     *  ({@link #operand}). The sub-operands are lowered by {@code arg} (a $subject getter on the value side, a
     *  bare property on the condition side); a +/- right operand is restated in the left's unit by {@code omrekenen}.
     *  Every leegwaarde form is a registered helper or inline arithmetic (rsNul/rsGedeeld/rsGedeeldAbs/rsVerminderd/
     *  rsMacht), i.e. a method call valid in either a modify-block or a pattern constraint — the only ternary is in
     *  {@code omrekenen}, which the condition side supplies ternary-free. A marker operand propagates unwrapped:
     *  {@link Frag#from} ORs its {@link Frag#marker} flag into the result. */
    private Frag binairTerm(Acc acc, Expr.Binair b, Function<Expr, Frag> arg, Omrekenen omrekenen) {
        if (b.op() == Expr.BinOp.TOT_DE_MACHT) {
            // §13.4.17.14: an INTEGER (literal) exponent — the common case — is computed exactly with
            // BigDecimal.pow(int); going through Math.pow (a double) would lose integer precision above 2^53
            // (7^19 off by 1) before the mandatory afronding. §6.7: any empty operand -> leeg, so the base goes
            // through a null-guarding helper (the integer exponent is a literal, never empty). A marker base
            // propagates unwrapped, like the other operators.
            Integer n = geheelExponent(b.rechts());
            if (n != null) {
                Frag l = arg.apply(b.links());
                if (l.marker()) return Frag.from("(" + l.text() + ").pow(" + n + ")", l);
                fnImport("rsMacht");
                return Frag.from("rsMacht(" + l.text() + ", " + n + ")", l);
            }
            // a genuinely fractional/runtime exponent: Math.pow yields a double, wrapped as a BigDecimal.
            Frag l = arg.apply(b.links()), r = arg.apply(b.rechts());
            if (l.marker() || r.marker())
                return Frag.from("java.math.BigDecimal.valueOf(Math.pow((" + l.text() + ").doubleValue(), (" + r.text() + ").doubleValue()))", l, r);
            fnImport("rsMachtReeel");
            return Frag.from("rsMachtReeel(" + l.text() + ", " + r.text() + ")", l, r);
        }
        if (b.op() == Expr.BinOp.PERCENTAGE_VAN) {
            // §486: route through the Breuk-aware rsPercentageVan (single-runtime, shared with the exec back-end) so
            // a non-terminating percentage ((1/3)% van 600) keeps its exact rational until an afronding — inline mvel
            // `l/100*r` computes at DECIMAL128 and would diverge. leeg counts as 0 inside the helper.
            Frag l = arg.apply(b.links()), r = arg.apply(b.rechts());
            fnImport("rsPercentageVan");
            return Frag.from("rsPercentageVan(" + l.text() + ", " + r.text() + ")", l, r);
        }
        if (b.op() == Expr.BinOp.GEDEELD_DOOR) {
            // §6.5 Tabel 12 leegwaarde: an empty LEFT (dividend) short-circuits to 0 — even leeg/leeg = 0; a
            // non-empty value over an empty divisor is a "fout", lowered as leeg (null) and surfaced as an
            // Inconsistentie by the companion rule. rsGedeeld carries that order and the null-guard a bare .divide
            // would NPE on.
            Frag l = arg.apply(b.links()), r = arg.apply(b.rechts());
            if (l.marker() || r.marker())
                return Frag.from("(" + l.text() + ").divide(" + r.text() + ", java.math.MathContext.DECIMAL64)", l, r);
            fnImport("rsGedeeld");
            return Frag.from("rsGedeeld(" + l.text() + ", " + r.text() + ")", l, r);
        }
        if (b.op() == Expr.BinOp.GEDEELD_DOOR_ABS) {
            // §6.5: "gedeeld door (ABS)" has at most 5 decimals, rounded TOWARD ZERO (divide(d, 5, DOWN)); same
            // §6.5 leegwaarde as plain gedeeld door (left leeg -> 0, value/leeg -> leeg).
            Frag l = arg.apply(b.links()), r = arg.apply(b.rechts());
            if (l.marker() || r.marker())
                return Frag.from("(" + l.text() + ").divide(" + r.text() + ", 5, java.math.RoundingMode.DOWN)", l, r);
            fnImport("rsGedeeldAbs");
            return Frag.from("rsGedeeldAbs(" + l.text() + ", " + r.text() + ")", l, r);
        }
        if (b.op() == Expr.BinOp.MAAL) {  // §6.4: units multiply, operands are NOT converted to match
            // §389: route through the Breuk-aware rsMaal (single-runtime) so exactness propagates when an operand is
            // a Breuk (9 maal 1/3 = 3), else the fast BigDecimal path. §6.4 Tabel 10: any empty operand -> 0 (in helper).
            Frag l = arg.apply(b.links()), r = arg.apply(b.rechts());
            if (l.marker() || r.marker()) return Frag.from("(" + l.text() + " * " + r.text() + ")", l, r);
            fnImport("rsMaal");
            return Frag.from("rsMaal(" + l.text() + ", " + r.text() + ")", l, r);
        }
        // §6/§13.4.16 date arithmetic: a datum/datumtijd ± a numeric-with-time-unit -> a new date (optellen=later,
        // aftrekken=earlier; an aantal of 0 leaves the date unchanged). NOT mvel "+": "LocalDate + BigDecimal" is a
        // type error that breaks the Drools build. Lower via the Java-bodied rsDatumReken helper (.plus/.minus keyed
        // on the ChronoUnit), exactly like rsTijdsduur — the enum/overload resolve cleanly in plain Java, not mvel.
        if (eff.typeVan(b.links()).isDatum() || eff.typeVan(b.rechts()).isDatum()) {
            boolean datumLinks = eff.typeVan(b.links()).isDatum();
            Expr datumExpr = datumLinks ? b.links() : b.rechts();
            Expr durExpr   = datumLinks ? b.rechts() : b.links();
            boolean optellen = b.op() == Expr.BinOp.PLUS;
            // "datum - duur" subtracts; "duur - datum" is not a date, and two datums added/subtracted is not date
            // arithmetic (a datum-difference is the §6.10 tijdsduur expression) -> honest marker.
            if (eff.typeVan(durExpr).isDatum() || (!optellen && !datumLinks))
                return Frag.todo("datum-rekenkunde (datum +/- tijdsduur; geen datum-datum)", b.bron());
            // §6.11 datum ± N <dagsoort> ("5 werkdagen"): not a fixed ChronoUnit — step day-by-day counting only the
            // dagsoort days (the §9.8 predicate). X never counts; the result is always a dagsoort day; 0/leeg -> X
            // unchanged. A dagsoort is day-granular, so the date operand must be a Datum in dagen (a LocalDate).
            // ANNOTATION BY DESIGN (WF-035, RUNTIME-SEMANTICS ch. 2.10 exception 2): the duration operand's
            // calendar unit is what routes date arithmetic, and ref routes on the same static type.
            io.github.yhavinga.regelspraak.model.Eenheid durEenheid = eff.typeVan(durExpr).eenheidOf();
            // ch. 3.8 puts the CALENDAR TABLE first — see Tijdrekening.dagsoortVoorEenheid for the order and why.
            String dagsoortUnit = Tijdrekening.dagsoortVoorEenheid(model, durEenheid);
            if (dagsoortUnit != null) {
                // §4.1/§4.2 Tabel 2/5 rule 2: a datum-EN-tijd left operand yields a datum-en-tijd result — step over the
                // date part and re-attach the time-of-day (rsDatumTijdPlusDagsoort). A Datum-in-dagen keeps the LocalDate
                // path. The walking loop lives in the runtime; the §9.8 predicate rides a rsDagsoort_<Naam> global.
                boolean datumTijd = eff.typeVan(datumExpr) instanceof Datatype.DatumTijd;
                String glob = dagsoortGlobaal(dagsoortUnit);
                Frag d = arg.apply(datumExpr), n = arg.apply(durExpr);
                if (glob == null || d.marker() || n.marker()) return Frag.todo("datum-rekenkunde (dagsoort)", b.bron());
                String stapFn = fnImport(datumTijd ? "rsDatumTijdPlusDagsoort" : "rsDatumPlusDagsoort");
                String cast = datumTijd ? "java.time.LocalDateTime" : "java.time.LocalDate";
                return Frag.from("((" + cast + ") " + stapFn + "(" + d.text() + ", " + n.text() + ", " + glob + ", " + optellen + "))", d, n);
            }
            String unit = Tijdrekening.chronoUnit(eff.typeVan(durExpr));
            if (unit == null) return Frag.todo("datum-rekenkunde (dagsoort/onbekende tijdseenheid)", b.bron());
            boolean tijdEenheid = switch (unit) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
            if (tijdEenheid && !(eff.typeVan(datumExpr) instanceof Datatype.DatumTijd))
                return Frag.todo("datum-rekenkunde: een tijdseenheid vereist een datum-tijd operand (§6.10)", b.bron());
            Frag d = arg.apply(datumExpr), n = arg.apply(durExpr);
            if (d.marker() || n.marker()) return Frag.todo("datum-rekenkunde", b.bron());
            fnImport("rsDatumReken");
            String cast = eff.typeVan(datumExpr) instanceof Datatype.DatumTijd ? "java.time.LocalDateTime" : "java.time.LocalDate";
            return Frag.from("((" + cast + ") rsDatumReken(" + d.text() + ", " + n.text() + ", \"" + unit + "\", " + optellen + "))", d, n);
        }
        // §4: restate the right operand in the left operand's unit before +/- so a convertible-but-different unit
        // (mnd vs jr) is scaled, not added raw.
        Frag links = arg.apply(b.links());
        Frag rechts = omrekenen.apply(arg.apply(b.rechts()), effType(b.rechts()), effType(b.links()));
        if (links.marker() || rechts.marker())     // a marker propagates unwrapped (no leegwaarde noise)
            return Frag.from("(" + links.text() + " " + infix(b.op()) + " " + rechts.text() + ")", links, rechts);
        // §6.3 Tabel 7/8 — the ONE place "min" and "verminderd met" differ: both subtract, but a "verminderd met"
        // whose LEFT operand is empty yields empty (rsVerminderd), while "min" treats an empty operand as 0 like
        // plus (rsNul). Without this they lowered identically — a silent-wrong for a leeg-left case.
        if (b.op() == Expr.BinOp.VERMINDERD_MET) {
            fnImport("rsVerminderd");
            return Frag.from("rsVerminderd(" + links.text() + ", " + rechts.text() + ")", links, rechts);
        }
        // §6.2/§6.3: route PLUS/MIN through the Breuk-aware rsPlus/rsMin (single-runtime, shared with the exec
        // back-end) so exactness propagates when an operand is a Breuk; leeg counts as 0 inside the helper. Inline
        // mvel `l + r` would compute at DECIMAL128 and cannot carry a Breuk. (A marker propagated above.)
        String plusFn = fnImport(b.op() == Expr.BinOp.PLUS ? "rsPlus" : "rsMin");
        return Frag.from(plusFn + "(" + links.text() + ", " + rechts.text() + ")", links, rechts);
    }

    // ---------------- value expression ("then" side: getters) ----------------
    private Frag waarde(Acc acc, Expr e, Objecttype subject) {
        // IR-SPEC ch. 4.3 / WF-110 (a): the §5.7 instance-set union denotes INSTANCES, so it is admissible as
        // the single argument of a count and at no other position — a count consumes it in accumulate() and
        // never reaches here. A union that DOES reach here stands where a VALUE is written, which is
        // ill-formed input; it is named here rather than left to the generic "expressie" fall-through, for
        // the reason the fall-through itself cannot serve: that sentence describes the POSITION and
        // identifies no construct, and this branch additionally CRASHED on it — the node is the only
        // loadable expression with a null type(), and the §4 unit restate reads that type unguarded
        // (measured 2026-09-22 on corpus/negatief/wf110-unie-als-regelwaarde: an ERROR where ref, clingo and
        // the exec-model branch all refuse by name).
        if (e instanceof Expr.InstanceSetUnion u)
            return Frag.todo("instantie-unie op een waardeplek: zij duidt de INSTANTIES van een §5.7-concatenatie"
                    + " aan en alleen een instantietelling verbruikt ze (WF-110 (a))", u.bron());
        if (e instanceof Expr.Literal lit)      return literal(lit);
        if (e instanceof Expr.TekstReeks tr)    return tekstReeks(acc, tr, x -> waarde(acc, x, subject));
        if (e instanceof Expr.ParameterRef p)   // §13.3.6 a TEMPORAL parameter is a List<Segment> global — only a
            // §7.3.2 tijdsevenredig base may read it (handled there); any scalar use is a List-in-scalar mismatch -> marker.
            return p.parameter().granulariteit() != null ? Frag.todo("temporele parameter in scalaire context", p.bron())
                    : Frag.ok(ident(p.parameter().naam(), false));
        if (e instanceof Expr.AttribuutRef a) {
            // §3.6: a gedimensioneerd attribuut read WITHOUT pinning its dimensions is under-specified (it
            // would emit a bare Map getter where a scalar is expected). A legitimate dimensioned read is an
            // Expr.DimensieCel; a bare AttribuutRef to a dimensioned attribute is therefore an error — marker.
            if (isGedimensioneerd(a)) return Frag.todo("ongedimensioneerde lezing van gedimensioneerd attribuut (§3.6)", a.bron());
            // §13.3.6/§8.2: a bare scalar read of a tijdsafhankelijk attribuut (a List<Segment> timeline) markers —
            // the legitimate temporal value reads are intercepted upstream (see isTemporeelAttribuut).
            if (isTemporeelAttribuut(a)) return Frag.todo("tijdsafhankelijk attribuut in scalaire context (§8.2)", a.bron());
            NaviRef n = naviResolve(a, subject);
            if (n.soort() == NaviSoort.GENAVIGEERD) {
                // §6.9 / §13.3.9: a value read THROUGH a to-one reference that is empty is leeg, not a crash. The
                // modify-block consequence runs for EVERY instance of the subject type (the rule's pattern is over
                // that type), including ones whose reference is null — and a bare $subject.getPartner().getX()
                // NPEs there, which inside a Drools consequence aborts the WHOLE fireAllRules pass. Guard the root
                // so a null reference yields null (the leegwaarde), which the surrounding helpers then propagate.
                // (The condition side already guards via genavigeerdLeegGevuld / "!."; only this value read did not.)
                String root = SUBJECT + "." + getter(n.rol()) + "()";
                return Frag.ok("(" + root + " == null ? null : " + root + "." + getter(a.attribuut()) + "())");
            }
            if (n.soort() == NaviSoort.KETEN) {
                // §13.4.1/§13.3.9 multi-hop to-one chain: one getter per declared role hop, EVERY relation prefix
                // null-guarded so an intervening leeg relation reads as leeg (never an NPE-abort) — the chain
                // generalisation of the single-hop root-guard above.
                List<String> guards = new ArrayList<>();
                String pad = SUBJECT;
                for (String rol : n.pad()) {
                    pad = pad + "." + getter(rol) + "()";
                    guards.add(pad + " == null");
                }
                return Frag.ok("(" + String.join(" || ", guards) + " ? null : " + pad + "." + getter(a.attribuut()) + "())");
            }
            return attribuutGetter(a, subject);
        }
        if (e instanceof Expr.DimensieCel c) {
            // §3.6 a single dimensioned cell reads the attribute's Map<label,value> field and indexes the
            // selected label. Scope: a single-dimension attribute (one label); a multi-dimension composite
            // key is deferred to a marker. An unresolvable underlying read (e.g. a navigation) markers too.
            Frag map = attribuutGetter(c.attribuut(), subject);
            if (map.marker() || c.labels().size() != 1) return Frag.todo("dimensiecel", c.bron());
            // §6.9/§13.3.9: a null gedimensioneerd Map (a default-constructed fact) reads as LEEG, not a crash — guard
            // the .get so it does not NPE-abort the consequence (the exec-model's scalarWaarde DimensieCel is already
            // null-safe; this mirrors the value-side GENAVIGEERD root-guard so both back-ends read a null Map as leeg).
            String cel = map.text(), label = c.labels().get(0);
            return Frag.ok("(" + cel + " == null ? null : (" + cel + ").get(" + drlString(label) + "))");
        }
        if (e instanceof Expr.Binair b)
            // §6.2-6.8 arithmetic, shared with the condition side: value getters for the sub-operands, and a
            // +/- right operand restated in the left's unit by the NULL-GUARDED omrekenWaarde (a "then"-side
            // ternary is fine here).
            return binairTerm(acc, b, x -> waarde(acc, x, subject), this::omrekenWaarde);
        if (e instanceof Expr.Afgerond af) {
            return afgerondeTerm(waarde(acc, af.basis(), subject), af);
        }
        if (e instanceof Expr.Begrensd bg) {
            // §4/§6.1.4: a convertible-but-different bound unit (a jr bound on a mnd basis) is
            // restated in the basis unit before the clamp compares magnitudes, exactly as the
            // +/- path does. No-op when the units match or are absent. Route through the Breuk-aware
            // rsBegrens (single-runtime, shared with the exec back-end): an inline mvel ternary
            // `r < m ? m : r` cannot compare a Breuk basis/bound, and clamps EXACTLY inside the helper.
            Frag r = waarde(acc, bg.basis(), subject);
            Datatype bt = effType(bg.basis());
            Frag min = bg.minimum() == null ? null : omrekenWaarde(waarde(acc, bg.minimum(), subject), effType(bg.minimum()), bt);
            Frag max = bg.maximum() == null ? null : omrekenWaarde(waarde(acc, bg.maximum(), subject), effType(bg.maximum()), bt);
            java.util.List<Frag> delen = new java.util.ArrayList<>(); delen.add(r);
            if (min != null) delen.add(min);
            if (max != null) delen.add(max);
            fnImport("rsBegrens");
            return Frag.from("rsBegrens(" + r.text() + ", " + (min == null ? "null" : min.text())
                    + ", " + (max == null ? "null" : max.text()) + ")", delen.toArray(new Frag[0]));
        }
        if (e instanceof Expr.Constante c) {
            Frag rk = rekenConstante(c);   // §5.3 Rekendatum/Rekenjaar as a VALUE operand (§13.4.17.34)
            return rk != null ? rk : Frag.todo("constante", c.naam());
        }
        if (e instanceof Expr.BinderRef br)     return binderLezing(acc, br);
        if (e instanceof Expr.Let b)            return letWaarde(acc, b, subject);
        if (e instanceof Expr.IfThenElse k)     return keuzeWaarde(acc, k, subject);
        if (e instanceof Expr.Functie f) {
            // ch. 4.10: a binder read inside an aggregation would have to be re-read from the WHEN (that is where
            // the accumulate lives) while its need is a CONSEQUENCE-local — the two cannot meet. The frontend
            // SUBSTITUTES a read inside a shape-reading construct instead of emitting a BinderRef there
            // (RUNTIME-SEMANTICS ch. 10.6, tracked delta 1), so a document carrying one is refused here BY NAME
            // rather than through a bare Drools compile error about an unresolvable identifier.
            if (Expr.bevat(f, x -> x instanceof Expr.BinderRef))
                return Frag.todo("binderlezing binnen een aggregatie (ch. 4.10)", f.bron());
            Frag scalar = scalarFunctie(acc, f, x -> waarde(acc, x, subject)); // §13.4.17 single-value builtins (abs, datum-delen, datum-met)
            if (scalar != null) return scalar;
            Frag dim = dimensieAggregatie(acc, f, subject);      // SOM/min/max over a dimension's labels
            if (dim != null) return dim;
            Frag concat = concatenatieAggregatie(acc, f, subject); // pairwise min/max over listed operands
            if (concat != null) return concat;
            Frag tijd = tijdreeksAggregatie(acc, f, subject);    // §7.1 "het totaal van" a temporal attribute's timeline
            if (tijd != null) return tijd;
            Frag var = accumulate(acc, f, subject, false);       // hoists an accumulate into the "when" (value side: bare fold)
            if (var != null) return var;
            // A window under GEEN or a counting quantifier in the die/dat filter is WF-112's position; the filter's
            // own marker is dropped on the way up, so the rule is named here, in the sentence the exec branch gives.
            String vensterWeigering = Vensterpositie.weigering(f.filter());
            return Frag.todo(vensterWeigering != null ? vensterWeigering : "aggregatie " + f.kind(), f.bron());
        }
        return Frag.todo("expressie", e.bron());
    }

    // ---------------- IR-SPEC ch. 4.10 / RUNTIME-SEMANTICS ch. 10.6: the value-level binding ----------------

    /**
     * WF-064 a read of the nearest enclosing binder: the consequence-local variable its need was forced into.
     * The binding is a real one — the need is evaluated ONCE and every read is the same variable — so a §9.5
     * channel reason, a fault or a leeg comes from one evaluation, not from one per read, which is what a text
     * substitution of the value would have made of it.
     *
     * <p>A read with no binding in scope is a document this branch cannot lower (WF-064 says there is always
     * one, so it never comes off the wire verifier-clean): an honest marker, never a silent leeg.
     */
    private Frag binderLezing(Acc acc, Expr.BinderRef br) {
        String var = acc.binders.get(letSleutel(br.naam()));
        return var == null ? Frag.todo("ongebonden binderlezing (WF-064)", br.bron()) : Frag.ok(var);
    }

    /**
     * ch. 10.6 lowered as CONTROL FLOW, not as substitution. The need is forced into a fresh consequence-local
     * mvel variable ({@code $behoefteN}) by one PRELUDE line, and the {@code Let} itself renders as the
     * leeg-BARRIER over it: {@code ($behoefteN == null ? null : <body>)}.
     *
     * <p>The three clauses, each a piece of that shape:
     * <ul>
     *   <li><b>1 — lazy, and once.</b> One prelude line per need, one variable, every read the same variable.
     *       Re-emitting the value text at each read would evaluate it once per read; the shared local is what
     *       makes "one need" true here, and it is why the body's units, its fout companions and its freshness
     *       watch keep speaking about the SAME evaluation.</li>
     *   <li><b>2 — the barrier at the read, owned by the binding Let.</b> The {@code == null} ternary sits AT
     *       this node, so a leeg need empties exactly THIS {@code Let} and nothing above it: a sibling
     *       {@code Let} under the same {@code plus} keeps its own answer (and the §6.2 leegwaarde table then
     *       coalesces this one), where a consequence-wide guard would have emptied the whole value.</li>
     *   <li><b>3 — an unread binder evaluates nothing.</b> A body that does not read the binder gets NO prelude
     *       line and NO barrier: the {@code Let}'s outcome IS the body's, and the value is never evaluated —
     *       no leeg, no fault. {@link #leestBinder} decides that statically, which is exact in the subset this
     *       branch lowers (every other form it lowers evaluates all its children).</li>
     * </ul>
     *
     * <p><b>Where the forcing sits, and why the prelude line is itself guarded.</b> The needs are forced BEFORE
     * the body rather than at the textual read, which is observationally identical here (the values are pure and
     * every read that exists is reached) — but only if a need the barrier would have cut off is not forced
     * anyway: {@code rsGedeeld} by a numeric zero THROWS, and a throw inside a consequence aborts the whole
     * fire. So each prelude line carries the conjunction of the needs ENCLOSING it ({@link Acc#letBewaking}),
     * and a chain whose outer need is leeg forces no inner one. What remains, and is stated rather than hidden:
     * ch. 10.6 orders the reads left-to-right, so a §6.5 site standing BEFORE the read that raises the barrier
     * does run in {@code ref} and does not here — a MISSED fault, never an invented one, and the §6.5/§6.6
     * companions are gated by the same needs so they agree with this lowering.
     */
    private Frag letWaarde(Acc acc, Expr.Let b0, Objecttype subject) {
        if (acc.letVoorspel == null)   // no consequence to carry the need — honest marker, never a half lowering
            return Frag.todo("Let buiten de resultaatwaarde van een berekening (ch. 4.10)", b0.bron());
        Expr.Let b = letWissel(b0);    // ch. 10.2: the guard's binder must stand ABOVE the need it guards
        String sleutel = letSleutel(b.binder());
        Leesgarde garde = leesGarde(b.body(), sleutel);
        if (garde instanceof Nooit) return waarde(acc, b.body(), subject);   // clause 3
        if (garde instanceof Onbepaald)
            return Frag.todo("behoefte gelezen onder twee verschillende takgardes (ch. 10.2/10.6)", b.bron());
        // A read confined to ONE ch. 10.2 arm forces the need only when that arm runs, and its barrier fires only
        // then too. The guard is lowered in the CURRENT scope — this binder is deliberately not bound yet (a
        // condition reading it would have answered ALTIJD). A guard whose own binder is bound INSIDE the body is
        // a different name here than it is at the branch node, and {@link #letWissel} above has already moved
        // what it can: what is left really cannot be stated at this position, so it markers by name instead of
        // resolving against whatever enclosing binding happens to carry the same name.
        String takTekst = null;
        if (garde instanceof Tak t) {
            Frag test = gardeLeestBinnenBinder(t.conditie(), b.body()) ? null : keuzeConditie(acc, t.conditie(), subject);
            if (test == null || test.marker())
                return Frag.todo("takgarde van een voorwaardelijk gelezen behoefte (ch. 10.2)", b.bron());
            takTekst = t.positief() ? test.text() : "!(" + test.text() + ")";
        }
        // WF-076: the value is lowered in the ENCLOSING scope — the binder is not visible in its own value, so it
        // is bound only after this call.
        Frag v = waarde(acc, b.value(), subject);
        String var = "$behoefte" + (++acc.letN);
        List<String> poorten = new ArrayList<>(acc.letBewaking);
        if (takTekst != null) poorten.add(takTekst);
        String bewaakt = poorten.isEmpty() ? v.text()
                : "(" + String.join(" && ", poorten) + ") ? " + v.text() + " : null";
        acc.letVoorspel.add("    " + var + " = " + bewaakt + ";\n");
        String vorige = acc.binders.put(sleutel, var);   // WF-064: an inner binder SHADOWS, it never overwrites
        // ch. 10.6 clause 2, at the binding Let. Under a branch guard the barrier carries it too: a need the
        // unentered arm never read is null because it was never forced, and that must NOT empty the Let.
        String barriere = takTekst == null ? var + " == null" : "(" + takTekst + ") && " + var + " == null";
        // What stands between a DEEPER need and the consequence is this Let's BARRIER, not this need's emptiness
        // — and under a branch guard the two are different propositions. On the arm that does not read this need
        // it is never forced, so the variable is null there for a reason that has nothing to do with ch. 10.6: a
        // deeper need gated on `!= null` was then not forced EITHER, and its own barrier (which carries the OTHER
        // arm's guard) emptied its Let. Measured before this line said `!barriere`: `Let k in Let z = k/restpost
        // in Let w = 1,00 in (als k > 10,00 dan w anders z)` answered leeg on the dan-arm where ref, clingo and
        // the exec branch answer 1,00, and with a coalescing wrapper around the branch node 5,00 where all three
        // answer 6,00 — the barrier shrunk to the arm. The unguarded spelling stays `var != null` verbatim: it is
        // the same proposition, and every document without a conditionally-read need keeps its DRL byte for byte.
        acc.letBewaking.add(takTekst == null ? var + " != null" : "!(" + barriere + ")");
        Frag body;
        try {
            body = waarde(acc, b.body(), subject);
        } finally {
            acc.letBewaking.remove(acc.letBewaking.size() - 1);
            if (vorige == null) acc.binders.remove(sleutel); else acc.binders.put(sleutel, vorige);
        }
        return Frag.from("(" + barriere + " ? null : " + body.text() + ")", v, body);
    }

    // ---------------- IR-SPEC ch. 4.10 / RUNTIME-SEMANTICS ch. 10.2: the value-level conditional ----------------

    /**
     * ch. 10.2: evaluate the guard, then EXACTLY the chosen branch — strict, no coalesce, the other arm not
     * evaluated at all. An mvel ternary is that semantics verbatim: it evaluates one arm, so a §6.5 division in
     * the arm not taken never runs (and the companion built for it carries the same guard, {@link #takGarde}).
     */
    private Frag keuzeWaarde(Acc acc, Expr.IfThenElse k, Objecttype subject) {
        Frag test = keuzeConditie(acc, k.conditie(), subject);
        if (test == null) return Frag.todo("ch. 10.2-conditie buiten de gelowerde vorm", k.bron());
        // The ternary evaluates one arm, but a Let INSIDE an arm hoists its need into the shared consequence
        // prelude, which runs before the ternary does. So the arm's own guard joins {@link Acc#letBewaking} for
        // the length of that arm: a need bound in the arm not taken is then forced under a false guard and stays
        // unevaluated, exactly as ch. 10.2 says. Without this the prelude carried only the ENCLOSING needs and a
        // division inside the unchosen arm really ran (lens finding 2026-09-18: ref answered 5,00 while this
        // branch aborted the fire with "Breuk deling door 0").
        Frag dan = inTak(acc, test.text(), () -> waarde(acc, k.dan(), subject));
        Frag anders = inTak(acc, "!(" + test.text() + ")", () -> waarde(acc, k.anders(), subject));
        return Frag.from("(" + test.text() + " ? " + dan.text() + " : " + anders.text() + ")", test, dan, anders);
    }

    /** Lower one ch. 10.2 arm with its guard on the need-forcing gate, popped again however the lowering ends. */
    private Frag inTak(Acc acc, String garde, java.util.function.Supplier<Frag> lowering) {
        acc.letBewaking.add(garde);
        try {
            return lowering.get();
        } finally {
            acc.letBewaking.remove(acc.letBewaking.size() - 1);
        }
    }

    /**
     * The ch. 10.2 branch guard as a single mvel term, through the SHARED {@code rsAggVergelijk} — the same helper
     * the exec branch's own ch. 10.2 lowering uses, so both branches read one table for the leeg cells (§5.3
     * Tabel 37 / §5.6 Tabel 44 / Tabel 33) and compare an exact Breuk exactly.
     *
     * <p><b>The leeg flank, measured 2026-09-18 rather than assumed.</b> WF-077 admits only binders, parameters
     * and literals (with arithmetic combinators over them) as condition operands, and each of the three is
     * non-leeg where it is read: a binder read either yields a gevuld value or ends its {@code Let} first
     * (ch. 10.6 clause 2), a parameter that a case does not supply is a FAIL-FAST fault in {@code ref}
     * ("parameter … is unbound in this case", ch. 5.7 — measured, it is not leeg), and a literal has a lexeme.
     * So a verifier-clean document cannot present a leeg guard operand at all, which is exactly what WF-077 is
     * for. Routing through {@code rsAggVergelijk} nevertheless means that a document that somehow does gets
     * {@code ref}'s own table rather than an mvel accident.
     *
     * <p>null — an honest marker at the caller — for everything else, mirroring the exec branch's refusals one
     * for one: a non-comparison condition, a non-numeric operator, a non-numeric operand, an operand that does
     * not lower, and a comparison that would need a §5.1 Tabel 32 UNIT restatement, which {@code rsAggVergelijk}
     * does not apply. No half lowering.
     */
    private Frag keuzeConditie(Acc acc, Conditie c, Objecttype subject) {
        if (!(c instanceof Conditie.Vergelijking v) || !numeriekeOperator(v.op())) return null;
        if (!getalachtig(v.links()) || !getalachtig(v.rechts())) return null;
        Datatype lt = effType(v.links()), rt = effType(v.rechts());
        io.github.yhavinga.regelspraak.model.Eenheid el = lt == null ? null : lt.eenheidOf();
        io.github.yhavinga.regelspraak.model.Eenheid er = rt == null ? null : rt.eenheidOf();
        Eenheidsconflict.eisHerschaalbaar(el, er, "een ch. 10.2-takgarde (DRL-tekst)");
        if (el != null && er != null && el.convertibleNaar(er) && el.vereistOmrekening(er)) return null;
        Frag l = waarde(acc, v.links(), subject), r = waarde(acc, v.rechts(), subject);
        if (l.marker() || r.marker()) return null;
        fnImport("rsAggVergelijk");
        return Frag.ok("rsAggVergelijk(" + l.text() + ", " + r.text() + ", \"" + Operatoren.symbool(v.op()) + "\")");
    }

    /** The comparison operators a ch. 10.2 guard lowers — the numeric six; a date operator is refused, exactly as
     *  the exec branch refuses it (mirrors {@code ExecModelTranspiler.isNumeriekOp}). */
    private static boolean numeriekeOperator(Conditie.Operator op) {
        return switch (op) {
            case KLEINER, KLEINER_OF_GELIJK, GROTER, GROTER_OF_GELIJK, GELIJK, ONGELIJK -> true;
            default -> false;
        };
    }

    /** Whether an operand is number-like enough for {@code rsAggVergelijk} (mirrors the exec branch's
     *  {@code getalachtigVergelijkbaar}); an unknown-typed operand is refused rather than guessed. */
    private boolean getalachtig(Expr e) {
        return e != null && eff.typeVan(e) != null && eff.typeVan(e).isGetalachtig();
    }

    /**
     * §13.4.17 the SCALAR built-in functions — those that map to a single mvel/Java expression rather than an
     * accumulate over instances: the absolute value, the date/time component extractions, and the date /
     * datetime constructors, plus the tijdsduur between two dates. Returns null for every other
     * {@link Expr.FunctieKind} (the aggregations and the not-yet-lowered builtins like concatenatie/paasdag),
     * so the caller falls through to the accumulate path or its marker. A marker propagates straight through
     * when an argument is itself unresolved.
     *
     * <p>{@code arg} lowers an argument expression: on the VALUE side it is {@link #waarde} (a {@code
     * $subject.getX()} getter), on the CONDITION side {@link #operand} (the bare property a pattern constraint
     * needs, since {@code $subject} is not bound inside its own pattern). Every scalar builtin lowers to a
     * single mvel term valid in either position, so the one routine serves both — the aggregations, which need
     * a {@code when}-clause accumulate, stay null here and let each caller fall through to its marker.
     */
    private Frag scalarFunctie(Acc acc, Expr.Functie f, Function<Expr, Frag> arg) {
        List<Expr> a = f.argumenten();
        if (f.kind() == Expr.FunctieKind.TIJDSDUUR || f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR)
            return tijdsduur(f, arg);
        // §13.4.17.18-.24 / §6.12 a DATE or TIME component (jaar/maand/dag/uur/minuut/seconde/milliseconde uit).
        // All seven read as a Numeriek via one ChronoField, lowered through the rsDatumDeel helper that carries
        // the §6.12 / typeringen Tabel 28 leegwaarde rule — an empty input yields an empty result. That null
        // guard is essential, not cosmetic: a bare "(d).getLong(…)" NPEs on a null date, and inside a `when`
        // constraint the throw is a ConstraintEvaluationException that aborts the WHOLE fireAllRules() pass; a
        // pattern constraint also cannot express the guard with a ternary, so the helper is the only form valid
        // in both positions. DATE parts read off any datum; the TIME parts (HOUR_OF_DAY..MILLI_OF_SECOND) are
        // defined only on a LocalDateTime, so a plain Datum-in-dagen operand markers honestly (LocalDate would
        // throw UnsupportedTemporalType). (The resolver does not yet reject "het uur uit een Datum" — carded.)
        String chronoField = switch (f.kind()) {
            case JAAR_UIT -> "YEAR"; case MAAND_UIT -> "MONTH_OF_YEAR"; case DAG_UIT -> "DAY_OF_MONTH";
            case UUR_UIT -> "HOUR_OF_DAY"; case MINUUT_UIT -> "MINUTE_OF_HOUR";
            case SECONDE_UIT -> "SECOND_OF_MINUTE"; case MILLISECONDE_UIT -> "MILLI_OF_SECOND";
            default -> null;
        };
        if (chronoField != null) {
            if (a.size() != 1) return null;
            boolean tijdDeel = switch (f.kind()) {
                case UUR_UIT, MINUUT_UIT, SECONDE_UIT, MILLISECONDE_UIT -> true; default -> false;
            };
            if (tijdDeel && !(eff.typeVan(a.get(0)) instanceof Datatype.DatumTijd))
                return Frag.todo("tijd-component uit een niet-datum-tijd (§13.4.17.21-.24)", f.bron());
            Frag d = arg.apply(a.get(0));
            if (d.marker()) return d;
            fnImport("rsDatumDeel");
            return Frag.from("rsDatumDeel(" + d.text() + ", java.time.temporal.ChronoField." + chronoField + ")", d);
        }
        return switch (f.kind()) {
            case ABSOLUTE_WAARDE -> {                 // §13.4.17.17 / §6.9 Tabel 20: |leeg| = leeg, so route
                if (a.size() != 1) yield null;        // through rsAbs (a null guard a constraint cannot ternary).
                Frag x = arg.apply(a.get(0));
                if (x.marker()) yield x;
                fnImport("rsAbs");
                yield Frag.from("rsAbs(" + x.text() + ")", x);
            }
            case WORTEL -> {                          // §13.4.17.13 — the radicand's root, rounded by the
                if (a.size() != 1) yield null;        // MANDATORY afronding (an Afgerond wrapper the resolver
                Frag x = arg.apply(a.get(0));         // adds, now null-safe). rsWortel does BigDecimal.sqrt at
                // 34-digit precision, NOT Math.sqrt whose ~16-digit double noise can flip a directional
                // (FLOOR/CEILING) afronding on a terminating root, and carries the §6.6 leegwaarde (leeg -> leeg)
                // so a leeg radicand propagates through the afronding. A NEGATIVE radicand also yields leeg (no
                // session-aborting throw); the §6.6 foutmelding is surfaced by wortelNegatiefFout's companion rule.
                if (x.marker()) yield x;
                fnImport("rsWortel");
                yield Frag.from("rsWortel(" + x.text() + ")", x);
            }
            case DATUM_MET     -> datumConstructor(a, arg, "java.time.LocalDate", 3, false);     // §13.4.17.35 (3 parts)
            case DATUMTIJD_MET -> datumConstructor(a, arg, "java.time.LocalDateTime", 7, true);  // §13.4.17.36 (7 parts)
            case EERSTE_PAASDAG -> {                  // §13.4.17.37 — Easter Sunday of the given year
                if (a.size() != 1) yield null;
                Frag j = arg.apply(a.get(0));
                if (j.marker()) yield j;
                fnImport("rsEerstePaasdag");
                yield Frag.from("rsEerstePaasdag(" + j.text() + ")", j);
            }
            default -> null;
        };
    }

    /** §13.4.17.35/.36 build a LocalDate/LocalDateTime from its {@code n} numeric component expressions
     *  (jaar, maand, dag[, uur, minuut, seconde, milliseconde]). Lowered via a registered Java-bodied helper
     *  (rsDatumMet / rsDatumtijdMet) rather than an inline {@code LocalDate.of((c).intValue(), …)}, because a
     *  §6.9 EMPTY component yields an empty (null) date — and a bare {@code (null).intValue()} would NPE inside
     *  the constructor at fire time, aborting the whole session (the same leegwaarde class the date-component
     *  helpers already close). The helper null-guards every component and carries the §13.4.17.36 milli→nano
     *  scale (LocalDateTime.of's 7th arg is nanoOfSecond) in plain Java. A marker argument propagates. */
    private Frag datumConstructor(List<Expr> a, Function<Expr, Frag> arg, String type, int n, boolean laatsteIsMilli) {
        if (a.size() != n) return null;
        List<String> args = new ArrayList<>();
        for (Expr e : a) {
            Frag v = arg.apply(e);
            if (v.marker()) return v;
            args.add(v.text());
        }
        if (laatsteIsMilli) { fnImport("rsDatumtijdMet"); return Frag.ok("rsDatumtijdMet(" + String.join(", ", args) + ")"); }
        fnImport("rsDatumMet");
        return Frag.ok("rsDatumMet(" + String.join(", ", args) + ")");
    }

    /**
     * §6.10 / §13.4.17.32 "de [absolute] tijdsduur van A tot B in [hele] &lt;tijdseenheid&gt;": the number of
     * COMPLETE units between two dates, which is exactly {@code ChronoUnit.<UNIT>.between(A, B)} — signed (A
     * later than B gives a negative result, §6.10) and symmetric, matching Tabel 14's +52 / -52. The absolute
     * form wraps it in {@code Math.abs}. §6.10 Tabel 14 also makes an EMPTY operand yield an empty result (not
     * 0), and {@code between(null, …)} would NPE, so both operands are null-guarded. A time unit (uur/…/
     * milliseconde) needs Datum-en-tijd operands (ChronoUnit.HOURS.between throws on a LocalDate); a kwartaal
     * (no ChronoUnit) or an unrecognised unit markers honestly.
     */
    private Frag tijdsduur(Expr.Functie f, Function<Expr, Frag> arg) {
        if (f.argumenten().size() != 2) return null;
        // §6.10 tijdsduur in N <dagsoort> ("in hele werkdagen"): COUNT the §9.8 dagsoort days in the interval
        // (signed; absolute when asked). The spec leaves the dagsoort boundary undefined; per §6.11's "X telt niet
        // mee" the interval is half-open (van, tot] (the §6.11-consistent reading). Day-granular -> Datum operands.
        // ANNOTATION BY DESIGN (WF-038, ch. 2.10 exception 3): a tijdsduur's own unit IS the operator's parameter
        // ("in hele werkdagen"), not a derived result type.
        io.github.yhavinga.regelspraak.model.Eenheid dagE = eff.typeVan(f).eenheidOf();
        // ch. 3.8 puts the CALENDAR TABLE first — see Tijdrekening.dagsoortVoorEenheid for the order.
        String dagsoortUnit = Tijdrekening.dagsoortVoorEenheid(model, dagE);
        if (dagsoortUnit != null) {
            Expr a0 = f.argumenten().get(0), a1 = f.argumenten().get(1);
            // §4.16 (L676): both operands must be the SAME tijd-precisie. Both datum-EN-tijd is legal — the count is over
            // the DATE parts (rsTijdsduurTijd_<Naam>, no time carried). Mixed precision is spec-illegal -> honest marker.
            boolean dt0 = eff.typeVan(a0) instanceof Datatype.DatumTijd, dt1 = eff.typeVan(a1) instanceof Datatype.DatumTijd;
            if (dt0 != dt1)
                return Frag.todo("tijdsduur in een dagsoort vereist datum-operanden van gelijke tijd-precisie (§6.10)", f.bron());
            String glob = dagsoortGlobaal(dagsoortUnit);
            Frag van0 = arg.apply(a0), tot0 = arg.apply(a1);
            if (glob == null || van0.marker() || tot0.marker()) return Frag.todo("tijdsduur (dagsoort)", f.bron());
            String durFn = fnImport(dt0 ? "rsTijdsduurTijdDagsoort" : "rsTijdsduurDagsoort");
            return Frag.from(durFn + "(" + van0.text() + ", " + tot0.text() + ", " + glob + ", "
                    + (f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR) + ")", van0, tot0);
        }
        String unit = Tijdrekening.chronoUnit(eff.typeVan(f));
        if (unit == null) return Frag.todo("tijdsduur-eenheid (dagsoort / onbekende tijdseenheid)", f.bron());
        boolean tijdEenheid = switch (unit) { case "HOURS", "MINUTES", "SECONDS", "MILLIS" -> true; default -> false; };
        Expr eVan = f.argumenten().get(0), eTot = f.argumenten().get(1);
        if (tijdEenheid && (!(eff.typeVan(eVan) instanceof Datatype.DatumTijd) || !(eff.typeVan(eTot) instanceof Datatype.DatumTijd)))
            return Frag.todo("tijdsduur in een tijdseenheid vereist datum-tijd operanden (§6.10)", f.bron());
        Frag van = arg.apply(eVan), tot = arg.apply(eTot);
        if (van.marker() || tot.marker()) return Frag.todo("tijdsduur", f.bron());
        // Lower via a registered Java-bodied helper, NOT inline mvel: a fully-qualified ChronoUnit.<U>.between
        // does not analyse in a Drools mvel modify-block, and the helper also carries the §6.10 null-guard and
        // the absolute/sign in plain Java where the enum and the overload resolve cleanly.
        fnImport("rsTijdsduur");
        return Frag.from("rsTijdsduur(" + van.text() + ", " + tot.text() + ", \"" + unit + "\", "
                + (f.kind() == Expr.FunctieKind.ABSOLUTE_TIJDSDUUR) + ")", van, tot);
    }

    /** Wrap a value-side arithmetic operand for the §6.2/§6.4/§6.8 "leeg telt als 0" rekentabel via the rsNul
     *  helper. The caller ({@link #binairTerm}) has already excluded a marker operand (which must propagate
     *  unwrapped), so this only ever wraps a real value term. */
    private String nulVeilig(Acc acc, String operand) {
        fnImport("rsNul");
        return "rsNul(" + operand + ")";
    }

    /**
     * §4.12-4.15 / §5.8.3-5.8.4 concatenation-of-expressies form: "de minimale/maximale/eerste/
     * laatste van A en B [en C …]" over two-or-more LISTED scalar operands — distinct from the
     * single-operand collection aggregation (handled by {@link #accumulate}, which ranges over
     * instances). Lowered as a left fold of ternaries: min/eerste keeps the smaller/earlier, max/
     * laatste the larger/later. Numeric operands are restated in the first operand's unit before
     * comparing (the result unit "Eenheid1", Tabel 22/23), exactly as +/- and begrenzing do;
     * dates lower to Comparable LocalDate(Time), which mvel's &lt;/&gt; compare directly.
     *
     * <p>§4.12-4.15 ("leegwaarden … worden niet meegeteld; alle leeg → leeg"): each fold step
     * SKIPS an empty (null) operand and yields empty only when every operand is empty — a bare
     * comparison would poison the whole result to null, because mvel evaluates "null &lt; x" as
     * false and so selects the empty branch. The guard re-references each operand by name (a
     * side-effect-free getter), so the step is O(operands) textually but correct; the corpus only
     * ever lists two operands. Returns null for the single-operand collection form and for
     * non-min/max/eerste/laatste kinds, and falls through to the marker path on an unmappable operand.
     */
    /**
     * §13.4.17.51-.54 a SOM / MINIMUM / MAXIMUM over a gedimensioneerd attribuut, restricted to a
     * {@link Expr.DimensieSelectie}'s labels. The labels are resolved at build time (the "vanaf … t/m …"
     * interval / "in {…}" set already expanded against the dimension's declared order), so this is NOT an
     * accumulate over instances but a FIXED fold of the attribute's per-label values: the dimensioned
     * attribute reads as a {@code Map<label, value>} field and the aggregation folds {@code map.get(label)}
     * over exactly the selected labels.
     *
     * <p>§5.8.2 leegwaarde semantics: an empty contributor (an absent label, {@code map.get} → null) is
     * SKIPPED, not treated as 0; an all-empty fold yields the declared "of … als die er niet zijn" fallback,
     * else null (an empty sum is a leegwaarde, not 0). Lowered as a call to a registered DRL {@code function}
     * (folded in Java, so the DRL stays O(1) in the label count rather than an exponential mvel ternary),
     * passing the attribute's Map and the selected label keys. Returns null for any function without a
     * dimensieselectie, so the caller falls through to the instance path.
     */
    private Frag dimensieAggregatie(Acc acc, Expr.Functie f, Objecttype subject) {
        Expr.DimensieSelectie sel = f.dimensieSelectie();
        if (sel == null) return null;
        String fn = switch (f.kind()) {
            case SOM     -> "rsDimSom";
            case MINIMUM -> "rsDimMin";
            case MAXIMUM -> "rsDimMax";
            default -> null;
        };
        if (fn == null || f.argumenten().size() != 1 || !(f.argumenten().get(0) instanceof Expr.AttribuutRef a))
            return null;
        Frag map = attribuutGetter(a, subject);   // the dimensioned attribute reads as its Map<label,value> field
        if (map.marker()) return null;
        fnImport(fn);
        StringJoiner keys = new StringJoiner(", ");
        for (String label : sel.labels()) keys.add(drlString(label));   // embedded quotes ESCAPED (escaping by construction)
        String call = fn + "(" + map.text() + ", java.util.Arrays.asList(" + keys + "))";
        // §5.8.2: an all-empty SOM is a leegwaarde — substitute the declared "of … als die er niet zijn"
        // fallback when present, else leave the helper's null (an empty sum, never a silent 0). MIN/MAX of
        // an empty set is likewise the helper's null (no §5.8.2 fallback form).
        if (f.kind() == Expr.FunctieKind.SOM && f.leegwaarde() != null) {
            Frag lw = waarde(acc, f.leegwaarde(), subject);   // a markered fallback must still propagate
            return Frag.from("(" + call + " == null ? " + lw.text() + " : " + call + ")", lw);
        }
        return Frag.ok(call);
    }

    private Frag concatenatieAggregatie(Acc acc, Expr.Functie f, Objecttype subject) {
        // An unlowered restriction (a dimensieselectie captured as an Onopgelost filter, p5) must marker
        // the whole fold too — otherwise the value-fold would emit its unrestricted ternary while the
        // resolver already WARNed that the aggregation is "buiten beschouwing". Bail so the caller falls
        // through to the marker path. (A value-fold otherwise carries no filter, so this only fires here.)
        if (f.filter() instanceof Conditie.Onopgelost) return null;
        String cmp = switch (f.kind()) {
            case MINIMUM, EERSTE  -> "<";   // keep the smaller / earlier
            case MAXIMUM, LAATSTE -> ">";   // keep the larger / later
            default -> null;
        };
        if (cmp == null || f.argumenten().size() < 2) return null;
        Datatype doel = effType(f.argumenten().get(0));          // Eenheid1: result carries the first operand's unit
        Frag accF = waarde(acc, f.argumenten().get(0), subject);
        if (accF.marker()) return null;
        String vouw = accF.text();
        for (int i = 1; i < f.argumenten().size(); i++) {
            Expr arg = f.argumenten().get(i);
            Frag xF = omrekenWaarde(waarde(acc, arg, subject), effType(arg), doel);
            if (xF.marker()) return null;
            String x = xF.text();
            // null-skipping: fold empty → take x; x empty → keep fold; else compare. All-empty stays null.
            vouw = "(" + vouw + " == null ? " + x + " : (" + x + " == null ? " + vouw
                    + " : (" + vouw + " " + cmp + " " + x + " ? " + vouw + " : " + x + ")))";
        }
        return Frag.ok(vouw);
    }

    /** §7.1 "het totaal van &lt;temporeel attribuut&gt;": a TOTAAL over a single OWN temporal attribute read sums
     *  each tijdvak's value weighted by the number of granularity periods it spans. The value is a piecewise
     *  Tijdreeks held as a {@code List<Segment{van,tot,waarde}>} on the bean; rsTijdTotaal folds it (the period
     *  count per segment from the attribute's declared {@link RegelSpraakModel.Granulariteit}). Returns null for
     *  any other operand — a navigated or non-temporal attribute, a multi-arg fold — so the instance-accumulate
     *  and marker paths are unchanged (a non-temporal TOTAAL still flows to {@link #accumulate}). */
    private Frag tijdreeksAggregatie(Acc acc, Expr.Functie f, Objecttype subject) {
        if (f.kind() != Expr.FunctieKind.TOTAAL || f.argumenten().size() != 1) return null;
        // §7.1 / §8.4 a "gedurende de tijd dat <voorwaarde>" restriction the resolver could not structure rides
        // as an Onopgelost filter with its WARNING ("blijft buiten beschouwing"); honour it like the sibling
        // accumulate / concatenatieAggregatie paths do — fall through to the marker rather than fold the FULL
        // unrestricted timeline (which would compute the wrong, condition-free totaal).
        if (f.filter() instanceof Conditie.Onopgelost) return null;
        if (!(f.argumenten().get(0) instanceof Expr.AttribuutRef a)) return null;
        if (a.objecttype() == null) return null;
        // A "het totaal van <attr> van een SET" (alle / ten minste … / etc.) is a SET aggregation: the fold carries a
        // range (IR-SPEC ch. 4.3), and its argument is then the element's attribute, never the subject's own timeline.
        // Folding it as the subject's own single timeline would silently drop the set, so bail to the marker (the
        // accumulate path then markers it too). No per-element fold yet.
        if (heeftBereik(f)) return null;
        RegelSpraakModel.Granulariteit g = opNaam(a.objecttype().granulariteitLeden(), a.attribuut());
        if (g == null) return null;   // not a temporal attribute -> fall through to the instance-accumulate / marker
        // rsTijdTotaal scales each segment value into a BigDecimal; over a non-numeric declaration the emitted
        // text did not COMPILE and took the whole file down unnamed. A per-form marker through the reason the
        // set arm shares, asked before any of the own/navigated/clipped getters below is built.
        String vw = io.github.yhavinga.regelspraak.transpiler.kern.Vouwwaarde.tijdlijnWeigering(f, a.objecttype(), a);
        if (vw != null) return Frag.todo(vw, a.bron());
        // §13.3.6.7 a MULTI-period attribute ("voor elke 5 maanden") weights each tijdvak by the period count,
        // whose totaal semantics §7.1 does not work out — marker it rather than mis-count it as single-period.
        if (Tijdlijnen.meerPeriodiek(a.objecttype().tijdlijnAantalLeden(), a.attribuut()))
            return Frag.todo("totaal van een meer-periodieke tijdreeks (§13.3.6.7, voor elke <n> " + g.name().toLowerCase()
                    + "en)", a.bron());
        // The timeline getter: the subject's own attribute, or a to-ONE navigated partner's, root-guarded so a
        // null reference is leeg (rsTijdTotaal returns null) rather than an NPE-abort. A to-many / unresolvable
        // navigation cannot be one getter — marker it (never a wrong sum() over the List<Segment>).
        NaviRef n = naviResolve(a, subject);
        String waardeTijdreeks;
        if (n.soort() == NaviSoort.EIGEN) {
            waardeTijdreeks = SUBJECT + "." + getter(a.attribuut()) + "()";
        } else if (n.soort() == NaviSoort.GENAVIGEERD) {
            String root = SUBJECT + "." + getter(n.rol()) + "()";
            waardeTijdreeks = "(" + root + " == null ? null : " + root + "." + getter(a.attribuut()) + "())";
        } else {
            return null;
        }
        // §7.1 "gedurende de tijd dat <temporeel kenmerk>": the resolver structured the restriction into a
        // temporal Conditie.Kenmerk of the subject; clip the value timeline to that kenmerk's TRUE periods
        // before totalling (rsTijdKnip). A filter the resolver could not structure stays Onopgelost -> the
        // marker above; any OTHER non-null filter we do not lower is markered rather than silently dropped.
        String guard = null;   // §8.4 a static (non-temporal) compound member gates the WHOLE totaal
        if (f.filter() instanceof Conditie.Kenmerk k && !genavigeerd(k)
                && subject != null && bevatNaam(subject.kenmerkGranulariteitLeden(), k.kenmerk())) {
            waardeTijdreeks = kenmerkTijdKnip(waardeTijdreeks, k);
        } else if (f.filter() instanceof Conditie.Kenmerk kn && genavigeerd(kn) && subject != null) {
            // §7.1 a NAVIGATED temporal kenmerk ("gedurende de tijd dat zijn partner een recht op korting heeft"): clip
            // the value timeline to the to-one role's kenmerk timeline, read root-guarded (a null reference -> leeg).
            String cond = navigatieKenmerkTijdreeks(kn, subject);
            if (cond == null) return null;   // negated / to-many / non-temporal navigated kenmerk -> marker
            fnImport("rsTijdKnip");
            waardeTijdreeks = "rsTijdKnip(" + waardeTijdreeks + ", " + cond + ", Segment.class)";
        } else if (f.filter() instanceof Conditie.Samengesteld s && subject != null) {
            // §8.4.2 a COMPOUND "gedurende de tijd dat er aan alle/ten minste 1 van de volgende voorwaarden wordt
            // voldaan": partition the members. TEMPORAL kenmerks clip the timeline (ALLE = intersect, ten minste 1 =
            // union); a §8.4 NON-temporal comparison member becomes a value-side STATIC guard ("de samengestelde
            // conditie is als geheel wel tijdsafhankelijk"). For ALLE the static guard AND-gates the clipped totaal
            // (false -> 0); for "ten minste 1" (OR) a TRUE static member WIDENS the result to the FULL value span
            // (the OR holds everywhere a static member holds), FALSE leaves only the temporal-union clip. A member we
            // cannot lower -> marker, never a wrong period.
            java.util.List<Conditie> temporeel = new java.util.ArrayList<>();
            java.util.List<Conditie.Vergelijking> statisch = new java.util.ArrayList<>();
            for (Conditie c : s.onderdelen()) {
                if (c instanceof Conditie.Kenmerk) temporeel.add(c);
                else if (c instanceof Conditie.Vergelijking v) statisch.add(v);
                else return null;
            }
            if (temporeel.isEmpty()) return null;   // no clip -> not a temporal restriction
            String volleReeks = waardeTijdreeks;    // the un-clipped timeline (the OR static-true span)
            Conditie.Samengesteld temporeelDeel = statisch.isEmpty() ? s
                    : new Conditie.Samengesteld(s.kwantor(), s.drempel(), temporeel, s.bron());
            String compound = samengesteldeTijdKnip(temporeelDeel, waardeTijdreeks, subject);
            if (compound == null) return null;
            waardeTijdreeks = compound;
            if (!statisch.isEmpty()) {
                if (s.kwantor() == Conditie.Kwantor.TEN_MINSTE) {
                    // mixed OR: the static guard ORs its members; TRUE -> the full span, FALSE -> the temporal clip.
                    String orGuard = statischeGuard(acc, statisch, subject, " || ");
                    if (orGuard == null) return null;
                    fnImport("rsTijdTotaal");
                    return Frag.ok("(" + orGuard
                            + " ? rsTijdTotaal(" + volleReeks + ", \"" + g.name() + "\")"
                            + " : rsTijdTotaal(" + waardeTijdreeks + ", \"" + g.name() + "\"))");
                }
                guard = statischeGuard(acc, statisch, subject);   // ALLE: AND-gate the clipped totaal
                if (guard == null) return null;   // a static member we cannot lower -> marker, never an ungated totaal
            }
        } else if (f.filter() instanceof Conditie.Vergelijking vc && subject != null) {
            // §7.1 EUR71 "gedurende de tijd dat <temporeel attr> <op> <scalar>": clip the value timeline to the
            // periods where the comparison holds (its OWN boolean timeline, rsTijdConditie) before totalling. The
            // comparison's temporal attribute may differ from the totalled one ("totaal van de bonus gedurende de
            // tijd dat het inkomen > 1000"); rsTijdKnip intersects the two timelines' intervals.
            String cond = vergelijkingTijdreeks(acc, vc, subject);
            if (cond == null) return null;   // a non-temporal / cross-unit / datum / navigated comparison -> marker
            fnImport("rsTijdKnip");
            waardeTijdreeks = "rsTijdKnip(" + waardeTijdreeks + ", " + cond + ", Segment.class)";
        } else if (f.filter() != null) {
            return null;   // a restriction we cannot lower -> marker, never a silently-unrestricted total
        }
        fnImport("rsTijdTotaal");
        String totaal = "rsTijdTotaal(" + waardeTijdreeks + ", \"" + g.name() + "\")";
        return Frag.ok(guard == null ? totaal : "(" + guard + " ? " + totaal + " : java.math.BigDecimal.ZERO)");
    }

    /** §7.1 EUR71 the condition boolean timeline for a "gedurende de tijd dat &lt;temporeel attr&gt; &lt;op&gt;
     *  &lt;scalar&gt;" restriction: {@code rsTijdConditie(timeline, toets, grens)} lists the periods where the
     *  comparison holds. Mirrors {@link #geheelPeriodeCall}'s operand extraction (temporal side put on the left via
     *  {@link #omkeren}; a cross-unit / datum / navigated / non-temporal comparison returns null -&gt; the caller
     *  markers, never a wrong clip). */
    private String vergelijkingTijdreeks(Acc acc, Conditie.Vergelijking v, Objecttype subject) {
        Expr.AttribuutRef tijd; Expr ander; boolean linksTemp;
        if (v.links() instanceof Expr.AttribuutRef la && isTemporeelAttribuut(la))      { tijd = la; ander = v.rechts(); linksTemp = true; }
        else if (v.rechts() instanceof Expr.AttribuutRef ra && isTemporeelAttribuut(ra)) { tijd = ra; ander = v.links();  linksTemp = false; }
        else return null;
        if (!(eff.typeVan(tijd).isNumeriek() || eff.typeVan(tijd).isPercentage())) return null;   // datum/tekst temporal cmp -> marker
        io.github.yhavinga.regelspraak.model.Eenheid el = eff.eenheidVan(tijd), er = eff.eenheidVan(ander);
        if (ander instanceof Expr.AttribuutRef ar && isTemporeelAttribuut(ar)) return null;   // both-temporal (timeline vs timeline) -> marker
        if (el != null && er != null && el.vereistOmrekening(er)) return null;        // cross-unit -> marker (raw-magnitude would be wrong)
        Integer code = Tijdrekening.toets(linksTemp ? v.op() : Operatoren.omkeren(v.op()));               // temporal operand on the left (REVERSE, not negate)
        if (code == null) return null;                                                // a datum ordering op -> marker
        String getter = temporeelTijdlijnGetter(tijd, subject);
        if (getter == null) return null;                                              // navigated -> marker
        // the scalar grens lands in the VALUE/then block (rsTijdTotaal(...)), so lower it value-side (waarde ->
        // $subject.getX() / a root-guarded navigated getter / a literal / a global), NOT operand()'s when-side bare
        // field name (which does not resolve in a then block — KieBase build abort).
        Frag g = waarde(acc, ander, subject);
        if (g.marker()) return null;
        fnImport("rsTijdConditie");
        return "rsTijdConditie(" + getter + ", " + code + ", " + g.text() + ", Segment.class)";
    }

    /** §8.4 the value-side static guard for a mixed gedurende: the AND ({@code " && "}) of each non-temporal
     *  comparison member for an ALLE compound, or the OR ({@code " || "}) for a "ten minste 1" compound. Each member
     *  lowers to a {@code $subject.getX()}-form mvel boolean. Null when any member is not lowerable (the caller markers
     *  the whole totaal). */
    private String statischeGuard(Acc acc, java.util.List<Conditie.Vergelijking> leden, Objecttype subject) {
        return statischeGuard(acc, leden, subject, " && ");
    }
    private String statischeGuard(Acc acc, java.util.List<Conditie.Vergelijking> leden, Objecttype subject, String scheider) {
        java.util.List<String> termen = new java.util.ArrayList<>();
        for (Conditie.Vergelijking v : leden) {
            String t = statischeVergelijkingGuard(acc, v, subject);
            if (t == null) return null;
            termen.add(t);
        }
        return String.join(scheider, termen);
    }

    /** §8.4 one non-temporal comparison member as a value-side ($subject.getX()-form) boolean. The guard rides in the
     *  totaal's CONSEQUENCE, so it uses the value-side {@code waarde()} form, not the when-side bare property, with the
     *  leegwaarde semantics of the §5.1 comparison matrix: an ORDERING/datum comparison is onwaar if either operand is
     *  empty (Tabel 40-43); a GELIJK is waar for both-empty NUMERIEK (Tabel 37) but onwaar for both-empty non-numeriek
     *  (Tabel 38); an ONGELIJK is waar for gevuld-vs-empty and onwaar for both-empty (Tabel 44). A markered/unresolvable
     *  operand -> null. */
    private String statischeVergelijkingGuard(Acc acc, Conditie.Vergelijking v, Objecttype subject) {
        Frag lvF = waarde(acc, v.links(), subject), rvF = waarde(acc, v.rechts(), subject);
        if (lvF.marker() || rvF.marker()) return null;
        String lv = lvF.text(), rv = rvF.text();
        String[] ov = vergelijkingsOperanden(lv, effType(v.links()), rv, effType(v.rechts()));
        String volBeide = "(" + lv + " != null && " + rv + " != null && " + ov[0] + " " + Operatoren.symbool(v.op()) + " " + ov[1] + ")";
        if (ordeningOfDatum(v.op())) {
            // §5.7 Tabel 46: a DATUM ordering (eerder/later) with BOTH operands empty-capable (an attr-vs-attr compare,
            // no datum literal) is "fout" when both are empty — NOT the "onwaar" that volBeide renders. The exec
            // statischeVergelijking fires a datum ordering static member ONLY as <own-attr> <op> <datum literal> and
            // SKIPS every other datum form, so DRL markers them too (return null) to stay symmetric — never a
            // DRL-fires/exec-skips divergence the differential would mask. A numeric ordering both-empty is Tabel 42/33
            // "onwaar" = the volBeide false (correct), so it keeps firing; a literal-RHS datum ordering (never both-empty)
            // keeps firing in both back-ends.
            if (eff.typeVan(v.links()).isDatum()) {
                boolean execFires = v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la)
                        && v.rechts() instanceof Expr.Literal dl && dl.type().isDatum();
                if (!execFires) return null;
            }
            return volBeide;
        }
        if (v.op() == Conditie.Operator.GELIJK)   // Tabel 37 numeriek both-empty -> waar; Tabel 38 non-numeriek -> onwaar
            return nietNumeriekVergelijking(v) ? volBeide
                    : "((" + lv + " == null && " + rv + " == null) || " + volBeide + ")";
        if (v.op() == Conditie.Operator.ONGELIJK)   // Tabel 44 gevuld-vs-empty -> waar; both-empty -> onwaar
            return "(((" + lv + " == null) != (" + rv + " == null)) || " + volBeide + ")";
        return null;
    }

    /** §5.1.4 lower a RHS that PRODUCES a timeline written to a single-period temporal doel attribute, into a
     *  {@code List<Segment>} expression — or null when it is not a supported timeline producer (the caller then
     *  markers). The first writer: §13.4.17.57 "het aantal dagen in <de maand|het jaar> dat <temporeel kenmerk>"
     *  -> rsAantalDagenIn over the kenmerk's true-period timeline, bucketed by the DOEL's granularity (the spec
     *  requires the expression's "in <maand|jaar>" to match the doel's tijdlijn, §7.2). Only a positive
     *  single-period subject temporal-kenmerk condition lowers; a negated/comparison/compound/navigated condition
     *  left the filter unset in the resolver -> null here. */
    private String temporeleWaarde(Acc acc, Expr w, Objecttype subject, RegelSpraakModel.Granulariteit gran, Datatype doelType, String anker) {
        // §5.8.2/§5.1.4 "de som van <temporeel attr> van alle <naar-veel-rol>" into a TEMPORAL doel: a PER-PERIOD
        // sommatie over the navigated SET, keeping the timeline (§5.1.4: only the §7.1 "totaal van" aggregation
        // collapses to a scalar; "som van" over temporal elements stays temporal). An accumulate COLLECTS each
        // element's List<Segment> timeline; rsTijdSomAlle folds them per merged-knip period with the §5.8.2 leeg rule
        // (skip empties, all-empty period -> leeg unless "of 0 als die er niet zijn"). Anchored (variable-startpunt)
        // doelen stay a marker (the per-object bucket interaction with a set fold is out of scope).
        if (anker == null) {
            String somSet = tijdreeksSomAlleSet(acc, w, subject, gran, doelType);
            if (somSet != null) return somSet;
        }
        if (w instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.AANTAL_DAGEN_IN
                // §4.21/§7.2 the operator's "in <maand|jaar>" bucket unit MUST match the doel's tijdlijn — a
                // mismatch ("in het jaar" into a per-maand doel) markers rather than silently bucketing by the doel.
                && f.granulariteit() == gran && subject != null) {
            String cond = conditieTijdreeks(f.filter(), subject);   // single kenmerk OR a §8.4.2 (mixed) compound; null -> marker
            if (cond != null) {
                // §3.8: an anchored doel buckets at the per-object startpunt (rsAantalDagenInVanaf); a leeg
                // anchor yields no periods (the §6.8 fout rides the startpunt rule's own companion).
                String fn = anker == null ? "rsAantalDagenIn" : "rsAantalDagenInVanaf";
                String args = anker == null ? "" : ", " + anker;
                String call = mixedGuardWrap(acc, fn + "(" + cond + ", \"" + gran.name() + "\"" + args + ", Segment.class)", f.filter(), subject);
                if (call != null) { fnImport(fn); return call; }
            }
        }
        // §13.4.17.58/§7.3.2 "het tijdsevenredig deel per <maand|jaar> van <basis> gedurende de tijd dat <conditie>":
        // per calendar bucket, value = baseValueInBucket × (true-days / total-days). The base must be a single-period
        // TIMELINE — its value-timeline is intrinsic (§5.1.2): either the subject's own temporal attribute, OR a
        // TEMPORAL PARAMETER (the spec's literal STANDAARD BELASTINGVERMINDERING base, a List<Segment> global). A
        // plain scalar base markers (the resolver left its filter unset). §4.22: NO auto-round (DECIMAL64, not the
        // doel's decimals — §6.1.2 forbids it); a leeg base bucket yields no segment. The base must be CONSTANT within
        // each output bucket (§7.3.2 lines 3093/3123 read it "voor de hele maand/jaar"), so its granularity must be
        // COARSER-OR-EQUAL to the bucket (DAG<WEEK<MAAND<KWARTAAL<JAAR via ordinal); a unit mismatch (the §7.3.2 ex.3
        // €/jr→€/mnd rate conversion) or a negated/navigated condition markers.
        if (w instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.TIJDSEVENREDIG_DEEL
                && f.granulariteit() == gran && f.argumenten().size() == 1 && subject != null) {
            Expr base = f.argumenten().get(0);
            RegelSpraakModel.Granulariteit baseGran = null; Datatype baseType = null; String baseExpr = null;
            if (base instanceof Expr.AttribuutRef ba && ongenavigeerd(ba) && ba.objecttype() == subject
                    && bevatNaam(subject.granulariteitLeden(), ba.attribuut())
                    && !Tijdlijnen.meerPeriodiek(subject.tijdlijnAantalLeden(), ba.attribuut())) {
                baseGran = opNaam(subject.granulariteitLeden(), ba.attribuut());
                baseType = effType(ba);
                baseExpr = SUBJECT + "." + getter(ba.attribuut()) + "()";
            } else if (base instanceof Expr.AttribuutRef ba && !ongenavigeerd(ba)
                    && ba.objecttype() != null && bevatNaam(ba.objecttype().granulariteitLeden(), ba.attribuut())
                    && !Tijdlijnen.meerPeriodiek(ba.objecttype().tijdlijnAantalLeden(), ba.attribuut())
                    && naviResolve(ba, subject).soort() == NaviSoort.GENAVIGEERD) {
                // §7.3.2 a TO-ONE NAVIGATED base ("de X van zijn partner"): read the partner's temporal attribute,
                // root-guarded (a null reference -> leeg). The gedurende condition stays over the SUBJECT. Mirrors exec navReader.
                baseGran = opNaam(ba.objecttype().granulariteitLeden(), ba.attribuut());
                baseType = effType(ba);
                String root = SUBJECT + "." + getter(naviResolve(ba, subject).rol()) + "()";
                baseExpr = "(" + root + " == null ? null : " + root + "." + getter(ba.attribuut()) + "())";
            } else if (base instanceof Expr.ParameterRef bp && bp.parameter().granulariteit() != null) {
                baseGran = bp.parameter().granulariteit();
                baseType = bp.parameter().datatype();
                baseExpr = ident(bp.parameter().naam(), false);   // the List<Segment> global the runtime supplies
            }
            if (baseExpr != null && baseGran.ordinal() >= gran.ordinal() && baseType.isNumeriek()) {
                // §7.3.2 ex.3 rate conversion: the base's per-tijdseenheid unit may DIFFER from the doel's (€/jr base
                // into a €/mnd doel). §7.3.1 restates the base FIRST (÷12) — rsTijdSchaal scales the base timeline by
                // the unit factor BEFORE proration. Same unit -> factor null (no scale); convertible-different -> a
                // factor; not convertible -> marker (omrekenFactor null AND the units differ).
                boolean zelfdeEenheid = java.util.Objects.equals(baseType.eenheidOf(), doelType == null ? null : doelType.eenheidOf());
                // §4.22 the tijdsevenredig operand must be "numeriek met een eenheid per tijdseenheid" (a RATE). A unit
                // MISMATCH is a §7.3.2 rate conversion ONLY when the base unit is a rate (has a noemer); a non-rate
                // mismatch (cm -> m) is NOT a rate restatement and stays a marker (never a silent unit-rebase).
                boolean baseIsRate = baseType.eenheidOf() != null && baseType.eenheidOf().noemer() != null;
                java.math.BigDecimal fwd = (zelfdeEenheid || !baseIsRate) ? null : omrekenFactor(baseType, doelType);
                // §7.3.1 specifies the restatement as a DIVISION ("gedeeld door de omrekenfactor 12"): one of the two
                // reciprocal factors is the EXACT integer ratio (12) and the other is the lossy 1/12 — pick the exact
                // direction (fewer significant digits) and DIVIDE by it when it is the inverse, so a non-clean base
                // stays exact (a pre-rounded ×0.0833 diverges from ÷12 for ~13% of bases).
                java.math.BigDecimal inv = fwd == null ? null : omrekenFactor(doelType, baseType);
                boolean deel = inv != null && inv.precision() < fwd.precision();
                java.math.BigDecimal factor = fwd == null ? null : (deel ? inv : fwd);
                String basis = factor == null ? baseExpr
                        : "rsTijdSchaal(" + baseExpr + ", new java.math.BigDecimal(\"" + factor.toPlainString() + "\"), " + deel + ")";
                String cond = (zelfdeEenheid || factor != null) ? conditieTijdreeks(f.filter(), subject) : null;
                String call = null;
                if (cond != null) {
                    // §3.8 anchored buckets for a variable-startpunt doel (see the aantal-dagen-in branch).
                    String fn = anker == null ? "rsTijdsevenredig" : "rsTijdsevenredigVanaf";
                    String extra = anker == null ? "" : ", " + anker;
                    call = mixedGuardWrap(acc, fn + "(" + cond + ", " + basis + ", \"" + gran.name() + "\"" + extra + ", Segment.class)", f.filter(), subject);
                    if (call != null && anker != null) { fnImport(fn); if (factor != null) fnImport("rsTijdSchaal"); return call; }
                } else if (anker == null && (zelfdeEenheid || factor != null)) {
                    // §8.4.2 OR-mixed (ten minste 1: a temporal kenmerk OR a static comparison): conditieTijdreeks
                    // markers a mixed OR, but a tijdsevenredig HAS a base whose value-timeline bounds the static-true
                    // span (unlike aantal-dagen-in, which has no base/extent), so it lowers like the totaal-READ
                    // OR-mixed (~line 1757): a TRUE static member -> prorate the base over its FULL extent (the base AS
                    // its own all-true condition — rsTijdsevenredig reads a condition's SEGMENTS as the true periods,
                    // so basis-as-condition is factor 1 per fully-covered bucket); FALSE -> the temporal-union clip.
                    call = orMixedTijdsevenredig(acc, f.filter(), subject, basis, gran);
                }
                if (call != null) { fnImport("rsTijdsevenredig"); if (factor != null) fnImport("rsTijdSchaal"); return call; }
            }
        }
        // §5.1.4 a tijdsafhankelijke arithmetic expression PLUS/MIN over two temporal operands: apply the operator per
        // period on the merged-knip timeline (rsTijdReken; leeg -> 0). §5.1.4.2/§4.1: SUM in the LEFT operand's unit
        // (restate the right operand into it), THEN restate the SUM ONCE to the doel unit ("daarna wordt dit omgerekend
        // naar €/jr"). Scaling each operand to the doel FIRST would DOUBLE-ROUND a non-terminating factor (÷12) — the
        // spec sums first, converts once (review wf_115473d5-eee F1). Mixed granularity rides the merged-knip union.
        if (w instanceof Expr.Binair bin && (bin.op() == Expr.BinOp.PLUS || bin.op() == Expr.BinOp.MIN) && subject != null) {
            Datatype sumType = effType(bin.links());
            String l = temporeelRekenOperand(bin.links(), subject, gran, sumType);
            String rr = temporeelRekenOperand(bin.rechts(), subject, gran, sumType);
            if (l != null && rr != null) {
                fnImport("rsTijdReken");
                String schaal = schaalTijd("rsTijdReken(" + l + ", " + rr + ", \"" + bin.op().name() + "\", Segment.class)", sumType, doelType);
                if (schaal != null) return schaal;   // the sum unit is convertible to the doel (§4.1) -> restate once
            }
        }
        return null;
    }

    /** §5.8.2/§5.1.4 "de som van &lt;temporeel attr&gt; van alle &lt;naar-veel-rol&gt;" into a TEMPORAL doel — a per-period
     *  sommatie over the navigated SET. Emits a {@code collectList} accumulate (each element's List&lt;Segment&gt; timeline)
     *  after the subject pattern, and returns an {@code rsTijdSomAlle(...)} fold over the bound list. Only a SOM (not the
     *  §7.1 "totaal van", which collapses to a scalar) over a SET of a single-period temporal attribute lowers; a
     *  to-one/own operand, a multi-period element, an unresolvable join, or an unstructured die-filter -> null (marker). */
    private String tijdreeksSomAlleSet(Acc acc, Expr w, Objecttype subject, RegelSpraakModel.Granulariteit gran, Datatype doelType) {
        if (!(w instanceof Expr.Functie f) || f.kind() != Expr.FunctieKind.SOM || f.argumenten().size() != 1) return null;
        if (!(f.argumenten().get(0) instanceof Expr.AttribuutRef a) || !heeftBereik(f) || a.objecttype() == null) return null;
        RegelSpraakModel.Granulariteit eg = opNaam(a.objecttype().granulariteitLeden(), a.attribuut());
        if (eg == null) return null;                                   // non-temporal element -> the scalar accumulate path
        if (Tijdlijnen.meerPeriodiek(a.objecttype().tijdlijnAantalLeden(), a.attribuut())) return null;   // multi-period element -> out of scope
        if (eg != gran) return null;                                   // §5.1.4 the element tijdlijn must match the doel's (no set-fold gran promotion yet)
        if (!eff.typeVan(a).isNumeriek()) return null;                 // §5.8.2 a sommatie is over numeriek operands (soort = routing)
        if (!schaalbaarNaarDoel(effType(a), doelType)) return null;    // §4.1 element unit must restate to the doel unit (else marker)
        Objecttype elem = f.elementObjecttype();
        if (elem == null || subject == null) return null;
        // IR-SPEC ch. 4.3: the fold's range IS its join; a path has no one-join lowering here
        FactModelGenerator.Bereikjoin bj = FactModelGenerator.bereikjoin(model, rolBindingen.get(f));
        String rol = bj.rol();
        if (rol == null) {
            if (bj.weigering() != null) navReden = bj.weigering();
            return null;                                               // unresolvable join -> marker
        }
        // §13.3.9: this fold resolves its own join, so it asks the shared question itself. Measured 2026-09-11 on a
        // verifier-clean model whose element role is to-many (the field lives at the far end): the emitted
        // `bewarendeJutter == $subject` sank the package. Mirrors the exec branch's gate on the same fold.
        if (!navVeld("temporele set-sommatie", elem, rol, subject)) return null;
        int n = ++acc.n;
        String ev = "$e" + n, var = "$tl" + n;
        String filterScope = ident(rol, false) + " == " + SUBJECT;
        if (f.filter() != null) {
            Frag fc = conditie(acc, f.filter(), elem);                 // §13.4.2 die/dat subselectie on the element
            if (fc.marker()) return null;                              // unstructured filter -> marker
            filterScope = filterScope + ", " + fc.text();
        }
        // §4.1 restate each element timeline into the doel unit BEFORE folding (the §5.8.2 sum is unit-homogeneous;
        // an equal unit scales by factor 1 -> the getter verbatim). collectList binds the List<List<Segment>>.
        String elemTijdlijn = schaalTijd(ev + "." + getter(a.attribuut()) + "()", effType(a), doelType);
        acc.clausules.add("accumulate( " + ev + " : " + ident(elem.naam, true) + "( " + filterScope + " ) ; "
                + var + " : collectList( " + elemTijdlijn + " ) )");
        boolean fallbackNul = f.leegwaarde() != null;                  // §5.8.2 "of 0 als die er niet zijn"
        fnImport("rsTijdSomAlle");
        return "rsTijdSomAlle(" + var + ", " + fallbackNul + ", Segment.class)";
    }

    /** Whether a numeric temporal timeline in {@code van}'s unit can restate into {@code naar}'s (equal, or unit-convertible). */
    private boolean schaalbaarNaarDoel(Datatype van, Datatype naar) {
        io.github.yhavinga.regelspraak.model.Eenheid eu = van == null ? null : van.eenheidOf(), du = naar == null ? null : naar.eenheidOf();
        if (java.util.Objects.equals(eu, du)) return true;
        return eu != null && du != null && eu.convertibleNaar(du);
    }

    /** §5.1.4 a temporal operand of a tijdsafhankelijke arithmetic expression — an own or to-one navigated SINGLE-period
     *  numeric temporal attribute whose granularity is COARSER-OR-EQUAL to the doel's (gran) and whose unit is CONVERTIBLE
     *  to {@code unitTarget} (the sum unit). Returns the {@code List<Segment>} getter (restated to unitTarget when the
     *  unit differs), or null (not such an operand / incompatible unit / finer granularity -> marker). */
    private String temporeelRekenOperand(Expr e, Objecttype subject, RegelSpraakModel.Granulariteit gran, Datatype unitTarget) {
        // §5.1.4 an N-operand chain ("a plus b plus c" = Binair(Binair(a,b), c)): the operand is itself a temporal sub-sum.
        // Recurse — sum in the LEFT sub-operand's unit (restate the right into it), then restate the sub-sum ONCE to
        // unitTarget (the enclosing sum unit). Arbitrary depth + mixed +/- fall out of the recursion, each Binair its own op.
        if (e instanceof Expr.Binair bin && (bin.op() == Expr.BinOp.PLUS || bin.op() == Expr.BinOp.MIN)) {
            Datatype subSum = effType(bin.links());
            String l = temporeelRekenOperand(bin.links(), subject, gran, subSum);
            String r = temporeelRekenOperand(bin.rechts(), subject, gran, subSum);
            if (l == null || r == null) return null;
            fnImport("rsTijdReken");
            return schaalTijd("rsTijdReken(" + l + ", " + r + ", \"" + bin.op().name() + "\", Segment.class)", subSum, unitTarget);
        }
        if (!(e instanceof Expr.AttribuutRef a) || !eff.typeVan(a).isNumeriek()) return null;
        Objecttype ot; String getter;
        if (ongenavigeerd(a) && a.objecttype() == subject) {
            ot = subject; getter = SUBJECT + "." + getter(a.attribuut()) + "()";
        } else if (!ongenavigeerd(a) && a.objecttype() != null && naviResolve(a, subject).soort() == NaviSoort.GENAVIGEERD) {
            ot = a.objecttype();
            String root = SUBJECT + "." + getter(naviResolve(a, subject).rol()) + "()";
            getter = "(" + root + " == null ? null : " + root + "." + getter(a.attribuut()) + "())";
        } else return null;
        // §5.1.4.2 mixed granularity: the operand timeline must be COARSER-OR-EQUAL to the doel's (DAG<WEEK<MAAND<
        // KWARTAAL<JAAR via ordinal), so ALL its knips fit the (finer-or-equal) doel timeline; the merged-knip union
        // then evaluates per actual period. An operand FINER than the doel cannot be represented -> marker.
        RegelSpraakModel.Granulariteit og = opNaam(ot.granulariteitLeden(), a.attribuut());
        if (og == null || og.ordinal() < gran.ordinal()) return null;
        if (Tijdlijnen.meerPeriodiek(ot.tijdlijnAantalLeden(), a.attribuut())) return null;   // multi-period -> out of scope
        return schaalTijd(getter, effType(a), unitTarget);   // §4.1 restate into the sum unit (null -> incompatible -> marker)
    }

    /** §4.1/§5.1.4.2 restate a temporal timeline expr from {@code van}'s unit into {@code naar}'s (rsTijdSchaal by the
     *  EXACT-integer direction, ÷ when the inverse has fewer sig digits — like §7.3.1). Returns the expr UNCHANGED when
     *  the units are equal or no conversion is required; null when the units are INCOMPATIBLE (caller markers). */
    private String schaalTijd(String expr, Datatype van, Datatype naar) {
        io.github.yhavinga.regelspraak.model.Eenheid eu = van == null ? null : van.eenheidOf(), du = naar == null ? null : naar.eenheidOf();
        if (java.util.Objects.equals(eu, du)) return expr;                       // equal (incl. both null) -> no scale
        if (eu == null || du == null || !eu.convertibleNaar(du)) return null;    // incompatible unit -> marker
        if (!eu.vereistOmrekening(du)) return expr;                              // convertible with factor 1 -> no scale
        java.math.BigDecimal fwd = omrekenFactor(van, naar), inv = omrekenFactor(naar, van);
        boolean deel = inv != null && inv.precision() < fwd.precision();
        java.math.BigDecimal factor = deel ? inv : fwd;
        fnImport("rsTijdSchaal");
        return "rsTijdSchaal(" + expr + ", new java.math.BigDecimal(\"" + factor.toPlainString() + "\"), " + deel + ")";
    }

    /** The TRUE-period {@code List<Segment>} timeline of a temporal WRITE-producer's condition — the periods whose
     *  days/value get bucketed by rsAantalDagenIn / rsTijdsevenredig. A single positive single-period subject kenmerk
     *  is its getter; a §8.4.2 COMPOUND of such kenmerks is their combined boolean timeline (ALLE = intersection via
     *  rsTijdKnip, "ten minste 1" = union via rsTijdUnie — both fold over the members' getters). Null when unsupported
     *  (a negated / navigated / multi-period kenmerk, or a mixed / non-kenmerk / other-kwantor compound): a negated
     *  member has no standalone complement here (unlike the totaal CLIP, where the value timeline bounds it), so the
     *  caller markers rather than mis-count. */
    private String conditieTijdreeks(Conditie filter, Objecttype subject) {
        if (filter instanceof Conditie.Kenmerk k) return temporeelKenmerkTijdreeks(k, subject);
        // §8.4.2 ALLE = intersection; "ten minste 1" = union. Only drempel 1 is a union — a "ten minste N>1" is a
        // threshold, not an OR (the resolver already markers it, gedurendeSamengesteld gates telwoord==1; this guard
        // keeps the union semantics honest if a drempel>1 Samengesteld ever reaches here).
        if (filter instanceof Conditie.Samengesteld s
                && (s.kwantor() == Conditie.Kwantor.ALLE
                    || (s.kwantor() == Conditie.Kwantor.TEN_MINSTE && s.drempel() == 1))) {
            boolean alle = s.kwantor() == Conditie.Kwantor.ALLE;
            java.util.List<String> regios = new java.util.ArrayList<>();
            for (Conditie c : s.onderdelen()) {
                // §8.4 a static (non-temporal) comparison member is the value-side guard (mixedGuardWrap), not part of
                // the timeline — admitted only in an ALLE compound here. A mixed OR widens the span (the static-true
                // case), which the WRITE producers do not yet express, so an OR-mixed markers here (only the totaal,
                // tijdreeksAggregatie, handles it).
                if (c instanceof Conditie.Vergelijking) { if (!alle) return null; continue; }
                if (!(c instanceof Conditie.Kenmerk k)) return null;
                String g = temporeelKenmerkTijdreeks(k, subject);
                if (g == null) return null;   // a negated/navigated/multi-period member -> marker (no standalone complement)
                regios.add(g);
            }
            if (regios.isEmpty()) return null;             // no temporal clip (a pure-static compound)
            if (regios.size() == 1) return regios.get(0);  // one kenmerk (+ optional static members -> the guard)
            if (alle) fnImport("rsTijdKnip"); else fnImport("rsTijdUnie");
            String combiner = alle ? "rsTijdKnip" : "rsTijdUnie";
            String acc = regios.get(0);
            for (int i = 1; i < regios.size(); i++) acc = combiner + "(" + acc + ", " + regios.get(i) + ", Segment.class)";
            return acc;
        }
        return null;
    }

    /** §8.4.2 lower an OR-mixed ("ten minste 1": a temporal kenmerk OR a static comparison) gating the §13.4.17.58
     *  tijdsevenredig WRITE producer. The disjunction is span-dependent: a TRUE static member satisfies the OR for the
     *  WHOLE span, a FALSE one leaves only the temporal-union periods. tijdsevenredig HAS a base value-timeline that
     *  bounds the "full span", so (mirroring the totaal-READ OR-mixed at ~line 1757): static TRUE -> prorate the base
     *  over its full extent ({@code rsTijdsevenredig(basis, basis)} — basis-as-condition feeds rsTijdsevenredig the
     *  base's segments as the true periods, i.e. factor 1 per fully-covered bucket); static FALSE -> prorate over the
     *  temporal union only. Null when the compound is not a temporal+static OR-mix or a member is not lowerable (a
     *  negated/navigated temporal member, an unliftable static comparison) -> the caller markers. ONLY tijdsevenredig
     *  lowers an OR-mix: §7.2 aantal-dagen-in has NO value base — its result is intrinsically bounded by the condition's
     *  true-periods (the spec's own caveat at §7.2 line 3005: an ever-true / "zeer lange periode" condition yields a
     *  "zeer groot aantal resultaten"), so a static-TRUE OR (true for ALL time) is UNBOUNDED with no finite
     *  List<Segment>; bounding it by the kenmerk segments would drop the full-day months and under-count, so it stays
     *  an honest marker (verified: aantal-dagen-in OR-mixed markers in both back-ends). */
    private String orMixedTijdsevenredig(Acc acc, Conditie filter, Objecttype subject, String basis, RegelSpraakModel.Granulariteit gran) {
        if (!(filter instanceof Conditie.Samengesteld s) || s.kwantor() != Conditie.Kwantor.TEN_MINSTE || s.drempel() != 1)
            return null;
        java.util.List<Conditie> temporeel = new java.util.ArrayList<>();
        java.util.List<Conditie.Vergelijking> statisch = new java.util.ArrayList<>();
        for (Conditie c : s.onderdelen()) {
            if (c instanceof Conditie.Kenmerk) temporeel.add(c);
            else if (c instanceof Conditie.Vergelijking v) statisch.add(v);
            else return null;   // an unsupported member type -> marker
        }
        if (temporeel.isEmpty() || statisch.isEmpty()) return null;   // not a temporal+static mixed compound
        Conditie.Samengesteld temporeelDeel = new Conditie.Samengesteld(s.kwantor(), s.drempel(), temporeel, s.bron());
        String unionCond = conditieTijdreeks(temporeelDeel, subject);
        if (unionCond == null) return null;   // a negated / navigated / multi-period temporal member -> marker
        String orGuard = statischeGuard(acc, statisch, subject, " || ");
        if (orGuard == null) return null;     // a static member we cannot lower -> marker
        String g = "\"" + gran.name() + "\"";
        return "(" + orGuard
                + " ? rsTijdsevenredig(" + basis + ", " + basis + ", " + g + ", Segment.class)"
                + " : rsTijdsevenredig(" + unionCond + ", " + basis + ", " + g + ", Segment.class))";
    }

    /** §8.4 wrap a temporal-WRITE producer call in a mixed compound's STATIC guard: a non-temporal comparison member
     *  gates the whole per-bucket write — when it fails the doel gets an empty timeline. The call is unchanged for a
     *  pure-temporal filter; null when a static member is not lowerable (the caller then markers). */
    private String mixedGuardWrap(Acc acc, String call, Conditie filter, Objecttype subject) {
        if (!(filter instanceof Conditie.Samengesteld s)) return call;
        java.util.List<Conditie.Vergelijking> statisch = new java.util.ArrayList<>();
        for (Conditie c : s.onderdelen()) if (c instanceof Conditie.Vergelijking v) statisch.add(v);
        if (statisch.isEmpty()) return call;
        String guard = statischeGuard(acc, statisch, subject);
        return guard == null ? null : "(" + guard + " ? " + call + " : new java.util.ArrayList())";
    }

    /** A single positive single-period subject kenmerk's TRUE-period getter ({@code $subject.getK()}), or null when it
     *  is not lowerable as a standalone true-period timeline: a navigated, negated, multi-period, or non-subject
     *  kenmerk. */
    private String temporeelKenmerkTijdreeks(Conditie.Kenmerk k, Objecttype subject) {
        if (genavigeerd(k) || k.negatie() || subject == null
                || !bevatNaam(subject.kenmerkGranulariteitLeden(), k.kenmerk())
                || Tijdlijnen.meerPeriodiek(subject.kenmerkTijdlijnAantalLeden(), k.kenmerk())) return null;
        return SUBJECT + "." + getter(k.kenmerk()) + "()";
    }

    /** §7.1 the boolean-timeline getter for a NAVIGATED temporal kenmerk ("zijn partner een recht op korting heeft") —
     *  the to-ONE role's kenmerk {@code List<Segment>}, root-guarded so a null reference reads as a leeg (null)
     *  timeline (rsTijdKnip then clips to empty). null for a negated / to-many / non-temporal navigated kenmerk. */
    private String navigatieKenmerkTijdreeks(Conditie.Kenmerk k, Objecttype subject) {
        if (!genavigeerd(k) || k.negatie() || subject == null) return null;
        KenmerkHop hop = kenmerkHop(k, subject);
        if (hop == null) return null;   // the bound hop does not fit this subject
        Objecttype doel = hop.doel();
        if (doel == null || !bevatNaam(doel.kenmerkGranulariteitLeden(), k.kenmerk())
                || Tijdlijnen.meerPeriodiek(doel.kenmerkTijdlijnAantalLeden(), k.kenmerk())) return null;
        // §13.3.9: the two-step hop on the timeline arm reads the same reference field naviResolve gates on the
        // scalar arm. Measured 2026-09-11 on a verifier-clean, ref-clean model where a declared member displaces the
        // role name: ref and clingo PASS with the target left leeg, while this branch emitted
        // `$subject.getWerf().getBedrijvig()` and the package died on "Unable to Analyse Expression rsGeheelPeriode(…".
        if (!navVeld("genavigeerde kenmerktijdreeks", subject, hop.rol(), doel)) return null;
        String root = SUBJECT + "." + getter(hop.rol()) + "()";
        return "(" + root + " == null ? null : " + root + "." + getter(k.kenmerk()) + "())";
    }

    /** Whether a kenmerk check navigates: IR-SPEC ch. 4.5, its binding IS the hop. */
    private boolean genavigeerd(Conditie.Kenmerk k) {
        return navigatieBindingen.containsKey(k);
    }

    /** The one hop of a navigated kenmerk check: the reached role (the bean field on the subject) and the type it
     *  reaches, from the wire's {@code navigation} — null when the check does not navigate or that hop does
     *  not fit {@code subject}. */
    private record KenmerkHop(String rol, Objecttype doel) {}
    private KenmerkHop kenmerkHop(Conditie.Kenmerk k, Objecttype subject) {
        if (!genavigeerd(k)) return null;
        FactModelGenerator.Navigatiewandeling w =
                FactModelGenerator.navigatiewandeling(model, subject, navigatieBindingen.get(k), null);
        return w == null || w.rollen().size() != 1 ? null : new KenmerkHop(w.rollen().get(0), w.bereikt().get(0));
    }

    /** The §8.2 whole-period qualifier on a predicate, or null when the predicate carries none. */
    private static Conditie.GeheelPeriode geheelPeriodeVan(Conditie c) {
        if (c instanceof Conditie.Vergelijking v) return v.periode();
        if (c instanceof Conditie.Predicaat p)    return p.periode();
        if (c instanceof Conditie.Kenmerk k)      return k.periode();
        return null;
    }

    /** The §8.2 timeline getter for a temporal attribute read — the subject's own {@code List<Segment>} field, or a
     *  to-ONE navigated partner's, root-guarded so a null reference reads as a leeg (null) timeline rather than NPE.
     *  null for a to-many / unresolvable navigation. Mirrors {@link #tijdreeksAggregatie}'s timeline getter. */
    private String temporeelTijdlijnGetter(Expr.AttribuutRef a, Objecttype subject) {
        NaviRef n = naviResolve(a, subject);
        if (n.soort() == NaviSoort.EIGEN) return SUBJECT + "." + getter(a.attribuut()) + "()";
        if (n.soort() == NaviSoort.GENAVIGEERD) {
            String root = SUBJECT + "." + getter(n.rol()) + "()";
            return "(" + root + " == null ? null : " + root + "." + getter(a.attribuut()) + "())";
        }
        return null;
    }

    /** §8.2 / §13.4.15.70: the RAW boolean {@code rsGeheelPeriode(timeline, "JAAR"|"MAAND", toets, grens)} call for a
     *  whole-period-qualified predicate (the caller appends {@code == true|false}, NIET flips which), or a marker Frag
     *  when the form is outside the supported set. Supported: a numeric comparison of a temporal attribute vs a scalar
     *  grens (§8.2 line 3467), a GEVULD over a temporal attribute, and a temporal kenmerkcheck (§8.2 line 3469). A
     *  LEEG / ELFPROEF / NUMERIEK-met-exact whole-period form is degenerate without a bounding period -> marker. */
    private Frag geheelPeriodeCall(Acc acc, Conditie c, Conditie.GeheelPeriode gp, Objecttype subject) {
        String gran = gp.jaar() ? "JAAR" : "MAAND";
        String getter; int toets; String grens = "null";
        if (c instanceof Conditie.Vergelijking v) {
            Expr.AttribuutRef tijd; Expr ander; boolean linksTemp;
            if (v.links() instanceof Expr.AttribuutRef la && isTemporeelAttribuut(la))      { tijd = la; ander = v.rechts(); linksTemp = true; }
            else if (v.rechts() instanceof Expr.AttribuutRef ra && isTemporeelAttribuut(ra)) { tijd = ra; ander = v.links();  linksTemp = false; }
            else return Frag.todo("§8.2 zonder tijdsafhankelijk attribuut", c.bron());
            if (!(eff.typeVan(tijd).isNumeriek() || eff.typeVan(tijd).isPercentage()))
                return Frag.todo("§8.2 niet-numerieke gehele-periodevergelijking", c.bron());
            // §8.2 cross-unit grens: the grenswaarde is compared against each Segment's value (in the temporal
            // attribute's unit el), so restate it from its own unit er into el via the EXACT factor (the same
            // restatement the scalar comparison path uses — exacteHerschaalFactoren ×÷, one DECIMAL64 rounding). An
            // OPAQUE product unit (no exact factors) stays an honest marker, kept symmetric in both back-ends.
            io.github.yhavinga.regelspraak.model.Eenheid el = eff.eenheidVan(tijd), er = eff.eenheidVan(ander);
            boolean crossUnit = el != null && er != null && el.vereistOmrekening(er);
            if (crossUnit && er.exacteHerschaalFactoren(el) == null)
                return Frag.todo("§8.2 gehele-periodevergelijking met eenheidsomrekening", c.bron());
            Integer code = Tijdrekening.toets(linksTemp ? v.op() : Operatoren.omkeren(v.op()));   // put the temporal operand on the left (REVERSE, not negate)
            if (code == null) return Frag.todo("§8.2 datum-gehele-periodevergelijking", c.bron());
            getter = temporeelTijdlijnGetter(tijd, subject);
            if (getter == null) return Frag.todo("§8.2 navigatie", c.bron());
            Frag g = operand(acc, ander, subject);    // the scalar grenswaarde (literal / parameter-global / own attribute)
            if (g.marker()) return Frag.todo("§8.2 grenswaarde", c.bron());
            toets = code; grens = crossUnit ? herschaalExact(g.text(), effType(ander), effType(tijd)) : g.text();
        } else if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.GEVULD
                && p.operand() instanceof Expr.AttribuutRef pa && isTemporeelAttribuut(pa)) {
            getter = temporeelTijdlijnGetter(pa, subject);
            if (getter == null) return Frag.todo("§8.2 navigatie", c.bron());
            toets = 6;
        } else if (c instanceof Conditie.Predicaat p && p.soort() == Conditie.Predicaatsoort.LEEG
                && p.operand() instanceof Expr.AttribuutRef pa && isTemporeelAttribuut(pa)) {
            // §8.2 line 3468 "... gedurende de gehele maand leeg is": the inverse of GEVULD (toets 6) — every moment
            // of each touched bucket carries NO value (toets 8 in rsGeheelPeriode).
            getter = temporeelTijdlijnGetter(pa, subject);
            if (getter == null) return Frag.todo("§8.2 navigatie", c.bron());
            toets = 8;
        } else if (c instanceof Conditie.Kenmerk k) {
            getter = temporeelKenmerkTijdreeks(k, subject);   // null for a navigated/negated/non-temporal kenmerk
            if (getter == null) getter = navigatieKenmerkTijdreeks(k, subject);   // §8.2 over a to-one NAVIGATED temporal kenmerk (root-guarded)
            if (getter == null) return Frag.todo("§8.2 kenmerk", c.bron());
            toets = 7;
        } else {
            return Frag.todo("§8.2 predicaat", c.bron());     // LEEG / ELFPROEF / NUMERIEK-met-exact whole-period
        }
        fnImport("rsGeheelPeriode");
        return Frag.ok("rsGeheelPeriode(" + getter + ", \"" + gran + "\", " + toets + ", " + grens + ")");
    }

    /** §7.1 clip a value timeline by ONE temporal-kenmerk membership: rsTijdKnip to its TRUE periods, or
     *  rsTijdKnipNiet to the complement for a §8.1.8.1/.2 negated membership ("gedurende de tijd dat hij GEEN …"). */
    private String kenmerkTijdKnip(String waarde, Conditie.Kenmerk k) {
        String knip = k.negatie() ? "rsTijdKnipNiet" : "rsTijdKnip";
        if (k.negatie()) fnImport("rsTijdKnipNiet"); else fnImport("rsTijdKnip");
        return knip + "(" + waarde + ", " + SUBJECT + "." + getter(k.kenmerk()) + "(), Segment.class)";
    }

    /** §8.4.2 fold a COMPOUND gedurende over the value timeline. Every onderdeel must be a single-period temporal
     *  kenmerk of the subject (the resolver gated this; re-checked defensively) -> else null (marker). ALLE nests
     *  the per-kenmerk clips (the intersection of TRUE periods, negation per member). "ten minste 1" (TEN_MINSTE,
     *  drempel 1) clips the value to the UNION of the per-member true-periods. */
    private String samengesteldeTijdKnip(Conditie.Samengesteld s, String waarde, Objecttype subject) {
        for (Conditie c : s.onderdelen())
            if (!(c instanceof Conditie.Kenmerk k) || genavigeerd(k)
                    || !bevatNaam(subject.kenmerkGranulariteitLeden(), k.kenmerk())) return null;
        if (s.kwantor() == Conditie.Kwantor.ALLE) {
            String expr = waarde;
            for (Conditie c : s.onderdelen()) expr = kenmerkTijdKnip(expr, (Conditie.Kenmerk) c);
            return expr;
        }
        if (s.kwantor() == Conditie.Kwantor.TEN_MINSTE) {
            // OR = clip the value to the UNION of each member's true-region. A positive member's region is its
            // getter timeline; a NEGATED member's region within the value is rsTijdKnipNiet(value, getter) (the
            // value ∩ ¬kenmerk — the span-dependent complement, materialised against the value's own segments). The
            // union (rsTijdUnie coalesces the intervals) is then clipped once: value ∩ (A ∪ ¬B ∪ …) == the OR.
            fnImport("rsTijdUnie");
            fnImport("rsTijdKnip");
            String unie = lidSteun(waarde, (Conditie.Kenmerk) s.onderdelen().get(0));
            for (int i = 1; i < s.onderdelen().size(); i++)
                unie = "rsTijdUnie(" + unie + ", " + lidSteun(waarde, (Conditie.Kenmerk) s.onderdelen().get(i)) + ", Segment.class)";
            return "rsTijdKnip(" + waarde + ", " + unie + ", Segment.class)";
        }
        return null;
    }

    /** One OR-member's true-region timeline for the union: a positive member is its kenmerk getter; a negated
     *  member is the value clipped to the kenmerk's FALSE periods (rsTijdKnipNiet over the value), so the complement
     *  is bounded by the value's own span rather than an unbounded ¬kenmerk. */
    private String lidSteun(String waarde, Conditie.Kenmerk k) {
        String g = SUBJECT + "." + getter(k.kenmerk()) + "()";
        if (!k.negatie()) return g;
        fnImport("rsTijdKnipNiet");
        return "rsTijdKnipNiet(" + waarde + ", " + g + ", Segment.class)";
    }

    /**
     * Turn an aggregation into a Drools accumulate: bind a source pattern over the
     * element object type and an accumulate function (sum/count/min/max), register it
     * for the "when", and return the result variable to drop into the value. The
     * element scope — which instances relate to the subject — is left as a marker
     * here; navigation joins fill it. Returns null for aggregations not yet mappable.
     */
    private Frag accumulate(Acc acc, Expr.Functie f, Objecttype subject, boolean leegCount) {
        // §5.7 / IR-SPEC ch. 4.3 / WF-110 the count over an INSTANCE-SET UNION. Read first and
        // returning: the union is a source of its own and carries neither elementObjecttype nor the
        // one-step bronRol every derivation below reads.
        if (f.kind() == Expr.FunctieKind.AANTAL && f.argumenten().size() == 1
                && f.argumenten().get(0) instanceof Expr.InstanceSetUnion unie)
            return unieAccumulate(acc, unie, subject);
        // §4.11/§5.8.1 telling over a concatenation of expressies ("het aantal X, Y en Z"): the
        // count is the UNION of the per-operand sets. When the resolver could OR the per-operand
        // kenmerk filters (f.filter() is the disjunction), this lowers to ONE accumulate whose
        // source pattern carries that OR — an instance matching several operands counts once. A
        // union it could not resolve has no filter -> marker, never a count over the first operand.
        if (f.kind() == Expr.FunctieKind.AANTAL && f.argumenten().size() > 1 && f.filter() == null) return null;
        // §5.8.3 the maximum/minimum of a MULTI-operand value-fold over to-many sets ("de hoogste van de prijs
        // van alle X én de andere prijs van alle X") would need one accumulate per operand folded together;
        // accumulate reads only argumenten().get(0), so operands 2..n (and any die on them) are silently
        // dropped — a wrong max/min. A scalar multi-operand fold is already handled upstream by
        // concatenatieAggregatie (it runs first), so only the over-a-set form reaches here. Mark it rather
        // than fake a partial fold over the first set; the single-operand set MIN/MAX (size 1) is untouched.
        if ((f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.MAXIMUM)
                && f.argumenten().size() > 1) return null;
        // §7.1+§5.8 a SOM/TOTAAL over a TEMPORAL attribute of a SET ("het totaal van <attr per maand> van alle <set>")
        // is a PER-ELEMENT timeline fold: each element's tijdreeks is summed over time (rsTijdTotaal) to a scalar, and
        // those scalars are summed over the set. A bare Drools sum() over the getter would aggregate the List<Segment>
        // itself (a ClassCastException), so the element value is wrapped in rsTijdTotaal below. MIN/MAX over a temporal
        // attribute is ambiguous (min/max of which moment?) -> still a marker. (The explicit "de som van het totaal van
        // X van alle" phrasing attaches the set to the INNER totaal, leaving the outer SOM element-less -> elem == null
        // markers it; the inner totaal is then a single-object operand, not this set fold.)
        boolean temporeelArg = !f.argumenten().isEmpty() && f.argumenten().get(0) instanceof Expr.AttribuutRef ta0
                && ta0.objecttype() != null && bevatNaam(ta0.objecttype().granulariteitLeden(), ta0.attribuut());
        if (temporeelArg && f.kind() != Expr.FunctieKind.SOM && f.kind() != Expr.FunctieKind.TOTAAL)
            return null;
        Objecttype elem = f.elementObjecttype();
        // IR-SPEC ch. 4.3 / RUNTIME-SEMANTICS ch. 3.4c — the FILLED-VALUE count rides the SAME `count`
        // accumulate as the instance count, over a source narrowed to the members whose value exists. The two
        // kinds therefore part on the SCOPE and not on the function, which is why the flag below is a kind test
        // rather than an `fn` test: every place that asks "is this a count?" means "does it fold the element
        // instead of a value?", and both counts answer yes to that.
        boolean gevuldeTelling = f.kind() == Expr.FunctieKind.AANTAL_GEVULDE;
        String fn = switch (f.kind()) {
            case SOM, TOTAAL -> "sum";
            case AANTAL, AANTAL_GEVULDE -> "count";
            case MINIMUM -> "min";
            case MAXIMUM -> "max";
            default -> null;
        };
        if (elem == null || fn == null) return null;
        // The emitted sum()/min()/max() folds a BigDecimal getter, and a member that is not a number
        // has no place in it: until 2026-09-20 this branch emitted DRL that did not COMPILE ("Unable
        // to Analyse Expression") over a date-valued member, which reaches a report as a whole-file
        // compile refusal naming no form at all. Asked through the reason the exec branch shares, so
        // the two branches cannot drift into two wordings for one limit.
        if (!fn.equals("count") && !f.argumenten().isEmpty()
                && f.argumenten().get(0) instanceof Expr.AttribuutRef va) {
            String vw = io.github.yhavinga.regelspraak.transpiler.kern.Vouwwaarde.weigering(f, elem, va);
            if (vw != null) { navReden = vw; return null; }
        }
        // IR-SPEC ch. 4.3 / WF-102: a RANGE that is a PATH replaces the one-step join entirely. Resolved BEFORE
        // scope() and instead of it, because the by-pair search that function performs is exactly the wrong
        // answer here — it binds whatever DIRECT relation happens to connect subject and element and folds that
        // set, which is the misbinding this field closes. A path the bean model cannot walk is a per-form refusal
        // carrying its own sentence (the shared one, so both branches say it in the same words).
        java.util.List<RegelSpraakModel.RolBindingRef> pad = bronPaden.get(f);
        io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.Plan plan = null;
        if (pad != null) {
            plan = io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.ontleed(model, subject, pad, elem);
            if (plan.weigering() != null) { navReden = plan.weigering(); return null; }
        }
        Frag scope = plan != null ? Frag.ok("") : scope(elem, subject, f);
        if (scope.marker()) return null;   // unresolvable join -> marker, never a global aggregate
        // §13.4.2 "die/dat <predicaat>" subselectie on the element ("… van de elementen die een kind
        // zijn") -> an extra constraint on the accumulate source pattern, so only the matching
        // instances are aggregated. A filter the resolver could not structure (Onopgelost) lowers to
        // a marker here -> bail, so the aggregation is a visible no-op rather than counting EVERY
        // instance (a silent miscompile).
        java.util.List<String> bereik = new ArrayList<>();
        if (plan == null) bereik.add(scope.text());
        if (f.filter() != null) {
            Frag fc = conditie(acc, f.filter(), elem);  // Kenmerk -> bare element property; Onopgelost -> marker
            if (fc.marker()) return null;
            bereik.add(fc.text());
        }
        int n = ++acc.n;
        String ev = "$e" + n, var = "$acc" + n;
        // the path's own clauses: an in-pattern constraint when the two functional walks meet, else an
        // `exists` over the intermediates AFTER the element pattern — the dedup the flat union asks for.
        String naPatroon = "";
        if (plan != null) {
            PadClausules pc = padClausules(plan, ev, n);
            if (!pc.constraint().isEmpty()) bereik.add(0, pc.constraint());
            naPatroon = pc.naPatroon();
        }
        String filterScope = samen(bereik);
        Expr arg = f.argumenten().isEmpty() ? null : f.argumenten().get(0);
        // §7.1 a per-element timeline fold: a temporal-attribute element value is summed over time (rsTijdTotaal at the
        // attribute's granularity) to a scalar BEFORE the set aggregate, instead of the bare List<Segment> getter.
        java.util.function.Function<Expr.AttribuutRef, String> elemWaarde = a -> {
            String getr = ev + "." + getter(a.attribuut()) + "()";
            if (!temporeelArg) return getr;
            fnImport("rsTijdTotaal");
            return "rsTijdTotaal(" + getr + ", \"" + opNaam(a.objecttype().granulariteitLeden(), a.attribuut()).name() + "\")";
        };

        // §4.10/§5.8.2 sommatie ("som van"): an empty sum (no non-empty contributors) is a
        // leegwaarde, not 0 — unless "of … als die er niet zijn" supplies a fallback. Drools
        // sum() can't tell an empty sum from a real 0, so count the non-empty contributors and
        // branch on it. TOTAAL ("totaal van") is excluded: §4.20 makes its empty result an
        // unconditional 0, which the plain sum() over an empty accumulate already yields.
        // Needs a resolved join and a plain attribute argument; otherwise fall through.
        if (f.kind() == Expr.FunctieKind.SOM
                && arg instanceof Expr.AttribuutRef a && !scope.marker()) {
            String cnt = "$n" + n;
            acc.clausules.add("accumulate( " + ev + " : " + ident(elem.naam, true)
                    + "( " + samen(bereik, ident(a.attribuut(), false) + " != null") + " )" + naPatroon + " ; "
                    + cnt + " : count(" + ev + "), " + var + " : sum( " + elemWaarde.apply(a) + " ) )");
            Frag fallback = f.leegwaarde() != null ? waarde(acc, f.leegwaarde(), subject) : Frag.ok("null");
            return Frag.from("(" + cnt + " == 0 ? " + fallback.text() + " : " + var + ")", fallback);
        }

        String elementExpr = ev;
        if (!fn.equals("count") && arg instanceof Expr.AttribuutRef a)
            elementExpr = elemWaarde.apply(a);
        // §4.12 a leegwaarde (null) contributor is not counted — AND (crucially for a source DERIVED in the same
        // fireAllRules pass) an element must not enter the accumulate window until its value exists: an element joined
        // with a null value, then modify-fired once another rule of the cycle derives it, triggers a phreak REVERSE of
        // the null prior contribution, which crashes ("Reversing a not existing accumulated object"). A `!= null` source
        // guard makes that transition an INSERT (null -> present) rather than a reverse. (SOM guards above; extend it to
        // TOTAAL/MIN/MAX over a plain attribute. A temporal per-element fold reads a List<Segment> getter, never null.)
        // The filled count joins that guard rather than the bare scope: for it the `!= null` is not a defensive
        // measure against a mid-pass derivation but the FOLD ITSELF — it is what separates a member that has a
        // value from one that does not, and dropping it turns the kind back into the instance count.
        String foldScope = filterScope;
        if ((!fn.equals("count") || gevuldeTelling) && !temporeelArg && arg instanceof Expr.AttribuutRef a)
            foldScope = samen(bereik, ident(a.attribuut(), false) + " != null");
        // §5.8.3 an empty MIN/MAX is LEEG (§5.3 Tabel 37 / §5.6 Tabel 44), but a bare min()/max() accumulate binds NO
        // result row over an EMPTY source — so a downstream CONDITION compare would never run (the rule silently reads
        // onwaar, wrong for leeg==leeg / leeg!=gevuld). In a condition context (leegCount) bind count() alongside the
        // min/max so the accumulate ALWAYS produces a row; the result is null for an empty set (count==0) and the min/max
        // value otherwise — letting rsAggVergelijk apply the leegwaarde semantics. The VALUE side (leegCount=false) keeps
        // the bare fold: an empty value-aggregation simply does not fire (unchanged).
        if (leegCount && (f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.MAXIMUM)) {
            String cnt = "$n" + n;
            acc.clausules.add("accumulate( " + ev + " : " + ident(elem.naam, true) + "( " + foldScope + " )" + naPatroon
                    + " ; " + cnt + " : count(" + ev + "), " + var + " : " + fn + "( " + elementExpr + " ) )");
            return Frag.ok("(" + cnt + " == 0 ? null : " + fnImport("toBd") + "(" + var + "))");
        }
        acc.clausules.add("accumulate( " + ev + " : " + ident(elem.naam, true)
                + "( " + foldScope + " )" + naPatroon + " ; " + var + " : " + fn + "( " + elementExpr + " ) )");
        // §5.8.3 a min/max reduction binds $accN as a java.lang.Number (the element's static type in the accumulate),
        // so a downstream cross-unit restate (.multiply/.divide) or arithmetic on it fails Drools' strict-mode method
        // resolution (Number has no .multiply). Coerce to BigDecimal, null-safe (an empty min/max stays leeg), mirroring
        // the exec's RegelSpraakRuntime.toBd. SUM already yields a BigDecimal accumulate; COUNT stays an integer count.
        if (f.kind() == Expr.FunctieKind.MINIMUM || f.kind() == Expr.FunctieKind.MAXIMUM)
            return Frag.ok(fnImport("toBd") + "(" + var + ")");
        return Frag.ok(var);
    }

    /** §5.7 / IR-SPEC ch. 4.3 / WF-110 — the instance count over a UNION of selections, semantics
     *  RUNTIME-SEMANTICS ch. 3.4d. ONE accumulate over ONE element pattern whose constraint is the
     *  DISJUNCTION of the selections, each selection being its own join AND its own die/dat filter:
     *
     *  <pre>accumulate( $e1 : Vondst( (aanmelder == $subject &amp;&amp; gemerkt == true) || (berger == $subject) )
     *              ; $acc1 : count($e1) )</pre>
     *
     *  <p><b>One pattern is what deduplicates.</b> A pattern matches an element once whatever makes
     *  it match, so an instance two selections both select contributes ONE tuple and the count
     *  answers |A ∪ B| — the operation ch. 3.4d defines, against the sum of the parts. Two
     *  accumulates added together would count the shared element twice, and a DRL `or` between
     *  patterns binding the same variable produces one match per satisfied branch. Same shape, same
     *  argument and same numbers as the exec-model branch's {@code unieTelling}.
     *
     *  <p>null -> marker, with {@link #navReden} naming the form, never a silently wrong count. */
    private Frag unieAccumulate(Acc acc, Expr.InstanceSetUnion u, Objecttype subject) {
        if (u.takken().isEmpty()) return null;
        Objecttype elem = u.takken().get(0).elementType();
        for (Expr.InstanceSetUnion.Tak t : u.takken())
            if (t.elementType() != elem) {          // WF-110 (d), re-asked: one pattern has one class
                navReden = "instantie-unie over twee populaties ('" + (elem == null ? "?" : elem.naam)
                        + "' en '" + (t.elementType() == null ? "?" : t.elementType().naam) + "'): één"
                        + " telling antwoordt een aantal over ÉÉN gedeclareerd type (WF-110 (d))";
                return null;
            }
        if (elem == null) return null;
        java.util.List<String> delen = new ArrayList<>();
        for (Expr.InstanceSetUnion.Tak t : u.takken()) {
            String join = unieScope(elem, subject, t.bronRol());
            if (join == null) return null;          // navReden set by unieScope
            if (t.filter() == null) { delen.add("(" + join + ")"); continue; }
            Frag fc = conditie(acc, t.filter(), elem);
            if (fc.marker()) return null;
            delen.add("(" + join + " && " + fc.text() + ")");
        }
        int n = ++acc.n;
        String ev = "$e" + n, var = "$acc" + n;
        acc.clausules.add("accumulate( " + ev + " : " + ident(elem.naam, true)
                + "( " + String.join(" || ", delen) + " ) ; " + var + " : count(" + ev + ") )");
        return Frag.ok(var);
    }

    /** The join constraint of ONE union selection, read ON THE ELEMENT — derived from the wire
     *  binding ALONE and never from the type pair, which is WF-110 (e)'s ground as code: every
     *  selection of one union shares one pair, so a by-pair search would give them all the same
     *  range and the union would restate one of them. The §13.3.9 bean-field question is asked
     *  through the shared derivations, so both branches keep one wording per limit. */
    private String unieScope(Objecttype elem, Objecttype subject, RegelSpraakModel.RolBindingRef binding) {
        FactModelGenerator.Bereikjoin bj = FactModelGenerator.bereikjoin(model, binding);
        String rol = bj.rol();
        if (rol == null) {
            navReden = bj.weigering() != null ? bj.weigering()
                    : "de relatie van een instantie-unie-selectie is niet af te leiden uit haar binding (WF-110 (e))";
            return null;
        }
        if (!navVeld("instantie-unie-join", elem, rol, subject)) return null;
        return ident(rol, false) + " == " + SUBJECT;
    }

    /** Join non-empty constraint texts into a DRL pattern's constraint list. Empty in, empty out — a pattern
     *  with no constraints is {@code Type( )}, which is legal, whereas a stray comma is not. */
    private static String samen(java.util.List<String> delen, String... extra) {
        java.util.List<String> alle = new ArrayList<>(delen);
        alle.addAll(java.util.Arrays.asList(extra));
        StringBuilder b = new StringBuilder();
        for (String d : alle) {
            if (d == null || d.isEmpty()) continue;
            if (b.length() > 0) b.append(", ");
            b.append(d);
        }
        return b.toString();
    }

    /** A WF-102 path's DRL: the constraint that goes ON the element pattern (empty when the walk alternates) and
     *  the clause that follows it inside the accumulate source (empty when it does not). */
    private record PadClausules(String constraint, String naPatroon) {}

    /**
     * IR-SPEC ch. 4.3 / WF-102 — the fold-source RANGE as a path, in DRL text. The shape mirrors the exec branch
     * one-for-one, because the two answer to the same bean model: §13.3.9 stores every relation in ONE scalar
     * reference field, so a backward hop is a getter FROM the element and a forward hop a getter FROM the subject.
     *
     * <p>A path that is a forward run followed by a backward run is therefore one equality between two getter
     * chains — every dereferenced prefix null-guarded, as the §13.4.1 chain reader guards its hops, so an
     * intermediate that reaches nothing drops out of the union instead of aborting the fire. No element can be
     * reached twice that way.
     *
     * <p>Where a BACKWARD hop is followed by a FORWARD one the intermediate is a real existential — two
     * intermediates may reach the same element — so it becomes a pattern inside ONE {@code exists} after the
     * element pattern. That keeps the element pattern the only tuple generator: the element contributes ONCE
     * however many routes reach it, which is exactly what joining the intermediates into the source would break.
     */
    private PadClausules padClausules(io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.Plan plan,
            String ev, int n) {
        if (plan.ontmoet()) {
            int m = plan.ontmoeting();
            java.util.List<String> bewaking = new ArrayList<>();
            String links = keten("this", plan.ketenVanafElement(m), bewaking, true);
            String rechts = keten(SUBJECT, plan.ketenVanafOnderwerp(m), bewaking, false);
            return new PadClausules("(" + samenEn(bewaking, links + " == " + rechts) + ")", "");
        }
        java.util.Map<Integer, String> naam = new java.util.LinkedHashMap<>();
        for (int i : plan.tussenknopen()) naam.put(i, "$pm" + n + "_" + i);
        StringBuilder ex = new StringBuilder();
        int laatste = plan.achterwaartsVanaf() - 1;
        for (int i : plan.tussenknopen()) {
            java.util.List<String> cs = new ArrayList<>();
            cs.add(hop(plan, i, i, ev, naam));                                  // the hop INTO this node
            if (i == laatste) cs.add(hop(plan, plan.achterwaartsVanaf(), i, ev, naam));   // and the hop OUT of it
            if (ex.length() > 0) ex.append(" and ");
            ex.append(naam.get(i)).append(" : ").append(ident(plan.knoopTypen().get(i).naam, true))
              .append("( ").append(samen(cs)).append(" )");
        }
        return new PadClausules("", " and exists( " + ex + " )");
    }

    /** One hop's constraint text, seen from the pattern of node {@code dit}. */
    private String hop(io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.Plan plan, int stap, int dit,
            String ev, java.util.Map<Integer, String> naam) {
        java.util.List<String> bewaking = new ArrayList<>();
        String bron = knoop(plan, stap - 1, dit, ev, naam, bewaking);
        String doel = knoop(plan, stap, dit, ev, naam, bewaking);
        var st = plan.stappen().get(stap - 1);
        String kern = st.achterwaarts()
                ? doel + "." + getter(st.beanveld()) + "() == " + bron
                : bron + "." + getter(st.beanveld()) + "() == " + doel;
        // the DEREFERENCED side must exist before its getter runs; the other side must exist for the equality to
        // MEAN reachability (two broken walks are not a meeting)
        bewaking.add(st.achterwaarts() ? doel : bron);
        bewaking.add(st.achterwaarts() ? bron : doel);
        return "(" + samenEn(bewaking, kern) + ")";
    }

    /** The text denoting node {@code j}, plus any null-guards its getter chain needs. */
    private String knoop(io.github.yhavinga.regelspraak.transpiler.kern.Bronpad.Plan plan, int j, int dit,
            String ev, java.util.Map<Integer, String> naam, java.util.List<String> bewaking) {
        if (j == dit) return "this";
        if (j <= plan.voorwaarts()) return keten(SUBJECT, plan.ketenVanafOnderwerp(j), bewaking, false);
        if (j >= plan.achterwaartsVanaf()) return keten(ev, plan.ketenVanafElement(j), bewaking, false);
        return naam.get(j);
    }

    /** {@code wortel.getA().getB()}, recording every prefix that gets DEREFERENCED as a null-guard, plus — when
     *  {@code eindNietLeeg} — the whole expression, so a walk that ends nowhere never equals another that does. */
    private String keten(String wortel, java.util.List<String> velden, java.util.List<String> bewaking,
            boolean eindNietLeeg) {
        String uit = wortel;
        for (int i = 0; i < velden.size(); i++) {
            if (i > 0) bewaking.add(uit);
            uit = uit + "." + getter(velden.get(i)) + "()";
        }
        if (eindNietLeeg && !velden.isEmpty()) bewaking.add(uit);
        return uit;
    }

    /** {@code a != null && b != null && <kern>} — the guards first, deduplicated, and never on a bare variable
     *  ({@code this}, {@code $subject} and a bound pattern variable are non-null by construction). */
    private static String samenEn(java.util.List<String> bewaking, String kern) {
        StringBuilder b = new StringBuilder();
        java.util.Set<String> gezien = new java.util.LinkedHashSet<>();
        for (String g : bewaking) if (g != null && g.indexOf('.') >= 0) gezien.add(g);
        for (String g : gezien) b.append(g).append(" != null && ");
        return b.append(kern).toString();
    }

    /** The element scope: the join binding the element to the subject ("the elements
     *  OF this houder" -> houder == $subject), if the connecting feittype is unambiguous;
     *  otherwise the source phrase as a marker. */
    private Frag scope(Objecttype elem, Objecttype subject, Expr.Functie f) {
        Expr.AttribuutRef arg = !f.argumenten().isEmpty() && f.argumenten().get(0) instanceof Expr.AttribuutRef a ? a : null;
        // IR-SPEC ch. 4.3: the fold's range IS its join. The binding names the role the ELEMENT plays, and the
        // field on the element that points back at the subject is its sibling, the role the SUBJECT plays — read
        // from the one binding as the exec branch reads it.
        RegelSpraakModel.Rol[] paar = FactModelGenerator.gebondenRolpaar(model, rolBindingen.get(f));
        String rol = paar == null ? null : paar[1].naam();
        // §13.3.9 STORAGE DIRECTION: a resolved ROLE is not yet a bean FIELD. FactModelGenerator.leden derives a
        // scalar reference only where the role is to-one, uniquely reachable, and its name is not taken by a
        // declared member — so the binding can name a role the beans do not carry. The join emitted
        // below reads `<rol>` ON THE ELEMENT, which is therefore the side the shared question is asked of. The
        // exec counterpart has asked it since 2026-08-30; this branch asked nothing and emitted a getter nobody
        // has, sinking the whole KieBase on a bare compiler error that named neither role nor member. A refusal
        // falls through to the marker route below, where navReden sharpens the placeholder's generic text.
        // NOT mirrored from exec: its SEPARATE, conservative refusal of a to-one element role whose sister role
        // is to-many (aggregatieReden) — lifting or copying that one shifts gate-12 channels and needs its own
        // measurement.
        if (rol != null && navVeld("aggregatie-join", elem, rol, subject))
            return Frag.ok(ident(rol, false) + " == " + SUBJECT);
        // navVeld's generic sentence cannot reach the CARDINALITY diagnosis (a to-many role whose far end
        // derives nothing either), and until 2026-09-18 only the exec branch named it — one fold-join failure,
        // two sentences. Restate it here from the shared derivation, at the one seam where "both sides to-many"
        // is the true reading; every other navVeld caller keeps the generic one, which is correct for them.
        if (rol != null)
            navReden = "aggregatie-join: "
                    + FactModelGenerator.geenJoinveldReden(model, elem, rol, subject) + " (§13.3.9 opslagrichting)";
        // No join resolvable: emit a TODO marker, so accumulate() returns null and the aggregation
        // becomes a visible no-op — never a silently-wrong count over EVERY instance (§5.8.1: a count
        // ranges over the subject's related instances). Consistent with the resolver's "unresolvable
        // -> marker" rule.
        return Frag.todo("aggregatie-bereik", arg != null ? arg.bron().snippet() : f.bron().snippet());
    }

    /** §5.8/§13.4.14 an AGGREGATION (count / SUM / TOTAAL / MIN / MAX) as one side of a condition comparison ("het aantal /
     *  de som van / de maximale waarde van &lt;elem&gt; van &lt;subject&gt; &lt;op&gt; RHS") — lowered to a result-constrained
     *  accumulate CE hoisted after the subject pattern (an EMPTY-inline {@link LoweredConditie} whose extra is the accumulate
     *  + an rsAggVergelijk eval). The RHS may be a literal, an own attribute, a numeric scalar Parameter-global, or a to-ONE
     *  navigated numeric attribute (null-safe via rsRelGetal); any comparison operator (ordering / GELIJK / ONGELIJK, the
     *  §5.3/§5.6 leegwaarde cells). An empty SOM/MIN/MAX reads leeg (leegCount). BOTH sides aggregations = agg-vs-agg (two
     *  accumulate CEs). null when unlowerable (an unresolved join / a navigated-or-multi-global RHS / a to-many RHS) -> the
     *  caller falls through to the marker path. Mirrors the exec Gate. */
    private LoweredConditie aggregatieVoorwaarde(Acc acc, Conditie.Vergelijking v, Objecttype subject) {
        return aggregatieVoorwaarde(acc, v, subject, false);
    }
    /** {@code negeer} negates the boolean RESULT of the comparison (an eval-level {@code !( … )}), NOT the operator — for
     *  a single aggregation bullet under a GEEN quantifier ("aan geen van de volgende voorwaarden: - het aantal … > 10").
     *  Result-negation is leegwaarde-correct (NOT(SOM(leeg) > 10)=WAAR, whereas SOM(leeg) <= 10 stays onwaar). */
    private LoweredConditie aggregatieVoorwaarde(Acc acc, Conditie.Vergelijking v, Objecttype subject, boolean negeer) {
        if (subject == null) return null;
        // accumulate() appends its CE to the THREADED Acc context (no-mutable-build-state) before returning the result Frag.
        // A post-accumulate bail (an unresolvable 2nd aggregation in agg-vs-agg, a to-many/unresolvable navigated RHS) must
        // roll that back via aggTerug(acc, mark) — else the marker rule keeps a dangling ORPHAN accumulate (inert but malformed).
        final int mark = acc.clausules.size();
        boolean linksAgg = v.links() instanceof Expr.Functie fl && isAggregatieFunctie(fl.kind());
        boolean rechtsAgg = v.rechts() instanceof Expr.Functie fr && isAggregatieFunctie(fr.kind());
        if (!linksAgg && !rechtsAgg) return null;               // no aggregation operand
        if (linksAgg && rechtsAgg) {                            // §5.8 agg-vs-agg ("het aantal X <op> het aantal Y")
            // Two accumulate CEs over the same subject; the eval compares both results via rsAggVergelijk (the right agg
            // coerced to BigDecimal via toBd). leegCount=true so an empty SOM/MIN/MAX on either side reads leeg.
            Frag a1 = accumulate(acc, (Expr.Functie) v.links(), subject, true);
            if (a1 == null || a1.marker()) return aggTerug(acc, mark);
            Frag a2 = accumulate(acc, (Expr.Functie) v.rechts(), subject, true);
            if (a2 == null || a2.marker()) return aggTerug(acc, mark);
            String cmp = fnImport("rsAggVergelijk") + "(" + a1.text() + ", "
                    + fnImport("toBd") + "(" + a2.text() + "), \"" + Operatoren.symbool(v.op()) + "\")";
            acc.clausules.add("eval( " + (negeer ? "!(" + cmp + ")" : cmp) + " )");
            return new LoweredConditie("", java.util.List.of(), false);
        }
        Expr.Functie f = (Expr.Functie) (linksAgg ? v.links() : v.rechts());
        Expr ander = linksAgg ? v.rechts() : v.links();
        // scope: a LITERAL, an OWN (non-navigated) attribute, a numeric scalar Parameter-GLOBAL, or a numeric to-ONE
        // NAVIGATED attribute threshold (the eval has $subject and the header-declared globals in scope). The own-attr
        // containsKey guard is load-bearing: a navigation the resolver could NOT encode surfaces as navRol==null with a
        // name NOT on the subject — the exec's scalarWaarde would build a LIVE crashing rule for it (review wf_900d3055-a62
        // F2), so ONLY a PROPER navigation (navRol != null, resolved to a to-one below via naviResolve / navReader) is
        // admitted, read null-safely (rsRelGetal / navReader). The global/navigated attr must be numeric (isNumeriek/
        // isPercentage -> a BigDecimal, matching rsAggVergelijk's `n`) and non-temporal; a global is also scalar
        // (granulariteit==null) — EXACTLY the set the exec tracks/reads. A tekst/datum/temporal/to-many RHS stays a marker.
        // ALSO admitted: a numeric arithmetic EXPRESSION reading 2 DISTINCT scalar numeric globals ("global A plus
        // global B") — DRL renders each global as a free mvel name in the eval (waarde() below), the exec binds each into
        // its own Optional from-decl (aggMultiGlobalRhs). Capped at 2 distinct globals to match the exec Predicate-arity
        // ceiling; 3+ distinct stays a symmetric marker. A single global goes through the ParameterRef arm above.
        boolean multiGlobalRhs = !(ander instanceof Expr.AttribuutRef) && !(ander instanceof Expr.Literal)
                && !(ander instanceof Expr.ParameterRef) && aggRhsGlobalNamen(ander).size() == 2;
        if (!(ander instanceof Expr.Literal)
                && !(ander instanceof Expr.AttribuutRef ar && ongenavigeerd(ar) && ar.objecttype() == subject
                     && subject.attribuutLeden().containsKey(ar.attribuut()))
                && !(ander instanceof Expr.ParameterRef pr && pr.parameter().granulariteit() == null
                     && (pr.parameter().datatype().isNumeriek() || pr.parameter().datatype().isPercentage()))
                && !(ander instanceof Expr.AttribuutRef nav && nav.objecttype() != null
                     && (!ongenavigeerd(nav) || nav.objecttype() != subject) && nav.objecttype().heeftLid(nav.attribuut())
                     && eff.typeVan(nav) != null && (eff.typeVan(nav).isNumeriek() || eff.typeVan(nav).isPercentage())
                     && !isTemporeelAttribuut(nav))
                && !multiGlobalRhs) return null;
        Conditie.Operator op = linksAgg ? v.op() : Operatoren.omkeren(v.op());   // orient the aggregation on the LEFT (Operatoren.omkeren(ONGELIJK)=ONGELIJK, != symmetric)
        // Reuse the VALUE-side accumulate lowering: it registers the accumulate CE (count / BigDecimal sum with the §5.8.2
        // empty-sum leegwaarde branch / min·max coerced to BigDecimal) in the Acc context and returns the RESULT value expr
        // ($accN, or the SOM/min·max count-branch ternary). The comparison then rides an eval AFTER the accumulate (both
        // flushed after the subject pattern) via rsAggVergelijk. leegCount=true so an empty SOM/MIN/MAX binds a count row
        // -> the compare runs with a null aggregate and applies the §5.3/§5.6 leegwaarde cells (leeg==leeg / leeg!=gevuld).
        Frag agg = accumulate(acc, f, subject, true);
        if (agg == null || agg.marker()) return aggTerug(acc, mark);   // accumulate() returns NULL (not a marker Frag) for an
                                                        // unresolvable join / unstructured die-filter / multi-operand fold
        // the RHS rides the EVAL (a standalone when-clause), so it needs a $subject-qualified value ($subject.getX()), NOT
        // operand()'s pattern-relative field name. A literal/own-attr/global goes through waarde() ($subject-qualified). A
        // to-ONE navigated numeric read goes through the null-safe rsRelGetal($subject.get<Rol>(), "attr") — an absent
        // relation reads leeg, NOT a short-circuit or an NPE (waarde() would emit the un-guarded $subject.getRol().getAttr()).
        Frag n;
        if (ander instanceof Expr.AttribuutRef nar && (!ongenavigeerd(nar) || nar.objecttype() != subject)) {
            NaviRef nr = naviResolve(nar, subject);
            if (nr.soort() != NaviSoort.GENAVIGEERD) return aggTerug(acc, mark);   // AFWEZIG / NAAR_VEEL / ONOPGELOST -> marker
            fnImport("rsRelGetal");
            n = Frag.ok("rsRelGetal(" + SUBJECT + "." + getter(nr.rol()) + "(), \"" + nar.attribuut() + "\")");
        } else {
            n = waarde(acc, ander, subject);
            if (n.marker()) return aggTerug(acc, mark);
        }
        String cmp = fnImport("rsAggVergelijk") + "(" + agg.text() + ", " + n.text() + ", \"" + Operatoren.symbool(op) + "\")";
        acc.clausules.add("eval( " + (negeer ? "!(" + cmp + ")" : cmp) + " )");
        return new LoweredConditie("", java.util.List.of(), false);
    }

    /** The DISTINCT scalar-numeric Parameter-global names an aggregation-condition RHS expression reads, walking the
     *  arithmetic tree (Binair/Afgerond/Begrensd/Functie args). Only §13.3.8 scalar (granulariteit==null) numeric/
     *  percentage parameters count — a temporal or non-numeric parameter yields an EMPTY set, so the multi-global arm
     *  declines it (staying a marker). A §13.4.1 multi-hop CHAIN ref anywhere in the tree POISONS the walk (empty set):
     *  this arm lowers the RHS through the value-side {@code waarde()}, which fires chains, while the exec's condition
     *  path (ketenWaardePad off) declines them — admitting it here would be a DRL-fires/exec-skips split. Used by
     *  {@link #aggregatieVoorwaarde} to admit the 2-distinct-global RHS. */
    private Set<String> aggRhsGlobalNamen(Expr e) {
        Set<String> uit = new java.util.LinkedHashSet<>();
        return aggRhsGlobalNamen(e, uit) ? uit : java.util.Set.of();
    }
    private boolean aggRhsGlobalNamen(Expr e, Set<String> uit) {
        if (e instanceof Expr.AttribuutRef a) return !keten(a);   // a chain leg -> poison (see doc)
        if (e instanceof Expr.ParameterRef p) {
            if (p.parameter().granulariteit() == null
                    && (p.parameter().datatype().isNumeriek() || p.parameter().datatype().isPercentage()))
                uit.add(p.parameter().naam());
            return true;
        }
        if (e instanceof Expr.Binair b) return aggRhsGlobalNamen(b.links(), uit) && aggRhsGlobalNamen(b.rechts(), uit);
        if (e instanceof Expr.Afgerond a) return aggRhsGlobalNamen(a.basis(), uit);
        if (e instanceof Expr.Begrensd bg)
            return aggRhsGlobalNamen(bg.basis(), uit)
                    && (bg.minimum() == null || aggRhsGlobalNamen(bg.minimum(), uit))
                    && (bg.maximum() == null || aggRhsGlobalNamen(bg.maximum(), uit));
        if (e instanceof Expr.Functie fn) {
            for (Expr a : fn.argumenten()) if (!aggRhsGlobalNamen(a, uit)) return false;
            return true;
        }
        return true;
    }

    /** Roll back any accumulate CEs {@link #accumulate} appended into the Acc context since {@code mark}, then signal
     *  "not lowered" (null) — so a post-accumulate bail (unresolvable 2nd agg / to-many navigated RHS) leaves NO orphan
     *  accumulate in the marker rule (review wf_ee9d1919-c3c). */
    private LoweredConditie aggTerug(Acc acc, int mark) {
        if (acc.clausules.size() > mark) acc.clausules.subList(mark, acc.clausules.size()).clear();
        return null;
    }

    private static boolean isAggregatieFunctie(Expr.FunctieKind k) {
        return k == Expr.FunctieKind.AANTAL || k == Expr.FunctieKind.SOM || k == Expr.FunctieKind.TOTAAL
                || k == Expr.FunctieKind.MINIMUM || k == Expr.FunctieKind.MAXIMUM
                || k == Expr.FunctieKind.AANTAL_GEVULDE;
    }
    /** Does the comparison have an aggregation (count/sum/totaal/min/max) as either operand? — a bullet routed through
     *  {@link #aggregatieVoorwaarde} rather than the counting fold's inline-boolean expansion (which cannot host it). */
    private static boolean isAggregatieVergelijking(Conditie.Vergelijking v) {
        return (v.links() instanceof Expr.Functie fl && isAggregatieFunctie(fl.kind()))
                || (v.rechts() instanceof Expr.Functie fr && isAggregatieFunctie(fr.kind()));
    }

    // ---------------- condition ("when" side: bare field constraints) ----------------
    /** A lowered condition: the inline mvel constraint spliced into the subject pattern, plus zero or
     *  more SEPARATE when-clauses (exists/not joins) appended AFTER it. {@code extra} is non-empty only
     *  for a hoistable far-side membership (§8.1.7 "heeft een &lt;rol&gt;") — directly, or carried up
     *  through a pure conjunction (§13.4.14 alle/de). An exists-join cannot live inside the in-pattern
     *  constraint, so it rides here instead; a caller that has nowhere to append it degrades to a marker
     *  via {@link #conditieInline}. */
    private record LoweredConditie(String inline, List<String> extra, boolean marker) {
        /** A fully-lowered constraint (no placeholder), with no hoisted clauses. */
        static LoweredConditie van(String inline) { return new LoweredConditie(inline, List.of(), false); }
        /** An unresolved constraint: the always-false {@code false /* TODO … *​/} placeholder, flagged. */
        static LoweredConditie marker(String wat, String bron) {
            return new LoweredConditie(onopgelosteConstraint(wat, bron), List.of(), true);
        }
        /** As {@link #marker(String, String)} but from a structured {@link Bron} (its snippet is spliced). */
        static LoweredConditie marker(String wat, Bron bron) {
            return marker(wat, bron == null ? null : bron.snippet());
        }
        boolean heeftExtra() { return !extra.isEmpty(); }
    }

    /** The inline constraint of a condition for a caller that has NO place to append an extra when-clause
     *  (the §13.4.8 schending OR/negation, an accumulate source pattern, the feitcreatie anker/$doel
     *  patterns, the objectcreatie guard). A condition that WANTS to hoist a join ({@code heeftExtra})
     *  has no faithful inline form here, so it collapses to the always-false marker {@link Frag} — exactly
     *  the un-lowerable signal these sites already detect via {@link Frag#marker}, never a silent drop. */
    private Frag conditieInline(Acc acc, Conditie c, Objecttype subject, boolean alsVoorwaarde) {
        LoweredConditie lc = conditie(acc, c, subject, alsVoorwaarde);
        if (lc.heeftExtra()) return new Frag(onopgelosteConstraint("voorwaarde", c.bron()), true);
        return new Frag(lc.inline(), lc.marker());
    }

    private Frag conditie(Acc acc, Conditie c, Objecttype subject) { return conditieInline(acc, c, subject, false); }

    /** Lower a condition to an mvel constraint plus any hoisted join clauses. {@code alsVoorwaarde} marks a
     *  rule-GUARD / filter context, where the §5.3 both-empty rule applies; it is false when this is the
     *  NEGATION of a consistentie assertion (built by {@link #schendingConstraint}'s complement / negeer
     *  tail), because there a both-empty "fout" must still FIRE the violation (Tabel 44), so the guard
     *  must NOT be added. */
    private LoweredConditie conditie(Acc acc, Conditie c, Objecttype subject, boolean alsVoorwaarde) {
        // §8.2 / §13.4.15.70 "gedurende de/het gehele jaar|maand": a whole-period check over a tijdsafhankelijk
        // onderwerp (a List<Segment> timeline). Lower to a boolean rsGeheelPeriode(...) term BEFORE the scalar paths
        // below (which would marker the temporal operand). A form §8.2 cannot lower stays an honest marker.
        Conditie.GeheelPeriode gp = geheelPeriodeVan(c);
        if (gp != null) {
            Frag f = geheelPeriodeCall(acc, c, gp, subject);
            if (f.marker()) return LoweredConditie.marker("§8.2 gehele-periode", c.bron());
            return LoweredConditie.van(f.text() + " == " + (gp.niet() ? "false" : "true"));
        }
        // §8.4.3 / §13.4.15.68-.69 periodevergelijking: the rule applies during the period, and the engine
        // evaluates scalar rules as of the §4.2 Rekendatum — so lower to "rekendatum inside [vanaf, tot)"
        // (§5.1.3: vanaf inclusive; tot exclusive; tot en met inclusive), the expression-bound analogue of the
        // geldigheidGuard window. Bounds are restricted to the shapes the exec back-end mirrors — a Datum-in-dagen
        // literal or the subject's OWN scalar Datum-in-dagen attribute (null-guarded: a leeg bound never matches);
        // anything else (parameter/navigated/computed/Datum-en-tijd) stays a SYMMETRIC honest marker.
        if (c instanceof Conditie.Periode per) {
            StringJoiner sj = new StringJoiner(" && ");
            sj.add("rekendatum != null");
            for (boolean isVan : new boolean[]{true, false}) {
                Expr grens = isVan ? per.vanaf() : per.tot();
                if (grens == null) continue;
                if (!Vensterpositie.grensLowerbaar(grens, subject, a -> !ongenavigeerd(a)))
                    return LoweredConditie.marker("periodevergelijking-grens '" + grens.bron() + "' (§8.4.3)", c.bron());
                Frag g = operand(acc, grens, subject);
                if (g.marker()) return LoweredConditie.marker("periodevergelijking-grens (§8.4.3)", c.bron());
                if (!(grens instanceof Expr.Literal)) sj.add(g.text() + " != null");
                sj.add(isVan ? "!rekendatum.isBefore(" + g.text() + ")"
                             : (per.totInclusief() ? "!rekendatum.isAfter(" : "rekendatum.isBefore(") + g.text() + ")");
            }
            rekendatumNodig = true;
            return LoweredConditie.van(sj.toString());
        }
        if (c instanceof Conditie.Vergelijking va) {
            // §5.8/§13.4.14 a COUNT aggregation as one side of the comparison ("het aantal X van Y <op> N") — lower it to
            // a result-constrained accumulate CE hoisted after the subject pattern, not an inline scalar constraint (which
            // markers, since a count is not one getter). Checked BEFORE the scalar operand paths below.
            LoweredConditie agg = aggregatieVoorwaarde(acc, va, subject);
            if (agg != null) return agg;
        }
        if (c instanceof Conditie.Vergelijking v) {
            // An operand the resolver/transpiler could not lower (an unresolvable navigation, an
            // under-specified dimensioned read) makes the whole comparison unfaithful — and emitting it raw
            // would be uncompilable DRL (a Map-vs-number term sinks the package). Make the guard an honest
            // always-false marker instead. (Checked BEFORE the unit restatement, which a marker operand must
            // never reach — its .multiply would wrap the placeholder.)
            Frag lo = operand(acc, v.links(), subject), ro = operand(acc, v.rechts(), subject);
            if (lo.marker() || ro.marker()) {
                // The label is the manifest reason a reader sees. A bare "voorwaarde" cannot distinguish a
                // construct this back-end does not carry from an UNREADABLE date or datum-tijd lexeme (IR-SPEC ch. 4.1) —
                // a document defect the producer can fix. Name those, in the exec branch's words.
                // The same holds for a window under GEEN or a counting quantifier in an operand's aggregation
                // filter: WF-112's position, which the exec branch names in these words.
                boolean lexeem = Literalen.onleesbaarDatumTijdLexeem(v.links()) || Literalen.onleesbaarDatumTijdLexeem(v.rechts());
                boolean datumLexeem = Literalen.onleesbaarDatumLexeem(v.links()) || Literalen.onleesbaarDatumLexeem(v.rechts());
                String venster = Vensterpositie.inExpressie(v.links());
                if (venster == null) venster = Vensterpositie.inExpressie(v.rechts());
                return LoweredConditie.marker(lexeem ? "voorwaarde met onleesbaar datum-tijd-lexeem"
                        : datumLexeem ? "voorwaarde met onleesbaar datum-lexeem"
                        : venster != null ? venster : "voorwaarde", v.bron());
            }
            // §5.5.5 a comparison with a TO-ONE navigated operand: mvel's `!.` (from operand()) short-circuits the whole
            // constraint to a non-match on a null relation — spec-correct for ordering/GELIJK (a leeg operand is onwaar)
            // but WRONG for ONGELIJK (Tabel 44: a leeg-vs-gevuld "ongelijk aan" is WAAR). Read the comparison leegwaarde-
            // faithfully via explicit leeg/vol/waarde guards (the complement of the consistentie schending form) instead.
            String navVw = genavigeerdeVergelijkingVoorwaarde(acc, v, subject);
            if (navVw != null) return LoweredConditie.van(navVw);
            // §5.1 Tabel 32: a comparison across convertible-but-different units restates
            // the right operand in the left's unit first ("24 mnd > 2 jr", not "24 > 2").
            String[] o = vergelijkingsOperanden(lo.text(), effType(v.links()), ro.text(), effType(v.rechts()));
            // §389/§486: when a numeric operand is arithmetic that can carry an exact Breuk (a non-terminating
            // division/percentage or a rational literal), an inline mvel "l OP r" would throw on a Breuk (mvel's
            // == casts to BigDecimal) — so compare EXACTLY via the Breuk-aware rsAggVergelijk (single-runtime; it
            // also handles the numeric leegwaarde tables 37/44/33). Only for a numeric comparison with a Breuk-capable
            // operand; the ordinary field/literal comparison keeps its inline form (no churn).
            boolean numeriek = (eff.typeVan(v.links()) != null && eff.typeVan(v.links()).isGetalachtig())
                    || (eff.typeVan(v.rechts()) != null && eff.typeVan(v.rechts()).isGetalachtig());
            if (numeriek && (bevatBreukBron(v.links()) || bevatBreukBron(v.rechts()))) {
                fnImport("rsAggVergelijk");
                return LoweredConditie.van("rsAggVergelijk(" + o[0] + ", " + o[1] + ", \"" + Operatoren.symbool(v.op()) + "\")");
            }
            String cmp = o[0] + " " + Operatoren.symbool(v.op()) + " " + o[1];
            // §5.3 / typeringen Tabel 38: a NON-numeriek "gelijk aan" with BOTH operands empty is "fout"
            // (a foutmelding), NOT "waar" — so as a rule GUARD the condition is not satisfied and the rule
            // must NOT fire. Plain mvel reads "null == null" as true (it would fire), so exclude the
            // both-empty case (p40). A numeriek both-empty stays "waar"/fires per Tabel 37 (the §5.3 prose
            // says "onwaar" there — a spec contradiction left to the existing Tabel-37 reading).
            if (alsVoorwaarde && v.op() == Conditie.Operator.GELIJK && nietNumeriekVergelijking(v)
                    && !(v.links() instanceof Expr.Literal) && !(v.rechts() instanceof Expr.Literal))
                return LoweredConditie.van("(" + cmp + " && !(" + o[0] + " == null && " + o[1] + " == null))");
            return LoweredConditie.van(cmp);
        }
        if (c instanceof Conditie.Predicaat p) {
            // A leeg/gevuld predicate over a TO-ONE navigated operand cannot use the null-safe "!." form: "!."
            // short-circuits the whole term to a NON-match on a null reference, which is the WRONG answer for
            // `is leeg` (typeringen Tabel 54: an absent value IS leeg -> waar). Guard the reference explicitly
            // so an absent reference reads as EMPTY — leeg = (rol == null || rol.attr == null); gevuld = (rol
            // != null && rol.attr != null) — the "." after the short-circuit guard never dereferences null.
            String navLeeg = genavigeerdLeegGevuld(p, subject);
            if (navLeeg != null) return LoweredConditie.van(navLeeg);
            Frag o = operand(acc, p.operand(), subject);
            if (o.marker()) return LoweredConditie.marker("voorwaarde", p.bron());  // e.g. an under-specified dimensioned read
            return switch (p.soort()) {
                case LEEG   -> LoweredConditie.van(o.text() + " == null");
                case GEVULD -> LoweredConditie.van(o.text() + " != null");
                case ELFPROEF -> { fnImport("rsElfproef"); yield LoweredConditie.van((p.negatie() ? "!" : "") + "rsElfproef(" + o.text() + ")"); }  // §8.1.3 (Tabel 16 NIET-vorm)
                case NUMERIEK_MET_EXACT -> { fnImport("rsNumeriekMetExact");   // §8.1.4 (Tabel 16 NIET-vorm)
                    yield LoweredConditie.van((p.negatie() ? "!" : "") + "rsNumeriekMetExact(" + o.text() + ", " + p.cijfers() + ")"); }
                case DAGSOORT -> {  // §8.1.5 "<datum> is (geen) <dagsoort>" -> the §9.8 definitie predicate (a global)
                    String glob = dagsoortGlobaal(p.dagsoort());
                    // §5.15: een leegwaarde is nooit een dagsoort -> a null date is ONWAAR (no match), not an NPE. The
                    // NEGATED form (Tabel 16 "is geen <dagsoort>") wraps it in "!" -> a leeg/non-dagsoort datum is WAAR.
                    // §8.1.5/§5.15/Tabel 57: a Datum-en-tijd (LocalDateTime) operand is day-granular here — take its
                    // .toLocalDate() for the §9.8 LocalDate predicate (after the != null guard, so no NPE; the && short-circuits).
                    String dag = eff.typeVan(p.operand()) instanceof Datatype.DatumTijd ? o.text() + ".toLocalDate()" : o.text();
                    yield glob == null ? LoweredConditie.marker("dagsoortcontrole '" + p.dagsoort() + "'", p.bron())
                            : LoweredConditie.van((p.negatie() ? "!" : "") + "(" + o.text() + " != null && " + glob + ".test(" + dag + "))");
                }
                default     -> LoweredConditie.marker("predicaat " + p.soort(), p.bron());
            };
        }
        // §8.1.8 kenmerk test: a boolean property constraint inside the subject pattern. The property form
        // (not getX()) resolves to the boolean isX() getter the fact declares. A navigated kenmerk reads
        // the property through the to-one role's reference field, guarded against a null reference (mvel is
        // not null-safe, so a missing partner must read as "not gepensioneerd", not throw): the constraint
        // becomes "partner != null && partner.gepensioneerd == true".
        if (c instanceof Conditie.Kenmerk k) {
            // §13.4.7 feitcreatie hosts a doel-kenmerk on the $doel pattern: a kenmerk tagged with an
            // eigenaar emits ONLY on that type's pattern (a marker elsewhere), so it never lands on the
            // anker — even when the anker happens to declare a same-named kenmerk. An untagged kenmerk
            // (the single-subject rule) is unconstrained and emits on whatever pattern hosts the voorwaarde.
            // WF-109: the host is satisfied EXTENSIONALLY (subject ⊑ host), not by name identity — a
            // supertype host is this pattern's too. Shared with the exec branch so the two cannot drift.
            if (!Gastheer.hostOp(k, subject))
                return LoweredConditie.marker("kenmerk '" + k.kenmerk() + "' hoort bij " + k.eigenaar().naam, c.bron());
            // §13.3.6/§8.2: a tijdsafhankelijk kenmerk is a List<Segment> boolean timeline — a bare "<k> == true"
            // membership check (or the §8.2 "gedurende het gehele jaar <k> is/heeft") would compare the List to a
            // boolean and sink the build; marker it. The gedurende CLIP path reads the same kenmerk as a timeline
            // via temporeelKenmerkTijdreeks (not this scalar leaf), so the clip is unaffected. Covers an own-subject
            // kenmerk AND a to-one navigated one (its owner is resolved from the reference role's far type).
            KenmerkHop hop = kenmerkHop(k, subject);
            if (genavigeerd(k) && hop == null)
                return LoweredConditie.marker("navigatiebinding van kenmerk '" + k.kenmerk() + "' past niet op "
                        + (subject == null ? "het onderwerp" : subject.naam), c.bron());
            Objecttype kHouder = hop == null ? subject : hop.doel();
            if (kHouder != null && bevatNaam(kHouder.kenmerkGranulariteitLeden(), k.kenmerk()))
                return LoweredConditie.marker("tijdsafhankelijk kenmerk in scalaire voorwaarde (§8.2)", c.bron());
            String prop = ident(k.kenmerk(), false) + " == " + (k.negatie() ? "false" : "true");
            if (hop == null) return LoweredConditie.van(prop);
            String nav = ident(hop.rol(), false);
            return LoweredConditie.van(nav + " != null && " + nav + "." + prop);
        }
        // §8.1.7 / IR-SPEC ch. 4.5 the role check: a link of the fact type exists in which the subject plays the role
        // (the loader's carrier and its binding — IrLoader.Loaded#rolBindingen), and where the check narrows the other
        // end, its filler belongs to that type. Where the link is STORED decides the lowering
        // (FactModelGenerator.lidmaatschapsopslag). A field on the SUBJECT, naming the other end, is a null-test inside
        // the subject's own pattern ("is geen": the inverse). A field on the other end, naming the subject, is an
        // EXISTS-join — a separate when-clause that cannot live inside the subject pattern, so it rides out as an extra
        // clause (empty inline): regel() appends it after the subject pattern, a pure-conjunction compound carries it
        // up, and a caller without a place to host it (conditieInline) degrades it to a marker. Either field must be
        // the REFERENCE: a declared member that took the role's name is non-null whenever it holds a value, so a
        // null-test would read that VALUE as proof of membership — measured 2026-09-10, both branches identically
        // ("FAIL verwacht leeg, werkelijk 8"); a join on a field no bean carries sank the whole package on a bare
        // "Unable to Analyse Expression" (measured 2026-09-11). Same shared question the exec branch asks.
        if (c instanceof Conditie.Rollidmaatschap r) {
            FactModelGenerator.Lidmaatschapsopslag o = FactModelGenerator.lidmaatschapsopslag(model, rolBindingen.get(r));
            if (o.weigering() != null) return LoweredConditie.marker(o.weigering(), c.bron());
            if (subject == null) return LoweredConditie.marker("rollidmaatschap '" + r.rol() + "'", c.bron());
            Objecttype tegenpartij = io.github.yhavinga.regelspraak.loader.IrLoader.tegenpartij(r);
            if (o.opSubject()) {
                if (!navVeld("rollidmaatschapscheck", subject, o.beanveld(), o.ander()))
                    return LoweredConditie.marker("rollidmaatschap '" + r.rol() + "'", c.bron());
                String veld = ident(o.beanveld(), false);
                if (tegenpartij == null) return LoweredConditie.van(veld + (r.negatie() ? " == null" : " != null"));
                String test = veld + " instanceof " + ident(tegenpartij.naam, true);   // null is no instance
                return LoweredConditie.van(r.negatie() ? "!(" + test + ")" : test);
            }
            Objecttype vuller = tegenpartij != null ? tegenpartij : o.ander();
            if (!navVeld("verre rollidmaatschapscheck", vuller, o.beanveld(), subject))
                return LoweredConditie.marker("§8.1.7 verre rolcheck zonder beanveld", c.bron());
            return new LoweredConditie("", List.of(verreRolClausule(vuller, o.beanveld(), r.negatie())), false);
        }
        // §8.1.9 / §13.4.15.66 regelpredicaat "regelversie <X> is gevuurd|inconsistent". "is inconsistent"
        // reads the §9.5 Inconsistentie channel: an exists-join on the flag fact the named consistentie/
        // uniciteit/verdeling rule inserts (hoisted like the far-side rolcheck below — it cannot live inside
        // the subject pattern). "is gevuurd" has NO observable runtime record (rules write values, they do
        // not log firing) and a name that is not a known Inconsistentie producer would be a never-matching
        // exists (a silent never-fire) — both stay an honest marker, symmetric with the exec skip.
        if (c instanceof Conditie.Regelversie rv) {
            if (rv.inconsistent() && model.inconsistentieProducent(rv.regel()))
                return new LoweredConditie("", List.of("exists " + FACT_PACKAGE + ".Inconsistentie( regel == \""
                        + escapeRegelNaam(rv.regel()) + "\" )"), false);
            // §8.1.9 "is gevuurd" over an instrumented main-regel() producer: an exists on the RuleFired marker
            // that producer's consequence inserts (hoisted like the Inconsistentie exists — it has no subject of
            // its own, so it cannot live inside the subject pattern). Drools re-activates this reader when the
            // producer fires later in the same session (§8.1.9 fixpoint: the insert re-triggers the exists).
            if (!rv.inconsistent() && model.gevuurdProducent(rv.regel())) {
                ruleFiredNodig = true;
                return new LoweredConditie("", List.of(ruleFiredGuard(rv.regel())), false);
            }
            // Un-instrumented target (a consistentie/uniciteit rule whose "result" is the Inconsistentie channel,
            // an objectcreatie/verdeling, or an unknown name) stays an honest marker, symmetric with the exec skip
            // — never a never-matching exists (a silent never-fire).
            return LoweredConditie.marker("regelpredicaat (§8.1.9): " + (rv.inconsistent()
                    ? "onbekende regelversie '" + rv.regel() + "'"
                    : "'is gevuurd' over niet-geïnstrumenteerde regelversie '" + rv.regel() + "'"), c.bron());
        }
        if (c instanceof Conditie.Samengesteld s) {
            // §8.4.3 a window under GEEN or a counting quantifier (ten minste/ten hoogste/precies), at any depth:
            // WF-112, since 2026-09-23 — the quantifier would complement or count the window's verdict and no
            // reading of that is specified. Refused by name, in the sentence both branches share
            // (kern.Vensterpositie), because not every document goes through the verifier. Asked of EVERY
            // composite, the conjunctions included: an ALLE or DE composite puts its own generic marker over a
            // member that refused, so a WF-112 position nested in one (a GEEN fold or composite member, a GEEN
            // composite in an aggregation filter) lost its name here — measured 2026-09-23, where the exec
            // branch named it. A conjunction without such a position lowers normally below (the spec's own
            // §8.4.3 example).
            String vensterWeigering = Vensterpositie.weigering(s);
            if (vensterWeigering != null) return LoweredConditie.marker(vensterWeigering, s.bron());
            // §13.4.14.4 a SINGLE aggregation bullet under GEEN / "ten minste 1": the quantifier reduces to a plain
            // boolean transform of the one leaf (GEEN = NOT the leaf; ten minste 1 = the leaf), so route it through the
            // aggregation lowering with a boolean-NEGATION flag for GEEN. Negating the rsAggVergelijk RESULT (not the
            // comparison operator) is leegwaarde-correct: NOT(SOM(leeg) > 10)=WAAR, whereas SOM(leeg) <= 10 would be
            // onwaar (§5 empty-set ordering). The aggregation contributes an accumulate CE (a separate when-clause) +
            // an eval, which the counting fold's inline-boolean expansion below CANNOT host — so this is the only way
            // an aggregation bullet fires under a counting quantifier. >1 bullet / ten hoogste / precies stay markers.
            if ((s.kwantor() == Conditie.Kwantor.GEEN
                 || (s.kwantor() == Conditie.Kwantor.TEN_MINSTE && s.drempel() == 1))
                    && s.onderdelen().size() == 1
                    && s.onderdelen().get(0) instanceof Conditie.Vergelijking av && isAggregatieVergelijking(av)) {
                LoweredConditie agg = aggregatieVoorwaarde(acc, av, subject, s.kwantor() == Conditie.Kwantor.GEEN);
                if (agg != null) return agg;   // null -> unlowerable RHS -> fall through to the general marker below
                return LoweredConditie.marker("aggregatie-bullet onder tellende kwantor", s.bron());
            }
            // Any OTHER aggregation bullet under a counting (non-conjunction) quantifier — a MULTI-bullet compound, or a
            // ten hoogste / precies — cannot be folded into the inline-boolean expansion (an aggregation is an accumulate
            // CE + eval, not a pattern constraint), so it MARKERS symmetrically (the exec's fact-only fold skips it too).
            // Checked BEFORE recursing so no orphan accumulate leaks into a broken "!(())" fold.
            if (s.kwantor() != Conditie.Kwantor.ALLE && s.kwantor() != Conditie.Kwantor.DE
                    && s.onderdelen().stream().anyMatch(d -> d instanceof Conditie.Vergelijking cv && isAggregatieVergelijking(cv)))
                return LoweredConditie.marker("aggregatie-bullet onder tellende kwantor (>1 bullet / ten hoogste / precies)", s.bron());
            // Only a PURE CONJUNCTION can host a child's separate when-clause (see the hoist note below), and the
            // children have to know that BEFORE they lower — a child that writes into the Acc and only then hears
            // it cannot take the clause back without every caller in the chain knowing to. So the flag goes down
            // with the recursion, and the mark below stays as the net for a channel nobody read it on.
            final int hijsMark = acc.clausules.size();
            boolean conjunctie = s.kwantor() == Conditie.Kwantor.ALLE || s.kwantor() == Conditie.Kwantor.DE;
            boolean vorigVerbod = acc.hijsVerbod;
            String vorigeHijsReden = acc.hijsReden;
            if (!conjunctie) acc.hijsVerbod = true;
            acc.hijsReden = null;
            List<LoweredConditie> kinderen = new ArrayList<>();
            String hijsReden;
            try {
                for (Conditie deel : s.onderdelen()) kinderen.add(conditie(acc, deel, subject, alsVoorwaarde));
            } finally {
                acc.hijsVerbod = vorigVerbod;
                hijsReden = acc.hijsReden;
                acc.hijsReden = vorigeHijsReden;
            }
            if (kinderen.isEmpty()) return LoweredConditie.van("true");
            // Only a PURE CONJUNCTION (§13.4.14 alle/de) is a plain && of its parts, so a hoisted exists/not
            // clause — implicitly AND-ed as a separate when-clause — is faithful there. The other quantifiers
            // fold the parts with || / ! (geen, ten hoogste, precies, and ten minste via `minstens`), where a
            // separate clause would instead ASSERT the membership unconditionally — a silently-wrong gate. So a
            // child that wants to hoist (`heeftExtra`) under any NON-conjunction quantifier no-ops the whole
            // compound, exactly as a marker child does. Keyed on alle/de EXPLICITLY (never the default arm) so a
            // future quantifier cannot silently inherit the hoist.
            // An unresolved sub-condition cannot be faithfully combined. Splicing its always-false marker into
            // one operand is wrong under the NEGATED quantifiers (geen/ten hoogste/precies, which wrap the
            // operands in !(…)): there the dropped criterion flips into a positive firing decision. So mark the
            // WHOLE compound a no-op (rule never fires) rather than risk a silently-wrong gate — for every quantifier.
            boolean wilHoisten = kinderen.stream().anyMatch(LoweredConditie::heeftExtra);
            // The SECOND hoist channel, as a net rather than as the mechanism: a child that pushed straight into
            // the Acc leaves an EMPTY inline and no marker, so neither test above sees it, and the fold below
            // then wrote "(())" — a parse error instead of a named refusal (measured 2026-09-16 on a nested fold,
            // again 2026-09-18 on a §8.2 fold under "ten minste 1"). The flag above is what keeps a child from
            // getting here; this is what happens if a future node does not read it.
            boolean hijstInAcc = acc.clausules.size() != hijsMark;
            if (kinderen.stream().anyMatch(LoweredConditie::marker) || ((wilHoisten || hijstInAcc) && !conjunctie)) {
                aggTerug(acc, hijsMark);   // a markered compound keeps no orphan clause (the kvTerug discipline)
                return LoweredConditie.marker(
                        hijsReden != null ? hijsReden
                        : hijstInAcc && !conjunctie
                            ? "een onderdeel dat zijn clausule BUITEN de inline hijst onder een niet-conjunctieve"
                              + " kwantor: een aparte when-clausule beweert onvoorwaardelijk wat de vouw weegt"
                        : "voorwaarde", s.bron());
            }
            if (conjunctie) {
                // Join ONLY the non-empty inlines (a pure far-side child has an empty inline — dropping it
                // avoids a dangling "()" / " && "); concat every child's hoisted clauses. When every child is a
                // pure far-side membership the combined inline is "" — the subject pattern stays "(  )" and the
                // exists clauses carry the whole gate.
                List<String> inlineDelen = kinderen.stream().map(LoweredConditie::inline)
                        .filter(x -> !x.isEmpty()).map(x -> "(" + x + ")").toList();
                List<String> extra = kinderen.stream().flatMap(k -> k.extra().stream()).toList();
                return new LoweredConditie(String.join(" && ", inlineDelen), extra, false);
            }
            // §13.4.14.4: geen = none holds (!OR); the counting quantifiers express "k of the criteria hold"
            // purely in booleans (a Drools pattern constraint accepts &&/||/! but not arithmetic over ternaries),
            // via the OR of every k-subset (minstens): at-least-N directly, at-most-N as not-at-least-(N+1),
            // exactly-N as the conjunction of the two. No child has extra here (guarded above).
            List<String> delen = kinderen.stream().map(k -> "(" + k.inline() + ")").toList();
            int n = s.drempel();
            return LoweredConditie.van(switch (s.kwantor()) {
                case GEEN        -> "!(" + String.join(" || ", delen) + ")";
                case TEN_MINSTE  -> minstens(delen, n);
                case TEN_HOOGSTE -> "!(" + minstens(delen, n + 1) + ")";
                case PRECIES     -> "(" + minstens(delen, n) + " && !(" + minstens(delen, n + 1) + "))";
                default          -> String.join(" && ", delen); // unreachable (alle/de handled above), kept total
            });
        }
        // IR-SPEC ch. 4.5 / RUNTIME-SEMANTICS ch. 3.9a the QUANTIFIER FOLD — see kwantorvouw below, which
        // lowers the non-temporal matrix and markers every other shape WITH ITS OWN REASON.
        if (c instanceof Conditie.Kwantorvouw kv) return kwantorvouw(acc, kv, subject);
        return LoweredConditie.marker("voorwaarde", c.bron());
    }

    /** The marker this branch leaves for a quantifier fold it does not lower, carrying the QUANTIFIER, the
     *  element type and — the part that has to be right — the reason THIS fold was declined. One text for
     *  every decline was measured wrong on 2026-09-16: it said "ch. 3.9a wordt alleen in de exec-model-tak
     *  gelowerd" for a shape the exec branch ALSO refuses, and for a range whose reason has nothing to do
     *  with either branch. The reasons are per decline now, and each names the axis it was declined on. */
    private LoweredConditie kvMarker(Conditie.Kwantorvouw kv, String reden) {
        // "§8.2" only where the matrix IS the §8.2 whole-period form: a fold over any other matrix is ch. 3.9a's
        // node without the qualifier, and the frame said §8.2 for it until 2026-09-23.
        return LoweredConditie.marker((geheelPeriodeVan(kv.toets()) != null ? "§8.2 kwantorvouw (" : "kwantorvouw (")
                + kv.kwantor() + " over "
                + (kv.elementObjecttype() == null ? "<onopgelost elementtype>" : kv.elementObjecttype().naam)
                + ") — " + reden, kv.bron());
    }

    /** Roll back any clause a bailed-out fold lowering appended since {@code mark} — the {@link #aggTerug}
     *  discipline, so a markered rule keeps no orphan clause — and marker with {@code reden}. */
    private LoweredConditie kvTerug(Acc acc, int mark, Conditie.Kwantorvouw kv, String reden) {
        aggTerug(acc, mark);   // for the rollback; its own "not lowered" null is replaced by the marker below
        return kvMarker(kv, reden);
    }

    /** RUNTIME-SEMANTICS ch. 3.9a: lower the quantifier fold over a NON-temporal matrix to a single hoisted
     *  clause. {@code ALLE} and {@code GEEN} are a denied failure WITNESS —
     *  {@code not <Element>( <join>, <filter>, <matrix or its negation> )}: GEEN fails on a member that
     *  SATISFIES the matrix, ALLE on one that does not, so the only difference is whether the matrix
     *  constraint is negated. The three COUNTING quantifiers need the SIZE of the satisfier set, which no
     *  witness yields, so they take the accumulate form this branch already uses for an aggregation —
     *  {@code Number( intValue <op> N ) from accumulate( $e : <Element>( <join>, <filter>, <matrix> ),
     *  count($e) )} — over exactly the same element pattern. A count accumulate always produces a row, 0 over
     *  an empty range, so ch. 3.9a's "the empty set is the count" holds without an arm for it.
     *
     *  <p>Three things follow from the shape rather than being arranged for. An EMPTY range denies nothing and
     *  the clause holds, which is ch. 3.9a's vacuous rule under BOTH quantifiers. The matrix is lowered with
     *  the ELEMENT as subject through the ordinary {@link #conditieInline} — the same per-subject translation
     *  every other condition of this branch gets, which is exactly what ch. 3.9a says {@code toets} is — so the
     *  NIET prefix rides INSIDE it ({@code Kenmerk.negatie}, or the comparison's own operator) and the
     *  quantifier is untouched. And the {@code filter} is the die/dat subselection on the same pattern, exactly
     *  as it is on an aggregation's accumulate source.
     *
     *  <p><b>The WHOLE-PERIOD matrix takes the other road</b>, {@link #heelperiodeVouw}: a witness pattern
     *  cannot ask ch. 3.9's per-bucket question of a {@code List<Segment>} member, so that arm collects the
     *  contributors' timelines and folds them through the shared ch. 3.9 runtime helpers. The join and the
     *  filter built here are its accumulate SOURCE, which is why it is entered from below rather than beside.
     *
     *  <p><b>A matrix or filter that HOISTS is refused, and not only one that markers.</b> Both are measured:
     *  {@link #conditieInline} degrades a condition carrying {@code extra} to a marker Frag, but a condition
     *  that pushes straight into the {@link Acc} — which is what THIS method does, so a NESTED fold is the
     *  first one that can — returns an EMPTY inline and no marker at all. Taken at face value that emitted an
     *  orphan clause joined against {@code $subject} of the wrong type plus an empty {@code !(  )} constraint,
     *  and the package died on a parser error instead of refusing by name (measured 2026-09-16). So the guard
     *  is BOTH channels: a marker, or any clause appended since {@code mark}. */
    private LoweredConditie kwantorvouw(Acc acc, Conditie.Kwantorvouw kv, Objecttype subject) {
        // WF-112 before anything else. This arm writes a window in the filter or the matrix into the element
        // pattern whatever the quantifier, so under GEEN or a counting one it answered the counted or
        // complemented verdict no reading specifies — measured 2026-09-23 under all four, and under GEEN in a
        // time-free session the rule fired for every subject. The verifier refuses such a document; this branch
        // refuses it too, because not every document goes through the verifier.
        // The sentence names the position that is ill-formed — this fold, or a composite or fold nested in its
        // filter or toets, each with its own quantifier and element type — so it is not framed as THIS fold's.
        String vensterWeigering = Vensterpositie.weigering(kv);
        if (vensterWeigering != null) return LoweredConditie.marker(vensterWeigering, kv.bron());
        // Both routes below answer with a SEPARATE when-clause (a `not` witness pattern, a count accumulate, the
        // §8.2 collectList+eval) and an EMPTY inline — which is faithful only where the compound AND-s its parts.
        // Under a fold that ORs or negates them the clause would assert unconditionally what the fold means to
        // weigh, so the position is refused before anything is written, by name. Measured: without this the node
        // emitted its clause anyway and the package died on "[ERR 101] … no viable alternative at input ')'" — a
        // parse error where ch. 3.12 promises a named refusal. The exec branch refuses the same document on its
        // own axis ("voorwaarde buiten Phase-B scope"), so the two stay symmetric.
        if (acc.hijsVerbod) {
            String staart = "onder een niet-conjunctieve kwantor: haar clausule is een eigen when-clausule en"
                    + " geen patroonconstraint, dus zij kan niet in een ||/!-vouw staan";
            // The compound replaces a markered child's text with its own, so the axis is handed up explicitly.
            acc.hijsReden = "kwantorvouw (" + kv.kwantor() + " over "
                    + (kv.elementObjecttype() == null ? "<onopgelost elementtype>" : kv.elementObjecttype().naam)
                    + ") " + staart;
            return kvMarker(kv, "een vouw " + staart);
        }
        if (subject == null) return kvMarker(kv, "geen subjectpatroon om het bereik aan te hangen");
        if (kv.toets() == null) return kvMarker(kv, "een kwantorvouw zonder matrix (WF-108 (d))");
        if (kv.kwantor() == Conditie.Kwantor.DE)
            return kvMarker(kv, "kwantor DE (WF-108 (a)): de bepaalde lidwoordvorm noemt ÉÉN lid en is geen "
                    + "kwantificatie over een verzameling");
        if (!kv.bronPad().isEmpty())
            return kvMarker(kv, "een meerstaps bereik (WF-108 (c)); het elementpatroon draagt één joinconstraint");
        Objecttype elem = kv.elementObjecttype();
        if (elem == null) return kvMarker(kv, "onopgelost elementObjecttype");
        // WF-111: this branch writes a window in the filter and the toets, so a window it declines there is
        // declined for its BOUND, and the refusal says so rather than naming the node class (it said "een
        // matrixvorm die deze tak niet IN het elementpatroon kan schrijven (Periode)" until 2026-09-23).
        if (Vensterpositie.ondraagbareGrens(kv.filter(), elem, a -> !ongenavigeerd(a))
                || Vensterpositie.ondraagbareGrens(kv.toets(), elem, a -> !ongenavigeerd(a)))
            return kvMarker(kv, Vensterpositie.GRENSVORM);
        // §13.3.9 STORAGE DIRECTION, the question scope() asks for an aggregation join: the role the SUBJECT
        // plays is read as a FIELD ON THE ELEMENT, and a resolved role is not yet a bean field.
        // IR-SPEC ch. 4.5: the range IS the join, read from the one binding as the instance fold reads it
        FactModelGenerator.Bereikjoin bj = FactModelGenerator.bereikjoin(model, kv.bronRol());
        String rol = bj.rol();
        if (rol == null)
            return kvMarker(kv, bj.weigering() != null ? bj.weigering()
                    : "geen verbindende rol tussen " + subject.naam + " en " + elem.naam);
        if (!navVeld("kwantorvouw-bereik", elem, rol, subject))
            return kvMarker(kv, "de bereik-join heeft geen beanveld — " + navReden);
        final int mark = acc.clausules.size();
        StringJoiner delen = new StringJoiner(", ");
        delen.add(ident(rol, false) + " == " + SUBJECT);
        if (kv.filter() != null) {
            Frag ff = conditieInline(acc, kv.filter(), elem, false);
            if (ff.marker() || acc.clausules.size() != mark)
                return kvTerug(acc, mark, kv, "een die/dat-subselectie die deze tak niet IN het elementpatroon"
                        + " kan schrijven (" + kv.filter().getClass().getSimpleName() + ")");
            delen.add(ff.text());
        }
        Conditie.GeheelPeriode gp = geheelPeriodeVan(kv.toets());
        if (gp != null) return heelperiodeVouw(acc, kv, gp, elem, subject, delen, mark);
        Frag mf = conditieInline(acc, kv.toets(), elem, true);
        if (mf.marker() || acc.clausules.size() != mark)
            return kvTerug(acc, mark, kv, "een matrixvorm die deze tak niet IN het elementpatroon kan schrijven ("
                    + kv.toets().getClass().getSimpleName() + ")");
        String vergelijking = switch (kv.kwantor()) {
            case TEN_MINSTE  -> ">= " + kv.drempel();
            case TEN_HOOGSTE -> "<= " + kv.drempel();
            case PRECIES     -> "== " + kv.drempel();
            default          -> null;   // ALLE / GEEN: the witness route below
        };
        if (vergelijking != null) {
            delen.add(mf.text());
            String ev = "$kv" + (++acc.n);   // the Acc's own element counter, shared with the aggregations
            acc.clausules.add("Number( intValue " + vergelijking + " ) from accumulate( " + ev + " : "
                    + ident(elem.naam, true) + "( " + delen + " ), count(" + ev + ") )");
            return new LoweredConditie("", java.util.List.of(), false);
        }
        delen.add(kv.kwantor() == Conditie.Kwantor.ALLE ? "!( " + mf.text() + " )" : mf.text());
        acc.clausules.add("not " + ident(elem.naam, true) + "( " + delen + " )");
        return new LoweredConditie("", java.util.List.of(), false);
    }

    /** Is {@code lid} a SINGLE-PERIOD tijdlijn member of {@code elem}? The matrix of a fold is evaluated per
     *  contributor with the ELEMENT as subject (ch. 3.9a), so the member is looked up on the element type
     *  chain-closed (a contributor may be a subtype that inherits the timeline) rather than through
     *  {@link #isTemporeelAttribuut}, which asks the question of the rule's own subject. Multi-period
     *  (§13.3.6.7) is excluded, as everywhere under §8.2. */
    private static boolean elementTijdlijnLid(Objecttype elem, String lid) {
        return bevatNaam(elem.granulariteitLeden(), lid)
                && !Tijdlijnen.meerPeriodiek(elem.tijdlijnAantalLeden(), lid);
    }

    /**
     * RUNTIME-SEMANTICS ch. 3.9a with a §8.2 WHOLE-PERIOD matrix: the fold's other road, entered from
     * {@link #kwantorvouw} once the join and the die/dat filter are built (they become this arm's accumulate
     * SOURCE, unchanged — one definition of "which members", not two).
     *
     * <p><b>Why not the denied witness.</b> ch. 3.9's question is per BUCKET over a whole
     * {@code List<Segment>} member, and a Drools pattern constraint has no way to ask it of the element it
     * ranges over without calling the helper on the element's own timeline — which is a List in a scalar
     * constraint, the shape this branch refuses everywhere else. So the contributors' TIMELINES are collected
     * ({@code collectList}) and the fold happens in one eval over the collected
     * {@code List<List<Segment>>} — the same shape the production toolchain lowers this sentence to.
     *
     * <p><b>One table, three readings of it.</b> The per-contributor verdict is
     * {@link io.github.yhavinga.regelspraak.runtime.RegelSpraakRuntime#rsGeheelPeriode}, reached through the
     * three shared folds over it: {@code rsGeheelPeriodeAlle} (no contributor fails), {@code rsGeheelPeriodeGeen}
     * (no contributor satisfies) and {@code rsGeheelPeriodeTelling} (how many do). No heelperiode logic is
     * restated as MVEL here — the emitted text carries a call and a comparison, nothing else, so this branch
     * and the exec branch cannot drift on the matrix itself.
     *
     * <p><b>Where the NIET prefix lands</b>, exactly as ch. 3.9a states it and as the exec twin does it: under
     * ALLE/GEEN it FLIPS the quantifier (∀c.¬P = ¬∃c.P, ¬∃c.¬P = ∀c.P), and under a threshold there is no
     * quantifier to flip into, so it lands on the COUNT — {@code count(¬P) = |contributors| − count(P)}, an
     * equality because the whole-period predicate is total. The contributor count is the collected list's own
     * size, which is why the members with NO timeline at all matter: {@code collectList} keeps their null, so
     * they are counted and — per ch. 3.9 — they satisfy the matrix vacuously.
     *
     * <p><b>The grens must be $subject-free</b>, a literal or a scalar Parameter-global. The eval rides AFTER
     * the accumulate and the exec twin's eval binds no subject there at all; refusing an own/navigated bound on
     * both sides keeps the two branches' carried set the same, which is what makes a gate-12 refusal readable.
     * Every other decline below names the axis it was declined on and rolls back through {@link #kvTerug}.
     */
    private LoweredConditie heelperiodeVouw(Acc acc, Conditie.Kwantorvouw kv, Conditie.GeheelPeriode gp,
            Objecttype elem, Objecttype subject, StringJoiner delen, int mark) {
        String gran = gp.jaar() ? "JAAR" : "MAAND";
        String tlVeld; int toetsCode; String grens = "null";
        if (kv.toets() instanceof Conditie.Vergelijking v) {
            Expr.AttribuutRef tijd; Expr ander; boolean linksTemp;
            if (v.links() instanceof Expr.AttribuutRef la && ongenavigeerd(la) && elementTijdlijnLid(elem, la.attribuut()))
                { tijd = la; ander = v.rechts(); linksTemp = true; }
            else if (v.rechts() instanceof Expr.AttribuutRef ra && ongenavigeerd(ra) && elementTijdlijnLid(elem, ra.attribuut()))
                { tijd = ra; ander = v.links(); linksTemp = false; }
            else return kvTerug(acc, mark, kv,
                    "een matrixvergelijking zonder enkelperiodiek tijdlijnattribuut van het elementtype");
            if (!(eff.typeVan(tijd).isNumeriek() || eff.typeVan(tijd).isPercentage()))
                return kvTerug(acc, mark, kv, "een niet-numerieke heelperiodevergelijking in de matrix");
            if (ander instanceof Expr.AttribuutRef ar && ongenavigeerd(ar) && elementTijdlijnLid(elem, ar.attribuut()))
                return kvTerug(acc, mark, kv, "een tijdlijn-vs-tijdlijnvergelijking in de matrix");
            io.github.yhavinga.regelspraak.model.Eenheid el = eff.eenheidVan(tijd), er = eff.eenheidVan(ander);
            boolean crossUnit = el != null && er != null && el.vereistOmrekening(er);
            if (crossUnit && er.exacteHerschaalFactoren(el) == null)
                return kvTerug(acc, mark, kv, "een matrixgrens in een eenheid zonder exacte herschaalfactoren");
            // put the temporal operand on the left: REVERSE the operator (operand swap), never negate it —
            // ch. 3.9's rule, because on a leeg day BOTH orderings are false and a negation would make it satisfy.
            Integer code = Tijdrekening.toets(linksTemp ? v.op() : Operatoren.omkeren(v.op()));
            if (code == null) return kvTerug(acc, mark, kv, "een datumvergelijking als heelperiodematrix");
            if (!(ander instanceof Expr.Literal)
                    && !(ander instanceof Expr.ParameterRef pr && pr.parameter().granulariteit() == null))
                return kvTerug(acc, mark, kv,
                        "een niet-scalaire matrixgrens (de eval na de accumulate bindt geen $subject)");
            Frag g = waarde(acc, ander, subject);
            if (g.marker() || acc.clausules.size() != mark)
                return kvTerug(acc, mark, kv, "een matrixgrens buiten de scalaire scope");
            tlVeld = tijd.attribuut(); toetsCode = code;
            grens = crossUnit ? herschaalExact(g.text(), effType(ander), effType(tijd)) : g.text();
        } else if (kv.toets() instanceof Conditie.Predicaat p && p.operand() instanceof Expr.AttribuutRef pa
                && ongenavigeerd(pa) && elementTijdlijnLid(elem, pa.attribuut())
                && (p.soort() == Conditie.Predicaatsoort.GEVULD || p.soort() == Conditie.Predicaatsoort.LEEG)) {
            tlVeld = pa.attribuut();
            toetsCode = p.soort() == Conditie.Predicaatsoort.GEVULD ? 6 : 8;   // §8.2 gevuld / leeg over every moment
        } else if (kv.toets() instanceof Conditie.Kenmerk k && !genavigeerd(k) && !k.negatie()
                && k.kenmerk() != null && bevatNaam(elem.kenmerkGranulariteitLeden(), k.kenmerk())
                && !Tijdlijnen.meerPeriodiek(elem.kenmerkTijdlijnAantalLeden(), k.kenmerk())) {
            tlVeld = k.kenmerk(); toetsCode = 7;                                // §8.2 the kenmerk's TRUE-period timeline
        } else {
            return kvTerug(acc, mark, kv, "een matrixvorm buiten de §8.2 heelperiodeverzameling (vergelijking /"
                    + " gevuld / leeg / tijdsafhankelijk kenmerk van het element)");
        }

        int n = ++acc.n;                       // the Acc's own element counter, shared with the aggregations
        String ev = "$kv" + n, lijst = "$kvtl" + n;
        acc.clausules.add("accumulate( " + ev + " : " + ident(elem.naam, true) + "( " + delen + " ) ; "
                + lijst + " : collectList( " + ev + "." + getter(tlVeld) + "() ) )");
        String staart = ", \"" + gran + "\", " + toetsCode + ", " + grens + ")";
        boolean telt = kv.kwantor() == Conditie.Kwantor.TEN_MINSTE
                || kv.kwantor() == Conditie.Kwantor.TEN_HOOGSTE || kv.kwantor() == Conditie.Kwantor.PRECIES;
        if (telt) {
            String telling = fnImport("rsGeheelPeriodeTelling") + "(" + lijst + staart;
            // the NIET prefix cannot flip a threshold, so it lands on the count: n − count(P).
            String geteld = gp.niet() ? "(" + lijst + ".size() - " + telling + ")" : telling;
            String op = switch (kv.kwantor()) {
                case TEN_MINSTE  -> ">= ";
                case TEN_HOOGSTE -> "<= ";
                default          -> "== ";   // PRECIES (the three arms above are the only ones reaching here)
            };
            acc.clausules.add("eval( " + geteld + " " + op + kv.drempel() + " )");
            return new LoweredConditie("", java.util.List.of(), false);
        }
        // ALLE/GEEN: the NIET prefix is the MATRIX's, so it flips the quantifier rather than the verdict.
        boolean alle = (kv.kwantor() == Conditie.Kwantor.ALLE) != gp.niet();
        String fold = fnImport(alle ? "rsGeheelPeriodeAlle" : "rsGeheelPeriodeGeen");
        acc.clausules.add("eval( " + fold + "(" + lijst + staart + " == true )");
        return new LoweredConditie("", java.util.List.of(), false);
    }

    /** An unresolved CONDITION lowers to an always-false constraint, so the rule is a visible no-op
     *  that COMPILES — unlike the value-side {@code … null} marker, a bare {@code null} is not a
     *  boolean pattern constraint ({@code Subject( null )} fails to compile, which would sink the
     *  whole DRL). Wrapped in a flagged {@link LoweredConditie#marker} so callers branch on the type. */
    private static String onopgelosteConstraint(String wat, String bron) {
        return "false /* TODO " + commentVeilig(wat + ": " + bron) + " */";
    }

    /** As {@link #onopgelosteConstraint(String, String)} but from a structured {@link Bron} (snippet spliced). */
    private static String onopgelosteConstraint(String wat, Bron bron) {
        return onopgelosteConstraint(wat, bron == null ? null : bron.snippet());
    }

    /** The role check whose link is stored on the other end, as a separate when-clause: the subject is satisfied when
     *  at least one filler of type {@code vuller} references it through {@code veld}
     *  ({@code exists <Vuller>( <veld> == $subject )}); the negated check is the {@code not} of that. Bound AFTER
     *  the subject pattern so {@code $subject} is in scope. */
    private String verreRolClausule(Objecttype vuller, String veld, boolean negatie) {
        String patroon = ident(vuller.naam, true) + "( " + ident(veld, false) + " == " + SUBJECT + " )";
        return (negatie ? "not " : "exists ") + patroon;
    }

    /** A boolean expression true when AT LEAST {@code k} of {@code delen} hold: the OR over
     *  every k-subset, each subset AND-ed. k&lt;=0 is always true; k&gt;size is never. The
     *  bullet lists are small, so the C(size,k) expansion stays modest. */
    // ---------------- §9.8 dagsoort definitie -> a shared Predicate global ----------------

    /** Resolve (idempotently) the §9.8 definitie of {@code dagsoortNaam} via the SHARED {@link Dagsoorten}
     *  compiler (single-runtime principle: ONE predicate implementation for both back-ends) and declare it as a
     *  {@code global java.util.function.Predicate rsDagsoort_<Naam>;} that a session builder supplies from
     *  {@link #dagsoortGlobalen()}. Returns the global name, or null when no such definitie exists or its
     *  condition is not a pure date predicate (the caller then markers) — the same verdict the exec back-end
     *  reaches from the same compiler, so the two manifests stay aligned. */
    private String dagsoortGlobaal(String dagsoortNaam) {
        java.util.function.Predicate<java.time.LocalDate> pred = dagsoorten.predikaat(dagsoortNaam);
        if (pred == null) return null;
        String naam = "rsDagsoort_" + ident(dagsoortNaam, true);
        dagsoortGlobalen.putIfAbsent(naam, pred);
        return naam;
    }


    private static String minstens(List<String> delen, int k) {
        if (k <= 0) return "true";
        if (k > delen.size()) return "false";
        List<String> combos = new ArrayList<>();
        combineer(delen, k, 0, new ArrayList<>(), combos);
        return "(" + String.join(" || ", combos) + ")";
    }
    private static void combineer(List<String> delen, int k, int start, List<String> huidig, List<String> uit) {
        if (huidig.size() == k) { uit.add("(" + String.join(" && ", huidig) + ")"); return; }
        for (int i = start; i <= delen.size() - (k - huidig.size()); i++) {
            huidig.add(delen.get(i));
            combineer(delen, k, i + 1, huidig, uit);
            huidig.remove(huidig.size() - 1);
        }
    }

    /** A condition operand: the subject's own attribute reads as a bare field name; a
     *  related object's attribute reads as a property path ("partner.datum"). */
    /** A leeg/gevuld predicate over a TO-ONE navigated operand, with the navigation root guarded explicitly
     *  (so a null reference reads as an empty value per typeringen Tabel 53/54, not a Drools "!." non-match),
     *  or null when the operand is not such a navigation (own attribute, to-many, unresolved → the caller's
     *  plain form applies). The "." is reached only after the short-circuit root guard, so it never NPEs. */
    private String genavigeerdLeegGevuld(Conditie.Predicaat p, Objecttype subject) {
        if (p.soort() != Conditie.Predicaatsoort.LEEG && p.soort() != Conditie.Predicaatsoort.GEVULD) return null;
        if (!(p.operand() instanceof Expr.AttribuutRef a)) return null;
        NaviRef n = naviResolve(a, subject);
        if (n.soort() != NaviSoort.GENAVIGEERD) return null;   // own/to-many/unresolved → the caller's plain form
        // §13.3.6/§8.2: a navigated TEMPORAL attribute is a List<Segment> — fall through to operand(), which markers it,
        // rather than emit "partner.attr == null" (a list-nullness test that silently drops the §8.2 whole-period leeg check).
        if (isTemporeelAttribuut(a)) return null;
        String wortel = ident(n.rol(), false), pad = wortel + "." + ident(a.attribuut(), false);
        return p.soort() == Conditie.Predicaatsoort.LEEG
                ? "(" + wortel + " == null || " + pad + " == null)"
                : "(" + wortel + " != null && " + pad + " != null)";
    }

    private Frag operand(Acc acc, Expr e, Objecttype subject) {
        if (e instanceof Expr.BinderRef br) {
            // ch. 4.10 on the CONDITION side: a separate rule (a §6.5/§6.6 companion) cannot read the host
            // consequence's local, so the binder stands for the need's OWN re-readable form. A need without one
            // markers, which drops the companion instead of inventing a guard — see takGarde.
            OperandDeel deel = acc.binderDeel.get(letSleutel(br.naam()));
            return deel == null ? Frag.todo("binder zonder herleesbare behoefte in een patroon", br.bron())
                    : Frag.ok(deel.waarde());
        }
        if (e instanceof Expr.DimensieCel c)
            // §3.6 a dimensioned cell on the CONDITION side is deferred to a marker (the value/then side
            // lowers it). A Drools PATTERN constraint cannot host the null-guarded ternary the value form
            // uses (the inline "? :" mis-parses, and a null cell vs a number is a build-breaking type error),
            // and a correct null-safe cell comparison needs a guarded conjunction this single-operand path
            // cannot express. So it markers honestly (-> false voorwaarde): the rule does not fire, never a
            // wrong or uncompilable comparison. Carded.
            return Frag.todo("dimensiecel in voorwaarde", c.bron());
        if (e instanceof Expr.AttribuutRef a) {
            // §3.6: a bare value read of a gedimensioneerd attribuut is under-specified (its property is a
            // Map, so "prijsInJaar > 5" is a Map-vs-number type error that fails the whole DRL build). A
            // legitimate dimensioned read is an Expr.DimensieCel (above); a bare one is a marker.
            if (isGedimensioneerd(a)) return Frag.todo("ongedimensioneerde lezing van gedimensioneerd attribuut (§3.6)", a.bron());
            // §13.3.6/§8.2: a tijdsafhankelijk attribuut is a List<Segment> timeline — a bare when-side comparison
            // operand ("loon == 1000", or the §8.2 "gedurende het gehele jaar gelijk is aan") would leak the List
            // into a scalar constraint and sink the build; marker it (see isTemporeelAttribuut).
            if (isTemporeelAttribuut(a)) return Frag.todo("tijdsafhankelijk attribuut in scalaire voorwaarde (§8.2)", a.bron());
            return attribuutOperand(a, subject);
        }
        if (e instanceof Expr.ParameterRef p)   // §13.3.6 a TEMPORAL parameter is a List<Segment> global — a scalar
            // (condition-side) use is a List-in-scalar mismatch that would sink the whole DRL build; marker instead.
            return p.parameter().granulariteit() != null ? Frag.todo("temporele parameter in scalaire context", p.bron())
                    : Frag.ok(ident(p.parameter().naam(), false));
        if (e instanceof Expr.Literal lit)    return literal(lit);
        if (e instanceof Expr.TekstReeks tr)  return tekstReeks(acc, tr, x -> operand(acc, x, subject));
        if (e instanceof Expr.Afgerond af) {
            // §13.4.17.13/.25: an afronding wraps a getalexpressie — and "de wortel van X" carries a MANDATORY
            // afronding, so a condition-side wortel is always Afgerond(Functie(WORTEL)), never the bare Functie.
            // setScale is a pure method call (no ternary), valid in a pattern constraint, so descend with the
            // operand-side (bare-property) lowering. A marker basis (e.g. an afronding over a Begrensd clamp,
            // which still uses a ternary and markers here) propagates.
            Frag b = operandReken(acc, af.basis(), subject);
            return b.marker() ? b : afgerondeTerm(b, af);
        }
        if (e instanceof Expr.Binair b)
            // §6.2-6.8 arithmetic in a CONDITION: the SAME lowering as the value side (binairTerm), with the
            // sub-operands as bare properties and the +/- unit restatement via herschaalConditie — the EXACT null-safe
            // rsHerschaalExact (×÷ the integer factorNaarCanoniek), so a convertible cross-unit +/- FIRES (only an opaque
            // composite stays a marker). The leegwaarde helpers (rsNul/rsGedeeld/rsVerminderd/rsMacht) are method
            // calls, valid in a constraint.
            return binairTerm(acc, b, x -> operandReken(acc, x, subject), this::herschaalConditie);
        if (e instanceof Expr.Begrensd bg) {
            // §6.1.4 begrenzing in a CONDITION: the value side clamps with an inline ternary "(r < m ? m : r)"
            // that mis-parses in a pattern constraint, so lower through the registered rsBegrens helper (a method
            // call, null-safe, clamping min-then-max sequentially to match the value side's order). The bounds are
            // restated EXACTLY into the basis unit via herschaalConditie (×÷ the integer factorNaarCanoniek, like the
            // +/- and value-side begrenzing paths), so a "min van 24 mnd" clamp on a jr basis compares against an
            // exact 2 jr — it FIRES, not markers (only an OPAQUE composite bound, or a marker basis/bound, propagates).
            fnImport("rsBegrens");
            Datatype bt = effType(bg.basis());
            Frag basis = operandReken(acc, bg.basis(), subject);
            Frag min = bg.minimum() != null ? herschaalConditie(operandReken(acc, bg.minimum(), subject), effType(bg.minimum()), bt) : Frag.ok("null");
            Frag max = bg.maximum() != null ? herschaalConditie(operandReken(acc, bg.maximum(), subject), effType(bg.maximum()), bt) : Frag.ok("null");
            if (basis.marker() || min.marker() || max.marker()) return Frag.todo("begrenzing in voorwaarde", bg.bron());
            return Frag.ok("rsBegrens(" + basis.text() + ", " + min.text() + ", " + max.text() + ")");
        }
        if (e instanceof Expr.Functie f) {
            // §13.4.17 a SCALAR builtin (abs / date-component / datum-met / wortel / paasdag / tijdsduur) is a
            // valid pattern-constraint operand once its arguments lower to bare properties — so reuse the one
            // value-side routine, swapping its arg lowering to operand() (the field name, not a $subject getter,
            // since $subject is not bound inside its own pattern). The AGGREGATIONS (som/min/max over instances)
            // return null there: they need a `when` accumulate and cannot live inside a single constraint, so
            // they fall through to an honest marker (-> the caller's `false /* TODO */`).
            Frag scalar = scalarFunctie(acc, f, x -> operandReken(acc, x, subject));
            if (scalar != null) return scalar;
            return Frag.todo("aggregatie-functie in voorwaarde " + f.kind(), f.bron());
        }
        if (e instanceof Expr.Constante c) {
            Frag rk = rekenConstante(c);   // §5.3 Rekendatum/Rekenjaar as a condition operand
            if (rk != null) return rk;
        }
        return Frag.todo("operand", e.bron());
    }

    /** §5.3 Rekendatum as an expression: the §4.2 rekendatum global (a LocalDate the runtime supplies; an unset
     *  rekendatum reads leeg, the leegwaarde rules then apply). Valid in both a pattern constraint and a
     *  consequence — a global is a free name in either. Rekenjaar is admitted and not executed (kern.Rekenjaar):
     *  it becomes a placeholder, so the construct is a marker wherever it stands, and navReden gives the manifest
     *  the reference interpreter's sentence. null for any other constant ("hij", "de dag": handled by their own
     *  paths). */
    private Frag rekenConstante(Expr.Constante c) {
        if ("Rekendatum".equals(c.naam())) { rekendatumNodig = true; return Frag.ok("rekendatum"); }
        if (Rekenjaar.is(c)) { navReden = Rekenjaar.WEIGERING; return Frag.todo(Rekenjaar.WEIGERING, c.bron()); }
        return null;
    }

    /** The condition-side property access for an attribute — the bare own-property, a null-safe "!." to-one
     *  navigation, or a marker for an absent/naar-veel/unresolvable read. Shared by the plain operand and the
     *  dimensioned-cell read (which needs the Map property before indexing a label), so the latter bypasses
     *  the §3.6 under-specified guard. Drools "!." yields null (a leegwaarde) for a null to-one reference
     *  instead of NPE-ing the mvel "." dereference. */
    private Frag attribuutOperand(Expr.AttribuutRef a, Objecttype subject) {
        NaviRef n = naviResolve(a, subject);
        return switch (n.soort()) {
            case EIGEN       -> Frag.ok(ident(a.attribuut(), false));
            case GENAVIGEERD -> Frag.ok(ident(n.rol(), false) + "!." + ident(a.attribuut(), false));
            case NAAR_VEEL   -> Frag.todo("naar-veel-navigatie", a.bron());
            default          -> Frag.todo("navigatie", a.bron());   // AFWEZIG / ONOPGELOST
        };
    }

    /** The CONDITION-side lowering of an operand sitting INSIDE arithmetic (a binairTerm / begrenzing / afronding /
     *  scalar-functie leg): identical to {@link #operand} EXCEPT a to-ONE NAVIGATED NUMERIC attribute reads through the
     *  null-safe {@link #registreerRelGetalFunctie rsRelGetal} helper instead of mvel's {@code rol!.attr}. Inside a +/-
     *  the §4.1 Tabel 3 leegwaarde reads an absent operand as 0, but mvel's {@code !.} short-circuits the WHOLE
     *  constraint to a non-match on a null relation — dropping the leeg-as-0 (a latent bug: "de prijs van zijn partner plus
     *  1" with no partner must compute 0+1, not silently fail). rsRelGetal yields a leeg (null) value the surrounding
     *  rsNul/rsVerminderd leegwaarde handling then applies, mirroring the exec's navReader. A non-numeric (datum/tekst)
     *  navigated leg, a non-navigated leaf, or a nested expression falls through to {@link #operand} unchanged — leeg-as-0
     *  is a numeric rule, and nested arithmetic re-enters here because operand's arithmetic cases lower through operandReken. */
    private Frag operandReken(Acc acc, Expr e, Objecttype subject) {
        if (e instanceof Expr.AttribuutRef a && !ongenavigeerd(a) && eff.typeVan(a) != null && eff.typeVan(a).isNumeriek()
                && !isGedimensioneerd(a) && !isTemporeelAttribuut(a)) {
            NaviRef n = naviResolve(a, subject);
            if (n.soort() == NaviSoort.GENAVIGEERD) {
                fnImport("rsRelGetal");
                return Frag.ok("rsRelGetal(" + ident(n.rol(), false) + ", \"" + a.attribuut() + "\")");
            }
        }
        return operand(acc, e, subject);
    }

    // ---------------- leaves & helpers ----------------
    /** Whether an expression tree can ACTUALLY introduce an exact {@link io.github.yhavinga.regelspraak.runtime.Breuk} — i.e. it
     *  contains a §389 division (GEDEELD_DOOR, whose non-terminating quotient becomes a Breuk), a §486 PERCENTAGE_VAN,
     *  or a NON-terminating rational literal. A plain maal/plus/min of decimals never yields a Breuk, so it is NOT a
     *  source. Used to route a COMPARISON through the Breuk-aware rsAggVergelijk only when a Breuk can genuinely reach
     *  it (else the ordinary inline mvel comparison stays — no churn, no over-wrap). Recurses through the operands. */
    static boolean bevatBreukBron(Expr e) {
        if (e == null) return false;
        if (e instanceof Expr.Binair b) {
            if (b.op() == Expr.BinOp.GEDEELD_DOOR || b.op() == Expr.BinOp.PERCENTAGE_VAN) return true;
            return bevatBreukBron(b.links()) || bevatBreukBron(b.rechts());
        }
        if (e instanceof Expr.Begrensd bg) return bevatBreukBron(bg.basis()) || bevatBreukBron(bg.minimum()) || bevatBreukBron(bg.maximum());
        if (e instanceof Expr.Afgerond) return false;   // an afronding collapses any Breuk to a BigDecimal
        if (e instanceof Expr.Functie f) { for (Expr a : f.argumenten()) if (bevatBreukBron(a)) return true; return false; }
        if (e instanceof Expr.Literal lit && lit.type() != null && lit.type().isNumeriek()) {
            java.math.BigDecimal[] r = Rationaal.van(lit.waarde());
            if (r == null) return false;
            try { r[0].divide(r[1]); return false; } catch (ArithmeticException nietEindig) { return true; }
        }
        return false;
    }

    /** Whether a value expression could compute to an exact {@link io.github.yhavinga.regelspraak.runtime.Breuk} — the trigger
     *  for the §6.1.2 rsWaarde write-boundary collapse. True for any arithmetic (a §389/§486 division/percentage/
     *  maal/plus/min whose exactness can propagate), a begrenzing/afronding wrapping such arithmetic, and a
     *  NON-TERMINATING rational LITERAL (1/3 → a Breuk). A bare attribute/parameter read, a terminating literal, a
     *  date/text/boolean, or an aggregation is never a Breuk, so it stays unwrapped (no pin churn). Conservative:
     *  when in doubt (a compound), it wraps — the wrap is an identity for a BigDecimal. */
    private boolean kanBreukOpleveren(Expr e) {
        // Only a NUMERIC-typed result can be a Breuk. A DATUM-arithmetic Binair (a date ± a duration) or a
        // date/text builtin yields a KNOWN non-numeric value that rsWaarde (→ BigDecimal) would wrongly cast, so it
        // must NOT be wrapped — reject a known non-numeric result type. An Onbekend type (a beslistabel-desugared
        // arithmetic value carries one) falls through to the structural check: a Binair there is arithmetic (numeric),
        // and rsWaarde is a safe identity for its BigDecimal / Breuk result.
        if (eff.typeVan(e) != null && !eff.typeVan(e).isGetalachtig() && !eff.typeVan(e).isOnbekend()) return false;
        if (e instanceof Expr.Binair || e instanceof Expr.Begrensd || e instanceof Expr.Afgerond) return true;
        // ch. 4.10: a Let's result IS its body's (WF-076), so the collapse question is the body's; a binder read
        // hands back whatever its need computed, which may be an exact Breuk — the numeric-type gate above is what
        // keeps the rsWaarde wrap safe there, and for a terminating BigDecimal the wrap is the identity.
        if (e instanceof Expr.Let b) return kanBreukOpleveren(b.body());
        // ch. 10.2: either arm may be the value, so either arm may be the Breuk — a blind answer here takes the
        // (BigDecimal) cast path and THROWS on the exact rational the arm really computes.
        if (e instanceof Expr.IfThenElse k) return kanBreukOpleveren(k.dan()) || kanBreukOpleveren(k.anders());
        if (e instanceof Expr.BinderRef) return true;
        // §6.9 "de absolute waarde van" preserves a Breuk (rsAbs returns Object); other builtins (date-component,
        // datum-met, aggregation) return a BigDecimal/date, so they need no collapse and stay unwrapped.
        if (e instanceof Expr.Functie f && f.kind() == Expr.FunctieKind.ABSOLUTE_WAARDE) return true;
        if (e instanceof Expr.Literal lit && lit.type().isNumeriek()) {
            java.math.BigDecimal[] r = Rationaal.van(lit.waarde());
            if (r == null) return false;
            try { r[0].divide(r[1]); return false; }              // terminating -> a plain BigDecimal
            catch (ArithmeticException nietEindig) { return true; } // non-terminating -> a Breuk
        }
        return false;
    }

    /** The integer exponent when {@code e} is an integer-valued numeric literal ("2" in "tot de
     *  macht 2"), else null — lets an integer power lower exactly via {@code BigDecimal.pow(int)}
     *  rather than through Math.pow's double. Reads the number as {@link #literal} does, so a
     *  rational that reduces to a whole number ("4/2" -> 2) is an exact exponent too, while a true
     *  fraction ("3/2") or non-terminating quotient ("1/3") falls through to the Math.pow path. */
    private static Integer geheelExponent(Expr e) {
        if (!(e instanceof Expr.Literal lit) || !lit.type().isNumeriek()) return null;
        java.math.BigDecimal[] r = Rationaal.van(lit.waarde());
        if (r == null) return null;
        try {
            java.math.BigDecimal bd = r[0].divide(r[1]); // exact value; throws when non-terminating
            // BigDecimal.pow(int) is exact only for 0..999999999; a negative or fractional exponent
            // falls through to the Math.pow path (which handles them, at double precision).
            if (bd.signum() >= 0 && bd.stripTrailingZeros().scale() <= 0) {
                int n = bd.intValueExact();
                if (n <= 999_999_999) return n;
            }
        } catch (ArithmeticException ignore) {}
        return null;
    }

    /** Render a literal as DRL text — the VALUE comes from the shared {@link Literalen} reader (single shared implementation:
     *  one interpretation for both back-ends), this method only formats it for mvel. */
    private Frag literal(Expr.Literal lit) {
        Datatype t = lit.type();
        // §13.3.3.4 Tekst / §13.2.14 enumeratiewaarde 'X' (typed Onbekend at parse): both compare by VALUE
        // equality (§8.1.1). The VALUE comes from the shared reader — a Tekst verbatim, an enumeratiewaarde
        // without its single-quote delimiters — and drlString only RENDERS it: every double quote in it is
        // ESCAPED, never stripped, so the emitted constraint compares against the value the exec-model branch,
        // ref and clingo compare against. Stripping here (the reader's old blanket replace) is what made this
        // branch read `merk "AA"` as `merk AA` and a quote-delimited wire value as its bare interior.
        String label = Literalen.tekstOfEnumLabel(lit);
        if (label != null) return Frag.ok(drlString(label));
        if (t instanceof Datatype.Booleaan) return Frag.ok(Literalen.waar(lit.waarde()) ? "true" : "false");
        if (t.isDatum()) { // the ISO lexeme (WF-084/WF-095) — the shared readers parse; the components splice as
            // DECIMAL ints (a raw "08"/"09" would be an invalid octal literal in mvel/Java, §13.2.19).
            if (t instanceof Datatype.DatumTijd) {
                java.time.LocalDateTime dt = Literalen.parseDatumTijd(lit.waarde());
                // TERMINAL: an unreadable datum-tijd literal markers here, it never falls through to the bare
                // date below. That fall-through matched the ISO lexeme's DATE PREFIX and compared a
                // datum-tijd against midnight — a silent wrong answer (measured 2026-09-05: the sub-day
                // witness fired "verwacht aanwezig, werkelijk afwezig") where the exec-model branch at
                // least dropped the rule loudly.
                if (dt == null) return Frag.todo("datumtijdliteral", lit.waarde());
                return Frag.ok("java.time.LocalDateTime.of(" + dt.getYear() + ", " + dt.getMonthValue() + ", "
                        + dt.getDayOfMonth() + ", " + dt.getHour() + ", " + dt.getMinute() + ", "
                        + dt.getSecond() + ", " + dt.getNano() + ")");
            }
            java.time.LocalDate d = Literalen.parseDatum(lit.waarde());
            if (d == null) return Frag.todo("datumliteral", lit.waarde());
            return Frag.ok("java.time.LocalDate.of(" + d.getYear() + ", " + d.getMonthValue() + ", " + d.getDayOfMonth() + ")");
        }
        // numeric (possibly "0 €"), incl. a §13.2.5 rational ("4/2", "1_1/2", "1/3"). A terminating quotient
        // ("4/2"->2, "3/2"->1.5) splices as an exact BigDecimal constant (the fast path). A NON-terminating
        // rational ("1/3") lowers to an EXACT Breuk (rsBreuk("1","3")) — §389 — so its arithmetic stays exact
        // instead of the former 16-digit DECIMAL64 approximation (9 maal 1/3 = 3, not 2.9999999999999997).
        java.math.BigDecimal[] r = Rationaal.van(lit.waarde());
        if (r != null) {
            try {
                java.math.BigDecimal exact = r[0].divide(r[1]);   // throws when non-terminating
                return Frag.ok("new java.math.BigDecimal(\"" + exact.toPlainString() + "\")");
            } catch (ArithmeticException nietEindig) {
                // reduce to lowest terms so the emitted teller/noemer are the canonical fraction
                io.github.yhavinga.regelspraak.runtime.Breuk b = io.github.yhavinga.regelspraak.runtime.Breuk.van(r[0]).delen(io.github.yhavinga.regelspraak.runtime.Breuk.van(r[1]));
                fnImport("rsBreuk");
                return Frag.ok("rsBreuk(\"" + b.teller() + "\", \"" + b.noemer() + "\")");
            }
        }
        java.math.BigDecimal n = Literalen.getal(lit);
        if (n == null) return Frag.todo("literal", lit.waarde());
        return Frag.ok("new java.math.BigDecimal(\"" + n.toPlainString() + "\")");
    }

    /** §5.4/§13.4.17.9 reeks van teksten en waarden: an mvel concatenation of the literal chunks (quoted) and the
     *  «expressie» parts, each coerced to Tekst by the null-safe rsTekst helper (a leeg value -> "", never the string
     *  "null"). {@code arg} lowers a sub-expression — {@link #waarde} on the value side, {@link #operand} on the
     *  condition side. A markered sub-expression propagates (the Frag marker flag), so an unlowerable «…» part makes
     *  the whole tekstreeks an honest marker. */
    private Frag tekstReeks(Acc acc, Expr.TekstReeks tr, Function<Expr, Frag> arg) {
        fnImport("rsTekst");
        java.util.List<String> delen = new java.util.ArrayList<>();
        boolean marker = false;
        for (Expr.TekstReeks.Deel d : tr.delen()) {
            if (d instanceof Expr.TekstReeks.Letterlijk lit)
                delen.add("\"" + lit.tekst().replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
            else if (d instanceof Expr.TekstReeks.Ingevoegd ing) {
                Frag f = arg.apply(ing.expr());
                delen.add("rsTekst(" + f.text() + ")");
                marker |= f.marker();
            }
        }
        return new Frag("(" + String.join(" + ", delen) + ")", marker);
    }

    /** Wrap an emitted value in a unit conversion when its unit differs from (but is
     *  convertible to) the target unit; otherwise return it unchanged (§13.3.5.2).
     *  Never wraps an unlowered placeholder — that would emit (null).multiply(...). */
    /** Operands for a unit-aware COMPARISON, scaled to AVOID {@link #omreken}'s lossy ratio. omreken
     *  restates the right operand in the left's unit by {@code factorVan / factorNaar}, which truncates
     *  for a non-terminating quotient (mnd→jr = 1/12 = 0.0833…), so "24 mnd gelijk aan 2 jr" lowered to
     *  "2 != 1.9999…" — a false-positive violation (p43). For two SIMPLE convertible units this instead
     *  restates BOTH operands in their shared canonical unit by the DECLARED (exact) factorNaarCanoniek,
     *  so the comparison scales exactly (24 == 24). No conversion when units match or are absent; a
     *  composite (rate) unit keeps the single-operand ratio (its precision edge is carded). Returns
     *  {leftOut, rightOut}.
     *
     *  <p>An ENUMERATION comparison goes through here too, and its operands are read as the declared names they
     *  are: both wrapped in the runtime's {@code rsEnumNaam} (ref's {@code Checks.norm}, null left null), which is
     *  the identity ref and clingo compare labels by and the one the exec-model branch applies. Wrapping the
     *  OPERANDS rather than rewriting the operator keeps every caller's leegwaarde guard as it was. Either side
     *  decides, because an Onbekend-typed quoted literal from the source path carries no enumeration type of its
     *  own. The literal itself stays verbatim in {@link #literal}, which also serves the write position. */
    private String[] vergelijkingsOperanden(String l, Datatype lt, String r, Datatype rt) {
        if (lt instanceof Datatype.Enumeratie || rt instanceof Datatype.Enumeratie) {
            String f = fnImport("rsEnumNaam");
            return new String[]{f + "(" + l + ")", f + "(" + r + ")"};
        }
        io.github.yhavinga.regelspraak.model.Eenheid el = lt.eenheidOf(), er = rt.eenheidOf();
        Eenheidsconflict.eisHerschaalbaar(el, er, "een §5.1 vergelijking (DRL-tekst)");
        if (el == null || er == null || !el.convertibleNaar(er) || !el.vereistOmrekening(er))
            return new String[]{l, r};
        if (el.noemer() == null && er.noemer() == null)
            return new String[]{naarCanoniek(l, el), naarCanoniek(r, er)};
        return new String[]{l, omreken(r, rt, lt)};   // composite (rate) unit: keep the existing conversion
    }

    /** Multiply {@code expr} by a unit-conversion {@code factor} — the one place the ×factor restatement is
     *  emitted, as a call to the null-safe runtime helper (a leeg operand restates to leeg — never an NPE-abort;
     *  a Drools pattern constraint could not host an inline {@code == null} ternary anyway, and the SAME call
     *  form is valid on the value side, so both sides share the ONE {@code rsHerschaal} implementation).
     *  Identity when there is no real conversion (a {@code null} or unit (1) factor). The callers
     *  (omrekenWaarde / herschaalConditie / naarCanoniek) guard a placeholder {@link Frag} BEFORE calling. */
    private String vermenigvuldigMetFactor(String expr, java.math.BigDecimal factor) {
        if (factor == null || factor.compareTo(java.math.BigDecimal.ONE) == 0) return expr;
        String f = "new java.math.BigDecimal(\"" + factor.toPlainString() + "\")";
        return fnImport("rsHerschaal") + "(" + expr + ", " + f + ")";
    }

    /** EXACT §4 restatement of a value in {@code van} into {@code naar} for a +/- operand whose units are SIMPLE and
     *  convertible — multiply by the source unit's integer factorNaarCanoniek and DIVIDE by the target's (one DECIMAL64
     *  rounding, AT THE END), NOT a multiply by the pre-rounded ratio (which truncates mnd→jr = 1/12 = 0.0833…, so
     *  "0 jr + 24 mnd" read 1.9999… ≠ 2). Mirrors the §7.3.1 rsTijdSchaal exact-÷ and the comparison path's naarCanoniek.
     *  A COMPOSITE (rate) unit keeps the single-ratio path (its precision edge is separately carded); no conversion is
     *  the identity. One null-safe form for BOTH sides: the imported {@code rsHerschaalExact} runtime helper. */
    private String herschaalExact(String expr, Datatype van, Datatype naar) {
        io.github.yhavinga.regelspraak.model.Eenheid ev = van.eenheidOf(), en = naar.eenheidOf();
        Eenheidsconflict.eisHerschaalbaar(ev, en, "een §4-waardeherschaling (DRL-tekst, exacte factoren)");
        if (ev == null || en == null || !ev.convertibleNaar(en) || !ev.vereistOmrekening(en)) return expr;
        java.math.BigDecimal[] f = ev.exacteHerschaalFactoren(en);
        if (f == null)   // an opaque product unit (no canonical) / unknown factor: keep the single (lossy) ratio
            return vermenigvuldigMetFactor(expr, ev.omrekenfactorNaar(en));
        String fv = "new java.math.BigDecimal(\"" + f[0].toPlainString() + "\")";
        String fn = "new java.math.BigDecimal(\"" + f[1].toPlainString() + "\")";
        return fnImport("rsHerschaalExact") + "(" + expr + ", " + fv + ", " + fn + ")";
    }

    /** The §4 conversion factor to restate a {@code van}-typed quantity in the {@code naar} unit, or {@code
     *  null} when no conversion applies (either side unitless, not convertible, or already the same unit). */
    private static java.math.BigDecimal omrekenFactor(Datatype van, Datatype naar) {
        io.github.yhavinga.regelspraak.model.Eenheid ev = van.eenheidOf(), en = naar.eenheidOf();
        Eenheidsconflict.eisHerschaalbaar(ev, en, "een §4-omrekening in een constraint (DRL-tekst)");
        if (ev == null || en == null || !ev.convertibleNaar(en) || !ev.vereistOmrekening(en)) return null;
        return ev.omrekenfactorNaar(en);
    }

    /** Restate a comparison operand in its system's canonical unit by the DECLARED (exact) factor —
     *  a multiplication, never the lossy division omreken uses. Identity for a canonical/unitless
     *  operand or a placeholder marker. */
    private String naarCanoniek(String expr, io.github.yhavinga.regelspraak.model.Eenheid e) {
        return vermenigvuldigMetFactor(expr, e.factorNaarCanoniek());
    }

    /** Unit conversion for a CONSTRAINT operand — null-safe via the rsHerschaal helper ({@link #vermenigvuldigMetFactor}). */
    private String omreken(String expr, Datatype van, Datatype naar) {
        return vermenigvuldigMetFactor(expr, omrekenFactor(van, naar));
    }

    /** Unit conversion on the VALUE side, null-safe: a leegwaarde (an empty sommatie has no fallback) converts to a
     *  leegwaarde rather than an NPE on .multiply. EXACT for SIMPLE convertible units (×source.factorNaarCanoniek
     *  ÷target.factorNaarCanoniek, one DECIMAL64 rounding — {@link #herschaalExact}), so an assignment / begrenzing-bound /
     *  functie-arg / +/- restatement no longer truncates the non-terminating ratio mnd→jr = 1/12 (24 mnd stored in a jr
     *  attribute = 2, not 1.9999…). A COMPOSITE (rate) keeps the single ratio (carded). Only the "then"/value side — the
     *  constraint form is {@link #omreken} (lossy) / {@link #herschaalConditiePlus} (exact, +/-). */
    private Frag omrekenWaarde(Frag expr, Datatype van, Datatype naar) {
        return expr.marker() ? expr : Frag.ok(herschaalExact(expr.text(), van, naar));
    }

    /** §4 EXACT restatement of a value in {@code van} into {@code naar} on the CONSTRAINT side, shared by a +/- RIGHT
     *  operand AND a begrenzing min/max bound: a SIMPLE convertible cross-unit restate now FIRES via the EXACT
     *  rsHerschaalExact (no more lossy-ratio truncation), so "0 jr + 24 mnd == 2 jr" decides 24 == 24 and a "min van 24
     *  mnd" clamp compares against an exact 2 jr. A COMPOSITE (rate) cross-unit is exact too (exacteHerschaalFactoren
     *  folds the inverted denominator); only an OPAQUE product unit (no canonical factors) stays an honest marker; no
     *  conversion is the identity. The null-safe mirror of {@link #omrekenWaarde} (value side). */
    private Frag herschaalConditie(Frag expr, Datatype van, Datatype naar) {
        if (expr.marker()) return expr;
        io.github.yhavinga.regelspraak.model.Eenheid ev = van.eenheidOf(), en = naar.eenheidOf();
        Eenheidsconflict.eisHerschaalbaar(ev, en, "een §4-herschaling in een rekenkundige voorwaarde (DRL-tekst)");
        if (ev == null || en == null || !ev.convertibleNaar(en) || !ev.vereistOmrekening(en)) return expr;   // no conversion -> identity
        if (ev.exacteHerschaalFactoren(en) == null)   // an opaque product unit (no exact factors) -> honest marker
            return Frag.todo("eenheidconversie in een rekenkundige voorwaarde (niet-herschaalbare samengestelde eenheid)", expr.text());
        return Frag.ok(herschaalExact(expr.text(), van, naar));
    }

    /** §13.4.17.25 afronding: round a basis term to {@code af.decimalen()} via the matching
     *  {@link java.math.RoundingMode}, through the rsAfronding helper so a leeg basis rounds to leeg rather
     *  than NPE-ing on (null).setScale. That null-guard is load-bearing: wortel and macht carry a MANDATORY
     *  afronding (§13.4.17.13/.14), and §6.6/§6.7 make their leeg result leeg — which can only propagate if the
     *  afronding wrapper passes a null through. The helper (not an inline ternary) is also the form valid in a
     *  pattern constraint; shared by {@code waarde} and {@code operand} so an afronding lowers identically in
     *  either position. */
    private Frag afgerondeTerm(Frag basis, Expr.Afgerond af) {
        String mode = switch (af.modus()) {
            case NAAR_BENEDEN -> "FLOOR"; case NAAR_BOVEN -> "CEILING";
            case REKENKUNDIG -> "HALF_UP"; case RICHTING_NUL -> "DOWN"; case WEG_VAN_NUL -> "UP";
        };
        fnImport("rsAfronding");
        return Frag.from("rsAfronding(" + basis.text() + ", " + af.decimalen() + ", java.math.RoundingMode." + mode + ")", basis);
    }

    private static String infix(Expr.BinOp op) {
        return switch (op) {
            case PLUS -> "+";
            case MIN, VERMINDERD_MET -> "-";
            case MAAL -> "*";
            case GEDEELD_DOOR, GEDEELD_DOOR_ABS -> "/";
            default -> "+"; // TOT_DE_MACHT / PERCENTAGE_VAN handled by caller
        };
    }

    private static String javaType(Datatype t) {
        if (t.isPercentage() || t.isNumeriek()) return "java.math.BigDecimal";
        if (t.isTekst() || t instanceof Datatype.Enumeratie) return "String";   // an enumeratiewaarde is stored as its String label
        if (t instanceof Datatype.Booleaan) return "Boolean";
        if (t instanceof Datatype.DatumInDagen) return "java.time.LocalDate";
        if (t instanceof Datatype.DatumTijd) return "java.time.LocalDateTime";
        return "Object";
    }

    private static String todo(String wat, String bron) { return "/* TODO " + commentVeilig(wat + ": " + bron) + " */ null"; }

    /** As {@link #todo(String, String)} but from a structured {@link Bron} (its snippet is spliced). */
    private static String todo(String wat, Bron bron) { return todo(wat, bron == null ? null : bron.snippet()); }

}
