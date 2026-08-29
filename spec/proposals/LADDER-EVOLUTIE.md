# LADDER-EVOLUTIE — the ladder design round

*Status: proposal, 2026-08-29, awaiting maintainer ratification of §12. Companion
to `PHASE4-DEFAULTS.md` (the adopted default-calculus design),
`PRIORITY-RELATION.md` (the structural-priority decision), `PHASE5D-DIFF.md`
(the frontend diff that seeded the frontend question), and
`spec/RUNTIME-SEMANTICS.md` ch. 6–7. Produced from a read-only exploration
wave (eight tracks, 2026-08-29); nothing in this round builds before §12 is
answered. All file:line citations are at uchar `main` db8675d and toolchain
`main` 99b3156.*

---

## 1. Why this round

Five recorded debts converge on the ladder, and one reality test now exists
to hold the design against.

1. **The Phase-4 deferral premise is void.** Full Drools ladder service was
   deferred "until a frontend emits ladders — realistically the Phase-5
   Catala plugin" (`PHASE4-DEFAULTS.md:449-453`, decision 6). The Catala
   plugin emits ladders today (six ladder-carrying catala goldens), yet the
   Drools loader refuses the entire `defaultLadders` section
   (`IrLoader.java:74-95`): every ladder golden is a gate-12 auto-exclusion.
   The condition has been met; the work was never scheduled.
2. **The plan anticipates the inversion.** `spec/PLAN.md` §5: "The planned
   upgrade, once the ladder-producing frontend evolution starts, is
   inverting the dependency so the toolchain consumes this frontend — one
   source of frontend truth."
3. **WF-031 left an OPEN addressed here by name.** "OPEN: whether mutually
   recursive day-kind definitions are admissible … decided with the
   stratification chapter" (`IR-SPEC.md:1136-1138`). There is no
   stratification chapter yet — IR-SPEC's numbering even jumps §4 → §6.
