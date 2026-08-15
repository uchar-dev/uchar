# Seed the backlog

Goal: Create the coordination ledger `WAY-OF-WORKING.md` §2 prescribes —
`backlog/{new,doing,done}` — and cut the first session-sized cards from the
Phase 0–1 exit criteria in `spec/PLAN.md` §6.

Acceptance: `backlog/` exists with the initial card set; every card has
Goal/Acceptance/May touch/Out of scope; `scripts/check-self-contained.sh`
passes.

May touch: `backlog/`

Out of scope: any `spec/` or `corpus/` content; working any of the cards.

Result: Seeded `backlog/{new,doing,done}` with seven cards: the in-repo
Phase-0 deliverable (constructs inventory), the corpus workstream (Jutterswet
statute text), three Phase-1 openers (IR-SPEC v0 mirror of the model layer,
RUNTIME-SEMANTICS ch. 1–2, JSON schema v0), the early ASP hand-translation
that PLAN §6 wants started during Phase 1, and CI bootstrap. Cross-repo work
(source anchoring, the `--ir` emitter — both land in `regelspraak-java`) is
recorded as dependency notes on the cards that need it, not as cards here.
