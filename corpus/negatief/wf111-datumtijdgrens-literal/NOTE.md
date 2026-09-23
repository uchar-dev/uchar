# wf111-datumtijdgrens-literal

Violates WF-111's DAY-PRECISION clause (`spec/IR-SPEC.md` ch. 4.6) with a
literal: the window's `vanaf` is a `Literal` annotated `DatumTijd`.

    WF-111 FAIL /rules/resolvedRegels/0/voorwaarde/vanaf: the vanaf bound of a
    time window is a datum-tijd: …

Derived from `corpus/golden/jutterswet-premievenster/ir.json`. Only the
`vondstpremie` rule is kept, and its window's lower bound is replaced by an
instant literal.

A literal carries no declaration, so the verifier judges its precision by
the annotation. That is the arm of the clause this document pins. Its twin
`wf111-datumtijdgrens-attribuut` pins the declared-attribute arm, and it
carries the reason for the clause.
