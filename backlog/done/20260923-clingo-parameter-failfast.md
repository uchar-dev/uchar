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
leest, faalt nu in refs woorden waar ref faalt. Waar ref de lezing niet
bereikt, speelt clingo ook, behalve op vier vastgelegde plaatsen (open
punten 4, 5, 7 en 8). De eerste versie van dit Result schreef "speelt
waar ref speelt" zonder uitzondering; de lens weerlegde dat en de twee
fixrondes hieronder leggen vast wat er gerepareerd is en wat niet.
Gemeten op 88 corpusvarianten (88/88 in overeenstemming), op 66
proefdocumenten van de eerste ronde en 29 van de tweede fixronde, en in
de suite (785 checks). De corpusvertaling is byte-identiek aan main, op
de twee rode cases van deze kaart na. De Drools-lijstregel voor gate 8
staat als voorstel in commit `21e6b22e` (beschermd pad); daarmee is gate
8 groen (49 rode cases).

Commits (hashes na de rebase op main `e7e9e178`): `133ce58c` (claim),
`af901cc1` (de gedeelde fail-fast met suite, README en de gedeelde
diagnose), `a404c327` (de dode datumlezingen, item van de hoofdsessie),
`15ddac29` (rode corpusdrager), `f492aaa9` (kaart naar done),
`d8c26075` (venstercommentaar, tweede item van de hoofdsessie),
`dd32569a` (Result), `21e6b22e` (gate-8-regel, voorstel beschermd pad),
fixronde 1: `f830ed3b`, `d760a99b`, `011e1030`, fixronde 2: `f8c2f9d4`,
`b5e114eb`, `e1e6c873`, `c62f0aed`, en de commit van dit Result.

### (a) Inventaris

Elke match op de parameterconstructor in `translate.ml`
(regelnummers van de branchtip):

| r. | functie | aard | stand na de kaart |
|----|---------|------|-------------------|
| 447 | `eff_unit` | statisch: eenheid uit de declaratie | geen waardelezing |
| 710/769 | `params_before_binder` | statisch: de nieuwe volgordeanalyse | geen waardelezing |
| 1292/1330 | `provably_unitless` / `provably_pure_pct` | statisch: eenheidsbewijs | geen waardelezing |
| **2061** | `expr_term` | DE scalaire lezing (`param/2`) | **draagt de gedeelde garde** |
| 2609 | `expr_term` (`filter_reads` van het horizonhek) | statisch: vormtoets | geen waardelezing |
| 3429 | `expr_term` (TemporalRead) | granulaire parameter, punt- of vintagelezing | faalde al in refs woorden (`needs its timeline in the case`); ongewijzigd |
| 4638 | `whole_period_fails` | vormtoets op de grens | leest via `expr_term` (garde), nu met het onderwerp van de aanroeper |
| **4794** | `dk_value` | dagsoorttabel, geëvalueerd in de vertaling | vertaalfout, nu met de gedeelde zin |
| 6349 | `list_source` | lijstparameter (`plist`) | afwezig is leeg, in beide uitvoerders; vrijgesteld |
| **6608** | `closed_term` | scalaire lezing in een gesloten body | **draagt de gedeelde garde** |
| 7029 | `value_condition` | statisch: WF-077-vormtoets | geen waardelezing |

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
   `a179db05`, alleen commentaar). Het commentaar bij
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

### Voorstel beschermd pad (gecommit als voorstel in `21e6b22e`)

`scripts/check-rood.sh`, Drools-uitsluitingslijst, één regel,
alfabetisch vóór `ambiguous-case-role-key`:
```
aanvoergeld-parameter-ongebonden|NOFAULT measured 2026-09-23 (card clingo-parameter-failfast): both cases run to a model. The backend answers a parameter the case does not bind the way the ASP backend did before that card — case1 aanvoergeld 310.00 / 87.40 / 0.00, the absent tarief coalesced to 0; case2 aanvoerplichtig absent on J1 and J3 — where ref and clingo fault fail-fast in one sentence (ch. 5.7). Listed rather than fragment-pinned because this leg does not fault here at all; the day the backend binds parameters fail-fast, this entry goes stale and the gate says so
```

### Fixronde 1 (vorige bouwer, 2026-09-23)

De lens vond buiten het corpus 17 verifier-schone documenten waar ref en
clingo op main spelen en de branch UNSAT gaf: de garde vuurde waar ref
de parameter nooit leest. Drie commits van de vorige bouwer:

