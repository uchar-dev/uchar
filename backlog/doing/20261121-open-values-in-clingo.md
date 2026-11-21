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

## Result (deel 1)

*Bouwer deel 1, 2026-11-22. Branch `card/open-values-in-clingo`, op `bcabd51eb`.*

### Wat er is

| commit | wat |
|---|---|
| `cf70ced33` | runner: `--program` schrijft het programma zonder op te lossen (de meting per document en case) |
| `6804621b5` | de kern: standen, grens, oordeel als paar, regel, consistentie, case, modelcontrole, weigering bij naam; de frames WV-1.5-* en WV-1.5-witness-* in de suite; README, `codes.tsv`; runner `--closed-inputs` |
| `9976c7297` | gate 6: de reden in de kanaallijst van `jutterswet-afleiden-uit-leeg` (code ongewijzigd, CLINGO-049) |
| (slotcommit) | dit Result |

De predicaten in een document met `inferFromEmpty: false` (alleen daar geschreven):

| predicaat | betekenis |
|---|---|
| `settled(I,A)` | het attribuut staat vast als leeg |
| `hasnot(I,K)` | van het kenmerk staat vast dat het niet geldt |
| `stands(I,A)` | het attribuut staat: `val/3` of `settled/2` |
| `ob<n>(S)` | de grens n (één waarde of één atomaire voorwaarde) bereikt op S een lezing die niet staat; `ob<n>_r<k>(S)` per lezing, in de volgorde van ch. 5.11 |
| `jd<n>(S)` | een kant van het oordeel van een voorwaarde (voor de vorm zelf of voor een ander, waar of onwaar) |
| `cj<n>_<i>(S,H)` | de eerste i leden van een samengestelde voorwaarde staan voor een ander, H ervan waar (de poort van het lid, voorlopig lezen) |
| `wnil<i>(S)` | van schrijver i (index in het document) staat vast dat hij op S niets vult |
| `vdef<n>(S)` | de waarde van een regel heeft een waarde (om "beslist met een vaststaand lege waarde" te zeggen) |
| `wnila(S,A)` | elke rekenende schrijver van A op het concrete type van S vult niets; daar staat de invoerwaarde |

Gewijzigde bestaande test (de kaart verandert het gedrag): "inferFromEmpty false: refused before translation with
CLINGO-049, form document" en de runner-check ernaast verwachtten dat clingo `ref/test/fixtures/infer-from-empty`
weigert. clingo draagt dat document nu: de checks heten "the document translates (ch. 1.5)" en "the runner plays it,
meldplichtig of V3 open" (PASS, 4 verwachtingen). Fixture en case ongewijzigd.

### Per eis van "Acceptance (deel 1)"

- **De getuigen van ref spelen in clingo** (`WV-1.5-witness-*`, suite van gate 6; de fixtures op hun plaats gelezen).
  Spelen, met ref ernaast, 66 cases: `absent-link`, `absent-link-path`, `characteristic`, `comparison`, `compound-all`,
  `compound-any`, `compound-counting`, `compound-none`, `consistency`, `filled-test`, `initialisation`, `let`, `marker`,
  `navigated-read`, `settled-empty`, `settled-empty-date` (16). Geweigerd, 74 cases: met CLINGO-049 en de slug
  `defaults-group` (`composite-filled-test`, `defaults-characteristic`, `defaults-stack`, `defaults-subtype`,
  `filled-test-not-closure`, `settled-empty-stack`), `set-fold` (`empty-by-fault`, `fact-creation`, `listed-fold`,
  `set-fold`, `settled-empty-fold`), `object-creation`, `quantifier-fold`; met een grens van elk document: `arithmetic`
  (CLINGO-005, een niet-literale grens van een Clamp; ook een TextComposition, CLINGO-042) en `exact-digits` (CLINGO-046).
  Wachten op het veld `complete` (kaart `complete-member`): `closed-input`, `input-above-computation`; de check houdt
  dat hun `closed.json` bestaat, zodat de regel verdwijnt waar de reden verdwijnt. De suite noemt elke getuige in
  precies één lijst, in beide richtingen. Rood: alle 140 witness-checks rood op de code van vóór (weigering CLINGO-049
  `document`).
