# rood-asofdate-kalender: de kalenderhelft van de asOfDate-weigering pinnen

Voorstel uit de ref-geldigheid-kaart (2026-09-23): het case-schema
weigert een dag-eerst-`asOfDate` maar laat een kalender-ongeldige
door (`2026-13-01`, `2026-02-30` — het patroon toetst de cijfers,
niet de kalender). Sinds die kaart weigert de caselader zulke waarden
benoemd (CASE ERROR, exit 2) en clingo volgt via de gedeelde module;
de weigering is alleen suite-gepind. Een ROOD document met een
kalender-ongeldige asOfDate zou hem corpusbreed pinnen — meet eerst
hoe gate 8's Drools-been een Drools-ERROR op een case telt (de
Drools-takken weigeren de waarde ook, maar met hun eigen zin).

## Acceptatie (bij oppakken aanscherpen)

- (a) Het rode document (`expectedFault {ref, clingo}` met de
  laderzin), rood-eerst tegen de oude lader (die stil antwoordde);
  de Drools-uitsluitings- of fragmentkant van check-rood gemeten en
  zo nodig als **voorstel**.
- (b) Weeg of het schemapatroon de kalender kan leren (gate 2/4,
  spec/schema is **voorstel**) — dan verschuift de getuige naar
  negatief-achtig terrein; motiveer de keuze.
- (c) Alle veertien gates groen; pins bewegen niet.

May touch: `corpus/rood/**`, `scripts/check-rood.sh` (**voorstel**),
`spec/schema/**` (**voorstel**), kaartverplaatsing.

## Acceptatie (aangescherpt bij de claim, 2026-09-23)

Branch `card/rood-duo`, samen met `rood-heeltijdvak-ongenegeerd`.

- (a) `corpus/rood/asofdate-kalender`: `ir.json` is het document van
  golden `jutterswet-vensterheffing` byte voor byte (twee
  geldigheidsvensters, dus de datum doet ertoe), de case heeft
  `asOfDate` `2026-02-30`. Gemeten: gate 4 laat de waarde door. ref en
  clingo weigeren bij het laden. Gepind wordt de volle laderzin, geen
  bodemzin.
- (b) Rood-eerst tegen de lader van `cc0e6a1b` (basis van de
  geldigheidskaart, vóór `6ff39603`): `git archive` naar /tmp,
  `dune build`, en laten zien dat de oude runners stil antwoorden.
- (c) Drools-been gemeten met DroolsRoodMain en met beide takken. Bij
  een FAULT met een eigen zin wordt het fragment gepind. Bij een ERROR
  volgt een open punt en geen uitsluiting.
- (d) Schemaweging gemotiveerd. `spec/schema` blijft ongewijzigd,
  tenzij er een dwingende reden is (dan als voorstel).
- (e) Gates 1–14 groen, elk los. De pins bewegen niet.
