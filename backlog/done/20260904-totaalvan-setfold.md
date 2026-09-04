# Set-fold "totaal van" over een tempo-eenheid: geldig, gepind, of uitgesloten?

Goal: lens 1 op de filter-LHS-kaart (2026-09-04) vond dat de enige vorm
waarin het aggregaattype en de foldbron-declaratie uiteenlopen — een
set-fold `het totaal van <€/mnd>` met §4.20-noemerstrip — nergens gepind
is, én dat de reference-interpreter die vorm niet kan evalueren
(`EVALUATION FAULT: incompatible units: EUR vs EUR`, ook zonder filter):
de frontend typeert een vorm die de oracle niet draagt, en geen gate
raakt het gat omdat het corpus de vorm niet kent. Voorvraag aan de
maintainer (genummerd, vóór het bouwen): is een set-fold `totaal van`
over een tempo-eenheid überhaupt een geldige vorm, of is `som van` de
set-fold en `totaal van` uitsluitend de temporele integratie? De
grammatica laat hem toe en de frontend typeert hem volwaardig
(noemerstrip onvoorwaardelijk).

## Het antwoord (maintainerbesluit 2026-09-04)

De set-lezing van `het totaal van` is een **alias van `de som van`,
ZONDER noemerstrip**. De noemerstrip blijft **exclusief voor de
tijdsaggregatie** (de waardepertijdseenheid-aggregatie van §4.20 Tabel 30).
Wat gecorrigeerd wordt is de **typering op de set-route**. De twee sporen
uit de oude kaart (ref leert de vorm dragen / de frontend weigert de vorm)
vervallen daarmee allebei.

**Wat "de toolchain" hier wel en niet zegt.** De toolchain *accepteert* de
set-vorm — grammatica en operandcontrole laten hem door — maar zij
**typeert hem gestript**: `vendor/uchar-frontend/**/ExpressieBouwer.java`
op de huidige pin (`SPEC_REV 904ecc3`) draagt de arm `case TOTAAL ->`
onvoorwaardelijk. Deze fix **divergeert daar bewust van**, per
maintainerbesluit; de pin-bump brengt de nieuwe typering mee. De
acceptatie volgen we, de typering corrigeren we.

## De flank vóór de fix (rood-eerst, gemeten)

Minimale bron: één `€/mnd`-attribuut op Vondst, één set-fold op Jutter
(`het totaal van het tarief van alle vondsten van de Jutter`). Beide
doeldeclaraties gemeten, met de frontend van `main`:

```
doel €/mnd:  ERROR (line 17): berekening: toekenning:
             eenheden 'EUR/mnd' en 'EUR' zijn niet in elkaar om te rekenen
doel €:      ir: … geschreven   →   argument.type.eenheid = EUR/mnd
                                    functie.type.eenheid  = EUR   (noemer gestript)
             ref:  EVALUATION FAULT: incompatible units: EUR vs EUR
```

De eerlijke declaratie werd statisch afgewezen; de declaratie die door de
frontend kwam faalde bij de oracle. `ref/lib/arith.ml:60` (`restate_into`)
is de faultplek: de fold telt `EUR/mnd`-waarden op en het `EUR`-doel weigert
de toewijzing — `unit_name` toont alleen de teller, vandaar "EUR vs EUR".

## De routesplitsing (gemeten)

Er is geen aparte bouwroute: één builder maakt voor beide lezingen één
`Expr.Functie(TOTAAL, …)`. De routes scheiden op de **operandvorm**, en
alle bestaande plekken testen dezelfde velden:

* `ExpressieBouwer.gedurendeKenmerkFilter` (r. 713) — temporele route:
  `AttribuutRef … navigatieRol()==null && !a.set() && objecttype()==huidigSubject
  && attribuutGranulariteit.containsKey(…)`.
* de gedeclareerde-lid-check (r. 474) — set-route: `argumenten.get(0)
  instanceof AttribuutRef sa && sa.set()`.
* `totaalOperandFout` (r. 1158) — `temporeel` via `attribuutGranulariteit`.
* `ref/lib/eval.ml:951` splitst downstream op precies dezelfde vorm; alles
  wat niet in die arm valt gaat naar `Arith.agg_totaal` (r. 1157), de gewone
  instance-fold.

`functieType` was de **enige** plek die van geen van beide wist: de strip
vuurde onvoorwaardelijk voor TOTAAL. Dat was de fout.

## Aanpak (het gekozen spoor)

