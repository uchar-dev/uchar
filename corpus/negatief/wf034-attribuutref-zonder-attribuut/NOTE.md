Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an `AttributeReference` that
names no attribute. The pair `objecttype` + `attribuut` is what a reference
resolves through; with the member name absent it lands on a declared object
type and reads nothing there.

    WF-034 FAIL /rules/resolvedRegels/0/versions/0/condition/links: attribute
    reference on 'Jutter' without an attribute name: it resolves against no
    declaration

Why it has to exist beside `wf034-attribuutref-zonder-objecttype`: it is the
OTHER half of the same pair, and it silences the same rules from the other
side. `resolved_attr_datatype` needs both halves, so the empty name makes it
`None` and the whole identity family (WF-089/WF-090/WF-091) defers; the member
check that would report an undeclared member (WF-034) and the
characteristic-in-a-value-position check (WF-080) both sit behind
`match attribuut with Some a`, so they have nothing to look up either. One
absent field, four rules silent — the shape of the widening this rule already
carries, on the second field of the same node.

Until L2 this document carried the measured **dangerous** shape, not a
neutral one: it was the sibling case's `t`-for-`kg` lie moved onto the
surviving field. The reference read `Jutter.aangevoerd gewicht`, declared in
`kg`, and was typed in `t` (same canonical unit, factor 1000 — the
convertible class no compatibility cross reaches). With the name filled that
lie was refused arm-exact (`WF-091 FAIL … unit mismatch: attribute reference
'aangevoerd gewicht' is typed 't' …`); with the name emptied it was
verifier-clean. Since L2 the reference carries no type of its own (B13), so
the lie cannot be written any more; what remains is the empty name.

Measured before the closure, on this document against
`corpus/golden/jutterswet-aanvoergeld/case.json`:

    ref    : EVALUATION FAULT: missing AttribuutRef.attribuut in IR node
    clingo : TRANSLATION FAULT: missing attribuut
    Drools : EXCLUDE transpiler SKIP: regel: aanvoerplichtig bij drempel
             — regel (voorwaarde buiten Phase-B scope)

So this channel is the loud one: two executors refuse the document outright
and the third drops the rule that carries the broken read rather than
answering off it. That is what makes it a smaller fire than the sibling
channels — and no reason to leave the verifier silent, because a document is
ill-formed on its own terms, not on its executors' mercy.

*Measured dracht:* zero outside this witness — 152 golden + 22 rood + 30
parity documents and the 238 negatief cases that predate this card carry no
`AttribuutRef` with `attribuut: null` anywhere. A sweep emptying all 751
`attribuut` positions of the goldens one at a time left **409** verifier-clean
survivors before this clause; over-rejection is therefore impossible by
measurement.

A hand-broken variant of `corpus/golden/jutterswet-aanvoergeld/ir.json`: one
field emptied, a one-line diff since L2 (until L2 also one unit abbreviation
and factor changed on the reference's own type, a three-line diff);
the shape is schema-valid — `AttribuutRef.attribuut` is `["string","null"]` on
the wire, like nearly every reference field — which is precisely why the
verifier, not the schema, is the fence.

Falsifiable, clause-exact: restore the `| None -> []` arm of the member check
in `walk_expr`'s `Attribuut_ref` case and this case alone turns clean, every
other `wf034-*` case staying red. Measured in the L2 fix round (2026-09-24), with the arm replaced by
`| None -> []` in a copy of `spec/verifier/lib/checks.ml`: this document
answers `verifier: PASS`, and over golden, rood, negatief and the parity
zone (718 documents) no other report changes.
