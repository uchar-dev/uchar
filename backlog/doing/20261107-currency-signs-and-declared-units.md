# currency-signs-and-declared-units: valutatekens in de bron, en een eenheid die haar stelsel declareert

*Opdracht van de eigenaar, 2026-11-07: de twee open punten van `backlog/done/20261107-unit-symbol.md` aanpakken.*

Goal: (1) de RegelSpraak-frontend lext een valutateken als eenheidsspelling, zodat `de Amerikaanse dollar USD $` en
`de Braziliaanse real BRL R$` te schrijven zijn; (2) de verifier weigert een eenheid op een `Number` die een stelsel noemt
dat haar afkorting niet declareert (een nieuwe arm van WF-009), zodat het OPEN punt van IR-SPEC ch. 4.6 (r) sluit.

## Deel 1: valutatekens in de lexer

Nu kent de lexer alleen `€` (`RegelSpraakLexer.g4`: `EURO : '€' ;`), en `eenheidAfkorting` neemt `LOWER_ID`, `UPPER_ID`,
`EURO` en de tijdwoorden. `$`, `£`, `¥`, `₽` en dergelijke zijn geen token, dus `USD $` is niet te schrijven. De
RegelSpraak-specificatie definieert een `<eenheidsafkorting>` als karakterreeks (§13.3.5.4); de lexer van uchar is daar
strenger, en dit deel verkleint dat verschil.

- **Het token.** Eén lexerregel voor een valutateken: een teken uit de Unicode-categorie Sc (currency symbol,
  `[\p{Sc}]`; de lexer gebruikt al `\p{L}`), eventueel voorafgegaan door letters (`R$`, `US$`, `NZ$`). `€` valt eronder;
  het token `EURO` gaat op in de nieuwe regel (of blijft als eerste alternatief, als dat de rest van de frontend
  eenvoudiger houdt; technische keuze van de bouwer). Een Engelse tokennaam (`CURRENCY_SIGN`).
- **Meten vóór de keuze**: geen bron van de repository mag anders lexen. Een letter direct gevolgd door `€` buiten een
  tekst (`bedrag€`) zou met het letterprefix één token worden; tel of dat voorkomt. Alle `.rgs` van de repository
  emitteren byte-gelijk (gates 10, 11, 16).
- **De uitzondering op RGS-362.** `DefinitionResolver` (r. ~745) laat `€` toe zonder declaratie (`a.EURO() == null`,
  sinds `c12177d73`). Zoek uit waarom; houd het gedrag voor `€` gelijk, en geef andere valutatekens die uitzondering
  alleen als dezelfde reden voor hen geldt. Zonder reden: een ander valutateken moet gedeclareerd zijn, zoals elke
  eenheid.
- **Semantic tokens**: het nieuwe token krijgt de soort die `EURO` had (`SemanticTokens`, r. ~356), en een verwijzing
  met een gedeeld symbool krijgt geen plaats (zoals sinds `unit-symbol`).
- **DIVERGENCES**: in "De eenheidsregel — afkorting en symbool" (of een eigen sectie) welke tekens een eenheid mag
  spellen, met de meting.
- **Tests** (frontend): `USD $`, `CAD $` en `AUD $` in één stelsel; `met eenheid $` is RGS-390 en noemt de drie
  afkortingen; `met eenheid USD` werkt; `R$` is één token en het symbool van BRL; `100 $` en `$/jr` bij een gedeeld
  symbool RGS-390; een enkel `£` als symbool van GBP; `«X (eenheid)»` schrijft `(R$)` en `(£)`. Een golden is niet
  nodig (de draad draagt het symbool als gewone string en `jutterswet-tekst-van-een-waarde` speelt het symbool al);
  voeg er een toe als het goedkoop is.

## Deel 2: een eenheid die haar stelsel declareert (WF-009)

