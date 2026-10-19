Violates WF-047 (`spec/IR-SPEC.md`): the allocation's `connectingRole`
binds role `bewaker`, which is not a declared role of fact type
`bergingsdeelname` (its roles are `berging` and `berger`; the plural
does not bind either).

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-bergingstoezicht/ir.json`.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and
its recipient binding is the version's `connectingRole`; the report cites
that field of the version. `expect.txt` pins the arm.
