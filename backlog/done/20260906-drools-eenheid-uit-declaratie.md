# De Drools-backend leest de eenheid van de annotatie, niet van de declaratie

Goal: de Drools-backend de **effectieve-typewet** laten volgen op de
attribuutlees-positie — de eenheid van een `AttribuutRef` komt uit de
**declaratie** waar hij uit leest, niet uit de `type`-annotatie die naast hem
op de wire staat (`spec/RUNTIME-SEMANTICS.md` §2.10). Vandaag speelt de
backend een document dat de verifier, de referentie-interpreter én de
ASP-backend alle drie weigeren, en levert het de "juiste" getallen — omdat hij
de mismatch nooit ziet.

## De meting (2026-09-06, pre-existent — deze kaart introduceert niets)

Startpunt is de ongewijzigde golden `corpus/golden/jutterswet-schouwrooster`.
Daarin telt `schouwplicht = vaste schouwlast + extra schouwlast`, beide
gedeclareerd met `eenheid schouwdagen`; de `AttribuutRef`s in de berekening
dragen dezelfde eenheid als `type`-annotatie.

**De mutant:** alleen de **declaratie** van één attribuut wordt verdraaid
(`objecttypen.Strandmeester.attributen["extra schouwlast"].eenheid`), de
annotaties op de leesposities blijven onaangeraakt. Twee sterktes gemeten,
allebei dezelfde uitkomst:

| mutant | verifier | ref | clingo | Drools |
|---|---|---|---|---|
| declaratie → `dagen` (factor 1) | **WF-091 FAIL** | **FAULT** (case1) | **FAULT** (beide) | **PLAYED 2** |
| declaratie → `uur` (factor 3600 naar `seconde`) | **WF-091 FAIL** | **FAULT** (case1) | **FAULT** (beide) | **PLAYED 2** |

Letterlijk:

- verifier: `WF-091 FAIL /resolvedRegels/0/waarde/rechts/type: unit mismatch:
  attribute reference 'extra schouwlast' is typed 'schouwdagen' (factor 1 to
  'schouwdagen'), but the declaration it reads from is 'uur' (factor 3600 to
  'seconde')`
- ref: `EVALUATION FAULT: incompatible units: schouwdagen vs uur`
- clingo: `TRANSLATION FAULT: non-identical units under an additive join are
  not translated`
- Drools: `PLAYED 2` — beide cases spelen, met **exact** de gepinde
  verwachtingen van de ongemuteerde golden (15 / 24 / 20 en 9).

Dat de tweede mutant — een eenheid uit een heel ander stelsel, met factor 3600
— dezelfde getallen oplevert als de eerste, is het beslissende punt: de
backend consulteert de declaratie op deze positie **niet**, ook niet om te
converteren. Hij neemt de wire-annotatie. Ref bewijst het spiegelbeeld: die
noemt in zijn foutmelding juist beide kanten (`schouwdagen vs uur`), dus ref
leest de declaratie wél.

Waarom `ref case2` in beide rijen PASS geeft: in `case2` is de tweede operand
leeg, dus de optelling zet nooit twee gevulde eenheden naast elkaar en de
vergelijking wordt nooit bereikt. Dat is de coalescing-helft van `plus`
(`RUNTIME-SEMANTICS.md` ch. 1.2), geen zwakte in de meting — clingo weigert
ook die case, omdat hij op vertaaltijd oordeelt.

## Waarom geen bestaande gate dit ziet

- **Gate 3** (verifier) draait niet over mutanten; hij ziet alleen de
  welgevormde corpusdocumenten, en die zijn WF-091-schoon.
- **Gate 12** (Drools) speelt alleen die welgevormde documenten. Een document
  dat de verifier afwijst komt er nooit langs.
- **Gate 13** (annotatie-onverschilligheid) kan het **per constructie** niet
  zien: die houdt de declaraties byte-identiek en verdraait uitsluitend
  `type`-annotaties, op documenten die welgevormd blijven. Precies de omgekeerde
  richting van dit lek. Een gate-13-twist en deze mutant zijn elkaars spiegel:
  de eerste vraagt "reproduceert de consument de uitkomst als de annotatie
  liegt?", de tweede "merkt de consument het als de annotatie en de declaratie
  elkaar tegenspreken?".

