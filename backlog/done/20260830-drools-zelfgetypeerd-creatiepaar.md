# Drools: het zelfgetypeerde creatiepaar (+ de Phase-D-consistentie)

Goal: Twee vormen die de objectcreatie-kaart (2026-08-30) na eerlijke
triage reden-dragend heeft uitgesloten — de uitsplitsingsclausule van die
kaart, hier opgehaald. Beide zijn nu LUIDE weigeringen die gate 12 telt;
geen van beide is een waardedivergentie.

## 1. Het zelfgetypeerde rollenpaar bij objectcreatie (de hoofdvorm)

`IrLoader.objectCreatie` (r360-366) bouwt de `ResolvedObjectCreatie`
ZONDER de symbolische `verbindendeRol` die op de draad staat — het record
(`frontends/regelspraak/.../ir/ResolvedObjectCreatie.java` r20-21) draagt
dat veld niet, en de autoriteitsparagraaf van de loader (r55-64) archiveert
zo'n binding als "emitter-afgeleid, de transpiler leidt hem opnieuw af uit
het model". Voor een ZELFGETYPEERD paar valt daar niets af te leiden:
`RegelSpraakModel.verbindendeRol` telt twee gelijkgetypeerde zusterrollen,
komt op n=2 en geeft null — waarna `ExecModelTranspiler.objectCreatie`
terecht weigert ("zelfgetypeerd rollenpaar: uit twee gelijkgetypeerde
rollen leidt de bean geen referentieveld af").

ref gaat precies andersom (`ref/lib/eval.ml` r1517): de expliciete binding
WINT van de type-paar-afleiding en is "the only route for a self-typed
pair". De IR draagt hem ook echt — alle vier de getroffen artefacten
hebben `verbindendeRol = {opvolging, opvolger}`. De backend gooit dus weg
wat de scheidsrechter als enige gebruikt.

Sluiten vergt een ONTWERPBESLUIT in `FactModelGenerator`, geen doorgeef-
patch: `leden()` leidt referentievelden per TYPEPAAR af en slaat
`ot == doelwit` over; `zelfpaarRol` dekt alleen de wederkerige vorm
(§13.3.9.2, één rol die zijn eigen tegenhanger is). Een GEWOON zelfpaar
heeft twee onderscheiden zusterrollen die allebei door hetzelfde type
gevuld worden (`voorganger` én `opvolger` op `Wachter`) — welk(e) veld(en)
de bean dan draagt, en hoe de by-pair-navigatie van de transpilers ertussen
kiest, is de open vraag. `leden()` wordt gedeeld met `ContractValidator`,
dus elk antwoord verschuift beide kanten tegelijk.

Wat het ontgrendelt:
- twee goldens: `corpus/golden/anchor/jutterswet-wachtersketen` en
  `corpus/golden/synth2-budgetgrens-onder`. **LET OP** — die dragen ELK
  nog een TWEEDE, onafhankelijke uitsluiting: `transpiler SKIP: regel:
  opvolgerrang van een wachter — regel (aggregatie buiten Phase-C scope)`.
  De creatiefix alleen zet ze dus NIET op PLAYED; beide moeten landen, of
  de kaart levert de creatievorm met een eerlijk gebleven exclusie-regel.
- twee rood-fault-pins: `corpus/rood/finite-chain-over-budget` (het
  generatiebudget wordt nooit bereikt) en `corpus/rood/runaway-recursion`
  (de weggelopen recursie evenmin) — nu statische EXCLUDE-regels in
  `scripts/check-rood.sh` (r136, r141). Ze horen dan hun échte uitkomst te
  tonen: een FAULT met gepind `expectedFault.drools`-fragment, of een
  eerlijk hergelijste reden.

## 2. De Phase-D-consistentie (de tweede uitgesplitste vorm)

`corpus/golden/anchor/jutterswet-penninguitgifte` draagt sinds
2026-08-30 een uitvoerbare objectcreatie (twee creaties, waarvan één
toekenningloos), maar blijft uitgesloten op
`transpiler SKIP: consistentie: penningcontrole — consistentie (schending
buiten Phase-D scope)`. Het gaat om twee assertievormen in één golden:
- `penningcontrole`: een `Kenmerk`-assertie ("elke Penning is
  uitgereikt"); de SCHENDING is de genegeerde kenmerklezing — een
  kenmerk-negatie in de LHS;
- `houderscontrole`: een AANTAL-aggregatie over de `houder`-rol
  vergeleken met 1 ("elke Penning heeft een houder") — een
  aggregatie-assertie.

**AMENDEMENT 2026-08-30 (lens, trap 1).** De oorspronkelijke aanname hier —
"de consistentie-lowering is het enige wat deze golden nog van PLAYED
scheidt" — is GEFALSIFIEERD. De `houderscontrole` stuit op de
§13.3.9-GEDEELDE-BEANVELD-GRENS, niet op een ontbrekende lowering:
`penningbezit` paart een to-one `houder` met een to-many `penning`, dus
draagt de relatie precies ÉÉN beanveld (`Penning.houder`) dat noodzakelijk
BEIDE richtingen dient. `case2` levert de koppeling aan de JUTTER-kant
(`J3{penningen:[P1]}`) en pint tóch `inconsistent houderscontrole @ P1` —
ref leest P1's EIGEN links onder de `houder`-sleutel leeg (ref-geverifieerd:
`oracle: PASS (5 expectations)` op de ongewijzigde pins). Dat ene veld kan
die twee lezingen niet tegelijk uitdrukken: de CaseRunner schrijft de
jutterzijdige levering juist WEL op `Penning.houder`, waarna een
presentietelling 1 zou lezen waar ref 0 leest. De assertie wordt daarom per
constructie BIJ NAAM geweigerd (gepind in
`ObjectCreatieWitnessTest.een_aantalassertie_over_een_gedeeld_veld_wordt_luid_geweigerd`);
de `penningcontrole`-arm speelt wel. De golden blijft dus uitgesloten op een
WARE reden, en sluiten vergt de OPSLAGRICHTING zelf — dezelfde restlacune
als hieronder — niet de consistentie-lowering.

Daarnaast hoort hier de rood-case `corpus/rood/ambiguous-backlink`: ref en
clingo FAULTen daar bij evaluatie ("the created object's back-link is
ambiguous"), Drools WEIGERT bij lowering ("geen uniek verbindend
feittype") en staat daarom als EXCLUDE in `scripts/check-rood.sh` (r129).
Dezelfde ontwerpvraag als hierboven, aan de rode kant: hoort een ch.-5.5-
terugkoppelingsbesluit bij lowering (een eerlijke weigering) of bij fire
(een gepinde fault)? Beantwoord die vraag één keer, voor beide zijden.

## Restlacunes (expliciet benoemd, met meting — geen van drieën gesloten)

Drie grenzen die deze kaart NIET sluit en die zonder deze paragraaf
stilzwijgend zouden blijven. Alle drie zijn pre-existent, geen regressie.

1. **De to-many-spiegelvorm (§13.3.9 opslagrichting).** Het comment bij de
   to-ONE-weigeringsarm in `ExecModelTranspiler.aggregatie` noemde het gevaar
   ("een van de verre kant geleverde koppeling leest hier als de eigen") en
   noemde de to-many-backref-arm "unchanged" — maar dat gevaar is een
   eigenschap van de VORM, niet van de to-one-rol, en geldt daar onverkort.
   Daar wordt hij WEL stil meegeteld. Gemeten op
   `corpus/golden/jutterswet-vondsttelling` (elementrol `vondst` meervoudig,
   het ene veld `Vondst.aanmelder`), levering aan de ELEMENT-kant
   (`V1{aanmelder:[J1]}`, `V2{aanmelder:[J1]}`):
   ref `J1 aantal 0, gewicht 0` — Drools `aantal 2, gewicht 3.75`.
   GEEN corpusgetuige: elke golden levert een to-many-relatie aan de
   ÉÉN-kant, waar beide executors het eens zijn, dus geen gate ziet het. Het
   comment is nu eerlijk gescoped en de meting staat als characterisation in
   `OpslagrichtingWitnessTest` (faalt zodra de lacune sluit).
   **Kandidaat-kaartpunt, bewust niet hier:** sluiten betekent bijhouden AAN
   WELKE KANT een koppeling geleverd is, en dat verschuift het opslagmodel
   onder ELKE bestaande aggregatie — een eigen afweging, geen zijdelingse
   patch.

2. **De Q4-lege-wereld-versmalling.** Een Drools-regel heeft een matchend
   feit nodig, dus in een LEGE wereld wordt de ch.-5.5-fout ("the created
   object's back-link is ambiguous") nooit bereikt, waar ref — die het
   rolpaar oplost vóór hij de subjecten loopt — óók op een lege wereld
   FAULT. Niets in het corpus hangt ervan af; gepind in
   `ObjectCreatieWitnessTest.diezelfde_ambiguiteit_zonder_subject_bereikt_de_fout_niet`,
   zodat het een besluit op de rol is en geen stille lacune.

3. **De dubbel-meervoudige zelfrelatie (nieuwe vondst 2026-08-30).**
   `anchor/jutterswet-erflijn`: het feittype `erflijn` relateert Wachter aan
   Wachter via TWEE MEERVOUDIGE rollen (`erflater`, `erfvolger`), dus leidt
   het beanmodel aan GEEN van beide uiteinden een veld af — de opslag van een
   to-many-rol is het veld van de andere kant, en hier zijn beide kanten
   to-many. De verwachting dat de zetRollen-by-name-route deze golden op
   PLAYED zou zetten is daarmee GEFALSIFIEERD: de case-INVOER wordt nu wel
   gedragen (`W1{opvolger:[W2]}` landt gericht op `Wachter.opvolger`), maar de
   aggregatie over `erfvolgers` heeft geen joinveld. Vóór deze kaart lowerde
   die join tóch en stierf bij de eerste fire ("geen getter voor 'erflater'")
   — een ongeclassificeerde ERROR die gate 12 terecht weigert te excuseren;
   nu is het een reden-dragende lowering-SKIP. **Kandidaat-kaartpunt:** de
   `feitCreatie`-arm draagt dezelfde latente vorm (`zetVeld`, `joinVeld` en
   het `tussenstap`-veld worden niet op veldbestaan getoetst). Geen artefact
   bereikt hem — de aggregatie-SKIP sluit de golden al uit — dus bewust NIET
   speculatief gehard.

## Amendement 2026-08-30 (lens 2)

**De naam-botsingspoort (het zwaarste lek van deze ronde).** De
zelfpaarvelden werden op NAAM afgeleid en op NAAM gelezen, terwijl `leden()`
elke naam door een dedup haalt (attribuut > kenmerk > referentie). Een
Wachter die óók een Numeriek attribuut `opvolger` declareert houdt dus de
PROPERTY en verliest de REFERENTIE — en beide armen schreven er een feit in.
Gemeten op een verifier-schone én ref-schone variant van
`anchor/jutterswet-wachtersketen` (`verifier: PASS`, `oracle: PASS`), jar
`71c9ad9`: de creatiearm `ERROR … ClassCastException: Cannot cast …
facts.Wachter to java.math.BigDecimal`, de case-rolarm `ERROR
RuntimeException: kon setter setOpvolger(Wachter) niet aanroepen op Wachter`
— twee ongeclassificeerde ERRORs waar main een schone EXCLUDE gaf.
`FactModelGenerator.heeftReferentieVeld` toetst nu naam ÉN type (één
afleiding, gedeeld door transpiler en CaseRunner), en creatiearm, fold-join
en `zetRollen` weigeren luid met het lid dat de referentie verdrong. Gepind
in `OpslagrichtingWitnessTest` §3, beide paden.

**De DERDE verschoven anchor: `anchor/jutterswet-gemeenteberging`.** Naast
penninguitgifte (de assertie) en erflijn (de dubbel-meervoudige zelfrelatie)
is dit de derde golden wiens uitsluitingsreden door deze kaart VAN PLAATS
verandert, en hij stond nergens benoemd. Gemeten, jar tegen jar op dezelfde
golden:

- main (`8fcce2f`): `EXCLUDE role 'aangemelde vondsten' has no uniquely
  derivable reference field` — een CASE-INVOER-weigering (`aanmelding` heeft
  twee meervoudige rollen);
- deze kaart: `EXCLUDE transpiler SKIP: regel: aantal bergende gemeenten van
  een vondst — … de enkelvoudige elementrol 'bergende gemeente' wiens
  zusterrol 'geborgen vondst' meervoudig is … (§13.3.9 opslagrichting)` — de
  GEDEELDE-VELD-aggregatieweigering.

De verschuiving is geen kosmetiek: zij dichtte een **LATENTE fire-crash**.
main droeg géén veldbestaans- of opslagrichtingspoort in `aggregatie` (de
poort komt uit `0203fd9`), dus lowerde daar een join op een veld dat de bean
niet draagt; alleen de case-invoerweigering hield hem buiten schot. Met
diezelfde golden en een case waaruit uitsluitend de geweigerde rolinvoer is
weggelaten fireert main wél, en dan: `ERROR ConstraintEvaluationException:
Error evaluating constraint ''` — precies de ongeclassificeerde ERROR die
gate 12 weigert te excuseren. Op deze kaart is het een reden-dragende
lowering-SKIP.

**NOTE-9, bewoordingscorrectie (gedaan, niet alleen genoteerd).** De
rest-weigering van `zetRollen` schreef "neither direction of the type pair
derives a unique to-one role" ook waar de by-name-afleiding juist perfect
UNIEK was en alleen de CARDINALITEIT hem stopte (`erfvolger` op de
erflijn-vorm: uniek, maar meervoudig, dus opgeslagen aan de overzijde). Niet
uniek en uniek-maar-meervoudig zijn verschillende gebreken; de weigering
meldt ze nu apart, met de rol bij naam.

Acceptance:
- Rood-eerst: de huidige EXCLUDE-regels en gate-tellingen eerst getoond
  (nulmeting bij het schrijven van deze kaart: gate 12 = played 92 /
  auto-excluded 44 / value-divergence 1 / no-case 1, JUnit 82; gate 8 =
  3 gepind / 19 gelijst), per werkstuk een falende toestand vóór en de
  groene erna.
- Elke verwachting EERST door ref (`_build/default/ref/bin/runner.exe
  <ir> <case>`); nooit een ongeverifieerde waarde pinnen.
- Het FactModelGenerator-besluit staat expliciet in de code, met de
  afgewezen alternatieven benoemd — een zelfpaarveld dat "onder-
  geïmpliceerd, nooit over-geïmpliceerd" blijft, zoals de bestaande
  afleiding zegt.
- `bash scripts/check-drools.sh` → PASS; elk getroffen artefact speelt
  tegen zijn ONGEWIJZIGDE pins óf staat reden-dragend gelijst (de
  aggregatie-SKIP op de twee goldens expliciet benoemd, niet stilzwijgend
  meegeteld).
- `sh scripts/check-rood.sh` → PASS; de drie rood-cases hertriagerd — een
  échte FAULT pint zijn `expectedFault.drools`-fragment (corpus/rood:
  uitsluitend dát veld), anders een eerlijk hergelijste EXCLUDE-reden.
- Nieuwe witnesses in `backends/drools/src/test` pinnen de zelfpaar-
  koppeling (beide uiteinden, ch. 5.5) en de twee consistentie-
  assertievormen; de bestaande weigerings-witnesses in
  `ObjectCreatieWitnessTest` verhuizen mee of vervallen mét reden.
- Alle twaalf gates groen; gates 5/6 met ongewijzigde getallen.

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `scripts/check-drools.sh` en
`scripts/check-rood.sh` (uitsluitend entries/lijsten),
`corpus/rood/*/case*.json` uitsluitend het `expectedFault.drools`-veld,
`toka/cases/parity.manifest`, `backlog/`.

Out of scope: ref/clingo/verifier/spec en alle corpusverwachtingen en
-pins, toka-ir's (byte-identiek), de v1-loadervormen (ListType/StructType/
SumType/Let/functionDefs/TemporalRead), de terugveld-degradatie (een
meervoudige subjectzijdige én een meervoudige gecreëerde rol SPELEN nu —
het schrijfbare uiteinde draagt ch. 5.5's koppeling, het meervoudige wordt
overgeslagen — beide gepind in `ObjectCreatieWitnessTest`; alleen het paar
met TWEE meervoudige rollen blijft een reden-dragende weigering ("beide
rollen zijn meervoudig", buiten scope van deze kaart)),
gate-scriptgedrag buiten de lijsten.

Result (2026-08-30): Geland op main na één bouwronde en twee seriële
adversariële lensrondes met elk een fixronde. Het zelfgetypeerde
creatiepaar landt via het zijkanaal: `IrLoader.Loaded` draagt naast model
en defaults nu `rolBindingen` (IdentityHashMap, knoop-identiteit — door
lens 2 als load-bearing bewezen), gevuld met de symbolische
`verbindendeRol` op creaties en `bronRol` op aggregaties; de vendored
frontend-slice is byte-onaangeroerd. `FactModelGenerator.zelfpaarRollen`
leidt per to-one rol één scalair veld af, by-name, met de afgewezen
alternatieven in de code; `verbindendeRol(ot,ot)` blijft bewust null.
WF-047 sluit de "wie wint"-vraag structureel (lens 2: een binding kan een
geslaagde by-pair-afleiding op verifier-schone IR nooit tegenspreken).
De by-name-route bedient ook case-invoerrollen; het gewone zelfpaar
spiegelt NIET (ref-gearbitreerd: links zijn gericht per rolsleutel).

Gate 12: 92 → 94 played / 44 → 42 auto-excluded (wachtersketen en
synth2-budgetgrens-onder spelen — creatiebinding én de gedichte
aggregatie over de gebonden rol); JUnit 82 → 99. Gate 8: 3 → 5
fault-pinned — ambiguous-backlink lowert nu naar een fire-fault met refs
bewoording (het lowering-vs-fire-antwoord: een ambigue terugkoppeling is
een programma-eigenschap, geen backend-gat; de lege-wereld-versmalling is
gepind), runaway-recursion pint "geen fixpunt" (de fire-cap, eerlijk
benoemd), finite-chain-over-budget werd reden-dragende NOFAULT (104
generaties — v0 kent geen generatiebudget; ref/clingo pinnen daar hun
eigen budgetfaults, spec-conform per lens-2-analyse). Corpusdiff:
uitsluitend twee expectedFault.drools-velden.

De §13.3.9-OPSLAGRICHTINGSWET (bijvangst, door beide lenzen
gearbitreerd): ref leest een aggregatieset als de eigen gerichte links
van het subject onder de elementrol; het beanmodel slaat per relatie één
veld op. De to-one-elementrol-arm joint nu voorwaarts (ref-conform in
beide leveringsrichtingen, acht witnesses in OpslagrichtingWitnessTest);
de to-many-spiegelarm blijft een BENOEMDE pre-existente divergentie
zonder corpusgetuige (element-kant-levering telt daar mee waar ref 0
leest) — restlacune, kandidaat-kaart. Als derde anchor verschoof
jutterswet-gemeenteberging van uitsluitingsgrond — waarbij een LATENTE
fire-crash dichtging (main lowerde een join op een veld dat de bean niet
draagt; jar-tegen-jar bewezen).

NIET geland, met de ware reden: penninguitgifte — de kaart-aanname bleek
onjuist; een to-one rol met meervoudige zusterrol deelt één beanveld
voor beide richtingen, en refs gerichte halve koppeling is daarin
onuitdrukbaar (trap-bewezen: de case levert J3→P1 en pint alsnog
houderscontrole@P1 waar het veld 1 leest). De kenmerk-negatie-assertie
speelt; de AANTAL-assertie is gebouwd voor 1:1-vormen en weigert de
gedeelde-veldvorm met precies die reden. erflijn bleek dubbel-meervoudig
(geen beanveld aan weerszijden) — de aggregatie kreeg een
veldbestaan-poort en skipt met een gescherpte §13.3.9-reden waar eerst
een latente crash wachtte. Lens 2's naam-botsingsvondst is gedicht:
`heeftReferentieVeld` is typebewust en alle drie de lezers (creatiearm,
case-rolarm, fold-join) weigeren een naamdelend veld luid
(naamdelings-precedent) waar eerst twee ongeclassificeerde ERRORs
ontstonden op verifier- en ref-schone IR.

Open kandidaten, vastgelegd: de gedeelde-veldgrens (gerichte
link-representatie naast de beans — het penninguitgifte-besluit),
consistentie-salience op berekende waarden, bronRol-precedentie
(terugval i.p.v. winnaar — WF-047 hekt het werkelijke verschil), de
to-many-spiegelarm, en de zelfgetypeerde verdeling (derde bindingdrager
op de wire, geen zijkanaal). Alle twaalf gates integraal groen op de
eindstand (160/168, 396, 374, 374, 338+36, 22+5/17, 169+11, 144+23,
toka 29/0/0, drools 94/42/1/1, JUnit 99).
