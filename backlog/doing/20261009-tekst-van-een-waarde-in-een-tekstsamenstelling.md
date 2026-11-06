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

## Result

*Gebouwd op 2026-11-06 op branch `card/text-output-foldfilter` (worktree `wt/clingo-round2`), afgetakt van main
`fd03c6217`. Het besluit is VOORLOPIG: het aanbevolen antwoord van het Fable-advies van 2026-11-06, gebouwd; de
eigenaar kan het omdraaien. Logs onder `uchar-dev/werk/clingo-round2/logs/`.*

- **(a) Spec.** RUNTIME-SEMANTICS ch. 1.2, "Text coercion": de alinea van het besluit naar de huidige stand
  geredigeerd (geen plakwerk van het advies), met de tabel per type en het uitgewerkte voorbeeld WV-1.2c-text; de
  voorlopigheid en de drie afwijkingen van ALEF staan er cursief bij. IR-SPEC ch. 4.2 (`TextComposition`) en
  ch. 4.3 (`concatenation`) verwijzen ernaar. ASP-TRANSLATION noemt de grens CLINGO-042. Geen WF-regel (het besluit
  is "welgevormd").
- **(b) Golden.** `corpus/golden/jutterswet-tekst-van-een-waarde/` (`source.rgs`, gate 10), twee handgeschreven
  cases met 62 verwachtingen: bedrag in €, geheel getal, percentage, waarheidswaarde, datum (ook jaar 0999),
  datum-tijd (met en zonder fractie), opsomming, literal-deel `«5,50»`, een breuk (`11/6`, `5/3`, `-1/3`) en een
  eindigende deling (`1,75`), negatieve getallen, leeg, en het uitgewerkte voorbeeld. `leeg gedeeld door 3` is 0
  (ch. 1.2, de linkeroperand van een deling coalesceert), dus `Deel: 0`. De sum-typerij en de lijst/record-fout
  staan niet in de golden (de frontend spelt geen constructor, en `de concatenatie van` is in de frontend geen
  waarde, DIVERGENCES "concatenatie as a standalone value"); ze worden gedragen door ref's suite
  (`WV-1.2c-text-*`, 23 rijen plus twee voorbeeldrijen) en door de nieuwe Drools-test `TextOfAValueTest`.
- **(c) Uitvoerders, rood eerst.** Vóór de bouw (`logs/k1-red-ref.log`): ref schreef `5.5`, `2026-06-21`,
  `@1782032519999ms` (29 mismatches); clingo `TRANSLATION FAULT: CLINGO-002` (de bodem); Drools (jar van main)
  `EXCLUDE DROOLS-183` (`logs/k1-red-drools-main.log`). Na de bouw: ref `oracle: PASS (41)` en `(21)`; clingo
  weigert met de nieuwe limiet `CLINGO-042 text-composition-no-string-functions` (`symbolic_term`, ook voor een
  samenstelling van alleen teksten en voor `concatenation`; twee suitechecks), de golden staat in
  `EXPECTED_CHANNEL` van gate 6; beide Drools-takken `PLAYED 2`. `DROOLS-183` is `RETIRED` (`textPart` laat elk
  type door); `rsTekst` is de typegerichte omzetting voor beide takken (getal via `Breuk`, `waar`/`onwaar`,
  `dd-MM-uuuu`, `dd-MM-uuuu HH:mm:ss.SSS`, `ConstructorValue`, anders een fout). De test
  `TmsBuiltinWitnessTest.a_text_composition_with_a_number_is_refused` controleerde de weigering die deze kaart
  opheft; hij is vervangen door `a_text_composition_with_a_number_reads_its_decimal_with_a_comma` (5,50 → `Bedrag:
  5,5`, leeg → `Bedrag: `).
- **(d) Gates.** Los gedraaid op de werkboom vóór de commit, alle rc=0: 1, 2, 3, 4, 5 (suite 2579 voorbeelden), 6
  (2760 suitechecks; vijf goldens in het weigerkanaal), 7, 8, 10, 12 (`EXPECTED_PLAYED` 354 → 355, beide takken
  355; JUnit 1874), 13, 15, 17 (`logs/k1-gates-a.log`, `logs/k1-gates-b.log`, per gate `logs/k1-check-*.log`).
  `make gates` volgt aan het eind van de ronde.

Pins die bewogen: gate 12 `EXPECTED_PLAYED` +1 (de nieuwe golden speelt; na de rebase op main `3df621a37`, dat
`jutterswet-lidnamen` toevoegde, 355 → 356); gate 6 één regel in `EXPECTED_CHANNEL`.
`CLAUDE.md` noemt DROOLS-183 bij de RETIRED-codes van gate 12. De catalogusregels van DROOLS-183 blijven, zoals bij
elke RETIRED-code.

Voor de hoofdsessie: `CLAUDE.md`, gate 6, beschrijft twee goldens in `EXPECTED_CHANNEL`; de lijst noemt er nu zes
(de twee verzuimrente-ankers, `jutterswet-keurquotient`, `jutterswet-macht-van-nul`, `jutterswet-tekst-van-een-waarde`).
Voorgestelde CONSTRUCTS-rijtekst: "Tekst van een waarde (TextComposition, concatenation): de tekst is een functie
van de waarde; getal exact met decimale komma zonder eindnullen, `t/n` niet-eindigend, `waar`/`onwaar`,
`dd-MM-jjjj`, `dd-MM-jjjj uu:mm:ss.mmm`, constructor `tag(payload)`; lijst/record een fout. Voorlopig besluit
2026-11-06 (Fable, ALEF gemeten). ref en beide Drools-takken; clingo weigert als limiet CLINGO-042."

Eindrun van de ronde: `make gates` op `2df79f6a3`, alle zeventien gates en `after-run` rc=0, red=0, 435 s
(`out/gates/20261106-164133/summary.txt` in de worktree, log `uchar-dev/werk/clingo-round2/logs/gates-final.log`).

### Rebase en DRL-pins

Op verzoek van de hoofdsessie gerebased op main `3df621a37` (de Drools-opruimronde). Conflicten alleen in de
boekhouding: `EXPECTED_PLAYED` wordt de som (main 355, plus de drie goldens van deze ronde = 358, alle
commentaarregels behouden), de RETIRED-lijst van gate 12 in `CLAUDE.md` noemt DROOLS-045/046, DROOLS-058 en
DROOLS-183, en in RUNTIME-SEMANTICS ch. 5.5 blijft main's nieuwe meting van `jutterswet-erflijn` staan zonder de
zin over de nestgrens. De `make gates`-runs hierboven (red=0) liepen vóór de rebase, op de toen geldende hashes.
`DrlTextPinWitnessTest` met `-Duchar.drlpin` schreef drie nieuwe pins, voor `jutterswet-tekst-van-een-waarde`,
`jutterswet-vouwfilter-buiten-de-beurt` en `jutterswet-kwantorvouw-en-unie`; geen bestaande pin veranderde (ook niet
door `rsTekst`: geen golden van main heeft een tekstdeel van een ander type dan tekst). Gate 12 met de suite na de
rebase: PASS, beide takken `PLAYED` 358, JUnit 1884 tests (`DrlTextPinWitnessTest` inbegrepen), `logs/f3-check-drools.log`.
