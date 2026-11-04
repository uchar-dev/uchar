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

*Bouwer stap A, 2026-11-04. Niet gemerged, niet gepusht. Logs onder `werk/absence-markers/logs/`, scripts onder
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
constructor. Gebouwd met de armen `base-not-sum`, `base-untyped`, `domain-undeclared`, `constructor-undeclared`,
`constructor-nullary`, `type-differs`, `access-untyped` (de vorm, naar het voorbeeld van WF-074) en `unguarded` (geen
toets ervoor); het veld van de operand heet `base`, zoals bij `FieldAccess`. Een toets ervoor is: de knoop is zelf de
operand van een gevuld- of leegtoets; of een conjunct van de voorwaarde van zijn vorm (regelversie, default,
objectcreatie) is een niet-ontkende gevuldtoets van een `PayloadAccess` met dezelfde operand (structureel, de bron niet
meegeteld) en dezelfde constructor — in de voorwaarde zelf een conjunct dat vóór de lezing staat (ch. 5.10); of, binnen
een fold over leden, zo'n conjunct van het filter van die fold. Elke andere plaats is onbewaakt en geweigerd. De
frontend weigert dezelfde lezing in de bron met **RGS-376**.

**Caseformaat.** Geen nieuwe vorm: een marker en een waarde zijn constructorwaarden van de bestaande variant
(`{"constructor": "onbekend"}`, `{"constructor": "waarde", "payload": {"number": "40.00"}}`), geen `null` (N3).

**Datatypen.** WF-060 verruimd: een payload is een getal, tekst, boolean, datum (dag en milliseconde), enumeratie of
gedeclareerd somtype (geen lijst of struct). Een enumeratie als payload is een gebruiksplaats van haar domein; WF-088
loopt daarom ook door de payload van een somtypedeclaratie (`wf088-sum-payload-drift`).

**Aggregatie (aanbeveling, normvraag).** Een aggregatie over leden met markers eist een filter op de waarde
("die een waarde hebben"): de fold leest `PayloadAccess` per lid onder een gevuldtoets in zijn filter (WF-127, arm
`unguarded`). Niet: markers overslaan zoals leeg, want dan telt een som "onbekend" stil als afwezig, terwijl het besluit
zegt dat wie leest eerst toetst.

**Per periode (stap A).** Een marker schrijven op een tijdlijndoel (een scalaire `Construct` in een venster), de invoer
per dag, EF-016 per dag, en een toets per periode met `Comparison equal` (ch. 3.10, vergelijking met een scalaire
grens). Een `PayloadAccess` op een tijdlijn heeft een lezing per dag (ch. 9.5), maar de bewaking per periode is een
gevuldtoets per periode, en die is open (ch. 3.12 rij 5): ref en clingo weigeren hem bij naam. *Gemeten na de bouw
(`probes/rgs/p3.rgs`):* de frontend schrijft zo'n regel wel, met de `PayloadAccess` onder die toets, en de verifier laat
hem toe; de weigering staat dus bij de uitvoerders.

### Commits

| hash | inhoud |
|---|---|
| `5e5a65ccc` | kaart naar `doing/` |
| `01b085a20` | ontwerpnotitie |
| `b60a59138` | corpus (rood eerst): golden `jutterswet-afwezigheidsmarkers`, rode tweeling `jutterswet-afwezigheidsmarkers-botsing`, negatieven `wf127-*` (10) en `wf088-sum-payload-drift`, fixtures `ref/test/fixtures/absence-markers/` (4); de hernoeming `wf060-text-payload` → `wf060-list-payload` |
| `af829446a` | OCaml: AST, loader, verifier (WF-127, WF-060, WF-088), schema (beschermd, voorstel); ref; clingo; de arm van de Catala-frontend; ref-suite en clingo-suite |
| `a4abcb53f` | frontend: grammatica, resolver, emitter, RGS-376 met catalogi, `AbsenceMarkerTest`, het rijke document van `ScenarioLoweringTest` |
| `921915ede` | gates 8 en 12 (beschermd, voorstel): de golden en de rode tweeling met DROOLS-010 in het kanaal |
| `54b9879fa` | RS ch. 9 en 9.5, IR-SPEC, ASP-TRANSLATION (voorstel) en DIVERGENCES |
| `f377ce686` | ref: `PayloadAccess` in de nummering van de lezing (gate 17 was rood in `gates-1`) |
| `249285cbf` | Drools: de lader weigert `PayloadAccess` bij naam, DROOLS-313 (gate 12 was rood in `gates-1`, VB-7) |
| `69867d173` | DIVERGENCES: de lezing van een waarde per periode, zoals gemeten (de frontend schrijft haar) |
| (deze) | Result |

### Wat per laag veranderde

- **IR (schema, AST, loader; beschermd, voorstel).** Nieuwe knoop `PayloadAccess {node, type, base, constructor,
  source}` in de `expr`-unie; de beschrijvingen van `SumType` en `Construct` noemen de frontend als schrijver. Geen
  nieuw veld op `Attribute`, geen nieuwe waarde in de waardeunie: de markers zijn constructoren.
- **Verifier (beschermd, voorstel).** WF-127 (vormarmen in de expressiewandeling, de wachtarm in
  `wf127_payload_guards`); een `PayloadAccess` over de `AttributeReference` van het element is een fold-bron (WF-037,
  WF-047, de fold-bronregels op de base). WF-060 laat elk scalair datatype als payload toe; WF-088 loopt door de
  payloads van een somtypedeclaratie. `precision` leest de payload als gedeclareerd.
