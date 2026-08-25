# Clingo real-world 2/4 — validity-window gating

Goal: translate validity windows (`geldigheid` on rules, ladders,
creations — RUNTIME-SEMANTICS ch. 8.2 runtime gating against `asOfDate`)
in `backends/clingo`, removing the S19 "validity windows (rules, ladders,
creations)" refusal where ref carries the semantics. Today
`jutterswet-vormenkabinet` is the only window-carrying corpus document
and is verifier-only by its own NOTE (no case replays it); the TOKA
stress test hit the window refusal on a live scenario. This card builds
the translation machinery plus at least one *replaying* window-carrying
golden: a rule with a bounded window, an `asOfDate` case inside the
window (rule fires) — the full two-dated-versions witness is a separate
follow-up card (20260825-versie-gating-getuige); keep this one narrow.
Ref is the meaning: no `ref/` changes, no semantics beyond ref. Whatever
stays untranslated keeps a loud fault; S19 updated, never renumbered
(S19 stays highest).

Acceptance:
- Red-first, shown in the Result: the new window-carrying golden replays
  green in ref while today's clingo refuses loudly (fault text quoted);
  after the change gate 6 agrees on it.
- Gates 1–9 all green (incl. gate 7 noop discipline for new cases).
- `spec/ASP-TRANSLATION.md` documents the window gating; S19 shrinks.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*` or `corpus/golden/anchor/jutterswet-*`
(new own-authored goldens), `scripts/check-noop.sh` (survivor list only,
with reasons).
Out of scope: `ref/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `spec/CONSTRUCTS.md` (do NOT touch),
the two-versions gating witness (follow-up card), decision tables,
allocation.
