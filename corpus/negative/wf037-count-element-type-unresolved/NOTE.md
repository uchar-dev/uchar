# wf037-count-element-type-unresolved

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its fold-source arm (the
card aantal-bronarm-en-drools-lader), as
`wf037-count-element-type-literal` does: the same `count` over the
element type `Vondst` with the range `vondstaanmelding`/`vondst`, and as
its one argument an `Unresolved` marker (ch. 4.4) where the source should
stand, accounted for by a warning diagnostic that contains its source
snippet (WF-113): a marker, and no source of instances.

    WF-037 FAIL /rules/rules/0/versions/0/value: a count aggregation
    carries elementType "Vondst" and an Unresolved operand: the source of
    an instance fold is an AttributeReference

The document is that of `wf037-count-element-type-literal` with this
argument in place of the literal. The arm names the marker as it names any
other node there: under the count the form was already refused, on WF-047,
and under the filled count on its set-source arm, so no verdict changes;
the marker keeps its own rule (WF-044) wherever it stands in a well-formed
document.

Before the arm the verifier refused the node on WF-047 alone, in the
sentence about the binding (`a fold-source role binding on a node that is
not an instance fold over a relation`, at
`/rules/rules/0/versions/0/value/sourceRole`), and gate 3 reported the
case `rejected, but not for WF-037`. Measured before the arm with the case
of that witness: `ref` and the ASP backend refused in their bottom
sentences (`unsupported instance-fold source shape`; `expression kind not
translated (no corpus case exercises it)`), and both Drools branches
played it — PLAYED 1, the range counted and the argument never read. Since
the arm `ref`, the ASP backend and the Drools loader refuse it in the
verifier's sentence.

The rood-eerst measurement, the verifier checksums and the sweeps with
their mutants are recorded once, in
`corpus/negative/wf037-count-element-type-literal/NOTE.md`.

`expect.txt` pins the arm.
