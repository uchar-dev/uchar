# backends/clingo — the ASP backend (Phase 3)

Not started. Preceded by the paper exercise it exists to validate: the
translation of the core IR into stratified Datalog with aggregates
(ASP-Core-2), documented as `../../spec/ASP-TRANSLATION.md`. The hand
translation of one representative rule starts *during* Phase 1 — a spec
written without a second, alien target breaks at first contact.

If the anchor is right, this generator is near a pretty-printer for the
initial subset: assignment, characteristic attribution, the initialisation
default, one role membership, one timeline. With it, differential testing
becomes a square — Drools ≡ reference interpreter ≡ Clingo over the golden
corpus — and every divergence is by definition a spec bug.

Every IR node that does not translate cleanly is still carrying a Drools
assumption; the known candidates are listed in `../../spec/CONSTRUCTS.md`
(rows 4–6).
