# jutterswet-strandseizoen — a cell over two dimensions, and folds with a selection per dimension

The attribute `vondstgewicht` of a `Jutter` is dimensioned by `seizoen`
(`voorjaar`, `zomer`, `najaar`, read behind the attribute after `in`) and
`strand` (`noord`, `zuid`, read after `op`), in that order. The document reads
two of its cells and folds it four ways, written in RegelSpraak and emitted by
the RegelSpraak frontend; gate 10 re-emits `ir.json` byte for byte. Where
RegelSpraak v2.3.0 is silent, the written forms follow ALEF
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "§3.6 / §13.4.17.51 More
dimensions"; IR-SPEC ch. 2.7).

## Source (`source.rgs`)

| rule | written value | on the wire |
|---|---|---|
| `zuidvoorjaarsgewicht` | `zijn vondstgewicht in voorjaar op zuid` | `DimensionCellRead`, labels `voorjaar`, `zuid` |
| `noordnajaarsgewicht` | `zijn vondstgewicht op noord in najaar` | `DimensionCellRead`, labels `najaar`, `noord` |
| `noordgewicht` | `de som van zijn vondstgewicht over alle seizoenen over de stranden in {noord}` | `sum`; `seizoen` all three, `strand` `noord` |
| `zuidpiekgewicht` | `de maximale waarde van zijn vondstgewicht over de stranden in {zuid} over de seizoenen vanaf voorjaar t/m zomer` | `maximum`; `seizoen` `voorjaar`, `zomer`, `strand` `zuid` |
| `dalgewicht` | `de minimale waarde van zijn vondstgewicht over de seizoenen vanaf zomer t/m najaar over alle stranden` | `minimum`; `seizoen` `zomer`, `najaar`, `strand` both |
| `jaargewicht` | `de som van zijn vondstgewicht over alle seizoenen over alle stranden` | `sum`; both dimensions whole |

A cell carries its labels per position, label i a label of dimension i
(WF-040), whatever order the sentence writes them in: `noordnajaarsgewicht`
writes the strand first. A fold carries one `DimensionSelection` per dimension
of its argument, in the declared order of the dimensions: `zuidpiekgewicht`
writes the strand first and goes out with the season first. A fixed dimension
is a selection of one label, as ALEF writes it (`over de stranden in {noord}`).
In `dalgewicht` the operand takes `over de seizoenen` as name words and leaves
the bare interval; the frontend splits the operand off by the declared plural,
so the operand's source snippet keeps those words and the selection's snippet
is the interval alone.

## The cases — computed by hand

`case1-twee-stranden.json`: J1 with the cells (`voorjaar`, `noord`) 2.00,
(`najaar`, `noord`) 1.00, (`voorjaar`, `zuid`) 5.00 and (`zomer`, `zuid`)
0.50; (`zomer`, `noord`) and (`najaar`, `zuid`) are empty.

| attribute | cells read | value |
|---|---|---|
| `zuidvoorjaarsgewicht` | (`voorjaar`, `zuid`) | 5.00 |
| `noordnajaarsgewicht` | (`najaar`, `noord`) | 1.00 |
| `noordgewicht` | 2.00 + empty + 1.00 | 3.00 |
| `zuidpiekgewicht` | max(5.00, 0.50) | 5.00 |
| `dalgewicht` | min(empty, 0.50, 1.00, empty) | 0.50 |
| `jaargewicht` | 2.00 + 1.00 + 5.00 + 0.50 | 8.50 |

`case2-lege-cellen.json`: J2 with the one cell (`zomer`, `noord`) 4.00, and
J3 without any cell. For J2 both cell reads are empty (an empty cell), the sum
over the strand `noord` and the sum over everything are 4.00, the minimum over
`zomer` and `najaar` on both strands is 4.00, and the maximum over `voorjaar`
and `zomer` on `zuid` is empty: every cell of that selection is empty, and a
wholly empty selection folds to empty (`RUNTIME-SEMANTICS.md` ch. 5.8c). For
J3 all six are empty. Neither case pins an input cell, and each has a value
the no-op guard (gate 7) cannot find on the input, so both fail under it.

| executor | outcome (2026-11-02) |
|---|---|
| `ref` (gate 5) | `oracle: PASS (7 expectations)`, `oracle: PASS (13 expectations)` |
| `clingo` (gate 6) | `clingo: PASS (7 expectations)`, `clingo: PASS (13 expectations)` |
| Drools (gate 12) | `PLAYED 2`, both branches |

The verifier passes the document (gate 3); no rule reads another rule's
target, so `--dependency-edges` prints no edge.
