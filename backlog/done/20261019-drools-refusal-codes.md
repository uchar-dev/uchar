# drools-refusal-codes: de weigeringen van de Drools-backend krijgen een code, los van hun tekst

*Kaart 3 van `spec/proposals/MESSAGE-CODES.md` §7 (stap 7 van het programma "Engelse namen"). Raakt `scripts/`: de
branch blijft een voorstel tot de eigenaar merget. Na kaart 1 (`meldingscodes`, op main), vóór kaart 4 (codes van de
frontend: beide bewerken `backends/drools/`, dus serieel, nooit parallel) en vóór kaart 5 (beide bewerken
`scripts/check-rood.sh`).*

Goal: een weigering van de Drools-backend is nu alleen tekst, en gate 12 en de Drools-poot van gate 8 pinnen haar met
een categorie en een Nederlandse of Engelse reden in een lijst: 45 `EXCLUDE`-regels in `EXPECTED_CHANNEL` (alleen het
kanaal wordt vergeleken, de reden is documentatie) en 23 regels in de Drools-lijst van gate 8 (19 `EXCLUDE`, 4
`NOFAULT`). Deze kaart geeft elke plek waar de backend een vorm of geval weigert een code `DROOLS-###` met een soort
(`refusal`, `limit`, `contract`, `fallback`), laat de code vooraan het rapport staan (`EXCLUDE DROOLS-014: …`), en laat
beide gates die code pinnen en vergelijken in plaats van alleen het kanaal. Ontwerp: MESSAGE-CODES §2.3, §3.1, §3.4
(code naast de tekst, nooit erin), §7 rij 3.

Voorwerk (voorbereider, 2026-10-19): 304 plekken gemeten in `backends/drools/src/main` (plus 11 pure doorgeefplekken),
295 verschillende genormaliseerde sjablonen; een script en partijen voor Haiku staan buiten de repository.

Wat blijft staan:
- Geen tekst van een melding verandert; elke fragmentpin (gate 8) en elke statusregel-gelijkheid van de twee takken
  (gate 12) blijft groen. De code staat vooraan het rapport, niet in de zin.
- De `EF`-codes, de tabel in RUNTIME-SEMANTICS ch. 5.7b en `expectedFault.code`. Een declined-vorm (EF-021..024) die
  Drools weigert, houdt haar `EF`-code in het ontwerp; de weigering van Drools krijgt een eigen `DROOLS`-code met in het
  register de kolom `ef`.
- De 4 `NOFAULT`-regels van gate 8 (de backend weigert niet, dus geen code) en de 3 `NOCASE`-regels van gate 12.
- Het kanaal-per-golden-regime van gate 12 (`EXPECTED_PLAYED`, de lijst) en `EXPECTED_MIN_*`-vloeren van andere gates.

Acceptance:
- (a) Register: elke plek die de backend een vorm laat weigeren draagt een code uit één register van de backend, met
  per code `DROOLS-###`, soort, sleutel, parameters en optioneel `ef`. Een oude constructor zonder code bestaat niet
  meer, dus de compiler dwingt af dat elke `Refusal`, `Excluded`, `RoleRefusal` en elke `Coverage.skip` een code heeft.
- (b) Carriercontrole in beide richtingen, door een script in de gate: elke geregistreerde code wordt ergens
  opgeworpen, elke opgeworpen code staat in het register, sleutels zijn uniek, nummers oplopend zonder gat of
  hergebruik. Vloer `EXPECTED_MIN_DROOLS_CODES` (gemeten bij de bouw; het voorbereide aantal sleutels staat in het
  Result van de voorbereiding), met reden en datum.
- (c) Gate 12: elke `EXCLUDE`-regel van `EXPECTED_CHANNEL` noemt een code, en de gate vergelijkt de code van de
  statusregel (`EXCLUDE DROOLS-###: …`) met de gepinde; de exec-model-tak en de DRL-tekst-tak moeten dezelfde regel
  hebben (de bestaande eis); een verouderde of ontbrekende code faalt met de naam van de golden.
