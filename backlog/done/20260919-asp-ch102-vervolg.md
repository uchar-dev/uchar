# asp-ch102-vervolg: corpusgoldens voor de tak-volgende lezing en de capture-restvorm

Rest van de asp-hek/asp-duw-lijn (2026-09-18/19). Twee onderdelen:

1. **Corpusgoldens door het hele vierkant.** De ch.-10.2/10.6-vormen
   die de clingo-kaarten draagbaar maakten (arm-hekken, operator-route,
   gesloten body, geneste Let via distributie en wissel) kunnen nu als
   goldens met absolute verwachtingen — gate 6 zou ze vóór die kaarten
   terecht hebben afgewezen. Kanaaltellings-consequentie voor gate 12
   als gemandateerd beschermd-padvoorstel (de vormen spelen in beide
   Drools-takken sinds de daarbij-geldt-kaart).
2. **De capture-guardvorm** (slotlens 2026-09-19, pre-existent, geen
   corpustreffer): leest de body de buitenste behoefte alleen via de
   geneste waarde, dan zit de lezing in precies één arm en divergeert
   de eager plaatsing die de distributie-guard bewust laat staan —
   `Let k in Let z = teller/deler in Let w = (z plus 1,00) in (als k >
   10,00 dan 0,00 anders w)`, dan-arm: ref PASS, clingo meldt/breekt.
   Dragen (de guard verfijnen naar werkelijke leesplaats) of luid
   weigeren; nooit stil.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst per vorm; ref als arbiter; reikwijdte-pin verplicht
  bij elke draagroute.
- (b) Goldens met handberekende verwachtingen door verifier, ref,
  clingo én beide Drools-takken; EXPECTED_PLAYED-beweging verantwoord.
- (c) Suite-getuigen + mutatieproeven; alle veertien gates groen.

May touch: `backends/clingo/**`, nieuwe goldenmappen onder
`corpus/golden/`, `scripts/check-drools.sh` uitsluitend voor de
kanaaltelling (voorstel), kaartverplaatsing.

Result: geland. Vijf corpusgoldens zetten de ch.-10.2/10.6-takvormen op
handberekende absolute waarden die ref, clingo én de exec-model-tak van
Drools alle drie halen, en de capture-restvorm is GEDRAGEN in plaats van
geweigerd: de distributie-guard is van een syntactische vermelding een
gemeten toets op de werkelijke leesplaats geworden. Gate 12's
`EXPECTED_PLAYED` gaat van 154 naar 159, en dat is de enige aanraking van
een beschermd script.

## Wat de frontend niet spelt, en wat dat betekent

Alle vijf documenten zijn met de hand op de wire geschreven, zonder
`bron.rgs`. Dat is geen keuze maar een meting: de RegelSpraak-emitter
bouwt geen `IfThenElse` — `IrEmitter` gooit bij die knoop letterlijk
"IfThenElse is never emitter-produced (IR-SPEC ch. 4.10)" — en geen enkele
golden in het hele corpus draagt vandaag een `bron.rgs` én een
`IfThenElse`. Deze familie heeft dus geen brontaal; zij staat in dezelfde
positie als haar `synth3-`-zusters. Gate 10 raakt ze daarom niet en hoeft
dat ook niet.

## De vijf goldens, met hun handberekende waarden

Alle vier de uitvoerders zijn per golden gedraaid: `verifier.exe`,
`runner.exe` (ref), `clingo_runner.exe`, en de zelfgebouwde shaded jar van
`backends/drools` zonder vlag (exec-model) én met `--drl` (de
DRL-tekst-tak). `drempel` 10,00, `basisheffing` 5,00 in alle vijf.

    synth3-takhek        Let k = inkomen in (als k > drempel dan basisheffing
                                             anders k gedeeld door restpost)
    synth3-operatortak   dezelfde vorm, de takknoop ONDER een plus
    synth3-armverdeling  Let k in Let z = k/restpost in Let w = 1,00 in
                         (als k > drempel dan w anders z)   — plus de wikkelregel
    synth3-armwissel     ... Let w = k in (als w > drempel dan 0,00 anders z)
    synth3-armvangst     ... Let w = (z plus 1,00) in
                         (als k > drempel dan 0,00 anders w) — plus de wikkelregel

