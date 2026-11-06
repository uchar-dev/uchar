# verdeling-lege-pot-en-negatief-gewicht: ref en clingo volgen de twee v1-besluiten van ch. 4.4

*Gevonden op 2026-10-09 door de bouwer van stap 1 van `20261008-tms-verdeling-en-tijdlijnen` (de verdeling in de tak
met truth maintenance) en door de hoofdsessie nagemeten en vastgelegd volgens de afspraak dat een bestaande bug in ref,
clingo of de spec een kaart krijgt. Gemeten met ref, clingo en de verifier uit `_build` van main `ac0f9008` en de shaded
jar van die kaartbranch (`e9182168`).*

## Aanleiding

RUNTIME-SEMANTICS ch. 4.4 legt voor de verdeling twee v1-besluiten vast die ref en clingo niet uitvoeren:

- **Een lege pot** (tabelrij "empty source amount"): "**Delta (decided, v1):** 'empty pot ⇒ no distribution' is uniform
  across methods". Op volgorde is het al zo. In gelijke delen en naar rato faalt ref nog op het v0-gat ("EVALUATION
  FAULT: empty source amount in equal-shares allocation (v0 mirror hole, ch. 4.4)", en de variant voor naar rato), en
  geeft clingo `viol("allocation: empty pot","B1")`. `spec/ASP-TRANSLATION.md` (rond regel 1455) beschrijft dat zo.
- **Een negatief gewicht naar rato** (tabelrij "a negative weight — proportional"): "**Decided (v1):** the ordered
  method's clamp is adopted uniformly — a negative weight counts as 0 + §9.5 error". Ref houdt het v0-gat (`ref/lib/alloc.ml`
  rond regel 84, "A negative weight is the recorded v0 hole: unclamped"): gewichten 3 en −1 over 100 geven 150 en −50,
  zonder deponering. Clingo faalt met `viol("allocation: negative weight","J2")`.

De tak met truth maintenance volgt sinds die kaart de spec in beide (geen verdeling; het gewicht telt als 0 met een
deponering op de ontvanger). De oude exec-model-tak en de oude DRL-teksttak werpen bij een lege pot een
NullPointerException en rekenen het negatieve gewicht zoals ref.

Daarnaast is één zin dubbelzinnig. Ch. 4.2 zegt "In the weighted tie-break, a negative weight counts as 0 and an empty
weight counts as 0 (the member is assigned a real 0, plus the §9.5 error of ch. 4.3)", en de v1-zin hierboven spreekt
van "the ordered method's clamp … + §9.5 error". Beide lezen alsof een negatief tiegewicht op volgorde deponeert. De
tabel van ch. 4.4 ("counts as 0: claims nothing"), ref en clingo deponeren niet; de tak volgt die drie.

De documenten (verifier PASS) staan onder `/tmp/lens-tms-k3/stap1/metingen/documenten/` (`d-lege-pot-gelijk`,
`d-lege-pot-naar-rato`, `d-lege-pot-op-volgorde`, `d-negatief-gewicht-naar-rato`) en zijn in de getuige
`TmsAllocationWitnessTest` (deel D) uitgeschreven; de meting in `/tmp/lens-tms-k3/proef/stap1-bijvangst.txt`.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met een lege pot in gelijke delen en naar rato (ook naast een leeg gewicht) en met een negatief
  gewicht naar rato, eigen Jutterswet-casus, rechtenvrij; de verwachtingen zoals ch. 4.4 v1.
- (b) Ref en clingo gerepareerd, rood eerst; `ASP-TRANSLATION.md` mee (beschermd pad: voorstel).
- (c) De zin van ch. 4.2 en de v1-zin bij naar rato eenduidig: het negatieve tiegewicht telt als 0 zonder deponering
  (beschermd pad: voorstel).
- (d) Gate 12: de nieuwe golden speelt in de tak met truth maintenance; in de oude takken staat hij op de lijsten met
  reden, of hij wacht op `20261008-tms-omschakeling` (pins als voorstel). Alle gates groen.

## Omvang

Klein.

