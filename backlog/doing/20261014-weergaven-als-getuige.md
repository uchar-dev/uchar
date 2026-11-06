# weergaven-als-getuige: plan, executable model en DRL-tekst van het corpus vastgezet in de repository

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit
besluit 41 van die kaart (Fable-advies 4, §1). Een vervolg op die kaart, geen fout.*

## Aanleiding

Tijdens die kaart bewaakten drie tekstdumps over het hele corpus de analyse en haar twee weergaven: de planvorm (het
plan per regel, 250 goldens, 1851 regels), de modelvorm (het executable model, 21558 regels) en de drlvorm (de
DRL-tekst, 256 teksten, 41836 regels), elk tegen een nulmeting in `/tmp`. Fable-advies 4 bracht zeventien mutaties in;
vier glipten door het gedragsharnas en alleen een tekstdump zag ze:
- W1, de volgorde van de feiten binnen één gevolg, en W5, een bredere catch om het creatiebudget: geen semantiek, maar
  een verandering van de tekst;
- W4 en A1, de `collapse` van een breuk in rust weggelaten in de weergave of in het plan (sinds de landing heeft dat een
  gedragsgetuige).

Na de landing bestaan die nulmetingen niet meer. Een verandering in een weergave is dan alleen nog zichtbaar waar ze
het gedrag verandert.

## Acceptatie (bij oppakken aanscherpen)

- (a) Kies de vorm: de dumps zelf in de repository (groot), of per golden een hash met een hulpmiddel dat de dump
  opnieuw maakt en het verschil toont. Weeg de omvang tegen de leesbaarheid van een review.
- (b) Een getuige in de suite vergelijkt de dumps van de huidige code met wat vastligt, en faalt met de naam van de
  golden en de eerste afwijkende regel; een bewuste wijziging zet de vastgelegde vorm in dezelfde commit opnieuw.
- (c) Rood eerst met een mutatie als W1 van Fable-advies 4.

May touch: `backends/drools/**` (testboom, testbronnen).

Out of scope: `scripts/` (geen nieuwe gate; de suite draait in gate 12).
