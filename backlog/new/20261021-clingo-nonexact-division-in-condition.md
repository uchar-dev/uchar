# clingo-nonexact-division-in-condition: clingo weigert een kale niet-exacte deling in een voorwaarde als EF-006

*Vondst van de kaart `clingo-day-kind-walk-after-fold-stop` (2026-10-21), gemeten met een proef in een scratchpad: de
matrix van `jutterswet-keurgemiddelde` zonder de afronding, lid W2 met 7 / 2. Luid: clingo is UNSATISFIABLE waar ref een
antwoord geeft; geen stil fout antwoord. Geen golden draagt de vorm.*

## Aanleiding

Een kale deling die niet exact is (7 / 2) in een voorwaarde geeft in de clingo-backend
`viol("non-exact division (an explicit rounding is mandated before precision is lost)", …)` met de code EF-006, zowel
voor een lid na de stop van een kwantorvouw als voor een bereikt lid. ref vergelijkt het exacte rationale getal en
schrijft niets, dus speelt beide. EF-006 is "a written value does not fit the declared decimals of its target"
(RUNTIME-SEMANTICS ch. 5.7b, ch. 2.4): hier wordt niets geschreven, dus draagt de weigering een code die niet past. Het
bereik van de vouw (`param_reach`) zou alleen de helft na de stop dichten.

## Doel

De clingo-backend vergelijkt een kale deling in een voorwaarde exact, zoals ref, of weigert haar met een eigen
`CLINGO-###`-code (register `backends/clingo/codes.tsv`, gate 8) in plaats van EF-006; de kaart kiest op wat de
vertaling toelaat en zegt waarom. Een deling die een waarde schrijft, houdt EF-006.

## Acceptatie

- (a) Rood eerst: een golden met een kale niet-exacte deling in een voorwaarde (en, als de keuze exact vergelijken is,
  in de matrix van een kwantorvouw na de stop), rood in clingo vóór de fix, groen in ref en Drools; of, bij de keuze
  weigeren, de vorm in de divergentielijst van gate 8 met de nieuwe code.
- (b) Gates 5, 6, 8, 12 en 17 groen; de eigen suite van de clingo-backend groen.

May touch: `backends/clingo/`, één golden of een divergentie-entry, deze kaart.
