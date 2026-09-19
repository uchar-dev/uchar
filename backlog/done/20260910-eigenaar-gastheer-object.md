# De gastheer van een kenmerkcheck: welk OBJECT?

Goal: beslissen — met een machinaal toetsbare uitkomst — wat een
kenmerkcheck met een `eigenaar` die NIET het type van het onderwerp is,
*betekent*: over welk object gaat hij. WF-100 (geland 2026-09-10) sloot de
declaratiehelft — de genoemde eigenaar moet het kenmerk verklaren. De
objecthelft staat open en wordt vandaag in twee van de drie uitvoerders met
een weigering beantwoord.

## Wat er vandaag staat (gemeten 2026-09-10)

De producent zegt het zelf. De RegelSpraak-frontend zet dit veld op precies
één vorm: de voorwaarde van een §13.4.7 feitcreatie die een kenmerk van het
DOEL noemt. De frontend bouwt die voorwaarde eerst met het ANKER als
onderwerp, krijgt een onopgeloste marker, hérbouwt hem met het doel als
onderwerp, en tagt het resultaat met `eigenaar = doel` — met als toelichting
dat de transpiler hem dan op het `$doel`-patroon plaatst "en nooit op de
anker, ook niet wanneer de anker een gelijknamig kenmerk verklaart".

De drie uitvoerders doen daar drie verschillende dingen mee:

| uitvoerder | eigenaar = onderwerptype | eigenaar ≠ onderwerptype |
| --- | --- | --- |
| `ref` | leest het onderwerp | **weigert** (sinds 2026-09-10, ch. 5.8b) |
| ASP-backend | leest het onderwerp | **weigert** (idem, bij vertaling) |
| Drools | leest het onderwerp | plaatst de constraint op het andere patroon — in een gewone regel is er geen tweede patroon, dus valt de regel weg als transpiler-skip; in een feitcreatie is er wél een tweede patroon en leest hij daar het DOEL-object |

De weigering in de twee OCaml-uitvoerders is een *kleiner* antwoord dan een
implementatie: ze zegt dat het document niet gelezen kan worden, niet wat het
betekent. Dat is bewust zo gelaten — het alternatief was de hosting-semantiek
implementeren, en dat is deze kaart.

## De uitgangsmeting (2026-09-10, op de producentvorm zelf)

Gebouwd uit de golden `jutterswet-postwacht`: feitcreatie met anker `Kustvak`
en doel `Sein`, het kenmerk `gehesen` op het DOEL gedeclareerd, guard
`eigenaar: "Sein"` — precies wat `RuleChecker.doelVoorwaarde` emitteert. Case:
S1 draagt `gehesen`, S2 niet.

| lezer | vóór WF-100 | ná WF-100 |
| --- | --- | --- |
| verifier | `WF-034 FAIL /feitCreaties/0/voorwaarde: 'gehesen' is not a declared characteristic of 'Kustvak'` | **PASS** |
| `ref` | antwoordt `vakseinaantal = 0` (leest de ANKER, die `gehesen` nooit draagt, dus vuurt de guard nooit) | **fault** (ch. 5.8b regel 2) |
| ASP-backend | idem, `0` | **fault** (bij vertaling) |
| Drools exec-model | speelt, `vakseinaantal = 1` | ongewijzigd: speelt, `1` |
| Drools DRL-tekst | speelt, `vakseinaantal = 1` | ongewijzigd: speelt, `1` |

Twee dingen die deze meting vaststelt en die de kaart hierboven aannam:

1. **De Drools-backend leest de eigenaar-INSTANTIES werkelijk.** De uitkomst 1
   volgt uit welke `Sein`-instanties `gehesen` dragen (één van twee) en niet uit
   de anker. Beide takken, dezelfde waarde. Het is dus geen aanhechtings-*hint*
   die toevallig hetzelfde antwoord geeft; het is een andere lezing.
2. **De stand van vandaag ÍS antwoord 3 hieronder**, ongekozen. Een
   verifier-schoon document dat één uitvoerder speelt en twee weigeren bestaat
   nu — WF-100 heeft die vorm conform gemaakt en de weigering heeft hem
   onspeelbaar gehouden. De weigering is een capaciteitsgrens van `ref` en de
   ASP-backend (één object per conditie), geen verkapte "eigenaar moet het
   onderwerptype zijn"-regel: de verifier zegt daar niets over en zou zo'n
   document toelaten.

