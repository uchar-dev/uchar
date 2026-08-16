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
`spec/CONSTRUCTS.md` (row 11), `backlog/**`.

Out of scope: the node design itself (IR-SPEC/schema/verifier/ref —
the cut implementation card); any RegelSpraak surface syntax (frontend,
pin-bump territory).
