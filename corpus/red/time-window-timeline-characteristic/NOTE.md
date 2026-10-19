# time-window-timeline-characteristic

`RUNTIME-SEMANTICS.md` ch. 3.10 decides on the rule's **target**. A scalar
target gets the evaluation-date gate. A time-dependent target gets the
clip: the window cuts the written timeline instead of deciding whether the
rule applies. Since 2026-09-23 the clip is built for ATTRIBUTE targets
(golden `corpus/golden/jutterswet-pachtvenster`). This document is the
position that stays refused, and the case that shows the decision is made
from the right table.

The target here is a **characteristic**. A characteristic declares its
timeline on its own `Characteristic` node (`timeline`), not on an
`Attribute`, and an executor that asks only the attribute table sees a
scalar target where the document has a timeline. That happened: on
2026-09-20, before the fix, the ASP backend PLAYED the scalar gate on this
very document (`geborgen` present at an `asOfDate` inside the window,
absent outside it), while both Drools branches named the §8.4.3 clip.

The clip cannot be built here. Neither OCaml executor writes a temporal
characteristic (ch. 3.11), so the timeline the window would cut is never
produced. Both refuse at the rule, before the window is read. Measured on
2026-09-23 on this document with the window removed: `ref` refused the
temporal characteristic write, but the ASP backend wrote a scalar
`heeft/2` fact and passed. It refuses by name since that day, in `ref`'s
words, and its own suite pins it.

## Four executors, four refusals of the same construct

    verifier            PASS
    ref                 EVALUATION FAULT: … is ch. 3.10's CLIP over a
                        characteristic timeline … Declined at evaluation
    clingo              TRANSLATION FAULT: … is ch. 3.10's CLIP over a
                        characteristic timeline … Declined in translation
    Drools, both        EXCLUDE transpiler SKIP: regel: berging van een
    branches            vondst — het tijdsafhankelijke kenmerk 'geborgen'
                        van Vondst als doel: deze tak draagt deze vorm nog
                        niet; … (RUNTIME-SEMANTICS ch. 3.11)

Both pins run to the **phase tail**, `Declined at evaluation` against
`Declined in translation`, so a swap of the two phases fails here and
cannot pass unseen. The Drools branches are in the gate's exclusion list,
not fragment-pinned: the transpiler raises the refusal as an Excluded,
which the red run reports as EXCLUDE, and that leg pins a fragment only
on a FAULT. The Drools row is measured 2026-10-14: since card
`drl-teksttak-op-truth-maintenance` both branches render one analysis and
refuse in one sentence, at the target, one step before the window. The
destructive lowerings refused at the clip: the exec-model branch until card
`tms-omschakeling`, "periodevergelijking op tijdsafhankelijk doel — §8.4.3
clip", and the DRL-text branch until card
`drl-teksttak-op-truth-maintenance`, "drl-transpiler MARKER: TODO
periodevergelijking op een tijdsafhankelijk doel (§8.4.3)".

This directory becomes a golden when temporal characteristic writes land
(ch. 3.11). The gate's stale guards will report it then.
