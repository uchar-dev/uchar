# De clingo-vouwarmen vernauwen: één opruiming voor het verkeerd-object-lek

Goal: De filterloze temporele `Totaal`-arm van
`backends/clingo/lib/translate.ml` (~:1682) matcht te ruim. Hij eist
**niet** `navigatie_rol = None`, **niet** `navigatie_pad = []`, en
**niet** de ci-controle `norm aot = norm subject_ot` die zijn zusterarm
met de `gedurende`-clip (~:1473) wél draagt — terwijl hij vervolgens
`val(<subject>, …)` emitteert. Op een **genavigeerde** temporele totaal
leest hij daardoor het attribuut van het VERKEERDE object. Gemeten
(2026-09-04, eigen bron in `/tmp`, jaargranulariteit; `Vondst.bewaarlast
= het totaal van het bewaartarief van de bewarende jutter`, J1 = 10,
V1 = 99):

```
met naamdeling (Jutter én Vondst dragen 'bewaartarief'):
  ref    : EVALUATION FAULT: unsupported instance-fold source shape
  clingo : MISMATCH: V1.bewaarlast = [99], expected 10      <- het VERKEERDE object

zonder naamdeling (Vondst draagt 'vondsttarief'):
  ref    : EVALUATION FAULT: unsupported instance-fold source shape
  clingo : MISMATCH: V1.bewaarlast is underived (leeg), expected 10
```

Twee stille antwoorden waar de andere executor luid weigert: het ene
fout, het andere leeg. Pre-existent — ouder dan de
`ref-tijdtotaal`-kaart, die de arm alleen een `set`-guard gaf.

**Rood-eerst, gereproduceerd bij de claim van deze kaart** (eigen bron
`/tmp/setfold/{a-naamdeling,b-geen-naamdeling}.rgs`, door gate 10's
frontend naar IR geëmitteerd; één Jutter J1 met `bewaartarief` = 10 op
het penningjaar, één Vondst V1 met 99, `Eén bewarende jutter bewaart
meerdere vondsten`, verwachting `V1.bewaarlast = 10`):

```
variant a (Jutter EN Vondst dragen 'bewaartarief')
  ref    : EVALUATION FAULT: unsupported instance-fold source shape
  clingo : MISMATCH: V1.bewaarlast = [99], expected 10
variant b (Vondst draagt 'vondsttarief')
  ref    : EVALUATION FAULT: unsupported instance-fold source shape
  clingo : MISMATCH: V1.bewaarlast is underived (leeg), expected 10
```

Woord voor woord de meting uit de kaartkop — de vorm is stabiel en de
wire-vorm die hem draagt is `Functie{kind: TOTAAL}` met een operand
`AttribuutRef{objecttype: "Jutter", navigatieRol: "bewarende jutter",
set: false}` onder subject `Vondst`.

Dezelfde klasse zit op de armen die de fixronde van die kaart nu
temporeel bewaakt: de `Som`-arm (~:1293) en de `Minimum`/`Maximum`-arm
(~:1358) matchen `navigatie_rol = None, navigatie_pad = []` wél, maar
leiden hun rol af uit `subject_ot`/`el` zonder de ci-controle die de
`gedurende`-arm draagt. Eén kaart, één vernauwingspatroon, alle armen
langs.

Acceptance:
- Rood eerst: de twee metingen hierboven staan als **vóór**-regel in de
  kaart; ná de vernauwing weigert clingo beide vormen luid met een
  reden-dragende melding, of vertaalt ze correct (10). Nooit meer stil
  fout, nooit meer stil leeg.
- De `Totaal`-arm draagt `navigatie_rol = None`, `navigatie_pad = []` en
  `norm aot = norm subject_ot` — letterlijk de vernauwing van de
  `gedurende`-zusterarm, zodat de twee armen op dezelfde vorm matchen.
- Elke vouwarm met een `set`- of objecttype-dragend operand is
  nagelopen op dezelfde drie punten; wat niet vernauwd wordt draagt een
  reden in commentaar.
- Getuige: een golden of, waar poort 6 geen weigerkanaal heeft, een
  `ref/test`- of `backends/clingo/test`-pin die de vorm vastlegt.
- Alle dertien gates groen; geen bestaande pin beweegt (gemeten
  verwachting: 0 — geen corpus- of toka-golden draagt een genavigeerde
  temporele totaal).

May touch: `backends/clingo/**`, `corpus/golden/**` (nieuwe getuige),
`backlog/`.

Out of scope: de §7.1 set-vouw écht vertalen (`Totaal`, `Som`,
`Minimum`/`Maximum` over een temporeel attribuut) — dat is een eigen
kaart en de armen weigeren die vorm sinds 2026-09-04 luid; `ref/`;
`toka/`; de frontends.

