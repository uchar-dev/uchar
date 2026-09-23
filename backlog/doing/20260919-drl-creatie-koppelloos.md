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

Erbij belegd (lens op de objectcreatie-defect-kaart, 2026-09-19; zelfde
codegebied, zelfde weigerklasse — hier oppakken):

- **Weigeren bij naam waar exec dat doet.** Bij een gedeclareerd lid
  dat het referentieveld verdringt (naamdeling rol/lid) weigert exec de
  creatie bij naam (`transpiler SKIP: … deelt haar naam met een
  gedeclareerd lid`), maar de DRL-tak speelt stil door met een half
  geschreven koppeling — het voorwaartse schrijven blijft achter de
  `heeftReferentieVeld`-garde uit, zonder melding. Vandaag onschadelijk
  (geen golden draagt de vorm; elke lezer van het veld weigert zelf),
  maar het is de stilteklasse die het gate-12-regime uitsluit.
- **De fail-fast op een niet-booleaanse kenmerkwaarde.** Exec werpt
  luid (WF-027-fail-fast, `IR-SPEC.md` verwijst de vorm expliciet naar
  de runtime); de DRL-tak laat sinds de defect-kaart stil ongezet —
  vóór die kaart blies de klasse luid op bij het vuren. Een werpende
  helper is beschikbaar (de tak importeert de runtime-statics en
  `RegelSpraakRuntime` werpt zelf), dus dit is een keuze, geen
  onmogelijkheid; bouw de fail-fast of leg normatief vast waarom de
  takken hier verschillen.

## Hertriage (2026-09-23, bij oppakken, basis main 8784d5e8)

Uitkomst (i): alle drie de flanken staan nog. P2 dekte ze niet: P2
ging over de §13.4.7-FEITcreatie (`creatievelden`, `scopeVeld`), niet
over de §13.4.6-OBJECTcreatie. Exec leest de `verbindendeRol` van de
objectcreatie al sinds `drools-zelfgetypeerd-creatiepaar` (2026-08-30);
`DrlTranspiler.objectCreatie` leidt de terugkoppeling nog steeds alleen
af met `model.verbindendeRol(nieuw, subject)` en raakt
`rolBindingen.get(oc)` niet aan. Gemeten met de jar van main, per
proef verifier / ref / clingo / exec / DRL-tekst:

- (1) Koppelloze insert. `anchor/jutterswet-wachtersketen`
  (zelfgetypeerd): PASS / PASS 2+3 / PASS 2+3 / PLAYED 2 / EXCLUDE
  "MARKER: objectcreatie: werf de opvolger — onopgelost terugkoppeling
  naar Wachter". Proef herhaald paar (rekenkamer + tweede feittype
  `reservetaxatie` Jutter–Rekenkamer): PASS / PASS 2 / PASS 2 / PLAYED 1
  / EXCLUDE idem "naar Jutter". De gegenereerde DRL draagt in beide
  gevallen `// onopgelost terugkoppeling naar …` gevolgd door
  `insert($new);` en een CreationMarker-garde: de transpiler emitteert
  een koppelloze creatie, alleen de scan in `meet()` weigert haar.
- (2a) Naamdeling rol/lid. Voorwaarts (Numeriek `rekenkamer` op
  Jutter): PASS / PASS / PASS / EXCLUDE "transpiler SKIP: … deelt haar
  naam met een gedeclareerd lid" / PLAYED 1 — de DRL schrijft alleen
  de terugverwijzing en zwijgt. Terugwaarts (Numeriek `opdrachtgever` op
  Rekenkamer, nieuwe meting): PASS / PASS / PASS / EXCLUDE idem / EXCLUDE
  "drl compile: … Unable to Analyse Expression" — een weigering van het
  hele pakket die geen vorm noemt.
- (2b) Niet-booleaanse kenmerkwaarde (literal 5 op overzee): WF-027
  FAIL / EVALUATION FAULT "… is not boolean: 5 (WF-027)" / TRANSLATION
  FAULT / ERROR "… niet booleaans: 5 (WF-027)" / PLAYED 1 — de DRL
  gardeert met `Objects.equals(v, true)` en laat stil ongezet.

Gevolg voor gate 12: flank (1) dichten laat de twee goldens die
DRL_CHANNEL als "MARKER objectcreatie: unresolved back-link to
Wachter" pint in de DRL-tak spelen (verwacht 157 → 159). Dat is de kern
van de kaart zelf, geen bijwerking; het pin-diff op `scripts/` is
beschermd en gaat als voorstel in het Result.

## Acceptatie (herformuleerd op de gemeten stand)

- (a) Rood-eerst, beide takken via de replay-API: het herhaalde paar en
  het zelfgetypeerde paar spelen in de DRL-tak zoals in exec (waarden
  van ref); de naamdeling weigert in beide takken bij naam, aan beide
  zijden; de niet-booleaanse kenmerkwaarde faalt in beide takken luid
  met WF-027. Plus een tekstassertie dat de gegenereerde DRL nergens
  een `insert` zonder koppeling draagt.
- (b) `DrlTranspiler.objectCreatie` volgt exec: binding eerst
  (`FactModelGenerator.gebondenRolpaar`), typenpaar alleen zonder
  binding; beide uiteinden door `heeftReferentieVeld`; een creatie
  zonder schrijfbaar uiteinde weigert per construct met de reden van
  exec, nooit een koppelloze insert; de kenmerkwaarde werpt bij een
  niet-booleaanse waarde. Mutatieproef per flank, uitgevoerd.
- (c) Corpussweep beide takken vóór/na: exec byte-identiek; DRL-tekst
  alleen de twee genoemde goldens EXCLUDE → PLAYED. Pin-diff voor
  `scripts/check-drools.sh` als voorstel. Overige dertien gates groen.
- De bestaande rij `DrlCreatieTakWitnessTest.een_gelijknamig_lid_…`
  pint de divergentie die (2a) opheft en verwijst zelf naar deze kaart;
  zij wordt omgezet naar een tweetakkenweigering (gemeld mandaat).
