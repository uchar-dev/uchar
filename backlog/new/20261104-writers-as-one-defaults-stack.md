# writers-as-one-defaults-stack: alle schrijvers van een doel als één impliciete defaults-stapel

*Opgesteld door de hoofdsessie op 2026-11-04, op verzoek van de eigenaar. Na de kaart `defaults-per-period-on-timeline`.*

## Aanleiding

De ASP-backend weigert invoer uit de case op een berekend attribuut dat naast een rekenende regel ook een initialisatie, een verdeling of
een toekenning bij het maken van een object schrijft (`TRANSLATION FAULT`, het hek van `spec/ASP-TRANSLATION.md` S2 en S19). In ASP staat
een feit naast alles wat de regels afleiden; een invoer zonder weerlegbare route geeft dus twee waarden tegelijk, en het hek weigert liever
dan een fout antwoord te geven. Voor rekenende regels en defaults-groepen bestaat die route (`inputval/3` met `not wdecides`, de
defaults-keten met `not`); voor de drie andere schrijvers niet. ref en Drools beantwoorden de gevallen wel; geen golden draagt er een, dus
geen gate merkt het (kaart `empty-write-decides-nothing`, Result, VB-3).

RS ch. 7.8 beschrijft de invoer al als default, de virtual input default, tussen de laagste gewone default en de initialisatie.

## Goal

Elke schrijver van een doel staat in één impliciete defaults-stapel, per dag op een tijdlijndoel (kaart `defaults-per-period-on-timeline`):

1. rekenende regels, verdelingen en toekenningen bij het maken, onderling van gelijke rang (verschillen er twee op een dag: EF-016);
2. de invoer uit de case;
3. de initialisatie (§9.6: alleen waar er nog geen waarde is).

Een schrijver die met een lege waarde vuurt, beslist niets (besluit van de eigenaar, 2026-11-04). In clingo wordt de stapel één generieke
codering, de defaults-keten met `not`, en het hek verdwijnt. ref en Drools volgen die rangorde al; meet het en schrijf het als één regel
in RS ch. 7.8 (en ch. 3.11, 5.7 waar ze dezelfde rangorde in eigen woorden zeggen).

## Acceptance

- (a) Rood eerst: invoer op een doel met een rekenende regel en een (voorwaardelijke) initialisatie, een verdeling en een toekenning bij het
  maken, elk met een regel die beslist en een die leeg oplevert, scalair en, waar de vorm een tijdlijn toelaat, per dag: clingo weigert op
  main, en speelt daarna met de uitkomsten van ref en Drools. Goldens voor de vormen die het corpus nog niet draagt.
- (b) Elke bestaande verwachting blijft; de kanaal- en divergentielijsten van gate 6, 8 en 12 krimpen alleen waar het hek een regel had.
- (c) RS ch. 7.8 als huidige stand (voorstel), met de stapel als één regel en een uitgewerkt voorbeeld; ASP-TRANSLATION S2, S3, S10, S19
  zonder het hek; CONSTRUCTS rij 8.
- (d) `make gates` groen.

May touch: `backends/clingo/`, `ref/` en `backends/drools/**` alleen waar de meting een verschil toont, `spec/RUNTIME-SEMANTICS.md` en
`spec/ASP-TRANSLATION.md` (voorstel), `spec/verifier/` (het staande domein, voorstel), `corpus/golden/`, `scripts/` (alleen lijsten die
krimpen; voorstel), kaartverplaatsing.

Meenemen (gemeten in `flavours-of-empty`): clingo weigert een door de case gegeven enumeratiewaarde op een berekend doel zonder
registercode ("a case-supplied text/enum computed target … has no encoding here"); onder de stapel krijgt die invoer dezelfde route.

## Omvang

Klein tot middel.

## Result