May touch: `ref/`, `backends/clingo/`, `corpus/golden/<nieuwe golden>/`, `spec/RUNTIME-SEMANTICS.md` en
`spec/ASP-TRANSLATION.md` (voorstel), `scripts/check-drools.sh` (lijsten, voorstel), kaartverplaatsing.

## Result

Gebouwd 2026-11-06 op branch `card/allocation-letbinder-dates` (van main `17f956d92`), in één commit. Logs onder
`werk/allocation-letbinder-dates/logs/` (naast de uchar-worktree).

- (a) Golden `corpus/golden/jutterswet-lege-pot-en-negatief-gewicht`, met `source.rgs` (eigen Jutterswet-casus): een
  Berging verdeelt haar opbrengst in gelijke delen en naar rato van het geborgen gewicht. `case1-lege-pot`: een lege
  pot naast een leeg gewicht (geen aandeel, geen rest, invoer 7 blijft staan, consistent) en een tweede Berging met
  pot 10 (zodat de no-op-wacht faalt); `case2-negatief-gewicht`: 100 over gewichten 3, −1 en leeg → naar rato 100, 0
  (EF-038) en leeg (EF-028), rest 0; gelijk 33, 33, 33, rest 1. Verifier PASS.
- (b) Rood eerst (`k1-red-before.log`): ref "EVALUATION FAULT: empty source amount in equal-shares allocation (v0
  mirror hole, ch. 4.4); …" op case 1 en `B1.rato-aandeel = 150 / B2 = -50`, kanaal zonder EF-038, op case 2; clingo
  UNSAT op `viol("allocation: empty pot","G1")` en `viol("allocation: negative weight","B2")`. Groen
  (`k1-green-after.log`): ref en clingo PASS (11 en 10 verwachtingen). Ref: `ref/lib/alloc.ml` (lege pot → `Nothing`
  in elke methode; negatief gewicht naar rato telt als 0 met deponering EF-038), worked examples
  `WV-4.6-h-pot-*`, `WV-4.6-h-neg-prop*`, `WV-4.6-h-neg-err` in `ref/test/test_ref.ml`. Clingo: de twee `viol`-regels
  weg, het negatieve gewicht als `vdI_weight(X,0)` met deponering EF-038, en de deponeringen EF-028/EF-038 wachten op
  de gevulde pot (zoals ref niets deponeert bij een lege pot). `ASP-TRANSLATION.md` (S-sectie verdeling) mee.
- (c) RUNTIME-SEMANTICS ch. 4.2 (naar rato en de tie-break op volgorde), ch. 4.4 (rijen lege pot, negatief gewicht op
  volgorde en naar rato) en ch. 4.6 (twee voorbeelden) herschreven: een negatief tiegewicht telt als 0 zonder fout;
  naar rato als 0 met de §9.5-fout. Het negatieve gewicht naar rato heeft nu een getuige en dus een code: EF-038 in de
  tabel van ch. 5.7b ("two kinds have no witness" in plaats van drie). De Drools-tak deponeert sindsdien EF-038
  (`transpiler/tms/Allocation.java`), `TmsAllocationWitnessTest` deel D pint de code; README's van clingo en Drools
  mee.
- (d) Gate 12: beide takken `PLAYED 2` op de golden (`k1-drools.log`); `EXPECTED_PLAYED` 347 → 348 met regel.
  Gerichte gates groen (`logs/k1/summary.txt`, `logs/k1b/summary.txt`): 1, 2, 3, 4, 5 (2504 worked examples), 6, 7,
  8, 10, 12, 13, 15, 17.

Voorgestelde rijtekst voor CONSTRUCTS (rij 17, verdeling), door de hoofdsessie te landen: "Lege pot: geen verdeling
in elke methode (v1, ch. 4.4; ref, clingo en Drools sinds 2026-11-06). Negatief gewicht naar rato: telt als 0 + §9.5
(EF-038), golden `jutterswet-lege-pot-en-negatief-gewicht`."

### Landing (hoofdsessie, 2026-11-06)

Geland op main na `make gates` op de bouwerstand (`319628d89`, 17 van 17 groen, 433 s; na de rebase op main alleen
backlog en CONSTRUCTS anders). De voorgestelde CONSTRUCTS-tekst staat in de landingscommit.
