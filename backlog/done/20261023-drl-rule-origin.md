# drl-rule-origin: de Drools-backend zegt per regel van welke vorm en welke default hij is

*Uit de toolchain-kaart `drl-rule-provenance` (werkbank, 2026-10-23; toolchain `backlog/new/20261023-drl-rule-provenance.md`).
De toolchain-sessie besliste daar dat de herkomst uit uchar komt. Klein; landt vóór de toolchain-kaart, die erna de
pin bumpt.*

## Aanleiding

De werkbank toont per variant van een defaults-groep haar verlaagde DRL (tak `DRL_TEXT`) en zoekt die regels nu op naam.
De naam van een regel van een stapel is `<stapel>: default <label>` of `<stapel>: bijdrage aan vouw <n>`
(`TmsTranspiler`, r. ±3044). Het label is de eigen nummering van de lowering, en een bijdrage aan een vouw noemt de
default niet waarvoor zij bijdraagt. `TmsTranspiler.formOf(ruleName)` geeft de vorm van een regel, maar niet welke
default van de groep. Gemeten op v2 (de werkbank): de groep `herleving art. 21 lid 1` wordt `default 0` tot en met
`default 4`, de uitzondering `nieuw recht boven de herlevingsgrens art. 21 lid 2 onder a` is `default 2`, en de stapel
heeft daarnaast `bijdrage aan vouw 16` tot en met `22`. Bij een stapel over subtypen (RUNTIME-SEMANTICS ch. 7.10) en bij
geneste uitzonderingen is de nummering niet uit het IR af te leiden zonder die van de lowering na te bouwen. De
toolchain heeft geen semantiek van zichzelf en bouwt haar niet na.

## Doel

- Een publieke ingang naast `formOf` (bijvoorbeeld `originOf(String ruleName)` op `TmsTranspiler`, bereikbaar via
  `DroolsBackend.Build`), die per regel van het model zegt waarvan hij een regel is:
  - de vorm van het document, zoals `formOf` (de naam van een regelvorm of van een defaults-groep), of geen voor een regel
    van geen vorm;
  - waar de regel bij een default hoort: die default, als JSON Pointer in de vorm die `decisionsOf` al gebruikt (kaart
    `defaults-decider`), zodat de toolchain dezelfde pointer krijgt als voor de beslissing; en waar een gewone regel in
    een stapel beslist (ch. 7.10), die regelvorm;
  - dat ook voor de hulpregels van een default of regel (een bijdrage aan een vouw in haar voorwaarde of waarde, een
    lezingregel, een doorgeefregel): de default of regel waarvoor zij bestaan.
  De bouwer kiest de vorm van het antwoord en zegt waarom.
- Geen gedragsverandering: de DRL-tekst en het exec-model zijn bytegelijk (`BranchDumpsCorpusWitnessTest`
  ongewijzigd); de ingang leest wat het plan al weet (`Node`, de pointer van de default).

## Acceptatie

- (a) Een getuige over het corpus: voor elke golden die de backend draagt heeft elke regel van het model een antwoord;
  elke regel waarvan de vorm een defaults-groep is, wijst een default aan die in het document bestaat (de pointer lost
  op), behalve wat de stapel zelf draagt zonder default (dat zegt het antwoord expliciet); elke default van een gedragen
  groep heeft ten minste één regel.
- (b) Gericht: een vlakke groep, een geneste uitzondering, een stapel over een subtype met een gewone regel erin, en een
  default met een vouw in zijn voorwaarde: de bijdragen aan die vouw wijzen naar die default.
- (c) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

## Result (2026-10-23)

Tak `card/drl-rule-origin`, vanaf `a68dd644`. Geen beschermd pad geraakt; geen delta voor `spec/CONSTRUCTS.md` of
`spec/PLAN.md` (geen spec-wijziging, een ingang van de backend).

- `1c0329a0` de ingang `originOf` en de twee getuigen.
- dit Result, in het commit na `1c0329a0`.

