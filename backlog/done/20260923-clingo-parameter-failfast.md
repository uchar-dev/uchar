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
evaluatie de lezing bereikt. Waar een creatie of feitcreatie leden aan een
vouwbereik toevoegt, is hun volgorde niet genormeerd (ch. 3.9a/5.7a), en
refs eigen volgorde is dat evenmin: leest elke toegestane volgorde de
parameter, dan vuurt de garde in refs woorden; leest de ene toegestane
volgorde hem wel en een andere niet, dan weigert clingo in een zin die de
ongenormeerde volgorde noemt, ook waar ref speelt (maintainerbesluit
2026-09-23, gehandhaafd in fixronde 4); leest geen enkele volgorde hem,
dan vuurt er niets, behalve waar twee vouwen over één lijst elk een eigen
volgordemodel hebben ([N4]). Voor een Let-barrière is sinds fixronde 5
ook de pass vóór een creatie zo'n toegestane volgorde (besluit van de
hoofdsessie): ref draait in elke pass eerst de regels en dan de creaties,
dus in die pass ontbreekt het gecreëerde lid, en ook de barrière die
alleen dat lid opwerpt. Waar de garde verder niet exact is, wijkt ze af in
één van twee vastgelegde richtingen (zie "Twee richtingen" hieronder):
luid, een weigering waar ref speelt, op twee plaatsen (de lezing in de
vertaling, en de pass vóór een creatie in een regel die ch. 6.3 boven die
creatie zet), en stil, een antwoord waar ref faalt, op twee (de
Let-verlaging, en de tussentijdse pass binnen één stratum). De takhoist
([S2] in fixronde 4) is sinds fixronde 5 exact. Elke post heeft een frame
in de suite dat rood wordt zodra de post niet meer klopt. De eerste versie
van dit Result schreef "speelt waar ref speelt" zonder uitzondering, de
tweede "faalt waar ref faalt" zonder uitzondering, de derde rekende op een
model van refs volgorde dat over strata heen niet klopte, en de vierde
telde in de familie "in elke volgorde" een lezing door een gecreëerd lid
mee en kende de tussentijdse pass niet. De lens weerlegde alle vier; de
vijf fixrondes hieronder leggen vast wat er gerepareerd is en wat niet.
Gemeten op 88 corpusvarianten (88/88 in overeenstemming), op 66
proefdocumenten van de eerste ronde, 29 van de tweede fixronde, 54 van de
derde, de 87 van de lens van ronde 3, 19 eigen van de vierde, de 145 van
de lens van ronde 4 (haar 124, mijn 19 van de vierde en haar twee
nr-case-controles) en 23 eigen van de vijfde, en in de suite (947
checks). De corpusvertaling is byte-identiek aan main, op de twee rode
cases van deze kaart na. De Drools-lijstregel voor gate 8 staat als
voorstel in commit `89f4f660` (beschermd pad); daarmee is gate 8 groen
(50 rode cases).

Commits (hashes na de rebase op main `6e67ad8f`): `87313da5` (claim),
`15eaffd4` (de gedeelde fail-fast met suite, README en de gedeelde
diagnose), `e28caeb9` (de dode datumlezingen, item van de hoofdsessie),
`84689225` (rode corpusdrager), `6df7f44b` (kaart naar done),
`bae2f5c0` (venstercommentaar, tweede item van de hoofdsessie),
`e2952c83` (Result), `89f4f660` (gate-8-regel, voorstel beschermd pad),
fixronde 1: `42588f6e`, `4578fea3`, `325568da`, fixronde 2: `bbdffca9`,
`592fc479`, `130c90e3`, `2fb49933`, `0f712e4e` (Result), fixronde 3:
`e0e43028` (schaduwing), `10ed80d7` (gesloten lichaam), `b1e7ba14`
(ledenvolgorde: barrière per lid en creatievolgorde), `5fc17565`
(teksten), `b499f2d3` (Result), fixronde 4: `036508e4` (geen model van
refs volgorde), `28b62345` (payloadhernoeming), `eb2adb45` (de tak uit
een Let-waarde, en de vastgepinde afwijkingen), `dd1d36d3` (teksten),
`a51a3ebe` (Result), fixronde 5 (niet gerebased, basis blijft main
`6e67ad8f`): `6a500c85` (de pass vóór een creatie), `cc0be1e2` ([S3]),
`17863dc6` ([N4]), `865fe21e` (de takhoist), `cf084787` ([L1] voor de
dagsoorttabel), `36f73e73` (README en commentaar: [S2] met een datum in
plaats van het kaartwoord), en de commit van dit Result.

### Twee richtingen (stand na fixronde 5)

Elke post draagt het label van haar post in `backends/clingo/README.md`
en heeft een frame in de suite, `README [label]`, dat het huidige gedrag
pint en rood wordt zodra het verdwijnt (het overleverspatroon van gate
7). Gemeten met ref en clingo op main `6e67ad8f` en op de tip.

Benoemd volgens het besluit (geen afwijking, wel gepind):
- **[N1]** cr-8-3-cond: twee creaties van 8 en 3 per werf, elk achter een
  voorwaarde die geldt (nul = 0), matrix `TEN MINSTE 1 [lengte > 5,
  lengte > p]`: ref `PASS` (draait de creaties in documentvolgorde, leest
  8 en stopt), clingo UNSAT met de benoemde zin ("the quantifier fold");
  [3, 8] leest p.
- **[N2]** pm-cr-8-3-cond: dezelfde creaties onder `Let k = restpost in
  som(lengte van schuiten die TEN MINSTE 1 [ALLE[lengte > 5, k > 0],
  lengte > p])`, W1 zonder schuiten in de case: ref `PASS` (de barrière
  bij 8 beëindigt de Let), clingo UNSAT met de benoemde zin ("the Let
  "k"").
- **[N3]**, de woorden: disj-8-3, twee kwantorvouwen over dezelfde
  gecreëerde 8 en 3 onder `Let k = p`, de ene matrix leest k bij 3 als 3
  eerst komt, de andere bij 8 als 8 eerst komt. Elke toegestane volgorde
  leest p; ref faalt. clingo weigert benoemd, waar refs woorden passen:
  "elke toegestane volgorde" wordt per lezing beslist. Op main `PASS`, op
  `b499f2d3` UNSAT in refs woorden (via het oude model).
- **[N4]**, de woorden en de uitkomst (fixronde 5, lenspunt 3): nr-1, W1
  met twee creaties van 8 en 3 per werf, `keur 99 indien TEN MINSTE 1
  schuiten [TEN MINSTE 1 [lengte > 5, TEN MINSTE 1 van de schuiten van
  zijn werf [TEN MINSTE 1 [lengte < 5, lengte > p]]]]`. In [8, 3] sluit de
  buitenste vouw bij 8, in [3, 8] de binnenste bij 3; geen volgorde leest
  p en ref `PASS`. clingo weigert benoemd ("the quantifier fold"): de
  tweeling voegt "de buitenste vouw leest 3 in een toegestane volgorde"
  ([3, 8]) samen met "de binnenste leest 8 in een toegestane volgorde"
  ([8, 3]). Dit is een weigering waar volgens het besluit niets hoort te
  vuren; de post staat hier omdat de zin de ongenormeerde volgorde noemt.
  Controles nr-case-8-3 en nr-case-3-8 (de twee schuiten in de case, in
  beide volgordes): beide spelen. Op main `PASS`, op `a51a3ebe` benoemd.

Luid, een weigering waar ref speelt:
1. **[L1] De dagsoorttabel en het horizonhek** lezen hun parameter in de
   vertaling en weigeren daar, ook achter een regelconditie die faalt
   (open punt 4). Getuigen hf-cond, het horizonhekframe van de suite met
   voorwaarde 1 > 2, en sinds fixronde 5 dk-cond, de dagsoorttabel met
   "de dag later is dan de grensdatum", grensdatum ongebonden en dezelfde
   voorwaarde: ref `PASS` (keurlast en uitkomst leeg), clingo `TRANSLATION
   FAULT: parameter "…" is unbound …` (main weigerde daar al, met de
   hekzin en de tabelzin).
2. **[L2] De pass vóór een creatie in een regel boven die creatie**
   (fixronde 5, gevolg van punt 1). Een Let-barrière die alleen een
   gecreëerd lid opwerpt, telt de pass vóór de creatie ook in een regel
   die ch. 6.3 boven die creatie zet, waar ref zo'n pass niet heeft; de
   vertaler kent de strata niet (open punt 13). Getuige tr-strat-1: W1,
   één schuit van 8 per Werf, `bonus = Let k = restpost in (som(lengte
   van schuiten die k > 0) plus p)` onder de conditie `ALLE schuiten
   [lengte > 0]`, restpost leeg: ref `PASS` (bonus leeg), clingo weigert
   benoemd ("the Let "k""). Op main en `a51a3ebe` `PASS`.

De vroegere luide posten 2 (de creatievolgorde) en 3 (de Let-barrière
per lid over gecreëerde leden) zijn sinds fixronde 4 het besluit zelf:
[N1] en [N2]. De luide post die de lens in ronde 3 vond (cl-capture-ww,
een armpayload die de Let-keten vangt) is gerepareerd.

Stil, een antwoord waar ref faalt:
1. **[S1] De Let-verlaging** houdt een waarde achter een omsluitende
   behoefte op een pad dat haar niet leest, en de garde volgt de
   verlaging (open punt 5). Getuige lz-lv-ctl: `Let k = restpost in Let
   bel = (als q > 0 dan k anders 0) in ((1,00 plus bel) plus p)`, q =
   -5, restpost leeg, p ongebonden: ref faalt bij p, clingo antwoordt
   heffing leeg, op de tip en op main. De getuige van fixronde 3, lz-v-ctl
   (`… in (bel plus p)`, bel het eerste wat het lichaam evalueert), is
   sinds fixronde 4 exact.
2. **[S3] De tussentijdse pass binnen één stratum** (fixronde 5, lenspunt
   2; het residu van ch. 6.1 in fail-fast-vorm). ref draait de vormen van
   een stratum pass voor pass; een vorm die leest wat een latere vorm van
   hetzelfde stratum schrijft, ziet dat in de eerste pass leeg. Valt
   daardoor een kortsluiting weg, dan leest ref de parameter en faalt,
   terwijl het fixpunt hem niet leest; clingo antwoordt het fixpunt.
   Getuige t1: `heffing = 5,00 indien TEN MINSTE 1 [a > 0, p > 0]`,
   geschreven vóór `a = inkomen`, inkomen 20, p ongebonden: ref faalt,
   clingo heffing 5,00; controle t1b (de schrijvende regel eerst): beide
   5,00. De schrijver kan ook een creatie zijn, getuige tr-of-1: `keur =
   99 indien TEN MINSTE 1 [TEN MINSTE 1 schuiten [lengte > 5], p > 0]`,
   één schuit van 8 per Werf: ref faalt in de pass vóór de creatie,
   clingo keur 99; controle tr-of-case (de schuit in de case): beide 99.
   Op main, `a51a3ebe` en de tip gelijk. Voor een Let-barrière geeft die
   pass sinds punt 1 de benoemde weigering; voor een kortsluiting blijft
   ze stil (open punt 14).

[S2], de takhoist die de conditie van de tak vóór de operand ernaast las,
is sinds fixronde 5 exact (punt 4): hoist-ctl en h-diep weigeren in refs
woorden.

Vastgelegde beperking zonder parameter (README, "Recorded limitations"):
- **[V1]** lz-m3-bound-laag: `Let k = restpost in ((match klasse: Hoog →
  k, Laag → 0,00) plus p)`, Laag, restpost leeg, p = 10: ref 10,00,
  clingo leeg, ook op main.
- **[V2]** lz-lv-bound: de vorm van [S1] met p = 10: ref 11,00, clingo
  leeg. lz-v-bound (bel eerst) geeft sinds fixronde 4 in beide 10,00; op
  main leeg.

### (a) Inventaris

Elke match op de parameterconstructor in `translate.ml`
(regelnummers van de tip na fixronde 4):

| r. | functie | aard | stand na de kaart |
|----|---------|------|-------------------|
| 518 | `eff_unit` | statisch: eenheid uit de declaratie | geen waardelezing |
| 848/891 | `walk_order` / `cond_param_names` | statisch: evaluatievolgorde van een Let-lichaam (sinds fixronde 2; `params_before_binder` is weg) | geen waardelezing |
| 1193/1226 | `expr_reaches` / `demand_for` | statisch: kan een Let-waarde een ongebonden parameter lezen | geen waardelezing |
| 2689 | `closed_let_chain` | statisch: Let-keten boven een Match in een gesloten lichaam (fixronde 3; de payloadvoorwaarden sinds fixronde 4 weg) | geen waardelezing |
| 1790/1828 | `provably_unitless` / `provably_pure_pct` | statisch: eenheidsbewijs | geen waardelezing |
| **2743** | `expr_term` | DE scalaire lezing (`param/2`) | **draagt de gedeelde garde** |
| 3294 | `expr_term` (`filter_reads` van het horizonhek) | statisch: vormtoets | geen waardelezing |
| 4116 | `expr_term` (TemporalRead) | granulaire parameter, punt- of vintagelezing | faalde al in refs woorden (`needs its timeline in the case`); ongewijzigd |
| 5331 | `whole_period_fails` | vormtoets op de grens | leest via `expr_term` (garde), nu met het onderwerp van de aanroeper |
| **5487** | `dk_value` | dagsoorttabel, geëvalueerd in de vertaling | vertaalfout, nu met de gedeelde zin |
| 7090 | `list_source` | lijstparameter (`plist`) | afwezig is leeg, in beide uitvoerders; vrijgesteld |
| **7387** | `closed_term` | scalaire lezing in een gesloten body | **draagt de gedeelde garde** |
| 7809 | `value_condition` | statisch: WF-077-vormtoets | geen waardelezing |

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
   `bae2f5c0`, alleen commentaar). Het commentaar bij
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

