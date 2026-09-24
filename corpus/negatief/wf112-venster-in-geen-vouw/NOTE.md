# wf112-venster-in-geen-vouw

Violates WF-112 (`spec/IR-SPEC.md` ch. 4.6) in its FOLD arm: the time
window is the `toets` of a `Kwantorvouw` whose quantifier is `GEEN`.

    WF-112 FAIL /rules/resolvedRegels/2/versions/0/condition: a time window
    under GEEN or a counting quantifier: …

Derived from `corpus/golden/jutterswet-schouwploeg/ir.json`. The rule
*bevoegd bij diploma* has quantifier `GEEN` and the bare window
[2024-01-01, 2025-01-01) as its toets. Nothing else changed.

This is the arm that matters, not an extra spelling of the composite arm.
On 2026-09-23 the Drools DRL-text branch answered exactly this document
instead of refusing it. In a time-free session it read *bevoegd* as
present, because no member satisfies a window that never matches. That is
the complement reading WF-112 refuses (the measurement table is in
`corpus/negatief/wf112-periodevenster-tellende-kwantor/NOTE.md`). `ref`
and the ASP backend refuse the position by name.

Since card `drools-venster-vouwtoets` (2026-09-23) both Drools branches
refuse this document themselves, by name, without the verifier in front:
the exec-model branch and the DRL-text branch both report "periodevenster
in filter of toets van een kwantorvouw onder GEEN (WF-112): …". Measured
with the time-free `case1-allen-ervaren.json` of
`corpus/golden/jutterswet-schouwploeg`, which pins *bevoegd* present, the
complement: before the card the DRL-text branch returned `PLAYED 1` on it.
The backend's `VensterVouwWitnessTest` replays this document through both
branches.

Falsifiable: remove the `wf112_position` call from the `Kwantorvouw` arm
of `spec/verifier/lib/checks.ml`, and this document is the one negatief
case that turns clean.
