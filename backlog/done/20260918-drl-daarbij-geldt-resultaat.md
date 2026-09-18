# drl-daarbij-geldt-resultaat: classic DRL draagt resultaatbindings

Goal: De classic-DRL-tak lowert de door de RegelSpraak-frontend
geproduceerde `Let`/`BinderRef`-keten voor resultaatzijdige en
doorgekoppelde `Daarbij geldt`-variabelen. Daarmee speelt valide
RegelSpraak-bron die in beide oorspronkelijke Drools-armen werkte ook
weer in beide uchar-armen, zonder de binding als tekstsubstitutie te
vereenvoudigen en zonder de normatieve leegbarrière te verliezen.

Evidence:

- De uitputtende vergelijking van 624 fixture-arraygevallen vond
  precies vier oude dual-backendgevallen in deze familie:
  `PHASE_11` (een variabele gelezen in voorwaarde én resultaat; een
  latere variabele die een eerdere leest) en `PHASE_11_ABS`
  (nul-gardering; een deling pas na de gardering). Beide oorspronkelijke
  manifests waren `LOWERED` en de differentiaal vuurde beide KieBases.
- De huidige publieke brongetuigen
  `jutterswet-drempelvangst`, `jutterswet-lichterloon`,
  `jutterswet-vondstrecht` en
  `anchor/jutterswet-tariefstelling` spelen in exec-model. Classic DRL
  markeert de eerste resultaatbinding met `TODO expressie: <naam> is ...`.
- De frontend bouwt resultaatbindings als gewone `Expr.Let`- en
  `Expr.BinderRef`-knopen. `CaseRunner.drlTak` geeft de geladen
  `lets()` nu niet door en `DrlTranspiler` initialiseert
  `EffectieveEenheid` met een lege let-index.

Acceptance:

- Rood-eerst: leg de huidige marker vast op minstens de eenvoudige en
  de geketende vorm:

  ```sh
  mvn -q -o -f backends/drools/pom.xml package
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/jutterswet-drempelvangst/ir.json \
    corpus/golden/jutterswet-drempelvangst/case*.json
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/anchor/jutterswet-tariefstelling/ir.json \
    corpus/golden/anchor/jutterswet-tariefstelling/case*.json
  ```

- `DrlTranspiler` ontvangt de geladen let-index en lowert
  frontend-produceerbare `Let`/`BinderRef`-bomen met lexicale scope:
  een eerdere binder is leesbaar in een latere definitie en in het
  resultaat; herhaald lezen levert dezelfde behoefte; een
  binnenscope kan niet buiten zijn scope lekken. Naamgelijkheid alleen
  mag geen capture of globale DRL-variabelenbotsing veroorzaken.
- De lowering volgt RUNTIME-SEMANTICS ch. 10.6 call-by-need voor deze
  produceerbare subset: een ongelezen behoefte wordt niet geëvalueerd,
  een gelezen lege behoefte maakt de omhullende Let leeg, en een
  gardering voorkomt dat een deling in de niet-betreden route wordt
  geëvalueerd. Foutcompanions, freshness/reactiviteit en effectieve
  eenheden wandelen door de binding heen; zij mogen niet verdwijnen
  doordat alleen de waardetekst wordt ingevoegd.
- Voeg dual-branch JUnit-getuigen toe voor de vier gemeten vormen:
  binder in conditie én resultaat, latere binder leest eerdere,
  nul-gardering vuurt niet, en de gevulde delingsroute levert de
  absolute oorspronkelijke uitkomst. Pin daarnaast leeg versus gevuld,
  zodat een altijd-schrijven en een nooit-schrijven implementatie elk
  rood wordt.
- De vier genoemde goldens eindigen zonder vlag én met `--drl` in
  `PLAYED`, met ongewijzigde verwachtingen. De gate-12
  `EXPECTED_PLAYED`-telling verandert niet: de exec-model-tak speelde
  ze al.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en alle veertien gates
  zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`,
gedeelde helpers onder
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/kern/`
alleen voor werkelijk gedeelde binding/effectieve-eenheidlogica,
`backends/drools/src/test/**`, deze kaart en bij afronding haar
verplaatsing binnen `backlog/`. Een scriptwijziging is niet voorzien;
een onverwacht stale beschermd scriptpin wordt als exact voorstel in
het `Result:` gezet, niet opportunistisch verbreed.

