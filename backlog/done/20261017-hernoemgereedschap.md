# hernoemgereedschap: hernoemen per hele naam, met een inventaris, een tabel, controles en een bewijs op twee zones

*Op verzoek van de eigenaar (2026-10-17). Tweede kaart van het programma "Engelse namen"; vereist
`engelse-namen-afspraak` (de regel en de woordenlijst). Raakt `scripts/`: de branch blijft een voorstel tot de
eigenaar merget.*

Goal: het programma hernoemt ±8.000 Nederlandse identifiers en ±2.600 testnamen in zinsvorm. Met de hand is dat
foutgevoelig en niet te reviewen. Met tekstvervanging (`sed s/Rol/Role/g`) raakt een regel de binnenkant van een
andere naam (`Rol` in `Rolfout` of `Controle`), hangt de uitkomst af van de volgorde van de regels, en verandert een
naam ook in strings, waar hetzelfde woord vaak domeininhoud is. Deze kaart bouwt `scripts/lib/naming/rename.py`, dat
per zone meet wat er nog Nederlands is en per hele naam hernoemt volgens een tabel die de eigenaar reviewt, en
bewijst het op twee kleine zones.

Modellen: de bouwer (Opus) schrijft het script en kiest de betere namen (`improved`); Haiku vertaalt de partijen en
de testzinnen en stelt de beslislijst voor; Copilot `sol6` (alleen lezen) is de lens op betekenis. Een bouwer start
zelf geen subagents en roept geen Copilot aan: hij zet de partijen klaar, de hoofdsessie draait Haiku en de lens.

1. Inventaris, `rename.py inventory <zone>`, op de tokenizer `scripts/lib/naming/tokens.py` (Java, OCaml, shell,
   ANTLR):
   - leest de identifiers buiten strings en commentaar, en de bestands- en mapnamen;
   - splitst een naam in woorden (camelCase, snake_case, cijfers) en zoekt in een woord dat het Engelse woordenboek
     niet kent ook naar woorden uit de woordenlijst van vier letters of meer, zodat een samenstelling zonder
     hoofdletter (`Invoervormfout`) gevonden wordt en `control` niet;
   - laat domeinwoorden, de tokennamen uit `RegelSpraakLexer.g4` en eigennamen (`RegelSpraak`, `Jutterswet`) buiten
     beschouwing;
   - meldt elke naam met bestand, regel, het Nederlandse woord en het Engelse voorstel uit de woordenlijst.

   Een hernoemkaart begint met de inventaris van haar zone en landt pas als die in de zone niets meer meldt, op de
   vaste uitzonderingen van `CLAUDE.md` na. Een Nederlandse naam die een andere kaart intussen toevoegde, komt zo in
   de tabel van de eerstvolgende stap. Er is geen gate voor namen (besluit van de eigenaar, 2026-10-17).
2. De tabel, `scripts/lib/naming/tables/<zone>.tsv`, met kolommen `scope`, `old`, `new`, `kind`, `words`, `reason`:
   - `old` en `new` zijn complete identifiers (`Invoervormfout` → `InputFormError`), nooit delen ervan;
   - `scope` is `*`, een bestand, of een Java-klasse of OCaml-module, voor een woord met twee betekenissen (`tak` als
     branch van de transpiler of als arm van een voorwaarde, `stand`, `lezing`);
   - `kind` is `translation` of `improved`; `words` is de splitsing van `old` (`invoer|vorm|fout`); `reason` is
     verplicht bij `improved`.
3. Voorstellen:
   - `rename.py propose <zone>` schrijft partijen van ±200 namen uit de inventaris, elk met zijn declaratie en de
     eerste zin van zijn javadoc of commentaar. Elke partij gaat naar een Haiku-subagent (`model: "haiku"`) met de
     woordenlijst en een vast JSON-regelformaat; `rename.py merge` voegt de antwoorden samen tot de tabel.
   - Een naam waarvan de letterlijke vertaling niet zegt wat het ding is, markeert Haiku. Die gaat naar een
     Opus-subagent die code en spec leest en een `improved`-naam met reden voorstelt. Voorkeur, in deze volgorde: de
     naam van de draad of de term van IR-SPEC en RUNTIME-SEMANTICS (bij `Emmerscalair` de "asOfDate bucket";
     "bucket" staat 87× in RUNTIME-SEMANTICS), een gangbare vakterm (`Betatoets` → `BetaConstraint`, `Stuurfeit` →
     `ControlFact`), een letterlijke beschrijving. Geen nieuw beeld; `Sonde` (probe) is er een.
   - Alleen namen: geen klasse splitsen, geen code verplaatsen. Anders vervalt het bewijs van punt 6.
