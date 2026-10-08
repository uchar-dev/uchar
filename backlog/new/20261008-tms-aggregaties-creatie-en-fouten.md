# tms-aggregaties-creatie-en-fouten: de truth-maintenance-tak draagt aggregaties en creaties, en voert een fout als logisch feit

*Vastgelegd door de hoofdsessie op 2026-10-08, na het besluit van de eigenaar
op het `Result:` van `20261007-drools-logische-afleiding` (PLAN §9,
2026-10-08): de truth-maintenance-tak wordt uitgebouwd tot vervanging van de
exec-modeltak. Dit is de eerste van vier bouwkaarten, daarna
`20261008-tms-expressievormen-en-rekendatum`,
`20261008-tms-verdeling-en-tijdlijnen` en `20261008-tms-omschakeling`. Het
zijn bouwkaarten, geen haalbaarheidsproeven. Tellingen uit de corpusdekking
van de proeftak in dat `Result:` (bij (e)); het corpus is sindsdien niet
veranderd.*

## Aanleiding

De tak (`DroolsBackend.Branch.TRUTH_MAINTENANCE`, vlag `--tms`, pakket
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`) speelt
66 goldens zonder FAIL en weigert er 226 per vorm. De grootste familie is
`Aggregation`: de eerste weigering van 63 goldens in een regel en van 2 in een
defaults-groep (`jutterswet-premiestaffel`, `jutterswet-sluitdag-gemeld`).
Objectcreatie weigert hij in 8 documenten, feitcreatie in 1. Het `Result:`
noemt dit de grootste risico's, en daarom komen ze vooraan:

- een `accumulate` moet opnieuw rekenen als een lid, of een afgeleide waarde
  van een lid, verandert, en mag daarvoor alleen lezen wat de LHS bindt;
- een logisch ingevoegd object vervalt met zijn rechtvaardiging, en
  recursieve creatie is precies de positieve kring die `Kringen` nu weigert
  (waarneming 5: tien kringen in 322 documenten, alle in documenten met
  objectcreatie);
- de tak heeft geen strata en komt langs tussenstanden die ref niet ziet, zoals
  een aantal dat nog 0 is of een som zonder de laatste term. Een fout die daar
  geworpen wordt, kan truth maintenance niet intrekken (waarneming 2).

Goal: de tak draagt aggregaties, object- en feitcreatie, en voert een
evaluatiefout als logisch feit dat pas aan het fixpunt telt; de goldens die hij
daarop weigerde, spelen of weigeren op een vorm van een volgende kaart.

## Wat de tak erbij krijgt

1. **Aggregaties** (`Aggregation`, ook in de rechtvaardiging en de consequentie
   van een default): een `accumulate` in de LHS over de leden die het pad bindt,
   met de afgeleide waarden van die leden als patronen (ontwerpeis 1 van de
   proefkaart).
2. **Fouten als logisch feit.** Een evaluatiefout, zoals een deling door een
   numerieke nul (RUNTIME-SEMANTICS ch. 1.4), wordt een logisch ingevoegd feit
   met de rechtvaardiging van de regel die hem maakt. De case meldt de fouten
   die aan het fixpunt staan, in een vaste volgorde. Een consequentie werpt
   geen evaluatiefout meer. De deponeringen op het kanaal (ch. 5.6) zijn in de
   tak al logische feiten.
3. **Objectcreatie en feitcreatie** (RUNTIME-SEMANTICS ch. 5.5): één creatie per
   regel per onderwerp; het gecreëerde object vervalt met zijn
   rechtvaardiging, met zijn ledeninitialisaties. `Kringen` laat een kring door
   die WF-045 toelaat (door een creatielid van een recursieve regelgroep),
   sleutelt op de knopen van IR-SPEC ch. 5.1 (attribuut, kenmerk, kanaal,
   populatie, relatie) en blijft een kring met een negatieve rand weigeren
   (WF-046).
4. **Het beslisblok van de creatie** (eerst de binding, dan de terugval op het
   typenpaar, de guards op de referentievelden, de weigerzinnen en
   `verbindendeFeittypen`) staat nu als privékopie in `ExecModelTranspiler` en
   in `DrlTranspiler`. De tak krijgt geen derde kopie: het blok komt één keer in
   `schema/FactModelGenerator.java`, zoals
   `20260923-drools-rolpaar-ontdubbeling` vraagt. Of de twee andere takken er
   in deze kaart al op overgaan, beslist de bouwer. Doen ze dat, dan blijven hun
   sweeps byte-gelijk en sluit die kaart mee.

## Acceptatie

- (a) Rood-eerst getuigen onder
  `backends/drools/src/test/java/dev/uchar/regelspraak/cases/`, elk gedraaid in
  elke volgorde van de betrokken regels in het model:
  - een aggregatie over een afgeleid lid dat later verandert;
  - een aggregatie in een default;
  - een tijdelijke deling door nul onder een aggregatie die aan het fixpunt
    verdwijnt, en een fout die aan het fixpunt blijft staan;
  - een idempotente niet-recursieve creatie, en een creatie waarvan de
    rechtvaardiging vervalt;
  - een recursieve creatie die WF-045 toelaat, en een kring met een negatieve
    rand die geweigerd blijft.
- (b) De corpusgetuige (`TmsCorpusWitnessTest`) heeft geen FAIL. Zijn vloer
  `EXPECTED_MIN_PLAYED` gaat naar het gemeten aantal, als bewuste verhoging in
  de commit die het bereikt. Elke golden die de tak op `Aggregation`,
  `ObjectCreation` of `FactCreation` weigerde, speelt of weigert met naam op een
  vorm van een volgende kaart; het `Result:` geeft de nieuwe telling per
  familie.
- (c) `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over
  `transpiler/tms/` geeft niets.
- (d) De exec-modeltak en de DRL-teksttak zijn ongewijzigd, op punt 4 na: gate
  12 groen met `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED` gelijk; alle gates van
  `CLAUDE.md` groen.

## Raakvlakken

- `20261008-fout-op-tussenstand` vraagt eerst een normbesluit: telt een fout op
  een tussenstand? Deze kaart wacht daar niet op. Punt 2 is nodig omdat déze
  tak tussenstanden bezoekt die ref niet ziet, en op de goldens geven ref en
  clingo al hetzelfde antwoord. Wel bepaalt dat besluit wat er wordt van twee
  weigeringen die de tak nu heeft omdat hij anders speculatief rekent: een
  deling in een default, en een deling in een initialisatie naast een andere
  schrijver (document A van die kaart). Die blijven staan tot het besluit. Het
  `Result:` geeft wat de tak met fouten als feit op de documenten A en B
  antwoordt, in beide volgordes, als meting voor die kaart.
- Waarneming 3 van de proefkaart hoort bij dezelfde vraag: een deponering op een
  tussenstand trekt de tak in, terwijl ref haar houdt (RUNTIME-SEMANTICS ch. 6.1,
  "Recorded residuals").
- `20261006-check-wf046-door-recursieve-creatie`: de frontend laat een negatieve
  rand op een kring door een recursieve creatie door; de tak weigert zo'n kring
  zelf (punt 3).
- `20261008-deling-door-leeg-onder-initialisatie`: de tak geeft daar al het
  goede antwoord; geen werk in deze kaart.

## Review

Zoals in `20261007-drools-logische-afleiding`: een lens na elke bouwstap, een
eindlens over de hele diff vóór het `Result:`, en na elke stap een doorloop van
het hele corpus (de corpusgetuige draait in de suite). In die kaart vond de
corpusdoorloop meer dan de lenzen. Een lensvondst wordt alleen overgenomen met
een concreet faalscenario op echte invoer of een getuige die het laat zien;
anders wordt ze met één zin reden afgewezen in het `Result:`.

## Omvang

Groot. De aggregatie is de grootste familie van het corpus, de creatie raakt
`Kringen` en de bonen, en fouten als feit veranderen de afloop van elke case
in de tak.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`,
`backends/drools/src/main/java/dev/uchar/regelspraak/schema/FactModelGenerator.java`
(punt 4), `cases/CaseRunner.java` en `cases/DroolsBackend.java` (de fouten aan
het fixpunt, de gecreëerde objecten bij het terugzetten), bij punt 4 de
beslisblokken in `ExecModelTranspiler` en `DrlTranspiler`, tests onder
`backends/drools/src/test/java/`, de sectie "The truth-maintenance trial branch"
in `backends/drools/README.md`, kaartverplaatsing. NB: serieel met andere
Drools-kaarten.

Out of scope: de exec-modeltak en de DRL-teksttak behalve punt 4, `scripts/`,
`corpus/`, `spec/`, de toolchain, en de vormen van de volgende kaarten.
