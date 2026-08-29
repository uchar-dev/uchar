# The art. 4 defaults: the characteristic PrioritizedDefaults lands (Phase 4, first cut)

Goal: implement the first slice of the ch. 7 default model — the
**characteristic defaults** — end to end, with the art. 4 exception
defaults as its witness. On the decided wire-entry discipline: an
optional top-level extension list `prioritizedDefaults`, each a
`PrioritizedDefaults` node (subject object type, target characteristic,
defaults in *descending* priority, each default a `Default` with a `besluit`
boolean — attribute or do-not-attribute, ch. 7.1 — and a justification
condition). Semantics per ch. 7: the highest justified default decides
(on a characteristic every justified default decides); a true decision
attributes at the fixpoint of the defaults' justification strata; a
false decision derives nothing and displaces. WF-050 (defaults
well-formedness: ≥ 1 default, subject and characteristic resolve,
justifications present, at most one defaults group per target in this format —
the declared-unordered group is a later additive slice) and WF-051
(every default-justification read joins the ch. 6 graph as a negative
edge toward the defaults' write; a cycle through a defaults group cites
WF-051) get verifier checks and negatief cases. `ref/` evaluates
prioritized defaults inside the existing strata machinery (shared
`Checks.dep_forms`, so verifier and executor cannot drift); clingo
lowers per ch. 7.5's `decides_m` table. Witness: the anchor
`jutterswet-uitsluiting` — the art. 3/4 shape with input
characteristics (vergunning geweigerd / weigering herroepen /
herroeping vervallen), the three-default group on `uitgesloten`, and a
negated consumer (`penninggerechtigd` unless excluded, forcing the
defaults below the negative reader) — with four case files: w / wh / whv
pin the hand-run outcomes of `spec/ASP-TRANSLATION.md` ch. 6.3 and
`RUNTIME-SEMANTICS.md` ch. 7.6.1 (against the simplified consumer);
the bare case is the added null witness.

Acceptance: gates 1–6 green; all four anchor cases replay exactly
through ref and clingo; `wf050-*` and `wf051-*` negatiefs rejected
citing their rules; existing goldens byte-unchanged.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` (ch. 7
status), `spec/CONSTRUCTS.md` (rows 7/8), `spec/schema/**`,
`spec/verifier/**`, `corpus/golden/anchor/**`, `corpus/negatief/**`,
`ref/**`, `backends/clingo/**`, `backlog/**`.

Out of scope: attribute-target defaults and leeg fall-through (needs
the gevuld gate — next slice); declared-unordered groups and the §9.5
conflict path; the initialisation default; speculative-deposit guards
(characteristic justifications deposit nothing); the v0 desugaring and
the emitter (pin-bump); art. 4bis parallel grounds.

Result: landed in full. The characteristic PrioritizedDefaults rides the
wire as the optional prioritizedDefaults extension list (IR-SPEC v0.7
ch. 6.1); WF-050 (≥ 1 default; subject/characteristic resolve; the
target has no other writer — including the plain-attribution-rule
clause the verification demanded) and WF-051 (every
default-justification read is a negative edge in the shared dep_forms
graph; a cycle through a defaults group cites WF-051) are checked with four
negatief cases between them. ref/ evaluates prioritized defaults inside the strata
machinery (highest justified default decides; do-not-attribute displaces
and derives nothing); clingo carries the ch. 7.5 decides_m lowering
verbatim. The anchor jutterswet-uitsluiting puts the art. 4 defaults
under a negated consumer, and its four cases (w / wh / whv from the
ASP ch. 6.3 hand-runs against the simplified consumer, plus the bare
null witness) replay exactly through gates 5 and 6 — worked example
7.6.1 is machine-checked. Adversarial verification (two opus lenses):
no blockers; fixed before merge: the WF-051/ch. 7.4 norm text now
states the implemented conservative reading (ALL justification reads
are negative, not only "reads of the defaults above"), the
single-writer gap (a plain characteristic rule beside a defaults group
silently defeating do-not-attribute) became a WF-050 clause with its
negatief, the stale "reserved" tail in CONSTRUCTS row 8, the
implicit/explicit multiplicity wording, and the bare-case provenance.
Recorded, not fixed (informative): clingo itself does not reject
defaults cycles — the verifier is the guard that a document is
clingo-suitable, as the README already states. Deliberately left: the
remaining ch. 7 slices (attribute defaults, unordered groups,
initialisation default, speculative-deposit guard, v0 desugaring) and
art. 4bis parallel grounds.
