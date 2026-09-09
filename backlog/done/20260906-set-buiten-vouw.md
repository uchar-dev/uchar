# `set: true` op een gewone lezing BUITEN een vouw: welgevormd of niet?

Goal: De WF-034-golf-7-kaart
(`backlog/done/20260906-sentinel-op-leespositie.md`) beslechtte het
ONBEKEND-TYPE op elke leespositie. Daarmee bleef één veld van dezelfde
knoop onbeslist: **`set: true`**. Een `AttribuutRef` met `set: true`, een
ECHT datatype en géén omhullende vouw is vandaag verifier-SCHOON terwijl
**geen enkele uitvoerder hem draagt** — en de RegelSpraak-frontend emitteert
hem, zonder één diagnostiek. Dat is de PLAN-§6-klasse in haar andere
gedaante: geen twee-tegen-één-divergentie maar een welgevormd document dat
nergens uitvoerbaar is, en dus een spec die iets toelaat waarover ze niets
zegt. Deze kaart beslist of de vorm ill-formed is (een WF-clausule, met
negatiefs) of een geldige leesvorm (dan moeten `ref` en de ASP-backend hem
dragen, met een getuige-golden met handberekende pins).

## Wat er gemeten is (2026-09-06, tijdens de golf-7-fixronde)

**De vorm bestaat in het echt, uit de producent.** Uit
"Het maatgevend gewicht van een Jutter moet berekend worden als het gewicht
van **alle** vondsten van de Jutter." emitteert de frontend, met
`diagnostics: []`:

    {"node":"AttribuutRef","attribuut":"gewicht","objecttype":"Vondst",
     "onderwerpBron":"alle vondsten van de Jutter","navigatieRol":null,
     "type":{"node":"Numeriek","geheel":false,"decimalen":2,…},
     "bron":"het gewicht van alle vondsten van de Jutter","set":true,
     "navigatiePad":[],
     "navigatieBindingen":[{"node":"RolBinding","feittype":"vondstaanmelding",
                            "rol":"vondst"}]}

Een ECHT type (geen sentinel — dus WF-093 raakt hem niet), een echte
rolbinding, en de knoop staat als hele regelwaarde, niet als vouwargument.

**De drie uitvoerders, op precies dat document:**

| | uitkomst |
|---|---|
| verifier | **PASS** |
| `ref` | `EVALUATION FAULT: unsupported AttribuutRef shape (set/sentinel outside a fold)` |
| ASP-backend | `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)` |
| Drools | **EXCLUDE** transpiler SKIP: `regel (waarde buiten scope)` |

Drie weigeringen, drie verschillende soorten, één schoon document. Geen
antwoord om te vergelijken, dus ook geen differentiaalvierkant dat het vangt:
gate 5/6 zien het document niet (het heeft geen golden-case), gate 12 telt het
als auto-uitsluiting.

**Twee posities, gemeten identiek.** Als hele regelwaarde en als
linkeroperand van een `Binair` (beide handgebouwd op hetzelfde model): allebei
`verifier: PASS`, allebei dezelfde drie weigeringen woord voor woord. Er is
dus geen reden om de posities te splitsen — dit is **één beslissing voor
lezing én operandpositie**, precies zoals golf 7 er één was voor het
onbekend-type.

**De regressiegrens is nul.** Over golden + rood + negatief + pariteit
(546 documenten) draagt **geen enkel** document vandaag een `AttribuutRef`
met `set: true` buiten een vouwargument. Een weigerende clausule kan dus per
constructie niets over-afwijzen; een dragende arm heeft per constructie geen
bestaande getuige en moet er een meebrengen.

## De twee te wegen antwoorden

**(i) `set: true` hoort binnen een vouwbron en nergens anders.** Dan komt er
een clausule — bij WF-034 of, waarschijnlijker, bij WF-083 (de vouwbronvorm)
dan wel een eigen nummer — die `set: true` weigert waar geen vouw de
verzameling consumeert. Kosten: negatiefs per positie (regelwaarde,
operandpositie), een arm-exacte mutant, en het antwoord op de vraag wat de
frontend dan met "alle vondsten" moet doen — want die emitteert de vorm
vandaag zonder diagnostiek, dus (i) snijdt een LEVENDE producentroute af en
de frontend moet meebewegen (een eigen kaart, spec-first).

**(ii) `set: true` is een geldige leesvorm.** Dan is `ref`'s
`unsupported AttribuutRef shape (set/sentinel outside a fold)` een gat en
geen wet, en moet er een SEMANTIEK komen: wat IS de waarde van een
verzamelingslezing op een scalaire schrijfpositie? Eén-element-verzameling
naar scalair? Fault bij meer dan één? Leeg → leegwaarde? Die vraag is niet
retorisch — het is de hele kaart, en ze raakt `RUNTIME-SEMANTICS.md`
(ch. 2.10 en de vouwhoofdstukken), `ref`, de ASP-backend en de
gate-12-discipline. Kosten: een arm in `ref`, een ASP-vertaling, een
getuige-golden met absolute pins, en een Drools-beslissing (dragen of
reden-dragend weigeren).

