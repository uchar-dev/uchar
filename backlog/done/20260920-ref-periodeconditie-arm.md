# ref-periodeconditie-arm: de laatste bodemzin-drager onder de condities

Gemeten bij kaart `v4-verre-rol-getuige` (2026-09-20): de voormalige
restarm over conditiesoorten in `ref/lib/eval.ml` vangt sindsdien alleen
nog `Periode_conditie`, als expliciete constructor gematcht, met de
ongewijzigde bodemzin `condition kind not implemented in this card` uit
de gate-8-lijst (de redenkolom in `scripts/check-rood.sh` regel ~145 is
bij die kaart al op deze werkelijke stand gebracht). De zin beschrijft
daarmee één bekende vorm in plaats van een restklasse — precies de
toestand die de bodemzinnen-discipline wil opheffen: de cure is een
benoemde arm, nooit een geherformuleerde bodem. Clingo's evenknie
(`condition kind not translated`, vóór de v4-kaart door dezelfde verre
vorm gedeeld) verdient dezelfde meting.

## Acceptatie (bij oppakken aanscherpen)

- (a) Hermeten wat de twee OCaml-uitvoerders en beide Drools-takken
  vandaag met een scalaire periodeconditie doen; de bestaande metingen
  liggen verspreid (de WF-097-rij voor de clips, het tijdelijke derde
  hek van WF-109 voor de feitcreatie-guard, de §8.2-notitie in de
  kenmerkcheck-kaarten).
- (b) Benoemde armen in `ref` en clingo — echte semantiek of een
  weigering per vorm — waarna de 36-lijst in `scripts/check-rood.sh`
  de betrokken bodemzin(nen) verliest of hun bron/reden bijstelt
  (beschermd pad, voorstel).
- (c) Samenhang benoemen met `backlog/new/20260920-drl-periode-zink.md`
  (de DRL-tak zinkt op een compile-fout in dezelfde familie): één
  periodefamilie-kaart mag beide nemen, samen met het vallen van het
  derde WF-109-hek.

May touch: `ref/**`, `backends/clingo/**`, `scripts/check-rood.sh`
(**voorstel**), `spec/**` (**voorstel**), `corpus/**`,
kaartverplaatsing.

## Aanscherping bij oppakken (2026-09-20)

De kaart bouwt **benoemde weigerarmen**, geen semantiek. De
§8.4.3-periodevergelijking normeren is een latere, grotere kaart; wat
hier landt is per vorm een zin die zegt WAT geweigerd wordt en WAAROM,
die de constructie noemt en die interpolatievrij — dus pinbaar — is.
Het artefact dat de armen vasthoudt is een `corpus/rood`-getuige, mits
de vorm verifier-schoon en wire-bereikbaar blijkt; de hermeting
hieronder beslist dat.

## Hermeting (2026-09-20, vóór elke wijziging)

Het meetdocument is met de hand geschreven en zo klein als de vorm
toelaat: één objecttype `Vondst` met één attribuut `vondstpremie`, één
BEREKENING-regel die er 50 in schrijft, en als hele voorwaarde de
scalaire periodeconditie — `{"node":"Periode","vanaf":1 januari
2026,"tot":1 juli 2026,"totInclusief":false}`. Geen kenmerkcheck, geen
`gedurende`-clip, geen feitcreatie: precies de positie die de bestaande
metingen openlieten. De WF-097-rij dekt de drie clips (een
`gedurende`-lidkwalificatie), het derde WF-109-hek dekt de
feitcreatie-guard; dit is de vorm daarbuiten.

Geval: één `Vondst` V1, `asOfDate` 2026-03-01 — binnen het venster, dus
de handberekening zegt `vondstpremie = 50`.

| lezer | uitkomst | exit |
|---|---|---|
| verifier | `verifier: PASS` | 0 |
| `ref` | `EVALUATION FAULT: condition kind not implemented in this card` | 1 |
| clingo | `TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)` | 1 |
| Drools exec-model | `PLAYED 1` — `vondstpremie` 50 | 0 |
| Drools DRL-tekst | `PLAYED 1` — `vondstpremie` 50 | 0 |

Drie dingen die de meting toevoegt.

**Het vierkant splijt twee-twee.** Beide Drools-takken DRAGEN de
§8.4.3-periodevergelijking: ze lowereren haar naar "rekendatum binnen
[vanaf, tot)" en spelen. Tweezijdig gemeten, zodat het geen vacuüm is:
met de verwachting op 99 in plaats van 50 meldt elke tak
`FAIL … attribuut vondstpremie: verwacht 99, werkelijk 50`; buiten het
venster (`asOfDate` 2026-09-01) speelt elke tak `vondstpremie` leeg; en
met `asOfDate` null speelt elke tak hem óók leeg, want de lowering zet
`rekendatum != null` vooraan. De twee OCaml-uitvoerders weigeren —
allebei met een **bodemzin** uit de lijst van `scripts/check-rood.sh`.

