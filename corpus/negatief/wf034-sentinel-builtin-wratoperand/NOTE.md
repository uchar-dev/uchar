# wf034-sentinel-builtin-wratoperand

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.3, the sentinel fence): the v0 row-1
wart is written **verbatim** as the operand of a single-argument **BUILTIN**
carrying an `elementObjecttype` — a declared ROLE plural in the `attribuut`
slot, under the unknown sentinel. A builtin has no fold source, so nothing
here is the wart the ch. 4.3 exception admits; it is the wart's spelling in a
position that cannot mean it.

    WF-034 FAIL /rules/resolvedRegels/0/waarde/argumenten/0/type: unknown-type
    sentinel on a fold operand that is not a fold SOURCE: the sentinel is
    admissible on the single argument of an AGGREGATION alone (the v0 row-1
    wart, ch. 4.3) — with two or more arguments the node is the listed value
    fold, whose operands are ordinary reads, and at a non-aggregation kind
    there is no source at all

This is the second of the two subforms the fix round of 2026-09-06 takes
together (the first is `wf034-sentinel-builtin-operand`, the degraded read of
a real attribute). They are one decision because they are one wire shape and
one rule; they are two documents because the third executor treats them
differently, and that difference is the evidence, not a detail.

The document. A single-argument `ABSOLUTE_WAARDE` with `elementObjecttype:
"Vondst"`, whose operand names `vondsten` — the declared plural of the
`vondst` role of the model's one fact type — on `objecttype: "Jutter"`, with
`navigatieRol: null`, `set: false` and the unknown sentinel as its type. The
name resolution stays silent by design: the type pair `Jutter`/`Vondst`
derives exactly one connecting role, so ch. 4.3's fallback admits the name and
the sentinel is the document's ONLY defect. That is what makes this a clean
witness for the fence rather than for the naming class.

Rood-eerst, measured on this document. Pre-fix verifier
(md5 `c95650b1ab993c8a8b370e5b0617b0e6`, the seventh wave with the arity-only
guard): `verifier: PASS`. With the widened guard
(md5 `8ea45e866dddcf861b357b430d50b4d1`): the rejection above.

Executors, on this document (case: one Jutter, drempelgewicht 12.00 kg):

    ref         : EVALUATION FAULT: unsupported AttribuutRef shape
                  (set/sentinel outside a fold)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : ERROR ConsequenceException … java.lang.IllegalStateException:
                  geen getter voor 'vondsten' op …facts.Jutter

The third row is the point. The bean-model executor does not decline this
shape and does not ignore the role name: it READS it, looks for a Java getter
of that name on the subject bean, finds none, and throws out of the
consequence. A verifier-clean document crashing an engine is the sharpest form
of the class — worse than the sibling's wrong-but-quiet answer, because
nothing downstream can distinguish it from an engine defect.

Falsifiable, arm-exact: revert the KIND half of the guard, leaving
`List.length f.argumenten = 1` alone (`spec/verifier/lib/checks.ml`). Swept
over golden + rood + negatief + parity (546 documents), exactly this case and
`wf034-sentinel-builtin-operand` turn clean and nothing else moves — no
verdict, no report line.