De kaart moet ook wegen of het ANTWOORD verschilt tussen "de verzameling
heeft precies één lid" en "meer dan één" — dat is waar (ii) zijn scherpste
keuze maakt, en waar (i) zijn goedkoopste argument heeft.

Acceptance:
- **Eén beslissing voor BEIDE posities** (hele regelwaarde, operand van een
  samengestelde knoop), expliciet gemotiveerd; de motivering noemt wat de
  andere kant zou hebben gekost.
- Rood-eerst: bij (i) de negatiefs, bij (ii) de getuige-golden — bestaand en
  gemeten vóór de regel/de arm er is, met de verifier-md5 van vóór en ná.
- Elke nieuwe of gewijzigde WF-regel: negatief-case + **arm-exacte mutant**
  (de mutatie maakt precies dat negatief groen en niets anders), gemeten over
  alle vier de zones.
- De producentroute wordt geadresseerd: bij (i) staat er zwart op wit wat de
  frontend in plaats daarvan moet emitteren (en dat is een eigen kaart, geen
  stille wijziging hier); bij (ii) draagt de frontend-emissie de nieuwe
  semantiek ongewijzigd.
- `spec/IR-SPEC.md` ch. 4.3 flank **(5)** wordt dichtgeschreven dan wel
  herschreven naar wat er dan nog open staat.
- **Alle dertien gates groen.**

May touch: `spec/IR-SPEC.md` (voorstel — beschermd pad),
`spec/RUNTIME-SEMANTICS.md` (voorstel — idem), `spec/verifier/**` (voorstel —
idem), `corpus/negatief/**`, `corpus/golden/**` (nieuwe getuige), `ref/**` en
`backends/clingo/**` alléén bij antwoord (ii), `backlog/`.

Out of scope: `frontends/**` — de emissie is hier BEWIJSMATERIAAL en wordt
alleen gelezen en gedraaid; een frontend-wijziging is een eigen kaart ná deze.
`backends/drools/**` — de transpiler-SKIP is meting, geen doelwit. Het
ONBEKEND-type is beslecht door WF-034 golf 7 en wordt hier niet heropend: deze
kaart gaat over een lezing met een ECHT type, wat precies de reden is dat de
twee vragen orthogonaal zijn en apart gesneden zijn.

---

Result (2026-09-09): antwoord **(i)** — de vorm is ill-formed op de wire.
**WF-096** (eigen nummer; motivering hieronder) landt in `spec/verifier/` en
in `spec/IR-SPEC.md` ch. 4.3: een `AttribuutRef` met `set: true` is alleen
toegestaan als BRON van een eenargument-instantievouw. Eén beslissing voor
beide posities, want beide meten identiek (dezelfde drie weigeringen, woord
voor woord).

*Waarom een eigen nummer en niet WF-034 of WF-083.* WF-083 is normatief
gescopeerd op de vouwBRON — "the argument of a single-argument instance
fold", en de tekst zegt uitdrukkelijk dat de regel niets zegt over een vouw
met twee of meer argumenten; het domein van deze regel is precies het
complement daarvan (elke andere positie), dus WF-083 verbreden zou zijn eigen
scope-zin moeten breken, en die zin is met een golden door vier uitvoerders
gepind. WF-034 gaat over RESOLUTIE tegen declaraties (naam, objecttype,
sentinel-type); `set` resolveert niets. Vandaar WF-096, edge-to-edge met de
twee: bij de bron beslist de sentinel de KIND (WF-093) en beschrijft `set`
alleen; weg van de bron is de sentinel WF-034's zevende golf en is `set` deze
regel.

Geleverd: drie negatiefs (`wf096-setlezing-regelwaarde`,
`wf096-setlezing-operand`, `wf096-setlezing-geleste-operand`), de eerste twee
BYTE VOOR BYTE de emissie van de RegelSpraak-frontend zelf uit twee bronzinnen
met `diagnostics: []` — daarmee is ook de OPERANDpositie een levende
producentroute, niet alleen de hele regelwaarde, wat de kaart nog als
handgebouwd noteerde. De derde is een één-veld-verdraaiing van
`wf034-sentinel-geleste-setlezing` (sentinel → het gedeclareerde type) en pint
de tweede walk (de vouwargumenttak vervangt de generieke expressiewalk).

Rood-eerst: alle drie PASS op de binary van vóór de clausule
(md5 `8e07cdc5515ec6b29bd6de42e750de54`), alle drie precies één falende regel
die WF-096 en de positie noemt op de kop (md5
`5111932e2d1e51de144290ab16abab00`).

