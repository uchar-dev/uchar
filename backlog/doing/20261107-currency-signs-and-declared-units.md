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
