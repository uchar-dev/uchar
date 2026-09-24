# wf112-venster-in-alle-binnen-geen

Violates WF-112 (`spec/IR-SPEC.md` ch. 4.6) at DEPTH: the time window is
the only member of an `ALLE` composite, and that composite is a member of
a `GEEN` composite.

    WF-112 FAIL /rules/resolvedRegels/0/versions/0/condition: a time window
    under GEEN or a counting quantifier: …

Derived from `corpus/negatief/wf112-periodevenster-tellende-kwantor`: the
window member there is wrapped in `ALLE [ … ]`. The inner `ALLE` carries
the window as WF-112 allows. The outer `GEEN` still complements the inner
composite's verdict, and with it the window's. The rule forbids the window
under such a quantifier "at any depth", and this document is what makes
that phrase checkable.

Falsifiable: make `cond_has_window` in `spec/verifier/lib/checks.ml` stop
at a nested composite, and this document turns clean.
