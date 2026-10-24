package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * What one effective defaults stack decided for one object at the fixpoint of a session, read off the control facts
 * the stack left standing ({@link ControlFact}), never computed again ({@code DroolsBackend.decisionsOf}): which default
 * decided, with its decision, which conflict stood, or that no default decided. Every object of the session, the beans
 * of the input and the objects the creations made ({@link Fixpoint#objects}), has one record per effective stack of its
 * concrete type. A record with neither a decider nor a conflict is one on which no default decided: its target keeps
 * its input (RUNTIME-SEMANTICS ch. 7.1), and its {@link #rootChains} still name the defaults of the stack in their order.
 *
 * <p>A default is named by a JSON pointer (RFC 6901) into the IR document the build was made from: the {@code Default}
 * node, {@code /rules/prioritizedDefaults/3/defaults/1/exceptions/members/0/defaults/0}, on any depth of nesting
 * (ch. 7.9). An ordinary rule that a subtype's stack takes in as its base or its initialisation default (ch. 7.10,
 * SUBTYPING §4.1 rules 1 and 4) has no {@code Default} node: it is named by the rule version it comes from,
 * {@code /rules/rules/0/versions/0}. A consumer lays a pointer back on the document, or on the model it made the
 * document from: the emitter writes the groups of {@code RegelSpraakModel.prioritizedDefaults} in their order, and
 * each default, exception group and member at its index.
 *
 * <p>{@link #rootChains} gives the order in which the stack evaluates its defaults, which the pointers alone do not
 * give where ch. 7.10 composes a subtype's stack from two declarations: per group at the root of the effective stack the
 * pointers of its defaults, highest first; more than one group only at a declared-unordered root (ch. 7.2). Below the
 * root a default's exception groups are evaluated in the order the document writes them, so the pointer of a default
 * there names the defaults before and after it in its chain as well. The whole composition, with the exception groups
 * below the root, is the composition of the stack in {@code TmsTranspiler.stacks} ({@link StackComposition}), whose
 * chains at the root are these.
 *
 * @param object         the object the stack decided for, a bean of the build's bean model, compared by identity
 * @param member         the stack's target, named as the model declares it, as {@link Fixpoint} names a member
 * @param characteristic whether the target is a characteristic (true) or an attribute (false)
 * @param rootChains     the pointers of the defaults at the root of the effective stack, per group, highest first
 * @param decider        the default that decided, or null where a conflict stood or no default decided
 * @param conflict       the conflict that stood, or null where a default decided or no default decided
 */
public record DefaultsDecision(Object object, String member, boolean characteristic, List<List<String>> rootChains,
                               Decider decider, Conflict conflict) {

    public DefaultsDecision {
        Objects.requireNonNull(object, "object");
        Objects.requireNonNull(member, "member");
        rootChains = rootChains.stream().map(List::copyOf).toList();
        if (decider != null && conflict != null)
            throw new IllegalArgumentException("een beslissing van een stapel noemt hoogstens één van beslisser en"
                    + " conflict, niet beide");
    }

    /**
     * A default that decided, the default whose own content gave the decision: at a default with exception groups the
     * member that decided, followed down to its own default (ch. 7.9), and the default itself only where it fell back
     * on its own decision because no member settled. Where several members decided alike, the first in member order,
     * the one the reconciliation takes its decision from.
     *
     * @param pointer  the default, as {@link DefaultsDecision} names a default
     * @param decision a characteristic's decision, a {@code Boolean}: true attributes, false does not; an attribute's
     *                 value, in the form {@link Fixpoint.DerivedValue} gives it
     */
    public record Decider(String pointer, Object decision) {
        public Decider {
            Objects.requireNonNull(pointer, "pointer");
            Objects.requireNonNull(decision, "decision");
        }
    }

    /**
     * A conflict (ch. 7.2 at the root, ch. 7.9 below it): two or more members that decided differently, so the stack
     * wrote nothing and deposited the §9.5 inconsistency under {@link #key}. A conflict among the exception groups of a
     * default goes up to the root unchanged, the first in member order where more than one stood.
     *
     * @param pointer  the default whose exception groups conflicted, or null for the groups at the root of the stack
     * @param key      the rule of the §9.5 inconsistency: the target on the type that declares it, and below the root
     *                 the default-index path ({@code Jutter.wachtgeld}, {@code Jutter.penninggerechtigd/1})
     * @param deciders every member that decided, in member order, each followed down to its default as
     *                 {@link Decider} is
     */
    public record Conflict(String pointer, String key, List<Decider> deciders) {
        public Conflict {
            Objects.requireNonNull(key, "key");
            deciders = List.copyOf(deciders);
        }
    }
}
