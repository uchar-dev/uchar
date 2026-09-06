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
