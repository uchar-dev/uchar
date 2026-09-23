# De telling zonder bron — één arm of twee? (normvraag)

Goal: beslissen wat `AANTAL` betekent wanneer hij GEEN instantiebron
draagt en ook geen opgesomde vorm is — ariteit 1 zonder
`elementObjecttype`, en ariteit 0 — en die beslissing op de wire zetten.
Vandaag zijn beide welgevormd, antwoordt precies één uitvoerder, en
weigeren de andere drie. Gevonden bij de meting van
`backlog/done/20260920-aantal-opgesomde-telling.md`, die de opgesomde
vorm (ariteit ≥ 2) van de wire haalde en deze rand BENOEMD open liet
omdat het maintainersbesluit van die dag over "twee of meer" ging.

## De meting (2026-09-20, met de hand nagerekend)

Vier probes tegen de verifier van die kaart, elk met ref, clingo en
beide Drools-takken erachter. De eerste twee rijen zijn geland; de
laatste twee zijn deze kaart.

    elementtype  ariteit  verifier  ref                     clingo/Drools
    gezet        1        PASS      telt instanties         vertaald/gespeeld
    null/gezet   >=2      WF-037    —                       —
    null         1        PASS      gevuldtelling: 0 of 1   weigeren
    null         0        PASS      0                       weigeren

`ref` bereikt die twee via `| None, args -> ` `Values`` in
`eval_functie`'s `contributors ()`, waarna de `Aantal`-arm
`agg_aantal (filled vs)` doet — de gevuldtelling, precies de betekenis
die het setbron-besluit van 2026-09-19 en de opgesomde-kaart van
2026-09-20 allebei van deze soort weg hebben genomen. Zij is hier nog
bereikbaar, en `ref/test/` pint dat expliciet
(`WV-3.4a-eenoperand-gevuld`, `-eenoperand-leeg`, `-geen-operand`).

## De scherpste spelling: de telling van een LIJSTLITERAAL

Ariteit 1 zonder elementtype heeft een spelling die het gebrek laat zien
in plaats van het te beschrijven: één `ListLiteral`-argument
(`IR-SPEC.md` ch. 4.8, `RUNTIME-SEMANTICS.md` ch. 10.1). Gemeten
2026-09-20:

    verifier : PASS
    ref      : 1     bij een lijst van TWEE elementen
    ref      : 1     bij een LEGE lijst
    clingo   : TRANSLATION FAULT: expression kind not translated
    Drools   : EXCLUDE loader: form not carried by the v0 Drools backend:
               Expr:ListLiteral (unknown expression node)

De telling van een lijst antwoordt dus ÉÉN, ongeacht hoeveel de lijst
draagt — zij telt het OPERAND, niet de inhoud. Dat is geen randgeval van
de vraag maar de vraag zelf: een telling zonder instantiebron telt iets
wat niemand bedoelt, en zij doet het stil.

## De normvraag (maintainer)

- **(a) Eén arm: een telling MOET een elementtype dragen.** Dan vallen
  ariteit 0 en ariteit 1-zonder-elementtype samen onder één zin, en die
  zin is LETTERLIJK de tweede arm die `AANTAL_GEVULDE` sinds 2026-09-20
  draagt ("the filled count is an INSTANCE fold and MUST carry an
  element type"). De twee tellingen zouden dan dezelfde wet hebben op
  dezelfde positie, wat het sterkste argument voor deze route is.
  Kosten: nul documenten (gemeten — alle 109 tellingen in de vier zones
  dragen een elementtype).
- **(b) Twee armen.** Ariteit 0 is een vouw zonder enig argument en
  ariteit 1-zonder-elementtype is een vouw met een argument dat geen
  bron is; wie die als twee gebreken leest, wil twee zinnen en twee
  getuigen.
- **(c) Openlaten en `ref` laten weigeren.** Dan verhuist de grens naar
  de uitvoerder in plaats van naar de wire, tegen het huispatroon in
  (de wire beslist, de uitvoerder is tweede verdedigingslinie).

Aanbeveling van de meting: **(a)**. Eén zin, één arm, letterlijk
gespiegeld aan de zustersoort, nul bestaande documenten geraakt.

## Acceptatie (bij oppakken aanscherpen)

- Rood-eerst: een negatief per gekozen arm, eerst falend gemeten met de
  verifier-md5 ervóór en erna, plus een arm-exacte mutant over golden +
  rood + negatief + pariteit.
- Minstens drie spellingen wegen: ariteit 0; ariteit 1 met een gewone
  eigen lezing; ariteit 1 met een `ListLiteral`. De laatste is de
  getuige die het gebrek toont.
- `ref`'s bereikbare arm: na de beslissing is `agg_aantal (filled vs)`
  onder `Aantal` volledig onbereikbaar. Dan pas geldt wat de
  opgesomde-kaart over die arm NIET kon zeggen; de comment daar en de
  zes `WV-3.4a-opgesomd`-getuigen moeten mee bewegen, want drie ervan
  pinnen juist de bereikbaarheid.
- De producentkant MEETEN, niet aannemen. De opgesomde kaart vond een
  lek in de frontendgrammatica waar de brontaal niets spelt; dezelfde
  vraag hoort hier gesteld: kan `ExpressieBouwer` een telling zonder
  elementtype emitteren, en langs welke zin?
- Alle veertien poorten groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` als VOORSTEL,
`spec/RUNTIME-SEMANTICS.md` als VOORSTEL, `corpus/negatief/**`,
`ref/lib/eval.ml` + `ref/test/` (de comment en de getuigen van de
opgesomde kaart), en `frontends/regelspraak/**` als de meting daar een
route aanwijst.

