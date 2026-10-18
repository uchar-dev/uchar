# rename-public-api: de namen die de distributie leest in het Engels, met de pakketten

*Stap 6 van het programma "Engelse namen" (Vervolg in `backlog/done/20261017-engelse-namen-afspraak.md`). Vereist
`hernoemgereedschap` en de lessen van `rename-frontend` en `rename-ocaml`; landt na die twee. Hoort bij de kaart
`engelse-namen` van `regelspraak-toolchain`, die dezelfde dag haar code laat volgen en de pin verzet. Raakt beschermde paden
(`scripts/`, `CLAUDE.md`, via de beslislijst `spec/`). Voorbereiding: `scripts/lib/naming/frozen/` en `glossary.tsv` (2026-10-18).*

Mandaat van de eigenaar (2026-10-18): de kaart landt vandaag, met een ff-merge op `main` in de lokale checkout; er wordt niet
gepusht. Dat mandaat dekt de beschermde paden die de kaart raakt: `scripts/` (het gereedschap, de bevroren lijsten, de woordenlijst,
`check-graaf.sh`, dat het pakket `graaf` noemt) en `CLAUDE.md` (de Conventions en gate 15, die de map noemen).

Goal: hernoem met `scripts/lib/naming/rename.py` de Nederlandse namen in `backends/drools` en `frontends/regelspraak` die tot nu toe
bevroren waren omdat de distributie `regelspraak-toolchain` ze leest: 283 namen (125 in `backends/drools`, 158 in de frontend) plus wat de
woordenlijst sindsdien zichtbaar maakte (382 namen in de inventaris na de woorden van 2026-10-18), en de pakketten `dev.uchar.regelspraak.graaf`,
`dev.uchar.regelspraak.kern` (frontend) en `dev.uchar.regelspraak.transpiler.kern` met hun mappen. Er verandert geen gedrag, geen melding en niets
op de draad. De distributie volgt in haar eigen kaart; de pin gaat pas naar uchar nadat deze kaart op `main` staat.

Blijft staan:
- wat `toka/` leest (40 namen van de frontend, 2 van de backend: `frozen/*.tsv`, categorie `toka`) en de v0-spellingen die de frontend zelf
  vastlegt (26, categorie `spelling`);
- wat de distributie buiten de code zichtbaar maakt: enumconstanten die zij als JSON, OpenAPI, CLI-uitvoer of uitlegproza schrijft
  (`ATTRIBUUT`, `INVOER`, `NIET_GERAAKT`, `ELFPROEF`, `ENUMERATIEWAARDE`, `ROL`, `VORM`; categorie `visible`), en `GEVUURD`
  (zichtbaar, maar onbekend voor de woordenlijst);
- de RegelSpraak-taal: sleutelwoorden, hun tokens, bronnen, domeininhoud, en de parserregels en labels van de grammatica met wat
  ANTLR eruit maakt (406 rijen in `frozen/regelspraak.tsv`, categorie `grammar`; besluit eigenaar 2026-10-18);
- wat de productieruntime leest (`Namen`, `Lidnamen`, `lidnamen`, `getTot`, `getVan`, `getWaarde`), behalve de importregels voor het
  pakket `kern` (zie werkwijze 4);
- meldingen, sleutels en waarden op de draad, v0-spellingen in weigertests, RegelSpraak-tekst, corpuspaden en slugs.

Werkwijze (eigenaar, 2026-10-18): kleine Haiku-sessies voor het bulkwerk, kleine Sonnet-sessies voor de goede woorden, een bouwer voor
samenvoegen, toepassen, compileren en testen; geen lensronde. De voorbereiding maakte 2 partijen met `propose` (382 namen, geen testzinnen); maak ze opnieuw als de woordenlijst verandert.
1. `merge` en `check` op de zone `backends/drools frontends/regelspraak`, tabel `scripts/lib/naming/tables/public-api.tsv`.
2. `apply --files-only`, commit; `apply` met beslislijst (`triage`, Haiku, `merge-decisions`), commit; hash in `.git-blame-ignore-revs`.
3. De 7 resten van `backends/drools` (`ContractValidator`, `VRIJGESTELD`, `vrijgesteld`, `notVrijgesteld`, `ariteit`, `frase`, `toon`) krijgen
   in de partij `keep` of een vertaling; `ContractValidator` is Engels.
