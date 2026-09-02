package io.github.yhavinga.regelspraak.loader;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.model.Datatype;

/**
 * The IR-SPEC ch. 4.10 <b>Let cluster</b> as the Drools backend's OWN shape — {@code Let}, {@code BinderRef}
 * and {@code IfThenElse} (semantics {@code RUNTIME-SEMANTICS.md} ch. 10.6 / ch. 10.2).
 *
 * <p><b>Why a shape of our own.</b> {@link Expr} is a SEALED union in the vendored frontend slice, which the
 * toolchain consumes at a pin. It has since grown {@code Let} and {@code BinderRef} — the RegelSpraak emitter
 * lowers a §13.4.2.10 variabelendeel to them — but NOT {@code IfThenElse} (still emitter-free, IR-SPEC ch. 4.10),
 * and this backend keeps ONE shape for the whole cluster rather than half a union and half a side channel. So the
 * loader parses each wire node into a CARRIER — an {@link Expr.Onopgelost} typed with the node's own datatype —
 * and registers the real shape beside the model in an identity-keyed map, exactly the side-channel
 * {@code IrLoader.Loaded.rolBindingen} already uses for the symbolic role bindings. A union {@code Expr.Let} /
 * {@code Expr.BinderRef} therefore never reaches the transpiler; one that did would under-report in silence
 * through the walks below, so {@code ExecModelTranspiler.geenUnieLet} fails fast on it at every entry point.
 *
 * <p><b>What the carrier does and does not guarantee.</b> On the VALUE paths it is safe by construction:
 * nothing in the transpiler lowers an {@code Onopgelost}, so a missed consultation of this side channel
 * returns null and degrades to a LOUD, reason-carrying skip in the gate report.
 *
 * <p>That guarantee does <b>not</b> extend to the STRUCTURAL walks. To every generic {@code Expr} tree walk a
 * carrier is an opaque LEAF, so a walk that does not descend explicitly under-reports in SILENCE — a missing
 * §9.5 fout-companion reads as "consistent", a missing read reads as "nothing to watch". ONE accessor on
 * {@code ExecModelTranspiler} does that descent — {@code letKinderen}, every hidden child: a Binding's value+body,
 * a Keuze's two branches and every Expr operand of its condition — and every walk that must see through a
 * carrier takes it. The FULL inventory, so the next addition sees it:
 *
 * <p><b>(a) Descend via {@code letKinderen}</b>, output a REFUSAL or a REGISTRATION, where seeing more than
 * actually runs is still a correct answer: {@code foutplekSoort} (the ch. 7 defaults division/wortel fence —
 * refuses more often), {@code navRefs} (far-side reactivity — watches more), {@code exprLeesnamen} (defaults salience —
 * sinks further), {@code kanBreukOpleveren} (exact-comparison routing — at worst takes the exact path where
 * the plain one would also have served; blind, it takes a {@code (BigDecimal)} cast that throws).
 *
 * <p><b>(b) Descend via {@code letKinderen}</b>, output an ANSWER — a §6.5/§6.6 fout-companion rule, which is
 * WRONG (inconsistent where ref says consistent) if built off a path that never runs: {@code verzamelDelingen}
 * and {@code verzamelWortels}. They see every site that MAY run, and the companion of a site under a carrier
 * carries the PATH: its condition re-evaluates the outermost carrier on the subject with a probe
 * ({@code ExecModelTranspiler.Sonde}) and fires only when that very site ran and faulted — ch. 10.2 (exactly
 * one branch) and ch. 10.6 (call-by-need, the barrier at the read) come from the value lowering itself, and the
 * evaluation ORDER within each form is mirrored from ref, measured per form (a Binair left before right; a
 * Begrensd basis, then maximum, then minimum) — the lowering is ref's operational semantics only where that
 * order agrees. A site outside every carrier keeps the static
 * leeg/negatief predicate (the unchanged v0 companion). Before the companion-onder-drager card these two walks
 * took a "certainly evaluated" subset ({@code letKinderenZeker}) and so MISSED every real fault in a body, a
 * taken branch or a partially-read value; that accessor is gone.
 *
 * <p><b>(c) Deliberately do NOT descend</b>, each for a checked reason:
 * <ul>
 *   <li>{@code startpuntCompanions} (§6.8) — measured on a two-flank pair: ref carries no leeg-anchor check at
 *       all, so descending would ADD a ref-divergence instead of removing one. Recorded at that method;</li>
 *   <li>{@code heeftTijdgebonden} (§4.2) — it descends through the VENDORED {@code Expr.bevat}, whose
 *       {@code Onopgelost} arm is a leaf (the switch is exhaustive since the let-lowering card; an
 *       {@code Onopgelost} maps to {@code false} explicitly). A §5.3 Rekendatum/Rekenjaar behind a carrier therefore leaves
 *       the rekendatum global unregistered — and the value lowering that needs it then skips LOUDLY ("regel
 *       (THEN-waarde refereert niet-geregistreerde global rekendatum)"), never quietly;</li>
 *   <li>{@code parityOnveilig}, {@code containsAggregatie}, {@code isDatumRekenExpr}, {@code lowerVal} — each
 *       pattern-matches concrete v0 node shapes, so a carrier matches none and the answer is "not that shape".
 *       None of the four can fabricate a fault: what the carrier hides is lowered — or refused, loudly — by
 *       {@code scalarWaarde}, which consults this side channel FIRST, and {@code lowerVal}'s own fall-through
 *       is a null, i.e. an honest skip;</li>
 *   <li>{@code companionOperand} — a carrier is not an {@code AttribuutRef}, so a division/wortel operand
 *       hidden behind one (a {@code BinderRef} dividend, say) never qualifies and raises no Inconsistentie.
 *       That is the §6.5/§6.6 scope limit already documented there for a literal / parameter / to-many /
 *       temporal operand, kept deliberately (maintainer scope of the companion-onder-drager card) — the path
 *       probe would make widening it a one-line change, since it observes the evaluation rather than
 *       re-reading the operands;</li>
 *   <li>{@code transpiler.Stratificatie.verzamelReads} (§9.2) — a {@code Conditie} walk over a rule's
 *       {@code voorwaarde} only, so a Keuze's condition (which lives in the VALUE) never reaches it. Doubly
 *       unreachable today: {@code letConditie} refuses every condition form but a bare numeric Vergelijking,
 *       so a Keuze carrying a kenmerk read does not lower and its rule is skipped whole.</li>
 * </ul>
 */
