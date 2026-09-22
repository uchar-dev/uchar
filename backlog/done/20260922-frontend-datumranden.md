# frontend-datumranden: kale NPE en stil tijdverlies bij datuminvoer

Twee gemeten defecten, gevonden door de P5-lens (beide ouder dan die
kaart, beide via de CLI gereproduceerd), zelfde familie: de frontend
verliest of crasht op datumranden waar de fail-fast-regel een benoemde
fout eist.

1. De KALE-datumtak (`ExpressieBouwer.java` ~444) crasht met een
   `NullPointerException` op een onmogelijke datum (`30-02-2026`):
   `Literalen.parseDatum` geeft null en de tak leest ongewacht door.
   De datumtijd-tak geeft sinds de P5-kaart een ERROR-diagnostic
   zonder uitvoer; de kale tak hoort dezelfde benoemde weigering te
   krijgen.
2. `geldig vanaf 01-01-2026 10:00:00.000` emitteert stil
   `"vanaf": "2026-01-01"` — de meegeschreven tijdcomponent valt
   zonder diagnostic weg. Stil informatieverlies; de frontend hoort te
   weigeren of de component te dragen (weigeren ligt voor de hand: de
   geldigheidsvelden zijn datumgranulair).

## Acceptatie (bij oppakken aanscherpen)

- (a) Beide paden geven een benoemde ERROR-diagnostic zonder uitvoer;
  rood-eerst een JUnit-getuige per pad (de NPE resp. de stille emissie
  eerst gezien).
- (b) Corpussweep: geen bestaande golden of toka-fixture draagt een
  van beide vormen (de lens telde nul dragers — hermeet bij oppakken).
- (c) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, `corpus/**`,
kaartverplaatsing.

## Result:

Afgerond 2026-09-22 op `card/frontend-datumranden`, inclusief een
fixronde na de lens (F1, F2, A2 en A4-A7).

(a) Alle paden weigeren met een benoemde ERROR. De CLI schrijft dan geen
bestand en eindigt met exit 1.
- Kale datumliteral (`ExpressieBouwer`): zelfde patroon als de
  datum-tijd-tak, melding `datum-literal '<lexeem>' is geen geldige datum
  (§13.2.19)`. De tak toetst eerst het HELE lexeem
  (`[dd. ]d-m-jjjj`) en roept pas daarna de gedeelde lezer aan.
  `Literalen.parseDatum` zelf is ongewijzigd: de backends delen die
  lezer, en de lezerkant hoort bij het kaartje
  `20260922-datum-lexeem-hele-match`.
- Versiegeldigheidsgrens (`RuleChecker.grensOfMeld`/`grens`, de enige
  plek waar een `Geldigheid` ontstaat, ook voor beslistabellen): `grens`
  matcht het hele lexeem in plaats van `find()`. Een tijdcomponent geeft
  `versiegeldigheidsgrens '<grens>' draagt een tijdcomponent; ...`. Een
  grens die buiten de kalender valt EN een tijd draagt, meldt beide
  fouten tegelijk. Beide meldingen worden ontdubbeld met `contains`,
  zodat een beslistabel met tijd in zijn kopgeldigheid één fout geeft en
  niet één per tabelrij. Gekozen is weigeren en niet dragen:
  `Geldigheid` houdt `LocalDate`s en WF-101 pint de grens op de wire op
  het ISO-datumlexeem.

Rood eerst gezien in `IrEmitterTest`. Alle getuigen pinnen de volledige
foutzin.
- `datum_literal_buiten_de_kalender_is_een_fout`: `NullPointerException:
  Cannot invoke "java.time.LocalDate.toString()" because the return value
  of "...Literalen.parseDatum(String)" is null`
- `versiegeldigheid_met_tijdcomponent_is_een_fout`: `vanaf 01-01-2026
  10:00:00.000: verwacht precies een fout, kreeg: [] ==> expected: <1>
  but was: <0>`
- `datum_literal_met_meercijferige_dag_is_een_fout`: `2024-01-2026 ==>
  expected: <[datum-literal '2024-01-2026' is geen geldige datum
  (§13.2.19)]> but was: <[]>`. De tweede ingang via de CLI gezien:
  `123-01-2026` emitteerde `"waarde": "2026-01-23"`.
