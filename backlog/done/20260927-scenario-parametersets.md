# scenario-parametersets: een benoemde parameterset met jaargangen in het scenariodocument, gekozen door de peildatum

De oude toolchain kende parametersets als zijbestand naast een scenario of
een dienst-descriptor (`<naam>.parameters.json`: benoemde sets, per
parameter waarden met een geldigheid `vanaf`/`tot`, gevalideerd tegen het
opgeloste model; de dienst kiest per verzoek de jaargang aan de hand van de
peildatum, en een parameter die zowel in de descriptor-defaults als in een
set staat is een startfout). De dienst in uchar-dev/regelspraak-toolchain
draagt dat nog (`ParametersetLader`, `BeslisserKern`,
`ParametersetServiceTest`); de scenarioroute verloor het bij de
verhuizing, omdat uchars scenariolaag elke gelezen parameter in de situatie
eist en van een JSON-zijbestand niets weet (benoemd verlies in de README
van de distributie, 2026-09-26). Het idee is goed en hoort spec-eerst hier
(maintainer, 2026-09-27): bedragen en percentages veranderen per jaar, los
van de regels, en een scenario moet "in 2025" en "in 2026" tegen dezelfde
regels kunnen draaien. Herkomst: ALEF's testsets dragen parameterwaarden
als testdata, niet als regeltekst.

Wat uchar al heeft (`frontends/regelspraak/grammar/DIVERGENCES.md`,
"Scenariotoetsen"): een situatie draagt parameterwaarden, met een periode
`van <datum> tot <datum>` alleen voor een tijdafhankelijke parameter; een
situatie kan `gebaseerd op de situatie <naam>` zijn; een scenario heeft
`met peildatum <datum>`; het caseformaat draagt per parameter een waarde of
een hele tijdlijn (`spec/schema/uchar-case-v0.schema.json`, `parameters`).
Een parameter zonder granulariteit heeft in de IR één waarde; de jaargang
is dus geen IR-vraag maar een vraag van de lowering: welke waarde het
casebestand krijgt.

## U-besluiten (hoofdsessie, 2026-09-27)

- U1 Oppervlak: een documentblok `Parameterset <naam>` naast de
  situatie-definities, met regels in de vorm van de situatie
  (`de parameter <naam> is <waarde>`, optioneel `van <datum> tot <datum>`;
  meerdere regels voor dezelfde parameter zijn jaargangen, hun perioden
  mogen elkaar niet overlappen). Een scenario kiest een set met
  `met parameterset <naam>` (of een situatie met `gebaseerd op de
  parameterset <naam>` — kies bij het oppakken één van de twee en leg de
  reden vast; de grammatica is de uchar-grammatica, de extensie staat in
  DIVERGENCES).
- U2 Lowering: voor een parameter zonder granulariteit schrijft de lowering
  de waarde waarvan de periode de peildatum van het scenario bevat (de
  jaargang; precies één, anders een weigering in de zin van de resolver);
  voor een tijdafhankelijke parameter de hele tijdlijn, zoals de situatie
  dat nu al doet. Een scenario zonder peildatum dat een parameter met
  jaargangen leest, wordt geweigerd met een zin die de parameter en het
  scenario noemt. De IR verandert niet.
- U3 Conflictregel (als de dienst): een parameter die zowel in de gekozen
  set als in de situatie van het scenario een waarde heeft, is een
  weigering — nooit "de laatste wint". Een parameter die in geen van beide
  staat, blijft de bestaande weigering van de parameterpoort; daarmee heeft
  het open punt "a scenario has no surface for an absent parameter" (PLAN
  §9) een oppervlak: de set.
- U4 Getuigen: een Jutterswet-golden met `scenario.rgs` dat één set met
  twee jaargangen draagt en twee scenario's met verschillende peildatum,
  waarvan de verwachte uitkomsten verschillen door de jaargang alleen;
  gate 16 regenereert de casebestanden byte-gelijk; gates 5, 6 en 12 spelen
  ze (scalaire parameters dragen alle drie de uitvoerders). Negatieve
  getuigen in de frontendsuite: overlappende jaargangen, geen jaargang op
  de peildatum, conflict met de situatie, scenario zonder peildatum.
