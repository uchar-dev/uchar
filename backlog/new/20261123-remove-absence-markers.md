# remove-absence-markers: de markers `onbekend` en `niet van toepassing` vervallen

*Besluit van de eigenaar, 2026-11-23: ontwerp B van het voorstel van 2026-11-22, dat een tweede lezing (extern model, hoogste
stand) ook adviseerde. De eigenaar gaf mandaat voor de beschermde paden die deze kaart raakt. Nog niet begonnen: de eigenaar
geeft het sein. Bijgewerkt na een tweede review van de kaarten (extern model, 2026-11-23).*

## Besluit

- Beide markers vervallen. Er is één leeg: in een gewoon document leeg (RUNTIME-SEMANTICS ch. 1.2), in een document met
  `inferFromEmpty: false` open en vaststaand leeg (ch. 1.5).
- Een regel of een opgave beslist niet dat er geen waarde is. Een status ("komt er niet") schrijft de auteur als kenmerk:
  "een Vondst is afhaalbaar indien hij niet drijfhout is" en "de afhaaldatum van een Vondst is zijn claimdatum plus 7
  dagen indien hij afhaalbaar is". Bij meer uitsluitingen een kenmerkstapel: basis "een Vondst is afhaalbaar", uitzondering
  "niet afhaalbaar indien hij drijfhout is".
- "Nog niet" tegenover "nooit" op één attribuut maakt alleen een strikt document (open, vaststaand leeg). In een gewoon
  document is een kenmerk tweewaardig; wie het daar nodig heeft, schrijft een enumeratie van de stand naast de waarde
  (`FLAVOURS-OF-EMPTY.md` §6.1). Een strikt document weigert nog tijdlijnen en dimensiecellen (ch. 1.5).
- Geen eigen toets op vaststaand leeg ("staat vast dat X leeg is") in deze kaart.
- Sum types zonder markers (ch. 9.1–9.4), `Construct` en `Match` blijven.

## Waarom

- De markers zijn gevulde waarden in een gesloten wereld (ch. 9.5: "A marker is a filled value"); ze veranderen niets aan
  hoe een ontbrekend gegeven gelezen wordt. Naast ch. 1.5 beantwoorden twee lagen dezelfde vraag: open naast `onbekend`,
  vaststaand leeg naast `niet van toepassing`. Een attribuut in een strikt document kan dan vijf dingen zijn, en "is er een
  waarde?" heeft twee toetsen (`is gevuld`, waar op een marker, en "heeft een waarde").
- WF-127 dwingt op elke som over een attribuut met markers het filter "die een waarde hebben" af, en in een strikt document
  is dat filter een sluiting. De auteur kan zo'n som dus niet monotoon schrijven: gemeten met `ref` geeft "de som van de
  taxatie van alle vondsten die een waarde hebben" 300 zonder de taxatie van de tweede vondst en 400 met haar, waar de som
  zonder marker open blijft.
- Bekende logica: dit zijn Codd's A- en I-marks (RM/V2, 1990); SQL nam ze niet over en houdt één NULL.
- Gebruik, gemeten op 2026-11-22: één regelset buiten het repository gebruikt `niet van toepassing`, op één attribuut, met
  twee schrijvende regels, dertien lezende toetsen en 101 regels in toetsgevallen; daar diende zij om "nog niet" en "nooit"
  te scheiden, wat een strikt document zelf doet. `onbekend`: nul keer.

## Wat de auteur verliest (aanvaard)

- Eén uitzondering bovenaan een stapel die met "niet van toepassing" de hele stapel stopt: de kenmerkvoorwaarde komt in
  elke laag, of de stapel krijgt een kenmerk dat ervoor beslist.
- Het verdringen van een invoerwaarde en de botsing EF-016 tussen "geen waarde" en een waarde: wie dat wil, schrijft een
  consistentieregel ("de afhaaldatum moet leeg zijn indien hij drijfhout is").
- Een opgave "er is er geen" met haar afwijkingsmelding: wie dat nodig heeft, geeft een kenmerk als invoer.
- De verplichte toets van WF-127 vóór een lezing: zonder haar rekent een vaststaand lege waarde door zoals in ch. 1.2
  (telt als 0, `≠` is waar).

## Wat vervalt of verandert

1. **Spec.**
   - RUNTIME-SEMANTICS ch. 9.5 vervalt, met de drie verwijzingen ernaar elders in ch. 9. De verwijzingen naar het §9.5-kanaal
     van RegelSpraak (inconsistenties, in ch. 1.2, 1.5, 5.6, 5.7b en verder) blijven.
   - IR-SPEC: WF-127 vervalt, zoals WF-078 als vervallen regel blijft staan; `PayloadAccess` vervalt (alleen de vier goldens
     met markers gebruiken hem), met zijn rij in ch. 4.7 en de verwijzing in WF-037. Ch. 4.7: de RegelSpraak-emitter maakt
     dan ook geen `Construct` meer. WF-062 heeft `jutterswet-afwezigheidsmarkers-creatie` als getuige en krijgt een andere.
   - `spec/ASP-TRANSLATION.md` en `spec/proposals/STRICT-EMPTY.md` verliezen hun verwijzingen naar markers en
     `PayloadAccess`. `spec/proposals/FLAVOURS-OF-EMPTY.md` krijgt bovenaan het besluit van 2026-11-23: N1 tot N5 herzien,
     en bij N6 dat een kenmerk in een strikt document drie standen heeft.
   - Schema: `PayloadAccess` uit `uchar-ir-v1.schema.json`; de markerwaarden uit `uchar-case-v0.schema.json`, met behoud van
     de waarden van een sum type.
