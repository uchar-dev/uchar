# Frontend-extensie: Uitzondering op — ladders uit RegelSpraak-bron

Goal: de dialectextensie van LADDER-EVOLUTIE §4 / Q1a (geratificeerd
2026-08-29), ná de omdraaiing (één bron): `Uitzondering op <regelnaam>:
<naam>`-blokken (geordend, stapelen boven de genoemde basisregel) en de
groepsvorm `Uitzonderingen op …, ongeordend` (het enige §9.5-oppervlak).
Grammatica puur additief (nieuwe topniveau-alternatieven naast
regel/regelGroep/beslistabel; inert op officiële documenten — het
regelGroep-precedent); resolver bouwt er `DefaultLadder`-knopen van
(basisregel resolven, zelfde doel afdwingen, WF-050-schrijverschecks);
emitter schrijft `defaultLadders`. De divergentie van de v2.3.0-arbiter
wordt vastgelegd in CONSTRUCTS (hoofdsessie serieel) en in een nieuw
grammar-divergences-log naast de frontend-grammatica. Herkenning van
vlakke families transformeert NOOIT — hooguit een adviserende lint.
Minstens twee nieuwe `bron.rgs`-goldens: één geordende
kenmerkuitzondering en één ongeordende groep met conflictcase — die via
de geland zijnde Drools-ladderservice ook in gate 12 SPELEN.

Acceptance: gate 10 groen met de nieuwe goldens (byte-identieke
re-emissie) én alle bestaande twintig `.rgs`-goldens byte-identiek
(additiviteit bewezen); gates 3/5/6 groen over de nieuwe goldens; gate 12
speelt ze; de toolchain-conformance blijft groen op de pin ná herpin
(hoofdsessie). Rood-eerst: een misvormd extensieblok (verkeerd doel)
weigert luid in de resolver, met test.

May touch: frontends/regelspraak/ (grammar, resolver, emit, tests, het
nieuwe divergentielog), corpus/golden/ (nieuwe rgs-goldens), backlog/.

Out of scope: CONSTRUCTS/PLAN (hoofdsessie serieel); attribuutladders uit
bron als de kenmerkvorm volstaat voor de eerste kaart (dan expliciet
gemeld); toolchain-aanpassingen buiten de herpin.

## Voortgang (2026-08-29)

Gebouwd en groen over de VOLLEDIGE poortenset 1-12.

- Grammatica puur additief: `Uitzondering op <basis>: <naam>` (geordend) en
  `Uitzonderingen ongeordend:` (groep) als nieuwe topniveau-alternatieven;
  tokens `UITZONDERING_OP` / `UITZONDERINGEN` / `ONGEORDEND`. Alle 20
  bestaande `.rgs`-goldens re-emitteren byte-identiek (additiviteit bewezen,
  poort 10).
- Resolver lift base+uitzonderingen naar één `DefaultLadder` (uitzonderingssport
  boven, later-geschreven = hoger), en de ongeordende groep naar zusterladders
  (`ongeordend` op elk, toelagegronden-vorm). Basis verlaat resolvedRegels
  (WF-050). Fail-fast op onbekende basis / afwijkend doel / initialisatie-basis
  / multi-versie. Emitter schrijft `defaultLadders` alleen als de extensie
  ladders opleverde (v0-mirror).
- Twee nieuwe goldens met `bron.rgs`: `jutterswet-bergingsdrempel` (geordende
  kenmerkuitzondering) en `jutterswet-wachtgeld` (ongeordende attribuutgroep
  met §9.5-conflictcase). Beide SPELEN in poort 12 (PLAYED 4 elk — ref ≡
  clingo ≡ Drools). Verwachtingen handberekend (NOTE.md), niet gefit.
- Rood-eerst JUnit (`LadderExtensieTest`) + positieve dekking + additiviteit;
  rood-eerst aangetoond (6/7 rood met de ladderpass uitgezet).
- Grammar-divergentielog: `frontends/regelspraak/grammar/DIVERGENCES.md`.

Beperkingen (bewust, eerste kaart): alleen kenmerkvorm voor de geordende
sport (attribuut-geordend is expressief maar niet nodig); geen bron-genest
`Rung.excepties`; multi-versie basis/uitzondering geweigerd.

Protected-path voorstellen (maintainer merget): `scripts/check-noop.sh` kreeg
drie survivor-entries (bergingsdrempel case3/case4, wachtgeld case4-geen) —
defeasibele standing, de absentie IS de assertie (analoog aan hardheid
case3-stilte/case4 en samenloop case6-geen-grond).

Nog te doen (hoofdsessie serieel, buiten deze kaart): CONSTRUCTS-delta voor de
v2.3.0-divergentie; toolchain-herpin/re-vendor bij landing.
