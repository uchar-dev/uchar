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

**Groen (ná).**

```
drempelvangst   --drl: PLAYED 1                      exec: PLAYED 1
lichterloon     --drl: PLAYED 1                      exec: PLAYED 1
vondstrecht     --drl: PLAYED 1                      exec: PLAYED 1
tariefstelling  --drl: EXCLUDE drl-transpiler MARKER: regel: aanslag van een jutter — TODO expressie: als aftrek groter dan belasting dan 0,00 anders belasting min aftrek
tariefstelling   exec: PLAYED 3
```

Geen IR, geen case en geen verwachting is aangeraakt.

**De vierde golden haalt PLAYED niet, en dat is een botsing binnen deze
kaart.** `anchor/jutterswet-tariefstelling` draagt in de body van zijn
Let-keten een `Expr.IfThenElse`. De marker is verschoven van de eerste
resultaatbinding naar die knoop: de binding is gelowerd, de knoop erboven
niet. `Expr.IfThenElse` staat in Out of scope, met als grond dat de
RegelSpraak-emitter hem niet produceert — en die grond klopt hier: deze
golden is een anchor zonder `bron.rgs`, dus hij valt buiten de Goal ("valide
RegelSpraak-bron"), terwijl de Acceptance hem wel bij naam noemt. Ik heb de
Out-of-scope-regel laten winnen en de knoop niet gelowerd. Dezelfde grens
raakt `synth3-conditielezing`, `synth3-drempelheffing` en
`synth3-takbarriere` (elk precies één `IfThenElse`). Wil de maintainer die
vier laten spelen, dan is daar één kaart voor nodig — "de DRL-tekst-tak
lowert ch. 10.2" — met de WF-077-operanden en hun gardering als kern.

**Wat de zestien Let-dragende goldens nu doen met `--drl`.** Zes spelen:
`jutterswet-drempelvangst`, `jutterswet-kinderweging`,
`jutterswet-lichterloon`, `jutterswet-vondstrecht`, `synth3-barrierebezit`
en `synth3-operandvolgorde` — waar alle zestien tevoren op de `Let`
markeerden. Vier weigeren nu op `IfThenElse` (hierboven), twee op de ch.-7
prioritized-defaults (de luide weigering van die extensie blijft ongemoeid),
drie op een loaderweigering (list/sum-typen).

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

Mutatieproeven, feitelijk gebouwd, gedraaid en hersteld:

```
barriere weg (altijd schrijven)           -> 5 rood, o.a. een_lege_behoefte…, het_lege_doel…
Let altijd leeg (nooit schrijven)         -> 11 rood, elke gevulde pin erbij
prelude-gardering weg                     -> 1 ERROR: "Breuk deling door 0" in een_lege_buitenbehoefte…
companionwandeling stopt bij de Let       -> 1 rood: "verwacht inconsistent …, werkelijk consistent"
companion zonder behoeftepoort            -> 1 rood: "verwacht consistent, werkelijk inconsistent"
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
385 tests, vloer 239. Gate 13: de DRL-tekst-tak speelde 19 goldens vóór en
21 ná, vloer 13; 108 twists over 22 goldens, vloer 74; 0 survivors. Beide
tellingen groeien boven hun vloer, dus geen scriptwijziging, en geen
beschermde scriptpin bleek stale.

**Veertien gates**, na de laatste commit, alle exit 0:

```
self-contained: 0   ir-schema: 0   verifier: 0   case-schema: 0
oracle: 0           clingo: 0      noop: 0       rood: 0
catala: 0           regelspraak: 0 toka-parity: 0 drools: 0
reftype-derivation: 0              flogic: 0
```

**May touch.** `git diff --name-only main...HEAD` geeft exact:
`DrlTranspiler.java`, `CaseRunner.java`, de nieuwe
`LetResultaatTakWitnessTest.java`, `DrlEenheidsplekWitnessTest.java` en de
verplaatsing van deze kaart. `transpiler/kern/` is NIET aangeraakt: er bleek
geen werkelijk gedeelde binding- of eenheidslogica nodig — de binding leeft
in de DRL-tekst en `EffectieveEenheid` kon de keten al aan zodra zij haar
index kreeg.

**Bewust buiten scope gebleven.** `Expr.IfThenElse` (zie hierboven); een
`Let` in een prioritized-defaultswaarde, waar de luide weigering van de hele
extensie blijft staan; list-, struct- en function-body-Lets; elke frontend-
of wirewijziging; corpusverwachtingen.

**Register.** Niets voor `spec/PLAN.md` of `spec/CONSTRUCTS.md`: dit is
takconformiteit, geen spec-wijziging, en rij 24 doet geen uitspraak over de
DRL-tekst-tak, dus niets erin is stale geworden. Wil de maintainer de
dekking toch laten meelopen, dan past één zin achter de getuigen van rij 24:
"sinds 2026-09-18 spelen `jutterswet-lichterloon` en `jutterswet-vondstrecht`
ook in de DRL-tekst-tak van `backends/drools`, zodat de constructie in uchar
vier uitvoerders heeft."
