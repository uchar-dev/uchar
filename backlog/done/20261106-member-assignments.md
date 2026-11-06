# member-assignments: de leden van een objectcreatie zijn toekenningen, geen initialisaties

*Opgesteld door de hoofdsessie op 2026-11-06, na de toolchain-kaart `creation-value-wording` (toolchain `f83e89ace`).
Besluit van de eigenaar, 2026-11-06: draad en proza in één kaart ("Draad en proza", de aanbevolen optie). De
beschermde paden die deze kaart raakt (`spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`, `spec/verifier/`,
`scripts/`, `toka/`) hebben daarmee mandaat; de landing wacht op de review van de eigenaar.*

## Aanleiding

De toolchain noemt de waarde die een objectcreatie een attribuut van het gemaakte object geeft sinds 2026-11-06 "de bij
de objectcreatie toegekende waarde", met de woorden van de RegelSpraak-specificatie v2.3.0: het construct heet
ObjectCreatie (§4.3, §9.3, §13.4.6), en elk lid een waardetoekenning (§13.4.6.3 attribuutwaarde, §13.4.6.4
kenmerkwaarde). ALEF zegt hetzelfde ("Tijdens creatie kan aan attributen een waarde worden toegekend",
`ALEF/docs/regels/Actie_Objectcreatie.md`). In BRM, de handboeken en ALEF is een begin- of standaardwaarde een
terugvalwaarde voor een leeg gegeven: de rol van de initialisatie (§9.6).

uchar wijkt daar op twee plaatsen van af:

1. **Op de draad.** De leden heten `ObjectCreation.initialisations`, een lijst van `MemberInit` (IR-SPEC ch. 3.3), en de
   regelsoort Initialisatie is `RuleVersion.initialisation` (ch. 3.1, §9.6). Hetzelfde woord voor twee constructs die in
   de stapel van RUNTIME-SEMANTICS ch. 7.8 direct onder elkaar staan: rang 2 is de waarde uit de creatie, rang 3 de
   initialisatie. v0 noemde de leden `toekenningen`/`Toekenning`; de v1-hernoeming (M5, M8; `spec/proposals/
   v1-naamtabel.json`) verliet dat woord. Het proza volgt de draad: "member initialisation" staat op 42 regels in 24
   bestanden, en IR-SPEC (rond r. 9739, de ASP-weigering) zegt "refuses such an initialisation", waar de lezer niet ziet
   welke van de twee bedoeld is.
2. **In het proza.** "the value of its making" (ch. 7.8), "at its making", "after the making", "the fault at the
   making", "moment of making": 54 regels in spec en code. Het is een calque zonder bron, en de toolchain-vertaling ervan
   ("de waarde bij het maken") begreep geen lezer.

## Goal

- **Draad (stap L7).** `ObjectCreation.initialisations` wordt `assignments`, de knoop `MemberInit` wordt
  `MemberAssignment`; de velden van het lid (`member`, `labels`, `value`, `characteristic`) blijven. Schema, verifier,
  ref, clingo, de Drools-loader en de frontends die een creatie schrijven (de RegelSpraak-emitter en Catala; F-logic schrijft er geen)
  en het hele corpus gaan in één commit mee, zoals L6: loaders, emitters, schema en corpus kunnen niet los groen zijn.
  Een migratiescript `spec/proposals/v1-migratie/m7.py` doet de corpusstap en faalt luid op een vorm die het niet kent.
  `toka/` migreert mee (gate 11, de regressievorm); `toka/README.md` noemt L7 bij de bewuste afwijkingen.
- **Identifiers.** De Java- en OCaml-namen volgen de draad (`ResolvedObjectCreation.initialisations`,
  `MEMBER_INITIALISATION_INCOMPLETE` en zijn sleutel `member-initialisation-incomplete`, de constructors en functies in
  `ast.ml`, `loader.ml`, `checks.ml`, `eval.ml`, `translate.ml`, `IrLoader`, `TmsTranspiler`, `Consequence`, …): 61
  codebestanden noemen het nu. Een registercode houdt haar nummer.
