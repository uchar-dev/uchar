# wf009-declaration-system-mismatch

Violates WF-009 (`spec/IR-SPEC.md` ch. 2.5): a declaration in a unit system's
`units` whose `system` names another declared system than the one that
declares it. The names are compared under the identity by which WF-034 resolves
a system (ch. 4.6 (r)).

    WF-009.declaration-system-mismatch FAIL /model/unitSystems/1/units/0/system:
    the unit system 'Massa' declares the unit 'kg' as a unit of the unit system
    'Gewicht': a declaration names the system that declares it

The document is `corpus/golden/jutterswet-stelselweging` with the `system` of
Massa's kilogram changed from `Massa` to `Gewicht`. `Gewicht` declares a `kg`
with the same conversion pair, so the arms on a unit where it occurs pass the
declaration, and the verifier reports only this arm.

**Why it matters.** A factor where a unit occurs is its declaration without
`symbol` and `spellings` (ch. 2.5). A producer that copies this declaration
writes `system: Gewicht`, and the copy is then a unit of `Gewicht`: another
unit, which WF-091 tells apart from Massa's kilogram. No executor reads the
field on a declaration: on the golden's case file (2026-11-07) `ref` and clingo
pass its 37 expectations and both Drools branches play it, each finding a
declaration through the system that holds it.

`expect.txt` pins the arm.
