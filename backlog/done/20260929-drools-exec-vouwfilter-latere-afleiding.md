# drools-exec-vouwfilter-latere-afleiding: een vouw met een subselectie op afwezigheid herziet niet na een latere afleiding

*Vondst van een regelsetbouwer, 2026-09-29 (v2-skelet-vervolg), bevestigd door
de hoofdsessie op uchar `26ca6b0b` via de distributie (`bin/regelspraak
scenario`) en de eigen uitvoerders; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/vouwfilter/` (bron.rgs, scenario.rgs, cases/; alleen lezen).
Een stil fout antwoord: `check` exit 0, ref en clingo halen de verwachting,
de exec-tak niet.*

## Aanleiding

Een vouw over een rol met een subselectie die op afwezigheid toetst ("die aan
de volgende voorwaarde voldoen: zijn afgeleide dag is leeg") neemt in de
exec-tak een element op zolang die waarde nog niet is afgeleid, en herziet
de vouw niet wanneer een andere regel de waarde later schrijft. Met een
gewone regel als schrijver hangt de uitkomst af van de bronvolgorde
(schrijver vóór de vouw goed, erna fout); met een defaultsgroep als
schrijver is zij altijd fout; een ontkend kenmerk in de subselectie in
plaats van de leeg-toets gedraagt zich hetzelfde (meting van de bouwer op
`d59dc37`). Dezelfde familie als de kaart
drools-exec-voorwaarde-genavigeerde-afleiding (een toets die niet opnieuw
loopt wanneer de geteste waarde later verandert), nu in het filter van een
vouw. `backends/drools/README.md` noemt de volgordegrens van de leeg-toets
voor een voorwaarde; voor de subselectie van een vouw geldt hij ook, en
zonder de volgorde te helpen bij een defaultsgroep.

## Minimale reproductie

`26ca6b0b`, distributie `bin/regelspraak check|scenario|cases`.

    Objecttype de Vondst (mv: Vondsten)
        de vinddag Datum in dagen;
        de opgegeven dag Datum in dagen;
        de afgeleide dag Datum in dagen;
        het gewicht Numeriek (geheel getal);
        het zwaarste gewicht Numeriek (geheel getal);
    Objecttype de Berging (mv: Bergingen)
    Feittype BergingBetreftVondst
        de berging Berging
        de vondst (mv: vondsten) Vondst
    Eén berging betreft meerdere vondsten

    Regel afgeleid
        geldig altijd
            De afgeleide dag van een Vondst moet gesteld worden op de vinddag van de Vondst
            indien zijn vinddag gevuld is.

    Afwijking van afgeleid: opgave gaat voor
        geldig altijd
            De afgeleide dag van een Vondst moet gesteld worden op zijn opgegeven dag
            indien zijn opgegeven dag gevuld is.

    Regel zwaarste
        geldig altijd
            Het zwaarste gewicht van een Vondst moet berekend worden als de maximale waarde van het gewicht van alle vondsten van de berging van de Vondst die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg.

    Scenario: een Berging B met vondsten V1 (gewicht 10, geen vinddag) en V2 (vinddag 01-03-2026,
    gewicht 20); verwacht: de afgeleide dag van V2 is 01-03-2026, het zwaarste gewicht van V1 is 10.

    nu: check exit 0; scenario FAIL "attributeValue V1 zwaarste gewicht (verwacht=10, werkelijk=20)";
        cases + ref `oracle: PASS (2 expectations)`, clingo `clingo: PASS (2 expectations)`.

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: de
  leeg-toets en de gevuld-toets in de subselectie, een ontkend en een
  bevestigd kenmerk, een vergelijking op een later afgeleide waarde; de
  schrijver als gewone regel vóór en na de vouw, en als defaultsgroep; de vouw
  in een waarde en in een voorwaarde (kwantor); een en twee rollen ver.
- (b) De exec-tak herziet een vouw wanneer een waarde die haar subselectie
  toetst later verandert (dezelfde far-side-reactiviteit als de kaart
  drools-exec-voorwaarde-genavigeerde-afleiding gaf aan een voorwaarde over
  een rol), of weigert de vorm benoemd waar dat niet lukt; rood-eerst
  JUnit-getuige met deze reproductie.
- (c) Golden (werknaam `jutterswet-zwaarste-vondst`), handgerekend, in ref ≡
  clingo ≡ Drools-exec; `EXPECTED_PLAYED` in `scripts/check-drools.sh` als
  **voorstel**; de DRL-teksttak volgens haar kanaal.
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). `backends/drools/README.md`: de
  alinea over de leeg-toets noemt de subselectie van een vouw.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel na de kaart
drools-exec-defaults-justificatie-vouw (zelfde regelevaluatie).

## Result (2026-09-30, bouwer)

Tak `card/drools-exec-vouwfilter-latere-afleiding` in de worktree van de kaart, afgetakt van main `c16cb4d6`; niet
gerebased, niet gepusht, niet gemerged. De kaart is geschreven op `26ca6b0b`; sinds dat punt landden in dezelfde
transpiler drools-exec-defaults-justificatie-vouw, drools-exec-kwantorvoorwaarde-eigen-operand en
drools-exec-datumvouw-over-rol, dus alles is op deze kop opnieuw gemeten. Gemeten met de eigen uitvoerders van deze
repo: `_build/default/ref/bin/runner.exe`, `_build/default/backends/clingo/bin/clingo_runner.exe`,
`_build/default/spec/verifier/bin/verifier.exe`, de frontend (`Resolver --ir … --cases … bron.rgs scenario.rgs`) en
de Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de teksttak). Vier jars van de exectak: "vóór" is de jar
van de claim (`7a687922`, backend gelijk aan main), "na" die van de fix (`e781b077`), en twee proefjars met elk één
helft van de fix: "alleen herziening" (geen NO_LOOP op een regel met een vouw met subselectie in de waarde, geen
volgorde) en "alleen volgorde" (de volgorde, NO_LOOP overal). Elke vorm is een eigen Jutterswet-document (bron.rgs +
scenario.rgs), door een generator geschreven, door de frontend naar ir.json en cases verlaagd en door een meetscript
gemeten; beide staan buiten de repo, naast de reproductie van de kaart. De dertig vormen met een kwantor over twee
rollen weigert de frontend als zin; hun IR is met een script afgeleid van de tweeling over één rol (onderwerp de
Vondst, `sourcePath` berging → vondst). Alle gemeten documenten zijn verifier-schoon (`verifier: PASS`).

### De reproductie op deze kop

De IR die de frontend op deze kop uit bron.rgs en scenario.rgs van de kaart maakt, is byte-gelijk aan de ir.json van
de kaart. ref `oracle: PASS (2 expectations)`, clingo `clingo: PASS (2 expectations)`, exectak vóór `FAIL
case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20`, na
`PLAYED 1` (ook met elk van de twee proefjars), DRL-tak `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch
has no defaults lowering`.

### Oorzaak

1. **Een herziening die NO_LOOP tegenhoudt.** Een vouw in de waarde is een accumulate. Verlaat een element haar
   subselectie (de afgeleide dag van V2 wordt geschreven), dan trekt Drools de bijdrage terug en werkt het resultaat
   bij. Gelezen in de broncode van Drools 8.44: `PhreakAccumulateNode.removeMatch` zet geen propagatiecontext op de
   accumulatie (alleen `addMatch` doet dat, met die van het element), dus `propagateResult` geeft de bijgewerkte tupel
   de context van de linkertupel, het onderwerp. `PhreakRuleTerminalNode.doLeftTupleUpdate` blokkeert bij een
   NO_LOOP-regel een activatie waarvan de recentste context uit de eigen consequentie komt. Was de eigen schrijving
   van de regel de laatste wijziging van het onderwerp, dan viel de herziening weg en bleef het maximum staan dat het
   element nog meetelde. Een spoor van de vuringen op de reproductie: na `defaults A:Vondst.afgeleide dag [V2]` ontstaat
   alleen `zwaarste [V2 10]`, niet `zwaarste [V1 10]`. Een element dat de subselectie binnenkomt (de gevuld-toets, het
   bevestigde kenmerk, de vergelijking) loopt via `addMatch` en speelde al. Of de eigen schrijving het onderwerp
   raakt, hangt aan het onderwerppatroon: een patroon zonder toets reageert op geen bijwerking (één rol ver zonder eigen
   voorwaarde speelde), een patroon met een toets op elke (één rol ver met "indien zijn openingsdag gevuld is" faalde).
   Twee rollen ver is het onderwerp een Vondst, het lid van de vouw heeft het type van het onderwerp, en `eigenReads`
   telt die lezing als eigen lezing en zet de versheidsexpr op het patroon.
2. **Een bron die leeg wordt.** Verlaten alle elementen de subselectie, dan is de maximale waarde leeg (ch. 1.2e). De
   kale accumulate bindt dan geen rij en de regel vuurt niet; met een telling ernaast (gemeten, verworpen alternatief 3)
   schrijft `werkBij` de leegte niet, omdat leeg op een staand doel niets beslist (RUNTIME-SEMANTICS ch. 7.8). De waarde
   over de tijdelijke bron bleef staan, ook over een invoer van de case heen, en een kenmerk dat erop was toegekend
   bleef. ref en clingo rekenen de vouw na haar schrijver uit en zien de tijdelijke bron nooit.
3. **Een vouw in een voorwaarde.** Een regel die vuurde terwijl de getoetste waarde nog leeg was (ten minste één met
   het element er nog in, alle over een subselectie die nog leeg is), schrijft niet terug wanneer het oordeel omslaat.
   Dat is de eerste bullet van de IR-SPEC-flank (ch. 5.5) en de leeg-toets van de README-sectie over een voorwaarde
   over een rol, nu in de subselectie van een vouw; bij een defaultsgroep als schrijver hielp de bronvolgorde niet, want
   een groep komt na de regels in het model.

### Fix (`e781b077`) en de gekozen vorm

`ExecModelTranspiler.java` +121, −6 regels, commentaar inbegrepen; `Stratificatie`, de DRL-tak, de loader en de
beangeneratie zijn niet aangeraakt.

- **Herziening.** `regel()` zet geen NO_LOOP op een regel waarvan de waarde een vouw over een setbron met een
  subselectie bevat (`Expr.bevat` op `isInstantievouw` met `filter() != null`). `werkBij` schrijft alleen een
  verandering, dus de regel vuurt op haar eigen schrijving nog één keer, schrijft niets en stopt.
- **Volgorde.** `groepVolgorde` zet een regel onder de band van elke regel en groep die schrijft wat een vouw in haar
  voorwaarde of waarde negatief leest (`vouwOntkend`), in de polariteit van de afhankelijkheidswandeling van de verifier
  (`dep_cond`, WF-046): een lezing onder een leeg- of gevuld-toets en een ontkend kenmerk; elke lezing in het bereik (de
  subselectie) van een kwantor anders dan ten minste één; elke lezing in de matrix onder geen, ten hoogste en precies;
  beide onder een ontkenningsachtige samenstelling. Alleen een lezing binnen een vouw telt: een leeg-toets buiten een
  vouw blijft de grens van de README. De band van een schrijver is de laagste band van hem en wat hij leest, zodat de
  regel ook na diens eigen producenten komt. Deze randen zijn een deel van de negatieve randen van de verifier; eigen
  gevolgtrekking: een verifier-schoon document geeft daardoor geen kring (WF-046). De kringmelding van `groepVolgorde`
  noemt nu ook een vouw in een voorwaarde.

Elk van de twee helften alleen laat gemeten vormen fout (tabel (a)): alleen de volgorde laat een vergelijking fout
waarvan het element de subselectie verlaat nadat de regel vuurde (een initialisatie, dan een berekening:
x-herzien-vergelijking-*), want een vergelijking is geen negatieve lezing; alleen de herziening laat de voorwaarde, de
bron die leeg wordt, de invoer op het doel en het kenmerk op de vouw fout (18 vormen).

Verworpen:
1. Alleen een volgorde (een salience onder de schrijvers): herziet geen vouw waarvan een element na het vuren
   verandert; de kaart vraagt dat de vouw herziet (de far-side-reactiviteit van drools-exec-voorwaarde-genavigeerde-
   afleiding), en de twee x-herzien-vormen en de getuigerij `een_vergelijking_tussen_twee_gevulde_waarden` blijven dan
   fout.
2. Alleen de herziening: 18 vormen blijven fout (de voorwaarde, de lege bron, de invoer, het kenmerk op de vouw).
3. Een telling naast de kale ordenende vouw, zodat een bron die leeg wordt leeg leest (gebouwd en gemeten): de
   leegte beslist op een staand doel niets (ch. 7.8), dus x-alle-weg bleef `verwacht leeg, werkelijk 20`; met de
   volgorde is de telling niet nodig en ze is weer weggehaald.
4. Het filter als beta-constraint of binnen de accumulatiefunctie, zodat een element blijft en alleen zijn bijdrage
   verandert: ook `doRightUpdatesProcessChildren` roept `removeMatch` zonder context aan voor een element dat niet meer
   past, en een bijdrage `null` in de accumulatie is precies wat de bestaande `!= null`-bewaking in `vouw` voorkomt
   ("Reversing a not existing accumulated object").
5. Een eigenschapsgerichte `update` met een `watch` die het eigen doel uitsluit: verandert de bijwerkdiscipline van
   elke regel en raakt de patronen met een `watch` in de verdeling.
6. De volgorde in `Stratificatie` in plaats van `groepVolgorde`: `Stratificatie` delen beide takken, en de DRL-tak
   hoort volgens de kaart haar kanaal te houden; `groepVolgorde` is alleen van de exectak en kent de groepen al.
7. De vormen in een voorwaarde benoemd weigeren: de volgorde laat ze spelen, en een weigering had de vormen die al
   speelden (de schrijver vóór de regel, een oordeel dat na de schrijver waar wordt) uit het kanaal gehaald.

### (a) Vormruimte, vóór en na

Situatie van elke vorm: een Berging B met V1 (gewicht 10, geen vinddag) en V2 (vinddag 01-03-2026, gewicht 20), in de
x-vormen soms met een derde gegeven (een vinddag voor V1, een invoer op het doel, een opgegeven dag). De schrijver leidt
de afgeleide dag af (de vinddag, of de opgegeven dag in de groep); gedateerd volgt uit de vinddag (gewone regel) of uit
de afgeleide dag (bij de groep). Naamgeving: w-<filter>-<afstand>-<schrijver> is de vouw in de waarde (de maximale
waarde van het gewicht), v-<filter>-<oordeel>-<afstand>-<schrijver> de vouw als kwantor in een voorwaarde
(lichtklasse 1), met oordeel "waar" (pas waar na de schrijver) of "vals" (alleen waar zolang de schrijver niet liep);
filter leeg, gevuld, niet-kenmerk ("hij is niet gedateerd"), kenmerk, vergelijking ("later dan 01-02-2026"); afstand
1 (onderwerp de Berging), 1v (idem, met de eigen voorwaarde "indien zijn openingsdag gevuld is") of 2 (onderwerp de
Vondst, "alle vondsten van de berging van de Vondst"); schrijver voor (in de bron vóór de vouw), na, of groep. De
x-vormen: x-alle-weg (beide vondsten verlaten de subselectie), x-invoer-blijft (idem, met een invoer 99 op het doel),
x-gevolg-kenmerk (een kenmerktoekenning leest het resultaat), x-aantal en x-som (andere vouwsoorten), x-plus (de vouw
als operand, "1 plus de maximale waarde"), x-aggvergelijking (een aggregatievergelijking in de voorwaarde),
x-herzien-vergelijking (een vergelijking op een registratiedag die een initialisatie en daarna een berekening
schrijven). "idem" in een kolom van de exectak: gelijk aan vóór.

| vorm | omschrijving | ref | clingo | exec vóór | alleen herziening | alleen volgorde | exec na | DRL-tekst (vóór = na) |
|---|---|---|---|---|---|---|---|---|
| v-gevuld-vals-1-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel onwaar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-gevuld-vals-1-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel onwaar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| v-gevuld-vals-1-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel onwaar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-gevuld-vals-2-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel onwaar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-gevuld-vals-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-gevuld-vals-2-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel onwaar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-gevuld-vals-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-gevuld-vals-2-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel onwaar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-gevuld-vals-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-gevuld-waar-1-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel waar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-gevuld-waar-1-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel waar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-gevuld-waar-1-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel waar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-gevuld-waar-2-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel waar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-gevuld-waar-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-gevuld-waar-2-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel waar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-gevuld-waar-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-gevuld-waar-2-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is gevuld', oordeel waar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-gevuld-waar-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-vals-1-groep | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel onwaar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-kenmerk-vals-1-na | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel onwaar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-vals-1-voor | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel onwaar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-vals-2-groep | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel onwaar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-kenmerk-vals-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-vals-2-na | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel onwaar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-kenmerk-vals-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-vals-2-voor | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel onwaar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-kenmerk-vals-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-waar-1-groep | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel waar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-kenmerk-waar-1-na | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel waar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-waar-1-voor | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel waar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-waar-2-groep | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel waar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-kenmerk-waar-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-waar-2-na | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel waar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-kenmerk-waar-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-kenmerk-waar-2-voor | vouw als kwantor in de voorwaarde, filter 'hij is gedateerd', oordeel waar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-kenmerk-waar-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-vals-1-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel onwaar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-leeg-vals-1-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel onwaar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| v-leeg-vals-1-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel onwaar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-vals-2-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel onwaar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-leeg-vals-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-vals-2-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel onwaar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-leeg-vals-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-vals-2-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel onwaar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-leeg-vals-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-waar-1-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel waar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-leeg-waar-1-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel waar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-waar-1-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel waar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-waar-2-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel waar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-leeg-waar-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-waar-2-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel waar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-leeg-waar-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-leeg-waar-2-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is leeg', oordeel waar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-leeg-waar-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-vals-1-groep | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel onwaar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-niet-kenmerk-vals-1-na | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel onwaar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-vals-1-voor | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel onwaar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-vals-2-groep | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel onwaar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-niet-kenmerk-vals-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-vals-2-na | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel onwaar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-niet-kenmerk-vals-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-vals-2-voor | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel onwaar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-niet-kenmerk-vals-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-waar-1-groep | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel waar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-niet-kenmerk-waar-1-na | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel waar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-waar-1-voor | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel waar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-waar-2-groep | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel waar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-niet-kenmerk-waar-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-waar-2-na | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel waar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-niet-kenmerk-waar-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-niet-kenmerk-waar-2-voor | vouw als kwantor in de voorwaarde, filter 'hij is niet gedateerd', oordeel waar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-niet-kenmerk-waar-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-vals-1-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel onwaar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-vergelijking-vals-1-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel onwaar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| v-vergelijking-vals-1-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel onwaar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-vals-2-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel onwaar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-vergelijking-vals-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-vals-2-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel onwaar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-vergelijking-vals-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-vals-2-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel onwaar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-vergelijking-vals-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-waar-1-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel waar na de schrijver, 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| v-vergelijking-waar-1-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel waar na de schrijver, 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-waar-1-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel waar na de schrijver, 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-waar-2-groep | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel waar na de schrijver, 2 rollen ver, schrijver groep; IR met de hand afgeleid van v-vergelijking-waar-1-groep (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-waar-2-na | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel waar na de schrijver, 2 rollen ver, schrijver na; IR met de hand afgeleid van v-vergelijking-waar-1-na (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| v-vergelijking-waar-2-voor | vouw als kwantor in de voorwaarde, filter 'zijn afgeleide dag is later dan 01-02-2026', oordeel waar na de schrijver, 2 rollen ver, schrijver voor; IR met de hand afgeleid van v-vergelijking-waar-1-voor (de frontend weigert de kwantor over twee rollen met een subselectie) | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | EXCLUDE loader: form not carried by the v0 Drools backend: Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role) | idem | idem | idem | gelijk aan exec vóór |
| w-gevuld-1-groep | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-gevuld-1-na | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-gevuld-1-voor | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-gevuld-1v-groep | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 1 rol ver met een eigen voorwaarde op de Berging, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-gevuld-1v-na | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 1 rol ver met een eigen voorwaarde op de Berging, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-gevuld-1v-voor | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 1 rol ver met een eigen voorwaarde op de Berging, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-gevuld-2-groep | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 2 rollen ver, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-gevuld-2-na | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 2 rollen ver, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-gevuld-2-voor | vouw in de waarde, filter 'zijn afgeleide dag is gevuld', 2 rollen ver, schrijver voor | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-kenmerk-1-groep | vouw in de waarde, filter 'hij is gedateerd', 1 rol ver, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-kenmerk-1-na | vouw in de waarde, filter 'hij is gedateerd', 1 rol ver, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-kenmerk-1-voor | vouw in de waarde, filter 'hij is gedateerd', 1 rol ver, schrijver voor | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-kenmerk-1v-groep | vouw in de waarde, filter 'hij is gedateerd', 1 rol ver met een eigen voorwaarde op de Berging, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-kenmerk-1v-na | vouw in de waarde, filter 'hij is gedateerd', 1 rol ver met een eigen voorwaarde op de Berging, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-kenmerk-1v-voor | vouw in de waarde, filter 'hij is gedateerd', 1 rol ver met een eigen voorwaarde op de Berging, schrijver voor | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-kenmerk-2-groep | vouw in de waarde, filter 'hij is gedateerd', 2 rollen ver, schrijver groep | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-kenmerk-2-na | vouw in de waarde, filter 'hij is gedateerd', 2 rollen ver, schrijver na | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-kenmerk-2-voor | vouw in de waarde, filter 'hij is gedateerd', 2 rollen ver, schrijver voor | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-leeg-1-groep | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-leeg-1-na | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-leeg-1-voor | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-leeg-1v-groep | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 1 rol ver met een eigen voorwaarde op de Berging, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-leeg-1v-na | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 1 rol ver met een eigen voorwaarde op de Berging, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| w-leeg-1v-voor | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 1 rol ver met een eigen voorwaarde op de Berging, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-leeg-2-groep | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 2 rollen ver, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-leeg-2-na | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 2 rollen ver, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| w-leeg-2-voor | vouw in de waarde, filter 'zijn afgeleide dag is leeg', 2 rollen ver, schrijver voor | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-niet-kenmerk-1-groep | vouw in de waarde, filter 'hij is niet gedateerd', 1 rol ver, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-niet-kenmerk-1-na | vouw in de waarde, filter 'hij is niet gedateerd', 1 rol ver, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-niet-kenmerk-1-voor | vouw in de waarde, filter 'hij is niet gedateerd', 1 rol ver, schrijver voor | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-niet-kenmerk-1v-groep | vouw in de waarde, filter 'hij is niet gedateerd', 1 rol ver met een eigen voorwaarde op de Berging, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-niet-kenmerk-1v-na | vouw in de waarde, filter 'hij is niet gedateerd', 1 rol ver met een eigen voorwaarde op de Berging, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| w-niet-kenmerk-1v-voor | vouw in de waarde, filter 'hij is niet gedateerd', 1 rol ver met een eigen voorwaarde op de Berging, schrijver voor | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-niet-kenmerk-2-groep | vouw in de waarde, filter 'hij is niet gedateerd', 2 rollen ver, schrijver groep | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-niet-kenmerk-2-na | vouw in de waarde, filter 'hij is niet gedateerd', 2 rollen ver, schrijver na | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| w-niet-kenmerk-2-voor | vouw in de waarde, filter 'hij is niet gedateerd', 2 rollen ver, schrijver voor | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-vergelijking-1-groep | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 1 rol ver, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-vergelijking-1-na | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 1 rol ver, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-vergelijking-1-voor | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 1 rol ver, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-vergelijking-1v-groep | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 1 rol ver met een eigen voorwaarde op de Berging, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-vergelijking-1v-na | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 1 rol ver met een eigen voorwaarde op de Berging, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-vergelijking-1v-voor | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 1 rol ver met een eigen voorwaarde op de Berging, schrijver voor | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-vergelijking-2-groep | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 2 rollen ver, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| w-vergelijking-2-na | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 2 rollen ver, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| w-vergelijking-2-voor | vouw in de waarde, filter 'zijn afgeleide dag is later dan 01-02-2026', 2 rollen ver, schrijver voor | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| x-aantal-1-groep | telling met filter leeg, 1 rol, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-aantal-1-na | telling met filter leeg, 1 rol, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| x-aantal-2-groep | telling met filter leeg, 2 rollen, schrijver groep | FRONTEND   ERROR (vorm/x-aantal-2-groep/bron.rgs:24): vouw 'het aantal vondsten van de berging van de Vondst die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg' zonder bereik (geen relatie verbindt Vondst met Vondst): a single-argument fold with an elementType carries its range (sourceRole or sourcePath) under every kind but total: the range is written, never derived | | | | | | |
| x-aantal-2-na | telling met filter leeg, 2 rollen, schrijver na | FRONTEND   ERROR (vorm/x-aantal-2-na/bron.rgs:24): vouw 'het aantal vondsten van de berging van de Vondst die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg' zonder bereik (geen relatie verbindt Vondst met Vondst): a single-argument fold with an elementType carries its range (sourceRole or sourcePath) under every kind but total: the range is written, never derived | | | | | | |
| x-aggvergelijking-vals-1-groep | aggregatievergelijking in de voorwaarde, filter leeg, oordeel onwaar na de schrijver, 1 rol, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-aggvergelijking-vals-1-na | aggregatievergelijking in de voorwaarde, filter leeg, oordeel onwaar na de schrijver, 1 rol, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| x-aggvergelijking-vals-2-groep | aggregatievergelijking in de voorwaarde, filter leeg, oordeel onwaar na de schrijver, 2 rollen, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1; verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-aggvergelijking-vals-2-na | aggregatievergelijking in de voorwaarde, filter leeg, oordeel onwaar na de schrijver, 2 rollen, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1; verwacht leeg, werkelijk 1 | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| x-aggvergelijking-waar-1-groep | aggregatievergelijking in de voorwaarde, filter leeg, oordeel waar na de schrijver, 1 rol, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-aggvergelijking-waar-1-na | aggregatievergelijking in de voorwaarde, filter leeg, oordeel waar na de schrijver, 1 rol, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| x-aggvergelijking-waar-2-groep | aggregatievergelijking in de voorwaarde, filter leeg, oordeel waar na de schrijver, 2 rollen, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-aggvergelijking-waar-2-na | aggregatievergelijking in de voorwaarde, filter leeg, oordeel waar na de schrijver, 2 rollen, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| x-alle-weg-1-groep | vouw in de waarde, filter leeg, beide vondsten krijgen een afgeleide dag: de bron wordt leeg, 1 rol, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 20 | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-alle-weg-1-na | vouw in de waarde, filter leeg, beide vondsten krijgen een afgeleide dag: de bron wordt leeg, 1 rol, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 20 | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| x-alle-weg-2-groep | vouw in de waarde, filter leeg, beide vondsten krijgen een afgeleide dag: de bron wordt leeg, 2 rollen, schrijver groep | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 20 | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-alle-weg-2-na | vouw in de waarde, filter leeg, beide vondsten krijgen een afgeleide dag: de bron wordt leeg, 2 rollen, schrijver na | oracle: PASS (4 expectations) | clingo: PASS (4 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 20 | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| x-gevolg-kenmerk-1-groep | een kenmerktoekenning die de vouw leest (zwaar indien het zwaarste gewicht groter is dan 15), filter leeg, 1 rol, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: kenmerk zwaar: verwacht afwezig, werkelijk aanwezig | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-gevolg-kenmerk-1-na | een kenmerktoekenning die de vouw leest (zwaar indien het zwaarste gewicht groter is dan 15), filter leeg, 1 rol, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: kenmerk zwaar: verwacht afwezig, werkelijk aanwezig | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| x-herzien-vergelijking-1v | vergelijking in de subselectie op een waarde die van gevuld naar gevuld gaat (initialisatie, dan berekening), de vouw vóór de schrijvers, 1 rol met een eigen voorwaarde | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | idem | PLAYED 1 | PLAYED 1 |
| x-herzien-vergelijking-2 | vergelijking in de subselectie op een waarde die van gevuld naar gevuld gaat (initialisatie, dan berekening), de vouw vóór de schrijvers, 2 rollen | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 10, werkelijk 20 | PLAYED 1 | idem | PLAYED 1 | PLAYED 1 |
| x-invoer-blijft-1-groep | vouw in de waarde, filter leeg, beide vondsten krijgen een afgeleide dag, een invoer op het doel, 1 rol, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 99, werkelijk 20 | idem | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-invoer-blijft-1-na | vouw in de waarde, filter leeg, beide vondsten krijgen een afgeleide dag, een invoer op het doel, 1 rol, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 99, werkelijk 20 | idem | PLAYED 1 | PLAYED 1 | gelijk aan exec vóór |
| x-plus-1-groep | 1 plus de maximale waarde met filter leeg, 1 rol, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-plus-1-na | 1 plus de maximale waarde met filter leeg, 1 rol, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |
| x-plus-2-groep | 1 plus de maximale waarde met filter leeg, 2 rollen, schrijver groep | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 11, werkelijk 21 | PLAYED 1 | PLAYED 1 | PLAYED 1 | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-plus-2-na | 1 plus de maximale waarde met filter leeg, 2 rollen, schrijver na | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: attribuut zwaarste gewicht: verwacht 11, werkelijk 21 | PLAYED 1 | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| x-som-1-groep | som met filter leeg, 1 rol, schrijver groep | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering |
| x-som-1-na | som met filter leeg, 1 rol, schrijver na | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | PLAYED 1 | idem | idem | idem | gelijk aan exec vóór |

Samengevat, 133 vormen: de frontend weigert er twee (x-aantal-2-*, de telling over twee rollen: `vouw 'het aantal
vondsten van de berging van de Vondst die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg' zonder bereik
(geen relatie verbindt Vondst met Vondst)`); op de andere 131 `verifier: PASS`, ref `oracle: PASS` en clingo `clingo:
PASS`. Exectak vóór: 71 PLAYED, 30 FAIL, 30 EXCLUDE; alleen herziening: 83 PLAYED, 18 FAIL, 30 EXCLUDE; alleen
volgorde: 99 PLAYED, 2 FAIL, 30 EXCLUDE; na: 101 PLAYED, 0 FAIL, 30 EXCLUDE. De 30 EXCLUDE zijn de kwantor over twee
rollen, in beide takken door de loader geweigerd met naam (`Kwantorvouw.bronPad`), vóór en na gelijk. DRL-teksttak vóór
en na gelijk: 55 PLAYED, 9 FAIL, 67 EXCLUDE (37 op de defaultsgroep, 30 op de loader). De 30 vormen die de fix laat
spelen: de waardevouw met een subselectie die het element loslaat, twee rollen ver of met een eigen voorwaarde, met de
schrijver na de vouw of als groep (w-leeg en w-niet-kenmerk, 1v en 2; x-plus-2); de kwantor en de
aggregatievergelijking in een voorwaarde met een oordeel dat alleen vóór de schrijver waar is (v-leeg-vals-1,
v-gevuld-vals-1, v-vergelijking-vals-1, x-aggvergelijking-vals, schrijver na en groep); de bron die leeg wordt, de
invoer op het doel, het kenmerk op de vouw (x-alle-weg, x-invoer-blijft, x-gevolg-kenmerk); de vergelijking tussen twee
gevulde waarden (x-herzien-vergelijking). Waar de schrijver in de bron vóór de vouw staat, speelde elke vorm al,
behalve de kwantor over twee rollen, die de loader weigert; bij een defaultsgroep hielp de bronvolgorde niet (de
reproductie van de kaart heeft de groep vóór de vouw, de groep-vormen van de tabel erna). Ook open punt 1 van drools-exec-kwantorvoorwaarde-eigen-operand
(b-flank-geen-onwaar-na, "de bergdag van geen van de vondsten van de Berging gevuld is" vóór de schrijver) speelt nu in
de exectak: vóór en met alleen herziening `FAIL case1-b-flank-geen-onwaar-na.json: verwacht leeg, werkelijk 1`, met
alleen volgorde en na `PLAYED 1`; de DRL-tak `FAIL case1-b-flank-geen-onwaar-na.json: verwacht leeg, werkelijk 1`.

### (b) Getuigen

`VouwfilterLatereAfleidingWitnessTest` (exectak, vijftien rijen, documenten in de klasse op Jutterswet-materiaal): de
reproductie van de kaart (twee rollen, leeg-toets, defaultsgroep), de gewone schrijver na de vouw, een ontkend kenmerk in
de subselectie, een eigen voorwaarde één rol ver, de vouw in een rekensom, een vergelijking tussen twee gevulde waarden,
een invoer op het doel, een kenmerk dat de vouw leest, een bron die leeg wordt, een kwantor die alleen vóór de schrijver
waar is (gewone schrijver en groep), alle over een subselectie die later vult, een aggregatievergelijking, en drie
flanken (de schrijver vóór de vouw, een subselectie die de vondst opneemt, een kwantor die na de schrijver waar wordt).
Elk document is apart nagemeten: `verifier: PASS`, `oracle: PASS` en `clingo: PASS`. Rood op de claim: twaalf rijen
in `9fb410f1`, drie erbij in `2f4a24e3`; de vijftien van `2f4a24e3` met de transpiler van de claim:

    Tests run: 15, Failures: 12, Errors: 0, Skipped: 0
    alle_over_een_subselectie_die_later_vult:180 alle over een subselectie die later vult … <case: verwacht leeg, werkelijk 1>
    de_reproductie_van_de_kaart:56 de vouw herziet na de defaultsgroep … <case: attribuut zwaarste gewicht: verwacht 10, werkelijk 20>
    de_vouw_in_een_rekensom:99 de vouw als operand … <case: attribuut zwaarste gewicht: verwacht 11, werkelijk 21>
    een_aggregatievergelijking_die_alleen_voor_de_schrijver_waar_is:188 de aggregatievergelijking … <case: verwacht leeg, werkelijk 1>
    een_bron_die_leeg_wordt_leest_leeg:156 de lege bron … <case: verwacht leeg, werkelijk 20>
    een_eigen_voorwaarde_een_rol_ver:90 de eigen voorwaarde op een rol afstand … <case: attribuut zwaarste gewicht: verwacht 10, werkelijk 20>
    een_gewone_schrijver_na_de_vouw:65 de schrijver na de vouw … <case: attribuut zwaarste gewicht: verwacht 10, werkelijk 20>
    een_invoer_op_het_doel_blijft_staan:131 de invoer op het doel … <case: attribuut zwaarste gewicht: verwacht 99, werkelijk 20>
    een_kenmerk_dat_de_vouw_leest:146 het kenmerk op de vouw … <case: kenmerk zwaar: verwacht afwezig, werkelijk aanwezig>
    een_kwantor_die_alleen_voor_de_schrijver_waar_is:168 de gewone schrijver na de regel … <case: verwacht leeg, werkelijk 1>
    een_ontkend_kenmerk_in_de_subselectie:75 het ontkende kenmerk … <case: attribuut zwaarste gewicht: verwacht 10, werkelijk 20>
    een_vergelijking_tussen_twee_gevulde_waarden:116 de herziening tussen twee gevulde waarden … <case: attribuut zwaarste gewicht: verwacht 10, werkelijk 20>

Het tweede deel van de kwantorrij (de groep als schrijver) liep daar niet; als document apart gemeten op de jar van de
claim: `FAIL case1.json: verwacht leeg, werkelijk 1` (en v-leeg-vals-1-groep in de tabel). De drie flanken zijn groen
vóór en na. Met alleen de volgorde (NO_LOOP overal) faalt precies één rij: `een_vergelijking_tussen_twee_gevulde_waarden:116
… <case: attribuut zwaarste gewicht: verwacht 10, werkelijk 20>`. Na de fix: de klasse 15 tests, 0 failures; de suite
937 tests, 0 failures, 0 skipped (922 op de claim; vloer 239). Geen bestaand testbestand is gewijzigd.

### (c) Golden `jutterswet-zwaarste-vondst` (`269ef1df`)

Vier vouwen over de vondsten van een Berging waarvan de subselectie een waarde toetst die regels erna afleiden:
`zwaarste` (de maximale waarde van het gewicht van alle vondsten van de berging van de Vondst, "zijn afgeleide dag is
leeg": de reproductie van de kaart), `zwaarste late` (dezelfde vouw, "zijn registratiedag is later dan 01-02-2026"),
`losse gewicht` (de som over "hij is niet gedateerd") en `meldklasse` (1 indien het gewicht van ten minste één van de
vondsten "die ...: zijn afgeleide dag is leeg" groter is dan 15). De afgeleide dag komt uit een defaultsgroep ("Regel
afgeleid" en "Afwijking van afgeleid: opgave gaat voor"), gedateerd uit een regel op de afgeleide dag, de
registratiedag uit een initialisatie op de vinddag en een berekening op de opgegeven dag; de vier lezers staan in de
bron vóór hun schrijvers. Handgerekend in NOTE.md. De IR uit `--ir bron.rgs` alleen is byte-gelijk aan die met het
scenario. Toegelaten via `scripts/admit.sh` met het gate-12-voorstel in de werkboom: `admit: ADMITTED
corpus/golden/jutterswet-zwaarste-vondst (3 case(s); verifier-clean, ref = clingo, no-op refuted, gates green)`.

| case | verwachtingen | ref | clingo | exec vóór | alleen herziening | alleen volgorde | exec na | no-op |
|---|---|---|---|---|---|---|---|---|
| case1-een-gedateerde-en-een-ongedateerde-vondst | 11 | `oracle: PASS (11 expectations)` | `clingo: PASS (11 expectations)` | FAIL meldklasse `verwacht leeg, werkelijk 1` | FAIL (idem) | PASS | PASS | 8 MISMATCH |
| case2-de-opgave-gaat-voor | 16 | `oracle: PASS (16 expectations)` | `clingo: PASS (16 expectations)` | FAIL `attribuut zwaarste gewicht: verwacht 20, werkelijk 25` | PASS | PASS | PASS | 14 MISMATCH |
| case3-alle-vondsten-gedateerd | 12 | `oracle: PASS (12 expectations)` | `clingo: PASS (12 expectations)` | FAIL `verwacht leeg, werkelijk 20` (3×), `verwacht leeg, werkelijk 1` | FAIL (idem) | PASS | PASS | 8 MISMATCH |

Per golden, literal: exectak vóór

    FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 | case2-de-opgave-gaat-voor.json: attribuut zwaarste gewicht: verwacht 20, werkelijk 25 | case3-alle-vondsten-gedateerd.json: verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 1

alleen herziening `FAIL case1-een-gedateerde-en-een-ongedateerde-vondst.json: verwacht leeg, werkelijk 1 |
case3-alle-vondsten-gedateerd.json: verwacht leeg, werkelijk 20; verwacht leeg, werkelijk 20; verwacht leeg, werkelijk
20; verwacht leeg, werkelijk 1`; alleen volgorde en na `PLAYED 3`; DRL-tak `EXCLUDE drl: ch. 7 prioritizedDefaults —
the DRL-text branch has no defaults lowering`. In deze golden speelt `zwaarste late` ook met alleen de volgorde
(gemeten); de herziening zelf pint de getuigerij `een_vergelijking_tussen_twee_gevulde_waarden`.

### Gate-12-voorstel (`1eb07f5c`, `scripts/check-drools.sh`, beschermd pad)

- `EXPECTED_PLAYED`: 229 → 230, redenblok "229 -> 230 (2026-09-30, the fold whose subselection tests a value derived
  later)" na dat van jutterswet-vondstbereik.
- `DRL_CHANNEL` klasse (A): `"jutterswet-zwaarste-vondst|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in
  this branch"` op naamvolgorde; de kop 37 → 38 regels ("37 of the 38 lines refuse on that one sentence", "The 39 is
  43 exec-only players — those 37, …") en een hertelzin (gat 39 = 230 − 191; A 38, B 37, C 11, D 2, E 3, F 3 = 94
  regels); lijstkop "eighty-eight of the ninety-four".
