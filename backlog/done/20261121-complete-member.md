# complete-member: een gegeven dat de regelset als volledig declareert

*Opgesteld door de hoofdsessie op 2026-11-21. De lezing van een document dat uit leeg niets afleidt staat in
`spec/RUNTIME-SEMANTICS.md` ch. 1.5; de referentie-interpreter rekent haar (kaart `open-values-in-ref`).*

## Aanleiding

In een regelset met `Afleiden uit leeg: nee` is een invoergegeven dat de case niet geeft open: er staat niets over
vast, en wat het leest staat ook niet vast. Voor sommige gegevens weet de auteur dat de invoer volledig is: geeft de
case geen waarde, dan is er geen waarde. Ch. 1.5 kent dat als *complete input*, maar alleen als instelling van de
sessie; een regelset kan het niet zeggen. Daardoor kan een auteur geen enkel gegeven sluiten, en beslist de berekening
onder een opgave (`(mag opgegeven worden)`) niet zolang de case geen opgave geeft: de opgave is dan open, en de
berekening eronder wacht op haar.

Een voorbeeld uit de Jutterswet:

```
Afleiden uit leeg: nee

Objecttype de Vondst
    het gewicht Numeriek (geheel getal);
    de taxatie Numeriek (geheel getal);
    de toeslag Numeriek (geheel getal) (volledig);
    is drijfhout kenmerk (volledig);
    het bergloon Numeriek (geheel getal) (mag opgegeven worden);
```

"De uitbetaling van een Vondst is zijn taxatie plus zijn toeslag": bij taxatie 50 en zonder toeslag is de uitbetaling
50, want de toeslag is volledig en dus vaststaand leeg (ch. 1.2: een leeg lid telt in een plus als 0); zonder
`(volledig)` was de uitbetaling open. "De drijftoeslag van een Vondst is 2 indien hij niet drijfhout is" is 2 waar de
case drijfhout niet noemt. "Het bergloon van een Vondst is zijn gewicht maal 2": bij gewicht 10 is het bergloon zonder
opgave 20, met een opgave van 30 is het 30.

## Goal

Een declaratie van een attribuut of een kenmerk kan zeggen dat de invoer van dat lid volledig is. De IR draagt dat als
een veld op de declaratie, de referentie-interpreter leest het, de RegelSpraak-frontend schrijft het voor `(volledig)`
en voor de leden die een opgave genereert, en elke lezer van de wire kent het veld.

## Wat er komt

1. **De wire.** `complete: true` op `Attribute` en op `Characteristic` (IR-SPEC ch. 2, de tabellen van de twee
   declaraties), geschreven als `true` of niet, zoals `inferFromEmpty` alleen `false` kent. Kernveld, geen presentatie.
   Plaats in de canonieke volgorde: na `timeline`, vóór `origin`. Betekenis, in de woorden van ch. 1.5: op een object
   waar het lid invoer is, is het lid zonder waarde vaststaand leeg, en staat van een kenmerk dat de case niet noemt
   vast dat het niet geldt. Op een object waar een vorm het lid schrijft, volgt het lid zijn schrijvers en verandert
   het veld niets. In een document zonder `inferFromEmpty: false` is niets open en verandert het veld geen uitkomst.
   Schema, verifier (AST, lader, emitter met byte-gelijke rondgang), de zelftest van gate 2 (de vorm: alleen `true`; de
   plaats in de volgorde). Geen WF-regel: het veld heeft geen eis buiten zijn vorm. Een subtype erft het lid met zijn
   veld.
2. **ref.** `Eval.open_inputs` leest het veld van de declaratie: een lid is geen open invoer waar zijn declaratie
   `complete` draagt. De sessie houdt één instelling over, "elk invoerlid is volledig" (`runner --closed-inputs`, de
   poot voor behoud in gate 5). De lijst van leden per sessie (`Eval.Members`, `closed.json` in twee fixtures) vervalt:
   de fixtures en elke test die haar gebruikt (`WV-1.5-closed-*`, rijen van `WV-1.5-stack-opgave` en
   `WV-1.5-past-undetermined-filter`, het register van `WV-1.5-monotone`) zeggen het met het veld in hun document. Er komt geen tweede plaats die
   de eigenschap `inferFromEmpty` leest.
