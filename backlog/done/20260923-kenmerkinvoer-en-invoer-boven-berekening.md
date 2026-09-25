# kenmerkinvoer-en-invoer-boven-berekening: wat `niet` tegenover invoer betekent, en een oppervlak voor "opgegeven, niet afgeleid" (vorm 3)

Besluit 7 van de onderzoekskaart dialect-defaults-niet-en-nesting
(maintainersbesluit 2026-09-23): een eigen kaart. Bronnen: het rapport
buiten de repo
(`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`,
§4.4 flank 2, §8.4, §9 besluit 7) en het voorstel
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §6 (kaartbranch).

Gemeten (ref ≡ clingo ≡ Drools-exec): een `besluit: false` neemt een
doelkenmerk dat in de casusinvoer staat nergens weg. ref zet
invoerkenmerken bij laden en kent geen intrekking; clingo zendt ze als kale
feiten `heeft(label,k).` uit, ook op een defaults-doel
(`backends/clingo/lib/translate.ml:8797`; invoer*attributen* op een
defaults-doel gaan wél via `inputval/3`, r. 8793); de Drools-groepsregel
schrijft bij `false` niets (`ExecModelTranspiler.java:7649`). Dat is
consistent met ch. 7.1 ("not a retraction") en met §3.5 van de spec
(invoer mag een kenmerk zetten, regels voegen toe), maar ch. 7.8's virtuele
invoer-default is voor kenmerkdoelen in de onwaar-richting leeg, terwijl hij
voor attributen wél door een beslissende default wordt verdrongen
(ch. 7.6.2). Daarnaast wil de werkbank afgeleide waarden als invoer kunnen
aannemen en een afleiding laten overrulen (Catala's `context`, Reading B,
PHASE5-CATALA besluit 6); dat is vandaag alleen via een subscope-aanroep
bereikbaar, niet vanuit de casusinvoer.

## Te beslissen door de maintainer (de kaart levert de analyse en een voorstel)

- (7-i) Vastleggen wat is: ch. 7.8 krijgt de zin dat de virtuele
  invoer-default op een kenmerkdoel door geen besluit wordt verdrongen,
  omdat de groep nooit `onwaar` schrijft; de ankercase
  `case7-invoer-staat-tegen-blokkade` (kaart
  anker-ww-herleving-dragervoorwaarde) pint het. Nul code.
- (7-ii) Kenmerkinvoer defeasibel maken (Reading A ook voor kenmerken):
  clingo `heeft(S,k) :- inputchar(S,k), not grpG_eff(S)` op defaults-doelen;
  ref houdt invoerkenmerken van defaults-doelen apart en laat ze alleen
  staan als de groep niet beslist; Drools idem. Een getrackte delta
  (ch. 7.8, `CONSTRUCTS.md`), drie uitvoerders, de ankercase krijgt
  verwachting afwezig.
- (7-iii) Een oppervlak voor Reading B: een markering op het doel ("mag
  opgegeven worden") die uit de casusinvoer een hoogste default genereert,
  zoals het Catala-frontend dat voor `context` via de subscope-aanroep doet;
  met een uitlegkanaal ("opgegeven, niet afgeleid") en een keuze wat er
  gebeurt als feiten en opgave elkaar tegenspreken (een §9.5-feit, of stil
  de opgave). Per attribuut of per groep.

(7-ii) en (7-iii) staan los van elkaar; (7-i) is de minimale vastlegging.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een voorstel (sectie in `DIALECT-DEFAULTS-NIET-EN-NESTING.md` of eigen
  bestand in `spec/proposals/`) met de drie opties, de wisselwerking met
  ch. 7.8 en PHASE5-CATALA besluit 6, de spelling van de markering voor
  (7-iii), en de gemeten stand per uitvoerder.
- (b) De maintainersbesluiten vastgelegd; de zin voor ch. 7.8 als
  **voorstel** (beschermd pad); de bouwkaarten voor de gekozen opties
  gesneden.
- (c) Geen code; alle gates ongewijzigd groen.

May touch: `spec/proposals/**`, `spec/RUNTIME-SEMANTICS.md` (**voorstel**,
één zin), `backlog/**`.

Niet in deze kaart: de uitvoering van (7-ii) of (7-iii).

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Regelnummers (`translate.ml:8797`, `ExecModelTranspiler.java:7649`) zijn
  van 2026-09-23; zoek op `inputval`/`inputchar` en `compileDefault`.
- Wire-namen: `besluit` → `Default.decision`; `Default {decision?,
  justification?, consequence?, initialisation?, exceptions?}`;
  `PrioritizedDefaults {name, subject, characteristic | attribute,
  unordered?, defaults}`; `ALLE` → `Compound {quantifier: "all"}`; `bron` →
  `source {snippet}`. De ASP-predicaten (`inputval/3`, `heeft/2`) en de
  Drools-namen zijn geheugennamen, geen wire-namen.
- Het voorstel DIALECT-DEFAULTS-NIET-EN-NESTING.md is geland op main; §6
  (r. 920–990) geeft de richtingen; §10 besluit 1 BEHIELD "een negatieve
  beslissing verwijdert nooit een invoerkenmerk" (RegelSpraak §3.5
  invoer-of-toekenning).
