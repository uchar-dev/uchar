# commentdrift-sweep: commentaar dat door de voortgang is ingehaald

Aanleiding: de CasusTest-javadoc bleek drie resultaatdeel-soorten als
"not yet lifted" te beschrijven terwijl alle drie allang in de IR
zitten. De maintainer vraagt een repo-brede sweep: welke commentaren,
javadocs, docstrings, NOTE's en README-passages beschrijven een stand
die inmiddels niet meer waar is — en corrigeer de aantoonbaar
achterhaalde.

## Regels

- Meetplicht: een treffer is pas achterhaald wanneer de huidige stand
  het aantoont (de knoop/vorm/gate bestaat nu; de weigering is
  opgeheven; de telling klopt niet meer). Twijfel wordt gerapporteerd,
  niet gefixt.
- ALLEEN commentaar/proza bewegen: javadoc, //- en (* *)-commentaar,
  docstrings, NOTE.md, README-passages. NOOIT stringliterals die
  gedrag dragen (marker- en weigeringsredenen, gate-uitvoer,
  foutmeldingen) — ook niet als hun tekst verouderd lijkt: die zijn
  gepind gedrag en horen bij een eigen kaart.
- Testbestanden: alleen commentaar, geen assertie — gemandateerd door
  deze kaart, per geval gemeld in het Result.
- `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`: NIET aanraken;
  vondsten daar als voorstel met letterlijke oud/nieuw-tekst in het
  Result. `spec/verifier/`-commentaar: bijwerken MAG (gemandateerd,
  het Five-arms-precedent), alleen commentaar.
- `backlog/done/` is geschiedenis: niet aanraken. `spec/CONSTRUCTS.md`
  en `spec/PLAN.md`: niet aanraken; vondsten als voorstel.

## Acceptatie

- (a) Het Result draagt de volledige treffer-inventaris in vier
  categorieën: gefixt (met oud→nieuw), achterhaald-in-beschermd-pad
  (voorstel), nog-waar (kort waarom), onzeker (wat een meting zou
  vergen).
- (b) Alle veertien gates groen na de fixes.
- (c) Geen gedragswijziging: de diff raakt uitsluitend commentaar en
  proza; geen assertie, geen stringliteral, geen normtekst.

## Result

**Zoekbasis.** Case-insensitieve grep over de hele werkboom behalve
`backlog/done` en `_build`, op de Engelse en Nederlandse hedge-termen uit
de kaart: 455 treffers in de hoofdronde, 18 in de nalevering ("once",
"until", "lands", "zodra"), 39 in de corpuszone-ronde. Daarvan viel het
leeuwendeel mechanisch af: **stringliterals** (de `fault "… no corpus
case"`-familie in `backends/clingo/lib/translate.ml` alleen al goed voor
~50 treffers, plus de `refuse`/`marker`/`Diagnostic`-teksten in de
frontend), **kalender-"day"** (de dagteller, de maandklem, "the day
boundary"), en **ruimtelijk "nowhere"** ("fires here and nowhere else").
Wat overbleef is hieronder per categorie verantwoord.

### (1) Gefixt — 22 passages in 14 bestanden

De IR van de RegelSpraak-frontend, waar de drift het dikst zat:

1. `ir/ResolvedRegel.java:10` — "the other resultaatdeel kinds
   (kenmerktoekenning, objectcreatie, verdeling, …) and the
   voorwaardendeel **will join the IR** as the resolver learns to bind
   them" → alle drie **hebben** hun knoop (`Soort.KENMERKTOEKENNING`,
   `ResolvedObjectCreatie`, `ResolvedVerdeling`, plus feitcreatie,
   startpunt, dagsoort, consistentie, uniciteit), en het voorwaardendeel
   is het `voorwaarde`-component van deze record zelf.
2. `ir/Conditie.java:18` — "Forms the resolver **does not yet
   structure** (kenmerk/objecttype membership tests, period
   comparisons)" → precies die drie zijn eigen knopen geworden:
   `Kenmerk`, `Rollidmaatschap`/`VerreRollidmaatschap`, `Periode`.
3. `ir/Conditie.java:194` — "A condition form the resolver **did not
   yet structure** (membership/period tests)" → idem; `Onopgelost` is
   nu wat daarna nog overblijft.
4. `ir/Expr.java:20` — "What it **cannot yet bind** (deep van-chains,
   role navigation)" → `AttribuutRef` draagt ze als `navigatieRol` en
   `navigatiePad`.
