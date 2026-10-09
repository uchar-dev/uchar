# drools-filterfout-buiten-de-lezende-regel: de exec-modeltak en de DRL-teksttak geven een fout uit het filter van een vouw waar de lezende regel niet geldt

*Gevonden op 2026-10-08 door de lens op stap 2 van de kaart
`20261008-tms-aggregaties-creatie-en-fouten` (waarneming 2 van haar
`Result:`) en door de hoofdsessie vastgelegd volgens de afspraak dat een
bestaande bug een kaart krijgt. Gemeten op uchar main `54ba99f7` (2026-10-09),
met ref, clingo en de verifier uit `_build` van de hoofdcheckout en de shaded jar
van `backends/drools`, gebouwd uit een bevroren kopie van die commit. De
proefbestanden stonden onder `/tmp/tms-volgorde-dump-LOPc/docsF/`
(`Z0-niet-actief`, `Z1-actief`, `D0-niet-actief`, `D1-actief`); de documenten
staan hieronder, zodat de meting te herhalen is.*

## Aanleiding

Ref rekent eerst de voorwaarde van een regel uit en de waarde alleen waar de
regel vuurt. Een samengestelde voorwaarde rekent hij maar tot het lid waarna
haar oordeel vaststaat (`spec/RUNTIME-SEMANTICS.md` ch. 5.10), en het filter van
een vouw per lid alleen waar hij de vouw uitrekent. Deelt dat filter daar door
een numerieke nul, dan faalt de evaluatie (ch. 1.4, punt 4). Nergens anders
wordt die deling uitgevoerd, dus nergens anders ontstaat een fout.

Model: objecttype Jutter met `zware vondsten` (geheel getal) en de kenmerken
`actief` en `druk`; objecttype Vondst met `gewicht` en `deelnemers` (gehele
getallen); feittype aanmelding: één aanmelder (Jutter) heeft meer vondsten
(Vondst).

Document Z:

    Regel zware vondsten: De zware vondsten van een Jutter moet berekend worden
        als het aantal vondsten van de Jutter die aan de volgende voorwaarde
        voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2,
        indien hij actief is.

Document D:

    Regel drukte: Een Jutter is druk indien hij aan alle volgende voorwaarden
        voldoet:
        - hij is actief;
        - het aantal vondsten van de Jutter die aan de volgende voorwaarde
          voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2,
          is groter dan 0.

Case (beide documenten): J1 met de vondsten V1 (gewicht 10, deelnemers 2) en V2
(gewicht 9, deelnemers 0). J1 is niet actief. Verwacht bij Z: `zware vondsten`
van J1 leeg, consistent; bij D: J1 niet druk, consistent. Is J1 wel actief, dan
rekent ref de vouw uit en is de deling van V2 een fout.

## Gemeten (2026-10-09, main `54ba99f7`)

| Uitvoerder | Z, J1 niet actief | D, J1 niet actief | Z en D, J1 actief |
|---|---|---|---|
| verifier | `PASS` | `PASS` | `PASS` |
| ref | `oracle: PASS (2 expectations)` | idem | `EVALUATION FAULT: division by numeric zero` |
| clingo | `clingo: PASS (2 expectations)` | idem | `UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric zero","V2")` |
| Drools, exec-modeltak | `ERROR DelingDoorNul: deling door nul in de regel "zware vondsten": 9 gedeeld door 0 …` | idem, regel "drukte" | idem |
| Drools, DRL-teksttak | `ERROR ConstraintEvaluationException: Error evaluating constraint 'rsAggVergelijk(rsGedeeld(gewicht, deelnemers), …)'` | idem | idem |
| Drools, truth-maintenance-tak (`--tms`) | `PLAYED 1` | `PLAYED 1` | `ERROR DelingDoorNul …` |

