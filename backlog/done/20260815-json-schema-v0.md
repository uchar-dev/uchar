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

Result: `spec/schema/regelspraak-ir-v0.schema.json` (draft 2020-12, 61
$defs, strict: closed objects, required keys, discriminated unions on the
`node` field, enums pinned to the exact v0 constants, nullability exactly
where the emitter can emit null) plus gate 2,
`scripts/check-ir-schema.sh`: metaschema check, validation of every
`corpus/golden/**/ir.json`, and a byte-identical round-trip (raw number
lexemes preserved; layout matched to the emitter's writer). Verified
against a real dump generated from the merged emitter (pin `c51652a`; the
dump is TOKA-derived and stayed outside this repo): accept + round-trip
byte-identical; three mutations rejected with useful errors; red-then-green
replayed through the real corpus path (planted invalid
`corpus/golden/**/ir.json` fails the gate, cleanup restores green); an
empty corpus is reported explicitly, never silently green. Graduation
done: `spec/schema/README.md` swept self-contained and its exception
removed from `check-self-contained.sh` (the list only shrinks). Beyond the
card's may-touch, flagged for review: the `CLAUDE.md` gate list gains gate
2 in this same proposal (the constitution must not lag the gate set — the
established same-merge pattern). Known limitation, recorded: node kinds
not exercised by the test dump are schema'd from the emitter source, not
from observed data — the Jutterswet golden corpus will exercise them; a
supplementary-plane character would fail the round-trip loudly rather than
pass silently. This branch is a PROPOSAL: `spec/schema/`, `scripts/`, and
`CLAUDE.md` are protected paths — the maintainer merges (fast-forward).
