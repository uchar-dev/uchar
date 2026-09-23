Violates WF-024 (`spec/IR-SPEC.md` ch. 3.2): the COMPLEX allocation
placeholder is not admissible IR.

    WF-024 FAIL /rules/verdelingen/0/methode: allocation method COMPLEX is not
    admissible IR

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** As
`wf020-crossform-collision`: the allocation carried no source type and no
recipient role, and since L2 the role is required (B12). Written by hand
with the fact type `strandgoedverdeling`, `Jutter` as source type and the
`ontvanger` role bound; the method is untouched.
