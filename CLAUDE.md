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
  only. No ©-Belastingdienst material enters this repo: no TOKA casus, no
  RegelSpraak specification excerpts. Programs *written in* RegelSpraak are
  ours; the language specification text is not.
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
   `corpus/negatief`, `spec/schema`, `spec/verifier`, `ref`, `backends`)
   carry no machine-local paths or private-repo references, and corpus data
   is free of TOKA material. Working prose (plan, backlog, reviews) may
   reference the private source repos during development — see
   `WAY-OF-WORKING.md` §5–6.
2. `scripts/check-ir-schema.sh` — the schema file is metaschema-valid;
   every `corpus/golden/**/ir.json` validates against
   `spec/schema/regelspraak-ir-v0.schema.json` and round-trips
   byte-identically; an unpopulated corpus is reported explicitly, never
   silently green.
3. `scripts/check-verifier.sh` — the OCaml verifier (`spec/verifier/`)
   builds; every `corpus/golden/**/ir.json` is verifier-clean; every
   `corpus/negatief/<wf###-slug>/ir.json` is rejected for exactly the WF
   rule its directory name declares; an empty negatief corpus is a
   failure, never silently green.

Standing harness guards once
the corpus runs in CI: oracle (interpreter replay passes), no-op (an identity
transpiler must fail), unsolved-state (`Onopgelost` surfaces in output, never
vanishes).

## Repo relationships

- `regelspraak-java` (separate repository) is the first frontend + Drools
  backend. It consumes this repo at a pinned `SPEC_REV`; changes flow
  **spec-first**: the delta lands here (spec + corpus + schema), then a
  pin-bump conformance PR lands there.
- Its `RegelSpraakRuntime` is production semantics. `ref/` must share no code
  with it — separate repositories make this structural; no copy-paste imports
  either.

## Conventions

- Infrastructure, spec metamodel, and node names: English. Domain content
  (Jutterswet articles, declared names in corpus sources): Dutch, as
  authored.
- Smallest change that satisfies the task; prefer editing existing files;
  fail fast — no silent fallbacks or swallowed errors.
