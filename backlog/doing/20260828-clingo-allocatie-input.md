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
