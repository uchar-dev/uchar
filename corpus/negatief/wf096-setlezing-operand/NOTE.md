# wf096-setlezing-operand

Violates WF-096 (`spec/IR-SPEC.md` ch. 4.3, the set fence): the same set
reading as `wf096-setlezing-regelwaarde`, one position deeper — the LEFT
operand of a `Binair` `PLUS`. An operand slot of a composite node wants one
value exactly as a rule value does, and nothing in a `Binair` consumes a
collection.

    WF-096 FAIL /resolvedRegels/0/waarde/links: set reading at an ordinary
    read position: a set reading names the members' values, one per member,
    and is admissible only as the SOURCE of a single-argument instance fold
    (ch. 4.3) — here nothing consumes the collection and the slot wants one
    value

The document. The same model as the sibling case with one attribute added
(`drempelgewicht` on `Jutter`, kg with two decimals), and one BEREKENING
whose value is `Binair PLUS`: `links` the set read of `gewicht` on `Vondst`
(real type, `navigatieBindingen` hop), `rechts` an ordinary scalar read of
`drempelgewicht` on the subject. Both operand types and the node type are the
same kg-with-two-decimals type, so the numeric and unit families are silent
and the set reading is the document's only defect. One fault.

This case exists because the decision of 2026-09-09 is ONE decision for BOTH
positions. The two were measured identically — the same three refusals, word
for word, from the same three executors — so splitting them would have
produced two rules with one boundary, and a document whose defect moved one
level down the tree would have been answered by a different rule number for
no reason a reader could give. The sibling case pins the whole-rule-value
position, this one the operand position, and the clause that refuses them is
one clause in one place.

Producer-authored, like its sibling. This is the RegelSpraak frontend's
emission, byte for byte, from

    Regel maatgevend gewicht
        geldig altijd
            Het maatgevend gewicht van een Jutter moet berekend worden als
            het gewicht van alle vondsten van de Jutter plus het
            drempelgewicht van de Jutter.

again with `diagnostics: []`. So the operand position is a LIVE producer
route too, not a hand-built variant of one: the frontend places the set read
under a composite node as readily as it places it at the top.

Rood-eerst, measured on this document. Pre-clause verifier
(md5 `8e07cdc5515ec6b29bd6de42e750de54`): `verifier: PASS`. With the clause
(md5 `5111932e2d1e51de144290ab16abab00`, the head this document lands on):
the single rejection above.

Executors, on this document (case: one Jutter of drempelgewicht 12.00 kg
linked to two Vondsten of 12.00 and 7.00 kg):

    ref         : EVALUATION FAULT: unsupported AttribuutRef shape
                  (set/sentinel outside a fold)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP — regel (waarde buiten scope)

Identical to the whole-rule-value measurement, which is the evidence the one
decision rests on.

Falsifiable, arm-exact: drop the WF-096 clause from `walk_expr`'s
`Attribuut_ref` arm (`spec/verifier/lib/checks.ml`) and this case turns clean
again, together with `wf096-setlezing-regelwaarde`; over the four zones (560
documents) nothing else changes a verdict.
