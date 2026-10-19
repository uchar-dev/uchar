# wf037-sum-element-type-literal

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its fold-source arm (the
card aantal-bronarm-en-drools-lader): an aggregation that carries
`elementType` takes one argument, its fold source, and under a value fold
that source is the attribute read on each contributor, an
`AttributeReference`. This `sum` carries the element type `Vondst`, the
range `vondstaanmelding`/`vondst`, and as its one argument a `Literal`,
2.00 kg: a value, and no source of instances.

    WF-037 FAIL /rules/rules/0/versions/0/value: a sum aggregation carries
    elementType "Vondst" and a Literal operand: the source of an instance
    fold is an AttributeReference

The document is that of `wf037-sum-element-type-without-operand`: a `Jutter`
whose `aangemeld gewicht` (kg, two decimals) is the target, a `Vondst`
with a kg `gewicht`, and the fact type `vondstaanmelding`. The node's type
is the target's, so the argument is the only defect.

Before the arm the verifier refused the node on WF-047 alone, in the
sentence about the binding (`a fold-source role binding on a node that is
not an instance fold over a relation`, at
`/rules/rules/0/versions/0/value/sourceRole`), and gate 3 reported the
case `rejected, but not for WF-037`. Measured before the arm with a case
outside the corpus (the population of golden `jutterswet-vondsttelling`):
`ref` and the ASP backend refused in their bottom sentences (`unsupported
instance-fold source shape`; `expression kind not translated (no corpus
case exercises it)`), the Drools exec-model branch excluded the rule
(transpiler SKIP) and the DRL-text branch crashed on it
(`ClassCastException`: a `Vondst` cast to a `Number`). Since the arm
`ref`, the ASP backend and the Drools loader refuse it in the verifier's
sentence.

The rood-eerst measurement, the verifier checksums and the sweeps with
their mutants are recorded once, in
`corpus/negative/wf037-count-element-type-literal/NOTE.md`.

`expect.txt` pins the arm.
