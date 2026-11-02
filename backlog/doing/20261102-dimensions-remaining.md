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
