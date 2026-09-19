# drl-objectcreatie-defect: de creatie-lowering van de DRL-tekst-tak

Gemeten spanning (drl-takteller, 2026-09-19): vier goldens met precies één
`objectCreatie` spelen wel in het exec-kanaal en niet in de DRL-tekst-tak,
zonder dat die tak ze bij naam weigert. `anchor/jutterswet-rekenkamer` en
`jutterswet-rekenkamerheffing-catala` antwoorden leeg waar ref, clingo en
exec 70 / 160 / -15 / 60 geven — de toekenningen van de creatie landen
niet. `anchor/jutterswet-rekenkamer-overzee` en `synth3-kamerheffing`
vallen bij het vuren om met een reflectieve `NullPointerException` in de
consequence van de creatieregel. Sinds de takteller staan ze gepind in
`DRL_CHANNEL` (klasse D, kanalen `FAIL` en `ERROR`), dus repareren beweegt
die lijst en `EXPECTED_DRL_PLAYED` met vier omhoog.

De luide helft van hetzelfde gat staat in klasse C: `anchor/jutterswet-wachtersketen`
en `synth2-budgetgrens-onder` dragen een objectcreatie-MARKER op een
onopgeloste terugkoppeling naar Wachter, en worden door exec gewoon
gespeeld (PLAYED 2 en PLAYED 1). Repareren van die kant hoort bij de
koppelloos-kaart hieronder, niet bij deze.

NB serieel met `20260919-drl-creatie-koppelloos.md` (beide raken
`DrlTranspiler.objectCreatie`). Niet samenvoegen: het zijn disjuncte
codepaden, disjuncte goldens en disjuncte symptomen. De vier goldens van
deze kaart nemen de `rolNaarSubject == null`-tak niet — hun terugkoppeling
lóst op en het misgaan zit bij het vuren; de goldens die die tak wél nemen
staan in klasse C als `EXCLUDE`.

## Acceptatie (bij oppakken aanscherpen)

- (a) De `NullPointerException` bij het vuren is weg, of is een luide
  weigering per vorm.
- (b) De twee leeg-antwoordende goldens spelen met de gepinde waarden, of
  hun afwijking draagt een reden die zegt waarom dit v0-semantiek is.
- (c) `DRL_CHANNEL` en `EXPECTED_DRL_PLAYED` bewegen mee; de exec-pins
  bewegen niet; alle veertien poorten groen.

May touch: `backends/drools/` (`DrlTranspiler` en zijn eigen suite),
`scripts/check-drools.sh` (voorstel — beschermd pad), kaartverplaatsing.
