# static-precision-gate: frontend en verifier weigeren een schrijfactie die niet gegarandeerd past

*Vastgelegd door de hoofdsessie op 2026-11-02, op besluit van Yeb dezelfde dag. Onderzoek:
`/Users/yeb/Developer/uchar-dev/research/precisie-en-afronding-2026-11-01/` (`rapport.md`, beslispunten B1 en B2,
`notes/regelspraak.md` voor ALEF). Pas oppakken na `20261009-drools-schrijfgrens`.*

Goal: een auteur krijgt de fout bij het opstellen, niet bij één bedrag in productie. Een schrijfactie waarvan de typering niet
garandeert dat de waarde in de decimalen van het doel past, is een statische fout in de frontend en in de verifier, in de vorm die
ALEF gebruikt. De toets bij uitvoering (EF-006, ch. 2.4 punt 1 na `drools-schrijfgrens`) blijft als vangnet.

## Aanleiding

Na `drools-schrijfgrens` is een waarde die niet past een evaluatiefout. Die hangt van de waarde af: `x gedeeld door 3` naar een
veld met 2 decimalen speelt bij x = 999 en faalt bij x = 1000. Een regel kan dus op alle testgevallen spelen en in productie
falen. ALEF voorkomt dat statisch. De frontend doet het al voor `maal`, `plus` en literals (RGS-338), maar laat de deling door,
omdat ch. 2.3 haar op maxDec typeert (`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Typing.java:122-127`).
De verifier heeft geen precisietypering: de "precision gate" van WF-023 staat OPEN in IR-SPEC.

## De regel (ALEF-vorm, zoals gelezen in de MPS-modellen van ALEF)

Elke expressie krijgt een aantal decimalen uit de declaraties, als bovengrens; een schrijfactie waarvan dat aantal groter is
dan de decimalen van het doel, is een fout, behalve naar de onbegrensde vorm (`Numeriek (getal)`).

- `plus`, `min`, `verminderd met`: het hoogste aantal; `maal`: de som (ch. 2.3, ongewijzigd).
- `gedeeld door` met een noemer die geen literal is: oneindig; past alleen in de onbegrensde vorm. Met een literal-noemer: de
  decimalen van de teller plus die van 1/noemer (`/ 2` één erbij, `/ 4` twee, `/ 3` oneindig).
- Percentage: links + 2 + rechts, nog na te gaan in ALEF (het rapport heeft dit niet per golden nagerekend).
- Minimum en maximum: het hoogste aantal van de operanden (ch. 2.3 neemt nu het laagste, en claimt daarmee te weinig).
- Eenheidsomrekening: de factor telt mee (km naar m kan decimalen wegnemen, m naar km voegt er drie toe; ALEF doet dit, de
  frontend niet, `Typing.java:369`).
- Gemiddelde en tijdsevenredig deel: delingen, dus oneindig.
- Afronding op n: n.
- Verdeling: alleen het type van de pot (zoals ALEF en RGS-340). Een aandeel dat niet past, blijft EF-006 bij uitvoering. Dat houdt
  EF-006 bereikbaar voor een rood geval.

Een waarde die past, maar waarvan de typering het niet garandeert (`x gedeeld door y` met y altijd een deler van x), vraagt dan
een afronding die niets verandert. Wie geen tussentijdse afronding wil, schrijft het tussenresultaat in `Numeriek (getal)` en rondt
af in het eindattribuut.

## Normvraag vooraf: de Catala-route

Een Catala-`decimal` is exact en staat nu op een vaste schaal van 4 decimalen (ch. 2.9). Onder deze regel weigert de verifier elke
Catala-deling naar zo'n veld, want een Catala-bron heeft daar geen afronding. Geen van de 12 Catala-goldens raakt het nu. Opties:
(a) een Catala-`decimal` wordt de onbegrensde vorm, en clingo weigert dan een waarde die niet geheel is met naam; (b) de Catala-route
valt buiten de regel, en dat is een uitzondering per frontend op een norm van de IR. Beslissen vóór de bouw (naar Fable als Yeb er
niet is). **Besloten door Yeb op 2026-11-02: (a), een Catala-`decimal` wordt de onbegrensde vorm.**

## Wat er verandert

