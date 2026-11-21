# infer-from-empty-property: een document draagt de keuze dat uit leeg niets volgt, en ref en clingo lezen haar niet stil anders

*Opgesteld door de hoofdsessie op 2026-11-21. Eerste kaart van het landen van "Afleiden uit leeg" langs de route die de
eigenaar koos: de wire draagt het document van de auteur met de keuze, en elke executor draagt de lezing zelf. Het
voorstel met de tabellen staat in `spec/proposals/STRICT-EMPTY.md`.*

## Aanleiding

De eigenaar besloot: uit leeg mag niets afgeleid worden, als algemene eis aan het dialect. Een regelset kiest dat met
één regel, `Afleiden uit leeg: nee`; zonder die regel, of met `ja`, geldt de lezing van nu (RUNTIME-SEMANTICS ch.
1.2–1.4). Een voorbeeld uit de Jutterswet: "Een vondst is kostbaar indien zijn taxatie groter is dan 1.000 €". Is de
taxatie leeg, dan leest de lezing van nu "niet kostbaar"; onder `Afleiden uit leeg: nee` staat niet vast of de vondst
kostbaar is.

De proef (`card/strict-empty-lowering`, niet geland) zette een document om naar een document zonder de keuze. De
eigenaar koos daarna de andere route: het document van de auteur gaat ongewijzigd over de wire, met de keuze als
eigenschap, en ref, clingo en Drools bouwen de lezing elk in hun eigen verlaging in. Die route begint met de eigenschap
zelf, en met de eis dat geen afnemer een document dat haar draagt met de lezing van nu rekent: dat zou een fout antwoord
zonder melding zijn.

## Goal

De wire kent de eigenschap `inferFromEmpty` op de wortel van een document. Het schema, de OCaml-lader en de
OCaml-schrijver lezen en schrijven haar; de verifier keurt een document dat haar draagt gewoon. ref en clingo weigeren
zo'n document bij naam, met een code uit hun register, vóór zij iets rekenen. Geen lader laat een onbekende eigenschap
op de wortel door, en een gate houdt dat vast.

De lezing zelf (de tabellen, de sluitingen, de drie standen van een kenmerk) is niet van deze kaart: zij komt met de
kaart waarin ref haar rekent.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): de keuze staat in de regelset, als ja/nee-regel "Afleiden uit leeg"; de wire draagt het
document van de auteur; de backends dragen de lezing.

Van de hoofdsessie (voorlopig, ter bevestiging door de eigenaar):

1. **De naam** van de eigenschap is `inferFromEmpty`. Gebruik die ene spelling overal (in OCaml `infer_from_empty`),
   zodat een andere naam later één mechanische vervanging is.
2. **De vorm**: een boolean op de wortel, waarvan alleen `false` geschreven wordt. Afwezig betekent: de lezing van
   RUNTIME-SEMANTICS ch. 1.2–1.4. `true`, `null` of een andere waarde is een laadfout, zoals `EmbeddedExpression.unit`
   alleen `true` kent (één schrijfwijze per betekenis; gate 2 en gate 10 vergelijken byte voor byte). In het schema:
   `{"const": false}` met een beschrijving. De laadfout van de OCaml-lader luidt, op het pad `/inferFromEmpty`:
   `expected false, the one spelling of a document that infers nothing from an empty value, got <json>`. De Java-lader
   krijgt in een parallelle kaart dezelfde zin; wijzig hem niet zonder het in het Result te zeggen.
3. **De plaats** in de canonieke sleutelvolgorde van de wortel: direct na `format`, vóór `vocabularies`.
4. **Geen presentatieveld**: de eigenschap verandert uitkomsten. Gate 13 stript of wisselt haar niet, en IR-SPEC ch. 10
   noemt haar niet.
5. **Geen WF-regel**: de vorm is een laadfout en een schemaregel, zoals bij `EmbeddedExpression.unit`. Schrijf in
   IR-SPEC geen zin met MUST over een document die geen controle in de verifier heeft.
6. **De weigering**: ref en clingo krijgen elk één code van soort `refusal`, met de sleutel `infer-from-empty-false`
   en de parameter `form`: `REF-014` in `ref/codes.tsv`, het eerstvolgende vrije nummer in
   `backends/clingo/codes.tsv`. De betekenis: het document draagt `inferFromEmpty: false` en deze executor draagt die
   lezing niet voor `form`. In deze kaart is `form` altijd `document`. De code blijft bestaan als de executor de lezing
   later draagt: dan weigert hij er de vormen mee die hij strikt nog niet draagt. Maak dus geen tweede code.
