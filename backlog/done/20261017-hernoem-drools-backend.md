# hernoem-drools-backend: de Drools-backend in het Engels, behalve wat de distributie gebruikt

*Stap 3 van het programma "Engelse namen" (Vervolg in `backlog/done/20261017-engelse-namen-afspraak.md`). Vereist
`hernoemgereedschap` (`scripts/lib/naming/`). Raakt beschermde paden (`scripts/`, `spec/`, `CLAUDE.md`, alleen voor
verwijzingen naar hernoemde namen): de branch blijft een voorstel tot de eigenaar merget. Tellingen op `85dbbb1c`.*

Goal: hernoem met `scripts/lib/naming/rename.py` de Nederlandse namen in `backends/drools`: de hoofdcode, de tests,
de source set `src/bronroute` en de testresources. De inventaris telt daar ±5.150 Nederlandse namen, waarvan ±1.650
testnamen in zinsvorm (zoals `ZusterrolInvoerWitnessTest` en `een_lid_dat_het_pad_niet_bereikt_wordt_niet_gefilterd`).
Er verandert geen gedrag, geen melding en niets op de draad.

Blijft staan in deze kaart:
- de productieruntime onder `backends/drools/src/main/java/dev/uchar/regelspraak/runtime/` (vaste uitzondering van
  `CLAUDE.md`);
- de namen die de distributie gebruikt: elke Nederlandse naam van de zone die als identifier voorkomt in de eigen
  Java-bronnen van `/Users/yeb/Developer/uchar-dev/regelspraak-toolchain` (alles buiten haar submodule `uchar/`),
  zoals `CaseInvoer`, `ScenarioResultaat`, `ProducentConsument` en de pakketten `graaf` en `transpiler.kern`. Die
  gaan in stap 6 mee, samen met de distributie. De kaart legt ze vast in `scripts/lib/naming/frozen/drools.tsv`
  (naam, het bestand van de distributie dat hem gebruikt);
- meldingen (een zin die een gebruiker of gate te zien krijgt; kaart `meldingscodes` en de catalogi van stap 7);
- sleutels en waarden op de draad, v0-spellingen in tests die ze weigeren, RegelSpraak-tekst, domeininhoud;
- corpuspaden en case-slugs (stap 8).

Werkwijze, met het model per taak (de verdeling van het programma; Copilot staat uit):
1. Inventaris van de zone; de bevroren lijst; ontbrekende woorden in de woordenlijst (bouwer, Opus). Kan
   `rename.py` een bevroren lijst niet uitsluiten, dan krijgt het daarvoor een optie, met een fixture in
   `test_rename.py` (eerst rood, dan groen).
2. `propose`; Haiku vertaalt de partijen (de hoofdsessie draait ze, in golven van ±10 agents, met
   `prompts/translate.md`).
3. `merge` en `check`. Een geweigerde regel gaat met de foutmelding van `check` terug naar Haiku (tweede ronde,
   hoofdsessie). Wat dan nog faalt en de gemarkeerde namen lost de bouwer op; een `improved`-naam krijgt een reden en
   volgt de voorkeursvolgorde van de kaart `hernoemgereedschap` (punt 3). Vraagt dat meer context dan één bouwer
   heeft, dan nemen opeenvolgende bouwers elk een bereik van partijen, met een overdracht.
4. `apply --files-only`, commit; `apply` tot de beslislijst er staat; Haiku stelt de beslissingen voor
   (`prompts/decide.md`, hoofdsessie); de bouwer loopt ze na op de regels die de proef opleverde (draad, v0-spelling,
   meldingen, corpuspaden, plekken buiten de zone).
5. `apply --decisions`, commit; de inhoudscommit in `.git-blame-ignore-revs`.
6. Bewijs: de volledige gateset vóór en na (hoofdsessie); `verify` met tabel, beslislijst, weergaven en gate-logs.
7. Lens: een verse Sonnet-agent leest op betekenis alle `improved`-regels, alle regels die `check` in de eerste ronde
   weigerde, een steekproef van 10% van de rest (vaste seed, in het Result), en elke `change` buiten de zone.

Acceptance:
- (a) `rename.py inventory backends/drools` meldt alleen nog namen van `frozen/drools.tsv` en de productieruntime.
- (b) `check` geeft 0 fouten op de tabel; `verify` slaagt op de boom, de weergaven en de gate-logs (basis: de commit
  vóór `--files-only`).
