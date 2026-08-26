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

Result (landed): WF-084 — the next free number after WF-083 — added to
`spec/IR-SPEC.md` beside WF-081's numeric grammar (ch. 4.1) with its
normative ISO grammar and index row: a day-precision date literal's
lexeme MUST be the strict ISO form `yyyy-mm-dd` (fixed field widths,
month 1–12, day 1..days-in-month, leap-aware). Verifier: the
`lexeme_iso_date` predicate + a WF-084 arm in the Literal check
(`spec/verifier/lib/checks.ml`). Executors: `ref/lib/value.ml`
`date_of_iso` hardened (fixed widths + range + leap); the clingo backend
reuses that same reader (`uchar-ref`'s `Value`), so both executors fault
loudly with ONE change — no separate `backends/clingo/` edit was needed.
Negatief: `wf084-bare-non-iso` (`31-01-2024`, the day-first form ref +
clingo silently misread as `0036-07-16`) and `wf084-source-snippet`
(`dd. 31-01-2024`, the raw snippet form). New emitter golden
`jutterswet-vondstverjaring` (an ISO date literal `2024-01-31`, source
snippet `31-01-2024` kept in `bron`), replayed exactly by ref and
clingo. Red-first shown: pre-fix emitter emitted the snippet; ref
computed `0036-07-16` and clingo the identical civil day `-706182`
(differential-blind); post-hardening both fault
("malformed date lexeme"). All nine uchar gates green.

Toolchain (separate repo `regelspraak-java`, one-time write mandate, NO
SPEC_REV commit): `ExpressieBouwer` emits the bare ISO waarde for a
deDato literal (snippet in `bron`); `Literalen.parseDatum` made the
single dual-format reader (ISO waarde + day-first snippet) so the
Drools/ExecModel back-ends read the emitted ISO waarde — a required
companion to the emitter change, without which `make test` was red; two
pinning `IrEmitterTest` cases. `make test` green (555 tests);
`uchar-conformance` 18/19 goldens byte-identical INCLUDING the new one.
The single divergence (`jutterswet-kaveltelling`, a `bronRol`
fold-source field) is pre-existing drift between the toolchain `main`
and this card's corpus base — reproduced with my changes stashed, wholly
unrelated to WF-084. Datetime literals left day-first (out of scope).
