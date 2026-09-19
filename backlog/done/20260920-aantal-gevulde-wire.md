# AANTAL_GEVULDE, kaart 1: de wire, de poort en `ref`

Uitvoeringskaart 1 van 2 onder het ontwerp
`backlog/doing/20260919-aantal-gevulde-gemiddelde.md`. Het besluitenblok
daar (G1–G9, maintainer 2026-09-19) is bindend en wordt hier niet
heropend; §5 "Uitvoeringsschets" verdeelt het werk over twee kaarten en
dit is de eerste. De ontwerpkaart blijft in `doing/` tot kaart 2 landt.

Goal: de waardentelling `AANTAL_GEVULDE` bestaat op de wire, wordt door
de verifier per arm behekt en door `ref` uitgevoerd, met een
worked-example-clausule in `spec/RUNTIME-SEMANTICS.md` die letterlijk
`ref/test/` haalt. Geen golden en geen rode casus — die wachten op
kaart 2, die clingo en beide Drools-takken de vorm leert; een golden
zonder clingo-drager breekt poort 6 en een rode casus eist twee
weigerende uitvoerders met benoemde fragmenten.

## Acceptatie

- (a) **Wire.** `spec/schema/regelspraak-ir-v0.schema.json` draagt de
  ene nieuwe `kind`-enumwaarde, additief; geen veld erbij, geen veld
  met nieuwe betekenis. Poort 2 groen, round-trip byte-identiek.
- (b) **Spec.** `spec/IR-SPEC.md` ch. 4.3 draagt de soort (kindrij +
  vormbeschrijving), WF-037 twee arms (precies één argument; dat
  argument MOET een setlezing zijn), WF-038 de scope-zin (de soort is
  geen numerieke vouw; besluit G8), WF-096 de exemptiezin zes → zeven.
  Geen nieuw WF-nummer (besluit G5). Beschermd pad: voorstel in de
  branch, merge is aan de hoofdsessie.
- (c) **Rood-eerst.** Eén negatief per arm in `corpus/negatief/`, elk
  eerst gemeten tegen de HUIDIGE verifier (md5 erbij) en daarna tegen
  de nieuwe, plus een arm-exacte mutantsweep over golden + rood +
  negatief + pariteit met beide uitkomsten in het dossier.
- (d) **`ref`.** De vouw wordt uitgevoerd; `spec/RUNTIME-SEMANTICS.md`
  krijgt ch. 3.4c met absolute worked examples en de ch. 1.2e-rij. De
  voorbeelden staan verbatim in `ref/test/` en dekken expliciet: lege
  bron → 0, gemengde gevuld/ongevuld-tellingen, en elk waardetype
  (getal, tekst, datum, enumeratie, booleaan).
- (e) **Benoemde weigering bij de andere uitvoerders.** clingo en de
  Drools-loader kennen de vorm nog niet — dat is correct voor kaart 1 —
  maar zij weigeren hem BENOEMD, niet met een bodemzin en niet met een
  onbeheerde uitzondering, elk met een getuige in de eigen suite. Zo
  blijft poort 8's bodemzinhek toekomstvast wanneer kaart 2 een rode
  casus zou willen pinnen.
- (f) Alle veertien poorten groen, elk los gedraaid.

