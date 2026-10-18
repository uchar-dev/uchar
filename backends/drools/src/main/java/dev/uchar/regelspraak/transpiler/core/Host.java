package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;

/**
 * The GASTHEER test of a characteristic check (WF-109, {@code spec/IR-SPEC.md} ch. 4.6 (q2);
 * {@code spec/RUNTIME-SEMANTICS.md} ch. 5.8b): does the pattern bound to {@code subject} carry a check
 * hosted on {@code k.owner()}?
 *
 * <p>The answer is EXTENSIONAL, and that is the whole point of this class. The spec reads "an object of
 * type T satisfies host O iff T ⊑ O", so a host naming a SUPERTYPE of the pattern's type is satisfied —
 * an inherited characteristic is read on the object that hosts it (ch. 2.2). Both transpiler branches
 * asked the question by NAME IDENTITY until 2026-09-19 ({@code k.owner() != subject}), which made a
 * legal supertype host — measured: a {@code Sein} doel under a {@code Baken} host — lower to NEITHER
 * pattern and the whole creation refuse itself by name, while the verifier, {@code ref} and the ASP
 * backend all carried the document. One verifier-clean form, two executors answering and two refusing:
 * exactly the split WF-109 exists to close.
 *
 * <p>ONE place for the two branches, on the WF-097 shared-fence discipline: an identity test is one
 * character away from returning and neither branch would notice the other drifting back to it.
 */
public final class Host {
    private Host() {}

    /** Whether {@code subject}'s pattern hosts {@code k} — true when the host is absent (the check is
     *  unconstrained and rides whatever pattern carries the condition) or when {@code subject ⊑ host}. */
    public static boolean hostOp(Condition.CharacteristicCheck k, Objecttype subject) {
        Objecttype host = k.owner();
        if (host == null) return true;
        return subject != null && subject.chain().contains(host);
    }

    /** The reason both branches give when a creation guard carrying a whole-period qualifier declines on the anker. */
    public static final String TARGET_ATTEMPT_WHOLE_PERIOD =
            "voorwaarde niet naar anker of doel te lowereren — gehele-periodekwalificatie: alleen de anker is gebonden (WF-109)";

    /** Whether a §13.4.7 creation guard may be tried on the DOEL at all. Not when it carries a §8.2 whole-period
     *  qualifier anywhere: WF-109's third scope fence binds only the anker there, so a doel lowering answers a
     *  question the document did not ask. Measured 2026-09-23 on verifier-clean documents that reach the doel
     *  attempt: the exec branch read the doel where ref and clingo read the anker, and the DRL-text branch — whose
     *  whole-period lowering is wired to {@code $subject}, the anker inside the {@code $doel} pattern — either sank
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
    public static boolean targetHostOutsideRange(Condition guard, Objecttype target,
            java.util.function.Predicate<Condition.CharacteristicCheck> navigated) {
        if (guard instanceof Condition.CharacteristicCheck k && !navigated.test(k)) return false;
        return namesTargetHost(guard, target);
    }

    private static boolean namesTargetHost(Condition c, Objecttype target) {
        if (c instanceof Condition.CharacteristicCheck k) return k.owner() != null && hostOp(k, target);
        if (c instanceof Condition.Compound s) return s.components().stream().anyMatch(d -> namesTargetHost(d, target));
        return false;
    }

    private static boolean carriesWholePeriod(Condition c) {
        if (c instanceof Condition.Comparison v) return v.periode() != null;
        if (c instanceof Condition.Predicate p) return p.periode() != null;
        if (c instanceof Condition.CharacteristicCheck k) return k.periode() != null;
        if (c instanceof Condition.Compound s) return s.components().stream().anyMatch(Host::carriesWholePeriod);
        if (c instanceof Condition.QuantifierFold kv) return carriesWholePeriod(kv.filter()) || carriesWholePeriod(kv.test());
        return false;
    }
}
