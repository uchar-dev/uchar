Violates WF-108 (c) (`spec/IR-SPEC.md` ch. 4.5): the quantifier fold spells its
range TWICE — a `bronPad` beside a `bronRol`. One meaning, two spellings: the
one-step range IS `bronRol`, so the two fields are mutually exclusive.

This is WF-102's discipline under this node's own number, and the number is new
on purpose: WF-102 is scoped to a fold's ARGUMENT — the source position, which
a quantifier fold does not have — and a negatief directory must be able to
declare the rule it violates.

The document co-fires the connectedness arm of the same rule, because the
two-step path it writes walks `berger` twice and the second step therefore
starts at `Berger` while its opposite role `ploeg` is filled by
`Bergingsploeg`. That is admitted ("incidental extra violations are allowed")
and it is informative: it shows the walk is checked step by step, not merely
counted.

Verifier: `WF-108 FAIL /rules/resolvedRegels/0/voorwaarde/bronPad: bronPad beside
bronRol on a quantifier fold …` plus the connected-walk line on step 1.