4. Controles vóór het toepassen, `rename.py check <tabel>`:
   - elke `old` komt als heel token voor in zijn scope;
   - injectief: geen twee `old` op dezelfde `new` in scopes die elkaar zien;
   - vers: geen `new` bestaat al als naam die blijft, in de zone of in een zone die haar gebruikt, en ook niet als lid
     van een bibliotheektype dat een van onze klassen uitbreidt of implementeert (Java: `javap` op die typen);
   - bij `translation`: `words` aaneen is `old` (hoofdletters genegeerd), en de inhoudswoorden van `new` zijn precies
     de vertalingen van `words` volgens de woordenlijst, in willekeurige volgorde en zonder functiewoorden
     (`labelVanBean` → `beanLabel`). Alleen de `improved`-regels vragen dan een review op betekenis;
   - de inventaris meldt in geen enkele `new` een Nederlands woord.

   Met injectief en vers blijven namen die gelijk waren gelijk en blijven namen die verschilden verschillen, dus
   overload-resolutie en shadowing veranderen niet. Wie een lokale `waarde` hernoemt naar `value` in een klasse met
   een veld `value`, krijgt code die compileert en waarin verwijzingen naar het veld opeens de lokale variabele
   lezen; de versheidscontrole weigert dat.
5. Toepassen, `rename.py apply`:
   - één pass over de tokens; elk token wordt hooguit één keer vervangen, door zijn eigen regel, dus de volgorde van
     de regels doet er niet toe;
   - strings en commentaar slaat de pass over. In commentaar past ze de regels toe waarvan `old` twee of meer woorden
     heeft of gekwalificeerd is (`CaseInvoer.zetRollen`). Elk ander voorkomen van een `old` in commentaar, in een
     string of in een tekstbestand (scripts, docs, `pom.xml`, NOTE's) komt op
     `scripts/lib/naming/decisions/<zone>.tsv` met `keep` of `change`. Haiku stelt de keuze voor, de sessie beslist,
     en `apply` voert de lijst uit.
6. Bewijs na het toepassen, `rename.py verify`:
   - de werkboom is gelijk aan de uitgangsboom met tabel en beslislijst toegepast, zodat handwerk (een compileerfout
     oplossen) in de tabel of de lijst moet staan;
   - de gegenereerde DRL-tekst, de exec-model-weergave en het ASP-programma van elke golden bevatten geen `old` van
     twee of meer woorden meer;
   - de uitvoer van de volledige gateset, zonder tijdsregels, is gelijk aan die van vóór met de tabel erop toegepast.
7. Git: bestandshernoemingen en inhoud in aparte commits, zodat `git log --follow` de geschiedenis houdt; de
   inhoudscommits in `.git-blame-ignore-revs` (GitHub leest dat bestand; lokaal
   `git config blame.ignoreRevsFile .git-blame-ignore-revs`).

Acceptance:
- (a) `python3 -I scripts/lib/naming/test_rename.py`: een fixture per gevaar, elk eerst rood tegen een tijdelijke
  mutatie van het script (gemeten, niet bewaard) en dan groen: de inventaris vindt `Invoervormfout` en meldt
  `control` niet; substring (`Invoer` naast `Invoervormfout`); volgorde van regels; naamvangst (de lokale `waarde`
  naast het veld `value`); een niet-injectieve tabel; een lid van een bibliotheektype; een codenaam in een string
  (komt op de beslislijst); handwerk buiten de tabel (`verify` faalt).
- (b) Twee zones hernoemd met het script:
  - `frontends/flogic/` (OCaml, zes bestanden; geen modulenaam ervan komt buiten de zone voor);
  - `backends/drools/src/test/java/dev/uchar/regelspraak/loader/` (twintig testklassen, zoals
    `LaderWeigeringWitnessTest`). Hun namen staan ook in `frontends/regelspraak/grammar/DIVERGENCES.md`,
    `backends/drools/README.md`, `backends/drools/pom.xml`, twee testklassen in `cases/` en acht NOTE's in het
    corpus; die gaan via de beslislijst.

  De tabellen en beslislijsten staan in de branch. Gate 14 met dezelfde telling, gate 12 met dezelfde JUnit-telling,
  en `rename.py inventory` meldt in die twee zones niets meer.
- (c) Haiku gemeten: het aantal partijen, het aandeel regels dat `check` weigerde en welke soorten fouten, en het
  aantal betekenisfouten dat de lens per honderd testzinnen vindt. Dat bepaalt of Haiku de vervolgkaarten draagt of
  de testzinnen naar Sonnet gaan. Het Result zegt ook of de stappen 3–5 een Sonnet-bouwer kunnen krijgen: dat kan als
  `check` en `verify` elke fout van het proefwerk vingen zonder dat de bouwer moest ingrijpen buiten de tabel.
- (d) Een lens van Copilot (`sol6`, alleen lezen) op de twee tabellen, op betekenis; elke vondst verwerkt of met
  reden afgewezen in het Result.
- (e) Alle gates van `CLAUDE.md` groen op de eindstand van de branch.

May touch: `scripts/lib/naming/` (`glossary.tsv` alleen voor een ontbrekende vertaling, genoemd in het Result),
`frontends/flogic/`, `backends/drools/src/test/java/dev/uchar/regelspraak/loader/`, de bestanden die de beslislijsten
van die twee zones noemen, `.git-blame-ignore-revs` (nieuw), deze kaart.

Out of scope:
- Een gate voor namen.
- Andere zones: de vervolgkaarten.
- Meldingen; een betere naam die meer vraagt dan hernoemen.

Gate set: alle gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (deel 1, 2026-10-17)

Eerste bouwer: het gereedschap van de punten 1–7 en acceptance (a). De proef op de twee zones (b), de meting van
Haiku (c), de lens (d) en de gateset op de eindstand (e) zijn van de volgende bouwer. Alles staat onder
`scripts/lib/naming/`; `README.md` daar beschrijft de subcommando's, de formaten en de regels. De branch blijft een
voorstel.

Commits: `c07360e2` tokenizer; `787fb0c6` `rename.py` (inventaris, tabel, controles, apply, verify, render), de
lijsten `technical-terms.tsv` en `proper-names.tsv` en tien woorden in de woordenlijst; `fd495e4c` de opdrachten
voor Haiku en `README.md`; `2137a63e` `test_rename.py`; `efdff4ea` `check` weigert een variabele achter een gepunde
OCaml-recordlabel van buiten de zone (met fixture); `320dd62f` `propose` knipt de eerste zin niet bij "ch.".

- **Acceptance (a):** `python3 -I scripts/lib/naming/test_rename.py`: `Ran 19 tests`, `OK`. Elke fixture is eerst
  rood gezien tegen een tijdelijke mutatie van een kopie van de map (gemeten met een script in de werkmap, niets
  bewaard), en daarna groen:

  | fixture | mutatie | uitkomst |
  |---|---|---|
  | `Tokenizer.test_java` | geen text blocks | rood: `TokenizeError` (string niet afgesloten) |
  | `Tokenizer.test_ocaml` | commentaar nest niet | rood: `waarde` uit het commentaar telt als code |
  | `Tokenizer.test_shell` | geen expansie binnen dubbele aanhalingstekens | rood: `NAAM`, `LENGTE`, `basename` ontbreken |
  | `Tokenizer.test_antlr` | een actie wordt niet als Java gelezen | rood: `"}"` is geen string |
  | `Inventory` (`Invoervormfout`) | geen zoektocht naar woorden in een woord | rood: `Invoervormfout` niet gevonden |
  | `Inventory` (`control`) | zoeken vanaf drie letters | groen: het woordenboek kent `control` |
  | `Inventory` (`control`) | geen woordenboek én vanaf drie letters | rood: `control` gemeld (`rol`) |
  | `Substring` | sed: `str.replace` per regel, in tabelvolgorde | rood: `Inputvormfout` |
  | `Order` | dezelfde sed | rood: `return amount + amount` |
  | `Capture` (Java, veld `value`) | geen versheid tegen namen die blijven | rood: geen fout |
  | `Capture` (OCaml, `let`) | idem | rood: geen fout |
  | `Injective` | geen injectiviteit | rood: geen fout |
  | `LibraryMember` (`Comparator.reversed`, `Object.toString`) | geen bibliotheekleden | rood: geen fout |
  | `CodeNameInString` | strings overgeslagen | rood: `apply` past toe (0) in plaats van een lijst (3) |
  | `Comments` | elke naam in commentaar automatisch | rood: geen beslislijst |
  | `Handwork` | geen vergelijking van de bomen | rood: `verify` slaagt met handwerk |
  | `FilesOnly` | `--files-only` schrijft ook de inhoud | rood: status `RM` in plaats van `R` |
  | `External` | kwalificaties buiten de zone genegeerd | rood: `Buiten.value` |
  | `Punned` | geen controle op gepunde labels | rood: geen fout |
  | `Translation` | geen vertaalcontrole | rood: geen fout |
  | `Merge` | een ontbrekend antwoord mag | rood: `merge` slaagt |

  `control` is dus twee keer beschermd: door het woordenboek en door de grens van vier letters.

- **Inventaris op de hele repo** (`inventory . --by-zone 2`): 9.727 Nederlandse namen, 130.768 voorkomens;
  looptijd 2,4 s (2,9 s met het laden van woordenboek, woordenlijst en corpus). Per zone, een naam in twee zones
  telt in beide: `backends/clingo` 803, `backends/drools` 5.157, `corpus/golden` 449, `corpus/negatief` 362,
  `corpus/rood` 32, `corpus/jutterswet` 1, `frontends/catala` 255, `frontends/flogic` 194,
  `frontends/regelspraak` 3.395, `ref/lib` 354, `ref/test` 516, `spec/verifier` 479, `scripts/` 61
  (`check-reftype-derivation.sh` 19, `check-graaf.sh` 16, `check-drools.sh` 10, `check-rood.sh` 4,
  `check-verifier.sh` 3, de andere elk 1, vooral hun bestandsnaam). In corpus tellen alleen bestands- en
  mapnamen; de `jutterswet-*`-mappen zijn domein. 51 namen van de productieruntime telt de hele repo niet.
  Op zichzelf: `frontends/flogic` 75 namen (221 voorkomens; 119 namen die buiten de zone gedeclareerd zijn, vooral
  in `Ir_v0.Ast`, telt de zone niet), de lader-tests 425 namen (2.688; 163 van buiten, zoals
  `IrLoader.Documentfout` en `IrPad`). `propose` geeft voor flogic één partij van 75 namen (35 bestandsnamen), voor
  de lader-tests twee partijen namen (200 en 25) en één partij van 200 testzinnen.
- **De woordenlijst** miste deze tien Nederlandse woorden het vaakst (aantal namen, voorkomens); ze staan er nu in,
  met `source` en in `note` de reden ("added by card hernoemgereedschap: in N identifiers"): `hoog` → `high` (8,
  22, `plain`), `benoembaar` → `designatable` (4, 346, `spec`, IR-SPEC §2.12), `paden` → `paths` (4, 76, `wire`),
  `zinnen` → `sentences` (4, 64, `spec`, §4.6), `gezet` → `set` (4, 59), `houd` → `keep` (4, 48), `genavigeerd`
  → `navigated` (4, 37, `spec`, §4.1a), `elementaire` → `elementary` (4, 36), `globalen` → `globals` (4, 25,
  `field`), `vloer` → `floor` (4, 24). Niet opgenomen: de afkortingen `mnd` (12) en `gran` (8), zoals de eerste
  kaart geen afkortingen opnam, en `getij` (6) en `wrak` (4), domeinwoorden van testdocumenten die het corpus niet
  declareert. Elke rij is alleen ingevoegd; de volgorde van de bestaande rijen bleef.
- **Open punt (i) van de eerste kaart, besloten:** `van` en `die` zijn Nederlands binnen een naam van twee of meer
  woorden, `of` en `op` nooit; de vertaalcontrole laat alle vier als functiewoord weg (`labelVanBean` →
  `beanLabel`). Ze staan als vaste lijst in `rename.py` (`COMPOUND_FUNCTION_WORDS`, `CODE_FUNCTION_WORDS`), niet in
  de woordenlijst, omdat een rij daar ze overal Nederlands zou maken. De zwakte: een Engelse naam van twee woorden
  met `van` of `die` (`dieRoll`) wordt als Nederlands gemeld. Functiewoorden herkent het script aan `context` =
  `function word`; ontkenningen (`niet`, `geen`, `zonder`, `nooit`, …) laat de controle staan.
- **Open punt (ii), besloten:** `callee`, `iterator`, `overprecision` en `standalone` staan in
  `technical-terms.tsv`, met reden; daarbij `json`, `drl`, `tms`, `asp` en `mvel`.

Anders dan de kaart of de opdracht, en waarom:

1. `rename.py` is één bestand met alle subcommando's; de commits volgen de onderdelen waar dat kon (tokenizer,
   script, opdrachten, fixtures), niet één commit per subcommando.