| Wat | Hoeveel | Wat ermee |
|---|---|---|
| goldens met een deling door een attribuut naar een begrensd doel | 25 goldens, 33 schrijfacties | `Rounding` invoegen of een onbegrensd tussenattribuut; de waarde past aan het fixpunt, dus de verwachtingen blijven gelijk |
| goldens met een percentage of een literal-noemer (ALEF-vorm) | 7 en 3, niet per golden nagerekend | idem |
| `anchor/` daaronder | `jutterswet-vindersloon`, `jutterswet-tariefstelling` | beschermd: voorstel aan de maintainer |
| goldens met een `source.rgs` daaronder | 10 | de bron mee, gate 10 en zo nodig gate 16 |
| de acht witnesses van ch. 5.7 (`jutterswet-fout-op-tussenstand`, `-initialisatie-naast-regel`, `-lezer-van-initialisatie`, `-deling-in-afwijking`, elk met `-omgekeerd`) | 8 | opnieuw op een andere foutsoort, want een niet-passende tussenstand van EF-006 kan dan niet meer ontstaan |
| rode EF-006-gevallen met een deling | 9 | worden negatieve gevallen van de nieuwe WF-arm, of krijgen een verdeling; `EXPECTED_MIN_RED_CASES=44` en ch. 5.7b moeten mee |

Lijsten per golden: de scan `meting/reconcile2.py` in de onderzoeksmap.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een WF-arm in de verifier (de precisiepoort van WF-023, of een eigen nummer) met een negatief geval per regel hierboven, en
  een golden die elk geval laat passen met een afronding of een onbegrensd doel.
- (b) De frontend geeft dezelfde uitkomst als de verifier op dezelfde bron (RGS-338 voor de deling, het percentage en de eenheidsfactor).
- (c) Ch. 2.3 en ch. 2.4 beschrijven de typering en de poort; de kandidaatregel van de v0-noot en CONSTRUCTS rij 2 gaan erin op.
- (d) De goldens en witnesses van de tabel aangepast, met ref ≡ clingo ≡ beide Drools-branches op dezelfde absolute verwachtingen.
- (e) De Catala-normvraag beslist en uitgevoerd.
- (f) `make gates` groen.

## Omvang

Middel tot groot: de typering in de verifier is nieuw, de rest is grotendeels mechanisch.

May touch: `spec/verifier/`, `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Typing.java`, `corpus/golden/`,
`corpus/negative/`, `corpus/red/`, `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`, `spec/CONSTRUCTS.md` (main-sessie), `scripts/check-red.sh`,
`frontends/catala/` (na de normvraag); beschermde paden als voorstel.

## Result

Bouwer, 2026-11-02, op main `5afac5597` (gerebased: de branch begon op `e284bab9a`, ging over `3b75ac8e0`
(`written-value-at-fixpoint`) naar `5afac5597` (`drools-exact-arithmetic`); één conflict, ch. 2.8 en de rij EF-006 van
ch. 5.7b, samengevoegd). Niet gemerged, niet gepusht.

### Commits

- `ff454c80c` kaart naar doing
- `0cd15d00a` verifier: de precisiepoort van WF-023 in de ALEF-vorm (veertien negatieven)
- `280a7c01e` frontend: RGS-338 en RGS-370 over dezelfde grens als de verifier
- `da039b210` precisiepoort: een som over eenheden in de eenheid die haar begrenst; tijdlijndoel buiten de poort
- `173e7bfd8` corpus: elke schrijfactie past gegarandeerd (goldens, getuigen, rood naar negatief, testpaden ref en Drools)
- `ea9618ede` spec: de precisiegrens en de poort (voorstel)
- `6eee50e0b` spec: ch. 2.8 na de rebase, de tijdlijn buiten de statische poort
- `65375df0a` gate 12: `EXPECTED_PLAYED` 316 met `jutterswet-precisiepoort` (voorstel)
- `bcfe4f257` frontend: de poort spreekt RGS-372, RGS-338 blijft bij de case-waarde
- `57d2a5003` suites en gate 8: hun documenten zijn verifier-schoon onder de poort
- `174d63b6d` ASP-TRANSLATION: de verhuisde rode gevallen bij hun nieuwe plaats (voorstel)
- en de commit van dit Result

