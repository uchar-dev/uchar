Violates WF-077 (`spec/IR-SPEC.md` ch. 4.10): the `IfThenElse` condition reads
an attribute (`inkomen`) directly as an operand. Everywhere an `IfThenElse` is
admissible — here inside a `Let` body — the condition may READ only
binders, parameters and literals; an attribute read must pass through a
`Let` binding first (the leeg-barrier, which since maintainer decision O3
sits at the READ — ch. 10.6 clause 2).

A minimal hand-built document.
