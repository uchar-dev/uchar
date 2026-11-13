# wf001-duplicate-rule-group

Violates WF-001 (`spec/IR-SPEC.md` ch. 2.1) in the rule-group namespace
(`rules.ruleGroups`, ch. 3.9): two rule groups whose names differ in case
alone. A scope `toetst de regelgroep` and a designation reach a group by its
name, so with two groups of one name they took the first.

    WF-001.namespace-collision FAIL /rules/ruleGroups: namespace 'ruleGroups':
    names 'premieberekening' and 'Premieberekening' collide case-insensitively

The document is `corpus/golden/jutterswet-vocabulaires` with its one group
`premieberekening` split in two: the first keeps three members and its
designations, the second, `Premieberekening`, takes the other two members and
designations of its own in both vocabularies, so that the effective names
differ and WF-118 stays silent. `expect.txt` pins the arm.
