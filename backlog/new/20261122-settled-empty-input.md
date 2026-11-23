# settled-empty-input: een casus kan zeggen dat er vaststaand geen waarde is

*Geparkeerd door de eigenaar op 2026-11-22. Voorstel van de hoofdsessie van dezelfde dag; niet begonnen.*

Reden: een tweede weg om op een leeg attribuut door te rekenen maakt de lezing van leeg moeilijker te begrijpen, en zij moet op bekende logica blijven rusten; sluiten is aan de auteur (`(volledig gegeven)`, een kenmerk). Sinds het
besluit van 2026-11-23 (kaart `one-empty`) vervalt ook de marker `niet van toepassing`; "er is er geen" als invoer bestaat dan alleen nog als kenmerk dat de casus geeft. Wat overblijft, is een aanvrager die zeker weet dat er niets is bij een attribuut zonder marker, en een strikt
antwoord `{"state": "empty"}` dat als verzoek naar een volgende regelset gaat. Weer oppakken als een van die twee zich voordoet.

## Aanleiding

In een document met `inferFromEmpty: false` kan een casus van een kenmerk zeggen dat het niet geldt (`characteristicsAbsent`, ch. 1.5 "The case"), en van een attribuut niet dat er geen waarde is: een invoerattribuut zonder waarde is open, tenzij zijn declaratie `(volledig gegeven)` zegt. Een aanvrager die zeker weet dat er geen bergloon is, kan dat dus niet
melden: laat hij het weg, dan blijft elke som over dat lid open en blijft de dienst ernaar vragen; vult hij 0 in, dan is het een gevulde waarde (`is leeg` is onwaar, een gemiddelde verandert). De auteur kan het lid `(volledig gegeven)` verklaren, maar dan is het bij een andere aanvrager,
die het gewoon niet weet, ook vaststaand leeg. En een strikt antwoord `{"state": "empty"}` van de ene regelset kan niet als verzoek de volgende in.

## Voorstel

1. Het caseschema krijgt per object naast `characteristicsAbsent` een lijst `attributesEmpty`: de invoerattributen waarvan vaststaat dat er geen waarde is. In een document zonder de eigenschap verandert zij geen uitkomst.
2. Ch. 1.5 "The case": zo'n attribuut is vaststaand leeg. Naast een regel die het schrijft, geldt het als elke invoerwaarde: het staat waar van elke schrijver vaststaat dat hij niets vult (dan stond het er al), en is open zolang een schrijver open is. Naast `(volledig gegeven)` verandert het niets. Een attribuut in `attributes` en in
   `attributesEmpty` is een fout van de casus, zoals EF-047 voor een kenmerk in beide lijsten. Monotonie: het is een gegeven, dus de eis geldt ongewijzigd (het weglaten van `attributesEmpty` laat het lid open).
3. `ref`, clingo (`settled/2`), Drools (`Input`), gate 4 (vorm), gate 5 (behoud en monotonie), een golden met drie casussen (weggelaten, `attributesEmpty`, een waarde), een rode casus.
4. De toolchain: een strikt verzoek `{"state": "empty"}` op een attribuut. `openInputs` vraagt het dan niet meer. Een gewone regelset weigert het als elk object op de plaats van een waarde.

## Besluiten voor de eigenaar

- De naam `attributesEmpty` (en `{"state": "empty"}` in het verzoek).
- Of de scenariolaag van het dialect een zin krijgt ("zijn bergloon is leeg" als gegeven); voorstel: nee, zolang niemand erom vraagt.
