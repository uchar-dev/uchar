# dimensions-executed: een gedimensioneerd attribuut draait in ref, clingo en Drools

*Vastgelegd door de hoofdsessie op 2026-11-02, op verzoek van Yeb: RegelSpraak (specificatie §3.6, §13.3.7) en ALEF kennen
dimensies, dus uchar moet ze uitvoeren, niet alleen beschrijven. Aanleiding: bij `20261009-drools-schrijfgrens` bleek dat EF-004
op een dimensiecel geen test kan krijgen.*

Goal: een attribuut met een dimensie (een geordende lijst labels, IR-SPEC ch. 2.7) houdt per label een waarde, en het lezen van
een cel, het schrijven van een cel en de vouw over een selectie van cellen draaien in het vierkant ref ≡ clingo ≡ Drools, op
goldens met cases en absolute verwachtingen.

## Stand op main `b54a8fd3a`

| Laag | Dimensies |
|---|---|
| IR en schema | `Dimension {name, plural?, preposition?, labels}`, `DimensionCellRead {attribute, labels}`, `Aggregation.dimensionSelection {dimension, labels}` (sum, minimum, maximum) |
| verifier | WF-014, WF-040, WF-089 tot en met WF-091 |
| frontend | schrijft cel en vouw; goldens `jutterswet-seizoenvondst`, `jutterswet-seizoenvouw`, `jutterswet-vormenkabinet`, alle zonder cases |
| semantiek | ch. 5.8.5 (lege cel telt niet mee; een geheel lege selectie is leeg); de vouw staat als "postponed, not rejected" (IR-SPEC rond 5843) |
| case-formaat (`spec/schema/uchar-case-v0.schema.json`) | geen invoer of verwachting voor een cel |
| ref | "Not implemented" (`ref/README.md`), faalt met context |
| clingo | geen vertaling |
| Drools | de transpiler kent `DimensionCellRead` (één dimensie; meer is `DIMENSION_CELL_MULTI_DIMENSION`) en de vouw (`rsDimSom`, `rsDimMin`, `rsDimMax`), maar weigert gedimensioneerde invoer (DROOLS-047) |

CONSTRUCTS rij 28 deelt het in als "split": de declaratie van een dimensie is core, het gebruik op een attribuut dialect.

## Eerst onderzoeken (vóór de bouw)

1. Wat RegelSpraak (specificatie v2.3.0, §3.6, §13.3.7; parafrase met §-nummer, geen citaten in de repo) en ALEF
   (`/Users/yeb/Developer/uwv/ALEF`, o.a. `Test_Spraken.Dimensie_voorbeelden.mps`, de structuur- en translatormodellen) met een
   dimensie doen: meer dimensies op één attribuut, een cel schrijven, een lege cel, een interval van labels, de volgorde, invoer en
   uitvoer van een cel, en wat de geporte runtime (`RegelSpraakRuntime`, de `rsDim*`-functies) doet.
2. Wat in de IR ontbreekt: een schrijfactie naar een cel (een doel met labels), meer dimensies tegelijk, een cel op een tijdlijn.
3. Een voorstel voor de case-vorm van een cel (invoer en verwachting), de semantiek per uitvoerder, en de goldens; daarna een
   ontwerpadvies (Fable) over de keuzes.

## Acceptatie (na het onderzoek aanscherpen)

- (a) De case-vorm draagt een cel als invoer en als verwachting (schema, beschermd: voorstel); gate 4 toetst haar.
- (b) Ref en clingo lezen, schrijven en vouwen cellen volgens ch. 5.8.5 en de nieuwe tekst; Drools draagt de invoer (DROOLS-047
  `RETIRED` of beperkt) en speelt de goldens op beide branches.
- (c) De drie bestaande goldens krijgen cases; nieuwe goldens voor wat het onderzoek vindt (meer dimensies, schrijven naar een cel),
  eigen Jutterswet-casus, rechtenvrij. Gate 12 `EXPECTED_PLAYED` en `EXPECTED_CHANNEL` bewegen met naam.
- (d) EF-004 op een cel (de schrijfgrens en de invoertoets uit `20261009-drools-schrijfgrens`), met een rood geval.
- (e) Gate 17 leest een `DimensionCellRead` (nu buiten gate 17, RUNTIME-SEMANTICS rond 6055).
- (f) `make gates` groen.

Omvang: groot (schema, drie uitvoerders, spec, corpus). Waarschijnlijk in stappen, zoals `unit-factors`.

May touch: `spec/schema/uchar-case-v0.schema.json`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md` (voorstel), `ref/`,
`backends/clingo/`, `backends/drools/`, `frontends/regelspraak/` (als de schrijfvorm ontbreekt), `corpus/`, `scripts/` (pins,
voorstel), `ref/README.md`.