- **ref.** `PayloadAccess` (`payload_of`); de fold over leden met `PayloadAccess` als bron; een constructor van een
  literal is een scalaire operand van de periode-aritmetiek, dus een marker in een venster op een tijdlijndoel; een
  `PayloadAccess` op een tijdlijn weigert bij naam (scalair en als fold-bron). De rest (EF-016 per dag, de stapel per
  dag, de vergelijking per periode) speelde al.
- **clingo.** Een constructornaam met spaties wordt één symbool (`niet_van_toepassing`), met een weigering waar twee
  constructoren in één symbool samenvallen; de payload als eigen lid (`val(O,"taxatie#waarde",P)`, projectieregels per
  unaire constructor, `find_decl` kent het lid); `PayloadAccess` in `expr_term` en als fold-bron
  (`cell_source_fold`); een tijdlijn van constructoren in de invoer, geschreven door een constructor van een literal
  (`translate_period_arithmetic`), vergeleken per periode (`period_condition`); de modelcontrole leest een
  tijdlijnverwachting met constructorwaarden (`canonical_timeline` generiek).
- **Frontend.** Drie tokens en de regel `afwezigheidsmarkering`; `ObjectType.attributeMarkers`;
  `RegelSpraakModel.absenceMarkerSites` (per knoop, op identiteit: marker, waardetoets, lezing van de waarde); de
  marker als waarde in `RuleChecker.register`, de toetsen in `characteristicCheckPredicate`, "die een waarde hebben" in
  `ExpressionBuilder.aggregationFilter`; de slotpas `AbsenceMarkers` (classificeert elke lezing en geeft RGS-376); de
  emitter schrijft somtype, domein, `Construct` en `PayloadAccess`; de semantische tokens lezen de woorden van de
  markers als sleutelwoord in een document met markers.
- **Corpus en lijsten.** Zie de commits; gate 12 `EXPECTED_CHANNEL` +1 (DROOLS-010), gate 8 Drools-uitsluitingslijst
  +1 (DROOLS-010); `EXPECTED_PLAYED` ongewijzigd (de golden speelt niet in Drools). Geen vloer verschoven.
- **Drools.** Eén weigering erbij in de lader, `case "PayloadAccess"` met DROOLS-313 `expr-payload-access` (register,
  beide catalogi, `UnknownNodeWitnessTest`): de suite loopt elke knoop van het schema af en eiste dat (VB-7). Verder
  niets; de jar compileert met de nieuwe modelvelden, en beide branches weigeren beide documenten bij het laden met
  `EXCLUDE DROOLS-010: loader: form not carried by the v0 Drools backend: Datatype:SumType`.

### Rood eerst (letterlijk)

- **verifier, ref, clingo op main** (`53605e865`, gebouwd in een losse worktree; `logs/06-red-golden-main.log`): op
  elke case van de golden en de rode tweeling `IR LOAD ERROR at /rules/rules/3/versions/0/condition/operand/node:
  unknown expr node "PayloadAccess"`, de verifier `LOAD ERROR at … unknown expr node "PayloadAccess"`.
- **De somtypeweg zonder PayloadAccess op main** (`logs/07-red-probes-main.log`, `probes/sum`): de verifier
  `WF-060.payload-not-admissible … constructor payload is text` (en boolean); clingo `constructor name "niet van
  toepassing" does not normalise to a valid ASP functor symbol`; op een tijdlijn ref `temporal attribute
  "maandinkomen" as a target: temporal writes are not implemented` en clingo `temporal sum carriage is a recorded
  flank; no corpus case`.
- **frontend op main** (`logs/20-red-frontend-main.log`): `ERROR (line 5): syntaxfout: extraneous input '('
  expecting {<EOF>, …}`; zonder de markering (`golden/plain.rgs`) gaven "onbekend" en elke toets een WARNING
  `onopgelost: …` en bleef "die een waarde hebben" `RGS-069`.
- **gate 15 met de bron als `source.rgs`** (`logs/13-gate15-red.log`): `GraphSourceRouteTest … jutterswet-
  afwezigheidsmarkers => dev.uchar.regelspraak.loader.IrLoader$Refusal: form not carried by the v0 Drools backend:
  Datatype:SumType` — de reden dat de bron in stap A in `AbsenceMarkerTest` staat (VB-6).
- **gate 7**: de eerste case 4 ("niets") overleefde `--noop` (gemeten: `oracle: PASS (4 expectations, NO-OP)`), en case 8
  droeg evenzo alleen lege verwachtingen; beide kregen een afgeleide verwachting erbij (V2 met schatting; V2
  `onbekend`), in plaats van een regel in de overleverslijst.

### Groen

ref-suite `test_ref: all 2444 worked examples pass` (was 2409); clingo-suite `test_clingo: 2633 checks, 0 failures`
(was 2607); frontend-suite 1506 tests, 0 skipped, 0 failed (na de correctie van de regelverwachting in mijn eigen
test); gate 3 `624 negative rejected …, 469 on their declared arm` (elf negatieven erbij, elk met `expect.txt`);
Drools-suite in gate 12 (onder).

### Tests die de kaart omdraait of uitbreidt (oud → nieuw)

