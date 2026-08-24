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

Status: **GEBLOKKEERD**. `tutorial_end_2_3.catala_en` kan binnen het
geratificeerde ontwerp géén drievoudig-groene (plugin → verifier → ref →
clingo) frontend-fixture worden. Drie onafhankelijke blokkades, elk met
letterlijk bewijs. Het ontwerp is NIET gewijzigd; de werkboom is schoon
(geen code-diff).

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
