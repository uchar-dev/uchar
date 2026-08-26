# The band condition reaches ref and clingo

Goal: the re-measurement (backlog/done/20260825-toka-hermeting.md)
surfaced ONE scenario now stranding on a condition kind ref does not
carry — the "band condition" (it surfaced from behind the unit-literal
fault, so no exploration report covers it yet). INVESTIGATE FIRST:
identify the exact wire form (own-authored .rgs through the toolchain
resolver in /tmp — the band/"ligt tussen"-style comparison or whatever
the kind proves to be), the production semantics in
RegelSpraakRuntime/DrlTranspiler (boundary inclusivity, empty operands,
mixed types), and ref's exact fault site. Then close it spec-first,
narrowest form, v0-mirror: RUNTIME-SEMANTICS clause + worked example
(pin the boundary corners: on-boundary, off-boundary, empty operand),
ref evaluation, clingo translation (or a motivated loud fault + S19
entry if the form genuinely cannot ride the integer encoding — justify
it), witness golden with hand-computed cases on both sides of each
boundary through both executors, WF clause + negatief only if a
genuinely ill-formed variant exists. RIGHTS RULE: the investigation
happens in the toolchain and /tmp; everything landing in uchar is
own-authored Jutterswet material.

Acceptance:
- Red-first: the verifier-clean own-authored repro faults in today's
  ref (fault text quoted); after the change both executors replay the
  golden and agree (gates 5–6), boundary corners pinned.