Wie deze kaart doet begint dus niet bij nul: de vorm, de case en de vijf
metingen liggen er, en het werk is de keuze plus haar artefacten.

## De vraag

Wat betekent een kenmerkcheck met een vreemde eigenaar in een **feitcreatie**?
Drie mogelijke antwoorden, elk met een prijs:

1. **De check gaat over het doel-object.** `ref` en de ASP-backend evalueren de
   feitcreatie-voorwaarde dan per DOEL in plaats van eenmalig per anker — de
   voorwaarde filtert de doelen, precies wat het bonenmodel doet. Prijs: de
   feitcreatie-evaluatie in `ref` verandert van vorm (`run_feit_creatie`
   evalueert de voorwaarde vandaag vóór de doelen bekend zijn), en de
   ASP-vertaling moet de literal op de doelvariabele zetten in plaats van op
   het onderwerp.
2. **De verifier weigert de vorm ook.** De weigering van ch. 5.8b wordt een
   WF-regel: de eigenaar MOET het type van het omvattende onderwerp zijn, zonder
   uitzondering. Dan bestaat de producentvorm niet meer op de wire — de
   frontend-route wordt afgesneden en de frontend moet iets anders emitteren of
   de guard onopgelost laten. Prijs: precies dat afsnijden, plus de meting
   hoeveel bronnen de vorm produceren; en de reparatie die WF-100 net leverde
   (de vorm werd conform) wordt weer ingetrokken, nu op een andere grond.
   *Let op: een variant met een feitcreatie-UITZONDERING — de eigenaar mag het
   doeltype zijn zodra de vorm een feitcreatie-guard is — is GEEN antwoord 2.
   Die uitzondering laat de producentvorm juist toe, en omdat `ref` en de
   ASP-backend hem dan nog steeds niet kunnen lezen, valt hij samen met
   antwoord 3: verifier-schoon, één uitvoerder die speelt, twee die weigeren.
   Wie die variant wil, kiest 3 en schrijft de uitzondering op als de vorm
   waarin 3 geldt.*
3. **De vorm blijft geweigerd in `ref`/ASP en gedragen in Drools.** De stand
   van vandaag, expliciet als besluit vastgelegd. Prijs: een verifier-schoon
   document dat één uitvoerder speelt en twee weigeren, permanent — en het
   vierkant sluit daar dus nooit.

## Wat er eerst gemeten moet worden

- Hoeveel bronnen in `corpus/` en in de pariteitszone produceren deze vorm
  werkelijk? (Vandaag: **nul** in golden/rood/negatief buiten de getuigen die
  voor de regels zelf gebouwd zijn.) Zonder producent is (2) goedkoop en (1)
  een implementatie zonder gebruiker.
- Wat doet de pariteitszone? Gemeten 2026-09-10: 30 documenten, **9**
  kenmerkchecks, **0** met een eigenaar. De toolchain-gepinde ©-casus draagt de
  vorm dus evenmin. Dat maakt (2) goedkoop en haalt het pariteitsargument onder
  (1) weg — maar het is een momentopname van de zone zoals hij nu is, niet van
  wat de producent kán maken.
- Doet de Drools-backend het in de feitcreatie-route werkelijk goed, of is de
  anker→doel-terugval een toevalstreffer die op de meerstaps-tussenstappen
  breekt?

## Acceptance (aangescherpt op het maintainersbesluit, antwoord (1))

De vraag is beantwoord; wat rest is de uitvoering van dat antwoord.

- **Spec-voorstel.** `spec/IR-SPEC.md` normeert het `eigenaar`-veld als
  GASTHEERVELD — het zegt op welk objecttype de check woont — en draagt de
  bereikbaarheidseis als MUST met eigen WF-ID, verifier-check, passende
  golden en falende negatief-case. `spec/RUNTIME-SEMANTICS.md` ch. 5.8b gaat
  van weigering naar semantiek, met een worked example met concrete
  verwachte uitkomst die verbatim in `ref/test/` staat. De wire-naam en
  -vorm blijven v0 (`eigenaar`, een objecttype-string); een v1-hernoeming is
  een voorstel in `spec/proposals/V1-WIREMIGRATIE.md`, geen wijziging hier.
