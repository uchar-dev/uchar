# at-most-reads-until-settled

The **too-eager guard** witness for `at most` (`RUNTIME-SEMANTICS.md`
ch. 5.10).

Every other witness of the trigger table pins that a member the outcome
has already settled away is **not** read. This case pins the other half:
that a member the outcome has **not** settled away **is** read. Without
it, a stop that fires one member too early passes every gate.

## Why it needs an observable of the member itself

A composite's members are ordinary joins, so making the guard *stricter*
removes nothing from the verdict — the truth value is computed from the
full member count either way. All a too-eager guard removes is what the
skipped member does besides holding or not: a fault or a §9.5 fact. Until
2026-10-19 that was a fault and this was a red case; since the card
`zero-division-channel` (ch. 1.4 point 4) a division by a numeric zero
deposits a fact of kind EF-005, and this is a golden that pins the fact.

## What it pins

```
Het stopteken van een Vacuumwacht moet gesteld worden op 1
indien hij aan ten hoogste twee van de volgende voorwaarden voldoet:
- zijn ploegomvang is groter dan 100
- het aandeel is groter dan 0            <- de deling, op lid 2
- zijn vangstwaarde is groter dan 100000
- zijn ploegomvang is gelijk aan 0.
```

*(illustrative rendering; this directory carries `ir.json` only.)*

With `ploegomvang` = 0 and `vangstwaarde` = 100: member 1 fails, so after
one member *h* = 0 with *r* = 3. The at-most stop needs *h* > 2 (no) or
*h* + *r* ≤ 2 (0 + 3 = 3, no). **The outcome does not stand**, so member 2
is read — and it divides 100 by 0: leeg, which does not hold, and the
deposit. Then *h* = 0 with *r* = 2, so *h* + *r* ≤ 2 and the outcome stands
true. Pinned: `stopteken` 1 and `stopteken van een vacuumwacht × B1` with
code EF-005.

The division sits on member **2**, not member 1, on purpose: member 1 is
never guarded away by anything, so a division there would deposit under any
guard whatsoever and pin nothing. Member 2 is the first position at which a
one-off guard can silence a member the norm reads.

## The mutant it kills

`at most`'s prefix guard is `H ≤ d` **and** `H ≥ d − r + 1`. Shifting
that `+1` to `+2` makes the guard fire one member early — it reads
strictly less than ch. 5.10 does. Measured when this was a red case: that
mutant survived all thirteen gates without this case, and failed gate 8
with it. As a golden the case asks the fact that the mutant does not make
(not measured again).
