# frontend-datumranden: kale NPE en stil tijdverlies bij datuminvoer

Twee gemeten defecten, gevonden door de P5-lens (beide ouder dan die
kaart, beide via de CLI gereproduceerd), zelfde familie: de frontend
verliest of crasht op datumranden waar de fail-fast-regel een benoemde
fout eist.

1. De KALE-datumtak (`ExpressieBouwer.java` ~444) crasht met een
   `NullPointerException` op een onmogelijke datum (`30-02-2026`):
   `Literalen.parseDatum` geeft null en de tak leest ongewacht door.
   De datumtijd-tak geeft sinds de P5-kaart een ERROR-diagnostic
   zonder uitvoer; de kale tak hoort dezelfde benoemde weigering te
   krijgen.
2. `geldig vanaf 01-01-2026 10:00:00.000` emitteert stil
   `"vanaf": "2026-01-01"` — de meegeschreven tijdcomponent valt
   zonder diagnostic weg. Stil informatieverlies; de frontend hoort te
   weigeren of de component te dragen (weigeren ligt voor de hand: de
   geldigheidsvelden zijn datumgranulair).

## Acceptatie (bij oppakken aanscherpen)

- (a) Beide paden geven een benoemde ERROR-diagnostic zonder uitvoer;
  rood-eerst een JUnit-getuige per pad (de NPE resp. de stille emissie
  eerst gezien).
- (b) Corpussweep: geen bestaande golden of toka-fixture draagt een
  van beide vormen (de lens telde nul dragers — hermeet bij oppakken).
- (c) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, `corpus/**`,
kaartverplaatsing.