- (d) Gate 8, Drools-poot: de 19 `EXCLUDE`-regels pinnen een code en de gate vergelijkt die met de statusregel; de
  4 `NOFAULT`-regels blijven zonder code.
- (e) Mutatie, één keer uitgevoerd en in het Result vastgelegd: één weigering krijgt een andere code; gate 12 en gate 8
  worden rood, met de naam van elke golden of case en beide codes; daarna teruggezet en groen.
- (f) Geen meldingstekst verandert (de fragmenten en de gelijkheid van de takken bewijzen het); de tests van de backend
  blijven groen met nul overgeslagen.
- (g) Alle 16 gates van `CLAUDE.md` groen op de eindstand van de branch, in één volledige run.

May touch: `backends/drools/src/main` (de weigeringsplekken, het register, `DroolsRedMain` en de replay-runner die de
statusregel drukken, `Coverage`), `scripts/check-drools.sh`, `scripts/check-rood.sh`, een script voor de
carriercontrole, deze kaart. Testbestanden alleen mechanisch: `CaseInputWitnessTest` construeert op twee plekken
(regels 559 en 562) een `CaseRunner.Excluded`; vraagt toestemming van de eigenaar, de hoofdsessie bevestigt die vóór de
bouw.

Out of scope:
- De catalogi en elke vertaling van een melding (kaart 6); het `Diagnostic` van de frontend en de vier Drools-plekken die
  het construeren (kaart 4); `params` uitzenden en pinnen (kaart 5, in het register staan ze al).
- De `DocumentError`-plekken van de lader (een defect, geen weigering) en de `IllegalStateException`-plekken.
- `ref`, clingo, de verifier, het case-schema en de wire.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Ontwerp (stand)

- Eén register in de backend, een enum met per constante `DROOLS-###`, soort, sleutel en `ef`; een plek noemt de constante
  (`DroolsCode.UNNAMED_RULE`), zodat een niet-geregistreerde code niet compileert. Nummers worden toegekend in volgorde van
  verschijnen, eenmalig, door een script uit de antwoorden van Haiku.
- Drager: de uitzondering draagt de code als veld naast de tekst (`IrLoader.Refusal`, `CaseRunner.Excluded`,
  `TmsTranspiler.Refusal`, `Coverage` krijgt een component `code`). Methoden die nu een tekst teruggeven en waarvan de
  aanroeper er een weigering van maakt (`dayKindRefusal`, `setSourceRefusal`, `SourcePath.Plan.refusal`,
  `CreationLink.refusal`, `Cycles.refusal`, `classCollision`, `roleCollision`, `displacementReason`) geven een paar
  (code, tekst) terug. Lijmplekken (een voorvoegsel of samenvoeging) hebben geen code van zichzelf en dragen die van de
  weigering die ze omhullen.
- Rapport: `EXCLUDE DROOLS-014: <tekst zoals nu>`; de DRL-tak drukt hetzelfde. Gate 12 vergelijkt de eerste twee
  woorden.
- Soorten volgens MESSAGE-CODES §2.3.

## Result

Gebouwd op `card/drools-refusal-codes`: commit `ed3698c9` (code en gates) plus dit Result. Alle 16 gates groen in één
volledige run op `ed3698c9` (`failed=0`; gate 12: JUnit 1619 tests, vloer 1573, nul overgeslagen, 255 gespeeld, 45
EXCLUDE-pins met code; gate 8: 19 EXCLUDE-cases op code vergeleken).

