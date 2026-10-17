# engelse-namen-afspraak: de naamregel in `CLAUDE.md` en een woordenlijst

*Op verzoek van de eigenaar (2026-10-17), na `ZusterrolInvoerWitnessTest` in `9159c9bd`. Eerste kaart van het
programma "Engelse namen" (volgorde onder Vervolg). Raakt beschermde paden (`CLAUDE.md`, `scripts/`): de branch
blijft een voorstel tot de eigenaar merget. Tellingen op `9159c9bd`.*

Goal: `CLAUDE.md` zegt "Infrastructure, spec metamodel, and node names: English", maar de code volgde dat niet. Van
de Java-klassen die sinds september zijn toegevoegd, kreeg ±90% een Nederlandse naam (september 167 van 189, oktober
210 van 232), vaak zoals de kaart ze voorschreef (`CaseInvoer`, `Rolbotsing` in `case-invoer-publiek`). Deze kaart
maakt de regel precies en legt de woordenlijst vast waaruit kaarten, bouwers en het hernoemscript hun Engelse
woorden halen. Er komt geen gate bij (besluit van de eigenaar, 2026-10-17): de regel geldt bij het schrijven van een
kaart en bij de review, en de inventaris van het hernoemscript meet per stap wat er nog Nederlands is. Bestaande
namen hernoemen de vervolgkaarten.

Modellen: Haiku deelt de kandidaatwoorden in (Nederlands, Engels, afkorting, domein) en stelt per woord een Engelse
term voor; de bouwer (Opus) stelt de woordenlijst en de regel vast. Een bouwer start zelf geen subagents; de
hoofdsessie draait de Haiku-partijen.

