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

Result: landed in full. The attribute ladder rides the extension form
backward-compatibly (kenmerk nullable, attribuut as the alternative
target; Rung gains consequentie and initialisatie); ref implements the
ch. 7.1 fall-through with a per-rung speculation buffer, and clingo
gets the gevuld gate for free (consequence bindings inside decides_n)
plus the defeasible-input lowering (inputval/3), so a deciding rung
overwrites input exactly as ref's write does. The anchor
jutterswet-vondstwaardering pins ch. 7.6.2's outcomes across six
cases (50/30/25/25/40/50), replayed by gates 5 and 6. WF-050 carries
the shape clauses and the full single-writer lift — after the
adversarial round also against object-creation initialisations (the
found major: a creation-time write would void the init rung's
input-emptiness justification) and with the allocation clause's own
negatief; five negatiefs joined this card in total. Verification also
established the sharpest cross-executor risk is safe by design: the
ch. 1.2a leeg-coalescing constructs (Binair arithmetic) that could
diverge from clingo's leeg-as-absence body-joins are exactly the
consequence forms clingo refuses to translate — an honest fault, never
a silent wrong answer (recorded). Also recorded: the speculation
buffer now wraps the characteristic branch too (corpus-invisible,
conform ch. 7.1). Deliberately left: the declared-unordered group and
the §9.5 conflict path (7.6.3, the next slice); the v0 desugaring of
Initialisatie/computation rules into ladders; temporal ladder targets
(refused fail-fast); the emitter (pin-bump).