## Wat deze kaart moet opleveren

- De Drools-backend leidt de eenheid van een attribuutlees-positie af uit de
  declaratie (de effectieve-typewet, `RUNTIME-SEMANTICS.md` §2.10), zoals ref
  dat doet — in **beide** transpilerbranches (de exec-model-tak die gate 12
  speelt en de klassieke DRL-tekst-tak).
- Een backend-eigen witness-test (JUnit, additief) die de mutant hierboven
  vastlegt: een document met een tegensprekende declaratie/annotatie MOET niet
  stilzwijgend de "juiste" waarde opleveren. Of de backend dan faalt, weigert,
  of markert is de ontwerpkeuze van deze kaart — **maar niet: stil doorspelen**.
  Schrijf de keuze op.
- Gate 12 blijft groen met ongewijzigde speelvloer en ongewijzigde
  divergentielijst: de corpusdocumenten zijn WF-091-schoon, dus een correcte
  afleiding mag daar niets bewegen. Beweegt er wél iets, dan is dat de echte
  vondst en hoort ze in de kaart.
- Gate 13 blijft groen met ongewijzigde tellingen.

## Acceptance

- De twee mutanten hierboven, opnieuw gebouwd uit de dan-geldende golden,
  worden door de Drools-backend **niet** stil gespeeld; het rapport noemt de
  reden.
- De backend-JUnit-suite groen met een hoger, niet-nul testaantal en nul
  skips.
- Alle dertien gates groen, met de tellingen genoteerd.

May touch: `backends/drools/**`, `backlog/`.

## Out of scope

- **De verifier.** WF-091 doet zijn werk: hij wijst beide mutanten af met de
  positie en beide eenheden erbij. Er is hier niets aan de regel te repareren.
- Ref en clingo: die weigeren al, elk in hun eigen register.
- Elke uitbreiding van gate 13. De blinde vlek hierboven is een *eigenschap*
  van die gate (declaraties byte-identiek houden is haar hele opzet), geen
  fout erin. Of er een aparte harnas moet komen die de omgekeerde richting
  verdraait, is een eigen vraag en een eigen kaart.

---

## Result (2026-09-09)

### De diagnose van de kaart klopte in symptoom, niet in oorzaak

De kaart schreef het stille doorspelen toe aan een *wire-lees* op de
attribuutlees-positie. Dat is sinds 2026-09-04 niet meer waar: de
eenheidsafleiding uit de declaratie (`EffectieveEenheid`) stond er al, en beide
takken lopen er volledig doorheen. Er resteren precies vier rechtstreekse
annotatie-lezingen, alle vier de ch. 2.10-uitzonderingen met naam (twee maal de
WF-035 duur-operand, twee maal de WF-038 tijdsduur).

Beslissende hermeting, een **derde** mutant op de actuele golden: de declaratie
van `extra schouwlast` naar een *converteerbare* zusterheenheid (`halve
schouwdagen`, canoniek `schouwdagen`, factor 0,5). Als de backend de annotatie
las, was er niets gebeurd; hij gaf:

```
FAIL case1.json: attribuut schouwplicht: verwacht 15, werkelijk 13.5;
                 attribuut schouwplicht: verwacht 24, werkelijk 21.0
```

De declaratie wérd dus geraadpleegd en de herschaling wérd uitgevoerd.

De echte oorzaak zit één stap later. Elke herschaalhelper in beide takken
bewaakte met één samengestelde test:

```java
if (ev == null || en == null || !ev.convertibleNaar(en) || !ev.vereistOmrekening(en)) return identiteit;
```

Die gooit twee verschillende feiten op één hoop — *"er is geen omrekening
nodig"* (zelfde eenheid, of een eenheidloze zijde) en *"er BESTAAT geen
omrekening"* (twee eenheden met losse canonieken) — en beantwoordt ze allebei
met de identiteit. Een document waarvan de declaraties de optelling onmogelijk
maken leverde daardoor exact de getallen van een kloppend document.

