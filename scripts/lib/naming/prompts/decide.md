# Opdracht: stel per voorkomen voor of een oude naam verandert of blijft

Je krijgt de beslislijst `scripts/lib/naming/decisions/<zone>.tsv`, een bereik van rijen (bijvoorbeeld rij 1 tot en
met 200; rij 1 is de eerste regel na de kop), de woordenlijst `scripts/lib/naming/glossary.tsv` en het pad van je
antwoordbestand. Je leest alleen die bestanden, en de bestanden die de rijen noemen als de kolom `context` niet
genoeg zegt. Je verandert niets anders dan je antwoordbestand.

## Wat erin staat

Een hernoemkaart vervangt Nederlandse namen in de code door Engelse namen. Elke rij is één plek waar een oude naam
(`old`) staat op een plaats die het script niet zelf mag veranderen: in een string, in commentaar (een naam van één
woord), in een tekstbestand (documentatie, een `NOTE.md`, `pom.xml`, een script), of buiten de zone van de kaart.
Kolommen: `file`, `line`, `col` (waar het voorkomen begint), `old`, `new` (de nieuwe naam; bij meer nieuwe namen
gescheiden door `|`), `decision` (leeg) en `context` (de regel waarin het staat).

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
- Bij meer nieuwe namen in `new`: `change:<een van die namen>`, de naam die bij deze plek hoort.
- Twijfel je, kies `keep` en zeg in `why` waarom; de sessie beslist.

Een melding (een zin die een gebruiker of een gate te zien krijgt) blijft in deze stap altijd `keep`, ook als er
een codenaam in staat: meldingen zijn een eigen kaart.

## Antwoordformaat

Eén JSON-regel per rij van je bereik, in de volgorde van de lijst:

    {"row": 17, "old": "lees", "decision": "keep", "why": "Nederlandse zin in commentaar"}

`row` is het rijnummer in de lijst (1 is de eerste regel na de kop), `old` de kolom `old` van die rij, `decision`
`keep`, `change` of `change:<naam>`, en `why` een korte reden van hoogstens tien woorden.

## Hoe je schrijft

Schrijf het antwoordbestand in delen: eerst met de Write-tool de eerste hoogstens 50 regels, daarna met Edit
steeds hoogstens 50 regels erbij (vervang de laatste regel door die regel plus de nieuwe). Elke regel is één geldig
JSON-object, zonder lege regels. Tel aan het eind: één regel per rij van je bereik. Meld in je laatste bericht het
aantal regels en hoeveel daarvan `change` zijn.
