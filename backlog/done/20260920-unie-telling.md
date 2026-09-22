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

## Result (2026-09-22; het besluit blijft gedateerd 2026-09-20)

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
     2 check-ir-schema          PASS  270 documenten valideren en round-trippen,
                                      vloer 192; 24 patroon-zelftestrijen
     3 check-verifier           PASS  270 golden schoon; 430 negatief afgewezen
                                      op hun eigen WF, vloer 350
     4 check-case-schema        PASS  619 casusbestanden
     5 check-oracle             PASS  ref-suite 773, vloer 489; 578 cases exact,
                                      vloer 427
     6 check-clingo             PASS  backendsuite 479, vloer 83; 578 cases
                                      ref = clingo
     7 check-noop               PASS  542 cases weerleggen een niets-doener;
                                      36 gepinde overlevers
     8 check-rood               PASS  41 rode cases, vloer 26; 82 fragmenten
                                      citeren geen van de 36 bodemzinnen
     9 check-catala             PASS  201 checks, vloer 201; 12 goldens, vloer 12
    10 check-regelspraak        PASS  370 JUnit-tests, vloer 173; 68 rgs-goldens
                                      BYTE-IDENTIEK, vloer 38
    11 check-toka-parity        PASS  29 cases, vloer 29; nul divergenties
                                      (ref 29, clingo 29, Drools 29 PLAYED)
    12 check-drools             PASS  gespeeld 177 (pin 177), kanaalvloer 52;
                                      DRL-tekst 154 (pin 154), vloer 75;
                                      JUnit 532, vloer 239
    13 check-reftype-derivation PASS  131 twists over 27 goldens, vloer 74;
                                      128 replays, 25 DRL-tekst, vloer 13
    14 check-flogic             PASS  130 checks, vloer 81; 2 goldens, vloer 1

(Eindstand na fixronde 1, de naronde en fixronde 3; elk als losse aanroep na
`eval $(opam env)`.)