4. De pakketten `kern` staan niet in de partijen: `kern` is in het woordenboek Engels, en de runtime leest `transpiler.kern.Namen`, dus `check` en
   `apply` weigeren de rij. Eigen commit: `git mv` van `frontends/regelspraak/src/{main,test}/java/dev/uchar/regelspraak/kern` en
   `backends/drools/src/{main,test}/java/dev/uchar/regelspraak/transpiler/kern` naar `core`, en één vervanging van `regelspraak.kern` en
   `transpiler.kern` in de imports en de javadoc, ook in `runtime/Accessors.java` en `runtime/RegelSpraakRuntime.java` (leesplekken van de runtime,
   geen van haar namen). Het pakket `graaf` gaat via de tabel (`graph`), met `scripts/check-graaf.sh` (twee regels) en `CLAUDE.md` gate 15.
5. Daarna `freeze` niet blind opnieuw: de lijsten zijn nu klein en dragen hun reden in de kop. Een nieuwe `freeze` over de distributie op haar
   nieuwe main hoort bij haar kaart, niet bij deze.

Acceptance:
- (a) `inventory backends/drools frontends/regelspraak` meldt alleen bevroren namen.
- (b) `check` geeft 0 fouten; `verify` slaagt op de boom.
- (c) De code compileert; de suites van frontend en backend zijn groen met hetzelfde aantal tests als voor de kaart; alle 16 gates groen.
- (d) Gate 15 (`check-graaf.sh`) draait met de nieuwe pakketnaam en haar floors ongewijzigd; `check-drools.sh` rapporteert dezelfde `EXPECTED_PLAYED`.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

Result (2026-10-18):
- Commits (op `card/rename-public-api`, na de voorbereiding `fa2c88d2` met de bestanden van de eerste tabel, alleen `git mv`, 31):
  gereedschap met fixture `ce195d56` (een tweede declarator heeft het type van de eerste) en `615c5f06` (het element van een `List`, `Optional`
  of `Map` heeft het type argument); de tabel, de bevroren lijsten en de beslislijst met beslissingen `e56714a1`, `9fd2ba18`, `1c1df062`;
  de inhoud `e0fced3f`; `kern` en `graaf` (`ceb9aab3`, achteraf `778bb7b2` voor de package- en importregels van `graaf`); de tweede tabel
  `public-api-visible` (`86028790`..`ffd4f9ae`, met twee tooltweaks met fixture) en haar inhoud `b092a30a`; `ee52377f`
  (`.git-blame-ignore-revs`: `e0fced3f`, `ceb9aab3`, `b092a30a`, `778bb7b2`, en de bevroren namen in de lijst van `backends/drools`).
- Tabel `public-api.tsv`: 349 rijen (271 translation, 78 improved), 55 daarvan met een bestand als scope (het Engelse woord staat al in het
  bestand); van de 382 namen van de partijen bleven er 71 bevroren (grammatica, toka, v0-spelling) of vielen er af. Beslislijst 17.146 plekken:
  15.365 keep, 1.781 change, waarvan 1.219 correcties (strings die de naam volgen) uit regels, zonder Haiku-ronde. Inhoud: 26.780 identifiers,
  829 in commentaar, 448 bestanden; `.md`-bestanden alleen waar de oude naam tussen backticks stond (CLAUDE.md, spec/PLAN.md, de README's van
  `backends/drools` en `frontends/regelspraak`, niet `DIVERGENCES.md`).
- Tweede tabel `public-api-visible.tsv` (besluit eigenaar 20:45: de zichtbare API Engels, behalve wat uit RegelSpraak komt): 16 rijen;
  `ScenarioResult.CoverageKind` (`GEVUURD` is `FIRED`, `NIET_GERAAKT` is `NOT_FIRED`: de doc van de enum zegt "whether a lowered construct fired")
  en `RuleGraph.NodeKind` (`REGEL` is `RULE`, `ATTRIBUUT` is `ATTRIBUTE`, `KENMERK` is `CHARACTERISTIC`, `INVOER` is `INPUT`); 43 identifiers,
  6 in commentaar, 13 beslissingen, 9 bestanden. `NIET_GERAAKT`, `ATTRIBUUT` en `INVOER` gingen uit `frozen/drools.tsv`. Blijven Nederlands:
  `Benoembaar.Soort`, `PredicateKind`, `ResolvedRegel.Soort`.
- Gereedschap (fixtures in `test_rename.py`, nu 100 tests): een tweede declarator (`A x = a, y = b`) heeft het type van de eerste; het element
  van `List<T>.get`, `Optional`, `Map<K,V>.get` heeft het type argument (anders nam een gelezen eigenschap de rij van het bestand, niet die van
  de declaratie); `merge` houdt een antwoord voor één scope met een reden ook als de naam geen Nederlandse naam is (een token, een domeinwoord:
  `GEVUURD`, `REGEL`, `KENMERK`); `check` telt de strings en commentaren van een bestand als plek voor een rij met dat bestand als scope.
  `validator` is technische term (`ContractValidator` is Engels en valt uit de tabel).
