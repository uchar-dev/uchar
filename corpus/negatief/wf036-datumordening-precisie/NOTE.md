Violates WF-036 (`spec/IR-SPEC.md` ch. 4.6), the DATE-ORDERING arm of the
comparison typing rule: `De aanmelddatum van een Vondst moet later of
gelijk zijn aan de vondstdag van de Vondst` orders a day-precision date
(`DatumInDagen`) against a millisecond-precision one (`DatumTijd`). Both
operands are dates, so this is not the number-versus-text defect of
`wf036-order-number-vs-text` — but a date in days and a date in
milliseconds are two REPRESENTATIONS: the first names a whole day, the
second a point inside one, and no order runs between them. Neither side
is wrong alone; their combination is, which is why the report sits at the
comparison node and not at an operand.

    WF-036 FAIL /consistenties/0/assertie: date ordering across two date
    precisions (day and millisecond), which share no order

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstnummer`
(a `LATER_OF_GELIJK` consistency over two `DatumInDagen` operands). Two
lines move and nothing else: the declaration of `Vondst.vondstdag` from
`DatumInDagen` to `DatumTijd`, and the single operand annotation that
reads it — so every annotation stays truthful and the document carries
exactly one defect.

Measured on 2026-09-05, before the clause, verifier-clean, and answered
by no executor. The golden's own case inputs were replayed with
`vondstdag` restated as a `dateTime` value, which the changed declaration
demands:

  * `ref` faults at evaluation —
    `EVALUATION FAULT: unordered comparison: 2025-03-05 vs
    @1740787200000ms`;
  * the ASP backend refuses ahead of translation —
    `TRANSLATION FAULT: unscalable datatype in the ASP translation`;
  * the Drools backend never reaches the comparison: it refuses this
    document's form outright (`EXCLUDE transpiler SKIP: consistentie
    (schending buiten Phase-D scope)`). Measured separately, so the
    refusal is not mistaken for coverage: the same defect placed in a
    rule condition — a hand-broken
    `corpus/golden/jutterswet-vondstverjaring` — is refused there too
    (`EXCLUDE transpiler SKIP: regel (voorwaarde buiten Phase-B scope)`),
    and so is the same-precision `DatumTijd` control, so that backend
    carries no millisecond-precision date comparison at all.

Unlike the ordering-fold sister under WF-038, the RegelSpraak frontend
does NOT refuse this shape: its comparison typing lets the four ordering
operators cross the precision seam deliberately, while its equality,
assignment, tijdsduur and eerste/laatste rules each demand one precision.
The clause is therefore a v1 tightening of a form the writer can still
emit — the wire fence moving ahead of the frontend, recorded rather than
hidden — and it settles the question the WF-036 text carried as OPEN.
It costs the corpus nothing: over the four verified zones (golden, rood,
negatief and parity — 504 documents on 2026-09-05) the only report that
moved was this one.

Falsifiable, clause-exact: let the date-ordering arm read the two
representations as one family — `Datum_in_dagen` and `Datum_tijd` both
answering one `Date` tag, the shape the arm carried before this clause —
and this case alone turns clean, while `wf036-order-number-vs-text` (an
ordering across number and text) and the six ordering-fold cases under
WF-038 stay red.
