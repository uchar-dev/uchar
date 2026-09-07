# jutterswet-klipwacht

The **clip-as-operand witness** of `RUNTIME-SEMANTICS.md` ch. 5.10.

Boundary 2 of ch. 5.10 says the `gedurende` clip's **own members** stay
fully evaluated — a clip folds period sets, and an intersection has no
moment at which "the outcome stands". That is about the clip's insides.
It says nothing about the clip as an **operand**: a totalled clip is an
expression, so it can sit in a comparison that is itself a member of an
ordinary composite, and there the short circuit applies like anywhere
else. Two fences on the clip path missed that distinction.

## What it pins

```
Het klipteken van een Kustwacht moet gesteld worden op 1
indien hij aan alle volgende voorwaarden voldoet:
- zijn ploegomvang is ongelijk aan 0
- (het totaal van zijn wachtvergoeding per maand
   gedurende de tijd dat zijn wachtvergoeding per maand groter is dan 10)
  is groter dan 0.
```

Member 1 fails on `K1`, so member 2 is never read — and member 2 carries
a timeline whose values trip the segment range fence. Measured on the
parent commit (2026-09-07), from this very golden:

```
ref    : oracle: PASS (3 expectations)
clingo : UNSATISFIABLE — a fail-fast guard fired:
         viol("arithmetic range exceeded (clingo grounds in machine integers)","K1")
```

Two fences were at fault, both keyed on the clip helper's own free `S`
rather than on the rule subject, and both without the prefix: the clip
**comparison** fence and the clip **segment** fence (`|W| > 46340`). The
second only surfaced because this witness exists — the first was found by
the lens, the second by dumping this golden's program and reading the
remaining unguarded rules.

## Hand-computed absolutes

| Kustwacht | ploegomvang | `wachtvergoeding per maand` (2023) | member 1 | member 2 | `klipteken` |
|-----------|-------------|-------------------------------------|----------|----------|-------------|
| K1 | 0 | 30 000 000 / maand | **false** | *never read* | **leeg** |
| K2 | 4 | 100 / maand | true | total over the 12 clipped months > 0 → true | **1** |
| K3 | 4 | 5 / maand | true | 5 > 10 nowhere, so the clip is empty and totals **0**; 0 > 0 → false | **leeg** |

`K2` and `K3` are the controls, and `K3` is the sharper of the two: it
proves member 2 is genuinely read and genuinely decides, because its
`leeg` comes from the clip totalling **zero** rather than from a stop.
Without it, `K1`'s `leeg` could be mistaken for a composite that never
evaluates its second member at all.

## The differential square

`ref` ≡ `clingo` over all three expectations (gate 5 ≡ gate 6), anchored
to the hand-written absolutes above. Gate 10 re-emits `bron.rgs` to a
byte-identical `ir.json`. NOOP-REFUTING: `K2`'s `klipteken` is derived and
absent from the raw input (gate 7). The Drools backend's channel status
for this golden is pinned by gate 12 as for every other.
