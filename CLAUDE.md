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
  front, and ends by updating that task; a scope change reaches
  `spec/PLAN.md` through the main session's serial bookkeeping (see
  Protected paths).
- Update THIS file only when an invariant or protected path changes. It is
  the constitution's index, not a logbook.

## Protected paths — propose, never merge

Sessions may propose diffs to these; only the maintainer merges them:

- `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`,
  `spec/verifier/`
- `corpus/golden/anchor/` (hand-written cases with absolute expected values)
- `.github/` (CI configuration, CODEOWNERS)
- `scripts/` (gate scripts), `WAY-OF-WORKING.md`, and this file

Mechanism: `.github/CODEOWNERS` plus branch protection (note: on the free org
plan branch protection requires a public repo). A change to a protected path
without maintainer review must not land.

**The CODEOWNERS claim is wider on purpose** (recorded 2026-09-10, existing
practice). `.github/CODEOWNERS` claims `/spec/` whole — a safety net,
deliberately broader than the list above: too much review beats too little.
Within that claim, per item:

- `spec/PLAN.md` and `spec/CONSTRUCTS.md` are the card protocol's
  bookkeeping channel, and the MAIN session keeps it — the maintainer's own
  serial session on `main`, not a card session, so that landing IS the
  maintainer's hand and the rule above holds. The tracked CONSTRUCTS deltas,
  and PLAN when scope changed, land at the card's landing: in the commit
  that moves the card to `done/`, or in one right after it (merges are
  fast-forward; there is no merge commit). No separate review up front. A
  card session does not touch them unless its card says so explicitly; it
  puts proposed row text in its `Result:` note. `WAY-OF-WORKING.md` §1
  forbids parallel cards to share a registry; these two are exactly that,
  and serial main-session bookkeeping is how they satisfy it.
- Everything else under `spec/` — today `spec/ASP-TRANSLATION.md`,
  `spec/IR-REVIEW.md`, `spec/proposals/` — follows that net: a change is a
  proposal unless the maintainer grants mandate for it, normally in the
  card.

## Spec discipline

- Every normative MUST carries an ID (`WF-###`) with a verifier check, at
  least one passing golden case, and one failing negatief case.
- Every semantics clause carries a worked example with concrete expected
  output; those examples become the reference interpreter's tests verbatim.
- **A spec sentence without a machine-checkable artifact is not done** — the
  spec-level "a green subset is not green".
- Mirror, then diverge: v0 is the faithful dump of `regelspraak-java`'s IR;
  every v1 deviation is a tracked delta in `spec/CONSTRUCTS.md` (landed by
  the main session — see Protected paths), never a silent rewrite.

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
   `spec/schema/uchar-ir-v1.schema.json` (since L1 of the v1 wire
   migration, 2026-09-24; `regelspraak-ir-v0` until then) and round-trips
   byte-identically; an unpopulated corpus is reported explicitly, never
   silently green.
3. `scripts/check-verifier.sh` — the OCaml verifier (`spec/verifier/`)
   builds; every `corpus/golden/**/ir.json` and `corpus/rood/**/ir.json`
   is verifier-clean; every
   `corpus/negatief/<wf###-slug>/ir.json` is rejected, citing the WF
   rule its directory name declares — and, since L2 of the v1 wire
   migration (2026-09-24), on the ARM its `expect.txt` names where that
   file is present (one literal fragment of the verifier's sentence,
   pointer-free; a blank or multi-line file fails; the count carries a
   floor `EXPECTED_MIN_EXPECT`), so a witness that drifts to another arm
   of its own rule no longer passes unseen; an empty negatief corpus is a
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
   green. Since 2026-09-10 it also runs `ref/`'s own worked-example suite
   (`ref/test/`) — the executable form of the `RUNTIME-SEMANTICS.md`
   examples, which until then no gate ran at all — demanding its count
   line and holding it to a floor, exactly as gate 6 runs the clingo
   backend's own suite.
6. `scripts/check-clingo.sh` — the differential: `backends/clingo`
   translates every corpus case to ASP-Core-2 and clingo's stable model
   must match the same absolute expectations gate 5 pins for `ref/`
   (ref ≡ Clingo over the corpus, anchored to hand-written values); an
   empty case set is a failure, never silently green. It also runs the
   backend's own unit suite — the normalisation fault modes no corpus
   witness can exercise — demanding its check-count line and holding
   that count to a floor; an un-run or shrunken suite is a silent green,
   so neither is allowed.
7. `scripts/check-noop.sh` — the no-op guard (PLAN §8: an identity
   transpiler must fail): every golden case run through the reference
   runner's `--noop` mode (expectations against the raw input, no
   evaluation) must fail; the cases that legitimately survive — they pin
   defeasible standing — live in the script's explicit, reason-carrying
   survivor list, which fails when stale; an empty case set is a
   failure.
