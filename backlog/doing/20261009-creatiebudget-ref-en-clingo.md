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

## Result (2026-10-09)

*Geschreven door de hoofdsessie. De eigenaar was offline en vroeg bij twijfel een Fable-adviseur te raadplegen. Het
besluit is twee keer op advies genomen: eerst generaties per stratum, na de lens op stap 1 aanmaakdiepte per stratum.
Het blijft VOORLOPIG tot de eigenaar het bevestigt. De kaart staat bovenop
`20261009-creatie-initialisatie-leest-groeiende-waarde`, landt samen met die kaart en stopt vóór de merge.*

| Commit | Wat |
|---|---|
| `e15baf01` | claim |
| `7a6414af` | clingo: de diepte per stratum (`depth(X,K,D)`), de honderdste aanmaak toegelaten, een nestgrens van 190 (stap 1) |
| `fe795294` | tms: één fase per generatie (V2 uit de kaart eronder: elke creatie van een generatie leest dezelfde wereld) (stap 1) |
| `465b3519` | corpus: golden `jutterswet-budgetgrens-op` (100 aanmaken) en rood `jutterswet-budgetgrens-over` (101) (stap 2) |
| `df1539cb` | tms: een getuige die een objectcreatie met auto-focus weer vangt (document R) (stap 2) |
| `03aac501` | VOORSTEL gate 12: `EXPECTED_PLAYED` 240 → 241, `EXPECTED_DRL_PLAYED` 194 → 195 |
| `1756c3f8` | VOORSTEL gate 8: het nieuwe rode geval is NOFAULT in de exec-modeltak (geen budget daar) |
| `7b86bd55` | kaart: het besluit herzien naar aanmaakdiepte per stratum (hoofdsessie) |
| `fe857c73` | ref: het creatiebudget telt de aanmaakdiepte per stratum (stap 3) |
| `64302b1b` | clingo: één foutzin "creation budget exhausted", en een klem op een ontbrekende diepte (stap 3) |
| `fa7ede26` | corpus: de rode gevallen van het budget pinnen die zin (stap 3) |
| `8e06d163` | tms: het creatiebudget telt de aanmaakdiepte per stratum (stap 3) |
| `f534b387` | VOORSTEL gate 8: de noot bij `finite-chain-over-budget` |
| `8862edc1` | VOORSTEL spec ch. 5.5: het creatiebudget telt de aanmaakdiepte |
| `4a2dd490` | VOORSTEL IR-SPEC ch. 3.9: een sluiting valt niet onder het budget |
| `d6fbe084` | clingo: de nestgrens rust op een verkeerd gelezen meting (README en commentaar; eindlens) |
| `a84b7426` | corpus: de golden met twee ketens pint dat beide ketens volledig zijn (eindlens) |
| `b78a8b9e` | rood: de budgetgevallen pinnen de plaats van de fout (eindlens) |
| `f741b4bd` | VOORSTEL spec ch. 5.5 en IR-SPEC ch. 3.9: resourcegrens naast budget (eindlens) |

### Wat er staat

Elk gemaakt object heeft een aanmaakdiepte in het stratum van zijn creatie. Die diepte is één meer dan die van zijn
onderwerp in dat stratum. Een invoerobject en een object van een lager stratum hebben diepte 0. Een aanmaak op diepte 101
is in ref, clingo en de tak met truth maintenance een fout, met de woorden "creation budget exhausted". Feitcreaties en
de vormen die niets maken hebben geen budget. Het leesmoment blijft de generatie van de kaart eronder; alleen de teller
is anders.

- **Ref** legt stratum en diepte vast bij het maken. Het generatiebudget van de kaart eronder vervalt. De lus over de
  generaties heeft geen grens nodig: elke generatie behalve de laatste maakt of linkt iets, en onder het budget zijn
  objecten en links eindig. Een onderwerp dat een hoger stratum maakte, faalt luid als invariant.
- **Clingo** telde in stap 1 al per stratum. Stap 3 voegde de gedeelde zin toe en een klem: een creatie waarvan het
  onderwerp geen diepte heeft, faalt nu luid in plaats van stil niet te vuren.
- **De tak met truth maintenance** draagt stratum en diepte op het merkteken `Gemaakt`. Bij diepte 101 gooit de
  creatieregel `Creatiebudget`, vóór er iets gemaakt is, en de driver eindigt daarmee. Eén fase per generatie houdt het
  leesmoment van de kaart eronder (V2).
- **De exec-modeltak en de DRL-teksttak** hebben geen budget, alleen een vuurplafond. Ze draaien een eindige keten
  voorbij het budget uit; gate 8 noemt dat NOFAULT met reden.

