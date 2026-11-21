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
