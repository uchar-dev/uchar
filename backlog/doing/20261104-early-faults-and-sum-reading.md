# early-faults-and-sum-reading: de restpunten van clingo's vroege fouten, en de lezing van een falende som-operand in Drools

*Opgesteld door de hoofdsessie op 2026-11-04 uit het Result van `clingo-lazy-bucket-scalar-fault` (vragen 1–4); de eigenaar besloot ze
alle vier te repareren.*

## Aanleiding

`clingo-lazy-bucket-scalar-fault` liet clingo EF-037 en EF-014 van de periode-aritmetiek geven waar de vorm geëvalueerd wordt, zoals ref en
Drools. Bij het meten bleven vier punten over:

1. Twee frames van `backends/clingo/README.md` [L1] geven nog EF-014 tijdens het vertalen, waar ref ze uitrekent: een fout waar de norm er
   geen kent.
2. EF-010, EF-011, EF-018, EF-019, EF-020, EF-021, EF-022 en EF-024 vallen in clingo nog tijdens het vertalen. Op het corpus en de suites
   geven ze hetzelfde als ref, maar geen golden of fixture zet zo'n vorm achter een voorwaarde die niet geldt.
3. Waar het bereik van een lezing afhangt van de volgorde waarin een creatie leden toevoegt, geeft clingo de fout tijdens het vertalen, over
   elke instantie die de emmerscalair geeft.
4. Drools leest een operand die in een som faalt als `unreached`, waar RUNTIME-SEMANTICS ch. 5.12 `fault` zegt.

## Goal

clingo geeft elke EF-code alleen waar ref haar geeft; kan de codering een vorm niet dragen, dan weigert clingo met een code van het soort
`refusal` of `limit`, nooit met een EF-code. Drools leest een falende som-operand als ch. 5.12 zegt. Elk punt met een getuige in het corpus
of de suites, zodat gate 5, 6, 12 of 17 het vasthoudt.

## Acceptance

- (a) Punt 1: de twee [L1]-frames spelen in clingo of weigeren met een code; README [L1] als huidige stand.
- (b) Punt 2: per EF-code een document met de vorm achter een voorwaarde die niet geldt (en een die wel geldt), in ref, clingo en beide
  Drools-branches; elk verschil opgelost zoals in de Goal.
- (c) Punt 3: gemeten met een document waar het verschil zichtbaar zou zijn; opgelost of, als de codering het niet draagt, een weigering
  met code en een zin in ASP-TRANSLATION.
- (d) Punt 4: rood eerst in de suite (of een golden waar gate 17 het ziet), daarna de lezing van ch. 5.12 op beide branches.
- (e) Rood eerst per punt; per nieuwe bewaking een mutatie die een test rood maakt. `make gates` groen.

May touch: `backends/clingo/`, `backends/drools/**`, `ref/` (alleen waar een meting een verschil met ch. 5.12 toont), `corpus/`,
`spec/RUNTIME-SEMANTICS.md` en `spec/ASP-TRANSLATION.md` (voorstel), `scripts/` (alleen lijsten en pins die schuiven; voorstel),
kaartverplaatsing.

## Omvang

Klein tot middel.

## Result
