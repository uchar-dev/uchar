# fold-in-element-condition: een vouw met filter over een relatie van het regelonderwerp, in de elementvoorwaarde van een andere vouw

*Grens gevonden door een regelsetbouwer, 2026-10-02 (brug K13 van de regelset herleving-WW v2, kernfix 3 na de eval),
nagemeten door de hoofdsessie op 2026-10-20 op uchar `60979bf2` via de distributie `f5adbd1a` en ref en clingo uit
`_build`. Luid: de vorm met filter geeft check exit 1; de vorm zonder filter een WARNING en een fout bij de evaluatie;
geen fout antwoord.*

## Aanleiding

De norm: de eerste maand zonder overschrijding, niet eerder dan de maand van het laatste einde van een bepaalde soort
gronden. De directe zin is een vouw (de laatste einddag van de zware klachten van de schuit) in het filter van een andere
vouw (de eerste maand van de ladingen van de schuit). De regelset herleving-WW zet daarom het laatste einde in de
voorwaarden van de regel en vouwt over de maanden vanaf de beëindigingsdatum (brug K13); wat die brug niet ziet, staat
in haar open punt 42. Verwant: open punt 5 van `done/20260926-frontend-breuk-en-navigatiekeuze.md`, "een `sourcePath`
vanaf een ander anker dan het onderwerp spelt de frontend niet (featurevraag)".

Getuige (kop):

    Eenheidsysteem Tijd
        de dag dg
        de maand mnd

    Domein Klachtsoort is van het type Enumeratie
        'Zwaar'
        'Licht'

    Objecttype de Schuit (mv: Schuiten)
        de eerste schone maand vanaf-de-eindmaand Datum in dagen;

    Objecttype de Lading in-een-maand (mv: Ladingen in-een-maand)
        de maand Datum in dagen;
        het gewicht Numeriek (geheel getal);
        de overlading kenmerk (bezittelijk);

    Objecttype de Klacht (mv: Klachten)
        de einddag Datum in dagen;
        de soort Klachtsoort;

    Feittype lading in-een-maand
        de schuit Schuit
        de lading (mv: ladingen) Lading in-een-maand
    Eén schuit heeft meerdere ladingen

    Feittype schuitklacht
        de beklaagde schuit Schuit
        de klacht (mv: klachten) Klacht
    Eén beklaagde schuit heeft meerdere klachten

    Regel overlading
        geldig altijd
            Een Lading in-een-maand heeft de overlading
            indien zijn gewicht groter is dan 100.

    R1  Regel eerste schone maand vanaf de eindmaand
            geldig altijd
                De eerste schone maand vanaf-de-eindmaand van een Schuit moet berekend worden als de eerste van de maand van alle ladingen van de Schuit die aan alle volgende voorwaarden voldoen:
                - hij heeft geen overlading
                - zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'.
        nu: check exit 1, "de 'die/dat'-deelselectie 'aan alle volgende voorwaarden voldoen: - hij heeft geen overlading
        - zijn maand plus 1 mnd is later dan de laatste van de einddag van alle klachten van de Schuit' wordt niet omgezet
        en zou stil wegvallen: …", met WARNINGs dat 'zijn soort' en ''Zwaar'' onopgelost zijn: de binnenste "die …
        voldoen:" komt niet bij "alle klachten".
    R2  de binnenste vouw tussen haakjes, of als variabele: check exit 1, "vouw '…' zonder bereik (… een ander anker dan
        het onderwerp …)" (de variabele: kaart `rule-variable-in-subselection`).
    R3  R1 zonder filter: check exit 0 met "WARNING: tussenstap in een vouwbron: 'alle klachten van de Schuit' bereikt de
        elementen via Schuit, maar een vouwbron draagt precies één relatie vanaf het regelonderwerp Lading in-een-maand
        (IR-SPEC ch. 4.3: één elementType plus één sourceRole) — de tussenstap zou spoorloos wegvallen, dus de aggregatie
        blijft onopgelost (§13.4.1/§13.4.17)"; scenario "ERROR ComputeFault: … Unresolved reached in evaluation: this
        rule fails observably".
    R4  R3 via de rol van het element ("alle klachten van de schuit van de Lading in-een-maand"): check exit 0 met
        "WARNING: keten zonder sourcePath in een vouwbron: … dus de aggregatie blijft onopgelost (§13.4.17)", terwijl het
        IR de vouw met sourcePath [lading in-een-maand.schuit, schuitklacht.klacht] draagt en Drools en ref de
        verwachte uitkomst geven; clingo: kaart `clingo-nested-aggregate-in-fold-filter`. Met filter: als R1.
    R5  de brug: "De laatste einddag van een Schuit moet berekend worden als de laatste van de einddag van alle klachten
        van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan 'Zwaar'", en R1 leest "de laatste
        einddag van de Schuit": check exit 0; PASS in Drools, ref en clingo.

    Casussen: zware klachten met einddag 15-03-2026 en 10-02-2026, een lichte met 20-05-2026; ladingen februari (50),
    maart (50), april (150) → 01-03-2026. Eén zware klacht 15-03-2026; februari (50), maart (150), april (50) →
    01-04-2026.

