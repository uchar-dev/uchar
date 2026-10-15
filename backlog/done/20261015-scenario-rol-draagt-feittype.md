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

## Result (2026-10-15)

`4c63e449` geeft `ResolvedScenario.RolWaarde` het veld `Feittype feittype`, tussen `sleutel` en `doeltype`:
`RolWaarde(String sleutel, Feittype feittype, Objecttype doeltype, List<String> doelLabels, boolean meervoudig, int
line)`. Het is het `Feittype` van het model zelf, zoals `doeltype` het `Objecttype` zelf is; een consument leest de
naam als `feittype().naam`.

- `ScenarioResolver.rol` bewaart per treffer het feittype naast de rol (een lokaal record `Treffer(Feittype, Rol)` in
  plaats van `Rol[]`) en geeft het feittype van de enige treffer na de eenduidigheidstoets door aan `RolWaarde`. Dat
  is de enige plek die `RolWaarde` bouwt. `CaseEmitter` en `ScenarioTypeGateTest` lezen alleen velden en veranderen
  niet; het case-formaat ook niet (gate 16: 30 scenario-goldens geven hun 102 case-bestanden en hun `ir.json`
  bytegelijk).
- (a) Gehaald, zie boven.
- (b) Nieuw: `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/ScenarioRolFeittypeTest.java`, 4 tests op
  een eigen Jutterswet-model waarin de rol `vergunning` in drie feittypen staat (`vergunninghouderschap`, `vergunde
  vondst`, `vergunde strook`): het feittype per instantie (een Jutter, een Vondst en een Strook die elk via een
  ander feittype een Vergunning bereiken), de andere kant vanaf de Vergunning, het wederkerige feittype
  `strandmaatschap`, en dat het veld het object uit `model.feittypen` is. Rood op `79f8741b`: de test compileert niet
  (`cannot find symbol: method feittype()`). Een tijdelijke mutatie die het feittype opzoekt op alleen de rolnaam
  (het eerste feittype met een rol van die naam) laat de eerste test falen met `expected: <vergunde vondst> but
  was: <vergunninghouderschap>`. Geen bestaand testbestand gewijzigd.
- (c) Alle 16 gates groen op `4c63e449` (de boom `d09d274f`), 09:14:35–09:32:18; deze commit raakt daarna alleen
  `backlog/`:
  1 PASS; 2 348 bestanden valideren en round-trippen; 3 348 golden schoon, 505 negatief afgewezen; 4 873
  case-bestanden; 5 ref-suite 1286, 823 cases; 6 clingo-suite 2198 checks, 823 cases; 7 778 cases, 45 survivors;
  8 50 rode cases; 9 241 checks, 12 catala-goldens; 10 JUnit 1169 tests (de 4 nieuwe erbij), 125 rgs-goldens; 11 29
  TOKA-cases op ref, clingo en Drools; 12 596 s, 255 gespeeld op beide takken, JUnit 1574; 13 43 twists over 24
  goldens, 122 replays; 14 150 checks, 2 goldens; 15 266 van 303 goldens vergeleken, 125 op de bronroute, 69
  unittests; 16 30 scenario-goldens, 102 case-bestanden.

Voor de distributie: de playground van regelspraak-toolchain zet het feittype als `feittype` bij elke rol in
`situatie.objecten[].rollen[]`, op haar kaart `playground-rol-geeft-feittype`, met een pin op deze branch. Die pin
kan pas landen als deze kaart op origin staat; een rebase vóór de landing verandert de commit en dan de pin.

Geen registerrij voor `spec/CONSTRUCTS.md` en geen wijziging van `spec/PLAN.md`: de kaart verandert geen construct,
geen schema en geen case-formaat, alleen een record van de frontend-API.
