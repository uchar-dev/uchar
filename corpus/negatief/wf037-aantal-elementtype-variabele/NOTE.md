# wf037-aantal-elementtype-variabele

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its fold-source arm (the
card aantal-bronarm-en-drools-lader), as
`wf037-aantal-elementtype-literaal` does: the same `count` over the
element type `Vondst` with the range `vondstaanmelding`/`vondst`, and as
its one argument a `VariableReference` named `x`: the residual marker of a
variable the resolver could not bind (ch. 4.1), and no source of
instances.

    WF-037 FAIL /rules/rules/0/versions/0/value: a count aggregation
    carries elementType "Vondst" and a VariableReference operand: the
    source of an instance fold is an AttributeReference
    WF-034 FAIL /rules/rules/0/versions/0/value/arguments/0: variable
    reference 'x' resolves against no binding: a rule-local variable is a
    Let, read by a BinderRef

The WF-034 line stands since 2026-09-25: the node is refused wherever it
stands (`wf034-variabele-zonder-binding`).

The document is that of `wf037-aantal-elementtype-literaal` with this
argument in place of the literal.

Before the arm the verifier refused the node on WF-047 alone, in the
sentence about the binding (`a fold-source role binding on a node that is
not an instance fold over a relation`, at
`/rules/rules/0/versions/0/value/sourceRole`), and gate 3 reported the
case `rejected, but not for WF-037`. Measured before the arm with the case
of that witness: `ref` and the ASP backend refused in their bottom
sentences (`unsupported instance-fold source shape`; `expression kind not
translated (no corpus case exercises it)`), and both Drools branches
played it — PLAYED 1, the range counted and the argument never read. With
the arm, `ref`, the ASP backend and the Drools loader refused it in the
verifier's WF-037 sentence. Since 2026-09-25 all three refuse the document
earlier, at load and at the argument, in the WF-034 sentence (measured with
the case of `jutterswet-vondsttelling`: `ref` and clingo `IR LOAD ERROR at
/rules/rules/0/versions/0/value/arguments/0: variable reference 'x' resolves
against no binding: …`, both Drools branches `ERROR Documentfout: malformed
uchar-ir-v1 document at '/rules/rules/0/versions/0/value/arguments/0':
variable reference 'x' resolves against no binding: …`), so none of them
reaches the fold's source arm with this node.

The rood-eerst measurement, the verifier checksums and the sweeps with
their mutants are recorded once, in
`corpus/negatief/wf037-aantal-elementtype-literaal/NOTE.md`.

`expect.txt` pins the arm.
