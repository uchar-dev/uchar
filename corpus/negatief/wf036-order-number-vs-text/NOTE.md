Violates WF-036 (`spec/IR-SPEC.md` ch. 4.6): an ordering comparison between
a number and a text — `aantal aangemelde vondsten` (a number)
`GROTER_OF_GELIJK` the parameter `vondstdrempel`, declared `Tekst`.

    WF-036 FAIL /rules/rules/0/versions/0/condition/right: ordering
    operand is text

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the
parameter was declared numeric and only its reference's own `type`
annotation said text; with the annotation gone (B13) the reference is typed
by its declaration, and the migration's translation was verifier-clean.
Written by hand: the text type now sits on the parameter's declaration.
`expect.txt` pins the arm.
