# jutterswet-kwantorvouw-en-unie — a division in the matrix of a quantifier fold and in a branch of a union

Hand-written (`ir.json` only): the RegelSpraak frontend writes no division in
the test of a quantifier fold and no condition other than a characteristic in
a branch of an instance-set union. Own Jutterswet casus, rights-free.

A filled dividend divided by a numeric zero is leeg with a §9.5 fact of kind
EF-005 on the rule × subject that evaluates it (`RUNTIME-SEMANTICS.md`
ch. 1.4 point 4, ch. 5.6). The matrix of a quantifier fold stops at the first
contributor whose verdict settles the fold, and a member never reached
produces no channel fact (ch. 3.9a); the selections of a union are read only
where the count that holds them is evaluated (ch. 3.4d).

## Forms

- **drukte**: a Jutter is druk if for at least one of his vondsten the
  gewicht divided by the deelnemers is greater than 2 (`QuantifierFold`,
  `atLeast` 1, the division in its `test`).
- **unietelling**: for an actief Jutter, the number of his vondsten that are
  gemerkt or whose gewicht divided by their deelnemers is greater than 2
  (`InstanceSetUnion`, the division in the filter of the second branch).

## Hand-computed absolutes

Case 1 (`case1-niet-bereikt`): J1, not actief, with V1 (10, 2) and V2 (9, 0),
in that order. V1 settles "ten minste één" (5 > 2), so V2 is not reached: J1
is **druk**; unietelling is **leeg** (the rule does not fire). Consistent.

Case 2 (`case2-bereikt`): J2, actief, with V3 (9, 0) and V4 (10, 2, gemerkt),
in that order. The matrix divides at V3 (leeg, not greater than 2) and V4
settles it: J2 is **druk**. The union counts V4 once (gemerkt, and 5 > 2) and
V3 in neither branch: unietelling is **1**. The channel carries exactly two
EF-005 facts, both on the subject: `drukte × J2` and `unietelling × J2`.

## The differential square

ref, clingo and both Drools branches play both cases (gates 5, 6 and 12).
