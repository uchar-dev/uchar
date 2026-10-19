# jutterswet-ploegaandeel

The **short-circuit witness** of the composite condition
(`RUNTIME-SEMANTICS.md` ch. 5.10): the conjunctive quantifier evaluates
its members left to right in wire order and stops at the first member
after which the outcome stands, so an earlier member can GATE a later
one that would otherwise fault.

## What it pins

```
Het bergingsaandeel van een Jutter moet gesteld worden op 1
indien hij aan alle volgende voorwaarden voldoet:
- zijn ploegomvang is ongelijk aan 0
- het aandeel is groter dan 0.
Daarbij geldt:
    het aandeel is de vangstwaarde van de Jutter
        gedeeld door de ploegomvang van de Jutter
        naar beneden afgerond op 0 decimalen.
```

Member 1 tests the divisor against zero; member 2 divides by that very
divisor. This is the guard-a-division-by-an-earlier-condition shape the
RegelSpraak specification states as a generic rule (§11.1: conditions are
checked left to right, and variables are not computed unnecessarily);
composites are §10.2/§11.1's own construct.

Without the short circuit BOTH executors fault on `J1` — measured on
this exact golden on the parent commit, 2026-09-06:

```
ref:    EVALUATION FAULT: division by numeric zero
clingo: UNSATISFIABLE — a fail-fast guard fired:
        viol("division by numeric zero","J1")
```

With it, member 2 is never read on `J1`: no fault, no §9.5 channel fact,
the rule simply does not fire.

## Hand-computed absolutes

| Jutter | ploegomvang | vangstwaarde | member 1 | member 2 | `bergingsaandeel` |
|--------|-------------|--------------|----------|----------|-------------------|
| J1     | 0           | 100          | **false** | *never read* | **leeg** |
| J2     | 4           | 100          | true     | ⌊100/4⌋ = 25 > 0 → true | **1** |
| J3     | 7           | 3            | true     | ⌊3/7⌋ = 0 > 0 → false | **leeg** |

`J2` and `J3` are the controls that keep the witness honest: they prove
member 2 IS read whenever member 1 holds, and that it decides the
outcome both ways. `J1`'s leeg is therefore the short circuit, not a
composite that stopped evaluating altogether.

## The differential square

`ref` ≡ `clingo` over all three expectations (gate 5 ≡ gate 6), anchored
to the hand-written absolutes above; gate 12 replays them in Drools.
Gate 10 re-emits `source.rgs` to a byte-identical `ir.json`.
NOOP-REFUTING: `J2`'s `bergingsaandeel` is derived and absent from the
raw input, so a do-nothing run mismatches it (gate 7) — the case does
not ride on the two leeg expectations alone.
