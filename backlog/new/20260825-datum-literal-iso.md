# A3 — the date-literal lexeme pin (ISO on the wire) + emitter fix

Goal: the WF-081 sibling with a PROVEN silent miscalculation route: the
emitter writes date literals as the raw source snippet
(`"waarde": "dd. 31-01-2024"` — IrEmitter.java:318 emits the bron
snippet) while both executors parse only ISO; worse, a bare
"31-01-2024" is silently read as ISO year 31 (ref evaluated it to
0036-07-16 in the exploration probe) and clingo would follow exactly —
differential-blind, wrong answer, no fault. Maintainer decision
(2026-08-25): full fix under a NEW one-time toolchain write mandate,
the WF-081/1-A protocol: (1) a new WF rule (next free number after the
chain's earlier claims — check the index): a date(-time) literal lexeme
MUST be the ISO form the executors parse (specify the grammar
normatively in IR-SPEC beside WF-081's numeric grammar), with negatief
case(s) pinning the snippet form AND the bare non-ISO form; (2) strict
`date_of_iso` in BOTH executors: format and range validation (month
1–12, day 1–month length, fixed field widths) — a malformed lexeme
faults loudly instead of misparsing; (3) toolchain: the emitter writes
the bare ISO lexeme (the resolver already holds the parsed date; keep
the snippet in `bron`), mirror of the unit-literal fix in
ExpressieBouwer/IrEmitter; two pinning emitter tests; make test +
uchar-conformance green (verify whether any existing emitter golden
carries a date literal — the exploration says none does; if one does it
moves spec-first); (4) an emitter golden whose source carries a date
literal, replaying through both executors, closing the gate-blind spot.
NO SPEC_REV commit (the maintainer re-pins after landing).

Acceptance:
- Red-first: today's emitter output on a date-literal source violates
  the new WF rule (quoted), and the bare "31-01-2024" silent-misparse
  is demonstrated pre-fix (quoted) and faults loudly post-fix in both
  executors.
- Toolchain `make test` green; conformance byte-identical on all
  existing emitter goldens plus the new one; all nine uchar gates green.

May touch: `spec/IR-SPEC.md` (WF rule + index), `spec/verifier/`,
`ref/` (value.ml date_of_iso hardening), `backends/clingo/` (same),
`corpus/negatief/wf###-*`, `corpus/golden/<new emitter golden>`;
toolchain emitter + its tests (one-time mandate, NO SPEC_REV commit).
Out of scope: datetime lexemes beyond mirroring the same ISO discipline
(keep the change narrow), `spec/CONSTRUCTS.md` (serial at merge).
