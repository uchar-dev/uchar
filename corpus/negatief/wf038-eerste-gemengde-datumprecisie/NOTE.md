Violates WF-038 (`spec/IR-SPEC.md` ch. 4.6), the **cross-operand** half
of the ordering-fold clause at its date seam: `de eerste van zijn
ochtendhoogwater en zijn avondhoogwater` lists one millisecond-precision
date (`Date`, `precision: millisecond`) and one day-precision date
(`precision: day`). Both
operands are dates, so this is neither the equality-only defect of the
four sibling cases nor the number-versus-date mix of
`wf038-eerste-gemengde-familie` — but a date in days and a date in
milliseconds are two REPRESENTATIONS: the first names a whole day, the
second a point inside one, and no order runs between them.

    WF-038 FAIL /rules/resolvedRegels/0/waarde: EERSTE orders dates of two
    precisions (day and millisecond), which share no order
    WF-038 FAIL /rules/resolvedRegels/1/waarde: LAATSTE orders dates of two
    precisions (day and millisecond), which share no order

Both fold nodes are refused: the document declares one attribute at each
precision and reads that same pair twice, once per ordering kind.

A minimal hand-broken variant of `corpus/golden/jutterswet-hoogwaterpaar`
(an articulated `EERSTE`/`LAATSTE` pair over two millisecond-precision
operands). Three fields move and nothing else: the precision of the
declaration of `Getijdendag.avondhoogwater` from `millisecond` to `day`, and
the
two operand annotations that read it — so every annotation stays
truthful and the document has exactly one defect.

Measured before the clause, verifier-clean, with three different
outcomes — one per executor: `ref` faults at evaluation (`EVALUATION
FAULT: unordered comparison: 2026-06-21 vs @1782022200000ms`); the ASP
backend refuses ahead of translation (`TRANSLATION FAULT: a
non-datum-tijd (or mixed-granularity) operand under a datum-tijd
eerste/laatste fold has no order here`); and the Drools backend
**throws** on the same input (`ERROR ConsequenceException: …
java.lang.ClassCastException: class java.time.LocalDate cannot be cast
to class java.time.chrono.ChronoLocalDate`) — a crash on a document
every gate called well-formed.

The RegelSpraak frontend already refuses this shape at typing
(`Typering.datumAggregatie`: "alle datumexpressies moeten dezelfde
tijd-precisie hebben"), so the clause costs no emittable form: it brings
the wire fence into agreement with the frontend that writes to it.

Falsifiable, clause-exact: make the two date halves of the ordering
fold's family function share one tag — `Datum_in_dagen` and `Datum_tijd`
both answering one `Date` family, the shape the clause carried when it
landed — and this case alone turns clean, while
`wf038-eerste-gemengde-familie` (number versus date) and the four
equality-only cases stay red.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The two date
representations are one `Date` node with a `precision`, `day` or
`millisecond` (delta B15 of `spec/proposals/V1-WIREMIGRATIE.md`); the
document is the translation of `spec/proposals/v1-migratie/m3.py`, and the
arm reads the precision. `expect.txt` pins the arm.
