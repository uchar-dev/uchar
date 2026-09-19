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

## Lensronde (2026-09-19) — vier blokkerende defecten, gemeten en gedicht

Twee onafhankelijke lenzen keurden de eerste stand af. Elk defect kreeg
eerst zijn falende getuige, daarna de fix.

### 1. De gastheer was statisch te omzeilen via subtype-overlap

Reproductie (uit `jutterswet-seinhijsing`, één veld erbij): declareer
`Sein` als SUBTYPE van `Kustvak`, laat `gehesen` alleen geërfd zijn,
hernoem `seinvoering`'s rol `post` naar `seinpost` (anders botst hij met
de `post`-rol die `Sein` nu van `Kustvak` erft), en lever in de case het
anker als een concrete `Sein`. **Vóór de fix gemeten:** schema PASS,
verifier PASS, `ref` PASS (2 verwachtingen), clingo PASS (2) — vier
groene lichten op een document waar `eigenaar: "Sein"` op runtime zowel
het anker als het doel aanwijst.

De kruising telde alleen bindingen met `T ⊑ O`, en een gedeclareerde
`Kustvak` is geen `Sein`, dus het anker was onzichtbaar. Populaties zijn
extensioneel en de telling moest dat ook worden. **Twee vragen nu, niet
één:** een binding BEVREDIGT de gastheer als `T ⊑ O` (elk object dat zij
kan dragen is er een) en OVERLAPT hem als het gedeclareerde
typegraafmodel een type onder allebei kent. Precies één bevredigende
binding, en geen tweede overlappende.

*Waarom niet alléén overlap, zoals de opdracht letterlijk voorstelde.*
Gemeten: alleen-overlap LAAT een gastheer toe die een strikt SUBTYPE van
het gebonden object noemt (`Strandvondst ⊑ Vondst`), en dan antwoordt
`ref` voor een `Strandvondst` en fault hij voor een gewone `Vondst` — een
oordeel dat van de case afhangt in plaats van van de wire, precies het
gat dat de regel dicht. De conjunctie is daarom strikt sterker dan de
oude regel (niets wordt nieuw toegelaten) en sterker dan
alleen-overlap. De afgewezen deelpopulatie-vorm heeft haar eigen getuige,
`wf109-gastheer-deelpopulatie`, en de spec laat expliciet open of zo'n
gastheer ooit ONWAAR zou moeten lezen in plaats van geweigerd te worden.
**Dit is de enige plek waar ik van de letterlijke opdracht afwijk; de
bedoeling — het gat dichten — is gevolgd.**

### 2. Beide Drools-takken kenden de extensionele lezing niet

Reproductie: `jutterswet-bakensein` — `Sein extends Baken`, `gehesen` op
`Baken`, gastheer `"Baken"`, anker `Kustvak` los daarvan. **Vóór de fix:**
verifier PASS, `ref` PASS (3), clingo PASS (3), exec
`EXCLUDE transpiler SKIP: feitcreatie (voorwaarde niet naar anker of doel
te lowereren)`, drl-tekst `EXCLUDE drl-transpiler MARKER` met dezelfde
reden. Twee die antwoorden, twee die weigeren — de splitsing die WF-109
juist opheft.

Beide takken vroegen `k.eigenaar() != subject`, naamidentiteit, waar de
spec `T ⊑ O` zegt. Eén gedeelde plek nu,
`transpiler/kern/Gastheer.hostOp`, die de gedeclareerde keten loopt.
Ná de fix spelen beide takken de doellezing (1), met de ankerlezing (2)
en de tussenstaplezing (0) allebei waarneembaar falend.

### 3. De periode-flank was ongehekt

Reproductie: `jutterswet-seinhijsing` met `gehesen` tijdsafhankelijk op
`Sein` en een `GeheelPeriode`-kwalificatie op de guard. **Vóór de fix
gemeten:** verifier PASS, `ref` PASS 1, clingo PASS 1, exec PLAYED 1,
drl-tekst `EXCLUDE drl compile: Unable to Analyse Expression
rsGeheelPeriode($subject.getGehesen(), "JAAR", 7, null)` — die tak
lowerde de vouw op de ANKER en liet het hele pakket zinken. In beide
bonenmodel-takken ligt de ch.-3.9-vouwarm vóór de gastheertoets, dus daar
beslist het veld niets.

Kleinste eerlijke reikwijdte: een derde hek. Het doel telt alleen als de
check GEEN periode draagt — in de verifier én in `guard_on_doel`, zodat
de vorm op de nul-armen valt. De spec noteert dat dit hek valt zodra de
§8.2-familie de positie normeert.

### 4. De fail-fast bij twee aangewezen objecten was onwaar

