# wf112-window-in-all-within-none

Violates WF-112 (`spec/IR-SPEC.md` ch. 4.6) at DEPTH: the time window is
the only member of an `all` composite, and that composite is a member of
a `none` composite.

    WF-112 FAIL /rules/rules/0/versions/0/condition: a time window
    under none or a counting quantifier: …

Derived from `corpus/negative/wf112-time-window-counting-quantifier`: the
window member there is wrapped in `all [ … ]`. The inner `all` carries
the window as WF-112 allows. The outer `none` still complements the inner
composite's verdict, and with it the window's. The rule forbids the window
under such a quantifier "at any depth", and this document is what makes
that phrase checkable.

Falsifiable: make `cond_has_window` in `spec/verifier/lib/checks.ml` stop
at a nested composite, and this document turns clean.