2. Twee subcommando's erbij: `merge-decisions` (de antwoorden van Haiku op de beslislijst, als JSON-regels per rij,
   in de kolom `decision`; een TSV laat een taalmodel makkelijk scheef) en `render` (de weergaven voor `verify`).
3. De beslislijst heeft na `decision` een kolom `context` (de regel), zodat Haiku zonder de bestanden kan kiezen;
   `change:<naam>` kiest bij een naam met meer nieuwe namen.
4. Buiten de zone zoekt `apply` alleen namen van twee of meer woorden, de typen en modules van de zone, en
   hernoemde bestanden met hun extensie (`aggregatie.flogic`): een naam van één woord is daar gewoon Nederlands
   proza. Een eerste proef zonder die grens zette elk `aggregatie` in commentaar en meldingen van de hele repo op
   de lijst.
5. Een voorkomen dat iets buiten de zone noemt (gekwalificeerd door een type of module van buiten, een Java-type
   of OCaml-constructor of -veld dat alleen buiten gedeclareerd is, een recordlabel van een type van buiten), telt
   de inventaris niet en laat `apply` staan; `check` weigert een regel voor een naam die alleen buiten bestaat.
   Zonder dat kan de inventaris in de lader-tests nooit leeg worden: `IrLoader.Documentfout` staat in main.
