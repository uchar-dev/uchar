# clingo-suite-shards: de suite van clingo in delen naast elkaar

*Opgesteld door de hoofdsessie op 2026-11-22, op verzoek van de eigenaar (de volledige gate-set duurde 1229 s; "veel te lang
om iets routinematig te doen").*

## Aanleiding, gemeten

`make gates` duurde 704 s en duurt nu 1229 s, doordat gate 6 (1221 s) het kritieke pad is. Alleen duurt gate 6 233 s: 177 s
suite (één proces, 168 s CPU, 4454 controles, elke controle start één of twee `clingo`-processen) en 47 s replays op acht
processen. De suite is één draad; onder de belasting van de andere gates is zij ongeveer zes keer zo traag. De suite naast de
replays laten lopen scheelt hoogstens 47 s.

## Wat er komt

`test_clingo` draait met `UCHAR_SUITE_SHARD=k/n` de k-de van n delen; gate 6 start n delen naast elkaar (`scripts/lib/parallel.sh`
of `xargs`) en telt. Zonder de variabele draait alles, zoals nu. Geen stille groene gate: een controle die in geen deel of in
twee delen draait, laat de gate falen; de floor van het aantal controles staat op de gemeten stand.

## May touch

`backends/clingo/test/test_clingo.ml`, `scripts/check-clingo.sh` (mandaat van de eigenaar voor deze kaart), `CLAUDE.md` gate 6
alleen als zin verandert (dan als voorstel in het Result).

## Acceptance

- Gate 6 in de volle set ≤ 400 s, en alleen ≤ 120 s; de volle `make gates` terug naar ongeveer 704 s.
- De gate faalt op een weggelaten deel, een dubbel deel en een controle die ontbreekt, elk aangetoond met een mutant.
- De suite geeft dezelfde controles en hetzelfde resultaat zonder de variabele.
