# wf111-date-time-bound-literal

Violates WF-111's DAY-PRECISION clause (`spec/IR-SPEC.md` ch. 4.6) with a
literal: the window's `from` is a `Literal` annotated with a `Date` of
`precision: millisecond`.

    WF-111 FAIL /rules/rules/0/versions/0/condition/from: the from
    bound of a time window is a millisecond-precision date: …

Derived from `corpus/golden/jutterswet-premievenster/ir.json`. Only the
`vondstpremie` rule is kept, and its window's lower bound is replaced by an
instant literal.

A literal carries no declaration, so the verifier judges its precision by
the annotation. That is the arm of the clause this document pins. Its twin
`wf111-date-time-bound-attribute` pins the declared-attribute arm, and it
carries the reason for the clause.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The two date
representations are one `Date` node with a `precision`, `day` or
`millisecond` (delta B15 of `spec/proposals/V1-WIREMIGRATIE.md`); the
document is the translation of `spec/proposals/v1-migratie/m3.py`, and the
arm reads the precision. `expect.txt` pins the arm.
