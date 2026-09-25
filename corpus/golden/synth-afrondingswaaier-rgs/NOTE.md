# synth-afrondingswaaier-rgs — the rounding directions written in RegelSpraak

The RegelSpraak-source twin of `synth-afrondingswaaier`: one division,
`opbrengst / kavels` on a `Veilingronde`, rounded in each of the five modes
to whole numbers and once more half away from zero to one decimal. The
source is `bron.rgs`, the document is the RegelSpraak frontend's emission,
and gate 10 re-emits it byte for byte, so the four directions beside
`naar beneden` — `naar boven`, `rekenkundig`, `richting nul` and
`weg van nul` (RUNTIME-SEMANTICS ch. 2.5; on the wire `ceiling`,
`halfAwayFromZero`, `towardZero`, `awayFromZero`) — are pinned as frontend
output for the first time. The five case files are the carrier's, copied
byte for byte, with the same absolute expectations. The directory name
follows the carrier; the document itself is not synthesized.

## Source (`bron.rgs`)

Six rules of one shape, `De <prijs> van een Veilingronde moet berekend worden
als de opbrengst van de Veilingronde gedeeld door de kavels van de
Veilingronde <modus> afgerond op <n> decimalen.`, with the carrier's rule
names (case4 pins them in its `inconsistent` expectations). The attribute
names are the carrier's own spelling, `prijsBenedenaf` … `prijsFijn`, one
name word each. The grammar reads the unit word `decimalen` only, so the
one-decimal rule is written `afgerond op 1 decimalen`.

## Structural diff against `synth-afrondingswaaier/ir.json`

Read with every `source` stripped and the keys sorted, the documents differ
at six paths, one per rule, all of the same kind:

| path | hand-written | emitted | triage |
|---|---|---|---|
| `/rules/rules/{0..5}/versions/0/value/base/type/whole` | `true` | `false` | (i) emission difference |

The difference sits in the type annotation of the division under each
rounding: both documents annotate it with 0 decimals and no unit, and only
the wholeness flag differs. RUNTIME-SEMANTICS ch. 2.10 (clause 2) has a
consumer take a composite's precision from the ch. 2.3 table, not from the
wire; the division's row there gives maxDec, a documented under-report, and
says nothing about wholeness. The division is also one of ch. 2.10's
fourth-class positions, which keep their annotation for the unit only, and
neither document annotates a unit. No verifier rule crosses the flag:
IR-SPEC WF-091 lists the division among the positions it leaves uncrossed.
Of the two flags the frontend's is the one the values bear out, since
87 / 4 = 21,75 is not a whole number. The flag changes no outcome here: all
four executors replay the same pinned values over both documents (below).

At the byte level the `source` records differ as well: the rule targets read
`De prijsBovenaf van een Veilingronde` (the declared name) where the
hand-written document has `De prijs bovenaf …`, and the one-decimal rounding
ends in `1 decimalen` where it has `1 decimaal`. Both documents carry
`source` records on the same nodes.

## Hand computation

Rounding per RUNTIME-SEMANTICS ch. 2.5; the empty operands per ch. 1.2
(an empty dividend coalesces to 0, an empty divisor under a filled dividend
yields empty plus a §9.5 inconsistency, and a rounding propagates empty).

| case | opbrengst / kavels | quotient | benedenaf | bovenaf | rekenkundig | naar nul | van nul | fijn (1 dec.) |
|---|---|---|---|---|---|---|---|---|
| case1 | 87 / 4 | 21,75 | 21 | 22 | 22 | 21 | 22 | 21,8 |
| case2 | 84 / 4 | 21 | 21 | 21 | 21 | 21 | 21 | 21 |
| case3 | leeg / 4 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| case4 | 87 / leeg | leeg | leeg | leeg | leeg | leeg | leeg | leeg |
| case5 | 85 / 4 | 21,25 | 21 | 22 | 21 | 21 | 22 | 21,3 |

case1 separates the floor pair from the ceiling pair; case5 puts
`rekenkundig` on the floor side at 21,25, which is what tells it apart from
`naar boven` and `weg van nul`; case2 is the exact quotient that no mode
moves. case4 expects all six attributes empty and six `inconsistent`
facts, one per rule. case1, case2, case3 and case5 expect `consistent`.
Every case fails a no-op run, so none needs a survivor entry.

## Executors

ref and clingo replay all five cases with the pinned values; the Drools
backend plays them in both branches, exec-model and DRL text, `PLAYED 5`
each, as it does the carrier (measured 2026-09-25).
