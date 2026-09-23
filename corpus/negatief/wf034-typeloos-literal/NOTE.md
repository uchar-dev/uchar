Violates WF-034 (`spec/IR-SPEC.md`): a value node whose static type is plain
null. Here a `Literal` with `"type": null` sits as the right operand of a
numeric operator (`MAAL`). Such a literal is schema-valid (`Literal.type` is
nullable on the wire) yet would compute as a bare unitless number in both
executors; the verifier is the fence. Since L2 a literal's `type` is one of the
few annotations every value node still carries of its own: the five reference
classes carry none, and their declaration is their type.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.
