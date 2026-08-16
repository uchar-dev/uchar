Violates WF-045 (`spec/IR-SPEC.md`): a dependency cycle through no
recursive creation — `a` is computed from `b + 1` and `b` from `a + 1`
on one object type (`RUNTIME-SEMANTICS.md` ch. 6.2).

A minimal hand-built document on the shape of
`corpus/golden/jutterswet-penningdrempel/ir.json`.
