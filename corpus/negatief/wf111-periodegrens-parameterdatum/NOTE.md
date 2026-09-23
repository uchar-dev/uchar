# wf111-periodegrens-parameterdatum

Violates WF-111 (`spec/IR-SPEC.md` ch. 4.6): a time window's bound is a
date literal or a scalar date attribute of the subject itself, and nothing
else. Here the window is `vanaf de peildatum`, where `peildatum` is a date
PARAMETER. WF-042 alone accepts that, because a parameter of type
`DatumInDagen` is a date expression. WF-111 does not.

Expected verifier report:

    WF-111 FAIL /rules/resolvedRegels/0/voorwaarde/vanaf: the vanaf bound of a
    time window is the parameter 'peildatum' — …

## Where the rule comes from

Until 2026-09-23 this same document was the red witness
`corpus/rood/periodegrens-parameterdatum`. It was verifier-clean then, and
all four executors refused it at their own phase: `ref` at evaluation,
clingo in translation, the Drools exec-model branch with a transpiler
SKIP, and the DRL-text branch with a marker that names the bound. The
maintainer decision of 2026-09-23 promoted that shared refusal to the norm.
The literal and the subject's own scalar date attribute are the carried
pair, and `corpus/golden/jutterswet-premievenster` executes both in all
four executors. The general date expression (a parameter, a navigated read,
arithmetic) is recorded as an OPEN wire decision in
`RUNTIME-SEMANTICS.md` ch. 3.12. The reasons are in the decision: no corpus
document asks for it, and the ASP encoding has no fault atom to hang a
bound on when the bound is not tied to the subject.

The executors' refusal arms remain as fail-fast checks for a
non-conforming document that reaches them without going through the
verifier. Each executor's own suite still pins its sentence. The document
has lost its case file: a negatief directory holds only the ill-formed
`ir.json`.