- U5 Register: CONSTRUCTS-rij van de scenariotoetsen bijwerken (voorstel in
  het Result; de hoofdsessie landt hem), DIVERGENCES "Scenariotoetsen"
  uitgebreid, PLAN §9 het open punt afgesloten.
- U6 Niet in deze kaart: de dienst van de distributie (die leest de set
  later uit hetzelfde document in plaats van uit JSON — een kaart daar,
  alleen benoemen), tijdlijnen voor een parameter zonder granulariteit in
  de IR, en de Drools-weigering van tijdgranulaire parameterinvoer (gate
  12, bekend).

## Acceptatie

U1 tot en met U4 met de getuigen; alle zestien gates groen; het Result
noemt de gekozen syntaxis met reden, de weigeringszinnen letterlijk uit
echte uitvoer, en de tellingen van gate 16.

Mag raken: `frontends/regelspraak/` (resolver, `ScenarioResolver`,
`CaseEmitter`, grammatica als de extensie dat vraagt, DIVERGENCES, tests),
`corpus/golden/` (nieuwe Jutterswet-golden), `scripts/check-scenario.sh`
alleen voor een vloer. Niet: `spec/IR-SPEC.md`, `spec/schema/`, de
uitvoerders, `toka/`.

## Result (2026-09-27, bouwer)

**Stand.** Kop vóór deze Result-commit: `23d6d4a0`; werkboom schoon; de kaart blijft in `doing/`.
Commits: `62e7b8e7` claim; `b9b0c940` grammatica, resolver, suite; `f0cd6f0d` golden en vloeren gate
16; `fa5c3f74` DIVERGENCES; `f9eb6fcf` gate 12-pinnen (voorstel op een beschermd pad, apart gehouden);
`24012395` tokenreden gemeten en getuigd; `23d6d4a0` onvolledige periode.

**U1, de syntaxis (door mij gekozen).** Een blok `Parameterset <naam>:` met de parameterregels van een
situatie (`de parameter <p> is <waarde>`, optioneel `van <datum> tot <datum>`), en de keuze in de
scenariokop: `met parameterset <naam>`, na `met peildatum` en vóór `gebaseerd op de situatie`, ten
hoogste één set per scenario. Niet gekozen: `gebaseerd op de parameterset` in een situatie. Redenen
(eigen afweging): (1) U3 houdt set en situatie uit elkaar en weigert een conflict; in een situatie zou de
set meedoen in de samenvoeging, waar een latere parameterwaarde de eerdere vervangt, en dat is wat U3
uitsluit; (2) de peildatum kiest de jaargang en staat in dezelfde kop, een situatie heeft geen
peildatum; (3) de dienst noemt zijn set in de descriptor en krijgt situatie en peildatum per verzoek,
dezelfde verdeling; (4) een gedeelde situatie blijft alleen wereldgegevens. Het blok hangt alleen af
van de declaraties.
Twee nieuwe tokens: `PARAMETERSET` (`Parameterset`) en `MET_PARAMETERSET` (`met parameterset`, een
frasetoken zoals `MET_PEILDATUM`). Gemeten reden: een ander hoofdletterwoord op de plek van de kop werd
na een kenmerknaam in die naam opgenomen (boom: `kaleNaam … strandrecht Jaarset tarieven`) en na een
getal zonder eenheid als eenheid gelezen (`getalWaarde 3 (eenheidExpressie … Jaarset)`); `met` +
`LOWER_ID` loopt door in `scenarioNaam` en `enkeleRegelReferentie` (grammatica). Botsingsmeting: het
woord `parameterset` (elke schrijfwijze) stond in 0 van de 104 rgs-bronnen buiten de pariteitszone en in
0 van de 198 rgs-bronnen van de externe regelsets; gate 11 PASS na de lexerwijziging.

