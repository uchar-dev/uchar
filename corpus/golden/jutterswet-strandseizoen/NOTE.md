# jutterswet-strandseizoen — cells over two dimensions, read and written, and folds with a selection per dimension

The attribute `vondstgewicht` of a `Jutter` is dimensioned by `seizoen`
(`voorjaar`, `zomer`, `najaar`, read behind the attribute after `in`) and
`strand` (`noord`, `zuid`, read after `op`), in that order. The document reads
two of its cells and folds it four ways. The attribute `aanvoer` is
dimensioned by `strand` and `herkomst` (`zee`, `rivier`, read after `uit`, the
seventh preposition of §3.6); the document writes two of its cells, reads one
and folds it once. All of it is written in RegelSpraak and emitted by the
RegelSpraak frontend; gate 10 re-emits `ir.json` byte for byte. Where
RegelSpraak v2.3.0 is silent, the written forms follow ALEF
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "§3.6 / §13.4.17.51 More
dimensions"; IR-SPEC ch. 2.7).

## Source (`source.rgs`)

| rule | written value | on the wire |
|---|---|---|
| `zuidvoorjaarsgewicht` | `zijn vondstgewicht in voorjaar op zuid` | `DimensionCellRead`, labels `voorjaar`, `zuid` |
| `noordnajaarsgewicht` | `zijn vondstgewicht op noord en in najaar` | `DimensionCellRead`, labels `najaar`, `noord` |
| `noordgewicht` | `de som van zijn vondstgewicht over alle seizoenen over de stranden in {noord}` | `sum`; `seizoen` all three, `strand` `noord` |
| `zuidpiekgewicht` | `de maximale waarde van zijn vondstgewicht over de stranden in {zuid} over de seizoenen vanaf voorjaar t/m zomer` | `maximum`; `seizoen` `voorjaar`, `zomer`, `strand` `zuid` |
| `dalgewicht` | `de minimale waarde van zijn vondstgewicht over de seizoenen vanaf zomer t/m najaar over alle stranden` | `minimum`; `seizoen` `zomer`, `najaar`, `strand` both |
| `jaargewicht` | `de som van zijn vondstgewicht over alle seizoenen over alle stranden` | `sum`; both dimensions whole |
| `zuidzeeaanvoer` | target `De aanvoer op zuid en uit zee`, value `zijn vondstgewicht in voorjaar op zuid plus zijn vondstgewicht in zomer en op zuid` | target `DimensionCellRead`, labels `zuid`, `zee` |
| `zuidrivieraanvoer` | target `De aanvoer uit rivier en op zuid`, initialised on `0 kg` | target `DimensionCellRead`, labels `zuid`, `rivier` |
| `noordriviergewicht` | `zijn aanvoer uit rivier op noord` | `DimensionCellRead`, labels `noord`, `rivier` |
| `zeeaanvoergewicht` | `de som van zijn aanvoer over alle stranden over de herkomsten in {zee}` | `sum`; `strand` both, `herkomst` `zee` |

A cell carries its labels per position, label i a label of dimension i
(WF-040), whatever order the sentence writes them in: `noordnajaarsgewicht`
writes the strand first. Behind the attribute the labels stand side by side
(`zuidvoorjaarsgewicht`) or joined by "en", as ALEF writes them
(`noordnajaarsgewicht`); the wire is the same. A label of `herkomst` stands
after `uit`, side by side (`noordriviergewicht`) or joined by "en", after
another label (`zuidzeeaanvoer`) or before one (`zuidrivieraanvoer`). A cell
target names its labels between the attribute and the subject, as a cell read
names them behind the attribute, joined by "en" in both targets here. A fold
carries one `DimensionSelection` per dimension of its argument, in the
declared order of the dimensions: `zuidpiekgewicht` writes the strand first
and goes out with the season first. A fixed dimension is a selection of one
label, as ALEF writes it (`over de stranden in {noord}`).
In `dalgewicht` the operand takes `over de seizoenen` as name words and leaves
the bare interval; the frontend splits the operand off by the declared plural,
so the operand's source snippet keeps those words and the selection's snippet
is the interval alone.

