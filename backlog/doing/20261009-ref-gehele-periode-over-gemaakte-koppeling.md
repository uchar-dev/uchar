# ref-gehele-periode-over-gemaakte-koppeling: ref houdt de vacuüme uitkomst van vóór een creatie vast

*Gevonden op 2026-10-09 door de hoofdsessie bij het nameten van een lensvondst op stap 3 van
`20261008-tms-verdeling-en-tijdlijnen` (de vondst zelf bleek niet te kloppen) en vastgelegd volgens de afspraak dat een
bestaande bug in ref een kaart krijgt. Eerst gemeten met ref, clingo en de verifier uit `_build` van main `ac0f9008` en de
shaded jar van die kaartbranch (`58614e85`).*

*Nagemeten op `95947ae0f` (2026-11-03) met ref, clingo en de verifier uit `_build` van de worktree `wt/ref-whole-period`
en de shaded jar van die worktree, en met ref, clingo en de verifier van `ac0f90082`, `33b784e8a` (de ouder van
`a255ec161`) en `a255ec161` zelf. De documenten van `/tmp/lens-tms-k3/proef/lens4/` waren weg; ze zijn opnieuw
geschreven uit de beschrijving hieronder (`/Users/yeb/Developer/uchar-dev/werk/ref-whole-period/probe/gen_k2.py`,
uitvoer `probe/k2/`). Het probleem is op main verdwenen; zie het Result.*

## Aanleiding

Document: `corpus/golden/synth-gehelejaarkenmerk`, maar de koppeling `ligplaats` tussen Schuit en Werf komt niet uit
de invoer: een feitcreatie maakt haar uit een invoerrelatie `aanleg` (rollen `aanlegwerf`: Werf, één; `aanlegschuit`:
Schuit, meer). De regels "toeslag van een schuit" en "korting van een schuit" lezen "de werf van de Schuit gedurende
het gehele jaar (niet) bedrijvig". Verifier PASS. Vóór de creatie is er geen koppeling, en een mislukte stap naar één
geeft een null-tijdlijn die vacuüm slaagt (ch. 3.9). Na de creatie leest de regel de werf.

| Case (de casefiles van de golden, de rol als `aanlegwerf`) | ref `ac0f90082` en `33b784e8a` | ref `a255ec161` en `95947ae0f` | clingo (alle vier) | Drools, beide branches, `95947ae0f` |
|---|---|---|---|---|
| geen koppeling, geen tijdlijn, heel jaar | PASS | PASS | PASS | `PLAYED 4` |
| half jaar (verwacht: toeslag leeg, korting 8) | "MISMATCH: S1.toeslag = 8, expected leeg" | PASS | PASS | |

In beide regelvolgordes gelijk (`probe/k2/q` en `q-rev`). Met een invoerkoppeling (de golden zelf) geeft ref het goede
antwoord. Ref hield dus de uitkomst van vóór de creatie vast, waar ch. 5.5 (generaties) de regel de wereld na de creatie
laat lezen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Nagaan of het aan "gedurende het gehele" ligt of aan elke lezing langs een gemaakte koppeling (een ontbrekende
  rand in de afhankelijkheidsgraaf van ref, of de stratificatie van de creatie).
- (b) Een golden met de vorm, eigen Jutterswet-casus; ref gerepareerd, rood eerst; ref ≡ clingo ≡ de tak met truth
  maintenance.
- (c) Alle gates groen; gate 15 vergelijkt de randen van de graaf en kan de oorzaak tonen.

## Omvang

Klein tot middel.

May touch: `ref/`, `spec/verifier/` (als de graaf de rand mist; voorstel), `corpus/golden/<nieuwe golden>/`,
kaartverplaatsing.

## Result:

Gebouwd op main `95947ae0f` en gerebased op main `27cfd3dfa` in de worktree `wt/ref-whole-period` (branch
`card/ref-whole-period`), samen met
`20261009-ref-gehele-periode-lege-grens`. De kaart blijft in `doing/`; de hoofdsessie landt. Proefdocumenten, scripts en
logs onder `/Users/yeb/Developer/uchar-dev/werk/ref-whole-period/` (`probe/`, `logs/`).

### Meting op main: het probleem is weg

