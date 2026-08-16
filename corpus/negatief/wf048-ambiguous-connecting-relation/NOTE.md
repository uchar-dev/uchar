Violates WF-048 (`spec/IR-SPEC.md`): the allocation carries no
`verbindendeRol`, and **two** fact types (`bergingsdeelname` and
`bergingstoezicht`) relate `Berging` to `Berger` — the connecting
relation is not uniquely derivable from the type pair, so the
bindingless form is ill-formed (the lifted ch. 4.1 runtime fault of
`spec/RUNTIME-SEMANTICS.md`).

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-bergingstoezicht/ir.json`.