- De ankercase `case7-invoer-staat-tegen-blokkade` bestaat
  (`corpus/golden/anchor/ww-herleving-inkomen/`, tien cases).
- Het rapport buiten de repo: §4.4 r. 105–128, §8.4 r. 410–419, §9
  r. 420–435 van
  `../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`.
- Het voorstel komt in een EIGEN bestand
  `spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` (niet in
  DIALECT-DEFAULTS-NIET-EN-NESTING.md: dat bewerkt de kaart
  dialect-uitzondering-op-uitzondering tegelijk).

## U-besluiten (hoofdsessie, 2026-09-25; standaardantwoorden, aan de maintainer gemeld)

- U1 **(7-i) JA.** RS ch. 7.8 krijgt één zin (**voorstel**): de virtuele
  invoer-default op een KENMERKdoel wordt door geen beslissing verdrongen —
  een `decision: false` schrijft niets (ch. 7.1: geen intrekking; RegelSpraak
  §3.5) — anders dan op een attribuutdoel, waar een beslissende default de
  invoerwaarde verdringt (ch. 7.6.2); `case7-invoer-staat-tegen-blokkade`
  pint het. Geen D-/U-nummers, geen procestaal.
- U2 **(7-ii) NEE.** Kenmerkinvoer blijft niet-defeasibel: dat is precies wat
  besluit 1 van 2026-09-23 behield. Het voorstel legt de asymmetrie en haar
  reden vast en noemt (7-ii) als verworpen alternatief, met wat het zou
  kosten (ch. 7.8-delta, drie uitvoerders, de ankerverwachting omgekeerd).
- U3 **(7-iii) VOORSTEL, geen bouw.** Suiker over het hardheidspatroon
  (PHASE4-DEFAULTS §4.1; de `context`-lowering van het Catala-frontend,
  PHASE5-CATALA besluit 6): een markering op de attribuutdeclaratie in het
  dialect (spelling voorstellen, met de inertheidsvraag), die de frontend
  lowert tot een hoogste default die de casusinvoer via een tweede attribuut
  `opgegeven <naam>` leest — géén wire-verandering, géén ch. 7.8-wijziging;
  uitleg = de naam van het beslissende blok; tegenspraak feiten/opgave = de
  gematerialiseerde berekende waarde plus een consistentieregel (§9.5-feit).
  Het voorstel weegt ook de semantische variant (een invoer-default bóven de
  gewone defaults, per doel) en zet haar als alternatief neer. De bouwkaart
  voor de suikervorm staat als TEKST in het Result (de hoofdsessie snijdt
  hem; bouwers maken geen kaartjes); of hij vóór of ná de pin-bump komt is
  het sein van de maintainer.
- U4 **Gemeten stand** per uitvoerder (`ref`, clingo, Drools exec en DRL) op
  het WW-anker case7 en op een klein eigen proefdocument (L5-wire) met een
  `decision: false` tegenover een invoerkenmerk, commando's en uitkomsten in
  het voorstel; proefbestanden buiten de repo.