- (c) De verwijzingen naar hernoemde namen buiten de zone zijn bijgewerkt via de beslislijst: de testklassen die
  gate-scripts noemen (`check-drools.sh`, `check-graaf.sh` en andere), `CLAUDE.md` waar het een hernoemde testklasse
  noemt (gate 12), de spec en `spec/CONSTRUCTS.md`/`spec/PLAN.md` waar ze een hernoemde testklasse citeren (deze kaart
  mag die twee registers daarvoor raken, alleen mechanisch), `backends/drools/README.md`, NOTE's in het corpus.
- (d) Haiku gemeten zoals in `hernoemgereedschap` (c): partijen, weigeringen per soort in ronde 1 en 2, correcties
  van de bouwers; de lens met haar telling per soort regel.
- (e) Alle 16 gates groen op de eindstand van de branch, met gate 12 op dezelfde JUnit-telling, hetzelfde aantal
  played en dezelfde lijsten.

May touch: `backends/drools/` behalve `src/main/java/dev/uchar/regelspraak/runtime/`; `scripts/lib/naming/`;
`scripts/check-*.sh`, `CLAUDE.md`, `spec/*.md`, `frontends/regelspraak/grammar/DIVERGENCES.md`, `corpus/**/NOTE.md`
alleen voor verwijzingen naar hernoemde namen; `.git-blame-ignore-revs`; deze kaart.

Out of scope:
- Wat de distributie gebruikt (stap 6), meldingen (stap 7), corpuspaden en slugs (stap 8).
- De frontend (stap 4) en de OCaml-zones (stap 5).
- Code herstructureren, ook waar een betere naam erom vraagt.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (deel 1, 2026-10-17)

Eerste bouwer: werkwijze punt 1 en de voorbereiding van punt 2 (de partijen staan buiten de repo, in de werkmap van
de hoofdsessie). Commits: `f72b02bb` classifier, `3b0a5147` bevroren namen in het gereedschap, `0f7e8272` en
`4c528367` de bevroren lijst, `29110564` de woordenlijst. `python3 -I scripts/lib/naming/test_rename.py`: 35 tests,
OK; elke nieuwe fixture eerst rood tegen de oude stand en tegen een tijdelijke mutatie per onderdeel (21 mutaties,
gemeten in de werkmap, niets bewaard).

- **Classifier** (`f72b02bb`): de inventaris telde 23 enumconstanten van de frontend als namen van de zone:
  `case GROTER_OF_GELIJK, LATER_OF_GELIJK ->` (het tweede label gold ook als lambda-parameter) en
  `ResolvedRegel.Soort.KENMERKTOEKENNING` (de qualifier `Soort` vond de geneste `Uitkomst.Soort` in hetzelfde
  pakket). Een case-label leest nu de constante van de enum van de switch, een qualifierketen wordt opgelost vanaf
  het buitenste type of het pakket. Inventaris op `b566ade5`: 4756 namen, daarna 4733 (23 weg, 0 nieuw); 53 namen
  krijgen voorkomens terug die ten onrechte buiten telden (importregels, `Gevolg.Stapelfeit.Kenmerk`).
- **Bevroren namen** (`3b0a5147`): `scripts/lib/naming/frozen/<zone>.tsv`; de inventaris meldt ze apart onder
  `# frozen`, `propose` laat ze weg, `check` en `apply` weigeren een rij die er een hernoemt (README, Frozen
  names). `freeze` maakt de lijst: een naam van de zone telt als de distributie hem *leest* (import, type,
  qualifier, waarde met een geschreven type dat een zone-type kan zijn, statische import, supertype,
  case-label); een lokale of eigen klasse van de distributie met dezelfde spelling niet, een waarde zonder
  geschreven type wel (onbekend telt als lezing). Gemeten vóór de woordenlijst: de spelling alleen gaf 364 namen,
  de declaraties van de zone 300, het lezen 125 (`0f7e8272`); na de woordenlijst 129.
