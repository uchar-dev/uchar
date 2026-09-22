# unie-telling: de concatenatie van onderwerpexpressies als instantietelling

Gevonden bij de herpin na de opgesomde-telling-kaart (2026-09-20): drie
toolchain-tests dragen de opgesomde vorm als GETESTE feature —
`telling_concatenatie_met_oplosbare_kenmerkfilters_wordt_een_union_accumulate`,
`telling_concatenatie_met_onoplosbare_operand_blijft_marker` en
`union_over_verschillende_elementtypes_blijft_marker` in
`ExecModelDifferentialTest` — waarin "het aantal ⟨rolselectie⟩ en
⟨rolselectie⟩" een union-accumulate wordt: een INSTANTIEtelling over de
unie van twee selecties. Dat is een andere lezing dan de
gevulde-waarden-telling waarop het weigerbesluit van
`20260920-aantal-opgesomde-telling.md` was gebaseerd. Gemeten: nul
operationele documenten dragen de vorm (alle corpora doorzocht); alleen
de drie inline-testbronnen.

## Maintainersbesluit (2026-09-20, verbatim)

"De eerdere weigering blijft gelden voor multi-operand waarde-AANTAL,
maar raakt deze vorm niet: v2.3.0 §5.7 en typeringen §4.11 definiëren
een concatenatie van onderwerpexpressies die als instantieverzameling
wordt geteld. Modelleer dit als `CountInstances` over een expliciete
`InstanceSetUnion` — of, voor dezelfde bronrange, als één telling met
een OR-filter — met deduplicatie op instantie-identiteit. De bestaande
toolchaintests blijven conformancegetuigen; de herpin wacht op de
vier-uitvoerderkaart."

## Acceptatie (bij oppakken aanscherpen)

- (a) Wire-ontwerp per het besluit: een expliciete unie-bron onder de
  instantietelling (of de OR-filter-vorm waar de bronrange gelijk is),
  met deduplicatie op instantie-identiteit genormeerd; spec-first
  (IR-SPEC + schema + verifier als voorstel). De WF-037-weigering van
  de waarde-vorm blijft byte-exact staan — de twee vormen zijn
  onderscheidbaar op de wire.
- (b) De frontend-route: de `aantalFunc`-operandenlijst (de
  geregistreerde grammatica-verruiming) emitteert de unie-vorm voor
  SETLEZING-operanden; de fail-fast van de opgesomde-kaart blijft voor
  waarde-operanden. Rood-eerst emittergetuigen; de drie
  toolchain-testbronnen als conformance-invoer (©-vrij herformuleren
  in Jutterswet-stijl voor eigen getuigen).
- (c) Alle vier de uitvoerders dragen de vorm (golden met handberekende
  absoluten, gedeeld element telt één keer — de dedup-getuige);
  onoplosbare operand en ongelijke elementtypes krijgen hun benoemde
  weigering (de toolchain markert daar — meet en volg of wijk
  gemotiveerd af).
- (d) DAARNA de herpin: de toolchain staat tot die tijd op pin
  ccf7c793 (commit 34bec57); de pin-bump neemt de drie tests als
  conformancegetuigen mee en de registerrij van de opgesomde-kaart
  wordt op de nieuwe stand gebracht.
- (e) Alle veertien gates groen.

May touch: `spec/**` (voorstel), `frontends/regelspraak/**`, `ref/**`,
`backends/**`, `corpus/**`, `scripts/check-drools.sh` (voorstel),
kaartverplaatsing. NB: serieel met Drools/check-drools-kaarten; raakt
dezelfde frontend-plekken als de opgesomde-kaart.

## Result (2026-09-20)

De concatenatie van onderwerpexpressies heeft een eigen wire-vorm, vier
uitvoerders dragen haar, en de WF-037-weigering van de multi-operand
waardetelling staat byte-exact waar zij stond. Alle veertien poorten groen.
Acceptatiepunt (d), de toolchain-herpin, is NIET van deze kaart — die doet de
hoofdsessie na de merge.

### 1. De gekozen wire-vorm, en waarom die en niet de andere

