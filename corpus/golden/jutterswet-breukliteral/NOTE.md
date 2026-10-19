# jutterswet-breukliteral — a fraction literal as the wire's rational

A hand-computed Jutterswet golden for the RegelSpraak rational number
(§13.2.5): the mixed fraction `1_1/2`, its negative `-0_1/2`, and the proper
fraction `3/4`. It is written in `source.rgs`, and its `ir.json` is the
RegelSpraak frontend's emission; gate 10 re-emits it byte for byte.

## Source (`source.rgs`)

A `Vondst` with two whole, non-negative inputs, `gewicht` and `taxatie`, and
three results of 2 decimals. Three rules:

- `Het strandgeld van een Vondst moet berekend worden als zijn gewicht maal
  1_1/2.`
- `De gemeentekorting van een Vondst moet berekend worden als zijn gewicht
  maal -0_1/2.`
- `Het gemeentedeel van een Vondst moet berekend worden als zijn taxatie maal
  3/4.`

The wire carries a number lexeme as an exact decimal or as a rational a/b
with the sign on the numerator (IR-SPEC ch. 4.1). The frontend writes a mixed
fraction as its reduced rational — `1_1/2` goes out as `3/2`, `-0_1/2` as
`-1/2` — and a proper fraction as written; each literal's `source` snippet
keeps the author's spelling. The literals are typed with the smallest scale
that holds their value (1 decimal for 3/2 and −1/2, 2 for 3/4), and so are
the products.

Before the frontend normalised the mixed form, it wrote `1_1/2` as the lexeme,
and every reader refused the document. Measured 2026-09-26 on this golden
with the two lexemes put back: the verifier reports `WF-081 FAIL
/rules/rules/0/versions/0/value/right/value: numeric literal lexeme '1_1/2' is
not a number of the case format (an optional '-', digits, an optional '.' and
digits, or a rational a/b whose denominator has no leading zero — ch. 4.1)`,
ref and clingo stop with the same sentence as `IR LOAD ERROR`, and the Drools
loader with `malformed uchar-ir-v1 document`.

## Hand computation

`maal` is exact (RUNTIME-SEMANTICS ch. 2.2), and under `maal` an empty operand
coalesces to 0 (ch. 1.2: `5 × leeg = 0`).

| case | label | `gewicht` | `taxatie` | `strandgeld` = gewicht × 3/2 | `gemeentekorting` = gewicht × −1/2 | `gemeentedeel` = taxatie × 3/4 |
|---|---|---|---|---|---|---|
| case1-gevuld | V1 | 7 | 10 | **10,50** | **−3,50** | **7,50** |
| case1-gevuld | V2 | 4 | 3 | **6,00** | **−2,00** | **2,25** |
| case2-rand | V3 | 0 | 0 | **0,00** | **0,00** | **0,00** |
| case2-rand | V4 | leeg | leeg | **0,00** | **0,00** | **0,00** |

V1's odd weight keeps the half of 1_1/2, and V2's valuation puts the three
quarters on the second decimal. V3 is the zero operand and V4 the empty one,
which `maal` reads as 0. Both cases also expect `consistent`. Each case pins
values the input does not carry, so a no-op run fails both (ref's `--noop`
exits 1 on each) and neither needs a survivor entry.

## Executors

ref replays both cases, `oracle: PASS (7 expectations)` each, and so does
the ASP backend, `clingo: PASS (7 expectations)` each. Both Drools branches
play them, exec-model and DRL text, `PLAYED 2` each (measured 2026-09-26).

The ASP backend holds a number as an integer at a power-of-ten scale, and it
read a literal's scale from the digits after the lexeme's point, which a
rational has none of. Before it read a rational at the smallest scale that
holds its value exactly (ASP-TRANSLATION S4), it faulted on the first one:
`TRANSLATION FAULT: value 3/2 does not fit 0 decimals`.

An expectation twisted from 10.50 to 10.51 fails in all four: ref
(`MISMATCH: V1.strandgeld = 10.5, expected 10.51`), clingo
(`MISMATCH: V1.strandgeld = [1050], expected 1051`) and both Drools branches
(`FAIL case1.json: attribuut strandgeld: verwacht 10.51, werkelijk 10.5`).
