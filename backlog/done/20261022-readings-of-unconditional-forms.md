# readings-of-unconditional-forms: een vorm zonder voorwaarde krijgt een lezing van haar waarde

*Uit het Fable-advies van 2026-10-22 over de uitleg van berekende waarden
(`uchar-dev/research/uitleg-berekende-waarden-2026-10-22/fable-advies.md`, §1.2, §3, §5 U4; beslispunt 3: voorlopig
besluit van de hoofdsessie naar dat advies). Voorwaarde voor de toolchain-kaart `explanation-computed-attributes`.*

## Aanleiding

RUNTIME-SEMANTICS ch. 5.12 legt voor een vorm met een voorwaarde de lezing vast, en daarin ook de knopen van de waarde
(tussenwaarden, de bijdragers van een vouw met hun bijdrage, de eenheid per knoop). Een vorm zonder voorwaarde heeft er
geen ("A form without a condition has none either", r. ±5080): de Drools-backend plant de leesregel van een regel op een
attribuut alleen met een test (`TmsTranspiler` r. ±1670, een default r. ±3285), en ref neemt alleen `(Some condition,
Some objective)` (`ref/lib/reading.ml` r. ±348–352). Een `Regel … moet gesteld worden op …` zonder `indien` heeft dus
geen waarde van haar operanden en geen bijdragers van haar vouw. In de v2-regelset herleving-WW raakt dat drie van de
17 uitkomsten zonder uitlegzin (het maandloon van het recht; de eerste kalendermaand met overschrijding; de eerste
kalendermaand zonder overschrijding na-de-eindiging) en de initialisatie van de maximeringsfactor.

## Doel

Een vorm zonder voorwaarde krijgt een lezing: de status is `HOLDS` wanneer de vorm aan de beurt is, met de knopen van
haar waarde zoals bij een vorm met voorwaarde. Ch. 5.12 voegt dat toe en herschrijft niets voor vormen met voorwaarde.
ref (`--trace`) en de Drools-backend (beide takken, de build met lezingen) leveren haar; gate 17 vergelijkt haar.

## Acceptatie

- (a) Rood eerst: een `ReadingsWitnessTest`-rij voor een regel `gesteld worden op` zonder `indien` (verwacht een
  `HOLDS` met de waarde van de lezing) faalt op de code van nu; gate 17 op een golden met zo'n regel.
- (b) De rijen groen op beide takken; `ReadingsCorpusWitnessTest` en gate 17 groen, met de gemeten aantallen in de
  Result (vloeren zijn ondergrenzen: geen verhoging nodig, wel noemen); de productiebuild bytegelijk
  (`ReadingRulesFixpointWitnessTest`); `EXPECTED_PLAYED` ongewijzigd tenzij een nieuwe golden; de zeventien gates groen.
- (c) De spec-clausule in ch. 5.12 als voorstel, met een gewerkt voorbeeld.

May touch: `backends/drools/**` buiten de productieruntime, `ref/lib/reading.ml`, `ref/test/`, een golden; `spec/` en
`scripts/` als voorstel; deze kaart. Start na de landing van `cycle-across-instances` (beide raken Drools, ref en spec).

## Voorlopige besluiten (bouwer, 2026-10-22)

*Gemarkeerd voorlopig; de eigenaar kan elk besluit omdraaien. Elk staat ook in ch. 5.12 (voorstel).*

- **V1 Aan de beurt.** Een vorm zonder voorwaarde is `HOLDS` waar zij aan de beurt is, ook waar haar waarde leeg is of
  faalt; een initialisatie zonder voorwaarde (die van een regel, een gevouwen regel in een stapel, of de
  initialisatiedefault van ch. 7.3) is `DISPLACED` waar de invoer of een andere schrijver het doel vult; een
  attribuutdefault zonder rechtvaardiging en een gevouwen regel zonder voorwaarde zijn `DISPLACED` waar een hogere
  default besliste. De rij `DISPLACED` van de statustabel noemt daarom ook de initialisatiedefault. Een vorm zonder
  voorwaarde die hier geen waarde berekent (kenmerktoekenning, kenmerkdefault, groepsdefault, verdeling, creatie) houdt
  geen lezing.
