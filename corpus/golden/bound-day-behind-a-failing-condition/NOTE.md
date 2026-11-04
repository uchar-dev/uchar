# bound-day-behind-a-failing-condition — the bound day where the evaluation does not reach it

`de dag` outside a day-kind definition denotes nothing, and evaluating it is a
fault (EF-010, `spec/RUNTIME-SEMANTICS.md` ch. 3.8, 5.7b). A fault is raised
where the evaluation reaches the node (ch. 5.11): behind a condition that does
not hold, and after the stop of a composite, it is not. Own casus, rights-free;
the document is hand-built (no `source.rgs`).

## The document

A `Baken` with a peildatum, a jaartal and the characteristics `actief` and
`gemeld`:

- **jaartal van een baken**: "Het jaartal van een Baken moet berekend worden
  als het jaar uit de dag indien hij actief is." The value reads the bound day,
  under the rule's condition.
- **gemeld baken**: "Een Baken is gemeld indien hij aan ten minste een van de
  volgende voorwaarden voldoet: hij is niet actief; het jaar uit de dag is
  groter dan 2000." The bound day is read in the second member of the
  composite.

## Case (hand-computed, absolute)

`case1-niet-actief` (asOfDate null): B1, peildatum 2026-03-10, not actief.

- jaartal: the condition does not hold, so the value is not evaluated: **leeg**.
- gemeld: the first member holds, so `ten minste een` stands after it and the
  second member is not evaluated (ch. 5.10): **gemeld**.
- No fault, and the channel is consistent.

The same document with B1 actief evaluates both reads and faults with EF-010:
`corpus/red/bound-day-behind-a-holding-condition`.
