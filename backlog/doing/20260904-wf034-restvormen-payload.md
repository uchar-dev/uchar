# De drie WF-034-restvormen en de SumType-payload-positie

Goal: de voortzetting van het WF-034-verbredingsbesluit (2026-09-04) op
de vier vormen die toen gemeten en bewust open gelaten zijn
(maintaineropdracht 2026-09-04, tweede golf): (1) een `AttribuutRef`
zonder `attribuut` (409 van 751 golden-posities overleven
één-voor-één-lediging; ref en clingo falen luid, Drools geeft een
ConstraintEvaluationException of een transpiler-SKIP); (2) een
`ParameterRef` zonder `parameter` (35 van 37; ref/clingo luid, Drools
NPE); (3) het regeldoel zonder `type` — de "unspelled twin" (295 van
295; ref en clingo spelen het document SCHOON en alleen Drools NPE't op
verifier-schone invoer — de gevaarlijkste van de drie); (4) het
Onbekend-sentinel op een `SumType`-constructor-payload (12 van de 21
overlevenden van de 2.159-injectiemeting; de WF-034-wandeling daalt de
payload niet in). Dracht van alle vier: nul buiten de eigen getuigen
(gemeten over vier zones).

Route, per het staande besluit: WF-034-clausules voor (1)–(3) — zelfde
wet, volgende vormen — en voor (4) de sentinel-wandeling door
constructor-payloads; meet daarbij of de WF-007-vormcheck dezelfde
wandeling deelt en neem hem alleen mee als dat één beweging is (anders
eerlijk open laten). Elk rood-eerst met een eigen negatief,
clausule-exact mutant-bewezen; de niet-exhaustieve
restvormen-opsommingen in IR-SPEC worden korter en moeten kloppend
herformuleerd (de les van de vorige golf: tel de eigen getuigen nooit
mee als drachtnul).

Acceptance:
- Vier negatiefs (rood-eerst zichtbaar in de historie), vier clausules,
  elk mutant-exact.
- Nul over-afwijzing, gemeten over golden+rood+toka+negatief (de
  bestaande negatiefs behouden hun nummer).
- IR-SPEC-passages (voorstel) bijgewerkt; alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `backlog/`.

Out of scope: executors, frontends, de overige onbereikte
vormcheck-posities uit de D-kaart (FunctionDef/lambda/ListType/Struct —
die zijn buurregel-gedekt of hebben hun eigen OPEN-notitie), bestaande
artefacten.
