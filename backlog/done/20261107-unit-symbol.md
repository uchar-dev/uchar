# unit-symbol: het symbool van een eenheid op de draad, in een tekstdeel en in de bron

*Besluit van de eigenaar, 2026-11-07, in de hoofdsessie besproken en goedgekeurd (spectekst hieronder). Vervolg op
`backlog/done/20261009-tekst-van-een-waarde-in-een-tekstsamenstelling.md` ("Vraag voor de eigenaar", punt 2).*

Goal: een eenheidsdeclaratie draagt haar symbool in een eigen veld `DeclaredUnit.symbol`. Een tekstdeel `«X (eenheid)»`
schrijft elke factor met het symbool van zijn declaratie, waar die er een heeft, en anders met zijn afkorting
(`5,50 (€)`, `1200,00 (€/jr)`, `1,25 (kg)`, `12,50 (%)`). In de bron is het token na de afkorting op een eenheidsregel het
symbool. Twee eenheden mogen één symbool dragen; een verwijzing met zo'n symbool is dan dubbelzinnig en een fout, een
verwijzing met de afkorting (`met eenheid USD`) werkt altijd.

## Aanleiding en normbron

- RegelSpraak-specificatie §3.7 (lopende tekst en voorbeeld `de euro (mv: euros) EUR €`): een eenheid heeft een
  naamwoord en eventueel een afkorting en/of een symbool. De EBNF van §13.3.5.1 kent één `<eenheidsafkorting>`; onze
  grammatica neemt er al willekeurig veel (`eenheidAfkorting*`). De specificatie verwijst zelf met het symbool naar de
  eenheid (`met eenheid €`, `1000 €`); het corpus doet dat 87 keer in `met eenheid`.
- ALEF (gemeten 2026-11-07 op 872b859, alleen als achtergrond; de IR-SPEC noemt ALEF niet): `BasisEenheid` heeft de
  aparte optionele eigenschappen `afkorting` en `symbool`; het standaardstelsel `Valuta ISO-4217` geeft AUD, CAD en USD
  alle drie `$`. Een symbool is dus geen identiteit. ALEF's runtime-tekstomzetting schrijft geen eenheid.
- Nu schrijft de frontend het tweede token als eerste spelling (`"spellings": ["€", "euro", "euros"]`), en de
  tekstomzetting schrijft de afkorting (`5,50 (EUR)`).
- Gemeten over alle `.rgs` van de repository: 150 eenheidsregels, 116 met één token na het naamwoord, 34 met twee
  (33 × `EUR €`, 1 × `kg kgm` in `jutterswet-stelselweging`), geen met drie of meer.

## Ontwerp (goedgekeurd)

### Draad en IR-SPEC ch. 2.5

`DeclaredUnit` krijgt het optionele veld `symbol` (string, `minLength: 1`), in de sleutelvolgorde tussen
`factorToCanonical` en `spellings`. Alleen op de declaratie; op een voorkomen (`Unit`) weigert het schema het al
(`additionalProperties: false`) en weigert elke lezer het, zoals `spellings`. Het symbool staat niet ook in `spellings`.
Geen WF-regel: geen uniciteit, en geen executor zoekt een eenheid op haar symbool.

Rij in de tabel van `Unit` (ch. 2.5):

> | `symbol` | string? | the symbol of the unit: a written form beside its abbreviation, in which a text part writes the unit (RUNTIME-SEMANTICS ch. 1.2); on its declaration in `units` only, never empty |

Alinea na de tabel (de bestaande zin "a factor of a unit that occurs on a `Number` is the same node without `spellings`"
wordt "without `symbol` and `spellings`"):

> *The symbol.* A unit is identified by its system and its abbreviation. Its symbol is only the form in which a value
> of the unit is written for a reader. Two units may carry one symbol, so no rule holds a symbol unique and no reader
> identifies a unit by it. A factor on a `Number` carries neither `symbol` nor `spellings`. The RegelSpraak frontend
> takes as the symbol the token after the abbreviation on a unit line (`de euro (mv: euros) EUR €`; RegelSpraak
> specification §3.7).

