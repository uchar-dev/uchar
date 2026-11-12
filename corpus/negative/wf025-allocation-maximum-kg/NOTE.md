Violates WF-025 (`spec/IR-SPEC.md` ch. 3.2): the maximum of an allocation
reduces to the unit of the share, since a share is capped at it
(`spec/RUNTIME-SEMANTICS.md` ch. 4.2). The golden `jutterswet-berging` with its
`bergingsmaximum` declared in kg beside the share `bergingsaandeel` in EUR.

    WF-025.unit-not-share FAIL /rules/allocations/0/versions/0/maximumAttribute:
    the field maximumAttribute of the allocation names 'bergingsmaximum' in
    unit 'kg', which does not reduce to the unit 'EUR' of the share
    'bergingsaandeel': the amount, the rest and the maximum are amounts of the
    share

Measured before the arm (2026-11-12): the verifier passed this document; ref,
clingo and Drools then failed it at evaluation, each without a code.
`expect.txt` pins the arm, the field and both units.
