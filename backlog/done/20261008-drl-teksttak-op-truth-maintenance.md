# drl-teksttak-op-truth-maintenance: de DRL-teksttak leidt af met truth maintenance, defaults inbegrepen

*Vastgelegd door de hoofdsessie op 2026-10-06 als `drl-defaults-lowering` ("de
DRL-teksttak heeft geen lowering voor `prioritizedDefaults`"), en op
2026-10-08 herschreven na het besluit van de eigenaar op het `Result:` van
`20261007-drools-logische-afleiding` (PLAN §9, 2026-10-08): de DRL-teksttak
gaat ook op truth maintenance. Daarmee vervalt de keuze tussen de routes A, B1
en B2 die deze kaart eerst vroeg; route A, met logische stuurfeiten, is in de
vorm van de proeftak de verlaging van beide takken. De oude tekst staat in de
geschiedenis van dit bestand. Regelnummers en tellingen op uchar `3619921b`.*

## Aanleiding

Gate 12 speelt elke golden door twee takken van `backends/drools`. De
DRL-teksttak (`transpiler/DrlTranspiler.java`, 5755 regels; de DRL-tekst wordt
met `KieHelper` gecompileerd) schrijft destructief op de bonen en ordent met
`transpiler/Stratificatie.java`. Hij heeft een eigen gelijkheidspin
(`EXPECTED_DRL_PLAYED=194`, `scripts/check-drools.sh` regel 1690) en een eigen
lijst van niet-spelers met reden (`DRL_CHANNEL`). Voor `prioritizedDefaults`
heeft hij geen verlaging: `cases/DroolsBackend.java` regel 289 weigert elk
document met een niet-lege lijst, en 40 goldens weigert hij alleen om die reden
(klasse (A) van `DRL_CHANNEL`, kop op regel 1271). `anchor/jutterswet-kustwacht`
staat er met een FAIL (regel 1521); de proeftak speelt die golden.

Na `20261008-tms-omschakeling` is de exec-modeltak de truth-maintenance-verlaging.
Volgens het besluit krijgt de DRL-teksttak hetzelfde ontwerp. Gate 12 vergelijkt
dan twee weergaven van één ontwerp, in PatternDSL en in DRL-tekst; de
onafhankelijke toets van het ontwerp blijft bij ref en clingo. Wat gate 12
tussen de twee nog vangt, zijn fouten in de weergave, zoals de `from`-bron die
het executable model tussen twee regels deelde en die de proeftak vond
(`ParameterStand`).

Goal: `DrlTranspiler` schrijft DRL-tekst die afleidt met `insertLogical` in een
KieBase in EQUALITY, over dezelfde feiten als de exec-modeltak, met de defaults
via logische stuurfeiten; de goldens van klasse (A) spelen; `Stratificatie` en
de maatregelen tegen destructief schrijven verdwijnen uit deze tak.

## Ontwerp

- **Gedeeld met de exec-modeltak**: de feiten (`HeeftKenmerk`, `Waarde`, de
  standfeiten, `Beslist`, `Uitkomst` en wat de bouwkaarten toevoegen), `Kringen`,
  `Stapels`, `Feitnamen`, `transpiler/kern/`, de weigeringen per vorm en het
  terugzetten. Eigen aan deze tak is alleen de weergave in DRL-tekst. Reden: het
  besluit kiest één ontwerp voor beide takken, en een tweede analyse zou een
  tweede kans op afwijking zijn; `transpiler/kern/StaandeDoelen` werd om
  dezelfde reden één klasse ("the boundary is what has to be shared"). De
  bouwer kiest de vorm van de scheiding (een gedeeld plan met twee weergaven,
  of gedeelde hulpklassen); de keuze en de reden staan in het `Result:`.
- **Volgorde**: na `20261008-tms-omschakeling`, zodat deze tak een af ontwerp
  weergeeft. Tot dan vergelijkt gate 12 de nieuwe exec-modeltak met een anders
  gebouwde DRL-teksttak, wat tijdens de omschakeling een sterkere toets is.

## Gevolgen voor gate 12

- Elke golden van klasse (A) die gaat spelen, verlaat `DRL_CHANNEL` en verhoogt
  `EXPECTED_DRL_PLAYED`; de lijst faalt als zij in een van beide richtingen
  verouderd is. Ten hoogste 40 goldens, dus van 194 naar ten hoogste 234. Een
  golden die daarna op een andere grens van deze tak stuit, verhuist naar die
  klasse met zijn nieuwe reden.
- `DRL_CHANNEL` aanvaardt `EXCLUDE`, `FAIL`, `ERROR` en `NOCASE` (de kop op
  regel 46: "here an ERROR is LISTABLE"). Met één ontwerp voor beide takken is
  een FAIL in deze tak op een golden die de exec-modeltak speelt een fout in de
  weergave. De landing doet een **voorstel** of een FAIL hier op de lijst mag
  blijven.
- Gate 13 (`scripts/check-reftype-derivation.sh`) speelt ook `--drl`; zijn
  telling en vloer gaan mee.

## Aanvulling na `20261008-tms-expressievormen-en-rekendatum` (2026-10-09)

- **De scheiding van analyse en weergave blijft de keuze van deze kaart.** Punt 5 van die kaart (een leestabel naast
  `Lezingen`, een gesloten woordenschat van structurele constraints met één mapper naar PatternDSL, en de IR-knoop bij
  elke gecompileerde toets en waardefunctie) is daar niet gebouwd, omdat het deze keuze zou voornemen (verslag van
  stap 2 in haar `Result:`). De tak met truth maintenance mengt analyse en PatternDSL dus nog. Zijn vormen sinds die
  kaart (de waardevormen, de voorwaardevormen, de schrijvers, de deponeringen via een sonde, de rekendatum als feit)
  zijn wat deze tak in DRL-tekst moet weergeven.
- **Gedrag dat alleen de destructieve DRL-teksttak heeft** (afspraak van de eigenaar van 2026-10-09: geen eigen kaart;
  het verdwijnt met deze kaart): een versie met een begrensde geldigheid in een sessie zonder rekendatum speelt, waar
  ref faalt; de terugvalwaarde van een som in een andere eenheid wordt niet herrekend; een kenmerk dat op een
  tussenstand gezet werd, blijft staan (het eenheiddocument van `TmsRekenvormWitnessTest` faalt in 8 van de 24
  volgordes); een deling door leeg deponeert niet in een voorwaarde, bij een operand die geen gelezen attribuut is, of
  in het filter van een vouw; een tekstdeel van een getal of een waarheidswaarde schrijft "5.50" en "true".

## Aanvulling na `20261008-tms-verdeling-en-tijdlijnen` (2026-10-09)

- **Wat deze tak in DRL-tekst moet weergeven,** naast de vormen van de vorige kaarten: de verdeling (een `Ontvanger`-feit
  per bron en ontvanger en één activering per bron; de berekening is de statische functie `Verdeling.bereken`, die
  DRL-tekst kan aanroepen zoals nu `rsVerdeelOpVolgorde`), een tijdlijn als `List<Tijdvak>` in een feit, de invoervorm
  (`Emmerinvoer`), "gedurende het gehele" en de dagsoorten (een gecompileerd predicaat als constante van het document;
  deze tak levert het nu als sessie-global `rsDagsoort_<Naam>`).
- **Gedrag dat alleen de destructieve DRL-teksttak heeft** (afspraak van de eigenaar van 2026-10-09: geen eigen kaart;
  het verdwijnt met deze kaart): een later afgeleide volgordesleutel geeft 450/450 waar 900/0 hoort; twee verdelingen
  over dezelfde ontvangers werpen "cannot invoke method: setBergingsaandeel" (`20260923-drl-verdeling-dubbelbezet`; de
  tak met truth maintenance draagt die vorm, getuige `TmsVerdelingWitnessTest` deel B); een verdeling met twee versies
  deponeert op elke ontvanger in 2026 en faalt niet zonder rekendatum; een lege pot geeft een NullPointerException; de
  twee punten van `rsVerdeelOpVolgorde`; een rekenende regel naast een verdeling speelt 100 waar ref faalt; een tarief
  zonder gedeclareerde noemer geeft 120; de emmerscalair als segment (zie punt 18 van `20261008-tms-omschakeling`).

## Acceptatie

- (a) Rood-eerst getuigen in deze tak voor de vormen van
  `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §5.2 (geordende defaults met
  doorval en initialisatie, kenmerk-defaults, ongeordend met §9.5, geneste
  `excepties`), en voor de vormen van `20260923-drl-aantal-filter-zink` en
  `20260923-drl-verdeling-dubbelbezet`; elk in elke volgorde van de betrokken
  regels in het model.
- (b) De goldens van klasse (A) spelen of staan met een nieuwe reden in een
  andere klasse; elke golden die deze tak nu speelt, blijft spelen;
  `EXPECTED_DRL_PLAYED` en `DRL_CHANNEL` als **voorstel** (beschermd pad
  `scripts/`).
- (c) In de DRL die de tak over het hele corpus schrijft, staan geen `no-loop`,
  `salience`, `@watch`, `modify` of `update`; `transpiler/Stratificatie.java` is
  weg.
- (d) De exec-modeltak is ongewijzigd in gedrag: gate 12 groen met
  `EXPECTED_PLAYED` gelijk; alle gates van `CLAUDE.md` groen.

## Raakvlakken

- `20260923-drl-aantal-filter-zink` (een gekwalificeerd `AANTAL`-filter laat de
  DRL-compilatie van het hele pakket falen) en
  `20260923-drl-verdeling-dubbelbezet` (twee verdelingen op één ontvanger werpen
  een exceptie) zijn fouten in de destructieve verlaging die deze kaart
  vervangt. Hun vormen worden getuigen in (a), en die kaarten gaan met deze
  landing naar `done/`, met een verwijzing.
- `20260923-drools-rolpaar-ontdubbeling`: na deze kaart staat het beslisblok
  van de creatie alleen nog in `schema/FactModelGenerator.java` (eerste
  bouwkaart, punt 4); die kaart sluit mee als ze nog open is.
- `20261008-website-drools-routes-en-faq` beschrijft de stand na deze kaart.

## Review

Zoals in de bouwkaarten van de exec-modeltak: een lens na elke bouwstap, een
eindlens over de hele diff, na elke stap een doorloop van het hele corpus, en
een lensvondst alleen met een concreet faalscenario op echte invoer of een
getuige.

## Omvang

Middelgroot. De analyse is er dan al; het werk is de weergave in DRL-tekst van
elke vorm die de exec-modeltak draagt, plus de defaults.

May touch: `backends/drools/**` (de DRL-teksttak, `Stratificatie`, en de
gedeelde klassen voor zover de scheiding dat vraagt, zonder gedragswijziging in
de exec-modeltak), `scripts/check-drools.sh` en
`scripts/check-reftype-derivation.sh` (pins en lijst, **voorstel**),
kaartverplaatsing, ook van de drie kaarten onder Raakvlakken. NB: serieel met
andere Drools-kaarten.

Out of scope: het gedrag van de exec-modeltak, de toolchain (die gebruikt deze
tak niet), `spec/`, `corpus/`.

## Na `20261008-tms-omschakeling` (2026-10-11)

De exec-model-tak is de tak met truth maintenance (`transpiler/tms/`, label `exec-model:`); gate 12 pint 248 en 196, en
klasse (A) van `DRL_CHANNEL` telt 52 spelers alleen in de exec-model-tak. Acceptatie (c) van de omschakeling vond
alleen `DrlTranspiler.java:1965`, een commentaarregel. Begininventaris voor deze kaart, uit het `Result:` van de
omschakeling:
- de gemeten grenzen van de DRL-teksttak die in de acht weggevallen README-secties "Recorded: …" stonden (onder meer
  een test over een rol in het patroon van het subject, `anchor/jutterswet-kustwacht`; een lege-toets en vergelijking
  op de tussenstand; geen volgorde van creaties; markers voor een datumvouw; een maximum dat blijft staan als de
  subselectie leegloopt; `gesplitsteConjunctie`; geen defaultsverlaging); ze staan in de geschiedenis van
  `backends/drools/README.md` vóór `ba0e8264`;
- `DrlTranspiler` noemt `ExecModelTranspiler` op negen plekken, waaronder een `{@link
  ExecModelTranspiler#stratificatieSalience}` (r. 351) die nergens heen wijst (javadoc merkt het, javac niet);
  daarnaast `Stratificatie` r. 142 en `kern/Vouwwaarde` r. 63, r. 98 en de weigerzin r. 68–70;
- circa 180 keer "the truth-maintenance branch" als naam van de tak in testjavadoc: niet onjuist, maar dubbelzinnig
  zodra deze tak ook truth maintenance heeft.

## Na `20261010-objectcreatie-zonder-connectingrole-binding` (2026-10-11)

- **De exec-model-tak draagt een objectcreatie zonder `connectingRole`-binding.** De terugkoppeling volgt uit het
  gedeelde beslisblok en de relatie uit `TmsTranspiler.typenpaarrelatie`, met exacte vullers. Een gemaakte relatie leest
  de tak over koppelingsfeiten. De DRL-weergave neemt dat over.
- **`anchor/jutterswet-penninguitgifte`** speelt in de exec-model-tak (PLAYED 3). De DRL-teksttak weigert hem nog
  (`MARKER consistentie: unresolved consistency rule over every Penning`, `DRL_CHANNEL`). Speelt hij ook in deze tak, dan
  gaan `EXPECTED_DRL_PLAYED` en die regel mee.
- **Bijvangst: een bestaande fout van de DRL-teksttak**, gemeten door de bouwer van die kaart. Op
  `backends/drools/src/test/resources/objectcreatie/afwijzing-na-het-besluit` (verifier PASS; ref en clingo PASS, J1 0
  afwijzingen) geeft de DRL-teksttak `FAIL case.json: attribuut aantal afwijzingen: verwacht 0, werkelijk 1`. Op twee
  proefdocumenten van die bouwer (`strataproef3`, `strataproef4`, niet bewaard) gebeurt hetzelfde. De gevolgtrekking van
  de bouwer, niet nagegaan in de DRL-tekst: de afwijzende creatie vuurt vóór de creatie die de relatie maakt die zij
  negatief leest.

## Result (2026-10-14)

*Branch `card/drl-teksttak-op-truth-maintenance`, claim `c84c4c79` op main `b536bbc9` (2026-10-12 20:46). Op
2026-10-14 gerebased op main `078c89d9`; daarna is de claim `9ebf30ea` en de tip vóór dit Result `4ef4f2be` (145
commits na de claim). Gemeten op drie manieren, na elke bouwstap:*
- *`stapklaar.sh`: een bevroren boom van de stapcommit zonder `toka/`, een shaded jar uit die boom, en elke golden met
  cases door beide Drools-takken met die jar, per golden vergeleken met de nulmeting op de claim en, sinds de
  omschakeling, met elkaar; daarbij de acht scenariobestanden van v2 herleving-ww door de exec-model-tak.*
- *`meet-alles.sh`, het harnas `Vuurspoor` met de naad in `CaseRunner`: een dump per case van elke golden en per sessie
  van de suite (invoer, `lees`, de feiten aan het fixpunt, de uitkomst van het vuren, het vuurspoor). De exec-model-tak
  strikt tegen de nulmeting v2 (de code van de claim, `297e3867`, met de dump van de tip); de DRL-teksttak per case
  tegen de exec-model-tak op invoer, `lees`, `feiten` en `fixpunt`, met een vaste lijst van 47 bekende spoorverschillen
  en een vastgezette lijst van de suitesessies; daarnaast tekstdumps van het plan (planvorm), het executable model
  (modelvorm) en de DRL-tekst (drlvorm) over het corpus, tegen een nulmeting; en een zelftest van de vergelijking.*
- *`gates-kopie.sh`: de 16 gates van `CLAUDE.md` op een kopie van de stapcommit als eigen git-repo.*

*Elke stap kreeg een lens (copilot sol6), de kaart vier adviezen van Fable (`FABLE.md` tot en met `FABLE-4.md`) en een
eindlens (copilot Opus 5.5, oordeel "Landen"). De werkmap met besluiten, verslagen en metingen was
`/tmp/lens-drl-tms/`.*

### Wat de kaart opleverde

De Drools-backend heeft twee takken van één ontwerp, allebei met truth maintenance. Eén analyse
(`transpiler/tms/TmsTranspiler`) levert per regel een plan (`transpiler/tms/plan/`, samen een `Modelplan`): de lezingen,
de toetsen en waarden als boom van aanroepen (`Term`, `Functie`), het gevolg als soort. Twee weergaven maken er een
regelbank van: `PatternDslWeergave` (de exec-model-tak, `Branch.EXEC_MODEL`) en `DrlWeergave` (de DRL-teksttak,
`Branch.DRL_TEXT`, `--drl`). Een weergave beslist niets; wat de analyse weigert, weigert zij voor beide takken in één
zin (label `exec-model:`), en de DRL-weergave weigert alleen wat DRL-tekst niet kan uitdrukken (label `drl:`). Elke
RegelSpraak-toets en -berekening is in de DRL-tekst een Java-functie met de RegelSpraak-zin erboven (besluit van de
eigenaar, 2026-10-13).

- **Voortgang tegen de 250.** Op de claim speelde de exec-model-tak 250 goldens en de destructieve DRL-teksttak 197.
  Nu spelen beide takken 255 goldens (de 250 en de vijf nieuwe), met per golden dezelfde statusregel en per case gelijke
  invoer, `lees`, feiten en fixpunt. Van de oude naar de nieuwe DRL-tabel: 51 goldens van EXCLUDE en 2 van FAIL
  (`anchor/jutterswet-kustwacht`, `jutterswet-initialisatievolgorde`) naar PLAYED; geen speler verloren. De
  exec-model-tak is over de hele kaart gelijk gebleven aan de nulmeting v2.
- **Weg:** `transpiler/DrlTranspiler.java` (5833 regels), `transpiler/Stratificatie.java`, `cases/Schrijfconflict.java`,
  `kern/Faalbaar`, `kern/Vouwwaarde`, `runtime/CreationMarker`, `runtime/RuleFired`, `Accessors.getter`/`setter`,
  `Branch.DRL_TMS` (de tussentak van stap 2 tot 6a), het vaste vuurplafond, en 26 helpers van `RegelSpraakRuntime`
  zonder aanroeper. In de DRL-tekst van het hele corpus (261 teksten) staan geen `no-loop`, `salience`, `@watch`,
  `modify` of `update`.
- **Nieuwe goldens** (besluit van de eigenaar 2026-10-14, normvraag 2 van Fable-advies 4): `jutterswet-verdeling-
  dubbelbezet`, `jutterswet-uniciteit-drie-bronnen`, `jutterswet-uitsluiting-gedateerd`,
  `jutterswet-aantalfilter-gehele- jaar` en `jutterswet-aantalfilter-gehele-jaar-creatieguard` (de laatste twee zonder
  `bron.rgs`: de frontend zet hun filter niet om, zie de kaart hieronder), en de rode case
  `jutterswet-uitsluiting-zonder-rekendatum`. Ref, clingo en beide takken geven dezelfde uitkomst. "Out of scope:
  `corpus/`" in deze kaart is daarmee door dat besluit vervangen; verder raakte de kaart in `corpus/` alleen de tekst
  van vier `NOTE.md`'s.
- **Getuigen in de suite** (besluit van de eigenaar, normvraag 1): `TakdumpsCorpusWitnessTest` vergelijkt per case van
  elke golden die de exec-model-tak speelt (255 goldens, 695 cases) en per rode case (50) de dumps of het rode
  resultaat van beide takken; `NaadWitnessTest` en `RodeTakkeuzeWitnessTest` bewaken de naad en de takkeuze;
  `BreukInRustWitnessTest` (normvraag 3) brengt een niet-eindigende breuk in rust door beide takken;
  `GelijkeSchrijversWitnessTest` (twee rijen: regels, en ongeordende defaults-groepen) pint de keuze tussen schrijvers
  die het eens zijn; de
  spelling van de dump staat vast (`SessiedumpWitnessTest`); de volgordeproeven lopen ook op de DRL-tekst
  (`Tekstvolgorde`).
- **Gate 12** (voorstel, beschermd pad): per golden dezelfde statusregel op beide takken, één lijst niet-spelers, één
  pin `EXPECTED_PLAYED` (255); `EXPECTED_DRL_PLAYED` en `DRL_CHANNEL` vervallen. De gate faalt als de suite
  `TakdumpsCorpusWitnessTest` niet draaide; `EXPECTED_MIN_TESTS` gaat van 239 naar 1573. Nieuw: `--fast` voor een
  bouwer die een pin of lijst wijzigt (de jar zonder suite, de replay met elke pin en lijst, zonder DRL-sweep; geen
  landingsrun). **Gate 13** (voorstel): de DRL-leg moet de onaangeraakte regel van de exec-model-tak geven en evenveel
  goldens spelen. **Gate 7** (voorstel): case 2 van `jutterswet-uniciteit-drie-bronnen` als overlever (een consistentie
  die al op de invoer geldt). De tekst voor `CLAUDE.md` (gate 12, 13 en 7) en een zin in `RUNTIME-SEMANTICS.md` ch. 8
  (het Rekenjaar: beide takken spelen het nu op alle zeven posities) staan in eigen commits, als voorstel.

### Acceptatie

- (a) Rood-eerst getuigen voor de vormen van `PRIORITIZED-DEFAULTS-EVOLUTIE.md` §5.2 en voor die van
  `20260923-drl-aantal-filter-zink` en `20260923-drl-verdeling-dubbelbezet`, op beide takken; de volgordeproeven ook op
  de DRL-tekst. Gedaan (ronde 3 tot en met 6a); beide vormen zijn nu ook golden.
- (b) De goldens van klasse (A) spelen: alle 250 van de exec-model-tak spelen op de DRL-teksttak. De pin en de lijst
  van de DRL-tak vervallen in de strenge gate 12 (besluit van de eigenaar, 2026-10-13).
- (c) Geen `no-loop`, `salience`, `@watch`, `modify` of `update` in de DRL van het corpus; `Stratificatie.java` is weg.
- (d) De exec-model-tak is gelijk gebleven: tegen de nulmeting v2 0 regels verschil, planvorm en modelvorm 0; gate 12
  groen met `EXPECTED_PLAYED` 250, en 255 met de vijf nieuwe goldens; de 16 gates groen op een kopie van elke stap,
  het laatst op `88e350be` (gate 12 635 s, de DRL-sweep 220 s) en `763f7571`, vóór de rebase (nu `805e25a8` en
  `a077072e`). De landingsrun op de eindtip staat in PLAN §9.

### Gedragswijzigingen buiten de meting

- `Namen.getter`/`setter` volgen de beangenerator: een lid als "İş" wordt nu gelezen en gezet (`getIş`, `setIş`, wat de
  generator schrijft); vroeger zocht de tak `getİş`.
- `CaseRunner` zoekt bij een case die een lid met een andere hoofdletter spelt de gedeclareerde naam op, nu ook in het
  scenariorapport (`werkelijk`).
- `renderIncs` sorteert: de tekst van een mismatchzin en van `VerwachtingResultaat.werkelijk` verandert, geen feit en
  geen status.
- Twee schrijvers die het eens zijn met een andere schaal (5 en 5.00): de lezer neemt nu de waarde van de regel die in
  het document eerst komt (een defaults-stapel of een verdeling is altijd de enige schrijver van haar doel); vroeger
  hing de schaal op de bean af van de agendavolgorde, en wisselde ze zelfs per object binnen één sessie.
- Een DRL-tekst die de compiler weigert, is een `IllegalStateException` (een fout van de weergave), geen `Excluded`.
- De statusregel van `jutterswet-pachtvenster` noemt de oude DRL-teksttak niet meer.

### Dekkingsverlies

Wat alleen de destructieve DRL-teksttak speelde en de analyse weigert, weigert nu ook de DRL-teksttak, bijvoorbeeld een
vouw of deling in een ledeninitialisatie van een creatie (`FilterfoutWitnessTest`). De omgezette en geschrapte
getuigenrijen staan in de verslagen van stap 6a.

### Ontwerpnotities

- Het lezen van het fixpunt hangt niet af van de agendavolgorde (fouten gesorteerd, schrijvers geordend, inconsistenties
  gesorteerd, creaties per stratum en fase). De 47 bekende spoorverschillen zijn de volgorde en het aantal vuringen bij
  een accumulate; het fixpunt is gelijk.
- `MAX_LEZINGEN=13` is een grens van PatternDSL in de analyse; de DRL-teksttak weigert die vormen daardoor ook.
- `DrlWeergave` leidt twee dingen zelf af, zonder de semantiek te veranderen: de null-guard van een join uit de scope,
  en het lezen via de eigen klasse of de route `lees`.
- De lijsten van `Fixpunt` hebben geen volgorde; een afnemer die er een toont, sorteert zelf.

### Grenzen, bijvangst en kaarten

- **Breuk in rust.** Ref schrijft 1/3 exact (RUNTIME-SEMANTICS ch. 2.4), clingo weigert met een naam, Drools schrijft
  `0.3333333333333333`: een vastgelegde grens van de backend, die `BreukInRustWitnessTest` bewaakt. De begrensde vorm
  (`corpus/rood/decimal-nonterminating`) is het gat van de v0-spiegel uit ch. 2.4; gate 8 noemt het voor Drools.
- **Kaarten** in `backlog/new/`: `20261014-beanlidnamen-zonder-java-identifier`, `20261014-caselezer-zoals-ref` (met de
  vondst van de lens op 6c: een verwachting op "premie-som" slaagt voor het lid "premie som"),
  `20261014-frontend-heelperiode-in-aantalfilter`, `20261014-weergaven-als-getuige` en
  `20261014-clingo-default-literaalschaal` (bijvangst van stap 6d: clingo weigert een default die de literal 5 naar
  een doel met twee decimalen schrijft, waar de verifier en ref het document goedkeuren en spelen).
- **Gemeld, niet gekaart:** of de weigering van een cijferproef over een pad nog nodig is (niet getoetst); of rij 2 van
  `GemengdeUnieRandWitnessTest` nog bijt (niet gemeten).

### Voor de distributie (bij de pin-bump)

- `RegelSpraakCli.java:156` en `Playground.java:162`: `.drlTranspiler().drl()` wordt `.drl()`.
- `CaseSessie.java:62–65`: de bewaking `execModelTranspiler() == null` onderscheidt de takken niet meer.
- CLI en Playground vangen de `IllegalStateException` van een DRL-tekst die niet compileert; nu geeft de CLI een
  stacktrace met exit 1 en de Playground HTTP 500.
- `Namen.getter`/`setter` volgen de generator (in `RolSchrijver`, `Verbalisator`, `CaseSessie`, `ScenarioRunner`,
  `VerzoekMapper`); het DRL-paneel toont nu ook documenten met defaults.

### Kosten

Gate 12 duurt ongeveer 650 s (op de claim ongeveer 300 s): de suite ongeveer 300 s, de strenge DRL-sweep 206–228 s.
`--fast` duurt 131–160 s. De suite telt 1574 tests.

### Commits per stap

| Stap | Commits | Inhoud |
|---|---|---|
| 1a–1e | `41f7cc80` … `27a70826` | het harnas en de naad; de analyse levert het plan, `PatternDslWeergave` bouwt het model |
| 2a–2b | `8705e359` … `dc5da43b` | `DrlWeergave`, de tussentak `DRL_TMS`, de namen van een beanlid op één plek |
| 3–5b | `b28b72f8` … `5fc6c918` | vouwen, uniciteit, kanaal, invoervorm; defaults; creaties, koppelingen, verdeling, dagsoorten; de dump gepind |
| 6a | `4a3fe792` … `d2e1c306` | de omschakeling: de code, de gate-scripts als voorstel, de hernoeming |
| 6b | `9e6d95bf` … `805e25a8` | de goldens, de breukgetuige, de harnasgetuige, dode runtimecode, `--fast` |
| 6c–6d | `a15ac6f2` … `0b5092a7` | de fixrondes van de lenzen en de eindlens |
| landing | `9d1aa391`, `4ef4f2be` en dit Result | de voorstellen voor `CLAUDE.md` en `RUNTIME-SEMANTICS.md`, dit Result |

### Raakvlakken

`20260923-drl-aantal-filter-zink` en `20260923-drl-verdeling-dubbelbezet` gaan met deze landing naar `done/`: hun
vormen zijn getuigen op beide takken en golden (`jutterswet-aantalfilter-gehele-jaar*`,
`jutterswet-verdeling-dubbelbezet`). `20260923-drools-rolpaar-ontdubbeling` stond al in `done/`.
`20261008-website-drools-routes-en-faq` beschrijft de stand na deze kaart.