- **De meettabel van 2026-09-19 is de rood-eerst-basis.** Elke rij die de
  nieuwe spec legaal verklaart wordt een verwachting; elke rij die zij
  afwijst wordt een negatief- of rood-getuige.
- **Alle vier de uitvoerders dragen de vorm.** De weigering in `ref` en de
  ASP-backend verdwijnt in DEZELFDE commit als de vervangende
  implementatie. Een gastheer die geen bereikbaar object noemt is
  verifier-werk, geen uitvoerder-weigering; zo sluit het vierkant.
- **Ten minste één golden** met absolute verwachtingen die alle vier halen,
  die het doel-gastheerpad POSITIEF onderscheidt van de ankerlezing (anker
  draagt hetzelfde kenmerk: ankerlezing 2, doellezing 1). Plus
  `jutterswet-postwacht` hermeten.
- **Drools-getuigen in beide takken** pinnen de doel-lezing tegen de nieuwe
  spec-tekst, inclusief het meerstapsgeval, met uitgevoerde en
  teruggedraaide mutatieproeven.
- **Alle veertien gates groen**, elk los gedraaid; de gate-12-pins
  herberekend en gedocumenteerd.

May touch: `spec/IR-SPEC.md` (**voorstel**), `spec/RUNTIME-SEMANTICS.md`
(**voorstel**), `spec/verifier/**` (**voorstel**),
`spec/proposals/V1-WIREMIGRATIE.md` (**voorstel**), `ref/**`,
`backends/clingo/**`, `backends/drools/**`, `corpus/**`, `scripts/`
(gate-12-pins, **voorstel**), `backlog/`.

## Out of scope

- De declaratiehelft: WF-100 (geland 2026-09-10) sluit die.
- Het clip-lid: WF-097 weigert de eigenaar daar al.
- De `periode` op de scalaire kenmerkcheck — de §8.2-familie, eigen kaart.
- **Vorm B uit het meetrapport (de doelVERGELIJKING).** Dat veld bestaat op
  de vergelijking niet en de frontend produceert de vorm niet; antwoord (1)
  generaliseert er zonder nieuw wire-veld niet heen.
- **De `AttribuutRef.objecttype`-spanning.** Een vreemd type wordt daar door
  alle lezers stil als navigatie over de afgeleide relatie gelezen — de wire
  geeft twee tegengestelde antwoorden op dezelfde intuïtie. Deze kaart
  benoemt die spanning in het spec-voorstel en lost haar niet op.


## Meetrapport 2026-09-19 (read-only meetsessie, samen met de
feitcreatie-triagekaart; besluit ligt hierna bij de maintainer)

De volledige tabel is gemeten op bdea4ff4 met eigen verifier-schone
invoeren waarin het ANKER hetzelfde kenmerk draagt als het doel, zodat
de lezing positief onderscheiden is (anker-lezing 2, doel-lezing 1):

- Vorm A (doelkenmerk als feitcreatie-guard), uniek én herhaald paar:
  verifier PASS, frontend emitteert, ref/clingo FAULT, beide
  Drools-takken spelen de DOEL-lezing (1) — ook met twee tussenstappen
  (doel 3, niet tussenstap 1 of anker 4; de guard kent maar twee
  aanhechtingsplaatsen). Zelfgetypeerd is gedegenereerd (eigenaar-string
  onderscheidt anker en doel niet; alle vier lezen het anker) en de
  frontend heeft er geen route (Onopgelost-guard).
- Vorm B (doelvergelijking): bij anker ≠ doel bestaat GEEN
  verifier-schone instantie — structureel: de afgeleide lezing valt op
  WF-045 (cyclus) of WF-053 (ambigu). Antwoord (1) generaliseert dus
  niet naar de vergelijking zonder nieuw wire-veld. Beide Drools-takken
  spelen documenten die de verifier afwijst.
- Vorm C (vreemde eigenaar in een gewone regel): verifier PASS, geen
  frontend-route, ref/clingo FAULT, beide takken EXCLUDE.

