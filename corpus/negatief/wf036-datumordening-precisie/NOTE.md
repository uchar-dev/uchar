Violates WF-036 (`spec/IR-SPEC.md` ch. 4.6), the DATE-ORDERING arm of the
comparison typing rule: `De aanmelddatum van een Vondst moet later of
gelijk zijn aan de vondstdag van de Vondst` orders a day-precision date
(`Date`, `precision: day`) against a millisecond-precision one (`precision:
millisecond`). Both
operands are dates, so this is not the number-versus-text defect of
`wf036-order-number-vs-text` — but a date in days and a date in
milliseconds are two REPRESENTATIONS: the first names a whole day, the
second a point inside one, and no order runs between them. Neither side
is wrong alone; their combination is, which is why the report sits at the
comparison node and not at an operand.

    WF-036 FAIL /rules/consistencies/0/versions/0/assertion: date ordering
    across two date precisions (day and millisecond), which share no order

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstnummer`
(a `LATER_OF_GELIJK` consistency over two day-precision operands). Two
lines move and nothing else: the precision of the declaration of
`Vondst.vondstdag` from `day` to `millisecond`, and the single operand
annotation that
reads it — so every annotation stays truthful and the document carries
exactly one defect.

Measured on 2026-09-05, before the clause, verifier-clean, and answered
by no executor. The golden's own case inputs were replayed with
`vondstdag` restated as the `dateTime` value `2025-03-01T00:00:00`,
which the changed declaration demands — that instant is what the
millisecond number below spells:

  * `ref` faults at evaluation —
    `EVALUATION FAULT: unordered comparison: 2025-03-05 vs
    @1740787200000ms`. It is the only leg that meets the defect itself:
    a same-precision millisecond ordering plays here, and only the mixed
    pair faults;
  * the ASP backend refuses ahead of translation —
    `TRANSLATION FAULT: unscalable datatype in the ASP translation`.
    That is a FORM refusal, not a verdict on this defect: the same
    message comes on the CLEAN same-precision ordering, because the ASP
    scale table hands a millisecond-precision date no scale — a
    millisecond-precision ordering FOLD
    (`corpus/golden/jutterswet-hoogwaterpaar`) does translate and play;
  * the Drools backend refuses the crossing on an explicit guard of its
    own. It SKIPS this document (`EXCLUDE transpiler SKIP: consistentie
    (schending buiten Phase-D scope)`), while the SAME document with
    BOTH operands at millisecond precision PLAYS — measured in this
    consistency and in a rule condition alike, discriminating a
    one-millisecond difference a day reading would call equal (the
    reversed expectation fails on that input, so the comparison really
    is evaluated). Only the mixed pair is refused, at a guard whose own
    comment names it a §5.3 Tabel 36 type mismatch never to be lossily
    coerced: a third executor, built for another engine, fencing where
    this rule fences.

Unlike the ordering-fold sister under WF-038, the RegelSpraak frontend
does NOT refuse this shape: its comparison typing lets the four ordering
operators cross the precision seam deliberately, while its equality,
assignment, tijdsduur and eerste/laatste rules each demand one precision.
The clause is therefore a v1 tightening of a form the writer can still
emit — the wire fence moving ahead of the frontend, recorded rather than
hidden — and it settles the question the WF-036 text carried as OPEN.
It costs the corpus nothing: over the four verified zones (golden, rood,
negatief and parity — 504 documents on 2026-09-05) the only report that
moved was this one. What the corpus does not carry: all seven of its
date orderings put two DAY-precision operands side by side, so the
ADMITTED millisecond half rests on the executors, not here — refusing
the same-precision millisecond pair too moves zero of those 504 reports.
A same-precision millisecond ordering golden is the missing regression
witness.

Falsifiable, clause-exact: let the date-ordering arm read the two
representations as one family — `Datum_in_dagen` and `Datum_tijd` both
answering one `Date` tag, the shape the arm carried before this clause —
and this case alone turns clean, while `wf036-order-number-vs-text` (an
ordering across number and text) and the six ordering-fold cases under
WF-038 stay red.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The two date
representations are one `Date` node with a `precision`, `day` or
`millisecond` (delta B15 of `spec/proposals/V1-WIREMIGRATIE.md`); the
document is the translation of `spec/proposals/v1-migratie/m3.py`, and the
arm reads the precision. `expect.txt` pins the arm.
