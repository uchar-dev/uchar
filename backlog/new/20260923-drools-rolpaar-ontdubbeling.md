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
