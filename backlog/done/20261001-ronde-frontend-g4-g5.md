# ronde-frontend-g4-g5: het percentage van een genavigeerde lezing, een vouw met to-one stappen na de verzameling, de opgave-markering op een bedrag, de elementnaam van een vouwbron, een lid zonder lidwoord na een kop, de elementnaam in een kwantormatrix

*Ronde op zes vondsten van cluster G4+G5 in de RegelSpraak-frontend (V3, V2,
K10, V1, V7, en V9 uit de exec-ronde), opdracht van de hoofdsessie van
2026-10-01; gebouwd vanaf uchar `921fcc64` op de tak `ronde/g4-g5-frontend`.
Reproducties op Jutterswet-materiaal in `/tmp/v2/probe/g4-g5-uchar/v3/`,
`v2/`, `k10/`, `v1/` en `v7/` (alleen gelezen); de reproductie van V9 is de
matrixvorm uit de vormruimte van de exec-ronde. Alle zes waren luid (een
ERROR, of een WARNING met een marker); bij het meten van V1 kwam een stil fout
antwoord van dezelfde oorzaak boven, dat met V1 landde. Geen kaart: dit record
legt de ronde vast, zodat de eigenaar er zijn Keuring bij kan zetten.*

## Aanleiding

**V3, het percentage van een genavigeerde lezing.**

    Het havengeld van een Vondst moet berekend worden als (het percentage
    havengeld van het taxatiebedrag van de taxatie van de Vondst) naar beneden
    afgerond op 2 decimalen.

Zonder procentteken leest de grammatica dit als één attribuutlezing; de
resolver herleest haar als de percentagefunctie als het eerste naamdeel een
Percentage-parameter is, maar nam daarbij een grondslag van hooguit twee
naamdelen. Gemeten op `921fcc64`: `ERROR … het lid 'percentage havengeld van
het taxatiebedrag' bestaat niet op 'Taxatie', de vuller van de rol 'taxatie',
en op geen ander objecttype …`. Met haakjes om de grondslag een syntaxfout,
met een variabele uit "Daarbij geldt" de ERROR dat de naam geen rol is, en met
een "%" achter de parameter (ook bij een eigen lid) de ERROR van een lid
waarvan de naam op "%" eindigt.

**V2, een vouw met to-one stappen na de verzameling.**

    Het zwaarste gewicht van een Ploeg moet berekend worden als de maximale
    waarde van het gewicht van de weging van de vondst van alle jutters van
    de Ploeg.

De naam van de lezing loopt over elke "van" heen, dus de vouw las het lid
"gewicht van de weging van de vondst" van Jutter, dat niemand declareert.
Gemeten op `921fcc64`: `WARNING … is geen (meervoud van een) gedeclareerd
attribuut van Jutter — de aggregatie blijft onopgelost (§13.4.17)`, een marker
in de vier uitvoerders; met "die aan de volgende voorwaarde voldoen: hij is
actief" erbij ook de ERROR dat de deelselectie stil zou wegvallen.

**K10, de opgave-markering op een bedrag.** `de waarde Numeriek (getal met 2
decimalen) met eenheid € (mag opgegeven worden)` weigerde de resolver als
familie die nog niet in de drie uitvoerders gemeten was (dialect §7). De
handvorm in € speelde al in ref, clingo en de exectak (gemeten).

**V1, de elementnaam van een vouwbron bindt naar de volgorde.** Met een
feittype `StrandvondstHeeftWeging` (rol `vondst`, vuller het subtype
Strandvondst) vóór `BergingBetreftVondst` (rol `vondst (mv: vondsten)`, vuller
Vondst) las "alle vondsten" het objecttype via de eerste rol in
declaratievolgorde, ook via het afgeleide meervoud "vondst"+"en". "De maximale
waarde van het gewicht van alle vondsten van de berging van de Strandvondst"
weigerde dan (`keten zonder sourcePath in een vouwbron: … komt via haar
stappen uit bij Vondst, maar de geschreven elementnaam is gelezen als
Strandvondst`), en de omgekeerde volgorde speelde. Bij het meten bleek de vouw
van één stap vanaf de Berging ("alle vondsten van de Berging") in die volgorde
stil fout: `elementType` Strandvondst, en ref, clingo en beide Drools-takken
antwoordden 5 waar 9 hoort.