6. Vrijgesteld naast de vaste uitzonderingen van `CLAUDE.md`: `backlog/` (de kaarten en hun slugs) en
   `scripts/lib/naming/` (de tabellen noemen elke oude naam). `verify` vergelijkt de bomen buiten die twee mappen
   en buiten `.git-blame-ignore-revs`.
7. Extra controles: de schrijfwijze van `new` volgt `old` (een `old` van één woord laat de keuze); geen sleutelwoord;
   twee regels van één naam met even ver reikende scopes die overlappen; een hernoemd bestand komt niet op een
   bestaand; een type of module van de zone die een regel hernoemt, wordt niet als code buiten de zone gebruikt; een
   gepunde OCaml-label (`Numeriek { geheel; ... }`) van een type van buiten. Woorden van de `jutterswet-*`-mappen
   gelden als domeinwoorden, zoals de gedeclareerde namen.
8. De exec-model-weergave heeft in de repo geen tekstvorm: het PatternDSL-model zelf wordt nergens uitgeschreven.
   `render` neemt daarvoor de sessiedump van de exec-model-tak (de naad `uchar.vuurspoor` van de jar, per golden),
   die elke feitklasse en elk lid noemt dat de weergave invoegt. Wat ontbreekt is een dump van het model zelf
   (regelnamen, declaraties, de veldnamen van `PatternDslWeergave`); die vraagt code in de backend, buiten deze
   kaart. De DRL-tekst komt uit de test die `uchar.drlvorm` leest, het ASP-programma van een `clingo` vooraan op
   `PATH` die elk programma kopieert. Gemeten op `efdff4ea`: 261 DRL-teksten, 694 sessiedumps over 300 goldens
   (255 PLAYED, 45 EXCLUDE), 823 ASP-programma's, ±5,5 min. Een proeftabel met `KenmerkStand` gaf 414 treffers.
