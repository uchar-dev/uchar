# drools-static-function: de enum van statische functies heet `StaticFunction`

*Vervolg op `backlog/done/20261018-drools-word-fixes.md` (Open 1). Raakt `scripts/`: de branch blijft een voorstel tot
de eigenaar merget.*

Goal: de plan-enum `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/plan/Aggregation.java` (v0
`Functie`: de statische functies die een term aanroept) heet `StaticFunction`; het geneste `Accumulation.Aggregation`
(de aggregatiefuncties) blijft `Aggregation`. Daarvoor leert `rename.py` dat een genest type dat via zijn buitentype
gelezen wordt (`Accumulation.Aggregation`), en een lezing binnen dat buitentype, de rij van de eigen declaratie volgt
en niet die van een gelijknamig type op het hoogste niveau. Er verandert geen gedrag.

Acceptance:
- (a) Een fixture in `test_rename.py` voor de nieuwe regel, eerst rood tegen de oude stand, dan groen.
- (b) Een tabel `scripts/lib/naming/tables/drools-function.tsv`; `check` geeft 0 fouten.
- (c) De code compileert; de suite van de backend is groen met 1619 tests; alle 16 gates groen.

May touch: `scripts/lib/naming/`, `backends/drools/` behalve `src/main/java/dev/uchar/regelspraak/runtime/`, de
bestanden die de beslislijst noemt, `.git-blame-ignore-revs`, deze kaart.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (2026-10-18)

De plan-enum heet `StaticFunction`, en het bestand `plan/Aggregation.java` heet `plan/StaticFunction.java`. Het
geneste `Accumulation.Aggregation` blijft, met zijn 32 lezingen. Het werk deed één Sonnet-bouwer, zonder lensronde.

- **(a)** `rename.py` kreeg één regel erbij: een genest type dat als `Outer.Inner` gelezen wordt, of met zijn korte
  naam binnen `Outer`, volgt de rij van zijn eigen declaratie. Heeft die declaratie geen rij, dan blijft de naam
  staan. Tot nu toe viel zo'n lezing terug op de rij van de plek. Er zijn 3 fixtures, eerst rood tegen `efa94dad`;
  `test_rename.py` geeft 90 tests OK.
- **(b)** De tabel `scripts/lib/naming/tables/drools-function.tsv` heeft 10 rijen, één per bestand dat de enum leest;
  `check` geeft 0 fouten. Hernoemd zijn 184 plekken, 162 identifiers en 22 plekken in commentaar (`32e55bb7`). De
  beslislijst telt 685 plekken, alle `keep`: het is de draadknoop `Aggregation`.
- **(c)** De code compileert (main, test, `src/source-route`), en de suite is groen met 1619 tests. `verify` (basis
  `08e7b470`) geeft PASS op de boom, en de 16 gates zijn groen op `f6af047b`.

Open: in `ComputeHelpers.java:28` (`{@code plan.Aggregation}`) en `plan/Route.java:6` (`{@link Aggregation}`) heet de
enum nog `Aggregation`. `check` weigert een rij voor een bestand zonder code-token, en een scope `*` of een map zou
het geneste type meenemen. Om dit te herstellen moet `check` een gekwalificeerde lezing in commentaar tellen.
