# wf053-eigen-lezing-supertype

Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a), the arm of the read WITHOUT
navigation bindings: such a read is the subject's own read, and its
`objecttype` is the subject type itself — equality in the WF-001 form, not ⊑.
A supertype of the subject does not satisfy it.

The rule "het geborgen gewicht van een Munt is het brutogewicht van de Munt
plus 1 t" has the subject `Munt` (⊑ `Vondst`). Its left operand reads
`brutogewicht`, an attribute `Munt` inherits from `Vondst`, and writes
`objecttype: "Vondst"`, without navigation bindings.

    WF-053 FAIL /rules/resolvedRegels/1/versions/0/value/links: an attribute
    reference without navigation bindings is the subject's own read, so its
    objecttype is the subject type 'Munt' — 'Vondst' names another type, which
    only a navigation reaches

Why equality. The attribute resolves on `Vondst` as well as on `Munt`, so a
reader that admitted ⊑ would find the same declaration and answer the same
value; the defect is that two spellings would then name one read, and the
readers did not agree on whether the second one is admitted. Measured on the
L2 union (`bd2c48de`) with the golden's `case.json`: `ref` and the ASP
backend admitted the supertype (`oracle: PASS (8 expectations)`, `clingo:
PASS (8 expectations)`, the golden's answers), while the verifier refused it
here and the Drools backend required equality (the latter as the L2 lens
reported it; not measured again here). The L2 fix round made equality
the one rule for every reader (IR-SPEC ch. 4.1a); a read on a supertype names
the subject type itself or navigates.

The document is `corpus/golden/jutterswet-muntweging` with that one
`objecttype` changed from `Munt` to `Vondst`; nothing else fires.
`expect.txt` pins the arm.
