# De rij-1-wrat als bron van een WAARDEvouw: telbron of setbron?

Goal: De v0-instantievouwbron — de rij-1-wrat, `CONSTRUCTS.md` rij 1: een
vouwargument dat een ROL noemt in zijn `attribuut`-slot en het
onbekend-sentinel draagt op zijn eigen `type` — is in de spec beschreven
als de presentatie van "het aantal `<rol>` van de `<onderwerp>`". Een
TELLING dus. Maar niets in de well-formedness zegt dat, en onder elke
andere vouwsoort is dezelfde bron verifier-clean terwijl de executors het
oneens zijn of hem niet dragen. Deze kaart beslecht de vraag: is de wrat
alléén een telbron (dan hoort er een WF-regel die hem in waardeposities
weigert), of is hij een geldige SETbron (dan moet `ref` hem dragen)?
Beide antwoorden zijn machinaal toetsbaar; het huidige "geen van beide"
is de enige stand die dat niet is.

## Gemeten (2026-09-05, `card/wf083-vouwbron-anker` ná de fixronde, eigen
## bron op de `bewaring`-declaraties: Jutter-onderwerp, `elementObjecttype`
## Vondst, twee leden met meldloon 12 en 7)

**Spelling A — de kale wrat** (`set: false`, `attribuut` noemt de
rolmeervoud, `type` = onbekend-sentinel, geen `navigatieRol`):

```
soort              verifier   ref                         ASP-backend        Drools
AANTAL             (n.v.t. — de gedragen vorm; antwoordt 2 in alle drie)
SOM/TOTAAL         PASS       FAULT: value aggregation    FAULT: expression  ERROR
MINIMUM/MAXIMUM    PASS         over a bare role source     kind not          ConstraintEval-
                                                            translated        uationException
EERSTE/LAATSTE     PASS       idem                        idem               EXCLUDE per vorm
```

Zes soorten verifier-clean, nul executors die hem dragen. `ref`'s
foutmelding is precies de these van deze kaart: *value aggregation over a
bare role source* — hij weet dat dit geen waardebron is, en zegt het bij
executie in plaats van bij welgevormdheid.

**Spelling B — de SET-wrat met onderwerp-benoemende `navigatieRol`**
(`set: true`, onbekend-sentinel, `objecttype` = `elementObjecttype`,
`navigatieRol` = een rol die het ONDERWERP vervult):

```
soort              verifier            ref               ASP-backend   Drools
SOM/TOTAAL         PASS (was FAIL)     FAULT: value      FAULT: expr   19
MAXIMUM            PASS (was FAIL)       aggregation       kind not    12
MINIMUM            PASS (was FAIL)       over a bare       translated   7
EERSTE/LAATSTE     PASS (was FAIL)       role source                   EXCLUDE per vorm
```

`was FAIL` = gemeten tegen de verifier van `15d0df4`, vóór de
ankerverplaatsing van `20260905-wf083-vouwbron-anker`: die toetste de
zelfbenoeming tegen het OPERAND-objecttype (`Vondst`), dat de rol
`bewarende jutter` niet vervult. Met het anker op het onderwerp (`Jutter`,
dat hem wél vervult) is de vorm nieuw toegelaten. Dat is geen fout van die
kaart — het anker hoort op het onderwerp, beide executors lezen daar — maar
het legt deze vormvraag bloot, die er los van staat.

**Vier soorten waarop ÉÉN executor antwoordt en twee weigeren onder een
verifier-clean document.** Dat is per PLAN §6 per definitie een spec-bug:
niet omdat een executor ongelijk heeft, maar omdat de spec niet zegt welke
enkelvoudige bronnen er bestaan.

## De te beslechten vraag, en wat elk antwoord kost

**(i) De wrat is alleen een TELBRON.** Dan komt er een WF-regel — hetzij
een clausule (d) onder WF-083, hetzij een eigen nummer — die de
onbekend-getypeerde rolnoemende bron buiten `AANTAL` weigert. Kosten:
vier negatiefs (kaal + set, waardesoorten), een arm-exacte mutant per
clausule, en de regressiegrens over de vier zones. Gemeten vooraf: geen
enkele golden draagt de vorm, dus de grens is naar verwachting nul — de
kaart moet dat tellen, niet aannemen.