**U2, jaargang en tijdlijn.** De regels van een parameter zonder granulariteit zijn jaargangen, van
inclusief tot exclusief; een regel zonder periode geldt op elke dag. Geen twee jaargangen van één
parameter delen een dag, in welke schrijfvolgorde ook. De peildatum kiest er precies één; geen
peildatum of geen jaargang die hem bevat is een weigering. Eigen besluit: dat geldt voor elke parameter
van de set, gelezen of niet, omdat het casebestand alle parameters van de set draagt en omdat de dienst
(`Parameterset.waardenOp`) elk gat weigert. Een parameter met tijdlijn gaat heel mee, met de
tijdlijnregels van een situatie (volgorde, geen overlap, raster, geen vaste waarde ernaast). De IR
verandert niet. De keuze gebeurt in de resolver, omdat de weigeringen ERRORs moeten zijn vóór er iets
geschreven wordt; de gekozen waarden openen de parameterlijst van het scenario (eerst de set, in de
volgorde van de eerste regel per parameter, dan de situatie). `CaseEmitter` bleef daardoor ongewijzigd
(eigen gevolgtrekking: `ResolvedScenario` draagt wat het casebestand draagt, dus de lowering schrijft
wat ze krijgt, en de scenariorunner van de distributie, die `CaseEmitter.emit` aanroept, krijgt sets
zonder eigen wijziging).

**U3, conflict en afwezige parameter.** Een parameter die set en situatie allebei geven is een
weigering. De parameterpoort blijft; bij een gekozen set noemt haar zin ook de set, en na een onbekende
set zwijgt ze (wat de set zou geven valt niet te zeggen). De bewering van U3 dat het open punt "a
scenario has no surface for an absent parameter" met de set een oppervlak krijgt, klopt niet, en ik heb
geen vorm voor een lege waarde gebouwd. Gemeten: de enige gepinde case (`anchor/jutterswet-
staffelvergoeding/case3-afwezig.json`) laat een lijstparameter weg, en RegelSpraak kent geen
lijsttype; een weggelaten gelezen scalaire parameter faalt in ref (`EVALUATION FAULT: parameter
"penningbedrag" is unbound in this case (binding is fail-fast, ch. 5.7)`) en clingo (`UNSATISFIABLE —
a fail-fast guard fired: viol("parameter "penningbedrag" is unbound in this case (binding is
fail-fast, ch. 5.7)","J1")`), zoals rood `aanvoergeld-parameter-ongebonden` pint; Drools speelt
(`PLAYED 1`, bekend: gate 8 NOFAULT). De poort weigert dus wat de uitvoerders zouden weigeren; het
caseformaat heeft geen lege scalaire parameterwaarde. Het voorstel voor PLAN hieronder sluit het punt
met die reden.

**U4, getuigen.** Golden `corpus/golden/jutterswet-jaarvaststelling` (art. 7 Jutterswet, waarden uit
`versiegeschiedenis.md`: 2025 drempel 12, € 250; 2026 drempel 14, € 275; eigen aanvulling: fonds-
percentage 10 % / 8 % en uitbetaaldag 12-12-2025 / 11-12-2026). Eén set, twee jaargangen, twee
scenario's op dezelfde situatie met peildatum 31-12-2025 en 01-01-2026; de uitkomsten verschillen door
de jaargang alleen, en de grens `tot` is exclusief. `ir.json` = de productieroute (`cmp`); casebestanden
met de hand nagelezen. ref `oracle: PASS (9 expectations)` en clingo `clingo: PASS (9 expectations)`
per case, Drools exec `PLAYED 2`, DRL-tekst `PLAYED 2`, verifier `PASS`, no-op faalt (8 en 4
mismatches). Case 2 met de jaargang 2025 faalt in alle drie op 7 verwachtingen. Frontendsuite:
`ScenarioParametersetTest`, 22 tests (tokens en hun grenzen, parse en afscheiding van de productieroute,
setkop direct na kenmerknaam en na getal zonder eenheid, een met de hand geschreven casebestand met
jaargang en hele tijdlijn, de exclusieve grens, volgorde, regel zonder periode, set vóór situatie,
typering, en de weigeringen op hun regel: overlap, geen jaargang op de peildatum, conflict met de
situatie, scenario zonder peildatum, onbekende en dubbele set, poortzin, tijdlijnregels in een set,
ongekozen set, onvolledige periode).

