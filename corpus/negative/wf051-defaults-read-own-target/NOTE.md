Violates WF-051 (`spec/IR-SPEC.md`): a default's justification reads the
defaults' own target characteristic (`uitgesloten`), so a dependency
cycle passes through them. Displacement reads are negative edges
(RUNTIME-SEMANTICS ch. 7.4), so no evaluation strata exist; the same
document also trips WF-045/046 on the extended graph — incidental
extra violations, the directory declares WF-051.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-uitsluiting/ir.json`.
