# dagsoort-onvervulbare-wandeling

The **gap fence** of the day-kind walk (`RUNTIME-SEMANTICS.md` ch. 3.8): a
step that passes 3 660 consecutive calendar days without finding a day of the
kind faults, instead of walking for ever.

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
| `ref` | walks, counting the gap; faults when it reaches the fence | `no day of the kind within 3660 calendar days` |
| clingo | GROUNDS the walk over `[D+1, D+N·3660]` and derives the fence atom | same |
| Drools | walks in `RegelSpraakRuntime.rsDatumPlusDagsoort`, `IllegalStateException` at fire | same |

All three carry it, which is why this red case pins `expectedFault.drools`
too — seven of the red zone's cases do.

**The Drools fence moved with this card.** The ported production runtime
bounded the walk at 4 000 000 TOTAL steps, not at a gap, under the message
`onvervulbare dagsoortdefinitie?`. Two things were wrong with that as a
uchar fence: it refused a legitimate large N (`datum plus 5 000 werkdagen`
walks some nineteen years) while blaming the definition, and no other
executor shared it. It is now the same gap fence, in the same words — the
backend implementing the uchar spec beyond the ported semantics, as it
already does for the ch. 7 prioritizedDefaults lowering.

## Why 3 660

Ten calendar years. It has to exceed the longest recurrence gap a Gregorian
day kind can have — the century-skipped leap day, 2096-02-29 to 2104-02-29,
which is **2 921** days — and stay low enough for an executor that GROUNDS
the walk to materialise it. `ref`'s own suite pins both ends: the leap-day
step across the century still succeeds, and the difference it walks is
measured at 2 921.