### Rood-eerst, hermeten op de actuele golden

Mutant = alléén `objecttypen.Strandmeester.attributen["extra schouwlast"].eenheid`
verdraaid; elke annotatie onaangeraakt (geverifieerd met een structurele diff:
drie velden bewegen, alle drie in die declaratie).

| mutant | verifier | ref | clingo | Drools VÓÓR | Drools NA |
|---|---|---|---|---|---|
| `dagen` (factor 1) | WF-091 FAIL | FAULT (case1) | TRANSLATION FAULT | **PLAYED 2** | **ERROR** |
| `uur` (3600 → seconde) | WF-091 FAIL | FAULT (case1) | TRANSLATION FAULT | **PLAYED 2** | **ERROR** |

Letterlijk, ná de wijziging, beide takken:

```
exec-model: ERROR Eenheidsconflict: onverenigbare eenheden bij een §4-waardeherschaling:
            'uur' (canoniek seconde) herschaalt niet naar 'schouwdagen' (canoniek schouwdagen)
            — er bestaat geen omrekenfactor
DRL-tekst:  ERROR Eenheidsconflict: onverenigbare eenheden bij een §4-herschaling (DRL):
            'uur' (canoniek seconde) herschaalt niet naar 'schouwdagen' (canoniek schouwdagen)
            — er bestaat geen omrekenfactor
```

De ongemuteerde golden speelt onveranderd `PLAYED 2` in béide takken.

### De ontwerpkeuze: falen, luid