3. **RUNTIME-SEMANTICS ch. 1.5.** De alinea "Complete input" zegt het veld en de instelling van de sessie; de alinea
   over de opgave zegt dat een opgave die als volledig gedeclareerd is voorrang geeft waar de case haar niet geeft; de
   alinea "The two requirements" noemt volledige invoer al als sluiting. Elk voorbeeld is een test met zijn naam; een
   test dat het veld in een document zonder de eigenschap geen uitkomst verandert.
4. **De frontend.** Een token `(volledig)` als heel token met zijn haakjes, zoals `MAG_OPGEGEVEN_WORDEN`, zodat
   `volledig` als naamwoord vrij blijft. Op een attribuut staat het op de plaats van de opgave-marker (na de tijdlijn
   en de afwezigheidsmarkering), op een kenmerk na de tijdlijn. De resolver zet `complete` op de declaratie; de emitter
   schrijft het veld. De twee attributen die een opgave genereert, `opgegeven X` en `berekende X`, dragen het veld
   altijd: de opgave mag gegeven worden, dus zonder opgave is er geen; en `berekende X` is van de regels, nooit van de
   case. Geweigerd met een eigen melding (één RGS-code per reden, in beide catalogi): `(volledig)` naast
   `(mag opgegeven worden)` op één attribuut (het attribuut is dan het doel van een stapel en geen invoer), en
   `(volledig)` op een lid dat een regel of een defaults-groep op het declarerende type zelf schrijft (wat
   `Eval.open_inputs` als schrijver telt: het lid is dan op geen enkel object invoer, en de marker zou stil niets
   doen; een schrijver op een subtype alleen is geen reden, want op een object buiten dat subtype is het lid invoer). `grammar/DIVERGENCES.md` krijgt de sectie, met de meting van §6 en §7
   herhaald: elk `.rgs`-document van de repository door de frontend van vóór en van na, en geen verschil buiten de
   goldens met een opgave.
5. **De andere lezers van de wire.** De lader van de Drools-backend (`IrLoader`) laat het veld toe op de twee
   declaraties en doet er niets mee: de backend weigert een document met `inferFromEmpty: false` (DROOLS-370), en
   zonder die eigenschap verandert het veld niets. clingo leest de AST van de verifier en doet er evenmin iets mee
   (CLINGO-049). De OCaml-frontends zetten het veld op `false` waar zij een declaratie bouwen.
6. **Het corpus.** De acht goldens met `(mag opgegeven worden)` krijgen een opnieuw geëmitteerd `ir.json` (het veld
   op hun twee gegenereerde attributen), en spelen hun cases ongewijzigd in de drie executors. Een nieuwe golden
   `jutterswet-volledig-gegeven` met `Afleiden uit leeg: nee`: een attribuut en een kenmerk met `(volledig)`, hetzelfde
   attribuut en kenmerk zonder (in één document, onder een andere naam), een opgave boven een berekening; cases met
   handberekende verwachtingen voor: het volledige attribuut zonder waarde (vaststaand leeg, de lezer rekent door), het
   niet volledige zonder waarde (open), het volledige kenmerk niet genoemd (staat vast dat niet, de ontkennende regel
   vuurt), de opgave gegeven, de opgave niet gegeven (de berekening beslist), de opgave niet gegeven en de berekening
   open. clingo en Drools weigeren haar bij naam (de kanaallijsten van gate 6 en 12, en gate 17 zoals voor
   `jutterswet-afleiden-uit-leeg`).

## De architectuur (bindend)

- Eén manier om een lid volledig te noemen: het veld op de declaratie. De sessie-instelling "alles volledig" bestaat
  alleen voor de poot voor behoud.
- De eigenschap `inferFromEmpty` wordt in ref nog steeds op één plaats gelezen voor de evaluatie (`open_inputs`) en op
  één voor de weigering (`refuse_uncarried`).
- Geen executor leest de herkomst (`origin`) van een declaratie om te weten of zij volledig is: gate 13 strijkt haar
  weg en eist gelijke uitkomsten.
- Een document zonder `complete` geeft byte voor byte dezelfde uitkomsten, lezingen en eindstand als nu.

## Acceptance

Rood eerst per voorbeeld.

- **De wire**: schema, IR-SPEC (twee rijen en een alinea die naar ch. 1.5 verwijst), verifier met rondgang; gate 2
  houdt de vorm en de plaats in een zelftest; gate 3 groen.
