# clingo-day-kind-walk-after-fold-stop: clingo meldt de dagsoortwandeling van een kwantorlid na de stop

*Vondst van de kaart `fold-contributor-verdicts` (2026-10-21), gemeten met een proefdocument op de kaarttak. Luid: clingo
geeft UNSATISFIABLE met een fout waar ref een antwoord geeft; geen stil fout antwoord. Geen golden draagt de vorm.*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.9a: "a member never reached produces no fault". In een samenstelling (ch. 5.10) bewaakt de
prefix-wacht van de clingo-backend een lid na de stop, ook bij een dagsoortwandeling (EF-009); in de matrix van een
kwantorvouw niet: het hulpatoom van de wandeling wordt voor elk lid van het bereik afgeleid. Proef (bewaard in
`uchar-dev/werk/defaults-decider/u5/probes/kwantor-dagsoortwandeling/`, `ir.json` en `case1.json`): een vouw
`alle [gekeurd ; <dagsoortwandeling zonder dag binnen de grens>]` waarvan lid 1 niet gekeurd is; ref geeft PASS, clingo
`UNSATISFIABLE — … viol("day-kind walk: no day of the kind within 40000 calendar days …","W2")`. De tegenproef in een
samenstelling (`u5/probes/samenstelling-dagsoortwandeling`) is in clingo groen.

Daarnaast, als ontwerp en buiten deze kaart: clingo geeft EF-011 (geen rekendatum) en EF-019 (een onopgeloste
voorwaarde) als vertaalfout, ook voor een lid dat nooit gelezen wordt; samen met Drools' lezing van een ongebonden
parameter als leeg (gate 8) is dat de reden dat geen golden een fout na de stop draagt (ch. 5.12, "The artifacts").

## Doel

De clingo-vertaling van de matrix van een kwantorvouw bewaakt de dagsoortwandeling van een lid met de stop van de vouw,
zoals de prefix-wacht dat in een samenstelling doet, zodat een lid na de stop geen EF-009 geeft.

## Acceptatie

- (a) Rood eerst: de proef als golden (kanaal `consistent`, met een controlecase waarin het lid wel bereikt wordt en
  EF-009 geeft, als rode case), rood in clingo vóór de fix, groen in ref en Drools.
- (b) Gates 5, 6, 8, 12 en 17 groen; de eigen suite van de clingo-backend groen.

May touch: `backends/clingo/`, één golden en één rode case, deze kaart.

Out of scope: EF-011 en EF-019 als vertaalfout (ontwerp van de clingo-backend).
