# drools-exec-kwantorvoorwaarde-eigen-operand: een berekening met een kwantorvoorwaarde herziet niet wanneer een eigen operand later wordt afgeleid

*Vondst van een regelsetbouwer, 2026-09-30 (V1 in zijn clusterdocument),
bevestigd door de hoofdsessie op uchar `de809cf7` via de distributie
(`bin/regelspraak scenario`) en de eigen uitvoerders; reproductie op
Jutterswet-materiaal in `/tmp/v2/probe/v1/` (bron.rgs, bron-na.rgs,
bron-plus.rgs, scenario.rgs, scenario-plus.rgs, cases/, cases-plus/; alleen
lezen). Een stil fout antwoord: `check` exit 0, ref en clingo halen de
verwachting, de exec-tak niet.*

## Aanleiding

Een gewone berekening waarvan de voorwaarde een kwantor over een rol is
("indien de bergdag van alle vondsten van de Berging gevuld is") en waarvan
de waarde eigen attributen leest die een andere regel later afleidt, rekent
in de exec-tak één keer: zodra de kwantor waar wordt, met de operand nog
leeg. Wordt de operand daarna geschreven, dan herziet de regel niet. Bij
`maximale waarde van A en B` blijft de uitkomst leeg; bij `A plus 1` rekent
de lege operand als 0 en staat er een getal dat nergens op slaat. Staat de
schrijver van de operand in de bron vóór de berekening, of noemt de
voorwaarde de operand zelf ("zijn laatste bergdag is gevuld"), dan klopt de
uitkomst. Zonder rol in de voorwaarde (een gevuld-toets op een eigen
attribuut) is dezelfde berekening wél reactief op haar operanden (gemeten,
R5). Dezelfde familie als drools-exec-voorwaarde-genavigeerde-afleiding en
drools-exec-vouwfilter-latere-afleiding: een regel die niet opnieuw loopt
wanneer een gelezen waarde later verandert, nu bij de operanden van de
waarde onder een kwantorvoorwaarde. `backends/drools/README.md` ("Recorded:
a condition that reads through a role") noemt de volgordegrens van de
leeg-toets in een voorwaarde; deze vorm valt erbuiten: niet de voorwaarde
loopt te vroeg, de waarde wordt niet opnieuw berekend. De regelset heeft
het omzeild door de operanden in de voorwaarde te noemen (de berekening
"geldt alleen als een van beide operanden gevuld is"), een voorwaarde die de
wet niet stelt.

## Minimale reproductie

`de809cf7`, distributie `bin/regelspraak check|scenario|cases`; ref en
clingo via `_build/default/ref/bin/runner.exe` en
`_build/default/backends/clingo/bin/clingo_runner.exe` op `cases/ir.json`
en het casebestand.

    Objecttype de Berging (mv: Bergingen)
        de laatste bergdag Numeriek (geheel getal);
        de gemelde dag Numeriek (geheel getal);
        de sluitdag Numeriek (geheel getal);
    Objecttype de Vondst (mv: Vondsten)
        de vinddag Numeriek (geheel getal);
        de bergdag Numeriek (geheel getal);
    Feittype BergingBetreftVondst
        de berging Berging
        de vondst (mv: vondsten) Vondst
    Eén berging betreft meerdere vondsten

    Regel sluitdag
        geldig altijd
            De sluitdag van een Berging moet berekend worden als de maximale waarde van zijn laatste bergdag en zijn gemelde dag
            indien de bergdag van alle vondsten van de Berging gevuld is.

    Regel bergdag
        geldig altijd
            De bergdag van een Vondst moet berekend worden als zijn vinddag plus 1
            indien zijn vinddag gevuld is.

    Regel laatste bergdag
        geldig altijd
            De laatste bergdag van een Berging moet berekend worden als de maximale waarde van de bergdag van alle vondsten van de Berging.

    Scenario: een Berging B met één Vondst V (vinddag 30, berging B); verwacht: de laatste
    bergdag van B is 31, de sluitdag van B is 31.

    R1  bron.rgs zoals hierboven.
        nu: check exit 0; scenario FAIL "verwachting 2 FOUT: attributeValue B sluitdag
        (verwacht=31, werkelijk=leeg)", trace: sluitdag, bergdag en laatste bergdag GEVUURD;
        ref `oracle: PASS (2 expectations)`; clingo `TRANSLATION FAULT: expression kind not
        translated (no corpus case exercises it)` (het maximum van twee scalaire operanden; los
        van deze kaart, niet bouwen).
    R2  dezelfde bron met `Regel sluitdag` achteraan (bron-na.rgs).
        nu: PASS.
    R3  de waarde `zijn laatste bergdag plus 1`, verwacht 32 (bron-plus.rgs, scenario-plus.rgs).
        nu: exec FAIL "(verwacht=32, werkelijk=1)": de lege operand rekent als 0; ref PASS,
        clingo `clingo: PASS (2 expectations)`.
    R4  R1 met de operand in de voorwaarde ("hij voldoet aan alle volgende voorwaarden: de
        bergdag van alle vondsten van de Berging is gevuld; hij voldoet aan ten minste één van de
        volgende voorwaarden: zijn laatste bergdag is gevuld, zijn gemelde dag is gevuld").
        nu: PASS (meting van de regelsetbouwer, /tmp/v2/probe/g2-g10/v1-minimaal/).
    R5  zonder rol: `indien zijn invoer gevuld is` met een later afgeleide operand.
        nu: PASS (idem, model.rgs en model2.rgs): de reactiviteit op de operanden bestaat, alleen
        niet onder een kwantorvoorwaarde.

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: de
  kwantor "alle", "ten minste één" en "geen" over een rol als voorwaarde
  (ook met een tweede, eigen voorwaarde ernaast); de operanden van de waarde
  eigen, één rol ver en als vouw; de schrijver van de operand als gewone
  regel vóór en na de berekening, en als defaultsgroep; de waarde als
  maximum of minimum van twee operanden, als som, en als één operand; ook
  `moet gesteld worden op`.
- (b) De exec-tak herziet een berekening met een kwantorvoorwaarde wanneer
  een operand van haar waarde later wordt afgeleid, in elke bronvolgorde,
  met dezelfde uitkomst als ref (en clingo waar clingo de vorm vertaalt), of
  weigert de vorm benoemd waar dat niet lukt; nooit een lege operand als 0;
  rood-eerst JUnit-getuige met deze reproductie.
- (c) Golden (werknaam `jutterswet-sluitdag`), handgerekend, in ref ≡ clingo
  ≡ Drools-exec (de plus-vorm van R3, zodat clingo meespeelt); `EXPECTED_PLAYED`
  (224) en `EXPECTED_DRL_PLAYED` (190) in `scripts/check-drools.sh` als
  **voorstel**; de DRL-teksttak volgens haar kanaal.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). `backends/drools/README.md`: een
  eigen alinea onder "Recorded" over de kwantorvoorwaarde en de reactiviteit
  op de operanden van de waarde, met wat na de kaart nog niet speelt.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: geen andere kaart in bouw;
werk op de kop van main (`de809cf7`).

## Result (2026-09-30, bouwer)

Tak `card/drools-exec-kwantorvoorwaarde-eigen-operand` in de worktree van de kaart, gebouwd op `b2325d34` (main
`de809cf7` met de kaartcommit); niet gerebased, niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van
deze repo: `_build/default/ref/bin/runner.exe`, `_build/default/backends/clingo/bin/clingo_runner.exe`,
`_build/default/spec/verifier/bin/verifier.exe`, de frontend (`Resolver --ir … --cases … bron.rgs scenario.rgs`) en
de Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de teksttak). "Vóór" is de jar van de claim (backend
gelijk aan main), "na" de jar met de fix (`97b0e912`). Elke vorm is een eigen Jutterswet-document (bron.rgs +
scenario.rgs), door een generator geschreven, door de frontend naar ir.json en cases verlaagd en door een meetscript
gemeten; beide staan naast de reproducties van de kaart, buiten de repo.

