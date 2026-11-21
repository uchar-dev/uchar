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