Out of scope: `Expr.IfThenElse` (de RegelSpraak-emitter produceert die
knoop niet), een Let in prioritized-defaultswaarden (classic DRL draagt
die extensie als geheel nog niet), list/struct/function-body Lets,
frontend- of wireherontwerp, en corpusverwachtingen. Condition-only
variabelen die de frontend al zonder Let naar beide takken brengt mogen
niet regresseren. Deze kaart deelt `DrlTranspiler.java` met de andere
Drools-pariteitskaarten van 2026-09-18 en wordt serieel gepland.

Result: De classic-DRL-tak lowert de ch.-4.10 `Let`/`BinderRef`-keten van het
§11-variabelendeel als een echte binding. `CaseRunner.drlTak` geeft
`loaded.lets()` door en `DrlTranspiler` bouwt zijn `EffectieveEenheid`
daarmee; zonder die index valt een binderlezing terug op haar eigen
wire-annotatie, precies de herhaling die ch. 2.10 niet wil lezen.

**De vorm.** Een gelezen behoefte wordt eenmaal geforceerd in een
consequence-lokale mvel-variabele (`$behoefteN`, een prelude-regel), en de
`Let` zelf rendert als de leegbarriere daaroverheen:
`($behoefteN == null ? null : <body>)`. Clausule 1 is die gedeelde variabele
— elke lezing is dezelfde variabele, zodat eenheden, foutcompanions en de
freshness-watch over dezelfde evaluatie spreken; clausule 2 zit op de knoop
zelf, zodat een zusterbinding onder dezelfde `plus` haar eigen antwoord
houdt waar een consequence-brede garde de hele waarde zou legen; clausule 3
is het ontbreken van prelude en barriere bij een ongelezen behoefte. Elke
prelude-regel draagt de conjunctie van de omhullende behoeften, want
`rsGedeeld` door nul gooit en een throw in een consequence breekt de hele
fire af. Emitted voorbeeld (`jutterswet-lichterloon`, één behoefte, twee
lezingen):

```
    $behoefte1 = rsMaal($subject.getVrachttal(), $subject.getEenheidsloon());
    if (!java.util.Objects.equals($subject.lichterloon, rsWaarde(($behoefte1 == null ? null : rsPlus($behoefte1, rsMaal($behoefte1, $subject.getSpoedtal()))))))
```

Gemeten grens, niet verzwegen: ch. 10.6 leest van links naar rechts, dus een
§6.5-plek die VOOR de barriere-lezing staat draait in ref wel en hier niet.
Dat is een gemiste melding, nooit een verzonnen; de companions hieronder
zijn met dezelfde poort gebouwd, zodat tak en companion het eens blijven.

**Wandelingen.** `verzamelEigenReads`, `verzamelNavRefs`,
`verzamelFilterKenmerkReads`, `verzamelDelingen`, `verzamelWortels` en
`kanBreukOpleveren` dalen in de binding af. De twee §6.5/§6.6-companion-
wandelingen dragen per plek de behoeften waarachter zij staat, en zij dalen
in de waarde van een behoefte alleen af wanneer de body die binder leest
(clausule 3). Een behoefte die niet tot een herleesbare poort ontleedt laat
de companion vallen — dezelfde scope-grens die een literal- of
parameteroperand daar al draagt. Een binderlezing binnen een aggregatie en
een `Let` buiten de scalaire resultaatwaarde worden bij naam geweigerd, niet
via een kale compile-fout.

**Rood-eerst, letterlijk (vóór).**

```
--drl drempelvangst  : EXCLUDE drl-transpiler MARKER: regel: drempelaandeel van een jutter — TODO expressie: de vangst is het drempelgewicht van de Jutter.
--drl tariefstelling : EXCLUDE drl-transpiler MARKER: regel: aanslag van een jutter — TODO expressie: ink is het inkomen van de Jutter
--drl lichterloon    : EXCLUDE drl-transpiler MARKER: regel: lichterloon van een lichterbeurt — TODO expressie: het grondloon is het vrachttal van de Lichterbeurt maal het eenheidsloon van de Lichterbeurt.
--drl vondstrecht    : EXCLUDE drl-transpiler MARKER: regel: vondstrecht van een vondst — TODO expressie: de toeslag is de meerprijs van de Vondst.
zonder vlag          : PLAYED 1 / PLAYED 3 / PLAYED 1 / PLAYED 1   (exec speelde ze al)
```

