# frontend-extensie-herdeclaratie: een Extensie die een bestaand lid opnieuw declareert, vervangt het stil

*Vondst van een regelsetbouwer, 2026-09-28, bevestigd door de hoofdsessie op
uchar `056df376` via de distributie; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/ext/` (alleen lezen).*

## Aanleiding

`Extensie van objecttype` voegt leden toe aan een elders gedeclareerd
objecttype. Declareert een extensie een attribuut dat het objecttype al
heeft, dan vervangt de tweede declaratie de eerste zonder melding, ook met
een ander datatype: `de waarde Numeriek (geheel getal)` gevolgd door een
extensie met `de waarde Tekst` geeft een IR met `waarde: Text` en
`diagnostics: []`. Een regelset die het fundament (één bestand) en zijn
uitbreidingen (andere bestanden) scheidt, merkt zo'n fout pas in een
uitvoerder. De gemeten grens die een gebruiker wél nodig heeft: dezelfde
declaratie nog eens, alleen om de opgave-markering `(mag opgegeven worden)`
(DIVERGENCES §7) op een bestaand attribuut te zetten vanuit de uitbreiding
(gemeten: de markering werkt dan, de lowering volgt).

## Minimale reproductie

`056df376`, distributie `bin/regelspraak ir`.

    R1  Objecttype de Vondst (mv: Vondsten)
            de waarde Numeriek (geheel getal);
        Extensie van objecttype de Vondst
            de waarde Tekst;
        nu: ir exit 0, diagnostics [], het attribuut 'waarde' is Text.
    R2  Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;
            de meldingsdatum Datum in dagen;
        Extensie van objecttype de Vondst
            de meldingsdatum Datum in dagen (mag opgegeven worden);
        Regel melding / geldig altijd /
        De meldingsdatum van een Vondst moet berekend worden als de vinddag van de Vondst plus 3 dagen.
        nu: check exit 0, de markering wordt gelowerd (opgave boven berekening meldingsdatum).

## Besluiten (voorstel; de maintainer beslist)

- E1: een extensie die een attribuut, kenmerk of rol herdeclareert dat het
  objecttype (of zijn erflijn) al draagt, is een ERROR op de regel van de
  herdeclaratie, met beide regels in de melding — behalve E2.
- E2: een herdeclaratie die letterlijk gelijk is aan de bestaande (zelfde
  datatype, eenheid, tijdlijn, dimensies) en alleen `(mag opgegeven
  worden)` toevoegt, is toegelaten en markeert het bestaande attribuut; de
  gegenereerde leden staan waar §7 ze zet. Dat is de vorm waarmee een
  uitbreiding een fundamentattribuut opgeefbaar maakt zonder het fundament
  te wijzigen. DIVERGENCES §7 en de Extensie-tekst noemen het.
- E3 (bijvangst, alleen als het klein is): de WARNING van Tabel 16 over een
  ontkend kenmerk met het verkeerde werkwoord staat op `line 0`; geef haar
  de regel van de zin.

## Acceptatie

- (a) R1 geeft een ERROR die beide declaraties noemt; R2 blijft exit 0 met
  dezelfde lowering; JUnit-getuigen rood-eerst voor E1 (attribuut, kenmerk,
  rol; ander datatype; zelfde datatype zonder markering) en E2.
- (b) Inertie met de methode van DIVERGENCES §6 over corpus en
  pariteitszone: geen bron verandert van IR of exitcode (verwacht: geen
  enkele bron herdeclareert in een extensie; tel het).
- (c) DIVERGENCES: de regel bij de Extensie-tekst en één zin in §7; gate 10
  en alle overige gates groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/<nieuwe golden>/` als
een getuige in het corpus nodig is, kaartverplaatsing. Niet: `backends/`,
`ref/`, `spec/`, `scripts/`.

## Result (2026-09-28, bouwer)

Tak `card/frontend-extensie-herdeclaratie`, basis `396097de`. De kaart blijft
in `doing/`. Alle uitvoer hieronder komt van het frontend van de werkboom
(`Resolver --ir`); de distributie-CLI draait op de oude pin.

### Wat landde

- `DefinitionResolver`: elke liddeclaratie (kop of extensie) wordt in
  documentvolgorde vastgelegd. Declareert een extensie een attribuut of
  kenmerk dat het objecttype al draagt (in de kop, of in een eerdere extensie
  als geen kop het declareert), dan blijft de bestaande declaratie in het
  model en valt het oordeel pas als alle declaraties gelezen zijn (de kop mag
  na de extensie komen). Toegelaten: dezelfde woorden (de afsluitende `;` en
  de opgave-markering buiten beschouwing), en daarbij één keer
  `(mag opgegeven worden)` erbij. Elke andere herhaling is een ERROR op de
  regel van de herhaling met beide bronregels en beide teksten. Een rolregel
  in een extensie (`de vinder Jutter;`, door de grammatica gelezen als
  attribuut met domein) waarvan de naam een rol is van een feittype waarin
  het type of een supertype een rol vervult, is een ERROR met de regel van
  de rol; het lid gaat uit het model, zodat 'ongedefinieerd domein' op regel
  0 er niet naast komt. De WF-105-zin (langs de erflijn) noemt nu beide
  bronregels.
