Violates WF-018 (`spec/IR-SPEC.md` ch. 2.10): the parameter `vondstdrempel`
is declared as an enumeration of the domain `Drempels`, and no domain of
that name is declared.

    WF-018 FAIL /model/parameters/0: parameter enumeration references
    undeclared domain 'Drempels'

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the
parameter's datatype was the v0 unknown-type node ("domein onvindbaar"). L2
took that node out of the datatype union (B16), and an unfindable domain is
spelled as a declaration naming an undeclared domain (D15); the migration
refuses the old node. Written by hand: the parameter names `Drempels`, and
the rule compares it with a literal of that domain for equality, so nothing
but the declaration is at fault. `expect.txt` pins the arm (U1).
