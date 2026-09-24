# wf034-rollidmaatschap-vreemde-vullersklasse

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6), the role arm of the
`RoleMembership` node on its counterpart class (`counterpartType`, since the
L2 landing): the check "hij een strandjutter heeft" on a `Wachtpost` names the
fact type `wachtindeling`, the role `toegewezen wachtpost` the subject fills,
and narrows the other end to `counterpartType: "Wachtpost"`. The other role,
`wachtjutter`, is filled by `Jutter`, and `Wachtpost` is no subtype of it: the
check names a link whose other end can never be one, so it asks for nothing.

The document is `corpus/golden/jutterswet-postbezetting` with the
counterpart class written on its two strandjutter checks: `Strandjutter` (⊑
`Jutter`, the narrowing the golden's `case3-strandwacht` pins) on the
negated one, and `Wachtpost` on the other — the one defect.

    WF-034 FAIL /rules/resolvedRegels/2/versions/0/condition/counterpartType: the
    counterpart type 'Wachtpost' is no subtype of 'Jutter', the filler of the
    other role 'wachtjutter' of fact type 'wachtindeling': the check names a
    link whose other end can never be one

**Added at L2 (2026-09-24), with `counterpartType`.** Until L2 the far-side
role check named its partner type in a filler field, and the narrowing to a
subtype of the declared filler was carried there; the first L2 form of
`RoleMembership` had no carrier for it, and the migration folded the
strandjutter checks of the golden into its jutter checks — a change of outcome
the null measurement of the OCaml side found. `counterpartType` carries the
narrowing again, written only where it narrows, and WF-034 crosses it against
the declared filler of the other role. `expect.txt` pins the arm.