Bij `spellings` erbij: de emitters schrijven geen spelling gelijk aan het symbool (zoals nu al: geen gelijk aan de
afkorting). De passage in ch. 2.1 over de v0-sleutel `€` ("so `€` is a spelling of `EUR` (ch. 2.5)") naar de huidige
stand: `€` is het `symbol` van `EUR`. De IR-SPEC staat op zichzelf: geen ALEF-term, RegelSpraak alleen als verwijzing
naar de herkomst van de bronvorm. Normatieve tekst beschrijft de huidige stand, zonder kaartnamen of
uitvoerdersstatus.

### RUNTIME-SEMANTICS ch. 1.2

Bij een deel met `unit`: elke factor geschreven met het symbool van zijn declaratie (het `symbol` van de `DeclaredUnit`
met dezelfde `system` en `abbreviation`), waar die er geen heeft met zijn afkorting; `%` en de kale kalendereenheden
(zonder `system`) met hun afkorting; verder zoals nu (positieve factoren met `·`, dan `/` en elke negatieve, een macht
waar die niet 1 is). Voorbeelden en tabelrijen: `5,50 (€)` (EUR met symbool €), `5,50 (EUR)` bij een EUR zonder
symbool, `1,25 (kg)`, `12,50 (%)`, een samengestelde eenheid met een gemengde schrijfwijze (`€/kg` of `€/jr`). De
cursieve noot: "that unit is its abbreviation ... no role that marks one of them as its symbol" vervalt; de eenheid
schrijft haar symbool waar haar declaratie er een heeft, besluit van de eigenaar van 2026-11-07. Meldingen, de trace van
ch. 5.12, dumps en de case-uitvoer houden de afkorting (`Units.render`, `Unit.rendering()` blijven zoals ze zijn): alleen
het tekstdeel schrijft het symbool.

### Frontend (RegelSpraak)

- Een eenheidsregel is naamwoord, (mv), afkorting en eventueel symbool: het tweede token is het symbool. Een regel met
  één token heeft alleen een afkorting, ook als dat token `€` is. Een derde of verder token heeft geen rol en is een
  fout met een eigen RGS-code (gemeten: geen bron schrijft er een).
- Verwijzen (`resolveUnit`, `declaresUnit`): de volgorde stelsel voor stelsel blijft; binnen een stelsel eerst de
  afkorting, dan het symbool, dan woord en meervoud. Een symbool dat in het stelsel waar de verwijzing uitkomt op twee
  of meer eenheden staat, is dubbelzinnig: een fout met een eigen RGS-code die het symbool en de afkortingen van de
  kandidaten noemt, op de plek van de verwijzing (`met eenheid $`, `100 $`, ook in een samengestelde eenheid
  `$/jr` en in een omrekenspecificatie). Nooit stil de eerste kiezen.
- De declaratie met een gedeeld symbool (`USD $` en `CAD $` in één stelsel) is toegestaan. WF-009 (RGS-016) telt
  alleen de afkortingen. Een symbool gelijk aan een afkorting van zijn eigen stelsel (ook zijn eigen, `kg kg`) blijft een
  fout: het kan nooit naar zijn eenheid verwijzen; hergebruik RGS-016 als de zin past, anders een eigen code.
- De emitter schrijft `symbol` en haalt het symbool uit `spellings`. `IrEmitter` controleert al dat een spelling niet
  gelijk is aan de afkorting; idem voor het symbool.
- `grammar/DIVERGENCES.md`: de eenheidsregel (de EBNF kent één afkorting; het symbool volgt §3.7) en een gedeeld
  symbool; de sectie "De eenheid van een tekstdeel" naar het symbool.
- Register en beide catalogi voor de nieuwe codes (gate 10, `scripts/check-rgs-codes.sh`), tests in de frontend.

### Executors

- OCaml-loader (verifier, ref, clingo): `symbol` op een declaratie lezen (een lege string, `null` of een niet-string is
  een laadfout in de zin die de loader voor `spellings` schrijft); op een voorkomen geweigerd. Geen controle in
  `checks.ml`.
- Ref: `text_of` schrijft per factor het symbool van zijn declaratie, anders de afkorting. Suite-rijen
  (`WV-1.2c-text-*`) voor symbool, zonder symbool, `%`, samengesteld gemengd.
