# clingo-parameter-failfast: ongebonden parameters zijn stil leeg

Gemeten door de clingo-leeg-negatie-lens (2026-09-23, proeven
E2/E3, bestond al): een ONgebonden parameter is in clingo stil leeg
waar ref fail-fast werpt ("parameter … is unbound in this case
(binding is fail-fast, ch. 5.7)") — bij een GEVULD-test en bij een
gewone waardelezing. De leeg-negatie-kaart dichtte de LEEG-testflank
in haar eigen arm (en `dk_value` weigerde al langer); de overige
leesposities antwoorden nog. Zolang elke corpuscase alle parameters
bindt is er geen corpusdrager, maar de klasse is dezelfde als de
gedichte: een stellig antwoord waar de arbiter faalt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Inventariseer de parameterleesposities in translate.ml; meet
  per positie ref versus clingo op een ongebonden parameter.
- (b) Eén gedeelde fail-fast in refs woorden (het dk_value/leeg-arm-
  precedent veralgemeend), rood-eerst per positie.
- (c) Corpussweep byte-identiek; alle veertien gates groen.

May touch: `backends/clingo/**`, kaartverplaatsing. NB: serieel met
andere translate.ml-kaarten.

## Aangescherpt bij de claim (2026-09-23, basis 8dd05e25)

Eerst gemeten, daarna aangescherpt. Van elke golden-case zijn varianten
gemaakt met telkens één gelezen scalaire parameter weggelaten: 88
varianten. ref faalt op 63 daarvan en speelt de overige 25, omdat hij de
lezing daar nooit bereikt (een niet-gekozen tak, een lege lijst, een
Let-behoefte die eerst leeg blijkt, een conditie die faalt, een
verdrongen default). Een weigering in de VERTALING, zoals de leeg-arm en
`dk_value` die hadden, zou alle 25 afwijzen, waar ch. 5.11 zegt dat een
operand die nooit bereikt wordt niet faalt. De aanscherping is daarom:

- (b) De gedeelde fail-fast is één viol/2-garde, dezelfde soort als elke
  andere fail-fast van deze vertaler. Hij vuurt waar de evaluatie de
  lezing bereikt, en nergens anders. De zin is die van ref letterlijk:
  `parameter "…" is unbound in this case (binding is fail-fast, ch. 5.7)`.
  De fase staat in het rapport. ref meldt `EVALUATION FAULT:`, clingo
  `UNSATISFIABLE — a fail-fast guard fired: viol("…","J1")`, en de twee
  plekken waar de vertaler zelf in de vertaling leest (de dagsoorttabel
  en het horizonhek) melden `TRANSLATION FAULT:` met dezelfde zin. Het is
  een benoemde arm met de parameternaam erin, dus geen bodemzin.
- Meetlat: op alle 88 varianten moeten ref en clingo overeenstemmen
  (faalt ref, dan faalt clingo, en speelt ref, dan speelt clingo), en de
  corpusvertaling moet byte-identiek blijven.

## Result (2026-09-23)

**Gedragen.** Elke positie waar de ASP-vertaling een scalaire parameter
leest, draagt één viol/2-garde in refs woorden, die vuurt waar de
evaluatie de lezing bereikt. Waar dat niet exact is, wijkt de garde af in
één van twee vastgelegde richtingen (zie "Twee richtingen" hieronder):
luid, een weigering waar ref speelt, op drie plaatsen, en stil, een
antwoord waar ref faalt, op één: de Let-verlaging, die al zonder
parameter afwijkt. Waar leden die een creatie aan het bereik van een
kwantorvouw toevoegt beslissen of de matrix de parameter leest, weigert
clingo in een zin die dat benoemt (maintainerbesluit 2026-09-23). De
eerste versie van dit Result schreef "speelt waar ref speelt" zonder
uitzondering en de tweede "faalt waar ref faalt" zonder uitzondering. De
lens weerlegde beide; de drie fixrondes hieronder leggen vast wat er
gerepareerd is en wat niet. Gemeten op 88 corpusvarianten (88/88 in
overeenstemming), op 66 proefdocumenten van de eerste ronde, 29 van de
tweede fixronde en 54 van de derde, en in de suite (841 checks). De
corpusvertaling is byte-identiek aan main, op de twee rode cases van
deze kaart na. De Drools-lijstregel voor gate 8 staat als voorstel in
commit `63f9637d` (beschermd pad); daarmee is gate 8 groen (50 rode
cases).

Commits (hashes na de rebase op main `7d97353e`): `fff8c963` (claim),
`607fd90d` (de gedeelde fail-fast met suite, README en de gedeelde
diagnose), `4e70d107` (de dode datumlezingen, item van de hoofdsessie),
`b4b21f10` (rode corpusdrager), `e25e46af` (kaart naar done),
`e8b12f93` (venstercommentaar, tweede item van de hoofdsessie),
`d343fc29` (Result), `63f9637d` (gate-8-regel, voorstel beschermd pad),
fixronde 1: `ddbe3b7c`, `3c197a1f`, `08585e3e`, fixronde 2: `bfecfb28`,
`409398c5`, `c5b1c7fc`, `92c633b3`, `2d739d9e` (Result), fixronde 3:
`7b009ae3` (schaduwing), `86f3bf1d` (gesloten lichaam), `82f78afe`
(ledenvolgorde: barrière per lid en creatievolgorde), `742cb3f3`
(teksten), en de commit van dit Result.

### Twee richtingen (stand na fixronde 3)

Waar de garde niet exact is, wijkt ze in één richting af. Elke plaats
heeft een getuige, gemeten met ref en clingo op main `7d97353e`, op de
tip vóór deze ronde (`2d739d9e`) en op de tip na deze ronde.

Luid, een weigering waar ref speelt:
1. **De dagsoorttabel en het horizonhek** lezen hun parameter in de
   vertaling en weigeren daar, ook achter een regelconditie die faalt
   (open punt 4). Getuige hf-cond, het horizonhekframe van de suite met
   voorwaarde 1 > 2: ref `PASS` (keurlast leeg), clingo `TRANSLATION
   FAULT: parameter "minimumgewicht" is unbound …` (main weigerde daar
   al, met de hekzin).
2. **De benoemde creatievolgorde-weigering** neemt het kleinste
   voorvoegsel dat ref kan hebben waar het programma refs volgorde van de
   toegevoegde leden niet kent. Het kent die alleen voor onvoorwaardelijke
   creaties met literal-toekenningen, zonder recursie en zonder
   feitcreaties. Getuige cr-8-3-cond, de twee creaties van cr-8-3 met een
   voorwaarde die geldt (nul = 0): ref `PASS` (leest 8 eerst en stopt),
   clingo UNSAT met de benoemde zin.
3. **Een Let-barrière in een deel per lid** stopt de garde alleen voor
   een lid waarvan het programma de volgorde kent. Getuige
   pm-cr-8-3-cond: `Let k = restpost in som(lengte van schuiten die TEN
   MINSTE 1 [ALLE[lengte > 5, k > 0], lengte > p])`, W1 zonder schuiten
   in de case, dezelfde twee creaties met voorwaarde: ref `PASS` (de
   barrière bij 8 beëindigt de Let), clingo UNSAT in refs woorden. Waar
   de volgorde wel bekend is, vuurt de garde precies waar ref faalt, maar
   ook dan in refs woorden en niet in de benoemde zin (pm-cr-3-8: ref
   faalt, clingo UNSAT in refs woorden; open punt 8).

Stil, een antwoord waar ref faalt:
1. **De Let-verlaging** houdt een waarde achter een omsluitende behoefte
   op een pad dat haar niet leest, en de garde volgt de verlaging (open
   punt 5). Die verlaging wijkt al zonder parameter af: lz-m3-bound-laag
   (`Let k = restpost in ((match klasse: Hoog → k, Laag → 0,00) plus
   p)`, Laag, p = 10) en lz-v-bound geven ref 10,00 en op main, op
   `2d739d9e` en op de tip `MISMATCH: J1.heffing is underived (leeg)`.
   Met p ongebonden (lz-v-ctl) faalt ref en speelt clingo.

### (a) Inventaris

Elke match op de parameterconstructor in `translate.ml`
(regelnummers van de tip na fixronde 3):

| r. | functie | aard | stand na de kaart |
|----|---------|------|-------------------|
| 517 | `eff_unit` | statisch: eenheid uit de declaratie | geen waardelezing |
| 827/870 | `walk_order` / `cond_param_names` | statisch: evaluatievolgorde van een Let-lichaam (sinds fixronde 2; `params_before_binder` is weg) | geen waardelezing |
| 1145/1173 | `expr_reaches` / `demand_for` | statisch: kan een Let-waarde een ongebonden parameter lezen | geen waardelezing |
| 2604 | `closed_let_chain` | statisch: Let-keten boven een Match in een gesloten lichaam (fixronde 3) | geen waardelezing |
| 1747/1785 | `provably_unitless` / `provably_pure_pct` | statisch: eenheidsbewijs | geen waardelezing |
| **2641** | `expr_term` | DE scalaire lezing (`param/2`) | **draagt de gedeelde garde** |
| 3192 | `expr_term` (`filter_reads` van het horizonhek) | statisch: vormtoets | geen waardelezing |
| 4014 | `expr_term` (TemporalRead) | granulaire parameter, punt- of vintagelezing | faalde al in refs woorden (`needs its timeline in the case`); ongewijzigd |
| 5229 | `whole_period_fails` | vormtoets op de grens | leest via `expr_term` (garde), nu met het onderwerp van de aanroeper |
| **5385** | `dk_value` | dagsoorttabel, geëvalueerd in de vertaling | vertaalfout, nu met de gedeelde zin |
| 6988 | `list_source` | lijstparameter (`plist`) | afwezig is leeg, in beide uitvoerders; vrijgesteld |
| **7278** | `closed_term` | scalaire lezing in een gesloten body | **draagt de gedeelde garde** |
| 7700 | `value_condition` | statisch: WF-077-vormtoets | geen waardelezing |

Daarnaast kunnen sommige posities een parameter **ongeacht de binding**
niet dragen. Ze weigeren met een eigen of met een bodemzin, dus luid en
niet stil. Het gaat om een tekst-, enum-, boolean-, datum-tijd-, som- of
structparameter (`symbolic_term`, `dt_term`, `sum_term`, `struct_base`,
`scrutinee_pattern`), een Construct-payload, een Begrensd-grens die geen
literal is, en een runtime-exponent. Het horizonhek (§7.1) leest zijn
die/dat-filter in de vertaling. Die lezing werd een 14e positie (zie
bijvangst).

Meting per positie. ref is de arbiter. "vóór" is het antwoord van de
basisvertaler op de ongebonden case, "ná" dat van de branchtip. Elke
rij heeft ook een gebonden tweeling die in beide uitvoerders speelt.

| positie | proef | ref | clingo vóór | clingo ná |
|---------|-------|-----|-------------|-----------|
| waarde: kale lezing | V01 | fout | heffing leeg | garde |
| rekenkunde: plus | V02 | fout | heffing 20,00 (0 gecoalesceerd) | garde |
| rekenkunde: min (rechts) | V03 | fout | heffing 20,00 | garde |
| rekenkunde: verminderd met (links) | V04 | fout | leeg | garde |
| rekenkunde: maal | V05 | fout | 0,00 | garde |
| rekenkunde: percentage van | V06 | fout | 0,00 | garde |
| rekenkunde: deler | V07 | fout | leeg plus een §6.5-melding | garde |
| rekenkunde: deeltal | V08 | fout | 0,00 | garde |
| begrensd: minimum | V09 | fout | weigert de vorm (bestond al) | idem, ongewijzigd |
| afgerond | V10 | fout | leeg | garde |
| Let-waarde | V11 | fout | leeg | garde |
| IfThenElse: conditie | V12 | fout | leeg | garde |
| IfThenElse: gekozen tak | V13 | fout | leeg | garde |
| tot de macht | V14 | fout | leeg | garde |
| conditie: ordening | C01 | fout | leeg | garde |
| conditie: ongelijk | C02 | fout | **5 (waar)** | garde |
| conditie: gelijk | C03 | fout | leeg | garde |
| conditie: ALLE, 2e lid | C04 | fout | leeg | garde |
| conditie: GEEN | C05 | fout | **5 (waar)** | garde |
| conditie: TEN MINSTE | C06 | fout | leeg | garde |
| conditie: GEVULD / LEEG | C07/C08 | fout | vertaalfout (leeg-arm) | garde |
| assertie (consistentie) | C09 | fout | **inconsistent** | garde |
| vouw: die/dat-filter | F01 | fout | **som 0** | garde |
| kwantorvouw: matrix (heel tijdvak) | F02 | fout | kenmerk afwezig | garde |
| gesloten body: lambda | suite | fout | leeg | garde |
| gesloten body: init | suite | fout | leeg | garde |
| gesloten body: MapList | suite | fout | **0,00** | garde |
| temporeel: datum plus duur | T01 | fout | **dezelfde datum** | garde |
| temporeel: tijdsduur | T02 | fout | leeg | garde |
| temporeel: heel tijdvak, NIET | suite | fout | **5 (waar)** | garde |
| temporeel: gedurende-clip | CL01 | fout | **topsom 0** | garde |
| dagsoorttabel | bestaande pin | fout | vertaalfout | idem, gedeelde zin |
| default: gevolg | D01 | fout | leeg | garde |
| default: rechtvaardiging | D03 | fout | **25 (init besliste)** | garde |
| verdeling: voorwaarde | VD01 | fout | geen aandelen | garde |
| objectcreatie: toekenning | OC01 | fout | rapport zonder bedrag | garde |
| objectcreatie: voorwaarde | OC02 | fout | geen rapport | garde |
| Let: parameter vóór de binder | A01/A02/A03 | fout | leeg | garde |
| Let: gemengde tak, then | A05a | fout | 0,00 | garde |
| horizonhek, gebonden | H01 | speelt (36) | **vertaalfout** | speelt (36) |
| horizonhek, ongebonden | suite | fout | vertaalfout (hekzin) | vertaalfout (gedeelde zin) |

Lui (ref leest de parameter nooit, beide spelen, vóór en ná): de
niet-gekozen tak (L01), een falende conditie (L02), een ALLE-prefix die
faalt (L03), een GEEN-kortsluiting (L04), een Let-barrière (L05), een
ongelezen binder (L06), geen onderwerp (L07), een beslist TEN MINSTE
(L08), een lege set (L10), een binder vóór de parameter (A04), de
gemengde tak op else (A05b), een Let-waarde onder een operator die
alleen de niet-gekozen tak leest (A07), een Let in een conditieoperand
(R1), een verdrongen default (D02/D04), leden buiten elk bereik, een
gesloten body die nooit draait of die geen onderwerp bereikt. Twee
proeven wijken af en zijn geen parameterklasse: A06 (deling door nul
vóór een lege binder, zie open punt 3) en V09 (Begrensd, weigert de
vorm ongeacht de binding).

Corpusvarianten (88): vóór de kaart faalde ref op 63, en clingo
antwoordde op 61 daarvan (58 MISMATCH, 3 PASS met een stil antwoord) en
faalde op 2 (dagsoort). Ná de kaart: 61 keer UNSAT met de garde, 2 keer
een vertaalfout in de dagsoorttabel, en 25 keer PASS waar ref speelt,
met hetzelfde aantal verwachtingen.

### (b) De gedeelde fail-fast

- `param_unbound` beslist zoals ref: een gedeclareerde, niet-granulaire
  parameter die de case niet bindt. Een lijstparameter is vrijgesteld
  (ref leest hem leeg), een granulaire heeft zijn eigen weigeringen.
- `unbound_param_reason` is de ene zin. `unbound_param_guard` emitteert
  alleen voor een ongebonden parameter
  `viol(<zin>, <onderwerp>) :- <bereik>`. Een gebonden lezing emitteert
  niets.
- Het bereik (`rule_reach`) bestaat uit het onderwerp plus `rule_guard`
  (conditie, compositieprefix, gekozen tak of matchpatroon,
  Let-behoeften) plus `param_reach`. Dat laatste is een nieuw ctx-veld
  voor wat `rule_guard` niet draagt. Bij een vouwlid of kwantorelement
  is dat het onderwerp met de bereik-join (`with_member_reach`, op negen
  plekken). Bij een gesloten body (vouwbody, init, MapList) is het de
  onderwerpcontext van de vouw. Bij een default zijn het de verdringing
  (`not stl_m`) en de rechtvaardiging, lui opgevraagd, zodat een
  gebonden document in de oude volgorde vertaalt. `rule_guard` zelf is
  onaangeraakt: wie hem breder zet, verschuift de hekken van 60
  corpusprogramma's (gemeten en teruggedraaid).
- Let-behoeften: ch. 10.6 zet de barrière op de EERSTE lezing van de
  binder. In deze eerste ronde bepaalde `params_before_binder` per Let
  welke parameters vóór die lezing kunnen vallen, en voor die parameters
  viel de behoefte uit hun garde. Dat stond hier als "conservatief naar
  de luide kant". Dat klopte niet: de analyse was ook stil (open punt 5
  en fixronde 2, punt 4). Sinds fixronde 2 beslist de garde de barrière
  per lezing.