- **ref**: `WV-1.5-complete-*` (de voorbeelden van de alinea "Complete input" met het veld in het document; het veld op
  een supertype geldt voor een object van een subtype; het veld op een lid dat een regel op het type schrijft
  verandert niets; het veld in een document zonder de eigenschap verandert niets) en `WV-1.5-stack-opgave` met de
  opgave als gedeclareerd volledig. `Eval.Members` en de twee `closed.json` bestaan niet meer; de getuigen
  `closed-input` en `input-above-computation` spelen hun cases ongewijzigd. De poot voor behoud en de poot voor
  monotonie van gate 5 groen, met hun floors nagerekend waar een telling verandert.
- **De frontend**: een JUnit-test per stuk (de marker op een attribuut en op een kenmerk, de opgave, elk van de twee
  weigeringen met de letterlijke zin, de marker op een tijdsafhankelijk lid), `--ir` van de acht goldens met een opgave
  en van de nieuwe golden byte-gelijk aan hun `ir.json`; de semantische tokens kennen het token zoals zij de
  opgave-marker kennen; gate 10 groen (de RGS-codes in het register en in beide catalogi).
- **Drools en clingo**: de acht goldens met een opgave spelen als voorheen (gate 6 en 12, statusregels gelijk); de
  nieuwe golden staat in de kanaallijsten met CLINGO-049 en DROOLS-370.
- **De golden**: de cases spelen in ref (gate 5); elke verwachting staat met haar berekening in het Result.
- Los groen: gate 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 15, 16, 17. `make gates` draait de hoofdsessie.

## May touch

- `spec/schema/uchar-ir-v1.schema.json`, `spec/IR-SPEC.md` (de twee rijen, één alinea), `spec/RUNTIME-SEMANTICS.md`
  ch. 1.5 (de alinea's "Complete input", de opgave, en wat ernaar verwijst), `spec/verifier/` (AST, lader, emitter,
  tests);
- `ref/` (bibliotheek, runner, tests, fixtures);
- `frontends/regelspraak/` (grammatica, resolver, emitter, catalogi, register, tests, `grammar/DIVERGENCES.md`);
- `backends/drools/` alleen de lader en zijn test; `backends/clingo/`, `frontends/catala/`, `frontends/flogic/` alleen
  waar een signatuur van de AST het vraagt;
- `corpus/golden/` (de acht `ir.json`, de nieuwe golden);
- een gate-script waar een zelftest, een floor of een kanaallijst het vraagt, in een eigen commit;
- deze kaart.

Mandaat voor de beschermde paden: de opdracht van de eigenaar van 2026-11-21 voor dit programma. De hoofdsessie leest
na en landt.

## Out of scope

De lezing in clingo en Drools (eigen kaarten); de inventaris `verifier --empty-closures` (noem in het Result of een
volledig lid daar een eigen soort hoort te zijn); de scenariolaag; tijdlijnen in een document met de eigenschap (het
veld mag op een lid met een tijdlijn staan; zo'n document wordt geweigerd met de slug `timeline-member`); de toolchain;
`spec/CONSTRUCTS.md` en `spec/PLAN.md` (stel de rij voor); `CLAUDE.md` (stel de tekst voor als de alinea van een gate
verandert).

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): de lezing met drie standen; sluiten per gegeven bestaat; voorbeelden uit de Jutterswet.

Van de hoofdsessie (voorlopig, ter bevestiging door de eigenaar): de schrijfwijze `(volledig)`; de naam `complete` op
de wire; een opgave is volledig door wat de frontend ervoor schrijft, niet door haar herkomst; ook `berekende X` draagt
het veld; `(volledig)` naast `(mag opgegeven worden)` en op een lid dat het eigen type schrijft is een fout; de
sessie-instelling per lid vervalt.

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, schade, vindloon, bergloon). In niets dat in de
repository komt staat een naam, regel of casus van een regelset buiten deze repository.

## Result

### Wat er is