- U5 Nul code; gate 1 en de naamtabel-toets als sanity; de unie draait alle
  veertien.

## Result (2026-09-25, bouwer)

## Result — kenmerkinvoer-en-invoer-boven-berekening

### Stand

- Branch `card/kenmerkinvoer-en-invoer-boven-berekening`, worktree
  `/Users/yeb/Developer/uchar-dev/wt/kenmerkinvoer`, basis main 2f47d80e, claim 0063f24b.
- Kop **fbf5a264**. Eigen commits (klein, bouwbaar, niet gepusht, niet gemerged):
  - 2f4175d0 kenmerkinvoer: de gemeten stand van invoer op een defaults-doel (stap 1)
  - de4d2dbb kenmerkinvoer: opties, besluiten en de markering voor opgave boven berekening (stap 2)
  - fbf5a264 RUNTIME-SEMANTICS ch. 7.8: kenmerkinvoer staat tegen elk besluit (voorstel) (stap 3)
- Gewijzigd t.o.v. de claim: `spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md`
  (nieuw, 1003 regels, Engels) en `spec/RUNTIME-SEMANTICS.md` (+11/−1: één zin, **voorstel**,
  beschermd pad). Nul code, geen script, geen corpus, geen schema, geen naamtabel. De kaart
  zelf niet bijgewerkt; geen kaartje in backlog/new.

### De RS-zin (ch. 7.8, eerste alinea, na "…contributes no dependency edges.") — letterlijk

> On a characteristic target no decision removes what the virtual input default holds: a
> do-not-attribute decision displaces the lower defaults and derives nothing, which is not a
> retraction (ch. 7.1), so a characteristic the case input sets stays set — the input is a
> source of the characteristic beside the attribution (§3.5) — whereas on an attribute target a
> deciding default replaces the input value (ch. 7.6.2); worked, absolute, through all three
> executors (gates 5, 6 and 12): the anchor `ww-herleving-inkomen/case7-invoer-staat-tegen-blokkade`,
> whose blocking default decides do-not-attribute over an input `recht op uitkering`, leaves
> that characteristic **present**.

De regel met "This is a re-description …" is alleen herwikkeld; zijn tekst is ongewijzigd.
Getoetst per bijzin tegen de meting (voorstel §4.1, tabel): false-besluit (case7, A case2,
A2 case3), true-besluit (A case3), conflict (A2 case2), verdringing zonder invoer (A case1),
attribuut verdrongen (A case2/case3, vondstwaardering case6). Niets in de zin weerspreekt een
meting. Geen D-/U-nummers, geen procestaal.

### Meting (U4), 2026-09-25, op builds van 0063f24b (code = main 2f47d80e)

