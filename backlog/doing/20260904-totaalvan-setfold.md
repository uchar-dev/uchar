# Set-fold "totaal van" over een tempo-eenheid: geldig, gepind, of uitgesloten?

Goal: lens 1 op de filter-LHS-kaart (2026-09-04) vond dat de enige vorm
waarin het aggregaattype en de foldbron-declaratie uiteenlopen — een
set-fold `het totaal van <€/mnd>` met §4.20-noemerstrip — nergens gepind
is, én dat de reference-interpreter die vorm niet kan evalueren
(`EVALUATION FAULT: incompatible units: EUR vs EUR`, ook zonder filter):
de frontend typeert een vorm die de oracle niet draagt, en geen gate
raakt het gat omdat het corpus de vorm niet kent. Voorvraag aan de
maintainer (genummerd, vóór het bouwen): is een set-fold `totaal van`
over een tempo-eenheid überhaupt een geldige vorm, of is `som van` de
set-fold en `totaal van` uitsluitend de temporele integratie? De
grammatica laat hem toe en de frontend typeert hem volwaardig
(noemerstrip onvoorwaardelijk).

## Het antwoord (maintainerbesluit 2026-09-04)

De set-lezing van `het totaal van` is een **alias van `de som van`,
ZONDER noemerstrip**. De noemerstrip blijft **exclusief voor de temporele
route** (de waardepertijdseenheid-aggregatie van §4.20 Tabel 30). De
toolchain accepteert de set-vorm al en die lijn volgen we; wat gecorrigeerd
wordt is de **typering op de set-route**. De twee sporen uit de oude kaart
(ref leert de vorm dragen / de frontend weigert de vorm) vervallen daarmee
allebei.

## De flank vóór de fix (rood-eerst, gemeten)

Minimale bron: één `€/mnd`-attribuut op Vondst, één set-fold op Jutter
(`het totaal van het tarief van alle vondsten van de Jutter`). Beide
doeldeclaraties gemeten, met de frontend van `main`:

```
doel €/mnd:  ERROR (line 17): berekening: toekenning:
             eenheden 'EUR/mnd' en 'EUR' zijn niet in elkaar om te rekenen
doel €:      ir: … geschreven   →   argument.type.eenheid = EUR/mnd
                                    functie.type.eenheid  = EUR   (noemer gestript)
             ref:  EVALUATION FAULT: incompatible units: EUR vs EUR
```

De eerlijke declaratie werd statisch afgewezen; de declaratie die door de
frontend kwam faalde bij de oracle. `ref/lib/arith.ml:60` (`restate_into`)
is de faultplek: de fold telt `EUR/mnd`-waarden op en het `EUR`-doel weigert
de toewijzing — `unit_name` toont alleen de teller, vandaar "EUR vs EUR".

## De routesplitsing (gemeten)

Er is geen aparte bouwroute: één builder maakt voor beide lezingen één
`Expr.Functie(TOTAAL, …)`. De routes scheiden op de **operandvorm**, en
alle bestaande plekken testen dezelfde velden:

* `ExpressieBouwer.gedurendeKenmerkFilter` (r. 713) — temporele route:
  `AttribuutRef … navigatieRol()==null && !a.set() && objecttype()==huidigSubject
  && attribuutGranulariteit.containsKey(…)`.
* de gedeclareerde-lid-check (r. 474) — set-route: `argumenten.get(0)
  instanceof AttribuutRef sa && sa.set()`.
* `totaalOperandFout` (r. 1158) — `temporeel` via `attribuutGranulariteit`.
* `ref/lib/eval.ml:951` splitst downstream op precies dezelfde vorm; alles
  wat niet in die arm valt gaat naar `Arith.agg_totaal` (r. 1157), de gewone
  instance-fold.

`functieType` was de **enige** plek die van geen van beide wist: de strip
vuurde onvoorwaardelijk voor TOTAAL. Dat was de fout.

## Aanpak (het gekozen spoor)

De set-route van TOTAAL typeert als SOM — geen strip, eenheid = elementeenheid
— en de temporele route blijft onaangeraakt. Eén predicaat
(`setLezing(args)`) op één wire-veld. Daarna een golden
`jutterswet-tempototaal` die de routesplitsing op de wire pint.

Acceptance:
- De genummerde voorvraag gesteld en beantwoord.
- Het gekozen spoor gebouwd, rood-eerst waar een pin bijkomt.
- Alle gates groen; herpin (frontend geraakt).

May touch: `frontends/regelspraak/src/**`, `corpus/golden/**`, `backlog/`.

Out of scope: de Drools- en clingo-kant (volgen de bestaande gates), de
verifier, de temporele route.
