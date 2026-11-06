# jutterswet-lidnamen — leden waarvan de naam geen Java-naam is

Een Vondst met vier attributen en een kenmerk waarvan de gedeclareerde naam
zoals geschreven geen Java-identifier of DRL-eigenschap geeft: `m²` (een
cijferteken dat geen identifier draagt), `final` (een woord van Java), `in`
(een woord van DRL), `3e lid` (een cijfer vooraan) en het kenmerk `is` (een
woord van DRL). WF-057 laat elk van die namen toe (IR-SPEC ch. 2.1); de
verifier geeft PASS.

    De in van een Vondst moet gesteld worden op de m² van de Vondst
    indien de Vondst is is.

    Het 3e lid van een Vondst moet gesteld worden op de in van de Vondst
    indien de final van de Vondst groter is dan 0.

- `case1-alles-gevuld`: m² 12.50, final 1, is: in **12.50**, 3e lid **12.50**.
- `case2-final-nul`: final 0: in **12.50**, 3e lid **leeg**.
- `case3-zonder-is-invoer`: niet is, in 5.00 als invoer: de regel vuurt niet
  en de invoer blijft staan, **5.00**, en het 3e lid leest die, **5.00**.

Het document heeft geen `source.rgs`: de RegelSpraak-grammatica laat `in` en
`is` niet als naam van een lid toe.

## Rood eerst

Gemeten op 2026-11-06 met de jar van vóór de kaart
`beanlidnamen-zonder-java-identifier`: beide Drools-takken stopten bij het
compileren van de bean (`illegal character: '²'` op het veld `m²`), en de
DRL-teksttak weigerde een lid `in` met DROOLS-058. `ref` en clingo spelen elke
case. Sinds die kaart geeft `Namen.ident` elk lid een Java-identifier (`m2`,
`x3eLid`; een woord van Java krijgt een veld `final_` met de getter
`getFinal`), en leest de DRL-tekst een lid zonder DRL-eigenschap via zijn
getter (`getIn()`, `isIs()`): beide takken spelen de drie cases.
