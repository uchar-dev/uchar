# drl-rule-origin: de Drools-backend zegt per regel van welke vorm en welke default hij is

*Uit de toolchain-kaart `drl-rule-provenance` (werkbank, 2026-10-23; toolchain `backlog/new/20261023-drl-rule-provenance.md`).
De toolchain-sessie besliste daar dat de herkomst uit uchar komt. Klein; landt vóór de toolchain-kaart, die erna de
pin bumpt.*

## Aanleiding

De werkbank toont per variant van een defaults-groep haar verlaagde DRL (tak `DRL_TEXT`) en zoekt die regels nu op naam.
De naam van een regel van een stapel is `<stapel>: default <label>` of `<stapel>: bijdrage aan vouw <n>`
(`TmsTranspiler`, r. ±3044). Het label is de eigen nummering van de lowering, en een bijdrage aan een vouw noemt de
default niet waarvoor zij bijdraagt. `TmsTranspiler.formOf(ruleName)` geeft de vorm van een regel, maar niet welke
default van de groep. Gemeten op v2 (de werkbank): de groep `herleving art. 21 lid 1` wordt `default 0` tot en met
`default 4`, de uitzondering `nieuw recht boven de herlevingsgrens art. 21 lid 2 onder a` is `default 2`, en de stapel
heeft daarnaast `bijdrage aan vouw 16` tot en met `22`. Bij een stapel over subtypen (RUNTIME-SEMANTICS ch. 7.10) en bij
geneste uitzonderingen is de nummering niet uit het IR af te leiden zonder die van de lowering na te bouwen. De
toolchain heeft geen semantiek van zichzelf en bouwt haar niet na.

## Doel

- Een publieke ingang naast `formOf` (bijvoorbeeld `originOf(String ruleName)` op `TmsTranspiler`, bereikbaar via
  `DroolsBackend.Build`), die per regel van het model zegt waarvan hij een regel is:
  - de vorm van het document, zoals `formOf` (de naam van een regelvorm of van een defaults-groep), of geen voor een regel
    van geen vorm;
  - waar de regel bij een default hoort: die default, als JSON Pointer in de vorm die `decisionsOf` al gebruikt (kaart
    `defaults-decider`), zodat de toolchain dezelfde pointer krijgt als voor de beslissing; en waar een gewone regel in
    een stapel beslist (ch. 7.10), die regelvorm;
  - dat ook voor de hulpregels van een default of regel (een bijdrage aan een vouw in haar voorwaarde of waarde, een
    lezingregel, een doorgeefregel): de default of regel waarvoor zij bestaan.
  De bouwer kiest de vorm van het antwoord en zegt waarom.
- Geen gedragsverandering: de DRL-tekst en het exec-model zijn bytegelijk (`BranchDumpsCorpusWitnessTest`
  ongewijzigd); de ingang leest wat het plan al weet (`Node`, de pointer van de default).

## Acceptatie

- (a) Een getuige over het corpus: voor elke golden die de backend draagt heeft elke regel van het model een antwoord;
  elke regel waarvan de vorm een defaults-groep is, wijst een default aan die in het document bestaat (de pointer lost
  op), behalve wat de stapel zelf draagt zonder default (dat zegt het antwoord expliciet); elke default van een gedragen
  groep heeft ten minste één regel.
- (b) Gericht: een vlakke groep, een geneste uitzondering, een stapel over een subtype met een gewone regel erin, en een
  default met een vouw in zijn voorwaarde: de bijdragen aan die vouw wijzen naar die default.
- (c) De volledige gate-set groen vóór de landing (`WAY-OF-WORKING.md` §6).