- **Meldingen.** Elke melding die een lid van een creatie "initialisation" noemt, noemt het een toekenning:
  WF-027 (`member initialisation without a member name` → `member assignment …`, `characteristic initialisation without
  a value` → `characteristic assignment …`), DROOLS-150, -152, -153 (en hun Nederlandse tekst), RGS-187 (die in de
  Nederlandse catalogus Engelse tekst draagt en "characteristic initialisation" zegt), en RGS-155 ("kent '{attribute}'
  toe bij het aanmaken" → "… bij de objectcreatie"). De `expect.txt` van de negatieven pinnen sleutels en parameters,
  geen tekst; een gewijzigde sleutel gaat met zijn pins mee.
- **Proza.** IR-SPEC ch. 3.3 (met de regel "the wire has written it as `MemberAssignment` since L7; `MemberInit` from
  L5, v0 `Toekenning`") en elke andere plaats; RUNTIME-SEMANTICS ch. 5.5, 7.8 en de rest: "the value of its making" →
  "the value its creation assigned", "at its making" → "at its creation", "the fault at the making" → "the fault at the
  creation", "member initialisation" → "member assignment". CONSTRUCTS krijgt de delta (de draadnaam, L7), de naamtabel
  de rijen. READMEs, `NOTE.md` van goldens en commentaar als huidige stand. Geschiedenis (`backlog/done/`,
  `spec/proposals/v1-migratie/m1–m6`) blijft zoals ze is.
- **Geen semantiek.** Geen waarde, geen verwachting en geen pin van een bestaande vorm beweegt; de enige verschillen in
  het corpus zijn de twee namen.

## Acceptance

- (a) Het migratiescript loopt droog met een telling: bestanden, `ObjectCreation`-versies, leden; daarna is
  `git grep -E '"initialisations"|"MemberInit"'` buiten de geschiedenis leeg, en idempotent (een tweede run verandert
  niets).
- (b) Elke `ir.json` van golden, red en negative valideert tegen het schema en rondreist byte-gelijk (gate 2, met de
  nieuwe sleutels in de canonieke volgorde); de zelftestrijen van `check-ir-schema.sh` noemen de nieuwe namen, met een rij
  die `initialisations` en `MemberInit` nu weigert.
- (c) Gate 10: elke golden met `source.rgs` emitteert byte-gelijk met de gemigreerde `ir.json`; gate 9 idem voor
  Catala, en gate 14 blijft groen.
- (d) De replay van ref, clingo en beide Drools-takken geeft over het corpus dezelfde uitkomsten als vóór de kaart; de
  lezingen van gate 17 zijn regel voor regel gelijk op de namen na, als die erin staan.
- (e) `git grep -i -E 'member initiali[sz]ation|of (its|the) making|at (its|the) making|the making\b'` buiten
  `backlog/done/`, `spec/proposals/v1-migratie/` en de v0-namen die als v0-naam staan, is leeg.
- (f) `make gates` groen; floors die de hernoeming raakt, verlaagd of gelijk met reden in dezelfde commit.
- (g) Toolchain: pin-bump naar de landing, `make gates` groen; het Engelse proza "the value of the making" in README,
  `docs/ENGINE.md` en commentaar volgt (de identifiers `CaseSession.making` en de JSON-waarde `creation` blijven, tenzij
  de eigenaar anders zegt).

May touch: `spec/` (IR-SPEC, RUNTIME-SEMANTICS, CONSTRUCTS, ASP-TRANSLATION, schema, verifier, naamtabel, `v1-migratie/m7.py`),
`ref/`, `backends/`, `frontends/`, `corpus/`, `toka/`, `scripts/` (alleen namen in zelftestrijen en commentaar), READMEs,
kaartverplaatsing. Niet: semantiek, nieuwe vormen.

## Omvang

Gemeten op `74119a23e`: `"initialisations"`/`"MemberInit"` in 27 golden-, 36 negative- en 4 red-documenten, 2 in
`toka/`, 6 ref-fixtures, 9 Drools-testresources en 3 in `frontends/` (85 `MemberInit`-knopen in het corpus); het schema
(`$defs/MemberInit`, de versie van `ObjectCreation`); 61 codebestanden; 42 regels "member initialisation", 54 regels
"making" in deze zin. Groot in aantal, mechanisch van aard. Stappen, elk een eigen bouwer: (1) `m7.py` met de
droogloop en de naamtabel; (2) de draadstap L7 in één commit (schema, loaders, emitters, corpus, toka, zelftestrijen);
(3) identifiers, registersleutels en meldingen; (4) proza; (5) gates, review van de eigenaar, landing; (6) pin-bump van
de toolchain.

## Ook gevonden (2026-11-06)

De Nederlandse catalogus van de frontend (`messages-frontend.nl.tsv`) heeft ongeveer 39 entries met Engelse tekst
(RGS-018, -035, -036, -037, -080, -087, -104, …, RGS-187, RGS-334, RGS-337; gemeten met een heuristiek op Engelse en
Nederlandse functiewoorden). Gate 10 (`check-rgs-codes.sh`) toetst Engelse entries op Nederlandse woorden, niet
omgekeerd. RGS-187 hoort bij deze kaart; de andere zijn een eigen stap als de eigenaar ze in deze landing wil.

## Result

*Bouwer (opus55-ttl5min, subagent), stappen 1–4 en een fixronde; boekhouding en Result door de hoofdsessie,
2026-11-06. Volledig verslag, tellingen en valkuilen: `werk/member-assignments/OVERDRACHT.md` (buiten de repo), logs
in `werk/member-assignments/logs/`.*

### Commits

| hash | inhoud |
|---|---|
| `6ea4f0f44` | kaart naar `doing/` |
| `39efeccc9` | stap 1: `spec/proposals/v1-migratie/m7.py` |
| `ce1b42453` | stap 2, L7 (één commit): schema, lezers en schrijvers van de draad, corpus, `toka/`, fixtures, zelftestrijen, IR-SPEC ch. 3.3, naamtabel (M10) |
| `5e7bdb2dd` | stap 3: identifiers, registersleutels, meldingen |
| `1a9ae5a0a` | stap 4: proza |
| `842a5c976` | fixronde: RGS-155 zonder dubbeling; 85 resterende "initialis…" in de zin van een lid, in 41 bestanden |
| (deze) | Result; CONSTRUCTS rij 8, 9, 21, 28 en 142, PLAN; de kaart naar `done/` |

### Wat er veranderde

- **Draad (L7).** `ObjectCreation.assignments`, een lijst van `MemberAssignment` (`member`, `labels`, `value`,
  `characteristic` ongewijzigd). `m7.py`: 1246 documenten gelezen, 78 herschreven (88 versies, 98 leden; golden
  27, negative 36, red 4, toka 2, ref-fixtures 5, Drools-testresources 4), 0 geweigerd, idempotent (`--check`). Het
  script faalt luid op een oude naam op een andere plaats. `toka/casus/ir.json` her-emitteert byte-gelijk (gate 11);
  `toka/README.md` noemt L7. `check-ir-schema.sh` weigert de oude namen in vier rijen, de clingo-suite in twee.
- **Namen en meldingen.** Java en OCaml volgen de draad (`ResolvedObjectCreation.MemberAssignment`/`assignments`,
  `Consequence.Assignment`, ref `assigned_member`, verifier `assigns`, …). Registersleutels met behoud van nummer:
  DROOLS-150 `member-assignment-incomplete`, -151 `characteristic-assignment-non-boolean`, -152
  `member-assignment-division`, -153 `member-assignment-square-root`, CLINGO-027
  `characteristic-assignment-expression-shape`; de toolchain noemt geen van die sleutels. Meldingen: WF-027
  (sleutels gelijk), DROOLS-150..153 (Nederlands overal "lidtoekenning"), RGS-187 (de Nederlandse entry is nu
  Nederlands), RGS-155 ("kent '{attribute}' een waarde toe"), en de interne meldingen van ref, clingo, Catala en
  `Consequence`.
- **Proza.** "member assignment", "the value its creation assigned", "at its creation", "moment of creation" in
  IR-SPEC, RUNTIME-SEMANTICS, ASP-TRANSLATION, READMEs, `NOTE.md`, commentaar en tests. De Initialisatie-regel
  (§9.6) en de v0-namen bleven. Bewust als geschiedenis gelaten: de L4-tabel in `corpus/negative/README.md`, de
  L5-lijst in `spec/schema/README.md`, de Landed-noten in CONSTRUCTS (rij 142 L5, r. 107).
- **Boekhouding (hoofdsessie).** CONSTRUCTS: de L7-noot bij rij 142 (`ResolvedObjectCreatie`); rij 8 (de stapel),
  9, 21 en 28 in de nieuwe woorden. PLAN: de vier regels over de creatie in generaties.

### Acceptatie

- (a) Droogloop met telling, daarna geen `"initialisations"`/`"MemberInit"` buiten de geschiedenis; idempotent.
- (b) Gate 2 groen (530 shape-rijen), met de vier weigerrijen.
- (c) Gate 10 en gate 9 groen: elke `source.rgs` en elke Catala-golden her-emitteert byte-gelijk; gate 14 groen.
- (d) Gates 5, 6, 12 en 17 groen: geen uitkomst en geen lezing veranderde.
- (e) `git grep -i -E 'member initiali[sz]ation|of (its|the) making|at (its|the) making|the making\b'` buiten
  `backlog/` en `spec/proposals/v1-migratie/` is leeg (ook in CONSTRUCTS, PLAN en CLAUDE.md, na de boekhouding).
- (f) `make gates` op de bouwerstand (`1a9ae5a0a`): 17 van 17 groen, `after-run` groen, 369 s; geen floor geraakt.
  Op de eindstand (`85a2b4807`, 2026-11-06 12:06–12:16): de drie builds fresh, gates 01 tot 17 elk `rc=0`,
  `after-run` groen, `red=0`, 612 s (`out/gates/20261106-120619/`).
- (g) Toolchain: pin-bump na de landing.

### Open punten

1. `spec/proposals/v1-naamtabel-toets.py` (geen gate) was op `079d0104e` al rood met 45 bevindingen, niet van deze
   kaart; na de kaart 42 (drie opgelost, geen nieuwe). Niet uitgezocht.
2. `ReaderSentenceBoundaryTest` houdt "initialisation" in zijn Engelse woordenlijst: daar is het een woord, geen lid.
