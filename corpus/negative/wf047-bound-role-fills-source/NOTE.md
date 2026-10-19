Violates WF-047 (`spec/IR-SPEC.md`): the allocation's `connectingRole`
resolves — `berging` is a declared role of `bergingsdeelname` — but it
binds the **source-side** role: its filler is `Berging`, while the node
names `Berger` as `targetType` (and the opposite role's filler mismatches
`sourceType` symmetrically). The binding must bind the recipient role.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-bergingstoezicht/ir.json`.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and
its recipient binding is the version's `connectingRole`; the report cites
that field of the version. `expect.txt` pins the arm.
