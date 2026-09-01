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
 * §9.5 fout-companion reads as "consistent", a missing read reads as "nothing to watch". Each such walk must
 * therefore descend via {@code ExecModelTranspiler.letKinderen} as its FIRST step, and each one that does is
 * listed here so the next addition sees the list:
 * {@code eigenReads} (the freshness watch), {@code navRefs} (far-side reactivity),
 * {@code exprLeesnamen} (defaults salience), {@code bevatDeling} (the defaults division fence),
 * {@code verzamelDelingen} (§6.5), {@code verzamelWortels} (§6.6) and {@code kanBreukOpleveren}
 * (the exact-comparison routing; blind, it takes a {@code (BigDecimal)} cast that throws).
 * The one walk that deliberately does NOT descend is {@code startpuntCompanions} (§6.8) — its reason, and the
 * two-flank measurement behind it, are recorded at that method.
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
