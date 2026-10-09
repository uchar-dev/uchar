# tms-expressievormen-en-rekendatum: de truth-maintenance-tak draagt de expressie- en voorwaardevormen, de rekendatum en elke schrijversvorm

*Vastgelegd door de hoofdsessie op 2026-10-08, na het besluit van de eigenaar
op het `Result:` van `20261007-drools-logische-afleiding` (PLAN §9,
2026-10-08). De tweede van vier bouwkaarten, na
`20261008-tms-aggregaties-creatie-en-fouten`; geen haalbaarheidsproef.
Tellingen uit de corpusdekking van de proeftak in dat `Result:` (bij (e)): de
eerste weigering per golden, dus een golden kan daarna nog op een volgende
vorm stuiten.*

## Aanleiding

Na de aggregaties zijn de expressie- en voorwaardevormen de grootste groep
weigeringen van de tak: `Let` (15, waarvan 1 in een default,
`jutterswet-bergingstoeslag`), afronding (12), `QuantifierFold` (12),
`BuiltinCall` (10), de procentvormen (5), uniciteit (5, waarvan 1 over meer
bronnen), `RoleMembership` (4), `Clamp` (3), een additieve operandgrens over
eenheden die herrekend moeten worden (3) en `power` (1). Het `Result:` noemt
ze grotendeels bekend Drools-gebied: functies over gebonden waarden, met de
rekenhulpen van `runtime/RegelSpraakRuntime` die er al zijn. Een kwantorvouw en
uniciteit lezen een verzameling en bouwen op de `accumulate` van de eerste
kaart.

Daarnaast weigert de tak twee groepen die over de schrijvers van een doel
gaan, en die de exec-modeltak wel speelt:

- twee of meer rekenende regels op één doel (9). De tak weigert ze met "hun
  v0-betekenis is de volgorde op de agenda (RUNTIME-SEMANTICS ch. 7.7,
  WF-079)". Schrijvers met elkaar uitsluitende voorwaarden of met dezelfde
  waarde spelen in beide andere takken; verschillende waarden aan het fixpunt
  zijn een fout (ch. 5.7, `backends/drools/README.md`, "Recorded: two writers
  of one target");
- de tak sleutelt schrijvers op het declarerende type en weigert daardoor ook
  paren op zustersubtypes die WF-079 niet verbiedt (waarneming 6; onder meer
  `jutterswet-diepwrakbasis` en `jutterswet-opgave-vormen`). WF-079 en
  ch. 7.10 lezen per concreet type;
- een rekenende regel op een doel buiten het staande domein, waar een
  berekende leeg het doel leeg maakt (ch. 7.8). Geen feit van de tak zegt nu
  "schrijft leeg" (open punt 2 van de proefkaart).

En de rekendatum, nodig voor een versie met een begrensde geldigheid (2), een
`TimeWindow` (1) en een `ContextValue` (`evaluationDate`, `evaluationYear`;
1); ook een defaults-groep met een gedateerde geldigheid weigert de tak nu.

Goal: de tak draagt deze vormen; de goldens die hij erop weigerde, spelen of
weigeren op een vorm van `20261008-tms-verdeling-en-tijdlijnen`.

## Wat de tak erbij krijgt

1. **De expressievormen**: `Let` met `VariableReference` en `BinderRef`,
   afronding, `BuiltinCall`, `Clamp`, de procentvormen, `power`, het herrekenen
   van eenheden in een operandgrens, en elke andere expressievorm die
   `TmsTranspiler` nu met `NOG_NIET` of `BUITEN` weigert, behalve die van de
   twee andere bouwkaarten.
2. **De voorwaardevormen**: `QuantifierFold`, `RoleMembership` (ook de verre),
   uniciteit, ook over meer bronnen, met de schending op het kanaal
   (ch. 5.6, 5.9).
3. **De schrijvers**: twee of meer rekenende regels op één doel, met de fout
   van ch. 5.7 als logisch feit (de eerste kaart) wanneer ze aan het fixpunt
   verschillen, met de schrijvers in documentvolgorde; schrijvers gesleuteld
   per concreet type (ch. 7.10); een feit voor "schrijft leeg" buiten het
   staande domein, op de grens van `transpiler/kern/StaandeDoelen`, die alle
   takken delen.
4. **De rekendatum** als gebonden waarde: versies met een begrensde
   geldigheid, defaults-groepen met een gedateerde geldigheid, `TimeWindow` en
   `ContextValue`.

5. **De leeslaag** (aanbeveling van het advies in het `Result:` van
   `20261008-tms-aggregaties-creatie-en-fouten`, vraag 2; de bouwer beslist).
   `TmsTranspiler` mengt analyse en PatternDSL, en de methoden die deze kaart
   raakt, raken dat ook. Het advies: (1) splits `Lezingen` in een leestabel die
   niet aan een weergave vastzit (soort, sleutel, index, plaats in de LHS, en de
   ontdubbeling per sleutel) en de PatternDSL-uitwerking ervan; (2) vervang de
   losse structurele lambda's door een kleine gesloten woordenschat (alfa op een
   lidsleutel, join op het onderwerp, join op een bereik, de vensters van de
   stuurfeiten, de nultoets op invoer, de klassetoets) met één mapper naar
   PatternDSL, zodat gelijke constraints een gelijk id krijgen en Drools knopen
   kan delen (dat raakt de schaalproef van `20261008-tms-omschakeling`); (3)
   bewaar bij elke gecompileerde `Toets` en `Waardefunctie` de IR-knoop, zodat
   `20261008-drl-teksttak-op-truth-maintenance` voorwaarden uit de IR kan
   weergeven. Doet de bouwer het niet, dan zegt het `Result:` waarom.
