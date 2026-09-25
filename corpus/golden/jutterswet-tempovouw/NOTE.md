# jutterswet-tempovouw — the §7.1 per-element timeline fold

The **fourth reading** of `het totaal van`, and the one its sibling
`corpus/golden/jutterswet-tempototaal` deliberately leaves out: a **set read of
a TEMPORAL attribute**. That sibling pins the other three side by side — the
non-temporal set fold (the `de som van` alias), `de som van` itself, and the
subject's own timeline — over one unit, `€/mnd`. This document is the row its
table names and does not carry.

| this golden | operator | operand | operand type | result type |
|---|---|---|---|---|
| `bewaarlast` | `het totaal van` | `het bewaartarief van alle vondsten van de Jutter` (`set = true`, **timeline-declared**, `voor elke maand`) | `€/mnd` | **`€`** |

Two folds happen, in this order (RUNTIME-SEMANTICS ch. 3.6 and §7.1):

1. **per element, over time.** Each `Vondst`'s own `bewaartarief` timeline
   collapses first, at that attribute's own granularity (`month`): every
   segment contributes `value × whole grid units of its span` (§4.20 Tabel 30).
2. **over the set.** Only those per-member scalars are summed. An element that
   brings no timeline contributes nothing; an empty set totals `0`
   unconditionally (§4.20).

## The §4.20 noemer strip

The strip belongs to the **time aggregation**, not to the operator. Because
this fold integrates over time, the `/tijdseenheid` of the operand drops: an
operand of `€/mnd` yields a result of `€`, declared as such on `bewaarlast`.
That is exactly the dividing line `jutterswet-tempototaal` draws — there the
**non-temporal** set reading is the one place the strip is suppressed and the
result keeps `€/mnd`. The two goldens are each other's counterexample, on the
same operator and the same unit, so neither line can move unnoticed.

The strip is exact here and only here: denominator and grid are the same `mnd`,
so the declared factor is `1` and the whole-month count cancels it outright. A
convertible-but-foreign denominator (`€/jr` over a month grid, factor 12) is a
restatement, which the reference interpreter performs exactly and the ASP
translator refuses loudly rather than round.

## The hand computation

`J1` bewaart `V1`, `V2` and `V3`; `J2` bewaart nothing. `asOfDate` is
2026-03-15, which this fold never consults — the integration runs per member,
so every member brings its own horizon.

| member | `bewaartarief` timeline | whole months × value | contributes |
|---|---|---|---|
| `V1` | `[2026-01-01, 2026-04-01)` = 12 €/mnd | 3 × 12 | **36 €** |
| `V2` | `[2026-01-01, 2026-02-01)` = 7 €/mnd | 1 × 7 | 7 € |
| `V2` | `[2026-02-01, 2026-03-01)` = 5 €/mnd | 1 × 5 | 5 € |
| `V3` | none | — | **drops out** |

| Jutter | `bewaarlast` |
|---|---|
| `J1` | 36 + 7 + 5 = **48 €** |
| `J2` | **0 €** — §4.20: an empty totaal is unconditionally 0 |

Plus `consistent`: the document declares no consistency rules. Three
expectations, hand-computed first and only then replayed.

## What the executors say

Measured on the checked-in `ir.json` + `case.json`:

```
verifier                : PASS
ref                     : oracle: PASS (3 expectations)
backends/clingo         : clingo: PASS (3 expectations)
backends/drools         : PLAYED 1
frontend re-emission    : byte-identical to ir.json
```

So the differential square closes on this form: `ref` ≡ clingo ≡ Drools, all
three against values written down before any of them ran.

## What this shape cannot pin

The per-member spans enter the ASP encoding as calendar data, one `#sum` term
per **(member, segment)** pair, and the integer encoding bounds that
accumulation. The bound is a translation-time fault, not a value — a document
whose members together cover more whole units than the guard admits is refused
by name instead of summed. It cannot be a red-corpus case, because the
reference interpreter simply replays such a document, and the red corpus
demands an observable fault in both executors; the boundary is pinned in the
ASP backend's own suite instead. This golden sits far below it: 5 whole
member-months in total.
