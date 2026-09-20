# periodevenster-tijdlijnkenmerk

`RUNTIME-SEMANTICS.md` ch. 3.10 splits on the rule's **target**: a scalar
target gets the evaluation-date gate, a time-dependent one gets the clip —
the window intersects the written timeline instead of deciding whether the
rule applies. The clip is specified and not built, so every executor must
refuse it, and this document is the case that proves the split is asked on
the right table.

The target here is a **characteristic**. A characteristic declares its
timeline in `kenmerkGranulariteit`, not in `attribuutGranulariteit`, and an
executor that asks only the attribute table sees a scalar target where the
document has a timeline. That is not hypothetical: measured 2026-09-20,
before the fix, the ASP backend PLAYED the scalar gate on this very
document — `geborgen` present at an `asOfDate` inside the window and absent
outside it — while both Drools branches named the §8.4.3 clip. `ref`
faulted, but on an unrelated sentence ("temporal writes are not
implemented"), so its refusal was luck rather than the split.

Both OCaml executors now ask the characteristic table where the target is a
characteristic, each through its OWN two-line reader. The condition WALK
they share (`Eval.bevat_periode`) is what made the missing half invisible:
agreement on a shared function is one implementation, not two that agree.

## Four executors, four refusals of the same construct

    verifier            PASS
    ref                 EVALUATION FAULT: … is ch. 3.10's CLIP … Declined at
                        evaluation
    clingo              TRANSLATION FAULT: … is ch. 3.10's CLIP … Declined in
                        translation
    Drools exec-model   EXCLUDE transpiler SKIP: periodevergelijking op
                        tijdsafhankelijk doel — §8.4.3 clip
    Drools DRL-tekst    EXCLUDE drl-transpiler MARKER: TODO
                        periodevergelijking op een tijdsafhankelijk doel
                        (§8.4.3)

The pins run to the **phase tail** — `Declined at evaluation` against
`Declined in translation` — so a swap of the two phases fails here instead
of passing unseen. The Drools branches are listed in the gate's exclusion
list rather than fragment-pinned: that leg reserves FAULT for a genuine
fault at fire, and a transpiler refusal is one phase earlier.

When the clip lands (`backlog/new/20260920-tijdvenster-conditie-normeren.md`)
this directory becomes a golden with a written timeline, and the gate's
stale guards say so.