7. **Waar de weigering staat**: op één plaats per executor waar elke ingang langs komt (voor ref de replay, de trace en
   de no-op-modus; voor clingo de vertaling), vóór enige evaluatie of vertaling. De gedeelde lader weigert niet: hij
   leest de eigenschap in het record, en elke executor beslist zelf.
8. **De verifier** doet verder niets met de eigenschap: een document dat haar draagt is welgevormd, en de modi
   (`--empty-closures`, `--dependency-edges`, `--arms`) lezen het zoals nu.

## Acceptance

Rood eerst: toon per getuige de rode stand op de code van vóór je wijziging.

- **Schema** (gate 2): een document met `inferFromEmpty: false` valideert en gaat byte voor byte rond; zelftestrijen in
  `scripts/check-ir-schema.sh` houden vast dat `true`, `null`, een tekst en een getal geweigerd worden, en dat een
  onbekende eigenschap op de wortel (bijvoorbeeld `inferFromNothing`) geweigerd wordt.
- **OCaml-lader en -schrijver**: `false` wordt gelezen en op de canonieke plaats teruggeschreven; afwezig blijft
  afwezig (elke bestaande golden en elk bestaand rood document gaat byte-identiek rond: gate 2, 3, 9, 14); `true` en
  een niet-boolean geven de laadfout van besluit 2; een onbekende eigenschap op de wortel geeft `unknown field`. De
  getuigen staan waar een gate ze draait (zeg in het Result welke gate elk van hen draait).
- **ref**: elk van zijn ingangen weigert een document met de eigenschap met `REF-014` en de parameter `form=document`,
  zonder een uitkomst te schrijven; de getuige draait in gate 5 (`ref/test/`) of in een gate die je noemt.
- **clingo**: de runner weigert zo'n document met zijn code en dezelfde parameter; de getuige draait in de suite van de
  backend (gate 6).
- `scripts/check-exec-codes.sh` (gate 8) groen: beide registers kloppen met de bronnen, parameters inbegrepen.
- De frontends in OCaml (`frontends/catala`, `frontends/flogic`) bouwen het record met de standaard (geen eigenschap) en
  hun goldens herschrijven byte-identiek (gate 9 en 14).
- Los groen in jouw worktree: gate 1, 2, 3, 4, 5, 6, 7, 8, 9, 14. De volledige set (`make gates`) draait de hoofdsessie
  vóór de landing; jij draait haar niet.

## May touch

- `spec/schema/uchar-ir-v1.schema.json`; `spec/IR-SPEC.md` (een korte paragraaf over de eigenschap: wat zij is, haar
  vorm, dat afwezig de lezing van RUNTIME-SEMANTICS ch. 1 is, dat een afnemer die de lezing van `false` niet draagt het
  document weigert; als voorstel, de hoofdsessie redigeert haar bij de landing); `scripts/check-ir-schema.sh` (de
  zelftestrijen), in een eigen commit;
- `spec/verifier/lib/` (`ast.ml`, `loader.ml`, `emit.ml` en wat het documentrecord bouwt of kopieert), en een getuige
  onder `spec/verifier/test/` als je daar een poot voor nodig hebt; dan ook `scripts/check-verifier.sh`, in een eigen
  commit;
- `ref/` (de weigering, `codes.tsv`, `ref/test/`); `backends/clingo/` (de weigering, `codes.tsv`, de suite, de README
  waar zij de weigeringen opsomt);
- `frontends/catala/lib/`, `frontends/flogic/lib/` alleen voor het veld van het record;
- deze kaart.

Mandaat voor de beschermde paden hierboven: de opdracht van de eigenaar van 2026-11-21 om voor deze route te bouwen. De
hoofdsessie leest na en landt.

## Out of scope

- De lezing zelf en `spec/RUNTIME-SEMANTICS.md`; het case-schema en de casebestanden; `spec/CONSTRUCTS.md` en
  `spec/PLAN.md` (stel de rij voor in het Result).
- `frontends/regelspraak/` en `backends/drools/` (een parallelle kaart, `regelspraak-infer-from-empty`, bouwt daar de
  regel in de bron, de Java-lader en de weigering van Drools). Raak die mappen niet aan.
- Geen golden in deze kaart: de golden met de eigenschap komt met de frontendkaart (zij draagt een `source.rgs`).
- De toolchain, de werkbank, elke regelset buiten deze repository.

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon). In niets dat in de repository komt
staat een naam, regel of casus van een regelset buiten deze repository.

