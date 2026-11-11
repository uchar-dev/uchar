# wf043-wholeperiod-navigated-nontemporal

Violates WF-043 (`spec/IR-SPEC.md`) at a navigated characteristic check: a
whole-period qualifier on a characteristic that is not time-dependent on the
type the navigation reaches. Here the check reads `bemand` on the `Strandpost`
that `standplaats` reaches from the Jutter, and `bemand` carries no timeline.

The document is `corpus/golden/jutterswet-gehele-jaar-gemaakte-koppeling/ir.json`
with the `timeline` of `bemand` removed.

Red first: the verifier of `ccf6de21c` passes this document; it crossed the
time dependence only for a check without navigation. `expect.txt` pins the arm
`WF-043.whole-period-characteristic`.