Producentmeting bdea4ff4: 651 documenten, 264 kenmerkchecks, 10 met
eigenaar, 2 vreemd (beide al-afgewezen negatief-getuigen), 0 in enige
feitcreatie-voorwaarde, toka 9/0. Kosten gemeten: (1) is voor het kale
kenmerk ~6 regels per OCaml-uitvoerder (guard na de keten per doel;
owner_hosts stopt vanzelf met falen) maar generiek vergt het een
gastheerveld dat B10/B11/B13 niet plannen en dat exec voor samengestelde
guards niet kan dragen; (2) kost nul conforme documenten maar verplaatst
de breuk naar de Onopgelost-guard, waar sinds
`20260919-exec-stille-guardflanken` **beide Drools-takken weigeren** —
geen enkele uitvoerder antwoordt de vorm dan nog; (3) is één ch.-5.8b-zin + een rood-getuige met
NOFAULT-regel (gemeten haalbaar: ref/clingo voldoen aan expectedFault,
Drools levert het NOFAULT-token).

AANBEVELING van de meetsessie (maintainersprincipes gewogen): leg (3)
nu scherp vast — het veld is een gastheerhint die alleen een
meerpatroons-uitvoerder kan honoreren; conform maar niet herspeelbaar —
mét de drie ontbrekende zinnen (derde-type-rij, Onopgelost-guard,
zelfgetypeerde degeneratie) en één rood-getuige; en zet (1) apart als
latere GENERIEKE kaart. Kies (1) niet stuksgewijs voor het kenmerk
alleen; kies (2) niet zoals geformuleerd. V1-wisselwerking: dit is P7
en blokkeert B10.

Nieuwe metingen die aparte opvolging kregen én intussen gedicht zijn
(`20260919-exec-stille-guardflanken.md`, gemeten VÓÓR die kaart): exec
speelde STIL 0 op een Onopgelost-feitcreatie-guard (een Onopgelost
verdween — de gate-8-familie heeft op die positie nog geen getuige) en
een derde-type-eigenaar spleet de twee Drools-takken (exec inert →
geteld als gespeeld met 0, drl EXCLUDE); sinds die kaart weigeren
beide takken benoemd. Los daarvan gemeten:
`AttribuutRef.objecttype` met een vreemd type wordt door ALLE lezers
stil als navigatie over de afgeleide relatie gelezen — de wire geeft
daarmee twee tegengestelde antwoorden op dezelfde intuïtie; dat hoort
bij dit besluit op tafel.

## Maintainersbesluit (2026-09-19): antwoord (1) — het gastheerveld

De maintainer kiest de GENERIEKE draagroute nu, en volgt daarmee de
aanbeveling van de meetsessie (antwoord 3) bewust niet: de vorm wordt
legaal via een gastheerveld dat zegt op welk objecttype de check
woont, en alle vier de uitvoerders leren hem. Uitvoering spec-first:
het IR-veld en de WF-100-herziening als beschermd-padvoorstel, de
meettabel van deze kaart als rood-eerst-basis, en de v1-interactie
expliciet vastgelegd — B10/B11/B13 plannen vandaag géén gastheerveld,
dus de uitvoeringskaart draagt die planbijstelling als voorstel mee.

## Uitvoeringsdossier (2026-09-19)

### Rood-eerst, gemeten vóór elke regel bestond

Op de producentvorm zelf, geëmitteerd door de frontend uit de nieuwe bron
(`corpus/golden/jutterswet-seinhijsing/bron.rgs`), vóór WF-109 en vóór de
ref/clingo-vervanging:

| lezer | uitkomst |
| --- | --- |
| verifier | `verifier: PASS` |
| `ref` | `EVALUATION FAULT: characteristic check "gehesen" is hosted on "Sein" while the subject is "Kustvak" …` |
| clingo | `TRANSLATION FAULT` — dezelfde zin |
| Drools exec | `PLAYED 1` |
| Drools drl-tekst | `PLAYED 1` |

De vijf negatief-getuigen kregen dezelfde behandeling: alle vijf
`verifier: PASS` vóór de regel, alle vijf precies één `WF-109 FAIL` erna.

### Handberekeningen, opgeschreven vóór de eerste run

