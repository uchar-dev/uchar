# drl-creatie-koppelloos: de objectcreatie zonder koppeling is stil

Gemeten (bereikafleiding-lens, 2026-09-19): bij een §13.4.6-objectcreatie
over een herhaald of zelfgetypeerd paar lost exec de terugkoppeling op
via de wire-binding (`FactModelGenerator.gebondenRolpaar`,
`ExecModelTranspiler.objectCreatie`), maar
`DrlTranspiler.objectCreatie` leidt haar uitsluitend af met
`model.verbindendeRol(nieuw, subject)` en raadpleegt de binding nooit.
Bij een niet-oplosbare `verbindendeRol` zet de DRL-tak géén marker: hij
emitteert een commentaarregel (`// onopgelost terugkoppeling naar …`)
en voert `insert($new)` tóch uit — een aangemaakt object zónder
koppeling, zonder zichtbare weigering. Geen getuige dekt dit
(`ObjectCreatieWitnessTest` draait exec-only, geen `BeideTakken`), en
de DRL-tak is niet het gate-12-pad, dus geen corpusrisico vandaag —
wel precies de stille-flankvorm die `20260919-exec-stille-guardflanken`
aan exec-zijde dichtte.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de stille route vastgelegd (commentaarregel +
  insert) op een verifier-schoon document met herhaald paar.
- (b) De DRL-tak leest de wire-binding zoals exec, óf weigert per
  construct met reden (nooit een koppelloze insert);
  dual-branchgetuige + mutatieproef.
- (c) Corpussweep beide takken: nul kanaalwisselingen (geen golden
  draagt de vorm — hermeten); alle veertien gates groen.

May touch: `backends/drools/**` (DrlTranspiler + tests),
kaartverplaatsing. NB: serieel met andere DrlTranspiler-kaarten
(drl-let-meldplek, drl-geneste-let-duw).
