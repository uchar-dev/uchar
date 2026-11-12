Violates WF-025 (`spec/IR-SPEC.md` ch. 3.2): the weight of a proportional
allocation is a number. The golden `jutterswet-centdeling` with its
`centgewicht` declared as a text.

    WF-025.field-not-number FAIL /rules/allocations/0/versions/0/ratioAttribute:
    the field ratioAttribute of the allocation names 'centgewicht' of 'Jutter',
    of kind text: the amount, share, rest, weight and maximum of an allocation
    are numbers

Measured before the arm (2026-11-12): the verifier passed this document, and a
text pot and share alike. `expect.txt` pins the arm and the field.
