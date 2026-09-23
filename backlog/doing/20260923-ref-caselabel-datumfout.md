# ref-caselabel-datumfout: de misvormde inputdatum meldt veld noch fase

Gemeten door de ref-geldigheid-kaart (2026-09-23, op
jutterswet-bergingsklok): een misvormde `date`-waarde in de
case-INPUT wordt luid geweigerd maar verkeerd gelabeld —
`EVALUATION FAULT: malformed date lexeme "10-03-2026"` zonder
attribuutnaam en zonder laadfase, waardoor de eigen
`malformed %s date`-checks van de caselader onbereikbaar zijn. Nooit
een stil fout antwoord, wel een melding die de auteur laat zoeken.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproduceer; traceer waarom de laderzin onbereikbaar is (welke
  lezer wint) en welke posities hetzelfde hebben.
- (b) De weigering draagt veld, waarde en fase (laadtijd); de dode
  laderchecks gaan werken of verdwijnen (fail-fast, geen dode armen).
  Rood-eerst suitegetuige; mutatieproef.
- (c) Corpussweep: geen replay beweegt; alle veertien gates groen.

May touch: `ref/**`, kaartverplaatsing. NB: serieel met andere
ref-caselaagkaarten.

## Acceptatie, aangescherpt bij de claim (2026-09-23)

- (a) Reproductie op `jutterswet-bergingsklok` case2 (één datum
  dag-eerst) in ref en clingo, letterlijk. Trace: welke lezer wint en
  waarom de `malformed %s date`-armen van de caselader niet vuren.
  Inventaris van ELKE positie in het casebestand waar een datum- of
  datumtijdlexeem binnenkomt — attribuutwaarde, tijdlijnranden en
  -segmentwaarden (attribuut, kenmerk, parameter), parameterwaarde,
  lijst-element, structveld, constructorpayload, verwachte waarde en
  verwachte tijdlijn, asOfDate — als tabel: positie, lezer, zin, fase.
- (b) Elke positie weigert bij het laden als `CASE ERROR` (exit 2) met
  positie (pad of attribuutnaam), lexeem en de reden van de lezer; de
  dode armen verdwijnen of gaan werken. Rood-eerst suitegetuigen in
  `ref/test/test_ref.ml` (via de gedeelde caselader), de hele zin
  gepind; mutatieproef (label terug ⇒ rood ⇒ herstel, `cmp`).
  HARDE RANDVOORWAARDE: de asOfDate-weigerzin (misvormd en
  kalender-ongeldig) blijft byte-gelijk aan main, vóór en na gemeten
  met `cmp`; bestaande tests blijven ongewijzigd.
- (c) Sweep ref en clingo over alle golden- en roodcases vóór/na
  identiek; gates 5, 6 en 8 identiek in getallen; alle veertien gates
  groen, elk los. Clingo's caselezing gemeten (deelt hij de zinnen of
  heeft hij eigen?) zonder `backends/clingo` aan te raken.

May touch (aangescherpt): `ref/lib/case.ml`, zo nodig `ref/lib/value.ml`,
`ref/test/test_ref.ml` (alleen toevoegen), deze kaart. Niet:
`ref/lib/eval.ml`, `backends/**`, `frontends/**`, `spec/**`, `corpus/**`,
`scripts/**`.
