# Opdracht: stel per voorkomen voor of een oude naam verandert of blijft

Je krijgt een partij `rest-NNN.jsonl` (de rijen die de voorsortering, `rename.py triage`, niet besliste), of de
beslislijst `scripts/lib/naming/decisions/<zone>.tsv` met een bereik van rijen (bijvoorbeeld rij 1 tot en met 200;
rij 1 is de eerste regel na de kop). Daarbij de woordenlijst `scripts/lib/naming/glossary.tsv` en het pad van je
antwoordbestand: bij partij `X.jsonl` is dat `X.answer.jsonl`. Je leest alleen die bestanden, en de bestanden die de
rijen noemen als de kolom `context` niet genoeg zegt. Je verandert niets anders dan je antwoordbestand.

## Wat erin staat

Een hernoemkaart vervangt Nederlandse namen in de code door Engelse namen. Elke rij is één plek waar een oude naam
(`old`) staat op een plaats die het script niet zelf mag veranderen: in een string, in commentaar (een naam van één
woord), in een tekstbestand (documentatie, een `NOTE.md`, `pom.xml`, een script), of buiten de zone van de kaart.
Velden: `file`, `line`, `col` (waar het voorkomen begint), `old`, `new` (de nieuwe naam; bij meer nieuwe namen
gescheiden door `|`), `context` (de regel waarin het staat), en in een partij `row` (het rijnummer in de lijst). In
de lijst zelf heten ze kolommen, met daarbij `decision` (leeg).

## Wat de voorsortering al besliste

De voorsortering besliste de rijen waarover de regels van de kaart zeker zijn; die staan niet in een partij:
- RegelSpraak-tekst: een `.rgs`-bestand, of een string die een RegelSpraak-bron is (`keep`);
- de draad: een `ir.json`, een `case*.json`, een string die IR- of case-JSON is (`keep`);
- meldingen die ze aan de aanroep herkent: de tekst van `new ...Exception(...)` en van de weigeringstypen
  (`new Weigering(...)`), van `fail(...)`, het laatste argument van een `assert...`, een `println`, en de verwachting
  van een melding (`e.getMessage().contains("...")`) (`keep`);
- een functiewoord (`de`, `dan`, `met`) in Nederlandse tekst van een string of in commentaar (`keep`);
- een naam in `{@code ...}`, `<code>...</code>` of backquotes die in dat bestand gedeclareerd is of een type van de
  zone is (`change`);
- een woord in een Nederlandse commentaarregel of een Nederlands citaat in commentaar (`keep`).
- met `--default-keep` (kaart `hernoem-drools-backend`): elke andere string en commentaarregel in de code van de
  zone, en elk `.txt`-bestand van de zone (`keep`); de testsuite laat daarna zien welke string een hernoemde
  naam moet volgen. Een partij houdt dan alleen `README.md` en `pom.xml` van de zone en de rijen buiten de zone.

Wat overblijft is vooral: strings die geen melding zijn of waarvan de voorsortering de melding niet herkende (een
melding in een variabele, een hulpmethode als `weiger(...)` of `BeideTakken.geweigerd(...)`, `String.format`),
verwachte dumps in tests, DRL-tekst, namen voor reflectie, paden, commentaar in het Engels of in gemengde taal, en
alles buiten de zone. Kom je toch een melding, RegelSpraak-tekst of draad tegen, dan blijft die `keep`.

## Wat je doet

Per rij een beslissing:

- `change`: het voorkomen noemt het ding dat hernoemd wordt. Bijvoorbeeld: een klasse-, methode-, veld- of testnaam
  in documentatie of commentaar (`{@code leesWaarde}`, tussen backquotes, in een pad als
  `.../loader/TellingBronarmWitnessTest.java`), een bestandsnaam (`"aggregatie.flogic"`), of een string die de code
  gebruikt om het ding op te zoeken (een naam voor reflectie, een veldnaam die een andere klasse leest, een
  property).
