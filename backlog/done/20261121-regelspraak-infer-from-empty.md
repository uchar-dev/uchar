# regelspraak-infer-from-empty: de regel `Afleiden uit leeg: nee` in de bron, en Drools leest haar niet stil anders

*Opgesteld door de hoofdsessie op 2026-11-21. Tweede kaart van het landen van "Afleiden uit leeg"; zij loopt naast
`infer-from-empty-property` (de eigenschap in het schema en in OCaml, de weigering van ref en clingo) en landt daarna.
Het voorstel met de tabellen staat in `spec/proposals/STRICT-EMPTY.md`.*

## Aanleiding

De eigenaar besloot: uit leeg mag niets afgeleid worden, als algemene eis aan het dialect. Een regelset kiest dat met
één regel, `Afleiden uit leeg: nee`; zonder die regel, of met `ja`, geldt de lezing van nu (RUNTIME-SEMANTICS ch.
1.2–1.4). Een voorbeeld uit de Jutterswet: "Een vondst is kostbaar indien zijn taxatie groter is dan 1.000 €". Is de
taxatie leeg, dan leest de lezing van nu "niet kostbaar"; onder `Afleiden uit leeg: nee` staat niet vast of de vondst
kostbaar is.

De wire draagt het document van de auteur met de keuze als eigenschap `inferFromEmpty: false` op de wortel; ref, clingo
en Drools bouwen de lezing elk in hun eigen verlaging in. Tot een executor de lezing draagt, weigert hij het document
bij naam: met de lezing van nu rekenen zou een fout antwoord zonder melding zijn.

## Goal

De RegelSpraak-frontend leest de regel `Afleiden uit leeg: nee` en `Afleiden uit leeg: ja` en schrijft bij `nee` de
eigenschap `inferFromEmpty: false` op de wortel van het document; bij `ja` en zonder de regel schrijft zij niets, zodat
elk bestaand document byte-identiek blijft. De Java-lader van de Drools-backend leest de eigenschap; de backend weigert
een document dat haar draagt bij naam, op beide branches. Een golden uit de Jutterswet draagt de regel in haar bron en
de eigenschap op de wire.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): de keuze staat in de regelset, als ja/nee-regel met de woorden `Afleiden uit leeg`; de
wire draagt het document van de auteur; de backends dragen de lezing.

Van de hoofdsessie (voorlopig, ter bevestiging door de eigenaar):

1. **De schrijfwijze**: `Afleiden uit leeg: nee` of `Afleiden uit leeg: ja`, op het hoogste niveau van een bestand,
   naast de definities (een dialectuitbreiding zoals `vocabulaireDefinition`: het openingswoord komt in geen bestaand
   document voor; meet dat op het corpus en op `toka/` en zet de meting in `grammar/DIVERGENCES.md`).
2. **Eén keer per document.** Een document is wat de frontend in één aanroep leest (één bestand of een keten van
   bestanden). De regel staat daarin hoogstens één keer; een tweede regel is een fout met een eigen `RGS-###`-code, ook
   als zij hetzelfde zegt. De regel geldt voor het hele document, in welk bestand zij ook staat.
3. **`ja` schrijft niets**: de regel is dan alleen de uitdrukkelijke vermelding van de standaard.
4. **De eigenschap op de wire**: `inferFromEmpty`, alleen `false`, op de wortel direct na `format` en vóór
   `vocabularies`. Gebruik die ene spelling overal, zodat een andere naam later één mechanische vervanging is.
5. **De Java-lader** leest de eigenschap in het model; `true`, `null` of een andere waarde is een `DocumentError` op het
   pad `/inferFromEmpty` met de zin van de OCaml-lader:
   `expected false, the one spelling of a document that infers nothing from an empty value, got <json>`. Een onbekende
   eigenschap op de wortel blijft `unknown field`.