Geen enkele VLOER aangeraakt. Twee GELIJKHEIDSpins wel, en dat is een
beschermd-padvoorstel: `EXPECTED_PLAYED` 172 → **177** en
`EXPECTED_DRL_PLAYED` 149 → **154** in `scripts/check-drools.sh`, elk met een
redenblok per stap bij de pin. De handberekende som: VIJF nieuwe goldens spelen
in BEIDE takken (`jutterswet-vondstunie`, `jutterswet-keurunie`, uit fixronde 1
`jutterswet-unievoorwaarde`, uit de naronde `jutterswet-uniestratum` en uit
fixronde 3 `jutterswet-uniekwantor`), dus 172 + 5 en 149 + 5.

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
> gaat van 172 naar 177 en `EXPECTED_DRL_PLAYED` van 149 naar 154. Het
> meerstapsbereik onder een telling blijft waar WF-096 en WF-102 het lieten: een
> selectie draagt een eenstaps `bronRol` en geen `bronPad`, benoemd als open
> flank in plaats van als bijwerking beslecht. **Clausule (a) gaat over de
> KNOOP en niet over de telling erboven**, en dat verschil is expliciet gemaakt
> omdat het op twee manieren te lezen was: een TELLING over een unie is een
> gewoon geheel getal en staat waar elke andere telling staat, een `voorwaarde`
> inbegrepen — producentbereikbaar, door alle vier gedragen en gepind met
> `jutterswet-unievoorwaarde`, waarvan de `J2` bovendien de deduplicatie over
> een KENMERK laat beslissen (unie 2, haalt de drempel niet; som der delen 4,
> zou hem halen). **Alle vier weigeren nu bij naam waar de regel weigert**: de
> fixronde legde de tweede verdedigingslijn voor clausule (d) ook in `ref` en
> clingo, die een unie over twee populaties tot dan stil met 3 beantwoordden
> terwijl beide Drools-takken haar bij naam weigerden — ch. 4.3 zegt zelf dat
> de regel weigert in plaats van er per ongeluk een antwoord op te geven, en
> een uitvoerder die toch antwoordt maakt die zin onwaar op precies het
> document dat haar nodig heeft. Dezelfde ronde sloot een CRASH die deze kaart
> zelf had geopend: de unie is de enige laadbare expressieknoop met een null
> `type()`, en de DRL-tekst-tak las dat type ongewacht bij de §4-eenheidsherschrijving
> (NullPointerException waar de andere drie bij naam weigerden); beide
> Drools-takken zeggen er nu één ding over, in dezelfde woorden. **De naronde
> vond bovendien een ORDEfout die niet van deze kaart was maar die zij wel
> deelt:** de gedeelde Drools-stratificatie daalde alleen af in geneste
> CONDITIES en nooit in de EXPRESSIE-operanden van een conditie, dus een
> negatieve lezing van een AFGELEID kenmerk in een vouwfilter — of in een
> unie-takfilter — was voor haar onzichtbaar; met de consument boven haar
> producent vuurden beide Drools-takken in het overgangsvenster en namen de
> monotone kenmerktoekenning niet terug, waar `ref` en clingo de rand zagen.
> De GEWONE eenbronsvouw divergeerde identiek en deed dat al sinds haar filter
> landde, dus één wandeling sluit beide, en beide helften zijn gepind
> (`jutterswet-uniestratum` onder de poorten, `VouwfilterStratumWitnessTest`
> in de suite). **Die wandeling had zelf nog twee gaten**, door een derde lens
> gevonden en met eigen probes gereproduceerd: zij las de polariteit
> uitsluitend uit `Kenmerk.negatie()` — zodat een kenmerk onder een
> NEGATIE-ACHTIGE kwantor (GEEN, TEN_HOOGSTE, PRECIES; ch. 6.1, en de
> verifier voert het zo uit) positief las en de consument in band 0 bleef —
> en zij liet het omliggende ONDERWERP vallen, zodat een vouwfilter zonder
> elementtype op de naam-BREDE producentmatch uitkwam en een gelijknamig
> kenmerk op een ONVERWANT objecttype een valse rand vormde (gemeten stratum
> −2 waar −1 normatief is; een valse rand kan bovendien een valse cyclus
> sluiten). De wandeling draagt nu een conservatieve negatiecontext én het
> omliggende subject mee, met dezelfde regel als de verifier
> (`elementObjecttype` anders het subject); ALLE, DE en TEN_MINSTE geven de
> context dóór in plaats van hem om te zetten, zodat geen regel voor niets
> zakt. Gepind in `VouwfilterStratumAnalyseTest` (de banden zelf, beide
> richtingen) en in de golden `jutterswet-uniekwantor` (dezelfde orde langs de
> GEEN-spelling, elf identieke verwachtingen). De toolchain-herpin (acceptatie
> (d)) volgt hierna.

## Fixronde 1 (2026-09-22)

Twee blokkades en zes lensadviezen; alles afgewerkt, niets teruggedraaid.

**BLOKKEREND 1 — onware getuigeverwijzing.** `spec/IR-SPEC.md` verwees voor de
kale-selectie-naast-gefilterde naar `jutterswet-vondstunie`, "its third rule".
Die golden heeft ÉÉN regel en beide takken dragen `filter: null`. Verbeterd
naar `jutterswet-keurunie`, its second rule — de regel die de vorm werkelijk
draagt.

**BLOKKEREND 2 — de DRL-tekst-tak crashte kaal.** Op
`corpus/negatief/wf110-unie-als-regelwaarde` gaf de `--drl`-route een
`NullPointerException` op `r.waarde().type().isNumeriek()`. Dit is een
REGRESSIE van deze kaart: de unie is de enige laadbare expressieknoop met een
`type()` van null, en de §4-eenheidsherschrijving las dat type ongewacht. Vóór
de `IrLoader`-case was de knoop niet laadbaar en gaf de loader de benoemde
form-not-carried-EXCLUDE. Gemeten vóór de fix: `ref` EVALUATION FAULT WF-110,
clingo TRANSLATION FAULT WF-110, exec-model de GENERIEKE skip
`regel (waarde buiten scope)`, DRL-tekst ERROR/NPE. Gefixt met een BENOEMDE arm
in **beide** Drools-takken (`waarde()` in de DRL-tak, `scalarWaarde()` in de
exec-tak, één zin, dezelfde woorden) plus een nulwacht bij de herschrijving die
géén stille terugval is — de arm erboven heeft een typeloze waarde dan al bij
naam geweigerd. Rood-eerst-getuige:
`InstantieUniePopulatieWitnessTest.de_unie_op_een_waardeplek_wordt_in_beide_takken_benoemd_geweigerd`,
via `BeideTakken.geweigerd`, die van beide takken het citaat `WF-110 (a)` eist.