## Doel

R1, de zin van de norm, geeft check exit 0 zonder WARNING en de uitkomsten van R5 (01-03-2026 en 01-04-2026) in ref ≡
clingo ≡ Drools. Daarvoor: (1) de "die … voldoen:" van de binnenste vouw hoort bij haar eigen verzameling; (2) een vouw over
een relatie van het regelonderwerp in een filter krijgt een bereik vanaf het regelonderwerp (via het pad van het element
naar het onderwerp, zoals R4 al krijgt, of via een `Let` op de regel; zie `rule-variable-in-subselection` voor de
afweging); (3) geen diagnostic zegt "blijft onopgelost" waar het IR de vouw gelowerd draagt (R4). Rood-eerst met R1, R3 en
R4; alle gates groen.

## Voorlopige besluiten (2026-10-22, naar het Fable-advies)

Bron: `uchar-dev/research/bruggen-2026-10-22/fable-advies.md`, vraag 2a (B2.1, B2.2, B2.4). Deze kaart wordt samen met
`rule-variable-in-subselection` gebouwd, na `semantic-tokens` en `subselection-per-chain`. De eigenaar kan de besluiten
omdraaien.

- **Route (i): herankering door een padprefix.** Een lezing die bij het regelonderwerp hoort en onder een ander
  onderwerp gelezen wordt (filter of matrix van een vouw; inline of als variabele van `Daarbij geldt:`, ook een vouw
  met filter), krijgt haar bij declaratie gebouwde `Expr` terug met vóór elke verwijzing die aan het regelonderwerp
  verankerd is het pad element → regelonderwerp; een vouw met `sourceRole` vanaf het onderwerp wordt zo een
  `sourcePath`. De definitietekst wordt niet opnieuw gebouwd met het element als onderwerp ("zijn X" zou dan het
  element lezen). Het bestaande mechanisme is de éénstapsherankering van 2026-10-20 (`roleTargetTypeToSubject` met
  `toManyRole`, `ExpressionBuilder` ±2806–2820). Route (ii), een `Let` op de regel, niet nu: een `Let` overspant geen
  voorwaarde (IR-SPEC ch. 4.10 (a)), zou de betekenis bij een lege waarde veranderen (ch. 4.10 (c), ch. 10.6), en raakt
  twee open gaten (Drools open punt 6, `20261009-clingo-let-binder-tussen-onderwerpen`).
- **Een weg terug bestaat alleen** als de buitenste vouw over een relatie van het regelonderwerp loopt en elke stap vanaf
  het element terug to-one is. Anders een ERROR die de vouw, het element en de ontbrekende weg terug noemt, met de uitweg
  (een eigen lid op het onderwerp); nooit een WARNING met een marker.
