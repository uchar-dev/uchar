# subselection-reads-subject: het predicaat van een subselectie leest het regelonderwerp

*Voorstel vanuit de werkbank (2026-10-23), door de eigenaar doorgegeven aan de uchar-sessie; tekst van de werkbank,
ongewijzigd op deze kop en de laatste paragraaf na. Aanleiding: normvraag
B2.5 van Fable-advies 2 (`uchar-dev/research/bruggen-2026-10-22/fable-advies-2.md`, vraag 2, wegen c1–c3) en vraag V1
van de review op de v2-wijziging (werkbank `werknotities/20261023-b25-nieuw-recht/review.md`).*

## Aanleiding

Art. 21 lid 2 onder a WW: het recht herleeft niet als een nieuw recht ontstaat met een maandloon van meer dan 87,5 % van
het maandloon van het eerdere recht. In v2 (hww-06, na B2.5) vouwt de uitzondering op het eerdere recht R1 over de
rechten van zijn samenloop en neemt het hoogste maandloon van de rechten die ontstonden uit een aanvraag met soort
'Nieuw recht':

```
Een Recht op uitkering WW heeft geen indicatie herleving recht op uitkering
indien hij aan alle volgende voorwaarden voldoet:
- zijn herlevingsgrens is gevuld
- de code soort aanvraag ww bij ontslag van de aanvraag uitkering van het Recht op uitkering WW is ongelijk aan 'Nieuw recht'
- (de maximale waarde van het maandloon van alle rechten van de samenloop van het Recht op uitkering WW die aan de volgende
  voorwaarde voldoen: de code soort aanvraag ww bij ontslag van de aanvraag uitkering van het RechtOpUitkering is gelijk
  aan 'Nieuw recht') is groter dan zijn herlevingsgrens.
```

R1 staat zelf in die samenloop. Na een intrekking kan R1 zelf een aanvraag 'Nieuw recht' hebben (v2 README open punt
32); dan telt R1 in de vouw mee, en het maandloon van een recht ligt altijd boven zijn eigen grens. De tweede voorwaarde
sluit dat uit, maar zet daarmee de uitzondering voor zo'n R1 helemaal uit, ook tegenover een echt nieuw recht boven de
grens: in de proef van de review herleeft R1 dan ten onrechte.

De juiste vorm sluit het onderwerp zelf uit de vouw uit, of vergelijkt met het onderwerp:

- (a) "… alle rechten van de samenloop van het Recht … die aan de volgende voorwaarden voldoen: … en die niet het Recht
  zelf zijn";
- (b) "… die aan de volgende voorwaarde voldoen: zijn eerste rechtdag is later dan de eerste rechtdag van het Recht" —
  weg c3 van Fable-advies 2, daar "niet uitdrukbaar" genoemd.

Dat kan vandaag niet:

- RegelSpraak v2.3.0 §5.6: in het predicaat van een subselectie leest "zijn" het element; de specificatie heeft geen vorm
  voor het onderwerp van de regel.
- uchar: een lezing van het regelonderwerp in een filter krijgt alleen een binding via route (i) van K13, een weg van
  het element terug naar het onderwerp met elke stap to-one; samenloop → rechten is to-many, dus die weg bestaat hier
  niet. En is het element van het type van het regelonderwerp, dan weigert de frontend de naam als dubbelzinnig
  (RGS-350; Fable-advies 2026-10-22, vraag 2, "Zelfde type").

## Doel

Een dialectvorm (DIVERGENCES.md) waarmee het predicaat van een subselectie het onderwerp van de regel leest, ook als het
element van hetzelfde type is:

- een identiteitstoets van het element tegen het onderwerp (vorm a);
- een attribuut of kenmerk van het onderwerp in een vergelijking met een lid van het element (vorm b).

uchar kiest de woorden en zegt waarom; eis is dat de vorm eenduidig is bij hetzelfde type (een vast woord voor het
onderwerp, niet de typenaam). In de IR een verwijzing naar het onderwerp in het filter van een `Aggregation`, met de
betekenis in IR-SPEC en RUNTIME-SEMANTICS (verhouding tot ch. 4.10, variabelen in een aggregatie; een leeg attribuut van
het onderwerp in de vergelijking), in elke uitvoerder (verifier, ref, Drools, clingo) en in de lezingen van ch. 5.12,
zodat de uitleg van de werkbank de vergelijking ziet.