- Clingo: alleen de AST-patronen volgen; het tekstdeel blijft CLINGO-042.
- Drools: de loader leest `symbol` op een declaratie en weigert het op een voorkomen (zoals `spellings`,
  `IrLoader` rond r. 1181–1218, met rijen in `TypeSystemRefusalWitnessTest`); de analyse (`TmsTranspiler.textPart`)
  geeft `rsTekst` de spelling met symbolen als constante. `Unit.rendering()` blijft voor meldingen.

### Corpus

- `jutterswet-stelselweging`: het kilogram van Massa wordt via zijn naamwoord bereikt in plaats van via de tweede
  afkorting `kgm`, bijvoorbeeld `het massakilogram (mv: massakilogrammen) kg` en `met eenheid massakilogram`. Wat de
  golden toetst blijft: twee records met canonieke naam `kg` in twee stelsels, factor 1, dezelfde verwachtingen. NOTE.md
  mee.
- `jutterswet-tekst-van-een-waarde`: `(EUR)` → `(€)` in de verwachtingen; een deel over een eenheid zonder symbool
  (bijvoorbeeld een tweede stelsel met `kg`) en een samengestelde eenheid met gemengde schrijfwijze erbij, zodat beide
  regels in ref en beide Drools-takken spelen. DRL-pin opnieuw (`DrlTextPinWitnessTest`, zie hieronder).
- Migratie van de rest: het symbool verhuist van `spellings` naar `symbol`. Gemeten (2026-11-07, `ir.json` met `€` of
  `kgm` in een eenheidsstelsel): 28 goldens, 34 negatieven, 1 red-case, 30 bestanden in `toka/`, 15 ref-fixtures,
  2 bestanden onder `backends/drools/src/test/resources`. Waar een `source.rgs` bij staat, schrijft de frontend het
  opnieuw (gates 10, 11 en 16 houden dat byte-gelijk); de rest met een script `spec/proposals/v1-migratie/m8.py`
  (precedent m1–m7), dat telt wat het verplaatst en faalt bij iets onverwachts. `toka/README.md` krijgt de landing
  onder zijn bewuste afwijkingen, met de tellingen, zoals de eerdere landingen.
- Negatieven waarvan een pointer of `expect.txt` naar een spellingsindex wijst: nalopen (gate 3).

## Acceptance

- Rood eerst: de nieuwe verwachtingen van `jutterswet-tekst-van-een-waarde` op de code van main `a5b857a47` falen in ref
  en beide Drools-takken (log).
- `make gates` in de worktree: alle zeventien gates en `after-run` rc=0 (logs onder
  `uchar-dev/werk/unit-symbol/logs/`). Gate 9 (catala) draait mee waar catala staat.
- Geen vloer daalt; waar een telling bewust daalt (spellingen die symbool worden), staat dat met reden in het Result.
- Frontendtests voor: symbool gelezen en geschreven, derde token geweigerd, gedeeld symbool gedeclareerd, verwijzing met
  een gedeeld symbool geweigerd (ook in een samengestelde eenheid), `met eenheid USD` bij een gedeeld symbool werkt,
  symbool gelijk aan een afkorting geweigerd.

