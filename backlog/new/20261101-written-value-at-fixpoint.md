# written-value-at-fixpoint: ref houdt een geschreven waarde vast als de voorwaarde later onwaar wordt

*Vastgelegd door de hoofdsessie op 2026-11-01, bij de landing van `20261008-fout-op-tussenstand`. Fable-advies van die
dag, vraag C: `/Users/yeb/Developer/uchar-dev/werk/fout-op-tussenstand/fable/advies.md`. Gemeten op die branch en op
`7c0220d11`, met dezelfde uitkomst.*

Goal: de uitkomst van ref hangt voor een scalair doel met één schrijver niet meer af van de regelvolgorde. Ch. 5.2 noemt
drie uitzonderingen op de confluentie en dit is er geen; ch. 6.1 legt het vast als residu dat op ch. 7 wachtte.

## Aanleiding

Document (`werk/fout-op-tussenstand/fable/t` en `t-rev`): "Het aandeel moet gesteld worden op 1 indien zijn teller kleiner
is dan 5" en "De teller moet gesteld worden op zijn score"; invoer teller 3, score 10.

| Uitvoerder | lezer eerst | schrijver eerst |
|---|---|---|
| ref | `MISMATCH: V1.aandeel = 1` | `PASS` (leeg) |
| clingo | leeg | leeg |
| Drools, exec-model en DRL-tekst | leeg | leeg |

Dezelfde klasse meten `TmsTimelineWitnessTest` B2 (1 van 2 volgordes fout), `TmsWholePeriodWitnessTest` A (8 van 24 volgordes
goed) en het deel `J1.gelijkpremie = 20` van A3 (3 van 6 volgordes fout). Het deel `J5.ongelijkpremie = 10` van A3 is de kaart
`20261009-ref-gehele-periode-lege-grens`.

## Geen normvraag voor een scalair doel met één schrijver

Ch. 7.8 (een regel die niet beslist, laat de waarde van de feitenbasis staan) en ch. 7.7 beslissen het. Open blijven doelen met
meerdere schrijvers (ASP afgeschermd, v0 agendavolgorde) en kenmerken (ch. 5.3 "only ever set true"; de truth-maintenance-tak trekt
ze al in): meet ze en leg ze aan Fable voor als ze meedoen.

## Besluiten Yeb (2026-11-02)

- **Kenmerk: intrekken.** Een kenmerk geldt aan het fixpunt alleen als een regel die het toekent daar beslist; ch. 5.3 "only ever set
  true" wordt aangepast.
- **Meerdere schrijvers: wat aan het fixpunt beslist.** Geen beslissende vorm: de invoerwaarde; één: diens waarde; twee met
  verschillende waarden: de fout van twee schrijvers (ch. 5.7). In elke regelvolgorde.

## Richting

Dezelfde constructie als `fout-op-tussenstand`: `World.outcome` draagt ook de schrijfactie (attribuut, waarde). Vuurt de
her-evaluatie van de vorm op het onderwerp niet, of leeg op een standing of ontsuikerd doel, en is de staande waarde van deze
vorm, dan gaat het doel terug naar de waarde van de invoerwereld (`world0`), met `changed = true`. `init_written` wordt het
algemene eigenaarschap.

Acceptance:
- (a) Rood eerst: een golden met het document hierboven, beide regelvolgordes, eigen Jutterswet-casus; ref ≡ clingo ≡ beide
  Drools-takken op absolute verwachtingen.
- (b) Elke volgorde van `TmsTimelineWitnessTest` B2 en `TmsWholePeriodWitnessTest` A haalt ref; het kenmerk en meerdere
  schrijvers gemeten, met de keuze in het Result.
- (c) De zin in ch. 6.1 over een geschreven waarde die blijft staan, en die in `spec/PLAN.md`, zeggen de nieuwe stand (voorstel).
- (d) `make gates` groen.

May touch: `ref/lib/world.ml`, `ref/lib/eval.ml`, `ref/test/`, `corpus/golden/<nieuw>/`, `spec/RUNTIME-SEMANTICS.md` (voorstel),
`scripts/check-drools.sh` (pin, voorstel), kaartverplaatsing.

Out of scope: `reading.ml`, `backends/clingo` en `backends/drools`, tenzij de golden daar iets laat zien.
