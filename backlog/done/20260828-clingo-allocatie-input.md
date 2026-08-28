# clingo: case input on an allocation target — the S10 inputval default over verdeling

Goal: the fence at backends/clingo/lib/translate.ml (translate_verdeling,
"the case supplies input for allocation target") is replaced by the S10
defeasible-default machinery that already carries ladder/standing targets.
ref is the meaning and is NOT touched: ref (like v0) simply OVERWRITES the
target attribute when the verdeling writes it (World.write_attr,
RUNTIME-SEMANTICS ch. 4), and case input stands where the verdeling does not
reach. Concretely:
(1) translate_case emits case input for an allocation target as inputval/3
    instead of val/3 (extend the ladder_target test with an alloc_target
    test).
(2) A per-verdeling effectiveness helper (vd%d_eff-style) fires exactly when
    the verdeling derives a value for a target instance — same body shape as
    the val-heads, the established S10 pattern.
(3) The pass-through rule val(D,attr,V) :- inputval(D,attr,V), <typing>,
    not vd%d_eff(D) lets the input stand on unreached instances; where the
    verdeling reaches, its head wins and the input is invisible.
(4) STRATIFICATION FENCE: if the allocation target attribute is READ
    anywhere that can feed back into the verdeling's own source (pot),
    criteria, weights or condition — directly or transitively — the
    translator keeps faulting loudly (a negation cycle has no stable
    encoding here). The fence errs loud: only the provably acyclic case
    translates. Also keep faulting when the same target attribute is
    written by multiple verdelingen or by another rule kind, unless ref's
    behaviour for that overlap is pinned by a witness.
(5) The verdeling machinery itself (GELIJKE_DELEN, NAAR_RATO, maximum,
    groepeer, afronding, the onverdeelde-rest rule) is ALREADY translated
    (S12) and must not change semantics — this card only adds the
    input-default layer around it.

One rights-free, own-authored Jutterswet golden (hand-computed absolute
values, noop-refuting) witnesses BOTH halves at once: the verdeling
overwrites the supplied case input on reached instances AND the supplied
input stands on an instance the verdeling does not reach. The six toka pins
toka-32/33/34/35/36/37 move FAULT->PASS in toka/cases/parity.manifest
(clingo 22 -> 28 of 30). spec/ASP-TRANSLATION.md: S12 (and/or S10) gains the
input-default paragraph; the S19 sentence about case input on allocation
targets shrinks to exactly what remains fenced (the stratification refusal
and any other computed-target classes that stay loud). S19 stays the highest
section and shrinks only by translation.

Acceptance:
- Red-first: BEFORE the change, the clingo runner on each of the six toka
  ir/case pairs exits non-zero with the pinned fragment "the case supplies
  input for allocation" (evidence summarised without casus content), and the
  new golden faults the same way.
- After: bash scripts/check-toka-parity.sh green with the six pins at
  PASS|- (clingo 28 PASS / 2 loud faults: datum-tijd fold, non-numeric
  operand).
- The new golden passes gates 2-8; gate 12 plays it or carries a
  reason-carrying, evidence-backed exclusion (never silent). NOTE: the
  Drools CaseRunner pre-fills the bean from case input and the v0 runtime
  overwrites on allocation — expectation is that it PLAYS and matches; if
  it does not, triage honestly before excluding.
- The stratification fence has a red witness: an IR document where the
  target feeds back (e.g. the pot reads the target) still faults loudly —
  shown in the build log or as a corpus/negatief-style check ONLY if a WF
  rule exists for it; otherwise a demonstrated runner fault in the Result
  suffices (no new WF rule in this card).
