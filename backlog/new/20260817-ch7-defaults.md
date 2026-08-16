# Ch. 7 — defaults and defeasibility (the Phase-4 gate, design first)

Goal: write `spec/RUNTIME-SEMANTICS.md` ch. 7 as the normative design
for the IR's defeasibility model — the Phase-4 gate. Adopt Catala's
default calculus (prioritised defaults with conflict detection) as
decided direction (PLAN §6 Phase 4, `IR-REVIEW.md`), and reconcile it
with what the ASP hand-translation established
(`spec/ASP-TRANSLATION.md` findings 3/6, CONSTRUCTS rows 7/8): v0's
only defeasible layer is one negation-as-failure over a declared
contrary, initialisation is a default with lowest priority, and
rule-version selection is temporal, not defeasible. Chapter content:
the default node shape (justification, consequence, priority relation),
conflict = the §9.5 channel (unordered simultaneous exceptions),
initialisation-as-default restated, the interaction with
stratification (ch. 6 — priorities must not smuggle in unstratified
negation), and the lowering table: how v0's mechanisms (salience,
agenda boundary, init rules) become derivable lowerings of declared
defaults. Per spec discipline every MUST gets a WF number with
verifier/negatief obligations — this card lands the design and the WF
statements with their index rows marked OPEN where the wire shape must
land first; the implementation cards (wire + executors + corpus
witnesses) follow, sized per construct.

Acceptance: ch. 7 normative draft with worked examples pinned to
absolute outcomes; CONSTRUCTS rows 7/8 updated with the decided model
and the delta column naming the wire entry (extension discipline);
WF statements numbered with honest OPEN rows; gates 1–6 stay green
(no corpus change required by this card).

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md` (ch. 6
skeleton section), `spec/CONSTRUCTS.md` (rows 7/8), `spec/PLAN.md`
(only if the Phase-4 scope itself shifts), `backlog/**`.

Out of scope: any wire, verifier, ref, or backend implementation (the
follow-up cards); Catala frontend work (Phase 5).
