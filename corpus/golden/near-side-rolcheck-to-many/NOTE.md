# near-side-rolcheck-to-many

*Until L2 a red case, since L2 a golden* (`spec/proposals/V1-WIREMIGRATIE.md`
B9; `spec/CONSTRUCTS.md` row 5; the L2 card,
`backlog/done/20260923-l2-referentielaag.md`).
"Een Wachtpost is bemand indien hij een toegewezen wachtpost is" makes the
**Wachtpost** the subject of the role check. Before L2 the check named the
filler on the other end and was read as a null-test on the subject's
reference to it; from a Wachtpost that reference is the to-MANY role
`wachtjutter`, and both executors refused the shape. Since L2 the check is
`RoleMembership { factType: "wachtindeling", role: "toegewezen wachtpost" }`:
it holds iff a link of `wachtindeling` exists in which the subject fills
`toegewezen wachtpost` (IR-SPEC ch. 4.5). That is an existence question
over the relation, so how many links the far end holds is no part of it and
no longer a fault.

**Hand computation** (`case1`). J1 and J2 are both assigned post W1, so two
links of `wachtindeling` exist in which W1 fills `toegewezen wachtpost`;
W2 is assigned to no one.

| instance | links in which it fills `toegewezen wachtpost` | `bemand` |
|---|---|---|
| W1 | (J1, W1), (J2, W1) | present |
| W2 | none | absent |

Two links read exactly as one would: existence, never a count and never a
pick.
