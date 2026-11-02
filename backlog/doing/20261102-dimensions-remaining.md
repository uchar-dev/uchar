# dimensions-remaining: wat de kaart dimensions-executed bij naam liet liggen

*Vastgelegd door de hoofdsessie op 2026-11-02 bij de landing van `20261102-dimensions-executed` (Result, "Wat buiten de kaart
blijft"). Elk punt is nu een weigering of grens met naam; geen stille afwijking.*

Goal: elke vorm van een dimensie die RegelSpraak of ALEF schrijft, draait in ref, clingo en beide Drools-branches, of is een
gedocumenteerde normkeuze.

## Punten

1. **`eerste`, `laatste`, `aantal` (en `filledCount` met een selectie) over cellen.** *Besluit Yeb 2026-11-02: bouwen zoals ALEF,*
   positioneel: `eerste` en `laatste` zijn de eerste en laatste gevulde cel in de labelvolgorde van de selectie, `aantal` het aantal
   gevulde cellen (ALEF `Vectorspace.java:195-201`, `tel`; test met cellen 1, leeg, 0, 0, 6, 7: eerste 1, laatste 7). Een bewuste
   afwijking van de typering van de RegelSpraak-specificatie (die beperkt ze tot datums en instanties), vast te leggen in CONSTRUCTS
   rij 28. WF-037 laat ze dan toe op een selectie; uchars eigen `first`/`last` over instanties blijven op waarde.
2. **Een cel of vouw over een rol.** Ref en clingo lezen een cel over een pad; Drools weigert het lezen (DROOLS-189) en een celdoel
   met een pad (DROOLS-100); de frontend schrijft de vorm niet (`grammar/DIVERGENCES.md`, "Scope / limitations"); ALEF heeft het
   (`EenOpMeer_AggregatieDimensies`). Een golden met bron.
3. **Een vouw over een geschreven cel van de onbegrensde vorm** in Drools (DROOLS-296, ruim: ook waar de waarde eindig is); valt
   samen met `20261102-drools-exact-arithmetic` (`ExactSum`).
4. **Het paar initialisatie plus berekening op één cel**: ref en Drools antwoorden in de defaults-lezing, clingo weigert met naam;
   geen corpusgeval en geen Drools-getuige.
5. **Een cel van een ander type dan getal** in clingo (een symbolische cellezing valt op CLINGO-002) en de `emptyFallback` van een
   dimensiesom in clingo (alleen 0).
6. **Frontend.** Te bouwen: de scenariolaag leest een cel (RGS-237, gate 16), zodat een toetsdocument cellen als invoer en
   verwachting schrijft. Blijven als gedocumenteerde grens (`grammar/DIVERGENCES.md`; de taal is daar dubbelzinnig: "en" scheidt een
   opsomming en verbindt labels), voorlopig besluit van de hoofdsessie: labels vóór het attribuut met "en"; een cel met "en" of ","
   in een operandenlijst (RGS-364 met waarschuwing); labels achter het onderwerp (RGS-308, niet de vorm van ALEF); een attribuut
   met de naam van een eenheidswoord na een lidwoord.

## Stappen

- **A** (punten 1, 4, 5): `eerste`/`laatste`/`aantal` over cellen in spec, verifier, ref, clingo, Drools en frontend; het paar
  initialisatie plus berekening op één cel in clingo; een cel van een ander type dan getal en de `emptyFallback` van een
  dimensiesom in clingo.
- **D** (punt 6, scenariolaag), parallel aan A.
- **B** (punt 2, cel over een rol in Drools en frontend, golden), na A.
- Punt 3 gaat mee met `20261102-drools-exact-arithmetic`.

Acceptance (bij oppakken aanscherpen): per punt rood eerst, goldens met absolute verwachtingen in het vierkant, of een
normbesluit met een WF-weigering; `make gates` groen.

May touch: zoals `20261102-dimensions-executed`.

## Result

### Stap A

Branch `card/dimensions-remaining`, worktree `/Users/yeb/Developer/uchar-dev/wt/dimensions-remaining`, op main `b7e3a9611`.
Commits:

- `96151e1b2` kaart naar `doing/`;
- `439a680fc` Drools: `CaseInput.CellFault` (EF-034, EF-035) is publiek met publieke `code()` en `params()`, zoals
  `CaseRunner.InputPrecisionFault` (aanvulling van de hoofdsessie; geen gedragswijziging);
- `743737cb0` eerste, laatste en aantal over cellen in verifier, ref, clingo, Drools (beide branches) en frontend, met de
  suites, het negatief `wf037-total-dimension-selection`, de NOTE- en README-rij van het negatief en de wijzervloer van gate 3;
- `d751929d9` golden `jutterswet-seizoenvouw` uitgebreid (bron, `ir.json`, drie cases, NOTE);
- `fbe1a61c4` Drools-getuige van de lezingen leest de negen vouwen van `seizoenvouw`;
- `efce47400` clingo leest het paar berekening en initialisatie op een cel (desugaring op de cel, de defaults-arm past de
  waarde aan de decimalen aan), golden `jutterswet-seizoenaanvang`, clingo-frames, Drools-getuige, `EXPECTED_PLAYED` 307;
- `03d834e06` clingo leest een cel van tekst, datum en boolean, golden `jutterswet-seizoenlogboek`, de nieuwe goldens als
  rijen van `ref/test`, `EXPECTED_PLAYED` 308;
- `34e198c41` `seizoenaanvang` initialiseert op 1 kg (de schaalaanpassing wordt zichtbaar);
- `0bb3f2760` `seizoenlogboek`: de najaarssoort kopieert de zomer (eerste en laatste tekstcel verschillen);
- `7a918c571` voorstel spec: IR-SPEC ch. 2.7, 4.3, WF-037, WF-038 en de indexrij; RUNTIME-SEMANTICS ch. 1.2, 5.8c, 5.11,
  7.7; ASP-TRANSLATION;
- `64c2f1d4c` READMEs (ref, clingo, Drools, `corpus/golden`) en de zin van DROOLS-185 (nl/en);
- `08e716151` `seizoenvouw`: de verzameling met drie labels (zie besluit 11);
- `374cc5b9f` voorstel spec: ch. 5.8c noemt die verzameling;
- het commit van dit Result.

Beschermde paden (voorstel, niet gemerged): `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/verifier/lib/checks.ml`,
`spec/verifier/lib/desugar.ml`, `scripts/check-verifier.sh`, `scripts/check-drools.sh`; `spec/ASP-TRANSLATION.md` onder het
net van `/spec/`. Buiten de may-touch-set: niets.

**Begintoets.** `make gates` op `96151e1b2` (de kaart in `doing/`, verder `b7e3a9611`): alle zeventien groen,
`out/gates/20261102-180557`. ALEF nagelezen (`872b859`): `Vectorspace.first/last/count` (regels 195-208; `count` telt de
gevulde cellen), de vertaler van `DimensieAggregatie` (`regelspraak.translator.mps`: de accumulator begint op
`BigRational.ZERO` voor `tel` en anders op null, dus een geheel lege selectie geeft voor `tel` 0 en voor `eerste`/`laatste`
leeg), `labelcombinaties` (`regelspraak/behavior.mps`: het eerste filter is de buitenste lus) en de labels van
`LabelFilterVerzameling` (`gegevensspraak/behavior.mps`: in geschreven volgorde). ALEF schrijft de vouwen als "de eerste
van", "de laatste van" en "het aantal voorkomens van" (editor van `DimensieAggregatie`, `regelspraak/editor.mps`), en
`maximaal1Dimensie` (`regelspraak/typesystem.mps` 5872-5926) laat bij eerste/laatste één dimensie met meer labels toe.

#### Wat er veranderde, per laag

- **Verifier (voorstel).** WF-037 geeft `dimensionSelections` aan `sum`, `minimum`, `maximum`, `first`, `last` en
  `filledCount` (`dimension_fold_kinds`); de zin van `WF-037.dimension-selection-kind` noemt de zes. De drie armen van de
  filled count over instanties (elementtype, ariteit, setbron) wijken voor een filled count met selecties. WF-038's
  ordeningsarm reikt niet tot een `first` of `last` met selecties (op positie, elk type). `Desugar.document` herschrijft ook
  het paar op een cel naar een defaults-groep op de lidnaam van de cel (`cell_target` is weg); alleen clingo leest dat
  document.
- **ref.** `eval_dimension_fold`: `first` en `last` nemen de eerste en laatste gevulde cel in de volgorde van
  `selected_cells`, `filledCount` telt de gevulde cellen (0 bij geen). Geen andere verandering: het paar op een cel las ref
  al in de defaults-lezing.
- **clingo.** De dimensievouw (`dimension_cells`, `expr_term`): `first`/`last` als `I = #min/#max { C : dimN(K,C,_) },
  dimN(K,I,V)`, `filledCount` als `#count` met een definitieatoom op elk onderwerp (0), een som met een willekeurige
  `emptyFallback` via een hulpatoom met twee regels (de `#sum` waar een cel gevuld is, de terugval waar geen is, beide naar
  de fijnste schaal); `member_read ~scaled:false` voor een telling over cellen van elk type. `symbolic_term`: een cel van
  tekst, enumeratie of boolean van het onderwerp, en `first`/`last` over zulke cellen (`doc_env` daarvoor naar voren
  gehaald). `Model.check`: `cellValue` van een datum, tekst, enumeratie en boolean. Het paar op een cel: de weigering ("the
  ch. 7.7 defaults reading of the pair is not translated for a cell") is weg; de defaults-arm past een gevolg aan de
  decimalen van het doel aan met `fitted` in plaats van te weigeren bij een andere schaal.
- **Drools.** `TmsTranspiler.dimensionFold` neemt `EERSTE`, `LAATSTE`, `AANTAL_GEVULDE` naast som/min/max, met
  `ComputeHelpers.firstCell`, `lastCell`, `filledCells` (`StaticFunction.CELL_FIRST`, `CELL_LAST`, `FILLED_CELLS`, beide
  weergaven); de weigering van een vouw over een geschreven onbegrensde cel (DROOLS-296) geldt alleen nog voor som/min/max.
  DROOLS-185 (nu alleen nog `total`/`count`) noemt de zes soorten. `CaseInput.CellFault` publiek. Voor het paar op een cel
  veranderde niets (Drools antwoordde al); nieuw is de getuige.
- **Frontend.** `eersteLaatsteFunc` neemt `dimensieSelectie*`; nieuw token `AANTAL_VOORKOMENS_VAN` en alternatief
  `aantalVoorkomensFunc` (`dimensieSelectie+`) → `AANTAL_GEVULDE`. `ExpressionBuilder`: de selecties van die twee vormen,
  geen RGS-067 (setlezing) voor "het aantal voorkomens van", geen datumtypering (`dateAggregation`) voor eerste/laatste met
  selecties. `grammar/DIVERGENCES.md` "More dimensions": de vormen, de inertheidsmeting, de lezing, de grens.
- **Corpus.** Zie hieronder. Negatief `wf037-filled-count-dimension-selection` (een filled count over instanties mét
  selectie) vervangen door `wf037-total-dimension-selection` (een `total` met selectie), `corpus/negative/README.md` (twee
  rijen).
- **Spec (voorstel).** IR-SPEC ch. 2.7 (de normbron: ook waar de typering ALEF tegenspreekt volgt het dialect ALEF), ch. 4.3
  (de rij `dimensionSelections`, de filled count over cellen in plaats van "postponed, not rejected"), WF-037, WF-038, de
  indexrij van WF-037. RUNTIME-SEMANTICS ch. 1.2 (de rij van de dimensievouwen; eerste/laatste zijn ordeningsvouwen over
  instanties en opgesomde operanden), ch. 5.8c punt 2 (het paar op de draad; een uitvoerder die verlaagt, doet dat op de
  cel), punt 3 (de zes soorten, de volgorde van het product, ALEFs grens niet overgenomen), uitgewerkte voorbeelden (ALEFs
  twee tests op `kustvak`, twee dimensies, `seizoenvouw`, `seizoenlogboek`, `seizoenaanvang`), "Where the executors
  stand"; ch. 5.11 (de rij van de dimensievouw: eerste en laatste zijn de getuige van de volgorde); ch. 7.7. ASP-TRANSLATION:
  het celdoel en het paar, de cellezing (symbolisch), de dimensievouw (zes soorten, terugval), de verwijzing naar het
  negatief, de vervallen weigering in de lijst van S19.

**De volgorde bij meer dimensies** (vastgelegd in ch. 5.8c, motivatie): de cellen in de volgorde van het product van de
selecties, de eerste selectie (de eerste gedeclareerde dimensie) buitenste lus en elke selectie in haar eigen labelvolgorde;
dat is de volgorde waarin elke dimensievouw al leest (ch. 5.11), de afhankelijkheidsgraaf haar cellen noemt en ALEFs
`labelcombinaties` loopt. Binnen ALEFs grens (één dimensie met meer labels) geeft dat dezelfde uitkomst als ALEF.

#### Goldens en cases

- `jutterswet-seizoenvouw` (uitgebreid, bron): vijf regels erbij (`openingsgewicht` eerste, `slotgewicht` laatste,
  `najaarsvoorrangsgewicht` eerste over `{najaar, zomer en voorjaar}`, `aantal vondstseizoenen` "het aantal voorkomens
  van", `basisgewicht` som "of 0,50 kg als die er niet zijn"); case1 5 → 10 verwachtingen, case2 9 → 19, nieuw
  `case3-nul-in-zomer.json` (10; J4 `zomer` 0.00, `najaar` 1.50, `voorjaar` leeg). Lege cel: J1 `zomer`, J4 `voorjaar`;
  geheel lege selectie: J2.
- `jutterswet-seizoenaanvang` (nieuw, bron): het paar op de cel `najaar` (kopie van `voorjaar`, initialisatie op 1 kg) en
  de som; `case1-berekening-en-aanvang.json` (9): J1 2.00/4.00, J2 1.00/1.00, J3 5.00/5.00, J4 3.00/6.00.
- `jutterswet-seizoenlogboek` (nieuw, bron): cellen van tekst, datum en boolean (lezen, een tekstcel schrijven, eerste over
  tekst, aantal over tekst, laatste over datum, een vergelijking van een booleancel); `case1-soort-datum-vergunning.json`
  (17).

Alle drie: verifier `PASS`, ref en clingo `PASS`, Drools `PLAYED` op beide takken, elke case valt onder `--noop`.

#### Rood op `b7e3a9611` en mutaties

Rood (detached werkboom op `b7e3a9611` met de nieuwe tests of documenten, opgeruimd; logs
`/Users/yeb/Developer/uchar-dev/werk/dimensions-remaining/logs/`):

- `ref/test` (`stapA-red-reftest-1.log`): `WV-5.8c-eerste-laatste-aantal` en `-twee-dimensies` rood, elk niet
  verifier-schoon en met de fout "a dimension selection on a filledCount aggregation: WF-037 admits one on sum, minimum and
  maximum".
- clingo-suite (`stapA-red-clingotest-1.log`, `-2.log`): de suite stopt op die fout van ref (exit 2) bij het eerste nieuwe
  frame. Het paar op een cel apart (`stapA-red-pair-1.log`): oude clingo `TRANSLATION FAULT: the initialisation of the cell
  "vondstgewicht[najaar]" sits beside a computing rule …`, ook voor het voorwaardelijke paar; nieuw `PASS`.
- Drools (`stapA-red-drools-witness-1.log`): de nieuwe rij van `DimensionCellWitnessTest` `EXCLUDE` (DROOLS-185).
- Frontend (`stapA-red-fe-dimfold-1.log`): de zes nieuwe tests van `DimensionFoldTest` rood, 16 bestaande groen.
- `seizoenvouw` (`stapA-red-golden-vouw-1.log`): verifier `WF-037.dimension-selection-kind` (4×) en
  `WF-037.filled-element-type`, ref `EVALUATION FAULT`, clingo `CLINGO-008`, Drools `EXCLUDE DROOLS-185` op beide takken,
  frontend vijf syntaxfouten. De terugval alleen (`stapA-red-fallback-1.log`): oude clingo `a dimension fold with an
  emptyFallback other than 0 on a sum is not translated`, ref en Drools al groen.
- `seizoenaanvang` (`stapA-red-golden-aanvang-1.log`): verifier, ref, Drools en frontend al groen; clingo `TRANSLATION
  FAULT: the case supplies input for computed target "vondstgewicht[najaar]"`.
- `seizoenlogboek` (`stapA-red-golden-logboek-1.log`, `stapA-red-cells-other-types-1.log`): heel rood door eerste/laatste/
  aantal; per vorm zonder die regels: clingo `CLINGO-002` op de tekstcel en de booleancel, een `cellValue` van tekst of
  datum "this encoding carries a cell of a number"; een datumcel las clingo al.

Mutaties, elk één vervanging in een `git worktree add --detach` (opgeruimd), recept
`/Users/yeb/Developer/uchar-dev/werk/dimensions-remaining/mutants/stapA-mutate.py` en `stapA-mutate-extra.py`, uitkomst
`logs/stapA-mutants-1.log` tot `-4.log`; niet gecommit, elk rood:

| | bewaking | mutatie | rood |
|---|---|---|---|
| V1 | WF-037 laat de drie toe | `dimension_fold_kinds` = som/min/max | `seizoenvouw`: `WF-037.dimension-selection-kind` 4× |
| V2 | filled count over cellen zonder elementtype | uitzondering weg | `seizoenvouw`: `WF-037.filled-element-type` |
| V3 | WF-038 ordent eerste/laatste over cellen niet | uitzondering weg | `seizoenlogboek`: `WF-038.ordering-fold-operand` (tekst) |
| R1 | ref: eerste gevulde cel | `List.rev` | `-J1` (openingsgewicht 2 voor 3), `-eerste-laatste-aantal`, `-twee-dimensies` |
| R2 | ref: alleen gevulde cellen tellen | alle cellen | vier rijen (3 voor 2, 6 voor 5, …) |
| C1 | clingo: kleinste index | `#max` | twee frames (7 voor 1; 100 voor 200) |
| C2 | clingo: telling 0 op geen | definitie uit de helper | twee frames (`vondstvakken`/`vondstcellen` leeg voor 0) |
| C3 | clingo: terugval alleen zonder gevulde cel | `not dimN(K,_,_)` weg | `seizoenvouw`: `basisgewicht = [500;50]` |
| D1 | Drools: eerste cel | eerste sleutel overgeslagen | witness: eerste vondst 5.00 voor 2.00 |
| D2 | Drools: gevulde cellen | alle sleutels | witness: vondstcellen 6 voor 3 |
| F1 | grammatica: selecties bij eerste/laatste | `dimensieSelectie*` weg (accessor leeg) | 4 tests; de eerste poging zonder de accessor compileerde niet |
| F2 | "het aantal voorkomens van" is `AANTAL_GEVULDE` | `AANTAL` | 2 tests |
| F3 | geen RGS-067 op die vorm | uitzondering weg | 2 tests |
| F4 | geen datumtypering op eerste over cellen | uitzondering weg | 3 tests |
| P1 | desugaring van het paar op een cel | celfilter terug | `seizoenaanvang`: `TRANSLATION FAULT … computed target` |
| P2 | defaults-arm past de schaal aan | geen aanpassing | `seizoenaanvang`: J2 `[1]` voor 100 |
| S1 | symbolische cellezing | arm uit | `seizoenlogboek`: `CLINGO-002` |
| S2 | symbolische eerste: kleinste index | `max` | `seizoenlogboek`: eerste soort "wrakhout" voor "barnsteen" |
| M1 | `cellValue` van tekst leest het lid van de cel | het attribuut | `seizoenlogboek`: drie celverwachtingen leeg |

Parse-boom (`scratch/parsetree/ParseTrees.java`, de parser vóór en na naast elkaar): onder `regelSpraakDocument` en
`testDocument` gelijk voor alle 271 `.rgs`-bestanden van de werkboom en de TOKA-fragmenten als één document, op het
uitgebreide `seizoenvouw` na; de oude `seizoenvouw`-bron gelijk.

#### Aangepaste verwachtingen, tests, pins en vloeren (oud → nieuw)

- Negatief `wf037-filled-count-dimension-selection` (filled count met selectie geweigerd) → `wf037-total-dimension-selection`
  (een `total` met selectie geweigerd): de kaart draait de weigering om. Gate 3 `EXPECTED_MIN_POINTERS` 706 → 705 (voorstel):
  het oude rapport noemde twee wijzers, het nieuwe één. Andere vloeren van gate 3 gelijk (568 negatief, 411 armen, …).
- Zin `WF-037.dimension-selection-kind`: "dimension selection on a kind other than sum/min/max" → "… other than sum,
  minimum, maximum, first, last or filledCount"; de zin van ref voor een andere soort en DROOLS-185 (nl/en) idem.
- clingo-frame "the initialisation of a cell beside a computing rule on that cell" (`faults_with`, de weigering) → `ref_matches`
  en `clingo_matches` op J1 2.00, J2 0.00, J3 5.00, J4 3.00: de kaart draait dit om; nieuw frame voor het paar buiten het
  domein (J1 2.00; J2 EF-016 in ref, de getuige "no fixpoint" in clingo).
- `DimensionCellWitnessTest.the_cell_and_the_fold_are_the_nodes_of_a_reading`: de lezingen van `seizoenvouw` 4 vouwen → 9
  (en de literal van de terugval als eigen knoop); de golden kreeg vijf regels.
- `seizoenvouw`: case1 5 → 10, case2 9 → 19 verwachtingen, case3 nieuw; geen bestaande verwachting veranderd.
- Gate 12 `EXPECTED_PLAYED` 306 → 308 (voorstel), met reden; `EXPECTED_CHANNEL` gelijk (47); DRL-teksttak 306 → 308.
- Tellingen (oud: `out/gates/20261102-180557`, nieuw: `out/gates/20261102-190122`): gate 2 411 → 413 bestanden, 54872 →
  55178 objecten in 979 → 981 documenten; gate 3 411 → 413 golden schoon; gate 4 1053 → 1056 cases; gate 5 1850 → 1860
  voorbeelden, 982 → 985 cases; gate 6 2280 → 2289 checks, 982 → 985 cases; gate 7 937 → 940, 45 overlevers; gate 8
  gelijk (71 rode gevallen, 133 codechecks, 209 parameterchecks; Drools-leg 51, 79, 20, 18); gate 10 1402 → 1408 tests, 163
  → 165 rgs-goldens, 359 codes; gate 12 JUnit 1759 → 1761, 299 codes; gate 13 70 → 75 getwiste annotaties over 32 → 33
  goldens, 174 → 180 replays, DRL-tak 31 → 32, 152 → 159 annotaties met eenheid; gate 15 316/353 → 318/355 vergeleken, 280 →
  284 afhankelijkheden, route 163 → 165; gate 16 gelijk (38, 131); gate 17 982 → 985 getraceerd, 306 → 308 goldens, 852 → 855
  case-bestanden, 6891 → 6941 lezingen, 22434 → 22491 knoopregels, 3800 → 3801 `unreached`, afwijkingen 0.

#### Voorgestelde tekst voor CONSTRUCTS (hoofdsessie)

Rij 28: in de noot van 2026-11-02 vervalt "where the specification's typing contradicts ALEF (`first`, `last`, `count` over
cells) the dialect admits no fold", en in "Still refused by name" vervalt "the pair initialisation plus calculation on one
cell in clingo". Daarachter:

> **Landed (card `dimensions-remaining`, step A, 2026-11-02, `743737cb0` … `374cc5b9f`; owner's decision 2026-11-02):** a
> deliberate deviation from the typing of RegelSpraak v2.3.0, which types `de eerste van`/`de laatste van` over dates by
> value and `het aantal` over instances: over the cells of a dimensioned attribute the dialect follows ALEF. `first` and
> `last` with `dimensionSelections` pick the first and the last filled cell by position, in the order of the product of the
> selections (the first selection outermost, a set in its written order), over cells of any type, and `filledCount` with
> `dimensionSelections` counts the filled cells, 0 where none is (ALEF's eerste, laatste, tel). The kinds keep their names;
> over instances and listed operands `first` and `last` keep their reading by value. ALEF admits one dimension with more
> than one label under a first or a last; the dialect admits more, in the order of the product. WF-037 grants
> `dimensionSelections` to `sum`, `minimum`, `maximum`, `first`, `last`, `filledCount`; WF-038's ordering clause does not
> reach a positional first or last. The RegelSpraak frontend writes "de eerste van" and "de laatste van" with selections and
> ALEF's "het aantal voorkomens van" (`grammar/DIVERGENCES.md`). The ASP backend reads the pair initialisation plus
> calculation on one cell in the defaults reading (the ch. 7.7 desugaring on the cell), a cell of a text, an enumeration and
> a boolean of the subject, and a sum's `emptyFallback` other than 0. Goldens `jutterswet-seizoenvouw` (extended),
> `jutterswet-seizoenaanvang` and `jutterswet-seizoenlogboek` play in the square ref ≡ clingo ≡ Drools; the negative
> `wf037-filled-count-dimension-selection` gave way to `wf037-total-dimension-selection`.

Rij `DimensieSelectie` (:102): "**Landed (`dimensions-remaining`, stap A):** ook op `first`, `last` en `filledCount`."

#### Voorlopige besluiten

1. **Bestaande soorten met selectie, geen eigen naam.** `first`, `last` en `filledCount` met `dimensionSelections`: de
   dimensievouw is al een eigen vorm (haar bron is het product van cellen in normatieve labelvolgorde, ch. 2.7), de soorten
   heten zoals RegelSpraak en ALEF ze noemen, geen oud document verandert van betekenis (WF-037 en elke uitvoerder weigerden
   de combinatie), en er komt geen nieuwe enumwaarde met v0-spelling in schema, `Spelling.java` en de naamtabel. De prijs: een
   lezer moet weten dat `first`/`last` met selecties op positie kiest; ch. 1.2 en ch. 4.3 zeggen het bij de soort.
2. **`aantal` over cellen is `filledCount`, niet `count`**: een cel is geen instantie, en het aantal cellen van een selectie
   zonder te kijken naar gevuld is een constante; `count` met selectie blijft geweigerd.
3. **Meer dimensies bij eerste/laatste: de productvolgorde, geen weigering** (opdracht); ALEFs `maximaal1Dimensie` niet
   overgenomen, ook niet in de frontend; vastgelegd in ch. 5.8c en `DIVERGENCES.md`.
4. **Eerste/laatste over cellen van elk type** (ALEF): de WF-038-uitzondering en de frontend zonder datumtypering;
   `seizoenlogboek` draagt een eerste over tekstcellen.
5. **De frontend schrijft ALEFs "het aantal voorkomens van"**, als frasetoken; de vorm van §13.4.17.50 ("het aantal" + cel +
   selectie) en "het aantal gevulde waarden van" met selectie leest ze niet.
6. **Het paar in clingo via de desugaring op de cel** in plaats van een eigen inheemse lezing: het herschreven document is
   alleen van clingo, de groep noemt het lid `vondstgewicht[najaar]`, zoals ref en clingo een cel als lid houden. Daarvoor
   past de defaults-arm een gevolg aan de schaal van het doel aan (`fitted`); dat verbreedt de arm ook voor een attribuut
   (een eerdere weigering wordt een antwoord, gelijk aan ref).
7. **De weigering van het paar op een cel is helemaal weg**: buiten het domein (een initialisatie met voorwaarde) leest de
   inheemse arm een cel zoals een attribuut, met dezelfde fout als ref (EF-016 tegenover de getuige "no fixpoint").
8. **Symbolische cellen alleen van het eigen onderwerp**; over een pad blijft het `CLINGO-002` (punt 2 van de kaart, stap
   B). Een berekend doel van type boolean blijft in clingo geweigerd (een bestaande grens, ook voor een attribuut), dus
   `seizoenlogboek` leest de booleancel in een voorwaarde.
9. **Drools-hulpen in `ComputeHelpers`** (`firstCell`, `lastCell`, `filledCells`), niet in de productieruntime, zoals
   `withCell`; `rsDimSom/Min/Max` ongewijzigd.
10. **`seizoenaanvang` initialiseert op 1 kg**, zodat een vergeten schaalaanpassing een ander getal geeft (mutant P2).
11. **De verzameling in `seizoenvouw` heeft drie labels** (`{najaar, zomer en voorjaar}`): een verzameling van twee met
    "en" leest de resolver als twee labels, maar de semantische tokens lossen dat lid niet op
    (`SemanticTokensCorpusTest`, gate 10, eerste run rood op `64c2f1d4c`); die grens van de tokens is niet van deze kaart.
12. **`CellFault` publiek** (aanvulling van de hoofdsessie).

#### Wat niet lukte of buiten stap A bleef

- Geen poging die twee keer mislukte. De eerste `make gates` (`stapA-gates-2.log`, `out/gates/20261102-185511`) was rood
  op gate 10 (besluit 11); daarna groen.
- Niet gebouwd: een symbolische cel of eerste/laatste over een pad in clingo; een `cellValue` van een datum-tijd, constructor,
  record of lijst in clingo (fout met naam); de semantische tokens van een verzamelingslid "x en y".
- `CLAUDE.md` noemt bij gate 3 nog de vloeren van 2026-10-19 (`EXPECTED_MIN_POINTERS` 619); niet aangeraakt.
- De indexrij van WF-037 in IR-SPEC zegt nog "The filled count has no golden yet" over de filled count over instanties;
  niet nagegaan, niet veranderd.

#### `make gates`

Laatste run op `374cc5b9f` (dit Result zonder deze alinea; de kaart is geen invoer van een gate),
`logs/stapA-gates-3.log`, `out/gates/20261102-190122/summary.txt`:

```
start 2026-11-02 19:01:22 374cc5b9f card/dimensions-remaining
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 217 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 537 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 121s
04-case-schema rc=0 7s
03-verifier rc=0 36s
05-oracle rc=0 20s
06-clingo rc=0 256s
07-noop rc=0 13s
08-red rc=0 157s
09-catala rc=0 5s
10-regelspraak rc=0 216s
11-toka-parity rc=0 213s
12-drools rc=0 309s
13-reftype rc=0 264s
14-flogic rc=0 1s
15-graph rc=0 45s
16-scenario rc=0 36s
17-readings rc=0 232s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 19:06:31 309s red=0
```

Niet gemerged, niet gepusht. De kaart blijft in `doing/`.

### Stap D (de scenariolaag leest een cel)

*Samengevoegd in stap M (zie daar): op `card/dimensions-remaining` heten de drie commits `243eead3b`, `ad900fe29` en
`dd7430cc6`, en RGS-380 heet RGS-369 (`f5c455073`). De tekst hieronder is het Result van stap D zoals geschreven.*

Branch `card/dimensions-scenario`, worktree `/Users/yeb/Developer/uchar-dev/wt/dimensions-scenario`, op main `b7e3a9611`.
Commits:

- `e41193929` frontend: scenariolaag (`ScenarioResolver`, `ResolvedScenario`, `CaseEmitter`), de grammaticaregel
  `naamVervolgDeel`, de semantische tokens van een cel, register en catalogi (RGS-237 weg, RGS-380 nieuw),
  `grammar/DIVERGENCES.md`, `ScenarioCellTest` en de omgedraaide test in `ScenarioTypeGateTest`;
- `9bf0fbf31` goldens: `scenario.rgs` bij `jutterswet-strandseizoen` en `jutterswet-seizoenboeking`, hun `NOTE.md` en
  `corpus/golden/README.md`;
- `9d2f091e7` frontend: getuige voor de keuze tussen gedeeltelijke cellezingen.

Beschermde paden: geen aangeraakt (geen `scripts/`, geen `spec/`). Buiten de opgegeven set, met reden:
`SemanticTokens.java` (de pas over de scenariolaag; zonder die wijziging faalt `SemanticTokensCorpusTest` op de twee nieuwe
toetsdocumenten, want de cel zou `unresolved` zijn), de grammaticaregel `naamVervolgDeel` (alleen gebruikt door de
scenariolaag, `begrensdeNaam`), `corpus/golden/README.md` (één zin per golden). Een map `.../scenario/` bestaat niet; de
scenariolaag is `resolver/ScenarioResolver.java`, `ir/ResolvedScenario.java` en `emit/CaseEmitter.java`.
`ExpressionBuilder.java` is niet aangeraakt (stap A werkt daar).

**Begintoets.** Op `b7e3a9611` (`logs/stapD-red-golden-1.log`): elke cel in een bullet geeft RGS-236 ("kent geen attribuut
'vondstgewicht in voorjaar'"), een kaal gedimensioneerd attribuut RGS-237; geen syntaxfout voor de vormen zonder `van`-label.
Het advies (§4: de scenariolaag pas als een golden met cellen een `scenario.rgs` krijgt) klopt.

#### Wat er veranderde

- **Lezen van een naam in een bullet** (`ScenarioResolver.member`). Een naam in een situatiebullet en in een verwachting
  leest zoals een regel een cel leest: eerst een gedeclareerd attribuut bij zijn naam; een gedimensioneerd attribuut alleen
  als cel, via `ExpressionBuilder.cellReadings` (labels vóór het attribuut bij een dimensie zonder voorzetsel, erachter na
  het voorzetsel, naast elkaar of verbonden met "en" en ",", in elke volgorde, labels van meer woorden). De keuze tussen
  lezingen is die van `ExpressionBuilder.dimensionCell`, gespiegeld: het langste attribuut wint; past de naam op twee cellen,
  dan RGS-365; noemt de beste lezing van een dimensie geen label, dan RGS-364 per ontbrekende dimensie (ook voor een kaal
  gedimensioneerd attribuut). De fouten dragen het kader `scenario: ` zoals elke fout van de laag.
- **Invoer.** De cellen van één attribuut zijn één `AttributeValue` met `cells` (nieuw record `Cell(labels, value)`), de
  labels per positie in gedeclareerde volgorde. De samenvoeging werkt per cel: een latere toekenning van dezelfde cel
  vervangt de eerdere op haar plaats, de andere cellen blijven, in volgorde van eerste voorkomen; ook binnen één blok. Een
  samengevoegde lijst is een eigen kopie, want één blok gaat in elk scenario dat op zijn situatie gebaseerd is.
- **Verwachting.** Nieuwe soorten `CELL_VALUE` en `CELL_EMPTY` met het veld `labels` op `Expectation`; `CaseEmitter`
  schrijft `cellValue` en `cellEmpty` in de sleutelvolgorde van het case-schema (`kind`, `label`, `attribute`, `labels`,
  `value`) en de invoer als `cellsValue` (`cells` → `labels`, `value`).
- **Geen case-vorm.** Een cel met een periode (invoer), een periodeverwachting op een cel en `is altijd leeg` op een cel:
  ERROR **RGS-380** `scenario-cell-period` (parameters `attribute`, `written`), nl en en.
- **Register en catalogi.** RGS-237 `scenario-attribute-dimensioned` uit het register en uit beide catalogi (geen plaats
  roept het nog aan; gate 10 eist dat elke code gebruikt wordt); RGS-380 erbij. 359 codes (gelijk), 377 → 380 plaatsen.
- **Grammatica.** `naamVervolgDeel : naamWoordLower kaleNaamWoord* ( labelOpsomming | uitLabel )*? ;` (was zonder de lus):
  een segment na `van` in een bulletnaam neemt de verbonden labels zoals `kaleNaamDeel` sinds stap 5b, zodat "de winst van
  vorig boekjaar en op noord" parseert. Inertheid gemeten met een parseboomvergelijking (`ParseTreeDump`, gereedschap in de
  scratchpad, niet bewaard): parser van `b7e3a9611` en van de branch, `toStringTree` onder `testDocument`, voor de 217
  `.rgs`-bestanden van `corpus/`, `toka/` en `frontends/` buiten de `scenario.rgs` en voor de 40 golden-toetsdocumenten
  (`source.rgs` + `scenario.rgs` als één document): 514 regels, `diff` leeg.
- **Semantische tokens** (`SemanticTokens.member`). Een bulletnaam die als cel leest, wordt woord voor woord gelezen zoals
  regeltekst (`words`, met het objecttype van de instantie als subject): het attribuut `attribute`, elk label
  `dimensionLabel`, voorzetsels en "en" houden hun lexicale soort.
- **`grammar/DIVERGENCES.md`**, "Scenariotoetsen": de cel onder Surface (met de meting), twee rijen in de tabel van
  verwachtingen, "Cells" onder Resolution, de lowering van `cells`/`cellValue`/`cellEmpty`, de weigeringen (de zin van
  RGS-237 vervangen door die van RGS-364, RGS-365 en RGS-380), de getuigen (`ScenarioCellTest`, elf goldens);
  "Semantic tokens", "A test document": de cel in een bullet.

#### De goldens

Geen bestaande invoer, verwachting of `ir.json` veranderd; alleen `scenario.rgs` erbij, die de met de hand geschreven cases
van stap 3, 5 en 6 in RegelSpraak schrijft.

- `jutterswet-strandseizoen/scenario.rgs`: twee scenario's, `twee stranden` (J1, zes invoercellen, 11 verwachtingen, waarvan
  2 `cellValue`) en `lege cellen` (J2 met twee invoercellen, J3 zonder, 21 verwachtingen, waarvan 4 `cellValue`). Vormen:
  naast elkaar (`in voorjaar op noord`), omgekeerd (`op noord in zomer`), met "en" (`op noord en in najaar`), met ","
  (`in voorjaar, op zuid`, `op zuid, uit rivier`), na `uit` (`op noord uit zee`, `uit rivier en op noord`), en in de
  verwachtingen `uit zee en op zuid`, `op zuid en uit rivier`.
- `jutterswet-seizoenboeking/scenario.rgs`: drie scenario's (`voorjaar en zomer` 9 verwachtingen, `zonder invoer` 6,
  `invoer in een geschreven cel` 8), cellen van één dimensie als invoer, `cellValue` en één `cellEmpty`.

Gate 16 regenereert de 5 case-bestanden en beide `ir.json` byte-gelijk. Ref, clingo en beide Drools-branches spelen de
cases al sinds stap 4b en 6 (gates 5, 6, 12 en 17 ongewijzigd groen: gate 12 306 gespeeld op beide takken).

#### Rood en mutaties

Rood op `b7e3a9611`, in een `git worktree add --detach` met de nieuwe testbestanden en de twee `scenario.rgs` erin
(opgeruimd; `logs/stapD-red-tests-2.log`, `-3.log`): van `ScenarioCellTest` 12 van 13 rood, elk op RGS-236 of RGS-237
waar een cel of RGS-364/365/380 verwacht werd; groen bleef `a_name_with_a_label_of_no_dimension_names_no_attribute`, een pin
van bestaand gedrag (RGS-236 voor "in winter op noord"). Rood ook de omgedraaide test in `ScenarioTypeGateTest` en
`SemanticTokensCorpusTest.every_golden_test_document_is_covered_and_resolved` (60 problemen: de ERRORs en de `unresolved`
cellen van de twee goldens). De goldens: gate 16 op de frontend van `b7e3a9611` `LOWER FAIL` voor beide (30 RGS-236,
`logs/stapD-red-golden-1.log`). Groen: de drie klassen 69/69, de hele frontendsuite 1412 (`logs/stapD-fe-suite-1.log`,
vóór de laatste getuige) en 1415 in gate 10.

Mutaties, één voor één in een `git worktree add --detach` van `9d2f091e7` met `git checkout -- .` ertussen (opgeruimd),
recept `/Users/yeb/Developer/uchar-dev/werk/dimensions-remaining/mutants/stapD/mutateD.py` en `runD.sh`, uitkomst
`logs/stapD-mutants-2.log` (`-1.log` liep met de verkeerde werkmap voor de corpusgetuige en is afgebroken); per mutant
`ScenarioCellTest`, `ScenarioTypeGateTest`, `SemanticTokensCorpusTest`, bij C1 en E1 ook gate 16. Niet gecommit:

| | bewaking | mutatie | rood |
|---|---|---|---|
| G1 | labels na een `van`-segment | `naamVervolgDeel` zonder lus | `the_labels_are_read_in_every_form_of_a_cell` (syntaxfout bij "van vorig boekjaar en") |
| B1 | kaal gedimensioneerd attribuut geweigerd | als scalair gelezen | `a_dimensioned_attribute_without_its_labels…`, `ScenarioTypeGateTest.a_dimensioned_attribute_names_a_label…` |
| C1 | een naam leest als cel | geen cellezing | 11 celtests, `SemanticTokensCorpusTest` (30 problemen); gate 16 `LOWER FAIL` voor beide goldens |
| A1 | RGS-365 | de eerste van twee cellen | `a_cell_that_fits_two_ways_is_refused` |
| L1 | langste attribuut wint | alle volledige lezingen tellen | `the_longest_attribute_wins` (RGS-365) |
| P1 | RGS-364 bij een gedeeltelijke lezing | RGS-236 | `a_cell_without_a_label…`, `a_partial_cell_is_refused…` |
| P2 | gedeeltelijk: langste attribuut | eerste lezing | `a_partial_cell_is_refused…` (`attribute=buit`) |
| P3 | gedeeltelijk: minste ontbrekende dimensies | geen voorrang | `a_partial_cell_is_refused…` (`dimension=glans` erbij) |
| M1 | samenvoegen per cel | hele attribuut vervangen | `the_cells_of_an_attribute_merge_cell_by_cell`, `a_situation_gives_its_cells…` |
| M2 | kopie bij samenvoegen | lijst van het blok aangevuld | `a_situation_gives_its_cells_to_every_scenario_alike` |
| I1 | dezelfde cel op haar plaats | altijd achteraan | `the_cells_of_an_attribute_merge_cell_by_cell` |
| R1 | RGS-380 op invoer | periode genegeerd | `a_cell_has_no_period` |
| R2 | RGS-380 op `altijd leeg` | door naar de tijdlijntoets | `a_cell_has_no_period` (RGS-273) |
| R3 | RGS-380 op een periodeverwachting | periode genegeerd | `a_cell_has_no_period` |
| E1 | `cellEmpty` met labels | zonder labels geschreven | 3 celtests; gate 16 `DIFF` op `jutterswet-seizoenboeking/case2-zonder-invoer.json` |
| S1 | semantische tokens van een cel | als één naam | `the_semantic_tokens_read_a_cell…`, `SemanticTokensCorpusTest` (30 problemen) |

#### Aangepaste verwachtingen, pins en vloeren (oud → nieuw)

- `ScenarioTypeGateTest.a_dimensioned_attribute_has_no_case_form` → `a_dimensioned_attribute_names_a_label_of_every_dimension`,
  door de kaart omgedraaid. Oud: `- het seizoensgewicht is 3,00 kg` geeft "het attribuut 'seizoensgewicht' is
  gedimensioneerd, en een casebestand draagt geen dimensiecoördinaten" (RGS-237). Nieuw: dezelfde regel geeft "'seizoensgewicht'
  noemt geen label van de dimensie 'seizoen' van het gedimensioneerde attribuut 'seizoensgewicht'; een cel noemt van elke
  dimensie één label" (RGS-364). De bewaking (geen kaal gedimensioneerd attribuut in een casebestand) blijft.
- RGS-237 ingetrokken (register en catalogi); RGS-380 nieuw.
- Geen pin of vloer veranderd. Gate 16 `EXPECTED_MIN_SCENARIO_GOLDENS=7` en `EXPECTED_MIN_SCENARIO_CASES=31` blijven (vloeren,
  geen gelijkheden): gemeten 38 → 40 goldens en 131 → 136 case-bestanden. `SemanticTokensCorpusTest`-vloeren ongewijzigd
  (groei). Tellingen: gate 10 1402 → 1415 tests, 163 rgs-goldens, 359 codes, 377 → 380 plaatsen; gates 2–9, 11–15 en 17
  gelijk aan de laatste run van stap 6 (gate 4 1053 cases, gate 5 1850 voorbeelden en 982 cases, gate 12 306 gespeeld,
  gate 17 306 goldens en 852 case-bestanden, 6891 lezingen, afwijkingen 0).

#### Voorgestelde tekst voor CONSTRUCTS (hoofdsessie)

Rij 28, na de noot van stap 6:

> **Landed (stap D van `dimensions-remaining`, 2026-11-02, `e41193929`, `9bf0fbf31`, `9d2f091e7`):** de scenariolaag van de
> RegelSpraak-frontend schrijft een cel als invoer en als verwachting, in de vormen waarin een regel een cel leest (labels
> vóór het attribuut zonder voorzetsel, erachter na hun voorzetsel, naast elkaar of verbonden met "en" en ","); `--cases`
> zet ze om in `cells`, `cellValue` en `cellEmpty`. Een gedimensioneerd attribuut zonder label van elke dimensie of met
> labels die op twee cellen passen krijgt de weigering van een regel (RGS-364, RGS-365), een cel met een periode of
> tijdlijn RGS-380; RGS-237 is ingetrokken. `jutterswet-strandseizoen` en `jutterswet-seizoenboeking` dragen een
> `scenario.rgs` die hun cases en `ir.json` byte-gelijk regenereert (gate 16).

#### Voorlopige besluiten

1. **RGS-364 en RGS-365 ook in de scenariolaag**, met het kader `scenario: `: het is hetzelfde gebrek als in een regel, met
   dezelfde zin en parameters. Een kaal gedimensioneerd attribuut krijgt één RGS-364 per dimensie, waar het in een regel
   aan de verifier wordt gelaten (WF-125): een casebestand kan het niet dragen. RGS-237 ingetrokken in plaats van herschreven:
   zijn zin klopt niet meer, en een code krijgt geen andere betekenis.
2. **Eén nieuwe code RGS-380** voor een periode of tijdlijn op een cel, ongeacht het raster van het attribuut: in een
   document dat de verifier aanneemt heeft een gedimensioneerd attribuut geen tijdlijn (WF-125), maar de frontend weigert
   de declaratie met beide niet, en de laag mag dan geen tijdlijn op een gedimensioneerd attribuut schrijven. **Nummer 380**
   laat 369–379 vrij voor stap A in hetzelfde register (gaten zijn toegestaan; zoals stap 5 vanaf 370 nummerde);
   hernummeren bij de samenvoeging mag.
3. **De keuze tussen cellezingen gespiegeld in `ScenarioResolver.member`**, niet uit `ExpressionBuilder.dimensionCell`
   gehaald: zo raakt stap D `ExpressionBuilder` niet, waar stap A werkt. De spiegel is bewaakt door
   `the_longest_attribute_wins` en `a_partial_cell_is_refused_for_what_its_best_reading_leaves_out` (L1, P2, P3). Bij de
   samenvoeging kan de keuze één methode worden.
4. **Samenvoegen per cel, ook binnen een blok**, zoals voor een scalair attribuut: dezelfde cel twee keer is de laatste
   waarde op de eerste plaats, dus een toetsdocument schrijft nooit een dubbele cel (EF-035 ontstaat er niet).
5. **Eigen soorten `CELL_VALUE` en `CELL_EMPTY`** met `labels` op `Expectation`, en `cells` als derde variant van
   `AttributeValue`, in plaats van `ATTRIBUTE_VALUE` met labels: een consument die op de soort schakelt, neemt een cel dan
   niet voor een attribuut.
6. **Een celwaarde wordt getypeerd tegen het attribuut** en heet in een typeerfout "het attribuut '<a>'" (geen nieuw
   `PART` voor een cel).
7. **Twee goldens in plaats van één**: `strandseizoen` draagt de vormen over twee dimensies, `seizoenboeking` de
   `cellEmpty`.

#### Wat niet lukte of buiten stap D bleef

- Niets mislukt. Eén runner-fout gecorrigeerd: de eerste mutatierun las `SemanticTokensCorpusTest` uit de werkmap van de
  hoofdcheckout; afgebroken op PID, herhaald vanuit de frontendmap (`-2.log` geldt).
- Grenzen die blijven (de vormen die een regel ook niet leest): labels vóór het attribuut met "en"; een cel over een rol
  (een situatie kent alleen eigen attributen); een attribuut met de naam van een eenheidswoord na een lidwoord.
- **Samenvoeging met stap A.** Breidt stap A `jutterswet-strandseizoen` uit (de opdracht van A noemt die golden), dan
  regenereert de `scenario.rgs` de nieuwe case-bestanden niet meer en meldt gate 16 `DIFF`/`SET DIFF` met naam: de nieuwe
  verwachtingen moeten dan als bullets in `scenario.rgs`. Verder mogelijke raakpunten: `corpus/golden/README.md`, het
  register en de catalogi (RGS-380 naast A's codes), `grammar/DIVERGENCES.md` (andere secties).
- **Distributie (pin-bump).** `SituationToRequest` en `ScenarioData` (service) lezen van een `AttributeValue` alleen
  `value()` en `timeline()`; bij een cel is `value()` null. `GoldenOverQuarkusTest.compare` schakelt op de soort zonder
  `CELL_VALUE`/`CELL_EMPTY` (een statement-switch: compileert, slaat de verwachting over). Bij de pin-bump moet de
  distributie een cel dragen of als `NonPortable` weigeren. Gecompileerd tegen de nieuwe records is niet gemeten; de
  distributie gebruikt alleen accessors, geen constructor van `AttributeValue` of `Expectation` (grep).

#### `make gates`

Eén run op `9d2f091e7` (`logs/stapD-gates-1.log`, `out/gates/20261102-182340/summary.txt`), gestart nadat de gate-run van
de toolchain-bouwer klaar was:

```
start 2026-11-02 18:23:40 9d2f091e7 card/dimensions-scenario
ocaml rc=0 6s ocaml-build: rebuilt (no stamp _build/.gates-built; 6s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 9s drools-jar: rebuilt (no shaded jar under backends/drools/target; 9s)
01-self-contained rc=0 2s
02-ir-schema rc=0 132s
04-case-schema rc=0 9s
03-verifier rc=0 39s
05-oracle rc=0 22s
06-clingo rc=0 267s
07-noop rc=0 13s
08-red rc=0 164s
09-catala rc=0 6s
10-regelspraak rc=0 228s
11-toka-parity rc=0 221s
12-drools rc=0 321s
13-reftype rc=0 273s
14-flogic rc=0 3s
15-graph rc=0 47s
16-scenario rc=0 43s
17-readings rc=0 240s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 18:29:10 330s red=0
```

Uit de gate-logs: gate 10 `check-rgs-codes` 359 codes, 380 plaatsen; JUnit 1415 tests, 163 rgs-goldens; gate 16 40
scenario-goldens, 136 case-bestanden en hun `ir.json` byte-gelijk; gate 11 29 PASS in ref en clingo; gate 12 299 codes;
gate 17 306 goldens en 852 case-bestanden vergeleken, afwijkingen 0. Niet gemerged, niet gepusht; `git status --short` leeg.

### Stap M (D samengevoegd met A; de tokens van een verzameling met "en")

Branch `card/dimensions-remaining`, op `bc42f8538` (stap A). Commits:

- `243eead3b`, `ad900fe29`, `dd7430cc6`: de drie commits van stap D (`e41193929`, `9bf0fbf31`, `9d2f091e7`), met
  `git cherry-pick b7e3a9611..9d2f091e7` overgezet;
- `f5c455073` RGS-380 heet RGS-369;
- `962c1087d` semantische tokens: een lid van een verzameling met "en" noemt elk label, met twee tests en
  `grammar/DIVERGENCES.md`;
- `b63d1772f` golden `jutterswet-seizoenvouw` weer met de verzameling van twee labels (terugdraaiing van `08e716151`, en
  de NOTE beschrijft de vorm);
- `bc11070ba` voorstel spec: ch. 5.8c noemt die verzameling weer met twee labels (terugdraaiing van `374cc5b9f`);
- `580c7959d` het Result van stap D als "Stap D" in de kaart;
- het commit van dit Result.

Beschermde paden: alleen `spec/RUNTIME-SEMANTICS.md` (de terugdraaiing van A's eigen voorstel); geen `scripts/`.

**Conflicten.** Geen tekstueel conflict: git voegde `grammar/DIVERGENCES.md`, `grammar/RegelSpraak.g4` en
`corpus/golden/README.md` zelf samen (A en D schreven in andere secties en regels). Nagelezen: in `RegelSpraak.g4` staan
D's `naamVervolgDeel` en A's `aantalVoorkomensFunc` en `eersteLaatsteFunc` naast elkaar; in de README A's alinea over
`seizoenaanvang`/`seizoenlogboek` na D's zinnen over de `scenario.rgs` van `strandseizoen` en `seizoenboeking`. Register
en catalogi: A voegde geen RGS-code toe, D verving RGS-237 door RGS-380; geen overlap. Semantische tokens: alleen D
raakte `SemanticTokens.java`. A breidde geen golden met een toetsdocument uit (`seizoenvouw` heeft geen `scenario.rgs`,
`seizoenaanvang` en `seizoenlogboek` zijn nieuw zonder), dus geen bullets erbij; gate 16 regenereert de 40 goldens.

**Hernummerd.** RGS-380 `scenario-cell-period` → **RGS-369**, het eerste vrije nummer na RGS-368 (de laatste code van
het register, ook na A): register, beide catalogi, de drie plaatsen in `ScenarioResolver` en de pin in
`ScenarioCellTest.a_cell_has_no_period`. RGS-369 kwam in geen commit van de geschiedenis voor (`git log --all
-S'RGS-369'` leeg). RGS-237 blijft vrij: een ingetrokken code krijgt geen andere betekenis (besluit 1 van D). In de tekst
van stap D en in zijn voorstel voor CONSTRUCTS leest de hoofdsessie RGS-369 voor RGS-380 en de nieuwe hashes.

**De tokenfout.** De grammatica laat "en" binnen een label toe (`dimensieWaardeWoord`), dus `{najaar en voorjaar}` is
één `dimensieWaarde`; de resolutie leest zo'n lid als labels (`ExpressionBuilder.memberLabels`: eerst het hele lid als
label, anders labels verbonden door "en" als precies één groepering labels geeft), maar `SemanticTokens.labelReference`
zocht het hele lid als één label en liet het `unresolved`. Nu leest `labelReference` met dezelfde regel, op de stukken van
de bron (`labelGrouping`, `labelGroupings`): elk label is een eigen `dimensionLabel`-naam en "en" houdt zijn lexicale
soort; bij twee groeperingen blijft het lid één onopgeloste naam (de resolutie weigert dan met RGS-070). Alleen bij een
bekende dimensie, zoals de resolutie.

- Rood (`logs/stapM-red-tokens-1.log`): de nieuwe test `SemanticTokensTest.a_set_member_of_labels_joined_by_en_names_each_label`
  vóór de reparatie: "unresolved: 'najaar en voorjaar' at line 16". Met de golden in de vorm van twee labels en de
  reparatie uit (mutant T1) ook `SemanticTokensCorpusTest`: "jutterswet-seizoenvouw:47:131 'najaar en voorjaar':
  unresolved in a document without diagnostics", de rode eerste run van A.
- Tests: `{najaar en voorjaar}` (de vorm van de golden), `{eb en vloed en springtij}` (een label met "en" naast een
  ander), `{eb en vloed}` (een gedeclareerd label met "en" blijft één naam), en
  `a_set_member_with_two_groupings_stays_one_unresolved_name` (labels eb, vloed, "eb en vloed": het lid "eb en vloed en
  springtij" blijft één `unresolved`-naam, RGS-070 op die regel).
- Mutaties (recept `mutants/stapM/mutateM.py`, `runM.sh`, in een `git worktree add --detach` van `bc11070ba`, opgeruimd;
  `logs/stapM-mutants-1.log`), elk rood:

| | bewaking | mutatie | rood |
|---|---|---|---|
| T1 | een lid wordt gegroepeerd | groepering uit | de nieuwe settest en `SemanticTokensCorpusTest` (`seizoenvouw`) |
| T2 | een label van meer stukken | één stuk per label | `{eb en vloed en springtij}` onopgelost; de tweede test leest "eb" |
| T3 | alleen bij precies één groepering | de eerste van meer groeperingen | de tweede test leest "eb" |

**"En" buiten een verzameling** (proef in de scratchpad, niet bewaard, met `Resolver.tokens` op één document): de tokens
lezen elke cel met "en" die de resolutie leest al goed, omdat regeltekst woord voor woord gelezen wordt (`words`), niet
uit één parseknoop: `zijn vondstgewicht op noord en in najaar`, `zijn winst van vorig boekjaar en op noord`, het celdoel
`De winst van vorig boekjaar en op zuid van een Jutter`, `in zomer en op zuid`; de bulletnamen van een toetsdocument lezen
ze sinds stap D zo ook. Dezelfde fout zat wel in elke andere verzameling van twee of meer met "en" in één lid
(`{zuid en noord}`, `{huidig boekjaar en vorig boekjaar}`, `{eb en vloed en springtij}`): alle vier onopgelost vóór, alle
opgelost na de reparatie, zoals de resolutie ze leest (`dimensionSelections` gecontroleerd in de `ir.json` van de proef).
Niet dezelfde fout en niet gerepareerd: een cel met een label dat zelf "en" bevat achter het attribuut (`zijn
getijgewicht bij eb en vloed`) is een syntaxfout (RGS-091); die lezen resolutie en tokens geen van beide.

**De golden.** De oorspronkelijke bedoeling van A (`d751929d9`) was `{najaar en voorjaar}`: de volgorde van de
verzameling én dat een verzameling alleen haar eigen labels vouwt (J3 met alleen `zomer` 1.25 is leeg). Daarom terug
naar twee labels in plaats van een extra case. Verwachting (oud → nieuw): `case2-leeg-en-zomer.json` J3
`najaarsvoorrangsgewicht` `attributeValue` 1.25 → `attributeEmpty`, de waarde van `d751929d9`; `ir.json` (de selectie
`najaar`, `voorjaar` en twee snippets) en de NOTE idem, plus een zin over het ene lid dat als twee labels gelezen wordt.
Besluit 11 van stap A vervalt.

**Pins en vloeren.** Geen pin of vloer veranderd. Tellingen tegen de laatste run van A (`out/gates/20261102-190122`):
gate 10 1408 → 1423 tests (13 van D, 2 van M), 359 codes (gelijk), 377 → 380 plaatsen (D), 165 rgs-goldens; gate 16 38 →
40 scenario-goldens, 131 → 136 case-bestanden; gates 2, 4, 5, 6, 7, 12, 13 en 17 gelijk (413 bestanden en 55178 objecten,
1056 cases, 1860 voorbeelden en 985 cases, 2289 checks, 940, 308 gespeeld op beide takken, 75 getwiste annotaties, 6941
lezingen en afwijkingen 0).

**Voorlopige besluiten.** (1) RGS-369 voor RGS-380 (opdracht: zonder gat), RGS-237 niet hergebruikt. (2) De tokens
volgen de regel van `memberLabels` met een eigen groepering op stukken van de bron in plaats van `memberLabels` te
hergebruiken: die geeft labels, geen posities. (3) De golden terug naar twee labels, geen extra case.

**Niets mislukt.**

#### `make gates`

Eén run op `580c7959d` (dit Result zonder deze sectie; de kaart is geen invoer van een gate),
`logs/stapM-gates-1.log`, `out/gates/20261102-192040/summary.txt`:

```
start 2026-11-02 19:20:40 580c7959d card/dimensions-remaining
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 217 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 6s drools-jar: rebuilt (12 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/dev/uchar/regelspraak/emit; 6s)
01-self-contained rc=0 2s
02-ir-schema rc=0 125s
04-case-schema rc=0 7s
03-verifier rc=0 36s
05-oracle rc=0 21s
06-clingo rc=0 255s
07-noop rc=0 12s
08-red rc=0 156s
09-catala rc=0 5s
10-regelspraak rc=0 218s
11-toka-parity rc=0 213s
12-drools rc=0 311s
13-reftype rc=0 264s
14-flogic rc=0 2s
15-graph rc=0 45s
16-scenario rc=0 38s
17-readings rc=0 232s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 19:25:57 317s red=0
```

Niet gemerged, niet gepusht. De kaart blijft in `doing/`.

### Stap B (een cel en een vouw over een rol, een label met "en", clingo over een pad)

Branch `card/dimensions-remaining`, op `28df094fd` (stap A, D en M). Commits:

- `b5265c790` verifier, ref en clingo: een cel van elk lid als vouwbron (WF-037, `Eval`, `cell_source_fold`), clingo leest
  een symbolische cel en eerste/laatste over een pad, de rij `WV-5.8c-cel-van-elk-lid`, de negatieven
  `wf037-count-cell-source` en `wf040-fold-source-cell-label`;
- `6b8feba0e` Drools: een cel en een dimensievouw over een pad, een cel van elk lid als bron van een vouw over een set,
  DROOLS-189 `RETIRED`, README;
- `93c21b5bd` frontend: een cel over een rol, een dimensievouw over een rol, een cel van elk lid, een label met "en",
  `grammar/DIVERGENCES.md`, `DimensionCellOverRoleTest`;
- `093900062` golden `jutterswet-ploegseizoen`, `corpus/golden/README.md`, de rijen van `DimensionCellWitnessTest`,
  gate 12 `EXPECTED_PLAYED` (voorstel);
- `bc109cfa8` voorstel spec: IR-SPEC ch. 4.3 en de indexrij van WF-037, RUNTIME-SEMANTICS ch. 5.8c en 5.12,
  ASP-TRANSLATION;
- `1410d7d23` de rode stand in de NOTE van de golden en de javadoc van de getuige zoals gemeten;
- het commit van dit Result.

Beschermde paden (voorstel, niet gemerged): `spec/verifier/lib/checks.ml`, `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `scripts/check-drools.sh`; `spec/ASP-TRANSLATION.md` onder het net van `/spec/`. Buiten de
may-touch-set: niets.

**Begintoets.** ALEF nagelezen (`872b859`, dump van de `.mps`-modellen met een eigen script in de scratchpad): een cel
over een rol staat in `Onderwerpen_Test.ALEF4486` ("td3 [dim] van de a van de V", ook over twee rollen) en in
`Consistentieregel_Test.ALEF4638`; een vouw van een cel van elk lid in `MeerdereObjecten_Test.EenOpMeer_AggregatieDimensies`
(de som van `inkomen [België]` van alle `kinderen`, verwacht 300) en in `Onderwerpen_Test.ALEFS473` (negen keer). ALEF
schrijft de cel als `DimAttribuutSelector` (lidwoord, attributieve labels, attribuut, predicatieve labels), dan "van" en
het object (`regelspraak/editor.mps`). ALEF schrijft geen celdoel over een rol: alle 30 doelen met een
`DimAttribuutSelector` staan op `UnivOnderwerp`. ALEF schrijft geen `DimensieAggregatie` over een rol: alle 37 staan op
het eigen onderwerp (`OnderwerpRef`). Gemeten op `28df094fd` met een hand-IR: ref en clingo lazen de cel en de vouw
over een pad al (stap 2); de vouw van een cel van elk lid weigerden verifier, ref, clingo en de Drools-lader met
`WF-037.aggregation-non-source` (de draad had geen vorm); clingo weigerde een tekstcel over een pad en de eerste
tekstcel over een pad met `CLINGO-002`, een getal (som, laatste) over een pad las clingo al; Drools weigerde elke
cellezing en dimensievouw over een pad met DROOLS-189. Het advies (B7: Drools met de pin tot later, een golden pas
met de frontendvorm) klopt; deze stap bouwt beide.

#### Wat er veranderde, per laag

- **Verifier (voorstel).** WF-037: de bron van `sum`, `minimum`, `maximum`, `first`, `last` en `filledCount` met een
  bereik en zonder `dimensionSelections` mag een `DimensionCellRead` zijn van een `AttributeReference` op het
  elementtype (`non_source`, `fold_source`); de labels gaan door WF-040 (`wf040_cell`), de verwijzing eronder door de
  bronregels (`aggregation_source_check`: geen pad, WF-083-anker, WF-034/080). Een `count` en een `total` nemen geen
  cel. De afhankelijkheidsgraaf las de cel onder het argument al per cel (ch. 5.1).
- **ref.** `eval_function_call`: een cel van elk lid als bron leest per lid `World.attr i (cell_member …)`, een lid met
  een lege cel draagt niets bij (`contributing_values`); de lezing (ch. 5.12) geeft de vouw haar bijdragers, zoals bij
  een attribuut. README.
- **clingo.** `cell_source_fold` herschrijft zo'n vouw naar de setvouw over het lid van de cel
  (`val(X,"vondstgewicht[voorjaar]",A)`), aan de ingang van `expr_term`, `symbolic_term` en `dt_term`;
  `set_source_attribute` zoekt de declaratie met `find_decl`, die een cel kent. `symbolic_term`: een cel van tekst,
  enumeratie of boolean en `first`/`last` over zulke cellen lezen over een pad via `member_read ~scaled:false` en
  `dimension_cells` (die twee en `dimensioned_reference` staan nu vóór `symbolic_term`, ongewijzigd).
- **Drools.** `TmsTranspiler.dimensionedPath` en `dimensionedRead` in plaats van `dimensioned`: een cel en een
  dimensievouw lezen op het object dat het pad bereikt (`Reads.navigated`): de kaart van de bereikte bean, een
  geschreven cel via de padstand, leeg bij een ontbrekende schakel; een cellezing over een pad noteert het bereikte
  object (`noteObject`), de vouw niet. `fold` en `contributionRules`: een cel van elk lid als bron, de bijdrage leest de
  ingang van de kaart (`Consequence.Input.MemberValue.cell`, beide weergaven: `ComputeHelpers.cell`, route `CELL`) of
  de stand van een geschreven cel (`cellMember`); `IrLoader.sourceRefusal` laat de cel door onder de waardevouwen en de
  filled count. DROOLS-189 `RETIRED`; DROOLS-100 blijft, met reden (hieronder). README.
- **Frontend.** `ExpressionBuilder.attributeRefAt`: na een navigatie of over een set leest een naam die geen attribuut
  is van het bereikte type als cel van dat type (`dimensionCell` met een fabriek voor de verwijzing eronder, die pad of
  setlezing draagt); een keten van to-one rollen eindigt bij de eerste grens waar het voorvoegsel een cel noemt, ook
  een gedeeltelijke (RGS-364). `resolveDimensionSelections` vouwt ook over een argument over een to-one pad;
  `IrEmitter.foldSourceReference` maakt een cel van elk lid tot vouwbron (`isFoldSource`, `foldRange`,
  `foldRefusalOf`, de bron zonder pad), `aggregationElement` en de vouwhekken lezen hem; `RuleChecker.checkSetRead` laat
  de uitzondering door de cel heen onder een waardevouw. Het label met "en": `Resolver.parse` en
  `ScenarioResolver.read` parsen een document met zo'n gedeclareerd label een tweede keer, met elke "en" buiten
  accolades waarvan de buurwoorden zo'n label spellen als `LOWER_ID` (`Resolver.labelsWithEn`, `labelWords`); geen
  grammaticawijziging, geen nieuwe code. `grammar/DIVERGENCES.md` "More dimensions": de vormen, de lowering, de grenzen.
- **Corpus.** Golden `jutterswet-ploegseizoen` (hieronder), twee negatieven.
- **Spec (voorstel).** IR-SPEC ch. 4.3 (de rij `arguments`: de cel van elk lid), de indexrij van WF-037 (de
  fold-source-arm, de getuigen); RUNTIME-SEMANTICS ch. 5.8c (uitgewerkt voorbeeld "A cell of each member", "Where the
  executors stand": Drools leest een cel over een pad, beide OCaml-uitvoerders een cel van elk lid, de golden), ch. 5.12
  (een cel over een pad valt binnen gate 17; "Four boundaries" wordt "Three"); ASP-TRANSLATION (de symbolische cel over
  een pad, de vouw over een pad, de cel van elk lid).

#### De golden

`jutterswet-ploegseizoen` (bron, met de hand uitgerekend): `vondstgewicht` (getal, kg) en `vondstsoort` (tekst) over
`seizoen` op de `Jutter`; een `Ploeg` met één `leider` en meer `leden`. Tien regels: de cel `najaar` als `zomer` maal 2,
de cel `voorjaar` en `najaar` (geschreven) van de leider, de som en de laatste over de cellen van de leider, de som,
het maximum (over geschreven cellen) en de filled count van een cel van elk lid, de tekstcel `zomer` en de eerste
tekstcel van de leider. `case1-twee-ploegen.json` 23 verwachtingen (P1 en P2, vier geschreven cellen), lege cel: J2
`voorjaar` (P2 `leidersvoorjaarsgewicht` leeg), J5 `voorjaar` (een lid zonder bijdrage); J3 `najaar` 0.00 (leeg maal 2,
ch. 1.2). `case2-zonder-leider-en-leden.json` 11 verwachtingen (P3 zonder leider en leden: alles leeg, de filled count
0; J4 buiten elke ploeg). ref en clingo `PASS (23)` en `PASS (11)`, Drools `PLAYED 2` op beide takken, beide cases
vallen onder `--noop`, gate 17 vergelijkt hun lezingen (`on "J1"`, `on none`, de bijdragers), gate 15 hun graaf.

#### Rood op `28df094fd` en mutaties

Rood (detached werkboom `base-B` op `28df094fd` met de nieuwe tests en documenten, opgeruimd;
`logs/stapB-red-1.log`, `-2.log`):

- verifier op de golden: `WF-037.aggregation-non-source` en `WF-053.read-other-type` op de drie vouwen van een cel van
  elk lid; ref `EVALUATION FAULT` en clingo `TRANSLATION FAULT` in dezelfde zin; Drools `DocumentError` van de lader,
  beide takken. Zonder die drie vouwen: verifier `PASS`, clingo `CLINGO-002` op de tekstcel en de eerste tekstcel over
  het pad, Drools `EXCLUDE DROOLS-189` op beide takken (de cel en de vouw over het pad).
- frontend op `source.rgs`: RGS-125 (cel over een rol), RGS-127 (vouw over de cellen van de leider), RGS-068 (cel van
  elk lid, marker); `DimensionCellOverRoleTest` 10 van 11 rood; groen bleef
  `an_en_outside_a_declared_label_stays_a_syntax_error`, een pin van bestaand gedrag (RGS-091).
- `ref/test` `WV-5.8c-cel-van-elk-lid`: niet verifier-schoon en de fout van de vouw.
- `DimensionCellWitnessTest`: de drie nieuwe rijen rood op de `DocumentError`, 18 bestaande groen.
- De negatieven op de oude verifier: beide verworpen, met `WF-037.aggregation-non-source` erbij; het zijn pinnen van de
  verbreding (V2, V3 hieronder).

Mutaties, elk één vervanging in een `git worktree add --detach` van `bc109cfa8` (`mut-B`, opgeruimd), recept
`/Users/yeb/Developer/uchar-dev/werk/dimensions-remaining/mutants/stapB/mutateB.py` en `runB.sh`, uitkomst
`logs/stapB-mutants-1.log` en `-2.log` (R1 en D3 opnieuw: de eerste R1 bouwde niet, warning 26, en mat de oude binary;
de eerste D3 veranderde twee dingen); niet gecommit, elk rood:

| | bewaking | mutatie | rood |
|---|---|---|---|
| V1 | WF-037 laat de cel van elk lid toe | uitzondering uit | golden: `WF-037.aggregation-non-source` 3×; `WV-5.8c-cel-van-elk-lid` niet schoon |
| V2 | geen cel onder een count | ook onder count | `wf037-count-cell-source` krijgt `WF-093.count-source-member`, niet de gepinde arm |
| V3 | labels van de bron door WF-040 | `wf040_cell` weg | `wf040-fold-source-cell-label` niet verworpen |
| R1 | ref leest de cel van elk lid | het attribuut | case1 `aantal voorjaarsleden` 0 voor 2; `WV-…-cel-van-elk-lid` drie waarden |
| C1 | clingo: cel van elk lid | herschrijving uit | golden `TRANSLATION FAULT` (de vouw) |
| C2 | clingo: symbolische cel over een pad | alleen eigen onderwerp | golden `CLINGO-002` |
| C3 | clingo: symbolische eerste over een pad | alleen eigen onderwerp | golden `CLINGO-002` |
| D1 | Drools: pad van de cel | pad genegeerd | `EXCLUDE DROOLS-279`; twee rijen |
| D2 | geschreven cel over een pad via de padstand | uit de kaart | `leidersnajaarsgewicht` leeg voor 2.50 en 8.00 |
| D3 | object van een cel over een pad in de lezing | niet genoteerd | de lezingsrij (`on "J1"` ontbreekt) |
| D4 | bijdrage leest de cel uit de kaart | de hele kaart | `ClassCastException` (LinkedHashMap naar BigDecimal), twee rijen |
| D5 | geschreven cel van elk lid via de stand | uit de kaart | `zwaarste ledennajaarsgewicht` leeg voor 4.20 en 8.00 |
| D6 | DRL-tekst: de cel in de bijdrage | omgedraaid | DRL-teksttak `ClassCastException`, exec-model `PLAYED` |
| F1 | frontend: cel over een rol | lezing uit | 5 tests; golden niet geëmitteerd |
| F2 | gedeeltelijke cel aan een ketengrens | alleen volledige | `a_cell_over_a_role_names_a_label…` (RGS-125 voor RGS-364) |
| F3 | cel van elk lid is vouwbron | `foldSourceReference` alleen verwijzing | `a_value_fold…` (RGS-071, RGS-165); golden niet |
| F4 | sethek door de cel heen | niet | `a_value_fold…` (RGS-165); golden niet |
| F5 | dimensievouw over een pad | alleen eigen | `a_dimension_fold…` (RGS-070); golden niet |
| F6 | cel over een set | alleen na navigatie | `a_value_fold…`; golden niet |
| F7 | tweede parse met de labels | uit | 3 tests (RGS-091) |
| F8 | accolades houden hun "en" | ook binnen | `SemanticTokensTest.a_set_member_with_two_groupings…` |
| F9 | alleen "en" binnen een gedeclareerd label | elke "en" | 5 tests (een marker waar RGS-091 hoort) |

Eerste poging voor het label met "en", gemeten en verworpen: een grammaticaproductie `labelVervolg : EN kaleNaamWoord+`
in de niet-gulzige lus van `kaleNaamDeel` veranderde de parseboom van drie bronnen (`jutterswet-unievoorwaarde`,
`jutterswet-vondstunie`, `toka-22`: de "en" van een instantievereniging werd een naamwoord), gemeten met de
parseboomvergelijking van stap A (`ParseTrees.java`, oude en nieuwe parser, 308 documenten: alle `.rgs` van de werkboom,
de TOKA-fragmenten als één document, de 40 toetsdocumenten); met een semantisch predikaat ervoor gaf de voorspelling
het alternatief toch en faalden die drie op RGS-091. De herspelling van tokens laat de grammatica ongemoeid.

#### Aangepaste verwachtingen, tests, pins en vloeren (oud → nieuw)

- Geen bestaande test of verwachting omgedraaid. `FormsPlan` bouwt `MemberValue` met een vierde argument (`null`); geen
  verwachting veranderde.
- Gate 12 `EXPECTED_PLAYED` 308 → 309 (voorstel), met reden; `EXPECTED_CHANNEL` gelijk (47); DRL-teksttak 308 → 309;
  register 299 codes, retired 11 → 12.
- Geen vloer veranderd. Tellingen (oud: `out/gates/20261102-192040`, stap M; nieuw: `out/gates/20261102-201508`): gate 2
  413 → 414 bestanden, 55178 → 55493 objecten in 981 → 984 documenten; gate 3 413 → 414 golden schoon, 568 → 570
  negatief, 411 → 413 op hun arm, 686 → 692 `name=value`-regels, 705 → 708 wijzers; gate 4 1056 → 1058 cases; gate 5
  1860 → 1862 voorbeelden, 985 → 987 cases; gate 6 2289 checks (gelijk), 985 → 987 cases; gate 7 940 → 942; gate 8
  gelijk; gate 10 1423 → 1434 tests, 165 → 166 rgs-goldens, 359 codes en 380 plaatsen (gelijk); gate 12 JUnit 1761 →
  1764; gate 13 75 → 80 getwiste annotaties over 33 → 34 goldens, 180 → 186 replays, DRL-tak 32 → 33; gate 15 318/355 →
  319/356 vergeleken, 284 → 288 afhankelijkheden, route 165 → 166; gate 16 gelijk (40, 136); gate 17 985 → 987
  getraceerd, 308 → 309 goldens, 855 → 857 case-bestanden, 6941 → 6973 lezingen, 22491 → 22533 knoopregels, 926 → 930
  met bijdragers, 808 → 817 met het object van een lezing (122 → 125 `on none`), afwijkingen 0.
- `CLAUDE.md` (beschermd, niet aangeraakt): gate 12 noemt bij de retired codes DROOLS-189 niet.

#### Voorgestelde tekst voor CONSTRUCTS (hoofdsessie)

Rij 28: in de noot van `dimensions-executed` wordt "Still refused by name: a cell over a path in Drools (DROOLS-189,
DROOLS-100)" "a cell target over a path in Drools (DROOLS-100)". Daarachter:

> **Landed (card `dimensions-remaining`, step B, 2026-11-02, `b5265c790` … `1410d7d23`):** a cell of another object than
> the subject. The Drools backend reads a cell, and folds the cells, of the object a path of to-one roles reaches, as
> ref and the ASP backend did (DROOLS-189 retired); the ASP backend reads a cell of a text, an enumeration or a boolean,
> and the first and the last of such cells, over a path as well. The source of a value fold or a filled count over a
> set may be a cell of each member, a `DimensionCellRead` of the element type (ALEF's fold of one cell over a role;
> WF-037, IR-SPEC ch. 4.3), carried by all three executors. The RegelSpraak frontend writes these forms as ALEF writes
> them ("het vondstgewicht in voorjaar van de leider van de Ploeg", "de som van het vondstgewicht van de leider van de
> Ploeg over alle seizoenen", "de som van het vondstgewicht in voorjaar van alle leden van de Ploeg"), and reads a
> declared label that contains "en" as one label behind the attribute. Golden `jutterswet-ploegseizoen` plays in the
> square ref ≡ clingo ≡ Drools; negatives `wf037-count-cell-source`, `wf040-fold-source-cell-label`. Still refused by
> name: a cell target over a path (DROOLS-100, as every target over a path; ALEF writes none); a dimension fold over the
> cells of every member of a set has no wire form (ALEF writes none).

Rij `DimensieCel`: "**Landed (`dimensions-remaining`, stap B):** ook over een pad in Drools, en als bron van een vouw
over een set (een cel van elk lid)."

#### Voorlopige besluiten

1. **Een cel van elk lid is een `DimensionCellRead` als vouwbron**, geen nieuwe knoop en geen genavigeerde verwijzing in
   de bron: de draad heeft de cel al, de verwijzing eronder houdt de bronregels (geen pad, het anker op het
   elementtype), en elke uitvoerder leest haar per lid zoals een attribuut. Toegestaan onder de zes soorten van een
   dimensievouw, met een bereik en zonder selecties; een `count` (vouwt instanties) en een `total` (integreert een
   tijdlijn, die een cel niet heeft, WF-125) niet.
2. **DROOLS-100 blijft voor een celdoel over een pad**, met reden: ALEF schrijft geen celdoel over een rol (gemeten: alle
   30 op het eigen onderwerp), een doel over een pad weigert Drools ook voor een attribuut, en de frontend leest het
   niet.
3. **Geen dimensievouw over de cellen van alle leden**: de draad heeft er geen vorm voor (een dimensievouw draagt geen
   elementtype, WF-037) en ALEF schrijft er geen (gemeten: alle 37 `DimensieAggregatie` op het eigen onderwerp); grens in
   `DIVERGENCES.md`.
4. **De frontend leest een cel van een ander object alleen na een navigatie of over een set**; een bepaalde
   terugverwijzing naar het eigen type ("het vondstgewicht in voorjaar van de Jutter") niet: de doelroute van een regel
   gaat door dezelfde code en moet ongewijzigd blijven. Grens in `DIVERGENCES.md`.
5. **Een gedeeltelijke cel aan een ketengrens beëindigt de keten**, zodat de fout RGS-364 is (een label ontbreekt) en
   niet RGS-125 (geen lid).
6. **Het label met "en" door herspelling van tokens in een tweede parse**, niet door een productie (gemeten, hierboven):
   alleen een document met zo'n gedeclareerd label wordt anders gelezen, en alleen op de woorden van zo'n label;
   binnen accolades groepeert de resolutie zelf (stap M), dus daar blijft de "en". De langste die past: een label wordt
   als geheel tegen de gedeclareerde labels gelezen (`cellReadings`), en een opsomming vraagt het voorzetsel na "en",
   dus een label met "en" en twee labels zijn nooit beide een volledige lezing; twee volledige lezingen blijven RGS-365.
   Geen nieuwe code: een "en" buiten zo'n label blijft RGS-091. De prijs (in `DIVERGENCES.md`): een "en" tussen de
   woorden van een gedeclareerd label is overal buiten accolades een woord van dat label, ook waar het twee operanden
   van een lijst scheidt; een beslistabel parst haar cellen zonder de labels.
7. **`Consequence.Input.MemberValue.cell`** (de sleutel van de cel in de kaart van het lid) in plaats van een invoer met
   een term: beide weergaven lezen de ingang met de bestaande route `CELL`.
8. **clingo herschrijft de vouw aan de drie ingangen** in plaats van in een voorpas over het document: de vouw komt
   alleen daar binnen, en de herschrijving raakt geen gedeelde bibliotheek.
9. **Twee negatieven als pin van de verbreding** (count, label), geen negatief voor een `total` met een cel: dezelfde
   arm als de count.

#### Wat niet lukte of buiten stap B bleef

- Lukte niet na twee pogingen, en vervangen: het label met "en" als grammaticaproductie (zonder en met predikaat); zie
  hierboven.
- Buiten deze stap, met reden (besluiten 2–4, 6): het celdoel over een pad, de dimensievouw over alle leden, de
  bepaalde terugverwijzing, een label met "en" in een beslistabel.
- De zin van CLINGO-002 ("an own-subject attribute read or a literal") is niet aangepast; een cel over een pad valt er
  niet meer onder.

#### Wat van de kaart nog open is

- Punt 2: een celdoel over een pad (DROOLS-100; frontend leest het niet; ALEF schrijft het niet); een dimensievouw over de
  cellen van alle leden (geen draadvorm; ALEF schrijft het niet).
- Punt 3: een vouw over een geschreven cel van de onbegrensde vorm in Drools (DROOLS-296), met
  `20261102-drools-exact-arithmetic`.
- Punt 5: een `cellValue` van een datum-tijd, constructor, record of lijst in clingo (fout met naam, stap A).
- Punt 6 (frontend, gedocumenteerde grenzen): labels vóór het attribuut met "en"; een cel met "en" of "," in een
  operandenlijst (RGS-364 met waarschuwing); labels achter het onderwerp (RGS-308); een attribuut met de naam van een
  eenheidswoord na een lidwoord; de bepaalde terugverwijzing naar het eigen onderwerp; een label met "en" in een
  beslistabel.
- Distributie (pin-bump): zoals stap 4a/4b/D; nieuw is alleen `Resolver.parse` met de tweede parse (geen API) en
  `IrEmitter.foldSourceReference` (nieuwe publieke methode).
- `CLAUDE.md` (beschermd): DROOLS-189 bij de retired codes van gate 12; de vloeren van gate 3 (zie stap A).