- **V2 De eenheid achter een getal** in de canonieke tekst is die van het effectieve type van de knoop (ch. 2.10), het
  getal daarin herschreven; tot nu toe stond er "the unit it carries", en `ref` schreef de eenheid die zijn evaluatie
  meedroeg (een leeg die tot 0 coalesceert: geen; `leeg kg plus 1 t`: `1 "t"`), Drools de effectieve (`0 "kg"`,
  `1000 "kg"`). Gemeten op `jutterswet-stelselweging`, `-keurlast`, `-muntbewaring`, `-munttelling`, `-tempototaal`,
  `-tempovouw`, `-vondstrecht`, `-vondsttelling`.
- **V3 De gevulde telling is een telling** (ch. 3.4c zegt het): haar bijdragers zijn de leden die zij telt, zonder hun
  waarde (`"V1" empty reached`), zoals bij `count`; `ref` schreef de waarde van elk lid, Drools niets.

## Result (2026-10-22)

**Commits** (tak `card/readings-of-unconditional-forms`, gerebaset op main `4970cceb`):
`61265363` naar doing; `ae874142` spec-voorstel ch. 5.12; `7a94f677` ref; `c55dc0a0` Drools;
`ffcd2713` voorlopige besluiten; dit Result. Niet gemerged, niet gepusht: `spec/RUNTIME-SEMANTICS.md` is een
beschermd pad, de tak is het voorstel.

**Rood eerst.** `ReadingsWitnessTest.forms_without_a_condition` (een som over een rol met bijdragers en een lege, een
`gesteld worden op` één rol ver, een initialisatie aan en niet aan de beurt, een initialisatiedefault aan de beurt en
verdrongen door een hogere default en door de invoer) faalde op de code van `018be1df` op beide takken: van de vijftien
records stonden er drie, alleen die van de default met voorwaarde. `runner --trace` gaf op `jutterswet-erfwacht`,
`-stukopbrengst` en `-kistinhoud` nul regels. Na de bouw groen op beide takken; ref en Drools geven dezelfde lezingen.

**Gemeten corpus** (main `4970cceb`): 476 regelversies zonder voorwaarde die een attribuut berekenen of gelijkstellen en
3 initialisaties zonder voorwaarde, in 235 goldens met cases; 9 attribuutdefaults zonder rechtvaardiging, waarvan 7
initialisatiedefaults met een waarde (krijgen een lezing) en 2 groepsdefaults zonder inhoud (krijgen er geen). Een
nieuwe golden was niet nodig: `EXPECTED_PLAYED` is ongewijzigd.

**Gate 17**, basis (kopie van `018be1df`) → eindstand:
| | basis `018be1df` | eindstand, zelfde corpus (`g17-3.log`) | eindstand op main `4970cceb` (`gates-1`) |
|---|---|---|---|
| goldens / casebestanden vergeleken | 269 / 734 | 269 / 734 | 271 / 743 |
| lezingen (statusregels) | 3905 | 6062 (+2157) | 6089 |
| knoopregels | 14319 | 19147 (+4828) | 19513 |
| waarvan `unreached` | 3164 | 3304 (+140) | 3425 |
| met bijdragers | 324 | 780 (+456) | 780 |
| met een tijdlijn | 76 | 319 (+243) | 319 |
| bijdragers van ref niet bereikt | 110 | 110 | 110 |

Afwijkingen 0 (`DEVIATIONS` en `READINGS_ERROR_KNOWN` leeg); de twee goldens die main sinds de basis kreeg
(`jutterswet-premietabel-twee-ofcellen`, `-vrijstelling-drie-niveaus`) lezen op beide takken gelijk aan ref.