- `keep`: het voorkomen is gewoon Nederlands of domeininhoud, en geen verwijzing naar de code. Bijvoorbeeld: een
  Nederlands woord in een Nederlandse zin (`de waarde van het attribuut`), een melding aan de gebruiker, RegelSpraak-
  tekst, een gedeclareerde naam uit het corpus (`Vondst`, `aantal vrienden`), een sleutelwoord van RegelSpraak, of
  een v0-naam in de spec (`v0 name: ...`, `spec/CONSTRUCTS.md`).
- Bij meer nieuwe namen in `new`: `change:<een van die namen>`, de naam die bij deze plek hoort. De eerste is die
  van de rij op deze plek; de andere zijn de andere nieuwe namen van dezelfde oude naam (een map, een component
  van een record). Een gewone `change` mag dan niet.
- Twijfel je, kies `keep` en zeg in `why` waarom; de sessie beslist.

Een melding (een zin die een gebruiker of een gate te zien krijgt) blijft in deze stap altijd `keep`, ook als er
een codenaam in staat: meldingen zijn een eigen kaart. Ook `keep`: een sleutel of waarde op de draad (ook in een
tekstblok van een test), een v0-spelling in een test die haar weigert, een corpuspad of case-slug (`negatief`,
`rood`, de mappen `jutterswet-*`), een string die een script of programma buiten de zone leest of meegeeft (een
systeemeigenschap zoals `uchar.drlvorm`, een vlag van de replay-runner, een regel die een gate zoekt), en een
shellvariabele in een gate-script.

Strings die `change` zijn, omdat de code of een script er de hernoemde naam mee opzoekt:
- de routenamen van `plan/Route.java`: `ZET(Rekenhulpen.class, "zet")` noemt de methode die de route aanroept; de
  string volgt de methode;
- namen voor reflectie: een eigenschap die `Rekenhulpen.langs(a, List.of("volgende"))` leest, een klassenaam die
  met `getSimpleName()` vergeleken wordt, waar dat lid of die klasse een Java-declaratie van de zone is die hernoemd
  wordt. Een getter of klasse van een gegenereerde bean (`"getRapport"`) heet naar een gedeclareerde naam en blijft
  `keep`; weet je niet of de klasse gegenereerd is, kies `keep` met `why` `reflectie: <naam>`;
- resourcepaden naar een hernoemde map of een hernoemd bestand onder `src/test/resources` (`zoals-ref` heet
  `like-ref`, `dagsoorten` heet `day-kinds`): een `Path.of(...)`, een resource-naam, een pad in commentaar. Een
  padstuk kiest de schrijfwijze van de nieuwe map (`dayKinds|day-kinds`: `change:day-kinds`);
- in een gate-script (`scripts/check-*.sh`) en in `CLAUDE.md`: de naam van een testklasse die het script draait of
  zoekt, en een pad in `backends/drools`.
- in een verwachte dump of DRL-tekst van een test: een klassenaam of enumconstante die de code met haar naam
  afdrukt (`getSimpleName()`, `name()`, de klasse in de DRL-tekst), waar die naam hernoemd wordt. Een label dat
  de dumpcode als string schrijft (`toets`, `waarde`), en de bron tussen aanhalingstekens (`bron 'de rang van de
  Wachter'`), blijven `keep`.

## Antwoordformaat

Eén JSON-regel per rij van je partij of je bereik, in de volgorde waarin ze staan:

    {"row": 17, "old": "lees", "decision": "keep", "why": "Nederlandse zin in commentaar"}

`row` is het rijnummer in de lijst (in een partij het veld `row` van de rij; in de lijst is 1 de eerste regel na de
kop), `old` het veld `old` van die rij, `decision` `keep`, `change` of `change:<naam>`, en `why` een korte reden van
hoogstens tien woorden.

## Hoe je schrijft

Schrijf het antwoordbestand in delen: eerst met de Write-tool de eerste hoogstens 50 regels, daarna met Edit
steeds hoogstens 50 regels erbij (vervang de laatste regel door die regel plus de nieuwe). Elke regel is één geldig
JSON-object, zonder lege regels. Tel aan het eind: één regel per rij van je partij of je bereik. Meld in je laatste
bericht het aantal regels en hoeveel daarvan `change` zijn.
