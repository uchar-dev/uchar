# jutterswet-ploegseizoen — cells of another object

Cells of a dimensioned attribute read on another object than the subject
(`RUNTIME-SEMANTICS.md` ch. 5.8c, IR-SPEC ch. 4.3), in the forms ALEF writes:
a cell over a to-one role, a fold over the cells of the object that role
reaches, and a fold of one cell of each member of a to-many role. Written in
`source.rgs` and emitted by the RegelSpraak frontend; gate 10 re-emits
`ir.json` byte for byte.

## Source (`source.rgs`)

A dimension `seizoen` with the labels `voorjaar`, `zomer`, `najaar`; on the
`Jutter` the dimensioned attributes `vondstgewicht` (2 decimals, kg) and
`vondstsoort` (Tekst). A `Ploeg` has one `leider` (`ploegleiding`, one to
one) and many members `lid` (`ploeglidmaatschap`, one to many).

| rule | target | value |
|---|---|---|
| `najaarsgewicht van een jutter` | the cell `najaar` of `vondstgewicht` | the cell `zomer` times 2 |
| `leidersvoorjaarsgewicht` | Ploeg | the cell `voorjaar` of the leader (input) |
| `leidersnajaarsgewicht` | Ploeg | the cell `najaar` of the leader (written) |
| `leidersjaargewicht` | Ploeg | the sum of the leader's `vondstgewicht` over all seasons |
| `laatste leidersgewicht` | Ploeg | the last of the leader's `vondstgewicht` over all seasons |
| `ploegvoorjaarsgewicht` | Ploeg | the sum of the cell `voorjaar` of every member |
| `zwaarste ledennajaarsgewicht` | Ploeg | the maximum of the cell `najaar` of every member (written) |
| `aantal voorjaarsleden` | Ploeg | the number of filled cells `voorjaar` of the members |
| `leiderssoort` | Ploeg (Tekst) | the cell `zomer` of the leader's `vondstsoort` |
| `eerste leiderssoort` | Ploeg (Tekst) | the first of the leader's `vondstsoort` over all seasons |

On the wire a cell over a role is a `DimensionCellRead` whose
`AttributeReference` carries the `path`; the fold over the leader's cells an
`Aggregation` with `dimensionSelections` and that argument; and the fold of
one cell of each member an `Aggregation` with `elementType` `Jutter` and the
range `ploeglidmaatschap`/`lid`, whose source is the `DimensionCellRead` of
`vondstgewicht` on the element type, without a path.

## The cases — measured, per executor

Hand-computed. The product of a leeg cell and 2 is 0 (ch. 1.2, "maal: both
coalesce"), so J3's written cell `najaar` is 0.00, a filled cell.

`case1-twee-ploegen.json`: J1 `vondstgewicht` `voorjaar` 2.50, `zomer`
1.25, `vondstsoort` `zomer` "barnsteen", `najaar` "wrakhout"; J2
`vondstgewicht` `zomer` 4.00, `vondstsoort` `voorjaar` "touw"; J3
`vondstgewicht` `voorjaar` 3.00; J5 `vondstgewicht` `zomer` 2.10. P1 with
leader J1 and members J1, J3, J5; P2 with leader J2 and member J2.

| expectation | P1 | P2 |
|---|---|---|
| `leidersvoorjaarsgewicht` | 2.50 | leeg (J2 has no cell `voorjaar`) |
| `leidersnajaarsgewicht` | 2.50 | 8.00 |
| `leidersjaargewicht` | 6.25 | 12.00 |
| `laatste leidersgewicht` | 2.50 | 8.00 |
| `ploegvoorjaarsgewicht` | 5.50 (J5's cell is leeg) | leeg |
| `zwaarste ledennajaarsgewicht` | 4.20 | 8.00 |
| `aantal voorjaarsleden` | 2 | 0 |
| `leiderssoort` | "barnsteen" | leeg |
| `eerste leiderssoort` | "barnsteen" | "touw" |

and the written cells `najaar`: J1 2.50, J2 8.00, J3 0.00, J5 4.20 (23
expectations with `consistent`).

`case2-zonder-leider-en-leden.json`: J4 `vondstgewicht` `voorjaar` 7.00,
`zomer` 1.50, `vondstsoort` `zomer` "schelp", in no ploeg; P3 without a
leader and without members: every expectation of P3 leeg, the filled count
0, and J4's cell `najaar` 3.00 (11 expectations). Neither case holds on its
input, so both fail under `--noop` (gate 7).

| executor | outcome (2026-11-02) |
|---|---|
| `ref` (gate 5) | `oracle: PASS (23 expectations)`, `PASS (11 expectations)` |
| `clingo` (gate 6) | `clingo: PASS (23 expectations)`, `PASS (11 expectations)` |
| Drools (gate 12) | `PLAYED 2`, both branches |

Gate 17 compares the readings of both case files: a cell over the role names
the object the path reached (`on "J1"`, `on none` for P3), the fold over the
leader's cells none, and a fold of one cell of each member its contributors.

Red on `28df094fd` (the card's step B): the frontend wrote none of the forms
(a cell over a role an unknown member, RGS-125; the fold over the leader's
cells a fold source with a written step, RGS-127; a cell of each member an
undeclared attribute of the element type, a marker with RGS-068). On this
`ir.json` the verifier refused the three folds of one cell of each member
(`WF-037.aggregation-non-source` and `WF-053.read-other-type`), `ref` and the
ASP backend refused them in the same sentence, and the Drools loader refused
the document (`DocumentError`, the same sentence). Without those three folds
the document was verifier-clean; the ASP backend then refused the text cell
over a path and the first of text cells over a path (`CLINGO-002`), and both
Drools branches the cell and the fold over a path (`EXCLUDE DROOLS-189`).
