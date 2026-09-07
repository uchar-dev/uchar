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

**De gemeten klasse (2026-09-07, gecorrigeerd na lens 2).** De
divergentie beslaat **heel `TEN_HOOGSTE`**, niet alleen de rand:

* Uitputtend gemeten over n ≤ 6: **636 combinaties DIVERGEREN**, waarvan
  **258 met d < n** — d < n is dus het leeuwendeel van de gevaarlijke
  klasse, niet het veilige gebied. (Een eerdere formulering las 636 als
  "gemeten combinaties waarvan 258 het eens zijn"; dat was precies
  omgekeerd.)
* De oorzaak is de TEKSTUELE expansie: `ten hoogste d` wordt
  `!(minstens(leden, d+1))`, een OR over (d+1)-deelverzamelingen.
  Java's `||`/`&&` geven die expansie een eigen luiheid die NIET die van
  ch. 5.10 is — hij kan een lid overslaan dat de norm wél leest. Bij
  `k` boven het ledental expandeert `minstens` bovendien naar het
  letterlijke `false`, en dan komt er geen enkel lid in de DRL.
* Het waarneembare gedrag is één en hetzelfde in de hele klasse: de tak
  **ANTWOORDT waar de norm FAULT**. Waarheidswaarden kloppen overal; de
  andere vier kwantoren divergeren nooit.
* Kleinste end-to-end getuige: `TEN HOOGSTE 1` over TWEE leden, lid 1
  onwaar, lid 2 deelt door nul. ref `EVALUATION FAULT: division by
  numeric zero`, clingo `UNSATISFIABLE … viol("division by numeric
  zero")`, Drools-exec `ERROR ConstraintEvaluationException`,
  Drools-DRL-tekst: schrijft `1`.

**Correctie op de bouwronde-Results.** Die noemden als voorbeeld "TEN
HOOGSTE 1 over drie leden waarvan het eerste faalt" en beschreven de
tak als *luier dan of gelijk aan* ch. 5.10. Beide zijn onjuist: dat
voorbeeld divergeert zelf niet, en "luier" is de verkeerde richting —
de tak is niet luier maar **onjuist waar**, want hij past een
stoptrigger toe die de norm niet kent.

Een golden kan de divergentie zelf niet DRAGEN: de divergerende gevallen
zijn precies die waar de norm FAULT, en een case waar de norm fault is
geen golden. `scripts/check-reftype-derivation.sh` (gate 13) draagt
daarom een reden-dragende NAAMLIJST over elke TEN_HOOGSTE-drager in
`corpus/golden` én `corpus/rood` — een waakhond over de VORM, die faalt
bij een ongelijste drager én bij een stale regel. Vandaag staan er drie
dragers op.

Acceptance:
- De tak leest de leden in wire-volgorde en stopt op de ch.-5.10-
  triggertabel; vorm vrij, maar een tekstexpansie kan het niet — het
  wordt vermoedelijk een runtime-helper over lid-*suppliers*
  (`RegelSpraakRuntime`), zodat de stop in Java gebeurt en niet in de
  gegenereerde tekst.
- De getuige hierboven (TEN HOOGSTE 1 over één faultend lid) faultet
  ná de kaart óók in de DRL-tekst-tak — additieve test in de
  backend-JUnit-suite.
- De naamlijst in `scripts/check-reftype-derivation.sh` verdwijnt: zodra
  de tak ch. 5.10 haalt is er geen vorm meer om te bewaken. Tot dan
  groeit de lijst mee met elke nieuwe TEN_HOOGSTE-drager.
- Alle dertien gates groen.

May touch: `backends/drools/**` (DrlTranspiler + runtime + tests),
`corpus/golden/**` (de nieuwe getuige), `scripts/check-reftype-derivation.sh`
(de registratie opheffen), `scripts/check-drools.sh` (tellingen), `backlog/`.

Out of scope: de exec-model-tak (draagt ch. 5.10 al), ref, clingo,
`spec/RUNTIME-SEMANTICS.md` ch. 5.10 zelf (de norm staat; dit is een
consument die hem nog niet haalt), `toka/`.
