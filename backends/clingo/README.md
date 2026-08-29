# backends/clingo — the ASP backend

The second executor: a systematic translator from `regelspraak-ir-v0`
documents plus `uchar-case-v0` cases to ASP-Core-2, run by clingo. With
it the differential is real: gate 6 (`scripts/check-clingo.sh`) demands
that clingo's stable model matches the same absolute expectations gate 5
pins for the reference interpreter — two semantically alien executors,
one answer, over every corpus case. Every divergence is by definition a
spec bug.

## Translation (spec/ASP-TRANSLATION.md's conventions, systematised)

- Instances are constants; a characteristic is `heeft(Inst, "naam")`; an
  attribute value is `val(Inst, "naam", V)`; a role link is
  `rol(Bron, "rolnaam", Doel)`; a parameter is `param("naam", V)`.
  Names are normalised on both sides.
- Exact arithmetic by declared-decimals integer scaling: a value of an
  attribute declared with d decimals is the integer value × 10^d;
  comparisons harmonise scales by multiplication. Clingo's integer
  division on the allocation's non-negative operands is exactly the
  floor the recurrence prescribes.
- Emptiness is absence: an underivable `val/3` atom is leeg, and an
  ordering comparison over leeg fails through the unbound body join —
  the ch. 1.2d ordering rule for free. The equality/inequality empty
  asymmetry has no such free encoding and faults until a corpus case
  demands it (recorded finding). Kin to it: a computed **leeg
  overwriting an input value** (ch. 1.2g) has no absence encoding on
  the native rule path — the input fact would stand beside the
  underivable computation, so a case pinning that write goes red on
  gate 6 (recorded limitation; the prioritized-defaults path carries the
  same situation correctly through its defeasible input).
- Negated characteristic reads are negation-as-failure — sound because
  WF-045/046 guarantee stratifiability; the verifier is thereby also
  the guard that a document is clingo-suitable — as it is for the
  to-one single-writer discipline: WF-052 statically excludes the two
  1:1 creations whose second filler `ref/` faults but a guardless
  model would silently hold.
