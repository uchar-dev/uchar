# `attribuutGranulariteit` met waarde null — de zusternaad van WF-059's tweede arm

Goal: beslissen wat een `attribuutGranulariteit`-entry met waarde `null`
betekent, en de vier lezers weer op één lijn brengen. **Lage prioriteit,
uitdrukkelijk**: de meting hieronder laat zien dat dit *niet* de klasse van
WF-059's tweede arm is — er is geen stil verkeerd antwoord. Wat er wél is: een
melding die zijn eigen document tegenspreekt, en twee uitvoerders die dezelfde
wire-fout verschillend diagnosticeren.

Afgesplitst van `20260906-gedurende-kwalificatie-stiltes` (geland 2026-09-09,
fixronde 2026-09-10). Die kaart sloot `kenmerkGranulariteit` — schema én
verifier — en liet deze zustertabel uitdrukkelijk liggen, omdat weigeren op het
bewijs van een zustertabel over-afwijzing zou zijn. De ontbrekende meting is nu
gedaan en staat hieronder; dat is precies wat de kaart als voorwaarde noemde.

## De naad

Identiek van vorm aan de gesloten zuster. Een `attribuutGranulariteit`-entry
verklaart zijn attribuut **temporeel door het bestaan van zijn SLEUTEL** en
noemt het raster **in zijn WAARDE**, en de lezers beslissen niet op hetzelfde:

| lezer | plek | beslist op |
| --- | --- | --- |
| verifier | `spec/verifier/lib/checks.ml`, `attr_temporal` | sleutelbestaan |
| `ref` (caselezer) | `ref/lib/case.ml`, de attribuut-arm (`Some (Some gran)`) | de **waarde** |
| ASP-backend | deelt die caselezer | de **waarde** |
| Drools | `attribuutGranulariteit.get(...)` / `containsKey` | wisselend |

## De meting (2026-09-10, uitgevoerd in de fixronde)

Één waarde op `null` gezet, verder niets gewijzigd.

**(a) Op een pro-rata basis** (`anchor/jutterswet-borgdeel`, attribuut
`borgtarief`):

    schema    VALIDEERT            (null staat nog in de attribuut-enum)
    verifier  WF-056 FAIL          "pro-rata base 'borgtarief' is not a temporal attribute"
    ref       CASE ERROR           "a timeline value on a non-temporal attribute"
    clingo    CASE ERROR           idem

De verifier vángt hem hier — maar via WF-056, en met een reden die iets anders
zegt dan wat er mis is.

**(b) Op een gewone clip-basis** (`jutterswet-tegenwacht`, attribuut
`wachtvergoeding per maand`), waar WF-056 niet reikt:

    schema    VALIDEERT
    verifier  PASS                 <-- stil
    ref       CASE ERROR           "a timeline value on a non-temporal attribute"
    clingo    CASE ERROR           idem

De melding **spreekt het document tegen**: het verklaart het attribuut temporeel
(de sleutel staat er), en de lader zegt dat het niet-temporeel is.

**(c) Hetzelfde document, attribuut SCALAIR geleverd** — de vorm die de lader
wél aanneemt:

    ref       EVALUATION FAULT     "temporal attribute "wachtvergoeding per maand" without a granularity"
    clingo    TRANSLATION FAULT    "the gedurende clip of "wachtvergoeding per maand" needs the full timeline; ..."

Twee uitvoerders, twee verschillende diagnoses van hetzelfde wire-defect.

**Wat er NIET is: een stil verkeerd antwoord.** Dat is het verschil met
`kenmerkGranulariteit`, waar de meting 60 en 120 opleverde waar 30 en 30 gepind
stond, in beide uitvoerders identiek. Hier sterft elk pad luid. Vandaar de lage
prioriteit — dit is een diagnose- en wire-hygiënekwestie, geen correctheidsgat.