- **Element van het type van het regelonderwerp:** weigeren als RGS-350 (de naam is dubbelzinnig).
- **Geen verouderde WARNING:** RGS-073/075 uit de eerste bouw vervallen zodra de herbouw bindt (doel (3)).
- **Bekende grens voor de uitleg:** een vouw in een filter wordt per lid geëvalueerd en heeft geen eigen lezing (ch.
  5.12); de Result noemt dat, en de hoofdsessie zet het in PLAN §9. Ch. 5.12 verandert niet.
- Rood eerst: R1, R3, R4 van deze kaart en R1, R4 van `rule-variable-in-subselection`; één golden op de
  Jutterswet-casus van deze kaart (01-03-2026 en 01-04-2026), gespeeld in gates 5, 6, 12 en 17.

## Result (2026-10-22)

Tak `card/fold-in-element-condition`, samen met `subselection-per-chain` (eerst) en `rule-variable-in-subselection`
(hetzelfde mechanisme; haar Result verwijst hierheen). Geraakt: `frontends/regelspraak/**` (`ExpressionBuilder`,
`RuleChecker`, een publieke overload van `IrEmitter.rangeRole`, het register en beide catalogi, twee nieuwe
testklassen, twee testlijsten uitgebreid, `grammar/DIVERGENCES.md`), de golden
`corpus/golden/jutterswet-schone-maand-na-zware-klacht/`, `EXPECTED_PLAYED` in `scripts/check-drools.sh` en de drie
kaarten. Niet geraakt: `ref/`, `backends/`, `spec/`, `toka/`, `CLAUDE.md`, `spec/PLAN.md`, `spec/CONSTRUCTS.md`.

Op main `f4a55f8a` gerebased (de landing van `date-with-in-ref-and-clingo` en `clingo-nested-aggregate-in-fold-filter`).

- `6d973cc9` de drie kaarten naar `doing/`.
- `b082d7b5` `subselection-per-chain` (zie die kaart).
- `039e7eec` de herankering, de weigeringen, de typecontrole van een deelselectie op het element, de golden,
  `EXPECTED_PLAYED` (de commit noemt 265, van vóór de rebase; na de rebase 267 → 268).
- `b90a6a05` een lezing zonder type en de operatoren over de eigen tijdlijn blijven buiten de herankering; twee
  getuigen erbij (een variabele in het filter van een variabele, een variabele in de toets van een kwantor).
- `56634c2e` de golden `fold-filter-inner-fold-via-element-role` (R4, van de gelande tak) opnieuw uit haar
  `source.rgs`: het IR verliest alleen de verouderde WARNING in `diagnostics`; haar NOTE zegt dat.
- de commit van deze Results.

### Wat er is

**Route (i), herankering door een padprefix** (`ExpressionBuilder.frame`, `reanchor`). Een filter van een vouw wordt
gebouwd binnen een `Frame`: het onderwerp waarvan de vouw geschreven is, het elementtype, de weg terug van een element
naar dat onderwerp (per stap van het bereik, van achteren, de andere rol van het feittype van die stap; elke stap
to-one) en de deelselectie zoals geschreven. Drie lezingen in zo'n filter horen bij het buitenste onderwerp en worden
daar gebouwd zoals geschreven, waarna `reanchor` vóór elke lezing en elk bereik dat bij dat onderwerp begint de weg
terug zet:

