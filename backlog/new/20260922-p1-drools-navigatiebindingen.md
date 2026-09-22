# p1-drools-navigatiebindingen: de loader leest de navigatiebindingen van de wire

V1-preconditie P1 (spec/proposals/V1-WIREMIGRATIE.md §2.2 en §7,
blokkeert B11). `backends/drools`' `IrLoader` NEGEERT vandaag
`AttribuutRef.navigatieBindingen` en `Kenmerk.navigatieBinding` en
leidt de navigatie her-af uit de v0-velden `navigatieRol`/
`navigatiePad` — precies de velden die bij B11 van de wire verdwijnen.
Zonder deze port is B11 een amputatie. Het bestaande zijkanaal-patroon
(`Loaded#rolBindingen`, dat de vouw-`bronRol` en de
objectcreatie-`verbindendeRol` al draagt) is de gebaande weg; de
recordtypen zijn gedeeld met de frontend (build-helper), dus de velden
zijn er al.

## Acceptatie

- (a) Beide Drools-takken lezen de navigatiebinding van de wire; de
  her-afleiding uit `navigatieRol`/`navigatiePad` vervalt in de
  beslissende rol (mag hooguit als kruiscontrole blijven die hardop
  faalt bij verschil — kies en motiveer).
- (b) De leesproef rood-eerst: een IR-mutatieproef die de
  wire-binding twist en aantoont dat het GEDRAG (of een benoemde
  fout) de wire volgt — vóór de port wordt dezelfde twist genegeerd.
  Uitgevoerd en teruggedraaid met build-rc in één keten.
- (c) Gelijkheidsbewijs: de volledige corpussweep over beide takken
  speelt identiek; gate 12-pins (EXPECTED_PLAYED /
  EXPECTED_DRL_PLAYED) bewegen NIET.
- (d) Alle veertien gates groen; wire en schema ongewijzigd (dit is
  een consumer-port, geen wire-wijziging).

May touch: `backends/drools/**`, kaartverplaatsing. NB: serieel met
andere Drools-kaarten (P2, P3, unie-telling).