6. **De weigering van Drools**: één code in `DroolsCode` (het eerstvolgende vrije nummer), van de soort die een
   statusregel `EXCLUDE DROOLS-###: <zin>` geeft, met de sleutel `infer-from-empty-false` en de parameter `form`. De
   betekenis: het document draagt `inferFromEmpty: false` en deze backend draagt die lezing niet voor `form`. In deze
   kaart is `form` altijd `document`. De code blijft bestaan als de backend de lezing later draagt: dan weigert hij er
   de vormen mee die hij strikt nog niet draagt. De weigering staat in de bouw (`DroolsBackend.build`, met en zonder
   lezingen), niet in de lader: de graaf van gate 15 leest het document wel.
7. **De scenariolaag** (`--cases`): een toetsdocument naast een bron met de regel geeft een `ir.json` dat de eigenschap
   draagt, ook met een scope.
8. **De golden**: `corpus/golden/jutterswet-afleiden-uit-leeg/` met `source.rgs` en `ir.json`, zonder casebestand: geen
   executor speelt haar nog, en gate 5 kent geen kanaal voor een golden die ref weigert. De kaart waarin ref de lezing
   rekent, geeft haar cases. Houd de bron klein: een vondst met een taxatie en een gewicht, het kenmerk kostbaar, één
   regel met een vergelijking en één met een optelling, en de regel `Afleiden uit leeg: nee`.

## Acceptance

Rood eerst: toon per getuige de rode stand op de code van vóór je wijziging.

- **Frontend** (gate 10, JUnit zonder overgeslagen test): `nee` schrijft de eigenschap op haar plaats; `ja` en geen
  regel geven byte voor byte hetzelfde document; de regel in het tweede bestand van een keten geldt voor het document;
  een tweede regel geeft de nieuwe code met haar parameters, in het Nederlands en het Engels (`check-rgs-codes.sh`
  groen); de semantische tokens van de regel; de scenariolaag houdt de eigenschap (besluit 7).
- **Geen bestaand document verandert**: elke golden met een `source.rgs` herschrijft byte-identiek (gate 10), de
  `toka/`-zone ook (gate 11), en de toetsdocumenten (gate 16).
- **De golden** herschrijft byte-identiek door `--ir` (gate 10) en staat waar een gate een golden zonder casebestand
  verwacht (meet per gate hoe zij zo'n golden behandelt, en zeg het in het Result; gate 12 kent `NOCASE`).
- **Java-lader**: leest `false`; `true` en een niet-boolean geven de zin van besluit 5; een onbekende eigenschap op de
  wortel geeft `unknown field`. JUnit in de suite van de backend.
- **Drools**: de bouw van een document met de eigenschap geeft op beide branches de statusregel `EXCLUDE DROOLS-###:`
  met de zin van de code, en bouwt geen KieBase; JUnit in de suite van de backend, die onder `uchar.messages.locale=nl`
  draait; de catalogi in beide talen (`check-drools-codes.sh` groen).
- De graaf van gate 15 leest de golden (de SPEC-poot en de ROUTE-poot).
- Los groen in jouw worktree: gate 1, 10, 11, 12, 15, 16. Gate 2 en 3 zijn in jouw worktree rood op de nieuwe golden
  tot de parallelle kaart geland is (het schema en de OCaml-lader kennen de eigenschap daar nog niet): toon dat dat de
  enige rode regels zijn. De hoofdsessie zet je branch daarna op `main` en draait de volledige set; jij draait
  `make gates` niet.

## May touch

- `frontends/regelspraak/` (grammatica, lexer, model, resolver, emitter, het coderegister en de catalogi, de tests,
  `grammar/DIVERGENCES.md`, de README waar zij de uitbreidingen opsomt);
- `backends/drools/` (de lader, de bouw, `DroolsCode` en de catalogi, de tests, de README waar zij de weigeringen
  opsomt);
- `corpus/golden/jutterswet-afleiden-uit-leeg/` (nieuw);
- `scripts/check-drools.sh`, `scripts/check-regelspraak.sh`, `scripts/check-rgs-codes.sh`,
  `scripts/check-drools-codes.sh` alleen waar een lijst of pin de nieuwe golden of de nieuwe code moet noemen; elke
  wijziging in `scripts/` in een eigen commit, met de reden;
- deze kaart.

Mandaat voor de beschermde paden hierboven: de opdracht van de eigenaar van 2026-11-21 om voor deze route te bouwen. De
hoofdsessie leest na en landt.

## Out of scope

- De lezing zelf; `spec/` (schema, IR-SPEC, RUNTIME-SEMANTICS, verifier), `ref/`, `backends/clingo/`,
  `frontends/catala`, `frontends/flogic`: de parallelle kaart. Raak die mappen niet aan.
- Het case-schema, casebestanden en de zinnen van de scenariotaal voor "onwaar" of "staat niet vast".
- `spec/CONSTRUCTS.md` en `spec/PLAN.md` (stel de rij voor in het Result).
- De toolchain, de werkbank, elke regelset buiten deze repository. Wat de distributie na de pin moet weten (de regel, de
  statusregel van de weigering), staat als lijst in het Result.

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon). In niets dat in de repository komt
staat een naam, regel of casus van een regelset buiten deze repository.

