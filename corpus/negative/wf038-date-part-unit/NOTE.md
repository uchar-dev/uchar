Violates WF-038 (`spec/IR-SPEC.md` ch. 4.6): every part of a date construction
is a whole unitless number (`spec/RUNTIME-SEMANTICS.md` ch. 3.8), and a unit is
a property of the declaration, refused statically as for the Easter year. The
red case `date-part-not-whole` with its month part `vervalmaand` declared as a
whole number in the unit `dg`.

    WF-038.date-part-unit FAIL /rules/rules/0/versions/0/value/arguments/1:
    a part of dateWith carries the unit 'dg', must be a whole unitless number

Measured before the arm (2026-11-12, month value 2): the verifier passed this
document; ref faulted with EF-046 at evaluation, clingo refused it with
CLINGO-048, and both Drools branches played it with the unit dropped. A part of
a type with decimals stays the evaluation fault EF-046 (`corpus/red/date-part-not-whole`).
`expect.txt` pins the arm, the builtin and the unit.
