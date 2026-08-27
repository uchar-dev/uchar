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
model-theoretic anchor). Each step gets a gatekeeper in the CI gate set.

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
no ©-Belastingdienst material — no TOKA casus, no RegelSpraak specification
excerpts — enters the uchar repos. A test case that cannot be published is
not a test case for a public spec.

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
    └── clingo/  (Phase 3)
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
  stay outside. The planned upgrade, once the ladder-producing frontend
  evolution starts, is inverting the dependency so the toolchain
  consumes this frontend — one source of frontend truth.)*
- **The held-out corpus split** — a separate private repository (e.g.
  `uchar-corpus-heldout`), pulled only by the CI acceptance job (§8).
- **The org profile** (`.github` repo) — the public landing page until the
  spec is worth publishing; then `uchar.dev` serves `spec/` via GitHub Pages,
  so the domain serves the product itself.

## 6. Phases

Phases overlap deliberately where noted; each has an exit criterion that
lands in the CI gate set.

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
one deliberately malformed failing case in `corpus/negatief/` — the
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
(statute text is copyright-free under art. 11 Auteurswet; candidate:
zorgtoeslag — compact, computation-heavy, with genuine annual parameter
changes for the vintage machinery). Whether `regelspraak-java`'s *internal*
gates also migrate off TOKA is that repo's own decision, out of scope here;
this repo is TOKA-free from its first commit.

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
verifier check, a passing case, and a failing negatief case; the corpus is
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
`ref/` and `clingo`. No gate runs Drools against uchar cases; that
direct leg is a recorded open flank, not claimed — it needs the
toolchain to *execute* uchar case files, the same machinery a
ladder-producing frontend needs (Phase 5), and the divergence class
it could catch is meanwhile fenced by the corpus-scoped agreement
plus the spec-first delta regime.

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
pinned revision). In ASP a fact type is simply the predicate `rol/3`
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
fragment:** the ladder rides the wire in both target kinds with WF-050/051
(`defaultLadders`; anchors through both executors, incl. the ch. 7.8
standing norm and the input rung — `RUNTIME-SEMANTICS.md` ch. 7,
`spec/proposals/PHASE4-DEFAULTS.md`); the Drools salience is derived from
the declared kenmerk graph (the WF-046 strata principle as a lowering,
`CONSTRUCTS.md` row 6, toolchain pin `0b7f284`) instead of the syntactic
trick this section names; and ref ≡ clingo holds over the defaults corpus
(gates 5–6), with the Drools side agreeing over the shared v0 forms through
its own differential. Deliberately open, recorded: full ladder service in
the Drools backend waits for a ladder-producing frontend (Phase 5 — building
it against hand-authored IR tests no production path); temporal standing
targets stay fenced (scalar-first, RS ch. 7.8); and one latent, non-defaults
loader divergence is carded (`backlog/new/20260820-input-precision-fence.md`:
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
navigation); general folds and function values are planned kernel
additions, designed at this phase — full-Catala expressibility is the goal,
not a subset stance (`CONSTRUCTS.md` rows 10/36); and on
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
`defaultLadder`) and diffs the two IR dumps, both carried through the full
gate set: every difference is administrative or a tracked v0↔v1 delta
(`CONSTRUCTS.md` rows 8/2/7), no category-(iii) design flaw, and the resulting
spec change is one besluit-free documentary note (no normative change, no
numbered maintainer question). With both criteria met the hourglass is real —
two frontends, two-plus backends, 2×2 through one waist instead of four
translators.
**Proposal adopted (2026-08-21):** `spec/proposals/PHASE5-CATALA.md`
carries the design; its six decisions are all taken (attachment at the
scopelang→dcalc boundary; context-override polarity as a top rung —
Reading B as rung order, the input rung reserved for the case dossier;
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
gatekeeper in CI.

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
therefore policy, *mechanized*, not optimism. A held-out corpus split exists
**in a separate private repository, pulled only by the CI acceptance job** —
held-out material inside the working repo would be theater for a repo-reading
agent — and acceptance is measured on it. `IR-SPEC.md`,
`RUNTIME-SEMANTICS.md`, the schema, the verifier, the anchor corpus, and the
CI configuration are protected paths: sessions may propose diffs to them,
never merge them — enforced by CODEOWNERS plus branch protection and a CI
check on protected paths, not by convention. The oracle and no-op checks run
on every change, so a weakened harness fails loudly rather than silently. And
because the agent is stateless, the repository is the state: `CLAUDE.md`
carries the invariants and the protected-path list; every session ends by
updating the issue it worked and — only when scope changed — this plan;
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
behaviour.

New in v4. (8) **Decided:** the published corpus is rights-free by
construction — the Jutterswet (plus its quantitative extensions) replaces
TOKA as the golden corpus, and one statute-based case (art. 11 Auteurswet;
candidate: zorgtoeslag) joins for realism; no ©-Belastingdienst material
enters the uchar repos. (9) **Decided
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
stands with it.

## 10. Rough effort

Phase 0 in weeks; Phases 1+2 together a few months of serious work; Phases 3
and 4 each in the same order of magnitude; Phase 5 comparable once 1–4 hold —
all depending on whether this is evenings or work time. *(As of
2026-08-21 Phases 0–4 are Met; the estimate stands only for Phase 5.)* The gate set grows
monotonically: nothing that has entered it may regress.
