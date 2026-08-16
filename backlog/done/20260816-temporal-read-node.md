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
the anchor golden as witness; gates 5 **and 6** replay the anchor case
through the node (gate 6 postdates this card's cutting: the corpus is
differential, so the Clingo translator must carry the read — a segment
relation joined on containment — or the gate set regresses); gates 1–4
green (the anchor ir.json validates and round-trips; the extended case
schema is metaschema-valid).

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` (ch. 3.3
status + the §3.12 summary row), `spec/CONSTRUCTS.md` (row 11),
`spec/ASP-TRANSLATION.md` (the finding-4 landing note),
`spec/schema/**`, `spec/verifier/**`, `corpus/golden/anchor/**`,
`corpus/negatief/**`, `ref/**`, `backends/clingo/**` (added with
gate 6, see above), `backlog/**`.

Out of scope: RegelSpraak surface syntax and the emitter (pin-bump
territory); the Drools lowering; ch. 8's Parameterset data format
beyond what the anchor case needs.

Result: landed in full, on the wire-entry discipline the
relations-first-class card recorded. `TemporalRead` (IR-SPEC ch. 4.3a,
WF-049 with three negatiefs: non-temporal basis, datetime index,
type/element mismatch) rides the expr union as an extension node; the
uchar-case-v0 format gained the full-timeline value variant
(grid-aligned, ordered, non-overlapping — ref-loader cross-checks);
ref/ implements the node on Timeline.read verbatim (empty index →
leeg), clingo as a segment-containment join (tl/5, param_tl/4) where
gap and empty index are leeg-as-absence. The anchor witness
(anchor/jutterswet-aanmeldquotum, asOfDate null — the read indexes a
non-evaluation-date instant) exercises the attribute read, the
parameter vintage read, the gap year, and the empty index, replayed
exactly by gates 5 and 6. Adversarial verification (three opus
lenses): one major, fixed before merge — clingo silently read leeg
where ref faults on a convention mismatch (read without the full
timeline in the case; total over a full timeline); clingo now carries
mirroring translation faults, so "executors fault, never guess" holds
for both. Also fixed: the missing element-type negatief, the stale
§3.12 summary row, and the may-touch omissions (ASP-TRANSLATION.md;
backends/clingo joined with gate 6, which postdates the card's
cutting — that scope note landed with the implementation commit, not
the claim commit, recorded here honestly). Deliberately left: the
temporal-characteristic operand (OPEN — no characteristic-timeline
case format); TOTAAL over a case-supplied full timeline (both
executors fault); the emitter surface (pin-bump); ch. 8's Parameterset
session-start coverage check (data plane, its own card).