2. **Verifier.** De controle van WF-127 en `PayloadAccess` in `spec/verifier` (met de README), en de elf negatieve cases
   `corpus/negative/wf127-*`. De negatieve cases `wf025-allocation-sum-type-amount` en `wf088-sum-payload-drift` gebruiken
   een sum type met de constructor `onbekend` en worden herschreven met een sum type zonder marker. De vloeren van gate 3
   dalen in dezelfde commit, met naam.
3. **Frontend.** De tokens `OF_ONBEKEND`, `OF_NIET_VAN_TOEPASSING` en `OF_ONBEKEND_OF_NIET_VAN_TOEPASSING` met hun
   grammaticaregels; de schrijfvormen van een marker (gelijkstelling, creatie, initialisatie, cel, tijdlijn); de toetsen "is
   onbekend", "is niet van toepassing", "heeft een waarde" en de subselectie "die een waarde hebben"; de resolver-pass van
   de markers en hun plaats in de verlaging van de opgave; de marker in de scenariolaag en in de semantic tokens; de
   RGS-codes van de markers (onder meer RGS-376, RGS-377 en RGS-461; het register kent geen vervallen soort, dus ze
   verdwijnen, en hun nummers worden niet hergebruikt). `DIVERGENCES.md`: de sectie "Afwezigheidsmarkeringen" vervalt; §7
   (de opgave), "Volledig gegeven" en "Scenariotoetsen" verliezen hun markerverwijzingen. Een document met een
   declaratiemarkering of een marker in een scenario krijgt een syntaxfout; een regelzin als "zijn taxatie onbekend is"
   leest weer als de kenmerktoets die hij zonder markers was, met de melding van de resolver.
4. **Executors.** `PayloadAccess` en de markerpaden in `ref` (ook `ref/lib/reading.ml`), clingo en Drools (codes met de soort
   `RETIRED`), met hun README's. `scripts/check-drools.sh`: `EXPECTED_PLAYED` is een gelijkheid en daalt met de geschrapte
   goldens; redenen in `EXPECTED_CHANNEL` die de kaart `absence-markers` noemen, worden bijgewerkt.
5. **Corpus.** De goldens `jutterswet-afwezigheidsmarkers`, `-cel`, `-creatie`, `-initialisatie`, `-kopie`,
   `jutterswet-opgave-afwezigheidsmarkers` en de markerdelen van `jutterswet-gevuldtoets-soorten`; de rode case
   `jutterswet-afwezigheidsmarkers-botsing`; `ref/test/fixtures/absence-markers`; de voorbeelden `WV-9.5-*`. Elk vervalt of
   wordt zonder marker herschreven waar het iets anders vastpint dat moet blijven (de opgave met initialisatie en per maand,
   de kopie, de initialisatie, de gevuld-toetsen, de cel, de creatie). De vloeren die daardoor dalen, in welke gate ook, dalen
   in dezelfde commit, met naam.
6. **Bookkeeping** (hoofdsessie, bij de landing): de rij van de markers in `spec/CONSTRUCTS.md`.

## Besluit voor de eigenaar, vóór de bouw

**Wat met sum types gebeurt dat alleen de markers gebruikten.** Na het schrappen maakt geen frontend meer een sum type op
een tijdlijn, in een cel, in een creatie of een kopie, en pint geen golden meer de eenheidsconversie in een `Construct`,
de precisiepoort van WF-062 op een `Construct` of EF-016 tussen twee constructorwaarden. De Catala-frontend en de
wire-first goldens gebruiken sum types alleen als scalaire waarde met `Match`. Voorstel, volgens "geen ongebruikte code":
de bouwer meet eerst welke paden na het schrappen geen getuige meer hebben; wat alleen de markers gebruikten, gaat uit de
executors en wordt bij naam geweigerd, en ch. 9 zegt dat. Het alternatief is die paden houden met nieuwe wire-first
getuigen.

## Acceptatie

- Geen marker, geen `PayloadAccess` en geen WF-127 meer in `spec/IR-SPEC.md` (behalve de vervallen regel),
  `spec/RUNTIME-SEMANTICS.md`, `spec/schema`, `spec/verifier`, `ref`, `backends`, `frontends`, `scripts` en `corpus`, behalve
  waar een tekst het besluit of de geschiedenis noemt.
- Eén Jutterswet-golden toont het kenmerkpatroon in een gewoon en in een strikt document: drijfhout genoemd geeft een lege
  (strikt: vaststaand lege) afhaaldatum; in het strikte document geeft drijfhout open een open afhaaldatum, en drijfhout in
  `characteristicsAbsent` de berekende datum. Het strikte deel heeft een `scenario.rgs` (kaart `strict-scenarios`).
- `make gates` groen; elke gedaalde vloer staat met haar oude en nieuwe waarde in de commitboodschap.
- Geen `Co-Authored-By` of andere vermelding van een assistent in commits.

## Volgorde

1. Kaart `strict-scenarios` in uchar, met de pin in de toolchain, zodat de regelset die nu `niet van toepassing` gebruikt
   haar overstap kan toetsen.
2. Die regelset wordt een strikt document zonder markers. Dat doet haar eigen sessie, op een tekst van de hoofdsessie.
3. Deze kaart in uchar.
4. De toolchain haalt de markers uit verzoek, antwoord, uitleg, playground, scripts en `openapi` (kaart
   `20261123-absence-markers-out-of-service`) en bumpt de pin.
5. De website (`talen.html`, "Onbekend is niet open: vier manieren …", en de lijst van dialectvormen), op het sein van de
   eigenaar.
