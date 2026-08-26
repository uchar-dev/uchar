# near-side-rolcheck-to-many

The near-side rolcheck (`Rollidmaatschap`, RUNTIME-SEMANTICS ch. 5.8a)
reads the subject's to-ONE reference to the other end of the fact type
as a null-test. This document flips the subject to the far side of the
same `wachtindeling` fact type: the rule "Een Wachtpost is bemand
indien hij een toegewezen wachtpost is" makes **Wachtpost** the subject,
so the connecting reference to the other filler (`Jutter`) is the
to-MANY role `wachtjutter`. This shape is reachable from ordinary,
well-formed RegelSpraak — the resolver emits a bare near-side
`Rollidmaatschap` whichever end is the subject — and it is
verifier-clean (the node references no attributes and adds no
dependency edge, so no static WF rule rejects it).

The pinned v0 runtime (`DrlTranspiler` / `ExecModelTranspiler`) lowers
this exact shape to a MARKER — a `false` DRL constraint / null
`WaardeFn` — a silent no-op that completes the run: a to-many
connecting reference cannot be a scalar first-filler existence test
(the far side needs the distinct `VerreRollidmaatschap` exists-join).
Execution here refuses to guess: the reference interpreter faults at
evaluation (`eval_cond`) and the ASP backend faults at translation
(`cond_atoms`), through the shared `Eval.connecting_role` derivation,
citing the to-many connecting role — a recorded delta, fail-fast over
silent degradation, mirroring the `ambiguous-backlink` precedent. A
well-formedness rule pinning connecting-role multiplicity would move
the refusal to static time, but v0 admits the shape statically (its
marker is a lowering decision, not a static rejection), so the delta
lives at runtime.
