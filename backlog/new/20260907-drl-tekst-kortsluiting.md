# De DRL-tekst-tak krijgt de getrouwe ch.-5.10-kortsluiting

Goal: De slapende DRL-TEKST-tak van de Drools-backend
(`DrlTranspiler`) vertaalt de telkwantoren als een TEKSTUELE
k-deelverzameling-vouw: `ten hoogste d` wordt
`!(minstens(leden, d+1))`, `ten minste d` wordt een OR over alle
d-deelverzamelingen. Java's `&&`/`||` geven die vouw *een* luiheid,
maar niet DE luiheid die `RUNTIME-SEMANTICS.md` ch. 5.10 voorschrijft.
Deze kaart geeft de tak de getrouwe vorm: de leden worden in
wire-volgorde gelezen en de evaluatie stopt op precies de
triggertabel van ch. 5.10.

**De gemeten klasse (2026-09-07) — let op, dit is NIET wat de
bouwronde-kaart aanvankelijk beweerde.** De divergentie is exact
`TEN_HOOGSTE` met **drempel ≥ ledental**, en uitsluitend daar:

* `minstens(leden, k)` met `k` BOVEN het ledental expandeert naar het
  letterlijke `false`, dus `!(false)` is `true` en er komt **geen enkel
  lid** in de DRL. De tak implementeert daarmee precies de
  vacuüm-waar-richting die ch. 5.10 grens 1 uitsluit: hij ANTWOORDT waar
  ref, clingo én de exec-model-tak FAULTEN.
* Uitputtend gemeten over n ≤ 6: 636 combinaties, waarvan 258 met
  d < n — die zijn het allemaal eens. De andere vier kwantoren
  divergeren nooit; waarheidswaarden kloppen overal; de GEVAARLIJKE
  richting (een lid lezen dat de norm overslaat) is dicht.
* Getuige: een `TEN HOOGSTE 1`-samengestelde over ÉÉN lid dat door nul
  deelt. ref `EVALUATION FAULT: division by numeric zero`, clingo
  `UNSATISFIABLE … viol("division by numeric zero")`, Drools-exec
  `ERROR ConstraintEvaluationException`, Drools-DRL-tekst: schrijft `1`.

**Correctie op de bouwronde-Results.** Die noemden als voorbeeld "TEN
HOOGSTE 1 over drie leden waarvan het eerste faalt" en beschreven de
tak als *luier dan of gelijk aan* ch. 5.10. Beide zijn onjuist: dat
voorbeeld divergeert zelf niet, en "luier" is de verkeerde richting —
de tak is niet luier maar **onjuist waar**, want hij past een
stoptrigger toe die de norm niet kent.

Vandaag is de klasse corpus-onbereikbaar (0 goldens dragen een
at-most-samengestelde met drempel ≥ ledental) en dat feit staat
reden-dragend, falend-wanneer-stale geregistreerd in
`scripts/check-reftype-derivation.sh` (gate 13).

Acceptance:
- De tak leest de leden in wire-volgorde en stopt op de ch.-5.10-
  triggertabel; vorm vrij, maar een tekstexpansie kan het niet — het
  wordt vermoedelijk een runtime-helper over lid-*suppliers*
  (`RegelSpraakRuntime`), zodat de stop in Java gebeurt en niet in de
  gegenereerde tekst.
- De getuige hierboven (TEN HOOGSTE 1 over één faultend lid) faultet
  ná de kaart óók in de DRL-tekst-tak — additieve test in de
  backend-JUnit-suite.
- Een golden mét een at-most-samengestelde waarvan de drempel het
  ledental dekt landt in `corpus/golden/`, zodat de klasse
  corpus-BEREIKBAAR wordt; de registratie in
  `scripts/check-reftype-derivation.sh` verdwijnt daarmee (de scan die
  vandaag faalt-wanneer-stale is, hoort dan groen te zijn omdat de tak
  klopt, niet omdat de vorm ontbreekt).
- Alle dertien gates groen.

May touch: `backends/drools/**` (DrlTranspiler + runtime + tests),
`corpus/golden/**` (de nieuwe getuige), `scripts/check-reftype-derivation.sh`
(de registratie opheffen), `scripts/check-drools.sh` (tellingen), `backlog/`.

Out of scope: de exec-model-tak (draagt ch. 5.10 al), ref, clingo,
`spec/RUNTIME-SEMANTICS.md` ch. 5.10 zelf (de norm staat; dit is een
consument die hem nog niet haalt), `toka/`.
