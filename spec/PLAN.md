# PLAN — Toward a Normative, Engine-Neutral IR for Machine-Executable Law

*Status: draft v6 · 2026-08-21 · lives in the `uchar` monorepo · companion to
`IR-REVIEW.md` (same directory).*

*Changes in v4 relative to v3: adds §5 (where things live: the monorepo and
what stays outside it); replaces TOKA with the rights-free Jutterswet corpus
throughout (new design principle in §4, corpus workstream in Phase 1,
Phase 5); makes the mirror-then-diverge method and the spec test discipline
explicit in Phase 1; hardens §8 (held-out split physically elsewhere,
enforcement mechanism for protected files, corpus-synthesis circularity,
session-end bookkeeping); extends §9 with the corpus-provenance decision and
two new open decisions (license, visibility).*

*Changes in v5 relative to v4: §9 decisions 7, 9 and 10 are taken (empty
frozen to the mirror + whitelist; Apache-2.0 + CC-BY-4.0; public at latest
at the Phase-1 exit); full-Catala expressibility becomes the Phase-5 goal
(general folds and function values planned core, `CONSTRUCTS.md` rows
10/36); the allocation edge holes and the row-30 "is gevuurd" verdict are
decided in `RUNTIME-SEMANTICS.md`.*

*Changes in v6 relative to v5 (2026-08-21): every phase through 4
carries its Met marking with evidence (Phase 0 via the source-anchoring
landing; Phase 2 with the Drools leg explicitly indirect; Phases 1, 3
and 4 as previously marked), and Phase 5 carries its adopted proposal
with all six decisions taken — the plan states the actual position;
nothing in it waits on a decision.*

## 1. Vision and background

The long-term ambition of this project is inspired by Leibniz's
*characteristica universalis*: a shared formal medium in which rules can be
expressed so precisely that disputes about their consequences reduce to
computation — *calculemus*. Applied to law and regulation, the goal is an
ecosystem in which legislation is written in domain-specific languages
(DSLs), translated into a common semantic core, and executed on multiple rule
engines and solvers, with every derived conclusion traceable back to its
legal source.

Leibniz's programme had two components that are often conflated: the
*characteristica* (the notation in which concepts are expressed) and the
*calculus ratiocinator* (the apparatus that computes over them). Our DSLs —
RegelSpraak today, others tomorrow — are characteristica: different notations
for different domains. The universality does not live in any surface
language; it lives in the shared semantic core they all translate into: one
intermediate representation (IR) with a well-defined meaning, from which we
compile to Drools, Clingo, and whatever comes next.

This is the hourglass architecture that every successful "universal" system
in software uses. LLVM has one IR between dozens of frontend languages and
dozens of backend targets; Pandoc has one internal document model between
thirty formats. The universality sits in the waist. Without it, *n* DSLs and
*m* engines require *n×m* translators; with it, *n+m*. "Universal" is a
regulative ideal — the direction we build in — not a claim that version 1
covers everything. Each new DSL that lands on the core, and each new engine
generated from it, is evidence the core works.

The corollary that shapes everything below: **the IR specification is the
product.** The parsers, transpilers, and runtimes are its proof.

## 2. Where we stand

An independent review of `regelspraak-java` (see `IR-REVIEW.md`, alongside
this plan) assessed the current IR against this ambition. Summary of the
verdict: the project has a good *RegelSpraak-to-Drools* IR, but not yet a
waist.

The IR genuinely models meaning rather than surface syntax: typed expression
trees, bound object types, expanded dimension selections, desugared decision
tables, and rule versions with explicit validity windows. Source snippets are
carried on every expression, and expressiveness gaps are honestly marked
(`Onopgelost` nodes, the coverage manifest). Temporal modelling —
rule-version validity, timelines with granularity, parameter vintages with
fail-fast selection — is strong within its single time axis.

Three findings, however — as the founding review found them; Phases
1–4 have since answered each — blocked the universal ambition, and
they are connected. First, there is no serialization format and no normative
specification: the IR exists only as in-memory Java between resolver and
transpiler, so there is nothing a second frontend or a non-JVM backend could
attach to. Second, the semantics that matter most — empty-value discipline
("empty = 0"), §6 arithmetic, timeline algebra, allocation, the fixpoint
evaluation model and negation stratification — live exclusively as Java code
in `RegelSpraakRuntime`. This is the most dangerous form of leakage: even a
perfect serialization helps a Clingo backend nothing if meaning is defined
only by a JVM runtime. Third, Drools assumptions have reached the IR
definitions themselves: the §9.4 reference-field fact model
(`zetVeld`/`joinVeld`, `scopeVeld`, the top-level-only restriction on distant
role membership), agenda-salience as the stratification mechanism, and IR
nodes documented in terms of what "the Drools transpiler will emit".

None of this is a reproach — for a project with one target, it is the natural
and even sensible order of construction. But it marks precisely the
transition we now face: features that leak straight through to one engine are
exactly what destroys the waist.

## 3. What we borrow, and from whom

We examined existing systems for two different kinds of guidance: *how to
keep an IR normative* (a spec regime) and *what a legal IR must express*
(semantic content). The conclusion: borrow LLVM's discipline, not its IR —
LLVM-IR is imperative SSA at instruction level, the wrong abstraction height
for declarative legal rules — and borrow semantic content from Catala and a
Datalog/ASP core.

| Source | What we take |
|---|---|
| **LLVM (LangRef)** | The spec regime: one normative document per construct (syntax, well-formedness, semantics) separate from any implementation; isomorphic in-memory and textual forms with guaranteed round-trip; a verifier as a separate pass that enforces invariants mechanically; golden tests (FileCheck-style) on the IR form itself. |
| **MLIR** | Progressive lowering via dialects: an upper dialect close to RegelSpraak semantics (roles, fact types, timelines, Dutch names) and a small neutral core IR that backends attach to, with the lowering specified and tested rather than living as transpiler-internal knowledge. |
| **Catala** (INRIA, ICFP 2021) | The closest existing design in our domain: a language for tax law that desugars to a small, formally specified *default calculus* — exceptions and priority as first-class defeasible defaults, not a salience trick. Also: literate programming interweaving rule text and statute text (our `// Bron:` need), and multiple verified backends from one core. |
| **Datalog with stratified negation / ASP-Core-2** | A semantic anchor: define the meaning of the core IR by translation into a logic with model-theoretic semantics. Stratification becomes an IR well-formedness condition instead of a Drools agenda trick, and a Clingo backend becomes nearly a pretty-printer. |
| **LegalRuleML** (OASIS) | Not the XML vehicle, but the requirements list: defeasibility with override relations, multiple time axes, jurisdiction. Standing test: "can our IR express what LegalRuleML can?" |
| **eFLINT / Wetsanalyse (JAS)** | A second standing requirements list, for the normative layer: `spec/proposals/DEONTIEK-EISENLIJST.md` (route R0 of the 2026-09-02 exploration; row 32 stays closed until the meldplichtspoor level-0 evidence). |
| **DMN/FEEL** (OMG) | An example of vendor-neutral normative decision semantics (including decision-table hit policies) — but weak on time and defeasibility; a partial aspect, not a model. |
| **OpenFisca** | Confirms our parameter-vintage design (validity-periodised parameters and formulas), and demonstrates how far you get — and where it hurts — when the Python code *is* the spec. |
| **GHC Core** | Keep the core small: all sugar desugars to a minimal typed kernel, and one typecheck on the kernel guards the whole compiler. `BeslistabelDesugar` already does this; the standing question per IR node is: *kernel, or sugar that desugars?* |
| **WebAssembly spec** | The gold standard for "spec first": formal semantics, validation rules, textual and binary forms, and a reference interpreter that implementations demonstrably must not diverge from. |

If one thing is read before starting: the Catala paper ("Catala: A
Programming Language for the Law", Merigoux, Chataing & Protzenko, ICFP
2021). It is the only design known to us that has actually solved our three
hardest requirements — defeasibility, statute-text traceability, and multiple
backends from one formal core — in the same domain.

## 4. Design principles

**The waist must be strict.** Any DSL feature not expressible in the IR,
leaking directly to one engine, forfeits the universality the project exists
for. The coverage manifest's honest LOWERED/MARKER/SKIP verdicts are the
template: the core may say "I cannot express this (yet)" — it may not
pretend.

**Meaning moves out of code in three steps.** From `RegelSpraakRuntime`
(shared Java, hence shared bugs possible) → to IR-SPEC plus a reference
interpreter (an executable specification) → to an ASP translation (a
model-theoretic anchor). Each step gets a gatekeeper in the gate set.

**Dialect and core are separated (MLIR lesson).** RegelSpraak-specific
richness — Dutch names, the §9.10 rule-group extension, CNL-flavoured
constructs — lives in an upper dialect. The core IR stays small,
language-neutral, and engine-neutral. This also settles two open questions
from the review: the metamodel (node names, structure) becomes
language-independent, while domain names (object types, attributes, roles)
remain whatever the source declares — the core is agnostic about them.

**Adopt, don't invent, the defeasibility model.** Catala's default calculus
(prioritised defaults with conflict detection) is formally worked out and
mechanically proven. Adopting it makes the future Catala frontend nearly
structure-preserving and spares us designing a novel exception logic.

**Traceability is a first-class obligation.** Every derived legal conclusion
must carry a thread back through the IR to the exact source rule, file,
position, and — via `// Bron:` annotations — the statute article. For a
law-execution IR this is the core promise, not a nice-to-have.

**The published corpus is rights-free by construction.** The corpus ships
with the spec and inherits its obligations. Own-authored casus material (the
Jutterswet) and statute text (copyright-free under art. 11 Auteurswet) only;
no RegelSpraak specification excerpts enter the uchar repos (the
specification carries "Alle rechten voorbehouden"), with one exception: the
TOKA casus, which is not corpus material and lives in the `toka/` parity
zone, outside `corpus/` (§9 (10); `CLAUDE.md`, Corpus rules). A test case
that cannot be published is not a test case for a public spec.

