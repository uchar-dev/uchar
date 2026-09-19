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

## Hermeetnotitie (2026-09-19, hoofdsessie, na de takteller-lens)

Twee onafhankelijke lensmetingen op de takteller-kaart weerleggen de
premisse "zonder zichtbare weigering" op het REPLAY-pad:
`DrlTranspiler.meet()` scant elke `// onopgelost`-commentaarregel in de
slice tot MARKER en de replay weigert daarop — gemeten tonen precies de
twee goldens die deze tak nemen (`anchor/jutterswet-wachtersketen`,
`synth2-budgetgrens-onder`) een `EXCLUDE drl-transpiler MARKER:
objectcreatie: … onopgelost terugkoppeling naar Wachter`, en sinds de
takteller-kaart staan zij reden-dragend in gate 12's `DRL_CHANNEL`. Ook
regel "de DRL-tak is niet het gate-12-pad" is sindsdien achterhaald: de
tak draagt nu een eigen gelijkheidspin in de gate. Wat wél staat: de
TRANSPILER zelf blijft stil (koppelloze `insert` in de gegenereerde
DRL; alleen de harnasscan redt het) en het GEDRAGSGAT tegenover exec —
exec lost de terugkoppeling via de wire-binding op en speelt — is echt.
Bij oppakken: eerst hertriëren en de kop plus acceptatie (a)
herformuleren op deze stand; (b), de binding lezen zoals exec, blijft
de kern. Los defect, eigen kaart: de vier goldens waar de
terugkoppeling WÉL oplost en het vuren faalt
(`20260919-drl-objectcreatie-defect.md`) — disjunct codepad, niet
samenvoegen.
