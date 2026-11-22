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
| `1bf43f5b8` | runner: `--program` schrijft het programma zonder op te lossen (de meting per document en case) |
| `f12549e9b` | de kern: standen, grens, oordeel als paar, regel, consistentie, case, modelcontrole, weigering bij naam; de frames WV-1.5-* en WV-1.5-witness-* in de suite; README, `codes.tsv`; runner `--closed-inputs` |
| `48654d2ed` | gate 6: de reden in de kanaallijst van `jutterswet-afleiden-uit-leeg` (code ongewijzigd, CLINGO-049) |
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

Op `f12549e9b` + `48654d2ed` (logs in de werkmap):

- gate 1 (`1-10-check-self-contained.log`): check-self-contained: PASS
- gate 3 (`1-10-check-verifier.log`): check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 509 golden clean; 657 negati …
- gate 5 (`1-10-check-oracle.log`): check-oracle: PASS (ref suite 4033 worked examples, floor 489; 1205 cases replayed exactly, floor 427; preservation with complete knowledge: 280 goldens and 784 …
- gate 6 (`1-13-gate6.log`): check-clingo: PASS (backend suite 3772 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 11 golden(s) in the refusal channel, by name, 36 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
- gate 7 (`1-10-check-noop.log`): check-noop: PASS (1163 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
- gate 8 (`1-10-check-red.log`): check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; 181 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 301 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
- gate 9 (`1-10-check-catala.log`): check-catala: PASS — frontend suite green (262 checks, floor 201); 13 catala golden(s) regenerate byte-identically (floor 12)
- gate 13 (`1-15-gate13.log`): check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
- gate 14 (`1-10-check-flogic.log`): check-flogic: PASS — frontend suite green (164 checks, floor 81); 3 flogic golden(s) re-emit byte-identically (floor 1)

## Result (deel 2)

*Bouwers deel 2 en 2b, 2026-11-22. Branch `card/open-values-in-clingo`, op `f73467236`.*

### Wat er is

| commit | wat |
|---|---|
| `dd8405c0d` | suite: de getuigen na de kaart `complete-member` |
| `36b14634d` | de stapel (kenmerkstapel, uitzonderingsgroepen, ongeordende groep, effectieve stapel van een subtype), het veld `complete`, de kwantorvouw |
| `b498d71c4` | de vouwen over een verzameling, de sluiting binnen een waarde, `IfThenElse`, object- en feitcreatie |
| `136728f46` | suite: de weigeringen na deel 2, de versmallende host, de melding van de modelcontrole |
| `eec84b93c` | gate 6: de goldens `jutterswet-afleiden-uit-leeg` en `jutterswet-volledig-gegeven` uit `EXPECTED_CHANNEL` |
| `a07d9432c` | een oordeel in een filter of kwantortoets per paar van vormonderwerp en lid; het filter van een vouw één keer, met zijn deponeringen en fouten op hun plaats; wat een creatie open laat; de limiet in een voorlopig gelezen lid |
| `b0483ca17` | de telling over een unie van instantieverzamelingen (de eigen slug `instance-set-union` vervalt); README |
| `22e244be3` | de stand van een zuiver invoerattribuut als feit (`given/2`); de poort van een fout in een filter loopt langs de links van de case |
| `dfd9353ce` | een oordeel zonder sluiting deelt zijn hulppredicaten met het oordeel voor een ander (architectuurpunt 2) |
| `da9f4720f` | README bij de lezing van open waarden |
| (slotcommit) | dit Result |

De predicaten die deel 2 toevoegt, alleen in een document met `inferFromEmpty: false`:

| predicaat | betekenis |
|---|---|
| `<tag>_gw<n>(S)`, `<tag>_rch<n>(S)`, `<tag>_rcx<n>(S)` | van default n staat vast dat hij voorrang geeft; default n wordt bereikt (elke hogere geeft voorrang); de initialisatie wordt bereikt, ook onder de eerste default die open is |
| `<tag>_gw(S)`, `<tag>_st(S)`, `grp<g>_nil(S)` | de keten of groep geeft voorrang; zij beslist, botst of geeft voorrang; van de groep staat vast dat zij niets vult (`member_states` leest dat als van een schrijver) |
| `qf<n>_*(K,X)` | de kwantorvouw: filter en matrix per wandeling (`ft`,`ff`,`tt`,`tf` en, waar een sluiting staat, `fu`,`fx`,`tu`,`tx`), het lid (`ot`,`of`,`xt`,`xf`), de omvang (`n`), bereikt (`rch`), voorlopig (`pv`), gelezen als ch. 3.9a (`rd`), het bereik open (`ro`) |
| `ff<n>_*(K,X)`, `fj<n>(F,X)` | het filter van een vouw over een verzameling: in het bereik, onbepaald, een lid ervoor onbepaald; een kant van het oordeel over het lid |
| `oc<n>(K)` | een sluiting in de grens n sloot over een open waarde (alleen geschreven waar er een sluiting staat) |
| `cc<n>(S)`, `ropen(I,rel)`, `fco<n>(S)` | de voorwaarde van een creatie staat vast; de relatie is open op I; de keten van een feitcreatie kruist een open relatie |
| `aopen(I,A)`, `kopen(I,K)` | de creatie die I maakte, liet het attribuut of kenmerk open |
| `given(I,A)` | een attribuut dat geen vorm schrijft en geen gemaakt object draagt, staat |

K is de sleutel van de plaats (`judged_at`): het onderwerp, en binnen een filter of een kwantortoets het paar van
vormonderwerp F en lid.

### Per vorm van "Wat je bouwt"

- **De stapel**: gedragen. Elke default heeft drie uitkomsten; een lagere default beslist waar van elke hogere positief
  vaststaat dat hij voorrang geeft; de initialisatie van een stapel sluit over de eerste default die open is; een lid
  buiten zijn venster geeft voorrang; een uitzonderingsgroep en een lid van een ongeordende groep beslist waar van elk
  ander lid vaststaat wat het doet (`stands_beside`). De ontsuikering van ch. 7.7 blijft uit: een initialisatie naast een
  berekening is een sluiting over de schrijvers (`not wdecides`, deel 1), een initialisatie in een groep is dezelfde
  sluiting over de stapel (`rcx`), en beide lezen haar voorwaarde en waarde als die van een regel of default. Frames
  WV-1.5-stack-first-decides, -gives-way, -settled-leeg, -initialisation, -opgave, -characteristic, -exception-groups,
  -unordered, -type-chain.
- **`complete`**: gedragen (`input_states` leest het effectieve type). Frames WV-1.5-complete-*, getuigen `closed-input`,
  `input-above-computation`, `input-above-computation-open`, golden `jutterswet-volledig-gegeven`.
- **De kwantorvouw**: gedragen, ook met een vormanker, een identiteitscheck of een andere vouw in filter of matrix en over
  een bereik waaraan een creatie een lid kan toevoegen. Frames WV-1.5-quantifier-fold-*, -past-undetermined-fold, -filter.
- **De vouwen over een verzameling, de sluiting binnen een waarde**: gedragen, ook genest, met een vormanker of een binder
  in het filter, over een payload en als telling over een unie. Frames WV-1.5-set-fold*, -closure-in-a-value*.
- **`IfThenElse`**: gedragen voor een vergelijking als voorwaarde; een toets op leeg of gevuld daar weigert in elk document
  (CLINGO-023). Frames WV-1.5-if-then-else.
- **De creaties**: gedragen, met een open relatie als open lezing van elke telling, vouw, kwantorvouw, rolcheck en
  genavigeerde lezing, en een feitcreatie over een keten die open is. Een gemaakt object heeft de standen van zijn
  toewijzingen: een getal, een somtype, een kenmerk uit een constante of een boolean attribuut. Frames
  WV-1.5-creation-condition, -creation-open, -creation-member*, -fact-creation*.
- **De staart van deel 1**: WV-1.5-reached (binder), [S1], [L3], WV-1.5-past-undetermined-barrier en -binder blijven
  geweigerd met `let-binder-not-first`, elk met een frame (dat van [L3] is nieuw). De reden: de lowering zet de waarde van
  een binder naast elke lezing van de body, en `bind_let` vertaalt haar lezingen vooraf; een lezing vóór de eerste lezing
  van de binder telt dan alleen waar de binder gevuld is, en de lezingen van de waarde tellen ook waar de binder nooit
  gelezen wordt. Dragen vraagt het bereik van de binder per pad (README [V1]–[V3]), wat het programma van elke Let in een
  document zonder de eigenschap verandert, of een tweede Let-lowering. De voorlopige lezing van een binder in een
  samengestelde `IfThenElse`-voorwaarde weigert bovendien in elk document met CLINGO-023. Het frame voor de versmallende
  host staat er (WV-1.5-negation (a narrowing host)), en de modelcontrole zegt "open" of "settled leeg" waar een lid geen
  waarde heeft (WV-1.5-case-attribute).
- **Het corpus**: beide goldens met de eigenschap spelen in gate 6 (12 cases). De rode case
  `jutterswet-kenmerk-wel-en-niet` heeft in het clingo-been van gate 8 geen regel nodig: het kenmerk in beide lijsten is een
  fout van de caselezer (EF-047), die clingo meldt zoals ref. `scripts/check-red.sh` is niet gewijzigd.

**Getuigen** (`ref/test/fixtures/strict-empty/`): 32 spelen (144 cases), 2 weigeren met een grens van elk document:
`arithmetic` (4 cases, CLINGO-005, een niet-literale grens van een Clamp; ook een TextComposition, CLINGO-042) en
`exact-digits` (3 cases, CLINGO-046). Geen getuige weigert met een eigen slug.

**Eigen slugs na de 22 van ch. 1.5**: één, `let-binder-not-first`, met de reden hierboven. Zij raakt 31 cases van 13
goldens die ref met de eigenschap draagt.

### De vragen van de nalezing

**a. De kale fouten.** Beide vormen zijn weg. Een vouw die vanuit een lid van een andere vouw gelezen wordt, is verifier-schoon
en wordt door ref gedragen: een vouw met een vormanker in een filter (goldens `fold-filter-subject-two-spellings`,
`jutterswet-schone-maand-na-zware-klacht`). Zij wordt gedragen, met elke andere lezing van het vormonderwerp in een filter
of kwantortoets: een verankerde lezing of check viel met "a helper rule over a member reads the form subject", en een
binder van een Let in een filter gaf een oordeel dat over elk vormonderwerp bestond, zonder fout. Een vouw waarvan de bron
geen attribuut van haar lid is, is niet verifier-schoon (WF-037, WF-083, WF-093; een payload is een lid van zijn eigen,
onder WF-127); `fold_reads` schrijft daar niets en de eigen vertaling van de vouw zegt de zin van de verifier.

Elke andere `fault` die deel 1 en 2 toevoegden:

| fout | waarom een document dat `refuse_uncarried` doorlaat haar niet bereikt |
|---|---|
| `boundary_read`: "the read of … stands outside one value or one atomic condition"; `fold_reads`: "a fold over a set stands outside …" | Een waarde van een regel, default of toewijzing en een atomaire voorwaarde worden binnen `within_boundary` vertaald. De enige plaats die dat niet deed, de toewijzing van een somtype aan een gemaakt object, viel zo: hersteld (frame WV-1.5-creation-member (a sum type)). |
| `within_boundary`: "a boundary inside a boundary" | `judge` wordt buiten een grens aangeroepen; vanuit een waarde alleen door `fold_reads`, die de grens van de waarde opzij zet. |
| `judge_ref`: "the judgement … asked before it is defined" | De verwijzing wordt bij het laden van de module gezet. |
| `judge`: "the navigated check reaches the to-many role" | WF-053 (`hop-to-many`); dezelfde zin als in elk document. |
| `judge`: "the … has no judgement here" | De voorwaarden zonder arm zijn die welke `refuse_uncarried` weigert: `whole-period`, `day-kind`, `time-window`, `rule-status-check`, `unresolved`. |
| `fills_nothing`: "rule … is not among the rules of its document" | `translate_rule` loopt over `ctx.doc.resolved_rules` zelf. |
| `refuse_uncarried`: "names …, which uncarried_forms does not list" | Elke slug in de wandeling staat in de lijst; geen document kiest een slug. |

Bij dit onderzoek gevonden en hersteld, elk met een frame: een gevuld-toets op een berekende waarde in een filter telde het
lid mee (7 in plaats van 4); een deponering van een filter bleef staan waar de vouw door een ander lid open is; een fout
van een filter ontstond na een open lezing; een kenmerk dat een creatie uit een boolean attribuut toewijst, en één dat zij
"onwaar" toewijst naast een regel; een open toewijzing naast een regel die niets vult; een feitcreatie over een open keten;
de bereikwacht in een grens zag het lid van de kwantorvouw niet (CLINGO-035 ontbrak); de limiet CLINGO-034 in een
voorlopig gelezen lid maakte het lid onbepaald in plaats van te zeggen dat het programma het niet weet; een attribuut en een
kenmerk van één naam golden als één lid.

**b.** Het commentaar staat bij zijn functie: `translate_chain`, `open_read`, `boundary_read`. Ook het commentaar bij de
ontsuikering in `translate_guarded` en dat boven `uncarried_forms` beschrijft de huidige stand.

**c. Kan de vertaling van ch. 1.5 de enige worden?** Nee, nu niet.

- *De kosten zijn geen bezwaar.* Van de 421 goldens zonder de eigenschap die clingo speelt, vertaalt hij er 262 ook met
  haar, en alle 262 spelen met de eigenschap en `--closed-inputs` hun eigen 737 cases (`jutterswet-keurquotient` vertaalt hij
  ook, en weigert hij in beide met CLINGO-039). Over die 262: de runner met processtart 11,8 s tegen 12,8 s (factor 1,08);
  clingo zelf 0,66 s tegen 1,46 s (2,2); gegronde regels 24 715 tegen 65 698 (2,7); programmaregels 39 836 tegen 113 253
  (2,8). De andere 159 dragen een vorm zonder lezing (147) of een Let die zijn binder niet eerst leest (12).
- *Het bezwaar is dat de oude armen blijven.* Ch. 1.5 geeft 22 vormen geen lezing, en een op de drie goldens draagt er een
  (tijdlijnen, dagsoorten, verdelingen, uniciteit, lijsten, dimensies). Daar is de samengestelde voorwaarde van
  `cond_atoms_raw` de enige vertaling: per periode (`period_condition`), in de dagsoorttabel, in het filter van een totaal per
  element en van een lijst; haar kwantorvouw evenzo. Die twee armen vervallen pas als ch. 1.5 die vormen draagt. De
  vertaling van ch. 1.5 eerder ook voor gewone documenten gebruiken, ruilt de schakelaar "het document draagt de eigenschap"
  voor een schakelaar "het document draagt zo'n vorm", met dezelfde twee armen: dan geven twee documenten die alleen in een
  tijdlijnlid verschillen, voor dezelfde regel twee vertalingen. Dat is slechter leesbaar dan nu, en het geeft de eis van
  gelijke programma's op.
- *Wat al één vertaling is*: de atomaire voorwaarde, elke uitdrukking, de vouw (haar vier armen lezen hun filter nu op één
  plaats, `fold_filter`), de unie en de creatie. Wat dubbel is: de samengestelde voorwaarde, de kwantorvouw, de kenmerkcheck
  en de toets op leeg en gevuld (in `judge`), en de keten van een defaults-groep (`translate_chain`, elf lezingen van de
  schakelaar).
- *De volgende stap die wel los kan*: een defaults-groep op een doel zonder tijdlijn met drie uitkomsten lezen in elk
  document dat geen enkele vorm zonder lezing draagt. Dat haalt de tweewaardige keten niet weg (een document met zo'n vorm
  houdt haar), dus ook dat levert pas op als ch. 1.5 de tijdlijnstapel draagt. Ik stel voor het nu te laten.

### De rode stand

De suite van nu in de losse worktree `logs/2-red-wt` (opgeruimd), met in de kopie de fout van `ov_fault_frame` en van de
limiet-check afgevangen:

- op `dd8405c0d` (vóór deel 2): 192 van 4237 checks rood, alle over documenten met de eigenschap; elke kant van ref groen,
  elk frame zonder de eigenschap groen (log `2b-16-red-67f769971.log`).
- op `eec84b93c` (vóór 2b): 23 rood, de frames van de herstellingen onder a en van de unie (log `2b-17-red-4fb79aa09.log`).

### Byte-gelijk

`dump_programs.py` op de tip tegen `logs/2-programs-base`: 1313 paren, 1300 van documenten zonder de eigenschap, 0
verschillen (`logs/2b-26-dump.log`). De 13 andere: de twee goldens met de eigenschap (12 cases, anders) en de rode case
(gelijk).

### De kosten van de grens

`measure_cost.py` (hoogstens vier processen, rem op 8 GB en tien minuten; geen document raakte de rem; logs
`2b-27-cost.log`, `2b-28-cost-all.log`). De twintig goldens met de langste tijd, clingo's eigen tijd en gegronde regels,
opgeteld over de cases, zoals het document is tegen met de eigenschap:

| golden | cases | tijd (s) | regels | factor tijd / regels |
|---|---|---|---|---|
| `jutterswet-opgave-vormen` | 15 | 0,03 → 0,09 | 324 → 1033 | 3,0 / 3,2 |
| `jutterswet-opgave-subtype` | 12 | 0,02 → 0,05 | 303 → 1045 | 2,0 / 3,5 |
| `jutterswet-opgave-boven-berekening` | 12 | 0,01 → 0,05 | 273 → 1170 | 4,0 / 4,3 |
| `jutterswet-deling-in-afwijking` (en `-omgekeerd`) | 10 | 0,03 → 0,05 | 476 → 1207 | 1,7 / 2,5 |
| `anchor/ww-herleving-inkomen` (en `ww-herleving-inkomen-rgs`) | 10 | 0,02 → 0,04 | 298 → 1074 | 2,0 / 3,6 |
| `anchor/jutterswet-nestuitzondering` | 9 | 0,01 → 0,02 | 135 → 401 | 2,0 / 3,0 |
| `jutterswet-registervoorrang` | 7 | 0,01 → 0,02 | 203 → 1361 | 3,0 / 6,7 |
| `jutterswet-opgave-bedrag` | 7 | 0,01 → 0,02 | 169 → 571 | 3,0 / 3,4 |
| `jutterswet-opgave-datum` | 7 | 0,01 → 0,01 | 129 → 462 | 2,0 / 3,6 |
| `synth-afrondingswaaier` (en `-rgs`) | 5 | 0,01 → 0,01 | 174 → 292 | 1,5 / 1,7 |
| `jutterswet-samenloop` | 6 | 0,01 → 0,01 | 70 → 214 | 1,2 / 3,1 |
| `jutterswet-twee-schrijvers-aan-fixpunt` | 6 | 0,01 → 0,01 | 57 → 162 | 1,0 / 2,8 |
| `anchor/jutterswet-vondstwaardering` | 6 | 0,01 → 0,01 | 61 → 162 | 1,0 / 2,7 |
| `synth3-nestkorting` | 5 | 0,01 → 0,01 | 134 → 262 | 2,0 / 2,0 |
| `jutterswet-paasvondst` | 5 | 0,01 → 0,02 | 177 → 347 | 1,3 / 2,0 |
| `jutterswet-loodsgrens` | 5 | 0,01 → 0,02 | 137 → 733 | 4,0 / 5,4 |
| `jutterswet-premietabel-twee-ofcellen` | 5 | 0,01 → 0,01 | 136 → 424 | 2,0 / 3,1 |

Alle twintig spelen. De langste case van de 262 goldens loopt 20 ms (`date-with-parts`); clingo meet in hele
milliseconden, dus een factor op een tijd onder 0,02 s zegt weinig. Boven 3: `jutterswet-opgave-boven-berekening` en `jutterswet-loodsgrens` (4,0), en bij de regels de goldens met een
kwantorvouw (tot 8,8, `jutterswet-ploegblokkade`, 133 → 1164). Geen enkele regel veroorzaakt het: een default krijgt er
drie predicaten en een grens bij, een kwantorvouw een tabel van tien predicaten per paar.

De groei met het bereik (`tools/scale.py`, één Jutter met 400 vondsten): een gefilterde som 8 → 12 ms, gewone regels 8 → 13
ms, een kwantorvouw 8 → 54 ms, kwadratisch in het bereik (de stop van ch. 3.9a telt per lid de leden ervoor; de gewone
vertaling doet dat alleen waar een fout in de matrix staat). Vóór `22e244be3` kostte de gefilterde som 680 ms: `stands/2`
ligt op de component van `val/3`, dus geen element van de som was een feit en clingo somde de mogelijke sommen op; dat is
hersteld met `given/2`. Een vouw over een afgeleid attribuut groeit lineair (400 vondsten: 9 → 12 ms, ook met open leden).

### Zoeken naast de suite

Niet in de repository; scripts in `werk/open-values-in-clingo/tools/`. Elke uitkomst tegen ref (`--end-state`, per lid
gevuld/vaststaand leeg/open, getallen, kanaal met codes):

- *behoud* (elke golden met de eigenschap en `--closed-inputs`, beide executors): 737 cases spelen in beide, 31 weigeren met
  `let-binder-not-first`, 16 met dezelfde limiet als het document zonder de eigenschap, 0 anders; 415 cases weigert ref en
  clingo met dezelfde slug van ch. 1.5.
- *open leden* (elke golden die clingo draagt: de case met alles wat zij niet geeft open, zonder elk invoerattribuut apart,
  en met elk lid volledig op één na): 7766 runs gelijk, 6 fouten in beide, 20 limieten, 2 verschillen (open punt 1).
- *willekeurige documenten* (`fuzz.py`: regels, defaults, initialisaties, kenmerken, consistenties, creaties, vouwen,
  kwantorvouwen, unies, ankers, Lets; verifier-schoon), op de tip 1500 documenten: 5364 cases gelijk, 380 geweigerd, 0 verschillen; over de opeenvolgende standen ruim 18 000 cases
  gelijk en 3 verschillen (open punten 2 en 3). De geweigerde cases raken de limiet "a helper rule over a member reads the
  form subject", die het document zonder de eigenschap ook heeft.

### De eigenschap op één plaats

`infer_from_empty`: `translate.ml` 2 (`refuse_uncarried`, `translate_guarded`), `model.ml` 1. `ctx.strict` wordt 32 keer
gelezen; per functie met de reden: `boundary_read` (een lezing hoort bij een grens), `expr_term` 3 (de lezingen van een vouw
vóór de vouw; sleutel en filter van een unie), `creation_condition` (de voorwaarde als oordeel), `no_writer_decides`,
`translate_rule` 2, `translate_consistency`, `translate_case` (deel 1), `translate_object_creation` 4 (de standen van een
gemaakt object: niets toegewezen, open, de grens van een waarde, de ongenoemde leden), `translate_fact_creation` (de keten
kan open zijn), `stands_beside` en `translate_group` (een lid naast een open lid), `translate_chain` 11 (het bereik van een
default en van de initialisatie, voorrang uit rechtvaardiging, waarde en uitzonderingen, de wandeling zelf),
`translate_defaults_group` 4 (een lid buiten zijn venster, de groep als schrijver, het kenmerk dat niet toegekend wordt, de
invoerwaarde). `translate_guarded` zet het veld en schrijft `stands/2`, `member_states` en de `#show`-regel. Omvang van de
diff tegen `f73467236` (erbij/eraf, deel 1 erbij): `translate.ml` 1533/211, `test_clingo.ml` 1782/20, `README.md` 70/7,
`model.ml` 38/15, `clingo_runner.ml` 24/4, `codes.tsv` 1/1, `check-clingo.sh` 0/2.

### Voorlopige besluiten

1. **De stapel natief**, met `gw`/`rch`/`rcx` per default en zonder ontsuikering; een botsende groep vult niets (het doel
   staat vast als leeg, met het feit), een groep die beslist niet toe te kennen vult niets op het kenmerk.
2. **De vouw als één lezing van haar grens**: eerst het bereik, dan alle filters, dan de waarden. Zij telt met haar eigen
   oordeel over het filter (`fold_filter`), niet met een tweede vertaling ervan.
3. **Elk oordeel en elke grens in een filter of toets hangt van het paar van vormonderwerp en lid af** (`judged_at`), ook
   waar het filter het vormonderwerp niet leest. Eenvoudiger dan per vorm uitzoeken; de kosten staan hierboven.
4. **De volgorde van fouten in een filter**: een fout wacht op elk lid ervóór in de volgorde van de case. Een lid dat een
   creatie toevoegt heeft in deze codering geen plaats in die volgorde, dus een fout van zo'n lid wacht op geen ander lid
   (luid).
5. **De matrix van een kwantorvouw telt als gelezen waar elke volgorde van de toegevoegde leden haar leest**, zoals in een
   gewoon document; een fout daar weigert met CLINGO-035.
6. **Een guard zonder EF-code is een limiet van de codering** en vuurt ook in een voorlopig gelezen lid (`limit_gate`): het
   programma antwoordt dan niet. De poort is de tabel van Kleene over de leden ervoor.
7. **Een feitcreatie over een open keten** laat de relatie open op het anker en op elk object van het doeltype, zoals ref; ch.
   1.5 zegt "on every object it could reach".
8. **Een open toewijzing is een eigen stand** (`aopen/2`, `kopen/2`): het lid is open tenzij een schrijver beslist.
9. **Een attribuut en een kenmerk van één naam zijn twee leden**, zoals ch. 1.5 ("a member"); ref houdt ze niet uit elkaar
   (open punt 1).
10. **`given/2`** voor een attribuut waarvan alleen de case de stand bepaalt; elders `stands/2`.
11. **De unie van instantieverzamelingen** leest per selectie als een vouw; de eigen slug verviel.

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, in plaats van het voorstel van deel 1: "The Drools build refuses a document that carries it before it builds, with
`DROOLS-370` (key `infer-from-empty-false`, parameter `form`, here `document`)." Na de zin over ref: "clingo carries the
reading for the same forms (a rule, a defaults group on a target without a timeline, a creation off a cycle, a consistency
rule, and a condition or an expression without a timeline, the quantifier fold, the folds over a set and the count of a
union of instance sets, and an IfThenElse over a comparison among them); settled leeg is derived positively (`settled/2`,
`hasnot/2`), open is the absence of every atom, and a value, a judgement and a deposit stand only where no read of their
boundary is open. It refuses a document with another form by name, `CLINGO-049` with the slug of the form as `form`: the
first of the list of ch. 1.5, and after it `let-binder-not-first` (a Let whose body reads something before its binder)." De
zin "is refused by name by clingo (gate 6)" over de goldens met de eigenschap vervalt: zij spelen. Getuigenkolom: "WV-1.5-*
and WV-1.5-witness-* in the suite of gate 6 (32 of the 34 witnesses play, with ref's outcome beside clingo's); goldens
`jutterswet-afleiden-uit-leeg`, `jutterswet-volledig-gegeven`". Rij 49 (`complete`): "clingo refuses" vervalt; "clingo reads
the field (`input_states`)".

### Voorstel voor `spec/ASP-TRANSLATION.md` (deel 3 schrijft de sectie)

De sectie "Open values" neemt de alinea over die nu in `backends/clingo/README.md` staat (met haar punten over waarde,
voorwaarde, filter, vouw, stapel en creatie), de tabel van predicaten hierboven en de besluiten 2–8.

### Voorstel voor `CLAUDE.md`

Geen: gate 6 en 8 noemen geen document met de eigenschap.

### Open punten

1. **ref houdt een attribuut en een kenmerk van één naam niet uit elkaar** (`World.opened`, `open_inputs.written`: sleutel op
   label en genormaliseerde naam). Kleinste document: een Vondst met het attribuut toeslag, dat een defaults-groep van
   gemerkt laat afhangen, en het kenmerk toeslag, dat een defaults-groep aan een volledig kenmerk geeft; gemerkt open. Ch.
   1.5: het attribuut is open, het kenmerk staat vast als niet. clingo zegt dat, ref noemt beide open. Golden
   `anchor/jutterswet-toelagegronden`, case 2, met elk lid volledig behalve `wachter`. Waar het kenmerk invoer is en het
   attribuut geschreven wordt, noemt ref het kenmerk vaststaand afwezig waar ch. 1.5 het open noemt.
2. **ref verliest een sluiting waar de slagboom van een Let in de voorwaarde van een `IfThenElse` valt.** Kleinste document:
   "het keurgewicht van een Jutter is, daarbij geldt: t is de som van het gewicht van zijn vondsten waarvan getuigen leeg is,
   1 als t gelijk is aan 3, anders t", over een vondst met een volledig leeg gewicht en open getuigen. Ch. 1.5 (een waarde die
   na een sluiting leeg is, laat haar doel open) geeft open, en clingo ook; ref geeft vaststaand leeg. Zonder de `IfThenElse`
   (body "t" of "t plus t") geeft ref open.
3. **Een §9.5-deponering in de matrix van een kwantorvouw over een bereik waaraan een creatie een lid toevoegt, ontbreekt in
   clingo, in elk document.** "Een Jutter is k indien geen van zijn vondsten een gewicht gedeeld door getuigen groter dan 3
   heeft", over V1 (gewicht 2, getuigen 0) en een gemaakte vondst (20, 1): ref leest V1 eerst en deponeert EF-005, clingo
   deponeert niets (de matrix geldt als gelezen waar elke volgorde haar leest). Een fout op die plaats weigert met
   CLINGO-035; een deponering niet. Zelfde uitkomst met en zonder de eigenschap.
4. **`let-binder-not-first`** (31 cases van 13 goldens): dragen vraagt het bereik van een binder per pad; zie "De staart van
   deel 1".
5. **Een kwantorvouw groeit met de eigenschap kwadratisch in het bereik** (400 leden: 8 → 54 ms): de stop van ch. 3.9a telt per
   lid de leden die ervoor kunnen staan, ook waar geen lid een deponering of een fout kan geven.
6. **De getuigen `arithmetic` en `exact-digits`** worden door hun grens in geen document tegen ref gehouden (open punt 3 van
   deel 1 staat nog).
7. **Een helper in een lid die het vormonderwerp leest** (een optelling met een verankerde lezing onder een vergelijking,
   `coalesce0`) weigert met een kale fout, in elk document: "a helper rule over a member reads the form subject … and not this
   one". Geen golden raakt haar; de willekeurige documenten in ongeveer een op de achttien cases.

### Gates, los gedraaid

Op `dfd9353ce` (`da9f4720f` wijzigt daarna alleen de README; logs in de werkmap). Gate 13 startte eenmaal terwijl een andere
bouwer net `make gates` begon; gestopt op procesgroep en opnieuw gedraaid toen die klaar was.

- gate 1 (`2b-30-check-self-contained.log`): check-self-contained: PASS
- gate 3 (`2b-30-check-verifier.log`): check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 510 golden clean; 657 negative rejected for their declared rule, floor 568; 502 on their declared arm (expect.txt), floor 411, 277 arm keys, floor 239, 844 name=value lines, floor 686; 374 keys of one template each in the reports, floor 340, 468 key literals in checks.ml, floor 442; 825  …
- gate 5 (`2b-30-check-oracle.log`): check-oracle: PASS (ref suite 4082 worked examples, floor 489; 1211 cases replayed exactly, floor 427; preservation with complete knowledge: 280 goldens and 784 cases play, floors 280 and 784; 151 goldens refused per form (timeline-member 49, allocation 21, day-kind 17, list-family 16, dimension-cell 8, match 8, uniqueness 8, creation-cycle 7, record 7, function-def 4, time-window 2, dimension-fold 1, rule-status-che …
- gate 6 (`2b-31-gate6.log`): check-clingo: PASS (backend suite 4238 checks, floor 83; 1181 cases replay exactly through clingo — ref = clingo over the corpus; 10 golden(s) in the refusal channel, by name, 30 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
- gate 7 (`2b-30-check-noop.log`): check-noop: PASS (1169 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
- gate 8 (`2b-32-gate8.log`): check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; 181 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 301 pinned parameter checks confirmed, floor 120; 47 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)
- gate 9 (`2b-30-check-catala.log`): check-catala: PASS — frontend suite green (262 checks, floor 201); 13 catala golden(s) regenerate byte-identically (floor 12)
- gate 13 (`2b-35-gate13.log`): check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)
- gate 14 (`2b-30-check-flogic.log`): check-flogic: PASS — frontend suite green (164 checks, floor 81); 3 flogic golden(s) re-emit byte-identically (floor 1)

## Result (deel 3)

*Bouwer deel 3, 2026-11-22. Branch `card/open-values-in-clingo`, op `0c27516df` (main met de kaart `let-in-composite`). De
hashes in de tabellen van deel 1 en 2 zijn die na de rebase.*

### Wat er is

| commit | wat |
|---|---|
| `f575f0261` | de slug `let-in-composite` in `refuse_uncarried`, met de vormgrens van ref; de eigen slug `let-in-filter`; README |
| `79d55954c` | een sluiting die de berekening van een binder toepaste, geldt bij elke lezing van die binder (`computation`, `li_closed`, `note_binder_read`); de getuigen `WV-1.5-closure-before-a-barrier` en `WV-1.5-shared-name` naast ref |
| `8754c89de` | een operand die leeg als nul leest (`coalesce0`) draagt in een filter het vormonderwerp in zijn sleutel, in een document met de eigenschap |
| `bc1e7d71b` | twee initialisaties in één stapel: elke beslist waar niets erboven beslist, ook onder een open initialisatie (`translate_chain`) |
| (slotcommit) | dit Result |

**De slug.** `uncarried_forms` is nu de 23 vormen van ch. 1.5 in hun volgorde (de suite houdt haar aan
`Uchar_ref.Eval.uncarried_forms`) en daarna `let-binder-not-first` en `let-in-filter`. De wandeling houdt per binder bij of
de plaats binnen de romp van zijn `Let` in een later lid van een samenstelling of in een kwantorvouw staat, zoals ref. Het
register `codes.tsv` verandert niet: de code, haar parameter en de functie zijn dezelfde, en de slugs staan er niet in.

**Wat vervalt.** In `translate.ml` en `model.ml` niets. Een samenstelling of kwantorvouw in de voorwaarde van een
`IfThenElse` vertaalt clingo in geen document (CLINGO-023), en de twee getuigen van de vervallen regels weigerde hij al met
`let-binder-not-first`: er was geen code die de barrière of de deponering van een binder in een voorlopig gelezen lid droeg.
Wat van een binder in een filter over is (`judged_at`, de sleutel per paar), draagt het eerste lid en het atomaire filter en
blijft. Vervallen zijn de frames `WV-1.5-past-undetermined-barrier` en `-binder`; hun documenten staan er als
`WV-1.5-let-in-composite (the condition of a choice, …)`. Het frame "README [L3] with the property" verwacht nu
`let-in-composite` in plaats van `let-binder-not-first`: de matrix van een kwantorvouw leest de binder, en de tabel gaat voor.

**De bindersluiting.** clingo hield de sluiting alleen bij de grens van de waarde. Waar het filter van een vouw in de romp de
binder las, stond de grens van dat filter er los van, en een later lid van het filter werd gelezen als ch. 5.10 in plaats van
voorlopig: de deponering bleef staan en de fout ontstond. De berekening heeft nu binnen haar grens een eigen atoom `oc<n>`; elke
lezing van de binder in een andere grens meldt het daar, op het bereik van die lezing (een binder als deler van een leeg deeltal
is niet gelezen). De getuigen van ch. 1.5 zelf (`WV-1.5-closure-of-a-binder-*` met de else-tak, `WV-1.5-binder-broken-off-*`)
weigert clingo met `let-binder-not-first` (de voorwaarde van de keuze is een samenstelling). Dat een afgebroken berekening niets
deponeert, volgt waar clingo een Let draagt uit de grens eromheen: de romp leest de binder eerst, dus een open lezing in de
berekening is een open lezing van die grens.

**De ref-fouten** (open punt 1 en 2 van deel 2) zijn op `main` hersteld. `anchor/jutterswet-toelagegronden`, de case met alles
wat zij niet geeft open, zonder elk invoerattribuut apart en met elk lid volledig op één na: 49 runs gelijk aan ref, geen
verschil (was 2, in case 2). `letclosure.py`: de vijf documenten open in ref en in clingo (de twee met een `IfThenElse` waren in
ref vaststaand leeg). Beide staan als frames met ref ernaast in de suite: `WV-1.5-shared-name` (11 frames, met defaults-groepen
en een gemaakt object: een regel op zo'n naam is WF-022) en `WV-1.5-closure-before-a-barrier` (7; "1 als t gevuld is" is
CLINGO-023).

**Twee initialisaties in één stapel** (aanvulling van de hoofdsessie). clingo maakte het doel open in drie gevallen waar ch. 1.5
het een waarde geeft ("it decides where nothing above it decides, also where a default above it is open"): de default open en de
geërfde initialisatie geeft voorrang, de default open en de geërfde open, en de default geeft voorrang en de geërfde is open. Nu
5 in alle drie. ref op `0c27516df` geeft in het derde geval 5 en in de eerste twee open; die twee staan daarom als verwachting
van clingo alleen in de suite (`WV-1.5-stack-two-initialisations`), de andere zeven met ref ernaast. Zodra ref de correctie op
`main` heeft, horen de twee rijen in dezelfde lijst.

### Getuigen

Suite 4238 → 4450 checks, 0 rood. Rood op de code van vóór elke wijziging (`logs/3-03-suite-red.log`, `binderplus.py`,
`logs/3-09-twoinit-red.log`):

| eis | frame | rood |
|---|---|---|
| de slug, in de volgorde van de tabel | `WV-1.5-uncarried-forms: the forms of ch. 1.5 head the list` | ja |
| een later lid, genest, de toets en het filter van een kwantorvouw, de waarde van een Let in een later lid, een keuze | `WV-1.5-let-in-composite (…)`, 8 documenten | 8 keer ("speelt", of `let-binder-not-first`) |
| zonder de eigenschap speelt elk | dezelfde zes documenten met een vouw | groen vóór en na |
| het eerste lid, en een binder die in een later lid gebonden is, wordt gelezen | `WV-1.5-let-in-composite (… is read, …)`, 11 frames | groen vóór en na |
| [L3] | `README [L3] with the property` | ja |
| de bindersluiting | `WV-1.5-closure-of-a-binder (…)`: 4 deponeringen en 3 fouten, elk met de getuigen open en volledig; "a read that is not reached" | 7 keer, alleen met de getuigen open |
| een Let in een filter | `WV-1.5-set-fold (a binder of a Let in an outer filter)` | ja (60 in plaats van 30) |
| een operand onder een operator in een filter | `WV-1.5-set-fold (the form subject under an operator in a filter, …)`, 4 frames | 3 keer een ander bedrag (16 in plaats van 1), 1 keer een kale fout |
| twee initialisaties | `WV-1.5-stack-two-initialisations (…)`, 9 frames | 3 keer |

### Byte-gelijk

`dump_programs.py` op de tip tegen `logs/2-programs-base` (de stand van vóór de kaart): 1313 paren, 1300 van documenten zonder de
eigenschap, 0 verschillen (`logs/3-12-dump.log`); de 12 andere zijn de cases van de twee goldens met de eigenschap.

### Zoeken naast de suite

Niet in de repository (`tools/`), op de tip, elke uitkomst tegen ref (`--end-state`: per lid gevuld, vaststaand leeg of open, getallen,
kanaal met codes):

- `fuzz3.py`, willekeurige documenten met een Let in de meeste regels (een sluiting in de berekening, een vouw in de romp met een filter
  dat de binder in het eerste lid leest, Lets in filters): 1219 verifier-schone documenten, 4876 cases gelijk, 0 verschillen, 0
  geweigerd (`logs/3-21-fuzz.log`). Op de standen ervoor 9812 cases gelijk en 2 verschillen, beide open punt 3 van deel 2 (een
  deponering in de matrix van een kwantorvouw over een bereik met een gemaakt lid).
- `fuzz.py`, de generator van deel 2: 862 documenten, 3448 cases gelijk, 0 verschillen, 0 geweigerd (`logs/3-23-fuzz.log`). De kale fout
  van open punt 7 van deel 2 (een optelling met een verankerde lezing in een filter) komt in een document met de eigenschap niet meer voor.
- behoud (elke golden met de eigenschap en `--closed-inputs`): 737 cases spelen in ref en clingo, 29 cases van 12 goldens weigert
  clingo met `let-binder-not-first` en geen met `let-in-filter`, 16 met dezelfde limiet als zonder de eigenschap, 417 weigeren beide met dezelfde slug
  (`logs/3-22-preservation.log`; de golden `jutterswet-grens-per-jutter` weigeren beide nu met `let-in-composite`).

### Voorlopige besluiten

1. **`let-in-filter` is een eigen slug**, en geen sleutel over drie variabelen: een Let in een filter of kwantortoets waarvan een
   dieper filter de binder leest, gaf zonder melding een fout bedrag. Dragen vraagt het lid van het buitenste filter in de sleutel
   van elk oordeel, elke grens en elk hulppredicaat; geen golden en geen getuige heeft de vorm.
2. **De sleutel van `coalesce0` alleen in een document met de eigenschap**: zonder haar zou het programma van een gewoon document
   met zo'n filter veranderen (open punt 1).
3. **Een sluiting van een binder wordt gemeld in elke andere grens dan die van zijn Let**; die laatste draagt haar al bij het
   binden, omdat de romp de binder eerst leest.

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

Rij 48, in plaats van het voorstel van deel 2: "ref, clingo and the Drools build refuse a document that carries it …" wordt "A
document with a form an executor does not carry is refused before it is evaluated, translated or built, with `REF-014`,
`CLINGO-049` and `DROOLS-370` (key `infer-from-empty-false`, parameter `form`)". Na de zin over ref: "clingo carries the reading
for the same forms, the IfThenElse over a comparison; settled leeg is derived positively (`settled/2`, `hasnot/2`), open is the
absence of every atom, and a value, a judgement and a deposit stand only where no read of their boundary is open. After the forms
of ch. 1.5 it refuses, by a slug of its own, `let-binder-not-first` (a Let whose body reads something before its binder) and
`let-in-filter` (a Let in the filter of a fold or in a quantifier test whose binder a filter in its body reads)." De zin "The golden
`jutterswet-afleiden-uit-leeg` plays in ref and is refused by name by clingo (gate 6) …" verliest clingo: hij speelt haar.
Getuigenkolom: "WV-1.5-* and WV-1.5-witness-* in the suite of gate 6 (32 of the 34 witnesses play); goldens
`jutterswet-afleiden-uit-leeg`, `jutterswet-volledig-gegeven`". Rij 49: "clingo reads the field (`input_states`)".

### Voorstel voor `CLAUDE.md`

Geen.

### Open punten

1. **Een binder onder een operator in het filter van een vouw mengt in een gewoon document de vormonderwerpen**, zonder melding, op
   `main`: "het grenstotaal van een Jutter is, daarbij geldt: de grens is zijn drempel, de grens plus de som van het gewicht van
   zijn vondsten waarvan het gewicht plus de grens groter is dan 12", over J1 (drempel 10, vondsten van 20 en 5) en J2 (drempel 1,
   vondsten van 5 en 10): ref 35 en 1, clingo 35 en 16. Ook bij min, verminderd met en maal, en onder een afronding of grens. De
   oorzaak is `coalesce0` (een hulppredicaat op het lid alleen); `helper_key`, zoals nu in een document met de eigenschap, herstelt
   het, met 0 programma's anders over het corpus, maar maakt een tellende samenstelling met zo'n lid in een filter ("hoogstens 1
   van") een grondingsfout, omdat haar hulppredicaat (`cq<n>_m`) het vormonderwerp niet draagt. Dezelfde grondingsfout geeft een
   tellende samenstelling met een vergelijking `=` of `≠` met de binder nu al (luid). Geen golden heeft een van deze vormen.
2. **Een Let in een filter waarvan een dieper filter de binder leest, met `=` of `≠`**, geeft in een gewoon document op `main` een
   ander bedrag dan ref (60 in plaats van 50 bij "gelijk aan", leeg in plaats van 60 bij "ongelijk aan", op het document van het frame
   "a binder of a Let in an outer filter"); met "groter dan" speelt het. Zelfde oorzaak: de sleutel van het hulppredicaat kent het
   lid van het buitenste filter niet. Met de eigenschap geweigerd (`let-in-filter`).
3. **`WV-1.5-stack-two-initialisations`**: twee rijen alleen voor clingo, tot ref de correctie draagt (hierboven).
4. Van deel 2 blijven staan: 3 (een deponering in de matrix over een bereik met een gemaakt lid, in elk document), 4
   (`let-binder-not-first`), 5 (een kwantorvouw groeit kwadratisch), 6 (`arithmetic`, `exact-digits`); 1 en 2 zijn hersteld, 7 geldt
   nog in een gewoon document.
5. De vijf open punten van `backlog/done/20261121-clingo-filled-test.md`, de poot van gate 6 en de sectie in
   `spec/ASP-TRANSLATION.md` zijn niet gebouwd (aanwijzing van de eigenaar).

### Gates, los gedraaid

Op de inhoud van `bc1e7d71b`, één tegelijk, steeds wanneer geen andere bouwer een zware run had (`logs/3-20-*`).

- gate 1: check-self-contained: PASS
- gate 3: check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 510 golden clean; 657 negative rejected for their declared rule, floor 568; 502 on their declared arm (expect.txt), floor 411, 2 …
- gate 5: check-oracle: PASS (ref suite 4217 worked examples, floor 489; 1211 cases replayed exactly, floor 427; preservation with complete knowledge: 279 goldens and 782 cases play, floors 279 and 782; 152 goldens refused per form (timeline-member 49, allocation 21, day-kind 17, list-family 16, dimension-cel …
- gate 6: check-clingo: PASS (backend suite 4450 checks, floor 83; 1181 cases replay exactly through clingo — ref = clingo over the corpus; 10 golden(s) in the refusal channel, by name, 30 cases refused with the pinned codes; the channel rule passes 22 rows of its own)
- gate 7: check-noop: PASS (1169 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)
- gate 8, met een PATH zonder mvn: `check-exec-codes: PASS — 14 REF codes and 50 CLINGO codes …`; het ref- en het clingo-been zonder een rode regel. De ene FAIL-regel is die van het
  Drools-been, dat niet gedraaid is (mvn ontbreekt).
- gate 14: check-flogic: PASS — frontend suite green (164 checks, floor 81); 3 flogic golden(s) re-emit byte-identically (floor 1)
- gate 12, 13, 17 en `make gates`: niet gedraaid (hoofdsessie).

## Landing

`make gates` op `9423f6e9f`: alle 17 gates groen (1229 s, after-run groen; gate 6 duurt nu 1221 s in de set, tegen 704 s
daarvoor, en ongeveer vier minuten alleen: de suite is één proces van 4454 checks en loopt onder de belasting van de andere
gates langer). Deel 3 is naar wens van de eigenaar vervallen: geen breed differentieel als poot, geen sectie in
`spec/ASP-TRANSLATION.md`. Een tweede lezer (Google Neon, alleen lezen) noemde drie fouten in deel 1 en 2: twee zijn in de IR
uitgesloten (WF-069, WF-077) en een weigert deze backend al; de vermoedens zijn niet bevestigd.

Openstaand voor de eigenaar: (a) in een **gewoon** document mengt een binder onder een operator in het filter van een vouw de
vormonderwerpen zonder melding (open punt 1 van het Result van deel 3), en een Let in een filter met een dieper filter dat de
binder met `=` of `≠` leest geeft een ander bedrag (open punt 2); geen golden heeft de vorm. (b) De deponering in de matrix van
een kwantorvouw over een bereik met gemaakte leden ontbreekt, zoals in elk document. (c) Gate 6 kan de suite naast de
corpusreplays laten lopen (nu na elkaar).
