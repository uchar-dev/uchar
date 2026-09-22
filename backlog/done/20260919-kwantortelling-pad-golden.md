# kwantortelling-pad-golden: de telling over een meerstapsbereik krijgt haar getuige

Gemeten (aantal-setbron-lens, 2026-09-19): een tellende kwantor
(`TEN_MINSTE n`) over een tweestaps `bronPad` (Jutter→Boot→Vondst) is
verifier-SCHOON — WF-108 (c) laat `bronPad` op de kwantorvouw toe en
`RUNTIME-SEMANTICS.md` ch. 3.9a deelt daar het bereik van ch. 3.4a/3.4b
— en wordt door `ref` én clingo beantwoord. Het lensdocument scheidde
bovendien de VLAKKE UNIE van de geneste lezing: met één via twee
tussenexemplaren bereikbaar element telt vlak 3 distinct (kenmerk
afwezig bij drempel 4) waar genest 4 voorkomens zou tellen (aanwezig).
Beide OCaml-uitvoerders antwoordden vlak — maar niets pint dat: er is
geen corpus-golden met een tellende kwantor over een meerstapsbereik,
en de versmalde WF-102-COUNT-passage in `spec/IR-SPEC.md` ("niet
uitdrukbaar als AANTAL-vouw"; de kwantorroute is de knoop die wél telt)
staat daarmee zonder positieve getuige — precies wat de huisregel
verbiedt zodra iemand de positieve claim opschrijft.

## Acceptatie (bij oppakken aanscherpen)

- (a) Eén handgeschreven golden (of frontend-geëmitteerd, als de
  producentroute bestaat — meten): een tellende kwantor over een
  tweestaps `bronPad` met een gedeeld element, handberekende
  verwachtingen die de vlakke unie pinnen (het gedeelde element telt
  één keer), casus met beide uitkomsten van de drempel.
- (b) Gespeeld door ref en clingo; Drools gespeeld of benoemde EXCLUDE
  in BEIDE kanaallijsten (exec én DRL — beschermd-padvoorstel).
- (c) Daarna mag de positieve zin in de WF-102-COUNT-passage
  (voorstel): de kwantorvouw telt over een meerstapsbereik, vlak, met
  deze golden als getuige.
- (d) Alle veertien gates groen.

May touch: nieuwe goldenmap onder `corpus/golden/`,
`scripts/check-drools.sh` uitsluitend de kanaallijsten (voorstel),
`spec/IR-SPEC.md` (voorstel), kaartverplaatsing. NB: serieel met
DrlTranspiler/check-drools-kaarten (de kanaallijsten bewegen mee).

## Result (2026-09-22)

- (a) GEHAALD, handgeschreven. `corpus/golden/jutterswet-ketenkwantor`:
  TEN_MINSTE 4 (`vondstrijk`) en TEN_HOOGSTE 3 (`vondstarm`) over de
  tweestaps `bronPad` bemanning/gevoerde boot → berging/geborgen vondst,
  drie casussen. Case 1 is de scheidende cel: V2 bereikbaar via B1 en B2,
  vlak 3, voorkomens 4 — de voorkomens-telling draait beide verdicten om.
  Case 2 haalt de drempel precies (vlak 4, voorkomens 5); case 3 laat de
  toets op één element falen (vlak 3, voorkomens 4). Handberekening in de
  NOTE, vóór de eerste run. De tweede regel kwam er na gate 7: met alleen
  `vondstrijk` werden case 1 en 3 (enkel een afwezig kenmerk) door een
  niets-doende uitvoerder gehaald; met de complementregel pint elke casus
  de telling van beide kanten en faalt ze onder `--noop`. De `berging`
  is veel-op-veel, anders kan een vondst niet gedeeld zijn (ref weigerde
  de eerste, één-op-veel versie met WF-013 to-one).
  Producentroute GEMETEN: bestaat niet. De zin via `make -C
  frontends/regelspraak ir` geeft WF-096 "setlezing buiten een
  vouwbron", voor "van alle gevoerde boten" en "van de gevoerde boten",
  voor beide kwantoren: `RuleChecker.kwantorvouw` wijst een `bereikPad`
  af. Geen `bron.rgs`; de IR is de frontend-emissie van de éénstapszin
  (subject Boot, `bronRol`) met subject Jutter en `bronPad`.
- (b) GEHAALD. ref en clingo spelen alle drie de casussen (gate 5/6: 587
  casussen). Drools, beide takken gemeten: `EXCLUDE loader: form not
  carried by the v0 Drools backend: Kwantorvouw.bronPad` — benoemde
  weigering, geen crash, geen fout antwoord. Achter die weigering ligt de
  veel-op-veel `berging` (de bootberging-weigering), dus alleen de loader
  optillen laat hem nog niet spelen. Route: de golden in `EXPECTED_CHANNEL`
  én `DRL_CHANNEL` (klasse B, loader Refusals 35 → 36; de totaaltelling
  in het lijstcommentaar zie de fixronde hieronder).
  Beschermd-padvoorstel. PINDELTA: nul — `EXPECTED_PLAYED` blijft 179,
  `EXPECTED_DRL_PLAYED` blijft 156.
- (c) GEHAALD, voorstel-zone. `spec/IR-SPEC.md`: de WF-102-COUNT-bullet
  krijgt na "a claim about it would need its own witness" de positieve
  zin (de kwantorvouw telt over een meerstapsbereik, vlak, met deze
  golden als getuige, gespeeld door gates 5 en 6); de WF-108-registerrij
  krijgt de golden in de goldenkolom — het is de enige golden die (c)'s
  `sourcePath` draagt.
- (d) GEHAALD. Alle veertien gates groen, elk los gedraaid: 1 PASS; 2 273
  bestanden; 3 273 schoon / 431 negatief; 4 628 casusbestanden; 5 ref
  775 voorbeelden, 587 casussen; 6 clingo 479 checks, 587 casussen; 7 551
  weerleggen, 36 overlevers; 8 41 rood; 9 201 checks, 12 goldens; 10 374
  tests, 70 rgs-goldens; 11 PASS (29 toka-casussen); 12 played 179, DRL
  156, kanaalvloer 53 / 76; 13 PASS, 0 overlevers; 14 130 checks.

Voorgestelde registerbijwerking (hoofdsessie): `spec/CONSTRUCTS.md` rij 40
(kwantorvouw) — bij de getuigen: "`jutterswet-ketenkwantor`: de tellende
kwantor over een tweestaps `bronPad` telt vlak (gedeeld element één keer),
ref ≡ clingo; beide Drools-takken weigeren `Kwantorvouw.bronPad` benoemd".
Buiten deze kaart: `RUNTIME-SEMANTICS.md` ch. 3.9a / de uitvoeringstabelrij
van de kwantorvouw noemt nog geen meerstapsgetuige (beschermd pad, niet
aangeraakt).

### Fixronde (lensronde 2026-09-22)

- B1 (bereik niet gepind): `case4-bereik-en-tweede-jutter` toegevoegd,
  handberekend in de NOTE vóór de run. J1 houdt de keten van case 1
  (vlak 3), J2 voert B3 met V2 (gedeeld met J1), V4, V5, V6 (vlak 4), en
  V8 (9,00) wordt door geen boot geborgen. J1 afwezig/aanwezig, J2
  aanwezig/afwezig in één wereld, dus elke telling die niet van het
  subject afhangt faalt; de populatietelling (7) en de lekkende telling
  (J1: 6) draaien J1's verdicten om. Omkeerproef: elk van de vier
  kenmerkverdicten omgedraaid geeft MISMATCH in ref én clingo.
- B2 (commentaartelling): `DRL_CHANNEL` telt 76 regels (A 25, B 36, C 12,
  D 1, E 2); de klasseredenen (ch. 7, loader Refusal, MARKER — de
  havenrooster-MARKER staat in A) dragen er 73. Het commentaar zegt nu
  "seventy-three of the seventy-six". Voorgeschiedenis: op main stond al
  "seventy-one of the seventy-four" bij 75 regels en 72 klasseredenen, dus
  één te laag; mijn eerste versie schoof die fout door. Niet aangeraakt en
  hier gemeld: het A-kopje zegt "24" bij 25 regels (havenrooster staat er
  met een MARKER-reden in).
- A3: de specszin claimt alleen de at-least- en at-most-arm en zegt
  expliciet dat de exactly-arm over een meerstapsbereik geen getuige heeft;
  de WF-108-rij noemt TEN_MINSTE 4 en TEN_HOOGSTE 3, de tweede subject, en
  dat PRECIES niet getuigd is.
- A4: zin en NOTE noemen beide alternatieve lezingen in de termen van de
  COUNT-bullet: de join-tuple-telling (case 1/3/4: 4 tegen 3) en de
  telling van tellingen = tussen-instanties (case 2: 2 tegen 4).
- A5: de reden in beide kanaallijsten is nu dezelfde regel (zoals het
  B-kopje belooft) en noemt de veel-op-veel-grens AFGELEID, niet gemeten,
  met de verwijzing naar bootberging en muntketen.
- Gates na de fix: 2 273 bestanden; 3 273 schoon / 431 negatief; 4 629
  casusbestanden; 5 775 voorbeelden, 588 casussen; 6 479 checks, 588
  casussen; 7 552 weerleggen, 36 overlevers; 13 PASS, 0 overlevers; 12
  played 179, DRL 156, kanaalvloer 53 / 76, JUnit 539. Pindelta nog
  steeds 0/0.
