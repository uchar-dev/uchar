# jutterswet-seizoenaanvang — a computing rule and an initialisation on one cell

The pair of `RUNTIME-SEMANTICS.md` ch. 7.7 on a cell (ch. 5.8c, 2): a rule
that computes the cell `najaar` of the `vondstgewicht` and an initialisation
of the same cell. A cell is a target of its own in the domain of the v0
desugaring, so the pair reads as one defaults chain on the cell, the computed
value above the initialisation, and the input of the cell standing where
neither decides. Written in `source.rgs` and emitted by the RegelSpraak
frontend; gate 10 re-emits `ir.json` byte for byte.

## Source (`source.rgs`)

A dimension `seizoen` with the labels `voorjaar`, `zomer`, `najaar`; on the
`Jutter` a `vondstgewicht` in `kg` dimensioned by it and a `jaargewicht`.

| rule | form | target | value |
|---|---|---|---|
| `najaarsgewicht van een jutter` | computation | the cell `najaar` | the cell `voorjaar` |
| `najaarsaanvang van een jutter` | initialisation | the cell `najaar` | 1 kg |
| `jaargewicht van een jutter` | computation | `jaargewicht` | the sum over all seasons |

The initialisation writes 1 with no decimals into a cell of 2; the write
boundary of ch. 2.4 holds it, 1.00.

## The case — measured, per executor

`case1-berekening-en-aanvang.json`, hand-computed:

| instance | input | cell `najaar` | `jaargewicht` | why |
|---|---|---|---|---|
| J1 | `voorjaar` 2.00 | 2.00 | 4.00 | the copy decides |
| J2 | none | 1.00 | 1.00 | the copy is leeg and does not decide; the initialisation writes |
| J3 | `najaar` 5.00 | 5.00 | 5.00 | the copy is leeg; the cell is filled, so the initialisation is not at its turn and the input stands |
| J4 | `voorjaar` 3.00, `najaar` 5.00 | 3.00 | 6.00 | the copy decides and displaces the input |

The case names the input cell of J3 as an expectation; J1, J2 and J4 do not
hold on the input, so the case fails under `--noop` (gate 7).

| executor | outcome (2026-11-02) |
|---|---|
| `ref` (gate 5) | `oracle: PASS (9 expectations)` |
| `clingo` (gate 6) | `clingo: PASS (9 expectations)` |
| Drools (gate 12) | `PLAYED 1`, both branches |

Red on main `b7e3a9611`: the verifier, `ref` and both Drools branches as
above; the ASP backend `TRANSLATION FAULT: the case supplies input for
computed target "vondstgewicht[najaar]"`, and without the input cells of J3
and J4 its refusal of the initialisation of a cell beside a computing rule.