Out of scope: de opgesomde vorm zelf — geland 2026-09-20. De
lijstwereld van ch. 4.8 als geheel; deze kaart raakt hem alleen als
ARGUMENT van een telling.

## Maintainersbesluit (2026-09-23)

OPTIE (a) — één arm: een `AANTAL`-telling MOET een elementtype dragen;
ariteit 0 en ariteit 1 zonder elementtype vallen onder één zin, letterlijk
gespiegeld aan de tweede arm die `AANTAL_GEVULDE` sinds 2026-09-20 draagt
('the filled count is an INSTANCE fold and MUST carry an element type').
Motivering: de gevuldtelling-betekenis is per de besluiten van 2026-09-19
(setbron) en 2026-09-20 (opgesomde vorm) al van `AANTAL` weggenomen en
heeft met `AANTAL_GEVULDE` een eigen soort; een bronloze telling telt het
OPERAND en niet de inhoud (de lijstliteraal-meting: antwoord 1 ongeacht de
lijstinhoud, stil) — precies de verboden klasse; de kosten zijn nul
documenten (alle 109 tellingen in de vier zones dragen een elementtype); en
beide tellingsoorten krijgen zo dezelfde wet op dezelfde positie.

## Claim (2026-09-23) — aangescherpte acceptatie

Branch `card/aantal-zonder-bron`, basis `main` `91880111`.

1. Hermeting van de vier-probe-tabel op de huidige stand, en de
   AANTAL-sweep over golden + rood + negatief + pariteit opnieuw — er is
   sinds 2026-09-20 veel geland (WF-110 de instance-set-unie onder meer).
2. Spec-first: een WF-037-arm "een telling MOET een elementtype dragen"
   (IR-SPEC-tekst + verifier-arm, beide VOORSTEL); het nummer motiveren.
3. Rood-eerst, drie negatiefs: ariteit 0, ariteit 1 met een eigen lezing,
   ariteit 1 met een `ListLiteral`; elk PASS onder de oude verifier (rc 0),
   FAIL onder de nieuwe, met verifier-md5 vóór en na; een arm-exacte mutant
   over golden + rood + negatief + pariteit.
4. `ref`: de waardearm onder `Aantal` wordt onbereikbaar en weigert
   benoemd (fail-fast, tweede verdedigingslinie); de zes
   `WV-3.4a`-getuigen kantelen naar die weigering, rood-eerst gemeten
   tegen de oude `eval.ml`.
5. Clingo en beide Drools-takken ná de arm gemeten; `backends/**` niet
   aangeraakt.
6. De producentkant gemeten: kan `ExpressieBouwer` een telling zonder
   elementtype emitteren, en langs welke zin — zo ja, dichten.
7. Alle veertien poorten groen, elk als losse aanroep, tellingbewegingen
   benoemd.

## Result (2026-09-23)

Optie (a) uitgevoerd. Eén nieuwe arm onder WF-037, drie negatiefs, `ref`
weigert benoemd, en de frontend weigert een LEVENDE producentroute die de
kaart niet kende. Nul beweging in golden, rood en de pariteitszone; alle
veertien poorten groen.