**(ii) De wrat is een geldige SETBRON.** Dan is `ref`'s
`value aggregation over a bare role source` een gat en geen wet, en moet
`ref` de vorm dragen: de rol levert de leden, en de vouw leest per lid…
wát? De kale spelling noemt geen attribuut, dus (ii) kan hoogstens gelden
voor spelling B, waar `attribuut` een attribuut van het elementtype noemt.
Kosten: `ref`'s vouwarm, clingo's vertaling, een golden met absolute pins
(handberekend, dan door `ref`), en de vaststelling dat Drools' 19/12/7 de
juiste getallen zijn en niet toevallig dezelfde.

Het vermoeden na meten is **(i) voor spelling A en (ii)-of-(i) voor
spelling B** — maar dat is een vermoeden, en de kaart moet het beslechten
met de differentiaal, niet met smaak.

Acceptance:
- Eén beslissing, expliciet gemotiveerd, voor BEIDE spellingen; de
  motivering noemt wat de andere kant zou hebben gekost.
- Rood-eerst: de negatiefs (bij (i)) dan wel de getuige-golden (bij (ii))
  bestaan en zijn gemeten vóór de regel/de arm er is.
- Elke nieuwe of gewijzigde WF-regel: negatief-case + arm-exacte mutant
  (de mutatie maakt precies dat negatief groen en niets anders), gemeten
  over alle vier de zones.
- De regressiegrens geteld, niet aangenomen: hoeveel wrat-bronnen dragen
  de vier zones, en hoeveel bewegen. Verwachting 0 — geteld op
  2026-09-05: **51** enkelvoudige wrat-vouwbronnen onder een
  `elementObjecttype`, **alle van soort `AANTAL`** (50 met `set: false`,
  waarvan 4 met een `navigatieRol`; de ene met `set: true` is de
  negatief `wf083-vouwbron-anker-op-operand`, al afgewezen). Geen enkele
  waardevouw over een wrat in de hele corpus: de vorm van deze kaart is
  vandaag uitsluitend handgebouwd.
- De flank in `spec/IR-SPEC.md` ch. 4.3 ("Still open at this branch",
  punt 3) wordt óf dichtgeschreven óf herschreven naar wat er dan nog
  open staat.
- Alle dertien gates groen.

May touch: `spec/IR-SPEC.md` (voorstel — beschermd pad),
`spec/RUNTIME-SEMANTICS.md` (voorstel — idem), `spec/verifier/**`
(voorstel — idem), `corpus/negatief/**`, `corpus/golden/**` (nieuwe
getuige), `ref/**` en `backends/clingo/**` alléén bij antwoord (ii),
`backlog/`.

Out of scope: de ankerregel zelf (geland,
`backlog/done/20260905-wf083-vouwbron-anker.md`); de geleste waardevouw
(idem, de fixronde); `backends/drools/**` — die speelt de vorm al en zijn
uitkomst is hier bewijsmateriaal, geen doelwit; `toka/`.

## Result (2026-09-06, `card/wrat-waardevouw`) — antwoord (i): de wrat is een TELBRON