### De ingang

`TmsTranspiler.originOf(String ruleName)` → `TmsTranspiler.RuleOrigin`, naast `formOf`. Bereikbaar via de build:
`build.execModelTranspiler().originOf(name)`; de `Build` van beide takken draagt dezelfde analyse, en zo leest de
toolchain nu al `generated()` (`Playground.java`).

```java
public record RuleOrigin(String form, Kind kind, String pointer) {
    public enum Kind { NO_FORM, FORM, DEFAULT, STACK }
}
```

- `NO_FORM`: een regel van geen vorm (een stand, een koppeling uit de invoer, de vergelijking van twee schrijvers);
  `form` en `pointer` null.
- `FORM`: een regel van een vorm die geen effectieve stapel is; `form` zoals `formOf`, `pointer` null. Ook de
  bijdrageregels van een vouw die een gewone vorm als eerste registreerde, ook waar een stapel die die regel opnam
  dezelfde vouw leest (`jutterswet-dringende-melding`: `dringende melding: bijdrage aan vouw 0`).
- `DEFAULT`: een regel van een stapel die één default verlaagt; `form` de stapel (`defaults A:<type>.<lid>`),
  `pointer` de default zoals `decisionsOf` hem noemt: `/rules/prioritizedDefaults/i/defaults/j` met
  `/exceptions/members/m/defaults/l` per niveau, of voor een gewone regel die de stapel van een subtype opneemt
  (ch. 7.10) `/rules/rules/f/versions/v`. Bij een default horen: zijn eigen regel (`default <label>`), zijn terugval,
  de foutregel van zijn rechtvaardiging, de doorgeef-, verschil- en eensregels die zijn uitzonderingsgroepen verzoenen
  (ch. 7.9), de bijdrage-, filterfout-, lidfout- en filterdeponeringsregels van de vouwen in zijn rechtvaardiging of
  gevolg, en zijn leesregel.
- `STACK`: een regel van de stapel zelf, geen default; `form` de stapel, `pointer` null: de verzoening van de groepen
  aan de wortel (`doorgeven wortel`, `verschil wortel`, `eens wortel`, ch. 7.2) en `geen rekendatum` van een stapel
  met een gedateerde groep.

Waarom deze vorm:

- Eén record met een expliciete soort in plaats van een combinatie van nulls: "van de stapel zelf" is een waarde
  (`STACK`), geen ontbrekende pointer naast een vorm die toevallig een stapel is. De constructor weigert elke andere
  combinatie: een pointer precies bij `DEFAULT`, een vorm precies niet bij `NO_FORM`.
- De pointer in de vorm van `DefaultsDecision`: de toolchain vertaalt die vorm al (`Verbaliser`, `DEFAULT_POINTER` en
  `RULE_POINTER`), dus een beslissing en een DRL-regel krijgen dezelfde sleutel.
- Geen groepsnaam, variantnaam of positie in het antwoord: die staan in het document op de pointer, en de ingang
  herhaalt het IR niet.
- Geen aparte soort voor een gewone regel in een stapel: `DefaultsDecision` noemt die ook een default, met een
  regelversie als pointer; de vorm van de pointer onderscheidt ze, en de toolchain heeft daar al een patroon voor.
- Een leesregel (alleen in een bouw met lezingen) krijgt de herkomst van de regel die hij leest, de vorm
  inbegrepen; `formOf` geeft hem bewust null, zodat een waarnemer zijn vuren niet als vuren van de vorm telt. Dat is
  het enige verschil met `formOf`, en de corpusgetuige houdt het vast.
- Een naam van geen regel van het model wordt geweigerd (`IllegalArgumentException`), zoals door `formOf`. Een regel
  van een stapel waarvan de verlaging niets noteerde faalt luid (`IllegalStateException`), en `stackRules` weigert
  zo'n regel al bij de bouw: de regels van de stapel en de bijdrageregels die op dat moment onder de naam van de stapel
  staan (fixronde, zie Review).