**Corpusdekking:** 0 van de 572 documenten over de vier zones én de
pariteitszone draagt een `attribuutGranulariteit` met waarde `null` (geteld
2026-09-10 op de kop van `card/gedurende-kwalificatie-stiltes`). De sluiting kost dus, net als bij de zuster, geen enkele regressie
— dat moet bij het landen opnieuw geteld worden, niet overgeschreven.

## De vraag

Het antwoord ligt voor de hand en is daarom niet vrijblijvend: **sluit hem
zoals de zuster gesloten is** — `null` uit de enum in
`spec/schema/regelspraak-ir-v0.schema.json` én een derde arm onder WF-059 — of
motiveer waarom deze zijtabel anders behandeld mag worden dan de vorige. Er is
één echte deelvraag:

- Moet de **melding** van de caselezer ook mee? "a timeline value on a
  non-temporal attribute" is onjuist op een document dat het attribuut wél
  temporeel verklaart. Met de enum dicht is dat pad onbereikbaar via de wire,
  maar de lezer houdt hem voor handgebouwde documenten — dan moet hij de juiste
  reden noemen.

## Kernwaarschuwingen

1. **Beweeg schema en verifier samen**, zoals bij de zuster: eenzijdig sluiten
   laat de andere helft open.
2. **De Catala-emitter schrijft `null` voor elke entry van deze map**
   (`frontends/catala/lib/emit.ml`, de `attribuutGranulariteit`-regel), en wat
   dat vandaag onschadelijk maakt is alleen dat de map altijd leeg is. Met de
   enum dicht wordt die dode tak een latent conformiteitsdefect — gate 2 wordt
   rood zodra die frontend ooit een temporeel attribuut modelleert. Precies
   dezelfde omzetting als bij `kenmerkGranulariteit`; benoem hem weer.
3. **Meet vóór je sluit** dat het nog steeds 0 documenten zijn.

## Acceptance

- De vraag beantwoord met reden; bij sluiten: schema én verifier-arm, met een
  `corpus/negatief/wf059-<slug>/` erbij.
- Rood eerst: de getuige faalt aantoonbaar vóór de wijziging.
- De meting hierboven **opnieuw gedaan** en genoteerd; gate 2 vóór en ná.
- Alle dertien gates groen.

May touch: `spec/schema/**` (**voorstel** — beschermd pad), `spec/verifier/**`
(**voorstel**), `spec/IR-SPEC.md` (**voorstel**), `ref/**`,
`backends/clingo/**`, `corpus/negatief/**`, `backlog/`.

## Out of scope

- `kenmerkGranulariteit`: gesloten 2026-09-09 (WF-059, tweede arm).
- De `attributen`-map met datatype `null`: dat is WF-059's **eerste** arm, al
  gesloten.

## Result (2026-09-10)

**Antwoord: gesloten zoals de zuster.** `null` uit de
`attribuutGranulariteit`-enum in `spec/schema/regelspraak-ir-v0.schema.json`
(voorstel, beschermd pad) én een **derde arm** onder WF-059 in
`spec/verifier/lib/checks.ml` (voorstel) met de bijbehorende clausule- en
registertekst in `spec/IR-SPEC.md` (voorstel). Getuige:
`corpus/negatief/wf059-attribuut-zonder-granulariteit/`. De deelvraag is met ja
beantwoord: de caselezer-melding is meeverhuisd.

De reden om te sluiten en niet te motiveren waarom deze zijtabel anders mag: de
naad is structureel identiek en de kosten zijn nul (0 documenten). Dat de
FOUTKLASSE anders is — geen stil verkeerd antwoord, wel een verkeerde diagnose —
verandert niets aan het argument om te sluiten; het verandert alleen het bewijs
waarop de arm sluit, en dat bewijs is nu de meting hieronder in plaats van die
van de zuster. Zo blijft de "geen over-afwijzing op andermans bewijs"-regel
overeind.

### De hermeting (2026-09-10, op deze branchkop, pre-arm binary md5 `f55fdba98ccd5ac68bb99492519aa580`)

