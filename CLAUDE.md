# CLAUDE.md — uchar

## Mission

Build the **uchar IR**: a normative, engine-neutral intermediate
representation for machine-executable law, plus its corpus, reference
interpreter, and engine backends. **The specification is the product;
implementations are its proof.** The full programme, phases, and rationale:
`spec/PLAN.md` — read it before any non-trivial task. The founding review:
`spec/IR-REVIEW.md`. How sessions operate day to day — pace, parallelism,
backlog, worktrees, the self-containedness gate: `WAY-OF-WORKING.md`.

## The repository is the state

Sessions are stateless; what accumulates is artifacts — the spec, the corpus,
the verifier, the gate set, the plan. Therefore:

- Every session works one defined task with acceptance criteria stated up
  front, and ends by updating that task and — only if scope changed —
  `spec/PLAN.md`.
- Update THIS file only when an invariant or protected path changes. It is
  the constitution's index, not a logbook.

## Protected paths — propose, never merge

Sessions may propose diffs to these; only the maintainer merges them:

- `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`,
  `spec/verifier/` (once it exists)
- `corpus/golden/anchor/` (hand-written cases with absolute expected values)
- `.github/` (CI configuration, CODEOWNERS)
- `scripts/` (gate scripts), `WAY-OF-WORKING.md`, and this file

Mechanism: `.github/CODEOWNERS` plus branch protection (note: on the free org
plan branch protection requires a public repo). A change to a protected path
without maintainer review must not land.

## Spec discipline

- Every normative MUST carries an ID (`WF-###`) with a verifier check, at
  least one passing golden case, and one failing negatief case.
- Every semantics clause carries a worked example with concrete expected
  output; those examples become the reference interpreter's tests verbatim.
- **A spec sentence without a machine-checkable artifact is not done** — the
  spec-level "a green subset is not green".
- Mirror, then diverge: v0 is the faithful dump of `regelspraak-java`'s IR;
  every v1 deviation is a tracked delta in `spec/CONSTRUCTS.md`, never a
  silent rewrite.

## Corpus rules

- **Rights-free by construction.** Own-authored casus material (the
  Jutterswet) and statute text (copyright-free under art. 11 Auteurswet)
  only. No ©-Belastingdienst material enters this repo — no TOKA casus, no
  RegelSpraak specification excerpts — **with exactly one exception (below).**
  Programs *written in* RegelSpraak are ours; the language specification text
  is not.
- **The `toka/` exception (maintainer decision 2026-08-27, temporary).**
  ©-Belastingdienst TOKA material is admitted for feature-parity testing of
  uchar's own frontend against the original toolchain, isolated in one
  top-level `toka/` zone and **nowhere else**. Consequences, accepted and
  recorded: (1) the material is in git **history** from the card that created
  the zone, so the repository **must stay private while the zone exists**;
  (2) any public flip (PLAN §9 decision 10) requires **both** removing the
  zone **and** a history rewrite (filter-repo or a fresh public copy) —
  deletion alone does not erase history; (3) every other zone stays TOKA-free
  and gate 1 keeps tripping on it (`scripts/check-self-contained.sh`). The
  zone is exempt from the self-containedness prose rules but carries no
  machine-local paths.
- Synthesized cases are admitted only when the full differential square
  agrees (Drools ≡ reference interpreter ≡ Clingo, to the extent those
  exist); disagreement routes to adversarial triage, never to admission.
- The anchor subset (`corpus/golden/anchor/`) is hand-written with absolute
  expected values and permanently pinned — parity between backends is never
  accepted as truth by itself.
- The held-out acceptance split lives in a separate **private** repository,
  pulled only by CI. Never vendor it here; never read it in a working
  session.

## Gate set

Grows monotonically; nothing that entered may regress; a green subset is not
green. Current gates:

1. `scripts/check-self-contained.sh` — artifact zones (`corpus/golden`,
   `corpus/negatief`, `corpus/rood`, `spec/schema`, `spec/verifier`,
   `ref`, `backends`, `frontends` — since the Phase-5a skeleton — and,
   since the Phase-1 exit, `spec/IR-SPEC.md`
   and `spec/RUNTIME-SEMANTICS.md`)
   carry no machine-local paths or private-repo references, and corpus data
   is free of TOKA material — with the single, temporary exception of the
   `toka/` parity zone (see Corpus rules), which the tripwire deliberately
   skips while it keeps firing on every other zone; `toka/` itself is checked
   only for machine-local paths. Working prose (plan, backlog, reviews) may
   reference the private source repos during development — see
   `WAY-OF-WORKING.md` §5–6.