Hoe: de verlaging noteert terwijl zij bouwt, nooit achteraf uit de naam. `ownRule` noteert zijn regels bij zijn
default en houdt die default in het veld `lowering` zolang hij bouwt; de registratie van de leesbestanden van een
stapel (waar de bijdrageregels van de vouwen worden ingeschreven) zet `lowering` per default; `Reads.recorder` noteert
elke bijdrageregel die het inschrijft bij `lowering`; de verzoening onder een default en die aan de wortel noteren
hun regels apart; `ReadingRule` kent de naam van de regel die hij leest. De pointer wordt pas bij de vraag berekend,
door dezelfde `pointerOf` als `decisionsOf`. Geen gedragsverandering: geen `RulePlan` verandert. Gemeten: de
DRL-tekst van alle 277 gedragen goldens, zonder en met lezingen (554 teksten), is bytegelijk aan die van `a68dd644`
(een probe in de werkmap, niet gecommit); het exec-model rendert hetzelfde plan.

### Getuigen

- (a) `RuleOriginCorpusWitnessTest`: elke golden die de backend draagt, op de tak `DRL_TEXT` (de tak waarvan de
  toolchain de tekst toont), zonder en met lezingen; per regel van de KieBase heeft `originOf` een antwoord, de vorm
  is die van `formOf` (een leesregel: de herkomst van de regel die hij leest), een regel van een stapel is `DEFAULT`
  of `STACK` en nooit `FORM`, en elke `DEFAULT`-pointer lost op in `ir.json` naar een `Default` van een groep op het
  lid van de stapel of naar een regelversie op dat lid; elke `Default` van elke groep, op elke diepte, is de default
  van ten minste één regel. Gemeten 2026-10-23, de vloeren: 277 goldens; zonder lezingen 2062 regels (1142 `FORM`,
  681 `NO_FORM`, 213 `DEFAULT`, 26 `STACK`), van de `DEFAULT`-regels 4 van een regelversie en 12 bijdrageregels; met
  lezingen 901 leesregels (176 van een default, 725 van een vorm); 160 defaults van de documenten, elk met een regel.
  Ongeveer 21 s.
- (b) `RuleOriginWitnessTest`, zeven tests (de zevende uit de fixronde, zie Review), per stapel de hele tabel in de
  volgorde van het model:
  - vlakke groep: `jutterswet-premiestaffel`, groep `premie` (de vierde van het document): `default 0` tot en met
    `default 2` → `/rules/prioritizedDefaults/3/defaults/0` tot en met `/2`, met op die pointers de groepsnaam
    `premie` en de varianten `premie bij een zware vangst`, `premie bij veel vondsten`, `premie`;
  - geneste uitzondering: `anchor/jutterswet-nestdiepte`, twee niveaus: `default 0.1.0.1.0` →
    `…/defaults/0/exceptions/members/1/defaults/0/exceptions/members/1/defaults/0`, `verschil 0.1.0`, `eens 0.1.0` en
    `terugval 0.1.0` → `…/defaults/0/exceptions/members/1/defaults/0`, `doorgeven 0` … `terugval 0` →
    `…/defaults/0`, `doorgeven wortel` en `eens wortel` → `STACK`;
  - stapel over een subtype met een gewone regel erin: `jutterswet-wrakbasis`, `defaults A:Wrakrecht.uitkering`:
    `default 0` → `/rules/prioritizedDefaults/0/defaults/0` (de groep op Strandrecht), `default 1` →
    `/rules/rules/0/versions/0` (`uitkering van een wrakrecht`), hun leesregels idem; de regel zelf blijft `FORM`;
  - default met een vouw in zijn voorwaarde: `jutterswet-premiestaffel`: `bijdrage aan vouw 0` (de som in de
    rechtvaardiging van `premie bij een zware vangst`) → `/3/defaults/0`, `vouw 1` (de telling) → `/3/defaults/1`,
    `vouw 2` en `vouw 3` (minimum en maximum van `lichte vondsten`) → `/rules/prioritizedDefaults/4/defaults/0`;
  - de stapel zelf: `jutterswet-uitsluiting-gedateerd`, `geen rekendatum` → `STACK`;
  - een regel van geen vorm → `NO_FORM`, een naam van geen regel geweigerd.