## Result

De wire kent `inferFromEmpty` op de wortel van een document, alleen `false`, direct na `format`. Schema, OCaml-lader
en OCaml-schrijver lezen en schrijven haar; de verifier keurt een document dat haar draagt goed. ref weigert zo'n
document met `REF-014`, clingo met `CLINGO-049`, beide van soort `refusal`, sleutel `infer-from-empty-false`,
parameter `form` (hier `document`), vóór er iets geëvalueerd of vertaald wordt.

Commits (branch `card/infer-from-empty-property`):

- `9f8ed704a` schema: `"inferFromEmpty": {"const": false}` met beschrijving, na `format`; de wortelbeschrijving
  noemt haar.
- `1934de45e` gate 2 (eigen commit, beschermd script): zes zelftestrijen op de M1-basis (`false` aanvaard; `true`,
  `null`, `"nee"`, `0` en een wortelveld `inferFromNothing` geweigerd) en twee sleutelvolgorde-rijen (direct na
  `format` in de volgorde van het schema, na `diagnostics` niet).
- `c57917abb` OCaml: het veld `infer_from_empty : bool` vooraan in `Ast.document` (afwezig is `true`); de lader
  leest het in `check_keys ... ~ext:[ "inferFromEmpty"; "vocabularies" ]` en geeft op `/inferFromEmpty` de laadfout
  `expected false, the one spelling of a document that infers nothing from an empty value, got <json>`; `Emit` schrijft
  `false` direct na `format` en niets bij `true`; de Catala- en F-logic-frontend bouwen het record met `true`.
- `18e56ab94` ref: `Eval.refuse_infer_from_empty` (`REF-014`, `ref/codes.tsv`), aangeroepen in `load_document` van
  `ref/bin/runner.ml`, de ene plaats waar de replay, `--noop` en `--trace` hun document lezen. De getuige
  (`ref/test/test_ref.ml`, `WV-2-infer-from-empty-*`) op de fixture `ref/test/fixtures/infer-from-empty/`: de golden
  `jutterswet-meldplicht-flogic` met de eigenschap, en een casus waarin V3 geen vondstwaarde heeft.
- `f758f381f` clingo: `Translate.refuse_infer_from_empty` (`CLINGO-049`, `backends/clingo/codes.tsv`), de eerste
  regel van `translate_guarded`, waar het golden- en het rode pad van de runner langs komen; de getuige in
  `backends/clingo/test/test_clingo.ml` op dezelfde fixture; de README noemt de weigering.
- `a189158c3` ref-suite: de verifier vindt niets in de fixture.
- `20bd22ff4` IR-SPEC (voorstel, de hoofdsessie redigeert): een rij `inferFromEmpty` in de tabel van het document in
  ch. 2 en een alinea direct onder die tabel (wat zij is, `layer: core`, haar vorm en plaats, afwezig = de lezing van
  RUNTIME-SEMANTICS ch. 1.2–1.4, laadfout voor elke lezer, geen WF-regel, een executor die de lezing niet draagt
  weigert met een geregistreerde code; een voorbeeld uit de Jutterswet; de getuigen).

### Per eis: getuige, gate, rode stand

- **Schema** (gate 2): de zelftestrijen hierboven. Rood: het nieuwe script op het schema van `58cc53cee` weigert
  `inferFromEmpty = false`; mutant `{"type": "boolean"}` laat `true` door; mutant zonder `const` laat `true`, `null`,
  tekst en getal door; mutant met de eigenschap achteraan in `properties` breekt beide volgorderijen; mutant met een
  open wortel laat `inferFromNothing` door (logs `red-gate2-*.log` in de logmap van de bouwer).
- **OCaml-lader en -schrijver** (gate 5, `ref/test/`): `false` gelezen en afwezig `true`, voor de verifier en een
  executor; `true`, `null`, `"nee"`, `0` geven de laadfout op `/inferFromEmpty`, `inferFromNothing` geeft
  `unknown field 'inferFromNothing'` op de wortel, voor beide lezers; `Emit` op de geladen fixture geeft de fixture
  terug, op de golden de golden. Byte-identiek rond voor alle bestaande documenten: gate 2 (506 bestanden), 3, 9
  (13 Catala-goldens), 14 (3 F-logic-goldens). Rood met mutanten: lader die `true` aanvaardt (2 rood), lader die
  `inferFromNothing` toelaat (2 rood), schrijver die nooit schrijft (1 rood).
