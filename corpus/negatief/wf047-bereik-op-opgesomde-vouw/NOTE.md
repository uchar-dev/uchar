Violates WF-047 (`spec/IR-SPEC.md` ch. 2.6; the `sourceRole` row of
ch. 4.3): a fold-source role binding on a fold that is not an instance fold —
a `sourceRole` on a listed `LAATSTE` of two operands.

    WF-047 FAIL /rules/rules/0/versions/0/value/sourceRole: a
    fold-source role binding on a node that is not an instance fold over a
    relation

**Why a listed fold has no range.** A range binds the relation whose
instances an instance fold ranges over (ch. 4.3): the fold's single argument
is then read on each instance the relation reaches. A listed value fold (two
or more `arguments`) folds its operands as written and ranges over nothing,
so a binding on it states a relation that no reading of the node uses. The
binding here resolves and is well formed in itself — it is the relation the
second operand navigates (`bewaring`, role `bewarende jutter`) — so the
position is the whole defect. Since L4 a `BuiltinCall` cannot carry a range
at all (delta B21), and this arm's witness until L4,
`wf047-bereik-op-builtin`, was removed with reason
(`corpus/negatief/README.md`, L4); the arm keeps its object on an aggregation
that is no instance fold, which is this document.

**No executor reads the binding.** Measured 2026-09-24 against the two cases
of the source golden: the reference interpreter and the ASP backend pass both,
and the Drools backend plays both. Only the verifier reports it.

**The document.** `corpus/golden/jutterswet-sluitsom/ir.json` with one field
added to the `LAATSTE` of its rule, after `elementType`:
`"sourceRole": {"node": "RolBinding", "feittype": "bewaring", "rol":
"bewarende jutter"}`.

Falsifiable, arm-exact: emptying the arm `None, Some _` of the range match in
`walk_expr` (`spec/verifier/lib/checks.ml`) turns this case clean and nothing
else — 703 documents compared over golden, rood and negatief, one verdict
moved.
