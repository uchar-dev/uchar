Violates WF-030 (`spec/IR-SPEC.md`): Union uniqueness sources disagreeing on key arity.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 the single-type uniqueness and the union are one `Uniqueness` whose
version carries a list of `sources` (delta B27); here two `Source` nodes whose
`keys` differ in length. `expect.txt` pins the arm.