- `Benamingen` leest de benamingen van een herhaling niet nog eens;
  `RuleChecker.opgaveDeclaraties` slaat een geweigerde herhaling over, zodat
  de eigen weigerzin alleen staat. `Resolver.bouw` geeft het oordeel door.
- E3: de WARNING van Tabel 16 in een voorwaarde staat op de regel van de
  controle; in een rij van een beslistabel (eigen parsestroom) op de regel
  van de tabel. De resultaatkant had al haar regel.
- DIVERGENCES: de regel als bullet bij de Extensie-tekst in §5, en één zin
  in §7 (de markering via een letterlijke herhaling in een extensie).

### Weigerzinnen, letterlijk

R1 (`/tmp/v2/probe/ext/bron3.rgs`), exit 1, geen IR geschreven:

    ERROR (line 5): objecttype 'Vondst': de extensie declareert het attribuut 'waarde' (bronregel 5), dat Vondst al draagt (bronregel 2), en 'de waarde Tekst' is niet letterlijk 'de waarde Numeriek (geheel getal)' — een extensie voegt leden toe en vervangt er geen: een lid dat het objecttype al draagt, herhaalt zij alleen letterlijk, en '(mag opgegeven worden)' komt er hooguit één keer bij (WF-002, dialect §7)

Kenmerk van een andere soort, exit 1:

    ERROR (line 6): objecttype 'Vondst': de extensie declareert het kenmerk 'waardevol' (bronregel 6), dat Vondst al draagt (bronregel 3), en 'het waardevol kenmerk (bezittelijk)' is niet letterlijk 'is waardevol kenmerk (bijvoeglijk)' — een extensie voegt leden toe en vervangt er geen: een lid dat het objecttype al draagt, herhaalt zij alleen letterlijk, en '(mag opgegeven worden)' komt er hooguit één keer bij (WF-002, dialect §7)

Rolregel in een extensie, exit 1 (vóór: `ERROR (line 0): ongedefinieerd
domein Jutter (gebruikt door 'vinder')`):

    ERROR (line 13): objecttype 'Vondst': de extensie declareert 'vinder' met het objecttype 'Jutter' (bronregel 13), en dat is de rol 'vinder' van feittype 'vondst van jutter' (bronregel 8) — een extensie voegt attributen en kenmerken toe en geen rollen: een rol staat in haar feittype

Tweede markering, exit 1 (vóór: de botsingszin van §7 met `(bronregel 0)`):

    ERROR (line 9): objecttype 'Vondst': de extensie declareert het attribuut 'meldingsdatum' (bronregel 9), dat Vondst al draagt (bronregel 3), en dat draagt '(mag opgegeven worden)' al (bronregel 6) — een extensie voegt leden toe en vervangt er geen: een lid dat het objecttype al draagt, herhaalt zij alleen letterlijk, en '(mag opgegeven worden)' komt er hooguit één keer bij (WF-002, dialect §7)

Extensie van een subtype op een geërfd attribuut, exit 1:

    ERROR (line 8): objecttype 'Wrak': attribuut 'waarde' (bronregel 8) is al gedeclareerd op 'Vondst' (bronregel 2) hogerop in de keten — WF-105: een subtype herdeclareert geen geërfde naam (één exemplaar zou twee declaraties van die naam dragen, en welke een lezer vindt bepaalt datatype, dimensies en tijdlijnfacet van elke lezing ervan)

R2 (`/tmp/v2/probe/ext/bron2.rgs`): exit 0, geen diagnose, IR byte-gelijk
vóór en na (`cmp`). Plek van de leden: attributen `vinddag, meldingsdatum,
opgegeven meldingsdatum, berekende meldingsdatum`, kenmerk `afwijkende
opgave meldingsdatum` achteraan, groep `opgave boven berekening
meldingsdatum`, regel `afwijking opgave meldingsdatum`, consistentie
`controle opgave meldingsdatum`. Tegen hetzelfde document met de markering
in de kop verschilt de IR op één regel: de bronregel van de consistentie
(6 tegen 3), zoals §7 die bron op de gemarkeerde declaratie zet.

