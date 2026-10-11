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
