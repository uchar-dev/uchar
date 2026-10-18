# drools-word-fixes: de verkeerde woorden van de Drools-backend recht

*Vervolg op `backlog/done/20261017-hernoem-drools-backend.md` (Open 1–3). Raakt beschermde paden (`scripts/`, en via
de beslislijst `spec/` en `CLAUDE.md`): de branch blijft een voorstel tot de eigenaar merget.*

Goal: de namen van `backends/drools` die de lens van stap 3 met het verkeerde woord vond, krijgen het juiste:
`terugzetten` is write back, `fout` in transpiler en TMS is fault, `reikwijdte` is reach (ch. 3.8), `dubbel` is double
waar twee verschillende dingen bedoeld zijn, `voor` is before in tijd of volgorde, `lopen` met een lijdend voorwerp
is walk, `dubbele punt` is colon, `haalt` met een drempel is reaches, `benoemd geweigerd` is refused by name,
`tijdvrij` is time-free, `Functie` als statische functie is `Function`, en `opslag` is storage; de 18 `improved`-rijen
met scope `*` krijgen per declaratie een passende naam; de testzinnen die een andere bewering maken dan de
Nederlandse, worden recht gezet. Er verandert geen gedrag.

Werkwijze (eigenaar, 2026-10-18): kleine Sonnet-sessies kiezen de woorden, kleine Haiku-sessies doen het bulkwerk, één
bouwer voegt samen (het recept van de tabel van stap 3) en past toe; geen lensronde.

Acceptance:
- (a) De antwoorden van de Sonnet-sessies staan in de tabel `scripts/lib/naming/tables/drools.tsv`; `check` geeft 0
  fouten; de woordenlijst heeft per woord hierboven een rij met `context`, zodat stap 4 het goed krijgt.
- (b) De code compileert; de suite van de backend is groen met 1619 tests.
- (c) Alle 16 gates groen op de eindstand van de branch.

May touch: als `hernoem-drools-backend`.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (2026-10-18)

Een tweede tabel, `scripts/lib/naming/tables/drools-fixes.tsv`, staat bovenop main `1bd57532`. Ze heeft 395 rijen,
alle met `kind` `improved`; `old` is de Engelse naam van stap 3 en `new` de juiste. `drools.tsv` blijft het verslag
van stap 3. De woorden kozen tien Sonnet-sessies op 470 rijen (191 `fix`, 32 `split`, 247 `ok`); twee
Sonnet-bouwers voegden samen en pasten toe. Er was geen lensronde.

- **(a)** Overgenomen zijn 211 antwoorden plus 8 zusterrijen. Niet overgenomen zijn er 14: 12 vallen onder het
  domeinbesluit (zie Open 2), en twee kan het gereedschap niet scheiden: `otherwise` → `changed` in
  `TmsTranspiler#withoutDepositFlag` en `check` → `control` in `FilterErrorWitnessTest`. `check` geeft 0 fouten. De
  woordenlijst kreeg ±40 rijen met `context` voor de woorden van de kaart.
- **Inhoud:**
  - `cf57789e` hernoemt 1.676 identifiers, en `4c73201b` 9 bestanden.
  - De beslislijst `403b068c` heeft 316 plekken, waarvan 55 `change`. Daarvan zijn er 41 correcties uit de suite:
    strings die een hernoemde naam noemen.
  - Buiten de zone veranderen `spec/CONSTRUCTS.md` (2), `spec/IR-SPEC.md` (1) en
    `corpus/golden/jutterswet-filterwacht/NOTE.md` (1), telkens een hernoemde testklasse.
- **(b)** De code compileert (main, test, `src/source-route`), en de suite is groen met 1619 tests. `verify` (tweede
  tabel, basis `1bd57532`) geeft PASS op de boom.
- **(c)** 16/16 gates op `d5f1dcaf`.
- **Gereedschap**, elk met een fixture:
  - `java_type_file` koos bij twee typen met dezelfde naam in één pakket op setvolgorde, waardoor `check` in 4 van 10
    runs faalde. Nu wint het type op het hoogste niveau.
  - Twee aangrenzende methodescopes gaven ten onrechte "equal reach".
- **Afwijking:** een bouwer gebruikte twee keer `rm -f` op een pad met een variabele, op eigen `.bak`-bestanden. Er
  ging niets verloren.

Open:
1. **`Functie`.** De plan-enum (een statische functie die een term aanroept) heet nog `Aggregation`, net als het
   geneste `Accumulatie.Functie`. Het gereedschap scheidt een genest type dat via een punt gelezen wordt niet van een
   gelijknamig type in hetzelfde bestand: 33 compileerfouten. Ook `eisHerschaalbaar` bleef `requireRescalable`, omdat
   de spec "rescalable" zegt.
2. **Domeinwoorden.** Dit is Open 4 van `hernoem-drools-backend`. Niet overgenomen zijn `veilig`,
   `…voor_de_veilige_haven`, `gemeldeDag`, `dubbelop`, `heffingsdeler`, `takopslaggrond`, `metMassa`, `metVondstmassa`,
   `grondslag`, `sluitdagen`, `sluitdaggroep` en `lichtklasse`.
3. **Testzinnen.** Ze zijn niet volledig nagelezen; alleen de patronen en de vondsten van de lens zijn hersteld.