**V7, een lid zonder lidwoord na een kop.**

    Extensie van objecttype de Vondst
        boven de maat kenmerk (bezittelijk);

De lexer slaat elke regelovergang over, en de naam van de kop nam kleine
woorden ook van de volgende regel op. Gemeten op `921fcc64`: `ERROR (line 5):
objecttype 'Vondst boven': het kenmerk 'maat' begint met 'de' op de regel waar
de kop eindigt, direct na 'boven'; een lid begint op een eigen regel — …`, en
zo voor elk lid zonder lidwoord van twee woorden of meer als eerste lid na een
extensiekop of een kop zonder `(mv: …)`.

**V9, de elementnaam in de matrix van een kwantorvouw.**

    De laatklasse van een Jutter moet berekend worden als 1 indien de vinddag
    van ten minste één van de vondsten van de Jutter later is dan de sluitdag
    van de jutter van de Vondst.

Gemeten op `921fcc64`: `ERROR … genavigeerde lezing 'de sluitdag van de jutter
van de Vondst' is niet als navigatie gelegd: …`, terwijl "de sluitdag van zijn
jutter" bindt en dezelfde lezing in een subselectie ook.

## Wat er landde

**V3.** `ExpressieBouwer.bouwAttribuut`: de grondslag is de gewone lezing van
alle naamdelen na de parameter op het onderwerp (de to-one-keten wordt
afgelopen); zonder naamdeel na de parameter het onderwerp als waarde of een
variabele uit "Daarbij geldt"; een "%" achter de parameter hoort niet bij de
naam. Grammatica: `percentageVanHaakjes : bepaaldLidwoord? kaleNaamDeel VAN
LPAREN expressie RPAREN`, het laatste alternatief van `primaireExpressie`, zodat
elke lezing die de grammatica al had (de periode "van (A) tot (B)", de vorm met
procentteken) een dubbelzinnigheid wint; de resolver eist links een
percentage. Het resultaat is overal het bestaande `percentageOf`. Commits
`5a6f001e` (getuige, rood), `fd0df2e3` (fix), golden `0fe75c98`
(`jutterswet-havengeld`), gate-12-voorstel `c97c24d4`.

