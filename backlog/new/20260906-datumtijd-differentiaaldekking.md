# De differentiaal heeft nul DatumTijd-dekking

Goal: `DatumTijd` is een gedeclareerd datatype van de wire, maar geen
enkele executor-differentiaal raakt het. Gemeten 2026-09-06: het
golden-corpus bevat **nul** `DatumTijd`-vergelijkingen en **nul**
`DatumTijd`-literals — wel 19 `dateTime`-waarden op de case-wire, die
dus alleen worden ingelezen en nooit ergens mee vergeleken. De ASP-vertaling
faalt op een `DatumTijd`-ordening die **geen literal draagt**: een document
dat twee `DatumTijd`-attributen met `EERDER` vergelijkt geeft
`TRANSLATION FAULT: unscalable datatype in the ASP translation`, terwijl de
referentie-interpreter datzelfde document exact naspeelt
(`oracle: PASS`). Draagt het document wél een literal, dan valt ook de
interpreter uit: `EVALUATION FAULT: literal of this datatype not
implemented` — in beide lexemen. Alleen de Drools-backend voert zo'n regel
uit. Eén executor is geen differentiaal: elke `DatumTijd`-semantiek die
vandaag zou verschuiven, verschuift ongezien.

Daaronder ligt een tweede gat. Er zijn **drie** lezers van een
datum-tijd-lexeem — de gedeelde case-lezer van interpreter + ASP-backend,
de eigen case-lezer van de Drools-backend, en de literal-lezer van de
IR-wire — en buiten het schema-patroon van `uchar-case-v0` zijn ze het
oneens (gemeten 2026-09-06):

| vorm | literal-lezer | case-lezer (Drools) | case-lezer (interpreter + ASP) |
|---|---|---|---|
| `…T12:00:00.1` | weigert | 100 ms | 1 ms |
| `…T12:00:00.100000` | weigert | 100 ms | 100 000 ms |
| `…T12:00:00.1234` | weigert | 123,4 ms | 1 234 ms |
| `2024-1-31T09:00:00` | weigert | weigert | 09:00 op 31 januari |
| `…T12:00` (geen seconden) | weigert | 12:00:00 | `malformed dateTime` |

Vandaag is dat onbereikbaar: gate 4 dwingt op elk casebestand het
schema-patroon af (vaste breedtes, `T`, secondenveld, fractie exact drie
cijfers), en binnen dat hek vallen de drie lezingen samen. Het hek is dus
de enige reden dat de divergentie nooit zichtbaar wordt — geen van de
lezers weigert zelf wat hij niet eenduidig kan lezen. Een casebestand dat
langs gate 4 komt (een handmatige run, een generator, een tweede
case-producent) krijgt drie antwoorden. Dat is precies de klasse fout die
uchar met de differentiaal wil vangen, en die de differentiaal hier niet
kan vangen.

De kaart sluit beide gaten in één slag, en levert daarmee de golden op die
`backlog/done/20260905-drools-datumtijd-iso-lexeem.md` bewust niet kon
bouwen (de lexeempassage in IR-SPEC ch. 4.1 is beschrijvend gebleven omdat
elke MUST hier een doorlopende golden nodig heeft, en die kan pas bestaan
als ref en de ASP-vertaling `DatumTijd` dragen).

Acceptance:
- **Ref draagt de `DatumTijd`-literal.** De literal-arm leest het
  canonieke ISO-lexeem naar dezelfde ms-laag waarin de case-wire zijn
  `dateTime` al leest; een niet-canoniek lexeem faalt luid met de
  positie, nooit stil. Meetbaar: het document dat vandaag
  `EVALUATION FAULT: literal of this datatype not implemented` geeft,
  speelt daarna exact.
- **De ASP-vertaling draagt de `DatumTijd`-ordening** (en daarna de
  literal). Vandaag faalt `decimals_of_datatype` erop; de ms-laag is een
  gehele schaal, dus de ordening is vertaalbaar — de vraag die de kaart
  moet beantwoorden is welke schaal de vertaling voert en wat dat met de
  bestaande integer-schaal doet. Meetbaar: hetzelfde literaalvrije
  ordeningsdocument dat nu `TRANSLATION FAULT` geeft, geeft daarna
  dezelfde stabiele modeluitkomst als ref.
- **De drie lezers klemmen zelf.** Elke lezer weigert wat buiten het
  schema-patroon valt (fractie ≠ drie cijfers, veldbreedte, ontbrekend
  secondenveld, zone/offset) in plaats van te gokken — fail-fast, met de
  vorm in de foutmelding. Rood-eerst per lezer: een vorm uit de tabel
  hierboven die vandaag drie antwoorden geeft, geeft daarna drie
  weigeringen. De literal-lezer klemt al (2026-09-06); de twee
  case-lezers zijn het werk. Gate 4 blijft staan — het schema is dan een
  tweede hek, niet het enige.
- **De golden.** Eén nieuwe golden met een `DatumTijd`-attribuut, een
  ordening tegen een `DatumTijd`-literal in het canonieke lexeem, en een
  subdag-drempel (een middernachtdrempel verbergt precies de fout die de
  vorige kaart aantoonde). Case-waarden vóór, óp en ná de drempel, plus
  een millisecondenpaar. Hij moet door gates 5, 6 en 12 heen: ref ≡
  clingo ≡ Drools op absolute, met de hand berekende waarden.
- **Daarna pas de wire-vraag.** Met een doorlopende golden kan de
  beschrijvende ch.-4.1-passage een genummerde MUST worden (verifier-check
  + negatief-cases per veld). Die stap is een eigen voorstel op een
  beschermd pad; deze kaart levert er de voorwaarde voor en mag hem
  voorstellen, niet mergen.
- Alle dertien gates groen; geen bestaande pin beweegt. Let op gate 13:
  een nieuwe golden met unit-dragende annotaties komt in de twist-tabel.

May touch: `ref/` (de literal-arm en de case-lezer),
`backends/clingo/` (de vertaling van de ordening en de literal),
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/`
(de eigen case-lezer), `corpus/golden/**` (de nieuwe golden),
`spec/IR-SPEC.md` en `spec/schema/` (voorstel, alleen als de MUST-stap
binnen deze kaart wordt gehaald), `backlog/`.

Out of scope: de literal-lezer van de IR-wire zelf (geland 2026-09-06,
verankerd en met getuige); de emitter die voor een `DatumTijd`-literal nog
het dag-eerste snippet in `waarde` schrijft (eigen kaart — dat vergt een
herpin van de emitter-goldens); de dagprecisie-kant van WF-084 (dicht);
`Typering.vergelijk` op de precisienaad (restpunt van de
WF-036-kaart).
