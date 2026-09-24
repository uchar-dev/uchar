Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a `ParameterReference` that names
no parameter. A parameter reference resolves through exactly one field; with
it absent the node reads nothing at all.

    WF-034 FAIL /rules/rules/0/versions/0/condition/right: parameter
    reference without a parameter name: it resolves against no declaration

Why the existing WF-034 parameter arm does not cover it: that arm reports a
name that resolves nowhere (`reference to undeclared parameter 'x'`), and it
lives inside `match parameter with Some p`. The absent name walked past it
through the `| None -> []` twin — the same shape as the object-type channel
closed on 2026-09-04, one node kind over. Silent with it, until L2: the
identity family, which crossed this reference's serialized `type` against the
parameter's declared datatype (WF-089 domain, WF-090 struct domain, WF-091 the
full type) and deferred when the declaration did not resolve. Since L2 the
reference carries no `type` (B13) and its type is its declaration's, so an
unnamed parameter reference has no type at all.

Until L2 this document carried the measured **dangerous** shape, not a
neutral one: the reference read the parameter `aanvoerdrempel`, declared in
`kg`, and was typed in `t` (same canonical unit, factor 1000 — the
convertible class no compatibility cross reaches; comparison compatibility,
WF-036, admits it by construction). With the name filled that lie was refused
arm-exact (`WF-091 FAIL … unit mismatch: parameter reference 'aanvoerdrempel'
is typed 't' …`); with the name emptied it was verifier-clean. Since L2 the
lie cannot be written; what remains is the empty name.

Measured before the closure, on this document against
`corpus/golden/jutterswet-aanvoergeld/case.json`:

    ref    : EVALUATION FAULT: missing ParameterRef.parameter in IR node
    clingo : TRANSLATION FAULT: missing ParameterRef.parameter
    Drools : ERROR NullPointerException: Cannot invoke
             "…RegelSpraakModel$Parameter.naam()" because the return value of
             "…Expr$ParameterRef.parameter()" is null

Two executors refuse the document by name; the third throws out of the engine.
None of the three answers off the lie — the cost of the silence is the
uncaught unit annotation, plus a document that reaches three engines before
anything says what is wrong with it.

*Measured dracht:* zero outside this witness — 152 golden + 22 rood + 30
parity documents and the 238 negatief cases that predate this card carry no
`ParameterReference` with `parameter: null` anywhere. A sweep emptying all 37
`parameter` positions of the goldens one at a time left **35** verifier-clean
survivors before this clause (both refusals are WF-049 positions — a
`TemporalRead` base, whose shape clause demands a *granular parameter
reference* and no longer recognises one without a name); over-rejection is
therefore impossible by measurement.

A hand-broken variant of `corpus/golden/jutterswet-aanvoergeld/ir.json`: one
field emptied, a one-line diff since L2 (until L2 also one unit abbreviation and
factor changed on the reference's own type, a three-line diff); the shape is
schema-valid — `ParameterReference.parameter` is `["string","null"]` on the wire
— which is precisely why the verifier, not the schema, is the fence.

Falsifiable, clause-exact: restore the `| None -> []` arm of `Parameter_ref`
in `walk_expr` and this case alone turns clean, every other `wf034-*` case
staying red. Measured in the L2 fix round (2026-09-24), with the arm replaced by
`| None -> []` in a copy of `spec/verifier/lib/checks.ml`: this document
answers `verifier: PASS`, and over golden, rood, negatief and the parity
zone (718 documents) no other report changes.