Buiten de "May touch" van de kaart, gemeld omdat de corpusmigratie ze raakte: `backends/drools/src/test` (de paden van
`WriteBoundaryWitnessTest` en `DimensionCellWitnessTest` naar de verhuisde documenten; de oude documenten van
`jutterswet-fout-op-tussenstand` met hun cases als testresource `src/test/resources/write-boundary/fout-op-tussenstand`,
zodat die getuige de schrijfgrens op een tussenstand blijft toetsen), `ref/test/test_ref.ml` (het pad van
`WV-5.8c-schrijfgrens-ef006`; de rijen `WV-5.7-fixpoint` zoals ch. 5.7 ze nu beschrijft), `backends/clingo/test/test_clingo.ml`
en `frontends/catala/test` (hun documenten moeten verifier-schoon zijn), `frontends/regelspraak` buiten `Typing.java`
(`RuleChecker`, `TimeCalculation.gridUnit`, `ExpressionBuilder.setRead` zichtbaar in het pakket, register, catalogi,
`DIVERGENCES.md`, tests), `corpus/negative/README.md`, `scripts/check-drools.sh`, `spec/ASP-TRANSLATION.md`.
`ref/lib/` en `backends/drools/src/main` zijn niet aangeraakt.

### De typeregel per bewerking, zoals gebouwd

Verifier `spec/verifier/lib/checks.ml` (`precision`, `wf023_precision`, met zarith), frontend `Typing.precision` en
`Typing.precisionGate`; beide leiden de grens af uit declaraties en vorm, nooit uit de type-annotatie. Een grens draagt de
eenheid waarin zij geldt. ALEF: `/Users/yeb/Developer/uwv/ALEF` op `872b859`, gelezen met een eigen weergave van de XML
(alleen `resolve=` en `value=`); TR = `languages/regelspraak/languageModels/regelspraak.translator.mps`, GB =
`languages/objecten/languageModels/gegevensspraak/behavior.mps`, GT = `languages/objecten/languageModels/gegevensspraak.translator.mps`.

| Bewerking | Grens | ALEF-bron |
|---|---|---|
| attribuut, parameter, cel | declaratie: geheel 0, n decimalen n, `getal` ∞ | GT r. 6098-6247 (`mapping_nodeType` → `decimalen`); `gegevensspraak/structure.mps` r. 1182-1186: "-1 ~ getal, 0 ~ geheel getal, n > 0 ~ getal met n decimalen" |
| literal | decimalen van de exacte waarde, ∞ als zij niet eindigt | GT r. 5871-5917 (`numberOfDecimals`, -1); `BigRational.numberOfDecimals` r. 506-509 |
| factorregel (literal-operand, eenheid) | `max(0, d + dec(f) − tienen(teller f))`, ∞ als f niet eindigt | GB r. 44807-44990 (`OmrekenFactor.decimalen`) |
| plus, min, verminderd met | hoogste, beide omgerekend naar de operand-eenheid die de kleinste grens geeft | TR r. 27423-27454 en `combine` r. 28547-28688 (beide naar `resultaatEenheid`, GB r. 41253) |
| maal | som; met een literal de factorregel; met omrekening (ch. 2.6 regel 2) de factor erbij | TR r. 27551-27704 |
| gedeeld door | ∞; door een literal ≠ 0 de factorregel met het omgekeerde | TR r. 27709-27855 |
| gedeeld door (abs) | 5 | (uchar ch. 2.2; ALEF kent de vorm niet) |
| percentage van | links + 2 + rechts; literal links L: factorregel met L/100; literal rechts R: factorregel over links + 2 | TR r. 27891-28051 (`plus(plus(links,2),rechts)`); zie besluit 3 |
| macht, wortel, tijdsevenredig deel | ∞ | TR r. 28107-28140 (-1) |
| minimum, maximum, eerste, laatste, som, begrenzing, tak | hoogste, als plus | TR r. 27163-27335 (begrenzing), r. 27340-27371 (concatenatie, `combine`) |
| totaal over een tijdlijn | operand, met de rasterfactor waar de periode van het tarief niet de rastereenheid is | (uchar ch. 2.6 regel 8) |
| aantal, tijdsduur, datumdeel | 0 | TR r. 27065-27158, 27403-27417 |
| absolute waarde | operand | TR r. 28517-28539 |
| afgerond op n | n | TR r. 28083-28102 |
| binder (Let, arm, functieparameter), vouw | de waarde die zij houdt; een vouw verbreed tot zij niets meer toevoegt; payload, structveld: declaratie | TR r. 28428-28450 (`Variabele` → `waarde`) |

De poort (ALEF `checkDecimalen`, GB r. 25211-25300, met `vergelijkDecimalen` r. 25077-25203: -1 als `Integer.MAX_VALUE`):
grens omgerekend naar de eenheid van het doel, groter dan de decimalen van het doel is een fout; de onbegrensde vorm heeft
geen plafond. Vier armen: `WF-023.precision-exceeds` (`valueDecimals`, `targetDecimals`), `WF-023.precision-unbounded`
(`targetDecimals`), `WF-023.share-precision-exceeds` (`shareDecimals`, `targetDecimals`) en
`WF-023.share-precision-unbounded`; frontend RGS-372, RGS-370, RGS-340, RGS-371 in dezelfde zinnen (RGS-338 blijft de
Nederlandse zin van een case-waarde in een scenario).