**Vloeren niet verhoogd** (ondergrenzen; de groei vraagt geen bump): gate 17 `EXPECTED_MIN_TRACED_CASES`,
`EXPECTED_MIN_COMPARED_GOLDENS`, `EXPECTED_MIN_COMPARED_CASES`, `EXPECTED_MIN_READINGS`, `EXPECTED_MIN_NODE_LINES`,
`EXPECTED_MIN_UNREACHED`, `EXPECTED_MIN_CONTRIBUTOR_LINES`, `EXPECTED_MIN_TIMELINE_LINES`,
`EXPECTED_MIN_UNREACHED_CONTRIBUTORS`; `ReadingsCorpusWitnessTest` `EXPECTED_MIN_READINGS` (3635, nu 5813 per tak),
`EXPECTED_MIN_NODES` (13971, nu 19147), `EXPECTED_MIN_OPERANDS`, `EXPECTED_MIN_COMPOUNDS`, `EXPECTED_MIN_DISPLACED`,
`EXPECTED_MIN_OUTSIDE`; `ReadingRulesFixpointWitnessTest` `EXPECTED_MIN_GOLDENS`, `EXPECTED_MIN_CASES`,
`EXPECTED_MIN_READINGS` (3635, nu 5813 leesfeiten per tak); gate 5 `EXPECTED_MIN_EXAMPLES` (489; de suite telt nu 1365).

**De productiebuild is bytegelijk.** De DRL-tekst van de build zonder lezingen van alle 318 goldens van `018be1df`
(272 gebouwd, 46 met dezelfde weigering) is gelijk tussen de jar van de basis en die van de eindstand
(`werk/bruggen/u4/tools/drldump`, `diff -r` leeg); `ReadingRulesFixpointWitnessTest` groen op beide takken.

**Bestaande fouten in de lezingen, op deze tak gefixt.** Gate 17 vergeleek voor het eerst de waardeknopen van vormen
zonder voorwaarde en was rood op 42 casebestanden, op beide takken (`werk/bruggen/u4/g17-1.log`):

1. *De knip van een temporeel totaal* (27 casebestanden: `erfwacht`, `getijdenwacht`, `tegenwacht` ×6, `dubbelwacht`
   ×4, `vast-startpunt` ×7, `synth2-clip-over-prorata`, `synth2-clip-cond-over-prorata`, met de filters hieronder).
   Drools las de clausule als de tijdlijn die de knip overlaat (de waarden van het basisattribuut, cumulatief over de
   leden van een samenstelling); ch. 5.12 en ref lezen de perioden waarin zij geldt: van een kenmerk zijn perioden,
   ontkend die erbuiten binnen elk segment, een samenstelling de doorsnede van de leden, een vergelijking de segmenten
   van de vergeleken tijdlijn, gesneden waar de getotaliseerde tijdlijn gesneden is. Drools leidt die lezing nu af uit
   termen die de regel al evalueert (`Derived`, `noteDerived`), zodat de regel zelf ongewijzigd blijft. Getuige:
   `ReadingsWitnessTest.the_clip_of_a_temporal_total_reads_the_periods_in_which_it_holds` en ref
   `WV-5.12-gedurende-clip`, één document (`backends/drools/src/test/resources/readings/temporal-total-clip`, met
   `source.rgs`), elke regel met de hand uit ch. 3.6; rood met de jar vóór de fix (`werk/bruggen/u4/clip/`).
2. *Een afwezige kenmerktijdlijn als filter* van een tijdsevenredig deel of een dagtelling (`borgdeel`, `dagentelling`,
   `dagentelling-rgs`, `vast-startpunt`): Drools schreef `empty`, ref `[]`; nu beide `[]` (geen perioden).
3. *De gevulde telling* (6: `gevuldegemiddelde`, `gevuldepad` ×2, `gevuldesoorten`, `gevuldetelling` ×2): ref schreef de
   waarde van elk lid, Drools het lid zonder waarde; voorlopig besluit V3, ref past zich aan (in `reading.ml`).
4. *De eenheid achter een getal* (7: `keurlast`, `muntbewaring`, `munttelling`, `stelselweging`, `tempototaal`,
   `tempovouw`, `vondsttelling`): voorlopig besluit V2, ref schrijft het getal in de eenheid van het effectieve type.
5. *Een binderlezing* (`vondstrecht`): ref kende bij het schrijven de typen van de Let-binders niet en schreef een lege
   `BinderRef` zonder eenheid; de knopen dragen nu hun Let-bereik, de eenheid is die van de gebonden waarde (ch. 2.10).
6. *De emmerscalair* (4: `penningjaar`, `tempototaal`, `vast-startpunt` ×2): Drools schreef `bucket[…]`, een vorm die de
   canonieke tekst niet kent; nu de tijdlijn van zijn ene periode, zoals ref (`RecordingRendering.given`).

