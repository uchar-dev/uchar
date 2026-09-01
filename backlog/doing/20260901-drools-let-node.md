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

## Resultaat (bouwgolf 2026-09-01)

Gate 12: **played 94 → 98, auto-excluded 42 → 38, JUnit 99 → 111**;
value-divergence 1 (kustwacht) en no-case 1 ongewijzigd, de overige
weigeringsregels letterlijk gelijk. De regel `4x … Expr:Let` is weg.

GEEN verschuivingen: de vier andere Let-dragende goldens weigeren
onveranderd op hun eigen eerste vorm (letbeslag → `Datatype:SumType`,
kinderlet/postenheffing → `Datatype:ListType`, kamerhuishouden-catala →
`functionDefs`) — de datatype-/sectieweigering valt in het
declaratiedeel, dus vóór `resolvedRegels`. Zo blijven ListType 13,
StructType 6, SumType 5 en functionDefs 3 exact staan.

Vorm: `Let`/`BinderRef`/`IfThenElse` laden naar een `Expr.Onopgelost`
DRAGER plus een identity-zijkanaal (`IrLoader.Loaded.letVormen`,
`loader/LetVorm.java`) — hetzelfde patroon als `rolBindingen`, want het
`Expr`-record is sealed en gevendord. De binder-omgeving rijdt mee in de
globals-map die elke `WaardeFn` al draadt: per evaluatie een KOPIE met
één extra sleuf (`"binder <naam>"`, met spatie — `Namen.ident` kan die
nooit produceren), dus zuiver functioneel, geen cel en geen ThreadLocal.
Daardoor erft de Let-body de bestaande leegwaarde-tabellen, de §4
eenheidsherwaardering en de Breuk-bewuste `rs*`-helpers ongewijzigd, en
profiteert het defaults-pad automatisch mee.

Vier generieke Expr-wandelingen leren door de drager heen kijken
(`letKinderen`): de freshness-watch (`eigenReads`), de
verre-zijde-reactiviteit (`navRefs`), de defaults-salience
(`exprLeesnamen`) en het defaults-delingshek (`bevatDeling`). Alleen de
eerste is corpus-observeerbaar gemaakt — met een eigen witness waarin
stratificatie de producent ná de consument laat vuren.