**De weigering hangt niet aan de datum.** `ref` en clingo geven
dezelfde zin bij `asOfDate` 2026-03-01 (binnen), 2026-09-01 (buiten) en
null (tijdvrij). Het is dus geen ontbrekende rekendatum — beide
uitvoerders hebben er een (`env.as_of`, `ctx.case.as_of`, en beide
poorten `Geldigheid` er al mee) — maar een ongeschreven vorm.

**De speczin bestaat wél.** `RUNTIME-SEMANTICS.md` ch. 8.2 zegt dat de
periodeconditie de datum tegen een venster toetst, en `IR-SPEC.md`
ch. 4.5 draagt de rij `TimeWindow` (`from?`, `to?`, `toInclusive`;
from inclusief, to exclusief tenzij `toInclusive`; nooit allebei
afwezig). Daar staat geen bewerkt voorbeeld bij en geen uitvoerder
onder, en ch. 5.11 noteert zelf al "*ref declines the form at
evaluation*". Dat is een geregistreerde bevinding, geen opdracht van
deze kaart: de weigerarm mag de vorm niet stilzwijgend gelijkstellen
aan het `Geldigheid`-venster van ch. 3.10 (dat heeft twéé inclusieve
grenzen) en mag ook niet kiezen wat een tijdvrije sessie ervan maakt.
Daarom weigert hij — met naam.

Conclusie: de vorm is verifier-schoon en wire-bereikbaar (de
RegelSpraak-emitter schrijft hem, `corpus/golden/jutterswet-vormenkabinet`
draagt er een), dus een `corpus/rood`-getuige is de juiste artefactvorm.

Result: de laatste bodemzin onder de condities heeft geen drager meer.
`ref`'s conditie-match was al uitputtend — geen jokerarm, alleen een
zin die niets noemde — dus met de benoemde `Periode_conditie`-arm erbij
kan geen enkele invoer `condition kind not implemented in this card` nog
produceren, en die zin is uit de gate-8-lijst verwijderd (37 → 36
bodemzinnen). Clingo's evenknie bleef: `cond_atoms` eindigt in een echte
joker die de ongedragen `Predicaat`-soorten en `Regelversie` nog vangt,
dus daar kreeg de periodeconditie een eigen arm vóór de val-door en
bleef de lijstregel staan.

De twee zinnen, woordelijk. `ref`:

> a TimeWindow condition (ch. 4.5 Periode, the §8.4.3 van/tot window)
> gates a rule on the evaluation date, and no runtime clause fixes that
> gate: ch. 8.2 names the window in one sentence and carries no worked
> example, and it is NOT the ch. 3.10 validity window, whose two bounds
> are both inclusive. Declined here rather than guessed

clingo spreekt hem woordelijk na tot aan de slotzin, die de fase van
deze backend noemt: *Declined in translation rather than grounded on a
guess*. Dat is het patroon van de heelperiode-lezer en van de
WF-044-arm: één constructie, één formulering, twee fasen.

**Waarom een weigering en geen semantiek.** De kaart vroeg armen, geen
normering, en de hermeting laat zien waarom dat ook het eerlijke
antwoord is. De speczin bestaat: ch. 8.2 zegt dat de periodeconditie de
rekendatum tegen een venster toetst. Maar hij draagt geen bewerkt
voorbeeld en hij noemt de knoop "the ch. 3.10 period condition" —
terwijl ch. 3.10 het GELDIGHEIDsvenster is, met twee inclusieve
grenzen, waar deze knoop er één exclusieve draagt (ch. 4.5, tenzij
`totInclusief`). `Eval.applies` hergebruiken zou dus een weigering
inruilen voor een gok met een bekende randfout. Daar komt bij dat de
tijdvrije sessie en het temporele doel ongezegd zijn, en dat het corpus
niet kan arbitreren: beide Drools-takken spelen de vorm, beide
OCaml-uitvoerders niet, dus het vierkant splijt twee-twee en een golden
van deze vorm kan niet bestaan. Die hele toestand staat nu vast in
`corpus/rood/periodeconditie-bereikt` (`ir.json` + `case1.json` +
`NOTE.md`), en het vervolg heeft een eigen kaart:
`backlog/new/20260920-tijdvenster-conditie-normeren.md`.

