# let-in-composite: een lokale naam in een later deel van een voorwaarde, bij naam geweigerd

*Opgesteld door de hoofdsessie op 2026-11-22. Besluit van de eigenaar op dezelfde dag, na een onafhankelijke second
opinion: de combinatie hieronder wordt in de spec en in elke executor bij naam geweigerd, zodat de twee regels over een
binder van een `Let` uit ch. 1.5 vervallen.*

## Aanleiding

In een document met `inferFromEmpty: false` leest een samengestelde voorwaarde door voorbij een lid dat niet vaststaat;
zo'n later lid wordt voorlopig gelezen (ch. 1.5). Leest dat lid een lokale naam van een `Let` eromheen ("daarbij geldt"),
dan staat niet vast of die naam gelezen mag worden. Ch. 1.5 legt daarvoor twee regels vast: de barrière van een `Let` is
dan een open lezing, en wat het berekenen van de naam deponeert wacht op een gewone lezing. De eigenaar wil die regels
niet: geen enkele regelset kan ze gebruiken zolang de Drools-backend ze niet draagt, en in het corpus komt de vorm niet
voor (32 goldens met een `Let`, één met een samengestelde voorwaarde in de romp, geen enkele die de naam in een later lid
leest). De vorm wordt daarom een vorm zonder lezing, die elke executor bij naam weigert. Is zij later nodig, dan komt er
een eigen kaart.

Los daarvan staan in ref twee fouten in de lezing van ch. 1.5, gevonden doordat de clingo-vertaling op duizenden gevallen
naast ref gelegd werd:

1. ref houdt een attribuut en een kenmerk van één naam niet uit elkaar waar het om open leden gaat (`World.opened`,
   `open_inputs`: sleutel op label en genormaliseerde naam). Voorbeeld: een Vondst met het attribuut *toeslag*, dat een
   defaults-groep van *gemerkt* laat afhangen, en het kenmerk *toeslag*, dat een defaults-groep aan een volledig kenmerk
   geeft; *gemerkt* open. Ch. 1.5: het attribuut is open, het kenmerk staat vast als niet. ref noemt beide open. Waar het
   kenmerk invoer is en het attribuut geschreven wordt, noemt ref het kenmerk vaststaand afwezig waar ch. 1.5 het open noemt.
2. ref verliest een sluiting waar de barrière van een `Let` in de voorwaarde van een `IfThenElse` valt. Voorbeeld: "het
   keurgewicht van een Jutter is, daarbij geldt: t is de som van het gewicht van zijn vondsten waarvan getuigen leeg is, 1
   als t gelijk is aan 3, anders t", over een vondst met een volledig leeg gewicht en open getuigen. Een waarde die na een
   sluiting leeg is, laat haar doel open (ch. 1.5): het keurgewicht is open. ref geeft vaststaand leeg.

## Wat er komt

1. **De vorm zonder lezing.** Een rij in de tabel "Forms without the reading" van ch. 1.5, als laatste, met de slug
   `let-in-composite`: een `Let` (ch. 10.6) waarvan een voorwaarde in de romp de binder leest in een lid van een
   samenstelling dat niet haar eerste is, op elke diepte, of in het filter of de toets van een kwantorvouw. Een document
   met die vorm wordt geweigerd zoals elke vorm in de tabel (REF-014 met de slug).
2. **De twee regels vervallen uit ch. 1.5**: de zinnen over de barrière en over de binder in "The judgement of a condition"
   (en in de kwantorvouw, waar zij genoemd worden), met hun voorbeelden. Het voorlopig lezen zelf blijft (oordeel telt,
   deponering vervalt, fout maakt het lid onbepaald). De examples `WV-1.5-past-undetermined-barrier` en `-binder`
   vervallen; wat zij aan de ref-suite toevoegden, wordt vervangen door getuigen van de weigering (zie Acceptance).
3. **ref verkleint**: `provisional` op de uitgestelde binder, het omzetten van de barrière in `judge_cond` en het
   uitstellen van deponeringen van een binder vervallen; `Let_barrier` en de lezing van een `Let` in een gewoon document
   blijven byte-gelijk.
4. **De twee fouten in ref hersteld**, elk met een getuige die vóór het herstel rood is.

## Niet in deze kaart

clingo (de branch `card/open-values-in-clingo` neemt de slug over bij zijn landing) en Drools (neemt hem over in het
volgende bouwdeel). Deze kaart raakt `ref/` en ch. 1.5.

## May touch

`spec/RUNTIME-SEMANTICS.md` ch. 1.5 (en de verwijzingen ernaar in ch. 3.9a en ch. 5.12 die de vervallen voorbeelden noemen),
`ref/lib/`, `ref/test/`, `ref/README.md` waar het de vorm noemt. Dit is mandaat van de eigenaar voor dit hoofdstuk.

## Acceptance

- Getuigen `WV-1.5-let-in-composite-*`: een samenstelling die de naam in een later lid leest, één die haar in een genest
  later lid leest, een filter en een toets van een kwantorvouw, worden geweigerd met de slug; een `Let` waarvan de romp de
  naam alleen in het eerste lid, of buiten elke voorwaarde leest, speelt onveranderd; zonder `inferFromEmpty: false` speelt
  elk van deze documenten zoals nu.
- Getuigen voor fout 1 (attribuut en kenmerk met één naam, beide kanten) en fout 2 (sluiting en barrière), elk rood op de
  code van vóór het herstel.
- De suite van ref telt niet minder dan 489 voorbeelden (gate 5); elke vervallen getuige staat in het Result met de reden.
- Gates 1, 3, 5, 6, 7, 8 (ref-been), 14 groen; gate 5 met dezelfde tellingen voor behoud en monotonie als op main
  (280 goldens, 784 cases; 2255 runs, 401 sluitingen), of met de gemeten reden waarom een getal verschuift.
