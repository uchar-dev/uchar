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
 * <p><b>Why {@code Onopgelost} is the right carrier.</b> Nothing in the transpiler lowers an
 * {@code Onopgelost}: every value path returns null for it, which is an HONEST, reason-carrying skip. So a
 * consultation of this side channel that is ever missed degrades to a loud refusal in the gate report — never
 * to a silently wrong answer.
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
