# creatiebudget-ref-en-clingo: ref en clingo begrenzen een keten van creaties op verschillende plaatsen

*Gevonden op 2026-10-08 tijdens de kaart
`20261008-tms-aggregaties-creatie-en-fouten` (waarnemingen 9 en 10 van haar
`Result:`, de eindlens en de gerichte lens op haar budgetfix) en door de
hoofdsessie vastgelegd volgens de afspraak dat een bestaande bug een kaart
krijgt. Gemeten op die branch op 2026-10-08 en opnieuw op uchar main
`54ba99f7` (2026-10-09), met ref, clingo en de verifier
uit `_build` van de hoofdcheckout en de shaded jar van `backends/drools` uit een
bevroren kopie van die commit. De documenten zijn `documentB` en `documentL` in
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/TmsCreatiestratumWitnessTest.java`
(regel 325–483 op main); de proefbestanden stonden onder
`/tmp/tms-stratum-JfLq/probe/keten/` en `/tmp/tms-stratum-JfLq/docs2/`, de
uitkomsten in `/tmp/tms-stratum-JfLq/refclingo2.txt`.*

## Aanleiding

`spec/RUNTIME-SEMANTICS.md` ch. 5.5 (het slot over recursieve creatie): "a
bounded fire budget turns a non-terminating set into a **fault**, never a silent
truncation (the reference interpreter's pass budget; the ASP backend's
generation-depth bound, whose exhaustion is a named `viol/2` witness → UNSAT)".
De spec noemt twee mechanismen en zegt niet waar de grens ligt.

- Ref begrenst het fixpunt van elk stratum op 100 passen (`ref/lib/eval.ml`,
  `fixpoint` rond regel 3610 en de strata-lus rond regel 3997–4004); een pas telt
  als hij iets verandert, en een nieuw object zonder geschreven waarde is geen
  verandering.
- Clingo telt een generatiediepte per gecreëerd object (`depth/2`,
  `#const oc_budget = 100` en `viol("generation budget exhausted",X) :- depth(X,oc_budget)`
  in `backends/clingo/lib/translate.ml` rond regel 11336–11341). Een lid van een
  recursieve regelgroep maakt alleen onder `D < oc_budget`, en een
  niet-recursieve creatie in een document met recursieve ketens erft `D+1` van
  haar onderwerp (rond regel 9912–9924), zodat de diepte over strata optelt.
- De Drools-exec-modeltak en de DRL-teksttak kennen geen van beide; ze hebben
  een vuurplafond van 100 000 vuringen. De tak met truth maintenance telt sinds
  `1f2ad05c` zoals ref.

Het rode geval `corpus/rood/finite-chain-over-budget` en de golden
`synth2-budgetgrens-onder` pinnen de grens voor één keten in één stratum
("the exact flip sits at 100 → 101 generations, verified in BOTH executors");
daar valt hij voor beide op dezelfde plaats. Op de vormen hieronder niet.

## Gemeten

Model (documentB/documentL): objecttypen Wachter (`rang`) en Boei (`nummer`,
`wachterrang`, en in documentL ook `kopie` en het kenmerk `genummerd`);
feittypen opvolging (voorganger/opvolger, Wachter), boeiketen (vorige
boei/volgende boei, Boei) en bewaking (wachter/boei). "werf de opvolger" maakt
zolang de rang kleiner is dan 52 een opvolger met rang plus 1 (51 generaties
vanaf W1); "leg de volgende boei" maakt zolang het nummer kleiner is dan de grens
een volgende boei met nummer plus 1 en een `wachterrang` die de rang van de
wachter van de Boei leest, waardoor die keten een stratum hoger staat. Beide in
een eigen recursieve regelgroep. Case: W1 (rang 1) bewaakt B1 (nummer 1).

| Document | ref | clingo |
|---|---|---|
| Keten met drie strata (`probe/keten`): Wachters tot rang 51 (50 generaties), een niet-recursieve creatie die bij rang 51 één Boei met nummer 1 maakt, Boeien tot nummer 51 (50 generaties) | `PASS` | `UNSATISFIABLE … "generation budget exhausted"` (50 + 1 + 50) |
| documentL, Boeien tot B101 (100 generaties in stratum 1), zonder berekende eigenschap | `PASS` (de 101e pas verandert niets) | `UNSATISFIABLE … "generation budget exhausted"` |
| idem, met de kenmerktoekenning "genummerd" (een stratum hoger) | `PASS` | `UNSATISFIABLE …` |
| idem, met de rekenregel "kopie = nummer" in hetzelfde stratum | `EVALUATION FAULT: no fixpoint within 100 passes …` | `UNSATISFIABLE …` |
| idem tot B100, met "kopie" | `PASS` | `PASS` |

