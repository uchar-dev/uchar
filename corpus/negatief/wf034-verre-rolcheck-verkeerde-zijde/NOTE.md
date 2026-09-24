Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6), the role arm of the
`RoleMembership` node (D11): the condition on the subject `Wachtpost` names
the fact type `wachtindeling` and its role `wachtjutter` — a role of the
right fact type on the wrong side. `wachtjutter` is filled by the `Jutter`;
the `Wachtpost` fills `toegewezen wachtpost`. The check therefore names a
link the subject can never be in, and every name in it resolves.

    WF-034 FAIL /rules/resolvedRegels/0/versions/0/condition/role: the subject
    'Wachtpost' does not fill the role 'wachtjutter' of fact type
    'wachtindeling', which is filled by 'Jutter': the check names a link the
    subject can never be in

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 this was
the far-side role check, a node carrying a filler type and a back-reference,
whose back-reference named the filler's role instead of the subject's. L2
merged the near-side and far-side checks into `RoleMembership` (B9), and the
migration derives its fact type and role from exactly the pair this witness
gets wrong — it refused the document, finding no candidate. Written by hand
in the merged form, with the same mistake: this is the red-first proof V4
(b) asks of the v1 role arm, which crosses the fact type, the role and the
subject's filling together. `expect.txt` pins the arm (U1).
