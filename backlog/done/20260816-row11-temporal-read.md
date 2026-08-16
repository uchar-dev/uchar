# Row 11 decided: the point-in-time read enters the v1 core

Goal: record the maintainer's decision of 2026-08-16 — the IR must be
expressive enough to carry every source-language primitive, and the
"op dat tijdstip" gap is decided accordingly: v1 exposes `read(tl, d)`
(RUNTIME-SEMANTICS ch. 3.1) as a core IR expression node — a temporal
read carrying a timeline operand and a date operand — rather than
keeping timelines collapse-only. This makes Jutterswet art. 1 (the
value at the child's birth) and the ch. 8 vintage read (a parameter
timeline read at the evaluation date) expressible. Record the verdict
where the OPEN lives (ch. 3.3, the ch. 3.12 row, CONSTRUCTS row 11)
and cut the implementation card (node design in IR-SPEC, schema,
verifier WF, ref/, and an anchor case — spec-first; the emitter
follows via a pin bump).

Acceptance: no stray row-11 OPEN left in RUNTIME-SEMANTICS or
CONSTRUCTS; all five gates green; implementation card in backlog/new/.

May touch: `spec/RUNTIME-SEMANTICS.md` (ch. 3.3/3.12 verdicts only),
`spec/CONSTRUCTS.md` (row 11), `spec/ASP-TRANSLATION.md` (the finding-4
pointer to the decision), `backlog/**`.

Out of scope: the node design itself (IR-SPEC/schema/verifier/ref —
the cut implementation card); any RegelSpraak surface syntax (frontend,
pin-bump territory).

Result: the decision is recorded at every site where the OPEN lived —
RUNTIME-SEMANTICS ch. 3.3 (decided: a core temporal-read node; the
vintage read keeps read's leeg-on-gap totality with ch. 8's fail-fast
in the data plane), the ch. 3.12 row, CONSTRUCTS row 11, and the
ASP-TRANSLATION finding-4 pointer (in ASP the node is a plain join).
The implementation card is cut executable: the wire-entry choice
(additive v0 extension vs format bump) is its first step, and the
golden witness is a hand-written anchor case — anchor/ is hand-written
by charter — so gate 5 replays without waiting for the pin bump; the
emitter follows via the pin-bump conformance PR. Verification (1 lens)
found 2 majors (the fail-fast/leeg-on-gap conflict unreconciled; the
implementation card unsatisfiable under its own schema policy) — both
fixed before the merge.
