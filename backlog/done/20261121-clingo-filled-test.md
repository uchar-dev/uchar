# clingo-filled-test: clingo vertaalt de toets op gevuld en leeg van een waarde die geen getal of datum is

*Opgesteld door de hoofdsessie op 2026-11-21. Een bestaande fout, gevonden bij de proef met de strikte lezing van
leeg: de omgezette documenten van die proef schrijven veel toetsen op gevuld, en clingo weigerde er elf cases op.*

## Aanleiding

De vertaling van clingo leest de operand van een toets op leeg of gevuld (`Predicate` met `variety` `Empty_predicate`
of `Filled`) met `expr_term`, tenzij de operand een record is. `expr_term` schaalt elke waarde met
`decimals_of_datatype`, en die functie kent alleen een getal, een percentage en een datum; voor elk ander datatype
valt zij met "unscalable datatype in the ASP translation", zonder registercode (`backends/clingo/lib/translate.ml`,
de arm van de toets en `decimals_of_datatype`). Een regel als "Een vondst is ingedeeld indien zijn categorie gevuld
is", met een categorie van een enumeratie, speelt daardoor in ref en niet in clingo, en de weigering staat in geen
register. Voor een enumeratie of tekst bestaat in de vertaling `symbolic_term`, voor een sum type `sum_term`; de arm
van de vergelijking gebruikt die al.

Geen golden draagt zo'n toets: gate 6 eist dat een case speelt of met een registercode weigert, en zou hierop rood
zijn.

## Goal

Een toets op leeg of gevuld speelt in clingo voor elk datatype dat de vertaling als waarde kan binden: een
enumeratie, een tekst, een boolean, en een attribuut met afwezigheidsmarkeringen (sum type), naast het getal, het
percentage en de datum van nu. Wat dan nog niet te vertalen is, weigert met een registercode en niet met de kale zin.

## Acceptance

- Meet eerst, vóór je iets wijzigt, per datatype dat het schema voor een attribuut kent en per vorm van de operand
  (een attribuut van het onderwerp, een attribuut over een rol, een parameter) wat de toets op gevuld en op leeg in
  clingo nu doet: speelt, weigert met code, of valt met de kale zin. Zet de tabel in het Result.
- Een nieuwe golden uit de Jutterswet, `corpus/golden/jutterswet-gevuldtoets-soorten/` (of een naam die beter past),
  met `source.rgs`, `ir.json` en cases: regels die op gevuld en op leeg toetsen van een enumeratie-, een tekst- en een
  boolean-attribuut en van een attribuut met een afwezigheidsmarkering, elk met een case waarin het attribuut gevuld is
  en een waarin het leeg is, en voor de markering een case met de marker zelf (een gevulde waarde, IR-SPEC en
  RUNTIME-SEMANTICS ch. 9.5). Verwachtingen met de hand berekend. Rood eerst: de golden faalt in clingo op de code van
  `main` met de kale zin.
- De golden speelt in ref (gate 5), clingo (gate 6) en beide Drools-branches (gate 12); draagt Drools een van de
  vormen niet, dan is dat een uitkomst: zeg welke en met welke code, en snijd de golden zo dat wat Drools draagt
  speelt (twee goldens als dat nodig is), met de pin die gate 12 vraagt.
- De kale zin "unscalable datatype in the ASP translation" is voor een toets op leeg of gevuld niet meer bereikbaar:
  wat de arm niet vertaalt, weigert met een bestaande of een nieuwe `CLINGO-###`-code van soort `refusal`
  (`backends/clingo/codes.tsv`, gehouden door `scripts/check-exec-codes.sh`). Een frame in de suite van de backend per
  nieuw gedragen datatype.
- Geen bestaande golden verandert van uitkomst of van kanaal.
- Los groen in jouw worktree: gate 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 13, 15, 16, 17. De volledige set draait de
  hoofdsessie vóór de landing; jij draait `make gates` niet.

## May touch

- `backends/clingo/` (de vertaling, de suite, `codes.tsv`, de README waar zij de toets of de weigeringen beschrijft);
- de nieuwe golden onder `corpus/golden/`;
- `scripts/check-clingo.sh`, `scripts/check-drools.sh` en een ander gate-script alleen waar een lijst of pin de nieuwe
  golden moet noemen, elk in een eigen commit met de reden;
