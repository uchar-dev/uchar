# De AANTAL-setbron: telt hij leden of gevulde waarden, of telt hij niet

Goal: `AANTAL` met een setbron-argument is verifier-schoon en wordt door
`ref` en Drools stil verschillend gelezen — `ref` telt gevulde waarden,
Drools telt leden, en op een casus waarin één lid het attribuut mist is dat
2 tegen 3 (gemeten 2026-09-19, kaart eerste-laatste-setvouw-kloof §3). De
ASP-backend weigert de vorm. Er is geen producentroute: de
RegelSpraak-frontend weigert de natuurlijke zin met haar eigen
WF-096-melding. `RUNTIME-SEMANTICS.md` ch. 3.4a/3.4b en de normatieve
RegelSpraak-spec §5.8.1 zeggen allebei dat een telling instanties telt.
Deze kaart voert het besluit uit dat de maintainer neemt uit de drie
genummerde opties van die kaart (i-instanties / i-waarden / ii).

Acceptance: één besluit, met de kosten van de niet-gekozen opties erbij;
rood-eerst (bij (ii) het negatief per spelling met de verifier-md5 vóór en
ná plus een arm-exacte mutant; bij (i) de getuige-golden met een LEEG
elementattribuut in de casus, want zonder dat lid is de vorm niet
onderscheidend); de corpussweep vóór/ná; alle veertien poorten groen.

May touch: bij (ii) `spec/IR-SPEC.md` en `spec/verifier/**` als VOORSTEL
(beschermde paden) plus `corpus/negatief/**`; bij (i-waarden)
`backends/clingo/**`, `corpus/golden/**` en de waardedivergentielijst van
`scripts/check-drools.sh`; bij (i-instanties) bovendien `ref/**` en
`spec/RUNTIME-SEMANTICS.md` als voorstel. `backlog/`.

Out of scope: EERSTE/LAATSTE — beslecht 2026-09-19, route (i) gebouwd.
`frontends/**`.