| commit | inhoud |
|---|---|
| `aba2e1b76` | De wire: `complete` op `Attribute` en `Characteristic` in het schema (`{"const": true}`, na `timeline`, vóór `origin`), IR-SPEC ch. 2.2 (twee rijen, één alinea naar ch. 1.5), de OCaml-AST (`object_type.attribute_complete`, `.characteristic_complete`, per soort lid omdat een attribuut en een kenmerk een naam mogen delen; `effective_object_type` sluit ze over de keten), lader, schrijver, catala/flogic (`[]`), de Java-lader (laat het veld toe, leest het in `ObjectType.completeAttributes`/`completeCharacteristics`, weigert elke andere waarde in de zin van de OCaml-lader) |
| `8df540516` | Gate 2: zeven vormrijen en vier volgorderijen |
| `d9d5b5b25` | ref: `Eval.open_inputs` leest het veld; `type closed`, `Eval.Members` en de twee `closed.json` weg; `Eval.evaluate ?closed_inputs:bool` (runner `--closed-inputs`); elke test die een lid sloot zegt het met het veld (`ov_complete`) |
| `f5351689f` | RUNTIME-SEMANTICS ch. 1.5: "The three states", de opgave, "Complete input", "The two requirements" |
| `290df9852` | De frontend: token `VOLLEDIG` `'(volledig)'`, grammatica, resolver, RGS-463 en RGS-464 in register en beide catalogi, de opgave schrijft het veld op `opgegeven X` en `berekende X`, `CompleteMarkerTest`, `InputMarkingTest` (zie onder), DIVERGENCES "Volledig gegeven" met de meting en §7; de acht `ir.json` met een opgave opnieuw geëmitteerd |
| `85b954fe3` | De golden `jutterswet-volledig-gegeven` (zes cases, NOTE.md) en de byte-gelijke test ervan |
| `eda057bad`, `d1d36fc78` | Gate 6 en gate 12: de golden in de kanaallijsten (CLINGO-049, EXCLUDE DROOLS-370) |
| `539116d1f` | Gate 5: `EXPECTED_MIN_MONOTONE_CLOSURES` 443 → 401 (hieronder) |

### Per eis van Acceptance

- **De wire.** Getuigen: de gate-2-rijen (`Attribute.complete`/`Characteristic.complete` = true geldig; false, null,
  tekst, getal geweigerd; na de tijdlijn in volgorde, ervoor niet); `WV-2.2-complete` in de ref-suite (de gedeelde
  lader leest true voor verifier en executor, weigert false/null/tekst/getal op `…/complete` en een verschreven veld,
  de verifier vindt niets, de schrijver geeft het document byte voor byte terug); `CompleteLoaderWitnessTest` (Java).
  Rood: de nieuwe gate-2-rijen op het schema van vóór het veld, losse worktree op `e6f10ba40`: "FAIL: shape self-test,
  Attribute.complete = true: schema refuses it" en hetzelfde voor Characteristic, 2 rijen. Gate 2 en 3 groen.
- **ref.** `WV-1.5-complete-*`: member, other-member, member-given, every-input, session, characteristic (ook named),
  on-the-supertype, written (drie standen, uitkomsten gelijk aan zonder het veld), writer-on-a-subtype,
  without-the-property; `WV-1.5-stack-opgave` met de rijen `complete` (20) en `complete-given` (30). Rood op `eval.ml`
  van vóór de wijziging, met de nieuwe tests erin: 9 failures, "WV-1.5-complete-member (vindloon) (got open, want leeg)",
  … , "WV-1.5-complete-writer-on-a-subtype (Vondst)", "WV-1.5-stack-opgave-complete (got open, want 20)". `Eval.Members`
  en beide `closed.json` bestaan niet meer. `closed-input` speelt zijn vier cases ongewijzigd met `complete` op schade
  en beschadigd in zijn `ir.json`; `input-above-computation` speelt zijn cases ongewijzigd, zes in de map zelf (de
  opgave als volledig gedeclareerd, zoals de frontend haar schrijft) en de case `opgave-open` in de nieuwe map
  `input-above-computation-open` (zie de besluiten). De suite: 4082 voorbeelden. Gate 5 groen; de poot voor behoud
  ongewijzigd (280 goldens, 784 cases), de poot voor monotonie met de floor van de sluitingen herrekend (hieronder).
