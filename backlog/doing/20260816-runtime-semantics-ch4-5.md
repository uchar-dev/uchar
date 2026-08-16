# RUNTIME-SEMANTICS ch. 4-5: allocation and the evaluation model (proposal)

Goal: draft ch. 4 (allocation) as recurrence equations over recipient
groups — not "one helper call": the three methods, the per-recipient
maximum, rounding-floor, the undistributed remainder, and the empty/edge
rules (zero recipients, empty source, zero total ratio) — and ch. 5 (the
evaluation model): forward chaining to fixpoint, freshness and
idempotence of writes (the change-detection rule ch. 1.2/ch. 3.1 lean
on), what "a berekening is an equation" means operationally, declared
order-independence with its documented v0 exceptions, creation inside
the loop and termination, initialisation ordering, and the operational
side of the §9.5 inconsistency channel. v0 refusals recorded as
refusals; genuinely under-determined points marked OPEN.

Acceptance: every semantics clause carries a worked example with
concrete expected output; open decisions marked OPEN; gates pass.

May touch: `spec/RUNTIME-SEMANTICS.md` (protected — merge mandate),
`backlog/`.

Out of scope: ch. 6 (stratification — ch. 5 may name the constraint),
ch. 7 (defaults), ch. 8 (time selection); verifier changes; corpus.
