Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (o), its empty half): a near-side
**role check** (ch. 5.8a) with **no** `anderTeFiller` at all.

    WF-034 FAIL /rules/resolvedRegels/0/voorwaarde/anderTeFiller: role check without
    an anderTeFiller: the connecting role is derived from that type and from
    nothing else, so the check resolves against no declaration

**The other spelling of the same defect.** The fifth wave closed the FILLED
half — a filler type the subject is related to by no declared fact type — and
recorded this one as the half it passed over. `anderTeFiller` is the whole of
the reference: the executors take the connecting role from the subject to that
type and null-test it, while the written `rol` is presentation. With the field
absent there is no type to derive a role TO, so the node carries no reference
whatever — and the filled arm cannot report it, because its cross needs a type
to cross.

**Executor observability**, on this document's host golden and its case:

    ref     EVALUATION FAULT: missing Rollidmaatschap.anderTeFiller in IR node
    clingo  TRANSLATION FAULT: missing Rollidmaatschap.anderTeFiller

against `PASS (4 expectations)` in both on the untouched golden. Both are
loud, which makes this the tidy half — but the verifier said nothing, and a
rule that fences only what an engine already shouts at still has to be
written down for the engine that does not.

**Measured.** `anderTeFiller` emptied on each of the **4** golden role checks
in turn (`jutterswet-wachtindeling` 2, `jutterswet-wachtploeg` 2): 4 of 4
stayed verifier-clean under the verifier of the day before this card, and none
does now.

**The one document that carries the shape, and why it costs nothing.** Of the
10 role checks the four zones held before this card, nine name a type and
**one** carries `anderTeFiller: null` — `corpus/negatief/wf031-dagsoort-leest-rolstaat`,
a day-kind condition. That is not a legitimate form the arm would be refusing:
WF-031 already refuses it outright, because a day-kind voorwaarde may not read
role state at all — which is precisely why the frontend had no subject to
derive a filler from. It keeps its WF-031 verdict and gains one truthful
incidental WF-034 line. No golden and no rood document carries the shape.

*Measured breadth:* over the 510 documents of golden, rood, negatief and the
parity zone that predate this card, not one verdict moves and exactly one
report does — that incidental line.

A hand-broken variant of `corpus/golden/jutterswet-wachtindeling/ir.json` —
one field emptied, a one-line diff; schema-valid, since
`Rollidmaatschap.anderTeFiller` is `["string","null"]` on the v0 wire. It is
deliberately the same golden the filled-name witness breaks, and the same
rule's condition, so the two witnesses are the two spellings of one position.

Falsifiable, arm-exact: delete the `| None, _ ->` branch of the
`Rollidmaatschap` arm of `walk_cond` (`spec/verifier/lib/checks.ml`) and this
case turns clean while `wf031-dagsoort-leest-rolstaat` loses its incidental
line and keeps its verdict — 522 documents compared, one verdict moved, one
other report moved, nothing else.