May touch: `spec/schema/regelspraak-ir-v0.schema.json`,
`spec/IR-SPEC.md` (alleen ch. 4.3 en de WF-037/WF-038/WF-096-bullets
van ch. 4.6), `spec/RUNTIME-SEMANTICS.md` (alleen ch. 1.2e, ch. 3.4b's
verwijzing en het nieuwe ch. 3.4c), `spec/verifier/lib/**`,
`corpus/negatief/**`, `ref/lib/eval.ml`, `ref/test/test_ref.ml`,
`backends/clingo/lib/translate.ml`, `backends/clingo/test/test_clingo.ml`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/loader/IrLoader.java`,
een JUnit-getuige onder `backends/drools/src/test/`, en deze kaart.

Out of scope: `corpus/golden/**` en `corpus/rood/**` (kaart 2);
clingo-vertaling en beide Drools-transpilers (kaart 2); `scripts/**`
(geen vloer beweegt: alle vloeren zijn ondergrenzen en poort 12's
gelijkheden lezen niets zolang er geen golden is); `frontends/**` (de
dialectzin is besluit G9 en kaart 3); `spec/CONSTRUCTS.md` en
`spec/PLAN.md` (registervoorstel gaat als rijtekst in het `Result:`).

## Result (2026-09-20)

`AANTAL_GEVULDE` staat op de wire, wordt per arm behekt en wordt door `ref`
uitgevoerd. Zeven negatiefs, eerst falend gemeten; zes arm-exacte mutanten
plus drie op de uitvoerders, alle werkelijk gedraaid en teruggedraaid; alle
veertien poorten groen. Geen golden en geen rode casus — die horen bij
kaart 2, en waarom staat hieronder.

### 1. Wat er landde

**De wire.** Eén enumwaarde erbij in `spec/schema/regelspraak-ir-v0.schema.json`,
tussen `LAATSTE` en `WORTEL` zodat het aggregatieblok aaneengesloten blijft.
Geen veld erbij, geen veld met een nieuwe betekenis — de knoop is de
bestaande `Functie` in de instantievouwvorm. Poort 2 leest 250 documenten
byte-identiek terug; de serialiser van die poort is soortonafhankelijk en
hoefde dus niets te leren, wat zelf de controle is dat de vorm additief is.

**De verifier.** Drie plaatsen. `Aantal_gevulde` in `functie_kind` (ast) en
in de `FunctieKind`-tabel (loader), en in `checks.ml` één regel in
`value_fold_name` plus één nieuw WF-037-blok van drie arms. Die ene regel in
`value_fold_name` doet het meeste werk: `set_source_exempt` en
`source_exempt` lezen die functie, dus de zes worden zeven zonder dat er een
letter aan de formules verandert.

**`ref`.** Eén arm: `Arith.agg_aantal (Arith.filled (values ()))`. `values ()`
weigert een `Instances`-bron al met de bestaande luide fout, dus de kale
rolbron valt daar vanzelf goed uit — de wire-fence van WF-037 zet dat hek
ervóór en de fout blijft de tweede verdedigingslinie.

**De spec.** `IR-SPEC.md` ch. 4.3 krijgt de kindrij en een eigen blok "The
filled count" (wire-vorm, waarom geen eigen knoop, waarom niet te
desuikeren, de zes-wordt-zeven-alinea, de drie WF-037-arms, de
WF-038-scope-zin); de WF-037-, WF-038- en WF-096-bullets van ch. 4.6 en hun
WF-index-rijen zijn bijgewerkt. `RUNTIME-SEMANTICS.md` krijgt ch. 3.4c met
vier tabellen absolute waarden, plus de ch. 1.2e-rij.

### 2. De afwijking van de uitvoeringsschets, prominent

De schets zegt bij WF-093: de nieuwe soort valt eronder "zonder dat er een
letter aan WF-093 verandert". Het MECHANISME veranderde inderdaad niet —
geen guard, geen melding, geen clausulevoorwaarde — en de negatief
`wf093-aantalgevulde-wrat` meet dat: zijn diagnose verschuift van WF-034
naar WF-093 op die ene regel in `value_fold_name` alleen. Maar de
regeltekst van WF-093 droeg een OPSOMMING ("under any of the other six
aggregation kinds (sum, total, minimum, maximum, first, last)"), en een
opsomming die niet meegroeit maakt de regel onwaar over precies de soort
waarvoor hij nu wél geldt. Die opsomming is dus wél aangepast, en er staat
bij dat het een opsomming van de familie is die met de familie meegroeit
terwijl het mechanisme onbewogen bleef. Hetzelfde geldt voor vier andere
plaatsen waar "de zes waardevouwen" als getal in lopende tekst stond
(ch. 4.3 tweemaal, WF-034's zevende golf, de WF-096-bullet en zijn
indexrij).

Tweede, kleinere afwijking: de schets kondigt "dezelfde zin in de twee
meldingsteksten die de zusterkaart herschreef" aan. Gemeten: die twee
WF-096-meldingen tellen niet — zij zeggen "a value AGGREGATION" en "a
single-argument VALUE fold" en blijven woordelijk juist zodra de familie
groeit. Wat het getal wél droeg waren de CODE-COMMENTAREN bij
`source_exempt` en `set_source_exempt`; die zijn bijgewerkt, de meldingen
niet. Een melding die geen getal noemt hoeft niet mee te groeien, en dat is
de reden dat zij zo geschreven was.

Derde punt, geen afwijking maar een keuze die de schets aan de uitvoering
liet: de schets noemt twee WF-037-arms, het zijn er DRIE geworden. De derde
is de eis van een `elementObjecttype`. Reden, gemeten: WF-037's bestaande
element-typehek is gespeld als "een waardevouw MOET ≥ 2 argumenten dragen"
en vuurt op een aggregatie zonder elementtype met minder dan twee operanden
— een zin die voor deze soort precies verkeerd om staat, want haar wet is
één argument. De soort is daarom uit die clausule gesloten en krijgt haar
eigen zin; mutant M6 meet wat die uitsluiting koopt.

### 3. Rood-eerst, per negatief

Zeven negatiefs, elk gemeten tegen de verifier MET de enumwaarde en ZONDER
de arms (md5 `220741bf2cee253d71ca6b13b8ad1402`) en daarna met de arms
(md5 `d970a571d0ecbc88de4bea5b14da9fc7`):

    wf037-aantalgevulde-tweede-argument        PASS  -> WF-037 (ariteit)
    wf037-aantalgevulde-zonder-elementtype     PASS  -> WF-037 (elementtype)
    wf037-aantalgevulde-niet-setbron           PASS  -> WF-037 (setbron)
    wf037-aantalgevulde-leegwaarde             WF-037 + WF-096 -> WF-037
    wf037-aantalgevulde-dimensieselectie       WF-037 + WF-096 -> WF-037
    wf037-aantalgevulde-granulariteit          WF-037 + WF-096 -> WF-037
    wf093-aantalgevulde-wrat                   WF-034 -> WF-093

De drie facetnegatiefs waren vóór de arms al rood maar om de VERKEERDE
reden mede-rood: hun setbron viel nog buiten de exemptie. Na de verbreding
dragen zij één citatie. De welgevormde referentievorm (niet gecommit; zij
hoort bij de golden van kaart 2) ging van WF-096-rood naar schoon.

Elke negatief draagt precies ÉÉN citatie. Dat kostte één ontwerpkeuze: de
setbron-arm stapt opzij op de sentinel, want elke kale wrat is per
constructie een niet-setlezing en zou anders twee regels op één
malformatie zetten. Mutant M5 meet dat.

### 4. Corpussweep en mutanten, werkelijk gedraaid

Sweep over golden + rood + negatief + pariteitszone, **695 documenten**,
vóór en na: bewegen doen uitsluitend de zeven nieuwe negatiefs. Geen
golden, geen rode casus, geen bestaande negatief en geen pariteitsdocument
verandert een oordeel of een regel van zijn verslag.

Zes arm-exacte mutanten op de verifier, elk gedraaid, gesweept en
teruggedraaid (aantal bewogen regels in de 695):

    M1  Aantal_gevulde uit value_fold_name      5  (3 facetnegatiefs krijgen
                                                    WF-096 terug; de wrat valt
                                                    terug op WF-034)
    M2  de ariteitsarm uit                      2  (de getuige blijft rood maar
                                                    onder de elementtype-zin)
    M3  de elementtype-arm uit                  4  (de getuige wordt schoon)
    M4  de setbron-arm uit                      4  (de getuige wordt schoon)
    M5  de sentinel-uitgang uit                 1  (de wrat krijgt een tweede
                                                    citatie voor één defect)
    M6  de uitsluiting uit de ≥2-clausule       1  (de elementtype-getuige krijgt
                                                    "value fold with fewer than
                                                    two operands" erbij)

M2 is eerlijk de zwakste: de getuige wordt niet schoon maar verschuift van
zin. Dat is geen defect maar de vorm van een ketting van drie arms — de
eerste weghalen laat de latere spreken. De arm wordt dus gemeten aan de zin
die hij bezit, en aan het feit dat geen ander document van de 695 beweegt.

Drie mutanten op de uitvoerders, elk gedraaid en teruggedraaid:

    ref: de Aantal_gevulde-arm uit    -> de suite sterft bij WV-3.4c-gemengd-aantal
                                        op "function kind not implemented in this
                                        card", een BODEMZIN van poort 8
    clingo: de benoemde arm uit       -> 2 FAILURES; de weigering wordt
                                        "expression kind not translated", de
                                        bodemzin die poort 8 verbiedt te pinnen
    Drools: de benoemde loader-arm uit -> IllegalArgumentException: No enum
                                        constant ...FunctieKind.AANTAL_GEVULDE,
                                        een onbeheerde uitzondering waar gate 12
                                        een ERROR van maakt in plaats van een
                                        kanaal met reden

Werkboom na alle mutanten: schoon (`git status` leeg).

### 5. De uitvoerders, en waarom kaart 1 hier ophoudt

`ref` DRAAGT de vorm. clingo en beide Drools-takken niet, en dat is correct
voor deze kaart: zij zijn kaart 2. Wat deze kaart wel deed is hun weigering
BENOEMD maken, want anders zou de vorm in een bodemzin verdwijnen.

- **clingo** krijgt een eigen arm vóór de catch-all, op het precedent van de
  gelijste som ("de arm bestaat voor zijn NAAM"), plus twee checks in de
  eigen suite: dat de weigering de vorm noemt, en dat zij NIET de bodemzin
  is. Suite 452 -> 454.
- **De Drools-loader** weigert een niet-gedragen `Functie.kind` nu als
  benoemde `Refusal`, op het precedent van `Kwantorvouw.kwantor`, met een
  JUnit-getuige. Suite 502 -> 504. De arm is voor de KLASSE geschreven, niet
  voor deze ene soort: de volgende wire-first-soort verdient hetzelfde
  antwoord. `frontends/**` bleef onaangeraakt, dus `Expr.FunctieKind` kent
  de soort nog niet — dat hoort bij kaart 2, die de transpiler-armen bouwt.

### 6. Poortuitkomsten (elk los gedraaid)

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  250 documenten, vloer 192
     3 check-verifier           PASS  250 golden schoon; 408 -> 415 negatief, vloer 350
     4 check-case-schema        PASS  586 casusbestanden
     5 check-oracle             PASS  ref-suite 686 -> 708, vloer 489; 552 cases
     6 check-clingo             PASS  backendsuite 452 -> 454, vloer 83; 552 cases
     7 check-noop               PASS  516 cases, 36 overlevers
     8 check-rood               PASS  34 rode cases; 68 fragmenten citeren geen
                                      van de 36 bodemzinnen
     9 check-catala             PASS  201 checks; 12 goldens
    10 check-regelspraak        PASS  322 tests; 63 rgs-goldens
    11 check-toka-parity        PASS  29 cases, nul divergenties
    12 check-drools             PASS  gespeeld 164 (pin), DRL-tekst 141 (pin),
                                      JUnit 502 -> 504, vloer 239
    13 check-reftype-derivation PASS  129 twists, vloer 74
    14 check-flogic             PASS  130 checks; 2 goldens

Geen vloer bewoog en geen gelijkheidspin bewoog. Poort 12's
`EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` bleven op 164 en 141 omdat deze
kaart geen golden toevoegt — precies de reden dat de kaart er geen
toevoegt. `scripts/` is dus onaangeraakt.

### 7. Wat bewust NIET gebeurde

- **Geen golden.** Een golden zonder clingo-drager breekt poort 6, want
  `ref ≡ clingo` is daar geen streefdoel maar de poort zelf. De worked
  examples wonen in `ref/test/`, waar poort 5's tweede been ze draait.
- **Geen rode casus.** De rode zone eist twee weigerende uitvoerders met
  gepinde fragmenten. `ref` weigert de vorm juist niet (hij draagt hem), dus
  er is niets te pinnen dat niet onwaar zou zijn.
- **Geen hek op het knooptype.** De soort levert per constructie een
  eenheidloze kardinaliteit, maar de verifier eist dat niet van het
  `type`-veld — precies zoals hij dat van `AANTAL` niet eist. Bewust
  gespiegeld en hier genoteerd als open flank, niet als stilte. Poort 13
  ziet hem niet zolang er geen golden is; als de goldens van kaart 2 een
  eenheiddragend `type` op de knoop zouden zetten, is dat waar het opvalt.
- **`frontends/**`.** Besluit G9: de dialectzin is kaart 3.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`.** Voorstelrijtekst hieronder.

### 8. Registervoorstel (hoofdsessie, serieel)

Voor `spec/CONSTRUCTS.md` rij 1 (de aggregatierij), aan te hangen:

> **Landed (2026-09-20): `AANTAL_GEVULDE`, de waardentelling** (IR-SPEC
> ch. 4.3, `RUNTIME-SEMANTICS.md` ch. 3.4c; maintainersbesluiten G1–G8 van
> 2026-09-19). Een achtste vouwsoort op dezelfde `Functie`-knoop, die per
> bijdrager een WAARDE leest en telt hoeveel daarvan niet leeg zijn. **Geen
> spiegeling maar een v1-delta**: de brontaal kent de operatie niet — haar
> §5.8-catalogus is een gesloten alternatie van vijf vormen, "gevuld"
> bestaat er alleen als predicaat over één onderwerpexpressie, en het
> typeringendocument sluit de waardelezing expliciet uit van de
> telloperator. Daarom is de vorm WIRE-FIRST en heeft zij in geen enkele
> frontend een producentroute; de dialectspelling is een eigen latere kaart
> (besluit G9). De soort dwingt de setbron af in plaats van haar toe te
> staan (WF-037, drie arms), draagt nooit `leegwaarde`, `dimensieSelectie`
> of `granulariteit`, en levert een geheel, eenheidloos getal. WF-096's
> exemptie gaat van zes naar ZEVEN waardevouwen — de toepassing van de
> versmalling van 2026-09-19, niet een uitzondering erop: die versmalling
> vraagt of een soort een waarde per bijdrager leest, en deze doet dat.
> WF-093 en WF-034's sentinelfence bewogen geen mechanisme; alleen hun
> opsommingen groeiden mee. Het gemiddelde blijft handwerk als
> `SOM ÷ AANTAL_GEVULDE`, met de deel-door-nul op een geheel lege bron
> bewust bij de regelschrijver (besluit G1). Zeven negatiefs; clingo en
> beide Drools-takken weigeren de soort voorlopig BENOEMD, met getuige in
> de eigen suite. Uitvoering in de backends: kaart 2.

Voor `spec/PLAN.md` is er geen scopewijziging: de kaart voert uit wat het
ontwerp van 2026-09-19 al vastlegde. Wel een feit dat vermelding verdient
als de hoofdsessie een regel wil: ch. 3.4b's woord ELEMENTS heeft sinds
deze kaart zijn drager, en ch. 3.4b is daarop bijgewerkt.

### 9. Overdracht naar kaart 2

Wat er nog moet gebeuren, en waar het staat.

**clingo** moet `Functie { kind = Aantal_gevulde; _ }` vertalen in plaats
van weigeren. De arm staat in `backends/clingo/lib/translate.ml`, vlak
vóór de catch-all van `expr_term`, en zegt zelf wat er komen moet: een
`#count` over dezelfde aanwezigheidsliteral waarmee de som de
presence-guard van min/max al bouwt. Het `set`-bronpatroon van de
SOM/TOTAAL-arm is de vorm om te kopiëren (zelfde bronvorm, zelfde
`bronRol`/`bronPad`-behandeling, zelfde filter). De twee checks in
`backends/clingo/test/test_clingo.ml` moeten dan van weigering naar
vertaling; laat de tweede check (NIET de bodemzin) staan of vervang hem
door een echte replay.

**Drools** moet twee dingen leren, in deze volgorde. (1)
`AANTAL_GEVULDE` in `Expr.FunctieKind`
(`frontends/regelspraak/src/main/java/.../ir/Expr.java`) — dat is de
gedeelde IR-recordvorm die de backend meecompileert, dus die stap raakt
`frontends/**` en hoort bij kaart 2, niet bij de dialectkaart. Zodra die
constante bestaat, bereikt de soort de loader-Refusal niet meer en moeten
(2) de twee transpilers hem dragen of per vorm weigeren:
`ExecModelTranspiler` (de accumulate-CE, zie zijn `count`-tak rond de
`AANTAL`-behandeling) en `DrlTranspiler` (de DRL-tekst-tak, zijn
`case AANTAL -> "count"` is de plek). Een per-vorm-weigering daar komt
vanzelf met reden in het kanaalverslag terecht; een gedragen vorm dwingt
`EXPECTED_PLAYED` (164) en `EXPECTED_DRL_PLAYED` (141) in dezelfde commit
omhoog, en dat zijn GELIJKHEDEN in `scripts/check-drools.sh`, een beschermd
pad.

**De weigerarmen die er nu staan**, zodat kaart 2 weet wat hij vervangt:

    clingo   expr_term, vlak vóór de catch-all:
             "the filled-value count (AANTAL_GEVULDE) is not translated: this
              backend carries no #count over the presence of a set source's
              values yet, so it refuses the kind by name rather than answering"
             getuige: backends/clingo/test/test_clingo.ml, twee checks
    Drools   IrLoader.expr, case "Functie", Refusal("Functie.kind", …):
             "'<kind>' is an aggregation or builtin kind this backend does not
              carry; the wire form is defined (IR-SPEC ch. 4.3) and a transpiler
              arm for it has not landed here yet"
             getuige: OnbekendeFunctieKindWitnessTest (2 tests)
             NB: deze arm is voor de KLASSE, niet voor deze soort — hij blijft
             staan en blijft nuttig als AANTAL_GEVULDE hem straks passeert
    ref      geen weigering: ref DRAAGT de vorm (eval.ml, arm Aantal_gevulde);
             de kale rolbron valt op de bestaande "value aggregation over a
             bare role source", tweede verdedigingslinie achter WF-037

**De vier getuigen van kaart 2** staan in §5 van de ontwerpkaart en zijn
onveranderd geldig. Twee dingen zijn nu gemeten in plaats van verwacht:
`ref` rekent de `bronPad`-vorm werkelijk (WV-3.4c-bronpad-vlakke-unie en
-gedeeld-element), dus de `bronPad`-golden kan absolute verwachtingen
dragen zodra clingo hem ook kan; en de typeloosheid is per waardetype
gepind in `ref/test/`, dus de niet-numerieke getuige hoeft alleen nog door
het vierkant heen.
