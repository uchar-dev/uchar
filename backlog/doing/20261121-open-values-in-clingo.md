# open-values-in-clingo: clingo vertaalt een document dat uit leeg niets afleidt

*Opgesteld door de hoofdsessie op 2026-11-21. De lezing staat in `spec/RUNTIME-SEMANTICS.md` ch. 1.5 (de standen, de
open lezing, het oordeel als paar, de drie uitkomsten van een schrijver, de sluitingen, de twee eisen, de vormen zonder
lezing) en ch. 5.12/5.12a; de referentie-interpreter rekent haar (`backlog/done/20261121-open-values-in-ref.md`). clingo
weigert zo'n document nu met CLINGO-049.*

## Aanleiding

Een regelset kan kiezen dat uit een lege waarde niets volgt (`inferFromEmpty: false`). Een lid is dan gevuld,
vaststaand leeg of open; wat een open waarde leest is open; een voorwaarde die een open waarde leest is onbepaald,
tenzij een ander deel haar beslist. Het differentieel van gate 6 (clingo tegen de absolute verwachtingen die gate 5
vastlegt) bestaat voor zulke documenten nog niet: de goldens met de eigenschap staan in het weigerkanaal.

Een voorbeeld uit de Jutterswet: "Een Vondst is kostbaar indien zijn taxatie groter is dan 1000", "Het vindloon van
een Vondst is 10 indien hij kostbaar is", "De bewakingstoeslag van een Vondst is 5 indien hij niet kostbaar is". Taxatie
50: kostbaar staat vast als niet, het vindloon is vaststaand leeg, de bewakingstoeslag is 5. Zonder taxatie: kostbaar is
open, vindloon en bewakingstoeslag zijn open, en geen van beide regels vuurt.

## Goal

`backends/clingo` vertaalt een document met `inferFromEmpty: false` volgens ch. 1.5, de goldens met de eigenschap
spelen in gate 6 met de verwachtingen die gate 5 vastlegt (`attributeOpen` en `characteristicOpen` inbegrepen), en wat
clingo van zo'n document niet draagt, weigert hij bij naam met de slug van de vorm.

## De architectuur (bindend)

**clingo bouwt de semantiek van ch. 1.5 in de vorm van ASP, niet het mechanisme van de referentie-interpreter.** ref
houdt "open" bij als merkteken en leest vaststaand leeg als de afwezigheid daarvan, met een exceptie die een evaluatie
afbreekt. In ASP is dat de verkeerde kant: een lezer moet op vaststaand leeg kunnen vuren (een plus met 0), en
negatie-als-falen over een predicaat `open` dat zelf van de voorwaarden van de schrijvers afhangt, is op predicaatniveau
een cyclus door negatie. Dus:

1. **Vaststaand leeg wordt positief afgeleid, open is afwezigheid.** Een atoom zegt dat een attribuut van een object
   vaststaand leeg is, een ander dat van een kenmerk vaststaat dat het niet geldt; open is: geen waarde en geen van die
   atomen. Voor een invoerlid komt het atoom uit de case (een lid dat als volledig geldt en geen waarde heeft; een
   kenmerk in `characteristicsAbsent`); voor een afgeleid lid uit zijn schrijvers, per concreet type van het object:
   van elke schrijver die daar schrijft staat vast dat hij niets vult. Een eigen predicaat, geen sterke ontkenning van
   clingo (`-has`): een invoerkenmerk naast een regel die het tegendeel afleidt mag het programma niet zonder
   antwoordverzameling laten.
2. **Een voorwaarde heeft een positieve en een negatieve kant**, "staat vast als waar" en "staat vast als onwaar", elk
   positief afgeleid. De samengestelde voorwaarde telt beide kanten (de tabel van ch. 1.5). Het paar van ch. 1.5 (het
   oordeel voor de vorm zelf en voor een ander) vraagt een tweede wandeling alleen onder een voorwaarde of waarde
   waarin een sluiting staat (de toets op leeg en gevuld, de gevuld-telling, een filter op gevuld, een `IfThenElse` die
   een sluiting besliste); overal elders delen de twee dezelfde hulppredicaten.
3. **"De evaluatie breekt af" is een conditie op de body.** Een waarde, een oordeel en een deponering in het
   §9.5-kanaal ontstaan alleen waar elke *bereikte* lezing binnen de grens vaststaat; "bereikt" is het bereik van ch.
   5.11 zoals ch. 1.5 het onder "An open read" zegt (een leeg deeltal leest de deler niet, een binder wordt gelezen waar
   de body hem leest, de samengestelde voorwaarde leest tot haar oordeel voor een ander vaststaat). Een deponering of
   fout van een lid dat voorbij een onbepaald lid gelezen wordt, volgt de regel van ch. 1.5 onder "The judgement of a
   condition".
