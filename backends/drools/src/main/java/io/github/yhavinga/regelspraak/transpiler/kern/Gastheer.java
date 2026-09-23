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

    /** The reason both branches give when a creation guard carrying a whole-period qualifier declines on the anker. */
    public static final String DOELPOGING_GEHEELPERIODE =
            "voorwaarde niet naar anker of doel te lowereren — gehele-periodekwalificatie: alleen de anker is gebonden (WF-109)";

    /** Whether a §13.4.7 creation guard may be tried on the DOEL at all. Not when it carries a §8.2 whole-period
     *  qualifier anywhere: WF-109's third scope fence binds only the anker there, so a doel lowering answers a
     *  question the document did not ask. Measured 2026-09-23 on verifier-clean documents that reach the doel
     *  attempt: the exec branch read the doel where ref and clingo read the anker, and the DRL-text branch — whose
     *  whole-period lowering is wired to {@code $subject}, the anker inside the {@code $doel} pattern — either sank
     *  the whole package at compile or read the anker from the doel's pattern. */
    public static boolean doelpogingToegestaan(Conditie c) {
        return !draagtGeheelPeriode(c);
    }

    /** The reason both branches give when a creation guard names the doel as host where WF-109 does not bind it. */
    public static final String DOELPOGING_GENEST =
            "voorwaarde niet naar anker of doel te lowereren — doelgastheer binnen een samengestelde of genavigeerde"
            + " voorwaarde: alleen de anker is gebonden (WF-109)";

    /** WF-109's first two doel fences: the doel is a bound object only where the check hosted on it IS the whole
     *  guard and does not navigate (the third fence, the whole-period qualifier, is {@link #doelpogingToegestaan}'s).
     *  True when the guard names a host the doel satisfies anywhere else — inside a composite at any depth, or on a
     *  navigating check. Measured 2026-09-23: a composite whose checks were all hosted on the doel's supertype was
     *  refused by the verifier, {@code ref}, clingo and the exec-model branch, and PLAYED by the DRL-text branch,
     *  which inlines a composite on the doel pattern. */
    public static boolean doelgastheerBuitenBereik(Conditie guard, Objecttype doel) {
        if (guard instanceof Conditie.Kenmerk k && k.navigatie() == null) return false;
        return noemtDoelgastheer(guard, doel);
    }

    private static boolean noemtDoelgastheer(Conditie c, Objecttype doel) {
        if (c instanceof Conditie.Kenmerk k) return k.eigenaar() != null && hostOp(k, doel);
        if (c instanceof Conditie.Samengesteld s) return s.onderdelen().stream().anyMatch(d -> noemtDoelgastheer(d, doel));
        return false;
    }

    private static boolean draagtGeheelPeriode(Conditie c) {
        if (c instanceof Conditie.Vergelijking v) return v.periode() != null;
        if (c instanceof Conditie.Predicaat p) return p.periode() != null;
        if (c instanceof Conditie.Kenmerk k) return k.periode() != null;
        if (c instanceof Conditie.Samengesteld s) return s.onderdelen().stream().anyMatch(Gastheer::draagtGeheelPeriode);
        if (c instanceof Conditie.Kwantorvouw kv) return draagtGeheelPeriode(kv.filter()) || draagtGeheelPeriode(kv.toets());
        return false;
    }
}