**Backend diversity before frontend diversity.** The deepest problem is "the
runtime is the bearer of meaning". A reference interpreter attacks it first
and cheapest; a semantically alien backend (Clingo) then proves neutrality. A
second frontend (Catala) tests the top of the hourglass but leaves the bottom
intact, so it comes later.

**"Empty = 0" is a design question, not a fact to document.** Whether a
missing income is zero or unknown is a substantive legal choice with
three-valued-logic consequences, and it interacts directly with open- versus
closed-world modelling in ASP. The IR spec records it as an explicit decision
with its exception cases, not as inherited behaviour.

## 5. Where things live

One monorepo, `uchar-dev/uchar`, holds the product and everything that proves
it:

```
uchar/
├── spec/        PLAN.md (this file), IR-REVIEW.md, IR-SPEC.md,
│                RUNTIME-SEMANTICS.md, CONSTRUCTS.md, ASP-TRANSLATION.md
│                (Phase 3), schema/, verifier/ (Phase 1)
├── corpus/      golden corpus (incl. the pinned anchor/ subset),
│                negatief-corpus, casus source material (jutterswet/)
├── ref/         reference interpreter (Phase 2)
├── frontends/
│   ├── catala/       Catala plugin at the scopelang boundary (Phase 5)
│   └── regelspraak/  the RegelSpraak parser+resolver+emitter — a copy
│                     of the toolchain's frontend half (2026-08-27),
│                     drift-guarded by gate 10's byte-identical
│                     re-emission of every rgs-carrying golden
└── backends/
    ├── clingo/  (Phase 3)
    └── drools/  the transpiler + production runtime copied from the
                 toolchain (2026-08-27), consuming the wire through its
                 own ir.json loader; gate 12 plays the differential
                 square over the carried subset with a loud exclusion
                 list
```

The layout borrows LLVM's shape — self-contained siblings around a core —
with one deliberate inversion: LLVM keeps LangRef inside `llvm/` because
there the implementation is the product; here `spec/` is a top-level sibling
that owns nothing and is owned by nothing, because the spec is the product.
CI enforces the dependency direction: `spec/` and `corpus/` depend on
nothing; `ref/` shares no code with any production runtime.

Three things live deliberately outside the monorepo:

- **`regelspraak-java`** — the first frontend + Drools backend pair, a mature
  toolchain with its own gate set. It consumes this repo at a pinned
  `SPEC_REV`; changes flow spec-first (the delta lands here with corpus and
  schema, then a pin-bump conformance PR lands there). If external
  implementers ever require the spec to stand alone, `git subtree split`
  extracts it with history; if two-repo friction ever outweighs independence,
  the toolchain moves in. Both doors stay open. *(Partially exercised
  2026-08-27: the frontend half — grammar, resolver, IR emitter — lives
  in the monorepo as `frontends/regelspraak` on the copy route (four
  ratified decisions), dual-pinned against drift by gate 10 here and
  `uchar-conformance` there; the Drools backend and production runtime
  stay outside. The planned upgrade, once the defaults-producing frontend
  evolution starts, is inverting the dependency so the toolchain
  consumes this frontend — one source of frontend truth. Ratified
  2026-08-29, `PRIORITIZED-DEFAULTS-EVOLUTIE.md` Q5: vendored-subtree consumption at the
  SPEC_REV pin, scheduled ahead of the frontend evolution —
  freeze-invert-evolve.)*
- **The held-out corpus split** — a separate private repository (e.g.
  `uchar-corpus-heldout`), pulled only by an acceptance job (§8). Planned;
  not created as of 2026-09-26.
- **The org profile** (`.github` repo) — the public landing page until the
  spec is worth publishing; then `uchar.dev` serves `spec/` via GitHub Pages,
  so the domain serves the product itself.

## 6. Phases

Phases overlap deliberately where noted; each has an exit criterion that
lands in the gate set.

### Phase 0 — Foundation (weeks, not months)

Two jobs that carry no design risk, plus the reading assignment (the Catala
paper).

*Source anchoring* (work lands in `regelspraak-java`). Replace the bare
`String bron` with `Bron(file, line, column, snippet)` on `Expr`, `Conditie`,
and every `Resolved*` form, and pass `// Bron:` statute annotations through
the lexer on a hidden channel so explanations can cite articles of law.
Small, mechanical, touches no semantics — and it closes the most painful gap
in the review for a law-execution project.

*Constructs matrix → `spec/CONSTRUCTS.md`* (this repo; a seeded skeleton
exists). This is the right form of "Catala as input" at this stage: not
building a frontend, but mining Catala's default calculus as a requirements
list — which constructs exist there (exception trees with priority, scopes
with in/out variables, ConflictError, general folds and function values) and
what their fate in our IR is: v1 scope, explicit non-goal, or later. Do the
same with our own coverage manifest (`Onopgelost` inventory), LegalRuleML's
requirements (defeasibility with overrides, multiple time axes,
jurisdiction), and optionally OpenFisca and DMN. Give every row the GHC-Core
column: *kernel, or sugar?* This document forces us to define what
"universal" means in v1 before anything is serialized — and its delta column
carries the v0→v1 divergences (see Phase 1), which doubles as the Phase 0–4
roadmap.

**Exit criteria:** every IR node carries file/line/column; explanations can
cite statute annotations; `spec/CONSTRUCTS.md` has an explicit v1 scope
decision per construct.
**Met (2026-08-21):** the constructs matrix has carried its per-row v1
verdict since the Phase-0 card that seeded it (37 rows, the delta
column doubling as the roadmap). The source-anchoring card
(`backlog/done/20260815-bron-anchoring.md`) landed the other two
criteria in `regelspraak-java`: the `Bron(file, line, column,
snippet)` record replaces the bare string on `Expr`, `Conditie` and
every executable-law `Resolved*` form that ever carried a source
anchor (the test-harness `ResolvedScenario`, row 37, keeps its
string by design) — real
coordinates at every resolver construction site bar one synthetic
literal, with container forms deriving position through their
component expressions — and `// Bron:` statute annotations ride the
lexer's hidden channel onto `ResolvedRegel.bronAnnotaties`,
retrievable from the resolved form. The v0 wire is byte-identical
throughout (the emitter-golden conformance at the `SPEC_REV` pin);
the structured-`Bron` *wire* form remains the tracked row-3 v1 delta,
by design.

### Phase 1 — The spec regime exists (months; the big job)

The method first: **mirror, then diverge.** The `--ir` dump of the *current*
`regelspraak-java` IR is **v0** — a faithful snapshot, warts included, at a
pinned commit. `IR-SPEC.md` describes **v1**, deviating deliberately where
the review flagged Drools leakage (bean-field names → role bindings, salience
→ declared stratification, `bron` string → structured source references).
Each deviation is a tracked delta with a migration note, recorded in the
`CONSTRUCTS.md` delta column — never a silent rewrite. The spec starts as a
mirror and ends as the master.

Then the LLVM move, in four parts. (a) `spec/IR-SPEC.md` as our LangRef: per
node kind its structure, well-formedness conditions, and semantics — lifted
from the already spec-§-citing javadoc, which moves from "commentary on Java"
to "document Java must conform to". (b) A JSON serialization via `Resolver
--ir out.json` (the emitter lands in `regelspraak-java`; the schema and the
emitted corpus files land here), with round-trip tests — two isomorphic forms
suffice; we skip LLVM's third, binary form. (c) A verifier as a separate
pass, living in `spec/verifier/` — *not* in the Java resolver — mechanically
enforcing the invariants that today live as comments (e.g. the `AttribuutRef`
navigation invariant noted at `Expr.java:167-172`): enforced once, not
re-assumed per backend. (d) A golden corpus of (source → IR-JSON) pairs in
`corpus/golden/`, FileCheck-style, so every IR change is visible as a diff.

The spec's own test discipline, from day one: every normative MUST carries an
ID (`WF-###`) with a verifier check, at least one passing golden case, and
one deliberately malformed failing case in `corpus/negative/` — the
negatief-corpus discipline `regelspraak-java` already runs as gate 7bis,
applied to the IR itself. Every semantics clause carries a worked example
with concrete expected output; those examples become the reference
interpreter's test suite verbatim in Phase 2. The standing rule: **a spec
sentence without a machine-checkable artifact is not done.**

*The corpus workstream.* The golden corpus is built on the **Jutterswet**
(see `corpus/jutterswet/`): rights-free by construction, and — through its
British Nationality Act lineage — comparable against four decades of
logic-programming and ASP literature, which is precisely what the Phase-3
differential square wants. The casus is extended with the quantitative
machinery the retired TOKA casus used to exercise: allocation with
maximum/rounding/remainder, units and dimensions, timelines and variable
starting points, parameter vintages, day-kinds, uniqueness and consistency,
object/fact creation, decision tables (the extension table lives in
`corpus/jutterswet/README.md`). One real regulation joins as realism case
(statute text is copyright-free under art. 11 Auteurswet; decided
2026-08-29, `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` Q14: **WW-herleving**,
art. 15–21 WW — a genuine layered default/exception structure with
termijnen, the defaults round's reality test — replacing the earlier
zorgtoeslag candidate). Whether `regelspraak-java`'s *internal*
gates also migrate off TOKA is that repo's own decision, out of scope here;
the corpus is TOKA-free from its first commit, and the TOKA casus has its
own parity zone, `toka/`, since 2026-08-27 (§9 (10)).

Take the dialect/core split as an explicit design decision in the spec now,
even if the mechanical separation lands later: mark per node whether it
belongs to the RegelSpraak dialect or the neutral core, and specify the
lowering between them as a testable step.

Alongside the spec: `RUNTIME-SEMANTICS.md` capturing empty-value discipline
(as an open design question with its cases), §6 arithmetic and rounding,
timeline algebra (cut-merge, pro-rata), allocation as recurrence equations
over groups rather than "one helper call", the fixpoint evaluation model, the
stratification requirement for negation over derived characteristics, and
initialisation as an explicitly defeasible default. Version the spec from day
one.