1. `corpus/negative/wf060-text-payload` (een `Text`-payload, WF-060) → `corpus/negative/wf060-list-payload` (een
   `ListType`-payload): de kaart laat elk scalair datatype als payload toe, dus de oude getuige werd schoon; de nieuwe
   houdt dezelfde arm `WF-060.payload-not-admissible`.
2. `corpus/negative/wf088-functiondef-drift/NOTE.md`: de bijkomende regel `WF-060 … enumeration` valt weg (de
   verifier meldt nu precies de twee WF-088-regels); de getuige zelf is ongewijzigd.
3. `ScenarioLoweringTest.RIJK` kreeg een attribuut met `(of onbekend)` en een regel die de marker schrijft:
   `a_scope_that_names_everything_is_the_production_document` eist dat het rijke document elke verzameling van het
   model vult, ook de nieuwe `absenceMarkerSites`.
4. Drools `UnknownNodeWitnessTest`: `PayloadAccess` in de lijst van de knopen die de lader als vorm weigert.

### Voorgestelde tekst (huidige stand)

**RUNTIME-SEMANTICS** ch. 9 (inleiding), ch. 9.4 (tijdlijnen) en het nieuwe ch. 9.5, **IR-SPEC** ch. 2.4 (tabelrij
`SumType`, WF-060, WF-088, de declaratiezijde in WF-089 en zijn indexrij), ch. 4.7 (`PayloadAccess`, WF-127) en de
WF-index (WF-060, WF-088, WF-127), **ASP-TRANSLATION** S14 en S19, **DIVERGENCES** "Afwezigheidsmarkeringen" en het
**IR-schema** (`PayloadAccess`, de beschrijvingen van `SumType` en `Construct`): in `54b9879fa` en `af829446a`.

**Case-schema** (`spec/schema/uchar-case-v0.schema.json`), de beschrijving van de constructorvariant van `value`, ter
vervanging: "A sum-type constructor value (CONSTRUCTS row 36): a declared constructor tag plus, for a unary
constructor, its one payload value; 'payload' is absent for a nullary constructor. An absence marker is such a value
(spec/RUNTIME-SEMANTICS.md ch. 9.5): `{"constructor": "onbekend"}`, and a value of an attribute with markers is
`{"constructor": "waarde", "payload": …}`; the format has no null." Geen structuurwijziging: de markers rijden op de
bestaande variant (N3).

**CONSTRUCTS rij 36**, in de kolom v0→v1 delta: "`SumType`/`Construct`/`Match` never emitter-produced" →
"`Match` never emitter-produced; the RegelSpraak frontend writes `SumType`, `Construct` and `PayloadAccess` for an
attribute with absence markers (row 47)"; "temporal sum carriage a recorded flank in the ASP inventory" → "a sum-typed
timeline carried in ref and the ASP backend for the input, a write of a constructor of a literal and a comparison per
period"; achteraan: "**The payload access (2026-11-04, card `absence-markers`):** `PayloadAccess` reads the payload of a
unary constructor, leeg on another constructor or leeg (RUNTIME-SEMANTICS ch. 9.5), and WF-127 holds it under a test of
its constructor; WF-060 admits every scalar datatype as a payload, and WF-088 walks the payload of a sum-type
declaration. Executed in ref and the ASP backend (golden `jutterswet-afwezigheidsmarkers`); the Drools backend refuses
the sum type at load (DROOLS-010) and the node by name (DROOLS-313)."

**CONSTRUCTS, nieuwe rij 47:**

> | 47 | Absence markers — onbekend and niet van toepassing as filled values, per attribute | the decisions of the
> maintainer of 2026-11-04 (`spec/proposals/FLAVOURS-OF-EMPTY.md`, N1–N5); Codd's A- and I-marks; Catala's enumeration
> with payload and L4's `MAYBE`, which make absence a type the author handles | dialect (RegelSpraak) over core | sugar
> over row 36 | new (no v0 counterpart): `(of onbekend)`, `(of niet van toepassing)`, `(of onbekend of niet van
> toepassing)` on an attribute lower it to a sum type of its own with the constructors `waarde` (the declared datatype
> as payload), `onbekend` and `niet van toepassing`; a marker written is a `Construct` and decides as a number does
> (the input, the stack of ch. 7.8, EF-016, per day on a timeline); a marker tested is an equality with it; the value
> is read with `PayloadAccess` under a test of it (WF-127, RGS-376). Leeg stays the one absence | RUNTIME-SEMANTICS
> ch. 9.5, `frontends/regelspraak/grammar/DIVERGENCES.md` "Afwezigheidsmarkeringen"; golden
> `jutterswet-afwezigheidsmarkers`, red `jutterswet-afwezigheidsmarkers-botsing`, negatives `wf127-*`. Ref and the ASP
> backend play scalar and per period (writing, input, EF-016, the test per period); the value read per period is
> refused (ch. 3.12 row 5). Drools: step B of the card |