Elke golden draagt twee casebestanden. `case1-nietbetreden` zet drie
jutters naast elkaar — J1 met een LEGE restpost, J2 met restpost 0,00,
beide met inkomen 40,00 (de dan-arm), en J3 met inkomen 5,00 en restpost
2,00 (de anders-arm die gewoon rekent) — en eist daarbij `consistent`,
dus de afwezigheid van elk kanaalfeit. `case2-betreden` zet J4 met inkomen
5,00 en een LEGE restpost: de arm wordt betreden, §6.5 meldt, en de
ch.-10.6-barrière leegt.

    golden               J1        J2        J3        J4
    takhek               5,00      5,00      2,50      leeg + melding
    operatortak          10,00     10,00     7,50      5,00 + melding
    armverdeling         1,00      1,00      2,50      leeg + melding
      wikkelheffing      6,00      6,00      7,50      leeg  (NIET 5,00)
    armwissel            0,00      0,00      2,50      leeg + melding
    armvangst            0,00      0,00      3,50      leeg + melding
      wikkelheffing      5,00      5,00      8,50      leeg  (NIET 5,00)

Twee cellen verdienen hun eigen zin. `operatortak` J4 is 5,00 en NIET
leeg: de betreden arm evalueert leeg, en de `plus` erboven coalesceert die
leegte naar 0 (ch. 1.2), want daar staat geen `Let`-barrière tussen — het
kanaalfeit staat er wel. De twee `wikkelheffing`-kolommen zijn het
omgekeerde en zijn de REIKWIJDTE-PIN: daar leest de arm de behoefte wél
via een `Let`, dus de barrière hoort bij die `Let` en leegt het GEHEEL,
inclusief de `plus`. Zou een herschrijving de reikwijdte tot de arm
verkleinen, dan leest diezelfde `plus` de geleegde arm als 0,00 en
antwoordt 5,00.

Status per golden, viervoudig gemeten:

    golden               verifier  ref    clingo  exec        --drl
    synth3-takhek        PASS      PASS   PASS    PLAYED 2    FAIL (a)
    synth3-operatortak   PASS      PASS   PASS    PLAYED 2    FAIL (a)
    synth3-armverdeling  PASS      PASS   PASS    PLAYED 2    FAIL (a)+(b)
    synth3-armwissel     PASS      PASS   PASS    PLAYED 2    EXCLUDE (c)
    synth3-armvangst     PASS      PASS   PASS    PLAYED 2    FAIL (a)

## De DRL-tekst-tak speelt er geen een, en dat is bij naam verantwoord

De kaart ging ervan uit dat deze vormen sinds de daarbij-geldt-kaart in
BEIDE Drools-takken spelen. Voor de exec-model-tak klopt dat; voor de
DRL-tekst-tak niet. Drie redenen, elk apart gemeten op een eigen minimale
invoer, alle drie pre-existent (deze kaart heeft geen regel Drools-bron
aangeraakt):

(a) **Geen §9.5-depositie voor een §6.5-plek binnen een `Let`-body.**
    Gemeten met twee documenten die alleen in de `Let` verschillen:
    `heffing := zijn inkomen gedeeld door zijn restpost` speelt in beide
    takken (`--drl PLAYED 1`), terwijl
    `heffing := Let k = zijn inkomen in (k gedeeld door zijn restpost)`
    in de DRL-tekst-tak `FAIL: verwacht inconsistent (heffing van een
    jutter @ J4), werkelijk consistent` geeft en in exec `PLAYED 1`. De
    grens is dus de BINDING, niet de takknoop. Dit is precies de gemiste
    melding die de daarbij-geldt-kaart bij zichzelf optekende, nu met een
    corpusgetuige ernaast.
(b) **Een TWEEDE geneste `Let` tussen de behoefte en de takknoop staat er
    nog eager.** Gemeten: één geneste `Let` (`Let k in Let m = k/restpost
    in (als k > drempel dan basisheffing anders m)`) speelt in beide
    takken; de D1-vorm met een derde `Let` ertussen geeft in de
    DRL-tekst-tak `FAIL: attribuut heffing: verwacht 1.00, werkelijk leeg`
    waar ref, clingo én exec 1,00 antwoorden. Dat is een VERKEERDE WAARDE,
    niet alleen een gemiste melding — en `armverdeling`'s wikkelregel
    antwoordt daar 5,00 waar alle drie de andere leeg zeggen, dus ook de
    reikwijdte staat daar nog verkeerd.