**Groen (ná ronde 1).**

```
drempelvangst   --drl: PLAYED 1                      exec: PLAYED 1
lichterloon     --drl: PLAYED 1                      exec: PLAYED 1
vondstrecht     --drl: PLAYED 1                      exec: PLAYED 1
tariefstelling  --drl: EXCLUDE drl-transpiler MARKER: regel: aanslag van een jutter — TODO expressie: als aftrek groter dan belasting dan 0,00 anders belasting min aftrek
tariefstelling   exec: PLAYED 3
```

**De vier takgoldens, vóór en ná ronde 2.** Vóór is gemeten met de
transpiler van ronde 1 (de Let-lowering zonder ch. 10.2):

```
tariefstelling  vóór --drl: EXCLUDE drl-transpiler MARKER: regel: aanslag van een jutter — TODO expressie: als aftrek groter dan belasting dan 0,00 anders belasting min aftrek
                 ná --drl: PLAYED 3        exec vóór en ná: PLAYED 3
conditielezing  vóór --drl: EXCLUDE drl-transpiler MARKER: regel: heffing van een jutter — TODO expressie: 5,00 boven nul en 9,00 daaronder
                 ná --drl: PLAYED 1        exec vóór en ná: PLAYED 1
drempelheffing  vóór --drl: EXCLUDE drl-transpiler MARKER: regel: heffing van een jutter — TODO expressie: de basisheffing, verhoogd met de volle heffing zodra de drempel is overschreden
                 ná --drl: PLAYED 4        exec vóór en ná: PLAYED 4
takbarriere     vóór --drl: EXCLUDE drl-transpiler MARKER: regel: heffing van een jutter — TODO expressie: de basisheffing, boven de drempel zonder opslag en daaronder met opslag
                 ná --drl: PLAYED 1        exec vóór en ná: PLAYED 1
```

Geen IR, geen case en geen verwachting is aangeraakt.

**Besluit 2026-09-18 (maintainer): ch. 10.2 hoort bij deze kaart.** De ronde
hierboven eindigde met een botsing binnen de kaart:
`anchor/jutterswet-tariefstelling` staat bij name in de Acceptance, maar
draagt in de body van zijn Let-keten een `Expr.IfThenElse`, die in Out of
scope stond. De marker was daarmee alleen verschoven — van de eerste
resultaatbinding naar die knoop. De maintainer heeft de botsing beslecht in
de richting die de Acceptance eiste: de Out-of-scope-regel vervalt voor deze
kaart en de als-dan-anders is in een tweede ronde direct meegenomen. Dezelfde
grens raakte `synth3-conditielezing`, `synth3-drempelheffing` en
`synth3-takbarriere`; alle vier spelen nu.

**De ch.-10.2-lowering.** De garde is één term door de GEDEELDE
`rsAggVergelijk` — dezelfde helper als de ch.-10.2-lowering van de
exec-model-tak — zodat beide takken één tabel lezen voor de leegcellen (§5.3
Tabel 37 / §5.6 Tabel 44 / Tabel 33) en een exacte Breuk exact vergelijken.
De keuze zelf is een mvel-ternair: precies de gekozen arm wordt geëvalueerd,
dus een deling in de niet-betreden arm draait niet. De weigeringen spiegelen
die van de exec-tak één op één: geen vergelijking, geen numerieke operator,
een niet-getalachtige operand, een operand die niet lowert, en een
vergelijking die een §5.1-Tabel-32-herschaling zou vergen, die
`rsAggVergelijk` niet toepast. Geen halve lowering.

**De lege conditie, gemeten vóór er iets gepind werd.** WF-077 laat in een
garde alleen binders, parameters en literals toe (met rekencombinatoren
daarover), en geen van de drie is leeg op de plek waar hij gelezen wordt: een
binderlezing levert een gevulde waarde of beëindigt eerst haar `Let`
(ch. 10.6 clausule 2), en de enige andere route — een parameter die de case
niet levert — is in ref een FAIL-FAST fout, niet leeg. Letterlijk gemeten op
een eigen mini-invoer (één Let, één garde over een niet-geleverde parameter):

```
ref    : EVALUATION FAULT: parameter "drempel" is unbound in this case (binding is fail-fast, ch. 5.7)
clingo : MISMATCH: J1.keuze is underived (leeg), expected 200
Drools : beide takken spelen (de ongebonden global leest null, de garde wordt onwaar)
```

