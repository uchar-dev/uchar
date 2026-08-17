# The attribute ladder: leeg fall-through and the initialisation rung

Goal: land the second ch. 7 slice — the **attribute-target ladder**
with Catala's fall-through and the **initialisation rung** — end to
end. Wire (additive on the existing `DefaultLadder`/`Rung` extension
form, backward-compatible with the landed characteristic anchor):
`kenmerk` becomes nullable, `attribuut` joins as the alternative
target (exactly one, WF-050); `Rung.besluit` becomes nullable,
optional `consequentie` (expression) and `initialisatie` (boolean)
join. Semantics per ch. 7.1/7.3: an attribute rung decides when its
justification holds AND its consequence evaluates gevuld — a leeg
consequence falls through; a ladder without a decision writes nothing
(the input value is left standing); the initialisation rung (lowest
only, no written justification) is justified iff the target is empty
in the input facts; rung evaluation is speculative (only the deciding
rung's channel deposits persist). WF-050 gains the shape clauses
(exactly one target; rung shapes per kind; init lowest; no other
writer — now including plain rules and allocations on an attribute
target); WF-051 gains the consequence reads (the gevuld gate is
decision-bearing). `ref/` implements the fall-through in run_ladder
with a per-rung speculation buffer; clingo puts the consequence
bindings inside `decides_n` (leeg = underivable = fall-through for
free) and lowers the input value of a ladder target defeasibly
(`inputval/3`; `val :- inputval, not decided`) so a deciding rung
overwrites input exactly as ref/'s write does. Witness: the anchor
`jutterswet-vondstwaardering` — three rungs (erkend → basisbedrag,
kandidaat → kandidaatbedrag, initialisation → 25) with six cases
pinning ch. 7.6.2's outcomes: top rung decides (50), fall-through one
rung on a leeg consequence (30), fall-through to the initialisation
(25), initialisation alone (25), input left standing (40), a deciding
rung overwriting input (50).

Acceptance: gates 1–6 green; all six anchor cases replay exactly
through ref and clingo; the new `wf050-*` negatiefs rejected citing
their rule; existing goldens and cases byte-unchanged (the landed
characteristic anchor keeps validating against the widened schema).

May touch: `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` (ch. 7
status), `spec/CONSTRUCTS.md` (rows 7/8), `spec/schema/**`,
`spec/verifier/**`, `corpus/golden/anchor/**`, `corpus/negatief/**`,
`ref/**`, `backends/clingo/**`, `backlog/**`.

Out of scope: the declared-unordered group and the §9.5 conflict path
(next slice); the v0 desugaring of existing Initialisatie/computation
rules into ladders (the v0 forms stay the mirror); temporal ladder
targets (refused fail-fast); the emitter (pin-bump).
