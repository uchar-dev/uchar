# Drools: fout-companions leren wélke paden draaien

Goal: De §6.5/§6.6-fout-companions (deling-door-leeg/nul, wortel-van-
negatief) zijn losse statische regels op de leegte van de operanden; ze
kunnen niet testen of het pad waarin de expressie staat ook drááit.
Sinds de Let-node-kaart (2026-09-01) is de gekozen kant nooit-verzinnen
(`letKinderenZeker`): een deling in een niet-genomen IfThenElse-tak, of
in een value die geen enkele lezing forceert, fabriceert geen fout meer
— maar de spiegelflank mist zijn fout. Sinds de L-unificatie (besluit
O3, golf B van `20260901-let-lowering-spec-first`) is die flank GROTER,
want onder call-by-need draait de Let-body ALTIJD:
- op `Let d = deler in inkomen gedeeld door tweede` met `tweede` leeg
  zegt ref *inconsistent* en Drools *consistent* — nu voor élke deling
  of wortel in een Let-body, niet meer alleen wanneer de value gevuld
  is (gemeten met de L-ref, 2026-09-01). Dit is precies de vorm die
  `LetDragerWandelingWitnessTest.
  een_deling_achter_een_nooit_gepasseerde_leeg_barriere_levert_geen_fout`
  vandaag pint: die assertie is groen om de restpost, niet meer om
  overeenstemming met ref, en hoort bij déze fix te worden
  herschreven (protected-achtig: bestaande witness, maintainerbesluit);
- idem voor een value wiens binder op sommige maar niet alle paden
  gelezen wordt, of wiens lezing achter een lezing van een ANDERE
  binder ligt (die kan via diens barrière het pad afbreken —
  `leestAltijd` weigert daar te certificeren).
Geen corpus-golden draagt de vorm — gate 12 wordt luid zodra er één
komt. De echte oplossing: de companion draagt de barrière-/takconditie
mee (vuurt alleen wanneer het omhullende pad aantoonbaar evalueert),
zodat beide flanken met ref meelopen.

Verwant, zelfde klasse maar andere oorzaak (alleen documenteren, geen
fix hier): `startpuntCompanions` (§6.8) is productie-semantiek die ref
in het geheel niet kent — de kale flank divergeert vandaag al; dat is
een spec-kwestie (hoort §6.8 in RUNTIME-SEMANTICS of is de port te
ruim?), geen drager-kwestie.

Acceptance:
- Rood-eerst: de gemeten spiegelflank (deling met lege operand in een
  wél-draaiende Let-body) als witness die vóór de fix faalt tegen ref.
- Na de fix: beide flankfamilies (fabricage én misser) elk met kale
  tweeling ref-conform, mutant-rood bewezen; gate 12 exact gelijk
  (played 98 / auto-excluded 39 / value-divergence 1 / no-case 1 —
  gemeten 2026-09-01) of beter; alle twaalf gates groen.
- De §6.8-vraag als notitie met meting in de Result (geen spec-wijziging
  onder deze kaart).

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `backlog/`.

Out of scope: spec-teksten, ref/clingo/verifier, corpuspins, de overige
v1-vormen.

## Bouwnotitie (subagent, 2026-09-02)

**Meettabel (stap 2).** Alle flanken eerst door de L-ref gemeten
(`/tmp/1b-metingen/`, dezelfde wire-vormen als de testhelpers; ref-runner
exit 0 = exacte match). Drools "voor" is de stand op HEAD vóór de fix
(letKinderenZeker, nooit-verzinnen), "na" de stand met de sonde.

