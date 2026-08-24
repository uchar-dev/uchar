# Fase 5c, kaart 4 — subscope-calls en de tutorial-exitgetuige

Goal: PHASE5C-TUTORIAL §3 als lowering op bestaande kernel (geen
nieuwe knoop): EScopeCall → ResolvedObjectCreatie van het
callee-objecttype met args als Toekenningen op input-attributen +
verbindendeRol + genavigeerde AttribuutRef voor outputs; de
callee-regels draaien in de caller-fixpoint; per-element-calls,
multi-state en context-inputs blijven luide recorded flanken
(besluit-6's gegenereerde rung geratificeerd-maar-uitgesteld, met de
WF-050-randvoorwaarde). Daarbovenop de exitgetuige (vraag 3: A):
tutorial_end_2_3 als frontend-side E2E-fixture (plugin → verifier →
ref → clingo; het corpus blijft rechtenvrij per besluit 2), naast een
eigen-geschreven Jutterswet-subscope-golden dóór het corpus met de
volle gate-set. Bij groen valt het formele 5a-exitcriterium: PLAN §6
Phase 5 krijgt zijn Met-markering voor dat criterium (PLAN-update in
deze kaart).

Acceptance: alle negen gates groen; de tutorialfixture drievoudig
groen; rood-eerst; herpin; twee opus-lenzen met bevindingen gefixt
vóór de merge; spec/PLAN.md-markering.

May touch: als kaart 1 plus spec/PLAN.md. Out of scope: 2-4
(states/per-element-calls); besluit-6-implementatie.

---

## Opdracht "tutorial-exitgetuige" — ROOD-EERST en BLOKKADE (2026-08-24)

Status: **OPGEHEVEN — besloten en opgeheven (maintainersbesluit 2026-08-24,
genummerde vragen, beide A).** De analyse hieronder is als rood-eerst
vastgelegd (drie onafhankelijke blokkades, elk met letterlijk bewijs; het
ontwerp was op dat moment NIET gewijzigd, de werkboom schoon). Het besluit:
(1) **WF-027 is verruimd** — een kenmerk-initialisatie in een `ObjectCreatie`
mag een boolean-getypte EXPRESSIE dragen (de constante blijft de v0-vorm),
wat **B1** (boolean subscope-invoer) op élke laag opheft (verifier, ref,
clingo, plugin); (2) **de clingo-flanken B2** (datum-literaal in termpositie)
**en B3** (ListLength-in-term-binding) **landen in deze kaart** en heffen het
clingo-been op. `tutorial_end_2_3` loopt nu drievoudig groen (plugin →
verifier → ref → clingo, gate 9) — zie de landingsnotities verderop in dit
bestand (WF-027-verruiming; ref-evaluatie + rekenkamer-overzee-anker; clingo
kenmerk-init-expressie; clingo B2; clingo B3; plugin-verruiming). De
oorspronkelijke blokkade-analyse blijft hieronder staan als
rood-eerst-record.

### Herkomst van de fixture

`examples/tutorial_end_2_3.catala_en` opgehaald van catala-book `main`
(`raw.githubusercontent.com/CatalaLang/catala-book/main/...`). **Upstream-
drift:** `main` regel 143 gebruikt `Integer.sum of` (stdlib-module
`Stdlib_en`), niet de verouderde `sum <type> of`-aggregatievorm die het
ontwerp §1/§5 en deze opdracht verwachten. Enige verschil met de ontwerp-
tijdversie (ouder-commit `62c35379`, "Autoformat", vóór commit
`d2de59a9` "Update for the newer 'Integer.sum' syntax") is precies die
regel: `sum integer of` → `Integer.sum of`. De plugin draait met
`~no_stdlib:true`; `Integer.sum` faalt dus met "standard library module
Stdlib_en could not be found". De ontwerp-getrouwe vorm is `sum integer of`
(desugart boven de scopelang-cut naar de 5b-gedekte Reduce+Optional-vorm;
catala geeft er alleen een deprecation-WARNING op — die tolereert de plugin
al). Dit is geen blokkade op zich: onder `sum integer of` gaat Article 7
schóón door de plugin.

### ROOD-EERST (letterlijke plugin-uitvoer)

1. Ongewijzigde `main`-fixture (met `Integer.sum of`):
   `catala_frontend: Catala_utils.Message.CompilerErrors(_)` (exit 2) —
   catala zelf faalt: `The standard library module Stdlib_en could not be
   found`.

2. Ontwerp-getrouwe variant (`sum integer of`, hele bestand incl.
   Test-scopes): eerst catala's deprecation-WARNING op `sum integer of`
   (getolereerd), dan:
   `catala_frontend: refused: variable "tax_rate" read as an
   arithmetic/comparison operand is not always filled (not an input, no
   unconditional definition) ...` (exit 3).

