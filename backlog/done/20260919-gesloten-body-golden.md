# gesloten-body-golden: de vouwvorm met Let-body als corpusgetuige

Gemeten (asp-ch102-kaart, 2026-09-19): de gesloten lambdabody — een
vouw uit `jutterswet-kinderdeler`-vorm met body `Let m = grondslag / x
in (als x >= 0 dan acc plus 1,00 anders acc plus m)` — is verifier
PASS, ref PASS, clingo PASS, en BEIDE Drools-takken weigeren op de
loader (`Datatype:ListType (unknown datatype node)`). De golden kon in
die kaart niet landen omdat de benodigde `EXPECTED_CHANNEL`-regel
buiten het kanaaltellingsmandaat viel.

De regel die daarbij hoort, letterlijk uit dat Result
(`EXPECTED_PLAYED` blijft ONGEWIJZIGD — een nieuwe golden die niet
speelt is nooit meegeteld):

    "synth3-armlichaam|EXCLUDE|loader Refusal: Datatype:ListType — a
    closed lambda body needs a list-typed fold operand (WF-069), which
    the v0 loader does not carry; the form itself is pinned by ref and
    clingo"

## Acceptatie (bij oppakken aanscherpen)

- (a) De golden met handberekende verwachtingen, gespeeld door
  verifier, ref en clingo; beide Drools-takken EXCLUDE met de gepinde
  loader-reden.
- (b) De `EXPECTED_CHANNEL`-regel als gemandateerd
  beschermd-padvoorstel; alle veertien gates groen.

May touch: nieuwe goldenmap onder `corpus/golden/`,
`scripts/check-drools.sh` uitsluitend de kanaallijst (voorstel),
kaartverplaatsing.

Result: geland. `corpus/golden/synth3-armlichaam` is de zesde golden van
de ch.-10.2/10.6-familie en de eerste die de takherschrijving in de
GESLOTEN BODY vastlegt. De vijf zusters zetten de `Let`/`IfThenElse`-keten
op regelniveau, waar een binder leeg kan zijn en de barrière het
§6.5-kanaal is; hier kan geen binder leeg zijn, dus wat de arm moet dragen
zijn de HEKKEN van de deling. De golden speelt in geen van beide
Drools-takken — de gedeelde v0-loader weigert `Datatype:ListType` — dus
beide gelijkheidspins staan onveranderd en de twee kanaallijsten kregen
ieder één regel erbij.

## De handberekening, en waarom hij falsifieerbaar is

De vouw is `(acc, x) -> Let m = de grondslag gedeeld door het kindertal in
(als x >= 0 dan acc plus 1,00 anders acc plus m)` over de lijstparameter
`kindertallen`, met `grondslag = 60` en vouwbasis `0,00`. Het elementtype
staat op `teken: ONBEPAALD` en niet op `NIET_NEGATIEF` zoals bij
`jutterswet-kinderdeler`: met die laatste zou `x >= 0` vacuüm waar zijn en
de anders-arm onbereikbaar, en dan zou het document een eenarmige getuige
zijn die zich als tweearmig voordoet.

    case1-nuldeler    [0]              0 >= 0 -> dan-arm, m NIET berekend
                                       0,00 + 1,00                      = 1,00
    case2-maatdeel    [-4]             -4 >= 0 onwaar -> anders-arm
                                       0,00 + (60 / -4 = -15,00)        = -15,00
    case3-gemengd     [0, 3, -5, -2]   dan, dan, anders, anders
                                       0,00 +1,00 +1,00 -12,00 -30,00   = -40,00
    case4-leeg-lijst  []               de vouwbasis vuurt op lpres      = 0,00

Vier verwachtingen, alle vier vooraf met de hand berekend en pas daarna
gedraaid; ref en clingo halen ze alle vier bij de eerste run exact
(`oracle: PASS (2 expectations)` en `clingo: PASS (2 expectations)` per
case). Elke deler deelt 60 exact op de 2-decimalenschaal, dus het
exactheidshek van de gesloten body (`((P2)*100) \ (V) == 0`) laat het
quotiënt door als waarde in plaats van als `viol/2`.

