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
- `.github/` (CODEOWNERS; the workflow file, hand-triggered only)
- `scripts/` (gate scripts), `WAY-OF-WORKING.md`, and this file

Mechanism: `.github/CODEOWNERS` lists these paths, and the mandate-and-review
protocol of `WAY-OF-WORKING.md` carries the review (§3: a card branch that
touches them stays an unmerged proposal). The repository is public since
2026-10-12. Since 2026-10-13 a ruleset on `main` blocks a force-push and the
deletion of the branch; it requires no pull request, so GitHub does not
enforce that review. A change to a protected path without maintainer review
must not land.

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
  only. No RegelSpraak specification excerpts enter this repo — the
  specification carries "Alle rechten voorbehouden" (all rights reserved) —
  **with exactly one exception:** the TOKA casus, Bijlage 1 of the
  specification, in the `toka/` parity zone (below). Programs *written in*
  RegelSpraak are ours; the language specification text is not.
- **The `toka/` parity zone (maintainer decision 2026-08-27; amended
  2026-10-13).** TOKA material is kept for feature-parity testing of uchar's
  own frontend against the original toolchain (gate 11), in one top-level
  `toka/` zone and **nowhere else**. Every other zone stays TOKA-free and
  gate 1 keeps tripping on it (`scripts/check-self-contained.sh`): the corpus
  is own work, and TOKA is third-party material whose release is not yet
  confirmed in writing. The repository is public since 2026-10-12, with the
  zone and its git history; the history is not rewritten. The zone is exempt
  from the self-containedness prose rules but carries no machine-local
  paths. Provenance and status, the same text as in `toka/README.md` and
  `README.md`:

  > **TOKA casus.** Source: *RegelSpraak specificatie* v2.3.0 (4-12-2025),
  > Bijlage 1, "Treinen Op Korte Afstand" (TOKA, a fictitious act from the
  > Belastingdienst's ALEF course), published by the Belastingdienst at
  > https://wendbarewetsuitvoering.pleio.nl/page/view/291f5846-cdfc-42d9-9f31-a2158fe59128/specificaties-regelspraak.
  > Rights notice of the source:
  > "© 2025 Belastingdienst / Alle rechten voorbehouden".
  > Status: the Belastingdienst indicated to the maintainer on 2026-10-13 that
  > TOKA is meant to be releasable. It is probably under EUPL-1.2, like ALEF
  > (https://github.com/belastingdienst/ALEF) and the repository
  > MinBZK/regels.overheid.nl, which carries a 2022 version of the course
  > casus. Written confirmation: not yet received.
  > The licences of this repository (Apache-2.0, CC BY 4.0) do not cover this
  > material.
  >
  > Files in this repository. Copied verbatim from the source: the 47
  > definition fragments `toka/casus/toka-*.rgs`. Derived from them or own
  > work: the IR dump `toka/casus/ir.json` and the 29 IR slices
  > `toka/cases/scenario-*/ir.json` (the dump and 22 of the slices carry text
  > of the fragments in their source snippets); the 29 case files
  > `toka/cases/scenario-*/case.json`, with situations and expected values
  > derived by hand; the parity manifest `toka/cases/parity.manifest`; the two
  > tests under `toka/src/test/java/`, which quote lines of the fragments; and
  > `toka/README.md`.
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
green.

**Where the gates run (maintainer decision 2026-09-26).** The gate set runs
LOCALLY, on the maintainer's machine, before every landing
(`WAY-OF-WORKING.md` §6); a red gate blocks the landing. GitHub Actions is
not used and not paid for: `.github/workflows/gates.yml` documents the order
and the per-gate environment and is triggered by hand only
(`workflow_dispatch`), so it never runs on a push. Wherever a gate below
says "CI leg unconditional and blocking" (or, for gate 9, conditional on
the runner), read that as the decision taken at the time about the
workflow; what is enforced is the local run, and gate 9 there is the
reviewer's duty on a machine that has catala. Current gates:

1. `scripts/check-self-contained.sh` — artifact zones (`corpus/golden`,
   `corpus/negatief`, `corpus/rood`, `spec/schema`, `spec/verifier`,
   `ref`, `backends`, `frontends` — since the Phase-5a skeleton — and,
   since the Phase-1 exit, `spec/IR-SPEC.md`
   and `spec/RUNTIME-SEMANTICS.md`)
   carry no machine-local paths or private-repo references, and corpus data
   is free of TOKA material (provenance: the corpus is own work, and TOKA is
   third-party material whose release is not yet confirmed in writing) —
   with the single exception of the `toka/` parity zone (see Corpus rules),
   which the tripwire deliberately skips while it keeps firing on every
   other zone; `toka/` itself is checked only for machine-local paths.
   Working prose (plan, backlog, reviews) may reference the private source
   repos during development — see `WAY-OF-WORKING.md` §5–6.
2. `scripts/check-ir-schema.sh` — the schema file is metaschema-valid;
   every `corpus/golden/**/ir.json` and `corpus/rood/**/ir.json`
   validates against
   `spec/schema/uchar-ir-v1.schema.json` (since L1 of the v1 wire
   migration, 2026-09-24; `regelspraak-ir-v0` until then) and round-trips
   byte-identically; and, since the card-less fix round of 2026-09-25,
   every object in `corpus/golden`, `corpus/rood` and `corpus/negatief`
   carries its keys in the canonical order of the subschema it instantiates
   (the `properties` order the schema's description makes canonical; an
   object no subschema admits is counted apart, never skipped), failing
   with the path and both orders on a deviation; an unpopulated corpus is
   reported explicitly, never silently green.
3. `scripts/check-verifier.sh` — the OCaml verifier (`spec/verifier/`)
   builds; every `corpus/golden/**/ir.json` and `corpus/rood/**/ir.json`
   is verifier-clean; every
   `corpus/negatief/<wf###-slug>/ir.json` is rejected, citing the WF
   rule its directory name declares — and, since L2 of the v1 wire
   migration (2026-09-24), on the ARM its `expect.txt` names where that
   file is present (one literal fragment of the verifier's sentence,
   pointer-free; a blank or multi-line file fails; the count carries a
   floor `EXPECTED_MIN_EXPECT`), so a witness that drifts to another arm
   of its own rule no longer passes unseen; and, since the card-less fix
   round of 2026-09-25, every JSON pointer a negatief report cites resolves
   in that case's document (RFC 6901; the pointer is read up to the first
   `: `, spaces included, and a FAIL line without that separator fails the
   gate; the count carries a floor `EXPECTED_MIN_POINTERS`), so a witness
   whose report points into the void no longer passes; an empty negatief
   corpus is a failure, never silently green.
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
   defeasible standing, or a consistency that already holds on the input
   (`jutterswet-vondstnummer/case-consistent`, and since 2026-10-14
   `jutterswet-uniciteit-drie-bronnen/case2-zonder-botsing`) — live in
   the script's explicit, reason-carrying survivor list, which fails
   when stale; an empty case set is a failure.
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

11. `scripts/check-toka-parity.sh` — the `toka/` parity zone (maintainer
    decision 2026-08-27; a regular gate of the set since 2026-10-13, when
    the maintainer decided that the zone stays). The frontend builds and
    the TOKA integration tests under `toka/` (excluded at the frontend
    takeover, brought here in original form) compile + pass against it with a
    non-zero test count; every toolchain-pinned `toka/**/ir.json` re-emits
    through uchar's own frontend byte-identically (frontend ≡ toolchain on
    the TOKA casus, which the corpus does not carry — since L1 of the v1
    wire migration, 2026-09-24, a REGRESSION form: the zone
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
    Since the card toka-welgevormd (2026-09-25; maintainer decision the same
    day: the zone stays a while longer, in its own directory) the gate also
    holds every replay slice VERIFIER-CLEAN — a well-formedness leg with its
    own floor `EXPECTED_MIN_CLEAN` and a reason-carrying holdout list
    (`VERIFIER_HOLDOUTS`, the slice whose clamp bound carries a unit against a
    unitless base, WF-099) that fails when stale; the casus dump stays outside
    that leg (the parity object, over-determined by its nature: its scenarios
    write the same targets each in their own way).
    CI leg unconditional and blocking.

12. `scripts/check-drools.sh` — the Drools differential leg of the square
    (maintainer decision 2026-08-27). The Drools backend
    (`backends/drools`, a shaded Maven runtime uber-jar, JAR-ONLY like
    gate 10) CONSUMES the uchar IR: it loads every golden's `ir.json` off
    the wire (never the RegelSpraak source — the source-compile path may
    serve only as an internal test comparison, NEVER as the gate path),
    builds a KieBase through the exec-model branch — since the card
    `tms-omschakeling` (2026-10-10) the lowering with truth maintenance
    (`transpiler/tms`), which replaced the exec-model back-end ported from the
    production toolchain; like that back-end since the defaults-service card
    (2026-08-29, PRIORITIZED-DEFAULTS-EVOLUTIE Q3) it carries a spec-ch.-7
    prioritizedDefaults lowering the production toolchain does not have, so
    the backend implements the uchar spec beyond the ported production
    semantics — and replays every `case*.json`. A golden whose forms the loader+transpiler carry
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
    gate also sweeps the DRL-TEXT branch (card gate12-drl-takteller)
    through the replay runner's branch flag. Since the card
    `drl-teksttak-op-truth-maintenance` (2026-10-14) that branch is the
    second rendering of the one analysis the exec-model branch fires
    (`transpiler/tms`: the analysis yields a plan per rule, rendered once
    as PatternDSL and once as DRL text), so it derives with truth
    maintenance too and carries no list and no pin of its own: per
    golden its status line must equal the exec-model branch's line
    verbatim (`PLAYED n`, or `EXCLUDE` in the same sentence), and any
    other line fails the gate by name — a refusal of the DRL rendering
    alone or a text the Drools compiler rejects is such a difference,
    never an entry of a list. Until then the leg had an
    `EXPECTED_DRL_PLAYED` equality pin and a reason-carrying
    `DRL_CHANNEL` list; both went with the destructive lowering they
    described. The sweep runs as the gate's final block, so only an
    otherwise-green gate pays its cost (measured 2026-10-14 on a copy:
    220 s of 635 s). The gate also fails when the backend's suite did
    not run `BranchDumpsCorpusWitnessTest`, the case-by-case comparison of
    the two branches over every golden and red case. An empty played
    set is a failure. For a builder who changes a pin or a list,
    `scripts/check-drools.sh --fast` rebuilds the jar without the JUnit
    suite, runs the exec-model replay with every pin and list, and
    leaves out the DRL-text sweep, so nothing compares the two branches
    under it; its report names what did not run and its last line says
    it is no landing run (maintainer request 2026-10-14). A landing
    always runs the full gate, without a flag. CI leg unconditional and
    blocking.

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
    2026-09-04, the DRL-text branch behind the runner's own flag — since
    2026-10-14 the second rendering of the same analysis) MUST all
    reproduce the pinned, untwisted outcomes — consumers
    derive units from declarations and the effective-type law
    (`RUNTIME-SEMANTICS.md` §2.10), never from the wire annotation.
    Each leg is compared against its OWN untouched status line. Since
    2026-10-14 the DRL-text leg's untouched line must moreover equal the
    exec-model branch's, and it must play exactly as many twistable
    goldens as the exec-model branch, never zero; the floor that leg had
    of its own (`EXPECTED_MIN_DRL_PLAYED`) is gone.
    Reason-carrying exemption positions (the Literal lexeme, the
    duration operand of date arithmetic, the `duration` builtin's own unit) are named
    in the script; a bookkeeping clamp FAILS the gate when a
    unit-bearing annotation appears at a node/operator class outside the
    twist table (the §2.10 fourth-class witness arriving), and a
    reason-carrying survivor list fails when stale (the check-noop
    pattern). An empty twist set is a failure, never silently green.
    Since 2026-09-27 (card `vocabulaires-benamingen`) the gate carries a
    VOCABULARY leg beside the unit twist: every golden with `vocabularies`
    and case files is replayed with the side table STRIPPED, with the
    vocabularies EXCHANGED and with the designations ROTATED within each
    scope of WF-118, and ref, clingo and both Drools branches must
    reproduce the untouched outcome (IR-SPEC ch. 2.12: the table carries
    no semantics, so no reading of it is legitimate and there is no
    survivor list); floors `EXPECTED_MIN_VOCABULARY_GOLDENS=1`,
    `EXPECTED_MIN_VOCABULARY_STRIPPED=46` and
    `EXPECTED_MIN_VOCABULARY_ROTATED=36` (measured 2026-09-27); a golden
    without vocabularies is not probed. Since 2026-10-15 (card
    `herkomst-op-de-draad`) an ORIGIN leg beside it: every golden with an
    `origin` (IR-SPEC ch. 2.13) and case files is replayed with the field
    STRIPPED, and ref, clingo and both Drools branches must reproduce the
    untouched outcome (no survivor list); floors
    `EXPECTED_MIN_ORIGIN_GOLDENS=9` and `EXPECTED_MIN_ORIGIN_STRIPPED=71`
    (measured 2026-10-15). CI
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

15. `scripts/check-graaf.sh` — the rule graph of the Drools backend
    (`backends/drools/.../graaf/`, a reading of the IR the distribution's
    scope analysis consumes; card `graaf-samenvoeging-en-gate`, 2026-09-26)
    holds to IR-SPEC ch. 5. SPEC LEG: the graph is built for every golden
    from its `ir.json` through the loader — never from the source — and
    compared per golden with the verifier's ch. 5 edges
    (`verifier --dependency-edges`, the list WF-045/046 walk) over an
    attribute or characteristic node, whatever forms of the rule layer they
    join (rules, object and fact creations, allocations, consistencies,
    uniquenesses, prioritized-defaults groups), without the edge kind and
    without the type in the node (the graph keys a member on its declaring
    type); edges over a population, a relation or a channel are counted per
    class and reported, never compared; a difference fails unless it is
    listed in `DELIBERATE_DIFFERENCE`, one projected line per entry with both
    forms' lists and its reason, stale-failing in both directions; a golden
    the loader refuses cannot be compared and is reported with its form.
    ROUTE LEG: on every golden with a `bron.rgs` the graph from the source
    route equals the graph from the wire route byte for byte, and the
    graph's own unit tests (under `backends/drools/src/source-route`, compiled
    by the gate against the frontend and the backend jar) pass with a
    non-zero count. Floors in the script (measured 2026-09-26 at the wave's
    landing): `EXPECTED_MIN_GOLDENS=234`, `EXPECTED_MIN_DEPENDENT=116`,
    `EXPECTED_MIN_SOURCE_ROUTE=98`, `EXPECTED_MIN_TESTS=23`. An empty
    comparison is a failure, never silently green. CI leg unconditional and
    blocking.

