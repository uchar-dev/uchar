# contributor-filter-verdict: de lezing van een kwantorvouw zegt per lid of zijn filter vaststaat

*Opdracht van de eigenaar, 2026-11-24: vervolgpunt van de toolchain-kaart `one-empty-in-service`; vóór de bouw gelezen
door een extern model (xhigh), de punten hieronder verwerkt. Nog niet begonnen.*

Goal: de lezing van een kwantorvouw (RUNTIME-SEMANTICS ch. 5.12, en de lezingen van ref en Drools die gate 17 gelijk
houdt) zegt per bijdrager niet alleen het oordeel van de matrix, maar ook of het filter op dat lid vaststaat. Dan kan
een lezer (de uitleg van de toolchain) een vouw met een open filter exact uitleggen, zonder eigen oordeel over het
filter.

Acceptance: een uitgewerkt voorbeeld in ch. 5.12 met een filter "A of B" (A waar, B open) en een filter dat op een
lid onbepaald is, als test van ref; de lezingen van ref en van Drools (beide takken) gelijk in gate 17, met de nieuwe
tekst; alle gates groen (`make gates`); geen bestaande lezing zonder onbepaald filter verandert (hun pins blijven).

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 5.12 (en ch. 1.5 waar het de lezing noemt), `ref/lib/reading.ml`,
`ref/lib/eval.ml` (`contribution`, en de onbereikte bijdragers), `ref/test/`, `backends/drools/.../transpiler/tms/ConditionReading.java`,
`SessionReaders.java`, `cases/SessionDump.java` (`readingLines`, waar de tekst geschreven wordt) en de tests en pins van
de lezingen, `scripts/check-readings.sh` (parser, vergelijking, tripwire, tellers, zelftestrijen),
`backends/drools/README.md`, deze kaart. Mandaat van de eigenaar voor de beschermde paden die het raakt (spec, scripts).

Out of scope: de toolchain (eigen kaart `quantifier-filter-in-explanation`, één pin-bump); de lezing van een vouw over
een set (som, aantal, …): een open lezing breekt die af en zij heeft geen bijdragers; de semantiek zelf (ch. 1.5 en 3.9a
veranderen niet: het oordeel van de vouw blijft hetzelfde).

## Aanleiding

De bijdragers van een kwantorvouw zijn de leden waarvan het filter waar of onbepaald is; per bijdrager geeft de lezing
het oordeel van de matrix (`true`, `false`, een fout, of `undetermined`) en of de evaluatie het lid bereikte
(ch. 5.12: `contributors [<object> <value|verdict|fault> reached|unreached; …]`; Drools
`ConditionReading.Contributor(member, value, fault, reached, undetermined)`, waar `undetermined` de matrix betreft). Of
het filter op een bijdrager vaststaat, staat er niet in. De uitleg van de toolchain kan daardoor bij "alle" met V2
(filter onbepaald, matrix onwaar) niet zeggen of V2 het oordeel draagt; zij noemt nu alleen het oordeel van de vouw
(toolchain-kaart `one-empty-in-service`, Result). Ref en Drools kennen het filteroordeel per lid wel
(`MemberTest.filter` in Drools; in ref het oordeel van het filter in de vouw).

## Voorstel

1. **Spec, ch. 5.12:** een bijdrager van een kwantorvouw waarvan het filter onbepaald is, krijgt in de canonieke tekst een
   aanduiding, bijvoorbeeld `<object> filter undetermined <verdict|fault> reached|unreached`; een bijdrager met een
   vaststaand waar filter blijft geschreven zoals nu, zodat bestaande pins niet veranderen. De bouwer kiest de precieze
   vorm in de stijl van ch. 5.12 en schrijft het uitgewerkte voorbeeld (Jutterswet).
2. **ref:** `reading.ml` schrijft de aanduiding uit het filteroordeel dat de vouw al heeft.
3. **Drools:** `ConditionReading.Contributor` krijgt het filteroordeel (bijvoorbeeld `boolean filterUndetermined`), gevuld
   uit `MemberTest.filter` in `SessionReaders`, op beide takken; de tekst van de lezing volgt ch. 5.12.
4. **Gate 17:** de lezingen blijven gelijk; vloeren die groeien, niet bumpen; een eigen zelftestrij voor de nieuwe
   aanduiding als de vergelijkingsregel dat vraagt.

## Uit de lezing vóór de bouw (extern model, nagerekend)

- **Bijdragers.** Ch. 5.12 en de javadoc van `ConditionReading` noemen de bijdragers nu "the members of its range that
  pass its filter". Zeg het precies: de leden waarvan het filter niet vaststaand onwaar is (waar of onbepaald). Een lid
  met een vaststaand onwaar filter is geen bijdrager, ook als dat filter een open deel heeft (`A en B` met A onwaar en
  B open); een lid met `A of B` (A waar, B open) heeft een vaststaand waar filter en krijgt geen aanduiding.
- **Fout in het filter.** Een filterfout op enig lid laat de vouw falen zonder bijdragers (ch. 5.12); er is geen derde
  filterstand. Binnen één samengesteld filter vangt het voorlopig doorlezen een fout na een onbepaald deel op tot
  onbepaald.
- **Positie.** `reached`/`unreached` gaat over de matrix: het filter loopt op elk lid vóór elke matrix, dus een
  onbereikte bijdrager kan een onbepaald filter hebben. ref verliest dat nu: `eval.ml` geeft de onbereikte leden als
  `List.map fst parts` (rond regel 3712–3766) en gooit het filteroordeel weg; Drools (`SessionReaders`) houdt het. Neem
  het mee, anders lopen ref en Drools uiteen. Een uitvoerder zonder volgorde schrijft `unordered`; ook daarnaast kan de
  aanduiding staan. De javadoc van `ConditionReading.Contributor` over `reached == null` volgt wat Drools in een
  lezingsbuild met een open filter doet.
- **Tekstvorm.** Kies een vorm zonder "undetermined undetermined" (filter én matrix onbepaald), met `<object>` voorop
  (de sortering). Het bestaande voorbeeld `WV-5.12-open-quantifier-fold` heeft een lid met een open filter en verandert
  dus; elke lezing zonder onbepaald filter blijft gelijk.
- **Gate 17.** De parser van `check-readings.sh` (`scan_value`, `split`, rond regel 378–409) leest een bijdrager als
  `<member> <één token of foutcode> <rest>`; met de nieuwe aanduiding zou hij het oordeel verkeerd lezen, de tripwire op
  `fault` stil overslaan, `unordered` niet herkennen en onbereikte bijdragers niet tellen. Pas parser, vergelijking,
  tripwire en tellers aan, met verplichte zelftestrijen: een onbepaald tegenover een vaststaand filter, `unordered`
  naast de aanduiding, en een tripwire-rij `… fault EF-019 unordered` met de aanduiding. Vloeren die groeien, niet
  bumpen.
- **Tests** (ref, en via gate 17 Drools): een onbereikte bijdrager met een onbepaald filter; filter én matrix
  onbepaald (ook een voorlopig opgevangen matrixfout); een vaststaand onwaar samengesteld filter met een open deel
  (geen bijdrager); een filterfout na een onbepaald filter (geen bijdragers).
