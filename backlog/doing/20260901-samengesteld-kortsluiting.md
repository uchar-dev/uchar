# Samengesteld ALLE/DE sluit links-naar-rechts kort (§11.1's echte drager)

Goal: Het §11.1-deling-door-nul-voorbeeld van de officiële RegelSpraak-
spec (read-only referentie; §-verwijzingen mogen, excerpten nooit) wordt
niet door een Let-semantiek gered maar door het VOORWAARDENDEEL: de
genormeerde evaluatievolgorde interleavet variabelen en enkele
voorwaarden links-naar-rechts, en een eerdere voorwaarde (`A ongelijk
aan 0`) poort de latere die zou faulten (`B > 0` met `B = b/A`).
Vandaag evalueert `Samengesteld ALLE/DE` (ch. 8.3.2) in ref álle leden
(`List.filter`, eval.ml ~1177) en clingo's integriteitsconstraint vuurt
volgorde-onafhankelijk — gemeten (verkenning 2026-09-01, probe H):
het voorbeeld fault in beide. Deze kaart maakt de kortsluiting
normatief: ALLE stopt bij het eerste onware lid, links-naar-rechts in
wire-volgorde; de leden ná de kortsluiting worden niet geëvalueerd
(geen fault, geen §9.5-kanaalfeit). Gemeten: in ref één regel
(`List.for_all`), 0/399 corpus-verschillen; clingo en Drools vergen
echt ontwerp (de deel-door-nul-constraint moet conjunct-volgorde-bewust
worden resp. de pattern-conjunctie kent geen volgorde-semantiek —
mogelijk een guard-lowering per conjunct-prefix).

Acceptance:
- Rood-eerst: de probe-H-vorm (voorwaarde `ALLE[deler ≠ 0; (Let q =
  100,00 ÷ deler in q) > 0,00]`, deler=0) fault vóór de kaart in ref én
  clingo; ná de kaart: geen fault, regel vuurt niet, doel ongeschreven —
  als getuige-golden met absolute pins (eerst door ref).
- RUNTIME-SEMANTICS ch. 8.3.2 draagt de volgorde-normatieve
  kortsluiting met gewerkt voorbeeld; ASP-TRANSLATION documenteert de
  vertaalvorm; Drools draagt de vorm of weigert reden-dragend per de
  gate-12-discipline.
- Alle twaalf gates groen; geen bestaande pin beweegt (gemeten
  verwacht: 0/399).

May touch: spec/RUNTIME-SEMANTICS.md, spec/IR-SPEC.md (indien WF-tekst),
spec/ASP-TRANSLATION.md, ref/**, backends/clingo/**,
backends/drools/**, corpus/golden/** (nieuwe getuige),
corpus/negatief/** (indien WF), scripts/ (lijsten), backlog/.

Out of scope: de Let-lowering zelf (kaart
20260901-let-lowering-spec-first), OF/GEEN-VAN-kwantoren tenzij de
spec-tekst ze gratis meeneemt, toka/.