- All twelve gates green in the worktree.
May touch: backends/clingo/**, corpus/golden/** (new golden only),
toka/cases/parity.manifest, spec/ASP-TRANSLATION.md, scripts/check-drools.sh
(ONLY for a reasoned exclusion of the new golden), backlog/.
Out of scope: ref/, spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/schema/,
spec/verifier/, frontends/**, backends/drools/** (code), the remaining fault
classes (datum-tijd fold, unit algebra / Onbekend), scripts/check-noop.sh
survivor entries (design the case to refute noop), spec/CONSTRUCTS.md,
spec/PLAN.md, any toolchain commit.

---

Result (landed on branch card/clingo-allocatie-input):
- backends/clingo/lib/translate.ml: the case_supplies fence in
  translate_verdeling is replaced by the S10 defeasible-input layer —
  translate_case emits inputval/3 for an allocation target (ladder_target
  extended with the alloc clause), emit_write derives a per-allocation
  vdI_eff/1 helper with the same body as each val head, and
  `val(D,da,V) :- inputval(D,da,V), is_a(D,dt), not vdI_eff(D)` re-admits
  the input on unreached instances. STRATIFICATION FENCE (narrowest safe
  form): the target read anywhere else, or written by a second form, still
  faults loudly — read/write detection reuses the verifier's own
  Ir_v0.Checks.dep_forms.
- New golden corpus/golden/jutterswet-veilinginvoer (GELIJKE_DELEN, pot 100
  over 3 jutters = 33 each, rest 1; J4 unreached keeps its input 5). Hand-
  computed in NOTE.md from RUNTIME-SEMANTICS ch. 4; noop-refuting; PLAYS in
  Drools (ref ≡ clingo ≡ Drools over all 6 expectations).
- toka/cases/parity.manifest: toka-32..37 move clingo FAULT->PASS
  (clingo 28 PASS / 2 loud faults).
- spec/ASP-TRANSLATION.md: S12 gains the input-default paragraph; the S19
  allocation-target sentence shrinks to the residual (read-elsewhere
  feedback + second-writer overlap).
- BIJVANGST (reported, in-scope as "what the six pins need"): toka-35 is the
  only unrounded proportional-with-maximum case; it was shadowed by the old
  case_supplies fence. A FENCE-ONLY narrowing (the emitted allocation
  machinery is unchanged) carries the proportional method with a cap no
  finer than its target (cdec <= tdec, so the clipped write is an exact
  integer at the target scale); the ordered method and a finer cap keep
  faulting. Zero corpus regression (gate 6 = 350 cases), zero silent
  divergence (gate 11).
- Stratification-fence RED WITNESS (not committed; verifier-dirty by design):
  an IR whose pot reads the target via a navigated SOM faults loudly
  "...read elsewhere: a feedback into the allocation's pot, weights,
  criteria or condition has no stratified negation-as-failure encoding...".
- All twelve gates green in the worktree (gate 9 catala via the opam switch,
  gate 12 drools PLAYS the new golden).

---

Result — review round (fix-and-close, 2026-08-28):

Two adversarial lenses (encoding-correctness; rights/scope/gates) each ran all
gates themselves and rated the card MERGE-READY. No CRITICAL. One MAJOR and
three minors; each handled with the narrowest fix or rejected with reason.

- MAJOR (lens 1) — the exact+maximum fence comment (translate.ml) and the S19
  spec sentence claimed the exactness viol refuses "exactly as ref faults it".
  Inaccurate: the Naar_rato exactness viol fires on the RAW share
  unconditionally (no cap guard), so in the clip-AND-inexact corner (e.g. pot
  100, weights 1/2, cap 10 on both) clingo refuses LOUDLY while ref writes the
  fitting integer cap and passes — clingo is STRICTER there, a conservative
  loud refusal, never a silent floor, and no corpus case exercises it. NARROWEST
  FIX = correct the claim in both places (translate.ml comment ~4586-4598 and
  spec/ASP-TRANSLATION.md S19). The machinery is untouched: changing the viol to
  match ref would alter the verdeling semantics the card forbids, and the corner
  is loud-safe (gate 6 = 350, gate 11 zero silent divergence).
- minor (lens 2, rights/process) — spec/ASP-TRANSLATION.md named `scenario-toka-35`,
  a private-zone ID, the sole toka reference in the whole product spec. Removed;
  the translation claim now stands on its mathematical argument (cdec <= tdec
  floors to an exact integer) plus the loud residual fence, decoupled from the
  private zone. Folded into the same S19 edit as the MAJOR.
- minor (lens 2, coverage) — the exact-Naar_rato-with-fitting-cap translator
  branch has only the private toka-35 witness. REJECTED for this round as a
  maintainer follow-up: adding a public golden exceeds "de smalste fix" for a
  card both lenses already blessed, and it is loud-safe with zero corpus
  regression. A future own-authored Jutterswet golden (exact proportional split
  with a clipping cap, exact-integer raws so clingo = ref) would close it.
- minor (lens 1, timeline) — `case_supplies` also trips on a timeline-typed
  target, but such input emits as tl/5 (never inputval/3) so the pass-through
  matches nothing. REJECTED: latent, no reachable bug (allocation targets are
  numeric scalars), out of this card's scope.

Gate re-run in the worktree (opam env; frontend jars in place; drools jar via
mvn -o package) — ALL TWELVE GREEN:
  1 self-contained PASS · 2 ir-schema PASS (149) · 3 verifier PASS (149/161) ·
  4 case-schema PASS (372) · 5 oracle PASS (350) · 6 clingo PASS (350) ·
  7 noop PASS (317 refute / 33 survivors) · 8 rood PASS (22) ·
  9 catala PASS (169 checks / 11 goldens) · 10 regelspraak PASS (130 tests /
  19 goldens) · 11 toka-parity PASS (clingo 28 PASS / 2 loud / 0 silent) ·
  12 drools PASS (played 50; the new golden PLAYS, no exclusion).

This round changed only prose: translate.ml comment and the S19 spec sentence.
No code semantics, corpus, manifest, or gate script changed. No merge, no push.
