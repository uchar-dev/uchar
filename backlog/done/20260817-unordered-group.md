# The declared-unordered default group and the §9.5 conflict path

Goal: land the last executable ch. 7 slice — **declared-unordered
default groups** with over-determination on the §9.5 channel (worked
example 7.6.3). Wire (additive on `PrioritizedDefaults`): an optional
boolean `ongeordend`; multiple defaults groups on one target are admissible
iff **every** defaults group on that target carries the marking (WF-050's
temporary single-group clause is thereby replaced by the designed
rule — priority explicit or explicitly absent, never implicit).
Semantics per ch. 7.2: each member defaults group decides independently
(fall-through per ch. 7.1); if two members decide differently, the
group **does not write** (the target keeps its input value, or stays
leeg) and one §9.5 channel fact records the conflict, keyed by the
target form (subject.target) × subject — an end-state observation;
equal decisions write once, no conflict. Characteristic groups
conflict on attribute-vs-do-not-attribute. `ref/` evaluates defaults
per group at the group's stratum (decisions collected first, then one
write or one channel fact); clingo lowers per group: per-defaults-group
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

Result: landed in full — ch. 7's executable design is complete. The
`ongeordend` marking rides the wire; WF-050's temporary single-group
clause became the designed rule (priority explicit or explicitly
absent; negatief wf050-half-marked-group). ref evaluates defaults per
group at the group's max stratum — members decide independently, the
group acts once: equal decisions write once, differing decisions
write nothing (input kept standing) and deposit one channel fact
keyed by the target form; clingo carries the ch. 7.2 group layer
(per-member outcomes, pairwise conflict to inconsistent/2, outcome
heads and defeasible input guarded on the conflict), with singleton
groups eliding the machinery — the two existing defaults anchors replay
byte-identically. The anchor jutterswet-toelagegronden pins worked
example 7.6.3 across six cases, including the characteristic-group
conflict and input-kept-standing. Adversarial verification (two opus
lenses): one major, fixed before merge — the group key ignored the
target KIND, wrongly grouping a name-sharing attribute and
characteristic (WF-002 allows the sharing) into one group, with false
WF-050 rejections and executor breakage; the key now carries a kind
discriminant in all three sites, and the anchor gained the
name-sharing witness (a singleton characteristic defaults group named
'toelage' that must never group with the attribute defaults). Also
fixed: the stale wf050 doc comment and dead scaffolding, and the
CONSTRUCTS row-8 tail. Recorded, not changed: ref keys the conflict
fact raw and clingo normalised — the inherited channel convention;
no gate asserts the key, and rule-readability stays OPEN.
Deliberately left: the v0 desugaring (the next card); rule-readability
of the conflict fact; the emitter (pin-bump).
