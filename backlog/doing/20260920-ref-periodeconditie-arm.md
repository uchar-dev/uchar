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
