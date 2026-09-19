# drl-let-meldplek: de §9.5-kanaalreden binnen een Let-body

Gemeten (asp-ch102-kaart, 2026-09-19, eigen minimale probes): de
DRL-tekst-tak deponeert geen §9.5-kanaalreden voor een §6.5-plek
BINNEN een `Let`-body. Hetzelfde document zónder de `Let` speelt in
beide takken; mét één `Let` eromheen speelt exec en faalt de DRL-tak
op de ontbrekende `inconsistent`-verwachting (`verwacht inconsistent
(heffing van een jutter @ J4), werkelijk consistent`). Het gat is de
binding, niet de tak: de meldplek-machinerie bestaat, ze wordt onder
een `Let` niet bereikt.

Vier van de vijf `synth3-*`-goldens (takhek, operatortak,
armverdeling, armvangst) wachten hierop om ook in de DRL-tak te
spelen; hun redenen staan bij naam in gate 12's redenblok
(`20260919-asp-ch102-vervolg` §DRL).

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de probe-documenten als getuigen (zonder/met `Let`),
  DRL-uitkomst gepind vóór en ná.
- (b) De DRL-tak deponeert de §9.5-reden ook onder een `Let`-body;
  dual-branchgetuige; mutatieproef.
- (c) Corpussweep beide takken: de synth3-goldens die hierdoor gaan
  spelen bij naam; kanaaltellings-gevolg (gate 12) als gemandateerd
  beschermd-padvoorstel. Alle veertien gates groen.

May touch: `backends/drools/**` (DrlTranspiler + tests),
`scripts/check-drools.sh` uitsluitend kanaaltelling (voorstel),
kaartverplaatsing. NB: serieel met andere DrlTranspiler-kaarten
(drl-geneste-let-duw, drools-bereikafleiding-deelt).

Result: geland. De §6.5/§6.6-companion van de DRL-tekst-tak volgt nu de
`Let`-binding: een foutplek waarvan een operand een binder is staat voor de
eigen herleesbare lezing van haar behoefte, precies de substitutie die de
takgarde-kant al deed. Drie van de vijf `synth3-*`-goldens gaan in die tak van
FAIL naar PLAYED (127 -> 130); exec beweegt niet (159, byte-identieke sweep),
dus gate 12's `EXPECTED_PLAYED` blijft staan en geen beschermd script is
aangeraakt.

## Rood-eerst: de twee probes, letterlijk vóór en ná

Twee documenten die alleen in de `Let` verschillen, hier nagebouwd als eigen
fixtures (niet uit het corpus gelezen), met één jutter J4, `inkomen` 5,00 en een
LEGE `restpost`, en de verwachting `heffing` leeg + `inconsistent`:

    P1  heffing := zijn inkomen gedeeld door zijn restpost
    P2  heffing := Let k = zijn inkomen in (k gedeeld door zijn restpost)

                 verifier  ref            clingo         exec       --drl
    P1 vóór      PASS      PASS (2 exp)   PASS (2 exp)   PLAYED 1   PLAYED 1
    P2 vóór      PASS      PASS (2 exp)   PASS (2 exp)   PLAYED 1   FAIL: verwacht
                                                                    inconsistent,
                                                                    werkelijk consistent
    P1 ná        PASS      PASS (2 exp)   PASS (2 exp)   PLAYED 1   PLAYED 1
    P2 ná        PASS      PASS (2 exp)   PASS (2 exp)   PLAYED 1   PLAYED 1

Het gemeten gat zit niet in een gemiste garde maar in een ONTBREKENDE REGEL: de
DRL van P2 bestond vóór de fix uit één regel, zonder companion; die van P1 uit
twee, met `Jutter( inkomen != null, restpost == null )` erbij. Ná de fix draagt
P2 dezelfde companion, met het behoeftehek en het deeltalhek naast elkaar
(`inkomen != null, inkomen != null, restpost == null` — de behoefte en het
deeltal zijn hier dezelfde lezing, dus het hek staat er tweemaal; redundant in
DRL, verder onschadelijk).

De vijf wachtende goldens, DRL-tak vóór en ná:

    golden               vóór                                    ná
    synth3-takhek        FAIL case2: verwacht inconsistent …     PLAYED 2
    synth3-operatortak   FAIL case2: verwacht inconsistent …     PLAYED 2
    synth3-armvangst     FAIL case2: verwacht inconsistent …     PLAYED 2
    synth3-armverdeling  FAIL case1 (waarden) + case2 (melding)  FAIL case1 (waarden)
    synth3-armwissel     EXCLUDE drl-transpiler MARKER           EXCLUDE (ongewijzigd)

## De fix, en waarom de route de meldplek miste