**CLAUDE.md**, gate 3: na "… WF-082 judges the `statuteAnnotations` of a Default too …" toevoegen: "Since the card
`absence-markers` (2026-11-04) WF-127 holds a `PayloadAccess` under a test of its constructor (IR-SPEC ch. 4.7), with
one negative per arm." Gate 12, in de opsomming van de codes: geen; DROOLS-313 is een gewone weigering. Gate 15: na
"ROUTE LEG: on every golden with a `source.rgs` …" toevoegen: "A golden whose `ir.json` the Drools loader refuses
carries no `source.rgs` (`jutterswet-afwezigheidsmarkers`: its source is held by the frontend's suite)." Verder geen
wijziging; de vloeren blijven ondergrenzen.

### Wat stap B moet doen

1. Het Drools-model (`model`, `ir` in de frontend, gedeeld met Drools) een somtype geven, en `Construct`, de
   gelijkheid met een constructor en `PayloadAccess` laden en lowering geven, op beide branches; DROOLS-010 en
   DROOLS-313 voor deze vormen `RETIRED` of smaller.
2. De lezingen van ch. 5.12 voor `Construct`, `PayloadAccess` en de vergelijking met een marker (gate 17).
3. De golden `jutterswet-afwezigheidsmarkers` en de rode tweeling uit de kanalen van gate 12 en 8 halen; de bron uit
   `AbsenceMarkerTest.GOLDEN` als `source.rgs` in de golden zetten (gate 10, 15, 16), en de test inkorten tot wat
   gate 10 dan niet al vasthoudt.
4. Zodra de frontend-resolver het somtype in het gedeelde model zelf kan dragen: de zijtabel `absenceMarkerSites`
   vervangen door knopen in het model.

### Voorlopige besluiten

- VB-1 (spelling): `(of onbekend)`, `(of niet van toepassing)`, `(of onbekend of niet van toepassing)` als hele tokens
  op de plaats van `(mag opgegeven worden)`; schrijven als waarde, toetsen in beide woordvolgordes, "een waarde
  heeft", "die een waarde hebben". Geen ontkende toets.
- VB-2 (verlaging): het somtype met `PayloadAccess` als enige nieuwe knoop, niet een vlakke markering op het attribuut
  (zie de ontwerpnotitie; de Drools-loader leest een onbekend veld als documentfout).
- VB-3 (domein): één domein per attribuut, `<attribuut> van een <Objecttype>`, constructoren `waarde`, `onbekend`,
  `niet van toepassing`; een marker gaat niet stil over naar een ander attribuut, ook niet bij gelijke datatypes.
- VB-4 (WF-060): elk scalair datatype als payload, ook een enumeratie (WF-088 loopt dan door de declaratie); clingo
  draagt een getal en een datum in dagen, de rest weigert hij bij naam.
- VB-5 (wacht van WF-127): een gevuldtoets van dezelfde toegang als conjunct ervoor; geen ontkende vorm, geen
  kwalificator, geen toets onder `of` of een tellende kwantor; een toegang over een andere base dan een
  attribuutreferentie heeft geen wacht.
- VB-6 (bron van de golden): de bron staat in stap A in `AbsenceMarkerTest.GOLDEN` en in de NOTE, niet als
  `source.rgs`: gate 15 leest elke `source.rgs` terug door de Drools-loader (gemeten rood, boven). Alternatief: de
  routetest van gate 15 een geweigerde golden laten melden zoals de spec-tak doet — een wijziging van een
  Drools-test en van het gatescript.
- VB-7 (DROOLS-313): één weigering bij naam in de Drools-lader, omdat de Drools-suite elke knoop van het schema
  afloopt; geen andere wijziging in Drools.
- VB-8 (per periode): een marker schrijven, de invoer per dag, EF-016 per dag en de vergelijking per periode; de
  lezing van de waarde per periode weigert (ch. 3.12 rij 5).
- VB-9 (gate 7): case 4 en 8 kregen een afgeleide verwachting erbij, zodat ze `--noop` niet overleven, in plaats van
  een regel in de overleverslijst.
- VB-10 (namen): golden `jutterswet-afwezigheidsmarkers`, rood `jutterswet-afwezigheidsmarkers-botsing`, negatieven
  `wf127-<arm>`, fixtures `ref/test/fixtures/absence-markers/<geval>`; identifiers Engels, de constructoren Nederlands
  (domeininhoud).

### Normvraag (aggregatie over leden met markers)

Leest een aggregatie over leden met markers alleen de leden die een waarde hebben, met een filter dat de auteur
schrijft ("die een waarde hebben"), of slaat zij de markers over zoals een leeg lid, zonder filter? **Gebouwd: het
filter** (WF-127, RGS-376 zonder filter). Reden: het besluit "wie leest, toetst eerst" en "zodat niets stil
doorrekent"; overslaan maakt van een som met een onbekend lid stil de som van de bekende leden. Gevolg van het
alternatief: WF-127 krijgt een uitzondering voor de fold-bron, en ch. 1.2's overslaan van lege bijdragers gaat ook voor
markers gelden. Een tweede vraag in hetzelfde veld, nog niet gesteld: moet `aantal` (de telling van leden) markers
anders tellen dan waarden? Het leest geen attribuut, dus het filter is daar nu niet nodig.

### Mutaties (niet gecommit; `git worktree add --detach`, opgeruimd; `logs/18-mutations/`)

| mutatie | rood |
|---|---|
| V1 WF-127: de wachtarm meldt niets | gate 3: `wf127-unguarded`, `wf127-guard-after-read`, `wf127-fold-unguarded` `passed the verifier (must violate WF-127)` |
| V2 WF-127: de leden van een `all` bewaken elkaar in elke volgorde | gate 3: `wf127-guard-after-read` |
| V3 WF-088: de payloads van een somtypedeclaratie niet gelopen | gate 3: `wf088-sum-payload-drift` |
| V4 WF-127: een nullaire constructor gelezen als payload | gate 3: `wf127-constructor-nullary` |
| R1 ref: de payload van een tijdlijn scalair gelezen | `FAIL WV-9.5-per-dag-payload-of-a-timeline` |
| R2 ref: een som over de payloads van een tijdlijn gevouwen | **groen**: het filter van zo'n som leest dezelfde toegang en weigert eerst, met dezelfde zin; de weigering in de fold is een tweede lijn, die alleen een document zonder filter bereikt, en dat weigert de verifier (WF-127) |
| C1 clingo: twee constructoren in één symbool niet geweigerd | `… meet in one ASP symbol … expected a loud fault, got a model` |
| C2 clingo: geen projectieregels | `case1-schatting … V1.vindloon is underived (leeg), expected 3000` en meer |
| C3 clingo: een som over de payloads van een tijdlijn gevouwen | `… a sum over the payloads of a timeline is refused faults loudly (temporal attribute "opslagkosten" in a scalar slot …)`: de naald mist |
| C4 clingo: een somtijdlijn uit een andere producent geschreven | `… a copy of a timeline with markers is refused faults loudly (unscalable datatype …)`: de naald mist |
| F1 frontend: RGS-376 nooit gemeld | `a_sum_without_the_subselection_is_refused`, `a_comparison_before_the_test_is_refused`, `a_read_without_the_test_is_refused` |
| F2 frontend: de leden van een `alle` bewaken elkaar in elke volgorde | `a_comparison_before_the_test_is_refused` |
| F3 frontend: een waarde zonder haar `Construct` geschreven | `the_golden_is_the_emission_of_its_source` |

### make gates

Een run aan het eind in de worktree op `69867d173` (`out/gates/20261104-225938/`, log `gates-3.log`); de commit daarna raakt
alleen deze kaart. Twee runs ervoor: `gates-1` op `54b9879fa` had gate 12 rood (`UnknownNodeWitnessTest … expr
PayloadAccess: … unknown expr node "PayloadAccess"`) en gate 17 rood (`the numbering of /rules/rules/3/versions/0 does
not follow the document`), hersteld in `f377ce686` en `249285cbf`; `gates-2` op `249285cbf` was groen, en daarna
veranderde alleen DIVERGENCES.

```
start 2026-11-04 22:59:38 69867d173 card/absence-markers
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 403 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 563 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 135s
04-case-schema rc=0 7s
03-verifier rc=0 42s
05-oracle rc=0 25s
06-clingo rc=0 288s
07-noop rc=0 15s
08-red rc=0 175s
09-catala rc=0 5s
10-regelspraak rc=0 258s
11-toka-parity rc=0 241s
12-drools rc=0 360s
13-reftype rc=0 332s
14-flogic rc=0 2s
15-graph rc=0 52s
16-scenario rc=0 42s
17-readings rc=0 272s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 23:05:38 360s red=0
```

Gate 3: 446 goldens schoon, 624 negatieven op hun regel, 469 op hun arm. Gate 5: ref-suite 2444, 1088 cases (was 1078).
Gate 6: clingo-suite 2633, 1084 cases (was 1074), 2 goldens in het weigeringskanaal (ongewijzigd). Gate 8: 73 rode
cases (was 71), Drools-been 19 niet gedragen met reden, 17 met hun DROOLS-code. Gate 10: 1506 JUnit-tests, 194
rgs-goldens. Gate 12: JUnit 1849 tests, `played 338` op beide branches (ongewijzigd), 49 niet-gespeelde goldens bij
naam. Gate 17: 339 goldens en 949 case files vergeleken (ongewijzigd: de nieuwe golden is een `EXCLUDE`), ref
traceerde 1088 case files.


## Result van stap B

*Bouwer stap B, 2026-11-04. Niet gemerged, niet gepusht. Logs `werk/absence-markers/logs/b*.log` en `gates-b1.log`,
het mutatiescript `werk/absence-markers/scripts/mutations-b.py` (buiten de repo). Punt 1 tot en met 3 van "Wat stap B
moet doen" zijn gebouwd; punt 4 niet (vraag 1).*

### Commits

| hash | inhoud |
|---|---|
| `48711cc1c` | gedeeld model (`Datatype.SumType`, `Expr.Construct`, `Expr.PayloadAccess`) en de armen van resolver en emitter; Drools: lader, `ConstructorValue`, analyse op beide branches, lezingen, case-lezer, DROOLS-314, DROOLS-010/-012/-313 `RETIRED`, README; `AbsenceMarkerWitnessTest` en de aangepaste tests |
| `4161023d3` | corpus: de bron als `source.rgs`, NOTE's, `AbsenceMarkerTest` ingekort, DIVERGENCES |
| `968802179` | gates 8 en 12 (beschermd, voorstel): kanaal- en uitsluitingslijst, `EXPECTED_PLAYED` |
| `a789b00b8` | RS ch. 5.12 (de constructorwaarde in de canonieke tekst) en ch. 9.5 (Drools uitvoerder), voorstel |
| `eddd2503b` | getuigen voor twee bewakingen zonder rode mutatie (som zonder filter, segmentgelijkheid) |
| (deze) | Result |
| (laatste) | kaart naar `done/` |

### Wat er in Drools en de lezingen veranderde

- **Gedeeld model** (`frontends/regelspraak` `model` en `ir`, die de Drools-pom meecompileert): `Datatype.SumType(domain,
  constructors)` met `Datatype.Constructor(name, payload)`, `Expr.Construct(type, constructor, argument)` en
  `Expr.PayloadAccess(base, constructor, type)`. De resolver bouwt ze niet. De wandelaars van resolver, emitter en
  Drools lopen erdoorheen; de emitter weigert ze luid, zoals `IfThenElse`, omdat hij de markeringen uit
  `absenceMarkerSites` schrijft.
- **Lader.** `SumType` op de declaratie van een domein en op elke gebruiksplaats, `Construct` en `PayloadAccess`; een
  constructor-literal met de zin van WF-114 (`a SumType`); `PayloadAccess` als vouwbron voor som, minimum, maximum,
  eerste, laatste en de gevulde telling, niet voor telling en totaal (WF-037, als de verifier).
- **Waarde.** `ConstructorValue(constructor, payload)`: gelijk waar de tags één gedeclareerde naam zijn (genormaliseerd,
  zoals ref) en de payloads één waarde, getallen op waarde; de hash past daarbij (EQUALITY). Op een bean (veldtype
  `Object`), in een `Value` of `State`, en in een segment: `Period.value` is `Object` (getal of constructorwaarde,
  andere waarden geweigerd), `Period.number()` voor de numerieke lezers, en de gegenereerde `Segment` houdt
  `Object waarde` met naast de constructor van de runtime een van `(LocalDate, LocalDate, Object)`.
- **Analyse** (één plan, twee weergaven): `CONSTRUCTOR_VALUE`, `CONSTRUCT` (leeg argument: leeg), `PAYLOAD` (andere tag
  of leeg: leeg); de gelijkheid met een constructorwaarde door `TEXT_EQUAL`; een vouw over leden draagt de payload van
  elk lid bij (`MemberValue.payload`, in de DRL-tekst de route `PAYLOAD`). Per periode: een constructor van een
  literal op een tijdlijndoel van een somtype (`PERIOD_SCALAR`, `PERIOD_CLIPPED`, `PERIOD_WRITTEN_VALUES`), de
  vergelijking met een constructorwaarde per periode (`PERIOD_WHERE_EQUAL`), EF-016 per dag en de vereniging met de
  invoer door `Period.sameValue`; `PeriodDecisions.Span.input` is `Object`.
- **Lezingen (ch. 5.12).** `SessionDump.canonical` schrijft `"onbekend"` en `"waarde"(300)`, zoals ref. Op alle tien
  case files zijn de regels van beide branches gelijk aan ref's trace (`b/ref-*.txt` tegen `b/exec-*.txt` en
  `b/drl-*.txt`, nul verschillen); gate 17 vergelijkt de golden nu.
