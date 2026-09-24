Violates WF-034 (`spec/IR-SPEC.md`): a value node whose static type is plain
null. Here a `Literal` with `"type": null` sits as the right operand of a
numeric operator (`MAAL`). Such a literal is schema-valid (`Literal.type` is
nullable on the wire) yet would compute as a bare unitless number in both
executors; the verifier is the fence. Since L2 a literal's `type` is one of the
few annotations every value node still carries of its own: the five reference
classes carry none, and their declaration is their type.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 a literal's value is typed (delta B22) and the migration chooses the
variant by the literal's `type`, so it cannot migrate a typeless literal; this
document was rewritten by hand to `"value": {"number": "2"}` with `"type":
null`, and the report cites
`/rules/resolvedRegels/0/versions/0/condition/links/rechts/type`. WF-114 does
not reach it: without a type there is nothing to cross the variant against.
`expect.txt` pins the arm.