## Result (bouwer, 2026-11-21)

De RegelSpraak-frontend leest `Afleiden uit leeg: nee` en `Afleiden uit leeg: ja` en schrijft bij `nee`
`inferFromEmpty: false` op de wortel, direct na `format`; de Java-lader van de Drools-backend leest de eigenschap in het
model; de bouw van de backend weigert zo'n document met DROOLS-370 op beide branches. De golden
`jutterswet-afleiden-uit-leeg` draagt de regel en de eigenschap, zonder casebestand. Merge, push, rebase: niet gedaan.

**Commits** (branch `card/regelspraak-infer-from-empty`, vanaf `151388ad6`):

- `864248f96` frontend: token `AFLEIDEN_UIT_LEEG`, de regels `afleidenUitLeegKeuze` en `afleidenUitLeegWaarde`,
  `RegelSpraakModel.inferFromEmpty()` (ook in `copy()`), de lezing in `DefinitionResolver`, de emissie in `IrEmitter`,
  `ja`/`nee` als sleutelwoord in `SemanticTokens`, RGS-462 in het register en beide catalogi, de sectie "Afleiden uit
  leeg" in `grammar/DIVERGENCES.md` (en de waarde in de sectie "Semantic tokens"), `InferFromEmptyDialectTest`, de
  golden (`source.rgs`, `ir.json`, `NOTE.md`);
- `84b0842d4` Drools: `IrLoader.inferFromEmpty`, `DroolsCode.INFER_FROM_EMPTY_FALSE` (DROOLS-370, `REFUSAL`, sleutel
  `infer-from-empty-false`, parameter `form`), beide catalogi, de weigering in `DroolsBackend.build`, de README,
  `InferFromEmptyLoaderWitnessTest`, `InferFromEmptyRefusalWitnessTest`;
- `04952c7d1` `scripts/check-drools.sh`: de golden in `EXPECTED_CHANNEL` als `NOCASE`;
- de commit van dit Result.

**Per eis: getuige, gate, rode stand.** Rood gemeten in een losse worktree op `151388ad6` met de getuigen en de golden
erin gekopieerd (logs onder `werk/infer-from-empty/logs-regelspraak/red-*`).

- Frontend (gate 10, JUnit): `InferFromEmptyDialectTest`, 9 tests: `nee` schrijft de eigenschap op haar plaats, één
  keer; `ja`, `ja;` en geen regel geven byte voor byte hetzelfde document, en de eigenschap is het hele verschil met
  `nee`; de regel in het eerste of het tweede bestand van een keten (`Resolver.irMode`) geeft hetzelfde document; een
  tweede regel is RGS-462 met `first`/`value` op haar eigen regel, in het Nederlands en het Engels, ook als zij hetzelfde
  zegt, en over twee bestanden schrijft `--ir` dan niets; een andere waarde (`misschien`, `Nee`, `onwaar`) is een
  syntaxfout op de regel; het token en de woorden `uit`/`leeg` daarbuiten; de semantische tokens (`keyword`,
  `punctuation`, `keyword`, `punctuation`); `--cases` zonder en met scope schrijft de eigenschap. Rood: de klasse
  compileert niet (`inferFromEmpty()` bestaat niet); met die twee regels eruit falen 7 van 9 (syntaxfout op
  `Afleiden`, RGS-091 in plaats van RGS-462, `casesMode` exit 1, het token is `UPPER_ID UIT LEEG`); de twee groene zijn
  grensgetuigen die op de basis ook een fout geven. De CLI van de basis op de golden: `syntaxfout: mismatched input
  'Afleiden'`. `check-rgs-codes.sh` groen (412 codes).