Een verifier-schone, ref-draaibare document kan dus geen lege garde-operand
aanbieden: dat is precies waar WF-077 voor is. De gardering is desondanks
conform gebouwd — via `rsAggVergelijk`, dus refs eigen tabel — zodat een
document dat er toch een aanbiedt refs antwoord krijgt en geen mvel-toeval.
De Drools-afwijking op de niet-geleverde parameter staat hier als meting: zij
is identiek in BEIDE takken, hoort bij de parameterbinding en niet bij
ch. 10.2, en geen corpuscase laat een parameter weg (gate 5 zou in ref
faulten). Niet in scope van deze kaart, wel gemeld.

**Wat een takknoop toevoegt, en waar het werk zat.** Zonder ch. 10.2 is "wordt
deze binder gelezen" een constante; met een takknoop niet meer. De leesvraag
beantwoordt nu een kleine tralie — ALTIJD, NOOIT, of onder precies één
takgarde — en een behoefte die alleen één arm leest wordt onder die garde
geforceerd, met dezelfde garde op haar barrière, zodat de niet-betreden arm
haar noch berekent noch leegmaakt. Dat is de flank die
`synth3-takbarriere` pint. Twee VERSCHILLENDE takgardes op één lezing worden
bij naam geweigerd in plaats van geraden: de conjunctie die dat vergt heeft
geen garantie dat haar operanden op het moment van de prelude al toegekend
zijn. Emitted vorm (`synth3-takbarriere`):

```
    $behoefte1 = $subject.getGrondslag();
    $behoefte2 = ($behoefte1 != null && !(rsAggVergelijk($behoefte1, drempel, ">"))) ? $subject.getOpslag() : null;
    … ($behoefte1 == null ? null : ((!(rsAggVergelijk($behoefte1, drempel, ">"))) && $behoefte2 == null ? null :
        (rsAggVergelijk($behoefte1, drempel, ">") ? basisheffing : rsPlus(basisheffing, $behoefte2))))
```

**De foutcompanions dragen de takgarde naast de behoeftepoort.** Een companion
is een eigen regel en ziet geen consequence-lokale, dus een binder in die
garde staat voor de HERLEZING van zijn eigen behoefte; wat niet herleesbaar
is laat de companion vervallen in plaats van hem verkeerd te garderen. De
poort reist daarom als onveranderlijke waarde mee met de wandeling, met de
binderomgeving erin, zodat zusters elkaars garde nooit erven. Emitted
companion van de getuige:

```
    $subject : Jutter( inkomen != null, rsAggVergelijk(inkomen, new java.math.BigDecimal("10.00"), ">"),
                       teller != null, takdeler == null )
```

**Gemeten en niet weggepind — een ref/clingo-verschil dat deze kaart niet mag
repareren.** Op een §6.5-plek in de NIET-betreden arm meldt de clingo-backend
inconsistent waar ref consistent zegt (`MISMATCH: expected consistent, but
the model carries 1 inconsistent fact(s)`); haar §6.5-hek is niet
tak-volgend. Beide Drools-takken volgen ref. `backends/clingo` valt buiten de
May touch van deze kaart, dus de vorm staat als getuige met ref als arbiter
en wordt GEEN corpusgolden — gate 6 zou hem terecht afwijzen. Dit verdient
een eigen kaart: "het §6.5-hek van de ASP-vertaling volgt de ch.-10.2-tak".

**Wat de zestien Let-dragende goldens nu doen met `--drl`.** TIEN spelen —
`anchor/jutterswet-tariefstelling`, `jutterswet-drempelvangst`,
`jutterswet-kinderweging`, `jutterswet-lichterloon`,
`jutterswet-vondstrecht`, `synth3-barrierebezit`, `synth3-conditielezing`,
`synth3-drempelheffing`, `synth3-operandvolgorde` en `synth3-takbarriere` —
waar alle zestien tevoren op de `Let` markeerden. Wat rest zijn twee
ch.-7-prioritized-defaultsweigeringen (`jutterswet-bergingstoeslag`,
`jutterswet-tariefrecht-catala`; de luide weigering van die extensie blijft
ongemoeid) en VIER loaderweigeringen (`jutterswet-kamerhuishouden-catala` op
`functionDefs`, `jutterswet-kinderlet` en `jutterswet-postenheffing` op
`ListType`, `synth3-letbeslag` op `SumType`). De ronde-1-versie van deze
alinea telde drie loaderweigeringen; dat was een telfout, hier gecorrigeerd.