16. `scripts/check-scenario.sh` — the scenariotoetsen extension of the
    RegelSpraak frontend (`frontends/regelspraak/grammar/DIVERGENCES.md`,
    "Scenariotoetsen"; card `scenariolaag-in-uchar`, 2026-09-26): every
    corpus golden carrying a `scenario.rgs` beside its `bron.rgs`
    regenerates, through the frontend's `--cases` mode, exactly its pinned
    case files (the same names, none missing and none extra)
    byte-identically, and its `ir.json` byte-identically — without a scope
    the production route's IR document, with one the forms the scope
    carries; the handwritten expectations are the anchor, never the
    lowering. An empty scenario-golden set is a failure; floors
    `EXPECTED_MIN_SCENARIO_GOLDENS=7` and
    `EXPECTED_MIN_SCENARIO_CASES=31` (measured 2026-09-27, card
    `scenario-parametersets`). Gate 4 stays
    the schema gate of those case files and gates 5, 6 and 12 their
    replay. CI leg unconditional and blocking (jar-only, like gate 10).

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

- `uchar-dev/regelspraak-toolchain` (separate, public repository since
  2026-10-12; the successor of `yhavinga/regelspraak-java`, maintainer
  decision 2026-09-26; the old repository is archived since the same day)
  is the DISTRIBUTION — CLI, service, docker/helm — with no semantics of its
  own: it consumes this repo as a git SUBMODULE (the submodule commit is the
  pin; no `SPEC_REV`, no conformance gate) and builds `frontends/regelspraak`
  and `backends/drools` from that submodule in a Maven reactor. Package
  ownership: `dev.uchar.regelspraak.*` is THIS repo's (renamed from
  `io.github.yhavinga.regelspraak` at the card `pakkethernoeming`,
  2026-09-26); the distribution lives in `dev.uchar.regelspraak.toolchain.*`
  and reads only public API — never the same package. Changes still flow
  **spec-first**: the delta lands here (spec + corpus + schema), then the
  distribution moves its submodule pin.
