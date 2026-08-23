Violates WF-077 (`spec/IR-SPEC.md` ch. 4.10): the `IfThenElse` condition reads
an attribute (`inkomen`) directly as an operand. Everywhere an `IfThenElse` is
admissible — here inside a strict `Let` body — the condition may READ only
binders, parameters and literals; an attribute read must pass through a strict
`Let` binding first (the leeg-barrier).

A minimal hand-built document.
