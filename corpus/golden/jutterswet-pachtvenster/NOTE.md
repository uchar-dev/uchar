# jutterswet-pachtvenster

The witness for `RUNTIME-SEMANTICS.md` ch. 3.10's **second** case: a
period condition on a TEMPORAL target. Here the window does not decide
whether the rule applies. It cuts the timeline the rule writes:

    written(tl, window) = tl restricted to [vanaf, tot)

A `Strandpachter` has a yearly `pachttarief`, two month-granular temporal
characteristics (`vergund`, `aanwezig`) and a scalar `ingangsdatum`. There
are three temporal rules. In each one the window is the rule's whole
condition, and none of them has a validity window. That combination
belongs to ch. 8's single-axis decision, which is OPEN (ch. 3.12).

| rule | producer | window |
|---|---|---|
| `jaarpacht` (year) | pro-rata per year of the tarief while `vergund` | `vanaf 1 januari 2025` |
| `maandpacht` (month, 2 dec.) | pro-rata per month, likewise | `van 1 maart 2025 tot en met 30 april 2025` |
| `zomerdagen` (month) | days in the month while `aanwezig` | `vanaf de ingangsdatum tot 1 september 2025` |

The expectations were computed by hand before any executor saw the
document. The computation is in
`backlog/done/20260920-tijdvenster-conditie-normeren.md`.

## What each cell pins

`case1-voorbeeld` has **no evaluation date**.

- `jaarpacht`: unclipped {2024: 10, 2025: 12}, written {2025: 12}. This is
  the chapter's worked example verbatim.
- `maandpacht`: 24 monthly segments unclipped, March and April 2025
  written. "tot en met 30 april" ends the window on 1 May.
- `zomerdagen`: June 11, July 31, August 31, September 9 unclipped. The
  subject's own `ingangsdatum` (1 July) opens the window and 1 September
  closes it, so only July and August remain.

The rule writes although no evaluation date is set. The scalar gate's
clause "an unset evaluation date never matches" is about the evaluation
moment, and the clip has no counterpart to it.

`case2-lege-en-gemiste-vensters` has evaluation date **2025-03-20**, inside
`maandpacht`'s window.

- P2 has no `ingangsdatum`, so `zomerdagen` is empty. A leeg bound puts no
  day in the window; unclipped the value would be June 30. Its
  `jaarpacht` (105, from 105 vergund days of 365 at a tarief of 365) and
  `maandpacht` (365 for March, 182.5 for half of April) are written as
  in case 1.
- P3's timelines all lie in 2024 (and its `aanwezig` in June 2025), and
  its `ingangsdatum` 1 October comes after the window's `tot`. All three
  targets are empty. A scalar gate would have fired `maandpacht` on
  2025-03-20 and written P3's twelve 2024 months. The clip writes none.

`case3-evaluatiedatum-buiten-elk-venster` has case 1's input and
evaluation date **2023-06-01**, before every window. Hand computation: the
clip does not look at the evaluation date, so the written timelines are
case 1's, cell for cell: `jaarpacht` {2025: 12}, `maandpacht` March and
April 12, `zomerdagen` July and August 31. A scalar gate on 2023-06-01 would
have written none of the three. Case 1 has no evaluation date and case 3
has one outside every window, so between them they pin "the evaluation date
plays no part" from both sides. Added 2026-09-23 after a lens finding:
without it, an ASP translation that gated the clipped rule on a SET
evaluation date stayed green in both golden and suite.

## What the corpus cannot carry

The clip cuts SEGMENTS and keeps their VALUES: a bound inside a bucket
leaves the bucket's value on the part inside the window. The case format
cannot state such an expectation, because
`spec/schema/uchar-case-v0.schema.json` pins every timeline value, expected
ones included, to the declaration's granularity grid. So every bound in
this document lies on the grid. Each OCaml executor's own suite pins the
clause with a bound in mid-March: `ref` as
`WV-3.10-clip-knipt-segment-houdt-waarde` (and ch. 3.10's day-count
sentence verbatim, `WV-3.10-clip-dagtelling-31-gesneden-op-de-vijfde`), the
ASP backend by reading the `tl/5` atoms from its stable model. Whether a
cut bucket may ever become a corpus expectation is OPEN (ch. 3.12): it
needs the case schema to release expected timelines from the grid.

## Executors

`ref` and the ASP backend replay all three cases (4, 7 and 4
expectations). Both
Drools branches refuse the clip by name before fire. The exec-model branch
reports `transpiler SKIP … periodevergelijking op tijdsafhankelijk doel —
§8.4.3 clip`, the DRL-text branch `MARKER … TODO periodevergelijking op een
tijdsafhankelijk doel (§8.4.3)`. Gate 12 therefore pins the golden in both
channel lists, and neither played count moves.