9. Gate-logs: geijkt op twee volledige runs van de 16 gates op één boom (alle 16 groen, 18 min elk). Ze verschillen
   in twee logs: de `drools-jar:`-regel van gate 8 (`rebuilt` tegen `fresh`) en de veegtijd van gate 12 (194 s
   tegen 195 s). `verify` laat de `drools-jar:`-regels weg en schrijft duur, kloktijd en tijdelijke paden als
   `<time>`, `<clock>`, `<tmp>`, zodat de regel "DRL-text branch played 255" meetelt; daarna 0 verschillen.
10. `frontends/flogic/` telt 39 bestanden, waarvan zes OCaml; de andere zijn F-logic-testprogramma's, case-bestanden,
    `dune`-bestanden en een README. Hun namen staan in de inventaris en via strings in `suite.ml` op de beslislijst.

Voor de volgende bouwer (de proef op de twee zones):

- Per zone, met `Z` = `frontends/flogic` en tabel `flogic`, of `Z` =
  `backends/drools/src/test/java/dev/uchar/regelspraak/loader` en tabel `drools-loader-tests`:
  `python3 -I scripts/lib/naming/rename.py inventory Z`; `... propose Z <werkmap>/<tabel>` (schrijft
  `names-NNN.jsonl`, `sentences-NNN.jsonl`, `zone.json`); de hoofdsessie draait per partij een Haiku-agent met
  `prompts/translate.md` (antwoord in `<partij>.answer.jsonl`); `... merge <werkmap>/<tabel>
  scripts/lib/naming/tables/<tabel>.tsv`; de gemarkeerde regels een `improved`-naam met reden geven; `... check
  scripts/lib/naming/tables/<tabel>.tsv` tot het schoon is, commit; `... apply <tabel> --files-only`, commit;
  `... apply <tabel>` (schrijft `decisions/<tabel>.tsv`, exit 3); Haiku met `prompts/decide.md` per bereik van
  rijen; `... merge-decisions scripts/lib/naming/decisions/<tabel>.tsv <antwoorden>...`; nalopen, commit; `...
  apply <tabel> --decisions scripts/lib/naming/decisions/<tabel>.tsv`, commit, de hash in
  `.git-blame-ignore-revs`.
- Bewijs: `... render <werkmap>/render-na` (achtergrond, ±6 min), de 16 gates voor en na met één log per gate in
  een map, dan `... verify <tabel> --base <commit vóór --files-only> --decisions ... --renderings
  <werkmap>/render-na --gate-logs <voor> <na>`; een gemist been meldt `NOT RUN` en "no landing proof".
- Wat de proef met een kleine tabel op flogic al liet zien: `geheel`, `decimalen` en `teken` zijn gelabelde
  parameters van `num` en tegelijk gepunde labels van `A.Numeriek`; `check` weigert ze, en ze blijven tot stap 5
  de velden van `Ir_v0.Ast` hernoemt. `whole` bestaat al als label in `lower.ml` (vers). `rol_binding` is zowel
  een functie van de zone als het type van `Ir_v0.Ast`: het script scheidt de naamruimten van OCaml niet, de
  compiler meldt het.
- Woorden die de woordenlijst voor de lader-tests nog mist (de vertaalcontrole weigert een `translation`-regel met
  een Nederlands woord buiten de lijst): `optioneel`, `structureel`, `generieke`, `daalt`, `laad`, `verplicht`,
  `kort`, `laag` (in `defaultslaag`), `hernoeming`, `hernoemd`, `synthetisch`, `gedekt`, `afrond`. Toevoegen mag,
  genoemd in het Result.

Open:

- De weergave van het exec-model zelf (punt 8 hierboven).
- De naamregels van ANTLR-gegenereerde code (`ToewijzingContext`, `visitToewijzing` bij de parserregel
  `toewijzing`) leidt het script niet af; stap 6 van het programma zet ze als eigen regels in de tabel.
- Java en OCaml worden gelezen per token, niet geparst: de tweede variabele van `int a, b;` geldt niet als
  declaratie, en OCaml-waarden en -typen delen hier één naamruimte. Wat erdoor glipt, meldt de compiler luid; de
  controles zijn er voor de stille gevallen (vangst, schaduw, bibliotheeklid).
- Valse vrienden gelden als Nederlands, zoals de opdracht zegt; in deze repo bleken de gemeten namen met alleen
  een valse vriend (`naam`, `met`, `datum`, `pad`, `links`) Nederlands. `over` is neutraal: een van zijn
  vertalingen is het woord zelf.

## Result (deel 2, 2026-10-17)

Tweede en derde bouwer: de proef op de twee zones (b), de meting van Haiku (c) en de lens (d); (e) vult de
hoofdsessie in na de laatste gate-run. De branch blijft een voorstel.

Commits sinds deel 1: `bae359f5` woordenlijst (95 rijen, alleen ingevoegd); `12ee5bdf` de vertaalcontrole telt
functiewoorden aan beide kanten; `b580b958` check en propose, bijgesteld op de antwoorden van Haiku; `4fb0bfb3`
woordenlijst (7 rijen); `71aec063` een lid achter een waarde is van de zone alleen als de zone het declareert;
`7b916954` versheid per bestand; `aa276a9f` de twee tabellen (de basis van `verify --base`); `74e2ca51` en `c339a47c`
de bestandshernoemingen (40 en 19 bestanden, alleen `R`); `e2ffab04` en `f06d75c9` de beslislijsten zonder
beslissingen; `64512aeb` de beslissingen; `0283a683` `munt` en `GRENSRAND` in de tabel van de lader-tests; `862a9519`
`verify` neemt de tabellen van alle zones van een branch; `e9a17155` de vondsten van de lens; `0d1a6251` en `de5c2763`
de inhoud van flogic en van de lader-tests; `ffb6351f` `.git-blame-ignore-revs` met die twee inhoudscommits.

- **Acceptance (b).**
  - `frontends/flogic`: tabel 56 rijen (alle `translation`), 40 bestanden hernoemd; beslislijst 52 rijen, alle in
    `test/suite.ml`: 42 `change` (de hernoemde bestandsnamen in strings), 10 `keep` (de uitvoer `[emit-proef]`,
    `emit-proef : ...` en `flogic-proef`; alleen de waarde `proef` heet `trial`).
  - De lader-tests: tabel 440 rijen (416 `translation`, 24 `improved`), 19 bestanden hernoemd; beslislijst 358 rijen,
    28 `change` en 330 `keep`, waarvan 29 buiten de zone (13 in `SubtypeDialectTest`, 8 NOTE's in het corpus, 2 in
    `DIVERGENCES.md`, 6 in andere testklassen en `IrLoader.java`). `munt` en `GRENSRAND`, die de tweede bouwer
    Nederlands liet als domein- en goldennaam, staan nu in de tabel, omdat de inventaris leeg moet zijn: `munt`
    (twee variabelen met het eenheidstelsel dat de golden `Munt` noemt; de woordenlijst kent `munt` alleen als
    werkwoord) → `unitSystem` (`improved`, de naam van de draad), `GRENSRAND` (de constante van golden
    `synth-grensrand`) → `BOUNDARY_EDGE`; de slug blijft tot stap 8.
  - `inventory`: de lader-tests 0 namen. flogic 24 namen (123 voorkomens), die met stap 5 van het programma meegaan,
    als de AST van `Ir_v0.Ast` hernoemd wordt; het gereedschap kent geen OCaml-typen en is daarvoor niet uitgebreid:
    - gepunde labels van een AST-record, die `check` weigert: `geheel`, `decimalen`, `teken` (`A.Numeriek`),
      `objecttype` (`Attribuut_ref`), `kwantor`, `drempel` (`Samengesteld`), `kenmerk` (`Kenmerk_check`),
      `objecttypen`, `feittypen` (`A.document`);
    - een functie van de zone die heet naar het AST-type dat ze uitschrijft en dat type in haar annotatie draagt
      (`let rol (r : rol)`): `conditie`, `geldigheid`, `rol`, `feittype`, `rol_binding`, ook `objecttype`; het
      gereedschap zou de annotatie mee hernoemen. `resolved_regel` staat alleen als type in een annotatie;
    - een AST-label waarvan het Engelse woord in het bestand al een naam is (`check`: niet vers): `naam`, `soort`,
      `doel` (`doel = Some (doel m ...)` in `lower.ml`);
    - een AST-label naast de waarde van dezelfde naam (`bron = Some bron`, `waarde = Some waarde`): `bron`, `waarde`;
      de helper `regel ~waarde ~voorwaarde` in `suite.ml` blijft in één taal met die labels: `regel`, `voorwaarde`;
    - namen uit het handgebouwde testdocument van `suite.ml`, die het corpus niet declareert: `seinpaar`,
      `zichtbaar`.

    Bij `bron`, `waarde`, `regel`, `voorwaarde` en `zichtbaar` weigert `check` niets: zij blijven op een keuze, de
    andere negentien houdt `check` of de compiler tegen.
  - Gate 14 op `ffb6351f`: 150 checks, 2 goldens, gelijk aan de run vóór de proef. Gate 12 op de stand vóór de
    lens: JUnit 1619, played 255, dezelfde lijsten (auto-excluded 45 met dezelfde redenen, channel floor 48,
    DRL-tekst 255); alleen de veegtijd verschilt. De volledige gateset op `8799905f` (de stand vóór de lens) is
    groen, 16/16.
  - `verify` van beide tabellen, `--base aa276a9f`, met beide beslislijsten, de weergaven van `8799905f` en de
    gate-logs van vóór de proef en van `8799905f`: de boom is de basis met beide tabellen en lijsten toegepast; de
    weergaven, 1781 bestanden, bevatten geen oude naam van twee of meer woorden (304 zulke namen); de 16 gate-logs
    zijn gelijk na de tabel. Het overzicht `summary.txt` van het gate-script verschilt in zijn startregel (commit en
    branch); `verify` meldt dat als enige verschil, met alleen de 16 logs zegt het `verify: PASS`. De vondsten van de
    lens komen in geen gate-log voor: een groene gate drukt geen testnamen en geen checknamen af.

