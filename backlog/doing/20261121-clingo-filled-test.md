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

De arm van de toets op leeg of gevuld in de vertaling van clingo leest zijn operand via `filled_operand_atoms`
(`backends/clingo/lib/translate.ml`), de ene plaats die per datatype kiest wat de operand bindt: een getal, een
percentage en een datum blijven bij `expr_term`; een attribuut of cel van elk ander datatype bindt zijn `val/3`-atoom
zonder schaal (`member_read ~scaled:false`), op het onderwerp of over een pad van to-one-rollen; een andere operand gaat
naar de termfunctie van zijn datatype (`symbolic_term`, `dt_term`, `struct_term`, en `expr_term` voor een sum type).
Een lijst en een parameter van een ander datatype dan getal, percentage of datum weigeren met de nieuwe code
CLINGO-050 (`refusal`, sleutel `filled-test-operand-not-bound`, parameters `operand`, `datatype`). De golden
`jutterswet-gevuldtoets-soorten` speelt in ref, clingo en beide Drools-branches. Merge, push, rebase: niet gedaan.

**Commits** (branch `card/clingo-filled-test`, vanaf `4d02536bd`):

- `f05b2102d` clingo: `filled_operand_atoms` en de arm die het gebruikt, CLINGO-050 in `codes.tsv`, de README (de
  toets onder "Emptiness is absence", de weigering onder "Recorded limitations"), 78 checks in de suite;
- `05286726b` corpus: de golden `jutterswet-gevuldtoets-soorten` (`source.rgs`, `scenario.rgs`, `ir.json`, drie cases,
  `NOTE.md`);