- **Een frame per principe** (suite van gate 6, ref en clingo op dezelfde verwachtingen van de case, verifier-schoon):
  de drie standen van een invoerlid (WV-1.5-open-input, -no-open-without-the-property, -closed-every-input,
  -case-characteristics-absent, -case-attribute-open/-empty/-without-the-property, -input-per-object,
  -outside-the-window); het afgeleide lid (WV-1.5-derived-attribute met twee schrijvers, -derived-characteristic,
  -derived-chain in beide volgordes, -input-per-object met een schrijver op een subtype); wat vaststaat
  (WV-1.5-settled-operand); een open lezing (WV-1.5-open-passes-on, ook zonder de eigenschap met EF-031); het bereik:
  deeltal (WV-1.5-reached, 0 en open), grens (WV-1.5-boundary-deposit met EF-005, WV-1.5-boundary-fault: EF-014 bij
  "het TARIEF maal zijn gewicht", geen fout en open bij "zijn gewicht maal het TARIEF"), binder geweigerd
  (WV-1.5-reached (binder), `let-binder-not-first`); de tabellen (WV-1.5-table-and/-or/-none elk 9 rijen, -not 3,
  -at-least-2/-at-most-1/-exactly-1 de drie rijen van de spec, WV-1.5-atom); de toets op leeg en gevuld
  (WV-1.5-filled-test-two-valued/-open/-settled, met de sluiting, de gevuld-toets in "en", de ontkenning "geen van"
  (eigen ontkenning) en de bewakingstoeslag; -computed met EF-005 en zonder feit); de ontkende kenmerkcheck
  (WV-1.5-negation); de regel (WV-1.5-rule-decides/-fills-nothing/-open-condition/-open-value); de initialisatieregel
  (WV-1.5-initialisation, onvoorwaardelijk en voorwaardelijk); de consistentieregel (WV-1.5-consistency); de lezing over
  een link (WV-1.5-link-absent/-present-open/-present-filled, met rolcheck); invoer naast een schrijver
  (WV-1.5-input-beside-a-writer); daarbij het voorlopig lezen (WV-1.5-past-undetermined-deposit, -fault met EF-011).
  Rood: elk frame met de eigenschap rood op de code van vóór (112 checks buiten de getuigen; de frames zonder de
  eigenschap zijn de controle en groen). Log `logs/1-12-red.log` in de werkmap (de suite van nu met stubs van de nieuwe
  interface in een losse worktree op `bcabd51eb`, opgeruimd).
- **De weigering**: één functie, `Translate.refuse_uncarried`, met de lijst `Translate.uncarried_forms`: de 22 slugs van
  ch. 1.5 in hun volgorde (WV-1.5-uncarried-forms houdt die kop gelijk aan `Uchar_ref.Eval.uncarried_forms`), daarna
  `let-binder-not-first`, `defaults-group`, `quantifier-fold`, `set-fold`, `if-then-else`, `object-creation`,
  `fact-creation`. De eerste van de lijst die het document draagt wordt genoemd (frames voor de volgorde binnen de eigen
  slugs en tabel-vóór-eigen). `codes.tsv`: CLINGO-049, functie `refuse_uncarried`; gate 8 (`check-exec-codes`) groen.
- **Een document zonder de eigenschap verandert niet**: `dump_programs.py` in de werkmap schrijft het programma van elke
  golden- en rode case (`clingo_runner --program`) en vergelijkt twee mappen. 1307 paren, waarvan 1300 van documenten
  zonder de eigenschap: 0 verschillen (`logs/1-programs-base` tegen `logs/1-programs-07`, en opnieuw na de laatste
  wijziging). De 7 andere: de golden met zes cases en de rode case `jutterswet-kenmerk-wel-en-niet`. Gate 6, 8 groen,
  gate 13 zie onder.
- **De eigenschap op één plaats**: `grep -c infer_from_empty backends/clingo/lib/*.ml` vóór: `translate.ml` 3 (de
  weigering van het document, naam en lezing), `model.ml` 0; na: `translate.ml` 2, `model.ml` 1. De plaatsen:
  `refuse_uncarried` (welke vormen geweigerd worden), `translate_guarded` (`ctx.strict` gezet, één keer), `Model.check`
  (alleen daar is iets open). Waar de vertaling `ctx.strict` leest, met de reden: `open_read` (een lezing hoort bij een
  grens; zonder grens faalt het luid), `no_writer_decides` (de invoerwaarde van een afgeleid lid staat waar elke
  schrijver niets vult), `translate_rule` twee keer (de voorwaarde wordt beoordeeld als paar; de drie uitkomsten in
  `fills_nothing`), `translate_consistency` (meldt op "staat vast onwaar voor zichzelf"), `translate_case`
  (`input_states`), en in `translate_guarded` het weglaten van de ontsuikering van ch. 7.7, de regels van `stands/2`,
  `member_states` en de `#show`-regel. Geen arm van een voorwaarde of uitdrukking heeft een tweede kopie: de atomaire
  armen zijn ongewijzigd en staan binnen een grens; de lezingen melden zich op zes plaatsen bij die grens
  (`member_read` twee keer, `symbolic_term`, `sum_term`, `struct_base`, `dt_term`); het bereik loopt mee in de deling
  (`reached_after`) en de `Let` (`bind_let`). De samengestelde voorwaarde, de kenmerkcheck en de toets op leeg en gevuld
  hebben in `judge` een eigen vertaling als oordeel: de tweewaardige keten van ch. 5.10 telt één kant, de tabel van
  Kleene twee. Omvang van de diff tegen `bcabd51eb` (erbij/eraf): `translate.ml` 624/42, `test_clingo.ml` 743/20, `model.ml`
  25/7, `clingo_runner.ml` 24/4, `README.md` 34/7, `codes.tsv` 1/1, `check-clingo.sh` 1/1.
