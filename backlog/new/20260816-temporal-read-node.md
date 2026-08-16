# The temporal-read node: design + artifacts (row 11, decided)

Goal: implement the decided point-in-time read as a v1 core node.
The wire-entry question is **already decided** (2026-08-16, with the
relations-first-class card): v1 constructs enter as additive, optional
extensions of regelspraak-ir-v0 — `spec/IR-SPEC.md` § Method and
`spec/schema/README.md` record the discipline; this card rides it (the
read is a new extension *node* in the expr union, the loaders accept
it, the emitter never produces it yet). Then: the expression node in
`spec/IR-SPEC.md` (a temporal read: timeline operand — a temporal
attribute, temporal characteristic, or granular parameter — plus a
day-precision date operand; result = the element type, leeg on a gap
per ch. 3.1; ch. 8's fail-fast lives in the data plane, not in the
read), WF rules (operand must be temporal; date operand day-precision)
with verifier checks and negatief cases, and the `ref/`
implementation (Timeline.read is the semantics). The golden witness is
a **hand-written anchor case** (`corpus/golden/anchor/` is hand-written
by charter: schema-valid, byte-round-tripping, absolute expected
values) carrying the art. 1 shape — "the value at the child's birth" —
with multi-segment timeline input, which needs a timeline value variant
in `spec/schema/uchar-case-v0.schema.json`. The emitter follows later
via a pin-bump conformance PR; that lands outside this card.

Acceptance: every new WF rule has a verifier check + negatief case +
the anchor golden as witness; gate 5 replays the anchor case through
the node; gates 1–4 green (the anchor ir.json validates and
round-trips; the extended case schema is metaschema-valid).

May touch: `spec/IR-SPEC.md`, `spec/schema/**`, `spec/verifier/**`,
`corpus/golden/anchor/**` (new), `corpus/negatief/**`, `ref/**`,
`backlog/**`.

Out of scope: RegelSpraak surface syntax and the emitter (pin-bump
territory); the Drools lowering; ch. 8's Parameterset data format
beyond what the anchor case needs.
