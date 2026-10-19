Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) at a composite position,
`Aggregation`/`count` arm, decimals facet: a count annotated at 2 decimals.
ch. 2.3 makes a count a whole, non-negative, unitless number, and that
derivation reads no operand at all — a count's type is a constant of the
operator.

    WF-091 FAIL /rules/rules/0/versions/0/value/type: decimals
    mismatch: the count aggregation is typed 2 decimals, but the type a count
    derives is 0 decimals

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) The document is
the migration's translation (`spec/proposals/v1-migratie/m2.py`): the two
operand reads lost their own `type` (B13), and the count's annotation, a
derived position, kept its (V5 (a)). The card's rewrite drops the second
operand once the count carries no operand list (B21, L4); until then the
operand list stays, and with it the WF-037 line beside this one, which
refuses a count's listed form. `expect.txt` pins the arm.