### Voorstel beschermd pad (gecommit als voorstel in `89f4f660`)

`scripts/check-rood.sh`, Drools-uitsluitingslijst, één regel,
alfabetisch vóór `ambiguous-case-role-key`:
```
aanvoergeld-parameter-ongebonden|NOFAULT measured 2026-09-23 (card clingo-parameter-failfast): both cases run to a model. The backend answers a parameter the case does not bind the way the ASP backend did before that card — case1 aanvoergeld 310.00 / 87.40 / 0.00, the absent tarief coalesced to 0; case2 aanvoerplichtig absent on J1 and J3 — where ref and clingo fault fail-fast in one sentence (ch. 5.7). Listed rather than fragment-pinned because this leg does not fault here at all; the day the backend binds parameters fail-fast, this entry goes stale and the gate says so
```

### Fixronde 1 (vorige bouwer, 2026-09-23)

De lens vond buiten het corpus 17 verifier-schone documenten waar ref en
clingo op main spelen en de branch UNSAT gaf: de garde vuurde waar ref
de parameter nooit leest. Drie commits van de vorige bouwer:

- `42588f6e`: de rechtvaardiging van een vorm-a-default krijgt de `not
  U_any` die haar gevolg al had (lensklasse 1; getuigen d-e, d-l, d-i).
- `4578fea3`: de Let-behoefte in de garde hangt per IR-knoop aan het pad
  in plaats van per parameternaam (lenspunt 4; getuigen lz-1, lz-3,
  lz-m1). De opdracht van fixronde 2 noemde deze commit lenspunt 6. Hij
  wijzigt alleen de garde: `rule_guard` en de andere hekken zijn in geen
  van de drie commits aangeraakt, en de sweep hieronder vindt geen
  corpusprogramma dat door een van de fixrondes verandert.
- `325568da`: een parameter in een Let-waarde wacht op een vraagpredicaat
  dat bij elke lezing van de binder wordt afgeleid (lensklasse 3;
  getuigen iv-c, iv-d, lv-2, lv-4, lv-5).

Gecontroleerd in fixronde 2: de suite van `325568da` tegen de vertaler van
`89f4f660`, daarna teruggezet met `cmp` identiek:
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
Op `325568da` zelf: `test_clingo: 735 checks, 0 failures`. De drie commits
doen wat hun boodschap zegt. De klasse van lenspunt 4 bleef wel
bereikbaar, en de Let-analyse had ook een stil gat (fixronde 2, punt 4).

### Fixronde 2 (2026-09-23)

Proeven onder /tmp (niet in de repo), elk verifier-schoon, gedraaid met
ref, clingo op main `e7e9e178`, op `325568da` en op de tip. "fout" is
refs `EVALUATION FAULT: parameter "p" is unbound …`, "UNSAT" is de garde.

**Punt 2, de kwantorvouw (`bbdffca9`).** ch. 3.9a zet de leden eerst vast,
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

| proef | vorm | ref | main | 325568da | tip |
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
gematerialiseerde lijst op label). Rood eerst op `325568da`:
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
Mutaties op `bbdffca9`, elk teruggezet met `cmp` identiek: MK1, de garde
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

| proef | leden | ref | main | 325568da | tip |
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

**Punt 4, de Let-analyse (`592fc479`, `130c90e3`).** Na `4578fea3` was de
klasse nog bereikbaar, en het oordeel "de binder wordt zeker eerder
gelezen" was ook stil (restpost leeg, p ongebonden):

| proef | vorm | ref | main | 325568da | tip |
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
`130c90e3`: `592fc479` hield een deel dat per lid draait (filter en
argumenten van een setvouw, filter en matrix van een kwantorvouw)
ongeordend, en maakte daarmee de gewone vorm `(k plus lengte) groter dan
p` in een filter UNSAT waar ref speelt. Zo'n deel is nu geordend zoals
één lid het uitvoert.

Rood eerst, de suite van `592fc479` tegen de vertaler van `bbdffca9`,
teruggezet met `cmp` identiek:
```
FAIL Let, lazy: the Match arm read k before p (lz-m3): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: the taken arm read k before p, a later operand again (lz-c): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let, lazy: each path reads k in one of two branches before p (lz-d): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
FAIL Let: an inner barrier ends before k is read, so p is (lz-s2): clingo fires the shared fail-fast (clingo answered heffing leeg: the guard waited for k's need, which the inner Let never read)
FAIL Let: a value's inner barrier ends before k is read, so p is (lz-s): clingo fires the shared fail-fast (clingo answered heffing leeg: the guard waited for k's need, which the value never read)
test_clingo: 781 checks, 5 FAILURES
```
Het frame van `130c90e3` op `592fc479`:
```
FAIL Let, lazy: each member's filter reads k before p (per member): clingo pins the values (UNSATISFIABLE — a fail-fast guard fired)
test_clingo: 785 checks, 1 FAILURES
```
Mutaties op `130c90e3`, elk teruggezet met `cmp` identiek: ML1, geen
barrière-atoom, elke behoefte uit de garde: `785 checks, 17 FAILURES`
(alle luie Let-frames van beide fixrondes). ML2, de behoeften blijven in
de garde en er is geen barrière-atoom: `11 FAILURES` (de ongebonden
tweelingen en controles van de Let-frames, waar ref p leest; lz-s en
lz-s2 inbegrepen). ML3, een lezing
binnen de waarde van een geneste Let telt nergens: `1 FAILURES` (de
controle van lz-s). Wat de garde hier niet exact maakt, staat in open
punten 5 en 8.

**Punt 5, de teksten (`2fb49933` en dit Result).** README, het
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
zero" …)` op main, op `325568da` en op de tip. Open punt 9.

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
`325568da` tegen de tip: `diff -r` leeg over alle 644 cases. Geen
corpusprogramma verandert dus door een van de fixrondes, `4578fea3`
inbegrepen.

**Gates (fixronde 2, elk los na `eval $(opam env)`, op `2fb49933`).**
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
clingo gedraaid op main `7d97353e`, op `0f712e4e` (de tip vóór deze
ronde) en op de tip. Op twee na zijn ze verifier-schoon; de twee vuile
(een kwantorvouw als conditie van een waarde-IfThenElse) tellen niet mee.
"fout" is refs `EVALUATION FAULT: parameter "p" is unbound …`, "UNSAT" de
garde in refs woorden, "benoemd" de nieuwe weigering van punt 4.

**Punt 1, schaduwing (`e0e43028`).** De garde sloeg bij een lezing van
een binder elke omsluitende Let over die dezelfde NAAM bond (`reach_at`
vergeleek `Some li.li_binder = skip`). Een binnenste Let met de naam van
een buitenste nam zo diens barrière mee. Nu kent een lezing de Let die ze
leest bij identiteit: `skip` is een `let_info`, `note_binder_read` vindt
de Let via de binding die de scope oplevert en leidt diens vraag af
(voorheen via een naamopzoeking in `ctx.demands`), en `read_places` geeft
de Let door in plaats van zijn naam. Restpost en extra leeg, p ongebonden:

