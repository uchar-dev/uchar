# Clingo real-world 4/4 — allocation/verdeling to ref parity

Goal: the heaviest clingo gap from the TOKA stress test (6 scenarios):
"allocatie/verdeling nog niet volledig vertaald". The S19 inventory
names non-proportional and conditional allocation and misaligned
allocation among the loud faults. Bring `backends/clingo` to ref parity
on the allocation forms `ref/` actually carries (RUNTIME-SEMANTICS
allocation chapter: recurrence over groups, maximum/rounding/remainder),
corpus-borne with own-authored Jutterswet goldens. Where ref itself
refuses a form, clingo keeps its loud fault — do not out-run the
reference. Ref is the meaning: no `ref/` changes, no semantics beyond
ref; the narrowest change that reaches parity; S19 updated, never
renumbered (S19 stays highest).

Acceptance:
- Red-first, shown in the Result: per newly covered allocation form a
  new own-authored Jutterswet golden (with cases pinning absolute
  values) that ref replays green and today's clingo refuses loudly
  (fault text quoted); after the change gate 6 agrees.
- Gates 1–9 all green (incl. gate 7 noop discipline; allocation cases
  are prime noop-refuters — justify any survivor).
- `spec/ASP-TRANSLATION.md` documents the allocation translation; S19
  shrinks.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*` and `corpus/golden/anchor/jutterswet-*`
(new own-authored goldens), `scripts/check-noop.sh` (survivor list only,
with reasons).
Out of scope: `ref/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `spec/CONSTRUCTS.md` (do NOT touch),
condition kinds and windows (previous cards in this chain).
