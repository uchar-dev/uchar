Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a composite position,
`Binary`/PLUS arm, soort facet: two plain numbers are summed and the result
is annotated `Booleaan`. ch. 2.3's additive row derives a number from two
numbers.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: soort mismatch: the PLUS
    composite is typed boolean, but the type derived from its operands is
    number

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the
annotation was a percentage. Since L3 a percentage is a number with the unit
`%` (delta B14 of `spec/proposals/V1-WIREMIGRATIE.md`), and on that annotation
the report would have moved from the soort facet to the unit facet; L2
rewrote the annotation to a non-numeric soort, so the arm this witness pins
did not move at L3 (measured: the report is unchanged). The WF-023 line
beside it is the boolean written into the percentage target, a consequence of
the same annotation. `expect.txt` pins the arm.
