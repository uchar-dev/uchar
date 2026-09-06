# wf034-sentinel-geleste-setlezing

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.3, the sentinel fence): the third
spelling of a LISTED fold operand carrying the unknown sentinel — the SET
reading. Where its two siblings read the fold subject's own attribute, this
operand reads a member attribute of another type (`set: true`,
`objecttype` = the fold's `elementObjecttype`), which is the shape ch. 4.3
calls the set source when it stands alone. Beside a second operand it is not
a source at all, and the sentinel is as ill-formed here as at any other read.

    WF-034 FAIL /resolvedRegels/0/waarde/argumenten/1/type: unknown-type
    sentinel on a listed fold operand: a fold with two or more arguments is
    the listed value fold, whose operands are ordinary reads — the sentinel is
    admissible on the SINGLE-argument instance-fold source alone (the v0 row-1
    wart, ch. 4.3)

The document. A `MAXIMUM` with two arguments and `elementObjecttype:
"Vondst"`; operand 0 an ordinary read of the subject's `drempelgewicht` with
its declared type, operand 1 a set read of `gewicht` on `Vondst` under the
sentinel. The hop it makes is legitimate — WF-053 resolves it against the
declared relation and says nothing — so the sentinel is the document's only
defect. One fault.

This case exists because the decision of 2026-09-06 is ONE decision for ALL
read positions, and `set` is the one field that could have been read as a
second reading of the operand. It is not: exactly as in WF-093, the sentinel
decides and `set` only describes.

Rood-eerst, measured on this document. Pre-rule verifier
(md5 `2ab63519d103a0abee721296973a61d7`): `verifier: PASS`. With the clause
(md5 `8660a9ef58134b0aaea1f8947fff6bcf`): the rejection above.

Executors, on this document (case: one Jutter of drempelgewicht 12.00 kg
linked to two Vondsten of 12.00 and 7.00 kg):

    ref         : EVALUATION FAULT: unsupported AttribuutRef shape
                  (set/sentinel outside a fold)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE, transpiler SKIP — the rule's value is out of the
                  backend's scope, so the form is refused per form and never
                  fires

Three dead ends, three different kinds of dead end: over the three subforms
of this one position the third executor plays one, throws on another and
declines this one. That spread is the argument for refusing the shape at the
wire rather than in three places.

Falsifiable, arm-exact: drop the sentinel clause at the head of the
fold-argument branch's reference arm (`spec/verifier/lib/checks.ml`); this
case turns clean along with its two siblings and nothing else moves over the
537 documents of the four zones.