- **Acceptance (c), Haiku.** Vier partijen namen en zinnen, drie partijen beslissingen.

  | partij | antwoorden | gemarkeerd | `check` weigerde in de eerste run (gereedschap op `12ee5bdf`) |
  |---|---|---|---|
  | flogic `names-001` | 80 | 4 | 19 (24%): niet vers 15, gepunde label 8, niet injectief 1 |
  | lader-tests `names-001` | 200 | 20 | 22 (11%): niet vers 15, niet injectief 3, vertaling 3, Nederlands over 2, buiten gedeclareerd 1 |
  | lader-tests `names-002` | 34 | 4 | 4 (12%): niet vers 2, vertaling 2 |
  | lader-tests `sentences-001` | 200 | 6 | 57 (28%): vertaling 55, Nederlands over 2, stijl 2 |

  (Een regel met twee fouten telt één keer.) De meeste weigeringen kwamen niet van Haiku:
  - van het gereedschap: 53 van de 55 vertaalweigeringen van de zinnen (woordvorm `is_refused` tegen `refuses`,
    `non` tegen `not`, `every`, `too`, `another`, `become`), de 2 stijlweigeringen, 17 onverse namen achter een punt
    en de derde variabele van één declaratie; opgelost in het gereedschap (zie de Sonnet-vraag);
  - van de zone: de gepunde labels en de onverse namen van flogic, en de niet-injectieve paren, twee Nederlandse
    woorden op één Engels woord (`voorwaarde`/`conditie`, `lees`/`gelezen`), opgelost met een scope per bestand.

  Fouten van Haiku zelf: 16 van 514 antwoorden (3,1%). `check` ving er 13: een samenstelling als één woord
  (`DAYDATE`, 4), een Nederlands woord blijven staan (1), een synoniem naast de woordenlijst (`verbatim`, 1), de valse
  vriend `stand` (2), bulten in een OCaml-naam (`factType`, 5). Niet gevangen, betekenis: 3 (`dagdatumErbij` →
  `dayDateWith`, twee keer `is_refused_as_named`), gevonden door de tweede bouwer. De lens vond daarnaast 1
  betekenisfout op 198 testzinnen (0,5 per honderd) en één beeld (advies); zie (d).

  Wat de bouwers met de antwoorden deden: van de 514 namen en zinnen overgenomen 439; door de bouwer gewijzigd 18
  (scopes per bestand voor de niet-injectieve paren, twee namen vrijgemaakt: `kind` → `child`, `value` →
  `boundValue`); gemarkeerd en toch een gewone vertaling 18; een `improved`-naam 13, later 2 erbij (`munt` en de
  testzin van vondst 1 van de lens); Nederlands gebleven 26: 24 in flogic, en `munt` en `GRENSRAND`, die nu in de
  tabel staan.

  De beslislijsten (Haiku stelt voor, de bouwer beslist): flogic 1 van 52 omgedraaid (rij 44, het voorvoegsel van
  het tijdelijke bestand: uitvoer, dus `keep`); de lader-tests 19 van 358, alle van `change` naar `keep`: de sleutel
  `value` van een Let-knoop op de draad (rij 67), de componenten van `Expr.IfThenElse` in de frontend (62–64), twee
  parameters `naamVeld` buiten de zone (330, 335), en `Plek 1` tot en met `Plek 6` in `SubtypeDialectTest`, de zes
  plekken in de frontend en niet het record `Plek` (346–358). Samen 20 van 410 (4,9%), steeds `change` waar `keep`
  goed was. De derde bouwer zette zelf 9 rijen van flogic ten onrechte op `change` (de checknamen `emit-proef`);
  de lens ving dat.

  Conclusie: Haiku kan de vertaling, de testzinnen en de beslislijsten dragen, met `check`, een bouwer die de
  gemarkeerde rijen en de beslislijst naloopt, en een lens op betekenis.

  De Sonnet-vraag: nee voor de stappen 3–5, zolang het gereedschap tijdens een proef buiten de tabel moet worden
  bijgesteld. Dat gebeurde hier vijf keer, elk met een fixture die eerst rood en dan groen was: `12ee5bdf` en
  `b580b958` (de vertaalcontrole, de stijl, versheid achter een punt, de tweede variabele van een declaratie),
  `71aec063` (`apply` hernoemde ±250 lezingen van leden van de hoofdcode achter een punt, in 43 rijen; `check`
  ving dat niet, de compiler wel, en de tabel kon het niet herstellen), `7b916954` (versheid per bestand),
  `862a9519` (`verify` kon een branch met twee zones niet bewijzen: met alleen de tabel van flogic telden de
  hernoemingen van de lader-tests als handwerk, 49 verschillen). Daarbij kreeg de woordenlijst twee keer rijen
  (`bae359f5`, 95; `4fb0bfb3`, 7). Elke fout die de proef opleverde, moest de bouwer zelf vinden of in het
  gereedschap oplossen; een Sonnet-bouwer die alleen de tabel en de lijst bijstelt, kan dat niet.