- Bevroren erbij: 6 v0-waarden van `spec/proposals/v1-naamtabel.json` (`EERDER_OF_GELIJK`, `GEEN`, `GROTER_OF_GELIJK`, `KLEINER_OF_GELIJK`,
  `LATER_OF_GELIJK`, `PERCENTAGE_VAN`) en `ZELFSTANDIG` (`SpellingTest` leest ze met `name()`); `KENMERKTOEKENNING`, `BESLISTABELRIJ`,
  `OPGAVE_BOVEN_BEREKENING` en in `backends/drools` `CONSISTENTIE`, `DEFAULTSGROEP`, `UNICITEIT`, `VERDELING`, `FEITCREATIE`, `OBJECTCREATIE`
  (hun spelling staat in meldingen en dumps: `name().toLowerCase()`); in de lijst van `backends/drools` ook de namen die de frontend bij naam
  bevriest (`kenmerken`, `ROL`, `regel`, `onderwerp`, `initialisatie`, `GEEN`, `VORM`).
- Gemeten: javac van frontend (main, test), drools (main, test, `src/source-route`) en toka zonder fouten; frontend JUnit 1.199 tests groen;
  backend `mvn -o test` 1.619 tests, 0 fouten, 0 overgeslagen. `verify` PASS op de inhoudscommit `e0fced3f` met `--base 7d3877c6` (tabel
  en lijst van de eerste tabel) en PASS voor de tweede tabel (`--base ffd4f9ae`), beide "incomplete: no landing proof" (weergaven en gate-logs
  niet gevraagd). `check`: 0 fouten voor beide tabellen. `inventory frontends/regelspraak`: 0 namen; `backends/drools`: 2 (`ATTRIBUUT`,
  `INVOER`, zie open punt 1).
- Gates: alle 16 groen op `ee52377f` (`gates.sh`, failed=0, 2026-10-18 21:52 tot 22:13); gate 12: JUnit 1.619, played 255 (gelijk aan
  `EXPECTED_PLAYED`), DRL-tekst 255; gate 15 met de nieuwe pakketnaam (`graph`) en haar floors ongewijzigd: 70 unit tests, 125 bronroutes;
  gate 10: 1.199 tests, 125 goldens byte-identiek; gate 11: 29 gespeeld. Een eerdere run (op `b092a30a`) was rood op gate 15, omdat de
  `package`-regels van `graaf` nog `graaf` zeiden (zie open punt 3); hersteld in `778bb7b2`, gate 15 los groen, daarna de volledige run.
- Open punten:
  1. `inventory backends/drools` meldt `ATTRIBUUT` en `INVOER`: de constanten van `ReadKind` (dumps van de plannen in de tests) en van
     `FactModelGenerator.Soort`. Ze zijn niet bevroren, omdat de lijst op naam werkt en `RuleGraph.NodeKind` ze hernoemt. Acceptance (a) is
     daarmee op die twee na gehaald.
  2. Zichtbaar en Nederlands, blijft: `Benoembaar.Soort` (`ATTRIBUUT`, `ENUMERATIEWAARDE`, `ROL`, `VORM` in `frontends/regelspraak`), de
     constanten van `PredicateKind` (`ELFPROEF`, `NUMERIEK_MET_EXACT`, `DAGSOORT`), `ResolvedRegel.Soort`, en de namen die `toka/` leest
     (40, categorie `toka`).
  3. Handwerk, na `verify`: de importregels van de runtime (`Accessors`, `RegelSpraakRuntime`) en alle `package`- en importregels van
     `kern` (`ceb9aab3`) en `graaf` (`778bb7b2`); de twee regels van `check-graaf.sh` en gate 15 in `CLAUDE.md`. Het gereedschap hernoemde de
     map `graaf` maar niet de pakketnaam in de bestanden.
  4. De 55 bestandsrijen zijn automatisch gemaakt (naamsbotsing met een bestaand Engels woord) en niet nagelopen op de beste naam; de
     1.219 correcties volgen uit regels (`toString` van records in verwachtingen, eigenschapsnamen van de feiten in de DRL-tekst,
     statische hulpmethoden die de DRL-tekst aanroept, planlabels in de dumps), niet uit een Haiku-ronde. De beslissingen over de
     documentatie (`.md`) volgen de regel van de hoofdsessie: `change` alleen waar de oude naam tussen backticks staat.
  5. Het commitbericht van `e0fced3f` noemt 1.688 beslissingen en 1.126 correcties; de gemeten aantallen zijn 1.781 en 1.219. De
     boom is die van `verify`.
  6. `W/build-uchar/MAPPING.tsv` (1.143 rijen: type en lid, met pakket en oud en nieuw bestand; ook de pakketten `kern` en `graaf` en de
     constanten van de tweede tabel) is de invoer voor `javac_follow.py` van de distributie.