- deze kaart.

## Out of scope

- De lezing van leeg zelf: de toets betekent wat zij nu betekent (RUNTIME-SEMANTICS ch. 1.2).
- `ref/`, `backends/drools/`, de frontends, `spec/`. Blijkt de frontend of Drools een fout te hebben op deze vormen,
  dan is dat een genummerd open punt in het Result met de gemeten uitkomst.
- `spec/CONSTRUCTS.md`, `spec/ASP-TRANSLATION.md`: stel de tekst voor in het Result.

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon, categorie). In niets dat in de
repository komt staat een naam, regel of casus van een regelset buiten deze repository.

## Result (bouwer, 2026-11-21)

De toets op leeg of gevuld, de gelijkheid op een niet-numeriek datatype en de case-emissie spelen in clingo voor elk
datatype dat een attribuut of parameter draagt, op het onderwerp, over een rol en als parameter; alleen de toets op een
lijst weigert, met CLINGO-050 (`refusal`, sleutel `filled-test-operand-not-bound`, parameters `operand`, `datatype`).
De golden `jutterswet-gevuldtoets-soorten` speelt in ref, clingo en beide Drools-branches. Merge, push, rebase: niet
gedaan.

In `backends/clingo/lib/translate.ml`:

- `filled_operand_atoms` is de ene plaats die per datatype kiest wat de operand van de toets bindt: een getal, een
  percentage en een datum via `expr_term`; een attribuut of cel van elk ander datatype zijn `val/3`-atoom zonder schaal
  (`member_read ~scaled:false`), op het onderwerp of over een pad van to-one-rollen; een parameter zijn `param/2`-atoom
  (`param_read`); een andere operand de termfunctie van zijn datatype (`symbolic_term`, `dt_term`, `struct_term`,
  `expr_term` voor een sum type); een lijst CLINGO-050.
- `param_read` is de ene lezing van een parameter (het atoom en de fail-fast-guard van ch. 5.7), gebruikt door
  `expr_term`, `dt_term`, `unscaled_term` en de toets.
- `unscaled_term` leest de operanden van de drie gelijkheidsarmen (record, sum type, tekst/enumeratie/boolean): een
  parameter via `param_read`, een attribuut over een rol via `member_read`, de rest via de termfunctie; `dt_term` leest
  een datum-tijd over een rol en als parameter; `symbolic_term` leest de payload van een tekst, enumeratie of boolean
  (`PayloadAccess`).
- De case schrijft een parameter (scalair en per segment van een tijdlijn) en de payload van een constructor als de term
  die een attribuut van dat datatype draagt; een ongelezen parameter staat er als feit.

**Commits** (branch `card/clingo-filled-test`, vanaf `4d02536bd`):

- `f05b2102d` clingo: `filled_operand_atoms`, CLINGO-050, README, de eerste frames;
- `05286726b` corpus: de golden;
- `61cae1b33` `scripts/check-drools.sh`: `EXPECTED_PLAYED` 383 naar 384;
- `f46d30a38` de DRL-pin van de golden;
- `9b27ae80e` het eerste Result;
- `4fb31f1c7` clingo: `param_read`, `unscaled_term`, de case-emissie van parameters en payloads, de payload in
  `symbolic_term`, de datum-tijd over een rol en als parameter, de README, de frames;
- `34e70601a` corpus: de golden met de herkeuring (enumeratie met markering) en de lezing van haar waarde;
- `7c8ad478a` de DRL-pin van de uitgebreide golden;
- de commit van dit Result.

**May touch.** Erbij kwam `backends/drools/src/test/resources/drl-text/jutterswet-gevuldtoets-soorten.drl`: gate 12
draait `DrlTextPinWitnessTest`, dat op een gespeelde golden zonder pin faalt. Het bestand is gegenereerd met
`-Duchar.drlpin`; de backend is niet veranderd.

**De meting per datatype en vorm.** Per datatype, vorm van de operand en toets één document uit de frontend (struct,
lijst en een parameter met markering op de wire, want RegelSpraak spelt ze daar niet), met een case waarin de operand
gevuld is, een waarin hij leeg is en bij een markering een met de marker. "speelt" is: dezelfde uitkomst als ref voor
gevuld én leeg; een ongebonden parameter is in ref en clingo EF-014 (fail-fast, ch. 5.7), de kolom parameter gaat over
de gebonden. Vóór is `4d02536bd`, na is `4fb31f1c7`; ref speelt elke cel.