- Geen bestaand document verandert: gate 10 (221 rgs-goldens byte-identiek), gate 11 (41 fragmenten, 29 cases), gate 16
  (47 scenariogoldens, 173 casebestanden). Daarnaast gemeten: de 392 `.rgs`-bronnen van de repository (zonder de nieuwe)
  lexen met de lexer van vóór en na identiek, 63633 tokens (type, positie, kanaal); `afleiden` komt in geen bron en geen
  testbron voor, in welke schrijfwijze ook.
- De golden: gate 10 herschrijft haar byte-identiek. Hoe elke gate een golden zonder casebestand behandelt:
  - gate 4: leest alleen casebestanden; de golden is onzichtbaar (gedraaid: PASS, 1297 casebestanden);
  - gate 5: noemt haar in de PASS-regel, "NOT replayed, no case file yet: corpus/golden/jutterswet-afleiden-uit-leeg"
    (gedraaid);
  - gate 6: leest `find corpus/golden -name 'case*.json'`; onzichtbaar, geen regel in zijn kanaallijst nodig (gelezen,
    niet gedraaid);
  - gate 7: leest alleen casebestanden; onzichtbaar (gedraaid: PASS, 1154 cases);
  - gate 12: een golden zonder casebestand meet `NOCASE`, en een ongenoemde golden is gepind op `PLAYED`, dus de lijst
    moet haar noemen (de kanaalvergelijking in `check-drools.sh`); `EXPECTED_PLAYED` blijft 383 (gedraaid: "no-case 1");
  - gate 13: elke poot slaat haar over (`[ -s "$CASES" ] || continue`; gelezen, niet gedraaid);
  - gate 15: de SPEC-poot leest elk `ir.json`, met of zonder casebestand, de ROUTE-poot elke golden met `source.rgs`;
  - gate 17: "no case file, not read: jutterswet-afleiden-uit-leeg", geen fout (gelezen, niet gedraaid).
- Java-lader (gate 12, JUnit): `InferFromEmptyLoaderWitnessTest`, 3 tests: `false` in het model, afwezig blijft de
  standaard; `true`, `null`, `"false"`, `"nee"`, `0`, `[]`, `{}` geven elk de `DocumentError` op `/inferFromEmpty` met
  de zin van besluit 5; `inferFromNothing` is `unknown field`. Rood: compileert niet op de basis; de jar van de basis op
  de golden: `ERROR DocumentError: malformed uchar-ir-v1 document at '/inferFromEmpty': unknown field`, op beide
  branches.
- Drools (gate 12, JUnit onder `uchar.messages.locale=nl`): `InferFromEmptyRefusalWitnessTest`, 4 tests: `build` werpt
  `CaseRunner.Excluded` met DROOLS-370 op beide branches, met en zonder lezingen (er komt geen `Build` en dus geen
  KieBase terug); `DroolsBackendMain` geeft `EXCLUDE DROOLS-370: <zin>` met exit 2 zonder vlag, met `--drl`, met
  `--readings` en met `--readings --drl`; de zin in beide talen; zonder de eigenschap bouwt het document en speelt het
  een case op beide branches (`PLAYED 1`), dus alleen de eigenschap wordt geweigerd. Rood: als bij de lader.
  `check-drools-codes.sh` groen (370 codes).
- De graaf: zie gate 15 hieronder.

**Los gedraaide gates** (in deze worktree, één tegelijk):

- gate 1: `check-self-contained: PASS`;
- gate 10: `check-regelspraak: PASS — JUnit suite green (1704 tests, floor 173); 221 rgs golden(s) re-emit
  byte-identically (floor 80)`;
