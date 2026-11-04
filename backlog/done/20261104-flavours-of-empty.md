# flavours-of-empty: welke soorten afwezigheid de IR onderscheidt

*Opgesteld door de hoofdsessie op 2026-11-04 op een vraag van de eigenaar, na het besluit dat een lege waarde niets beslist
(kaart `empty-write-decides-nothing`). Voor later; eerst onderzoek.*

## Aanleiding

uchar kent één leeg: de waarde die er niet is. Sinds 2026-11-04 beslist een schrijver die met leeg vuurt niets, op een tijdlijndoel en op
een scalair doel: de invoer blijft staan. Een regel kan een attribuut dus niet meer leegmaken. RegelSpraak heeft daar ook geen vorm voor:
`leeg` staat alleen in een predicaat ("is leeg") en in een verwachting van een toetsdocument, niet als waarde van een gelijkstelling.

De eigenaar vraagt hoe je dan "known unknown" schrijft: een waarde waarvan vaststaat dat ze onbekend is, tegenover een waarde die nog niet
bekend is. Misschien zijn er meer soorten (niet aangeleverd, niet van toepassing, bewust gewist, onbepaald door een fout).

## Goal

Een voorstel (`spec/proposals/`) dat de soorten afwezigheid opsomt die regelgeving en uitvoeringspraktijk onderscheiden, met per soort: of
RegelSpraak, ALEF en de werkbank-regelsets haar kennen en hoe, en vooral hoe de gevestigde, formeel gefundeerde regeltalen het doen (de
eigenaar wil niet alleen ALEF zien): Catala (default logic, optietypes), DMN/FEEL (`null`, S-FEEL), answer set programming (`not p` als
niet afleidbaar tegenover de klassieke negatie `-p` als bekend onwaar; s(CASP)), Datalog en Prolog (negation as failure, de gesloten
wereld), SQL (`NULL` en driewaardige logica), de logica van Kleene en Łukasiewicz, LegalRuleML en LKIF (open textuur, weerlegbaarheid),
L4, OpenFisca, en wat de literatuur over rules as code verder als gevestigd noemt. ALEF meet je in alle vier de routes met `alef-bridge`
(`/Users/yeb/Developer/uchar-dev/alef-bridge`); maak daar zo nodig een kaart. Per soort ook wat ze met de semantiek van uchar zou doen
(ch. 1 leeg, ch. 3.1 een gat, ch. 3.11 en 5.7 het schrijven, ch. 7 defaults, het §9.5-kanaal); en een aanbeveling: welke soorten de IR
moet dragen, en of een regel er een moet kunnen schrijven. Normvragen naar de eigenaar.

## Acceptance

- (a) Het voorstel met bronnen per soort (paragraafnummers, bestand en regel, geen tekst uit de RegelSpraak-specificatie).
- (b) Per aanbevolen soort een uitgewerkt voorbeeld met absolute waarden.
- (c) De normvragen apart, elk met een aanbeveling.

May touch: `spec/proposals/` (nieuw document), kaartverplaatsing.

## Omvang

Onderzoek, middel.

## Result

*Onderzoeker, 2026-11-04. Niet gemerged, niet gepusht. Voorstel: `spec/proposals/FLAVOURS-OF-EMPTY.md` (commit
`8a93fe9e7`). Rapport en bronnotities buiten de repo: `research/flavours-of-empty/rapport.md` en `notes/`. Logs:
`werk/flavours-of-empty/logs/`.*

### Wat ik las

- RUNTIME-SEMANTICS ch. 1, 3.1, 3.11, 5.1–5.7b, 7.1–7.3, 7.8, 9, 10.1; `KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md`;
  `PHASE5-CATALA.md`; CONSTRUCTS rijen 7, 8, 16, 18; het caseschema; de goldens `jutterswet-opgave-bedrag` en
  `anchor/jutterswet-beschikkingsom`.
- RegelSpraak v2.3.0 §3.2, §3.3.3, §3.5, §5.8, §6, §8.1.1, §8.1.2, §9.1, §9.5, §9.6, §9.7.6, §12, §13.4, Bijlage 1, en het
  document typeringen (alleen paragraafnummers in het voorstel).
- ALEF `872b859`: het concept `Leeg`, de waarschuwing "Gelijkstelling met leeg doet niets" (`check_Gelijkstelling`,
  test `ALEF4951`), `MLazyProperty.pushResult`, de test "initialisatie met leeg doet niets" (`ALEF3393`), de
  verstekwaarde van een dienst.
- De werkbank-regelsets (`regelspraak-rules` `0aff7ed`): datalacunes als kenmerk (`dgl-18`), `gevuld` als bewijs.
- Catala (artikel ICFP 2021 §4.1, runtime en boek), DMN 1.5 (FEEL, S-FEEL, Tabellen 46–56) met DMN16-91 en Marković
  e.a. 2022, Gelfond en Lifschitz 1991, Wagner 2003, Kowalski 1989, s(CASP) en Blawx (Morris 2023), SWI-Prolog,
  SQL (Codd, via een secundaire bron), SEP "Many-Valued Logic", LegalRuleML 1.0, XACML 3.0, L4, OpenFisca, Oracle
  Intelligent Advisor, HL7 NullFlavor en FHIR DataAbsentReason.

### Wat ik mat

