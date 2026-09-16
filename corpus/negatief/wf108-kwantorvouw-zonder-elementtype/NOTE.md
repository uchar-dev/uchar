Violates WF-108 (b) (`spec/IR-SPEC.md` ch. 4.5): the quantifier fold names no
`elementObjecttype`, so the population it ranges over is unnamed. `elementType`
is the fold family's POPULATION — extensionally, every instance whose type is
⊑ it (ch. 2.2) — and without it the contributors are `reach ∩ ∅` in one reading
and `reach` in another, which is exactly the "one document, two answers" class
the rule keeps out.

Presence and resolution are two properties and the row splits them: the
PRESENCE is this rule's, while a present-but-undeclared name is WF-034's,
reported under that number at this position.

The document co-fires WF-053 on the matrix, which is the defect propagating
rather than a second one: with no element type the matrix's subject falls back
to the outer `Bergingsploeg`, where `dagvergoeding` can only be reached over the
to-many `berger` role. Admitted ("incidental extra violations are allowed").

Verifier: `WF-108 FAIL /resolvedRegels/0/voorwaarde/elementObjecttype: quantifier
fold without an elementObjecttype: the population it ranges over is unnamed`.