### Wat WÉL binnen het ontwerp doorloopt (geverifieerd)

- **Article 7 `HouseholdTaxComputation`** (list-of-struct + `sum integer
  of` + decimals + geld×decimaal): plugin exit 0, verifier PASS, ref
  `oracle: PASS` (household_tax = $30.000 / $50.000, tegen
  `catala interpret` geijkt). Kaart-1+3-integratie draait.
- **Article 1-6 `IncomeTaxComputation`**: plugin+verifier+ref groen zodra
  de operand-vulanalyse de **uitputtende datum-complementaire basisgroep**
  (article_2 oud `< 2000` / nieuw `>= 2000`, fallback `false ⊢ ∅`) als
  "gevuld" erkent. Dat is een veilige, in-ontwerp verfijning van de
  conservatieve `empty-operand`-hek (de ladder is totaal → nooit leeg →
  geen coalesce-vs-hard-divergentie; het uitgestelde deviatiebesluit wordt
  niet geraakt). Bewijs met tijdelijke hek-relaxatie: ref `oracle: PASS`
  (income_tax = $4.000 / $7.500 / $30.000, tegen `catala interpret`
  geijkt). Dit is de "gedekte vorm die nog niet doorloopt" — een plugin-fix
  binnen bereik, maar hij heft de blokkades hieronder niet op.

### BLOKKADES (vormen buiten het geratificeerde ontwerp)

**B1 — boolean subscope-invoer (hard; blokkeert de hele tutorial, elke
executor).** Article 8 `HouseholdTaxIndividualComputation` roept
`income_tax_computation scope IncomeTaxComputation` aan en geeft
`overseas_territories` (een boolean, dus een kenmerk) als subscope-invoer:
`definition income_tax_computation.overseas_territories equals
overseas_territories`. Het geratificeerde ontwerp §3.2 bindt args als
"member initialisations (`Toekenningen`) on the callee's input
**attributes**" — een boolean is een kénmerk, geen attribuut. De plugin
weigert dit luid (translate.ml:1054-1057 waardepositie-boolean;
translate.ml:1244-1249 "boolean subscope input ... refused", WF-027). En
zelfs met een `toe_kenmerk=true`-toekenning eist **WF-027** (beschermd pad,
spec/verifier/lib/checks.ml:737-741; normatieve MUST IR-SPEC.md:922) dat de
kenmerk-initialisatiewaarde een boolean **constante** is; de tutorial geeft
een caller-kénmerk (variabele), geen literal — ref faalt idem
(ref/lib/eval.ml:1316-1322). Een niet-literale kenmerk-initialisatie (een
caller-kenmerk over de verbindendeRol naar een callee-kenmerk gekopieerd)
is niet ontworpen en vergt een besluit + wijziging aan beschermde paden
(WF-027 + ref + clingo). Buiten het ontwerp → BLOKKADE, geen eigen keuze.
De tutorial heeft géén andere subscope, dus dit blokkeert de exitgetuige
volledig (ook ref-only kan niet).

**B2 — clingo kan datum-literalen niet vertalen (clingo-been).**
`IncomeTaxComputation`'s exceptieboom vertakt op datum-literalen
(`current_date < |2000-01-01|`). Clingo: `TRANSLATION FAULT: non-numeric
literal in a term position` (backends/clingo/lib/translate.ml:553-560) —
de gedocumenteerde backend-limiet (datum.catala_en is dáárom ref-only).

**B3 — clingo onveilige variabele bij ListLength-in-geldproduct
(clingo-been).** `$10.000 * (decimal of (number of individuals) + ...)`
grondt tot `co15(S,((10000*L2)+C14)) :- ...` met **L2 ongebonden**:
`error: unsafe variables ... 'L2' is unsafe`. Onafhankelijk van B2.

### Gevolg en aanbeveling

"Drievoudig groen (incl. clingo)" is voor de volle tutorial onbereikbaar
binnen het ontwerp: B1 blokkeert élke executor, B2/B3 blokkeren het
clingo-been zelfs voor de datum-/subscopevrije delen. De subscope-
constructie zélf is al bewezen (subscope.catala_en ref+clingo groen; de
jutterswet-rekenkamer-anker). Aanbeveling voor de maintainer: (a) een
ontwerpbesluit + vervolgkaart voor boolean/kenmerk-subscope-invoer
(WF-027-verruiming naar genavigeerde kenmerk-initialisatie), plus (b)
clingo-been-uitbreidingen (datum-literal → dagnummer; ListLength-term-
binding) — geïnventariseerd als flanken naast de 2-4-restvormen. De
veilige operand-vulanalyse-verfijning (uitputtende basisgroep) kan bij die
vervolgkaart landen zodra er een groene fixture is die hem uitoefent. Het
5a-exitcriterium (PLAN §6) valt hiermee NIET in deze kaart.

---

