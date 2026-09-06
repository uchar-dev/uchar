# Het onbekend-sentinel op een GEWONE leespositie: welgevormd of niet?

Goal: WF-093 (2026-09-06) beslechtte de wrat als vouwBRON. De regel trok
daarbij een grens, en de grens is de bron. Deze kaart beslecht wat aan de
andere kant ervan ligt: het **onbekend-sentinel op een gewone
LEESpositie** — het operand van een geleste vouw, een kale referentie, een
set-lezing — waar geen vouwbronregel bij komt. Vandaag is dat de
één-antwoordt-twee-weigeren-klasse die PLAN §6 per definitie een spec-bug
noemt: het document is verifier-schoon terwijl `ref` weigert
(`unsupported AttribuutRef shape (set/sentinel outside a fold)`), de
ASP-backend de expressiesoort weigert
(`expression kind not translated (no corpus case exercises it)`) en de
Drools-backend hem SPEELT en antwoordt. Twee antwoorden zijn machinaal
toetsbaar; het huidige "geen van beide" is de enige stand die dat niet is.

## De twee te wegen antwoorden

**(i) Het sentinel hoort NIET buiten de telbron.** Dan komt er een
WF-regel die het sentinel op elke leespositie weigert — een eigen nummer,
of een clausule bij WF-034, dat het sentinel *binnen een structured node*
al weigert maar de vouwargument-tak niet bereikt (die tak vervangt
`walk_expr`; zie hieronder). Kosten: negatiefs per deelvorm, een
arm-exacte mutant, de regressiegrens over de vier zones geteld, en een
antwoord op de vraag wat er dan met de pariteitszone gebeurt (die de vorm
draagt en read-only geverifieerd wordt).

**(ii) Het sentinel is een geldige leesvorm.** Dan is `ref`'s
`unsupported AttribuutRef shape (set/sentinel outside a fold)` een gat en
geen wet, en moeten `ref` én de ASP-backend de vorm dragen: de lezer leidt
het datatype af uit de DECLARATIE (de effectieve-type-wet,
`RUNTIME-SEMANTICS.md` §2.10 — precies wat gate 13 al van elke consument
eist voor de eenheid). Kosten: een arm in `ref`, een vertaling in de
ASP-backend, en een golden met handberekende absolute pins.

**(iii) De derde optie, die HIER gewogen moet worden en niet in WF-093.**
Tijdens de WF-093-fixronde kwam een derde sleutel bovendrijven: WF-093
niet op het sentinel sleutelen maar op "het `attribuut`-slot resolveert
naar een ROL". Dat lijkt scherper — het scheidt de rolnoemende wrat van
een gedegradeerde attribuutlezing — maar het is gemeten GEVAARLIJK zolang
`ref` niet meebeweegt: de deelklasse *sentinel op een echte
elementattribuut-set-lezing zonder `navigatieRol`* wordt daarmee
verifier-SCHOON, en op die vorm antwoorden clingo én Drools het juiste
getal (19.00/12.00 op leden van 12 en 7; en op
`corpus/golden/jutterswet-vondsttelling` met het `type` van zijn echte
set-bron gedegradeerd: alle zeven gepinde verwachtingen, de 3.75
inbegrepen) terwijl `ref` weigert. Dat is een LEVENDE twee-tegen-één
divergentie onder een welgevormd document — precies de klasse die WF-093
sloot. Deze kaart mag optie (iii) alleen kiezen samen met (ii): eerst
`ref` laten dragen, dan pas de sleutel verfijnen.

## De twee deelvormen, gemeten (2026-09-06) — ze gedragen zich VERSCHILLEND

Beide zijn **verifier-PASS**; het verschil zit in de derde uitvoerder.

| deelvorm | verifier | `ref` | ASP-backend | Drools |
|---|---|---|---|---|
| sentinel op een ECHTE attribuutlezing (v0 degradeerde het type) | PASS | FAULT `unsupported AttribuutRef shape (set/sentinel outside a fold)` | FAULT `expression kind not translated` | **PLAYED** — hij vuurt en antwoordt |
| de WRAT VERBATIM als gelest operand (een ROLnaam in `attribuut`, onder het sentinel) | PASS | idem | idem | **ERROR** `IllegalStateException: geen getter voor 'vondsten'` |

De tweede rij is het bewijs dat de derde uitvoerder de ROLNAAM wel
degelijk leest — hij zoekt er een getter voor en crasht. Eén regel die op
deze positie sleutelt moet die twee dus scheiden, of hun verschil
expliciet wegnemen.

## De vorm, reconstrueerbaar zonder de casus

De tegenvorm die deze meting opleverde is eigen materiaal en staat
hieronder voluit; ze is op het model van
`corpus/golden/jutterswet-vondsttelling` gebouwd (plus één ingevoerd
`Jutter`-attribuut `drempelgewicht`, kg, 2 decimalen):

