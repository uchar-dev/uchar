# JSON schema v0 + validation gate (proposal)

Goal: First cut of `spec/schema/` mirroring the v0 `--ir` dump, plus the
repo's second gate: every `corpus/golden/**/ir.json` validates against the
schema and round-trips (load → emit → byte-identical canonical form).
Includes sweeping `spec/schema/README.md` clean and dropping its exception
from `scripts/check-self-contained.sh` (the graduation rule,
`WAY-OF-WORKING.md` §6).

Acceptance: schema validates the first emitted corpus files; the round-trip
gate shown red then green; the README exception removed;
`scripts/check-self-contained.sh` passes.

Depends on: the `--ir` emitter in `regelspraak-java` (cross-repo; spec-first:
the schema shape is decided here, the emitter conforms there). Blocked until
that emitter exists.

May touch: `spec/schema/` and `scripts/` (protected paths — proposal; the
maintainer merges), `backlog/`

Out of scope: v1 deviations (the schema mirrors v0 first); `spec/verifier/`.

Unblocked 2026-08-15: the emitter merged in regelspraak-java (main
b85dff3 → c51652a); the dump-producing pin is c51652a. Review citations
remain pinned at b85dff3 (contained in that history).