**`InstanceSetUnion` als het ENE argument van een `AANTAL`, met
`InstanceSetBranch` per selectie.** Elke selectie draagt haar eigen
`elementType`, haar eigen `sourceRole` (één stap) en haar eigen `filter`; de
telling erboven draagt géén `elementObjecttype`, `bronRol`, `bronPad` of
`filter`. De knoop draagt **geen `type`**: hij duidt instanties aan en geen
waarde, zoals een `Lambda` dat niet doet, en WF-034's typeproloog zondert hem
op diezelfde grond uit — zonder die uitzondering zou élke unie een
meevurende `value node without a static type` dragen (gemeten).

De ariteit blijft ÉÉN. Dat is de hele reden dat de weigering van 2026-09-20
letterlijk kon blijven staan: een telling met twee of meer `argumenten` is
WF-037, woord voor woord; een telling met één argument dat een
`InstanceSetUnion` is, is deze vorm. De twee zijn op de wire uit elkaar te
houden zonder dat één regel een letter veranderde.

**De OR-filtervariant is NIET genormeerd als unievorm**, en dat is een keuze
met een prijs en een opbrengst. Zij was er al: één telling over één bereik met
een samengestelde `filter` is een gewone telling, alle vier de uitvoerders
droegen haar en deze kaart raakt haar niet. Zij heeft toevallig dezelfde
EXTENSIE als een unie van twee selecties over hetzelfde bereik, maar dat is
een feit over die twee ZINNEN en geen wire-identiteit: "die gemerkt of
gekeurd zijn" is één selectie met een samengestelde voorwaarde, "die gemerkt
zijn en … die gekeurd zijn" zijn twee selecties. De frontend emitteert de
unievorm voor élke §5.7-concatenatie, ook bij gelijk bereik, zodat één
geschreven vorm één wire-vorm houdt.

Wat de expliciete unie koopt: twee selecties over VERSCHILLENDE relaties
("aangemelde vondsten van de Jutter en geborgen vondsten van de Jutter")
hebben geen OR-filterspelling — één telling met één bereik kan ze niet
samenvatten. En juist daar houdt deduplicatie op vanzelf te gebeuren: bij
gelijke bronrange dedupliceert elk uitvoerdersmechanisme al omdat er één
patroon is, bij ongelijke bronrange is het een operatie die elk van de vier
apart goed moet doen. De dedup-clausule zou onder de OR-filtervorm een zin
zijn zonder machinaal artefact.

**Wat de unie NIET heropent:** een selectie draagt een eenstaps `sourceRole`
en geen `sourcePath`. Een meerstapsbereik onder een TELLING is waar WF-096 en
WF-102 elkaar ontmoeten (ch. 3.4b, gevolg 2); dat via een nieuwe knoop
binnenlaten zou die vraag als bijwerking beslechten. Benoemde open flank,
geen stilzwijgende.

### 2. WF-110, vijf clausules, zeven negatiefs

`WF-110` is het eerstvolgende vrije nummer (gemeten: 109 was de hoogste in
spec en verifier). Eén regel voor één constructie:

- **(a)** de unie staat alleen als ENIG argument van een `AANTAL` —
  `wf110-unie-als-regelwaarde`, `wf110-unie-onder-een-builtin`
- **(b)** minstens TWEE selecties — `wf110-unie-een-tak`
- **(c)** de telling erboven noemt geen eigen bron — `wf110-unie-naast-vouwbron`
- **(d)** alle selecties over dezelfde populatie — `wf110-unie-ongelijke-elementtypes`
- **(e)** elke selectie noemt haar populatie en haar relatie, zonder
  typenpaar-terugval — `wf110-unie-tak-zonder-elementtype`,
  `wf110-unie-tak-zonder-bronrol`

De grond onder (e) is deze regel eigen en niet geleend: WF-048 leidt een
relatie af uit een TYPENPAAR, en clausule (d) geeft elke selectie van één unie
hetzelfde paar — dus zou een afgeleide reikwijdte élke bindingloze selectie
DEZELFDE relatie geven en is de unie een herhaling van één van haar selecties.
Welgevormd, tellend, en stil verkeerd.

**Rood-eerst, met de md5's.** (1) Knoop op de wire, regel nog niet geschreven
(`spec/verifier/lib/checks.ml` md5 `c6ba34b2f4ece55bc0e6dac19e086ef2`, binary
`cb285f7baacdbb16d8363b8119eed78b`): vier van de zeven `verifier: PASS`, twee
vielen op een ANDERE regel (WF-047 respectievelijk WF-037) en zijn daarom
herschreven tot documenten waarvan WF-110 het enige gebrek is. (2) Mét de
regel (source md5 `675c051a56e94f4c1e96abcda0b6a6ca`, binary
`5fca029498f72359e648b5ea2a0f63b1`): alle zeven afgewezen, elk met precies
ÉÉN WF-110-melding.