IR-SPEC ch. 2.5 en ch. 4.6 (r): WF-034 (r) eist dat een aanwezig `system` van een eenheid een gedeclareerd
`UnitSystem` noemt, en laat de vraag of haar `abbreviation` een eenheid is die dat stelsel declareert bewust aan WF-009,
waar ze OPEN staat (de scope-noot van WF-009, de zin "Whether the unit's `abbreviation` is a unit that system declares
is WF-009's question and stays open" in (r), en de lijst "Measured, still open" rond r. 4120). Sinds `unit-symbol` leest
een tekstdeel het symbool van de declaratie; zo'n eenheid heeft er geen, en ref faalt en Drools weigert (DROOLS-369) op
een document dat de verifier goedkeurt.

- **Regel.** Een nieuwe arm van WF-009: een eenheidsfactor die een `system` noemt, MUST een `abbreviation` hebben die dat
  stelsel declareert, vergeleken onder de identiteit van ch. 2.1 (zoals WF-009 een `canonical` vergelijkt; het stelsel
  zelf lost op zoals WF-034 (r) het oplost). Gerapporteerd op de pointer van de factor, met een sleutel
  `WF-009.<slug>` en parameters (in elk geval `unit` en `unitSystem`). Overal waar een eenheid op de draad staat (de
  wandeling die WF-034 (r) al loopt). Een factor zonder `system` (`%`, de kale kalendereenheden) valt erbuiten.
- **Meten vóór de regel**: tel de eenheidsfactoren met een `system` in de vier zones, `toka/` en de negatieven, en
  hoeveel de nieuwe arm weigert. Een golden, een red-case of een `toka/`-slice die weigert: stop en meld het in het
  Result, niet ombouwen. Een negatief van een andere regel die ook deze arm raakt: nagaan of gate 3 dat toelaat, en zo
  nodig het negatief zo aanpassen dat het alleen zijn eigen regel breekt, met reden in zijn NOTE.md.
- **Spec.** IR-SPEC: de arm in WF-009, de scope-noot en de zin in (r) naar de huidige stand, de regel uit de lijst
  "still open", de WF-tabel. RUNTIME-SEMANTICS ch. 1.2: de zin "A factor that names a system the document does not
  declare, or an abbreviation its system does not declare, … a broken document" verwijst naar WF-034 (r) en WF-009;
  ref en DROOLS-369 blijven het melden (een executor leest de draad ook zonder verifier).
- **Getuigen.** Een negatief `corpus/negative/wf009-<slug>/` met `ir.json`, `expect.txt` (sleutel en parameters) en
  `NOTE.md`; het passerende geval is elke golden met een eenheidsstelsel.
- **Andere lezers.** Meet wat ref (zonder verifier), clingo en de Drools-loader op het negatief doen en schrijf het in
  het Result; verander ze niet, tenzij een van hen er stil mee rekent (dan het Result en een voorstel, geen stille
  verbreding).

## Acceptance

- Deel 1: de frontendtests hierboven; gate 10 (JUnit, her-emissie, register en catalogi als er een code bijkomt), 11 en
  16 groen.
- Deel 2: het negatief weigert op zijn arm (gate 3); geen golden, red-case of toka-slice weigert (gates 3 en 11).
- `make gates` in de worktree: alle zeventien gates en `after-run` rc=0. Geen vloer daalt.

May touch: `frontends/regelspraak/`, `spec/IR-SPEC.md` (WF-009, ch. 4.6 (r), de lijst "still open", de WF-tabel),
`spec/RUNTIME-SEMANTICS.md` (ch. 1.2, die ene zin), `spec/verifier/`, `corpus/negative/` (het nieuwe negatief, en
negatieven die de nieuwe arm onbedoeld raken), `scripts/check-verifier.sh` alleen als een vloer of lijst het vraagt,
deze kaart. Een golden alleen als hij in deel 1 goedkoop is.

Out of scope: `spec/CONSTRUCTS.md`, `spec/PLAN.md`, `CLAUDE.md` (voorstellen in het Result); ref, clingo en Drools
veranderen (alleen meten); gelijkheid van `canonical` en `factorToCanonical` van een voorkomen met haar declaratie (als
dat een gat blijkt: in het Result als open punt); de distributie (pin-bump na de landing).

## Result

*Gebouwd op 2026-11-07 op branch `card/currency-signs-and-declared-units` (worktree `wt/currency-signs`), afgetakt van
main `89950cc4d`. Logs, probes en meetscripts onder `uchar-dev/werk/currency-signs/` (`logs/`, `probe/`,
`measure_units.py`, `tools/TokenDump.java`).*

### Commits

- `8f68b5c53` — deel 2: de arm `WF-009.undeclared-abbreviation` in `check_unit_form` (`spec/verifier/lib/checks.ml`),
  IR-SPEC (WF-009, ch. 2.5, ch. 4.6 (r), de lijst "Measured, still open", de WF-tabel), RUNTIME-SEMANTICS ch. 1.2 (de
  ene zin), het negatief `corpus/negative/wf009-undeclared-abbreviation`.
- `a0b696222` — deel 1: het token `CURRENCY_SIGN` (lexer, grammatica, `DefinitionResolver`, `SemanticTokens`),
  DIVERGENCES, toetsen.
- Dit Result.

### Deel 2: een eenheid die haar stelsel declareert (WF-009)

**Meting vóór de regel** (`measure_units.py`, `logs/p2-measure.log`; elke `Unit`-knoop, declaraties inbegrepen,
vergeleken onder de identiteit van ch. 2.1):

| Zone | Documenten | Factoren | Met `system` (waarvan declaraties) | Stelsel onbekend (WF-034) | Afkorting niet gedeclareerd |
|---|---|---|---|---|---|
| `corpus/golden` | 407 | 643 | 572 (107) | 0 | 0 |
| `corpus/red` | 65 | 57 | 24 (7) | 0 | 0 |
| `corpus/negative` | 636 | 1167 | 1042 (274) | 4 | 0 |
| `toka` | 30 | 880 | 790 (420) | 0 | 0 |
| `ref/test/fixtures` | 77 | 257 | 257 (46) | 0 | 0 |
| `backends/drools/src/test/resources` | 51 | 36 | 32 (13) | 0 | 0 |

Geen golden, red-case of toka-slice weigert; deel 2 kon door. Alle 808 declaraties in de vier zones dragen een `system`
gelijk aan de naam van hun eigen stelsel.

**De arm.** In `check_unit_form`, de wandeling die WF-034 (r) en WF-092 al delen, zodat elke positie van een eenheid
meedoet (declaraties, modellaag, expressietypes, fold-bronnen, lambdaparameters): een factor met een `system` dat
oplost, moet een `abbreviation` hebben die een `units`-declaratie van dat stelsel is, onder `norm`. Sleutel
`WF-009.undeclared-abbreviation`, parameters `unit` (de afkorting) en `unitSystem` (het stelsel zoals de factor het
schrijft), op de pointer van de factor. Een factor zonder `system` valt erbuiten; een `system` dat geen stelsel noemt,
blijft alleen WF-034. `system_names` heet nu `declared_systems` en geeft de stelsels zelf, omdat de arm hun eenheden
leest.

**Verifier-sweep over alle 1267 documenten** (vier zones, fixtures van ref en Drools; `logs/p2-sweep.log`): de eerste
versie loste een stelsel op als het eerste met die naam, en raakte zo ook `wf001-duplicate-unit-system` (stelsels
`Gewicht` en `gewicht`; de `g` van `gewicht` "niet gedeclareerd" door `Gewicht`). Gate 3 liet dat toe, maar het was een
tweede citaat op een document dat WF-001 al weigert, en het was onwaar voor het tweede stelsel. Daarom: waar twee
stelsels de naam dragen (WF-001), mag elk van beide de eenheid declareren. Daarna raakt de arm alleen zijn eigen
negatief.

**Spec.** IR-SPEC WF-009: de arm na de kalenderarm, met sleutel, parameters, positie, wat erbuiten valt, waarom het
ertoe doet (een tekstdeel leest het symbool van de declaratie; omrekenen leest de factor zelf) en de getuige; de
scope-noot naar de huidige stand. Ch. 2.5 ("The wire form of a unit"): de zin "WF-009 checks only the table's internal
consistency" werd onwaar en zegt nu dat WF-009 eist dat het stelsel de afkorting declareert. Ch. 4.6 (r): de twee
zinnen naar de huidige stand. De lijst "Measured, still open": de WF-009-regel eruit. De WF-tabel: arm, gate, positief,
negatief. RUNTIME-SEMANTICS ch. 1.2: de zin verwijst naar WF-034 (ch. 4.6 (r)) en WF-009 (ch. 2.5) en zegt dat een
executor die de declaratie zoekt het ook meldt.

**Het negatief** `wf009-undeclared-abbreviation`: `jutterswet-tekst-van-een-waarde` met de `system` van beide factoren
`kg` (attributen `gewicht` en `prijs`) van `Gewicht` naar `Munt`. Alleen de nieuwe arm rapporteert (twee keer).
Rood eerst: de verifier van `89950cc4d` gaf `verifier: PASS` op het negatief (`logs/p2-red-first.log`).

**Wat de andere lezers op het negatief doen** (met de twee case-bestanden van de golden; `logs/p2-executors.log`,
`logs/p2-drools.log`):

| Lezer | `case1-gevuld` | `case2-leeg` |
|---|---|---|
| ref | `EVALUATION FAULT: the text part writes the unit kg, whose factor 'kg' the unit system 'Munt' does not declare` | `oracle: PASS (31 expectations)`: zonder gewicht schrijft het deel geen eenheid |
| clingo | `TRANSLATION FAULT: CLINGO-042` (de eigen grens van de golden) | idem |
| Drools, beide takken | `EXCLUDE DROOLS-369: … no unit system of the document declares its factor 'kg' of the unit system 'Munt' …` | (één aanroep voor beide cases) |

Omdat clingo de golden zelf al weigert, nog een probe die clingo speelt (`probe/stelselweging-t-gewicht.json`,
`logs/p2-probe-stelselweging.log`): in `jutterswet-stelselweging` de drie voorkomens van `t` (Massa) naar het stelsel
`Gewicht`, dat geen `t` declareert. De verifier weigert nu drie keer; ref `PASS (37)`, clingo `PASS (37)`, Drools beide
takken `PLAYED 1`. Geen lezer rekent er stil fout mee: elk leest de omrekening van de factor zelf (`canonical`,
`factorToCanonical`), niet van de declaratie. Niets aan ref, clingo of Drools veranderd.

**Gate 3** (`logs/p2-gate3.log`): 472 golden/red schoon, 637 negatieven (+1), 482 op hun arm (+1), 266 armsleutels,
804 parameterregels (+2), 364 rapportsleutels (+1), 464 sleutelliteralen in `checks.ml` (+1), 797 pointers (+2). Geen
vloer veranderd. Gate 2 en gate 1 groen.

### Deel 1: valutatekens in de lexer

**Meting vóór de keuze** (`tools/TokenDump.java`, `logs/p1-tokens-{before,after,diff}.txt`): de 359 `.rgs` van de
repository (corpus 263, `toka/casus` 47, `ref/test` 38, `backends/drools` 11) geven 59424 tokens; met het nieuwe token
lexen ze identiek (`EURO` en `CURRENCY_SIGN` als één naam gelezen; de diff is leeg). Ze schrijven 178 valutatekens,
alle `€`, geen enkel direct na een letter (`bedrag€` komt niet voor), en geen ander teken uit Sc. Ook de testbronnen
in Java (frontend, Drools, `toka/src`) dragen een `$` alleen in Java zelf (formaatcodes `%1$s`, klassenamen
`CaseRunner$SessionMeasurement`, een reguliere expressie), niet in een RegelSpraak-tekst.

**Het token.** `CURRENCY_SIGN : LETTER* [\p{Sc}] ;` vervangt `EURO`. Eén token voor elk teken houdt de rest van de
frontend eenvoudiger dan `EURO` als eerste alternatief: `eenheidAfkorting` neemt `CURRENCY_SIGN`, `a.EURO()` wordt
`a.CURRENCY_SIGN()`, en `SemanticTokens` geeft het de lexicale soort `punctuation` die `EURO` had (op een
eenheidspositie claimt de resolutie het als `unit`). De distributie leest `EURO` nergens (gezocht in de lokale checkout
buiten de submodule, alleen gelezen). `eenheidWoord` neemt geen teken: het zelfstandig naamwoord blijft een woord.

**De uitzondering op RGS-362.** Uit `c12177d73` (commitboodschap, DIVERGENCES "A number over a unit", de javadoc van
`unitOf`): RGS-362 weigert een naam na de schuine streep direct na een getal omdat `2/foo` anders als een deling leest
die niet plaatsvindt; `2 foo` en `2 1/foo` houden de oudere lezing van een kale eenheid. `€` mocht daar zonder
declaratie omdat het teken geen naam van een waarde is (geen attribuut, parameter of variabele kan het dragen), zodat
`2/€` niet als deling kan lezen, net als `5 €` een eenheid zonder declaratie is. Die reden geldt voor elk token
`CURRENCY_SIGN`, `R$` inbegrepen (een naam bestaat uit woorden, en `R$` is geen woord). Dus: elk valutateken is daar
uitgezonderd. Gedrag voor `€` gelijk. De javadoc noemt de reden nu.

**Toetsen** (JUnit 1550, alle groen; `logs/p1-gate10.log`):
- `UnitSymbolTest` (+6): `a_currency_sign_is_one_token_with_the_letters_before_it` (`$`, `R$`, `US$`, `£`, `€`,
  `bedrag€`, `BRL R$`, `100 $/jr`); `three_units_may_carry_the_dollar_sign_and_each_abbreviation_names_its_unit`
  (`USD $`, `CAD $`, `AUD $` in één stelsel, `met eenheid USD` en `CAD/jr`);
  `a_reference_with_the_shared_dollar_sign_names_none_of_the_three` (`met eenheid $`, `100 $` en `$/jr`: RGS-390 met
  `'USD', 'CAD', 'AUD'`); `a_sign_with_letters_and_a_single_sign_are_the_symbols_of_their_units` (`R$` van BRL, `£`
  van GBP); `a_text_part_with_its_unit_reaches_the_currency_symbol_of_its_declaration`;
  `the_noun_of_a_unit_line_is_one_word` (zie open punt 1).
- `SemanticTokensTest.a_currency_sign_is_one_span_and_a_shared_one_points_at_no_declaration` (+1).
- `UnitIdentityTest.a_calendar_unit_and_the_euro_sign_need_no_declaration_over_a_number` heet nu
  `a_calendar_unit_and_a_currency_sign_need_no_declaration_over_a_number` en kreeg de rijen `2/$`, `2/R$` en `2/£`; de
  vier bestaande rijen staan ongewijzigd. Dit is de enige bestaande toets die geraakt is.

**`«X (eenheid)»` schrijft `(R$)` en `(£)`.** De frontend kan niet rekenen; zijn toets houdt vast dat het deel zijn
eenheid vraagt en dat de declaraties het symbool dragen. Gemeten met ref en Drools op een bron met BRL `R$`, GBP `£` en
USD `$` (`probe/munt.rgs`, `probe/munt-case.json`, `logs/p1-probe-text.log`): verifier `PASS`, ref `oracle: PASS`,
Drools beide takken `PLAYED 1` op de verwachting `12,50 (R$), 3,00 (£), 7,25 ($)`. Geen golden: een golden met een
tekstdeel vraagt een regel in `EXPECTED_CHANNEL` van gate 6 (beschermd pad buiten de kaart) en een DRL-pin in de
Drools-toetsen (buiten "May touch"), dus niet goedkoop.

**DIVERGENCES**, sectie "De eenheidsregel — afkorting en symbool": de alinea "What a token after the noun may be" (welke
tokens een eenheid mag spellen, het teken na letters, `bedrag€`, het woord van een eenheidsregel) met de meting en de
nieuwe getuigen; "A number over a unit": een valutateken in plaats van "the sign `€`", met de reden; "Semantic tokens":
de lijst van `punctuation`.

**Gates** los, na de bouw: 10 (JUnit 1550, 209 goldens her-emitteerd, 380 RGS-codes en beide catalogi; geen nieuwe
code), 11 (45 TOKA-bronnen her-emitteerd, 28 slices verifier-schoon, TOKA-suite 28), 16 (43 scenariogoldens, 155
case-bestanden), 1. Gate 1 viel eerst op het woord `toka` in DIVERGENCES (de meting noemde de map); de zin noemt nu
"the parity zone".

### make gates

Op `a0b696222`: alle zeventien gates en `after-run` rc=0, red=0, 435 s (`out/gates/20261107-113420/summary.txt` in de
worktree; kopie met de statusregels onder `werk/currency-signs/logs/gates-20261107-113420/`, log
`logs/gates-final.log`). Gate 9 (catala) draaide mee: 250 checks, 12 goldens. Het Result (dit commit) raakt alleen de
kaart.

### Afwijkingen en eigen keuzes (met reden)

1. **Twee stelsels met één naam.** De kaart zegt "het stelsel zelf lost op zoals WF-034 (r) het oplost"; WF-034 vraagt
   alleen of er een stelsel met die naam is. Waar twee stelsels de naam dragen, mag elk van beide de eenheid
   declareren: anders citeert de arm onwaar op een document dat WF-001 al weigert (gemeten op
   `wf001-duplicate-unit-system`). IR-SPEC zegt het in één bijzin.
2. **Sleutel `undeclared-abbreviation`.** Gate 3 eist een slug van ten hoogste drie woorden;
   `unit-undeclared-in-system` (vier) viel af.
3. **Ch. 2.5 aangepast** buiten de vijf plaatsen die "May touch" noemt: de zin "WF-009 checks only the table's
   internal consistency" in "The wire form of a unit" was met de arm onwaar geworden.
4. **Valutatekens met letters** (`R$`): het prefix is `LETTER*` zoals de kaart zegt, dus ook kleine letters
   (`bedrag€` is één token); de meting liet geen bron zien die daardoor anders lext.

### Open punten

1. **Het woord van een eenheidsregel is één token.** `de Amerikaanse dollar USD $`, het voorbeeld uit het doel van de
   kaart, leest `Amerikaanse` als woord, `dollar` als afkorting en `USD` als symbool, en `$` is RGS-389
   (`UnitSymbolTest.the_noun_of_a_unit_line_is_one_word`). De RegelSpraak-specificatie schrijft een `<naamwoord>`
   (§13.3.5.1), dat meer woorden kan hebben. Met een woord met koppelteken is het te schrijven
   (`de VS-dollar (mv: VS-dollars) USD $`). Meer woorden toelaten is een grammaticabesluit: met `(mv: …)` is de grens
   van het woord zichtbaar, zonder is de regel dubbelzinnig (`de grote euro EUR` tegen `de euro EUR €`). Niet gebouwd.
2. **Twee eenheden met één woord in een stelsel**: de latere neemt het woord stil. `de dollar USD $` en
   `de dollar CAD $`: `met eenheid dollar` noemt CAD zonder melding, en de emitter schrijft de spelling `dollar` alleen
   op CAD (`probe/dollar-word.rgs`). Geen bron van de repository doet het (79 eenheidstelsels, 115 eenheidsregels
   geteld). Het raakt deze kaart direct: drie dollars met één woord is de voor de hand liggende schrijfwijze. IR-SPEC
   ch. 2.5 ("What a spelling means is OPEN") zegt dat de eerste lezer die op spelling oplost, spellingen uniek binnen
   hun stelsel nodig heeft. Voorstel: een RGS-code bij de tweede declaratie van een woord of meervoud in een stelsel,
   zoals RGS-016 voor een afkorting (of, naar het model van RGS-390, een dubbelzinnige verwijzing). Niet gebouwd: niet
   gevraagd, en het vraagt een normkeuze en een code met catalogi.
3. **Factor van een voorkomen tegen die van de declaratie** (buiten scope, gemeten): in `jutterswet-stelselweging` de
   drie voorkomens van `t` met `factorToCanonical` 100 in plaats van de gedeclareerde 1000
   (`probe/stelselweging-t-factor100.json`, `logs/p2-probe-factor.log`): `verifier: PASS`, en ref rekent met de factor
   van het voorkomen (`MISMATCH: V1.ondergewicht = 250, expected 1000`, vijf mismatches). Een gat: geen regel houdt
   `canonical` en `factorToCanonical` van een voorkomen gelijk aan die van haar declaratie, en de executors lezen het
   voorkomen. Clingo en Drools niet gemeten.
4. **Het `system` van een declaratie** tegen haar eigen stelsel: geen regel; gemeten 808 van 808 gelijk. Een
   declaratie in A met `system: B`, waar B dezelfde afkorting declareert, passeert elke regel.
5. **`corpus/negative/wf034-unit-unknown-system/NOTE.md`** zegt in "Scope" dat de afkortingsvraag van WF-009 is "and
   stays out of scope (ch. 4.6, and the open list says so)": dat laatste is niet meer waar. Niet aangepast (het negatief
   raakt de arm niet, dus buiten "May touch").