Alle drie de vormen reproduceren letterlijk wat de kaart noteerde. Eén
granulariteitswaarde op `null`, verder niets gewijzigd (één-veld-diff,
geverifieerd met `diff`).

**(a)** `anchor/jutterswet-borgdeel`, attribuut `borgtarief`:

    schema    check-ir-schema: PASS (schema valid; 1 file(s) validate and round-trip, floor 192)
    verifier  WF-056 FAIL /resolvedRegels/0/waarde/argumenten/0: pro-rata base 'borgtarief' is not a temporal attribute
    ref       CASE ERROR: J1.borgtarief: a timeline value on a non-temporal attribute
    clingo    CASE ERROR: J1.borgtarief: a timeline value on a non-temporal attribute

**(b)** `jutterswet-tegenwacht`, attribuut `wachtvergoeding per maand`
(case1-overlap):

    schema    check-ir-schema: PASS (schema valid; 1 file(s) validate and round-trip, floor 192)
    verifier  verifier: PASS                       <-- stil, exit 0
    ref       CASE ERROR: J1.wachtvergoeding per maand: a timeline value on a non-temporal attribute
    clingo    CASE ERROR: J1.wachtvergoeding per maand: a timeline value on a non-temporal attribute

**(c)** hetzelfde document, attribuut SCALAIR geleverd:

    ref       EVALUATION FAULT: temporal attribute "wachtvergoeding per maand" without a granularity
    clingo    TRANSLATION FAULT: the gedurende clip of "wachtvergoeding per maand" needs the full timeline; "J1" supplies only the                asOfDate-bucket scalar

Bevestigd: nergens een stil verkeerd antwoord. Elk pad sterft; wat fout is, is
de diagnose.

### Rood eerst

De getuige is de (b)-vorm, één veld van `corpus/golden/jutterswet-tegenwacht`:

    $ diff corpus/golden/jutterswet-tegenwacht/ir.json corpus/negatief/wf059-attribuut-zonder-granulariteit/ir.json
    33c33
    <         "wachtvergoeding per maand": "MAAND"
    ---
    >         "wachtvergoeding per maand": null

Op de pre-arm binary, en daarmee gate 3 rood:

    $ /tmp/…/verifier-pre.exe corpus/negatief/wf059-attribuut-zonder-granulariteit/ir.json
    verifier: PASS
    exit=0

    $ sh scripts/check-verifier.sh    (code onveranderd, getuige aanwezig)
    FAIL: negatief case corpus/negatief/wf059-attribuut-zonder-granulariteit/ir.json passed the verifier (must violate WF-059):
    verifier: PASS
    gate3 exit=1

Ná de arm, en niets anders dan WF-059:

    $ ./spec/verifier/_build/default/bin/verifier.exe corpus/negatief/wf059-attribuut-zonder-granulariteit/ir.json
    WF-059 FAIL /objecttypen/Jutter/attribuutGranulariteit/wachtvergoeding per maand: attribute declared time-dependent carries no granularity: the key makes the temporal readers treat it as a timeline while the case loader reads the value and refuses that timeline
    exit=1

De wire is óók dicht — dezelfde twee meetdocumenten, ná de schemawijziging:

    $ sh scripts/check-ir-schema.sh /tmp/…/b-tegenwacht.ir.json
    FAIL: …/b-tegenwacht.ir.json
      at: objecttypen/Jutter/attribuutGranulariteit/wachtvergoeding per maand
      None is not one of ['DAG', 'WEEK', 'MAAND', 'KWARTAAL', 'JAAR']

En (a) wordt nu bij de juiste naam genoemd, met WF-056 ernaast:

    WF-059 FAIL /objecttypen/Jutter/attribuutGranulariteit/borgtarief: attribute declared time-dependent carries no granularity: …
    WF-056 FAIL /resolvedRegels/0/waarde/argumenten/0: pro-rata base 'borgtarief' is not a temporal attribute

