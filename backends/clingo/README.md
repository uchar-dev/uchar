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
  `role(Bron, "rolnaam", Doel)`; a parameter is `param("naam", V)`.
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
  ref keeps (maintainer decision 2026-09-23); a read no allowed order
  reaches fires nothing, but for [N4] below. That holds for a
  quantifier fold's matrix, for a Let barrier in a part that runs
  member by member, and for the value of a Let whose binder is read at
  such a place. For the Let barrier the pass before a creation is one
  of the allowed orders: ref runs a pass's rules before its creations,
  so the member a creation adds is absent in that pass, and so is a
  barrier only that member raises. `Let k = restpost in (som(bonus van
  werven die TEN MINSTE 1 schuiten [k > 0]) plus p)`, with one schuit
  created for each Werf and restpost leeg, refuses in the named
  sentence where ref faults at p in the pass before the creation. The
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
  refuses in the named sentence where ref faults at p in the pass
  before the creation. So the named refusal also stands where ref
  plays: two creations of 8
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
  - Loud, a refusal where ref plays. [L1] The day-kind table and the
    §7.1 horizon fence read their parameter at translation and refuse
    there, also behind a rule condition that fails, where ref never
    reads it. [L2] A Let barrier that only a created member raises, or a
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
    out of bel's value and the guard is exact. ([S2], retired
    2026-09-23: the branch hoist put the operand beside the branch
    behind the branch's condition; that operand's read now has a second
    guard without the arm's atoms.) [S3] The intermediate pass within
    one stratum (the ch. 6.1 residual of order-dependent transients, in
    fail-fast form). ref runs the forms of a stratum pass by pass until
    nothing changes, so a form that reads what a later form of the same
    stratum writes sees it leeg in the first pass. Where a short
    circuit depends on that value, ref reads the
    parameter in that pass and faults, while the fixpoint never reads
    it, and clingo answers the fixpoint: `heffing = 5,00 indien TEN
    MINSTE 1 [a > 0, p > 0]`, written before `a = inkomen`, with
    inkomen 20 and p unbound, answers 5,00 where ref faults; with the
    writing rule first both answer 5,00. Where the writer is a
    creation, that pass is the pass before the creation and the
    refusal is named (above). Whether ref's fault in such a pass is
    the arbiter's answer is a norm question for the maintainer. [S4] A
    condition that holds on absence (ch. 6.1: a count against zero,
    read positively) with a creation as writer, in an `all` prefix, in
    a taken arm or in a rule's condition: `ALLE [het aantal schuiten =
    0, p > 0]`, `Let n = het aantal schuiten in (als n > 0 dan 99
    anders p)`, or `keur = p indien het aantal schuiten = 0`, with one
    schuit created for each Werf. In the pass before the creation the
    member holds, the other arm is taken or the condition holds, and
    ref faults there; clingo answers the fixpoint. Only an `atLeast`
    prefix gets that pass (above); an `all` prefix, a branch condition
    and a rule's condition are read at the fixpoint.

  Each tagged entry, here and under Recorded limitations ([V1], [V2],
  [V3]),
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
  initialisations are `val`/`has_atom` heads on the term; the back-link
  relation (the explicit `verbindendeRol` binding, else the
  uniquely-derivable pair) lands both `role/3` ends; the
  1:1 linked-instance guard is `not inputrol/3` over the case links
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
  carries no budget. Until 2026-10-09 the depth ran on over the strata
  and the witness fell at the 100th mint: a chain of 100 mints, and
  chains of 50 + 1 + 50 in three strata, were UNSAT here where the
  reference interpreter plays them. The suite's `ch5.5 creation budget`
  frames pin both sides, each beside the reference interpreter, among
  them the two forms on which a count of generations and the depth part:
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
  (measured 2026-10-09; about 7 s while the depth ran on over the
  strata), where a single chain of 100 takes well under a second.
- A member initialisation takes the value at the moment of making
  (ch. 5.5, Generations): ref reads it on the settled world of the
  generation before, and a later generation may change the value read
  without the initialised member following it. This encoding has no
  moment of making; the initialisation's head reads the stable model,
  which holds the end value. The two differ where a later generation of
  the creation's own stratum changes the value read, and there only a
  creation on a dependency cycle (WF-045 admits one through a recursive
  group) can change it. So an initialisation is refused by name
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
  and W1 ervaren in the input, ref answers 1, and this backend answered
  3, without a refusal until 2026-10-09. A creation in a higher stratum
  than the cycle reads the end value, as ref does, and stays carried. An
  attribute or characteristic that only creations write is set once per
  object, at its making, so a read of it stays carried: the anchor
  `jutterswet-wachtersketen` reads its rang so.
  A member that a creation and a rule on the cycle both write is
  refused: with rang plus 1 and beginstand = zijn rang initialised, and a
  rule that sets the rang of a Wachter with an opvolger to 2, ref starts
  W1's opvolger at beginstand 1 while W1 ends at rang 2. The test reads
  the static dependency graph of `Ir_v0.Checks` (the graph ref
  stratifies) and ref's strata over it, not the values: a rule on the
  cycle whose value on an object is fixed at the object's making is
  refused as well, and so is a writer on the cycle whose validity the
  case's date excludes. Only a creation version the date excludes is not
  asked, because it is not translated, and it does not count as a
  creation that reaches a writer either: it mints and links nothing,
  here and in ref, so no later generation brings a change from it. With
  "geef door" valid from 2027-01-01 and a case on 2026-01-01 the Rapport
  is carried, and ref and this backend both answer rapportaantal(H1) = 1
  with W3 not ervaren; until the second fix round of 2026-10-09 this
  backend refused it. On the worked example of ch. 5.5 (beginstand =
  zijn stand, the stand counting the opvolgers) ref answers
  opvolgerbegin(W1) = 0, and this backend answered 1 before the
  refusal; the suite's `ch5.5 generations` frames pin the refusals, each
  beside ref's absolute answer, and the carried forms, and the `ch5.5
  generations off the cycle` frames do the same for a creation off the
  cycle: refused in the cycle's stratum, also with "geef door" valid on
  the case's date, carried a stratum higher, beside the cycle out of its
  reach, where it reads only what a creation writes, and where the date
  excludes "geef door".
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
  itself. On attribute defaults
  the consequence bindings sit inside `decides_n`, so a leeg
  consequence leaves it underivable — the gevuld gate for free — and
  the input value of a defaults target enters as `inputval/3`, standing
  defeasibly (`val :- inputval, not decided`) exactly where ref/'s
  write overwrites it; the initialisation default's justification is the
  absence of that input. The ch. 7.8 **standing targets** (a bare
  scalar computing rule as its target's only writer,
  `Ir_v0.Desugar.standing_targets`) take the same entry: the rule's
  effectiveness atom (`stateN_eff`) carries the full val-head body —
  condition AND value bindings — so a leeg evaluation derives neither
  and the input stands (`val :- inputval, not stateN_eff`), matching
  ref/'s no-write-on-leeg. The temporal arm mirrors it
  (`standing_temporal_targets`): a standing pro-rata target's input
  timeline enters as `inputtl/5`, standing WHOLE unless the fold
  derives a bucket (`prN_eff`, the full bucket body incl. the
  exactness guard) — the whole-timeline reading of RS ch. 7.8. The
  ch. 6d computed-target fence no longer applies to standing targets
  (scalar or temporal); it remains on multi-writer and versioned
  computing targets, and an allocation `doelAttribuut` supplied by the
  case is fenced the same way.
- Two or more computing rules on one target whose populations share a
  concrete type (on one type, or on a type and its subtype) would give
  the target two values in one model wherever two of them apply to one
  subject and differ, where ref faults on its pass budget (ch. 5.1,
  5.7). Each such writer therefore also derives its value under its own
  index (`wval/4`) and its firing (`wfires/3`); a writer that fires
  without a value writes leeg, unless it is standing (ch. 7.8: on a
  type chain each writer is the only one of its own key, and a standing
  writer writes nothing on leeg). Every pair of writers that share a
  concrete type gets one named witness, `viol("two writers of one target
  disagree: rules "A" and "B" write different values to the attribute a
  of a T — no fixpoint, divergence is a fault (ch. 5.7)", S)`, so the
  program is UNSAT and never a second value. Writers whose conditions
  exclude each other, or that write the same value, derive no witness
  (`corpus/golden/jutterswet-melding-naar-aard`,
  `corpus/rood/jutterswet-dubbele-melding`). On a standing target the
  case input stands only where no writer of the subject decides, a
  standing writer on another type of the chain included. Two computing
  rules on a temporal target are refused by name: a timeline is no
  single term to compare.
- An initialisation outside the desugaring domain — a conditional one,
  which ch. 7.7 excludes, or a windowed one — translates natively, as
  ref replays it (RS ch. 5.4): its `val` head needs the condition and
  `not inputfilled(S,a)`, a case fact beside the untouched input `val`,
  so a supplied value stands. Beside one computing rule on the target
  that writes a concrete type of its chain as a rule too, the head also
  needs `not comp_decides(S,a)`, which that rule derives with its own
  val body: the computed value wins. Where the rule fires and evaluates
  leeg (`comp_fires` without `comp_decides`), v0 erases what the
  initialisation writes and the initialisation writes it again; ref
  faults on its pass budget, and a `viol/2` witness makes clingo refuse
  too, unless the rule is a ch. 7.8 standing one, which writes nothing on
  leeg. Two initialisations that write one concrete type as rules
  (WF-079 refuses them first), two computing rules on one target, an
  allocation, a creation-time write or a defaults group of its own level
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
or fold is the node's explicit `RoleBinding`: since L2 the wire writes
every range and every recipient role, and the loader refuses a node that
omits one (`spec/proposals/V1-WIREMIGRATIE.md` B12). Until L2 the backend
shared the type-pair derivation for bindingless v0 dumps with `uchar-ref`
(`Eval.connecting_role`, the recorded ch. 4.1 wart, unique there by
WF-048); that derivation left with WF-048. The one relation still derived
when absent is an object creation's back-link, through `uchar-ref`'s
`Eval.connecting_pair` (ch. 5.5). Every route ends in the same
`role_canonical` convention the case facts use. All evaluation
semantics run in clingo; the hand-written absolute expectations keep a
shared-derivation bug from ever passing silently.

Recorded limitations:

- Clingo 5.8.2 aborts (SIGABRT) while it writes a model as JSON
  (`--outf=2`, the form the runner reads) when a shown atom that holds a
  string has one of certain lengths, whatever the nesting of its terms;
  the runner then reports `clingo killed`, loud but without a name.
  Measured 2026-10-09: `a("x…x")` aborts with a string of 1 017, 2 040,
  3 063, 5 109, 7 155 and 8 178 characters and with no other length from
  1 to 8 999; `has(X,"k")` with X nested 201 levels of `oc0` around
  `"W1"` aborts, with 195 to 200 and 202 to 229 levels it prints, and
  `val(X,"a",1)` aborts at 202 levels only. An atom without a string, and
  the text output (`--outf=0`), abort at none of those lengths. A minted
  instance is a term that grows by one functor per creation, so a long
  chain of creations reaches such a length: a document whose chain of 187
  object creations over 188 object types, without a recursive group
  (functors `oc0` to `oc186`), ends in a creation that sets a
  characteristic aborts clingo, and the same document with 120 to 186 or
  188 to 260 creations plays. This backend kept no bound there before the
  card `creatiebudget-ref-en-clingo` either, and behaved the same.
- In a document with an object creation in a recursive group every
  creation also carries `nest(S,Nest), Nest < oc_nest` and derives
  `nest(ocN(S),Nest+1)`, with `oc_nest` = 190, and an instance at that
  depth derives the named witness `viol("term nesting bound reached …",
  _)` → UNSAT: a chain of 190 nested creations over all strata is refused
  by name here, where the reference interpreter, which keeps no such
  bound, plays it. The bound was set (2026-10-09) on the reading that
  clingo aborts once a term nests past about 200 levels; the measurement
  above shows that the abort follows the length of the printed atom. With
  the functor `oc0` and a short label and short names the bound keeps
  every shown atom below the first length that aborts; with longer
  functors, labels or names it does not. Until 2026-10-09 the creation
  depth ran on over the strata and kept every term under 101 levels; per
  stratum it no longer does (a chain of 100 + 1 + 100 nests 201 levels).
  The suite pins the refusal on the 50 + 1 + 50 frame with the bound
  lowered to 60 in the program text.
- Clingo integers are 32-bit and wrap silently; the scaled encoding is
  exact only while every value and intermediate product stays within
  ±2³¹ (the allocation forms A×W before dividing). A mismatch fails
  loud against the absolute expectations, but a bignum/rational
  encoding is follow-up work for realistic monetary magnitudes.
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
