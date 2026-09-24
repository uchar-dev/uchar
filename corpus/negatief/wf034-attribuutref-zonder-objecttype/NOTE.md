Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an `AttributeReference` that
names no object type. `objecttype` is the endpoint the attribute's value
lives on (ch. 4.1a), so a reference without one resolves against no
declaration at all.

    WF-034 FAIL /rules/resolvedRegels/0/versions/0/condition/links: attribute
    reference 'aangevoerd gewicht' without an object type: it resolves against
    no declaration

Why it has to exist: the empty field is not one rule's deferral but a
**generic exemption**. With `objecttype` absent, `resolved_attr_datatype`
yields `None` and the arm's whole `Some otn` branch is skipped, so the
member check (WF-034), the navigation discipline (WF-053), the
characteristic-in-a-value-position check (WF-080) and the entire identity
family (WF-089/WF-090/WF-091, which defer on an unresolvable declaration)
all fall silent together — one absent field disarms five rules.

Until L2 this document carried the measured **dangerous** shape, not a
neutral one: the reference read `Jutter.aangevoerd gewicht`, declared in
`kg`, and was typed in `t` (same canonical unit, factor 1000 — the
convertible class no compatibility cross reaches). With `objecttype` filled
that lie was refused arm-exact (`WF-091 FAIL … unit mismatch: attribute
reference 'aangevoerd gewicht' is typed 't' …`); with the field emptied it
was verifier-clean. Since L2 the reference carries no type of its own (B13),
so the lie cannot be written any more; what remains is the missing object
type.
The measured consequence of that silence, before the closure (card
`20260903-identiteitsarmen-getuigen`): `ref` faults
(`EVALUATION FAULT: missing AttribuutRef.objecttype in IR node`), clingo
refuses (`TRANSLATION FAULT: missing objecttype`), and the Drools backend
PLAYS the document and answers off the lie — a differential wrong answer
no executor and no other rule fenced. The shape is carried by zero documents outside this witness
(152 golden + 22 rood + 30 parity documents, plus every negatief that
predates this card, measured), so the widening cannot over-reject. The same empty field in a **fold source**
needed a clause of its own — see `wf034-foldbron-zonder-objecttype`, where
all three executors play it.

A hand-broken variant of `corpus/golden/jutterswet-aanvoergeld/ir.json`:
one field emptied, a one-line diff since L2 (until L2 also one unit
abbreviation and factor changed on the reference's own type); the shape
is schema-valid — `AttribuutRef.objecttype` is `["string","null"]` on the
v0 wire, like nearly every reference field — which is precisely why the
verifier, not the schema, is the fence.

Falsifiable, clause-exact: restore the `| None -> []` arm of
`Attribuut_ref` in `walk_expr` and this case alone turns clean, every
other `wf034-*` case staying red. Measured in the L2 fix round (2026-09-24), with the arm replaced by
`| None -> []` in a copy of `spec/verifier/lib/checks.ml`: this document
answers `verifier: PASS`, and over golden, rood, negatief and the parity
zone (718 documents) no other report changes.
