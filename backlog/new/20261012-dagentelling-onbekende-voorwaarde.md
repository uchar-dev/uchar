# dagentelling-onbekende-voorwaarde: de frontend laat een voorwaarde van een dagentelling niet meer stil weg

*Vastgelegd door de hoofdsessie op 2026-10-12, uit de bijvangst van de toolchainkaart
`20261010-sessie-via-de-ingang-van-uchar` (fixronde 1). Een bestaande fout in de frontend krijgt een eigen kaart.
Gemeten op uchar `b536bbc9`, met de verifier, ref en clingo uit `_build` van de hoofdcheckout.*

## Aanleiding

Een kopie van de golden `jutterswet-dagentelling-rgs` waarin de regel `borgdagen` een niet-gedeclareerd kenmerk noemt:

    De borgdagen van een Jutter moet berekend worden als het aantal dagen in de maand dat hij borgvrij is.

(in de golden: "borgplichtig"). De frontend (`bin/regelspraak check`, en `make ir`) geeft geen fout en geen
waarschuwing over `borgvrij`. Ze schrijft een `dayCount` zonder voorwaarde in het IR: één filter in plaats van twee, en
geen `Unresolved`-knoop. Hetzelfde gebeurt met een voorwaarde die geen kenmerk is ("dat zijn heffing gevuld is").

Het IR zegt daarmee iets anders dan de bron. Een stil fout antwoord geeft het niet: elke uitvoerder weigert het
document.

| Uitvoerder | Uitkomst |
|---|---|
| verifier | `WF-058 FAIL /rules/rules/0/versions/0/value: dayCount without a gedurende condition` |
| ref | `EVALUATION FAULT: a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape …` |
| clingo | `TRANSLATION FAULT:` met dezelfde zin |
| Drools, exec-model-tak | `transpiler SKIP: … een dagentelling … met een voorwaarde die geen kenmerk van het onderwerp is (geen voorwaarde)` |

De afnemer krijgt dus een weigering die over een ontbrekende voorwaarde gaat, en niet de echte oorzaak: een onbekende
naam in zijn bron.

Na te maken: een kopie van de golden met in regel `borgdagen` het kenmerk `borgvrij` in plaats van `borgplichtig`,
met `make -C frontends/regelspraak ir FILE=… OUT=…` en de case `case1-voorbeeld.json` van de golden.

## Wat er gebeurt

De frontend meldt een onbekende of ongeldige voorwaarde van een dagentelling bij de resolutie. Ze geeft een diagnose
met de naam, of een `Unresolved`-knoop zoals bij andere onopgeloste namen, en laat de voorwaarde nooit stil weg. Het
IR van een bron zonder diagnose is verifier-schoon.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood eerst: een frontendtest met de bron hierboven, die de diagnose pint.
- (b) Gate 10 groen, en elke golden met een `source.rgs` emitteert byte-gelijk.
- (c) Alle gates van `CLAUDE.md` groen.

## Omvang

Klein. May touch: `frontends/regelspraak/**` (resolver en tests), kaartverplaatsing.
