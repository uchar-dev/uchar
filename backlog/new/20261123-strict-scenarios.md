# strict-scenarios: een toetsscenario voor een strikt document

*Voorstel van de hoofdsessie, 2026-11-23; nog niet besloten en niet begonnen. Volgt uit het besluit `one-empty`:
"nog niet" en "nooit" op één attribuut scheidt voortaan een strikt document, en een toetsdocument daarvan moet dat kunnen
zeggen. Bijgewerkt na een tweede review (extern model, 2026-11-23).*

## Aanleiding

De scenariolaag van het dialect (`DIVERGENCES.md`, "Scenariotoetsen"; `emit/CaseEmitter.java`) schrijft in een situatie
alleen de kenmerken die gelden, en als verwachting `attributeValue`, `attributeEmpty`, `characteristicPresent` en
`characteristicAbsent`. Het caseschema kent ook `characteristicsAbsent` (invoer) en `attributeOpen` en `characteristicOpen`
(verwachting, RUNTIME-SEMANTICS ch. 1.5), maar geen bestand onder `frontends/` schrijft ze (gemeten 2026-11-23). In een
document met `Afleiden uit leeg: nee` kan een toetsscenario dus niet zeggen dat een kenmerk vast niet geldt (een kenmerk dat
de situatie niet noemt, is open), en niet verwachten dat iets open blijft. De strikte golden `jutterswet-afleiden-uit-leeg`
heeft daarom alleen handgeschreven cases.

## Voorstel

1. **Situatie**, in het blok van een instantie (`een Vondst V1`): "- is niet drijfhout" (bijvoeglijk, lidwoord zoals bij
   "- is drijfhout") en "- heeft geen strandrecht" (bezittelijk) zetten het kenmerk in `characteristicsAbsent`. Hetzelfde
   kenmerk als geldend en als niet geldend in één blok is een fout met een eigen RGS-code; een situatie `gebaseerd op` een
   andere overschrijft wat zij van die andere erft, zodat de case het kenmerk niet in beide lijsten krijgt (EF-047). Een
   periode bij een niet geldend kenmerk wordt geweigerd. In een gewoon document verandert `characteristicsAbsent` geen
   uitkomst.
2. **Verwachting**: "- de afhaaldatum van V1 staat niet vast" (ook met "het") wordt `attributeOpen`; "- of V1 afhaalbaar is,
   staat niet vast" en "- of V1 strandrecht heeft, staat niet vast" worden `characteristicOpen`, de komma vrij. Dat zijn de
   woorden die de uitleg van de toolchain al gebruikt. `of`, `staat`, `niet` en `vast` worden geen gereserveerde woorden. In een
   gewoon document weigert de frontend beide met een eigen RGS-code (daar is niets open).
3. "- de afhaaldatum van V1 is leeg" blijft `attributeEmpty`, in een strikt document dus vaststaand leeg. Een attribuut als
   vaststaand leeg in de situatie hoort hier niet bij: dat vraagt `attributesEmpty` in het caseschema (kaart
   `settled-empty-input`, geparkeerd).
4. Gate 16 regenereert; een golden met een `scenario.rgs` voor een strikt document (het strikte deel van de kenmerkgolden van
   `one-empty`, of `jutterswet-afleiden-uit-leeg`).
5. **Toolchain** (pin-bump): `ScenarioData`, `SituationToRequest` en de playground zetten `characteristicsAbsent` om in
   `{"state": "holdsNot"}` en lezen `attributeOpen` en `characteristicOpen` als verwachting.

## Besluiten voor de eigenaar

- De zinnen: "- is niet X" en "- heeft geen X" in een situatie, "staat niet vast" in een verwachting.
- De volgorde: voorstel is deze kaart eerst, samen met deel 2 van `one-empty`, vóór de overstap van de regelset die nu
  `niet van toepassing` gebruikt en vóór deel 1 van `one-empty`, zodat die regelset haar "nog niet"-gevallen als open kan
  toetsen.
