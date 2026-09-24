# wf083-vouwbron-vreemd-objecttype

Violates WF-083 (`spec/IR-SPEC.md` ch. 4.3), the **anchor cross**, in its
non-set half: a fold source that is not a set read is read on the fold
SUBJECT, so its `objecttype` must name the subject. Here it names another
declared type, and the annotation and the declaration the executors actually
consult come apart.

    WF-083 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: fold-source
    objecttype 'Jutter' is not the fold's anchor 'Vondst' (a fold without a
    range reads the subject's own attribute): the executors resolve
    'bewaarvergoeding per maand' against the anchor, never against this field,
    so the two spellings name different declarations

The document. A Vondst-subject rule whose value is the ch. 3.6 temporal
total, `elementObjecttype: "Vondst"` (the subject, the tag v0 stamps on an
own temporal total), one operand: `bewaarvergoeding per maand` with
`objecttype: "Jutter"`, no range (so the subject's own read, D8 (iv)), no
navigation. `Jutter` really does
declare that attribute, at MAAND granularity, so every other rule that
crosses the reference against a declaration is satisfied — this is precisely
why the shape stayed verifier-clean: nothing in the document is wrong except
the type it says the fold is anchored at.

What the executors do with it. `ref`'s temporal-total arm carries the control
`norm aot = norm self.otype` outright; failing that, the document falls to
the instance-fold source gathering, where an operand that is neither a count
source nor a set read has no shape at all. The ASP backend states the
same anchor as a named function of the fold's reading, and refuses by
reason. Both were already loud before this rule existed; the verifier was the
one consumer that let the form through.

    ref         : EVALUATION FAULT: unsupported instance-fold source shape
    ASP backend : TRANSLATION FAULT: the temporal total of "bewaarvergoeding
                  per maand" is declared on "Jutter", not on the subject type
                  "Vondst": the carried arm totals the SUBJECT's own timeline
                  and would answer for the wrong object; ref refuses the shape
                  outright, so this arm must not answer either
    Drools      : fires and answers 10 — the Jutter's monthly rate, read on a
                  Vondst subject

The Drools reading is the shape's own indictment: the value that comes out is
the other object's, exactly the wrong-instance read the ASP backend names in
its refusal. Three executors, three outcomes, one verifier-clean document —
the divergence the plan calls a spec bug by definition.

Hand-authored, from the temporal-total golden's declarations: one rule, one
operand, the object type of that operand being the whole defect.

Falsifiable, arm-exact: in `aggregation_source_check`, let the anchor of the
cross fall back to the operand itself in the non-set case (`let anchor = if
set then Some element_type else Some otn`, the code of the day). Measured over
golden + rood + negatief + parity — 508 documents — exactly one report
changed and this case, alone, turned clean; its set-half sibling
`wf083-vouwbron-vreemd-elementtype` stayed rejected, and so did the two
self-denotation cases. Since L2 the anchor is chosen per reading of the
source: the same mutant is `Src_own -> Some otn` in place of `Src_own ->
subject`. Measured in the L2 fix round (2026-09-24) over 718 documents:
exactly one report changes, this one, which turns clean.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the source
lost its own `type` (B13); a `TOTAAL` without a range reads the subject's
own attribute (D8 (iv)), so its anchor is the fold subject, and its
`objecttype` still names another type. The rule still fires on the same arm,
and `expect.txt` pins it (U1).
