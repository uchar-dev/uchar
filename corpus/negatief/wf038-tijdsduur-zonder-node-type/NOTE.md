Violates WF-038 (`spec/IR-SPEC.md`): a tijdsduur without a node type at all —
the counted unit lives on the node type, so an absent type cannot carry the
required time-unit spelling; without the check this wire is verifier-clean and
faults only at evaluation.

A minimal hand-broken variant of `corpus/golden/anchor/jutterswet-jutleeftijd/ir.json`.
