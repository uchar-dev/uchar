# dialect-niet-nesting-goldens: de RegelSpraak-tweelingen van drie handankers

*Status: concept, 23 september 2026 — wacht op de besluiten 1–8 van
spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md; volgt op
anker-ww-herleving-hoeken en dialect-niet-nesting-frontend.*

Doel: de goldens G2, G3 en G4 van voorstel §8.2–8.3, elk gebouwd met de
penningrechtmethode (`spec/proposals/PHASE5D-DIFF.md` §2–3): een
`bron.rgs`, de door de frontend geëmitteerde en door de serializer van gate
2 gecanoniseerde `ir.json`, dezelfde cases met dezelfde absolute
verwachtingen als het handanker, en een NOTE met de structurele diff
(elke `bron` gestript, sleutels gesorteerd) en de triage (i)
dialectverschil, (ii) IR-ontwerpfout, (iii) frontendfout.

- **G2** `ww-herleving-inkomen-rgs` (naam open) — de tweeling van het
  herstelde `anchor/ww-herleving-inkomen`, zeven cases; bron in voorstel
  §8.2. Het vereiste golden van de onderzoekskaart.
- **G3** `jutterswet-uitsluiting-rgs` — art. 4 Jutterswet, de bijvoeglijke
  ontkenning en één nestniveau; partner `anchor/jutterswet-uitsluiting`.
- **G4** `jutterswet-nestkenmerk-rgs` — ongeordende uitzonderingen op een
  basis, één ervan negatief, met het geneste kenmerkconflict; partner
  `anchor/jutterswet-nestkenmerk`.

## Acceptatie (bij oppakken aanscherpen)

- (a) De voorspelde diff van voorstel §8.2 (tabel) en §8.3 komt uit; elk
  item krijgt een triageregel in de NOTE. Een (ii)- of (iii)-item stopt de
  kaart en gaat terug naar de maintainer.
- (b) Gates 2–7, 10, 12 en 13 groen. Gate 12: `EXPECTED_PLAYED` +3 en de
  drie namen in klasse (A) van `DRL_CHANNEL` (de DRL-teksttak heeft geen
  defaults-verlaging); gate 7: G4's `case1-y-wint` op de survivorlijst,
  met de reden van het anker. Beide scripts zijn beschermd: de wijziging
  is een voorstel op de tak.
- (c) De registerzin voor `spec/CONSTRUCTS.md` rij 8 in het Result; de
  hoofdsessie landt hem.

May touch: `corpus/golden/ww-herleving-inkomen-rgs/`,
`corpus/golden/jutterswet-uitsluiting-rgs/`,
`corpus/golden/jutterswet-nestkenmerk-rgs/`, `scripts/check-drools.sh` en
`scripts/check-noop.sh` (als voorstel), kaartverplaatsing.