5. `ir/Expr.java:117` — "A filter the resolver cannot yet structure
   (**the kenmerk-membership predicates these subselecties use are
   still Onopgelost**)" → een kenmerk van het elementtype wordt een
   echte `Conditie.Kenmerk` (`ExpressieBouwer:499`, `:829`).
6. `model/RegelSpraakModel.java:705` — "**The startpunt is not yet
   captured.**" → het VARIABELE startpunt wel (`Tijdlijn.variabelStartpunt`,
   `ResolvedStartpunt`, `startpuntAnkerLid`, op de wire via `IrEmitter`);
   alleen het INLINE vaste startpunt blijft vallen — die kwalificatie
   staat er nu bij.
7. `resolver/RuleChecker.java:45` — "a construct it **does not yet
   fully resolve** (e.g. deep van-chains or aggregations)" → beide
   resolven; de zin noemt nu wat er wél staat.
8. `resolver/RuleChecker.java:843` — "the other resultaatdeel kinds keep
   the base naam and **(for now) no validity guard**" → elke tak in
   `resultaat()` geeft `versieNaam` én `geldigheid` door; geen soort
   zonder geldigheidsvenster.
9. `resolver/RuleChecker.java:900` — "Compound criteria and uniciteit
   (different shapes) **are not yet lifted**" → compound criteria
   landen als `Conditie.Samengesteld` op dezelfde `ResolvedConsistentie`
   (`bouwCriteria`), uniciteit als `ResolvedUniciteit` /
   `ResolvedUniciteitUnie`.
10. `resolver/RuleChecker.java:942` — de "kunnen we nog niet
    uitdrukken"-opsomming noemde "**a cross-type 'verenigd met'
    uniqueness**" → die verliet de lijst toen `ResolvedUniciteitUnie`
    landde; vervangen door de tak die er wél doorheen valt (een
    samengestelde onder GEEN of een tellende kwantor, per
    `transpileerbaar`).
11. `resolver/RuleChecker.java:956` — "Returns false — leaving the
    WARNING — for **the cross-type union, a uniqueness scoped within a
    parent**, or any name that does not resolve" → beide zijn gelift
    (unie → `ResolvedUniciteitUnie`, ouder-scope → `parentScoped` met
    `scopeVeld`/`scopeRol`); de werkelijke false-redenen staan er nu.
12. `resolver/RuleChecker.java:1631` — "**Forms not yet structured** —
    kenmerk/objecttype membership tests, period comparisons — become
    Onopgelost" → idem aan (2).
13. `resolver/ExpressieBouwer.java:33` — "Aggregation result typing is
    **deliberately left at Onbekend for now**" → `functieType` typeert
    SOM, TOTAAL (met de §4.20-rate-strip), MIN/MAX, AANTAL en de
    datumfuncties; alleen de soorten zonder scalaire betekenis houden
    Onbekend.

Testcommentaar (alleen commentaar, geen assertie — per kaartmandaat;
drie gevallen):

14. `frontends/regelspraak/.../KwantorvouwTest.java:33` — "the **two**
    routes most likely to be widened by accident (an unqualified
    voorwaarde, **a counting quantor**)" → de tellende kwantor is sinds
    2026-09-16 een eigen verlaging, en de rij eronder meet de telvouw
    die eruit komt; de klassedoc beschreef nog de stand vóór die kaart.
    De rij zelf is al in die kaart omgedraaid en is niet aangeraakt.
15. `backends/drools/.../HeelperiodeKwantorvouwWitnessTest.java:25` —
    "**The form has no golden yet** … these cases pin what THIS executor
    does with them, which is the part **gate 12 cannot see until then**"
    → `jutterswet-ploegpremie` en `jutterswet-keurploeg` dragen ALLE en
    GEEN over een GeheelPeriode-matrix, en `check-drools.sh:415`/`:467`
    pinnen beide als NEW PLAYER. De hand-gebouwde rijen blijven staan
    als variatie eromheen.
16. `toka/src/test/.../ModelTest.java:62` — "the **period count and
    startpunt are not yet captured**" → de periodetelling wél
    (`attribuutTijdlijnAantal`, `DefinitionResolver:312`), en
    `meer_periodieke_tijdlijn_legt_zijn_periodeaantal_vast` in datzelfde
    bestand asserteert hem; alleen het inline vaste startpunt blijft
    vallen.

Backends en referentie-interpreter:

