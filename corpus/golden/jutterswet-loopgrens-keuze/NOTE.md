# jutterswet-loopgrens-keuze — a Let binder compared with a value in another unit

One rule chooses a class for a Strook from its `aanlooplengte` (metres):

    schouwklasse = Let loop = aanlooplengte in (als de loop groter is dan 1 km dan 1 anders 2)

The condition of the `IfThenElse` compares the binder with a literal in kilometres. The binder carries the unit of
its value (RUNTIME-SEMANTICS ch. 2.6, 10.6), so the comparison restates: 1 km is 1000 m.

| case | aanlooplengte | correct reading | bare reading (500 > 1) |
|---|---|---|---|
| case1-korte-aanloop | 500 m | 500 > 1000 is false: **2** | true: 1, the other branch |
| case2-lange-aanloop | 2000 m | 2000 > 1000 is true: **1** | true: 1, the same branch |

## Provenance

Own work (card `clingo-let-binder-vergelijking-over-eenheden`, 2026-11-06), the form of the document `let-cross-unit`
under `backends/drools/src/test/resources/like-ref/` in the Jutterswet casus. The RegelSpraak frontend writes no
`IfThenElse`, so there is no source: the IR is the frontend's document of a placeholder rule with its value written
by hand. Measured at admission: verifier PASS; ref and both branches of the Drools backend play both cases; the ASP
backend of main `17f956d92` answered 1 in case 1, and plays both cases since that card.
