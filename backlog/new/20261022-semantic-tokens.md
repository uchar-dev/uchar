# semantic-tokens: de frontend geeft per stuk brontekst een soort en een positie, naast het IR

*Besluit van de eigenaar 2026-10-22 ("ik wil dit"), na de vraag of uchar genoeg handvatten biedt om de regels in een
afnemer met kleur te tonen: een naam gekleurd naar wat hij noemt (objecttype, attribuut, rol, parameter,
domeinwaarde), een onopgeloste naam in rood. Richting door de hoofdsessie. De directe afnemer is de werkbank van de
regelset herleving-WW, via de distributie; de uitvoer is algemeen en kent die afnemer niet.*

## Aanleiding

Wat er nu is, gemeten op main `680e5263`:

- **De lexer** (`grammar/RegelSpraakLexer.g4`) geeft sleutelwoorden als tokens, ook woordgroepen
  (`WORDT_BEREKEND_ALS` is "moet berekend worden als"), literals, datums, bullets en leestekens. Commentaar staat op
  het HIDDEN-kanaal (`LINE_COMMENT`, r. 501), witruimte wordt overgeslagen. Namen splitst hij in losse woorden
  (`UPPER_ID`, `LOWER_ID`, kopnoot 2); de parser zet er een naam van meer woorden van, en wat een naam noemt, weet pas
  de resolutie.
- **De draad** draagt per knoop `source.snippet`, en `line` alleen op een consistentieversie en een regelgroep; het
  schema laat `file` en `column` niet toe ("until the card that first writes positions"). Een `Diagnostic` draagt
  alleen een regel. Terecht: het IR is de abstracte vorm (beslistabellen ontsuikerd, gegenereerde namen,
  defaults-groepen), geen weergave van de tekst.
- **Het model in het geheugen** heeft per knoop `ir.Source(file, line, column, snippet)`, maar per knoop en niet per
  naam, en niet naar buiten.
- **De CLI** kent `--ir` en `--cases`. Meerdere bestanden leest hij als één keten tekst; `casesMode` rekent een regel in
  de keten terug naar bestand en regel (`firstLines`).

Een afnemer die een naam naar zijn soort wil kleuren, of een onopgeloste naam wil aanwijzen, heeft dus geen bron. Hij
kan het IR niet gebruiken (geen posities, en een gelowerde vorm is niet de tekst), en de soort van een naam zelf
afleiden zou de resolutie van de frontend nabouwen.

## Doel

Een uitvoer van de frontend naast het IR, niet op de draad, zoals de diagnosecodes (`Diagnostic(severity, code,
params, …)`, kaart `frontend-codes`): per gelezen bestand een lijst spans die de tekst dekt.

- **Dekking.** Elk token van de lexer, op het standaardkanaal en op HIDDEN, valt in precies één span; spans overlappen
  niet; witruimte valt erbuiten. Een naam van meer woorden is één span, ook als hij over een regeleinde loopt.
- **Positie.** Begin en eind van de span: regel (vanaf 1, in het eigen bestand, zoals de diagnostics) en kolom (vanaf
  0, in UTF-16-code-units: de standaardcodering van posities in het Language Server Protocol en de eenheid van een
  JavaScript-string, zodat een browser en een language server de waarde zonder omrekening gebruiken). ANTLR telt
  kolommen in code points; buiten het BMP verschillen die twee.
- **Lexicale soorten**, zonder resolutie: sleutelwoord, commentaar, getal, datum, tekstliteral, leesteken (bullets,
  `;`, `.`, `:`, haakjes). De bouwer kiest de indeling en documenteert haar.
- **Naamsoorten**, uit de resolutie: objecttype, attribuut, kenmerk, rol, feittype, parameter, domein, domeinwaarde,
  eenheid, dimensie en dimensielabel, regelnaam, variabele (`Daarbij geldt:`), en wat de bouwer verder tegenkomt; plus
  `unresolved` voor een naam die niets bindt. De namen van de soorten volgen de naamregel van `CLAUDE.md` (de draad
  waar die een naam heeft: `objectType`, `characteristic`, …). Op de plaats van de declaratie draagt de span
  `declaration: true`; een verwijzing draagt de positie van de span van haar declaratie (bestand, regel, kolom), als die
  in de gelezen bestanden staat.
- **Diagnostics in dezelfde uitvoer**, met code, parameters, tekst, bestand en regel in dat bestand, zodat een afnemer
  één document leest.
- **Een tabel** in de documentatie geeft per soort het semantic-token-type van LSP 3.17 dat er het dichtst bij ligt
  (bijvoorbeeld objecttype → `class`, attribuut → `property`, parameter → `variable` met `readonly`, domeinwaarde →
  `enumMember`, sleutelwoord → `keyword`), zodat een latere language server de uitvoer kan doorgeven. Een language
  server zelf hoort niet bij deze kaart.
- **Toegang.** Een publieke ingang in Java naast `Resolver.resolve`: meerdere bestanden met naam en tekst erin, spans
  per bestand en de diagnostics eruit; de distributie roept die aan. Daarnaast een CLI-modus `Resolver --tokens
  <out.json> <file.rgs…>`. Hij schrijft ook bij ERRORs en syntaxfouten, want dan is de uitvoer het nuttigst: exit 0 als
  het bestand geschreven is, 2 bij verkeerd gebruik. Wat een token krijgt dat buiten een ontlede naam valt na een
  syntaxfout (zijn lexicale soort of `unresolved`), kiest en documenteert de bouwer.
- **Het IR verandert niet**: `--ir` en `--cases` blijven bytegelijk.

Niet in deze kaart: een testdocument van de scenariolaag (`--cases`, `ScenarioResolver`) als invoer, tenzij het met
weinig code meekomt (anders als open punt in de Result); de distributie (subcommando, endpoint, pin-bump) en de
afnemer, elk met een eigen kaart.

## Acceptatie

- (a) Rood eerst: een JUnit-getuige op een eigen Jutterswet-model met elk soort naam, een naam over twee regels, een
  onopgeloste naam, een `// Bron:`-commentaar, twee bestanden met een verwijzing over de bestandsgrens heen, en een
  teken buiten het BMP in een commentaar vóór een naam op dezelfde regel (de UTF-16-kolom).
- (b) Een corpusgetuige in de JUnit-suite, over elke `corpus/golden/**/source.rgs` (128 op `680e5263`) en de 47
  `toka/casus/toka-*.rgs` samen als één document: de dekking (elk token in precies één span, geen overlap), de tekst
  op elke span is de brontekst op die positie, elke verwijzing wijst naar een span met `declaration: true` van dezelfde
  soort, en een document zonder diagnostics heeft geen span `unresolved`. Een uitzondering daarop staat in een lijst
  met reden die faalt als ze verouderd is. Vloeren in de test (bestanden, spans, naamspans per soort), gemeten bij de
  bouw.
- (c) Als meting, niet als getuige en zonder bestand in deze repository: de v2-regelset herleving-WW (fundament en
  rules, alleen lezen) geeft volledige dekking en geen `unresolved`; de uitkomst en de telling per soort in de Result.
- (d) De vorm van de uitvoer, de soorten, de positieconventie en de LSP-tabel staan in `grammar/DIVERGENCES.md`, bij de
  beschrijving van de CLI (r. ±2996) of in een eigen sectie.
- (e) De zeventien gates groen; gate 10, 11 en 16 tonen dat `--ir` en `--cases` bytegelijk bleven.

May touch: `frontends/regelspraak/**`, deze kaart. Geen schema, geen spec, geen gate-script.