| datatype | eigen attribuut vóór → na | over een rol vóór → na | parameter vóór → na |
|---|---|---|---|
| getal, percentage, datum | speelt → speelt | speelt → speelt | speelt → speelt |
| datum-tijd | kale zin → speelt | kale zin → speelt | kale zin → speelt |
| tekst | kale zin → speelt | kale zin → speelt | kale zin → speelt |
| boolean | kale zin → speelt | kale zin → speelt | kale zin → speelt |
| enumeratie | kale zin → speelt | kale zin → speelt | kale zin → speelt |
| sum type, getal met markering | speelt → speelt | kale zin → speelt | kale zin → speelt (wire) |
| sum type, enumeratie of tekst met markering | kale zin op `waarde` → speelt | kale zin → speelt | geen RegelSpraak-spelling |
| struct (wire) | speelt → speelt | CLINGO-014 → speelt | CLINGO-014 → speelt |
| lijst (wire) | kale zin → CLINGO-050 | kale zin → CLINGO-050 | kale zin → CLINGO-050 |

De metende scripts en hun uitvoer staan buiten de repository in de werkmap van de kaart.

**Per eis en per punt van de fixronde: getuige, gate, rode stand.** Rood gemeten in losse worktrees op `4d02536bd` (vóór
de kaart) en op `9b27ae80e` (vóór de fixronde), met de golden en de suite van nu erin.

- De meting: de tabel hierboven.
- De golden `jutterswet-gevuldtoets-soorten` (gates 5, 6, 12; 10 en 16 herschrijven haar): dertien regels, gevuld en
  leeg op `categorie` (enumeratie), `omschrijving` (tekst), `aangifte` (boolean, onwaar is gevuld), `taxatie` (getal
  met de markeringen onbekend en niet van toepassing), `herkeuring` (enumeratie met de markering onbekend) en, over de
  rol `jutter` van `vangst`, de `naam` van de Jutter; en `houten`: "zijn herkeuring heeft een waarde" en "zijn
  herkeuring is gelijk aan 'hout'" (een `PayloadAccess`). Case 1 alles gevuld, case 2 alles leeg (ook een Vondst zonder
  Jutter), case 3 de markeringen onbekend en niet van toepassing, die gevuld zijn, en een herkeuring metaal. Verwachtingen
  met de hand; de cases zijn de verlaging van `scenario.rgs`. Rood: op `4d02536bd` valt clingo op de drie cases met
  "unscalable datatype in the ASP translation", op `9b27ae80e` met CLINGO-002 (de lezing van de payload). Groen: ref,
  clingo, exec-model en DRL-tekst spelen de drie (`PLAYED 3`). Drools draagt elke vorm; geen snit.
- De suite van de backend (gate 6), het blok vóór de samenvatting van `test_clingo.ml`, 273 checks, elk document
  verifier-schoon en ref en clingo met dezelfde uitkomst:
  - de toets op gevuld en leeg op tekst, enumeratie, boolean onwaar, datum-tijd, de marker van een sum type, een sum
    type met een payload van tekst, enumeratie, boolean en datum-tijd, en een record, elk eigen, over een rol en als
    gebonden parameter (180 checks);
  - gelijk en ongelijk over een rol en met een parameter op tekst, enumeratie, boolean, sum type en record, waar
    ongelijk geldt als precies één operand leeg is (60 checks); later over een rol en dan een parameter op een
    datum-tijd (6 checks);
  - een ongelezen parameter van tekst, enumeratie, boolean, datum-tijd, sum type en record, en een tekst op een
    tijdlijn, die de case bindt (21 checks);
  - CLINGO-050 met zijn parameters op de toets op een lijstattribuut en een lijstparameter, waar ref speelt (6 checks).
  Rood: op `9b27ae80e` 65 FAILURES (23 met de kale zin: de payloads en de ongelezen parameters; de rest de
  fallback-codes CLINGO-002, -013, -014, de datum-tijd-fallback en de vorige CLINGO-050 op een parameter); de frames
  van de eerste ronde gaven op `4d02536bd` 22 FAILURES. Groen: 3231 checks, 0 failures.