- een vouw waarvan het geschreven bereik verankerd is aan het buitenste onderwerp, met de eigen typenaam ("de
  Schuit"), met of zonder eigen deelselectie (`foldAtAnchor`, de haak vooraan `buildFunction`);
- een variabele van de regel (`variableInFilter`, in `nameRef`; de kaart `rule-variable-in-subselection`);
- in de toets van een kwantor over één relatie: de operand naast de gekwantificeerde, die bij het regelonderwerp
  gebouwd wordt (`RuleChecker.matrixFromElement`).

Een lezing wordt een navigatie over de weg terug en haar eigen stappen (één stap als `navigationRole`, meer als
`navigationPath`); het bereik van een vouw wordt een `sourcePath` (IR-SPEC ch. 4.3, WF-102), en de verifier toetst
WF-102 (d) ten opzichte van het filteronderwerp. De definitietekst wordt niet opnieuw gebouwd met het element als
onderwerp. Het nieuwe pad wordt nagerekend met de afleiding van de emitter (`IrEmitter.navigationBindings` vanaf het
element moet precies het pad geven). Een rol van het element ("de schuit"), een bezittelijke vorm ("zijn schuit") en
een naam die pas na het weglaten van een woord een type noemt, blijven de wandeling vanaf het element volgen (en
weigeren zoals voorheen, RGS-131).

**IR van R1** (de inner vouw in het filter van de ladingen, zonder `source`):

```
{"node": "Aggregation", "kind": "last",
 "arguments": [{"node": "AttributeReference", "attribute": "einddag", "subjectType": "Klacht"}],
 "elementType": "Klacht",
 "filter": {"node": "Compound", "quantifier": "the", "threshold": 0, "parts": [{"node": "Comparison", "op": "equal",
   "left": {"node": "AttributeReference", "attribute": "soort", "subjectType": "Klacht"},
   "right": {"node": "Literal", "value": {"enumeration": "Zwaar"}, …}}]},
 "sourcePath": [{"node": "RoleBinding", "factType": "lading in-een-maand", "role": "schuit"},
                {"node": "RoleBinding", "factType": "schuitklacht", "role": "klacht"}], …}
```

Dat is, op de bronnen na, het IR van R4 (de spelling via de rol van het element) met hetzelfde filter
(`FoldInElementConditionTest.r1_reads_the_klachten_of_the_rule_subject_over_the_way_back`); R2 met haakjes is R1, R3
zonder filter is R4 zonder filter.

**De weigering zonder weg terug** (RGS-352, ERROR, nooit een WARNING met een marker). Is een stap terug to-many ("meerdere
schuiten hebben meerdere ladingen") of het feittype wederkerig, dan wijst een element niet één regelonderwerp aan. De
melding noemt de lezing, het regelonderwerp, het element, de deelselectie en de stap, met de uitweg (R1 met "meerdere
schuiten hebben meerdere ladingen"; de deelselectie hier ingekort tot '…'):

> 'de laatste van de einddag van alle klachten van de Schuit die aan de volgende voorwaarde voldoen: zijn soort is
> gelijk aan 'Zwaar'' hoort bij het regelonderwerp Schuit, en de deelselectie '…' wordt per Lading in-een-maand
> gelezen, met het element als onderwerp: het IR leest daar vanaf het element en heeft geen verwijzing naar het
> buitenste onderwerp, dus de lezing komt alleen over een weg terug van Lading in-een-maand naar Schuit bij haar
> onderwerp, en die schrijft deze frontend hier niet: in het feittype 'lading in-een-maand' heeft een Lading
> in-een-maand meer dan één 'schuit', dus de weg terug wijst niet één object aan. Schrijf de waarde als eigen lid van
> Schuit, met een regel die haar zet, en lees dat lid in de deelselectie

RGS-352 weigert ook een telling over de weg terug (een telling telt over één relatie, WF-102 (c)) en een vorm zonder
spelling vanaf het element (een keuze, een vereniging, een vouw over een dimensie, de operatoren over de eigen
tijdlijn). In de toets van een kwantor blijft de operand zonder weg terug zoals gebouwd, en spreken de weigeringen die
er al waren (RGS-348 over een pad, een ongebonden lezing, RGS-131).

**Eigen type.** Een vouw verankerd aan "de Schuit" in een deelselectie over Schuiten blijft RGS-350 (zoals sinds
2026-10-20). Een variabele van de regel in zo'n deelselectie is nu ook RGS-350 (zie vondst 1).

**Geen verouderde WARNING.** De typecontrole van de voorwaarden van een regel (`RuleChecker.conditions`) bouwde de
voorwaarden van een deelselectie met het regelonderwerp: daar kwam RGS-073 van R4 vandaan, terwijl het IR de vouw
gelowerd droeg. Ze leest die voorwaarden nu met het element en de frames waarmee het filter gebouwd is
(`ExpressionBuilder.inFilterScope`); RGS-073 en RGS-075 vallen bij R1, R3 en R4 niet meer. Gevolg: een typefout in een
filter wordt nu gezien (vondst 2).

**Rood eerst.** `FoldInElementConditionTest` (9) en `RuleVariableInSubselectionTest` (9): R1, R2, R3, R4 en de
weigeringen, eerst rood gemeten (12 van 16 rood; groen bleven de vormen die al speelden en RGS-350 op een vouw).
`SubselectionPerChainTest` (5) van de eerste stap.

**De golden** `jutterswet-schone-maand-na-zware-klacht`: R1 als bron (`source.rgs`, de directe zin met de
soortfilter), vier scenario's in `scenario.rgs` met handberekening in `NOTE.md`: de twee van de kaart (01-03-2026 en
01-04-2026; in de eerste een lichte klacht met een latere einddag, die zonder filter het antwoord leeg zou maken), een
met twee schuiten die elk hun eigen klachten lezen (01-03-2026 en 01-05-2026), en een met alleen een lichte klacht
(leeg). ref en clingo PASS op de vier, beide Drools-takken PLAYED 4, gate 17 vergelijkt de lezingen van ref en beide takken
zonder afwijking. clingo speelt haar sinds de rebase: de vertaling van een aggregaat in een vouwfilter kwam met
`clingo-nested-aggregate-in-fold-filter`; vóór de rebase faalde clingo op de geneste `#max` (een syntaxfout), dus een
divergentielijst in gate 6 was niet nodig. `EXPECTED_PLAYED` 267 → 268.

**Meting 5** (zonder bestand in de repository; werkmap `werk/bruggen/k13/v2/`). De v2-regelset (werkbank `c53dfd9`,
fundament en herleving-ww) met in `hww-16-inkomen.rgs` het lid van brug K13 als directe zin: "De eerste kalendermaand
zonder overschrijding-vanaf-de-eindmaand van een Recht op uitkering WW moet berekend worden als de eerste van de
kalendermaand van alle inkomens van het Recht op uitkering WW die aan alle volgende voorwaarden voldoen: - hij heeft
geen herlevingsgrens overschreden - zijn kalendermaand is later dan de beëindigingsdatum van het Recht op uitkering WW
min 1 dg - zijn kalendermaand plus 1 mnd is later dan de laatste van de einddatum-van-een-eindigingsgrond van alle
eindigingsgronden van het Recht op uitkering WW die aan alle volgende voorwaarden voldoen: .. hij heeft de
kalendermaandsystematiek .. zijn code reden beëindiging recht op uitkering ww is ongelijk aan 'Hoogte inkomsten uit
verzekerde arbeid'." Op main `8ded3090`: exit 1 (RGS-099, drie WARNINGs); op deze tak: exit 0, `diagnostics: []`,
verifier PASS, de binnenste vouw met `sourcePath [inkomen in-een-kalendermaand.recht,
RechtOpUitkeringHeeftEindigingsgrondRechtOpUitkering.eindigingsgrond]`. Dezelfde zin met de laatste einddag als
variabele van `Daarbij geldt:` geeft hetzelfde IR op de bronnen na (op main: RGS-131). De onveranderde v2: exit 0,
geen diagnostic en een bytegelijk IR met main (`subselection-per-chain` (c)).

### Bekende grens voor de uitleg

Een vouw in een filter (of in de toets van een kwantor) wordt per lid geëvalueerd en heeft geen eigen lezing
(RUNTIME-SEMANTICS ch. 5.12: een knoop die per lid gelezen wordt, houdt zijn nummer): de uitleg toont welke ladingen
slagen, niet de laatste einddag met haar bijdragers. Ch. 5.12 verandert niet.

**Voorstel voor PLAN §9** (de hoofdsessie zet het erin):

> **Uitleg van een vouw in een filter (K13, 2026-10-22).** Sinds `fold-in-element-condition` schrijft de frontend een
> vouw over een relatie van het regelonderwerp in het filter of de toets van een andere vouw als `sourcePath` vanaf het
> element (route (i)). Zo'n vouw wordt per lid geëvalueerd en heeft geen eigen lezing (ch. 5.12): de uitleg toont welke
> leden slagen, niet de waarde van de binnenste vouw en haar bijdragers. Een verfijning — een filterknoop die niets van
> het element leest één keer per onderwerp lezen — is een specvraag op zich; tot dan geldt de grens. Route (ii), een
> `Let` op de regel, zou die lezing geven, maar wacht op Drools open punt 6 en
> `20261009-clingo-let-binder-tussen-onderwerpen`, en verandert de betekenis bij een lege waarde (ch. 4.10 (c),
> ch. 10.6).

### Vondsten

1. *Frontend, gefixt (getuige `RuleVariableInSubselectionTest.a_variable_in_a_subselection_over_the_rule_subjects_own_type_is_refused`).*
   Een variabele van de regel in een deelselectie over elementen van het type van het regelonderwerp las stil de
   waarde van het element: "de peildag is zijn peildatum", gelezen in "alle gesleepten van de Schuit die aan de
   volgende voorwaarde voldoen: zijn peildatum is later dan de peildag", ging zonder diagnostic naar schijf en las de
   peildatum van de gesleepte. Nu RGS-350.
2. *Frontend, gefixt (getuige `FoldInElementConditionTest.a_comparison_in_a_filter_is_typed_on_the_element`).* De
   typecontrole bouwde de voorwaarden van een deelselectie met het regelonderwerp, dus de operanden van het element
   waren daar meestal onbekend en een typefout ging door: "zijn gewicht is later dan 01-01-2026" in een deelselectie
   over ladingen gaf geen fout. Nu de typefout. Geen golden, geen TOKA-bron en geen scenario-golden raakt dit (gates 10,
   11, 16 bytegelijk).
3. *Normkeuze, niet gebouwd.* RGS-348 weigert de toets van een kwantor over een pad die het regelonderwerp leest,
   omdat "geen wandeling vanaf het element het onderwerp noemt". Waar elke stap terug to-one is, noemt de weg terug het
   wel; route (i) zou die toets kunnen schrijven. De kaart besliste de toets over één relatie; het pad laat ik bij de
   weigering van 2026-10-20.
4. *Spec, ter kennisname.* Een telling over een relatie van het regelonderwerp in een filter heeft geen spelling: haar
   bereik zou een pad worden, en WF-102 (c) laat een pad alleen toe als bereik van een waardevouw. Nu RGS-352; een
   telling over een pad is de open flank van ch. 3.4b (consequentie 2).
5. *Tests uitgebreid, niet verzwakt.* `MarkerReasonPinTest.SANCTIONED` en `MarkerReportTest.CLASSIFICATION` kregen elk
   twee nieuwe families van een marker met reden null (`refuseInFilter`: RGS-352; `variableInFilter`: RGS-350), elk met
   de ERROR die de marker dekt. Geen bestaande test pinde RGS-073/075 of RGS-099 op deze vormen; niets aangepast.
6. *Proces.* Eén keer een `rm -f` met een relatief glob-pad in de golden-map gebruikt (bij het opnieuw genereren van de
   casebestanden); de bestanden zijn daarna opnieuw geschreven. Tegen de werkafspraak; niet herhaald.

### Gates

De volledige set op de eindstand `56634c2e` (na de rebase), `werk/bruggen/k13/gates-1.summary.txt`: alle zeventien
rc=0, `EXIT 0`. Uit de logs: gate 5 860 cases van ref exact; gate 6 860 cases door clingo; gate 10 1259 JUnit-tests en
131 rgs-goldens bytegelijk; gate 11 PASS; gate 12 played 268 op beide takken (exec-model en DRL-tekst); gate 16 33
scenario-goldens, 114 casebestanden; gate 17 268 goldens en 731 casebestanden vergeleken, 0 afwijkingen. Tussendoor
(vóór de rebase) gates 5, 12 `--fast` en 17 los voor de golden: groen, played 265.
