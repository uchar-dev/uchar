# frontend-heelperiode-in-aantalfilter: de frontend zet een subselectie met een heelperiode in een aggregatie niet om

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit het
verslag van stap 3a van die kaart. Bestaande fout van `frontends/regelspraak`.*

## Aanleiding

Een die/dat-subselectie met een heelperiodekwalificatie op de operand van een aggregatie, zoals "het aantal posten van
het Kustvak die gedurende het gehele jaar bemand zijn", zet de frontend niet om: hij schrijft een `Unresolved`-filter
en een waarschuwing. De vorm zelf is in de IR uit te drukken. Voor de getuigen van `20260923-drl-aantal-filter-zink`
is het filter met de hand in de IR ingevuld; die IR is verifier-schoon, en ref, clingo en beide Drools-takken spelen
haar. Sinds de landing van `drl-teksttak-op-truth-maintenance` staan die twee documenten als handgeschreven golden in
het corpus, zonder `bron.rgs`, met de bron als toelichting in hun `NOTE.md`: `jutterswet-aantalfilter-gehele-jaar`
en `jutterswet-aantalfilter-gehele-jaar-creatieguard`.

## Acceptatie (bij oppakken aanscherpen)

- (a) De frontend zet de subselectie met een heelperiode om naar het filter dat de twee goldens met de hand dragen,
  byte voor byte.
- (b) De twee goldens krijgen hun `bron.rgs`; gate 10 houdt ze byte voor byte gelijk.
- (c) Een frontendtest, rood eerst; de frontendsuite en gate 10 groen.

May touch: `frontends/regelspraak/**`, de twee goldens (`bron.rgs` erbij, `NOTE.md`), kaartverplaatsing.

Out of scope: `ref/`, `backends/`, `spec/`.
