# aantal-gevulde-gemiddelde: de waardentelling als eigen, normatieve vouw

**Maintainersopdracht 2026-09-19** (bij het AANTAL-setbron-besluit):
`AANTAL` telt instanties en dat blijft zo; voor berekeningen zoals een
gemiddelde is daarnaast een afzonderlijke operatie nodig die
uitsluitend GEVULDE attribuutwaarden telt — bij `[5, 7, leeg]` is dat
2. De twee betekenissen mogen op de wire niet vermengd worden; de
huidige AANTAL-setbron-vorm wordt in de zusterkaart
(`20260919-aantal-setbron-besluit.md`) geweigerd, en deze kaart bouwt
de nette vervanger.

Te ontwerpen (spec-first, met normbesluit-punten als genummerde
vragen waar de spec zwijgt):

- de vouwsoort(en): een waardentelling (werknaam `AANTAL_GEVULDE`)
  en/of een normatief gedefinieerde `GEMIDDELDE`-vouw (som van de
  gevulde waarden gedeeld door de waardentelling?);
- de leegwaardesemantiek, expliciet: wat telt als gevuld, wat doet
  een geheel lege bron (0? leeg? — vergelijk ch. 1.2e's aantal-is-
  nooit-leeg tegenover de presence-guard van min/max), en hoe
  verhoudt zich dat tot een `leegwaarde`-fallback;
- de bronvormen: setbron, rolbron, `bronPad` — en welke de eerste
  kaart draagt;
- de brontaalkant: kent RegelSpraak v2.3.0 een gemiddelde of een
  waardentelling (vindplaatsen, read-only), of is dit een
  uchar-dialectvorm met eigen CONSTRUCTS-rij?

## Acceptatie (bij oppakken aanscherpen)

- (a) Specvoorstel (IR-SPEC-knoop + RUNTIME-SEMANTICS-clausule met
  worked example) als beschermd-padvoorstel; WF-regel met verifier-
  check, golden mét een leeg lid in de casus (zonder dat lid is de
  vorm niet onderscheidend) en negatief.
- (b) Ondersteuning in alle backends: ref, clingo én beide
  Drools-takken dragen of weigeren benoemd; het vierkant speelt de
  golden.
- (c) Corpussweep; kanaal-/tellingsgevolgen als gemandateerd
  beschermd-padvoorstel; alle veertien gates groen.

May touch: bij oppakken bepalen (raakt `spec/**` als voorstel, alle
uitvoerders, corpus). NB: serieel ná de zusterkaart
aantal-setbron-besluit.
