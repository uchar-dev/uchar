# frontend-typemeervoud-telling: kent de brontaal "het aantal Vondsten"?

Openstaande normvraag uit de aantal-zonder-bron-kaart (2026-09-23,
open punt 2): "het aantal Vondsten" — een los typemeervoud als
telbron, zonder rol- of relatiebinding — wordt sinds de
`tellingBronFout`-ERROR door de frontend geweigerd, waar hij eerst
stil bronloos emitteerde (antwoord: het operand, niet de instanties).
Of de brontaal een telling over ALLE instanties van een type kent
(§13.4.16-lezing), en zo ja langs welke wire-vorm (een
instantiebron met elementtype en documentwijde scope?), is nooit
beslist. Vandaag is weigeren de veilige stand; deze kaart beslist of
er een gedragen vorm moet komen.

## Acceptatie (bij oppakken aanscherpen)

- (a) De brontaalvraag beantwoorden met sectieverwijzingen (v2.3.0
  read-only, niet citeren in artifactzones) en de toolchain-praktijk
  meten (emitteert de toolchain er iets voor in toka/?).
- (b) Bij "ja": de wire-vorm ontwerpen als voorstel (spec-first) en
  de frontend-route bouwen met getuigen; bij "nee": de weigering
  normatief vastleggen met een frontend-getuige die de zin pint.
- (c) Alle veertien gates groen; herpin bij frontend-beweging.

May touch: `frontends/regelspraak/**`, `spec/**` (**voorstel**),
`corpus/**`, kaartverplaatsing.

## Claim (2026-09-23) — aangescherpte acceptatie

Branch `card/frontend-trio` (derde van drie), na
`frontend-bron-annotatie-in-defaults`. Deze kaart raakt `spec/**` en
`corpus/**` niet: bij "ja" komt de wire-vorm als voorstel in het Result,
bij "nee" blijft de weigering staan.

1. De brontaalvraag beantwoorden met sectieverwijzingen naar RegelSpraak
   v2.3.0 (hoofddocument en typeringen, read-only, niet geciteerd): kent
   de taal een telling over ALLE instanties van een type?
2. De toolchain-praktijk meten: de toolchain-IR in `toka/` (read-only,
   geen citaten) op de wire-vormen van de telling, en de toolchain-resolver
   op dezelfde zin.
3. Bij "ja": de wire-vorm als voorstel (instantiebron, elementtype,
   documentwijde scope; wat IR-SPEC, schema, verifier, ref, clingo en
   Drools moeten dragen), niet gebouwd, en de weigering gepind tot hij
   landt. Bij "nee": de weigering normatief vastleggen met een
   JUnit-getuige die de zin pint en een `DIVERGENCES.md`-notitie.
4. Rood-eerst, mutatieproef per voorwaarde met cmp-herstel.
5. Gate 10 byte-identiek (70 rgs-goldens), alle veertien gates groen, elk
   als losse aanroep.

## Result (2026-09-23)

Antwoord: **nee**. RegelSpraak v2.3.0 kent geen telling over alle
instanties van een type, en de toolchain heeft er evenmin een vorm voor.
De weigering blijft staan en is normatief vastgelegd: een eigen zin die
het type, de norm en de uitweg noemt, gepind door
`TypemeervoudTellingTest`, met een notitie in `DIVERGENCES.md`. Geen
wire-vorm, geen wijziging in spec, schema of corpus; alleen de tekst van
een ERROR verandert. Alle veertien gates groen. Toch een HERPIN-kaart:
het vendored slice beweegt.

### 1. De brontaalvraag (v2.3.0, read-only, geparafraseerd)

- §13.4.16 is in v2.3.0 de productie `<berekening>` (de binaire
  rekenkunde) en zegt niets over een telling; de "§13.4.16-lezing" van de
  kaart valt niet op die nummering. De vraag wordt beslist door §5.5,
  §5.8, §8.1.6, §13.4.1 en §13.4.17.
- §5.8.1 en §13.4.17.46/.47: "het aantal" is een getalaggregatiefunctie
  over één expressie. Typeringen §4.11: alleen onderwerpexpressies, een
  concatenatie daarvan toegestaan, leegwaarden niet van toepassing.
- §5.8: een aggregatie neemt een meervoudige invoer en somt op wat
  meervoudig kan zijn: een concatenatie (§5.7), een selectie waarvan het
  object meervoudig is of die een meervoudige rol selecteert (§5.5.2), een
  subselectie (§5.6).
