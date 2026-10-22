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

## Result (2026-10-22)

Tak `card/percentage-of-snippets`, op main `4970cceb`. Geraakt:
`frontends/regelspraak/.../resolver/ExpressionBuilder.java`,
`frontends/regelspraak/src/test/.../PercentageReadTest.java`, `corpus/golden/jutterswet-havengeld/ir.json` en deze
kaart. Niet geraakt: `toka/` (gate 11 vroeg niets), `spec/`, de gate-scripts, de andere goldens. Geen bestaande test
aangepast.

- `aa33a78b` de kaart naar `doing/`.
- `36e50526` elke operand haar eigen brontekst; test `k_each_operand_carries_its_own_source`; de golden opnieuw
  uitgegeven.
- `a0a3875e` een bestaande fout: een verworpen lezing liet haar diagnose achter (hieronder); test
  `l_a_rejected_reading_reports_nothing_of_its_own`.

**De telling** (grep op `"percentageOf"`, main `4970cceb`). Negen goldens dragen een `percentageOf`. Vijf hebben een
`source.rgs` en worden door de frontend uitgegeven: `jutterswet-afrekening`, `jutterswet-bergloon`,
`jutterswet-havengeld`, `jutterswet-vroegste-vondst`, `synth-navigatiepremie` (de eerste, derde en vierde ook met een
`scenario.rgs`). Vier hebben er geen en blijven buiten de frontend: `anchor/jutterswet-vindersloon`,
`division-by-numeric-zero`, `jutterswet-kinderprocent`, `synth2-afrondingscombinaties`; de anchor-zone is dus niet
geraakt. Verder één negatieve case (`wf092-lambda-parameter-unit`, zonder bron). TOKA: de dump en de 29 slices dragen
geen `percentageOf`. Operanden met de snippet van hun ouder: drie, in twee knopen, allebei in `jutterswet-havengeld` (de
regels havengeld en toeslag, de vorm zonder '%'). De vier andere rgs-goldens schrijven de '%'-vorm en de regel kadegeld
de haakjesvorm; daar had elke operand al haar eigen tekst. Van de 133 rgs-goldens, na de fix alle opnieuw uitgegeven,
geeft alleen `jutterswet-havengeld` een ander IR.

**De regel.** P krijgt de tekst van het eerste naamdeel ("percentage havengeld"), zonder lidwoord, omdat het lidwoord
buiten het naamdeel staat; dat is dezelfde tekst die de haakjesvorm P al gaf. V krijgt de tekst van het tweede naamdeel
tot het eind van het onderwerp ("het taxatiebedrag van de taxatie van de Vondst"). Met één naamdeel had V al de tekst
van het onderwerp (`percentageBase`). De `Binary` houdt de hele zin. Keuze van de bouwer: een '%' na P valt buiten de
snippet van P ("het percentage havengeld % van …" geeft "percentage havengeld"). Het teken hoort bij de functie, zoals
in de '%'-vorm, waar de linkeroperand het ook niet draagt; `percentageParameter` laat het al buiten de naam.

**Rood naar groen.** `k_each_operand_carries_its_own_source` pint de snippets van P, V en de functie voor vijf zinnen
(navigatie, '%' na P, `zijn`-lid, eigen lid met onderwerp, haakjesvorm) en voor V uit "Daarbij geldt". Op de code van
`4970cceb` faalden vijf van de zes; de haakjesvorm was al goed (`werk/bruggen/percentage/red-0.log`). Na de fix groen.

**Diff-toets.** `werk/bruggen/percentage/snippet-diff.py 4970cceb0` vergelijkt elk veranderd JSON-bestand met main na
het weglaten van elk `snippet`-veld: één bestand veranderd, zonder snippets gelijk. De drie snippets:
`/rules/rules/0/versions/0/value/base/left` "het percentage havengeld van het taxatiebedrag van de taxatie van de
Vondst" → "percentage havengeld"; `…/base/right` dezelfde zin → "het taxatiebedrag van de taxatie van de Vondst";
`/rules/rules/2/versions/0/value/body/base/left` "het percentage toeslag van het meerbedrag" → "percentage toeslag".

**Bestaande fout, gefixt.** Valt de herlezing als percentagefunctie af (V is geen getal of niet te bouwen), dan bouwt
`buildAttribute` de zin opnieuw als navigatie over hetzelfde onderwerp. Wat de poging om V te bouwen meldde, bleef
staan: "het percentage havengeld van het taxatiebedrag van de taxatie van de als tweede genoemde Vondst" gaf twee keer
RGS-089 "rangtelwoord: de regel noemt geen 2 onderwerpen 'Vondst' (§5.5.3)"; een build van main van vanochtend geeft
hetzelfde. Afgeleid, niet gemeten: met de nieuwe snippets zou een RGS-090 (meer bindingen) twee verschillende zinnen
citeren, eerst die van V. Nu vervallen de diagnoses van een verworpen lezing; de navigatie meldt haar eigen. De test
`l_…` was rood met de twee meldingen en is groen.

**Lezers van de snippet.** `SemanticTokens` leest geen snippet van een `Expr`; het roept `buildAttribute` alleen aan om
de houder van een verdeling te vinden. In de frontend schrijft `IrEmitter` de snippet en citeren foutmeldingen haar;
`ScenarioResolver` leest de regelnummers van knopen, en die blijven binnen de regel. De Drools-backend zet de snippet
als commentaar boven de DRL-tekst (`Term.source`) en in weigeringen; geen test pint de DRL-tekst van
`jutterswet-havengeld`, en beide takken lezen hetzelfde IR. Verifier, ref en clingo rekenen niet met de snippet.

**Gates.** Tijdens de bouw: `make -C frontends/regelspraak test` 1278 tests groen; gate 10 PASS (1278 tests, 133
rgs-goldens bytegelijk), gate 2 PASS (364 bestanden), gate 16 PASS (35 scenario-goldens, 123 casebestanden), gate 11
PASS (29 TOKA-cases in ref, clingo en Drools). De volledige set (`werk/gates.sh`, `gates-1`, op `a0a3875e`): `EXIT 0`,
alle zeventien groen; gate 12 speelt 271 goldens op beide takken (`EXPECTED_PLAYED` ongewijzigd), gate 17 vergelijkt 271
goldens en 743 casebestanden.

### Landing (hoofdsessie, 2026-10-22)

`gates-1` op `a0a3875e` (de Result-commit erna raakt alleen deze kaart): alle zeventien groen, EXIT 0. Review: de
bijvangst (een verworpen lezing laat haar diagnose niet meer achter, RGS-089 niet meer dubbel) is een bug met getuige
en gaat mee. Geen CONSTRUCTS-rij: alleen de snippets veranderen, niet de wire-vorm.