Elk document in beide volgordes van de twee creaties, met dezelfde uitkomst. De
verifier geeft overal `PASS`. Drools: de exec-modeltak en de DRL-teksttak spelen
alle vijf (ook waar ref en clingo falen); de tak met truth maintenance geeft wat
ref geeft.

Waarneming 10 van het `Result:` (gelezen, niet gemeten): op de grens hangt ref af
van de documentvolgorde, omdat een regel die een latere regel leest in ref een
pas later tot rust komt. Er is geen document dat het laat zien.

De Fable-adviseur (zie onder) mat beide richtingen na (proefbestanden in
`/tmp/lens-bugs/advies-probes/`):

- **Ref hangt op de grens van de documentvolgorde af.** De keten tot B100 met
  "kopie = nummer" en "kopie2 = kopie": met de lezer eerst
  `EVALUATION FAULT: no fixpoint within 100 passes`, met de schrijver eerst
  `PASS`; clingo `PASS` in beide (`q2-lezer-eerst`, `q2-schrijver-eerst`).
- **Eén keten, één stratum, toch een venster.** De keten van
  `corpus/rood/finite-chain-over-budget` zonder consistentie: 99 aanmaken spelen
  beide, 100 aanmaken speelt ref en is in clingo UNSAT, 101 falen beide
  (`q2-grens-100/101/102`). De `NOTE.md` van dat rode geval ("no split window")
  klopt dus niet bij precies één generatie.

## Besluit (voorlopig, 2026-10-09)

*De eigenaar was offline en vroeg bij twijfel een Fable-adviseur te
raadplegen. Het advies (Fable 5.1, alleen lezend, op main `54ba99f7`) staat in
`/tmp/lens-bugs/advies-fable.md`, vraag 2; dit is het aanbevolen antwoord, en
het blijft voorlopig tot de eigenaar het bevestigt. Elke kaart stopt vóór de
merge.*

**Het budget telt generaties per stratum.** In de termen van de generaties van
`20261009-creatie-initialisatie-leest-groeiende-waarde`: een stratum laat ten
hoogste 100 generaties toe die een object maken of een link leggen; een 101e is
in elke executor een fout, nooit een stille afkapping. De vormen die niets maken
krijgen geen budget: hun fixpunt bestaat (WF-045, één schrijver per doel) en
wordt binnen N ronden bereikt, met N het aantal van die vormen in het stratum;
een executor die itereert, mag N + 1 ronden (of het grootste van 100 en N + 1)
als detector van het conflict tussen twee schrijvers van ch. 5.7 gebruiken, en
dat is geen semantiek. Volgorde-onafhankelijk, omdat een generatie een functie is
van een uitgerekende wereld en niet van een plaats in een pas; per stratum, omdat
ch. 6.3 een lager stratum tot zijn fixpunt brengt vóór het hogere begint, zodat
de voltooide keten van stratum k voor stratum k + 1 invoer is.

Afgewezen: passen zoals ref nu (op de grens volgorde-afhankelijk, gemeten);
generaties per keten zoals clingo nu (telt over strata op en ligt één generatie
lager dan ref); per recursieve regelgroep (twee groepen die elkaar in één
stratum voeden, vormen één kring, dus dat valt samen met per stratum). Het corpus
hoeft niet van de grens weg te blijven: na dit besluit is de grens norm, en dan
hoort ze vastgezet.

## Besluit herzien (voorlopig, 2026-10-09, na de lens op stap 1)

*De lens op stap 1 (`/tmp/lens-bugs/w9/lens1/lens.md`) vond twee vormen waarop de telling in generaties in clingo niet
uit te drukken is: een recursieve feitcreatie die een sluiting van meer dan 100 links legt (ref faalt op het budget,
clingo speelt), en een keten waarin aanmaken en links elkaar afwisselen (generatie 101 maakt pas W52: ref faalt, clingo
speelt). De hoofdsessie twijfelde en vroeg een tweede Fable-advies (`/tmp/lens-bugs/advies2-fable.md`); dat wordt het
voorlopige besluit, en het vervangt het besluit hierboven. Het blijft voorlopig tot de eigenaar het bevestigt.*

**Het budget telt de aanmaakdiepte per stratum.** Elk gemaakt object heeft een diepte in het stratum van de creatie die
het maakte: één meer dan de diepte van zijn onderwerp in dat stratum; een invoerobject, en een object dat een lager
stratum maakte, heeft diepte 0. Een aanmaak op diepte 101 is in elke executor een fout, bij het maken. Feitcreaties
hebben geen budget: ze maken niets, en over het eindige universum dat de aanmaken van een stratum achterlaten zijn de
links eindig (ch. 5.5 zegt al dat een relatiecreatie geen guard nodig heeft). De vormen die niets maken hebben geen
budget (WF-045), wel een begrenzing als detector van twee schrijvers. De generaties van
`20261009-creatie-initialisatie-leest-groeiende-waarde` blijven het leesmoment; alleen de teller verandert. Terminatie:
elk (creatie, onderwerp) maakt hooguit één object, dus de boom van aanmaken per stratum vertakt eindig en heeft hoogte
ten hoogste 100. Op een zuivere keten is diepte gelijk aan generatie, zodat de pins van
`finite-chain-over-budget`, `synth2-budgetgrens-onder` en de nieuwe grensgolden hun uitkomst houden; de foutzin wordt
in elke executor "creation budget exhausted". Clingo telt al zo (stap 1); ref en de tak met truth maintenance passen
zich aan (stap 3). De nestgrens van clingo (190 geneste termniveaus) blijft een benoemde backendgrens.

