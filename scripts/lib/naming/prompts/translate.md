# Opdracht: vertaal een partij Nederlandse namen naar het Engels

Je krijgt drie paden: een partijbestand (`.../names-NNN.jsonl` of `.../sentences-NNN.jsonl`), de woordenlijst
`scripts/lib/naming/glossary.tsv`, en het antwoordbestand dat je schrijft (hetzelfde pad met `.answer.jsonl` in
plaats van `.jsonl`). Lees eerst de woordenlijst helemaal, dan het partijbestand. Je leest geen andere bestanden en
je verandert niets anders dan het antwoordbestand.

## Wat erin staat

Elke regel van het partijbestand is één JSON-object: één naam uit de code van de repository uchar (Java, OCaml,
shell of ANTLR), of een bestands- of mapnaam. Velden: `id`; `name` (de naam zoals hij nu is); `kind` (`type`,
`method`, `test`, `variable`, `parameter`, `constant`, `value`, `field`, `constructor`, `module`, `rule`, `label`,
`file`, `dir` of `reference`); `file` en `line` (waar hij gedeclareerd is); `declaration` (die regel code); `doc`
(de eerste zin van het commentaar erboven, soms leeg); `words` (een voorstel voor de splitsing in woorden,
gescheiden door `|`); `dutch` (elk Nederlands woord met de Engelse woorden uit de woordenlijst, elk met zijn
`context` als er meer zijn); `domain` (de domeinwoorden van de naam: woorden van een gedeclareerde naam van het
corpus of van een RegelSpraak-bron of IR-document in de code, zoals `vondst`, `jutter`, `boei`); `occurrences` en
`files` (hoe vaak en in hoeveel bestanden).

De woordenlijst heeft de kolommen `dutch`, `english`, `source`, `context`, `note`. Een Nederlands woord met meer
regels heeft meer betekenissen; `context` zegt welke wanneer geldt. `english` in lowerCamel (`emptyFallback`) is
één term van meer woorden.

## Wat je doet

Per invoerregel precies één antwoordregel, met hetzelfde `id`, in dezelfde volgorde:

    {"id": "n-001-007", "old": "invoervormFout", "new": "inputFormError", "words": "invoer|vorm|fout", "flag": ""}

- `old` is `name`, letterlijk overgenomen.
- `words` is de splitsing van `old` in woorden, kleine letters, gescheiden door `|`. Aan elkaar geschreven moet
  het `old` zijn (hoofdletters, `_`, `-` en `'` tellen niet). Neem het voorstel over als het klopt; splits een
  Nederlandse samenstelling op de woorden van de woordenlijst (`invoervormfout` wordt `invoer|vorm|fout`).
- `new` is de Engelse naam:
  1. Vertaal elk Nederlands woord met een woord uit de woordenlijst. Kies bij meer regels de regel waarvan
     `context` past bij `declaration` en `doc`. Een woord dat al Engels is (`label`, `bean`, `json`), een getal en
     een afkorting (`tms`, `drl`, `attr`) blijven zoals ze zijn. Een domeinwoord (`domain`) is domeininhoud en
     blijft zoals het geschreven is, ook als je een Engels woord weet: `vondstWacht` wordt `vondstGuard`, niet
     `findGuard`; markeer een naam niet omdat een domeinwoord niet in de woordenlijst staat.
  2. Zet de woorden in Engelse volgorde en laat Nederlandse functiewoorden weg waar het Engels ze niet nodig
     heeft: `labelVanBean` wordt `beanLabel`, niet `labelOfBean`. Een ontkenning (`niet`, `geen`, `zonder`,
     `nooit`) blijft altijd staan. `van` aan het begin of het eind van een naam blijft, als `of` of `from`
     (`indexVan` wordt `indexOf`, `vanObject` wordt `fromObject`, `rVan` wordt `rFrom`); `op` vertaal je met de
     rij van de woordenlijst die bij de betekenis past (`rolOpNaam` wordt `roleByName`, `filterOpOnderwerp` wordt
     `filterOnSubject`, `afgerondOp2` wordt `roundedTo2`).
  3. Schrijf `new` volgens de conventie van zijn taal, elk Engels woord apart, ook als `old` één Nederlands woord
     is: hoofdletter of kleine letter vooraan zoals `old`; `_` of `-` tussen de woorden als `old` die heeft,
     camelCase als `old` camelCase is; verder in Java camelCase (`creatiediepte` wordt `creationDepth`,
     `Sessiedump` wordt `SessionDump`), een constante in hoofdletters met `_` tussen de woorden (`WORTEL` wordt
     `SQUARE_ROOT`, `MINSTENS` wordt `AT_LEAST`, `VERSIE` wordt `VERSION`), in OCaml en shell met `_` tussen de
     woorden (`feittype` wordt `fact_type`), en een bestands- of mapnaam zoals de namen ernaast (`bronroute` wordt
     `source-route`, naast `zoals-ref`). Schrijf nooit Engelse woorden aan elkaar (`creationdepth`, `SQUAREROOT`,
     `sourceroute`): `merge` schrijft elk antwoord volgens deze regel en `check` weigert een andere schrijfwijze.
  4. Verzin niets: geen synoniem, geen nieuw beeld, geen betere naam. Dat doet een andere stap.
- `flag` is leeg, of een korte zin die zegt waarom je de naam niet vertaalt. Markeer een naam (en laat `new`
  leeg) als:
  - de letterlijke vertaling niet zegt wat het ding is, bijvoorbeeld omdat het Nederlandse woord een beeld is
    (`sonde`, letterlijk `probe`, voor iets dat niets meet) of omdat `declaration` en `doc` iets anders laten zien
    dan de woorden zeggen;
  - een Nederlands woord niet in de woordenlijst staat en je de vertaling niet zeker weet;
  - de woordenlijst voor een woord meer betekenissen geeft en `declaration` en `doc` niet zeggen welke geldt.
  Een gemarkeerde naam gaat naar een sessie die de code leest en zelf een naam kiest.
- Een `scope`-veld laat je weg, tenzij de invoer zelf zegt dat dezelfde naam op verschillende plaatsen iets anders
  betekent; dan schrijf je `"scope": "<pad van het bestand>"` en markeer je de naam.

### Testnamen in zinsvorm (`sentences-NNN.jsonl`)

`kind` is `test`: een JUnit-testmethode waarvan de naam een Nederlandse zin is, woorden gescheiden door `_`
(`een_knoop_met_de_v0_spelling_van_zijn_soort_weigert`). Schrijf `new` als een Engelse zin in kleine letters met
`_` (`a_node_with_the_v0_spelling_of_its_kind_is_refused`): dezelfde bewering, met de inhoudswoorden uit de
woordenlijst, in Engelse woordvolgorde. Functiewoorden (`de`, `het`, `een`, `van`, `wordt`) mogen veranderen,
verdwijnen of erbij komen; de inhoudswoorden en de ontkenningen niet. Een code-term in de zin (`v0`, `wf113`,
`drl`, een klassenaam) blijft staan.

## Hoe je schrijft

Schrijf het antwoordbestand in delen, anders breekt je uitvoer af: maak het eerst met de Write-tool met de eerste
hoogstens 50 antwoordregels, en voeg de rest toe met Edit, steeds hoogstens 50 regels per keer (vervang de laatste
regel door die regel plus de nieuwe regels). Elke regel is één geldig JSON-object, zonder komma aan het eind, zonder
lege regels. Tel aan het eind: evenveel antwoordregels als invoerregels, elk `id` één keer. Meld in je laatste
bericht het aantal regels en het aantal gemarkeerde namen.
