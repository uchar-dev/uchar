Violates WF-108 (e) (`spec/IR-SPEC.md` ch. 4.5): the quantifier fold's `kwantor`
is `TEN_MINSTE` and it carries no `drempel`. A counting quantifier compares the
NUMBER of contributors whose matrix holds with a threshold, and this document
names none — so it states a quantification nothing can be measured against.

The field is present EXACTLY under the three counting quantifiers here, unlike
`Samengesteld`, which carries it always and writes a 0 nobody reads. That
divergence is the reason this directory can exist at all: a field that is
meaningful in one position and inert everywhere else can say anything (the
WF-083 (c) defect), so on this node the absence is checked rather than filled
in. Reading a missing threshold as 0 would be worse than refusing — WF-041
refuses a written 0 for the same vocabulary, so the silent default would
produce exactly the document the other rule rejects.

**History, recorded rather than elided.** Until 2026-09-16 this directory was
`wf108-kwantorvouw-telkwantor` and the same `ir.json` violated WF-108 (a): the
counting quantifiers were the node's named extension point and were refused by
name. They are built now (all three, in all three executors), so that premise
is gone and the document is well-formed apart from the threshold it does not
carry. The bare definite `DE` is what arm (a) still refuses, and it has its own
directory, `wf108-kwantorvouw-kwantor-de`.

The schema also refuses this document — its `if`/`then` on `kwantor` requires
`drempel` under a counting quantifier — so it is deliberately schema-invalid as
well. That is no substitute: the negatief zone is gate 3's, not gate 2's, and a
rule whose only enforcement were the schema would have no citable violation.

Verifier: `WF-108 FAIL /rules/rules/0/versions/0/condition/threshold: counting quantifier
fold without a threshold: the verdict compares the number of contributors
satisfying the matrix with a threshold, and there is none to compare it with`.
