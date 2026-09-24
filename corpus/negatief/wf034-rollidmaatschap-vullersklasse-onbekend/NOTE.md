# wf034-rollidmaatschap-vullersklasse-onbekend

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6), the role arm of the
`RoleMembership` node on its counterpart class: `counterpartType`, where it
is written, is a DECLARED object type ⊑ the declared filler of the other role
of `factType`. The check "hij een strandjutter heeft" on a `Wachtpost`
narrows the other end of `wachtindeling` to `counterpartType:
"Strandwachter"`, and no object type of the document is named that.

    WF-034 FAIL /rules/rules/2/versions/0/condition/counterpartType: role
    check's counterpart type 'Strandwachter' is not a declared object type: the
    check resolves against no declaration

Beside `wf034-rollidmaatschap-vreemde-vullersklasse` (a DECLARED type outside
the filler's chain), this is the other way the field can fail to resolve: the
class test has no class to test against.

What the executors do (measured in the L2 fix round, 2026-09-24, with the
golden's three cases): `ref` and the ASP backend both refuse by name on every
case — `role check: the counterpart type "Strandwachter" is no declared
object type (WF-034)`.

The document is `corpus/golden/jutterswet-postbezetting` with the counterpart
class of its first strandjutter check (rule 2) changed from `Strandjutter` (⊑
`Jutter`) to the undeclared `Strandwachter`. `expect.txt` pins the arm.