May touch: `spec/IR-SPEC.md` (ch. 2.1, 2.5), `spec/RUNTIME-SEMANTICS.md` (ch. 1.2), `spec/schema/uchar-ir-v1.schema.json`
(`DeclaredUnit`), `spec/verifier/`, `spec/proposals/v1-migratie/m8.py`, `scripts/check-ir-schema.sh` (vorm-zelftestrijen
voor `symbol`), `frontends/regelspraak/`, `ref/`, `backends/clingo/`, `backends/drools/`, `corpus/golden/`,
`corpus/negative/`, `corpus/red/`, `toka/` (de `ir.json`-bestanden en `toka/README.md`; de fragmenten
`toka/casus/toka-*.rgs` blijven letterlijk), deze kaart.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md` en `CLAUDE.md` (voorstellen in het Result; de hoofdsessie landt ze);
de distributie `regelspraak-toolchain` (pin-bump na de landing); andere frontends (Catala, F-logic schrijven geen
symbool); het symbool in meldingen, trace, dumps of case-uitvoer.

## Result

*Gebouwd op 2026-11-07 op branch `card/unit-symbol` (worktree `wt/unit-symbol`), afgetakt van main `a5b857a47`. Logs
onder `uchar-dev/werk/unit-symbol/logs/`.*

### Commits

- `6a24508ab` — de draad en de spec: `DeclaredUnit.symbol` in het schema (string, `minLength: 1`, tussen
  `factorToCanonical` en `spellings`; de beschrijvingen van `DeclaredUnit` en `Unit` mee), IR-SPEC ch. 2.1 en 2.5,
  RUNTIME-SEMANTICS ch. 1.2, zes vorm-zelftestrijen in gate 2 (`SYMBOL_SHAPE_ROWS`), en de nieuwe verwachtingen van de
  golden `jutterswet-tekst-van-een-waarde` (rood op `a5b857a47`).
- `dcbabd7db` — de gedeelde OCaml-loader (verifier, ref, clingo), `Units.render ?spell`, ref (`Eval.text_unit`,
  `Arith.text_form.unit_text`), zeven suite-rijen; de record-literals in de suites van ref en clingo krijgen
  `ge_symbol = None`.
- `ab9f56788` — de frontend: model, resolutie, emitter, semantic tokens, RGS-389/390/391 met register en beide
  catalogi, DIVERGENCES, het grammaticacommentaar, tests.
- `7eb80d600` — Drools: loader, analyse (`TmsTranspiler.textUnit`), `Unit.rendering(Function)` in het gedeelde model,
  DROOLS-369 met catalogi, tests, DRL-pin.
- `d7acc0eea` — het corpus: m8, `jutterswet-stelselweging`, de her-emissie, `toka/README.md`.
- `b548fb003` — `TypeSystemRefusalWitnessTest` leest de gemigreerde `jutterswet-penningjaar` met zijn symbool; de zin
  over semantic tokens in DIVERGENCES.
- Dit Result.

### Rood eerst

De nieuwe verwachtingen van `jutterswet-tekst-van-een-waarde` (bron met het tweede stelsel, uitgegeven door de frontend
van `a5b857a47`, dus met `€` als eerste spelling) op de code van `a5b857a47`: ref 12 `MISMATCH` (8 in `case1-gevuld`,
4 in `case2-leeg`), alle op het symbool (`(EUR)` waar `(€)` verwacht is, `(EUR/kg)` waar `(€/kg)`), terwijl de getallen
en `(kg)` al klopten (`logs/red-ref-base.log`); de jar van `a5b857a47` `FAIL` op de exec-modeltak en op `--drl`
(`logs/red-drools-base.log`). Na de bouw: ref `oracle: PASS (61 expectations)` en `(31)`, beide Drools-takken
`PLAYED 2`.

### Wat er gebouwd is

- **Draad en spec.** Zoals het ontwerp: `DeclaredUnit.symbol`; IR-SPEC ch. 2.5 de tabelrij, de zin "without `symbol`
  and `spellings`", de alinea *The symbol.*, bij `spellings` "none equal to the `abbreviation` or to the `symbol`", en
  in de geschiedenisalinea één zin dat de L3-sleutel `€` nu het `symbol` is; ch. 2.1 naar de huidige stand ("the key
  `€` is the `symbol` of `EUR`"). RUNTIME-SEMANTICS ch. 1.2: de regel per factor, de tabelrijen (`5,50 (€)`,
  `5,50 (EUR)` zonder symbool, `1,25 (kg)`, `12,50 (%)`, `6,050 (€)`, `2,50 (€/kg)`, `1200,00 (€/jr)`), het voorbeeld
  met de nieuwe regel `gewichttekst`, de noot naar het besluit van 2026-11-07, en de zin dat meldingen, de trace,
  dumps en de case-uitvoer de afkorting houden.
- **OCaml.** De loader leest `symbol` op een declaratie (een lege string, `null` of een niet-string: "a unit's symbol
  is a non-empty string, or the field is absent") en weigert het op een voorkomen als onbekend veld; geen controle in
  `checks.ml`. Ref schrijft in `text_unit` per factor het symbool van de declaratie met dezelfde `system` en
  `abbreviation` (onder de naamidentiteit), anders de afkorting; `%` en de kale kalendereenheden de afkorting.
  `Units.render` kreeg een optionele spelling per factor; elke andere aanroep schrijft de afkorting. Suite-rijen
  `WV-1.2c-text-symbool`, `-zonder-symbool`, `-zonder-symbool-kg`, `-samengesteld-gemengd`, `-symbool-tarief`,
  `-eenheid-onbekend-stelsel`, `-eenheid-ongedeclareerd` (2613 → 2620 voorbeelden). Clingo: alleen het record.
- **Frontend.** Op een eenheidsregel is het eerste token de afkorting, het tweede het symbool
  (`UnitSystem.symbols`, afkorting → symbool); één token is alleen een afkorting, ook `€`. Een derde token is
  **RGS-389**. WF-009 (RGS-016) telt alleen de afkorting. Verwijzen (`resolveUnit`, `declaresUnit`): stelsel voor
  stelsel, binnen een stelsel afkorting, symbool, woord/meervoud; een symbool op twee of meer eenheden van het stelsel
  is **RGS-390** op de regel van de verwijzing, met het symbool en de afkortingen van de kandidaten (`'SEK', 'NOK'`),
  en de verwijzing lost dan op als een eenheid zonder stelsel, nooit als de eerste kandidaat. Dat geldt in
  `met eenheid`, in een waarde (`100 kr`), in een samengestelde eenheid (`kr/jr`), in `%/x`, in een tijdsduur en in
  een omrekenspecificatie (`= 1/100 kr`). Een symbool gelijk aan een afkorting van het eigen stelsel onder de
  naamidentiteit, het eigen inbegrepen (`kg KG`, `de gram g kg` naast `het kilogram kg`), is **RGS-391** en de eenheid
  houdt dan geen symbool. De emitter schrijft `symbol` na `factorToCanonical`, geen spelling gelijk aan het symbool,
  en weigert (als frontenddefect) een symbool gelijk aan de eigen afkorting of op een eenheid die het stelsel niet
  declareert. DIVERGENCES: nieuwe sectie "De eenheidsregel — afkorting en symbool" (tabel, resolutie, meting,
  getuigen), "De eenheid van een tekstdeel" naar het symbool, en in "Semantic tokens" de zin over een gedeeld symbool.
  Tests: `UnitSymbolTest` (6), `IrEmitterTest.a_symbol_equal_to_its_abbreviation_is_refused_by_the_emitter`,
  `SemanticTokensTest.a_shared_symbol_points_at_no_declaration` (JUnit 1530 → 1538).
- **Drools.** De loader leest `symbol` in `UnitSystem.symbols` en weigert het op een voorkomen; een lege of
  niet-string: dezelfde zin als de OCaml-loader, `null` de bestaande zin van het optionele veld. De analyse geeft
  `rsTekst` de eenheid uit `textUnit` (per factor het symbool van de declaratie, gezocht onder `Namen.norm`).
  **DROOLS-369** (`TEXT_PART_UNIT_UNDECLARED`, CONTRACT) weigert een factor zonder declaratie. Tests:
  `TypeSystemRefusalWitnessTest.the_symbol_is_a_non_empty_string_on_the_declaration_only`,
  `TmsBuiltinWitnessTest.a_part_with_its_unit_writes_the_symbol_of_its_declaration` (beide takken, alle volgordes)
  en `…a_part_whose_factor_has_no_declaration_is_refused_by_name`. De DRL-pin van de golden is herschreven
  (`"€"` en `"€/kg"` als constante, de regel `gewichttekst`); geen andere pin bewoog.
- **Corpus.** Golden `jutterswet-tekst-van-een-waarde`: stelsel `Gewicht` met `het kilogram (mv: kilogrammen) kg`
  zonder symbool, attributen `gewicht` (kg), `prijs` (€/kg) en `gewichttekst`, regel `"Gewicht: «zijn gewicht
  (eenheid)», prijs «zijn prijs (eenheid)», samen «zijn prijs maal zijn gewicht (eenheid)»"`; zes nieuwe verwachtingen
  (V1 `Gewicht: 1,20 (kg), prijs 2,50 (€/kg), samen 3,0000 (€)`, V2 `0,00 (kg) … 4,00 (€/kg) … 0,0000 (€)`, V3 `0,25
  (kg) … 0,04 (€/kg) … 0,0100 (€)`, V4 `-2,00 (kg) … 1,25 (€/kg) … -2,5000 (€)`, V5/V6 `Gewicht: , prijs , samen
  0,0000 (€)`), en `(EUR)` → `(€)` in de zes bestaande verwachtingen van `eenheidtekst`. Golden
  `jutterswet-stelselweging`: `het massakilogram (mv: massakilogrammen) kg` in Massa en `met eenheid massakilogram`
  op de twee attributen die `kgm` schreven; met de frontend gecontroleerd dat het woord Massa's kilogram bereikt: het
  `ir.json` verschilt alleen in de spellingen van die declaratie (`kgm, kilogram, kilogrammen` →
  `massakilogram, massakilogrammen`), elk voorkomen draagt `system: Massa` als voorheen, en ref en de verifier passen
  (37 verwachtingen); NOTE.md mee.
