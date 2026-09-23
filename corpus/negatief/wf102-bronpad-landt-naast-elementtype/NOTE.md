# wf102-bronpad-landt-naast-elementtype

Violates **WF-102 (d)** (`spec/IR-SPEC.md` ch. 4.3), the LANDING: the last
step's bound role does not fill the element type.

    WF-102 FAIL /rules/resolvedRegels/0/waarde/bronPad/2: the last step's bound
    role 'ingepakte kist' fills 'Kist', but the fold's elementObjecttype is
    'Vondst' — the path must land on the element type

The path walks one step too far: Jutter → `gevoerde boten` → `geborgen
vondsten` → `ingepakte kisten`, while the fold reads `gewicht` on a Vondst.
Every step resolves and every step JOINS — the landing is the only defect, and
it falls alone, which is what makes this the arm's own witness rather than a
document that fails three ways at once. Without the arm the fold would range
over Kisten and read a `gewicht` none of them declares, on a document whose
element annotation says Vondst throughout.

The sibling of WF-047's filler clause on `bronRol`, stated for the step where
the range meets the element type.
