# De DRL-tekst-tak zinkt op een heelperiode-guard van een feitcreatie

Goal: de package-compile-fout waarmee de DRL-tekst-tak omvalt op een
§13.4.7-feitcreatie met een `gedurende het gehele <periode>`-guard, ofwel
gericht bedraden ofwel per construct weigeren met een benoemde reden — en in
beide gevallen vastleggen wélke documentvormen hem bereiken. Vandaag is het
geen benoemde weigering maar een ongelezen compilerfout die het HELE pakket
meeneemt.

## Wat er misgaat

`DrlTranspiler.feitCreatie` probeert de guard eerst op de ANKER te lowereren
en, als dat een marker oplevert, op het DOEL — waar het resultaat INLINE in het
`$doel`-patroon hangt. De heelperiode-lowering (`temporeelKenmerkTijdreeks` en
de `rsGeheelPeriode`-aanroep eromheen) bedraadt echter hard `$subject`. In het
`$doel`-patroon betekent `$subject` de anker, dus er komt een expressie te
staan die de anker-boon een veld vraagt dat zij niet heeft — of, erger, wel
heeft met een ander type. Drools kan hem niet analyseren en laat het hele
pakket zinken:

    EXCLUDE drl compile: Unable to Analyse Expression
    rsGeheelPeriode($subject.getGehesen(), "JAAR", 7, null)

Dat is geen per-vorm-weigering met een reden — zoals deze backend die overal
elders geeft — maar een pakketbrede compilerfout. Elke andere constructie in
hetzelfde document valt mee om.

## Wat er GEMETEN is (2026-09-19/20, kaart `eigenaar-gastheer-object`)

De trigger is scherp: de ANKER-poging moet markeren én de DOEL-poging moet een
heelperiode-lowering opleveren. Drie metingen bakenen dat af.

1. **De doel-gastheer met kwalificatie.** `gehesen` tijdsafhankelijk op `Sein`
   en niet op `Kustvak`, guard `gedurende het gehele jaar gehesen` met
   `eigenaar: "Sein"`. Gemeten vóór 2026-09-19: verifier PASS, `ref` PASS 1,
   clingo PASS 1, exec PLAYED 1, **drl-tekst de zink hierboven**. Sinds die
   kaart is deze vorm WF-109 FAIL (het derde reikwijdtehek), dus als
   verifier-schone route is hij dicht — de zink zelf niet.
2. **De heelperiode-VERGELIJKING over een vreemd `objecttype`.** Guard
   `de seinhoogte van het sein gedurende het gehele jaar groter is dan 0`, met
   `seinhoogte` tijdsafhankelijk op `Sein`. Een `Vergelijking` draagt geen
   `eigenaar`-veld, dus WF-109 ziet hem niet eens. Gemeten 2026-09-20:
   verifier **WF-045 FAIL** (cyclus) + **WF-053 FAIL** (de vreemde
   `objecttype` wordt als navigatie over de afgeleide relatie gelezen — de open
   spanning uit IR-SPEC ch. 4.6 (q2)), exec `ERROR
   IllegalArgumentException`, **drl-tekst zinkt opnieuw**:
   `Unable to Analyse Expression rsGeheelPeriode($subject.getSeinhoogte(), …)`.
   De verifier wijst het document af, maar de tak zinkt vóór en onafhankelijk
   van dat oordeel.
3. **De tegenmeting die de grens trekt.** Declareer `gehesen` óók
   tijdsafhankelijk op de ANKER en laat de gastheer weg: verifier PASS, `ref`
   PASS, exec PLAYED, **drl-tekst PLAYED**. De anker-poging lowert dan gewoon
   en de doel-poging komt er niet aan te pas. De zink zit dus niet in de
   kwalificatie als zodanig maar in het PAD anker-faalt→doel-probeert.

## Wat NOG gemeten moet worden