- **Migratie.** `spec/proposals/v1-migratie/m8.py` (`--all`, `--dry-run`, `--check`, in de vorm van m7): verplaatst een
  eerste spelling `€` naar `symbol` en weigert een `€` op een andere plaats, een eerste spelling die geen woord en
  geen bekend symbool is, een eenheid met al een `symbol`, en een `spellings` of `units` van een andere vorm. Een
  document in een andere opmaak dan die van gate 2 (één negatief schrijft `€` als zichzelf) houdt zijn opmaak; het
  resultaat moet dan precies als het gemigreerde document lezen. Gemeten: 108 symbolen in 108 documenten, niets
  geweigerd — `corpus/golden` 26, `corpus/negative` 34, `corpus/red` 1, `toka` 30 (`casus/ir.json` en de 29
  slices), `ref/test/fixtures` 15, `backends/drools/src/test/resources` 2 (`logs/s6-m8-all.log`); daarna `--check`
  0 van 1266. Daarna de frontend ter controle: alle 209 goldens met een `source.rgs` (de 25 met `EUR €` en de 2
  herschreven goldens inbegrepen) geven hun `ir.json` byte voor byte, net als de 45 fixtures met een `source.rgs`
  onder `ref/test/fixtures` en `backends/drools/src/test/resources` die de frontend uitgeeft, en de TOKA-casus
  (`logs/s6-toka-emit.log`); twee fixtures (`readings/temporal-total-clip`, `writers-stack/timeline-allocation`)
  weigert de frontend om redenen buiten deze kaart, ongewijzigd. Met de golden `tekst-van-een-waarde` (door de
  frontend verplaatst) staan nu 109 symbolen in `symbol`; in `stelselweging` verdween `kgm` met de herschreven bron.
  Samen zijn dat de 28 goldens die de kaart telde. `toka/README.md`: de landing onder de bewuste afwijkingen,
  met de tellingen. Negatieven: geen `expect.txt` of pointer wijst naar een spellingsindex (gate 3: 795 pointers
  lossen op, 481 op hun arm); de NOTE.md's die `de euro (mv: euros) EUR €` citeren blijven (brontekst).

