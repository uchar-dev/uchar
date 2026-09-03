# Drools: uniciteitssleutel-conventies (naamnormalisatie + kruis-bron gemengde unie)

Goal: Twee pre-existente lacunes in de uniciteits-lowering van
`backends/drools`, blootgelegd door de lenzen op de
temporele-sleutelvorm-kaart (2026-08-30). (1) NAAMNORMALISATIE: alle vier
sleutelsites (de twee joins in `ExecModelTranspiler` en de twee in de
sluimerende `DrlTranspiler`) toetsen temporaliteit met een exacte
String-match (`attribuutGranulariteit.containsKey`), waar ref en clingo
normaliseren (`Checks.norm`) — een anders gekapitaliseerde sleutel
passeert de verifier, telt hier als scalair, en levert op een GELDIG
één-component-model een stil verkeerd antwoord (gemiste botsingen die
ref/clingo vinden; lens-bewezen). De ref-getrouwe spiegel
(`temporeelAttribuut`) bestaat al in het bestand — trek de conventie op
alle sites gelijk, met witnesses. (2) KRUIS-BRON GEMENGDE UNIE: een
uniesleutel die temporeel is op bron A en scalair op bron B laat Drools
crashen (`ERROR ConstraintEvaluationException`) waar ref ≡ clingo gewoon
spelen (lens-bewezen op een verifier-schoon model); een ERROR is onder het
gate-12-regime nooit een geldige niet-gedragen uitkomst — draag de vorm of
weiger hem luid per vorm.

Acceptance:
- Rood-eerst per lacune (de lens-modellen als startpunt: de
  gekapitaliseerde-sleutel-variant met gemiste botsing; de
  kruis-bron-unie met de crash).
- Nieuwe witnesses pinnen beide kanten; ref is de scheidsrechter (elke
  verwachting eerst door ref).
- Alle twaalf gates groen met ongewijzigde tellingen (geen corpusgetuige
  voor beide vormen — verifieer dat dat zo blijft of voeg
  synthese-getuigen toe via het volledige differentiaalvierkant).

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `backlog/`.

Out of scope: ref/clingo/verifier/spec/corpusverwachtingen, de
vuur-tijd-plaatsing van de ariteitsguard (geland en gepind), gate-scripts.