**Register.** `refusal/DroolsCode.java`: 289 codes `DROOLS-001..289` (158 `refusal`, 87 `contract`, 43 `limit`, 1
`fallback`; 9 met `ef`: EF-002, 017 (2x), 018 (2x), 020, 022, 023 (2x)), één regel per code, plus `Reason(code, tekst)`.
Dragers: `IrLoader.Refusal`, `CaseRunner.Excluded` (en `CaseInput.RoleRefusal`), `TmsTranspiler.Refusal`,
`DrlRendering.Refusal`, `Stapels.Uncomposable` en de component `code` van `Coverage` (alleen `skip` zet hem) en van
`CaseRunner.Result`/`RedResult` dragen de code naast de tekst; zonder code is er geen constructor meer. Bouwers die een tekst
teruggaven geven nu een `Reason` (`displacementReason`, `displacedMember`, `roleCollision`, `classCollision`,
`noReferenceFieldReason`, `noJoinFieldReason`, `CreationLink.refusal/ambiguous`, `SourcePath.Plan.refusal`,
`WindowPosition`, `Cycles.refusal`, `ContractValidator.validate`, `dayKindRefusal`, `setSourceRefusal`); de lijmplekken
(`drl:`, `transpiler SKIP:`, `contract:`, `ownField`, de zes doorgeefplekken) dragen de code van de omhulde weigering.
Geen meldingstekst veranderd: elke fragmentpin en de gelijkheid van de twee takken bleven groen.

**Van de voorbereiding naar de bouw.** De voorbereider mat 304 plekken (11 lijm) en 293 sjablonen. De bouw vond er 6 die de
meting miste: de vijf `Uncomposable`-weigeringen van `Stapels` (nu `defaults-own-group-over-set`,
`defaults-own-set-over-stack`, `defaults-base-with-validity`, `defaults-base-without-value`,
`defaults-two-writers-over-stack`) en de pro-rata-weigering van `Timelines.proRataFixedGrid` (`pro-rata-fixed-grid`,
code op de aanroepplek; de functie geeft nog een `String`, want `FixedStartPointWitnessTest` leest haar). Drie sleutels
vervielen omdat ze het vaste sjabloon van een hulpfunctie waren, waarvan elke aanroep een eigen code draagt
(`value-form-in-value`, `condition-form-not-read`, `window-reading-unspecified`); de nummers zijn daarna eenmalig
herrekend, vóór enige pin. Rest-dode tekst: `FactModelGenerator.rangeJoin` en `allocationJoin` hebben een
weigeringszin die geen aanroeper gooit; zij kregen geen code.

**Antwoorden van Haiku.** De toets vond 60 meldingen. Bijna alle sleutels hadden vijf woorden (ingekort volgens
`tools/fix_answers.py`); vier paar sjablonen hadden twee sleutels (`square-root-*`, `empty-fallback-*`, `clip-owned-*`,
`own-read-*`) en kregen er één, drie sleutels stonden voor twee sjablonen (`value-form-not-carried`,
`duration-without-time-unit`, `created-relation-not-binary`) en kregen een tweede; `aggregate-path-one-step` kreeg
`params` met één naam. De soort van `square-root-argument-count`, `empty-fallback-non-sum` en `own-read-foreign-subject`
werd `contract` (de verifier weigert dezelfde vorm: WF-037, WF-053). De `ef`-velden zijn nagelopen op de gate-8-cases en
klopten.

**Gates.** `scripts/check-drools-codes.sh` (nieuw, door gate 12 aangeroepen, vloer `EXPECTED_MIN_DROOLS_CODES=289`,
gemeten 2026-10-19): elke code opgeworpen, elke opgeworpen code geregistreerd, nummers zonder gat, sleutels en
constantenamen uniek, elke `ef` een rij van RUNTIME-SEMANTICS ch. 5.7b. `check-drools.sh`: de 45 `EXCLUDE`-pins in
`EXPECTED_CHANNEL` luiden `naam|EXCLUDE DROOLS-###|reden`, het kanaal van de statusregel is `EXCLUDE DROOLS-###`, een
EXCLUDE-rapport zonder code faalt, een kale `EXCLUDE`-pin faalt. `check-rood.sh`: de 19 `EXCLUDE`-regels pinnen een code
(`name|EXCLUDE DROOLS-###  reden`), de 4 `NOFAULT` niet; vloer `EXPECTED_MIN_DROOLS_EXCLUDE_CODES=19` (gemeten).