Builds: `eval $(opam env)`; `dune build ref backends spec/verifier`; `dune build --root
spec/verifier`; `mvn -o -B -q -f backends/drools/pom.xml package -DskipTests` (één keer);
`dune build frontends/catala`. Aanroepen als gates 3/5/6/12: `spec/verifier/_build/default/bin/verifier.exe IR`,
`_build/default/ref/bin/runner.exe IR CASE`, `_build/default/backends/clingo/bin/clingo_runner.exe IR CASE`,
`java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar [--drl] IR CASE`.
Proefbestanden en logs: (buiten de repo),proef-a,proef-a2,proef-b,proef-b2,proef-b3,catala,log,asp}`.

| Document / cases | verifier | ref | clingo | Drools exec | DRL |
|---|---|---|---|---|---|
| anker ww-herleving-inkomen case7 (aanwezig) + case3 | PASS | 2 PASS | 2 PASS | 2× PLAYED 1 | EXCLUDE (defaults-zin) |
| case7 omgekeerd (afwezig) | – | MISMATCH | MISMATCH | FAIL | EXCLUDE |
| proef A (kenmerk- én attribuutdoel, zelfde vorm), 4 cases | PASS | 4 PASS | 4 PASS | 4× PLAYED 1 | 4× EXCLUDE |
| proef A flips (kenmerk / attribuut omgekeerd) | – | 2 MISMATCH | 2 MISMATCH | 2 FAIL | EXCLUDE |
| proef A2 (kenmerkconflict, twee ongeordende groepen), 3 cases | PASS | 3 PASS | 3 PASS | 3× PLAYED 1 | 3× EXCLUDE |
| proef A2 flip | – | MISMATCH | MISMATCH | FAIL | EXCLUDE |
| proef B (7-iii-lowering, consistentie met `atLeast` 1), 7 cases | PASS | 7 PASS | 7 PASS | 7× EXCLUDE (consistentie buiten Phase-D scope) | 7× EXCLUDE |
| proef B flips | – | 3 MISMATCH | 3 MISMATCH | EXCLUDE | EXCLUDE |
| proef B2 (idem, bewaking als gegenereerd kenmerk), 8 cases | PASS | 8 PASS | 8 PASS | 8× PLAYED 1 | 8× EXCLUDE |
| proef B2 flips | – | 3 MISMATCH | 3 MISMATCH | 3 FAIL | EXCLUDE |
| proef B3 (B2 + initialisatie op het gemarkeerde attribuut), 5 cases | PASS | 5 PASS | 5 PASS | 5× PLAYED 1 | 5× EXCLUDE |
| proef B3 flips | – | 2 MISMATCH | 2 MISMATCH | 2 FAIL | EXCLUDE |
| Catala top-level `context` (frontend-IR), case drempel 7 → uitkomst 40 | PASS | PASS | PASS | PLAYED 1 | EXCLUDE |
| idem omgekeerd (43) | – | MISMATCH `K.uitkomst = 40, expected 43` | MISMATCH | FAIL | EXCLUDE |
| idem via `clerk run --scope Test` met aanroeper-override 7 | – | – | – | – | catala 1.2.1: `uitkomst = 43` |

Tellingen: 6 eigen IR-proefdocumenten + het anker; 30 cases met ware verwachting (alle PASS in
ref en clingo; Drools exec 23 PLAYED, 7 EXCLUDE = proef B), 13 flips (alle falen in ref en clingo;
Drools exec 10 FAIL, 3 EXCLUDE = proef B); DRL-tak weigert alle 43 op zijn defaults-zin.
Kern (proef A case2): hetzelfde blokkerende besluit geeft attribuut 0.00 over invoer 40.00, en laat
het invoerkenmerk staan. Corpus: 8 goldens met een kenmerk-defaultsdoel, 1 case zet dat doel in de
invoer (case7); `corpus/rood` en `toka/` dragen geen defaults; (7-ii) zou precies case7 omdraaien.

### Technische keuzes (zelf genomen, vastgelegd in het voorstel)

1. Markering `(mag opgegeven worden)`, één lexertoken met haakjes (patroon `(bezield)`), laatste
   element van `attribuutSpecificatie`; ook toegelaten na een kenmerkdeclaratie om daar gericht te
   weigeren. Eerste inertheidstelling: 0 treffers in 120 `.rgs` (73 goldens, 47 toka) en in de zes
   md-documenten van de v2.3.0-spec; `opgegeven`/`berekende` daar alleen in proza en tabellen.
2. **Verfijning van de U3-schets:** de tegenspraak blijft "gematerialiseerde berekening +
   consistentieregel (§9.5-feit)", maar de bewaking "beide gevuld en ongelijk" krijgt een
   gegenereerd kenmerk `afwijkende opgave X` (toegekend onder `all`), en de consistentie toetst dat
   kenmerk ontkend. Reden (gemeten, proef B): de Drools-exectak lowert geen consistentie met een
   `atLeast`-assertie (`ExecModelTranspiler.java:5577`, reden `:5734`). Bijvangst: het kenmerk is
   regel-leesbaar, het §9.5-feit niet.
3. Een auteursinitialisatie van X blijft op X als initialisatie-default onderaan de gegenereerde
   groep (gemeten, proef B3); op `berekende X` zou zij altijd vullen en valse afwijkingen geven.
4. Gegenereerde namen: `opgegeven X`, `berekende X`, `afwijkende opgave X` (bezittelijk), groep
   `opgave boven berekening X`, regel `afwijking opgave X`, consistentie `controle opgave X`;
   botsing = ERROR met beide plekken.
5. Eerste bouwkaart alleen Numeriek zonder eenheid (de corpus draagt als attribuut-defaultsdoel
   alleen Numeriek: 28 van 28, geen met eenheid).
6. Plaats van de RS-zin: in de definitie-alinea van ch. 7.8, zodat de volgende zin ("This is a
   re-description …") waar blijft.

### Gates (elk als losse aanroep na `eval $(opam env)`)

- `sh scripts/check-self-contained.sh` → `check-self-contained: PASS` (rc 0)
- `python3 spec/proposals/v1-naamtabel-toets.py` → `v1-naamtabel-toets: OK` (rc 0)
- Gewijzigde scriptregels: geen. Weigerzinnen: geen (nul code). CONSTRUCTS-registerzinnen: geen —
  (7-i) legt gelande semantiek vast zonder delta, (7-ii) is verworpen, (7-iii) is niet gebouwd; de
  bouwkaart vraagt haar eigen registerzin in acceptatie (e).
- Geen nulmeting: geen golden, gate-12-kanaal of rood-fragment bewoog.

### Wat aan de kaarttekst niet klopte (gemeten)

1. Regelnummers (zoals de claim al meldde): clingo `translate.ml:8797` is nu `:10296` (kale
   `heeft/2`-feiten), `:8793` is `:10292` (`inputval/3`); Drools `ExecModelTranspiler.java:7649` is
   `:7671`.
2. (7-ii) noemt `heeft(S,k) :- inputchar(S,k), not grpG_eff(S)`: een kenmerkgroep heeft in clingo
   vandaag geen `grpN_eff`-atoom (alleen de attribuuttak, `:9589-9602`); (7-ii) zou een nieuw atoom
   vergen dat "beslist, welke kant ook, zonder conflict" draagt.
3. "ch. 7.8's virtuele invoer-default is voor kenmerkdoelen in de onwaar-richting leeg": de uitkomst
   wordt al door ch. 7.1 beslist ("not a retraction" en "Input facts are **not** a default: they are
   the fact base the group's write meets", r. 4926–4927); wat ontbrak is dat ch. 7.8's herbeschrijving
   dat niet zegt. De RS-zin sluit precies dat.
4. "Reading B is vandaag alleen via een subscope-aanroep bereikbaar": klopt, maar gemeten ernaast:
   een Catala-`context`-variabele van een scope die niemand aanroept wordt NIET geweigerd (anders
   dan het kopcommentaar `translate.ml:34`, `:51-59` zegt) en krijgt uit de casus Reading A (40
   tegen catala's 43).
5. De U3-schets van de tegenspraak (één consistentieregel) is in de Drools-exectak in de voor de
   hand liggende `atLeast`-vorm niet gedragen; zie keuze 2.
6. Verder klopte alles wat de kaart en de claim-correcties beweren: case7 bestaat (tien cases),
   ref ≡ clingo ≡ Drools-exec houden het invoerkenmerk, de rapportregels §4.4/§8.4/§9 kloppen.

### Bouwkaarttekst (voor backlog/new/20260925-dialect-opgave-boven-berekening.md; ook voorstel §6)

## dialect-opgave-boven-berekening: de markering `(mag opgegeven worden)` op een attribuut (vorm 3, suiker)

Bouwkaart uit de kaart kenmerkinvoer-en-invoer-boven-berekening (optie
7-iii: voorstel zonder bouw, hoofdsessie 2026-09-25). Bron:
`spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` §5. Na de
kaart dialect-uitzondering-op-uitzondering (zelfde resolverbestanden, dus
serieel). Of zij vóór of ná de pin-bump van regelspraak-java komt, is het
sein van de maintainer.

Een werkbankgebruiker wil een afgeleid attribuut als invoer kunnen opgeven
en de afleiding laten wijken ("opgegeven, niet afgeleid"; Reading B),
zonder dat casusinvoer als geheel van Reading A afwijkt (RUNTIME-SEMANTICS
ch. 7.8; PHASE4-DEFAULTS §10 besluit 1). Het hardheidspatroon
(PHASE4-DEFAULTS §4.1) doet dat vandaag als programmastructuur, het
Catala-frontend genereert het voor `context` (PHASE5-CATALA besluit 6).
Deze kaart geeft het RegelSpraak-dialect er een declaratie voor. Geen
wire-, spec-, verifier- of uitvoerderswijziging: de lowering hieronder is
met de hand op de L5-wire gemeten en speelt in ref, clingo en de
Drools-exectak (voorstel §5.4, proeven B2 en B3: dertien cases, elk met
een omgekeerde tegencase).

### Ontwerp (voorstel §5.1–5.6)

- Spelling: `(mag opgegeven worden)` als laatste element van een
  attribuutdeclaratie, vóór de `;`:

      de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);

  Eén nieuw lexertoken `MAG_OPGEGEVEN_WORDEN : '(mag' WSP 'opgegeven' WSP
  'worden)'`, geheel met haakjes zoals `BEZIELD`, `BEZITTELIJK` en
  `BIJVOEGLIJK`; de grammatica laat het ook toe aan het eind van een
  kenmerkdeclaratie, zodat de resolver daar gericht weigert. Het token
  reserveert geen naamwoord.
- Lowering voor een gemarkeerd attribuut X van objecttype T:
  1. attribuut `opgegeven X` op T, datatype van X; geen schrijver, de
     casus levert het;
  2. attribuut `berekende X` op T, datatype van X; elke rekenende
     schrijver die de auteur voor X schreef (gelijkstelling, berekening,
     beslistabelrij, en de defaults-groepen van `Uitzondering op` en
     `Uitzonderingen ongeordend`) krijgt `berekende X` als doel; naam en
     vorm blijven, een ongeordende set verhuist als set;
  3. één `PrioritizedDefaults` op X met naam `opgave boven berekening X`,
     van hoog naar laag: de opgave-default (rechtvaardiging de lege
     `Compound` met `quantifier: "all"` en `source.snippet` "altijd",
     consequentie `opgegeven X`, `source.snippet` `opgegeven X`), de
     rekendefault (idem met `berekende X`) en, als de auteur een
     initialisatie voor X schreef, die initialisatie als
     `initialisation`-default onderaan: zij blijft op X;
  4. bezittelijk kenmerk `afwijkende opgave X` op T, toegekend door de
     regel `afwijking opgave X` onder `Compound` `all` van: `opgegeven X`
     gevuld, `berekende X` gevuld, `opgegeven X` ongelijk aan
     `berekende X`;
  5. consistentie `controle opgave X` met als assertie het kenmerk
     `afwijkende opgave X`, ontkend.
  Lezers van X elders blijven X lezen.
- Uitleg: de beslissende default draagt zijn bloknaam in `source.snippet`
  (`opgegeven X` of `berekende X`); `berekende X` houdt de berekende
  waarde vast, ook als de opgave beslist.
- Tegenspraak: opgave en berekening beide gevuld en ongelijk geeft één
  §9.5-feit `controle opgave X` × onderwerp plus het kenmerk; een opgave
  zonder berekenbare waarde is geen tegenspraak.
- Casusinvoer op X zelf blijft Reading A (ch. 7.8 ongewijzigd; voorstel
  §5.4 cases 6 en 7).

### Weigeringen (ERROR op de regel van de declaratie) en één waarschuwing (voorstel §5.7)

- de markering op een kenmerk (de positieve richting geldt zonder
  markering, ch. 7.8; de negatieve vraagt de driewaardige opgave van
  voorstel §5.8, eigen kaart);
- de markering op een attribuut dat geen Numeriek zonder eenheid is (de
  corpus draagt als attribuut-defaultsdoel alleen Numeriek, 28 van 28,
  geen met eenheid); een ander datatype of een eenheid pas met een eigen
  golden die in de drie uitvoerders speelt;
- de markering op een tijdsafhankelijk attribuut (WF-086) of op een
  gedimensioneerd attribuut;
- een verdeling of objectcreatie-initialisatie die X schrijft;
- een schrijver van X op een super- of subtype (ch. 7.10);
- een gegenereerde naam die het model al declareert of gebruikt, met beide
  plekken in de melding.
- WARNING: de markering op een attribuut dat geen regel schrijft (al
  invoer; er wordt niets gegenereerd).

### Acceptatie (bij oppakken aanscherpen)

- (a) Inertheid gemeten, niet aangenomen (de methode van `DIVERGENCES.md`
  §6): elk vlak RegelSpraak-document dat de werkbank bereikt, door het
  frontend vóór en na het token, IR plus diagnostics vergeleken; de 73
  `bron.rgs`-goldens byte-identiek via gate 10.
- (b) Rood-eerst JUnit-getuigen: de vijf stukken van de lowering (namen,
  volgorde, `source.snippet`), elke weigering en de waarschuwing, met de
  weigerzinnen letterlijk.
- (c) Een golden met `bron.rgs` (werknaam
  `jutterswet-opgave-boven-berekening`, eigen Jutterswet-casus) met de
  cases van voorstel §5.4 (geen opgave, gelijk, afwijkend met het
  §9.5-feit, opgave zonder feiten, niets, X zelf in de invoer met en zonder
  feiten, initialisatie), groen in ref ≡ clingo ≡ Drools-exec. Gate 12:
  `EXPECTED_PLAYED` +1 en de golden in klasse (A) van `DRL_CHANNEL` (de
  DRL-teksttak heeft geen defaults-lowering); gate 7: de cases die een
  niets-doende uitvoerder haalt met reden op de overleverslijst. Beide
  scripts beschermd: **voorstel** op de branch.
- (d) `DIVERGENCES.md`: een eigen sectie voor de markering (spelling,
  lowering, weigeringen, de inertheidsmeting).
- (e) De registerzin voor `spec/CONSTRUCTS.md` in het Result (het dialect
  krijgt een oppervlak voor het hardheidspatroon; geen wire-delta); de
  hoofdsessie landt hem.
- (f) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/<nieuwe golden>/**`,
`scripts/check-drools.sh` en `scripts/check-noop.sh` (**voorstel**),
kaartverplaatsing.

