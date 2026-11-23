# strict-scenarios: een toetsscenario voor een strikt document

Goal: de scenariolaag van de RegelSpraak-frontend schrijft voor een document met `Afleiden uit leeg: nee` wat het
caseschema al kent: `characteristicsAbsent` uit "- is niet X", "- heeft geen X" en "- heeft niet het X" in een situatie,
en `attributeOpen` en `characteristicOpen` uit "staat niet vast" in een verwachting. De golden
`jutterswet-afleiden-uit-leeg` krijgt een `scenario.rgs` dat zijn zes handgeschreven cases byte voor byte geeft.

Acceptance: `bash scripts/check-scenario.sh` PASS met 49 scenario-goldens en zes case files meer, en geen case file of
`ir.json` in het corpus veranderd (`git diff --stat main -- corpus` toont alleen `scenario.rgs` en `NOTE.md`);
`bash scripts/check-regelspraak.sh` PASS (de JUnit-suite zonder overgeslagen test, plus het register), met tests voor de
drie situatievormen, de tegenstrijdigheid in één blok, `gebaseerd op` in beide richtingen, de periode en het
tijdsafhankelijke kenmerk, "staat niet vast" over een attribuut (`de`/`het`) en over een bijvoeglijk en een bezittelijk
kenmerk (met en zonder lidwoord en komma), de weigering in een gewoon document, de tokens, en een bestaande naam met
`staat`, `vast`, `of` of `niet` die leest als voorheen; `sh scripts/check-self-contained.sh` PASS; `make gates` groen
(de hoofdsessie, vóór de landing).

May touch: `frontends/regelspraak/grammar/RegelSpraak.g4`, `frontends/regelspraak/grammar/DIVERGENCES.md`,
`frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/ScenarioResolver.java`,
`.../resolver/SemanticTokens.java`, `.../ir/ResolvedScenario.java`, `.../emit/CaseEmitter.java`,
`frontends/regelspraak/src/main/resources/dev/uchar/regelspraak/diagnostic-codes.tsv` en de twee catalogi onder
`.../regelspraak/message/`, `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/`,
`corpus/golden/jutterswet-afleiden-uit-leeg/` (`scenario.rgs` nieuw, `NOTE.md`), deze kaart.

Out of scope: de toolchain (`ScenarioData`, `SituationToRequest`, playground: eigen kaart, één pin-bump later); de
markers (`afwezigheidsWaarde` en alles van `one-empty` deel 1 blijft staan); "- X is leeg" in een situatie
(`attributesEmpty`, kaart `settled-empty-input`, geparkeerd); spec, schema's, executors en `scripts/`.

*Besloten door de eigenaar, 2026-11-23 (de zinnen en de volgorde); nog niet begonnen. Volgt uit het besluit `one-empty`:
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
   "- is drijfhout") en "- heeft geen strandrecht" (bezittelijk; met een bepaald lidwoord "- heeft niet het strandrecht") zetten het kenmerk in
   `characteristicsAbsent`. Hetzelfde
   kenmerk als geldend en als niet geldend in één blok is een fout met een eigen RGS-code; een situatie `gebaseerd op` een
   andere overschrijft wat zij van die andere erft, zodat de case het kenmerk niet in beide lijsten krijgt (EF-047). Een
   periode bij een niet geldend kenmerk wordt met een eigen RGS-code geweigerd. In een gewoon document verandert `characteristicsAbsent` geen
   uitkomst.
2. **Verwachting**: "- de afhaaldatum van V1 staat niet vast" (ook met "het") wordt `attributeOpen`; "- of V1 afhaalbaar is,
   staat niet vast" en "- of V1 strandrecht heeft, staat niet vast" worden `characteristicOpen`, met dezelfde lidwoorden als een verwachting dat het kenmerk geldt, de komma vrij. Dat zijn de
   woorden die de uitleg van de toolchain al gebruikt. `of`, `staat`, `niet` en `vast` worden geen gereserveerde woorden. In een
   gewoon document weigert de frontend beide met een eigen RGS-code (daar is niets open).
3. "- de afhaaldatum van V1 is leeg" blijft `attributeEmpty`, in een strikt document dus vaststaand leeg. Een attribuut als
   vaststaand leeg in de situatie hoort hier niet bij: dat vraagt `attributesEmpty` in het caseschema (kaart
   `settled-empty-input`, geparkeerd).
4. Gate 16 regenereert; `jutterswet-afleiden-uit-leeg` krijgt een `scenario.rgs`. De kenmerkgolden van `one-empty` deel 1
   komt later en krijgt er meteen ook een.
5. **Toolchain** (pin-bump): `ScenarioData`, `SituationToRequest` en de playground zetten `characteristicsAbsent` om in
   `{"state": "holdsNot"}` en lezen `attributeOpen` en `characteristicOpen` als verwachting.

## Besluiten (2026-11-23)

- De zinnen: "- is niet X" en "- heeft geen X" in een situatie, "staat niet vast" in een verwachting.
- De volgorde: deze kaart eerst, dan `one-empty` deel 1 en deel 2, dan de toolchain in één pin-bump; daarna zet de werkbank
  de regelset om die nu `niet van toepassing` gebruikt.
