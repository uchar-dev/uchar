Violates WF-052 (`spec/IR-SPEC.md`): two object creations
(`dossier een`, `dossier twee`) both write the connecting relation
`dossiervoering`, whose created-side role (`dossier`) is to-one — on
any shared subject the second writer's link is the second filler the
reference interpreter faults while a guardless stable model would
silently hold both. At most one object-creation writer per such
relation (the WF-026/WF-050 single-writer family).
