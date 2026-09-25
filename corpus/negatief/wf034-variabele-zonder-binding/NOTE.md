Violates WF-034 (`spec/IR-SPEC.md` ch. 4.1, 4.6): a `VariableReference`
resolves against no binding. The node is the frontend's marker for a
variable its resolver could not bind (the `VariableReference` row of
ch. 4.1), and the wire serializes no variable binding site — a rule-local
variable is a `Let`, read by a `BinderRef` (ch. 4.10). Broken from
`corpus/golden/jutterswet-kamerheffing-catala` by one move: in the rule
`berekening bedrag` the left operand of `grondslag + 100`, the attribute
read `grondslag`, becomes a `VariableReference` named `vrijstelling`.

    WF-034 FAIL /rules/rules/0/versions/0/value/left: variable reference
    'vrijstelling' resolves against no binding: a rule-local variable is a
    Let, read by a BinderRef

Rood-eerst: until 2026-09-25 the verifier let the node pass
(`verifier: PASS` on this document). With the golden's two cases, `ref`
answered `oracle: PASS (2 expectations)` — no rule the cases fire reads the
node — and the ASP backend refused the document at translation in the words
of its residual arm, `expression kind not translated (no corpus case
exercises it)`. Since then every executor refuses it at load, in the rule's
sentence and at its pointer (the shared loader's executor stance), measured
on this document with the same cases:

    IR LOAD ERROR at /rules/rules/0/versions/0/value/left: variable reference
    'vrijstelling' resolves against no binding: a rule-local variable is a
    Let, read by a BinderRef

for `ref` and for clingo alike. The Drools loader refuses the node in the
same sentence (`IrLoader`, witnessed by `RegelEnExpressielaagWeigeringWitnessTest`),
but on this document it stops one section earlier, at the function
definitions it does not carry: `EXCLUDE loader: form not carried by the v0
Drools backend: functionDefs (unknown model section)` (measured 2026-09-25).
Inside a closed lambda or function body the
same node is refused first by the closure clause (WF-067/WF-069, "a variable
read is not admissible inside a closed function or lambda body").
`expect.txt` pins the arm.