**Rood-eerst en mutatieproeven.** De getuige is in commit `ee6ddd1e`
vóór de armen geland en zette gate 8 rood op precies drie punten (beide
pins mismatch, plus een rode case zonder Drools-regel). Drie mutaties
daarna, elk uitgevoerd en teruggedraaid: `ref`'s armzin terug naar de
bodemzin laat gate 8 falen op naam
(`corpus/rood/periodeconditie-bereikt/case1.json — ref did not observe
the expected fault`); clingo's arm weghalen laat de vorm terugvallen in
de joker en de pin mismatchen; en de clingo-pin naar de bodemzin zetten
laat het bodemzin-hek zelf toeslaan, met regelnummer en reden. Dat
laatste is de proef dat de gekozen zin echt nodig was en niet cosmetisch.

**Samenhang met `backlog/new/20260920-drl-periode-zink.md` — die kaart
blijft liggen, en terecht.** De zink daar zit op `GeheelPeriode`, de
§8.2-heelperiode-KWALIFICATIE op een feitcreatie-guard, waar de
DRL-tak `rsGeheelPeriode($subject.…)` in het `$doel`-patroon zet en het
hele pakket omvalt. Deze kaart gaat over `Periode`, de
§8.4.3-CONDITIE — een andere wireknoop, een andere positie, en een tak
die hem juist wél speelt. Ze delen alleen het woord "periode". Deze
kaart raakt `backends/drools/**` dan ook niet, en het derde WF-109-hek
(geen heelperiode-kwalificatie op een doel-gastheer) valt hier
evenmin: dat hek hangt aan de zink-kaart, niet aan deze. Wie beide
families in één kaart wil nemen, doet dat als bewuste bundeling.

**Buiten de kaart gemeld.** (1) `scripts/check-rood.sh` (beschermd pad,
mandaat: "May touch … **voorstel**") krijgt drie wijzigingen: de
lijstregel van `ref`'s conditie-bodemzin is verwijderd, er staat een
alinea boven de lijst die vastlegt dat de lijst KRIMPT als een zin zijn
laatste drager verliest — met deze ene, gedateerde verwijdering en haar
reden erin — en de Drools-uitsluitingslijst krijgt de regel
`periodeconditie-bereikt|NOFAULT …`, omdat beide takken de vorm spelen.
Geen telling in het script beweegt: `EXPECTED_MIN_ROOD_CASES` blijft 26
(26 ≤ 37, een ondergrens die groei niet hoeft te volgen). (2)
`corpus/golden/jutterswet-vormenkabinet/NOTE.md`: twee plekken noemden
de oude bodemzin als reproductie-instructie en werden door deze kaart
onwaar; ze citeren nu de benoemde zin en verwijzen naar de rode
getuige. (3) `spec/**` is NIET aangeraakt: er verandert geen
speculaire uitspraak — ch. 5.11 zegt nog steeds dat `ref` de vorm
weigert — en de correctie van ch. 8.2 hoort bij de normeringskaart, met
het bewerkte voorbeeld erbij. (4) `spec/CONSTRUCTS.md` en
`spec/PLAN.md` zijn niet aangeraakt; er is geen v1-delta bij, dus er is
geen rijvoorstel, alleen de nieuwe kaart hierboven.

Gates, elk los gedraaid, alle veertien groen: 1 PASS; 2 PASS (259
documenten, vloer 192); 3 PASS (259 golden schoon, 420 negatief
geweigerd voor hun eigen regel, vloer 350); 4 PASS (601 casebestanden);
5 PASS (726 bewerkte voorbeelden, vloer 489; 564 cases exact, vloer
427); 6 PASS (457 suitechecks, vloer 83; 564 cases); 7 PASS (528 cases
weerleggen de nietsdoener, 36 overlevers); 8 PASS (37 rode cases, vloer
26; 74 fragmenten citeren geen van de 36 bodemzinnen; Drools-been 9
fout-gepind, 28 niet-gedragen met reden); 9 PASS (201 checks, 12
catala-goldens); 10 PASS (337 tests, 64 rgs-goldens); 11 PASS (29/29 in
ref, clingo en Drools); 12 PASS (played 169, DRL-tekst 146, JUnit 509
tests, vloer 239); 13 PASS (131 verdraaiingen over 27 goldens, vloer
74; 128 replays, 25 gespeeld door de DRL-teksttak, vloer 13); 14 PASS
(130 checks, 2 flogic-goldens).

---

## Fixronde 2026-09-20 (lensvondst, BLOKKEREND): de weigerreden was onwaar

