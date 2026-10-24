# drl-rule-functions: de DRL-tekst zegt per regel welke functies bij hem horen

*Voorstel van de werkbank (2026-10-24). De eigenaar vroeg de werkbank-sessie deze kaart te schrijven en zelf te bouwen;
zij begint na de landing van `drl-rule-part`, omdat `Rendered` en `originOf` ernaast liggen. Niet gecommit tot de bouw.
Vervolg in de toolchain: `backlog/new/20261024-drl-rule-functions-fields.md`.*

## Aanleiding

De werkbank toont op de kaart "Verlaagde DRL" per definitie haar DRL-regels, als de stukken `rule "…" … end` van `drl`
(`/playground/analyse`). Wat een regel toetst en berekent, staat daar niet in: elke toets en elke waarde van het plan is
een functie in het Java-dialect, die de regel aanroept (`DrlRendering`, javadoc r. ±60–76). Een lezer ziet zo niet waar
een waarde gemaakt wordt. Voorbeeld van de eigenaar, v2/herleving-ww (regels `ece012e`, toolchain `b0b3f849`): de
uitzondering `herleving per kalendermaand art. 21 lid 1 na onderdeel c` berekent "de datum met jaar: het jaar uit (zijn
einddatum-van-een-eindigingsgrond plus 1 mnd), maand: … en dag: 1". Haar regel
`defaults A:Eindigingsgrond.herlevingsdatum: terugval 2` roept `r118_toets0($einddatumVanEenEindigingsgrond, …)` en
`r118_afloop2($einddatumVanEenEindigingsgrond)` aan; de dag 1 staat alleen in de functie `r118_afloop2`, en de vier
voorwaarden (drie geërfd van de basisregel, dialect §4) in `r118_samengesteld1`. De DRL-tekst van die bundel heeft 339
regels en 562 functies.

`DrlRendering.Rule` weet welke functies van een regel zijn (`functions`, namen `r<n>_<soort><plaats>`) en zet ze direct
vóór de regel, elk met de RegelSpraak-zin van haar knoop als commentaar (`fill`). De werkbank leidt die toewijzing niet
af uit de tekst: zij leest geen namen en knipt de tekst alleen waar de toolchain zegt.

## Doel

- `DrlRendering.Rendered` (of een ingang ernaast, bereikbaar zoals `originOf` via de build) zegt per regel van de tekst
  welk stuk tekst bij hem hoort: zijn functies en de regel zelf, als plaatsen in `text` (bijvoorbeeld het begin van zijn
  eerste functie, het begin van `rule "…"` en het einde na `end`), of als de namen van zijn functies met hun plaatsen.
  De bouwer kiest de vorm en zegt waarom; eis is dat een lezer zonder de tekst te ontleden per regel zijn functies kan
  tonen, met hun commentaar.
- Roept een regel een functie aan die niet van hem is (als dat voorkomt), dan noemt het antwoord die ook.
- Geen gedragsverandering: `text` bytegelijk, het exec-model ongewijzigd (`BranchDumpsCorpusWitnessTest` ongewijzigd).

## Acceptatie

- (a) Een getuige over het corpus: voor elke golden die de backend als DRL-tekst draagt, dekken de stukken van de regels
  samen precies de tekst na de kop (package, imports, globals), zonder overlap; elk stuk begint met zijn functies (of met
  `rule` als hij er geen heeft) en eindigt na zijn `end`; elke functie die een regel aanroept, staat in zijn eigen stuk
  of in de opgave van de vorige bullet.
- (b) Gericht: een regel zonder functies, een regel met een samengestelde voorwaarde (een functie die een andere
  functie van dezelfde regel aanroept), en een regel van een stapel met een terugval.
- (c) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

## Daarna (geen deel van deze kaart)

De toolchain geeft het per vermelding van `drlRules` door (kaart `drl-rule-functions-fields`); de kaart "Verlaagde DRL"
van de werkbank toont boven elke DRL-regel zijn functies, met de RegelSpraak-zin uit hun commentaar.