De set-route van TOTAAL typeert als SOM — geen strip, eenheid = elementeenheid
— en de temporele route blijft onaangeraakt. Eén predicaat
(`setLezing(args)`) op één wire-veld. Daarna een golden
`jutterswet-tempototaal` die de routesplitsing op de wire pint.

Acceptance:
- De genummerde voorvraag gesteld en beantwoord.
- Het gekozen spoor gebouwd, rood-eerst waar een pin bijkomt.
- Alle gates groen; herpin (frontend geraakt).

May touch: `frontends/regelspraak/src/**`, `corpus/golden/**`, `backlog/`.

Out of scope: de Drools- en clingo-kant (volgen de bestaande gates), de
verifier, de temporele route.

## De fix

`ExpressieBouwer.functieType`, arm `TOTAAL`: één predicaat ervoor.

```java
case TOTAAL -> setLezing(args) ? eerste
        : eerste instanceof Datatype.Numeriek n && n.eenheid() != null && n.eenheid().noemer() != null
        ? new Datatype.Numeriek(n.geheel(), n.decimalen(), n.teken(), n.eenheid().teller())
        : eerste;

private static boolean setLezing(List<Expr> args) {
    return args.size() == 1 && args.get(0) instanceof Expr.AttribuutRef a && a.set()
            && !(a.objecttype() != null && a.objecttype().attribuutGranulariteit.containsKey(a.attribuut()));
}
```

`setLezing(args)` is een set-lezing over een **niet-tijdlijn**-attribuut
(zie de fixronde hieronder voor de tweede helft en waarom die er moest
zijn) — het `set()`-veld dat `gedurendeKenmerkFilter` negatief test om de
temporele clip toe te laten, plús dezelfde granulariteitstoets die
`totaalOperandFout` al toepast. `eerste` is letterlijk wat de SOM-arm doet:
eenheid, decimalen, geheel en teken van het element, ongewijzigd. De
operandcontrole (`totaalOperandFout`, §4.20 Tabel 30) is **niet**
aangeraakt — dit besluit gaat over de typering.

De flank na de fix, dezelfde bron, dezelfde twee doeldeclaraties:

```
doel €/mnd:  ir: … geschreven   →   argument.type.eenheid = EUR/mnd
                                    functie.type.eenheid  = EUR/mnd  (noemer BEHOUDEN)
             ref:  oracle: PASS
doel €:      ERROR (line 17): berekening: toekenning:
             eenheden 'EUR' en 'EUR/mnd' zijn niet in elkaar om te rekenen
```

De runtimefault is een compile-time diagnostic geworden en de eerlijke
declaratie is de geaccepteerde.

## Herpin-risico voor de toolchain (read-only gemeten)

`regelspraak-java` heeft geen eigen resolver meer: er is **geen**
`src/main/**/resolver/ExpressieBouwer.java`, de repo compileert
`vendor/uchar-frontend/**` als haar enige frontend. Drie toolchain-tests
raken deze vorm; alle drie zijn opnieuw door de **gefixte** frontend
gehaald en houden:

* `ScenarioOrakelTest:765` (`aggregatie_lege_totaal_is_nul`) — de
  set-vorm die het besluit volgt, maar met **unitloze** operanden
  (`Numeriek (geheel getal)`). De strip was daar altijd al een no-op, dus
  de typering beweegt niet; de test pint bovendien alleen de
  **waarde-uitkomst** (leeg totaal = 0), geen typering. Gemeten: resolvet
  schoon.
* `RuleCheckTest.totaal_van_rate_operand_integreert_naar_de_teller_eenheid`
  — dit is de test die de strip wél pint, maar met de operand `de tarief
  van een Ding`: een scalaire eigen-lezing (`set = false`), dus de
  temporele arm. Gemeten: arm A resolvet schoon, arm B geeft nog steeds
  `… niet in elkaar om te rekenen`.
* `RuleCheckTest.totaal_van_niet_tijdseenheid_rate_wordt_gemeld` — raakt
  alleen `totaalOperandFout`, ongewijzigd.

Vierde raakvlak, gemeten na de fixronde: de differentialtest
`ExecModelDifferentialTest` Phase PE (`het totaal van <temporeel attr> van
alle <set>`, 120+30+0=150). Die vorm is precies de vorm die de fixronde
terug onder de strip bracht, maar haar operand is een **unitloos** `Bedrag`
— door de gefixte frontend gehaald typeert de knoop
`Numeriek(decimalen=2, eenheid=null)`, dus de strip is er een no-op en de
typering beweegt niet.

