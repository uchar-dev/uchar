# jutterswet-wachtdeling

The **counting-quantifier** half of the short-circuit witness
(`RUNTIME-SEMANTICS.md` ch. 5.10). `jutterswet-ploegaandeel` pins the
conjunctive case; this golden pins that the rule is ONE rule over all
five quantifiers, in both directions — a stop that fixes the outcome
**false** (GEEN) and a stop that fixes it **true** (TEN MINSTE).

## What it pins

Two rules over the same two members, in the same wire order, differing
only in the quantifier:

```
Het geenteken van een Wacht moet gesteld worden op 1
indien hij aan geen van de volgende voorwaarden voldoet:
- zijn ploegomvang is gelijk aan 0
- het aandeel is groter dan 0.

Het minsteteken van een Wacht moet gesteld worden op 1
indien hij aan ten minste één van de volgende voorwaarden voldoet:
- zijn ploegomvang is gelijk aan 0
- het aandeel is groter dan 0.

Daarbij geldt (in beide regels):
    het aandeel is de vangstwaarde van de Wacht
        gedeeld door de ploegomvang van de Wacht
        naar beneden afgerond op 0 decimalen.
```

Member 1 is TRUE exactly where member 2 would divide by zero. GEEN then
stops at the first hit (the outcome is false and cannot change); TEN
MINSTE 1 stops at the same hit (the outcome is true and cannot change).
Two different verdicts, one stop — which is the point: the stop follows
from the quantifier's own trigger, never from a special case for ALLE.

Without the short circuit both executors fault on `W1` — measured on
this exact golden on the parent commit, 2026-09-06:

```
ref:    EVALUATION FAULT: division by numeric zero
clingo: UNSATISFIABLE — a fail-fast guard fired:
        viol("division by numeric zero","W1")
```

## Hand-computed absolutes

| Wacht | ploegomvang | vangstwaarde | member 1 | member 2 | `geenteken` | `minsteteken` |
|-------|-------------|--------------|----------|----------|-------------|---------------|
| W1    | 0           | 100          | **true**  | *never read* | **leeg** (hits 1 > 0) | **1** (hits 1 ≥ 1) |
| W2    | 4           | 100          | false    | ⌊100/4⌋ = 25 > 0 → true | **leeg** (hits 1) | **1** (hits 1) |
| W3    | 7           | 3            | false    | ⌊3/7⌋ = 0 > 0 → false | **1** (hits 0) | **leeg** (hits 0) |

`W2` and `W3` are the controls: with member 1 false the composite reads
member 2 and lets it decide, both ways and for both quantifiers. Only
`W1` exercises the stop, and it does so with opposite verdicts in the
two rules.

## The differential square

`ref` ≡ `clingo` over all six expectations (gate 5 ≡ gate 6), anchored to
the hand-written absolutes above; gate 12 replays them in Drools. Gate 10
re-emits `bron.rgs` to a byte-identical `ir.json`. NOOP-REFUTING:
`minsteteken` on `W1`/`W2` and `geenteken` on `W3` are derived and absent
from the raw input, so a do-nothing run mismatches three expectations
(gate 7).
