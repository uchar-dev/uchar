# wf096-aantal-setbron-wrat

Violates WF-096 (`spec/IR-SPEC.md` ch. 4.3) in the KIND half of 2026-09-19,
second spelling: the row-1 wart — a count source written as the unknown
sentinel on the argument's own `type` — carrying `set: true` beside it.

    WF-096 FAIL /resolvedRegels/0/waarde/argumenten/0: set reading on a
    fold operand that is not a VALUE fold's SOURCE: a set reading is
    admissible on the single argument of a value AGGREGATION alone
    (ch. 4.3) — with two or more arguments the node is the listed value
    fold, whose operands are ordinary reads; at a non-aggregation kind
    there is no source at all; and under AANTAL the fold reads the
    CONTRIBUTORS, never a value per contributor, so a set reading there
    names a collection the count does not consume

The document. `corpus/golden/jutterswet-vondsttelling` with its second rule
removed and three fields moved on the count's source: `set` from `false` to
`true`, and `objecttype` from the subject `Jutter` to the anchor `Vondst`,
which WF-083 (c) demands of a set read (with the subject annotation left in
place the document falls for THAT rule instead, and the set field is never
reached — measured). `attribuut` stays the role plural `vondsten` and the
`type` stays the sentinel, so the source is the wart in every other
respect. One fault.

Why this spelling is refused, stated plainly, because it is NOT the class
its sibling `wf096-aantal-setbron` belongs to. All three executors answer
this document, and they AGREE: `ref` PASSES the golden's pinned counts,
clingo PASSES them and the Drools backend PLAYS them — 3 for `J1`, 1 for
`J3`, the instance counts. `ref` matches the sentinel on its FIRST
fold-source arm, before its set arm and regardless of `set`; the other two
key their count arm the same way. The field is INERT here.

So what this case pins is the wire, not an executor gap: the sentinel says
the source hands the fold linked INSTANCES (WF-093, ch. 4.3) and `set` says
it hands the members' VALUES, one per member. One node, two contradictory
claims, three readers that happen to prefer the same one. That is WF-083
(c)'s question — a field nobody reads is a field that can say anything —
and the answer the repository gives elsewhere is one meaning, one spelling
(WF-102 (a) draws the same line between `bronRol` and `bronPad`). The
narrowing refuses both spellings of the count's set source in one clause
rather than carving the inert one out, which would have made the rule turn
on which executor currently ignores which field.

Rood-eerst, measured on this document. Pre-narrowing verifier
(md5 `441c25d84dc4cc54931ed8a5504b00f5`): `verifier: PASS`. With the
narrowing (md5 `3549d7ea72a43cd3164585e7f2955ae2`): the single rejection
above.

Falsifiable, arm-exact: the same one-disjunct mutant its sibling names
(`AANTAL` back into `set_source_exempt`) turns this case clean again, and
over golden + rood + negatief + parity (666 documents) exactly four
documents move.
