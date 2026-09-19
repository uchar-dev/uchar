package io.github.yhavinga.regelspraak.transpiler.kern;

import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;

/**
 * The GASTHEER test of a characteristic check (WF-109, {@code spec/IR-SPEC.md} ch. 4.6 (q2);
 * {@code spec/RUNTIME-SEMANTICS.md} ch. 5.8b): does the pattern bound to {@code subject} carry a check
 * hosted on {@code k.eigenaar()}?
 *
 * <p>The answer is EXTENSIONAL, and that is the whole point of this class. The spec reads "an object of
 * type T satisfies host O iff T ⊑ O", so a host naming a SUPERTYPE of the pattern's type is satisfied —
 * an inherited characteristic is read on the object that hosts it (ch. 2.2). Both transpiler branches
 * asked the question by NAME IDENTITY until 2026-09-19 ({@code k.eigenaar() != subject}), which made a
 * legal supertype host — measured: a {@code Sein} doel under a {@code Baken} host — lower to NEITHER
 * pattern and the whole creation refuse itself by name, while the verifier, {@code ref} and the ASP
 * backend all carried the document. One verifier-clean form, two executors answering and two refusing:
 * exactly the split WF-109 exists to close.
 *
 * <p>ONE place for the two branches, on the WF-097 shared-fence discipline: an identity test is one
 * character away from returning and neither branch would notice the other drifting back to it.
 */
public final class Gastheer {
    private Gastheer() {}

    /** Whether {@code subject}'s pattern hosts {@code k} — true when the host is absent (the check is
     *  unconstrained and rides whatever pattern carries the condition) or when {@code subject ⊑ host}. */
    public static boolean hostOp(Conditie.Kenmerk k, Objecttype subject) {
        Objecttype host = k.eigenaar();
        if (host == null) return true;
        return subject != null && subject.keten().contains(host);
    }
}
