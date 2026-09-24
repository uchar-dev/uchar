Violates WF-035 (`spec/IR-SPEC.md` ch. 4.6): a power whose base operand
carries a unit — `aantal aangemelde vondsten` of `Jutter` is declared with
the unit `jr`, and the condition raises it to the power 2.

    WF-035 FAIL /rules/rules/0/versions/0/condition/left: power
    operands must be unitless

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the base
operand was a read of a unitless attribute whose own `type` annotation
claimed `jr`; with the annotation gone (B13) the read is typed by its
declaration, and the migration's translation was verifier-clean. Written by
hand: the unit now sits on the declaration the operand reads, so the base
genuinely carries it. `expect.txt` pins the arm.
