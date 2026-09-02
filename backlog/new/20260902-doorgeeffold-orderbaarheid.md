# Doorgeeffolds over equality-only-typen: orderen of weigeren

Goal: EERSTE / LAATSTE / MINIMUM / MAXIMUM zijn in v0 ORDENENDE folds,
terwijl IR-SPEC ch. 2.4 Enumeratie en SumType equality-only verklaart.
Gemeten (lens 2 van de use-site-declaratie-identiteit-kaart): een
volstrekt trouwe `EERSTE` over een enum-attribuut is verifier-schoon en
fault in ref zodra er een tweede bijdrager is ("unordered comparison"),
terwijl hij met één bijdrager stil slaagt — een datumafhankelijke
faultroute op een verifier-schoon document. De clingo-kant is nog niet
per tak gemeten. De vraag is een WF-038-operandvraag, geen
domein-identiteitsvraag (WF-089 kruist het knooptype van de doorgeeffold
sinds 2026-09-02 wél tegen de operanden, maar zegt niets over
orderbaarheid): óf de verifier weigert een equality-only-elementtype
onder de ordenende folds (en EERSTE/LAATSTE krijgen een aparte
beslissing: die ordenen op POSITIE of tijdlijn, niet op waarde — v0
navragen), óf de spec definieert een orde (declaratievolgorde per
WF-006?) en beide executors dragen haar. Rood-eerst beide flanken meten
(één en twee bijdragers, ref én clingo, alle vier de folds) vóór de
richtingkeuze; de keuze als genummerde vraag met meting.

Acceptance:
- De meettabel (vier folds × {enum, som} × {1, 2+ bijdragers} × {ref,
  clingo}) in de kaart; daarna de genummerde richtingvraag.
- Bij weigeren: WF-regel + negatief per fold-soort + golden-getuige dat
  de regel niet over-afwijst; bij definiëren: spec-clausule met gewerkt
  voorbeeld, beide executors, absolute pins eerst door ref.
- Alle twaalf gates groen.

May touch: `spec/verifier/**` (proposal), `spec/IR-SPEC.md` (proposal),
`spec/RUNTIME-SEMANTICS.md` (proposal, alleen bij definiëren),
`corpus/negatief/**`, `corpus/golden/**`, `ref/**`, `backends/clingo/**`
(alleen bij definiëren), `backlog/`.

Out of scope: SOM/TOTAAL/AANTAL (rekenend, geen orde nodig), de
struct-tweeling (eigen kaart), WF-089 zelf.
