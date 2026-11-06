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

### Landing (hoofdsessie, 2026-11-06)

Geland op main na `make gates` op de stand vóór de rebase (red=0, 436 s) en, na de rebase op `3df621a37`, gate 12 en de
Drools-suite groen; CONSTRUCTS en de gate-6-tekst van `CLAUDE.md` in de landingscommit. De volledige set draait de
hoofdsessie op main na de laatste landing van de dag.

## Besluit van de eigenaar (2026-11-06 avond): een getal in de decimalen van zijn type

De eigenaar draait het voorlopige besluit voor getallen om: "de tekst is een string-literale representatie van de
waarde in een bepaald type, en als het type een precisie heeft van 2 decimalen, dan is de string literal daarbij 5,50
en niet 5,5". Dus:

- Een getal (ook een percentage, een bedrag, een getal met een eenheid) schrijft precies de decimalen van zijn type,
  met een decimale komma: 5,50 bij 2 decimalen is `5,50`, 5 bij 2 decimalen `5,00`, 5 als geheel getal `5`,
  12,5 % bij 2 decimalen `12,50`. Geen eenheid, geen procentteken, geen duizendtalscheider.
- Het type van een deel is het gedeclareerde type van wat het leest (attribuut, parameter, kenmerk), en van een
  samengesteld deel zijn precisiegrens (RUNTIME-SEMANTICS ch. 2.3, de grens die de precisiepoort al afleidt uit de
  declaraties en de vorm van de expressie): `zijn basis maal 1,1` met basis in 2 decimalen schrijft 3 decimalen. Een
  deel zonder eindige grens (de onbegrensde vorm, een deling) schrijft de exacte kortste vorm, en een getal dat niet
  eindigt `t/n`, zoals het voorlopige besluit.
