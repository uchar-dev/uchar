Violates WF-034 (`spec/IR-SPEC.md`): a value node whose static type is plain
null — the unspelled twin of the explicit Onbekend unknown-type sentinel. Here
a `Literal` with `"type": null` sits as the right operand of a numeric operator
(`MAAL`). Such a literal is schema-valid (`Literal.type` is nullable on the v0
wire) yet would compute as a bare unitless number in both executors; the
verifier is the fence, refusing it symmetrically with the spelled sentinel.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.