## WF-027-verruiming geland (2026-08-24, MAINTAINERSBESLUIT besluit 1)

De spec-kant van de B1-blokkade (boolean/kenmerk-subscope-invoer) is
opgeheven door de geratificeerde WF-027-verruiming: een kenmerk-initialisatie
in een ObjectCreatie mag nu een boolean-getypte EXPRESSIE dragen (de constante
blijft de v0-vorm; de parallel met attribuut-`Toekenningen`; additief op de
wire). Spec-first geland:

- `spec/IR-SPEC.md`: WF-027 (ch. 3.3) geherformuleerd — de waarde MOET
  boolean-getypt zijn (constante = v0-vorm); MemberInit-tabel en de
  RuleVersion-waarderegel (ch. 3.1) bijgewerkt met de v0-spiegelnotitie
  (de v0-emitter produceert alleen de constante-vorm; de expressievorm is de
  v1-verruiming die de Catala-frontend benut). WF-022
  (RuleVersion-kenmerktoekenning = constante true) is ONGEWIJZIGD — buiten
  besluit 1.
- `spec/RUNTIME-SEMANTICS.md` ch. 5.5: de kenmerk-init-waarde evalueert op
  het creatie-subject (als attribuut-inits); waar → gezet, onwaar → ongezet
  (gesloten-wereld, equivalent aan een afwezige init).
- `spec/verifier/lib/checks.ml` WF-027: `typ_of_expr` naar boven verplaatst
  zodat de check hem kan gebruiken; boolean-getypt → geaccepteerd, statisch
  niet-boolean (numeriek/tekst-literal, evident niet-boolean knoop) →
  afgekeurd met WF-027, de v0 onbekend-sentinel verschuift naar de
  runtime-fail-fast (de WF-023-skip).
- `spec/CONSTRUCTS.md` rij 21: gedocumenteerde delta.
- `corpus/negatief/wf027-nonboolean-characteristic-init/`: pint de nieuwe
  grens (een numeriek-getypte kenmerk-init afgekeurd met WF-027).

ROOD-EERST bewezen (letterlijke uitvoer): de huidige verifier keurt een
kenmerk-init met een boolean-`AttribuutRef` af ("must be a boolean constant");
na de verruiming PASS; de nieuwe negatief blijft afgekeurd ("must be a
boolean-typed expression"). Gates 1-4 groen (self-contained; ir-schema 78
files; verifier 78 golden clean + 137 negatief; case-schema 202). Geen
bestaande golden breekt — de verruiming verbreedt alleen (het anker
`jutterswet-penninguitgifte` houdt zijn constante kenmerk-init).

Nog open in deze kaart (besluit 2 + executor-stappen): de clingo-flanken B2
(datum-literaal in termpositie) en B3 (ListLength-in-term-binding); ref +
clingo executor-ondersteuning voor de niet-constante kenmerk-init; de
plugin-splitsing van `EScopeCall`; de tutorial-exitgetuige.

## ref-evaluatie kenmerk-init-expressie + corpus-getuige geland (2026-08-24)

De ref-kant van de WF-027-verruiming (besluit 1) is geland, met een
corpus-anker dat hem uitoefent.

- `ref/lib/eval.ml` (`run_object_creatie`): een kenmerk-initialisatie met een
  expressie-waarde evalueert nu op het creatie-subject (dezelfde
  caller-context als attribuut-`Toekenningen`, de RuleVersion-Kenmerktoekenning-
  spiegel) — `Bool true` → kenmerk gezet op de geminte instantie, `Bool false`
  → niet gezet (gesloten-wereld), niet-boolean → fail-fast. De twee
  boolean-constante-takken (`Literal "waar"`/`"onwaar"`) staan exact zoals ze
  waren (v0-vorm ongewijzigd). Smalste wijziging: alleen de `_`-fallthrough van
  fault naar evaluatie gesplitst.
- Nieuw zuster-anker `corpus/golden/anchor/jutterswet-rekenkamer-overzee/`:
  Jutter (caller) draagt een boolean read-bron `overzees`; de ObjectCreatie
  `richt de rekenkamer op` initialiseert het callee-kenmerk
  `Rekenkamer.overzees` uit die caller-lezing (de niet-literale
  kenmerk-init-expressie). Een voorwaardelijke callee-regel
  (`KenmerkCheck overzees`) geeft de overzeese Rekenkamer een +1000-toeslag.
  `case1-overzees` (overzees=waar → callee-kenmerk gezet → heffing 6000) en
  `case2-binnengaats` (overzees=onwaar → niet gezet → basisbedrag 5000, zelfde
  grondslag 5000) pinnen beide flanken absoluut. ir.json canoniek
  geserialiseerd (gate-2-slice via /tmp/gen_overzee.py), byte-identieke
  round-trip.