- **ALEF, vier routes** (alef-bridge, kaart `backlog/doing/20261104-empty-kinds-in-alef.md`, branch
  `card/empty-kinds-in-alef`, commits `8a0ae52` en `c980d68`; 92 lezingen in `alef-models/EmptyKinds/expected.tsv`):
  een gelijkstelling aan `leeg` laat invoer 40 staan in gegenereerd lazy, interpreter lazy en interpreter eager, en
  wist hem in gegenereerd eager (daar wint de laatste regel in de volgorde); op een maandtijdlijn wist geen route; een
  Boolean `bevestigd` geeft voor `= onwaar`, `≠ waar` en `is leeg` in alle routes leeg/leeg/leeg (waar), 1/1/leeg
  (onwaar) en leeg/1/1 (niet gegeven). Met een periode genereert ALEF Java die niet compileert; vervolgkaart
  `alef-bridge/backlog/new/20261104-empty-in-a-period.md`. De `WritersInput`-meting gaf met de aangepaste scripts
  dezelfde 84 lezingen.
- **uchar**: drie RegelSpraak-documenten (voorstel §6.1–§6.3, 13 cases) door frontend, ref, clingo-runner en de
  Drools-jar (beide branches). ref: 12 PASS en de verwachte EF-016; Drools: alles gespeeld, EF-016 idem; clingo weigert
  een Boolean-attribuut in een conditie ("unscalable datatype in the ASP translation") en een enumeratie-invoer op een
  berekend doel, beide zonder registercode. `anchor/jutterswet-beschikkingsom` in ref: 4 PASS.
- **Catala 1.2.1**: geen regel → `NoValue`; twee regels → `Conflict`, ook bij berekende gelijke waarden (niet bij twee
  letterlijke 40's); `optional` vraagt een `match`; een eigen enumeratie `Unknown | NotApplicable | Known` werkt en een
  ontbrekende tak is een typefout.
- **clingo 5.8.2**: `not p` tegen `-p` geeft "onbekend" voor een object zonder feit; de gesloten wereld als regel;
  `p` en `-p` samen: onvervulbaar. **sqlite 3.43.2**: `NULL = NULL` is NULL, `NULL + 5` NULL, `NULL OR 1` 1, `SUM`
  over alleen NULL NULL, `COUNT` 0, `TOTAL` 0.0.

### Aanbeveling in vijf regels

1. De IR houdt één afwezigheid als waarde, leeg, met de regels van ch. 1.2; geen driewaardige logica (ch. 1.4 blijft).
2. Gedragen en ongewijzigd: nog niet afgeleid, niet aangeleverd, niet van toepassing door een voorwaarde (leeg), bekend onwaar (Boolean onwaar), fout (leeg plus §9.5-feit of evaluatiefout), tegenstrijdig (EF-016), lege verzameling.
3. Bekend onbekend en niet van toepassing als uitkomst zijn een stand naast de waarde: een enumeratie in RegelSpraak, ook een somtype in de IR; een regel schrijft ze als gewone waarde.
4. Een regel schrijft geen leeg; bewust wissen blijft onuitdrukbaar (besluit 2026-11-04).
5. Geen nieuwe vorm in wire, schema of dialect; wel twee clingo-kaarten en daarna een golden met het standpatroon.

### Normvragen (voorstel §8, elk met aanbeveling)

- N1 een tweede afwezige waarde naast leeg? Nee.
- N2 mag een regel bewust leegmaken? Nee.
- N3 een expliciet leeg (`null`) in het caseformaat? Nee.
- N4 het standpatroon als schrijfwijze van bekend onbekend en niet van toepassing? Ja: enumeratie in RegelSpraak,
  daarnaast het somtype in de IR.
- N5 dialectsuiker voor het patroon? Nu niet; pas bij een statuut in het corpus.
- N6 een kenmerk met drie standen? Nee; daarvoor is het Boolean-attribuut.

### Voorgestelde vervolgkaarten (niet aangemaakt; de hoofdsessie beslist)

- `clingo-boolean-attribute-in-condition`: een Boolean-attribuut in een conditie in de ASP-backend.
- `clingo-enum-input-on-computed-target`: een door de case gegeven tekst- of enumeratiewaarde op een berekend doel.
- Na N4: een golden met het standpatroon van voorstel §6.1 (de rode case 6 met de gedeelde EF-016-parameters).

Gate 1 (`scripts/check-self-contained.sh`) PASS. Geen `make gates` (alleen `spec/proposals/` en de kaart).

### Besluiten van de eigenaar (2026-11-04, hoofdsessie)

Eén leeg (de afwezigheid) en twee markers als gevulde waarden, onbekend en niet van toepassing, per attribuut aan te zetten voor elk
datatype (ook per periode), niet op een kenmerk; een marker beslist, leeg niet; wie een attribuut met markers leest, toetst eerst. De
bronvorm is dialectsuiker die naar het somtype verlaagt. Vastgelegd bovenaan `spec/proposals/FLAVOURS-OF-EMPTY.md`; bouwkaart
`20261104-absence-markers`. Vervolg in clingo, gemeten in dit onderzoek: een Boolean-attribuut in een conditie en een enumeratie-invoer op
een berekend doel worden zonder registercode geweigerd; mee in `20261104-writers-as-one-defaults-stack` (de enumeratie-invoer) en in
`absence-markers` (de Boolean, omdat markers op een Boolean-attribuut die conditie nodig hebben).
