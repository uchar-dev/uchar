# rename-paths: de paden van corpus, bronbestanden en gatescripts in het Engels

*Stap 8 van het programma "Engelse namen" (Vervolg punt 8 in `backlog/done/20261017-engelse-namen-afspraak.md`).
Vereist kaart 6 (`message-catalogues`) op `main`. Raakt `scripts/`, `CLAUDE.md`, `spec/` en het corpus: de branch
blijft een voorstel tot de eigenaar merget. Voorbereiding en meting: `werk/engelse-namen/stap8/` (`MEASURE.md`,
`PLAN.md`).*

Goal: hernoem de paden waarvan de oorsprong niet in RegelSpraak ligt, en de verwijzingen ernaar. Er verandert geen
gedrag, geen melding en niets op de draad.

- `corpus/negatief` wordt `corpus/negative`, `corpus/rood` wordt `corpus/red`.
- `bron.rgs`, `bron.catala_en` en `bron.flogic` worden `source.rgs`, `source.catala_en` en `source.flogic`.
- `scripts/check-rood.sh` wordt `scripts/check-red.sh`, `scripts/check-graaf.sh` wordt `scripts/check-graph.sh`.
- De 42 bestandsnamen van de Catala-testcases (besluit 2 van `rename-ocaml`) worden Engels.
- Case-slugs met een Nederlands technisch woord worden Engels (`wf044-onopgelost` wordt `wf044-unresolved`); de lijst
  en de voorstellen staan in `MEASURE.md`.
- Elke verwijzing in getrackte bestanden volgt: scripts, OCaml, Java, tests, spec, README's, `CLAUDE.md`,
  `WAY-OF-WORKING.md`, `expect.txt`, `case*.json` en de manifesten.

Blijft staan: wat zijn oorsprong in RegelSpraak heeft; domeininhoud en slugs uit de Jutterswet of uit een
RegelSpraak-bron (`jutterswet-*`); `scenario.rgs` en `ir.json` (al Engels); de `toka/`-zone; `backlog/done/`,
git-historie en `spec/proposals/` (voorstellen blijven). De verwijzingen in de distributie `regelspraak-toolchain`
volgen in een eigen kaart daar, samen met haar pin-bump en haar standaardlocale `nl`.

Mandaat van de eigenaar (2026-10-18): stap 8 volgt na kaart 6; lokaal; ff-merge door de hoofdsessie; niet pushen.
De kaart raakt `scripts/`, `CLAUDE.md`, `spec/` en het corpus, dus de branch blijft een voorstel tot de eigenaar
merget. De kaart raakt ook testbestanden (Java, OCaml, `frontends/catala/test/`) en hernoemt corpusmappen; het
mandaat van de eigenaar dekt dat, omdat de verwijzingen anders breken.

Werkwijze: eerst `git mv` van mappen en bestanden in één commit, dan de verwijzingen met een script in één commit,
dan de gates (zie `PLAN.md`).

Acceptance:
- (a) Na de kaart staat geen enkel oud pad nog in getrackte bestanden, behalve in `backlog/done/`, de git-historie,
  `spec/proposals/`, de `toka/`-zone en deze kaart zelf.
- (b) Gate 3 leest de nieuwe mapnamen `corpus/negative/<wf###-slug>`; alle JSON-pointers en paden in `expect.txt`,
  `case*.json` en de manifesten kloppen; de tellingvloeren van de gates zijn gelijk of hoger.
- (c) Alle 16 gates groen op de eindstand van de branch.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result

Gebouwd op `main` `21e820a30` (kaart 6 staat erop). Commits: `830cb7e74` (A, 1.697 bestanden alleen `git mv`, 0 regels
inhoud), `86f325c73` (B, `refs.py`: 546 bestanden; 449 pad-, 328 bron-, 59 script-, 1.640 slug-, 55 Catala-, 15
constante- en 476 prozavervangingen), `dd5fa6511` (C, 111 regels met de hand), en een laatste commit met deze Result en
de drie hashes in `.git-blame-ignore-revs`.

