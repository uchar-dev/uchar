# jutterswet-vouwrand

The third **fence-carriage witness** of `ASP-TRANSLATION.md` S7, for the
one fence its two siblings do not reach: the **eerste/laatste fold**.

`jutterswet-randwacht` covers the numeric equality and ordering arms.
This golden covers the fold, whose fence fires only when its operands
carry **different scales** — the fold harmonises them, and the operand
with the coarser scale is multiplied up.

## Why this golden has no `bron.rgs`

It is authored as IR on purpose. The frontend refuses `de eerste van`
over numerics — `eerste van vereist datumexpressies` — and over dates all
operands share one scale, so the mixed-scale branch the fence guards is
never reached from source. The shape is nonetheless well-formed IR
(verifier-clean) and the translation carries it, so the fence exists and
must behave. Recording that here rather than quietly leaving the fence
untested: the wire is the contract, and the wire admits this.

## What it pins

```
Het vouwteken van een Randwacht moet gesteld worden op 1
indien hij aan alle volgende voorwaarden voldoet:
- zijn ploegomvang is gelijk aan 0
- de eerste van zijn grootgetal en zijn bedrag is groter dan 0.
```

*(illustrative source rendering — this golden's `ir.json` is authored
directly; see above.)*

`grootgetal` is whole, `bedrag` carries two decimals, so the fold
harmonises by ×100 and the bound drops to `1073741823 / 100`. `V1` puts
30 000 000 above it **and** makes member 1 false.

## Hand-computed absolutes

| Randwacht | ploegomvang | grootgetal | bedrag | member 1 | eerste(grootgetal, bedrag) | `vouwteken` |
|-----------|-------------|-----------|--------|----------|----------------------------|-------------|
| V1 | 1 | 30 000 000 | 1,00 | **false** | *never read* | **leeg** |
| V2 | 0 | 5 | 1,00 | true | 5 (the first filled operand) → 5 > 0 | **1** |
| V3 | 0 | 0 | 1,00 | true | 0 — filled, so it wins over 1,00 → 0 > 0 false | **leeg** |

`V3` is the sharper control: a filled **zero** is not empty, so the fold
returns it and the comparison fails. That distinguishes "member 2 was
read and said no" from "member 2 was skipped", which is exactly the
distinction `V1` relies on.

## The differential square

`ref` ≡ `clingo` over all three expectations (gate 5 ≡ gate 6), anchored
to the hand-written absolutes. NOOP-REFUTING through `V2` (gate 7). With
the fold's fence guard removed, `V1` turns clingo `UNSATISFIABLE` with
`viol("arithmetic range exceeded …")` while ref still passes — this
golden is that mutant's only artifact.