4. **Eén vertaling per vorm.** Geen kopie van de vertaling van een vorm voor documenten met de eigenschap. De
   eigenschap wordt gelezen in één klein blok: hoe een lege lezing vaststaat, hoe een voorwaarde als onwaar vaststaat,
   en wat er van een schrijver vaststaat. In een document zonder de eigenschap is niets open; daar is
   negatie-als-falen over de binding dezelfde uitspraak als het positieve atoom, en blijft de vertaling wat zij is.
5. **Een document zonder de eigenschap geeft byte voor byte hetzelfde ASP-programma als nu**, gemeten over elk
   golden- en rood document met elk van zijn cases, vóór en na. Gate 6, 8, 13 en de suites van de OCaml-frontends
   blijven groen zonder dat een verwachting, pin of lijst verandert buiten de goldens met de eigenschap.
6. **Eerst de twee beschreven onjuiste hoeken van het bereik**, [L3] en [S1] in `backends/clingo/README.md`: in de
   lezing van ch. 1.5 geven zij een andere *waarde* (open tegen gevuld), niet alleen een andere fout. Herstel ze, of
   weiger de vormen waar ze spelen in een document met de eigenschap bij naam (CLINGO-049 met een eigen slug), met een
   frame in de suite dat de weigering houdt.

Afwijken van 1–4 mag met de reden en de meting in het Result, zolang 5 en 6 blijven staan en de vertaling één lezing
houdt.

## Bouwdelen

Elk deel een verse bouwer; elk deel eindigt groen, met een Result-sectie en een overdracht.

1. **De kern.** De hoeken [L3] en [S1]; de standen van een invoerlid en een afgeleid lid; de case (open, volledig,
   `characteristicsAbsent`) en de modelcontrole (`attributeOpen`, `characteristicOpen`, en `attributeEmpty` en
   `characteristicAbsent` als "staat vast"); de atomaire voorwaarden met hun twee kanten; de toets op leeg en gevuld
   met haar sluiting; de samengestelde voorwaarde met de tabel; de regel (gelijkstelling, berekening,
   kenmerktoekenning, initialisatieregel) met haar drie uitkomsten; de consistentieregel; de lezing over een link;
   `Let`; de deponeringen onder "elke bereikte lezing staat"; de weigering bij naam van elke andere vorm.
2. **De stapel, de vouwen, de creaties.** De defaults-groep met drie uitkomsten per default en het positieve "geeft
   voorrang", de initialisatie als sluiting, de opgave met het veld `complete` van de declaratie, de kenmerkstapel, de
   uitzonderingsgroepen; de kwantorvouw; de vouwen over een verzameling en de sluiting binnen een waarde;
   `IfThenElse`; object- en feitcreatie met een open relatie. De goldens met de eigenschap uit het weigerkanaal van
   gate 6; de rode case met het kenmerk in beide lijsten (EF-047) in het clingo-been van gate 8.
3. **Het brede differentieel en de staart.** Behoud bij volledige kennis door clingo (elke golden die clingo speelt,
   met de eigenschap erbij en elk invoerlid volledig, speelt haar eigen cases) en het differentieel met open leden
   tegen ref (de runs van de poot voor monotonie van gate 5), als poot van gate 6 binnen een tijdsbudget dat de
   hoofdsessie na deel 2 vaststelt; de vijf open punten van `backlog/done/20261121-clingo-filled-test.md`; de sectie in
   `spec/ASP-TRANSLATION.md`; de README; het Result van de kaart.

## Acceptance (deel 1; de volgende delen krijgen de hunne in hun opdracht)

Rood eerst per frame.

- **De getuigen van ref spelen in clingo.** Elke getuige onder `ref/test/fixtures/strict-empty/` waarvan clingo de
  vormen in dit deel draagt, speelt in de suite van clingo met dezelfde casebestanden en dezelfde verwachtingen (de
  suite leest de fixtures op hun plaats; geen kopie). Elke andere getuige weigert met CLINGO-049 en de slug van haar
  vorm, en de suite noemt per getuige welke van de twee. Twee getuigen (`closed-input`, `input-above-computation`)
  wachten op het veld `complete` van de kaart `complete-member` en staan tot dan in de lijst van weigeringen met die
  reden.
