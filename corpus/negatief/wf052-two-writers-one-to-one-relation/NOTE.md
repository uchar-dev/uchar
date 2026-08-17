Violates WF-052 (`spec/IR-SPEC.md`): two object creations
(`dossier een`, `dossier twee`) both write the connecting relation
`dossiervoering`, which carries a to-one role — on any shared subject
the second writer would inevitably hand that end a second filler,
which the reference interpreter faults and a stable model cannot
refuse without a guard. At most one creating writer per to-one
connecting relation (the WF-026/WF-050 single-writer family).