| flank | model (uit := …) | invoer | ref | Drools voor | Drools na |
|---|---|---|---|---|---|
| a spiegel | `Let d = deler in inkomen ÷ tweede` | tweede leeg, deler leeg | leeg, INCONS | consistent (misser) | INCONS |
| a spiegel | idem | tweede leeg, deler 2 | leeg, INCONS | consistent (misser) | INCONS |
| a kaal | `inkomen ÷ tweede` | tweede leeg | leeg, INCONS | INCONS | INCONS |
| b barrière-vóór | `Let d = deler in d plus (inkomen ÷ tweede)` | beide leeg | leeg, CONS | CONS | CONS |
| b barrière-vóór | idem | deler 2 | 2.0000, INCONS | consistent (misser) | INCONS |
| b barrière-ná | `Let d = deler in (inkomen ÷ tweede) plus d` | beide leeg | leeg, INCONS | consistent (misser) | INCONS |
| b barrière-ná | idem | deler 2 | 2.0000, INCONS | consistent (misser) | INCONS |
| c genomen tak | `als a > 100 dan inkomen ÷ deler anders 7` (kaal én in Let-body) | a 200, deler leeg | leeg, INCONS | consistent (misser) | INCONS |
| c niet-genomen | idem | a 1 | 7.0000, CONS | CONS | CONS |
| d value gelezen | `Let q = inkomen ÷ tweede in q plus 1` | tweede leeg | leeg, INCONS | INCONS | INCONS |
| d value ongelezen | `Let q = inkomen ÷ tweede in 42` | tweede leeg | 42.0000, CONS | CONS | CONS |
| d value één tak | `Let k = a in Let q = inkomen ÷ tweede in als k > 100 dan q anders 7` | a 200 / a 1 | leeg INCONS / 7.0000 CONS | consistent (misser) / CONS | INCONS / CONS |
| e vreemde lezing vóór | `Let x = leegding in Let q = inkomen ÷ tweede in x plus q` | beide leeg | leeg, CONS | CONS | CONS |
| e vreemde lezing ná | … `in q plus x` | beide leeg | leeg, INCONS | consistent (misser) | INCONS |
| f wortel spiegel | `Let d = deler in wortel(saldo)` (kaal: `wortel(saldo)`) | saldo −4 | leeg, INCONS | consistent (misser) / INCONS | INCONS |
| f wortel keuze | `Let a = a in als a > 100 dan wortel(saldo) anders 7` | a 200 / a 1 | leeg INCONS / 7.0000 CONS | consistent (misser) / CONS | INCONS / CONS |
| g defaults | ch.-7-groep, consequentie `Let d = deler in inkomen ÷ tweede` | tweede leeg / tweede 2 | toeslag 0.0000 CONS (ch. 7.1: speculatieve deponering vervalt) / 5.0000 CONS | weigering (delingshek) | weigering (delingshek) |
| h twee delingen | `(inkomen ÷ deler) plus (Let d = leegding in d plus (inkomen ÷ tweede))` | alles leeg / deler 2, leegding 1 / deler 2 | 0.0000 INCONS / 6.0000 INCONS / 5.0000 CONS | INCONS / consistent (misser) / CONS | INCONS / INCONS / CONS |
| p parameter in drager | `Let d = deler in (inkomen ÷ tweede) plus GRONDSLAG` (param 3) | tweede leeg / tweede 2 | 3.0000 INCONS / 8.0000 CONS | consistent (misser) / CONS | INCONS / CONS |

ref's evaluatievolgorde van `Binair` is links vóór rechts
(`ref/lib/eval.ml`, gemeten met b en e): een lezing links van de deling
kapt de Let af vóór de deling, een lezing rechts ervan niet.

**Gekozen constructie: kandidaat (ii), de sonde.** Per gastregel
verzamelen `verzamelDelingen`/`verzamelWortels` nu élke plek die KAN
draaien (volle `letKinderen`) mét de buitenste drager erboven
(`Plek(site, drager)`). Een plek zónder drager houdt het statische
leeg/negatief-predicaat (v0 ongewijzigd, DRL-pariteit). Een plek ónder
een drager krijgt een companion die in zijn consequentie de
`WaardeFn` van die buitenste drager op het subject her-evalueert met een
`Sonde` in de omgevingsmap (sleutel `"sonde fout"`, met spatie); de
delingsarm van `scalarWaarde` en de wortelarm van `scalarWrapper` melden
een §6.5/§6.6-fout aan de sonde (no-op zonder sonde — de gastregel zelf
draagt er geen), en de companion insert de Inconsistentie alleen als er
gemeld is — bij deze bouwnotitie nog per plek, sinds fixronde 2 (L2-F5,
gemeten) per DRAGER. Barrières en call-by-need komen zo uit de
value-lowering zelf; de evaluatievolgorde BINNEN een vorm is dat alleen
waar de lowering ref's volgorde spiegelt — per vorm gemeten, niet
automatisch (fixronde 1: Begrensd evalueerde minimum vóór maximum, ref
andersom); de value-side is niet verbouwd (twee meldregels erbij, en in
fixronde 1 de Begrensd-volgorde). De
consequentie bindt de globals van de drager zoals de gastregel de zijne
(`consequenceOf`/`vulGlobalMap`); een drager die niet lowert of een
global die de gast niet kon binden is een invariantschending en faalt
hard. Het patroon krijgt de freshness-expr (klasse-reactief), de
§9.5-idempotentieguard blijft. De buitenste drager is de juiste eenheid:
alle niet-dragervormen die de walk afdaalt (Binair, Afgerond, Begrensd,
Functie-argumenten) evalueren hun kinderen onvoorwaardelijk, hier en in
ref, en een barrière ontsnapt nooit aan haar eigen Let; `lowerVal` (de
aggregatie-in-arithmetiek-route) lowert geen drager (gast skipt), dus
elke dragerplek zit onder `scalarWaarde`.