**Arm-exact, over vier zones.** De verifier van vóór deze kaart (binary md5
`038de1619540a813d10eac09357bbe65`) tegen de eindstand, over golden + rood +
negatief + pariteit: de ENIGE documenten waarvan de uitvoer verschilt zijn de
negen die deze kaart maakte — zeven negatiefs en twee goldens — en de oude
binary kan die niet eens LADEN (`unknown expr node "InstanceSetUnion"`). Geen
enkel bestaand document beweegt een verdict of een rapportregel.

### 3. De randen: gemeten, en één keer gemotiveerd afgeweken

De drie toolchain-tests (`DrlTest`, niet `ExecModelDifferentialTest` — daar
staan ze niet; gemeten op pin ccf7c793 / commit 34bec57):

| toolchain-test | vorm | toolchain | uchar |
|---|---|---|---|
| `…_met_oplosbare_kenmerkfilters_wordt_een_union_accumulate` | één relatie, twee kenmerkfilters | union-accumulate | GEDRAGEN, `jutterswet-keurunie` regel 1 |
| `union_over_verschillende_elementtypes_blijft_marker` | twee populaties | marker | STATISCH GEWEIGERD, WF-110 (d) |
| `…_met_onoplosbare_operand_blijft_marker` | kale selectie naast gefilterde | marker | GEDRAGEN, `jutterswet-keurunie` regel 2 |

**Ongelijke elementtypes: de grens volgt de toolchain, de grond niet.** Haar
eigen testcommentaar noemt de reden: één accumulate heeft één bronpatroon. Dat
is een eigenschap van een codegenerator. uchar weigert op een eigen grond —
`elementObjecttype` is overal elders in de IR de POPULATIE (ch. 2.2), een
telling antwoordt een kardinaliteit over één gedeclareerd type, en of twee
selecties met verschillende populaties een instantie kunnen DELEN is een vraag
over de subtypelattice (ch. 2.6, `Munt ⊑ Vondst`) die de deduplicatie zou
moeten beantwoorden. Deze regel beantwoordt haar niet en weigert de vorm in
plaats van er per ongeluk een antwoord op te geven. **STATISCH**, want de wire
draagt de elementtypes en een verifier ziet ze: negatief-getuige, geen rode.

**Onoplosbare operand: gemotiveerd afgeweken.** De toolchain-marker daar is
hetzelfde mechanismefeit — zij lowert een concatenatie door de filtertermen te
OR-en en een operand zonder filter levert geen term — en onze wire draagt de
SELECTIES zelf, dus `filter: null` is de gewone vorm. Alle vier de uitvoerders
dragen hem (`jutterswet-keurunie` regel 2). Een codegeneratorgrens als
normatieve regel importeren zou de verkeerde kant op spiegelen.

**Geen enkele rand is RUNTIME.** Beide zijn statisch, en dat is een bevinding
en geen omissie: wat een unie na het laden nog fout kan doen, doet zij fout in
een veld dat op de wire staat. Er zijn daarom geen nieuwe rode casussen; poort
8 blijft op 41 rode casussen staan.

### 4. De dedup-getuigen, handberekend

`jutterswet-vondstunie` — twee selecties over VERSCHILLENDE relaties
(`vondstaanmelding`, `vondstberging`), één populatie (`Vondst`):

| jutter | aangemeld | geborgen | unie | som der delen |
|---|---|---|---|---|
| `J1` | {V1, V2, V3} | {V3, V4} | **4** | 5 |
| `J2` | {V5} | {V5} | **1** | 2 |
| `J3` | {} | {V6} | **1** | 1 |
| `J4` | {} | {} | **0** | 0 |

`J2` is de scherpste: beide selecties zijn dezelfde eenling, dus 1 en niet 2 —
een uitvoerder die deeltellingen optelt valt daar om zonder dat er iets
"gedeeld" oogt. `J4` pint dat de lege unie 0 is en nooit leeg (ch. 1.2e).

`jutterswet-keurunie` — één relatie, twee kenmerkfilters, en een kale selectie
naast een gefilterde. `J1` meldt `V1` (gemerkt), `V2` (gekeurd) en `V3`
(beide) aan; `J2` meldt `V7` (geen van beide); `J3` meldt niets:

