# clingo-refusal-channel: gate 6 krijgt een weigerkanaal, en de verzuimrente-ankers worden exact

*Opgesteld door de hoofdsessie op 2026-11-03 uit de architectuurreview van Fable (`werk/review-fable-2026-11-03/advies.md`, §1 punt 6, §3
"Clingo", advies 2). Besluit van de eigenaar 2026-11-03: **gate 6 krijgt een weigerkanaal**.*

## Aanleiding

Gate 6 eist dat elke golden in clingo speelt; er is geen kanaal voor een vorm die de ASP-codering niet draagt. Daardoor vormde clingo het
corpus: de ankers `corpus/golden/anchor/jutterswet-verzuimrente-dagen` en `-30-360` ronden af op de cent (139,18 €, 73,77 €, 141,67 €, 72,22 €)
in plaats van de exacte waarden 139 13/73, 73 47/61, 141 2/3 en 72 2/9 €, omdat clingo geen niet-decimale waarde houdt, en het ene anker
kreeg een geheel afdrachtbedrag om onder de factorgrens 46340 te blijven. CLAUDE.md zegt dat de ankers nooit door een backend gevormd worden.
Clingo is het model-theoretische anker voor fixpunt, negatie, defaults en kringen; voor rekenkunde levert het weinig tweede mening (de
kalender rekent het met de code van ref, `calendar_weights`).

## Goal

`scripts/check-clingo.sh` krijgt een reason-carrying lijst `EXPECTED_CHANNEL` naar het model van gate 12: per golden die clingo niet speelt
`golden|CLINGO-###|reden`, waarbij de gate de code van de weigering vergelijkt met de regel (de code uit `backends/clingo/codes.tsv`, kind
`refusal` of `limit`), stale-bewaakt in beide richtingen (een regel voor een golden die speelt of weg is, faalt; een golden die weigert
zonder regel, faalt), en met een vloer of een nul-regel die zegt waarom. Daarna de twee verzuimrente-ankers terug naar hun exacte waarden in
de onbegrensde vorm, zoals de ankercasus van `spec/proposals/IMPLICIT-TIME.md` §4 ze geeft; clingo staat dan met naam in het kanaal ([V6]
en de factorgrens), ref en beide Drools-branches spelen ze exact.

## Acceptance

- (a) Rood eerst: de gate zonder kanaal faalt op een anker met exacte waarden (letterlijk); een zelftestrij per richting van de
  stale-bewaking.
- (b) CLAUDE.md gate 6 beschrijft het kanaal (voorstel; de hoofdsessie landt), in de stijl van gate 12, als huidige stand.
- (c) De ankers exact: de verwachtingen in de onbegrensde vorm (of een doel dat de breuk houdt), het afdrachtbedrag terug naar 10 000,00 €
  als dat de ankercasus is; ref en beide Drools-branches spelen ze; gate 13 en 17 groen.
- (d) Geen nieuwe rekenkunde en geen rationalen in `translate.ml`.
- (e) `make gates` groen.

May touch: `scripts/check-clingo.sh` (voorstel), `CLAUDE.md` (voorstel), `corpus/golden/anchor/jutterswet-verzuimrente-*` (voorstel),
`backends/clingo/codes.tsv` als een code ontbreekt, `scripts/check-drools.sh` als een pin verschuift, kaartverplaatsing.

Out of scope: andere goldens die nu om clingo heen geschreven zijn (meet en noem ze in het Result, als vervolg).

## Omvang

Klein tot middel.