(c) `armwissel` wordt LUID geweigerd: `EXCLUDE drl-transpiler MARKER:
    regel: heffing van een jutter — TODO takgarde van een voorwaardelijk
    gelezen behoefte (ch. 10.2)`. Dat is het eerlijke kanaal.

**Afweging, expliciet.** De `inconsistent`-verwachtingen uit `case2` en de
`consistent` uit `case1` zijn precies wat ref en clingo allebei beweren,
en zij zijn de helft die een vertaling die NOOIT meldt zou passeren.
Ze weghalen om de DRL-tekst-tak groen te krijgen zou de golden op de
zwakste uitvoerder snijden en het vals-groene raam weer openzetten dat
deze documenten dichtdoen. Ze blijven staan; de tak wordt bij naam
verantwoord, hier en in het redenblok van `scripts/check-drools.sh`.
Gate 12 draait alleen de exec-tak, dus dit maakt geen gate rood.

## Rood-eerst, deel 2: de capture-vorm, letterlijk vóór en ná

Twee documenten, beide `verifier: PASS`, beide ref PASS vóór én ná —
`Let k = zijn inkomen in (Let z = k gedeeld door zijn restpost in (Let w =
(z plus 1,00) in (als k > de drempel dan 0,00 anders w))))`, kaal (V1) en
met een `plus de basisheffing` eromheen (V2).

    V1 inkomen 40,00, restpost leeg   ref PASS   clingo MISMATCH: expected consistent,
                                                 but the model carries 1 inconsistent fact(s)
    V1 inkomen 40,00, restpost 0,00   ref PASS   clingo UNSATISFIABLE — a fail-fast guard
                                                 fired: viol("division by numeric zero","J1")
    V1 inkomen 5,00,  restpost leeg   ref PASS   clingo PASS        (eager is daar exact)
    V2 inkomen 40,00, restpost leeg   ref PASS   clingo MISMATCH (1 inconsistent fact)
    V2 inkomen 40,00, restpost 0,00   ref PASS   clingo UNSATISFIABLE
    V2 inkomen 5,00,  restpost leeg   ref PASS   clingo PASS

NÁ: alle zes `clingo: PASS`. Identiek op main gemeten (deze tak is main
plus de kaartverplaatsing op het moment van meten), dus pre-existent.

## De keuze: DRAGEN, en waarom

De oude guard was syntactisch: distribueer de geneste `Let` niet als haar
waarde de buitenste binder NOEMT. Die formulering is één begrip te grof.
Of de buitenste behoefte werkelijk in beide armen gelezen wordt is een
ch.-10.6-vraag: de kopie in de arm die de geneste binder niet leest
evalueert haar waarde nooit (clausule 3), dus de buitenste behoefte wordt
gelezen waar de geneste binder gelezen wordt — en nergens anders.

De verfijning vraagt daarom de HELE herschrijving (`let_into_arm`, niet
haar zoekhelft) waar de geneste `Let` landt, en TOETST daarna of de
buitenste behoefte in precies één arm overblijft. Generiek boven
speciaal: er is geen patroon voor deze vorm bijgekomen, er is een aanname
door een meting vervangen. Waar de toets faalt — de behoefte in beide
armen, direct of via de geneste waarde — blijft eager plaatsing staan, en
dat is wat ch. 10.6 daar vraagt.

Twee dingen die deze route gemeten heeft gekost, beide opgeschreven in de
code:

1. **Naar de zoekhelft reiken is fout.** De eerste versie riep
   `hoist_branch b2` rechtstreeks aan. Dat liet deze arm ANTWOORDEN voor
   een vorm die niet de zijne is: D3 (de conditie leest de tussenbinder)
   wordt door de WISSEL opgelost, maar `hoist_branch` kent de wissel niet
   en gooide daar de luide weigering van D4. Gemeten: vijf suite-getuigen
   rood. `let_into_arm` doet de wissel eerst en weigert alleen waar de
   weigering thuishoort.
2. **Een zoektocht met een ANDERE binder kost één garde extra.** Elke
   takknoop die de herschrijving teruggeeft heeft een conditie die de
   binder waarmee gezocht is niet noemt — dat is de toets van het
   basisgeval. Hier is de zoeksleutel `b2`, dus de buitenste binder moet
   apart tegen de conditie getoetst worden, anders staat hij na de wrap
   buiten zijn eigen `Let`. Gemeten zonder die toets:
   `TRANSLATION FAULT: unbound binder "k"`. Een conditie die de buitenste
   binder wél leest forceert de behoefte hoe dan ook, dus daar is
   afzien ook het goede antwoord.

