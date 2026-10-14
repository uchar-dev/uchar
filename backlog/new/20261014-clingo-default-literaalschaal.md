# clingo-default-literaalschaal: clingo weigert een default die een literal met een kleinere schaal schrijft

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit het
verslag van stap 6d van die kaart. Bestaande fout van `backends/clingo`; `backends/clingo`, `ref/` en `spec/verifier/`
zijn in die kaart en op `main` (`fd5929ce`) gelijk aan haar claimbasis, dus de meting geldt voor `main`.*

## Aanleiding

Een `PrioritizedDefaults`-groep op het attribuut `premie` (`Number`, `decimals: 2`) van `Jutter`, met een default
waarvan de rechtvaardiging `zijn basis > 0` is en de consequentie de literal `5` (`Number`, `whole: true`,
`decimals: 0`). Gemeten op een eigen document met twee ongeordende groepen op `premie`, waarvan de tweede die literal
beslist:

- verifier: PASS;
- ref: `oracle: PASS (2 expectations)`;
- clingo: `TRANSLATION FAULT: default value scale 1 does not match target scale 100`, exit 1
  (`backends/clingo/lib/translate.ml`, r. 10239 op `fd5929ce`, de vertaling van een default-consequentie).

Voor een gewone regel met dezelfde literal naar hetzelfde doel speelt clingo wel. Een waarde met minder decimalen dan
het doel is volgens RUNTIME-SEMANTICS ch. 2.4 punt 1 toegestaan (5 schrijft 5,00), dus clingo wijkt hier af van ref.
De Drools-takken zijn op dit document niet gemeten.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met de kleinste vorm: één defaults-groep, één default met een literal van een kleinere schaal dan het
  doel; eerst rood in gate 6.
- (b) `translate.ml` schaalt de literal van een default-consequentie naar het doel zoals bij een regel; de backendsuite
  krijgt een eigen controle.
- (c) Het vierkant op de golden (ref, clingo, beide Drools-takken); gate 5, 6, 12 en 13 groen, `EXPECTED_PLAYED` als
  voorstel.

May touch: `backends/clingo/**`, `corpus/golden/**`, `scripts/check-drools.sh` (**voorstel**, pin).

Out of scope: `ref/`, `backends/drools` (tenzij de golden daar iets laat zien; dan meten en melden).