### Oorzaak

`ExecModelTranspiler.regel` zet de toetsen van een voorwaarde op het onderwerppatroon en regelt daarna de versheid
(RUNTIME-SEMANTICS ch. 5.1): een regel met een voorwaarde gold als klassereactief (`klasseReactief =
r.voorwaarde() != null …`), omdat een ondoorzichtige toets op het patroon het op elke bijwerking van het onderwerp
laat reageren; alleen een regel zonder voorwaarde kreeg, als zijn waarde eigen attributen leest, de ondoorzichtige
ware expr (`fresh`). Vier vormen van voorwaarde zetten echter geen toets op het onderwerppatroon: een kwantorvouw
(`kwantorvouwVoorwaarde`: een collectList-accumulate over de elementen en een toets op de verzamelde lijst), een
aggregatievergelijking (`aggregatieVoorwaarde`: een accumulate en een toets op haar resultaat), een rolcheck waarvan
de koppeling aan de overkant staat en een regelversiecheck (elk een `exists`); een ALLE-samenstelling van zulke leden
evenmin. Het onderwerppatroon bleef dan zonder toets, en zo'n patroon reageert op geen bijwerking van het onderwerp
(de code zegt het bij de versheid: "a pattern WITHOUT constraints has an EMPTY reactivity mask"). De regel vuurde één
keer, zodra de accumulate of de exists waar werd, en las een eigen operand die een later gedeclareerde regel of een
defaultsgroep nog moest schrijven als leeg: het maximum van leeg en leeg bleef leeg, en `leeg plus 1` werd 1, omdat
plus een lege operand als 0 neemt (ch. 1.2). Stond de schrijver in de bron vóór de regel, dan had hij al geschreven
toen de voorwaarde waar werd (R2); noemde de voorwaarde de operand zelf (R4), of stond er een eigen toets naast, dan
stond er een toets op het onderwerp. Hetzelfde gold voor een eigen operand die de voorwaarde in haar accumulate
vergelijkt: met de drempel nog leeg is de vergelijking onwaar, en zij werd niet opnieuw gedaan. Een kwantorvouw over
een kenmerk speelde meestal toch, door de stratificatie: `Stratificatie` leest de matrix van een vouw als ontkende lezing
en zet de regel onder band 0, zodat de schrijvers in band 0 eerst vuurden; alleen een schrijver in dezelfde band die
na de regel vuurde, werd gemist (b-kenmerk-band-na).

### Fix (`97b0e912`) en de gekozen vorm

