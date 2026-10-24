# drl-rule-part: de herkomst van een regel noemt ook zijn rol, en een stapel haar type

*Voorstel van de werkbank (2026-10-24), met akkoord van de eigenaar door de werkbank-sessie in deze backlog gezet (niet
gecommit). Vervolg op `drl-rule-origin` (done). Klein; landt vóór de toolchain-kaart `drl-rule-part-fields`
(toolchain `backlog/new/20261024-drl-rule-part-fields.md`), die erna de pin bumpt.*

## Aanleiding

De werkbank toont een defaults-stapel aan een jurist als **beslisvolgorde**: de varianten van een regel, de hoogste
eerst (ch. 7.1). Op de kaart "Verlaagde DRL" van een variant staan de DRL-regels die haar default verlaagt, en in de
trace en de dekking van een scenario de stapels zelf. Twee dingen kan de werkbank daar niet zeggen zonder de naam van
een regel te ontleden, en dat doet zij niet:

1. **Welke rol een regel in de verlaging heeft.** `originOf` zegt sinds `drl-rule-origin` van welke default een regel
   is (`RuleOrigin.Kind.DEFAULT` met de pointer), maar niet wat hij voor die default doet. De javadoc van
   `Kind.DEFAULT` noemt de rollen: de regel van de eigen beslissing, de terugval, de foutregel van de rechtvaardiging,
   de doorgeef-, verschil- en eensregels die de uitzonderingsgroepen verzoenen (ch. 7.9), de bijdrageregels van de
   vouwen in rechtvaardiging of gevolg (met hun filterfout-, lidfout- en filterdeponeringsregels), en de leesregel;
   `Kind.STACK` de verzoening aan de wortel (ch. 7.2) en `geen rekendatum`. Die rol staat nu alleen in het label van
   de naam (`TmsTranspiler` r. ±3159 `": default "`, r. ±3220 `": terugval "`, r. ±5163 `": filterfout in "`, …).
   Gemeten op v2 (regels `ece012e`, toolchain `b0b3f849`, de bundel fundament plus herleving-ww): van de 339 regels van
   de DRL-tekst zijn er 64 `default`, 32 `bijdrage aan vouw`, 9 `fout`, 8 `eens`, 4 `terugval` en 2 `filterfout`.
2. **Voor welk type een stapel loopt.** `generated()` geeft bij een stapel het lid dat zij beslist, "as declared on
   its owner's chain" (`GeneratedFrom` met `owner` = het declarerende type). Het type waarvoor dit exemplaar van de
   stapel loopt (`st.owner()` in `stackName`, r. ±2891; ch. 7.10, "the effective stack per type") staat alleen in de
   naam. Op v2 zijn `defaults K:Recht op uitkering WW.indicatie herleving recht op uitkering` en
   `defaults K:RechtOpUitkeringWwBijOntslag.indicatie herleving recht op uitkering` twee stapels met dezelfde
   `generatedFrom` (`{KENMERK, RechtOpUitkering, indicatie herleving recht op uitkering, defaultsstapel}`); de
   werkbank kan ze niet uit elkaar houden of het subtype noemen.

## Doel

- `RuleOrigin` krijgt de rol van de regel in de verlaging van zijn vorm, default of stapel, als vaste sleutel: voor
  `DEFAULT` en `STACK` de rollen hierboven, voor `FORM` dezelfde sleutels waar een gewone vorm dezelfde soort
  hulpregel heeft (een bijdrage aan een vouw, een foutregel), en voor de eigen regel van een vorm een sleutel daarvoor.
  Gelezen uit wat de verlaging noteerde toen zij de regel bouwde, zoals `originOf` nu, nooit uit de naam. De bouwer
  kiest de sleutels en zegt waarom; ze zijn stabiel genoeg om in een publiek veld van de toolchain te staan.
- Waar het zonder nieuwe nummering kan: bij een bijdrageregel (en haar filterfout-, lidfout- en
  filterdeponeringsregels) de vouw waartoe zij hoort, als JSON Pointer naar de `Aggregation` in het document, zodat een
  lezer de tekst van die vouw (haar `source.snippet`) kan tonen. Kan dat niet zonder de nummering van de lowering na
  te bouwen, dan zegt het Result waarom en blijft het weg.
- `generated()` noemt bij een stapel ook het type waarvoor zij loopt (de eigenaar van de effectieve stapel), naast het
  lid zoals gedeclareerd; bij een versie van een vorm verandert niets. De bouwer kiest de vorm (een veld naast
  `GeneratedFrom`, of een eigen record voor een stapel) en zegt waarom.
- Geen gedragsverandering: DRL-tekst en exec-model bytegelijk (`BranchDumpsCorpusWitnessTest` ongewijzigd).

## Acceptatie

- (a) Een getuige over het corpus: elke regel met een vorm (`FORM`, `DEFAULT`, `STACK`) heeft een rol; elke stapel in
  `generated()` heeft een type, en dat type is het declarerende type of een subtype ervan.
- (b) Gericht: een groep met een uitzonderingsgroep (eigen beslissing, terugval, verzoening), een default met een vouw
  in zijn voorwaarde (de bijdragen en hun filterfout wijzen naar die vouw, als de pointer er komt), een gedateerde
  groep (`geen rekendatum`), en een stapel over een subtype (twee stapels met hetzelfde lid en elk hun eigen type).
- (c) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).

## Niet in deze kaart

- Welke default van een stapel in een sessie vuurde (de trace van de toolchain telt per vorm). Als de werkbank dat
  later vraagt, is dat een eigen kaart.
- De namen van de regels en de stapels: die blijven zoals ze zijn.

## Daarna, in de werkbank (geen deel van deze kaart)

Na de toolchain-kaart en een pin-bump zegt de kaart "Verlaagde DRL" bij elke regel van een variant in de woorden van
een jurist wat hij doet (de toets van de variant, een verzamelstap voor haar voorwaarde, een foutregel, …), en noemen de
kaart, de trace en de dekking bij een stapel het type waarvoor zij loopt; de codenaam van de stapel gaat naar de
tooltip.