### 1. Het besluit (verbatim)

OPTIE (a) — één arm: een `AANTAL`-telling MOET een elementtype dragen;
ariteit 0 en ariteit 1 zonder elementtype vallen onder één zin, letterlijk
gespiegeld aan de tweede arm die `AANTAL_GEVULDE` sinds 2026-09-20 draagt
('the filled count is an INSTANCE fold and MUST carry an element type').
Motivering: de gevuldtelling-betekenis is per de besluiten van 2026-09-19
(setbron) en 2026-09-20 (opgesomde vorm) al van `AANTAL` weggenomen en
heeft met `AANTAL_GEVULDE` een eigen soort; een bronloze telling telt het
OPERAND en niet de inhoud (de lijstliteraal-meting: antwoord 1 ongeacht de
lijstinhoud, stil) — precies de verboden klasse; de kosten zijn nul
documenten (alle 109 tellingen in de vier zones dragen een elementtype); en
beide tellingsoorten krijgen zo dezelfde wet op dezelfde positie.

### 2. De hermeting (basis `91880111`)

De vier-probe-tabel reproduceert woordelijk. Probes afgeleid van
`wf037-aantal-opgesomd` (J1 met `gewogen vangst` 3.00, J2 leeg):

    elementtype ariteit  verifier  ref        clingo            Drools exec / DRL
    null        1 eigen  PASS      J1 1, J2 0 TRANSLATION FAULT SKIP / MARKER
    null        0        PASS      0, 0       TRANSLATION FAULT SKIP / MARKER
    null        1 lijst2 PASS      1, 1       TRANSLATION FAULT loader ListLiteral (beide)
    null        1 lijst0 PASS      1, 1       TRANSLATION FAULT loader ListLiteral (beide)

**De sweep wijkt af van het besluitcijfer, en dat raakte de arm.** Over
de 743 `ir.json` van golden + rood + negatief + pariteit vóór deze kaart:

    AANTAL  ariteit 1  MET elementtype   AttribuutRef       112
    AANTAL  ariteit 1  ZONDER elementtype InstanceSetUnion   12  (5 goldens + wf110-negatiefs)
    AANTAL  ariteit 1  MET elementtype   InstanceSetUnion     1  wf110-unie-naast-vouwbron
    AANTAL  ariteit >=2 ...                                   4  de opgesomde negatiefs
    AANTAL  ariteit 0/1 zonder elementtype, niet-unie         0

"Alle 109 dragen een elementtype" was waar op 2026-09-20; sinds WF-110
(de instance-set-unie, 2026-09-20/22) staan er twaalf tellingen over een
unie ZONDER elementtype op de wire — en dat MOET: WF-110 (c) VERBIEDT een
elementtype naast een unie, omdat elke selectie haar eigen draagt. Een
letterlijke arm "MOET een elementtype dragen" had vijf goldens rood
gemaakt. De arm heeft daarom één uitgang, een enkel
`InstanceSetUnion`-argument: daar is de bron aanwezig, één knoop lager.
Dat is een technische lezing binnen het besluit, geen verbreding: de
motivering (een bronloze telling telt het operand) geldt niet voor een
telling waarvan de bron in de unie staat. De kostenclaim blijft waar:
nul bestaande documenten bewegen van verdict.

### 3. De arm, en waarom WF-037

    | Aantal, None, [ Instance_set_union _ ] -> []
    | Aantal, None, ([] | [ _ ]) ->
        WF-037 "the count is an INSTANCE fold and MUST carry an element
        type: without one its argument is no fold source at all — and with
        no argument there is none — so the count has no contributors to
        count, only its operand, whatever that operand holds (ch. 4.3)"

Geen nieuw nummer: WF-037 draagt de vorm en ariteit per soort, de
zustersoort draagt precies deze zin op deze regel, en het register zegt
al "an instance fold MUST carry `elementType`". Een tweede nummer zou één
wet over twee regels splitsen. Ariteit ≥ 2 blijft de opgesomde arm en
wordt niet dubbel gemeld (geen meevurende regel op de vier bestaande
opgesomde negatiefs — gemeten, zie §5).

### 4. Rood-eerst, per spelling

