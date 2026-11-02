# static-precision-gate: frontend en verifier weigeren een schrijfactie die niet gegarandeerd past

*Vastgelegd door de hoofdsessie op 2026-11-02, op besluit van Yeb dezelfde dag. Onderzoek:
`/Users/yeb/Developer/uchar-dev/research/precisie-en-afronding-2026-11-01/` (`rapport.md`, beslispunten B1 en B2,
`notes/regelspraak.md` voor ALEF). Pas oppakken na `20261009-drools-schrijfgrens`.*

Goal: een auteur krijgt de fout bij het opstellen, niet bij één bedrag in productie. Een schrijfactie waarvan de typering niet
garandeert dat de waarde in de decimalen van het doel past, is een statische fout in de frontend en in de verifier, in de vorm die
ALEF gebruikt. De toets bij uitvoering (EF-006, ch. 2.4 punt 1 na `drools-schrijfgrens`) blijft als vangnet.

## Aanleiding

Na `drools-schrijfgrens` is een waarde die niet past een evaluatiefout. Die hangt van de waarde af: `x gedeeld door 3` naar een
veld met 2 decimalen speelt bij x = 999 en faalt bij x = 1000. Een regel kan dus op alle testgevallen spelen en in productie
falen. ALEF voorkomt dat statisch. De frontend doet het al voor `maal`, `plus` en literals (RGS-338), maar laat de deling door,
omdat ch. 2.3 haar op maxDec typeert (`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Typing.java:122-127`).
De verifier heeft geen precisietypering: de "precision gate" van WF-023 staat OPEN in IR-SPEC.

## De regel (ALEF-vorm, zoals gelezen in de MPS-modellen van ALEF)

Elke expressie krijgt een aantal decimalen uit de declaraties, als bovengrens; een schrijfactie waarvan dat aantal groter is
dan de decimalen van het doel, is een fout, behalve naar de onbegrensde vorm (`Numeriek (getal)`).

- `plus`, `min`, `verminderd met`: het hoogste aantal; `maal`: de som (ch. 2.3, ongewijzigd).
- `gedeeld door` met een noemer die geen literal is: oneindig; past alleen in de onbegrensde vorm. Met een literal-noemer: de
  decimalen van de teller plus die van 1/noemer (`/ 2` één erbij, `/ 4` twee, `/ 3` oneindig).
- Percentage: links + 2 + rechts, nog na te gaan in ALEF (het rapport heeft dit niet per golden nagerekend).
- Minimum en maximum: het hoogste aantal van de operanden (ch. 2.3 neemt nu het laagste, en claimt daarmee te weinig).
- Eenheidsomrekening: de factor telt mee (km naar m kan decimalen wegnemen, m naar km voegt er drie toe; ALEF doet dit, de
  frontend niet, `Typing.java:369`).
- Gemiddelde en tijdsevenredig deel: delingen, dus oneindig.
- Afronding op n: n.
- Verdeling: alleen het type van de pot (zoals ALEF en RGS-340). Een aandeel dat niet past, blijft EF-006 bij uitvoering. Dat houdt
  EF-006 bereikbaar voor een rood geval.

Een waarde die past, maar waarvan de typering het niet garandeert (`x gedeeld door y` met y altijd een deler van x), vraagt dan
een afronding die niets verandert. Wie geen tussentijdse afronding wil, schrijft het tussenresultaat in `Numeriek (getal)` en rondt
af in het eindattribuut.

## Normvraag vooraf: de Catala-route

Een Catala-`decimal` is exact en staat nu op een vaste schaal van 4 decimalen (ch. 2.9). Onder deze regel weigert de verifier elke
Catala-deling naar zo'n veld, want een Catala-bron heeft daar geen afronding. Geen van de 12 Catala-goldens raakt het nu. Opties:
(a) een Catala-`decimal` wordt de onbegrensde vorm, en clingo weigert dan een waarde die niet geheel is met naam; (b) de Catala-route
valt buiten de regel, en dat is een uitzondering per frontend op een norm van de IR. Beslissen vóór de bouw (naar Fable als Yeb er
niet is). **Besloten door Yeb op 2026-11-02: (a), een Catala-`decimal` wordt de onbegrensde vorm.**

## Wat er verandert

| Wat | Hoeveel | Wat ermee |
|---|---|---|
| goldens met een deling door een attribuut naar een begrensd doel | 25 goldens, 33 schrijfacties | `Rounding` invoegen of een onbegrensd tussenattribuut; de waarde past aan het fixpunt, dus de verwachtingen blijven gelijk |
| goldens met een percentage of een literal-noemer (ALEF-vorm) | 7 en 3, niet per golden nagerekend | idem |
| `anchor/` daaronder | `jutterswet-vindersloon`, `jutterswet-tariefstelling` | beschermd: voorstel aan de maintainer |
| goldens met een `source.rgs` daaronder | 10 | de bron mee, gate 10 en zo nodig gate 16 |
| de acht witnesses van ch. 5.7 (`jutterswet-fout-op-tussenstand`, `-initialisatie-naast-regel`, `-lezer-van-initialisatie`, `-deling-in-afwijking`, elk met `-omgekeerd`) | 8 | opnieuw op een andere foutsoort, want een niet-passende tussenstand van EF-006 kan dan niet meer ontstaan |
| rode EF-006-gevallen met een deling | 9 | worden negatieve gevallen van de nieuwe WF-arm, of krijgen een verdeling; `EXPECTED_MIN_RED_CASES=44` en ch. 5.7b moeten mee |

Lijsten per golden: de scan `meting/reconcile2.py` in de onderzoeksmap.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een WF-arm in de verifier (de precisiepoort van WF-023, of een eigen nummer) met een negatief geval per regel hierboven, en
  een golden die elk geval laat passen met een afronding of een onbegrensd doel.
- (b) De frontend geeft dezelfde uitkomst als de verifier op dezelfde bron (RGS-338 voor de deling, het percentage en de eenheidsfactor).
- (c) Ch. 2.3 en ch. 2.4 beschrijven de typering en de poort; de kandidaatregel van de v0-noot en CONSTRUCTS rij 2 gaan erin op.
- (d) De goldens en witnesses van de tabel aangepast, met ref ≡ clingo ≡ beide Drools-branches op dezelfde absolute verwachtingen.
- (e) De Catala-normvraag beslist en uitgevoerd.
- (f) `make gates` groen.

## Omvang

Middel tot groot: de typering in de verifier is nieuw, de rest is grotendeels mechanisch.

May touch: `spec/verifier/`, `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Typing.java`, `corpus/golden/`,
`corpus/negative/`, `corpus/red/`, `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `spec/CONSTRUCTS.md` (main-sessie), `scripts/check-red.sh`,
`frontends/catala/` (na de normvraag); beschermde paden als voorstel.