- een `Functie` `MAXIMUM` met **twee** argumenten en
  `elementObjecttype: "Jutter"` (die combinatie is wat telt: mét een
  `elementObjecttype` lopen de argumenten door de vouwargument-tak, die
  `walk_expr` VERVANGT — daarom bereikt WF-034's sentinel-prologue ze
  niet; zonder `elementObjecttype` vallen ze wél in `walk_expr` en vuurt
  WF-034 gewoon. Dat verschil is de hele reden dat de vorm bestaat);
- operand 0: een gewone `AttribuutRef` op `drempelgewicht` met zijn echte
  type;
- operand 1: ófwel dezelfde lezing met `type` = `{"node":"Onbekend"}`
  (deelvorm 1), ófwel de wrat verbatim — `attribuut: "vondsten"`,
  `objecttype: "Jutter"`, `set: false`, `navigatieRol: null`, sentinel
  (deelvorm 2).

## Waar de vorm vandaag in de corpus staat

Drie LEESposities over de vier zones, alle in de pariteitszone, alle in
`toka/casus/ir.json`, benoemd per JSON-pointer en als klasse — **één
gelest vouw-operand, twee kale lezingen**:

- `/resolvedRegels/5/waarde/argumenten/1` — het geleste operand (de
  omhullende `Functie` is `EERSTE` met twee argumenten en een
  `elementObjecttype`);
- `/resolvedRegels/15/waarde` — een kale lezing als de hele regelwaarde;
- `/resolvedRegels/16/waarde/links` — een kale lezing als linkeroperand
  van een `Binair`.

Twee sentinelposities die géén lezing zijn en dus buiten de vraag vallen,
voor de volledigheid geteld: een regelDOEL in
`corpus/negatief/wf034-onbekend-regeldoel/ir.json`
(`/resolvedRegels/1/doel/type`, al afgewezen), en het eigen `type` van de
`Binair` die door de derde lezing hierboven wordt gevoed
(`/resolvedRegels/16/waarde/type`). Gemeten over alle vier zones: 63
sentinel-`type`-posities in totaal, waarvan 59 vouwargumenten (58 wratten
+ het ene geleste operand).

## De producentroute