- **De frontend.** `CompleteMarkerTest`, 14 tests: de marker op een attribuut en op een kenmerk, na een tijdlijn, na een
  afwezigheidsmarkering, de twee leden van de opgave (en geen veld waar de opgave terugtrekt), de golden byte voor
  byte, RGS-463 in beide volgordes met de letterlijke zin in NL en EN, RGS-464 voor een regel (NL en EN), een
  kenmerktoekenning en een defaults-groep, een opgave die een extensie toevoegt, een schrijver op een subtype alleen,
  het token, de semantische tokens. Rood op de frontend van vóór (`e6f10ba40`): 12 van de 13 toen bestaande tests,
  elk met de syntaxfout op `(volledig)`. `--ir` van de acht goldens met een opgave en van de nieuwe golden byte-gelijk
  (gate 10). Gate 10 groen.
- **Drools en clingo.** De acht goldens met een opgave spelen als voorheen: gate 6 (1169 cases ref = clingo), gate 12
  (regel hieronder). De nieuwe golden staat in beide kanaallijsten.
- **De golden.** Speelt in ref (gate 5); de berekening per case staat hieronder.
- **Gewijzigde bestaande tests**, omdat de kaart het gedrag verandert dat zij vastleggen: `InputMarkingTest` (twee
  verwachtingen van de wire-tekst van `opgegeven X`/`berekende X` krijgen `"complete": true`); in de ref-suite
  `WV-5.12-open-stack (the initialisation closes)` leest de open opgave nu uit `input-above-computation-open`, en elke
  test die een lid sloot (`filled-test`, `filled-guard`, `read-for-another`, `past-undetermined-filter`, de
  stack-tests, `creation-member`, `fact-creation`, de getuigenlezer, monotone en preserved) zegt het met het veld; de
  scenarionamen `closed-*` heten `complete-*`, en het register van `WV-1.5-monotone` noemt ze zo, met één nieuwe rij
  (`stack-opgave-complete-given` zonder opgave: bergloon 20, opgave leeg).

### De golden `jutterswet-volledig-gegeven`, per case

Een Vondst met `toeslag` (volledig) en `bijslag`, `drijfhout` (volledig) en `wrakhout`; uitbetaling = taxatie plus
toeslag, bijbetaling = taxatie plus bijslag, drijftoeslag = 2 indien niet drijfhout, wraktoeslag = 2 indien niet
wrakhout, bergloon (mag opgegeven worden) = gewicht maal 2.