**Exit criteria:** a second program — the verifier in `spec/verifier/`, not
the Java resolver — can load and verify an IR file; the golden corpus and
round-trip tests gate CI; every well-formedness rule in the spec has a
verifier check, a passing case, and a failing negative case; the corpus is
rights-free (Jutterswet-based) from its first commit. **Met (2026-08-19):**
all four hold (the WF index carries no unmotivated OPEN), and the normative
documents graduated into the self-containedness gate. The §9 decision 10
public flip is now unblocked and awaits the maintainer.

### Phase 2 — Reference interpreter (starts as soon as the JSON form exists)

A small, slow, readable tree-walker over the IR, in `ref/`: not a third
production backend, but the executable definition of the semantics, on the
WebAssembly model. Language free of choice; it must share no code with
`RegelSpraakRuntime` — otherwise it defines nothing — and the repo split
makes that structural (no copy-paste imports either). The existing
differential test becomes a triangle: both Drools backends must reproduce the
reference interpreter over the golden corpus. `RUNTIME-SEMANTICS.md` remains
as prose accompaniment — its worked examples are the interpreter's unit tests
verbatim; the interpreter is the arbiter.

This is where meaning first leaves `RegelSpraakRuntime`. It is also where the
default model (Phase 4) gets implemented first.

**Exit criteria:** interpreter passes the golden corpus; the
Drools≡interpreter differential gates CI; no shared code with the production
runtime (structural: different repository, no dependency).
**Met (2026-08-21), with the Drools leg indirect:** gate 5
(`scripts/check-oracle.sh`) replays every golden case through `ref/`
with exact expectation match, and the no-shared-code criterion is
structural fact (separate repository, no dependency, no copy-paste —
`ref/` was written against the spec). The Drools≡interpreter leg is
**indirect and partial**, in the Phase-4 house phrasing: the
toolchain's own differential holds its two Drools back-ends to each
other and to pinned absolute values (both share the one v0 runtime —
parity plus pins, not an independent oracle), while the
emitter-golden conformance (`make uchar-conformance` at the
`SPEC_REV` pin) plus gates 5–6 hold the same corpus *documents* to
`ref/` and `clingo`. *(Flank closed 2026-08-27:)* gate 12 (`scripts/check-drools.sh`) now
runs the copied Drools backend (`backends/drools`) directly against
uchar cases — loading `ir.json` off the wire through its own loader,
never the RegelSpraak source — and requires the same pinned
expectations gates 5–6 verify, over the carried subset (41 goldens
played at landing) with a reason-carrying exclusion list that fails
when stale; the uncarried forms (defaults groups, folds, structs, and the
listed v0 leeg-semantics divergences) stay loud, not silent.

### Phase 3 — Semantic anchor in ASP, then the Clingo backend

First on paper: translate the core IR into stratified Datalog with aggregates
(ASP-Core-2), even if only for a subset — documented as
`spec/ASP-TRANSLATION.md`. Every node that does not translate cleanly is
still carrying a Drools assumption — the review's candidates are the
reference-field fact model (`zetVeld`/`joinVeld`, `scopeVeld`, `backref`),
the top-level-only restriction on distant role membership, and implicit
evaluation order. This exercise forces the "relations first-class" refactor:
replace field-name parameters with symbolic role bindings
(`RolBinding(feittype, rol)`), pushing the role-binding→bean-field derivation
down into `regelspraak-java`'s `schema/` and its Drools backends. In ASP a
fact type then becomes simply a predicate.

Then build the Clingo generator in `backends/clingo/` for that subset — near
a pretty-printer if the anchor is right: assignment, characteristic
attribution, the initialisation default, one role membership, one timeline.
Differential testing becomes a square: Drools ≡ interpreter ≡ Clingo over the
golden corpus. Every divergence is by definition a spec bug.

Start the hand translation of one representative rule *during* Phase 1, not
after it — a spec written without a second, alien target breaks at first
contact. Nothing exposes IR shortcomings as mercilessly as actually serving a
semantically different engine.

**Exit criteria:** documented ASP translation of the core; role bindings
replace bean-field names in the IR; Clingo backend passes the differential
square on the subset; "engine-neutral" is a claim with evidence.
**Met (2026-08-21):** the systematic translation is
`spec/ASP-TRANSLATION.md` Part I (S1–S19: every translated construct
with its gate witnesses, and the complete loud non-translation
inventory in S19 — the core may say "not yet", it may not pretend).
Criterion 2 is met under the **semantic reading**, ratified by the
maintainer (2026-08-21, numbered question): the symbolic role
bindings are first-class on the wire and are the only thing the uchar
executors execute — the review's reference-field trio each landed its
symbolic successor with a WF rule (fact creation's
`zetVeld`/`joinVeld` → the symbolic ch. 3.4 form, IR-SPEC
ch. 3.4/WF-028; uniqueness's `scopeVeld` → `scopeRol`, IR-SPEC
ch. 3.6/WF-054; the far-side `backref` → the subject-side role of
RoleMembership, `CONSTRUCTS.md` row 5 — with navigated reads bound
symbolically besides, ch. 4.1a/WF-053); the executors never read
`zetVeld`/`joinVeld` (a bean-field-only fact creation faults loudly)
and the `scopeVeld` fallback resolves a *declared role by name*,
corpus-witnessed. The v0 wire still *carries* the bean fields as
inert mirror baggage — mirror-then-diverge requires it — and their
syntactic removal is the tracked v1 migration; the
role-binding→bean-field derivation lives in `regelspraak-java`'s
schema/resolver with its Drools backends consuming the result (the
ch. 3.4 migration note's forcing move, made on the executing side).
The differential holds as gates 5–6 (ref ≡ clingo, anchored to
hand-written absolute values — corpus-scoped, as every such claim
is), and the toolchain conforms through the `SPEC_REV` pin
(`make uchar-conformance`: every emitter golden byte-identical at the
pinned revision). In ASP a fact type is simply the predicate `role/3`
— the claim this phase existed to earn.

### Phase 4 — The default model in the IR

Adopt Catala's default calculus (prioritised defaults with conflict
detection) as the IR's defeasibility model — the hard dependency identified
for any Catala interoperability, and the declarative replacement for today's
mechanisms. Initialisation-as-default, rule-version selection, and negation
stratification become explicit IR notions; the salience −1 trick at
`DrlTranspiler.java:1045-1047` becomes a derivable lowering of a specified
requirement instead of the bearer of it. Unordered simultaneous exceptions
map onto the existing §9.5 inconsistency channel.

May run partly in parallel with Phase 3; the reference interpreter implements
the model first — that is where it is tested.

**Exit criteria:** defaults, priorities, and conflicts are expressible and
verified in the IR; the salience mechanism in the Drools backend is generated
from IR-level defaults; interpreter and both engine backends agree on default
resolution over new golden cases. **Met (2026-08-20), on the v0-expressible
fragment:** the prioritized-defaults group rides the wire in both target kinds with WF-050/051
(`prioritizedDefaults`; anchors through both executors, incl. the ch. 7.8
standing norm and the input default — `RUNTIME-SEMANTICS.md` ch. 7,
`spec/proposals/PHASE4-DEFAULTS.md`); the Drools salience is derived from
the declared kenmerk graph (the WF-046 strata principle as a lowering,
`CONSTRUCTS.md` row 6, toolchain pin `0b7f284`) instead of the syntactic
trick this section names; and ref ≡ clingo holds over the defaults corpus
(gates 5–6), with the Drools side agreeing over the shared v0 forms through
its own differential. Deliberately open, recorded: full defaults service in
the Drools backend waits for a defaults-producing frontend (Phase 5 — building
it against hand-authored IR tests no production path) *(scheduled
2026-08-29: the deferral premise is met by the Catala plugin —
`PRIORITIZED-DEFAULTS-EVOLUTIE.md` Q3)*; temporal standing
targets stay fenced (scalar-first, RS ch. 7.8); and one latent, non-defaults
loader divergence is carded (`backlog/done/20260820-input-precision-fence.md`:
ref accepts over-precise case input clingo faults — no corpus case exercises
it, so the gates-5–6 agreement above is corpus-scoped, as every such claim
is).

### Phase 5 — Catala as a frontend (direction 1)

Only now: an OCaml backend plugin for the Catala compiler, attaching at the
default-calculus level (all sugar gone, types known, exception trees
normalised — parsing Catala ourselves would duplicate their desugaring),
emitting our IR JSON. It lands on the *core* IR, not the RegelSpraak dialect.
Start with Catala's tutorial example through the plugin, next to a Jutterswet
fragment through the existing resolver, and diff both IR dumps: where two
frontends express the same semantics as different IR, an IR design flaw is
exposed — exactly the feedback a second frontend is for.

Known fits and frictions from the analysis: ConflictError maps well onto
§9.5; money/rationals/dates/durations fit our exact arithmetic; scopes with
rebound inputs are real design work (object type + object creation +
navigation); general folds and function values are kernel
since the Phase-5b landing, 2026-08-22 (`CONSTRUCTS.md` rows 10/36) —
full-Catala expressibility is the goal, not a subset stance; and on
time the mismatch runs the other way — our IR is richer (timelines, vintages)
than Catala, which handles law versions mainly at source level. Catala's
evaluation model is weaker than our fixpoint forward-chaining, and compiling
weak-to-strong is the good direction.