De lens heeft gelijk en de kaart verandert wezenlijk. `RUNTIME-SEMANTICS.md`
**ch. 3.10** ("Period conditions and de §8.4.3 clip") normeert de scalaire
periodeconditie VOLLEDIG, mét worked example:

> The rule applies iff the evaluation date lies in the window: `vanaf`
> inclusive, `tot` exclusive, "tot en met" inclusive; an absent bound is
> open; an unset evaluation date never matches; a leeg bound never matches.
> Worked example: window [2024-01-01, 2025-01-01), evaluation date
> 2024-12-31 → applies; 2025-01-01 → does not; the "tot en met 2024-12-31"
> form still applies on 2024-12-31.

Mijn armzin las "the ch. 3.10 period condition" in ch. 8.2 als IR-SPEC
ch. 3.10 (geldigheid). Dat is fout: regel 15 van het document zegt "This
document's own chapters are cited as `ch. N`", en ch. 3.3 en de statustabel
van ch. 3.12 noemen geldigheid en de periodeconditie als twee dingen naast
elkaar. Er is ook geen grensspanning — IR-SPEC ch. 4.5 (`from` inclusief,
`to` exclusief tenzij `toInclusive`) zegt hetzelfde als ch. 3.10. De
"geen arbiter"-claim vervalt daarmee ook: de twee Drools-takken voeren
ch. 3.10 niet als eigen keuze uit maar letterlijk, `totInclusief` incluis.

De fix is dus **afronding, geen herformulering**: de gate wordt gebouwd.

### Handberekening vóór de eerste run (2026-09-20)

De golden is `corpus/golden/jutterswet-premievenster`: artikel 14 van de
Jutterswet, zes regels op de Vondst. Eén regel zonder voorwaarde
(`basispremie`, de controle), vier met een literale grens in elk van de
vier spellingen, en één met de EIGEN `meldatum` van de vondst als grens —
de tweede door ch. 3.10's mirror-note gedragen grensvorm. De vensters zijn
precies de vensters van het worked example.

| attribuut | waarde | venster |
|---|---|---|
| `basispremie` | 1 | geen voorwaarde (controle) |
| `vondstpremie` | 50 | `van 2024-01-01 tot 2025-01-01` (tot exclusief) |
| `naseizoenpremie` | 20 | `van 2024-01-01 tot en met 2024-12-31` |
| `overgangspremie` | 30 | `vanaf 2025-01-01` (open boven) |
| `aanlooppremie` | 10 | `tot 2024-01-01` (open onder) |
| `bergloon` | 5 | `vanaf de meldatum van de Vondst` |

Twee vondsten in elk geval: V1 met `meldatum` 2024-03-01, V2 zonder
meldatum — de lege grens. De verwachtingen, uitgerekend vóór ref of clingo
het document ooit zag (`—` is leeg):

| geval | asOfDate | basis | vondst | naseizoen | overgang | aanloop | bergloon V1 | bergloon V2 |
|---|---|---|---|---|---|---|---|---|
| `case1-binnen-venster` | 2024-06-15 | 1 | 50 | 20 | — | — | 5 | — |
| `case2-op-de-vanafgrens` | 2024-01-01 | 1 | **50** | 20 | — | — | — | — |
| `case3-laatste-dag` | 2024-12-31 | 1 | 50 | **20** | — | — | 5 | — |
| `case4-op-de-totgrens` | 2025-01-01 | 1 | **—** | — | 30 | — | 5 | — |
| `case5-voor-het-venster` | 2023-06-01 | 1 | — | — | — | 10 | — | — |
| `case6-tijdvrij` | null | 1 | — | — | — | — | — | — |

De vetgedrukte cellen zijn de zes clausules, elk op zijn eigen plek:
`vanaf` inclusief (case2, 50 ÓP de ondergrens), `tot` exclusief (case4, leeg
ÓP de bovengrens — het worked example's "2025-01-01 → does not"), "tot en
met" inclusief (case3, 20 op 2024-12-31 — het worked example's derde zin),
een afwezige grens open (`overgangspremie` in case4 en `aanlooppremie` in
case5), een ongezette evaluatiedatum matcht nooit (case6, alles leeg op
`basispremie` na), en een lege grens matcht nooit (V2's `bergloon`, in elk
geval leeg terwijl V1's in vier gevallen vuurt). `basispremie` is in elk
geval 1: hij bewijst dat het venster alleen zijn eigen regel poort, en hij
houdt de nietsdoener-poort (gate 7) eerlijk in `case6`, waar alle andere
verwachtingen leeg zijn.
