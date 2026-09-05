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

*Regelnummerconventie: alle `~:`-verwijzingen in dit Result wijzen naar
`backends/clingo/lib/translate.ml` op de merge-base `ec7d188`
(5743 regels); de kaart laat het bestand met 54 regels groeien, dus op
de kop staan ze hoger. De Goal hierboven is tegen een oudere revisie
geschreven — dat verklaart het verschil tussen zijn `~:1682`/`~:1473`/
`~:1358` en het `~:1706`/`~:1497`/`~:1371` hieronder: dezelfde drie
armen, twee revisies.*

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

**Het lek was breder dan de filterloze route.** De onvernauwde arm
raadpleegde `filter` niet — zijn `_` slikte het veld — dus ook een
`gedurende`-totaal viel erin zodra zijn operand de zusterarm miste, en
dan verdween de clip stil. Nagemeten op een eigen bron (Vondst met een
eigen temporeel `bewaartarief`, temporeel kenmerk `geborgen`, regel
`het totaal van zijn bewaartarief gedurende de tijd dat het geborgen
is`; J1 = 10, V1 = 99), telkens dezelfde case:

```
                              vóór (merge-base)          ná (deze kaart)
aot = subjecttype (ongedraaid) luide "needs the full     ongewijzigd, luid
                               timeline"
aot vreemd (Jutter, geen nav)  MISMATCH = [99]           luide TRANSLATION FAULT
                               — de clip stil weg          (vreemd objecttype)
operand genavigeerd            MISMATCH = [99]           luide TRANSLATION FAULT
                               — de clip stil weg          (genavigeerd)

ref op alle drie: de eerste luid "needs the full timeline", de twee
gedraaide "unsupported instance-fold source shape" — nooit een waarde.
```

Elf van de veertien temporele `TOTAAL` in het corpus dragen een filter
(geteld over alle 486 `ir.json` in `corpus/` en `toka/`), dus dit was
de meerderheidsvorm — al draagt geen van die elf een gedraaid operand.

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

  **De weigervolgorde is een keuze, geen toeval:** set wint van
  navigatie, navigatie van vreemd objecttype. Een vorm die twee gronden
  draagt krijgt dus de bovenste. Gemeten: `set = true` mét `navigatieRol`
  krijgt de §7.1-set-reden; `navigatieRol` met vreemd `objecttype`
  krijgt de navigatie-reden. Zo staat de meest structurele reden voorop
  (de set-vouw is een andere vouwvorm, niet een verkeerd gekozen
  instantie), en de tekst blijft één reden per melding.

  **Eén oneerlijkheid, eerlijk genoemd: het temporaliteitsanker is niet
  consistent.** Deze weigerarm hangt zijn `attr_temporal`-guard aan het
  operand-objecttype `aot`; de `Som`- en `Minimum`/`Maximum`-guards (in
  de armen op :1293 en :1371) hangen hem aan `el`, het
  `elementObjecttype`.
  Op elke vorm die een frontend produceert vallen die samen. Op een
  gemengde annotatie niet, en dan valt de §7.1-set-vouw redenloos door.
  Gemeten met een derde objecttype `Kist` dat `bewaartarief`
  níét-temporeel declareert (`aot = "Kist"`, `elementObjecttype =
  "Vondst"` temporeel, `set = true`):

  ```
  clingo: TRANSLATION FAULT: expression kind not translated
          (no corpus case exercises it)
  ```

  Luid, dus geen stil gat — maar het noemt de vorm niet. Welk anker het
  juiste is hangt af van wat de set-vouw straks vertaalt, dus die keuze
  hoort in de kaart die hem vertaalt:
  `backlog/new/20260905-clingo-setvouw-71.md` neemt hem over, met deze
  gemengde annotatie als getuige.
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
- **De overige `Attribuut_ref`-armen die subject-verankerd lezen — al
  vernauwd, met twee bewuste uitzonderingen.** Acht armen (:961, :2431,
  :2996, :3043, :3102, :3157, :3214, :4387) dragen de volle drieslag
  `navigatie_rol = None; navigatie_pad = []; navigatie_bindingen = None;
  set = false` plus `when norm ot = norm subject_ot`. "Subject-verankerd
  lezen", niet "`val(<subject>,…)` emitteren": :4387 emitteert
  `heeft(<subject>,…)` en :3214 een `alist(<subject>,…)`-lijst-id — de
  ankering is dezelfde, de kop niet.

  Twee posities lezen even subject-verankerd maar dragen
  `navigatie_bindingen` **bewust niet**: het pro-rata-operand (:4290) en
  de `Temporal_read`-basis (:1737). Beide dragen wél de rest van de
  drieslag en de when-guard. De reden is gemeten, en het is dezelfde
  reden als bij de vernauwde `Totaal`-arm zelf: bindingen zonder
  `navigatie_rol` laten ref het subject lezen, dus de executors zijn het
  eens en een weigering hier zóu de divergentie zijn. Nagemeten door
  `navigatieBindingen` in `anchor/jutterswet-borgdeel` (pro-rata) en
  `anchor/jutterswet-aanmeldquotum` (`TemporalRead`) op de wire te
  zetten:

  ```
  met bindingen, arm ongewijzigd : ref 7/7 PASS, clingo 7/7 PASS
                                   (borgdeel 6 cases + aanmeldquotum 1)
  met bindingen, arm vernauwd    : clingo weigert alle 7, ref antwoordt
                                   nog steeds de gepinde waarden
  ```

  Uniform vernauwen zou dus zeven vandaag-exacte replays laten
  divergeren. En de vorm is er niet eens: **0 van de 486 `ir.json` in
  `corpus/` en `toka/` draagt bindingen op een van beide posities**, en
  de verifier wijst hem af zodra hij verschijnt —
  `WF-053 FAIL … navigation bindings on a reference that does not
  navigate`, op alle drie de geïnjecteerde posities. Onbereikbaar,
  gemeten, en de vernauwing zou alleen kosten.

  De temporele `Totaal` miste de eerste twee termen van de drieslag én
  de when-guard; dat wás het lek.

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
Testbestanden raken is voorleggen; de maintainer heeft deze
suite-uitbreiding op 2026-09-05 goedgekeurd.

