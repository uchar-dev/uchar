# clingo-lazy-bucket-scalar-fault: EF-037 alleen waar de vorm geëvalueerd wordt

*Opgesteld door de hoofdsessie op 2026-11-04, uit het Result van stap B van `defaults-per-period-on-timeline` (vraag 1).*

## Aanleiding

EF-037: een vorm die een hele tijdlijn leest, vindt alleen de emmerscalair van de rekendatum. ref en Drools geven die fout waar de vorm
geëvalueerd wordt. Een gevolg van een default dat op geen dag aan de beurt is, evalueren ze niet, dus geen fout (RS ch. 7.11,
`WV-7.11-gevolg-buiten-de-beurt`: met kustwacht en winterwacht in april 2025 **5**, **60** in april, **5**, geen fout). De ASP-backend leest
de emmerscalair al tijdens het vertalen en faalt op beide cases met EF-037. Dat is een fout waar de norm er geen kent, geen weigering van
de backend; gate 6 laat zoiets op een golden niet toe, maar het document staat alleen als fixture
(`ref/test/fixtures/defaults-per-day/lazy-consequence`), dus geen gate ziet het. RS ch. 7.11 noemt het nu als feit.

## Goal

clingo geeft EF-037 alleen waar ref en Drools het geven: waar de vorm die de tijdlijn leest, geëvalueerd wordt (een gevolg op een dag van
zijn beurt waar zijn rechtvaardiging geldt; een voorwaarde of waarde die bereikt wordt). Kan de codering dat niet dragen, dan weigert
clingo met een code van het soort `refusal` of `limit` uit `backends/clingo/codes.tsv`, nooit met een EF-code. Meet eerst elke plaats waar
clingo EF-037 (of een andere EF-code) bij het vertalen geeft, en vergelijk met ref op dezelfde documenten: dezelfde vroege fout buiten de
defaults hoort bij deze kaart.

## Acceptance

- (a) Rood eerst: de twee cases van `lazy-consequence` als golden (de case zonder fout) en als rode casus (EF-037, `attribute kustloon`,
  `label J1`) in het corpus; op main faalt clingo de golden met EF-037, daarna speelt hij (of staat hij met een refusal-code in het kanaal
  van gate 6, met reden). ref en beide Drools-branches spelen beide.
- (b) Elke andere plaats van een vroege EF-code in clingo: gemeten, en opgelost of als vraag in het Result.
- (c) RS ch. 7.11 zonder de zin over de ASP-backend (voorstel); ASP-TRANSLATION en CONSTRUCTS rij 8 als huidige stand.
- (d) `make gates` groen.

May touch: `backends/clingo/`, `corpus/golden/`, `corpus/red/`, `spec/RUNTIME-SEMANTICS.md` (voorstel), `scripts/check-clingo.sh`
(alleen het kanaal; voorstel), kaartverplaatsing.

## Omvang

Klein.

## Result
