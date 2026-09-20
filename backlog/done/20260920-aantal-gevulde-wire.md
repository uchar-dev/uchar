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
- ~~**Geen hek op het knooptype.**~~ **INGETROKKEN in de fixronde van
  2026-09-20 — de passage die hier stond was onjuist.** Zij beweerde dat de
  verifier het knooptype van `AANTAL` evenmin hekt, en dat is niet waar:
  `AANTAL` HEEFT dat hek, als een arm van WF-091's afgeleide-typemotor. De
  nieuwe soort had hem niet, omdat die motor op de KIND matcht. Het gat is
  gemeten, gedicht en van een getuige voorzien; zie §10.
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
> **Fixronde dezelfde dag:** het knooptype is DERIVED en wordt gekruist —
> WF-091's afgeleide-typemotor krijgt een twaalfde positie, want die motor
> matcht op de KIND en dekte de nieuwe soort niet. Wat dat dichtte is
> gemeten: een gevulde telling geannoteerd `kg` in een even liegend
> `kg`-doel was verifier-SCHOON terwijl dezelfde leugen onder `AANTAL`
> WF-091 gaf, en `ref` speelde hem uit (een eenheidloze 1 in een
> kg-gedeclareerd attribuut, oracle PASS). Getuige
> `wf091-aantalgevulde-eenheid`. En ch. 3.4c draagt sindsdien één
> normatieve uitsluiting die eerder alleen code-commentaar was: de gevouwen
> attribuut is niet TIJDSAFHANKELIJK — een telling heeft geen moment waarop
> zij een tijdlijn naar gevuldheid vraagt, dus die vorm faalt luid.

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

*En clingo heeft DRIE eenheidsafleiders die de soort moeten kennen*
(nagemeten 2026-09-20): `eff_unit` (rond :459), `provably_unitless`
(:1007) en `provably_pure_pct` (:1043). Alle drie matchen op
`Som | Totaal | Minimum | Maximum | Eerste | Laatste` en geven de eenheid
van operand 0 door. Voor een kardinaliteit is dat precies het VERKEERDE
antwoord, dus de soort mag daar niet bij de zes worden gezet. Gemeten
gevolg vandaag, en het is de spiegel van `AANTAL`: die staat er evenmin
in, dus `eff_unit` valt door naar `None` (juist — geen eenheid) en
`provably_unitless` naar `false` (behoudend: niet-bewijsbaar-eenheidloos
in plaats van fout). `AANTAL_GEVULDE` erft exact die positie, dus kaart 2
hoeft er niets te REPAREREN; wat hij moet WEGEN is of hij beide tellingen
aan `provably_unitless` toevoegt. Dat is een verbreding die meer rekenwerk
vertaalbaar maakt, zij geldt voor `AANTAL` net zo goed, en zij hoort dus
als eigen besluit genoteerd te worden in plaats van stilzwijgend
meegenomen.

*Poort 13 (`scripts/check-reftype-derivation.sh`) hoeft niets te leren, en
dat is gemeten in plaats van aangenomen.* De twisttabel raakt alleen
posities waar `eenheiddragend(type)` waar is, en een kardinaliteit is per
constructie eenheidloos. `AANTAL` staat om diezelfde reden noch in
`AFGELEIDE_KIND` noch in `GEEN_FUNCTIE`; `AANTAL_GEVULDE` erft die
positie. De WF-091-indexrij noemt dat verschil nu bij naam: de
afgeleide-typeinventaris en poort 13's uitzonderingenset zijn twee
handmatig uitgelijnde lijsten zonder tripwire, en zij verschillen op beide
tellingen. Wat WEL beweegt zodra kaart 2 een golden toevoegt is de TELLER:
de BRON van de soort is een echt getypeerde attribuutlezing en wordt
getwist zoals elke som-bron — en de telling moet daar onverschillig voor
zijn, wat precies is wat de harnas aantoont.

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

## Fixronde (2026-09-20, na de lensronde)

De lens keurde af op één blokkerende vondst en gaf vijf adviezen. Alle zes
zijn uitgevoerd. Wat hieronder staat vervangt §7's eerste bullet, die
onjuist was.

### 10. BLOKKEREND, gedicht: het knooptype was ongehekt

