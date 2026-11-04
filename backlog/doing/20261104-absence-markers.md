# absence-markers: "onbekend" en "niet van toepassing" als gevulde waarden, per attribuut aan te zetten

*Opgesteld door de hoofdsessie op 2026-11-04, na het onderzoek `flavours-of-empty` (`spec/proposals/FLAVOURS-OF-EMPTY.md`, de besluiten
van de eigenaar bovenaan).*

## Aanleiding

uchar heeft één leeg: de afwezigheid. Sinds 2026-11-04 beslist een schrijver die leeg oplevert niets, dus geen regel kan "geen waarde,
en dat is zo" schrijven. De eigenaar wil twee groepen eenduidig scheiden: leeg (ontbrekend; een regel, de invoer of een initialisatie
mag het nog vullen) tegenover een gevulde waarde zonder getal (niet van toepassing, bekend onbekend), die beslist. De gevestigde
formele talen doen dat met een waardedomein met benoemde varianten (Catala's enumeratie met payload, L4 `MAYBE`); de IR heeft daarvoor het
somtype (IR-SPEC `SumType`, CONSTRUCTS rij 36, RS ch. 9), maar RegelSpraak heeft er geen bronvorm voor.

## Goal

Twee markers, **onbekend** (er is een waarde, niet vast te stellen) en **niet van toepassing** (er bestaat geen waarde), per attribuut aan
te zetten, één of beide, voor elk datatype en ook op een tijdsafhankelijk attribuut (per periode); niet op een kenmerk.

- **Bron (dialect):** een markering op de attribuutdeclaratie in RegelSpraak-stijl, naar het voorbeeld van `(mag opgegeven worden)`
  (`frontends/regelspraak/grammar/DIVERGENCES.md` §7), bijvoorbeeld `het partnerinkomen Bedrag (of niet van toepassing);`; een regel
  schrijft een marker als waarde ("… moet gesteld worden op niet van toepassing"), een voorwaarde toetst haar ("indien zijn
  partnerinkomen niet van toepassing is", "… onbekend is", "… een waarde heeft"). De precieze spelling stelt de bouwer voor; de
  hoofdsessie besluit. Een nieuw DIVERGENCES-hoofdstuk.
- **IR:** de verlaging naar het somtype van ch. 9 (de waarde als constructor met payload, de markers als constructors zonder), of, als
  dat niet past, het kleinste wat er wel past; de bouwer meet en stelt voor, zonder een tweede afwezigheid in te voeren.
- **Semantiek (RS als voorstel):** een marker is een gevulde waarde en beslist overal waar een getal beslist: schrijven (ch. 3.11, 5.7),
  defaults en de invoer (ch. 7, lezing A en B), EF-016 tegen een andere waarde, per dag op een tijdlijn. Wie een attribuut met markers
  leest in rekenen, vergelijken of een aggregatie, toetst eerst of het een waarde is; de verifier weigert het anders (één nieuwe WF-regel)
  en de frontend ook (één nieuwe RGS-code). Hoe een aggregatie over leden met markers leest (een filter eisen, of de markers
  overslaan), stelt de bouwer voor; het is een normvraag voor de eigenaar.
- **Caseformaat:** een marker als waarde in invoer en verwachting (`uchar-case-v0`), geen `null`.

## Acceptance

- (a) Stap A — spec, schema, verifier, frontend, ref, clingo: een golden met `source.rgs` en een attribuut met beide markers (scalair en
  per periode), met cases waarin een regel een marker schrijft, een voorwaarde haar toetst, een marker de invoer verdringt en een
  marker botst met een getal (EF-016), een negatief voor de nieuwe WF-regel en een fixture per arm; uitgewerkte voorbeelden die
  `ref/test` letterlijk overneemt.
- (b) Stap B — Drools, beide branches, en de lezingen van ch. 5.12; de golden speelt in gate 12 en 17.
- (c) Rood eerst; per nieuwe bewaking een mutatie die een test rood maakt. Geen bestaande verwachting verandert; meet het.
- (d) Voorgestelde tekst (huidige stand) voor RS, IR-SPEC, het case-schema, CONSTRUCTS (rij 36 en een nieuwe rij of rij 1), en zo nodig
  CLAUDE.md.
- (e) `make gates` groen na elke stap.

May touch: `spec/` (voorstel), `frontends/regelspraak/`, `ref/`, `backends/clingo/`, `backends/drools/**`, `corpus/`, `scripts/` (alleen
lijsten en vloeren die schuiven; voorstel), kaartverplaatsing.

Out of scope: de dienst van de distributie (een toolchain-kaart, daarna een pin-bump); meer markers dan twee; een kenmerk met markers.

Meenemen (gemeten in `flavours-of-empty`): clingo weigert een Boolean-attribuut in een conditie zonder registercode ("unscalable
datatype in the ASP translation", `backends/clingo/lib/translate.ml`); markers op een Boolean-attribuut hebben die conditie nodig.

## Omvang

Middel tot groot; twee stappen.

## Result
