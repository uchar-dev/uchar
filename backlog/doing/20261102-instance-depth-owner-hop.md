# instance-depth-owner-hop: ref rekent de diepte van een instantiegeordend deel zonder kring

*Gevonden op 2026-11-01 bij het onderzoek van `20261009-drools-schrijfgrens` (ref haalde op 18 van de 175 v2-gevallen de
meetgrens niet) en op 2026-11-02 uitgezocht. Rapport, commando's en logs: `/Users/yeb/Developer/uchar-dev/research/ref-tijd-2026-11-02/`.*

Goal: ref eindigt op elke verifier-schone invoer met een instantiegeordend deel. De stap naar de eigenaar in `depth` geldt
alleen een satelliet, en een kring in die rekening is een fout met pad en labels, geen Stack overflow na 80 minuten.

## Aanleiding

Werkbank-regels `2d0aae3`, scenario `toets-g7-g9-restant-en-wekeneis`, IR en cases van uchar `3d4676b88`: 18 van de 28
gevallen (allemaal met een `eerdere recht`-link) halen in ref geen uitkomst. Drools speelt ze alle 28 in < 1 s, en clingo
speelt het verkleinde corpusgeval. Gemeten: geval 10 na 1800 s niet klaar, RSS 1,2 GB. Met `OCAMLRUNPARAM=l=8M` geeft het
`Stack overflow` na 24,5 s. Profiel: 90 % in de minor GC die de stack afloopt.

`ref/lib/eval.ml:4261-4262` neemt de eigenaarsstap ook vanaf het eerste recht. De rolnaam `recht` vindt daar via
`World.left_side` (op rolnaam, zonder feittype) de teruglink van `eerdere recht` die `ref/lib/case.ml:603` neerzette, en
`depth` (`eval.ml:4250-4266`, memo pas na de berekening) loopt R2 → R1 → R2 rond.

Sinds `9c6a5c792` (2026-10-22); gemeten op de golden `jutterswet-vergunningketen` met de eigenaarsrol hernoemd tot
`vergunning`: `cbab1246e` PASS, `9c6a5c792` tot en met `3d4676b88` Stack overflow. Met een typebewaking in een scratchkopie
halen alle 28 gevallen PASS in 0,2-1,25 s.

De beslisservice is niet geraakt: zij draait op Drools en roept ref niet aan, en de Drools-tijd per geval is tussen de
toolchain-pins `57ae7d515` en `f6082c2a` gelijk (mediaan 0,919 en 0,927 s, verschil binnen de ruis).

## Richting

In `depth` de eigenaarsstap alleen nemen vanaf een object waarvan het type de satellietkant van dat feittype vult (de
voorwaarde van `satellite_hop`, `spec/verifier/lib/checks.ml:8382-8395`). Daarnaast een markering "in bewerking" in de memo,
zodat een kring faalt met `ref/lib/eval.ml, instance order: …` en de labels, in de stijl van de `failwith` in hetzelfde blok.

Acceptance:
- (a) Rood eerst: een golden (Jutterswet) met een vergunningketen waarin de eigenaarsrol van de satellietrelatie dezelfde naam
  draagt als de zusterrol van de schakelrelatie, met een keten van twee en van drie. ref ≡ clingo ≡ beide Drools-branches op
  absolute verwachtingen, en ref binnen een seconde.
- (b) Een worked example in `ref/test/` (WV-6.4-…) voor de diepte van een object van het ketentype zonder voorganger, met een
  teruglink onder de naam van de eigenaarsrol: diepte 0.
- (c) De 28 gevallen van `toets-g7-g9-restant-en-wekeneis` halen in ref `oracle: PASS`; de meting in het Result.
- (d) `make gates` groen.

May touch: `ref/lib/eval.ml`, `ref/test/test_ref.ml`, `corpus/golden/<nieuw>/`, kaartverplaatsing. De spec niet:
ch. 6.2 (`RUNTIME-SEMANTICS.md:6293-6296`) zet een object zonder voorganger al op diepte 0.

Out of scope: dat `World.left_side` op rolnaam zonder feittype indexeert (`ref/lib/world.ml:20`). Hier is dat alleen de route
waarlangs de verkeerde stap iets vindt; of andere lezingen erdoor geraakt kunnen worden, is niet onderzocht.