17. `backends/clingo/lib/translate.ml:4318` — de Kwantorvouw-arm opende
    met "de QUANTIFIER FOLD — **alle/geen**", "**Both quantifiers** are
    ONE negated helper", "**both quantifiers** are vacuously TRUE on
    it" en "**The two quantifiers** differ in ONE step" → WF-108 heeft
    vijf armen en de arm heeft TWEE routes: ALLE/GEEN over de negated
    helper, de drie tellende over `#count` (met een andere leegte-regel:
    ten hoogste houdt, ten minste en precies niet). Het `#count`-blok
    eronder was al correct; de openingsparagraaf sprak eroverheen.
18. `ref/lib/eval.ml:13` — "constructs **no golden exercises yet**
    (navigation paths, dimension cells, filters, timelines, object/fact
    creation, day kinds) fault" → vier van de zes zijn geland
    (`nav_target` voor de hop-paden, de die/dat-filter in
    `eval_functie`, `Timeline` voor de tijdlijnen, `FeitCreatie`/
    `ObjectCreatie`); dimensiecellen en dagsoorten falen nog. De kop
    wijst nu naar `ref/README.md` als onderhouden lijst.
19. `ref/README.md:71` — "**as are temporal write targets (ch. 3.11)**"
    → twee vormen schrijven wél buckets: het ch.-3.7-pro-rata-doel (in
    `eval.ml:1881` letterlijk "the one implemented temporal write") en
    het ch.-3.8-dagtellingsdoel.
20. `ref/README.md:73` — de **Not-implemented**-lijst noemde
    "pro-rata bucket producers, navigation paths, filters, object and
    fact creation, scoped uniqueness, uniqueness unions" en "**any**
    reading of a granular parameter" → alle zes geland (`nav_target`,
    de fold-filter, `run_feitcreatie`/`run_objectcreatie`, de scope-hop
    van ch. 5.9/WF-054, `run_uniciteit_unie`), en de granulaire
    parameter wordt in een TEMPORELE leespositie wél gelezen
    (`env.param_tls`) — alleen in een scalaire sleuf faalt hij. De
    resterende lijst (dagsoorten, multi-periode-roosters, dimensiecellen
    en -selecties, variabele/constante refs, regelgroepen, "is gevuurd")
    is elk apart nagemeten en ongewijzigd gelaten; er staat nu een
    tweede opsomming bij van wat sinds het schrijven landde.

Corpusproza (buiten `corpus/golden/anchor/`):

21. `corpus/golden/jutterswet-tempototaal/NOTE.md:211` — "Those four
    values plus `consistent` are the case this golden gets **the day
    `backends/clingo` carries a set-fold `Totaal`**" → precies de
    kaart-genoemde vorm: hetzelfde NOTE zegt op regel 76 dat clingo de
    set-vouw op 2026-09-05 leerde en `case.json` toen landde, en
    `case.json` draagt die vier waarden plus `consistent` letterlijk.
22. `corpus/jutterswet/README.md:58` — "Naast de Jutterswet **komt** één
    op werkelijke wetgeving gebaseerde casus als realismetoets …
    kandidaat: de zorgtoeslag … **Eigen map zodra het
    Phase-1-corpuswerk start**" → die casus is er:
    `corpus/golden/anchor/ww-herleving-inkomen`, het WW-herlevingspad
    (BWBR0004045), in de ankerzone in plaats van in een eigen map. De
    zorgtoeslag staat er nu als oorspronkelijke kandidaat die nog open
    staat.

### (2) Achterhaald in een beschermd pad — voorstel, niet aangeraakt

**`spec/PLAN.md:506`** (de enige stille drift in de spec-zone; de zin
staat in de lopende Phase-5-analyse, buiten elk changelog-blok).

OUD:
> general folds and function values are planned kernel
> additions, designed at this phase — full-Catala expressibility is the goal,
> not a subset stance (`CONSTRUCTS.md` rows 10/36)

NIEUW (voorstel):
> general folds and function values are kernel since the Phase-5b
> landing, 2026-08-22 (`CONSTRUCTS.md` rows 10/36) — full-Catala
> expressibility is the goal, not a subset stance

Meting: `CONSTRUCTS.md` row 10 draagt "**Landed (2026-08-22):** both
executors carry the anchor-exercised core" en row 36 "**Landed
(2026-08-22):** both executors carry the full form".

