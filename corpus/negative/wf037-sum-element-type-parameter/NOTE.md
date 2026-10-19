# wf037-sum-element-type-parameter

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its fold-source arm, as
`wf037-sum-element-type-literal` does: the same `sum` over the element
type `Vondst` with the range `vondstaanmelding`/`vondst`, and as its one
argument a `ParameterReference` to the declared parameter `drempelgewicht`
(kg, two decimals): the value of a parameter, and no source of instances.

    WF-037 FAIL /rules/rules/0/versions/0/value: a sum aggregation carries
    elementType "Vondst" and a ParameterReference operand: the source of
    an instance fold is an AttributeReference

The document is that of `wf037-sum-element-type-without-operand`: a `Jutter`
whose `aangemeld gewicht` (kg, two decimals) is the target, a `Vondst`
with a kg `gewicht`, and the fact type `vondstaanmelding`. The model
declares `drempelgewicht` for this document only, so that the reference
resolves and the argument is the only defect.

Before the arm the verifier refused the node on WF-047 alone, in the
sentence about the binding, and gate 3 reported the case `rejected, but
not for WF-037`. Measured before the arm, as for
`wf037-sum-element-type-literal`: `ref` and the ASP backend in their
bottom sentences, the exec-model branch a transpiler SKIP, the DRL-text
branch a `ClassCastException`. Since the arm `ref`, the ASP backend and
the Drools loader refuse it in the verifier's sentence.

The rood-eerst measurement, the verifier checksums and the sweeps with
their mutants are recorded once, in
`corpus/negative/wf037-count-element-type-literal/NOTE.md`.

`expect.txt` pins the arm.