6. **De distributie**: pin-bump na de landing. Het token `EURO` bestaat niet meer; de distributie noemt het nergens.

### Voorstellen voor de hoofdsessie

- **CLAUDE.md, gate 3** (achter de zin van WF-129): "Since the card `currency-signs-and-declared-units` (2026-11-07)
  WF-009 refuses a unit that names a declared unit system which does not declare its abbreviation
  (`WF-009.undeclared-abbreviation`, with `unit` and `unitSystem`; IR-SPEC ch. 2.5), at every position a unit occupies,
  in the walk WF-034 (r) and WF-092 share; witness `wf009-undeclared-abbreviation`."
- **CLAUDE.md, gate 10** (achter de zin van RGS-388): "Since the card `currency-signs-and-declared-units` (2026-11-07)
  a currency sign is one token, `CURRENCY_SIGN` (a character of the Unicode category Sc after the letters it writes:
  `€`, `$`, `R$`; it replaced `EURO`), which a unit line and a unit reference take as abbreviation or symbol and which
  needs no declaration after the slash of a number (DIVERGENCES "De eenheidsregel"); no new RGS code."
- **CONSTRUCTS rij 26**, in de datumgeschiedenis vóór de regel van `unit-symbol`: "2026-11-07, card
  `currency-signs-and-declared-units`: a unit that names a system has an abbreviation that system declares
  (`WF-009.undeclared-abbreviation`); the RegelSpraak frontend reads a currency sign (Unicode Sc, after its letters:
  `$`, `R$`) as one token, an abbreviation or a symbol."
- **Geen vloer daalde.** Gate 3 zie boven; JUnit 1550 (vloer 173); RGS-codes 380 (vloer 347). Geen vloer telt
  valutatekens of WF-009-armen, dus er hoeft geen vloer omhoog.