De vorm is geen handbouwsel alleen. De RegelSpraak-frontend zet het
sentinel in
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/ExpressieBouwer.java:1663`
(`if (dt == null) dt = onbekend();`) — de terugval wanneer de
referentiebouwer geen attribuutdatatype vindt. Op de VOUWBRON-positie is
die route gemeten (kenmerk-aggregatie; WF-034 + WF-080 co-vuren daar
altijd, zie de WF-093-motivering in `spec/IR-SPEC.md` ch. 4.3). Of
dezelfde terugval ook een LEESpositie kan bereiken zonder co-vurend
defect, is een meting die deze kaart moet doen — en het antwoord bepaalt
of (i) een levende producent afsnijdt of alleen een handvorm.

Acceptance:
- **Eén beslissing voor ALLE posities** (gelest vouw-operand, kale
  referentie, set-lezing), expliciet gemotiveerd; de motivering noemt wat
  de andere kant zou hebben gekost, en weegt optie (iii) hierboven met de
  waarschuwing die erbij hoort.
- De twee deelvormen worden expliciet geadresseerd: één beslissing die ze
  samenneemt, of twee met hun onderscheid benoemd. Het verschil in
  uitvoerdergedrag mag niet onbesproken blijven.
- **Regressiegrens geteld, niet aangenomen**: hoeveel sentinel-posities
  dragen de vier zones op een LEESpositie, en hoeveel bewegen. Vandaag
  gemeten: 3 leesposities (alle pariteit), 2 niet-lezingen. De kaart
  hermeet.
- Rood-eerst: de negatiefs (bij (i)) dan wel de getuige-golden (bij (ii))
  bestaan en zijn gemeten vóór de regel/de arm er is, met de verifier-md5
  van vóór en ná.
- Elke nieuwe of gewijzigde WF-regel: negatief-case + **arm-exacte
  mutant** (de mutatie maakt precies dat negatief groen en niets anders),
  gemeten over alle vier de zones.
- De ch.-4.3-flank (4) in `spec/IR-SPEC.md` wordt óf dichtgeschreven óf
  herschreven naar wat er dan nog open staat.
- **Alle dertien gates groen.** Let op gate 11: de pariteitszone draagt de
  drie posities, dus antwoord (i) raakt haar direct.

May touch: `spec/IR-SPEC.md` (voorstel — beschermd pad),
`spec/RUNTIME-SEMANTICS.md` (voorstel — idem), `spec/verifier/**`
(voorstel — idem), `corpus/negatief/**`, `corpus/golden/**` (nieuwe
getuige), `ref/**` en `backends/clingo/**` alléén bij antwoord (ii),
`backlog/`.

Out of scope: `backends/drools/**` — die speelt de ene deelvorm en crasht
op de andere; beide uitkomsten zijn hier BEWIJSMATERIAAL, geen doelwit.
WF-093's eigen scope (de vouwBRON, geland
`backlog/done/20260905-wrat-waardevouw.md`) staat vast en wordt hier niet
heropend — behalve als optie (iii) samen met (ii) wordt gekozen, en dan
expliciet en met de divergentiemeting erbij. `toka/` wordt gelezen, nooit
gewijzigd.

---

Result (2026-09-06, branch `card/sentinel-leespositie`, unmerged — protected
paths):

**Het besluit: (i), WEIGEREN.** Het onbekend-sentinel op een gewone
LEESpositie is ill-formed. De regel hangt als **clausule bij WF-034 —
zevende golf, arm (r)** en krijgt géén eigen nummer: het is dezelfde
omheining, dezelfde melding en dezelfde wet als `walk_expr`'s
type-prologue, alleen op de posities die de vouwargument-tak (die
`walk_expr` VERVANGT) eraan onttrok. Zes eerdere golven hebben precies
zo gehandeld — een arm die op die tak herhaald moet worden houdt zijn
oorspronkelijke nummer (WF-034 golf 1 en 2, WF-089, WF-092, WF-053). Een
eigen nummer zou één omheining over twee regels splitsen.

**Conditieketen, en de WF-093-grens.** In de referentiearm van de
vouwargument-tak: `typ = Some Onbekend && List.length argumenten <> 1` →
WF-034 op `<pointer>/type`. De grens is de **ARITEIT en niets anders**.
WF-093 weigert een niet-AANTAL-soort over de ENKELVOUDIGE
sentinel-vouwbron; deze arm weigert het sentinel waar er helemaal geen
bron is. De twee condities zijn disjunct: geen positie wordt dubbel
gerapporteerd, geen positie valt ertussen. Gemeten met een mutant die de
uitzondering verschuift naar "enkelvoudig ÉN AANTAL": die voegt precies
aan de vier `wf093-wratvouw-*`-negatiefs een tweede citaat toe en aan
niets anders — de doorkruising die de ariteitsgrens vermijdt.

**Hermeten census (main 44b393c, 533 documenten; vier zones).** 71
sentinel-knopen totaal, waarvan **65** op een `type`-slot (de kaart telde
63; het verschil zijn twee wratten die met WF-094's negatiefs meekwamen).
Van die 65 zijn **61** vouwargumenten, alle onder een
`elementObjecttype`: **60** enkelvoudige wratten (de kaart: 58) en
precies **ÉÉN** gelest operand. De overige 4 zijn geen vouwargument: één
regelDOEL in een al-afgewezen negatief, twee kale lezingen en het eigen
`type` van de Binair die de tweede daarvan voedt. De 6 niet-`type`-knopen
zijn declaratie- en payloadposities, alle in al-afgewezen negatiefs.

**De drie "leesposities" van de kaart waren er al twee te veel.** Gemeten:
de twee kale lezingen in de pariteitszone worden vandaag AL geweigerd door
`walk_expr`'s prologue (`unknown-type sentinel inside a structured node`),
en het ene geleste operand daar wordt al geweigerd door WF-034's
naamresolutie-arm. **De pariteitszone verandert dus van verdict niet:**
het document telde 33 overtredingen en telt er nu 34. Nul golden, nul rood,
nul negatief beweegt. Gate 11 is onbewogen — gemeten, niet aangenomen:
45/2/27/29, exact de verwachte tellingen. Dat gate 3 de verifier niet over
die zone draait staat als boekhoudregel in de spec (het stond er al voor
flank (2) en staat er nu ook voor deze).

**Vier negatiefs, waarvan drie rood-eerst.** Verifier vóór:
md5 `2ab63519d103a0abee721296973a61d7`; ná: `c95650b1ab993c8a8b370e5b0617b0e6`.
`wf034-sentinel-gelest-vouwoperand` (deelvorm 1) PASS → FAIL;
`wf034-sentinel-gelest-wratoperand` (deelvorm 2) PASS → FAIL;
`wf034-sentinel-geleste-setlezing` (de set-lezing als gelest operand)
PASS → FAIL. `wf034-sentinel-kale-lezing` was **al** rood en is als
zodanig gelabeld: hij pint de prologue-arm, die in de HELE negatief-zone
geen enkele getuige had — een levende arm die ongemerkt had kunnen
regresseren.

**De twee deelvormen, met het uitvoerdersverschil benoemd** (zelf gemeten,
gate-12-jar): deelvorm 1 wordt door Drools GESPEELD en beantwoord (12.00,
geverifieerd met een opzettelijk foute pin); deelvorm 2 laat Drools
CRASHEN op de getter van de rolnaam; de set-lezing wordt door Drools per
vorm GEWEIGERD (transpiler SKIP); de kale lezing wordt gespeeld en
beantwoord. `ref` en de ASP-backend weigeren alle vier uniform. Drie
soorten doodlopende weg over één positie — dat is het argument om de vorm
op de wire te weigeren in plaats van op drie plekken.

**Arm-exacte mutanten** (/tmp via `git archive`, harde build-faal, `test -x`,
binary-md5 per sweep, volle vier-zone-sweep over 537 documenten):
M1 clausule weg → precies de drie getuigen vrij + één pariteitsregel weg,
niets anders (arm-exact). M2 ariteitsgarde omgedraaid → drie getuigen vrij
EN 12 documenten nieuw afgewezen (10 goldens, 5 daarvan anchors, 2 rood) +
29 rapporten met bijvangst. M3 ariteitsscope weg → dezelfde 12.
M4 uitzondering naar "enkelvoudig ÉN AANTAL" → precies de vier
`wf093-wratvouw-*` krijgen een tweede citaat (de doorkruising). M5 prologue
weg → precies `wf034-sentinel-kale-lezing` vrij; vóór deze kaart bevrijdde
dezelfde mutatie in de drie corpuszones niets.

**Injectieproef herhaald** over alle **2701** datatype-posities van elke
golden, één tegelijk: **6** overlevers vóór golf 7 (alle geleste operanden,
in `jutterswet-peildatum` en `jutterswet-sluitsom`), **0** erna. De
"9 fold-/aggregatie-argumenttypes" die golf 2 als open flank noteerde zijn
daarmee dicht; wat overblijft is niet een overlever maar de gedocumenteerde
uitzondering (de 15 no-op-posities die de wrat per constructie draagt).

**Producentroute: JA, en dat is gemeten.** `ExpressieBouwer`'s terugval
schrijft het sentinel alleen waar de referentie tegen niets resolveert (elk
herstelpad dat wél bindt herschrijft naam én eindpunt). Op een kale lezing
co-vuurt dan altijd de naamresolutie-arm; op een gelest operand meestal
ook — **behalve** wanneer de onopgeloste geschreven naam (ci) samenvalt met
een gedeclareerde rolnaam of -meervoud: dan admitteert ch. 4.3's
rolnaam-uitzondering hem in deze tak en is het sentinel het ENIGE gebrek.
Gemeten door de frontend zelf te draaien op een eigen `.rgs`: een
twee-operands `MAXIMUM` waarvan operand 1 de wrat verbatim is, **zonder
enige diagnostiek**, `verifier: PASS` vóór deze golf — en Drools crasht
erop terwijl ref en de ASP-backend weigeren. Antwoord (i) snijdt dus een
LEVENDE producent af, niet alleen een handvorm. Alleen gemeten aan de
frontend; niets gewijzigd.

**Optie (iii)** is gewogen en verworpen, met de waarschuwing uit de kaart
letterlijk opgenomen in ch. 4.3: hersleutelen van WF-093 op "het
`attribuut`-slot resolveert naar een ROL" heropent precies de
twee-tegen-één-divergentie die WF-093 sloot, en mag nooit vóór een
`ref`-arm.

**Wat (ii) gekost zou hebben** staat in de motivering: een arm in `ref`,
een ASP-vertaling en een golden met handberekende pins — voor een vorm die
uitsluitend als DEGRADATIE ontstaat.

**Dertien gates, alle groen** (vanuit deze worktree): 1 PASS; 2 = 178;
3 = 178 golden / **329** negatief (325 + 4); 4 = 431; 5 = 409 (2 zonder
case); 6 = 409; 7 = 373/36; 8 = 22 (Drools-leg 5/17); 9 = 201/12;
10 = 173/31; 11 = 45/2/27/29 **onbewogen**; 12 = played 110 / channel floor
46 / auto-excluded 43 / value-divergence 1 / no-case 2 / JUnit 207;
13 = 74/15/0.

**Aangeraakt:** `spec/IR-SPEC.md` (ch. 4.3 flank (4) gesloten met de
deelvormentabel, de producentmeting, de (ii)/(iii)-afweging en de
regressiegrens; WF-034's zevende golf in ch. 4.6; indexrij + de herhaalde
injectieproef; twee ripple-zinnen bij WF-038's defer),
`spec/verifier/lib/checks.ml` (één clausule + commentaar), vier negatiefs,
deze kaart. **Niet aangeraakt:** `spec/CONSTRUCTS.md` en `spec/PLAN.md`
(rijvoorstel apart doorgegeven), `ref/**`, `backends/**`, `frontends/**`
(alleen gelezen en gedraaid), `scripts/**`, `toka/**` (alleen gelezen).

---

Fixronde (2026-09-06, zelfde branch, ná twee lenzen — de kern hield, één
BLOKKERENDE vondst en één tussenklasse):

**De routekeuzes, uit het maintainerbesluit (i) afgeleid — "één beslissing
voor ALLE leesposities".** (a) **B1 gesloten met de None-arm.** De golf
herhaalde op de vouwargument-tak alleen de GESPELDE helft van de prologue;
de ongespelde tweeling (`"type": null`) bleef staan op precies dezelfde
posities. Eén regel die twee spellingen van hetzelfde gebrek verschillend
behandelt is geen omheining. (b) **MF2 gesloten door de garde te verbreden
naar ariteit ∧ soort.** Open boeken zou een LEVENDE
één-antwoordt-twee-weigeren mét crash hebben laten staan achter een grens
die net exact was verklaard; sluiten beweegt gemeten NUL documenten.
(c) **K5 (`set: true` met een echt type buiten een vouw) blijft een EIGEN
kaart** — gemeten orthogonaal: die vorm draagt een ECHT type, dus de
onbekend-type-omheining had er nooit rechtsmacht over.

**B1 — de ongespelde tweeling.** `checks.ml`: `| None when not
source_exempt -> [ v "WF-034" (p ^ "/type") "value node without a static
type" ]`, dezelfde zin als de prologue. Negatief
`wf034-sentinel-null-vouwoperand` (`jutterswet-peildatum` met
`/resolvedRegels/0/waarde/argumenten/1/type := null`). **Rood-eerst:**
verifier vóór md5 `c95650b1ab993c8a8b370e5b0617b0e6` → `verifier: PASS`;
ná md5 `8ea45e866dddcf861b357b430d50b4d1` → `WF-034 FAIL … value node
without a static type`. Uitvoerders, zelf gemeten: **`ref` PASS (5
expectations)** — hij SPEELT, en antwoordt 2026-05-01; clingo
`TRANSLATION FAULT: mixed date/non-date operands under an eerste/laatste
fold are not translated`; Drools **PLAYED**, óók 2026-05-01 (geverifieerd
met een opzettelijk foute pin). Twee antwoorden die het EENS zijn, één
weigering — de klasse van de andere kant gelezen. En één spelling verder:
dezelfde `null` op een EENHEIDDRAGENDE numerieke operand laat `ref` spelen
(1 expectation, 12.00) terwijl Drools **crasht**
(`NullPointerException: Cannot invoke "…Datatype.eenheidOf()"`). Die
tweede spelling deelt clausule, pointer en melding en staat in de NOTE, niet
als achtste negatief.

**De producentvraag, gemeten.** NEE: de frontend kan `type: null` niet
emitteren. `IrEmitter.datatype` schrijft JSON-`null` alleen als het model een
null-datatype aanreikt, en `ExpressieBouwer` vervangt dat eerst door het
sentinel. Direct gemeten: 73 van de 78 `.rgs`-bronnen van de repo emitteren
(de andere 5 zijn fragmenten die niet zelfstandig resolveren), **0
voorkomens** van `"type": null` in welk geëmitteerd document dan ook. B1
snijdt dus een WIRE-vorm af, geen producentroute — anders dan de gespelde
helft, die er wél een afsnijdt. Beide worden gelijk geweigerd omdat geen
consument ze onderscheidt.

**MF2 — de grens is ariteit ÉN soort.** `source_exempt = List.length
f.argumenten = 1 && (value_fold_name f.kind <> None || f.kind = Aantal)` —
letterlijk WF-093's eigen paar, zodat de twee regels rand aan rand liggen
over vier kwadranten: enkelvoudig+AANTAL → beide af; enkelvoudig+waardevouw
→ alleen WF-093; enkelvoudig+builtin → arm (r); ariteit ≥2 → arm (r).
Zelfde predicaat op beide armen. De oude claim "none falls between them"
was FALS: een enkelvoudige builtin mét `elementObjecttype` viel er precies
tussen. Zes builtin-spellingen handgebouwd en gemeten: **drie omheind** en
reproduceren nog steeds (WF-048, WF-083 (c), WF-047 — de laatste met
`bronRol`), **drie niet** (gedegradeerde eigen lezing, wrat verbatim, de
`set`-lezing). Twee negatiefs, rood-eerst met dezelfde twee md5's:
`wf034-sentinel-builtin-operand` (Drools **PLAYED 12.00**, geverifieerd met
een foute pin: `verwacht 99.00, werkelijk 12.00`) en
`wf034-sentinel-builtin-wratoperand` (Drools **ERROR
`IllegalStateException: geen getter voor 'vondsten' op …Jutter`**); de
`set`-spelling is Drools-**EXCLUDE** en deelt melding en reden.

**Meldingstekst herschreven** zodat hij beide uitgangen noemt: *"unknown-type
sentinel on a fold operand that is not a fold SOURCE: the sentinel is
admissible on the single argument of an AGGREGATION alone (the v0 row-1 wart,
ch. 4.3) — with two or more arguments the node is the listed value fold, whose
operands are ordinary reads, and at a non-aggregation kind there is no source
at all"*. De drie bestaande NOTEs citeren hem verbatim en zijn bijgewerkt.

**Mutanten** (alle: `/tmp` via `git archive HEAD | tar -x`, harde build-faal,
`test -x`, binary-md5, volle vier-zone-sweep over 546 documenten; vergeleken
tegen de gefixte verifier `8ea45e86…`. Alle zeven zijn ná de rebase op main
`2634463` opnieuw gebouwd en gedraaid; elke uitslag reproduceert getal voor
getal, de md5's hieronder zijn die van de gerebasede kop):

| mutant | receptuur (op `checks.ml`) | md5 | uitslag |
|---|---|---|---|
| M1 | de hele golf-7-clausule (beide armen + `source_exempt`) vervangen door `[]` | `2df57a7ba7a1213bf0d8f0cfad71e3aa` | **6** getuigen vrij (3 gelest + null + 2 builtin), 0 nieuw afgewezen, 1 pariteitsregel weg — arm-exact |
| M2 | ariteitshelft omgekeerd: `= 1` → `<> 1` in `source_exempt` | `5364407a1710872c7933c56b10d1a8dd` | 4 vrij (3 gelest + null) EN **12** nieuw afgewezen (10 goldens, 5 anchors, 2 rood) + 29 rapporten met bijvangst |
| M4 | uitzondering versmald: `= 1 && f.kind = Aantal` | `4681ec43f4821d5ba9e3ed6c97fde47c` | 0 vrij, 0 nieuw; precies de vier `wf093-wratvouw-*` krijgen een tweede citaat — de doorkruising die de soortenset vermijdt |
| M5 | prologue-arm: `Some (Datatype_onbekend _) -> []` | `b613bf5749d0f8e55f89e04665ee67bc` | precies `wf034-sentinel-kale-lezing` vrij + 1 pariteitsregel — herbewijs |
| M6 | SOORT-helft terug: `source_exempt = List.length f.argumenten = 1` | `97c9e7499171247e5955a28d293f1cd2` | **precies** de twee `wf034-sentinel-builtin-*` vrij, geen ander verdict en geen andere rapportregel — de dichtgemaakte kier op ware grootte |
| M7 | de `| None when not source_exempt ->`-arm geschrapt | `c0db8ab71724082af799e240068dfe50` | **precies** `wf034-sentinel-null-vouwoperand` vrij, verder niets |

M1 dient tegelijk als de "vóór arm (r)"-verifier van de injectieproef: hij
haalt beide armen weg, en de proef injecteert alleen sentinels, dus de
None-arm doet daar niet mee.

**Injectieproef, herschreven recept en hermeten** (getallen van de
gerebasede kop, 2026-09-06)**.** 157 goldens; elke
JSON-knoop met `node` ∈ de elf datatype-soorten (`Numeriek`, `Percentage`,
`Tekst`, `Booleaan`, `DatumInDagen`, `DatumTijd`, `Enumeratie`, `Onbekend`,
`SumType`, `ListType`, `StructType`) één tegelijk vervangen door
`{"node":"Onbekend","reden":"niet opgelost"}`; overlever = exit 0; de 15
no-ops (posities die het sentinel per constructie al dragen) apart geteld.
**2813 injecties. Vóór arm (r): 10 overlevers**, alle geleste vouwoperanden —
acht in `jutterswet-peildatum` (vier vouwen × twee operanden) en twee in
`jutterswet-sluitsom`. **Ná: 0.** De eerdere telling (6 over 2701) kwam van
een smaller instrument; noemer én teller zijn nu allebei genoemd. Vóór de
rebase gemeten als 2804 over 156 goldens; main's nieuwe golden voegt er negen
posities aan toe en geen enkele overlever.

**Nulcensus en builtin-census, vier zones.** Waardeknopen met een expliciete
`null` op `type`: **drie**, alle in al-afgewezen negatiefs (één
`AttribuutRef` — een regelDOEL —, één `Literal`, één `Functie`) en **nul**
op een vouwargument. Niet-aggregatie-`Functie` mét `elementObjecttype`:
**nul** vóór de twee nieuwe getuigen. Beide helften van de fixronde bereiken
dus niets wat er al stond.

**Regressiegrens van de fixronde.** Gesweept over alle **546** documenten
(157 golden + 22 rood + 337 negatief + 30 pariteit) vóór en ná: **zeven
rapporten en drie verdicts** bewegen, en de twee helften zijn apart gemeten.
De SEMANTISCHE helft — de verbrede garde plus de tweelingarm, tegen de golf
precies zoals die eerst landde (mutant PRE,
md5 `7fc1c77686f022e96d30ee6d51733f65`; receptuur: `source_exempt` terug naar
`List.length f.argumenten = 1` én de `| None when not source_exempt ->`-arm
geschrapt) — beweegt **drie documenten en verder niets**, alle drie een
verdict en alle drie een eigen nieuwe getuige. De overige vier rapporten zijn
de MELDINGSherschrijving: drie bestaande `wf034-sentinel-*` en de
pariteitscasus herformuleren één regel, geen verdict, en het
overtredingsaantal van de pariteitscasus blijft 34. Nul goldens, nul rood,
geen ander negatief.

**Spec-meebewegingen** (alle in `spec/IR-SPEC.md`, beschermd pad): "refused
alike" wordt waar en zegt sinds wanneer; ch. 4.3 flank (4) op de nieuwe grens
(vier kwadranten, de zes gemeten builtin-spellingen, "none falls between
them" nu waar); de deelvormentabel uitgebreid naar **zes** rijen met een
CONTROLEREGEL per rij (rij 1: de ASP-kolom is niet toerekenbaar — de
omhullende vouw weigert ook mét echt type; rij 3: pint de
set-buiten-vouw-flank, niet het sentinel; rij 6: `ref` staat aan de
ANTWOORDENDE kant); "Three kinds of dead end" → **vier**, met de twee throws
bij naam (`IllegalStateException` op de getter, `NullPointerException` op
`eenheidOf`); de "SAME fault, which is the norm"-zin herzien (waar voor rij
1–5, ONWAAR voor rij 6); de nulcensus en de builtin-census; de producentmeting
van de tweeling; ch. 4.6 golf 7 herschreven mét het injectierecept; WF-038's
defer-scope symmetrisch gemaakt; de indexrij krijgt een golf-7-paragraaf, de
fixrondetelling wordt **vijf** (één per golf vanaf de derde) en "forty arms"
wordt vervangen door een controleerbare noemer: **43** `v "WF-034"`-emissie-
plekken in `checks.ml`, na te tellen met één grep.

**K5 als eigen kaart:** `backlog/new/20260906-set-buiten-vouw.md`, en
ch. 4.3 krijgt flank **(5)**. Gemeten grondslag: de frontend emitteert uit
"… het gewicht van **alle** vondsten van de Jutter" een `AttribuutRef` met
`set: true`, een ECHT `Numeriek`-type en een `navigatieBindingen`-rolbinding,
mét `diagnostics: []`; verifier PASS; `ref` FAULT
(`unsupported AttribuutRef shape (set/sentinel outside a fold)`), ASP
`expression kind not translated`, Drools **EXCLUDE** (`waarde buiten scope`).
Drie weigeringen, geen antwoord om te vergelijken — dus geen enkele
differentiaalpoort vangt hem. Twee posities (hele regelwaarde, operand van
een `Binair`) gemeten IDENTIEK, dus één beslissing voor beide. Nul
voorkomens in alle 546 documenten.

**Precisering.** "de frontend emitteert exact dit document" (NOTE van
`wf034-sentinel-gelest-wratoperand`) is nagemeten blad voor blad: de emissie
en het gepinde document verschillen in **exact drie bladeren**, geen ervan het
gebrek — de twee `bron`-provenance-strings en de `decimalen` van de vouwknoop
zelf (0 tegen 2). De wratoperand zelf is byte-identiek, en de eigen emissie
van de frontend wordt door dezelfde clausule op dezelfde pointer met dezelfde
melding geweigerd.

**Dertien gates, alle groen** (gemeten vóór de rebase, op de toenmalige
corpusstand van 540 documenten): 1 PASS; 2 = 178; 3 = **178 golden / 332
negatief**; 4 = 431; 5 = 409 (2 zonder case); 6 = 409; 7 = 373/36; 8 = 22
(Drools-leg 5/17); 9 = 201/12; 10 = 173/31; 11 = 45/2/27/29 **onbewogen**;
12 = played 110 / channel floor 46 / auto-excluded 43 / value-divergence 1 /
no-case 2 / JUnit 207; 13 = 74/15/0. De stand ná de rebase staat onderaan.

**Aangeraakt in de fixronde:** `spec/IR-SPEC.md`,
`spec/verifier/lib/checks.ml` (één predicaat, één arm, drie
commentaarblokken), drie nieuwe negatiefs + vier bijgewerkte NOTEs,
`backlog/new/20260906-set-buiten-vouw.md`, deze kaart. **Niet aangeraakt:**
`spec/CONSTRUCTS.md`, `spec/PLAN.md`, `ref/**`, `backends/**`, `scripts/**`,
`toka/**`; `frontends/**` alleen gelezen en één keer gedraaid voor de
producentmeting (`make compile` + `--ir`, alles in gitignored build-mappen).

---

Rebase op main `2634463` (2026-09-06, dezelfde worktree, ná de fixronde):

**De rebase liep CONFLICTVRIJ.** Twaalf commits opnieuw toegepast, geen
enkele conflicthunk. De twee voorziene raakvlakken bleken disjunct in de
regels die ze aanraken: in `spec/IR-SPEC.md` staat mijn WF-034-indexrij
(r. 5136) los van de nieuwe WF-095-rij (r. 5192) en zijn mijn ch.-4.3/4.6-
passages disjunct van hun ch.-4.1-passage; in `spec/verifier/lib/checks.ml`
zit hun WF-095-arm in de Literal-walk (r. ~239 en ~3743) en zitten mijn
golf-7-armen in de vouwargument-tak (r. ~4205-4230). Repo-breed op
conflictmarkers gegrepen ná afloop: de enige treffers zijn de decoratieve
`====`-linialen in `LICENSE-CC-BY-4.0`, geen conflictmarkers. Rijvolgorde
in de index blijft WF-034 vóór WF-093/094/095, zoals gevraagd.

**Hermeten na de rebase.**

- De noemer van de indexrij is **onveranderd 43** `v "WF-034"`-emissieplekken
  — de datumtijd-branch raakte WF-034 niet, en dat is gemeten, niet aangenomen.
- De vier zones zijn **157 golden + 22 rood + 337 negatief + 30 pariteit =
  546**. Alle plekken waar mijn teksten een corpusstand claimen zijn
  bijgewerkt en gedateerd: de NOTEs (546), ch. 4.3 flank (4) en (5), de
  K5-kaart, en het zonegroei-grootboek in de indexrij, dat nu de stand noemt
  waarop deze golf LANDT (negatief 330 → 337, vier zones 539 → 546) en de
  pre-rebase-stand (325 → 329, 533 → 537) als historische regel houdt.
- **Injectieproef hermeten**: 157 goldens, **2813** injecties, 15 no-ops,
  **0** overlevers ná arm (r) en **10** ervóór — dezelfde tien geleste
  operanden in dezelfde twee goldens. Main's nieuwe golden voegt negen
  posities toe en geen enkele overlever.
- **Sentinel-census hergeteld**: 71 `type`-posities (was 65), 66 op een
  vouwargument (was 61), 62 enkelvoudige wratten (was 60) en **4** geleste —
  de drie getuigen van deze golf plus dat ENE pariteitsoperand. Die
  invariant, niet de totalen, draagt de claim.
- **Nulcensus en builtin-census onveranderd**: drie waardeknopen met een
  expliciete `null` op `type`, alle in al-afgewezen negatiefs, **nul** op een
  vouwargument; **nul** niet-aggregatie-`Functie` met `elementObjecttype`
  buiten de twee eigen getuigen.

**De drie rood-eerst-getuigen en alle zeven mutanten opnieuw gedraaid op de
gerebasede kop.** Elke uitslag reproduceert getal voor getal (M1 zes vrij /
0 nieuw / 1 pariteitsregel; M2 vier vrij / 12 nieuw / 29 bijvangst; M4 nul /
nul / vier tweede-citaten; M5 één vrij / 1 pariteitsregel; M6 precies twee
vrij; M7 precies één vrij; PRE precies drie). Alle md5's in de NOTEs en in
de mutantentabel hierboven zijn die van de gerebasede kop; de PASS-vóór-md5's
(`2ab63519…` voor de golf, `c95650b1…` voor de fixronde) blijven staan als
HISTORISCHE meting met datum, want een md5 verschuift bij elke
commentaarwijziging in `checks.ml` en zegt alleen iets samen met de kop
waarop hij gemeten is.

**Nieuwe verifier-md5 op de gerebasede kop:
`8ea45e866dddcf861b357b430d50b4d1`.**

**Dertien gates op de gerebasede kop, alle groen** (vanuit deze worktree, ná
commit; `sh`/`bash` per shebang, `eval $(opam env)`, gates 12 en 13 hebben de
jar herbouwd): 1 PASS; 2 = **179**; 3 = **179 golden / 337 negatief**;
4 = **434**; 5 = **412** (2 zonder case: `jutterswet-seizoenvondst`,
`jutterswet-vormenkabinet`); 6 = **412**; 7 = **376**/36; 8 = 22 (Drools-leg
5/17); 9 = 201/12; 10 = 173/31; 11 = 45/2/27/29 **onbewogen**;
12 = played **111** / channel floor 46 / auto-excluded 43 /
value-divergence 1 / no-case 2 / JUnit 207; 13 = 74/15/0. Daarnaast:
`test_ref` 445 gewerkte voorbeelden, `test_clingo` 80 checks, beide groen.
`git status --short` leeg; de branch blijft ONGEMERGED (beschermde paden).