- gate 11: `check-toka-parity: PASS` (41 re-emitted, 29 cases door ref, clingo en Drools);
- gate 12: `check-drools: PASS` — played 383, auto-excluded 47, DRL-text branch played 383, JUnit 1976 tests, no-case 1.
  Gedraaid op de eindstand van de code: na deze run veranderde alleen deze kaart;
- gate 16: `check-scenario: PASS — 47 scenario golden(s) (floor 7) regenerate 173 case file(s) (floor 31) and their
  ir.json byte-identically`;
- gate 4: `check-case-schema: PASS (schema valid; 134 self-test row(s); 1297 case file(s) validate)`;
- gate 5: `check-oracle: PASS (ref suite 2806 worked examples, floor 489; 1196 cases replayed exactly, floor 427; NOT
  replayed, no case file yet: corpus/golden/jutterswet-afleiden-uit-leeg)`;
- gate 7: `check-noop: PASS (1154 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)`;
- gate 2, rood, alleen op de nieuwe golden: `FAIL: corpus/golden/jutterswet-afleiden-uit-leeg/ir.json` / `at: <root>` /
  `Additional properties are not allowed ('inferFromEmpty' was unexpected)` / `check-ir-schema: FAIL (1 of 507 file(s)
  bad)`;
- gate 3, rood, alleen op de nieuwe golden (de gate loopt door en meldt verder niets): `FAIL: document
  corpus/golden/jutterswet-afleiden-uit-leeg/ir.json is not verifier-clean: LOAD ERROR at <root>: unknown field
  'inferFromEmpty'` en `FAIL: empty-closures: corpus/golden/jutterswet-afleiden-uit-leeg/ir.json: verifier
  --empty-closures exits 2: LOAD ERROR at <root>: unknown field 'inferFromEmpty'`;
- gate 15, rood op dezelfde oorzaak: `LOAD ERROR at <root>: unknown field 'inferFromEmpty'` / `check-graph: FAIL —
  corpus/golden/jutterswet-afleiden-uit-leeg/ir.json: the verifier does not print its dependency edges`. De SPEC-poot
  vraagt de randen aan de OCaml-verifier, die de eigenschap in deze worktree nog niet kent, en de gate stopt daar. Wat
  van de Java-kant is, los gemeten: `GraphProjection` leest de golden (exit 0, geen randen: beide regels lezen alleen
  invoer), en de ROUTE-suite `GraphSourceRouteTest` over alle 221 goldens met `source.rgs`, de nieuwe erbij, is groen
  (221 tests). Gate 15 in een losse worktree van `04952c7d1` zonder de nieuwe golden: `check-graph: PASS` (spec leg 393
  van 430 goldens vergeleken, route leg 220 goldens, 83 unit tests). Na de landing van `infer-from-empty-property` hoort
  gate 15 groen te zijn; dat draait de hoofdsessie.

**Voorlopige besluiten.**

1. Het openingswoord is één token van drie woorden, `AFLEIDEN_UIT_LEEG : 'Afleiden' WSP 'uit' WSP 'leeg'`, zoals
   `bestaande uit de`; `ja` en `nee` worden geen tokens maar een `LOWER_ID` die op zijn tekst gelezen wordt, zoals
   `volgens` en `onbekend`. Elke andere waarde, ook `Nee`, is een syntaxfout (RGS-091) op de regel; geen eigen code.
2. Een `;` na de regel mag, zoals na `Vocabulaire`.
3. RGS-462 (`infer-from-empty-twice`, ERROR) heeft de parameters `first` en `value`, beide `ja` of `nee`, en staat op
   de regel van de tweede; geen regelnummer als parameter, want in een keten is dat het nummer in de keten.
4. Het veld in het model is privé met `inferFromEmpty()`; `copy()` loopt alleen over publieke collecties en kopieert
   het daarom uitdrukkelijk (anders verloor een scope in de scenariolaag de keuze; de getuige met scope houdt dat vast).
5. De Java-lader leest de eigenschap vóór de sleutelcontrole van de wortel, zodat ook `null` de zin van besluit 5
   krijgt en niet "an optional field is written whole or not at all".