**`jutterswet-seinhijsing`.** Wereld: V1 (Kustvak, gehesen) → P1 (Post,
NIET gehesen) → S1 (Sein, gehesen) en S2 (Sein, niet).

- `seinaantal` van P1 = aantal seinen van de post = |{S1, S2}| = **2**.
- `vakseinaantal` van V1: de keten bereikt {S1, S2}; de guard woont op
  `Sein`, dus hij wordt aan S1 (toegelaten) en aan S2 (afgevallen)
  gesteld → één link → **1**. Ankerlezing zou 2 geven (V1 is gehesen),
  tussenstaplezing 0 (P1 niet).
- `duisteraantal` van V1: dezelfde keten, guard ontkend → alleen S2 →
  **1**. Samen 1 + 1 = 2 = `seinaantal`.
- `consistent`: geen consistentieregel, geen tweede vuller op een
  enkelvoudige rol.

**`jutterswet-mastsein`.** Wereld: V1 (Kustvak, gehesen) → P1 (Post,
niet) → M1 (Mast, gehesen) → S1, S3 (gehesen), S2 (niet).

- `seinaantal` van M1 = |{S1, S2, S3}| = **3**.
- `vakseinaantal` van V1 = |{S1, S3}| = **2**. Anker 3, eerste
  tussenstap 0, tweede tussenstap 3 — vier lezingen, drie getallen, en
  alleen de doellezing geeft 2.

Alle getallen kwamen er zo uit; geen enkele verwachting is achteraf
bijgesteld.

### Mutatieproeven (uitgevoerd en teruggedraaid)

1. **Drools, beide takken.** `ExecModelTranspiler.leafPredicate` en
   `DrlTranspiler.conditie` het gastheerveld laten negeren →
   `mvn -o test -Dtest=GastheerObjectWitnessTest` → **MUTATIE exit=1**,
   5 van 5 getuigen falen en noemen de ankerlezing bij naam
   (`vakseinaantal: verwacht 1, werkelijk 2`;
   `verwacht 2, werkelijk 3`; `duisteraantal: verwacht 1, werkelijk 0`);
   de weigeringsgetuige meldt "Expected Excluded to be thrown, but
   nothing was thrown". Daarna `git checkout -- backends/drools/src/main/java`
   → **HERSTEL exit=0**, 5 van 5 groen.
2. **`ref`.** De doelfilter vervangen door `targets` →
   **MUTATIE-ref exit=1**, `MISMATCH: V1.vakseinaantal = 2, expected 1`
   en `V1.duisteraantal = 2, expected 1`. Teruggedraaid →
   **HERSTEL-ref exit=0**, `oracle: PASS (4 expectations)`.
3. **clingo.** `cond_doel` leeggemaakt → **MUTATIE-clingo exit=1**,
   gevangen door de eigen suite (`FAIL WF-109 gastheer is het doel:
   clingo pins the values (V1.vakseinaantal = [2], expected 1)`).
   Teruggedraaid → **HERSTEL-clingo exit=0**.
4. **verifier.** De WF-109-arm uitgeschakeld →
   **MUTATIE-verifier exit=1**, vijf regels `passed the verifier (must
   violate WF-109)`. Teruggedraaid → **HERSTEL-verifier exit=0**.

### Meting over de zones (vóór de eigen getuigen)

646 documenten (golden 213, negatief 399, rood 34), 260 kenmerkchecks,
10 met een expliciete eigenaar: acht noemen het eigen onderwerptype, één
noemt een ONGEDECLAREERD type (WF-034 (q), waarvoor WF-109 opzij stapt)
en precies één noemt een gedeclareerd type dat het onderwerp niet is —
de al-afgewezen WF-100-getuige. De pariteitszone: 30 documenten, 9
kenmerkchecks, 0 eigenaren. Ná deze kaart dragen 15 documenten het veld
(3 golden, 12 negatief) met 22 voorkomens.

## Result (2026-09-19)

Antwoord (1) uitgevoerd in de generieke vorm. Het `eigenaar`-veld is
genormeerd als GASTHEER — het zegt op welk objecttype de kenmerkcheck
woont — en alle vier de uitvoerders honoreren hem. De weigering die
`ref` en de ASP-backend uitspraken is een verifierregel geworden, zodat
elke verifier-schone vorm door alle vier gedragen wordt: het
differentiaalvierkant sluit op de vorm die de frontend zelf produceert.
De wire-naam en -vorm zijn niet aangeraakt.

