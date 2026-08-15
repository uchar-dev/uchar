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

Result: `spec/IR-SPEC.md` ch. 2 (the model layer) drafted as the first
normative content: eleven node definitions with field tables (English
metamodel names, v0 names as provenance), the unit-conversion algebra with
exact worked examples (future `ref/` tests), and WF-001..WF-019 — every
verifier check, golden case, and negatief case explicitly OPEN in the WF
index. Key mirror finding, verified against the source: v0's model layer
enforces nothing as an error (seven warning sites, zero error sites; silent
overwrite/merge; unknown references degrade to `Onbekend`; an unknown
conversion target is silently adopted as canonical) — so the v1 WF set is
almost entirely promotion of silent conditions, each cited inline with its
v0 behaviour and migration note. Deltas applied per `CONSTRUCTS.md`:
Attribute/Characteristic reification, Percentage as Number+`%`+dialect tag,
one Date type with `precision`, `Onbekend` removed, KenmerkSoort and
Dimension.preposition as presentation tags, Parameterset data-plane;
newly recorded: parameters get a full TimelineFacet (v0 kept only bare
granularity), rational conversion factors (also written into
`CONSTRUCTS.md` row 26 and its ch. 2.4 unit row — a deliberate, minimal
extension beyond this card's "may touch" set, required by the
no-silent-rewrite rule; reported here rather than hidden). Verified by
three adversarial reviewers (source fidelity, spec discipline, rights);
all findings fixed, none remain. This branch is a PROPOSAL: spec/IR-SPEC.md
is a protected path — the maintainer merges.

Review addendum (maintainer review, 2026-08-15): the Parameter timeline
delta is now also recorded in the CONSTRUCTS.md inventory row (review
item 2); WF-008 and WF-010 are explicitly marked use-time/expression-layer
forward references (item 4); the timeline name-normalization is marked as
mirrored v0 morphology with an OPEN design question (item 5); the header's
authority pointer to the private toolchain now names its own removal at
Phase-1 exit (item 3). Item 1 (the row-26 CONSTRUCTS edit beyond the
card's may-touch set) is accepted with this merge.