- Punt 1 (case-emissie van een `waarde` met tekst- of enumeratiepayload): de golden (herkeuring) en de payload-frames.
- Punt 2 (case-emissie van een gebonden parameter die geen getal, percentage of datum is): speelt; een ongelezen
  parameter laat geen document meer vallen (frames `ch5.7 an unread parameter, …`), en de toets en de gelijkheid op
  zo'n parameter spelen. Geen weigering nodig.
- Punt 3 (gelijkheid over een rol): speelt voor tekst, enumeratie, boolean, sum type en record, ook met een parameter
  (frames `ch1.2d …`); een datum-tijd over een rol speelt in een ordening (`ch1.2e …`). De gelijkheid op een datum-tijd
  is in clingo niet vertaald, ook op het eigen attribuut (open punt 2).
- Punt 4: de README-zin over gelijkheid en lege waarden beschrijft nu de huidige stand (de `eqN`-helper voor een getal,
  de positieve join en `neq_helper` voor de andere datatypes, ook over een rol en met een parameter); de README noemt
  de parameter als term van zijn datatype en CLINGO-050 voor een lijst alleen.
- Geen bestaande golden verandert van uitkomst of kanaal: gate 6 en gate 12 met ongewijzigde `EXPECTED_CHANNEL`,
  `EXPECTED_PLAYED` +1 voor de nieuwe golden alleen.

**Voorlopige besluiten.**

1. Een attribuut- of celread van een datatype dat `expr_term` niet schaalt, bindt via `member_read ~scaled:false`; een
   gelijkheid leest een parameter of een attribuut over een rol via `unscaled_term`, de rest via de termfunctie. De
   termfuncties zelf lezen alleen het eigen attribuut; zo blijft de keuze per vorm op twee plaatsen naast elkaar
   (`filled_operand_atoms`, `unscaled_term`), elk met één `param_read`.
2. Een sum-typed operand van de toets die geen attribuut, cel of parameter is, blijft bij `expr_term`: die bouwt meer
   sum-typed vormen dan `sum_term`.
3. De toets op een lijst weigert (CLINGO-050): dragen zou een lezing van `lpres` vragen naast de `val/3`-binding.
4. De parameter in de case is de term die een attribuut van zijn datatype draagt (ook een tijdlijnsegment); een
   constructorpayload van tekst, enumeratie, boolean of datum-tijd ook.

**Voorgestelde tekst voor `spec/CONSTRUCTS.md`**, rij 47 (afwezigheidsmarkeringen), een regel erbij:
"**Landed (2026-11-21, card `clingo-filled-test`):** clingo voert de toets op leeg en gevuld uit op elk datatype van
een attribuut of parameter, op het onderwerp en over een rol, een marker als gevulde waarde, en een payload van tekst,
enumeratie, boolean of datum-tijd; de gelijkheid op tekst, enumeratie, boolean, sum type en record ook over een rol en
met een parameter (golden `jutterswet-gevuldtoets-soorten`, ref ≡ clingo ≡ Drools). De toets op een lijst weigert het
met CLINGO-050."

**Voorgestelde tekst voor `spec/ASP-TRANSLATION.md`**, de alinea "The **gevuld gate**" (bij de structs), in plaats van
"a struct operand rides `struct_term`'s `val/3` atom, any other rides `expr_term`": "the operand's binding atoms as a
**positive join**: an attribute or a cell read binds its `val/3` atom, unscaled where its datatype has no scale, on the
subject or over a path of to-one roles; a parameter its `param/2` atom, which the case writes as the term an attribute
of its datatype carries; any other operand rides the term function of its datatype (`expr_term` for a number, a date
and a sum type, `struct_term`, `symbolic_term`, `dt_term`); either way an absent value leaves the atoms unbound, so the
gated rule does not fire — leeg-on-absence for free. A list is refused (CLINGO-050)." En bij de gelijkheid van records
en sum types: "an operand is an attribute on the subject or over a path of to-one roles, a parameter, a literal or a
constructor term alike."

**`CLAUDE.md`**: geen alinea van een gate verandert.

