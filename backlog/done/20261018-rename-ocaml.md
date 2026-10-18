# rename-ocaml: de OCaml-zones in het Engels, met de ASP-predicaten en de gatescripts

*Stap 5 van het programma "Engelse namen" (Vervolg in `backlog/done/20261017-engelse-namen-afspraak.md`). Vereist
`hernoemgereedschap` en de lessen van `hernoem-drools-backend`, `drools-word-fixes` en `rename-frontend`; landt na
`rename-frontend`. Tellingen op `6c41c6df`.*

Mandaat van de eigenaar (2026-10-18): de kaart landt vandaag, met een ff-merge op `main` in de lokale checkout; er
wordt niet gepusht. Dat mandaat dekt de beschermde paden die de kaart raakt: `spec/verifier/` (de gedeelde AST),
`spec/ASP-TRANSLATION.md` (de predicaatnamen), `scripts/` (de shellvariabelen en de vlag van de gatescripts) en
`CLAUDE.md` (de gate-tekst die die namen noemt).

Goal: hernoem met `scripts/lib/naming/rename.py` de Nederlandse namen in de OCaml-zones `ref`, `spec/verifier`,
`backends/clingo`, `frontends/catala` en `frontends/flogic`, en in de shellscripts `scripts/*.sh`: ±1.300 namen vóór de
woorden van deze kaart. De vijf OCaml-zones delen de AST `spec/verifier/lib/ast.ml` (`Ir_v0.Ast`); een constructor of
veld ervan krijgt de naam van de v1-draad (`spec/proposals/v1-naamtabel.json`, `spec/schema/uchar-ir-v1.schema.json`),
ook in `frontends/flogic`, dat zelf al Engels is sinds `hernoemgereedschap` maar de AST leest. Verder horen bij de
kaart de ASP-predicaten (in stringliteralen van `backends/clingo` en in `spec/ASP-TRANSLATION.md`, waar `rename.py` ze
niet ziet), de shellvariabelen van de gatescripts met de tekst in `CLAUDE.md` die ze noemt, en de vlag
`ref --bodemzin`. Er verandert geen gedrag, geen melding en niets op de draad.

Blijft staan:
- sleutels en waarden op de draad, en v0-spellingen in weigertests (`corpus/negatief`, `corpus/rood`);
- meldingen: elke zin die een gebruiker of gate te zien krijgt, ook de zinnen die `expect.txt` en gate 8 pinnen
  (`expectedFault`, de bodemzinnen van `scripts/check-rood.sh`);
- RegelSpraak-tekst, domeininhoud (de Jutterswet, gedeclareerde namen, caselabels) en de Catala-bronnen van
  `frontends/catala/test/`;
- corpuspaden en slugs, ook de scriptnamen `scripts/check-rood.sh` en `scripts/check-graaf.sh` (stap 8), en de
  `toka/`-zone;
