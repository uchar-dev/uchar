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
