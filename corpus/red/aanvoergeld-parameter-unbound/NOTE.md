# aanvoergeld-parameter-unbound

A parameter the case does **not bind**, read once in a condition and once in
a value. Binding is fail-fast (`RUNTIME-SEMANTICS.md` ch. 5.7): the read
faults, in every executor that reaches it, in one sentence.

## The shape

`corpus/golden/jutterswet-aanvoergeld`'s document verbatim (`ir.json` is a
byte copy): two unit-bearing parameters, each read at one of the two
positions a parameter reference occupies.

```
Een Jutter is aanvoerplichtig
indien zijn aangevoerd gewicht groter of gelijk is aan de aanvoerdrempel.   <- condition

Het aanvoergeld van een Jutter moet berekend worden als
    zijn aanvoerwaarde plus het aanvoertarief.                              <- value
```

The three Jutters are the golden's (J1 640,25 kg / 310,00 €, J2 499,99 kg /
87,40 €, J3 500,00 kg / 0,00 €). Each case binds one parameter and leaves the
other out:

| case | bound | unbound | the read that faults |
|------|-------|---------|----------------------|
| `case1-tarief-ongebonden` | aanvoerdrempel 500,00 kg | aanvoertarief | the value `aanvoerwaarde plus aanvoertarief` |
| `case2-drempel-ongebonden` | aanvoertarief 12,50 € | aanvoerdrempel | the condition `aangevoerd gewicht ≥ aanvoerdrempel` |

## Why each case faults (by hand)

Neither rule has a condition that could keep evaluation away from the read,
and no Let, branch or composite stands in front of it (ch. 5.11), so the read
is reached for every Jutter:

* case1: the aanvoergeld rule is unconditional, and `plus` evaluates its left
  operand (310,00, 87,40, 0,00 — all filled) and then the parameter. The
  first Jutter evaluated faults: `parameter "aanvoertarief" is unbound in
  this case (binding is fail-fast, ch. 5.7)`.
* case2: the aanvoerplichtig rule's condition compares each Jutter's weight
  (filled for all three) with the parameter: `parameter "aanvoerdrempel" is
  unbound in this case (binding is fail-fast, ch. 5.7)`. The aanvoergeld rule
  would play (its parameter is bound), but a fault fails the evaluation.

Both OCaml executors pin that sentence. `ref` raises it as an evaluation
fault; the ASP backend's shared fail-fast guard fires for each of J1, J2 and
J3 and the program is unsatisfiable, the diagnosed witness carrying the same
words.

## What it caught

Measured before card `clingo-parameter-failfast` (2026-09-23), with the
golden's own expectations as the yardstick:

| case | ASP backend before | Drools (both measured the same day) |
|------|--------------------|-------------------------------------|
| case1 | aanvoergeld 310,00 / 87,40 / 0,00 — the absent tarief coalesced to 0 | the same three amounts |
| case2 | aanvoerplichtig absent on J1 and J3 — the comparison failed silently | the same |

Definite answers where the arbiter has none. The ASP backend now refuses
both, in ref's words. The Drools backend still runs both cases to a model
(`NOFAULT`), so it is listed by name in gate 8's Drools exclusion list with
that reason rather than fragment-pinned.

## The green twin

`corpus/golden/jutterswet-aanvoergeld` binds both parameters and pins the
seven absolute values (322,50 / 99,90 / 12,50; J1 and J3 aanvoerplichtig,
J2 not) across ref, clingo and Drools.
