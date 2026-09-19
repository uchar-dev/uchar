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

May touch: `backends/drools/**` (de bedrading of de weigering), `corpus/**`,
`spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` (**voorstel**, alleen als het
hek valt), `scripts/check-drools.sh` (**voorstel**, alleen de pins).

## Out of scope

- De `AttribuutRef.objecttype`-spanning zelf (een vreemd type wordt stil als
  navigatie gelezen) — genoemd in IR-SPEC ch. 4.6 (q2) als open spanning, en de
  V1-post B11 is de plek waar zij opgeruimd kan worden.
- De rest van de §8.2-heelperiodefamilie: dit kaartje gaat over ÉÉN positie,
  de feitcreatie-guard, en over de bedrading van één lowering.
