# tekst-van-een-waarde-in-een-tekstsamenstelling: de spec legt niet vast hoe een getal of een waarheidswaarde in een tekst staat

*Gevonden op 2026-10-09 door een bouwer van `20261008-tms-expressievormen-en-rekendatum` (stap 1) en door de
hoofdsessie vastgelegd volgens de afspraak dat een bestaande bug in de spec, ref, clingo of de gedeelde runtime een
kaart krijgt. Gemeten op die kaartbranch met ref, clingo en de verifier uit `_build` van de worktree en de shaded jar
van `backends/drools`; het document staat onder `/tmp/lens-tms-k2/proef/tekstdeel/` en hieronder beschreven.*

## Aanleiding

`TextComposition` voegt de waarde van een expressie in een tekst in (IR-SPEC ch. 4.2). De spec zegt alleen dat een
leeg deel als `""` leest (RUNTIME-SEMANTICS ch. 1.2, "Text coercion of empty yields the empty string"). Hoe een
gevulde waarde van een ander type dan tekst er staat, zegt hij niet, en de uitvoerders verschillen.

Het document: objecttype Vondst met `basis` (een getal met 2 decimalen), `gemeld` (een waarheidswaarde) en twee
tekstattributen, en twee regels: "De omschrijving van een Vondst moet berekend worden als "Bedrag: " met zijn basis"
en "De status van een Vondst moet berekend worden als "Gemeld: " met zijn gemeld". Case: V1 met basis 5,50 en gemeld
waar.

| Uitvoerder | omschrijving | status |
|---|---|---|
| ref | `Bedrag: 5.5` | `Gemeld: waar` |
| clingo | weigert bij de vertaling: "a text/enum/boolean operand here is an own-subject attribute read or a literal" | |
| Drools, oude exec-model-tak en oude DRL-teksttak (`RegelSpraakRuntime.rsTekst`) | `Bedrag: 5.50` | `Gemeld: true` |
| Drools, tak met truth maintenance | weigert het deel met naam (sinds `20261008-tms-expressievormen-en-rekendatum`) | |

Geen golden heeft een tekstdeel van een ander type dan tekst. Dezelfde vraag geldt voor een datum, een percentage, een
opsomming, een bedrag met een eenheid, en voor de `concatenation`.

## Vraag aan de eigenaar (spec)

Welke tekst is normatief voor een gevulde waarde van een ander type dan tekst in een `TextComposition` of
`concatenation`? Kandidaten, per type: het minimale decimale lexeem ("5.5", zoals ref), de gedeclareerde schaal
("5.50", zoals de runtime), of de Nederlandse schrijfwijze ("5,50"); voor een waarheidswaarde "waar"/"onwaar" (ref) of
"true"/"false" (de runtime). Of: een tekstdeel van een ander type is niet welgevormd en de frontend schrijft een expliciete
omzetting.

## Acceptatie (bij oppakken aanscherpen)

- (a) Het besluit in RUNTIME-SEMANTICS ch. 1.2 (beschermd pad: voorstel), met een uitgewerkt voorbeeld met absolute
  tekst per type, of een WF-regel met een negatief geval als het besluit "niet welgevormd" is.
- (b) Een golden met de vormen van het besluit, eigen Jutterswet-casus, rechtenvrij.
- (c) Ref, clingo en de Drools-takken geven de besloten tekst; de tak met truth maintenance draagt het deel in plaats van
  het te weigeren; `rsTekst` volgt.
- (d) Alle gates van `CLAUDE.md` groen.

## Raakvlakken

- `20261008-tms-omschakeling`: tot deze kaart weigert de tak met truth maintenance een tekstdeel van een ander type,
  waar de oude exec-model-tak "5.50" en "true" schrijft; geen golden raakt het.

## Omvang

Klein tot middel: een normvraag, één zin in de spec, een golden en vier uitvoerders.

May touch: `spec/RUNTIME-SEMANTICS.md` (voorstel), `corpus/golden/<nieuwe golden>/`, `ref/`, `backends/clingo/`,
`backends/drools/` (de runtimehulp en de takken), kaartverplaatsing.