- Het heel-tijdvakhek leest zijn grens nu met het onderwerp van de
  aanroeper. Met het eigen `S` van het hulpprogramma botste de garde in
  een kwantormatrix met het onderwerp van de regel en vuurde hij nooit
  (M7).
- De leeg-arm verliest zijn eigen walker (`unbound_param_in`). De lezing
  in zijn operand draagt de gedeelde garde en weigert dus alleen waar
  de toets bereikt wordt. `dk_value` houdt zijn beslissing en leent de
  zin.
- De UNSAT-diagnose verhuist van de runner naar `Solve.diagnose`, zodat
  suite en runner dezelfde getuigen lezen. Hij maakt gringo's escapes
  ongedaan, zodat het rapport refs zin met de naam letterlijk draagt.
  Een getuige zonder escape (alle bestaande) blijft byte voor byte
  gelijk, en de sweep bevestigt dat.

**Suite** (`test_clingo.ml`): 523 → 699 checks. Elk frame is een paar:
de gebonden tweeling speelt in ref en clingo op handberekende rijen, en
de ongebonden tweeling laat ref falen en clingo's garde vuren. De rijen
van die tweede pinnen het stille antwoord van vóór de kaart, zodat een
rode run het stellige antwoord noemt. Daarbij komen luie frames,
horizonhekframes en de datumliteral-pin. **Gemeld mandaat** voor
bestaande tests: de twee leeg-arm-pins `LEEG (negated) on an unbound
parameter` gaan van `faults_with` (vertaalfout) naar `unbound_fires`
(garde plus ref-fout). Naald en naam zijn hetzelfde gebleven, alleen de
fase verschoof.