- Notitie (bijvangst/open flank): de caller-bron is een boolean *attribuut*,
  geen kenmerk — een value-positie-kenmerklezing heeft geen expressie-image in
  de IR (de plugin-hek `frontends/catala/lib/translate.ml:1054-1057`: "booleans
  are characteristics ... a value-position read of a characteristic has no
  image"). Het callee-*doel* is een echt kenmerk (het WF-027-subject) en de
  callee-regel leest het via `KenmerkCheck`. Een genavigeerde
  kenmerk-lees-expressie zou een nieuwe knoop vergen (ontwerpvraag → buiten
  deze kaart).

ROOD-EERST (letterlijke uitvoer): de ref VÓÓR de fix faalt op beide nieuwe
golden met `EVALUATION FAULT: characteristic initialisation of "overzees" is
not a boolean constant (WF-027)` (exit 1); NA de fix `oracle: PASS`. De
noop-refutatie faalt beide (`MISMATCH: J1.heffing = leeg, expected 6000` /
`J2.heffing = leeg, expected 5000`), dus geen noop-survivors.

Gates 1-5 en 7 volledig groen (self-contained; ir-schema 79; verifier 79
golden clean + 137 negatief; case-schema 204; oracle 187; noop 165 refute + 22
survivors). `@ref/runtest` groen: 250 worked examples (WV-5.5-charinit-* toe-
gevoegd — kaart-gesanctioneerd: expr-true/false, de v0-constante-vorm, en de
niet-boolean fail-fast).

CLINGO ROOD-EERST voor de volgende agent (gate 6, verwacht rood op de nieuwe
golden): `TRANSLATION FAULT: characteristic initialisation of "overzees" is not
a boolean constant (WF-027)` op beide cases. Site:
`backends/clingo/lib/translate.ml:2599-2604` — de kenmerk-init-tak matcht alleen
`Literal "waar"/"onwaar"` en fault op de `_`-fallthrough; de expressievorm moet
in ASP (evalueer de caller-context-boolean tot de `heeft/1`-hoofdguard) landen,
naast de B2/B3-flanken. De golden staan gecommit (rood-eerst voor het
clingo-been); gate 6 is NIET gedraaid als groen-eis in deze stap.

## clingo kenmerk-init-expressie geland (besluit 1, executor-stap) (2026-08-24)

De clingo-kant van de WF-027-verruiming: het kenmerk-deposit in
`translate_object_creatie` wordt conditioneel op de vertaalde boolean-
expressie, gelijk aan ref's gesloten-wereld-lezing. De twee
boolean-constante-takken (`Literal "waar"`/`"onwaar"`) staan byte-voor-byte
zoals ze waren; alleen de `_`-fallthrough van fault naar
expressie-vertaling gesplitst.

- `backends/clingo/lib/translate.ml`: nieuwe helper `bool_atoms` (een
  boolean-getypte kenmerk-init-EXPRESSIE → de body-atomen die exact bij TRUE
  gelden). Een boolean is een kenmerk in v0, dus een own-subject
  boolean-attribuutlezing is `[heeft(S,a)]` — aanwezig iff waar. Het deposit
  `heeft(ocI(S),m) :- <body>, <bool atoms>` vuurt op waar, blijft ongezet op
  onwaar. De case-emissie draagt een boolean-INPUT-attribuut nu ook als
  `heeft(label,a)` (waar) / afwezig (onwaar) — de `bool_atoms`-leesvorm aan de
  case-kant (was: fault op `Value.Bool` via `scaled_of_value`).
- `spec/ASP-TRANSLATION.md` S11: de kenmerk-init-expressievorm en de
  boolean-attribuut-carriage gedocumenteerd (bestaande sectie uitgebreid).

ROOD-EERST (letterlijke uitvoer, vóór de fix, beide cases):
`TRANSLATION FAULT: characteristic initialisation of "overzees" is not a
boolean constant (WF-027)`. NA de fix `clingo: PASS (2 expectations)` op beide.
Gate 6 volledig groen (187 cases). Bijvangst: de boolean-attribuut-carriage in
de case-emissie is een nieuwe representatie (booleans-als-kenmerk), vereist
door het caller-boolean-attribuut van het rekenkamer-overzee-anker.

## clingo B2 — datum-literaal in termpositie geland (besluit 2) (2026-08-24)

Een datum-literaal (`|2000-01-01|`) in een termpositie vertaalt nu naar
zijn burgerlijk dagnummer op schaal 1 — dezelfde representatie die een
datum-attribuutlezing (`val/3`), de puntlezing en de Rekendatum-constante al
gebruiken (geen tweede datumcodering). ref fault datum-REKENKUNDE
(`numeric_ok` hekt de additieve/multiplicatieve families), dus dit raakt
alleen vergelijkingsoperanden.

- `backends/clingo/lib/translate.ml` (`expr_term` Literal): `Some Datum_in_dagen`
  → `Value.date_of_iso` → `([], dagnummer, 1)` (de Rekendatum-tak-spiegel). De
  tekst/enum/boolean-literalen in termpositie faulten nog steeds.
- `spec/ASP-TRANSLATION.md` S4 (Literal-bullet) uitgebreid; de S19-regel
  gesplitst (datum-literaal vertaalt nu; tekst/enum/boolean-literaal faultt nog).
- Nieuwe golden `corpus/golden/jutterswet-verjaring/` (eigen-geschreven,
  rechtenvrij): een Jutter is `verjaard` als `peildatum` eerder is dan
  2000-01-01, `actueel` als niet-eerder — twee kenmerktoekenningen over een
  datum-literaal-vergelijking (`<` en `>=`). case1 (1999-06-15 → verjaard),
  case2 (2005-03-20 → actueel); beide kenmerken absoluut gepind door BEIDE
  executors, beide noop-gerefuteerd (geen survivor).

ROOD-EERST (letterlijk, vóór de fix): clingo `TRANSLATION FAULT: non-numeric
literal in a term position`; ref `oracle: PASS` op beide. NA de fix clingo
`PASS (3 expectations)` op beide, gate 6 volledig groen (189 cases).

## clingo B3 — ListLength in termpositie (self-contained Let-binder) geland (besluit 2) (2026-08-24)

Een `Let`-gebonden bare waarde (een `ListLength`-telling, een `Fold`-resultaat,
een attribuutlezing) die in een termpositie werd gelezen, grondde tot een
onveilige variabele: de binder-atomen zaten alleen in de `rule_guard`, maar
coalesce0 (S5), de range-hek (S6) en de geld-productdeelproducten bouwen hun
hulpregels uit de LOKALE operand-atomen — dus een binder-lezing binnen zo'n
hulpregel groundde `L2` ongebonden (`co7(S,(L2+C6)) :- …, is_a(S,"jutter")`).

- `backends/clingo/lib/translate.ml`: nieuwe helper `let_binder` maakt een
  atoom-dragende `Let`-waarde SELF-CONTAINED via een per-subject hulpregel
  `letN(S,term) :- is_a(S,ot), <waarde-atomen>` (voor een `ListLength` is dat
  het `#count`-aggregaat over de bestaande `lpres`/`lelem`-reïficatie). De
  binder-lezing draagt daarna het enkele `letN`-atoom, dus de waarde is gebonden
  waar hij ook landt — in een hulpregel-body of de omhullende regel. Een
  atoom-vrije waarde (een literal) is al grond en krijgt geen hulpregel. Het
  `ctx.binders`-type draagt nu de binder-atomen mee (`string list * string *
  int`); recurrence-gebonden binders (match-payload, lambda/fold-parameter)
  houden een lege atoomlijst (de omhullende join grondt hun ASP-variabele).
  Toegepast op de drie regel-waarde-`Let`-sites (`expr_term`, `value_branches`,
  de geld-product-peel). De leeg-propagatie blijft ongewijzigd (de hulpregel is
  nog steeds een positieve join over de waarde-atomen).
- `spec/ASP-TRANSLATION.md` S18(a) herschreven (de "atomen in de rule_guard"-
  claim die B3 weerlegde, gecorrigeerd naar de self-contained hulpregel); S15
  ListLength-alinea met een verwijzing uitgebreid. Geen S19-regel geschrapt: B3
  was een latente onveilige-variabele-bug (een clingo-grounding-fout), geen
  gedocumenteerde luide `Value.fault`-weigering.
- Nieuwe golden `corpus/golden/jutterswet-postenheffing/` (eigen-geschreven,
  rechtenvrij): `heffing = let aantal_posten = number of posten in 100.00 *
  (aantal_posten + number of posten)` — een `Let`-gebonden `ListLength` (de
  self-contained binder) náást een bare `ListLength`, samen in een product.
  case1 (`[12,7,9]` → 3 posten → 600.00), case2 (`[]` → 0 posten → 0.00, de
  aanwezig-maar-lege-lijst-telt-0 semantiek van ch. 10.1); beide absoluut gepind
  door BEIDE executors, beide noop-gerefuteerd (geen survivor). Magnitude bewust
  klein (100.00 schaalt naar 10000 ≤ 46340) zodat de multiplicatieve range-hek
  niet vuurt.

ROOD-EERST (letterlijk, vóór de fix, beide cases):
`error: unsafe variables in: co7(S,(L2+C6)):-…,is_a(S,"jutter").` / `note:
'L2' is unsafe` — grounding gestopt. ref `oracle: PASS` op beide. NA de fix
clingo `PASS (2 expectations)` op beide, gate 6 volledig groen (191 cases,
ref ≡ clingo overal). Gates 1-8 groen (self-contained; ir-schema 81; verifier
81 golden clean + 137 negatief; case-schema 208; oracle 191; noop 169 refute +
22 survivors; rood 17).

Bijvangst: de fix is generiek (elke atoom-dragende `Let`-waarde, niet alleen
`ListLength`) — een `Fold`-resultaat of attribuutlezing gebonden aan een `Let`
en gelezen in een gecoalescte term had dezelfde bug; de discriminator is
`va = []` (atoom-vrij → geen hulpregel) versus niet-leeg, de natuurlijke grens,
geen `ListLength`-special-case. Bestaande `Let`-goldens (tariefstelling) krijgen
nu `letN`-hulpregels in hun ASP maar blijven semantisch identiek en gate-6-groen.

Nog open in deze kaart: de plugin-splitsing van `EScopeCall` en de
tutorial-exitgetuige (B1 opgeheven op spec/ref/clingo-niveau; de plugin- en
frontend-fixture-stappen resteren).

## tutorial-exitgetuige drievoudig groen — plugin-verruiming geland (2026-08-24)

De B1-blokkade is nu ook op plugin/frontend-niveau opgeheven: `tutorial_end_2_3`
loopt drievoudig groen (plugin → verifier → ref → clingo). Vijf plugin-stappen
plus één clingo-uitbreiding.

- **Boolean subscope-invoer (besluit 1, item 1a/1c).**
  `frontends/catala/lib/translate.ml`: een boolean caller-variabele die als
  subscope-arg-WAARDE wordt gelezen (`value_read_bools`) wordt als boolean
  ATTRIBUUT gedeclareerd (de rekenkamer-overzee-anker-vorm: ref leest uit attrs,
  clingo als `heeft`), en `read_var` produceert er een `AttribuutRef` voor
  i.p.v. te weigeren. In `build_subscope` wordt de boolean-arg een
  kenmerk-`Toekenning` (`toe_kenmerk=true`) met die vertaalde waarde — de
  callee-kenmerk-init (WF-027-verruimde vorm). De waarde-positie-refusal blijft
  elders intact (`refuse_bool_value` weigert nog); een boolean die óók als
  conditie gelezen wordt, weigert luid (gemengde carriage).
- **Vulanalyse-verfijning (item 1b).** Een uitputtende datum-complementaire
  basisgroep (article_2 `< 2000` / `>= 2000` met `false ⊢ ∅`) wordt als 'gevuld'
  erkend: `base_is_filled` herkent een twee-lid-`Group_rung` met
  complementaire vergelijkingsoperatoren (`< / >=`, `> / <=`) over
  structureel-gelijke operanden (`Expr.equal`). Totaal → nooit leeg → geen
  coalesce-vs-hard-divergentie (het uitgestelde besluit blijft ongeraakt).
- **Cross-scope regelnaam-disambiguatie (bijvangst).** De tutorial heeft twee
  `household_tax`-outputs (Article 7 collectief, Article 8 per-individu) op
  verschillende objecttypen → botsende globale regelnaam (WF-020). `file_to_document`
  berekent de regel-variabelenamen die in >1 scope voorkomen; `translate_scope`
  qualificeert ALLEEN die met het subject (`berekening HouseholdTaxComputation
  household_tax`). Byte-neutraal voor de 6 bestaande catala-goldens (geen enkele
  heeft een cross-scope regelnaam-botsing).
- **`infer_dt` LRat + ToRat (bijvangst).** `1.0 + decimal of kids / 2.0` typeerde
  als `null`/int i.p.v. num_rat (4 dec), waardoor `money_mult_shape` in clingo
  faalde en `$10.000 * rate` via de generieke Maal de int31-range-guard trok.
  `infer_dt` (op de Catala-expr) krijgt `ELit (LRat _) → num_rat` en
  `ToRat_int|ToRat_mon → num_rat`, zodat de computed-rate money-product de
  split-encodering neemt. Byte-neutraal (geen golden heeft een LRat/ToRat als
  eerste Binair-operand).
- **clingo struct-subscope-invoer (bijvangst, naast B2/B3).**
  `backends/clingo/lib/translate.ml`: een struct/sum-getypte `Toekenning`
  (`individual : Individual` als subscope-invoer) rijdt zonder numerieke schaal
  in de `val`-head (spiegel van de translate_regel struct-target-tak), en een
  whole-value struct-`AttribuutRef`-LEZING in `expr_term` krijgt schaal 0
  (`value_scale`) i.p.v. `pow10 (attr_decimals …)` dat op `Struct_type` faultte.

**ROOD-EERST (letterlijk, originele plugin (HEAD) op de fixture):**
`catala_frontend: refused: variable "tax_rate" read as an arithmetic/comparison
operand is not always filled …` (exit 3). Na de vulanalyse-fix verschuift de
refusal naar `boolean variable "overseas_territories" read in a value position`;
na de boolean-subscope-fix loopt de fixture schoon.

**Fixture + cases.** `frontends/catala/test/tutorial_end_2_3.catala_en` (catala-book
`examples/tutorial_end_2_3.catala_en` @ commit `62c35379`, de ontwerp-getrouwe
`sum integer of`-vorm; de upstream Test-scopes — inline subscope-calls buiten het
fragment — weggelaten, grondwaarheid via een /tmp-kopie met #[test]-scopes en
`catala interpret --no-stdlib`). Drie cases, absoluut geijkt: binnengaats-laag
($15.000, 0 kinderen, 1999 → household_tax $7.000,00); overzee-hoog en
binnengaats-hoog ($100.001, 5 kinderen, 2005 → $9.999,75 vs $4.999,70, de
article_6-25%- tegen article_5-30%-flank). De kinderen ≤ 5 respecteren clingo's
int31-grens op de deling `kids/2.0` op schaal 10^8 bij de gepinde S=4 (een
gedocumenteerde backend-limiet, geen bug); de overseas-flank blijft observeerbaar
in household_tax.

**Gates.** Gate 9: `PASS — frontend suite green (120 checks); 6 catala golden(s)
regenerate byte-identically` (de tutorial voegt 8 checks toe: translate,
verifier-clean, 3 cases × ref+clingo). Gates 1-8 volledig groen (self-contained;
ir-schema 81; verifier 81 golden + 137 negatief; case-schema 208; oracle 191;
clingo 191; noop 169+22; rood 17) — de corpus-artefacten zijn ongewijzigd (de
wijzigingen raken alleen de frontend, de clingo-backend en de nieuwe
frontend-fixture). Refuse-fixtureset intact: per-element, multi-state, context,
whole-struct en de waarde-positie-kenmerklezing weigeren alle nog; de
creatie-sleutel-botsing is een defensieve backstop die Catala niet kan uitdrukken
(distincte instance-var-namen → distincte sleutels).

Nog open: de spec/PLAN.md §6-Met-markering voor het 5a-exitcriterium en de
opus-lensronde (buiten deze opdracht).

## Lensronde — drie CONFIRMED bevindingen verwerkt (2026-08-24)

Twee opus-lenzen op de subscope/tutorial-kaart leverden drie bevindingen; alle
drie gefixt, alle negen gates groen, de 6 catala-goldens byte-identiek.

**Bevinding 1 (BLOCKING, door beide lenzen onafhankelijk bevestigd) — de lege
flank van de WF-027-verruimde kenmerk-init divergeerde.** Een kenmerk-init
waarvan de boolean-expressie LEEG evalueert (het caller-attribuut ontbreekt in
de case, schema-geldig) liet ref faulten (`characteristic initialisation of X
is not boolean: leeg`) terwijl clingo stil doorrekent (leeg/false vallen samen
op atoom-afwezig → geen deposit → schoon antwoord). **Gekozen fix: leeg → het
kenmerk wordt NIET gezet, géén fault, in beide executors identiek.** Motivering
(de maintainerslijn die het besluit zelf aanriep): (a) de
attribuut-initialisatie-parallel — een attribuut-init schrijft een lege waarde
immers *stil* door, empirisch bevestigd; (b) de gesloten-wereld-lezing maakt
leeg en false ononderscheidbaar voor een kenmerk-check, precies zoals clingo's
afwezige atoom beide is. De fault-tak blijft voor écht niet-boolean waarden
(getallen/strings). Verwerkt:
- `spec/RUNTIME-SEMANTICS.md` ch. 5.5 + `spec/IR-SPEC.md` ch. 3.3 (WF-027):
  de drie-flanken-lezing genormeerd (waar→gezet; onwaar én leeg→ongezet, met de
  parallel + gesloten-wereld-motivering).
- `ref/lib/eval.ml` (`run_object_creatie`): `Value.Empty` naar de ongezet-tak
  (`Value.Bool false | Value.Empty -> w`); smalste wijziging.
- `corpus/golden/anchor/jutterswet-rekenkamer-overzee/case3-onbepaald.json`:
  overzees weggelaten → heffing 5000 (de gewone-rekenkamer-regel; zelfde
  uitkomst als de expliciete-onwaar-flank). Handberekend en absoluut gepind;
  noop-refuter (MISMATCH leeg vs 5000). Bewaakt door gate 5 én 6.
- `ref/test/test_ref.ml`: `WV-5.5-charinit-expr-empty` toegevoegd
  (kaart-gesanctioneerd, finding 1d — 251 worked examples).
  ROOD-EERST (letterlijk): vóór `EVALUATION FAULT: characteristic
  initialisation of "overzees" is not boolean: leeg (WF-027)` (exit 1); na
  `oracle: PASS (2 expectations)`; clingo ongewijzigd al `PASS`.

**Bevinding 2 (MINOR, lens 1) — WF-027 vertrouwde de gedeclareerde node-typ.**
Een `AttribuutRef` met een leugenachtige `typ={Booleaan}` op een numeriek
attribuut passeerde de verifier en divergeerde runtime. Fix: de
WF-027-waardecheck kruisvalideert voor een AttribuutRef-waarde nu het
OPGELOSTE attribuut-datatype tegen Booleaan (nieuwe helper
`resolved_attr_datatype`, analoog aan WF-023's kruisvalidatie; de
`Datatype_onbekend`-sentinel blijft de skip, een niet-oplosbare of
niet-AttribuutRef-waarde valt terug op de node-typ). Nieuwe negatief-case
`corpus/negatief/wf027-lying-attribuutref-type/` pint de leugen-vorm.
ROOD-EERST (letterlijk): vóór `verifier: PASS` (geaccepteerd); na `WF-027 FAIL
/objectCreaties/0/toekenningen/0: characteristic initialisation's value must
be a boolean-typed expression` (exit 1).

**Bevinding 3 (MINOR, lens 2) — de clingo-grens van de gesplitste
geldencodering stond alleen in de vergankelijke doing-kaart.** Het intermediair
van de computed-rate deling (`1.0 + decimal of kids / 2.0`) schaalt 10^8 en
overschrijdt clingo's int31 bij kids ≥ 6 (ref rekent correct, clingo vuurt de
range-hek; een gedocumenteerde backend-limiet, geen bug). Gedocumenteerd waar
een lezer hem vindt: een proza-notitie in
`frontends/catala/test/tutorial_end_2_3.catala_en` bij de cases-verwijzing én
een notitie in `spec/ASP-TRANSLATION.md` S18(c) (geen nieuwe S-sectie; S19
blijft de hoogste).

