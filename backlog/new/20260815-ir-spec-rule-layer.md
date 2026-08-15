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
