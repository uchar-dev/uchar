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

**Corpusdekking:** 0 van de 568 documenten over de vier zones én de
pariteitszone draagt een `attribuutGranulariteit` met waarde `null` (gemeten
2026-09-10). De sluiting kost dus, net als bij de zuster, geen enkele regressie
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