- De rest van het voorlopige besluit (waarheidswaarde, datum, datum-tijd, opsomming, sum type, leeg, lijst/record,
  clingo's limiet CLINGO-042) blijft.

Een optie om de eenheid erachter te schrijven, tussen haakjes (`5,50 (€)`), besprak de eigenaar als mogelijke
uitbreiding van de taal; die hoort niet bij deze stap.

## Besluit van de eigenaar (2026-11-06 avond, vervolg): de eenheid op verzoek

De eigenaar vroeg de eenheid mee te nemen. Een auteur kan in een tekstsamenstelling de eenheid laten meeschrijven,
achter het getal tussen haakjes (`5,50 (€)`, `12,50 (%)`); zonder die aanwijzing blijft het getal zonder eenheid. Op de
draad een optioneel veld op `EmbeddedExpression` (additief); de verifier weigert de aanwijzing bij een type zonder
eenheid (nieuwe WF-regel); in de bron een uitbreiding binnen `«…»` (DIVERGENCES). Dezelfde bouwer, tweede stap.

## Result: Besluit van de eigenaar, gebouwd

*Gebouwd op 2026-11-06 op branch `card/text-in-type-decimals` (worktree `wt/text-decimals`), afgetakt van main
`55ecb7603`. Logs onder `uchar-dev/werk/text-decimals/logs/`.*

### Stap 1: een getal in de decimalen van zijn type

- **Spec.** RUNTIME-SEMANTICS ch. 1.2, "Text coercion", naar het besluit: een getal schrijft precies de decimalen
  van het type van zijn deel, met decimale komma, zonder eenheid, procentteken of duizendtalscheider. Het type van
  een deel is zijn precisiegrens van ch. 2.3 in de eenheid waarin zijn waarde staat: van een read de declaratie, van
  een binder de grens van de waarde die hij draagt, van een getal-literal de decimalen van zijn exacte waarde, van
  een samengesteld deel de grens van zijn vorm. Zonder eindige grens (of zonder claim) de exacte kortste vorm, en
  niet-eindigend `t/n`. De tabel en WV-1.2c-text naar de nieuwe stand, met rijen voor `zijn basis maal 1,1`
  (`6,050`), `gedeeld door 4` (`1,3750`), de literal (`5,5`), de onbegrensde vorm en `Toegekend(1250,50)`. De
  cursieve noot noemt het besluit en vier afwijkingen van ALEF (de vierde is nieuw: ALEF schrijft een getal zonder
  eindnullen, omdat zijn rationale getallen geen schaal dragen).
- **Eén grens, geen tweede.** Ref leest de grens van de verifier (`Ir_v0.Checks.precision`, dezelfde die WF-023 en
  clingo's `Scale` lezen); de binder-omgeving van ref (`btypes`) draagt nu per binder ook zijn grens, in de vorm
  van de omgeving van de verifier (Let, match-arm, lijstfilter, argument, element en accumulator van een gesloten
  body). Voor Java bestond de grens alleen in de frontend (`resolver/Typing`, een pakket dat de Drools-build
  uitsluit); die is verhuisd naar `core/PrecisionBound`, dat de frontend en de Drools-backend allebei compileren,
  met een `Reading` voor wat elk anders ziet: de typen (frontend: zijn eigen; Drools: `EffectiveUnit`, ch. 2.10),
  of een vouw over leden gaat (frontend: `AttributeReference.set`; Drools: een bronrol of bronpad) en de
  defaults-groepen. `Typing.precisionGate`/`allocationAssignment` lezen de verhuisde klasse; `RuleChecker.fits` is
  `PrecisionBound.fits` geworden. Gate 10 (JUnit en her-emissie) bleef groen.
- **Drools.** De analyse geeft `rsTekst` de vorm van het deel als constante mee (`rsTekst(waarde, vorm)`: een
  `Integer` met de decimalen, `null` voor de exacte vorm, voor een sum type een lijst van constructornaam en vorm
  van de payload); beide takken renderen dezelfde aanroep. Een `Let` draagt de grens van zijn binder in
  `Reads.binderBounds`.
- **Technische keuzes.** Een waarde die niet in de decimalen van haar deel past, kan op een welgevormd document niet
  voorkomen (de grens garandeert het); ref meldt dan een fout (`Value.fault`), `rsTekst` een
  `IllegalStateException`, nooit een afronding. Een constructorwaarde schrijft haar payload in het gedeclareerde
  type van haar constructor (`Toegekend(1250,50)`), volgens dezelfde regel.
- **Golden `jutterswet-tekst-van-een-waarde`.** Verwachtingen die veranderden (bewuste gedragsverandering door de
  eigenaar): V1 `omschrijving` `Bedrag: 5,5` → `Bedrag: 5,50`, `aandeeltekst` `Aandeel: 12,5` → `12,50`,
  `verslag` `Bedrag: 5,5 …` → `5,50 …`; V2 `Bedrag: 5` → `5,00`, `Aandeel: 100` → `100,00`, `verslag` `5` →
  `5,00`; V4 `Bedrag: -1` → `-1,00`, `Aandeel: -0,5` → `-0,50`, `verslag` `-1` → `-1,00`. Gelijk gebleven:
  `aantaltekst` (geheel), `deeltekst` (gedeeld door 3: geen eindige grens, `11/6`, `1,75`, `0`), `vaste tekst`
  (`«5,50»`: `5,5`), V3 (`5,25`, `0,25`), de datum-, tijd-, opsommings- en waarheidsdelen. Nieuw: `toeslagtekst`
  (`zijn basis maal 1,1`, grens 3: `6,050`, `5,500`, `5,775`, `-1,100`, leeg `0,000`), `factortekst` (de
  onbegrensde vorm: `0,125`, `2`, `0,5`, leeg) en `heffingtekst` (een "Daarbij geldt"-binder, `de heffing is zijn
  basis maal 1,1`, die de frontend als `Let` om de samenstelling schrijft: dezelfde teksten). 5 bij 2 decimalen is
  V2 (`Bedrag: 5,00`).
- **Rood eerst.** De nieuwe verwachtingen op de oude code: ref van `55ecb7603` 19 mismatches
  (`logs/red-ref-base.log`), de jar van `55ecb7603` `FAIL` op beide takken (`logs/red-drools-base.log`). Na de bouw:
  ref `oracle: PASS (53)` en `(27)`, beide Drools-takken `PLAYED 2`.
- **Tests.** Ref-suite: de tabelrijen naar de nieuwe stand en een blok door de evaluatie (gedeclareerd, onbegrensde
  vorm, samengesteld, literal, binder, constructor, concatenatie, het voorbeeld). Drools:
  `TextOfAValueTest` naar `rsTekst(waarde, vorm)`; `TmsBuiltinWitnessTest.a_text_composition_with_a_number_reads_its_decimal_with_a_comma`
  heet nu `…_reads_the_decimals_of_its_type` en verwacht `Bedrag: 5,50` waar hij `Bedrag: 5,5` verwachtte, plus een
  object met 5 (`Bedrag: 5,00`); in `FormsPlan` krijgt de `RS_TEXT`-term zijn vorm-constante. De DRL-pin van de golden
  is herschreven (`rsTekst($basis, Integer.valueOf(2))` enz.); geen andere pin bewoog.
- Ook bijgewerkt: `backends/drools/README.md` noemde het tekstdeel nog als vorm die de backend weigert.
- **Gates na stap 1** (los, op de werkboom vóór de commit, alle rc=0): 1, 2, 3, 4, 5 (ref-suite 2600 voorbeelden), 6,
  7, 8, 10, 12 (JUnit 1892), 13, 15, 16, 17 (`logs/s1-summary.log`, `logs/s1b-summary.log`, per gate
  `logs/s1*-check-*.log`). De eerste run van gate 12 was rood: `DrlRenderingWitnessTest` bouwde een `RS_TEXT`-term
  met één argument (`FormsPlan`), bijgewerkt. `make gates` volgt na stap 2.

### Stap 2: de eenheid op verzoek, tussen haakjes

*Besluit van de eigenaar (2026-11-06 avond), ontwerpkeuzes van de hoofdsessie; bronvorm `«X (eenheid)»` door de
eigenaar gekozen. Mandaat voor deze stap: IR-SPEC, het schema, de verifier, `scripts/check-*.sh`, RUNTIME-SEMANTICS
ch. 1.2, `corpus/negative/` en de golden.*

- **Draad.** `EmbeddedExpression` krijgt het optionele veld `unit`, alleen `true`, achteraan in de sleutelvolgorde
  (schema `$defs/EmbeddedExpression`, met een beschrijving); afwezig is het deel zonder eenheid. Additief: een
  document zonder het veld leest als voorheen. Gate 2 kreeg vier vorm-zelftestrijen (`UNIT_SHAPE_ROWS`: `true`
  aanvaard; `false`, `null` en een eenheidsspelling geweigerd).
- **IR-SPEC ch. 4.2.** Het veld bij `TextComposition`, en **WF-129**: een deel met `unit` heeft een type met een
  eenheid (een getal met `unit`, een percentage draagt `%`); het type is het statische type van `expr` (ch. 2.10), een
  deel zonder gedeclareerd type wacht. Twee armen op de pointer van het deel: `WF-129.number-without-unit` en
  `WF-129.not-a-number` (parameter `kind`). Rij in de WF-tabel.
- **Verifier.** `Embedded_expression` draagt `with_unit`; de loader leest `unit` (alleen `true`, anders een
  laadfout in de zin die de Drools-loader ook schrijft); de controle staat in `walk_expr`, dus overal waar een
  tekstsamenstelling staat. Negatieven `corpus/negative/wf129-number-without-unit` en `wf129-not-a-number`, elk met
  `expect.txt` en `NOTE.md` (frontend-uitvoer zonder suffix, met het veld toegevoegd).
- **RUNTIME-SEMANTICS ch. 1.2.** De regel: na het getal een spatie en tussen haakjes de eenheid waarin de waarde
  staat (van een read de declaratie, van een samengesteld deel de afgeleide eenheid, ch. 2.6 en 2.10), gespeld zoals
  IR-SPEC ch. 2.5 een eenheid spelt; leeg blijft `""` zonder eenheid. Rijen in de tabel en het voorbeeld van de golden.
- **Keuze: de afkorting.** De eenheid wordt geschreven met haar afkorting, de ene spelling die elk voorkomen van een
  eenheid op de draad draagt (IR-SPEC ch. 2.5; `Units.render` in OCaml en `Unit.rendering()` in Java spellen haar
  gelijk). De verdere spellingen van een declaratie (`spellings`) hebben geen rol die er één als symbool aanwijst. De
  golden declareert `de euro (mv: euros) EUR €`, dus `5,50 (EUR)`; een document dat € als afkorting declareert,
  schrijft `5,50 (€)`. Een percentage schrijft `(%)`, een tarief `(EUR/jr)`.
- **Frontend.** `(eenheid)` is één token met zijn haakjes (`EENHEID_AANWIJZING`), zoals `(bezield)` en `(mag
  opgegeven worden)`; `tekstReeksDeel` neemt het na de expressie van een «…»-escape. Geen expressieproductie neemt
  het token, dus `«zijn basis maal 1,1 (eenheid)»` vraagt de eenheid van het product; `eenheid` blijft een gewoon
  woord, dus een attribuut of parameter die eenheid heet blijft leesbaar (`«zijn eenheid (eenheid)»`). Gemeten: geen
  van de 369 andere RegelSpraak-bronnen van de repository schrijft `(eenheid)`. Het record `EmbeddedExpression` draagt
  `unit`, de emitter schrijft `"unit": true`, en een deel met de suffix waarvan het type geen eenheid heeft is
  RGS-388 (register en beide catalogi). DIVERGENCES: "De eenheid van een tekstdeel". Test `TextPartUnitTest` (drie
  gevallen).
- **Ref.** `Arith.text_form` draagt `with_unit`; de eenheid is die van de waarde zelf (`Value.Num (q, Unit u)`),
  gespeld door `Units.render`. Een deel met eenheid op een waarde zonder eenheid of op een niet-getal is een fout
  (WF-129 gebroken), nooit een stil weggelaten eenheid. Suite: zeven rijen erbij (2607 voorbeelden).
- **Drools.** De loader leest `unit` (`unitRequest`, sleutel in de tabel van het deel); de analyse geeft
  `rsTekst(waarde, vorm, eenheid)` de spelling van de afgeleide eenheid (`EffectiveUnit`, `Unit.rendering()`) als
  constante, null zonder eenheid; een deel dat een eenheid vraagt die het niet heeft, weigert de backend bij naam:
  **DROOLS-368** (`TEXT_PART_UNIT_WITHOUT_UNIT`, CONTRACT; register en catalogi). Tests: `TextOfAValueTest.a_part_with_its_unit`,
  `TmsBuiltinWitnessTest.a_part_with_the_unit_of_a_number_without_one_is_refused_by_name`; DRL-pin herschreven.
- **Clingo** weigert tekstsamenstelling al (CLINGO-042); alleen de patronen van de AST volgen het nieuwe veld.
- **Golden.** Regel `eenheidtekst`: `"Bedrag: «zijn basis (eenheid)», aandeel «zijn aandeel (eenheid)», toeslag «zijn
  basis maal 1,1 (eenheid)», zonder eenheid «zijn basis»"`, zes nieuwe verwachtingen (V1 `Bedrag: 5,50 (EUR), aandeel
  12,50 (%), toeslag 6,050 (EUR), zonder eenheid 5,50`; V2–V4 zo; V5/V6 leeg: `Bedrag: , aandeel , toeslag 0,000 (EUR),
  zonder eenheid `, de vermenigvuldiging coalesceert leeg naar 0 in de eenheid van de declaratie). Geen bestaande
  verwachting veranderde in stap 2.
- **Rood eerst.** De golden op de code van `55ecb7603`: ref `IR LOAD ERROR … unknown field 'unit'`, de jar
  `ERROR DocumentError … unknown field on EmbeddedExpression` (`logs/red2-*.log`); na de bouw ref `PASS (57)` en
  `(29)`, beide Drools-takken `PLAYED 2`.
- **Gates na stap 2** (los, vóór de commit): 1, 2 (534 vorm-zelftestrijen), 3 (636 negatieven, 265 armsleutels), 4,
  5, 10 (JUnit 1530; 377 RGS-codes), 11, 12 (`logs/s2-summary.log`). Gate 12 was eerst rood op dezelfde
  `FormsPlan`-term als in stap 1 (nu met drie argumenten); daarna `DrlRenderingWitnessTest` groen.

### Voor de hoofdsessie

- **CONSTRUCTS, rij 31** (voorstel, vervangt de zin van het voorlopige besluit): "Tekst van een waarde
  (TextComposition, concatenation): een getal in de decimalen van het type van zijn deel (de precisiegrens van
  RUNTIME-SEMANTICS ch. 2.3), zonder eindige grens exact en `t/n`; `waar`/`onwaar`, `dd-MM-jjjj`, `dd-MM-jjjj
  uu:mm:ss.mmm`, een opsomming haar label, een constructor `tag(payload)` met de payload in zijn gedeclareerde type;
  lijst/record een fout. Op verzoek de eenheid erachter, `EmbeddedExpression.unit` (v1-extensie, WF-129; bron
  `«X (eenheid)»`), gespeld met de afkorting. Besluit van de eigenaar 2026-11-06. ref en beide Drools-takken (DROOLS-368
  bij een deel zonder eenheid); clingo weigert als limiet CLINGO-042."
- **CLAUDE.md, gate-teksten** (voorstel): gate 2 — "Since the card `tekst-van-een-waarde-in-een-tekstsamenstelling`
  (2026-11-06) the shape self-test holds `EmbeddedExpression.unit` to its one spelling, `true` (four rows)." Gate 3 —
  "Since the same card WF-129 refuses a text part that writes the unit of its number where its type has none, with one
  negative per arm." Gate 10 — "RGS-388 refuses the same at check (`«X (eenheid)»`, DIVERGENCES 'De eenheid van een
  tekstdeel')." Gate 12 — "Since the same card the analysis passes `rsTekst` the form of a part (its decimals, ch. 1.2)
  and its unit, and DROOLS-368 refuses a part that asks for a unit it does not have." Geen vloer bewoog: alle
  tellingen groeiden.

### Open punten (normvragen)

1. Een literal-deel `«5,50»` schrijft `5,5`: het type van een literal is volgens ch. 2.3 de decimalen van zijn exacte
   waarde (de frontend annoteert de literal ook met 1 decimaal). ALEF schrijft het lexeem van de bron, `5,50`.
2. De eenheid wordt met haar afkorting geschreven (`5,50 (EUR)` bij de declaratie `EUR €`), waar het voorbeeld van de
   eigenaar `(€)` schrijft. De draad kent geen rol "symbool" voor een spelling van een eenheid; een symbool schrijven
   vraagt een veld op de eenheidsdeclaratie (IR-SPEC ch. 2.5).
3. Een binder van een gesloten body (een functieparameter, alleen wire-first) draagt de grens van zijn argument
   (ch. 2.3), niet het gedeclareerde type van de parameter: een parameter van 2 decimalen met een geheel argument
   schrijft `5`, niet `5,00`.

### Eindrun

`make gates` op `f33ffbf3c`: alle zeventien gates en `after-run` rc=0, red=0, 435 s
(`out/gates/20261106-202429/summary.txt` in de worktree, log `uchar-dev/werk/text-decimals/logs/gates-final.log`).