- Rood, met mutaties in de werkmap (niet gecommit): M1, zonder de notitie in `Reads.recorder`: de corpusgetuige 24
  verschillen (de 12 bijdrageregels van een default, in beide bouwen) en de tests van de vlakke groep en de vouw rood;
  M2, de verzoening van de uitzonderingsgroepen van een default als `STACK`: de test van de geneste uitzondering rood,
  en de corpusgetuige meldt drie defaults zonder regel (`anchor/jutterswet-nestuitzondering` `/0/defaults/1`,
  `jutterswet-meldtermijn-strandjutter` `/1/defaults/0`, `jutterswet-nestkenmerk-rgs` `/0/defaults/0`).

### Suite en gate 12

Op `1c0329a0`:

- de JUnit-suite van `backends/drools` (`mvn -B -o test`): 1661 tests, 0 fouten, 0 overgeslagen; de corpusgetuige
  ongeveer 12 s in de volle suite;
- gate 12 (`scripts/check-drools.sh`): PASS; 274 goldens gespeeld op de exec-model-tak en op de DRL-teksttak, elke
  statusregel gelijk; kanaalvloer 49 niet-gespeelde goldens bij naam; de suite van de gate 1661 tests (vloer 1573);
  `check-drools-codes` PASS (292 codes).

De volledige gate-set draait de hoofdsessie vóór de landing (acceptatie (c)).

### Review (fixronde, 2026-10-23)

Een review (alleen lezen) vond geen fout en vier punten. Alle vier zijn verwerkt, in het commit na `d85c132a`:

1. Gat in de getuigen. `conditionFaultRule` roept `reads.recorder()` zelf aan, binnen `ownRule`. Een vouw die de
   foutregel van een default als eerste inschrijft, geeft haar bijdrageregels daardoor alleen via `lowering = k.d()` in
   `ownRule` een default. Geen golden heeft dat pad, want in het corpus kan geen rechtvaardiging falen die een vouw leest,
   en zonder die regel bleef alles groen. Nieuw is
   `RuleOriginWitnessTest.the_fault_rule_of_a_justification_registers_its_folds_under_its_default`, met een document dat
   in de test wordt geschreven (`TmsDocuments.document`): de default "zijn aanmelddatum indien het aantal vondsten groter
   is dan 2 en zijn aanmelddatum plus zijn wachtdagen later is dan 2026-01-01". De test eist dat `default 0`,
   `default 0: fout in de voorwaarde` en `bijdrage aan vouw 0` naar `/rules/prioritizedDefaults/0/defaults/0` wijzen
   en `default 1` naar `/1`. Met mutaties in de werkmap (niet gecommit) werd de test rood:
   - M3 schrapt `lowering = k.d()`: de bouw faalt in `stackRules` met "de regel 'defaults A:Jutter.meldtermijn:
     bijdrage aan vouw 0' van de stapel … verlaagt geen default en niet de stapel zelf";
   - M3b schrapt daarnaast de uitbreiding van de controle (hieronder): `originOf` faalt in de test zelf met "… en de
     verlaging noteerde niet van welke default of dat hij van de stapel zelf is".

   Zonder mutatie is de test groen.
   Keuze bij de zin "`stackRules` weigert zo'n regel al bij de bouw": ik heb de controle uitgebreid en de zin niet
   aangepast. De controle kijkt nu ook naar de regels die op dat moment in `formOfRule` onder de naam van de stapel
   staan. Waarom: dan klopt de zin, en een ontbrekende notitie faalt bij elke bouw van een stapel, met de naam van de
   regel en van de stapel. Anders faalt zij pas wanneer iemand `originOf` voor die regel vraagt. De corpusgetuige vraagt
   dat alleen voor de goldens, de bouw ook voor elk document van de toolchain. Op dat moment staan onder de stapelnaam
   precies de bijdrageregels die de verlaging van deze stapel inschreef; de regels van de stapel zelf zet `meet` pas
   daarna in `formOfRule`.