De nalens van 2026-09-20 meldt **vier verifier-schone routes** naar deze zink
(via WF-109-vormen, tweemaal via WF-043-vormen, en één via de
WF-053+WF-045-vorm). Die vier zijn hier **niet gereproduceerd**: van de twee
routes die deze kaart zelf heeft gemeten is de eerste sinds 2026-09-19
verifier-AFGEWEZEN (WF-109) en de tweede dat al (WF-045/WF-053), en de derde
meting speelt juist gewoon. Het kan zijn dat de nalens vóór het derde
reikwijdtehek mat, of dat er een familie is die deze kaart niet heeft geraakt.
**Uitzoeken hoort bij acceptatiepunt (a) en niet bij het geloof op voorhand.**

## Waarom dit geen corpusrisico is — vandaag

Geen enkele golden draagt de vorm, de EXCLUDE draagt een reden en gate 12
telt hem in het niet-gespeelde kanaal met naam. De schade is dus potentieel,
niet actueel. Maar een pakketbrede compilerfout is geen weigering: hij zegt
niet welk construct hem veroorzaakte, hij neemt alles mee wat naast hem stond,
en hij is precies het soort stille breuk waar de per-vorm-weigeringen van deze
backend voor bestaan.

## Acceptance (bij oppakken aanscherpen)

- (a) **Rood-eerst de routes vastleggen.** Per route een document plus de vijf
  uitkomsten (verifier, `ref`, clingo, exec, drl-tekst), en per route
  vastgesteld of hij verifier-SCHOON is. De drie metingen hierboven zijn de
  startpositie; de vier van de nalens moeten gereproduceerd of weerlegd worden.
- (b) **Kiezen en uitvoeren.** Óf de bedrading gericht maken —
  `temporeelKenmerkTijdreeks` krijgt de patroonvariabele mee in plaats van
  `$subject` hard te bedraden, zodat de doel-poging het doel leest — óf de
  doel-poging weigert een heelperiode-lowering per construct, met een reden in
  de trant van de bestaande `voorwaarde niet naar anker of doel te lowereren`.
  Een fix maakt de vorm speelbaar en vraagt om een golden; een weigering vraagt
  om een reden-dragende regel en laat gate 12 hem bij naam tellen.
- (c) **Het WF-109-hek opnieuw wegen.** Het derde reikwijdtehek van WF-109
  (geen heelperiode-kwalificatie op een doel-gastheer) is er gekomen omdat
  precies deze tak de vorm niet droeg. Als (b) een fix wordt, mag dat hek
  vallen en wordt `corpus/negatief/wf109-doelgastheer-periode` een golden. Dat
  is de reden dat deze kaart bij de §8.2-periodefamilie hoort en niet ervoor.
- (d) Alle veertien gates groen; gate-12-pins herberekend als een golden van
  kanaal wisselt.

### Aangescherpt bij de claim (2026-09-23, basis main 0f386245)

- (a) Hermeten op de huidige stand (na P1/P2/P3, leeg-negatie, lezerszone). Per
  route een document, vijf uitkomsten, verifier-schoon of niet. De nalens-vier
  worden gezocht in de families die de doel-poging bereiken: kenmerk (eigen en
  geërfd, met en zonder gastheer), vergelijking over een doel-getypeerd attribuut
  (met en zonder navigatierol), en naamnorm-verschillen tussen verifier en tak.
- (b) De keuze valt op de meting: werkt een herbedrading (een tijdelijke proef
  met `$subject` → patroonvariabele in de doel-poging) op de verifier-schone
  routes de JUISTE lezing uit, dan fix + golden; leest zij daar het verkeerde
  object, dan weigering in BEIDE takken met één gedeelde zin, een golden die
  gate 12 bij naam telt als ref en clingo hem dragen, en een eigen
  JUnit-getuige met rood-eerst.
- (c) Het hek valt alleen als alle vier de uitvoerders de vorm dragen; `ref/`
  en `backends/clingo/` vallen buiten dit May-touch, dus blijft het anders
  staan met expliciete motivering.
