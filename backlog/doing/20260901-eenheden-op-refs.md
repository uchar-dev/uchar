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

## Bouwnotitie (2026-09-03)

Geland: de facetmotor in `use_site_identity`, twaalf wf091-negatiefs, de
IR-SPEC-clausule + WF-091-indexrij + WF-078-herformulering (voorstel).
Alle twaalf gates groen; gate 3 172 golden / 232 negatief.

Drie afwijkingen van de opdracht, met reden:

1. **Zeven armen, geen negen.** Van de drie schrijfposities draagt
   alleen het REGELDOEL een eigen geserialiseerd type (`doel` is een
   expr). Een objectcreatie-lid (`Toekenning.attribuut`) en een
   defaults-consequentie (`PrioritizedDefaults.attribuut`) NOEMEN hun
   doel — er is daar geen referentietype om tegen de declaratie te
   leggen. Wat op die twee posities kruist is de TOEWIJZING (waarde
   tegen declaratie) onder WF-023/WF-090, en dat is compatibiliteit, geen
   identiteit: identiteit eisen zou elk literal-in-attribuut afwijzen. De
   motor is dus geregistreerd op zes leesarmen + het regeldoel; de
   `wf090-schrijf-*`-negatiefs blijven onaangeraakt bij hun eigen wet.
2. **Twee extra negatiefs** buiten de opgedragen lijst: `-teken` en
   `-reststructuur`. Zonder die twee zou de clausule zeven facetten
   noemen waarvan er twee door geen artefact falsifieerbaar zijn.
3. **De facetvolgorde pint het NUMMER, niet de weigering.** Gemeten: de
   reststructuurfacet is een vangnet dat elk verschil ziet, dus het
   uitschakelen van een genoemd facet degradeert alleen de MELDING
   (`residual structure mismatch`) — het document blijft geweigerd.
   Alleen de reststructuurfacet uitschakelen laat een document passeren.
   Het nummerkeuze-deel is wél falsifieerbaar (soortfacet uit →
   `wf078-binderref-type-lie` wordt WF-091; domeinfacet uit → alle 21
   wf089/wf090-gevallen worden WF-091). Zo staat het in de indexrij.

Bijvangst voor een eigen kaart: (a) de armen zonder positieve
golden-getuige voor de eenheidsfacet — `ParameterRef`, `DimensieCel` en
de doorgeef-aggregatie (gemeten nul; de pariteitszone heeft ze wel);
(b) WF-078 defereerde tot nu toe NIET op een binder met het
onbekend-sentinel terwijl ch. 4.7 zegt dat het defereert — de motor
volgt de spec en defereert nu wel.