**A3 — de tweede lijn voor clausule (d) ligt nu ook in `ref` en clingo.**
Gemeten: op `wf110-unie-ongelijke-elementtypes` antwoordden beide STIL **3**
(de unie over twee populaties) waar beide Drools-takken bij naam weigerden.
Nu: `ref` EVALUATION FAULT en clingo TRANSLATION FAULT, beide met
`ranges over TWO populations` en het citaat `WF-110 (d)`. Getuigen in de eigen
suites (`WV-3.4d-twee-populaties-faalt`; `WF-110 een unie over twee
populaties`), geen rode casus — de vorm blijft statisch afgewezen.

**A5 — de staart benoemt de tegenstelling.** Zij opent nu met "Wat de kop over
de LIJST zegt is waar; over de ZIN niet — " en zegt in dezelfde adem dat een
concatenatie geen operandenlijst maar een lijst SELECTIES is. De kop blijft
byte-exact.

**A6 — de conditiepositie is ondubbelzinnig én gepind.** De spec-zin sprak van
"not inside a condition", wat over de KALE unie gaat maar over de telling
gelezen kon worden. IR-SPEC ch. 4.3 zegt nu "not as an operand of a condition"
met een alinea die het verschil uitschrijft, WF-110 (a)'s clausuletekst herhaalt
het, en `RUNTIME-SEMANTICS.md` ch. 3.4d kreeg een eigen alinea plus een
uitgewerkt voorbeeld. Nieuwe golden `jutterswet-unievoorwaarde` (eigen
`bron.rgs`, poort 10 byte-identiek), handberekend:

| jutter | aangemeld | geborgen | unie | som der delen | > 2? |
|---|---|---|---|---|---|
| `J1` | {V1, V2, V3} | {V3, V4} | **4** | 5 | ja |
| `J2` | {V5, V6} | {V5, V6} | **2** | 4 | **nee** |
| `J3` | {} | {V7} | **1** | 1 | nee |
| `J4` | {} | {} | **0** | 0 | nee |

`J2` maakt er meer van dan een positiegetuige: daar beslist de deduplicatie
over een KENMERK in plaats van over een getal — de enige plek in het corpus
waar zij dat doet. Alle vier de uitvoerders dragen hem; geen enkele tak had er
een arm voor nodig. Pins daarom 174 → **175** en 151 → **152**.

**A7 — clausule (d) vergelijkt tegen de eerste tak MET een elementtype.** Zij
vergeleek tegen `branches[0]` en zweeg als die er geen droeg; de referentie is
nu de eerste selectie die er een noemt, en haar INDEX staat in de melding.
Randvoorwaarde expliciet gecontroleerd: **alle zeven bestaande negatiefs houden
precies één WF-110-melding** (nagemeten per document). Het gat is gemeten
gesloten: een unie met takken ⟨geen type, Vondst, Kavel⟩ levert nu twee
meldingen — de ontbrekende populatie op tak 0 en de mismatch van tak 2 tegen
tak 1 — waar zij eerst alleen de eerste gaf.

**A4 — de spurieuze WARNING is weg, de echte blijft.** De conditionele variant
is geprobeerd en GESLAAGD: `aggregatieFilter`'s unie-tak vraagt zijn vraag
alleen nog als een operand werkelijk een die/dat schrijft. Gemeten: een
geweigerde concatenatie zonder die/dat houdt nu alleen haar ERROR (de WARNING
verdween), en een concatenatie MET een onoplosbare deelselectie houdt ERROR én
WARNING. `MarkerRedenPinTest` blijft groen — de markertelling in die methode
beweegt niet, want de tak zelf blijft staan. Getuige:
`een_geweigerde_unie_zonder_deelselectie_krijgt_geen_deelselectie_waarschuwing`.