### Toetsen die het oude gedrag vastlegden (aangepast, omdat de kaart dat gedrag verandert)

- `IrEmitterTest.the_factor_is_the_declared_rational_and_the_spellings_are_on_the_declaration`: `de korrel kr kl` gaf
  `"spellings":["kl","korrel"]`, nu `"symbol":"kl","spellings":["korrel"]`.
- `IrEmitterTest.a_second_declaration_of_an_abbreviation_is_an_error_and_does_not_replace_the_first`: `het kilogram kg
  KG` gaf geen fout (twee spellingen van één afkorting), nu alleen RGS-391.
- `IrEmitterTest.an_abbreviation_that_is_also_the_unit_word_stands_once_in_the_spellings` heet nu
  `a_symbol_that_is_also_the_unit_word_is_no_spelling`: `de meter (mv: meters) m meter` gaf
  `"spellings":["meter","meters"]`, nu `"symbol":"meter","spellings":["meters"]`.
- `TypeSystemRefusalWitnessTest.the_loader_lays_the_wire_one_to_one_on_the_records`: de spellingen van EUR in
  `jutterswet-penningjaar` waren `[€, euro, euros]`, nu `[euro, euros]` met `symbols = {EUR: €}`.
- De golden `jutterswet-tekst-van-een-waarde`: `(EUR)` → `(€)` in de zes verwachtingen van `eenheidtekst`.

