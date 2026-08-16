# IR-SPEC ch. 4: the expression and condition layer (proposal)

Goal: the v0 mirror of the expression and condition layer in
`spec/IR-SPEC.md` per the inventory (`CONSTRUCTS.md` ch. 2.1/2.2): the
Functie split (aggregation-with-source vs builtin call), unified
navigation on `AttribuutRef`, the split `Constante`, merged role-existence
conditions (row 5: no backref, no top-level-only), quantified compounds
(counting quantifiers as sugar), `Onopgelost` with structured reason
(row 16 delta), typing rules referencing RUNTIME-SEMANTICS ch. 1–2 —
including the candidate WF from ch. 2.4 (explicit rounding before a
bounded write) as a real WF rule.

Acceptance: every MUST carries a WF-### ID; WF index extended gaplessly;
deviations only where CONSTRUCTS.md records a delta; no reproduced
specification text; gate passes.

May touch: `spec/IR-SPEC.md` (protected — proposal), `spec/CONSTRUCTS.md`
(delta sync only), `backlog/`.

Out of scope: rule layer (own card); dialect lowering chapter; verifier.

Result: `spec/IR-SPEC.md` ch. 4 (the expression and condition layer)
drafted: the full node set with field tables and WF-034..044, index
gapless through WF-044. Headline deltas, all tracked: the Functie split
into Aggregation (element type, filter, sum-only empty fallback,
dimension selection) and BuiltinCall with a per-kind arity/facet
catalogue (row 1); unified navigation on AttributeReference (row 4); the
Constante split into ContextValue (row 25); one existential
RoleMembership without backref or the top-level-only restriction (row 5);
the eigenaar pattern-steering field dropped; Unresolved with the
structured-reason delta marked OPEN until the negatief corpus fixes the
taxonomy (row 16); the pending cross-repo Bron delta (row 3) named as a
coming wire-format change. Verifier findings fixed before commit — two
blockers: date ordering does NOT require equal precision in v0 (a
deliberate asymmetry, now mirrored with an OPEN on whether v1 keeps it)
and TimeWindow bounds admit either date precision in v0 (day-precision
claim dropped); plus RFC-2119 phrasing on WF-035..038, the five-way
dialect enumeration in the intro, exact-digits positivity unclaimed, the
elementType value-fold artifact recorded, WF-040 promotion tagged, the
construct-keyword carve-out added to the header, RoleMembership's row
cite corrected, and the golden cells marked "(v0 mirror form)". CONSTRUCTS
sync: the BinOp variants note now marks GEDEELD_DOOR_ABS dialect (defined
collapse point) rather than lumping it under desugar. This branch is a
PROPOSAL: spec/IR-SPEC.md is a protected path — the maintainer merges
(fast-forward).

Addendum at merge (maintainer review): the WF-042 index cell brought in
line with the corrected body (bounds are dates, either precision), and
the Method paragraph's stale "Resolver --ir, to be built" updated to name
the emitting toolchain commit. Ch. 7's skeleton lag ("lands with
spec/schema/") noted as pre-existing, left for the serialization chapter.
