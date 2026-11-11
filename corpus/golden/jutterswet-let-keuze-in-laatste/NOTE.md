# jutterswet-let-keuze-in-laatste — an IfThenElse as an operand of a listed fold in a Let body

"De toeslaggrens van een Jutter is de laatste van de grens als de grens groter
is dan 3, anders 0, en 1; daarbij is de grens de drempel van de Jutter." The
value is a `Let` whose body is a listed `last` fold (the ordering fold of
`RUNTIME-SEMANTICS.md` ch. 1.2e: the greatest operand) over two operands, the
first an `IfThenElse` on the bound value. WF-069 admits an `IfThenElse` inside
a `Let` body (`spec/IR-SPEC.md` ch. 4.10, the Phase-5c widening); an operand
of a fold inside that body is inside it. The condition reads only the binder,
which the leeg-barrier keeps filled (WF-077). Own Jutterswet-flavoured casus,
rights-free.

## Casus (own text)

> **Art. 9b (toeslaggrens).** De toeslaggrens van een jutter is zijn drempel
> als die drempel groter is dan 3, en anders 0; is de uitkomst kleiner dan 1,
> dan is de toeslaggrens 1.

## Cases (hand-computed, absolute)

| case | Jutter | drempel | IfThenElse | last of (…, 1) | toeslaggrens |
|---|---|---|---|---|---|
| `case.json` | J1 | 5 | 5 (5 > 3) | 5 | 5 |
| `case.json` | J2 | 3 | 0 (3 > 3 is false) | 1 | 1 |
| `case.json` | J3 | -4 | 0 | 1 | 1 |

The boundary of the comparison is J2: equal to 3 takes the else branch.

## Provenance

Witness of card `review-candidates-triage`, candidate B11: the verifier of
`ccf6de21c` walked the operands of a fold without the body position and judged
this `IfThenElse` at rule level (`WF-069.ifthenelse-outside-body`). Ref, clingo
and both Drools branches play the case as it stands.
