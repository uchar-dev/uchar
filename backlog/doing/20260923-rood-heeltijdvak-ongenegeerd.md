# rood-heeltijdvak-ongenegeerd: de tweeling die de nieuwe weigering pint

Voorstel uit de heeltijdvak-leegtest-kaart (2026-09-23): een rode
tweeling van `corpus/rood/leegnegatie-heel-tijdvak` met
`negatie: false` zou de weigering van de ONgenegeerde
heel-tijdvak-leeg/gevuld-test blijvend in alle drie de uitvoerders
bewaken (ref en clingo weigeren met de ch.-3.9-zin, de Drools-loader
sinds die kaart met `Predicaat.periode`). Vandaag pint alleen de
JUnit-getuige de Drools-kant en de suite de OCaml-kant. Micro-kaart:
document + NOTE + check-rood-Drools-regel (**voorstel**).

## Acceptatie

- (a) Het rode document, rood-eerst tegen een jar zonder de
  loaderweigering (die speelde); gate 8 telt hem met beide fragmenten.
- (b) Alle veertien gates groen; geen pinbeweging.

May touch: `corpus/rood/**`, `scripts/check-rood.sh` (**voorstel**),
kaartverplaatsing.

## Acceptatie (aangescherpt bij de claim, 2026-09-23)

Branch `card/rood-duo`, samen met `rood-asofdate-kalender`.

- (a) `corpus/rood/leegtest-heel-tijdvak`: de tweeling van
  `leegnegatie-heel-tijdvak` met `negatie: false` (de `bron`-zin
  zonder "niet"), dezelfde case. NOTE met de handberekening volgens
  ch. 3.9 en per uitvoerder de reden van de weigering. ref en clingo
  pinnen de ch.-3.9-zin; `ref --bodemzin` tegen de lijst van de gate
  geeft geen HIT.
- (b) Rood-eerst: de jar van `8b685402` (basis van de
  heeltijdvak-leegtest-kaart, vóór `38bd97f0`) via `git archive` in
  /tmp gebouwd met `mvn -o package`. Daar geeft DroolsRoodMain NOFAULT
  en spelen beide takken een groene kopie met de handwaarden. De
  huidige jar geeft in beide takken EXCLUDE `Predicaat.periode`.
- (c) Drools-been: een `drools`-fragment kan alleen op FAULT. Eerst
  aantonen dat de gate een fragment op deze EXCLUDE weigert, dan één
  regel in de uitsluitingslijst (**voorstel**, beschermd pad).
- (d) Gates 1–14 groen, elk los. Gate 8 telt het document. Gate 12
  veegt alleen `corpus/golden`, dus de pins 181/160 bewegen niet.
