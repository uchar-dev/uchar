Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4): a `ParameterRef` used as a
`Match` scrutinee carries a `type` naming the enumeration domain `Kust`
while the parameter it reads, `standaardvaarwater`, is declared with the
domain `Vaarwater`.

    WF-089 FAIL /rules/resolvedRegels/0/waarde/scrutinee/type: parameter reference
    'standaardvaarwater' is typed with keuzetype domain 'Kust', but the
    declaration it reads from names domain 'Vaarwater'

The parameter is the second declaration source the rule names, with its
own code arm: the `Parameter_ref` walk previously checked only that the
parameter exists (WF-034). WF-018/WF-088 pin the parameter's own datatype
against the domain it names, WF-063 the arms against the scrutinee's named
domain — none of them reaches across. Before the rule: `verifier: PASS`;
ref faulted (`match has no arm for enumeration value "Wad"`); clingo
refused the translation on its pre-existing `symbolic_term` fence (an
enumeration-typed parameter as a scrutinee has no carrier, S19), so this
flank was ref-only — a fault, never a wrong answer, but on a document the
verifier had accepted.

Falsifiable: disable the `wf089_domain_identity` call in the
`Parameter_ref` arm and this case passes while the attribute, binder and
cell cases stay red.