- (d) Veertien gates los groen; mutatieproeven (weigering terug per tak ⇒
  getuige + gate 12 rood ⇒ herstel, cmp) uitgevoerd en vastgelegd.

May touch: `backends/drools/**` (de bedrading of de weigering), `corpus/**`,
`spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` (**voorstel**, alleen als het
hek valt), `scripts/check-drools.sh` (**voorstel**, alleen de pins).

## Out of scope

- De `AttribuutRef.objecttype`-spanning zelf (een vreemd type wordt stil als
  navigatie gelezen) — genoemd in IR-SPEC ch. 4.6 (q2) als open spanning, en de
  V1-post B11 is de plek waar zij opgeruimd kan worden.
- De rest van de §8.2-heelperiodefamilie: dit kaartje gaat over ÉÉN positie,
  de feitcreatie-guard, en over de bedrading van één lowering.

## Result (2026-09-23, branch card/drl-periode-zink, basis main 0f386245)

**Gekozen: de WEIGERING, in beide Drools-takken, met één gedeelde zin.** Het
WF-109-hek blijft staan. Eén nieuwe golden (`jutterswet-seinjaar`) laat gate 12
de weigering bij naam tellen; één nieuwe JUnit-getuige
(`HeelperiodeDoelpogingWitnessTest`, 4 tests) pint de routes die geen golden
kunnen zijn.

### (a) De routes, hermeten op de huidige stand

Elk document is de `wf109-doelgastheer-periode`-getuige met één of twee zetten,
in het geheugen gebouwd; S1 draagt de getoetste tijdlijn heel 2024, S2 de
eerste helft, dus de DOELlezing is overal 1. `exec`/`drl` = de twee
Drools-takken vóór deze kaart; `FIX` = een tijdelijke proef-herbedrading
(`$subject` → `this` in de doel-poging, voor `temporeelKenmerkTijdreeks` en de
EIGEN-arm van `temporeelTijdlijnGetter`), teruggedraaid; `na` = na deze kaart.

| route | vorm | verifier | ref | clingo | exec | drl-tekst | FIX drl | na (exec / drl) |
|---|---|---|---|---|---|---|---|---|
| R1 | de negatief-getuige: gastheer `Sein` + kwalificatie | WF-109 FAIL | EVALUATION FAULT (gastheer) | TRANSLATION FAULT (idem) | PLAYED (doel, 1) | **zink** `rsGeheelPeriode($subject.getGehesen(), "JAAR", 7, null)` | PLAYED (doel) | weigering / weigering |
| R2 | vergelijking over een `Sein`-attribuut, geen navigatie | WF-045 + WF-053 FAIL | EVALUATION FAULT WF-045 | TRANSLATION FAULT (WF-043-zin) | PLAYED (doel, 1) | **zink** `rsGeheelPeriode($subject.getSeinhoogte(), …)` | PLAYED (doel) | weigering / weigering |
| R3 | tegenmeting: `gehesen` ook temporeel op de anker, geen gastheer | PASS | PASS (0) | PASS (0) | PLAYED | PLAYED | PLAYED | PLAYED / PLAYED |
| R4 | anker erft `gehesen` (temporeel) van supertype `Baken`, gastheer `Baken` | PASS | PASS (0) | PASS (0) | PLAYED | PLAYED | PLAYED | ongewijzigd |
| R5 | R4 zonder gastheer | PASS | PASS | PASS | PLAYED | PLAYED | PLAYED | ongewijzigd |
| R6 | R4 met ook `Sein` onder `Baken` | PASS | PASS | PASS | PLAYED | PLAYED | PLAYED | ongewijzigd |
| R7 | subtype herdeclareert het geërfde kenmerk niet-temporeel | WF-105 FAIL | case-fout | case-fout | ERROR (invoer) | ERROR (invoer) | idem | geen route |
| **R9** | naamnorm: anker `gehesen`, doel `Gehesen`, check `Gehesen`, geen gastheer | **PASS** | PASS (0) | PASS (0) | **FAIL, doel 1** | PLAYED — bij toeval (`$subject` in het doelpatroon IS de anker) | **FAIL, doel 1** | weigering / weigering |
| R10 | genavigeerde vergelijking via to-one `vlagsein` (navigatieRol) | PASS | EVALUATION FAULT | TRANSLATION FAULT | PLAYED (anker) | PLAYED (anker) | PLAYED | ongewijzigd — geen route |
| R11 | R10 zonder navigatieRol (twee feittypen Kustvak–Sein) | WF-053 FAIL | EVALUATION FAULT | TRANSLATION FAULT | FAIL (doel 1) | **zink** | FAIL (doel 1) | weigering / weigering |
| **R12** | R10 + een lid `vlagsein` op Kustvak dat de rolnaam verdringt | **PASS** | EVALUATION FAULT | TRANSLATION FAULT | EXCLUDE (oude zin) | **zink** | EXCLUDE (bean-limiet) | weigering (nieuwe zin) / MARKER (bean-limiet van de ankerpoging) |
| R13 | R10 met navigatieRol `Vlagsein` | PASS | EVALUATION FAULT | TRANSLATION FAULT | PLAYED | PLAYED | PLAYED | ongewijzigd — geen route |

