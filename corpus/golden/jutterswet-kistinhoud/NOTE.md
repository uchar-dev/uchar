# jutterswet-kistinhoud — a power written in RegelSpraak

A hand-computed Jutterswet golden for the RegelSpraak power `tot de macht`
(§13.4.17.14; on the wire `Binary {op: "power"}`, RUNTIME-SEMANTICS
ch. 2.2). No golden outside the anchor zone carried a power that RegelSpraak
can write: `jutterswet-kindermacht` raises inside a fold over a list
parameter, a form the language does not have, and the anchor
`jutterswet-taxatie` writes the power by hand. This golden is new, written in
`source.rgs`, and its `ir.json` is the RegelSpraak frontend's emission; gate 10
re-emits it byte for byte.

## Source (`source.rgs`)

A `Wrakkist` with three unitless attributes, `ribbe`, `zijvlakte` and
`inhoud`, each `niet-negatief getal met 2 decimalen`; a power's operands
carry no unit (WF-035). Two rules:

- `De zijvlakte van een Wrakkist moet berekend worden als zijn ribbe tot de
  macht 2 naar beneden afgerond op 2 decimalen.`
- `De inhoud van een Wrakkist moet berekend worden als zijn ribbe tot de
  macht 3 naar beneden afgerond op 2 decimalen.`

The grammar makes the rounding part of the power itself (its power
production ends in a mandatory `afronding`), so the frontend always writes a
power under a `Rounding`: here `Rounding {floor, 2}` over
`Binary {power}`. The anchor's bare power has no RegelSpraak spelling. The
power's own type annotation is 2 decimals and not whole, the ch. 2.3 row
(maxDec of the base's 2 and the exponent's 0, not whole), and the power and
its rounding carry the same `source` snippet: the whole production, rounding
included.

The exponents are literals. The ASP backend translates a compile-time
non-negative integer exponent only, and it fences twice: the power's operand
at the largest base whose scaled cube stays inside 2^31 (12,90 at the base's
scale of 100), and the rounding above it at a scaled operand of 2^29, which
binds first: measured 2026-09-25, a ribbe of 8,12 replays and 8,13 faults
with `arithmetic range exceeded`. The case values stay far below that.

## Hand computation

An integer exponent is an exact integer power (ch. 2.2), `naar beneden` is
toward −∞ at the declared decimals (ch. 2.5), and an empty base propagates
through the power and the rounding (ch. 1.2).

| case | label | `ribbe` | ribbe² | `zijvlakte` | ribbe³ | `inhoud` |
|---|---|---|---|---|---|---|
| case1-afronding | K1 | 1,25 | 1,5625 | **1,56** | 1,953125 | **1,95** |
| case1-afronding | K2 | 2,50 | 6,25 | **6,25** | 15,625 | **15,62** |
| case2-rand | K3 | 0,00 | 0 | **0,00** | 0 | **0,00** |
| case2-rand | K4 | leeg | leeg | **leeg** | leeg | **leeg** |

K1 is rounded in both rules. K2's square is exact and its cube is cut at the
third decimal, where a rounding half up would give 15,63. K3 is the zero
base and K4 the empty one. Both cases also expect `consistent`: an empty base
is no error. Each case pins at least one value the input does not carry, so a
no-op run fails both and neither needs a survivor entry.

## Executors

ref and clingo replay both cases with the pinned values; the Drools backend
plays them in both branches, exec-model and DRL text, `PLAYED 2` each
(measured 2026-09-25). An expectation twisted from 15,62 to 15,63 fails in
all four.
