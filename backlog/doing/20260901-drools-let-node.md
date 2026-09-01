# Drools: de Let-node dragen (fase 1 van het Let-besluit)

Goal: De Drools-backend leert de `Let`-expressienode, zodat de
Let-dragende goldens spelen. Achtergrond (onderzoek 2026-09-01, officiële
RegelSpraak-spec v2.3.0 — read-only referentie op het
uwv/regelspraak-werkbank-pad; §-verwijzingen mogen, excerpten nooit): de
Let is GEEN Catala-luxe — RegelSpraak §11 (Variabelendeel, "Daarbij
geldt:") is er de directe tegenhanger van, en onze eigen
`spec/CONSTRUCTS.md` rij 24 draagt hem als core met Origin §13.4.2.10.
Dat de backend hem nooit ziet komt doordat de eigen frontend het
variabelendeel weg-inlinet (ExpressieBouwer, substitutie per
gebruiksplek) — dát wordt fase 2 (de spec-first-kaart
20260901-let-lowering-spec-first). Deze kaart is puur backend-dekking
van de bestaande wire-vorm.

Maintainerbesluit 2026-09-01 (geratificeerd): de Let-route is gefaseerd —
eerst deze backend-kaart, daarna de frontend-lowering spec-first; de
evaluatiesemantiek wordt normatief "op gebruiksplek" (§11.1-conform), te
verankeren in fase 2. Voor DEZE kaart geldt: Drools ≡ ref/clingo over de
bestaande Let-goldens — ref is de scheidsrechter over wat de huidige
pins eisen; elke gepinde verwachting eerst door ref.

LET OP de weigeringsattributie: de loader stopt bij de eerste onbekende
vorm, dus de "4 Let-goldens" van de gate-telling zijn een ondergrens —
na deze kaart kunnen goldens die Let mét een andere v1-vorm dragen naar
de vólgende weigering doorschuiven (synth3-letbeslag → SumType,
jutterswet-kinderlet → ListType). Rapporteer de verschuivingen expliciet.

Acceptance:
- Rood-eerst: nulmeting (gate 12 = played 94 / auto-excluded 42 /
  value-divergence 1 / no-case 1, JUnit 99) en de Let-weigeringen
  getoond; erna de nieuwe telling met de verschuivingen benoemd.
- `bash scripts/check-drools.sh` → PASS; elke Let-dragende golden speelt
  tegen ongewijzigde pins óf staat reden-dragend gelijst (een échte
  divergentie → EXCLUDE_KNOWN of genummerde vraag).
- Nieuwe witnesses (nieuwe testbestanden; bestaande niet wijzigen; de
  publieke replay-API-naam niet noemen) pinnen: de binding zelf, herbruik
  van de gebonden naam, en het gedrag dat ref op de bestaande goldens
  toont — geen eigen semantiekkeuzes vooruitlopend op fase 2.
- Alle twaalf gates groen; gates 5/6 ongewijzigd.

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `scripts/check-drools.sh` (uitsluitend
entries/lijsten), `backlog/`.

Out of scope: de frontend-lowering en de emitter (fase 2), elke
spec-tekst, ref/clingo/verifier, corpusverwachtingen en -pins, de overige
v1-vormen (ListType/StructType/SumType/functionDefs/TemporalRead).