### De negatieven

Nieuw, elk eerst `verifier: PASS` op `e284bab9a` (de oude verifier in een losse worktree), daarna geweigerd op de genoemde arm:
`wf023-precision-quotient-by-attribute` (∞ > 0), `-quotient-by-three` (∞ > 2), `-quotient-by-four` (2 > 1),
`-percentage-attribute` (5 > 2), `-percentage-literal` (2 > 1), `-maximum` (2 > 0), `-unit-factor` (m → km, 3 > 0),
`-time-proportional-part` (∞ > 2, een scalair doel), `-product` (4 > 2), `-sum` (2 > 1), `-unbounded-read` (∞ > 2),
`-power` (∞ > 2, `jutterswet-kistinhoud` zonder afronding), `-creation-member` (∞ > 2), `-allocation-share` (pot 2 > 0).
Elf rode gevallen die de poort statisch weigert, verhuisd (`git mv`) naar `corpus/negative/wf023-…`: `closed-nonexact-division`,
`closed-runtime-nonexact-division`, `decimal-nonterminating` (hun case weg, geen suite leest hem), en
`initialisatie-past-niet`, `afwijking-past-niet` (elk met `-omgekeerd`), `cel-schrijfgrens`, `halvering-past-niet`,
`kopie-past-niet`, `vast-bergloon-past-niet` (hun `case1.json` blijft: de suites van Drools en ref toetsen er de
schrijfgrens achter de poort op; `corpus/negative/README.md` noemt dat). Op acht andere negatieven (`wf070-lambda-body-wholeness`, `wf076-let-wholeness` en zes `wf091-*`) komt een melding
van de poort bij; gate 3 eist alleen de eigen regel.

### Corpuswijzigingen per golden

Gemeten met de nieuwe verifier op `3b75ac8e0` vóór de migratie: 43 schrijfacties in 28 goldens (10 met `source.rgs`),
15 in 13 rode gevallen; geen in de toka-zone, de Catala-goldens of de zes goldens van `written-value-at-fixpoint`; na de
tweede rebase geen nieuwe. De waarde past aan het fixpunt overal, dus geen verwachting veranderde (ref ≡ clingo ≡
beide Drools-takken, gemeten per golden en in `make gates`).

