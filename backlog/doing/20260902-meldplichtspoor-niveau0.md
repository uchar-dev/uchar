# Niveau 0: de meldplicht als gevouwen positiewaarde (nul IR-wijziging)

Goal: Eén nieuwe golden, `corpus/golden/jutterswet-meldplichtspoor/`, die
met UITSLUITEND gelande constructen een normatieve positie draagt en over
een gebeurtenisreeks bijwerkt: een keuzetype `meldstatus` (`Openstaand`
met de resterende termijn als getallading, `Vervuld`, `Geschonden`) als
accumulator van een `Fold` over een lijstinvoer van gebeurteniswaarden, met
`Match` op de accumulator en `IfThenElse`/`Let` in het lichaam, geschreven
naar een somtype-getypeerd attribuut op de Jutter. Het doel is BEWIJS, niet
vorm: `TOEKOMST_hohfeld_deontiek_20260822.md` §10 stelt niveau 0 voor als
stresstest die met bewijsmateriaal — niet met architectuurintuïtie —
bepaalt welke knopen van §5 hun plaats verdienen, en deze kaart maakt dat
uitvoerbaar zonder één IR-wijziging. Twee dingen komen er hard uit. (a) Of
de gelande som-, record- en foldkern een positie werkelijk draagt door BEIDE
executors: de samengestelde vorm — een `Fold` met SOMTYPE-accumulator over
een LIJSTINVOER — draait vandaag NERGENS in het corpus, terwijl WF-065 een
gedeclareerd `SumType` als lijstelement toelaat en WF-070 een
`SumType`-accumulator uitdrukkelijk toelaatbaar verklaart; de twee helften
draaien alleen los (somtype-accumulator over een objectattribuut in
`jutterswet-penningstaffel-catala` en `jutterswet-kamerhuishouden-catala`;
numerieke accumulator over een lijstparameter in `jutterswet-kinderdeler`
en `anchor/jutterswet-staffelvergoeding`). Die lacune sluiten heeft
corpuswaarde los van de deontische vraag. (b) WAAR de vorm breekt: de
verwachting is dat hij breekt op IDENTITEIT, want WF-060 laat als
somtype-lading alleen een getal, een dagpreciezige datum of een
gedeclareerd somtype toe (tekst is al negatief-gepind door
`corpus/negatief/wf060-text-payload`) en WF-071 kent voor structvelden
dezelfde beperking plus structs — houder en wederpartij kunnen dus niet ín
de waarde zitten. De houder is daarom het regelsubject en de wederpartij
per regel vast. Die grens wordt in de NOTE vastgelegd als het scherpste
argument dat een latere kernuitbreiding moet beantwoorden. De kaart
levert géén IR-uitbreiding, géén WF-nummer en géén spec-tekst; zij levert
een golden, hand-gerekende absolute verwachtingen, en een eerlijk
opgetekende grens.