| regel | `J1` | `J2` | `J3` |
|---|---|---|---|
| gemerkt ∪ gekeurd | **3** (som der delen 4) | **0** | **0** |
| gemerkt ∪ kaal | **3** | **1** | **0** |

Beide goldens dragen een `bron.rgs` en re-emitteren byte-identiek (poort 10).

**Mutatieproeven, uitgevoerd en teruggedraaid:**

- `ref`, dedup uit: 5 van de 11 `WV-3.4d`-checks vallen om; terug groen na
  herstel (772 checks).
- clingo, de selectie als sleutel meegeteld (`#count { X,I : unie(S,X,I) }`):
  antwoordt **5** waar ref 4 zegt, en `WF-110 clingo telt de unie gelijk aan
  ref` vangt hem bij naam; terug groen (478 checks).
- Drools, beide takken, alleen de EERSTE selectie geteld — de
  "stille telling over de eerste operand" waar de toolchain voor waarschuwt:
  `verwacht 4, werkelijk 3` en `verwacht 1, werkelijk 0` op vondstunie in
  BEIDE takken; terug `PLAYED 1` viermaal.

### 5. De vier uitvoerders

| uitvoerder | hoe |
|---|---|
| `ref` | bijdragers per selectie, daarna samengevoegd met dedup op instantieLABEL — dezelfde `filter_in_order`+`Hashtbl`-vorm die ch. 3.4b's vlakke unie gebruikt |
| clingo | één HULPPREDICAAT, één regel per selectie, `#count` daarover: een atoom geldt of geldt niet, dus het gedeelde element leidt de kop eenmaal af |
| Drools exec-model | ÉÉN accumulate over ÉÉN elementpatroon met een DISJUNCTIEVE constraint, per selectie haar eigen join én haar eigen filter, als samenstelbare `Predicate2`/`Predicate1` |
| Drools DRL-tekst | dezelfde vorm als tekst: `accumulate( $e1 : Vondst( (aanmelder == $subject && gemerkt == true) \|\| (berger == $subject) ) ; $acc1 : count($e1) )` |

Waarom in beide Drools-takken één patroon en geen `or`: een `DSL.or` over twee
patronen op dezelfde variabele matcht PER voldane tak (dubbeltelling), en twee
opgetelde accumulates tellen het gedeelde element twee keer. Daarom worden de
selecties tot samenstelbare predicaten geloweerd in plaats van via
`filterSource`/`scope`, die hun constraint AND-en en niet te OR-en zijn.

De join van een selectie komt uitsluitend uit haar wire-binding — nooit uit het
typenpaar — en dat is clausule (e) als code.

### 6. De frontend, en de halve divergentie die ophield er een te zijn

De operandenlijst bij `aantalFunc` was geregistreerd als widening omdat "de
brontaal geen opgesomde spelling voor een telling kent". **Dat is waar van een
lijst WAARDEN en onwaar van de zin.** §5.7 spelt een concatenatie van
onderwerpexpressies, en typeringen §4.11 zegt van de aantal-operator dat een
concatenatie van expressies met haar gebruikt kan worden, onbepaald in aantal,
alleen onderwerpexpressies. De vorige kaart citeerde de helft van §4.11 die de
waardelezing uitsluit en las de helft die de concatenatie toelaat niet. Dat is
de eerlijke correctie die deze kaart maakt.

Sinds deze kaart: rolselectie-operanden → `bouwInstantieUnie` → één
`InstanceSetUnion`; waardelezingen → de bestaande weigering, woord voor woord.
Die weigering kreeg een STAART in plaats van een herschrijving, omdat haar kop
op zichzelf misleidt: zij zegt dat de brontaal geen opgesomde vorm spelt, wat
waar is van de lijst en onwaar van de zin. De staart noemt de §5.7-route en
zegt waarom zij hier niet opent. Drie benoemde gronden, elk een ERROR die het
document van de schijf houdt: twee populaties (WF-110 (d)), een niet af te
leiden relatie (WF-110 (e)), en een deelselectie die niet oplost naar een
kenmerk van het element — die laatste omdat de unie een weggelaten filter
OPSLOKT (een kale selectie is een superset van elke gefilterde over hetzelfde
bereik), zodat de fout onzichtbaar zou blijven in het antwoord.

