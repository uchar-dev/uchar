# jutterswet-seinwacht

*Since 2026-10-19 (card `zero-division-channel`) a filled dividend divided by
a numeric zero is leeg with a §9.5 fact of kind EF-005, no fault
(`RUNTIME-SEMANTICS.md` ch. 1.4 point 4). The case pins `consistent` since: an
unread division is the absence of that fact where it was the absence of a
fault. The text below speaks of the fault.*

The **trigger-row witness** of `RUNTIME-SEMANTICS.md` ch. 5.10. The two
older short-circuit goldens pin that the stop happens; this one pins
*which* trigger causes it, for the three rows that had no artifact of
their own — and for the fact that the triggers are tested **before**
each member rather than after it.

A stop is observable only as a fault that does **not** happen. So every
rule below puts a member that divides by zero *after* the stopping
point: the pinned outcome is `leeg` **and** the absence of a fault, and
an executor that moves the row reaches the division and dies.

## What it pins

All three rules share the same variable part:

```
Daarbij geldt:
    het aandeel is de vangstwaarde van de Seinwacht
        gedeeld door de ploegomvang van de Seinwacht
        naar beneden afgerond op 0 decimalen.
```

| rule | quantifier | members (wire order) | trigger row pinned |
|------|------------|----------------------|--------------------|
| `hoogsteteken` | `atMost 1` | ploegomvang = 0 · vangstwaarde > 0 · **aandeel > 0** | `atMost`, threshold exceeded |
| `preciesteken` | `exactly 1` | ploegomvang = 0 · vangstwaarde > 0 · **aandeel > 0** | `exactly`, threshold exceeded |
| `minsteteken` | `atLeast 3` | **aandeel > 0** · ploegomvang = 0 | `atLeast`, out of reach — and the *before*-the-member ordering |
| `randteken` | `exactly 3` | **aandeel > 0** · ploegomvang = 0 | `exactly`, out of reach, at the boundary *d* = *n* + 1 |

`minsteteken` is the degenerate case ch. 5.10 spells out: a threshold of
**3 over 2 members** can never be reached, so the out-of-reach trigger
fires on the **empty prefix** and *no member is read at all*. The
faulting member is deliberately member **1**, which is what makes the
ordering observable: a checker that evaluates a member and asks
afterwards whether the outcome stood would read it and fault. The wire
admits this shape (WF-041 constrains only *d* ≥ 1) and the frontend emits
it from ordinary source — the grammar does not tie the numeral to the
bullet count — so it is reachable, not synthetic. Verifier: clean.

## Hand-computed absolutes

| Seinwacht | ploegomvang | vangstwaarde | `hoogsteteken` | `preciesteken` | `minsteteken` | `randteken` |
|-----------|-------------|--------------|----------------|----------------|---------------|-------------|
| S1 | 0 | 100 | **leeg** | **leeg** | **leeg** | **leeg** |
| S2 | 7 | 3 | **1** | **1** | **leeg** | **leeg** |

`randteken` is `minsteteken`'s twin at the same degenerate boundary
*d* = *n* + 1, under `exactly` instead of `atLeast`, and it is `leeg` for
every input for the same reason: the out-of-reach trigger fires on the
empty prefix. Its job is to pin that boundary **exactly**. A guard that
is one step too loose — `H ≥ d − n − 1` instead of `H ≥ d − n` — passes
every other case in this corpus and reads the faulting member here; so
does an executor that keeps only the "threshold exceeded" half of the
`exactly` trigger. Both are caught by this one rule, in clingo and in the
Drools exec-model branch respectively.

*S1, the stop.* Members 1 and 2 both hold (0 = 0; 100 > 0), so after two
members *h* = 2. `atMost 1` and `exactly 1` are both settled **false**
at that point and never read member 3 — which would divide 100 by 0.
`atLeast 3` over its two members is settled false before member 1, so
its division is never read either. Three unread divisions, no fault.

*S2, the control.* Member 1 fails (7 ≠ 0), member 2 holds (3 > 0), and
member 3 **is** read: ⌊3/7⌋ = 0, which is not > 0. So *h* = 1 — within
the cap and exactly the count — and both targets are written. Without
this row the S1 leegs could equally mean "the composite stopped
evaluating altogether"; S2 proves member 3 is live whenever the prefix
leaves the outcome open. `minsteteken` stays `leeg` here too: *d* > *n*
is dead for every input, which is the point.

## The differential square

`ref` ≡ `clingo` over all six expectations (gate 5 ≡ gate 6), anchored to
the hand-written absolutes above; gate 12 replays them in Drools. Gate 10
re-emits `source.rgs` to a byte-identical `ir.json`. NOOP-REFUTING: `S2`'s
`hoogsteteken` and `preciesteken` are derived and absent from the raw
input, so a do-nothing run mismatches two expectations (gate 7).