**Weigeringszinnen, echte uitvoer** (definitieve build, CLI over `bron.rgs` en een gemuteerde
`scenario.rgs` van de golden; elke mutatie rc 1, niets geschreven):
- `ERROR (overlap.rgs:6): scenario: de parameterset 'jaarlijkse vaststelling' geeft de parameter
  'vondstdrempel' twee jaargangen die elkaar overlappen (van 2025-01-01 tot 2026-01-01 en van 2025-12-31
  tot 2027-01-01); de peildatum van een scenario kiest precies één jaargang`
- `ERROR (gat.rgs:34): scenario: de parameterset 'jaarlijkse vaststelling' heeft voor de parameter
  'vondstdrempel' geen jaargang die de peildatum 2027-06-01 van het scenario 'nieuwjaarsdag 2026' bevat`
  (en drie dezelfde voor de andere parameters)
- `ERROR (conflict.rgs:34): scenario: de parameter 'vondstdrempel' krijgt een waarde uit de parameterset
  'jaarlijkse vaststelling' en uit de situatie van het scenario 'nieuwjaarsdag 2026'; een parameter krijgt
  zijn waarde uit één van beide`
- `ERROR (zonderpeildatum.rgs:33): scenario: de parameter 'vondstdrempel' heeft in de parameterset
  'jaarlijkse vaststelling' jaargangen, maar het scenario 'nieuwjaarsdag 2026' heeft geen peildatum die er
  één kiest (voeg 'met peildatum <datum>' toe)`
- `ERROR (onbekend.rgs:19): scenario: de parameterset 'jaarlijkse vastelling' is nergens in het document
  gedeclareerd`
- `ERROR (dubbel.rgs:47): scenario: de parameterset 'Jaarlijkse Vaststelling' is meer dan eens
  gedeclareerd`
- `ERROR (poort.rgs:15): scenario: de regel 'betaaldatum van de penning' leest de parameter
  'uitbetaaldag', die in de situatie van het scenario 'oudejaarsdag 2025' en in de parameterset
  'jaarlijkse vaststelling' geen waarde heeft`

**Gates, slotronde op `23d6d4a0`** (alle rc 0): 1 PASS; 2 PASS (311 bestanden, 38740 objecten in
sleutelvolgorde); 3 PASS (311 golden clean, 486 negatief); 4 PASS (770 casebestanden); 5 PASS (1152
voorbeelden, 730 cases); 6 PASS (1988 checks, 730 cases); 7 PASS (686 weerleggen, 44 overlevers); 8 PASS
(40 rood, 80 fragmenten); 9 PASS (241 checks, 12 goldens); 10 PASS (921 tests, 99 rgs-goldens); 11 PASS
(45 rgs, 27 tests, 29/29/29); 12 PASS (played 216, DRL-tekst 186, JUnit 840); 13 PASS (41 twists over
23 goldens); 14 PASS (150 checks); 15 PASS (235 van 272 goldens vergeleken, route 99); 16 PASS (7
scenario-goldens, vloer 7; 31 cases, vloer 31; was 6 en 29). `v1-naamtabel-toets.py`: OK, rc 0. Gate 12
eerst rood gemeten: `check-drools: FAIL — played 216, but EXPECTED_PLAYED pins 215 — …`.

**Voorstel CONSTRUCTS, rij 37** — achter de laatste zin ("… a case that throws fails alone).")
toevoegen: "**Parameter sets (2026-09-27, card `scenario-parametersets`):** a scenario document carries
`Parameterset <naam>:` blocks of parameter rows, and a scenario chooses one with `met parameterset
<naam>` (two new tokens, `PARAMETERSET` and `MET_PARAMETERSET`). The rows of a parameter without
granularity are jaargangen, from inclusive to exclusive; the scenario's peildatum chooses exactly one for
every parameter of the set, and overlapping jaargangen, a peildatum outside every jaargang and a missing
peildatum are refused; a parameter with a timeline enters the case file whole; a parameter that both the
set and the situation give a value is refused. The IR does not change: a parameter without granularity
has one value on the wire, and the case file carries the chosen one. Witness `jutterswet-jaarvaststelling`
(two jaargangen, two scenarios one day apart, played by ref, clingo and both Drools branches); seven
goldens now carry a `scenario.rgs` (gate 16 floors 7 goldens, 31 cases)."