**Exit criteria:** the tutorial example round-trips through the plugin into
valid, verifier-clean IR and executes on interpreter + one engine; the
frontend-diff exercise is documented with resulting spec changes; the
hourglass is real — two frontends, two-plus backends, 2×2 through one waist
instead of four translators.
**Met (2026-08-24):** both exit criteria stand. The first is discharged —
`tutorial_end_2_3` round-trips through the plugin into valid,
verifier-clean IR and executes on interpreter + one engine (plugin →
verifier → ref → clingo, triple-green under gate 9), on the subscope-call
lowering of `spec/proposals/PHASE5C-TUTORIAL.md` §3 (object creation +
member-init `Toekenningen` + `verbindendeRol` + navigated output reads)
with the WF-027 delta for boolean subscope inputs (`CONSTRUCTS.md`
rows 9/21); the own-authored Jutterswet witnesses —
`anchor/jutterswet-rekenkamer`, `anchor/jutterswet-rekenkamer-overzee`,
and the Catala golden `jutterswet-rekenkamerheffing-catala` — pass the
full gate set. **The frontend-diff criterion is now discharged (card 5d):**
`spec/proposals/PHASE5D-DIFF.md` runs the same law — Jutterswet art. 3 jo. 5
(penningrecht / hardheid) — through both frontends (the RegelSpraak toolchain
resolver, two flat `KENMERKTOEKENNING` rules; the Catala plugin, one
`prioritizedDefaults` group) and diffs the two IR dumps, both carried through the full
gate set: every difference is administrative or a tracked v0↔v1 delta
(`CONSTRUCTS.md` rows 8/2/7), no category-(iii) design flaw, and the resulting
spec change is one besluit-free documentary note (no normative change, no
numbered maintainer question). With both criteria met the hourglass is real —
two frontends, two-plus backends, 2×2 through one waist instead of four
translators.
**Proposal adopted (2026-08-21):** `spec/proposals/PHASE5-CATALA.md`
carries the design; its six decisions are all taken (attachment at the
scopelang→dcalc boundary; context-override polarity as a top default —
Reading B as default order, the input default reserved for the case dossier;
the rights-clean corpus route: tutorial frontend-side, a
Jutterswet-in-Catala article as the corpus case; `frontends/catala/`
in this monorepo with the separate-repo escape hatch recorded; loud
refusals in 5a for sub-root branching, folds/function values, payload
enums and cross-scope ∅-fallback; staged 5a→5d with 5b interleaving).
The 5a cards sit in the backlog.

### Phase 6 — Catala as a backend: opportunistic, possibly never

Deliberately last, against the "direction 2 first, cheap road to backend
diversity" option. Two reasons. Semantically: our fixpoint forward-chaining
is stronger than Catala's model, so IR→Catala compiles strong-to-weak — the
hard direction, feasible only for a subset, and less instructive than Clingo,
which is expressive enough for our whole semantics and therefore exposes
every gap. Strategically: it tempts us to leave the hard questions (defaults,
stratification, emptiness) unanswered because Catala's core solves them for
us — at which point their core quietly becomes ours, and our IR degrades to a
frontend intermediate. The value (riding Catala's verified C/JS/Python
backends) survives as a later extra, once our waist is proven; then it is
simply backend number three and degrades nothing.

## 7. Sequencing rationale, in one paragraph

The order is: make the waist exist (spec + serialization + verifier), make
meaning executable outside the production runtime (reference interpreter),
anchor it model-theoretically and prove neutrality (ASP + Clingo), make
defeasibility first-class (default calculus), then prove frontend-neutrality
(Catala in), and only opportunistically compile out to Catala. Backend
diversity precedes frontend diversity because the deepest defect is where
meaning lives, not how it is written down. The phases interleave on purpose:
the ASP hand-translation runs during spec-writing, and the interpreter
implements the default model before any engine does. Meaning thus moves in
three auditable steps — code → executable spec → logic — each with a
gatekeeper in the gate set.

## 8. Multi-session agentic development — the RL harness, minus the RL

This project is built over many stateless agent sessions (Claude Code and
kin), not by training a model. The 2025–26 environment-synthesis and
verifier-synthesis literature is nevertheless directly useful, because its
transferable contribution is harness discipline, not gradient updates. The
translation: in RL, what accumulates across episodes is model weights; here,
what accumulates across sessions is artifacts — the spec, the verifier, the
golden corpus, the gate set, this plan. Each session starts fresh but lands
in a persistently hardened environment. The monotonic gate set is the memory
that replaces learning.

Four practices are adopted from that literature.

*Sessions are tasks with acceptance criteria.* The differential square
doubles as a per-task reward signal at inference time: "implement construct X
in backend Y until Drools ≡ interpreter ≡ Clingo passes over the corpus" is a
fully specified session, verifiable without human line-by-line review. The
exit criteria in §6 get broken down into session-sized issues phrased this
way.

*Corpus synthesis (from Phase 2 onward).* Once the reference interpreter
exists, generator sessions scale the golden corpus from dozens of
hand-written cases to thousands: synthesized domain schemas, rule sets,
parameter vintages, and input cases. Coverage is steered by `CONSTRUCTS.md`
rows; admission is gated three times — the verifier rejects ill-formed
programs (the "unsolvable" failure mode), a small coverage judge rejects
degenerate programs that pass without exercising the construct they claim to
test (the "trivially given away" mode), and a case is admitted **only when
the full differential square agrees on it**: ground truth computed by the
interpreter alone would canonize interpreter bugs into thousands of cases, so
disagreement routes to the adversarial queue, never to admission. The
hand-written anchor subset (`corpus/golden/anchor/`) with absolute expected
values stays permanently pinned as the parity-independent ground truth — the
same "differential is parity-only, closed forms pinned to absolute spec
values" principle `regelspraak-java` already enforces.

*Adversarial sessions (from Phase 3 onward).* A standing hacker–fixer-style
job: a session whose only goal is to find programs on which the differential
square diverges, or which the verifier wrongly accepts. Every find is
minimized and enters the corpus. Compiler practice (differential testing in
the Csmith/EMI tradition) shows this loop needs no RL infrastructure; the
agentic layer adds generation of semantically interesting, legal-shaped cases
— interacting exceptions, vintage boundaries, empty-value edges — where
random generators produce noise.

*Harness guards as standing CI.* The oracle/no-op/unsolved-state protocol
transfers verbatim: an interpreter replay of each corpus case must pass
(oracle); an identity transpiler must fail (no-op); `Onopgelost` must surface
in output rather than vanish (unsolved state). These three checks guard the
harness itself against a session that quietly gutted it.

**Gate-set policy.** Reward hacking requires no gradients: a coding agent
told to "make the tests pass" will, some fraction of the time, special-case
the corpus, weaken an assertion, or edit the verifier. The defenses are
therefore policy, *mechanized*, not optimism. A held-out corpus split was
planned **in a separate private repository, pulled only by an acceptance
job** — held-out material inside the working repo would be theater for a
repo-reading agent. It does NOT exist, and no acceptance is measured on one
(recorded 2026-09-26, after an independent review found the earlier
sentence untrue; if it comes, it comes as that separate repository). The protected paths — the
authoritative list lives in `CLAUDE.md` (Protected paths), with
`.github/CODEOWNERS` as its mechanical mirror — take proposals from
sessions, never merges: the mandate-and-review protocol of
`WAY-OF-WORKING.md` carries it, and no separate CI check exists (recorded
2026-09-10). The repository is public since 2026-10-12, so the free org
plan allows branch protection; it is not enabled (2026-10-13), so GitHub
does not enforce the CODEOWNERS review, and whether to enable it is open.
The gate set runs locally before every landing — GitHub Actions
is not used (maintainer decision 2026-09-26: no paid compute; the workflow
file is hand-triggered only, `CLAUDE.md` Gate set) — so a weakened harness
fails loudly rather than silently, on the maintainer's machine. And
because the agent is stateless, the repository is the state: `CLAUDE.md`
carries the invariants and the protected-path list; every session ends by
updating the issue it worked, and a scope change reaches this plan through
the main session's serial bookkeeping (`CLAUDE.md`, Protected paths);
`CLAUDE.md` itself changes only when an invariant or protected path changes,
so it stays a constitution rather than a logbook.

What we deliberately do not adopt: RL training frameworks, trajectory
collection, and environment wrappers (OpenEnv and kin) — they exist to feed
training we are not doing. Publishing uchar as a deterministic, formally
specified environment suite for *others'* training remains an optional
community play once the core is proven, in service of the ecosystem ambition
of §1, and is out of scope for development itself.

## 9. Open decisions

Carried from the review; the plan takes positions, to be confirmed or
overruled by the maintainer. (1) The IR is a public, stable, versioned
interface — that is the product; decided by this plan, to be stated in the
repo. (2) The single time axis (valuation date = calculation date = vintage
selector) is accepted v1 scope; the spec must name the axis explicitly and
design the extension point for a second, knowledge-time axis (retroactive
legislation) so adding it later is not a re-foundation. (3) `Onopgelost`
nodes are a specified, permanent part of the contract — honest
inexpressibility beats feigned completeness — matching how the coverage
manifest already treats them. (4) BEREKENING vs GELIJKSTELLING: absent a
behavioural difference, one semantic node with a presentation tag for
verbalisation; two nodes without a meaning difference are surface syntax
sneaking into the core. (5) The §9.10 rule group stays in the RegelSpraak
dialect, out of the core. (6) Metamodel names go language-neutral/English;
declared domain names stay as-authored. (7) **Decided (2026-08-16):** the
empty-value discipline is frozen to the documented mirror with its coalesce
whitelist (`RUNTIME-SEMANTICS.md` ch. 1.2–1.4); empty-as-unknown is a
non-goal; numeric-zero division routes to the §9.5 channel in v1; the
open/closed-world split is expressed through Phase-4 defaults, not operator
behaviour. **Amended (2026-08-28, source-verified against the official
typeringen tables):** inequality with exactly one empty operand is WAAR,
uniform over every type — Tabel 44 followed completely; the previous
text/enum/boolean any-empty-false cell was an undocumented, internally
inconsistent deviation (dates already followed the numeric rule) and its
five corpus pins were re-pinned hand-computed, closing two gate-12
divergences. The equality cells stand unchanged, including the
non-numeric any-empty-equals-false mirror of v0's guard.

