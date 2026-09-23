# wf111-datumtijdgrens-attribuut

Violates WF-111's DAY-PRECISION clause (`spec/IR-SPEC.md` ch. 4.6): the
window's bound is the subject's own scalar attribute, and that attribute is
a `DatumTijd`.

    WF-111 FAIL /rules/resolvedRegels/0/voorwaarde/vanaf: the vanaf bound of a
    time window is a datum-tijd: …

Derived from `corpus/golden/jutterswet-premievenster/ir.json`. Only the
`bergloon` rule is kept, and its `meldatum` is declared (and annotated)
`DatumTijd`.

**Why the clause exists (maintainer decision 2026-09-23).** Before the
clause, this document was verifier-clean and the two OCaml executors split
on it. The ASP backend refused an instant bound statically, at
translation. `ref` refused only a FILLED instant value at evaluation. A
LEEG datum-tijd bound took `ref`'s "a leeg bound never matches" path and
played, so on a verifier-clean document `ref ≠ clingo`. The day-precision
clause closes that split statically, for every executor.

Falsifiable: empty the `instant` report in `wf111_bound`, and this document
and `wf111-datumtijdgrens-literal` turn clean.
