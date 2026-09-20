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

**Tussenstand, INGETROKKEN (hier bewaard omdat de fixronde hem
weerlegt).** De eerste afronding van deze kaart liet het bij BENOEMDE
WEIGERINGEN: `ref` en clingo kregen elk een arm die zei dat ch. 8.2 het
venster in één zin normeert zonder bewerkt voorbeeld, dat de knoop niet
het geldigheidsvenster van IR-SPEC ch. 3.10 is, en dat de vorm daarom
geweigerd werd in plaats van gegokt. Daar stond een rode getuige onder
(`corpus/rood/periodeconditie-bereikt`) en de conclusie dat het vierkant
twee-twee spleet zonder arbiter. De lens heeft die redenering weerlegd —
zie de fixronde hieronder — en alle drie de onderdelen zijn vervangen: de
armen door de gate, de rode getuige door een golden, de conclusie door
haar tegendeel. Wat staande bleef is het uitgangspunt: de bodemzin
`condition kind not implemented in this card` had één drager, die drager
is weg, en de zin is uit de gate-8-lijst.

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

### Uitbreiding van de getuige (tweede handberekening, 2026-09-20)

Nadat de zes gevallen groen stonden bleek de golden één ding niet te
dragen dat het corpus wél kende: het venster als LID van een samengestelde
voorwaarde. Dat was de vorm waarvoor
`corpus/golden/jutterswet-vormenkabinet` de WF-042-rij hield, en zonder
hem zou de opvolger de kabinetsrij niet dekken. Toegevoegd, met de
verwachtingen opnieuw eerst uitgerekend:

`borgpremie` = 15 onder `ALLE [ de Vondst geborgen is ; van 2024-01-01 tot
2025-01-01 ]`. V1 is `geborgen`, V2 niet.

| geval | borgpremie V1 | borgpremie V2 |
|---|---|---|
| `case1-binnen-venster` | 15 | — |
| `case2-op-de-vanafgrens` | 15 | — |
| `case3-laatste-dag` | 15 | — |
| `case4-op-de-totgrens` | — | — |
| `case5-voor-het-venster` | — | — |
| `case6-tijdvrij` | — | — |

V2 is in elk geval leeg zonder dat het venster ooit gelezen wordt: `ref`
stopt een `ALLE` bij het eerste onware lid. Dat is precies de
instantieafhankelijkheid die het kabinet noteerde, nu als groene
eigenschap in plaats van als weigering. Twaalf verwachtingen per geval
werden er veertien; alle vier de uitvoerders spelen ze.

## Result (na de fixronde)

De scalaire periodeconditie is GEBOUWD, niet geweigerd. `ref` en de
ASP-backend voeren `RUNTIME-SEMANTICS.md` ch. 3.10 uit in alle zes
clausules — `vanaf` inclusief, `tot` exclusief, "tot en met" inclusief,
een afwezige grens open, een ongezette evaluatiedatum matcht nooit, een
lege grens matcht nooit — met de twee grensvormen die ch. 3.10's
v0-mirror-note draagt: een datumliteral en een eigen scalair
datumattribuut van het onderwerp. Het vierkant sluit:
`corpus/golden/jutterswet-premievenster` (zes regels, zes gevallen,
veertien verwachtingen per geval) wordt door alle vier de uitvoerders
gespeeld, `PLAYED 6` in beide Drools-takken. Het worked example van het
hoofdstuk staat verbatim in `ref/test` als `WV-3.10`, het hoofdstuk dat
tot vandaag als enige geen WV-getuige had.

Wat het NIET doet, en dat bij naam. Twee helften van ch. 3.10 blijven
liggen en weigeren nu elk met een eigen zin waarin de FASE staat, zodat
een faseverwisseling niet ongezien passeert (het WF-044-zusterpatroon):

- de grens-EXPRESSIE die WF-042 toelaat en de mirror-note uitstelt —
  `"the <zijde> bound of a period condition is neither a date literal nor
  an own scalar date attribute of the subject: ch. 3.10's mirror note
  carries those two and defers the general date expression WF-042 admits.
  Declined at evaluation"`, en in clingo woordelijk hetzelfde met
  `Declined in translation`. Rode getuige:
  `corpus/rood/periodegrens-parameterdatum`, waar alle vier de
  uitvoerders weigeren — de DRL-tak noemt de grens zelfs zelf.