**Rood-eerst gemeten op de frontend van vóór deze kaart (5b42e5b9):** beide
golden-bronnen werden geweigerd met de WF-037-ariteitsfout en schreven niets.

**Geen bestaande test gewijzigd.** `AantalOpgesomdTest` blijft groen zoals hij
is, inclusief `de_spelling_met_elementtype_valt_ook`: die zin valt nu op
clausule (d), maar de melding draagt nog steeds haar WF-037-kop, dus de filter
van die test vindt precies één ERROR. Dat is geen toeval maar de reden voor de
staart-in-plaats-van-herschrijving: de kop blijft waar over de operandenlijst.

### 7. Eerlijke restanten

1. **Een spurieuze WARNING op een geweigerde unie.** `aggregatieFilter`'s oude
   OR-filtertak draait nog voor een concatenatie die `bouwInstantieUnie`
   weigerde, en levert dan een `Conditie.Onopgelost` → de WARNING "een
   'die/dat'-subselectie … wordt nog niet omgezet", ook als de zin helemaal
   geen die/dat draagt. Bestaand gedrag (de vorige kaart noteerde hem al), op
   een document dat de schijf niet haalt. Schrappen is GEPROBEERD en
   teruggedraaid: het verschuift de markerreden-telling die
   `MarkerRedenPinTest` pint (3 → 2) en het kost de `verbruikt`-boekhouding
   haar consumptie van de deelselecties, die zich dan als stil weggevallen
   melden — drie diagnoses waar de ERROR het één keer zei. De tak staat er nu
   met haar grafschrift; opruimen vergt een eigen kaartje, want het raakt een
   bestaande test.
