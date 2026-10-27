package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.model.RegelSpraakModel.ObjectType;

/**
 * The GASTHEER test of a characteristic check (WF-109, {@code spec/IR-SPEC.md} ch. 4.6 (q2);
 * {@code spec/RUNTIME-SEMANTICS.md} ch. 5.8b): does the pattern bound to {@code subject} carry a check
 * hosted on {@code k.owner()}?
 *
 * <p>The answer is EXTENSIONAL, and that is the whole point of this class. The spec reads "an object of
 * type T satisfies host O iff T ⊑ O", so a host naming a SUPERTYPE of the pattern's type is satisfied —
 * an inherited characteristic is read on the object that hosts it (ch. 2.2). Asked by NAME IDENTITY
 * ({@code k.owner() != subject}), a legal supertype host — measured 2026-09-19: a {@code Sein} doel under a
 * {@code Baken} host — would lower to NEITHER pattern and the whole creation would refuse itself by name, while
 * the verifier, {@code ref} and the ASP backend carry the document: one verifier-clean form, executors
 * answering and refusing, exactly the split WF-109 exists to close.
 *
 * <p>ONE place, on the WF-097 shared-fence discipline: an identity test is one character away, and a second
 * copy could drift to it unnoticed.
 */
public final class Host {
    private Host() {}

    /** Whether {@code subject}'s pattern hosts {@code k} — true when the host is absent (the check is
     *  unconstrained and rides whatever pattern carries the condition) or when {@code subject ⊑ host}. */
    public static boolean hostOp(Condition.CharacteristicCheck k, ObjectType subject) {
        ObjectType host = k.owner();
        if (host == null) return true;
        return subject != null && subject.chain().contains(host);
    }

    /** Whether {@code k} names a narrowing host over {@code subject}: a type strictly below it, whose characteristic the
     *  check reads on the subject itself, false where the subject is not an instance of the host
     *  ({@code spec/RUNTIME-SEMANTICS.md} ch. 5.8b rule 4). */
    public static boolean narrows(Condition.CharacteristicCheck k, ObjectType subject) {
        ObjectType host = k.owner();
        return host != null && subject != null && host != subject && host.chain().contains(subject);
    }

    /** The reason both branches give when a creation guard carrying a whole-period qualifier declines on the anker. */
    public static final String TARGET_ATTEMPT_WHOLE_PERIOD =
            "voorwaarde niet naar anker of doel te lowereren — gehele-periodekwalificatie: alleen de anker is gebonden (WF-109)";

    /** Whether a §13.4.7 creation guard may be tried on the DOEL at all. Not when it carries a §8.2 whole-period
     *  qualifier anywhere: WF-109's third scope fence binds only the anker there, so a doel lowering answers a
     *  question the document did not ask. Measured 2026-09-23 on verifier-clean documents that reach the doel
     *  attempt: the exec branch read the doel where ref and clingo read the anker, and the DRL-text branch — whose
     *  whole-period lowering is wired to {@code $subject}, the anker inside the {@code $target} pattern — either sank
     *  the whole package at compile or read the anker from the doel's pattern. */
    public static boolean targetAttemptAllowed(Condition c) {
        return !carriesWholePeriod(c);
    }

    /** The reason both branches give when a creation guard names the doel as host where WF-109 does not bind it. */
    public static final String TARGET_ATTEMPT_NESTED =
            "voorwaarde niet naar anker of doel te lowereren — doelgastheer binnen een samengestelde of genavigeerde"
            + " voorwaarde: alleen de anker is gebonden (WF-109)";

    /** WF-109's first two doel fences: the doel is a bound object only where the check hosted on it IS the whole
     *  guard and does not navigate (the third fence, the whole-period qualifier, is {@link #targetAttemptAllowed}'s).
     *  True when the guard names a host the doel satisfies anywhere else — inside a composite at any depth, or on a
     *  navigating check. Measured 2026-09-23: a composite whose checks were all hosted on the doel's supertype was
     *  refused by the verifier, {@code ref}, clingo and the exec-model branch, and PLAYED by the DRL-text branch,
     *  which inlines a composite on the doel pattern. */
    public static boolean targetHostOutsideRange(Condition guard, ObjectType target,
            java.util.function.Predicate<Condition.CharacteristicCheck> navigated) {
        if (guard instanceof Condition.CharacteristicCheck k && !navigated.test(k)) return false;
        return namesTargetHost(guard, target);
    }

    private static boolean namesTargetHost(Condition c, ObjectType target) {
        if (c instanceof Condition.CharacteristicCheck k) return k.owner() != null && hostOp(k, target);
        if (c instanceof Condition.Compound s) return s.components().stream().anyMatch(d -> namesTargetHost(d, target));
        return false;
    }

    private static boolean carriesWholePeriod(Condition c) {
        if (c instanceof Condition.Comparison v) return v.period() != null;
        if (c instanceof Condition.Predicate p) return p.period() != null;
        if (c instanceof Condition.CharacteristicCheck k) return k.period() != null;
        if (c instanceof Condition.Compound s) return s.components().stream().anyMatch(Host::carriesWholePeriod);
        if (c instanceof Condition.QuantifierFold kv) return carriesWholePeriod(kv.filter()) || carriesWholePeriod(kv.test());
        return false;
    }
}