- §5.5.1, §5.5.3 en §13.4.1: een objecttype staat in een onderwerpketen
  alleen enkelvoudig — na een onbepaald lidwoord als universeel onderwerp,
  waarop de regel per instantie wordt uitgevoerd, of na een bepaald
  lidwoord als verwijzing daarnaar. De meervoudsvorm van het objecttype
  heeft in een onderwerpketen geen plaats. §3.1 en §5.5.3: een regel
  verwijst nooit naar één bepaalde instantie en heeft altijd een
  universeel onderwerp.
- §5.5.5: een kwantificatie als "alle" staat voor een rolselector in een
  keten die aan een onderwerp hangt (in Jutterswet-woorden: "alle vondsten
  van de Jutter").
- §8.1.6 (uniciteit, syntaxis in §9.5): de enige constructie met een
  documentbrede reikwijdte over een type, een bijzondere onderwerpexpressie
  "van alle <objecttype>" in plaats van een lidwoord — alleen in een
  consistentieregel, als enkel criterium, met het predicaat "uniek zijn".
  Geen aggregatie-operand.
- Elke "het aantal …"-zin in het hoofddocument, het casusdocument en het
  typeringendocument is een roltelling of de naam van een attribuut of
  parameter. Geen enkele telt een type.

### 2. De toolchain-praktijk

- `toka/` (read-only, niet geciteerd): 30 toolchain-gepinde `ir.json`,
  daarin 4 `AANTAL`-knopen, alle vier over een meervoudige rol vanaf het
  onderwerp (een `AttribuutRef`-argument met een `elementObjecttype`). Nul
  typetellingen. De 47 casusbronnen schrijven twee tellingszinnen,
  allebei roltellingen.
- De resolver van de toolchain (regelspraak-java, de build onder de
  werkbank, frontend van vóór de overname) op de probe met Jutter, Vondst
  en feittype vondstaanmelding: "het aantal Vondsten" geeft `WARNING
  dekking: … MARKER — TODO aggregatie AANTAL` en exit 3 zonder
  `--allow-markers`; "het aantal vondsten van de Jutter" in dezelfde probe
  wordt een accumulate over de aanmelder-relatie.
- Breder: 513 `.rgs` in de werkbank en 176 in regelspraak-java dragen nul
  "het aantal <hoofdletterwoord>" in regeltekst (één treffer, in een
  commentaar).

### 3. De keuze (verbatim)

**NEE.** RegelSpraak v2.3.0 kent geen telling over alle instanties van een
objecttype. De telling neemt een meervoudige onderwerpexpressie, en elke
meervoudige vorm hangt aan een onderwerp dat de regel per instantie
bindt; een objecttype staat in een onderwerpketen alleen enkelvoudig. De
enige documentbrede reikwijdte is die van de uniciteitscontrole, en die
is geen aggregatie. De weigering die sinds de bronloze-tellingarm van
WF-037 bestaat, blijft daarom staan als norm en niet als voorlopige stand:
er komt geen wire-vorm, en de frontend zegt waarom. Motivering: de spec
(§1 hierboven) kent de vorm niet; de toolchain draagt hem niet (marker,
exit 3) en geen officiële casus gebruikt hem; een wire-vorm zou dus een
taalverbreding boven v2.3.0 zijn en geen gat in de frontend, en dat is een
normbesluit met een specwijziging erachter (open punt 1), geen
frontendkaart.

### 4. De vastlegging

De oude zin ("telling zonder bron: … noemt geen instanties om te tellen
…") is onwaar voor wie een populatie noemde. De nieuwe zin, op de probe:

    ERROR (line 16): telling zonder bron, over een objecttype: 'het aantal
    Vondsten' noemt het objecttype Vondst zonder onderwerp waarvandaan het
    geteld wordt, en zou zo alle instanties van Vondst in het document tellen
    — die telling kent de brontaal niet. RegelSpraak v2.3.0 telt de instanties
    van een meervoudige onderwerpexpressie (§5.8.1, typeringen §4.11);
    meervoudig is alleen een concatenatie, een selectie met een meervoudig
    object of een meervoudige rol, of een subselectie (§5.8), en elk daarvan
    hangt aan een onderwerp dat de regel per instantie bindt (§5.5.3). Een
    objecttype staat in een onderwerpketen alleen enkelvoudig: als universeel
    onderwerp of als verwijzing daarnaar (§5.5.1, §13.4.1). De enige
    documentbrede reikwijdte, 'van alle <objecttype>', hoort bij de
    uniciteitscontrole (§8.1.6) en is geen aggregatie. Tel via een rol vanaf
    het onderwerp ('het aantal <rol> van <onderwerp>', §5.5.5) — vanaf Jutter
    is dat de rol 'vondsten' (feittype vondstaanmelding) (WF-037, IR-SPEC
    ch. 4.3)

De zin begint met "telling zonder bron", zoals de attribuutbotsing van de
kaart frontend-aantal-attribuutnaam, zodat `AantalZonderBronTest`
ongewijzigd groen blijft. Alleen een operand die naar niets gedeclareerds
oploste (een kale `VariabeleRef` of een `Onopgelost`) komt in aanmerking.
De rol in de uitweg wordt alleen genoemd als precies één rol het type
vanaf het onderwerp bereikt. Code: `ExpressieBouwer.tellingOverObjecttype`,
in `tellingBronFout` direct na de attribuutbotsing. De grammatica is niet
aangeraakt.

Gemeten per spelling, vóór (build na de eerste kaart) en na:

    spelling                               vóór                         na
    het aantal Vondsten                    ERROR generiek               ERROR typezin + rol 'vondsten'
    het aantal alle Vondsten               ERROR generiek + WARNING     ERROR typezin + WARNING (marker)
    het aantal de Vondsten                 ERROR generiek + WARNING     ERROR typezin + WARNING (marker)
    het aantal Vondst                      ERROR generiek               ERROR typezin
    het aantal vondsten                    ERROR generiek               ERROR typezin
    (het aantal Vondsten)                  ERROR generiek               ERROR typezin
    het aantal (Vondsten)                  ERROR generiek               ERROR typezin
    het aantal Jutters (Jutter-regel)      ERROR generiek               ERROR typezin, zonder rol
    het aantal aanmelders                  ERROR generiek               ongewijzigd (noemt geen type)
    het aantal Vondsten die gewogen zijn   ERROR deelselectie + WARNING ongewijzigd (andere parse)
    het aantal vondsten van de Jutter      schoon, AANTAL elem Vondst   ongewijzigd

### 5. Rood-eerst en mutatie

    tegen de oude ExpressieBouwer (md5 15927e17509ad83b990e3a1d3961bd21):
      10 tests, 3 successful (de drie tegenproeven), 7 failed; elk van de
      zeven kreeg "telling zonder bron: '…' noemt geen instanties om te tellen …"
    na de fix:                                 10 successful
    mutant T1 (typezin nooit):                 7 failed (de zes spellingen en "Jutters")
    mutant T2 (opgeloste-operandwacht weg):    1 failed (de parameter "vondsten" kreeg de typezin)
    mutant T3 (uitweg zonder rol):             6 failed (de zes spellingen)
    elke mutant teruggezet met cp, cmp gelijk (md5 fd075975d5508f716a077957cce4c814,
    vóór de woordverfijning van commit 9c7c8c59)

### 6. Gates, elk als losse aanroep na `eval $(opam env)`

     1 check-self-contained     PASS
     2 check-ir-schema          PASS  278 documenten, vloer 192
     3 check-verifier           PASS  278 golden schoon; 440 negatief, vloer 350
     4 check-case-schema        PASS  638 casusbestanden
     5 check-oracle             PASS  ref-suite 806, vloer 489; 595 cases, vloer 427
     6 check-clingo             PASS  suite 523, vloer 83; 595 cases ref = clingo
     7 check-noop               PASS  559 weerleggen; 36 overlevers
     8 check-rood               PASS  43 rood, vloer 26; 86 fragmenten; Drools 9 gepind, 34 niet-gedragen
     9 check-catala             PASS  201 checks, 12 goldens
    10 check-regelspraak        PASS  431 JUnit (421 + 10), vloer 173; 70 rgs-goldens byte-identiek
    11 check-toka-parity        PASS  45 .rgs byte-identiek, 27 tests, 29 cases nul divergenties
    12 check-drools             PASS  gespeeld 182 (pin), DRL-tekst 161 (pin), JUnit 620
    13 check-reftype-derivation PASS  131 twists over 27 goldens, 128 replays, 25 DRL-tekst
    14 check-flogic             PASS  130 checks, 2 goldens

Gates 1, 10 en 11 opnieuw na de woordverfijning (9c7c8c59): PASS, 431 / 70
/ 45. Geen vloer of pin aangeraakt.

### 7. Herpin

Het vendored frontend-slice beweegt: `ExpressieBouwer.java`,
`DIVERGENCES.md` en de nieuwe test. Geen emissie van een welgevormd
document verandert. De herpin in regelspraak-java doet de hoofdsessie na
de merge.

### 8. Registervoorstel — `spec/CONSTRUCTS.md` rij 1

Aansluitend op het blok "**Follow-up (2026-09-23, card
`frontend-aantal-attribuutnaam`)** …", als laatste alinea ervan:

> **Follow-up (2026-09-23, card `frontend-typemeervoud-telling`): a count
> over all instances of a type is not RegelSpraak, and the refusal says
> so.** "het aantal Vondsten" — an object type's plural with nothing it
> is counted from — has been refused since the source-less-count arm, with
> a sentence that called the operand instance-less. Measured against
> v2.3.0: the count takes a plural subject expression (§5.8.1, typeringen
> §4.11); §5.8 lists the plural forms, each hanging on a subject the rule
> binds per instance (§5.5.3); an object type stands in an onderwerpketen
> only in the singular (§5.5.1, §13.4.1); the one document-wide range over
> a type is the uniqueness check's "van alle <objecttype>" (§8.1.6), which
> is no aggregation. The source toolchain leaves the sentence a marker and
> emits nothing, and no official casus counts a type. So no wire form
> follows. The frontend's refusal now names the type, the norm and the way
> out — a count through a role from the subject, naming the role when
> exactly one reaches the type — for every spelling that reaches it, and
> it leaves a parameter or variable that shares a type's name on the
> generic sentence. No emission changes; a HERPIN card.

### 9. Open punten

1. Een telling over alle instanties als dialectuitbreiding ("het aantal
   alle Vondsten") zou een taalverbreding boven v2.3.0 zijn. De wire heeft
   er een halve vorm voor: een telling met `elementObjecttype` en zonder
   bronargument is verifier-schoon en wordt door geen uitvoerder
   uitgevoerd (open punt 1 van de kaart aantal-zonder-bron). Dat is een
   normbesluit van de maintainer met een specwijziging erachter; geen
   bron vraagt erom (nul treffers in 689 werkbank- en toolchainbronnen).
2. "het aantal Vondsten die gewogen zijn" parseert anders: de woorden vóór
   "die" worden één naam, en de zin wordt geweigerd door het
   deelselectiehek van §5.6 met een eigen melding plus een markerwaarschuwing
   op die naam. Luid, maar de genoemde reden is de subselectie en niet de
   typetelling.
3. Een kaal rolwoord dat geen type noemt ("het aantal aanmelders", zonder
   anker) houdt de algemene zin; de ontbrekende verankering noemt die zin
   niet.

## Vierde onderdeel (hoofdsessie, 2026-09-23): de venstergrens en WF-111's ⊑

Opdracht van de hoofdsessie na de drie kaarten, als verbreding van
may-touch binnen `frontends/regelspraak` (geen kaartje; eigen commit
`af624b51`). Vastgelegd bij deze kaart omdat zij er het dichtst bij ligt:
ook hier trekt de frontend een hek op dat een wireregel van 2026-09-23
spiegelt. Aanleiding: de Drools-vouwtoets-kaart zag dat
`Tijdrekening.periodeGrensLowerbaar` het objecttype van een venstergrens
op identiteit vergelijkt, waar WF-111 `S ⊑ O` toelaat. Die helper staat
in `frontends/regelspraak` (pakket `kern`) maar wordt alleen door de
twee Drools-takken aangeroepen.

### De meting (build vóór de fence, `1c71ecf3`; model met `Klipjutter is een Jutter`)

    grens (subject: bound)                                   frontend            verifier
    Klipjutter: vanaf zijn peildatum (geërfd)                schoon, op Klipjutter  PASS
    Klipjutter: vanaf de peildatum van de Klipjutter         schoon, op Klipjutter  PASS
    Klipjutter: vanaf zijn klipdatum / Jutter: zijn peildatum schoon                PASS
    Klipjutter: vanaf de peildatum van de Jutter             [] geschreven, op Jutter  WF-053 (geen feittype)
      idem, met een feittype Klipjutter–Jutter               [] geschreven, bindingen  WF-111 (genavigeerd)
    Jutter: vanaf de klipdatum van de Klipjutter             [] geschreven          WF-111 + WF-053
    Jutter: vanaf de peilparameter                           [] geschreven          WF-111
    Jutter: vanaf de opendatum van zijn strandvak            [] geschreven          WF-111 (genavigeerd)
    Jutter: vanaf zijn meldmoment (datum-tijd)               [] geschreven          WF-111 (dagprecisie)
    Jutter: vanaf zijn peildatum plus 1 dg                   [] geschreven          WF-111 (berekend)
    Jutter: vanaf zijn knipdatum (tijdlijn)                  [] geschreven          WF-111 (tijdsafhankelijk)
    Jutter: vanaf dd. 1-7-2026 08:30:00.000                  [] geschreven          WF-111 (dagprecisie)
    Jutter: van dd. 01-01-2024 tot de peilparameter          [] geschreven          WF-111 (tot-grens)
    Jutter: vanaf dd. 01-01-2024 / tot en met zijn peildatum schoon                 PASS

**De ⊑-vraag zelf: geen gat in de helper.** Deze frontend schrijft een
eigen lezing, ook een geërfde, met het onderwerp als objecttype. Voor elk
document dat hij maakt, geven identiteit en ⊑ dus hetzelfde antwoord, en
leest `periodeGrensLowerbaar` de facetten door de keten (de bestaande
`TijdrekeningTest`). Een grens met een ander objecttype zendt hij uit als
navigatie, en de verifier weigert die (tabel). De helper is niet
aangeraakt.

**Wel een gat: de frontend had geen WF-111-hek.** Elke vorm die WF-111
weigert, ging met `diagnostics: []` de schijf op, de subtyperichting
("andersom") inbegrepen. Gedicht: `RuleChecker.periodeGrensFout`, een
ERROR per grens die de vorm noemt, aangeroepen in `bouwPeriode` (beide
schrijfwijzen: de losse periodevergelijking en de "het is de
periode"-bullet). "Eigen" vergelijkt ook deze fence op identiteit, om de
reden hierboven; met ⊑ zou hij de supertype-lezing doorlaten naar een
verifier die haar weigert. `DIVERGENCES.md` kreeg een sectie "§8.4.3 A
time window's bounds — narrowed to the wire's two shapes": de brontaal
laat hier elke datumexpressie toe (§13.4.15.68), en de frontend kiest de
twee vormen van de wire.

### Rood-eerst en mutatie

Getuige `PeriodegrensTest` (nieuw bestand): tien weigeringen, één per
vorm plus de tot-grens, en zeven schone vormen.

    tegen de oude RuleChecker (md5 b9e6b9626db152d022099131a727ae74):
      17 tests, 7 successful, 10 failed — alle tien "kreeg: [] ==> expected: <1> but was: <0>"
    na de fix:                                    17 successful
    mutant M1 (fence uit):                        10 failed
    mutant M2 (⊑ in plaats van identiteit):       1 failed (de supertype-lezing gaat door)
    mutant M3 (dagprecisie uit):                  2 failed (datum-tijd-attribuut en -literaal)
    mutant M4 (tijdsafhankelijkheid uit):         1 failed
    elke mutant teruggezet met cp, cmp gelijk (md5 b304844d2b62d7692fea1d54529d0ca1)

### Gates op `af624b51`, elk als losse aanroep

Alle veertien PASS; de getallen zijn die van de derde kaart, behalve gate
10: 448 JUnit (431 + 17), 70 rgs-goldens byte-identiek. Gate 11: 45 .rgs
byte-identiek. Gate 12: gespeeld 182, DRL-tekst 161, JUnit 620. Geen bron
in welke zone ook schrijft een venster met een geweigerde grens (28
venstergrenzen over golden, rood, negatief en pariteit: 24 literalen, 3
eigen attributen, 1 parameter in een negatief; geen `.rgs` met een
periodevergelijking), dus niets beweegt. Het vendored slice beweegt:
HERPIN.

### Registervoorstel — `spec/CONSTRUCTS.md` rij 11

Aansluitend op de WF-111-zinnen in de deltakolom:

> **Producer side (2026-09-23, main-session follow-up after the card
> `frontend-typemeervoud-telling`): the RegelSpraak frontend fences the
> window bound to WF-111's two shapes.** §13.4.15.68 admits any date
> expression as a bound, and the frontend built every one into a window:
> a parameter, a navigated read, a datum-tijd, a computed date, a
> time-dependent attribute and another type's attribute each reached disk
> silently and were refused by the verifier. They are now an ERROR naming
> the shape. "Own" is compared by identity with the subject, as the Drools
> gate does: the frontend writes an own read, an inherited one included,
> on the subject, and emits a read naming any other type, a supertype
> included, as a navigation the verifier refuses. No document moves.

### Open punten van dit onderdeel

1. **Een splitsing in de wireregels, niet in de frontend (beschermd pad,
   voor de hoofdsessie).** Handgeschreven, zonder bindingen: een
   ongenavigeerde `AttribuutRef` met als objecttype een echt supertype van
   het onderwerp, terwijl een feittype de twee types verbindt. De verifier
   laat haar door (`verifier: PASS`) langs twee lezingen die elkaar
   tegenspreken: WF-053 behandelt het naamverschil als navigatie per paar
   en lost die op over het feittype, en WF-111's `is_sub` noemt dezelfde
   knoop eigen. De uitvoerders splitsen, gemeten met een case waarin het
   onderwerp en zijn begeleider verschillende peildata hebben: `ref` leest
   de eigen datum van het onderwerp (PASS, en met de data omgedraaid
   `MISMATCH: K1.premie = leeg`); clingo `TRANSLATION FAULT: the vanaf
   bound of a period condition is neither a date literal nor an own scalar
   date attribute of the subject …`; Drools exec `SKIP`, DRL-tekst `MARKER
   … TODO periodevergelijking-grens`. Te beslissen: wat zo'n knoop
   betekent (eigen, zoals WF-111 en `ref`; of navigatie, zoals WF-053,
   clingo en Drools). Daarna WF-053/WF-111 en de uitvoerders gelijktrekken
   en een negatief of golden voor de gekozen lezing maken.
   `periodeGrensLowerbaar` is bewust niet naar ⊑ gezet: dat zou Drools
   naast `ref` zetten en tegen WF-053 en clingo in.
2. **Een supertypenaam als onderwerpverwijzing, algemeen.** "de bonus van
   de Jutter" in een regel op Klipjutter (een waardepositie, geen venster)
   wordt stil uitgezonden als off-subject-lezing op Jutter, en de verifier
   weigert haar onder WF-053. Het subtypedialect (`DIVERGENCES.md` §5) zegt
   niet of "de Jutter" het onderwerp kan aanduiden wanneer dat een
   Klipjutter is. Buiten de vensterfence, die dit alleen voor grenzen met
   een eigen zin weigert.

### Fixronde 1 (2026-09-23, hoofdsessie)

Open punten 1 en 2 van het vierde onderdeel (de supertype-`AttribuutRef`
die WF-053 als navigatie en WF-111 als eigen lezing leest, met de
uitvoerders gesplitst; en de supertypenaam als onderwerpverwijzing) zijn
normbesluiten voor de verifier- en speclijn. Ze blijven hier staan met
hun metingen; de hoofdsessie neemt ze mee in de kaart
verifier-supertype-dubbele-schrijver. De kleinere open punten van de
branch zijn in deze ronde gesloten: zie de Results van de kaarten
frontend-regelgroep-sluit-bij-uitzondering en
frontend-bron-annotatie-in-defaults.

### Fixronde 2 (2026-09-23): open normpunt en herpinnotitie (lensadvies 9 en 1)

**Open normpunt 3 van het vierde onderdeel: is "scalar" in WF-111 ook
"niet gedimensioneerd"?** Gemeten op de frontend van deze branch: een
kale lezing van een eigen gedimensioneerd datumattribuut als venstergrens
("vanaf zijn peildatum", met `de peildatum Datum in dagen gedimensioneerd
met seizoen`) gaat als `AttribuutRef` op het onderwerp de schijf op. De
WF-111-fence laat haar door, en de verifier ook (`verifier: PASS`). WF-111
noemt alleen "scalar (non-time-dependent)", en de verifier toetst alleen
tijdsafhankelijkheid. Op een case zonder invoer:

    ref    : MISMATCH: J1.premie = leeg, expected 10
    clingo : MISMATCH: J1.premie is underived (leeg), expected 10
    Drools : exec SKIP (voorwaarde buiten Phase-B scope);
             DRL-tekst MARKER TODO periodevergelijking-grens 'zijn peildatum'

Drools weigert elk gedimensioneerd attribuut als grens
(`Tijdrekening.periodeGrensLowerbaar` eist `dimensiesVan(...).isEmpty()`).
`ref` en clingo lezen leeg, en het casusformaat kent geen celinvoer, dus
wat zij met een gevulde cel zouden doen is op de wire niet te toetsen.
Te beslissen voor de verifier- en speclijn: telt een gedimensioneerd
attribuut in WF-111 als niet-scalair (en dan weigeren alle lezers, met
een negatief), of is de kale lezing van een gedimensioneerd attribuut
elders al ongeldig (WF-040 pint alleen `DimensionCellRead`)? De frontend
volgt het besluit; tot dan laat zij door wat de verifier doorlaat.

**Voor de herpin (hoofdsessie).** In regelspraak-java verwacht
`RuleCheckTest.periodevergelijking_met_parametergrens_blijft_eerlijke_marker`
dat "vanaf de PEILDATUM" (een parametergrens) zonder fouten resolvet. Sinds
de WF-111-fence van deze branch (`af624b51`) is een parametergrens een
ERROR. Die test kantelt dus bij de herpin, naar dezelfde weigering die de
verifier al gaf.

### Fixronde 2 (2026-09-23, hoofdsessie; lensadviezen 2, 3, 6 en 8)

**Advies 2, `3f89448e`: het WF-112-hek.** De brontaal laat een
periodebullet onder elke kwantor toe; de wire niet onder 'geen van de'
of een tellende kwantor. Gemeten op `78edd45d`: onder elk van de vier,
en onder een geen-samenstelling binnen een alle-samenstelling, ging het
document met `diagnostics: []` de schijf op, waarna de verifier WF-112
gaf. `RuleChecker.keurVensterposities` loopt nu over elke voorwaarde die
de emitter schrijft, met het bereik van de verifier (ook in expressies
genest), en geeft per samenstelling of kwantorvouw een ERROR die de
kwantor noemt. Onder 'alle' en als eigen voorwaardendeel blijft het
venster schoon. Notitie in `DIVERGENCES.md` §8.4.3.

    getuige VensterKwantorTest, rood-eerst tegen RuleChecker md5
      f29399455116b8c3c099dcb259097b5c: 7 tests, 2 successful, 5 failed (elk "[]")
    mutant W1 (geen en ten minste uit):   3 failed
    mutant W2 (geen recursie):            1 failed (geen binnen alle)
    mutant W3 (ook 'alle' geweigerd):     2 failed (de schone vormen)
    teruggezet met cp, cmp gelijk (md5 c2e56a2f9c606b30e205cd072dfca08c)

**Advies 3, `c424a087`: het regeldoel buiten het onderwerp, en het
gesplitste venster.** Een regel schrijft een attribuut of kenmerk van
zijn eigen universele onderwerp. Gemeten op `78edd45d`:

- "De vakpremie van het strandvak van een Jutter moet gesteld worden op
  10": doel 'vakpremie van het strandvak' op Jutter, omdat de naam de
  navigatie opslokte; `diagnostics: []`, verifier WF-022 en WF-034;
- hetzelfde met "berekend worden als", en voor een ongedeclareerd
  attribuutdoel ("De onbekende premie van een Jutter …");
- "Het strandvak van een Jutter is gemarkeerd": een kenmerkdoel zonder
  objecttype, `diagnostics: []`, verifier WF-022.

`RuleChecker.keurRegeldoelen` geeft elk een ERROR die de vorm noemt. Een
ongedeclareerd kenmerk op een opgelost onderwerp ("Een Jutter is
onbekend", evenzeer stil WF-022) blijft bewust buiten dit hek: dat is het
terrein van de dialectkaart dialect-niet-in-uitzondering ("is niet X"),
die na deze branch in dezelfde resolver werkt. Open punt 4.

De vensterbullet in een samengestelde deelselectie, gemeten: als LAATSTE
bullet sluit de parser de deelselectie af bij "het is de periode" (een
markerkenmerk, alleen een WARNING), en de grens wordt een venster op de
hele regel. Met een literale grens is dat `verifier: PASS`: een ander
document dan er staat, stil op het punt dat ertoe doet. Vooraan in de
lijst leest de parser dezelfde bullet als venster in het filter
(verifier PASS, zoals bedoeld). Een venster achter een enkelvoudige
deelselectie ("… die gewogen zijn vanaf dd. …") is het gewone
regelvenster. Het gesplitste geval weigert nu met een ERROR op de regel;
de grammatica is niet aangeraakt. `DIVERGENCES.md` kreeg de sectie
"§13.4.4 A rule target is written on the subject — narrowed" en een
alinea in §8.4.3.

    getuige RegeldoelTest, rood-eerst tegen RuleChecker md5
      c2e56a2f9c606b30e205cd072dfca08c: 10 tests, 5 successful, 5 failed
    mutant T1 (doelhek uit):                  4 failed
    mutant T2 (attribuutarm uit):             3 failed
    mutant T3 (arm zonder objecttype uit):    1 failed
    mutant T4 (splitsing niet herkend):       1 failed
    teruggezet met cp, cmp gelijk (md5 c7ec34fa545de39d0d517688d9e0ed99)

**Advies 6, `de18d48f`: alleen een meervoudige rol als uitweg.** In een
regel op Vondst noemde de typetellingzin bij "het aantal Jutters" de rol
'aanmelder', terwijl een vondst er één heeft. Nu wordt alleen een
meervoudige rol genoemd. Rood-eerst tegen ExpressieBouwer md5
e3c843a60f7f1561501abf71d466d256: 11 tests, 10 successful, 1 failed;
mutant E (ook enkelvoudig) 1 failed; cmp-herstel (md5
fa3771f6ed9b777fac0497f97ec4c53f).

**Advies 8, `8d6eac7d`:** de spec-voorbeeldfrase in §1 staat nu in
Jutterswet-woorden, en de toka-meting in §2 beschrijft de vorm zonder
casusnamen.

### Gates na de rebase op main `50d22c05`, elk als losse aanroep

     1 check-self-contained     PASS
     3 check-verifier           PASS  283 golden schoon; 441 negatief, vloer 350
    10 check-regelspraak        PASS  475 JUnit, vloer 173; 70 rgs-goldens byte-identiek
    11 check-toka-parity        PASS  45 .rgs byte-identiek; ref, clingo en Drools 29/29
    12 check-drools             PASS  gespeeld 182 (pin), DRL-tekst 161 (pin), JUnit 620

Gate 10 opnieuw na de hash-commit `bf9e2c54` (alleen commentaar en
backlog): PASS, 475 / 70. De rebase liep zonder conflict: main raakte
`frontends/` niet aan.

Registervoorstellen erbij:

> Row 11, after the WF-111 producer sentence: **the window's position
> too (WF-112).** A window under 'geen van de' or a counting quantifier —
> a composite member at any depth, or a quantifier fold's filter or test —
> reached disk silently and was refused by the verifier; the frontend now
> refuses it at the composite, naming the quantifier, over the same reach
> as the verifier's own walk. A window bullet written LAST in a compound
> subselection, which the grammar splits into a marker characteristic and
> a window on the whole rule, is refused as well.

> Row 2, as a follow-up: **a rule target is written on the subject.** A
> target naming another instance's attribute ("De vakpremie van het
> strandvak van een Jutter …"), an undeclared attribute, or a
> characteristic whose subject is a navigation used to reach the wire as
> an unresolved target for WF-022 to refuse; the frontend refuses each by
> name since 2026-09-23. An undeclared characteristic on a resolved
> subject is the negated-decision dialect card's.

### Open punten, bijgewerkt

1. en 2. Ongewijzigd: de supertype-`AttribuutRef` (WF-053 als navigatie,
   WF-111 als eigen lezing) en de supertypenaam als onderwerpverwijzing,
   voor de kaart verifier-supertype-dubbele-schrijver.
3. Het dimensie-grenspunt (hierboven, fixronde 2, advies 9).
4. Nieuw: een ongedeclareerd kenmerkdoel op een opgelost onderwerp ("Een
   Jutter is onbekend.") gaat nog stil de wire op (verifier WF-022). Het
   hoort bij de dialectkaart dialect-niet-in-uitzondering, die de
   kenmerktoekenning herschrijft.