- **Acceptance (d), de lens** (Copilot `sol6`, alleen lezen, op de twee tabellen en de beslislijsten):
  1. Blokkerend: `een_conditiemarker_met_type_en_een_lege_terugkoppeling_weigeren` →
     `a_condition_marker_with_type_and_an_empty_back_link_is_refused` las als één marker met twee gebreken; de test
     weigert twee documenten, elk apart. Nu `a_condition_marker_with_type_and_an_empty_back_link_are_each_refused`
     (`improved`, het voorstel van de lens; `each` is geen functiewoord, dus geen `translation`).
  2. Blokkerend: de kop `[emit-proef]`, de checknamen `emit-proef : ...` en het voorvoegsel `flogic-proef` in
     `suite.ml` zijn uitvoer, en uitvoer blijft in deze stap `keep` (`prompts/decide.md`). Teruggezet op `keep`,
     rijen 43–52 van `decisions/flogic.tsv`.
  3. Advies: `de_weg_terug_loopt_over_de_effectieve_naam` → `the_way_back_runs_through_the_effective_name` hield het
     beeld. Nu `the_effective_name_goes_back_to_its_declaration` (`improved`): IR-SPEC ch. 2.12 zegt dat de naam in
     een vocabulaire "goes back to" de declaratie, en de test leest benamingen terug met `benoemd`; de lens stelde
     `..._resolves_back_to_...` voor, het werkwoord van de spec past beter.

  Verwerkt volgens de README: de tabel en de lijst gecorrigeerd, `git reset --hard` tot vóór de inhoud, `apply`
  opnieuw, daarna `verify` (boven). De lens kon `git show` niet draaien en las de diff dus niet; die dekken `verify`
  (de boom is de basis met tabel en lijst) en de gates.

- **Acceptance (e):** gate-run van de hoofdsessie op `2978518d` (2026-10-17 21:49–22:08): alle 16 gates groen
  (gate 12 617 s, gate 6 206 s, gate 13 119 s; de rest onder de minuut). Daarna `verify` met beide tabellen en
  hun beslislijsten, `--base aa276a9f`, de weergaven van `bouwer-B3/render-na` en `--gate-logs` (de voor-logs
  van `bea2242d` zonder de samenvatting van de runner, de na-logs van `2978518d`): PASS op de boom, op 1781
  weergaven (304 namen van twee of meer woorden, geen oude meer) en op 16 gelijke gate-logs.

Open, voor de vervolgkaarten:

- De 24 namen van flogic hierboven: ze gaan mee met de AST in stap 5.
- Het commentaar op `IfThenElseUnionWitnessTest.java:129` noemt de componenten `conditie`, `dan` en `anders` van
  `Expr.IfThenElse`. Die blijven tot stap 4; de beslislijst van stap 4 noemt deze plek niet (namen van één woord
  buiten haar zone), dus die kaart zet hem zelf op de lijst.
- Eigennamen in de vertaalcontrole: een vertaling met `RegelSpraak` (`RegelSpraakInvoer`) kan nooit slagen. Niet in
  deze twee zones, wel in de vervolgkaarten.
- De weergave van het exec-model zelf (deel 1, punt 8): de sessiedump staat ervoor in.
- Copilot staat sinds vanavond uit: de lens van de vervolgkaarten is een verse Sonnet-agent.