6. **De fouten van de datumrekening als logisch feit** (bijvangst van
   `20261009-drools-datumstap-buiten-bereik`). Sinds die kaart geeft de runtime
   een fout voor een datumstap buiten het bereik en voor een aantal buiten het
   bereik; in deze tak verlaten die fouten de fire als exception, terwijl alleen
   een deling door nul een `Fout`-feit is dat pas aan het fixpunt telt. Een
   aantal dat alleen op een tussenstand buiten het bereik ligt, kan hier dus een
   fout geven waar ref een waarde geeft (niet gemeten). Het commentaar bij
   `TmsTranspiler.fout` ("division by zero is the only evaluation fault") klopt
   dan niet meer.

## Acceptatie

- (a) Rood-eerst getuigen, elk in elke volgorde van de betrokken regels in het
  model:
  - per expressievorm één getuige met een operand die later afgeleid wordt;
  - een kwantorvouw over leden waarvan een afgeleid kenmerk later verandert;
  - een uniciteitsschending die bij een tussenstand ontstaat en weer verdwijnt;
  - twee schrijvers met elkaar uitsluitende voorwaarden, met dezelfde waarde, en
    met verschillende waarden (de fout van ch. 5.7);
  - twee schrijvers op zustersubtypes;
  - een berekende leeg binnen en buiten het staande domein;
  - een versie aan beide kanten van de rekendatum.
- (b) De corpusgetuige heeft geen FAIL; de vloer gaat naar het gemeten aantal;
  het `Result:` geeft de nieuwe telling per familie.
- (c) De grep van de eerste kaart over `transpiler/tms/` geeft niets.
- (d) De exec-modeltak en de DRL-teksttak zijn ongewijzigd: gate 12 groen met
  beide pins gelijk; alle gates van `CLAUDE.md` groen.

## Raakvlakken

- Bouwt op de `accumulate` en de fouten als feit van
  `20261008-tms-aggregaties-creatie-en-fouten`.
- `20260930-frontend-kwantor-over-een-pad` raakt de kwantorvouw in de frontend,
  niet in de backend. Zijn nieuwe golden gaat wel door de corpusgetuige van de
  tak, en die laat geen FAIL toe; landt die kaart na deze, dan moet de tak de
  golden spelen of met naam weigeren.

## Review

Zoals in de eerste bouwkaart: een lens na elke bouwstap, een eindlens over de
hele diff, na elke stap een doorloop van het hele corpus, en een lensvondst
alleen met een concreet faalscenario op echte invoer of een getuige.

