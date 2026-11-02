# jutterswet-seizoenboeking — cells written in RegelSpraak

The golden of the cell as a write position written in RegelSpraak
(`IR-SPEC.md` ch. 3.1, ch. 3.3; `RUNTIME-SEMANTICS.md` ch. 5.8c, the worked
example "Writing"): a rule, an initialisation and an object creation that
each write one cell of a dimensioned attribute, and a cell computed from
another cell of the same attribute. Gate 10 re-emits `ir.json` from
`source.rgs` byte for byte.

## Source (`source.rgs`)

A dimension `seizoen` with the labels `voorjaar`, `zomer`, `najaar`; on the
`Jutter` a `vondstgewicht` in `kg` dimensioned by it, a `jaargewicht` and a
`geboekte gewicht`; on the `Vangst` a `vangstgewicht` dimensioned by
`seizoen` and a `vangsttotaal`; a fact type `vangstboeking`, one boeker to
one geboekte vangst.

| form | writes | reads |
|---|---|---|
| `najaarsgewicht van een jutter` (berekening) | the cell `najaar` of `vondstgewicht` | the cell `voorjaar` of `vondstgewicht`, maal 2 |
| `zomergewicht van een jutter` (initialisatie) | the cell `zomer` of `vondstgewicht`, op 0 kg | — |
| `jaargewicht van een jutter` | `jaargewicht` | the sum of `vondstgewicht` over all seasons |
| `boek een vangst bij elke jutter` (object creation) | the cell `najaar` of the created `vangstgewicht` | the cell `najaar` of the subject's `vondstgewicht` |
| `vangsttotaal van een vangst` | `vangsttotaal` | the sum of `vangstgewicht` over all seasons |
| `geboekte gewicht van een jutter` | `geboekte gewicht` | the `vangsttotaal` of the geboekte vangst |

A cell target names its label between the attribute and the subject ("Het
vondstgewicht in najaar van een Jutter"), the form ALEF writes a cell in on
either side of an assignment; the frontend writes it as a
`DimensionCellRead` over the attribute. The creation's member "vangstgewicht
in najaar" is a `MemberInit` with `labels`. A case cannot name an object the
session makes, so the last two rules carry the made object's cells back to
the jutter that is in the case.

The dependency graph has a node per cell (`IR-SPEC.md` ch. 5.1):
`najaarsgewicht van een jutter` reads `vondstgewicht[voorjaar]` and writes
`vondstgewicht[najaar]`, so it is no cycle; `jaargewicht` and the creation
read the cells the two rules write, and run after them
(`verifier --dependency-edges`, six edges; the frontend's graph is the same,
`CellEdgesTest`).

## Cases (hand-computed)

The product coalesces a leeg operand to 0 (`RUNTIME-SEMANTICS.md` ch. 1.2,
`maal`), so the cell `najaar` is 0.00 where `voorjaar` is empty.

| case | input | expected |
|---|---|---|
| `case1-voorjaar-en-zomer` | J1: `voorjaar` 1.50; J2: `voorjaar` 1.50, `zomer` 2.00 | J1: `najaar` 3.00, `zomer` 0.00, jaargewicht 4.50, geboekte gewicht 3.00; J2: `zomer` 2.00 (the input fills the cell, so the initialisation is not at its turn), `najaar` 3.00, jaargewicht 6.50, geboekte gewicht 3.00 |
| `case2-zonder-invoer` | J3 without a cell | `najaar` 0.00, `zomer` 0.00, `voorjaar` empty, jaargewicht 0.00, geboekte gewicht 0.00 |
| `case3-invoer-in-een-geschreven-cel` | J4: `najaar` 5.00; J5: `voorjaar` 1.00, `najaar` 5.00 | J4: `najaar` 0.00 (the computed value displaces the input of the cell, ch. 7.8), `zomer` 0.00, jaargewicht 0.00, geboekte gewicht 0.00; J5: `najaar` 2.00, jaargewicht 3.00, geboekte gewicht 2.00 |

The `ref/test` rows `WV-5.8c-schrijven-*` replay the three cases and
`WV-5.8c-objectcreatie-cel` reads the cells of the made `Vangst` directly
(`najaar` 3.00, `voorjaar` and `zomer` empty); `WV-5.12-celdoel` pins the
reading of the two rules that write a cell.

## Where the executors stand

Measured 2026-11-02: `ref`, the ASP backend and the Drools backend replay all
three cases (gates 5, 6 and 12; the Drools backend on both branches, `PLAYED
3`), and gate 17 holds the readings of the Drools backend equal to the trace
of `ref`. The Drools backend writes each cell as a member of its own: the
rule, the initialisation and the creation's member write it, a read and a fold
take it through its standing, and the write-back puts it into the map of the
attribute on the bean. Its rule graph carries the cells, and gate 15 compares
it per cell with the verifier's.
