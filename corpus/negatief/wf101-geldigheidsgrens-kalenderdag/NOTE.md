Violates WF-101 (`spec/IR-SPEC.md` ch. 4.1) at the **validity window's upper
bound**: the rule's `geldigheid.tot` reads `2026-02-30`, a lexeme of the right
shape naming a day February does not have. The document says nothing else
wrong.

    WF-101 FAIL /resolvedRegels/0/geldigheid/tot: validity bound
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
so the three readers that hold a bound as an opaque string never notice, and
on `corpus/golden/jutterswet-vensterheffing` with this bound substituted both
`ref` and the ASP backend replay the pinned expectations unchanged (measured
the same day, PASS on both casus in both executors). The defect is therefore
invisible to every reader that does NOT parse the bound, and refused by every
reader that does — the same split WF-084 closes one field deeper, for a date
LITERAL, with the same predicate. A window that begins or ends on a day that
does not exist is not a window any executor can be asked to agree about.

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