- Object creation (IR-SPEC ch. 3.3; ch. 5.5) mints the
  created instance as a deterministic function term `ocN(Subject)` —
  set semantics carries the per-(rule × subject) idempotence, exactly
  the reference interpreter's deterministic label. Member
  initialisations are `val`/`heeft` heads on the term; the back-link
  relation (the explicit `verbindendeRol` binding, else the
  uniquely-derivable pair) lands both `rol/3` ends; the
  1:1 linked-instance guard is `not inputrol/3` over the case links
  only (ch. 5.5's input-snapshot narrowing). An ambiguous connecting
  relation faults the translation, as in `ref/`.
- A creation that is a member of a **recursive rule group**
  (IR-SPEC ch. 3.9) grounds along a generation depth: every input
  instance is `depth(_, 0)`, the creation's body carries
  `depth(S,D), D < oc_budget`, and the mint derives `depth(ocN(S),
  D+1)` — so gringo grounds the chain exactly as far as its condition
  carries it (the nested terms ARE the generations), and a chain that
  reaches `oc_budget` (`#const`, 100 — the same bound as the reference
  interpreter's pass budget; ref counts passes, this side generations
  per chain, and one chain advances one generation per pass) derives
  the named witness `viol("generation budget exhausted", _)` → UNSAT
  where the reference interpreter's pass budget faults. Ch. 5.5's
  bounded budget, never a silent truncation.
- Fact creation (IR-SPEC ch. 3.4; ch. 5.5) derives `rol/3`: the join
  chain (anchor → tussenstappen → joinRol → target) becomes a body
  join over `rol/3`, and the head lands the created relation's links —
  both ends, the same two directions the reference interpreter's
  writer lands. Set semantics carries the ch. 5.5 idempotence: a
  target reached along two paths is one atom. A to-one end gets an
  explicit cardinality constraint, so a second distinct filler makes
  the program UNSATISFIABLE exactly where `ref/`'s writer faults
  (ch. 5.5) — the allocation-guard pattern, never a silent wrong
  model. Only the symbolic extension form (WF-028) translates; a
  bean-field-only v0 record faults, as in `ref/`.
- Aggregation is `#count`/`#sum` with the contributor skip encoded by
  the body join; consistency and uniqueness derive `inconsistent/2`
  facts (the §9.5 channel); dates are civil day numbers; the temporal
  total folds the asOfDate year bucket (factor 1).
- The temporal read (`TemporalRead`, IR-SPEC ch. 4.3a) is a
  segment-containment join: case timelines become `tl(Inst, A, From,
  To, V)` / `param_tl(P, From, To, V)` facts over civil day numbers,
  and the read is `From <= D, D < To`. A gap or an empty date index
  leaves the body unbound — leeg as absence, exactly ref/'s
  leeg-on-gap and empty-index rules.
- The `Rekendatum` constant is the session's evaluation date as a
  program constant (ch. 8.2), and the ch. 8.4 session-start
  Parameterset coverage check runs before a program is emitted — the
  shared data-plane validation of `uchar-ref` (like the case loader),
  so an uncovered vintage read faults identically in both executors
  instead of ever reaching clingo.
- The prioritized defaults (IR-SPEC ch. 6.1) are the ch. 7.5 `decides_m`
  lowering verbatim: one decides predicate per default, outcome heads
  guarded by `not decides_m` for every higher default — sound NAF because
  WF-051 makes displacement edges negative (the verifier remains the
  guard that a document is clingo-suitable). A declared-unordered
  group adds the ch. 7.2 layer: per-member outcome predicates, a
  pairwise conflict rule depositing `inconsistent/2` keyed by the
  target form, and outcome heads plus the defeasible input both
  guarded on the conflict — a conflicting group derives no `val`, so
  the input value stands, exactly ref/'s no-write.
- v0's initialisation-plus-computation pairs are read through the
  ch. 7.7 desugaring (`Ir_v0.Desugar`) before translation, so the
  defaults machinery carries them — while `ref/` replays the v0 forms
  natively; the gate-6 differential thereby witnesses the desugaring
  itself. On attribute defaults
  the consequence bindings sit inside `decides_n`, so a leeg
  consequence leaves it underivable — the gevuld gate for free — and
  the input value of a defaults target enters as `inputval/3`, standing
  defeasibly (`val :- inputval, not decided`) exactly where ref/'s
  write overwrites it; the initialisation default's justification is the
  absence of that input. The ch. 7.8 **standing targets** (a bare
  scalar computing rule as its target's only writer,
  `Ir_v0.Desugar.standing_targets`) take the same entry: the rule's
  effectiveness atom (`standN_eff`) carries the full val-head body —
  condition AND value bindings — so a leeg evaluation derives neither
  and the input stands (`val :- inputval, not standN_eff`), matching
  ref/'s no-write-on-leeg. The temporal arm mirrors it
  (`standing_temporal_targets`): a standing pro-rata target's input
  timeline enters as `inputtl/5`, standing WHOLE unless the fold
  derives a bucket (`prN_eff`, the full bucket body incl. the
  exactness guard) — the whole-timeline reading of RS ch. 7.8. The
  ch. 6d computed-target fence no longer applies to standing targets
  (scalar or temporal); it remains on multi-writer and versioned
  computing targets, and an allocation `doelAttribuut` supplied by the
  case is fenced the same way.
- Constructs outside the corpus-exercised subset fault the translator —
  fail fast, never a silent SKIP.

The backend reuses the loader (`uchar-ir-v0`) and the case loading and
value lexing of `uchar-ref` (IO). The connecting role of an allocation
or fold comes from the node's explicit `RolBinding` when the document
carries one (the relations-first-class delta, WF-047/048); only for
bindingless v0 dumps does the backend still share the type-pair
derivation with `uchar-ref` (`Eval.connecting_role`, the recorded
ch. 4.1 wart — WF-048 guarantees it is unique there, and it retires
with the v1 format bump). Both routes end in the same
`role_canonical` convention the case facts use. All evaluation
semantics run in clingo; the hand-written absolute expectations keep a
shared-derivation bug from ever passing silently.

Recorded limitations:

- Clingo's term handling aborts (SIGABRT, observed in clingo 5.8.2)
  once function terms nest beyond roughly 200 levels — the recursive
  creation's generations ARE nested terms, so chains beyond that depth
  cannot exist on this backend regardless of budget; `oc_budget` (100)
  sits deliberately below the abort so budget exhaustion is always the
  named `viol/2` witness, never a crash.
- Clingo integers are 32-bit and wrap silently; the scaled encoding is
  exact only while every value and intermediate product stays within
  ±2³¹ (the allocation forms A×W before dividing). A mismatch fails
  loud against the absolute expectations, but a bignum/rational
  encoding is follow-up work for realistic monetary magnitudes.
- Negative or empty allocation operands make the generated program
  UNSATISFIABLE — clingo then fails exactly where `ref/` faults
  (ch. 4.4), instead of silently diverging by the truncation/floor
  difference. All fail-fast guards (these, and the to-one creation
  guard) derive named `viol/2` witnesses closed by one global integrity
  constraint; on UNSAT the runner re-runs without that constraint and
  reports the fired witnesses, so an expectedFault fragment
  (corpus/rood, gate 8) pins the *reason*, never merely "some
  constraint fired".

## Run

    dune build backends
    _build/default/backends/clingo/bin/clingo_runner.exe <ir.json> <case.json>

Exit 0: every expectation matches; 1: mismatch or translation fault;
2: load error or clingo unavailable.