- **De lijst** (`4c528367`): 129 namen, gelezen van `regelspraak-toolchain` op `97d25bb` zonder `uchar/`; de tien
  meest gelezen: `ScenarioResultaat` (157), `naam` (102), `soort` (51), `Namen` (48), `diagnose` (41),
  `ProducentConsument` (37), `Fixpunt` (27), `verwachtingen` (25), `RegelGraaf` (22), `bron` (20). De lijst is per
  naam, dus houdt ze ook de gelijknamige locals van de zone: 18.644 van de 71.736 voorkomens blijven tot stap 6
  (`naam` alleen 2.218). `transpiler.kern` staat er niet in: `kern` is voor het gereedschap geen Nederlands
  (domeinwoord en Engels woord) en wordt dus ook niet hernoemd.
- **Woordenlijst** (`29110564`): 871 rijen ingevoegd, niets gewijzigd (plain 803, spec 28 met paragraaf, wire 21,
  field 19; 11 `false friend`), uit 1290 kandidaten (onbekende woorden en samenstellingen met een rest); niet
  opgenomen: Engels en vaktaal, afkortingen, domeinwoorden van testdocumenten. Vervoegingen en de tussen-s staan als
  eigen rij (`klassen`, `speelde`, `geldigheids` "the form in a compound"), zoals `afrond`. Een splitsdiff over
  alle namen van de repo ving acht rijen die elders verkeerd splitsten (`ding` in binding, `linker` in klinker,
  `mis` in mis_rendered, `assen` in passend, `beschermd`, `licht`, `dood`, `leegte`): weggelaten of gedekt met het
  langere woord (41 rijen, zoals `houden`, `termijn`, `opvolger`); `junit` staat in `technical-terms.tsv`.
- **Partijen** (`propose backends/drools`, niet in de repo): 4899 namen, 129 bevroren weggelaten; 3490 namen in 18
  partijen (method 1238, variable 938, field 686, type 426, constant 151, dir 37, file 7, test zonder `_` 6,
  parameter 1) en 1409 testzinnen in 8 partijen.

Open: een regel in de vertaalcontrole die een uitgang (`s`, `n`, `en`, `t`, `d`) na een Nederlands woord als
uitgang leest, zou de rijen voor vervoegingen overbodig maken; niet gedaan, het verandert `check`.

## Result (deel 2a, 2026-10-18)

Tweede bouwer: de eerste helft van werkwijze punt 3. De punten die in veel partijen terugkwamen staan in het
gereedschap of de woordenlijst, `merge` en `check` zijn gedraaid, en de tweede ronde voor Haiku staat klaar (buiten
de repo, in de werkmap van de hoofdsessie). Commits: `743e5ef3` schrijfwijze, `17a42833` `van` en `op`, `131a48f6`
domeinwoorden, `69735603` `merge` over meer mappen en de term van `afloop`, `1a34bf6a` vaste keuzes in de
woordenlijst, `a1a8dc2f` `melding` en verkeerde splitsingen, `e15e8ef9` de tabel en `prompts/retry.md`.
`test_rename.py`: 42 tests, OK; elke nieuwe fixture eerst rood tegen de oude stand (Keywords tegen een mutant: dat
gedrag bestond al).

- **Schrijfwijze.** `conventional` leidt de spelling af uit `old`, de taal en de vertaalde woorden (Java camelCase,
  een constante in hoofdletters met `_`, OCaml en shell met `_`, een bestands- of mapnaam als zijn buren, anders `-`);
  `merge` schrijft elk antwoord zo (296 antwoorden van de eerste ronde, alle als splitsing van aaneengeschreven
  woorden), `check` weigert een andere schrijfwijze. Een map met de naam van een identifier krijgt een eigen rij
  (`dagsoorten` en `objectcreatie`: `day-kinds`, `object-creation`).
- **`van` en `op`.** Rijen met context in de woordenlijst (`op`: by, on, to; `van`: of, from), getoond in de partij;
  `op` blijft op zichzelf Engels. `check` weigert een `new` die `van` aan begin of eind laat vallen (`indexOf`,
  `fromObject`): 4 rijen.
- **Domeinwoorden.** Het lexicon leest ook de declaraties van RegelSpraak-bronnen in code en de JSON van
  IR-documenten in tests; een partij noemt de domeinwoorden van elke naam; een naam van alleen domeinwoorden valt uit
  de inventaris (24 in de repo, nagelopen) en `merge` laat zijn antwoord weg (13 + `javaLang`). `opbrengst` kreeg
  geen rij: het is een gedeclareerde naam van het corpus.
