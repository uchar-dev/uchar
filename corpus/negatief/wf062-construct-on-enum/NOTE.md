Violates WF-062 (`spec/IR-SPEC.md` ch. 4.7): the `Construct` names the
value `Gaaf` of the ENUMERATION domain `keurstatus`. `Construct` builds a
sum-type constructor value and nothing else — its `type` must be a
`SumType`. The nullary keuzetype has no constructor node: an enumeration
value is written as a `Literal` (WF-039), and this is the "payload never
converts" half of the family split (ch. 2.4) seen from the expression
layer.

The witness matters because the WF-063 widening (2026-09-01) made
`Match` well-formed over an enumeration scrutinee; `Construct` was
deliberately NOT widened with it, and until now no case pinned that
asymmetry. The verifier's refusal reads "construct type is enumeration,
not a sum type".

A minimal hand-built document.
