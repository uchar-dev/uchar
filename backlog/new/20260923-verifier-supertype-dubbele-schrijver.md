# verifier-supertype-dubbele-schrijver: gewone regels op super- én subtype voor één doel, zonder defaults-groep

Bijvangst van de onderzoekskaart dialect-defaults-niet-en-nesting (gemeten,
oorzaak toen niet uitgezocht); het voorstel-concept
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` (§9, kaartbranch) geeft
de wortel: de verifier neemt in WF-079 aan dat een kale regel van het
subtype die van het supertype vervangt (`spec/verifier/lib/checks.ml:8899–8903`),
terwijl RUNTIME-SEMANTICS ch. 7.10 een kale regel alleen in een stapel
vouwt wanneer de keten een defaults-groep declareert
(`RUNTIME-SEMANTICS.md:5179–5184`; `spec/verifier/lib/desugar.ml:286–290`).
Gemeten: de verifier zegt niets; ref en de Drools-exec-tak vinden geen
fixpunt; clingo geeft twee waarden; de DRL-teksttak antwoordt naar
bronvolgorde. Vier uitvoerders, drie antwoorden, en de spec beslist het
geval niet.

## Te beslissen bij oppakken (spec-vraag, beschermd pad)

- (a) De stapelvervanging van ch. 7.10 geldt ook voor kale regels: de kale
  regel van het subtype vervangt die van het supertype op instanties van
  het subtype (het Flora-beginsel dat SUBTYPING §4.4 al voor blokken
  volgt). Dan: ref, clingo en beide Drools-takken implementeren het, en
  een ankercase pint het.
- (b) Twee kale schrijvers op één doel in één keten zijn ill-formed: WF-079
  (of WF-026 op de effectieve stapel, IR-SPEC ch. 6.1) weigert, met een
  negatief; de uitdrukbare vorm is een defaults-groep (basis op het
  supertype, eigen basis op het subtype), die ch. 7.10 al draagt.

Aanbeveling: (b) — het is de zin die IR-SPEC ch. 6.1 al uitspreekt ("the
single-writer rules … are evaluated on the per-type EFFECTIVE stack"), en
(a) voegt een tweede vervangingsmechanisme naast de defaults-groep toe.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproductie op alle vier uitvoerders in de kaart vastgelegd (het
  M7-document van het voorstel of een eigen rechtenvrije variant).
- (b) De gekozen weg: bij (b) een negatief `wf079-…` of `wf026-…`
  (**voorstel**), de verifier rood-eerst, en de spec-zin die het geval
  benoemt (**voorstel**); bij (a) de spec-zin, drie uitvoerders en een
  ankercase, allen exact.
- (c) Alle veertien gates groen.

May touch: `spec/verifier/**`, `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `corpus/negatief/**` (alle **voorstel**); bij
(a) `ref/**`, `backends/**`, `corpus/golden/anchor/**` (**voorstel**);
kaartverplaatsing.

Niet in deze kaart: de dialectvormen.
