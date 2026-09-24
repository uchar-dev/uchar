# jutterswet-afdrachtstapel

The **sum and the difference of two percentages** in a condition. A
`Wrakstuk` carries two percentages, its `bergloonpercentage` and its
`havengeldpercentage` (both `Percentage (geheel getal)`), and two
characteristics follow from them: `overbelast` when the two together exceed
50%, `bergergunstig` when the first exceeds the second by more than 20%.

## What the golden pins: a percentage plus or minus a percentage is a percentage

The frontend types `plus` and `min` over two numeric operands with the left
operand's unit (RUNTIME-SEMANTICS ch. 2.3: "plus, min, verminderd met | maxDec;
whole iff both operands whole; left operand's unit"), so each composite here
carries the unit `%` and is written as a `Number` whose unit is `%`, without the
presentation tag, which belongs to declarations only (IR-SPEC ch. 2.4). Since
L3 of the v1 wire migration (`spec/proposals/V1-WIREMIGRATIE.md` delta B14,
`spec/CONSTRUCTS.md` row 27) a number whose unit is `%` IS a percentage: the
wire has one spelling for both, and every reader lays such a `Number` back as a
percentage, whatever produced it. So the two composites are percentages, and
the type derived from their operands is a percentage too (WF-091 compares the
two).

That is a refinement and no change of value. Before L3 the composite's
annotation was a number with the unit `%` beside the declared percentages, a
type the readers held apart from a percentage; the arithmetic and the
comparison were the same, and they still are:

- `plus` and `min` are rational field operations on the magnitudes
  (RUNTIME-SEMANTICS ch. 2.2), and a percentage standing alone denotes its
  per-hundred number itself — no operator scales it, only `percentage van`
  divides by 100 (ch. 2.2). So 40% plus 15% is 55 (in `%`), not 0,55.
- The comparison restates both sides by their canonical factors (ch. 2.6):
  both carry the built-in `%` with factor 1, so it compares the magnitudes.
  `groter is dan` is strict.
- Both operands are always filled in the case, so the empty rules of ch. 1.2
  do not come into play.

## Hand computation (`case.json`)

| instance | bergloon | havengeld | sum | > 50 | difference | > 20 | overbelast | bergergunstig |
|----------|----------|-----------|-----|------|------------|------|------------|---------------|
| W1       | 40       | 15        | 55  | yes  | 25         | yes  | **present** | **present** |
| W2       | 10       | 45        | 55  | yes  | −35        | no   | **present** | **absent**  |
| W3       | 30       | 5         | 35  | no   | 25         | yes  | **absent**  | **present** |
| W4       | 35       | 15        | 50  | no   | 20         | no   | **absent**  | **absent**  |

W4 stands on both boundaries: 50 is not greater than 50 and 20 is not greater
than 20. No consistency rule exists, so the case ends `consistent`.

## Measured (2026-09-24)

ref and clingo pass the case (9 expectations each). The Drools backend plays it
in its DRL-text branch (PLAYED 1) and refuses it in its exec-model branch before
fire: that branch's numeric comparison arms admit an operand typed as a number
only, and both sides here are percentages ("voorwaarde buiten Phase-B scope").
The refusal is listed with its reason in `scripts/check-drools.sh`.

## noop refutation (gate 7)

`overbelast` and `bergergunstig` are absent from the input world, so the
`--noop` run fails on every `characteristicPresent` expectation (W1, W2, W3).
Not a survivor.
