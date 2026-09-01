package io.github.yhavinga.regelspraak.loader;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.model.Datatype;

/**
 * The IR-SPEC ch. 4.10 <b>Let cluster</b> as the Drools backend's OWN shape — {@code Let}, {@code BinderRef}
 * and {@code IfThenElse} (semantics {@code RUNTIME-SEMANTICS.md} ch. 10.6 / ch. 10.2).
 *
 * <p><b>Why a shape of our own.</b> {@link Expr} is a SEALED union in the vendored frontend slice, which the
 * toolchain consumes at a pin; it cannot grow three nodes here. So the loader parses each wire node into a
 * CARRIER — an {@link Expr.Onopgelost} typed with the node's own datatype — and registers the real shape
 * beside the model in an identity-keyed map, exactly the side-channel {@code IrLoader.Loaded.rolBindingen}
 * already uses for the symbolic role bindings.
 *
 * <p><b>What the carrier does and does not guarantee.</b> On the VALUE paths it is safe by construction:
 * nothing in the transpiler lowers an {@code Onopgelost}, so a missed consultation of this side channel
 * returns null and degrades to a LOUD, reason-carrying skip in the gate report.
 *
 * <p>That guarantee does <b>not</b> extend to the STRUCTURAL walks. To every generic {@code Expr} tree walk a
 * carrier is an opaque LEAF, so a walk that does not descend explicitly under-reports in SILENCE — a missing
 * §9.5 fout-companion reads as "consistent", a missing read reads as "nothing to watch". Two accessors on
 * {@code ExecModelTranspiler} do that descent, and which one a walk takes is decided by what the walk's output
 * IS. The FULL inventory, so the next addition sees it:
 *
 * <p><b>(a) Descend via {@code letKinderen}</b> — every hidden child (a Binding's value+body, a Keuze's two
 * branches and every Expr operand of its condition). Their output is a REFUSAL or a REGISTRATION, so seeing
 * more than actually runs is still a correct answer: {@code bevatDeling} (the ch. 7 defaults division fence —
 * refuses more often), {@code navRefs} (far-side reactivity — watches more), {@code exprLeesnamen} (defaults
 * salience — sinks further), {@code kanBreukOpleveren} (exact-comparison routing — at worst takes the exact
 * path where the plain one would also have served; blind, it takes a {@code (BigDecimal)} cast that throws).
 *
 * <p><b>(b) Descend via {@code letKinderenZeker}</b> — only the CERTAINLY evaluated children (a Binding's
 * value, since ch. 10.6 runs the body only past the leeg-barrier; a Keuze's condition operands, since ch. 10.2
 * runs exactly one branch; nothing for a Lezing). Their output is an ANSWER — a fout-companion rule — and one
 * built off a path that never runs makes this backend say INCONSISTENT where ref says consistent:
 * {@code verzamelDelingen} (§6.5) and {@code verzamelWortels} (§6.6). The converse limit (a fault on a branch
 * that IS taken gets no companion either) is recorded at that accessor.
 *
 * <p><b>(c) Deliberately do NOT descend</b>, each for a checked reason:
 * <ul>
 *   <li>{@code startpuntCompanions} (§6.8) — measured on a two-flank pair: ref carries no leeg-anchor check at
 *       all, so descending would ADD a ref-divergence instead of removing one. Recorded at that method;</li>
 *   <li>{@code heeftTijdgebonden} (§4.2) — it descends through the VENDORED {@code Expr.bevat}, whose default
 *       arm makes an {@code Onopgelost} a leaf. A §5.3 Rekendatum/Rekenjaar behind a carrier therefore leaves
 *       the rekendatum global unregistered — and the value lowering that needs it then skips LOUDLY ("regel
 *       (THEN-waarde refereert niet-geregistreerde global rekendatum)"), never quietly;</li>
 *   <li>{@code parityOnveilig}, {@code containsAggregatie}, {@code isDatumRekenExpr}, {@code lowerVal} — each
 *       pattern-matches concrete v0 node shapes, so a carrier matches none and the answer is "not that shape".
 *       None of the four can fabricate a fault: what the carrier hides is lowered — or refused, loudly — by
 *       {@code scalarWaarde}, which consults this side channel FIRST, and {@code lowerVal}'s own fall-through
 *       is a null, i.e. an honest skip;</li>
 *   <li>{@code companionOperand} — a carrier is not an {@code AttribuutRef}, so a division/wortel operand
 *       hidden behind one never qualifies and raises no Inconsistentie. That is the §6.5/§6.6 scope limit
 *       already documented there for a literal / parameter / to-many / temporal operand, not a new gap;</li>
 *   <li>{@code transpiler.Stratificatie.verzamelReads} (§9.2) — a {@code Conditie} walk over a rule's
 *       {@code voorwaarde} only, so a Keuze's condition (which lives in the VALUE) never reaches it. Doubly
 *       unreachable today: {@code letConditie} refuses every condition form but a bare numeric Vergelijking,
 *       so a Keuze carrying a kenmerk read does not lower and its rule is skipped whole.</li>
 * </ul>
 */
public sealed interface LetVorm {

    /** The node's own datatype (a {@code Let}'s is its body's — WF-076). */
    Datatype type();

    /** ch. 10.6 the strict value-level binding: evaluate {@code value} in the ENCLOSING scope; a leeg value
     *  propagates (the whole Let is leeg and {@code body} never runs — the leeg-barrier), otherwise bind
     *  {@code binder} and evaluate {@code body}. The binder is visible only in {@code body} (WF-076). */
    record Binding(String binder, Expr value, Expr body, Datatype type) implements LetVorm {}

    /** WF-064: a read of the nearest enclosing binder of {@code naam}. A bound binder is never leeg. */
    record Lezing(String naam, Datatype type) implements LetVorm {}

    /** ch. 10.2 the value-level conditional inside a strict Let body (the WF-069 widening): evaluate the
     *  condition under the ordinary comparison discipline — its operands disciplined to bound-non-leeg reads
     *  by WF-077 — then evaluate EXACTLY the chosen branch, strict, no coalesce. */
    record Keuze(Conditie conditie, Expr dan, Expr anders, Datatype type) implements LetVorm {}
}
