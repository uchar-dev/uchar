Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): the group default's `exceptions` carries an empty `members`
list — the exceptions field carries at least one exception group.

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-vondstwaardering/ir.json`, its single
default turned into a group default whose `exceptions.members` is empty.
