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
