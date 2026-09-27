# scenario-parametersets: een benoemde parameterset met jaargangen in het scenariodocument, gekozen door de peildatum

De oude toolchain kende parametersets als zijbestand naast een scenario of
een dienst-descriptor (`<naam>.parameters.json`: benoemde sets, per
parameter waarden met een geldigheid `vanaf`/`tot`, gevalideerd tegen het
opgeloste model; de dienst kiest per verzoek de jaargang aan de hand van de
peildatum, en een parameter die zowel in de descriptor-defaults als in een
set staat is een startfout). De dienst in uchar-dev/regelspraak-toolchain
draagt dat nog (`ParametersetLader`, `BeslisserKern`,
`ParametersetServiceTest`); de scenarioroute verloor het bij de
verhuizing, omdat uchars scenariolaag elke gelezen parameter in de situatie
eist en van een JSON-zijbestand niets weet (benoemd verlies in de README
van de distributie, 2026-09-26). Het idee is goed en hoort spec-eerst hier
(maintainer, 2026-09-27): bedragen en percentages veranderen per jaar, los
van de regels, en een scenario moet "in 2025" en "in 2026" tegen dezelfde
regels kunnen draaien. Herkomst: ALEF's testsets dragen parameterwaarden
als testdata, niet als regeltekst.

Wat uchar al heeft (`frontends/regelspraak/grammar/DIVERGENCES.md`,
"Scenariotoetsen"): een situatie draagt parameterwaarden, met een periode
`van <datum> tot <datum>` alleen voor een tijdafhankelijke parameter; een
situatie kan `gebaseerd op de situatie <naam>` zijn; een scenario heeft
`met peildatum <datum>`; het caseformaat draagt per parameter een waarde of
een hele tijdlijn (`spec/schema/uchar-case-v0.schema.json`, `parameters`).
Een parameter zonder granulariteit heeft in de IR één waarde; de jaargang
is dus geen IR-vraag maar een vraag van de lowering: welke waarde het
casebestand krijgt.

## U-besluiten (hoofdsessie, 2026-09-27)

- U1 Oppervlak: een documentblok `Parameterset <naam>` naast de
  situatie-definities, met regels in de vorm van de situatie
  (`de parameter <naam> is <waarde>`, optioneel `van <datum> tot <datum>`;
  meerdere regels voor dezelfde parameter zijn jaargangen, hun perioden
  mogen elkaar niet overlappen). Een scenario kiest een set met
  `met parameterset <naam>` (of een situatie met `gebaseerd op de
  parameterset <naam>` — kies bij het oppakken één van de twee en leg de
  reden vast; de grammatica is de uchar-grammatica, de extensie staat in
  DIVERGENCES).
- U2 Lowering: voor een parameter zonder granulariteit schrijft de lowering
  de waarde waarvan de periode de peildatum van het scenario bevat (de
  jaargang; precies één, anders een weigering in de zin van de resolver);
  voor een tijdafhankelijke parameter de hele tijdlijn, zoals de situatie
  dat nu al doet. Een scenario zonder peildatum dat een parameter met
  jaargangen leest, wordt geweigerd met een zin die de parameter en het
  scenario noemt. De IR verandert niet.
- U3 Conflictregel (als de dienst): een parameter die zowel in de gekozen
  set als in de situatie van het scenario een waarde heeft, is een
  weigering — nooit "de laatste wint". Een parameter die in geen van beide
  staat, blijft de bestaande weigering van de parameterpoort; daarmee heeft
  het open punt "a scenario has no surface for an absent parameter" (PLAN
  §9) een oppervlak: de set.
- U4 Getuigen: een Jutterswet-golden met `scenario.rgs` dat één set met
  twee jaargangen draagt en twee scenario's met verschillende peildatum,
  waarvan de verwachte uitkomsten verschillen door de jaargang alleen;
  gate 16 regenereert de casebestanden byte-gelijk; gates 5, 6 en 12 spelen
  ze (scalaire parameters dragen alle drie de uitvoerders). Negatieve
  getuigen in de frontendsuite: overlappende jaargangen, geen jaargang op
  de peildatum, conflict met de situatie, scenario zonder peildatum.
- U5 Register: CONSTRUCTS-rij van de scenariotoetsen bijwerken (voorstel in
  het Result; de hoofdsessie landt hem), DIVERGENCES "Scenariotoetsen"
  uitgebreid, PLAN §9 het open punt afgesloten.
- U6 Niet in deze kaart: de dienst van de distributie (die leest de set
  later uit hetzelfde document in plaats van uit JSON — een kaart daar,
  alleen benoemen), tijdlijnen voor een parameter zonder granulariteit in
  de IR, en de Drools-weigering van tijdgranulaire parameterinvoer (gate
  12, bekend).

## Acceptatie

U1 tot en met U4 met de getuigen; alle zestien gates groen; het Result
noemt de gekozen syntaxis met reden, de weigeringszinnen letterlijk uit
echte uitvoer, en de tellingen van gate 16.

Mag raken: `frontends/regelspraak/` (resolver, `ScenarioResolver`,
`CaseEmitter`, grammatica als de extensie dat vraagt, DIVERGENCES, tests),
`corpus/golden/` (nieuwe Jutterswet-golden), `scripts/check-scenario.sh`
alleen voor een vloer. Niet: `spec/IR-SPEC.md`, `spec/schema/`, de
uitvoerders, `toka/`.