**Voorstel PLAN §9** — in de alinea "The wave before the move landed (2026-09-26, …)" de clausule "a
scenario has no surface for an absent parameter;" schrappen, en toevoegen: "**Parameter sets in the
scenario document (2026-09-27, card `scenario-parametersets`):** the scenario route has its parameter
sets back, spec-first: a `Parameterset` block with jaargangen, chosen per scenario and resolved on the
peildatum (`frontends/regelspraak/grammar/DIVERGENCES.md`, Scenariotoetsen; golden
`jutterswet-jaarvaststelling`, gate 16). The open point 'a scenario has no surface for an absent
parameter' is closed without a new form: the one pinned case leaves out a list parameter, a datatype
RegelSpraak does not declare, and a scalar parameter that is read and left out faults in ref and clingo
(RUNTIME-SEMANTICS ch. 5.7; red case `aanvoergeld-parameter-ongebonden`), so the parameter gate refuses
what the executors would. Open for the distribution: its service reads the set from the same document
instead of its JSON file (a card there)."

**Wat de dienst nodig heeft (alleen benoemd).**
1. Een lezer: het blok parseert alleen onder `testDocument`. De opgeloste sets zijn nu privé in
   `ScenarioResolver`; de dienstkaart heeft hier (spec-eerst) een publieke vorm nodig: per set, per
   parameter de jaargangen `[van, tot)` met een getypeerde casewaarde, of een tijdlijn.
2. Grenzen: het model `dev.uchar.regelspraak.model.Parameterset` heeft een inclusief `tot`: `vanaf` =
   `van`, `tot` = `tot` min één dag; een regel zonder periode is `Geldigheid.ALTIJD`.
3. Waarden: het blok typeert tegen de declaratie, eist een eenheid waar die gedeclareerd is en herleidt
   exact naar de gedeclareerde eenheid (caselexemen); de JSON-sets lezen kale getallen en kennen een
   expliciete lege waarde (JSON null), waarvoor het blok geen vorm heeft (zie U3).
4. De descriptor noemt een setnaam uit het document in plaats van een bestandsnaam.
5. De keuze per verzoek is dezelfde regel als `waardenOp`/`meerVintage` na de grensomrekening.
6. Conflict: defaults tegen set blijft een startfout; maar de dienst laat een verzoekparameter winnen van
   een setwaarde, waar U3 in een scenario weigert — een besluit voor de dienstkaart.
7. Tijdlijnregels in een set hebben in de JSON-sets geen vorm, en Drools weigert tijdgranulaire
   parameterinvoer (gate 12, U6).
8. Hier kiest een scenario één set; de dienst voegt alle sets van zijn bestand samen.

**Restpunten.**
1. `scripts/check-drools.sh` (beschermd, niet in "Mag raken"): U4 eist dat gate 12 de golden speelt en
   beide tellingen zijn een gelijkheid, dus `EXPECTED_PLAYED` 215 → 216 en `EXPECTED_DRL_PLAYED` 185 →
   186 met redenregels, in een eigen commit (`f9eb6fcf`); de hoofdsessie beslist.
2. Het open punt van PLAN §9 is niet door een nieuwe vorm gesloten maar verklaard (U3 hierboven); het
   PLAN-voorstel zegt dat.
3. Gevonden en gerepareerd binnen de kaart: `leesPeriode` viel met een NullPointerException op een
   periode zonder tweede datum na syntaxherstel; een situatieregel van een tijdlijnparameter bereikte
   dat al vóór deze kaart (rood gemeten zonder de reparatie, groen erna; `23d6d4a0`).
4. Frasetokens dwingen geen woordgrens af: `met parametersets` wordt `MET_PARAMETERSET` + `LOWER_ID(s)`,
   net als de bestaande `de parameters` (`DE_PARAMETER` + `s`) en `met peildatums`. Niet veranderd.
