# ref/: reference interpreter — bootstrap + oracle gate

Goal: `ref/` as an OCaml dune project that reuses the `ir_v0` loader from
`spec/verifier/` without code duplication (dune workspace at the repo root;
`check-verifier.sh` keeps building with `--root spec/verifier`, unaffected).
The interpreter implements: exact rational arithmetic (zarith), the
empty-value discipline of RUNTIME-SEMANTICS ch. 1, §6 arithmetic and
rounding per ch. 2, the allocation recurrence per ch. 4.2, and fixpoint
evaluation with the §9.5 inconsistency channel per ch. 5. A case runner
loads `uchar-case-v0` files (`corpus/golden/**/case*.json`), performs the
cross-checks the case schema assigns to the ref loader (label uniqueness,
role targets, declared names against the ir.json), evaluates, and compares
expectations exactly. The worked examples of ch. 1–2 land verbatim as unit
tests. Gate 5 (oracle, PLAN §8): every case file must replay exactly;
goldens without case files are reported explicitly, never silently green.

Acceptance:
- `sh scripts/check-oracle.sh` green: builds `ref/`, replays every
  `corpus/golden/**/case*.json` with exact expectation match, lists goldens
  that have no case file; shown red (before the interpreter exists or with
  a deliberately broken expectation) and then green.
- `dune build @ref/runtest` (from the repo root) green: ch. 1–2 worked
  examples as unit tests.
- All existing gates stay green; CI gets a gate-5 job.

May touch: `ref/**` (new), `dune-workspace` (new, root), `.gitignore`
(root `_build/`), `scripts/check-oracle.sh` (new),
`scripts/check-self-contained.sh` (only dropping the `ref/README.md`
exception — the graduation the gate itself prescribes for a zone's first
real delivery), `.github/workflows/gates.yml`, `CLAUDE.md` (gate list),
`backlog/**`,
`corpus/golden/jutterswet-vondsttelling/case*.json` (new, only if that
golden evaluates time-free), `corpus/golden/README.md` (case table note),
`spec/verifier/dune-project` + `spec/verifier/lib/dune` (build metadata
only: a `public_name` so `ref/` can link `ir_v0` across projects).

Out of scope: timeline algebra (ch. 3) in `ref/`; a penningjaar case (needs
timelines); ch. 6 stratification; any code change to `spec/verifier/`;
any change to `spec/RUNTIME-SEMANTICS.md`.

Result: delivered in full, plus a verification round. ref/ stands as an
OCaml dune project (root workspace; ir_v0 linked via public name
uchar-ir-v0; check-verifier.sh untouched thanks to --root isolation):
value domain, ch. 1 empty discipline + ch. 2 exact arithmetic/rounding/
units, the ch. 4.2 allocation recurrence as a pure function, the ch. 5
fixpoint session with v0's single negation boundary, and the
uchar-case-v0 loader with its cross-checks. Gate 5 shown red, then
green: 6 cases replay exactly; the penningjaar golden is listed as
not-replayed (needs ch. 3). New corpus case: jutterswet-vondsttelling
(count-over-role wart, empty-source fallback). Unit tests: ch. 1-2
worked examples verbatim + the ch. 4.6 allocation examples. CI gained
the gate-5 job; CLAUDE.md gate list extended; gate 1 graduated
ref/README.md out of its exception list. Verification (3 adversarial
opus lenses) found 3 majors + 2 minors, all fixed with regression
tests: static routing of the ch. 1.2d equality asymmetry, exact decimal
reading of unit factors, strict number-lexeme validation, exponent
overflow as a fault, enum identity via norm. Deliberately left: ch. 3
timelines and ch. 6 stratification (cards cut in backlog/new/);
"is gevuurd" (row 30 OPEN). Erratum candidate flagged for the
maintainer, document untouched: the ch. 4.6 cap-degrade example says
"one §9.5 error" where ch. 4.3 + 5.6 imply one per degraded cap (two).