1. `CLAUDE.md`, Conventions, in de plaats van de huidige regel over Engelse namen, in de taal van dat bestand:
   - Every identifier is English: classes, interfaces, records, enums and their constants, methods, fields,
     variables, OCaml modules, types, constructors and functions, grammar rules, ASP predicates, shell variables,
     CLI flags and test names; so is every file and directory name.
   - Where the wire has a name for a concept, the code uses it (`emptyFallback`, not `leegwaarde`).
   - Dutch, as authored: domain content (the Jutterswet, declared names, RegelSpraak sources, case labels, the
     `jutterswet-*` names) and the RegelSpraak keywords (the lexer's literals and the token names that spell them).
   - Not renamed, each for its reason: the production runtime under
     `backends/drools/src/main/java/dev/uchar/regelspraak/runtime/` (gate 1 recognises a copy by its names), the
     `toka/` zone, v0 names where they stand as v0 names (IR-SPEC "v0 name:", CONSTRUCTS, `spec/proposals/`), git
     history and `backlog/done/`.
   - Working prose — cards, Results, commit messages, `spec/proposals/` — is Dutch or English; every identifier it
     names or introduces is English, also when a card prescribes a new class.
2. `scripts/lib/naming/glossary.tsv`, kolommen `dutch`, `english`, `source`, `context`, `note`:
   - `source` is `wire` (`spec/proposals/v1-naamtabel.json` of `spec/schema/uchar-ir-v1.schema.json`), `spec` (een
     term uit de Engelse tekst van IR-SPEC of RUNTIME-SEMANTICS, met paragraaf), `field` (een gangbare vakterm,
     zoals Rete's alpha en beta constraint of control fact) of `plain`, een gewone vertaling zonder bron in draad,
     spec of vakgebied (de meeste functiewoorden).
   - Eén Nederlands woord mag meer Engelse woorden hebben, elk met zijn `context`; de naamtabel doet dat al
     (`voorwaarde` → `condition`, en `justification` in een defaultsgroep).
   - Inhoud: elk `klasse: nl`-paar van de naamtabel, in woorden gesplitst; elk Nederlands woord dat op `9159c9bd` in
     minstens vijf verschillende identifiers voorkomt; de functiewoorden van de testzinnen (`een`, `het`, `zonder`,
     `wordt`, …), behalve woorden die ook gewone Engelse code zijn (`op`, `van`, `die`, `is`, `in`); en de valse
     vrienden die het Engelse woordenboek kent (`datum`, `stand`, `lid`), met `note` = `false friend`.
   - Woorden die alleen in gedeclareerde namen van het corpus voorkomen (`vondst`, `jutter`), komen er niet in: een
     testvariabele die een domeinobject draagt, mag zijn domeinnaam houden.
   - Voor de RegelSpraak-sleutelwoorden die de Engelse spec als term gebruikt (`leeg` 551× in RUNTIME-SEMANTICS,
     `waar`, `onwaar`, `gedurende`) een voorstel per woord, met telling en reden. Voorstel voor `leeg`: `empty`, de
     naam van de draad; de spec citeert `leeg` vooral in RegelSpraak-notatie (`leeg ÷ 5 = 0`), haar proza zegt
     "empty".

Acceptance:
- (a) De regel van punt 1 in `CLAUDE.md`, Conventions.
- (b) De woordenlijst bevat alles van punt 2, gemeten: het aantal regels per `source`, en welk deel van de
  Nederlandse woorden in de identifiers van `9159c9bd` ze dekt. Het voorstel voor de sleutelwoorden staat in het
  Result.
- (c) Alle 16 gates groen op de eindstand van de branch; er verandert geen code.

May touch: `CLAUDE.md`, `scripts/lib/naming/glossary.tsv` (nieuw), deze kaart.

Out of scope:
- Een gate voor namen.
- Een bestaande naam hernoemen: de vervolgkaarten.
- Meldingen (kaart `meldingscodes`), de distributie, de case-slugs.
- `WAY-OF-WORKING.md`: de regel staat in `CLAUDE.md`.

Vervolg, elk een eigen kaart, in deze volgorde, met het model van de bouwer:
1. deze kaart (Opus);
2. `hernoemgereedschap`: het hernoemscript met zijn inventaris, bewezen op `frontends/flogic/` en de lader-tests van
   de Drools-backend (Opus);
3. Drools-backend: alles wat de distributie niet importeert, met alle tests en testresources (Sonnet, als kaart 2
   bevestigt dat het script de correctheid draagt; anders Opus);
4. frontend: idem (idem);
5. de OCaml-zones (ref, verifier, clingo, catala) met de ASP-predicaten en `spec/ASP-TRANSLATION.md`, de
   shellvariabelen van de gatescripts en `ref --bodemzin` (idem);
6. wat de distributie importeert (publieke klassen, de pakketten `graaf` en `transpiler.kern`, de parserregels),
   samen met de distributie in één pin-bump (Opus);
7. `meldingscodes` (Opus, met één ontwerpreview van Fable), daarna de Engelse en Nederlandse catalogus voor frontend
   en Drools-backend (Opus);
8. paden: `corpus/negatief`, `corpus/rood`, `bron.*`, `check-rood.sh`, `check-graaf.sh` en de case-slugs, met de
   23 bestanden van de distributie die ze noemen (Opus).

In elke hernoemkaart vertaalt Haiku de namen, de testzinnen en de beslislijst, kiest Opus de betere namen, en leest
Copilot `sol6` de tabellen op betekenis.

Daarna de eigen namen van de distributie, in haar eigen backlog.

Gate set: alle 16 gates van `CLAUDE.md`, groen op de eindstand van de branch.

## Result (2026-10-17)

`9522550a` zet de naamregel in `CLAUDE.md` en voegt `scripts/lib/naming/glossary.tsv` toe. Er verandert geen code; de
branch blijft een voorstel tot de eigenaar merget.

- **`CLAUDE.md`, Conventions:** de eerste bullet is vervangen door de vijf regels van punt 1; de tweede regel (de
  draad) heeft één zin die naar de woordenlijst verwijst. Verder is `CLAUDE.md` gelijk.
- **De woordenlijst:** 1.061 regels, 986 Nederlandse ingangen (946 woorden, 40 woordgroepen), gesorteerd op `dutch`
  in bytevolgorde (`LC_ALL=C sort`), UTF-8, zonder tabs of aanhalingstekens in een veld. 68 ingangen hebben meer dan
  één regel (`tak`, `fout`, `lezing`, `voorwaarde`, `regel`).
  - `english` is één woord in kleine letters, of lowerCamel voor een term van meer woorden (`startPoint`,
    `emptyFallback`); de schrijfwijze in een identifier volgt de taal.
  - `context` is leeg bij één betekenis. 145 regels hebben `context` = `function word` of `function word: …`: de
    functiewoorden van de testzinnen, die de controle van punt 4 van `hernoemgereedschap` aan beide kanten kan
    weglaten.
  - `note` begint met de vlaggen, gescheiden door "; ": `keyword, decided at review` (4 regels) en `false friend`
    (137 regels, 125 woorden). Daarna volgt bij `spec` de paragraaf, bij `wire` de draadnaam als die niet letterlijk
    de Engelse term is, of een korte toelichting.
- **Punt 2 van deze kaart** heeft één bijzin over `plain`.

Meting (acceptance b), met `extract_words.py` uit de werkmap van de hoofdsessie op de worktree; buiten `backlog/`
is die gelijk aan `9159c9bd`, en de 1.031 kandidaten zijn dezelfde:

- Regels per `source`: `wire` 406, `spec` 129, `field` 14, `plain` 512.
- Dekking, geteld per woord als het aantal identifiers waarin het staat: de woorden in de lijst 25.722 van de
  32.879 voor alle woorden die ik Nederlands vind, **78,2%**. Zonder de domeinwoorden (414 woorden, 1.060) is het
  80,8%. Van de Nederlandse woorden in vijf of meer identifiers dekt de lijst 92,6%; de rest is `van`, `op`, `of` en
  `die` (1.659, de uitzondering van punt 2) en 43 domeinwoorden (371, zoals `jutter`, `vondst`, `schuit`).
- Nederlands telde ik: de woorden met een regel, de domeinwoorden, `van`, `op`, `of` en `die`, en van de 6.085
  woorden buiten de kandidaten (92 staan nu in de lijst) nog 3.224 andere (5.127). Die laatste groep is ingedeeld op
  frequentie in de Nederlandse proza van de repository (`backlog/`, de Jutterswet) tegenover de Engelse (spec,
  `NOTE.md`, README's), op samenstelling met een woord van de lijst, en de rest met de hand; een fout daar verschuift
  de dekking met hooguit enkele procentpunten.
  `in`, `is` en `per` telde ik niet: ze betekenen in beide talen hetzelfde; `min` evenmin, dat is bijna overal het
  Engelse `min`. Afkortingen telde ik niet als woord.

Voorstel voor de sleutelwoorden (in de lijst met `keyword, decided at review`; tellingen als heel woord, zonder
onderscheid in hoofdletters):

- `leeg` (RUNTIME-SEMANTICS 551×, IR-SPEC 107×) → `empty`. De draad zegt `empty` (`Predicate.kind`) en
  `emptyFallback`; het proza van de spec zegt "empty" (240× en 155×). `leeg` staat vooral in RegelSpraak-notatie
  (`leeg ÷ 5 = 0`) en in termen als "the leeg-barrier".
- `waar` (116×, 3×) → `true`. De draad schrijft de booleaanse literal als `{"boolean": true}`; het proza zegt "true"
  (76×, 77×). `waar` is ook het functiewoord "where", met een eigen regel.
- `onwaar` (97×, 4×) → `false`, om dezelfde reden: `{"boolean": false}`; "false" 55× en 36×.
- `gedurende` (31×, 24×) → `during`. De draad heeft er geen naam voor: de voorwaarde staat in `filter` (bij
  `timeProportionalPart`, `dayCount` en het temporele totaal) en "gedurende het gehele jaar" in `wholePeriod`. De spec
  houdt het sleutelwoord in haar proza ("the gedurende clip", "a gedurende condition"); "during" staat er 1× en 1×.
  Een identifier die het construct zelf noemt, kan het woord van de spec nemen: `clip` (RUNTIME-SEMANTICS §3.10).

De tien meest gebruikte afkortingen (niet in de lijst; aantal identifiers):

| afkorting | identifiers | betekenis |
|---|---|---|
| `tms` | 142 | truth maintenance system |
| `drl` | 75 | Drools Rule Language |
| `attr` | 52 | attribute |
| `func` | 41 | function, in grammaticaregels als `somFunc` |
| `synth` | 35 | synthesized, de goldens `synth-*` |
| `pf` | 28 | parameter fail-fast, de testframes van kaart `clingo-parameter-failfast` |
| `kv` | 27 | kwantorvouw (quantifier fold), testframes |
| `dt` | 26 | datatype; in de kalendercode van clingo ook date term |
| `tl` | 20 | timeline |
| `ot`, `decl`, `cb` | 19 elk | objecttype; declaration; closed body (gesloten body, testframes in `test_clingo.ml`) |

Anders dan de kaart, en waarom:

1. `source` heeft een vierde waarde, `plain` (512 regels): een gewone vertaling zonder bron in draad, spec of
   vakgebied. Punt 2 zegt dat nu in één bijzin.
2. Onder de uitzondering "ook gewone Engelse code" vallen ook `of` (`valueOf`, `yearOf`), `min` (`EXPECTED_MIN_*`)
   en `per`. Waar zo'n woord in een draadnaam staat, heeft de woordgroep een eigen regel (`kleiner of gelijk` →
   `lessOrEqual`, `percentage van` → `percentageOf`).
3. Woordgroepen (40) voor paren waarvan de vertaling per woord de draadnaam mist: behalve de `klasse: nl`-paren
   (`leegwaarde`, `bron annotaties` → `statuteAnnotations`) ook de waardefamilies van de naamtabel (`naar beneden` →
   `floor`, `gelijke delen` → `equalShares`), want die waarden staan als constanten in de code.
4. Functiewoorden staan erin zonder drempel van vijf identifiers (de kaart noemt voor die groep geen drempel), zodat
   de testzinnen gedekt zijn.
5. `false friend` is mechanisch gezet, met de woordenboektoets van het extractiescript, bij elk woord van één woord
   dat dat woordenboek kent. Ik doorzocht ook de woordenboekwoorden van vijf of meer identifiers die het script buiten
   de kandidaten liet (het eiste drie letters en 70% identifiers met een niet-Engels woord); Nederlands zijn onder
   meer `de` (1.399), `en` (441), `pad` (70), `na` (69), `kop` (36), `tot` (33) en `stand` (19).
6. Voorrang `wire` > `spec` > `field` > `plain`: `fixpunt` → `fixpoint` kreeg `spec` (RUNTIME-SEMANTICS §5.3) en
   `vouw` → `fold` kreeg `wire`, al noemde de opdracht ze als voorbeeld van `field`.
7. `tak` in een IfThenElse → `arm` (`field`), zoals de opdracht voorstelde en zoals de commentaren van de Drools- en
   clingo-tests al zeggen ("the dan-arm", "de niet-betreden arm"); RUNTIME-SEMANTICS §10.6 schrijft "branch". Zo
   blijft `branch` voor de twee weergaven van de Drools-transpiler (RUNTIME-SEMANTICS §3.12) en voor de selecties van
   een `InstanceSetUnion` (`InstanceSetBranch`).

Open punten:

- `van`, `op`, `of` en `die` blijven buiten de lijst en zijn in 1.659 identifiers Nederlands. De controle van
  `hernoemgereedschap` (punt 4, `labelVanBean` → `beanLabel`) moet ze als functiewoord kennen: met een vaste lijst in
  het script, of met een regel in de woordenlijst die een vlag draagt. Dat beslist die kaart.
- De vier sleutelwoorden: besluit bij de review.
- Een `spec`-regel noemt één paragraaf, die waar de term het meest staat of de kop is; andere paragrafen gebruiken
  hem ook.
- Werkwoorden kregen meestal `plain`, ook als het zelfstandig naamwoord een draad- of specterm is (`leest`,
  `schrijft`); uitzonderingen zijn de weigerwerkwoorden (`spec`) en de Drools-werkwoorden (`vuurt`, `field`).
  Afgeleide vormen gelden als `wire` (`annotation` bij `statuteAnnotations`, `bind` bij `binder`, `valid` bij
  `validity`), net als `true` en `false` voor de booleaanse literal.
- De meting telt identifiers buiten strings en commentaar. De ASP-predicaten van de clingo-backend staan in
  OCaml-strings en zijn niet meegeteld; een telling van de 79 predicaatnamen in de strings van `backends/clingo/lib/`
  vond als Nederlandse woorden `heeft`, `gewicht`, `unie` en `rol`, alle in de lijst, en samenstellingen als
  `wschrijft`, `opvolgerbegin` en `rapportaantal`. Een deel van de namen krijgt een voorvoegsel via `%s` (`%sontv`);
  stap 5 van het programma meet ze in het ASP-programma zelf.
- Het zoeken op woorden van de lijst van vier letters of meer binnen een woord dat het woordenboek niet kent (punt 1
  van `hernoemgereedschap`) gaf onder de gemeten woorden vier valse treffers: `callee` (`alle`), `overprecision`
  (`over`), `iterator` (`rato`), `standalone` (`stand`).
- `sonde` → `probe` is de letterlijke vertaling van een beeld; `hernoemgereedschap` kiest daar de betere naam.
  `Breuk` in de productieruntime blijft (gate 1); elders `breuk` → `rational`.
- Gate 1 (`scripts/check-self-contained.sh`) is groen op `9522550a`; de volledige gateset draait de hoofdsessie.

Gate-run van de hoofdsessie op `bea2242d` (2026-10-17 17:52–18:12): alle 16 gates groen. Duur per gate: 1 2 s,
2 25 s, 3 13 s, 4 1 s, 5 7 s, 6 198 s, 7 4 s, 8 31 s, 9 3 s, 10 33 s, 11 51 s, 12 704 s, 13 127 s, 14 1 s, 15 12 s,
16 10 s.
