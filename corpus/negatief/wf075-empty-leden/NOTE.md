Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): the group default's exception group carries an empty `leden`
list — an exception group carries at least one member.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-vondstwaardering/ir.json`, its single
default turned into a group default whose `excepties.leden` is empty.