**`spec/proposals/PHASE5D-DIFF.md:314`** (afgesloten analysedocument;
een voetnoot volstaat mogelijk).

OUD:
> It does **not** compare two independent v1-native frontends (that
> pairing does not yet exist).

NIEUW (voorstel):
> It does **not** compare two independent v1-native frontends (that
> pairing exists since the F-logic landing, 2026-09-12 —
> `CONSTRUCTS.md` row 39).

Meting: `frontends/flogic` bestaat, gate 14 draait hem blokkerend, en
row 39 pint de frontend-diff tegen `jutterswet-wrakrecht`.

**`spec/CONSTRUCTS.md` rijen 29 en 40** dragen elk een slotclausule die
door de kaarten van 2026-09-16 achterhaald is (rij 29: de
ordering/gelijkheidstoetsen draaien nu in alle drie de uitvoerders; rij
40: de drie tellende kwantoren, de die/dat-filter uit bron, en de
DRL-tak die nog slechts de heelperiode-matrix markert). **Geen
voorstel**: het register hanteert "retire in place" — de correcties
staan al als RETIRES/NARROWS naast de oude clausels, dus dit is
gedocumenteerde en geen stille drift. Schrappen zou een stijlkeuze zijn,
geen correctie.

### (3) Nog-waar — gemeten en met opzet ongemoeid gelaten

- **De `no corpus case`-familie** in `backends/clingo/lib/translate.ml`,
  `ref/lib/eval.ml`, `backends/clingo/lib/model.ml` en
  `frontends/catala/lib/translate.ml` (~60 treffers): stringliterals in
  `fault`/`refuse`. Gepind gedrag; per kaartregel nooit aangeraakt. Waar
  hun BEGELEIDENDE commentaar dezelfde bewering herhaalt, is die
  meegemeten en klopte hij.
- **`spec/verifier/lib/checks.ml`**: het WF-108-blok zegt al "Five arms"
  en "(a) the quantifier: ALLE, GEEN and — since 2026-09-16 — the three
  counting forms"; `dep_cond` splitst de vijf correct over
  negatie-achtig en niet. De gedateerde metingen (1555 op 2026-09-09,
  6265 op 2026-09-05/10, 9329 op de PRIORITIZED-DEFAULTS-ratificatie)
  zijn elk nagelopen en staan nog. Het gemandateerde spec/verifier-luik
  bleef daarmee ongebruikt.
- **`backends/drools`**: "a resolved ROLE is **not yet** a bean FIELD"
  (`DrlTranspiler:3118`/`:3658`, `ExecModelTranspiler:943`/`:2763`/
  `:3183`) is een structurele §13.3.9-uitspraak, geen stand — en
  `:3183` is nog exact zo gemeten op 2026-09-11. `ref` draagt inderdaad
  geen verre rolcheck (`eval.ml:1850` faalt erop), dus de grond onder de
  weigering staat.
- **`NietTemporeleKwantorvouwWitnessTest`**: de javadoc leest de goldens
  `jutterswet-schouwploeg` en `jutterswet-vuurtoren` en beschrijft de
  gate-12/13-verdeling correct; niets aan te passen.
- **`KwantorvouwTest`'s telkwantoren-rij** (regel 164 e.v.) is op
  2026-09-16 al omgedraaid, mét de datum en de reden erbij.
- **`RegelSpraakRuntime:1105`**, **`IrEmitterTest:337`** (IfThenElse
  never emitter-produced), **`IrTest:77`**, **`ExpressieBouwer:1231`**
  (de §8.2-kwalificatie op een subselectie-kenmerk is nog steeds een
  marker: `if (heeftPeriode) return null;`), **`DefinitionResolver:458`**
  (een tweede noemer is nog steeds niet gemodelleerd): alle vijf
  nagemeten, alle vijf waar.
- **`scripts/`** (beschermd, maar er viel niets te melden):
  `check-rood.sh:96` klopt — de `rood`-job in `gates.yml` heeft nog geen
  maven-cache- of buildstap; `check-reftype-derivation.sh:168` en
  `check-self-contained.sh:9` zijn zelfcorrigerend (de clamp faalt bij de
  eerste getuige; `EXCEPTIONS=''`).
- **`spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md`**: 19 hedge-zinnen
  nagemeten, alle nog waar. De registratiestijl (meten, dateren,
  neerzetten) houdt stand; de twee dimensie-tabelgoldens hebben nog geen
  case, `dagsoorten` laat nog `null` toe, `wf091-foldwortel-eenheid`
  bestaat, en RS:2170's leeg/gevuld-regel is de helft die rij 29 met
  opzet liet staan.