E3, voorwaarden (vóór beide op `line 0`), exit 0:

    WARNING (line 9): het bijvoeglijk kenmerk 'waardevol' wordt ontkend met het werkwoord 'heeft', maar Tabel 16 vereist 'is' bij dit soort kenmerk (§8.1.8): hij geen waardevol heeft
    WARNING (line 14): het bezittelijk kenmerk 'drijfvermogen' wordt ontkend met het werkwoord 'is', maar Tabel 16 vereist 'heeft' bij dit soort kenmerk (§8.1.8): hij geen drijfvermogen is

### Getuigen (rood-eerst)

- `ExtensieHerdeclaratieTest`, 20 toetsen, elke zin heel en op zijn regel:
  ander datatype; vijf herhalingen met hetzelfde datatype en een ander
  verschil (tijdlijn, getalspecificatie, lidwoord, meervoud, tijdlijn plus
  markering); kenmerk van een andere soort; extensie vóór de kop; twee
  extensies; rolregel; attribuut met de naam van een rol (toegelaten);
  letterlijke herhaling van attribuut en kenmerk (toegelaten, IR gelijk aan
  het document zonder extensie); E2 (lowering gelijk aan de kop op de
  bronregel na); E2 met een benaming; tweede markering; subtype-extensie
  (drie vormen, WF-105 met beide regels). Op het frontend van `396097de`:
  16 rood, 4 groen. De vier groene pinnen wat blijft: letterlijke herhaling
  van een attribuut (met en zonder `;`), attribuut met de naam van een rol,
  E2 zonder benaming.
- `RegisterlintRegelTest`, 1 toets: onderwerp, genavigeerd onderwerp,
  element van een kwantor en rij van een beslistabel; op `396097de` rood,
  alle vier op regel 0; na de stap op 20, 25, 30 en 32.
- Suite 969 → 990 toetsen, groen.

### Inertie (methode §6)

Frontend van `396097de` tegen de kop van de tak, per eenheid exitcode,
geprinte diagnostiek, IR en casebestanden vergeleken. 175 eenheden: 115
`.rgs` van het corpus en 47 van `toka/` elk los, de scenarioroute van de 11
goldens met een `scenario.rgs` (44 casebestanden), en de casus van `toka/`
als keten, zoals gate 11 hem leest (45 fragmenten) en zoals `CasusTest` hem
leest (47). Exit 0: 136 vóór en 136 na; de 39 andere eenheden houden hun
exitcode. Verschillen: 3, alleen de regelnummers van `Resolver.java` in de
stacktrace van de emitterweigering van `toka-32`, `toka-33` en `toka-36`
(212 → 213, 309 → 310, door de ene regel die in `Resolver.bouw` bijkwam);
na normalisatie daarvan 0. Geen eenheid draagt de Tabel 16-waarschuwing in
een voorwaarde of een WF-105-fout, dus E3 en de nieuwe WF-105-zin
veranderen niets.

Telling van herhalingen in een extensie (met een meetklasse buiten de repo,
in het pakket van de resolver, die de oordelen van `DefinitionResolver`
telt): van de 162
losse bronnen declareert er één een extensie (`toka-04`, drie leden),
zonder herhaling (los heeft ze geen kop). In beide casusketens: 3
herhalingen, alle drie toegelaten: `toka-04` herhaalt drie attributen van
de kop in `toka-01` woord voor woord, zonder markering. Het corpus
declareert geen enkele extensie. De verwachting van de kaart (nul) klopt
dus voor elke losse bron en niet voor de pariteitscasus.

### Afwijkingen van de kaart

