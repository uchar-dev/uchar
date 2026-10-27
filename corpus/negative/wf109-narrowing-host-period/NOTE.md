Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)): a characteristic check under
the whole-period qualifier names a host strictly below its subject.

    WF-109.only-some-objects FAIL /rules/rules/4/versions/0/condition/host: the
    characteristic check is hosted on 'Wrakrecht', which no object bound here
    IS: only SOME objects of the subject 'Strandrecht' are one, so the host
    would hold for some of them and fault for the rest

Broken from `corpus/golden/jutterswet-ploegblokkade` by adding one declaration
and replacing one condition: `Wrakrecht` declares the monthly characteristic
`aangemonsterd`, and the condition of the rule `zeewaardig` (subject
`Strandrecht`) becomes "hij gedurende het gehele jaar aangemonsterd is", with the
host `Wrakrecht` the golden's own check carried.

Why the narrowing host stops at the qualifier. Without the qualifier a host
strictly below the one bound object is the narrowing host (ch. 4.6 (q2);
`RUNTIME-SEMANTICS.md` ch. 5.8b rule 4). Under it the check folds the
characteristic's true periods (ch. 3.9), and in both bean-model branches that
fold runs before the host is looked at — the reason the qualified check is one
of WF-109's three fences at the guard of a fact creation as well. The §8.2
family is another decision's; until it norms the shape, the flank stays
`only-some-objects` here.

Falsifiable in one direction, verifier-checked on 2026-10-27: delete the
`host` — NOT clean, and other rules: `aangemonsterd` is then read on
`Strandrecht`, which does not declare it (`WF-034.characteristic-undeclared`,
`WF-043.whole-period-characteristic`).