5. De kopvolgorde is vast: `met parameterset` vóór `met peildatum` geeft `syntaxfout: mismatched input
   'met peildatum' expecting {GEBASEERD_OP_DE_SITUATIE, GEGEVEN_IS_DE_VOLGENDE_SITUATIE,
   MET_ALS_VERWACHT_RESULTAAT}`, gevolgd door het bestaande herstelgedrag; een tweede `met parameterset`
   idem.
6. Gemeten op een concept, niet in deze kaart: clingo weigert een percentage van een eurobedrag
   (`TRANSLATION FAULT: a unit-carrying multiplicative operand is not translated (…)`); de golden schrijft
   het percentage daarom in een eigen attribuut.
7. Rij 37 van `spec/CONSTRUCTS.md` bevat midden in de rij tekst die er niet thuishoort (Result-notities
   van `ref-caselabel-datumfout`: "Registerrijtekst …", "### Fixronde 1 (hoofdsessie, 2026-09-23) …").
   Niet aangeraakt.
8. In de distributie noemt de Javadoc van `ParametersetLader` een methode
   `ScenarioResolver#resolve(String, java.util.Set)` die hier niet bestaat; en het model
   `dev.uchar.regelspraak.model.Parameterset` (inclusieve grenzen) heeft hier geen gebruiker behalve zijn
   eigen test: twee conventies voor één begrip, voor de dienstkaart.
9. `frontends/regelspraak/grammar/DIVERGENCES.md` noemt in de sectie Scenariotoetsen al vóór deze kaart
   de werkbank van de externe regelsets bij naam ("move of the … workbench (2026-09-26)"), in een
   artefactzone; gate 1 vangt dat niet. Niet aangeraakt.

## Keuring (2026-09-27, hoofdsessie)

Geland als unie met de kaartloze fix `fix/aggregatie-meervoud` (de melding
van een keten zonder sourcePath; twee commits gerebased op de kaartbranch). De
pin-ophoging in `scripts/check-drools.sh` (215→216, 185→186) en de vloeren van
`scripts/check-scenario.sh` (7/31) zijn aanvaard: een gelijkheidspin vraagt dat
bij een nieuwe gespeelde golden. U3 is gesloten door verklaring, niet door een
nieuwe vorm (zie PLAN §9). Alle zestien gates op de unie:

- gate 1 (check-self-contained.sh) rc=0: check-self-contained: PASS
- gate 2 (check-ir-schema.sh) rc=0: check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 311 file(s) vali
- gate 3 (check-verifier.sh) rc=0: check-verifier: PASS (311 golden clean; 486 negatief rejected for their declared rule, floor 350; 329 on their
- gate 4 (check-case-schema.sh) rc=0: check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 770 case file(s) validate)
- gate 5 (check-oracle.sh) rc=0: check-oracle: PASS (ref suite 1152 worked examples, floor 489; 730 cases replayed exactly, floor 427; NOT repl
- gate 6 (check-clingo.sh) rc=0: check-clingo: PASS (backend suite 1988 checks, floor 83; 730 cases replay exactly through clingo — ref = cling
- gate 7 (check-noop.sh) rc=0: check-noop: PASS (686 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
- gate 8 (check-rood.sh) rc=0: check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of 
- gate 9 (check-catala.sh) rc=0: check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identic
- gate 10 (check-regelspraak.sh) rc=0: check-regelspraak: PASS — JUnit suite green (934 tests, floor 173); 99 rgs golden(s) re-emit byte-identically 
- gate 11 (check-toka-parity.sh) rc=0:     Drools excludes by form:
- gate 12 (check-drools.sh) rc=0:   JUnit 840 tests, floor 239  (the backend's own suite, green with a non-zero count)
- gate 13 (check-reftype-derivation.sh) rc=0:   corpus-wide accounting: 83 unit-bearing annotations = 45 twistable + 38 exempt (the annotation IS the source
- gate 14 (check-flogic.sh) rc=0: check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically 
- gate 15 (check-graaf.sh) rc=0: check-graaf: PASS
- gate 16 (check-scenario.sh) rc=0: check-scenario: PASS — 7 scenario golden(s) (floor 7) regenerate 31 case file(s) (floor 31) and their ir.json 