### De nieuwe caselezer-melding, letterlijk

`ref/lib/case.ml`, de attribuut-arm, tak `Some None` (sleutel aanwezig, waarde
null):

    CASE ERROR: J1.wachtvergoeding per maand: the attribute is declared time-dependent but its granularity is null: the timeline has no grid to sit on (WF-059)

Beide uitvoerders geven hem woordelijk identiek (de ASP-backend laadt via
`Uchar_ref.Case.load_file`). De oude tekst blijft staan voor de tak `None` — een
attribuut zónder granulariteitssleutel, als tijdlijn geleverd — en is daar wél
waar; nagemeten op `J1.tegenwachtsom` van het ongewijzigde golden:

    CASE ERROR: J1.tegenwachtsom: a timeline value on a non-temporal attribute   (ref én clingo)

Dat is ook waarom `corpus/negatief/wf034-wees-attribuutgranulariteit/NOTE.md`,
dat die tekst citeert voor de hernoemde-sleutel-meting, ongewijzigd blijft
kloppen.

### Corpustelling (opnieuw geteld, commando genoteerd)

**0** documenten met een `attribuutGranulariteit`-waarde `null` over de vier
zones en de pariteitszone, vóór de arm. Totaal **577** =
166 `corpus/golden` + 26 `corpus/rood` + 355 `corpus/negatief` + 30 `toka`
(de kaart noteerde 572 op een oudere kop; het verschil is aangegroeide
negatief-zone). Op de branchkop is de telling **1**: de getuige van deze arm.
Het commando (recursieve wandeling, niet alleen de objecttype-positie, zodat
geen enkele plek gemist wordt):

    python3 -c '
    import json,pathlib
    zones=["corpus/golden","corpus/rood","corpus/negatief","toka"]
    files=[]
    for z in zones: files += sorted(pathlib.Path(z).rglob("ir.json"))
    def walk(n,hits,path):
        if isinstance(n,dict):
            for k,v in n.items():
                if k=="attribuutGranulariteit" and isinstance(v,dict):
                    for ak,av in v.items():
                        if av is None: hits.append(path+"/attribuutGranulariteit/"+ak)
                walk(v,hits,path+"/"+k)
        elif isinstance(n,list):
            for i,v in enumerate(n): walk(v,hits,path+"/%d"%i)
    tot=0;carry=0
    for f in files:
        tot+=1; h=[]
        walk(json.loads(f.read_text()),h,"")
        if h: carry+=1; print("NULL-CARRIER:",f,h)
    print("total documents:",tot)
    print("documents with an attribuutGranulariteit value null:",carry)
    '

### Poorten, vóór en ná

| poort | vóór | ná |
| --- | --- | --- |
| 1 `check-self-contained` | PASS | PASS |
| 2 `check-ir-schema` | PASS (192 file(s) validate and round-trip, floor 192) | PASS (192 file(s) validate and round-trip, floor 192) |
| 3 `check-verifier` | PASS (192 golden clean; 355 negatief, floor 350) | PASS (192 golden clean; **356** negatief, floor 350) |
| 4 `check-case-schema` | — | PASS (453 case file(s) validate) |
| 5 `check-oracle` | — | PASS (ref suite 489 worked examples, floor 489; 427 cases replayed exactly, floor 427) |
| 6 `check-clingo` | — | PASS (backend suite 83 checks, floor 83; 427 cases replay exactly) |
| 7 `check-noop` | — | PASS (391 cases refute a do-nothing executor; 36 pinned survivors) |
| 8 `check-rood` | — | PASS (26 red cases fault observably in ref and clingo, floor 26) |

Gate 2 is vóór én ná gemeten zoals de kaart eist: **onveranderd 192** — de
getuige leeft in `corpus/negatief`, dat gate 2 niet schema-valideert. Gate 3
gaat met de getuige mee omhoog (355 → 356), ruim boven de vloer van 350. Geen
enkele vloer hoefde te bewegen.

