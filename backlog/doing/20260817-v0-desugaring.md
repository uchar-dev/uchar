# The v0 desugaring: existing rule forms read as ladders

Goal: land the last ch. 7 slice — the **desugaring of v0's
initialisation-plus-computation pair into the ladder** — as a
specified, machine-checked lowering. `RUNTIME-SEMANTICS.md` gains
ch. 7.7 (normative): on one (type, attribute) target, one
version-free computing rule (Gelijkstelling/Berekening; its condition
becomes the rung justification, an unconditional rule desugars to the
vacuously-true empty-ALLE justification) plus at most one
unconditional, version-free Initialisatie (the ch. 7.3 init rung)
read as one attribute ladder. Out of the desugaring's domain, faulted
never guessed: conditional initialisations (their justification would
need the input-emptiness guard, which is not a writable condition —
WF-051), versioned forms, and multiple computing rules on one target
(v0 agenda order). The **pinned delta and the mirror hole** are
normative text: a computation that evaluates leeg beside an
initialisation is *divergent* in the v0 reading (the empty write and
the refill alternate — ref faults on the fixpoint budget), while the
ladder reading defines it (fall-through; input left standing) — the
desugaring repairs a hole, and no case can pin the v0 side of it.
The executable witness is a **differential across the desugaring**:
`Ir_v0.Desugar` implements the lowering document-to-document; ref
replays the v0 forms *natively* (run_rule's existing Initialisatie
semantics) while clingo translates the *desugared* document through
the ladder machinery (Initialisatie thereby becomes translatable for
the first time) — gate 6 then demands both readings agree. Anchor:
`jutterswet-waardering-v0vorm`, the v0-form twin of the
`jutterswet-vondstwaardering` ladder anchor, four cases inside the
confluent domain (computed 50; init 25 on empty; input 40 left by the
init; computed 50 over input 40).

Acceptance: gates 1–6 green; the four v0vorm cases replay exactly
through ref (native) and clingo (desugared); existing goldens and
cases byte-unchanged; the desugaring faults honestly outside its
domain (shown in the verification record).

May touch: `spec/RUNTIME-SEMANTICS.md` (ch. 7.7 + status),
`spec/IR-SPEC.md` (ch. 6.1 pointer), `spec/CONSTRUCTS.md` (rows 7/8),
`spec/verifier/**` (the Desugar module), `corpus/golden/anchor/**`,
`ref/**` (untouched expected — native side), `backends/clingo/**`,
`backlog/**`.

Out of scope: desugaring versioned or multi-writer forms; conditional
initialisations; characteristic-attribution desugaring (a single
KENMERKTOEKENNING is already the one-rung ladder trivially — recorded,
not transformed); the emitter (pin-bump).
