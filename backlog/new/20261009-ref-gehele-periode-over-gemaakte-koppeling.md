# ref-gehele-periode-over-gemaakte-koppeling: ref houdt de vacuüme uitkomst van vóór een creatie vast

*Gevonden op 2026-10-09 door de hoofdsessie bij het nameten van een lensvondst op stap 3 van
`20261008-tms-verdeling-en-tijdlijnen` (de vondst zelf bleek niet te kloppen) en vastgelegd volgens de afspraak dat een
bestaande bug in ref een kaart krijgt. Gemeten met ref, clingo en de verifier uit `_build` van main `ac0f9008` en de
shaded jar van die kaartbranch (`58614e85`).*

## Aanleiding

Document: `corpus/golden/synth-gehelejaarkenmerk`, maar de koppeling `ligplaats` tussen Schuit en Werf komt niet uit
de invoer: een feitcreatie maakt haar uit een invoerrelatie `aanleg` (rollen `aanlegwerf`: Werf, één; `aanlegschuit`:
Schuit, meer). De regels "toeslag van een schuit" en "korting van een schuit" lezen "de werf van de Schuit gedurende
het gehele jaar (niet) bedrijvig". Verifier PASS. Vóór de creatie is er geen koppeling, en een mislukte stap naar één
geeft een null-tijdlijn die vacuüm slaagt (ch. 3.9). Na de creatie leest de regel de werf.

| Case (de casefiles van de golden, de rol als `aanlegwerf`) | ref | clingo | tak met truth maintenance |
|---|---|---|---|
| geen koppeling, geen tijdlijn, heel jaar | PASS | PASS | PASS |
| half jaar (verwacht: toeslag leeg, korting 8) | "MISMATCH: S1.toeslag = 8, expected leeg" | PASS | PASS |

In beide regelvolgordes gelijk. Met een invoerkoppeling (de golden zelf) geeft ref het goede antwoord. Ref houdt dus
de uitkomst van vóór de creatie vast, waar ch. 5.5 (generaties) de regel de wereld na de creatie laat lezen. De
documenten: `/tmp/lens-tms-k3/proef/lens4/`.

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
