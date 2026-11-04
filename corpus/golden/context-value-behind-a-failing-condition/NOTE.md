# context-value-behind-a-failing-condition — a context value without an evaluation date, not reached

The evaluation date and the evaluation year are context values of the session
(`spec/RUNTIME-SEMANTICS.md` ch. 8.2); in a session without an evaluation date
evaluating either is a fault (EF-011, ch. 5.7b). A fault is raised where the
evaluation reaches the node (ch. 5.11). Own casus, rights-free; the document is
hand-built (no `source.rgs`).

## The document

A `Vondst` with a vondstjaar, a peildatum and the characteristics `actief` and
`gemeld`:

- **peildatum van een vondst**: "De peildatum van een Vondst moet berekend
  worden als de Rekendatum indien hij actief is."
- **gemelde vondst**: "Een Vondst is gemeld indien hij aan ten minste een van
  de volgende voorwaarden voldoet: hij is niet actief; het Rekenjaar is groter
  dan zijn vondstjaar."

## Case (hand-computed, absolute)

`case1-niet-actief` (asOfDate null): V1, vondstjaar 2025, not actief.

- peildatum: the condition does not hold: **leeg**.
- gemeld: the first member holds and settles `ten minste een`, so the
  Rekenjaar is not read: **gemeld**.
- No fault, and the channel is consistent.

With V1 actief both context values are read and the session faults with
EF-011: `corpus/red/context-value-behind-a-holding-condition`.