- The production `RegelSpraakRuntime` lives in exactly ONE place —
  `backends/drools/runtime/` (the Drools backend, ported at the
  drools-backend card so the IR has a third executor; since the move the
  distribution carries no copy). `ref/` (the reference interpreter) and `spec/verifier/` (the
  OCaml verifier) must share NO code with it: separate implementations are the
  structural proof the spec is engine-neutral, no copy-paste imports either.
  Gate 1's no-share tripwire (decision V3-a) enforces this mechanically —
  `ref/` and `spec/verifier/` carry no production-runtime token
  (`RegelSpraakRuntime`, an `org.drools` import, or a signature method:
  `rsPlus`, `rsVerdeelOpVolgorde`, `Breuk`).

## Conventions

- Every identifier is English: classes, interfaces, records, enums and
  their constants, methods, fields, variables, OCaml modules, types,
  constructors and functions, grammar rules, ASP predicates, shell
  variables, CLI flags and test names; so is every file and directory
  name.
- Where the wire has a name for a concept, the code uses it
  (`emptyFallback`, not `leegwaarde`). `scripts/lib/naming/glossary.tsv`
  gives the English word for the Dutch words in the code, with its source
  per row (wire, spec, field or plain).
- Dutch, as authored: domain content (the Jutterswet, declared names,
  RegelSpraak sources, case labels, the `jutterswet-*` names) and the
  RegelSpraak keywords (the lexer's literals and the token names that
  spell them).
- Not renamed, each for its reason: the production runtime under
  `backends/drools/src/main/java/dev/uchar/regelspraak/runtime/` (gate 1
  recognises a copy by its names), the `toka/` zone, v0 names where they
  stand as v0 names (IR-SPEC "v0 name:", CONSTRUCTS, `spec/proposals/`),
  git history and `backlog/done/`.
- Working prose — cards, Results, commit messages, `spec/proposals/` — is
  Dutch or English; every identifier it names or introduces is English,
  also when a card prescribes a new class.
- Smallest change that satisfies the task; prefer editing existing files;
  fail fast — no silent fallbacks or swallowed errors.
