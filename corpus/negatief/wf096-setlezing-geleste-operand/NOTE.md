# wf096-setlezing-geleste-operand

Violates WF-096 (`spec/IR-SPEC.md` ch. 4.3, the set fence): a set reading as
the second operand of a LISTED value fold. A fold carrying two or more
arguments has no source at all (§5.8.3, and ch. 4.3 says so normatively);
its operands are ordinary reads evaluated one by one against the rule
subject. The set reading is the SOURCE's field, so beside a second operand it
is as ill-formed as it is anywhere else outside a fold.

    WF-096 FAIL /rules/resolvedRegels/0/waarde/argumenten/1: set reading on a fold
    operand that is not a VALUE fold's SOURCE: a set reading is admissible on
    the single argument of a value AGGREGATION alone (ch. 4.3) — with two or
    more arguments the node is the listed value fold, whose operands are
    ordinary reads; at a non-aggregation kind there is no source at all; and
    under AANTAL the fold reads the CONTRIBUTORS, never a value per
    contributor, so a set reading there names a collection the count does not
    consume

The document. A `MAXIMUM` with two arguments and `elementObjecttype:
"Vondst"`; operand 0 an ordinary read of the subject's `drempelgewicht`,
operand 1 a set read of `gewicht` on `Vondst` under the attribute's DECLARED
type. The hop it makes is legitimate — WF-053 resolves it against the
declared relation and says nothing — so the set reading is the document's
only defect. One fault.

A ONE-FIELD twist of `wf034-sentinel-geleste-setlezing`, and deliberately so.
That case is the same document with the unknown sentinel on operand 1's own
`type`; replace the sentinel by the type the attribute actually declares and
the sentinel fence (WF-034, ch. 4.3, seventh wave) falls silent, because that
fence keys on the sentinel and on nothing else. What remains is the `set`
field, and until this clause the document was clean. The pair is the
edge-to-edge witness: the two rules divide the same position between them —
the sentinel one refuses the TYPE, this one refuses the FIELD — and neither
covers the other's half.

It is also the case that pins the second walk path. A fold argument does not
travel through the generic expression walk: the aggregation branch replaces
it, exactly as it replaces the type prologue that WF-034's seventh wave had
to repeat there. The clause is therefore stated in both places, and this case
is the one that fails when the fold-branch half is removed.

Hand-built, unlike its two siblings. The frontend has no grammar for a listed
`MAXIMUM` over an "en"-joined operand pair in this shape (measured: the
sentence is a syntax error), so this position carries no producer route to
reproduce; the two producer-authored cases pin the positions that do.

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

The same three refusals the other two positions give, which is why one rule
covers all three.

Falsifiable, arm-exact: drop the WF-096 clause from the fold-argument branch
(`spec/verifier/lib/checks.ml`) and this case turns clean again — and exactly
one other document moves, `wf034-sentinel-geleste-setlezing`, which loses the
co-firing WF-096 line it gained and keeps its WF-034 verdict. Over the four
zones (560 documents) nothing else changes at all.