New in v4. (8) **Decided:** the published corpus is rights-free by
construction — the Jutterswet (plus its quantitative extensions) replaces
TOKA as the golden corpus, and one statute-based case (art. 11 Auteurswet;
2026-08-29: WW-herleving, `PRIORITIZED-DEFAULTS-EVOLUTIE.md` Q14 — previously
zorgtoeslag) joins for realism; no ©-Belastingdienst material
enters the uchar repos. **Amended (2026-10-13):** the clause no longer
covers the TOKA casus, Bijlage 1 of the RegelSpraak specification: it
stays outside the corpus, in the `toka/` parity zone (see (10)). Other
specification excerpts still do not enter the uchar repos; the
specification carries "Alle rechten voorbehouden". (9) **Decided
(2026-08-16):** Apache-2.0 for code (verifier, reference interpreter,
backends, gate scripts — and the JSON schemas, which ship as functional
artifacts) and CC-BY-4.0 for the specification and corpus text; the
license files live at the repository root. (10) **Decided
(2026-08-16):** public at latest at the Phase-1 exit — §8's enforcement
mechanism (branch protection requires a public repo on the free org plan)
argues for flipping early, and the rights hygiene (gate 1, the TOKA
tripwire) is in place. **Deferred (2026-08-19):** at the Phase-1 exit the
maintainer holds the flip — the repo stays private until further notice;
the enforcement caveat (no branch protection on a private free-plan repo)
stands with it. **Precondition added (2026-08-27):** the temporary
`toka/` parity zone admits ©-Belastingdienst material into the tree AND
the git history; the flip now additionally requires removing the zone
and rewriting history (filter-repo or a fresh public copy) — recorded in
`CLAUDE.md` with the zone's charter. **Taken (2026-10-12):** `uchar` and
`regelspraak-toolchain` are public. **Amended (2026-10-13, maintainer):**
the precondition of 2026-08-27 is withdrawn. The `toka/` zone stays, its
git history is not rewritten, and P11 of decision (11) lapses; gate 11 is
a regular gate of the set, so the set has no documented exception to
monotone growth any more. The provenance and status of the TOKA casus
stand in one fixed text in `toka/README.md`, `README.md` and `CLAUDE.md`:
the Belastingdienst indicated to the maintainer on 2026-10-13 that TOKA
is meant to be releasable; written confirmation is not yet received.
Branch protection is available on the public repository and not enabled
(2026-10-13); whether to enable it is open.


New in v5. (11) **Decided (2026-09-23):** the v1 wire migration
(`proposals/V1-WIREMIGRATIE.md`; all seventeen of its questions decided
2026-09-19) lands in FIVE landings instead of the eight M-cards the
proposal sliced (its §7): L1 = M1, the container (card
`backlog/doing/20260923-m1-schema-en-container.md`, in flight); L2 =
M2+M3, the reference layer (bindings authoritative, bean fields and the
reference `type` annotation off the wire, gate 13 revised per V15); L3 =
M4, the type system alone, because it is the landing most likely to move
gates 5/6/12 and that movement must be seen on its own; L4 = M5+M6, the
rule and expression layer; L5 = M7+M8, the source record and the rename,
with M7 kept as the null measurement in the card's first commit. Each
landing regenerates every corpus zone and the toka zone, holds all
fourteen gates green; the toolchain re-pin waits for the end of the migration
(maintainer decision 2026-09-24: one pin bump when v1 is complete). The `toka/`
zone stays through the whole migration — decision 10's precondition is
amended in timing only: gate 11 becomes a per-landing regression gate,
and P11 (zone removal plus history rewrite) executes at the public flip.
**L1 landed (2026-09-24, `32185b7b`, card `backlog/done/20260923-m1-schema-en-container.md`):**
`uchar-ir-v1` with the nested root `{format, model, rules, diagnostics}`,
the six registries as lists, `ObjectType` with member nodes, the `Domain`
wrapper, the `regels` stubs gone, `diagnostics` kept under WF-113; 21
negative witnesses removed with reason, one rewritten; all fourteen gates
green on the landing (gate 12 pins 185/164 with the WF-113 golden
`jutterswet-kaletabel`).
**L2 landed (2026-09-24, `fd3f0a73`, card `backlog/done/20260923-l2-referentielaag.md`):**
the reference layer — every reference symbolic (B11 one explicit path, B13 no
`type` on the five reference positions, B12 bindings required with WF-048
retired, B7/B8 bean fields gone, B9 one `RoleMembership` with the optional
`counterpartType`, B16 `Onbekend` gone, `Kenmerk.navigatie` gone); migration
`spec/proposals/v1-migratie/m2.py`; negative 444 → 400 with the stamp list and
a per-witness `expect.txt` arm fragment checked by gate 3; gate 13 revised to
the derived positions (38 twists); gate 12 pins 188/166; two red cases became
goldens; all fourteen gates green on the landing.
**L3 landed (2026-09-24, `4439d802`, card `backlog/done/20260924-l3-typesysteem.md`):**
the type system — `Number` with the `%` unit and the declaration tag (B14),
`Date {precision}` (B15), the exact rational `factorToCanonical {num, den}`
(B17 (i); V7's fallback invoked, no canonicalisation pass), the parameter
timeline facet (B18), `UnitSystem {name, units}` with spellings and `DayKind
{name, plural?}`; migration `spec/proposals/v1-migratie/m3.py` (the parity
zone's 540 pre-rounded factors reconstructed and confirmed byte-identical by
the frontend's re-emission); negative 400 → 407 with 165 arm fragments; no
gate-5/6/12 outcome moved; all fourteen gates green on the landing.
**L4 landed (2026-09-24, `1cdcd9fc`, card `backlog/done/20260924-l4-regel-en-expressielaag.md`):**
the rule and expression layer — the six named forms as `{node, name,
versions}` with `validity` on every version and `statuteAnnotations` on the
form (B19), `initialisation` + `presentation?` for the v0 kind (B20),
`Aggregation`/`BuiltinCall` for `Functie` (B21), one `Uniqueness` with
`sources` (B27), the typed `Literal.value` in the case schema's variants
(B22c; WF-114 new, WF-081 narrowed), `BoundDay`/`ContextValue` for
`Constante` (B23), WF-022's new arms (a target both attribute and
characteristic; the member kind against the tag); B24, B25 and B28 not
taken (deferred additively, the parity golden, the timeline anchor);
migration `spec/proposals/v1-migratie/m4.py`; the parity dump loses its two
`COMPLEX` allocations to WARNING diagnostics; negative 407 → 417 with 228 arm
fragments; no gate-5/6/12 outcome moved; all fourteen gates green on the
landing.
**L5 landed (2026-09-25, `71f5303d`, card `backlog/done/20260924-l5-bronrecord-en-hernoeming.md`):**
the source record and the renaming — `source {snippet, line?}` on every
node that carried a `bron` string, mandatory on `Unresolved` (B26), landed
first as its own commit range (M7) on which no output of ref, clingo,
Drools, the red set or the parity zone moved; then every remaining v0 node
name, key and list key renamed to its IR-SPEC name (B6; `host` for
`eigenaar`, B10; `subjectType`, `base`/`date`, and the six names IR-SPEC had
no v0 name for), every enumeration value under one spelling rule (English
lowerCamel; one translation table per language, checked against the naming
table), and the v0 `null` absent on the eighteen optional positions;
`Regelversie` keeps its v0 form (B25 waits for the parity golden); migration
`spec/proposals/v1-migratie/m5.py`; negative 417 → 418 with 260 arm
fragments; no gate-5/6/12 outcome moved; all fourteen gates green on the
landing. With L5 the wire `uchar-ir-v1` is complete and phase 2 of the wire
migration is closed; the one toolchain pin bump (phase 3) is now due, in the
new toolchain under uchar; the `toka/` zone stays until the public flip.

**Before the pin bump (2026-09-25, `54afea05`):** the six cards that touch spec
or goldens landed as one union ahead of phase 3, so the bump carries them —
WF-079 read through the type chain per concrete type
(`verifier-supertype-dubbele-schrijver`), the converse WF-037 arm
(`aantal-bronargument-welgevormd`), the numeric-operand arm of WF-091 with
five red witnesses moved to `corpus/negative` (`ref-klipvouw-nul`), the
ch. 7.8 sentence on characteristic input beside the form-3 proposal
(`kenmerkinvoer-en-invoer-boven-berekening`), a RegelSpraak source for each
of the five constructs no golden carried (`rgs-getuigen-bronloze-constructen`)
and the exception-on-exception dialect with `Afwijking van` and
`Uitzonderingen ongeordend op` (`dialect-uitzondering-op-uitzondering`); all
fourteen gates green on the landing. The bump itself waits for the
maintainer's signal.

**The last cards before the bump (2026-09-25, `07b1513c`):** six more landed as
one union the same day, all of them direct fixes of what the lens rounds and
the Results had left open — the dialect's opgave marker
(`dialect-opgave-boven-berekening`: `(mag opgegeven worden)` lowered to the
hardship pattern, no wire change), the emitter's last remnants
(`frontend-emitterrestanten`: typed `dayCount` and `concatenation`, the
dimension fold emitted, `connectingRole` on an object creation, the producer
refusals in the readers' English), WF-026 and WF-050 read through the type
chain (`verifier-allocatie-keten`), the source arm of the two counts with
the Drools loader refusing at load and both Drools branches carrying
consistencies under every quantifier (`aantal-bronarm-en-drools-lader`), the
ASP backend's money × rate encoding taking a rate of one to three decimals
so the WW twin is admitted (`clingo-tarief-decimalen`), and the parity zone
made well-formed on the v1 wire (`toka-welgevormd`; maintainer decision the
same day: the zone stays a while longer, in its own directory) — plus five
rounds of card-less fixes of the smaller open points (among them: datatype
names compared case-insensitively in the structural rules, WF-052 keyed on
the form so the versions of one object creation are one writer, WF-039
against the target's sign, WF-038 on the builtins' operands, the readers'
date and duration sentences in one library, gate 3 resolving every cited
pointer); all fourteen gates green on the landing. With this the spec, the
goldens and the frontend slice are what the one pin bump carries; the bump
waits for the maintainer's signal.

**The parity zone is well-formed on the v1 wire (2026-09-25, card
`toka-welgevormd`; maintainer decision the same day: the zone stays a while
longer, in its own directory):** gate 11 gained a well-formedness leg —
every replay slice verifier-clean (28 of 29; the 27 slices that lacked the
casus's start-point determination and its anchor member carry them now, so
the timeline with a variable start is anchored in every slice), with a
reason-carrying holdout list that fails when stale (toka-24, a clamp bound
written with a unit against a unitless base, WF-099) and the casus dump
outside that leg (over-determined by its nature: its scenarios write the
same targets each in their own way). The replays did not move.

**The pin bump became the toolchain move (2026-09-26, `359570c4`):** the
successor of `yhavinga/regelspraak-java` is `uchar-dev/regelspraak-toolchain`,
a private mirror with the full history, to become a DISTRIBUTION without
semantics of its own — CLI, service, docker/helm — that consumes this
repository as a git submodule (the submodule commit is the pin; `SPEC_REV` and
the conformance gate go) and builds `frontends/regelspraak` and
`backends/drools` in a Maven reactor. An independent investigation (report kept
beside the repository, `research/toolchain-verhuizing/`) measured why the plain
plan was not enough: the toolchain's service built through the two-argument
`ExecModelTranspiler.build`, which here delegates with EMPTY side tables, so
every defaults family would have fired nowhere — silently, HTTP 200. Hence the
order: first the mechanical package rename (this landing, card
`pakkethernoeming`: `io.github.yhavinga.regelspraak` → `dev.uchar.regelspraak`,
gates byte-identical), then one wave HERE before the distribution cuts its
copies — a public facade for the replay prelude with the short overloads
removed (`publieke-api-distributie`), the scenario layer as a frontend
extension lowered to case files (`scenariolaag-in-uchar`; maintainer decision:
it is semantics over the IR, so it lives here), the rule graph merged with the
toolchain's subtype key and gated (`graaf-samenvoeging-en-gate`), and the forms
the toolchain's in-memory route fired but the wire or the executors did not
carry: a mixed-fraction literal normalised to the wire's rational and a
navigated read that chooses its relation or refuses in a sentence
(`frontend-breuk-en-navigatiekeuze`), `evaluationYear` executed with its corpus
witness (`rekenjaar-als-waarde`), the empty/filled test under the whole-period
qualifier (`gehele-periode-leeg-gevuld`) and a fixed start point off the
calendar grid as a `Timeline` field (`vast-startpunt-tijdlijn`). Then the
distribution rebuilds on the submodule, the workbench re-points its
`toolchain` submodule, and the old repositories are archived — each step on
the maintainer's signal (`yhavinga/regelspraak-java` archived 2026-09-26
with a deprecation notice; the nluwv toolchain fork awaits consultation).
The UWV rule sets are rewritten by their authors on
the richer dialect rather than ported.

**The wave before the move landed (2026-09-26, `9d46e19d`):** the seven cards
above landed as one union with two new gates — 15, the rule graph against
the verifier's ch. 5 edges; 16, scenario documents regenerating their pinned
case files — after three lenses and one fix round of four fixers on disjoint
branches. What the lenses caught and the round fixed: the scenario lowering
wrote case files whose period bounds sat off the declaration's grid (now
refused with a sentence); an allocation over two relations still ended in an
emitter exception (now bound on the written role); the Drools runtime counted
whole months between clamped walls as zero where ref and clingo counted one
(a pre-existing gap the fixed grid made reachable); clingo's bucket calendar
spanned only the case input's endpoints, so a derived timeline on a fixed
grid passed the whole-period test vacuously; the graph called a member INPUT
when only a defaults group or a creation wrote it (the graph now reads every
form of the rule layer, and keys a navigated characteristic on the type the
hop reaches); clingo refused every non-whole rational literal (now read at
the smallest exact scale; `jutterswet-breukliteral` and `jutterswet-erfketen`
landed with it); `evaluationYear` without a date faults in one sentence in all
three (red case), and WF-098 crosses a context value's type annotation. Open,
recorded and not reopened (maintainer, 2026-09-26: finish, no scope creep,
on to the distribution): a rational without a finite decimal expansion (`1/3`)
is refused by name in clingo where ref computes exactly; the Drools replay
without an evaluation date reads `evaluationDate` as empty and plays where ref
and clingo fault; the graph draws no population, relation or channel nodes
(counted per class in gate 15) and keys sister subtypes where the verifier's
walk does not; a week grid's fixed start date is not canonical (two dates lay
the same grid); the general refusal of an undeclared read waits for the
parity zone to leave;
handwritten case files whose names or date-times do not follow the lowering's
spelling cannot be regenerated from a scenario; the loader cites a different
first finding than the verifier on doubly ill-formed documents. Norm
decisions taken by default and to be confirmed: the negation inside a
whole-period leeg/gevuld test is per day and "filled the whole year" is
vacuously true on a timeline without any value; an explicitly empty segment
is empty everywhere; "the whole year" over a member on a fixed grid reads the
calendar year; a name that is both a role and a type binds as the role where
the type pair does not decide; characteristic periods in a scenario stay
day-precise.