- `f830ed3b`: de rechtvaardiging van een vorm-a-default krijgt de `not
  U_any` die haar gevolg al had (lensklasse 1; getuigen d-e, d-l, d-i).
- `d760a99b`: de Let-behoefte in de garde hangt per IR-knoop aan het pad
  in plaats van per parameternaam (lenspunt 4; getuigen lz-1, lz-3,
  lz-m1). De opdracht van fixronde 2 noemde deze commit lenspunt 6. Hij
  wijzigt alleen de garde: `rule_guard` en de andere hekken zijn in geen
  van de drie commits aangeraakt, en de sweep hieronder vindt geen
  corpusprogramma dat door een van de fixrondes verandert.
- `011e1030`: een parameter in een Let-waarde wacht op een vraagpredicaat
  dat bij elke lezing van de binder wordt afgeleid (lensklasse 3;
  getuigen iv-c, iv-d, lv-2, lv-4, lv-5).

Gecontroleerd in fixronde 2: de suite van `011e1030` tegen de vertaler van
`21e6b22e`, daarna teruggezet met `cmp` identiek:
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
Op `011e1030` zelf: `test_clingo: 735 checks, 0 failures`. De drie commits
doen wat hun boodschap zegt. De klasse van lenspunt 4 bleef wel
bereikbaar, en de Let-analyse had ook een stil gat (fixronde 2, punt 4).

### Fixronde 2 (2026-09-23)

Proeven onder /tmp (niet in de repo), elk verifier-schoon, gedraaid met
ref, clingo op main `e7e9e178`, op `011e1030` en op de tip. "fout" is
refs `EVALUATION FAULT: parameter "p" is unbound …`, "UNSAT" is de garde.

**Punt 2, de kwantorvouw (`f8c2f9d4`).** ch. 3.9a zet de leden eerst vast,
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

| proef | vorm | ref | main | 011e1030 | tip |
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
gematerialiseerde lijst op label). Rood eerst op `011e1030`:
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
Mutaties op `f8c2f9d4`, elk teruggezet met `cmp` identiek: MK1, de garde
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

| proef | leden | ref | main | 011e1030 | tip |
|---|---|---|---|---|---|
| cr-8-3 | gecreëerd 8, dan 3 | speelt | speelt | UNSAT | speelt |
| cr-3-8 | gecreëerd 3, dan 8 | fout | speelt | UNSAT | **speelt** |
| cr-case8-new3 | S1 (8) uit de case, 3 gecreëerd | speelt | speelt | UNSAT | speelt |
| cr-case3-new8 | S2 (3) uit de case, 8 gecreëerd | fout | speelt | UNSAT | UNSAT |

Te beslissen: blijft de stille kant (vastgelegd, zoals nu), wordt het de
luide kant (vuren waar enige volgorde het lid bereikt), of weigert de
vertaler een ongebonden parameter in een matrix over een bereik dat een
creatie kan vullen; en moet de spec de volgorde van gecreëerde links
vastleggen.

**Punt 4, de Let-analyse (`b5e114eb`, `e1e6c873`).** Na `d760a99b` was de
klasse nog bereikbaar, en het oordeel "de binder wordt zeker eerder
gelezen" was ook stil (restpost leeg, p ongebonden):

| proef | vorm | ref | main | 011e1030 | tip |
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
`e1e6c873`: `b5e114eb` hield een deel dat per lid draait (filter en
argumenten van een setvouw, filter en matrix van een kwantorvouw)
ongeordend, en maakte daarmee de gewone vorm `(k plus lengte) groter dan
p` in een filter UNSAT waar ref speelt. Zo'n deel is nu geordend zoals
één lid het uitvoert.

