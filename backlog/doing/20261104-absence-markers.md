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

*Bouwer stap A, 2026-11-04/05. Niet gemerged, niet gepusht. Logs onder `werk/absence-markers/logs/`, scripts onder
`werk/absence-markers/scripts/`, proeven onder `werk/absence-markers/probes/` (buiten de repo).*

### Ontwerpnotitie (vóór de bouw)

**Spelling (dialect, voorstel; de hoofdsessie besluit).**
- Declaratie: één markering aan het eind van de attribuutdeclaratie, na een tijdlijn: `(of onbekend)`, `(of niet van
  toepassing)` of `(of onbekend of niet van toepassing)`; drie hele tokens zoals `(mag opgegeven worden)`, zodat geen
  woord gereserveerd raakt. Niet samen met `(mag opgegeven worden)` (de grammatica laat één van beide toe) en niet op een
  kenmerk (de grammatica laat haar daar niet toe).
- Schrijven: de marker als hele waarde van een gelijkstelling: "… moet gesteld worden op onbekend", "… op niet van
  toepassing". Een getal schrijven gaat zoals altijd.
- Toetsen (beide woordvolgordes, zoals "leeg is"/"is leeg"): "zijn taxatie onbekend is", "zijn taxatie niet van
  toepassing is", "zijn taxatie een waarde heeft"; in een opsomming "zijn taxatie is onbekend", "… heeft een waarde". Geen
  ontkenning in stap A ("geen waarde heeft" wacht op een golden).
- Een aggregatie: "de som van de taxatie van alle vondsten van de Jutter die een waarde hebben" (de subselectie op de
  waarde, zoals "die groter zijn dan de drempel").

**Verlaging (gemeten, `probes/sum`, `probes/rgs`).** Naar het somtype van RS ch. 9, met één knoop erbij:
- Een attribuut X van type T met markers M krijgt als datatype een `SumType` over een gegenereerd domein
  `<X> van een <Objecttype>`, met de constructoren `waarde` (payload T) en daarna de markers als nullaire constructoren
  `onbekend` en `niet van toepassing`, in die volgorde. Eén domein per attribuut (nominale identiteit, WF-061): een
  marker gaat niet stil van het ene attribuut naar het andere.
- Een waarde v schrijven: `Construct waarde(v)` (een lege v laat de `Construct` leeg, ch. 9.1, en beslist dus niets); een
  marker: `Construct onbekend`. Een marker toetsen: `Comparison equal X, Construct onbekend` (ch. 9.3). "Een waarde
  heeft": `Predicate filled` over de nieuwe knoop hieronder.
- **Nieuw: `PayloadAccess {type, operand, constructor}`** (IR-SPEC ch. 4.7): de payload van `operand` waar die de
  constructor `constructor` draagt, anders leeg (een mislukte lezing, zoals een to-one-navigatie zonder koppeling). Elke
  lezing van de waarde van X in rekenen, vergelijken of een aggregatie is `PayloadAccess X waarde`.
- Waarom niet het somtype zonder meer: (1) WF-060 laat alleen een getal, een datum in dagen en een somtype als payload toe
  (gemeten: tekst en boolean `WF-060.payload-not-admissible`); (2) de waarde lezen kan alleen met een `Match`, die voor
  elke marker een tak met een waarde eist (WF-063), en de IR heeft geen expressie die leeg is; (3) een aggregatie over
  `Match`-takken is geen fold-bron. Waarom niet een vlakke markering op het attribuut (`markers` naast het datatype, een
  markerwaarde in de waardeunie, drie nieuwe predicaatsoorten): de Drools-loader leest de wire strikt en geeft op een
  onbekend veld `ERROR DocumentError` (gate 12 faalt dan, ook met een kanaalregel); met het somtype weigert hij bij het
  laden met de bestaande code DROOLS-010, en de vlakke vorm zou daarnaast elke uitvoerder een tweede waardesoort geven.

**De leesregel en de nieuwe WF-regel.** RS ch. 9.5 (nieuw): een `PayloadAccess` op een andere constructor of op leeg is
leeg, per dag op een tijdlijn. **WF-127** (één regel): een `PayloadAccess` staat onder een toets van zijn eigen
constructor. Armen: `operand-not-sum` (de operand is geen somtype), `constructor-not-unary` (de constructor is geen
gedeclareerde unaire constructor van dat domein), `type` (het `type` is niet de gedeclareerde payload), `unguarded` (geen
toets ervoor). Een toets ervoor is: de knoop is zelf de operand van een gevuld- of leegtoets; of een conjunct van de
voorwaarde van zijn vorm (regelversie, default, objectcreatie) is een niet-ontkende gevuldtoets van een `PayloadAccess`
met dezelfde operand (structureel, de bron niet meegeteld) en dezelfde constructor — in de voorwaarde zelf een conjunct
dat vóór de lezing staat (ch. 5.10); of, binnen een fold over leden, zo'n conjunct van het filter van die fold. Elke
andere plaats is onbewaakt en geweigerd. De frontend weigert dezelfde lezing in de bron met **RGS-376**.

**Caseformaat.** Geen nieuwe vorm: een marker en een waarde zijn constructorwaarden van de bestaande variant
(`{"constructor": "onbekend"}`, `{"constructor": "waarde", "payload": {"number": "40.00"}}`), geen `null` (N3).

**Datatypen.** WF-060 verruimd: een payload is een getal, tekst, boolean, datum (dag en milliseconde), enumeratie of
gedeclareerd somtype (geen lijst of struct). Een enumeratie als payload: de enumeratiewandeling van WF-088 loopt al door
somtypen.

**Aggregatie (aanbeveling, normvraag).** Een aggregatie over leden met markers eist een filter op de waarde
("die een waarde hebben"): de fold leest `PayloadAccess` per lid onder een gevuldtoets in zijn filter (WF-127, arm
`unguarded`). Niet: markers overslaan zoals leeg, want dan telt een som "onbekend" stil als afwezig, terwijl het besluit
zegt dat wie leest eerst toetst.

**Per periode (stap A).** Een marker schrijven op een tijdlijndoel (een scalaire `Construct` in een venster), de invoer
per dag, EF-016 per dag, en een toets per periode met `Comparison equal` (ch. 3.10, vergelijking met een scalaire
grens). Een `PayloadAccess` op een tijdlijn heeft een lezing per dag (ch. 9.5), maar de bewaking per periode is een
gevuldtoets per periode, en die is open (ch. 3.12 rij 5): ref en clingo weigeren hem bij naam, de frontend met de
bestaande code voor die toets.