### Acceptatie

- (a) Ref: gedaan in de kaart eronder (generaties) en hier herzien naar diepte.
- (b) Clingo: diepte per stratum, de honderdste toegelaten, de 101e de fout. Stap 1 voegde ook een nestgrens van 190
  toe, op de lezing dat clingo 5.8.2 een term dieper dan ongeveer 200 niveaus niet afdrukt. De fixronde na de eindlens
  mat dat dat niet klopt: clingo breekt zijn JSON-uitvoer af op een string van bepaalde lengtes, niet op een diepte.
  Een keten van niet-recursieve creaties breekt zo af bij 187, en speelt bij 200 en 201; dat gebeurt ook al op main.
  De code van de nestgrens is ongewijzigd gebleven, met README en commentaar rechtgezet. De echte oorzaak staat op de
  kaart `20261009-clingo-uitvoer-breekt-af-op-lengte` (main `8aa05068`), die ook de nestgrens herziet.
- (c) Tak met truth maintenance: diepte, één fase per generatie (V2 gerepareerd, niet geweigerd), en een getuige die
  de auto-focusmutant weer vangt.
- (d) Corpus:
  - de golden `jutterswet-budgetgrens-op`: 100 aanmaken, met een regel in hetzelfde stratum; case 2 heeft twee ketens
    naast elkaar;
  - het rode geval `jutterswet-budgetgrens-over`: 101 aanmaken;
  - de NOTE's van `finite-chain-over-budget` en `runaway-recursion`, en de fragmenten van de rode gevallen.

  Na de eindlens pint case 2 dat beide ketens volledig zijn: een `ketennummer` en per keten een consistentie aan het
  plafond, met elk een eigen kanaalfeit. De rode gevallen pinnen de plaats van de fout: ref met creatie, stratum en
  "would mint at creation depth 101", clingo met `viol("creation budget exhausted",oc0(` en in de suite de exacte term
  op diepte 101.

  Geen golden over drie strata: clingo grondt 50 + 1 + 50 in ongeveer 23 s en 1,7 GB. Die vorm staat in de clingo-suite
  en de getuigen van de tak, telkens met ref ernaast.
- (e) Spec: voorstellen op de branch (ch. 5.5, IR-SPEC ch. 3.9); de tekst staat in de diff. Ch. 5.5 heeft na de
  eindlens een alinea "Resource bounds". Die scheidt het creatiebudget (de norm) van de resourcegrens van een executor:
  het vuurplafond van 100 000 in Drools, en de nestgrens van clingo. Gemeten op de erflijn over 450 Wachters (101 025
  links): ref en clingo geven erfvolgeraantal(W1) = 449, de tak met truth maintenance eindigt aan het plafond, en de
  andere twee takken dragen het document niet.
- (f) Pins: voorstel, 241 en 195. De golden speelt in de exec-modeltak en de DRL-teksttak.

Gemeten op de zes proefdocumenten van het tweede advies: een sluiting van 102 hops, en ketens die op een link wachten
tot diepte 100, 101 en daarboven. Ref en clingo geven nu dezelfde uitkomst: alles speelt, behalve diepte 101, dat in
beide faalt. Geen bestaande golden of rood geval veranderde van uitkomst. Alleen de foutzin van de rode budgetgevallen is
nieuw; hun pins zijn mee aangepast.

Corpusdoorloop van de hoofdsessie: elke golden door de drie Drools-takken, met een jar uit een bevroren kopie van
`4a2dd490`, tegen main `54ba99f7`. Alleen de nieuwe golden verschijnt, en die speelt in alle drie. Geen bestaande
statusregel veranderde.

Volledige gateset van de hoofdsessie op `f741b4bd` (lokaal): alle 16 groen. Gate 5: `ref suite 1277 worked examples; 803
cases replayed exactly`; gate 6: `backend suite 2138 checks; 803 cases replay exactly through clingo — ref = clingo`;
gate 8: `43 red cases …; 86 pinned fragments`; gate 12: `check-drools: PASS` (played 241, DRL 195). Beschermde paden
alleen als VOORSTEL in eigen commits: `scripts/check-drools.sh` (pins), `scripts/check-rood.sh` (een NOFAULT-regel en
de noot bij `finite-chain-over-budget`), `spec/RUNTIME-SEMANTICS.md` en `spec/IR-SPEC.md`.

### Lensvondsten