## Omvang

Middelgroot tot groot: veel vormen, maar de meeste zijn een functie over
gebonden waarden. Het meeste denkwerk zit in punt 3.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`,
`cases/CaseRunner.java` en `cases/DroolsBackend.java` voor zover de rekendatum
of het terugzetten dat vraagt, tests onder `backends/drools/src/test/java/`, de
sectie over de tak in `backends/drools/README.md`, kaartverplaatsing. NB:
serieel met andere Drools-kaarten.

Out of scope: de exec-modeltak en de DRL-teksttak, `transpiler/kern/` behalve
een nieuwe vraag aan `StaandeDoelen` zonder gedragswijziging voor de andere
takken, `scripts/`, `corpus/`, `spec/`, de toolchain, en de vormen van de andere
bouwkaarten.

## Result (2026-10-09)

*Branch `card/tms-expressievormen-en-rekendatum`, claim `1a9ac740` op main `5e3d3607`, tip `e6eefdce`. Gemeten met
een jar uit een bevroren kopie van elke stapcommit, door de drie Drools-takken over alle 294 goldens met cases
(`/tmp/lens-tms-k2/stapklaar.sh`).*

De tak met truth maintenance, die volgens het besluit van 2026-10-08 de destructieve exec-modeltak vervangt, draagt na
deze kaart de expressie- en voorwaardevormen, de schrijvers en de rekendatum. Hij speelt **196 van de 242 goldens** die
de exec-modeltak speelt (vóór de kaart 121), en 201 van de 294 in totaal (vóór de kaart 122), zonder FAIL of ERROR. De
46 goldens die de exec-modeltak speelt en hij niet, weigert hij alle als eerste op een vorm van
`20261008-tms-verdeling-en-tijdlijnen`. De exec-modeltak en de DRL-teksttak zijn na elke stap byte-gelijk gebleven aan
de nulmeting.

| Commit | Inhoud |
|---|---|
| `dd294cb3` | stap 1: de waardevormen, de rekendatum en de datumfouten als feit |
| `23975b2e` | stap 1: vloer van de corpusgetuige 120 → 171 |
| `771491eb` | stap 1: een wortel zonder afronding exact, zoals ref |
| `cf71341d` | stap 1: README |
| `a3842004` | stap 1: getuige voor de terugvalwaarde van een som in een andere eenheid |
| `ba8127f2` | lensfix stap 1: deponeren waar ref deponeert; een mislukte creatie maakt niets |
| `f9929a07` | lensfix stap 1: een filter van een vouw deponeert onder de vorm die haar leest |
| `deeaaa26` | lensfix stap 1: README, waar de tak deponeert |
| `695bf30a` | stap 2: de kwantorvouw |
| `dde9507d` | stap 2: de rolcontrole |
| `14d3bbb0` | stap 2: uniciteit, ook over meer bronnen |
| `d35a09c2` | stap 2: "is gevuurd" en een unie op een waardeplek weigeren met hun reden |
| `40741a76` | stap 2: de kwantorvouw ook in de volgorde van de gegenereerde regels |
| `f7bb0dd4` | stap 2: README en vloer 171 → 190 |
| `3e6b0498` | stap 3: twee of meer schrijvers van één doel, en leeg buiten het staande domein |
| `1738bf61` | stap 3: README en vloer 190 → 201 |
| `8185e0d6` | lensfix stap 3: de ledenwaarden van een creatie zijn invoer; de delingsweigering per concreet type |
| `e92d5fee` | eindlens: een uitzonderingsgroep onder een verdrongen default rekent niets |
| `0d796937` | eindlens: de weigerreden van een binder in het filter van een vouw |
| `e6eefdce` | eindlens: de getuigen zonder tijdelijk pad |

### Wat er staat, en waarom zo

- **Waardevormen** (stap 1): afronding, begrenzing, de builtins buiten de vouwen, tekstsamenstelling van teksten, de
  procentvormen, een macht met een hele exponent, de dimensiecel en -vouw, elfproef en exact-cijfers rekenen als
  functies over gebonden waarden, met de rekenhulpen van `RegelSpraakRuntime` die de exec-modeltak gebruikt; alleen de
  herkomst van de operanden verschilt. `Let` is een behoefte (ch. 10.6): een binder die het lichaam niet leest, rekent
  niet en faalt niet; `IfThenElse` rekent alleen de gekozen tak. Eenheden worden met exacte factoren herrekend, zoals
  ref.
- **Deponeringen** (stap 1 en zijn lensfixes): elke geëvalueerde deling door leeg met een gevulde teller en elke
  negatieve radicand deponeert op het §9.5-kanaal, in een waarde, een voorwaarde, een initialisatie, een assertie, de
  voorwaarde van een creatie en het filter van een vouw (dan onder de lezende vorm, op het onderwerp van de vouw), alleen
  waar de evaluatie haar bereikt (kortsluiting ch. 5.10, call-by-need ch. 10.6). Reden: ch. 5.6 kent geen beperking tot
  gelezen attributen, en ref deponeert zo.
- **De rekendatum** (stap 1) is één feit `Rekendatum` per sessie; een versie met een begrensde geldigheid, een
  gedateerde defaults-groep, `TimeWindow` en `ContextValue` poorten erop; een vorm die de datum nodig heeft, faalt in een
  sessie zonder datum zoals ref.
- **Evaluatiefouten**: elke evaluatiefout van deze vormen, ook de datumfouten van de runtime, is een logisch `Fout`-feit
  dat aan het fixpunt telt (PLAN §9, 2026-10-09); een creatie waarvan een ledeninitialisatie faalt, stelt alleen de
  fout en maakt niets; een regel van een ledenketen van een defaults-groep wacht op de posities boven al zijn
  voorouders, zodat een uitzonderingsgroep onder een verdrongen default niets uitrekent (ch. 7.1).
- **Voorwaardevormen** (stap 2): de kwantorvouw als vouw met bijdragen per lid, met de kortsluiting van ch. 5.10 waar
  de matrix kan falen of deponeren; de rolcontrole als bestaansvraag, ook de verre; uniciteit over één of meer bronnen,
  met de schending op het kanaal aan het fixpunt.
- **Schrijvers** (stap 3): twee of meer rekenende regels op één doel; waar twee aan het fixpunt op één object
  verschillen, de fout van ch. 5.7 als logisch feit, met de schrijvers in documentvolgorde en het object bij zijn
  caselabel, zoals ref. Buiten het staande domein schrijft een berekende leeg het doel leeg (ch. 1.2g, 7.8), met de
  bestaande vraag `staandScalair` van `StaandeDoelen`. Schrijvers tellen per object, dus per concreet type (WF-079,
  ch. 7.10). De ledenwaarden van een creatie zijn de invoer van het nieuwe object, zoals ref: een staande schrijver
  verdringt ze.
- **De leeslaag** (punt 5): niet gedaan, per deel met reden in het verslag van stap 2: de nieuwe lezingen passen in
  `Lezingen`; de gesloten woordenschat met gelijke ids is het middel van de schaalproef van `20261008-tms-omschakeling`;
  de leestabel en de IR-knoop bij elke toets nemen de keuze van `20261008-drl-teksttak-op-truth-maintenance` over de
  scheiding van analyse en weergave voor.

**Buiten "May touch", klein.** `cases/CaseRunner.java` (één regel) en een package-private overload in
`cases/DroolsBackend.java`: het terugzetten van de replay krijgt de caselabels, zodat de schrijversfout het object bij
zijn label noemt; de publieke vorm is ongewijzigd.

### Acceptatie

- **(a)** Rood-eerst getuigen in elke documentvolgorde van de betrokken regels, en waar de tussenstand anders niet
  optreedt in een steekproef van gegenereerde volgordes; de verwachtingen door ref bevestigd, en door clingo waar die de
  vorm vertaalt: `TmsLetWitnessTest`, `TmsRekenvormWitnessTest`, `TmsBuiltinWitnessTest`, `TmsRekendatumWitnessTest`,
  `TmsDatumfoutWitnessTest`, `TmsDeponeringWitnessTest`, `TmsFilterdeponeringWitnessTest`, de kwantor-, rol- en
  uniciteitsgetuigen van stap 2, `TmsSchrijversWitnessTest`, `TmsCreatieInvoerWitnessTest`,
  `TmsVerdrongenUitzonderingWitnessTest` en `TmsVouwbinderWitnessTest`. Waar de lezer vóór de
  afleiding staat, houdt ref nog een fout of deponering van een tussenstand vast; volgens het besluit van 2026-10-09
  telt die niet, en ref volgt met `20261008-fout-op-tussenstand`.
- **(b)** De corpusgetuige heeft geen FAIL; `EXPECTED_MIN_PLAYED` 120 → 201. Eerste weigering per familie, nulmeting →
  eind: gespeeld 122 → 201; de vormen van deze kaart 87 → 0 (`Let` 16, `QuantifierFold` 15, afronding 12, twee of meer
  rekenende regels 9, uniciteit 7, de procentvormen 6, `RoleMembership` 5, versies met een begrensde geldigheid 4,
  `BuiltinCall` 4, `Clamp` 3, de additieve operandgrens 3, `power`, contextwaarde en `TimeWindow` elk 1); de vormen van
  `20261008-tms-verdeling-en-tijdlijnen` 41 → 49 (goldens die na hun vorm van deze kaart op een vorm van die kaart
  stuiten); de lader 37 → 37; de grens van het beanmodel 3 → 3; overige weigeringen 4 → 4.
- **(c)** `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over `transpiler/tms/` geeft niets.
- **(d)** De exec-modeltak en de DRL-teksttak byte-gelijk aan de nulmeting na elke stap; gate 12 groen met played 242 en
  DRL 196; JUnit 1344 tests, 0 failures, 0 errors, 0 skipped; alle 16 gates groen op `a3842004`, `deeaaa26`,
  `f7bb0dd4`, `8185e0d6` en `e6eefdce`.