**The distribution's first card landed (2026-09-26, `uchar-dev/regelspraak-toolchain`
`316d448`):** the successor repository now builds `frontends/regelspraak`
and `backends/drools` from this repository as a submodule pinned at
`d67f5da1`, in one Maven reactor, its own copies of parser, loader,
transpiler and runtime deleted and its own code under
`dev.uchar.regelspraak.toolchain`; its service, engine facade and scenario
runner build through `DroolsBackend.build` and the frontend's scenario
layer. Three questions came back to this repository, recorded here and not
opened as cards (same maintainer decision): the scenario layer refuses a
scope that names a fact creation, and a scope in a document that carries a
fixed-start declaration — three of the twenty-nine scenarios of the
distribution's parity zone, which its manifest lists as reason-carrying
holdouts; the facade's refusal is a sentence rather than a typed value, so
the distribution reads the construct's name out of the sentence for its
marker verdict; and the tests the distribution deleted (the runtime and
kernel tests free of parity material, the frontend tests without a
counterpart here, the differential integration test; history at its commit
`b002492`) await the triage cards.

**The distribution's second and third cards landed (2026-09-26, `9821576`
and `ingangen-en-deploy` the same evening):** its service, scenario runner
and playground now answer as this repository's replay does, proven by three
gates of its own — the three dialect twins over HTTP, a replay of the
gate-12 played set through the service (188 of 215 goldens; the 27 that
carry a timeline are excluded by name) and its parity manifest — plus
stable entry points (`bin/regelspraak`), the container, and one local gate
sequence (`make gates`) run before every landing there. What came back
here, recorded and not opened as cards: the case-input step of
`CaseRunner` (beans, roles, the mirror of a reciprocal relation, timelines)
is not public API, so the distribution mirrors the non-timeline part and
cannot express a timeline in a request; a scope on the wire and the
scope-to-forms reading of `ScenarioResolver` are not public either, and
are mirrored too; and one norm question, answered by default: the scenario
layer refuses every scope in a document that carries a fixed-start
declaration, where the service keeps the declaration and builds the
scope's forms — the default reading is the service's (a scope selects
rule-layer forms and carries the document-level declarations), to be taken
up when the scenario layer next moves.

**The workbench moved (2026-09-26, the same evening):** the UWV workbench
now carries the distribution as its `toolchain` submodule and calls it
only through `bin/regelspraak`; the old repository `yhavinga/regelspraak-java`
is archived. Measured on the UWV rule sets, which their authors rewrite
anyway: one set resolves and its service smoke passes, but 15 of its 36
scenarios fail, all for one reason a read-only diagnosis pinned — the
scope filter of the scenario layer (above, and `DIVERGENCES.md`), where
the old runner fired the whole document; no rule computes differently
(with every scope completed, 36 of 36 pass in both). A second set fails
on the unit-typing decision (17b/17c), a third on a nested collection
step in a fold source the frontend does not carry (dialect §13.4.17).
Recorded, not opened: the scenario route lacks the scope-completeness
check the distribution's service performs (a scope that misses a rule
supplying a read value fails without a word where the service refuses to
start), and the nested collection step in a fold source. A read-only comparison with the nluwv fork's eleven
own commits (2026-09-27, `research/nluwv-fork-dekking-2026-09-27`) found
its behaviour carried here or deliberately different on every point but
one: a nested aggregation whose step uses a role plural the source does
not declare plays in the fork and is a marker here, and the frontend's
warning for it named a cause WF-102 had since removed. Since the card-less fix
`fix/aggregatie-meervoud` (2026-09-27, landed with `scenario-parametersets`)
that warning names the step that does not bind and why, with the way out
`(mv: …)`, and an unknown to-one step beside a direct relation no longer
drops out silently; a path step still binds no undeclared plural (IR-SPEC
ch. 2.6), while the one-step range accepts one through the type pair —
recorded, not opened. Also recorded from the distribution's fix of the same
day: the case input of `CaseRunner` refuses a subtype instance on a to-one
role as not carried, because it compares the bean field's Java type with
the instance's type instead of the role's declared type (a to-many role
takes it); the distribution mirrors that.

**Parameter sets in the scenario document (2026-09-27, card
`scenario-parametersets`):** the scenario route has its parameter sets back,
spec-first: a `Parameterset` block with jaargangen, chosen per scenario with
`met parameterset` and resolved on the peildatum
(`frontends/regelspraak/grammar/DIVERGENCES.md`, Scenariotoetsen; golden
`jutterswet-jaarvaststelling`, gate 16 floors 7 goldens and 31 cases; gate 12
plays 216 and 186). The open point "a scenario has no surface for an absent
parameter" is closed without a new form: the one pinned case leaves out a
list parameter, a datatype RegelSpraak does not declare, and a scalar
parameter that is read and left out faults in ref and clingo
(RUNTIME-SEMANTICS ch. 5.7; red case `aanvoergeld-parameter-unbound`), so
the parameter gate refuses what the executors would. Open for the
distribution: its service reads the set from the same document instead of
its JSON file (a card there). Recorded: the frontend's phrase tokens force no
word boundary (`met parametersets` reads as the token plus `s`, as `de
parameters` already did), and the head order is fixed (`met parameterset`
after `met peildatum`).

