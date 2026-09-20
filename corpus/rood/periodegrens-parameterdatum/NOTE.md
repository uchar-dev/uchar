# periodegrens-parameterdatum

The half of the period condition `RUNTIME-SEMANTICS.md` ch. 3.10
**defers**. The chapter's scalar gate is built and witnessed green
(`corpus/golden/jutterswet-premievenster`), but its v0 mirror note bounds
what a window bound may BE:

> v0's backends lower only literal or own-scalar-attribute bounds and
> refuse the rest […]; the IR admits any date expression (WF-042).

This document writes the bound WF-042 admits and the mirror note defers:
a window `vanaf de peildatum`, where `peildatum` is a date PARAMETER. The
document is verifier-clean — a date-typed bound is exactly what WF-042
asks for — and the case reaches it, so the refusal is the only outcome
the executors can give.

The two pins run to the **phase tail** on purpose. Both executors refuse
in one and the same sentence up to that tail, and only the tail says
which one spoke: `Declined at evaluation` for `ref`, `Declined in
translation` for the ASP backend. A pin that stopped earlier would pass
unchanged if the two phases were ever swapped, which is the WF-044
sisters' argument applied to this pair.

## All four executors refuse, and only two can carry a fragment

Measured 2026-09-20 on this document:

    verifier            PASS
    ref                 EVALUATION FAULT: … Declined at evaluation
    clingo              TRANSLATION FAULT: … Declined in translation
    Drools exec-model   EXCLUDE transpiler SKIP: regel 'vondstpremie van
                        een vondst' — voorwaarde buiten Phase-B scope
    Drools DRL-tekst    EXCLUDE drl-transpiler MARKER: TODO
                        periodevergelijking-grens 'de peildatum' (§8.4.3)

The agreement is unanimous and the DRL-text branch even names the same
limit in its own words. It is listed in the gate's Drools exclusion list
rather than fragment-pinned because that leg reserves FAULT for a genuine
fault at fire, and a transpiler refusal happens one phase before it.

The day a card carries the general date expression, this directory stops
being red: the window would simply be a window, and the gate's exclusion
list and this note would go stale together — which is what the gate's
stale guards are for.