De exec-modeltak en de DRL-teksttak rekenen het filter dus uit, ook waar de
lezende regel niet geldt. De tak met truth maintenance kreeg daarvoor in
`a987275c` een markeerfeit `Filterfout`; de fout ontstaat daar pas waar de
lezende vorm de vouw uitrekent. Zijn getuige `TmsFilterfoutWitnessTest`
bouwt beide documenten, met beide cases, en ook een document waarin twee
leden falen (het eerste in de vouwvolgorde telt, ch. 5.11) en een filter dat
alleen op een tussenstand faalt. Geen golden in `corpus/golden` heeft een
deling in het filter van een vouw onder een voorwaarde die niet geldt; daarom
zien de gates 5, 6 en 12 het niet.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met beide vormen (één document met beide regels mag), eigen
  Jutterswet-casus, rechtenvrij, met `bron.rgs`, `ir.json` en cases: J1 niet
  actief (de verwachting hierboven), en een tweede Jutter die wel actief is en
  geen vondst met deelnemers 0 heeft, zodat de vouw ook geteld wordt. Het geval
  "J1 actief" is een fout en hoort in `corpus/rood` (met
  `expectedFault {ref, clingo, drools}`), als het nog geen rood geval met deze
  vorm heeft.
- (b) De exec-modeltak en de DRL-teksttak gerepareerd, rood eerst: ref ≡ clingo
  ≡ Drools (alle drie de takken) op de nieuwe golden, tegen absolute
  verwachtingen, en de fout in het rode geval waar ref en clingo hem geven.
- (c) Nagaan of de andere plaatsen waar een vouw onder een voorwaarde staat
  hetzelfde doen: een vouw in de waarde van een regel met voorwaarde, in een lid
  van een samengestelde voorwaarde ná een lid dat het oordeel beslist, en in de
  voorwaarde of initialisatie van een creatie. Wat zo gevonden wordt, krijgt een
  case in de golden of een getuige.
- (d) Gates 5, 6, 8 en 12 groen; in `scripts/check-drools.sh` gaan
  `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` elk één omhoog (beschermd pad:
  voorstel); de corpusgetuige `TmsCorpusWitnessTest` speelt de golden; alle
  gates van `CLAUDE.md` groen.

## Omvang

Klein tot middel. In beide takken waarschijnlijk dezelfde maatregel: het filter
pas laten rekenen waar de lezende vorm de vouw uitrekent, of een fout uit het
filter uitstellen tot daar, zoals het markeerfeit van de tak met truth
maintenance. De bouwer kiest.

