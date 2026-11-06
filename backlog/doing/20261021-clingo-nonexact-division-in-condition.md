# clingo-nonexact-division-in-condition: clingo weigert een kale niet-exacte deling in een voorwaarde als EF-006

*Vondst van de kaart `clingo-day-kind-walk-after-fold-stop` (2026-10-21), gemeten met een proef in een scratchpad: de
matrix van `jutterswet-keurgemiddelde` zonder de afronding, lid W2 met 7 / 2. Luid: clingo is UNSATISFIABLE waar ref een
antwoord geeft; geen stil fout antwoord. Geen golden draagt de vorm.*

## Aanleiding

Een kale deling die niet exact is (7 / 2) in een voorwaarde geeft in de clingo-backend
`viol("non-exact division (an explicit rounding is mandated before precision is lost)", …)` met de code EF-006, zowel
voor een lid na de stop van een kwantorvouw als voor een bereikt lid. ref vergelijkt het exacte rationale getal en
schrijft niets, dus speelt beide. EF-006 is "a written value does not fit the declared decimals of its target"
(RUNTIME-SEMANTICS ch. 5.7b, ch. 2.4): hier wordt niets geschreven, dus draagt de weigering een code die niet past. Het
bereik van de vouw (`param_reach`) zou alleen de helft na de stop dichten.

## Doel

De clingo-backend vergelijkt een kale deling in een voorwaarde exact, zoals ref, of weigert haar met een eigen
`CLINGO-###`-code (register `backends/clingo/codes.tsv`, gate 8) in plaats van EF-006; de kaart kiest op wat de
vertaling toelaat en zegt waarom. Een deling die een waarde schrijft, houdt EF-006.

## Acceptatie

- (a) Rood eerst: een golden met een kale niet-exacte deling in een voorwaarde (en, als de keuze exact vergelijken is,
  in de matrix van een kwantorvouw na de stop), rood in clingo vóór de fix, groen in ref en Drools; of, bij de keuze
  weigeren, de vorm in de divergentielijst van gate 8 met de nieuwe code.
- (b) Gates 5, 6, 8, 12 en 17 groen; de eigen suite van de clingo-backend groen.

May touch: `backends/clingo/`, één golden of een divergentie-entry, deze kaart.

## Result

Gebouwd op branch `card/creation-condition-and-clingo-cleanup`. Logs onder `uchar-dev/werk/clingo-cleanup/logs/`.

Keuze: weigeren, niet exact vergelijken. Deze codering houdt elke term als geheel getal op één schaal; het quotiënt
van een deling in een voorwaarde staat op de schaal van zijn operanden (`max sl sr`), waar 13 / 2 van twee gehele
getallen niet op past en 20 / 3 op geen enkele schaal. Exact vergelijken vraagt kruislings vermenigvuldigen op elke
rekenpositie waar de deling kan staan (ook onder een `plus` of een `Let`); dat is een andere codering, geen arm. De
deling in een voorwaarde wordt vergeleken en nooit geschreven, dus EF-006 (de fout van een schrijfactie) past niet;
ze krijgt een eigen grens, `CLINGO-039` (soort `limit`, sleutel `non-exact-division-in-condition`, zonder parameters,
`backends/clingo/codes.tsv`), en een exacte deling in een voorwaarde speelt zoals eerst. Een deling die een waarde
schrijft, houdt EF-006 (of CLINGO-031 binnen een waarde).

- Bouw: `translate.ml` krijgt een veld `in_condition` op de context, dat `cond_atoms` en `value_condition` aanzetten
  voor de duur van de voorwaarde; `translate_division` kiest daar de zin `non_exact_division_in_condition`, en
  `viol_code` leest die als CLINGO-039.
- (a) Golden `corpus/golden/jutterswet-keurquotient` (eigen Jutterswet-casus, IR van de RegelSpraak-frontend, twee
  cases met de quotiënten 6,5, 5,9, 7, 6,25 en 6): rood vóór de bouw, `EF-006: clingo: UNSATISFIABLE … non-exact
  division (an explicit rounding is mandated …)` op beide cases, ref en beide Drools-takken spelen
  (`logs/k2-golden-before.log`); na de bouw `CLINGO-039` op beide cases (`logs/k2-golden-after.log`). Gate 6:
  `EXPECTED_CHANNEL` krijgt `jutterswet-keurquotient|CLINGO-039|…`. Suite: frame `a non-exact division in a condition`
  op de matrix van `jutterswet-keurgemiddelde` zonder afronding, een lid na de stop en een bereikt lid, met ref's
  oordeel ernaast en `faults_at_solving ~code:CLINGO-039`; rood op de oude `translate.ml` (2 FAILURES, EF-006,
  `logs/k2-suite-old-translate.log`), groen 2732 checks (`logs/k2-suite-2.log`).
- (b) Gates 1, 2, 4, 5, 6, 7, 8, 12, 13, 17 (`logs/k2-gates.log`, per gate `logs/k2-check-*.log`). Gate 12:
  `EXPECTED_PLAYED` +1 (na de rebase op main `575de08e5`: 352 → 353; de golden speelt op beide takken, PLAYED 2).
- Spec: één zin in `spec/ASP-TRANSLATION.md` bij de niet-exacte deling (beschermd pad, mandaat voor deze ronde).

Voor `CLAUDE.md` (hoofdsessie): de beschrijving van gate 6 noemt twee goldens in `EXPECTED_CHANNEL`; nu zijn het er
drie, met `jutterswet-keurquotient` (CLINGO-039).
