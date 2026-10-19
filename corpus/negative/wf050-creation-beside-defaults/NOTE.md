Violates WF-050 (`spec/IR-SPEC.md`): an object creation initialises
`Jutter.waardering` while attribute defaults own that target — a
creation-time write would silently defeat the defaults' decisions and
void the initialisation default's input-emptiness justification.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-vondstwaardering/ir.json`.
