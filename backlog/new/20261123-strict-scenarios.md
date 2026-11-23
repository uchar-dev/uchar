# strict-scenarios: een toetsscenario voor een strikt document

*Voorstel van de hoofdsessie, 2026-11-23; nog niet besloten en niet begonnen. Volgt uit het besluit `remove-absence-markers`:
"nog niet" en "nooit" scheidt voortaan alleen een strikt document, en dat moet een toetsdocument dan ook kunnen zeggen.*

## Aanleiding

De scenariolaag van het dialect (`DIVERGENCES.md`, "Scenariotoetsen"; `emit/CaseEmitter.java`) schrijft in een situatie
alleen de kenmerken die gelden, en als verwachting `attributeValue`, `attributeEmpty`, `characteristicPresent` en
`characteristicAbsent`. Het caseschema kent ook `characteristicsAbsent` (invoer) en `attributeOpen` en `characteristicOpen`
(verwachting, RUNTIME-SEMANTICS ch. 1.5), maar geen bestand onder `frontends/` schrijft ze (gemeten 2026-11-23). In een
document met `Afleiden uit leeg: nee` kan een toetsscenario dus niet zeggen dat een kenmerk vast niet geldt (een kenmerk dat
de situatie niet noemt, is open), en niet verwachten dat iets open blijft. De strikte golden `jutterswet-afleiden-uit-leeg`
heeft daarom alleen handgeschreven cases.

## Voorstel

1. Situatie: "- V1 is niet drijfhout" schrijft het kenmerk in `characteristicsAbsent`. In een gewoon document verandert dat
   geen uitkomst.
2. Verwachting: "- de afhaaldatum van V1 staat niet vast" wordt `attributeOpen`, "- of V1 afhaalbaar is, staat niet vast"
   wordt `characteristicOpen`, in de woorden die de uitleg van de toolchain al gebruikt. In een gewoon document weigert de
   frontend beide met een eigen RGS-code (daar is niets open).
3. "- de afhaaldatum van V1 is leeg" blijft `attributeEmpty`, in een strikt document dus vaststaand leeg.
4. Gate 16 regenereert; een golden met een `scenario.rgs` voor een strikt document, bijvoorbeeld de kenmerkgolden van
   `remove-absence-markers`.

## Besluiten voor de eigenaar

- De zinnen: "is niet X" in een situatie, "staat niet vast" in een verwachting.
- Of deze kaart vóór de overstap van de regelset landt die nu `niet van toepassing` gebruikt; voorstel: ja, want die
  regelset moet haar "nog niet"-gevallen dan als open kunnen toetsen.
