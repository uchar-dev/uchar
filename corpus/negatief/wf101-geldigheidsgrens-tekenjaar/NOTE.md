Violates WF-101 (`spec/IR-SPEC.md` ch. 4.1) at the **validity window's lower
bound**: the rule's `geldigheid.vanaf` reads `+012026-01-01`, the sign-prefixed
extended-year spelling. It is not the strict `yyyy-mm-dd` lexeme, and the
document says nothing else wrong.

    WF-101 FAIL /resolvedRegels/0/geldigheid/vanaf: validity bound
    '+012026-01-01' is not the ISO form yyyy-mm-dd (fixed field widths,
    valid ranges — ch. 4.1)

This is the SHAPE arm of the rule. Its sister case,
`wf101-geldigheidsgrens-kalenderdag`, carries the calendar arm; the two fall
independently, so dropping the whole rule frees both and relaxing only the
calendar half frees that one alone.

Why this lexeme and not an arbitrary non-date. Three readers — the verifier,
the reference interpreter and the ASP backend — hold a validity bound as an
OPAQUE STRING and order it with `<=`. That ordering is calendar order only
inside the strict lexeme. A leading `+` (0x2B) sorts before every digit, so
`+012026-01-01` is *smaller than every four-digit year*: the window silently
opens at the beginning of time. Measured 2026-09-11 on
`corpus/golden/jutterswet-vensterheffing` with this one bound substituted and
nothing else changed, both executors moved the same way and neither faulted:

    ref     case2-voor-venster  MISMATCH: M1.vensterheffing = 125, expected leeg
    clingo  case2-voor-venster  MISMATCH: M1.vensterheffing = [125], expected leeg

A rule version applied to a casus that falls before its window, with no
report anywhere. `-0500-01-01` does the same for the same reason (`-` is
0x2D, also below every digit), and `niet-een-datum` does the opposite —
sorting after every date, it closes the window over its own casus, and the
in-window casus then answers `leeg` where the pinned value is 125. Not one of
those forms produces a fault in any reader; every one of them produces a
silent wrong answer.

Minimal own-authored document (Jutterswet vocabulary), built on the skeleton
of `corpus/negatief/wf084-source-snippet` with the date LITERAL put back in
ISO form, so the only defect left is the bound. The well-formed counterpart is
`corpus/golden/jutterswet-vensterheffing`, whose two independently dated rules
carry `2026-01-01`/`2026-12-31` and an open-ended `2025-12-31` (distinct names
and distinct target attributes — that document exercises the window, not
version selection).

Note what does NOT guard this. The schema pattern on `Geldigheid.vanaf`/`tot`
refuses this same lexeme — but gate 2 validates `corpus/golden` and
`corpus/rood` only and never sees this zone, so the pattern and this case
guard different populations rather than the same one twice.

Arm-exact: measured over the verifier-gated zones the day the rule landed
(golden 169, negatief 364, rood 26 — 559 documents), the before/after sweep
moves this verdict and its sister's and not one other report line anywhere.