## De reikwijdte-pin van de draagroute

Verplicht bij elke draagroute, en hij zit op twee plaatsen. In de suite is
het V2's `, lege deler in de BETREDEN arm [meldt en leegt]`: heffing
`attributeEmpty`, niet 5,00. In het corpus is het `synth3-armvangst`'s
`wikkelheffing` op J4. Beide zijn gemeten falsifieerbaar (twist B
hieronder). Zou de duw de reikwijdte tot de arm verkleinen, dan leest de
`plus` die geleegde arm als 0,00 en antwoordt 5,00.

## Suite-getuigen, bij naam

Alle in `backends/clingo/test/test_clingo.ml`, de suite die gate 6 met
tellingsvloer draait. V1 en V2 krijgen de volle zes hoeken van de
bestaande helper `tak_vier` (verifier-clean; lege deler en nuldeler in de
niet-betreden arm; de nulgarde staat nog in het programma; de
reikwijdte-pin; de nuldeler in de betreden arm als `both_break`), telkens
met een `ref_matches` op dezelfde invoer ernaast:

    ch10.6 vangst: de body leest de behoefte alleen via de geneste waarde
    ch10.6 vangst onder een wikkel

en twee getuigen voor de andere kant van de toets, waar de herschrijving
zich moet INHOUDEN — beide uitvoerders melden en breken daar op de
DAN-arm, precies de arm waar V1 niets mag doen:

    ch10.6 vangst: de conditie leest de geneste binder
    ch10.6 vangst: de andere arm leest de behoefte direct

`test_clingo: 378 checks, 0 failures` vóór, `408 checks, 0 failures` ná —
30 checks erbij. `EXPECTED_MIN_SUITE_CHECKS` in `scripts/check-clingo.sh`
staat op 83 en is NIET aangeraakt: groei boven een vloer vraagt geen
scriptwijziging.

## Mutatieproef op de vertaling, feitelijk uitgevoerd en hersteld

Vijf mutaties, elk apart gebouwd, gedraaid en teruggedraaid; werkboom
daarna schoon en de suite terug op 408/0.

    MV  de capture-arm helemaal weg (terug naar de syntactische guard)  ->  4 FAILURES
    MC  de conditie-toets op de BUITENSTE binder weg                    -> 23 FAILURES
    MD  naar hoist_branch reiken i.p.v. naar de hele herschrijving      -> 13 FAILURES
    MI  de enkele-lezende-arm-TOETS weg (aannemen i.p.v. meten)         ->  4 FAILURES
    MA  de hele ch.-10.6-duw levert niets (geërfd)                      -> 20 FAILURES

MV legt ook een verkeerde WAARDE bloot en niet alleen een verzonnen
melding: `J1.heffing is underived (leeg), expected 500` op V2. MI valt
precies op de twee inhoud-getuigen om, met
`unbound binder "z" in the ASP translation` — de toets is dus niet
decoratief maar draagt de scope. MD valt op D3's vijf getuigen plus de
vier nieuwe. MA stond op main op 16 failures en staat nu op 20: vier
getuigen dekking erbij op dezelfde gedeelde helper.

## Mutatieproef op de goldens, feitelijk uitgevoerd en hersteld

Zeven twists van de handberekende waarden, elk apart, telkens door ref,
clingo én de exec-tak gehaald. Alle drie vallen elke keer om; werkboom
daarna schoon.

    A  armverdeling case2: wikkelheffing leeg -> 5,00 (de reikwijdte-pin)
       ref MISMATCH: J4.wikkelheffing = leeg, expected 5
       clingo MISMATCH: J4.wikkelheffing is underived (leeg), expected 500
       exec FAIL: attribuut wikkelheffing: verwacht 5.00, werkelijk leeg
    B  armvangst case2: idem — dezelfde drie regels
    C  takhek case1: consistent -> inconsistent op J1 (het hek meldt niet)
       ref MISMATCH: expected inconsistent …, but the channel carries: (empty)
       clingo MISMATCH: … but the model carries: (none)
       exec FAIL: verwacht inconsistent (heffing van een jutter @ J1), werkelijk consistent
    D  armverdeling case1: heffing 1,00 -> 2,00   (ref/clingo/exec alle drie MISMATCH/FAIL)
    E  armvangst case1: heffing 0,00 -> leeg      (idem — dit is de eager-plaatsingsuitkomst)
    F  armwissel case1: heffing 0,00 -> leeg      (idem)
    G  operatortak case1: heffing 10,00 -> leeg   (idem)

