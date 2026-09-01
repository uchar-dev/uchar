# Eenheden op referenties: de verifier kruist soort, geen eenheid

Goal: Een verifier-clean model kan op een `AttribuutRef` (en sinds de
Let-kaart ook op een `BinderRef`) een eenheid claimen die niet bij de
declaratie past — WF-078/WF-023 kruisen alleen de datatype-soort, niet
de eenheid. Lens-1-meting (2026-09-01, flankenpaar buiten het corpus):
zo'n eenheidsleugen geeft ref 550 waar Drools 500.05 zegt — een stille
executor-divergentie op een model dat gate 3 goedkeurt. De vorm is
pre-existent (de kale AttribuutRef-flank divergeert identiek, zonder
Let). Onderzoek en besluit: hoort een eenheids-consistentiecheck op
referentietypen in de WF-regels (verifier, rood-eerst met
negatief-geval), of is de declaratie-zijde de enige waarheid en moeten
de executors het ref-type negeren? Ref is scheidsrechter; de spec
beslist.

Acceptance:
- De divergentie gereproduceerd als gedocumenteerd flankenpaar
  (rood-eerst).
- Een voorstel-diff voor de WF-regel (protected path — als proposal
  achterlaten, genummerde vraag aan de maintainer) met minstens één
  passend negatief-geval; óf een gemotiveerd besluit dat de
  executor-kant moet veranderen, dan de backend-fix met witnesses.
- Alle twaalf gates groen.

May touch: `spec/verifier/**` (proposal), `corpus/negatief/**`,
`backends/drools/src/**`, `backlog/`.

Out of scope: de v1-vormen, corpuspins van bestaande goldens.
