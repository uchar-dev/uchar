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
(540 documenten) draagt **geen enkel** document vandaag een `AttribuutRef`
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
