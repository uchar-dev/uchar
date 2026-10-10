package dev.uchar.regelspraak.transpiler.tms;

/**
 * A control fact of the lowering of a defaults group (RUNTIME-SEMANTICS ch. 7): a logical fact that records how far
 * one effective stack got on one subject, which default decided or how a reconciliation came out, and that no form of
 * the model reads or writes. The derivations of the group are {@link Waarde} and {@link HeeftKenmerk}, its conflict a
 * §9.5 inconsistency; these facts are what decides between them under truth maintenance, where the destructive
 * lowering computed the same in one consequence until card tms-omschakeling.
 *
 * <p>The interface marks them so that a session can be measured at its end: the facts that implement it are exactly
 * the control facts the defaults left standing, apart from the derivations and the standings, and {@link #stapel}
 * groups them per effective stack (the bookkeeping that {@code spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md} §5.1
 * weighs against a lowering with control facts).
 */
public sealed interface Stuurfeit permits Beslist, Uitkomst {

    /** The object the stack decides for. */
    Object onderwerp();

    /** The name of the effective stack, one defaults group on one concrete type and one target
     *  ({@code TmsTranspiler.Defaultsgroep#naam}). */
    String stapel();
}