Het maintainerbesluit van 2026-09-06 is uitgevoerd: de onbekend-getypeerde,
enkelvoudige vouwbron (de rij-1-wrat) is een **telbron**, en dat geldt voor
BEIDE spellingen — de kale (`set: false`, geen `navigatieRol`) en de
SET-spelling (`set: true`, `objecttype` = `elementObjecttype`, een
onderwerp-benoemende `navigatieRol`). Onder elke andere vouwsoort is het
document ill-formed. Geland als **eigen nummer WF-093**, niet als clausule
(d) onder WF-083 — de spec zei het al zelf ("is a SHAPE rule, not an anchor
rule, and is tracked separately"), en de codevorm bevestigt het: WF-083's
armen kruisen `navigatieRol`/`navigatiePad`/`objecttype` tegen het anker,
terwijl deze regel op de vouwSOORT hangt en het anker niet aanraakt. De
verifier-functie is gedeeld (`aggregation_source_check`, die de wrat toch al
ontleedt), het regelnummer niet.

**Motivering, met de kosten van de andere kant.** `ref` is de norm en zegt
het al bij executie: zijn EERSTE vouwbron-arm matcht op het sentinel — vóór
de set-arm en ongeacht `set` — dus beide spellingen leveren instances op en
beide worden geweigerd met `value aggregation over a bare role source`; de
ASP-backend weigert de expressiesoort **op deze vier getuigen** — en niet om
het sentinel, maar om de `navigatieRol` die ze dragen (zie de fixronde-
appendix: zijn set-armen lezen `type` als wildcard). Het setbron-antwoord
(ii) had gekost:
een vouwarm in `ref` die moet beslissen wat "per lid" betekent voor een bron
die — in de KALE spelling — geen attribuut noemt, een vertaling in de
ASP-backend, en een golden met
handberekende absolute pins — én het had een vorm gecanoniseerd **die de
frontend wél produceert, maar nooit als een documents enige defect**. Dat is
gemeten, niet aangenomen: de
RegelSpraak-frontend leidt het elementtype van een waardevouw af uit
`a.objecttype()` (een echte attribuutlezing) en neemt de geschreven-rolroute
uitsluitend op de `AANTAL`-tak (`ExpressieBouwer.aggregatieElement` /
`aantalElement`) en de Catala-frontend construeert het sentinel nergens —
maar één route ontsnapt daaraan: een set-aggregatie over een KENMERK van het
elementtype (zie de appendix, getuige B2). Daar co-vuren WF-034 en WF-080
altijd mee. Wat
de vraag levend hield was dus nog steeds geen producent maar een derde
uitvoerder.

**Hermeten census (2026-09-06, stand `64bdb32`, 523 documenten over golden
155 + rood 22 + negatief 316 + pariteit 30).** **54** enkelvoudige
wrat-vouwbronnen onder een `elementObjecttype` — de kaartmeting van
2026-09-05 telde er 51 op 510 documenten; het verschil is corpusgroei, niet
een andere telling. **Alle 54 zijn `AANTAL`:**

| spelling | aantal | soorten |
|---|---|---|
| `set: false`, geen `navigatieRol` | 49 | alle AANTAL |
| `set: false`, mét `navigatieRol` | 4 | alle AANTAL |
| `set: true` (mét `navigatieRol`) | 1 | AANTAL — de negatief `wf083-vouwbron-anker-op-operand`, al afgewezen |

Per zone: golden 15, negatief 32, rood 3, pariteit 4. **Nul niet-AANTAL
wratten**: er is geen blokkade, en de regressiegrens is nul. Nevenmetingen
die de scope vastzetten: **1** wrat-vormig argument in een GELESTE vouw
(twee operanden, pariteitszone) — buiten scope, en de reden dat de regel
enkelvoudig-gescoped moet zijn; **0** niet-aggregatie-`Functie`-knopen met
een `elementObjecttype` (dus de soort-scoping kost vandaag niets).

**Rood-eerst, per negatief.** Verifier van de takkop vóór de regel:
md5 `924f0e3cfb69e9e8d9be7aa916a9bfe7`. Verifier mét WF-093:
md5 `957d3eeb5667188e33d5468b80f21b71` (fixronde 2026-09-06; vóór de
meldingstring-correctie was dat `945642482a4183057a3c5194a11749ce` — de
PASS-vóór stond en staat tegen de pre-regel-binary, die niet veranderde).

| negatief | vorm | vóór | ná |
|---|---|---|---|
| `wf093-wratvouw-kale-som` | kaal × SOM | PASS | WF-093 FAIL |
| `wf093-wratvouw-kale-eerste` | kaal × EERSTE | PASS | WF-093 FAIL |
| `wf093-wratvouw-set-som` | set × SOM | PASS | WF-093 FAIL |
| `wf093-wratvouw-set-maximum` | set × MAXIMUM | PASS | WF-093 FAIL |

Elk is één fout en verder welgevormd (de verifier noemt WF-093 en geen
andere regel), en elk is een kleine mutatie van de GEDRAGEN golden-vorm
`jutterswet-vondsttelling`, die de kale telwrat en de echte set-bron-SOM in
één document draagt. Per-uitvoerder-uitslag staat in de NOTE's; samengevat:
`ref` weigert alle vier met `value aggregation over a bare role source`, de
ASP-backend met `expression kind not translated`, en Drools crasht op de
kale SOM (`ConstraintEvaluationException`), weigert de kale EERSTE per vorm
(`transpiler SKIP`) en **antwoordt** op de set-spelling.

**Drools-herverificatie (zelf gemeten, 2026-09-06, jar uit gate 12).** De
kaartmeting van 2026-09-05 is onafhankelijk nagemeten op deze
documentfamilie, met twee leden van 12 en 7: **SOM 19.00**, **MAXIMUM
12.00**, **MINIMUM 7.00**. De NOTE's dragen dus geen ongecontroleerd citaat.
Zijmeting: dezelfde set-spelling met de rolmeervoud in `attribuut` (in
plaats van het elementattribuut) laat `ref` en de ASP-backend onveranderd en
Drools crashen — het `attribuut`-slot doet in WF-093 niet mee, alleen het
sentinel. `backends/drools` is uitsluitend gemeten, nooit gewijzigd.

**De armvorm, eerlijk: ÉÉN arm, vier getuigen.** Kaal en set lopen door
hetzelfde pad — `ref` doet dat ook (zijn sentinel-arm staat vóór de set-arm
en bindt `set` niet), en in de verifier verschijnt `set` alleen in de
MELDING. Bewezen per weggenomen deelconditie, elke mutant via
`git archive HEAD | tar -x` naar /tmp, build+meting in één `&&`-keten, volle
vier-zone-sweep (527 documenten):

*(md5's en getallen herdraaid in de fixronde van 2026-09-06 op de gefixte
takkop; de meldingstring veranderde de binary, dus alle zes md5's
verschoven. De uitslagen zelf zijn identiek gereproduceerd. Twee kolommen
in plaats van één: de oorspronkelijke tabel telde "documenten extra
geweigerd" waar het getal in werkelijkheid documenten met een gewijzigd
RAPPORT telde — een negatief dat al viel en er een WF-093-regel bij krijgt
verandert geen verdict.)*

| mutant | weggenomen | binary-md5 | nieuwe verdicts | docs met gewijzigd rapport | uitslag |
|---|---|---|---|---|---|
| m1 | de hele arm | `d71c075329201211a3cc4e088267ca0b` | **4** (negatief 4, alle vier SCHOON) | 4 (negatief 4) | precies de 4 getuigen worden schoon; nul andere verdicten, nul andere rapporten |
| m2 | de enkelvoudig-scope | `65e34530bf1d44fea6b4f1f06a1a7f8d` | **0** | 1 (pariteit 1) | de geleste EERSTE in de pariteitszone krijgt er een WF-093-regel bij op een document dat al viel; de melding zou daar onwaar zijn — het scope-argument blijft staan |
| m3 | de soortbeperking (AANTAL mee) | `4d272d89a1be02d485dbe6bee91f3794` | **12** (golden 10 + rood 2) | 35 (golden 10 + negatief 20 + rood 2 + pariteit 3) | de hele gedragen telpopulatie valt om |
| m4 | het sentinel-onderscheid | `0b33907fb163664bde5ddbb329a24bed` | **20** (golden 19 + rood 1) | 55 (golden 19 + negatief 32 + rood 1 + pariteit 3) | elke gedragen set-bron-waardevouw valt om |
| m5 | de `set`-helft van de MELDING | `851f9149c9055de4ab5c0681a8b232a2` | **0** | 2 (negatief 2) | 2 rapportteksten wijzigen, **nul** verdicten: `set` is presentatie, geen tweede arm |

Nul over-afwijzing elders: de sweep vóór/ná de regel over alle 527
documenten verschilt in exact 4 regels.

**`ref`s foutpad blijft staan.** Onder WF-093 is `value aggregation over a
bare role source` onbereikbaar op welgevormde documenten. Dat is de tweede
verdedigingslinie (het huispatroon), geen dode code: gemeten dat **geen**
rood-case, **geen** golden-case en **geen** `ref`-test dat fragment pint, dus
er hangt niets aan. `ref/**` en `backends/clingo/**` zijn niet aangeraakt.

**ch. 4.3-flank herschreven.** Punt (3) is DICHT (met de meting die hem
opende erin: 19/12/7 tegen twee weigeringen), en er staat een nieuwe,
gemeten flank (4) bij die tijdens het dichtschrijven bovenkwam: **het
sentinel op een GEWONE leespositie** — de operand van een geleste vouw, of
een kale referentie — waar geen vouwbronregel bij komt. Gemeten op een
handgebouwde `MAXIMUM` over twee geleste operanden waarvan de tweede het
sentinel draagt: verifier-schoon, terwijl `ref` weigert (`unsupported
AttribuutRef shape (set/sentinel outside a fold)`), de ASP-backend de
expressiesoort weigert en Drools hem SPEELT. Drie zulke posities bestaan
vandaag (1 geleste operand, 2 kale lezingen), alle in de pariteitszone.
Dezelfde één-antwoordt-twee-weigeren-klasse, maar een vraag over het
SENTINEL en niet over vouwbronnen — WF-093 is er bewust niet over
uitgebreid, want daar zou hij met een onware reden weigeren. **Kaartwaardig.**

**RUNTIME-SEMANTICS niet aangeraakt** — gemeten en niet nodig: ch. 3.4a
beschrijft de wrat uitsluitend in de telcontext ("a count folds instances,
not values"), en geen passage in dat document beschrijft een waardevouw over
een rolbron. De weigering is welgevormdheid, geen runtime-semantiek.

**Bijvangst.** (a) De set-spelling kan geen `bronRol` dragen: WF-047's
vouwbron-detector herkent een bron met een `navigatieRol` niet als vouwbron,
dus een binding daar is een tweede, losse fout. Dat is bestaand gedrag, geen
regressie, en het staat nu in de NOTE. (b) Flank (4) hierboven. (c) De
kaartmeting 51 → hermeten 54 komt volledig door corpusgroei (510 → 523
documenten).

**Voorstel voor `spec/CONSTRUCTS.md` rij 1** (NIET aangeraakt; de
hoofdsessie doet CONSTRUCTS serieel na de merge). Aan te hangen:
"**Landed (2026-09-06):** de rij-1-wrat is normatief een TELBRON (WF-093,
IR-SPEC ch. 4.3): een enkelvoudige instantievouwbron met het
onbekend-sentinel op zijn eigen `type` MOET soort `AANTAL` hebben, in beide
wire-spellingen (`set` doet niet mee). Sluit de flank waar vier soorten
verifier-schoon bleven terwijl `ref` en de ASP-backend weigerden en de
Drools-backend antwoordde (19/12/7); grens gemeten op 2026-09-06: van 58
wrat-vouwbronnen over de vier zones zijn de 54 die de regel voorgingen alle
`AANTAL` en beweegt er geen. Het setbron-antwoord is expliciet verworpen —
het had `ref`s vouwarm, clingo's vertaling en een handberekende golden
gekost voor een vorm die de frontend alleen produceert met co-vurende
WF-034/WF-080, nooit als een documents enige defect."

**Alle dertien gates groen** op de eindstand, vanuit de worktree: 1 PASS;
2 PASS (177 bestanden); 3 PASS (177 golden schoon, **320** negatief
afgewezen voor hun eigen regel — 316 + 4); 4 PASS (429 case-bestanden);
5 PASS (407 cases); 6 PASS (407 cases); 7 PASS (371 weerleggingen, 36
overlevers); 8 PASS (22 rode cases); 9 PASS (201 checks, 12 catala-goldens);
10 PASS (173 tests, 30 rgs-goldens); 11 PASS (29 pariteitscases door drie
uitvoerders, nul divergenties); 12 PASS (played **109**, auto-excluded 43,
value-divergence 1, no-case 2, JUnit **207**); 13 PASS (68 annotaties over
14 goldens, 0 overlevers).

Beschermd pad (`spec/IR-SPEC.md`, `spec/verifier/`): de tak blijft staan als
voorstel, NIET gemerged.

## Appendix — fixronde 2026-09-06 (na twee lezingen)

Het GEDRAG van de kaart houdt volledig: census, rood-eerst, arm-exactheid en
de uitvoerdersuitslagen zijn alle onafhankelijk gereproduceerd. Alle
correcties zijn TEKST plus één nieuw kaartvoorstel; geen enkele mutatie in
`ref/**`, `backends/**` of `frontends/**`. De regel zelf — het PATROON in
`aggregation_source_check` — is ongewijzigd.

**De routekeuze, met motivering.** WF-093 blijft gesleuteld op het
SENTINEL. Lens 2 vond een deelklasse waarop de verifier-weigering strenger
is dan de uitvoerders: een set-bron met het sentinel op een ECHTE
elementattribuut-lezing en ZONDER `navigatieRol`. Daar antwoorden clingo én
Drools het juiste getal, terwijl `ref` weigert. Die deelklasse hoort BEWUST
binnen de regel: `ref` is de norm en weigert de hele sentinel-klasse
uniform, dus een verifier-schone versie van die deelvorm zou een LEVENDE
`ref` ≢ clingo/Drools-divergentie zijn onder een welgevormd document —
precies de klasse die deze kaart sluit. De setbron-kant (`ref` leren dragen)
was al door de maintainer afgewezen. De door lens 2 aangedragen "derde
optie" — sleutelen op "het `attribuut`-slot resolveert naar een rol" —
heropent de divergentie zolang `ref` niet meebeweegt en is daarom NIET
doorgevoerd; ze wordt gewogen in het nieuwe kaartvoorstel
`backlog/new/20260906-sentinel-op-leespositie.md`.

**De deelklasse, zelf gemeten (2026-09-06).** Grond in de code: de set-armen
van de ASP-backend (`backends/clingo/lib/translate.ml`, som/totaal op :1444
en minimum/maximum op :1723) matchen `typ` als WILDCARD en eisen
`navigatie_rol = None` — de weigering op de vier getuigen komt dus van de
`navigatieRol`, niet van het sentinel. Drie metingen:

| document | verifier | `ref` | clingo |
|---|---|---|---|
| `wf093-wratvouw-set-som` zónder `navigatieRol` | WF-093 FAIL (alleen) | FAULT | **PASS — 19.00** |
| `wf093-wratvouw-set-maximum` zónder `navigatieRol` | WF-093 FAIL (alleen) | FAULT | **PASS — 12.00** |
| `jutterswet-vondsttelling` met het `type` van zijn echte set-bron gedegradeerd naar het sentinel | WF-093 + WF-047 FAIL | FAULT | **PASS — alle 7 gepinde verwachtingen, de 3.75 inbegrepen** |

Twee tegen één, onder documenten die zonder deze regel schoon zouden zijn.

**Getuige B2 — de frontend produceert de vorm wél.** De claim "geen enkele
frontend produceert hem" is onjuist en is vervangen door het gemeten feit.
De KENMERK-route ontsnapt aan de Onopgelost-poort: `heeftLid`
(`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/model/RegelSpraakModel.java:620`)
is waar voor een attribuut **of een kenmerk**, dus de poort
(`…/resolver/ExpressieBouwer.java:470-481`) vuurt niet, en de
referentiebouwer valt terug op het sentinel (`…/ExpressieBouwer.java:1663`,
`if (dt == null) dt = onbekend();`). Zelf geëmitteerd en geverifieerd met
`de som van het waardevol van alle vondsten van de Jutter` op een eigen
Jutterswet-bron: de frontend levert `attribuut: "waardevol"`, `set: true`,
`navigatieRol: null`, sentinel — **zonder één diagnostic** — en de verifier
meldt er DRIE regels op: WF-034 (het sentinel op het eigen `type` van de
vouw), WF-093 en WF-080 (kenmerk-eindpunt in een aggregatiebron). De vorm
heeft dus een producent, maar er bestaat geen document waarin WF-093 de
enige fout is. Dat is het argument dat de oude formulering zocht.

**Tweede deelvorm op de LEESpositie (ch.-4.3-flank 4), zelf gemeten.** Op
een handgebouwde `MAXIMUM` met twee argumenten en een `elementObjecttype`
(die combinatie stuurt de operanden door de vouwargument-tak, die
`walk_expr` vervangt — daarom bereikt WF-034's sentinel-prologue ze niet):

| deelvorm | verifier | `ref` | clingo | Drools |
|---|---|---|---|---|
| sentinel op een ECHTE attribuutlezing | PASS | FAULT `unsupported AttribuutRef shape (set/sentinel outside a fold)` | FAULT `expression kind not translated (no corpus case exercises it)` | **PLAYED 1** |
| de wrat VERBATIM als gelest operand | PASS | idem | idem | **ERROR** `IllegalStateException: geen getter voor 'vondsten'` |

De derde uitvoerder LEEST de rolnaam dus wel degelijk — hij zoekt er een
getter voor. Dat is de reden dat WF-093 niet naar deze positie is verbreed:
hij zou twee vormen met twee uitvoerdersprofielen onder één reden weigeren.

**Wat er in de tekst is rechtgezet.** In `spec/IR-SPEC.md` ch. 4.3: de
ASP-weigering beperkt tot de gemeten getuigen mét de deelklasse en de
routekeuze-motivering erbij; de valse implicatie "it names a ROLE, and a
role has no value to give" vervangen door de sleutel op het sentinel met de
drie gemeten gedaanten (rol / elementattribuut / kenmerk); de frontend-claim
vervangen door B2; flank (4) herschreven naar de gemeten positie mét de
tweede deelvorm; "three such positions" → "three such READ positions" (de
vierde sentinel zit op een regelDOEL in
`corpus/negatief/wf034-onbekend-regeldoel`, de vijfde op het eigen `type`
van de `Binair` die door een van die lezingen wordt gevoed — gemeten: 63
sentinel-`type`-posities over de vier zones, 59 daarvan vouwargumenten); de
tweede clausule van de ch.-3.4a-verwijzing geschrapt (dat hoofdstuk is
tel-only; `RUNTIME-SEMANTICS.md` is niet aangeraakt); "a source that names
no attribute" beperkt tot de kale spelling; de indexrij gelijkgetrokken. In
`spec/verifier/lib/checks.ml`: melding en armcommentaar sleutelen nu op wat
de arm sleutelt. In de vier NOTE's: het nieuwe meldingscitaat, de nieuwe
md5, het clingo-citaat voluit, en de `bronRol`-bijvangst ook bij de
MAXIMUM-sibling. In twee negatief-`ir.json`'s: de `bron`-string van de
vouwknoop zei "de som van vondsten van alle vondsten…" waar het operand
al "het gewicht van alle vondsten…" zei — gecorrigeerd; **gemeten dat
verdict én rapport van beide documenten byte-identiek blijven**.

**Hermeten defertelling.** De ch.-4.3-notitie "0 times in 510 documents"
(WF-083's twee defer-armen) is opnieuw gemeten met dezelfde
markeringsmutant over de huidige 527 documenten: **nog steeds 0**, beide
armen (probe-binary `3b85ce1d45a8d960f92742cce96cca6c`).

**Mutanten herdraaid.** De meldingstring veranderde de binary, dus de basis
en alle vijf mutant-md5's verschoven; ze staan hierboven in de tabel. Basis
mét WF-093 na de fixronde: `957d3eeb5667188e33d5468b80f21b71` (daarvóór
`945642482a4183057a3c5194a11749ce`, exact gereproduceerd uit
`git archive HEAD` — het protocol is herhaalbaar). De pre-regel-binary
`924f0e3cfb69e9e8d9be7aa916a9bfe7` veranderde niet, dus de
rood-eerst-claim staat ongewijzigd. De sweep basis-vóór/basis-ná over alle
527 documenten verschilt in **nul verdicten** en **vier rapporten** — de
vier getuigen, alleen hun meldingszin. Elke mutant via
`git archive HEAD | tar -x` naar /tmp, build+meting in één `&&`-keten, harde
build-faal (m1 faalde eerst op warning 27 en is pas gemeten toen hij bouwde),
`test -x` vóór de sweep, md5 per sweep.

**Alle dertien gates opnieuw groen** op de fixronde-eindstand, vanuit de
worktree: 1 PASS; 2 PASS (177 bestanden); 3 PASS (177 golden schoon, 320
negatief afgewezen voor hun eigen regel); 4 PASS (429 case-bestanden);
5 PASS (407 cases); 6 PASS (407 cases); 7 PASS (371 weerleggingen, 36
overlevers); 8 PASS (22 rode cases; Drools-been 5 fault-pinned, 17
not-carried); 9 PASS (201 checks, 12 catala-goldens); 10 PASS (173 tests,
30 rgs-goldens); 11 PASS (29 pariteitscases door drie uitvoerders, nul
divergenties); 12 PASS (played 109, auto-excluded 43, value-divergence 1,
no-case 2, JUnit 207); 13 PASS (68 annotaties over 14 goldens, 0
overlevers).
