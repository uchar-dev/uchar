Violates WF-027 (`spec/IR-SPEC.md`): An object creation initialising a member the created type does not declare.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 an object creation is an `ObjectCreation` with its versions (delta
B19); the defect sits on the `ObjectCreationVersion`, whose fields are
`createdType`, `subject`, `initialisations` (each a `Toekenning`) and
`connectingRole`. `expect.txt` pins the arm.
