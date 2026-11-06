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

## Voorlopig besluit (Fable-advies 2026-11-06)

*Voorlopig: het aanbevolen antwoord van een Fable-advies, gebouwd en geland; de eigenaar kan het omdraaien. Het volledige
advies, met de ALEF-meting (ALEF's eigen regressietests over tekstexpressies, interpreter en gegenereerde code, alle
PASS), de drie bewuste afwijkingen van ALEF, de spectekst, het ontwerp van de golden en wat niet mag veranderen:
`uchar-dev/werk/norm-advice-2026-11-06/text-of-a-value/ADVIES.md` (§3 is de acceptatie).*

De tekst van een gevulde waarde in een `TextComposition` of een `concatenation` is een functie van de waarde alleen,
niet van het knooppunt dat haar levert; een literal-deel leest als de waarde die het aanduidt. Per type: een tekst is
zichzelf; een opsommingswaarde is haar label; een waarheidswaarde is `waar` of `onwaar`; een getal, ook een
percentage, een bedrag of een getal met een eenheid, is zijn exacte decimale vorm zonder eindnullen en met een
decimale komma, zonder duizendtalscheider, zonder eenheid, zonder procentteken en zonder opvulling tot de
gedeclareerde decimalen (`5,50` → `5,5`; `5` → `5`; `12,50 %` → `12,5`; `1250,50 €` → `1250,5`), en een getal dat niet
eindigt is zijn breuk in laagste termen met het teken op de teller (`4/3`, `-1/3`); een datum is `dd-MM-jjjj`
(`21-06-2026`); een datum-tijd is `dd-MM-jjjj uu:mm:ss.mmm` met altijd drie cijfers achter de punt
(`21-06-2026 09:01:59.999`); een waarde van een sum type is de naam van haar constructor, bij een constructor met
payload gevolgd door de tekst van de payload tussen haakjes (`Afgewezen`, `Toegekend(1250,5)`); leeg blijft `""`.
Een lijst- of recordwaarde heeft geen tekst en blijft een fout. Normbron: de RegelSpraak-specificatie §5.4 (een
reeks van teksten en waarden zet de waarde van een expressie van elk datatype om) en, waar zij zwijgt, ALEF's
runtime-omzetting `TekstUtil.toAlefText`, gemeten op 2026-11-06 (§2); de drie bewuste afwijkingen van ALEF staan in
§2.4.

Uitvoerders volgens het advies: ref volgt; clingo kan de vorm niet dragen (geen tekstfuncties in ASP-Core-2) en weigert
als limiet met de volgende vrije code van `backends/clingo/codes.tsv`, met de golden in `EXPECTED_CHANNEL` van gate 6;
beide Drools-takken dragen het deel (`DROOLS-183` wordt `RETIRED`) met een typegerichte omzetting in `rsTekst`.

## Acceptatie (bij oppakken aanscherpen)

- (a) Het besluit hierboven in RUNTIME-SEMANTICS ch. 1.2 (beschermd pad: voorstel), met een uitgewerkt voorbeeld met absolute
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
