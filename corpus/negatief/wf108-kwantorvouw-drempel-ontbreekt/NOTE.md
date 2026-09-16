Violates WF-108 (a) (`spec/IR-SPEC.md` ch. 4.5): the quantifier fold's
`kwantor` is `TEN_MINSTE`. Only `ALLE` and `GEEN` are v0 forms — the counting
quantifiers are `Samengesteld`'s vocabulary over PARTS and this node's named
extension point, and no semantics clause in `RUNTIME-SEMANTICS.md` ch. 3.9a
corresponds to one here.

The arm refuses BY NAME rather than falling through, and that is the whole
reason the node exists. The frontend computes `AttribuutRef.set` as "the
onderwerp's quantor is not the bare definite", so `alle`, `geen van de` and
`ten minste 2 van de` used to reach the wire as one shape; a reader that met an
unhandled kwantor and answered the universal would answer the opposite of what
"ten minste 2 van de" wrote, silently. The schema pins the enum to ALLE/GEEN as
well, so this document is deliberately schema-invalid — the negatief zone is
gate 3's, not gate 2's, and a rule whose only enforcement were the schema would
have no citable violation.

Verifier: `WF-108 FAIL /resolvedRegels/0/voorwaarde/kwantor: quantifier fold
with a counting kwantor: only ALLE and GEEN are v0 forms …`.