| proef | vorm | ref | main | 0f712e4e | tip |
|---|---|---|---|---|---|
| sh-4 | `Let k = restpost in (k plus (Let k = p in k))` | speelt | speelt | UNSAT | speelt |
| sh-4-ctl | idem, restpost 1 | fout | MISMATCH | UNSAT | UNSAT |
| sh-5 | `Let k = restpost in (k plus (Let k = (Let k = p in k) in k))` | speelt | speelt | UNSAT | speelt |
| sh-5b | `Let k = restpost in (k plus (Let j = 1 in (Let k = p in (k plus j))))` | speelt | speelt | UNSAT | speelt |
| sh-7 | `Let x = restpost in ((Let k = extra in (k plus (Let k = x in k))) plus p)` | fout | speelt | **speelt** | UNSAT |
| sh-7b | idem, de binnenste binder heet m | fout | speelt | UNSAT | UNSAT |
| sh-7-ctl | sh-7 met extra 1 | speelt | speelt | speelt | speelt |
| id-payload | `Let k = p in (match optie: Aanwezig k → k, Afwezig → 0)` | speelt | speelt | speelt | speelt |

**Punt 3, gesloten lichaam (`10ed80d7`).** Het vraagpredicaat van
`325568da` werkt op regelniveau; `body_branches` vertaalde de waarde van
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

| proef | lambda | vangsten | ref | main | 0f712e4e | tip |
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

**Punt 2, een deel dat per lid draait (`b1e7ba14`).** Fixronde 2 ordende
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

| proef | filter van `Let k = restpost in som(lengte van schuiten die …)` | ref | main | 0f712e4e | tip |
|---|---|---|---|---|---|
| pm-over-1 | `GEEN[ALLE[lengte > 5, lengte > p], k > 0]` | speelt | speelt | UNSAT | speelt |
| pm-over-1-ctl | idem, restpost 1 | fout | speelt | UNSAT | UNSAT |
| pm-silent-8 | `TEN MINSTE 1 [ALLE[lengte > 5, k > 0], lengte > p]` | fout | speelt | **speelt** | UNSAT |
| pm-silent-8-rev | idem over [S2, S1] | speelt | speelt | speelt | speelt |
| pm-nest-a | kwantorvouw ALLE over de ladingen van elke schuit met matrix `TEN MINSTE 1 [ALLE[gewicht > 5, k > 0], gewicht > p]`; L1 8, L2 3 | speelt | speelt | speelt | speelt |
| pm-nest-b | idem, L1 3, L2 8 | fout | speelt | **speelt** | UNSAT |
| late-kv | `(som(… die ALLE ladingen [k > 0])) plus p`, L1 8 | speelt | speelt | UNSAT | speelt |
| late-kv-ctl | idem, restpost 1 | fout | MISMATCH | UNSAT | UNSAT |

**Punt 4, gecreëerde leden (`b1e7ba14`), maintainerbesluit.** Leden die
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
opgaat een garde zou stilzetten waar ref faalt. **[Weerlegd door de lens
van ronde 3: ref draait per stratum een fixpunt, en een creatie met een
hogere index kan eerst draaien. Precies zo'n claim zette de garde stil
waar ref faalt; `roN_r` is sinds fixronde 4 weg.]** `emit_kv_reach` telt
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
volgorde (`kvrN_o`): luid, nooit stil. **[Beide weerlegd: het voorvoegsel
volgens `roN_r` was niet het kleinste dat ref kan hebben, en de vraag was
stil op stratum-vraag-8-w-3dd. Zie fixronde 4.]** Matrix `TEN MINSTE 1 [lengte > 5,
lengte > p]` tenzij anders vermeld, creaties per werf in documentvolgorde:

| proef | leden | ref | main | 0f712e4e | tip |
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

**Punt 5, de teksten (`5fc17565` en dit Result).** README en het
commentaar bij `unbound_param_guard` noemen de afwijkingen nu per
richting, elk met een getuige (zie "Twee richtingen"). Het commentaar bij
`reach_at` zegt waar de stille kant vandaan komt. De kop van de suite
verwijst al naar de README.

**Suite** 785 → 841 checks: 28 frames, waarvan 14 getuigen en 14
controles. Rood eerst, de eindsuite tegen de vertaler van `0f712e4e`
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

**Gates (fixronde 3, elk los na `eval $(opam env)`, op `5fc17565`).** 1
self-contained PASS · 2 ir-schema PASS (286 bestanden, vloer 192) · 3
verifier PASS (286 schoon, 454 negatief, vloer 350) · 4 case-schema PASS
(658) · 5 oracle PASS (ref-suite 931, vloer 489; 608 cases, vloer 427) ·
6 clingo PASS (suite 841, vloer 83; 608 cases) · 8 rood PASS (50 rode
cases, vloer 26; Drools 11 fault-pinned, 39 not-carried; 100 fragmenten
zonder bodemzin).

### Fixronde 4 (lensronde 3, 2026-09-23)

De lens toetste de tip `b499f2d3` met 87 eigen proefdocumenten, elk
verifier-schoon (onder /tmp, niet in de repo). Eén punt was blokkerend,
vier waren advies, en de normvraag kreeg een bevinding. Mijn eigen
proeven (19, verifier-schoon, onder /tmp) staan per punt. Elke proef is
gedraaid met ref, met de clingo-runner van main `6e67ad8f`, met die van
`b499f2d3` (hieronder "vorige") en met die van de tip. "fout" is refs
`EVALUATION FAULT: parameter "p" is unbound …`, "UNSAT" de garde in refs
woorden, "benoemd" de zin van het besluit, met tussen haakjes wat de zin
noemt.

**Punt 1, geen model van refs volgorde (`036508e4`), blokkerend.** Besluit
van de hoofdsessie, handhaving van het maintainerbesluit: gecreëerde
vouwleden krijgen de letterlijke regel. Waar de volgorde van gecreëerde
links de uitkomst van de garde kan bepalen, weigert clingo benoemd,
ongeacht welke volgorde ref feitelijk aanhoudt. De lens toonde dat het
model van refs volgorde (`roN_r`: caselinks eerst, creaties in
documentvolgorde) over strata heen niet klopt. ref draait per stratum een
fixpunt. Een creatie "per Werf" staat boven de creatie die Werven maakt,
terwijl een creatie "per Droogdok" (subtype van Werf) in stratum 0
blijft, dus ref draait dan de creatie met de hogere index eerst. Drie
paden leunden op dat model en gaven stil een antwoord: de benoemde
tweeling (`emit_kv_reach`), de barrière per lid (`emit_barriers`) en de
vraag van een Let-waarde (`note_binder_read` via `kvrN_o`).

Uitvoering. `ro_created` en `roN_r` zijn weg. De enige volgorderelatie is
`roN_n`, "X gaat vóór Y in elke toegestane volgorde": de caselinks
onderling. Elk gardeatoom dat van de volgorde afhangt, heeft twee vormen
in `ctx.named`: het atoom zelf zegt "in elke toegestane volgorde", de
andere vorm "in een toegestane volgorde".
- Kwantormatrix: `kvrN` is open bij het grootste voorvoegsel (alle
  bijdragers die vóór X kunnen staan), `kvrN_o` bij het kleinste (alleen
  de bijdragers die elke toegestane volgorde vóór X zet: `roN_n(Y,X)`).
  Een uitkomst die vaststaat, blijft vaststaan als het voorvoegsel groeit.
  Open bij het grootste betekent dus dat elke volgorde X leest, en vast
  bij het kleinste dat geen enkele het doet. De caselinks staan vanaf het
  begin in het bereik, dus ook een eerdere pass van ref heeft minstens dat
  voorvoegsel.
- Barrière per lid: in een document met creaties heeft elke vraag twee
  families regels. `lbg…_c` betekent "gebarrièreerd in elke toegestane
  volgorde": een lezing die elke volgorde bereikt, door een lid dat elke
  volgorde eerst zet (`roN_n`). `lbg…` betekent "in een toegestane
  volgorde": een lezing die een volgorde bereikt, door een lid dat een
  volgorde eerst zet (`not roN_n(P,gardelid,leeslid)`). De garde leest
  `not lbg…`, de benoemde tweeling `not lbg…_c`.
- Vraag van een Let-waarde: `dmdN` wordt afgeleid uit de elke-volgordevorm
  van het bereik van een lezing van de binder, `dmdN_o` uit de
  een-volgordevorm, en `dmdN_q` is het verschil.

De garde in refs woorden gebruikt de elke-volgordevormen. De benoemde
tweelingen dekken de rest, één regel per ordeafhankelijk atoom,
gesplitst op het eerste atoom dat niet in elke volgorde geldt, zodat ze
elkaar en de garde niet overlappen. De zin noemt "the quantifier fold
"…"" of "the Let "k"". In een document zonder creaties schrijft de
vertaler geen van de tweede vormen. Proeven van de lens:

| proef | ref | main | vorige | tip |
|---|---|---|---|---|
| stratum-8-w-3dd | fout | speelt | **speelt** | benoemd (fold) |
| stratum-alle-3-w-8dd | fout | speelt | **speelt** | benoemd (fold) |
| stratum-pm2-8-w-3dd | fout | speelt | **speelt** | benoemd (Let) |
| stratum-vraag-8-w-3dd | fout | speelt | **speelt** | benoemd (Let) |
| stratum-pm-8-w-3dd | fout | speelt | **speelt** | benoemd (Let) |
| stratum-8-3dd-zonder-werfcreatie | speelt | speelt | speelt | benoemd (fold) |
| stratum-pm2-8-3dd-zonder-werfcreatie | speelt | speelt | speelt | benoemd (Let) |
| stratum-vraag-8-3dd-zonder-werfcreatie | speelt | speelt | speelt | benoemd (Let) |
| stratum-ctl-nietliteral | fout | speelt | benoemd | benoemd (fold) |
| de drie -gebonden | speelt | speelt | speelt | speelt |