E, F en G zijn met opzet de waarde die EAGER plaatsing zou opleveren: de
goldens falsifiëren dus niet zomaar een getal maar precies de fout die
deze lijn kaarten heeft opgeruimd.

## Corpussweep over ALLE goldens in BEIDE takken, vóór en ná

Eén regel per golden, status en reden, met de eigen shaded jar. Vóór: 203
goldens, exec PLAYED 154, DRL-tekst PLAYED 127. Ná: 208 goldens, exec
PLAYED 159, DRL-tekst PLAYED 127. Het verschil tussen de twee sweeps
bestaat uit precies vijf toegevoegde regels — de vijf nieuwe goldens — en
NUL gewijzigde regels: geen enkele andere golden verandert van kanaal, van
telling of van reden, in geen van beide takken.

## Alle veertien gates, exitcode per gate

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

Bewegende tellingen: gate 2 236 documenten (vloer 192), gate 3 236 golden
clean, gate 4 565 casebestanden, gate 5 537 cases (vloer 427), gate 6 537
cases en 408 suite-checks (vloer 83), gate 7 501 cases weerleggen een
niets-doener, gate 12 played 159 / kanaalvloer 49 / JUnit 459.

## Wat NIET af is, en wat tegenviel

**De gesloten body heeft geen golden gekregen.** De kaart noemt hem als
vijfde vorm; hij is gebouwd en gemeten, en hij kan niet spelen. Het
document — een vouw over de kindertallen met `(acc, x) -> Let m = de
grondslag gedeeld door x in (als x >= 0 dan acc plus 1,00 anders acc plus
m)`, grondslag 60, kindertallen `[0]`, afgeleid van de vorm van
`corpus/golden/jutterswet-kinderdeler` — is `verifier: PASS`, ref
`oracle: PASS (2 expectations)` en clingo `clingo: PASS (2 expectations)`,
maar BEIDE Drools-takken weigeren hem aan de loader: `EXCLUDE loader:
form not carried by the v0 Drools backend: Datatype:ListType`. Een
gesloten body vraagt per WF-069 een lambda als vouw-operand (dus een
lijsttype) of een functiesectie (`functionDefs`), en de v0-loader draagt
geen van beide. Zo'n golden zou dus een `EXCLUDE`-regel in
`EXPECTED_CHANNEL` nodig hebben, en het mandaat van deze kaart is
uitsluitend de kanaaltelling. De exacte regel, als de maintainer hem wil
landen, staat hieronder bij de registervoorstellen.

**De DRL-tekst-tak viel tegen.** De kaart verwachtte PLAYED in beide
takken; het werden er nul. De drie redenen staan hierboven en in het
script. Twee ervan zijn echte defecten in die tak, geen vormgrenzen, en
één ervan is een verkeerde waarde.

## Open twijfel

De verfijning kijkt één binder ver. Staat er tussen de buitenste `Let` en
de geneste een derde die de keten opnieuw breekt, dan daalt zij af zolang
elke stap een `Let` of een zuivere rekenwikkel is — dat is gemeten voor
twee lagen (V1/V2) en volgt structureel voor meer, maar er is geen
getuige voor drie lagen. Ik heb er geen gebouwd omdat elke laag hetzelfde
recursieve pad neemt; wie dat te dun vindt heeft gelijk dat het een
redenering is en geen meting.

Tweede twijfel: als `let_into_arm` een `Let`-gewortelde uitkomst
teruggeeft (de wisselroute), ziet de capture-arm niets bruikbaars en valt
terug op eager plaatsing. Voor de gemeten gevallen is dat exact — daar
leest de conditie de behoefte — maar dat is een eigenschap van de gemeten
gevallen, niet van de route. Een vorm waarin de wissel slaagt én de
buitenste behoefte armlokaal is, is niet gevonden; ik weet niet of hij
bestaat.

## Registervoorstellen (niet geland — hoofdsessie)

**`spec/CONSTRUCTS.md` rij 24.** VERVANG de zin

    The one shape neither route reaches, a wrapper standing between the
    two `Let`s with a condition-reading intermediate binder, is refused
    loudly by name rather than placed eagerly (measured: a reported §6.5
    place, a fired fail-fast guard, and a coalesced value on an arm ref
    never enters).