4. **The waist questions were registered for this round.** Universal unit
   conversion (the factor-1 card's registered binary) and CONSTRUCTS row 27
   (Percentage dissolves into Number + `%`-unit + dialect tag, with two
   recorded OPENs).
5. **The gate-12 triage left candidates to formalise**, plus the public-flip
   checklist item on the Onbekend sentinel's executing witness.

The reality test is **herleving-WW** (WW art. 15–21): a genuine, layered
default-and-exception structure in living statute text, rights-free under
art. 11 Auteurswet. §3 distils it into requirements; every proposal in
§§4–8 is checked against them.

## 2. Where the ladder stands

The wire contract is mature and two-executor-proven. Summary of the state
capture (full citations in the track-A findings; normative sources
`IR-SPEC.md:1904-2005`, `RUNTIME-SEMANTICS.md` ch. 7):

- `defaultLadders` is an optional top-level core-layer list the v0 emitter
  never produces. A ladder = one writer, one target (characteristic XOR
  attribute), rungs in descending priority, highest deciding rung wins;
  attribute rungs fall through on leeg; a ladder never writes leeg; rung
  evaluation is speculative (only the winner's §9.5 deposits persist).
  Nested exception groups (`Rung.excepties`, WF-075) form a rose tree;
  declared-unordered groups reconcile with equal-decisions-idempotent and
  differing-decisions → no write + one path-keyed §9.5 fact. Conflict-fact
  readability is closed for v1.
- **ref** executes the full contract including nesting and the ch. 7.7/7.8
  standing/desugaring readings (`ref/lib/eval.ml:1630-1771`); it faults on a
  temporal ladder target (`eval.ml:1640-1644`).
- **clingo** executes the same contract monotonically (S17 `decides_m`/NAF
  displacement, `translate.ml:4298-4594`), with the ch. 6d computed-target
  input fence for non-standing targets.
- **Drools** refuses ladders wholesale at the loader; its salience is still
  the kenmerk-negation heuristic, not derived from IR defaults.
- Witness inventory: seven hand-written ladder anchors (uitsluiting,
  vondstwaardering, toelagegronden, nestuitzondering, nestkenmerk,
  nestdiepte, hardheid), two non-anchor goldens (samenloop,
  synth3-nestkorting), six Catala-produced ladder goldens, seventeen
  WF-050/051/075 negatiefs.
- Recorded open holes: the Drools service (item 1 above); temporal ladder
  targets (executor fault with **no wire/WF fence and no witness**); the
  allocation-target input fence (`PHASE4-DEFAULTS.md` §4.4, latent superset
  gap, waits for the §9 executor card); sub-root shared base
  (Catala-refused, "workable by base-duplication").

## 3. The reality test: herleving-WW

### 3.1 The statute structure

All quotes from the verified BWB dossier (toestand 2026-07-01, confirmed
current against the SRU service); statute text is rights-free.

- **Art. 15, the gate sentence:** "Met inachtneming van de artikelen 16 tot
  en met 21 en de daarop berustende bepalingen heeft de werknemer die
  werkloos is recht op uitkering." — a base grant subordinated to six
  downstream articles.
- **Art. 16** defines werkloos (conjunctive: urenverlies én beschikbaarheid);
  **art. 17** is the wekeneis (26-uit-36), an origination gate relevant to
  herleving only negatively (a *new* right blocks revival, art. 21 lid 2).
- **Art. 19 lid 1**, aanhef + onderdelen a–n: "Geen recht op uitkering heeft
  de werknemer die: …" — one consequence, disjunctively triggered, where
  the onderdelen carry **different downstream termijn regimes** (geen /
  zes maanden / variabel).
- **Art. 20 lid 1** ends the right on four grounds, with two distinct
  timestamp regimes (per-day for the 19-ground, per-kalendermaand for the
  income ground).
- **Art. 21 lid 1** revives it — itself again "met inachtneming van" lid
  2/3/4/5, a nested gate sentence; **lid 2** blocks revival (nieuw recht >
  87,5%; WIA); **lid 3** gates it on the zesmaandstermijn with the decisive
  samenloop clause: "ook indien deze omstandigheden zich aaneensluitend
  voordoen, slechts … zes maanden" — consecutive grounds share **one**
  window from the first eindiging; **lid 4** and art. 8 lid 3 carry the
  variable termijnen; art. 43 lid 1 shifts the maxdatum by the elapsed
  eindiging→herleving period.

### 3.2 Ladder requirements distilled

- **R1 — gate-sentence scoping.** "Met inachtneming van …" compiles to a
  base rung displaced by higher rungs drawn from the referenced articles.
  *Expressible today* — the `jutterswet-uitsluiting` anchor is exactly this
  shape. Art. 21's embedded second gate sentence requires nesting (R3).
- **R2 — aanhef-with-onderdelen as identifiable grounds.** Because the
  onderdelen carry different termijn regimes, a faithful encoding needs the
  onderdelen as **separately identifiable rungs, each tied to its termijn
  regime** — not one flattened disjunction. This is the sharpest new
  requirement WW places on the ladder; no jutterswet anchor exercises it.
- **R3 — exception-to-the-termination.** grant (15) → do-not-attribute
  (19/20) → attribute again (21-1) → do-not-attribute (21-2) → gated by
  (21-3): four-plus nesting levels. *Expressible* via `Rung.excepties`
  (ch. 7.9); the deepest real nesting the ladder has faced (anchors nest
  two levels).
- **R4 — samenloop is ordering, not conflict.** The "aaneensluitend …
  slechts één keer" clause wants "the earliest ground's window governs" —
  an **ordered** chain keyed on eindiging-date. WW is a positive witness
  that the declared-unordered group + §9.5 channel is *not* the samenloop
  mechanism, confirming the closed v1 decision (`PRIORITY-RELATION.md`
  §10; `RUNTIME-SEMANTICS.md:2429-2435`).
- **R5 — termijnen as per-ground guards.** (a) *which* termijn applies per
  ground — ladder/lookup structure; (b) *the test* — "periode(eindiging,
  vervallen) ≤ termijn" — date arithmetic (`tijdsduur` WF-038 + ordering
  WF-036, both landed), fed into rung justifications as separate derived
  attributes (WF-051 forbids the ladder reading its own write).

### 3.3 What ladders alone cannot carry

The ladder is atemporal: one decision at one fixpoint. Three things compose
*around* it, not inside it: (1) the lifecycle over event-time
(lopend → geëindigd → herleefd) — the IR has no event/state-machine
construct, only anchor-only timelines; (2) the termijn deadline over two
event-dates — input facts or prior date-arithmetic rules; (3) the art. 43
maxdatum shift — plain date+duration arithmetic, not a ladder concern.
Herleving is a good realism test precisely because it forces this
composition end-to-end. The dossier also records a genuine
statute-arithmetic ambiguity (days-vs-months in the art. 43 period) that
the case must pin as an authored choice.

### 3.4 The realism-case sketch

First case: the income-termination path (art. 20-1-c → 21-1 first volzin,
geen termijn) — a nested characteristic ladder for `recht op uitkering`
(grant 15 → einde 20-1-c → herleving 21-1 → blokkade 21-2-a) plus ordinary
date-arithmetic rules (inkomensgrens 0,875 × maandloon; eindigingsdatum;
`tijdsduur`; nieuwe maxdatum). Graduation set beyond it: the
detentie/zesmaandstermijn boundary, the nieuw-recht blokkade, the
AOW-jaargrens underlayer, consecutive-grounds samenloop, and the variable
termijnen — each mapped to scenarios in the regelspraak-werkbank evaluation
dataset (read-only reference). Authored from statute text and own casus
facts only; handbook material never enters corpus or spec. Authoring
channel: hand-written ladder anchor first; through the §4 dialect extension
once that lands.

## 4. The ladder-producing frontend (agenda 1)

### 4.1 Findings that bound the design