De drie metingen van de kaart zijn gereproduceerd (R1, R2, R3), met één
verschuiving: sinds WF-109 weigeren `ref` en clingo R1, waar ze vóór het hek 1
gaven. **De vier verifier-schone routes van de nalens: in die vorm weerlegd.**
Gevonden zijn er TWEE die de doel-poging bereiken en verifier-schoon zijn — R9
(naamnorm) en R12 (rolnaamverdringing) — en geen van beide via een WF-109-vorm:
de WF-109-varianten (supertype-gastheer, geërfd, zonder gastheer) lowert de
ankerpoging gewoon (R4–R6), of de verifier wijst ze af (R1, R7). "Tweemaal via
WF-043-vormen" is vermoedelijk de WF-043-formulering in de weigerzinnen van
`ref` en clingo op R2/R10–R13; dat is geen verifier-oordeel. Wat de nalens
precies mat is nergens vastgelegd.

### (b) De keuze, met de afweging letterlijk

> De fix is technisch te richten zonder de andere aanroepers te raken:
> `temporeelKenmerkTijdreeks` heeft drie aanroepplaatsen (twee in
> `conditieTijdreeks`, de schrijfproducenten die altijd in de
> `$subject`-context staan, en één in `geheelPeriodeCall`), en ook
> `temporeelTijdlijnGetter` en `navigatieKenmerkTijdreeks` bedraden
> `$subject` hard. De proef liet R3–R6, R10 en R13 ongemoeid. Maar ze leest
> op ELKE route die de doel-poging bereikt het DOEL, en op de twee
> verifier-schone routes is dat het verkeerde object: op R9 zegt de verifier
> (sleutel = anker, geen gastheer) en zeggen `ref` en clingo 0, de fix zegt
> 1; op R12 gaat de guard over het vlagsein van de anker, niet over de eigen
> seinhoogte van het doel. De enige vorm waarop de doellezing de juiste is
> (R1, de gastheer op het doel) staat achter het derde WF-109-hek, en `ref`
> en clingo lezen een gekwalificeerde guard altijd op de anker, nooit op het
> doel (`guard_on_doel` matcht alleen `periode = None`; op R1 weigert daarna
> de gastheercheck met de WF-100-zin). Een fix zou dus een zichtbare
> compilerfout vervangen door een stil fout antwoord op verifier-schone
> documenten, en de vorm die hij wél goed zou lezen is van de wire. Daarom de
> weigering: kleiner, en de enige die op de gemeten routes waar is.