In `regel()`, direct na de voorwaarde: laat de voorwaarde het onderwerppatroon zonder toets (`zonderToets(pattern)`:
de items van `PatternDSL.PatternDefImpl` zijn leeg), dan krijgt het de ondoorzichtige ware expr, het idioom dat
`regel` al gebruikte voor een regel zonder voorwaarde en `regelVoorwaarde` voor een voorwaarde over een rol. Het
patroon wordt klassereactief: elke bijwerking van het onderwerp evalueert het opnieuw, de accumulate of exists erna
volgt, en de regel rekent opnieuw met de operanden van dat moment; de idempotente `werkBij` schrijft alleen een
verandering en NO_LOOP houdt de eigen schrijving buiten. `zonderToets` gooit een `IllegalStateException` als een
patroon niet uit `PatternDSL.pattern` komt, in plaats van stil onwaar te geven. `ExecModelTranspiler.java` +16
regels, commentaar inbegrepen; `Stratificatie`, de DRL-tak, de loader en de beangeneratie zijn niet aangeraakt.

Drie vormen overwogen:
1. de expr alleen als de waarde eigen attributen leest (`eigenReads`), zoals bij een regel zonder voorwaarde. Dekt de
   operanden van de waarde, niet een eigen drempel die de voorwaarde in haar accumulate vergelijkt
   (e-aggregatie-eigen-drempel-constant-na: de waarde is de constante 21 en leest niets);
2. de expr op elk patroon met een voorwaarde, ook als er al een toets op staat: gedragsgelijk, maar verandert het
   netwerk van elke regel met een voorwaarde;
3. gekozen: de expr alleen waar de voorwaarde het patroon zonder toets liet. Zo klopt de aanname die de versheid al
   maakte (een voorwaarde maakt het patroon klassereactief); het dekt de operanden van waarde en voorwaarde, en raakt
   alleen regels met zo'n voorwaarde (in het corpus 53 regels in 17 goldens, zie (d)).

Een volgorde (een salience onder de schrijvers van de operanden) is niet gekozen: zij herziet een regel niet als een
operand na het vuren verandert, dekt een defaultsgroep als schrijver niet vanzelf (een groep komt na de regels in het
model), en verschuift de salience van bestaande regels. De kaart vraagt dat de berekening herziet; dat is de
versheid van ch. 5.1.

Over "nooit een lege operand als 0": na de fix speelt elke gemeten vorm waarin een operand van de waarde later wordt
afgeleid terwijl de voorwaarde waar is, in beide bronvolgordes, met de waarde van ref; de tijdelijke 0 is uit de
eindtoestand verdwenen (ch. 5.2: "the transient 5 is unobservable in the end state"). Een operand die in de
eindtoestand leeg is, neemt ch. 1.2 als 0: case 3 van de golden (geen vondsten, laatste bergdag leeg, sluitdag leeg
plus 1 = 1) beantwoorden ref, clingo en beide Drools-takken gelijk. Eén gemeten vorm eindigt nog met een lege operand
als 0, b-flank-geen-onwaar-na, waar niet de waarde maar de voorwaarde op een tijdelijk lege waarde waar is (open punt
1).

### (a) Vormruimte, vóór en na

Basis: `Berging` (laatste bergdag, gemelde dag, openingsdag, sluitdag), `Vondst` (vinddag, bergdag), één berging
betreft meerdere vondsten; `bergdag` = vinddag plus 1 indien vinddag gevuld, `laatste bergdag` = maximum van de
bergdag van alle vondsten. De berekening is `sluitdag`. "-na": de berekening staat in de bron vóór de schrijver van
haar operand; "-voor": erna. a-*: de waarde, onder "de bergdag van alle vondsten van de Berging gevuld is"; b-*: de
kwantor; c-*: de plaats van de operand (één rol ver via `de strandvoogd` van feittype StrandvoogdBeheertBerging, of
als vouw); d-*: de schrijver; e-*: de andere voorwaarden die het patroon zonder toets laten. Alle 54 documenten zijn
verifier-schoon (`verifier: PASS`). "idem" in de kolom "exec na": gelijk aan vóór.