Niet in deze kaart: de markering op een kenmerk (voorstel §5.8); de
semantische variant (voorstel §5.9); de uitleg in regelspraak-java en de
werkbank (kaart regelspraak-java-defaults-in-scenario-en-werkbank, gat 3);
het Catala-frontend (voorstel §5.10); de `atLeast`-arm in de
consistentielowering van de Drools-exectak (voorstel §5.6, alternatief d).

### Open punten

1. **Anker-NOTE** `corpus/golden/anchor/ww-herleving-inkomen/NOTE.md:364-368` (beschermd, buiten
   may-touch) noemt case7 "voorlopig" tot deze kaart beslist en noemt opties 7-i/7-ii; na landing
   van de RS-zin kan de alinea naar ch. 7.8 verwijzen.
2. **Gate-7-overleverreden case7** `scripts/check-noop.sh:157-167` (beschermd): "Pinned as long as the
   card … does not choose its option 7-ii"; met 7-ii verworpen kan de reden naar ch. 7.8 verwijzen.
3. **Verouderd kopcommentaar Catala-frontend** `frontends/catala/lib/translate.ml:34` en `:51-59`
   ("context … REFUSED"); de code lowert context sinds besluit 6 (`:1297-1302`, `:1812-1847`).
4. **Catala-`context` van een scope die niemand aanroept**: de casus krijgt Reading A (gemeten 40
   in ref, clingo en Drools-exec; catala via `clerk run` met override 43). Nergens besloten of dat
   een vastgelegde flank is, een weigering, of een plek voor de §5.3-lowering in het Catala-frontend.
   Geen corpuscase raakt het.