**Named vocabularies landed (2026-09-27, card `vocabulaires-benamingen`):** a document can declare named vocabularies and give every designatable declaration — object type, attribute, characteristic, fact type, role, enumeration value, parameter, named form, rule group — a name in each, as a side table without semantics (IR-SPEC ch. 2.12; WF-116 to WF-120 with fifteen negatiefs, one per arm), written by the RegelSpraak dialect as `Vocabulaire <naam> (standaard) (taal: <code>)` and `(<vocabulaire>: <benaming>)` at the end of a name group, read structurally by every reader and carried by the Drools loader into `RegelSpraakModel` with a public reading entry; golden `jutterswet-vocabulaires`, gate 12 plays 217 and 187, and gate 13's new vocabulary leg holds ref, clingo and both Drools branches indifferent to the table stripped, exchanged and rotated. Three maintainer decisions of the same day shaped the surface: a vocabulary has no kind (every designation is text), the head's properties stand in their own parentheses, and the annotations close the name group. Open, recorded and not opened as cards: the service and the explanation per vocabulary (a card in the distribution), the lint against the concept map (the workbench), a status per designation and several languages, a strip output to bare RegelSpraak; a default vocabulary cannot coexist with declarations the frontend writes itself (decision-table rows, the opgave marker's members, a start point's anchor), which the completeness rule refuses; and the redundancy warning of WF-120 is accounted for by literal containment, with WF-113 (b)'s weakness. Recorded the same day from the distribution's card `dienst-vocabulaires` (the service reads every designation and answers in a chosen vocabulary): `RegelGraaf` gives no element key per node, so a consumer that wants to name a node in a vocabulary has to find the declaration through the node's label and cannot decide a node whose attribute and characteristic share a name; a public key per node, or a naming function on the Mermaid rendering, is the way out. And the reachability of a role from an object type (IR-SPEC ch. 2.6) lives in the frontend only privately (`feittypeRelateert`, `rolHeeftObjecttype`), so the distribution states the same rule a second time over the public model.

**Names in declaration heads landed (2026-09-28, card `namen-in-declaratiekoppen`):** the
RegelSpraak frontend reads an object-type name with the prepositions rule text already reads
(`Recht op premie`) in every position of the production, a plural up to its closing
parenthesis, and a fact-type name with its verbs (`jutter heeft premierecht`); a member or
role that begins on the line where the previous declaration ended is an ERROR on that line,
a role's filler that is no object type is reported before the emission, and a declared name
rule text cannot read back gets a WARNING naming the word
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Namen in declaratiekoppen"; inertness
measured over 643 sources, no exit status and no model changed; golden
`jutterswet-premierecht`, gate 12 plays 218 and 188).
Open: the filler check is an ERROR only where the document shows the misreading and a
WARNING where the type is declared nowhere, because the parity zone's own model test
resolves such a slice without an error — an ERROR everywhere needs that slice completed, the
maintainer's call; an undeclared dimension still passes the frontend and only the verifier
refuses it (WF-004); `scripts/admit.sh` and `corpus/README.md` do not know the
`scenario.rgs` of a golden.

**Truth maintenance replaces the destructive Drools lowering (2026-10-08,
card `drools-logische-afleiding`):** a trial branch of `backends/drools`
(`DroolsBackend.Branch.TRUTH_MAINTENANCE`, flag `--tms`) derives with
`insertLogical` in an EQUALITY KieBase instead of writing on the beans: a
derived characteristic or attribute is a logical fact justified by its rule,
every read a derived value depends on is a pattern in the rule's left-hand
side, prioritized defaults lower to logical control facts, and of the six
measures the exec-model branch needs against destructive writes only the fire
cap remains. On its eight-golden trial set it fires 251 times against 218 and
takes 1.27 times as long, under a millisecond per case; over the corpus it
plays 66 goldens without a FAIL and refuses the rest per form, aggregations
first (65). As an independent reading it found two existing faults, now
cards: `deling-door-leeg-onder-initialisatie` (the exec-model branch and
clingo deposit a division by empty under an initialisation whose target the
input fills) and `fout-op-tussenstand` (ref and both Drools branches fault or
answer depending on the rule order when a fault arises at an intermediate
state, against the confluence of RUNTIME-SEMANTICS ch. 5.2; a norm question
first). Maintainer decision the same day: build the branch out as the
REPLACEMENT of the exec-model branch, not as a third branch, and move the
DRL-text branch to truth maintenance too, so that the backend ends with two
branches of one design; gate 12 then compares two renderings of that design,
and the independent check stays with ref and clingo. The route choice
recorded in `drl-defaults-lowering` lapses with it. The follow-up cards are
build cards, not feasibility trials, in this order: aggregations, object and
fact creation, and faults as logical facts; the expression and condition
forms with the calculation date; allocation and timelines; the switch-over,
where the branch becomes gate 12's exec-model leg and the distribution's and
the destructive lowering leaves (the distribution follows with its own card
and a pin bump); then the DRL-text branch, with which `Stratificatie` leaves.
Recorded, not opened: this branch withdraws a deposit made at an intermediate
state where ref keeps it (RUNTIME-SEMANTICS ch. 6.1, "recorded residuals"),
the question `fout-op-tussenstand` asks for faults; no golden reaches it.

**Creation in the truth-maintenance lowering is stated, not logical
(2026-10-08, card `tms-aggregaties-creatie-en-fouten`):** the first build
card carries faults as logical facts that count only at the fixpoint,
aggregations as an `accumulate` over logical contributions, and object and
fact creation; the branch plays 120 goldens (from 66) without a FAIL. A
logically inserted object can keep itself alive through its own output after
the intermediate state that made it is retracted, and its initialisations
would follow later changes where ch. 5.5 evaluates them at the mint. Maintainer
decision during the card: a creation fires only at a fixpoint of the rule
layer, per stratum and in generations as in the reference interpreter (the
strata of RUNTIME-SEMANTICS ch. 6.1 and 6.3, the budget of 100 passes per
stratum), and the created object, its initialisations and its links are stated
facts keyed per (rule, subject) that do not lapse; the rule layer stays under
truth maintenance. Second decision: the switch-over card carries a synthetic
scale test with a floor (growth exponent at most 1.2, and firings under half
the cap at 10 000 objects; otherwise the joins are indexed first), because the
standing facts, the unindexed joins and a fixed fire cap of 100 000 meet large
cases there.

**The range of a date is normative (2026-10-09, cards
`drools-datumstap-buiten-bereik` and `datumrekening-slaat-stil-om`):** a
datum-tijd lies in [−2⁶², 2⁶² − 1] ms, checked in the reference interpreter's
order before the step is taken (RUNTIME-SEMANTICS ch. 3.8, landed with the
first card; the Drools runtime holds the same bound since that card, and the
ASP backend refuses a count beyond its 32-bit integers at translation).
Maintainer decision the same day: a day-precision date has the same range,
read at its midnight (epoch days −53 375 995 583 to 53 375 995 583); its
sentence in ch. 3.8 and the executors follow with the second card, which also
repairs the silent wrap-arounds it records.

**Faults and deposits count at the fixpoint (2026-10-09, card
`fout-op-tussenstand`, to be built):** maintainer decision on Fable advice: an
evaluation fault and a §9.5 deposit are outcomes of one form's evaluation on
one subject; re-evaluation under freshness replaces them, a form that does not
decide is not evaluated, and the session reports what stands at the fixpoint
of the form's stratum (ch. 5.2: an intermediate state is not observable). The
card's documents answer bergloon 30, aandeel 3 and toeslag 7 in every rule
order. The spec text, the reference interpreter and the ASP backend follow
with the card; the truth-maintenance branch already reads so, and its two
refusals that wait on this decision (a division in a default, a division in
an initialisation beside another writer) can go. The decision does not depend
on whether numeric-zero division becomes a channel fact (decision (7)).

**Creation reads by generations, and its budget counts depth (2026-10-09,
cards `creatie-initialisatie-leest-groeiende-waarde` and
`creatiebudget-ref-en-clingo`):** within a stratum evaluation proceeds in
generations. Every creation of a generation, object creation and fact creation
alike, reads the same settled world of the generation before, its condition as
well as its member initialisations; afterwards the forms that make nothing
settle stratum by stratum, the consistency and uniqueness forms of the lower
strata fire again, and those of the stratum itself fire once its generations
are done (RUNTIME-SEMANTICS ch. 5.5, Generations). The reference interpreter
evaluates so, the truth-maintenance branch already did, and the ASP backend,
which has no moment of making, refuses by name a member initialisation that
reads what a later generation of its stratum can still change. The creation
budget counts the creation depth per stratum: a mint at depth 101 is a fault,
"creation budget exhausted", in the reference interpreter, the ASP backend and
the truth-maintenance branch; fact creations and the forms that make nothing
carry none, and the Drools fire cap and the ASP nesting bound are named
resource bounds of an executor, not norm. This replaces the budget of 100
passes per stratum in the entry on creation in the truth-maintenance lowering
above. Both decisions were taken provisionally on Fable advice while the
maintainer was offline (the budget first as generations per stratum, revised
to depth when a lens found two forms the ASP backend cannot count in
generations) and confirmed by the maintainer the same day. The generation
reading also fixes the reference interpreter's answer for a creation's
condition, so the residuals "conditions that hold on absence" and "a value
that grows" (ch. 6.1) are an ASP shortfall now, not an open norm question
(card `clingo-creatievoorwaarde-op-de-kring`); whether such a condition
becomes a negative edge stays open.