- **Sleutelwoorden.** `check` weigerde ze al; 15 keuzes van de bouwer (`zaak` caseDocument, `klasse` javaClass, `vang`
  tryCatch, ...) staan als derde batchmap na de partijen, met reden; `afloop` is in de woordenlijst `decision`.
- **`check` op dezelfde antwoorden**, vóór (gereedschap `33bf795c`) en na: 1111 fouten op 1077 rijen, nu 784 op 764
  (4887 rijen). Per soort: gemarkeerd 432 → 416, inhoudswoorden 463 → 167, samenstelling in `words` 10 → 4, nog
  Nederlands 35 → 23, sleutelwoord 4 → 0, niet injectief 104 → 110, niet vers 44 → 41, type buiten gebruikt 15 → 15,
  lid naast lid 2 → 2, `van` aan de rand 0 → 4, javap 2 → 2. De zes paren erbij bij injectief verschillen alleen in
  hoofdletters (`datumwaarde`, `datumWaarde`): ze waren injectief omdat Haiku er een aan elkaar schreef.
- **Ronde 2:** 541 regels in 4 partijen (308 namen, 233 zinnen), elk met het eerdere antwoord en de meldingen van
  `check`; 22 kandidaten voor een betere naam (`sonde` en acht nagelopen namen) apart. Niet naar Haiku: 155 rijen die
  een scope of een andere naam van de sessie vragen, 46 gemarkeerde rijen met een woord dat de woordenlijst niet kent.

Open: `check` telt een OCaml-constructor of een eigen type van de frontend met dezelfde spelling als gebruik van een
Java-type van de zone (15) en vergelijkt een lid met een OCaml-recordveld (2); javap leest een geneste
bibliotheekklasse wel maar noemt haar `Map$Entry` (2). Op de beslislijst biedt `apply` voor een voorkomen in een string
alleen de `new` van de rij van de identifier aan, niet die van de maprij (`day-kinds`).

## Result (deel 2b en landing, 2026-10-18)

Geland op verzoek van de eigenaar ("ophouden met testen, veranderen, klaar"): de geteste stand `9974cba8`. De
vondsten van de lens zijn niet meer verwerkt; ze staan onder Open.

Commits na deel 2a (bouwers 3 tot en met 8): het gereedschap, elk punt met een fixture (`test_rename.py`: 85 tests);
`4887af7c` de tabel (4.926 rijen; `check` met de Drools-jars op `--classpath`: 0 fouten), de basis van `verify`;
`40aa34c9` en `5379fa49` de bestanden (363, alleen `git mv`); `a42f22f4` de beslislijst (38.552 rijen, 3.984 `change`);
`56948455` de inhoud (53.218 identifiers in 383 bestanden); `467b7c12` `.git-blame-ignore-revs`.

- **(a)** `inventory backends/drools` meldt geen Nederlandse naam, alleen de 130 namen van `frozen/drools.tsv` en vijf die
  de productieruntime leest (`Lidnamen`, `lidnamen`, `getVan`, `getTot`, `getWaarde`). Een woord dat de woordenlijst
  mist, ziet de inventaris niet (zie Open 1: `opslag`).
- **(b)** `check`: 0 fouten. `verify` op `9974cba8`: PASS. De boom is `4887af7c` met tabel en lijst; 1.781 weergaven
  bevatten geen oude naam van twee of meer woorden; de 16 gate-logs zijn gelijk aan de voor-logs van `2978518d` na
  toepassing van de tabel.
- **(c)** Via de lijst bijgewerkt buiten de zone: 142 plekken, waaronder `CLAUDE.md` (gate 12
  `BranchDumpsCorpusWitnessTest`, gate 15 `src/source-route`), de gate-scripts (35), de spec (71), NOTE's in het
  corpus (29) en de frontend (3). Daarbij twee commentaarregels buiten de May touch:
  `backends/clingo/test/test_clingo.ml` en `spec/verifier/lib/checks.ml:851`, elk een hernoemde testklasse.