**Wat er ligt.**

- **Spec-voorstel (beschermd pad, ongemerged).** `spec/IR-SPEC.md`:
  ch. 4.6 **(q2)** met **WF-109**, de WF-tabelrij, de
  `CharacteristicCheck`-knooprij, en een ch. 3.4-alinea die zegt dat de
  feitcreatie-guard de enige positie met twee gebonden objecten is; de
  zinnen in (q1), in de WF-100-rij en in ch. 4.5/4.6 die het besluit
  achterhaalt zijn herschreven in plaats van blijven staan.
  `spec/RUNTIME-SEMANTICS.md`: ch. 5.8b van weigering naar semantiek
  (regel 2 herschreven, regel 3 vervangen), ch. 5.5 uitgebreid met de
  filter-lezing, plus **worked example B** met concrete verwachte
  uitkomsten — verbatim in `ref/test/test_ref.ml` en gespiegeld in
  `backends/clingo/test/test_clingo.ml`. `spec/verifier/lib/checks.ml`:
  de WF-109-arm. Aan `spec/schema/` is niets veranderd: het veld bestaat
  al en de vorm wijzigt niet.
- **WF-109 in één zin.** De eigenaar MOET een type noemen dat precies
  één object dat de positie bindt is. Eén object overal — het onderwerp
  van de omvattende vorm — behalve bij de guard van een §13.4.7
  feitcreatie, die er twee bindt: de anker en het doel. Nul → FAIL (de
  check gaat over geen object). Twee → FAIL (het zelfgetypeerde paar en
  elk gemeenschappelijk supertype; geen voorrang verzonnen). Het doel
  telt alleen mee waar de check de HELE guard is en niet navigeert; die
  twee hekken dragen hun reden en hun getuige.
- **Corpus.** Twee goldens met bron, `jutterswet-seinhijsing` (één
  tussenstap, beide polariteiten) en `jutterswet-mastsein` (twee
  tussenstappen); beide onderscheiden de doellezing POSITIEF van de
  anker- en tussenstaplezing doordat het kenmerk op álle betrokken
  typen is gedeclareerd en elk lezing een ander getal geeft. Vijf
  negatief-getuigen: `wf109-gastheer-buiten-bereik`,
  `wf109-gastheer-derde-type`, `wf109-gastheer-ambigu-zelfgetypeerd`,
  `wf109-doelgastheer-samengesteld`, `wf109-doelgastheer-genavigeerd`.
- **Uitvoerders.** `ref` evalueert de guard per doel zodra hij een
  doel-gastheer draagt (de keten eerst, dan de filter) en is
  byte-identiek voor elke andere vorm; clingo hangt de literal aan de
  ketenvariabele, met de ankerarm ongewijzigd zodat elk ouder document
  hetzelfde programma krijgt. De `owner_hosts`-weigering blijft staan
  als fail-fast onder een document dat de verifier nu al afwijst — niet
  verzacht tot een terugval. Aan `backends/drools` is geen regel
  gewijzigd: beide takken lazen het doel al.
- **Drools-getuigen.** `GastheerObjectWitnessTest` (5 tests, additief)
  pint in BEIDE takken de doellezing, de ankerlezing en de
  tussenstap-weigering op hetzelfde document, plus waardeprobes; het
  meerstapsgeval zit erbij. Mutatieproeven uitgevoerd en teruggedraaid
  (zie dossier).
- **Gate-pins.** `EXPECTED_PLAYED` 161 → **163**, `EXPECTED_DRL_PLAYED`
  138 → **140**, beide met hun redenblok in het script.
- **V1-planbijstelling.** `spec/proposals/V1-WIREMIGRATIE.md`: B10 gaat
  van "verdwijnt" naar "hernoemt", verhuist van familie C naar B en van
  kaart M2 naar M8; P7 is afgevinkt; de V14-rij is bijgesteld; de
  wisselwerking met B11 (de `AttribuutRef.objecttype`-spanning) en B13
  staat er expliciet bij. Alles gemarkeerd als VOORSTEL.

**Wat er bewust NIET ligt.**