Op `95947ae0f` geven ref en clingo op `probe/k2/q` en `q-rev` per document 8 keer PASS en beide Drools-branches `PLAYED 4`
(`logs/step6-k2-base.log`). Op `ac0f90082` en `33b784e8a` geeft ref in beide volgordes "MISMATCH: S1.toeslag = 8,
expected leeg" op case4-half-jaar (`logs/step8-k2-base-ac0f.log`, `step8-k2-base-prewv.log`); op `a255ec161` alle
36 regels PASS (`logs/step9-k2-wv.log`). De reparatie is `a255ec161` ("ref: een schrijfactie staat alleen waar haar
vorm aan het fixpunt beslist", kaart `20261101-written-value-at-fixpoint`).

### (a) De oorzaak

Niet "gedurende het gehele", geen ontbrekende rand en niet de stratificatie: de schrijfactie die bleef staan.

- **Elke lezing langs de gemaakte koppeling die op afwezigheid geldt, had het.** Het tweede document `probe/k2/u` en
  `u-rev` leest dezelfde gemaakte koppeling zonder kwalificatie: toeslag "indien de diepgang van de werf van de Schuit
  ongelijk is aan 5", korting "… gelijk is aan 5", W1.diepgang 5 in elke case met een koppeling. Op `ac0f90082` en
  `33b784e8a` in beide volgordes "MISMATCH: S1.toeslag = 8, expected leeg" op case2, case3 en case4 (diepgang leeg
  is ongelijk aan 5, ch. 1.2, dus de regel schreef vóór de creatie); op `a255ec161` en `95947ae0f` PASS.
- **De rand is er.** `verifier --dependency-edges` op `k2/q` en `k2/u`: `positive /rules/factCreations/0/versions/0
  ligplaats van een schuit rel:ligplaats /rules/rules/0/versions/0 toeslag van een schuit`, en dezelfde naar de korting.
  Gate 15 vergelijkt randen over een relatie niet (ze worden per klasse geteld), maar de verifier en de graaf van ref
  delen `Ir_v0.Checks.dep_forms`, dus ref ziet dezelfde rand.
- **De volgorde is goed.** Een positieve rand zet creatie en regels in hetzelfde stratum; na de generatie die de
  koppeling legt, rekent `settle` de regels opnieuw uit (`Eval.evaluate`, de lus over de generaties). Dat de regels de
  koppeling daar lezen, laat `u` zien: de korting ("gelijk aan 5") kan alleen met de koppeling gelden, en is op de oude
  basis wel 8. Alleen de toeslag, door haar regel vóór de creatie geschreven op een voorwaarde die daarna onwaar werd,
  bleef staan: het residu van ch. 6.1 dat `written-value-at-fixpoint` wegnam (`World.release`).

Er is dus niets in ref of `spec/verifier/` te repareren; de reparatie van de oorzaak is `a255ec161`. De golden houdt
haar vast.

### Golden

`corpus/golden/jutterswet-gehele-jaar-gemaakte-koppeling` (`source.rgs`, `ir.json` uit de frontend, `NOTE.md`), 4 cases,
elk consistent. Een Jutter bereikt zijn Strandpost over de rol `standpost` van `standplaats`, die de feitcreatie
"standplaats van een jutter" uit de invoerrelatie `aanmelding` maakt. `wachtvergoeding` (de `dienstjaren` indien "de
standpost van de Jutter gedurende het gehele jaar bemand is"), `wachtkorting` (dezelfde onder "niet gedurende") en
`dieptegeld` ("de diepte van de standpost van de Jutter ongelijk is aan 5", de lezing zonder kwalificatie van (a)).

| case | aanmelding | `bemand` | `diepte` | vergoeding / korting / dieptegeld |
|---|---|---|---|---|
| case1-geen-aanmelding | geen | 2026 | 5 | 8 / leeg / 8 |
| case2-geen-tijdlijn | J1 bij P1 | geen | 7 | 8 / leeg / 8 |
| case3-heel-jaar | J1 bij P1 | 2026 | 5 | 8 / leeg / leeg |
| case4-half-jaar | J1 bij P1 | [2026-01-01, 2026-07-01) | 5 | leeg / 8 / leeg |

Ref, clingo en beide Drools-branches: 4 keer `oracle: PASS`, 4 keer `clingo: PASS`, `PLAYED 4` en `PLAYED 4`
(`logs/step19-goldens-now.log`); de lezingen van gate 17 zijn letterlijk gelijk.

### Rood

- Op `95947ae0f` is de golden in ref groen: het probleem is daar al weg (zie boven). Rood is ze op `33b784e8a`, de ouder
  van `a255ec161`, en op `ac0f90082` (`logs/step19-goldens-base-prewv.log`, `step19-goldens-base-ac0f.log`):
  case3-heel-jaar "MISMATCH: J1.dieptegeld = 8, expected leeg"; case4-half-jaar "MISMATCH: J1.wachtvergoeding = 8,
  expected leeg | MISMATCH: J1.dieptegeld = 8, expected leeg". Clingo en Drools zijn daar niet gemeten op de golden
  (de oude jar is er niet); clingo geeft er PASS.
- Mutatie R2 (niet gecommit, `logs/step18-mutation-R2.log`): `World.release` geeft niets meer terug (`when false &&`).
  Rood: de golden case3 en case4 met precies die twee regels, en 16 rijen `WV-6.1-written-*` van `ref/test`.

### Pins

`EXPECTED_PLAYED` 317 → 319 voor beide goldens van de branch (voorstel; zie de andere kaart). Geen andere pin of test.

### Voorgestelde tekst `spec/CONSTRUCTS.md`

Rij 8, in de alinea "**A write is read at the fixpoint (2026-11-02, …)**", achter de opsomming van de goldens: "Golden
`jutterswet-gehele-jaar-gemaakte-koppeling` (card `ref-gehele-periode-over-gemaakte-koppeling`, 2026-11-03): a rule that
decided on a link's absence before a fact creation made the link — the whole-period qualifier's vacuous pass, an
inequality against a leeg read — gives its write back at the fixpoint."

### Voorlopige besluiten

1. **Geen wijziging in ref of de verifier.** De oorzaak is gerepareerd door `a255ec161`; een eigen reparatie zou het
   symptoom dubbel dekken.
2. **De golden leest de koppeling ook zonder kwalificatie** (`dieptegeld`), zodat de corpus vastlegt wat (a) vond: het
   gaat om elke lezing die op afwezigheid geldt, niet om de kwalificatie.
3. **De koppeling komt van de Strandpost-kant** ("Een standjutter van een Strandpost is een aangemelde jutter van de
   Strandpost"), de vorm die de frontend schrijft; het proefdocument van de kaart maakte haar van de Schuit-kant. Beide
   leggen dezelfde koppeling en meten gelijk.

### Open vragen

Geen.

### Gates

Zie de andere kaart; één `make gates` voor de branch.