- **(d)** Haiku: ronde 1 vertaalde 3.490 namen en 1.409 testzinnen in 26 partijen. Daarvan waren 432 gemarkeerd, en
  `check` weigerde 764 rijen. Ronde 2 deed 541 regels, waarvan 195 opnieuw gemarkeerd. De rest losten de bouwers op.
  De beslislijst telde 38.549 rijen, omdat identifiers als functiewoord heten (`de`, `een`, `niet`). Daarom verving
  een vaste voorsortering (`rename.py triage`) Haiku per rij: die besliste 37.476 rijen, en Haiku de overige 1.066
  (321 `change`; de bouwer draaide er 150 om). Strings en commentaar in de zone die geen regel besliste, bleven staan.
  Waar een string een hernoemde naam moest volgen (methodenamen in `Route`, recordcomponenten die de DRL-tekst en
  `PatternDslRendering` bij naam lezen, reflectie, resourcepaden, verwachte dumps), toonde de suite dat, en dat werd
  `change`. De lens bestond uit vier Sonnet-agents, die samen 1.375 rijen lazen:

  | lens | rijen | blokkerend | testzinnen fout |
  |---|---|---|---|
  | `improved` en de 142 plekken buiten de zone | 320 | 18 | 0 van 33 |
  | de in ronde 1 geweigerde rijen | 645 | 44 | 30 van 215 |
  | steekproef van 10%, seed 20261018 | 410 | 9 | 5 van 104 |

  Het oordeel van alle vier was EERST FIXEN. De plekken buiten de zone waren in orde.
- **(e)** 16/16 gates op `9974cba8` (2026-10-18 07:37–07:55). Gate 12 heeft JUnit 1619, played 255 en DRL-tekst 255,
  dezelfde 45 uitsluitingen en geen waardeverschil, gelijk aan de voor-log. Gate 15 is byte voor byte gelijk.

Besluiten en afwijkingen:
- `DekkingSoort.NIET_GERAAKT` is bevroren: de dienst van de distributie schrijft de constanten in zijn JSON en OpenAPI,
  en `freeze` telt nu `name()`, `values()` en `valueOf` van de distributie.
- `Proefrecord`, `Proefsoort` en `Gemaakt` heten `SampleRecord`, `SampleType` en `CreationStamp`, zodat de gesorteerde
  sessiedump haar volgorde houdt. `Sonde` heet `DepositFlag`.
- De bouwers zetten de branch drie keer met `git reset --hard` terug naar een eigen commit. Eén keer begonnen ze
  opnieuw vanaf `4887af7c`, omdat de map `breuk/een-derde` `breach` was geworden. Twee keer gebruikten ze `rm` of
  `find -delete` op een pad met een variabele, op een eigen map en zonder schade.
- Het onafgemaakte gereedschap van de fixronde staat in de stash van de worktree.

Open, voor een vervolgkaart:
1. De woordenlijst kiest op deze plekken de verkeerde betekenis, en de tabel volgt haar. Telkens: wat er nu staat,
   en wat het moet zijn.

   | woord | nu | moet |
   |---|---|---|
   | `terugzetten` | reset | write back |
   | `fout`, `lidfout`, `filterfout` in transpiler en TMS | error | fault |
   | `reikwijdte` | scope | reach (ch. 3.8) |
   | `dubbel` | duplicate | double |
   | `voor` | for | before |
   | `lopen` met lijdend voorwerp | run | walk |
   | `dubbele punt` | double point | colon |
   | `haalt` | gets | reaches |
   | `benoemd geweigerd` | refused once named | refused by name |
   | `tijdvrij` | timeless | time-free |
   | `Functie` als statische functie | `Aggregation` | `Function` |

   `opslag` (storage) ontbreekt in de woordenlijst, en daardoor bleef `OpslagDirectionWitnessTest` deels Nederlands.
2. 18 `improved`-rijen met scope `*` passen op één declaratie, maar hernoemen alle gelijknamige. Voorbeelden: `nieuw`
   → `recomputed` op 138 plekken, `ander` → `earlierBean`, `knoop` → `jsonNode`, `rij` → `queue` en `terug` →
   `readBack`.
3. Testzinnen: tussen 5% (steekproef) en 14% (geweigerde rijen) maakt een andere bewering dan de Nederlandse zin.
4. Domeinwoorden:
   - een identifier die een gedeclareerde naam van zijn testmodel draagt, is vertaald waar de woordenlijst het woord
     kent (`KEURBAAR`, `grondslag`, `Melding`);
   - gewone woorden die in een of andere gedeclareerde naam voorkomen, bleven Nederlands (`nieuwe`, `breedte`,
     `opbrengst`).