`plekGuards` bouwde de companion-conditie in twee stukken met twee verschillende
scopes. De ch.-10.6-behoeftehekken en de ch.-10.2-takgardes werden gebouwd
BINNEN een binderomgeving (`Acc.binderPatroon`: binder -> de herleesbare lezing
van zijn behoefte, want een companion is een aparte regel en ziet geen enkele
lokale variabele van de consequentie). De PLEK zelf — deeltal en deler van de
deling, de radicand van de wortel — werd door de aanroeper BUITEN die omgeving
gedecomponeerd, met `companionDeel`, en die weigerde alles wat geen
`AttribuutRef` is. Een binder is dat niet. Dus: elke §6.5-plek in een `Let`-body
die haar behoefte leest decomponeerde naar `null`, en `null` betekent daar
"companion laten vallen" — het vangnet voor een literal- of parameteroperand,
dat hier een hele klasse van echte plekken opslokte. Stil, want vallen laten is
precies wat je bij een niet-herleesbare operand wél wilt.

De correctie is de scope, niet een nieuw geval: ch. 4.10 verplaatst de WAARDE,
niet de meldplicht. Een `Let` om een plek heen maakt er geen andere plek van.
Concreet:

- `Acc.binderPatroon` (binder -> patroontekst) wordt `Acc.binderDeel` (binder ->
  de hele `OperandDeel`), want een plek toetst LEEGTE en heeft dus het leeg/vol-
  hek van de behoefte nodig, niet alleen haar waardetekst.
- `companionDeel` lost een `BinderRef` op via die omgeving — dezelfde
  substitutie die `takGarde` al maakte. Een binder zonder decomponeerbare
  behoefte blijft `null` en laat de companion vallen, als elke andere operand.
- `plekGuards` krijgt het plek-deel als callback in plaats van als vooraf
  berekende tekst, zodat het BINNEN de omgeving draait die de hekken opzetten.
  De omgeving wordt in loopvolgorde opgebouwd (wat ook voorkomt dat een
  schaduwende binnenbehoefte zichzelf oplost) en in een `finally` afgebroken.

Twee aanroepers, beide uniform omgezet: `deelDoorLeegFout` (§6.5) en
`wortelNegatiefFout` (§6.6). Geen ander gedrag: de hekken, de volgorde en de
patroontekst van een plek zonder binder zijn ongewijzigd — de sweep bevestigt
dat (zie onder).

## De getuige

`backends/drools/src/test/java/io/github/yhavinga/regelspraak/cases/LetMeldplekWitnessTest.java`,
vijf rijen, alle door `BeideTakken` (exec-model én DRL-tekst, dezelfde
verwachting geëist van allebei). Elke waarde is eerst door `ref` geproduceerd —
de arbiter — en door clingo herhaald; de vijf modellen zijn verifier-clean.

    dezelfde_deling_meldt_met_en_zonder_let_in_beide_takken
      de kale plek en dezelfde plek onder één Let: leeg + inconsistent
    de_gebonden_deling_meldt_niet_als_de_deler_gevuld_is
      dezelfde model, deler 2,00: 5.0000 + consistent (geen verzonnen melding)
    de_barriere_van_de_behoefte_kapt_de_melding_af_en_laat_haar_anders_staan
      "Let k = inkomen in (k plus (saldo gedeeld door restpost))": behoefte leeg
      -> leeg + consistent; behoefte gevuld -> 10.0000 + inconsistent
    de_plek_onder_een_tak_meldt_alleen_op_de_betreden_arm
      de takhek-vorm in het klein: 5.0000 + consistent op de niet-betreden arm,
      leeg + inconsistent op de betreden
    een_literale_deeltal_meldt_in_geen_van_beide_takken_ook_zonder_let
      de GRENS, ROOD gepind (`BeideTakken.rood`): een literal deeltal levert
      geen herleesbaar hek, dus geen companion — mét of zonder `Let`, in beide
      takken. Gemeten op de kale tweeling, dus die miss gaat over de OPERAND en
      niet over de binding; hij is pre-existent en deze kaart heft hem niet op.

Suite: 466 -> 471 tests (+5), 0 failures, 0 skipped.

## Mutatieproef, feitelijk uitgevoerd en hersteld

Drie mutaties, elk apart gebouwd, gedraaid en teruggedraaid; werkboom daarna
schoon en de rij terug op 5/0.

    M1  de fix terug (companionDeel weigert de BinderRef weer)      2 FAILURES
        drl-tekst — dezelfde deling onder een Let ==> expected: <null>
          but was: <case: verwacht inconsistent, werkelijk consistent>
        drl-tekst — de betreden arm meldt ==> expected: <null>
          but was: <case: verwacht inconsistent, werkelijk consistent>
    M2  het behoeftehek uit de conditie                             1 FAILURE
        drl-tekst — de lege behoefte kapt af voor de deling ==> expected: <null>
          but was: <case: verwacht consistent, werkelijk inconsistent
          (uit van een jutter @ J1)>
    M3  de takgarde uit de conditie                                 1 FAILURE
        drl-tekst — de niet-betreden arm meldt niet ==> expected: <null>
          but was: <case: verwacht consistent, werkelijk inconsistent
          (uit van een jutter @ J1)>