Aantallen: `corpus/negative` 511 mappen (+ README), `corpus/red` 45, 140 `source.*`-bestanden, 2 scripts, 49 Catala-bestanden
verhuisd. Slugs hernoemd: 389 van de 438 kandidaten (363 negative, 25 red, 1 golden: `near-side-role-check-to-many`);
Haiku hield er 49 (`keep`). `jutterswet-*`, `synth*` en `ww-*` blijven (besluit 1). Gate 3 telt nog 511 negatieven,
354 op hun arm, 619 pointers; alle vloeren onveranderd (geen verlaagd, geen verhoogd).

Haiku-antwoorden: `merge-answers.py` meldde geen fout, geen botsing en geen dubbele naam; geen tweede ronde. De 18
beoordelingsregels: de 9 `keep-word`'s blijven volgens besluit 4 (`wrat`, `wratvouw`, `wratoperand`, `aanvoergeld`,
`jutterswet`, `de-dag`, `van`/`tot`); de 9 `change`'s zijn per regel gelezen en goed bevonden (`niet` als `not`,
`een` als `one`, `regel` als `rule`). Geen sleutel voor twee sjablonen: geen meldingssjabloon is geraakt.

Testbestanden (mechanisch, geen verwachting veranderd): 98 bestanden onder `*/src/test`, `frontends/catala/test`,
`ref/test` en `backends/clingo/test`, 213 regels in, 213 uit: padstrings, slugs, de constante `NEGATIEF` naar
`NEGATIVE` (6 Java-bestanden), Catala-bestandsnamen in `suite.ml`, en 17 prozaregels in tests met "negatief"/"rood" als
zonenaam. Java: de suites van gate 10 en 12 draaien (1.622 tests in gate 12, vloer 1.573, geen skip).

Handwerk (C): `scripts/check-red.sh` (twee regexen en `zone == "red"`, een commentaarvorm), een melding-commentaar in
`check-verifier.sh`, en het zonewoord in 111 proza- en commentaarregels van spec, `CLAUDE.md`, NOTE's en tests. Bewust
gelaten: `RegelSpraakRuntime.java` (`boolean negatief`), grammatica en `frontends/flogic` (taalwoorden), het enumlabel
`rood` in `wf006-duplicate-enum-value/ir.json`, `rood-eerst` (Nederlands spraakgebruik), Nederlandse proza van de kaarten
in `backlog/new/`, `scripts/lib/naming/` (besluit 2, uitzondering in acceptance (a)) en Nederlandse regels over een
negatief getal. Acceptance (a): `git ls-files` en `git grep` op de oude paden zijn leeg buiten `backlog/done/`,
`spec/proposals/`, `toka/`, `scripts/lib/naming/` en deze kaart.

Gates: alle 16 groen op `dd5fa6511` in een volledige run (`werk/engelse-namen/gates.sh`, nu met `check-red.sh` en
`check-graph.sh`; 20 minuten): 5 (825 cases), 6 (2.198 checks), 7 (45 overlevers), 8 (50 red cases), 9 (241 checks, 12
goldens), 12 (1.622 tests), 14 (150 checks), 16 (102 casebestanden), alles gelijk aan de stand ervoor. De laatste commit raakt
alleen `.git-blame-ignore-revs` en deze kaart; daarna is gate 1 opnieuw gedraaid.

Nieuwe toets in een gate: geen, dus geen mutatie en geen nieuwe vloer; geen voorstel voor `CLAUDE.md` behalve dat de
hoofdsessie bij het mergen controleert dat de gatebeschrijvingen de nieuwe namen dragen (al gedaan door `refs.py`).

Open: `scripts/lib/naming/` en de verwijzing in Conventions blijven tot na de syncronde (besluit 2); de distributie volgt
in haar eigen kaart (PLAN punt 6); de alfabetische volgorde van lijsten in gatescripts en `corpus/negative/README.md` is
na het hernoemen niet meer op volgorde (geen gate eist het).
