Violates WF-046 (`spec/IR-SPEC.md`): a negative edge on a
WF-045-admissible cycle — a recursive object creation ("maak kind",
member of recursive group "groei") writes `leeftijd`, the
characteristic rule derives `volwassen` from `leeftijd`, and the
creation's condition reads `volwassen` negated. The cycle is admissible
(it passes through recursive creation) but carries a negative edge, so
no evaluation strata exist (`RUNTIME-SEMANTICS.md` ch. 6.3). The
creation member is conditional, so WF-033 is satisfied — the case
isolates WF-046.

A minimal hand-built document on the shape of
`corpus/golden/jutterswet-penningdrempel/ir.json`.