2. `scripts/check-ir-schema.sh` — the schema file is metaschema-valid;
   every `corpus/golden/**/ir.json` and `corpus/rood/**/ir.json`
   validates against
   `spec/schema/regelspraak-ir-v0.schema.json` and round-trips
   byte-identically; an unpopulated corpus is reported explicitly, never
   silently green.
3. `scripts/check-verifier.sh` — the OCaml verifier (`spec/verifier/`)
   builds; every `corpus/golden/**/ir.json` and `corpus/rood/**/ir.json`
   is verifier-clean; every
   `corpus/negatief/<wf###-slug>/ir.json` is rejected, citing the WF
   rule its directory name declares; an empty negatief corpus is a
   failure, never silently green.
4. `scripts/check-case-schema.sh` — the case schema
   (`spec/schema/uchar-case-v0.schema.json`) is metaschema-valid; every
   `corpus/golden/**/case*.json` and `corpus/rood/**/case*.json`
   validates against it and sits beside an `ir.json`; zone discipline
   holds (golden carries `expected`, rood carries `expectedFault`); an
   empty case set is reported explicitly, never silently green.
5. `scripts/check-oracle.sh` — the reference interpreter (`ref/`) builds
   and replays every `corpus/golden/**/case*.json` with exact expectation
   match (the oracle guard of PLAN §8); goldens without a case file are
   listed explicitly; an empty case set is a failure, never silently
   green.
6. `scripts/check-clingo.sh` — the differential: `backends/clingo`
   translates every corpus case to ASP-Core-2 and clingo's stable model
   must match the same absolute expectations gate 5 pins for `ref/`
   (ref ≡ Clingo over the corpus, anchored to hand-written values); an
   empty case set is a failure, never silently green.
7. `scripts/check-noop.sh` — the no-op guard (PLAN §8: an identity
   transpiler must fail): every golden case run through the reference
   runner's `--noop` mode (expectations against the raw input, no
   evaluation) must fail; the cases that legitimately survive — they pin
   defeasible standing — live in the script's explicit, reason-carrying
   survivor list, which fails when stale; an empty case set is a
   failure.
8. `scripts/check-rood.sh` — the red corpus: every
   `corpus/rood/**/case*.json` declares `expectedFault {ref, clingo}`
   and MUST fail observably in **both** executors with the pinned
   fragment in each report (the unsolved-state guard among them:
   `Onopgelost` reached in evaluation faults with WF-044, never
   vanishes); an empty red set is a failure, never silently green.

9. `scripts/check-catala.sh` — the Catala frontend builds and its
   full test suite passes (E2E through verifier, ref and clingo plus
   the refusal fixtures), and every corpus golden carrying a
   `bron.catala_en` regenerates through the plugin byte-identically
   after canonicalisation (gate 2's serialiser shape, executed from
   that script — one source of truth); an empty catala-golden set,
   a suite that ran zero checks, or
   a missing catala installation is a failure, never silently green.
   *Its CI leg is conditional by maintainer decision (2026-08-24):
   the `gates.yml` job runs the gate in full wherever the catala
   tools are available on the runner — on the bare PATH or in the
   runner's own opam switch, in whose environment the gate then runs —
   and skips loudly — a warning, never a silent green, never a
   blocker — on runners without them. github.com's hosted runners
   have no catala, so hosted CI does NOT catch a frontend
   regression; only a catala-carrying runner does. The gate script
   itself is unchanged and still fails hard without catala, so gate 9
   remains the reviewer's pre-merge duty on a machine that has it.*

10. `scripts/check-regelspraak.sh` — the RegelSpraak frontend
    (`frontends/regelspraak`, jar-only: JDK 21+, ANTLR + JUnit jars
    fetched into a gitignored `lib/`) builds and its JUnit suite
    passes with a non-zero test count; every corpus golden carrying a
    `bron.rgs` re-emits through `--ir` byte-identically to the
    committed `ir.json`; an empty rgs-golden set or a zero-test run
    is a failure, never silently green. Its CI leg is unconditional
    and blocking (maintainer decision 2026-08-27).

11. `scripts/check-toka-parity.sh` — **TEMPORARY** (maintainer decision
    2026-08-27): the isolated `toka/` parity zone. The frontend builds and
    the TOKA integration tests under `toka/` (excluded at the frontend
    takeover, brought here in original form) compile + pass against it with a
    non-zero test count; every toolchain-pinned `toka/**/ir.json` re-emits
    through uchar's own frontend byte-identically (frontend ≡ toolchain on
    the ©-Belastingdienst casus the public corpus may never carry); an empty
    fixture set, zero tests, or a stale holdout is a failure, never silently
    green. The reason-carrying holdout list (`toka-23`, the §4.1 datum+datum
    casus-quirk; `toka-45`, dimensionally inconsistent as written — surfaced
    by the identity unit typing, decision 16Y 2026-08-29, where uchar-v1
    deliberately diverges from the toolchain) mirrors gate 7 and fails when
    stale.
    CI leg unconditional and blocking. **This gate is the one documented
    exception to gate-set monotonicity: it leaves the set with the zone,
    ahead of any public flip.**

