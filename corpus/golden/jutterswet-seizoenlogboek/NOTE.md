# jutterswet-seizoenlogboek — cells of a text, a date and a boolean

Cells of another type than a number (`RUNTIME-SEMANTICS.md` ch. 5.8c; IR-SPEC
ch. 2.7, where the specification is silent the dialect follows ALEF, which
dimensions any type): a text, a date and a boolean attribute dimensioned by
`seizoen`, read, written, folded and compared. Written in `source.rgs` and
emitted by the RegelSpraak frontend; gate 10 re-emits `ir.json` byte for
byte.

## Source (`source.rgs`)

A dimension `seizoen` with the labels `voorjaar`, `zomer`, `najaar`; on the
`Jutter` the dimensioned attributes `vondstsoort` (Tekst), `meldingsdatum`
(Datum in dagen) and `vergunning` (Boolean).

| rule | target | value |
|---|---|---|
| `najaarssoort` | the cell `najaar` of `vondstsoort` | the cell `zomer` of `vondstsoort` |
| `voorjaarssoort` | `voorjaarssoort` (Tekst) | the cell `voorjaar` of `vondstsoort` |
| `eerste soort` | `eerste soort` (Tekst) | the first of `vondstsoort` over all seasons |
| `aantal soortseizoenen` | a whole number | the number of filled cells of `vondstsoort` ("het aantal voorkomens van") |
| `najaarsmelding` | `najaarsmelding` (Datum) | the cell `najaar` of `meldingsdatum` |
| `laatste melding` | `laatste melding` (Datum) | the last of `meldingsdatum` over all seasons |
| `zomertarief` | `zomertarief`, 2 | where the cell `zomer` of `vergunning` is equal to waar |

A first and a last over cells pick by position and order nothing, so they
take a text cell (WF-038 admits it there). The cell `najaar` the rule
`najaarssoort` writes is a copy of `zomer`, so J1's first text cell,
"barnsteen", differs from its last, "wrakhout".

## The case — measured, per executor

`case1-soort-datum-vergunning.json`, hand-computed. J1: `vondstsoort`
`voorjaar` "barnsteen", `zomer` "wrakhout"; `meldingsdatum` `voorjaar`
2026-04-12, `najaar` 2026-10-03; `vergunning` `zomer` waar, `najaar` onwaar.
J2: `vondstsoort` `zomer` "touw"; `meldingsdatum` `zomer` 2026-07-30;
`vergunning` `voorjaar` waar.

| expectation | J1 | J2 |
|---|---|---|
| cell `najaar` of `vondstsoort` | "wrakhout" | "touw" |
| `voorjaarssoort` | "barnsteen" | leeg |
| `eerste soort` | "barnsteen" | "touw" |
| `aantal soortseizoenen` | 3 | 2 |
| `najaarsmelding` | 2026-10-03 | leeg |
| `laatste melding` | 2026-10-03 | 2026-07-30 |
| `zomertarief` | 2 | leeg |

J1 also pins two input cells, `meldingsdatum` `voorjaar` 2026-04-12 and
`vergunning` `najaar` onwaar, so that a cell expectation of a date and of a
boolean is read; the other expectations do not hold on the input, so the case
fails under `--noop` (gate 7). The `aantal soortseizoenen` counts the cell
`najaar` the rule writes.

| executor | outcome (2026-11-02) |
|---|---|
| `ref` (gate 5) | `oracle: PASS (17 expectations)` |
| `clingo` (gate 6) | `clingo: PASS (17 expectations)` |
| Drools (gate 12) | `PLAYED 1`, both branches |

Red on main `b7e3a9611`, measured form by form on this document without the
first, the last and the count over cells (which that main refuses in every
layer): the ASP backend refused the read of a text cell and the comparison of
a boolean cell with `CLINGO-002`, and a `cellValue` expectation of a text, a
date or a boolean with "this encoding carries a cell of a number"; it read a
date cell already, as the day number a date attribute is.