- All nine gates green with counts.

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md` (only if a
WF clause is needed), `spec/verifier/`, `ref/`, `backends/clingo/`,
`spec/ASP-TRANSLATION.md`, `corpus/golden/jutterswet-*`,
`corpus/negatief/` (only with a WF clause).
Out of scope: any other condition kind (narrowest change),
`spec/CONSTRUCTS.md` (serial at merge).

---

Result (2026-08-26, branch `card/band-conditie` on `card/typenpaar-fallback`):

**The "band condition" is a misnomer — the honest finding.** Investigated
scenario-toka-42-passagier-18-24 in /tmp (the one the re-measurement
labelled "band-conditie"). Its rule assigns the kenmerk "passagier van 18
tot en met 24 jaar" under an ALLE compound of THREE bullets: `leeftijd >=
volwassenleeftijd`, `leeftijd <= 24 jr`, and **`hij is een passagier`**. The
two age comparisons (the actual "band") ref ALREADY carries — they
evaluated clean once the unit-literal emitter fix let "24 jr" through, which
is exactly why the fault surfaced from behind it. The kind ref does not
carry is the third bullet: the **near-side rolcheck** (`Rollidmaatschap`,
§8.1.7). ref's exact fault site: `eval_cond`'s
`Rollidmaatschap | Verre_rollidmaatschap | Periode_conditie` arm →
`EVALUATION FAULT: condition kind not implemented in this card` (quoted from
the /tmp slice AND from the own-authored repro). So this card closes the
rolcheck, not a "ligt tussen" range operator (RegelSpraak has no such
operator; the grammar's `vergelijkingsTeken` is the only comparison, already
carried).

**Production semantics (RegelSpraakRuntime/DrlTranspiler §8.1.7, mirrored).**
The subject fills the role iff its to-ONE reference to the other end of the
fact type is set — a null-test on the connecting reference
(`model.verbindendeRol(subject, anderTeFiller)`); the "is geen" negation is
the inverse null-test. A to-MANY connecting reference is v0's marker (the
far-side "heeft een <rol>" is the distinct `VerreRollidmaatschap`
exists-join), so ref/clingo refuse it loudly — never a silent
first-filler existence test.

**Landed, narrowest, v0-mirror.**
- `ref/lib/eval.ml`: the `Rollidmaatschap` arm — `Eval.connecting_role`
  to `anderTeFiller`, a to-many refusal, then `role_links` non-empty as the
  null-test, negation flips. Only `Verre_rollidmaatschap | Periode_conditie`
  keep the old catch-all.
- `backends/clingo/lib/translate.ml`: `cond_atoms` Rollidmaatschap arm —
  positive is a bare `rol/3` join over the connecting role's canonical name;
  negation derives a per-subject helper `rmN(S) :- is_a(S,ot), rol(S,role,N)`
  read negated (subject kept bound by the head's `is_a`). Warning-free under
  `clingo --warn=all`; stable model equals ref's on the witness.
- `spec/RUNTIME-SEMANTICS.md`: new **ch. 5.8a The near-side rolcheck** (a
  PROTECTED path — this branch is therefore a proposal, left unmerged) with
  the four-corner worked example.
- `spec/ASP-TRANSLATION.md`: a **Role membership** translation entry; the
  stale S7 catch-all line no longer lists Rollidmaatschap as refused.
- `corpus/golden/jutterswet-wachtindeling/`: own-authored witness (Jutter /
  Wachtpost / feittype wachtindeling). J1 assigned post W1, J2 to none;
  expectations pin all four corners: link×positive (wachtdoend present),
  link×negation (vrijgesteld absent), no-link×positive (wachtdoend absent),
  no-link×negation (vrijgesteld present).
- `ref/test/test_ref.ml`: WV-5.8a (append-only) — the four corners plus the
  to-many refusal fault.
- `corpus/rood/near-side-rolcheck-to-many/`: own-authored red witness
  (Wachtpost as subject over the same `wachtindeling` feittype) pinning the
  reachable to-many connecting reference — verifier-clean, expectedFault
  {ref, clingo}, both executors fault on `wachtjutter`.
- `corpus/golden/jutterswet-wachtploeg/`: own-authored witness that folds
  the rolcheck INSIDE a counting composite (`TEN MINSTE 2` over a positive
  rolcheck member, a negation rolcheck member, and a kenmerk) — the
  differential guard for the broadened `cond_atoms` surface: both the
  positive `rol/3` join and the negation `rmN` helper fold under a
  non-monotone quantifier, ref ≡ clingo on all four instances.

**A rood witness for the to-many connecting reference; no WF clause /
negatief — justified.** The resolver emits a **bare** near-side
Rollidmaatschap whichever end is the subject: flipping the subject to the
far side of the SAME fact type — rule "Een Wachtpost is bemand indien hij
een toegewezen wachtpost is" — yields a verifier-clean node whose
connecting reference to the other filler (`Jutter`) is the **to-many** role
`wachtjutter` (§8.1.7's near-side "is een", distinct from the far-side
"heeft een" `VerreRollidmaatschap`; the node does NOT become
`VerreRollidmaatschap` or `Onopgelost`). This shape is therefore reachable
from ordinary well-formed RegelSpraak — **not** hand-crafted — so the
marker→loud-fault divergence needs a machine-checkable artifact. v0 lowers
it to a MARKER (a `false` DRL constraint / null WaardeFn — a silent no-op
completing the run); both executors here refuse loudly (ref at evaluation,
clingo at translation, through the shared `Eval.connecting_role`). That
runtime divergence is now pinned by
`corpus/rood/near-side-rolcheck-to-many/` (expectedFault {ref, clingo}),
mirroring the `ambiguous-backlink` precedent. No static WF rule is added:
v0 admits the shape statically (its marker is a lowering decision, not a
static rejection), the verifier already accepts Rollidmaatschap (it
references no attributes and adds no dependency edge), so the delta lives
at runtime, not in the verifier. `spec/IR-SPEC.md` and `corpus/negatief/`
untouched.

**Red-first (quoted).** own-authored `jutterswet-wachtindeling`, verifier
PASS, pre-change: ref `EVALUATION FAULT: condition kind not implemented in
this card`; clingo `TRANSLATION FAULT: condition kind not translated (no
corpus case exercises it)`. Post-change: ref `oracle: PASS (4
expectations)`, clingo `clingo: PASS (4 expectations)`. The originating
/tmp TOKA slice 42 now also replays `oracle: PASS` in ref (investigation
only; no TOKA material entered uchar).

**All nine gates green (worktree, re-run after the review fix):**
1 self-contained PASS; 2 ir-schema 142 validate+round-trip; 3 verifier 142
golden clean / 159 negatief rejected; 4 case-schema 364 valid; 5 oracle 342
replayed exactly; 6 clingo 342 replay (ref = clingo); 7 noop 307 refuters +
35 pinned survivors; 8 rood 22 both executors; 9 catala 169 checks, 11 catala
goldens byte-identical. `dune test` worked examples pass.

Branch left unmerged as the maintainer proposal (touches the protected
`spec/RUNTIME-SEMANTICS.md`).
