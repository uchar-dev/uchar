# De §7.1 set-vouw vertalen in backends/clingo — en de caseloze golden zijn case geven

Goal: `backends/clingo` weigert de §7.1 per-element-vouw sinds
2026-09-04 luid (`backlog/done/20260904-clingo-setfold-armen.md`). Die
weigering is eerlijk maar tijdelijk: ze staat tussen
`corpus/golden/jutterswet-tempototaal` en het case-bestand dat die
golden hoort te dragen. De NOTE van die golden draagt de handberekende
absolute waarden al — `J1.bewaarlast = 19`, `J1.bewaarsom = 19`,
`J2.bewaarlast = 0` (§4.20: een lege totaal is onvoorwaardelijk 0),
`J2.bewaarsom = leeg` (§4.10: een lege som zonder terugvalwaarde) plus
`consistent` — en zegt er in zoveel woorden bij dat de case op de
executor wacht. Deze kaart is die executor.

**Twee dingen moeten samen landen, anders blijft de golden caseloos.**
De NOTE meet het zelf: clingo weigert *beide* routes van dat document,
elk om een eigen reden.

1. de set-vouw-`Totaal` zelf — vandaag de luide §7.1-weigering;
2. de **niet-identieke schrijf-eenheid** — `TRANSLATION FAULT:
   non-identical units under the write to "bewaargeld" are not
   translated (ref restates with an exact conversion factor this
   integer encoding does not carry)`. Juist bij deze vouw is de
   schrijf-eenheid per constructie níét identiek (`€/mnd` in, `€` uit,
   de §4.20-noemerstrip van de tijdaggregatie).

Eén van de twee alleen laat de golden caseloos; poort 5 telt hem dan
nog steeds bij de "geen case-bestand"-lijst.

**Overneembare metingen** (deze kaart hoeft ze niet over te doen,
alleen te bevestigen dat ze nog staan — geteld over alle 486 `ir.json`
in `corpus/` en `toka/`):

- 79 enkelvoudige `AttribuutRef`-vouwoperanden in totaal (74 op de
  merge-base van de vouwarmen-kaart; de vierde WF-034-golf voegde tien
  negatief-documenten toe waarvan vijf er een dragen);
- 2 niet-temporele set-`TOTAAL` (beide `set = true`, beide zonder
  case-bestand — de twee die deze kaart moet gaan dragen);
- 14 temporele `TOTAAL`, waarvan 11 met filter (de `gedurende`-clip);
  alle 14 vallen vandaag binnen de vernauwde eigen-subject-arm.

**Twee dingen die vóór het pinnen verantwoord moeten zijn.**

*Ten eerste: ref antwoordt 0 op de §7.1-vorm in een minimaal frame.*
Gemeten (Vondst-subject, set-vouw over `bewaartarief` van de leden,
`elementObjecttype` gezet, geen `navigatieRol`):

```
attribuut temporeel     : verifier PASS | ref V1.bewaarlast = 0
                          | clingo luide §7.1-weigering
attribuut niet-temporeel: verifier PASS | ref V1.bewaarlast = 0
                          | clingo catch-all (redenloos)
```

Waar de per-element-tijdvouw 10 zou geven zegt ref 0. Een 0 is geen
leeg en geen weigering; vóór deze kaart een waarde pint moet vaststaan
of dat ref's bedoelde antwoord is (§4.20's onvoorwaardelijke 0 op een
lege bron, hier over een niet-lege bron) of ref's eigen gat. De tweede
regel is bovendien ref ≢ clingo onder een verifier-clean document — de
getuige die `backlog/new/20260905-wf083-vouwbron-anker.md` als derde
opvoert.

*Ten tweede: het temporaliteitsanker.* De §7.1-weigerarm van de
`Totaal` hangt zijn `attr_temporal`-guard aan het operand-objecttype
`aot`; de `Som`- en `Minimum`/`Maximum`-guards hangen hem aan `el`, het
`elementObjecttype`. Op elke frontend-vorm vallen die samen; op een
gemengde annotatie niet. Gemeten getuige, over te nemen: een derde
objecttype dat het gevouwen attribuut níét-temporeel declareert
(`aot` daarheen gedraaid, `elementObjecttype` temporeel, `set = true`)
valt redenloos in de catch-all —
`TRANSLATION FAULT: expression kind not translated (no corpus case
exercises it)`. Welk anker het juiste is volgt uit wat de set-vouw
vertaalt, dus die keuze hoort hier; deze vorm is de getuige die hem
vastlegt.

Acceptance:
- **Rood-eerst:** het case-bestand van de golden
  `jutterswet-tempototaal` landt eerst, met de vier NOTE-waarden plus
  `consistent`, en poort 6 is er meetbaar rood op vóór de vertaalslag.
- `ref` ≡ clingo ≡ Drools spelen die case met exact dezelfde
  verwachtingen (de differentiële vierkant-eis van poorten 5, 6 en 12).
- De keuze van het temporaliteitsanker staat in het Result, met de
  gemengde-annotatievorm als gepinde getuige.
- Alle dertien gates groen; poort 5 telt één caseloze golden minder.

May touch: `backends/clingo/**`,
`corpus/golden/jutterswet-tempototaal/**`, `backlog/`.

Out of scope: `ref/` en `backends/drools` aanpassen — die dragen de
vormen van deze golden al (de NOTE meet `ref: oracle: PASS` op het hele
document), dit is clingo's inhaalslag; de §4.20-noemerstrip (besloten 2026-09-04, de
NOTE is de vastlegging); de `EERSTE`/`LAATSTE`-set-vorm (vier
voorkomens, alle in `corpus/negatief` — een eigen kaartje als er ooit
een executorpad voor komt); en de **genavigeerde `Som`/`Minimum`/
`Maximum`-catch-all**, die blijft zoals hij is. Die weigering is luid
maar redenloos (`expression kind not translated (no corpus case
exercises it)`) en alleen via wire-chirurgie bereikbaar — geen frontend
emitteert de vorm. Wel gemeten en het noemen waard: de verifier vangt
hem maar half. Een geschreven rol die de *operand*-type zelfbenoemt
(`navigatieRol "bewarende jutter"` op `objecttype "Jutter"`) is
verifier-PASS; een die dat niet doet (`"vondst"` op `"Jutter"`) is
WF-083 FAIL. Dat is het anker dat
`backlog/new/20260905-wf083-vouwbron-anker.md` verplaatst, niet deze
kaart.
