package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * How one effective defaults stack is composed ({@code TmsTranspiler.Stack.composition}): its defaults in the order of
 * priority in which the stack evaluates them, across the groups RUNTIME-SEMANTICS ch. 7.10 composes it from, each with
 * the exception groups it carries (ch. 7.9), and where chains stand side by side without an order between them
 * ({@link #unordered}). Read off the knots the lowering laid for the stack, the ones
 * {@code TmsTranspiler.decisionsOf} lays the control facts of a session on, never off the labels in the names of its
 * rules ({@code default 0.0.1.0.0}) or the order of the DRL text.
 *
 * <p>One record for two places, since a default reconciles its exception groups exactly as the root of a stack
 * reconciles its groups (ch. 7.9): at the root the chains are the groups of the effective stack, below a default with
 * exception groups ({@link Entry#exceptions}) they are those exception groups. A chain is ordered, highest first.
 *
 * <p>A default is named by the JSON pointer {@link DefaultsDecision} names one by, and {@code originOf} the default of a
 * rule: the {@code Default} node, {@code /rules/prioritizedDefaults/1/defaults/0/exceptions/members/0/defaults/2}, or for
 * an ordinary rule that a subtype's stack takes in as its base or its initialisation (ch. 7.10, SUBTYPING §4.1 rules 1
 * and 4) the version of that rule, {@code /rules/rules/0/versions/0}. Each default of the stack stands in it once.
 *
 * <p>At the root, where nothing is inherited, the chains are the groups on the target in the order of the document, and
 * each chain the defaults of its group in their order. Where a type contributes its own group, or its ordinary rule,
 * over an inherited one, its stack has one chain in the fixed block order of ch. 7.10, which nothing on the wire
 * declares: the type's own exceptions, the inherited exceptions (over three levels the child's above the
 * grandparent's), the base (the type's own, which replaces the inherited one, else the inherited one), the
 * initialisations. A group-default (exception groups and no own content) is an exception of its block, with its
 * exception groups below it: the group {@code meldtermijn voor Strandjutter} of
 * {@code jutterswet-meldtermijn-strandjutter}, whose one default carries the exception group of an
 * {@code Afwijking van}, stands in the stack of a {@code Strandjutter} above the base of the group on {@code Jutter}.
 * A type that contributes nothing on the target inherits its supertype's composition whole, a declared-unordered root
 * among it. Below a default, the exception groups and the defaults of each stand in the order of the document, so the
 * pointer of each is the pointer of its default with {@code /exceptions/members/<m>/defaults/<j>} after it.
 *
 * <p>The chains at the root are the ones {@link DefaultsDecision#rootChains} gives, pointer for pointer.
 *
 * @param chains the chains, each the defaults of one group or exception group, highest first; at the root more than one
 *               only at a declared-unordered root (ch. 7.2), below a default one per exception group
 */
public record StackComposition(List<List<StackComposition.Entry>> chains) {

    public StackComposition {
        Objects.requireNonNull(chains, "chains");
        if (chains.isEmpty() || chains.stream().anyMatch(List::isEmpty))
            throw new IllegalArgumentException("een samenstelling heeft ten minste één keten en elke keten ten minste"
                    + " één default: " + chains);
        chains = chains.stream().map(List::copyOf).toList();
    }

    /**
     * Whether the chains stand side by side without an order: more than one. They are reconciled as equals (ch. 7.2 at
     * the root, ch. 7.9 below a default), which is what the document declares with {@code unordered}, on the groups at
     * the root (IR-SPEC ch. 6.1, WF-050) or on {@code exceptions} below a default (WF-075). A single chain is ordered,
     * also where the document marks it {@code unordered}, since nothing stands beside it.
     */
    public boolean unordered() {
        return chains.size() > 1;
    }

    /**
     * One default at its place in its chain.
     *
     * @param pointer    the default, as {@link StackComposition} names one
     * @param exceptions its exception groups (ch. 7.9), with their defaults, or null where it carries none
     */
    public record Entry(String pointer, StackComposition exceptions) {
        public Entry {
            Objects.requireNonNull(pointer, "pointer");
        }
    }
}