M1 valt precies op de twee rijen die de fix draagt en met de OUDE uitkomst; de
kale tweeling, de gevulde-deler-flank, de barrièreflank en de grensrij blijven
groen. M2 en M3 vallen elk op hun eigen fabricageflank, dus de herstructurering
heeft de bestaande hekken niet decoratief gemaakt.

## Corpussweep over ALLE goldens in BEIDE takken, vóór en ná

Eén regel per golden (208 goldens), met de zelfgebouwde shaded jar van vóór en
van ná. Exec: PLAYED 159 -> 159, het hele rapport BYTE-IDENTIEK — geen enkele
regel beweegt. DRL-tekst: PLAYED 127 -> 130, met precies vier gewijzigde regels:

    synth3-takhek        FAIL (case2 melding)            -> PLAYED 2
    synth3-operatortak   FAIL (case2 melding)            -> PLAYED 2
    synth3-armvangst     FAIL (case2 melding, 2 regels)  -> PLAYED 2
    synth3-armverdeling  FAIL (case1 waarden + case2 melding) -> FAIL (case1 waarden)

De vierde is een REDEN-verschuiving binnen hetzelfde kanaal en verdient haar
eigen zin: armverdeling's case2-meldingen zijn weg, haar case1-waardenafwijking
staat er nog — dat is reden (b), de geneste-`Let`-duw, en die hoort bij de kaart
`drl-geneste-let-duw`. `synth3-armwissel` blijft EXCLUDE op reden (c). Geen
andere golden verandert van kanaal, telling of reden, in geen van beide takken.

## Alle veertien gates, los gedraaid, exitcode per gate

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

Tellingen: gate 2 236 documenten (vloer 192), gate 3 236 golden clean + 395
negatief (vloer 350), gate 4 565 casebestanden, gate 5 643 worked examples
(vloer 489) + 537 cases (vloer 427), gate 6 537 cases + 408 suite-checks
(vloer 83), gate 7 501 cases weerleggen een niets-doener, gate 8 28 rode cases
(vloer 26), gate 9 201 checks + 12 goldens, gate 10 298 tests + 57 goldens,
gate 11 29/29/29, gate 12 played 159 / kanaalvloer 49 / JUnit 471 (vloer 239),
gate 13 126 twists over 23 goldens / 104 replays / 22 DRL-tekst-PLAYED
(vloer 13), gate 14 130 checks (vloer 81).

Gate 12's exec-telling beweegt niet: die gate draait alleen de exec-tak en die
tak was al compleet. Gate 13's DRL-PLAYED-telling beweegt óók niet — 22 vóór én
ná, apart gemeten door de basisversie van `DrlTranspiler.java` terug te zetten
en de gate opnieuw te draaien. Dat is geen toeval: geen van de vijf
`synth3-*`-goldens draagt een eenheidsdragende annotatie, dus geen van hen zit
in de twist-set van die gate.

## Registervoorstellen (niet geland — hoofdsessie)

**`spec/CONSTRUCTS.md` rij 24.** VERVANG de zin

    the DRL-text branch plays none of the five, for three separately measured
    reasons recorded at gate 12's channel count.

DOOR

    since 2026-09-19 (drl-let-meldplek card) the DRL-text branch plays THREE of
    them — `synth3-takhek`, `synth3-operatortak` and `synth3-armvangst` —
    because its §6.5/§6.6 companion now follows the BINDING: a fault site whose
    operand is a binder stands for its need's own re-readable read, so a §9.5
    place inside a `Let` body deposits exactly as the un-bound spelling does.
    The two it does not play are not that gap: `synth3-armverdeling` still
    answers a wrong VALUE on case 1 (a second nested `Let` between the need and
    the branch node is placed eagerly there) and `synth3-armwissel` is refused
    loudly by name; both reasons are recorded at gate 12's channel count.