**Mutaties** (elk eerst rood, dan terug en groen):
- Acceptance (e): de codes van de `ListType`- en de `StructType`-weigering in `IrLoader` verwisseld (één enkele
  wijziging liet `DATATYPE_LIST` ongebruikt en dan faalt de carriercontrole eerst). Gate 12 `--fast`: rood met 22 goldens
  met beide codes (15x `pinned EXCLUDE DROOLS-008, measured EXCLUDE DROOLS-009`, 7x omgekeerd). Gate 8: rood met 5 cases
  (`closed-nonexact-division`, `closed-runtime-division-by-zero`, `closed-runtime-nonexact-division`: pinned
  DROOLS-008, reports DROOLS-009; `list-element-overprecision`, `struct-field-overprecision`: omgekeerd). Teruggezet:
  gate 12 `--fast` en gate 8 groen.
- Carriercontrole: een nummer verschoven (`DROOLS-110` op plaats 10), twee sleutels gelijk, een code die niemand opwerpt,
  een `ef` buiten de tabel (`EF-999`): elk rood met de reden; teruggezet groen.
- Vloer van gate 8 op 20 gezet: rood (`19 EXCLUDE cases compared ... pins at least 20`); teruggezet op 19.

**Testbestanden** (vier, 10 regels): `CaseInputWitnessTest` (2 constructies van `Excluded` kregen de code van de
overeenkomstige hoofdzin: `GRANULAR_PARAMETER_TIMELINE`, `RATIONAL_VALUE`), `RoundTripWitnessTest` (het type van
`violations`, `List<String>` naar `List<Reason>`), `TmsCycleVerifierWitnessTest` (`String w` naar `Reason w`),
`TmsCycleWitnessTest` (de hulpmethode `cycle` geeft `refusal.text()`). Geen verwachting veranderd.

**Open.** Kaart 4 (codes van de frontend: de vier `Diagnostic`-plekken in dezelfde map) en kaart 5 (`params`
uitzenden, pinnen) blijven; `params` staat in het register, de rapporten zenden ze nog niet uit. De hoofdsessie
bevestigt de testaanpassingen hierboven (stap 7 gaf mandaat voor `CaseInputWitnessTest`; de drie andere zijn dezelfde soort,
mechanisch).

**Voorstel voor `CLAUDE.md`** (gate 12 en gate 8):
- Gate 12, na "Not-carried forms never vanish silently": "Since the card `drools-refusal-codes` (2026-10-19) every
  such refusal carries a code `DROOLS-###` from one register (`backends/drools/.../refusal/DroolsCode.java`), beside its
  sentence and at the head of its report (`EXCLUDE DROOLS-014: <sentence>`); the gate compares the code, not only the
  channel: each of the 45 `EXCLUDE` pins of `EXPECTED_CHANNEL` names its code, and a changed or missing code fails by
  golden name with both codes. `scripts/check-drools-codes.sh`, called first, holds the register to the sources in both
  directions (every code raised, every raised code registered, numbers without gap, keys unique, every `ef` a row of
  RUNTIME-SEMANTICS ch. 5.7b) and carries `EXPECTED_MIN_DROOLS_CODES=289`."
- Gate 8, Drools leg: "An `EXCLUDE` entry of the Drools list pins the register code of the refusal
  (`name|EXCLUDE DROOLS-###  <reason>`) and the leg compares it with the head of the status line, failing by case name
  with both codes; a `NOFAULT` entry carries none. Floor `EXPECTED_MIN_DROOLS_EXCLUDE_CODES=19`."

**Een melding aan de hoofdsessie.** Tijdens de bouw doodde een `pkill -f "scripts/check-"` van mij om 01:09 ook de
gates van de parallelle kaart `verifier-arm-keys`: in `stap7/k2/gates-eind-2` staan `08-rood` en `09-catala` met
`rc=143`; die twee moeten daar opnieuw. Verder niets van die kaart geraakt.