5. **De kenmerkmarkering** (voorstel §5.8, driewaardige opgave via een Boolean `opgegeven <kenmerk>`)
   wacht op een wettekst die "opgegeven: niet aanwezig" boven een berekende toekenning vraagt; niet
   gemeten.
6. **Drools-exec lowert geen consistentie met een ander compound dan `all`**
   (`ExecModelTranspiler.java:5577`, "other compound: later"): elke zo'n consistentie wordt
   uitgesloten, ongeacht welke kaart haar schrijft. Gevonden met proef B; geen golden draagt er
   vandaag een.
7. **Uitleg** (voorstel §5.5) hangt aan de regelspraak-java-kaart
   `backlog/new/20260923-regelspraak-java-defaults-in-scenario-en-werkbank.md` (gat 3); die kan de
   `source.snippet` van de gegenereerde defaults als bron voor "opgegeven, niet afgeleid" nemen.
8. **Verlopen regelverwijzing** in `backlog/new/20260923-verifier-supertype-dubbele-schrijver.md:10`
   (`RUNTIME-SEMANTICS.md:5179–5184`): die tekst (ch. 7.10, "A rule is absorbed only …") staat op
   main al op r. 5487 en na deze branch op r. 5497. Niet aangeraakt (andermans kaart).
9. **Volgorde en sein**: of de bouwkaart vóór of ná de pin-bump van regelspraak-java komt, is het
   sein van de maintainer; zij gaat na dialect-uitzondering-op-uitzondering (zelfde resolverbestanden).