- **Los groen**: zie "Gates, los gedraaid".

### [L3] en [S1]

Geweigerd in een document met de eigenschap, met de eigen slug `let-binder-not-first`: een `Let` waarvan de body iets
anders leest vóór zijn binder (in de volgorde van ch. 5.11). Dat is de vorm van beide hoeken: de lowering houdt de
waarde van de binder naast elke lezing van de body, en waar de eerste lezing van de binder niet de eerste lezing is,
zou een lezing die zij niet bereikt meetellen ([S1]: de binder in een arm van een geneste Let; [L3]: de binder in de
matrix van een kwantorvouw). Frame "README [S1] with the property"; [L3] vraagt een creatie en een kwantorvouw, die in
dit deel ook geweigerd worden. Een `Let` die zijn binder eerst leest wordt gedragen (WV-1.5-let, getuige `let`): dan
bereikt de evaluatie de waarde van de binder zodra de body begint, en de rest van de body alleen waar de binder een
waarde heeft.

### Voorlopige besluiten

1. **Open lezing als grens-predicaat, niet als merkteken per lid.** Elke lezing die de evaluatie binnen een grens
   bereikt schrijft `ob<n>(S) :- bereik, pad, not stands(X,a)`; een waarde, een oordeel en een deponering staan onder
   `not ob<n>(S)`. Negatie over `stands` (dat positief is: `val` of `settled`) — op predicaatniveau een cyclus door
   negatie zoals `coalesce0` die al heeft, op grondniveau gestratificeerd zolang het document dat is. Zo blijven de
   atomaire armen (vergelijking, cijfertoets, rolcheck, marker, datum-tijd) ongewijzigd: wat zij met een open lezing als
   afwezigheid berekenen wordt door de grens ongeldig gemaakt.
2. **Een fout na een open lezing**: `emit_viol` voegt binnen een grens `not ob<n>_r<k>(S)` toe voor elke eerdere lezing
   (vertaalvolgorde = evaluatievolgorde, dankzij de `Let`-weigering); dezelfde fout maakt de grens open, zodat een fout in
   een voorlopig gelezen lid het lid onbepaald laat. De poort van het lid (`member_gate`) staat alleen in de fout en de
   deponering, niet in de open-regel.
3. **Voorlopig lezen**: een lid wordt als ch. 5.10 het leest gelezen waar alle eerdere leden voor een ander vaststaan en
   de stoptrigger van ch. 5.10 nog niet vuurde (`cj<n>_<i>`); anders is het voorlopig (deponering vervalt, fout →
   onbepaald) of niet gelezen.
4. **Geen ontsuikering van ch. 7.7** in een document met de eigenschap: een initialisatie naast een berekening wordt
   natief gelezen, zoals ref; zij schrijft ook naast een schrijver die het doel open laat (`not wdecides`).
5. **Schrijver buiten zijn venster** vult niets: `wnil<i>(S) :- isa, not valid(N)`, ongegate (WV-1.5-outside-the-window).
6. **Eigen slugs na die van ch. 1.5**, in de volgorde `let-binder-not-first`, `defaults-group`, `quantifier-fold`,
   `set-fold`, `if-then-else`, `object-creation`, `fact-creation`; de walk volgt die van ref (ook defaults en
   creaties), zodat een vorm van de tabel voorgaat en het document dezelfde slug krijgt als bij ref waar die er één heeft.
7. **Getuigen die een grens van elk document raken** (`arithmetic`: CLINGO-005; `exact-digits`: CLINGO-046) houden die
   code en krijgen geen CLINGO-049: de reden is niet de lezing van ch. 1.5. De opdracht vroeg CLINGO-049 voor elke
   andere getuige; dit wijkt daarvan af.
