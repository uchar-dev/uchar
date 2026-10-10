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
`TmsVerdelingWitnessTest` (deel D) uitgeschreven; de meting in `/tmp/lens-tms-k3/proef/stap1-bijvangst.txt`.

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
