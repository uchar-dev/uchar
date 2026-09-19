# subselectie-gewone-positie: draagt v2.3.0 "de Jutter die bevoegd is" buiten een vouw?

Aanleiding: de diedat-kaart vond dat een §5.6-subselectie op een
GEWONE (niet-vouw)positie — "indien de leeftijd van de Jutter die
bevoegd is groter is dan 5" — stil werd weggegooid, en heeft die
stilte dichtgezet met een ERROR die de emissie blokkeert: een
uitbreidingspunt met luide weigering, want wat de subselectie op een
scalaire navigatie BETEKENT is een spec-vraag. Maintainersopdracht
2026-09-17: die spec-vraag beantwoorden — ondersteunt RegelSpraak
v2.3.0 deze vorm? Zo ja, dan moet uchar hem aankunnen.

Fase 1 — onderzoek (het Result draagt de uitkomst ook als de bouw een
vervolgkaart wordt):

- De v2.3.0-grond: welke §5.6-/onderwerpsketenproducties dragen de
  die/dat-bijzin op een niet-vouwpositie, en met welke semantiek
  (selecteert de bijzin de INSTANTIEBINDING van het onderwerp — een
  guard op de regelinstantie — of iets anders)? Vindplaatsen in het
  Result (read-only raadplegen; niets citeren in corpusmateriaal).
- Wat doet de toolchain-lezing (de gevendorde frontend en de
  toolchain-eigen paden) vandaag met die vorm — en wat deed de
  oorspronkelijke toolchain vóór de frontend-overname (git-historie)?

Fase 2 — bij een JA: het IR-ontwerp (hoe landt de subselectie op een
gewone positie — een filter op de onderwerpsbinding? een nieuw
conditieveld? hergebruik van de vouwfilter-machinerie?) als voorstel
met WF-regels, en de bouw door de lagen die het ontwerp aanwijst.
Bij een NEE: de bestaande luide weigering wordt de gedocumenteerde
eindtoestand, met de v2.3.0-vindplaats als grond in de weigeringstekst
en in rij 40.

## Acceptatie (bij oppakken aanscherpen)

- (a) Fase 1 volledig in het Result, met vindplaatsen.
- (b) Bij JA: ontwerpvoorstel + bouw of een scherp voorstel voor een
  vervolgkaart (maintainerskeuze bij de landing); bij NEE: de
  weigeringstekst en rij 40 dragen de grond.
- (c) Alle veertien gates groen; registervoorstel in het Result.
