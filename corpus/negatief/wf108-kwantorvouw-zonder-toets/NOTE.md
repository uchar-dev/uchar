Violates WF-108 (d) (`spec/IR-SPEC.md` ch. 4.5): the quantifier fold carries no
`toets`. It states a range — every Berger the ploegindeling reaches — and asks
nothing of it, so it has no verdict to give.

The wire field is nullable ON PURPOSE (`spec/schema/uchar-ir-v1.schema.json`,
`$defs/Kwantorvouw`) and the loader carries the absence rather than failing on
it: a load failure has no rule to name, and this rule needs a witness. The
reference interpreter refuses the same document with the same reason if it is
ever reached in evaluation (`quantifier fold without a toets … (WF-108)`), the
house second line of defence.

Verifier: `WF-108 FAIL /rules/resolvedRegels/0/versions/0/condition/toets: quantifier fold
without a toets: the matrix is what every contributor must satisfy …`.