**A8 — de bekende asymmetrie, genoteerd.** De exec-model-tak eist voor een
takfilter een `leafPredicate`/`leafCompound` (een feit-only predicaat dat
samenstelbaar is, want de selecties worden ge-OR-d), terwijl de DRL-tekst-tak
zijn takfilter door de algemene `conditie`-route haalt en dus in principe meer
vormen kan tekstualiseren. Gemeten: geen divergentie over het corpus, en poort
12 zou er een als waardedivergentie vangen — beide takken spelen dezelfde drie
goldens op dezelfde absoluten. De asymmetrie is dus reëel maar vandaag
onzichtbaar; zij wordt zichtbaar zodra iemand een takfilter schrijft die de
algemene route wél en de predicaatroute niet draagt, en dan is het de
exec-tak die een benoemde weigering geeft.

### Fixronde 1 — geraakte suites en poorten (tussenstand; de eindstand staat in §8)

    ref-suite                   773 checks (was 772)
    clingo-backendsuite         479 checks (was 478)
    frontend JUnit              370 tests  (was 368)
    Drools JUnit                529 tests  (was 526)
     2 check-ir-schema          PASS  268 documenten
     3 check-verifier           PASS  268 golden schoon; 430 negatief
     4 check-case-schema        PASS  616 casusbestanden
     5 check-oracle             PASS  ref-suite 773; 575 cases exact
     6 check-clingo             PASS  backendsuite 479; 575 cases ref = clingo
     7 check-noop               PASS  539 cases
     8 check-rood               PASS  41 rode cases; 82 fragmenten
    10 check-regelspraak        PASS  370 JUnit; 68 rgs-goldens byte-identiek
    12 check-drools             PASS  gespeeld 175 (pin 175); DRL-tekst 152
                                      (pin 152); JUnit 529
    13 check-reftype-derivation PASS

## Naronde (2026-09-22) — de tweede lens

De tweede lens bevestigde blokkade 1 van fixronde 1 onafhankelijk (dubbel
bevestigd, al gefixt) en bracht één nieuwe blokkade en één advies.

### C1 — de stratificatie was blind voor vouwfilters. GEREPRODUCEERD en gefixt.

**Rood-eerst, vóór enige fix.** Twee regels: `P` kent `gemerkt` toe aan een
Vondst boven een gewichtsdrempel; `C` kent de Jutter een monotoon kenmerk toe
wanneer een telling over NIET-gemerkte vondsten groter is dan nul. Gemeten in
alle vier, beide vormen (unie-takfilter én gewone eenbronsvouw met hetzelfde
filter), in twee regelvolgordes:

| document | ref | clingo | Drools exec | Drools DRL |
|---|---|---|---|---|
| producent eerst, unie | afwezig | afwezig | afwezig | afwezig |
| producent eerst, gewone vouw | afwezig | afwezig | afwezig | afwezig |
| **consument eerst, unie** | afwezig | afwezig | **AANWEZIG** | **AANWEZIG** |
| **consument eerst, gewone vouw** | afwezig | afwezig | **AANWEZIG** | **AANWEZIG** |

Twee antwoorden, twee uitvoerders elk, op een verifier-schoon document — de
klasse die `spec/PLAN.md` §6 een specfout noemt. De oorzaak staat waar de lens
hem wees: `Stratificatie.verzamelReads` daalde af in geneste CONDITIES (de
bullets van een samengestelde, filter en toets van een kwantorvouw) en nooit in
de EXPRESSIE-operanden van een conditie, dus het filter van een aggregatie was
onzichtbaar. De verifier ziet hem wél, inclusief negatie (`dep_cond` op het
filter, met het elementtype als subject).

**Wat de meting corrigeert aan de bevinding:** het gat is NIET van deze kaart.
De gewone eenbronsvouw divergeert identiek en deed dat al sinds haar filter
landde (2026-08-19); de unie deelt de klasse, zij opende hem niet. Dat verandert
niets aan de plicht hem te sluiten — hij is nu wél bereikbaar langs een vorm
die deze kaart aanlegt — maar het is de eerlijke lezing.