## Lens en fixronde (2026-09-25, hoofdsessie)

**Lens A (spec/verifier-kwartet, export van de unie d43eb002; oordeel EERST FIXEN).** Voor deze kaart: B7 — de anker-NOTE van ww-herleving-inkomen en de overleverreden in check-noop.sh worden onwaar zodra de zin in ch. 7.8 landt; A8 — de nieuwe zin telt ~110 woorden en de volgende zin verloor haar antecedent; A14 — in het voorstel een gedateerde telling, proefnamen die met de delta-ID's botsen en een pad naar backlog/doing; A17.3 — normvraag: een Catala-`context` in een scope die niemand aanroept krijgt Reading A (40 tegen catala's 43); A17.7 — het kopcommentaar van het Catala-frontend zegt dat `context` geweigerd wordt. Bevestigd: de besluiten 7-i/7-ii/7-iii juist weergegeven; de kernmeting (case7, proef A) herhaald; de markering `(mag opgegeven worden)` inert op 125 bronnen; geen D-/U-nummers.

**Besluiten van de hoofdsessie.** B7: NOTE en overleverreden verwijzen naar ch. 7.8 (voorstel); A8: de zin gesplitst; A14 en A17.7 gefixt; de bouwkaart `dialect-opgave-boven-berekening` staat in backlog/new (timing ten opzichte van de pin-bump: het sein van de maintainer); A17.3 blijft een normvraag voor de maintainer met standaardantwoord "vastgelegde flank, geen weigering".

**Fixronde (fixer A-exec).** B7: de anker-NOTE van ww-herleving-inkomen en de overleverreden van case7 in `scripts/check-noop.sh` citeren de zin van ch. 7.8 en noemen de keuze 7-i; A14: het voorstel telt geen goldens meer, de proeven heten *lowering proof*, *initialisation proof* en *guard proof* (P-namen botsen met de P-ID's van de wire-migratie), het kaartpad wijst naar done/, de regelnummers van ExecModelTranspiler en het Catala-frontend zijn meegeschoven, de open punten 1–3 zijn als opgelost gemarkeerd; A17.7: het kopcommentaar van `frontends/catala/lib/translate.ml` beschrijft de context-lowering van besluit 6. De bouwkaart `backlog/new/20260925-dialect-opgave-boven-berekening.md` is de tekst van voorstel §6 bij de landing.

**Fixronde (fixer A-spec).** A8: de zin in ch. 7.8 is gesplitst — "Worked, absolute, …" is een eigen zin en de volgende begint met "The virtual input default is a re-description …"; inhoud ongewijzigd.
