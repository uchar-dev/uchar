# Opdracht: vertaal opnieuw wat `check` weigerde of wat je eerder markeerde

Je krijgt drie paden: een partijbestand van de tweede ronde (`.../retry-names-NNN.jsonl` of
`.../retry-sentences-NNN.jsonl`), de woordenlijst `scripts/lib/naming/glossary.tsv`, en het antwoordbestand dat je
schrijft (hetzelfde pad met `.answer.jsonl` in plaats van `.jsonl`). Lees eerst `scripts/lib/naming/prompts/translate.md`:
die regels gelden hier ook, en het antwoord heeft hetzelfde formaat. Lees daarna de woordenlijst helemaal, dan het
partijbestand. Je leest geen andere bestanden en je verandert niets anders dan het antwoordbestand.

## Wat erin staat

Elke regel is een naam uit de eerste ronde, met dezelfde velden als daar (`id`, `name`, `kind`, `file`, `line`,
`declaration`, `doc`, `words`, `dutch`, `domain`, `occurrences`, `files`), en twee velden erbij:

- `previous`: jouw eerdere antwoord (`new`, `words`, `flag`);
- `errors`: wat `check` over dat antwoord zei, letterlijk; leeg als je de naam markeerde.

`dutch` en `domain` zijn opnieuw uit de woordenlijst berekend: de woordenlijst kreeg sinds de eerste ronde rijen en
een scherpere `context` (onder meer `fout`, `lezing`, `grens`, `soort`, `deel`, `dubbel`, `voor`, `toekenning`,
`een`, `niet`, `waar`, `op` en `van`), en `domain` noemt de domeinwoorden van de naam.

## Wat je doet met elke melding

- `the content words of NEW (...) are not the glossary translations of WORDS (A + B/C + ())`: de inhoudswoorden van
  je `new` zijn niet de vertalingen uit de woordenlijst. Tussen de haakjes staan per woord van `words` de toegestane
  vertalingen, gescheiden door `/`; `()` betekent dat het woord weg mag. Schrijf `new` met precies één van die
  vertalingen per woord, plus Engelse functiewoorden. Geen synoniem: staat het woord dat je wilt er niet bij, kies
  dan de vertaling uit de lijst die bij `declaration` en `doc` past, of markeer de naam.
- `...; domain words stay as written: X`: X is domeininhoud (een gedeclareerde naam van het corpus of van een
  RegelSpraak-bron of IR-document in de code). Neem X letterlijk over in `new` (`vondstWacht` wordt `vondstGuard`).
- `word X holds glossary words (A, B): split it in the words column`: splits X in `words` op die woorden.
- `new NEW still holds Dutch: X`: vertaal X met de woordenlijst.
- `van starts old ...` of `van ends old ...`: `van` aan het begin of het eind van de naam blijft, als `of` of
  `from` op dezelfde plaats (`indexVan` wordt `indexOf`, `vanObject` wordt `fromObject`).
- `new NEW joins English words ...` of `... is not written by its convention ...`: neem de spelling over die na
  `write` staat.
- Een andere melding (`not injective`, `not fresh`, `declared outside the zone`, `used as code outside it`, `is a
  member of a zone type`): die lost een andere sessie op met een scope of een andere naam. Vertaal de naam gewoon
  goed en laat zo'n melding verder rusten.
- Geen melding, wel een eerdere `flag`: je markeerde de naam. Kijk of `dutch`, `domain` en de woordenlijst nu een
  antwoord geven (een woord dat er nu wel in staat, een `context` die nu kiest, een domeinwoord dat blijft). Zo ja:
  vertaal hem, met lege `flag`. Zo nee: markeer hem opnieuw, met de reden die nu nog geldt.

Markeer een naam ook nu als de letterlijke vertaling niet zegt wat het ding is; die gaat naar een sessie die een
betere naam kiest.

## Hoe je schrijft

Per invoerregel precies één antwoordregel, met hetzelfde `id`, in dezelfde volgorde, in het formaat van
`translate.md` (`id`, `old`, `new`, `words`, `flag`), in delen van hoogstens 50 regels zoals daar beschreven. Meld
in je laatste bericht het aantal regels en het aantal gemarkeerde namen.
