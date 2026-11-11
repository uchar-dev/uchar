# backends/clingo — the ASP backend

The second executor: a systematic translator from `uchar-ir-v1`
documents plus `uchar-case-v0` cases to ASP-Core-2, run by clingo. With
it the differential is real: gate 6 (`scripts/check-clingo.sh`) demands
that clingo's stable model matches the same absolute expectations gate 5
pins for the reference interpreter — two semantically alien executors,
one answer, over every corpus case. Every divergence is by definition a
spec bug.

## Translation (spec/ASP-TRANSLATION.md's conventions, systematised)

- Instances are constants; a characteristic is `has(Inst, "naam")`; an
  attribute value is `val(Inst, "naam", V)`; a role link is
  `role(Bron, ("feittype", "rolnaam"), Doel)`, the fact type beside the
  role, since a link belongs to its fact type (ch. 5.7a) and two fact
  types may name a role alike; a parameter is `param("naam", V)`.
  Names are normalised on both sides. A scalar parameter the case does
  not bind has no fact, and binding is fail-fast (ch. 5.7): every read
  of it carries one `viol/2` guard in ref's own words, whose body is
  where evaluation reaches that read (ch. 5.11): the taken arm, a
  composite's or quantifier fold's open prefix, the barrier of every
  enclosing Let decided per read and, in a part that runs member by
  member, per member. Where a creation or a fact creation can add a
  member to a fold's range, the order of the added members is not
  normed (ch. 3.9a/5.7a), and ref's own order is no norm either: ref
  runs a fixpoint per stratum, so it can run a creation with a higher
  index first. A read that every allowed order reaches fires in ref's
  words; a read that one allowed order reaches and another does not
  fires in a sentence that names the unnormed order, whichever order
  ref keeps (maintainer decision 2026-09-23), a refusal of this backend
  (CLINGO-035) and no fault kind; a read no allowed order reaches fires
  nothing, but for [N4] below. That holds for a
  quantifier fold's matrix, for a Let barrier in a part that runs
  member by member, and for the value of a Let whose binder is read at
  such a place. For the Let barrier the pass before a creation is one
  of the allowed orders: ref runs a pass's rules before its creations,
  so the member a creation adds is absent in that pass, and so is a
  barrier only that member raises. `Let k = restpost in (som(bonus van
  werven die TEN MINSTE 1 schuiten [k > 0]) plus p)`, with one schuit
  created for each Werf and restpost leeg, refuses in the named
  sentence, where ref answers the fixpoint: its fault at p in the pass
  before the creation is replaced (RUNTIME-SEMANTICS ch. 5.7). The
  same holds for a short circuit that a created member settles at the
  fixpoint (decision of the main session, 2026-09-23): an `atLeast`
  composite's prefix or an `atLeast` fold's open prefix is open in that
  pass, and the read behind it, or the value of a Let whose binder such
  a read reads, gets the named refusal. The pass is followed through
  the reads ch. 6.3 counts positive (an `atLeast` fold's range, filter
  and matrix, an `all` fold's matrix, the members of an `all`, `the` or
  `atLeast` composite, and a comparison, a navigated characteristic or a
  role check that reads through a role, read there with the case's own
  links only); the read's own members are taken to be there, as a
  member a creation adds is there before the members added to its own
  ranges. `keur = 99 indien TEN MINSTE 1 [TEN MINSTE 1 schuiten
  [lengte > 5], p > 0]`, with one schuit of 8 created for each Werf,
  refuses in the named sentence, where ref again answers the fixpoint.
  So the named refusal also stands where ref plays on every order: two
  creations of 8
  and 3 for each Werf, each behind a condition that holds, and `TEN
  MINSTE 1 [lengte > 5, lengte > p]` over the schuiten — ref runs them
  in document order, reads 8 and stops, and [3, 8] reads p [N1]; the
  same creations under `Let k = restpost in som(lengte van schuiten
  die TEN MINSTE 1 [ALLE[lengte > 5, k > 0], lengte > p])`, where
  ref's barrier at 8 ends the Let [N2]. "Every allowed order" is
  decided per read, so where two reads of one binder each cover some
  orders and together all, the refusal is named although ref's words
  were due: two quantifier folds over the same created 8 and 3 whose
  matrices read k in opposite orders, under `Let k = p` [N3]. And
  "some allowed order" is decided per fold: each fold's range has an
  order model of its own, also where two folds range over the same
  list, so a named twin can join an atom that holds in one order with
  an atom that holds only in another. The refusal is then named where
  no allowed order reads the parameter: `TEN MINSTE 1 schuiten [TEN
  MINSTE 1 [lengte > 5, TEN MINSTE 1 van de schuiten van zijn werf
  [TEN MINSTE 1 [lengte < 5, lengte > p]]]]` over a created 8 and 3,
  where [8, 3] settles the outer fold at 8 and [3, 8] the inner one at
  3 [N4]. Where the guard is not exact otherwise, it errs in one of
  two directions:
  - Loud, a refusal where ref plays. [L2] A Let barrier that only a
    created member raises, or a
    short circuit that only a created member settles, counts the pass
    before the creation also where ref has no such pass for the read: in a
    form that ch. 6.1/6.3 puts above that creation (a rule, the condition
    of a creation or fact creation, a consistency assertion), or at a
    member ref makes only after the members that settle its short circuit;
    the translator knows neither the strata nor the order of ref's passes.
    `Let k = restpost in (som(lengte van schuiten die k > 0) plus p)`
    under the condition `ALLE schuiten [lengte > 0]`, with one schuit of 8
    created for each Werf and restpost leeg: ref answers leeg, clingo
    refuses in the named sentence; the same condition before `TEN MINSTE 1
    [TEN MINSTE 1 schuiten [lengte > 5], p > 0]` likewise. Ch. 6.1 puts a
    form with irrevocable writes strictly above what produces its reads,
    and ref checks a consistency only at the fixpoint of its stratum, so
    that same short circuit refuses in the named sentence where ref plays
    also as the condition of a creation, as a consistency assertion and as
    the condition of a characteristic assignment. And a rapport made for
    each Werf once it has a schuit of more than 5, with `keur = 99 indien
    TEN MINSTE 1 [TEN MINSTE 1 schuiten van zijn werf [lengte > 5], p >
    0]`: ref makes the rapport after the schuit and never reads p, clingo
    refuses in the named sentence.
  - Silent, an answer where ref faults. [S1] The Let lowering keeps a
    value behind an enclosing need on a path that does not read it, and
    the guard follows the lowering: `Let k = restpost in Let bel = (als
    q > 0 dan k anders 0) in ((1,00 plus bel) plus p)` with q = -5,
    restpost leeg and p unbound answers leeg where ref faults at p.
    Where bel is the first thing the body evaluates, the branch moves
    out of bel's value and the guard is exact. ([S2] is retired: the
    branch hoist puts the operand beside the branch behind the branch's
    condition, and that operand's read has a second guard without the
    arm's atoms.)

  The die/dat filter of a per-element total and the day-kind table read
  their parameter at translation, and its guard fires where ref reads it:
  the filter's own read in the fold carries the guard, and a member whose
  filter faults is no contributor to the §7.1 horizon fence; the table
  records the days on which the day kind's condition reads it (below).

  Each tagged entry, here and under Recorded limitations ([V1], [V2],
  [V3], [V4], [V5], [V6], [V7], [V8]),
  has a frame in the suite, named `README [tag]`, that pins it and
  goes red when the entry no longer holds.
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
  assignments are `val`/`has_atom` heads on the term; the back-link
  relation (the explicit `verbindendeRol` binding, else the
  uniquely-derivable pair) lands both `role/3` ends; the
  1:1 linked-instance guard is `not inputrole/3` over the case links
  only (ch. 5.5's input-snapshot narrowing). An ambiguous connecting
  relation faults the translation, as in `ref/`.
- In a document with an object creation in a **recursive rule group**
  (IR-SPEC ch. 3.9) every object creation grounds along a creation depth
  per evaluation stratum, ch. 5.5's creation budget: every minted
  instance has a depth in the stratum of the creation that minted it,
  and a mint at depth 101 is a fault, in the reference interpreter and
  here, each with "creation budget exhausted". `depth(X,K,D)` says that
  X is a mint at depth D in stratum K. The strata are the reference
  interpreter's, computed the same way over the verifier's dependency
  graph of the document as loaded. An input instance is depth 0 in every
  stratum of an object creation, and so is an instance a lower stratum
  made (`depth(X,K,0) :- depth(X,K0,D), D > 0, ocstratum(K), K > K0.`),
  since ch. 6.3 completes a lower stratum before a higher one starts.
  A creation of stratum k carries `depth(S,k,D), D <= oc_budget` and
  derives `depth(ocN(S),k,D+1)`: the depth goes on only from a subject
  stratum k made, which on a verifier-clean document is one on the
  creation's own WF-045 cycle, because a creation reads its subject's
  population and every other maker of its subjects sits in a lower
  stratum. The mint at depth 100 is made, and one at 101 derives the
  named witness `viol("creation budget exhausted", _)` → UNSAT. Ch.
  5.5's bounded budget, never a silent truncation. A mint whose
  condition waits for a link a fact creation lays, or for a value a
  later generation makes true, keeps its subject's depth, here and in
  the reference interpreter, and a fact creation mints nothing and
  carries no budget. The suite's `ch5.5 creation budget` frames pin both
  sides, each beside the reference interpreter: a chain of 100 mints and
  a chain of 50 + 1 + 50 in three strata play, a chain of 101 mints
  faults, and among them are the two forms on which a count of
  generations and the depth part:
  a closure of 102 hops that a recursive fact creation lays, and a chain
  whose every mint waits a generation for a link.
- Every creation of such a document also carries a clamp,
  `viol(…, S) :- isa(S, T), not hasdepth(S, k).`: a subject without a
  depth in the creation's stratum k would leave the creation silent
  under its depth guard. Only a mint of a higher stratum lacks that
  depth, and a verifier-clean document never gives a creation such a
  subject, by the population read above; the suite forges one in the
  program text and pins the witness. Without an object creation in a
  recursive group no depth is kept: no creation then mints twice in one
  chain of mints (it would close a dependency cycle WF-045 refuses), so
  a chain is no deeper than the document has object creations.
- The guards bound the grounding as well, and that is where its cost
  lies: gringo does not settle a chain's condition while it grounds, so
  a chain grounds as deep as its guard admits. A chain of a higher
  stratum grounds from every instance the lower chain could make: the
  50 + 1 + 50 frame grounds about 1.7 GB of rules and takes about 23 s
  (measured 2026-10-09), where a single chain of 100 takes well under a
  second.
- A member assignment takes the value at the moment of creation
  (ch. 5.5, Generations): ref reads it on the settled world of the
  generation before, and a later generation may change the value read
  without the assigned member following it. This encoding has no
  moment of creation; the assignment's head reads the stable model,
  which holds the end value. The two differ where a later generation of
  the creation's own stratum changes the value read, and there only a
  creation on a dependency cycle (WF-045 admits one through a recursive
  group) can change it. So a member assignment is refused by name
  (IR-SPEC ch. 5.5) when a writer of a node it reads lies on a
  dependency cycle through its creation, or is reached in the
  dependency graph from a creation on a dependency cycle in its
  creation's own stratum, in a version the case's date admits (below):
  a value a rule, an allocation or a defaults group computes there, also
  where a creation writes the same member, or a population or relation a
  creation on the cycle grows (the creation's own back-link included). A
  creation off the cycle can share the cycle's stratum, since a positive
  characteristic read is not stratum-strict: with the recursive fact
  creation "geef door", which
  makes an ervaren Wachter the maatgever of its opvolger, and a rule
  that marks a Wachter with a maatgever ervaren, a creation that gives a
  Haven a Rapport with the number of its ervaren wachters makes the
  Rapport in generation 1. Over W1 → W2 → W3, all wachters of the Haven
  and W1 ervaren in the input, ref answers 1 and the encoding, which
  reads the end value, would answer 3, so this backend refuses it. A
  creation in a higher stratum than the cycle reads the end value, as
  ref does, and stays carried. An
  attribute or characteristic that only creations write is set once per
  object, at its creation, so a read of it stays carried: the anchor
  `jutterswet-wachtersketen` reads its rang so.
  A member that a creation and a rule on the cycle both write is
  refused: with rang plus 1 and beginstand = zijn rang assigned, and a
  rule that sets the rang of a Wachter with an opvolger to 2, ref starts
  W1's opvolger at beginstand 1 while W1 ends at rang 2. The test reads
  the static dependency graph of `Ir_v0.Checks` (the graph ref
  stratifies) and ref's strata over it, not the values: a rule on the
  cycle whose value on an object is fixed at the object's creation is
  refused as well, and so is a writer on the cycle whose validity the
  case's date excludes. Only a creation version the date excludes is not
  asked, because it is not translated, and it does not count as a
  creation that reaches a writer either: it mints and links nothing,
  here and in ref, so no later generation brings a change from it. With
  "geef door" valid from 2027-01-01 and a case on 2026-01-01 the Rapport
  is carried, and ref and this backend both answer rapportaantal(H1) = 1
  with W3 not ervaren. On the worked example of ch. 5.5 (beginstand =
  zijn stand, the stand counting the opvolgers) ref answers
  opvolgerbegin(W1) = 0 and the encoding would answer 1; the suite's
  `ch5.5 generations` frames pin the refusals, each beside ref's
  absolute answer, and the carried forms, and the `ch5.5
  generations off the cycle` frames do the same for a creation off the
  cycle: refused in the cycle's stratum, also with "geef door" valid on
  the case's date, carried a stratum higher, beside the cycle out of its
  reach, where it reads only what a creation writes, and where the date
  excludes "geef door".
- The condition of a creation, an object creation or a fact creation, is
  read on the settled world of the generation before its own as well
  (ch. 5.5, Generations), and a later generation may make it false
  without undoing the creation; this encoding reads it on the stable
  model. So it is refused by name before solving (CLINGO-038,
  `creation-condition-reads-on-a-cycle`, parameters `creation`, `node`,
  `writer`) where it reads a node that the same writers as above can
  still change, unless the read stands on a monotone position of
  RUNTIME-SEMANTICS ch. 6.1: the reach of an `atLeast` fold or a
  positive role check (M1); the growing side of `greater`,
  `greaterOrEqual`, `less` or `lessOrEqual`, a count over its reach or an
  attribute that only unconditional counting rules write, with nothing
  that changes on the other side (M2); a positive characteristic check
  whose attributions are monotone themselves (M3). On the model of the
  golden `jutterswet-opvolgerquorum` with its case 4, the `all` matrix
  "merk kleiner dan 2" gives merk(W1) = 2 in ref, where the encoding
  answered 1; the six conditions of ch. 6.1 that hold on absence had no
  stable model. A count that grows by the creation's own product for its
  own subject ("het aantal opvolgers is groter dan 0" in "werf een
  opvolger") stands on a monotone position, but the assignment of the
  count by `#count` is not monotone in clingo's reading, so the program
  has no stable model and the runner refuses it (CLINGO-030); ref answers
  merk(W1) = 2. The suite's `ch5.5 creation condition on the cycle`
  frames pin each refusal beside ref's answer, and the carried forms.
- Fact creation (IR-SPEC ch. 3.4; ch. 5.5) derives `role/3`: the join
  chain (anchor → tussenstappen → joinRol → target) becomes a body
  join over `role/3`, and the head lands the created relation's links —
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
- The `evaluationDate` context value is the session's evaluation date as a
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
  itself. A cell of a dimensioned attribute is a target of its own there:
  the desugaring makes the defaults group on the cell's name, so the pair
  on one cell takes the defaults reading `ref/` answers (RUNTIME-SEMANTICS
  ch. 5.8c). A default's consequence is fitted to the target's declared
  decimals, as a rule's value is. On attribute defaults
  the consequence bindings sit inside `decides_n`, so a leeg
  consequence leaves it underivable — the gevuld gate for free — and
  the input value of a defaults target enters as `inputval/3`, standing
  defeasibly (`val :- inputval, not decided`) exactly where ref/'s
  write overwrites it; the initialisation default's justification is the
  absence of that input. Every writer of a scalar target stands in the
  target's **stack** (RS ch. 7.8), and its input takes the same entry: a
  computing rule that applies, of any datatype, derives its value under
  its index in the document as well (`wval/4`), an allocation under
  `vdN` for its share target and its rest, each with the full val-head
  body — condition AND value bindings — so a leeg evaluation derives
  neither and decides nothing (RS ch. 3.11); the input stands where no
  writer decides (`val :- inputval, not wdecides`), and two writers with
  different values on one object are the EF-016 witness below. The member
  value of a creation is the input of the object it makes: where a
  writer, a defaults group or an initialisation writes the member, the
  creation derives `inputval/3` (and `inputfilled/2`) on its term in
  place of `val/3`. On a time-dependent member the member value is the
  input of the made object per day: period arithmetic read on the subject
  (`translate_period_arithmetic` with the made object's type as the
  target's), a scalar on every day, whose periods enter as `inputtl/5` on
  the term where a writer, an initialisation or a defaults group writes
  the member, and as `tl/5` otherwise. An allocation that names a
  time-dependent attribute is not well-formed (IR-SPEC WF-128); the
  backend re-runs that check at the allocation and refuses the document
  in the verifier's sentence.
- A writer of a timeline target, also its only one, decides the days of
  the segments it writes (RUNTIME-SEMANTICS ch. 3.11, 5.7 per day). Each
  writer writes its segments as `tl/5` and also as `wtl/6`, under its
  index in the document. The input timeline of a target that a computing
  rule or an initialisation writes, the case's or a creation's, enters as
  `inputtl/5`; each input segment is cut
  at the bounds of the writers' segments inside it (`incut/5`,
  `inpiece/6`), and a piece that no writer's segment covers
  (`indecided/4`) is a `tl/5` segment of the target. A writer writes no
  leeg segment, so a day its timeline reads leeg is no day it decides,
  and an empty result leaves the input standing whole. Two writers that
  decide one day with different values are a named `viol/2` witness
  (EF-016); with one value, see [V7]. An initialisation outside the ch. 7.7
  desugaring is the lowest layer of the stack per day (RS ch. 7.8): its
  value, cut to the periods of its condition, is `itl/5`, each of its
  segments is cut at the bounds of the writers' and the input's segments
  inside it (`initcut/5`, `initpiece/6`), and a piece that neither covers
  (`initdecided/4`) is a `tl/5` segment. A sum-typed timeline takes a
  constructor of a literal or a copy of an own timeline of a sum type from
  each of these producers, a rule, a default, an initialisation and a
  member value alike; another producer of a constructor value per period is
  refused with CLINGO-036 at the head of the fault.
- A defaults group on a timeline attribute target decides per day
  (RUNTIME-SEMANTICS ch. 7.11): the chain and group predicates of the
  scalar lowering carry a period of the group's cut set, `grpG_d_per/3`,
  which holds every bound of the justifications' periods
  (`period_condition`), of the consequences' own cut sets (the period
  arithmetic, read as `p_val/4` without its write) and of the input
  timeline. A default decides a period where it is reached, its
  justification holds and its consequence reads a value; the
  initialisation default's justification is a period the input leaves
  empty (`grpG_in/3`). The decided periods, outside a period on which
  the members conflict, are written in maximal runs as `tl/5` and as
  the group's `wtl/6`, so the input stands on every other period as
  beside a rule. A deposit of a default stands where it decides on some
  period. A consequence is evaluated where its default is reached and
  justified on some period (`ndN_jrK/1`), a justification and the input
  the initialisation default reads where its default is reached on some
  period (`ndN_rchK/1`), and a fail-fast in them fires only there
  (`corpus/golden/jutterswet-gevolg-buiten-de-beurt`). The ch. 7.7
  desugaring of an initialisation beside at most one computation on a
  timeline target is such a group, and decides as ref's stack does per
  day. Refused by name: a consequence that is not period arithmetic (the
  time-proportional part, the day count).
- A fail-fast or a §9.5 deposit fires only where ref evaluates its place at
  the fixpoint (RUNTIME-SEMANTICS ch. 5.7): under a default that is reached,
  under the guard of an initialisation, and a deposit of a declared default
  only under its decision (ch. 7.1). A default the ch. 7.7 desugaring made
  from a v0 form deposits where it is evaluated, under its rule's name, and
  an initialisation on a timeline target, natively or desugared, where it
  decides some day.
- A form that reads the timeline of an attribute whole and finds the
  asOfDate-bucket scalar faults with EF-037 (RUNTIME-SEMANTICS ch. 8.4):
  the period arithmetic, a condition per period, the base of a pro-rata
  part, the value and the compared timeline of a gedurende clip, a
  member of a per-element total, a point read, the whole-period
  qualifier, also at the end of a path, and the input of an
  initialisation default and of an initialisation on a timeline target. Each instance that gives the scalar
  gets a `viol/2` witness in ref's sentence, with the instance as its
  subject, whose body is the reach of the read: the condition that gates
  the rule, the reach of a default's consequence or justification
  above, or, inside a condition or a value, the reach of the
  unbound-parameter guard (the taken arm, a composite's open prefix, the
  Let barriers), with the path or the set from the subject to the
  instance. An unbound parameter in the period arithmetic, time-dependent
  or a scalar operand, faults (EF-014) under the same reach. Where an atom
  of that reach hangs on the order of the members a creation adds, the
  witness takes the two forms of the unbound-parameter guard: ref's words
  where every allowed order reaches the read, the named sentence
  (CLINGO-035) where one does and another does not. A point read of an
  absent attribute faults in translation, without a code.
- Every other fault kind ref raises where it evaluates a node is a
  `viol/2` witness in ref's sentence under the reach of that node, with the
  same two forms: the bound day outside a day-kind evaluation (EF-010), a
  context value in a session without an evaluation date (EF-011), an
  `Unresolved` expression or condition (EF-019) and a filled count over a
  temporal attribute of a set (EF-021). The node's term derives nothing
  (`#false`): where the evaluation reaches it the program is
  UNSATISFIABLE, and where it does not, the form's answer does not hang on
  it. A clamp whose bound is such a context value stops after it, as ref
  does. The day-kind table records the days on which the day kind's
  condition faults — it reads a parameter the case does not bind (EF-014)
  or tests a day kind on another day (EF-022), its composite read in the
  order of ch. 5.10 — and a step faults where it walks such a day before
  it has found the days of the kind it counts, a count where such a day
  lies in its interval. A key with two temporal components faults for
  each instance of the type the uniqueness keys (EF-024), so over a type
  without instances nothing. The ambiguous back-link of a creation
  (EF-018) faults in translation, where ref faults wherever the creation
  is run. A walk that reaches a day that faults stops there, so no reach
  fence or window limit of the step fires after it; where the walk would
  pass the reach of 40 000 days without a day of the kind before it gets to
  that day, ref faults on the reach fence (EF-009) and this backend on the
  day, a limit no document of corpus or suite reaches.
- Two or more computing rules on one target whose populations share a
  concrete type (on one type, or on a type and its subtype) would give
  the target two values in one model wherever two of them apply to one
  subject and differ, where ref faults on its pass budget (ch. 5.1,
  5.7). Each writer derives its value under its own index (`wval/4`), a
  writer that fires without a value derives none and decides nothing
  (RS ch. 3.11), and every pair of writers that share a concrete type
  gets one named witness on two different `wval/4` values,
  `viol("two writers of one target disagree: rules "A" and "B" write
  different values to the attribute a of a T — no fixpoint, divergence
  is a fault (ch. 5.7)", S)`, so the program is UNSAT and never a second
  value. Writers whose conditions exclude each other, that write the
  same value, or of which one reads leeg, derive no witness
  (`corpus/golden/jutterswet-melding-naar-aard`,
  `corpus/red/jutterswet-dubbele-melding`). The case input stands only
  where no writer of the subject decides, a writer on another type of
  the chain included: the value at the fixpoint is that of the writers
  that decide there (`corpus/golden/jutterswet-twee-schrijvers-aan-fixpunt`).
  Writers of a timeline target are compared per day, by the item above.
- An initialisation outside the desugaring domain — a conditional one,
  which ch. 7.7 excludes, or a windowed one — translates natively, as
  ref replays it (RS ch. 5.4): its `val` head needs the condition and
  `not inputfilled(S,a)`, a case fact beside the untouched input `val`,
  so a supplied value stands, and on an object a creation made the
  creation derives it with the member value. Beside the writers of its
  stack — the computing rules that write a concrete type of its chain as
  a rule too and an allocation of the target — the head also needs `not
  wdecides(S,a)`: a writer's value wins. Where a rule fires and
  evaluates leeg it decides nothing (RS ch. 3.11), so the initialisation
  writes. On a timeline target it is read per day, by the item on the
  writers of a timeline target above. Two initialisations that write one
  concrete type as rules
  (WF-079 refuses them first), a defaults group of its own level
  (WF-050's) beside it, and a target that is no number or date are
  refused by name. A defaults group on another level of the chain is no
  co-writer: on the instances its stack runs on, the initialisation is
  folded into that stack with its condition (RS ch. 7.10), as the
  initialisation default whose body is `not grpN_has_input(S)` and the
  condition's atoms. Every default of a composed stack is translated on
  the level that declared it, so an own read in it names that type.
- Constructs outside the corpus-exercised subset fault the translator —
  fail fast, never a silent SKIP.

The backend reuses the loader (`uchar-ir-v0`) and the case loading and
value lexing of `uchar-ref` (IO). The connecting role of an allocation
or fold is the node's explicit `RoleBinding`: the wire writes every
range and every recipient role, and the loader refuses a node that omits
one (`spec/proposals/V1-WIREMIGRATIE.md` B12). The one relation derived
when absent is an object creation's back-link, through `uchar-ref`'s
`Eval.connecting_pair` (ch. 5.5). Every route ends in the same
`role_canonical` convention the case facts use. All evaluation
semantics run in clingo; the hand-written absolute expectations keep a
shared-derivation bug from ever passing silently.

Recorded limitations:

- Clingo integers are 32-bit and wrap silently; the scaled encoding is
  exact only while every value and intermediate product stays within
  ±2³¹ (the allocation forms A×W before dividing). A mismatch fails
  loud against the absolute expectations, but a bignum/rational
  encoding is follow-up work for realistic monetary magnitudes. The
  range guards on products, on sums and on an operand rescaled to the
  scale of the other (fenced before the multiplication, which itself
  can wrap) fire the witness
  `viol("arithmetic range exceeded …", _)`, the limit CLINGO-034; a
  number of the unbounded form whose declaration and values fix no
  finite scale is held as a whole number, and a division, a pro-rata
  part or a result written there that is not whole fires the limit
  CLINGO-033. Both are limits of this encoding, where ref writes the
  exact value; the anchor `jutterswet-verzuimrente-30-360` stands with
  both codes in gate 6's channel. A date step holds the day numbers up
  to 5000000-12-31 either way: its count is fenced before it is
  multiplied by its unit (CLINGO-034), and the day it reaches after it
  (CLINGO-037, "date arithmetic reaches a year after 5000000"), so a
  step never wraps; ref and Drools fault there only beyond the range of
  a date of RUNTIME-SEMANTICS ch. 3.8 (EF-007), some 146 million years
  out.
- A `TextComposition` and a `concatenation` are refused at translation
  as the limit CLINGO-042, also over texts alone: the text of a value
  (RUNTIME-SEMANTICS ch. 1.2, "Text coercion": `5,5`, `waar`,
  `21-06-2026`) needs string functions, ASP-Core-2 has none, and the
  encoding runs no script. The golden `jutterswet-tekst-van-een-waarde`
  stands with that code in gate 6's channel.
- The Let lowering keeps an enclosing Let's need on a path that does
  not read its binder, also with every parameter bound, so a value ref
  computes stays leeg. [V1] A Match with an arm that reads the binder,
  under an operator: `Let k = restpost in ((match klasse: Hoog → k,
  Laag → 0,00) plus p)` with klasse Laag, restpost leeg and p = 10
  answers leeg where ref answers 10,00. The branch hoist that moves an
  IfThenElse out of such an operator cannot move a Match: ref reads a
  Match over a leeg scrutinee as leeg (ch. 9.2) and still evaluates the
  operator, so the hoisted form differs exactly there. [V2] A branch in
  a Let's value where the body evaluates something before that Let's
  binder: the [S1] form with p = 10 answers leeg where ref answers
  11,00. [V3] A short circuit that settles before the binder's read:
  `Let k = restpost in som(bonus van werven die TEN MINSTE 1 [TEN
  MINSTE 1 schuiten [lengte > 5], k > 0, bonus > p])`, W1 with bonus 4
  and a schuit of 8, restpost leeg, answers leeg where ref answers
  4,00. With the schuit created, the pass before the creation reads k
  first, and its barrier ends the Let there before p, so no pass reads
  p and nothing is refused. The exact lowering, the need per path
  instead of on the whole body, changes the translation of every Let
  body.
- [V4] A division by a numeric zero in a closed body (a Fold or MapList
  body) yields leeg with a §9.5 deposit of EF-005 (RUNTIME-SEMANTICS
  ch. 1.4 point 4). A closed body lowers to one term per step and has no
  absence in it, so this backend carries that leeg only as an operand of
  plus, minus, times or percentage-of, where ch. 1.2 reads it as 0: the
  fold `acc + grondslag ÷ x` over [2, 0] with grondslag 60 answers 30,00
  with the deposit on the rule and the subject that read the fold, as ref
  does (golden `closed-runtime-division-by-zero`). Anywhere else in a
  closed body the leeg would propagate (the body's own value, the value of
  a Let whose binder is read, a rounding, a comparison), and a zero
  divisor there derives the named witness `viol("a division by a numeric
  zero in a closed body yields leeg …", _)` → UNSAT, where ref answers:
  `Let m = grondslag ÷ x in (als x ≥ 4 dan acc + 1,00 anders acc + m)`
  over [0] is leeg with the deposit in ref.
- [V5] The fences of a day-kind step (the reach fence, EF-009, and the
  window limits) carry the reach of the place beside the condition, as
  the §9.5 deposits do: in a quantifier fold's matrix the fold's stop,
  in a fold member or a default its range or its chain, so a member
  never reached walks no fence (ch. 3.9a; golden
  `jutterswet-keurtermijn`). In a document with an object creation or a
  fact creation the stop hangs on an order the spec does not norm, and
  the fences keep firing for every member, reached or not: `ALLE
  wachten [gekeurd ; keurdatum plus keurtermijn ligt na de rekendatum]`
  with W1 not gekeurd and W2 stepping from 2027-03-01 over keurdagen of
  2026 only, beside a creation of a Keurrapport for each Ploeg, refuses
  with the reach fence where ref answers.
- [V6] The temporal total that the calendar integrates (RUNTIME-SEMANTICS
  ch. 3.6: a rate per year, quarter or month over a grid of days or weeks,
  or per week over a grid of months) weighs a day by 1/len of its calendar
  unit, so its value has a denominator such as 365 or 366 that no decimal
  scale holds. The backend lays the weights over the least common multiple
  D of their denominators, carries the total at the scale of its
  value times D, and translates it only as the base of a rounding, which
  divides D away; anywhere else it refuses the total by name (CLINGO-032),
  where ref holds the exact value: the verzuimrente of the anchor
  `jutterswet-verzuimrente-dagen`, of the unbounded form, is 139 13/73 € in
  ref (gate 6's channel list; suite fixture `calendar-total-unrounded`).
- [V7] Two writers of one timeline target that decide one day with one
  value (RUNTIME-SEMANTICS ch. 5.7 per day) hold together. Ref writes the
  day in the segment of the writer the document names first; this backend
  writes each writer's segments as written, so the two would overlap, and
  it refuses that day by name instead: `wachtdagen` set to 30 in a whole
  month beside the day count of every month, over a November that is whole
  (30 days), is 30 on November in ref (suite fixture
  `twee-schrijvers-gelijk`).
- [V8] The temporal total counts a part of a unit of its grid by its
  days, over the days of the period of the grid that holds it
  (RUNTIME-SEMANTICS ch. 3.6), so where the case's bounds split a unit the
  weights of the spans have a denominator. The backend lays them over D,
  the least common multiple of those denominators, and divides D away
  again: under a rounding as in [V6], and elsewhere exactly, where D
  divides the total; a total D does not divide is refused at solving by
  the named witness `viol("… counts a part of a unit of its grid by its
  days …", _)`, where ref holds the exact value: 120 per year clipped to
  five days of 2024 is 100/61 in the unbounded form in ref (suite fixture
  `partial-unit/unrounded`).
- A negative pot of the equal-shares or the proportional method makes
  the generated program UNSATISFIABLE, instead of silently diverging by
  the truncation/floor difference; an empty pot distributes nothing and
  a negative proportional weight counts as 0 with a §9.5 deposit, as in
  `ref/` (ch. 4.4). All fail-fast guards (these, and the to-one creation
  guard) derive named `viol/2` witnesses closed by one global integrity
  constraint; on UNSAT the runner re-runs without that constraint and
  reports the fired witnesses, so the fault kind and its parameters pin the
  *reason*, never merely "some constraint fired" (corpus/red, gate 8). The
  place that writes a guard registers the code its witness stands for
  (`emit_viol`, `guardc`), and the diagnosis reads it back by the witness's
  reason (`Translate.viol_code`); a parameter is fixed there or read off the
  ground witness (its label, or a part of a tuple reason).

## Run

    dune build backends
    _build/default/backends/clingo/bin/clingo_runner.exe <ir.json> <case.json>

Exit 0: every expectation matches; 1: mismatch or translation fault;
2: load error or clingo unavailable.

The runner reads clingo's text output, one atom per line (`--outf=0
--out-ifs='\n'`, `Solve.run_clingo`). Clingo 5.8.2 aborts (SIGABRT) while
it writes a model as JSON (`--outf=2`) when a shown atom, JSON-escaped, is
1 024 + 1 023k characters long, whatever the nesting of its terms:
`a("x…x")` with a string of 1 017, 2 040, 3 063, 5 109, 7 155 or 8 178
characters. A created instance is a term one functor deeper per creation,
so a chain of creations reaches such a length. The text output prints
every such atom, and terms nested 10 000 levels deep; the suite reads the
six atoms back and plays a chain of 451 object creations, whose deepest
atom is 3 070 characters long JSON-escaped, and a recursive chain of
100 + 1 + 100 creations over three strata, 201 levels deep.