## Acceptatie

- (a) **Ref**: per stratum een buitenste lus van ten hoogste 100 generaties om de
  lus van de regellaag (die ten hoogste het grootste van 100 en N + 1 ronden
  draait, met de zin "writers of one target disagree" als die niet tot rust
  komt). Een 101e generatie die iets maakt of linkt, is de fout "no fixpoint
  within 100 generations …"; het fragment "no fixpoint within" van de rode
  gevallen blijft kloppen. Bouwt op de generatielus van de vorige kaart.
- (b) **Clingo**: de diepte begint per stratum opnieuw: een aanmaak krijgt
  `D+1` alleen van een onderwerp dat in hetzelfde stratum gemaakt kan zijn,
  anders diepte 1 (statisch uit de strata die `Ir_v0.Checks` geeft); de guard
  laat de honderdste generatie toe en `viol("generation budget exhausted", …)`
  valt op de 101e. De termdiepte blijft onder de grens die
  `backends/clingo/README.md` noemt.
- (c) **Drools, tak met truth maintenance**: telt generaties die iets maken of
  linken; de tak voor een pas waarin alleen de regellaag verandert
  (`DroolsBackend.java` rond regel 412–416, uit `1f2ad05c`) vervalt, en de
  getuige `een_pas_waarin_alleen_de_regellaag_verandert_telt_mee_voor_het_budget`
  (`TmsCreatiestratumWitnessTest` rond regel 436–458) wordt een getuige die
  speelt; noem die aanpassing met reden. De exec-modeltak en de DRL-teksttak
  hebben geen budget (`scripts/check-rood.sh`, `NOFAULT` bij
  `finite-chain-over-budget`) en blijven ongewijzigd.
- (d) **Corpus**: een golden óp de grens (de vorm van `finite-chain-over-budget`
  met precies 100 generaties, absolute verwachtingen) en een rood geval erover
  (101 generaties; fragmenten ref "no fixpoint within", clingo "generation
  budget exhausted"); `synth2-budgetgrens-onder` blijft; de `NOTE.md` van
  `finite-chain-over-budget` gecorrigeerd. Een golden of rood geval met de keten
  over drie strata (50 + 1 + 50). Voorbeelden in `ref/test/` voor de
  volgorde-onafhankelijkheid op de grens (`q2-lezer-eerst` en
  `q2-schrijver-eerst` spelen allebei).
- (e) **Spec** (beschermd pad: voorstel in het `Result:` en als diff op de
  branch): het slot van ch. 5.5 over het budget ("the generation budget": 100
  generaties per stratum die maken of linken, in elke executor, met de diepte van
  clingo die per stratum opnieuw begint, en geen budget voor de vormen die niets
  maken), en in `spec/IR-SPEC.md` (rond regel 3029) "pass budget" →
  "generation budget".
- (f) Gates: de nieuwe golden in `scripts/check-drools.sh` zoals
  `synth2-budgetgrens-onder` (de pins `EXPECTED_PLAYED` en
  `EXPECTED_DRL_PLAYED`; voorstel); de vloer van gate 8 groeit vanzelf. Geen
  andere bestaande verwachting in `corpus/golden` of `corpus/rood` verandert;
  verandert er een, dan stopt de bouwer en meldt het. Alle gates van
  `CLAUDE.md` groen.

## Omvang

Middel. Ref en clingo elk één plaats, de tak met truth maintenance één tak
minder, plus corpus en spectekst.

May touch: `ref/lib/**`, `ref/test/**`, `backends/clingo/**`,
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsBackend.java`
en de getuige in `TmsCreatiestratumWitnessTest`, `corpus/golden/<nieuw>/`,
`corpus/rood/<nieuw>/` en de `NOTE.md` van `finite-chain-over-budget`,
`scripts/check-drools.sh` (pins; voorstel), `spec/RUNTIME-SEMANTICS.md` en
`spec/IR-SPEC.md` (voorstel), kaartverplaatsing.

## Raakvlakken

- Bouwt op `20261009-creatie-initialisatie-leest-groeiende-waarde` (de
  generaties in ref); serieel, daarna, op diens branch.
- `20261008-tms-omschakeling`: haar schaalproef (punt 9) heeft een recursieve
  keten van diepte 3, ver onder het budget.