**Herpin-risico dus nul voor de bekende toolchain-tests.** Poort 11 zegt
hier **niets** over: de pariteitszone draagt **nul** `TOTAAL`-knopen (`grep`
over `toka/`: 0 hits op `"kind": "TOTAAL"` en 0 `.rgs` met "totaal van"),
dus haar byte-identiteit is geen bewijs voor deze wijziging — ze bewijst
alleen dat er niets anders is meegeschoven. De pin-bump is een gewone
conformance-PR, die de nieuwe typering meebrengt.

## De golden

`corpus/golden/jutterswet-tempototaal/` — eigen Jutterswet-materiaal,
rechtenvrij. Drie regels, één document, één eenheid (`€/mnd`):

| regel | operator | operand | operandtype | resultaattype |
|---|---|---|---|---|
| `bewaarlast` | `het totaal van` | set-lezing over een **niet-temporeel** attribuut (`set = true`) | `€/mnd` | **`€/mnd`** |
| `bewaarsom`  | `de som van`     | dezelfde set-lezing | `€/mnd` | `€/mnd` |
| `bewaargeld` | `het totaal van` | eigen tijdlijnattribuut (`set = false`) | `€/mnd` | **`€`** |

Rij 1 naast rij 2 pint het besluit (de alias); rij 1 naast rij 3 pint de
routesplitsing (de strip is de tijdsaggregatie alleen). Poort 10
her-emitteert `bron.rgs` byte-identiek, dus geen van beide kan nog stil
bewegen. De **vierde** vorm — een set-lezing over een *temporeel*
attribuut, die wél integreert — staat niet in deze golden; zij hoort bij
de ref-kaart die haar waarde-divergentie dicht.

**Caseloos, met reden.** Handberekend (J1 bewaart V1 = 12, V2 = 7,
V3 zonder tarief; J2 bewaart niets): `bewaarlast` J1 = 19 €/mnd, J2 = 0
(§4.20, onvoorwaardelijk); `bewaarsom` J1 = 19 €/mnd, J2 = **leeg**
(§4.10, geen fallback) — de alias strekt zich níét uit tot de
leegbron-regel. Uitvoerdersdracht, per route gemeten:

* set-route — `ref`: `oracle: PASS`; Drools: `PLAYED` op precies die
  verwachtingen; `backends/clingo`: **weigert**, letterlijk
  `TRANSLATION FAULT: expression kind not translated (no corpus case
  exercises it)`. De ASP-vertaler draagt `Som` met `set = true` en draagt
  `Totaal` alleen voor een temporele attribuutlezing; per regel geïsoleerd
  gemeten slaagt de `som`-helft (`clingo: PASS`) en weigert de
  `totaal`-helft. Poort 6 heeft géén weigerkanaal, dus een case zou poort
  6 rood maken — en verwachtingen verzwakken om een executor te laten
  slagen is niet aan de orde. De case wacht op de executor.
* temporele route — `ref` weigert óók, en om een andere reden:
  `EVALUATION FAULT: incompatible units: EUR vs EUR`. `Timeline.totaal`
  vermenigvuldigt elk segment met een **unitloze** telling, dus de
  `€/mnd`-eenheid overleeft de collapse en het `€`-doel weigert hem.
  Drools speelt diezelfde regel wél: op J1 met een tijdlijn van 10 €/mnd
  over 2024 geeft `bewaargeld` **120** — `PLAYED 1` — dus de strip is
  precies wat de derde executor uitvoert. Het is dus **geen open muntworp**
  ("strip of geen strip") maar een **ref-defect**: `ref` voert de
  Tabel-30-integratie op geen enkele route uit. Geen poort zag dat, omdat
  elke temporele `TOTAAL` in het corpus unitloos is. Deze golden maakt de
  claim zichtbaar op de wire; hem dichten is een ref-taak en staat als
  eigen kaart in `backlog/new/20260904-ref-tijdtotaal.md`.

`NOTE.md` draagt de vóór/na-flank, de weigerteksten letterlijk en de
handberekening — klaar om als case te landen zodra clingo de set-fold
`Totaal` draagt.

## Niet gedaan, bewust

Geen frontend-test toegevoegd: de testbestanden-vraag is voor deze kaart
niet gesteld. **De test die ik zou toevoegen**, in
`frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/EenheidIdentiteitTest.java`
(dat bestand resolvet hele bronnen en asserteert geëmitteerde typen):
`totaal_van_set_lezing_houdt_de_noemer` — dezelfde tweezijdige flank als
hierboven (doel `€/mnd` schoon, doel `€` de eenheidsmismatch), plus de
zusterassertie dat de `SOM`- en `TOTAAL`-knoop over dezelfde set een
**identiek** type dragen. De golden pint dat vandaag op de wire (poort
10); de unittest zou de diagnostiekkant erbij pinnen zonder een corpusrun.

