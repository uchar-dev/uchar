# Clingo real-world 1/4 — text/enum/boolean/date positions

Goal: shrink the S19 loud-refusal inventory (`spec/ASP-TRANSLATION.md`)
where `ref/` already carries the semantics: non-numeric scalar equality
(text/enum/boolean `Gelijk`/`Ongelijk`, RUNTIME-SEMANTICS ch. 1.2d) and
text/enum/boolean/date literals and reads in the positions ref evaluates.
The TOKA stress test scored clingo 4/30 with "unscalable datatype
(tekst/enum/datum-tijd)" as a 4-scenario refusal class; this card brings
clingo to ref parity on those forms over the corpus. Suggested approach:
a symbolic-term encoding (ASP constants/strings) beside the existing
integer encoding — but the design is the builder's, the invariant is not:
**ref is the meaning; do not touch `ref/`; do not widen semantics beyond
what ref does** (the narrowest change that reaches parity). Every form
that stays untranslated keeps or gains a loud fault, and S19 is updated
to match (S19 remains the highest-numbered section — never renumber).

Acceptance:
- Red-first, shown in the Result: for each newly covered form, a new
  own-authored Jutterswet golden (with case files) that ref replays green
  and today's clingo refuses loudly (fault text quoted), then translates
  and agrees after the change.
- Gates 1–9 all green, including gate 6 (ref ≡ clingo over the whole
  corpus) and gate 7 (new goldens noop-refuted, or listed with reason in
  the survivor list — justify any survivor).
- `spec/ASP-TRANSLATION.md` documents the new translation (its own S
  section or an existing one, per the file's conventions) and S19's
  inventory shrinks accordingly.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*` (new own-authored goldens only),
`scripts/check-noop.sh` (survivor list only, with reasons).
Out of scope: `ref/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `spec/CONSTRUCTS.md` (registry notes
land serially at merge time — do NOT touch it), allocation, validity
windows, decision tables (later cards in this chain).
