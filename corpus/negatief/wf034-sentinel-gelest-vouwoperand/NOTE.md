# wf034-sentinel-gelest-vouwoperand

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.3, the sentinel fence): the v0
unknown-type sentinel sits on a LISTED fold operand — an ordinary read, not
the single-argument instance-fold source the ch. 4.3 wart exception covers.
This is the first of the two subforms the decision of 2026-09-06 takes
together: the sentinel over a read that DOES resolve, v0's failed typing of a
real attribute.

    WF-034 FAIL /resolvedRegels/0/waarde/argumenten/1/type: unknown-type
    sentinel on a listed fold operand: a fold with two or more arguments is
    the listed value fold, whose operands are ordinary reads — the sentinel is
    admissible on the SINGLE-argument instance-fold source alone (the v0 row-1
    wart, ch. 4.3)

The document. A `MAXIMUM` with TWO arguments and an `elementObjecttype` —
that combination is the whole point: with an `elementObjecttype` the
arguments run through the fold-argument branch, which REPLACES the expression
walk, so the walk's own sentinel prologue never sees them; without one they
fall into the walk and the prologue fires as it always has. Operand 0 is an
ordinary read of `drempelgewicht`, carrying its declared type. Operand 1
reads `bergingsgewicht` — a genuinely declared attribute of the same object
type — but carries the unknown sentinel instead of that declaration. One
fault; the verifier reports WF-034 at that operand and no other rule.

What the shape bought itself while it was clean: the identity family defers
on the sentinel (`use_site_identity`), and the MINIMUM/MAXIMUM derived
expectation defers when any operand type is unknown, so one degraded
annotation switched off unit, precision, wholeness and domain checking at
that operand — the self-served exemption every WF-034 wave closes.

Rood-eerst, measured on this document. Pre-rule verifier
(md5 `2ab63519d103a0abee721296973a61d7`): `verifier: PASS`. With the clause
(md5 `c95650b1ab993c8a8b370e5b0617b0e6`): the rejection above.

Executors, on this document (case: one Jutter, drempelgewicht 12.00 kg,
bergingsgewicht 7.00 kg; the pinned answer would be 12.00):

    ref         : EVALUATION FAULT: unsupported AttribuutRef shape
                  (set/sentinel outside a fold)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : PLAYED — it fires and answers 12.00

Two refuse, one answers, under a document the verifier passed: the class
PLAN §6 calls a spec bug. The answer is not the point; that it is reachable
at all is.

Falsifiable, arm-exact: drop the sentinel clause at the head of the
fold-argument branch's reference arm (`spec/verifier/lib/checks.ml`). Swept
over golden + rood + negatief + parity (537 documents) exactly the three new
`wf034-sentinel-gelest-*`/`-geleste-*` cases turn clean, one report line
disappears in the parity zone, and nothing else moves. Inverting the arity
guard instead (`= 1` for `<> 1`) frees these three and newly rejects 12
documents — 10 goldens, 5 of them anchors, and 2 red cases — which is what
pins the boundary at the arity.
