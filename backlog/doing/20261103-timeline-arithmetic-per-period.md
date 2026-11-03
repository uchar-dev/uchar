# timeline-arithmetic-per-period: rekenen per dag, de tijdsafhankelijke parameter en de kalender (impliciete tijd, fase 1b en 2b)

*Opgesteld door de hoofdsessie op 2026-11-03 uit het Result van `backlog/done/20261010-impliciete-tijd.md` en het voorstel
`spec/proposals/IMPLICIT-TIME.md`, punt 1, 2 en 11. Na `20261103-timeline-arithmetic-by-rule`. Besluiten van de eigenaar
2026-11-03: alle tijd (2); **de kalender** (11): tussen een eenheid die in dagen telt (dg, wk) en een die in maanden telt (mnd,
kw, jr) integreert het totaal met de werkelijke lengte van de omsluitende kalendereenheid, en een gedeclareerde factor tussen
die families is ill-formed; binnen een familie blijft de factor.*

## Stap A — spec, ref, clingo, verifier, frontend

Goal: maal, gedeeld door (beide varianten), percentage van en verminderd met over tijdlijnen, per dag met de leeg-regels van
ch. 1.2 (punt 1, besloten: spec spreekt); een scalair en een tijdsafhankelijke parameter als tijdlijnoperand (VB 2.3); de
integratie van een rente over een raster van een andere eenheid volgens de kalender; in ref, clingo en de verifier, met
formulering B van de ankercasus als anker.

Acceptance:
- (a) Rood eerst: anker-golden `corpus/golden/anchor/jutterswet-verzuimrente-dagen` (beschermd: voorstel) met geval 1 en 2 van
  formulering B (IMPLICIT-TIME §4): `rentedragend` als invoer, de dagrente (voor elke dag, €/jr) = de verzuimrentevoet maal het
  afdrachtbedrag, de verzuimrente = het totaal van de dagrente gedurende de tijd dat hij rentedragend is; verwachtingen
  **139 13/73 €** en **73 47/61 €** (de kalender, 2028 is een schrikkeljaar). Op main de weigeringen Z4/Z15 (ref), Z7/Z16
  (clingo) van IMPLICIT-TIME §5.
- (b) Het dagraster in de laders van ref en clingo (Z14) en in de ASP-codering; een totaal over een dagraster.
- (c) RS ch. 2.4 (VB 1.4: de precisiepoort ook op een tijdlijndoel met zo'n waarde), 3.4, 3.6 (de kalender; de voorbeelden met
  mnd = 30 dg en hun `ref`-tests gaan mee, met oud en nieuw genoemd), 3.12 en 8.3 (voorstel); WF-023 op een tijdlijndoel; een
  nieuwe WF-arm voor een factor tussen de dag- en de maandfamilie (voorstel); een negatief per nieuwe arm; rood voor EF-031 en
  EF-005 in een periode (VB 1.3: één §9.5-feit per regel × onderwerp × code).
- (d) De `ref/test`-rijen `WV-3.4-per-dag`, `WV-3.4-domein` (als 1a haar nog niet heeft) en `WV-3.6-kalender` van het voorstel
  letterlijk.
- (e) De RegelSpraak-frontend schrijft de vormen van het anker (gate 10, `source.rgs`).
- (f) Meet, alleen lezend, of een regelset onder `/Users/yeb/Developer/uwv/regelspraak-werkbank/rulesets/regelspraak-rules/v2/` of
  een golden een factor tussen de dag- en de maandfamilie declareert (de nieuwe WF-arm zou die weigeren); noem elke vondst.
- (g) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `spec/verifier/` (voorstel), `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`,
`spec/ASP-TRANSLATION.md` (voorstel), `corpus/golden/anchor/<nieuw>/` (voorstel), `corpus/negative/<nieuw>/`,
`corpus/red/<nieuw>/`, `frontends/regelspraak/**` (alleen een gebrek dat het anker blootlegt), `scripts/check-drools.sh`,
`scripts/check-red.sh` (voorstel), kaartverplaatsing.

Out of scope: voorwaarden per periode, de kwalificator, twee schrijvers en de dagentelling (`20261103-conditions-per-period`);
het totaal over een gedeeltelijke eenheid (`20261103-total-over-partial-unit`).

## Stap B — Drools (direct na de landing van stap A)

Acceptance: (a) rood eerst: het anker geeft op main DROOLS-105, DROOLS-030 (tijdsafhankelijke parameter) of DROOLS-228
(integratie over een dagraster); (b) `PLAYED` op beide branches, `EXPECTED_PLAYED` omhoog, kanaalpins weg (voorstel),
codes zonder plek `RETIRED`; (c) gate 17 gelijk, een getuige per nieuwe vorm in elke regelvolgorde; (d) `make gates` groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (voorstel), kaartverplaatsing.

## Omvang

Middel tot groot (stap A), middel (stap B).