**De fix.** Eén wandeling: `verzamelReads` daalt nu ook af in de
expressie-operanden van `Vergelijking`, `Predicaat` en `Periode`, via een
nieuwe `readsInExpr` die een `Functie`'s filter met zijn `elementObjecttype`
als leescontext meeneemt en een `InstanceSetUnion`'s takfilters elk met hun
eigen `elementType` — precies zoals de kwantorvouw-recursie haar elementtype
doorgeeft. Exhaustief over de sealed union, zonder `default`, om dezelfde
reden als `Expr.bevat`: een ontbrekende arm rapporteert te wéinig, en een te
weinig gerapporteerde lezing is een regel die één stratum te vroeg vuurt. De
analyse is GEDEELD, dus beide Drools-takken zijn in één keer gerepareerd
(gemeten: beide divergeerden ervóór, beide PLAYED erna).

**Gepind.** Nieuwe golden `jutterswet-uniestratum` — handgeschreven, want de
frontend weigert "die niet gemerkt zijn" bij naam en er is dus geen
producentroute — met de consument BOVEN haar producent, elf handberekende
verwachtingen, `J1` als ordegetuige (0 ongemerkt → kenmerk afwezig) en `J3`
als dedupgetuige erbij (`V6` in beide takken: unie 1, som der delen 2). Alle
vier antwoorden gelijk. Mutatieproef uitgevoerd en teruggedraaid: met de
expressiewandeling uit melden BEIDE takken `verwacht afwezig, werkelijk
aanwezig` op `J1`. Daarnaast `VouwfilterStratumWitnessTest` in de
Drools-suite, twee delen — de unie-helft én de gewone-vouw-helft, zodat de ene
wandeling van beide kanten gepind is en een latere lezer niet te horen krijgt
dat de unie een gat uitvond dat zij alleen deelde. Pins daarom 175 → **176**
en 152 → **153**.

### C3 — het dubbele relatiefeit, nu een vier-uitvoerdergetuige.

Het caseformaat laat `"aangemelde vondsten": ["V1","V1"]` toe (geen
`uniqueItems`), en dat is een geldige case: een relatiefeit is een feit en geen
teller. Nieuwe casevariant `corpus/golden/jutterswet-vondstunie/case2-dubbel-feit.json`
pint het over alle vier: `J1` met `[V1, V1, V2]` aangemeld en `[V2, V2]`
geborgen telt **2**, `J2` met `[V3, V3]` telt **1**. Het is bewust iets anders
dan `WV-3.4d-dubbele-tak`, die de SELECTIE herhaalt: die getuige gaat over de
unie, deze over de invoerlezing, en pas samen dekken zij "een instantie telt
één keer" aan beide kanten.

## Fixronde 3 (2026-09-22) — de derde lens, op de wandeling zelf

De gerichte nalens bevestigde de fixronde en de naronde (pins, handberekeningen,
benoemde armen met hun citaten, de zeven negatiefs, de sealed switch) en vond
twee blokkades IN de nieuwe stratificatie-wandeling. Beide zijn rood-eerst
gereproduceerd met eigen probes vóór enige reparatie.

### N1 — de polariteit ging verloren onder een samengestelde kwantor

Een samengestelde met GEEN, TEN_HOOGSTE of PRECIES is NEGATIE-ACHTIG:
`RUNTIME-SEMANTICS.md` ch. 6.1 zegt het en de verifier voert het uit
(`dep_cond`, "negation-like: conservative"). De wandeling las de polariteit
uitsluitend uit `Kenmerk.negatie()` en liep een samengestelde binnen zonder
context, dus dezelfde zin in de andere spelling werd een POSITIEVE lezing.

**Rood-eerst.** Een GEEN-herschrijving van `jutterswet-uniestratum` — elk
`Kenmerk(gemerkt, negatie=true)` vervangen door
`Samengesteld(GEEN, [Kenmerk(gemerkt, negatie=false)])`, model en case
ongemoeid — is verifier-schoon en meet:

    ref     11 verwachtingen PASS
    clingo  11 verwachtingen PASS
    Drools exec-model   FAIL  J1: verwacht afwezig, werkelijk aanwezig
    Drools DRL-tekst    FAIL  idem

