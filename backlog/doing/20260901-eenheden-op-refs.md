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
`backends/drools/src/**`, `backlog/`, `spec/IR-SPEC.md` (proposal;
maintainerbesluit 2026-09-03 — de facetmotor-clausule en de
WF-091-indexrij horen bij de regel).

Out of scope: de v1-vormen, corpuspins van bestaande goldens.

## Maintainerbesluit 2026-09-03 (na de meetfase)

De meetfase (flankenpaar gereproduceerd: 550,00 in ref/clingo tegen
500,05/0,55/50.500/550.000 in Drools op vier verifier-schone
typeposities; nul over-afwijzing over 1.023 referenties in
golden+rood+toka; WF-035 vangt structureel de onschadelijke,
niet-converteerbare helft) leidde tot een structuurkeuze in plaats van
een losse eenheidsregel:

1. **Facetmotor + WF-091-paraplu.** Eén vergelijking van het volledige
   geserialiseerde referentietype tegen de declaratie, op de negen
   armen (zes lezend via de `use_site_identity`-plekken, drie
   schrijvend via de WF-023-machinerie). Het eerste afwijkende facet
   kiest het nummer: domein (ci) → WF-089/WF-090, binder-soort →
   WF-078, elk overig facet (soort op niet-binderreferenties, eenheid,
   decimalen, geheel, teken, rest) → **WF-091** met de facetnaam in de
   melding. Een volgend facet kost geen nieuwe kaart meer. Alle 220
   bestaande negatiefs behouden hun nummer.
2. **Geen herschrijvende canonicaliser** (silent-fallbackbezwaar;
   frontend-drift moet de verifier melden, niet gate 2 helen). In
   plaats daarvan: de v1-delta — het referentietype verdwijnt van de
   wire — als gevolgde afwijking in CONSTRUCTS (hoofdsessie), en een
   eigen kleine kaart voor de ene niet-declaratie-getrouwe emitterplek.
3. **Drools blijft in deze kaart onaangeraakt**; de consumentenclausule
   (RUNTIME-SEMANTICS), het verdraai-harnas en de herschaal-werklijst
   worden een eigen kaart, net als het pinnen van de getuigeloze armen
   en het WF-007-doelgat uit de bijvangst.