12. `scripts/check-drools.sh` — the Drools differential leg of the square
    (maintainer decision 2026-08-27). The Drools backend
    (`backends/drools`, a shaded Maven runtime uber-jar, JAR-ONLY like
    gate 10) CONSUMES the uchar IR: it loads every golden's `ir.json` off
    the wire (never the RegelSpraak source — the source-compile path may
    serve only as an internal test comparison, NEVER as the gate path),
    builds a KieBase through the ported exec-model back-end — which, since
    the defaults-service card (2026-08-29, PRIORITIZED-DEFAULTS-EVOLUTIE Q3), also carries a
    spec-ch.-7 prioritizedDefaults lowering the production toolchain does not
    have: the backend implements the uchar spec beyond the ported
    production semantics — and replays
    every `case*.json`. A golden whose forms the loader+transpiler carry
    MUST replay with EXACTLY the pinned expectations gates 5 (ref) and 6
    (clingo) verify — a played pass IS the square (ref ≡ clingo ≡ Drools
    over the corpus). Not-carried forms never vanish silently: per-form
    auto-refusals (a loader Refusal on a v1 node/section, a transpiler
    marker, a contract violation, a dimensioned/rational/list/struct or
    granular-parameter input, an objectcreatie, an underivable role
    reference, a name-sharing or ambiguous-reciprocal bean limit — the
    corpus-exercised temporal case-input forms and the wederkerig
    self-relation are CARRIED since 2026-08-30) each print their reason,
    grouped, in the gate report, and a reason-carrying value-divergence
    list IN THE SCRIPT (the check-noop survivor pattern) names each
    golden whose fired outcome differs from ref/clingo and FAILS WHEN
    STALE. The gate rebuilds the jar and runs the backend's own JUnit
    suite, which must pass with a non-zero, zero-skip test count
    (maintainer decision 2026-08-30). An empty played set is a failure.
    CI leg unconditional and blocking.

13. `scripts/check-reftype-derivation.sh` — the annotation-indifference
    harness (maintainer decision 2026-09-04). For every golden with case
    files, every unit-bearing `type` annotation on reference and
    composite positions is deterministically twisted (a convertible
    sister unit, or decimals+1) and ref, clingo and the Drools backend
    MUST all reproduce the pinned, untwisted outcomes — consumers derive
    units from declarations and the effective-type law
    (`RUNTIME-SEMANTICS.md` §2.10), never from the wire annotation.
    Reason-carrying exemption positions (the Literal lexeme, the
    duration operand of date arithmetic, TIJDSDUUR's own unit) are named
    in the script; a bookkeeping clamp FAILS the gate when a
    unit-bearing annotation appears at a node/operator class outside the
    twist table (the §2.10 fourth-class witness arriving), and a
    reason-carrying survivor list fails when stale (the check-noop
    pattern). An empty twist set is a failure, never silently green. CI
    leg unconditional and blocking.

## Repo relationships

- `regelspraak-java` (separate repository) is the first frontend + Drools
  backend. It consumes this repo at a pinned `SPEC_REV`; changes flow
  **spec-first**: the delta lands here (spec + corpus + schema), then a
  pin-bump conformance PR lands there.
- Its `RegelSpraakRuntime` is production semantics. That production runtime
  now lives HERE too, in exactly ONE place — `backends/drools/runtime/` (the
  Drools backend, ported at the drools-backend card so the IR has a third
  executor). `ref/` (the reference interpreter) and `spec/verifier/` (the
  OCaml verifier) must share NO code with it: separate implementations are the
  structural proof the spec is engine-neutral, no copy-paste imports either.
  Gate 1's no-share tripwire (decision V3-a) enforces this mechanically —
  `ref/` and `spec/verifier/` carry no production-runtime token
  (`RegelSpraakRuntime`, an `org.drools` import, or a signature method:
  `rsPlus`, `rsVerdeelOpVolgorde`, `Breuk`).

## Conventions

- Infrastructure, spec metamodel, and node names: English. Domain content
  (Jutterswet articles, declared names in corpus sources): Dutch, as
  authored.
- Smallest change that satisfies the task; prefer editing existing files;
  fail fast — no silent fallbacks or swallowed errors.