- `EXPECTED_DRL_PLAYED` blijft 191, `EXPECTED_MIN_TESTS` blijft 239, `EXPECTED_CHANNEL` en `EXCLUDE_KNOWN`
  ongewijzigd.

### (d) Sweep

Over alle 284 goldens van de claim, elke golden met al zijn cases, statusregel per golden, jar van de claim tegen jar
met de fix: exectak en DRL-tak alle 284 regels byte-gelijk (PLAYED 229 en 191). Een proefjar die meldt waar de fix
ingrijpt: 18 regels in 14 goldens zonder NO_LOOP (anchor/jutterswet-dubbelwacht, jutterswet-drempelvangst,
-erfwacht, -getijdenwacht (3), -keurlast, -premierecht, -strandgoed (2), -tegenwacht (2), -vast-startpunt,
-vocabulaires, -vroegste-vondst, -wachtvenster, synth2-clip-cond-over-prorata, synth2-clip-over-prorata), en 2 regels
een band lager, `sluitdag` en `controledag` van jutterswet-sluitdag (salience 0 → −1: hun kwantor leest de bergdag
onder een gevuld-toets); hun statusregels zijn gelijk. Met de nieuwe golden: gate 12 `played 230`, `DRL-text branch
played 191`.

### Gates

Alle zestien in de volgorde van `gates.yml`, op de kop `63702c13` met een schone werkboom: `eval $(opam env)`, `dune
build`, dan elk script onder zijn shebang (bash voor catala, regelspraak, toka-parity, drools, flogic, graaf en
scenario; sh voor de negen andere), plus `dune build @ref/runtest` na gate 5. Catala aanwezig. Per gate de exitcode en
de laatste niet-lege regel van de uitvoer:

    dune build: exit 0
    check-self-contained (sh, 1s): exit 0 — check-self-contained: PASS
    check-ir-schema (sh, 23s): exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 324 file(s) validate and round-trip, floor 192; 42697 object(s) in 825 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema (sh, 2s): exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 815 case file(s) validate)
    check-verifier (sh, 12s): exit 0 — check-verifier: PASS (324 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle (sh, 5s): exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 775 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    dune build @ref/runtest: exit 0
    check-clingo (sh, 151s): exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 775 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop (sh, 4s): exit 0 — check-noop: PASS (731 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood (sh, 17s): exit 0 — check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala (bash, 3s): exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak (bash, 25s): exit 0 — check-regelspraak: PASS — JUnit suite green (1008 tests, floor 173); 112 rgs golden(s) re-emit byte-identically (floor 80)
    check-toka-parity (bash, 48s): exit 0 —     Drools excludes by form:
    check-drools (bash, 257s): exit 0 —   JUnit 937 tests, floor 239  (the backend's own suite, green with a non-zero count)
    check-reftype-derivation (sh, 84s): exit 0 —   corpus-wide accounting: 91 unit-bearing annotations = 45 twistable + 46 exempt (the annotation IS the source) + 0 without a convertible sister + 0 at a §2.10 fourth-class branch (v0 derives no result unit) + 0 outside the twist table
    check-flogic (bash, 1s): exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf (bash, 11s): exit 0 — check-graaf: PASS
    check-scenario (bash, 7s): exit 0 — check-scenario: PASS — 19 scenario golden(s) (floor 7) regenerate 74 case file(s) (floor 31) and their ir.json byte-identically

Waar de laatste regel niet de PASS-regel is: gate 11 `check-toka-parity: PASS` (daarna de samenvatting, met als laatste
regel de lege lijst "Drools excludes by form:"); gate 12 `check-drools: PASS`, `played 230`, `channel floor 55
non-PLAYED goldens pinned by name; played matches the pinned 230`, `DRL-text branch played 191 (… pin 191)`; gate 13
`check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce
the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`, daarna zijn
boekhouding; gate 15 ook `check-graaf: route leg — the graph of the wire equals the graph of the source on all 112
goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)`. `dune build @ref/runtest`: `test_ref: all 1152
worked examples pass`. De toelating van de golden draaide de zestien gates ook (`scripts/admit.sh`, groen).

### README (`63702c13`)

`backends/drools/README.md`: de alinea over de leeg-toets in "Recorded: a condition that reads through a role" noemt nu
de subselectie van een vouw en de matrix van een kwantor, die de exectak sinds deze kaart ordent, en dat een leeg-toets
buiten een vouw de grens houdt; in "Recorded: a condition that puts no test on the subject" staat dat de eerste van de
drie grenzen (de matrix van geen vóór de schrijver) in de exectak gesloten is; en een eigen sectie "Recorded: a fold
whose subselection tests a value derived later": de drie oorzaken, de twee helften van de fix en waarom elk alleen niet
volstaat, golden en getuige, en wat blijft (open punten 1 tot 6).

### Afwijkingen van de kaart

1. De fix is meer dan een herziening: de exectak ordent een regel ook onder de schrijvers van wat een vouw in haar
   voorwaarde of waarde negatief leest. Reden: een regel met een vouw in een voorwaarde kan niet terugschrijven, en in
   een waarde beslist een herziening naar leeg niets op een staand doel (ch. 7.8); beide gemeten (18 vormen die met
   alleen de herziening fout blijven). Een benoemde weigering was de andere uitweg van (b); de volgorde laat de vormen
   spelen, dus is geen vorm geweigerd.
2. De vormruimte is breder dan de assen van de kaart: een eigen voorwaarde één rol ver (1v, het mechanisme zonder
   tweede rol), een bron die leeg wordt, een invoer op het doel, een kenmerk dat de vouw leest, andere vouwsoorten, de
   vouw als operand, de aggregatievergelijking, en een vergelijking tussen twee gevulde waarden (die alleen de herziening
   laat spelen).
3. De kwantor over twee rollen met een subselectie weigert de frontend als zin; zijn IR is met de hand afgeleid, en de
   loader weigert haar in beide takken met naam (`Kwantorvouw.bronPad`); ref en clingo spelen haar. Die dertig vormen
   zijn daarom een benoemde weigering, geen fout antwoord.
4. De klassekop van de getuige is in de tweede rode commit bijgewerkt (een nieuw bestand van deze kaart, geen bestaand
   testbestand).
5. Twee commitkoppen zijn langer dan 72 kolommen (`e781b077` 73, `269ef1df` 76); herschrijven vraagt een rebase, en
   die valt buiten het regime.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/IR-SPEC.md` ch. 5.5, "Recorded flank (measured 2026-09-23 …)", eerste bullet: de zin "(A fold in a rule's VALUE
  is re-evaluated and agrees in either order.)" vervangen door "(A fold in a rule's VALUE is re-evaluated, but that did
  not suffice: measured 2026-09-30, card `drools-exec-vouwfilter-latere-afleiding`, the exec-model branch's NO_LOOP
  dropped the revision when the rule's own write was the subject's last change, and in both Drools branches an
  ordering fold whose subselection emptied kept the value it had computed over the transient source, since leeg
  decides nothing on a standing target, RUNTIME-SEMANTICS ch. 7.8.)", en na de bullet toevoegen: "*Closed in the
  exec-model branch on 2026-09-30 for a test inside a fold (card `drools-exec-vouwfilter-latere-afleiding`, golden
  `jutterswet-zwaarste-vondst`): a rule is placed below the writers of what a fold in its condition or value reads
  negatively, in the polarity of WF-046, and a rule whose value folds with a subselection carries no NO_LOOP. A leeg or
  gevuld test outside a fold, and the DRL-text branch, keep the form described here.*"
- Geen rij in `spec/CONSTRUCTS.md`: RUNTIME-SEMANTICS ch. 5.1 (versheid), ch. 6 (strata) en ch. 7.8 schrijven het al
  voor; de exectak voldeed er niet aan en doet het nu voor een vouw.

### Open punten

1. De DRL-teksttak vuurt een vouw in een voorwaarde op de tijdelijke stand (v-leeg-vals-1-na, v-gevuld-vals-1-na,
   v-vergelijking-vals-1-na, x-aggvergelijking-vals-1-na en -2-na: `verwacht leeg, werkelijk 1`), houdt een maximum
   waarvan de subselectie leeg werd (x-alle-weg-1-na en -2-na: `verwacht leeg, werkelijk 20`; x-invoer-blijft-1-na:
   `verwacht 99, werkelijk 20`; x-gevolg-kenmerk-1-na: `kenmerk zwaar: verwacht afwezig, werkelijk aanwezig`), en weigert
   een document met een defaultsgroep. Een vouw in een waarde herziet zij wel (w-leeg-2-na, w-niet-kenmerk-2-na,
   x-plus-2-na: `PLAYED 1`). Ongewijzigd door deze kaart.
2. Een leeg- of gevuld-toets buiten een vouw ("indien zijn afgeleide dag leeg is") houdt de volgordegrens van de
   README-sectie over een voorwaarde over een rol; niet in deze vormruimte. Eigen gevolgtrekking, niet gebouwd en niet
   gemeten: dezelfde volgorde, uitgebreid naar een toets buiten een vouw, zou haar dekken.
3. Een kwantor over twee rollen met een subselectie: de frontend weigert de zin (`de 'die/dat'-deelselectie 'aan de
   volgende voorwaarde voldoen: …' wordt niet omgezet en zou stil wegvallen: …`), de loader de IR in beide takken
   (`Kwantorvouw.bronPad (a multi-step quantifier range (WF-108 (c)); the accumulate source carries one join role)`);
   ref en clingo spelen de dertig met de hand afgeleide documenten.
4. Een telling over twee rollen met een subselectie ("het aantal vondsten van de berging van de Vondst die …") weigert
   de frontend (`… zonder bereik (geen relatie verbindt Vondst met Vondst): a single-argument fold with an elementType
   carries its range (sourceRole or sourcePath) under every kind but total: the range is written, never derived`); in
   Drools niet gemeten.
5. De volgorde kent de regels en de groepen als schrijvers; een verdeling, een objectcreatie of een feitcreatie die de
   getoetste waarde schrijft, ziet zij niet. Niet gemeten of een vouw over zo'n waarde dan fout antwoordt; voor een
   vouw in een waarde werkt de herziening los van de volgorde.
6. De temporele som over een set (`tijdreeksSomAlleSet`) met een subselectie houdt NO_LOOP; niet gemeten. Evenzo een
   element dat de bron verlaat doordat zijn lid leeg wordt (de `!= null`-bewaking van `vouw`), zonder subselectie: die
   regel houdt NO_LOOP; niet gemeten.
7. De begeleiders van een regel (§6.5 deling door leeg, §6.6 wortel, leeg startpunt, gevuurd) krijgen geen salience en
   volgen een lager gezette regel niet, zoals al gold voor een regel die de stratificatie verlaagt; niet gemeten of een
   begeleider van een vouw in een voorwaarde op de tijdelijke stand een fout vastlegt.
8. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; de pin moet na het landen mee.

### Commits

    7a687922 claim: drools-exec-vouwfilter-latere-afleiding
    9fb410f1 VouwfilterLatereAfleidingWitnessTest: subselectie op later, rood
    2f4a24e3 VouwfilterLatereAfleidingWitnessTest: drie rijen erbij, rood
    e781b077 Exectak: een vouw met een subselectie herziet en wacht op haar schrijvers
    269ef1df golden jutterswet-zwaarste-vondst: subselectie op een later afgeleide waarde
    1eb07f5c voorstel gate 12: jutterswet-zwaarste-vondst speelt in de exectak
    63702c13 backends/drools/README: een vouw met een subselectie op later

en de commit die dit Result in de kaart zet en haar naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.