8. **`--closed-inputs`** op de runner en `?closed_inputs` op `translate`/`translate_guarded`: alle invoerleden volledig.
   Geen lezer voor `closed.json` (opdracht).

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, de kolom van de stand: "ref, clingo and the Drools build refuse a document that carries it before they evaluate,
translate or build, with `REF-014`, `CLINGO-049` and `DROOLS-370` (key `infer-from-empty-false`, parameter `form`, here
`document`)." wordt "The Drools build refuses a document that carries it before it builds, with `DROOLS-370` (key
`infer-from-empty-false`, parameter `form`, here `document`)." Na de zin over ref toevoegen: "clingo carries the reading
for a rule (an equation, a calculation, a characteristic attribution, an initialisation, read without the ch. 7.7
desugaring), a consistency rule, and a condition or an expression without a timeline, a fold, an IfThenElse or a Let
that reads something before its binder; settled leeg is derived positively (`settled/2`, `hasnot/2`), open is the
absence of every atom, and a value, a judgement and a deposit stand only where no read of their boundary is open; it
refuses a document with another form by name: `CLINGO-049` with the slug of the form as `form`, the first of the list of
ch. 1.5 and after it `let-binder-not-first`, `defaults-group`, `quantifier-fold`, `set-fold`, `if-then-else`,
`object-creation`, `fact-creation`." In "is refused by name by clingo (gate 6)" toevoegen "(form `defaults-group`)".
Getuigenkolom erbij: "WV-1.5-* and WV-1.5-witness-* in the suite of gate 6, with ref's outcome beside clingo's".

### Voorstel voor `spec/ASP-TRANSLATION.md` (deel 3 schrijft de sectie)

Een sectie "Open values" met de alinea die nu in `backends/clingo/README.md` staat (de standen en hun predicaten, de
grens per waarde en per atomaire voorwaarde met het bereik van deling en `Let`, het oordeel als paar en de tabel, de drie
uitkomsten van een schrijver, de weigering), plus de vier keuzes van "Voorlopige besluiten" 1–4.

### Voorstel voor `CLAUDE.md`

Geen: de alinea's van gate 6 en 8 noemen geen document met de eigenschap.

### Open punten

1. **De binder van WV-1.5-reached** (de binder alleen als deler) wordt geweigerd, niet gedragen. Dragen vraagt de
   behoefte per pad in plaats van op de hele body (README [V1]–[V3]); dat verandert de vertaling van elke Let-body en
   dus het programma van documenten zonder de eigenschap.
2. **De kenmerkcheck met een versmallende host** (ch. 5.8b regel 4) heeft in `judge` een arm (onwaar en vaststaand op
   een object dat geen host is), maar geen frame: geen getuige of golden met de eigenschap gebruikt hem.
3. **Getuigen `arithmetic` en `exact-digits`** spelen in geen enkel document door CLINGO-005 en CLINGO-046; het deel
   van `arithmetic` zonder Clamp en TextComposition (plus, Rounding, datum) wordt daardoor niet tegen ref gehouden.
4. **De melding van een open attribuut met een waarde in de case** zegt in `Model.check` "is underived (leeg)" bij een
   `attributeValue`-verwachting; de uitkomst klopt, de tekst niet (het lid is open).
5. **De rode stand** van elk frame is de weigering van het hele document (CLINGO-049 `document`); er is geen rode stand
   die alleen het principe raakt (zoals ref met "niets is open"-stubs had). Een tussenstand zonder de grens zou dat geven.

### Gates, los gedraaid

Op `6804621b5` + `9976c7297` (logs in de werkmap):

- gate 1 (`1-10-check-self-contained.log`): check-self-contained: PASS
- gate 3 (`1-10-check-verifier.log`): check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 509 golden clean; 657 negati …
- gate 5 (`1-10-check-oracle.log`): check-oracle: PASS (ref suite 4033 worked examples, floor 489; 1205 cases replayed exactly, floor 427; preservation with complete knowledge: 280 goldens and 784 …
- gate 6 (`1-13-gate6.log`): check-clingo: PASS (backend suite 3772 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 11 golden(s) in the refusal channel, by name, 36 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
- gate 7 (`1-10-check-noop.log`): check-noop: PASS (1163 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
- gate 8 (`1-10-check-red.log`): check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; 181 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 301 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
- gate 9 (`1-10-check-catala.log`): check-catala: PASS — frontend suite green (262 checks, floor 201); 13 catala golden(s) regenerate byte-identically (floor 12)
- gate 13 (`1-15-gate13.log`): check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
- gate 14 (`1-10-check-flogic.log`): check-flogic: PASS — frontend suite green (164 checks, floor 81); 3 flogic golden(s) re-emit byte-identically (floor 1)