Oude verifier: bron-md5 `30105e069c36ed916f1f7bef984bde10`, binary
`ea2811943bebe68c667c88e911728435`. Nieuwe: bron `8ff8078c0a1cb375d8ea232408bfab9b`,
binary `4948e9e50f32989525d4410359ec1567`.

    wf037-aantal-zonder-elementtype  (ariteit 1, eigen lezing)  oud: verifier: PASS rc=0
    wf037-aantal-geen-operand        (ariteit 0)                oud: verifier: PASS rc=0
    wf037-aantal-lijstliteraal       (ariteit 1, ListLiteral)   oud: verifier: PASS rc=0

Nieuw, alle drie met één fout, rc 1:

    WF-037 FAIL /resolvedRegels/0/waarde: the count is an INSTANCE fold
    and MUST carry an element type: without one its argument is no fold
    source at all — and with no argument there is none — so the count has
    no contributors to count, only its operand, whatever that operand
    holds (ch. 4.3)

Poort 2 in expliciete modus over de drie: `3 file(s) validate and
round-trip`.

### 5. Mutanten, over 746 documenten (vier zones)

- Arm uit (`when false`, bron-md5 `67db51d0ed8694a33013dc4b34f494eb`):
  OUD → NIEUW 3 verdicts (de drie negatiefs), 3 rapportregels; NIEUW →
  MUTANT exact diezelfde 3 terug; OUD → MUTANT **0 verdict- en 0
  zinsverschillen**. Arm-exact.
- Unie-uitgang weg (bron-md5 `8588fddb72f556550b708bb681068da1`): vijf
  goldens worden rood (`jutterswet-keurunie`, `-uniekwantor`,
  `-uniestratum`, `-unievoorwaarde`, `-vondstunie`) en vier
  wf110-negatiefs krijgen een meevurende regel. De uitgang is
  lastdragend en door poort 3 getuigd.

### 6. `ref`: de waardearm weigert benoemd (tweede linie)