Mutant, gemeten over de vier zones (560 documenten — hermeten, gegroeid sinds
de kaart): de hele clausule weg → precies VIER documenten bewegen, de drie
getuigen (elk een verdict) plus één mee-vurende regel op het al afgewezen
`wf034-sentinel-geleste-setlezing`, waarvan het verdict niet beweegt. Elke
helft apart gemuteerd is op zichzelf arm-exact. Over-afwijzing: de 16
legitieme set-vouwbronnen in 14 goldens bewegen niet; geen golden, rood of
pariteitsdocument verandert een zin.

Twee posities die de clausule NIET bereikt, gemeten en benoemd: `set: true` op
een regel-DOEL blijft verifier-schoon (nul documenten dragen het, en alle drie
de uitvoerders NEGEREN het veld en schrijven de scalar — drie-op-drie
overeenstemming op een inerte annotatie, niet deze klasse), en een
vouwargument met een incompleet resolutiepaar meldt WF-034 (de setclausule
staat vóór die armen zodat het veld niet ongemeld blijft).

`ref`'s fout, de ASP-weigering en de Drools-SKIP blijven staan als tweede
verdedigingslinie — de vorm bereikt ze niet meer via een welgevormd document.
`spec/RUNTIME-SEMANTICS.md` is niet aangeraakt: er is geen semantiekclausule
die correspondeert, precies zoals bij WF-093.

Frontend-vervolg: `backlog/new/20260909-frontend-alle-zonder-vouw.md`
(spec-first; de emissie is hier bewijsmateriaal en blijft ongewijzigd).

Dertien gates groen op `41b0bb2`: gate 2 188; gate 3 188 golden / **342**
negatief (339 → 342); gate 4 448; gates 5/6 425; gate 7 389 + 36 survivors;
gate 8 23; gate 9 201 checks / 12 catala-goldens; gate 10 173 tests / 38
rgs-goldens; gate 11 45 re-emissies / 27 tests; gate 12 played 118 / vloer 47
/ JUnit 223; gate 13 74 twists.

**Fixronde na lens 1 (2026-09-09), tekst-only in `spec/IR-SPEC.md`.** Lens 1
reproduceerde de kaart volledig — beide producent-negatiefs byte-identiek uit
zelf-gereconstrueerde bronzinnen, de rood-eerst-md5's exact, beide mutanten
arm-exact, de kwadranten rand aan rand, en de hijsing van de bronpositie-grens
gedragsneutraal (0 verschillen over 560 documenten). Drie tekstpunten
verwerkt, geen gedragswijziging, verifier-binary ongemoeid:

1. *Flank (5), de oorzaak van 0 → 1 gecorrigeerd.* De zin schreef het aan het
   corpus toe; onjuist. `wf034-sentinel-geleste-setlezing` bestond al toen de
   flank nul telde. Wat veranderde is het PREDICAAT: de flank telde
   setlezingen buiten een vouw-ARGUMENT, WF-096 weigert ze buiten een
   vouw-BRON, en een operand van de geleste waardevouw is wél argument en
   géén bron. Herschreven naar die oorzaak.
2. *De normatieve MUST valt nu samen met zijn verifier-check.* "At every
   other position" → "at every other **READ** position", met één zin die
   zegt waarom het woord de scope is en geen slag om de arm: een regelDOEL
   is een schrijfpositie, de clausule bereikt hem niet, en een MUST die
   verder reikt dan zijn check is de spec-variant van "een groene deelverzameling
   is niet groen".
3. *WF-083 wijst nu door.* De registerrij zei nog dat de operanden van de
   geleste waardevouw "ordinary reads governed by WF-053" zijn; daar staat nu
   WF-053 **en** WF-096 bij. Dezelfde veroudering zat in de Companion-regel
   van WF-083's normatieve bullet — dat is precies het pad dat een lezer
   volgt die de omheining vanaf WF-083 naleest — dus die is meegenomen, met
   `wf096-setlezing-geleste-operand` als getuige. Dit is één regel méér dan
   gevraagd, hier expliciet gemeld.

Eén meetverschil opgehelderd in plaats van overgenomen: lens 1 telde 357
regeldoel-posities, deze kaart 783. Beide kloppen onder hun eigen predicaat —
357 is golden + rood (de twee zones die schoon moeten blijven), 783 is alle
vier de zones. Geen van beide draagt `set: true`. De scope-alinea noemt nu
beide getallen mét hun predicaat, wat precies de fout is die punt 1 corrigeert.

Sanity na de fixronde: gate 1 PASS, gate 3 PASS (188 golden schoon / 342
negatief afgewezen voor hun gedeclareerde regel). Tekst-only, dus de overige
elf gates zijn onberoerd en niet opnieuw gedraaid.
