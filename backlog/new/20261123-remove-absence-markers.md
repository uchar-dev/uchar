# remove-absence-markers: de markers `onbekend` en `niet van toepassing` vervallen

*Besluit van de eigenaar, 2026-11-23: ontwerp B van het voorstel van 2026-11-22, dat een tweede lezing (extern model, hoogste
stand) ook adviseerde. De eigenaar gaf mandaat voor de beschermde paden die deze kaart raakt. Nog niet begonnen: de eigenaar
geeft het sein.*

## Besluit

- Beide markers vervallen. Er is één leeg: in een gewoon document leeg (RUNTIME-SEMANTICS ch. 1.2), in een document met
  `inferFromEmpty: false` open en vaststaand leeg (ch. 1.5).
- Een regel of een opgave beslist niet dat er geen waarde is. Een status ("komt er niet") schrijft de auteur als kenmerk:
  "een Vondst is niet afhaalbaar indien hij drijfhout is", "de afhaaldatum van een Vondst is zijn claimdatum plus 7 dagen
  indien hij afhaalbaar is".
- Een gewoon document onderscheidt "nog niet" en "nooit" niet; dat doet een strikt document (open, vaststaand leeg) of een
  kenmerk.
- Geen eigen toets op vaststaand leeg ("staat vast dat X leeg is") in deze kaart.
- Sum types zonder markers (ch. 9.1–9.4), `Construct` en `Match` blijven.

## Waarom

- De markers zijn gevulde waarden in een gesloten wereld (ch. 9.5: "A marker is a filled value"); ze veranderen niets aan
  hoe een ontbrekend gegeven gelezen wordt. Naast ch. 1.5 beantwoorden twee lagen dezelfde vraag: open naast `onbekend`,
  vaststaand leeg naast `niet van toepassing`. Een attribuut in een strikt document kan dan vijf dingen zijn, en "is er een
  waarde?" heeft twee toetsen (`is gevuld`, waar op een marker, en "heeft een waarde").
- WF-127 dwingt in een som een filter "die een waarde hebben" af, en in een strikt document is dat filter een sluiting:
  gemeten met `ref` geeft "de som van de taxatie van alle vondsten die een waarde hebben" 300 zonder de taxatie van de tweede
  vondst en 400 met haar. De marker breekt zo de monotonie van ch. 1.5.
- Bekende logica: dit zijn Codd's A- en I-marks (RM/V2, 1990); SQL nam ze niet over en houdt één NULL.
- Gebruik, gemeten op 2026-11-22: één regelset buiten het repository gebruikt `niet van toepassing`, op één attribuut, met
  twee schrijvende regels, dertien lezende toetsen en 101 regels in toetsgevallen; daar diende zij om "nog niet" en "nooit"
  te scheiden, wat een strikt document zelf doet. `onbekend`: nul keer.

## Wat vervalt of verandert

1. **Spec.** RUNTIME-SEMANTICS ch. 9.5 vervalt, met de verwijzingen ernaar elders (onder meer ch. 1.2, 1.5, 3.11, 5.6, 5.7b,
   7.x). IR-SPEC: WF-127 vervalt; `PayloadAccess` vervalt (gemeten: alleen vier goldens met markers gebruiken hem), en de zin
   in ch. 4.7 dat `Construct` en `PayloadAccess` een attribuut met markers schrijven en lezen. Hoe een vervallen WF-nummer
   blijft staan, volgt de bestaande praktijk. `spec/proposals/FLAVOURS-OF-EMPTY.md` krijgt bovenaan het besluit van
   2026-11-23 (N1 tot N5 herzien). Het schema alleen waar het een vorm draagt die alleen de markers gebruiken.
2. **Verifier.** De controle van WF-127 en haar elf negatieve cases (`corpus/negative/wf127-*`). De vloeren van gate 3 dalen
   in dezelfde commit, met naam.
3. **Frontend.** De tokens `OF_ONBEKEND`, `OF_NIET_VAN_TOEPASSING` en `OF_ONBEKEND_OF_NIET_VAN_TOEPASSING`; de schrijfvorm
   "moet gesteld worden op onbekend", de toetsen "is onbekend", "is niet van toepassing" en "heeft een waarde"; de marker in
   de scenariolaag; de marker naast de opgave; de RGS-codes van de markers (onder meer RGS-376, RGS-377 en RGS-461). `DIVERGENCES.md`: de sectie
   "Afwezigheidsmarkeringen" vervalt, de opgave-sectie verliest haar markerparagrafen. Een document dat nog een markering
   schrijft, krijgt de gewone syntaxfout.
4. **Executors.** De markers in `ref`, clingo en Drools (in Drools de codes met de soort `RETIRED`, zoals het register dat
   doet).
5. **Corpus.** De goldens `jutterswet-afwezigheidsmarkers`, `-cel`, `-creatie`, `-initialisatie`, `-kopie`,
   `jutterswet-opgave-afwezigheidsmarkers` en de markerdelen van `jutterswet-gevuldtoets-soorten`; de rode case
   `jutterswet-afwezigheidsmarkers-botsing`; `ref/test/fixtures/absence-markers`; de voorbeelden `WV-9.5-*`. Elk vervalt of
   wordt met een kenmerk herschreven waar het iets anders vastpint dat moet blijven (de opgave, de kopie, de initialisatie,
   de gevuld-toetsen). De vloeren die daardoor dalen, dalen in dezelfde commit, met naam.
6. **Bookkeeping** (hoofdsessie, bij de landing): de rij van de markers in `spec/CONSTRUCTS.md`.

## Acceptatie

- Geen marker, geen `PayloadAccess` en geen WF-127 meer in `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema`,
  `spec/verifier`, `ref`, `backends`, `frontends` en `corpus`, behalve waar een tekst het besluit of de geschiedenis noemt.
- Eén Jutterswet-golden toont het kenmerkpatroon in een gewoon en in een strikt document: drijfhout genoemd geeft een lege
  (strikt: vaststaand lege) afhaaldatum; in het strikte document geeft drijfhout open een open afhaaldatum, en drijfhout in
  `characteristicsAbsent` de berekende datum.
- `make gates` groen; elke gedaalde vloer staat met haar oude en nieuwe waarde in de commitboodschap.
- Geen `Co-Authored-By` of andere vermelding van een assistent in commits.

## Volgorde

1. Deze kaart in uchar.
2. De toolchain haalt de markers uit verzoek, antwoord, uitleg, playground en `openapi` (kaart
   `20261123-absence-markers-out-of-service` in de toolchain) en bumpt de pin.
3. De regelset die `niet van toepassing` gebruikt, moet zonder markers zijn herschreven vóór de toolchain die pin krijgt;
   anders breekt zij. Dat doet de sessie van die regelset, op een tekst van de hoofdsessie.
4. De website (`talen.html`, "Onbekend is niet open: vier manieren …", en de lijst van dialectvormen) na de landing, op het
   sein van de eigenaar.