Door het besluit gaan ook lensproeven waar ref speelt van "speelt" naar
"benoemd": e4d, e5a, e5n-nest-8-3, e5n-nest-3-8-casewerf8, e6a, e6d, e7a,
e7c, ijk-cr-8-3, keten-8-w-3 en keten-8-w-3-met-w1. Ik heb ze elk
nagelopen: in elk leest de ene toegestane volgorde p en de andere niet.
e5b, e6b en e6e (ref faalt) gaan van UNSAT naar benoemd. In de suite
draaien cr-8-3 en cr-case8-new3 om (van "beide spelen" naar "benoemd,
ref speelt"). Het frame-hulpmiddel `order_named` is veralgemeend tot
`named_refusal ~by ~ref_plays`. De controles uit de opdracht:
"-zonder-werfcreatie" en stratum-ctl-nietliteral zoals gemeten
(benoemd), "-gebonden" als `both_answer`.

Rood eerst, de eindsuite tegen de vertaler van `b499f2d3`, daarna
teruggezet met `cmp` identiek:
```
FAIL created members: 8 then 3, ref's order stops at 8, [3, 8] reads p (cr-8-3): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL created members: the case's 8 before a created 3 in ref's order, not in every one (cr-case8-new3): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL stratum: at-least 1, 8 for each Werf and 3 for each Droogdok, ref reads 3 first (stratum-8-w-3dd): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL stratum: ALLE, 3 for each Werf and 8 for each Droogdok, ref reads 8 first (stratum-alle-3-w-8dd): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL stratum: without the werf creation ref reads 8 first and plays (stratum-8-3dd-zonder-werfcreatie, control): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL stratum: a Let barrier per member, ref reads 3 first (stratum-pm2-8-w-3dd): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL stratum: the barrier per member without the werf creation, ref's barrier at 8 (stratum-pm2-8-3dd-zonder-werfcreatie, control): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL stratum: a Let value read in a matrix, ref reads 3 first (stratum-vraag-8-w-3dd): clingo refuses in the named sentence (clingo answered the case's rows)
FAIL stratum: the Let value without the werf creation, ref's 8 settles the matrix (stratum-vraag-8-3dd-zonder-werfcreatie, control): clingo refuses in the named sentence (clingo answered the case's rows)
test_clingo: 863 checks, 9 FAILURES
```
Mutatieproef M-doc, het documentvolgorde-paar terug: in `use_ro` voor een
dynamisch bereik van één stap de regel `roN_n(P,oc_i(P),oc_j(P)) :-
roN_m(P,oc_i(P)), roN_m(P,oc_j(P))` voor elke i < j (md5 van de suite
`2bb97c20…`): `11 FAILURES`. De vier getuigen worden rood met "clingo
answered the case's rows" (stil), daarnaast cr-3-8 en mx-alle (in refs
woorden in plaats van benoemd), cr-8-3, stratum-ctl-nietliteral en de
drie controles zonder werfcreatie.

**Punt 2, een armpayload die de keten vangt (`28b62345`), advies.**
`closed_let_chain` gaf `None` als een Let-waarde een naam las die een
armpayload bindt (of als een payload de naam van een Let van de keten
droeg). De waarde bleef dan bij de Let, en de garde vuurde voor elk
element, ook waar ref speelt (cl-capture-ww). Nu hernoemt
`unshadow_payload` bij het omwikkelen de payload. De arm bindt de payload
onder een naam die niets binnen de arm of de waarden leest, en een Let
bij het armlichaam bindt de oude naam daaraan. `closed_let_chain` laat
beide payloadvoorwaarden vallen. Een waarde is alleen zichtbaar waar p
niet gelezen wordt, en een ongelezen `k` valt al weg vóór de ketenanalyse.
Daarom lezen mijn getuigen k in de Waardeloos-arm, die niet genomen wordt,
en draagt een tweede Let j de waarde:

| proef | lambda over [5,00] | ref | main | vorige | tip |
|---|---|---|---|---|---|
| cl-capture-ww (lens) | `Let k = acc plus p in match x {Waardevol acc → acc plus k; Waardeloos → acc}` over [W, W] | speelt | MISMATCH | UNSAT | speelt |
| cl-capture-j5 | `Let k = p in Let j = acc plus 1,00 in match x {Waardevol acc → acc plus j; Waardeloos → acc plus k}` | speelt (6,00) | MISMATCH | UNSAT | speelt |
| cl-capture-j-ctl | idem over [5,00, W] | fout | MISMATCH | UNSAT | UNSAT |
| cl-shadow-j5 | `Let k = p in Let j = 1,00 in match x {Waardevol j → acc plus j; Waardeloos → acc plus k}` | speelt (5,00) | MISMATCH | UNSAT | speelt |
| cl-shadow-j-ctl | idem over [5,00, W] | fout | MISMATCH | UNSAT | UNSAT |

Rood eerst tegen de vertaler van `036508e4`: `3 FAILURES` (cl-capture-ww,
cl-capture-j5 en cl-shadow-j5, elk UNSAT waar ref speelt). Mutatieproef
M-cap1, de hernoeming uit en de voorwaarden van `closed_let_chain` ook
uit (md5 `b7d627d5…`): `2 FAILURES`, met precies de gevangen waarden:
`J1.totaal = [1100], expected 600` en `J1.totaal = [100], expected 500`.
Een eerste versie van de getuigen had gebonden tweelingen. Met p gebonden
neemt de vertaler het ketenpad niet, en de mutant bleef groen (`0
failures`). Die tweelingen zijn vervangen.

**Punt 3, de Let-verlaging (`eb2adb45`), advies.** Het "Silent"-voorbeeld
van de README (lz-m3 met p = 10) was een waardeafwijking zonder
parameter, geen stille garde. Onderzocht of die afwijking een begrensde
fix is:
- **lz-v (een tak in de waarde van een geneste Let): ja, gerepareerd.**
  In de vangarm van `hoist_branch` staat nu een waarde-arm. Call-by-need
  draait de waarde van `bel` bij de eerste lezing van `bel`. Is die
  lezing het eerste wat het lichaam evalueert (`reads_first`: de
  linkeroperand van een Binair, de basis van Afgerond en Begrensd, zoals
  ref evalueert), dan draait de conditie precies waar het lichaam begint.
  `Let bel = (als c dan T anders E) in B` wordt dan `als c dan (Let bel =
  T in B) anders (Let bel = E in B)`, en de buitenste Let komt in de arm
  die hem leest. Geen corpusprogramma heeft deze vorm: een scan van 740
  IR-bestanden vond 56 Let-knopen en geen enkele waarde die in één arm een
  buitenste binder leest.
- **lz-m3 (een Match onder een operator): nee.** De bestaande hoist van
  een IfThenElse uit een operator kan geen Match verplaatsen. ref leest
  een Match over een lege scrutinee als leeg (ch. 9.2) en evalueert de
  operator toch: lz-m3 met klasse afwezig en p = 10 geeft in ref 10,00
  (gemeten, lz-m3-klasse-leeg). De gehoiste vorm `match klasse: Hoog → (k
  plus p), Laag → (0,00 plus p)` zou daar leeg geven. De exacte verlaging
  is de behoefte per pad in plaats van op het hele lichaam, en die
  verandert de vertaling van elk Let-lichaam in het corpus (open punt 5).
- De waarde-arm eist dat `bel` het eerste is wat het lichaam evalueert.
  Een lichaam dat eerst iets anders evalueert, valt erbuiten. Ook een
  literal valt erbuiten, al zou dat exact zijn: een attribuutlezing kan
  in ref falen (een temporeel attribuut in scalaire context), dus de grens
  ligt bij "de lezing van de binder eerst". Daar blijft de stille post
  [S1]: lz-lv-ctl en lz-av-ctl (een literal of een attribuut vóór `bel`).
- Nieuw gevonden en vastgelegd als [S2]: de bestaande takhoist leest de
  conditie vóór de operand naast de tak (hoist-ctl, ook stil op main).

| proef | vorm (restpost leeg) | ref | main | vorige | tip |
|---|---|---|---|---|---|
| lz-v-bound | `Let k = restpost in Let bel = (als q > 0 dan k anders 0) in (bel plus p)`, q = -5, p = 10 | 10,00 | leeg | leeg | 10,00 |
| lz-v-ctl | idem, p ongebonden | fout | leeg | leeg | UNSAT |
| lz-v-hoog | idem, q = 5, p = 10 | leeg | leeg | leeg | leeg |
| lz-lv-bound | `… in ((1,00 plus bel) plus p)`, p = 10 | 11,00 | leeg | leeg | leeg [V2] |
| lz-lv-ctl | idem, p ongebonden | fout | leeg | leeg | leeg [S1] |
| lz-av-bound / -ctl | `… in ((inkomen plus bel) plus p)` | 30,00 / fout | leeg | leeg | leeg |
| lz-pv-bound / -ctl | `… in (p plus bel)` | 10,00 / fout | leeg | leeg / UNSAT | leeg / UNSAT |
| lz-mv-bound-laag / -ctl | `Let bel = (match klasse: Hoog → k, Laag → 0,00) in (bel plus p)`, Laag | 10,00 / fout | leeg | leeg / UNSAT | leeg / UNSAT |
| lz-m3-bound-laag | `(match klasse: Hoog → k, Laag → 0,00) plus p`, Laag, p = 10 | 10,00 | leeg | leeg | leeg [V1] |
| lz-m3-klasse-leeg | idem, klasse afwezig | 10,00 | leeg | leeg | leeg |
| hoist-ctl | `Let n = restpost in Let k = extra in (p plus (als n > 0 dan k anders 0))`, extra 1, p ongebonden | fout | leeg | leeg | leeg [S2] |
| hoist-bound | idem, p = 10 | leeg | leeg | leeg | leeg |
| hoist-rechts-bound | `Let k = extra in (p plus (als q > 0 dan k anders 0))`, extra leeg, q = -5, p = 10 | 10,00 | 10,00 | 10,00 | 10,00 |

Rood eerst tegen de vertaler van `28b62345`: `2 FAILURES` (lz-v-bound:
`J1.heffing is underived (leeg), expected 1000`; lz-v-ctl: clingo
antwoordt heffing leeg). Mutatieproef M-val, de waarde-arm uit (md5
`e9fe083b…`): dezelfde `2 FAILURES`.