Te bewaken: de vorm mag geen nieuwe kring over de instanties openen. In het gebruik van v2 leest zij alleen invoer van het
onderwerp (de identiteit, de eerste rechtdag), niets afgeleids; leest een document via deze vorm een afgeleide waarde
binnen het sterk samenhangende deel, dan weigert de verifier zoals bij lezing 4.

## Acceptatie

- (a) Een golden met een vouw over een hub waarvan het element het type van het regelonderwerp heeft, met vorm a en
  vorm b, in elke uitvoerder gelijk; clingo met één stabiel model.
- (b) Negatieven: dezelfde naam zonder de nieuwe vorm blijft dubbelzinnig (RGS-350); vorm b op een afgeleide waarde in
  een kring wordt geweigerd met de lezing genoemd.
- (c) DIVERGENCES.md, IR-SPEC, RUNTIME-SEMANTICS en de CONSTRUCTS-rij; `make gates` groen.

## Daarna, in de werkbank (geen deel van deze kaart)

hww-06 sluit R1 uit met vorm a (of leest vorm b); de voorwaarde op de eigen aanvraag van R1 vervalt, en de proef van
de review (een ingetrokken recht met een eigen aanvraag 'Nieuw recht' naast een echt nieuw recht boven de grens) wordt
een scenario: R1 herleeft niet.

## Aantekeningen bij de opname (hoofdsessie, 2026-10-23)

- **Beschermde paden.** De kaart raakt `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/` en
  `spec/verifier/` (een verwijzing naar het onderwerp in het filter van een `Aggregation` en de test van een
  `QuantifierFold`), en waarschijnlijk `corpus/negative/`; wat daar verandert, landt alleen met het mandaat van de
  eigenaar.
