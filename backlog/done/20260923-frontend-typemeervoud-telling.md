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
  keten die aan een onderwerp hangt ("alle passagiers van de Vlucht").
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
  daarin 4 `AANTAL`-knopen, alle vier over een rol (een
  `AttribuutRef`-argument over de rol "passagiers", `elementObjecttype`
  "Natuurlijk persoon"). Nul typetellingen. De 47 casusbronnen schrijven
  twee tellingszinnen, allebei roltellingen.
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
    vóór de woordverfijning van commit cc4f5c29)

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

Gates 1, 10 en 11 opnieuw na de woordverfijning (cc4f5c29): PASS, 431 / 70
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