1. De letterlijke herhaling zonder markering is toegelaten, zonder
   diagnose; de kaart noemt 'zelfde datatype zonder markering' een ERROR.
   Meting: de pariteitscasus herhaalt drie attributen zo (hierboven). Een
   strenge variant van het frontend (een kopie buiten de repo waarin de
   letterlijke herhaling een ERROR is) geeft op de keten van gate 11 drie
   ERRORs, exit 1 en geen casusdump, en `CasusTest` faalt erop ("false
   positives op de TOKA casus"). Gevolgtrekking, niet gedraaid: een WARNING
   komt in de diagnostieklijst van de vastgepinde casusdump (die draagt er
   nu vijf), dus die dump is dan niet meer byte-gelijk. `toka/` en
   `scripts/` vallen buiten May touch. De herhaling verandert niets aan het
   model. Hetzelfde datatype met elk ander verschil blijft een ERROR (vijf
   getuigen).
2. 'Rol': een extensie kent geen rolproductie (`objectTypeMember` is
   koptekst, kenmerk of attribuut). De enige schrijfwijze is een rolregel,
   die de grammatica als attribuut met domein leest; die weigert nu met een
   zin die de regel van de rolregel en de regel van de rol noemt. Een
   attribuut met de naam van een rol en een eigen datatype blijft
   toegelaten: rollen en leden zijn gescheiden naamruimten
   (IR-SPEC ch. 2.6, "Roles and object-type members are separate
   namespaces"), in de kop net zo.
3. Langs de erflijn blijft WF-105 de weigering, ook voor de letterlijke
   herhaling en voor de markering (die zou het attribuut van het supertype
   voor elk exemplaar veranderen, en de opgave componeert niet langs de
   keten); E2 geldt daar niet. De WF-105-zin noemt nu beide bronregels, ook
   voor een herhaling in een kop: één regel, één zin.
4. Voor E2 zoals de kaart hem stelt waren twee aanpassingen nodig: vóór deze
   stap weigerde E2 met een benaming op het attribuut als WF-117 ("draagt
   twee benamingen ... 'datum van melding' en 'datum van melding'"), en gaf
   een tweede markering de botsingszin van §7 met `(bronregel 0)`.

### Gates (volgorde van `gates.yml`, `eval $(opam env)`, bash waar de workflow bash gebruikt)

    check-self-contained: PASS
    check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 316 file(s) validate and round-trip, floor 192; 41430 object(s) in 817 document(s) in the schema's key order, 1 in no subschema)
    check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 785 case file(s) validate)
    check-verifier: PASS (316 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
    check-oracle: PASS (ref suite 1152 worked examples, floor 489; 745 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
    test_ref: all 1152 worked examples pass
    check-clingo: PASS (backend suite 1988 checks, floor 83; 745 cases replay exactly through clingo — ref = clingo over the corpus)
    check-noop: PASS (701 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
    check-rood: Drools leg PASS (12 fault-pinned, 28 not-carried with reason)
    check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
    check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    check-regelspraak: PASS — JUnit suite green (990 tests, floor 173); 104 rgs golden(s) re-emit byte-identically (floor 80)
    [mvn package backends/drools: Tests run: 862, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS]
    check-toka-parity: PASS
      re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (holdouts: toka-23-regel-bevestigingstijdstip-vlucht.rgs toka-45-regel-belasting-op-basis-van-afstand.rgs); TOKA suite 27 tests, floor 27
    check-drools: PASS
      played 219  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
    check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    check-graaf: route leg — the graph of the wire equals the graph of the source on all 104 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)
    check-graaf: PASS
    check-scenario: PASS — 11 scenario golden(s) (floor 7) regenerate 44 case file(s) (floor 31) and their ir.json byte-identically

Gate 9 draaide in volle omvang (catala in de opam-switch). Geen vloer
veranderd; geen CONSTRUCTS- of PLAN-tekst voorgesteld, de wire verandert
niet.

### Open punten

1. Een lid dat één kop twee keer declareert, of twee koppen van hetzelfde
   type, vervangt nog steeds stil: `de waarde Numeriek (geheel getal);` en
   `de waarde Tekst;` in één kop geven exit 0, geen diagnose, `waarde` is
   Text. WF-002 noemt dat onwelgevormd; de kaart ging over extensies.
2. In een keten van bestanden (`--ir a.rgs b.rgs`) zijn de bronregels in
   de zin ketenregels, zoals elke regel in die modus; `--cases` rekent
   alleen de regel van de diagnose om naar bestand en regel, niet de
   `(bronregel N)` in de tekst. Dat geldt ook voor de zinnen van §7.
3. Bijvangst, niet gebouwd: in een kwantorvoorwaarde wordt `alle vondsten
   van de Jutter geen drijfvermogen hebben` (bezittelijk kenmerk, goed
   werkwoord) gelowerd als de positieve controle (`negated: false`), gelijk
   aan `... drijfvermogen hebben`; gemeten op `396097de`, exit 0 zonder
   diagnose. `niet waardevol zijn` lowert goed (`negated: true`). Een stil
   verkeerde lezing die een eigen kaart vraagt.
4. Een rolregel in een extensie waarvan de naam geen rol van het type is,
   houdt de bestaande ERROR 'ongedefinieerd domein ...' op regel 0
   (`RuleChecker.meldOnbekendDomein` geeft altijd regel 0).
5. De distributie ziet dit pas na een pinverschuiving.

### Commits

- `e03d1563` claim kaart frontend-extensie-herdeclaratie
- `54439690` ExtensieHerdeclaratieTest: een extensie die een lid herhaalt, rood
- `7a928ae6` Resolver: een extensie herhaalt een lid alleen letterlijk
- `bd9dc0d8` RegisterlintRegelTest: de Tabel 16-waarschuwing in een voorwaarde, rood
- `b8f2d609` RuleChecker: de Tabel 16-waarschuwing staat op de regel van de zin
- `c5259acd` DIVERGENCES: een extensie herhaalt een lid alleen letterlijk
- de commit van dit Result
