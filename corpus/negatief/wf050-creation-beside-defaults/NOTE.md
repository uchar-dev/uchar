Violates WF-050 (`spec/IR-SPEC.md`): an object creation initialises
`Jutter.waardering` while an attribute ladder owns that target — a
creation-time write would silently defeat the ladder's decisions and
void the initialisation rung's input-emptiness justification.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-vondstwaardering/ir.json`.
