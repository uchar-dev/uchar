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
- `scripts/` (gate scripts), the root `Makefile` (`make gates`),
  `WAY-OF-WORKING.md`, and this file

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
  least one passing golden case, and one failing negative case.
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
  > derived by hand; the parity manifest `toka/cases/parity.manifest`; the three
  > tests under `toka/src/test/java/`, which quote lines of the fragments or
  > names written in them and whose identifiers follow every rename of the
  > frontend; and `toka/README.md`.
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

Grows monotonically; nothing that entered may regress; a green subset is not green. What each gate holds is below;
its numbers (floors, lists) live in its script, and how it came to hold it is in `git log` and `backlog/done/`.

**Where the gates run (maintainer decision 2026-09-26).** Locally, on the maintainer's machine, before every landing
(`WAY-OF-WORKING.md` §6); a red gate blocks the landing. GitHub Actions is not used: `.github/workflows/gates.yml`
documents the order and the per-gate environment and is triggered by hand only (`workflow_dispatch`). Gate 9 needs
catala; on a machine without it the gate fails hard, and running it is the reviewer's duty on a machine that has it.

**How the set runs (maintainer decision 2026-10-25).** `make gates` in the repository root first checks that the gate
scripts its targets run are those `.github/workflows/gates.yml` runs, in that order, and those this list names, and
stops before any build, naming the script, where they differ. It then builds the three trees the gates read, side by
side: the OCaml workspace (`scripts/lib/ocaml-build.sh`), the RegelSpraak frontend (`frontends/regelspraak/Makefile`)
and the Drools backend jar (`scripts/lib/drools-jar.sh`); then it runs the gates in parallel in the same checkout
(`make -k -j<cores>`), one log per gate under `out/gates/<run>/` and a summary with one line per gate in the order of
`gates.yml`. A single gate runs with its script. Every gate builds only what is stale: dune when a source is newer
than the stamp of the last green workspace build, the frontend when a source is newer than its classes, the jar when
a source is newer than the jar. After the build step no gate builds and no two gates write the same file, provided no
source changes during the run and no other gate, build or `make gates` runs in the same checkout. The step
`after-run` checks the first condition: the three builds run again and must be fresh, and none of their products may
be newer than the end of the build step. A red build, red gate, red `after-run` or missing line exits non-zero.
Replays run through `scripts/lib/parallel.sh` (one process per two cores, with its replay flags on JVMs) and are
judged in corpus order; no gate takes arguments.

Throughout: "stale-failing" means a list fails on an entry that no longer applies, in both directions where said;
"reason-carrying" means every entry carries its reason; an empty input set is a failure, never a silent green.

