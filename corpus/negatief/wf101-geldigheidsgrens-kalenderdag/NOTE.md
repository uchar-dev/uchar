Violates WF-101 (`spec/IR-SPEC.md` ch. 4.1) at the **validity window's upper
bound**: the rule's `geldigheid.tot` reads `2026-02-30`, a lexeme of the right
shape naming a day February does not have. The document says nothing else
wrong.

    WF-101 FAIL /rules/resolvedRegels/0/geldigheid/tot: validity bound
    '2026-02-30' is not the ISO form yyyy-mm-dd (fixed field widths,
    valid ranges — ch. 4.1)

This is the CALENDAR arm of the rule, and it is the arm no pattern can carry.
Its sister case, `wf101-geldigheidsgrens-tekenjaar`, carries the shape arm; the
two fall independently, so relaxing the calendar half frees this one alone and
leaves that one refused.

Why it needs a rule of its own layer. The schema pattern on
`Geldigheid.vanaf`/`tot` is a shape test, and `2026-02-30` passes it —
measured 2026-09-11, accepted by the schema, where `+012026-01-01`,
`-0500-01-01` and `niet-een-datum` are all refused there. It also orders
correctly: lexicographically it sits between `2026-02-28` and `2026-03-01`,
so no reader holding the bound as an opaque string ever REPORTS the defect —
but whether an ANSWER moves depends on which bound carries the lexeme, and the
claim is false without that position. Measured 2026-09-11 on
`corpus/golden/jutterswet-vensterheffing` (evaluation dates `2026-06-01` and
`2025-06-01`), one position at a time, `ref` and the ASP backend identical in
every row and nothing faulting anywhere:

- the LOWER bound of the first of its two dated rules (`vanaf`, window
  `[2026-02-30, 2026-12-31]`): both casus replay the pinned expectations
  unchanged — this is the position the ch. 4.1 measurement table substitutes
  at, in every one of its rows;
- the UPPER bound of that same rule (`tot`, window
  `[2026-01-01, 2026-02-30]`), **the position this witness uses**: the window
  silently shrinks past the in-window casus and both executors leave
  `M1.vensterheffing` underived where the pinned value is 125 — `ref` reports
  `= leeg`, the ASP backend `is underived (leeg)`;
- the upper bound of the second dated rule (`tot`, `2025-12-31` →
  `2026-02-30`): both casus unchanged again.

So the defect is never reported and only sometimes visible in an answer, and
it is refused outright by every reader that parses the bound — the same split
WF-084 closes one field deeper, for a date LITERAL, with the same predicate.
That it takes a rule rather than a gated case is exactly this asymmetry: a
corpus case catches the lexeme only where the shifted window happens to cross
an expectation. A window that begins or ends on a day that does not exist is
not a window any executor can be asked to agree about.

Minimal own-authored document (Jutterswet vocabulary), built on the skeleton
of `corpus/negatief/wf084-source-snippet` with the date LITERAL put back in
ISO form, so the only defect left is the bound. The well-formed counterpart is
`corpus/golden/jutterswet-vensterheffing`, whose upper bounds are
`2026-12-31` and `2025-12-31`.

Falsifiable in one edit: `2026-02-28` — a real day of that month — turns this
case clean, and so does `2028-02-29`, the leap year the shared predicate
admits and 2026 does not.

Arm-exact: measured over the verifier-gated zones the day the rule landed
(golden 169, negatief 364, rood 26 — 559 documents), the before/after sweep
moves this verdict and its sister's and not one other report line anywhere.
