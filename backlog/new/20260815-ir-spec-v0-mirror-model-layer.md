# IR-SPEC v0 mirror: the model layer (proposal)

Goal: First normative content in `spec/IR-SPEC.md`, per the
mirror-then-diverge method: the v0 mirror of the model layer (object types,
kenmerken, fact types and roles, parameters, domains/datatypes/units,
dimensions, timelines) node by node — structure, well-formedness conditions,
semantics reference — with the first `WF-###` IDs. Deviations from v0 only
where `CONSTRUCTS.md` records a delta; each with a migration note.

Acceptance: every MUST introduced carries a `WF-###` ID; where the verifier
or corpus case for a rule does not yet exist, the gap is explicitly marked
`OPEN` in the WF index (the spec-discipline debt is visible, never silent);
no verbatim RegelSpraak-specification text; `scripts/check-self-contained.sh`
passes.

Depends on: the constructs inventory card (the verdict column steers what is
mirrored versus deviated).

May touch: `spec/IR-SPEC.md` (protected path — the diff is a proposal; the
maintainer merges), `backlog/`

Out of scope: the rule layer and expression layer (follow-up cards); the JSON
schema; `spec/verifier/`.
