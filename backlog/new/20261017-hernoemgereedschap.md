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