`guard_on_doel` gaf bij twee treffers `false`, waarna de anker gewoon
gelezen werd — de stille vorm van juist de lezing die het besluit
afwijst, terwijl het commentaar het tegendeel beweerde. Er is nu een
benoemde arm ("reaches BOTH objects the fact creation binds (anker …,
doel …)"), getoetst door een ref-unittest op een geconstrueerd document;
corpus/rood kan hem niet dragen omdat een rode case verifier-schoon moet
zijn.

### Wat de vier nieuwe getuigen werkelijk zijn

Twee van de vier zijn NIEUWE ARMEN in de verifier — code die er niet was:

- `wf109-subtype-overlap` → de overlap-arm ("…is — but so can … be");
- `wf109-gastheer-deelpopulatie` → de deelpopulatie-arm ("no object bound here
  IS: only SOME objects of …").

De andere twee zijn nieuwe DOCUMENTVORMEN op armen die er al waren, en dat is
geen mindere bijdrage — het is precies wat een arm zonder getuige mist:

- `wf109-gastheer-ambigu-supertype` valt op de bestaande twee-treffers-arm, die
  de spec al bij name noemde en die alleen het zelfgetypeerde paar als getuige
  had;
- `wf109-doelgastheer-periode` valt op de bestaande nul-treffers-arm, via het
  nieuwe DERDE reikwijdtehek — het hek is nieuw, de arm niet.

### Mutatieproeven van de lensronde (uitgevoerd en teruggedraaid)

| mutatie | uitkomst | gevangen door |
| --- | --- | --- |
| overlap teruggebracht tot één richting | MUTATIE exit=1 | `wf109-subtype-overlap` passeert de verifier |
| alleen overlap tellen (satisfactie-eis weg) | MUTATIE exit=1 | `wf109-gastheer-deelpopulatie` passeert |
| periode-hek weg | MUTATIE exit=1 | `wf109-doelgastheer-periode` passeert |
| ambiguïteits-fault → `false` | MUTATIE exit=1 | `WV-5.8b-B-ambigue-gastheer-weigert` — en het blijft *faulten*, met de WF-100-zin die zegt "no other object to read", wat onwaar is: er waren er twee. Daarom toetst de getuige de ZIN en niet enkel de fault |
| `Gastheer.hostOp` terug naar naamidentiteit | MUTATIE exit=1 | `de_supertype_gastheer_leest_het_doel_in_beide_takken` (Excluded in beide takken) |

Elke herstelmeting daarna exit=0.

### Eerlijkheidsnotitie bij "het vierkant sluit" (advies f)

`ref` en clingo delen `Eval.guard_on_doel` — de gastheerbeslissing is
één stuk code met twee aanroepers, geen twee onafhankelijke
implementaties. Op déze beslissing is `ref ≡ clingo` dus geen
differentiaal. Gemeten door de helper uit te zetten:

```
ref    exit=1   EVALUATION FAULT (WF-100-zin)
clingo exit=1   (dezelfde helper, dezelfde val)
exec   exit=0   PLAYED 1
drl    exit=0   PLAYED 1
```

Beide OCaml-uitvoerders vallen samen om; wat een verkeerde
gastheerbeslissing werkelijk vangt zijn de goldens door de twee
Drools-takken (gate 12) en de verifier-negatieven (gate 3). Dit is een
bestaand patroon in deze repo — `clingo` leent meer helpers van `ref` —
maar het hoort bij deze claim genoemd te worden.

**En er is een tweede deling, één laag dieper.** `ref` leest de typegraaf
door `Ir_v0.Checks` — `subtype_of` en, sinds deze kaart,
`populations_overlap` — en dat is de bibliotheek van de VERIFIER. Ook dat
is bestaand patroon (`spec/verifier/lib` levert bewust "the same three
answers for a consumer that holds a DOCUMENT"), maar op WF-109 betekent
het dat verifier en `ref` het per constructie eens zijn over de
typegraafhelft: wie `T ⊑ O` en wie "kan allebei zijn" is, is één
definitie met drie aanroepers. Wat wél tweemaal onafhankelijk geschreven
staat is de COMBINATIETOETS — de verifier telt bindingen en rapporteert
per flank, `guard_on_doel` beslist anker/doel/fault — en de
bonenmodel-tak loopt de keten in Java, los van allebei. Dus: de
typegraafhelft is één implementatie met drie lezers, de combinatiehelft
staat er drie keer (verifier, `ref`+clingo samen, Drools). Dat is minder
differentiaal dan "vier uitvoerders" suggereert, en het staat hier omdat
de lezer dat moet kunnen wegen.

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
- **WF-109 in één zin.** De eigenaar MOET precies één object dat de
  positie bindt AANWIJZEN: precies één binding bevredigt hem (`T ⊑ O`) en
  geen tweede overlapt hem (het gedeclareerde typegraafmodel kent een type
  onder allebei). Eén object overal — het onderwerp van de omvattende
  vorm — behalve bij de guard van een §13.4.7 feitcreatie, die er twee
  bindt: de anker en het doel. Drie flanken, alle FAIL: nul aangewezen
  (de check gaat over geen object), deelpopulatie (de gastheer geldt voor
  sommige objecten en fault voor de rest), twee aangewezen (het
  zelfgetypeerde paar, elk gemeenschappelijk supertype, en elke
  subtype-tak tússen de twee). Geen voorrang verzonnen. Het doel telt
  alleen mee waar de check de HELE guard is, niet navigeert en geen
  heelperiode-kwalificatie draagt; die drie hekken dragen elk hun reden en
  hun getuige.
- **Corpus.** Drie goldens. `jutterswet-seinhijsing` (één tussenstap,
  beide polariteiten) en `jutterswet-mastsein` (twee tussenstappen) komen
  uit de frontend; beide onderscheiden de doellezing POSITIEF van de
  anker- en tussenstaplezing doordat het kenmerk op álle betrokken typen
  is gedeclareerd en elke lezing een ander getal geeft.
  `jutterswet-bakensein` is HANDGESCHREVEN IR zonder `bron.rgs` — de
  frontend tagt een guard met het eigen type van het doel en nooit met een
  voorouder, dus geen enkele bron produceert de supertype-gastheer en een
  frontend-wijziging (en dus een herpin) was niet nodig. Negen
  negatief-getuigen: `wf109-gastheer-buiten-bereik`,
  `wf109-gastheer-derde-type`, `wf109-gastheer-ambigu-zelfgetypeerd`,
  `wf109-gastheer-ambigu-supertype`, `wf109-gastheer-deelpopulatie`,
  `wf109-subtype-overlap`, `wf109-doelgastheer-samengesteld`,
  `wf109-doelgastheer-genavigeerd`, `wf109-doelgastheer-periode`.
- **Uitvoerders.** `ref` evalueert de guard per doel zodra hij een
  doel-gastheer draagt (de keten eerst, dan de filter) en is
  byte-identiek voor elke andere vorm; clingo hangt de literal aan de
  ketenvariabele, met de ankerarm ongewijzigd zodat elk ouder document
  hetzelfde programma krijgt. De `owner_hosts`-weigering blijft staan
  als fail-fast onder een document dat de verifier nu al afwijst — niet
  verzacht tot een terugval — en er is een tweede, benoemde fault bij
  gekomen voor de gastheer die béíde objecten aanwijst. In
  `backends/drools` is één ding gewijzigd, in beide takken: de
  gastheertoets is van naamidentiteit naar `T ⊑ O` gegaan, uit één
  gedeelde plek (`transpiler/kern/Gastheer`).
- **Drools-getuigen.** `GastheerObjectWitnessTest` (6 tests, additief)
  pint in BEIDE takken de doellezing, de ankerlezing, de
  tussenstap-weigering en de supertype-gastheer op hetzelfde document,
  plus waardeprobes; het meerstapsgeval zit erbij. Mutatieproeven
  uitgevoerd en teruggedraaid (zie dossier).
- **Gate-pins.** `EXPECTED_PLAYED` 161 → 163 → **164**,
  `EXPECTED_DRL_PLAYED` 138 → 140 → **141**, elke stap met zijn eigen
  redenblok in het script. De laatste stap is `jutterswet-bakensein`, de
  golden die de supertype-gastheer pint.
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
- **De drie reikwijdtehekken** (samengesteld, genavigeerd,
  heelperiode-gekwalificeerd) zijn weigeringen met reden, geen normen voor
  de eeuwigheid: elk kost een getuige in alle vier de uitvoerders om op te
  heffen, en het derde valt met de §8.2-familiekaart.
- **De deelpopulatie-gastheer** (een gastheer die een strikt subtype van
  het gebonden object noemt) is geweigerd, niet gelezen. Of zo'n gastheer
  ONWAAR zou moeten opleveren voor de objecten die hij niet dekt is een
  normvraag die niemand heeft beantwoord; weigeren houdt haar open.
- **De DRL-zink onder een heelperiode-guard** is niet gerepareerd, alleen
  omheind: `backlog/new/20260920-drl-periode-zink.md` draagt de meting en
  de keuze (bedrading gericht maken, of per construct weigeren). Dat is
  disjunct codegebied en hoort bij de §8.2-familie.

**Afwijkingen van de acceptatie.** Geen, op de rode getuige na — die is
hierboven verantwoord. De acceptatie vroeg "minstens één golden"; het
zijn er twee geworden omdat het meerstapsgeval anders alleen in een
JUnit-getuige had gestaan.

**Aangeraakte bestaande tests, met reden.** Geen enkele CHECK is
gewijzigd; wel is achterhaald COMMENTAAR bijgewerkt in
`ref/test/test_ref.ml` (ch. 5.8b), `backends/clingo/test/test_clingo.ml`
(WF-100) en `backends/drools/.../StilleGuardflankWitnessTest.java` — die
blokken beweerden dat de doel-gehoste vorm géén antwoordende uitvoerder
heeft, wat na deze kaart onwaar is.

**Bijgewerkte inventarisresten** (alle drie door de lensronde gevonden,
alle drie beweerden de oude norm): de NOTE's van
`corpus/negatief/wf100-eigenaar-verklaart-kenmerk-niet` (dat document
draagt nu twee citaties), `corpus/golden/jutterswet-keurhouder` (het
falsificatierecept gaf na deze kaart WF-109 FAIL in plaats van
"verifier-schoon, uitvoerders weigeren"; er is ook een supertype- en een
deelpopulatie-rij bij gekomen) en
`corpus/golden/jutterswet-dubbelverbinding` (beweerde nog dat alleen
Drools een doelgehoste guard draagt). Plus vier `wf109-*`-NOTE's waarvan
de geciteerde foutzin meebewoog ("is" → "can be"). En
`backlog/new/20260918-feitcreatie-doelvoorwaarde-triage.md`: de
normvraag is daar als beantwoord vastgelegd, met alleen wat werkelijk
open blijft — de doelvergelijking, de drie hekken en de
`AttribuutRef.objecttype`-spanning. De hoofdsessie hertriëert die kaart.
`spec/CONSTRUCTS.md` is NIET aangeraakt.

**Frontend.** `frontends/regelspraak` is NIET gewijzigd — ook niet in de
lensronde: de supertype-golden is bewust handgeschreven IR, precies om
die wijziging en de herpin te vermijden. Er zijn nu **63** rgs-goldens
(was 61). Geen toolchain-herpin nodig.

**Gates.** Alle veertien los gedraaid, alle veertien groen (na de
lensronde opnieuw). Kerngetallen: gate 2 250 bestanden; gate 3 250 golden
schoon / 408 negatief afgewezen; gate 4 586 cases; gate 5 686 worked
examples en 552 cases; gate 6 449 checks en 552 cases; gate 7 516 cases,
36 survivors; gate 8 34 rode cases, 68 fragmenten; gate 9 201 checks, 12
catala-goldens; gate 10 322 tests, 63 rgs-goldens; gate 11 29/29/29;
gate 12 played **164**, DRL **141**, JUnit 502; gate 13 129 twists over
25 goldens; gate 14 130 checks.

**Registervoorstel voor `spec/CONSTRUCTS.md`** (hoofdsessie landt dit
serieel; deze kaart raakt het bestand niet). De rij over
`Kenmerk.eigenaar` (ch. 2.2) draagt vandaag dat het veld een
Drools-aanhechtingshint is die in v1 verdwijnt, met WF-100 als scherpere
betekenis. Voorgestelde toevoeging aan die rij:

> **Bijgesteld 2026-09-19 (WF-109, IR-SPEC ch. 4.6 (q2)).** De
> OBJECT-helft is beslist en het veld is de GASTHEER: het noemt het
> objecttype waarop de check woont, en dat object is wat elke uitvoerder
> leest. De guard van een §13.4.7 feitcreatie is de enige conditiepositie
> met twee gebonden objecten (anker en doel); overal elders is er één. De
> aanwijzing is EXTENSIONEEL en telt twee dingen: precies één binding
> bevredigt de gastheer (`T ⊑ O`) en geen tweede overlapt hem (het
> gedeclareerde typegraafmodel kent een type onder allebei) — een
> `⊑`-telling alleen liet een subtype-tak een tweede kandidaat
> doorlaten, en overlap alleen zou een gastheer toelaten die maar voor
> een deel van de populatie geldt. Het doel telt alleen mee waar de check
> de hele guard is, niet navigeert en GEEN heelperiode-kwalificatie
> draagt; dat derde hek is tijdelijk en hoort bij de §8.2-familie.
> Daarmee is dit geen spiegelbagage meer maar betekenis die nergens
> anders op de wire staat, en de v1-delta is een HERNOEMING in plaats van
> een verwijdering (`proposals/V1-WIREMIGRATIE.md` B10, voorstel). Eerste
> goldens die de producentvorm dragen: `jutterswet-seinhijsing`,
> `jutterswet-mastsein`; de supertype-gastheer staat in
> `jutterswet-bakensein`.

Diezelfde rij eindigt vandaag op een pad dat niet meer bestaat
(`backlog/new/20260910-…`); bij het landen mag dat `backlog/done/…`
worden. Voor `spec/PLAN.md` is geen scopewijziging nodig.