**Tests die veranderden** (gedrag dat de kaart verandert): ref `WV-5.12-keurgemiddelde` pint nu ook de lezing van de
telling zonder voorwaarde (`/rules/rules/1`); `ReadingsCorpusWitnessTest` neemt een vorm zonder voorwaarde mee (haar
knopen zijn die van de waarde vanaf 0) en houdt de stopregel niet tegen een samenstelling die over een tijdlijn gelezen
wordt (die heeft perioden en geen oordeel, zoals een kenmerkcheck daar al overgeslagen werd). Geen test verzwakt.

**Wat de toolchain-kaart `explanation-computed-attributes` moet weten.**
- Een regel op een attribuut zonder `indien` heeft per object waarop zij loopt een `ConditionReading` onder de pointer
  van haar versie: `HOLDS` waar zij aan de beurt is, met de knopen van `positions.subtree(r.value())`, nummer 0 de wortel
  (de verschuiving van advies §3 is 0); de waarde van knoop 0 is de berekende waarde, ook leeg (`value` null), in de
  eenheid van het effectieve type (`NodeReading.unit`), vóór de schrijfgrens naar de eenheid van het doel.
- Een initialisatie zonder voorwaarde (`moet geïnitialiseerd worden op`) is `HOLDS` waar het doel leeg is in de invoer
  en geen andere schrijver het vulde, anders `DISPLACED` zonder knopen.
- Een regel zonder voorwaarde die ch. 7.10 in een stapel vouwt, wordt op de typen van die stapel gelezen als default
  van de stapel, onder de pointer van haar eigen versie: `HOLDS` waar geen hogere default besliste, anders
  `DISPLACED`; `DefaultsDecision.rootChains` noemt haar. De initialisatiedefault
  (`/rules/prioritizedDefaults/i/defaults/j`) heeft een lezing met knoop 0 haar `consequence`.
- Een kenmerktoekenning, kenmerkdefault, groepsdefault, verdeling of creatie zonder voorwaarde heeft geen lezing.
- Status `HOLDS` zegt bij een vorm zonder voorwaarde alleen "aan de beurt"; of zij schreef, staat in de waarde van knoop 0
  (leeg schrijft op een staand doel niets, ch. 7.8).
- Gewijzigd voor iedere afnemer van de knopen van een temporeel totaal: de knip-knopen dragen nu perioden zonder waarde
  (een vergelijking: segmenten met waarde), geen geknipte basistijdlijn meer; een emmerscalair komt als `List<Period>`.

**Spec.** Het voorstel staat in ch. 5.12 (`ae874142`); RUNTIME-SEMANTICS heeft geen index van ch. 5.12 of de
`WV`-voorbeelden buiten de alinea "The artifacts", die de drie nieuwe namen noemt. Voorgestelde PLAN §9-regel voor de
hoofdsessie: "readings-of-unconditional-forms (2026-10-22): een vorm zonder voorwaarde die een waarde berekent heeft de
lezing van haar waarde (ch. 5.12); zes bestaande verschillen in de lezingen van ref en Drools gefixt; voorlopige
besluiten V2 (eenheid van het effectieve type) en V3 (de gevulde telling is een telling)." Geen CONSTRUCTS-delta.

**Gates.** `werk/gates.sh` op `ffcd2713` (`werk/bruggen/u4/gates-1.summary.txt`): de zeventien gates groen,
`EXIT 0` (gate 12 945 s, gate 17 510 s). Tijdens de bouw: gate 5, gate 12 `--fast`, gate 17 (drie keer, met de bewaarde
werkmap) en de vier getuigen los, alles in `werk/bruggen/u4/`.

### Landing (hoofdsessie, 2026-10-22)

`gates-2` op `9b8689bd` (gerebased op main `c98b690d`, percentage-of-snippets): alle zeventien groen, EXIT 0
(15:37–16:08). Daarna gerebased op `d0daba6a` (de clingo-fix CLINGO-030; alleen `backends/clingo/` en één zin in
`spec/ASP-TRANSLATION.md`, geen overlap) zonder nieuwe run. PLAN §9 "A form without a condition has a reading of its
value", naar de voorgestelde regel. Geen CONSTRUCTS-delta.
