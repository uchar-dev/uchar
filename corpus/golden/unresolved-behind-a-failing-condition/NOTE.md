# unresolved-behind-a-failing-condition — an Unresolved node the evaluation does not reach

An `Unresolved` node never vanishes: where the evaluation reaches it, at an
expression or at a condition position, the session faults (EF-019, WF-044,
`spec/RUNTIME-SEMANTICS.md` ch. 5.7, 5.7b). Where the evaluation does not reach
it (ch. 5.11), nothing faults. Own casus, rights-free; the document is
hand-built (no `source.rgs`), and its diagnostics account for every marker
(WF-113).

## The document

A `Vondst` with a waardering and the characteristics `actief`, `bijzonder` and
`zwaar`:

- **waardering van een vondst**: the value is an `Unresolved` expression ("de
  waardering volgens het niet-gemodelleerde protocol"), under the condition
  "indien hij actief is".
- **bijzondere vondst**: "Een Vondst is bijzonder indien hij aan ten minste een
  van de volgende voorwaarden voldoet: hij is niet actief; hij voldoet aan het
  protocol", the second member an `Unresolved` condition.
- **zware vondst**: the same composite, its second member the comparison "de
  taxatie volgens het niet-gemodelleerde protocol is groter dan 5", whose left
  operand is an `Unresolved` expression.

## Case (hand-computed, absolute)

`case1-niet-actief` (asOfDate null): V1, not actief.

- waardering: the condition does not hold: **leeg**.
- bijzonder and zwaar: the first member holds and settles `ten minste een`, so
  the second is not evaluated: **bijzonder**, **zwaar**.
- No fault, and the channel is consistent.

With V1 actief every marker is reached and the session faults with EF-019:
`corpus/red/unresolved-behind-a-holding-condition`.