- **Official RegelSpraak v2.3.0 has no exception, priority, default-override
  or rule-ordering construct.** "tenzij" occurs only inside quoted statute
  prose; §9.10 is recursion, and the regelgroep exists for recursion only.
- **Kenmerk semantics forbid negative attribution** (§9.2: characteristics
  default onwaar, assignment is monotone waar, "Een Vlucht is niet duurzaam"
  is not admitted) — an exception can never *withdraw* a characteristic;
  it can only live in the base rule's condition.
- The language therefore expresses rule-plus-exception as **condition
  duplication** (the exception ground folded, negated, into the base rule's
  `indien`), and the flat source carries **no priority whatsoever**. The
  order a ladder encodes does not exist in RegelSpraak source; Catala's
  plugin reads its ladder structure off a native default tree the source
  language actually has.
- Corpus confirmation: of 20 ladder-carrying goldens, 11 are
  Catala-sourced, 9 hand-authored, **0 from a `bron.rgs`**; all 20
  `bron.rgs` goldens are flat.

### 4.2 Options

**(i) A uchar-dialect extension** — `Uitzondering op <regelnaam>` blocks
(ordered, stacking above the named base rule) plus an
`Uitzonderingen op …, ongeordend` group form (the only §9.5 surface).
Sketch, in RegelSpraak register:

```
Regel penninggerechtigd bij drempel
  geldig altijd
    Een Jutter is penninggerechtigd
    indien zijn aantal aangemelde vondsten groter of gelijk is aan 12.

Uitzondering op penninggerechtigd bij drempel: bij hardheid
  geldig altijd
    Een Jutter is penninggerechtigd
    indien hij hardheid toegepast heeft en zijn aantal aangemelde
    vondsten groter of gelijk is aan 6.
```

Purely additive grammar (new top-level alternatives beside
`regel`/`regelGroep`/`beslistabel`; the inert-on-official-documents posture
the grammar already takes for `regelGroep` and scenariotoetsen); a tracked
divergence from the v2.3.0 arbiter, logged in CONSTRUCTS and
`docs/grammar-divergences.md`. Resolver work is real but bounded (resolve
the base rule, check same target, build the `DefaultLadder`, run the
WF-050 writer checks, emit). No existing golden changes. Under R2 the
per-onderdeel identity comes for free: each onderdeel is its own named
uitzondering.

**(ii) Pattern recognition (flat → ladder folding in the resolver)** —
**rejected as a transformation.** Only the complementary-comparison split
is soundly recognisable, and that case is an order-free partition needing
no ladder. General mutual exclusivity needs a satisfiability check;
negation-guard matching needs authorial intent; and above all the
**priority itself is not recoverable** from a source that never encoded it
— folding overlap into a ladder invents an order the author never wrote
(exactly the "needs authorial intent, not a transformation" line ch. 7.7
already draws). At most, recognition may *advise* ("these rules look like a
defeasibility family — consider an `Uitzondering op` block"), never
rewrite. A silent fold would also rewrite all 20 flat `.rgs` goldens
(breaking gate-10 byte-identity) for no semantic gain.

**(iii) Status quo** — ladders stay Catala/hand-authored. Zero cost, zero
arbiter divergence, faithful to the source language's own expressive limit.
Cost: no Dutch-CNL authoring channel for ordered exceptions; the WW realism
case stays hand-anchor-only.

### 4.3 Recommendation

**(i), scheduled after the inversion (§6), with recognition demoted to an
advisory linter at most.** Three reasons. The WW test wants it: R2's
per-onderdeel exception identity is precisely what named `Uitzondering op`
blocks provide, and a herleving case authored in the Dutch CNL — flowing
frontend → wire → ref ≡ clingo ≡ Drools — is the strongest hourglass
witness the programme can produce. The plan anticipated it (§1 item 2).
And the main cost recorded against it — ladder-emitting goldens dropping
out of Drools coverage — is removed by scheduling the Drools ladder service
(§5) *first*. The extension is opt-in at the source level, so emission
changes nothing for documents that do not use it.

### 4.4 Disposition of the flat form

**Both forms persist.** The flat KENMERKTOEKENNING/gelijkstelling wire form
remains first-class (the v0-mirror invariant: the v0 emitter never produces
`defaultLadders`); the ladder is the canonical *semantic reading*,
delivered by the ch. 7.7/7.8 desugaring in consumers — "recorded, not
transformed" (`RUNTIME-SEMANTICS.md:2573-2574`). WF-050 and WF-079 are
written for exactly this coexistence. Canonicalising flat rules to one-rung
ladders would break gate-10 byte-identity, drop 20 goldens out of Drools
play, and obsolete the WF-079 regime, for no semantic gain.

## 5. Full Drools ladder service (agenda 2)

### 5.1 The lowering choice

Four mechanisms were weighed for "no higher rung fired" in forward
chaining:

- **(A) Per-rung control facts + NAF displacement** (the clingo mirror in
  DRL): declarative and structurally identical to the verified S17
  encoding, but the speculative-deposit rule and nested `excepties` demand
  elaborate control-fact bookkeeping.
- **(B) One reduce-rule per (group, subject)** — LHS the subject pattern
  with class-reactivity and navigated-read joins; RHS a direct Java port of
  ref's `run_ladder_group` walk (`eval.ml:1685-1771`): top-down rung walk,
  fall-through, recursion into `excepties`, member reconciliation, one
  idempotent write or one `Inconsistentie`. Speculative deposits become a
  trivial buffer-and-flush; nesting is plain recursion.
- **(C) Per-rung salience** — the generalised "salience −1 trick", the
  exact anti-pattern ch. 7.5 exists to replace, and it cannot express
  fall-through-on-leeg. **Rejected on spec grounds.**
- **(D) Activation groups** — cancellation is agenda-order and a
  fired-then-leeg rung has already cancelled the group; fall-through
  inexpressible. **Rejected.**

**Recommendation: (B), scalar-first, with loud fences.** A corpus scan
shows every flippable ladder golden's rungs are subject-local scalar/
kenmerk logic (no division in any consequence, no aggregation, one
navigated read — itself blocked on an orthogonal refusal), so
`leafPredicate`/`scalarWaarde` already cover the flippable set. Fences as
reason-carrying `Dekking.skip`s: temporal ladder targets (ref parity —
both fence), channel-depositing consequences (no witness), non-reducible
justifications. Salience: a minimal ladder-local "strictly below every
producer of every rung justification" computation (the ch. 7.4 negative
edges), rather than rebuilding the full `dep_forms` graph in Java — the §7
chapter then records the bounded-narrower-stratifier posture this
instantiates.