Gates (finaal, alle negen groen): self-contained PASS; ir-schema 81; verifier
81 golden + **138** negatief; case-schema **209**; oracle **192**; clingo
**192**; noop **170** refute + 22 survivors; rood 17; catala PASS (120 checks,
6 goldens byte-identiek).

Result: de subscope-lowering van PHASE5C-TUTORIAL §3 landde volledig op de
bestaande kernel (geen nieuwe knoop) en de tutorial-exitgetuige draait
drievoudig groen. Geland: het anker jutterswet-rekenkamer (creatie +
MemberInits + verbindendeRol + genavigeerde output-lezing; callee-regels in
de caller-fixpoint, zónder executor-wijziging); de plugin-splitsing
EScopeCall → creatie + genavigeerde reads met de catala-golden
jutterswet-rekenkamerheffing-catala en de E2E-getuige subscope.catala_en;
het maintainersbesluit 2026-08-24 (genummerde vragen, beide A): de
WF-027-verruiming — een kenmerk-initialisatie draagt een boolean-getypte
expressie — door spec, verifier, ref en clingo (anker
jutterswet-rekenkamer-overzee), plus de clingo-flanken B2 (datum-literaal
in termpositie, anker jutterswet-verjaring) en B3 (de generieke
Let-binder-hulpregel, golden jutterswet-postenheffing);
tutorial_end_2_3 als frontend-fixture door plugin → verifier → ref →
clingo (gate 9: 120 checks; cases geijkt tegen catala interpret), met de
vulanalyse-verfijning (uitputtende datum-complementaire basisgroep) en de
cross-scope-naamdisambiguatie; PLAN §6 Phase 5 "Met in part" voor het
plugin-exitcriterium; CONSTRUCTS-rijen 9/21 bijgewerkt. Rood-eerst per
stap, vastgelegd in de landingsnotities hierboven. De lensronde (twee
opus-lenzen, beide empirisch) ving één BLOCKING — de lege-boolean-flank
van de kenmerk-init divergeerde (ref fault, clingo stil door); genormeerd
op leeg → ongezet langs de attribuut-init-parallel die het besluit zelf
aanriep, gefixt in spec+ref met anker-case3 als permanente gate-5/6-wacht
— en twee MINORs (WF-027-kruisvalidatie van het opgeloste
attribuut-datatype + negatief-case; de kids≤5-magnitudegrens
gedocumenteerd in fixture en S18), alle gefixt vóór de merge (0410244).
Bleef bewust liggen, luid en geregistreerd: per-element-subscope-calls
(2-4), multi-state, context-inputs (besluit 6 geratificeerd-maar-
uitgesteld onder de WF-050-randvoorwaarde), whole-struct-consumptie,
de creatie-sleutel-botsing (backstop, door Catala niet uitdrukbaar),
geneste struct-veldtoegang in clingo (pre-existing kaart-1-flank), de
genavigeerde kenmerk-lees-expressie (geen IR-image; ontwerpvraag), de
kids≥6-grens van de gesplitste geldencodering (gedocumenteerde
backend-limiet: ref rekent, clingo hekt), en het frontend-diff-criterium
van Phase 5 — dat is kaart 5d.