Rood eerst, de suite van `b5e114eb` tegen de vertaler van `f8c2f9d4`,
teruggezet met `cmp` identiek:
```
FAIL Let, lazy: the Match arm read k before p (lz-m3): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the taken arm read k before p, a later operand again (lz-c): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: each path reads k in one of two branches before p (lz-d): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let: an inner barrier ends before k is read, so p is (lz-s2): clingo fires the shared fail-fast (clingo answered heffing leeg: the guard waited for k's need, which the inner Let never read)
FAIL Let: a value's inner barrier ends before k is read, so p is (lz-s): clingo fires the shared fail-fast (clingo answered heffing leeg: the guard waited for k's need, which the value never read)
test_clingo: 781 checks, 5 FAILURES
```
Het frame van `e1e6c873` op `b5e114eb`:
```
FAIL Let, lazy: each member's filter reads k before p (per member): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
test_clingo: 785 checks, 1 FAILURES
```
Mutaties op `e1e6c873`, elk teruggezet met `cmp` identiek: ML1, geen
barrière-atoom, elke behoefte uit de garde: `785 checks, 17 FAILURES`
(alle luie Let-frames van beide fixrondes). ML2, de behoeften blijven in
de garde en er is geen barrière-atoom: `11 FAILURES` (de ongebonden
tweelingen en controles van de Let-frames, waar ref p leest; lz-s en
lz-s2 inbegrepen). ML3, een lezing
binnen de waarde van een geneste Let telt nergens: `1 FAILURES` (de
controle van lz-s). Wat de garde hier niet exact maakt, staat in open
punten 5 en 8.

**Punt 5, de teksten (`c62f0aed` en dit Result).** README, het
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
zero" …)` op main, op `011e1030` en op de tip. Open punt 9.

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
`011e1030` tegen de tip: `diff -r` leeg over alle 644 cases. Geen
corpusprogramma verandert dus door een van de fixrondes, `d760a99b`
inbegrepen.

**Gates (fixronde 2, elk los na `eval $(opam env)`, op `c62f0aed`).**
1 self-contained PASS · 2 ir-schema PASS (283 bestanden, vloer 192) · 3
verifier PASS (283 schoon, 441 negatief, vloer 350) · 4 case-schema PASS
(644) · 5 oracle PASS (ref-suite 806, vloer 489; 595 cases, vloer 427) ·
6 clingo PASS (suite 785, vloer 83; 595 cases) · 8 rood PASS (49 rode
cases, vloer 26; Drools 10 fault-pinned, 39 not-carried; 98 fragmenten
zonder bodemzin).

### Registerzinnen (hoofdsessie, `spec/CONSTRUCTS.md`)

Rij `ParameterRef`: voeg aan de laatste cel toe:

