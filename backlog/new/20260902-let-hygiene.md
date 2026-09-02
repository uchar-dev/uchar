# Hygiëne: de Let-restpunten (testcommentaar, verifier-terminologie, defaults-golden)

Goal: Drie kleine, elk al gemeten restpunten uit de Let-ronde in één
veegbeurt. (1) Twee bestaande testbestanden dragen nog commentaar in de
oude STRIKTE Let-semantiek: de klassejavadoc van
`LetDragerWandelingWitnessTest` (de zinnen die de Bouwnotitie van de
companion-kaart exact opsomt: "the two paths a walk must NOT see
through", "descending there fabricates a companion for code that never
executes", en verwanten) en `LetClusterWitnessTest` rond r. 93 ("the
body never runs" — onder ch. 10.6-L draait de body wél; de lezing van de
binder forceert de lege need en de barrière valt op de LEESPLEK).
Maintainer-mandaat 2026-09-02: UITSLUITEND commentaar/javadoc in exact
deze twee bestanden; geen assertie, methode of pin wijzigt. (2)
`spec/verifier` draagt nog "strict Let"-vocabulaire in meldteksten
(o.a. de WF-077-melding "route state reads through a strict Let
binding") — sinds de L-unificatie is de Let call-by-need; de meldingen
horen de spec-terminologie te volgen zonder gedragswijziging. (3) De
prioritizedDefaults-consequentie draagt Lets gemeten-werkend maar
corpus-ongedekt: geen golden oefent een Let in een defaults-consequentie
door de gates — één kleine golden dicht dat (Jutterswet-sfeer, eigen
werk).

Acceptance:
- De twee testbestanden ch. 10.6-L-conform in hun commentaar; `git diff`
  toont UITSLUITEND commentaar-/javadocregels in die twee bestanden;
  de JUnit-suite draait ongewijzigd groen met dezelfde telling.
- `grep -ri "strict let" spec/verifier/` leeg; de verifier wijst exact
  dezelfde negatiefs af met dezelfde WF-IDs (gate 3 telling ongewijzigd
  op de nieuwe golden na); meldtekst-diffs opgesomd in de kaart.
- `corpus/golden/<nieuwe naam>/` met ir.json + minstens één case waarvan
  de consequentie van een prioritizedDefaults-groep een Let draagt;
  verwachtingen EERST door ref gemeten, door clingo bevestigd (gates
  5/6); gate-7-gedrag en de gate-12-status (played of reden-dragend
  auto-excluded) gemeten en gerapporteerd, nooit aangenomen.
- Alle twaalf gates groen; tellingsverschuivingen expliciet.

May touch: `backends/drools/src/test/java/**` (ALLEEN commentaar in de
twee genoemde bestanden), `spec/verifier/**` (alleen meldteksten,
proposal), `corpus/golden/<de nieuwe golden>/**`, `backlog/`.

Out of scope: asserties of testgedrag; verifier-semantiek; IR-SPEC- en
RUNTIME-SEMANTICS-teksten; bestaande goldens en pins; de
DrlTranspiler.