**`scripts/check-drools.sh`, het redenblok bij `EXPECTED_PLAYED` (de alinea die
met "The DRL-TEXT branch plays NONE of the five" begint).** Het beschrijft reden
(a) als actueel en dat is door deze kaart achterhaald; `EXPECTED_PLAYED` zelf
blijft 159. Voorstel, als vervanging van die alinea:

    # The DRL-TEXT branch plays THREE of the five since 2026-09-19 (card
    # drl-let-meldplek), and the two it does not are recorded here rather than
    # left to be rediscovered, because this gate does not measure that branch and
    # nothing else would say it. Of the three reasons measured when these goldens
    # landed, (a) is CLOSED: that branch deposited no §9.5 channel reason for a
    # §6.5 place inside a `Let` BODY — the same document without the `Let` played,
    # so the gap was the binding and not the branch — and its companion follows
    # the binding now, so takhek, operatortak and armvangst PLAY there (DRL-text
    # 127 -> 130, measured over all goldens in both branches; this gate's exec
    # count is unmoved and its whole exec report byte-identical). Two remain, both
    # pre-existing: (b) a SECOND nested `Let` between the need and the branch node
    # is still placed eagerly there, so armverdeling's case 1 answers leeg where
    # ref, clingo and the exec branch answer 1,00, and its wrapper rule answers a
    # coalesced 5,00 on that same case where all three answer 6,00 — the barrier
    # shrunk to the arm, so a value defect and not just a missing report;
    # (c) armwissel is refused LOUDLY by name (`TODO takgarde van een
    # voorwaardelijk gelezen behoefte (ch. 10.2)`), which is the honest channel.
    # Carrying (b) is Drools work on the DRL-text transpiler and belongs to its
    # own card (drl-geneste-let-duw); weakening these goldens to make that branch
    # pass would have removed exactly the expectations ref and clingo agree on.

## Wat NIET af is, en wat gemeten tegenviel

**De §6.6-wortel onder een binder is in deze tak onbereikbaar.** De
wortel-aanroeper is even uniform omgezet, maar `Let s = saldo in (de wortel van
s)` haalt de companion niet: de DRL-tekst-tak weigert dat document al eerder,
bij de waardevertaling (`TODO binderlezing binnen een aggregatie (ch. 4.10)`),
en clingo vertaalt WORTEL helemaal niet. De wijziging daar is dus vandaag
correct maar ongetuigd; een vorm waarin een wortel onder een `Let` staat met een
ATTRIBUUT als radicand liep al vóór deze kaart goed (die route gebruikt de
behoeftehekken, niet de binderomgeving).

**Een niet-decomponeerbare behoefte blijft een gemiste melding.** `Let k =
100,00 in (k gedeeld door restpost)`: ref meldt, exec meldt, de DRL-tekst-tak
niet. Dat is dezelfde pre-existente grens als bij een literal operand zonder
`Let` — op de kale tweeling `100,00 gedeeld door restpost` missen BEIDE
Drools-takken de melding — en dus niet het gat van deze kaart. Rood gepind in de
getuige, zodat het opheffen ervan een bewuste stap wordt.

**Een behoefte die zélf een binder is, mist haar companion** (lensvondst
2026-09-19, pre-existent, vóór én ná deze kaart identiek): `Let k = inkomen in
(Let m = k in (m gedeeld door restpost))` is verifier-clean en ref, clingo én
exec melden — de DRL-tekst-tak niet. Geïnstrumenteerd bevestigd: de behoeftelus
roept `companionDeel` aan vóórdat de binderomgeving is opgezet, dus een
behoefte-`BinderRef` lost op tegen een lege map en de companion valt weg
(veilige kant). De naïeve herordening zou de bewust gedocumenteerde
zelf-resolutiebescherming ongedaan maken; dit is een besluit, geen mechanische
fix — bijgeschreven op `drl-geneste-let-duw`, waar hij aan reden (b) grenst.

## Open twijfel

De schaduwtwijfel hieronder is door de lens gemeten en in beide richtingen
ongegrond: de letterlijke zelfverwijzende vorm is niet verifier-clean (WF-076,
`the let binder 'k' is referenced in its own value`), en een wél verifier-clean
schaduwvorm — `Let a = inkomen in (Let a = saldo in (a gedeeld door
restpost))` — gedraagt zich correct: ref, clingo, exec én de NA-DRL-tak melden
alle vier `inconsistent`, en het patroon draagt de saldo-hekken zónder
`inkomen != null`, wat juist is (ch. 10.6 clausule 3: de buitenste behoefte
wordt nooit gelezen). De VOOR-DRL-tak miste die melding — een vijfde vorm die
deze kaart blijkt te repareren. De oorspronkelijke twijfel, ter archivering:
de binderomgeving is een map met één ingang per NAAM, dus schaduwing zou de
buitenste behoefte kunnen verdringen — de meting laat zien dat precies dat
gedrag het juiste antwoord oplevert.

Tweede twijfel: het dubbele hek in het patroon (`inkomen != null, inkomen !=
null, …`) als behoefte en operand dezelfde lezing zijn. Drools aanvaardt het en
de semantiek is identiek, maar de DRL-tekst is er langer door dan nodig en een
latere lezer kan het voor een fout aanzien. Ontdubbelen is puur cosmetisch en
zou de patroontekst van BESTAANDE companions veranderen; ik heb het daarom
gelaten.
