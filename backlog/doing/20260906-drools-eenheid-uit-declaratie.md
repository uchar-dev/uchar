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

### De ontwerpkeuze: falen, luid, op bouwtijd

Gekozen is **falen** (de CLI's `ERROR`, exit 3), niet weigeren en niet markeren.
Drie redenen:

1. **Het register van ref.** Ref verzoent op precies deze plekken — additieve
   join, vergelijking, schrijfgrens, tijdlijnherschaling — via één
   `restate_into`, en faalt als het paar niet converteert. Clingo faalt vóór
   vertaling. Een derde been dat hetzelfde document met een getal beantwoordt,
   maakt het differentiële vierkant onleesbaar.
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
  `WaardeFn.herschaald` (die laatste op bouwtijd, zodat de melding uit de
  KieBase-bouw komt en niet uit een vurende consequentie);
- DRL-tekst: `herschaalExact`, `omrekenFactor`, `vergelijkingsOperanden`,
  `herschaalConditie`.

De overige herschaalplekken (`schaalTijd`, `schaalbaarNaarDoel`,
`tijdreeksSomAlleSet`, `grensOnveilig`, `parityOnveilig`) weigeren al met een
marker/skip; die zijn ongemoeid gelaten.

### Waarom gate 13's 74 twists hier niet op stuk lopen

De grens kijkt naar de twee **afgeleide** eenheden, nooit naar het verschil
tussen annotatie en declaratie. Een gate-13-twist liegt in de annotatie en laat
de declaratie staan; de afgeleide eenheden bewegen dan niet, dus er is niets te
verzoenen en er valt niets om. Een grens die op *tegenspraak* had gevuurd, had
elke twist van die gate geweigerd. Dat is expliciet geasserteerd in de
getuige-test (`een_liegende_annotatie_boven_een_kloppende_declaratie_verandert_niets`:
zelfde positie, zelfde vreemde eenheid, byte-identieke DRL-tekst).

### Echte vondst onderweg: de §7.1 afgetopte noemer

Bij de eerste, strengere versie van de grens vielen **twee ongemuteerde
corpusgoldens** om — een echte vondst, met meting:

```
jutterswet-tempototaal [exec] ERROR Eenheidsconflict: 'EUR/mnd' (canoniek EUR)
                                    herschaalt niet naar 'EUR' (canoniek EUR)
jutterswet-tempovouw   [exec] idem (en beide ook in de DRL-tak)
```

Oorzaak: bij een §7.1 TOTAAL over een `EUR/mnd`-attribuut naar een kaal
`EUR`-doel verbruikt de sommatie over de maand-granulariteit de noemer al. Ref
leidt de uitkomsteenheid daarom af als de *teller* en komt nooit bij een
herschaling. Deze backend leidt haar af als operand 0's eenheid (§4.6
*Eenheid1*, wat `EffectieveEenheid` implementeert) en houdt het tarief dus nog
vast als de schrijfgrens erom vraagt — waarna de stille identiteit toevallig het
juiste getal opleverde.

De afleiding echt repareren vergt onderscheid tussen een **tijdlijn**-totaal en
een **verzameling**-totaal, en die twee zijn in de expressie alleen niet uit
elkaar te houden: dezelfde golden draagt een verzamelings-TOTAAL van hetzelfde
`EUR/mnd`-attribuut naar een `EUR/mnd`-doel, en beide vormen komen voor met
`set` waar én onwaar. Die context zit in de transpiler, niet in de
expressiewandelaar. Daarom is de vorm **uitgezonderd en hardop opgeschreven**
(`Eenheidsconflict.afgetopteNoemer`, met de meting en beide goldens bij naam) in
plaats van beantwoord met een fout die het corpus tegenspreekt. Dit is een
kandidaat voor een eigen kaart.

De uitzondering is smal: ze eist converteerbare tellers, dus `EUR/mnd` tegen
`kg` faalt gewoon. Geasserteerd in beide richtingen.

### De drie benoemde uitzonderingen

1. een **eenheidloze zijde** — ch. 2.6 laat die de andere overnemen; ref ook;
2. een **opaak product** (`€·kg`) — ch. 2.10 legt de behoudende no-op van deze
   backend expliciet vast ("no restatement site will scale it by an invented
   factor"); erop falen zou die vastgelegde zin onwaar maken;
3. de **§7.1 afgetopte noemer** hierboven.

### Getuige-test (additief, 9 tests)

`EenheidsconflictWitnessTest` — de spiegel van
`scripts/check-reftype-derivation.sh`, één richting verder: die verdraait een
annotatie en houdt declaraties byte-identiek; deze verdraait een **declaratie**
en houdt de annotaties byte-identiek. Ze bouwt de mutant uit de dan-geldende
goldens (wandelt het corpus, vindt de additieve join tussen twee
eenheid-dragende attribuutdeclaraties) en eist dat **beide** takken falen; een
leeg probeersel is zelf de fout, per tak apart geteld.

Rood-eerst bewezen: met de grens tijdelijk uitgeschakeld gaat ze rood op
3 van de 9 tests en noemt beide goldens in beide takken —
`jutterswet-schouwrooster (exec-model)`, `(DRL-tekst)`, `synth-grensrand
(exec-model)`, `(DRL-tekst)`: "het document lowerde zonder melding".

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
| 12 drools | PASS — played **117**, kanaalvloer **47**, auto-excluded **44**, value-divergence **1**, no-case **2**, JUnit **232** | alleen JUnit 223 → 232 |
| 13 reftype-derivation | PASS — **74** twists over **15** goldens, **13** DRL-gespeeld, **0** survivors | = |

Geen enkele pin aangepast; de diff is puur additief (412 regels erbij, 0 eraf)
en raakt alleen `backends/drools/**` en `backlog/`.

### Bewust niet gedaan

- **De afleiding van een §7.1 TOTAAL over een tarief** — zie hierboven; vergt
  transpilercontext en verdient een eigen kaart.
- **De vier ch. 2.10-annotatielezingen** (WF-035 duur-operand, WF-038
  tijdsduur): dat zijn de benoemde posities waar de annotatie de bron *is*.
- **De verifier, ref, clingo** en elke uitbreiding van gate 13 — out of scope
  per de kaart.
- **De reeds weigerende herschaalplekken** (marker/skip): die zijn al luid.
