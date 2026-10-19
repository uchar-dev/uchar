# clingo-vouw-bodemzinnen: niet-numerieke vouwen weigeren op een valdoorarm

Gemeten door de temporele-datumvouw-kaart (2026-09-23, zusterarm-
meting d): een DATUM-TIJD- of TEKST-getypeerde tijdlijn of setbron
onder een optellende vouw geeft in clingo geen fout getal — maar de
weigering komt uit de valdoorarmen `dt_term` en `symbolic_term`, en
dat zijn bodemzinnen: dezelfde zin voor elke vorm die de arm vangt,
geen benoeming van het construct. Gate 8 verbiedt zo'n zin als pin,
dus deze vormen kunnen geen rode getuige krijgen tot ze een eigen
benoemde arm hebben. De `Datum_in_dagen`-vormen kregen die arm bij de
datumvouw-kaart (`refuse_date_timeline_total`); dit kaartje trekt de
zusters gelijk.

Aanvulling uit de datumvouw-lens (2026-09-23): ook MAXIMUM, MINIMUM,
EERSTE, LAATSTE en SOM over de eigen datumtijdlijn zijn
verifier-schoon en weigeren in ref én clingo uitsluitend met een
bodemzin (clingo: "expression kind not translated (no corpus case
exercises it)"). Geen fout getal, wel dezelfde onbenoemde klasse —
meenemen in de meting.

## Acceptatie (bij oppakken aanscherpen)

- (a) Meet per vorm (datum-tijd-tijdlijn, tekst-tijdlijn, de
  setbron-tegenhangers, en de vouwsoorten MAXIMUM/MINIMUM/EERSTE/
  LAATSTE/SOM over de eigen datumtijdlijn) de vijf uitvoerders; leg
  vast welke valdoorarm vandaag vangt.
- (b) Elke vorm krijgt een benoemde, fase-dragende weigerzin die het
  construct en de namen noemt (huispatroon van
  `refuse_date_timeline_total`); rood-eerst met mutatieproef.
- (c) Eén rode getuige per arm (`expectedFault {ref, clingo}`;
  Drools-been met reden in de gate-8-lijst — voorstel).
- (d) Alle veertien gates groen; pins bewegen niet.

May touch: `backends/clingo/**`, `corpus/red/**`,
`scripts/check-red.sh` (**voorstel**, telling/lijst),
kaartverplaatsing. NB: serieel met andere translate.ml-kaarten.