### 5.2 Flip inventory and order

Fifteen goldens / 67 cases flip on ladder work alone, risk-ascending:
**Tier 0** loader-accepts an *empty* `defaultLadders` list (three catala
goldens — no lowering at all); **Tier 1** ordered attribute ladder +
fall-through + initialisation (samenloop, vangstheffing-catala,
vondstwaardering); **Tier 2** characteristic ladders — strata-critical, the
monotone `set_char` cannot self-correct (uitsluiting, hardheid,
penningrecht-catala); **Tier 3** declared-unordered + §9.5
(toelagegronden); **Tier 4** nested `excepties` (tariefrecht-catala,
synth3-nestkorting, nestuitzondering, nestdiepte, nestkenmerk). Five more
catala goldens stay excluded on orthogonal refusals (objectcreatie,
functionDefs). Because ref ≡ clingo already pin every flip candidate, a
correct lowering MUST play-and-pass; a misvalue is a bug, never an
`EXCLUDE_KNOWN` entry.

### 5.3 Charter and witnessing consequences

- The production toolchain has **no** ladder concept; this lowering grows
  `backends/drools` beyond ported production semantics into a
  uchar-spec-ch.-7 implementation. That is exactly what the third executor
  of the square is for, but the gate-12 charter wording ("the ported
  exec-model back-end") should be amended at landing — a maintainer-serial
  CLAUDE.md edit.
- The new `DefaultLadder`/`Rung`/`SubLadder` Java records belong in the
  **shared frontend `ir/` package** (the one-copy rule; and if §4's
  extension lands, the frontend emitter needs them anyway). This orders the
  work *after* the inversion (§6), so the types land once, in the single
  source.
- `CaseRunner.consistentie` today counts any `Inconsistentie` without
  checking the key: gate 12 would witness conflict *presence* but not the
  ch. 7.2/7.9 path key ref and clingo pin. **Recommendation: extend
  `CaseRunner` to verify key + subject** — a small change that keeps the
  third leg's witness as sharp as the other two.

## 6. The dependency inversion (agenda 3)

### 6.1 Findings that bound the design

- Today's topology is dual-copy, dual-drift-gated: byte-identical emitters
  in both repos, gate 10 here, `uchar-conformance` there. The copies are
  *not* source-identical (the `--ir`-only Resolver trim; the kern-package
  move) — any sync is a transform, not a `cp`.
- **The toolchain's Drools backend consumes the frontend's in-memory Java
  types, not the wire** (no `ir.json` loader exists there). "The toolchain
  consumes uchar's frontend" therefore means compiling uchar's
  `ir/model/kern` sources — and uchar already runs that exact wiring
  intra-repo: `backends/drools/pom.xml` compiles the frontend's IR records
  as an added source root. The template exists and is proven.
- The toolchain's SPEC_REV drift set (`NORMATIVE=`) does **not** include
  `frontends/regelspraak` today; after inversion it must, or a frontend
  change slips past the spec-first pin discipline.
- A naive submodule of all of uchar into the toolchain is a **rights
  violation** (it would drag the `toka/` zone into the toolchain
  checkout); a `git subtree split` of the TOKA-clean frontend slice is
  mandatory for any source-consumption route.
- Ladder *emission* is decoupled from Drools ladder *execution*: the
  emitter is resolver-only. With §4's extension being source-opt-in,
  emission cannot surprise the toolchain's regression bed; the toolchain
  fire path must still adopt the loud-Refusal posture for ladder-carrying
  wire it cannot lower.

### 6.2 Options and recommendation

**(A) Subtree-split source consumption** — split `frontends/regelspraak`
into a standalone TOKA-clean repo, synced by push from uchar; the toolchain
consumes it (submodule pin or vendored subtree) as a compiled source root à
la `backends/drools/pom.xml`, and deletes its own
`{grammar,resolver,ir,model,emit}`. **(B) Jar artifact at the pin** — fights
the deliberately jar-only frontend build and turns "one source" into a
compiled blob. **(C) Reversed scripted source-sync** — today's pattern with
the authoritative label flipped: two physical copies persist, and source
divergence in non-emitting code is invisible to an emitted-IR byte gate.

**Recommendation: (A), sequenced invert-first.** Inverting while the
frontend is quiescent (flat, byte-identical, both gates green) is the
low-risk moment; evolving ladders across two live copies is the high-risk
path. Consequences to legislate up front: the toolchain re-provides
`--drl/--facts/--graaf` in a toolchain-side wrapper around the consumed
`--ir`-only resolver; the SPEC_REV normative set grows to
`frontends/regelspraak/**` (one pin, no second pin); `uchar-conformance`
repurposes into a "pinned frontend builds and emits my fixtures" smoke
check (uchar gate 10 becomes the sole emitter-golden authority); the
toolchain's execution path adopts the loud ladder Refusal until its own
lowering exists. Split-repo visibility: **private for now** — both repos
are private and CI tokens suffice; publishing the (provably TOKA-clean)
slice is a maintainer visibility decision best taken together with the
§9-decision-10 flip, not as a side effect here.

## 7. The stratification chapter (agenda 4)

### 7.1 The gap

IR-SPEC has no stratification chapter — the numbering jumps §4 → §6, and
WF-045/046 delegate their entire meaning to `RUNTIME-SEMANTICS.md` ch. 6.
The dependency graph has exactly one implementation
(`spec/verifier/lib/checks.ml` `dep_forms`), shared by the verifier and ref
and reused by clingo as the allocation-input stratification fence — while
Drools carries a separate, kenmerk-only salience heuristic whose javadoc
itself defers to "the spec's full dependency-graph treatment". Three
executors do not share one stratification notion; the spec never says
whether that is legitimate.

### 7.2 What the chapter must define

A new normative **IR-SPEC §5 "Stratification"** (filling the numbering
hole; RUNTIME-SEMANTICS ch. 6 stays as the operational companion), giving:
the node kinds (attribute, characteristic, channel, population, relation —
and the day-kind answer below); the three edge kinds (positive,
stratum-strict, negative — plus the WF-051 ladder fold: every rung read a
negative edge toward the ladder write); the illegal-cycle rule (WF-045
acyclicity modulo recursive creation; WF-046 no negative edge on any
cycle); and an **executor-neutrality clause**: stratification is an IR
property checked by the verifier, and a backend either realises a sound
refinement of the graph strata or carries a *narrower* stratifier plus a
loud auto-refusal for uncovered forms — ratifying, as a bounded and named
exception, the posture Drools already has (and which §5.1's ladder-local
salience extends).

### 7.3 The dagsoort answer

The OPEN conflates two sub-questions. **(Q-read)** may a dagsoort
`voorwaarde` read derived state? The toolchain structurally cannot compile
one (non-date operands make the whole definition a marker/skip), no uchar
executor evaluates the predicate at all, and the corpus carries exactly one
never-evaluated dagsoort — every claim here is test-free until witnesses
land. **(Q-cycle)** may dagsoort definitions reference each other? The
toolchain permits references and resolves a cycle to a *silent false*
(the §9.10 prohibition unenforced).

Options: **(A)** forbid derived-state reads (WF-031 purity clause:
dates, date-components, eerste-paasdag, literals, parameters, dagsoort
references only) and require the dagsoort-reference graph acyclic (a new
WF check; loud refusal where the toolchain is silently false) — day-kinds
stay input-only, outside `dep_forms`, framed as stratum 0; **(C)** make
day-kind a graph node with real edges — speculative machinery for a
construct nothing runs, coherent only if Q-read is answered yes.

**Recommendation: (A) with the stratum-0 framing**, landed with its first
witnesses (a derived-read negatief, a reference-cycle negatief, an acyclic
reference golden), and tracked as a diverge-from-v0 delta (v0 neither
checks nor documents).

## 8. The waist: units and percentage (agenda 5)

### 8.1 One mechanism, not two

The two taille questions turn out to be one: `%` is a unit in the dimension
system, and the row-27 collapse is a special case of unit
canonicalisation. Current landscape: ref carries the full exact-rational
algebra (`arith.ml`) and already treats Percentage as Numeriek everywhere;
Drools/the toolchain restate at runtime; the RegelSpraak frontend
type-checks with exact factors but emits units symbolically; **clingo
carries factor-1 only** — the standing embargo the factor-1 card drove
loud; Catala emits canonical decimals and never produces
Percentage/PERCENTAGE_VAN.

**The decisive technical constraint: canonical = the finest base unit.**
Restating to a finer unit multiplies by an integer (exact,
integer-encodable); restating coarser divides, possibly non-terminating —
which clingo's integer encoding cannot hold. With finest-unit canonicals,
every canonicalisation is multiply-only and the entire factor≠1 fault
class becomes translatable.

### 8.2 Options

- **(A) Canonical values on the wire** (frontends convert at emission,
  source units demoted to presentation metadata) — the cleanest core and
  the literal MLIR precedent, but it breaks the v0 value-mirror and forces
  a heavy corpus/anchor migration.
- **(B) Source units stay on the wire; a specified normalisation lowering
  pass** canonicalises units as clingo's pre-step (and as a differential
  check that ref/Drools-over-source ≡ clingo-over-canonical). No corpus
  migration; the conversion algebra lives in one tested artifact; the
  mirror stands.
