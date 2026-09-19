# clingo-onopgelost-conditie: WF-044 ook op de conditiepositie

De ASP-backend citeert WF-044 op de EXPRESSIE-positie (de
`Onopgelost`-arm in `backends/clingo/lib/translate.ml`, ~r. 3486) maar
laat een `Conditie_onopgelost` in de generieke bodem vallen:
`condition kind not translated (no corpus case exercises it)`. `ref`
heeft de arm wél (`ref/lib/eval.ml`: "Onopgelost condition reached in
evaluation (WF-044)"). Daardoor kan de Onopgelost-wacht geen rode
getuige op de guard-positie dragen: een `expectedFault.clingo` zou een
boodschap pinnen die beweert dat geen corpusgeval haar uitlokt, terwijl
de nieuwe case precies dat zou doen (gemeten,
`20260919-exec-stille-guardflanken` §c).

## Acceptatie (bij oppakken aanscherpen)

- (a) `backends/clingo/lib/translate.ml` krijgt een
  `Conditie_onopgelost`-arm die de WF-044-fout werpt zoals de
  expressie-arm dat doet; suite-getuige + mutatieproef.
- (b) Daarna `corpus/rood/wf044-onopgelost-guard/` met
  `expectedFault {ref, clingo}` op de §13.4.7-guard bij
  `doel == anker`, plus de reden-dragende Drools-regel (categorie
  EXCLUDE — beide takken weigeren de vorm per construct sinds
  2026-09-19). Gate 8 gaat 28 → 29, vloer 26 blijft; de
  `check-rood.sh`-regel is een beschermd-padvoorstel met datum + reden.
- (c) Alle veertien gates groen; corpussweep beide Drools-takken: nul
  kanaalwisselingen.

May touch: `backends/clingo/**`, `corpus/rood/**`,
`scripts/check-rood.sh` (voorstel), kaartverplaatsing.
NB: serieel met andere `translate.ml`-kaarten
(kruis-stelsel-eenheid, asp-ch102-vervolg — zelfde bestand).