**Open punten** (gevonden met `grep` op `decimals_of_datatype`, `attr_decimals`, `value_scale` en `scaled_input`,
en gemeten; niet hersteld in deze ronde):

1. Een tijdlijnattribuut van tekst, enumeratie of boolean in een case: de case-emissie van zijn segmenten
   (`segment_term` → `scaled_input`) valt met "unscalable datatype in the ASP translation", ook ongelezen (gemeten:
   `status Tekst` per maand, verifier-schoon, ref PASS, clingo de kale zin).
2. De gelijkheid op een datum-tijd (eigen of over een rol) is niet vertaald en valt met een zin zonder code ("datum-tijd
   equal/notEqual is not translated …"); de ordening speelt.
3. Een schrijfregel naar een attribuut met markering met een tekst of enumeratie als waarde: een literal valt met
   "malformed number lexeme" ("De herkeuring … moet gesteld worden op 'hout'", ref PASS), een read met de kale zin ("De
   notitie … moet gesteld worden op zijn omschrijving", `notitie Tekst (of onbekend)`, ref PASS): de `Construct
   waarde(…` schaalt zijn payload (`expr_term`, de Construct-arm, en `construct_ground_term`).
4. Een schrijfregel naar een tekst of enumeratie uit een parameter of een attribuut over een rol: `symbolic_term`
   leest alleen het eigen attribuut en valt in CLINGO-002 (niet gemeten op een document; dezelfde arm als de
   gelijkheid vóór deze kaart).
5. `expr_term` op een parameter of een read over een rol van een niet-numeriek datatype in een positie die een schaal
   leest, en een `PayloadAccess` van tekst of enumeratie daar (`value_scale`): de kale zin; geen vorm gevonden die de
   frontend schrijft en de verifier toelaat.

**Gates, los gedraaid in de worktree** (op `7c8ad478a`):

- gate 1: `check-self-contained: PASS`
- gate 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 552 shape self-test row(s); 508 file(s) validate and round-trip, floor 192; 66802 object(s) in 1165 document(s) in the schema's key order, 2 in no subschema)`
- gate 3: `check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 508 gold…`
- gate 4: `check-case-schema: PASS (schema valid; 134 self-test row(s); 1300 case file(s) validate)`
- gate 5: `check-oracle: PASS (ref suite 2829 worked examples, floor 489; 1199 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-afleiden-uit-leeg)`
- gate 6: `check-clingo: PASS (backend suite 3231 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 10 golden(s) in the refusal channel, by name, 30 cases refused with the pinned codes; the channel rule passes 22 rows of its own)`
- gate 7: `check-noop: PASS (1157 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)`
- gate 8: `check-red: PASS (101 red cases fault observably in ref and clingo, floor 44; 179 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 297 pinned parameter checks confirmed, floor 120; 46 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)`
- gate 10: `check-regelspraak: PASS — JUnit suite green (1704 tests, floor 173); 222 rgs golden(s) re-emit byte-identically (floor 80)`
- gate 12: `check-drools: PASS`; `played 384`; `DRL-text branch played 384`; `JUnit 1976 tests, floor 1573`
- gate 13: `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)`
- gate 15: `check-graph: PASS`
- gate 16: `check-scenario: PASS — 48 scenario golden(s) (floor 7) regenerate 176 case file(s) (floor 31) and their ir.json byte-identically`
- gate 17: `check-readings: PASS (ref traced 1199 golden case files; on both branches 385 goldens and 1073 case files compared, 8468 readings, 27262 node lines of…`

**Landing (hoofdsessie, 2026-11-21).** Nagelezen: de arm van de toets (`filled_operand_atoms`), `param_read`,
`unscaled_term` en de case-emissie van een payload en een parameter. De regel bij rij 47 van `spec/CONSTRUCTS.md` en de
alinea over de toets op gevuld in `spec/ASP-TRANSLATION.md` zijn gezet. De vijf open punten hierboven landen niet mee:
zij gaan over waarden die geen getal of datum zijn buiten de toets en de gelijkheid, en horen bij de kaart waarin
clingo de lezing "uit leeg volgt niets" zelf vertaalt, die dezelfde lezingen en schrijvers raakt. `make gates` groen op
`d8e3ec408` (800 s, 17 gates, red=0).