- **Geen rode getuige.** De kaart beloofde er een voor antwoord (3); bij
  (1) bestaat de vorm niet meer waarvoor hij nodig was. Elke
  gastheer-vorm is nu óf verifier-schoon en door alle vier gedragen, óf
  verifier-afgewezen (negatief). Een verifier-schoon document dat op de
  gastheer faalt bestaat niet meer, en een rode zone-case zou dus niets
  getuigen.
- **Vorm B (de doelVERGELIJKING)** blijft buiten scope; de grens staat
  als zodanig in ch. 4.6 (q2).
- **De `AttribuutRef.objecttype`-spanning** is in (q2) als open spanning
  benoemd en niet opgelost.
- **De twee reikwijdtehekken** (samengesteld, genavigeerd) zijn
  weigeringen met reden, geen normen voor de eeuwigheid: elk kost een
  getuige in alle vier de uitvoerders om op te heffen.

**Afwijkingen van de acceptatie.** Geen, op de rode getuige na — die is
hierboven verantwoord. De acceptatie vroeg "minstens één golden"; het
zijn er twee geworden omdat het meerstapsgeval anders alleen in een
JUnit-getuige had gestaan.

**Aangeraakte bestaande tests, met reden.** Geen enkele CHECK is
gewijzigd; wel is achterhaald COMMENTAAR bijgewerkt in
`ref/test/test_ref.ml` (ch. 5.8b), `backends/clingo/test/test_clingo.ml`
(WF-100) en `backends/drools/.../StilleGuardflankWitnessTest.java` — die
blokken beweerden dat de doel-gehoste vorm géén antwoordende uitvoerder
heeft, wat na deze kaart onwaar is. Ook bijgewerkt: de NOTE's van
`corpus/negatief/wf100-eigenaar-verklaart-kenmerk-niet` (dat document
draagt nu twee citaties) en `corpus/golden/jutterswet-keurhouder` (de
alinea die zei dat de producentvorm geen golden kon worden).

**Frontend.** `frontends/regelspraak` is NIET gewijzigd. Er zijn nu
**63** rgs-goldens (was 61). Geen toolchain-herpin nodig.

**Gates.** Alle veertien los gedraaid, alle veertien groen. Kerngetallen:
gate 2 249 bestanden; gate 3 249 golden schoon / 404 negatief afgewezen;
gate 4 585 cases; gate 5 684 worked examples en 551 cases; gate 6 449
checks en 551 cases; gate 7 515 cases, 36 survivors; gate 8 34 rode
cases, 68 fragmenten; gate 9 201 checks, 12 catala-goldens; gate 10 322
tests, 63 rgs-goldens; gate 11 29/29/29; gate 12 played 163, DRL 140,
JUnit 501; gate 13 129 twists over 25 goldens; gate 14 130 checks.

**Registervoorstel voor `spec/CONSTRUCTS.md`** (hoofdsessie landt dit
serieel; deze kaart raakt het bestand niet). De rij over
`Kenmerk.eigenaar` (ch. 2.2) draagt vandaag dat het veld een
Drools-aanhechtingshint is die in v1 verdwijnt, met WF-100 als scherpere
betekenis. Voorgestelde toevoeging aan die rij:

> **Bijgesteld 2026-09-19 (WF-109, IR-SPEC ch. 4.6 (q2)).** De
> OBJECT-helft is beslist en het veld is de GASTHEER: het noemt het
> objecttype waarop de check woont, en dat object is wat elke uitvoerder
> leest. De guard van een §13.4.7 feitcreatie is de enige conditiepositie
> met twee gebonden objecten (anker en doel); overal elders is er één.
> Daarmee is dit geen spiegelbagage meer maar betekenis die nergens
> anders op de wire staat, en de v1-delta is een HERNOEMING in plaats van
> een verwijdering (`proposals/V1-WIREMIGRATIE.md` B10, voorstel). Eerste
> goldens die de producentvorm dragen: `jutterswet-seinhijsing`,
> `jutterswet-mastsein`.

Diezelfde rij eindigt vandaag op een pad dat niet meer bestaat
(`backlog/new/20260910-…`); bij het landen mag dat `backlog/done/…`
worden. Voor `spec/PLAN.md` is geen scopewijziging nodig.