"Binding is fail-fast (RUNTIME-SEMANTICS ch. 5.7) in both OCaml
executors since card `clingo-parameter-failfast` (2026-09-23): a scalar
parameter the case does not bind faults in ref's words (`parameter "…"
is unbound in this case (binding is fail-fast, ch. 5.7)`) where
evaluation reads it. ref faults at the read. The ASP backend carries one
viol/2 guard per read, keyed on the read's reach (condition, composite
prefix, a quantifier fold's open prefix in ref's member order, taken arm,
the ch. 10.6 barrier of every enclosing Let decided per read, fold member
and range, closed-body subject, default displacement and justification,
a form-a default's exception groups), so a read ref does not reach stays
silent in clingo too (ch. 5.11), with four recorded exceptions. The
day-kind table and the §7.1 horizon fence read at translation and refuse
there, in the same sentence, also where ref never reaches the read.
Members a creation adds to a quantifier fold's range have no order the
program knows, so the guard in its matrix fires only where every
creation order reaches the member. A Let barrier inside a per-member
part is ordered as one member runs it. Where the Let lowering keeps a
value behind an enclosing need on a path that does not read it, the
guard follows the lowering. A list-typed parameter reads leeg on absence
in both. Red twin `corpus/rood/aanvoergeld-parameter-ongebonden` (value
and condition position) pins the refusal in ref and clingo. The Drools
backend still answers there (NOFAULT, listed in gate 8)."

Rij `— Predicaatsoort`: vervang "An unbound scalar parameter in the
operand faults in ref's words." door "An unbound scalar parameter in the
operand faults in ref's words wherever the test is reached, through the
shared parameter fail-fast (row `ParameterRef`)."

### Open punten

1. **Drools antwoordt op een ongebonden parameter** (zone van een
   parallelle kaart). De replay geeft op de rode tweeling dezelfde
   stille bedragen en hetzelfde afwezige kenmerk als de ASP-backend vóór
   deze kaart. Dit is dezelfde klasse, nu aan de Drools-kant. Tot dat
   gerepareerd is, staat de rode case met reden in de lijst.
2. **De gate-8-regel landt via de hoofdsessie.** Ze staat als voorstel in
   `21e6b22e`. `check-rood.sh` is een beschermd pad en een parallelle
   rood-kaart bewerkt het, dus de regel landt serieel. Met de regel is
   gate 8 groen (49 rode cases).
3. **Andere hekken, dezelfde Let-volgorde** (bestond al, werk van een
   andere orde). Deling door nul, bereikhekken en §6.5-meldingen gebruiken
   `rule_guard` met de Let-behoeften erin. Een fout vóór de eerste
   lezing van een lege binder blijft daardoor stil. Gemeten A06: `Let k
   = leeg in ((10 gedeeld door 0) plus k)` geeft ref `EVALUATION FAULT:
   division by numeric zero` en clingo `PASS`. De barrièrepredicaten van
   fixronde 2 kunnen dit dragen, maar het verschuift de hekken van
   bestaande programma's en hoort dus in een eigen kaart. Hetzelfde
   geldt voor de hekken in defaults: `rule_guard` daar zetten verschoof
   60 corpusprogramma's (gemeten, teruggedraaid). Niet gemeten, bij
   lezing van de code gezien: een Let in een gesloten lichaam wordt bij
   de Let vertaald (`closed_term`, `body_branches`), en waar de enige
   lezing van de binder in een Match-arm staat die niet genomen wordt,
   vuren een hek of garde in de waarde toch.
4. **De dagsoorttabel en het horizonhek weigeren in de vertaling**, ook
   waar ref de lezing niet bereikt. Ze zijn gretig ten opzichte van de
   conditie van de regel. Dat was al zo en ze gebruiken nu wel de
   gedeelde zin. Een ongebonden parameter achter een falende conditie
   weigert daar, waar ref speelt.
5. **De Let-verlaging houdt een waarde achter een omsluitende behoefte**
   (bestond al, parametervrij). De behoefte van een Let geldt voor zijn
   hele lichaam, behalve waar de vertaler een tak naar buiten kan halen
   (`reads_binder`, `let_into_arm`: "derive LESS … a missing write"). Met
   p gebonden gemeten: lz-m3-bound-laag (`Let k = restpost in ((match
   klasse: Hoog → k, Laag → 0,00) plus p)`, Laag, p = 10) en lz-v-bound
   (`Let k = restpost in Let bel = (als q > 0 dan k anders 0) in (bel
   plus p)`, q = -5, p = 10) geven ref 10,00 en op main, `011e1030` en de
   tip `MISMATCH: J1.heffing is underived (leeg)`. De garde volgt de
   verlaging: de behoefte van bel lijkt leeg zolang k leeg is, dus met p
   ongebonden (lz-v-ctl) faalt ref en antwoordt clingo op main,
   `011e1030` en de tip. Daarom pint de suite de controle van lz-v niet.
   Een verlaging die de behoefte per pad oplegt (de barrièrepredicaten
   van fixronde 2 kunnen dat) verandert de vertaling van elk
   Let-lichaam in het corpus: eigen kaart.
6. Posities die een parameter ongeacht de binding niet dragen (tekst,
   enum, boolean, datum-tijd, som, struct, een Begrensd-grens die geen
   literal is) falen bij een ongebonden parameter in beide uitvoerders,
   maar in andere woorden. Dat is luid, niet stil, en het bestond al.
7. **BESLISPUNT: de volgorde van gecreëerde leden van een kwantorvouw**
   (fixronde 2, punt 2). De garde vuurt daar alleen waar elke
   creatievolgorde het lid bereikt; cr-3-8 faalt in ref en speelt in
   clingo.
8. **Een Let-barrière in een deel dat per lid draait** is geordend zoals
   één lid het uitvoert. Een lezing van de binder die pas een later lid
   bereikt, telt als eerder dan de parameter bij het eerste lid, en daar
   blijft de garde stil waar ref faalt. Niet gemeten. Exact vraagt refs
   ledenvolgorde binnen de barrière; de volgordefeiten van punt 2 kunnen
   die leveren.
9. **Deling door nul in de matrix van een kwantorvouw negeert de
   kortsluiting** (bestond al, parametervrij; de laatste notitie van de
   vorige bouwer). i-h0 en q-a0 geven ref PASS en UNSAT op main,
   `011e1030` en de tip. Het `kvr`-predicaat van fixronde 2 kan de andere
   hekken in de matrix dragen, maar dat verschuift hekken in bestaande
   corpusprogramma's: samen met punt 3 in een eigen kaart.
