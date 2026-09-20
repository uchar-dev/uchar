# jutterswet-premievenster

The **period condition** (`IR-SPEC.md` ch. 4.5 `TimeWindow`, v0 wire node
`Periode`) on a SCALAR target: the evaluation-date gate of
`RUNTIME-SEMANTICS.md` ch. 3.10. This is that chapter's first witness —
until 2026-09-20 no corpus document executed the construct at all, and
the two OCaml executors refused it while the chapter specified it in six
clauses with a worked example.

Article 14 of the Jutterswet, six rules on the Vondst. Five of them carry
a window; the sixth, `basispremie`, carries none and is the control: it
fires in every case, which is what shows the window gates its own rule
and nothing else.

| attribute | value | window |
|---|---|---|
| `basispremie` | 1 | none (control) |
| `vondstpremie` | 50 | `van 2024-01-01 tot 2025-01-01` — `tot` exclusive |
| `naseizoenpremie` | 20 | `van 2024-01-01 tot en met 2024-12-31` — inclusive |
| `overgangspremie` | 30 | `vanaf 2025-01-01` — open above |
| `aanlooppremie` | 10 | `tot 2024-01-01` — open below |
| `bergloon` | 5 | `vanaf de meldatum van de Vondst` — an own scalar bound |
| `borgpremie` | 15 | an `ALLE` composite: `geborgen` **and** `van 2024-01-01 tot 2025-01-01` |

The windows are the chapter's own: [2024-01-01, 2025-01-01) with the
boundary dates 2024-12-31 and 2025-01-01, and the "tot en met 2024-12-31"
spelling beside it.

## Which case pins which clause

Two Vondsten throughout: V1 carries `meldatum` 2024-03-01, V2 carries no
meldatum at all.

| clause (ch. 3.10) | where it is pinned |
|---|---|
| `vanaf` inclusive | `case2-op-de-vanafgrens` — `vondstpremie` is 50 **on** 2024-01-01 |
| `tot` exclusive | `case4-op-de-totgrens` — `vondstpremie` is empty **on** 2025-01-01 (the worked example's "does not") |
| "tot en met" inclusive | `case3-laatste-dag` — `naseizoenpremie` is 20 on 2024-12-31 (the worked example's third sentence) |
| an absent bound is open | `overgangspremie` in `case4`, `aanlooppremie` in `case5` |
| an unset evaluation date never matches | `case6-tijdvrij` — `asOfDate` null, every window empty, only the control fires |
| a leeg bound never matches | V2's `bergloon`, empty in all six cases, while V1's fires in four |

`case1-binnen-venster` is the ordinary interior reading, and
`case5-voor-het-venster` sits before every window so the open-below one is
the only rule that fires.

`borgpremie` carries the window at the OTHER wire position: as a member of
an `ALLE` composite beside a characteristic check. V1 is `geborgen` and V2
is not, so V2's `borgpremie` is empty in all six cases without the window
ever being read — `ref` stops an `ALLE` at the first false member — while
V1's follows the window exactly. Until 2026-09-20 that shape lived only in
`corpus/golden/jutterswet-vormenkabinet`, as a construct no executor
carried; it is here because a witness cabinet is scaffolding and this is
where the construct executes.

The expectations were computed by hand and written into the card
(`backlog/done/20260920-ref-periodeconditie-arm.md`) before any executor
saw the document. All four executors replay them: `ref` and the ASP
backend 14 expectations per case, both Drools branches `PLAYED 6`.

## The bound shapes this document carries, and the one it does not

ch. 3.10's v0 mirror note bounds what a window bound may BE — a date
literal or the subject's own scalar date attribute — while WF-042 admits
any date expression. Both bound shapes are carried here (four literal
windows, one own-attribute window). The deferred general expression is
refused by name in both OCaml executors and witnessed red, in
`corpus/rood/periodegrens-parameterdatum`.

The chapter's SECOND case — a period condition on a TEMPORAL target,
where the window clips the written timeline instead of gating the rule —
is not built. Both executors refuse it by name at the rule, before the
window is read, because the case distinction is on the target and a
condition cannot see one. Each executor's own suite carries that refusal.