| vorm | omschrijving | ref | clingo | exec vóór | exec na | DRL (vóór = na) |
|---|---|---|---|---|---|---|
| a-een-na | één eigen operand, berekend | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-a-een-na.json: attribuut sluitdag: verwacht 31, werkelijk leeg | PLAYED 1 | PLAYED 1 |
| a-een-voor | één eigen operand, berekend | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| a-gesteld-na | één eigen operand, gesteld | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-a-gesteld-na.json: attribuut sluitdag: verwacht 31, werkelijk leeg | PLAYED 1 | PLAYED 1 |
| a-gesteld-voor | één eigen operand, gesteld | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| a-max-gemeld-na | maximum van twee eigen operanden, gemelde dag 20 | oracle: PASS (2 expectations) | TRANSLATION FAULT: expression kind not translated (no corpus case exercises it) | FAIL case1-a-max-gemeld-na.json: attribuut sluitdag: verwacht 31, werkelijk 20 | PLAYED 1 | PLAYED 1 |
| a-max-gemeld-voor | maximum van twee eigen operanden, gemelde dag 20 | oracle: PASS (2 expectations) | TRANSLATION FAULT: expression kind not translated (no corpus case exercises it) | PLAYED 1 | idem | PLAYED 1 |
| a-max-na | maximum van twee eigen operanden, gemelde dag leeg (R1/R2) | oracle: PASS (2 expectations) | TRANSLATION FAULT: expression kind not translated (no corpus case exercises it) | FAIL case1-a-max-na.json: attribuut sluitdag: verwacht 31, werkelijk leeg | PLAYED 1 | PLAYED 1 |
| a-max-voor | maximum van twee eigen operanden, gemelde dag leeg (R1/R2) | oracle: PASS (2 expectations) | TRANSLATION FAULT: expression kind not translated (no corpus case exercises it) | PLAYED 1 | idem | PLAYED 1 |
| a-min-na | minimum van twee eigen operanden, gemelde dag 40 | oracle: PASS (2 expectations) | TRANSLATION FAULT: expression kind not translated (no corpus case exercises it) | FAIL case1-a-min-na.json: attribuut sluitdag: verwacht 31, werkelijk 40 | PLAYED 1 | PLAYED 1 |
| a-min-voor | minimum van twee eigen operanden, gemelde dag 40 | oracle: PASS (2 expectations) | TRANSLATION FAULT: expression kind not translated (no corpus case exercises it) | PLAYED 1 | idem | PLAYED 1 |
| a-plus-na | eigen operand plus 1 (R3) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-a-plus-na.json: attribuut sluitdag: verwacht 32, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| a-plus-voor | eigen operand plus 1 (R3) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| a-som-na | som van twee eigen operanden, gemelde dag 5 | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-a-som-na.json: attribuut sluitdag: verwacht 36, werkelijk 5 | PLAYED 1 | PLAYED 1 |
| a-som-voor | som van twee eigen operanden, gemelde dag 5 | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-alle-eigen-na | alle met een tweede, eigen voorwaarde (openingsdag gevuld) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-alle-eigen-voor | alle met een tweede, eigen voorwaarde (openingsdag gevuld) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-alle-invoer-na | alle, matrix op de invoer (waar bij invoegen) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-b-alle-invoer-na.json: attribuut sluitdag: verwacht 32, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| b-alle-invoer-voor | alle, matrix op de invoer (waar bij invoegen) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-flank-geen-onwaar-na | flank: geen wordt onwaar zodra de bergdag er is; ref en clingo beslissen | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-b-flank-geen-onwaar-na.json: verwacht leeg, werkelijk 1 | idem | FAIL case1-b-flank-geen-onwaar-na.json: verwacht leeg, werkelijk 1 |
| b-flank-geen-onwaar-voor | idem, de berekening na haar schrijvers in de bron | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-flank-onwaar-na | flank: de kwantor blijft onwaar (V2 zonder vinddag), de sluitdag blijft leeg | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-geen-invoer-na | geen, leeg-toets op de invoer (waar bij invoegen) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-b-geen-invoer-na.json: attribuut sluitdag: verwacht 32, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| b-geen-invoer-voor | geen, leeg-toets op de invoer (waar bij invoegen) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-geen-na | geen, leeg-toets op de afgeleide bergdag | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-b-geen-na.json: attribuut sluitdag: verwacht 32, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| b-geen-voor | geen, leeg-toets op de afgeleide bergdag | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-kenmerk-band-na | ten minste één over een ingevoerd kenmerk; de schrijver van de operand in dezelfde band (ook zo'n kwantor), erna | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-b-kenmerk-band-na.json: attribuut sluitdag: verwacht 37, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| b-kenmerk-invoer-na | ten minste één, matrix een kenmerk dat de invoer al bij invoegen geeft | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-kenmerk-invoerkenmerk-na | ten minste één over een kenmerk dat de case invoert (geen producent, geen stratum) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-kenmerk-keten-na | ten minste één over een kenmerk op de invoer; de operand aan het eind van een keten | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-kenmerk-na | ten minste één, matrix een afgeleid kenmerk van de vondst | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| b-r4-na | de omweg van de regelsetbouwer: de operanden in de voorwaarde (R4) | oracle: PASS (2 expectations) | TRANSLATION FAULT: expression kind not translated (no corpus case exercises it) | PLAYED 1 | idem | PLAYED 1 |
| b-tenminste-na | ten minste één, matrix op de afgeleide bergdag | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-b-tenminste-na.json: attribuut sluitdag: verwacht 32, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| b-tenminste-samengesteld-na | ten minste één van: een kwantorvouw, een eigen vergelijking | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | EXCLUDE transpiler SKIP: regel: sluitdag — regel (voorwaarde buiten Phase-B scope): sluitdag | idem | EXCLUDE drl-transpiler MARKER: regel: sluitdag — TODO kwantorvouw (ALLE over Vondst) onder een niet-conjunctieve kwantor: haar clausule is een eigen when-clausule en geen patroonconstraint, dus zij kan niet in een \|\|/!-vouw staan: hij aan ten minste één van de volgende voorwaarden voldoet: - de bergdag van alle vondsten van de Berging is gevuld - zijn openingsdag is groter dan 100. |
| b-tenminste-voor | ten minste één, matrix op de afgeleide bergdag | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| c-gemengd-na | maximum van een afgeleide eigen operand en een tweede die een gewone regel schrijft | oracle: PASS (3 expectations) | TRANSLATION FAULT: expression kind not translated (no corpus case exercises it) | FAIL case1-c-gemengd-na.json: attribuut sluitdag: verwacht 45, werkelijk leeg | PLAYED 1 | PLAYED 1 |
| c-nav-invoer-na | operand één rol ver, kwantor op de invoer (waar bij invoegen) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | FAIL case1-c-nav-invoer-na.json: attribuut sluitdag: verwacht 53, werkelijk 1 |
| c-nav-na | operand één rol ver (afsluitdag van de strandvoogd), afgeleid | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| c-nav-voor | operand één rol ver (afsluitdag van de strandvoogd), afgeleid | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| c-nav-zonder-kwantor-na | controle: operand één rol ver onder een eigen gevuld-toets op de invoer, zonder kwantor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| c-som-vouw-na | operand als som-vouw (31 + 21) | oracle: PASS (1 expectations) | clingo: PASS (1 expectations) | PLAYED 1 | idem | PLAYED 1 |
| c-vouw-na | operand als vouw (maximum van de bergdag van alle vondsten) | oracle: PASS (1 expectations) | clingo: PASS (1 expectations) | PLAYED 1 | idem | PLAYED 1 |
| c-vouw-plus-na | vouw in een rekensom (1 plus het maximum) | oracle: PASS (1 expectations) | clingo: PASS (1 expectations) | PLAYED 1 | idem | PLAYED 1 |
| c-vouw-voor | operand als vouw (maximum van de bergdag van alle vondsten) | oracle: PASS (1 expectations) | clingo: PASS (1 expectations) | PLAYED 1 | idem | PLAYED 1 |
| d-groep-afgeleid-na | schrijver een defaultsgroep die de afgeleide laatste bergdag leest (31 > 30: 41) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-d-groep-afgeleid-na.json: attribuut sluitdag: verwacht 42, werkelijk 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| d-groep-afgeleid-voor | idem, de groep eerst in de bron | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-d-groep-afgeleid-voor.json: attribuut sluitdag: verwacht 42, werkelijk 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| d-groep-invoer-na | schrijver een defaultsgroep op de invoer (25 > 20: 35) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-d-groep-invoer-na.json: attribuut sluitdag: verwacht 36, werkelijk 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| d-groep-invoer-voor | idem, de groep eerst in de bron | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-d-groep-invoer-voor.json: attribuut sluitdag: verwacht 36, werkelijk 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| d-regel-na | schrijver een gewone regel na de berekening, kwantor op de invoer | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-d-regel-na.json: attribuut sluitdag: verwacht 31, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| d-regel-voor | schrijver een gewone regel vóór de berekening, kwantor op de invoer | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |
| e-aggregatie-eigen-drempel-constant-na | aggregatievergelijking met een eigen, later afgeleide drempel en een constante waarde (21) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-e-aggregatie-eigen-drempel-constant-na.json: attribuut sluitdag: verwacht 21, werkelijk leeg | PLAYED 1 | FAIL case1-e-aggregatie-eigen-drempel-constant-na.json: attribuut sluitdag: verwacht 21, werkelijk leeg |
| e-aggregatie-eigen-drempel-na | aggregatievergelijking met een eigen, later afgeleide drempel (max bergdag 31 > gemelde dag 25) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-e-aggregatie-eigen-drempel-na.json: attribuut sluitdag: verwacht 21, werkelijk leeg | PLAYED 1 | FAIL case1-e-aggregatie-eigen-drempel-na.json: attribuut sluitdag: verwacht 21, werkelijk leeg |
| e-aggregatie-na | voorwaarde een aggregatievergelijking (aantal vondsten groter dan 0) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-e-aggregatie-na.json: attribuut sluitdag: verwacht 32, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| e-rolcheck-na | voorwaarde een rolcheck waarvan de koppeling aan de overkant staat (hij is een berging) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-e-rolcheck-na.json: attribuut sluitdag: verwacht 32, werkelijk 1 | PLAYED 1 | PLAYED 1 |
| r5-na | controle zonder rol: eigen gevuld-toets op de invoer (R5) | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | PLAYED 1 |

Samengevat, 54 vormen: ref `oracle: PASS` op alle 54; clingo `clingo: PASS` op 46 en `TRANSLATION FAULT: expression
kind not translated (no corpus case exercises it)` op de 8 met het maximum of minimum van twee scalaire operanden
(los van de kaart). Exectak vóór: 30 PLAYED, 23 FAIL, 1 EXCLUDE; na: 52 PLAYED, 1 FAIL (b-flank-geen-onwaar-na, open
punt 1), 1 EXCLUDE (b-tenminste-samengesteld-na, open punt 2). DRL-teksttak vóór en na gelijk: 45 PLAYED, 4 FAIL, 5
EXCLUDE. Waar een "-voor"-tweeling gemeten is, speelde die al vóór de fix (de schrijver eerst in de bron), behalve bij
de defaultsgroep (d-groep-*: fout in beide bronvolgordes, want een groep komt na de regels in het model); de
controles R4 (b-r4-na) en R5 (r5-na) speelden vóór en na. De kenmerkvormen b-kenmerk-na, -invoer-na,
-invoerkenmerk-na en -keten-na speelden vóór door de stratificatie (`Stratificatie.verzamelReads` leest de matrix van
een vouw met negatie, dus zakt de regel naar salience -1 of lager, ook zonder producent van het kenmerk);
b-kenmerk-band-na, met de schrijver in die band en na de regel, niet. Een meetnoot: de frontend weigert "de maximale
waarde van de bergdag van alle vondsten van de Berging plus 1" (`setlezing buiten een vouwbron 'de bergdag van alle
vondsten van de Berging': hop 0 reaches the to-many role 'vondst' in a scalar slot — a to-many step needs an
aggregation, never a silent first pick`); c-vouw-plus-na schrijft daarom "1 plus de maximale waarde …".

### (b) Getuigen

`KwantorvoorwaardeOperandWitnessTest` (exectak, negen rijen, documenten in de klasse op Jutterswet-materiaal): R1, R3,
ten minste één en geen, de defaultsgroep als schrijver, de kenmerkvouw met de schrijver in dezelfde band, de
aggregatievergelijking met een later afgeleide eigen drempel, de rolcheck aan de overkant, en twee flanken (R2, en een
vouw die onwaar blijft). De documenten zijn apart nagemeten: alle negen `verifier: PASS` en `oracle: PASS`, clingo
`PASS` behalve de twee rijen met het maximum van twee operanden.

Rood op de claim (`77042037`):

    Tests run: 9, Failures: 7, Errors: 0, Skipped: 0
    de_berekening_rekent_opnieuw_zodra_haar_operand_er_is:44 … <case: attribuut sluitdag: verwacht 31, werkelijk leeg>
    een_defaultsgroep_schrijft_de_operand:95 … <case: attribuut sluitdag: verwacht 36, werkelijk 1>
    een_kwantor_over_een_kenmerk_met_de_schrijver_in_dezelfde_band:112 … <case: attribuut sluitdag: verwacht 37, werkelijk 1>
    een_lege_operand_telt_niet_als_nul:55 … <case: attribuut sluitdag: verwacht 32, werkelijk 1>
    een_rolcheck_aan_de_overkant:138 … <case: attribuut sluitdag: verwacht 32, werkelijk 1>
    een_voorwaarde_die_haar_eigen_drempel_later_krijgt:125 … <case: attribuut sluitdag: verwacht 21, werkelijk leeg>
    ten_minste_een_en_geen_herzien_ook:68 ten minste één ==> … <case: attribuut sluitdag: verwacht 32, werkelijk 1>

Het tweede deel van de laatste rij (geen) liep daar niet; apart gemeten, met het eerste deel tijdelijk weg op de
transpiler van de claim: `ten_minste_een_en_geen_herzien_ook:72 geen ==> expected: <null> but was: <case: attribuut
sluitdag: verwacht 32, werkelijk 1>`. De twee flanken zijn groen vóór en na. Na de fix: de klasse 9 tests, 0
failures; de suite 906 tests, 0 failures, 0 skipped (897 op de claim; vloer 239).

### (c) Golden `jutterswet-sluitdag` (`e4ee8b65`)

Drie regels op de Berging met een kwantorvouw over haar vondsten als voorwaarde: `sluitdag` = zijn laatste bergdag
plus 1 indien de bergdag van alle vondsten gevuld is (de plusvorm van R3), `afrekendag` gesteld op zijn uiterste
meldingsdag indien de vinddag van geen van de vondsten leeg is, `controledag` = zijn laatste bergdag plus zijn
wachttijd indien de bergdag van ten minste één van de vondsten gevuld is. De operanden komen van later gedeclareerde
regels: `laatste bergdag` (de vouw), `uiterste meldingsdag` (zijn laatste bergdag plus 5, twee stappen van de
bergdag), `wachttijd` (zijn openingsdag min 10). Geen defaultsgroep en geen maximum van twee operanden, zodat clingo
en de DRL-tak meespelen; die vormen staan in de getuige. Handgerekend in NOTE.md; toegelaten via `scripts/admit.sh`
met het gate-12-voorstel in de werkboom: `admit: ADMITTED corpus/golden/jutterswet-sluitdag (3 case(s);
verifier-clean, ref = clingo, no-op refuted, gates green)`. De IR uit `--ir bron.rgs` alleen is byte-gelijk aan die met
het scenario.

| case | verwachtingen | ref | clingo | exec vóór | exec na | DRL-tekst | no-op |
|---|---|---|---|---|---|---|---|
| case1-twee-gevonden-vondsten | 8 | PASS | PASS | FAIL sluitdag 1, afrekendag leeg, controledag 0 | PASS | PASS | 8 MISMATCH |
| case2-een-vondst-zonder-vinddag | 8 | PASS | PASS | FAIL controledag 0 | PASS | PASS | 5 MISMATCH |
| case3-geen-vondsten | 6 | PASS | PASS | FAIL afrekendag leeg | PASS | PASS | 4 MISMATCH |

Replay per uitvoerder: `oracle: PASS (8 expectations)`, `(8 expectations)`, `(6 expectations)`; `clingo: PASS`
idem; exectak `PLAYED 3`; DRL-tak `PLAYED 3`; exectak op de jar van de claim:

    FAIL case1-twee-gevonden-vondsten.json: attribuut sluitdag: verwacht 32, werkelijk 1; attribuut afrekendag: verwacht 36, werkelijk leeg; attribuut controledag: verwacht 46, werkelijk 0 | case2-een-vondst-zonder-vinddag.json: attribuut controledag: verwacht 36, werkelijk 0 | case3-geen-vondsten.json: attribuut afrekendag: verwacht 5, werkelijk leeg

### Gate-12-voorstel (`750cb48a`, `scripts/check-drools.sh`, beschermd pad)

- `EXPECTED_PLAYED`: 224 -> 225, redenblok "224 -> 225 (2026-09-30, the operand under a quantifier condition)" na dat
  van jutterswet-premiestaffel.
- `EXPECTED_DRL_PLAYED`: 190 -> 191, redenregel "190 -> 191 (2026-09-30, …): jutterswet-sluitdag, new. This branch
  plays its three cases with no change of its own, PLAYED 3 …".
- De kop van `DRL_CHANNEL` klasse (A): een hertelzin; het gat blijft 34 (225 - 191) en de zes klassekoppen houden 91
  regels. `EXPECTED_MIN_TESTS` (239), `EXPECTED_CHANNEL`, `EXCLUDE_KNOWN` en de lijsten zelf ongewijzigd.

### (d) Sweep

Over alle 281 goldens van de claim, elke golden met al zijn cases, statusregel per golden, jar van de claim tegen jar
met de fix: exectak en DRL-tak alle 281 regels byte-gelijk (PLAYED 224 en 190). Een proefjar die meldt waar de fix de
expr zet: 53 regels in 17 goldens (43 met een kwantorvouw, 6 met een rolcheck aan de overkant, 3 met een
aggregatievergelijking, 1 met een samenstelling), in jutterswet-keurploeg, -loodsgrens, -ploegpremie,
-postbezetting, -redderkeur, -reddingsvloot, -schouwploeg, -sleepvloot, -uniekwantor, -uniestratum,
-unievoorwaarde, -vuurtoren, -vuurtoren-flogic, -wachtvenster, -wrakmelding, -zinkende-jutter en
near-side-rolcheck-to-many; hun statusregels zijn gelijk. De beangeneratie is niet aangeraakt. Met de nieuwe golden:
gate 12 `played 225`, `DRL-text branch played 191`.

### Gates

Alle zestien in de volgorde van `gates.yml`, op de kop `0089cf79` met een schone werkboom: `eval $(opam env)`, `dune
build`, dan elk script onder zijn shebang (bash voor catala, regelspraak, toka-parity, drools, flogic, graaf en
scenario; sh voor de negen andere), plus `dune build @ref/runtest` na gate 5. Catala aanwezig. Per gate de exitcode
en de laatste niet-lege regel van de uitvoer:

    dune build: exit 0
    check-self-contained (sh, 1s): exit 0 — check-self-contained: PASS
    check-ir-schema (sh, 24s): exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 321 file(s) validate and round-trip, floor 192; 42198 object(s) in 822 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema (sh, 2s): exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 798 case file(s) validate)
    check-verifier (sh, 10s): exit 0 — check-verifier: PASS (321 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle (sh, 4s): exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 758 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    dune build @ref/runtest: exit 0
    check-clingo (sh, 153s): exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 758 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop (sh, 3s): exit 0 — check-noop: PASS (714 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood (sh, 19s): exit 0 — check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala (bash, 2s): exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak (bash, 25s): exit 0 — check-regelspraak: PASS — JUnit suite green (999 tests, floor 173); 109 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity (bash, 48s): exit 0 —     Drools excludes by form:
    check-drools (bash, 256s): exit 0 —   JUnit 906 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation (sh, 82s): exit 0 —   corpus-wide accounting: 86 unit-bearing annotations = 45 twistable + 41 exempt (the annotation IS the source) + 0 without a convertible sister + 0 at a §2.10 fourth-class branch (v0 derives no result unit) + 0 outside the twist table
    check-flogic (bash, 0s): exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf (bash, 10s): exit 0 — check-graaf: PASS
    check-scenario (bash, 6s): exit 0 — check-scenario: PASS — 16 scenario golden(s) (floor 7) regenerate 57 case file(s) (floor 31) and their ir.json byte-identically

Waar de laatste regel niet de PASS-regel is: gate 11 `check-toka-parity: PASS` (daarna de samenvatting, met als
laatste regel de lege lijst "Drools excludes by form:"); gate 12 `check-drools: PASS`, `played 225`, `DRL-text branch
played 191 (… pin 191)`, `channel floor 57 non-PLAYED goldens pinned by name; played matches the pinned 225`;
gate 13 `check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays
reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`, daarna
zijn boekhouding; gate 15 ook `check-graaf: route leg — the graph of the wire equals the graph of the source on all
109 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)`. De toelating van de golden draaide de
zestien gates ook (`scripts/admit.sh`, groen).

### README (`0089cf79`)

`backends/drools/README.md`, eigen sectie "Recorded: a condition that puts no test on the subject": welke voorwaarden
geen toets op het onderwerp zetten, wat de exectak nu doet (golden, getuige), waarom een kenmerkvouw meestal speelde,
en wat na de kaart nog niet speelt (open punten 1 tot 3).

### Afwijkingen van de kaart

1. De fix gaat verder dan de kwantorvoorwaarde: ook een aggregatievergelijking en een rolcheck aan de overkant (en
   de regelversiecheck, niet gemeten). Reden: zij laten het patroon op dezelfde manier zonder toets, zij waren gemeten
   fout (e-aggregatie-na, e-aggregatie-eigen-drempel-na, -constant-na, e-rolcheck-na), en de fix zit op die ene plek.
2. De fix maakt ook een eigen operand van de voorwaarde reactief, niet alleen de operanden van de waarde (vorm 3
   hierboven; zonder dat bleef e-aggregatie-eigen-drempel-constant-na fout).
3. `EXPECTED_DRL_PLAYED` gaat ook +1: de golden heeft bewust geen defaultsgroep en geen maximum van twee operanden,
   zodat de DRL-tak en clingo haar spelen; R1 en de defaultsgroep staan in de getuige.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/IR-SPEC.md` ch. 5.5, "Recorded flank (measured 2026-09-23 …)", eerste bullet: "in a fold filter or a
  quantifier filter inside a condition" aanvullen met "or the matrix of a quantifier fold (measured 2026-09-30: with
  'de bergdag van geen van de vondsten van de Berging gevuld is' declared before the rule that derives the bergdag,
  both Drools branches write 1 where `ref/` and `backends/clingo` answer leeg)".