Gekozen is **falen** (de CLI's `ERROR`, exit 3), niet weigeren en niet markeren.
Drie redenen:

1. **Het register van ref.** Ref stuurt elke verzoening door één
   `restate_into`, aangeroepen vanaf **zeven** plekken: de drie additieve
   joins (`add`, `sub`, `sub_min`), een grens, de vergelijking, de schrijfgrens
   en de tijdlijnherschaling. Hij faalt als het paar niet converteert. Clingo
   faalt vóór vertaling. Een derde been dat hetzelfde document met een getal
   beantwoordt, maakt het differentiële vierkant onleesbaar.
2. **Weigeren zou het verkeerde kanaal zijn.** Gate 12's kanaal 1 betekent "een
   *vorm* die de v0-backend niet draagt". Een onverenigbare optelling is geen
   niet-gedragen vorm — de backend lowert `PLUS` over twee attribuutlezingen
   prima — maar een kapot document. Ze in kanaal 1 stoppen verwatert wat dat
   kanaal beweert, en laat gate 12 groen blijven op een corpusdocument dat stuk
   is.
3. **`ERROR` kan door geen enkel gate-kanaal worden opgevangen**: gate 12 laat
   een ERROR altijd falen. Een corpusdocument dat ooit zo'n paar krijgt, valt
   dus met naam om in plaats van stil te worden meegeteld.

De grens staat aan de *kop* van elke herschaalhelper, vóór diens eigen
"geen omrekening nodig"-test, zodat de twee feiten gescheiden blijven:

- exec-model: `valueFactor`, `exactHerschaalFactoren`, `restatement`,
  `WaardeFn.herschaald`;
- DRL-tekst: `herschaalExact`, `omrekenFactor`, `vergelijkingsOperanden`,
  `herschaalConditie`.

**Waar de melding vandaan komt.** De meeste plekken vragen het op *bouwtijd*,
waar het paar al bekend is, en dan faalt de KieBase-bouw. Twee doen dat niet:
`valueFactor` en `exactHerschaalFactoren` worden óók vanuit een vurende
consequentie bereikt, dus een document waarvan het enige onverenigbare paar op
dat pad ligt faalt tijdens het vuren en de CLI meldt het verpakt in een
`ConsequenceException`. Dat is geen zwakkere uitkomst en de keuze is niet
bouwtijd-of-niets: voor de gate is een `ERROR` uit een consequentie precies zo
hard als één uit de bouw — beide zijn exit 3, die gate 12 niet kan wegsorteren.
Bouwtijd verdient de voorkeur omdat de melding dan onverpakt aankomt en de plek
noemt. Elk hek draagt daarom zijn **eigen** plek-tekst, zodat het rapport zegt
wélke vuurde in plaats van dat er drie één regel delen.

De overige herschaalplekken zijn ongemoeid gelaten, met twee verschillende
redenen: `schaalTijd`, `schaalbaarNaarDoel` en `tijdreeksSomAlleSet` weigeren
zelf al met een marker/skip. `grensOnveilig` en `parityOnveilig` doen dat
**niet** — ze vragen `vereistOmrekening`, en dat is voor een onverenigbaar paar
`onwaar`, dus hun antwoord is "veilig" en de constructie vuurt gewoon. Wat ze
onaangetast laat is dat de eigenlijke herschaling van diezelfde grens
stroomafwaarts wél door een hek gaat (`herschaald` / `omrekenWaarde`), waar het
paar alsnog omvalt. Ze zijn dus geen tweede weigerplek maar een predicaat dat
het conflict niet ziet; het hek erachter ziet het wel.

### Waarom gate 13's 74 twists hier niet op stuk lopen

De grens kijkt naar de twee **afgeleide** eenheden, nooit naar het verschil
tussen annotatie en declaratie. Een gate-13-twist liegt in de annotatie en laat
de declaratie staan; de afgeleide eenheden bewegen dan niet, dus er is niets te
verzoenen en er valt niets om. Een grens die op *tegenspraak* had gevuurd, had
elke twist van die gate geweigerd. Dat is expliciet geasserteerd in de
getuige-test (`een_liegende_annotatie_boven_een_kloppende_declaratie_verandert_niets`:
zelfde positie, zelfde vreemde eenheid, byte-identieke DRL-tekst).

### Echte vondst onderweg: de §7.1 tijdvouwing was een afleidingsfout

Bij de eerste, strengere versie van de grens vielen **twee ongemuteerde
corpusgoldens** om — een echte vondst, met meting:

```
jutterswet-tempototaal [exec] ERROR Eenheidsconflict: onverenigbare eenheden bij een
    §7.1 tijdlijn-TOTAAL naar het doel: 'EUR/mnd' (canoniek EUR) herschaalt niet naar
    'EUR' (canoniek EUR) — er bestaat geen omrekenfactor
jutterswet-tempovouw   [exec] ERROR ConsequenceException: … Eenheidsconflict: onverenigbare
    eenheden bij een §4-waardeherschaling …
```
(en beide ook in de DRL-tak).

Oorzaak, en het is een **afleidingsfout, geen uitzonderingsgeval**: bij een §7.1
TOTAAL over een tijdlijn-gedeclareerd attribuut vouwt eerst de tijdlijn van dat
attribuut zelf, op zijn eigen granulariteit, waarbij elk segment *waarde × het
hele aantal roostereenheden van zijn spanne* bijdraagt (§4.20 Tabel 30). Een
`EUR/mnd` wordt daar dus vermenigvuldigd met een **aantal maanden**, en wat
overblijft is de teller: `EUR`. De noemer wordt vóór de vouwing werkelijk
verbruikt. Ref leidt om die reden de teller af en komt nooit bij een
herschaling; deze backend leidde operand 0's eenheid ongewijzigd af (de kale
§4.6 *Eenheid1*-regel) en hield het tarief dus nog vast als de schrijfgrens
erom vroeg — waarna de stille identiteit toevallig het juiste getal opleverde.

De reparatie zit daarom in `EffectieveEenheid.naTijdvouwing`: een `TOTAAL`
waarvan operand 0 een tijdlijn-gedeclareerd attribuut is, levert de **teller**.
Beide takken profiteren zonder één aanroepplek te wijzigen, de DRL-tekst blijft
byte-identiek (identiteit vóór én ná), en het paar bereikt het hek niet meer.