Acceptance:
- De golden bestaat als `corpus/golden/jutterswet-meldplichtspoor/` met
  `ir.json`, ten minste drie `case*.json` (op tijd gemeld → `Vervuld`;
  te laat of niet gemeld → `Geschonden`; lege gebeurtenisreeks →
  `Openstaand` met de volle termijn) en een `NOTE.md` in de vorm van
  `corpus/golden/jutterswet-kinderdeler/NOTE.md`: hand-gerekende absolute
  waarden per casus, de gate-verwijzingen, en de opgetekende grens uit (b).
  De NOTE draagt GEEN herkomst — geen externe taal- of projectnamen, geen
  paden buiten de repo (gate 1; corpusregel "code en data dragen geen
  herkomst, git-historie en het plan doen dat").
- Elke casus pint de einduitkomst ABSOLUUT als
  `attributeValue` met een `constructor`/`payload`-waarde — de vorm die
  `anchor/jutterswet-beschikkingsom` al door beide executors speelt — plus
  `consistent`.
- ROOD-EERST waar van toepassing: als de samengestelde vorm in `ref` of
  clingo op een bestaande weigering stuit (de S19-inventaris, bijvoorbeeld
  de gesloten-lichaam-grammatica of een lijstbron), toon die weigering
  eerst, en laat haar daarna groen worden of — als dragen buiten deze kaart
  valt — laat de golden NIET landen en lever in plaats daarvan de
  opgetekende weigering plus een vervolgkaart. Een golden die maar door één
  executor speelt, landt niet: dat is de gate-regel, geen onderhandeling.
- `scripts/check-oracle.sh` (gate 5) en `scripts/check-clingo.sh` (gate 6)
  spelen alle nieuwe casussen met exact deze verwachtingen — ref ≡ clingo.
- `scripts/check-noop.sh` (gate 7) faalt op elke nieuwe casus (de
  verwachtingen gelden niet op de rauwe invoer). Als een casus de no-op
  overleeft, is de casus verkeerd gesneden — herzien, NIET aan de
  survivorlijst toevoegen (`scripts/` is protected).
- `scripts/check-drools.sh` (gate 12) blijft groen: een lijstinvoer valt
  onder de bestaande per-vorm-autoweigering, die haar reden gegroepeerd in
  het gate-rapport afdrukt. Toon dat rapport; voeg NIETS toe aan de
  waarde-divergentielijst in het script.
- Alle twaalf gates groen, met getoonde uitvoer:
  `scripts/check-self-contained.sh`, `check-ir-schema.sh`,
  `check-verifier.sh`, `check-case-schema.sh`, `check-oracle.sh`,
  `check-clingo.sh`, `check-noop.sh`, `check-rood.sh`, `check-catala.sh`,
  `check-regelspraak.sh`, `check-toka-parity.sh`, `check-drools.sh`.
  Gates 9 en 10 zijn per constructie ongemoeid: de golden draagt geen
  `bron.catala_en` en geen `bron.rgs`.
- Het `Result:`-briefje bij het verplaatsen naar `done/` benoemt expliciet:
  welke vorm speelde, welke grens is geraakt, en welke van de vier posten
  uit `TOEKOMST_hohfeld_deontiek_20260822.md` §5 door dit bewijs
  waarschijnlijker respectievelijk onwaarschijnlijker is geworden.

May touch: `corpus/golden/jutterswet-meldplichtspoor/**` (nieuw),
`backlog/`.

Out of scope: elke IR-, schema-, verifier- of spec-wijziging (`spec/**` is
protected en er is hier geen nieuwe WF-regel nodig); `corpus/golden/anchor/`
(protected — promotie tot anchor is een maintainersbesluit, niet deze
kaart); `corpus/negatief/` en `corpus/rood/` (de identiteitsgrens is al
gepind door `wf060-text-payload`; nieuwe negatieven horen bij een nieuwe
WF-regel); `scripts/**` (protected — geen survivor- of exclusielijst
aanpassen); elke handelings- of positieknoop, elk beëindigingseffect, elke
frameregel en elke spoorindex in het casusformaat (dat is rij 32/37-werk en
vraagt een fase, geen kaart); en elke verwijzing naar externe normtalen in
corpusdata.

## Bouwnotitie

**Uitkomst: de golden is GELAND.** Geen enkele weigering onderweg: de
samengestelde vorm speelt door beide executors, met nul IR-, schema-,
verifier- of spec-wijziging. Het rood-eerst-protocol is niet geactiveerd,
want er was geen rood: de verifier was schoon in één keer en beide
executors gaven meteen de hand-gerekende waarden.

### De gebouwde vorm

`corpus/golden/jutterswet-meldplichtspoor/` — twee keuzetype-domeinen
(`meldstatus`: `Openstaand(dagen)`/`Vervuld`/`Geschonden`; `gebeurtenis`:
`Dagverloop(dagen)`/`Melding`), één lijstparameter `meldreeks` van
gebeurteniswaarden (WF-065's gedeclareerde `SumType` als lijstelement —
de smalste vorm die het casusschema draagt, en hij DRAAGT hem: geen
hervorm naar getallen nodig), en één BEREKENING naar het somtype-attribuut
`Jutter.meldpositie`. De waarde is een `Fold` met somtype-accumulator
(WF-070), lambda-lichaam = `Match` op de accumulator met geneste `Match`
op het element en `IfThenElse` daaronder. `Vervuld` en `Geschonden` zijn
absorberend. Init = `Openstaand(30)`.

Géén `Let`: die zit hier niet in het lichaam omdat er niets te delen valt
(de vorm is al minimaal). De ASP-kant draagt een `Let` binnen een gesloten
lichaam trouwens wél (getuige `jutterswet-kinderlet`); wat zij weigert is
een binder die BUITEN het gesloten lichaam gebonden is — die grens is hier
niet geraakt.

### Handrekening (vooraf, met de hand, termijn = 30)

| casus | reeks | verloop | verwacht |
|---|---|---|---|
| case1 op tijd gemeld | `[Dagverloop(12), Melding]` | `Openstaand(30)` → 12≤30 → `Openstaand(18)` → melding | **`Vervuld`** |
| case2 te laat gemeld | `[Dagverloop(20), Dagverloop(14), Melding]` | `Openstaand(30)` → 20≤30 → `Openstaand(10)` → 14≤10 onwaar → `Geschonden` → absorberend | **`Geschonden`** |
| case3 lege reeks | `[]` | lijst aanwezig, geen elementen → basis = init | **`Openstaand(30)`** |
| case4 niet gemeld | `[Dagverloop(31)]` | 31≤30 onwaar | **`Geschonden`** |

### Metingen per executor (echte uitvoer)

| casus | verwacht | ref (gate 5) | clingo (gate 6) | Drools (gate 12) |
|---|---|---|---|---|
| case1 | `Vervuld` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case2 | `Geschonden` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case3 | `Openstaand(30)` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |
| case4 | `Geschonden` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | per-vorm autoweigering |

Drools, letterlijk: `EXCLUDE loader: form not carried by the v0 Drools
backend: Datatype:SumType (unknown datatype node)` — gegroepeerd in het
gate-rapport (de `Datatype:SumType`-groep ging van 5x naar 6x). Niets
toegevoegd aan de waarde-divergentielijst; `scripts/` ongemoeid.

Alle twaalf gates groen, in deze checkout gedraaid:
gate 1 `check-self-contained: PASS`;
gate 2 `check-ir-schema: PASS (schema valid; 169 file(s) validate and round-trip)`;
gate 3 `check-verifier: PASS (169 golden clean; 206 negatief rejected for their declared rule)`;
gate 4 `check-case-schema: PASS (schema valid; 411 case file(s) validate)`;
gate 5 `check-oracle: PASS (389 cases replayed exactly; …)`;
gate 6 `check-clingo: PASS (389 cases replay exactly through clingo — ref = clingo over the corpus)`;
gate 7 `check-noop: PASS (353 cases refute a do-nothing executor; 36 pinned survivors …)` — de vier nieuwe casussen falen alle vier onder `--noop` (exit 1), geen survivor-regel nodig;
gate 8 `check-rood: PASS (22 red cases fault observably in ref and clingo)`;
gate 9 `check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) …`;
gate 10 `check-regelspraak: PASS — JUnit suite green (159 tests); 26 rgs golden(s) …`;
gate 11 `check-toka-parity: PASS`;
gate 12 `check-drools: PASS — played 104, auto-excluded 41, value-divergence 1, JUnit 153 tests`.

### De geraakte grens

Zoals voorspeld in (b): **IDENTITEIT**, en niets anders. De hele
toestandsmachine — open positie met termijn, aftellen, kwijting,
schending, absorberende eindtoestanden, de lege reeks als basis — draagt
de gelande kern zonder één uitbreiding, exact, door beide executors. Wat
zij níet draagt is WIE. WF-060 laat als somtype-lading alleen een getal,
een dagpreciezige datum of een gedeclareerd somtype toe, en WF-071 voor
structvelden diezelfde verzameling plus structs; geen van beide laat een
objectverwijzing toe. De partijen van een verplichting kunnen dus niet ín
de positiewaarde zitten. Daarom is hier de houder het REGELSUBJECT (de
Jutter waaraan het attribuut hangt) en de wederpartij per regel vast,
nergens in de waarde genoemd. De grens toont zich hier een tweede keer
van de andere kant: de gebeurtenisreeks is een documentparameter, dus de
positie is per subject geïndexeerd terwijl haar bewijs helemaal niet
geïndexeerd is. Onuitdrukbaar blijft: één verplichting per partijenpaar,
meerdere zulke verplichtingen tegelijk op één houder, elk met een eigen
termijn.

### Wat dit bewijs doet met de vier posten van §5

- **`PositionType` — WAARSCHIJNLIJKER**, en van de vier het enige dat dit
  bewijs echt draagt. Niet om zijn `conduct`/`deadline`-velden (die deed
  de fold met de hand), maar om `holder`/`counterparty`: precies wat de
  ladingsomheining blokkeert.
- **`ActType` — GESPLITST.** Zijn `actor`/`recipient`-helft wordt
  waarschijnlijker om dezelfde identiteitsreden (de reeks kon niet per
  partij worden geïndexeerd); zijn `effects`-helft
  (`initiates`/`terminates`) wordt ONWAARSCHIJNLIJKER: de absorberende
  armen deden dat hier met bestaande knopen.
- **Schending als gedefinieerde semantiek — ONWAARSCHIJNLIJKER als
  KNOOP.** De schendingstoestand is hier gewoon een constructorwaarde die
  beide executors afleiden en die een casus absoluut pint. Wat overblijft
  is hoogstens één normatieve clausule met WF-nummer over bestaande
  vormen, geen nieuwe knoop.
- **Reparatie — ONWAARSCHIJNLIJKER als apart mechanisme.** Zij was al "een
  creatieregel die het schendingsfeit leest", en dat lezen is een gewone
  somtype-attribuutlezing; die vorm speelt aantoonbaar.