Rood eerst: de basisvertaler (8dd05e25) met de eindsuite, daarna
teruggezet met `cmp` identiek:
```
FAIL LEEG on an unbound parameter: clingo fires the shared fail-fast (translation fault: parameter "drempel" is unbound in this case)
FAIL LEEG negated on an unbound parameter: clingo fires the shared fail-fast (translation fault: parameter "drempel" is unbound in this case)
FAIL value: a bare read, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg)
FAIL value: plus coalesces the absence to 0, unbound: clingo fires the shared fail-fast (clingo answered heffing 20,00: the absent parameter coalesced to 0)
FAIL value: maal coalesces both operands, unbound: clingo fires the shared fail-fast (clingo answered heffing 0,00)
FAIL value: the divisor, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg with a ch. 6.5 channel fact: a leeg divisor under a filled dividend)
FAIL value: the dividend, unbound: clingo fires the shared fail-fast (clingo answered heffing 0,00: a leeg dividend)
FAIL value: a Let value, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg)
FAIL value: an IfThenElse condition, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg)
FAIL value: the taken IfThenElse arm, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg)
FAIL condition: an ordering, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg)
FAIL condition: a numeric ongelijk, unbound: clingo fires the shared fail-fast (clingo answered heffing 5,00: exactly one empty operand makes ongelijk waar (Tabel 44))
FAIL condition: the second member of ALLE, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg)
FAIL condition: GEEN, unbound: clingo fires the shared fail-fast (clingo answered heffing 5,00: no member held, so GEEN did)
FAIL condition: the first member of TEN MINSTE 1, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg)
FAIL condition: the assertion of a consistency check, unbound: clingo fires the shared fail-fast (clingo answered inconsistent: the assertion's join failed, so it read as violated)
FAIL Let: a parameter read before the binder, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg: the guard waited for the need)
FAIL Let: a parameter in the condition before the binder, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg: the guard waited for the need)
FAIL Let: the taken arm leaves the binder unread, unbound: clingo fires the shared fail-fast (clingo answered heffing 0,00: p coalesced on the arm that never reads k)
FAIL member: a set fold's filter, unbound: clingo fires the shared fail-fast (clingo answered bonus 0 on both werven: no member passed the filter)
FAIL member: a quantifier fold's matrix, unbound: clingo fires the shared fail-fast (clingo answered keur 99 on both werven: no member satisfied the matrix, so GEEN held)
FAIL closed body: the lambda of a fold over a list parameter, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg: the recursion stopped at the first element)
FAIL closed body: the fold's init, unbound: clingo fires the shared fail-fast (clingo answered heffing leeg)
FAIL closed body: a MapList body, unbound: clingo fires the shared fail-fast (clingo answered heffing 0,00: the mapped list kept its presence and lost its elements, so the fold summed nothing)
FAIL temporal: date plus a duration parameter, unbound: clingo fires the shared fail-fast (clingo answered vervaldatum 2026-01-10: the absent duration coalesced to 0 days)
FAIL temporal: tijdsduur from a date parameter, unbound: clingo fires the shared fail-fast (clingo answered looptijd leeg)
FAIL temporal: a whole-period bound in a quantifier matrix, unbound: clingo fires the shared fail-fast (clingo answered keur leeg: the member's whole-period test failed, so ALLE did)
FAIL temporal: a negated whole-period bound, unbound: clingo fires the shared fail-fast (clingo answered toeslag 5: no day satisfied, so the NEGATED whole-period test held)
FAIL temporal: the gedurende clip's bound, unbound: clingo fires the shared fail-fast (clingo answered topsom 0: no month passed the clip)
FAIL defaults: the deciding default's consequence, unbound: clingo fires the shared fail-fast (clingo answered waardering leeg)
FAIL defaults: a justification, unbound: clingo fires the shared fail-fast (clingo answered waardering 25: the justification failed and the initialisation decided)
FAIL allocation: the voorwaarde, unbound: clingo fires the shared fail-fast (clingo answered no shares at all: the source's voorwaarde failed)
FAIL object creation: the initialisation value, unbound: clingo fires the shared fail-fast (clingo answered keur leeg: the rapport was minted without a bedrag)
FAIL object creation: the voorwaarde, unbound: clingo fires the shared fail-fast (clingo answered keur leeg: no rapport was minted)
FAIL horizon fence: a bound parameter in the die/dat filter: clingo pins the values (fault: the per-element temporal fold of "keurtarief" over "V2": the case supplies an asOfDate-bucket scalar, …)
FAIL horizon fence: an unbound parameter in the die/dat filter, in translation faults loudly (the per-element temporal fold of "keurtarief" over "V2": the case supplies an asOfDate-bucket scalar, …)
test_clingo: 699 checks, 36 FAILURES
```
(De staart `(binding is fail-fast, ch. 5.7)` is in de weergave ingekort.)
"clingo answered <…>" betekent dat het model van de basisvertaler
precies de rijen trof die het frame als stil antwoord pint. Alle
gebonden tweelingen, alle luie frames en de datumliteral-pin waren op
de basis al groen: het zijn controles en geen getuigen. Ná de kaart:
`test_clingo: 699 checks, 0 failures`.

Mutatieproeven, elk gebouwd en gedraaid op de gecommitte code, daarna
teruggezet met `cmp` identiek (`restored, cmp identical`, twaalf keer):
- M1, de garde emitteert niets: `699 checks, 34 FAILURES`. Dat zijn de
  32 ongebonden getuigen (elk "clingo answered <stil antwoord>") en de
  twee leeg-arm-pins.
- M2, lidbereik zonder bereik-join: 2 FAILURES (`member, lazy: … in no
  range`, set en kwantor).
- M3, M3b en M3c, gesloten body, init en MapList zonder
  onderwerpbereik: elk 1 FAILURE (`closed body, lazy: no subject reaches
  the fold` / `… the fold's init` / `… the MapList`).
- M4 en M4b, default-gevolg of -rechtvaardiging zonder bereik: elk 1
  FAILURE (`defaults, lazy: a higher default decides before the
  consequence` / `… the justification`).
- M5, Let-behoeften altijd in de garde: 2 FAILURES (`Let: a parameter
  read before the binder` en `… in the condition before the binder`,
  "clingo answered heffing leeg").
- M6, Let-behoeften altijd eruit: 3 FAILURES (`Let, lazy: the binder is
  read before the parameter`, `… the taken arm reads the leeg binder
  first`, `lazy: a Let need ends the Let before the parameter`).
- M7, heel-tijdvakgrens op het eigen `S` van het hulpprogramma: 1
  FAILURE (`temporal: a whole-period bound in a quantifier matrix`,
  "clingo answered keur leeg").
- M8, horizonhek zonder de parameters van de case: 1 FAILURE (gebonden
  frame, vertaalfout). M9, horizonhek zonder de gedeelde weigering: 1
  FAILURE (hekzin in plaats van refs zin).

### (c) Sweep byte-identiek