**Punt 4, de lijsten gepind (`eb2adb45`), advies.** Elke post van de twee
lijsten en van de vastgelegde beperking heeft nu een frame met het label
van zijn README-post in de naam (`README [L1]` …): `pinned_loud` (ref
speelt, clingo weigert met de zin), `pinned_silent` (ref faalt, clingo
antwoordt precies de rijen van de case), `pinned_value` (ref speelt,
clingo antwoordt met precies de gegeven afwijkingen) en `named_refusal`
voor de posten van het besluit. Gepind: [L1] hf-cond, [S1] lz-lv-ctl,
[S2] hoist-ctl, [V1] lz-m3-bound-laag, [V2] lz-lv-bound, [N1]
cr-8-3-cond, [N2] pm-cr-8-3-cond en [N3] disj-8-3. Dat zo'n frame rood
wordt als de afwijking verdwijnt, toont mutatieproef M-lit:
`reads_first` slaat een literal links over (md5 `3b4f5c40…`). Dan
verdwijnt lz-lv, en [S1] en [V2] worden rood (`2 FAILURES`, "the
deviation is gone").

**Punt 5, de teksten (`dd1d36d3` en dit Result).** De README, het
commentaar bij `use_ro`, `emit_kv_reach`, `barred`, `emit_barriers`,
`note_binder_read`, `unbound_param_guard` en `hoist_branch`, "Twee
richtingen" en de registerzinnen zijn in overeenstemming gebracht met de
code. De zin "unconditional creations with literal initialisations and no
fact creation" is weg, en recursie wordt niet meer als voorwaarde
genoemd (recursie zonder voorwaarde is WF-033-rood). De beweringen van
fixronde 3 over `roN_r` zijn daar gemarkeerd als weerlegd.

**Punt 6, de normvraag** staat bij open punt 7.

**Suite** 841 → 899 checks. `dune test --force backends/clingo`:
`test_clingo: 899 checks, 0 failures`.

**Sweep.** Hetzelfde gereedschap als in (c), onder /tmp: een runner
gebouwd uit een `git archive` van main `6e67ad8f` en de runner van de tip
(md5 `e0b68c50…` en `61f5d515…`), over alle 658 corpuscases van de tip,
met elk aan clingo gegeven programma bewaard. 636 tegen 638 programma's.
`diff -r` vindt alleen de twee rode aanvoergeld-cases, met één garderegel
per programma, een waargenomen fout in plaats van een model en de
diagnoseherhaling als extra programma, precies als in fixronde 3. Geen
enkele fix van deze ronde verandert een corpusprogramma.

**Rebase** op main `6e67ad8f` zonder conflict: main bewoog alleen met
backlogkaarten. De hashes in dit Result zijn per commitonderwerp
omgezet.

**Gates (fixronde 4, elk los na `eval $(opam env)`, op `f146ca88`, na de
rebase `dd1d36d3`, zelfde bomen).**
```
check-clingo: PASS (backend suite 899 checks, floor 83; 608 cases replay exactly through clingo — ref = clingo over the corpus)
check-rood: PASS (50 red cases fault observably in ref and clingo, floor 26; 100 pinned fragments cite none of the 36 listed bottom sentences)
check-oracle: PASS (ref suite 931 worked examples, floor 489; 608 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-noop: PASS (571 cases refute a do-nothing executor; 37 pinned survivors pin defeasible standing)
check-verifier: PASS (286 golden clean; 454 negatief rejected for their declared rule, floor 350)
check-self-contained: PASS
```
Gate 8's Drools-been: `check-rood: Drools leg PASS (11 fault-pinned, 39
not-carried with reason)`.

### Fixronde 5 (lensronde 4, 2026-09-23)

De lens toetste de tip `a51a3ebe` met 143 proefdocumenten (124 eigen en
mijn 19 van fixronde 4; daarna nog twee nr-case-controles) en twee
prototypes in een kopie van `translate.ml`. Twee punten waren blokkerend,
drie advies; de hoofdsessie besliste per punt, en per punt staat hieronder
één commit. Niet gerebased (opdracht): main krijgt binnenkort de
M1-landing (v1-wire), en de rebase met de migratie van de ingebedde
v0-fixtures volgt in een eigen ronde. Mijn eigen proeven (23,
verifier-schoon, onder /tmp) staan per punt. Elke proef is gedraaid met
ref, met de clingo-runner van main `6e67ad8f` (md5 `e0b68c50…`), met die
van `a51a3ebe` ("vorige", `61f5d515…`), met die van het lensprototype
(`c8bc19dc…`) en met die van de tip (`4323a72e…`). "fout" is refs
`EVALUATION FAULT: parameter "p" is unbound …`, "UNSAT" de garde in refs
woorden, "benoemd" de zin van het besluit, "antwoordt" een model zonder
garde.

**Punt 1, de pass vóór een creatie (`6a500c85`), blokkerend.** De lens:
in een document met creaties telde de familie "in elke toegestane
volgorde" van `emit_barriers` (`…_c`) een lezing door een gecreëerd lid
mee. ref draait in elke pass eerst de regels en dan de creaties
(`eval.ml` ~3729-3796). In de pass vóór de creatie is het bereik leeg,
is er geen barrière, en leest ref de parameter. Besluit van de
hoofdsessie: die pass is de ontaarde volgorde waarin het lid ontbreekt,
valt onder het maintainerbesluit over de creatievolgorde en wordt een
benoemde weigering. Uitvoering volgens het lensprototype:
`roN_q(P,X) :- roN_o(P,X,_)` in `use_ro` (alleen in een document met
creaties: de leden die in elke pass in het bereik staan, de caselinks),
en in de familie "in elke volgorde" per deel in de keten van de lezing
het atoom `roN_q(ouder,lid)`: in de lezingen (`lbr…`) op elk niveau, in
de regels per lid alleen op de niveaus onder de niveaus die de regel
gelijkstelt aan het gardelid of daarvoor ordent (dat lid is via `roN_n`
al een caselink). Eén uitbreiding op het prototype: ook de delen binnen
een Let-waarde waarin de lezing staat, tellen mee (`read_places` geeft ze
als vierde component). Het prototype miste tr-w-1 en tr-wm-1, een lezing
van k in een somfilter binnen de waarde van j.

| proef | vorm (restpost leeg, p ongebonden tenzij anders) | ref | main | vorige | prototype | tip |
|---|---|---|---|---|---|---|
| tr-cr-1 (lens) | H1, W1 (bonus 4), één schuit van 8 per Werf; `bonus = Let k = restpost in (som(bonus van werven die TEN MINSTE 1 schuiten [k > 0]) plus p)` | fout | speelt | speelt | benoemd | benoemd |
| tr-case-1 (lens) | idem, de schuit in de case | speelt | speelt | speelt | speelt | speelt |
| tr-cr-1-ctl (lens) | idem, restpost 1 | fout | antwoordt 4 | UNSAT | UNSAT | UNSAT |
| tr-w-1 | W1, één schuit van 8; `Let k = restpost in Let j = (1 plus som(lengte van schuiten die k > 0)) in (j plus p)` | fout | speelt | speelt | **speelt** | benoemd |
| tr-wm-1 | H1, W1; `Let k = restpost in Let j = (1 plus som(bonus van werven die TEN MINSTE 1 schuiten [k > 0])) in (j plus p)` | fout | speelt | speelt | **speelt** | benoemd |
| tr-pm-1 | H1 met W1 (bonus 4) en W2 (bonus 5), een schuit van 8 voor een werf met bonus < 5; `Let k = restpost in som(bonus van werven die TEN MINSTE 1 [TEN MINSTE 1 schuiten [k > 0], ALLE [bonus > 4, bonus > p]])` | fout | speelt | speelt | benoemd | benoemd |
| tr-pm2-1 | H1, W1; `… die TEN MINSTE 1 [TEN MINSTE 1 schuiten [k > 0], bonus > p]` | fout | speelt | speelt | benoemd | benoemd |
| tr-same-1 | W1; `Let k = restpost in (som(lengte van schuiten die k > 0) plus p)` | fout | speelt | speelt | benoemd | benoemd |
| tr-strat-1 | idem onder `ALLE schuiten [lengte > 0]` | speelt | speelt | speelt | benoemd | benoemd [L2] |

De controles met de schuit in de case (tr-w-case, tr-wm-case, tr-pm-case,
tr-pm2-case, tr-strat-case) spelen overal; die met restpost 1 (tr-w-ctl,
tr-wm-ctl, tr-pm-ctl, tr-strat-ctl) zijn UNSAT op vorige, prototype en
tip, en antwoorden op main. tr-pm2-ctl (restpost 1) is geen barrière maar
een kortsluiting en valt onder [S3] (punt 2).

Gevolg, vastgelegd als **[L2]**: tr-strat-1. De ALLE-conditie leest het
bereik negatief, dus ch. 6.3 zet de regel boven de creatie, en ref heeft
geen pass waarin de regel vóór de creatie draait: ref speelt. De vertaler
kent de strata niet en weigert benoemd; het prototype doet hetzelfde.
Geen van de 145 lensproeven heeft deze vorm (open punt 13).

Frames: tr-cr-1, tr-w-1, tr-pm-1 en tr-pm2-1 als `named_refusal
~ref_plays:false`, hun case-controles als `both_answer`, tr-cr-1-ctl en
tr-w-ctl als `unbound_fires`, tr-strat-1 als `pinned_loud ~entry:"L2"`
met tr-strat-case als `both_answer`. Rood eerst, de suite tegen de
vertaler van `a51a3ebe`: `test_clingo: 915 checks, 3 FAILURES` (tr-cr-1
en tr-w-1 "clingo answered the case's rows", [L2] "clingo no longer
refuses"). Tegen het lensprototype: `2 FAILURES` (tr-w-1 "clingo
answered the case's rows"; [S2] "the deviation is gone", zie punt 4).
Mutatieproeven op de eindsuite (md5 van de mutant-`translate.ml`):
M-lbr, `roN_q` uit de lezingen (`ac795023…`): `3 FAILURES` (tr-cr-1,
tr-w-1, [L2]); M-inner, de delen binnen een Let-waarde uit
(`21830930…`): `1 FAILURES` (tr-w-1); M-lvl, `roN_q` uit de regels per
lid (`1b7fd163…`): `2 FAILURES` (tr-pm-1, tr-pm2-1); M-ir, alleen uit de
regel voor hetzelfde lid op een eerdere plaats (`3bd5573a…`): `1
FAILURES` (tr-pm2-1). Van de 145 lensproeven verandert door dit punt
alleen tr-cr-1 (speelt → benoemd), zoals de lens met het prototype mat.
Teksten: de README en het commentaar bij `use_ro`, `read_places` en
`emit_barriers` noemen de pass vóór een creatie; de registerzin
hieronder ook.

**Punt 2, de tussentijdse pass binnen één stratum (`cc0be1e2`),
blokkerend, geen code (besluit).** De lens: een vorm die leest wat een
latere vorm van hetzelfde stratum schrijft, ziet dat in pass 1 leeg; valt
daardoor een kortsluiting weg, dan leest ref de parameter en faalt,
terwijl het fixpunt hem niet leest. Het is het residu van ch. 6.1
("conditions on attribute-writing forms read positively here", met de
meting "with the reader listed first") in fail-fast-vorm. Uitgevoerd:
README-post [S3], twee `pinned_silent`-frames `README [S3]` (t1, en
tr-of-1 met een creatie als schrijver) met hun controles als
`both_answer` (t1b, tr-of-case), de zin in de registerzin en open punt
14 met de normvraag. Metingen bij dit punt (proeven van de lens en
eigen), elk op main, vorige, prototype en tip gelijk:

| proef | vorm | ref | clingo |
|---|---|---|---|
| t1 (lens) | `heffing = 5,00 indien TEN MINSTE 1 [a > 0, p > 0]` vóór `a = inkomen`, inkomen 20 | fout | 5,00 |
| t1b (lens) | de schrijvende regel eerst | 5,00 | 5,00 |
| tr-of-1 | W1, één schuit van 8 per Werf; `keur = 99 indien TEN MINSTE 1 [TEN MINSTE 1 schuiten [lengte > 5], p > 0]` | fout | 99 |
| tr-of-case | de schuit in de case | 99 | 99 |
| tr-kv-1 | H1 met W1 (bonus 4, krijgt een schuit van 8) en W2 (bonus 5); `keur = 99 indien TEN MINSTE 1 werven [TEN MINSTE 1 [TEN MINSTE 1 schuiten [lengte > 5], ALLE [bonus > 4, bonus > p]]]` | fout | 99 |
| tr-pm2-ctl | de vorm van tr-pm2-1 met restpost 1 | fout | bonus 4 |

tr-kv-1 laat zien dat ook de vouw die bij een eerder lid sluit (`kvrN_o`,
"gesloten bij het kleinste voorvoegsel") de waarheid van dat lid in het
fixpunt neemt: in de pass vóór de creatie sluit W1 de vouw niet, en W2
leest p. Voor een Let-barrière geeft die pass sinds punt 1 de benoemde
weigering; voor een kortsluiting blijft ze stil. Een "in elke
pass"-waarheid van vouwen over een groeiend bereik zou een tweede
vertaling van elke conditie met zo'n vouw vragen; niet begrensd, en
volgens het besluit geen code in deze kaart.

**Punt 3, [N4] (`17863dc6`), advies.** De lens toonde [N3] omgekeerd:
nr-1 (zie [N4] onder "Twee richtingen") weigert benoemd waar geen
volgorde p leest.
Uitgevoerd: de getagde post [N4] met nr-1 als `named_refusal
~ref_plays:true` en de case-controles nr-case-8-3 en nr-case-3-8 als
`both_answer`; de README-zin "a read no allowed order reaches fires
nothing" noemt de uitzondering, de registerzin ook, en open punt 12 is
bijgesteld. Gemeten of één gedeeld volgordemodel per lijst een begrensde
fix is: nee. In het programma van nr-1 vuurt de tweeling
`kvr15_o(X1,X6), kvr14_q(S,X1)` (één garde, twee ordeafhankelijke
atomen, twee tweelingen). `kvr14` hoort bij bereik `ro1` (de schuiten van
W1, via `bronRol`), `kvr15` bij `ro3` (de schuiten van de werf van X1,
via `bronPad`). Beide relaties `ro1_n` en `ro3_n` zijn leeg op de
gecreëerde leden, dus de twee modellen verschillen niet in hun relatie;
een gedeelde relatie verandert niets. Wat verschilt, is de getuige:
`kvr14_q(W1,3)` geldt in [3, 8], `kvr15_o(3,8)` in [8, 3], en elk atoom
kiest zijn eigen getuige. Exact beslissen vraagt één getuige voor alle
atomen van een tweelingregel over dezelfde lijst. Voor TEN MINSTE 1 is
"open bij X" een verzameling voorrangsparen (X vóór elk lid waarvan de
matrix geldt), waarvan de samenhang te toetsen is. Maar voor TEN MINSTE
n > 1, TEN HOOGSTE en PRECIES is het een telling op het voorvoegsel, en
de barrière- en vraagatomen zijn afgeleide predicaten waarvan de bodies
zelf ordeafhankelijke atomen bevatten. Bovendien moet de vertaler dan
bereiken herkennen die pas tijdens de uitvoering dezelfde lijst zijn (ro1
en ro3 alleen als de werf van X1 W1 is; een meerstapsbereik is een
vereniging). De exacte route is een keuze over de permutaties per lijst
met een bestaansvraag (UNSAT als één volgorde p leest), en die verandert
het model van de runner (één stabiel model, waartegen de verwachtingen
getoetst worden). Daarom alleen de lijsten.

**Punt 4, de takhoist (`865fe21e`), advies, besluit: bouwen.** Volgens
lensprototype 1, maar met ctx-velden in plaats van globale refs:
`ctx.hoisted` houdt per takknoop die de hoist bouwt (bij identiteit) de
operanden die hij achter de conditie zette terwijl ref ze ervóór
evalueert; doorgegeven in `one_of` (de operanden vóór de lezende operand
plus die van het binnenste niveau), in de Let-distributie, in de
waarde-arm en bij de omwikkeling in `let_into_arm` (`hoist_branch` en
`let_into_arm` krijgen daarvoor `ctx`). `ctx.before_arm` houdt, zolang
een arm van zo'n knoop vertaald wordt (`translate_ite`,
`value_branches`), die operanden en de plaats van de armatomen in
`rule_guard`. Een ongebonden parameterlezing in zo'n operand krijgt in
`expr_term` een tweede garde zonder die armatomen. Eén afwijking van het
prototype: de armatomen gaan eruit op hun plaats, niet op hun tekst. Een
omsluitende tak over dezelfde binder draagt dezelfde atoomtekst
(`letN(S,LB…)`), en het prototype liet dan de vergelijking van die tak
zonder haar binder staan: clingo `error: unsafe variables in:` met
`LB3>100` zonder het atoom van n, ook waar ref speelt (h-dubbel).

| proef | vorm (extra 1, restpost leeg tenzij anders) | ref | main | vorige | prototype | tip |
|---|---|---|---|---|---|---|
| hoist-ctl | `Let n = restpost in Let k = extra in (p plus (als n > 0 dan k anders 0))` | fout | antwoordt | antwoordt | UNSAT | UNSAT |
| h-diep (lens) | `… (1,00 plus (p plus (als n > 0 dan k anders 0)))` | fout | antwoordt | antwoordt | UNSAT | UNSAT |
| h-samengesteld (lens) | `… ((p plus 1,00) plus (als …))` | fout | antwoordt | antwoordt | UNSAT | UNSAT |
| h-voor-cond-q-pos (lens) | `… ((als q > 0 dan p anders 0) plus (als n > 0 dan k anders 0))`, q = 5 | fout | antwoordt | antwoordt | UNSAT | UNSAT |
| h-voor-cond-q-neg (lens) | idem, q = -5 | speelt | speelt | speelt | speelt | speelt |
| h-na-tak (lens) | `… ((als n > 0 dan k anders 0) plus p)` | speelt | speelt | speelt | speelt | speelt |
| h-dubbel-5 | `… (als n > 1 dan (p plus (als n > 0 dan k anders 0)) anders 0)`, restpost 5 | fout | antwoordt 1,00 | UNSAT | **clingo-fout** | UNSAT |
| h-dubbel-leeg | idem, restpost leeg | speelt | speelt | speelt | **clingo-fout** | speelt |
| hoist-bound, hoist-rechts-bound, h-diep-gebonden, h-voor-cond-q-pos-gebonden, h-dubbel-5-gebonden | p gebonden | speelt | speelt | speelt | speelt | speelt |

Het frame `README [S2]` werd rood zoals bedoeld (`937 checks, 1
FAILURES`: "the deviation is gone") en is vervangen door `unbound_fires`
op hoist-ctl en h-diep en `both_answer` op h-na-tak, plus `unbound_fires`
op h-dubbel-5 en `both_answer` op h-dubbel-leeg. [S2] is uit de stille
lijst van de README en uit open punt 11. Rood eerst, de eindsuite tegen
de vertaler van `17863dc6`: `945 checks, 2 FAILURES` (hoist-ctl en
h-diep, "clingo answered heffing leeg: the hoist put p behind the
condition"). Mutatieproeven: M-text, de armatomen op tekst eruit zoals
het prototype (`7d4401de…`): `2 FAILURES` (h-dubbel-5, h-dubbel-leeg,
"clingo exited 65"); M-level, zonder de operanden van het binnenste
niveau (`4667dcfc…`): `1 FAILURES` (h-diep). De gesloten lichamen
(`body_branches`) dragen de tweede garde niet: een gesloten lichaam is
leeg-vrij (ch. 10.1), dus de conditie kiest daar altijd een arm.

**Punt 5, [L1] voor de dagsoorttabel (`cf084787`), advies.** Frame
`pinned_loud ~entry:"L1"` dk-cond: het dagsoortdocument van de suite met
"de dag later is dan de grensdatum", grensdatum ongebonden, en
regelconditie 1 > 2. ref speelt (uitkomst leeg), clingo weigert in de
vertaling met de gedeelde zin.

**Suite** 899 → 947 checks. `dune test --force backends/clingo`:
`test_clingo: 947 checks, 0 failures`.

**Sweep.** Hetzelfde gereedschap als in (c), onder /tmp: de runner van
main `6e67ad8f` (`e0b68c50…`) en die van de tip (`4323a72e…`) over alle
658 corpuscases van de tip, met elk aan clingo gegeven programma bewaard.
636 tegen 638 programma's. `diff -r` vindt alleen de twee rode
aanvoergeld-cases, met één garderegel per programma, een waargenomen fout
in plaats van een model en de diagnoseherhaling als extra programma,
precies als in fixronde 4. Tussen de runners van `a51a3ebe` en de tip is
elk corpusprogramma byte-identiek (na punt 1 en na punt 4 gemeten). De
runner is die van `865fe21e`; `cf084787` raakt alleen de suite en
`36f73e73` alleen tekst.

**Classificatie van alle proeven** (145 van de lens: haar 124, mijn 19
van fixronde 4 en haar twee nr-case-controles; 23 eigen van fixronde 5),
op de tip, naar het frame dat hun uitkomst pint. Ten opzichte van
`a51a3ebe` veranderen er vijf lensproeven: tr-cr-1 (speelt → benoemd) en
hoist-ctl, h-diep, h-samengesteld en h-voor-cond-q-pos (antwoordt →
UNSAT). Op alle 143 proeven van de lens geeft de tip dezelfde uitkomst als
het lensprototype. Een permutatieproef (ref over elke documentvolgorde van
de creaties) toetst bij de benoemde weigeringen of een volgorde p leest;
waar ref de volgorde uit strata, feitcreaties of de pass vóór de creatie
haalt, geeft die proef één uitkomst en is de proef op zich nagelopen.

| frame | aantal | proeven |
|---|---|---|
| `unbound_fires` (fout, UNSAT) | 35 | cl-capture-w5, cl-ite-hoog-ctl, e1c-cond-8uit-3, e5b-twee-cr83-g3 (ook een benoemde tweeling), e6c-letlet-extraleeg-8-3, e7d-geen-al-3-8, e7e-alle-of-8-3, e7f-geen-al-8-3, e8a-filter-p-matrix-k-8-3, e8b-filter-p-matrix-k-3-8, fc-3-anker, h-diep, h-n-gevuld, h-rechts-ctl, h-samengesteld, h-voor-cond-q-pos, init-lijst-leeg, init-lijst-twee, lam-1-ctl, lam-2-fold-eerst, lz-m3-ctl-laag-ongebonden, lz-v-ctl, pred-gevuld-neg-bereikt, pred-leeg-bereikt, tr-cr-1-ctl, cl-capture-j-ctl, cl-shadow-j-ctl, hoist-ctl, lz-mv-ctl-laag, lz-pv-ctl, h-dubbel-5, tr-pm-ctl, tr-strat-ctl, tr-w-ctl, tr-wm-ctl |
| `both_answer` (speelt, speelt) | 51 | cl-capture-ww, cl-capture-ww-gebonden, cl-dup-w5, cl-dup-ww, cl-ite-laag, cl-shadow-w5, cl-shadow-ww, e1d-cond-3uit-8, h-diep-gebonden, h-na-tak, h-voor-cond-q-neg, h-voor-cond-q-pos-gebonden, init-lijst-afwezig, keten-8-w-3-gebonden, lam-1-letbarriere, lb-3-8-3-gebonden, lz-v-bound, ms-feit-vooruit-gebonden, nr-3-gebonden, nr-case-3-8, nr-case-8-3, pm-case-3-8-gebonden, pm-case-8-3-gebonden, pred-leeg-onbereikt, stratum-8-w-3dd-gebonden, stratum-pm2-3-w-8dd-gebonden, stratum-pm2-8-w-3dd-gebonden, stratum-vraag-8-w-3dd-gebonden, t1b-of-schrijver-eerst, t1c-of-lezer-eerst-gebonden, t2b-filter-kenmerk-schrijver-eerst, t3-tak-lezer-eerst, t3b-tak-schrijver-eerst, t4b-geen-schrijver-eerst, td-1, tr-case-1, cl-capture-j5, cl-shadow-j5, disj-8-3-gebonden, hoist-bound, hoist-rechts-bound, lz-v-hoog, h-dubbel-5-gebonden, h-dubbel-leeg, tr-kv-case, tr-of-case, tr-pm-case, tr-pm2-case, tr-strat-case, tr-w-case, tr-wm-case |
| `named_refusal ~ref_plays:false` (fout, benoemd), een volgorde | 19 | e1b-cond-3-8, e2b-nietlit-3-8, e4a-feit-lig-3-8, e4c-feit-case3-lig8, e5c-twee-cr38-g8, e5n-nest-3-8, e6b-letlet-3-8, e6e-letlet-extra1-3-8, e7b-geen-of-3-8, ijk-cr-3-8, keten-3-w-8, ms-feit-vooruit, stratum-8-w-3dd, stratum-alle-3-w-8dd, stratum-ctl-nietliteral, stratum-pm-8-w-3dd, stratum-pm2-8-w-3dd, stratum-vraag-8-w-3dd, td-3-3-8 |
| idem, de pass vóór de creatie (punt 1) | 8 | tr-cr-1, tr-w-1, tr-wm-1, tr-pm-1, tr-pm2-1, tr-same-1, lb-1-8-3, lb-2-3-8 (de laatste twee ook een volgorde) |
| idem, elke volgorde leest p ([N3]) | 1 | disj-8-3 |
| `named_refusal ~ref_plays:true` (speelt, benoemd), een volgorde (het besluit) | 24 | e1a-cond-8-3, e2a-nietlit-8-3, e4b-feit-lig-8-3, e4d-feit-case8-lig3, e4e-feit-plus-cr-8-3, e5a-twee-cr83-g8, e5n-nest-3-8-casewerf8, e5n-nest-8-3, e6a-letlet-8-3, e6d-letlet-extra1-8-3, e7a-geen-of-8-3, e7c-alle-al-3-8, ijk-cr-8-3, keten-8-w-3, keten-8-w-3-met-w1, nr-2-een-volgorde, stratum-3-w-8dd, stratum-8-3dd-zonder-werfcreatie, stratum-alle-8-w-3dd, stratum-pm-8-3dd-zonder-werfcreatie, stratum-pm2-3-w-8dd, stratum-pm2-8-3dd-zonder-werfcreatie, stratum-vraag-8-3dd-zonder-werfcreatie, td-2-8-3 |
| idem, geen volgorde leest p ([N4]) | 2 | nr-1-geen-volgorde, nr-1b-geen-volgorde-3-8 |
| `pinned_loud ~entry:"L2"` (speelt, benoemd) | 1 | tr-strat-1 |
| `pinned_silent ~entry:"S3"` (fout, antwoordt) | 6 | t1-of-lezer-eerst, t2-filter-kenmerk-lezer-eerst, t4-geen-lezer-eerst, tr-of-1, tr-kv-1, tr-pm2-ctl |
| `pinned_silent ~entry:"S1"` | 2 | lz-lv-ctl, lz-av-ctl |
| `pinned_value ~entry:"V1"` (speelt, antwoordt anders) | 3 | lz-m3-bound-laag, lz-m3-klasse-leeg, lz-mv-bound-laag |
| `pinned_value ~entry:"V2"` | 3 | lz-lv-bound, lz-av-bound, lz-pv-bound |
| ref wijkt af van de rijen (ch. 6.1-residu in waardevorm, open punt 10) | 2 | stratum-pm-8-w-3dd-gebonden (ref bonus 3), tr-cr-1-gebonden (ref bonus 1); clingo antwoordt het fixpunt |
| vorm niet vertaald (bestaand, geen parametergarde): ref speelt | 2 | fn-1-letbarriere, fn-3-argument (`expression kind not translated`, een Invoke op regelniveau) |
| idem, ref faalt | 1 | fn-1-ctl |
| niet verifier-schoon (niet toelaatbaar) | 8 | e3a-recursief-8-3, e3b-recursief-3-8 (WF-033), fc-1-doel, fc-2-doel-geen-doelen (WF-053), fn-2-tak (WF-069), t5-tak-lezer-eerst, t5b-tak-schrijver-eerst, t5c-tak-lezer-eerst-gebonden (WF-077) |

In de permutatieproef leest bij de 24 van het besluit in elke proef één
documentvolgorde p en een andere niet, behalve bij e4b en e4d
(feitcreaties: de volgorde van de gecreëerde links komt uit de join) en
bij e5n-nest-3-8-casewerf8, stratum-3-w-8dd, stratum-alle-8-w-3dd en
stratum-pm2-3-w-8dd (de volgorde komt uit de strata of de caselink gaat
in ref altijd vóór): daar is de andere volgorde nagelopen en leest die
p. nr-1 en nr-1b spelen in beide documentvolgordes, en ook in een
volgorde met een van de twee leden afwezig leest geen vouw p.

**Gates (fixronde 5, elk los na `eval $(opam env)`, op `cf084787` en opnieuw op `36f73e73`, met dezelfde regels).**
```
check-clingo: PASS (backend suite 947 checks, floor 83; 608 cases replay exactly through clingo — ref = clingo over the corpus)
check-rood: PASS (50 red cases fault observably in ref and clingo, floor 26; 100 pinned fragments cite none of the 36 listed bottom sentences)
check-oracle: PASS (ref suite 931 worked examples, floor 489; 608 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-noop: PASS (571 cases refute a do-nothing executor; 37 pinned survivors pin defeasible standing)
check-verifier: PASS (286 golden clean; 454 negatief rejected for their declared rule, floor 350)
check-self-contained: PASS
```
Gate 8's Drools-been: `check-rood: Drools leg PASS (11 fault-pinned, 39
not-carried with reason)`.

### Registerzinnen (hoofdsessie, `spec/CONSTRUCTS.md`)

Rij `ParameterRef`: voeg aan de laatste cel toe:

"Binding is fail-fast (RUNTIME-SEMANTICS ch. 5.7) in both OCaml
executors since card `clingo-parameter-failfast` (2026-09-23): a scalar
parameter the case does not bind faults in ref's words (`parameter "…"
is unbound in this case (binding is fail-fast, ch. 5.7)`) where
evaluation reads it. ref faults at the read. The ASP backend carries one
viol/2 guard per read, keyed on the read's reach (ch. 5.11): condition,
composite prefix, a quantifier fold's open prefix, taken arm or match arm
(in a closed body too), the ch. 10.6 barrier of every enclosing Let
decided per read and, in a part that runs member by member, per member,
fold member and range, closed-body subject, default displacement and
justification, a form-a default's exception groups. Members a creation
or a fact creation adds to a fold's range have no normed order (ch.
3.9a/5.7a), and ref's own order is no norm either (ref runs a fixpoint
per stratum). A read that every allowed order reaches fires in ref's
words; a read that one allowed order reaches and another does not fires
in a sentence that names the unnormed order, also where ref's own order
plays (maintainer decision 2026-09-23); a read that no allowed order
reaches fires nothing, except where two folds range over one list: each
fold's range has an order model of its own, so a named twin can join
atoms that no single order makes true, and the refusal is then named
where no allowed order reads the parameter. That holds for a quantifier
fold's matrix, a Let barrier in a per-member part and the value of a Let
whose binder is read at such a place. For the Let barrier the pass
before a creation is one of the allowed orders: ref runs a pass's rules
before its creations, so in that pass the created member is absent, and
so is a barrier only that member raises. "Every allowed order" is
decided per read, so two reads of one binder that each cover some orders
and together all give the named sentence where ref's words were due.
Where the guard is not exact otherwise it errs in one recorded
direction. Loud, a refusal where ref plays: the day-kind table and the
§7.1 horizon fence read at translation and refuse there, also behind a
failing condition; and a Let barrier that only a created member raises
counts the pass before the creation also in a rule that ch. 6.3 puts
above that creation, where ref has no such pass (the translator does not
know the strata). Silent, an answer where ref faults: the Let lowering
keeps a value behind an enclosing need on a path that does not read it,
and the guard follows that lowering, which deviates without any
parameter; and a read ref reaches only in a pass before a form of the
same stratum has written what the read's short circuit depends on (ch.
6.1, recorded residual). A list-typed parameter reads leeg on absence in
both. Red twin `corpus/rood/aanvoergeld-parameter-ongebonden` (value and
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
   `89f4f660`. `check-rood.sh` is een beschermd pad en een parallelle
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
   gedeelde zin. Getuigen hf-cond (fixronde 3) en dk-cond (fixronde 5):
   het horizonhek en de dagsoorttabel met voorwaarde 1 > 2 geven ref
   `PASS` en clingo een vertaalfout.
5. **De Let-verlaging houdt een waarde achter een omsluitende behoefte**
   (bestond al, parametervrij; README [V1], [V2], en via de garde [S1]).
   De behoefte van een Let geldt voor zijn hele lichaam, behalve waar de
   vertaler een tak naar buiten kan halen (`reads_binder`,
   `let_into_arm`: "derive LESS … a missing write"). Sinds fixronde 4
   haalt de waarde-arm ook een tak uit de waarde van een geneste Let, als
   het lichaam die Let's binder als eerste evalueert (lz-v-bound: ref en
   clingo 10,00, op main leeg). Wat blijft, met p gebonden gemeten op main
   `6e67ad8f` en de tip: (a) een Match met een arm die de binder leest,
   onder een operator of in een Let-waarde (lz-m3-bound-laag,
   lz-mv-bound-laag: ref 10,00, clingo leeg). Een Match kan niet uit de
   operator gehaald worden zoals een IfThenElse, omdat ref een Match over
   een lege scrutinee als leeg leest en de operator toch evalueert
   (lz-m3-klasse-leeg: ref 10,00); de gehoiste vorm zou daar leeg geven.
   (b) Een tak in een Let-waarde waar het lichaam eerst iets anders
   evalueert (lz-lv-bound 11,00, lz-av-bound 30,00, lz-pv-bound 10,00:
   clingo leeg). Met p ongebonden volgt de garde de verlaging waar de
   binder vóór p gelezen wordt, en is ze stil (lz-lv-ctl, lz-av-ctl:
   [S1]). De exacte route, de behoefte per pad in plaats van op het hele
   lichaam (de barrièrepredicaten van fixronde 2 kunnen dat), verandert
   de vertaling van elk Let-lichaam in het corpus: eigen kaart.
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
   punt 4, gehandhaafd in fixronde 4) laat clingo daar benoemd weigeren
   waar de volgorde beslist, ongeacht refs eigen volgorde. Bevinding van
   de lens (ronde 3): een norm "achter de caselinks, in documentvolgorde
   van de creaties" zou ref zelf schenden. ref draait per stratum een
   fixpunt; een creatie "per Werf" staat boven de creatie die Werven
   maakt, terwijl een creatie "per Droogdok" (subtype van Werf) in
   stratum 0 blijft, dus ref draait dan de creatie met de hogere index
   eerst (stratum-8-w-3dd: ref leest de 3 van creatie 2 vóór de 8 van
   creatie 0). De kandidaat-norm die ref wel volgt is "achter de
   caselinks, per stratum, en binnen een stratum in documentvolgorde".
   **Vraag voor de maintainer, geen besluit:** legt de spec die volgorde
   vast? Dan wordt de benoemde weigering een gewone garde in refs woorden,
   voor de matrix, de barrière per lid en de vraag van een Let-waarde
   gelijk, en moet de vertaler de strata van de creaties kennen. Open
   blijft dan de plaats van een gecreëerde link in een meerstapsbereik:
   ref zet haar achter de caselinks van haar eigen tussenschakel en vóór
   die van een latere. Sinds `811864c6` (WF-046: de kwantorvouw draait na
   haar creaties) kan die plaats de uitkomst bepalen. Sinds fixronde 5
   (besluit van de hoofdsessie) valt ook de pass vóór een creatie onder
   het besluit, voor de Let-barrière: in die pass ontbreekt het
   gecreëerde lid. Een norm per stratum zou ook [L2] opheffen (open punt
   13).
8. **Een Let-barrière in een deel per lid over leden van een creatie**
   (opgelost in fixronde 4). De barrière en de vraag van een Let-waarde
   hebben nu elk een tweede vorm ("mogelijk eerder" naast "zeker eerder",
   `lbg…` naast `lbg…_c`, `dmdN_o` naast `dmdN`) en een benoemde
   tweeling. pm-cr-8-3-cond weigert benoemd waar ref speelt ([N2], het
   besluit), en pm-cr-3-8 weigert benoemd waar ref faalt.
9. **Deling door nul in de matrix van een kwantorvouw negeert de
   kortsluiting** (bestond al, parametervrij; de laatste notitie van de
   vorige bouwer). i-h0 en q-a0 geven ref PASS en UNSAT op main,
   `325568da` en `0f712e4e`. Het `kvr`-predicaat kan de andere hekken in
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
    grows"). Een getuige daarvan uit de lens van ronde 3, met p gebonden:
    stratum-pm-8-w-3dd-gebonden (`Let k = restpost in som(lengte van
    schuiten die TEN MINSTE 1 [ALLE[lengte > 5, k > 0], lengte > p])`
    zonder regelconditie, creaties 8 per Werf, een werf per Haven en 3 per
    Droogdok, D1 een Droogdok, p = 1): ref schrijft bonus 3, want het
    evalueert de somvouw in stratum 0, als D1 alleen de 3 heeft, en trekt
    niet terug; clingo schrijft leeg. Op main `6e67ad8f` en op de tip
    gelijk. Een tweede getuige uit de lens van ronde 4, zonder
    niet-monotone vouw: tr-cr-1-gebonden (de vorm van tr-cr-1 met p = 1):
    ref schrijft bonus 1 in de pass vóór de creatie en trekt niet terug,
    clingo leeg. Dat is de waardevorm van [S3] (open punt 14).
11. **De takhoist van de Let-verlaging las de conditie vóór de operand
    naast de tak** (bestond al; opgelost in fixronde 5, punt 4; README
    [S2] is weg). `W[als c dan P anders Q]` wordt `als c dan W[P] anders
    W[Q]`; een operand die ref vóór de tak evalueert, staat daarna achter
    de conditie. Leest die operand een ongebonden parameter en leest de
    conditie een binder met een lege behoefte, dan koos de conditie geen
    arm en vuurde de garde nergens (hoist-ctl: ref fout, clingo heffing
    leeg, op main en op `a51a3ebe`). De hoist onthoudt nu per takknoop die
    hij bouwt welke operanden ref vóór de conditie leest (`ctx.hoisted`),
    en een ongebonden parameterlezing daarin krijgt een tweede garde
    zonder de atomen van de arm, weggenomen op hun plaats in `rule_guard`.
    hoist-ctl, h-diep, h-samengesteld en h-voor-cond-q-pos weigeren in
    refs woorden; h-na-tak, h-voor-cond-q-neg, hoist-rechts-bound en de
    gebonden tweelingen spelen zoals voorheen; geen corpusprogramma
    verandert. De gesloten lichamen dragen de tweede garde niet: ze zijn
    leeg-vrij (ch. 10.1), dus de conditie kiest daar altijd een arm.
12. **"Elke toegestane volgorde" wordt per lezing en per atoom beslist**
    (fixronde 4; README [N3] en [N4]). Twee lezingen van één binder die
    elk een deel van de volgordes dekken en samen alle, geven de benoemde
    zin waar refs woorden passen: disj-8-3 (twee kwantorvouwen over
    dezelfde gecreëerde 8 en 3 onder `Let k = p`, de ene leest k als 3
    eerst komt, de andere als 8 eerst komt): ref faalt, clingo weigert
    benoemd. Beide weigeren; alleen de woorden verschillen. Omgekeerd kan
    de conjunctie van "in een toegestane volgorde" per atoom een volgorde
    veronderstellen die als geheel niet bestaat; dan weigert clingo
    benoemd waar geen volgorde de parameter leest. De lens toonde dat in
    ronde 4 aan (nr-1, [N4]: twee vouwen over dezelfde lijst, elk met een
    eigen volgordemodel; ref speelt, clingo weigert benoemd). Gemeten in
    fixronde 5 (punt 3): één gedeeld volgordemodel per lijst is geen
    begrensde fix. De relaties van de twee bereiken verschillen niet op de
    gecreëerde leden; elk atoom kiest zijn eigen getuigevolgorde, en voor
    tellende kwantoren en de afgeleide barrière- en vraagatomen is de
    samenhang van die getuigen geen verzameling voorrangsparen. Exact
    beslissen vraagt de volgordes als geheel (in ASP: een keuze over de
    permutaties per lijst met een bestaansvraag, wat het model van de
    runner verandert); niet gebouwd.
13. **[L2]: de vertaler kent de strata niet** (fixronde 5, gevolg van punt
    1). Een Let-barrière die alleen een gecreëerd lid opwerpt, telt de
    pass vóór de creatie ook in een regel die ch. 6.3 boven die creatie
    zet, waar ref zo'n pass niet heeft: tr-strat-1 (ALLE-conditie over de
    schuiten, één schuit van 8 per Werf) geeft ref `PASS` en clingo de
    benoemde weigering; op main en `a51a3ebe` speelden beide. Met de
    strata in de vertaler (de dependency-vormen staan in
    `Ir_v0.Checks.dep_forms`; ref rekent de strata zelf uit in
    `eval.ml`) zou de aanwezigheid van zo'n lid gelden voor een vorm
    strikt boven elke creatie die het bereik vult, en viel [L2] weg. Dat
    raakt ook open punt 7 (een norm per stratum). Niet gebouwd: een eigen
    stratificatie in de vertaler is werk van een andere orde; de
    hoofdsessie beslist.
14. **Normvraag: de tussentijdse pass binnen één stratum** (fixronde 5,
    punt 2; README [S3]; het residu van ch. 6.1 in fail-fast-vorm). ref
    draait de vormen van een stratum pass voor pass. Een vorm die leest
    wat een latere vorm van hetzelfde stratum schrijft, ziet dat in de
    eerste pass leeg; valt daardoor een kortsluiting weg, dan leest ref de
    parameter en faalt. t1: `heffing = 5,00 indien TEN MINSTE 1 [a > 0,
    p > 0]` vóór `a = inkomen`: ref fout, clingo 5,00; met de schrijvende
    regel eerst (t1b) beide 5,00. De schrijver kan een creatie zijn
    (tr-of-1, tr-kv-1, tr-pm2-ctl), en in waardevorm schrijft ref een
    tussenwaarde die het niet terugtrekt (tr-cr-1-gebonden,
    stratum-pm-8-w-3dd-gebonden; open punt 10). **Vraag voor de
    maintainer, geen besluit:** is refs fout op zo'n tussentijdse lezing
    het antwoord van de arbiter, of een defect van ref dat bij het residu
    van ch. 6.1 hoort? Advies van de lens: het fixpunt van ch. 6.3 leest p
    daar niet. Daarbij hoort een samenhangsvraag: voor een Let-barrière
    maakt het besluit van fixronde 5 de pass vóór een creatie tot een
    benoemde weigering, voor een kortsluiting (een samengestelde
    conditie, of een vouw die bij een eerder lid sluit dankzij een geneste
    vouw over gecreëerde leden) blijft die pass stil. Beide benoemd maken
    vraagt een "in elke pass"-waarheid van elke vouw over een groeiend
    bereik (niet begrensd); beide exact maken volgt uit het antwoord op de
    normvraag.
15. **Rebase volgt in een eigen ronde** (opdracht fixronde 5). De kaart
    staat op main `6e67ad8f`. Main krijgt binnenkort de M1-landing
    (v1-wire); de rebase migreert dan ook de ingebedde v0-fixtures van
    `test_clingo.ml`.
