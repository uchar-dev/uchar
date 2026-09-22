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

Afgerond 2026-09-22 op `card/frontend-datumranden`.

(a) Beide paden weigeren met een benoemde ERROR, de CLI schrijft geen
bestand en eindigt met exit 1. Kale tak (`ExpressieBouwer`): zelfde
patroon als de datum-tijd-tak, melding `datum-literal '30-02-2026' is
geen geldige datum (§13.2.19)`. Geldigheidsgrens (`RuleChecker.grensOfMeld`,
de enige plek waar een `Geldigheid` ontstaat, ook voor beslistabellen):
melding `versiegeldigheidsgrens '01-01-2026 10:00:00.000' draagt een
tijdcomponent; de geldigheid is datumgranulair (...)`. Gekozen is
weigeren en niet dragen: `Geldigheid` houdt `LocalDate`s en WF-101 pint
de wire-grens op het ISO-datumlexeem, dus er is geen drager. Rood eerst
gezien in `IrEmitterTest`:
- `datum_literal_buiten_de_kalender_is_een_fout`: `NullPointerException:
  Cannot invoke "java.time.LocalDate.toString()" because the return value
  of "...Literalen.parseDatum(String)" is null`
- `versiegeldigheid_met_tijdcomponent_is_een_fout` (vanaf, t/m, en t/m
  na een jaargrens): `vanaf 01-01-2026 10:00:00.000: verwacht precies een
  fout, kreeg: [] ==> expected: <1> but was: <0>`

(b) Hermeten over de 119 RegelSpraak-bronnen in corpus/ en toka/ (70 +
49): 11 datumtokens, 3 met tijd, 0 buiten de kalender, 0
geldigheidsgrenzen met tijd (3 begrensde geldigheden, alle kaal). Gate
10 (70 rgs-goldens) en gate 11 (45 .rgs) re-emitteren byte-identiek.

(c) Alle veertien gates PASS, elk als losse aanroep.

Zusterranden, gemeten en NIET gefixt:
1. Een datumtoken met een dag van meer dan twee cijfers wordt stil
   verkeerd gelezen. De DATE-token neemt `DIGIT+` voor dag en maand.
   `2024-01-2026` als literal emitteert `"waarde": "2024-01-20"`
   (`Literalen.parseDatum` doet `find()` met het ISO-patroon eerst), en
   `geldig vanaf 2024-01-2026` emitteert `"vanaf": "2026-01-24"`
   (`RuleChecker.grens` doet `find()` op `d-m-jjjj`). Nul dragers in het
   corpus. De remedie is heel-lexeem-matching zoals `parseDatumTijd` die
   al heeft, maar `parseDatum` wordt gedeeld met de Drools-backend, dus
   dit is een eigen kaart.
2. `voor elke week startend op <datum>` (§13.3.6.3): het startpunt wordt
   helemaal niet gelezen, met of zonder tijd; alleen `WEEK` komt op de
   wire.
3. `regelversie X (vanaf <grens>) is gevuurd`: de versie-aanduiding
   staat alleen in `bron`. Dat is bewust en gedocumenteerd
   (`bouwRegelversie`), maar een tijdcomponent verdwijnt daar dus net zo
   stil als de hele grens.
4. Een periodegrens (`vanaf 01-01-2026 10:00:00.000` als
   periodevergelijking) gaat als `DatumTijd`-literal op de wire: wordt
   gedragen, niet stil weggelaten. Dagsoortdefinitie met een
   datum-tijd-vergelijking: de typering weigert. Parametersets hebben
   geen tekstpad, alleen de Java-API met `LocalDate`.
5. De backendlezers van `parseDatum`/`parseDatumTijd` (DRL-tak,
   exec-model, `Dagsoorten`) vangen allemaal null af (marker of drop).

Registervoorstel: er is geen rij die precies dit gedrag beschrijft. De
dichtstbijzijnde zijn rij 27 (WF-084, het datumlexeem) en de
`Geldigheid`-rij (WF-101). Een zin die aan de `Geldigheid`-rij kan
worden toegevoegd: "Producentenkant (2026-09-22, kaart
`frontend-datumranden`): de RegelSpraak-frontend weigert een
versiegeldigheidsgrens met een tijdcomponent met een benoemde ERROR,
want een datumgranulair venster heeft geen drager voor dat tijdstip.
Een kale datumliteral buiten de kalender krijgt dezelfde weigering als
de datum-tijd-tak. Emittergetuigen in `IrEmitterTest`, gate 10."
