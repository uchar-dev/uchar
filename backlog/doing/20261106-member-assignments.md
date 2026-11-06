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