| golden | schrijfactie (vorm, grens, doel) | oplossing |
|---|---|---|
| `anchor/jutterswet-taxatie` | oppervlakte van een kavel (power, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `anchor/jutterswet-taxatie` | inhoud van een kavel (power, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `anchor/jutterswet-taxatie` | krimpmaat van een kavel (power, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `anchor/jutterswet-vindersloon` | deel van een vondst (dividedBy, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `anchor/jutterswet-vindersloon` | bonus van een vondst (dividedBy, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `anchor/jutterswet-vindersloon` | middenprijs van een vondst (dividedBy, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `anchor/jutterswet-vindersloon` | krimp van een vondst (times, grens 2, doel 1) | `Rounding` halfAwayFromZero op 1 |
| `closed-runtime-division-by-zero` | delertotaal van een jutter (Fold, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `division-abs-by-numeric-zero` | oppervlakte van een kavel (power, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `division-abs-by-numeric-zero` | inhoud van een kavel (power, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `division-abs-by-numeric-zero` | krimpmaat van een kavel (power, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `division-by-numeric-zero` | deel van een vondst (dividedBy, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `division-by-numeric-zero` | bonus van een vondst (dividedBy, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `division-by-numeric-zero` | middenprijs van een vondst (dividedBy, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `division-by-numeric-zero` | krimp van een vondst (times, grens 2, doel 1) | `Rounding` halfAwayFromZero op 1 |
| `filled-count-unguarded-division` | gemiddelde vondstwaarde (dividedBy, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `jutterswet-bergloon` | helpersdeel van een berging (dividedBy, grens onbegrensd, doel 0) | bron: rekenkundig afgerond op 0 decimalen |
| `jutterswet-deling-in-afwijking-omgekeerd` | toeslag (plus, grens onbegrensd, doel 2) | bron: rekenkundig afgerond op 2 decimalen |
| `jutterswet-deling-in-afwijking-omgekeerd` | verhouding (dividedBy, grens onbegrensd, doel 2) | bron: rekenkundig afgerond op 2 decimalen |
| `jutterswet-deling-in-afwijking` | verhouding (dividedBy, grens onbegrensd, doel 2) | bron: rekenkundig afgerond op 2 decimalen |
| `jutterswet-deling-in-afwijking` | toeslag (plus, grens onbegrensd, doel 2) | bron: rekenkundig afgerond op 2 decimalen |
| `jutterswet-fout-op-tussenstand-omgekeerd` | aandeel (dividedBy, grens onbegrensd, doel 0) | opnieuw op EF-032 (datumconstructie) |
| `jutterswet-fout-op-tussenstand` | aandeel (dividedBy, grens onbegrensd, doel 0) | opnieuw op EF-032 (datumconstructie) |
| `jutterswet-gevuldegemiddelde` | gemiddelde vondstwaarde (dividedBy, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `jutterswet-initialisatie-naast-regel-omgekeerd` | standaardbergloon (dividedBy, grens onbegrensd, doel 0) | bron: rekenkundig afgerond op 0 decimalen |
| `jutterswet-initialisatie-naast-regel` | standaardbergloon (dividedBy, grens onbegrensd, doel 0) | bron: rekenkundig afgerond op 0 decimalen |
| `jutterswet-kinderdeler` | delertotaal van een jutter (Fold, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `jutterswet-kindermacht` | machttotaal van een jutter (Fold, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `jutterswet-lezer-van-initialisatie-omgekeerd` | aandeel (dividedBy, grens onbegrensd, doel 0) | bron: rekenkundig afgerond op 0 decimalen |
| `jutterswet-lezer-van-initialisatie` | aandeel (dividedBy, grens onbegrensd, doel 0) | bron: rekenkundig afgerond op 0 decimalen |
| `jutterswet-meldingsketen` | meldingsgewicht van een vondst (dividedBy, grens onbegrensd, doel 2) | bron: rekenkundig afgerond op 2 decimalen |
| `synth-schaalmenger` | zoomlengte van een zeil (plus, grens 2, doel 1) | `Rounding` halfAwayFromZero op 1 |
| `synth-schaalmenger` | stukprijs van een zeil (dividedBy, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `synth2-leegcomposities` | uitkomst van een meting (plus, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `synth3-armlichaam` | lichaamtotaal van een jutter (Fold, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `synth3-armvangst` | heffing van een jutter (Let, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `synth3-armvangst` | wikkelheffing van een jutter (Let, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `synth3-armverdeling` | heffing van een jutter (Let, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `synth3-armverdeling` | wikkelheffing van een jutter (Let, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `synth3-armwissel` | heffing van een jutter (Let, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `synth3-operandvolgorde` | vangstsom van een vangst (Let, grens onbegrensd, doel 0) | `Rounding` halfAwayFromZero op 0 |
| `synth3-operatortak` | heffing van een jutter (Let, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `synth3-takhek` | heffing van een jutter (Let, grens onbegrensd, doel 2) | `Rounding` halfAwayFromZero op 2 |
| `jutterswet-precisiepoort` (nieuw) | de vormen die de poort doorlaat: quotiënt in `getal`, afronding erna, ÷ 3 afgerond, ÷ 4, percentage afgerond, maal 100 | twee cases met de hand uitgerekend |

`anchor/jutterswet-taxatie` en `anchor/jutterswet-vindersloon` zijn beschermd: voorstel, met als reden dat de poort hun
onafgeronde macht en deling weigert en dat de afronding aan het fixpunt niets verandert (vijf cases elk, gelijk). Niet
geraakt, en dus witnesses van wat de poort doorlaat: `anchor/jutterswet-tariefstelling` (1 + k/2 in 4 decimalen),
`jutterswet-vergunningsduur` (2 jr plus mnd in mnd), `jutterswet-sleepbaan` (2,5 km plus m in hele m), de percentages
`20% van` een geheel getal in 2 decimalen, `anchor/jutterswet-borgdeel` en de andere pro-rata-tijdlijnen (besluit 4).

### De getuigen van ch. 5.7

- E, `jutterswet-fout-op-tussenstand` (±), opnieuw op EF-032: de initialisatie zet de bergtermijn op 30, de lezer
  `vervaldag` bouwt de datum met jaar 2025, maand 2 en dag 30; aan het fixpunt schrijft de regel 28. Cases: V1 gemeld,
  opgegeven termijn 28 → 28, 2025-02-28; V2 niet gemeld, maand 3 → 30, 2025-03-30. Rood `jutterswet-fout-aan-fixpunt` (±):
  opgegeven termijn 31, maand 4 → EF-032 `{year 2025, month 4, day 31}` in ref en clingo, aan het fixpunt.
- A `initialisatie-naast-regel` (±), B `lezer-van-initialisatie` (±), `deling-in-afwijking` (±): de deling krijgt een
  afronding, de deponeringen EF-005/EF-031 op tussenstanden blijven het onderwerp; case 2 van A (100/3) en case 6 van de
  afwijking (100/30 + 5) pinnen nu alleen dat de lagere vorm niet beslist (waarden gelijk). Hun rode tweelingen zijn negatieven.
- Tanden, mutatie R1 (ref `over_subjects` vangt `Value.Fault` niet, `when false`): `jutterswet-fout-op-tussenstand-omgekeerd`
  case1 geeft `EVALUATION FAULT: EF-032: dateWith: year 2025, month 2 and day 30 name no calendar day`, en
  `WV-5.7-fixpoint-fault-between` faalt in vijf volgordes; de voorwaartse golden speelt, zoals bij de oude E.

### Rood en mutaties

Rood: de veertien negatieven gaven elk `verifier: PASS` met de verifier van `e284bab9a` (gebouwd in een losse worktree); `PrecisionGateTest` op de frontend van
`e284bab9a`: 10 van 11 rood (`logs/stap2-frontend-red.log`; groen was alleen product en som, die RGS-338 al weigerde); de
tests die later bijkwamen (som over eenheden, tijdlijn buiten de poort) zijn rood onder F6, F7, F12. Mutaties in een
losse worktree van `57d2a5003`, niet gecommit (`werk/static-precision-gate/scratch/mutate.py`,
`logs/stap5-mutations-*.log`), elk één regel:

| | mutatie | uitkomst |
|---|---|---|
| V1 | deling door een niet-literal → 0 | `wf023-precision-quotient-by-attribute`, `-creation-member`: PASS |
| V2 | literal-deler zonder omgekeerde | `-quotient-by-three`, `-quotient-by-four`: PASS |
| V3 | literal-percentage zonder honderd | `-percentage-literal`: PASS |
| V4 | percentage van twee lezingen zonder + 2 | `-percentage-attribute`: PASS |
| V5 | minimum, maximum: laatste operand | `-maximum`: PASS |
| V6 | omrekening zonder factor | `-unit-factor`: PASS |
| V7 | som alleen in de linker eenheid | golden `jutterswet-vergunningsduur`: `WF-023.precision-unbounded` |
| V8 | maal: hoogste in plaats van som | `-product`: PASS |
| V9 | onbegrensde vorm begrensd | `-unbounded-read`: PASS |
| V10 | wortel en pro-rata begrensd | `-time-proportional-part`: PASS |
| V11 | aandeel zonder grens van de pot | `-allocation-share`: PASS |
| V12 | tijdlijndoel binnen de poort | goldens `anchor/jutterswet-borgdeel`, `synth2-prorata-breed`: `precision-unbounded` |
| V13 | afronding onbegrensd | golden `jutterswet-precisiepoort`: `precision-unbounded` |
| F1 | frontend: deling door niet-literal → 0 | 2 tests rood |
| F2 | literal-deler zonder omgekeerde | 1 rood |
| F3 | literal-percentage zonder honderd | 1 rood |
| F5 | minimum, maximum: eerste operand | 1 rood (de volgorde opbrengst, taxatie) |
| F6 | omrekening zonder factor | 2 rood |
| F7 | som alleen in de linker eenheid | 1 rood |
| F9 | onbegrensde vorm begrensd | 2 rood (lezing, pot) |
| F10 | pro-rata begrensd | 1 rood |
| F11 | aandeel zonder grens van de pot | 1 rood |
| F12 | tijdlijndoel binnen de poort | 1 rood |
| R1 | ref meldt een fout meteen | zie de getuigen van ch. 5.7 |

### Aangepaste verwachtingen, pins en vloeren

- `scripts/check-drools.sh`: `EXPECTED_PLAYED` 315 → 316 (de nieuwe golden; reden in het script).
- `scripts/check-red.sh`: de twee `EXCLUDE DROOLS-008`-regels van `closed-nonexact-division` en
  `closed-runtime-nonexact-division` weg (hun documenten zijn negatieven); `EXPECTED_MIN_DROOLS_EXCLUDE_CODES` 18 → 16 in
  hetzelfde commit, met reden. `EXPECTED_MIN_RED_CASES=44` blijft: 61 rode cases na de verhuizing (71 vóór, plus het rode
  geval van `drools-exact-arithmetic`). Geen andere vloer veranderd; gate 3 groeit (596 negatieven).
- Spec ch. 5.7b: EF-006 houdt als getuigen `jutterswet-verdeling-past-niet` en `jutterswet-tijdsevenredig-past-niet`; EF-032
  krijgt `jutterswet-fout-aan-fixpunt` (±).
- `frontends/catala/test/division.catala_en`: `vangst / jutters` → `vangst / 4` (de input `jutters` weg); cases
  `division_seven` 3.5000 → 1.7500 (7/4), `division_nine` 2.2500 blijft (9/4). Reden: een Catala-bron heeft geen afronding
  die de plugin draagt, en een quotiënt door een attribuut past zonder afronding niet in S = 4.
- `backends/clingo/test/test_clingo.ml`: `arm_division` rondt af op 2 decimalen; de geneste gesloten-body-documenten
  (`cb_three`) ronden de vouw af (`cb_doc ~rounded:true`); clingo draagt geen afronding binnen een gesloten body.
- `UnitIdentityTest`: vier bronnen ronden het quotiënt, de som van wederkerige eenheden en het totaal over een tijdlijn af.
- `ref/test`: `WV-5.7-fixpoint-a-nonexact` heet `-a-three`; A, B en a-equal ronden af; `fault-between` en `fault-stands`
  rusten op de datumconstructie (EF-032).
- `WriteBoundaryWitnessTest`, `DimensionCellWitnessTest`: alleen paden; de asserties zijn gelijk.
- Frontend: RGS-340 spreekt de zin van de verifier (Nederlands vóór); RGS-370, RGS-371, RGS-372 nieuw; register 362 codes.

### Voorgestelde tekst voor `spec/CONSTRUCTS.md` rij 2

Vervang "Red cases `jutterswet-verdeling-past-niet`, `-halvering-`, `-kopie-`, `-vast-bergloon-past-niet`. The candidate
WF rule (a possibly non-terminating value must pass an explicit rounding before a bounded write) is OPEN, card
`20261102-static-precision-gate`." door: "Red case `jutterswet-verdeling-past-niet`. **Precision gate (2026-11-02, card
`static-precision-gate`):** RUNTIME-SEMANTICS ch. 2.4 point 4 and WF-023 refuse statically a write whose precision bound
(ch. 2.3, the form ALEF types it: a quotient by a non-literal has no finite bound, a literal factor and a unit conversion
count, a percentage adds two decimals, a minimum or maximum takes the highest, a sum is bound in the operand unit that
bounds it best) exceeds the target's decimals, at a rule target, an object-creation member, a defaults consequence and an
allocation (bounded by its rounding or its pot); a timeline target stands outside it. The RegelSpraak frontend refuses
the same sources (RGS-370, RGS-372, RGS-340, RGS-371). The evaluation fault of point 1 stays behind it. Negatives
`corpus/negative/wf023-precision-*`; golden `jutterswet-precisiepoort`. Tracked delta against v0's maxDec typing of a
quotient; the candidate rule of the v0 note went into the gate." In rij 10 (of waar `decimal-nonterminating` als rood
geval staat): het pad wordt `corpus/negative/wf023-decimal-nonterminating`. `spec/PLAN.md`: geen scopewijziging, behalve
dat het Catala-punt (a) open staat.

### Voorlopige besluiten

1. De poort is een arm van WF-023 (vier sleutels), geen eigen nummer: IR-SPEC noemde haar al als deel van WF-023.
2. Een som, minimum, maximum, begrenzing en tak worden begrensd in de eenheid van een operand die de kleinste grens
   geeft, met de eenheid aan de grens; ALEF rekent naar zijn `resultaatEenheid`. De eerste bouw rekende in de linker
   eenheid van ch. 2.6 en weigerde daardoor `jutterswet-vergunningsduur` (2 jr plus b mnd is b/12 jr), dat ALEF doorlaat.
3. Percentage met een literal links: de factor L/100. ALEF neemt `numLiteral` van het percentage, en dat is de geschreven
   waarde (GB r. 22833, `alsBigRational` = `fromString(waarde)`, geen eigen versie voor `PercentageLiteral`, r. 22749): 20%
   telt daar als 20, zodat `20% van` een geheel getal 0 decimalen krijgt waar 20/100 = 1/5 er één heeft. Dat is een
   ondergrens die niet garandeert, dus niet gevolgd.
4. Een doel met een tijdlijn valt buiten de poort. Gemeten op `5afac5597`: een afronding rond de pro-rata-schrijfactie
   geeft in ref "temporal writes are not implemented", in clingo CLINGO-024 en in Drools DROOLS-105; met de poort zou een
   onafgeronde pro-rata in een begrensde tijdlijn geen uitvoerbare vorm hebben. De schrijfgrens per segment blijft erachter.
5. De verdeling telt alleen pot of afronding (ALEF); RGS-340 telde ook de decimalen van het gewicht, dat valt weg.
6. Een macht is ∞, ook met een gehele exponent (ALEF); een wortel ook.
7. Een veld van een struct en de payload van een constructor krijgen de grens van hun declaratie; WF-073 en WF-062 toetsen
   het type van de waarde, niet haar grens. Een binder van een functie houdt de grens van zijn argument.
8. Een waarde zonder claim (Unresolved, ongebonden variabele, geen getal) en een halve omrekenpaar (WF-092) schuiven door.
9. De ingevoegde afrondingen zijn `halfAwayFromZero` (rekenkundig) op de decimalen van het doel; de modus doet er aan het
   fixpunt niet toe.
10. Rode gevallen die de poort weigert, worden negatieven met hun case erbij waar een suite hem leest, in plaats van een
    verdeling: zo blijven de suites de schrijfgrens per vorm toetsen.
11. De type-annotatie van een samengestelde knoop houdt de v0-claim (ook "least" voor een opgesomd minimum of maximum, die
    WF-091 kruist); alleen de grens beslist. Geen golden heeft een opgesomd minimum of maximum, en clingo vertaalt het niet.
12. De frontend spreekt bij RGS-370, RGS-371, RGS-372 en RGS-340 de zin van de verifier (de regel van
    `ReaderSentenceBoundaryTest` en `DIVERGENCES.md` voor geweigerde documenten die de verifier ook weigert); RGS-338 blijft
    de Nederlandse zin voor een case-waarde.

### Wat niet lukte

- Catala (a), een Catala-`decimal` als de onbegrensde vorm (besluit Yeb). Gemeten op `e284bab9a` met `num_rat` op
  `decimals = 0`: `jutterswet-tariefrecht-catala` speelt in ref (4 van 4) en in Drools (`PLAYED 4`), en faalt in clingo op
  elk van de vier cases: `viol("result of penningplichtige.tarief does not fit its declared decimals","J1")` en
  `viol("arithmetic range exceeded …")` (het tarief 0,20 is niet geheel, en clingo houdt de onbegrensde vorm als geheel
  getal); de plugin weigert `jutterswet-kamerhuishouden-catala` te schrijven ("per-element callee "Vangstheffing" wraps a
  rounding that is not the money × 4-decimal-rate cent product"). Niet uitgevoerd; S = 4 blijft, ch. 2.9 noemt de meting
  als OPEN. Geen van de 12 Catala-goldens raakt de poort.
- Een golden met een opgesomd maximum en een met een omrekening bij de schrijfactie: clingo vertaalt beide niet
  (CLINGO-008; "non-identical units under the write"), dus de nieuwe golden laat ze weg.

### make gates

Eén run op `57d2a5003` (op `5afac5597`), alle regels groen (`out/gates/20261102-231851`); daarna veranderden alleen
`spec/ASP-TRANSLATION.md` (twee verwijzingen) en deze kaart:

```
start 2026-11-02 23:18:51 57d2a5003 card/static-precision-gate
ocaml rc=0 0s ocaml-build: rebuilt (5 source path(s) newer than the stamp, e.g. backends/clingo/test/test_clingo.ml; 0s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 2s drools-jar: rebuilt (4 source path(s) newer than the jar, e.g. frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/Typing.java; 2s)
01-self-contained rc=0 2s
02-ir-schema rc=0 124s
04-case-schema rc=0 7s
03-verifier rc=0 37s
05-oracle rc=0 22s
06-clingo rc=0 258s
07-noop rc=0 13s
08-red rc=0 139s
09-catala rc=0 4s
10-regelspraak rc=0 222s
11-toka-parity rc=0 211s
12-drools rc=0 312s
13-reftype rc=0 270s
14-flogic rc=0 1s
15-graph rc=0 46s
16-scenario rc=0 38s
17-readings rc=0 234s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 23:24:05 314s red=0
```