Elke ingang naar `` `Values `` onder `Aantal` is nu op de wire dicht:
setbron (WF-096), opgesomd ≥ 2 (WF-037, 2026-09-20), ariteit 0/1 zonder
elementtype (WF-037, nu). De arm `agg_aantal (filled vs)` is onbereikbaar
via elk welgevormd document; hij stond er als "defensief" maar antwoordde
stil. Gekozen fail-fast: `Value.fault` met

    a count folds INSTANCES and this node hands it %d value(s) instead — an
    operand list, a count without an element type, or a set source, all
    refused on the wire (WF-037, WF-096): counting them would count the
    operands, or their filled ones, and never a population (ch. 3.4a)

Geen bodemzin: de zin noemt de constructie (een telling die waarden
krijgt) en staat in de `Aantal`-arm alleen. Getuigen, gemeld mandaat: de
zes `WV-3.4a-opgesomd-*`/`-eenoperand-*`/`-geen-operand` kantelen van
`expect_num` (1, 2, 0, 1, 0, 0) naar `expect_fault_msg` op het fragment
"a count folds INSTANCES and this node hands it"; twee erbij,
`WV-3.4a-lijstliteraal-twee` en `-leeg`. Rood-eerst tegen de oude
`eval.ml`: **8 failure(s)**, alle acht "(no fault at all)". Suite 786 → 788.
`RUNTIME-SEMANTICS.md` ch. 1.2 draagt de werkvoorbeelden.

### 7. Clingo en Drools ná de arm

Gemeten op de drie negatiefs: clingo `TRANSLATION FAULT: expression kind
not translated`; Drools exec-model `transpiler SKIP (aggregatie buiten
Phase-C scope)` en DRL-tekst `MARKER TODO aggregatie AANTAL` op de eerste
twee; `loader: … Expr:ListLiteral (unknown expression node)` in beide
takken op de lijstliteraal. Hun weigeringen blijven linie twee;
`backends/**` niet aangeraakt. Geen gate-8/12-lijst beweegt (gate 8: 42
rood, 9 gepind / 33 niet-gedragen; gate 12: pins 180 en 159 ongewijzigd):
geen rode casus en geen golden draagt de vorm.

### 8. De producentkant — een LEVENDE route, gedicht

Gemeten met `make ir` op tien zinnen (model met Jutter, Vondst, een
feittype en een parameter `drempel`), vóór de fix:

    het aantal vondsten van de Jutter            elem Vondst  (controle)
    het aantal gewogen vangst van de Jutter      elem null  AttribuutRef  diagnostics []
    het aantal de gewogen vangst van de Jutter   elem null  AttribuutRef  []
    het aantal (de gewogen vangst van de Jutter) elem null  AttribuutRef  []
    het aantal 3                                 elem null  Literal       []
    het aantal (3 plus 4)                        elem null  Binair        []
    het aantal drempel / de drempel              elem null  ParameterRef  []
    het aantal Vondsten / Jutters                elem null  VariabeleRef  []
    het aantal de Jutter                         elem null  Onopgelost    WARNING (blokkeert niet)

Route: `aantalFunc` neemt elke `primaireExpressie`; `aggregatieElement`
levert null als het operandwoord geen rol of objecttype noemt, en alleen
als de hele zin een gedeclareerde parameter is (`het aantal werkdagen`)
keert `bouwFunctie` terug. Anders ging de knoop zonder elementtype de
wire op. Fix: één ERROR in `ExpressieBouwer.bouwFunctie`, ná de
parameter-terugval en de unie-route, vóór `meldAggregatie`
(`tellingBronFout`). Na de fix bereikt geen van de tien de schijf;
`het aantal werkdagen` blijft een `ParameterRef`, schoon. Ariteit 0 heeft
geen route (de grammatica eist een operand). De grammatica is niet
aangeraakt, zoals bij de opgesomde kaart.

Getuige `AantalZonderBronTest`: 9 geparametriseerde weigeringen + 2
tegenproeven (gewone telling, parameter-terugval). **Twee bestaande
tests gekanteld** in `TelbereikTest` (gemeld mandaat,
`frontends/regelspraak/**`): `een_gedeeld_typemeervoud_bindt_geen_van_beide`
en `een_woord_dat_niets_benoemt_levert_geen_elementtype` pinden dat de
frontend een telling ZONDER elementtype emitteerde (en de verifier haar
op WF-034 weigerde). Hun eigenschap — er wordt geen type gegokt — blijft
gepind; ze eisen nu daarnaast precies één fout, de bronloze-tellingfout.
Dit maakt deze kaart een HERPIN-kaart (frontendgedrag verandert).

### 9. Poorten, elk als losse aanroep

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  276 documenten, vloer 192
     3 check-verifier           PASS  276 golden schoon; 440 negatief (+3 t.o.v. 437), vloer 350
     4 check-case-schema        PASS  636 casusbestanden
     5 check-oracle             PASS  ref-suite 788 (+2; 6 gekanteld), vloer 489; 594 cases
     6 check-clingo             PASS  suite 484, vloer 83; 594 cases ref = clingo
     7 check-noop               PASS  558 weerleggen; 36 overlevers
     8 check-rood               PASS  42 rood, vloer 26; 84 fragmenten; Drools 9 gepind, 33 niet-gedragen
     9 check-catala             PASS  201 checks, 12 goldens
    10 check-regelspraak        PASS  404 JUnit (+11 AantalZonderBronTest), vloer 173; 70 rgs-goldens byte-identiek
    11 check-toka-parity        PASS  45 .rgs byte-identiek, 27 tests, 29 cases nul divergenties
    12 check-drools             PASS  gespeeld 180 (pin), DRL-tekst 159 (pin), JUnit 604
    13 check-reftype-derivation PASS  131 twists over 27 goldens, 128 replays, 25 DRL-tekst
    14 check-flogic             PASS  130 checks, 2 goldens

Geen vloer aangeraakt.

### 10. Beschermde-padvoorstellen

- `spec/IR-SPEC.md`: de `arguments`-rij van ch. 4.3; de WF-037-alinea in
  het register (de "BELOW two operands … open flank"-zinnen vervangen door
  de nieuwe arm); de WF-037-rij van de tabel (tekst + drie negatiefs).
- `spec/RUNTIME-SEMANTICS.md` ch. 1.2: de restflank-zin vervangen door de
  sluiting plus de werkvoorbeelden van de weigering.
- `spec/verifier/lib/checks.ml`: de arm, en de comment van de opgesomde
  arm ingekort (de open-flankalinea is vervallen).

### 11. Open punten

1. **Een telling MET elementtype maar zonder bron-argument.** Gemeten:
   `elementObjecttype` gezet met ariteit 0, of met één `ListLiteral`, is
   verifier-schoon, en ALLE uitvoerders weigeren (ref `unsupported
   instance-fold source shape`, een bodemzin; clingo TRANSLATION FAULT;
   Drools SKIP/MARKER of loader). Geen uitvoerderskloof, wel een
   welgevormd document dat niemand kan uitvoeren — dezelfde twijfel als
   `wf037-aantal-opgesomd-element`. Buiten dit besluit ("zonder
   elementtype"); kaartvoorstel.
2. "het aantal Vondsten" (een los typemeervoud) wordt nu geweigerd.
   Of de brontaal een telling over ALLE instanties van een type kent, is
   een aparte normvraag; vandaag emitteerde de frontend er een
   `VariabeleRef`-telling van die niets telde.
3. Herpin van de toolchain volgt spec-first (frontendgedrag veranderd).
4. Een attribuut dat "aantal …" heet. "het aantal gewogen posten van de
   Jutter", met een gedeclareerd attribuut `aantal gewogen posten`, krijgt
   nu "telling zonder bron". De ware oorzaak noemt die melding niet: de
   naamterugval in `bouwFunctie` vangt alleen PARAMETERS, geen attributen,
   dus de zin wordt als telling gelezen. Vóór deze kaart ging hij
   ongeblokkeerd als bronloze telling de wire op; nu is hij geweigerd met
   een misleidende reden. De hoofdsessie maakt er een kaart van:
   `20260923-frontend-aantal-attribuutnaam`.

**Dubbele citaties op al-foute vormen (lensronde, genoteerd, geen actie).**
Twee vormen die al om een andere reden geweigerd werden, krijgen nu een
meevurende WF-037-regel: de setbron onder een telling zonder elementtype
citeert WF-096 + WF-037, en een onopgeloste bron zonder elementtype
WF-034 + WF-037. Nul corpusdragers; de elementtypearm van de gevulde
telling gedraagt zich op dezelfde vormen gelijk. Bewust gelaten: de twee
tellingen houden zo dezelfde wet, en het verdict verandert nergens.

### 12. Registervoorstel — `spec/CONSTRUCTS.md` rij 1

Aansluitend op de alinea die met "**En het is een doorsnijding van een
LEVENDE producentroute …**" loopt en eindigt op "`AANTAL_GEVULDE` draagt
de betekenis sinds 2026-09-20 over een setbron.", vóór het blok
"**Landed (2026-09-23, card `temporele-datumvouw`):**". In die alinea
wordt het kaartpad `backlog/new/20260920-aantal-zonder-bron.md` voortaan
`backlog/done/…`.

> **Landed (2026-09-23, card `aantal-zonder-bron`): the count without a
> source leaves the wire too — option (a), "een telling MOET een
> elementtype dragen".** The flank the listed-count decision left open by
> name: a count of ONE operand without an element type, or of none, was
> verifier-clean and `ref` alone answered it — the filled count once more
> on one own read (1 or 0), 0 on no operand, and **1** on a single
> `ListLiteral` whether it held two elements or none, the operand counted
> and not its contents — while clingo and both Drools branches refused.
> WF-037 carries one new arm for both arities, in the sentence the filled
> count's element-type arm already states ("the count is an INSTANCE fold
> and MUST carry an element type"), so both counts carry one law at one
> position; no new number, since WF-037 already holds shape and arity per
> kind. **One exit, measured rather than assumed:** a single
> `InstanceSetUnion` argument, because WF-110 (c) forbids an element type
> beside a union whose selections carry their own — the decision's "all
> 109 counts carry an element type" predated the union's twelve
> element-less counts, and without the exit five goldens turn red. Three
> negatiefs (`wf037-aantal-zonder-elementtype`, `-geen-operand`,
> `-lijstliteraal`), each PASS under the old verifier and FAIL under the
> arm; the arm-off mutant equals the old verifier over 746 documents,
> zero verdict and zero sentence differences. `ref`'s value arm under the
> count, now out of reach through any well-formed document, **refuses by
> name** ("a count folds INSTANCES and this node hands it … value(s)
> instead") instead of answering; the six `WV-3.4a` witnesses tilted to
> that refusal and two list-literal witnesses joined them. **And again a
> live producer route:** the RegelSpraak frontend emitted an `AANTAL`
> with `elementObjecttype: null` for any operand that named no role or
> type — "het aantal gewogen vangst van de Jutter", "het aantal 3", "het
> aantal drempel", "het aantal Vondsten" — nine sentences with
> `diagnostics: []`. It refuses them since this card with a named ERROR at
> the point the element is derived, after the parameter fallback and the
> union route; the grammar is untouched. A HERPIN card. Cost: zero
> existing documents change verdict.