1. **case1** (taxatie 50, bijslag 5): de toeslag is een invoerlid, als volledig gedeclareerd, zonder waarde: vaststaand
   leeg. Wat de uitbetaling leest staat dus vast, en een vaststaand leeg operand telt in een plus als 0 (ch. 1.5 "What
   stands reads as before", ch. 1.2): uitbetaling 50. Bijbetaling 50 + 5 = 55. Consistent.
2. **case2** (taxatie 50, toeslag 5): de bijslag is een invoerlid zonder de markering en zonder waarde: open. De
   bijbetaling leest hem en heeft geen waarde (ch. 1.5 "An open read"): open. Uitbetaling 55. Consistent.
3. **case3**, V1 noemt niets: drijfhout is volledig en niet genoemd, dus staat vast dat het niet geldt; "niet drijfhout"
   is vastgesteld waar, de regel beslist: drijftoeslag 2. Wrakhout heeft de markering niet: open; "niet wrakhout" is
   onbepaald, de regel beslist niets: wraktoeslag open. V2 noemt beide: ze gelden, "niet …" is vastgesteld onwaar voor
   een andere vorm, dus staat vast dat de regels niets vullen: beide toeslagen vaststaand leeg. Consistent.
4. **case4** (gewicht 10, opgave 30): de opgave-default beslist met 30, de hoogste die beslist: bergloon 30. De
   berekening schrijft berekende bergloon 20. Opgave en berekening zijn beide gevuld en verschillen, dus geldt
   afwijkende opgave bergloon en meldt de consistentie haar feit (EF-025, `controle opgave bergloon`).
5. **case5** (gewicht 10): opgegeven bergloon is als volledig gedeclareerd en zonder waarde: vaststaand leeg. De
   opgave-default geldt dan met een vaststaand lege waarde en wijkt (ch. 1.5 "A stack"); de berekening beslist 20:
   bergloon 20, berekende bergloon 20. Het eerste lid van de afwijkingsvoorwaarde, "zijn opgegeven bergloon is gevuld",
   is vastgesteld onwaar, dus afwijkende opgave bergloon staat vast niet. Consistent.
6. **case6** (niets): de opgave vaststaand leeg en wijkt, zoals in case5; het gewicht is open, dus berekende bergloon
   open, en de berekeningsdefault onder de geweken opgave is open: bergloon open. De afwijking staat vast niet (eerste
   lid vastgesteld onwaar). Consistent.

Gemeten zonder het veld (dezelfde `ir.json` met `complete` weggehaald): cases 1, 3, 5 en 6 wijken af (toeslag,
drijfhout en opgave open, met uitbetaling, drijftoeslag, bergloon van case5 en de afwijking van case5 en 6); cases 2 en
4 lezen geen volledig lid zonder waarde.

### Voorlopige besluiten

1. De AST draagt twee lijsten op het objecttype, naar het patroon van de facettabellen, niet een veld per lid: zo blijft
   de signatuur van de catala- en flogic-frontends één regel. De lijsten zijn per soort lid.
2. De Java-lader leest het veld in het model (`completeAttributes`, `completeCharacteristics`, dezelfde verzamelingen die
   de frontend vult), zodat hij de inverse van de emitter blijft; de bouw doet er niets mee. Hij leest het vóór de
   sleutelset van het lid, zodat ook `null` de zin van de OCaml-lader krijgt.
3. De sessie-instelling is `Eval.evaluate ?closed_inputs:bool`; het testscenario draagt geen instelling meer.
4. De getuige `input-above-computation` is gesplitst. Met het veld in het document is een opgave zonder waarde
   vaststaand leeg; de case waarin zij open is (de initialisatie sluit erover, 25) kan daar niet meer staan. Zij staat
   ongewijzigd, als `case1-opgave-open.json`, in `input-above-computation-open` met het `ir.json` zonder het veld: een
   wire-document dat de frontend niet meer schrijft, en de getuige van ch. 1.5 "an initialisation closes over an open
   opgave". Verschil met de kaart ("spelen hun cases ongewijzigd"): de cases zijn ongewijzigd, één staat in een andere
   map.
5. De grammatica: op een attribuut staat `(volledig)` na de afwezigheidsmarkering en vóór de opgave-markering, en wordt
   ook ná de opgave-markering toegelaten (`volledigNaOpgave`) zodat RGS-463 in beide volgordes spreekt; op een kenmerk na
   de tijdlijn. `(volledig) (of onbekend)` is een syntaxfout zonder eigen zin.
6. Een extensie herhaalt een lid woord voor woord en `(volledig)` telt als woord: een herhaling die het toevoegt of
   weglaat is RGS-009. Een extensie die de opgave toevoegt aan een lid met `(volledig)` zet de twee markeringen in twee
   declaraties; RGS-464 noemt dan de gegenereerde groep als schrijver.
7. RGS-464 telt als schrijver wat `Eval.open_inputs` telt: een regel (elke soort, een initialisatie en een rij van een
   beslistabel inbegrepen; een kenmerktoekenning voor een kenmerk) of een defaults-groep met het declarerende type
   zelf als subject, na de defaults- en de opgaveverlaging. Een verdeling en een toekenning van een objectcreatie tellen
   niet: ch. 1.5 leest een lid dat een creatie toekent als invoer op een object van de case, en een document met de
   eigenschap en een verdeling wordt geweigerd (`allocation`).
8. De regel van RGS-463 is die van het token `(volledig)`, die van RGS-464 die van de eerste declaratie van het lid. De
   meldingen verwijzen naar de sectie als "(dialect, Volledig gegeven)", naar het patroon "(dialect §7)".
9. De golden schrijft zijn cases als JSON met de hand, geen scenariodocument: de scenariolaag kent `attributeOpen` en
   de andere standen niet als verwachting, en valt buiten de kaart.
10. De floor `EXPECTED_MIN_MONOTONE_CLOSURES` van gate 5 zakt van 443 naar 401. Van de 42 runs die geen sluiting meer
    tellen, laten 27 de opgave van een golden met een opgave weg (nu vaststaand leeg: geen invoerlid in de telling) en
    15 een ander invoerlid in een case zonder opgave (de initialisatie sluit niet meer over een open opgave; de run
    wordt vergeleken en houdt zijn uitkomsten). Gemeten vóór: 790 cases, 2255 runs vergeleken, 443 sluitingen, 71 geen
    invoerlid; na: 796, 2276, 401, 101 (de nieuwe golden: 6 cases, 9 runs).

### Voorgestelde rij voor `spec/CONSTRUCTS.md`

| 49 | A member whose input is complete — `complete` on an Attribute or a Characteristic, `(volledig)` in RegelSpraak | the owner's decision of 2026-11-21 that a rule set can close an input per member | core | neither: a field of a declaration, no node | v0 has none. Optional field `complete` on `Attribute` and `Characteristic`, after `timeline` and before `origin`, written `true` or not at all (schema `{"const": true}`); another value is a load error at `…/complete` in every reader; no WF rule. A subtype inherits the member with the field. RUNTIME-SEMANTICS ch. 1.5: in a document with `inferFromEmpty: false` a member declared complete is settled leeg where it is input and the case gives it no value, a characteristic stands not to hold where the case does not name it; beside a writer on the object's type it changes nothing; without the property it changes no outcome. ref reads it in `open_inputs`; the session setting "every input member complete" stays only for the preservation leg of gate 5 (`runner --closed-inputs`). The RegelSpraak frontend writes it for `(volledig)` on a member (a dialect extension; RGS-463 beside `(mag opgegeven worden)`, RGS-464 on a member a rule or a defaults group writes on its own type) and always on `opgegeven X` and `berekende X` of an opgave. clingo and the Drools build refuse a document with the property (CLINGO-049, DROOLS-370); the Drools loader reads the field into the model. | witnesses: the self-test rows of gate 2; WV-2.2-complete, WV-1.5-complete-*, WV-1.5-stack-opgave in the suite of gate 5; `CompleteLoaderWitnessTest`; `CompleteMarkerTest`; the golden `jutterswet-volledig-gegeven` with six cases; `ref/test/fixtures/strict-empty/closed-input`, `input-above-computation`, `input-above-computation-open` |

### Voorstel voor `CLAUDE.md` (gate 2)

In de alinea van gate 2: "A shape self-test holds `EmbeddedExpression.unit` to `true`, `DeclaredUnit.symbol` to a
non-empty string, `inferFromEmpty` to `false` on a root that admits no unknown field and `complete` to `true` on an
Attribute and a Characteristic; a key-order self-test holds `inferFromEmpty` directly after `format` and `complete`
after a member's `timeline`."

### Open punten

1. Gate 5, check 6 telt een run die een als volledig gedeclareerd lid weglaat bij "of no input member" (de afgeleide
   leden): de uitsluiting klopt (ch. 1.5 noemt volledige invoer als sluiting), het label niet. Voorstel: zulke runs apart
   tellen onder een eigen naam, uit de `complete`-lijsten van het document.
2. De inventaris `verifier --empty-closures` telt een plaats die een als volledig gedeclareerd lid leest zoals elke
   andere, terwijl de lezing van ch. 1.5 daar niets verandert (het lid is vaststaand leeg). Aanbeveling: geen eigen
   soort, maar zo'n plaats niet tellen, of een kolom die zegt dat elk leeg-kunnend lid volledig is; te meten op de acht
   goldens met een opgave (de lezingen van `opgegeven X` in de groep en de afwijking) en de nieuwe golden.
3. `spec/proposals/STRICT-EMPTY.md` (§7.4, de getuige `closed-input`) beschrijft nog de sessie die leden per naam
   sluit; buiten het mandaat van de kaart niet bijgewerkt.
4. `(volledig) (of onbekend)` op een attribuut is een kale syntaxfout; een eigen zin naar het patroon van RGS-461 is
   niet gebouwd.

### Samenvattingsregels van de losse gates

Alle op de tip van de branch, elk los gedraaid, de zware één voor één (logs in de werkmap).

- 1: `check-self-contained: PASS`
- 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 563 shape self-test row(s); 510 file(s) validate
  and round-trip, floor 192; 67110 object(s) in 1167 document(s) in the schema's key order, 2 in no subschema)`
- 3: `check-verifier: PASS (…; 510 golden clean; 657 negative rejected for their declared rule, floor 568; …;
  empty-closures inventory over 510 documents: 841 closure places, floor 827, 390 negation, floor 383, 206
  absent-instance, floor 206, …)`
- 4: `check-case-schema: PASS (schema valid; 146 self-test row(s); 1313 case file(s) validate)`
- 5: `check-oracle: PASS (ref suite 4082 worked examples, floor 489; 1211 cases replayed exactly, floor 427;
  preservation with complete knowledge: 280 goldens and 784 cases play, floors 280 and 784; …; 2 carrying the property
  already, played by check 3 (jutterswet-afleiden-uit-leeg, jutterswet-volledig-gegeven); …; monotonicity: 796 cases,
  2276 runs without one input compared, floor 2255, 401 with a closure apart, floor 401, 101 of no input member; 0
  listed gap(s) of ch. 1.5; …)`
- 6: `check-clingo: PASS (backend suite 3231 checks, floor 83; 1169 cases replay exactly through clingo — ref = clingo
  over the corpus; 12 golden(s) in the refusal channel, by name, 42 cases refused with the pinned codes; …)`
- 7: `check-noop: PASS (1169 cases refute a do-nothing executor; 42 pinned survivors pin defeasible standing)`
- 8: `check-red: PASS (102 red cases fault observably in ref and clingo, floor 44; …)`, Drools leg PASS
- 9: `check-catala: PASS — frontend suite green (262 checks, floor 201); 13 catala golden(s) regenerate byte-identically`
- 10: `check-rgs-codes: PASS — 414 codes, 447 sites, …`; `check-regelspraak: PASS — JUnit suite green (1718 tests, floor
  173); 223 rgs golden(s) re-emit byte-identically (floor 80)`
- 11: `check-toka-parity: PASS` (ref 29 PASS, clingo 29 PASS, Drools 29 PLAYED)
- 12: `check-drools: PASS` — played 384, auto-excluded 49 (2× DROOLS-370), DRL-text branch played 384, JUnit 1979
  tests, floor 1573
- 13: `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, floor 38; 242 executor replays
  reproduce the untouched outcome, …; 0 pinned survivors)`
- 14: `check-flogic: PASS — frontend suite green (164 checks, floor 81); 3 flogic golden(s) re-emit byte-identically`
- 15: `check-graph: PASS` (route leg: 223 goldens with a source.rgs)
- 16: `check-scenario: PASS — 48 scenario golden(s) (floor 7) regenerate 176 case file(s) (floor 31) and their ir.json
  byte-identically`
- 17: `check-readings: PASS (ref traced 1211 golden case files, …; on both branches 385 goldens and 1073 case files
  compared, 8468 readings, …; deviations 0, end-state deviations 0; …)`, 2 goldens excluded with DROOLS-370

**Landing (hoofdsessie, 2026-11-22).** Nagelezen: het veld op de wire (schema, IR-SPEC ch. 2.2, AST, lader, emitter, de
Java-lader), `Eval.open_inputs` met de ene sessie-instelling `closed_inputs`, de alinea's van ch. 1.5, de frontend
(het token, de resolver, RGS-463 en RGS-464 met hun zinnen in beide catalogi) en de zes cases van
`jutterswet-volledig-gegeven`, met de hand nagerekend. De floor `EXPECTED_MIN_MONOTONE_CLOSURES` ging van 443 naar 401
omdat de opgave van de goldens met een opgave-markering nu volledig is: de 27 runs die haar weglaten tellen als een lid
dat zonder zijn waarde niet open is, de 15 andere worden vergeleken en houden hun uitkomsten. Open punt 1 is hersteld
(het label en het commentaar van de poot in gate 5 noemen zo'n lid nu bij wat het is); de punten 2 tot 4 blijven open:
de inventaris `verifier --empty-closures` telt een plaats die een volledig lid leest zoals elke andere,
`spec/proposals/STRICT-EMPTY.md` beschrijft als verslag van de proef nog de sessie per lid, en `(volledig)` vóór een
afwezigheidsmarkering geeft een kale syntaxfout. Rij 49 van `spec/CONSTRUCTS.md` en de alinea's van gate 2 en 5 in
`CLAUDE.md` zijn gezet. `make gates` groen op `b399c479e` (661 s, 17 gates, red=0).

De schrijfwijze `(volledig)`, de naam `complete` en de regel dat `opgegeven X` en `berekende X` van een opgave het veld
altijd dragen, zijn voorlopige besluiten van de hoofdsessie, ter bevestiging door de eigenaar.
