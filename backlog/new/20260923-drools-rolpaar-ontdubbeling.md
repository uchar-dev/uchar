# drools-rolpaar-ontdubbeling: het creatie-beslisblok leeft twee keer

Gemeten door de creatie-koppelloos-lens (2026-09-23): de kaart
drl-creatie-koppelloos kopieerde het beslisblok van de
objectcreatie-terugkoppeling (binding eerst, typenpaar-terugval,
referentieveldguards, weigerzinnen) plus `verbindendeFeittypen` privé
uit `ExecModelTranspiler` naar `DrlTranspiler`, terwijl de eigen doc
van `FactModelGenerator.gebondenRolpaar` juist voor zo'n tweede
implementatie waarschuwt. Vandaag geen gemeten drift — de lens mat de
takken regel voor regel gelijk — maar de constructie nodigt drift uit;
het huispatroon is één gedeelde afleiding in `FactModelGenerator`
(zoals creatievelden, verdelingsjoin, navigatiewandeling).

## Acceptatie (bij oppakken aanscherpen)

- (a) Eén gedeelde beslisfunctie in `FactModelGenerator` (of
  `kern/`), door beide transpilers aangeroepen; de privékopieën
  verdwijnen.
- (b) De bestaande getuigen (`DrlCreatieKoppelingWitnessTest` 11
  rijen, `ObjectCreatieWitnessTest`, `DrlCreatieTakWitnessTest`)
  blijven groen zonder wijziging; een mutatieproef op de gedeelde
  functie raakt BEIDE takken rood.
- (c) Corpussweep byte-identiek, pins bewegen niet; alle veertien
  gates groen.

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten; zuivere herstructurering, geen gedragswijziging.

*Raakvlak (2026-10-08): `20261008-tms-aggregaties-creatie-en-fouten` (punt 4)
zet het beslisblok in `FactModelGenerator`; de kopie in `ExecModelTranspiler`
verdwijnt met `20261008-tms-omschakeling`, die in `DrlTranspiler` uiterlijk
met `20261008-drl-teksttak-op-truth-maintenance`. Deze kaart sluit mee met de
kaart die de laatste kopie weghaalt.*

## Result (2026-10-08)

Gedaan door `20261008-tms-aggregaties-creatie-en-fouten` (punt 4), commit `21bc5085`: het beslisblok van de
objectcreatie staat één keer, in `FactModelGenerator.creatiekoppeling` en `verbindendeFeittypen`, en de exec-modeltak
en de DRL-teksttak roepen het aan; de privékopieën zijn weg. (a) gehaald. (b) De bestaande getuigen blijven groen
zonder wijziging, en een mutant in de gedeelde functie maakt getuigen van beide takken rood. (c) De statusregels van
beide takken over alle 292 goldens zijn byte-gelijk vóór en na (jar van `42859692` tegen `21bc5085`: 0 verschillen per
tak, gemeten door de hoofdsessie); de pins bewogen niet; alle 16 gates groen op de eindcommit van die kaart. De
Raakvlak-regel hierboven ging ervan uit dat de kopieën pas met latere kaarten zouden verdwijnen; dat is achterhaald.
