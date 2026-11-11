Violates WF-033 (a) (`spec/IR-SPEC.md` ch. 3.9): every member name of a rule
group MUST resolve to a rule form, and here one names none.

A minimal hand-broken variant of `corpus/golden/jutterswet-vocabulaires/ir.json`:
in the non-recursive group `premieberekening` the member
`vondstbedrag van een jutter` is renamed to `wrakbedrag van een jutter`, which
no form of the document carries. The rule `vondstbedrag van een jutter` itself
is unchanged.

    WF-033.undeclared-member FAIL /rules/ruleGroups/0: group member
    'wrakbedrag van een jutter' does not resolve to a rule form

`expect.txt` pins the arm.