**Wat die vijf pins wél en niet dekken.** De twee genavigeerde pins
verschillen alleen in het *document* (Vondst deelt de attribuutnaam of
niet — precies de twee documenten die vóór de vernauwing 99 versus leeg
gaven); hun weigermelding is byte-identiek, want de arm interpoleert
alleen de attribuutnaam. Documentdekking dus, geen tekstdekking; de
tweede needle knipt een ander stuk uit dezelfde zin. En
`translates_with` is een niet-faalt-pin: hij controleert dát de
eigen-subjectvorm vertaalt, niet wélke waarde eruit komt. De
waardedekking van die vorm zit in poort 6, over de veertien gedragen
temporele `TOTAAL` in het corpus.

Rood-eerst staat in de history: commit `b8ea8fa` (na de rebase
`b115257`) legt de pin neer tegen de onvernauwde vertaler en is
opzettelijk rood — `test_clingo: 24 checks, 4 FAILURES`, drie ervan
letterlijk `expected a loud fault, got a model` (de stille 99, de
stille leeg, de vreemd-getypeerde lezing) en één op een gewijzigde
reden-tekst. Commit `9ac3eea` (na de rebase `f7a7a66`) vernauwt de arm:
`24 checks, 0 failures`. Beide byte-reproduceerbaar nagemeten met de
merge-base-vertaler onder de ongewijzigde suite.

Het commit-subject van die rood-eerst-commit telt echter verkeerd: het
leest "4 van 5 falen: drie 'got a model', de stille 99 en de stille
leeg" — 3 + 1 + 1 — terwijl het 3 + 1 is. De stille 99 en de stille leeg
*zijn* twee van de drie `got a model`; de vierde failure is de
gewijzigde reden-tekst op de set-vouw. De history blijft staan zoals
hij is; deze regel is de correctie.

### De sweep: 0 pins bewegen

Alle `ir.json` in `corpus/golden`, `corpus/rood`, `corpus/negatief` en
`toka` doorlopen op vouwoperanden (74 enkelvoudige `AttribuutRef`-
operanden op de merge-base; na de rebase 79, want de vierde
WF-034-golf bracht tien negatief-documenten mee waarvan vijf er een
dragen):

- **temporele `TOTAAL`: 14 voorkomens, alle 14 met
  `aot = el = subjecttype`, `set = false`, `navigatieRol = null`,
  `navigatiePad = []`, `navigatieBindingen` afwezig.** Elk daarvan valt
  binnen de vernauwde arm — geen enkele beweegt.
- niet-temporele `TOTAAL`: 2, beide `set = true`, beide zonder
  case-bestand; die raakten de arm al niet en raken hem nog steeds niet.
- genavigeerde vouwoperanden: 4, alle `AANTAL` (de WF-083-familie).
- `aot ≠ elementObjecttype`: 24, alle `AANTAL` (waar `aot` het
  subjecttype is) — geen enkele set-vouw. Na de rebase 25, nog steeds
  alle `AANTAL`: de vierde golf bracht er één bij en geen set-vouw.

De twee `TOTAAL`-regels hierboven staan ook na de rebase zo: 16
enkelvoudige `AttribuutRef`-`TOTAAL`, 14 temporeel en 2 met
`set = true`. Bevestigd door de poorten zelf: op poort 3 na (271 → 281,
de golf) zijn alle tellingen ongewijzigd.

### Poorten (alle dertien groen)