- de namen die een bibliotheek declareert en de zone alleen leest (`` `En `` van Catala).

Bevroren lijst: de meting (2026-10-18) vond geen OCaml-naam die code buiten deze zones leest, dus er is geen
`scripts/lib/naming/frozen/ocaml.tsv`. De distributie leest van deze zones alleen `EXPECTED_PLAYED` en
`EXPECTED_CHANNEL` in `scripts/check-drools.sh`, en die zijn al Engels.

Werkwijze (eigenaar, 2026-10-18): kleine Haiku-sessies voor het bulkwerk, kleine Sonnet-sessies voor de goede woorden,
een bouwer voor samenvoegen, toepassen, compileren en testen; geen lensronde. Kan `rename.py` iets niet dragen (de
ASP-predicaten in strings, de AST-namen van de draad), dan krijgt het daarvoor een optie, met een fixture in
`test_rename.py` (eerst rood, dan groen).

Acceptance:
- (a) `inventory ref spec/verifier backends/clingo frontends/catala frontends/flogic scripts` meldt geen Nederlandse
  naam meer, behalve de scriptnamen `check-graaf` en `check-rood` (stap 8) en `` `En` `` van Catala.
- (b) `check` geeft 0 fouten; `verify` slaagt op de boom en op de weergaven (de ASP-programma's van `render`).
- (c) Alles bouwt met `dune build`; de suites van `ref`, `spec/verifier`, `backends/clingo`, `frontends/catala` en
  `frontends/flogic` draaien met minstens hetzelfde aantal checks; alle 16 gates groen.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

Result (2026-10-18):
- Commits (op main `436240cc`): `2e37f372` (tabel: 1.299 rijen, 991 translation en 308 improved, `check` 0 fouten), `4611166e`
  (alleen `git mv`: 5 Catala-testbestanden), `bfadb6c7` en `57d72aa4` (beslislijst, 13.699 plekken: 27 `change`, rest `keep`),
  `3fc9e1c4` (inhoud: `apply --decisions`, 22.181 identifiers, 150 in commentaar, 42 bestanden, 183 plekken buiten de zone met
  rust gelaten), `a6184ed8` (stap 3: ASP en vlag), `32ea89b3` en `6e9c110e` (herstel na de eerste gate-run), daarna
  `.git-blame-ignore-revs`.
- Beslissingen: `triage --default-keep` besliste 9.909 rijen; de 3.790 overige beslist een script zonder Haiku (regel van de
  hoofdsessie): `change` alleen in `CLAUDE.md`, `spec/PLAN.md`, `scripts/` en `.md` onder de zones, waar de oude naam de
  hele backtickspan is (11 rijen: de `EXPECTED_MIN_*`-vlaggen en `VERSCHIL_BEWUST` in `CLAUDE.md`); `keep` voor de rest. Tien
  plekken in `frontends/catala/test/suite.ml` die de vijf hernoemde Catala-bestanden noemen kregen via `--corrections` `change`.
- `verify` PASS op `3fc9e1c4` (incomplete: geen weergaven, geen gate-logs).
- Stap 3 (`a6184ed8`, handwerk, alleen in stringliteralen van `backends/clingo`: `asp_rename.py` in de werkmap): `heeft` -> `has`,
  `rol` -> `role`, `inputrol` -> `inputrole`, `wschrijft` -> `wwrites`, `wgevuld` -> `wfilled`, `wvuurt` -> `wfires`,
  `leeg` (ASP-term) -> `empty`, `waar`/`onwaar` (ASP-symbolen; de RegelSpraak-invoer `"waar"` blijft) -> `true`/`false`,
  `stand%d_eff` -> `state%d_eff`, `unie%d` -> `union%d`, `%s_tot` -> `%s_total`, `%sgewicht` -> `%sweight`,
  `%sheeft_*` -> `%shas_*`; ook in `spec/ASP-TRANSLATION.md`, `backends/clingo/README.md` en `spec/PLAN.md`. De vlag is
  `ref --bottom-sentence` (runner en `check-rood.sh`). Meldingen en commentaar met `leeg` blijven.
- Handwerk dat het gereedschap miste (in `a6184ed8` en `32ea89b3`): een identifier na `::` (`x :: pad`, `p :: venster_overlap`,
  zes meer) en leden die de tests via een module lezen (`Arith.agg_som`, `Timeline.totaal`, `World.links`, `Value.Lijst`, ±100
  plekken in `test_ref.ml` en `test_clingo.ml`); awk-variabelen en de `trap` in de gatescripts; `bron.rgs`, `bron.catala_en`
  en `bron.flogic` (corpusbestanden) die het gereedschap als `bron` hernoemde en die terug zijn gezet; de bestandsnamen
  `stripped.json`, `swapped.json` en `rotated.json` van de vocabulairetak van gate 13; `FoldKindsBoundaryTest.java` (frontend)
  kreeg een kaart van de nieuwe verifier-constructors (`Sum_function`, `Count`, ...) naar de RegelSpraak-soorten; de naald
  `"unie"` in `test_clingo.ml` werd `"union"`.
- Gemeten: `dune build` zonder fouten; alle 16 gates groen op `32ea89b3` behalve gate 13 (vocabulairetak), die na `6e9c110e`
  los groen is (floors gehaald); gate 14: 150 checks. De volledige run is dus niet nogmaals over de eindstand gedraaid.
- Open punten:
  1. `inventory` meldt nog 55 namen: de 47 Catala-bestandsnamen (besluit 2, stap 8; 42 `cases/*.json` en `.catala_en`, plus
     de 5 hernoemde die al weg zijn) en `` `En ``.
  2. De OCaml-testlabels (strings) blijven Nederlands (besluit 1); het gereedschap kent ze niet.
  3. `spec/proposals/` noemt nog `EXPECTED_MIN_NEGATIEF` en `VERSCHIL_BEWUST` (voorstellen blijven); `corpus/negatief/README.md`
     volgt sinds `233487e4` (vijf plekken, de hoofdsessie).
  4. Het gereedschap mist een identifier na `::`, een lid via een module in een testmap en een bestandsnaam als `bron.rgs`
     in een shellscript; een fixture is niet geschreven.
- Landing: volledige gateset op `d009f87a` (de eindstand, met `233487e4`): 16/16 groen, 2026-10-18 18:02–18:20 (hoofdsessie).