- Geen rij in `spec/CONSTRUCTS.md`: RUNTIME-SEMANTICS ch. 5.1 schrijft de versheid al voor; de exectak voldeed er niet
  aan en doet het nu.

### Open punten

1. Een vouw waarvan het oordeel waar is zolang een afgeleide waarde nog leeg is ("de bergdag van geen van de vondsten
   van de Berging gevuld is"), met de regel vóór de schrijver: beide Drools-takken vuren op die lege waarde en de
   schrijving blijft (`verwacht leeg, werkelijk 1`, b-flank-geen-onwaar-na); met de regel na de schrijver spelen ze
   (b-flank-geen-onwaar-voor). Dit is de leeg-toets van de README-sectie over een voorwaarde over een rol en de eerste
   bullet van de IR-SPEC-flank, nu in de matrix van een vouw; de fix daarvoor is een salience onder de schrijvers van
   wat de toets leest, geen reactiviteit.
2. Een kwantorvouw als lid van een tellende samenstelling ("hij aan ten minste één van de volgende voorwaarden
   voldoet", met als lid "de bergdag van alle vondsten van de Berging is gevuld") weigeren beide takken
   (b-tenminste-samengesteld-na); de exectak met de algemene zin `regel (voorwaarde buiten Phase-B scope)`, die de vorm
   niet noemt, de DRL-tak met een marker die hem wel noemt.
3. De DRL-teksttak: een aggregatievergelijking met een later afgeleide eigen drempel vuurt niet
   (e-aggregatie-eigen-drempel-na en -constant-na, `verwacht 21, werkelijk leeg`); een operand één rol ver onder een
   vouw die bij invoegen waar is, wordt gelezen voordat zijn schrijver liep (c-nav-invoer-na, `verwacht 53, werkelijk
   1`; zonder kwantor speelt dezelfde operand, c-nav-zonder-kwantor-na); een document met een defaultsgroep weigert zij
   (d-groep-*).
4. De regelversiecheck ("regelversie X is gevuurd / inconsistent") laat het patroon ook zonder toets en krijgt nu
   dezelfde expr; niet gemeten, want geen corpusdocument draagt de vorm en ref voert "is gevuurd" niet uit (gemeten in
   de kaart drools-exec-voorwaarde-genavigeerde-afleiding: `EVALUATION FAULT: 'is gevuurd' is not implemented`).
5. Objectcreatie, feitcreatie en de begeleiders (§6.5, wortel, leeg startpunt, gevuurd) nemen dezelfde voorwaarde via
   `regelVoorwaarde` zonder deze wijziging; niet gemeten of een objectcreatie of feitcreatie onder zo'n voorwaarde een
   eigen operand mist die later wordt geschreven. De deling- en wortelbegeleider zetten zelf een toets (de lege of
   negatieve operand) of de expr (de padsonde) op hun patroon, de startpuntbegeleider een toets op het anker; de
   gevuurd-begeleider leest geen operand van de waarde.
6. Het maximum of minimum van twee scalaire operanden vertaalt de ASP-backend niet (`TRANSLATION FAULT`, R1; de kaart
   zegt: niet bouwen); daarom staat die vorm niet in de golden.
7. De frontend weigert "de maximale waarde van de bergdag van alle vondsten van de Berging plus 1" (zie de meetnoot in
   (a)); "1 plus de maximale waarde …" gaat wel. Niet onderzocht of dat de bedoelde lezing is.
8. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; de pin moet na het landen mee.

### Commits

    fdd9d730 claim: drools-exec-kwantorvoorwaarde-eigen-operand
    77042037 KwantorvoorwaardeOperandWitnessTest: operand onder een kwantor, rood
    97b0e912 Exectak: voorwaarde zonder toets op het onderwerp houdt het reactief
    e4ee8b65 golden jutterswet-sluitdag: operanden onder een kwantorvoorwaarde
    750cb48a voorstel gate 12: jutterswet-sluitdag speelt in beide takken
    0089cf79 backends/drools/README: een voorwaarde zonder toets op het onderwerp

en de commit die dit Result in de kaart zet en haar naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.
