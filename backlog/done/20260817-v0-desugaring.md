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

Result: landed, and the adversarial round earned its keep. Ch. 7.7 is
normative: exactly one unconditional, version-free Initialisatie plus
at most one version-free computing rule on a target read as one
attribute ladder (the init rung is what the desugaring exists for);
conditional inits, versioned forms, and multi-writer targets stay
native with their reasons; the pinned delta stands — computed-leeg
beside an init is divergent in the v0 reading (ref faults on the
fixpoint budget) while the ladder defines it. Ir_v0.Desugar implements
the table; the ASP backend desugars before translating (Initialisatie
became translatable), ref replays the v0 forms natively, and the
gate-6 differential over anchor jutterswet-waardering-v0vorm (four
confluent cases: 50 / 25 / 40 kept / 50 over input) witnesses the
lowering itself. The verification's blocker: the first cut desugared
bare computations without an init too — silently changing six goldens'
clingo path and diverging from ref on computed-leeg-over-input; the
domain now requires exactly one init, the goldens translate natively
again, and the bare-computation case is spec text. Also from the
round: the too-strong "faulted by a consumer" sentence was made
precise (order-dependent forms fault in ref; no gate admits executor
disagreement), and the underlying pre-existing native-path limitation
— a computed leeg overwriting an input value has no absence encoding —
is now a recorded clingo-README limitation, kin to the empty-equality
asymmetry. Ch. 7 is complete; only rule-readability of the conflict
fact stays OPEN. Deliberately left: the native-path defeasible-input
generalisation (its own card if a case ever demands it); the emitter
(pin-bump).