- **ref** (gate 5): `Eval.refuse_infer_from_empty` geeft `REF-014` met `[("form", "document")]`; de runner van
  dezelfde build weigert in de replay, `--noop` en `--trace` met exit 1 en `REF-014: the document carries
  inferFromEmpty false` en schrijft geen `oracle:`-, `MISMATCH`-, `reading`- of `state`-regel; dezelfde casus op de
  golden zonder eigenschap geeft `oracle: PASS (4 expectations)`. Rood: de runner van `58cc53cee` geeft
  `IR LOAD ERROR at <root>: unknown field 'inferFromEmpty'` (exit 2); mutant zonder de aanroep in de runner: replay
  `oracle: PASS (4 expectations)` (V3 niet meldplichtig, het stille foute antwoord), noop `MISMATCH`, trace
  `reading`-regels (3 rood); mutant met een lege weigerfunctie: 4 rood.
- **clingo** (gate 6, suite van de backend): `translate_guarded` weigert met `CLINGO-049` en `[("form", "document")]`;
  de runner geeft exit 1 en `TRANSLATION FAULT: CLINGO-049: the document carries inferFromEmpty false` zonder model;
  de golden zonder eigenschap vertaalt en speelt (`clingo: PASS (4 expectations)`). Rood: mutant zonder de aanroep, 2
  rood, de runner met `clingo: PASS (4 expectations)`.
- **Registers** (gate 8, `check-exec-codes.sh`): 14 REF- en 49 CLINGO-codes, parameters per plaats in hun regel.
- **De OCaml-frontends** (gate 9 en 14): bouwen het record met `true`, hun goldens herschrijven byte-identiek.
- **De verifier** (besluit 8): de fixture is verifier-clean (getuige in gate 5). Eenmalig gemeten, niet als getuige:
  `--empty-closures` en `--dependency-edges` op `jutterswet-keurgemiddelde` en `jutterswet-werfkeur`, en `--arms` op
  `wf001-duplicate-day-kind`, geven met de eigenschap erbij byte voor byte dezelfde uitvoer.

### Voorlopige besluiten

1. In het geheugen is de keuze `infer_from_empty : bool`, `true` bij afwezigheid, als eerste veld van het record
   (zoals `with_unit` bij `EmbeddedExpression.unit`); `Emit` schrijft alleen `false`.
2. De weigering van ref staat in een functie in `ref/lib/eval.ml` en wordt aangeroepen in `load_document` van de
   runner, niet in `Eval.evaluate` (de no-op-modus evalueert niet) en niet in `Case.load_file` (clingo deelt die
   caselezer).
3. Een rode casus waarvan het document de eigenschap draagt, krijgt van ref `EVALUATION FAULT: REF-014: …` en exit 1
   zonder `CODE`-regel: de weigering valt vóór het rode pad. Gate 8 faalt zo'n casus dan luid ("ref did not observe
   the expected fault") en kan hem niet als divergentie noteren. clingo meldt op het rode pad wel
   `CODE CLINGO-049 {"form":"document"}`. Kleinste wijziging; zo'n casus bestaat niet.
4. De weigering van clingo staat vóór `Scale.document` en de WF-007-weigering in `translate_guarded`; bij een
   document met de eigenschap en een presentatietag in een typeannotatie noemen ref en clingo dus allebei eerst
   hun eigen refusal-code.
5. De getuigen draaien de runner van dezelfde build (`_build/default/…` vanaf de root, `../bin/…` in de boom van
   dune); `ref/test/dune` en `backends/clingo/test/dune` noemen die runner als dep.
6. De fixture staat onder `ref/test/fixtures/infer-from-empty/` en de clingo-suite leest haar daar, zoals zij andere
   fixtures van ref leest.
7. De meldingstekst: `the document carries inferFromEmpty false, and this interpreter|backend does not carry the
   reading that infers nothing from an empty value for the document: it is refused, not read as ch. 1.2-1.4 lay
   down`.

### Voorgestelde rij voor `spec/CONSTRUCTS.md` (ch. 1)

