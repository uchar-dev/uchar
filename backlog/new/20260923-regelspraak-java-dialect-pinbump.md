# regelspraak-java-dialect-pinbump: "niet" en nesting in de toolchain (hoort in regelspraak-java)

*Status: concept, 23 september 2026 — wacht op de besluiten 1–8 van
spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md. Hoort volgens het
voorgestelde besluit 8 in de backlog van regelspraak-java; hier opgesteld
omdat de onderzoekssessie alleen in uchar schrijft. De hoofdsessie
verplaatst hem.*

Doel: na de landing van dialect-niet-nesting-frontend en
dialect-niet-nesting-goldens de SPEC_REV-pin-bump in regelspraak-java. De
gevendorde frontendslice (`vendor/uchar-frontend/`) brengt de nieuwe
grammatica en RuleChecker mee; de vuurweg hoeft niets nieuws te leren: de
exec-model-vertaling daar verlaagt `besluit` en geneste `excepties` al
(`ExecModelTranspiler.compileDefault`), en een `false`-beslissing schrijft
niets, zoals in uchar.

## Acceptatie (bij oppakken aanscherpen)

- (a) De conformantiecontrole (gevendorde kopie ≡ gepinde bron) groen op
  de nieuwe pin.
- (b) De tweelingen G2–G4 vuren in de toolchain met de verwachtingen van
  hun corpuscases.
- (c) De bestaande regressie van regelspraak-java groen.

Niet in deze kaart: de scenarioharnas, de graaf, de uitleg en de dekking
(zie testdocument-uitzonderingsvormen).

May touch (in regelspraak-java): `SPEC_REV`, `vendor/uchar-frontend/**`,
eigen tests.