**De vondst.** `IR-SPEC.md` ch. 4.3, de WF-038-rij en
`RUNTIME-SEMANTICS.md` ch. 3.4c zeggen alle drie dat een gevulde telling
een geheel, eenheidloos getal oplevert. Er stond geen check tegenover. De
afgeleide-typemotor van WF-091 (`composite_expectation` in
`spec/verifier/lib/checks.ml`) matcht op de KIND, dus de `Aantal`-arm dekte
de nieuwe soort niet, en die was daarmee de ENIGE aggregatie wier knooptype
door niets werd gekruist.

**Gemeten, vóór de fix** — en het is erger dan een document dat schoon is
en dat niet hoort te zijn, want de leugen wordt UITGESPEELD. Een
`AANTAL_GEVULDE` met knooptype `kg`, geschreven in een even liegend
`kg`-doel zodat WF-023 tevreden is:

    verifier (AANTAL_GEVULDE) : PASS
    verifier (AANTAL, zelfde leugen) : WF-091 FAIL: unit mismatch … but the
                                       type a count derives is no unit
    ref op datzelfde document      : oracle: PASS (1 expectations)
                                     — een eenheidloze 1 landt in een
                                       kg-gedeclareerd attribuut

De spiegel-claim van §7 ("precies zoals hij dat van `AANTAL` niet eist") was
dus onwaar in beide helften: `AANTAL` heeft het hek wél, en de nieuwe soort
miste het.

**De fix.** Eén arm naast de `Aantal`-arm, met dezelfde afgeleide
verwachting (geheel, 0 decimalen, niet-negatief, geen eenheid) en een eigen
naamzin, zodat de melding de soort noemt die loog. Twaalf afgeleide posities
in plaats van elf.

**Rood-eerst, en mutant, in één meting.** De verifier zónder de arm is
BYTE-IDENTIEK aan de binary die de soort landde (md5
`d970a571d0ecbc88de4bea5b14da9fc7`); mét de arm is hij
`cb504f858c625baf82378a77d00362bf`. De rood-eerst-toestand en de
arm-exacte mutant zijn daarmee letterlijk dezelfde meting. Sweep over
golden + rood + negatief + pariteit, **696 documenten**: precies één
document beweegt, de nieuwe negatief, van PASS naar WF-091. Niets anders
verandert een oordeel of een regel.

**Tweede mutant, op de facet in plaats van op de arm.** De eenheidsfacet
van de identiteitsmotor geneutraliseerd: **acht** afgeleide
eenheidsgevallen worden schoon, `wf091-aantalgevulde-eenheid` als achtste.
De WF-091-indexrij zei "all **7**" en is naar 8 bijgewerkt met de datum
erbij — dat getal was een controleerbare claim en zij werd door deze kaart
onwaar.

**Getuige.** `corpus/negatief/wf091-aantalgevulde-eenheid`, één citatie,
met NOTE en beide falsificatieproeven. De inventarissen die de positie
opsommen zijn meegegroeid: ch. 4.1's positietabel (elf → twaalf rijen), de
zin over de SIGN-uitsluiting ("ALL ELEVEN" → "ALL TWELVE"), honesty note
(1) en (4) van de WF-091-rij, de negatief-kolom ("thirteen more" →
"fourteen"), en de lijst waar de WF-091-inventaris en poort 13's
twisttabel van elkaar verschillen — daar staat de nieuwe soort nu naast
`AANTAL`, met de reden dat een kardinaliteit per constructie eenheidloos is
en dus door geen twisttabel bereikt wordt.

### 11. De vijf adviezen

**(a) De temporele uitsluiting was code-commentaar, geen norm.** ch. 3.4c
zei "whatever its type" en de WF-038-rij "its source may carry ANY type",
terwijl `ref` elke niet-Som/Totaal-vouw over een tijdlijnattribuut luid
weigert. Het gedrag is juist — alleen som en totaal definiëren de eerste
inklapping van ch. 3.6/§7.1, en een telling heeft geen MOMENT waarop zij
een tijdlijn naar gevuldheid vraagt — maar een NOTE draagt geen norm.
Rechtgezet: ch. 3.4c draagt nu een normatieve alinea die de uitsluiting op
de TEMPORELE as legt in plaats van op een waardetype, met "de deur dicht,
niet op slot"; ch. 4.3 en de WF-038-rij zeggen hetzelfde in één zin; en er
is een getuige, `WV-3.4c-temporeel-faalt`, die de weigering op haar zin
pint. Ref-suite 708 → 709.