**Getuigen.** `backends/drools/src/test/java/io/github/yhavinga/regelspraak/
cases/LetResultaatTakWitnessTest.java`, 16 tests, elk door BEIDE takken.
Document en cases zijn hier geschreven, geen corpusbestand aangeraakt, en
eerst door de arbiters gehaald: verifier PASS, ref PASS en clingo PASS op
alle zeven invoeren, beide Drools-takken PLAYED 7 — het volle differentiële
vierkant op dezelfde absolute waarden.

De vier gemeten vormen van de kaart:
`een_variabele_in_voorwaarde_en_resultaat_komt_in_beide_takken_aan` met zijn
niet-vurende flank `de_voorwaarde_die_niet_houdt_laat_het_doel_leeg`;
`een_latere_binder_leest_de_eerdere`;
`de_nulgardering_houdt_de_deling_uit_de_niet_betreden_route`;
`de_gevulde_delingsroute_levert_de_oorspronkelijke_uitkomst`. Daarnaast
`een_lege_buitenbehoefte_houdt_de_binnenste_deling_ongeforceerd`,
`een_lege_behoefte_maakt_de_hele_let_leeg_terwijl_dezelfde_maal_zonder_let_nul_rekent`,
`de_gevulde_invoer_schrijft_wel_en_beide_vormen_komen_uit_op_hetzelfde`,
`het_lege_doel_is_observeerbaar_afwezig_in_beide_takken`,
`dezelfde_binder_twee_keer_gelezen_levert_dezelfde_behoefte`,
`naamgelijkheid_schaduwt_binnen_en_lekt_niet_naar_buiten`,
`een_zusterbinding_houdt_haar_eigen_antwoord_als_de_buur_leegloopt` met
`de_zusterbinding_rekent_gevuld_gewoon_mee`, de twee companionflanken
`een_deling_door_leeg_binnen_een_behoefte_meldt_zich_nog_steeds` en
`een_deling_achter_een_lege_behoefte_meldt_zich_juist_niet`, en de
non-regressieflank `een_voorwaarde_only_variabele_blijft_zonder_let_werken`.

Ronde 2 voegde er acht ch.-10.2-getuigen aan toe, dezelfde klasse, dezelfde
dubbeltak-naad: `de_danstak_wordt_betreden_en_de_anderstak_niet` met zijn
tegenhanger `de_anderstak_wordt_betreden_en_de_danstak_niet`;
`een_lege_behoefte_in_de_takgarde_eindigt_de_let_voor_de_keuze`;
`een_ifthenelse_in_een_letbody_leest_een_eerdere_behoefte` (de
tariefstellingvorm); `een_behoefte_die_alleen_de_niet_betreden_tak_leest_
wordt_niet_geforceerd` met `dezelfde_behoefte_telt_wel_mee_zodra_haar_tak_
betreden_wordt`; en de twee companionrichtingen
`een_deling_in_de_betreden_tak_meldt_zich` en
`dezelfde_deling_in_de_niet_betreden_tak_meldt_zich_niet`. Het uitgebreide
document is opnieuw eerst gearbitreerd: verifier PASS, ref PASS op alle tien
invoeren, clingo PASS op negen (de tiende is de gemelde tak-onvolgendheid
hierboven), beide Drools-takken PLAYED 10.

Mutatieproeven, feitelijk gebouwd, gedraaid en hersteld (tellingen over de 24
tests van de eindstand):

```
barriere weg (altijd schrijven)           ->  6 rood, o.a. een_lege_behoefte…, het_lege_doel…
Let altijd leeg (nooit schrijven)         -> 18 rood, elke gevulde pin erbij
prelude-gardering weg                     ->  1 ERROR: "Breuk deling door 0" in een_lege_buitenbehoefte…
companionwandeling stopt bij de Let       ->  2 rood: "verwacht inconsistent …, werkelijk consistent"
companion zonder behoeftepoort            ->  1 rood: "verwacht consistent, werkelijk inconsistent"
ch. 10.2: takken omgewisseld              ->  6 rood, beide takpins erbij
companion zonder takgarde                 ->  1 rood: dezelfde_deling_in_de_niet_betreden_tak_meldt_zich_niet
takgarde weg uit prelude en barriere      ->  1 rood: een_behoefte_die_alleen_de_niet_betreden_tak_leest…
```