### Lensvondsten

Na elke stap een lens op sol6 (effort xhigh) en na elke fixronde een herkeuring (zeven runs, elk één premium
request), en vóór de landing een eindlens op Opus 5.5 (15 premium requests). De hoofdsessie mat elke vondst na met een
proefdocument dat de verifier goedkeurt.

Aanvaard en gefixt:
- stap 1: een deling door leeg in een voorwaarde deponeerde niet; de deponering vroeg twee gelezen attributen ("5 gedeeld
  door zijn noemer" deponeerde niet); een creatie waarvan een ledeninitialisatie faalde, maakte toch het object, zodat de
  tak twee fouten meldde waar ref er één meldt (`ba8127f2`); de deponering in het filter van een vouw ontbrak, op een
  case van de spelende golden `jutterswet-filterwacht` met een lid zonder deelnemers (`f9929a07`);
- stap 3: de delingsweigering telde schrijvers per declarerend type en weigerde zustersubtypes; samen met de bijvangst
  van de bouwer dat een creatie naast een staande regel een schrijversfout gaf waar ref 99 geeft (`8185e0d6`);
- eindlens (Opus 5.5), BLOKKEREND: een uitzonderingsgroep onder een default die een hogere positie verdrong, werd toch
  uitgerekend, en haar fout bleef staan: een defaults-groep "10 indien wrak" boven een groepsdefault met de
  uitzondering "20 indien het rekenjaar groter is dan 2025" faalde zonder rekendatum, waar ref 10 geeft. Een fout van
  de rekendatum als feit (stap 1) en de ledenketens van de eerste bouwkaart kwamen hier samen; de vier lenzen per stap
  zagen het niet (`e92d5fee`);
- eindlens, ADVIES: een binder van een `Let` in het filter of de matrix van een vouw werd geweigerd met de reden dat
  het document onwelgevormd is (WF-064); het document is welgevormd (ref en clingo 2), en de tak weigert de vorm nu met
  de reden dat hij haar nog niet draagt (`0d796937`).

Afgewezen, met reden:
- herkeuring van stap 1, "een voorwaardelijke initialisatie op een doel dat de invoer vult, deponeert niet": het besluit
  van 2026-10-09 (document C van `fout-op-tussenstand`) zegt dat zo'n initialisatie niet beslist en niet wordt
  uitgerekend; de tak volgt het besluit al, ref en clingo volgen met die kaart (document D, toegevoegd bij de landing).

### Bijvangst

Gemeten, niet in deze kaart gerepareerd:
- nieuwe kaarten: `20261009-tekst-van-een-waarde-in-een-tekstsamenstelling` (spec-gat met normvraag: ref "5.5"/"waar",
  de runtime "5.50"/"true", clingo weigert; de tak weigert met naam) en `20261009-clingo-gemaakt-lid-met-twee-waarden`
  (een creatie naast een staande regel: clingo 106 waar ref 99 geeft) en `20261009-clingo-let-binder-tussen-onderwerpen`
  (de binder van een `Let` lekt in een kwantorvouw onder een telling naar een ander onderwerp: ref 2, clingo 3);
- aanvulling op `20261009-vouwfilter-resterende-vormen`: clingo deponeert een deling uit een vouwfilter op het lid in
  plaats van op het onderwerp van de vouw, ook waar de vouw niet bereikt wordt; geen kortsluiting in de bewaking van de
  matrix van een kwantorvouw; een deponering uit een uitzonderingsgroep onder een verdrongen default, onder het label
  van een andere vorm;
- aanvulling op `20261008-fout-op-tussenstand`: document D;
- aanvulling op `20261008-tms-omschakeling` en `20261008-drl-teksttak-op-truth-maintenance`: wat alleen in de oude takken
  misgaat (geen eigen kaart, afspraak van 2026-10-09).

### Werkwijze

Zes opus55-bouwers: drie voor stap 1, één voor stap 2, één voor stap 3 met zijn fixronde, en één voor de fixronde van
de eindlens. De eerste bouwer van stap 1 liep na vijftig minuten tegen de contextlimiet, zonder commit: `mvn package`
schrijft per run zo'n 16 000 regels, en hij las ze mee. Zijn werk bleef als patch bewaard; de tweede bouwer
verantwoordde en committe het en deed de eerste fixronde tot zijn contextgrens; de derde bouwde de deponering in het
vouwfilter. De werkafspraken lezen sindsdien alleen samenvattingen van mvn en de corpusdoorloop. Na elke stap en elke
fixronde draaide de hoofdsessie zelf een doorloop van het hele corpus door de drie takken, en na elke stap de volledige
gateset.

### Open punten (voor de landing)

1. Een vouw, bereik of stap over een gecreëerde relatie weigert de tak nog; geen golden bereikt het. Voorstel: eerst een
   golden.
2. Uniciteit vergelijkt elke sleutel met elke sleutel, zoals de exec-modeltak: voor de schaalproef van
   `20261008-tms-omschakeling`.
3. Bij drie schrijvers noemt de tak elke schrijver die staat, ref de twee die het laatst veranderden; de README-sectie
   "Recorded: two writers of one target" over de oude takken zegt nog dat ref er drie noemt.
4. De schrijversfout noemt een object bij zijn label alleen in de replay; een afnemer van de publieke
   `zetAfgeleideWaarden` ziet type en identiteit. Voor de keuze over de publieke vorm in `20261008-tms-omschakeling`.
5. Vormen die ref en clingo niet uitvoeren (elfproef, exact-cijfers, uur tot en met milliseconde, `dateWith`,
   `dateTimeWith`, de dimensievormen): de tak volgt de exec-modeltak, zonder onafhankelijke bevestiging.
6. Een binder van een omsluitende `Let` in het filter of de matrix van een vouw draagt de tak niet (luid geweigerd; de
   oude takken weigeren de vorm ook; geen golden). Dragen vraagt de waarde van de `Let` in elke bijdrageregel, met een
   lege waarde die de `Let` over de regelgrens heen beëindigt.
7. De kolom "Los" van het meetharnas `TmsMeting` kan na `e92d5fee` niet meer gevuld raken; het harnas is niet
   aangepast (voor de meting van `20261008-tms-omschakeling`).
8. Geen registertekst voor `spec/CONSTRUCTS.md` of `spec/PLAN.md`: deze kaart verandert de spec niet.
