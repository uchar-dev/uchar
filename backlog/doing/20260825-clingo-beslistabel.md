# Clingo real-world 3/4 — decision-table / disjunctive condition kinds

Goal: the TOKA stress test's "decision-table / disjunctive condition
kind" refusal class (3 scenarios): decision tables desugar upstream
(`BeslistabelDesugar`), but the desugared v0 wire carries condition
shapes — disjunctive/composite condition kinds among them — that
`backends/clingo` refuses while `ref/` evaluates them. Investigate the
exact wire shapes first (the S19 inventory names "the ten-minste-1
composite union", "the static-comparison composite member", "disjunctive
condition kinds" — read `backends/clingo/lib/translate.ml` reason strings
and the stress-test report `backlog/done/20260825-toka-stresstest.md`),
then bring clingo to ref parity on the forms ref carries, corpus-borne.
Ref is the meaning: no `ref/` changes, no semantics beyond ref; loud
faults for whatever remains; S19 updated, never renumbered.

Acceptance:
- Red-first, shown in the Result: per newly covered condition shape a new
  own-authored Jutterswet golden that ref replays green and today's
  clingo refuses loudly (fault text quoted); after the change gate 6
  agrees.
- Gates 1–9 all green (incl. gate 7 noop discipline).
- `spec/ASP-TRANSLATION.md` documents the translation; S19 shrinks.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*` (new own-authored goldens),
`scripts/check-noop.sh` (survivor list only, with reasons).
Out of scope: `ref/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `spec/CONSTRUCTS.md` (do NOT touch),
allocation (next card), validity windows (previous card).
