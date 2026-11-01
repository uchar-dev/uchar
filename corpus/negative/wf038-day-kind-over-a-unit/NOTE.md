Violates WF-038 (`spec/IR-SPEC.md`): the counted unit of a duration is
`werkdag/m`. The document declares the day kind `werkdag`, but a day kind names
a declared unit, and `werkdag/m` is two factors. The sentence names the whole
unit.

A variant of `corpus/negative/wf038-duration-unknown-unit`. The Drools loader
refuses the document at load in the same sentence, at the same pointer
(`TypeSystemRefusalWitnessTest.a_day_kind_over_a_unit_is_refused_at_load`).
`expect.txt` pins the arm.
