# The declared-unordered ladder group and the §9.5 conflict path

Goal: land the last executable ch. 7 slice — **declared-unordered
ladder groups** with over-determination on the §9.5 channel (worked
example 7.6.3). Wire (additive on `DefaultLadder`): an optional
boolean `ongeordend`; multiple ladders on one target are admissible
iff **every** ladder on that target carries the marking (WF-050's
temporary single-ladder clause is thereby replaced by the designed
rule — priority explicit or explicitly absent, never implicit).
Semantics per ch. 7.2: each member ladder decides independently
(fall-through per ch. 7.1); if two members decide differently, the
group **does not write** (the target keeps its input value, or stays
leeg) and one §9.5 channel fact records the conflict, keyed by the
target form (subject.target) × subject — an end-state observation;
equal decisions write once, no conflict. Characteristic groups
conflict on attribute-vs-do-not-attribute. `ref/` evaluates ladders
per group at the group's stratum (decisions collected first, then one
write or one channel fact); clingo lowers per group: per-ladder
outcome predicates, a pairwise conflict rule, outcome heads and the
defeasible input both guarded on the conflict. Witness: the anchor
`jutterswet-toelagegronden` — an attribute group of three grounds
(visser → 10, wachter → 20, duinwachter → 10) and a characteristic
group of two (visser → gerechtigd, geschorst → do-not-attribute) —
with six cases pinning 7.6.3: single ground (10; 20), differing
grounds → leeg + inconsistent, equal grounds → 10 without conflict,
characteristic conflict → absent + inconsistent, and input 40 kept
standing under a conflict.

Acceptance: gates 1–6 green; all six anchor cases replay exactly
through ref and clingo; the new `wf050-*` negatief (half-marked
group) rejected citing its rule; existing goldens and cases
byte-unchanged.

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` (ch. 7
status), `spec/CONSTRUCTS.md` (row 8), `spec/schema/**`,
`spec/verifier/**`, `corpus/golden/anchor/**`, `corpus/negatief/**`,
`ref/**`, `backends/clingo/**`, `backlog/**`.

Out of scope: rule-readability of the conflict fact (stays OPEN); the
v0 desugaring (its own card, next); mixed groups across target kinds
(one target = one kind by construction); the emitter (pin-bump).
