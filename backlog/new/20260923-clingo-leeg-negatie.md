# clingo-leeg-negatie: de ASP-vertaling kent de leeg-test en de negaties niet

Gemeten door de drools-leeg-negatie-kaart (2026-09-23):
`backends/clingo/lib/translate.ml` vertaalt in een regelconditie alleen
de ongeneerde GEVULD-vorm; LEEG, LEEG¬ en GEVULD¬ geven elk een
`TRANSLATION FAULT: condition kind not translated (no corpus case
exercises it)`. De Drools-takken dragen alle vier de vormen sinds die
kaart (loader-wissel naar de zustertest; heel-tijdvak benoemd
geweigerd); ref draagt ze al. Zolang clingo de vormen niet kent, kan de
getuige-fixture geen golden worden — gate 6 heeft geen weigerlijst en
zou rood staan.

## Acceptatie (bij oppakken aanscherpen)

- (a) De vertaler krijgt een regelconditie-arm voor LEEG (helper plus
  `not`) en voor de genegeerde vormen (zusterwissel zoals de
  Drools-loader, of rechtstreeks; heel-tijdvak volgt ref of weigert
  benoemd — meet ref eerst). Rood-eerst getuigen in de clingo-suite.
- (b) De fixture uit `backends/drools/src/test/resources/leeg-negatie/`
  verhuist als golden naar `corpus/golden/jutterswet-waardemelding`
  (NOTE met de handberekening uit `LeegNegatieWitnessTest`), zodat
  gates 5/6/12 de vorm blijvend bewaken over het hele vierkant.
- (c) Gate-12-pins bewegen +1 exec / +1 DRL-tekst met redenregel —
  benoemd beschermd-pad-voorstel.
- (d) Corpussweep: geen bestaand document beweegt; alle veertien gates
  groen.

May touch: `backends/clingo/**`, `corpus/golden/**` (de nieuwe golden),
`backends/drools/src/test/**` (de fixture-verhuizing, gemeld),
`scripts/check-drools.sh` (**voorstel**, alleen de pins),
kaartverplaatsing. NB: serieel met andere Drools/clingo-kaarten.