Op bandniveau gemeten: de consument kreeg salience **0** waar **−1**
normatief is.

**Fix.** `verzamelReads` en `readsInExpr` dragen een conservatieve
negatiecontext mee, precies zoals de verifier hem doorgeeft: eenmaal negatief,
behoudend negatief — een tweede negatie heft de eerste niet op. GEEN,
TEN_HOOGSTE en PRECIES zetten hem aan; ALLE, DE en TEN_MINSTE geven hem dóór.
Die tweede helft is opzettelijk gepind: "behandel elke samengestelde als
negatief" is óók veilig maar laat regels zakken die daar geen reden voor
hebben, en een band die alleen maar dieper wordt valt niemand op.

**Getuigen, allebei.** De golden `jutterswet-uniekwantor` — de
GEEN-herschrijving zelf, handgeschreven omdat de frontend geen van beide
negatiespellingen oplost en de zin bij naam weigert — bewaakt hem blijvend via
de poorten 5, 6 en 12; `VouwfilterStratumAnalyseTest` pint de BANDEN zelf, in
beide richtingen (GEEN/TEN_HOOGSTE/PRECIES op −1, ALLE/TEN_MINSTE op 0).
Pins daarom 176 → **177** en 153 → **154**.

### N2 — de wandeling verloor het omliggende onderwerp

`readsInExpr` had geen subjectparameter; een functiefilter gebruikte
rechtstreeks `f.elementObjecttype()` en een geneste conditie kreeg expliciet
`null`. Maar `null` betekent in deze analyse de naam-BREDE producentmatch, een
terugval die `Stratificatie` uitsluitend voor een ONOPGELOSTE GENAVIGEERDE
lezing reserveert. De verifier bewaart het omliggende subject en valt alleen
op het elementtype terug wanneer dat er IS (`fsubject = elementObjecttype else
subject`).

**Rood-eerst.** Een document met een filterdragende telling zonder
`elementObjecttype` in een vergelijking, naast een GELIJKNAMIG kenmerk dat op
een ONVERWANT objecttype één band diep geproduceerd wordt: de consument kreeg
stratum **−2** waar **−1** normatief is — de homoniem op het andere type is
helemaal geen rand. Een valse rand is niet alleen een verkeerd getal: zij kan
een valse CYCLUS sluiten en de cyclus-lokale terugval activeren.

**Fix.** `readsInExpr` draagt het huidige subject door alle expressiekinderen,
en een functiefilter (en een unietakfilter) leest op `elementObjecttype` als
die er is en anders op het omliggende subject — de regel van de verifier,
letterlijk. `IfThenElse`'s guard krijgt het subject in plaats van `null`.

**Getuige.** `VouwfilterStratumAnalyseTest.een_vouwfilter_zonder_elementtype_leest_op_het_omliggende_onderwerp`
pint −1 voor de consument én −1 voor de echte ketenstap op het eigen type, dus
de reparatie kan de rand die er wél hoort te zijn niet zijn kwijtgeraakt. Het
anchor-corpus is niet aangeraakt.

### N3 — de WARNING-getuige pint nu ook wát er overblijft

`AantalUnieTest` telde twee diagnostieken en liet in het midden of de tweede de
bedoelde melding was. Nu: precies één ERROR, precies één WARNING, en die
WARNING draagt het fragment `'die/dat'-subselectie op een aggregatie-operand`.

### Wat deze ronde NIET meenam, gemeten en benoemd

De verifier leest alles onder de MATRIX van een kwantorvouw behoudend
NEGATIEF; deze wandeling geeft de context daar alleen dóór. De spiegeling is
een DERDE gedragswijziging en niet gratis: geteld over de 730 documenten van
de vier zones zitten er **13** kenmerklezingen onder een kwantorvouw in een
voorwaarde, waarvan **12 positief gespeld**, dus de spiegeling zou die
consumenten een band laten zakken en gate-12-kanalen verschuiven. Dat hoort
bij de eigen kaart van de kwantorvouw, met een eigen meting. Vastgelegd in de
code bij de arm, zodat het een besluit is en geen verschil dat niemand zag.

