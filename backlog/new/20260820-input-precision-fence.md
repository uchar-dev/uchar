# Fail fast on over-precise case input in ref's loader

Goal: a case value whose lexeme does not fit the attribute's declared
decimals (e.g. `40.005` or `1/3` on a 2-decimal attribute) is silently
accepted by `ref/` (stored as an exact rational) while the clingo
backend faults it (`scaled_of_value`: "does not fit N decimals") — a
ref/clingo divergence on schema-valid input, found by the standing-norm
card's breaker lens (2026-08-20, probe: input `40.005` on
`jutterswet-waardestelling`, ref PASS vs clingo TRANSLATION FAULT).
Enforce the declared decimals at ref's load/validation boundary (fault
with label.attribute context, before evaluation), so both executors
refuse identically; add a red-corpus witness pinning the fault in both.

Acceptance: the probe case faults in ref with a named reason; a new
`corpus/rood/` case pins the fault fragment in BOTH executors; all
eight gates green; no existing corpus case regresses.

May touch: `ref/lib/` (the case/world validation site), `corpus/rood/`,
`scripts/check-rood.sh` only if the survivor/pin machinery needs it.

Out of scope: the case schema (the pattern stays permissive; the check
is semantic, against the declaration); parameter values (check whether
the same gap exists there — if so, cover in this card, else record).