- **Ook in de test van een kwantor** (voorlopig besluit bij de opname, na de vraag van de eigenaar waarom niet). RGS-349
  (de test van een kwantorvouw leest het onderwerp van het type van het element) heeft dezelfde oorzaak als RGS-350: de
  test en het filter worden allebei per element gelezen, met het element als onderwerp (RUNTIME-SEMANTICS ch. 3.9a), en
  het IR heeft daar geen verwijzing naar het buitenste onderwerp (DIVERGENCES.md, "A read of a rule subject of the
  element's own type in a fold — refused"). De verwijzing geldt dus in elke context die het onderwerp naar het element
  verschuift: het filter van een `Aggregation` en de test van een `QuantifierFold`. Acceptatie (a) en (b) gelden voor
  beide; een beperking tot het filter zou een kunstmatige grens zijn.
- **Twee spellingen van één lezing.** Waar een to-one weg terug bestaat, schrijft de frontend een lezing van het
  regelonderwerp in een filter nu als navigatie vanaf het element (DIVERGENCES.md, "A read of the rule subject in a
  filter — written over the way back"): van een lading terug naar haar schuit is, voor elke lading van die schuit, de
  schuit zelf. Met de nieuwe verwijzing kan dezelfde lezing ook direct. Beide blijven geldig IR (een andere frontend of
  een document met de hand mag de navigatie schrijven); de vraag is niet welke verboden wordt, maar:
  1. welke de RegelSpraak-frontend schrijft waar beide kunnen (de weg terug houden en de verwijzing alleen schrijven
     waar die weg ontbreekt, zodat bestaande goldens bytegelijk blijven; of altijd de verwijzing, zodat de weg terug als
     omweg verdwijnt);
  2. dat beide spellingen hetzelfde oordeel van de verifier krijgen (afhankelijkheidsgraaf, kringen, lezing 4) en
     hetzelfde antwoord in elke uitvoerder: een paar goldens dat dezelfde regel op beide manieren spelt, als getuige.
- **Fable-advies**: al vóór de bouw gevraagd, op verzoek van de eigenaar; zie de volgende paragraaf.

## Voorlopige besluiten naar het Fable-advies (2026-10-23)

Advies: `uchar-dev/research/subselection-reads-subject-2026-10-23/fable-advies.md` (vraag: `fable-vraag.md` ernaast;
meetscript `proef/wayback_count.py`). Het criterium van de eigenaar: het IR mag veranderen, maar wordt niet
dubbelzinnig, niet vies en verrast niet. De aanbevelingen zijn voorlopige besluiten; de eigenaar kan elk omdraaien.

- **B1, de vorm in het IR.** Een optioneel veld `anchor` met als enige waarde `"form"` op de knopen die een wandeling bij
  het onderwerp beginnen (`AttributeReference`, `CharacteristicCheck`, `Aggregation`, `QuantifierFold`), en een nieuwe
  voorwaardeknoop `IdentityCheck {anchor, negated, source}`. Eén evaluatieregel: een geankerde knoop wordt geëvalueerd
  met het onderwerp van de vorm als onderwerp, alsof hij in de eigen voorwaarde of waarde van de vorm stond. Zonder
  anker verandert geen document. Afgewezen: `Let`/`BinderRef` (dekt de identiteitstoets en de test van een kwantor niet,
  en de leeg-barrière geeft een ander antwoord dan ch. 1.2d) en een veld op de vouw (dekt de vergelijking niet).
  `RoleMembership` en `InstanceSetUnion` krijgen geen anker, met reden in IR-SPEC.
- **B2, spellingen.** Beide blijven geldig IR. De frontend schrijft het anker overal waar de zin het regelonderwerp
  noemt, en de navigatie waar de zin vanaf het element navigeert ("de schuit van de Lading"); de lowering over de weg
  terug (`frame`/`reanchor`/`wayBack`) verdwijnt, en RGS-348 en RGS-352 vervallen. Gemeten over 921 documenten: 13
  knopen in 3 goldens krijgen een ander `ir.json` (`fold-filter-nested-aggregate`,
  `jutterswet-schone-maand-na-zware-klacht`, `jutterswet-vergunningtermijnen`), hun cases niet; toka, red en negative
  nul. Getuige: een golden die dezelfde lezing op beide manieren spelt, gelijk gepind.
- **B3, het woord** (eigenaar en werkbank: het staat in de wetszinnen van hww-06): `<lidwoord> <Typenaam> zelf`, "die
  niet het Recht zelf zijn", "later dan de eerste rechtdag van het Recht zelf"; `ZELF` wordt een sleutelwoord. Het
  alternatief was het typevrije "het regelonderwerp".
- **B4, de blote typenaam.** "het Recht" zonder `zelf` blijft RGS-349/RGS-350 waar het element het type van het
  onderwerp heeft, met `zelf` als uitweg in de melding; de blote naam van een ander type blijft het regelonderwerp.
- **B5, de test van een kwantor.** Dezelfde vorm, zonder beperking (bevestigt de aantekening hierboven).
- **B6, een tussenliggend element** in een geneste subselectie is bij zijn typenaam niet meer aanspreekbaar en wordt luid
  geweigerd (vandaag kan het over een to-one weg terug; 0 voorkomens in 921 documenten); de lezing over zijn rol
  blijft. Open punt voor PLAN §9 bij de landing.
- **B7, lezingen ch. 5.12.** Een geankerde knoop krijgt in deze kaart geen eigen lezing; een vervolgkaart als de uitleg
  van de werkbank die waarde nodig heeft.
- **B8, werkbank** (normvraag, niet van uchar): hww-06 met vorm a (R1 zelf uitgesloten); vorm b alleen als de
  wetsuitleg "nieuw" als "later ontstaan" leest.

Correctie op het advies: de nieuwe welgevormdheidsregel heet daar WF-120, maar dat nummer heeft de overbodige
benaming (vocabulaires, IR-SPEC ch. 2.12) en WF-121 de herkomst; de nieuwe regel krijgt het eerstvolgende vrije nummer,
nu WF-122. Het stappenplan van het advies (spec en schema, verifier en negatieven, ref, clingo, Drools, frontend,
corpus en CONSTRUCTS) is het bouwplan.

**Antwoord van de werkbank (2026-10-23, via de eigenaar).** "B8: vorm a. Het woord 'zelf' is goed; let op dat het in
scenario- en regelnamen vrije tekst blijft (v2 heeft 'zelf' in twee scenarionamen)." B3 en B8 zijn daarmee besloten.
Over de namen: `scenarioNaam` en `regelNaam` nemen elk token behalve hun eigen kopwoorden (`RegelSpraak.g4`), dus een
token `ZELF` blijft daar vrije tekst, en een woord als "zelfstandige" lext als één `LOWER_ID`. Extra acceptatie: een
frontend-test die een scenarionaam en een regelnaam met "zelf" erin (in de vorm van de twee v2-namen, "… van het recht
zelf in een samenloop", "… dat zelf is geëindigd") parset zoals vóór de kaart, en dat de 179 bronnen van de repository
bytegelijk emitteren.
