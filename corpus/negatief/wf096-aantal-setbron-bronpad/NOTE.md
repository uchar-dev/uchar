# wf096-aantal-setbron-bronpad

Violates WF-096 (`spec/IR-SPEC.md` ch. 4.3) in the KIND half of 2026-09-19,
third spelling: the count's set source with a MULTI-STEP range (`bronPad`,
WF-102) instead of a one-step `bronRol`. It is the form
`RUNTIME-SEMANTICS.md` ch. 3.4b consequence 2 speaks about — "a count over
the union counts ELEMENTS, never intermediate instances" — and after this
narrowing that clause has no wire form left to describe.

    WF-096 FAIL /resolvedRegels/0/waarde/argumenten/0: set reading on a
    fold operand that is not a VALUE fold's SOURCE: a set reading is
    admissible on the single argument of a value AGGREGATION alone
    (ch. 4.3) — with two or more arguments the node is the listed value
    fold, whose operands are ordinary reads; at a non-aggregation kind
    there is no source at all; and under AANTAL the fold reads the
    CONTRIBUTORS, never a value per contributor, so a set reading there
    names a collection the count does not consume

The document. `corpus/golden/anchor/jutterswet-bootberging` reduced to its
first rule and its two-step model (Jutter sails `gevoerde boten`, Boot
salvages `geborgen vondsten`; the Kist type and the `inpakking` fact type
are dropped with the rules that used them), with the `MINIMUM` turned into
an `AANTAL`: the target is a count attribute `aantal geborgen vondsten` on
`Jutter`, the fold type is the count's own, the `leegwaarde` is gone (a
count is never empty, ch. 1.2e) and the two-step `bronPad` is untouched.
One fault.

Why both spellings of a counting union fall. A `bronPad` is admissible only
on the SET source (WF-102 (c) — the non-set sources hand the fold something
else entirely), and a set source is no longer admissible under a count.
With `set: true` the node is this rule's; with `set: false` the range is
WF-102's. The two fences meet, so a count over a multi-step range is not
expressible at all, and that is recorded in ch. 3.4b rather than left for a
reader to derive.

Executors, on this document (the anchor golden's own chain case, trimmed to
the two-step model: `J1` reaches `V1` 3,00, `V2` 7,00 and `V3` 5,00 — three
elements, all filled; `J2` reaches `V4` 9,00 and `V5`, which carries NO
`gewicht` — two elements, one filled; `J3` sails a boat that salvages
nothing; `J4` sails nothing). The hand-computed element counts are 3, 2, 0,
0:

    ref         : MISMATCH J2 = 1, expected 2   (it counts the FILLED
                  VALUES; J1, J3 and J4 agree because no value is missing
                  there)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP — bronPad-stap 1 (Boot -> Vondst)
                  has no bean field (§13.3.9 opslagrichting)

So over a multi-step range the position is sharper than over a one-step
one: only ONE executor answers at all, and it answers against the clause
this very chapter states. Refusing the wire form leaves the clause standing
and the reading unamended; it waits for a carrier that a normatively
defined counting operation would give it
(`backlog/new/20260919-aantal-gevulde-gemiddelde.md`).

Rood-eerst, measured on this document. Pre-narrowing verifier
(md5 `441c25d84dc4cc54931ed8a5504b00f5`): `verifier: PASS`. With the
narrowing (md5 `3549d7ea72a43cd3164585e7f2955ae2`): the single rejection
above.

Falsifiable, arm-exact: the same one-disjunct mutant its siblings name
(`AANTAL` back into `set_source_exempt`) turns this case clean again, and
over golden + rood + negatief + parity (666 documents) exactly four
documents move.