Uitvoering: `kern.Gastheer.doelpogingToegestaan` (de guard draagt nergens een
§8.2-kwalificatie — ook niet in een samengestelde of kwantorvouw-lid) en de
gedeelde zin `Gastheer.DOELPOGING_GEHEELPERIODE`. DRL: een `eval(false)` met
die zin als marker vóór de doel-poging. Exec: een SKIP met die zin plus de
bronsnippet. Rechtstreeks in beide takken, want het exec-antwoord op R9 was
een stil verkeerd getal (verifier-schoon, 1 tegen `ref`/clingo 0).

### (c) Het WF-109-hek: blijft

Het valt alleen als alle vier de uitvoerders een gekwalificeerde doelgastheer
dragen. `ref/` en `backends/clingo/` lezen een gekwalificeerde guard altijd
op de anker, nooit op het doel (`guard_on_doel` matcht alleen
`periode = None`), en liggen buiten deze kaart; en (b) leert dat geen enkele verifier-schone
route de doellezing vraagt. `corpus/negatief/wf109-doelgastheer-periode/NOTE.md`
legt dat nu vast, met de nieuwe Drools-uitkomst. Geen wijziging in
`spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` of `spec/verifier/`.

### Rood-eerst en mutatieproeven (letterlijk)

Getuige op de basiscode (commit 5a141dad vóór 389753b0):

    Tests run: 4, Failures: 3
    de_genavigeerde_vergelijking_met_verdrongen_rol_…: exec-model names 'voorwaarde niet naar anker of doel te lowereren — gehele-periodekwalificatie': transpiler SKIP: … (voorwaarde niet naar anker of doel te lowereren: de seinhoogte van het vlagsein …)
    de_naamnorm_route_…: exec-model — … Expected CaseRunner.Excluded to be thrown, but nothing was thrown.
    de_wf109_periodegetuige_…: exec-model — … Expected CaseRunner.Excluded to be thrown, but nothing was thrown.
    (de flank de_anker_lowerbare_… groen)

Na de fix: `Tests run: 4, Failures: 0`.

Mutatie A (DRL-weigering uit: `else if (false && …)`): `Tests run: 4,
Failures: 3` (drl-tekst: `drl compile` op R12 en R1, geen Excluded op R9);
golden `--drl` → `PLAYED 3`; R12 `--drl` → `EXCLUDE drl compile: … Unable to
Analyse Expression rsGeheelPeriode($subject.getSeinhoogte(), …`; gate 12 onder
de mutatie: `check-drools: FAIL — the backend's build+JUnit suite is red`
(589 tests, 3 failures). Herstel: `cp` terug, `cmp` identiek.
Mutatie B (exec-weigering uit): `Tests run: 4, Failures: 3` (exec-model); golden
exec → `FAIL case1: verwacht 2, werkelijk 1 | case2: verwacht 0, werkelijk 1 |
case3: verwacht 2, werkelijk 1`. Herstel: `cp` terug, `cmp` identiek, daarna
weer `EXCLUDE transpiler SKIP: … gehele-periodekwalificatie …`.

### (d) Gates, elk los na `eval $(opam env)`

1 self-contained PASS · 2 ir-schema PASS (274 files, floor 192) · 3 verifier
PASS (274 golden clean, 431 negatief, floor 350) · 4 case-schema PASS (632) ·
5 oracle PASS (780 worked examples; 591 cases) · 6 clingo PASS (480 checks;
591 cases) · 7 noop PASS (555 refute, 36 survivors) · 8 rood PASS (41, Drools
leg 9 fault-pinned / 32 not-carried) · 9 catala PASS (201 checks, 12 goldens) ·
10 regelspraak PASS (393 tests, 70 goldens) · 11 toka-parity PASS (29/29/29) ·
12 drools PASS (played 180 = pin, 53 non-players pinned; DRL played 157 = pin,
76 non-players pinned; JUnit 589) · 13 reftype-derivation PASS (131 twists,
128 replays, 25 DRL-played) · 14 flogic PASS (130 checks, 2 goldens).

