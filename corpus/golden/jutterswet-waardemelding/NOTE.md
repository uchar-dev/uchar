# jutterswet-waardemelding

The negated leeg/gevuld test (`IR-SPEC.md` ch. 4, Predicate row, field
`negated`; `RUNTIME-SEMANTICS.md` ch. 1.2). `negatie` is the inner,
per-value negation of the test: negated leeg is gevuld, negated gevuld is
leeg. The whole-period qualifier's own negation is a different field
(`GeheelPeriode.niet`, ch. 3.9) and does not occur here.

Three rules, each writing a constant under one negated predicate:

- meldpremie := 40 if the lengte of the Schuit is **niet leeg**
  (LEEG, `negatie: true`, own attribute);
- the Schuit is ongemeten if its lengte is **niet gevuld**
  (GEVULD, `negatie: true`, own attribute);
- werfpremie := 25 if the tarief of the werf of the Schuit is **niet leeg**
  (LEEG, `negatie: true`, one bound navigation step over `ligplaats`).

## Hand computation

Werven: W1 has tarief 312, W2 has none. Schuiten:

| schuit | lengte | werf | tarief of the werf | meldpremie | ongemeten | werfpremie |
|---|---|---|---|---|---|---|
| S1 | 8 | W1 | 312 | **40** | absent | **25** |
| S2 | leeg | W1 | 312 | leeg | **present** | **25** |
| S3 | 5 | — | leeg (no werf) | **40** | absent | leeg |
| S4 | leeg | W2 | leeg | leeg | **present** | leeg |

- meldpremie: lengte is niet leeg for S1 (8) and S3 (5), so both get 40;
  S2 and S4 have no lengte and stay leeg.
- ongemeten: lengte is niet gevuld exactly where it is leeg, S2 and S4.
- werfpremie: an absent werf reads leeg (ch. 1.2, absence is leeg), so S3
  fails the test as S4 does, whose werf W2 carries no tarief. S1 and S2
  both reach W1's 312 and get 25.

Twelve value rows plus `consistent`: 13 expectations.

## Why this golden exists

Every row is inverted by an executor that reads the soort and ignores the
field — the silent wrong answer this document pins against. Both Drools
branches did exactly that before the loader lowered the negation to the
sister soort. The ASP backend, before it carried the leeg test and its
negations in its shared condition translation, refused the document
instead. The unnegated twin and the unbound written step are pinned in the
backends' own suites (`LeegNegatieWitnessTest`, `test_clingo.ml`), not
here, because a golden carries one expectation set. The refusal under a
whole-period qualifier is the red twin `corpus/rood/leegnegatie-heel-tijdvak`.

No `bron.rgs`: RegelSpraak spells no "is niet leeg", and the frontend
never writes `negatie` on these two soorten. The document is hand-written
IR, and the rules illustrate the form; no Jutterswet article renders them.
