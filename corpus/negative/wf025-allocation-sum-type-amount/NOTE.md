Violates WF-025 (`spec/IR-SPEC.md` ch. 3.2): the amount of an allocation is a
number. The golden `jutterswet-centdeling` with its `centopbrengst` declared
as a sum type of a number and the constructor `onbekend` (an absence marker,
ch. 2.4); the allocation still distributes it.

    WF-025.field-not-number FAIL /rules/allocations/0/versions/0/sourceAttribute:
    the field sourceAttribute of the allocation names 'centopbrengst' of
    'Centpot', of kind sumtype: the amount, share, rest, weight and maximum of
    an allocation are numbers

Measured before the arm (2026-11-12): the verifier passed this document, and
the executors had no reading of it: a pot of a sum type is no amount the
equations of `spec/RUNTIME-SEMANTICS.md` ch. 4.2 compute with. `expect.txt`
pins the arm and the field.
