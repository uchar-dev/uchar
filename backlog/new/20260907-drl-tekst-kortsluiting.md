# De DRL-tekst-tak en ch. 5.10 — VOORSTEL: laten vervallen

**Status: kandidaat om te vervallen (2026-09-07). Niet zelf verwijderd —
dat is een maintainerbesluit.**

Goal (oorspronkelijk): de slapende DRL-TEKST-tak van de Drools-backend
(`DrlTranspiler`) de getrouwe ch.-5.10-kortsluiting geven. Hij vertaalt
de telkwantoren als een TEKSTUELE k-deelverzameling-vouw — `ten hoogste
d` wordt `!(minstens(leden, d+1))` — en die vouw week af van de norm.

## Waarom deze kaart vermoedelijk overbodig is geworden

Het **maintainerbesluit van 2026-09-07** (genummerde vraag 3) nam
at-most's WAAR-trigger op: `ten hoogste d` stopt óók zodra
hits + resterend ≤ drempel. Daarmee is ch. 5.10 precies de duale van
`ten minste` — en dat is exact wat de tekst-tak al berekende:
`!(minstens(leden, d+1))` ís de at-most-lezing als "niet ten minste
d+1", en at-least's twee triggers (bereikt / onbereikbaar) vallen
één-op-één samen met at-most's twee zijden. De tak implementeerde de
duale al; de norm zegt nu hetzelfde.

**Hermeten over het volledige leesverzamelingsmodel** (beide richtingen,
alle vijf kwantoren, elke drempel inclusief d > n, elke
waarheidstoekenning), n = 1..7, 6646 combinaties:

| meting | DRL-tekst | clingo |
|--------|-----------|--------|
| waarde-divergenties t.o.v. ch. 5.10 | **0** | **0** |
| leest een lid dat ch. 5.10 overslaat (fault die de norm niet heeft) | **0** | **0** |
| slaat een lid over dat ch. 5.10 leest (antwoord waar de norm fault) | **0** | **0** |

End-to-end bevestigd op precies de getuige die vóór het besluit de
smoking gun was — `TEN HOOGSTE 1` over ÉÉN lid dat door nul deelt:

```
ref         : W1.geenteken = 1
clingo      : W1.geenteken = [1]
drools exec : 1
drools DRL  : 1
```

Vier executors, één antwoord. Vóór het besluit faultten de eerste drie
en antwoordde de vierde.

Ook `corpus/golden/jutterswet-vacuumwacht` (de vacuüm-vorm d ≥ n mét een
faultend lid, plus de waar-stop onder het ledental) speelt in **beide**
Drools-takken: `PLAYED` / `PLAYED`.

De waakhond-naamlijst die deze kaart in
`scripts/check-reftype-derivation.sh` had staan is daarmee opgeheven; er
is geen vorm meer om te bewaken. De meting staat wél reden-dragend in
dat script vastgelegd.

## Wat er nog zou pleiten vóór doorgaan

Eerlijkheidshalve, want "0 divergenties" is een MODEL-uitspraak plus een
handvol end-to-end-metingen, geen bewijs:

* het model abstraheert de tak tot "k-deelverzameling-OR met Java-luiheid";
  een implementatiedetail dat het model niet kent kan alsnog afwijken;
* n ≤ 7 is uitputtend, daarboven niet gemeten;
* de tekst-tak blijft een tweede, structureel andere vertaling van
  dezelfde norm — dat hij vandaag samenvalt is geen garantie dat een
  volgende ch.-5.10-wijziging hem meeneemt.

Wie die risico's wil afdekken kan de kaart smaller herformuleren: niet
"maak de tak getrouw" (hij ís het) maar "geef de tak een eigen
ch.-5.10-getuige in de backend-JUnit-suite, zodat een toekomstige
normwijziging hem niet stil achterlaat".

## Voorstel

Laten vervallen, óf herformuleren tot de smalle getuige-kaart hierboven.
Beide zijn maintainerbesluiten; deze kaart blijft tot dan in `new/`.
