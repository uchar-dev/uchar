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

Result: the fence lands at ref's evaluation boundary
(`validate_input_precision`: attributes, attribute timelines, scalar
parameters, parameter timelines — one shared check), red-first (40.005
replayed green through ref while clingo faulted), with the shared
"does not fit N decimals" fragment and the value rendered as its
decimal lexeme. Two rood witnesses pin it in both executors:
`input-overprecision` (the attribute branch, the waardestelling anchor
verbatim) and `input-overprecision-param` (the parameter branch, the
penningdrempel golden verbatim) — 13 red total, all gates green. The
verification round caught a REAL counter-divergence the first version
introduced — an over-precise scalar on a granular parameter made ref
fault where clingo never reads that slot — closed at the loader with
the symmetric refusal ("a scalar value on a granular parameter"),
mirroring the existing timeline-on-scalar rejection. Recorded, not
fixed here: the pre-existing null-datatype strictness gap (clingo
faults "unscalable datatype" where ref accepts — verifier-rejected
shapes, low realism); the timeline branches share the single check
code path with the two pinned branches but carry no own corpus witness.