6. DROOLS-370 is van soort `REFUSAL`. De zin: "the document carries inferFromEmpty false, a reading in which nothing
   follows from an empty value, and this backend does not carry that reading (form: {form})" / "het document draagt
   inferFromEmpty false, een lezing waarin uit een lege waarde niets volgt, en deze backend draagt die lezing niet
   (vorm: {form})". De weigering staat direct na het laden, vóór de controle op botsende beannamen.
7. De golden gebruikt kale gehele getallen, zonder eenheid; zij heeft een `NOTE.md` zoals de meeste goldens.

**Voorgestelde rij voor `spec/CONSTRUCTS.md`** (naast de rij van `infer-from-empty-property` voor de eigenschap zelf; de
hoofdsessie voegt samen):

> | 48 | The choice that nothing follows from an empty value — `Afleiden uit leeg: nee` | decision of the maintainer of
> 2026-11-21: "uit leeg mag niets afgeleid worden" is a general requirement of the dialect, chosen per rule set | dialect
> (RegelSpraak) over core | kernel (a root property that changes outcomes) | new (no v0 counterpart): the line
> `Afleiden uit leeg: nee` or `ja` at the top level of a document, once per document, a chain of files included
> (RGS-462 for a second); `nee` writes `inferFromEmpty: false` on the root directly after `format`, `ja` and no line
> write nothing, so every existing document is byte-identical. The Drools Java loader reads the property; the Drools
> build refuses such a document with DROOLS-370 (`form=document`) on both branches, with and without readings. Witnesses
> `InferFromEmptyDialectTest`, `InferFromEmptyLoaderWitnessTest`, `InferFromEmptyRefusalWitnessTest`; golden
> `jutterswet-afleiden-uit-leeg`, without a case file | the reading itself comes with the card in which ref computes it |

`CLAUDE.md`: geen alinea van een gate verandert (gate 12 kende `NOCASE` al; gate 10 en de registers houden de nieuwe
code zonder nieuwe regel).

**Voor de distributie na de pin.**

- Een bron kan `Afleiden uit leeg: nee` of `ja` dragen (token `AFLEIDEN_UIT_LEEG`; semantische tokens: `keyword`,
  `punctuation`, `keyword`); een tweede regel is RGS-462.
- Een document kan `inferFromEmpty: false` dragen; `DroolsBackend.build` werpt dan `CaseRunner.Excluded` met code
  `INFER_FROM_EMPTY_FALSE` (DROOLS-370) en `form=document`, op beide branches en met lezingen; de CLI geeft
  `EXCLUDE DROOLS-370: <zin>` met exit 2, en `CaseRunner.report` maakt er per case een marker van. Een ingang van de
  distributie die een KieBase bouwt zonder `DroolsBackend.build` zou de weigering missen: na te lopen in de distributie.
- `RegelSpraakModel.inferFromEmpty()` is de lezer van de keuze.

**Open punten.**

1. Gate 2, 3 en 15 zijn in deze worktree rood op de nieuwe golden, en alleen daarop, tot `infer-from-empty-property`
   geland is (het schema en de OCaml-lader kennen de eigenschap hier nog niet).
2. Een token van meer woorden pakt ook het begin van een langer woord: `Afleiden uit leegstand` zou lexen als
   `AFLEIDEN_UIT_LEEG` plus `stand`, zoals `bestaande uit de` dat doet met `bestaande uit dezelfde`. Geen bron schrijft
   het; de grammatica houdt het zoals bij de andere tokens van meer woorden.

**Landing (hoofdsessie, 2026-11-21).** Nagelezen: de grammatica, de resolver (één keuze per document, ook over de
bestanden van een keten heen), de emitter, de kopie van het model voor een scope, de lader en de weigering in
`DroolsBackend.build`. De branch staat op `main` na de landing van `infer-from-empty-property`; gate 2, 3 en 15 zijn
daar groen op de golden. De rij in `spec/CONSTRUCTS.md` is met die van de eigenschap samengevoegd. `make gates` groen
op `e8edbc57a` (734 s, 17 gates, red=0). Voor de pin van de distributie: nagaan dat elke ingang die een KieBase bouwt
langs `DroolsBackend.build` komt.