2. **Alleen kenmerkfilters in een selectie.** `elementKenmerkFilter` is de
   enige filterroute die de unie gebruikt; een vergelijkingsfilter ("die
   groter zijn dan 5") wordt benoemd geweigerd. Dat volgt de toolchain-grens;
   verbreden kan later en kost een vertaalarm in vier uitvoerders.
3. **Twee spellingen, één extensie.** "die gemerkt of gekeurd zijn" en "die
   gemerkt zijn en … die gekeurd zijn" tellen hetzelfde en emitteren
   verschillende knopen. Vastgelegd in IR-SPEC als een feit over de twee
   zinnen, niet als een wire-identiteit; geen getuige die beide naast elkaar
   pint (dat zou een golden met twee regels en één verwachting zijn — cheap,
   niet gedaan).
4. **Geen `sourcePath` in een selectie** (zie §1). Benoemd, niet gesloten.

### 8. Poorten, alle veertien, elk als losse aanroep

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  267 documenten valideren en round-trippen,
                                      vloer 192; 24 patroon-zelftestrijen
     3 check-verifier           PASS  267 golden schoon; 430 negatief afgewezen
                                      op hun eigen WF, vloer 350
     4 check-case-schema        PASS  615 casusbestanden
     5 check-oracle             PASS  ref-suite 772, vloer 489; 574 cases exact,
                                      vloer 427
     6 check-clingo             PASS  backendsuite 478, vloer 83; 574 cases
                                      ref = clingo
     7 check-noop               PASS  538 cases weerleggen een niets-doener;
                                      36 gepinde overlevers
     8 check-rood               PASS  41 rode cases, vloer 26; 82 fragmenten
                                      citeren geen van de 36 bodemzinnen;
                                      Drools-been 9 gepind, 32 niet-gedragen
     9 check-catala             PASS  201 checks, vloer 201; 12 goldens, vloer 12
    10 check-regelspraak        PASS  368 JUnit-tests, vloer 173; 67 rgs-goldens
                                      BYTE-IDENTIEK, vloer 38
    11 check-toka-parity        PASS  29 cases, vloer 29; nul divergenties
                                      (ref 29, clingo 29, Drools 29 PLAYED)
    12 check-drools             PASS  gespeeld 174 (pin 174), kanaalvloer 52;
                                      DRL-tekst 151 (pin 151), vloer 75;
                                      JUnit 526, vloer 239
    13 check-reftype-derivation PASS  131 twists over 27 goldens, vloer 74;
                                      128 replays, 25 DRL-tekst, vloer 13
    14 check-flogic             PASS  130 checks, vloer 81; 2 goldens, vloer 1

Geen enkele VLOER aangeraakt. Twee GELIJKHEIDSpins wel, en dat is een
beschermd-padvoorstel: `EXPECTED_PLAYED` 172 → **174** en
`EXPECTED_DRL_PLAYED` 149 → **151** in `scripts/check-drools.sh`, elk met een
redenblok bij de pin. De handberekende som: beide nieuwe goldens spelen in
BEIDE takken, dus 172 + 2 en 149 + 2.

### 9. Gewijzigde bestanden

    spec/schema/regelspraak-ir-v0.schema.json    InstanceSetUnion + InstanceSetBranch   VOORSTEL
    spec/IR-SPEC.md                              ch. 4.3-sectie + WF-110-rij            VOORSTEL
    spec/RUNTIME-SEMANTICS.md                    ch. 3.4d                               VOORSTEL
    spec/verifier/lib/{ast,loader,checks}.ml     knoop + WF-110 + zes wandelingen       VOORSTEL
    corpus/negatief/wf110-*/                     zeven getuigen, elk met NOTE.md        nieuw
    corpus/golden/jutterswet-vondstunie/         bron.rgs + ir.json + case.json + NOTE  nieuw
    corpus/golden/jutterswet-keurunie/           idem                                   nieuw
    ref/lib/eval.ml                              unie-bijdragers + dedup + drie armen
    ref/test/test_ref.ml                         elf WV-3.4d-checks
    backends/clingo/lib/translate.ml             hulppredicaat + twee benoemde weigeringen
    backends/clingo/test/test_clingo.ml          acht checks
    backends/drools/.../IrLoader.java            de knoop van de wire, drie weigeringen
    backends/drools/.../ExecModelTranspiler.java unieTelling + unieJoin
    backends/drools/.../DrlTranspiler.java       unieAccumulate + unieScope
    backends/drools/.../EffectieveEenheid.java   één wandelingsarm
    backends/drools/src/test/.../InstantieUnie*  zeven getuigen (loader + beide takken)
    frontends/regelspraak/.../ir/Expr.java       record + twee wandelingsarmen
    frontends/regelspraak/.../IrEmitter.java     emitterarm
    frontends/regelspraak/.../RuleChecker.java   één wandelingsarm
    frontends/regelspraak/.../ExpressieBouwer.java  bouwInstantieUnie + staart
    frontends/regelspraak/src/test/.../AantalUnieTest.java  tien getuigen        nieuw
    frontends/regelspraak/grammar/{RegelSpraak.g4,DIVERGENCES.md}  halve divergentie
    frontends/catala/lib/translate.ml            twee armen (typeloos + benoemde weigering)
    scripts/check-drools.sh                      twee gelijkheidspins             VOORSTEL

`spec/CONSTRUCTS.md` en `spec/PLAN.md` niet aangeraakt — §10 draagt de
voorgestelde rijtekst letterlijk.

### 10. Registervoorstel — `spec/CONSTRUCTS.md` rij 1

Rij 1 draagt sinds 2026-09-20 de zin die met "**Nuance dezelfde dag
(herpin-meting):**" opent en eindigt op "en de toolchain-herpin wacht
daarop." Voorgesteld wordt die ene zin te VERVANGEN door:

> **Nuance dezelfde dag (herpin-meting), beslecht 2026-09-20
> (`backlog/done/20260920-unie-telling.md`): v2.3.0 §5.7 definieert wél een
> CONCATENATIE van ONDERWERPexpressies die als instantieverzameling geteld
> wordt, en typeringen §4.11 laat haar onder juist deze operator toe,
> onbepaald in aantal. De weigering hierboven dekt dus het multi-operand
> WAARDE-`AANTAL` en niets anders — en dat is geen redding achteraf maar een
> correctie op een halve lezing: de opgesomde-kaart citeerde de helft van
> §4.11 die de waardelezing uitsluit en las de helft die de concatenatie
> toelaat niet.** De concatenatie krijgt een EIGEN WIRE-KNOOP in plaats van
> een vrijbrief voor de operandenlijst: `InstanceSetUnion` als het ENIGE
> argument van de telling, met één `InstanceSetBranch` per selectie die haar
> eigen populatie, haar eigen eenstaps `bronRol` en haar eigen die/dat-filter
> draagt. De ariteit blijft daarmee ÉÉN, wat precies de reden is dat WF-037's
> weigering letterlijk kon blijven staan: twee of meer `argumenten` is die
> regel, één argument dat een unie is, is deze. De knoop draagt geen `type` —
> hij duidt instanties aan, geen waarde — en WF-034's typeproloog zondert hem
> uit zoals zij een `Lambda` uitzondert. **`WF-110` draagt de vorm in vijf
> clausules** (alleen onder `AANTAL`; minstens twee selecties; de telling
> erboven noemt geen eigen bron; alle selecties over dezelfde populatie; elke
> selectie noemt haar populatie én haar relatie, zonder typenpaar-terugval —
> want alle selecties van één unie delen dat paar, dus een afgeleide
> reikwijdte zou ze ononderscheidbaar maken), met zeven negatiefs, één per
> wire-spelling. **Deduplicatie op instantie-identiteit is de operatie zelf**
> (`RUNTIME-SEMANTICS.md` ch. 3.4d): ch. 3.4b's vlakke unie, uitgesproken over
> selecties in plaats van over tussenstappen, en de goldens zijn zo gekozen dat
> de unie een ANDER getal is dan de som der delen (`jutterswet-vondstunie`:
> 4 tegen 5, en 1 tegen 2 waar beide selecties dezelfde eenling zijn;
> `jutterswet-keurunie`: 3 tegen 4). **De OR-filtervariant is NIET genormeerd
> als tweede spelling**: één telling over één bereik met een samengestelde
> voorwaarde is een gewone telling, was al uitdrukbaar, en heeft toevallig
> dezelfde extensie — een feit over twee zinnen, geen wire-identiteit. De
> frontend emitteert de unievorm voor élke concatenatie, ook bij gelijk
> bereik, zodat één geschreven vorm één wire-vorm houdt; wat de expliciete
> unie koopt is de vorm die de OR-filter niet KAN spellen, twee selecties over
> verschillende relaties, en juist daar houdt dedup op vanzelf te gebeuren.
> **De drie toolchaintests blijven conformancegetuigen en de grenzen vallen
> samen, op één gemotiveerde afwijking na.** Twee populaties: geweigerd, maar
> op een eigen grond — de toolchain noemt in haar eigen test één accumulate
> met één bronpatroon (een codegeneratorfeit), uchar zegt dat een telling een
> kardinaliteit over ÉÉN gedeclareerde populatie antwoordt en dat of twee
> selecties dan een instantie kunnen delen een vraag over de subtypelattice is
> die de deduplicatie zou moeten beantwoorden. Een kale selectie naast een
> gefilterde: de toolchain markert (haar lowering OR-t filtertermen en heeft er
> geen voor een operand zonder filter), uchar DRAAGT hem, want onze wire draagt
> de selecties zelf. Beide randen zijn STATISCH — wat een unie fout kan doen,
> doet zij fout in een veld dat op de wire staat — dus negatief-getuigen en
> geen rode. Alle vier de uitvoerders dragen de vorm: `ref` dedupliceert op
> instantielabel, clingo hangt de dedup aan een hulppredicaat (een atoom geldt
> of geldt niet), en beide Drools-takken lowern naar ÉÉN accumulate over ÉÉN
> elementpatroon met een disjunctieve constraint — de `DSL.or` over twee
> patronen en de som van twee accumulates tellen het gedeelde element allebei
> dubbel, wat de mutant meet (5 tegen 4). **Ook dit is een doorsnijding van een
> LEVENDE producentroute, maar de andere kant op:** de grammatica van de
> RegelSpraak-frontend spelt de operandenlijst bij `aantalFunc` al, en de helft
> daarvan houdt op een divergentie te zijn — rolselectie-operanden worden de
> unie, waardelezingen houden hun weigering woord voor woord, met een STAART
> die zegt waarom de §5.7-route niet opende. Arm-exact gemeten: de verifier van
> vóór deze kaart tegen de eindstand, over golden + rood + negatief +
> pariteit — de enige documenten waarvan de uitvoer verschilt zijn de negen die
> deze kaart maakte, en de oude binary kan die niet eens laden. `EXPECTED_PLAYED`
> gaat van 172 naar 174 en `EXPECTED_DRL_PLAYED` van 149 naar 151. Het
> meerstapsbereik onder een telling blijft waar WF-096 en WF-102 het lieten: een
> selectie draagt een eenstaps `bronRol` en geen `bronPad`, benoemd als open
> flank in plaats van als bijwerking beslecht. De toolchain-herpin (acceptatie
> (d)) volgt hierna.
