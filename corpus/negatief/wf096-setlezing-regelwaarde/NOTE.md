# wf096-setlezing-regelwaarde

Violates WF-096 (`spec/IR-SPEC.md` ch. 4.3, the set fence): an
`AttribuutRef` with `set: true` stands as the WHOLE rule value. The set
reading names the members' values, one per member — a collection — and the
only node that consumes a collection is the fold that owns the source. A
rule value is a scalar write position: nothing here consumes the set.

    WF-096 FAIL /resolvedRegels/0/waarde: set reading at an ordinary read
    position: a set reading names the members' values, one per member, and
    is admissible only as the SOURCE of a single-argument instance fold
    (ch. 4.3) — here nothing consumes the collection and the slot wants one
    value

The document. Two object types related by one fact type (`Jutter` —
`vondstaanmelding` — `Vondst`, one aanmelder to many vondsten), and one
BEREKENING whose value is the bare set read of `gewicht` on `Vondst`, with a
real `Numeriek` type (kg, two decimals — the declared type of the attribute
it reads), an explicit `navigatieBindingen` hop and an empty `navigatiePad`.
One fault: the type is genuine, so the sentinel fence (WF-034, ch. 4.3) has
no jurisdiction here, and WF-093 does not reach it either — that rule keys on
the sentinel. The hop is legitimate and WF-053 says nothing about it, for a
reason this case is built to show: the walk's to-many clause READS `set`, and
reads it as an escape, so the one field that made the document ill-formed was
also the field that switched off the rule that would otherwise have caught
the to-many hop in a scalar slot.

Producer-authored, not hand-built. This document is the RegelSpraak
frontend's own emission, byte for byte, from the source

    Regel maatgevend gewicht
        geldig altijd
            Het maatgevend gewicht van een Jutter moet berekend worden als
            het gewicht van alle vondsten van de Jutter.

which the frontend accepts with `diagnostics: []`. The sentence is
grammatical RegelSpraak that names a collection where the rule writes one
value, and no diagnostic is raised; what the frontend should emit instead is
spec-first work of its own, tracked on a separate card, and this document is
deliberately unchanged evidence of the route as it stands.

Rood-eerst, measured on this document. Pre-clause verifier
(md5 `8e07cdc5515ec6b29bd6de42e750de54`): `verifier: PASS`. With the clause
(md5 `5111932e2d1e51de144290ab16abab00`, the head this document lands on):
the single rejection above.

Executors, on this document (case: one Jutter linked to two Vondsten of
12.00 and 7.00 kg):

    ref         : EVALUATION FAULT: unsupported AttribuutRef shape
                  (set/sentinel outside a fold)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP — regel (waarde buiten scope)

Three refusals, three kinds of refusal, one document the wire admitted.
There is no answer to compare, so no differential gate can catch it: gates 5
and 6 never see a document without a golden case, and gate 12 counts the
form among its auto-exclusions. That is the argument for refusing the shape
at the wire. The three refusals stay where they are as the second line of
defence — after this clause the form no longer reaches them through a
well-formed document.

Falsifiable, arm-exact: drop the WF-096 clause from `walk_expr`'s
`Attribuut_ref` arm (`spec/verifier/lib/checks.ml`) and this case turns clean
again, together with `wf096-setlezing-operand`; over the four zones (560
documents) nothing else changes a verdict.