**The exec-model branch derives with truth maintenance, and the destructive
lowering is gone (2026-10-10, card `tms-omschakeling`):**
`DroolsBackend.Branch.EXEC_MODEL` builds through the truth-maintenance lowering
(`transpiler/tms`), and `Branch` has two values, `EXEC_MODEL` and `DRL_TEXT`;
the destructive lowering (`transpiler/model/ExecModelTranspiler`, 9 950 lines),
`Branch.TRUTH_MAINTENANCE` and the flag `--tms` left without an alias, and
`Stapels` is the one composition of RUNTIME-SEMANTICS ch. 7.10. Over every
golden with cases the branch plays the 242 the destructive lowering played,
with the same pinned outcomes, and six it refused, 248 of 294, without a FAIL;
gate 12 pins 248 (from 242) and the DRL-text branch's 196 unchanged. A consumer
fires a session through four public calls of `DroolsBackend`: `insert` states
the beans and what the branch reads beside them, `vuur` fires to the fixpoint
under a cap of max(10 000, 4 × R × N0) recomputed at every creation phase,
`lees` reads the fixpoint, and `setDerivedValues` writes it on the beans
once and closes the session. Until then the beans hold the input alone, and an
evaluation fault stands as a fact of the fixpoint that `lees` gives as data and
the write-back throws. Between two fires only an update of an attribute or a
characteristic is admitted; a watch on the session refuses anything else by
name, and it reads Drools' internal agenda (`org.drools.core`), because a
cleared agenda raises no event. A value set without an update is seen neither
by Drools nor by the entry, so a consumer updates after every change (the
README's list of what a consumer can forget). The scale test of the
maintainer's decision of 2026-10-08 meets its floor once the joins of an object
with an object are indexed: on the landed code growth exponent 1.05 and firings
at 0.016 of the cap at 10 000 Wachters, where the destructive lowering grew
with exponent 2.34 on the same model. Maintainer decisions during the card
(2026-10-10): the forms the destructive lowering carried and the new branch
refuses by name, none with a golden, are an accepted loss of coverage, listed
in the card's `Result:` and in the distribution's card, with one follow-up card
that adds a golden for an object creation without a connectingRole binding and
carries it (`objectcreatie-zonder-connectingrole-binding`); and gate 8's Drools
leg, which drives the exec-model branch, was re-pinned within the card on the
branch's own sentences (the `drools` fragments of twelve red cases, the leg's
exclusion list and head, and the eight notes the switch-over made untrue), the
ref and clingo pins untouched. Next: the distribution moves its pin with a card
of its own, in which its own `fireAllRules(100_000)` gives way to the consumer
entry in the service, the engine and the scenario explanation; then
`drl-teksttak-op-truth-maintenance` lowers the DRL-text branch to the same
design, and `Stratificatie` leaves with it.

**The DRL-text branch derives with truth maintenance too: two branches of one
design (2026-10-14, card `drl-teksttak-op-truth-maintenance`):** one analysis
(`transpiler/tms/TmsTranspiler`) makes a plan per rule
(`transpiler/tms/plan/`, together a `Modelplan`: the reads, the tests and
values as a tree of calls, the consequence as a kind), and two renderings
build a rule base from it, `PatternDslRendering` for `Branch.EXEC_MODEL` and
`DrlRendering` for `Branch.DRL_TEXT`. A rendering decides nothing: what the
analysis refuses, both branches refuse in one sentence, and the DRL rendering
refuses only what DRL text cannot express. In the DRL text every RegelSpraak
test and calculation is a Java function with its RegelSpraak sentence above
it, which the rule calls; a fact's key, a null test, a control fact's rank and
an identity join stay DRL constraints (maintainer decisions 2026-10-12 and
2026-10-13: one form and one semantics, no MVEL operators). Both branches play
255 goldens with the same status line per golden, and per case the same input,
reads, facts and fixpoint; at the claim the DRL-text branch played 197 of the
250, and it lost no player. `transpiler/DrlTranspiler` (5 833 lines),
`Stratificatie`, `Schrijfconflict`, the fixed fire cap and 26 helpers of
`RegelSpraakRuntime` without a caller left, and the DRL text of the corpus
(261 texts) carries no `no-loop`, `salience`, `@watch`, `modify` or `update`.
Maintainer decisions on the norm questions of the third and fourth Fable
advice (2026-10-13 and 2026-10-14): gate 12 is strict, so the DRL sweep must
give the exec sweep's status line for every golden, with one list of
non-players and one pin `EXPECTED_PLAYED` (255), and `EXPECTED_DRL_PLAYED` and
`DRL_CHANNEL` lapse; the card's comparison harness enters the suite as
`BranchDumpsCorpusWitnessTest` (both branches per case of every played golden,
695 cases, and per red case, 50), and gate 12 fails when it did not run; the
card's own documents with expectations became goldens
(`jutterswet-verdeling-dubbelbezet`, `jutterswet-uniciteit-drie-bronnen`,
`jutterswet-uitsluiting-gedateerd`, `jutterswet-aantalfilter-gehele-jaar` and
`jutterswet-aantalfilter-gehele-jaar-creatieguard`, the last two without a
`source.rgs` because the frontend does not convert their filter), with the red
case `jutterswet-uitsluiting-zonder-rekendatum`; and a non-terminating
fraction at rest got a behaviour witness before the landing
(`RationalAtRestWitnessTest`: ref writes 1/3, clingo refuses by name, both Drools
branches write 0.3333333333333333, a recorded limit of the backend). On
2026-10-14 the maintainer added `--fast` to gate 12, for a builder who changes
a pin or a list: a fresh jar without the suite, the replay under every pin and
list, no DRL sweep, and a last line saying it is no landing run; 130–160 s
against about 650 s for the full gate, which took about 300 s at the claim.
Gate 13's DRL leg must reproduce the exec-model branch's untouched line and
play as many goldens; gate 7 gains the survivor case 2 of
`jutterswet-uniciteit-drie-bronnen`, a consistency that holds on the input.
Landing run on the card's tip `722d8fb6`, on a copy as its own repository
(15:03–15:23): 16 of 16 gates green; gate 12 in 661 s, 255 played on both
branches with every status line equal, JUnit 1574 tests against the floor
1573; gate 13 in 88 s. The card's harness was green on the same tip
(15:03–15:10): the exec-model branch equal to the baseline of the claim, the
plan, model and DRL dumps unchanged, 680 cases equal on both branches, 47
known trace differences (the order and number of firings at an accumulate,
with an equal fixpoint) and 2 006 suite sessions equal. The distribution moved
its pin the same day (regelspraak-toolchain `dba5864`, pin `f752a5c9`, with
the `jackson-databind` 2.21.7 of card `jackson-databind-kwetsbaarheden`): the
CLI and the playground read `build.drl()` and catch the
`IllegalStateException` of a DRL text that does not compile, and
`Namen.getter`/`setter` follow the bean generator. The branch guard in
`CaseSessie` stays, as `build.branch() != Branch.EXEC_MODEL`: the scenario
explanation compares its firings with the trace of `CaseRunner.report`, which
plays the exec-model branch, and the DRL-text branch reaches the same fixpoint
with the 47 known differences in that trace. Cards in `backlog/new/`:
`beanlidnamen-zonder-java-identifier`, `caselezer-zoals-ref` (an expectation
on an undeclared spelling passes in the Drools case reader, where ref refuses
it), `frontend-heelperiode-in-aantalfilter`, `weergaven-als-getuige` (the
plan, model and DRL dumps of the corpus held in the repository; the card's
baselines lived outside it), `clingo-default-literaalschaal` (clingo refuses a
default whose literal has a smaller scale than its target, where the verifier
and ref accept and play the document) and
`uitvoerdernotities-na-truth-maintenance` (IR-SPEC ch. 5.5 and javadoc in the
backend's tests still describe the destructive lowerings as current).

**A fold in a filter has no reading of its own (2026-10-22, cards
`fold-in-element-condition`, `rule-variable-in-subselection`,
`subselection-per-chain`):** since these cards the frontend writes a fold over
a relation of the rule subject, read in the filter or the test of another
fold, as a `sourcePath` from the element back to the rule subject (route (i)
of the Fable advice of 2026-10-22, a provisional decision). Such a fold is
evaluated per member and has no reading of its own (ch. 5.12): an explanation
shows which members pass, not the value of the inner fold and its
contributors. A refinement — reading a filter node that reads nothing of the
element once per subject — is a spec question of its own; until then the
limit holds. Route (ii), a `Let` on the rule, would give that reading, but
waits on open point 6 of the Drools truth-maintenance lowering and on the
clingo binder leak (`20261009-clingo-let-binder-tussen-onderwerpen`), and
changes the meaning on an empty value (IR-SPEC ch. 4.10 (c), ch. 10.6). Where
the way back from the element is not to-one at every step, the frontend
refuses the read (RGS-352) instead of writing a marker.

**A cycle over a switch relation is ordered by depth (2026-10-22, card
`cycle-across-instances`):** since this card a value of an object may read the
same value of its predecessor over a switch relation (IR-SPEC ch. 5.3; a
provisional decision after the Fable advice of 2026-10-22, route (b)); WF-045,
WF-046 and WF-051 walk the graph without the shifted edges, ref evaluates such
a part per depth in the chain and per depth by level (RUNTIME-SEMANTICS ch.
6.2), and a cycle in the links is EF-033 at case input. The part is narrow on
purpose. Open, after the second Fable advice of the same day: the household
widening — a fold or a quantifier over the subject's own to-many satellites,
and the step to a satellite and back, counted as local or shifted — is the
next card (`instance-cycle-household-reads`), before the frontend admits the
form (`check-admits-instance-cycle`); a read over a hub (a relation whose role
on the side of the cycle's type is to-many) stays refused statically, because
measured on a miniature it gives two stable models, and no ground-graph test
per case is planned. The v2 rule set of the workbench therefore keeps its
bridge K12 until it rewrites the exception of art. 21 lid 2 onder a WW without
a derived end date of the new right — a norm question for the workbench
(the new right as an input characteristic in the concurrence, or as the
successor over `eerdere recht`).

## 10. Rough effort

Phase 0 in weeks; Phases 1+2 together a few months of serious work; Phases 3
and 4 each in the same order of magnitude; Phase 5 comparable once 1–4 hold —
all depending on whether this is evenings or work time. *(As of
2026-08-21 Phases 0–4 are Met; the estimate stands only for Phase 5.)* The gate set grows
monotonically: nothing that has entered it may regress.
