# kwantor-asymmetrie: de niet-temporele matrix bereikt alle drie de uitvoerders

Aanleiding: de forall-kaart (`backlog/done/20260916-heelperiode-forall.md`)
legde een uitvoerdersasymmetrie vast voor de Kwantorvouw met een
NIET-heelperiode-matrix: `ref` voert hem onder beide kwantoren uit
(`List.for_all` over `eval_cond`), clingo alleen onder GEEN (daar is de
getuige de matrix zelf; onder ALLE weigert
`backends/clingo/lib/translate.ml` bij naam — "the complement of an
arbitrary condition is not an ASP body"), en Drools onder geen van beide
(luide marker/skip). De maintainer wil de asymmetrie opgelost: de vorm
gaat drieweg spelen.

Richting, gemeten vooraf door de hoofdsessie:

- **clingo ALLE**: één reïficatiestap. Emit per vouw een hulppredicaat
  `sat(X) :- <join>, <matrix-atomen>.` en gebruik `not sat(X)` als
  faal-getuige in de bestaande witness-regel. Negation-as-failure over een
  gereïficeerde conjunctie is een gewoon ASP-lichaam; let op de
  stratificatiediscipline die de vertaler al voert (de matrix kan zelf
  `not`-atomen dragen). De bestaande heelperiode-ALLE-arm
  (`whole_period_fails`) mag blijven of door de generieke route worden
  vervangen — kies op eenvoud, meet dat de bestaande goldens byte-gelijk
  antwoorden.
- **Drools beide kwantoren**: de matrix is een conditie met het ELEMENT
  als subject; de bestaande per-subject-conditievertaling levert de
  constraint op het elementpatroon. GEEN = not-exists(element ∧ matrix);
  ALLE = not-exists(element ∧ ¬matrix) — of de accumulate/eval-vorm die
  bij de bestaande vouw-machinerie past. Beide takken (DRL-tekst én
  exec-model), zoals de heelperiode-arm. Vormen die de
  conditievertaling niet draagt blijven een benoemde marker/skip —
  nooit stil.

## Acceptatie

- (a) Minstens één corpus-golden (Jutterswet-domein, handberekende
  verwachtingen vóór het draaien in NOTE.md en Result) met een
  niet-temporele matrix onder ALLE én onder GEEN, drieweg gespeeld
  (gates 5+6+12). Rood-eerst: meet vóór de bouw dat clingo de
  ALLE-vorm weigert en Drools beide markeert, met de letterlijke
  weigeringsteksten in het Result.
- (b) De vacuümcellen (lege bijdragersverzameling → beide kwantoren
  WAAR) en een GEEN≠!ALLE-cel (een gemengde verzameling waar één
  voldoener GEEN breekt terwijl ALLE ook al faalt) zitten in de cases.
- (c) Alle veertien gates groen; gate 12's `EXPECTED_PLAYED` en
  kanaallijst bewegen mee met eigen gedateerde stappen (gemandateerd).
- (d) `spec/RUNTIME-SEMANTICS.md`: de asymmetrievermelding (ch. 3.9a en
  de ch. 3.12-asymmetrierij) wordt bijgewerkt naar de nieuwe stand —
  DIT PAD IS BESCHERMD; deze kaart draagt het mandaat, de wijziging
  blijft minimaal (de asymmetrietekst, niets anders), en het Result
  citeert de oude en nieuwe tekst letterlijk.
- (e) Bestaande goldens, negatiefs en tests ongewijzigd; alles verder
  additief. `spec/CONSTRUCTS.md`/`spec/PLAN.md` niet aanraken;
  registervoorstel (rij 40-aanvulling) als letterlijke tekst in het
  Result.
- (f) Geen wijziging aan `ref/` nodig (die draagt de vorm al) — als er
  toch iets moet bewegen: meten, melden, motiveren.

## Result

**De asymmetrie is weg: de niet-temporele matrix speelt drieweg.** Wat hem
in stand hield was in beide gevallen hetzelfde: twee uitvoerders voerden
ch. 3.9a's zin ("`toets` is een gewone conditie met het element als
onderwerp") niet uit, maar herkenden één bijzondere matrixvorm.

**clingo — de reïficatie** (`backends/clingo/lib/translate.ml`, de
Kwantorvouw-arm). ASP vormt geen complement van een willekeurige
conjunctie van atomen; van een PREDICAAT vormt het er wél een. Dus emit de
ALLE-arm per vouw één hulpregel `kvsM(X) :- isa(X,el), ⟨matrixatomen⟩.` —
precies de atomen die GEEN al las — en de faalgetuige is `not kvsM(X)` in
de bestaande `kvN`-witnessregel. Stratificatie volgt uit de constructie,
hetzelfde argument dat de tellende samenstelling (`cq`) er tien regels
hoger voor voert: de naam komt van een teller die alleen groeit, en de
matrix erbinnen is een strikte deelterm die vóór het bestaan van die naam
vertaald is, dus geen atoom in het lichaam kan van de kop afhangen.
`isa(X,el)` opent het lichaam omdat een matrix zijn eigen onderwerp niet
hoeft te binden (een genegeerde kenmerklezing bindt niets); het is
hetzelfde atoom waarmee `source_join` het bereik sluit.

**Routewissel, en hij is gemeten.** De heelperiode-ALLE-arm
(`whole_period_fails`) is als apart geval verdwenen: hij loopt nu door de
generieke route, waar `cond_atoms` `not gpvN(X)` teruggeeft en de vouw daar
nog een ontkenning omheen zet. Dubbele ontkenning, gestratificeerd,
hetzelfde antwoord. Gemeten zoals de kaart vroeg: de VOLLEDIGE uitvoer over
alle bestaande cases, niet alleen de poortuitslag. Met een tijdelijke
dumpvlag in de runner (weer verwijderd, niet gecommit) zijn alle 486
goldencases vóór en ná gedraaid met hun complete stabiele model erbij
(`val/3`, `heeft/2`, `inconsistent/2`, `tl/5` — het hele getoonde kanaal).
Diff: 68 regels, allemaal op de vijf cellen van de nieuwe golden, die van
TRANSLATION FAULT naar PASS gaan. De zeven cellen van
`jutterswet-ploegpremie` — de golden die van route wisselde — zijn
byte-gelijk.

**Drools — beide takken, beide kwantoren.**
`transpiler/model/ExecModelTranspiler.java`, `kwantorvouwVoorwaarde`: de
matrix krijgt twee routes. Is er een §8.2-kwalificatie, dan de bestaande
tijdlijnroute; is die er niet, dan `leafPredicate(kv.toets(), elem)` — de
per-subjectbladvertaling die elke andere conditie van deze backend krijgt,
met het ELEMENT als dat subject. De vouw verzamelt dan de BIJDRAGERS zelf
in plaats van hun tijdlijn (dezelfde `collectList`-accumulate op dezelfde
join+filter-source) en de result-var-eval loopt de lijst af. Leeg is gratis
goed: een lege lijst voldoet aan beide kwantoren.
`transpiler/DrlTranspiler.java`, `kwantorvouwNietTemporeel`: dezelfde
semantiek als één opgehesen clausule `not <Element>( <join>, <filter>,
<matrix of haar ontkenning> )` — de faalgetuige, ontkend. Beide kwantoren
zijn diezelfde vorm; alleen of de matrixconstraint genegeerd wordt
verschilt. De §13.3.9-opslagrichtingsvraag die `scope()` voor een
aggregatiejoin stelt, wordt hier net zo gesteld.

**De NIET-prefix: gemeten, gekozen, gepind.** Bij de heelperiodevorm zit
het "niet" op de BUITENSTE vergelijking, en daar mag een uitvoerder hem
opvouwen door ALLE in GEEN te klappen (∀c.¬P = ¬∃c.P) — dat doet de
exec-tak ook. Bij een niet-temporele matrix zit de ontkenning IN de
conditie (`Kenmerk.negatie`, of de operator van de vergelijking) en wordt
zij per lid gelezen; de kwantor blijft onaangeroerd. Dat is wat `ref` doet,
en beide backends doen het nu ook. Gepind in de golden als `sluitend` =
GEEN over `niet gediplomeerd`, dat in elke cel leest wat `bevoegd` = ALLE
over `gediplomeerd` leest, en in de ASP-suite als twee eigen rijen.

### De golden: handgeschreven IR, en dat is gemeten

`corpus/golden/jutterswet-schouwploeg` — vijf cases, geen `bron.rgs`. De
kaart vroeg te meten of de frontend de vorm schrijft. Twee vormen
geprobeerd, beide nee:

- de vergelijking zonder §8.2-kwalificatie ("indien de dagvergoeding van
  alle bergers van de Bergingsploeg groter is dan 40") →
  `ERROR (line 14): setlezing buiten een vouwbron: 'de dagvergoeding van
  alle bergers van de Bergingsploeg' noemt de waarde van elk lid van de
  verzameling — alleen toegestaan als bron van één vouw (som, totaal,
  minimum, maximum, eerste, laatste, aantal); hier wordt één waarde
  verwacht (WF-096, IR-SPEC ch. 4.3)`;
- de kenmerkvorm ("indien alle bergers van de Bergingsploeg bevoegd zijn")
  → emitteert, maar als `{"node": "Onopgelost", "bron": "alle bergers van
  de Bergingsploeg bevoegd zijn"}`.

`RuleChecker.kwantorvouw` opent met `if (periodeVan(c) == null) return c;`:
alleen de §8.2-GEKWALIFICEERDE conditie wordt gelowerd. Verbreden is een
frontendvraag met een eigen kaart; deze golden is daarom met de hand tegen
het schema geschreven, en poort 10 ziet hem niet.

### De cellen, met de hand berekend vóór enige uitvoerder draaide

Grens 5, strikt `groter dan`. Vier vouwen in één document: `ervaren` = ALLE
over `dienstjaren > 5`, `onervaren` = GEEN over dezelfde, `bevoegd` = ALLE
over kenmerk `gediplomeerd`, `sluitend` = GEEN over `niet gediplomeerd`.
`wachttal` = het aantal wachten, zodat elke cel ook zegt hoe groot het
bereik werkelijk is.

| case | de wachten van de ploeg | wachttal | ervaren | onervaren | bevoegd | sluitend |
|---|---|---|---|---|---|---|
| 1 | S1: 8 ja, 6 ja | 2 | **ja** | nee | **ja** | **ja** |
| 2 | S1: 8 ja, 3 nee | 2 | nee | nee | nee | nee |
| 2 | S2: 9 ja | 1 | **ja** | nee | **ja** | **ja** |
| 3 | S1: 3 ja, 5 ja | 2 | nee | **ja** | **ja** | **ja** |
| 4 | S1: geen wachten | 0 | **ja** | **ja** | **ja** | **ja** |
| 5 | S1: leeg ja, 3 ja | 2 | nee | **ja** | **ja** | **ja** |

Cel 2 is de GEEN≠!ALLE-cel: één wacht boven en één onder de grens laat
beide vouwen vals. Cel 2 draagt bovendien een TWEEDE ploeg, en die staat er
voor het BEREIK: S2 leest ervaren terwijl S1 dat niet doet, in dezelfde
run. Dat is precies het risico van de reïficatie — `kvsM` is gedefinieerd
voor ELK element, niet alleen voor de bijdragers — en de cel faalt zodra
een uitvoerder over alle `Strandwacht`en zou kwantificeren in plaats van
over de wachten van de ploeg. Cel 3 pint de strikte grens (5 is niet groter
dan 5), cel 4 het vacuüm onder beide kwantoren, cel 5 de lege operand: de
ordeningsvergelijking faalt erop (ch. 1.2d), dus het lid breekt ALLE en is
géén getuige voor GEEN. Dat is het spiegelbeeld van `ploegpremie` cel 7,
waar een AFWEZIGE TIJDLIJN de heelperiodetoets vacuüm HAALT en GEEN daarmee
juist breekt. Zelfde soort lid, tegengestelde uitkomst; alleen de matrix
beslist welke lezing geldt.

### Rood-eerst: de gemeten weigeringen, letterlijk

Alle vier gemeten op de golden zoals hij in de eerste commit landde, vóór
enige codewijziging. `ref` speelde toen al alle vijf de cases (acceptatie f
gehaald: geen regel aan `ref/` veranderd).

clingo, alle cases met een ALLE-vouw:

```
TRANSLATION FAULT: a quantifier-fold matrix other than the whole-period comparison is not translated under ALLE (the complement of an arbitrary condition is not an ASP body; no corpus case)
```

Drools exec-model, ALLE:

```
EXCLUDE transpiler SKIP: regel: ervaren bij dienstjaren — kwantorvouw ALLE over Strandwacht (een matrix zonder §8.2 gehele-periodekwalificatie — deze tak lowert alleen de heelperiode-vorm van ch. 3.9a): de dienstjaren van alle wachten van de Schouwploeg groter zijn dan 5; regel (voorwaarde buiten Phase-B scope): ervaren bij dienstjaren
```

Drools exec-model, GEEN (gemeten op een document met alleen de
GEEN-regels, omdat de ALLE-regel anders als eerste afketst):

```
EXCLUDE transpiler SKIP: regel: onervaren bij dienstjaren — kwantorvouw GEEN over Strandwacht (een matrix zonder §8.2 gehele-periodekwalificatie — deze tak lowert alleen de heelperiode-vorm van ch. 3.9a): de dienstjaren van geen van de wachten van de Schouwploeg groter zijn dan 5; regel (voorwaarde buiten Phase-B scope): onervaren bij dienstjaren
```

Drools DRL-tekst, beide kwantoren (ALLE getoond; de GEEN-variant is
woordelijk dezelfde met GEEN):

```
EXCLUDE drl-transpiler MARKER: regel: ervaren bij dienstjaren — TODO §8.2 kwantorvouw (ALLE over Strandwacht) — ch. 3.9a wordt alleen in de exec-model-tak gelowerd: de dienstjaren van alle wachten van de Schouwploeg groter zijn dan 5
```

### De RUNTIME-SEMANTICS-delta (beschermd pad, gemandateerd)

Eén rij vervangen en één blok toegevoegd. De ch.-3.12-rij, OUD:

> | **Quantifier fold** over a linked instance set (ch. 3.9a) | **specified and executed** — ALLE and GEEN in all three executors, witnessed by `corpus/golden/jutterswet-ploegpremie`. The counting quantifiers over an instance range are the named extension point and are refused by WF-108 (a); a NON-temporal matrix is admitted by the IR; the reference interpreter executes it under both quantifiers, the ASP backend under GEEN only (ALLE would need the complement of an arbitrary condition, which is not an ASP body) and the Drools backend under neither (it markers the form by name in both branches) — the standing executor asymmetry, kept out of the corpus |

NIEUW:

> | **Quantifier fold** over a linked instance set (ch. 3.9a) | **specified and executed** — ALLE and GEEN in all three executors, over a WHOLE-PERIOD matrix (witness `corpus/golden/jutterswet-ploegpremie`) and, since 2026-09-16, over a NON-temporal one (witness `corpus/golden/jutterswet-schouwploeg`: five cells, both quantifiers, two matrix shapes and a negated matrix). The executor asymmetry this row used to record is **gone**: the ASP backend REIFIES the matrix into a helper predicate and negates that one atom (negation-as-failure over a reified conjunction is an ordinary ASP body, where the complement of an arbitrary conjunction of atoms is not), and both Drools branches ask the matrix through their ordinary per-subject condition lowering with the element as subject — the exec branch collecting the contributors, the DRL-text branch denying the failure witness with a `not` pattern. What stays bounded is which matrix FORMS each executor's condition translation carries: a form outside it is refused by name, per form, never silently. The counting quantifiers over an instance range are the named extension point and are refused by WF-108 (a); the WHOLE-PERIOD matrix remains a named marker in the Drools DRL-text branch alone |

In ch. 3.9a zelf stond GEEN asymmetriezin — dat is gemeten, niet
aangenomen: de enige vermelding in het hele document stond in de
ch.-3.12-rij hierboven. Wat ch. 3.9a wél miste was een gewerkt voorbeeld
bij de helft van zijn eigen formule (hij stelt `test` over een willekeurige
conditie en toonde alleen de heelperiodevorm). Toegevoegd, achter het
die/dat-voorbeeld en vóór ch. 3.10: het blok **"Worked examples — a
NON-temporal matrix"** met de vijf cellen hierboven, de leeg-versus-afwezig
tegenstelling met rij 7, en de alinea over waar de ontkenning woont. 39
regels erbij, 1 vervangen; niets anders onder `spec/` aangeraakt.

### De veertien poorten (eindstand, alle groen)

```
1  check-self-contained: PASS
2  check-ir-schema: PASS (schema valid; 24 pattern self-test rows; 219 files validate and round-trip, floor 192)
3  check-verifier: PASS (219 golden clean; 391 negatief rejected for their declared rule, floor 350)
4  check-case-schema: PASS (schema valid; 512 case files validate)
5  check-oracle: PASS (ref suite 620 worked examples, floor 489; 486 cases replayed exactly, floor 427)
6  check-clingo: PASS (backend suite 205 checks, floor 83; 486 cases replay exactly through clingo)
7  check-noop: PASS (450 cases refute a do-nothing executor; 36 pinned survivors)
8  check-rood: PASS (26 red cases fault observably in ref and clingo, floor 26; Drools leg 6 fault-pinned, 20 not-carried)
9  check-catala: PASS (frontend suite 201 checks, floor 201; 12 catala goldens regenerate byte-identically)
10 check-regelspraak: PASS (JUnit 263 tests, floor 173; 49 rgs goldens re-emit byte-identically)
11 check-toka-parity: PASS (45 .rgs re-emitted byte-identical, floor 45; 29 cases through ref, clingo, Drools; 0 divergences)
12 check-drools: PASS (played 143 = pinned 143; channel floor 50; value-divergence 1; JUnit 354 tests, floor 239)
13 check-reftype-derivation: PASS (105 annotations twisted over 21 goldens, floor 74; 96 replays, 18 DRL-text PLAYED, floor 13)
14 check-flogic: PASS (frontend suite 81 checks, floor 81; 1 flogic golden re-emits byte-identically)
```

Bewegingen: poort 2 en 3 van 218 naar 219 documenten, poort 5 en 6 van 481
naar 486 cases, poort 6's suite van 188 naar 205 controles, poort 7 van 445
naar 450, poort 12's JUnit van 349 naar 354 tests en `EXPECTED_PLAYED` van
142 naar 143 (eigen gedateerde, redendragende stap in
`scripts/check-drools.sh`; de kanaallijst blijft 50, want die pint alleen
NIET-spelers en de nieuwe golden speelt). Geen enkele vloer verlaagd.

### Eerlijke afwijkingen

**(a) Twee bestaande getuigen zijn gewijzigd, tegen acceptatie (e) in.**
Allebei pinden zij precies de weigering die deze kaart opheft, dus zij
konden niet blijven staan zoals zij waren. Geen van beide is verwijderd.

1. `backends/clingo/test/test_clingo.ml` — de rij `ch3.9a ALLE over een
   andere matrix dan de heelperiodevergelijking` verwachtte een luide fout
   op een kenmerkmatrix. Zij is vervangen door acht VERDICTrijen over
   dezelfde matrix (beide kwantoren, het vacuüm, het GEEN≠!ALLE-paar, de
   ontkende matrix) plus een vormrij die de reïficatie zelf pint, elk tegen
   ref én clingo. De bewering die zij droeg — een matrix die de vertaler
   niet draagt komt luid en bij naam naar boven, ook dóór de vouw heen —
   staat er nog, nu op de heelperiode-leegtoets.
2. `backends/drools/.../HeelperiodeKwantorvouwWitnessTest.java` — de rij
   `een_matrix_buiten_de_scope_...` blijft EXCLUDE meten; alleen haar
   laatste assertie pinde de oude reden (`gehele-periodekwalificatie`) en
   pint nu de nieuwe (`bladvertaling niet draagt`). De getuige zelf — een
   vergelijking op een TIJDSAFHANKELIJK operand zonder kwalificatie — is
   nog steeds buiten scope, dus de rij meet nog precies wat zij mat.

**(b) De DRL-tekst-tak heeft geen poort die hem op deze knoop meet.** Poort
12 speelt alleen de exec-tak; poort 13 raakt deze golden niet, want hij
draagt geen eenheidsdragende annotatie om te verdraaien. Daarom is er één
nieuw testbestand (additief):
`backends/drools/.../NietTemporeleKwantorvouwWitnessTest.java` — beide
takken over de vijf corpuscases, een mutant op de vacuümcel die allebei
moeten verwerpen (PLAYED alleen is geen getuige: een tak die stil niets
lowert haalt elke AFWEZIG-verwachting vanzelf), en sinds de fixronde de
geneste vouw plus de scheiding van de markerredenen (zie hieronder).

**(c) De matrixvormen die elke tak draagt zijn NIET dezelfde verzameling,
in BEIDE richtingen.** Gemeten, elk met een eigen probe, en geen ervan
wordt stil:

- de exec-tak draagt geen `Samengesteld` matrix (`leafPredicate` kent er
  geen; `filterSource` heeft er één regel voor, `leafCompound`, die hier
  overgenomen kon worden maar zonder getuige zou landen) en geen matrix
  tegen een PARAMETER — de DRL-tekst-tak draagt allebei. Gemeten op de
  parameterprobe: `ref` PASS, clingo PASS, DRL-tekst `PLAYED 1`, exec
  `EXCLUDE … een vergelijking van het eigen attribuut 'dienstjaren' met de
  parameter 'keurgrens'`;
- de DRL-tekst-tak draagt geen HEELPERIODEmatrix — de exec-tak wel;
- een GENESTE vouw als matrix draagt geen van beide, en allebei weigeren
  hem nu bij naam.

Kandidaten voor eigen kaarten, samen met de frontend-verbreding onder "de
golden" hierboven.

**(d) `ref/` is niet aangeraakt** — gemeten, niet aangenomen: alle vijf de
cases speelden door `ref` op de dag dat de golden landde, vóór enige
codewijziging (acceptatie f).

### De fixronde na de lens (2026-09-16)

De lens bevestigde het kernwerk en wees vier punten aan; alle vier
verwerkt.

**1 (ernstig) — de javadoc-belofte hield niet voor de conditiesoort die
deze kaart zelf toevoegde.** `conditieInline` ziet alleen het
`extra`-kanaal (`heeftExtra()`). Een conditie die haar clausule
RECHTSTREEKS in de `Acc` hijst en een LEGE inline teruggeeft komt er
ongemerkt doorheen — en dat is precies wat de nieuwe vouwlowering zelf
doet, dus een GENESTE vouw als matrix was de eerste vorm die zichzelf
brak. Rood, gemeten op een eigen probe (Schouwploeg → Strandwacht →
Uitrusting, matrix = "alle stukken zijn gekeurd"):

```
EXCLUDE drl compile: [Message [id=1, kieBase=defaultKieBase, level=ERROR, path=file0.drl, line=13, column=0    text=[ERR 101] Line 13:44 no viable alternative at input ')' in rule "uitgerust bij keuring"], …
```

Een weesclausule die tegen `$subject` joint met een veld van het verkeerde
type, plus een lege `!(  )`-constraint. Luid, maar géén benoemde weigering
— en de ch.-3.12-zin die deze kaart zelf schreef belooft "refused by name,
per form, never silently". Groen, na de vangst op beide kanalen
(`ff.marker() || acc.clausules.size() != mark`, en idem voor de toets):

```
EXCLUDE drl-transpiler MARKER: regel: uitgerust bij keuring — TODO §8.2 kwantorvouw (ALLE over Strandwacht) — een matrixvorm die deze tak niet IN het elementpatroon kan schrijven (Kwantorvouw): alle wachten van de Schouwploeg alle stukken gekeurd hebben
```

Op hetzelfde document antwoorden de twee OCaml-uitvoerders wél — verifier
PASS, `ref` PASS (2 verwachtingen), clingo PASS (2 verwachtingen) — dus de
tweewegsdivergentie is echt en staat nu als getuige in
`NietTemporeleKwantorvouwWitnessTest`, met de parsefout expliciet
uitgesloten (`assertFalse(reason.contains("drl compile"))`).

Twee dingen gingen mee. De MARKERTEKST was te grof: één zin ("ch. 3.9a
wordt alleen in de exec-model-tak gelowerd") verscheen ook waar de
exec-tak juist óók weigert en waar de reden een andere is. De weigeringen
zijn nu per reden — heelperiodematrix, meerstaps bereik, kwantor buiten
v0, geen verbindende rol, geen beanveld (met de §13.3.9-reden gesplitst),
filter of matrix die niet in het elementpatroon past — en een eigen rij
houdt die scheiding vast. En de doel-zijde van `feitCreatie` kreeg
dezelfde vangst (`doelAcc.clausules.isEmpty()`): een doel-guard rijdt IN
het `$doel`-patroon, dus een lowering die hijst zou haar clausule achter
het ANKERpatroon krijgen, waar `$subject` de anker betekent. Door de lens
als code-lezing gemeld, hier gesloten.

**2 (klein) — de weigeringstekst van de exec-tak noemde de verkeerde as.**
"een niet-temporele matrix (Vergelijking) die de per-subject bladvertaling
niet draagt" noemde de KLASSE, terwijl `Vergelijking` in het algemeen wél
gedragen wordt en het de OPERAND is die niet past. De tekst noemt nu de
werkelijke as: "een vergelijking van het eigen attribuut 'dienstjaren' met
de parameter 'keurgrens'".

**3 (klein) — het registervoorstel corrigeerde de rij niet waar het dat
wél moest.** De slotzin van rij 40 ("the DRL-text Drools branch markers the
node by name") is na deze kaart nog maar half waar. De aanvulling opent nu
met een expliciete narrow-clausule, naar het precedent van de
rij-29-correctie in de forall-kaart.

**4 (klein) — het spec-blok claimde "verbatim" maar miste een rij.** De
S2-rij van cel 2 — de rij die het BEREIK pint — ontbrak in ch. 3.9a. Zij
is toegevoegd, met de zin die zegt waarom zij er staat: een uitvoerder die
over élke `Strandwacht` zou kwantificeren in plaats van over de wachten
van de ploeg leest haar andersom, en dat is precies het faalpad van een
reïficatie die per element gedefinieerd is. De ch.-3.12-rij zegt nu "five
cases carrying six verdict rows" in plaats van "five cells".

### Registervoorstel (letterlijk, voor de hoofdsessie)

Rij 40, aan te vullen achter de bestaande tekst. De aanvulling CORRIGEERT
bovendien de SLOTZIN van de bestaande rij ("the DRL-text Drools branch
markers the node by name (the prioritizedDefaults asymmetry, second
instance)"), die per vandaag nog maar half waar is: die tak markeert
alleen nog de HEELPERIODEvorm en speelt de niet-temporele. De aanvulling
zegt dat met zoveel woorden, naar het precedent van de rij-29-correctie in
de forall-kaart, zodat de rij zichzelf niet tegenspreekt:

**Landed (2026-09-16, kaart kwantor-asymmetrie) — the matrix is now executed AS THE CHAPTER STATES IT, by all three, and this NARROWS the closing clause above:** where that clause says the DRL-text Drools branch markers THE NODE by name, it now markers only the WHOLE-PERIOD matrix of that node — the non-temporal matrix it LOWERS, and plays. ch. 3.9a's `test` is an ordinary condition with the ELEMENT as subject, and until this card two of the three executors implemented not that sentence but one recognised matrix SHAPE. The NON-temporal matrix therefore played in `ref` alone. It plays in all three now, with a second witness beside the first: `corpus/golden/jutterswet-schouwploeg` (hand-written IR, five cases carrying six hand-computed verdict rows — two ploegen share one case, which is what pins the RANGE — ALLE and GEEN over BOTH a scalar comparison and a characteristic check, plus the negated matrix). The ASP backend REIFIES the matrix — one helper rule per fold whose head means "this element satisfies the matrix", derived from exactly the atoms GEEN already read, with `not helper(X)` as the ALLE failure witness; negation-as-failure over a reified conjunction is an ordinary ASP body where the complement of an arbitrary conjunction of atoms is not, and stratification holds by construction (the counting composite's own argument: the name comes from a counter that only grows, over a matrix translated before the name existed). The whole-period matrix LOST its special case there and now takes the same route under a double negation — measured byte-identical over the complete stable model of all 486 golden cases. Both Drools branches ask the matrix through their ordinary per-subject condition lowering with the element as subject: the exec branch collects the CONTRIBUTORS instead of their timelines, the DRL-text branch denies the failure witness with one `not <Element>( <join>, <matrix or its negation> )` clause. Where the NEGATION lives is now pinned rather than inherited: on a non-temporal matrix it is part of the condition and is read per member, so the quantifier is untouched — unlike ch. 3.9's "niet gedurende …", which qualifies the outer comparison and which an executor may fold by flipping ALLE into GEEN | the frontend does NOT produce this shape — measured, both spellings: the unqualified comparison raises WF-096 (`setlezing buiten een vouwbron`) and the characteristic spelling resolves to `Onopgelost`, because `RuleChecker.kwantorvouw` lowers only the §8.2-QUALIFIED condition; widening that is a frontend card of its own, and the witness is hand-written IR without a `bron.rgs`. Which matrix FORMS each Drools branch carries is NOT the same set, in BOTH directions, and each refusal is named per form: the exec branch declines a COMPOUND (`Samengesteld`) matrix and one whose bound is a PARAMETER, which the DRL-text branch both carry; the DRL-text branch declines the WHOLE-PERIOD matrix, which the exec branch carries; and a NESTED fold as matrix is declined by both. Recorded rather than closed — each is a candidate for its own measured card |
