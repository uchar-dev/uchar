# wf034-sentinel-builtin-operand

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.3, the sentinel fence): the v0
unknown-type sentinel sits on the operand of a **single-argument BUILTIN** that
carries an `elementObjecttype`. A builtin has no fold source — nothing about it
is the ch. 4.3 row-1 wart — so the operand is an ordinary read and the sentinel
is as ill-formed there as at any other read.

    WF-034 FAIL /rules/resolvedRegels/0/waarde/argumenten/0/type: unknown-type
    sentinel on a fold operand that is not a fold SOURCE: the sentinel is
    admissible on the single argument of an AGGREGATION alone (the v0 row-1
    wart, ch. 4.3) — with two or more arguments the node is the listed value
    fold, whose operands are ordinary reads, and at a non-aggregation kind
    there is no source at all

**Why this document exists.** The seventh wave first drew its exemption on the
ARITY alone: one argument meant "this is a fold source, leave it to WF-093".
That is true of an aggregation and false of everything else. An
`ABSOLUTE_WAARDE` — or a `WORTEL`, or a date extractor — carrying an
`elementObjecttype` has exactly one argument and no source whatever, so the
arity guard handed it the wart's licence. The fix round redraws the exemption
on the SAME pair WF-093 keys on, arity **and** kind (the six value folds plus
`AANTAL`), which is what makes the two rules meet edge to edge instead of
leaving a gap between them.

**Not every builtin spelling was open**, and that matters for reading this
witness: of the six hand-built spellings measured, three were already fenced —
`elementObjecttype` equal to the fold subject is WF-048 (`bindingless instance
fold: no fact type relates …`), an operand whose `objecttype` is the element
type rather than the anchor is WF-083 (c), and a `bronRol` beside it is
WF-047 (`the written fold source … does not denote the bound role …`). All
three still reproduce. Three were NOT fenced: this one, the wart verbatim
(`wf034-sentinel-builtin-wratoperand`) and the same read spelled `set: true`.
This document and its sibling pin the two that behave differently in the third
executor; the set spelling shares this one's reason and this one's message.

The document. A single-argument `ABSOLUTE_WAARDE` with `elementObjecttype:
"Vondst"` — a type the model's one fact type relates uniquely to the subject
`Jutter`, which is what keeps WF-048 quiet — over a read of `drempelgewicht`,
a genuinely declared numeric attribute of `Jutter`, carrying the unknown
sentinel instead of that declaration. One fault, at that operand.

Rood-eerst, measured on this document. Pre-fix verifier
(md5 `c95650b1ab993c8a8b370e5b0617b0e6`, the seventh wave with the arity-only
guard): `verifier: PASS`. With the widened guard
(md5 `8ea45e866dddcf861b357b430d50b4d1`): the rejection above.

Executors, on this document (case: one Jutter, drempelgewicht 12.00 kg; the
pinned answer would be 12.00):

    ref         : EVALUATION FAULT: unsupported AttribuutRef shape
                  (set/sentinel outside a fold)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : PLAYED — it fires and answers 12.00 (confirmed against a
                  deliberately wrong pin: `verwacht 99.00, werkelijk 12.00`)

One answers, two refuse, under a document the verifier passed — the class
PLAN §6 calls a spec bug, and the reason the arity-only boundary could not
stand.

Falsifiable, arm-exact: revert the KIND half of the guard, leaving
`List.length f.argumenten = 1` alone (`spec/verifier/lib/checks.ml`). Swept
over golden + rood + negatief + parity (546 documents), exactly this case and
`wf034-sentinel-builtin-wratoperand` turn clean and **nothing else moves at
all** — no verdict, no report line. Inverting the arity half instead frees the
listed-operand witnesses and newly rejects 12 documents (10 goldens, 5 of them
anchors, and 2 red cases), which is what pins that half where it is.