DOOR

    Since 2026-09-19 (asp-ch102-vervolg card) the distribution's second
    guard is a MEASUREMENT instead of a syntactic test: where the nested
    value mentions the outer binder, the rewrite asks where the nested
    `Let` lands and then checks whether the outer need is left read in
    exactly one arm — so a body that reaches the outer need ONLY through
    that nested value is carried, while a need genuinely wanted on both
    branches keeps eager placement, which is what ch. 10.6 asks for there.
    The one shape neither route reaches, a wrapper standing between the
    two `Let`s with a condition-reading intermediate binder, is refused
    loudly by name rather than placed eagerly (measured: a reported §6.5
    place, a fired fail-fast guard, and a coalesced value on an arm ref
    never enters). Five corpus goldens pin the whole line at absolute
    values — `synth3-takhek`, `synth3-operatortak`, `synth3-armverdeling`,
    `synth3-armwissel`, `synth3-armvangst` — each played by `ref`, by
    clingo and by the Drools exec-model branch; the DRL-text branch plays
    none of the five, for three separately measured reasons recorded at
    gate 12's channel count.

**`spec/ASP-TRANSLATION.md` S18(a′).** Deze sectie draagt clausule 2 en 3
nog in hun vorm van vóór de hek- en duwkaarten: zij beschrijft
`value_branches` met een per-tak prefix en noemt de herschrijving niet.
Voorstel, als nieuwe alinea aan het eind van (a′), vóór "Two residues
stay recorded":

    *Clause 2 again — the rewrite, not a guard.* Where the barrier's
    place is decided by the ARM rather than by the branch prefix, no
    amount of guard threading suffices: each lowering translates a
    `Let`'s value before it knows the body's branches, so the arm guard
    does not yet exist when that value's fences, `viol/2` guards and
    §9.5 deposits are emitted. A `Let` whose need is read in exactly one
    arm is therefore MOVED into that arm (`let_into_arm`), and the branch
    node is lifted out of any pure arithmetic wrapper standing between
    them so the barrier's extent stays the whole expression rather than
    the arm — a coalescing wrapper would otherwise read the emptied arm
    as 0. A nested `Let` between the two is handled by distributing it
    over the arms, or, where the condition reads its own binder, by
    exchanging the two `Let`s; and where the nested value is the only
    reader of the outer need, the rewrite asks where the nested `Let`
    lands and then CHECKS that the outer need is left read in exactly one
    arm, rather than inferring it from a mention. Where that check fails
    the need is wanted on both branches and eager placement is exact.
    Corpus witnesses: `synth3-takhek` and `synth3-operatortak` (the arm
    fences, on the rule-head route and under an operator),
    `synth3-armverdeling`, `synth3-armwissel` and `synth3-armvangst` (the
    nested `Let`, over all three routes) — each of them pinning, on the
    entered arm, that the barrier empties the whole expression and not
    just the arm.

**`scripts/check-drools.sh`, ALS de gesloten-body-golden alsnog landt** —
niet in deze kaart, want buiten het mandaat. De regel voor
`EXPECTED_CHANNEL`, met `EXPECTED_PLAYED` ONGEWIJZIGD (een nieuw
toegevoegde golden die niet speelt is nooit meegeteld):

    "synth3-armlichaam|EXCLUDE|loader Refusal: Datatype:ListType — a
    closed lambda body needs a list-typed fold operand (WF-069), which
    the v0 loader does not carry; the form itself is pinned by ref and
    clingo"

## Vervolgkaartvoorstellen (niet aangemaakt)

1. *drl-let-meldplek*: de DRL-tekst-tak deponeert geen §9.5-kanaalreden
   voor een §6.5-plek binnen een `Let`-body. Gemeten op twee documenten
   die alleen in de `Let` verschillen. Vier van de vijf nieuwe goldens
   wachten erop.
2. *drl-geneste-let-duw*: diezelfde tak plaatst een TWEEDE geneste `Let`
   tussen behoefte en takknoop nog eager, met een verkeerde waarde
   (`verwacht 1.00, werkelijk leeg`) en een tot de arm verkleinde
   barrière (`verwacht 6.00, werkelijk 5.00`). Dit is de
   ch.-10.6-herschrijving die de ASP-tak in twee kaarten heeft gekregen,
   nu voor de DRL-tekst-tak.
3. *gesloten-body-golden*: de vouwvorm hierboven als golden, mét de
   `EXPECTED_CHANNEL`-regel die daarbij hoort.
