Violates WF-057 (`spec/IR-SPEC.md`): the attribute name
`taxatie"waarde` carries a double quote — a declared name must be
target-text-safe (it lands between quotes in a generated target text).

A minimal hand-broken variant of
`corpus/golden/anchor/jutterswet-waardestelling/ir.json` with one attribute renamed.