- de CLIP op een tijdlijndoel (ch. 3.10's tweede geval) —
  `"a period condition on the temporal target <attr> is ch. 3.10's CLIP,
  not the evaluation-date gate: the window intersects the written
  timeline instead of deciding whether the rule applies, and that half of
  the chapter is not built. Declined at evaluation"`, idem met
  `in translation`. Die weigering zit met opzet BIJ DE REGEL en niet in
  de conditie: de gevalsonderscheiding van het hoofdstuk hangt aan het
  DOEL, en een conditie ziet er geen — zonder die plaatsing zou een
  tijdlijn stilletjes de scalaire poort krijgen, wat de verkeerde as is.
  Beide uitvoerders delen daarvoor één loopje (`Eval.bevat_periode`), zoals
  de verre rolcheck `Eval.far_side_role` deelt, zodat ze de grens van het
  geval niet verschillend kunnen leggen. Suite-gepind in beide.

Mutatieproeven, uitgevoerd en teruggedraaid. `vanaf` inclusief →
exclusief laat `case2-op-de-vanafgrens` bij naam vallen (en `case4` op de
tweede ondergrens); `tot` exclusief → inclusief laat `case4-op-de-totgrens`
vallen (en `case2` op de open-ondergrens); "tijdvrij matcht altijd" laat
`case6-tijdvrij` vallen; "lege grens is open" laat V2's `bergloon` in vijf
gevallen vallen. Het nieuwe krimphek is óók bewezen: één lijstzin in
`ref/lib/eval.ml` herschreven en gate 8 faalt met naam en reden.

Gates, elk los gedraaid, alle veertien groen: 1 PASS; 2 PASS (260
documenten, vloer 192); 3 PASS (260 golden schoon, 420 negatief, vloer
350); 4 PASS (607 casebestanden); 5 PASS (746 bewerkte voorbeelden, vloer
489; 570 cases exact, vloer 427); 6 PASS (461 suitechecks, vloer 83; 570
cases); 7 PASS (534 cases weerleggen de nietsdoener, 36 overlevers); 8
PASS (37 rode cases, vloer 26; 74 fragmenten citeren geen van de 36
bodemzinnen; Drools-been 9 fout-gepind, 28 niet-gedragen met reden); 9
PASS (201 checks, 12 catala-goldens); 10 PASS (337 tests, 64
rgs-goldens); 11 PASS (29/29); 12 PASS (played 170, DRL-tekst 147, JUnit
509 tests, vloer 239); 13 PASS (131 verdraaiingen over 27 goldens, vloer
74; 128 replays, 25 DRL-tekst); 14 PASS (130 checks, 2 flogic-goldens).

Buiten de kaart gemeld, met mandaat. (1) `scripts/check-rood.sh`
(**voorstel**): de bodemzin `condition kind not implemented in this card`
is uit de lijst (37 → 36) omdat `ref`'s conditie-match uitputtend is en
haar laatste vangst de gate werd; het krimpprotocol staat erboven; de
NOFAULT-regel van de oude rode getuige is vervangen door de
EXCLUDE-regel van de nieuwe; en het protocol kreeg zijn MACHINALE hek —
elke lijstzin moet in haar genoemde bronbestand terug te vinden zijn, met
OCaml-regelvervolg, escaped quotes én escaped BYTES genormaliseerd
(`translate.ml` schrijft ± als `\xc2\xb1`). Dat hek betrapte meteen een
echte val: zonder de byte-normalisatie meldde het de `dt_term`-zin ten
onrechte als dragerloos. (2) `scripts/check-drools.sh` (**voorstel**):
alleen de twee gelijkheidspinnen, `EXPECTED_PLAYED` 169 → **170** en
`EXPECTED_DRL_PLAYED` 146 → **147**, elk met een redenblok. Geen
kanaallijstregel erbij of eraf — de golden komt als SPELER binnen, want
deze backend hoefde niets: hij droeg §8.4.3 al. Let op bij het mergen:
de parallelle kaart `drools-enumlabel-lezer` raakt hetzelfde bestand;
mijn pinstand is gemeten op HEAD van deze branch en moet na die merge
herrekend worden als die kaart een kanaal verschuift. (3)
`spec/RUNTIME-SEMANTICS.md` (**voorstel**): ch. 3.10 noteert dat de
scalaire poort sinds 2026-09-20 door alle vier wordt uitgevoerd en dat de
clip dat niet is; de ch. 5.11-rijen zeggen niet langer dat `ref` de vorm
weigert; de statustabel van ch. 3.12 heeft drie rijen in plaats van één.
(4) `corpus/golden/jutterswet-vormenkabinet/NOTE.md`: de WF-042-rij is
sinds vandaag KLAAR OM TE VERTREKKEN — het venster voert uit, en de
opvolger bestaat. Het kabinet zelf is NIET gekrompen: de krimp van
2026-09-19 laat zien dat dat een eigen kaart is (welke declaraties een
blijvende rij nog nodig heeft, welke WF-indexcellen verschuiven), en een
halve krimp is erger dan geen.

Registervoorstel voor `spec/CONSTRUCTS.md` (niet aangeraakt; hoofdsessie
landt). Er landt nu WEL semantiek, en de rij die deze vorm draagt is
**rij 11** ("Timelines (granularity, variable starting points) +
parameter vintages") samen met de §8.4.3-noemingen in de
clip-delta-tekst. Voorgestelde toevoeging aan rij 11, achteraan:
*"**Landed (2026-09-20, card `ref-periodeconditie-arm`):** de §8.4.3
periodeconditie op een SCALAIR doel — RUNTIME-SEMANTICS ch. 3.10's
evaluatiedatum-poort — wordt uitgevoerd door alle vier de uitvoerders
(`ref` en de ASP-backend gebouwd bij deze kaart, beide Drools-takken
droegen hem al), met de zes clausules en het worked example gepind in
`corpus/golden/jutterswet-premievenster` op beide wireposities en in
`ref/test` als `WV-3.10`. Twee helften blijven tracked: de grensvorm
buiten de v0-mirror-note (rode getuige `periodegrens-parameterdatum`,
alle vier weigeren) en de CLIP op een temporeel doel (ch. 3.10's tweede
geval, in beide OCaml-uitvoerders geweigerd bij de regel en suite-gepind)
— vervolgkaart
`backlog/new/20260920-tijdvenster-conditie-normeren.md`."*

---

## Fixronde 2 (nalens, 2026-09-20): twee gaten in de as-toets en in het hek

De nalens rekende de 84 verwachtingen onafhankelijk na (alle groen) en
keurde de zes clausules, de golden, de armen en de rode getuige goed, maar
vond twee blokkerende gaten plus een half gedragen mirror-note.

**(1) Het temporele KENMERK-doel glipte door de doeltoets.** ch. 3.10
splitst op het DOEL, en een kenmerk declareert zijn tijdlijn in
`kenmerkGranulariteit` — een andere tabel dan `attribuutGranulariteit`.
Beide clip-toetsen lazen alleen de attribuuttabel. Gemeten op een
verifier-schoon document (kenmerk `geborgen`, JAAR-granulair,
kenmerktoekenning met het venster als hele voorwaarde):

| lezer | vóór de fix |
|---|---|
| clingo | **PLAYED de scalaire poort op een tijdlijn** — aanwezig binnen het venster, afwezig erbuiten |
| `ref` | faalde, maar op een andere zin ("temporal writes are not implemented") — toeval, niet de splitsing |
| Drools exec | `EXCLUDE … periodevergelijking op tijdsafhankelijk doel — §8.4.3 clip` |
| Drools DRL-tekst | `EXCLUDE … TODO periodevergelijking op een tijdsafhankelijk doel (§8.4.3)` |

Mijn eigen ch. 3.10-zin "so the scalar gate can never be applied to a
timeline by accident" was daarmee onwaar. Beide uitvoerders vragen de
kenmerktabel nu elk met een EIGEN lezer, en de spec-zin draagt nu zijn
mechanisme. Rode getuige: `corpus/rood/periodevenster-tijdlijnkenmerk`.

**(2) De tweede grens van de mirror-note ontbrak.** "…and refuse the
condition under counting quantifiers" was niet gebouwd; onder `GEEN` gaven
`ref` en clingo een lezing die nergens genormeerd is — het venster matcht
niet in een tijdvrije sessie, `GEEN` complementeert dat naar waar, en de
regel vuurt. De grens is GEMETEN en niet gekozen, één verifier-schoon
document per kwantor:

| kwantor | exec-model | DRL-tekst |
|---|---|---|
| `ALLE`, `DE` | PLAYED | PLAYED |
| `GEEN`, `TEN_MINSTE`, `TEN_HOOGSTE`, `PRECIES` | transpiler SKIP | `TODO periodevergelijking onder een tellende kwantor (§8.4.3)` |

De DRL-tak noemt de limiet dus zelf. `ref` en clingo weigeren nu diezelfde
vier bij naam; `ALLE` en `DE` blijven gedragen en `ALLE` staat groen in de
golden. Rode getuige: `corpus/rood/periodevenster-tellende-kwantor`. De
kwantorVOUW rijdt mee op dezelfde weigering zonder eigen Drools-meting —
opgeschreven, niet weggepoetst.

**(3) Het machinale hek deed niet wat zijn kop beloofde.** Drie bewezen
vals-PASS-modi, alle drie nu regel: de bronkolom werd gelezen en genegeerd
(een verzonnen bestandsnaam passeerde), er werd gezocht in de CONCATENATIE
van beide bronnen (voor de zes zinnen die in allebei staan was het hek
blind — de nalens herschreef `ref`'s "unsupported temporal-read basis
shape" en het hek zweeg), en een COMMENTAAR telde als drager. Het hek
normaliseert en zoekt nu PER BESTAND, uitsluitend in het bestand dat de
bronkolom noemt, en uitsluitend in CODE: commentaar gaat er nesting-bewust
uit, daarna de regelvervolgen, de escaped quotes en de escaped bytes. Er
staat een eigen zelftest bij, in de stijl van de `\u`-probe ernaast: één
synthetische bron met drie zinnen — één omwikkeld, één met hex-escapes,
één die alleen in commentaar staat — waarvan het hek de eerste twee moet
vinden en de derde moet missen.

**Mutatieproeven op de twee nieuwe wachten**, uitgevoerd en teruggedraaid.
De clip-toets terugzetten op de attribuuttabel laat
`periodevenster-tijdlijnkenmerk` bij naam vallen — en clingo meldt dan
`EXPECTED FAULT DID NOT OCCUR: clingo found a model`, precies de stille
as-verwisseling die de fix wegneemt. `GEEN` terugzetten in het gedragen
paar laat `periodevenster-tellende-kwantor` in beide uitvoerders vallen.
En de per-bestand-eis van het hek is bewezen op het voorbeeld van de
nalens zelf: `ref`'s "unsupported temporal-read basis shape" herschrijven
wordt nu betrapt, terwijl `translate.ml` de woorden nog draagt.

**Klein meegenomen.** `spec/IR-SPEC.md` ch. 4.6 (t) zei dat beide zinnen
bodemzinnen ZIJN; dat is sinds de krimp half onwaar en draagt nu een
gedateerde bijzin. De golden-NOTE zei 12 verwachtingen per geval; het zijn
er 14 sinds de uitbreiding. `Eval.bevat_periode` draagt een
eerlijkheidsnotitie naar het `guard_on_doel`-precedent: op de
clip-gevalsonderscheiding is `ref ≡ clingo` géén differentiaal, en juist
die gedeelde wandeling hield de NIET-gedeelde doeltoets eromheen
onzichtbaar — vandaar dat de doeltoets bewust ongedeeld blijft. En de
latente tuple-volgorde in de dagarm is niet alleen geregistreerd maar
vastgezet met een `let`; hij wordt pas scherp bij de grens-expressiekaart,
en staat daar in de vervolgkaart bij.