8. `scripts/check-rood.sh` — the red corpus: every
   `corpus/rood/**/case*.json` declares `expectedFault {ref, clingo}`
   and MUST fail observably — at load or at
   evaluation (contract widened 2026-09-19) — in **both** executors with the
   pinned fragment in each report (the unsolved-state guard among them:
   `Unresolved` — the v0 `Onopgelost` — reached in evaluation faults with WF-044, never
   vanishes); an empty red set is a failure, never silently green; and no pinned fragment cites a BOTTOM SENTENCE of `ref` or clingo — a fall-through arm's refusal, which is the same sentence for every form that arm catches because it describes the position and what is carried there rather than the construct rejected. Such a pin witnesses nothing of its own, and where the sentence claims no corpus case reaches it, it makes that claim untrue. The thirty-six sentences are listed in the script with source, function and reason; the fragments are decoded and compared by `ref --bodemzin`, the red path's own reader and matcher, so a JSON-escaped pin cannot slip past, and a standing self-test in the gate fails if that decoding is ever replaced by a text scrape. The cure is a named arm for the form, never a reworded bottom.

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
    the ©-Belastingdienst casus the public corpus may never carry — since L1
    of the v1 wire migration, 2026-09-24, a REGRESSION form: the zone
    migrates with every landing, the casus dump is re-emitted by the
    frontend and the slices go through the landing's migration script, so
    the comparison proves frontend ≡ the recorded state, `toka/README.md`
    lists each landing under its deliberate divergences); an empty
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
    granular-parameter input, an object creation, an underivable role
    reference, a name-sharing or ambiguous-reciprocal bean limit — the
    corpus-exercised temporal case-input forms and the reciprocal
    self-relation are CARRIED since 2026-08-30) each print their reason,
    grouped, in the gate report, and a reason-carrying value-divergence
    list IN THE SCRIPT (the check-noop survivor pattern) names each
    golden whose fired outcome differs from ref/clingo and FAILS WHEN
    STALE. Since 2026-09-06 an expected-channel floor IN THE SCRIPT pins
    the played channel itself: every NON-played golden is listed by name
    with its reason (played is the default), so a golden that SHIFTS
    CHANNEL and loses its PLAYED status fails the gate by name instead of
    being silently counted among the refusals, and a stale pin — a listed
    golden that plays again, or is gone — fails too. A pinned played
    count closes the one direction a played-by-default list cannot see: a
    newly arrived player. The gate rebuilds the jar and runs the
    backend's own JUnit suite, which must pass with a non-zero, zero-skip
    test count (maintainer decision 2026-08-30). Since 2026-09-19 the
    gate also counts the DRL-TEXT branch (card gate12-drl-takteller):
    its own sweep over every golden through the replay runner's branch
    flag, an EXPECTED_DRL_PLAYED equality pin plus a reason-carrying
    DRL_CHANNEL non-player list (played is the default; both lists
    share one guard function), stale-failing in both directions by
    name; the sweep runs as the gate's final block, so only an
    otherwise-green gate pays its cost. An empty played set is
    a failure. CI leg unconditional and blocking.

13. `scripts/check-reftype-derivation.sh` — the annotation-indifference
    harness (maintainer decision 2026-09-04). For every golden with case
    files, every unit-bearing `type` annotation on the derived (composite)
    positions that still carry one — since L2 of the v1 wire migration
    (2026-09-24) the reference positions carry no `type` at all (B13),
    so the twist table lost `AttribuutRef`, `ParameterRef`, `DimensieCel`
    and `BinderRef` (v0 names; since L5 `AttributeReference`,
    `ParameterReference`, `DimensionCellRead`) and `EXPECTED_MIN_TWISTS` was
    re-measured (V15 (b)) —
    is deterministically twisted (a convertible
    sister unit, or decimals+1) and ref, clingo and BOTH Drools
    transpiler branches (the exec-model gate-12 path and, since
    2026-09-04, the classic DRL-text branch behind the runner's own
    flag) MUST all reproduce the pinned, untwisted outcomes — consumers
    derive units from declarations and the effective-type law
    (`RUNTIME-SEMANTICS.md` §2.10), never from the wire annotation.
    Each leg is compared against its OWN untouched status line, with a
    floor: a DRL-text branch that PLAYS nothing fails the gate.
    Reason-carrying exemption positions (the Literal lexeme, the
    duration operand of date arithmetic, the `duration` builtin's own unit) are named
    in the script; a bookkeeping clamp FAILS the gate when a
    unit-bearing annotation appears at a node/operator class outside the
    twist table (the §2.10 fourth-class witness arriving), and a
    reason-carrying survivor list fails when stale (the check-noop
    pattern). An empty twist set is a failure, never silently green. CI
    leg unconditional and blocking.

14. `scripts/check-flogic.sh` — the F-logic frontend (`frontends/flogic`,
    plain OCaml in the workspace's dune build, so no fetched jars and no
    external compiler) builds and its own suite passes with a non-zero
    check count: the suite emits every test program, holds it
    verifier-clean, replays its cases through `ref` AND clingo, and drives
    one fixture per refusal, checking both the reason and the citation of
    the mandate document the refusal list belongs to. Every corpus golden
    carrying a `bron.flogic` re-emits through the frontend
    byte-identically after canonicalisation (gate 2's serialiser shape,
    executed from that script — one source of truth, as in gate 9). An
    empty flogic-golden set, a suite that ran zero checks, or a missing
    dune is a failure, never silently green. Counting floors in the
    script: `EXPECTED_MIN_CHECKS=81` and `EXPECTED_MIN_GOLDENS=1`, both
    measured 2026-09-12. CI leg unconditional and blocking — unlike gate
    9 this frontend links no external toolchain, so every runner can run
    it.

**Counting floors (maintainer decision 2026-09-09).** Beside the non-empty
checks and the name-carrying lists, every gate whose count could shrink unseen
carries an `EXPECTED_MIN_<name>` constant in its own script, with its reason
and its measurement date, and fails naming both the measured number and the
floor. They are lower bounds, not equalities: they catch shrink — a suite that
quietly runs half, a corpus zone that quietly halves — while growth needs no
bump, so an ordinary corpus card touches no protected script. Raising a floor
is a deliberate act, done at an occasion; a deliberate removal lowers it in the
same commit. The accepted weakness, recorded rather than hidden: a count that
stays equal while its content shifted passes, and the name-carrying lists are
what cover that direction. Gate 12's `EXPECTED_PLAYED` stays an equality
(decision 2026-09-06) — this regime generalised from it and does not reopen it.
A gate that gets no floor carries one sentence in its script saying why.

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