---

## Result (2026-09-04)

**Geland.** Eén arm vernauwd, één weigerarm erbij, twee armen na meting
bewust ongemoeid, vijf pins in de eigen suite. Geen corpusbestand
aangeraakt; de diff raakt `backends/clingo/lib/translate.ml`,
`backends/clingo/test/test_clingo.ml` en deze kaart, verder niets.

### De meting, vóór en ná

```
variant a — genavigeerde temporele totaal, MET naamdeling
  ref     : EVALUATION FAULT: unsupported instance-fold source shape
  vóór    : MISMATCH: V1.bewaarlast = [99], expected 10       <- het verkeerde object
  ná      : TRANSLATION FAULT: the temporal total of the NAVIGATED "bewaartarief"
            is not translated: the carried arm totals the SUBJECT's own timeline,
            so it would read "bewaartarief" off the subject instead of off the
            navigated object — a wrong value where the two types share the
            attribute name, a silent leeg where they do not; ref refuses the shape
            outright (an instance-fold source it does not carry), so this arm must
            not answer either

variant b — dezelfde vorm, ZONDER naamdeling
  ref     : EVALUATION FAULT: unsupported instance-fold source shape
  vóór    : MISMATCH: V1.bewaarlast is underived (leeg), expected 10
  ná      : dezelfde reden-dragende TRANSLATION FAULT
```

Beide stille antwoorden zijn weg. Vertalen was niet de goede uitkomst:
ref weigert de vorm zelf, dus een clingo die hem wél beantwoordt zou de
divergentie alleen omdraaien.

### Per arm

- **`Totaal`, temporeel (was ~:1706) — VERNAUWD.** Draagt nu
  `navigatie_rol = None`, `navigatie_pad = []`, `set = false` en
  `norm aot = norm subject_ot`: letterlijk het patroon van de
  `gedurende`-zusterarm (~:1497) minus diens filter, en letterlijk ref's
  eigen temporele-totaalarm. De vernauwing is dragend omdat de body
  `val(<SUBJECT>,attr,V)` emitteert. `navigatie_bindingen` is *niet*
  toegevoegd: met `navigatie_rol = None` leest ref óók het subject, dus
  een weigering daar zou juist de divergentie zijn. Reden in commentaar.
- **Nieuwe weigerarm eronder — TOEGEVOEGD.** Vangt elke temporele
  `Totaal` die de vernauwing uitsluit, met een eigen reden per vorm: de
  set-vouw (§7.1, de tekst die eerst in de arm zelf zat), de
  genavigeerde lezing, en het vreemde objecttype zonder navigatie. Alle
  drie gemeten en luid.
- **`Som` over een set (~:1293) — NIET vernauwd, gemeten reden.** De arm
  emitteert nooit `val(<subject>,…)`; elke lezing hangt aan de
  ELEMENT-variabele en de declaratie wordt op `el` opgezocht — precies
  zoals ref (diens set-bronarm bindt evenmin een `objecttype`). Het
  `objecttype` van de operand is dus bij **beide** executors inert, en
  een controle erop kan alleen vormen weigeren die ref beantwoordt.
  Gemeten door dat annotatieveld te draaien naar een naamdelend zustertype,
  mét en zonder expliciete `bronRol`: ref en clingo blijven allebei de
  gepinde waarde geven (2 van 2). Wat de draai wél breekt — een
  `elementObjecttype` dat het gevouwen attribuut niet declareert — is aan
  beide kanten al luid (een case-laadfout, of `attr_decimals`' "unknown
  attribute"), nooit een stille waarde. Reden in commentaar.
- **`Minimum`/`Maximum` over een set (~:1371) — NIET vernauwd, zelfde
  reden.** Zelfde draai op `jutterswet-vondstpiek`: ref en clingo
  allebei PASS (9 verwachtingen). De `elementObjecttype`-draai geeft
  ref PASS tegen een luide clingo-`TRANSLATION FAULT: unknown attribute
  "gewicht" on "Jutter"` — een divergentie, maar luid, en alleen
  bereikbaar via een wire-mutatie die geen frontend produceert.
  Terugverwijzing in commentaar.
- **`Aantal` (~:1247) — NIET vernauwd.** Bindt helemaal geen
  `objecttype` en spiegelt ref's rolbronarm veld voor veld (inclusief de
  WF-083 `self_denoting_role`-poort). Genoemd in hetzelfde commentaar.
- **De overige `Attribuut_ref`-armen die `val(<subject>,…)` emitteren
  (:961, :2431, :2996, :3043, :3102, :3157, :3214, :4387, en :4290's
  pro-rata) — al vernauwd.** Elk draagt de volle drieslag
  `navigatie_rol = None; navigatie_pad = []; navigatie_bindingen = None;
  set = false` plus `when norm ot = norm subject_ot`. De temporele
  `Totaal` was de enige die hem miste; dat wás het lek.