## Fixronde (2026-09-04) — de blocker van lens 1

Lens 1 vond dat het gelande predicaat te **breed** was: het testte alleen
`a.set()` en doodde de noemerstrip daarmee óók op een vierde vorm die de
kaart niet in beeld had — de set-lezing over een **temporeel** attribuut.
Die vorm is geen instance-vouw maar de §7.1 **per-element-tijdlijnvouw**:
elk element wordt éérst over de tijd gevouwen (`rsTijdTotaal` op de
granulariteit van het attribuut) en pas daarna over de set. Zij
**integreert dus wél** over de tijd, en hoort onder de strip.

**De reproductie, vóór de fix** — dezelfde bron als de flank hierboven,
maar met `het bewaartarief … met eenheid €/mnd voor elke maand`:

```
doel €/mnd:  ir: … geschreven   →   argument.type.eenheid = EUR/mnd
                                    functie.type.eenheid  = EUR/mnd   ← FOUT
doel €:      ERROR (line 20): berekening: toekenning:
             eenheden 'EUR' en 'EUR/mnd' zijn niet in elkaar om te rekenen
```

Fout, want de uitvoerder integreert: op drie vondsten (10 €/mnd over heel
2024, 5 €/mnd over het eerste halfjaar, één zonder tijdlijn) speelt Drools
**150** (`PLAYED 1`) — 120 + 30 + 0, de per-element-vouw. Drie artefacten
zeggen hetzelfde: `ExecModelTranspiler` r. 962 en r. 1060-1065
("a SOM/TOTAAL over a temporal attribute of a set is a PER-ELEMENT timeline
fold"), `DrlTranspiler` r. 2882 en r. 2917, en de toolchain-differential
`ExecModelDifferentialTest` Phase PE, die 120+30+0=150 absoluut pint.

**Na de fix**, dezelfde twee doeldeclaraties, omgeklapt zoals het hoort:

```
doel €/mnd:  ERROR (line 20): berekening: toekenning:
             eenheden 'EUR/mnd' en 'EUR' zijn niet in elkaar om te rekenen
doel €:      ir: … geschreven   →   argument.type.eenheid = EUR/mnd
                                    functie.type.eenheid  = EUR   (gestript)
```

**De lezing van het besluit, expliciet genoteerd.** Het maintainerbesluit
zegt "de strip blijft alleen voor de tijdsaggregatie". Dat dekt deze vorm:
de per-element-vouw **ís** een tijdsintegratie, ook al staat er `set = true`
op de wire. De scheidslijn is dus niet `set()` maar *integreert deze vouw
over de tijd*, en het predicaat draagt beide helften.

De remedie is de granulariteitstoets die `totaalOperandFout` (r. ~1164-1165)
al toepast, hergebruikt in `setLezing` — één toets, geen tweede begrip. De
onware javadoczin ("an instance fold that never integrates over time") is
weg; de comment noemt nu de per-element-vouw met naam.

Gemeten na de fix: de golden `jutterswet-tempototaal` re-emitteert
**byte-identiek** (haar set-regel leest een scalair `€/mnd`-attribuut, dus
`setLezing` blijft daar waar), de twee toolchain-armen van
`RuleCheckTest.totaal_van_rate_operand_integreert_naar_de_teller_eenheid`
houden (A schoon, B `… niet in elkaar om te rekenen`), en de Phase-PE-bron
typeert `Numeriek(decimalen=2, eenheid=null)` — unitloos, dus de strip is
er een no-op en de differential beweegt niet.

Vier prosafixes liften mee: de getuigenkast-sectie van
`corpus/golden/README.md` telde twee verifier-only goldens waar er nu drie
zijn; de vacuüme poort-11-bewijsclaim en de onjuiste toolchain-typeringszin
hierboven; het NOTE-zinnetje dat de alias het **resultaattype** dekt en niet
het §4.20-operandcontract (gemeten: `de som van <€>` resolvet schoon, `het
totaal van <€>` geeft `totaal van vereist een eenheid per tijdseenheid
(§4.20 Tabel 30), kreeg 'EUR'`). En de losse meting is een eigen kaart
geworden: `backlog/new/20260904-ref-tijdtotaal.md`.

## Poorten, alle dertien

     1 check-self-contained: PASS
     2 check-ir-schema: PASS (schema valid; 175 file(s) validate and round-trip)
     3 check-verifier: PASS (175 golden clean; 243 negatief rejected for their declared rule)
     4 check-case-schema: PASS (schema valid; 425 case file(s) validate)
     5 check-oracle: PASS (403 cases replayed exactly; 3 no-case goldens)
     6 check-clingo: PASS (403 cases replay exactly through clingo)
     7 check-noop: PASS (367 cases refute a do-nothing executor; 36 pinned survivors)
     8 check-rood: PASS (22 red cases fault observably in ref en clingo; Drools-leg 5 fault-pinned, 17 not-carried)
     9 check-catala: PASS (frontend suite 201 checks; 12 catala golden(s) byte-identiek)
    10 check-regelspraak: PASS (JUnit 171 tests; 29 rgs golden(s) byte-identiek)
    11 check-toka-parity: PASS (45 .rgs byte-identiek, 2 holdouts; TOKA suite 27 tests; 29 replay-slices door ref/clingo/Drools, nul stille divergenties)
    12 check-drools: PASS (played 106, auto-excluded 43, value-divergence 1, no-case 3, JUnit 198 tests)
    13 check-reftype-derivation: PASS (52 unit-annotaties gedraaid over 11 goldens; 39 executor-replays; 0 gepinde overlevenden)

Na de fixronde opnieuw gedraaid, alle dertien: **elke teller identiek**.
Dat is precies wat de versmalling belooft — zij raakt uitsluitend de vorm
"set-lezing over een tijdlijnattribuut", die geen enkel corpusartefact
draagt. Poort 5 en 12 noemen de drie caseloze goldens nu bij naam
(`jutterswet-seizoenvondst`, `jutterswet-tempototaal`,
`jutterswet-vormenkabinet`).

Vier tellers bewegen ten opzichte van vóór de kaart, alle vier door de
nieuwe golden: poort 2 en 3 van
174 naar 175 bestanden, poort 5 en 12 van 2 naar 3 caseloze goldens,
poort 10 van 28 naar 29 `.rgs`-goldens. Poort 13's **twistelling** blijft
52 over 11 goldens — een caseloze golden wordt niet herspeeld — maar de
corpusbrede boekhouding groeit van 84 naar 93 unit-dragende annotaties
(+9: drie doelen, drie argumenten, drie functietypen), waarvan alle negen
twistbaar. De klem van stap 6 ontmoet daarmee voor het eerst een
unit-dragende `Functie/TOTAAL`; die positieklasse staat al in de
twisttabel, dus de klem valt niet. Geen enkele bestaande pin is geraakt:
alle 28 eerdere `.rgs`-goldens her-emitteren byte-identiek.

## Result (2026-09-04)

De voorvraag beantwoord en het besluit gebouwd: de set-lezing van `het
totaal van` over een **niet-temporeel** attribuut is een alias van `de som
van` en houdt de noemer; de strip blijft de **tijdsaggregatie**. Eén
predicaat in `functieType` — na de fixronde met twee helften, want de
per-element-tijdlijnvouw over een set integreert wél en hoort onder de
strip. Geen ander gedrag beweegt: 28 bestaande `.rgs`-goldens en 45
pariteits-`.rgs` byte-identiek, en na de versmalling ook de kaart-eigen
golden. De flank is omgeklapt en gemeten, in beide richtingen: op de
scalaire set-lezing werd eerst de eerlijke declaratie (`€/mnd`-doel)
statisch afgewezen en faalde de geaccepteerde (`€`-doel) bij de oracle —
nu andersom; op de temporele set-lezing typeerde de eerste fix `€/mnd`
waar Drools naar `€` integreert (`PLAYED` op 150) — nu weer `€`. Het
herpin-risico is nul: vier toolchain-raakvlakken read-only gemeten en alle
vier houden, waaronder de Phase-PE-differential, wier operand unitloos is
zodat de strip er een no-op blijft. De golden `jutterswet-tempototaal` zet
de drie lezingen naast elkaar op de wire en is caseloos met reden: `ref` en
Drools dragen de set-fold, `clingo` weigert hem reden-dragend en poort 6
heeft geen weigerkanaal. De nevenmeting is een eigen kaart geworden
(`backlog/new/20260904-ref-tijdtotaal.md`): de §4.20-integratie is een
`ref`-defect, geen open vraag — Drools voert haar uit (120 op de
tijdlijnvouw, 150 op de per-element-vouw) terwijl `ref` `EUR vs EUR`
faultet respectievelijk 0 geeft. Alle dertien poorten groen; frontend
geraakt, dus herpin bij de landing.
