Violates WF-022 (`spec/IR-SPEC.md`): A rule target that is neither attribute nor characteristic of its type.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 the target is the `target` of a `RuleVersion` that carries neither a
`presentation` tag nor `initialisation` true, which the loader reads as a
characteristic attribution (delta B20). `expect.txt` pins the arm.
