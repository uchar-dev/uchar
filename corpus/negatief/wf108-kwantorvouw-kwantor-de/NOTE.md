Violates WF-108 (a) (`spec/IR-SPEC.md` ch. 4.5): the quantifier fold's `kwantor`
is `DE`. The bare definite is a description of ONE member — "de berger van de
Bergingsploeg" — and not a quantification over a set, so a fold over it has no
reading at all: there is no member list for the matrix to range over and no
verdict the node could give.

It is refused BY NAME rather than ignored, and that is the whole reason the node
records the quantor. A reader that met an unhandled `kwantor` and fell through to
the universal would answer ALLE for a sentence that wrote something else,
silently and with a straight face. `DE` is the one value of `Samengesteld`'s
shared enum that stays out here: ALLE, GEEN and the three counting quantifiers
are all admitted since 2026-09-16.

The schema pins the enum to the five admitted values, so this document is
deliberately schema-invalid as well — the negatief zone is gate 3's, not gate
2's, and a rule whose only enforcement were the schema would have no citable
violation.

Verifier: `WF-108 FAIL /resolvedRegels/0/voorwaarde/kwantor: quantifier fold with
kwantor DE: the bare definite is a description of ONE member, not a
quantification over the set — a fold over it has no reading`.
