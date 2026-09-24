# wf034-rollidmaatschap-rol-als-meervoud

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6), the role arm of the
`RoleMembership` node on its `role`: the name must be a DECLARED role name of
`factType`. The check "hij een wachtjutter heeft" on a `Wachtpost` names the
fact type `wachtindeling` and writes `role: "wachtjutters"` — the declared
PLURAL of the other role, `wachtjutter`. No role of `wachtindeling` is named
that, so the check resolves against no declaration.

    WF-034 FAIL /rules/resolvedRegels/0/voorwaarde/role: 'wachtjutters' is not
    a declared role of fact type 'wachtindeling' (a role check names the role
    by its declared name)

Why this spelling. `role` is the role the SUBJECT plays, by its declared name;
a plural is presentation and names no role here, as in a role binding. The
plural of the OTHER role is the spelling a producer carries over from the v0
node, whose written role name could name the far role by name or by plural
("heeft wachtjutters"). A reader that resolved plurals would find
`wachtjutter`, which the subject `Wachtpost` does not fill, and would answer
a different question than the one the sentence asks.

What the executors do (measured in the L2 fix round, 2026-09-24, with the
golden's three cases): `ref` and the ASP backend both refuse by name on every
case — `role check: role binding names no declared role 'wachtjutters' of
fact type 'wachtindeling' (the plural does not bind)`.

The document is `corpus/golden/jutterswet-postbezetting` with that one
`role` changed (rule 0, the first check). `expect.txt` pins the arm.