- `61cae1b33` `scripts/check-drools.sh`: `EXPECTED_PLAYED` 383 naar 384;
- `f46d30a38` `backends/drools/src/test/resources/drl-text/jutterswet-gevuldtoets-soorten.drl`, de DRL-pin die
  `DrlTextPinWitnessTest` voor elke gespeelde golden eist (geschreven met `-Duchar.drlpin`, geen code; buiten "May
  touch", zie voorlopig besluit 4);
- de commit van dit Result.

**De meting per datatype en vorm.** Per datatype, vorm van de operand en toets één document uit de frontend (struct,
lijst en een parameter met markering op de wire, want RegelSpraak spelt ze daar niet), met een case waarin de operand
gevuld is, een waarin hij leeg is en bij een markering een met de marker; clingo vóór (`4d02536bd`) en na (`f05b2102d`).
"speelt" is: dezelfde uitkomst als ref voor gevuld én leeg. Een ongebonden parameter is in ref en clingo EF-014
(fail-fast, ch. 5.7); de kolom parameter gaat over de case waarin hij gebonden is.

| datatype | eigen attribuut vóór → na | over een rol vóór → na | parameter vóór → na |
|---|---|---|---|
| getal, percentage, datum | speelt → speelt | speelt → speelt | speelt → speelt |
| datum-tijd | kale zin → speelt | kale zin → speelt | kale zin → CLINGO-050 |
| tekst | kale zin → speelt | kale zin → speelt | kale zin → CLINGO-050 |
| boolean | kale zin → speelt | kale zin → speelt | kale zin → CLINGO-050 |
| enumeratie | kale zin → speelt | kale zin → speelt | kale zin → CLINGO-050 |
| sum type, getal met markering | speelt → speelt | kale zin → speelt | kale zin → CLINGO-050 (alleen op de wire) |
| sum type, enumeratie of tekst met markering | marker en leeg spelen; de case met `waarde` valt vóór en na met de kale zin in de case-emissie, niet in de arm (open punt 1) | idem; vóór viel ook de arm | geen RegelSpraak-spelling |
| struct (alleen op de wire) | speelt → speelt | CLINGO-014 (fallback) → speelt | CLINGO-014 → CLINGO-050 |
| lijst (alleen op de wire) | kale zin → CLINGO-050 | kale zin → CLINGO-050 | kale zin → CLINGO-050 |

ref speelt elke cel. De metende scripts en hun uitvoer staan buiten de repository in de werkmap van de kaart.

**Per eis: getuige, gate, rode stand.** Rood gemeten in een losse worktree op `4d02536bd` (geen wijziging aan de
vertaling), met de golden en de nieuwe suite erin.

- De meting: de tabel hierboven.
- De golden `jutterswet-gevuldtoets-soorten`: tien regels, gevuld en leeg op `categorie` (enumeratie), `omschrijving`
  (tekst), `aangifte` (boolean, onwaar is gevuld), `taxatie` (getal met de markeringen onbekend en niet van toepassing)
  en, over de rol `jutter` van `vangst`, de `naam` van de Jutter. Case 1 alles gevuld, case 2 alles leeg (ook een
  Vondst zonder Jutter), case 3 de markeringen onbekend en niet van toepassing, die gevuld zijn. Verwachtingen met de
  hand; de cases zijn de verlaging van `scenario.rgs` (gate 16). Rood: clingo op `4d02536bd` geeft op alle drie de cases
  `TRANSLATION FAULT: unscalable datatype in the ASP translation`. Groen: ref, clingo, exec-model en DRL-tekst spelen
  de drie (`PLAYED 3` op beide branches).
- Drools draagt elke vorm van de golden; geen snit, geen tweede golden.
- De kale zin is voor de toets niet meer bereikbaar: wat de arm niet bindt, weigert met CLINGO-050. Suite
  (`backends/clingo/test/test_clingo.ml`, het blok vóór de samenvatting): per nieuw gedragen datatype (tekst,
  enumeratie, boolean onwaar, datum-tijd, de marker van een sum type, record) en per vorm (eigen, over een rol) en toets
  een document, verifier-schoon, ref en clingo met dezelfde uitkomst (72 checks); twee weigeringen met code en
  parameters (`ch1.2 the gevuld test on a list attribute`: `operand` aantekening, `datatype` list; `ch1.2 the leeg
  test on a text parameter`: `operand` grensaantekening, `datatype` text), waar ref speelt (6 checks). Rood: de suite van
  `f05b2102d` op de vertaling van `4d02536bd` geeft 22 FAILURES (20 frames met de kale zin of CLINGO-014, de twee
  weigeringen met de kale zin in plaats van CLINGO-050); groen 3036 checks, 0 failures.
- Geen bestaande golden verandert van uitkomst of kanaal: gate 6 (`EXPECTED_CHANNEL` ongewijzigd) en gate 12
  (`EXPECTED_CHANNEL` ongewijzigd, `EXPECTED_PLAYED` +1 voor de nieuwe golden alleen).

**Voorlopige besluiten.**

1. Een attribuut- of celread van een datatype dat `expr_term` niet schaalt, bindt via `member_read ~scaled:false` en
   niet via `symbolic_term`/`sum_term`/`dt_term`/`struct_term`: die lezen alleen het eigen attribuut van het onderwerp
   (een read over een rol valt daar in een fallback-code), en de toets heeft geen term nodig. De termfuncties dragen
   de andere operandvormen van hun datatype, zoals de vergelijkingsarm ze kiest.
2. Een sum-typed operand die geen attribuut of cel is, blijft bij `expr_term` (zoals vóór): die bouwt meer sum-typed
   vormen dan `sum_term` (een Construct met een gebonden payload, een Match, een vouw).
3. Een lijst en een parameter van een ander datatype dan getal, percentage of datum weigeren met één code, CLINGO-050,
   soort `refusal` zoals de kaart vraagt; dragen zou de case-emissie van parameters (`param/2` met een geheel getal) en
   een lezing van `lpres` vragen, buiten de arm.
4. De DRL-pin van de nieuwe golden staat in een eigen commit onder `backends/drools/src/test/resources/drl-text/`,
   buiten "May touch": gate 12 draait `DrlTextPinWitnessTest`, dat op een gespeelde golden zonder pin faalt. Het bestand
   is gegenereerd, de backend is niet veranderd. Is dat ongewenst, dan valt de commit weg en is gate 12 rood op "no pin".

**Voorgestelde tekst voor `spec/CONSTRUCTS.md`**, rij 47 (afwezigheidsmarkeringen), een regel erbij:
"**Landed (2026-11-21, card `clingo-filled-test`):** clingo voert de toets op leeg en gevuld uit op een attribuut met
markeringen, op een tekst, een enumeratie, een boolean, een datum-tijd en een record, op het onderwerp en over een rol:
een marker is gevuld (golden `jutterswet-gevuldtoets-soorten`, ref ≡ clingo ≡ Drools); een lijst en een parameter van
een ander datatype dan een getal of datum weigert het met CLINGO-050."

**Voorgestelde tekst voor `spec/ASP-TRANSLATION.md`**, de alinea "The **gevuld gate**" (bij de structs), in plaats van
"a struct operand rides `struct_term`'s `val/3` atom, any other rides `expr_term`": "the operand's binding atoms as a
**positive join**: an attribute or a cell read binds its `val/3` atom, unscaled where its datatype has no scale, on the
subject or over a path of to-one roles; any other operand rides the term function of its datatype (`expr_term` for a
number, a date and a sum type, `struct_term`, `symbolic_term`, `dt_term`); either way an absent value leaves the atoms
unbound, so the gated rule does not fire — leeg-on-absence for free. A list and a parameter of a datatype `param/2`
does not carry are refused (CLINGO-050)."

**`CLAUDE.md`**: geen alinea van een gate verandert.

**Open punten.**

1. Een sum type met een payload van tekst, enumeratie of boolean (`Categorie (of onbekend)`, `Tekst (of onbekend)`):
   de case-emissie van een `waarde` (`ctor_value_term` → `scaled_input` → `decimals_of_datatype`) valt in clingo met de
   kale zin "unscalable datatype in the ASP translation", los van de toets; de marker en leeg spelen. ref speelt. De
   golden gebruikt daarom een getal als payload.
2. Een parameter van tekst, enumeratie, boolean, datum-tijd, sum type of record die een case bindt: de case-emissie
   (`param/2` via `scaled_input`) valt met dezelfde kale zin, ook als geen regel de parameter leest (gemeten: een
   ongelezen tekstparameter, ref PASS, clingo `TRANSLATION FAULT: unscalable datatype in the ASP translation`).
3. De vergelijking gelijk/ongelijk op een tekst, enumeratie, boolean, datum-tijd, sum type of record over een rol valt
   in de fallback-code van de termfunctie (gemeten: "de aantekening van de jutter van de Vondst gelijk is aan
   'schelp'", ref PASS, clingo CLINGO-002), die geen pin van gate 6 mag noemen; dezelfde termfuncties lezen alleen het
   eigen attribuut.
4. `backends/clingo/README.md`, "Emptiness is absence": de zin "The equality/inequality empty asymmetry has no such
   free encoding and faults until a corpus case demands it" beschrijft niet de huidige stand (de vergelijkingsarmen
   dragen de asymmetrie met `eqN` en `neq_helper`); niet aangepast, want buiten de toets.

**Gates, los gedraaid in de worktree** (op `f46d30a38`):

- gate 1: `check-self-contained: PASS`
- gate 2: `check-ir-schema: PASS (… 508 file(s) validate and round-trip, floor 192; 66734 object(s) in 1165 document(s) in the schema's key order, 2 in no subschema)`
- gate 3: `check-verifier: PASS (… 508 golden clean; 657 negative rejected for their declared rule, floor 568; …)`
- gate 4: `check-case-schema: PASS (schema valid; 134 self-test row(s); 1300 case file(s) validate)`
- gate 5: `check-oracle: PASS (ref suite 2829 worked examples, floor 489; 1199 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-afleiden-uit-leeg)`
- gate 6: `check-clingo: PASS (backend suite 3036 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo over the corpus; 10 golden(s) in the refusal channel, by name, 30 cases refused with the pinned codes; the channel rule passes 22 rows of its own)`
- gate 7: `check-noop: PASS (1157 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)`
- gate 8: `check-red: PASS (101 red cases fault observably in ref and clingo, floor 44; 179 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 297 pinned parameter checks confirmed, floor 120; 46 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)`
- gate 10: `check-regelspraak: PASS — JUnit suite green (1704 tests, floor 173); 222 rgs golden(s) re-emit byte-identically (floor 80)`
- gate 12: `check-drools: PASS`; `played 384`; `DRL-text branch played 384`; `JUnit 1976 tests, floor 1573`
- gate 13: `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays reproduce the untouched outcome, 40 of them PLAYED by the DRL-text branch, equal to the exec-model branch; 0 pinned survivors)`
- gate 15: `check-graph: PASS`
- gate 16: `check-scenario: PASS — 48 scenario golden(s) (floor 7) regenerate 176 case file(s) (floor 31) and their ir.json byte-identically`
- gate 17: `check-readings: PASS (ref traced 1199 golden case files; on both branches 385 goldens and 1073 case files compared, 8453 readings, …)`