**Verworpen: kandidaat (i), statische padpredicaten.** Een predicaat per
plek zou de IfThenElse-condities (letConditie-lowering) én de
Let-leesstructuur moeten coderen, inclusief de vraag welke binder vóór
de plek in evaluatievolgorde gelezen wordt en of diens value leeg is —
geneste Lets, gedeelde binders, lezingen in condities. Dat is een tweede
implementatie van ch. 10.6 naast `letWaarde`, die bij elke verbreding van
letConditie/letWaarde mee zou moeten bewegen en die precies de
volgordevraag zou moeten modelleren die (ii) gratis meet. Niet smaller,
wel fout-gevoeliger.

**Indexering.** Ongewijzigd voor v0-vormen. Voor regels mét drager telt
de kwalificerende lijst nu ook plekken in takken/values die
letKinderenZeker uitsloot; het suffix blijft per kwalificerende plek
(flank h: `(deling door leeg 1)` statisch, `(deling door leeg 2)` sonde,
in één KieBase gebouwd). **Gecorrigeerd in fixronde 2 (L2-F4):** de
oorspronkelijke motivering hier ("DRL lowert geen Let, dus daar is geen
pariteit te bewaren") is onjuist en met beide transpilers over hetzelfde
model weerlegd. Gemeten op de (h)-vorm:

| stand | DRL | exec-model |
|---|---|---|
| main | `(deling door leeg)` | `(deling door leeg)` |
| deze kaart | `(deling door leeg)` | `(deling door leeg 1)` + `(deling door leeg 2)` |

`DrlTranspiler.deelDoorLeegFout` keert alleen vroeg terug op een
`voorwaardeMarker`; de WAARDE-marker (de `/* TODO expressie */` die de
drager in de gastregel achterlaat) stopt de companion-emissie niet, dus
DRL emitteert wél de companion van de KALE plek in een Let-dragende
regel. De suffix-pariteit breekt daarmee voor een regel die kale én
dragergehoste kwalificeerders mengt. Aanvaard, niet weggewerkt (optie
(a)): de companionNAAM is geen observabele — de Inconsistentie draagt in
beide backends de GASTregelnaam plus subject — en een eigen suffixruimte
voor dragergehoste plekken zou de naam van een kale plek laten afhangen
van welke ándere plekken lowerden. Staat als open punt hieronder en als
gemeten comment bij `delingCompanions`.

**Vervallen.** `letKinderenZeker`, `Lees`, `leestAltijd`, `samen`,
`samenTakken` (137 regels); de `LetVorm`-javadoc herschreven naar
(a) refusal/registratie-walks via letKinderen, (b) de twee
answer-walks via letKinderen mét sonde, (c) bewust niet afdalen.

**Mutanten** (in /tmp-kopieën via git archive; de tellingen zijn
gedateerd, want elke fixronde voegt witnesses toe). (1) padconditie uit
(`if (true || plek.drager() == null)`): 9 rood bij deze bouwnotitie
(12 witnesses), 13 op de stand ná fixronde 1 (lens 2), **18 rood ná
fixronde 2** (15 failures + 3 errors) — precies de fabricage-familie
(niet-genomen tak, ongelezen value, één-tak niet-genomen, barrière-vóór,
vreemde lezing eerst, wortel niet-genomen, h3, de twee grensvolgorde-
spiegels, de doorgegeven barrière, de twee nooit-verzinnen-witnesses in
LetDragerWandeling); de misser-flanken groen. (2) afdaling weg
(`letKinderen`-descent uit beide walks): 14 rood hier, 21 ná fixronde 1
(lens 2), **24 rood ná fixronde 2** — precies de misser-familie; de
fabricage-flanken groen.

**Gates.** Gate 1 PASS. Gate 12 PASS: played 104 / auto-excluded 39 /
value-divergence 1 / no-case 1, JUnit 125 → 138, nul skipped
(12 nieuwe witnesses + 1 zuster-witness). Geen spelende golden draagt
een kwalificerende deling/wortel onder een drager, dus de telling kon
niet bewegen. Gecontroleerd (fixronde 2, opnieuw over de hele
goldenverzameling): precies drie goldens dragen zowel een `Let` als een
deling/wortel — kinderweging, tariefstelling én kamerhuishouden-catala —
en alle drie delen door een **literal** (2,0000 / 2,0000 / 4,0000; de
derde met een `FieldAccess`-dividend), die `kwalificeertOperand` afwijst.
Geen enkele golden draagt een `WORTEL`.

**Open punten.**
- `companionOperand`-scope: de BinderRef-verbreding onder een drager is
  in fixronde 1 gedaan (`kwalificeertOperand`, zie hieronder); wat
  daarbuiten blijft staat daar exact opgesomd.
- `startpuntCompanions` (§6.8) blijft drager-blind; niet aangeraakt,
  alleen gedocumenteerd (de spec-vraag staat in de kaarttekst).
- Companions (statisch én sonde) dragen géén stratificatie-salience
  terwijl de gastregel die kan hebben; pre-existent. De sonde verbreedt
  de leesverzameling tot de hele drager: een later vurende producent kan
  alleen een fout tóevoegen (insert-only, klasse-reactief), en een fout
  die vroeg op een nog-niet-afgeleide lezing wordt geïnsert wordt nooit
  ingetrokken — dezelfde klasse als bij de statische companions.
- (g) defaults: ref laat de speculatieve §9.5-deponering van een
  doorgevallen default vervallen (0.0000, consistent); Drools blijft
  het delingshek weigeren (waarde niet geproduceerd) — een
  defaults-kaart, niet deze.
- De klasse-javadoc van `LetDragerWandelingWitnessTest` beschrijft het
  vóór-fix-mechanisme; de witnesses erin gelden nog. Tekst buiten de
  toegestane methode, dus niet aangeraakt (maintainerwerk). Verouderd
  zijn precies: (1) de eerste zin, "the two paths a walk must NOT see
  through"; (2) bullet 4, "the two paths those walks must NOT see
  through … descending there fabricates a companion for code that never
  executes" — de walks dalen nu volledig af en de sonde beslist bij het
  vuren; (3) in `een_deling_in_de_niet_genomen_tak_levert_geen_fout` de
  zinnen "A §6.5 companion built off it would fire anyway — the companion
  rule is a SEPARATE rule keyed on 'dividend present, divisor leeg' and
  cannot see which branch ran" en "So the walk descends only into the
  CERTAINLY evaluated children (the condition operands)". De
  sectiekop "4. the other half: a companion the walk must NOT fabricate"
  klopt nog als eis, niet meer als mechanisme.
- De `Excluded`-vraag: een dragerplek waarvan de drager niet her-lowert
  faalt hard (`IllegalStateException`) in plaats van luid te skippen;
  bewust (invariant: de gast lowerde die drager via dezelfde
  scalarWaarde), maar het is een throw in de transpile-fase, geen
  Dekking-skip.
- *(fixronde 2, L2-F4)* De companion-NAAMpariteit met DRL breekt voor
  een regel die kale én dragergehoste kwalificeerders mengt — gemeten,
  zie de tabel onder *Indexering*. Aanvaard omdat de naam geen
  observabele is; herstellen zou een eigen suffixruimte per drager
  vergen.
- *(fixronde 2, L2-F5)* De companionnaam dekt niet meer één syntactische
  plek: de sonde antwoordt per DRAGER. Niet observabel via de case-pins
  (de Inconsistentie draagt de gastregelnaam), dus gedocumenteerd in de
  `Sonde`-javadoc in plaats van gewitnesst.
- *(fixronde 2, L2-F2)* De waarde-kant-witness van de doorgegeven
  barrière staat nu in `CompanionPadWitnessTest` (e'); ze hoort náást de
  ch.-10.6-barrière-witnesses in `LetClusterWitnessTest` —
  maintainerwerk, bestaand testbestand.
- *(fixronde 2, L2-F8)* S-semantiek-rest, zelfde klasse als de drie
  LetDrager-zinnen hierboven: `LetClusterWitnessTest:93` zegt "the leeg
  value propagates, THE BODY NEVER RUNS". Onder de L-unificatie (besluit
  O3, call-by-need) draait de body wél — de lezing van `ink` forceert de
  lege need en de barrière valt op de LEESPLEK. Uitkomst en assertie
  kloppen, het mechanisme in de zin niet. Bestaand testbestand,
  maintainerwerk.

## Fixronde 1 (subagent, 2026-09-02)

Lensbevindingen L1-F1..F6 plus het open punt "BinderRef als operand".
Elke pin eerst door de L-ref (51 model+case-paren uit het testbestand
zelf gedumpt en gemeten, `/tmp/fr1-metingen/`, ref-runner exit 0 = exacte
match; de harde-fout-flank exit 1 met "EVALUATION FAULT: division by
numeric zero"). Rood-eerst tegen de ongefixte transpiler: 5 van 23
witnesses rood (Begrensd-sonde, Begrensd-waarde, binder-dividend,
binder-radicand, defaults-wortelhek); F2..F5 via mutanten.

- **L1-F1 gefixt (ref-divergentie).** De Begrensd-arm van `scalarWaarde`
  evalueert nu basis, dan MAXIMUM, dan minimum — ref's gemeten volgorde
  (`ref/lib/eval.ml`, Begrensd-arm: `Arith.clamp ~min_:… ~max_:… v` na
  een let-gebonden basis; OCaml past gelabelde argumenten rechts-naar-
  links toe). Witness (i) in CompanionPadWitnessTest: sonde-kant
  tweeling (`Let d = deler in begrensd(inkomen, min d, max inkomen ÷
  tweede)`: ref leeg + INCONSISTENT, vóór de fix "verwacht inconsistent,
  werkelijk consistent"; spiegel: ref leeg + CONSISTENT, vóór de fix een
  fabricage) en waarde-kant tweeling (`max inkomen ÷ nul`, nul 0,00: ref
  EVALUATION FAULT, Drools nu ArithmeticException "Breuk deling door 0"
  uit de consequentie, vóór de fix stil leeg; spiegel: ref leeg +
  CONSISTENT, Drools nu idem, vóór de fix de exception). Javadoc van
  `Sonde` en de LetVorm-inventaris gecorrigeerd: de volgorde is per vorm
  gemeten en gespiegeld, niet geërfd. Extra witness, ook ref-gemeten: de
  Vergelijking van een ch.-10.2-guard links vóór rechts (lezing links
  kapt af: leeg + CONSISTENT; deling links: leeg + INCONSISTENT).
  *Vervolg buiten deze kaart (ref + RUNTIME-SEMANTICS):* ref's
  grensvolgorde is een artefact van OCaml's ongespecificeerde
  argumentvolgorde — expliciet let-binden en normatief vastleggen (ch.
  1.2 begrensd of ch. 10.6), waarna Drools de vastgelegde volgorde
  spiegelt.
- **L1-F2 gefixt (witness).** De freshness-expr van de sonde-companion
  is nu gewitnesst (r): `deler` afgeleid door een regel die §9.2 ná de
  companion dwingt (negatief kenmerk met producent, salience -1 — de
  reactiviteitModel-vorm van LetClusterWitnessTest). ref: barrière-vorm
  `Let d = deler in d plus (inkomen ÷ tweede)` 2.0000 + INCONSISTENT;
  tak-vorm `als d > 1 dan inkomen ÷ tweede anders 7` leeg + INCONSISTENT.
  Mutant A (expr weg): 22/23 groen, (r) rood "verwacht inconsistent,
  werkelijk consistent". "Klasse-reactief" is daarmee bewezen, niet
  alleen beweerd.
- **L1-F3 gefixt (witness).** ABS-tweeling (a'): `Let d = deler in
  inkomen ÷(abs) tweede`, ref leeg + INCONSISTENT, naast de kale. Mutant
  B (melding in de ABS-arm weg): alleen (a') rood.
- **L1-F4 gefixt (witness).** Nul-radicand onder een Let (f'), saldo
  0,00: ref 0.0000 + CONSISTENT, kaal én in de body. Mutant C
  (`signum() <= 0`): alleen (f') rood (fabricage).
- **L1-F5 gefixt (witness).** Leeg dividend onder een Let (a'):
  `Let d = deler in leegding ÷ tweede`, beide leeg / tweede 2,00: ref
  0.0000 + CONSISTENT, plus de kale. Mutant D (`lv != null` weg): alleen
  (a') rood (fabricage).
- **L1-F6 gefixt (hek).** Het delingshek van de defaults-consequentie
  spiegelt één-op-één naar de wortel: `bevatDeling` is `foutplekSoort`
  geworden (levert "deling"/"wortel"/null) en de weigering noemt de
  soort. Meting: default 1 (kindertal ≥ 2) met consequentie
  `wortel(saldo) plus 0,0000`, saldo -4,00: ref toeslag 0.0000 +
  INCONSISTENT; Drools vóór de fix 0.0000 + consistent (stil), nu de
  luide weigering "kanaal-deponerende consequentie (wortel)". Witness
  (g'). De wáárde blijft voor de defaults-kaart (open punt (g)); dit hek
  maakt het stille consistent luid, meer niet. Geen golden draagt een
  wortel in een defaults-consequentie (gecontroleerd), gate 12 ongewijzigd.
- **BinderRef als operand onder een drager gedaan.** `kwalificeertOperand`:
  buiten een drager blijft `companionOperand` (statisch predicaat,
  DRL-pariteit); ónder een drager kwalificeert ook een binder-lezing
  (`LetVorm.Lezing`), want de sonde observeert de plek. Witnesses (j),
  ref-gemeten: dividend `Let n = inkomen in n ÷ tweede` (tweede leeg:
  leeg + INCONSISTENT, vóór de fix misser; inkomen leeg: barrière, leeg +
  CONSISTENT; beide gevuld 5.0000), deler `Let d = deler in inkomen ÷ d`
  (deler leeg: barrière vóór de deling, leeg + CONSISTENT — een binder
  antwoordt nooit leeg, dus §6.5 kan hier niet deponeren; de verbrede
  companion mag niet fabriceren), radicand `Let s = saldo in wortel(s)`
  (-4: leeg + INCONSISTENT, vóór de fix misser; leeg: CONSISTENT), elk
  naast zijn kale tweeling. **Buiten bereik, exact:** een Literal,
  ParameterRef, to-many-navigatie, temporeel of gedimensioneerd attribuut
  en elke samengestelde operand (geneste Binair, Functie) — kaal én onder
  een drager, zoals vóór deze kaart; een Let-node als operand van een
  plek die zelf bóven elke drager staat (`(Let q = a in q) ÷ tweede`,
  lens m5: ref inconsistent, Drools consistent — de plek heeft geen
  sonde-eenheid); een deling in de regel-VOORWAARDE (lens m11a); een
  deling als Functie-argument, `wortel(… ÷ …)` (lens m22, verzamelDelingen
  daalt niet in Functie-argumenten af, DRL-pariteit).
- **Weerlegd/bewust gelaten:** niets weerlegd; alle zes bevindingen
  gefixt. Bewust gelaten (gedocumenteerd, geen semantisch gevolg binnen
  de kaart): de bovenstaande buiten-bereik-vormen en de verouderde
  LetDrager-javadoc (maintainerwerk, zie open punten).

**Gates fixronde 1.** Gate 1 PASS. Gate 12 PASS: played 104 /
auto-excluded 39 / value-divergence 1 / no-case 1, JUnit 138 → 149, nul
skipped (11 nieuwe witnesses in CompanionPadWitnessTest: 23 totaal).

## Fixronde 2 (subagent, 2026-09-02)

Lensbevindingen L2-F1..F8. Acht nieuwe pins, elk eerst door de L-ref
gemeten (`/tmp/fr2/`, model+case uit de testhelpers zelf gedumpt,
runner exit 0 = exacte match). Rood-eerst tegen de ongefixte transpiler:
2 van de 4 nieuwe witnessmethodes rood (de twee (a'')-flanken); (e') en
(i') zijn mutant-dragend, niet HEAD-rood — zo waren ze bedoeld.

- **L2-F5 gefixt (ref-divergentie, het enige echte keuzepunt).** Eerst
  gemeten wáárom `gemeld.contains(plek)` op de (a'')-vorm mist, met een
  diagnostische print in een /tmp-kopie: de sonde meldt op
  `Let d = deler in ((inkomen ÷ tweede) plus (5,00 ÷ leegding))` precies
  ÉÉN plek — de literal-deling — terwijl de companion voor de ándere
  plek staat; `identiteit=false` én `structureel=false`. Het is dus geen
  object-vs-structuur-kwestie: de her-gelowerde drager levert dezelfde
  Expr-objecten, de gemelde plek is gewoon een andere plek. Beide
  kandidaten daarna gemeten op de hele suite:
  - kandidaat B (structurele plek-identiteit, `LinkedHashSet` i.p.v. de
    identity-set): 153 tests, **2 rood** — de (a'')-flanken blijven
    rood, en hij zou bovendien twee structureel gelijke plekken in één
    waarde samenvouwen;
  - kandidaat C (de ruimere poort: "een §6.5/§6.6-plek ónder deze
    drager draaide en faalde"): 153 tests, **0 rood**.
  Gekozen: C. `Sonde` draagt nu één boolean; `heeftGemeld(Expr)` is
  `heeftFoutGemeld()`. Witnesses (a''), ref-gemeten: de literal-deling
  onder dezelfde drager faalt → uit 5.0000 + INCONSISTENT (vóór de fix
  "verwacht inconsistent, werkelijk consistent"); `leegding` gevuld →
  6.0000 + CONSISTENT; plus de fabricage-tweeling áchter een lezing van
  `d`: `deler` leeg → leeg + CONSISTENT (niets meldt, niets vuurt),
  `deler` 2,00 → 7.0000 + INCONSISTENT. Wat de poort wél garandeert
  (geen fabricage: elke melding is een echte fout op een echt gedraaid
  pad) en wat niet (géén per-plek-attributie: twee kwalificerende
  plekken onder één drager geven twee companions die op elkaars fout
  vuren) staat in de `Sonde`-javadoc, mét de eerlijke bijsluiter dat de
  case-pins dat verschil niet kúnnen zien — de Inconsistentie draagt de
  gastregelnaam plus subject, en de §9.5-idempotentiepoort vouwt ze tot
  één feit. De resterende dekkingsgrens is ongewijzigd: een drager
  zónder kwalificerende plek krijgt geen companion, dus een eenzame
  niet-kwalificerende plek mist haar fout nog steeds.
- **L2-F2 gefixt (witness).** (e'): "een binnenste Let laat de vreemde
  barrière door naar de buitenste".
  `Let x = leegding in ((Let q = 1,0000 in x plus q) plus (inkomen ÷
  tweede))`, ref uit leeg + CONSISTENT — de lezing van `x` staat ín de
  binnenste Let maar bindt aan de buitenste, dus de binnenste handler
  moet de vreemde barrière RETHROWEN. Mutant M4 (`if (raised !=
  barriere) throw raised;` weg) overleefde de suite en is nu rood:
  "verwacht leeg, werkelijk 0; verwacht consistent, werkelijk
  inconsistent". De waarde-kant-tweeling (dezelfde vorm zonder
  §6.5-plek, rechts een literal 5,0000; ref uit leeg, geslikte barrière
  zou 6.0000 geven) staat als tweede assertie in dezelfde methode; ze
  hoort inhoudelijk náást de ch.-10.6-barrière-witnesses in
  `LetClusterWitnessTest`, maar dat is een bestaand testbestand buiten
  het mandaat van deze kaart — **maintainerwerk**, zie open punten.
- **L2-F3 gefixt (witness).** (i'): de Begrensd-tweeling voor het derde
  punt van de volgorde, basis vóór maximum.
  `Let d = deler in begrensd(basis = inkomen ÷ tweede, minimum =
  0,0000, maximum = d)` → ref leeg + INCONSISTENT; spiegel (lezing in de
  basis, deling in het maximum) → ref leeg + CONSISTENT. Mutant M11
  (volgorde maximum, basis, minimum) overleefde de suite en is nu rood
  ("verwacht inconsistent, werkelijk consistent"). Samen met (i) pint de
  suite nu de volledige driepuntsvolgorde in plaats van één paar.
- **L2-F4 gefixt (onwaar commentaar), optie (a).** Zie de gemeten tabel
  onder *Indexering* hierboven; comment bij `delingCompanions`
  vervangen door de meting, en de twee javadocs die zeiden dat DRL "zo'n
  regel whole markert" gecorrigeerd (DRL zet een TODO-expressie-marker
  vóór de drager IN de gastregelwaarde en emitteert de companions van de
  plekken daarbuiten gewoon).
- **L2-F7 gefixt (onwaar commentaar).** De sonde-sleutel botst niet met
  een Parameter-global dankzij de spatie, en niet met een binderslot
  dankzij het `"binder "`-prefix dat elk binderslot draagt (de spatie
  alleen sluit die niet uit — `binderSleutel` levert zelf een spatie).
- **L2-F1 gefixt (onwaar commentaar).** Het (c)-bullet in `LetVorm`
  beweerde nog dat een BinderRef-operand nooit kwalificeert en "kept
  deliberately" is, terwijl fixronde 1 precies die verbreding deed. Het
  bullet verwijst nu naar `kwalificeertOperand` — buiten een drager het
  statische predicaat (DRL-pariteit), ónder een drager kwalificeert ook
  een binder-lezing (witness (j)) — met de restlijst uit die methode
  zelf, zodat er één lijst is. Ook de (b)-alinea aangepast: de sonde
  antwoordt per DRAGER, niet per plek.
- **L2-F6 gefixt (kaart).** Mutantgetallen gedateerd en geactualiseerd
  (zie *Mutanten*); kamerhuishouden-catala aan de gecontroleerde
  goldens toegevoegd (zie *Gates*); de §6.8-meting hieronder klaargezet
  voor de Result; de gemeten gates opgesomd.
- **L2-F8 bewust gelaten.** `LetClusterWitnessTest:93` zegt nog "the
  body never runs" waar de body onder call-by-need juist wél draait (de
  barrière valt op de LEZING van `ink`). De uitkomst en de assertie
  kloppen; alleen het mechanisme in de zin niet. Bestaand testbestand
  buiten het mandaat — opgenomen in de open-puntenlijst naast de drie
  LetDrager-zinnen, zodat de S-resten op één lijst staan.

**§6.8-notitie mét meting (voor de Result).** `startpuntCompanions` is
productie-RegelSpraak-semantiek die ref in het geheel niet kent. Gemeten
op een twee-flanken-paar — een startpunt-lid-regel wiens anker een leeg
eigen attribuut is, één keer kaal en één keer omhuld als
`Let d = basis in d`: **ref antwoordt CONSISTENT op BEIDE flanken.** De
KALE flank divergeert dus vandaag al (Drools inconsistent, ref
consistent), en juist de drager-BLINDHEID van de leftmost-peel is wat de
OMHULDE flank mét ref laat kloppen. Afdalen zou hier een ref-divergentie
TOEVOEGEN in plaats van er een weg te nemen — daarom niet aangeraakt.
De vraag zelf (hoort §6.8 in RUNTIME-SEMANTICS, of is de port te ruim?)
is een spec-kwestie buiten deze kaart.

**Gates fixronde 2** (alle in de worktree gemeten, niet aangenomen).
Gate 1 PASS. Gate 12 PASS: played 104 / auto-excluded 39 /
value-divergence 1 / no-case 1 — ongewijzigd — JUnit 149 → **153**, nul
skipped (4 nieuwe witnessmethodes). Verder gemeten: gate 2 PASS (167
files), gate 3 PASS (167 golden clean, 178 negatief), gate 4 PASS (405
cases), gate 5 PASS (383 cases replayed), gate 6 PASS, gate 7 PASS,
gate 8 PASS. Gates 9, 10 en 11 raken `backends/drools/**` niet en zijn
niet gedraaid — dat is een gemeten-uitspraak-grens, geen groenverklaring.
