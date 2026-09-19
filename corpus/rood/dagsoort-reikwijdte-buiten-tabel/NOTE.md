# dagsoort-reikwijdte-buiten-tabel

The reach fence again, from the side that **only an executor which
materialises the day kind can fall through** — and it did. This case is the
witness of a defect found by review (F4, 2026-09-19) and is pinned here so the
hole cannot be reopened.

## The shape

A day kind with **exactly one** matching day in the whole window:

```
Een dag is een springtijdag
indien er aan alle volgende voorwaarden wordt voldaan:
- het jaar uit de dag gelijk is aan 2026
- de maand uit de dag gelijk is aan 12
- de dag uit de dag gelijk is aan 1.
```

The step asks for **three** such days from 2026-03-15. The first is
2026-12-01; there is no second, so the walk runs out and the reach fence must
fire. The case also carries dates in **1900** and **2200**, and that is not
decoration: it pushes the materialised window so far past the single match
that the extension is demonstrably **not truncated** at its upper edge.

## What was wrong, measured

| executor | before the fix | after |
|---|---|---|
| verifier | PASS | PASS |
| `ref` | `EVALUATION FAULT: no day of the kind within 40000 calendar days …` | unchanged |
| Drools | `IllegalStateException` with the same sentence | unchanged |
| clingo | **`PASS` — the attribute silently read leeg** | the same fault as the other two |

The ASP backend derives the answer by RANK over the day kind's extension. It
guarded two things: an operand outside the materialised window, and a missing
answer rank in a table that IS truncated at that edge. A missing rank in a
table that is **not** truncated fell between them: no guard fired, the result
atom was simply never derived, and an underived value reads as leeg. A step
beyond the reach answered leeg instead of faulting — which is exactly what
ch. 3.8's "the refusal is a FAULT and never a value" forbids.

The fix is the sentence that was missing: **a missing rank in a table that is
not truncated IS the reach fault.** The table then holds every matching day
any walk could have reached, so a rank it does not hold is a rank no walk
reaches either. `ref` and Drools were never wrong here — they walk, and a
walk cannot fall between two guards it does not have.

## Why a RED case

Because there is no answer, and because the three executors must agree on
that. All three now fault with the same sentence, so all three fields of
`expectedFault` are pinned — the second red case of this card able to pin
Drools too.