- **Elk principe van ch. 1.5 binnen het bereik van dit deel heeft een frame in de suite** met de naam van het
  voorbeeld in de spec (`WV-1.5-…`), op het kleinste document uit de Jutterswet, met de uitkomst van clingo naast die
  van ref: de drie standen van een invoerlid; een afgeleid lid dat zijn schrijvers volgt (twee schrijvers; een
  schrijver op een subtype); wat vaststaat rekent als ch. 1.2–1.4; een open lezing geeft geen waarde en geen
  deponering; de drie voorbeelden van het bereik (deeltal, binder, grens) of hun weigering; de volle tabellen van
  `all`, `atLeast 1` en `none` over twee leden en de rijen van `atLeast 2`, `atMost 1`, `exactly 1` van de spec; de
  toets op leeg en gevuld met haar tabel, ook over een berekende waarde; de eigen ontkenning van een gevuld-toets; de
  ontkende kenmerkcheck; de regel met haar drie uitkomsten; de initialisatieregel als sluiting; de consistentieregel;
  de lezing over een afwezige en over een aanwezige link; invoer naast een schrijver die niet beslist.
- **De weigering**: één functie somt op wat clingo in een document met de eigenschap niet draagt; de slugs zijn die
  van de tabel "Forms without the reading" van ch. 1.5 waar het om dezelfde vorm gaat, in die volgorde, en een eigen
  slug voor wat ref wel draagt en clingo nog niet. `backends/clingo/codes.tsv` en gate 8's `check-exec-codes` houden
  de code aan de bron.
- **Een document zonder de eigenschap verandert niet**: het ASP-programma van elke golden- en rode case byte-gelijk
  vóór en na (een script in de werkmap; rapporteer het aantal programma's en het aantal verschillen, dat 0 is); gate
  6, 8 en 13 groen.
- **De eigenschap op één plaats**: het Result noemt elke plaats in `backends/clingo/lib` die `infer_from_empty` leest,
  met haar reden, en de omvang van de diff per bestand.
- Los groen: gate 1, 3, 5, 6, 7, 8, 9 of 14 waar de bibliotheek hen raakt, 13. `make gates` draait de hoofdsessie.

## May touch

- `backends/clingo/` (bibliotheek, runner, suite, `codes.tsv`, README);
- `spec/ASP-TRANSLATION.md` (deel 3; in deel 1 en 2 een voorstel in het Result);
- `scripts/check-clingo.sh` (de kanaallijst, een floor) en `scripts/check-red.sh` (de divergentielijst) waar een golden
  of rode case van kanaal wisselt, elk in een eigen commit;
- deze kaart.

Niet: `ref/`, `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, de verifier, de frontends, `backends/drools/`, het
corpus. Vind je in ch. 1.5 een zin die clingo niet kan volgen of die twee lezingen toelaat, of een uitkomst van ref die
ch. 1.5 tegenspreekt, dan is dat een genummerd open punt in het Result met het kleinste document en beide uitkomsten;
je kiest voor de bouw de lezing van ref en gaat door.

Mandaat voor de beschermde paden: de opdracht van de eigenaar van 2026-11-21 voor dit programma. De hoofdsessie leest
na en landt.

## Out of scope

Tijdlijnen en elke andere vorm van de tabel "Forms without the reading"; de lezingen en de eindstand van ch. 5.12 en
5.12a (gate 17 vergelijkt ref met Drools; clingo heeft daar geen been) buiten wat deel 3 voor het differentieel met
open leden nodig heeft; `spec/CONSTRUCTS.md` en `spec/PLAN.md` (stel de rij voor); `CLAUDE.md` (stel de tekst voor); de
toolchain.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): de lezing met drie standen; de sterke tabel van Kleene, ook na een onbepaald lid; een
initialisatie is een sluiting; de wire draagt het document van de auteur en elke executor draagt de lezing zelf; de
juistheid rust op eigen tests per principe en op het corpus; code blijft schoon en to the point: één lezing per
executor.

Van de hoofdsessie (voorlopig): clingo krijgt een eigen vertaling en geen omzetting van het document bij het laden; de
architectuur hierboven, naar het onafhankelijke oordeel over de opzet in ref (voorwaarde 5: de semantiek in de vorm van
ASP); een document zonder de eigenschap houdt zijn programma byte voor byte.

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon). In niets dat in de repository komt
staat een naam, regel of casus van een regelset buiten deze repository.
