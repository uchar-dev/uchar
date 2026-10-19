# at-most-reads-until-settled

The **too-eager guard** tripwire for `at most` (`RUNTIME-SEMANTICS.md`
ch. 5.10).

Every green witness in this corpus pins that a member the outcome has
already settled away is **not** read. This case pins the other half: that
a member the outcome has **not** settled away **is** read. Without it,
a stop that fires one member too early passes every gate.

## Why only a RED case can hold this direction

A composite's members are ordinary joins, so making the guard *stricter*
removes nothing from the verdict — the truth value is computed from the
full member count either way. All a too-eager guard removes is **fault
surface**. A green golden therefore cannot see it: its expectations still
match. Only a case whose pinned outcome IS a fault can, because that is
the one observation a suppressed member destroys.

This became load-bearing when its predecessor was retired. The red case
`atmost-no-vacuous-true` used to guard this direction as a side effect;
the maintainer decision of 2026-09-07 turned that document into a
positive outcome (`corpus/golden/jutterswet-vacuumwacht`), and the
too-eager direction lost its only witness with it.

## What it pins

```
Het stopteken van een Vacuumwacht moet gesteld worden op 1
indien hij aan ten hoogste twee van de volgende voorwaarden voldoet:
- zijn ploegomvang is groter dan 100
- het aandeel is groter dan 0            <- de deling, op lid 2
- zijn vangstwaarde is groter dan 100000
- zijn ploegomvang is gelijk aan 0.
```

*(illustrative rendering; this directory carries `ir.json` only, as the
red corpus does throughout.)*

With `ploegomvang` = 0 and `vangstwaarde` = 100: member 1 fails, so after
one member *h* = 0 with *r* = 3. The at-most stop needs *h* > 2 (no) or
*h* + *r* ≤ 2 (0 + 3 = 3, no). **The outcome does not stand**, so member 2
is read — and it divides 100 by 0.

| executor | pinned |
|----------|--------|
| ref | `division by numeric zero` |
| clingo | `division by numeric zero` |
| Drools (exec-model) | `deling door 0` |

The division sits on member **2**, not member 1, on purpose: member 1 is
never guarded away by anything, so a division there would fault under any
guard whatsoever and pin nothing. Member 2 is the first position at which
a one-off guard can silence a member the norm reads.

## The mutant it kills

`at most`'s prefix guard is `H ≤ d` **and** `H ≥ d − r + 1`. Shifting
that `+1` to `+2` makes the guard fire one member early — it reads
strictly less than ch. 5.10 does and therefore answers where the norm
faults. Measured: that mutant survives all thirteen gates without this
case, and fails gate 8 with it.