### De doorval

Gemeten door de nieuwe weigerarm tijdelijk te verwijderen: de
genavigeerde vorm, de set-vorm en de vreemd-getypeerde vorm vallen dan
alle drie door naar de catch-all van `expr_term` —
`TRANSLATION FAULT: expression kind not translated (no corpus case
exercises it)`. Luid, dus geen stil gat, maar redenloos: hij noemt de
vorm niet en zegt niet waarom. Vandaar de expliciete weigerarm; de
catch-all blijft ongewijzigd achter de hand.

### De getuige

**Geen corpus-golden mogelijk, en dat is de gemeten waarheid, geen
verzwegen gat.** Poort 6 heeft geen weigerkanaal en `ref` weigert de
vorm zélf (`unsupported instance-fold source shape`), dus een
golden-case zou poort 5 én 6 rood maken. `backends/clingo` draagt wél
eigen testinfra (`backends/clingo/test/test_clingo.ml`, door
`scripts/check-clingo.sh` gedraaid met een niet-nul checktelling), dus
de weigering is dáár gepind — zoals de S19-deuren. Vijf checks erbij
(19 → 24): drie weigerteksten (genavigeerd mét en zonder naamdeling,
set-vouw, vreemd objecttype — vier `faults_with`) plus één positieve
controle die de eigen-subjectvorm vasthoudt, zodat een óververnauwing
hier faalt en niet pas in poort 6.

Rood-eerst staat in de history: commit `b8ea8fa` legt de pin neer tegen
de onvernauwde vertaler en is opzettelijk rood —
`test_clingo: 24 checks, 4 FAILURES`, drie ervan letterlijk
`expected a loud fault, got a model` (de stille 99, de stille leeg, de
vreemd-getypeerde lezing) en één op een gewijzigde reden-tekst. Commit
`9ac3eea` vernauwt de arm: `24 checks, 0 failures`.

### De sweep: 0 pins bewegen

Alle `ir.json` in `corpus/golden`, `corpus/rood`, `corpus/negatief` en
`toka` doorlopen op vouwoperanden (74 enkelvoudige `AttribuutRef`-
operanden):

- **temporele `TOTAAL`: 14 voorkomens, alle 14 met
  `aot = el = subjecttype`, `set = false`, `navigatieRol = null`,
  `navigatiePad = []`, `navigatieBindingen` afwezig.** Elk daarvan valt
  binnen de vernauwde arm — geen enkele beweegt.
- niet-temporele `TOTAAL`: 2, beide `set = true`, beide zonder
  case-bestand; die raakten de arm al niet en raken hem nog steeds niet.
- genavigeerde vouwoperanden: 4, alle `AANTAL` (de WF-083-familie).
- `aot ≠ elementObjecttype`: 24, alle `AANTAL` (waar `aot` het
  subjecttype is) — geen enkele set-vouw.

Bevestigd door de poorten zelf: alle tellingen ongewijzigd.

### Poorten (alle dertien groen)

```
 1 check-self-contained     PASS
 2 check-ir-schema          PASS (175 bestanden valideren en round-trippen)
 3 check-verifier           PASS (175 golden clean; 271 negatief afgewezen)
 4 check-case-schema        PASS (425 case-bestanden valideren)
 5 check-oracle             PASS (403 cases exact herspeeld; 3 golden zonder case)
 6 check-clingo             PASS (403 cases exact; eigen suite 24 checks, 0 failures)
 7 check-noop               PASS (367 weerleggen; 36 gepinde overlevers)
 8 check-rood               PASS (22 rode cases; Drools-been 5 fault-pinned, 17 not-carried)
 9 check-catala             PASS (201 checks; 12 catala-golden byte-identiek)
10 check-regelspraak        PASS (171 JUnit-tests; 29 rgs-golden byte-identiek)
11 check-toka-parity        PASS (ref 29/29, clingo 29/29, Drools 29 PLAYED)
12 check-drools             PASS (106 played; 1 value-divergence; JUnit 199)
13 check-reftype-derivation PASS (52 twists over 11 golden; 50 replays; 0 overlevers)
```

### Afwijking van de kaart

Eén, en in het voordeel van de kaart: de kaart bood `corpus/golden/**`
aan voor een nieuwe getuige. Die is er niet gekomen — niet uit
zuinigheid maar omdat geen executor de vorm draagt, dus de suite-pin is
de enige eerlijke plek. `corpus/` is ongemoeid gebleven. De §7.1
set-vouw écht vertalen blijft, zoals afgesproken, een eigen kaart.