Gereedschap (tijdelijk, onder /tmp): een `clingo`-shim vooraan op PATH
kopieert elk programma dat de runner aan clingo geeft naar een map per
case (`prog.<n>.lp`, dus ook de herhaalrun van de UNSAT-diagnose) en
start dan de echte clingo. `sweep.sh` draait de runner over elke
`corpus/golden` en `corpus/rood` case en bewaart per case de programma's
plus de genormaliseerde runneruitvoer (alleen de willekeurige
tempfilenaam in clingo's info-regels is weggenormaliseerd). Twee runs
met hetzelfde binary zijn identiek. Uitslag: basisrunner (8dd05e25)
tegen de branchtip, **638 bestaande cases en 621 programma's, `diff -r`
leeg**, runnerrapporten en UNSAT-diagnoses van de rode cases inbegrepen.
De twee nieuwe rode cases komen erbij (640/625).

### Corpusdrager (rood)

`corpus/rood/aanvoergeld-parameter-ongebonden` bevat het document van
golden `jutterswet-aanvoergeld` byte voor byte, met twee cases:
`case1-tarief-ongebonden` (waardepositie) en `case2-drempel-ongebonden`
(conditiepositie). De NOTE geeft de handafleiding van de fout en het
stille antwoord van vóór de kaart. ref en clingo:
`expected fault observed (parameter "aanvoertarief"|"aanvoerdrempel" is
unbound in this case (binding is fail-fast, ch. 5.7))`. Drools: `NOFAULT
ran to a model without faulting` op beide. De Drools-replay antwoordt
wat de ASP-backend vóór de kaart antwoordde: aanvoergeld 310,00 / 87,40
/ 0,00, en aanvoerplichtig afwezig op J1 en J3. `check-rood.sh` is niet
aangeraakt, dus gate 8 is rood op precies één reden (zie gates).

### Bijvangst binnen de zone (may-touch-verbreding, geregistreerd)

Alles bleef binnen `backends/clingo/**`, één nieuwe map
`corpus/rood/<naam>/` en de kaart.
1. **Horizonhek, gebonden parameter** (§7.1, per-element temporele
   vouw). Het hek evalueerde zijn die/dat-filter met een omgeving zonder
   parameters, zodat een gebonden parameter als ongebonden gold, het lid
   bleef meetellen en het hek een document weigerde dat ref speelt.
   Gemeten H01: ref `PASS` (keurlast 36), clingo vóór `TRANSLATION
   FAULT: … asOfDate-bucket scalar …`, ná `PASS`. De omgeving draagt nu
   de parameters van de case. Een ongebonden parameter in die filter
   weigert daar in de gedeelde zin (vertaalfase, zoals de dagsoorttabel).
   Pins: twee suitechecks, M8 en M9.
2. **Dode datumlezingen** (item van de hoofdsessie). Vier lezingen gaven
   de onmogelijke arm van `Value.date_of_iso` of
   `Value.datetime_of_lexeme` een eigen weigerzin, en één liet hem stil
   vallen. Per plek gemeten op de branch (basisregels 1904, 3746, 5839,
   4764, en een vijfde van dezelfde klasse in `dt_term`, basis ~4022):
   - datumliteral `2026-13-40`: `month out of range in date lexeme …`
     komt uit de lezer, in beide uitvoerders;
   - `asOfDate 2026-02-30` via Rekendatum en via een periodeconditie:
     `CASE ERROR: asOfDate … is not the uchar-case-v0 date lexeme …`,
     in beide, bij het laden;
   - datum-tijdliteral `2026-03-15 12:00:00`, via `dt_term` en via het
     venster van de dagsoorttabel (`jutterswet-havenrooster` plus één
     regel): `malformed datum-tijd lexeme …` uit de lezer, in beide.
   Geen enkele arm bleek bereikbaar. Ze zijn vervangen door één
   lezerspaar (`day_of_lexeme`, `ms_of_lexeme`) dat de onmogelijke arm
   één keer benoemt, in refs eigen idioom `assert false`. De stille
   `| _ -> ()` is weg. De uitvoer van de vijf proeven is vóór en ná
   gelijk, en de sweep is byte-identiek. Pin: één staande check (een
   misvormde datumliteral weigert in de lezer). De datum-tijdliteral had
   al twee pins. De asOfDate-weigering pint refs eigen suite.
3. **Gedeelde UNSAT-diagnose** (`Solve.diagnose`), zie (b).
4. **Venstercommentaar** (tweede item van de hoofdsessie, eigen commit
   `e8b12f93`, alleen commentaar). Het commentaar bij
   `periode_onder_telkwantor` (basis r. ~516, branch r. 543) citeerde de
   weigerzin van de DRL-tekst-tak van 2026-09-20 ("periodevergelijking
   onder een tellende kwantor") als geldende meting. Het dateert die
   meting nu (vóór kaart drools-venster-vouwtoets, 2026-09-23) en noemt
   de nieuwe stand volgens het Result van die kaart: beide Drools-takken
   spelen het venster onder ALLE, de exec-tak nu ook in toets en filter
   van een kwantorvouw, en beide weigeren elke WF-112-positie zelf, bij
   naam, in één gedeelde zin. Die kaart staat op
   `card/drools-venster-vouwtoets` en nog niet op main; het commentaar
   beschrijft de stand na haar landing. Suite 699/0 en gate 1 PASS na de
   wijziging. Aan de code veranderde niets, dus de gate-uitslagen
   hieronder gelden onverkort.

### Gates (elk los, na `eval $(opam env)`, op de gecommitte code)

1 self-contained PASS · 2 ir-schema PASS (279 bestanden, +1; vloer 192) ·
3 verifier PASS (279 schoon, +1 rood; 440 negatief; vloer 350) ·
4 case-schema PASS (640, +2) · 5 oracle PASS (ref-suite 806; 595 cases;
vloer 427) · 6 clingo PASS (suite 699, +176 t.o.v. 523; vloer 83; 595
cases) · 7 noop PASS (559; 36 survivors) · **8 rood FAIL**: `FAIL:
aanvoergeld-parameter-ongebonden carries no drools fragment and is not
in the Drools exclusion list — … NOFAULT ran to a model without
faulting` (twee keer, één per case). Met de voorgestelde regel lokaal
toegepast: `check-rood: Drools leg PASS (9 fault-pinned, 36 not-carried
with reason)` en `check-rood: PASS (45 red cases fault observably in ref
and clingo, floor 26; 90 pinned fragments cite none of the 36 listed
bottom sentences)`. Daarna teruggezet met `cmp` identiek en een schone
worktree · 9 catala PASS (201; 12 goldens) · 10 regelspraak PASS (409
tests; 70 goldens) · 11 toka-parity PASS (29/29/29) · 12 drools PASS
(played 182 = pin; DRL-tekst 161 = pin; JUnit 620) · 13
reftype-derivation PASS (131 twists, 128 replays, 25 DRL) · 14 flogic
PASS (130 checks; 2 goldens).

### Voorstel beschermd pad (gecommit als voorstel in `63f9637d`)

`scripts/check-rood.sh`, Drools-uitsluitingslijst, één regel,
alfabetisch vóór `ambiguous-case-role-key`:
```
aanvoergeld-parameter-ongebonden|NOFAULT measured 2026-09-23 (card clingo-parameter-failfast): both cases run to a model. The backend answers a parameter the case does not bind the way the ASP backend did before that card — case1 aanvoergeld 310.00 / 87.40 / 0.00, the absent tarief coalesced to 0; case2 aanvoerplichtig absent on J1 and J3 — where ref and clingo fault fail-fast in one sentence (ch. 5.7). Listed rather than fragment-pinned because this leg does not fault here at all; the day the backend binds parameters fail-fast, this entry goes stale and the gate says so
```

### Fixronde 1 (vorige bouwer, 2026-09-23)

De lens vond buiten het corpus 17 verifier-schone documenten waar ref en
clingo op main spelen en de branch UNSAT gaf: de garde vuurde waar ref
de parameter nooit leest. Drie commits van de vorige bouwer:

- `ddbe3b7c`: de rechtvaardiging van een vorm-a-default krijgt de `not
  U_any` die haar gevolg al had (lensklasse 1; getuigen d-e, d-l, d-i).
- `3c197a1f`: de Let-behoefte in de garde hangt per IR-knoop aan het pad
  in plaats van per parameternaam (lenspunt 4; getuigen lz-1, lz-3,
  lz-m1). De opdracht van fixronde 2 noemde deze commit lenspunt 6. Hij
  wijzigt alleen de garde: `rule_guard` en de andere hekken zijn in geen
  van de drie commits aangeraakt, en de sweep hieronder vindt geen
  corpusprogramma dat door een van de fixrondes verandert.
- `08585e3e`: een parameter in een Let-waarde wacht op een vraagpredicaat
  dat bij elke lezing van de binder wordt afgeleid (lensklasse 3;
  getuigen iv-c, iv-d, lv-2, lv-4, lv-5).

Gecontroleerd in fixronde 2: de suite van `08585e3e` tegen de vertaler van
`63f9637d`, daarna teruggezet met `cmp` identiek:
```
FAIL defaults, lazy: an exception decides before the form-a justification (d-e): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL defaults, lazy: an exception decides before the form-a justification on default 1 (d-l): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL defaults, lazy: an exception decides a characteristic before the form-a justification (d-i): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: both arms read k, the taken one first (lz-1): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the same branch under an operator (lz-3): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the Match arm reads k first (lz-m1): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the value is read only after a leeg need (iv-c): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the value p plus 1, read only after a leeg need (iv-d): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the same under an operator (lv-4): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the same in the taken arm of a branch (lv-2): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the binder read in a fold filter over an empty range (lv-5): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
test_clingo: 735 checks, 11 FAILURES
```
Op `08585e3e` zelf: `test_clingo: 735 checks, 0 failures`. De drie commits
doen wat hun boodschap zegt. De klasse van lenspunt 4 bleef wel
bereikbaar, en de Let-analyse had ook een stil gat (fixronde 2, punt 4).

### Fixronde 2 (2026-09-23)

Proeven onder /tmp (niet in de repo), elk verifier-schoon, gedraaid met
ref, clingo op main `e7e9e178`, op `08585e3e` en op de tip. "fout" is
refs `EVALUATION FAULT: parameter "p" is unbound …`, "UNSAT" is de garde.

**Punt 2, de kwantorvouw (`bfecfb28`).** ch. 3.9a zet de leden eerst vast,
filter inbegrepen, en leest dan de matrix lid voor lid in de eigen
volgorde van de leden, tot de uitkomst vaststaat volgens de
triggertabel van ch. 5.10. Drie rijen daarvan vuren al op het lege
prefix. De garde in de matrix vuurde bij elk lid in bereik. Nu wacht hij
op `kvrN(S,X)`: lid X van S wordt gelezen. Dat predicaat telt de leden en
de treffers vóór X en toetst de negatie van de stoptriggers
(`emit_kv_reach`). De volgorde is die van ref: de vertaler vraagt per
onderwerp in de case `Eval.fold_members` op, dus een geschreven lijst in
zijn geschreven volgorde en een gematerialiseerde op oplopend label
(ch. 5.7a), en schrijft die als feiten. De hulpregels ontstaan alleen
als een garde erom vraagt. Matrix leest ongebonden p; S1 lengte 8, S2
lengte 3:

| proef | vorm | ref | main | 08585e3e | tip |
|---|---|---|---|---|---|
| q-a | TEN MINSTE 2 over [S1] | speelt | speelt | UNSAT | speelt |
| q-b | TEN HOOGSTE 1 over [S1] | speelt | speelt | UNSAT | speelt |
| q-c | PRECIES 2 over [S1] | speelt | speelt | UNSAT | speelt |
| i-h | ALLE over [S2, S1], matrix ALLE [lengte > 5; lengte > p] | speelt | speelt | UNSAT | speelt |
| i-h-rev | idem over [S1, S2] | fout | speelt | UNSAT | UNSAT |
| i-f | GEEN over [S1, S2], matrix TEN MINSTE 1 [lengte > 5; lengte > p] | speelt | speelt | UNSAT | speelt |
| i-f-rev | idem over [S2, S1] | fout | speelt | UNSAT | UNSAT |
| q-d | TEN MINSTE 1 over [S1, S2], dezelfde matrix | speelt | speelt | UNSAT | speelt |
| q-d-rev | idem over [S2, S1] | fout | speelt | UNSAT | UNSAT |

Suite: dertien frames (zeven luie, zes controles, waaronder een
gematerialiseerde lijst op label). Rood eerst op `08585e3e`:
```
FAIL quantifier fold, lazy: at-least 2 over one member reads nothing (q-a): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL quantifier fold, lazy: at-most 1 over one member reads nothing (q-b): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL quantifier fold, lazy: exactly 2 over one member reads nothing (q-c): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL quantifier fold, lazy: ALLE stops at S2 before S1 is read (i-h): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL quantifier fold, lazy: GEEN stops at S1 before S2 is read (i-f): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL quantifier fold, lazy: at-least 1 stops at S1 before S2 is read (q-d): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL quantifier fold, lazy: the materialised order reads A3 first (i-h, labels): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
test_clingo: 761 checks, 7 FAILURES
```
Mutaties op `bfecfb28`, elk teruggezet met `cmp` identiek: MK1, de garde
zonder `kvr`-atoom: `761 checks, 7 FAILURES` (de zeven luie frames). MK2,
geen volgordefeiten, elk lid als gecreëerd: `6 FAILURES` (de zes
controles; clingo antwoordt waar ref faalt). MK3, de tellende triggers
zonder de onbereikbaar-helft: `3 FAILURES` (q-a, q-b, q-c). MK4, ALLE en
GEEN stoppen nooit: `3 FAILURES` (i-h, i-f, i-h op labels).

**BESLISPUNT voor de hoofdsessie: de volgorde van gecreëerde leden.** Een
lid dat een creatie tijdens de evaluatie aan het bereik toevoegt, heeft
geen volgordefeit. ref hangt zulke links achter die uit de case, in zijn
eigen creatievolgorde. De spec legt die volgorde nergens vast: ch. 3.9a
zegt "the contributors' own order" en ch. 5.7a regelt alleen de
case-invoer. Het ASP-programma kent geen creatievolgorde. Voor een lid
uit de case in een bereik van één stap is de vertaling exact, want
daarvóór staan alleen case-leden. Een gecreëerd lid telt elk ander lid
als eerder gelezen, en in een meerstapsbereik telt een case-lid elk
gecreëerd lid als eerder gelezen. Zo vuurt de garde alleen waar elke
creatievolgorde het lid bereikt: nooit UNSAT waar ref speelt, wel stil
waar de ene volgorde het lid bereikt en de andere niet. Gemeten met twee
creaties die elk een schuit voor W1 maken, matrix TEN MINSTE 1 [lengte >
5; lengte > p]:

| proef | leden | ref | main | 08585e3e | tip |
|---|---|---|---|---|---|
| cr-8-3 | gecreëerd 8, dan 3 | speelt | speelt | UNSAT | speelt |
| cr-3-8 | gecreëerd 3, dan 8 | fout | speelt | UNSAT | **speelt** |
| cr-case8-new3 | S1 (8) uit de case, 3 gecreëerd | speelt | speelt | UNSAT | speelt |
| cr-case3-new8 | S2 (3) uit de case, 8 gecreëerd | fout | speelt | UNSAT | UNSAT |

Te beslissen: blijft de stille kant (vastgelegd, zoals nu), wordt het de
luide kant (vuren waar enige volgorde het lid bereikt), of weigert de
vertaler een ongebonden parameter in een matrix over een bereik dat een
creatie kan vullen; en moet de spec de volgorde van gecreëerde links
vastleggen. (Besloten door de hoofdsessie op 2026-09-23, uitgevoerd in
fixronde 3, punt 4; de normvraag staat als open punt 7.)

**Punt 4, de Let-analyse (`409398c5`, `c5b1c7fc`).** Na `3c197a1f` was de
klasse nog bereikbaar, en het oordeel "de binder wordt zeker eerder
gelezen" was ook stil (restpost leeg, p ongebonden):

| proef | vorm | ref | main | 08585e3e | tip |
|---|---|---|---|---|---|
| lz-m3 | `Let k = restpost in ((match klasse: Hoog → k, Laag → 0,00) plus p)`, Hoog | speelt | speelt | UNSAT | speelt |
| lz-m3-ctl | idem, Laag | fout | speelt | UNSAT | UNSAT |
| lz-c | `Let k = restpost in (((als q > 0 dan k anders 0) plus p) plus k)`, q = 5 | speelt | speelt | UNSAT | speelt |
| lz-c-ctl | idem, q = -5 | fout | speelt | UNSAT | UNSAT |
| lz-d | `Let k = restpost in (((als q > 0 dan k anders 0) plus (als q > 0 dan 0 anders k)) plus p)` | speelt | speelt | UNSAT | speelt |
| lz-s2 | `Let k = restpost in ((Let m = extra in (m plus k)) plus p)`, extra leeg | fout | speelt | **speelt** | UNSAT |
| lz-s2-ctl | idem, extra 1 | speelt | speelt | speelt | speelt |
| lz-s | `Let k = restpost in ((Let j = (Let m = extra in (m plus k)) in j) plus p)`, extra leeg | fout | speelt | **speelt** | UNSAT |

De stille rijen: een binnenste Let met een lege behoefte eindigt voordat
hij k leest, en dan bereikt ref p; de analyse telde die Let als zeker
lezend en liet de garde op k's behoefte wachten.

De garde vraagt nu per plaats of een eerdere lezing van de binder, op
het gekozen pad, de behoefte leeg vond (`reach_at`, `emit_barriers`). De
vertaler nummert het lichaam van elke Let in refs evaluatievolgorde
(`let_order`, ch. 5.11). De waarde van een geneste Let krijgt geen
plaats: die loopt bij de eerste lezing van haar binder. Elke lezing van
een binder bewaart waar ze staat. Vraagt een garde erom, dan krijgt die
Let drie hulppredicaten: `lbnN` (de behoefte), `lbrN(S,I)` (de lezing
op plaats I wordt bereikt, met haar eigen bereik; binnen de waarde van
een geneste Let op de plaatsen waar die binder gelezen wordt) en
`lbbN(S,I)` (een eerdere lezing vond de behoefte leeg). De garde draagt
`not lbbN(S,I)` in plaats van de behoefte. Een barrièreregel leest
alleen barrières van dieper geneste Lets, dus het programma blijft
gestratificeerd. `params_before_binder`/`nodes_before_binder` zijn weg.
`c5b1c7fc`: `409398c5` hield een deel dat per lid draait (filter en
argumenten van een setvouw, filter en matrix van een kwantorvouw)
ongeordend, en maakte daarmee de gewone vorm `(k plus lengte) groter dan
p` in een filter UNSAT waar ref speelt. Zo'n deel is nu geordend zoals
één lid het uitvoert.

Rood eerst, de suite van `409398c5` tegen de vertaler van `bfecfb28`,
teruggezet met `cmp` identiek:
```
FAIL Let, lazy: the Match arm read k before p (lz-m3): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the taken arm read k before p, a later operand again (lz-c): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: each path reads k in one of two branches before p (lz-d): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let: an inner barrier ends before k is read, so p is (lz-s2): clingo fires the shared fail-fast (clingo answered heffing leeg: the guard waited for k's need, which the inner Let never read)
FAIL Let: a value's inner barrier ends before k is read, so p is (lz-s): clingo fires the shared fail-fast (clingo answered heffing leeg: the guard waited for k's need, which the value never read)
test_clingo: 781 checks, 5 FAILURES
```
Het frame van `c5b1c7fc` op `409398c5`:
```
FAIL Let, lazy: each member's filter reads k before p (per member): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
test_clingo: 785 checks, 1 FAILURES
```
Mutaties op `c5b1c7fc`, elk teruggezet met `cmp` identiek: ML1, geen
barrière-atoom, elke behoefte uit de garde: `785 checks, 17 FAILURES`
(alle luie Let-frames van beide fixrondes). ML2, de behoeften blijven in
de garde en er is geen barrière-atoom: `11 FAILURES` (de ongebonden
tweelingen en controles van de Let-frames, waar ref p leest; lz-s en
lz-s2 inbegrepen). ML3, een lezing
binnen de waarde van een geneste Let telt nergens: `1 FAILURES` (de
controle van lz-s). Wat de garde hier niet exact maakt, staat in open
punten 5 en 8.

**Punt 5, de teksten (`92c633b3` en dit Result).** README, het
commentaar bij `unbound_param_guard` en de kop van de suite zeiden
"UNSAT exactly where ref faults and answers where ref never reads it" en
"nowhere else". Ze noemen nu de vier plaatsen waar dat niet geldt. Het
Result en de registerzinnen zijn in deze commit rechtgezet.

**De laatste notitie van de vorige bouwer.** "Confirmed pre-existing
(base and branch alike), and parameter-free; it goes in the open points"
sloeg op de deling-door-nul-variant van de kwantorvouwklasse. Gemeten:
i-h0 (ALLE over [S2, S1], matrix ALLE [lengte > 5; (lengte gedeeld door
0) > 1]) en q-a0 (TEN MINSTE 2 over [S1], matrix (lengte gedeeld door 0)
> 1) geven ref PASS en `UNSATISFIABLE — viol("division by numeric
zero" …)` op main, op `08585e3e` en op de tip. Open punt 9.

**Sweep.** Hetzelfde gereedschap als in (c), onder /tmp: 644 cases, 625
programma's. Main (`e7e9e178`) tegen de tip: alleen de twee rode cases
van deze kaart verschillen, met één garderegel per programma en een
waargenomen fout in plaats van een model:
```
case1-tarief-ongebonden prog.0.lp:
> viol("parameter \"aanvoertarief\" is unbound in this case (binding is fail-fast, ch. 5.7)",S) :- isa(S,"jutter").
out.txt: < EXPECTED FAULT DID NOT OCCUR: clingo found a model
         > clingo: expected fault observed (parameter "aanvoertarief" is unbound in this case (binding is fail-fast, ch. 5.7))
case2-drempel-ongebonden prog.0.lp:
> viol("parameter \"aanvoerdrempel\" is unbound in this case (binding is fail-fast, ch. 5.7)",S) :- isa(S,"jutter").
out.txt: idem met aanvoerdrempel
```
`08585e3e` tegen de tip: `diff -r` leeg over alle 644 cases. Geen
corpusprogramma verandert dus door een van de fixrondes, `3c197a1f`
inbegrepen.

**Gates (fixronde 2, elk los na `eval $(opam env)`, op `92c633b3`).**
1 self-contained PASS · 2 ir-schema PASS (283 bestanden, vloer 192) · 3
verifier PASS (283 schoon, 441 negatief, vloer 350) · 4 case-schema PASS
(644) · 5 oracle PASS (ref-suite 806, vloer 489; 595 cases, vloer 427) ·
6 clingo PASS (suite 785, vloer 83; 595 cases) · 8 rood PASS (49 rode
cases, vloer 26; Drools 10 fault-pinned, 39 not-carried; 98 fragmenten
zonder bodemzin).

### Fixronde 3 (lensronde 2, 2026-09-23)

De lens mat ongeveer 160 eigen varianten. Alle 17 getuigen van ronde 1 en
de frames van fixronde 2 deden wat ze claimden, en de corpusvertaling was
byte-identiek aan main. Vier punten waren blokkerend, het vijfde betrof de
teksten. Mijn proeven (54, onder /tmp, niet in de repo) zijn met ref en
clingo gedraaid op main `7d97353e`, op `2d739d9e` (de tip vóór deze
ronde) en op de tip. Op twee na zijn ze verifier-schoon; de twee vuile
(een kwantorvouw als conditie van een waarde-IfThenElse) tellen niet mee.
"fout" is refs `EVALUATION FAULT: parameter "p" is unbound …`, "UNSAT" de
garde in refs woorden, "benoemd" de nieuwe weigering van punt 4.

**Punt 1, schaduwing (`7b009ae3`).** De garde sloeg bij een lezing van
een binder elke omsluitende Let over die dezelfde NAAM bond (`reach_at`
vergeleek `Some li.li_binder = skip`). Een binnenste Let met de naam van
een buitenste nam zo diens barrière mee. Nu kent een lezing de Let die ze
leest bij identiteit: `skip` is een `let_info`, `note_binder_read` vindt
de Let via de binding die de scope oplevert en leidt diens vraag af
(voorheen via een naamopzoeking in `ctx.demands`), en `read_places` geeft
de Let door in plaats van zijn naam. Restpost en extra leeg, p ongebonden:

| proef | vorm | ref | main | 2d739d9e | tip |
|---|---|---|---|---|---|
| sh-4 | `Let k = restpost in (k plus (Let k = p in k))` | speelt | speelt | UNSAT | speelt |
| sh-4-ctl | idem, restpost 1 | fout | MISMATCH | UNSAT | UNSAT |
| sh-5 | `Let k = restpost in (k plus (Let k = (Let k = p in k) in k))` | speelt | speelt | UNSAT | speelt |
| sh-5b | `Let k = restpost in (k plus (Let j = 1 in (Let k = p in (k plus j))))` | speelt | speelt | UNSAT | speelt |
| sh-7 | `Let x = restpost in ((Let k = extra in (k plus (Let k = x in k))) plus p)` | fout | speelt | **speelt** | UNSAT |
| sh-7b | idem, de binnenste binder heet m | fout | speelt | UNSAT | UNSAT |
| sh-7-ctl | sh-7 met extra 1 | speelt | speelt | speelt | speelt |
| id-payload | `Let k = p in (match optie: Aanwezig k → k, Afwezig → 0)` | speelt | speelt | speelt | speelt |

**Punt 3, gesloten lichaam (`86f3bf1d`).** Het vraagpredicaat van
`08585e3e` werkt op regelniveau; `body_branches` vertaalde de waarde van
een Let in een vouwlambda bij de Let. Met een ongebonden parameter in die
waarde vuurde de garde voor elk element, en zonder garde droeg de arm die
de binder niet leest het param-atoom van de waarde, zodat de vouw
onafgeleid bleef (main). Nu zet `body_branches` een keten van Lets boven
een Match (`closed_let_chain`) per arm om die arm heen; wat de arm niet
leest valt weg in de arm voor een ongelezen binder, en een tak binnen de
arm gaat door de bestaande verplaatsing (`let_into_arm`). Alleen voor een
keten waarvan een waarde een ongebonden parameter kan lezen, zonder
dubbele namen en zonder dat een armbinder een naam uit een waarde vangt;
elke andere vorm houdt de waarde bij de Let (luid). Een vouw over J1's
vangsten (Waardevol b / Waardeloos), init 0,00:

| proef | lambda | vangsten | ref | main | 2d739d9e | tip |
|---|---|---|---|---|---|---|
| cl-1 | `Let k = p in match x {Waardevol b → acc plus (b plus k); Waardeloos → acc}` | [W, W] | speelt (0,00) | MISMATCH | UNSAT | speelt |
| cl-1-ctl | idem | [W, 5,00] | fout | speelt | UNSAT | UNSAT |
| cl-1-bound | idem, p = 1 | [W, 5,00] | speelt (6,00) | speelt | speelt | speelt |
| cl-2 | `Let k = p in Let j = 1,00 in match …` | [W] | speelt | MISMATCH | UNSAT | speelt |
| cl-2-ctl | idem | [5,00] | fout | speelt | UNSAT | UNSAT |
| cl-3 | `Let k = p in match x {Waardevol b → als b > 1 dan acc plus k anders acc; …}` | [0,50, W] | speelt | MISMATCH | UNSAT | speelt |
| cl-3-ctl | idem | [5,00] | fout | speelt | UNSAT | UNSAT |
| cl-1-noval | waarde `p plus 0`, armbinder p2 | [W] | speelt | MISMATCH | UNSAT | speelt |

(W is Waardeloos; cl-1-shadow, cl-1-bound-leeg, cl-2-bound en cl-3-bound
spelen overal.)

**Punt 2, een deel dat per lid draait (`82f78afe`).** Fixronde 2 ordende
zo'n deel zoals één lid het uitvoert. Een lezing van de binder door een
later lid telde dan als eerder dan de parameter bij een eerder lid
(stil), en een lezing door een eerder lid als later dan die bij een later
lid (luid). Nu kent elke lezing en elke garde de delen tussen de Let en
zichzelf, met het lid per deel (`si_parts`). Leden gaan voor: een lezing
door een lid dat ref eerder leest, beëindigt de Let voor alle latere
leden, waar in het deel ze ook staat. Plaatsen tellen alleen binnen
hetzelfde lid, niveau voor niveau door geneste delen (`emit_barriers`,
predicaat `lbgN_k` met de leden als argumenten). Een lid waarvan het
programma de volgorde niet kent, telt niet als eerder, zodat zo'n
barrière nooit een garde stilzet die ref bereikt. Bijvangst: een
kwantormatrix waarvan het bereik pas door de barrièreregels gevraagd werd
(aan het eind van de vertaling), kreeg haar hulpregels nooit. De lezing
telde dan nooit en de garde vuurde waar de barrière de Let al beëindigde
(late-kv). Die regels komen er nu ook laat. W1 met schuiten S1 (3) en S2
(8), restpost leeg:

| proef | filter van `Let k = restpost in som(lengte van schuiten die …)` | ref | main | 2d739d9e | tip |
|---|---|---|---|---|---|
| pm-over-1 | `GEEN[ALLE[lengte > 5, lengte > p], k > 0]` | speelt | speelt | UNSAT | speelt |
| pm-over-1-ctl | idem, restpost 1 | fout | speelt | UNSAT | UNSAT |
| pm-silent-8 | `TEN MINSTE 1 [ALLE[lengte > 5, k > 0], lengte > p]` | fout | speelt | **speelt** | UNSAT |
| pm-silent-8-rev | idem over [S2, S1] | speelt | speelt | speelt | speelt |
| pm-nest-a | kwantorvouw ALLE over de ladingen van elke schuit met matrix `TEN MINSTE 1 [ALLE[gewicht > 5, k > 0], gewicht > p]`; L1 8, L2 3 | speelt | speelt | speelt | speelt |
| pm-nest-b | idem, L1 3, L2 8 | fout | speelt | **speelt** | UNSAT |
| late-kv | `(som(… die ALLE ladingen [k > 0])) plus p`, L1 8 | speelt | speelt | UNSAT | speelt |
| late-kv-ctl | idem, restpost 1 | fout | MISMATCH | UNSAT | UNSAT |

**Punt 4, gecreëerde leden (`82f78afe`), maintainerbesluit.** Leden die
een creatie aan het bereik van een kwantorvouw toevoegt, hebben geen
genormeerde plaats (ch. 3.9a/5.7a). Besluit: waar de volgorde van
gecreëerde links de uitkomst van de garde kan bepalen, weigert de
vertaler benoemd; waar ze er niet toe doet, blijft de garde exact.
Uitvoering. Per vouwbereik schrijft `use_ro` (alleen als een garde erom
vraagt) het bereik `roN_m` en de caselinks in refs volgorde `roN_o`, en
twee relaties. `roN_n`: X gaat vóór Y in elke volgorde die de spec
toestaat, dat zijn de caselinks onderling (in een meerstapsbereik alleen
zonder feitcreaties, die een caselid via de vereniging naar voren kunnen
halen). `roN_r`: X gaat vóór Y in refs volgorde, voor zover het
programma die kent. Dat is `roN_n`, plus in een bereik van één stap
"runtime-links staan achter de caselinks", plus, als elke creatie
onvoorwaardelijk is, alleen literals toekent en niet recursief is en er
geen feitcreatie bestaat, "de links die één onderwerp krijgt volgen de
creatie-index". Ref draait zulke creaties in één pass, in
documentvolgorde. Meer wordt niet geclaimd, omdat een claim die niet
opgaat een garde zou stilzetten waar ref faalt. `emit_kv_reach` telt
twee voorvoegsels. Het eerste bestaat uit alle bijdragers die vóór X
kunnen staan (niet `roN_n(X,Y)`): staat de uitkomst daar nog open, dan
leest elke toegestane volgorde X, en vuurt de garde in refs woorden
(`kvrN`). Het tweede is `roN_r`: staat de uitkomst alleen daar open, dan
vuurt een benoemde tweeling (`kvrN_q`, zin `parameter "p" is unbound in
this case, and whether the quantifier fold "…" reads it depends on the
order of the members a creation adds, which is not normed (ch.
3.9a/5.7a)`). Staat ze daar al vast, dan leest ref X niet. De tweede
prefix is het kleinste voorvoegsel dat ref kan hebben, ook in een eerdere
pass. N is het uiteindelijke aantal bijdragers, het grootste. Een
vraagpredicaat dat een lezing in zo'n matrix afleidt, rekent op refs
volgorde (`kvrN_o`): luid, nooit stil. Matrix `TEN MINSTE 1 [lengte > 5,
lengte > p]` tenzij anders vermeld, creaties per werf in documentvolgorde:

| proef | leden | ref | main | 2d739d9e | tip |
|---|---|---|---|---|---|
| cr-3-8 | gecreëerd 3, dan 8 | fout | speelt | speelt | benoemd |
| cr-8-3 | gecreëerd 8, dan 3 | speelt | speelt | speelt | speelt |
| cr-case8-new3 | S1 (8) uit de case, 3 gecreëerd | speelt | speelt | speelt | speelt |
| cr-case3-new8 | S2 (3) uit de case, 8 gecreëerd | fout | speelt | UNSAT | benoemd |
| cr-new3only | alleen een gecreëerde 3 | fout | speelt | UNSAT | UNSAT |
| mx-alle_new8-new3 | ALLE, matrix `ALLE [lengte > 5, lengte > p]`, 8 dan 3 | fout | speelt | speelt | benoemd |
| mx-alle_new3-new8 | idem, 3 dan 8 | speelt | speelt | speelt | speelt |
| ms-2 | GEEN over H1 → [W1 → [S1 (3)], W2 → [S2 (8)]], per werf een gecreëerde 8 | fout | speelt | speelt | benoemd |
| ms-4 | idem, TEN MINSTE 1 | fout | speelt | speelt | benoemd |
| ms-2-nocr | ms-2 zonder creatie | fout | speelt | UNSAT | UNSAT |
| cr-8-3-cond | cr-8-3, creaties met een voorwaarde die geldt | speelt | speelt | speelt | benoemd (luid) |

Bij ms-2 faalt ref ook zonder dat de creatievolgorde meetelt: sinds
`811864c6` op main (WF-046, de kwantorvouw als negatieve kant) draait de
regel na de creatie, en S1 staat dan vooraan. De benoemde weigering volgt
het model van het besluit, waarin een toegevoegd lid ook vóór S1 kan
staan.

**Punt 5, de teksten (`742cb3f3` en dit Result).** README en het
commentaar bij `unbound_param_guard` noemen de afwijkingen nu per
richting, elk met een getuige (zie "Twee richtingen"). Het commentaar bij
`reach_at` zegt waar de stille kant vandaan komt. De kop van de suite
verwijst al naar de README.

**Suite** 785 → 841 checks: 28 frames, waarvan 14 getuigen en 14
controles. Rood eerst, de eindsuite tegen de vertaler van `2d739d9e`
(vóór de rebase `1691a20e`, zelfde `translate.ml`), daarna teruggezet
met `cmp` identiek:
```
FAIL Let, lazy: an inner Let of the same name keeps the outer barrier (sh-4): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the same name three deep keeps the outer barrier (sh-5): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let: an inner k ends before its inner k reads x, so p is read (sh-7): clingo fires the shared fail-fast (clingo answered heffing leeg: the read of x counted as reached, the outer k's barrier skipped with the inner k's)
FAIL closed body, lazy: only the Waardevol arm reads k (cl-1): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL closed body, lazy: a chain of Lets over the Match (cl-2): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL closed body, lazy: the arm's branch that reads k is not taken (cl-3): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: S1's filter reads k, which ends the Let before S2's reads p (pm-over-1): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let: S1's filter reads p before S2's reads k (pm-silent-8): clingo fires the shared fail-fast (clingo answered bonus leeg: S2's read of k counted as before S1's read of p)
FAIL Let: nested, S1's lading reads p before S2's reads k (pm-nest-b): clingo fires the shared fail-fast (clingo answered bonus leeg: S2's lading's read of k counted as before S1's read of p)
FAIL Let, lazy: k read in a nested matrix ends the Let before p (late matrix reach): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL created members: 3 then 8, the first read reads p (cr-3-8): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL created members: ALLE over 8 then 3 reads p at 8 (mx-alle_new8-new3): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL created members: GEEN over two steps, S1 first in ref's order (ms-2): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL created members: at-least 1 over two steps, S1 first in ref's order (ms-4): clingo refuses in the named sentence (clingo answered the case's rows)
test_clingo: 841 checks, 14 FAILURES
```
De 14 controles zijn ook daar groen: de gevulde tweelingen van sh-4 en
sh-5, sh-7b, de drie cl-controles en die met p gebonden, de controles van
pm-over-1 en pm-silent-8, pm-nest-a, cr-8-3, cr-case8-new3, een enkele
gecreëerde 3, en ms-2 zonder creatie. Op de tip: `841 checks, 0
failures`. Het nieuwe frame-hulpmiddel `order_named` eist ref's fout, de
benoemde zin in een getuige en géén getuige met refs zin.

Mutatieproeven op de gecommitte code, elk teruggezet met `cmp` identiek:
- M-sh, de overgeslagen Let weer op naam: `3 FAILURES` (sh-4, sh-5, sh-7).
- M-cl1, `closed_let_chain` altijd `None`: `3 FAILURES` (cl-1, cl-2,
  cl-3). M-cl2, elke arm in alle Lets van de keten in plaats van alleen de
  gelezen: `0 FAILURES`. De bestaande arm voor een ongelezen binder laat
  een overbodige Let al vallen; de selectie is daarom geschrapt.
- MP1, geen delen in de vraag (alles op plaats, als in fixronde 2): `3
  FAILURES` (pm-over-1, pm-silent-8, pm-nest-b). MP2, geen ledenregels,
  alleen plaats binnen één lid: `3 FAILURES` (pm-over-1 en de controles
  pm-silent-8-rev en pm-nest-a). MP3, elk ander lid telt als eerder: `2
  FAILURES` (pm-silent-8, pm-nest-b).
- MK1, geen benoemde tweeling: `4 FAILURES` (cr-3-8, mx-alle, ms-2,
  ms-4). MK2, het eerste voorvoegsel op refs volgorde in plaats van elke
  toegestane: `4 FAILURES`, dezelfde vier, nu met refs zin. MK3, geen
  "runtime-links achter de caselinks": `1 FAILURES` (cr-case8-new3). MK4,
  geen creatie-index: `1 FAILURES` (cr-8-3). MK5, geen late hulpregels
  voor de matrix: `1 FAILURES` (late-kv).

**Rebase en sweep.** Rebase op main `7d97353e` zonder conflict: main
raakte `backends/clingo` en `scripts/check-rood.sh` niet (wel ref:
caselabels, WF-046). Hashes hierboven per commitonderwerp omgezet. Sweep
met hetzelfde soort gereedschap als in (c), onder /tmp: een runner
gebouwd uit main `7d97353e` tegen de tip, 658 cases (main heeft nu 48
rode cases, deze kaart voegt er 2 toe), 636 tegen 638 programma's.
`diff -r` vindt alleen de twee rode cases van deze kaart, met één
garderegel per programma, een waargenomen fout in plaats van een model en
de diagnoseherhaling als extra programma, precies als in fixronde 2.

**Gates (fixronde 3, elk los na `eval $(opam env)`, op `742cb3f3`).** 1
self-contained PASS · 2 ir-schema PASS (286 bestanden, vloer 192) · 3
verifier PASS (286 schoon, 454 negatief, vloer 350) · 4 case-schema PASS
(658) · 5 oracle PASS (ref-suite 931, vloer 489; 608 cases, vloer 427) ·
6 clingo PASS (suite 841, vloer 83; 608 cases) · 8 rood PASS (50 rode
cases, vloer 26; Drools 11 fault-pinned, 39 not-carried; 100 fragmenten
zonder bodemzin).

### Registerzinnen (hoofdsessie, `spec/CONSTRUCTS.md`)

Rij `ParameterRef`: voeg aan de laatste cel toe:

"Binding is fail-fast (RUNTIME-SEMANTICS ch. 5.7) in both OCaml
executors since card `clingo-parameter-failfast` (2026-09-23): a scalar
parameter the case does not bind faults in ref's words (`parameter "…"
is unbound in this case (binding is fail-fast, ch. 5.7)`) where
evaluation reads it. ref faults at the read. The ASP backend carries one
viol/2 guard per read, keyed on the read's reach (ch. 5.11): condition,
composite prefix, a quantifier fold's open prefix in ref's member order,
taken arm or match arm (in a closed body too), the ch. 10.6 barrier of
every enclosing Let decided per read and, in a part that runs member by
member, per member, fold member and range, closed-body subject, default
displacement and justification, a form-a default's exception groups.
Members a creation adds to a quantifier fold's range have no normed
order (ch. 3.9a/5.7a): a matrix read that every allowed order reaches
fires in ref's words; one only ref's order reaches fires in a sentence
that names the unnormed order (maintainer decision 2026-09-23). Where
the guard is not exact it errs in one recorded direction. Loud, a
refusal where ref plays: the day-kind table and the §7.1 horizon fence
read at translation and refuse there, also behind a failing condition;
where the program does not know ref's order of the added members (it
does for unconditional creations with literal initialisations and no
fact creation), the named refusal takes the smallest prefix ref can
have; a Let barrier in a per-member part stops the guard only for a
member whose order is known, and fires in ref's words, never in the
named sentence. Silent, an answer where ref faults: the Let lowering
keeps a value behind an enclosing need on a path that does not read it,
and the guard follows that lowering, which deviates without any
parameter. A list-typed parameter reads leeg on absence in both. Red
twin `corpus/rood/aanvoergeld-parameter-ongebonden` (value and
condition position) pins the refusal in ref and clingo. The Drools
backend still answers there (NOFAULT, listed in gate 8)."

Rij `— Predicaatsoort`: vervang "An unbound scalar parameter in the
operand faults in ref's words." door "An unbound scalar parameter in the
operand faults in ref's words wherever the test is reached, through the
shared parameter fail-fast (row `ParameterRef`, with the deviations
recorded there)."

### Open punten

1. **Drools antwoordt op een ongebonden parameter** (zone van een
   parallelle kaart). De replay geeft op de rode tweeling dezelfde
   stille bedragen en hetzelfde afwezige kenmerk als de ASP-backend vóór
   deze kaart. Dit is dezelfde klasse, nu aan de Drools-kant. Tot dat
   gerepareerd is, staat de rode case met reden in de lijst.
2. **De gate-8-regel landt via de hoofdsessie.** Ze staat als voorstel in
   `63f9637d`. `check-rood.sh` is een beschermd pad en een parallelle
   rood-kaart bewerkt het, dus de regel landt serieel. Met de regel is
   gate 8 groen (50 rode cases na de rebase op `7d97353e`).
3. **Andere hekken, dezelfde Let-volgorde** (bestond al, werk van een
   andere orde). Deling door nul, bereikhekken en §6.5-meldingen gebruiken
   `rule_guard` met de Let-behoeften erin. Een fout vóór de eerste
   lezing van een lege binder blijft daardoor stil. Gemeten A06: `Let k
   = leeg in ((10 gedeeld door 0) plus k)` geeft ref `EVALUATION FAULT:
   division by numeric zero` en clingo `PASS`. De barrièrepredicaten van
   fixronde 2 kunnen dit dragen, maar het verschuift de hekken van
   bestaande programma's en hoort dus in een eigen kaart. Hetzelfde
   geldt voor de hekken in defaults: `rule_guard` daar zetten verschoof
   60 corpusprogramma's (gemeten, teruggedraaid). In een gesloten lichaam
   wacht de waarde van een Let sinds fixronde 3 op de arm die haar leest,
   maar alleen voor een waarde met een ongebonden parameter; een ander
   hek in zo'n waarde (deling door nul) vuurt nog bij de Let, ook waar
   de lezende arm niet genomen wordt (bij lezing van de code, niet
   gemeten).
4. **De dagsoorttabel en het horizonhek weigeren in de vertaling**, ook
   waar ref de lezing niet bereikt. Ze zijn gretig ten opzichte van de
   conditie van de regel. Dat was al zo en ze gebruiken nu wel de
   gedeelde zin. Getuige hf-cond (fixronde 3): het horizonhekframe met
   voorwaarde 1 > 2 geeft ref `PASS` en clingo een vertaalfout.
5. **De Let-verlaging houdt een waarde achter een omsluitende behoefte**
   (bestond al, parametervrij). De behoefte van een Let geldt voor zijn
   hele lichaam, behalve waar de vertaler een tak naar buiten kan halen
   (`reads_binder`, `let_into_arm`: "derive LESS … a missing write"). Met
   p gebonden gemeten: lz-m3-bound-laag (`Let k = restpost in ((match
   klasse: Hoog → k, Laag → 0,00) plus p)`, Laag, p = 10) en lz-v-bound
   (`Let k = restpost in Let bel = (als q > 0 dan k anders 0) in (bel
   plus p)`, q = -5, p = 10) geven ref 10,00 en op main (`e7e9e178` en
   `7d97353e`), `08585e3e`, `2d739d9e` en de tip `MISMATCH: J1.heffing is
   underived (leeg)`. De garde volgt de verlaging: de behoefte van bel
   lijkt leeg zolang k leeg is, dus met p ongebonden (lz-v-ctl) faalt ref
   en antwoordt clingo. Daarom pint de suite de controle van lz-v niet.
   Een verlaging die de behoefte per pad oplegt (de barrièrepredicaten
   van fixronde 2 kunnen dat) verandert de vertaling van elk
   Let-lichaam in het corpus: eigen kaart. Dit is de enige stille plek
   (zie "Twee richtingen").
6. Posities die een parameter ongeacht de binding niet dragen (tekst,
   enum, boolean, datum-tijd, som, struct, een Begrensd-grens die geen
   literal is) falen bij een ongebonden parameter in beide uitvoerders,
   maar in andere woorden. Dat is luid, niet stil, en het bestond al.
7. **Normvraag: de volgorde van door creatie gevulde leden in een
   vouwbereik.** Ch. 3.9a leest de matrix "in the contributors' own
   order" en ch. 5.7a legt alleen de volgorde van de case-invoer vast. Waar
   een creatie of een feitcreatie een lid aan het bereik toevoegt, zegt
   de spec niet waar dat lid staat. ref hangt zulke links achter de
   eigen links van de ouder, in de volgorde waarin zijn passes de
   creaties draaien. Het maintainerbesluit van 2026-09-23 (fixronde 3,
   punt 4) laat clingo daar benoemd weigeren waar de volgorde beslist.
   Legt de spec de volgorde vast, bijvoorbeeld "achter de caselinks, in
   documentvolgorde van de creaties", dan wordt de benoemde weigering
   een gewone garde in refs woorden. Hetzelfde geldt voor de Let-
   barrière van punt 8 en voor de plaats van een gecreëerde link in een
   meerstapsbereik: ref zet haar achter de caselinks van haar eigen
   tussenschakel en vóór die van een latere. Sinds `811864c6` (WF-046: de
   kwantorvouw draait na haar creaties) kan die plaats de uitkomst
   bepalen.
8. **Een Let-barrière in een deel per lid over leden van een creatie.**
   Sinds fixronde 3 is de barrière per lid exact waar het programma de
   ledenvolgorde kent. Een lid waarvan het de volgorde niet kent (van een
   creatie met voorwaarde of een feitcreatie, of een gecreëerd lid in een
   meerstapsbereik) telt niet als eerder, en de garde vuurt dan in refs woorden, ook waar ref
   speelt (pm-cr-8-3-cond). Waar het de volgorde wel kent, vuurt de garde
   precies waar ref faalt, maar in refs woorden en niet in de benoemde
   zin die de matrix krijgt (pm-cr-3-8). Het besluit van punt 7 is
   letterlijk op de kwantormatrix genomen. Een benoemde tweeling voor de
   barrière vraagt een tweede barrièrerelatie ("mogelijk eerder" naast
   "zeker eerder") en een benoemde variant van het vraagpredicaat. Dat is
   niet gebouwd; de hoofdsessie beslist of het moet.
9. **Deling door nul in de matrix van een kwantorvouw negeert de
   kortsluiting** (bestond al, parametervrij; de laatste notitie van de
   vorige bouwer). i-h0 en q-a0 geven ref PASS en UNSAT op main,
   `08585e3e` en `2d739d9e`. Het `kvr`-predicaat kan de andere hekken in
   de matrix dragen, maar dat verschuift hekken in bestaande
   corpusprogramma's: samen met punt 3 in een eigen kaart.
10. **Advies 8 van de lens: een niet-monotone vouw vóór de creatie die
    haar bereik vult** (bestond al, parametervrij). "keur 99 indien ALLE
    schuiten lengte > 5", W1 zonder schuiten in de case en een creatie
    die een schuit van 3 maakt: op main `e7e9e178` schreef ref keur 99,
    omdat het de vouw over het nog lege bereik evalueerde en niet
    terugtrok, en clingo schreef niets. Met GEEN en een schuit van 8
    idem, met TEN MINSTE 1 gelijk; verifier-schoon (a8-alle, a8-geen,
    a8-tm1). Na de rebase op `7d97353e` geven alle drie ref = clingo
    (keur leeg): `811864c6` (WF-046) telt bereik en matrix van de
    kwantorvouw als negatieve kant, zodat de regel pas na de creatie
    draait. Voor de kwantorvouw is het advies daarmee opgelost. Voor
    andere niet-monotone lezingen van een groeiend bereik hoort het bij
    de kaart verifier-leegtest-negafhankelijkheid (restpunt "a value that
    grows").