**V2.** `ExpressieBouwer.stappenNaDeVerzameling` en `RuleChecker.hulpregels`:
bij een waardevouw over één verzameling met to-one stappen erna krijgt het
elementtype (of het type dat de andere rol van de eerste stap vult, als het
elementtype dat is of eronder valt) een gegenereerd lid `<lid> via <rol 1>, …
en <rol n>`, en zet een gegenereerde regel van die naam het uit de keten,
zoals een auteur het nu met de hand schrijft; de vouw loopt over dat lid, een
deelselectie blijft op het element. Een som of telling daarover telt per
element; de vlakke vereniging blijft de lezing van een bereik ("van alle
boten van alle jutters"). Commits `c2ec5e60` (getuige, rood), `f386d308` (fix),
`76d317d0` (het lid op de vuller van de eerste stap, zodat een vouw over een
subtype en een over zijn supertype het lid in elke volgorde delen), golden
`df8520ee` (`jutterswet-zwaarste-weging`), gate-12-voorstel `9b13883e`.

**K10.** `RuleChecker.opgaveDeclaratieFout`: de markering staat ook op een
Numeriek met één eenheid; een samengestelde eenheid (€/jr) blijft geweigerd.
Een berekening of invoer in een andere eenheid van hetzelfde stelsel weigert de
typering al, dus opgave en berekening delen één eenheid. Commits `c2f16bbd`
(getuige, rood; de weigertabel in `OpgaveMarkeringTest` met de rij €/jr in
plaats van €), `c03f0437` (fix), golden `070ec0c3` (`jutterswet-opgave-bedrag`),
gate-12-voorstel `6e872fc9`.

**V1.** `RegelSpraakModel.objecttypeViaRol`: een gedeclareerde naam of een
gedeclareerd meervoud bindt vóór een afgeleid meervoud, en rollen van één naam
met verschillende vullers kiezen geen van beide. `ExpressieBouwer.wandelBereik`:
in een wandeling over twee stappen of meer beslist de landing het elementtype
waar de elementnaam geen objecttype noemt; noemt zij een objecttype buiten de
landing, dan blijft de weigering. De rij in `GenesteVouwbronTest` die de oude
lezing pinde, staat nu op een model waar de weigering geldt. Commits
`fcb5477e` (getuige, rood), `f48ccd0f` (fix).

**V7.** Grammatica: `objectTypeNaam` neemt een woord alleen van de regel waarop
het vorige woord eindigt (semantisch predicaat `opRegelVanVorigWoord` over de
tokenregels); een woord op de regel van de kop blijft bij de naam. Commits
`164c784c` (getuige, rood), `8e1efd73` (fix).

**V9.** `RuleChecker.kwantorvouw` → `elementlezing`: een operand naast de
gekwantificeerde die op het element een lid leest dat het element niet heeft,
wordt opnieuw gebouwd met het element als onderwerp en opnieuw getypeerd; de
draad is die van de bezittelijke vorm. Commits `077c10d4` (getuige, rood),
`126c71cd` (fix).

## Metingen

Gemeten met de eigen bouw van de worktree (frontendklassen, backendjar,
`_build/default` van ref, clingo en de verifier) tegen een kopie van de bouw
van `921fcc64`; elke vorm een eigen Jutterswet-document, ref en clingo als
arbiter.

| vondst | vóór | na |
|---|---|---|
| V3 a, b, d, en het "%" na de parameter, zonder eenheid | `check exit=1` | ref en clingo PASS, beide Drools-takken PLAYED |
| V3 dezelfde vormen met € | `check exit=1` | ref PASS, exectak en DRL-tak PLAYED, clingo weigert luid (eenheid in een product) |
| V3 eigen lid, letterlijk percentage, "P % van (Y)", "12,5 % van (Y)", de periode tussen haakjes | speelt | IR, uitvoer en exitcode gelijk |
| V2 a, b, c | exit 3 of 1, marker in de vier | ref en clingo PASS, exectak PLAYED, DRL-tak PLAYED (c: haar EERSTE-marker) |
| V2 som, som met "of 0", minimum, aantal gevulde waarden, totaal over twee stappen | — | ref, clingo, exectak, DRL-tak PASS/PLAYED |
| K10 de markering op € | `check exit=1` | golden: ref en clingo exact, exectak PLAYED 7 |
| V1 twee stappen, beide meervouden | exit 3, marker in de vier | ref en clingo PASS, beide takken PLAYED |
| V1 één stap vanaf de Berging | stil 5 waar 9 hoort, in de vier | ref en clingo PASS, beide takken PLAYED |
| V7 de vormen e1-f5, -f6, -f8, -f10, o2-f5, -f6, -f8 | exit 1 | exit 0 |
| V9 de matrix met de elementnaam | `check exit=1` | ref en clingo PASS, exectak PLAYED (de far-side join), DRL-tak haar benoemde matrixgrens |

**Getuigen** (JUnit in `frontends/regelspraak`, elk rood vóór de fix):
`PercentageLezingTest` (12 rijen; rood 5 van 10), `StappenNaDeVerzamelingTest`
(10; rood 7 van 9), `OpgaveBedragTest` (3; rood 3) met de weigertabel van
`OpgaveMarkeringTest` (rood 5), `ElementnaamVouwbronTest` (8; rood 4),
`KopRegeleindeTest` (7; rood 4), `KwantormatrixElementnaamTest` (5; rood 3). De
suite na de ronde: 1113 tests, 0 failures.

**Goldens.** `jutterswet-havengeld` (vier cases, PASS (3) elk; beide takken
PLAYED 4), `jutterswet-zwaarste-weging` (twee cases, 6 en 8 verwachtingen;
exectak PLAYED 2; de som per jutter 240 naast de vereniging over de boten 140),
`jutterswet-opgave-bedrag` (zeven cases; exectak PLAYED 7). Elk met
`scenario.rgs`, handgerekend in `NOTE.md`, ref ≡ clingo exact, elke case
weerlegt de niets-doende uitvoerder, de tegencases (elk één verwachting
verschoven) falen in ref, clingo en de exectak, en de IR uit `bron.rgs` alleen
is byte-gelijk aan die naast de cases.

**Inertie.** Alle 196 getrackte RegelSpraak-bronnen van de repo (elke
`bron.rgs` los, elke scenario-golden als testdocument, elke bron van de
pariteitszone) geven vóór en na dezelfde IR, uitvoer, casebestanden en
exitcode, op de drie nieuwe goldens na; de 108 bronnen van de distributie
buiten haar uchar-submodule ook.

**Pins** (`scripts/check-drools.sh`, voorstel): `EXPECTED_PLAYED` 236 → 239
(drie redenregels), `EXPECTED_DRL_PLAYED` 192 → 193 (`jutterswet-havengeld`),
`DRL_CHANNEL` klasse (A) 40 → 41 (`jutterswet-opgave-bedrag`) en klasse (C) 14
→ 15 (`jutterswet-zwaarste-weging`), met de koppen en hertelzinnen.

**Gates.** De zestien in de volgorde van `gates.yml`, `dune build` vooraf,
elk script onder zijn shebang, direct na de grammaticawijziging van V7
(`8e1efd73`) en op de kop van de ronde: zeventien regels `exit 0` en `DONE`
(de letterlijke samenvatting staat in het Result van de ronde).

## Open punten

1. Het product met een percentage ("zijn getaxeerde waarde maal het
   percentage havengeld") blijft de typefout; de vermenigvuldigingstabellen
   van het typeringshoofdstuk hebben geen rij Percentage. Advies: zo laten,
   eventueel de weigerzin de uitweg laten noemen.
2. clingo weigert een percentage van een bedrag met eenheid luid, en een
   groot bedrag zonder eenheid ook ("arithmetic range exceeded"); de golden van
   V3 heeft daarom geen eenheid. Laag `backends/clingo`.
3. V2, normkeuze: een som of telling over to-one stappen na de verzameling
   telt per element (een gedeelde boot telt per jutter). Alternatief: de
   vlakke vereniging ook daar; dat zou de stappen tot een bereik maken en de
   deelselectie naar het eindobject verplaatsen. De geschreven vorm kiest nu
   ("van de boot" per element, "van alle boten" de vereniging).
4. V2: een gegenereerd lid blijft staan als dezelfde vouw daarna om een andere
   reden weigert; geen gevolg voor een uitvoerder (de vouw is dan een marker of
   het document een ERROR).
5. De kaart `frontend-kwantor-over-een-pad`: de kwantor over een pad (haar R1)
   blijft luid open, en de telling over een pad van meer dan één stap blijft
   STIL afgekapt (haar eerste Aanvulling; opnieuw gemeten op de kop van deze
   ronde). Voorstel voor een tweede Aanvulling staat in het Result.
6. V7: `(bijvoeglijk)` zonder "is" neemt de grammatica aan, ruimer dan
   §13.3.2. Niet gewijzigd. Na V7 lezen ook "boven de maat kenmerk
   (bijvoeglijk)" na een extensiekop en na een kop zonder meervoud (vóór
   weigerde de naam die vorm daar, elders las zij al).
7. V1: een naam die zowel een rol noemt die de wandeling bindt als het
   meervoud van een objecttype buiten de landing, las vóór als de rol en
   weigert nu (de weigering die het standaardantwoord houdt).
8. Bijvangst, laag `spec/verifier`: de verifier geeft PASS op een document met
   een `Unresolved` regelwaarde, terwijl ref en clingo luid falen (WF-044);
   gemeten op een vouw over een to-many stap na de verzameling, die na deze
   ronde een marker blijft. De vormen van V1 en V2 lost de ronde op.
9. Bijvangst, laag frontend: twee regels met één naam schrijft de frontend
   zonder melding, de verifier weigert het document (`WF-020 FAIL
   /rules/rules/1: duplicate name …`), ref en clingo spelen het. Ook op
   `921fcc64`. Niet in deze ronde: de namen van uitzonderingsblokken en
   beslistabelrijen delen die ruimte, en een weigering op de koppen alleen zou
   te veel weigeren zonder eigen meting.
10. De distributie draait op haar pin en kent deze ronde niet; de pin moet na
    het landen mee.

## Keuring (hoofdsessie, 2026-10-02)

- Gates: `/tmp/v2/uchar-gates23/summary.txt` op de kop f150b29b: dune build en
  zestien gates "exit 0", `DONE`; ook direct na de grammaticawijziging van V7
  (8e1efd73) groen. Gate 12 played 239 (pins 236 → 239 door de goldens
  `jutterswet-havengeld`, `jutterswet-zwaarste-weging`,
  `jutterswet-opgave-bedrag`), DRL-tekst 193, JUnit 1113. Inertie: 190/190
  oudere bronnen van de repo en 108/108 van de distributie geven dezelfde IR,
  uitvoer en exitcode.
- Gelezen: het Result per vondst (getuigen rood-vóór/groen-na met de
  letterlijke uitkomsten, de metingen in ref, clingo en beide Drools-takken),
  de pins en de registertekst. De bijvangst bij V1 (een vouw van één stap gaf
  door de eerste-treffer-rolbinding 5 waar 9 hoort, stil, in alle vier
  uitvoerders) is met de fix verdwenen en door `ElementnaamVouwbronTest`
  gepind; aanvaard dat er geen golden voor is.
- Twee bestaande tests zijn aangepast omdat zij precies de oude grens
  vastlegden: in `OpgaveMarkeringTest` de €-rij naar €/jr (een samengestelde
  eenheid blijft geweigerd), in `GenesteVouwbronTest` de rij
  `een_keten_die_elders_uitkomt_wordt_benoemd` op een model waar de weigering
  nog geldt. Beide met meting en reden in het Result; aanvaard, ter kennis van
  de eigenaar.
- Registertekst geplaatst (commit "Register: …"): DIVERGENCES, IR-SPEC ch.
  4.3, CONSTRUCTS rij 1 en 8, de tweede Aanvulling van de kaart
  frontend-kwantor-over-een-pad.
- Open punten: 1 (product met een percentage) en 6 ((bijvoeglijk) zonder
  "is") aanvaard zoals geadviseerd; 2 (clingo en een percentage van een bedrag
  met eenheid) en 8 (de verifier geeft PASS op `Unresolved`, WF-044 alleen in
  de uitvoerders) zijn normvragen voor de eigenaar; 3, 4 en 7 aanvaard; 5 (de
  telling over een pad van meer dan één stap blijft stil afgekapt) en 9 (twee
  regels met één naam passeren `check` zonder melding) gaan naar een kleine
  frontend-naronde, samen met de benoemde nuldeler van de exec-ronde; 10
  aanvaard; 11 (de distributiepin) volgt nu.