1. `scripts/check-self-contained.sh` — the artifact zones (`corpus/golden`, `corpus/negative`, `corpus/red`,
   `spec/schema`, `spec/verifier`, `ref`, `backends`, `frontends`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`)
   carry no machine-local paths or private-repo references, and corpus data carries no TOKA material (the corpus is
   own work; TOKA is third-party material whose release is not yet confirmed in writing). The `toka/` parity zone is
   skipped by the TOKA tripwire and checked only for machine-local paths. The no-share tripwire: `ref/` and
   `spec/verifier/` carry no production-runtime token (see Repo relationships). Working prose (plan, backlog,
   reviews) may reference the private source repos (`WAY-OF-WORKING.md` §5–6).

2. `scripts/check-ir-schema.sh` — `spec/schema/uchar-ir-v1.schema.json` is metaschema-valid; every
   `corpus/golden/**/ir.json` and `corpus/red/**/ir.json` validates against it and round-trips byte-identically;
   every object in `corpus/golden`, `corpus/red` and `corpus/negative` carries its keys in the canonical order of the
   subschema it instantiates (the `properties` order), failing with the path and both orders; an object no subschema
   admits is counted apart, never skipped. A shape self-test holds `EmbeddedExpression.unit` to `true` and
   `DeclaredUnit.symbol` to a non-empty string. Floor: `EXPECTED_MIN_DOCS`.

3. `scripts/check-verifier.sh` — the OCaml verifier (`spec/verifier/`) builds in the workspace `_build`
   (`scripts/lib/ocaml-build.sh`), the one gates 11 and 15 use, and builds on its own: no dune file under
   `spec/verifier` names a library of another directory, so `dune build --root spec/verifier` works. Every golden
   and red `ir.json` is verifier-clean; every `corpus/negative/<wf###-slug>/ir.json` is rejected citing the rule its
   directory names, one negative per arm of every rule. A violation carries a key `WF-###.<slug>` (one per message
   template) and named parameters; the report line is `WF-###.<slug> FAIL <pointer>: <message>`, and
   `verifier --arms` prints key, pointer and parameters. `expect.txt`, where present, is a key line of the declared
   rule plus `name=value` lines, matched as a subset against the parameters of one violation. Every JSON pointer a
   negative report cites resolves in its document (RFC 6901, read up to the first `: `; a FAIL line without that
   separator fails). The gate refuses a key with two message templates and a violation built without a key in
   `checks.ml`. Floors: `EXPECTED_MIN_ARMS`, `EXPECTED_MIN_PARAM_LINES`, `EXPECTED_MIN_REPORT_KEYS`,
   `EXPECTED_MIN_ARM_SITES`, `EXPECTED_MIN_EXPECT`, `EXPECTED_MIN_POINTERS`, `EXPECTED_MIN_NEGATIVE`.

4. `scripts/check-case-schema.sh` — `spec/schema/uchar-case-v0.schema.json` is metaschema-valid; every golden and
   red `case*.json` validates against it and sits beside an `ir.json`; golden carries `expected`, red carries
   `expectedFault`. Self-test rows hold the shape of `expectedFault` (a `params` object of strings, no fragment key).

5. `scripts/check-oracle.sh` — the reference interpreter (`ref/`) builds and replays every golden `case*.json` with
   exact expectation match (the oracle guard of PLAN §8); goldens without a case file are listed. It also runs
   `ref/test/`, the executable form of the `RUNTIME-SEMANTICS.md` worked examples, demanding its count line. Floors:
   `EXPECTED_MIN_CASES`, `EXPECTED_MIN_EXAMPLES`.

6. `scripts/check-clingo.sh` — the differential: `backends/clingo` translates every golden case to ASP-Core-2 and
   clingo's stable model must match the absolute expectations gate 5 pins. Played is the default. A golden whose form
   the encoding does not carry stands in `EXPECTED_CHANNEL`, `<golden>|CLINGO-###[, CLINGO-###…]|<reason>`, and
   refuses on every case with exactly those codes at the head of the runner's report, in any order, each a `refusal`
   or `limit` code of `backends/clingo/codes.tsv` (never a `fallback` code). The gate fails by golden name on a
   refusal without a line, a line whose golden plays every case, refuses with other codes or has no case file, and a
   golden whose cases partly play and partly refuse; a case that fails without a register code at its head, with an
   EF code there or with a mismatch fails whether listed or not. The list refuses a golden named twice, a line
   without a reason, a code named twice and a code that is no refusal or limit; its judge passes rows of its own
   first. The list has no floor: it is name-carrying in both directions, and gate 5 floors the case set. It also runs
   the backend's unit suite (normalisation fault modes no corpus witness reaches). Floor: `EXPECTED_MIN_SUITE_CHECKS`.

7. `scripts/check-noop.sh` — the no-op guard (PLAN §8: an identity transpiler must fail): every golden case run
   through the ref runner's `--noop` mode (expectations against the raw input) must fail. Cases that legitimately
   survive — they pin defeasible standing, or a consistency that already holds on the input — stand in `SURVIVORS`,
   reason-carrying and stale-failing.

8. `scripts/check-red.sh` — every red `case*.json` declares `expectedFault {code, params}`: the EF code of its fault
   kind (RUNTIME-SEMANTICS ch. 5.7b) and its parameters as an object of strings, no other key. Each must fail
   observably, at load or at evaluation, in both executors as that kind: ref and clingo print one
   `CODE <code> <json>` line per observed code and `CODE-PINNED <code> <json>`, decoded by the runner's case reader
   (never a shell scrape), and the pinned code with its pinned parameters must be among the emitted ones (a subset).
   The Drools leg reads the same from its status line `FAULT EF-### {…}: …` for every case not in its exclusion list,
   whose `EXCLUDE` entry (`name|EXCLUDE DROOLS-###  <reason>`) pins the register code of the refusal, compared with
   the head of the status line by case name with both codes; a `NOFAULT` entry carries none. `Unresolved` reached in
   evaluation faults with EF-019 (WF-044). An executor that fails a case for a reason of its own stands in the
   divergence list `<case>|<executor>|<CODE>|<CATEGORY> <reason>`, with its `REF-###` or `CLINGO-###` code and the
   category of that code's kind (`REFUSAL`, `LIMIT`); stale-failing in both directions. `scripts/check-exec-codes.sh`,
   called by this gate, holds `ref/codes.tsv` and `backends/clingo/codes.tsv` to their sources in both directions,
   parameters included. A `fallback` code is named by no pin, divergence entry, Drools exclusion or gate-6/12 channel
   entry, and a fault reporting only a `fallback` code is a code other than the pinned one. Every emitted parameter
   is a key of the parameter column of ch. 5.7b; the case pairs that differ in a parameter only are told apart by
   their pinned `params` (stale-guarded). A §9.5 channel fact carries the EF codes of its kinds (ch. 5.6); a golden
   `inconsistent` expectation may pin one (`code`), gates 5, 6 and 12 hold the executors to it, and this gate holds
   every such code to a row of ch. 5.7b on the §9.5 channel. Floors: `EXPECTED_MIN_RED_CASES`,
   `EXPECTED_MIN_RED_CODES`, `EXPECTED_MIN_RED_PARAMS`, `EXPECTED_MIN_EF_CODES`, `EXPECTED_MIN_GOLDEN_CODES`,
   `EXPECTED_MIN_DROOLS_EXCLUDE_CODES`, `EXPECTED_MIN_DROOLS_PARAMS`, and in check-exec-codes
   `EXPECTED_MIN_REF_CODES`, `EXPECTED_MIN_CLINGO_CODES`.

9. `scripts/check-catala.sh` — the Catala frontend builds and its full suite passes (E2E through verifier, ref and
   clingo, plus the refusal fixtures); every golden carrying a `source.catala_en` regenerates through the plugin
   byte-identically after canonicalisation (gate 2's serialiser, executed from that script). A suite that ran zero
   checks or a missing catala installation fails. The `gates.yml` job (maintainer decision 2026-08-24) runs it where
   catala is on the bare PATH or in the runner's opam switch and skips loudly elsewhere. Floors:
   `EXPECTED_MIN_CHECKS`, `EXPECTED_MIN_GOLDENS`.

10. `scripts/check-regelspraak.sh` — the RegelSpraak frontend (`frontends/regelspraak`, jar-only: JDK 21+, ANTLR and
    JUnit jars fetched into a gitignored `lib/`) builds and its JUnit suite passes; every golden carrying a
    `source.rgs` re-emits through `--ir` byte-identically. The frontend renders Dutch by default; its Makefile copies
    `src/main/resources` to `out/`. Floors: `EXPECTED_MIN_GOLDENS`, `EXPECTED_MIN_TESTS`. It first calls
    `scripts/check-rgs-codes.sh`: every message the frontend raises, and the three the Drools backend raises itself,
    carries a code `RGS-###` and named parameters (`Diagnostic(severity, code, params, message, line)`; the code is
    not on the wire) from one register,
    `frontends/regelspraak/src/main/resources/dev/uchar/regelspraak/diagnostic-codes.tsv`, held to the sources in
    both directions (codes passed and registered, parameter names agreeing, no three-argument `new Diagnostic(`
    outside `IrLoader`). The two catalogues under `.../regelspraak/message/` (Dutch, English) are held to the
    register: every code has an entry in both languages, every entry belongs to a code, part, frame or shared part,
    the places of an entry are parameters of its code, every brace is a place or doubled; a site passes code and
    parameters and no text (the argument count of every call form), also as `new Msg(Component.FRONTEND, code,
    params(...))` for an exception text a user sees. An English entry carries no Dutch function word except a listed
    (entry, word) pair. A Dutch entry equal to its English sister fails unless it has no word outside its places and
    quotations or `SAME_IN_BOTH` lists it; a Dutch entry with an English function word fails unless
    `ENGLISH_EXCEPTIONS` lists entry and word; all three lists are reason-carrying and stale-failing. Floors:
    `EXPECTED_MIN_RGS_CODES`, `EXPECTED_MIN_RGS_MESSAGES`, `EXPECTED_MIN_RGS_SAME_IN_BOTH`.

11. `scripts/check-toka-parity.sh` — the `toka/` parity zone (see Corpus rules). The frontend builds and the TOKA
    tests under `toka/` (two integration tests in original form and the semantic-token witness of the casus) compile
    and pass against it; every toolchain-pinned `toka/**/ir.json` re-emits through uchar's frontend byte-identically.
    This is a regression form: the zone migrates with every wire landing (the casus dump re-emitted by the frontend,
    the slices through the landing's migration script), so the comparison proves frontend ≡ the recorded state;
    `toka/README.md` lists each landing under its deliberate divergences. `HOLDOUTS` (casus quirks and fragments
    where uchar deliberately diverges from the toolchain) is reason-carrying and stale-failing. A well-formedness leg
    holds every replay slice verifier-clean (the workspace `_build` verifier), with `VERIFIER_HOLDOUTS`
    reason-carrying and stale-failing; the casus dump stays outside it (its scenarios write the same targets each in
    their own way). Floors: `EXPECTED_MIN_CASES`, `EXPECTED_MIN_FRAGMENTS`, `EXPECTED_MIN_TESTS`,
    `EXPECTED_MIN_CLEAN`.

12. `scripts/check-drools.sh` — the Drools differential leg of the square (maintainer decision 2026-08-27). The
    backend (`backends/drools`, a shaded Maven runtime uber-jar, jar-only) loads every golden's `ir.json` off the
    wire — never the RegelSpraak source, which serves only as an internal test comparison — builds a KieBase through
    the exec-model branch, the lowering with truth maintenance (`transpiler/tms`), and replays every `case*.json`. A
    played golden replays with exactly the expectations gates 5 and 6 verify: a played pass is ref ≡ clingo ≡ Drools.
    What the lowering carries is in `backends/drools/README.md`. A form it does not carry is refused with a code
    `DROOLS-###` from one register (`backends/drools/.../refusal/DroolsCode.java`) at the head of its report
    (`EXCLUDE DROOLS-###: <sentence>`), grouped in the gate report. `EXPECTED_CHANNEL` lists every non-played golden
    by name with its reason (played is the default), each `EXCLUDE` pin with its code: a golden that shifts channel,
    a changed or missing code (both codes shown) and a stale pin fail by golden name. `EXCLUDE_KNOWN`, the
    value-divergence list (a golden whose fired outcome differs from ref/clingo), is reason-carrying and
    stale-failing. `EXPECTED_PLAYED` is an equality and catches a newly arrived player. The DRL-text branch is the
    second rendering of the same analysis (a plan per rule, rendered as PatternDSL and as DRL text); per golden its
    status line must equal the exec-model branch's verbatim, with no list or pin of its own, any other line failing
    by name. The replay runs in two passes, exec-model then DRL text, the second only after the first is green. The
    jar is taken fresh or rebuilt (`scripts/lib/drools-jar.sh`), and the backend's JUnit suite runs with
    `mvn -o test` on the same `target/` and must pass with a non-zero, zero-skip count (maintainer decisions
    2026-08-30 and 2026-10-25); surefire runs it in `forkCount` 0.5C JVMs with `reuseForks`
    (`backends/drools/pom.xml`), the pom's system property and environment hold in every fork, and no test class
    relies on state another class leaves in its JVM. The gate fails by name when the suite did not run
    `BranchDumpsCorpusWitnessTest` (the two branches compared case by case over every golden and red case, the
    `reading` section included), `ReadingRulesFixpointWitnessTest`, `ReadingsCorpusWitnessTest` or
    `ReadingRulesObserverWitnessTest` — the witnesses of the build with readings
    (`DroolsBackend.build(…, readings)`, read by `readingsOf`; a displaced default reads `DISPLACED`), whose
    production build is byte-equal to the one without; their floors are in those classes. The backend renders
    English by default; its suite runs under `uchar.messages.locale=nl`. Floor: `EXPECTED_MIN_TESTS`. It first calls
    `scripts/check-drools-codes.sh`: the register held to the sources in both directions (every code raised except
    those of kind `RETIRED`, which no place names and which keep their number; numbers without gap; keys unique;
    every `ef` a row of RUNTIME-SEMANTICS ch. 5.7b), and the catalogues under `backends/drools/.../message/` held to
    the register as in gate 10, with `EF_PLACES` for the places of a fault kind; a site names a code as
    `DroolsCode.X.reason(name, value, ...)` or `.part(...)`. `SAME_IN_BOTH` (the fault-kind sentences the runtime
    writes and `ComputeFault` reads back, sentences left English, the names of parts) and `ENGLISH_EXCEPTIONS` are
    reason-carrying and stale-failing. Floors: `EXPECTED_MIN_DROOLS_CODES`, `EXPECTED_MIN_DROOLS_MESSAGES`,
    `EXPECTED_MIN_DROOLS_SAME_IN_BOTH`.

13. `scripts/check-reftype-derivation.sh` — annotation indifference (maintainer decision 2026-09-04): consumers derive
    units from declarations and the effective-type law (RUNTIME-SEMANTICS §2.10), never from the wire annotation.
    UNIT leg: for every golden with case files, every unit-bearing `type` annotation on a derived (composite)
    position — a `percentageOf` included (ch. 2.6 rule 7); reference positions carry no `type` — is twisted
    deterministically (a convertible sister unit, or decimals+1), and ref, clingo and both Drools branches must
    reproduce the pinned outcomes (for a golden in gate 6's refusal channel, clingo's refusal of the untouched run
    byte for byte), each leg against its own untouched status line; the DRL-text leg's untouched line equals the
    exec-model branch's and it plays as many twistable goldens, never zero. Exemption positions (the Literal lexeme,
    the duration operand of date arithmetic, the `duration` builtin's own unit) are named in the script; a
    bookkeeping clamp fails when a unit-bearing annotation appears outside the twist table (the §2.10 fourth-class
    witness); `SURVIVORS` is reason-carrying and stale-failing. VOCABULARY leg: every golden with `vocabularies` and
    case files replays with the side table stripped, the vocabularies exchanged and the designations rotated within
    each scope of WF-118 (IR-SPEC ch. 2.12). ORIGIN leg: every golden with an `origin` (ch. 2.13) replays with it
    stripped. NAME leg: every golden whose defaults carry a `name` (ch. 6.1) replays with every name and every
    `statuteAnnotations` of a Default stripped. In those three legs the four executors must reproduce the untouched
    outcome, with no survivor list. The four legs run as four processes; a Drools run on an untouched golden that
    ends in no status line (`PLAYED`, `FAIL`, `EXCLUDE`, `ERROR`) fails by golden, leg and executor. Floors:
    `EXPECTED_MIN_TWISTS`, `EXPECTED_MIN_VOCABULARY_GOLDENS`, `EXPECTED_MIN_VOCABULARY_STRIPPED`,
    `EXPECTED_MIN_VOCABULARY_ROTATED`, `EXPECTED_MIN_ORIGIN_GOLDENS`, `EXPECTED_MIN_ORIGIN_STRIPPED`,
    `EXPECTED_MIN_NAME_GOLDENS`, `EXPECTED_MIN_NAME_STRIPPED`, `EXPECTED_MIN_ANNOTATIONS_STRIPPED`.

14. `scripts/check-flogic.sh` — the F-logic frontend (`frontends/flogic`, plain OCaml in the workspace dune build, no
    external toolchain) builds and its suite passes: it emits every test program, holds it verifier-clean, replays
    its cases through ref and clingo, and drives one fixture per refusal, checking the reason and the citation of the
    mandate document. Every golden carrying a `source.flogic` re-emits byte-identically after canonicalisation (gate
    2's serialiser). Zero checks or a missing dune fails. Floors: `EXPECTED_MIN_CHECKS`, `EXPECTED_MIN_GOLDENS`.

15. `scripts/check-graph.sh` — the rule graph of the Drools backend (`backends/drools/.../graph/`, the reading of the
    IR the distribution's scope analysis consumes) holds to IR-SPEC ch. 5. SPEC leg: per golden the graph, built from
    `ir.json` through the loader, is compared with the verifier's ch. 5 edges (`verifier --dependency-edges` from the
    workspace `_build`, the list WF-045/046 walk) over attribute and characteristic nodes, whatever forms join them,
    without the edge kind and the node's type (the graph keys a member on its declaring type); edges over a
    population, relation or channel are counted per class and reported, not compared; a difference fails unless
    `DELIBERATE_DIFFERENCE` lists it (one projected line with both lists and the reason, stale-failing in both
    directions); a golden the loader refuses is reported with its form. ROUTE leg: on every golden with a
    `source.rgs` the graph from the source route equals the one from the wire byte for byte, and the graph's unit
    tests (`backends/drools/src/source-route`, compiled by the gate against the frontend and the jar) pass. Floors:
    `EXPECTED_MIN_GOLDENS`, `EXPECTED_MIN_DEPENDENT`, `EXPECTED_MIN_SOURCE_ROUTE`, `EXPECTED_MIN_TESTS`.

16. `scripts/check-scenario.sh` — the scenariotoetsen extension of the RegelSpraak frontend
    (`frontends/regelspraak/grammar/DIVERGENCES.md`, "Scenariotoetsen"): every golden carrying a `scenario.rgs`
    beside its `source.rgs` regenerates through `--cases` exactly its pinned case files (same names, none missing or
    extra) byte-identically, and its `ir.json` byte-identically (without a scope the production route's document,
    with one the forms the scope carries); the handwritten expectations are the anchor. Gate 4 checks the schema of
    those case files, gates 5, 6 and 12 replay them. Floors: `EXPECTED_MIN_SCENARIO_GOLDENS`,
    `EXPECTED_MIN_SCENARIO_CASES`.

17. `scripts/check-readings.sh` — the reading of a condition at the fixpoint (RUNTIME-SEMANTICS ch. 5.12, which
    defines the canonical text, timeline targets, constructor values and displaced defaults included). The ref
    trace (`runner --trace`, `ref/lib/reading.ml`: per form with a condition and object it runs on, the condition
    evaluated on the fixpoint world through an observer, per node) and the backend's readings
    (`DroolsBackendMain --readings`) are held equal per golden case file, on both branches; the two share no code
    (gate 1). The trace reads every golden case file; per golden both branches end in `READINGS <n>`, n its case
    files, or in the `EXCLUDE` line the plain replay prints; an `ERROR` fails unless `READINGS_ERROR_KNOWN` lists
    it. Every line is compared verbatim — the object a role path reached (`on "<object>"`, `on none`) and a
    `DISPLACED` default's node lines included — except the position of a contributor of a quantifier fold: member
    and verdict, value or fault are compared verbatim, the position (`reached`/`unreached`) where both write one
    (`unordered` where the session keeps no order), and where the backend writes `unordered` no contributor of the
    trace may carry a fault (a tripwire, held on a listed case too). A difference fails unless `DEVIATIONS` names case
    and branch; both lists are reason-carrying and stale-failing in both directions, and the comparison rule, the
    tripwire, the object reader and the displaced counter pass rows of their own. Traces run on one process per
    core. Floors: `EXPECTED_MIN_TRACED_CASES`, `EXPECTED_MIN_COMPARED_GOLDENS`, `EXPECTED_MIN_COMPARED_CASES`,
    `EXPECTED_MIN_READINGS`, `EXPECTED_MIN_NODE_LINES`, `EXPECTED_MIN_UNREACHED`, `EXPECTED_MIN_CONTRIBUTOR_LINES`,
    `EXPECTED_MIN_TIMELINE_LINES`, `EXPECTED_MIN_UNREACHED_CONTRIBUTORS`, `EXPECTED_MIN_OBJECT_LINES`,
    `EXPECTED_MIN_DISPLACED_NODE_LINES`.

**Counting floors (maintainer decision 2026-09-09).** Beside the non-empty checks and the name-carrying lists, every
gate whose count could shrink unseen carries an `EXPECTED_MIN_<name>` constant in its own script, with its reason and
measurement date, and fails naming both the measured number and the floor. Floors are lower bounds: they catch shrink,
and growth needs no bump, so an ordinary corpus card touches no protected script. Raising a floor is a deliberate act;
a deliberate removal lowers it in the same commit. The accepted weakness: a count that stays equal while its content
shifted passes; the name-carrying lists cover that direction. Gate 12's `EXPECTED_PLAYED` stays an equality (decision
2026-09-06). A gate that gets no floor carries one sentence in its script saying why.

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
  constructors and functions, ASP predicates, shell variables, CLI flags
  and test names; so is every file and directory name. Existing card
  files in `backlog/new/` keep their Dutch names; a new card follows the
  rule.
- Where the wire has a name for a concept, the code uses it
  (`emptyFallback`, not `leegwaarde`); otherwise the term of the English
  spec text (`spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`). This holds
  for the classes of the resolved model that stand for a RegelSpraak
  construct too (`ObjectType`, `FactType`, `Datatype.Numeric`; owner's
  decision 2026-10-19): the language is what the RegelSpraak text spells,
  not the model that holds it. A few fixed
  choices: `fout` is error for a parse, load or document error and fault
  for an evaluation fault; `voorwaarde` is condition; `soort` is kind;
  `terugzetten` is write back. The renaming programme of 2026-10-17/19 and
  its tool are in the git history (`backlog/done/20261017-engelse-namen-afspraak.md`
  and the cards after it).
- Dutch, as authored, and nothing else (a name outside this list is
  English, also when it is spelled like one of them or was read by a
  test; owner's decisions 2026-10-18 and 2026-10-19):
  1. domain content: the Jutterswet, declared names, RegelSpraak sources,
     case labels, the `jutterswet-*` names, the golden slugs `synth*` (names
     from the synthetic casus, which is own domain content) and `ww-*` (the
     WW, a Dutch statute; both owner's decision on card `rename-paths`,
     2026-10-19), and a case file name that carries such a label
     (`case2-leeg-lijst.json`);
  2. RegelSpraak keywords: the lexer's literals and the token names that
     spell them (`NEGATIEF`, `GEEN`);
  3. the parser rules and labels of the RegelSpraak grammar and what ANTLR
     generates from them: `FeitTypeNaamContext`, `ctx.feitTypeNaam()`,
     `visitFeitTypeNaam`. A plain Java field, record component, method
     or variable that happens to be spelled like a rule (`tekst`,
     `periode`, `naam`) is English. The grammar has a few inherited mixed
     rule names (`objectTypeDefinition` beside `objectTypeExtensie`); they
     stay, because an ANTLR name is not renamed;
  4. enum constants that are the v0 spelling and reach `name()` in a
     message, a dump or the wire: the families of `ir/Spelling.java` (the
     section `waarden` of `spec/proposals/v1-naamtabel.json`, compared by
     `SpellingTest`: functions, comparisons, rule kinds, `Number.sign`),
     `ResolvedRule.Kind`, the predicate kinds, and the values of
     `Designatable.Kind` and their mirrors in `backends/drools`
     (`FactModelGenerator.Origin.Kind`, `TestItem.Role`), which the
     distribution writes into its JSON;
  5. what the production runtime reads (below);
  6. the variable names in the DRL text the Drools backend generates
     (`$houder`, `uitkomst`): the text is the output of the plan, and the
     tests pin it.
- Not renamed, each for its reason: the production runtime under
  `backends/drools/src/main/java/dev/uchar/regelspraak/runtime/` (gate 1
  recognises a copy by its names) and the members of `backends/drools`
  that it reads (`Namen`, `Namen.lidnamen`, the getters of `Period` it
  looks up by reflection); the generated fact-model classes `Inconsistentie`
  and `Segment(van, tot, waarde)` (`schema/FactModelGenerator.java`), which
  the runtime builds and reads by name; the 47 TOKA fragments
  `toka/casus/toka-*.rgs`, which stay verbatim (the three tests under
  `toka/src/test/java` are own work and follow every rename
  mechanically); v0 names where they stand as
  v0 names (IR-SPEC "v0 name:", CONSTRUCTS, `spec/proposals/`); git
  history and `backlog/done/`. Messages and prose that quote a RegelSpraak
  term, a v0 wire name or a Dutch diagnostic stay as written.
- Working prose — cards, Results, commit messages, `spec/proposals/` — is
  Dutch or English; every identifier it names or introduces is English,
  also when a card prescribes a new class.
- Smallest change that satisfies the task; prefer editing existing files;
  fail fast — no silent fallbacks or swallowed errors.
