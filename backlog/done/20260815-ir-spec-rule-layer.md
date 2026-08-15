# IR-SPEC ch. 3: the rule layer (proposal)

Goal: the v0 mirror of the rule layer in `spec/IR-SPEC.md`, node by node
per the inventory (`CONSTRUCTS.md` ch. 2.3): assignment (one node +
presentation tag, explicit version selection — no name mangling),
characteristic attribution, object creation, fact creation re-expressed
with symbolic role bindings and {role, direction} steps (rows 4/22),
consistency (declarative), unified uniqueness (row 20), allocation with
its methods (row 17, COMPLEX excluded as verifier error), day-kind
definitions, start-point registration — with the next WF-### block and
every not-yet-existing artifact marked OPEN.

Acceptance: every MUST carries a WF-### ID; WF index extended gaplessly;
deviations only where CONSTRUCTS.md records a delta (each with migration
note); no reproduced specification text; `scripts/check-self-contained.sh`
passes.

May touch: `spec/IR-SPEC.md` (protected — proposal; maintainer merges),
`spec/CONSTRUCTS.md` (delta-column sync only, the established pattern),
`backlog/`.

Out of scope: the expression/condition layer (own card); serialization;
`spec/verifier/`.

Result: `spec/IR-SPEC.md` ch. 3 (the rule layer) drafted: ten node
definitions with field tables and WF-020..033, the WF index extended
gaplessly. The headline deltas, all tracked: explicit version structure
under one rule name (replacing the v0 " #i" name-suffix scheme, which
rides every versioned rule form — migration by suffix-stripping across
forms); one Rule node with the presentation tag and the target's member
kind realising decision 4 (CONSTRUCTS Soort row synced); fact creation
and uniqueness scope re-expressed symbolically ({role, direction} path
steps, no zetVeld/joinVeld/scopeVeld — rows 4/20/22); allocation without
COMPLEX and with per-method field validity plus the one-allocation-per-
target rule (row 17); consistency declarative with the v0 shape-gate
recorded as backend maturity (row 19); day-kind single-definition and
start-point promotions; rule group as dialect with the recursion marker
carried, not interpreted. Two verifier blockers fixed before commit: the
version-overlap mirror note (v0 already errors — not a promotion) and the
target-resolution mirror note (v0 degrades silently — promotion, correctly
tagged); plus the register/consistency set (WF-index header ch. 2–3,
WF-019 golden cell honest about the singleton-list witness, promotion
summary rescoped per layer, presentation-tag layer carve-out, temporal
uniqueness keys marked OPEN per row 20, RuleGroup fixpoint pointer to
RUNTIME-SEMANTICS ch. 5). The golden case is cited as instantiating the
v0 mirror form of ch. 3.1. This branch is a PROPOSAL: spec/IR-SPEC.md is
a protected path — the maintainer merges (fast-forward).
