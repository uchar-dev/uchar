Violates WF-039 (`spec/IR-SPEC.md`): A negative literal under a non-negative sign restriction.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 the literal's value is typed (delta B22), `{"number": "-3"}` under
the sign restriction `NIET_NEGATIEF`; the report cites `/value`. `expect.txt`
pins the arm.
