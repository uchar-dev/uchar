Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): the source of a value fold over
the subject's `maat`s (a reciprocal relation over `Jutter`, its range
written) names the attribute `bestaatniet`, which no declaration carries. A
value fold's source reads an attribute of each contributor, and this one
resolves against nothing.

    WF-034 FAIL /rules/resolvedRegels/1/waarde/argumenten/0: 'bestaatniet' is
    not a declared attribute of 'Jutter': a value fold's source reads an
    attribute of each contributor

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) Until L2 the fold
carried no range and the verifier's type-pair fallback stood in for it; the
attribute name was the defect then as now. Since L2 the range is written
(B12, D7): the migration derived it from the one reciprocal role, and the
unknown name stayed. `expect.txt` pins the arm (U1).