### Afwijkingen van het ontwerp en eigen keuzes (met reden)

1. **Een factor zonder declaratie is een fout.** Het ontwerp zegt "het symbool van zijn declaratie, anders de
   afkorting"; de opdracht van de hoofdsessie: een factor waarvan het `system` onbekend is, is een fout. Ik heb dat
   doorgetrokken naar een afkorting die haar (bekende) stelsel niet declareert, omdat ook daar geen declaratie is om
   een symbool van te nemen; WF-034 vangt het eerste op de draad, het tweede is de open buurt van WF-009 (IR-SPEC ch.
   2.5), dus een verifier-schoon document kan het dragen. Ref faalt (`Value.fault`), Drools weigert met DROOLS-369.
   RUNTIME-SEMANTICS ch. 1.2 zegt het in één zin ("A factor that names a system the document does not declare, or an
   abbreviation its system does not declare, … a broken document").
2. **RGS-391 is een eigen code**: de zin van RGS-016 ("de eenheid 'x' is twee keer gedeclareerd") past niet op een
   symbool. Het symbool en de afkorting worden onder de naamidentiteit vergeleken (zoals WF-009 en de emitter), dus
   `kg KG` is ook een fout; de exacte resolutie zou `KG` als symbool wel bereiken, een lezer onder de naamidentiteit
   niet.
3. **Omrekenspecificatie.** Een doel dat de afkorting van geen eenheid is maar het symbool van precies één, lost op
   naar die eenheid (`de cent ct = 1/100 €`), zoals de verwijzingsvolgorde voorschrijft; vóór deze kaart gaf zo'n
   doel een canonieke naam die geen eenheid van het stelsel is (WF-009). Een gedeeld symbool daar is RGS-390, en de
   omrekening vervalt na de melding.
4. **Na RGS-390** lost de verwijzing op als een eenheid zonder stelsel (wat de frontend voor een onbekende naam al
   deed), zodat geen aanroeper een `null` hoeft af te handelen; de ERROR houdt het document tegen. De
   één-argumentvorm `resolveUnit(String)` blijft voor de toetsen (ook `toka/src/test`), met regel 0.
5. **Semantic tokens**: een verwijzing met een gedeeld symbool krijgt geen plaats, in plaats van de eerste declaratie
   (`SemanticTokensTest.a_shared_symbol_points_at_no_declaration`). Niet in het ontwerp genoemd; het volgt uit "nooit
   stil de eerste kiezen".
6. **De toetsen gebruiken `kr`** (SEK, NOK) als gedeeld symbool: `$` is geen token van de lexer, en het ontwerp vraagt
   geen lexeruitbreiding.
7. **Een spelling gelijk aan het symbool** schrijft de emitter niet (ontwerp), ook niet waar het woord gelijk is aan
   het symbool (`m meter`): het woord blijft in het model en lost op als voorheen.

### Open punten

- **Inline documenten in toetsen** met `€` als eerste spelling bleven staan: `ref/test/test_ref.ml` (1),
  `backends/clingo/test/test_clingo.ml` (2), `ReaderUnderWriterWitnessTest` (1). Het is geldige draad (een spelling
  is vrij, ch. 2.5) en geen van die toetsen gaat over eenheden; m8 leest alleen bestanden.
- **`$` lext niet** (`eenheidAfkorting` kent `LOWER_ID`, `UPPER_ID`, `€` en de tijdwoorden). Het ALEF-voorbeeld
  (`USD $`) is in de bron dus niet te schrijven; een symbool van letters (`kr`) wel. Een lexeruitbreiding is een
  eigen besluit.
- **De distributie** (`regelspraak-toolchain`): pin-bump na de landing. In het model veranderde dat `UnitSystem.units`
  het symbool niet meer als sleutel draagt (het staat in het nieuwe `UnitSystem.symbols`); gemeten in de lokale
  checkout van de distributie (alleen gelezen): geen Java-bron buiten de submodule leest `unitSystems`, `.units`,
  `.spellings` of `resolveUnit`.

### Voorstellen voor de hoofdsessie

- **CONSTRUCTS rij 31** (de zin over de eenheid vervangen): "… On request the unit follows in parentheses,
  `EmbeddedExpression.unit` (v1 extension, WF-129; source `«X (eenheid)»`), each factor written with the symbol of its
  declaration (`DeclaredUnit.symbol`) and with its abbreviation where that has none (`5,50 (€)`, `1,25 (kg)`,
  `2,50 (€/kg)`; owner's decision 2026-11-07, card `unit-symbol`). ref and both Drools branches (DROOLS-183 retired,
  DROOLS-368 for a part without a unit, DROOLS-369 for a factor without a declaration); the ASP backend refuses as a
  limit (CLINGO-042; golden `jutterswet-tekst-van-een-waarde`)."
- **CONSTRUCTS rij 26** (de zin "`Unit` carries the IR-SPEC field names and its `spellings`" vervangen): "`Unit`
  carries the IR-SPEC field names, and a declaration in `UnitSystem.units` its `symbol` and its `spellings` (IR-SPEC
  ch. 2.5; since 2026-11-07, card `unit-symbol`, the symbol has its own field: a written form beside the
  abbreviation, which no rule holds unique and no reader identifies a unit by; the RegelSpraak frontend takes the
  token after the abbreviation, RGS-389/390/391)."
- **CLAUDE.md, gate 2**: "Since the card `unit-symbol` (2026-11-07) the shape self-test holds `DeclaredUnit.symbol`
  to a non-empty string on a declaration (six rows: a symbol and two units with one symbol admitted; an empty, a null
  and a list symbol and a symbol on an occurrence refused)."
- **CLAUDE.md, gate 10**: "Since the card `unit-symbol` (2026-11-07) the token after the abbreviation on a unit line
  is the unit's symbol (DIVERGENCES "De eenheidsregel"): RGS-389 refuses a third token, RGS-390 a reference with a
  symbol two units of its system carry, RGS-391 a symbol equal to an abbreviation of its own system."
- **CLAUDE.md, gate 12**: "Since the card `unit-symbol` (2026-11-07) the loader reads `DeclaredUnit.symbol` and the
  analysis passes `rsTekst` the unit of a part with each factor by the symbol of its declaration; DROOLS-369 refuses a
  factor without a declaration." De vloer `EXPECTED_MIN_DROOLS_CODES` blijft 289 (gemeten nu 369 codes).
- **Geen vloer daalde.** Gate 2 vormrijen 534 → 540; ref-suite 2613 → 2620; frontend-JUnit 1530 → 1538; RGS-codes
  377 → 380; Drools-codes 368 → 369. Geen vloer telt spellingen, dus het verhuizen van 109 symbolen naar `symbol`
  raakt er geen.

### Gates

- Los, na de bouw (alle rc=0): 1, 2 (540 vormrijen), 3 (636 negatieven, 795 pointers), 4, 5 (ref-suite 2620, 1135
  cases), 6 (clingo-suite 2772, 1125 cases), 7, 8, 10 (JUnit 1538, 209 goldens her-emitteerd, 380 RGS-codes), 11,
  12 (JUnit 1898, 369 Drools-codes), 13, 15, 16, 17 (`logs/s7-*.log`, `logs/s7-a-summary.log`,
  `logs/s7-b-summary.log`).
- **`make gates`** op `b548fb003`: alle zeventien gates en `after-run` rc=0, red=0, 467 s
  (`out/gates/20261107-103955/summary.txt` in de worktree, kopie onder `logs/gates-20261107-103955/`, log
  `logs/gates-final.log`). Gate 9 (catala) draaide mee: 250 checks, 12 goldens.

### Landing (hoofdsessie, 2026-11-07)

Geland op main na `make gates` op `b548fb003` (17 van 17 groen en `after-run`, 467 s); main bewoog niet sinds de
aftakking. De IR-SPEC-tekst is de door de eigenaar goedgekeurde, met één zin minder: de alinea *The symbol.* herhaalde
dat een factor op een `Number` geen `symbol` en geen `spellings` draagt, wat de alinea erboven al zegt. CONSTRUCTS rij 26
en rij 31 en `CLAUDE.md` gate 2, 10 en 12 beschrijven de nieuwe stand. Open, als vraag voor de eigenaar: `$` lext niet,
dus `USD $` is in de bron niet te schrijven; en een factor waarvan het stelsel de afkorting niet declareert, is
verifier-schoon (alleen het tekstdeel weigert hem nu).
