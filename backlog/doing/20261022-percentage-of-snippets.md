# percentage-of-snippets: elke operand van `het percentage X van Y` draagt haar eigen brontekst

*Uit het Fable-advies over de uitleg van berekende waarden
(`uchar-dev/research/uitleg-berekende-waarden-2026-10-22/fable-advies.md`, §1.4, §5 U5, beslispunt 6). De eigenaar wil
dat dit direct wordt opgelost. Het stond eerst als sectie in `check-admits-instance-cycle`. Fable-advies 2
(`research/bruggen-2026-10-22/fable-advies-2.md`, vraag 3) maakt er een eigen kleine kaart van, die naast de verruiming
kan lopen.*

## Aanleiding

Beide operanden van `het percentage X van Y` dragen de brontekst van de hele uitdrukking als snippet
(`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/…/ExpressionBuilder.java` r. ±3039–3053). Daar krijgen
`new Expr.ParameterRef(pct, source)` en `attributeRef(…, source)` de `source` van de `Binary`. Een uitlegzin die de
operanden met hun waarde noemt, noemt dan twee keer de hele frase. Dat raakt de herlevingsgrens van v2, die de
toolchain-kaart `explanation-computed-attributes` (T1) met waarden wil verantwoorden.

## Doel

- De `ParameterRef` krijgt de bron van het eerste naamdeel.
- De operand V krijgt de bron van de overige delen met het onderwerp.

## Acceptatie

- (a) Rood eerst: een frontendtest pint de snippets van beide operanden.
- (b) Elke golden met een `percentageOf` wordt opnieuw uitgegeven; gates 2, 10 en 16 zijn groen. De verifier en de
  uitvoerders lezen geen snippet, dus gates 3, 5, 6, 12 en 17 veranderen niet.
- (c) De TOKA-slices met een `percentageOf` gaan mee, als gate 11 dat vraagt, en `toka/README.md` noemt de landing. De
  zeventien gates zijn groen.

May touch: `frontends/regelspraak/**`, `ir.json` en case-bestanden van de geraakte goldens, `toka/casus/ir.json`,
`toka/cases/**/ir.json` en `toka/README.md` (alleen als gate 11 dat vraagt), deze kaart. Start na de landing van
`third-condition-level`, dat ook `frontends/regelspraak` raakt.