De reikwijdte is **gemeten, niet geraden**: over het hele corpus zijn álle folds
met een tijdlijn-gedeclareerde operand `TOTAAL` (19, waarvan 4 een tarief
dragen — de twee goldens plus twee `corpus/negatief`-documenten die de verifier
sowieso afwijst). Een `MINIMUM` of `EERSTE` over een tijdlijn kiest een waarde
in plaats van te sommeren, verbruikt dus geen noemer en mag er geen laten
vallen; dat is apart geasserteerd. Wat bewust **niet** gecontroleerd wordt: dat
de noemer de eenheid van het rooster zélf is (`€/kg` op een MAAND-tijdlijn). Ref
controleert dat wel; hier zou het een granulariteit→eenheid-afbeelding vergen
waarvoor deze klasse geen eerlijke bron heeft, en die uit afkortingen raden is
precies de naamtest waar dit werk al één keer door gebeten is. De vorm ontbreekt
in het corpus (alle vier de tariefgevallen zijn `€/mnd` op MAAND) en het is een
vraag over de *waarde* van de vouwing, die de verifier en ref bezitten, niet
over een herschaling.

### De twee benoemde uitzonderingen

1. een **eenheidloze zijde** — ch. 2.6 laat die de andere overnemen; ref ook;
2. een **opaak product** (`€·kg`) — ch. 2.10 legt de behoudende no-op van deze
   backend expliciet vast ("no restatement site will scale it by an invented
   factor"); erop falen zou die vastgelegde zin onwaar maken.

### Fixronde na lens 1 — twee restgaten gesloten

Lens 1 reproduceerde alle claims en dertien gates, en vond twee gaten van
precies de soort die deze kaart sluit. Beide zijn nu dicht; hieronder vóór/ná
met echte output (mutanten uit `/tmp/lens-eenheid1-work/mut/`).

**Gat 1 — de §7.1-uitzondering was plaats-blind.** De eerste versie hing de
vrijstelling aan het *eenhedenpaar* (tarief tegen kale eenheid met
converteerbare tellers) en niet aan de plek die haar motiveerde, dus ze gold op
alle acht hekposities. Gemeten: `I` (declaratie-noemer op een additieve join),
`L` (schrijfgrens), `M` (§5.1-vergelijking) en `K` (schone tariefvariant) gaven
ref `EVALUATION FAULT` en clingo `TRANSLATION FAULT`, maar Drools `PLAYED 2` in
beide takken. Ná de reparatie — de afleiding hierboven vervangt de uitzondering,
dus het hek is weer streng — noemt elke mutant zijn eigen positie:

```
I  exec: ERROR Eenheidsconflict: … bij een additieve operandgrens:
        'schouwdagen/mnd' (canoniek schouwdagen) herschaalt niet naar 'schouwdagen' …
L  exec: ERROR Eenheidsconflict: … bij een §4 schrijfgrens naar het doel:
        'schouwdagen' herschaalt niet naar 'schouwdagen/mnd' …
M  exec: ERROR Eenheidsconflict: … bij een §5.1 vergelijking:
        'schouwdagen' herschaalt niet naar 'schouwdagen/mnd' …
```
(alle drie ook in de DRL-tak, met hun eigen DRL-plekteksten).

**Gat 2 — de opaak-productvrijstelling was naam-gebaseerd.** Ze testte op
`afkorting().contains("·")` plus lege canoniek/factor. Een op de wire
gedeclareerde eenheid die toevallig zo heet kreeg de vrijstelling zonder ooit
door `Eenheid.product` gebouwd te zijn: mutant `H` (`appel·peer` als declaratie)
en `J` (dezelfde eenheid, welgevormd geannoteerd) gaven ref FAULT en Drools
`PLAYED 2`.

De reparatie is **structureel**: de vrijstelling hangt nu aan de *herkomst*, via
een identiteitstabel van elk opaak product dat deze afleiding zelf bouwde
(`EffectieveEenheid.isAfgeleidProduct`). Waarde-gelijkheid helpt niet — de wire
kan exact dezelfde vier velden dragen — dus alleen identiteit scheidt de twee.
Een markerveld op `Eenheid` zelf was de kleinere codering, maar dat record ligt
in de frontend-module en valt buiten het may-touch van deze kaart; de tabel is
dezelfde uitspraak, gedaan vanaf de enige plek in de backend die zo'n product
bouwt. Ze groeit met hooguit één verwijzing per MAAL-knoop met twee
eenheid-dragende operanden per model — over het hele corpus nul. Ná:

```
H  exec: ERROR Eenheidsconflict: … bij een additieve operandgrens:
        'appel·peer' (canoniek null) herschaalt niet naar 'schouwdagen' …
J  exec: idem
```
Geasserteerd in de getuige-test met een **waarde-gelijk** paar: het afgeleide
product blijft vrijgesteld, de wire-naamgenoot valt om.

**Controle op de andere kant.** Mutant `O` (de gate-13-twist: alleen de
annotatie liegt, de declaratie klopt) speelt onveranderd `PLAYED 2` in beide
takken, en de ongemuteerde golden `PLAYED 2`. De twee §7.1-goldens spelen weer
`PLAYED 1` in beide takken. Corpusbrede scan: **0** goldens raken het hek.

### Slotronde na lens 2 — de tijdvouwing dekt ook SOM

Lens 2 oordeelde mergen (gates groen op de kop én na een proefmerge met `main`;
DRL corpusbreed byte-identiek 130/130; alle mutanten reproduceren) en vond één
gedragsfout van één token, plus twee documentatiepunten.

**De afleiding was tot `TOTAAL` beperkt, en dat is te smal.** `SOM` draagt
dezelfde per-element tijdlijnvouwing, en beide consumenten zeggen dat met zoveel
woorden: de instance-fold-guard van `ExecModelTranspiler` laat `SOM` én `TOTAAL`
toe en slaat een temporele operand voor élke andere soort over; `ref`'s
`eval.ml` faalt op de andere met "only som and totaal carry the per-element
timeline fold". De reikwijdte hoorde dus van de consumenten te komen, niet van
het corpus — en het corpus kón haar ook niet leveren, want het draagt geen
enkele `SOM` over een tijdlijn-gedeclareerd attribuut (alle 19 zulke folds zijn
`TOTAAL`).

Gemeten gevolg van de te smalle regel, en de reparatie:

| mutant | ref | Drools vóór | Drools ná |
|---|---|---|---|
| `P1` SOM, tijdlijn-tarief, doel `EUR` | MISMATCH `J2.bewaarlast = leeg, expected 0` | `ERROR Eenheidsconflict` | **FAIL met exact dezelfde ene J2-afwijking** |
| `P2` SOM, tijdlijn-tarief, doel `EUR/jr` | `EVALUATION FAULT: incompatible units: EUR/jr vs EUR` | `FAIL … verwacht 48, werkelijk 576` | **`ERROR Eenheidsconflict`** |
| `P3` TOTAAL, tijdlijn-tarief, doel `EUR/jr` | idem | `ERROR Eenheidsconflict` | ongewijzigd luid |

`P2` is het punt: 576 is 48 × 12, de `mnd→jr`-factor toegepast op een waarde die
de vouwing al had opgeleverd. Dat was een **stil verkeerd getal** op precies het
faalpatroon dat deze kaart sluit. Ná de uitbreiding faalt hij luid, en `P1`
speelt met dezelfde afwijking die ref en clingo ook geven — dus de backend volgt
ref weer.

**De groeigrens van de herkomsttabel stond fout opgeschreven.** Ik claimde "ten
hoogste één entry per MAAL-knoop per getranspileerd model". Gemeten is dat
onwaar: `Eenheid.product` newt élke keer, dus drie transpilaties van hetzelfde
document in één JVM geven 0 → 2 → 4 → 6. De werkelijke grens is **één entry per
`maalEenheid`-evaluatie**, monotoon voor de JVM-levensduur, nooit geschoond —
een sterk-referentielek, nu zo opgeschreven en met een getuige gepind
(`de_herkomsttabel_groeit_per_afleiding_niet_per_document`). Correctheid staat
er los van: geen enkele oude entry kan een nieuwe eenheid markeren, want er
wordt nooit een bestaande instantie teruggegeven. Per transpilatie legen is
overwogen en afgewezen: dat vergt een garantie dat geen andere afleiding leeft,
en die geeft de bouwroute niet — beide transpilertakken bouwen hun eigen
`EffectieveEenheid`, dus een clear-bij-constructie zou een product ontmarkeren
dat de andere tak nog vasthoudt. Dat ruilt een begrensd lek voor een verkeerd
antwoord. Een echte begrenzing wil een zwakke *identiteits*-set, die de JDK niet
kent; dat is een kaartje waard, geen improvisatie hier.

**Product-tegen-product.** De vrijstelling wordt door één zijde gedragen, dus
twee **verschillende** afgeleide producten stellen elkaar ook vrij (`€·kg` tegen
`€·m`), niet alleen een product tegen een concrete eenheid. Dat is dezelfde
ch. 2.10-houding en geen bredere — geen van beide zijden heeft een canoniek, dus
er valt hoe dan ook geen factor te verzinnen. Wat het kost is een *diagnose*,
geen verkeerd getal. Bijzin toegevoegd aan de klassendoc en gepind.

### De vondst die de kaart sterker maakt: een verifier-SCHONE vorm

Mutant `N` is het interessantste geval dat lens 1 bouwde: een `Begrensd` over
twee `EUR`-attributen met een `Literal`-ondergrens geannoteerd in `kg`.

| consument | uitkomst |
|---|---|
| **verifier** | **PASS** — geen WF-regel omheint deze positie |
| ref | `EVALUATION FAULT: incompatible units: EUR vs kg` |
| clingo | `TRANSLATION FAULT: non-identical units under a Begrensd bound are not translated` |
| Drools **vóór** het hek | `PLAYED` |
| Drools **ná** het hek | `ERROR Eenheidsconflict: … bij een §6.1.4 begrenzing-ondergrens: 'kg' … herschaalt niet naar 'EUR' …` |

De grens is een `Literal`, en dat is juist één van de drie
ch. 2.10-posities waar de annotatie de **bron** is — er valt dus niets te
kruisen tegen een declaratie, en WF-091 kan hier niet redden. De andere drie
posities zijn wél omheind (`WF-035` additieve join, `WF-036`
vergelijkingsoperanden, `WF-023` toekenning); deze valt ertussen. Het hek sluit
daarmee niet alleen documenten die de verifier al afwijst, maar ook één die hij
doorlaat. Of de verifier die omheining hoort te krijgen is een spec-vraag op
beschermde paden en staat als kandidaat-kaart klaar:
`backlog/new/20260909-begrensd-grens-verifier-omheining.md`.

### Getuige-test (additief, 14 tests)

`EenheidsconflictWitnessTest` — de spiegel van
`scripts/check-reftype-derivation.sh`, één richting verder: die verdraait een
annotatie en houdt declaraties byte-identiek; deze verdraait een **declaratie**
en houdt de annotaties byte-identiek. Ze bouwt de mutanten uit de dan-geldende
goldens (wandelt het corpus, vindt de vorm) en eist dat **beide** takken falen;
een verdraaiing die geen enkele golden vindt is zelf de fout, per tak apart
geteld.

**Vier declaratieverdraaiingen, één per hekpositie** — de laatste drie zijn de
tarief-tegen-eigen-teller-vorm, en die zijn per positie gepind juist omdat het
gat een plaats-blindheid was en een pin op één positie de andere twee niet zou
hebben gezien:

| verdraaiing | positie |
|---|---|
| additieve operand → vreemde eenheid | additieve operandgrens |
| additieve operand → tarief met dezelfde teller | additieve operandgrens |
| schrijfgrens → doel als tarief | §4 schrijfgrens |
| §5.1 vergelijking → parameter als tarief | §5.1 vergelijking |

Daarnaast: het afgeleide opake product blijft vrijgesteld terwijl zijn
**waarde-gelijke** wire-naamgenoot omvalt; twee verschillende afgeleide
producten stellen elkaar vrij; de herkomsttabel groeit per afleiding en niet per
document; de §7.1-tijdvouwing levert de teller voor `TOTAAL` én `SOM` en niet
voor een `MINIMUM` over dezelfde tijdlijn; en de omgekeerde richting (liegende
annotatie boven kloppende declaratie) laat de DRL-tekst byte-identiek.

Rood-eerst bewezen: met de grens tijdelijk uitgeschakeld ging de eerste versie
rood op 3 van de 9 tests en noemde beide goldens in beide takken —
`jutterswet-schouwrooster (exec-model)`, `(DRL-tekst)`, `synth-grensrand
(exec-model)`, `(DRL-tekst)`: "het document lowerde zonder melding". De drie
tarief-verdraaiingen zijn rood-eerst aangetoond met de mutantmeting hierboven
(vóór: `PLAYED 2`; ná: `ERROR`, elk met zijn eigen positie).

### Gates — alle dertien groen

| gate | uitkomst | t.o.v. start |
|---|---|---|
| 1 self-contained | PASS | = |
| 2 ir-schema | PASS (187 bestanden) | = |
| 3 verifier | PASS (187 golden schoon; 337 negatief) | = |
| 4 case-schema | PASS (442 cases) | = |
| 5 oracle | PASS (419 cases) | = |
| 6 clingo | PASS (419 cases) | = |
| 7 noop | PASS (383 cases; 36 survivors) | = |
| 8 rood | PASS (23 rode cases; Drools 6 fault-pinned / 17 niet-gedragen) | = |
| 9 catala | PASS (201 checks; 12 goldens) | = |
| 10 regelspraak | PASS (173 tests; 37 rgs-goldens) | = |
| 11 toka-parity | PASS (45 .rgs; 27 tests; 29 cases) | = |
| 12 drools | PASS — played **117**, kanaalvloer **47**, auto-excluded **44**, value-divergence **1**, no-case **2**, JUnit **237** | alleen JUnit 223 → 237 |
| 13 reftype-derivation | PASS — **74** twists over **15** goldens, **13** DRL-gespeeld, **0** survivors | = |

Geen enkele pin aangepast; de wijziging raakt alleen `backends/drools/**` en
`backlog/`.

**Rebase-notitie.** Bovenstaande tellingen zijn gemeten op de branchkop, afgetakt
van `ee45dca`. `main` is intussen doorgelopen; lens 2 heeft de proefmerge al
groen gemeten. Ná de merge gelden **425 cases** en **played 118** — de merge
brengt een golden mee die speelt. De rebase en de hermeting van deze tabel horen
bij de merge, niet bij deze kaart.

### Bewust niet gedaan

- **De WF-omheining van de `Begrensd`-grens** (mutant N): een spec-vraag op
  beschermde paden, weggeschreven als
  `backlog/new/20260909-begrensd-grens-verifier-omheining.md`.
- **Een echte begrenzing van de herkomsttabel** (een zwakke identiteits-set):
  zie de slotronde hierboven; het lek is begrensd, gemeten en opgeschreven.
- **De controle dat de noemer de roostereenheid is** bij de §7.1-tijdvouwing —
  zie de reikwijdte-alinea hierboven; vergt een granulariteit→eenheid-afbeelding
  zonder eerlijke bron, en de vorm ontbreekt in het corpus.
- **De vier ch. 2.10-annotatielezingen** (WF-035 duur-operand, WF-038
  tijdsduur): dat zijn de benoemde posities waar de annotatie de bron *is*.
- **De verifier, ref, clingo** en elke uitbreiding van gate 13 — out of scope
  per de kaart.
- **`grensOnveilig` / `parityOnveilig`**: predicaten die het conflict niet zien
  (zie hierboven); het hek stroomafwaarts ziet het wel, dus ze zijn niet
  aangeraakt.