## The scenario document (`scenario.rgs`)

The two cases written in RegelSpraak, one scenario each (`twee stranden`,
`lege cellen`), with the cells as input and as expectation in the forms in
which a rule reads a cell: side by side (`in voorjaar op noord`), in either
order (`op noord in zomer`), joined by "en" (`op noord en in najaar`) and by
"," (`in voorjaar, op zuid`), and after `uit` (`op noord uit zee`, `uit
rivier en op noord`). The expectations are the hand-computed ones below,
transcribed; gate 16 regenerates both case files and `ir.json` from
`source.rgs` and `scenario.rgs` byte for byte
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Scenariotoetsen").

## The cases — computed by hand

Both cases also pin the four rules over `aanvoer`; the expectations of the
first six rules are as they were, since no rule over `aanvoer` writes a cell
of `vondstgewicht`.

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

J1 also has the cells of `aanvoer` (`noord`, `zee`) 1.50 and (`noord`,
`rivier`) 0.75.

| expectation | computed | value |
|---|---|---|
| cell `aanvoer` (`zuid`, `zee`) | 5.00 + 0.50, the cells (`voorjaar`, `zuid`) and (`zomer`, `zuid`) of `vondstgewicht` | 5.50 |
| cell `aanvoer` (`zuid`, `rivier`) | no input, initialised | 0.00 |
| `noordriviergewicht` | the input cell (`noord`, `rivier`) | 0.75 |
| `zeeaanvoergewicht` | 1.50 (input) + 5.50 (written) | 7.00 |

`case2-lege-cellen.json`: J2 with the one cell (`zomer`, `noord`) 4.00, and
J3 without any cell. For J2 both cell reads are empty (an empty cell), the sum
over the strand `noord` and the sum over everything are 4.00, the minimum over
`zomer` and `najaar` on both strands is 4.00, and the maximum over `voorjaar`
and `zomer` on `zuid` is empty: every cell of that selection is empty, and a
wholly empty selection folds to empty (`RUNTIME-SEMANTICS.md` ch. 5.8c). For
J3 all six are empty.

Over `aanvoer`, J2 has the one input cell (`zuid`, `rivier`) 2.00 and J3
none. For both, the written cell (`zuid`, `zee`) is 0.00: its two operands are
empty cells and `plus` coalesces an empty operand to 0, so empty plus empty is
0 (`RUNTIME-SEMANTICS.md` ch. 1.2), and the sum over `zee` folds that 0 with
the empty cell (`noord`, `zee`) to 0.00. The initialisation leaves J2's input
2.00 on (`zuid`, `rivier`) standing and writes 0.00 for J3. The cell
(`noord`, `rivier`) is empty for both, so `noordriviergewicht` is empty.

| expectation | J2 | J3 |
|---|---|---|
| cell `aanvoer` (`zuid`, `zee`) | 0.00 | 0.00 |
| cell `aanvoer` (`zuid`, `rivier`) | 2.00 (input, standing) | 0.00 |
| `noordriviergewicht` | empty | empty |
| `zeeaanvoergewicht` | 0.00 | 0.00 |

The one expectation that equals an input cell is J2's (`zuid`, `rivier`),
which pins that the initialisation does not displace input; each case has
values the no-op guard (gate 7) cannot find on the input, so both fail under
it.

| executor | outcome (2026-11-02) |
|---|---|
| `ref` (gate 5) | `oracle: PASS (11 expectations)`, `oracle: PASS (21 expectations)` |
| `clingo` (gate 6) | `clingo: PASS (11 expectations)`, `clingo: PASS (21 expectations)` |
| Drools (gate 12) | `PLAYED 2`, both branches |

The verifier passes the document (gate 3); `--dependency-edges` prints one
edge, from `zuidzeeaanvoer` to `zeeaanvoergewicht` over the cell
`attr:jutter:aanvoer[zuid, zee]`: the fold reads the cell the rule writes.