**(b) Een bestaande flank, gemeten, met eigen kaartje.** De OPGESOMDE
`AANTAL` (twee of meer argumenten, geen `elementObjecttype`) telt in `ref`
nog steeds de GEVULDE waarden — precies de betekenis die het besluit van
2026-09-19 van de wire haalde in de setbron-spelling. Zelf nagemeten op een
handgebouwd document met twee eigen kg-lezingen waarvan er één leeg is:

    verifier : PASS
    ref      : 1     (Arith.filled vóór agg_aantal)
    clingo   : TRANSLATION FAULT: expression kind not translated
    Drools   : EXCLUDE, transpiler SKIP (aggregatie buiten Phase-C scope)

Eén antwoordt, twee weigeren, onder een welgevormd document — de klasse die
PLAN §6 een specfout noemt. De vorm overleeft doordat WF-037's
≥2-clausule `Aantal` uitsluit, terwijl de brontaal (§5.8.1, typeringen
§4.11) geen opgesomde telling kent. Corpusbreed gemeten over alle 696
documenten: precies ÉÉN drager, `corpus/negatief/wf091-aantal-decimalen`,
al afgewezen om een andere reden. Dit is NIET door deze kaart veroorzaakt
en niet door deze kaart opgelost: het is een normvraag voor de maintainer,
en zij staat met meting en drie routes in
`backlog/new/20260920-aantal-opgesomde-telling.md`. Hier genoteerd als open
flank naast de andere.

**(c) Poort 2 ziet `corpus/negatief` niet.** De acceptatie "(a) Wire … poort
2 groen, round-trip byte-identiek" was over het nieuwe lid dus LEEG: poort 2
scant alleen `corpus/golden` en `corpus/rood`. Alsnog gemeten, met poort 2's
eigen script in expliciete modus over alle acht nieuwe negatiefs:
`8 file(s) validate and round-trip`. Genoteerd als regel: zolang geen golden
de soort draagt, is dat de enige meting die het nieuwe enumlid tegen het
schema houdt, en zij is handmatig. De golden van kaart 2 wordt de blijvende,
gegate getuige.

**(d)** in §9 verwerkt.

**(e) Cosmetisch, hersteld.** In `wf093-aantalgevulde-wrat/ir.json` zei de
`bron`-zin nog "het aantal gevulde waarden van HET GEWICHT van alle
vondsten" terwijl het argument de kale wrat is. Nu: "het aantal gevulde
waarden van de vondsten van de Jutter".

### 12. Poortuitkomsten na de fixronde

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  250 documenten, vloer 192
                                      (+ expliciete modus: 8 negatiefs valideren
                                       en round-trippen byte-identiek)
     3 check-verifier           PASS  250 golden schoon; 416 negatief, vloer 350
     4 check-case-schema        PASS  586 casusbestanden
     5 check-oracle             PASS  ref-suite 709, vloer 489; 552 cases
     6 check-clingo             PASS  backendsuite 454, vloer 83; 552 cases
     7 check-noop               PASS  516 cases, 36 overlevers
     8 check-rood               PASS  34 rode cases; 68 fragmenten citeren geen
                                      van de 36 bodemzinnen
     9 check-catala             PASS  201 checks; 12 goldens
    10 check-regelspraak        PASS  322 tests; 63 rgs-goldens
    11 check-toka-parity        PASS  29 cases, nul divergenties
    12 check-drools             PASS  gespeeld 164 (pin), DRL-tekst 141 (pin),
                                      JUnit 504, vloer 239
    13 check-reftype-derivation PASS  129 twists, vloer 74
    14 check-flogic             PASS  130 checks; 2 goldens

### 13. Openstaande flanken na deze kaart, volledig

1. De OPGESOMDE `AANTAL` telt gevulde waarden — gemeten, niet van deze
   kaart, kaartje `backlog/new/20260920-aantal-opgesomde-telling.md`.
2. Het MOMENT van een gevulde telling over een tijdlijnattribuut is niet
   gedefinieerd; ch. 3.4c zegt nu normatief dat de vorm luid faalt, en dat
   een latere kaart met een echte casus de momentregel mag leveren.
3. clingo en beide Drools-takken dragen de soort niet; zij weigeren hem
   benoemd. Kaart 2.

Het knooptype-hek staat NIET meer op deze lijst.