public sealed interface LetVorm {

    /** The node's own datatype (a {@code Let}'s is its body's — WF-076). */
    Datatype type();

    /** ch. 10.6 the value-level binding as a NEED (call-by-need, maintainer decision O3): bind {@code binder}
     *  and evaluate {@code body}; the {@code value} — in the ENCLOSING scope, so the binder is invisible in it
     *  (WF-076) — is evaluated at the FIRST read of the binder and that one result serves every further read.
     *  A read of a leeg need makes the whole Let leeg (the leeg-barrier, at the READ place); an unread binder
     *  evaluates nothing at all and the Let's outcome IS the body's. */
    record Binding(String binder, Expr value, Expr body, Datatype type) implements LetVorm {}

    /** WF-064: a read of the nearest enclosing binder of {@code naam}. A bound binder is never leeg — the read
     *  either yields a gevuld value or ends its owning Let. */
    record Lezing(String naam, Datatype type) implements LetVorm {}

    /** ch. 10.2 the value-level conditional inside a Let body (the WF-069 widening): evaluate the
     *  condition under the ordinary comparison discipline — its operands disciplined to bound-non-leeg reads
     *  by WF-077 — then evaluate EXACTLY the chosen branch, strict, no coalesce. */
    record Keuze(Conditie conditie, Expr dan, Expr anders, Datatype type) implements LetVorm {}
}