Case1 is de falsifieerbare: 60 gedeeld door 0 staat in de arm die de vouw
NIET neemt. Een uitvoerder die de `Let`-waarde eager plaatst faalt daar,
en dat is geen redenering maar een corpusmeting —
`corpus/rood/closed-runtime-division-by-zero` is dezelfde vouw met
dezelfde grondslag en een `0` tussen de kindertallen ZONDER takknoop, en
faalt in ref én clingo met *"division by numeric zero"*. Zelfde deler,
zelfde deeltal, één structureel verschil.

Het gedumpte ASP-programma laat de plaatsing letterlijk zien (opgevangen
met een `clingo`-wikkel op PATH die het tijdelijke `.lp` kopieert; niets
in de repo aangeraakt). De `Let`-waarde staat SYNTACTISCH buiten de
takknoop, en toch draagt elk van de zes `viol/2`-regels die de deling
meebrengt — twee operandbereikhekken, het nulhek, het exactheidshek, twee
resultaatbereikhekken — de armgarde `V < 0`, naast de anders-recurrentie;
de dan-recurrentie en haar eigen twee bereikhekken dragen `V >= 0` en géén
deelhek. `V < 0, (V) == 0` is onvervulbaar, en dat is precies waarom case1
antwoordt. (clingo's grounder meldt bij `V = 0` `info: operation undefined
((100*P2)\V)` op het exactheidshek; die regelinstantie valt hoe dan ook af
op `V < 0`, en dezelfde melding staat op de rode tweeling hierboven, dus
het is een bestaande grounder-opmerking en geen eigenschap van dit
document.) Dit alles staat in `NOTE.md` naast de handberekening.

## De gemeten weigerreden, per tak

Beide takken, met de zelfgebouwde shaded jar, op dezelfde vier cases —
één regel, letterlijk identiek, want de loader is gedeeld:

    exec (geen vlag)  EXCLUDE loader: form not carried by the v0 Drools
                      backend: Datatype:ListType (unknown datatype node)
    --drl             EXCLUDE loader: form not carried by the v0 Drools
                      backend: Datatype:ListType (unknown datatype node)

Een gesloten lambdabody bestaat alleen als `Fold`/`MapList`-functieoperand
(WF-069) of als functiesectie, en de v0-loader draagt geen van beide. Het
is het enige familielid buiten het Drools-vierkant: de exec-tak speelt de
andere vijf, die dezelfde keten op regelniveau zetten waar geen lijsttype
aan te pas komt. De absoluten zijn hier dus een ref = clingo-overeenkomst
verankerd op handgeschreven waarden, en dat staat in `NOTE.md` in plaats
van dat het uit een telling afgeleid moet worden.

## De twee lijstregels, letterlijk

Toegevoegd aan `scripts/check-drools.sh`, alfabetisch tussen
`synth2-afrondingscombinaties` en `synth3-besluitschakel` in
`EXPECTED_CHANNEL`, en in klasse (B) van `DRL_CHANNEL` tussen
`jutterswet-waarderingsinvoer` en `synth3-besluitschakel`. Beide dragen
DEZELFDE redentekst, wat de conventie van klasse (B) is ("Every one of
these names an EXPECTED_CHANNEL entry above with the same reason"):

    "synth3-armlichaam|EXCLUDE|loader Refusal: Datatype:ListType — a closed lambda body needs a list-typed fold operand (WF-069), which the v0 loader does not carry; the form itself is pinned by ref and clingo"

`EXPECTED_PLAYED` blijft 159 en `EXPECTED_DRL_PLAYED` blijft 132: een
nieuw toegevoegde golden die niet speelt is nooit meegeteld. Wat wel
beweegt zijn de twee door-naam-vloeren, 50 -> 51 en 77 -> 78, en die
bewegen omdat de lijsten zelf groeien. Gemeten na toevoeging: gate 12
groen, `played 159`, `DRL-text branch played 132`, en de
`Datatype:ListType`-groep in het redenrapport 14x -> 15x.

## De mutatieproef op gate 5

Eén verwachting gedraaid: `case3-gemengd` van `-40.00` naar `4.00` — de
waarde die een uitvoerder zou geven die ALTIJD de dan-arm neemt, dus een
echte misleesfout en niet zomaar een ander getal. Gate 5 valt er bij naam
op om, exitcode 1:

    FAIL: corpus/golden/synth3-armlichaam/case3-gemengd.json does not replay:
    MISMATCH: J1.lichaamtotaal = -40, expected 4

Daarna hersteld; gate 5 weer groen. De lijstdiscipline zelf is vandaag al
door de takteller-kaart bewezen en is hier niet opnieuw gemuteerd.

## Alle veertien gates, exitcode per gate

    self-contained: 0        rood: 0
    ir-schema: 0             catala: 0
    verifier: 0              regelspraak: 0
    case-schema: 0           toka-parity: 0
    oracle: 0                drools: 0
    clingo: 0                reftype-derivation: 0
    noop: 0                  flogic: 0

Nieuwe standen (één document en vier cases erbij; geen vloer verhoogd):
gate 2 239 documenten (vloer 192), gate 3 239 golden clean en 398 negatief
(vloer 350), gate 4 572 casebestanden, gate 5 543 cases (vloer 427) en 643
worked examples (vloer 489), gate 6 543 cases en 419 suite-checks (vloer
83), gate 7 507 cases weerleggen een niets-doener, gate 8 29 rode cases
(vloer 26), gate 12 played 159 / DRL-played 132 / kanaalvloer 51 /
DRL-vloer 78 / JUnit 478, gate 13 126 twists over 23 goldens.

Gate 12 is twee keer gedraaid. De eerste run liep per ongeluk gelijk op
met gate 8, die de shaded jar óók onvoorwaardelijk herbouwt; hij was
groen, maar een meting onder een gelijktijdige jar-herbouw is geen meting,
dus hij is daarna alleen gedraaid (104s DRL-sweep, zelfde uitslag).

## Registervoorstellen (niet geland — hoofdsessie)

**`spec/CONSTRUCTS.md` rij 24.** De rij noemt nu vijf corpusgoldens. NA de
zin die eindigt op "…since 2026-09-19 the DRL-text branch plays ALL FIVE,
over two cards." toevoegen:

    A SIXTH, `synth3-armlichaam` (2026-09-19, gesloten-body-golden card),
    pins the same line in the CLOSED-BODY lowering, where no binder can be
    leeg and what the arm must carry is the division's fences rather than
    the §6.5 channel: a `Let` value that is a runtime division stands
    syntactically outside the branch node and every one of its six `viol/2`
    fences carries the anders-arm guard, so a fold fed a single `0` answers
    1,00 where `corpus/rood/closed-runtime-division-by-zero` — the same
    divisor without a branch node — faults in both executors. It is the one
    member of the family OUTSIDE the Drools square: both branches refuse it
    at the SHARED v0 loader on `Datatype:ListType`, pinned by name in both
    of gate 12's channel lists, so its absolutes are a ref = clingo
    agreement anchored to hand-written values rather than the full square.

**`scripts/check-drools.sh`, twee tellingen in commentaar.** Bewust NIET
aangeraakt — het mandaat van deze kaart is de twee lijstregels en niets
anders in het script — maar ze lopen er wel een achter. In het
klasseblok (B) van `DRL_CHANNEL`: "loader Refusals — 34" wordt 35. In het
blok dat `DRL_CHANNEL` opent: "sixty-eight of the seventy-seven entries
carry one of three class reasons" wordt "sixty-nine of the seventy-eight".
Geen gate leest ze; wie ze bijwerkt doet dat als bijwerking van de
lijsten, niet van een pin.

## Wat NIET af is, en wat open blijft

De ch.-10.6-duw is hier alleen in de ASP-tak gemeten, want de Drools-takken
komen niet voorbij de loader. Of de gesloten body in een van beide takken
ooit speelt is een loader-kaart (`Datatype:ListType` dragen), geen kaart
over deze vorm; zolang die er niet is blijft dit document een
twee-uitvoerdersgetuige, en de kanaallijsten zeggen dat bij naam.

Tweede open punt, eerlijk: de golden meet de plaatsing van de `Let`-waarde
in de arm, niet de VOLGORDE waarin de armen hun hekken zetten. Een
implementatie die beide armen correct schermt maar de hekken in de
verkeerde volgorde evalueert antwoordt hier hetzelfde. Daar is geen getuige
voor gebouwd; ch. 5.11's operandvolgorde heeft zijn eigen
(`synth3-operandvolgorde`), maar niet binnen een gesloten body.