Herdraaid op de kop na de rebase op `main` (73b824a, de vierde
WF-034-golf). Die rebase verliep conflictvrij en raakte
`backends/clingo` niet; alleen poort 3 telt anders, omdat de golf tien
negatief-documenten meebracht (271 → 281). Alle overige tellingen zijn
ongewijzigd.

```
 1 check-self-contained     PASS
 2 check-ir-schema          PASS (175 bestanden valideren en round-trippen)
 3 check-verifier           PASS (175 golden clean; 281 negatief afgewezen)
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

### Fixronde (2026-09-05)

Twee lenzen lazen de landing na. Beide bevestigden hem in de kern — de
64 veldcombinaties over de vier armen sluiten, er is geen doorval meer,
het inertie-argument voor `Som`/`Minimum`/`Maximum` houdt, en de
rood-eerst-commit is byte-reproduceerbaar. De rebase op `main`
(73b824a) verliep conflictvrij en semantisch neutraal: `main` raakt
`backends/clingo` niet, en de wart-versmalling van de vierde
WF-034-golf gaf 0 verdictverschillen over 64 getuigedocumenten. Geen
regel code is in deze ronde veranderd; wat veranderde is de tekst
hierboven.

Gecorrigeerd, elk met hermeting:

- **De grep-lijst-zin was op twee punten onwaar.** Hij telde negen
  posities als "al vernauwd met de volle drieslag"; het zijn er acht.
  Het pro-rata-operand en de `Temporal_read`-basis dragen
  `navigatie_bindingen` *bewust* niet, om precies dezelfde reden als de
  vernauwde `Totaal`-arm zelf. De tegenhypothese is gebouwd en gemeten:
  uniform vernauwen laat zeven vandaag-exacte replays divergeren.
  En "emitteert `val(<subject>,…)`" klopte niet voor twee van de acht —
  `heeft/2` en een `alist/2`-lijst-id; "subject-verankerd lezen" is de
  juiste koepel.
- **De vóór/ná-meting was te smal.** Het lek zat niet alleen op de
  filterloze route: ook een `gedurende`-totaal met een vreemd of
  genavigeerd operand viel door de onvernauwde arm, die `filter` niet
  raadpleegt, en dan verdween de clip stil. Nagemeten, met de meting
  erbij gezet.
- **De weigervolgorde stond nergens** — set wint van navigatie,
  navigatie van vreemd objecttype. Gemeten en benoemd.
- **Het temporaliteitsanker is inconsistent** (`aot` bij de
  `Totaal`-weigerarm, `el` bij `Som`/`MinMax`) en op een gemengde
  annotatie valt de set-vouw redenloos in de catch-all. Gemeten, eerlijk
  genoemd, en de keuze doorgeschoven naar de kaart die de set-vouw
  vertaalt.
- **De suite-dekking was optimistisch beschreven.** De twee
  genavigeerde pins geven een byte-identieke melding (documentdekking,
  geen tekstdekking) en `translates_with` is een niet-faalt-pin.
  Nagemeten en benoemd.
- **Het commit-subject van de rood-eerst-commit telt 3 + 1 + 1 waar het
  3 + 1 is.** De history blijft staan; de correctie staat hierboven.
- **Regelnummers** dragen nu één conventiezin bovenaan dit Result, die
  ook het revisieverschil tussen Goal en Result dekt.
- **De poortentabel** is herdraaid op de gerebasede kop (poort 3:
  271 → 281, de rest gelijk), net als de sweep-tellingen.

Twee vervolgkaarten geschreven, allebei met de metingen van deze ronde
als getuige:

- `backlog/new/20260905-clingo-setvouw-71.md` — de §7.1-set-vouw
  vertalen, zodat `corpus/golden/jutterswet-tempototaal` eindelijk zijn
  case krijgt (samen met de niet-identieke schrijf-eenheid; één van de
  twee alleen laat de golden caseloos). Het temporaliteitsanker wordt
  daar gekozen.
- `backlog/new/20260905-wf083-vouwbron-anker.md` — WF-083 ankert de
  zelfbenoemende rol op het operand-objecttype waar beide executors op
  het vouwsubject ankeren, en geen regel kruist de vouwbron tegen
  subject of `elementObjecttype`. Drie gemeten getuigen, waaronder een
  welgevormd document waarop `ref` 0 zegt en clingo weigert.

De genavigeerde `Som`/`Minimum`/`Maximum`-catch-all blijft zoals hij is
— luid maar redenloos, alleen via wire-chirurgie bereikbaar — en staat
in de out-of-scope van de eerste vervolgkaart.

De suite-uitbreiding van deze kaart raakt een testbestand; de maintainer
heeft daar op 2026-09-05 mee ingestemd.

Alle dertien poorten zijn na deze ronde opnieuw groen gedraaid.
