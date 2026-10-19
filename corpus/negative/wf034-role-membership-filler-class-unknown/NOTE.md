# wf034-role-membership-filler-class-unknown

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6), the role arm of the
`RoleMembership` node on its counterpart class: `counterpartType`, where it
is written, is a DECLARED object type ⊑ the declared filler of the other role
of `factType`. The check "hij een strandjutter heeft" on a `Wachtpost`
narrows the other end of `wachtindeling` to `counterpartType:
"Strandwachter"`, and no object type of the document is named that.

    WF-034 FAIL /rules/rules/2/versions/0/condition/counterpartType: role
    check's counterpart type 'Strandwachter' is not a declared object type: the
    check resolves against no declaration

Beside `wf034-role-membership-foreign-filler-class` (a DECLARED type outside
the filler's chain), this is the other way the field can fail to resolve: the
class test has no class to test against.

What the executors do (measured in the L2 fix round, 2026-09-24, with the
golden's three cases): `ref` and the ASP backend both refuse by name on every
case — `role check: the counterpart type "Strandwachter" is no declared
object type (WF-034)`. Re-measured 2026-09-25, when the readers' sentences lost
their rule-number tail: the same refusal on every case in both, now without the
`(WF-034)`.

The document is `corpus/golden/jutterswet-postbezetting` with the counterpart
class of its first strandjutter check (rule 2) changed from `Strandjutter` (⊑
`Jutter`) to the undeclared `Strandwachter`. `expect.txt` pins the arm.
