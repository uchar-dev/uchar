# defaults-per-period-on-timeline: een defaults-groep op een tijdlijndoel beslist per periode

*Opgesteld door de hoofdsessie op 2026-11-04, op verzoek van de eigenaar (lezing B per periode).*

## Aanleiding

Invoer op een berekend attribuut heeft in uchar twee lezingen (`spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md`, besloten
2026-09-25). Lezing A, de standaard: de invoer is de laagste default boven de initialisatie (RS ch. 7.8, de virtual input default); een
regel die beslist, verdringt haar, en sinds 2026-11-04 per dag en voor elke schrijver (kaarten `writers-per-day-beside-input`,
`empty-write-decides-nothing`). Lezing B, "opgegeven, niet afgeleid", per attribuut met de dialectmarkering `(mag opgegeven worden)`
(`frontends/regelspraak/grammar/DIVERGENCES.md` §7): de frontend verlaagt haar tot een defaults-groep met de opgave boven de berekening.

B kan niet op een tijdsafhankelijk attribuut: de frontend weigert de markering daar (RGS-150), en de verifier weigert elke defaults-groep
op een tijdlijndoel (IR-SPEC WF-086: "een defaults-groep beslist op één fixpunt en heeft geen tijdcoördinaat om op te schrijven";
PRIORITIZED-DEFAULTS-EVOLUTIE §9 Q12a, niet ingepland). De praktijk vraagt het wel: een vastgesteld dagloon of een correctie van een
behandelaar geldt voor een periode, en daarbuiten de berekening.

**Besluit van de eigenaar (2026-11-04):** B per periode bouwen.

## Goal

Een defaults-groep (`prioritizedDefaults`) op een tijdsafhankelijk attribuut beslist per periode, gelezen per dag zoals heel ch. 3: op elke
dag wint de hoogste default die die dag beslist (zijn rechtvaardiging geldt die dag en zijn waarde leest die dag een waarde); een lege
waarde valt per dag door; de virtual input default leest de invoer van die dag. Op een maand- of jaarraster is dat per emmer, met de
rastereisen van WF-126. De markering `(mag opgegeven worden)` mag dan op een tijdsafhankelijk attribuut en verlaagt zoals op een scalair.
Uitgewerkt voorbeeld (absoluut): `strandloon` per maand met de markering, invoer 10 op elke maand van 2025, een regel die 20 schrijft op
maart–mei: **10** op heel 2025. Dezelfde regel met invoer alleen op januari–februari: **10** op januari–februari, **20** op maart–mei,
leeg daarbuiten.

Wat er vanzelf meekomt, meet en noem je: een uitzondering (`Uitzondering op`, de defaults-tilt van het dialect) op een tijdlijndoel, en de
uitleg van een default per periode (RS ch. 5.12, gate 17).

## Acceptance

- (a) Stap A — spec, verifier, frontend, ref, clingo: RS ch. 7 (7.1, 7.6, 7.8, 7.10 waar ze een tijdlijndoel raken) als huidige stand
  (voorstel), met het uitgewerkte voorbeeld dat `ref/test` letterlijk overneemt; WF-086 houdt alleen de kenmerkarm (een tijdsafhankelijk
  kenmerk blijft buiten, RS ch. 3.12 rij 7), met een nieuwe witness; de attribuutarm van de negatieve `wf086-temporal-default-target`
  wordt een golden of een negatief van een regel die blijft (de kaart verandert die regel uitdrukkelijk; noem oud en nieuw); RGS-150 valt
  weg of houdt alleen wat blijft; een golden met `source.rgs` en de markering op een tijdlijn, met minstens de twee gevallen van het
  voorbeeld, in ref en clingo.
- (b) Stap B — Drools, beide branches: de defaults-stapels per periode, met de lezingen van ch. 5.12; de golden speelt in gate 12 en 17.
- (c) Rood eerst per uitvoerder; per nieuwe bewaking een mutatie die een test rood maakt.
- (d) De corpusmeting: geen bestaande verwachting hoort te veranderen (geen golden draagt nu een defaults-groep op een tijdlijn); meet het.
- (e) Voorgestelde tekst voor CONSTRUCTS (rij 8 en rij 11, delta tegen v0) en zo nodig CLAUDE.md, als huidige stand.
- (f) `make gates` groen, na elke stap.

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `spec/verifier/` (voorstel), `frontends/regelspraak/`, `ref/`, `backends/clingo/`,
`backends/drools/**`, `corpus/golden/`, `corpus/negative/` (de WF-086-arm, met reden), `scripts/` (alleen lijsten en vloeren die schuiven;
voorstel), kaartverplaatsing.

Out of scope: een defaults-groep op een tijdsafhankelijk kenmerk; de dienst van de distributie, die nog geen tijdsafhankelijk attribuut als
invoer neemt (een kaart in `regelspraak-toolchain`, daarna een pin-bump).

## Omvang

Middel tot groot; twee stappen, elk landen zodra groen.

## Result
