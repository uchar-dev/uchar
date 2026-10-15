# scenario-rol-draagt-feittype: een roltoekenning in een opgeloste situatie draagt haar feittype

*Op verzoek van de eigenaar (2026-10-15). Regelnummers op `79f8741b`.*

Goal: `ResolvedScenario.RolWaarde` krijgt het feittype waarin `ScenarioResolver.rol` de rol vond. Eén rolnaam kan in
meer feittypen staan; welk feittype een toekenning bedoelt, hangt af van het type van de instantie waarop ze staat. De
resolver weet dat op het moment dat hij de rol vindt, maar geeft het niet door: de lus over de feittypen
(`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/ScenarioResolver.java:727`) bewaart van een
treffer alleen de `Rol` (r. 730, 731, 734), en de `RolWaarde` wordt zonder feittype gebouwd (r. 758). Een consument
van de situatie (de playground van regelspraak-toolchain, de RegelSpraak-werkbank) kan het feittype dan alleen zelf
afleiden, en dat mag hij niet. In de scenario's van herleving-ww hangen veel objecten via de rol `recht` aan één
recht, elk via een ander feittype.

Het veld is het `Feittype` van het model zelf, zoals `doeltype` het `Objecttype` zelf is. `RolWaarde` wordt alleen in
`ScenarioResolver.rol` gebouwd; `CaseEmitter` en `ScenarioTypeGateTest` lezen alleen velden. Het case-formaat
(`roles: {<rolnaam>: [labels]}`) verandert niet.

Acceptance:
- (a) `RolWaarde(String sleutel, Feittype feittype, Objecttype doeltype, List<String> doelLabels, boolean meervoudig,
  int line)`; `ScenarioResolver.rol` vult `feittype` met het feittype van de enige treffer na de eenduidigheidstoets.
- (b) Een nieuwe testklasse `ScenarioRolFeittypeTest`, eerst rood: een scenario waarin één rolnaam in drie feittypen
  staat en het feittype per instantie verschilt, beide kanten van een feittype, een wederkerig feittype, en het
  feittype is het object van het model.
- (c) Alle 16 gates van `CLAUDE.md` groen op de eindstand van de branch; gate 16 laat zien dat de case-bestanden
  bytegelijk blijven.

May touch: `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/ir/ResolvedScenario.java`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/ScenarioResolver.java`, de nieuwe
`frontends/regelspraak/src/test/java/dev/uchar/regelspraak/ScenarioRolFeittypeTest.java`, deze kaart.

Out of scope: het case-formaat en zijn schema; `CaseEmitter`; de toolchain (die neemt het veld over na de landing en
een pin-bump, op haar eigen kaart).