- **`corpus/negatief/**/NOTE.md`**: de "no golden carries"-familie
  (WF-034-tabellen, WF-091 `DimensionCellRead`, WF-094 zelfnaam) is
  steekproefsgewijs hergemeten — geen golden draagt een unit-dragende
  `DimensionCellRead`, en de WF-034-sweeps staan met hun eigen aantal en
  datum. De "until this card"-zinnen zijn verledentijdsvormen over hun
  eigen aankomst, geen openstaande claim.
- **`corpus/golden/jutterswet-meldstip/NOTE.md:77`** ("The RegelSpraak
  emitter cannot yet write this document"): de map heeft geen `bron.rgs`
  en de reden staat gemeten bij (2026-09-06, het `DatumTijd`-lexeem in
  `waarde` én `bron`).
- **`frontends/flogic` en `frontends/catala/lib`**: nul hedge-treffers
  in commentaar; de catala-treffers waren `refuse`-literals.
- **`frontends/regelspraak/grammar/*`**: "nowhere" gaat er steeds over
  de externe, bevroren v2.3.0-grammatica.

### (4) Onzeker — wat een meting zou vergen

- **"Phase-7 value model"** in `RegelSpraakModel.java:705` en
  `DefinitionResolver.java:308` ("so the Phase-7 value model can later
  lower the periods"): de tijdlijnmachinerie draait in alle drie de
  uitvoerders, dus "later" klinkt achterhaald — maar "Phase 7" is de
  fasenummering van het DONOR-repo, niet die van `spec/PLAN.md` (dat bij
  Phase 6 ophoudt). Een meting vergt de fasekaart van
  `regelspraak-java`; niet aangeraakt.
- **`EffectieveEenheid.java:282`** ("the corpus produces ZERO entries —
  no golden carries a two-unit MAAL"): meetbaar, maar alleen door de
  tabel tijdens een gate-12-run te instrumenteren. Geen greppbaar
  bewijs; eigen kaartje waard als iemand de lek-bewering wil herbevestigen.
- **`spec/IR-SPEC.md:1580`**: de botsingssweep is gedateerd 2026-09-10
  over 555 documenten en het corpus is sindsdien gegroeid. De
  CONSTRUCTS-tweeling noemt 585. Herrekenen is een eigen handeling, geen
  commentaarfix.
- **`spec/RUNTIME-SEMANTICS.md:3974`** ("an extension of the ch. 6.1
  edge classes, which today carry only negated characteristic reads and
  channel tests"): het prioritizedDefaults-knooppunt is geland, maar of
  de ch.-6.1-kanten inmiddels meer dragen vergt het uitlezen van ref's
  afhankelijkheidsgraaf per kantsoort.
- **`spec/PLAN.md:223` en `:305`**: beide gaan over de stand in
  `regelspraak-java` (de omkering van de afhankelijkheid; een invariant
  die daar commentaar zou zijn). Niet meetbaar vanuit deze werkboom.
- **`spec/proposals/PHASE5B-KERNEL.md:104`** ("text and enumeration
  values have no clingo encoding today"): de keuzetype-familiekaart liet
  `Match` over enum-scrutinees draaien, maar dat raakt condities, niet
  de payload-waarde-encoding waar de zin over gaat. Een meting vergt een
  gerichte emissie met een enum-waarde in een termpositie.

### (b) De veertien gates

Alle veertien groen op de eindboom, elk met exitcode 0:
`check-self-contained` 0, `check-ir-schema` 0, `check-verifier` 0,
`check-case-schema` 0, `check-oracle` 0, `check-clingo` 0, `check-noop` 0,
`check-rood` 0, `check-catala` 0, `check-regelspraak` 0,
`check-toka-parity` 0, `check-drools` 0, `check-reftype-derivation` 0,
`check-flogic` 0.

### (c) Geen gedragswijziging

De diff is machinaal nagelopen: elke toegevoegde en verwijderde regel in
`*.java` en `*.ml` ligt binnen een javadoc-, `//`- of `(* *)`-blok. Geen
assertie, geen stringliteral, geen normtekst (`corpus/jutterswet/wet-v*.md`
is niet aangeraakt), geen `ir.json` of `case*.json` — gate 2's
round-trip en gates 10/11's byte-identieke her-emissie bevestigen dat.
