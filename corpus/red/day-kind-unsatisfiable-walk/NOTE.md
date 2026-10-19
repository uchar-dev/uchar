# day-kind-unsatisfiable-walk

The **reach fence** of the day-kind walk (`RUNTIME-SEMANTICS.md` ch. 3.8): a
step that passes 40 000 consecutive calendar days — a little over 109 years —
without finding a day of the kind faults, instead of walking for ever.

## The shape

A day kind `nevelsdag` defined as `de maand uit de dag gelijk is aan 13` — a
month number no calendar day has, so the predicate is **false everywhere**.
The document's one rule steps forward by one such day:

```
De vervaldatum van een Baken moet berekend worden als zijn peildatum plus
zijn termijn.          (termijn: 1, in nevelsdagen)
```

## Why a RED case, not a golden

There is no answer. "The first nevelsdag after 10 March 2026" denotes
nothing, and the three readings that would let it pass quietly are all worse
than a fault: leeg (which would make an unsatisfiable definition look like a
missing input), the start date itself (which §6.11's zero rule reserves for
N = 0), and a walk with no end. The count form has no such problem and is not
here: `tijdsduur … in hele nevelsdagen` over an unsatisfiable day kind is
**0**, pinned as a worked example in `ref/`'s own suite.

## What each executor pins

| executor | route | pinned fragment |
|---|---|---|
| `ref` | walks, counting the gap; faults when it reaches the reach | `no day of the kind within 40000 calendar days` |
| clingo | reads the fence off the day kind's EXTENSION — no matching day within a reach of the start — and derives the fault atom | same |
| Drools | walks in `RegelSpraakRuntime.rsDatumPlusDagsoort`, `IllegalStateException` at fire | same |

All three carry it, which is why this red case pins `expectedFault.drools`
too — seven of the red zone's cases do.

**The Drools fence moved with this card.** The ported production runtime
bounded the walk at 4 000 000 TOTAL steps, not at a gap, under the message
`onvervulbare dagsoortdefinitie?`. Two things were wrong with that as a
uchar fence: it refused a legitimate large N (`datum plus 5 000 werkdagen`
walks some nineteen years) while blaming the definition, and no other
executor shared it. It is now the same reach fence, in the same words — the
backend implementing the uchar spec beyond the ported semantics, as it
already does for the ch. 7 prioritizedDefaults lowering.

## Why 40 000, and what the fault does NOT say

A little over 109 calendar years. The number has to clear two bars. The
longest recurrence gap a Gregorian day kind can have is the century-skipped
leap day — 2096-02-29 to 2104-02-29, **2 921** days — but a day kind may also
be bound to a YEAR or to a PARAMETER ("the 1st or the 15th of 2040"), and
such a kind is verifier-clean, perfectly satisfiable, and may lie decades
past any date the case carries. A ten-year reach refused those; 40 000 days
does not. `ref`'s own suite pins both: the leap-day step across the century,
and the 2040 kind stepped from 2026.

The fault text says the walk **reaches no further**. It does not say the
definition is unsatisfiable, and this case is the reason the wording matters:
a reader who takes the message as a diagnosis will look for a defect in a
definition that may have none. The residual cost is accepted and stated in
ch. 3.8 — a day kind whose next occurrence lies beyond the reach is refused
although it exists. What the fence buys is termination, nothing more.