May touch: `corpus/golden/<nieuwe golden>/`, `corpus/rood/<nieuw geval>/`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/DrlTranspiler.java`
(en wat die tak deelt, voor zover nodig), getuigen onder
`backends/drools/src/test/java`, `scripts/check-drools.sh` (de twee pins;
voorstel), kaartverplaatsing. Niet in deze kaart: ref en clingo (geven al het
goede antwoord), de spec, en de tak met truth maintenance.

## Raakvlakken

- Na het besluit van 2026-10-08 (PLAN §9) vervangt `20261008-tms-omschakeling`
  de exec-modeltak door de verlaging met truth maintenance, en zet
  `20261008-drl-teksttak-op-truth-maintenance` de DRL-teksttak op truth
  maintenance. De fix in die twee takken vervalt dan; de golden en het rode
  geval blijven. Deze kaart is klein en kan daarvóór landen.
- `20261008-fout-op-tussenstand` gaat over een fout op een tussenstand; deze
  kaart over een fout waar geen evaluatie is. Ze raken elkaar niet, behalve dat
  beide een fout als iets van de evaluatie zien en niet van de verlaging.
- Serieel met andere kaarten in `backends/drools` (de pins van gate 12).

## Result (2026-10-09)

*Geschreven door de hoofdsessie. De eigenaar was offline; elke kaart van deze ronde stopt vóór de merge.*

| Commit | Wat |
|---|---|
| `abc71db2` | claim |
| `6ac1ff52` | een delend vouwfilter rekent pas waar een vorm de vouw bereikt; golden `jutterswet-filterwacht`, rood geval `vouwfilter-deelt-door-nul`, getuige `FilterfoutWitnessTest`, README |
| `9482ec1d` | VOORSTEL gate 12: `EXPECTED_PLAYED` 240 → 241, `EXPECTED_DRL_PLAYED` 194 → 195 |
| `4ada319d` | lensfix: de volgorde van de leden rond een vouw, de lijst `Faalbaar`, de weigering van een defaults-groep met een vouw die kan falen, de reactiviteit van een opnieuw gebonden element in de DRL-teksttak |
| `9c6adc07` | eindlensfix: een vouw die kan falen onder een andere kwantor dan "alle", en de getuige met onderscheidbare vouwen |
| `ae6ac6ed` | herkeuringsfix: de weigering geldt alleen voor een lid dat ref kan overslaan |

### Wat er staat

Ref rekent het filter van een vouw alleen uit waar een vorm de vouw bereikt. De exec-modeltak en de DRL-teksttak deden
dat nu ook:
- **Herbinding.** Een filter dat in de runtime hard kan falen, bindt opnieuw op het element in de bron van de
  accumulate. Welke operaties dat zijn, staat in de gesloten lijst `transpiler/kern/Faalbaar.java`, gedeeld door beide
  takken: deling, macht, datumrekening met de dagsoortstap, datum uit delen, Rekenjaar. Drools toetst zo'n filter alleen
  voor tupels die de accumulate bereiken, en alleen voor de elementen van dat onderwerp. Andere filters blijven op het
  elementpatroon, zodat het regelnetwerk van de andere goldens niet verandert.
- **Volgorde van de leden.** In een samenstelling staan de leden ná het eerste lid met eigen items (een accumulate, een
  exists) op een opnieuw gebonden onderwerp, ná die items. Zo houdt de tak de volgorde van ch. 5.10 aan.
  - In de exec-modeltak geldt dat voor alle condities.
  - In de DRL-teksttak geldt het voor de voorwaarde van een regel, van een objectcreatie en voor het anker van een
    feitcreatie. Daar komt de voorwaarde nu ook vóór de waarde.
- **Reactiviteit in de DRL-teksttak.** Het opnieuw gebonden element krijgt `java.util.Objects.nonNull(this)`. Zonder die
  aanroep zag een opnieuw gebonden filter een deler niet die een regel later schrijft. Dat was een regressie van de
  eerste commit, gevonden en gefixt in de fixronde.
- **Wat de exec-modeltak luid weigert**, per vorm met reden:
  - een defaults-groep met een vouw waarvan het filter kan falen: de tak verzamelt de accumulates van alle
    rechtvaardigingen vóór de prioriteitsbeslissing, en kan die evaluatie daarom niet uitstellen;
  - een samenstelling onder een andere kwantor dan "alle" met een datumvouw, waarvan een filter kan falen, als die vouw
    in een lid staat dat ref kan overslaan (een lid waarvóór een stoptrigger van de samenstelling kan vuren, ook genest;
    ch. 5.10): de tak bouwt alle accumulates van zo'n samenstelling vóór de toets die de leden kortsluit.

  Geen golden raakt een van die weigeringen.

### Acceptatie

- (a) Golden `jutterswet-filterwacht`, Jutterswet-casus, met `bron.rgs`, `scenario.rgs`, `ir.json`, `NOTE.md` en twee
  cases:
  - case 1: J1 niet actief, met V2 met deelnemers 0;
  - case 2: J2 en J3 actief, zonder nuldeler.

  Twee losse cases, omdat clingo één case met J1 en J2 samen UNSAT maakt. Dat is een clingo-fout; de kaart
  `20261009-vouwfilter-resterende-vormen` houdt hem.

  Rood geval `vouwfilter-deelt-door-nul`: J1 actief, met `expectedFault` ref/clingo "division by numeric zero" en Drools
  "9 gedeeld door 0 — een numerieke nul als deler is een fout". Welke regel de Drools-zin noemt, hangt van de
  evaluatievolgorde af.
- (b) Exec en DRL gerepareerd, rood eerst.
  - Vóór de fix gaf de golden in exec `ERROR DelingDoorNul … "zware vondsten": 9 gedeeld door 0` en in DRL `ERROR
    ConstraintEvaluationException … [Rule "drukte", "zware vondsten" …]`.
  - Na de fix speelt de golden in alle drie de takken (`PLAYED 2`).
  - `FilterfoutWitnessTest` loopt door beide takken: 22 tests, onder meer alle 24 volgordes van vier leden (actief, een
    delende vouw, een delend lid, een kwantorvouw met een eigen deler) op vier cases (192 cellen, tegen ref nagerekend via
    de frontend), de vormen van (c), en de weigeringen en hun tegenhangers die moeten spelen. Op de code van vóór elke
    fix gemeten rood; mutanten (de volgorde van twee vouwen, het element zonder `nonNull`, de waarde vóór de voorwaarde)
    worden gevangen.
- (c) De andere plaatsen, gemeten en in de getuige:
  - vouw in de waarde onder een voorwaarde;
  - voorwaarde en initialisatie van een objectcreatie;
  - filter en matrix van een kwantorvouw;
  - element buiten elk bereik, ook via een pad;
  - leden ná de vouw in een samenstelling, in alle 24 volgordes van vier leden;
  - het anker van een feitcreatie;
  - een overschreven deler;
  - de andere operaties van de lijst.

  Wat niet met dezelfde maatregel kon, staat bij de open punten.
- (d) Gates: zie onder. In `scripts/check-drools.sh` gaan `EXPECTED_PLAYED` 240 → 241 en `EXPECTED_DRL_PLAYED`
  194 → 195. Dat is een voorstel in een eigen commit (`9482ec1d`); het beschermde pad is verder niet geraakt.

Corpusdoorloop van de hoofdsessie: elke golden door de drie takken, met een jar uit een bevroren kopie van de tip,
tegen dezelfde doorloop op main `54ba99f7`. Alleen de nieuwe golden verschijnt, en die speelt in alle drie. Geen
bestaande statusregel veranderde: exec 241, DRL 195 plus de 2 bekende FAIL, truth maintenance 121.

Volledige gateset van de hoofdsessie op `ae6ac6ed`: alle 16 groen. Gate 12: `check-drools: PASS` (played 241, DRL
195, JUnit 1253); gate 8: `43 red cases …`; gate 5 en 6: `803 cases replay exactly` (ref = clingo); gate 7: `759 cases
refute a do-nothing executor`; gate 10: `123 rgs golden(s) re-emit byte-identically`; gate 16: `30 scenario golden(s)`.

### Lensvondsten

Copilot (sol6, effort xhigh) draaide vier runs (lens na de bouw, eindlens na de fixronde, twee herkeuringen), elk één
premium request. De eindlens met Opus 5.5 is niet gedraaid;
die stemt de hoofdsessie eerst met de eigenaar af.

Aanvaard en gefixt:
- **Lens 1, BLOKKEREND:** in een "alle"-samenstelling met de vouw vóór een lid dat niet geldt, gaf de eerste fix een
  antwoord waar ref een fout geeft. Dat was een nieuwe afwijking; vóór de fix kwam de fout er toevallig wel. Andersom
  faalde een lid ná een vouw die al besliste. Opgelost met de volgorde van de leden (`4ada319d`).
- **Lens 1, BLOKKEREND:** een lagere default rekende in de exec-modeltak een vouw uit die ref niet leest. Opgelost met
  een luide weigering (`4ada319d`).
- **Lens 1, BLOKKEREND:** de herbinding gold alleen voor een deling. Opgelost met de lijst `Faalbaar` (`4ada319d`).
- **Eindlens, BLOKKEREND:** een tellende kwantor met een datumvouw las in de exec-modeltak te vroeg. Opgelost met een luide weigering (`9c6adc07`), die de herkeuring te ruim vond (een
  datumvouw als enig lid onder "geen" speelde vóór de fix goed). Versmald in `ae6ac6ed` tot een lid dat ref kan overslaan.
  De hoofdsessie vroeg "alleen na het eerste lid"; de bouwer liet met een mutant zien dat dat niet klopt (de drempel kan al
  vóór het eerste lid beslissen, ch. 5.10) en bouwde de preciezere regel.
- **Eindlens, BLOKKEREND:** de getuige met 24 volgordes onderscheidde zijn twee vouwen niet. Opgelost: de twee vouwen delen nu door verschillende operanden, een vierde case laat de
  volgorde van de twee vouwen beslissen, en een mutant die die volgorde omdraait, valt (`9c6adc07`).

Afgewezen:
- **Eindlens, ADVIES "de DRL-fout verliest zijn regelnaam":** de uitkomst (een fout) blijft gelijk en gepind op haar
  fragment. De regelnaam is diagnostiek; ze staat beschreven in de README, de javadoc en de getuige. De DRL-teksttak
  wordt bovendien door `20261008-drl-teksttak-op-truth-maintenance` op truth maintenance herbouwd.
- **Lens 1, ADVIES (de twee losse cases door een clingo-fout):** geen fout in deze kaart. De clingo-fout staat op de kaart
  `20261009-vouwfilter-resterende-vormen`.

Herkeuring van `ae6ac6ed` (sol6), afgewezen met reden, omdat de kaart hier ophoudt:
- **BLOKKEREND "de foutvolgorde":** onder "precies 1" met eerst een delend lid (5/0) en dan een datumvouw (9/0) geven ref
  en de exec-modeltak allebei een fout. Ref noemt de eerste deling (ch. 5.11), de tak de tweede. De uitkomst is gelijk;
  welke deling genoemd wordt, is diagnostiek. De tak met truth maintenance houdt ch. 5.11 aan, en de exec-modeltak
  vervalt met de omschakeling.
- **BLOKKEREND "een altijd gelezen vouw blijft geweigerd":** "precies 1" over (actief; niet actief; datumvouw) wordt luid
  geweigerd, hoewel ref de vouw daar altijd leest. Het is een weigering met reden van een vorm met elkaar uitsluitende
  leden, geen fout antwoord. De README noemt deze ruimte.
- **ADVIES "pin ook precies 2 over één vouw":** die vorm wordt correct geweigerd door dezelfde regel als de gepinde
  vormen "ten hoogste 1" en "ten minste 2".

### Open punten

1. **Kaart `20261009-vouwfilter-resterende-vormen`** (main `84932950`, aangevuld in `e0d67d6a`) houdt wat deze kaart
   niet repareert:
   - de foutguard van clingo zonder join van het lid, die de golden in twee cases splitst;
   - clingo onder een lagere default;
   - de "unsafe variables" van clingo bij een creatie onder een vouwvoorwaarde;
   - de kwantorvouw met stoptrigger in de matrix (ref PASS, exec en DRL een fout, al vóór deze kaart);
   - de begeleider in de DRL-teksttak die de vouw in de voorwaarde negeert;
   - de negatieve wortel in een filter (een deponering).
2. **Kaart `20261009-ref-macht-van-nul`** (main `e0d67d6a`): ref crasht op nul tot een negatieve macht met een ongevangen
   exceptie.
3. **Bijvangst die al een plaats heeft:**
   - ref deponeert een deling door leeg op een tussenstand: de restpost van ch. 6.1 en de kaart
     `20261008-fout-op-tussenstand`;
   - een datum voorbij de jaren van `java.time`: de kaart `20261009-datumrekening-slaat-stil-om`.
4. **De weigeringen in de exec-modeltak** (defaults-groep, een samenstelling onder een andere kwantor) zijn breder dan strikt nodig: ze vallen ook op
   een vouw die ref altijd leest. De omschakeling naar truth maintenance laat ze vervallen.
5. **Landing:** de branch staat op main `9b9a42ad`. Vóór de ff-merge eerst `git rebase main` en de gates opnieuw.
   `20261009-drools-datumstap-buiten-bereik` raakt `ExecModelTranspiler` en `TmsTranspiler` op andere plaatsen.
6. Geen registertekst voor `spec/CONSTRUCTS.md`; voor PLAN §9 niets.

### Landing (2026-10-09)

Na `git rebase` op de gelande `20261009-drools-datumstap-buiten-bereik`, zonder conflict; alle 16 gates groen op die tip
(gate 12: played 241, DRL 195), ff-merge naar main.
