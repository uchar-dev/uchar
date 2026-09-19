# clingo-som-catchall: de bodemzin is live onwaar op de expressiepositie

Lensvondst (clingo-onopgelost-lens, 2026-09-19, pre-existent): de
expressie-catch-all in `backends/clingo/lib/translate.ml` (~r. 3561)
luidt `expression kind not translated (no corpus case exercises it)` —
en `corpus/rood/som-fallback-always-evaluated/case1.json` pint exact
dat bericht als zijn clingo-fragment. Er ís dus een corpusgeval dat de
zin uitlokt: precies de onwaarheid waarvoor de conditiepositie zojuist
is behoed, live op de expressiepositie. De NOTE van die rode case
noemt het gedrag zelf "an honest refusal"; alleen de zín liegt.

Twee routes, te wegen bij oppakken: (a) de eenregelige
eerlijkheidscorrectie — de bodemzin herformuleren tot een weigering
zonder de valse geen-corpusgeval-claim (raakt het gepinde fragment van
de rode case: die pin beweegt mee, gemandateerd binnen de kaart); of
(b) de listed-operand-som een eigen arm geven zodat de bodem weer
onbereikt is. (a) is een uur; (b) is de structurele sluiting.

## Acceptatie (bij oppakken aanscherpen)

- (a)/(b) gekozen met reden; rood-eerst; bij (a) beweegt het gepinde
  fragment van `som-fallback-always-evaluated` mee in dezelfde commit;
  suite-getuige + mutatieproef; alle veertien gates groen.

May touch: `backends/clingo/**`, `corpus/rood/som-fallback-always-
evaluated/**` (fragment-pin), kaartverplaatsing.