### Beschermd-pad-voorstel

`scripts/check-drools.sh`, uitsluitend de lijsten: `jutterswet-seinjaar` in
`EXPECTED_CHANNEL` (EXCLUDE, transpiler SKIP) en in `DRL_CHANNEL` klasse C
(EXCLUDE, MARKER), elk met redenregel; de klasse-C-kop hertelt 12 (de lijst
stond op 11 onder een kop van 12 sinds bergingstoezicht vertrok, seinjaar
maakt het weer 12) en de gedeelde-grens-opsomming noemt de doel-poging i.p.v.
de vertrokken verdeling. `EXPECTED_PLAYED` 180 en `EXPECTED_DRL_PLAYED` 157
bewegen niet: de golden is nieuw en speelt in geen van beide takken.

### Open punten (niet in deze kaart)

- **Naamnorm in de temporele lookups van `backends/drools`.** 22 keer
  `kenmerkGranulariteitLeden().containsKey` en 50 keer
  `granulariteitLeden().containsKey` op exacte spelling, waar verifier, `ref`
  en clingo hoofdletterongevoelig vergelijken (R9). Te breed voor deze kaart;
  kaartwaardig. Wordt het gerepareerd, dan speelt `jutterswet-seinjaar` in
  beide takken en falen de twee pins bij naam — zo bedoeld.
- De genavigeerde §8.2-vergelijking (R10, R13): verifier-schoon, beide
  Drools-takken spelen haar, `ref` en clingo weigeren. De bekende rest van de
  §8.2-familie ("the navigated form is untouched"), buiten scope.
- **Dezelfde zink op een andere positie (lensvondst, slotronde):** een
  heelperiode-kwalificatie in het FILTER van een AANTAL ("het aantal posten …
  die gedurende het gehele jaar bemand zijn" > 0) is verifier-schoon, `ref`
  en clingo PASS 0, exec speelt correct, maar de DRL-tekst-tak zinkt
  pakketbreed (`Unable to Analyse Expression rsGeheelPeriode($subject.getBemand(), …)`),
  als creatie-guard én in een gewone rekenregel. Het zit in de accumulate van
  de ANKERpoging, buiten de ene positie van deze kaart. De hoofdsessie maakt er
  kaart `20260923-drl-aantal-filter-zink` van.
- De scalaire tegenhanger van R12 (zelfde document zonder kwalificatie) is
  gemeten: verifier PASS, `ref`/clingo PASS (0), beide Drools-takken weigeren
  bij naam. Geen open punt.

### Registerrijen (voorstel, Engels)

Rij 22 (Fact creation), kruisverwijzing, toe te voegen aan het eind van de wijzigingskolom:

> **Whole-period guard (2026-09-23, card drl-periode-zink):** a creation guard carrying a whole-period qualifier is tried on the anker only; both Drools branches refuse its doel attempt per construct, by name — see row 29.

Rij 29 (Whole-period qualifier):

> **Fact-creation guard, doel attempt (2026-09-23, card drl-periode-zink):** under a whole-period qualifier a creation guard binds the anker only (WF-109's third fence), and both Drools branches now refuse the DOEL attempt of such a guard by name ("voorwaarde niet naar anker of doel te lowereren — gehele-periodekwalificatie: alleen de anker is gebonden (WF-109)"), one sentence shared through `kern.Gastheer`. Before, the DRL-text branch lowered it with the whole-period call wired to `$subject` — the anker inside the `$doel` pattern — and either failed the whole package at compile or read the anker by accident, and the exec branch read the doel. Measured on two verifier-clean routes (a case-variant characteristic name, a role name displaced by a member): a rewired lowering reads the doel on both, which is the wrong object, so the fence stays and the form is a refusal, not a fix. Witness: `corpus/golden/jutterswet-seinjaar` (ref = clingo 2 / 0 / 2; both Drools branches refuse, pinned by name in gate 12) and `HeelperiodeDoelpogingWitnessTest`.
