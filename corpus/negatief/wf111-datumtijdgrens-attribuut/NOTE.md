# wf111-datumtijdgrens-attribuut

Violates WF-111's DAY-PRECISION clause (`spec/IR-SPEC.md` ch. 4.6): the
window's bound is the subject's own scalar attribute, and that attribute is
a millisecond-precision `Date`.

    WF-111 FAIL /rules/rules/0/versions/0/condition/from: the from
    bound of a time window is a millisecond-precision date: …

Derived from `corpus/golden/jutterswet-premievenster/ir.json`. Only the
`bergloon` rule is kept, and its `meldatum` is declared (and annotated) a
`Date` with `precision: millisecond`.

**Why the clause exists (maintainer decision 2026-09-23).** Before the
clause, this document was verifier-clean and the two OCaml executors split
on it. The ASP backend refused an instant bound statically, at
translation. `ref` refused only a FILLED instant value at evaluation. A
LEEG millisecond-precision bound took `ref`'s "a leeg bound never matches" path and
played, so on a verifier-clean document `ref ≠ clingo`. The day-precision
clause closes that split statically, for every executor.

Falsifiable: empty the `instant` report in `wf111_bound`, and this document
and `wf111-datumtijdgrens-literal` turn clean.

**Migrated at L3 (2026-09-24), stamp: herschreven (m).** The two date
representations are one `Date` node with a `precision`, `day` or
`millisecond` (delta B15 of `spec/proposals/V1-WIREMIGRATIE.md`); the
document is the translation of `spec/proposals/v1-migratie/m3.py`, and the
arm reads the precision. `expect.txt` pins the arm.