- **Case-lezer.** Een constructorwaarde in invoer, verwachting en tijdlijn; tag en payload tegen de declaratie
  (`constructorFits`, een fout van de case), zoals ref's lader.
- **Geweigerd bij naam:** de payload van een tijdlijn, scalair en als vouwbron (DROOLS-314, ref's zin); een `Match`
  (DROOLS-021); elke andere waarde op een tijdlijndoel van een somtype (bestaande codes; de kopie van een somtijdlijn,
  ref's fixture `copy-of-a-timeline`, met DROOLS-303, zoals clingo weigert).

### Rood eerst (letterlijk, per branch)

- Main `d9c218ce1` (`logs/b02-red-drools-main.log`): exec-model en DRL-tekst, ook met `--readings`, op de golden
  `EXCLUDE DROOLS-010: loader: form not carried by the v0 Drools backend: Datatype:SumType (unknown datatype node)`;
  `DroolsRedMain` op beide cases van de tweeling dezelfde regel.
- `AbsenceMarkerWitnessTest` op main in een losse worktree (`logs/b22-red-witness-main.log`): `Tests run: 4,
  Failures: 2, Errors: 2` (`Refusal vorm niet gedragen door de v0-Drools-backend: Datatype:SumType`; `expected:
  <FAULT> but was: <EXCLUDE>`; de weigering kwam als `IrLoader.Refusal`, niet als de verwachte uitsluiting).
- Na de bouw: `PLAYED 10` op beide branches; de tweeling `FAULT EF-016 {…"attribute":"taxatie","label":"V1"…}` en
  `{…"attribute":"opslagkosten","label":"V1"…}`.

### Mutaties (niet gecommit; één `git worktree add --detach`, opgeruimd; `logs/b29-mutations`, `b31`)

| mutatie | rood |
|---|---|
| M1 `ConstructorValue`: payload met `Objects.equals` | `a_constructor_value_compares_and_reads_as_ch_9_says`: `expected: <waarde(300)> but was: <Waarde(300.00)>` |
| M2 `payload` negeert de tag | idem: `expected: <null> but was: <300>` |
| M3 `construct` laat een lege payload door | idem, en de golden: `WritersConflict … schrijft waarde en … onbekend` |
| M4 `whereEqual` leest altijd gelijk | `a_timeline_of_constructor_values_is_compared_per_day` |
| M5 de canonieke tekst zonder aanhalingstekens | `the_readings_write_a_constructor_value_as_the_trace_does` |
| M6 de exec-model-bijdrage zonder payload | de golden: `ClassCast … ConstructorValue cannot be cast to … BigDecimal` |
| M7 de DRL-tekstbijdrage zonder payload | idem |
| M8 geen weigering van de scalaire payload van een tijdlijn | `the_payload_of_a_timeline_is_refused_by_name`: `expected: <PAYLOAD_ACCESS_OF_TIMELINE> but was: <TIMED_MEMBER_SINGLE_PLACE>` |
| M9 geen weigering van de vouwbron op een tijdlijn | eerst **groen**: het filter leest dezelfde toegang en weigert eerst (zoals ref's R2 in stap A); met de rij "som zonder filter" (`eddd2503b`) rood: `but was: <TEMPORAL_TOTAL_NON_NUMBER>` |
| M10 `constructorFits` aanvaardt een onbekende tag | `a_constructor_the_sum_type_does_not_declare_is_a_defect_of_the_case`: `NullPointerException` |
| M11 de lader laat `PayloadAccess` als telbron toe | `CountSourceArmWitnessTest.a_constructor_or_a_payload_as_count_source_is_refused` |
| M12 de vergelijking per periode van een somtijdlijn weigert | de golden en de weigeringsrij |
| M13 `Period.sameValue` op identiteit | eerst **groen**; met de rij segmentgelijkheid (`eddd2503b`) rood: `expected: <…=onbekend> but was: <…=Onbekend>` |

### Codes en pins (oud → nieuw)

- DROOLS-010 `datatype-sum`, DROOLS-012 `expr-construct` en DROOLS-313 `expr-payload-access`: `REFUSAL` → `RETIRED`
  (geen plaats raakt ze meer); nieuw DROOLS-314 `payload-access-of-timeline` (`REFUSAL`, parameter `attribute`), beide
  catalogi. De zin van DROOLS-038 (een limiet die blijft): "non-numeric timeline value … the generated Segment holds a
  BigDecimal waarde" → "a timeline value on '{attribute}' that is neither a number nor a constructor value is not
  carried by the v0 backend". Register 314 codes, 25 retired.
- Gate 12 `EXPECTED_CHANNEL`: `jutterswet-afwezigheidsmarkers` eruit; `anchor/jutterswet-beschikkingsom`,
  `jutterswet-penningbeschikking-catala`, `synth3-besluitschakel`, `synth3-letbeslag`, `synth3-standtarief`:
  `EXCLUDE DROOLS-010` → `EXCLUDE DROOLS-021` (de `Match`); `jutterswet-meldplichtspoor`: `EXCLUDE DROOLS-010` →
  `EXCLUDE DROOLS-008` (zijn `ListType`). `EXPECTED_PLAYED` 338 → 339; het kanaal 49 → 48 goldens.
- Gate 8: de Drools-uitsluiting van de tweeling eruit; het Drools-been 54 → 56 fault-gepind, 19 → 17 niet gedragen.
  Geen vloer verschoven.
- Tests die de kaart omdraait of aanpast: `UnknownNodeWitnessTest` (`Construct`, `PayloadAccess` en `SumType` uit de
  geweigerde vormen); `CountSourceArmWitnessTest` (`Construct` uit de lijst van niet-gedragen knopen, nieuwe rij: een
  `Construct` en een `PayloadAccess` als telbron weigeren met de zin van WF-037); `RouteWitnessTest` (+ `PAYLOAD`);
  `AbsenceMarkerTest` (de bron naar `source.rgs`, `the_golden_is_the_emission_of_its_source` weg, die gate 10 houdt, de
  onopgeloste-woordenlus weg, die `SemanticTokensCorpusTest` houdt); `PeriodDecisionsWitnessTest`,
  `TmsTimelineWitnessTest`, `FormsPlan` (de API: `Period.number()`, `MemberValue.payload`, de waarde per dag ook een
  constructorwaarde).

### Groen

Drools-suite 1857 tests, 0 skipped (was 1849); frontend-suite 1505 (was 1506: één emissietest weg); ref-suite 2444 en
clingo-suite 2633 ongewijzigd. Eén `make gates` op `eddd2503b` (`out/gates/20261104-235137/`, `gates-b1.log`); de
commits daarna raken alleen deze kaart:

```
start 2026-11-04 23:51:37 eddd2503b card/absence-markers
jar rc=0 3s drools-jar: rebuilt (11 source path(s) newer than the jar, …; 3s)
01-self-contained rc=0 2s     02-ir-schema rc=0 131s    04-case-schema rc=0 7s    03-verifier rc=0 41s
05-oracle rc=0 23s            06-clingo rc=0 283s       07-noop rc=0 14s          08-red rc=0 178s
09-catala rc=0 5s             10-regelspraak rc=0 253s  11-toka-parity rc=0 240s  12-drools rc=0 355s
13-reftype rc=0 326s          14-flogic rc=0 1s         15-graph rc=0 51s         16-scenario rc=0 40s
17-readings rc=0 270s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-04 23:57:35 358s red=0
```

Gate 12: `played 339` op beide branches, 48 niet-gespeelde goldens bij naam, JUnit 1857. Gate 17: 340 goldens en 959
case files vergeleken (was 339 en 949), 0 afwijkingen. Gate 15: routepoot 195 goldens met een `source.rgs` (was 194),
specpoot 350 van 387. Gate 10: 195 rgs-goldens. Gate 8: 73 rode cases.

### Voorgestelde tekst (huidige stand)

**RUNTIME-SEMANTICS** ch. 5.12 (de canonieke tekst van een constructorwaarde) en ch. 9.5 ("Executed"): in `a789b00b8`.

**CONSTRUCTS rij 36** (vervangt het voorstel van stap A): in de kolom v0→v1 delta "`SumType`/`Construct`/`Match` never
emitter-produced" → "`Match` never emitter-produced; the RegelSpraak frontend writes `SumType`, `Construct` and
`PayloadAccess` for an attribute with absence markers (row 47)"; "temporal sum carriage a recorded flank in the ASP
inventory" → "a sum-typed timeline carried in ref, the ASP backend and the Drools backend for the input, a write of a
constructor of a literal and a comparison per period"; achteraan: "**The payload access (2026-11-04, card
`absence-markers`):** `PayloadAccess` reads the payload of a unary constructor, leeg on another constructor or leeg
(RUNTIME-SEMANTICS ch. 9.5), and WF-127 holds it under a test of its constructor; WF-060 admits every scalar datatype as
a payload, and WF-088 walks the payload of a sum-type declaration. Executed in ref, the ASP backend and both branches of
the Drools backend (golden `jutterswet-afwezigheidsmarkers`); the Drools backend refuses a `Match` (DROOLS-021) and the
payload of a timeline (DROOLS-314)."

**CONSTRUCTS rij 47**, de laatste kolom (vervangt "Ref and the ASP backend play … Drools: step B of the card"): "Ref,
the ASP backend and both branches of the Drools backend play scalar and per period (writing, input, EF-016, the test
per period, the readings of ch. 5.12); the value read per period is refused (ch. 3.12 row 5)." De rest van de rij als
in het voorstel van stap A.

**CLAUDE.md**, gate 12: na "… the standing of a timeline target of two writers is their union with its input
(`Consequence.WritersUnion`)." toevoegen: "Since the card `absence-markers` (step B, 2026-11-04) both branches carry a
sum type (RUNTIME-SEMANTICS ch. 9): its value is a `ConstructorValue` on a bean, in a fact and in a segment, built by
`Construct`, compared by equality and read by `PayloadAccess`, also as the source of a fold over members; on a timeline
target of a sum type a rule writes a constructor of a literal in the periods of its condition, and a comparison with a
constructor value is read per period. A `Match` stays refused (DROOLS-021), and the payload of a timeline is refused by
name (DROOLS-314)." In de opsomming van de gepensioneerde codes, na "… DROOLS-302, DROOLS-104 and DROOLS-311":
", since the card `absence-markers` (2026-11-04) DROOLS-010, DROOLS-012 and DROOLS-313". Gate 15: geen wijziging; het
voorstel van stap A (een golden zonder `source.rgs`) vervalt, de golden draagt er nu een. Gate 17: na de alinea over
`readings-of-displaced-defaults` toevoegen: "Since the card `absence-markers` (2026-11-04) a constructor value is
written as its tag quoted and, for a unary constructor, its payload in parentheses (ch. 5.12)." Geen vloer verschuift.

### Voorlopige besluiten (stap B)

- VB-B1 (DROOLS-010): `RETIRED`, niet smaller. De lader draagt elk somtype dat de wire schrijft, met elke scalaire
  payload en een genest somtype; een lijst- of structpayload laat WF-060 niet toe en weigert onder DROOLS-008/009. Er
  blijft voor DROOLS-010 niets over; een `Match` weigert bij zijn eigen knoop (DROOLS-021), zodat vijf goldens van pin
  wisselen. Een `Match` dragen was voor de golden niet nodig en is niet gebouwd.
- VB-B2 (representatie): één waardeklasse `ConstructorValue` voor scalair en tijdlijn, en `Period.value` als `Object`
  in plaats van een tweede segmentklasse, zodat de bestaande per-dagmachine (vereniging, EF-016, knip, lezing) zonder
  tweede pad werkt; de numerieke lezers lezen `Period.number()`, die faalt op een constructorwaarde.
- VB-B3 (codes): één nieuwe code, DROOLS-314, in de zin van ref; elke andere vorm op een somtijdlijn weigert onder een
  bestaande code.
- VB-B4 (tag): de tag houdt de spelling van zijn bron (case of IR) en vergelijkt genormaliseerd, zoals ref; de case-lezer
  eist een gedeclareerde tag.

### Vragen

1. **Punt 4, de zijtabel `absenceMarkerSites`.** Het gedeelde model draagt nu `SumType`, `Construct` en
   `PayloadAccess`, maar de resolver typeert een attribuut met markeringen als zijn payloaddatatype, zodat "zijn taxatie
   gedeeld door 10" door de typering gaat; de emitter schrijft de somtypevorm uit de zijtabel. De tabel vervangen door
   knopen vraagt dat de resolver elke lezing als `PayloadAccess` bouwt en typeert, de toets als `Construct`, en RGS-376
   op die knopen zet: een frontendkaart van eigen omvang. Een eigen kaart?
2. **Pin-bump van de distributie.** `Period.value`, `Segment.getWaarde()` en `PeriodDecisions.Span.input` zijn nu
   `Object`. De `Verbaliser` van de toolchain neemt de invoer al als `Object` (compileert), maar hoe hij een
   constructorwaarde van een somtypeattribuut weergeeft, is werk voor de toolchainkaart.
3. **De kopie van een somtijdlijn** (ref's fixture `copy-of-a-timeline`): ref speelt hem, clingo en Drools weigeren bij
   naam. Dragen, of zo laten tot een golden erom vraagt?