Poorten 9–13 (catala, regelspraak, toka-pariteit, drools, reftype-derivation)
zijn in deze sessie **niet** gedraaid — die vragen catala, JDK/Maven-jars en de
volle differentiaal; de hoofdsessie draait de volle set. Wel een signaal: gate 8
draaide zijn Drools-been mee en gaf PASS (6 fault-pinned, 20 not-carried), en
geen enkele test in de repo pint de oude meldingstekst (volledige grep op de
string over alle zones gedaan; de enige treffers zijn proza:
`wf034-wees-attribuutgranulariteit/NOTE.md`, deze kaart, en de nieuwe NOTE).

### Kernwaarschuwing 2 — benoemd, NIET gefixt

`frontends/catala/lib/emit.ml:289` schrijft een constante `` `Null `` voor
**elke** entry van `attribuutGranulariteit` (precies zoals regel 295 dat voor
`kenmerkGranulariteit` doet). Vandaag onschadelijk, en aantoonbaar zo:
`frontends/catala/lib/translate.ml:1460` zet `attribuut_granulariteit = []`, dus
de map is altijd leeg en de tak is dood. Met de enum dicht is dat een **latent
conformiteitsdefect**: gate 2 wordt rood zodra die frontend ooit een temporeel
attribuut modelleert. Precies dezelfde omzetting als bij de zuster, en nu op
béíde zijtabellen. `frontends/catala` staat niet in May touch, dus hier alleen
gemeld en in de IR-SPEC-clausule vastgelegd zodat het latere rood als verwacht
gelezen wordt, niet als regressie.

De andere emitter is nagemeten en veilig:
`resolver/DefinitionResolver.java:144` schrijft alleen bij
`if (g != null)`, dus de RegelSpraak-frontend kan de vorm niet produceren —
dezelfde garantie als bij `kenmerkGranulariteit`.

### Wat bewust niet bewoog

- **De (c)-divergentie.** Bij een SCALAIRE levering op een temporeel verklaard
  attribuut houden `ref` ("temporal attribute … without a granularity") en de
  ASP-backend ("the gedurende clip … needs the full timeline") elk hun eigen
  diagnose. Beide zijn waar van het document en beide zijn luid. Ze samenvoegen
  is een andere vraag — wáár een ontbrekende tijdlijn gediagnosticeerd hoort te
  worden — en die vraag stelt deze kaart niet.
- **Geen extra weigering bij de scalaire levering.** De zuster kreeg wel zo'n
  spiegelarm (statische lijst op een temporeel kenmerk); hier niet, want daar
  was de aanleiding een stil verkeerd antwoord en hier sterft dat pad al.
- `spec/RUNTIME-SEMANTICS.md`: nagekeken, noemt `attribuutGranulariteit` nergens,
  dus geen verouderde bewering om te herstellen (en het staat ook niet in May
  touch).
- `spec/CONSTRUCTS.md` en `spec/PLAN.md`: niet aangeraakt (hoofdsessie-serieel).
  **Er is geen CONSTRUCTS-rij nodig**: dit is geen v0→v1-delta maar een
  verstrakking van een bestaande WF-regel, en de zuster-arm van 2026-09-09 heeft
  om dezelfde reden geen rij gekregen. Het register in `spec/IR-SPEC.md` draagt
  de arm (rij WF-059, nu "three declaration arms").

### Gemeld, niet gekaart

De ASP-backend-melding van meting (c) draagt een opmaakfout: "needs the full
timeline; \"J1\" supplies only the**                **asOfDate-bucket scalar" —
een reeks spaties midden in de zin, uit een OCaml-stringvervolg zonder
`\`-continuatie in `backends/clingo/lib/translate.ml:1922`. Cosmetisch, en het
raakt een melding die deze kaart meet, maar niet wat de kaart vraagt; hier
gemeld in plaats van meegenomen.