`DrlEenheidsplekWitnessTest` bouwt nu langs dezelfde weg als de tak (model
plus Let-index). Zonder die wijziging werd hij rood op
`jutterswet-vondstrecht`: de verdraaide annotatie op een binderlezing
verplaatste de emitted tekst. Zijn vangst groeide gemeten van 9 naar 21
goldens onder de mutant `EffectieveEenheid.van` → `e.type()`, en zijn
verouderde alinea daarover is bijgesteld.

**Tellingen.** Gate 12 meldt `played matches the pinned 148`:
`EXPECTED_PLAYED` is ONGEWIJZIGD, zoals de kaart eist — het exec-kanaal is
wat gate 12 telt en dat bewoog niet. De backend-suite groeide van 369 naar
393 tests, vloer 239. Gate 13: de DRL-tekst-tak speelde 19 goldens vóór deze
kaart en 21 na ronde 1, vloer 13; ná ronde 2 nog steeds 21, en dat is geen
stilstand maar een eigenschap van de harnas-vraag: de vier takgoldens dragen
samen NUL eenheiddragende annotaties (gemeten), dus er valt voor de
verdraaiing niets aan te raken en zij kunnen die teller niet bewegen. Verder
onveranderd: 108 twists over 22 goldens, vloer 74; 0 survivors. Alle
tellingen zitten boven hun vloer, dus geen scriptwijziging, en geen
beschermde scriptpin bleek stale.

**Veertien gates**, na de laatste commit van elke ronde, alle exit 0:

```
self-contained: 0   ir-schema: 0   verifier: 0   case-schema: 0
oracle: 0           clingo: 0      noop: 0       rood: 0
catala: 0           regelspraak: 0 toka-parity: 0 drools: 0
reftype-derivation: 0              flogic: 0
```

**May touch**, na beide rondes opnieuw gedraaid.
`git diff --name-only main...HEAD` geeft exact: `DrlTranspiler.java`,
`CaseRunner.java`, de nieuwe `LetResultaatTakWitnessTest.java`,
`DrlEenheidsplekWitnessTest.java` en de verplaatsing van deze kaart.
`transpiler/kern/` is NIET aangeraakt: er bleek geen werkelijk gedeelde
binding- of eenheidslogica nodig — de binding leeft in de DRL-tekst, de
takgarde gaat door de al gedeelde `rsAggVergelijk` en `Operatoren`, en
`EffectieveEenheid` kon de keten al aan zodra zij haar index kreeg.

**Bewust buiten scope gebleven.** Een `Let` in een
prioritized-defaultswaarde, waar de luide weigering van de hele extensie
blijft staan; list-, struct- en function-body-Lets; elke frontend- of
wirewijziging; corpusverwachtingen; het §6.5-hek van de ASP-vertaling (de
gemelde tak-onvolgendheid van clingo) en de fail-fast parameterbinding, beide
in backends die deze kaart niet mag raken. `Expr.IfThenElse` stond in ronde 1
buiten scope en is in ronde 2 op maintainersbesluit alsnog gelowerd.

**Register.** Niets voor `spec/PLAN.md` of `spec/CONSTRUCTS.md`: dit is
takconformiteit, geen spec-wijziging, en rij 24 doet geen uitspraak over de
DRL-tekst-tak, dus niets erin is stale geworden. Wil de maintainer de
dekking toch laten meelopen, dan past één zin achter de getuigen van rij 24:
"sinds 2026-09-18 spelen `jutterswet-lichterloon` en `jutterswet-vondstrecht`
ook in de DRL-tekst-tak van `backends/drools`, zodat de constructie in uchar
vier uitvoerders heeft." Na ronde 2 luidt die zin ruimer: "sinds 2026-09-18
lowert ook de DRL-tekst-tak van `backends/drools` de ch.-4.10-cluster
(`Let`, `BinderRef` en — met dezelfde gedeelde vergelijkingshelper als de
exec-tak — `IfThenElse`), zodat tien van de zestien Let-dragende goldens in
beide Drools-armen spelen." Voor `spec/PLAN.md` valt niets te melden. Eén
waarneming verdient wél een eigen kaart, en die is hierboven beschreven: het
§6.5-hek van de ASP-vertaling volgt de ch.-10.2-tak niet, waardoor ref en
clingo verschillen op een deling in de niet-betreden arm.