| 48 | The choice that nothing is inferred from an empty value — `inferFromEmpty` on the document | the owner's decision of 2026-11-21 that a rule set can choose that nothing is inferred from an empty value (in RegelSpraak the rule `Afleiden uit leeg: nee`) | core | neither: a field of the document, no node | v0 has none. **Landed (2026-11-21, card `infer-from-empty-property`):** optional field `inferFromEmpty` on the document, directly after `format`, written `false` or not at all (schema `{"const": false}`); absent is the reading of RUNTIME-SEMANTICS ch. 1.2–1.4. No WF rule: another value is a load error at `/inferFromEmpty` in every reader. ref and clingo refuse a document that carries it before they evaluate or translate, with `REF-014` and `CLINGO-049` (key `infer-from-empty-false`, parameter `form`, here `document`). The reading itself is open. | witnesses: the self-test rows of gate 2; `ref/test/fixtures/infer-from-empty` in the suites of gates 5 and 6 |

### Voorstel voor `CLAUDE.md`, gate 2

De zin "A shape self-test holds `EmbeddedExpression.unit` to `true` and `DeclaredUnit.symbol` to a non-empty string."
wordt: "A shape self-test holds `EmbeddedExpression.unit` to `true`, `DeclaredUnit.symbol` to a non-empty string and
`inferFromEmpty` to `false` on a closed root, and a key-order self-test holds `inferFromEmpty` directly after
`format`."

### Open punten

1. De parallelle kaart `regelspraak-infer-from-empty` geeft de Java-lader dezelfde zin en Drools zijn weigering; haar
   golden valideert pas tegen het schema en de OCaml-lader van deze kaart.
2. Besluit 3: zodra een rode casus met de eigenschap nodig is, hoort de weigering van ref op het rode pad met een
   `CODE`-regel te komen.
3. Bijvangst buiten het mandaat `frontends/*/lib/`: `frontends/catala/test/suite.ml` en
   `frontends/flogic/test/suite.ml` bouwen elk één documentrecord, en kregen het veld `infer_from_empty = true` (de
   compiler eist het).
4. `spec/schema/README.md` en `ref/README.md` noemen de wortelvelden en de refusal-codes van ref niet; niet gewijzigd.
5. De lezing zelf (RUNTIME-SEMANTICS) en de verlaging in ref, clingo en Drools zijn open.

### Losse gates in de worktree

Alle op `20bd22ff4`, één tegelijk (gate 9 met catala op deze machine):

- `check-self-contained: PASS`
- `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 552 shape self-test row(s); 506 file(s) validate and round-trip, floor 192; 66520 object(s) in 1163 document(s) in the schema's key order, 2 in no subschema)`
- `check-verifier: PASS (the verifier's dune files name no library of another project of the workspace: 4 names (ir_v0 yojson zarith); 506 golden clean; 657 negative rejected for their declared rule, floor 568; 502 on their declared arm (expect.txt), floor 411, 277 arm keys, floor 239, 844 name=value lines, floor 686; 374 keys of one template each in the reports, floor 340, 468 key literals in checks.ml, floor 442; 825 cited pointers resolve in their documents, floor 705; empty-closures inventory over 506 documents: 827 closure places, floor 827, 383 negation, floor 383, 206 absent-instance, floor 206, 20 kinds, floor 20, 24 witnesses verbatim, floor 24, 1454 pointers resolve)`
- `check-case-schema: PASS (schema valid; 134 self-test row(s); 1297 case file(s) validate)`
- `check-oracle: PASS (ref suite 2829 worked examples, floor 489; 1196 cases replayed exactly, floor 427)`
- `check-clingo: PASS (backend suite 2958 checks, floor 83; 1166 cases replay exactly through clingo — ref = clingo over the corpus; 10 golden(s) in the refusal channel, by name, 30 cases refused with the pinned codes; the channel rule passes 22 rows of its own)`
- `check-noop: PASS (1154 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)`
- `check-red: PASS (101 red cases fault observably in ref and clingo, floor 44; 179 ref and clingo code checks report the case's code, floor 82, 23 listed as divergence; 297 pinned parameter checks confirmed, floor 120; 46 fault kinds in RUNTIME-SEMANTICS ch. 5.7b, floor 31)`
- `check-catala: PASS — frontend suite green (262 checks, floor 201); 13 catala golden(s) regenerate byte-identically (floor 12)`
- `check-flogic: PASS — frontend suite green (164 checks, floor 81); 3 flogic golden(s) re-emit byte-identically (floor 1)`
- `check-exec-codes: PASS — 14 REF codes and 49 CLINGO codes, numbered without a gap, every one not retired raised in its source, every raised code registered, every EF code a row of ch. 5.7b, the parameters of every site in its line or row (floors 12 and 27)` (in gate 8)