- `versiegeldigheid_met_meercijferige_dag_is_een_fout`: `2024-01-2026
  ==> expected: <[ongeldige versiegeldigheidsgrens '2024-01-2026' (...)]>
  but was: <[]>`. Voordien emitteerde dit `"vanaf": "2026-01-24"`.
- `versiegeldigheid_met_onmogelijke_datum_en_tijd_meldt_beide`: gaf
  alleen de tijdcomponent-melding.
- `beslistabel_met_tijdcomponent_in_geldigheid_meldt_eenmaal`: gaf
  dezelfde melding twee keer, één keer per tabelrij.

(b) Hermeten over de getrackte bronnen: 117 `.rgs`-bestanden (70 in
corpus, 47 in toka) plus 2 Java-testbronnen in toka. In de `.rgs` staan
9 datumtokens, waarvan 3 met tijd. Nul liggen buiten de kalender, nul
hebben een dag of maand van meer dan twee cijfers, nul
geldigheidsgrenzen dragen een tijd (er zijn 3 begrensde geldigheden,
alle zonder tijd). Gate 10 (70 rgs-goldens) en gate 11 (45 .rgs)
re-emitteren byte-identiek, ook met de strengere guards.

(c) Alle veertien gates PASS, elk als losse aanroep. Na de fixronde zijn
gates 1, 3, 10 (380 tests), 11 en 12 (179/156, JUnit 549) opnieuw
gedraaid en allemaal PASS.

Zusterranden, gemeten en NIET gefixt:
1. De lezerkant van de find()-rand: `Literalen.parseDatum` zoekt nog met
   `find()`. De frontend is nu afgeschermd. Alle backendaanroepers lezen
   de ISO-waarde van de wire, die WF-084 afschermt. Zie kaartje
   `20260922-datum-lexeem-hele-match`.
2. Het startpunt van een tijdlijn valt weg bij ALLE vormen: `startend op
   <datum>` bij week en bij meervoudigePeriode, en de
   `startend op dag N ...`-varianten voor maand, kwartaal en jaar. Alleen
   de granulariteit (en het periodeaantal) komt op de wire. Een datum
   buiten de kalender in het startpunt (`30-02-2026`) passeert zonder
   fout. Het bestaande commentaar in
   `toka/src/test/java/io/github/yhavinga/regelspraak/ModelTest.java`
   documenteert dat het startpunt wegvalt al. Zie kaartje
   `20260922-tijdlijn-startpunt-emissie`.
3. `regelversie X (vanaf <grens>) is gevuurd`: de versie-aanduiding
   staat alleen in `bron`. Dat is bewust en gedocumenteerd
   (`bouwRegelversie`), maar een tijdcomponent verdwijnt daar dus net zo
   stil als de hele grens.
4. Een fout in de kopgeldigheid van een beslistabel meldt het regelnummer
   van de gedesugarde regel (regel 2 van de synthetische bron) en niet de
   bronregel. Dat gebrek is ouder dan deze kaart en is niet gefixt.
5. Geen stil verlies bij deze vormen: een periodegrens met datum-tijd
   gaat als `DatumTijd`-literal op de wire; een dagsoortdefinitie met een
   datum-tijd-vergelijking wordt door de typering geweigerd;
   parametersets hebben geen tekstpad, alleen de Java-API met
   `LocalDate`. De backendlezers van `parseDatum`/`parseDatumTijd` vangen
   allemaal null af (marker of drop).

Registervoorstel: er is geen rij die precies dit gedrag beschrijft. De
dichtstbijzijnde zijn rij 27 (WF-084, het datumlexeem) en de
`Geldigheid`-rij (WF-101). Een zin die aan de `Geldigheid`-rij kan
worden toegevoegd: "Producentenkant (2026-09-22, kaart
`frontend-datumranden`): de RegelSpraak-frontend leest een
versiegeldigheidsgrens en een kale datumliteral als heel lexeem en
weigert een grens met een tijdcomponent, een datum buiten de kalender
en een dag of maand van meer dan twee cijfers met een benoemde ERROR.
Emittergetuigen in `IrEmitterTest`, gate 10."