Copilot (sol6, effort xhigh), drie runs (lens op stap 1, eindlens, herkeuring van de fixronde), elk één premium
request. Daarnaast twee Fable-adviezen (Claude, alleen lezend). De eindlens met Opus 5.5 is niet gedraaid,
omdat de hoofdsessie die eerst met de eigenaar afstemt.

- **Lens op stap 1, BLOKKEREND (twee keer).** Clingo had geen budget voor feitcreaties: een erflijn van 103 Wachters
  speelt in clingo en faalt in ref. Een keten waarin aanmaken en links elkaar afwisselen faalt in ref en speelt in
  clingo. Aanvaard. De hoofdsessie twijfelde, vroeg een tweede Fable-advies, en het besluit werd aanmaakdiepte per
  stratum; zie stap 3. Onder dat besluit spelen beide vormen in alle executors zolang de diepte ≤ 100 blijft.
- **Lens op stap 1, ADVIES (twee keer).** De nestgrens en de grondkosten staan nu in de spectekst en de README. De
  auto-focusgetuige is document R.

Eindlens (sol6, de hele diff op `4a2dd490`), vier keer BLOKKEREND:
- Aanvaard en gefixt:
  - de spectekst beloofde dat een sluiting van elke lengte in elke executor afloopt, maar het vuurplafond van Drools
    eindigt haar (`f741b4bd`);
  - de rode gevallen pinden niet waar de fout valt (`b78a8b9e`, rood op mutanten);
  - de golden met twee ketens bewees haar 200 aanmaken niet (`a84b7426`, rood op een variant).
- Aanvaard, opgelost buiten de code: de nestgrens van clingo ontbrak zonder recursieve objectcreatie. De bouwer mat dat
  de aanname eronder niet klopt; het bestond al op main. README en commentaar zijn rechtgezet (`d6fbe084`); de rest gaat
  naar de eigen kaart `20261009-clingo-uitvoer-breekt-af-op-lengte`.

Herkeuring van de fixronde (sol6): vondst 2 tot en met 4 zijn hersteld. Vondst 1 blijft als fout op geldige invoer
staan: de keten van 187 niet-recursieve creaties breekt in clingo af waar ref speelt. Afgewezen voor deze kaart, met
reden: het is een bestaande fout op main, die niet van deze kaart afhangt, en ze heeft een eigen kaart
(`20261009-clingo-uitvoer-breekt-af-op-lengte`, main `8aa05068`). De README is er eerlijk over, en de spectekst beschrijft
de benoemde weigering alleen voor documenten met een recursieve objectcreatie.

### Open punten (voor de eigenaar)

1. **Het besluit zelf** (advies 2 in `/tmp/lens-bugs/advies2-fable.md`).
   - Voorgestelde CONSTRUCTS-rij: "**Creation budget (2026-10-09, card `creatiebudget-ref-en-clingo`):** every minted
     instance has a creation depth in its creation's stratum; a mint at depth 101 is a fault ("creation budget
     exhausted") in `ref/`, the ASP backend and the Drools truth-maintenance branch; fact creations carry no budget; the
     ASP nesting bound (190) stays a named backend limit."
   - Voorgestelde zin voor PLAN §9: "The creation budget counts the creation depth per stratum (provisional decision
     2026-10-09 on a second Fable advice, confirmed by the owner on …)."
2. **De nestgrens van clingo** (190) rust op een verkeerd gelezen meting. De afbraak hangt van de lengte van een string
   in de JSON-uitvoer af. Herzien op de kaart `20261009-clingo-uitvoer-breekt-af-op-lengte`. Ref faalt niet op een lange
   keten van niet-recursieve creaties: de diepte begint per stratum opnieuw (gemeten: 200 en 201 spelen).
3. **Clingo-README.** "A single chain of 100 takes well under a second" geldt niet met een som met terugval: synth2 duurt
   ongeveer 20 s, de wachtersketen 16 tot 18 s per case. Dat was al zo op main.
4. **Gate 10** controleert alleen `corpus/golden`. De `bron.rgs` van een rood geval houdt dus geen gate bij zijn
   `ir.json`.
5. **Landing.** Deze kaart en de kaart eronder landen samen. Op een proefstapel (main plus W3, W2, W1 en deze kaart,
   cherry-picks; het enige conflict waren de pins in `scripts/check-drools.sh`, opgelost naar 242 en 196 met beide
   commentaarblokken) zijn alle 16 gates groen (`bf1d3dec` in `wt/proefstapel`: 805 cases, 49 rode gevallen). Bij de
   landing van deze kaart na `20261009-drools-filterfout-buiten-de-lezende-regel` dus hetzelfde conflict, met dezelfde
   oplossing.
