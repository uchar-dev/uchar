# drools-schrijfgrens: de Drools-backend schrijft een waarde die niet in de decimalen van het doel past

*Gevonden op 2026-10-09 door de bouwer van stap 1 van `20261008-tms-verdeling-en-tijdlijnen` en door de hoofdsessie
nagemeten en vastgelegd volgens de afspraak dat een bestaande bug buiten de oude takken een kaart krijgt. Gemeten met
ref, clingo en de verifier uit `_build` van main `ac0f9008` en de shaded jar van die kaartbranch (`e9182168`).*

## Aanleiding

RUNTIME-SEMANTICS ch. 2.4 punt 1: een waarde schrijven waarvan de precisie de gedeclareerde decimalen van het doel
overschrijdt, is een fout; de auteur moet expliciet afronden. Ref controleert dat bij het schrijven (`ref/lib/world.ml`,
"the write boundary (ch. 2.4)"), clingo met een benoemde `viol`. De Drools-backend controleert het in geen van zijn
drie takken.

Document: `corpus/golden/jutterswet-gelijkdeling` zonder `roundingDecimals` op de verdeling, met de case
`case2-restant` (1001 in gelijke delen over twee jutters, het aandeel een geheel getal). Verifier PASS.

| Uitvoerder | J1.veilingdeel |
|---|---|
| ref | fout: "J1.veilingdeel: value 1001/2 does not fit the declared 0 decimals — an explicit rounding is mandatory (ch. 2.4)" |
| clingo | `viol("allocation: a share does not fit the target scale (an explicit rounding is mandated before precision is lost)","J1")` |
| Drools, alle drie de takken | 500.5 (PLAYED) |

Geen golden raakt het, want gate 5 speelt elke case eerst door ref. Volgens de bouwer kent ook een rekenende regel in de
tak met truth maintenance geen schrijfgrenscontrole; dat is niet nagemeten.

## Acceptatie (bij oppakken aanscherpen)

- (a) Nagaan of ch. 2.4 punt 1 ("a static error") voor deze vorm statisch kan (de verifier), of dat het een
  evaluatiefout is zoals in ref; voor een regel en voor een verdeling.
- (b) De tak met truth maintenance (na `20261008-tms-omschakeling` dé exec-model-tak) faalt zoals ref, als logisch
  `Fault`-feit aan het fixpunt; rood eerst, in elke volgorde van de betrokken regels.
- (c) Een rood geval in `corpus/red/` als het een evaluatiefout blijft (ref en clingo met hun zin).
- (d) Alle gates groen; de pins van gate 12 bewegen niet.

## Omvang

Klein tot middel. Na `20261008-tms-omschakeling`, zodat alleen de tak met truth maintenance mee hoeft; de DRL-teksttak
volgt met `20261008-drl-teksttak-op-truth-maintenance`.

May touch: `backends/drools/**`, `corpus/red/<nieuw geval>/`, `spec/verifier/` (voorstel), kaartverplaatsing.

## Na `20261008-tms-omschakeling` (2026-10-11)

Sinds `20261008-tms-omschakeling` (2026-10-10) heeft de Drools-backend twee takken; 'drie takken' is de stand van
2026-10-09.