2. De Javadoc van `originOf` zegt nu "een regel die de stapel toevoegt om een default te verlagen" en "de
   rechtvaardiging of het gevolg" (`jutterswet-sluitdag-gemeld` vouwt in het gevolg).
3. `": geen rekendatum"` staat nu één keer, in een lokale variabele van `stackValidityFault`.
4. De escapes in de kop van de DRL-tekst staan onder "Wat de toolchain ermee doet".

Geen gedragsverandering. Vergeleken met `d85c132a` is de DRL-tekst bytegelijk voor alle 277 gedragen goldens, zonder
en met lezingen (554 teksten; gemeten met een probe in de werkmap, niet gecommit). `BranchDumpsCorpusWitnessTest` is
ongewijzigd en groen. De suite (`mvn -B -o test`) draaide 1662 tests: 0 fouten, 0 overgeslagen. Gates zijn in de
fixronde niet gedraaid; die draait de hoofdsessie.

### Wat de toolchain ermee doet (kaart `drl-rule-provenance`)

Per regelnaam in `drl` (dezelfde namen als de KieBase van de build): `RuleOrigin o =
build.execModelTranspiler().originOf(name)`, en dan naar de sleutels van `GROUND`. `originOf` verwacht de naam zonder
escapes, zoals de KieBase hem heeft (`Rule.getName()`). De kop `rule "…"` in de DRL-tekst escapet de naam als
stringliteral (`Namen.drlString`, aangeroepen in `DrlRendering.java` r. ±399: `\` → `\\`, `"` → `\"`, CR → `\r`,
LF → `\n`); wie de naam uit de tekst leest, maakt die vier escapes eerst ongedaan, of neemt de namen uit de KieBase.

- `FORM`: `rule` = `RegelSpraakModel.formName(o.form())` (het versie-achtervoegsel `#k` eraf);
- `DEFAULT` met pointer `/rules/prioritizedDefaults/<i><rest>`: `group` = `rules.prioritizedDefaults[i].name`,
  `pointer` = `<rest>` (de pointer zonder `/rules/prioritizedDefaults/<i>`), `variant` = de `name` van de `Default` op
  de volle pointer waar die er een heeft, `position` = de laatste index van `<rest>` plus één;
- `DEFAULT` met pointer `/rules/rules/<f>/versions/<v>`: `rule` = `rules.rules[f].name`;
- `STACK`: van geen default en, bij een stapel uit groepen van meer typen, van geen enkele groep; de toolchain kiest of
  zij dan niets of de stapel noemt (`generated()` geeft het lid);
- `NO_FORM`: geen beslisser.

De DRL van een variant is de verzameling regels met `DEFAULT` en haar pointer; bij een default met uitzonderingsgroepen
horen daar de verzoeningsregels van die groepen bij (zij zetten de uitkomst van die default). Een regel van de
opgavemarkering is voor `originOf` een gewone vorm of stapel; wat haar maakte leest de toolchain zoals nu
(`generated` en `generatedFrom`).

### Landing (hoofdsessie, 2026-10-23)

Review door een verse Opus-agent (alleen lezen): geen fout; één gat in de getuigen en drie kleine punten, verwerkt in
`9f0bedf8` (zie "Review"). De volledige gate-set op `9f0bedf8` (`werk/gates.sh`): 17 van 17 groen in 31 minuten (12
804 s, 13 224 s, 17 410 s). Fast-forward op main en gepusht; daarna de toolchain-kaart `drl-rule-provenance` met de
pin-bump.