- **(C) Teach clingo the rational algebra in place** — re-implements ref
  inside the integer encoding; the very thing the factor-1 card avoided.
  **Rejected.**

**Recommendation: (B) now, with canonical fixed to the finest base unit;
(A) recorded as the eventual v1 wire form** once the corpus migration is
budgeted. The MLIR mapping is exact: source-unit wire = upper dialect,
normalisation pass = lowering, canonical wire = core dialect.

### 8.3 The row-27 resolution

- **Collapse:** the `Percentage` node is removed in v1; a percentage is
  `Numeriek` with a built-in `%` unit plus a `dialect(percentage)`
  presentation tag. The executors already treat Percentage as Numeriek, so
  the semantic delta is ~nil; the work is wire/schema/verifier plus the
  `%`-unit decision. This resolves the recorded row-27 OPEN ("whether the
  expression layer needs the tag") as: **the tag is presentation-only; the
  expression layer does not need it** — the `%` unit carries the meaning.
- **The `%` reading (strict):** `%`'s convertibility class is the singleton
  {`%`} — it never adopts a sibling unit for addition, comparison or
  write (no silent `20% + 5 km`). It canonicalises to a dimensionless
  magnitude with factor 1/100 (exact, integer-encodable) **only** through
  the one privileged bridge out of `%`-space:
- **PERCENTAGE_VAN becomes upper-dialect sugar** with the specified
  lowering `PERCENTAGE_VAN(P, V) ⇒ MAAL(strip%(P) ÷ 100, V)` — exactly
  ref's `pct_of` and clingo's scale-fold, with no dedicated core operator
  in v1.
- **The anchor gap closes.** Today the only two goldens with a
  Percentage-typed PERCENTAGE_VAN operand hand-set `eenheid:null` — a
  load-bearing hack, because clingo refuses any unit-carrying operand while
  the real frontend emits `%`-united literals (witnessed by
  `jutterswet-vondsttarief`). Near-term, v0-compatible card: teach clingo's
  `Percentage_van` to admit a **pure-`%`** left operand by folding the
  ÷100 (its scale machinery already does this arithmetic), then respell
  `anchor/jutterswet-vindersloon` and `synth2-afrondingscombinaties` to the
  frontend-faithful `eenheid:%` spelling. The anchors then pin a wire shape
  a frontend actually produces, and the canonical core form of vindersloon
  becomes byte-shape-identical to what the Catala frontend already emits.
- **Housekeeping, same epoch:** `factorToCanonical` migrates from a JSON
  float to an exact rational `{num,den}` on the wire (the row-26 delta),
  retiring ref's shortest-decimal-recovery-or-fault workaround.

## 9. Gate-12 triage as v1 deltas, and the flip checklist (agenda 6–7)

The load-bearing triage outcome: **three of the four named candidates are
not open deltas** — they are already-decided v1 semantics whose gate-12
divergences are by-design witnesses (Drools faithfully mirrors v0 where
ref/clingo implement the decided norm):

1. **Empty-source assignment as no-op** = the ch. 7.8 standing norm
   (decided 2026-08-20), worked verbatim on the waardestelling anchor.
   Nothing to add. The `waardering-v0vorm` entry is a distinct phenomenon —
   a v0-*exec-model* non-termination (the DRL oracle settles at ref's
   value) sitting in the value-divergence list against the gate's own
   "a fault at fire is an auto-exclusion" rule: **rewrite its reason line
   now** to state exec-model-non-termination-only; reclassify only if the
   CaseRunner taxonomy is ever revisited.
2. **Empty-operand comparison** was resolved in the **opposite** direction
   (Tabel 44, 2026-08-28, source-verified) and the square already closed on
   it; empty-as-unknown stays a frozen non-goal. Candidate closed against.
3. **Stratification of negated kenmerk reads** = ch. 5.3 + WF-046, already
   specified and ref/clingo-correct; the kustwacht divergence isolates a
   *navigated cross-object* case the toolchain's same-subject strata fix
   did not cover — a **toolchain conformance gap** (spec-first already
   satisfied), to be sharpened in CONSTRUCTS row 6 (maintainer-serial).
4. **keurmerk** is a `backends/drools` string-encoding limitation, not a
   spec matter. **Recommendation: fix the exec-model text-equality
   encoding** (unprotected path; the quote-free variant already plays, so
   the fix moves a divergence to played and closes the square).

**The Onbekend flip item, corrected.** The blanket claim "the sentinel's
only machine witness is in the parity zone" is false: ten public goldens
execute Onbekend in the count position, and four negatiefs pin the
rejections. The **refined** claim is true: the `numeric_ok`
arithmetic-recursion admission (Onbekend proven numeric on a Binair
operand/result, `translate.ml:619-624`) has no public executing witness —
its only one lives in the gate-11 parity zone and leaves with it.
**Recommendation:** author one public Jutterswet golden exercising that arm
**before** the public flip (the flip-checklist gating item), and retire
both the golden's necessity and the arm itself at the v1 WF-003 fail-fast
promotion, which removes the sentinel entirely.

## 10. Sequencing

After ratification, in dependency order (cards cut per WAY-OF-WORKING;
protected-path deltas ride as proposals):

1. **Inversion** (§6): subtree split, toolchain consumption, pin-set
   growth, conformance repurpose. The frontend is quiescent; lowest risk.
2. **Drools ladder service** (§5): tiers 0–4 against the existing 15-golden
   flip set; ladder records land in the now-single frontend `ir/` package;
   CaseRunner conflict-key extension. Independent of 1 in content but
   ordered after it so the types land once.
3. **Frontend `Uitzondering op` extension** (§4): grammar + resolver +
   emitter + first `.rgs`-sourced ladder goldens — which then *play* in
   gate 12 thanks to 2.
4. **WW realism case** (§3.4): the hand-anchor path needs nothing from 1–3
   and may start any time; the RegelSpraak-authored variant follows 3.
5. **Stratification chapter + dagsoort WF** (§7) and **row-27/units
   cards** (§8): spec-and-executor work, largely orthogonal to 1–4; the
   near-term `%`-operand card (§8.3) is small and independent.
6. **Triage closure** (§9): reason-line rewrite, keurmerk fix, Onbekend
   witness golden.

## 11. Maintainer-serial documentary notes (no agent touches these)

- `spec/CONSTRUCTS.md:63` prose "no corpus golden exercises a Percentage
  literal" is stale (vondsttarief, synth2, vindersloon all carry one).
- CONSTRUCTS row 6: record the navigated cross-object stratification case
  as a v0-Drools conformance gap distinct from the closed same-subject
  chain.
- `spec/PLAN.md` realism-case sentence: the candidate becomes the
  WW-herleving case (§3.4) in place of zorgtoeslag, if Q13 ratifies.
- CLAUDE.md gate-12 wording ("the ported exec-model back-end") amended when
  Q3 lands ladder machinery beyond the ported semantics.

## 12. Ratification questions

**The frontend (§4)**

1. **Ladder production from RegelSpraak source.** (a) Adopt the
   uchar-dialect extension — `Uitzondering op <regel>` ordered blocks plus
   an `ongeordend` group form — as a tracked, additive divergence from the
   v2.3.0 arbiter, scheduled after the inversion; recognition of flat
   families may only ever *advise*, never transform. (b) Status quo:
   frontend stays flat; ladders remain Catala/hand-authored; the extension
   waits for demonstrated demand. (c) Resolver-side flat→ladder folding —
   rejected for the reasons of §4.2. **Recommendation: (a).**
2. **Flat-form disposition (confirm).** Flat rules remain a first-class
   wire form; the ladder is the canonical semantic reading via ch. 7.7/7.8;
   the v0-mirror invariant stands; no canonicalisation of flat goldens.
   **Recommendation: confirm.**

**The Drools service (§5)**

3. **Schedule and shape.** Schedule full ladder service now (the deferral
   premise is void): lowering Option B (reduce-rule port of ref's
   `run_ladder_group`), scalar-first with loud `Dekking.skip` fences
   (temporal targets, channel-depositing consequences, non-reducible
   justifications), tiers 0→4 over the 15-golden flip set, ladder-local
   salience ("strictly below all justification producers"), the new Java
   records in the shared frontend `ir/` package, and the backend charter
   amended to say it implements spec ch. 7 beyond the ported production
   semantics. **Recommendation: yes, as stated.**
4. **Conflict-key witnessing.** Extend `CaseRunner.consistentie` to verify
   the §9.5 conflict key and subject (so the Drools leg witnesses the
   ch. 7.2/7.9 path key exactly as ref/clingo do), rather than accepting
   coarse presence-parity. **Recommendation: extend.**

**The inversion (§6)**

5. **Mechanism and sequencing.** (a) Subtree-split of
   `frontends/regelspraak` into a standalone TOKA-clean repo, consumed by
   the toolchain as a compiled source root; performed *before* the frontend
   evolution (freeze-invert-evolve); SPEC_REV's normative set grows to
   `frontends/regelspraak/**` (one pin); `uchar-conformance` repurposed to
   a smoke check; the toolchain re-provides `--drl/--facts/--graaf` in a
   wrapper; its fire path adopts the loud ladder Refusal. (b) Jar
   artifact. (c) Reversed source-sync. **Recommendation: (a).**
6. **Split-repo visibility.** Keep the split frontend repo private for now
   (CI tokens suffice), revisiting publication together with the
   §9-decision-10 flip — or publish it immediately (it is provably
   TOKA-clean), decoupling the toolchain from uchar's privacy timeline.
   **Recommendation: private for now.**

**Stratification (§7)**

7. **Chapter placement.** Lift the dependency-graph and edge-kind
   definitions into a new normative IR-SPEC §5 (WF-045/046/051 cite it;
   RUNTIME-SEMANTICS ch. 6 remains the operational companion), including
   the executor-neutrality clause that ratifies a narrower backend
   stratifier *plus loud refusal* as a bounded, named posture.
   **Recommendation: yes.**
8. **The dagsoort answer.** WF-031 gains the purity clause (a dagsoort
   voorwaarde reads only dates, date-components, eerste-paasdag, literals,
   parameters and dagsoort references — never attributes/characteristics)
   and a new acyclicity check on the dagsoort-reference graph (loud, where
   the toolchain is silently false-in-cycle); day-kinds stay outside the
   dependency graph, framed as stratum 0; first witnesses land with the
   rule. **Recommendation: yes (Option A with the stratum-0 framing).**

**The waist (§8)**

9. **Units.** Adopt Option B — source units on the wire plus a specified
   normalisation lowering pass (clingo's pre-step, differentially checked),
   with **canonical = finest base unit** — and record Option A
   (canonical-unit wire) as the intended eventual v1 form.
   **Recommendation: yes.**
10. **Row 27 package.** Ratify: the Percentage node dissolves in v1 into
    `Numeriek` + built-in `%` unit + presentation-only dialect tag (the
    expression layer does not need the tag); `%`'s convertibility class is
    the singleton {`%`} with the ÷100 bridge reserved for PERCENTAGE_VAN;
    PERCENTAGE_VAN becomes upper-dialect sugar with the specified MAAL
    lowering; near-term v0 card: clingo admits a pure-`%` left operand and
    the two anchors respell to the frontend-faithful `eenheid:%` form;
    `factorToCanonical` becomes an exact rational `{num,den}` at the same
    v1 epoch. **Recommendation: yes, as one package.**

**Triage, fences, flip (§§2, 9)**

11. **Triage closure.** Confirm deltas 1–3 as already-settled semantics
    (no new spec text); rewrite the `waardering-v0vorm` reason line
    (non-termination, DRL-oracle ≡ ref); schedule the keurmerk exec-model
    string-encoding fix (moves it to played); CONSTRUCTS row-6 sharpening
    maintainer-serial. **Recommendation: confirm all.**
12. **Ladder fences.** (a) Add a document-layer WF refusal for temporal
    ladder targets (with negatief witness), replacing today's
    executor-only fault, until temporal targets are actually scheduled;
    (b) the allocation-target input fence keeps waiting for the §9
    executor card as recorded; (c) sub-root shared base stays refused
    pending a real statute witness. **Recommendation: yes to all three.**
13. **Onbekend flip witness.** Author the public golden for the
    `numeric_ok` arithmetic arm now (flip-gating checklist item); retire
    arm and golden together at the v1 WF-003 fail-fast promotion.
    **Recommendation: yes.**

**The reality case (§3)**

14. **WW realism case.** Adopt the herleving-WW case as the realism case
    (replacing the zorgtoeslag candidate — plan sentence maintainer-serial):
    lifecycle modelled as a per-evaluation-date scalar decision over an
    event-date fact base (no new temporal construct; timeline-lifecycle
    deferred to a dedicated card once this shows where the atemporal ladder
    pinches); termijnen as ordinary date-arithmetic rules feeding rung
    justifications (a first-class per-ground termijn association recorded
    as a tracked question, not built); samenloop encoded as an ordered
    ladder (confirming the closed §9.5 non-readability decision); art. 19
    onderdelen as separately identifiable rungs tied to their termijn
    regime (R2); authored first as a hand-written anchor from statute text
    and own casus facts only, handbook material barred from all artifacts.
    **Recommendation: yes.**
